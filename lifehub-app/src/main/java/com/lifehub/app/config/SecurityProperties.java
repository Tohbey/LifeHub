package com.lifehub.app.config;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.validation.annotation.Validated;

/**
 * The same {@code lifehub.security.*} keys the services use, so one set of Logto settings serves both.
 * lifehub-app only checks user tokens, so it needs no internal audience.
 *
 * @param issuer        Logto's OIDC issuer, for example {@code https://auth.example.com/oidc}
 * @param jwkSetUri     where signing keys are fetched; defaults to {@code <issuer>/jwks}
 * @param audience      audience of user tokens
 * @param caCertificate PEM file of the CA that signed Logto's TLS certificate ({@code LOGTO_CA_CERT}), for a
 *                      private CA such as mkcert. When set, calls to Logto trust only this CA. Leave empty
 *                      when Logto has a publicly trusted certificate.
 */
@Validated
@ConfigurationProperties("lifehub.security")
public record SecurityProperties(@NotBlank String issuer, String jwkSetUri, @NotBlank String audience,
        String caCertificate) {

    private static final Duration LOGTO_TIMEOUT = Duration.ofSeconds(5);

    /** Returns the configured JWKS URL, or {@code <issuer>/jwks} when none is set. */
    public String resolvedJwkSetUri() {
        if (jwkSetUri != null && !jwkSetUri.isBlank()) {
            return jwkSetUri;
        }
        return (issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer) + "/jwks";
    }

    /** HTTP client settings for calls to Logto: 5 s timeouts, and the CA certificate as trust when one is set. */
    public HttpClientSettings logtoHttpClientSettings() {
        HttpClientSettings settings = HttpClientSettings.defaults().withTimeouts(LOGTO_TIMEOUT, LOGTO_TIMEOUT);
        if (caCertificate == null || caCertificate.isBlank()) {
            return settings;
        }
        // A bare path would be read from the classpath; the setting is always a file on disk.
        String location = caCertificate.contains(":") ? caCertificate : "file:" + caCertificate;
        return settings.withSslBundle(
                SslBundle.of(new PemSslStoreBundle(null, PemSslStoreDetails.forCertificates(location))));
    }
}
