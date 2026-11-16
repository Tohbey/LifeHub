package com.lifehub.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.sun.net.httpserver.HttpServer;

/**
 * lifehub-app end to end: PWA, token check and routing. Eureka is switched off and the "identity-workspace"
 * instance is registered with Spring Cloud's simple discovery client instead, pointing at a stub, so
 * lb://identity-workspace resolves exactly as it would through the registry.
 */
@SpringBootTest
class LifehubApplicationTest {

    private static final List<String> STUB_REQUESTS = new CopyOnWriteArrayList<>();
    private static final HttpServer STUB = startStub();

    /** Disables Eureka, registers the stub as identity-workspace, and sets dummy Logto settings. */
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.client.simple.instances.identity-workspace[0].uri",
                () -> "http://localhost:" + STUB.getAddress().getPort());
        registry.add("lifehub.security.issuer", () -> "https://auth.lifehub.test/oidc");
        registry.add("lifehub.security.audience", () -> "https://api.lifehub.test");
    }

    @Autowired
    ApplicationContext context;

    WebTestClient client;

    /** Builds a client bound to the application with Spring Security's test support. */
    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToApplicationContext(context).apply(springSecurity()).configureClient().build();
        STUB_REQUESTS.clear();
    }

    /** Stops the stub service. */
    @AfterAll
    static void stopStub() {
        STUB.stop(0);
    }

    @Test
    void servesThePwaAtTheRootAndForClientRoutes() {
        for (String path : List.of("/", "/finance", "/households/123/members")) {
            client.get().uri(path).exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                    .expectBody(String.class).value(body -> assertThat(body).contains("<title>LifeHub</title>"));
        }
    }

    @Test
    void missingStaticFilesAreNotFound() {
        client.get().uri("/missing.js").exchange().expectStatus().isNotFound();
    }

    @Test
    void apiNeedsAToken() {
        client.get().uri("/api/v1/me").exchange().expectStatus().isUnauthorized();
        assertThat(STUB_REQUESTS).isEmpty();
    }

    @Test
    void apiRequestsWithATokenReachTheRegisteredService() {
        client.mutateWith(mockJwt()).get().uri("/api/v1/me/workspaces?page=0").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("{\"from\":\"identity-workspace\"}");
        assertThat(STUB_REQUESTS).containsExactly("GET /api/v1/me/workspaces?page=0");
    }

    @Test
    void servicesWithNoRegisteredInstanceAreUnavailable() {
        client.mutateWith(mockJwt()).get().uri("/api/v1/workspaces/1/transactions").exchange()
                .expectStatus().isEqualTo(503);
    }

    @Test
    void unknownApiPathsAreNotFound() {
        client.mutateWith(mockJwt()).get().uri("/api/v1/nothing-here").exchange().expectStatus().isNotFound();
    }

    @Test
    void internalPathsAreNeverExposed() {
        client.mutateWith(mockJwt()).get().uri("/internal/v1/workspaces/1/members?sub=x").exchange()
                .expectStatus().isForbidden();
        client.get().uri("/internal/v1/workspaces/1/members?sub=x").exchange().expectStatus().is4xxClientError();
        assertThat(STUB_REQUESTS).isEmpty();
    }

    @Test
    void healthIsPublicAndOtherActuatorEndpointsAreNot() {
        client.get().uri("/actuator/health").exchange().expectStatus().isOk();
        client.get().uri("/actuator/env").exchange().expectStatus().is4xxClientError();
    }

    @Test
    void apiDocsAreFetchedFromTheServicesPublicGroupOnly() {
        client.get().uri("/openapi/identity-workspace").exchange().expectStatus().isOk();
        assertThat(STUB_REQUESTS).containsExactly("GET /v3/api-docs/public");
    }

    @Test
    void swaggerUiIsServedAndListsTheServices() {
        client.get().uri("/swagger-ui.html").exchange().expectStatus().is3xxRedirection();
        client.get().uri("/v3/api-docs/swagger-config").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body).contains("/openapi/identity-workspace"));
    }

    /** Starts an HTTP server that records each request and answers as identity-workspace. */
    private static HttpServer startStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                STUB_REQUESTS.add(exchange.getRequestMethod() + " " + exchange.getRequestURI());
                byte[] body = "{\"from\":\"identity-workspace\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
