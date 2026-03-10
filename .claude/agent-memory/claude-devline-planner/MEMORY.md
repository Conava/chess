# Planner Agent Memory

## Project Structure
- Java 17, Maven multi-module: core, application (JavaFX 21), server
- All packages under `io.github.conava.chess`
- `Game` constructor initializes board immediately via `createRuleset` + `getStartBoard`
- `OnlineGame` uses static factory `OnlineGame.create(...)` with two-phase construction
- `Chess.java` in application module is the facade (Architecture Law 2)
- Plans go in `docs/plans/` as JSON with companion design doc as MD

## Key Architecture Patterns
- Observer: `GameObserver`/`Observable` for state propagation
- Strategy: `Ruleset` interface for rule variants
- Facade: `Chess.java` single entry point
- `Move.fromString`/`toProtocolString` for wire serialization (protocol format: `e2-e4`, `O-O`, `O-O-O`, `a7-a8=QUEEN`)

## Chess960 Plan (2026-03-10)
- Revised to v2 with: deferred board init for online games, `Ruleset.getGameLabel()`, `Ruleset.deserializeMove()`
- 16 tasks, 4 parallel groups
- Key tasks: T04b (Ruleset interface extensions), T09 (Game deferred init), T10 (OnlineGame refactor)
