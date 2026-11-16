package com.lifehub.starter.security;

import java.util.List;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Builds decoders for Logto access tokens. Each decoder checks signature, expiry, issuer, one audience and
 * that the token names a subject.
 * Signing keys are fetched on first use, so a service starts even if Logto is briefly unreachable.
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
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD, aud -> aud != null && aud.contains(audience)),
                // Every use case keys the caller on "sub"; a token without one must never reach them.
                new JwtClaimValidator<String>(JwtClaimNames.SUB, sub -> sub != null && !sub.isBlank())));
        return decoder;
    }
}
