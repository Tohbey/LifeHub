package com.lifehub.app.config;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The same {@code lifehub.security.*} keys the services use, so one set of Logto settings serves both.
 * lifehub-app only checks user tokens, so it needs no internal audience.
 *
 * @param issuer    Logto's OIDC issuer, for example {@code https://auth.example.com/oidc}
 * @param jwkSetUri where signing keys are fetched; defaults to {@code <issuer>/jwks}
 * @param audience  audience of user tokens
 */
@Validated
@ConfigurationProperties("lifehub.security")
public record SecurityProperties(@NotBlank String issuer, String jwkSetUri, @NotBlank String audience) {

    /** Returns the configured JWKS URL, or {@code <issuer>/jwks} when none is set. */
    public String resolvedJwkSetUri() {
        if (jwkSetUri != null && !jwkSetUri.isBlank()) {
            return jwkSetUri;
        }
        return (issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer) + "/jwks";
    }
}
