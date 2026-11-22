package com.lifehub.identityworkspace.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How verification emails are sent.
 *
 * @param enabled     send email over SMTP (spring.mail.*); off until SMTP is configured
 * @param logLinks    DEVELOPMENT ONLY: write the verification link to the log instead of sending it, for testing
 *                    without a mail server. Anyone who can read the log can then verify any address; never turn it
 *                    on in production
 * @param appUrl      the public LifeHub URL the link points at
 * @param from        sender address
 * @param linkTtl     how long a link works
 * @param resendAfter how long a user waits before another link is sent
 */
@ConfigurationProperties("lifehub.identity.email")
public record IdentityEmailProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("false") boolean logLinks,
        @DefaultValue("http://localhost:8089") String appUrl,
        @DefaultValue("lifehub@localhost") String from,
        @DefaultValue("24h") Duration linkTtl,
        @DefaultValue("60s") Duration resendAfter) {
}
