package com.lifehub.identityworkspace.mapper;

import com.lifehub.identityworkspace.model.dto.MemberDTO;
import com.lifehub.identityworkspace.model.entity.User;
import com.lifehub.identityworkspace.model.entity.WorkspaceMember;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MemberMapper {

    /** Maps a membership and its user to a member DTO. */
    public static MemberDTO toDto(WorkspaceMember member, User user) {
        return new MemberDTO(member.getUserId(), user.getDisplayName(), member.getRole(), member.getJoinedAt());
    }
}
