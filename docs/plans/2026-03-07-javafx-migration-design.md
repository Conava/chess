# JavaFX Migration Design

Date: 2026-03-07
Status: Approved

## Summary

Complete rewrite of the `application` module, replacing all Swing code with JavaFX 21.
`core` and `server` are untouched. The result is a modern, themeable desktop chess UI
using FXML + Controller pattern, CSS-driven theming, and the Java Preferences API for
settings persistence.

---

## Goals

- Delete all Swing code; no mixed Swing/JavaFX coexistence
- Modern UI: card layouts, CSS transitions, polished look
- On-the-fly light/dark mode switching
- Board color theme presets (Classic, Ocean, Walnut)
- Language switching: English and German
- Remember last-used player names
- Settings persisted via `java.util.prefs.Preferences`
- Full feature parity with the existing Swing app (offline game, online game,
  promotion dialog, move list, waiting screen, settings screen)

---

## Architecture

### Approach

Single-Stage FXML Navigation (Approach A). One `Stage` persists for the app lifetime.
Screens are FXML-defined scene graphs swapped in/out via `SceneManager`. Themes are
applied by replacing CSS stylesheets on the active scene.

### Package Structure

```
io.github.conava.chess.application
├── Chess.java                         ← extends javafx.application.Application
├── navigation/
│   └── SceneManager.java              ← swaps scenes into the primary Stage
├── theme/
│   ├── ThemeManager.java              ← singleton; ObjectProperty<Theme>
│   ├── Theme.java                     ← enum: DARK, LIGHT
│   └── BoardTheme.java                ← enum: CLASSIC, OCEAN, WALNUT
├── i18n/
│   └── I18n.java                      ← wraps ResourceBundle; observable string values
├── settings/
│   └── SettingsService.java           ← reads/writes java.util.prefs.Preferences
├── controllers/
│   ├── MainMenuController.java
│   ├── GameController.java            ← implements GameObserver
│   ├── SettingsController.java
│   ├── OfflineSetupController.java
│   ├── OnlineSetupController.java
│   ├── PromotionController.java
│   └── WaitingController.java
├── network/
│   └── ServerCommunicationTask.java   ← same logic, implements core ServerConnection
└── tasks/
    └── ExecuteMove.java               ← javafx.concurrent.Task<Void>
```

### Resources

```
src/main/resources/
├── fxml/
│   ├── main-menu.fxml
│   ├── game.fxml
│   ├── settings.fxml
│   ├── offline-setup.fxml
│   ├── online-setup.fxml
│   ├── promotion.fxml
│   └── waiting.fxml
├── css/
│   ├── base.css           ← layout, fonts, structure (theme-neutral)
│   ├── dark.css           ← dark color variables
│   ├── light.css          ← light color variables
│   └── board/
│       ├── classic.css
│       ├── ocean.css
│       └── walnut.css
├── i18n/
│   ├── messages_en.properties
│   └── messages_de.properties
└── icon/                  ← existing piece PNGs reused as-is
```

---

## Screen Designs

### Main Menu
Full-height split: left panel with logo/title, right panel with navigation buttons
(Local Game, Online Game, Settings, Exit). Buttons use card-style with hover elevation
via CSS transitions.

### Game Screen
Three-column layout:
- Left sidebar: black player card (name + active indicator)
- Center: chessboard — square aspect ratio via `StackPane` binding; legal move dots
  as semi-transparent circle overlays; board squares are `StackPane` nodes with
  `ImageView` piece children; click handling via `setOnMouseClicked`
- Right sidebar: white player card (top), scrollable move history (bottom), Leave Game button

### Settings Screen
Card-based sections:
- **Appearance**: Light/Dark segmented toggle; board theme picker with color swatches
- **Language**: EN / DE toggle
- **Player Defaults**: two text fields for remembered names
- Save / Cancel buttons — all changes committed on Save; Preferences written immediately

### Dialogs
All dialogs are JavaFX `Stage` with `Modality.APPLICATION_MODAL`, styled to match the
active theme via the same CSS swap mechanism. Each has its own FXML + controller.
- `offline-setup.fxml` — player names + ruleset
- `online-setup.fxml` — IP, port, join code, ruleset; join/create toggle
- `promotion.fxml` — piece icon tiles for promotion selection
- `waiting.fxml` — join code display while waiting for opponent

### ColorScheme replacement
`ColorScheme` is eliminated. All colors live in CSS as JavaFX looked-up color variables.
Controllers never reference color values directly.

---

## Data Flow & Threading

### Observer → UI
`GameController` implements `GameObserver`. `onGameStateChanged()` may arrive on any
thread. All UI mutations are wrapped in `Platform.runLater(() -> update())`.

### Move Execution
`ExecuteMove` is a `javafx.concurrent.Task<Void>`. Submitted to a single-thread daemon
`ExecutorService` in `GameController`. On `IllegalMoveException`, `setOnFailed` calls
`Platform.runLater` to show error state. `ExecuteMove` holds no reference to `GameController`;
the observer chain drives all board refresh.

### Online Connection
`ServerCommunicationTask` remains a `Runnable` on a daemon thread. The `CountDownLatch`
handshake in `Chess.createOnlineGame()` is preserved unchanged.

### Theme Switching
`ThemeManager.setTheme(Theme)` calls `scene.getStylesheets().setAll(base.css, theme.css,
boardTheme.css)` on every open scene. Change is instant. `SettingsController` binds the
toggle to `ThemeManager.currentThemeProperty()`.

### Settings Persistence
`SettingsService` reads all preferences on startup and exposes them as JavaFX properties.
On any change, `SettingsService.save()` writes to `java.util.prefs.Preferences` immediately.
All changes take effect without restart.

---

## Error Handling

| Scenario | Handling |
|----------|----------|
| Server connection failure | `GameState.SERVER_ERROR` → JavaFX `Alert` dialog prompting return to menu |
| Illegal move | Silently ignored; board state unchanged, legal squares cleared |
| Preferences read/write failure | Logged; default values used silently |

---

## Testing

### Covered by unit tests
| Class | What is tested |
|-------|----------------|
| `ThemeManager` | Stylesheet list changes on theme switch; property fires |
| `SettingsService` | Read/write round-trips for all settings using an isolated Preferences node |
| `I18n` | Key lookup returns correct strings for EN and DE; observable values update on locale switch |
| `SceneManager` | Correct FXML loaded for each navigation target (mock Stage) |
| `ExecuteMove` | Calls `chess.movePiece()` or `chess.promoteMove()` correctly (mock Chess facade) |
| `ChessTest` | Existing 10 empty test bodies filled with meaningful facade tests |

### Not covered (manual/integration only)
- FXML rendering
- CSS visual correctness
- Dialog interactions

### Framework
JUnit 5 (already in pom.xml). No new test dependencies needed.

---

## Architecture Law Compliance

| Law | Status |
|-----|--------|
| 1. Module boundaries hard | COMPLIANT — pom.xml depends on `core` only |
| 2. Chess facade only API surface | COMPLIANT — all controllers go through `Chess.java` |
| 3. Observer pattern for state | COMPLIANT — `GameController` implements `GameObserver` |
| 4. core is logic-only | NOT APPLICABLE (constrains `core`, not `application`) |
| 5. Strategy pattern for rulesets | COMPLIANT — passes `RulesetOptions` through facade |

---

## Out of Scope

- i18n beyond English and German
- Animations/transitions between screens (can be added later)
- Accessibility (keyboard navigation, screen reader support)
- Board flip (playing black from bottom)
- Game save/load
