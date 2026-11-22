package com.lifehub.identityworkspace.mapper;

import com.lifehub.identityworkspace.model.dto.InvitationDTO;
import com.lifehub.identityworkspace.model.dto.PendingInvitationDTO;
import com.lifehub.identityworkspace.model.entity.HouseholdInvitation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InvitationMapper {

    /** {@code token} is only known at creation; it is never stored. */
    public static InvitationDTO toDto(HouseholdInvitation invitation, String token) {
        return new InvitationDTO(invitation.getId(), invitation.getEmail(), invitation.getRole(),
                invitation.getExpiresAt(), token);
    }

    /** Maps a pending invitation for the list, without any token. */
    public static PendingInvitationDTO toPendingDto(HouseholdInvitation invitation) {
        return new PendingInvitationDTO(invitation.getId(), invitation.getEmail(), invitation.getRole(),
                invitation.getInvitedByUserId(), invitation.getCreatedAt(), invitation.getExpiresAt());
    }
}
