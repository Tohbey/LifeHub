package com.lifehub.identityworkspace.repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import com.lifehub.identityworkspace.model.entity.EmailVerification;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    /** Finds a link by its token hash and locks it, so two clicks cannot both use it. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerification> findByTokenHash(String tokenHash);

    /** The user's most recent link, to limit how often links are sent. */
    Optional<EmailVerification> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}
