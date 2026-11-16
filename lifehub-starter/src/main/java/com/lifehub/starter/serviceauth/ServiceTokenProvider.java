package com.lifehub.starter.serviceauth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.RequiredArgsConstructor;

/**
 * Gets this service's machine-to-machine token from Logto (client credentials grant) and reuses it until
 * shortly before it expires. The token identifies the service, never a user.
 */
@RequiredArgsConstructor
public class ServiceTokenProvider {

    private static final Duration REFRESH_MARGIN = Duration.ofSeconds(60);

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn) {
    }

    private final RestClient restClient;
    private final String tokenUri;
    private final String clientId;
    private final String clientSecret;
    private final String resource;
    private final String scope;
    private final Clock clock;

    private String token;
    private Instant refreshAt = Instant.MIN;

    /**
     * Returns a valid service token, fetching a new one from Logto when the cached one is within a
     * minute of expiry.
     */
    public synchronized String token() {
        Instant now = clock.instant();
        if (token == null || !now.isBefore(refreshAt)) {
            TokenResponse response = fetch();
            token = response.accessToken();
            refreshAt = now.plusSeconds(response.expiresIn()).minus(REFRESH_MARGIN);
        }
        return token;
    }

    /** Requests a token from Logto with the client credentials grant. */
    private TokenResponse fetch() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("resource", resource);
        form.add("scope", scope);
        TokenResponse response = restClient.post()
                .uri(tokenUri)
                .headers(h -> h.setBasicAuth(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("Logto returned no access token");
        }
        return response;
    }
}
