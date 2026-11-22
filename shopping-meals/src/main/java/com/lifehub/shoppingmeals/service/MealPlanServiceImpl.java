package com.lifehub.shoppingmeals.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.shoppingmeals.mapper.MealMapper;
import com.lifehub.shoppingmeals.model.dto.CreateMealPlanEntryRequest;
import com.lifehub.shoppingmeals.model.dto.MealPlanEntryDTO;
import com.lifehub.shoppingmeals.model.entity.MealPlanEntry;
import com.lifehub.shoppingmeals.model.entity.Recipe;
import com.lifehub.shoppingmeals.repository.MealPlanEntryRepository;
import com.lifehub.shoppingmeals.repository.RecipeRepository;
import com.lifehub.starter.id.Ids;
import com.lifehub.starter.membership.Membership;
import com.lifehub.starter.membership.WorkspaceAccess;
import com.lifehub.starter.web.ApiException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class MealPlanServiceImpl implements MealPlanService {

    private static final Comparator<MealPlanEntry> PLAN_ORDER = Comparator.comparing(MealPlanEntry::getPlanDate)
            .thenComparing(MealPlanEntry::getSlot)
            .thenComparing(MealPlanEntry::getCreatedAt);

    private final WorkspaceAccess access;
    private final MealPlanEntryRepository entries;
    private final RecipeRepository recipes;
    private final Clock clock;

    /** Checks membership and the range, then sorts by day and slot (the slot column sorts by name in SQL). */
    @Override
    @Transactional(readOnly = true)
    public List<MealPlanEntryDTO> getPlan(UUID workspaceId, LocalDate from, LocalDate to) {
        access.require(workspaceId);
        if (to.isBefore(from)) {
            throw ApiException.badRequest("'to' must be on or after 'from'.");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) {
            throw ApiException.badRequest("Ask for at most " + MAX_DAYS + " days at a time.");
        }
        return entries.findByWorkspaceIdAndPlanDateBetween(workspaceId, from, to).stream()
                .sorted(PLAN_ORDER)
                .map(MealMapper::toDto)
                .toList();
    }

    /** Checks membership; a recipe must belong to the same workspace, and its name becomes the title. */
    @Override
    @Transactional
    public MealPlanEntryDTO addEntry(UUID workspaceId, CreateMealPlanEntryRequest request) {
        Membership caller = access.require(workspaceId);
        String title;
        if (request.recipeId() != null) {
            Recipe recipe = recipes.findByIdAndWorkspaceId(request.recipeId(), workspaceId)
                    .orElseThrow(() -> ApiException.badRequest("That recipe is not in this workspace."));
            title = recipe.getName();
        } else if (request.title() != null && !request.title().isBlank()) {
            title = request.title().strip();
        } else {
            throw ApiException.badRequest("Choose a recipe or type what you are eating.");
        }
        String note = request.note() == null || request.note().isBlank() ? null : request.note().strip();
        MealPlanEntry entry = entries.save(new MealPlanEntry(Ids.newId(), workspaceId, request.date(), request.slot(),
                request.recipeId(), title, note, caller.userId(), clock.instant()));
        return MealMapper.toDto(entry);
    }

    @Override
    @Transactional
    public void deleteEntry(UUID workspaceId, UUID entryId) {
        access.require(workspaceId);
        entries.delete(entries.findByIdAndWorkspaceId(entryId, workspaceId)
                .orElseThrow(() -> ApiException.notFound("Planned meal not found.")));
    }
}
