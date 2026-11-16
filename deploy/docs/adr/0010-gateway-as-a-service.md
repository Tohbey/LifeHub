# 0010. The gateway is its own service; no service registry

- Status: Superseded by [0011](0011-routing-in-app-with-eureka.md)
- Date: 2026-11-15
- Supersedes: the "lifehub-app composition" decision in the README (gateway code packaged inside lifehub-app)

## Context

`lifehub-gateway` started as a library packaged inside `lifehub-app`, with routes built in Java. The project owner wants the gateway to be a service of its own, configured the standard Spring Cloud Gateway way, with `lifehub-app` handing API calls to it.

A related question is whether services should register with a registry such as Eureka so the gateway can find them.

## Decision

- `lifehub-gateway` is a runnable Spring Cloud Gateway (WebFlux) service. Its routes are declared in `application.yaml` under `spring.cloud.gateway.server.webflux.routes`. It validates user tokens on `/api/**` and denies everything else except health and info.
- `lifehub-app` serves the PWA and has one route: `/api/**` to `lifehub-gateway`. It has no knowledge of individual services.
- There is no service registry. Each route's URI is the service's Docker Compose name (`http://finance:8080`), overridable through `SERVICE_URL_<NAME>`. Compose's DNS is the discovery mechanism.

## Consequences

- Routing changes are configuration in one file, and `lifehub-app` never changes when a service is added.
- One more JVM (about 256 MB heap) and one more network hop per API call.
- Without a registry there is no client-side load balancing across several instances of a service. If that is ever needed, add `spring-cloud-starter-netflix-eureka-client` to the gateway and services, run a separate `eureka-server` container, and change route URIs to `lb://<service-name>`. The routes themselves stay the same.
