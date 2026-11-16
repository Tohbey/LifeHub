package com.lifehub.identityworkspace.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.identityworkspace.model.entity.User;
import com.lifehub.identityworkspace.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Runs outside any surrounding transaction, so a user committed by a concurrent first request is
 * visible on the re-read (MySQL's repeatable-read snapshot would otherwise hide it).
 */
@Service
@RequiredArgsConstructor
class CurrentUserServiceImpl implements CurrentUserService {

    private final UserRepository userRepository;
    private final UserProvisioner provisioner;

    /** Looks the user up by Logto subject and provisions them if missing. */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public User resolve(String logtoSubject) {
        return userRepository.findByLogtoSubject(logtoSubject).orElseGet(() -> provisionOrReload(logtoSubject));
    }

    /** Provisions a new user; if a concurrent request created them first, returns that user instead. */
    private User provisionOrReload(String logtoSubject) {
        try {
            return provisioner.provision(logtoSubject);
        } catch (DataIntegrityViolationException raceLost) {
            return userRepository.findByLogtoSubject(logtoSubject).orElseThrow(() -> raceLost);
        }
    }
}
