# Application Module -- CLAUDE.md

## Status

**Pre-migration Swing application.** The root CLAUDE.md states a Swing-to-JavaFX migration
is planned, but no JavaFX code exists anywhere in this module. Every UI class is pure Swing
(`JFrame`, `JPanel`, `JDialog`, `JButton`, `SwingWorker`, etc.). There are no FXML files,
no JavaFX controllers, and no `Application` subclass. The module is stable in its current
Swing form but has not yet begun the declared migration.

## Responsibility

This module owns the desktop GUI for the chess application and serves as the executable
entry point (`Chess.main`). It provides a Swing-based main menu, game board rendering,
move-list sidebar, player-status panels, dialog windows (confirmation, input, promotion,
settings, waiting-for-player), and background move execution. It delegates all game logic
to the `core` module via the `Chess` facade class, which wraps `Game` instances. It does
**not** own any chess rules, board data structures, or networking logic -- those belong to
`core` and `server` respectively.

## Package Structure

- `io.github.conava.chess.application` -- Entry point (`Chess.java`), facade wrapping core `Game`.
- `io.github.conava.chess.application.components` -- Reusable Swing UI widgets: board buttons, panels, styled controls, color scheme configuration.
- `io.github.conava.chess.application.tasks` -- Background task for move execution (`ExecuteMove`, a `SwingWorker`).
- `io.github.conava.chess.application.window` -- Top-level windows and screens: main frame, main menu, chess game panel, dialogs (confirm, input, online input, promotion, settings, message, waiting).

## Key Classes

### `io.github.conava.chess.application.Chess`
- **Responsibility:** Application entry point and facade. Contains `main()`. Wraps a `core.Game` instance and delegates all game operations (start, move, state query, observer management) to it. Initializes the Swing GUI via `MainFrame`.
- **Collaborators:** `MainFrame`, `ColorScheme`, core's `OfflineGame`, `OnlineGame`, `GameObserver`.

### `io.github.conava.chess.application.window.MainFrame`
- **Responsibility:** The top-level `JFrame`. Manages screen transitions between `MainMenu` and `ChessGame` panels. Configures window sizing and launches dialogs for offline/online game setup.
- **Collaborators:** `Chess`, `ChessGame`, `MainMenu`, `InputDialog`, `OnlineGameInputDialog`, `Settings`, `ColorScheme`.

### `io.github.conava.chess.application.window.ChessGame`
- **Responsibility:** The in-game `JPanel`. Implements `GameObserver` to react to game state changes. Composes the board panel, top/bottom player panels, and side panels. Handles square click logic, promotion detection, and delegates move execution to `ExecuteMove`.
- **Collaborators:** `Chess`, `MainFrame`, `BoardPanel`, `TopPanel`, `BottomPanel`, `SidePanel`, `ExecuteMove`, `PromotionWindow`, `ConfirmDialog`, `WaitingForPlayerWindow`.

### `io.github.conava.chess.application.window.MainMenu`
- **Responsibility:** Splash/main menu `JPanel` with title image and navigation buttons (local play, online play, settings, exit).
- **Collaborators:** `MainFrame`, `ColorScheme`, `CustomButton`, `ExitButton`.

### `io.github.conava.chess.application.window.InputDialog`
- **Responsibility:** Modal `JDialog` for offline game setup. Collects two player names and a `RulesetOptions` selection.
- **Collaborators:** `CustomTextField`, `CustomComboBox`, `ColorScheme`.

### `io.github.conava.chess.application.window.OnlineGameInputDialog`
- **Responsibility:** Modal `JDialog` for online game setup. Collects IP, port, join code, and ruleset. Validates IP (v4/v6/localhost) and port range. Supports join/create radio toggle.
- **Collaborators:** `CustomTextField`, `CustomComboBox`, `CustomJRadioButton`, `MessageWindow`, `ColorScheme`.

### `io.github.conava.chess.application.window.ConfirmDialog`
- **Responsibility:** Modal yes/no confirmation `JDialog` with rounded shape.
- **Collaborators:** `CustomButton`, `ExitButton`, `ColorScheme`.

### `io.github.conava.chess.application.window.PromotionWindow`
- **Responsibility:** Modal `JDialog` for pawn promotion piece selection. Loads piece icons from resources.
- **Collaborators:** `BoardButton`, `ColorScheme`, core's `Pieces`, `PlayerColor`.

### `io.github.conava.chess.application.window.MessageWindow`
- **Responsibility:** Simple modal message `JDialog` with an OK button.
- **Collaborators:** `CustomButton`, `ColorScheme`.

### `io.github.conava.chess.application.window.Settings`
- **Responsibility:** Placeholder settings `JDialog`. Displays "not yet implemented" message.
- **Collaborators:** `CustomButton`, `ColorScheme`.

### `io.github.conava.chess.application.window.WaitingForPlayerWindow`
- **Responsibility:** Modal `JDialog` shown while waiting for an online opponent. Displays the join code.
- **Collaborators:** `CustomButton`, `ColorScheme`.

### `io.github.conava.chess.application.components.ColorScheme`
- **Responsibility:** Immutable value object holding the full UI color palette and default font. Passed throughout the UI layer.
- **Collaborators:** Uses `java.awt.Color` and `java.awt.Font` only.

### `io.github.conava.chess.application.components.BoardPanel`
- **Responsibility:** `JPanel` containing the 8x8 grid of `BoardButton` instances. Manages piece placement, legal-move dot markers, and click delegation to `ChessGame`.
- **Collaborators:** `BoardButton`, `ChessGame`, `ColorScheme`, core's `Board`, `Square`.

### `io.github.conava.chess.application.components.BoardButton`
- **Responsibility:** A single `JButton` representing one board square. Renders piece icons and legal-move dot overlays.
- **Collaborators:** `ColorScheme`, core's `Piece`, `Square`.

### `io.github.conava.chess.application.components.TopPanel`
- **Responsibility:** `ControlPanel` subclass displaying the black player's name and active-turn indicator.
- **Collaborators:** `MainFrame`, `ColorScheme`, `CustomLabel`.

### `io.github.conava.chess.application.components.BottomPanel`
- **Responsibility:** `ControlPanel` subclass displaying the white player's name, active-turn indicator, and a "leave game" button.
- **Collaborators:** `MainFrame`, `Chess`, `ColorScheme`, `ConfirmDialog`, `ExitButton`.

### `io.github.conava.chess.application.components.SidePanel`
- **Responsibility:** `JPanel` displaying the scrollable move history list.
- **Collaborators:** `CustomLabel`, `CustomScrollPane`, `ColorScheme`.

### `io.github.conava.chess.application.components.ControlPanel`
- **Responsibility:** Trivial `JPanel` subclass that defaults to `setOpaque(false)`. Base class for `TopPanel` and `BottomPanel`.
- **Collaborators:** None.

### `io.github.conava.chess.application.components.CustomButton`
- **Responsibility:** Styled `JButton` with rounded corners and hover color change.
- **Collaborators:** `ColorScheme`.

### `io.github.conava.chess.application.components.ExitButton`
- **Responsibility:** Red-tinted variant of `CustomButton` for destructive/exit actions.
- **Collaborators:** `ColorScheme`.

### `io.github.conava.chess.application.components.CustomTextField`
- **Responsibility:** Styled `JTextField` with rounded corners, focus border highlight, and disable state.
- **Collaborators:** `ColorScheme`.

### `io.github.conava.chess.application.components.CustomComboBox<T>`
- **Responsibility:** Styled `JComboBox` with rounded corners and custom cell renderer.
- **Collaborators:** `ColorScheme`.

### `io.github.conava.chess.application.components.CustomJRadioButton`
- **Responsibility:** Styled `JRadioButton` with rounded background and hover effect.
- **Collaborators:** `ColorScheme`.

### `io.github.conava.chess.application.components.CustomLabel`
- **Responsibility:** `JLabel` that applies `ColorScheme` font and foreground color.
- **Collaborators:** `ColorScheme`.

### `io.github.conava.chess.application.components.CustomScrollPane`
- **Responsibility:** Styled `JScrollPane` with custom scrollbar thumb rendering.
- **Collaborators:** `ColorScheme`.

### `io.github.conava.chess.application.tasks.ExecuteMove`
- **Responsibility:** `SwingWorker<Void, Void>` that runs `chess.movePiece()` or `chess.promoteMove()` off the EDT, then calls `chessGame.update()` on completion.
- **Collaborators:** `Chess`, `ChessGame`.

## Design Patterns Identified

### Facade
- **Classes:** `Chess` (application module, not core).
- **How it works:** `Chess` wraps a `core.Game` instance. All UI code (`ChessGame`, `BottomPanel`, `MainFrame`) calls methods on `Chess` rather than touching `Game` directly. `Chess` delegates every call (`startGame`, `movePiece`, `getState`, `addObserver`, etc.) straight through to the underlying `Game`.

### Observer
- **Classes:** `ChessGame` implements `GameObserver`; registers via `chess.addObserver(this)`.
- **How it works:** After `startGame()`, `ChessGame` registers itself as a `GameObserver`. The `updateFromRemote()` callback triggers `update()` which re-reads game state and refreshes all UI panels. Note: `updateFromRemote()` has a TODO comment acknowledging it should run in a separate thread but currently just calls `update()` directly.

### SwingWorker (Background Task)
- **Classes:** `ExecuteMove` extends `SwingWorker<Void, Void>`.
- **How it works:** Move execution (which may block for online games) runs in `doInBackground()` off the EDT. The `done()` callback invokes `chessGame.update()` on the EDT.

### Theming via Value Object
- **Classes:** `ColorScheme` passed to every UI component constructor.
- **How it works:** A single `ColorScheme` instance is created in `Chess` constructor and threaded through the entire widget tree. All colors, font, and hover colors derive from it.

## Public API

The module boundary is the `Chess` class. These are its public methods, which are the only
way external code (or the UI internally) interacts with core game logic:

| Method | Signature | Notes |
|--------|-----------|-------|
| `main` | `static void main(String[] args)` | Entry point. `"nogui"` arg disables GUI. |
| constructor | `Chess(boolean gui)` | Creates app; `true` launches Swing GUI. |
| `startGame` | `void startGame(int online, RulesetOptions, String, String, Map<String,String>)` | `online=0` offline, `online=1` online. Creates `OfflineGame` or `OnlineGame`. |
| `getState` | `GameState getState()` | Delegates to `game.getState()`. |
| `getBoard` | `Board getBoard()` | Delegates to `game.getBoard()`. |
| `addObserver` | `void addObserver(GameObserver)` | Delegates to `game.addObserver()`. |
| `removeObserver` | `void removeObserver(GameObserver)` | Delegates to `game.removeObserver()`. |
| `endGame` | `void endGame()` | Sets `game = null`. |
| `getCurrentPlayer` | `Player getCurrentPlayer()` | Delegates to `game.getCurrentPlayer()`. |
| `getPlayerWhite` | `Player getPlayerWhite()` | Delegates to `game.getPlayerWhite()`. |
| `getPlayerBlack` | `Player getPlayerBlack()` | Delegates to `game.getPlayerBlack()`. |
| `getPieceAt` | `Piece getPieceAt(Square)` | Delegates to `game.getPieceAt()`. |
| `getLegalSquares` | `List<Square> getLegalSquares(Square)` | Delegates to `game.getLegalSquares()`. |
| `getMoveList` | `List<String> getMoveList()` | Delegates to `game.getMoveList()`. |
| `movePiece` | `void movePiece(Square, Square)` | Throws `IllegalMoveException`. |
| `promoteMove` | `void promoteMove(Square, Square, Pieces)` | Throws `IllegalMoveException`. |
| `getJoinCode` | `String getJoinCode()` | Unsafe cast to `OnlineGame`. Crashes if game is offline. |

## Internal Dependencies

```
Chess (facade, entry point)
  |
  +--> window.MainFrame (top-level JFrame)
  |      |
  |      +--> window.MainMenu (splash screen)
  |      +--> window.ChessGame (game screen, implements GameObserver)
  |      |      |
  |      |      +--> components.BoardPanel --> components.BoardButton
  |      |      +--> components.TopPanel
  |      |      +--> components.BottomPanel --> window.ConfirmDialog
  |      |      +--> components.SidePanel
  |      |      +--> tasks.ExecuteMove (SwingWorker)
  |      |      +--> window.PromotionWindow
  |      |      +--> window.WaitingForPlayerWindow
  |      |
  |      +--> window.InputDialog
  |      +--> window.OnlineGameInputDialog --> window.MessageWindow
  |      +--> window.Settings
  |
  +--> components.ColorScheme (threaded through everything)

All components.* classes depend on ColorScheme.
components.ControlPanel is base class for TopPanel, BottomPanel.
```

## Architecture Law Compliance

### Law 1 -- Module boundaries are hard
**COMPLIANT.** `pom.xml` depends only on `core`. No dependency on `server`.

### Law 2 -- Chess facade is the only API surface
**VIOLATION.** `Chess.java` directly instantiates `OfflineGame` and `OnlineGame` (line 114-115
of `/home/marlon/source/chess/modules/application/src/main/java/io/github/conava/chess/application/Chess.java`).
The root CLAUDE.md says "Direct instantiation of `Game` subclasses from outside `core` is banned."
Additionally, `ChessGame.java` imports and uses `core.data.pieces.Pawn` directly (line 4 of
`/home/marlon/source/chess/modules/application/src/main/java/io/github/conava/chess/application/window/ChessGame.java`)
for an `instanceof` check, and calls `localBoard.executeMove(new Move(...))` directly on a
core `Board` object (line 331), bypassing the facade entirely.

### Law 3 -- Observer pattern for all state propagation
**PARTIAL VIOLATION.** `ChessGame` does implement `GameObserver` and registers correctly.
However, `ExecuteMove.done()` calls `chessGame.update()` directly rather than relying on the
observer notification. The `updateFromRemote()` implementation has a TODO noting it should use
a separate thread but currently just calls `update()` synchronously. Additionally,
`ChessGame.clickedOn()` eagerly mutates `localBoard` via `localBoard.executeMove(new Move(...))`
before the actual move completes through the facade, which is a form of optimistic UI update
that sidesteps the observer flow.

### Law 4 -- core is logic-only
**NOT APPLICABLE** to this module (this law constrains `core`, not `application`).

### Law 5 -- Strategy pattern owns ruleset variation
**COMPLIANT.** This module passes `RulesetOptions` through the facade; it does not branch on
rules internally.

## Known Debt / Gotchas

1. **JavaFX migration has not started.** The existing CLAUDE.md and root CLAUDE.md both describe
   an active Swing-to-JavaFX migration, but the codebase is 100% Swing. There are no FXML files,
   no JavaFX imports, no `Application` subclass, and no `Platform.runLater()` calls. The
   `src/main/resources/fxml/` directory mentioned in the old CLAUDE.md does not exist.

2. **`getJoinCode()` will crash for offline games.** It unconditionally casts `game` to
   `OnlineGame` (line 307 of `Chess.java`). There is no guard; calling this on an offline game
   throws `ClassCastException`.

3. **`endGame()` is just `game = null`.** No observer cleanup, no `game.endGame()` call. Any
   registered observers on the old `Game` instance will remain referenced by the now-orphaned
   object, potentially causing issues if `GameObserver` implementations hold back-references.

4. **`startGame()` silently refuses if a game already exists.** It logs a warning but does not
   throw or return a status, so the caller has no way to know the start was rejected.

5. **Tests are all empty.** `/home/marlon/source/chess/modules/application/src/test/java/io/github/conava/chess/application/ChessTest.java`
   has 10 `@Test` methods, all with empty bodies. Zero assertions.

6. **Core tests are misplaced in the application module.** The test tree contains packages like
   `io.github.conava.chess.application.core.data` and
   `io.github.conava.chess.application.core.logic.ruleset` with tests for `Board`, `Player`,
   `Square`, `StandardChessRuleset`, and piece-specific move tests. These test core logic but
   live under the application module's test source root. They should be in `modules/core`.

7. **German UI strings are hardcoded.** Labels like "Spielzuge", "Am Zug", "Warten", "Spiel
   Verlassen", "Einstellungen sind noch nicht implementiert" are scattered throughout components
   and windows with no i18n support.

8. **`Settings` window is a stub.** It displays "Einstellungen sind noch nicht implementiert"
   (Settings are not yet implemented) and only has an OK button to close.

9. **`ChessGame.clickedOn()` mutates `localBoard` directly.** Line 331 calls
   `localBoard.executeMove(new Move(selectedSquare, clickedSquare))` to optimistically update
   the UI board before the actual move executes through the facade. If the move fails, the
   local board and the real board will be out of sync.

10. **`BottomPanel.addComponents()` adds panels to `middlePanel` twice.** Lines 82-84 and 86-88
    of `/home/marlon/source/chess/modules/application/src/main/java/io/github/conava/chess/application/components/BottomPanel.java`
    add `leftPlaceholder`, `roundedWhitePanel`, and `rightPlaceholder` to `middlePanel` twice.

11. **`PromotionWindow` title border is overwritten.** Line 38-39 of `PromotionWindow.java` sets
    a `MatteBorder` then immediately overwrites it with an `EmptyBorder`, so the matte border
    never renders.

12. **`System.out.println` debug output remains.** `ChessGame.update()` line 194 prints state,
    and `ChessGame.clickedOn()` line 321 prints piece info to stdout.

13. **Unused title images.** Resources contain `newTitleImage1.png` through `newTitleImage9.png`
    but only `chessTitleImage.jpg` is referenced in code.

14. **`ChessGame` imports `Move` from core.** Line 13 of `ChessGame.java` imports
    `io.github.conava.chess.core.logic.moves.Move` and constructs `Move` objects directly,
    which reaches into core internals beyond the facade API.
