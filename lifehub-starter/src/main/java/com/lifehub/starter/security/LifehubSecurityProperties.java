package com.lifehub.starter.security;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * How services validate Logto access tokens. See ADR 0007.
 *
 * @param issuer           Logto's OIDC issuer, for example {@code https://auth.example.com/oidc}
 * @param jwkSetUri        where signing keys are fetched; defaults to {@code <issuer>/jwks}
 * @param audience         audience of user tokens, accepted on {@code /api/**}
 * @param internalAudience audience of service tokens, accepted on {@code /internal/**}
 */
@Validated
@ConfigurationProperties("lifehub.security")
public record LifehubSecurityProperties(
        @NotBlank String issuer,
        String jwkSetUri,
        @NotBlank String audience,
        @NotBlank String internalAudience) {

    /** Returns the configured JWKS URL, or {@code <issuer>/jwks} when none is set. */
    public String resolvedJwkSetUri() {
        return jwkSetUri == null || jwkSetUri.isBlank() ? stripTrailingSlash(issuer) + "/jwks" : jwkSetUri;
    }

    /** Removes one trailing slash from a URL, if present. */
    static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
