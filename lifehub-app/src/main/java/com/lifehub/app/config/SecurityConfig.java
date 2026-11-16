package com.lifehub.app.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Rejects API requests without a valid user token before they reach a service. Services validate the token
 * again; this is the first line of defence, not the only one. The user's token is passed on unchanged.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Denies /internal/**, which is never routed; requires a valid user token on /api/**; opens health and
     * info; and leaves everything else public, because it is the PWA's static files.
     */
    @Bean
    SecurityWebFilterChain gatewaySecurity(ServerHttpSecurity http, SecurityProperties properties) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers("/internal/**").denyAll()
                        .pathMatchers("/api/**").authenticated()
                        .pathMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .pathMatchers("/actuator/**").denyAll()
                        .anyExchange().permitAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtDecoder(decoder(properties))))
                .build();
    }

    /** Builds the decoder that checks a Logto user token's signature, expiry, issuer, audience and subject. */
    private static NimbusReactiveJwtDecoder decoder(SecurityProperties properties) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(properties.resolvedJwkSetUri())
                // Logto signs with ES384 by default; RS256 covers a Logto instance configured for RSA keys.
                .jwsAlgorithms(algorithms -> {
                    algorithms.add(SignatureAlgorithm.ES384);
                    algorithms.add(SignatureAlgorithm.RS256);
                })
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.contains(properties.audience())),
                // Services key the caller on "sub"; reject a token without one here already.
                new JwtClaimValidator<String>(JwtClaimNames.SUB, sub -> sub != null && !sub.isBlank())));
        return decoder;
    }
}
