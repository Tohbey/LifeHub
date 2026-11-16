package com.lifehub.starter.web;

import java.util.List;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * OpenAPI docs and Swagger UI for every service, at {@code /swagger-ui.html}. Two groups:
 * {@code public} ({@code /api/**}, also served through lifehub-app) and {@code internal}
 * ({@code /internal/**}, only on the Docker network). Turn both off with {@code springdoc.api-docs.enabled=false}
 * and {@code springdoc.swagger-ui.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class OpenApiAutoConfiguration {

    static final String BEARER_SCHEME = "bearer-jwt";

    /**
     * Describes the service and its bearer token. The server is "/", so "Try it out" calls the origin the
     * UI was loaded from: lifehub-app when browsing through the gateway, the service itself otherwise.
     */
    @Bean
    @ConditionalOnMissingBean
    OpenAPI lifehubOpenApi(@Value("${spring.application.name:lifehub-service}") String serviceName) {
        return new OpenAPI()
                .info(new Info().title(serviceName).version("v1")
                        .description("Paste a Logto access token under Authorize. /api/** takes a user token, "
                                + "/internal/** a service token."))
                .servers(List.of(new Server().url("/")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    /** The public API group, {@code /v3/api-docs/public}. lifehub-app's Swagger UI reads this one. */
    @Bean
    @ConditionalOnMissingBean(name = "publicApiGroup")
    GroupedOpenApi publicApiGroup() {
        return GroupedOpenApi.builder().group("public").pathsToMatch("/api/**").build();
    }

    /** The service-to-service API group, {@code /v3/api-docs/internal}. Never routed by lifehub-app. */
    @Bean
    @ConditionalOnMissingBean(name = "internalApiGroup")
    GroupedOpenApi internalApiGroup() {
        return GroupedOpenApi.builder().group("internal").pathsToMatch("/internal/**").build();
    }
}
