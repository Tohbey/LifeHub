package com.lifehub.identityworkspace.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.lifehub.identityworkspace.config.IdentityEmailProperties;
import com.lifehub.starter.web.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Sends the verification link: by SMTP when enabled, or to the log in development. */
@Component
@RequiredArgsConstructor
@Slf4j
class VerificationMailer {

    private final IdentityEmailProperties properties;
    private final ObjectProvider<JavaMailSender> mailSender;

    /** Sends the link to the address; fails with 503 when no way of sending is configured or SMTP fails. */
    void send(String email, String token) {
        String link = properties.appUrl() + "/verify-email/" + token;
        JavaMailSender sender = mailSender.getIfAvailable();
        if (properties.enabled() && sender != null) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.from());
            message.setTo(email);
            message.setSubject("LifeHub: confirm your email address");
            message.setText("Open this link while signed in to LifeHub to confirm this is your address:\n\n" + link
                    + "\n\nThe link works once and expires in " + properties.linkTtl().toHours() + " hours."
                    + " If you did not ask for this, ignore this email.");
            try {
                sender.send(message);
                return;
            } catch (MailException ex) {
                log.warn("Could not send a verification email", ex);
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "The email could not be sent. Try again later.");
            }
        }
        if (properties.logLinks()) {
            log.warn("DEVELOPMENT ONLY (lifehub.identity.email.log-links): verification link for {}: {}", email, link);
            return;
        }
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Email sending is not set up on this LifeHub.");
    }
}
