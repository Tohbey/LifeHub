# 0001. Record architecture decisions

- Status: Accepted
- Date: 2026-11-15

## Context

LifeHub has many moving parts (several services, Kafka, Logto, per-service schemas). Decisions made in conversation or in the README get lost or quietly contradicted as the code grows.

## Decision

Record significant decisions as short ADRs in `deploy/docs/adr/`, using the template in this folder's README. A decision is significant if reversing it would touch more than one module, or if someone would reasonably ask "why is it like this?".

## Consequences

- New contributors (and agents) can read why things are the way they are.
- The README stays a description of the system; the reasoning lives here.
- Changing an accepted decision means writing a new ADR, which forces the trade-off to be stated.
