package com.lifehub.starter.membership;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param baseUrl  identity-workspace's base URL on the Docker network, for example {@code http://identity-workspace:8080}
 * @param cacheTtl how long a positive answer is reused; a removed member keeps access for up to this long
 * @param timeout  connect and read timeout for one lookup
 * @param defaultTimezone the timezone used for a personal workspace, or when identity-workspace cannot answer
 */
@ConfigurationProperties("lifehub.membership")
public record MembershipProperties(
        String baseUrl,
        @DefaultValue("30s") Duration cacheTtl,
        @DefaultValue("2s") Duration timeout,
        @DefaultValue("Europe/London") String defaultTimezone) {
}
