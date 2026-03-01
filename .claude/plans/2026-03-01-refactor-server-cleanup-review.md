# Review: refactor/server-cleanup
Date: 2026-03-01
Verdict: APPROVED

## Checklist Results

### Architecture Laws
[PASS] Law 1: No illegal module imports introduced. Core has no dependencies on server or application.
[PASS] Law 2: Chess facade / factory method is the API surface. `GameInstance` uses `Game.createServerGame()` exclusively. No `new ServerGame()` outside core. `ServerGame` constructor is package-private.
[PASS] Law 3: All state changes go through observers. `GameInstance` implements `GameObserver`, registers via `game.addObserver(this)`. Terminal state transitions propagated via observer callback.
[PASS] Law 4: No UI, Swing, JavaFX, or raw I/O in core. No such imports in core changes.
[PASS] Law 5: No ruleset logic added outside the Ruleset interface. No ruleset changes on this branch.

### Implementation Quality
[PASS] Every new or changed public class has class-level Javadoc -- Server.java:19, GameInstance.java:17, ClientHandler.java:21, ServerGame.java:8, Game.createServerGame:128.
[PASS] Every new or changed public method has Javadoc with @param/@return/@throws.
[WARN] Dead code: `GameInstance.connectPlayer(ClientHandler)` one-arg overload at GameInstance.java:106 has zero callers in production code or test code. All callers use the two-arg `connectPlayer(ClientHandler, String)` variant. The Javadoc says "will be removed once all callers are updated" but all callers are already updated on this branch.
[PASS] No System.out.println introduced. The 6 occurrences in `Server.printServerStatus()` (Server.java:187-195) are pre-existing from main.

### Best Practices (Java 17)
[PASS] No raw types or unchecked casts in production code. One `@SuppressWarnings("unchecked")` in test code (GameInstanceTest.java:528) for reflection -- acceptable.
[PASS] No overly broad catch blocks (catch Exception / catch Throwable) in production code.
[PASS] No public mutable state (no public non-final fields).
[PASS] Proper access modifiers. `ServerGame` constructor is package-private.
[PASS] No magic numbers -- `MAX_GAMES = 40`, `DEFAULT_PORT = 54321` are named constants.
[PASS] Resource management -- `ServerSocket` in try-with-resources in `Server.start()`.
[PASS] No deprecated API usage introduced.
[WARN] `Server.getGamesList()` (Server.java:255) returns the live internal `ConcurrentHashMap`. Callers (`ClientHandler`) can mutate it directly. This is intentional for the current design (ClientHandler adds/removes games), but it exposes internal mutable state. Consider returning an unmodifiable view if read-only access is ever needed.

### Test Coverage
[PASS] `Game.createServerGame()` -- tested in CreateServerGameTest.java (7 tests).
[PASS] `GameInstance` -- extensively tested in GameInstanceTest.java (29 tests): deferred creation, factory usage, connectPlayer, observer terminal/non-terminal, move relay, illegal moves, malformed moves, synchronization, disconnect.
[PASS] `ClientHandler` -- integration-tested in ClientHandlerIntegrationTest.java (11 tests): malformed messages, invalid ruleset, invalid gameId, key-value format, shadow bug fix, sendMessage thread safety.
[PASS] `Server` -- tested in ServerTest.java (11 tests): constructor initialization, instance independence.
[PASS] Tests assert behavior, not just absence of exceptions.
[WARN] Missing tests from plan: `getPort_invalidArgs_returnsDefault` (port validation, `getPort` is private static), `consoleThread_isDaemon` (requires starting server with blocking accept loop). Both are difficult to test without refactoring or integration harness. Acceptable omission.

### Documentation
[PASS] `modules/server/CLAUDE.md` -- comprehensively rewritten. Accurately reflects instance-based Server, GameObserver in GameInstance, deferred game creation, synchronized methods, protocol extensions.
[PASS] `modules/core/CLAUDE.md` -- updated with `Game.createServerGame()` in Public API, `ServerGame` constructor signature updated.
[PASS] ADR 0001 (server game creation factory) and ADR 0002 (player name protocol extension) created in `docs/decisions/`.
[PASS] No historical language or branch references in CLAUDE.md files.
[PASS] Commit messages follow `<type>(<scope>): <description>` convention.

### Plan Adherence
[PASS] All 7 declared tasks completed (POM fix, factory method, Server refactor, GameInstance rewrite, ClientHandler refactor, disconnect notification, test update).
[WARN] Undeclared file modified: `.claude/agents/architect.md` -- minor template improvements (added "Execution Notes" section, reformatted "Open Questions" section). Harmless change to agent tooling, not production code.

## Known Debt (for docs-keeper to record)

1. **`connectPlayer(ClientHandler)` one-arg overload is dead code.** GameInstance.java:106. All callers already use the two-arg variant. Should be removed.
2. **`Server.getGamesList()` exposes live mutable map.** Server.java:255. By design for current use, but a potential encapsulation concern if more consumers are added.
3. **Port validation and daemon-thread tests are missing.** `getPort` is private static and `startConsoleCommandListener` blocks, making them difficult to unit test without refactoring.
4. **Pre-existing: `System.out.println` in `Server.printServerStatus()`.** Server.java:187-195. Should use LOGGER instead. Not introduced by this branch.

## Issues Requiring Action

### Executor must fix:
- None required for merge. Optional: remove the dead `connectPlayer(ClientHandler)` one-arg overload at `modules/server/src/main/java/io/github/conava/chess/server/management/GameInstance.java:106` since all callers already use the two-arg version.

### Test-writer must fix:
- None required for merge.

### Docs-keeper must fix:
- Add items 1-3 from Known Debt above to `modules/server/CLAUDE.md` Known Debt section.
- Add item 4 (pre-existing System.out) to `modules/server/CLAUDE.md` Known Debt section if not already present.

## Summary

This branch resolves two Architecture Law violations (Law 2: direct ServerGame instantiation, Law 3: no observer pattern) in the server module, fixes the `joinGame()` shadow bug, converts Server from static to instance-based for testability, extends the protocol to carry player names, adds synchronization for thread safety, and improves error handling for malformed messages. The implementation is thorough, well-documented, and closely follows the plan. Code quality is high: proper Javadoc throughout, good use of Java 17 features (enhanced switch, records), correct synchronization strategy, and comprehensive test coverage (58 new tests across 4 test files). The only actionable item is a dead compatibility overload that can be removed in a follow-up. The branch is ready to merge.

## Merge Support

**Merge commit title:**
`refactor(server): resolve architecture violations, fix bugs, and improve testability`

**Merge commit message:**
Resolve Law 2 and Law 3 violations in the server module. GameInstance now creates
games exclusively via Game.createServerGame() factory (Law 2) and implements
GameObserver for terminal state transitions (Law 3). Convert Server from static
to instance-based design for testability. Fix joinGame() shadow bug that left
gameInstance field null. Extend protocol with playerName parameter for CREATE_GAME
and JOIN_GAME. Add synchronization to processMessage() and sendMessage(). Add
disconnectPlayer() to notify remaining player on disconnect. Fix stale POM artifact
reference. Includes 58 new tests across core and server modules, two ADRs, and
comprehensive CLAUDE.md updates.
