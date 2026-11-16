package com.lifehub.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocJavadocConfiguration;
import org.springdoc.core.configuration.SpringDocPageableConfiguration;
import org.springdoc.core.configuration.SpringDocSortConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springdoc.webmvc.ui.SwaggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lifehub.notification.service.NotificationService;
import com.lifehub.notification.service.PreferenceService;
import com.lifehub.notification.service.NotificationStreams;
import com.lifehub.starter.membership.WorkspaceAccess;

import com.lifehub.starter.security.LifehubSecurityAutoConfiguration;
import com.lifehub.starter.web.OpenApiAutoConfiguration;
import com.lifehub.starter.web.WebAutoConfiguration;

/**
 * Generates this service's real OpenAPI document from its controllers, without a database: every endpoint
 * is in the public group with its Swagger tag, summary and error responses, no internal endpoint
 * leaks into it, and Swagger UI is served without a token.
 */
@WebMvcTest
@ImportAutoConfiguration({ LifehubSecurityAutoConfiguration.class, WebAutoConfiguration.class, OpenApiAutoConfiguration.class,
        SpringDocConfiguration.class, SpringDocConfigProperties.class, SpringDocJavadocConfiguration.class,
        SpringDocPageableConfiguration.class, SpringDocSortConfiguration.class, SpringDocSpecPropertiesConfiguration.class,
        SpringDocWebMvcConfiguration.class, MultipleOpenApiSupportConfiguration.class, SwaggerConfig.class,
        SwaggerUiConfigProperties.class, SwaggerUiOAuthProperties.class })
@TestPropertySource(properties = {
        "lifehub.security.issuer=https://auth.lifehub.test/oidc",
        "lifehub.security.audience=https://api.lifehub.test",
        "lifehub.security.internal-audience=https://internal.lifehub.test",
        "lifehub.security.ca-certificate=" })
class OpenApiDocsTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    NotificationService notificationService;

    @MockitoBean
    PreferenceService preferenceService;

    @MockitoBean
    NotificationStreams streams;

    @MockitoBean
    WorkspaceAccess workspaceAccess;


    @Test
    void publicDocsDescribeEveryEndpoint() throws Exception {
        String docs = mvc.perform(get("/v3/api-docs/public"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(docs).contains("\"title\":\"notification\"", "bearer-jwt");
        assertThat(docs).contains("\"/api/v1/me/notifications\"");
        assertThat(docs).contains("\"/api/v1/me/notifications/{notificationId}\"");
        assertThat(docs).contains("\"/api/v1/me/notifications/stream\"");
        assertThat(docs).contains("\"/api/v1/me/notifications/unread-count\"");
        assertThat(docs).contains("\"/api/v1/me/notifications/read-all\"");
        assertThat(docs).contains("\"/api/v1/me/notification-preferences\"");
        assertThat(docs).contains("\"name\":\"Notifications\"");
        assertThat(docs).contains("\"name\":\"Notification preferences\"");
        assertThat(docs).contains("\"summary\":\"List my notifications\"", "\"401\"", "#/components/schemas/ProblemDetail");
        assertThat(docs).doesNotContain("\"/internal/v1");
    }

    @Test
    void swaggerUiNeedsNoToken() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk());
    }
}
