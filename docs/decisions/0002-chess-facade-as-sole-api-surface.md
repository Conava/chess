# 0002: Chess Façade as Sole API Surface

Status: accepted
Date: 2024-01-01
Deciders: project founders

## Context and Problem Statement

The `application` and `server` modules need to interact with the `core` game engine.
`core` contains multiple classes (`Game`, `OfflineGame`, `OnlineGame`, `ServerGame`, `Board`,
`Ruleset`, etc.). Should consumers reference these classes directly, or through a single
entry point?

## Considered Options

1. Direct access — controllers and server code reference `Game` subclasses and `Board` directly.
2. Façade — a single class (`Chess`) wraps the game engine and is the only approved API surface.

## Decision Outcome

Chosen option: "Façade", because it decouples the UI and server from internal `core` class
hierarchies, allows the `core` internals to be refactored without changing controller code,
and makes it impossible to instantiate the wrong game subtype from outside `core`.

### Consequences

- Good: `Game` subclass constructors can remain package-private, preventing accidental direct
  instantiation.
- Good: Controllers are simpler — they call `chess.*` methods without needing to know whether
  a game is local or networked.
- Good: The façade's null-safe query methods (`getState()` returns `null` when no game is
  active) give controllers a uniform API regardless of game lifecycle state.
- Bad: An additional class (`Chess`) exists that adds indirection.
- Bad: If the façade's `startGame()` silently ignores a call when a game is already running,
  callers cannot detect this condition (current known debt).

## Pros and Cons of the Options

### Direct access

- Good: No indirection; no additional class.
- Bad: Controllers become tightly coupled to `Game` subtypes; any rename or restructuring of
  `core` requires controller changes.
- Bad: Nothing prevents a controller from instantiating `OfflineGame` directly, bypassing
  the factory method.

### Façade

- Good: Stable API surface; `core` can be refactored independently.
- Good: Factory method enforcement — `Chess` is the only place that calls `Game.createGame()`.
- Good: Consistent null-safety contract for all callers.
