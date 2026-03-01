# Review: fix/core-violations (reviewer-remarks follow-up)

Date: 2026-03-01
Reviewer: Claude (reviewer agent)
Verdict: **APPROVED WITH MINOR NOTES**

---

## Architecture Law Compliance

**[PASS] Law 1** — No illegal module imports. All core files import only core packages; `ServerCommunicationTask.java` imports only core interfaces.

**[PASS] Law 2** — Chess facade remains the only external API entry point. No new direct instantiation of `Game` subclasses from outside `core`.

**[PASS] Law 3** — Observer pattern intact. `executeMoveFromRemote` calls `super.executeMove(canonical)` which internally calls `notifyObservers()`. No polling introduced.

**[PASS] Law 4** — No UI, Swing, JavaFX, or raw I/O in `core`.

**[PASS] Law 5** — No ruleset logic added outside the `Ruleset` interface.

---

## Coordinate Fix Correctness (primary concern)

**[PASS] `Move.squareToProtocol()` — `Move.java:125`**
`'a' + square.getX()` for file letter, `'1' + square.getY()` for rank digit. `Square(1,4)` → `"e2"`. Correct.

**[PASS] `Move.convertToSquare()` — `Move.java:199`**
`new Square(charAt(1)-'1', charAt(0)-'a')` → y=rank, x=file. `"e2"` → `Square(1,4)`. Round-trips cleanly.

**[PASS] `Move.fromString()` castling squares — `Move.java:153-162`**
- White kingside: `Square(0,4)` → `Square(0,6)` (e1→g1). Correct.
- White queenside: `Square(0,4)` → `Square(0,2)` (e1→c1). Correct.
- Black kingside: `Square(7,4)` → `Square(7,6)` (e8→g8). Correct.
- Black queenside: `Square(7,4)` → `Square(7,2)` (e8→c8). Correct.

**[PASS] `Move.toProtocolString()` direction check — `Move.java:101`**
`this.end.getX() > this.start.getX()` → kingside (x=6 > x=4) = "O-O", queenside (x=2 < x=4) = "O-O-O". Correct.

**[PASS] `Board.handleCastleMove()` direction check — `Board.java:146`**
`endSquare.getX() == 2` checks c-file (queenside). Previously was `getY()` (row). Correct.

**[PASS] `Board.handleCastleMove()` rook square lookups — `Board.java:147-153`**
- Queenside: rook `getSquare(startSquare.getY(), 0)` → `getSquare(startSquare.getY(), 3)`. a-file cleared. Correct.
- Kingside: rook `getSquare(startSquare.getY(), 7)` → `getSquare(startSquare.getY(), 5)`. h-file cleared. Correct.

**[PASS] `OnlineGame.executeMoveFromRemote()` — `OnlineGame.java:295-307`**
Translates via `toBoardSquare()` before reconstructing. All three Move subtypes handled in correct instanceof order (CastleMove, PromotionMove, Move). Canonical squares pass into `Board.executeMove`, so the board's actual grid squares are mutated. Correct.

---

## Exception Safety

**[PASS] `ServerCommunicationTask.run()` — `ServerCommunicationTask.java:79-87`**
Per-message parse and dispatch are wrapped in `try { ... } catch (RuntimeException e)` inside the `while` loop. `readLine()` sits outside the inner try block so an `IOException` propagates to the outer catch and terminates the connection properly. A single malformed message cannot kill the listener thread.

**[WARN] `ServerCommunicationTask.java:97`** — `catch (Exception ignored)` in `finally` block when closing `out`. Should be `catch (IOException ignored)`. Pre-existing issue; not introduced by this branch.

---

## Javadoc Accuracy

**[FAIL] `Move.java:13`** — `public class Move` has NO class-level Javadoc comment. The class was substantially modified in this branch but the class declaration itself has no `/** */` block. Pre-existing omission, but the class was the primary target of this branch.

**[PASS]** All new and modified public/private methods have Javadoc with `@param`, `@return` where applicable:
- `Move.toProtocolString()`, `Move.fromString()` — fully documented with coordinate convention and examples
- `Move.squareToProtocol()`, `Move.convertToSquare()` — private helpers, documented with coordinate convention
- `Board.handleCastleMove()` — private, documented with `getSquare(y, x)` convention and file indices
- `OnlineGame.executeMoveFromRemote()` — private, documents the canonical-square translation step and why it is required
- `ServerCommunicationTask.run()` — updated to mention per-message exception handling
- `Square` constructors, `getX()`, `getY()` — all corrected from "Column/Row" to "row/rank" and "column/file"

---

## Test Quality

**[PASS] T1 — Method renames:** `connectToServerGame_sendsAtLeastOneMessage` (line 71) and `connectToServerGame_sendsCreateGameMessage` (line 76) correctly renamed. Section comment and assertion messages updated.

**[WARN] `OnlineGameServerConnectionTest.java:127`** — `construction_withJoinCode_sendsJoinGameMessage` still uses the misleading `construction_` prefix. The test body explicitly calls `joiningGame.connectToServerGame()` at line 133 before asserting. This was NOT in the plan's T1 list but has the same structural problem as the two methods that were renamed.

**[PASS] T2 — Castling square assertions:** White kingside `Square(0,4)` → `Square(0,6)`, black kingside `Square(7,4)` → `Square(7,6)`, white queenside `Square(0,4)` → `Square(0,2)`. Inline comments explain the row/rank and column/file mapping.

**[PASS] T3 — Thread-leak guard:** `assertFalse(writer.isAlive(), "Writer thread must have terminated within the join timeout — possible deadlock")` present after `writer.join(5_000)` in `ObservableTest.java:109`.

**[PASS] T4 — Coordinate round-trip test:** `fromString_regularMove_producesCorrectBoardCoordinates` parses `"e2-e4"` and asserts `Square(1,4)` start and `Square(3,4)` end. Validates absolute board coordinates, not just internal round-trip fidelity.

**[PASS] T5 — Existing castling round-trip tests** (`toProtocolString_kingsideCastling_roundTrips`, `toProtocolString_queensideCastling_roundTrips`) pass after the coordinate fixes.

**[NOTE] Missing coverage (not in plan):** No test for black queenside castling coordinates (`fromString("O-O-O", BLACK)` → `Square(7,4)`, `Square(7,2)`). Not a plan violation; a coverage gap to address in a future iteration.

---

## CLAUDE.md Accuracy

**[PASS] Root `CLAUDE.md` line 48** — Square coordinate convention added to Conventions section. Accurate.

**[PASS] `modules/core/CLAUDE.md`** — `Square` entry corrected. Known Debt item 3 marked FIXED. Known Debt item 8 (`getRowCount`/`getColCount` name swap) added as documented-only debt. All content accurate and matches implementation.

---

## Plan Adherence

**[PASS]** All five executor tasks completed correctly (Move.java, Board.java, OnlineGame.java, Square.java, ServerCommunicationTask.java).

**[PASS]** All five test tasks (T1–T5) completed.

**[PASS]** All documentation tasks completed.

**[PASS]** No undeclared source files modified.

---

## Issues Requiring Action

### Test-writer must fix:

1. **`OnlineGameServerConnectionTest.java:127`** — Rename `construction_withJoinCode_sendsJoinGameMessage` → `connectToServerGame_withJoinCode_sendsJoinGameMessage`. The test body calls `connectToServerGame()` before asserting — the `construction_` prefix is misleading, same pattern as the two methods already renamed in T1.

### Executor or docs-keeper must address:

2. **`Move.java:13`** — Add a class-level Javadoc comment to `public class Move`. Suggested content: describe it as an immutable start/end `Square` pair representing one chess move, with `toProtocolString()`/`fromString()` for wire serialization and `toString()` for algebraic display. Until fixed, record as Known Debt in `modules/core/CLAUDE.md`.

---

## Pre-existing Known Debt Observed (not introduced by this branch)

- `Move.java:13` — Missing class-level Javadoc
- `ServerCommunicationTask.java:97` — `catch (Exception ignored)` should be `catch (IOException ignored)` in finally block
- `PromotionMove.java:14` — `public final Piece targetPiece` is a public field; should be private with accessor

---

## Summary

All eight reviewer remarks from the prior review are correctly addressed. The five critical coordinate bugs (`squareToProtocol`, `convertToSquare`, `fromString` castling squares, `toProtocolString` direction, `handleCastleMove` rook lookup) are fixed with the correct y=row/rank, x=column/file convention. `executeMoveFromRemote` now translates to canonical board squares before applying moves. `ServerCommunicationTask` listener loop survives per-message `RuntimeException`. All three test quality issues are resolved. CLAUDE.md files are updated accurately.

Two items need follow-up before the branch is closed: rename `construction_withJoinCode_sendsJoinGameMessage` in `OnlineGameServerConnectionTest` (same misleading prefix not caught in T1), and add a class-level Javadoc comment to `Move.java`. Neither blocks merge.
