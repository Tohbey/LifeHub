package com.lifehub.starter.security;

import java.util.List;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTypeValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Builds decoders for Logto access tokens. Each decoder checks signature, expiry, issuer, one audience, the
 * access-token type ({@code typ: at+jwt}) and that the token names a subject.
 * Signing keys are fetched on first use, so a service starts even if Logto is briefly unreachable. The fetch
 * trusts {@code lifehub.security.ca-certificate} when it is set.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LogtoJwtDecoders {

    /** Builds a decoder that accepts Logto tokens for exactly this audience, signed with ES384 or RS256. */
    public static JwtDecoder forAudience(LifehubSecurityProperties properties, String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.resolvedJwkSetUri())
                // Logto signs with ES384 by default; RS256 covers a Logto instance configured for RSA keys.
                .jwsAlgorithms(algorithms -> {
                    algorithms.add(SignatureAlgorithm.ES384);
                    algorithms.add(SignatureAlgorithm.RS256);
                })
                .restOperations(new RestTemplate(
                        ClientHttpRequestFactoryBuilder.jdk().build(properties.logtoHttpClientSettings())))
                .build();
        decoder.setJwtValidator(validator(properties.issuer(), audience));
        return decoder;
    }

    /**
     * The claim checks every Logto access token must pass: expiry, issuer, audience, subject, and type.
     * Logto marks access tokens {@code typ: at+jwt} (RFC 9068); ID tokens are {@code typ: JWT}, so only
     * accepting at+jwt also stops an ID token being used as an access token.
     */
    static OAuth2TokenValidator<Jwt> validator(String issuer, String audience) {
        // Passing our own type validator replaces Spring's default one, which accepts only "JWT".
        return JwtValidators.createDefaultWithValidators(
                new JwtIssuerValidator(issuer),
                new JwtTypeValidator("at+jwt"),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD, aud -> aud != null && aud.contains(audience)),
                // Every use case keys the caller on "sub"; a token without one must never reach them.
                new JwtClaimValidator<String>(JwtClaimNames.SUB, sub -> sub != null && !sub.isBlank()));
    }
}
