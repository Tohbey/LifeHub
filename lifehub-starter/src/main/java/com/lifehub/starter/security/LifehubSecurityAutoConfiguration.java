package com.lifehub.starter.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security for every business service. Three chains, checked in order:
 * <ol>
 * <li>{@code /internal/**}: service tokens only (internal audience).</li>
 * <li>{@code /api/**}: user tokens only (public audience).</li>
 * <li>everything else: health, info and the API docs are open, the rest is denied.</li>
 * </ol>
 * The gateway checks user tokens too, but services never rely on that.
 */
@AutoConfiguration(before = ServletWebSecurityAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(LifehubSecurityProperties.class)
@EnableWebSecurity
@EnableMethodSecurity
public class LifehubSecurityAutoConfiguration {

    /** Secures /internal/**: only service tokens with the internal audience are accepted. */
    @Bean
    @Order(1)
    SecurityFilterChain internalApiSecurity(HttpSecurity http, LifehubSecurityProperties properties) throws Exception {
        return statelessResourceServer(http, LogtoJwtDecoders.forAudience(properties, properties.internalAudience()))
                .securityMatcher("/internal/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .build();
    }

    /** Secures /api/**: only user tokens with the public audience are accepted. */
    @Bean
    @Order(2)
    SecurityFilterChain publicApiSecurity(HttpSecurity http, LifehubSecurityProperties properties) throws Exception {
        return statelessResourceServer(http, LogtoJwtDecoders.forAudience(properties, properties.audience()))
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .build();
    }

    /**
     * Everything else: health, info, the error page and the OpenAPI docs are open; all other paths are denied.
     * The docs only describe the API; every call made from Swagger UI still needs a token.
     */
    @Bean
    @Order(3)
    SecurityFilterChain fallbackSecurity(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                        .permitAll()
                        .anyRequest().denyAll())
                .build();
    }

    /** Applies the settings shared by both token chains: no CSRF, no sessions, JWT validation with this decoder. */
    private static HttpSecurity statelessResourceServer(HttpSecurity http, JwtDecoder decoder) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(decoder)));
    }
}
