package com.lifehub.starter.security;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.validation.annotation.Validated;

/**
 * How services validate Logto access tokens. See ADR 0007.
 *
 * @param issuer           Logto's OIDC issuer, for example {@code https://auth.example.com/oidc}
 * @param jwkSetUri        where signing keys are fetched; defaults to {@code <issuer>/jwks}
 * @param audience         audience of user tokens, accepted on {@code /api/**}
 * @param internalAudience audience of service tokens, accepted on {@code /internal/**}
 * @param caCertificate    PEM file of the CA that signed Logto's TLS certificate ({@code LOGTO_CA_CERT}), for a
 *                         private CA such as mkcert. When set, calls to Logto trust only this CA. Leave empty
 *                         when Logto has a publicly trusted certificate.
 */
@Validated
@ConfigurationProperties("lifehub.security")
public record LifehubSecurityProperties(
        @NotBlank String issuer,
        String jwkSetUri,
        @NotBlank String audience,
        @NotBlank String internalAudience,
        String caCertificate) {

    private static final Duration LOGTO_TIMEOUT = Duration.ofSeconds(5);

    /** Returns the configured JWKS URL, or {@code <issuer>/jwks} when none is set. */
    public String resolvedJwkSetUri() {
        return jwkSetUri == null || jwkSetUri.isBlank() ? stripTrailingSlash(issuer) + "/jwks" : jwkSetUri;
    }

    /** HTTP client settings for calls to Logto: 5 s timeouts, and the CA certificate as trust when one is set. */
    public HttpClientSettings logtoHttpClientSettings() {
        HttpClientSettings settings = HttpClientSettings.defaults().withTimeouts(LOGTO_TIMEOUT, LOGTO_TIMEOUT);
        if (caCertificate == null || caCertificate.isBlank()) {
            return settings;
        }
        return settings.withSslBundle(trustingOnly(caCertificate));
    }

    /** An SSL bundle whose trust store holds only the certificate(s) in this PEM file. */
    static SslBundle trustingOnly(String pemLocation) {
        // A bare path would be read from the classpath; the setting is always a file on disk.
        String location = pemLocation.contains(":") ? pemLocation : "file:" + pemLocation;
        return SslBundle.of(new PemSslStoreBundle(null, PemSslStoreDetails.forCertificates(location)));
    }

    /** Removes one trailing slash from a URL, if present. */
    static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
