package com.lifehub.notification.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.lifehub.notification.config.NotificationProperties;
import com.lifehub.notification.repository.DeliveryAttemptRepository;
import com.lifehub.notification.repository.DeliveryAttemptRepository.PendingEmail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends queued notification emails over SMTP, retrying failures with exponential backoff (1, 2, 4, 8
 * minutes...) until {@code lifehub.notification.max-email-attempts}, then marking them failed. Emails are
 * plain text, so user-entered titles cannot inject markup.
 */
@Component
@ConditionalOnProperty(name = "lifehub.notification.email-enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
class EmailDeliveryWorker {

    static final int BATCH_SIZE = 10;

    private final DeliveryAttemptRepository deliveries;
    private final JavaMailSender mailSender;
    private final NotificationProperties properties;
    private final TransactionTemplate transactions;
    private final Clock clock;

    /** Runs every 15 seconds: sends one batch of due emails. */
    @Scheduled(fixedDelay = 15_000)
    public void sendDueEmails() {
        transactions.executeWithoutResult(status -> deliveries.claimDueEmails(BATCH_SIZE).forEach(this::send));
    }

    /** Sends one email and records the outcome. */
    void send(PendingEmail email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.mailFrom());
        message.setTo(email.to());
        message.setSubject("LifeHub: " + oneLine(email.title()));
        message.setText(email.body() + (email.link() == null ? "" : "\n\n" + properties.appUrl() + email.link())
                + "\n\nYou can turn these emails off in LifeHub under notification settings.");
        try {
            mailSender.send(message);
            deliveries.markSent(email.id());
        } catch (MailException ex) {
            int attempt = email.attempts() + 1;
            Instant retryAt = attempt >= properties.maxEmailAttempts() ? null
                    : clock.instant().plus(Duration.ofMinutes(1L << Math.min(attempt - 1, 10)));
            log.warn("Email delivery {} failed (attempt {}){}", email.id(), attempt, retryAt == null ? ", giving up" : "");
            deliveries.markFailed(email.id(), String.valueOf(ex.getMessage()), retryAt);
        }
    }

    /** Removes line breaks, so a title can never add mail headers. */
    static String oneLine(String text) {
        return text.replaceAll("[\\r\\n]+", " ");
    }
}
