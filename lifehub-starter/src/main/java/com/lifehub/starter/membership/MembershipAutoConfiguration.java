package com.lifehub.starter.membership;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.lifehub.starter.security.LifehubSecurityAutoConfiguration;
import com.lifehub.starter.security.LifehubSecurityProperties;
import com.lifehub.starter.serviceauth.ServiceAuthProperties;
import com.lifehub.starter.serviceauth.ServiceTokenProvider;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

/**
 * Wires the membership check. identity-workspace declares its own {@link MembershipClient} that reads its
 * database directly, so the HTTP client here backs off.
 */
@AutoConfiguration(after = LifehubSecurityAutoConfiguration.class)
@EnableConfigurationProperties({ MembershipProperties.class, ServiceAuthProperties.class })
public class MembershipAutoConfiguration {

    /** Provides a UTC clock, so tests can replace time. */
    @Bean
    @ConditionalOnMissingBean
    Clock lifehubClock() {
        return Clock.systemUTC();
    }

    /** Provides this service's machine-to-machine token source, when Logto client credentials are configured. */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("lifehub.service-auth.client-id")
    ServiceTokenProvider serviceTokenProvider(ServiceAuthProperties serviceAuth, LifehubSecurityProperties security,
            RestClient.Builder builder, Clock clock) {
        String issuer = security.issuer().replaceAll("/$", "");
        return new ServiceTokenProvider(
                builder.clone().requestFactory(requestFactory(Duration.ofSeconds(5))).build(),
                orDefault(serviceAuth.tokenUri(), issuer + "/token"),
                serviceAuth.clientId(),
                serviceAuth.clientSecret(),
                orDefault(serviceAuth.resource(), security.internalAudience()),
                String.join(" ", serviceAuth.scopes()),
                clock);
    }

    /**
     * Provides the HTTP membership client, unless the service defines its own (identity-workspace does).
     * Fails at startup if the identity-workspace URL or the service credentials are missing.
     */
    @Bean
    @ConditionalOnMissingBean
    MembershipClient httpMembershipClient(MembershipProperties properties, RestClient.Builder builder,
            ObjectProvider<ServiceTokenProvider> tokenProvider) {
        if (properties.baseUrl() == null || properties.baseUrl().isBlank()) {
            throw new IllegalStateException("lifehub.membership.base-url must point at identity-workspace");
        }
        ServiceTokenProvider tokens = tokenProvider.getIfAvailable(() -> {
            throw new IllegalStateException(
                    "lifehub.service-auth.client-id and client-secret are needed to call identity-workspace");
        });
        RestClient restClient = builder.clone()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory(properties.timeout()))
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.token());
                    return execution.execute(request, body);
                })
                .build();
        CircuitBreaker breaker = CircuitBreaker.of("membership", CircuitBreakerConfig.custom()
                .slidingWindowSize(20)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .build());
        return new HttpMembershipClient(restClient, breaker, properties);
    }

    /** Provides the membership guard services call at the start of each workspace-scoped use case. */
    @Bean
    @ConditionalOnMissingBean
    WorkspaceAccess workspaceAccess(MembershipClient membershipClient) {
        return new WorkspaceAccess(membershipClient);
    }

    /** Builds an HTTP client with the same connect and read timeout. */
    private static JdkClientHttpRequestFactory requestFactory(Duration timeout) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(timeout).build());
        factory.setReadTimeout(timeout);
        return factory;
    }

    /** Returns {@code value}, or {@code fallback} when the value is missing or blank. */
    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
