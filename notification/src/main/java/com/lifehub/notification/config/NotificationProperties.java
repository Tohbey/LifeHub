package com.lifehub.notification.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param appUrl          public URL of LifeHub, used for links in emails
 * @param emailEnabled    send email at all; off until SMTP is configured
 * @param mailFrom        sender address of notification emails
 * @param maxEmailAttempts attempts before an email delivery is marked failed
 * @param groupingWindow  shopping updates for the same list within this window become one notification
 * @param maxStreamsPerUser open live streams allowed per user (tabs and devices)
 */
@ConfigurationProperties("lifehub.notification")
public record NotificationProperties(
        @DefaultValue("http://localhost:8089") String appUrl,
        @DefaultValue("false") boolean emailEnabled,
        @DefaultValue("lifehub@localhost") String mailFrom,
        @DefaultValue("5") int maxEmailAttempts,
        @DefaultValue("10m") Duration groupingWindow,
        @DefaultValue("5") int maxStreamsPerUser) {
}
