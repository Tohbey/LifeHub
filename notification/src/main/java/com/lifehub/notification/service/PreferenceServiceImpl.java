package com.lifehub.notification.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.notification.model.dto.NotificationPreferenceDTO;
import com.lifehub.notification.model.dto.UpdatePreferencesRequest;
import com.lifehub.notification.model.entity.NotificationType;
import com.lifehub.notification.repository.PreferenceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class PreferenceServiceImpl implements PreferenceService {

    private final PreferenceRepository preferences;

    /** Merges stored preferences over the defaults, in the enum's order. */
    @Override
    @Transactional(readOnly = true)
    public List<NotificationPreferenceDTO> getPreferences(UUID userId) {
        Map<NotificationType, NotificationPreferenceDTO> stored = preferences.findByUser(userId);
        return Arrays.stream(NotificationType.values())
                .map(type -> stored.getOrDefault(type, PreferenceDefaults.of(type)))
                .toList();
    }

    /** Saves each given preference, then returns the full list. */
    @Override
    @Transactional
    public List<NotificationPreferenceDTO> updatePreferences(UUID userId, UpdatePreferencesRequest request) {
        request.preferences().forEach(preference -> preferences.save(userId, preference));
        return getPreferences(userId);
    }
}
