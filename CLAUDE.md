# CLAUDE.md

## Project Overview
Modularized chess game. Java 17, Maven multi-module. Migrating UI from Swing to JavaFX.
All packages under `io.github.conava.chess`.

## Build & Run Commands
[...keep your existing section verbatim...]

## Architecture Laws (Non-Negotiable)
These apply to ALL tasks. Never violate without explicit human approval.

1. **Module boundaries are hard.** `core` has zero dependencies on `application` or `server`.
   `application` and `server` depend on `core` only — never on each other.
2. **The `Chess` façade is the only API surface.** Application and server code must go
   through `Chess.java`. Direct instantiation of `Game` subclasses from outside `core` is banned.
3. **Observer pattern for all state propagation.** UI components must implement `GameObserver`
   and register via `chess.addObserver()`. Polling game state in loops is banned.
4. **`core` is logic-only.** No UI imports, no JavaFX, no Swing, no I/O in `core`.
5. **`Strategy pattern` owns ruleset variation.** New rule variants must implement `Ruleset`,
   not branch inside `Game` or `Chess`.

## Design Patterns in Use
- Observer: `GameObserver` / `Observable` — all state change notifications
- Strategy: `Ruleset` interface — rule variants
- Façade: `Chess.java` — single entry point for all game interaction

## Current Migration Context
- Replacing `modules/application/` Swing UI with JavaFX
- `core` and `server` must not change during UI migration
- JavaFX target: 21. Use FXML + Controller pattern. Entry point extends `Application`.
- All JavaFX UI updates via `Platform.runLater()`. No direct UI mutation from observer callbacks.

## Conventions
- No test code in `src/main`. No production logic in `src/test`.
- Every new public class in `core` needs a unit test.
- CLAUDE.md files are living documents — update them when architecture decisions are made.

## Branch Strategy

All work happens in feature branches. No agent ever commits to `main` directly.

### Naming Pattern
`<type>/<short-slug>`

Types: `feat`, `refactor`, `fix`, `test`, `docs`, `chore`

### Rules
- Architect creates the branch at the start of every task
- All agents (executor, test-writer, docs-keeper, reviewer) use that same branch
- Human reviews the branch and merges manually when satisfied
- Branch is deleted after merge