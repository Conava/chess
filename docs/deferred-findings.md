# Deferred Findings

Unresolved technical debt and open findings tracked for future work.
Items are added here when a review identifies an issue but it is deferred to a later task.
Resolved items are removed.

---

## core module

### C-001 · `OnlineGame.isLocalPlayerPiece` NPE on empty square
**Severity:** Medium
**File:** `modules/core/src/main/java/io/github/conava/chess/core/logic/game/OnlineGame.java`
**Description:** `isLocalPlayerPiece` calls `board.getSquare(...).getPiece().getPlayer().color()` without a null guard on `getPiece()`. If the UI requests legal squares for an empty square in an online game, this throws `NullPointerException` unchecked.
**Fix:** Add null check on `getPiece()` before dereferencing.

### C-002 · Unit test coverage is partial
**Severity:** Medium
**File:** `modules/core/src/test/`
**Description:** Tests cover `Observable`, `Game.getNewPiece`, `Move.fromString`/`toProtocolString`, and `OnlineGame` server-connection behaviour. Many public classes in `logic/` and `data/` have no tests (e.g., `Board`, `StandardChessRuleset`, individual piece move generators). Architecture Law requires every new public class in `core` to have a unit test.
**Fix:** Add unit tests for `Board`, `StandardChessRuleset`, `Chess960Ruleset`, and all move-generator classes.

### C-003 · `Board.getRowCount()` and `Board.getColCount()` return values are swapped
**Severity:** Low
**File:** `modules/core/src/main/java/io/github/conava/chess/core/data/board/Board.java`
**Description:** `getRowCount()` returns `board[0].length` (inner array = columns) and `getColCount()` returns `board.length` (outer array = rows). `PossibleStandardKingMoves` double-swaps them which cancels out, so behaviour is currently correct by accident. Any new caller using these methods as documented will get incorrect results.
**Fix:** Rename or correct the return values. Audit all callers.

### C-004 · Default player names are in German
**Severity:** Low
**File:** `modules/core/src/main/java/io/github/conava/chess/core/logic/game/Game.java`
**Description:** `Game.getDefaultPlayerName` returns `"Spieler 0 (Weiß)"` / `"Spieler 1 (Schwarz)"`. This is a localisation inconsistency; all other identifiers and comments are in English, and the application's UI text is driven by i18n properties files.
**Fix:** Replace hardcoded German strings with English defaults, or delegate to i18n.

### C-005 · Performance: `hasAnyLegalMove` is O(pieces × moves) per turn
**Severity:** Low (acceptable for human-speed play; risk if AI or server-side usage grows)
**File:** `modules/core/src/main/java/io/github/conava/chess/core/logic/game/Game.java`
**Description:** `hasAnyLegalMove` iterates all pieces of the current player and calls `getLegalSquares` on each, which performs a deep-copy simulation per candidate move. Worst case (stalemate check) is O(P × M) deep copies. Chess960 amplifies this for castling candidates (up to 6 deep copies per candidate). Documented in ADR 0006.
**Fix:** When performance matters, switch to undo/redo (make/unmake) for the inner simulation loop. See ADR 0006 for trade-offs.

---

## application module

### A-001 · `startGame()` silently refuses if a game is already running
**Severity:** Low
**File:** `modules/application/src/main/java/io/github/conava/chess/application/Chess.java`
**Description:** `startGame()` logs a warning and returns without throwing or returning a status code when a game is already running. The caller has no way to detect a rejected start.
**Fix:** Return a boolean or throw `IllegalStateException` from `startGame()` when a game is already active.

### A-002 · Application controller tests are thin
**Severity:** Medium
**File:** `modules/application/src/test/`
**Description:** `ChessTest.java` has placeholder test methods with empty bodies. `SceneManagerTest.java` covers constructor injection and basic overlay guard behaviour. Full controller tests (board rendering, click logic, overlay flows) require a running JavaFX runtime (TestFX or Headless FX) and are not yet in place.
**Fix:** Add TestFX-based integration tests for `GameController` click handling, promotion overlay flow, and game-end overlay.

### A-004 · `SettingsService.loadTheme()` corrupt-value fallback branch is untested
**Severity:** Low
**File:** `modules/application/src/main/java/io/github/conava/chess/application/settings/SettingsService.java`
**Description:** `loadTheme()` wraps `Theme.valueOf(...)` in a try/catch that returns `Theme.DARK_PURPLE` if the stored string does not match any enum constant. This fallback guard is unverified by any test. The logic is a one-liner catch block with low branching complexity, so the risk is minimal.
**Fix:** Add a test `loadThemeReturnsDefaultForUnknownStoredValue` that writes a nonsense value to the Preferences node and asserts `loadTheme()` returns `Theme.DARK_PURPLE`.

### A-003 · `SceneManager.showDialog()` is deprecated
**Severity:** Low
**File:** `modules/application/src/main/java/io/github/conava/chess/application/navigation/SceneManager.java`
**Description:** `showDialog()` exists only for source compatibility and forwards to `showOverlay()`. All callers should be updated to call `showOverlay()` directly.
**Fix:** Update all callers to use `showOverlay()` and remove the deprecated `showDialog()` method.
