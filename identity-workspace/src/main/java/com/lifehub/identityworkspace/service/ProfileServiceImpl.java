package com.lifehub.identityworkspace.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.identityworkspace.mapper.UserMapper;
import com.lifehub.identityworkspace.mapper.WorkspaceMapper;
import com.lifehub.identityworkspace.model.dto.UpdateUserRequest;
import com.lifehub.identityworkspace.model.dto.UserDTO;
import com.lifehub.identityworkspace.model.dto.WorkspaceDTO;
import com.lifehub.identityworkspace.model.entity.MemberStatus;
import com.lifehub.identityworkspace.model.entity.User;
import com.lifehub.identityworkspace.model.entity.Workspace;
import com.lifehub.identityworkspace.model.entity.WorkspaceMember;
import com.lifehub.identityworkspace.model.event.IdentityEvents;
import com.lifehub.identityworkspace.repository.UserRepository;
import com.lifehub.identityworkspace.repository.WorkspaceMemberRepository;
import com.lifehub.identityworkspace.repository.WorkspaceRepository;
import com.lifehub.starter.events.DomainEventPublisher;
import com.lifehub.starter.web.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class ProfileServiceImpl implements ProfileService {

    private final CurrentUserService currentUsers;
    private final UserRepository users;
    private final WorkspaceRepository workspaces;
    private final WorkspaceMemberRepository members;
    private final DomainEventPublisher events;
    private final Clock clock;

    /** Resolves (or provisions) the current user and maps them to a DTO. */
    @Override
    public UserDTO getCurrentUser(String subject) {
        return UserMapper.toDto(currentUsers.resolve(subject));
    }

    /**
     * Reloads the current user in this transaction, applies the changes and returns the result. Publishes
     * user.profile.updated when the name or email actually changed, so notification knows where to send email.
     */
    @Override
    @Transactional
    public UserDTO updateCurrentUser(String subject, UpdateUserRequest request) {
        UUID userId = currentUsers.resolve(subject).getId();
        User user = users.findById(userId).orElseThrow();
        String previousName = user.getDisplayName();
        String previousEmail = user.getEmail();

        user.updateProfile(request.displayName(), request.email(), clock.instant());

        List<String> changed = new ArrayList<>();
        if (!Objects.equals(previousName, user.getDisplayName())) {
            changed.add("displayName");
        }
        if (!Objects.equals(previousEmail, user.getEmail())) {
            changed.add("email");
        }
        if (!changed.isEmpty()) {
            events.publish(IdentityEvents.USER_PROFILE_UPDATED, null, userId,
                    new IdentityEvents.UserProfileUpdated(userId, user.getDisplayName(), user.getEmail(), changed),
                    userId.toString());
        }
        return UserMapper.toDto(user);
    }

    /** Pages through the user's active memberships and pairs each with its workspace. */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<WorkspaceDTO> getWorkspaces(String subject, Pageable pageable) {
        UUID userId = currentUsers.resolve(subject).getId();
        Page<WorkspaceMember> page = members.findByUserIdAndStatus(userId, MemberStatus.ACTIVE, pageable);
        Map<UUID, Workspace> byId = workspaces.findAllById(page.map(WorkspaceMember::getWorkspaceId).getContent())
                .stream().collect(Collectors.toMap(Workspace::getId, Function.identity()));
        return PageResponse.of(page, m -> WorkspaceMapper.toDto(byId.get(m.getWorkspaceId()), m));
    }
}
