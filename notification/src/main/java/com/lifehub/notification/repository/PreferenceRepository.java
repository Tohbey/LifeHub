package com.lifehub.notification.repository;

import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.lifehub.notification.model.dto.NotificationPreferenceDTO;
import com.lifehub.notification.model.entity.NotificationType;
import com.lifehub.starter.persistence.Dates;
import com.lifehub.starter.id.Ids;

import lombok.RequiredArgsConstructor;

/** Per-user, per-type channel choices. Only choices that differ from the defaults need a row. */
@Repository
@RequiredArgsConstructor
public class PreferenceRepository {

    private final JdbcClient jdbc;
    private final Clock clock;

    /** The user's stored preferences, by type. Types without a row use the defaults. */
    public Map<NotificationType, NotificationPreferenceDTO> findByUser(UUID userId) {
        Map<NotificationType, NotificationPreferenceDTO> result = new EnumMap<>(NotificationType.class);
        jdbc.sql("SELECT type, in_app, email FROM notification_preferences WHERE user_id = :user")
                .param("user", Ids.toBytes(userId))
                .query((rs, n) -> {
                    // A type removed from the code may still have a row; skip it rather than fail.
                    for (NotificationType type : NotificationType.values()) {
                        if (type.name().equals(rs.getString(1))) {
                            result.put(type, new NotificationPreferenceDTO(type, rs.getBoolean(2), rs.getBoolean(3)));
                        }
                    }
                    return null;
                })
                .list();
        return result;
    }

    /** Stores one preference for the user. */
    public void save(UUID userId, NotificationPreferenceDTO preference) {
        jdbc.sql("""
                INSERT INTO notification_preferences (user_id, type, in_app, email, updated_at)
                VALUES (:user, :type, :inApp, :email, :now)
                ON DUPLICATE KEY UPDATE in_app = VALUES(in_app), email = VALUES(email), updated_at = VALUES(updated_at)
                """)
                .param("user", Ids.toBytes(userId))
                .param("type", preference.type().name())
                .param("inApp", preference.inApp())
                .param("email", preference.email())
                .param("now", Dates.utc(clock.instant()))
                .update();
    }
}
