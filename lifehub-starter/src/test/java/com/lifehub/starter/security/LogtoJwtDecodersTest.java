package com.lifehub.starter.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

class LogtoJwtDecodersTest {

    private static final String ISSUER = "https://auth.lifehub.test/oidc";
    private static final String AUDIENCE = "https://api.lifehub.test";

    private final OAuth2TokenValidator<Jwt> validator = LogtoJwtDecoders.validator(ISSUER, AUDIENCE);

    @Test
    void acceptsALogtoAccessToken() {
        assertThat(validator.validate(token("at+jwt", ISSUER, AUDIENCE, "user-1")).hasErrors()).isFalse();
    }

    @Test
    void refusesAnIdTokenOrAnUntypedToken() {
        assertThat(validator.validate(token("JWT", ISSUER, AUDIENCE, "user-1")).hasErrors()).isTrue();
        assertThat(validator.validate(token(null, ISSUER, AUDIENCE, "user-1")).hasErrors()).isTrue();
    }

    @Test
    void refusesTheWrongIssuerAudienceOrNoSubject() {
        assertThat(validator.validate(token("at+jwt", "https://evil.test/oidc", AUDIENCE, "user-1")).hasErrors()).isTrue();
        assertThat(validator.validate(token("at+jwt", ISSUER, "https://internal.lifehub.test", "user-1")).hasErrors()).isTrue();
        assertThat(validator.validate(token("at+jwt", ISSUER, AUDIENCE, null)).hasErrors()).isTrue();
    }

    @Test
    void refusesAnExpiredToken() {
        Jwt expired = Jwt.withTokenValue("t").header("alg", "ES384").header("typ", "at+jwt")
                .issuer(ISSUER).audience(List.of(AUDIENCE)).subject("user-1")
                .issuedAt(Instant.now().minusSeconds(7200)).expiresAt(Instant.now().minusSeconds(3600)).build();
        assertThat(validator.validate(expired).hasErrors()).isTrue();
    }

    /** A token valid for an hour with these header type, issuer, audience and subject. */
    private static Jwt token(String typ, String issuer, String audience, String subject) {
        Jwt.Builder builder = Jwt.withTokenValue("t").header("alg", "ES384")
                .issuer(issuer).audience(List.of(audience))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600));
        if (typ != null) {
            builder.header("typ", typ);
        }
        if (subject != null) {
            builder.subject(subject);
        }
        return builder.build();
    }
}
