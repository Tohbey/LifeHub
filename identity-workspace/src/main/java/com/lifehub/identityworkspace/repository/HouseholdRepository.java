package com.lifehub.identityworkspace.repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.lifehub.identityworkspace.model.entity.Household;
import org.springframework.stereotype.Repository;

@Repository
public interface HouseholdRepository extends JpaRepository<Household, UUID> {

    @Query("select h from Household h where h.id = :id")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    /** Loads a household and locks its row until the transaction ends, so membership changes run one at a time. */
    Optional<Household> findByIdForUpdate(UUID id);
}
