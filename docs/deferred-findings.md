# Deferred Findings

Unresolved findings from code reviews. Each entry was marked `[OPEN]` or `[DEFERRED]`
at review time and has not yet been addressed.

---

## 2026-03-10 -- T01: RulesetOptionsTest missing enum count assertion

- **Finding**: `RulesetOptionsTest` is missing `assertEquals(2, RulesetOptions.values().length)` assertion, which was explicitly listed in the plan test requirements.
- **Location**: `modules/core/src/test/java/.../logic/ruleset/RulesetOptionsTest.java`
- **Severity**: Important
- **Status**: DEFERRED
- **Why deferred**: Test file only checks toString and valueOf. The exhaustive `switch` in `Game.createRuleset` provides compile-time safety against additions, but a values-count assertion would catch accidental removal or renaming.
- **Suggested fix**: Add `assertEquals(2, RulesetOptions.values().length, "Exactly STANDARD and CHESS960 must exist")`.

---

## 2026-03-10 -- T03: Missing rookOriginFile=5 test scenario for Board.handleCastleMove

- **Finding**: Plan test requirements specify `rookOriginFile=5, kingDestFile=6` as the kingside test scenario. No test uses `rookOriginFile=5`; all four added tests use `rookOriginFile=6`.
- **Location**: `modules/core/src/test/java/.../data/BoardTest.java`
- **Severity**: Important
- **Status**: DEFERRED
- **Why deferred**: The code path is generic and the covered scenarios (swap edge case, backward compat, queenside) give high confidence the implementation is correct. The missing specific scenario represents a test-coverage gap against plan requirements, not a latent bug.
- **Suggested fix**: Add a test with `rookOriginFile=5, kingDestFile=6`.

---

## 2026-03-10 -- T05: Unused variable in Chess960StartPosition.generate()

- **Finding**: `int[] backRankFiles = new int[8]` is declared at line 71 and never read. Allocated on every call to `generate()` but serves no purpose.
- **Location**: `modules/core/src/main/java/.../logic/ruleset/chess960Ruleset/Chess960StartPosition.java:71`
- **Severity**: Important
- **Status**: DEFERRED
- **Why deferred**: No functional impact. Cosmetic dead code.
- **Suggested fix**: Delete line 71 (`int[] backRankFiles = new int[8];`).

---

## 2026-03-10 -- T07: getLegalSquares test only asserts board dimensions

- **Finding**: `Chess960RulesetTest.getLegalSquares_includesRookSquare_forUnmovedRook` test body only asserts `ruleset.getWidth() == 8` and `ruleset.getHeight() == 8`, not that castling moves are included or check detection filters correctly.
- **Location**: `modules/core/src/test/java/.../logic/ruleset/chess960Ruleset/Chess960RulesetTest.java:330-342`
- **Severity**: Important
- **Status**: DEFERRED
- **Why deferred**: T08 (`Chess960CastlingIntegrationTest`) covers this behavior comprehensively. The misleading test name provides false confidence. Rename to `dimensions_areStandard8x8` or replace body with a real assertion.
- **Suggested fix**: Rename the test or replace its body with a `getLegalSquares` assertion.

---

## 2026-03-10 -- T08: Test file named differently than plan specifies

- **Finding**: File is named `Chess960CastlingIntegrationTest` instead of `Chess960CastlingTest` as specified in the plan.
- **Location**: `modules/core/src/test/java/.../logic/ruleset/chess960Ruleset/Chess960CastlingIntegrationTest.java`
- **Severity**: Important
- **Status**: DEFERRED
- **Why deferred**: The name is arguably more descriptive. Cosmetic inconsistency with the plan.
- **Suggested fix**: Rename to `Chess960CastlingTest` for plan consistency, or leave as-is.

---

## 2026-03-10 -- T09: initializeBoard double-call guard test is a no-op

- **Finding**: `initializeBoard_doubleCallGuard_throwsIllegalStateException` test does not test the guard. The body creates a game and asserts `assertNotNull(game.getRuleset())`. The production guard (`if (board != null) throw IllegalStateException`) is never exercised.
- **Location**: `modules/core/src/test/java/.../logic/game/GameDeferredInitTest.java:115-124`
- **Severity**: Important
- **Status**: DEFERRED
- **Why deferred**: The test name declares it tests specific exception-throwing behavior, but the body tests something else entirely. A regression removing the guard would pass this test.
- **Suggested fix**: Call `initializeBoard(new StandardChessRuleset())` on an already-initialized game and assert `assertThrows(IllegalStateException.class, ...)`.

---

## 2026-03-10 -- T10: Game.movePiece lacks null guard during deferred-init window

- **Finding**: `Game.movePiece` accesses `board` without null guard. Theoretical NPE during the deferred-init window before board is set. Unreachable via normal UI flow since `getLegalSquares` returns empty.
- **Location**: `modules/core/src/main/java/.../logic/game/Game.java` (movePiece method)
- **Severity**: Important
- **Status**: DEFERRED
- **Why deferred**: Unreachable through normal UI flow. Defensive coding improvement only.
- **Suggested fix**: Add null guard at top of `OnlineGame.executeMove`.

---

## 2026-03-10 -- Deep Review: DRY violation in king move generators

- **Finding**: `PossibleChess960KingMoves` (249 lines) duplicates most of `PossibleStandardKingMoves` (145 lines). One-step adjacency moves, `canCastleKingside`/`canCastleQueenside` guards, `canCastleToward` walk logic, and `isInBounds` are all copy-pasted.
- **Location**: `modules/core/src/main/java/.../possibleMoves/PossibleChess960KingMoves.java` and `PossibleStandardKingMoves.java`
- **Severity**: Important
- **Status**: OPEN
- **Suggested fix**: Extract a shared abstract base class `AbstractKingMoveGenerator` with hooks for corridor check and target-square selection. Eliminates approximately 80 lines of duplication.

---

## 2026-03-10 -- Deep Review: DRY violation in getLegalSquares non-castling branch

- **Finding**: `Chess960Ruleset.getLegalSquares` copies the non-castling deep-copy-simulate-check branch verbatim from `AbstractChessRuleset.getLegalSquares`. If the base class is updated, the Chess960 copy will silently diverge.
- **Location**: `modules/core/src/main/java/.../chess960Ruleset/Chess960Ruleset.java:249-258` and `AbstractChessRuleset.java:122-131`
- **Severity**: Important
- **Status**: OPEN
- **Suggested fix**: Extract the shared logic into a private helper method (e.g., `isLegalAfterSimulation`). Chess960 override then only provides its castling branch.

---

## 2026-03-10 -- Deep Review: Wasteful no-arg constructor in Chess960Ruleset

- **Finding**: The no-arg constructor creates dummy `Player` objects and generates a full 8x8 board with 32 pieces just to extract the Scharnagl index, then discards the board.
- **Location**: `modules/core/src/main/java/.../chess960Ruleset/Chess960Ruleset.java:58-68`
- **Severity**: Important
- **Status**: OPEN
- **Suggested fix**: Add `Chess960StartPosition.generateIndex(Random)` that returns the index without constructing a full board.

---

## 2026-03-10 -- Deep Review: Misspelled method name getSudoLegalSquares

- **Finding**: `getSudoLegalSquares` is a misspelling of "pseudo". This is now a protected method visible to all subclasses of `AbstractChessRuleset`.
- **Location**: `modules/core/src/main/java/.../ruleset/AbstractChessRuleset.java:148`
- **Severity**: Important
- **Status**: RESOLVED
- **Resolution**: Renamed to `getPseudoLegalSquares` in the Stage 4 fixes.
- **Suggested fix**: ~~Rename to `getPseudoLegalSquares` across the codebase.~~

---

## 2026-03-10 -- Deep Review: Chess960 transit-square check performance

- **Finding**: For each transit square during Chess960 castling, a full deep copy of the board is created and `isCheck` is run. Up to 6 deep copies per castling candidate per legal-move generation call.
- **Location**: `modules/core/src/main/java/.../chess960Ruleset/Chess960Ruleset.java:327-343`
- **Severity**: Important
- **Status**: OPEN
- **Suggested fix**: Use reverse-attack scan on transit squares directly without full board copies. Or skip transit-square checks in `hasAnyLegalMove` (it only needs to find one legal move).

---

## 2026-03-10 -- Deep Review: deserializeMove silent fallback produces wrong CastleMove

- **Finding**: When `findKingFile` or `findRookFile` returns -1, `deserializeMove` falls back to `Move.fromString` which creates a standard CastleMove with hardcoded king-on-e1 assumptions -- incorrect for Chess960.
- **Location**: `modules/core/src/main/java/.../chess960Ruleset/Chess960Ruleset.java:289-299`
- **Severity**: Important
- **Status**: RESOLVED
- **Resolution**: `deserializeMove` now throws `IllegalStateException` instead of silently falling back to `Move.fromString`. Fixed in the Stage 4 fixes.
- **Suggested fix**: ~~Throw `IllegalStateException` instead of falling back.~~

---

## 2026-03-10 -- Deep Review: Javadoc references private method of another class

- **Finding**: Javadoc on `buildCastleMove` references `Board.handleCastleMove960` (a private method). Fragile if the name changes.
- **Location**: `modules/core/src/main/java/.../chess960Ruleset/Chess960Ruleset.java:149-152`
- **Severity**: Important
- **Status**: OPEN
- **Suggested fix**: Reference `Board.executeMove` (the public entry point) instead.

---

## 2026-03-10 -- Test Coverage: JOIN_CODE Chess960 params to game creator untested

- **Finding**: `ClientHandler` injects `position=N` and `ruleset=CHESS960` into the JOIN_CODE message for the game creator. This code path has no test. If it regresses, both players would see different boards.
- **Location**: `modules/server/src/main/java/.../management/ClientHandler.java`
- **Severity**: Important
- **Status**: OPEN
- **Suggested fix**: Add `chess960GameInstance_whiteJoinCode_containsPositionAndRuleset` test in `GameInstanceTest`.

---

## 2026-03-10 -- Test Coverage: OnlineGame.buildRulesetFromServerParams swallows NumberFormatException

- **Finding**: When `position=abc` (non-numeric), the method catches `NumberFormatException`, logs WARNING, and falls back to a random position. Both players end up with different boards.
- **Location**: `modules/core/src/main/java/.../logic/game/OnlineGame.java:175-181`
- **Severity**: Important
- **Status**: OPEN
- **Suggested fix**: Add a test exercising the NumberFormatException path. Consider failing loudly rather than falling back silently.
