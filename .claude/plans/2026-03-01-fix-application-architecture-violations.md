# Fix Application Module Architecture Violations and Tech Debt

**Branch:** `fix/application-architecture-violations`
**Date:** 2026-03-01
**Status:** APPROVED -- all Open Questions answered, plan finalized.

---

## Problem Statement

The `modules/application` module violates two Architecture Laws from CLAUDE.md:

1. **Law 2 (Facade-only API surface):** The application's `Chess.java` facade directly
   instantiates `OfflineGame` and `OnlineGame` (core `Game` subclasses). `ChessGame.java`
   imports `core.data.pieces.Pawn` for an `instanceof` check and imports
   `core.logic.moves.Move` to construct `Move` objects and call `localBoard.executeMove()`
   directly -- all bypassing the facade.

2. **Law 3 (Observer pattern for state propagation):** `ExecuteMove.done()` manually calls
   `chessGame.update()` instead of relying on the observer notification that `Game.executeMove`
   already fires. `ChessGame.clickedOn()` optimistically mutates `localBoard` via
   `localBoard.executeMove(new Move(...))` before the real move completes, creating a
   shadow state that can desync from the real board.

Additionally, the module carries significant tech debt: duplicate panel additions in
`BottomPanel`, overwritten border in `PromotionWindow`, debug output leaking class names,
the `Chess.getJoinCode()` method uses `instanceof OnlineGame` which leaks `Game` subclass
knowledge into the facade, the `int online` parameter is a magic number instead of a
boolean, facade methods NPE when no game is active, and 10 core-logic test files are
misplaced in the application module.

---

## Architecture Law Impact

| Law | Status | Risk from this task |
|-----|--------|---------------------|
| Law 1 -- Module boundaries | COMPLIANT | NONE -- no cross-module dependency violations introduced |
| Law 2 -- Facade-only API | VIOLATED | HIGH -- this task fixes it |
| Law 3 -- Observer for state | PARTIALLY VIOLATED | MEDIUM -- this task fixes it |
| Law 4 -- Core is logic-only | N/A | NONE |
| Law 5 -- Strategy pattern | COMPLIANT | NONE |

---

## Decision Table

| # | Decision | Choice | Rationale |
|---|----------|--------|-----------|
| D1 | How to eliminate direct `OfflineGame`/`OnlineGame` instantiation from the application facade | Add a static factory method `Game.createGame(...)` in core that the facade calls instead of constructors | Keeps construction knowledge inside core. The facade passes parameters; core decides which subclass to create. |
| D2 | How to replace `instanceof Pawn` check in `ChessGame.clickedOn()` | Use `Piece.getType() == Pieces.PAWN` via the facade's `getPieceAt()` | The `Pieces` enum is already part of the public API. This avoids importing any concrete piece class. |
| D3 | How to eliminate `localBoard.executeMove(new Move(...))` optimistic update | Remove the local board mutation entirely; let the observer callback trigger UI refresh | The optimistic update is architecturally wrong (Law 3) and creates desync risk. The observer already fires after every successful move. The `switchIconToSelected` method provides immediate visual feedback. |
| D4 | How to fix `ExecuteMove.done()` calling `chessGame.update()` directly | Remove the `done()` override entirely | `Game.executeMove` -> `notifyObservers()` -> `ChessGame.onGameStateChanged()` -> `SwingUtilities.invokeLater(this::update)` already handles it. The `done()` call is redundant. |
| D5 | How to eliminate `instanceof OnlineGame` in `Chess.getJoinCode()` | Add `getJoinCode()` to `Game` base class returning `null` by default; `OnlineGame` already overrides it | Keeps subclass knowledge inside core; facade just delegates `game.getJoinCode()`. |
| D6 | How to handle the `localBoard` field after removing optimistic mutation | Keep `localBoard` as a cached reference for the `updateBoard()` diff check, but never mutate it directly | `updateBoard()` compares `chess.getBoard()` with `localBoard` to skip redundant repaints. This is a legitimate caching optimization via the facade. |
| D7 | How to expose `connectToServerGame()` and `handleMessage()` through the factory pattern (Q1 = Option A) | Add default no-op `connectToServerGame()` and `handleMessage(Message)` methods to `Game` base class | Simplest approach. No casting, no new interfaces. Same pattern as `endGame()` being a no-op on `OfflineGame`. `OnlineGame` already has the overriding implementations. |
| D8 | Whether `Chess.createOnlineGame()` remains a separate private method (Q2 = Option A) | Keep it as a separate private method | The online setup flow is complex (latch, connection check, message handler wiring) and deserves its own method for readability. |
| D9 | Whether to include null guards on facade methods (Q3 = Option A) | Include null guards in `Chess.java` | Return `null` or empty collections for query methods; throw `IllegalStateException("No active game")` for action methods (`movePiece`, `promoteMove`). Matches existing pattern where `endGame()` already null-checks. |
| D10 | Whether to replace `int online` with a boolean (PI-3 = included) | Replace `int online` with `boolean online` throughout the call chain: `Chess.startGame()`, `ChessGame` constructor, `ChessGame.initializeGame()`, and the `Game.createGame()` factory | Eliminates magic number. A boolean is the correct type for a two-valued parameter with clear true/false semantics. |
| D11 | Whether to move misplaced core tests (PI-4 = included) | Move 10 test files from `modules/application/src/test/` to `modules/core/src/test/`, updating their package declarations from `io.github.conava.chess.application.core.*` to `io.github.conava.chess.core.*` | Tests for core logic must live in the core module. Their current location under the application module is a boundary violation. |

---

## Affected Files

| File | What changes | Why | Cascade Risk |
|------|-------------|-----|--------------|
| `modules/core/.../logic/game/Game.java` | Add static factory `createGame(boolean, ...)`, default `getJoinCode()`, default no-op `connectToServerGame()`, default no-op `handleMessage(Message)` | Provides facade-safe construction, eliminates `instanceof` in app facade, exposes online methods on base type | CROSS-MODULE -- application depends on this |
| `modules/application/.../Chess.java` | Use `Game.createGame()`; replace `instanceof OnlineGame` with `game.getJoinCode()`; change `int online` to `boolean online`; add null guards; remove `OfflineGame`/`OnlineGame` imports; change `createOnlineGame()` return type to `Game` | Fix Law 2, add null safety, clean parameter types | DEPENDENT on Game.java changes |
| `modules/application/.../window/ChessGame.java` | Remove `Pawn`/`Move` imports; replace `instanceof Pawn` with `Pieces.PAWN` check; remove `localBoard.executeMove(new Move(...))`; change `int online` to `boolean online`; fix debug log to use `getType()` not `getClass()` | Fix Law 2 and Law 3, clean parameter types | DEPENDENT on facade changes |
| `modules/application/.../tasks/ExecuteMove.java` | Remove `done()` override; remove `ChessGame` field and parameter | Fix Law 3 (redundant manual update) | DEPENDENT on ChessGame changes (call sites) |
| `modules/application/.../window/MainFrame.java` | Change `ChessGame` constructor calls from `int` to `boolean` for online parameter | PI-3 cascade | DEPENDENT on ChessGame constructor change |
| `modules/application/.../components/BottomPanel.java` | Remove duplicate `middlePanel.add(...)` calls | Bug fix: panels added twice | NONE |
| `modules/application/.../window/PromotionWindow.java` | Fix overwritten border on title label | Bug fix: `MatteBorder` immediately replaced by `EmptyBorder` | NONE |
| 10 test files under `modules/application/src/test/.../core/` | Move to `modules/core/src/test/`, update package declarations | Misplaced core tests | NONE (tests only, no production code cascade) |

---

## Ordered Implementation Tasks (EXECUTOR ONLY)

### Task 1: Add factory method, `getJoinCode()`, `connectToServerGame()`, and `handleMessage()` defaults to `Game.java` in core

**Files to touch:**
- `/home/marlon/source/chess/modules/core/src/main/java/io/github/conava/chess/core/logic/game/Game.java`

**Changes:**

1. Add the following import (if not already present):
   ```java
   import io.github.conava.chess.core.data.io.Message;
   import java.util.Map;
   ```

2. Add a public static factory method:
   ```java
   public static Game createGame(boolean online, RulesetOptions selectedRuleset,
                                  String playerWhiteName, String playerBlackName,
                                  Map<String, String> onlineGameSettings,
                                  ServerConnection connection)
   ```
   - When `online == false`: return `new OfflineGame(selectedRuleset, playerWhiteName, playerBlackName)`
   - When `online == true`: return `OnlineGame.create(selectedRuleset, playerWhiteName, playerBlackName, onlineGameSettings, connection)`

3. Add a public method with default implementation:
   ```java
   public String getJoinCode() { return null; }
   ```
   `OnlineGame` already has a `public String getJoinCode()` method that returns the actual join code -- it will naturally override this default.

4. Add a public no-op method:
   ```java
   public void connectToServerGame() { /* No-op for non-online games */ }
   ```
   `OnlineGame` already has `public void connectToServerGame()` which will override this.

5. Add a public no-op method:
   ```java
   public void handleMessage(Message message) { /* No-op for non-online games */ }
   ```
   `OnlineGame` already has `public void handleMessage(Message message)` which will override this.

**Acceptance criteria:**
- `Game.createGame(false, ...)` returns an `OfflineGame` instance
- `Game.createGame(true, ...)` returns an `OnlineGame` instance
- `game.getJoinCode()` returns `null` for `OfflineGame`
- `game.connectToServerGame()` is a no-op for `OfflineGame`
- `game.handleMessage(msg)` is a no-op for `OfflineGame`
- Core module compiles: `mvn compile -pl modules/core`

**Required Javadoc:**
- `Game.createGame(...)` -- new public static method
- `Game.getJoinCode()` -- new public method
- `Game.connectToServerGame()` -- new public method
- `Game.handleMessage(Message)` -- new public method

---

### Task 2: Refactor `Chess.java` facade -- use factory, add null guards, change `int online` to `boolean online`, remove subclass imports

**Files to touch:**
- `/home/marlon/source/chess/modules/application/src/main/java/io/github/conava/chess/application/Chess.java`

**Changes:**

1. **Change `startGame()` signature** from `int online` to `boolean online`:
   ```java
   public void startGame(boolean online, ...)
   ```
   Update the body: replace `if (online == 1)` with `if (online)`.

2. **Replace offline game construction** in `startGame()`:
   Replace `game = new OfflineGame(selectedRuleset, playerWhiteName, playerBlackName);`
   with `game = Game.createGame(false, selectedRuleset, playerWhiteName, playerBlackName, null, null);`

3. **Replace online game construction** in `startGame()`:
   Replace `game = createOnlineGame(...)` call -- it will still call the private helper, but the helper return type changes (see next point).

4. **Refactor `createOnlineGame()` return type and body:**
   - Change return type from `OnlineGame` to `Game`.
   - Change the `gameHolder` type from `OnlineGame[]` to `Game[]`.
   - Change the message handler lambda from `msg -> gameHolder[0].handleMessage(msg)` -- this now works because `handleMessage(Message)` is on `Game` base class.
   - Replace `OnlineGame onlineGame = OnlineGame.create(...)` with `Game onlineGame = Game.createGame(true, selectedRuleset, playerWhiteName, playerBlackName, onlineGameSettings, task);`
   - `onlineGame.setGameState(GameState.SERVER_ERROR)` already works (method is on `Game`).
   - `onlineGame.connectToServerGame()` now works (method is on `Game` base class, overridden by `OnlineGame`).

5. **Simplify `getJoinCode()`:**
   Replace:
   ```java
   if (game instanceof OnlineGame onlineGame) {
       return onlineGame.getJoinCode();
   }
   return null;
   ```
   with:
   ```java
   return game != null ? game.getJoinCode() : null;
   ```

6. **Add null guards** to all facade methods that delegate to `game.*`:
   - Query methods (`getState`, `getBoard`, `getCurrentPlayer`, `getPlayerWhite`, `getPlayerBlack`, `getPieceAt`, `getLegalSquares`, `getMoveList`): if `game == null`, return `null` (for object returns) or `Collections.emptyList()` / `List.of()` (for list returns).
   - Action methods (`movePiece`, `promoteMove`): if `game == null`, throw `IllegalStateException("No active game")`.
   - Observer methods (`addObserver`, `removeObserver`): if `game == null`, throw `IllegalStateException("No active game")`.

7. **Remove imports:** `OfflineGame`, `OnlineGame`.

**Acceptance criteria:**
- `Chess.java` has zero imports of `OfflineGame` or `OnlineGame`
- `Chess.java` has zero `instanceof` checks against `Game` subclasses
- `startGame()` signature uses `boolean online` not `int online`
- All facade methods are null-safe (no NPE when `game == null`)
- Application module compiles: `mvn compile -pl modules/application -am`

**Required Javadoc:**
- Update `Chess.startGame()` -- parameter type changed, no longer references direct construction
- Update `Chess.getJoinCode()` -- remove `instanceof` reference
- Update `Chess.createOnlineGame()` -- return type changed to `Game`
- Add `@throws IllegalStateException` to `movePiece`, `promoteMove`, `addObserver`, `removeObserver`

---

### Task 3: Update `MainFrame.java` -- change `int online` to `boolean` in `ChessGame` constructor calls

**Files to touch:**
- `/home/marlon/source/chess/modules/application/src/main/java/io/github/conava/chess/application/window/MainFrame.java`

**Changes:**

1. In `switchToOfflineGame()` (line 66), change:
   ```java
   chessGame = new ChessGame(this, chess, colorScheme, 0, selectedRuleset, ...)
   ```
   to:
   ```java
   chessGame = new ChessGame(this, chess, colorScheme, false, selectedRuleset, ...)
   ```

2. In `switchToOnlineGame()` (line 97), change:
   ```java
   chessGame = new ChessGame(this, chess, colorScheme, 1, selectedRuleset, ...)
   ```
   to:
   ```java
   chessGame = new ChessGame(this, chess, colorScheme, true, selectedRuleset, ...)
   ```

**Acceptance criteria:**
- No `int` literal `0` or `1` used for the online parameter
- Application module compiles: `mvn compile -pl modules/application -am`

**Required Javadoc:** None (no public API change)

---

### Task 4: Fix `ChessGame.java` -- remove core internal imports, optimistic board mutation, change `int online` to `boolean`

**Files to touch:**
- `/home/marlon/source/chess/modules/application/src/main/java/io/github/conava/chess/application/window/ChessGame.java`

**Changes:**

1. Remove import of `io.github.conava.chess.core.data.pieces.Pawn` (line 4).
2. Remove import of `io.github.conava.chess.core.logic.moves.Move` (line 13).

3. Change the constructor signature parameter from `int online` to `boolean online`:
   ```java
   public ChessGame(MainFrame mainFrame, Chess chess, ColorScheme colorScheme, boolean online, ...)
   ```

4. Change `initializeGame()` signature from `int online` to `boolean online`:
   ```java
   private void initializeGame(boolean online, ...)
   ```

5. In `initializeGame()`, the call to `chess.startGame(online, ...)` will now pass a `boolean` which matches the updated `Chess.startGame(boolean, ...)` signature from Task 2.

6. In `clickedOn()`, replace the `instanceof Pawn` check (line 334):
   ```java
   if (chess.getPieceAt(selectedSquare) instanceof Pawn && (clickedSquare.getY() == 0 || clickedSquare.getY() == 7))
   ```
   with:
   ```java
   Piece piece = chess.getPieceAt(selectedSquare);
   if (piece != null && piece.getType() == Pieces.PAWN && (clickedSquare.getY() == 0 || clickedSquare.getY() == 7))
   ```

7. In `clickedOn()`, remove the optimistic board mutation line (line 331):
   ```java
   localBoard.executeMove(new Move(selectedSquare, clickedSquare));
   ```

8. In `clickedOn()`, fix the debug log line (line 321) to use `piece.getType()` instead of `piece.getClass()`:
   ```java
   clickedPieceOptional.ifPresent(piece -> LOGGER.info("Piece on clickedSquare: " + piece.getType() + " Player:" + piece.getPlayer().name()));
   ```
   Also remove the `piece.getPlayer().color()` call since it is redundant with the player name.

**Acceptance criteria:**
- `ChessGame.java` has zero imports from `core.data.pieces.Pawn`
- `ChessGame.java` has zero imports from `core.logic.moves.Move`
- No direct `Board.executeMove()` calls from application code
- Constructor and `initializeGame()` use `boolean online` not `int online`
- Application module compiles: `mvn compile -pl modules/application -am`

**Required Javadoc:**
- Update `ChessGame` constructor Javadoc -- parameter type changed
- Update `ChessGame.clickedOn()` Javadoc -- no more optimistic update

---

### Task 5: Remove redundant `done()` from `ExecuteMove.java` and update call sites

**Files to touch:**
- `/home/marlon/source/chess/modules/application/src/main/java/io/github/conava/chess/application/tasks/ExecuteMove.java`
- `/home/marlon/source/chess/modules/application/src/main/java/io/github/conava/chess/application/window/ChessGame.java` (call sites only)

**Changes:**

1. In `ExecuteMove.java`:
   - Remove the `chessGame` field.
   - Remove the `ChessGame` parameter from the constructor. New signature:
     ```java
     public ExecuteMove(Chess chess, Square start, Square end, Pieces promotionPiece)
     ```
   - Remove the `done()` override entirely. The observer pattern handles UI refresh:
     `Game.executeMove()` -> `notifyObservers()` -> `ChessGame.onGameStateChanged()` -> `SwingUtilities.invokeLater(this::update)`.
   - Remove the import of `ChessGame` if present.

2. In `ChessGame.java` `clickedOn()` method, update the two `ExecuteMove` constructor calls:
   - Change `new ExecuteMove(chess, this, selectedSquare, clickedSquare, selectedPiece).execute();`
     to `new ExecuteMove(chess, selectedSquare, clickedSquare, selectedPiece).execute();`
   - Change `new ExecuteMove(chess, this, selectedSquare, clickedSquare, null).execute();`
     to `new ExecuteMove(chess, selectedSquare, clickedSquare, null).execute();`

**Acceptance criteria:**
- `ExecuteMove` has no `done()` override
- `ExecuteMove` has no reference to `ChessGame`
- `ExecuteMove` constructor has 4 parameters: `Chess`, `Square`, `Square`, `Pieces`
- All call sites in `ChessGame` updated to new 4-parameter constructor
- Application module compiles: `mvn compile -pl modules/application -am`

**Required Javadoc:**
- Update `ExecuteMove` class Javadoc -- no longer calls `chessGame.update()` on completion; relies on observer
- Update `ExecuteMove` constructor Javadoc -- `ChessGame` parameter removed

---

### Task 6: Fix `BottomPanel.java` duplicate panel additions

**Files to touch:**
- `/home/marlon/source/chess/modules/application/src/main/java/io/github/conava/chess/application/components/BottomPanel.java`

**Changes:**

1. Remove lines 86-88 which duplicate the additions already made on lines 82-84:
   ```java
   // REMOVE these three lines (they are exact duplicates of lines 82-84):
   middlePanel.add(leftPlaceholder, BorderLayout.WEST);
   middlePanel.add(roundedWhitePanel);
   middlePanel.add(rightPlaceholder, BorderLayout.EAST);
   ```

**Acceptance criteria:**
- Each of `leftPlaceholder`, `roundedWhitePanel`, `rightPlaceholder` is added to `middlePanel` exactly once
- Application module compiles: `mvn compile -pl modules/application -am`

**Required Javadoc:** None (private method, no public API change)

---

### Task 7: Fix `PromotionWindow.java` overwritten border

**Files to touch:**
- `/home/marlon/source/chess/modules/application/src/main/java/io/github/conava/chess/application/window/PromotionWindow.java`

**Changes:**

1. Replace the two consecutive border assignments on lines 38-39:
   ```java
   titleLabel.setBorder(new MatteBorder(0, 0, 1, 0, Color.GRAY));
   titleLabel.setBorder(new EmptyBorder(0, 0, 20, 0));
   ```
   with a single compound border that preserves both effects:
   ```java
   titleLabel.setBorder(BorderFactory.createCompoundBorder(
       new MatteBorder(0, 0, 1, 0, Color.GRAY),
       new EmptyBorder(0, 0, 20, 0)
   ));
   ```
   Add import for `BorderFactory` if not already present (it should be available via `javax.swing.BorderFactory`).

**Acceptance criteria:**
- Title label has both a matte bottom border AND empty border padding in a single compound border
- Only one `setBorder()` call for the title label
- Application module compiles: `mvn compile -pl modules/application -am`

**Required Javadoc:** None (constructor, no public API change)

---

### Task 8: Move misplaced core tests from application module to core module

**Files to move (10 files total):**

Source (application module) -> Destination (core module), with package declaration update:

1. `.../application/src/test/java/io/github/conava/chess/application/core/data/BoardTest.java`
   -> `.../core/src/test/java/io/github/conava/chess/core/data/BoardTest.java`
   Package: `io.github.conava.chess.application.core.data` -> `io.github.conava.chess.core.data`

2. `.../application/src/test/java/io/github/conava/chess/application/core/data/PlayerTest.java`
   -> `.../core/src/test/java/io/github/conava/chess/core/data/PlayerTest.java`
   Package: `io.github.conava.chess.application.core.data` -> `io.github.conava.chess.core.data`

3. `.../application/src/test/java/io/github/conava/chess/application/core/data/SquareTest.java`
   -> `.../core/src/test/java/io/github/conava/chess/core/data/SquareTest.java`
   Package: `io.github.conava.chess.application.core.data` -> `io.github.conava.chess.core.data`

4. `.../application/src/test/java/io/github/conava/chess/application/core/logic/ruleset/StandardChessRulesetTest.java`
   -> `.../core/src/test/java/io/github/conava/chess/core/logic/ruleset/StandardChessRulesetTest.java`
   Package: `io.github.conava.chess.application.core.logic.ruleset` -> `io.github.conava.chess.core.logic.ruleset`

5. `.../application/src/test/java/io/github/conava/chess/application/core/logic/ruleset/possibleMovesTest/PossibleStandardBishopMovesTest.java`
   -> `.../core/src/test/java/io/github/conava/chess/core/logic/ruleset/possibleMovesTest/PossibleStandardBishopMovesTest.java`
   Package: `io.github.conava.chess.application.core.logic.ruleset.possibleMovesTest` -> `io.github.conava.chess.core.logic.ruleset.possibleMovesTest`

6. `.../application/src/test/java/io/github/conava/chess/application/core/logic/ruleset/possibleMovesTest/PossibleStandardKingMovesTest.java`
   -> `.../core/src/test/java/io/github/conava/chess/core/logic/ruleset/possibleMovesTest/PossibleStandardKingMovesTest.java`
   Package: same pattern

7. `.../application/src/test/java/io/github/conava/chess/application/core/logic/ruleset/possibleMovesTest/PossibleStandardKnightMovesTest.java`
   -> `.../core/src/test/java/io/github/conava/chess/core/logic/ruleset/possibleMovesTest/PossibleStandardKnightMovesTest.java`
   Package: same pattern

8. `.../application/src/test/java/io/github/conava/chess/application/core/logic/ruleset/possibleMovesTest/PossibleStandardPawnMovesTest.java`
   -> `.../core/src/test/java/io/github/conava/chess/core/logic/ruleset/possibleMovesTest/PossibleStandardPawnMovesTest.java`
   Package: same pattern

9. `.../application/src/test/java/io/github/conava/chess/application/core/logic/ruleset/possibleMovesTest/PossibleStandardQueenMovesTest.java`
   -> `.../core/src/test/java/io/github/conava/chess/core/logic/ruleset/possibleMovesTest/PossibleStandardQueenMovesTest.java`
   Package: same pattern

10. `.../application/src/test/java/io/github/conava/chess/application/core/logic/ruleset/possibleMovesTest/PossibleStandardRookMovesTest.java`
    -> `.../core/src/test/java/io/github/conava/chess/core/logic/ruleset/possibleMovesTest/PossibleStandardRookMovesTest.java`
    Package: same pattern

**For each file:**
1. Create the destination directory if it does not exist.
2. Copy the file to the new location.
3. Update the `package` declaration on line 1: replace `io.github.conava.chess.application.core` with `io.github.conava.chess.core`.
4. Delete the original file from the application module.
5. After all files are moved, delete the now-empty `core/` directory tree under `modules/application/src/test/java/io/github/conava/chess/application/core/`.

**Acceptance criteria:**
- Zero test files remain under `modules/application/src/test/java/io/github/conava/chess/application/core/`
- The empty `core/` directory tree is deleted from the application test sources
- All 10 test files exist under `modules/core/src/test/java/io/github/conava/chess/core/` with correct package declarations
- Core module tests compile and pass: `mvn test -pl modules/core`
- Application module tests compile and pass: `mvn test -pl modules/application -am`

**Required Javadoc:** None (test files only)

---

## Testing Requirements (TEST-WRITER ONLY)

| Behavior | Suggested test method | What to assert |
|----------|----------------------|----------------|
| `Game.createGame(false, ...)` returns a working offline game | `testCreateOfflineGameViaFactory()` | Returned `Game` is not null; after `startGame()`, `getState()` returns `RUNNING`; `getJoinCode()` returns `null` |
| `Game.createGame(true, ...)` returns an online game | `testCreateOnlineGameViaFactory()` | Returned `Game` is not null; can call `getJoinCode()` without error; can call `connectToServerGame()` without error |
| `Game.getJoinCode()` returns null for offline games | `testGetJoinCodeReturnsNullForOfflineGame()` | `new OfflineGame(...).getJoinCode()` returns `null` |
| `Game.connectToServerGame()` is a no-op for offline games | `testConnectToServerGameNoOpForOffline()` | `new OfflineGame(...).connectToServerGame()` does not throw |
| `Game.handleMessage()` is a no-op for offline games | `testHandleMessageNoOpForOffline()` | `new OfflineGame(...).handleMessage(msg)` does not throw |
| `Chess` facade null guards -- query methods | `testFacadeQueryMethodsReturnNullWhenNoGame()` | Before `startGame()`: `getState()` returns `null`, `getBoard()` returns `null`, `getCurrentPlayer()` returns `null`, `getLegalSquares()` returns empty list, `getMoveList()` returns empty list |
| `Chess` facade null guards -- action methods | `testFacadeActionMethodsThrowWhenNoGame()` | Before `startGame()`: `movePiece(...)` throws `IllegalStateException`, `promoteMove(...)` throws `IllegalStateException` |
| `ExecuteMove` constructor has 4 parameters (no `ChessGame`) | `testExecuteMoveConstructorSignature()` | Verify via reflection that constructor accepts `(Chess, Square, Square, Pieces)` only |

---

## Documentation & Javadoc Requirements (DOCS-KEEPER ONLY)

1. **Update `/home/marlon/source/chess/modules/application/CLAUDE.md`:**
   - Architecture Law Compliance: mark Law 2 as COMPLIANT, mark Law 3 as COMPLIANT.
   - Known Debt: remove items 7 (localBoard mutation), 8 (BottomPanel duplicate adds), 9 (PromotionWindow border), 10 (System.out.println debug), 12 (Move import).
   - Known Debt: remove item 4 (misplaced core tests) -- they are now moved.
   - Update `Chess` key class description: no longer instantiates `OfflineGame`/`OnlineGame` directly; uses `Game.createGame()`.
   - Update `ExecuteMove` key class description: no longer holds `ChessGame` reference; no longer calls `chessGame.update()`.
   - Update `ChessGame` key class description: no longer mutates `localBoard` directly; no longer imports `Pawn` or `Move`.
   - Update Public API table: `startGame` signature uses `boolean online` not `int online`.
   - Update Public API table: `getJoinCode` no longer mentions `instanceof`.

2. **Update `/home/marlon/source/chess/modules/core/CLAUDE.md`:**
   - Add `Game.createGame(boolean, RulesetOptions, String, String, Map, ServerConnection)` to Public API.
   - Add `Game.getJoinCode()` to Public API.
   - Add `Game.connectToServerGame()` to Public API.
   - Add `Game.handleMessage(Message)` to Public API.

3. **Javadoc for new/changed public members:**
   - `Game.createGame(...)` -- full Javadoc with `@param`, `@return`
   - `Game.getJoinCode()` -- Javadoc with `@return`
   - `Game.connectToServerGame()` -- Javadoc noting no-op default
   - `Game.handleMessage(Message)` -- Javadoc noting no-op default
   - `Chess.startGame()` -- updated signature, updated description
   - `Chess.getJoinCode()` -- remove `instanceof` reference
   - `Chess.createOnlineGame()` -- return type changed
   - `Chess.movePiece()`, `Chess.promoteMove()` -- add `@throws IllegalStateException`
   - `ExecuteMove` class and constructor -- updated description
   - `ChessGame` constructor -- parameter type changed
   - `ChessGame.clickedOn()` -- behavior changed

---

## Known Debt Additions

These items were identified during audit and remain OUT OF SCOPE for this task.
The docs-keeper should add them to the appropriate module's CLAUDE.md:

1. **`application` module -- Empty test methods:** `ChessTest.java` has 10 `@Test` methods with
   empty bodies. Zero assertions. (Already documented as Known Debt #3.)

2. **`application` module -- German UI strings hardcoded:** No i18n support. (Already documented
   as Known Debt #5.)

3. **`application` module -- Unused title images:** Resources contain `newTitleImage1-9.png` but
   only `chessTitleImage.jpg` is used. (Already documented as Known Debt #11.)

4. **`application` module -- `Settings` window is a stub.** (Already documented as Known Debt #6.)

---

## Open Questions

All resolved. Answers applied to the Decision Table and all affected tasks.

| # | Question | Answer | Tasks affected |
|---|----------|--------|----------------|
| Q1 | How to expose `connectToServerGame()`/`handleMessage()` | Option A: default no-op methods on `Game` base class | Task 1, Task 2 |
| Q2 | Keep `createOnlineGame()` separate? | Option A: yes, keep it | Task 2 |
| Q3 | Include null guards? | Option A: include them | Task 2 |
| PI-3 | Replace `int online` with boolean? | Yes, include | Tasks 1, 2, 3, 4 |
| PI-4 | Move misplaced core tests? | Yes, include | Task 8 |
