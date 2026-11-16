package com.lifehub.starter.serviceauth;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * This service's own Logto machine-to-machine application. See ADR 0007.
 *
 * @param clientId     the M2M application's ID ({@code LOGTO_M2M_CLIENT_ID})
 * @param clientSecret the M2M application's secret ({@code LOGTO_M2M_CLIENT_SECRET})
 * @param tokenUri     defaults to {@code <lifehub.security.issuer>/token}
 * @param resource     the internal API resource indicator; defaults to {@code lifehub.security.internal-audience}
 * @param scopes       the scopes to request, for example {@code membership:read}
 */
@ConfigurationProperties("lifehub.service-auth")
public record ServiceAuthProperties(
        String clientId,
        String clientSecret,
        String tokenUri,
        String resource,
        @DefaultValue("membership:read") List<String> scopes) {
}
