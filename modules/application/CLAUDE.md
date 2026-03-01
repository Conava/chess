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
- `io.github.conava.chess.application.network` -- Networking layer: `ServerCommunicationTask` implements `core`'s `ServerConnection` interface, managing the TCP socket to the chess server.
- `io.github.conava.chess.application.tasks` -- Background task for move execution (`ExecuteMove`, a `SwingWorker`).
- `io.github.conava.chess.application.window` -- Top-level windows and screens: main frame, main menu, chess game panel, dialogs (confirm, input, online input, promotion, settings, message, waiting).

## Key Classes

### `io.github.conava.chess.application.Chess`
- **Responsibility:** Application entry point and facade. Contains `main()`. Wraps a `core.Game` instance and delegates all game operations (start, move, state query, observer management) to it. Initializes the Swing GUI via `MainFrame`. Game construction goes through `Game.createGame(boolean, ...)` — no `OfflineGame` or `OnlineGame` constructors are called directly from this class. For online games, `createOnlineGame()` returns a `Game` and follows a three-step pattern: (1) construct `ServerCommunicationTask` with the message handler lambda and start it on a daemon thread; (2) wait on `CountDownLatch` until connection is confirmed, then check `task.isConnected()` — if not connected, set `SERVER_ERROR` state and return early; (3) call `game.connectToServerGame()` only after confirming the connection is live. All facade methods that delegate to `game.*` are null-safe: query methods return `null` or empty collections when no game is active; action methods (`movePiece`, `promoteMove`, `addObserver`, `removeObserver`) throw `IllegalStateException` when no game is active. `getJoinCode()` delegates to `game.getJoinCode()` without any `instanceof` check.
- **Collaborators:** `MainFrame`, `ColorScheme`, core's `Game`, `GameObserver`, `ServerCommunicationTask`.

### `io.github.conava.chess.application.window.MainFrame`
- **Responsibility:** The top-level `JFrame`. Manages screen transitions between `MainMenu` and `ChessGame` panels. Configures window sizing and launches dialogs for offline/online game setup.
- **Collaborators:** `Chess`, `ChessGame`, `MainMenu`, `InputDialog`, `OnlineGameInputDialog`, `Settings`, `ColorScheme`.

### `io.github.conava.chess.application.window.ChessGame`
- **Responsibility:** The in-game `JPanel`. Implements `GameObserver` to react to game state changes. Composes the board panel, top/bottom player panels, and side panels. Handles square click logic, promotion detection, and delegates move execution to `ExecuteMove`. Promotion detection uses `piece.getType() == Pieces.PAWN` via the facade — no direct `Pawn` or `Move` imports. `localBoard` is kept as a cached reference for the `updateBoard()` diff check but is never mutated directly.
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
- **Responsibility:** A single `JButton` representing one board square. Renders piece icons (loaded locally from classpath resources using `Piece.getType()` and `Piece.getPlayer().color()`) and legal-move dot overlays. Icon loading was moved here from `core`'s `Piece` class when the Swing dependency was removed from `core`.
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

### `io.github.conava.chess.application.network.ServerCommunicationTask`
- **Responsibility:** Implements `core`'s `ServerConnection` interface and `Runnable`. Opens a TCP socket to the server, reads lines in a loop, parses each line into a `Message` via `MessageParser`, and dispatches to the `Consumer<Message>` handler injected at construction time. No message deduplication is performed — TCP guarantees ordering and the protocol defines no message IDs. All socket/stream resources (socket, `BufferedReader`, `PrintWriter`) are closed in a `finally` block on every exit path, including normal loop termination. The constructor requires the `messageHandler` as a final field so that no messages can arrive before a handler is registered. The `Chess` facade constructs this, starts it on a daemon thread, and passes it to `OnlineGame.create()` — keeping all socket I/O out of `core`.
- **Collaborators:** `ServerConnection` (core interface), `MessageParser`, `Message`, `CountDownLatch` (signals connection established or failed to the `Chess` facade).

### `io.github.conava.chess.application.tasks.ExecuteMove`
- **Responsibility:** `SwingWorker<Void, Void>` that runs `chess.movePiece()` or `chess.promoteMove()` off the EDT. UI refresh after move completion is driven entirely by the observer notification chain: `Game.executeMove()` → `notifyObservers()` → `ChessGame.onGameStateChanged()` → `SwingUtilities.invokeLater(this::update)`. `ExecuteMove` holds no reference to `ChessGame` and does not call `update()` directly.
- **Collaborators:** `Chess`.

## Design Patterns Identified

### Facade
- **Classes:** `Chess` (application module, not core).
- **How it works:** `Chess` wraps a `core.Game` instance. All UI code (`ChessGame`, `BottomPanel`, `MainFrame`) calls methods on `Chess` rather than touching `Game` directly. `Chess` delegates every call (`startGame`, `movePiece`, `getState`, `addObserver`, etc.) straight through to the underlying `Game`.

### Observer
- **Classes:** `ChessGame` implements `GameObserver`; registers via `chess.addObserver(this)`.
- **How it works:** After `startGame()`, `ChessGame` registers itself as a `GameObserver`. The `onGameStateChanged()` callback dispatches `update()` via `SwingUtilities.invokeLater`, ensuring all Swing UI mutations happen on the EDT even when the callback is triggered from the network thread.

### SwingWorker (Background Task)
- **Classes:** `ExecuteMove` extends `SwingWorker<Void, Void>`.
- **How it works:** Move execution (which may block for online games) runs in `doInBackground()` off the EDT. UI refresh is handled by the observer chain, not by `ExecuteMove` directly.

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
| `startGame` | `void startGame(boolean online, RulesetOptions, String, String, Map<String,String>)` | `online=false` offline, `online=true` online. Delegates construction to `Game.createGame()`. |
| `getState` | `GameState getState()` | Delegates to `game.getState()`. |
| `getBoard` | `Board getBoard()` | Delegates to `game.getBoard()`. |
| `addObserver` | `void addObserver(GameObserver)` | Delegates to `game.addObserver()`. |
| `removeObserver` | `void removeObserver(GameObserver)` | Delegates to `game.removeObserver()`. |
| `endGame` | `void endGame()` | Calls `game.endGame()` (closing connection for online games) then sets `game = null`. No-op if `game` is already `null`. |
| `getCurrentPlayer` | `Player getCurrentPlayer()` | Delegates to `game.getCurrentPlayer()`. |
| `getPlayerWhite` | `Player getPlayerWhite()` | Delegates to `game.getPlayerWhite()`. |
| `getPlayerBlack` | `Player getPlayerBlack()` | Delegates to `game.getPlayerBlack()`. |
| `getPieceAt` | `Piece getPieceAt(Square)` | Delegates to `game.getPieceAt()`. |
| `getLegalSquares` | `List<Square> getLegalSquares(Square)` | Delegates to `game.getLegalSquares()`. |
| `getMoveList` | `List<String> getMoveList()` | Delegates to `game.getMoveList()`. |
| `movePiece` | `void movePiece(Square, Square)` | Throws `IllegalMoveException`. |
| `promoteMove` | `void promoteMove(Square, Square, Pieces)` | Throws `IllegalMoveException`. |
| `getJoinCode` | `String getJoinCode()` | Delegates to `game.getJoinCode()`; returns `null` when no game is active or when the game is offline. |

## Internal Dependencies

```
Chess (facade, entry point)
  |
  +--> network.ServerCommunicationTask (implements core's ServerConnection)
  |      passed into core's OnlineGame constructor as ServerConnection
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
**COMPLIANT.**

### Law 3 -- Observer pattern for all state propagation
**COMPLIANT.**

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

2. **`startGame()` silently refuses if a game already exists.** It logs a warning but does not
   throw or return a status, so the caller has no way to know the start was rejected.

3. **Tests are all empty.** `/home/marlon/source/chess/modules/application/src/test/java/io/github/conava/chess/application/ChessTest.java`
   has 10 `@Test` methods, all with empty bodies. Zero assertions.

4. **German UI strings are hardcoded.** Labels like "Spielzuge", "Am Zug", "Warten", "Spiel
   Verlassen", "Einstellungen sind noch nicht implementiert" are scattered throughout components
   and windows with no i18n support.

5. **`Settings` window is a stub.** It displays "Einstellungen sind noch nicht implementiert"
   (Settings are not yet implemented) and only has an OK button to close.

6. **Unused title images.** Resources contain `newTitleImage1.png` through `newTitleImage9.png`
   but only `chessTitleImage.jpg` is referenced in code.
