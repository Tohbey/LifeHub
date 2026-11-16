package com.lifehub.identityworkspace.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.identityworkspace.mapper.WorkspaceMapper;
import com.lifehub.identityworkspace.model.dto.WorkspaceDTO;
import com.lifehub.identityworkspace.model.entity.HouseholdInvitation;
import com.lifehub.identityworkspace.model.entity.WorkspaceMember;
import com.lifehub.identityworkspace.model.event.IdentityEvents;
import com.lifehub.identityworkspace.repository.HouseholdInvitationRepository;
import com.lifehub.identityworkspace.repository.WorkspaceMemberRepository;
import com.lifehub.identityworkspace.repository.WorkspaceRepository;
import com.lifehub.identityworkspace.util.InvitationTokenUtil;
import com.lifehub.starter.events.DomainEventPublisher;
import com.lifehub.starter.id.Ids;
import com.lifehub.starter.web.ApiException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class InvitationServiceImpl implements InvitationService {

    private final CurrentUserService currentUsers;
    private final HouseholdInvitationRepository invitations;
    private final WorkspaceMemberRepository members;
    private final WorkspaceRepository workspaces;
    private final DomainEventPublisher events;
    private final Clock clock;

    /**
     * Finds the invitation by token hash, adds the caller (or reactivates a former member) with the
     * invited role, marks the invitation accepted and publishes member.added.
     */
    @Override
    @Transactional
    public WorkspaceDTO acceptInvitation(String subject, String token) {
        UUID userId = currentUsers.resolve(subject).getId();
        Instant now = clock.instant();
        HouseholdInvitation invitation = invitations.findByTokenHash(InvitationTokenUtil.hash(token))
                .filter(i -> i.isUsable(now))
                .orElseThrow(() -> ApiException.notFound("This invitation is invalid, used or expired."));
        UUID householdId = invitation.getHouseholdId();

        WorkspaceMember member = members.findByWorkspaceIdAndUserId(householdId, userId)
                .map(existing -> {
                    if (existing.isActive()) {
                        throw ApiException.conflict("You are already a member of this household.");
                    }
                    existing.rejoin(invitation.getRole(), now);
                    return existing;
                })
                .orElseGet(() -> members.save(
                        new WorkspaceMember(Ids.newId(), householdId, userId, invitation.getRole(), now)));
        invitation.accept(userId, now);

        events.publish(IdentityEvents.MEMBER_ADDED, householdId, userId,
                new IdentityEvents.MemberAdded(householdId, userId, member.getRole(), invitation.getId()));
        return WorkspaceMapper.toDto(workspaces.findById(householdId).orElseThrow(), member);
    }
}
