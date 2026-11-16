package com.lifehub.starter.serviceauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ServiceTokenProviderTest {

    private static final String TOKEN_URI = "https://auth.example.com/oidc/token";

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-15T10:00:00Z"));

    @Test
    void reusesTokenUntilShortlyBeforeExpiry() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo(TOKEN_URI))
                .andExpect(header("Authorization", "Basic Y2xpZW50OnNlY3JldA=="))
                .andExpect(content().formDataContains(java.util.Map.of(
                        "grant_type", "client_credentials",
                        "resource", "https://internal.lifehub",
                        "scope", "membership:read")))
                .andRespond(withSuccess("{\"access_token\":\"first\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(TOKEN_URI))
                .andRespond(withSuccess("{\"access_token\":\"second\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));

        ServiceTokenProvider provider = new ServiceTokenProvider(builder.build(), TOKEN_URI, "client", "secret",
                "https://internal.lifehub", "membership:read", clock);

        assertThat(provider.token()).isEqualTo("first");
        clock.advance(Duration.ofMinutes(58));
        assertThat(provider.token()).isEqualTo("first");
        clock.advance(Duration.ofMinutes(1));
        assertThat(provider.token()).isEqualTo("second");
        server.verify();
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
