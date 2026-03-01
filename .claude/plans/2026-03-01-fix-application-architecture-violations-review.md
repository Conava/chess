# Review: fix/application-architecture-violations
Date: 2026-03-01
Verdict: **APPROVED**

---

## Architecture Laws

[PASS] **Law 1** — No illegal module imports introduced. Application depends only on core; server unchanged.

[PASS] **Law 2** — Chess facade is the only external API entry point.
- `Chess.java` has zero imports of `OfflineGame` or `OnlineGame`.
- All game construction delegates to `Game.createGame(boolean, ...)`.
- `getJoinCode()` delegates to `game.getJoinCode()` with no `instanceof` check.

[PASS] **Law 3** — All state changes go through `notifyObservers()`.
- `ExecuteMove.done()` override removed; observer chain drives all UI refresh.
- `localBoard.executeMove(new Move(...))` optimistic mutation removed from `ChessGame.clickedOn()`.

[PASS] **Law 4** — No UI, Swing, JavaFX, or raw I/O in core. New methods added to `Game.java` contain no UI or I/O.

[PASS] **Law 5** — No ruleset logic added outside the `Ruleset` interface.

---

## Implementation Quality

[PASS] New public methods in `Game.java` have full Javadoc with `@param`/`@return`:
- `Game.createGame(...)`, `Game.getJoinCode()`, `Game.connectToServerGame()`, `Game.handleMessage(Message)`

[WARN] `Game.setGameState(GameState)` at `Game.java:241` is a pre-existing public method with no Javadoc. Not introduced by this branch, but the branch touched this file heavily.

[PASS] No dead code introduced. Removed imports (`OfflineGame`, `OnlineGame`, `Pawn`, `Move`) are cleanly gone.

[WARN] `System.out.println` present in moved `StandardChessRulesetTest.java` (lines 69, 71, 76, 78). Pre-existing; moved as-is from application module by Task 8.

[PASS] No `System.out.println` introduced in any production source file by this branch.

[PASS] No magic numbers introduced. `int online` (0/1) replaced with `boolean online` throughout (`Chess.java:122`, `ChessGame.java:62`, `MainFrame.java:66,97`).

---

## Test Coverage

[PASS] `Game.createGame(false, ...)` — `GameFactoryTest`: non-null, RUNNING state, null join code.

[PASS] `Game.createGame(true, ...)` — `GameFactoryTest`: non-null, `getJoinCode()` and `connectToServerGame()` do not throw.

[PASS] `Game.getJoinCode()` returns null for offline game — `GameFactoryTest`.

[PASS] `Game.connectToServerGame()` no-op for offline — `GameFactoryTest`.

[PASS] `Game.handleMessage()` no-op for offline — `GameFactoryTest`.

[PASS] `Chess` facade null guards (query methods) — `ChessTest.testFacadeQueryMethodsReturnNullWhenNoGame()`.

[PASS] `Chess` facade null guards (action methods + observers) — `ChessTest.testFacadeActionMethodsThrowWhenNoGame()`.

[PASS] `ExecuteMove` 4-parameter constructor verified via reflection — `ChessTest.testExecuteMoveConstructorSignature()`.

[WARN] `BoardTest.testBoard()` has an empty test body. Pre-existing, brought in by Task 8 test move.

[WARN] `StandardChessRulesetTest` has 7 empty test methods and uses `System.out.println` instead of assertions in `testGetLegalMoves()`. Pre-existing.

[WARN] `ChessTest` retains 10 empty stubs (lines 25–74, pre-existing known debt). New tests added by this branch (lines 79–182) have real assertions.

---

## Plan Adherence

[PASS] Task 1 — `Game.java` factory + default methods. All four new methods present with Javadoc.

[PASS] Task 2 — `Chess.java` refactor. No subclass imports; boolean parameter; null guards; factory delegation; `getJoinCode` simplified.

[PASS] Task 3 — `MainFrame.java` boolean update. Lines 66 and 97 use `false` and `true`.

[PASS] Task 4 — `ChessGame.java` cleanup. No `Pawn`/`Move` imports; `boolean online`; `Pieces.PAWN` check; debug log uses `getType()`; no `localBoard.executeMove()`.

[PASS] Task 5 — `ExecuteMove.java` refactor. No `done()` override; no `ChessGame` reference; 4-parameter constructor.

[PASS] Task 6 — `BottomPanel` duplicate panel adds removed.

[PASS] Task 7 — `PromotionWindow` compound border fix applied.

[PASS] Task 8 — All 10 test files in `modules/core/src/test/` with correct `io.github.conava.chess.core.*` package declarations. Zero test files remain under `application/core/`.

---

## Issues Requiring Action (post-merge)

### Test-writer
- `StandardChessRulesetTest.testGetLegalMoves()` — add real assertions or delete; remove `System.out.println` calls.
- `BoardTest.testBoard()` — add a real assertion or remove.

### Docs-keeper
- Add Javadoc to `Game.setGameState(GameState)` at `Game.java:241`.
- Record empty test stubs in `core/CLAUDE.md` Known Debt.

---

## Summary

All two architecture violations fixed cleanly. Law 2 and Law 3 are now fully compliant. Tech debt items (BottomPanel bug, PromotionWindow bug, `int online` magic number, misplaced tests, debug logging) resolved. Test coverage is solid. Documentation accurate.

The only flagged items are pre-existing issues migrated into `core` by the test move — none block merge.

**APPROVED — ready to merge.**
