package com.lifehub.identityworkspace.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.lifehub.identityworkspace.model.dto.ChangeRoleRequest;
import com.lifehub.identityworkspace.model.dto.CreateHouseholdRequest;
import com.lifehub.identityworkspace.model.dto.CreateInvitationRequest;
import com.lifehub.identityworkspace.model.dto.HouseholdDTO;
import com.lifehub.identityworkspace.model.dto.InvitationDTO;
import com.lifehub.identityworkspace.model.dto.MemberDTO;
import com.lifehub.identityworkspace.model.dto.PendingInvitationDTO;
import com.lifehub.identityworkspace.model.dto.UpdateHouseholdRequest;
import com.lifehub.starter.web.PageResponse;

/**
 * Household use cases. Every method checks the caller's membership first. A personal workspace, a missing
 * household and a household the caller does not belong to all look the same: 404. Role rules are in
 * {@link MemberPolicy}.
 */
public interface HouseholdService {

    /** Creates a household and its shared workspace, with the caller as owner. */
    HouseholdDTO createHousehold(String subject, CreateHouseholdRequest request);

    /** Returns a household the caller belongs to, with their role in it. */
    HouseholdDTO getHousehold(String subject, UUID householdId);

    /** Changes a household's settings. Owners and admins only. */
    HouseholdDTO updateHousehold(String subject, UUID householdId, UpdateHouseholdRequest request);

    /** The returned DTO carries the invite token, which is not available again. */
    InvitationDTO inviteMember(String subject, UUID householdId, CreateInvitationRequest request);

    /** Lists the household's pending, unexpired invitations, newest first. Owners and admins only. */
    PageResponse<PendingInvitationDTO> getInvitations(String subject, UUID householdId, Pageable pageable);

    /** Withdraws a pending invitation so its link stops working. Owners and admins only. */
    void revokeInvitation(String subject, UUID householdId, UUID invitationId);

    /** Lists the active members of a household the caller belongs to. */
    PageResponse<MemberDTO> getMembers(String subject, UUID householdId, Pageable pageable);

    /** Changes a member's role. Owner only; setting OWNER hands over ownership. */
    MemberDTO changeMemberRole(String subject, UUID householdId, UUID userId, ChangeRoleRequest request);

    /** Removing yourself means leaving the household. */
    void removeMember(String subject, UUID householdId, UUID userId);
}
