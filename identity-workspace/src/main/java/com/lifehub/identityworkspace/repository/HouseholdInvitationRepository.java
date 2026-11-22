package com.lifehub.identityworkspace.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.lifehub.identityworkspace.model.entity.HouseholdInvitation;
import com.lifehub.identityworkspace.model.entity.InvitationStatus;
import org.springframework.stereotype.Repository;

@Repository
public interface HouseholdInvitationRepository extends JpaRepository<HouseholdInvitation, UUID> {

    /**
     * Finds an invitation by the SHA-256 hash of its token and locks its row until the transaction ends,
     * so two people using the same link at once cannot both join (single use).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<HouseholdInvitation> findByTokenHash(String tokenHash);

    /** Pages through a household's invitations with this status that expire after the given time. */
    Page<HouseholdInvitation> findByHouseholdIdAndStatusAndExpiresAtAfter(UUID householdId, InvitationStatus status,
            Instant after, Pageable pageable);

    /** Finds an invitation only if it belongs to this household. */
    Optional<HouseholdInvitation> findByIdAndHouseholdId(UUID id, UUID householdId);
}
