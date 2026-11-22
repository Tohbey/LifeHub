package com.lifehub.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** The route table in application.yaml, evaluated by Spring Cloud Gateway itself: first match in order wins. */
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "lifehub.security.issuer=https://auth.lifehub.test/oidc",
        "lifehub.security.audience=https://api.lifehub.test",
        "lifehub.security.ca-certificate=" })
class RoutesTest {

    private static final String W = "/api/v1/workspaces/0193a4a0-1b2c-7e4f-8a00-5d6e7f809a1b";

    @Autowired
    RouteLocator routes;

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "/api/v1/me,                                    identity-workspace",
            "/api/v1/me/workspaces,                         identity-workspace",
            "/api/v1/me/notifications,                      notification",
            "/api/v1/me/notifications/stream,               notification",
            "/api/v1/me/notification-preferences,           notification",
            "/api/v1/me/notifications/unread-count,         notification",
            "/api/v1/households,                            identity-workspace",
            "/api/v1/households/abc/members/def,            identity-workspace",
            "/api/v1/invitations/token/accept,              identity-workspace",
            W + "/transactions,                             finance",
            W + "/transactions/123,                         finance",
            W + "/finance/summary,                          finance",
            W + "/finance/categories,                       finance",
            W + "/budgets,                                  finance",
            W + "/tasks,                                    planner",
            W + "/events/1,                                 planner",
            W + "/shopping-lists,                           shopping-meals",
            "/api/v1/shopping-items/1,                      shopping-meals",
            "/api/v1/shopping-lists/1/items,                shopping-meals",
            "/openapi/finance,                              openapi-finance",
            "/openapi/audit,                                openapi-audit",
            W + "/meals,                                    shopping-meals",
            W + "/recipes/1/shopping-list,                  shopping-meals",
            W + "/bookmarks,                                notes",
            W + "/tags,                                     notes",
            "/openapi/notes,                                openapi-notes",
            W + "/notes,                                    notes",
            W + "/audit,                                    audit",
            "/internal/v1/workspaces/1/members,             NONE",
            W + "/unknown,                                  NONE",
            W + "/documents,                                NONE",
            "/finance,                                      NONE",
    })
    void routesToTheOwningService(String path, String expected) {
        assertThat(routeFor(path)).isEqualTo(expected);
    }

    /** Returns the ID of the first route, in order, whose predicate accepts a GET of this path. */
    private String routeFor(String path) {
        return Flux.from(routes.getRoutes())
                .concatMap(route -> Mono.from(route.getPredicate()
                                .apply(MockServerWebExchange.from(MockServerHttpRequest.get(path))))
                        .filter(Boolean::booleanValue)
                        .map(matched -> route))
                .next()
                .map(Route::getId)
                .defaultIfEmpty("NONE")
                .block();
    }
}
