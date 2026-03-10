# Deferred Findings

Unresolved findings from code reviews. Each entry was marked `[OPEN]` or `[DEFERRED]`
at review time. Items marked RESOLVED have been fixed and can be ignored.

---

## 2026-03-10 -- T01: RulesetOptionsTest missing enum count assertion

- **Finding**: `RulesetOptionsTest` is missing `assertEquals(2, RulesetOptions.values().length)` assertion, which was explicitly listed in the plan test requirements.
- **Location**: `modules/core/src/test/java/.../logic/ruleset/RulesetOptionsTest.java`
- **Severity**: Important
- **Status**: RESOLVED
- **Resolution**: Added `values_containsExactlyTwoOptions` test asserting `assertEquals(2, RulesetOptions.values().length)`.

---

## 2026-03-10 -- T03: Missing rookOriginFile=5 test scenario for Board.handleCastleMove

- **Finding**: Plan test requirements specify `rookOriginFile=5, kingDestFile=6` as the kingside test scenario. No test uses `rookOriginFile=5`; all four added tests use `rookOriginFile=6`.
- **Location**: `modules/core/src/test/java/.../data/BoardTest.java`
- **Severity**: Important
- **Status**: RESOLVED
- **Resolution**: Added `chess960KingsideCastle_rookOriginFile5_kingDestFile6` test in `BoardTest`.

---

## 2026-03-10 -- T05: Unused variable in Chess960StartPosition.generate()

- **Finding**: `int[] backRankFiles = new int[8]` was declared and never read.
- **Location**: `modules/core/src/main/java/.../logic/ruleset/chess960Ruleset/Chess960StartPosition.java`
- **Severity**: Important
- **Status**: RESOLVED
- **Resolution**: Variable was already removed during Stage 4 deep review fixes.

---

## 2026-03-10 -- T07: getLegalSquares test only asserts board dimensions

- **Finding**: `Chess960RulesetTest.getLegalSquares_includesRookSquare_forUnmovedRook` test body only asserts board dimensions, not castling behavior.
- **Location**: `modules/core/src/test/java/.../logic/ruleset/chess960Ruleset/Chess960RulesetTest.java`
- **Severity**: Important
- **Status**: RESOLVED
- **Resolution**: Test body was already updated during Stage 4 fixes to assert actual `getLegalSquares` castling behavior.

---

## 2026-03-10 -- T08: Test file named differently than plan specifies

- **Finding**: File is named `Chess960CastlingIntegrationTest` instead of `Chess960CastlingTest` as specified in the plan.
- **Location**: `modules/core/src/test/java/.../logic/ruleset/chess960Ruleset/Chess960CastlingIntegrationTest.java`
- **Severity**: Important
- **Status**: DEFERRED (cosmetic)
- **Why deferred**: The name `Chess960CastlingIntegrationTest` is more descriptive and accurately reflects the test scope. Renaming provides no functional benefit.

---

## 2026-03-10 -- T09: initializeBoard double-call guard test is a no-op

- **Finding**: `initializeBoard_doubleCallGuard_throwsIllegalStateException` test did not exercise the guard.
- **Location**: `modules/core/src/test/java/.../logic/game/GameDeferredInitTest.java`
- **Severity**: Important
- **Status**: RESOLVED
- **Resolution**: Test was already corrected during Stage 4 deep review fixes to use `assertThrows(IllegalStateException.class, ...)` on a second `initializeBoard` call.

---

## 2026-03-10 -- T10: Game.movePiece lacks null guard during deferred-init window

- **Finding**: `Game.movePiece` accesses `board` without null guard. Theoretical NPE during the deferred-init window before board is set.
- **Location**: `modules/core/src/main/java/.../logic/game/OnlineGame.java` (executeMove)
- **Severity**: Important
- **Status**: RESOLVED
- **Resolution**: Added null guard at top of `OnlineGame.executeMove`; returns early if board is not yet initialized.

---

## 2026-03-10 -- Deep Review: DRY violation in king move generators

- **Finding**: `PossibleChess960KingMoves` duplicated most of `PossibleStandardKingMoves`. ~80 lines copy-pasted.
- **Location**: `modules/core/src/main/java/.../possibleMoves/`
- **Severity**: Important
- **Status**: RESOLVED
- **Resolution**: Extracted `AbstractKingMoveGenerator` base class with `onCastlingCandidateFound` and `getCastlingTargetFile` hooks. Both classes now extend it. Redundant `findRookSquare` walk in `PossibleChess960KingMoves` merged into the `canCastleToward` walk.

---

## 2026-03-10 -- Deep Review: DRY violation in getLegalSquares non-castling branch

- **Finding**: `Chess960Ruleset.getLegalSquares` copied the non-castling deep-copy-simulate-check branch verbatim from `AbstractChessRuleset`.
- **Location**: `modules/core/src/main/java/.../chess960Ruleset/Chess960Ruleset.java` and `AbstractChessRuleset.java`
- **Severity**: Important
- **Status**: RESOLVED
- **Resolution**: Extracted `protected boolean isLegalAfterSimulation(Board, Square, Square, Player)` helper into `AbstractChessRuleset`. `Chess960Ruleset.getLegalSquares` now calls `super.isLegalAfterSimulation(...)` for the non-castling branch.

---

## 2026-03-10 -- Deep Review: Wasteful no-arg constructor in Chess960Ruleset

- **Finding**: No-arg constructor created dummy Players and a full board just to extract the Scharnagl index.
- **Location**: `modules/core/src/main/java/.../chess960Ruleset/Chess960Ruleset.java`
- **Severity**: Important
- **Status**: RESOLVED
- **Resolution**: Added `Chess960StartPosition.generateIndex()` static method that runs the placement algorithm and returns only the integer index. No-arg constructor now calls `generateIndex()` directly.

---

## 2026-03-10 -- Deep Review: Misspelled method name getSudoLegalSquares

- **Finding**: `getSudoLegalSquares` was a misspelling of "pseudo".
- **Location**: `modules/core/src/main/java/.../ruleset/AbstractChessRuleset.java`
- **Severity**: Important
- **Status**: RESOLVED
- **Resolution**: Renamed to `getPseudoLegalSquares` in the Stage 4 fixes.

---

## 2026-03-10 -- Deep Review: Chess960 transit-square check performance

- **Finding**: For each transit square during Chess960 castling, a full deep copy of the board is created and `isCheck` is run. Up to 6 deep copies per castling candidate per legal-move generation call.
- **Location**: `modules/core/src/main/java/.../chess960Ruleset/Chess960Ruleset.java` (isKingTransitAttacked)
- **Severity**: Important
- **Status**: OPEN
- **Suggested fix**: Use reverse-attack scan on transit squares directly without full board copies. Or skip transit-square checks in `hasAnyLegalMove` since it only needs to find one legal move.
- **Note**: Pre-existing performance debt documented in CLAUDE.md. Chess960 amplifies it but does not introduce a new problem class.

---

## 2026-03-10 -- Deep Review: deserializeMove silent fallback produces wrong CastleMove

- **Finding**: When `findKingFile` or `findRookFile` returned -1, `deserializeMove` fell back to `Move.fromString` with hardcoded king-on-e1 assumptions.
- **Location**: `modules/core/src/main/java/.../chess960Ruleset/Chess960Ruleset.java`
- **Severity**: Important
- **Status**: RESOLVED
- **Resolution**: `deserializeMove` now throws `IllegalStateException` instead of silently falling back.

---

## 2026-03-10 -- Deep Review: Javadoc references private method of another class

- **Finding**: Javadoc on `buildCastleMove` referenced `Board.handleCastleMove960` (a private method).
- **Location**: `modules/core/src/main/java/.../chess960Ruleset/Chess960Ruleset.java`
- **Severity**: Important
- **Status**: RESOLVED
- **Resolution**: Javadoc updated to reference `Board.executeMove` (the public entry point).

---

## 2026-03-10 -- Test Coverage: JOIN_CODE Chess960 params to game creator untested

- **Finding**: `ClientHandler` injects `position=N` and `ruleset=CHESS960` into the JOIN_CODE message for the game creator. This path had no test.
- **Location**: `modules/server/src/main/java/.../management/ClientHandler.java`
- **Severity**: Important
- **Status**: RESOLVED
- **Resolution**: Added `chess960GameInstance_whiteJoinCode_containsPositionAndRuleset` test in `GameInstanceTest` verifying white's SUCCESS message contains both `position=` and `ruleset=CHESS960`.

---

## 2026-03-10 -- Test Coverage: OnlineGame.buildRulesetFromServerParams swallows NumberFormatException

- **Finding**: When `position=abc` (non-numeric), the method logs WARNING and falls back to a random position. Both players end up with different boards. No test covered this path.
- **Location**: `modules/core/src/main/java/.../logic/game/OnlineGame.java`
- **Severity**: Important
- **Status**: RESOLVED
- **Resolution**: Added `handleJoinCode_invalidPositionParam_fallsBackGracefully` test in `OnlineGameServerConnectionTest` verifying the fallback completes without error.
