package com.lifehub.identityworkspace.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.identityworkspace.mapper.HouseholdMapper;
import com.lifehub.identityworkspace.mapper.InvitationMapper;
import com.lifehub.identityworkspace.mapper.MemberMapper;
import com.lifehub.identityworkspace.model.dto.ChangeRoleRequest;
import com.lifehub.identityworkspace.model.dto.CreateHouseholdRequest;
import com.lifehub.identityworkspace.model.dto.CreateInvitationRequest;
import com.lifehub.identityworkspace.model.dto.HouseholdDTO;
import com.lifehub.identityworkspace.model.dto.InvitationDTO;
import com.lifehub.identityworkspace.model.dto.MemberDTO;
import com.lifehub.identityworkspace.model.dto.PendingInvitationDTO;
import com.lifehub.identityworkspace.model.dto.UpdateHouseholdRequest;
import com.lifehub.identityworkspace.model.entity.Household;
import com.lifehub.identityworkspace.model.entity.HouseholdInvitation;
import com.lifehub.identityworkspace.model.entity.InvitationStatus;
import com.lifehub.identityworkspace.model.entity.MemberStatus;
import com.lifehub.identityworkspace.model.entity.User;
import com.lifehub.identityworkspace.model.entity.Workspace;
import com.lifehub.identityworkspace.model.entity.WorkspaceMember;
import com.lifehub.identityworkspace.model.entity.WorkspaceType;
import com.lifehub.identityworkspace.model.event.IdentityEvents;
import com.lifehub.identityworkspace.repository.HouseholdInvitationRepository;
import com.lifehub.identityworkspace.repository.HouseholdRepository;
import com.lifehub.identityworkspace.repository.UserRepository;
import com.lifehub.identityworkspace.repository.WorkspaceMemberRepository;
import com.lifehub.identityworkspace.repository.WorkspaceRepository;
import com.lifehub.identityworkspace.util.InvitationTokenUtil;
import com.lifehub.identityworkspace.util.ValidationUtil;
import com.lifehub.starter.events.DomainEventPublisher;
import com.lifehub.starter.id.Ids;
import com.lifehub.starter.membership.WorkspaceRole;
import com.lifehub.starter.web.ApiException;
import com.lifehub.starter.web.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class HouseholdServiceImpl implements HouseholdService {

    private static final String DEFAULT_CURRENCY = "GBP";
    private static final String DEFAULT_TIMEZONE = "Europe/London";

    private final CurrentUserService currentUsers;
    private final UserRepository users;
    private final WorkspaceRepository workspaces;
    private final HouseholdRepository households;
    private final WorkspaceMemberRepository members;
    private final HouseholdInvitationRepository invitations;
    private final DomainEventPublisher events;
    private final Clock clock;
    @Value("${lifehub.invitations.ttl:7d}")
    private final Duration invitationTtl;

    /**
     * Creates the workspace, household settings and owner membership, then publishes household.created.
     * Currency defaults to GBP and timezone to Europe/London.
     */
    @Override
    @Transactional
    public HouseholdDTO createHousehold(String subject, CreateHouseholdRequest request) {
        UUID actorId = currentUsers.resolve(subject).getId();
        String currency = request.defaultCurrency() == null ? DEFAULT_CURRENCY : request.defaultCurrency();
        String timezone = request.timezone() == null ? DEFAULT_TIMEZONE : ValidationUtil.requireTimezone(request.timezone());
        Instant now = clock.instant();

        Workspace workspace = workspaces.save(
                new Workspace(Ids.newId(), WorkspaceType.HOUSEHOLD, request.name().strip(), now));

        Household household = households.save(new Household(workspace.getId(), currency, timezone, actorId, now));

        members.save(new WorkspaceMember(Ids.newId(), workspace.getId(), actorId, WorkspaceRole.OWNER, now));

        events.publish(IdentityEvents.HOUSEHOLD_CREATED, workspace.getId(), actorId,
                new IdentityEvents.HouseholdCreated(workspace.getId(), workspace.getName(), actorId));

        return HouseholdMapper.toDto(workspace, household, WorkspaceRole.OWNER);
    }

    /** Checks the caller is a member, then returns the household with their role. */
    @Override
    @Transactional(readOnly = true)
    public HouseholdDTO getHousehold(String subject, UUID householdId) {
        UUID actorId = currentUsers.resolve(subject).getId();

        WorkspaceMember actor = requireMember(householdId, actorId);

        return HouseholdMapper.toDto(workspace(householdId), household(householdId), actor.getRole());
    }

    /**
     * Applies only the fields that actually change and publishes household.updated listing them.
     * Publishes nothing when nothing changed.
     */
    @Override
    @Transactional
    public HouseholdDTO updateHousehold(String subject, UUID householdId, UpdateHouseholdRequest request) {
        UUID actorId = currentUsers.resolve(subject).getId();
        WorkspaceMember actor = requireMember(householdId, actorId);
        if (!actor.getRole().isAtLeast(WorkspaceRole.ADMIN)) {
            throw ApiException.forbidden("Only owners and admins can change household settings.");
        }
        Workspace workspace = workspace(householdId);
        Household household = household(householdId);
        String timezone = request.timezone() == null ? null : ValidationUtil.requireTimezone(request.timezone());
        Instant now = clock.instant();

        List<String> changed = new ArrayList<>();
        if (request.name() != null && !request.name().strip().equals(workspace.getName())) {
            workspace.rename(request.name().strip(), now);
            changed.add("name");
        }
        if (request.defaultCurrency() != null && !request.defaultCurrency().equals(household.getDefaultCurrency())) {
            changed.add("defaultCurrency");
        }
        if (timezone != null && !timezone.equals(household.getTimezone())) {
            changed.add("timezone");
        }
        if (!changed.isEmpty()) {
            household.changeSettings(request.defaultCurrency(), timezone, now);
            events.publish(IdentityEvents.HOUSEHOLD_UPDATED, householdId, actorId,
                    new IdentityEvents.HouseholdUpdated(householdId, workspace.getName(), changed));
        }
        return HouseholdMapper.toDto(workspace, household, actor.getRole());
    }

    /**
     * Checks the caller may invite at this role, stores the invitation with a hashed token,
     * publishes member.invited and returns the plain token once.
     */
    @Override
    @Transactional
    public InvitationDTO inviteMember(String subject, UUID householdId, CreateInvitationRequest request) {
        UUID actorId = currentUsers.resolve(subject).getId();

        WorkspaceMember actor = requireMemberForUpdate(householdId, actorId);

        check(MemberPolicy.invite(actor.getRole(), request.role()));

        String email = request.email().strip().toLowerCase();
        String token = InvitationTokenUtil.newToken();
        Instant now = clock.instant();

        HouseholdInvitation invitation = invitations.save(new HouseholdInvitation(Ids.newId(), householdId, email,
                request.role(), InvitationTokenUtil.hash(token), actorId, now, now.plus(invitationTtl)));

        // Only a verified email identifies a user: anyone can type any address into their profile.
        UUID invitedUserId = users.findFirstByEmailAndEmailVerifiedAtIsNotNull(email).map(User::getId).orElse(null);

        events.publish(IdentityEvents.MEMBER_INVITED, householdId, actorId, new IdentityEvents.MemberInvited(
                householdId, invitation.getId(), email, invitedUserId, request.role(), invitation.getExpiresAt()));

        return InvitationMapper.toDto(invitation, token);
    }

    /** Requires owner or admin, then pages through invitations that can still be accepted. */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<PendingInvitationDTO> getInvitations(String subject, UUID householdId, Pageable pageable) {
        UUID actorId = currentUsers.resolve(subject).getId();
        requireAdmin(requireMember(householdId, actorId), "Only owners and admins can see invitations.");
        return PageResponse.of(invitations.findByHouseholdIdAndStatusAndExpiresAtAfter(householdId,
                InvitationStatus.PENDING, clock.instant(), pageable), InvitationMapper::toPendingDto);
    }

    /** Requires owner or admin; only a pending invitation can be revoked. Publishes member.invitation.revoked. */
    @Override
    @Transactional
    public void revokeInvitation(String subject, UUID householdId, UUID invitationId) {
        UUID actorId = currentUsers.resolve(subject).getId();
        requireAdmin(requireMemberForUpdate(householdId, actorId), "Only owners and admins can revoke invitations.");
        HouseholdInvitation invitation = invitations.findByIdAndHouseholdId(invitationId, householdId)
                .orElseThrow(() -> ApiException.notFound("Invitation not found."));
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw ApiException.conflict("This invitation was already " + invitation.getStatus().name().toLowerCase() + ".");
        }
        invitation.revoke();
        events.publish(IdentityEvents.INVITATION_REVOKED, householdId, actorId,
                new IdentityEvents.InvitationRevoked(householdId, invitationId, invitation.getEmail()));
    }

    /** Checks the caller is a member, then pages through active members with their user details. */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<MemberDTO> getMembers(String subject, UUID householdId, Pageable pageable) {
        UUID actorId = currentUsers.resolve(subject).getId();

        requireMember(householdId, actorId);

        Page<WorkspaceMember> page = members.findByWorkspaceIdAndStatus(householdId, MemberStatus.ACTIVE, pageable);

        Map<UUID, User> byId = users.findAllById(page.map(WorkspaceMember::getUserId).getContent()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return PageResponse.of(page, m -> MemberMapper.toDto(m, byId.get(m.getUserId())));
    }

    /**
     * Applies the role change and publishes member.role.changed. When the new role is OWNER, the
     * current owner becomes admin first and a second event is published for that.
     */
    @Override
    @Transactional
    public MemberDTO changeMemberRole(String subject, UUID householdId, UUID userId, ChangeRoleRequest request) {
        UUID actorId = currentUsers.resolve(subject).getId();

        WorkspaceMember actor = requireMemberForUpdate(householdId, actorId);

        WorkspaceRole newRole = request.role();

        check(MemberPolicy.changeRole(actor.getRole(), actorId.equals(userId), newRole));

        WorkspaceMember target = requireTarget(householdId, userId);

        WorkspaceRole previous = target.getRole();
        if (previous != newRole) {
            if (newRole == WorkspaceRole.OWNER) {
                // Handing over ownership: the current owner steps down to admin, so there is always one owner.
                actor.changeRole(WorkspaceRole.ADMIN);
                events.publish(IdentityEvents.MEMBER_ROLE_CHANGED, householdId, actorId,
                        new IdentityEvents.MemberRoleChanged(householdId, actorId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN));
            }
            target.changeRole(newRole);
            events.publish(IdentityEvents.MEMBER_ROLE_CHANGED, householdId, actorId,
                    new IdentityEvents.MemberRoleChanged(householdId, userId, previous, newRole));
        }
        return MemberMapper.toDto(target, users.findById(userId).orElseThrow());
    }

    /**
     * Marks the member as removed and publishes member.removed with reason LEFT or REMOVED.
     * The owner cannot be removed and cannot leave.
     */
    @Override
    @Transactional
    public void removeMember(String subject, UUID householdId, UUID userId) {
        UUID actorId = currentUsers.resolve(subject).getId();

        WorkspaceMember actor = requireMemberForUpdate(householdId, actorId);

        boolean self = actorId.equals(userId);

        WorkspaceMember target = self ? actor : requireTarget(householdId, userId);

        check(MemberPolicy.remove(actor.getRole(), self, target.getRole()));

        target.remove(clock.instant());

        events.publish(IdentityEvents.MEMBER_REMOVED, householdId, actorId, new IdentityEvents.MemberRemoved(
                householdId, userId, self ? IdentityEvents.MemberRemoved.LEFT : IdentityEvents.MemberRemoved.REMOVED));
    }

    /**
     * Returns the caller's active membership of the household, or 404 if the household does not exist,
     * is a personal workspace, or the caller is not a member.
     */
    private WorkspaceMember requireMember(UUID householdId, UUID userId) {
        if (!households.existsById(householdId)) {
            throw ApiException.notFound("Household not found.");
        }

        return members.findByWorkspaceIdAndUserId(householdId, userId)
                .filter(WorkspaceMember::isActive)
                .orElseThrow(() -> ApiException.notFound("Household not found."));
    }

    /**
     * Like {@link #requireMember}, but first locks the household row until the transaction ends. Invitations,
     * role changes and removals in one household then run one at a time and each sees the others' results,
     * so two concurrent ownership handovers cannot leave two owners.
     */
    private WorkspaceMember requireMemberForUpdate(UUID householdId, UUID userId) {
        if (households.findByIdForUpdate(householdId).isEmpty()) {
            throw ApiException.notFound("Household not found.");
        }

        return members.findByWorkspaceIdAndUserId(householdId, userId)
                .filter(WorkspaceMember::isActive)
                .orElseThrow(() -> ApiException.notFound("Household not found."));
    }

    /** Returns the active membership being acted on, or 404 if that user is not an active member. */
    private WorkspaceMember requireTarget(UUID householdId, UUID userId) {
        return members.findByWorkspaceIdAndUserId(householdId, userId)
                .filter(WorkspaceMember::isActive)
                .orElseThrow(() -> ApiException.notFound("Member not found."));
    }

    /** Loads the household's workspace, which holds its name. */
    private Workspace workspace(UUID householdId) {
        return workspaces.findById(householdId).orElseThrow();
    }

    /** Loads the household's settings. */
    private Household household(UUID householdId) {
        return households.findById(householdId).orElseThrow();
    }

    /** Fails with 403 unless the member is an owner or admin. */
    private static void requireAdmin(WorkspaceMember member, String reason) {
        if (!member.getRole().isAtLeast(WorkspaceRole.ADMIN)) {
            throw ApiException.forbidden(reason);
        }
    }

    /** Turns a denied policy decision into a 403 with the policy's reason. */
    private static void check(MemberPolicy.Decision decision) {
        if (!decision.allowed()) {
            throw ApiException.forbidden(decision.reason());
        }
    }
}
