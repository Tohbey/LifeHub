# 0011. Routing in lifehub-app, services found through Eureka

- Status: Accepted
- Date: 2026-11-15
- Supersedes: [0010](0010-gateway-as-a-service.md)

## Context

ADR 0010 split routing into a separate `lifehub-gateway` service and used Docker Compose names instead of a registry. The project owner wants a service registry instead, and the routing and token check back in `lifehub-app`.

## Decision

- `lifehub-gateway` is replaced by `lifehub-service-registry`, a Eureka server on port 8761.
- Every business service registers with it under its `spring.application.name`, through the Eureka client in `lifehub-starter`. Instances register their IP address, because Docker hostnames are not resolvable from other containers.
- `lifehub-app` is the API gateway again. It validates user tokens on `/api/**` (`lifehub.security`) and declares the route table in its `application.yaml` under `spring.cloud.gateway.server.webflux.routes`, with URIs of the form `lb://<service>`. It registers too, so the registry lists every running LifeHub application.
- The registry is a single instance with self-preservation off: on one host, missed heartbeats mean a service is really down.

## Consequences

- No service URLs are configured anywhere. Running two instances of a service spreads traffic across both without config changes.
- The registry is on the startup path: services and `lifehub-app` need it to find each other. Compose starts it first, and clients retry until it is up. If it goes down, `lifehub-app` keeps routing with its last fetched copy of the registry.
- A newly started service takes up to about 30 seconds to become reachable (registration plus `lifehub-app`'s 10-second registry refresh). A stopped service may still receive traffic for a similar time and answer 503 until it is evicted.
- Service-to-service calls (the membership lookup) still use `lifehub.membership.base-url`. They can move to `lb://identity-workspace` later with a `@LoadBalanced` `RestClient.Builder`.
- Same number of JVMs as ADR 0010: the registry replaces the gateway service.
