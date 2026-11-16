package com.lifehub.identityworkspace.repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.lifehub.identityworkspace.model.entity.HouseholdInvitation;
import org.springframework.stereotype.Repository;

@Repository
public interface HouseholdInvitationRepository extends JpaRepository<HouseholdInvitation, UUID> {

    /**
     * Finds an invitation by the SHA-256 hash of its token and locks its row until the transaction ends,
     * so two people using the same link at once cannot both join (single use).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<HouseholdInvitation> findByTokenHash(String tokenHash);
}
