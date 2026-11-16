package com.lifehub.notification.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.lifehub.notification.model.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** Pages through a user's notifications. */
    Page<Notification> findByUserId(UUID userId, Pageable pageable);

    /** Pages through a user's unread notifications. */
    Page<Notification> findByUserIdAndReadAtIsNull(UUID userId, Pageable pageable);

    /** Finds one notification, only if it belongs to this user. */
    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    /** Counts a user's unread notifications. */
    long countByUserIdAndReadAtIsNull(UUID userId);

    /** The newest notification with this group key for this user created after the given time, for grouping. */
    Optional<Notification> findFirstByUserIdAndGroupKeyAndCreatedAtAfterOrderByCreatedAtDesc(UUID userId,
            String groupKey, Instant after);

    @Modifying
    @Query("update Notification n set n.readAt = :now, n.updatedAt = :now where n.userId = :userId and n.readAt is null")
    /** Marks every unread notification of the user as read. Returns how many changed. */
    int markAllRead(UUID userId, Instant now);
}
