package com.lifehub.identityworkspace.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.identityworkspace.model.entity.MemberStatus;
import com.lifehub.identityworkspace.model.entity.WorkspaceMember;
import com.lifehub.identityworkspace.model.entity.User;
import com.lifehub.identityworkspace.model.entity.Household;
import com.lifehub.identityworkspace.repository.HouseholdRepository;
import com.lifehub.identityworkspace.repository.UserRepository;
import com.lifehub.identityworkspace.repository.WorkspaceRepository;
import com.lifehub.identityworkspace.repository.WorkspaceMemberRepository;
import com.lifehub.starter.membership.InternalMembershipApi;
import com.lifehub.starter.membership.InternalMembershipApi.WorkspaceInfoResponse;
import com.lifehub.starter.membership.Membership;
import com.lifehub.starter.membership.MembershipClient;
import com.lifehub.starter.membership.WorkspaceRole;

import lombok.RequiredArgsConstructor;

/**
 * This service is the source of truth, so it answers membership questions from its own tables. Replaces
 * the starter's HTTP client here, and backs the internal membership endpoint other services call.
 */
@Component
@RequiredArgsConstructor
public class LocalMembershipClient implements MembershipClient {

    private final WorkspaceMemberRepository members;
    private final UserRepository users;
    private final WorkspaceRepository workspaces;
    private final HouseholdRepository households;

    /** Looks up the subject's active membership directly in this service's tables. */
    @Override
    @Transactional(readOnly = true)
    public Optional<Membership> findBySubject(UUID workspaceId, String logtoSubject) {
        return members.findActiveBySubject(workspaceId, logtoSubject)
                .map(m -> new Membership(m.getWorkspaceId(), m.getUserId(), m.getRole()));
    }

    /** Returns the active members among the given user IDs, with their roles. */
    @Override
    @Transactional(readOnly = true)
    public Map<UUID, WorkspaceRole> findMembers(UUID workspaceId, Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return members.findByWorkspaceIdAndStatusAndUserIdIn(workspaceId, MemberStatus.ACTIVE, userIds).stream()
                .collect(Collectors.toMap(WorkspaceMember::getUserId, WorkspaceMember::getRole));
    }

    /** Looks the user up by Logto subject. Never provisions: that only happens on the user's own request. */
    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findUserIdBySubject(String logtoSubject) {
        return users.findByLogtoSubject(logtoSubject).map(User::getId);
    }

    /** Reads the names straight from this service's users table. */
    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> findDisplayNames(Collection<UUID> userIds) {
        Map<UUID, String> names = new HashMap<>();
        if (!userIds.isEmpty()) {
            users.findAllById(userIds).forEach(user -> names.put(user.getId(), user.getDisplayName()));
        }
        return names;
    }

    /** Reads the workspace, its household settings (if it is a household) and its active members. */
    @Override
    @Transactional(readOnly = true)
    public Optional<WorkspaceInfoResponse> findWorkspace(UUID workspaceId) {
        return workspaces.findById(workspaceId).map(ws -> {
            Optional<Household> household = households.findById(workspaceId);
            var activeMembers = members.findByWorkspaceIdAndStatus(workspaceId, MemberStatus.ACTIVE,
                            Pageable.unpaged()).stream()
                    .map(m -> new InternalMembershipApi.MemberListResponse.Member(m.getUserId(), m.getRole()))
                    .toList();
            return new WorkspaceInfoResponse(ws.getId(), ws.getType().name(), ws.getName(),
                    household.map(Household::getTimezone).orElse(null),
                    household.map(Household::getDefaultCurrency).orElse(null), activeMembers);
        });
    }
}
