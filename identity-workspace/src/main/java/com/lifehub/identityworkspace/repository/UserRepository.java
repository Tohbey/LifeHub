package com.lifehub.identityworkspace.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lifehub.identityworkspace.model.entity.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /** Finds the user linked to a Logto subject. */
    Optional<User> findByLogtoSubject(String logtoSubject);

    /** Finds a user by their (lowercased) email, used to tell an existing user about an invitation. */
    Optional<User> findFirstByEmail(String email);
}
