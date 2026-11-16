package com.lifehub.identityworkspace.mapper;

import com.lifehub.identityworkspace.model.dto.InvitationDTO;
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
}
