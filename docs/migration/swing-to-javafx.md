# Swing to JavaFX Migration

## Status: Complete

The Swing-to-JavaFX migration is finished. The `application` module contains no Swing imports.
Every UI class uses JavaFX 21. The entry point (`Chess`) extends `javafx.application.Application`.

This document is retained for historical context and to document the architecture decisions
made during the migration. The formal ADRs are in `docs/decisions/`.

## What was migrated

The `modules/application` module was rewritten from Swing to JavaFX. The `core` and `server`
modules were not changed during the migration.

### Before migration

- Entry point: a class that used `javax.swing.JFrame` and `javax.swing.SwingUtilities`
- Board rendering: a `JPanel` subclass painting pieces with `Graphics2D`
- Dialogs: `JDialog` instances for promotion, game end, and settings
- No FXML; all layout built programmatically

### After migration

| Component | JavaFX replacement |
|-----------|-------------------|
| `JFrame` | `javafx.stage.Stage` (owned by `SceneManager`) |
| `JPanel` (board) | `javafx.scene.layout.GridPane` of `StackPane` squares |
| `JDialog` | FXML overlay loaded by `OverlayManager` |
| `SwingUtilities.invokeLater` | `Platform.runLater()` |
| Programmatic layout | FXML + CSS |
| Look-and-feel | CSS stylesheets (base + theme + board theme) |

## Architecture decisions made during migration

### FXML + constructor-injected controllers

Controllers receive their dependencies through a constructor rather than through FXML-declared
`fx:controller` attributes. `SceneManager` uses `FXMLLoader.setControllerFactory` to supply
pre-constructed controller instances. This avoids the reflection-based default construction
that FXML would otherwise use, and allows proper dependency injection.

### `OverlayManager` with nested event loop

Modal dialogs are hosted as in-window overlays rather than native `Dialog` windows. The
`OverlayManager` uses `Platform.enterNestedEventLoop` / `Platform.exitNestedEventLoop` so
that callers can read the controller state synchronously after the dialog is dismissed,
without blocking the FX Application Thread.

### `ExecuteMove extends Task<Void>`

Move execution is offloaded to a background thread via `ExecuteMove`, a
`javafx.concurrent.Task<Void>`. This prevents the board click handler from blocking the FX
Application Thread during move validation and server communication. UI refresh is driven by
the observer chain (`notifyObservers` → `Platform.runLater`), not by `Task.setOnSucceeded`.

### `ServerCommunicationTask` implements both `Runnable` and `ServerConnection`

The TCP client for online games runs on a daemon thread. It implements `core`'s
`ServerConnection` interface so that `OnlineGame` can call `sendMessage(String)` without
importing any application classes. Message dispatch to the game uses a `Consumer<Message>`
lambda injected at construction time, which is called from the network read loop.

## Current state of the application module

All eight screens and overlays are implemented:

| FXML file | Controller | Purpose |
|-----------|-----------|---------|
| `main-menu.fxml` | `MainMenuController` | Main menu with offline/online/settings navigation |
| `offline-setup.fxml` | `OfflineSetupController` | Player names and ruleset for offline games |
| `online-setup.fxml` | `OnlineSetupController` | IP, port, join code, ruleset for online games |
| `game.fxml` | `GameController` | In-game board, move list, player info panels |
| `game-end.fxml` | `GameEndController` | End-of-game result overlay |
| `promotion.fxml` | `PromotionController` | Pawn promotion piece selection overlay |
| `waiting.fxml` | `WaitingController` | Waiting-for-opponent overlay with join code |
| `settings.fxml` | `SettingsController` | Theme, board, language, default name settings |

Themes and board color schemes implemented:

| Category | Options |
|----------|---------|
| UI themes | Midnight (`dark-purple.css`), Ember (`dark-charcoal.css`), Manuscript (`light-paper.css`), Fjord (`light-arctic.css`) |
| Board themes | Classic (`classic.css`), Ocean (`ocean.css`), Walnut (`walnut.css`) |

Locales implemented: English (`messages_en.properties`), German (`messages_de.properties`).

## What remains to be done (open items)

These are outstanding items unrelated to the Swing→JavaFX migration itself:

- [x] Chess rules: en passant, check-legality filtering, threefold repetition, fifty-move
  rule, insufficient material, fix castling validation — all implemented
- [ ] UI: keyboard shortcuts, in-game clock, board coordinate labels
- [ ] Server: TLS, reconnection support, authentication, configurable game limit,
  server-side move validation, persistent game history

See the Roadmap section in the root `README.md` for the full list.
