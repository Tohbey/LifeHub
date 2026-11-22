package com.lifehub.shoppingmeals.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lifehub.shoppingmeals.model.entity.MealPlanEntry;

@Repository
public interface MealPlanEntryRepository extends JpaRepository<MealPlanEntry, UUID> {

    /** Finds an entry only if it belongs to this workspace. */
    Optional<MealPlanEntry> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    /** A workspace's plan for the days from {@code from} to {@code to}, both included, in no particular order. */
    List<MealPlanEntry> findByWorkspaceIdAndPlanDateBetween(UUID workspaceId,
            LocalDate from, LocalDate to);
}
