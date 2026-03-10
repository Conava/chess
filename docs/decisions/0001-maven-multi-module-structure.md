# 0001: Maven Multi-Module Structure

Status: accepted
Date: 2024-01-01
Deciders: project founders

## Context and Problem Statement

The chess application needs a game engine, a desktop GUI, and a multiplayer server. These three
concerns have different dependencies, different deployment targets, and different change rates.
How should the codebase be organized to enforce separation of concerns?

## Considered Options

1. Single Maven module — all code in one `src/` tree
2. Maven multi-module — one module per concern (`core`, `application`, `server`)
3. Separate Git repositories per concern

## Decision Outcome

Chosen option: "Maven multi-module", because it enforces dependency boundaries at the build
level, allows modules to be built and tested independently, and keeps all code in a single
repository for ease of development.

### Consequences

- Good: Maven's dependency declarations make illegal cross-module imports fail at compile time.
- Good: `core` can be built and tested without JavaFX or server dependencies.
- Good: Application and server can be packaged as fat JARs independently.
- Bad: Maven multi-module projects have more configuration overhead than single-module projects.
- Neutral: The `core` module must be installed locally (`mvn install`) before `application`
  or `server` can resolve it.

## Pros and Cons of the Options

### Single module

- Good: Simpler `pom.xml`; no inter-module dependency management.
- Bad: No compile-time enforcement of boundaries; UI imports can creep into game logic.

### Maven multi-module

- Good: Boundary violations are build errors, not just code-review findings.
- Good: Independent packaging; server can be deployed without JavaFX.
- Bad: Requires understanding of Maven reactor ordering.

### Separate repositories

- Good: Maximum isolation.
- Bad: Requires publishing `core` as an artifact; adds CI/CD complexity; harder to make
  cross-module changes atomically.
