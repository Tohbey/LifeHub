package com.lifehub.notification.service;

import java.util.List;
import java.util.UUID;

import com.lifehub.notification.model.dto.NotificationPreferenceDTO;
import com.lifehub.notification.model.dto.UpdatePreferencesRequest;

/** Which notifications a user wants, and over which channels. */
public interface PreferenceService {

    /** Every notification type with the user's effective setting (stored or default). */
    List<NotificationPreferenceDTO> getPreferences(UUID userId);

    /** Stores the given preferences and returns the full, effective list. */
    List<NotificationPreferenceDTO> updatePreferences(UUID userId, UpdatePreferencesRequest request);
}
