package com.lifehub.finance.controller;

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

import com.lifehub.finance.service.TransactionService;
import com.lifehub.finance.service.CategoryService;
import com.lifehub.finance.service.BudgetService;
import com.lifehub.finance.service.FinanceExportService;
import com.lifehub.finance.service.FinanceSummaryService;

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
    TransactionService transactionService;

    @MockitoBean
    CategoryService categoryService;

    @MockitoBean
    BudgetService budgetService;

    @MockitoBean
    FinanceSummaryService summaryService;

    @MockitoBean
    FinanceExportService exportService;


    @Test
    void publicDocsDescribeEveryEndpoint() throws Exception {
        String docs = mvc.perform(get("/v3/api-docs/public"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(docs).contains("\"title\":\"finance\"", "bearer-jwt");
        assertThat(docs).contains("\"/api/v1/workspaces/{workspaceId}/transactions\"");
        assertThat(docs).contains("\"/api/v1/workspaces/{workspaceId}/transactions/{transactionId}\"");
        assertThat(docs).contains("\"/api/v1/workspaces/{workspaceId}/finance/categories\"");
        assertThat(docs).contains("\"/api/v1/workspaces/{workspaceId}/budgets\"");
        assertThat(docs).contains("\"/api/v1/workspaces/{workspaceId}/finance/summary\"");
        assertThat(docs).contains("\"name\":\"Transactions\"");
        assertThat(docs).contains("\"name\":\"Categories\"");
        assertThat(docs).contains("\"name\":\"Budgets\"");
        assertThat(docs).contains("\"name\":\"Finance summary\"");
        assertThat(docs).contains("\"summary\":\"Record income or an expense\"", "\"401\"", "#/components/schemas/ProblemDetail");
        assertThat(docs).doesNotContain("\"/internal/v1");
    }

    @Test
    void swaggerUiNeedsNoToken() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk());
    }
}
