package com.lifehub.identityworkspace.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.identityworkspace.config.IdentityEmailProperties;
import com.lifehub.identityworkspace.mapper.UserMapper;
import com.lifehub.identityworkspace.model.dto.UserDTO;
import com.lifehub.identityworkspace.model.entity.EmailVerification;
import com.lifehub.identityworkspace.model.entity.User;
import com.lifehub.identityworkspace.model.event.IdentityEvents;
import com.lifehub.identityworkspace.repository.EmailVerificationRepository;
import com.lifehub.identityworkspace.repository.UserRepository;
import com.lifehub.identityworkspace.util.InvitationTokenUtil;
import com.lifehub.starter.events.DomainEventPublisher;
import com.lifehub.starter.id.Ids;
import com.lifehub.starter.web.ApiException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class EmailVerificationServiceImpl implements EmailVerificationService {

    static final String INVALID_LINK = "This link is invalid, used or expired.";

    private final CurrentUserService currentUsers;
    private final UserRepository users;
    private final EmailVerificationRepository verifications;
    private final VerificationMailer mailer;
    private final DomainEventPublisher events;
    private final IdentityEmailProperties properties;
    private final Clock clock;

    /** Stores the hash of a new random token and mails the link; the token itself is never stored. */
    @Override
    @Transactional
    public void requestVerification(String subject) {
        User user = users.findById(currentUsers.resolve(subject).getId()).orElseThrow();
        if (user.getEmail() == null) {
            throw ApiException.badRequest("Add an email address to your profile first.");
        }
        if (user.isEmailVerified()) {
            throw ApiException.conflict("This email address is already verified.");
        }
        Instant now = clock.instant();
        verifications.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                .filter(last -> last.getCreatedAt().isAfter(now.minus(properties.resendAfter())))
                .ifPresent(last -> {
                    throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                            "A link was sent a moment ago. Wait a minute before asking for another.");
                });
        String token = InvitationTokenUtil.newToken();
        verifications.save(new EmailVerification(Ids.newId(), user.getId(), user.getEmail(),
                InvitationTokenUtil.hash(token), now, now.plus(properties.linkTtl())));
        mailer.send(user.getEmail(), token);
    }

    /** Checks the link belongs to the caller and their current email, uses it, and publishes the change. */
    @Override
    @Transactional
    public UserDTO confirm(String subject, String token) {
        User user = users.findById(currentUsers.resolve(subject).getId()).orElseThrow();
        Instant now = clock.instant();
        EmailVerification verification = verifications.findByTokenHash(InvitationTokenUtil.hash(token))
                .filter(v -> v.isUsable(now))
                .filter(v -> v.getUserId().equals(user.getId()))
                .orElseThrow(() -> ApiException.notFound(INVALID_LINK));
        if (!verification.getEmail().equals(user.getEmail())) {
            throw ApiException.conflict("Your email address changed after this link was sent. Ask for a new link.");
        }
        verification.use(now);
        user.markEmailVerified(now);
        events.publish(IdentityEvents.USER_PROFILE_UPDATED, null, user.getId(),
                new IdentityEvents.UserProfileUpdated(user.getId(), user.getDisplayName(), user.getEmail(), true,
                        List.of("emailVerified")),
                user.getId().toString());
        return UserMapper.toDto(user);
    }
}
