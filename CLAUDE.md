# CLAUDE.md

## Project Overview
Modularized chess game. Java 17, Maven multi-module. Migrating UI from Swing to JavaFX.
All packages under `io.github.conava.chess`.

## Standard Operating Procedure (The 10-Step Pipeline)
As the root orchestrator, you MUST drive every task through this exact pipeline:

**Phase 1: Planning & Alignment**
1. **ARCHITECT (Initial):** Create branch, write initial plan.
2. **HUMAN GATE:** Wait for answers to "Open Questions".
3. **ARCHITECT (Update):** Apply human answers to the plan. Wait for "Start execution".

**Phase 2: Execution (Implementation Only)**
4. **EXECUTOR (Loop):** Feed the `executor` strictly ONE task at a time from the "Ordered Implementation Tasks" section ONLY.
5. **EXECUTOR (Continuation):** Repeat until all tasks in that specific section are complete.

**Phase 3: Quality Assurance & Docs**
6. **TEST-WRITER:** Invoke `test-writer` and instruct it to implement the "Testing Requirements" section of the plan.
7. **DOCS-KEEPER:** Invoke `docs-keeper` and instruct it to implement the "Documentation & Javadoc Requirements" section of the plan.

**Phase 4: Review & Merge**
8. **REVIEWER:** Invoke `reviewer` to analyze the branch against Architecture Laws. Write findings to `.claude/plans/YYYY-MM-DD-<slug>-review.md`.
9. **HUMAN GATE (Review Loop):** Wait for human to read the review. Route fixes to the appropriate agent based on the feedback.
10. **HUMAN GATE (Merge):** Wait for the human to manually merge.

## Build & Run Commands

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