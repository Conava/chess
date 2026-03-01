# 0001: Server Game Creation via Game.createServerGame() Factory Method

Status: accepted
Date: 2026-03-01
Deciders: human, architect

## Context and Problem Statement

The server module needs to create server-side chess game instances. Before this decision, `GameInstance` directly instantiated `new ServerGame(ruleset)`. Direct instantiation of `Game` subclasses from outside `core` violates Architecture Law 2: "The `Chess` façade is the only API surface. Direct instantiation of `Game` subclasses from outside `core` is banned."

The question is: what is the sanctioned path for the server to obtain a `Game` instance?

## Considered Options

1. Add `Game.createServerGame(RulesetOptions, String, String)` static factory method to `core`.
2. Extend the existing `Game.createGame()` method with an additional parameter or overload to cover the server case.
3. Keep `new ServerGame()` but make `ServerGame` public and treat it as permitted.

## Decision Outcome

Chosen option: Option 1 — add `Game.createServerGame(RulesetOptions, String, String)` to `core`.

The factory method pattern is already established in `core` (`Game.createGame()` exists for application use). This is a small, additive, non-breaking change that provides a named, clearly-scoped entry point for server-side game creation. The server calls only `Game.createServerGame()`; `ServerGame` constructor visibility remains package-private, making the constraint enforceable at compile time.

### Consequences

- Good: Law 2 compliance is restored and enforced by the compiler — `ServerGame` is not visible outside `core`.
- Good: The factory method name makes the server-specific intent explicit in the API.
- Good: Both player names are passed at construction time, keeping `Game` and `Player` immutable after creation.
- Bad: `core` grows a method specifically for one consumer (server). If a third consumer emerges with different needs, a third factory may be needed.
- Neutral: `ServerGame` constructor signature changes from `(RulesetOptions)` to `(RulesetOptions, String, String)`, which is a non-breaking change since the constructor is package-private.

## Pros and Cons of the Options

### Option 1 — Game.createServerGame() factory method

- Good: Non-breaking, additive change.
- Good: Named factory makes intent clear.
- Good: Keeps `ServerGame` package-private.
- Bad: Adds a consumer-specific method to the core API.

### Option 2 — Extend createGame() overload

- Good: Single factory entry point for all game types.
- Bad: Pollutes the existing `createGame()` signature which is designed for online/offline application games. A boolean flag or enum parameter to distinguish server vs. application games is semantically confusing.

### Option 3 — Keep new ServerGame() with public visibility

- Good: No change to `core`.
- Bad: Violates Architecture Law 2 explicitly. Makes the law unenforceable.
