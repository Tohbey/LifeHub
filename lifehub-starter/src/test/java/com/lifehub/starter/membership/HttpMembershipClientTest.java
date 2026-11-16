package com.lifehub.starter.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.lifehub.starter.id.Ids;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

class HttpMembershipClientTest {

    private static final String BASE = "http://identity-workspace:8080";

    private final UUID workspaceId = Ids.newId();
    private final UUID userId = Ids.newId();

    private MockRestServiceServer server;
    private HttpMembershipClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        CircuitBreaker breaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build());
        client = new HttpMembershipClient(builder.build(), breaker,
                new MembershipProperties(BASE, Duration.ofSeconds(30), Duration.ofSeconds(1)));
    }

    @Test
    void returnsMembershipAndCachesIt() {
        server.expect(once(), requestTo(lookupUrl("sub-1")))
                .andRespond(withSuccess("""
                        {"workspaceId":"%s","userId":"%s","role":"ADMIN"}
                        """.formatted(workspaceId, userId), MediaType.APPLICATION_JSON));

        assertThat(client.findBySubject(workspaceId, "sub-1"))
                .contains(new Membership(workspaceId, userId, WorkspaceRole.ADMIN));
        // Second call is served from the cache; the server expects exactly one request.
        assertThat(client.findBySubject(workspaceId, "sub-1")).isPresent();
        server.verify();
    }

    @Test
    void notFoundMeansNotAMemberAndIsNotCached() {
        server.expect(requestTo(lookupUrl("sub-2"))).andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo(lookupUrl("sub-2"))).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.findBySubject(workspaceId, "sub-2")).isEmpty();
        assertThat(client.findBySubject(workspaceId, "sub-2")).isEmpty();
        server.verify();
    }

    @Test
    void serverErrorFailsClosed() {
        server.expect(requestTo(lookupUrl("sub-3"))).andRespond(withServerError());

        assertThatThrownBy(() -> client.findBySubject(workspaceId, "sub-3"))
                .isInstanceOf(MembershipUnavailableException.class);
    }

    @Test
    void openCircuitFailsClosedWithoutCallingTheServer() {
        server.expect(requestTo(lookupUrl("a"))).andRespond(withServerError());
        server.expect(requestTo(lookupUrl("b"))).andRespond(withServerError());

        assertThatThrownBy(() -> client.findBySubject(workspaceId, "a")).isInstanceOf(MembershipUnavailableException.class);
        assertThatThrownBy(() -> client.findBySubject(workspaceId, "b")).isInstanceOf(MembershipUnavailableException.class);
        // The breaker is now open: this call must not reach the server (no third expectation registered).
        assertThatThrownBy(() -> client.findBySubject(workspaceId, "c")).isInstanceOf(MembershipUnavailableException.class);
        server.verify();
    }

    @Test
    void bulkLookupReturnsOnlyMembers() {
        UUID other = Ids.newId();
        server.expect(requestTo(BASE + "/internal/v1/workspaces/" + workspaceId + "/members?userId=" + userId
                + "&userId=" + other))
                .andRespond(withSuccess("""
                        {"workspaceId":"%s","members":[{"userId":"%s","role":"MEMBER"}]}
                        """.formatted(workspaceId, userId), MediaType.APPLICATION_JSON));

        assertThat(client.findMembers(workspaceId, List.of(userId, other)))
                .containsOnlyKeys(userId)
                .containsEntry(userId, WorkspaceRole.MEMBER);
    }

    @Test
    void looksUpTheUserIdOfASubjectAndCachesIt() {
        server.expect(once(), requestTo(BASE + "/internal/v1/users?sub=sub-1"))
                .andRespond(withSuccess("{\"userId\":\"%s\"}".formatted(userId), MediaType.APPLICATION_JSON));

        assertThat(client.findUserIdBySubject("sub-1")).contains(userId);
        assertThat(client.findUserIdBySubject("sub-1")).contains(userId);
        server.verify();
    }

    @Test
    void anUnknownSubjectHasNoUserId() {
        server.expect(once(), requestTo(BASE + "/internal/v1/users?sub=sub-2")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.findUserIdBySubject("sub-2")).isEmpty();
    }

    private String lookupUrl(String sub) {
        return BASE + "/internal/v1/workspaces/" + workspaceId + "/members?sub=" + sub;
    }
}
