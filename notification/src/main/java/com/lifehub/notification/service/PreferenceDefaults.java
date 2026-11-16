package com.lifehub.notification.service;

import com.lifehub.notification.model.dto.NotificationPreferenceDTO;
import com.lifehub.notification.model.entity.NotificationType;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** What a user gets before they change anything: everything in the app, no email. Email is opt-in. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PreferenceDefaults {

    /** The default preference for this type. */
    public static NotificationPreferenceDTO of(NotificationType type) {
        return new NotificationPreferenceDTO(type, true, false);
    }
}
