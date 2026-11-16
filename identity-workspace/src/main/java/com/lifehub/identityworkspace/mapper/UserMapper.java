package com.lifehub.identityworkspace.mapper;

import com.lifehub.identityworkspace.model.dto.UserDTO;
import com.lifehub.identityworkspace.model.entity.User;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserMapper {

    /** Maps a user entity to the profile DTO. */
    public static UserDTO toDto(User user) {
        return new UserDTO(user.getId(), user.getDisplayName(), user.getEmail(), user.getCreatedAt());
    }
}
