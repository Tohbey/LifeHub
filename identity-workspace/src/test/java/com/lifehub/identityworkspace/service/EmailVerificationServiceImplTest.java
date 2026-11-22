package com.lifehub.identityworkspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.lifehub.identityworkspace.config.IdentityEmailProperties;
import com.lifehub.identityworkspace.model.entity.EmailVerification;
import com.lifehub.identityworkspace.model.entity.User;
import com.lifehub.identityworkspace.model.event.IdentityEvents;
import com.lifehub.identityworkspace.repository.EmailVerificationRepository;
import com.lifehub.identityworkspace.repository.UserRepository;
import com.lifehub.identityworkspace.util.InvitationTokenUtil;
import com.lifehub.starter.events.DomainEventPublisher;
import com.lifehub.starter.web.ApiException;

class EmailVerificationServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");

    private final CurrentUserService currentUsers = mock(CurrentUserService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final EmailVerificationRepository verifications = mock(EmailVerificationRepository.class);
    private final VerificationMailer mailer = mock(VerificationMailer.class);
    private final DomainEventPublisher events = mock(DomainEventPublisher.class);
    private final EmailVerificationServiceImpl service = new EmailVerificationServiceImpl(currentUsers, users,
            verifications, mailer, events,
            new IdentityEmailProperties(false, true, "https://lifehub.test", "x@test", Duration.ofHours(24), Duration.ofSeconds(60)),
            Clock.fixed(NOW, ZoneOffset.UTC));

    private User alice;

    @BeforeEach
    void signedIn() {
        alice = new User(UUID.randomUUID(), "logto-alice", NOW.minusSeconds(86400));
        alice.updateProfile("Alice", "Alice@Example.com", NOW.minusSeconds(3600));
        when(currentUsers.resolve("alice")).thenReturn(alice);
        when(users.findById(alice.getId())).thenReturn(Optional.of(alice));
        when(verifications.findFirstByUserIdOrderByCreatedAtDesc(alice.getId())).thenReturn(Optional.empty());
    }

    @Test
    void sendsALinkAndStoresOnlyTheTokensHash() {
        service.requestVerification("alice");

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(mailer).send(eq("alice@example.com"), token.capture());
        ArgumentCaptor<EmailVerification> saved = ArgumentCaptor.forClass(EmailVerification.class);
        verify(verifications).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(InvitationTokenUtil.hash(token.getValue()));
        assertThat(saved.getValue().getTokenHash()).isNotEqualTo(token.getValue());
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
    }

    @Test
    void refusesWithoutAnEmailWhenAlreadyVerifiedAndWhenAskedTooOften() {
        EmailVerification recent = new EmailVerification(UUID.randomUUID(), alice.getId(), "alice@example.com", "h",
                NOW.minusSeconds(10), NOW.plusSeconds(3600));
        when(verifications.findFirstByUserIdOrderByCreatedAtDesc(alice.getId())).thenReturn(Optional.of(recent));
        assertThatThrownBy(() -> service.requestVerification("alice"))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.status().value()).isEqualTo(429));

        alice.markEmailVerified(NOW);
        assertThatThrownBy(() -> service.requestVerification("alice")).hasMessageContaining("already verified");

        alice.updateProfile(null, "", NOW);
        assertThatThrownBy(() -> service.requestVerification("alice")).hasMessageContaining("Add an email");
        verify(mailer, never()).send(anyString(), anyString());
    }

    @Test
    void aValidLinkVerifiesTheEmailOnceAndPublishesIt() {
        when(verifications.findByTokenHash(InvitationTokenUtil.hash("tok"))).thenReturn(Optional.of(
                new EmailVerification(UUID.randomUUID(), alice.getId(), "alice@example.com",
                        InvitationTokenUtil.hash("tok"), NOW.minusSeconds(60), NOW.plusSeconds(3600))));

        assertThat(service.confirm("alice", "tok").emailVerified()).isTrue();

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(events).publish(eq(IdentityEvents.USER_PROFILE_UPDATED), eq(null), eq(alice.getId()), payload.capture(),
                eq(alice.getId().toString()));
        assertThat(((IdentityEvents.UserProfileUpdated) payload.getValue()).emailVerified()).isTrue();
    }

    @Test
    void someoneElsesLinkOrAChangedEmailDoesNotVerify() {
        when(verifications.findByTokenHash(InvitationTokenUtil.hash("bob-token"))).thenReturn(Optional.of(
                new EmailVerification(UUID.randomUUID(), UUID.randomUUID(), "alice@example.com", "h",
                        NOW.minusSeconds(60), NOW.plusSeconds(3600))));
        assertThatThrownBy(() -> service.confirm("alice", "bob-token")).hasMessage(EmailVerificationServiceImpl.INVALID_LINK);

        when(verifications.findByTokenHash(InvitationTokenUtil.hash("old"))).thenReturn(Optional.of(
                new EmailVerification(UUID.randomUUID(), alice.getId(), "old@example.com", "h",
                        NOW.minusSeconds(60), NOW.plusSeconds(3600))));
        assertThatThrownBy(() -> service.confirm("alice", "old")).hasMessageContaining("changed");
        assertThat(alice.isEmailVerified()).isFalse();
        verify(events, never()).publish(any(), any(), any(), any(), anyString());
    }

    @Test
    void changingTheEmailMakesItUnverifiedAgain() {
        alice.markEmailVerified(NOW);
        alice.updateProfile(null, "alice@example.com", NOW);
        assertThat(alice.isEmailVerified()).as("same address").isTrue();
        alice.updateProfile(null, "new@example.com", NOW);
        assertThat(alice.isEmailVerified()).isFalse();
    }
}
