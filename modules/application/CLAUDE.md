# Application Module -- CLAUDE.md

## Status

**Active JavaFX application.** The Swing-to-JavaFX migration is complete. Every UI class uses
JavaFX. There are no Swing imports anywhere in this module. The entry point is `Chess`, which
extends `javafx.application.Application`.

## Responsibility

This module owns the desktop GUI for the chess application and serves as the executable entry
point (`Chess.main` → `Chess.start`). It provides a JavaFX main menu, game board rendering,
move-list sidebar, player-status panels, overlay dialogs (confirmation, promotion, game-end,
waiting-for-player, online/offline setup, settings), and background move execution. It
delegates all game logic to the `core` module via the `Chess` facade class, which wraps a
`core.Game` instance. It does **not** own any chess rules, board data structures, or
networking protocol logic -- those belong to `core` and `server` respectively.

## Package Structure

- `io.github.conava.chess.application` -- Entry point (`Chess.java` extends `Application`).
  The facade through which all UI code interacts with `core`.

- `io.github.conava.chess.application.controllers` -- FXML controllers for every screen and
  overlay:
  - `GameController` -- in-game screen; implements `GameObserver`; uses `Platform.runLater`
    for all UI updates triggered by observer callbacks.
  - `GameEndController` -- end-of-game overlay showing the result and offering rematch/return.
  - `MainMenuController` -- main menu screen with navigation buttons.
  - `OfflineSetupController` -- overlay for offline game setup (player names, ruleset).
  - `OnlineSetupController` -- overlay for online game setup (IP, port, join code, ruleset).
  - `PromotionController` -- overlay for pawn promotion piece selection.
  - `SettingsController` -- settings screen (theme, board theme, language, player name defaults).
  - `WaitingController` -- overlay shown while waiting for an online opponent.

- `io.github.conava.chess.application.navigation` -- Screen and overlay lifecycle management:
  - `SceneManager` -- loads FXML, swaps full scenes on the primary `Stage`, and delegates
    overlay operations to `OverlayManager`.
  - `OverlayManager` -- hosts modal dialogs as in-window overlays on a root `StackPane`.
    Blocks the FX Application Thread via `Platform.enterNestedEventLoop` so callers can read
    controller state synchronously after the dialog is dismissed.

- `io.github.conava.chess.application.i18n` -- `I18n` -- wraps `ResourceBundle` with an
  observable `Language` property. Supports EN and DE.

- `io.github.conava.chess.application.theme` -- Theme system:
  - `ThemeManager` -- holds the active `Theme` and `BoardTheme`, applies CSS to all registered
    `Scene` objects.
  - `Theme` -- enum of available UI colour themes (e.g. `DARK_PURPLE`). Each value carries a
    CSS file path.
  - `BoardTheme` -- enum of available board colour themes (e.g. `CLASSIC`). Each value carries
    a CSS file path.

- `io.github.conava.chess.application.settings` -- `SettingsService` -- persists and retrieves
  user preferences (theme, board theme, language, default player names) via `java.util.prefs.Preferences`.

- `io.github.conava.chess.application.network` -- `ServerCommunicationTask` -- implements
  `core`'s `ServerConnection` interface and `Runnable`. Manages the TCP socket to the chess
  server.

- `io.github.conava.chess.application.tasks` -- `ExecuteMove` -- extends
  `javafx.concurrent.Task<Void>` to run move execution off the FX Application Thread.

## Key Classes

### `io.github.conava.chess.application.Chess`
- **Responsibility:** Application entry point (`main`/`start`) and sole facade between the UI
  layer and `core` (Architecture Law 2). In `start()`, it instantiates `SettingsService`,
  `ThemeManager`, `I18n`, and `SceneManager`, then shows the main menu. All game interaction
  (start, move, query, observer management) flows through this class.
- **Online game construction:** A three-step pattern. (1) Construct `ServerCommunicationTask`
  with a message-handler lambda and start it on a daemon thread. (2) Await `connectionLatch`;
  if `task.isConnected()` is false, call `onlineGame.setGameState(SERVER_ERROR)` and return
  early. (3) Otherwise call `onlineGame.connectToServerGame()`.
- **Null-safety:** Query methods (`getState`, `getBoard`, `getCurrentPlayer`, etc.) return
  `null` or empty collections when no game is active. Action methods (`movePiece`,
  `promoteMove`, `addObserver`, `removeObserver`) throw `IllegalStateException` when no game
  is active.
- **Collaborators:** `SceneManager`, `ThemeManager`, `I18n`, `SettingsService`,
  `ServerCommunicationTask`, core's `Game`, `GameObserver`.

### `io.github.conava.chess.application.navigation.SceneManager`
- **Responsibility:** Owns the primary `Stage`. Loads FXML via `FXMLLoader` using constructor-
  injected controllers (controller factory overrides FXML-declared class). Provides
  `showMainMenu()`, `showGame()`, `showSettings()`, `showOverlay()`, `showConfirm()`, and
  `dismissOverlay()`. On first scene load, creates the root `StackPane` and `OverlayManager`.
- **Collaborators:** `OverlayManager`, all controllers, `ThemeManager`, `I18n`, `Chess`.

### `io.github.conava.chess.application.navigation.OverlayManager`
- **Responsibility:** Hosts FXML-based overlays and inline confirmation cards on top of the
  root `StackPane` by pushing/popping a dimmed backdrop + content node. Uses
  `Platform.enterNestedEventLoop` / `Platform.exitNestedEventLoop` to block the caller
  synchronously while keeping the UI responsive. Supports a stack of nested overlays.
- **Collaborators:** `I18n`.

### `io.github.conava.chess.application.controllers.GameController`
- **Responsibility:** In-game screen controller. Implements `GameObserver`; registers via
  `chess.addObserver(this)` in `initialize()`. Observer callback dispatches all UI mutations
  via `Platform.runLater(this::update)`. Builds the 8x8 board as a `GridPane` of `StackPane`
  squares with responsive `NumberBinding`-based sizing. Handles square click logic, legal-move
  dot marker animation, promotion detection, and delegates move execution to `ExecuteMove` via
  a single-thread `ExecutorService`.
- **Collaborators:** `Chess`, `SceneManager`, `ThemeManager`, `I18n`, `ExecuteMove`,
  `PromotionController`, `WaitingController`, `GameEndController`.

### `io.github.conava.chess.application.controllers.GameEndController`
- **Responsibility:** End-of-game overlay. Displays the game result (checkmate, stalemate,
  etc.) and winner/draw information. Offers "Return to Menu" and "Rematch" actions. The
  caller reads `getChoice()` after the overlay is dismissed to decide what to do next.
- **Collaborators:** `I18n`.

### `io.github.conava.chess.application.controllers.MainMenuController`
- **Responsibility:** Main menu screen. Handles navigation: launches offline setup overlay,
  online setup overlay, settings screen, or exits the application. Reads controller state
  after overlays are dismissed to decide whether to start a game.
- **Collaborators:** `SceneManager`, `I18n`, `OfflineSetupController`, `OnlineSetupController`.

### `io.github.conava.chess.application.controllers.OfflineSetupController`
- **Responsibility:** Offline game setup overlay. Collects player names and `RulesetOptions`.
  The caller checks `isConfirmed()`, `getPlayerWhite()`, `getPlayerBlack()`, `getRuleset()`.
- **Collaborators:** `I18n`, `SettingsService`.

### `io.github.conava.chess.application.controllers.OnlineSetupController`
- **Responsibility:** Online game setup overlay. Collects server IP, port, join code, ruleset,
  and player names. Validates input before confirmation. Exposes `isConfirmed()`, `getIp()`,
  `getPort()`, `getJoinCode()`, `getRuleset()`, `getPlayerWhite()`, `getPlayerBlack()`.
- **Collaborators:** `I18n`, `SettingsService`.

### `io.github.conava.chess.application.controllers.PromotionController`
- **Responsibility:** Pawn promotion overlay. Displays piece icons for the promoting player's
  colour and captures the user's selection. The caller reads `getSelectedPiece()` after
  dismissal.
- **Collaborators:** `I18n`.

### `io.github.conava.chess.application.controllers.SettingsController`
- **Responsibility:** Settings screen. Allows the user to change UI theme, board theme,
  language, and default player names. Persists changes via `SettingsService` and applies them
  immediately via `ThemeManager` and `I18n`.
- **Collaborators:** `SceneManager`, `ThemeManager`, `I18n`, `SettingsService`.

### `io.github.conava.chess.application.controllers.WaitingController`
- **Responsibility:** Overlay shown while waiting for an online opponent to join. Displays the
  join code and a cancel button. The caller checks `isCancelled()` after dismissal.
- **Collaborators:** `I18n`.

### `io.github.conava.chess.application.theme.ThemeManager`
- **Responsibility:** Tracks active `Theme` and `BoardTheme` as observable properties. Applies
  three CSS stylesheets (`base.css`, theme CSS, board theme CSS) to every registered `Scene`.
- **Collaborators:** `Theme`, `BoardTheme`.

### `io.github.conava.chess.application.i18n.I18n`
- **Responsibility:** Wraps `ResourceBundle` with an observable `Language` property. Provides
  `get(key)` for string lookup. Supports EN and DE locales. The bundle is reloaded when the
  language changes.
- **Collaborators:** None (pure wrapper).

### `io.github.conava.chess.application.settings.SettingsService`
- **Responsibility:** Persists and retrieves user preferences via `java.util.prefs.Preferences`.
  Accepts an injected `Preferences` node in its single-arg constructor for testability.
- **Collaborators:** `Theme`, `BoardTheme`, `I18n.Language`.

### `io.github.conava.chess.application.network.ServerCommunicationTask`
- **Responsibility:** Implements `core`'s `ServerConnection` interface and `Runnable`. Opens a
  TCP socket, reads lines in a loop, parses each into a `Message` via `MessageParser`, and
  dispatches to the `Consumer<Message>` handler injected at construction. Uses
  `CountDownLatch` to signal the `Chess` facade once the connection is established or has
  failed. All socket resources are closed in a `finally` block on every exit path.
- **Collaborators:** `ServerConnection` (core interface), `MessageParser`, `Message`.

### `io.github.conava.chess.application.tasks.ExecuteMove`
- **Responsibility:** `javafx.concurrent.Task<Void>` that runs `chess.movePiece()` or
  `chess.promoteMove()` off the FX Application Thread. UI refresh after move completion is
  driven by the observer chain (`Game.executeMove()` → `notifyObservers()` →
  `GameController.onGameStateChanged()` → `Platform.runLater`). Holds no reference to
  `GameController`.
- **Collaborators:** `Chess`.

## Public API

The module boundary is the `Chess` class. All UI code interacts with `core` only through
these methods:

| Method | Signature | Notes |
|--------|-----------|-------|
| `main` | `static void main(String[] args)` | Entry point. `"nogui"` param skips GUI. |
| `start` | `void start(Stage)` | JavaFX lifecycle method. Bootstraps services and `SceneManager`. |
| `startGame` | `void startGame(boolean, RulesetOptions, String, String, Map<String,String>)` | `online=false` offline, `online=true` online. Delegates construction to `Game.createGame()`. No-op (with log warning) if a game is already running. |
| `getState` | `GameState getState()` | Returns `null` when no game is active. |
| `getBoard` | `Board getBoard()` | Returns `null` when no game is active. |
| `addObserver` | `void addObserver(GameObserver)` | Throws `IllegalStateException` when no game is active. |
| `removeObserver` | `void removeObserver(GameObserver)` | Throws `IllegalStateException` when no game is active. |
| `endGame` | `void endGame()` | Calls `game.endGame()` then sets `game = null`. No-op if already `null`. |
| `getCurrentPlayer` | `Player getCurrentPlayer()` | Returns `null` when no game is active. |
| `getPlayerWhite` | `Player getPlayerWhite()` | Returns `null` when no game is active. |
| `getPlayerBlack` | `Player getPlayerBlack()` | Returns `null` when no game is active. |
| `getPieceAt` | `Piece getPieceAt(Square)` | Returns `null` when no game is active. |
| `getLegalSquares` | `List<Square> getLegalSquares(Square)` | Returns empty list when no game is active. |
| `getMoveList` | `List<String> getMoveList()` | Returns empty list when no game is active. |
| `movePiece` | `void movePiece(Square, Square)` | Throws `IllegalMoveException`. Throws `IllegalStateException` when no game is active. |
| `promoteMove` | `void promoteMove(Square, Square, Pieces)` | Throws `IllegalMoveException`. Throws `IllegalStateException` when no game is active. |
| `getJoinCode` | `String getJoinCode()` | Returns `null` when no game is active or for offline games. |

## Internal Dependencies

```
Chess (Application subclass, facade, entry point)
  |
  +--> SettingsService (Preferences persistence)
  +--> ThemeManager --> Theme, BoardTheme
  +--> I18n (ResourceBundle wrapper)
  +--> SceneManager
  |      |
  |      +--> OverlayManager (nested-event-loop overlay hosting)
  |      |
  |      +--> MainMenuController
  |      |      +--> OfflineSetupController (overlay)
  |      |      +--> OnlineSetupController  (overlay)
  |      |
  |      +--> GameController (implements GameObserver)
  |      |      +--> ExecuteMove (Task<Void>, off-thread move execution)
  |      |      +--> PromotionController (overlay)
  |      |      +--> WaitingController   (overlay)
  |      |      +--> GameEndController   (overlay)
  |      |
  |      +--> SettingsController
  |
  +--> ServerCommunicationTask (implements core's ServerConnection, Runnable)
         passed to core's OnlineGame via Game.createGame()
```

## Design Patterns

### Facade
- **Class:** `Chess` (application module -- not to be confused with `core.Chess`).
- **How it works:** Wraps a `core.Game` instance. All controllers call methods on `Chess`
  rather than touching `Game` directly. Every facade method delegates straight through to the
  underlying `Game`.

### Observer
- **Classes:** `GameController` implements `GameObserver`; registers via `chess.addObserver(this)`.
- **How it works:** After `chess.startGame()`, `GameController` registers itself. The
  `onGameStateChanged()` callback dispatches all UI mutations via `Platform.runLater`, ensuring
  updates happen on the FX Application Thread even when the callback originates from a network
  or background thread.

### FXML + Controller
- **How it works:** Every screen and overlay is defined in an FXML file under
  `src/main/resources/fxml/`. Controllers are constructed with dependencies by the caller and
  injected via `FXMLLoader.setControllerFactory`. This avoids reflection-based construction and
  allows constructor injection of collaborators.

### Strategy
- **How it works:** `RulesetOptions` is collected from setup overlays and passed through
  `chess.startGame()` to `Game.createGame()` in `core`. The application module never branches
  on rules internally.

## Architecture Law Compliance

### Law 1 -- Module boundaries are hard
**COMPLIANT.** `pom.xml` depends only on `core`. No dependency on `server`.

### Law 2 -- Chess facade is the only API surface
**COMPLIANT.** All controller code reaches `core` exclusively through `Chess` methods. No
`Game` subclass is instantiated directly from this module.

### Law 3 -- Observer pattern for all state propagation
**COMPLIANT.** `GameController` implements `GameObserver` and reacts to `onGameStateChanged()`
callbacks. There are no polling loops.

### Law 4 -- core is logic-only
**NOT APPLICABLE** to this module (this law constrains `core`, not `application`).

### Law 5 -- Strategy pattern owns ruleset variation
**COMPLIANT.** This module passes `RulesetOptions` through the facade; it never branches on
ruleset logic internally.

## Known Debt

1. **`startGame()` silently refuses if a game already exists.** It logs a warning but does
   not throw or return a status, so the caller has no way to detect a rejected start.

2. **Application tests are thin.** `ChessTest.java` has placeholder test methods with empty
   bodies. `SceneManagerTest.java` covers constructor injection and basic overlay guard
   behaviour. Full controller tests (board rendering, click logic, overlay flows) require a
   running JavaFX runtime (TestFX or Headless FX) and are not yet in place.

3. **`SceneManager.showDialog()` is deprecated.** The method exists only for source
   compatibility and forwards to `showOverlay()`. All callers should be updated to call
   `showOverlay()` directly and the deprecated method removed.
