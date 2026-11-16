package com.lifehub.identityworkspace.service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.identityworkspace.model.entity.MemberStatus;
import com.lifehub.identityworkspace.model.entity.WorkspaceMember;
import com.lifehub.identityworkspace.repository.WorkspaceMemberRepository;
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
}
