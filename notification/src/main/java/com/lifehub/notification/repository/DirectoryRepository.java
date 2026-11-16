package com.lifehub.notification.repository;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.lifehub.notification.service.Directory;
import com.lifehub.starter.persistence.Dates;
import com.lifehub.starter.id.Ids;

import lombok.RequiredArgsConstructor;

/**
 * This service's copy of workspaces, memberships and people, kept up to date from identity events
 * (event-carried state). Writes are upserts, so replaying an event is harmless.
 */
@Repository
@RequiredArgsConstructor
public class DirectoryRepository implements Directory {

    private final JdbcClient jdbc;
    private final Clock clock;

    /** A person notifications can be addressed to. The email is unverified profile data, or null. */
    public record Recipient(UUID userId, String displayName, String email) {
    }

    /** Records or renames a workspace. */
    public void saveWorkspace(UUID workspaceId, String name) {
        jdbc.sql("""
                INSERT INTO directory_workspaces (id, name, updated_at) VALUES (:id, :name, :now)
                ON DUPLICATE KEY UPDATE name = VALUES(name), updated_at = VALUES(updated_at)
                """)
                .param("id", Ids.toBytes(workspaceId))
                .param("name", name)
                .param("now", Dates.utc(clock.instant()))
                .update();
    }

    /** Records a membership as active with this role, or marks it inactive when {@code role} is null. */
    public void saveMember(UUID workspaceId, UUID userId, String role) {
        if (role == null) {
            jdbc.sql("UPDATE directory_members SET active = FALSE, updated_at = :now WHERE workspace_id = :ws AND user_id = :user")
                    .param("ws", Ids.toBytes(workspaceId))
                    .param("user", Ids.toBytes(userId))
                    .param("now", Dates.utc(clock.instant()))
                    .update();
            return;
        }
        jdbc.sql("""
                INSERT INTO directory_members (workspace_id, user_id, role, active, updated_at)
                VALUES (:ws, :user, :role, TRUE, :now)
                ON DUPLICATE KEY UPDATE role = VALUES(role), active = TRUE, updated_at = VALUES(updated_at)
                """)
                .param("ws", Ids.toBytes(workspaceId))
                .param("user", Ids.toBytes(userId))
                .param("role", role)
                .param("now", Dates.utc(clock.instant()))
                .update();
    }

    /** Changes the role of an existing active membership; does nothing if the member is unknown or inactive. */
    public void changeRole(UUID workspaceId, UUID userId, String role) {
        jdbc.sql("UPDATE directory_members SET role = :role, updated_at = :now WHERE workspace_id = :ws AND user_id = :user AND active")
                .param("ws", Ids.toBytes(workspaceId))
                .param("user", Ids.toBytes(userId))
                .param("role", role)
                .param("now", Dates.utc(clock.instant()))
                .update();
    }

    /** Records a new user with their display name. Never overwrites a profile that is already known. */
    public void saveNewRecipient(UUID userId, String displayName) {
        jdbc.sql("INSERT IGNORE INTO directory_recipients (user_id, display_name, email, updated_at) VALUES (:user, :name, NULL, :now)")
                .param("user", Ids.toBytes(userId))
                .param("name", displayName)
                .param("now", Dates.utc(clock.instant()))
                .update();
    }

    /** Stores a user's current display name and email. */
    public void saveRecipient(UUID userId, String displayName, String email) {
        jdbc.sql("""
                INSERT INTO directory_recipients (user_id, display_name, email, updated_at) VALUES (:user, :name, :email, :now)
                ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), email = VALUES(email), updated_at = VALUES(updated_at)
                """)
                .param("user", Ids.toBytes(userId))
                .param("name", displayName)
                .param("email", email)
                .param("now", Dates.utc(clock.instant()))
                .update();
    }

    /** The stored profile of a user, if known. */
    public Optional<Recipient> recipient(UUID userId) {
        return jdbc.sql("SELECT display_name, email FROM directory_recipients WHERE user_id = :user")
                .param("user", Ids.toBytes(userId))
                .query((rs, n) -> new Recipient(userId, rs.getString(1), rs.getString(2)))
                .optional();
    }

    /** {@inheritDoc} */
    @Override
    public String workspaceName(UUID workspaceId) {
        if (workspaceId == null) {
            return "your workspace";
        }
        return jdbc.sql("SELECT name FROM directory_workspaces WHERE id = :id")
                .param("id", Ids.toBytes(workspaceId))
                .query(String.class)
                .optional()
                .orElse("your household");
    }

    /** {@inheritDoc} */
    @Override
    public Map<UUID, String> activeMembers(UUID workspaceId) {
        Map<UUID, String> members = new LinkedHashMap<>();
        if (workspaceId == null) {
            return members;
        }
        jdbc.sql("SELECT user_id, role FROM directory_members WHERE workspace_id = :ws AND active ORDER BY updated_at")
                .param("ws", Ids.toBytes(workspaceId))
                .query((rs, n) -> members.put(Ids.fromBytes(rs.getBytes(1)), rs.getString(2)))
                .list();
        return members;
    }

    /** {@inheritDoc} */
    @Override
    public String displayName(UUID userId) {
        if (userId == null) {
            return "Someone";
        }
        return recipient(userId).map(Recipient::displayName).filter(n -> n != null && !n.isBlank()).orElse("Someone");
    }
}
