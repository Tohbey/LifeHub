package com.lifehub.serviceregistry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** The registry starts, reports healthy, and serves the Eureka REST API with no applications yet. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServiceRegistryApplicationTest {

    @LocalServerPort
    int port;

    @Test
    void isHealthy() {
        String health = client().get().uri("/actuator/health").retrieve().body(String.class);
        assertThat(health).contains("\"status\":\"UP\"");
    }

    @Test
    void servesTheEurekaApi() {
        String apps = client().get().uri("/eureka/apps").accept(MediaType.APPLICATION_JSON).retrieve().body(String.class);
        assertThat(apps).contains("\"applications\"");
    }

    /** Builds a client for the registry running on the random test port. */
    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }
}
