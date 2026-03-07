# UI Overhaul Design

Date: 2026-03-07
Status: Approved

## Summary

Visual and structural overhaul of the JavaFX application module. No changes to `core` or `server`.
Five problem areas addressed: color depth, button consistency, OS-window dialogs, board scaling, and the game-end experience.

---

## Problem Statement

1. **Flat background** — `#121212` dark background has no surface hierarchy; everything looks the same depth.
2. **Inconsistent buttons** — `nav-button`, `primary-button`, `danger-button` have different padding, sizing, and hover behavior with no shared visual language.
3. **OS-window dialogs** — All dialogs (offline-setup, online-setup, promotion, waiting, game-end, leave-game confirmation) open as separate OS `Stage` windows with title bars and window controls.
4. **Board does not scale** — Squares hardcoded at `72×72px`; rank/file labels hardcoded to `72`; right sidebar too narrow, truncating "Leave Game".
5. **Game-end screen out of place** — Raw `javafx.scene.control.Alert` is un-themed and jarring.

---

## Approach

**CSS-first, minimal Java changes.** Rework CSS and FXML. Replace `SceneManager.showDialog()` with a new `OverlayManager` that injects a dimmed `StackPane` into the root of the current scene. Board scaling is fixed by binding square/label/piece sizes to the grid. All existing controller logic is preserved.

---

## Section 1: Color System

The dark theme gains a 3-level surface hierarchy:

| Token | Old value | New value | Role |
|---|---|---|---|
| `app-bg` | `#121212` | `#0f0f1a` | Window background |
| `app-surface` | `#1e1e2e` | `#1a1b2e` | Cards, panels |
| `app-card` | `#252538` | `#22243a` | Nested card backgrounds |
| `app-elevated` | *(none)* | `#2a2c45` | Hover states, selected items |
| `app-border` | `#3a3a5c` | `#2e3058` | Subtle dividers |
| `app-primary` | `#7c3aed` | `#7c3aed` | Unchanged |
| `app-primary-hover` | `#5b21b6` | `#6d28d9` | Slightly lighter hover |
| `app-text` | `#f0f0f5` | `#e8e8f0` | Body text |
| `app-subtext` | `#9090a8` | `#8585a0` | Labels, hints |

- Cards get `dropshadow(gaussian, rgba(0,0,0,0.35), 12, 0, 0, 3)` to lift off the background.
- Light theme color tokens adjusted proportionally (surface hierarchy preserved).

---

## Section 2: Button System

All buttons share `border-radius: 8` and unified vertical padding. Three roles:

| Class | Fill | Border | Text | Use |
|---|---|---|---|---|
| `.btn-primary` | `app-primary` | none | white | Save, Start, Connect, Return to Menu |
| `.btn-ghost` | transparent | `app-border` | `app-text` | Cancel, secondary actions |
| `.btn-danger` | `app-danger` | none | white | Leave Game, Exit |
| `.nav-button` | `app-surface` | `app-border` | `app-text` | Main menu navigation |

Hover behavior:
- `.btn-primary:hover` → `app-primary-hover` fill
- `.btn-ghost:hover` → `app-primary` border + text
- `.btn-danger:hover` → `app-danger-dark` fill
- `.nav-button:hover` → `app-elevated` fill + `3px` left `app-primary` border accent

All hover states use `translateY(-1px)` lift. No `scaleX/Y` (prevents layout jitter).

Rename in all FXML and controllers: `primary-button` → `btn-primary`, `danger-button` → `btn-danger`.

---

## Section 3: OverlayManager

Replaces `SceneManager.showDialog()`. Dialogs render inside the main window as a full-scene dimmed overlay — no separate OS windows.

### Architecture

- `SceneManager.swapScene()` wraps every FXML root in a persistent `StackPane` container (index 0 = page content, index 1 = overlay when active).
- New class: `io.github.conava.chess.application.navigation.OverlayManager`
  - `showOverlay(String fxmlPath, Object controller)` — loads FXML, wraps in centered card inside a `Region` with `rgba(0,0,0,0.55)` fill, appends to root `StackPane`, blocks via `CountDownLatch`.
  - `dismiss()` — removes overlay node, releases latch.
- `SceneManager` holds an `OverlayManager` field. `showDialog()` replaced by `showOverlay()` — same signature, callers unchanged.
- Clicking the dim backdrop does nothing (all dialogs require explicit action).

### Dialogs converted to overlays

| FXML | Controller | Trigger |
|---|---|---|
| `offline-setup.fxml` | `OfflineSetupController` | Local Game button |
| `online-setup.fxml` | `OnlineSetupController` | Online Game button |
| `promotion.fxml` | `PromotionController` | Pawn reaches back rank |
| `waiting.fxml` | `WaitingController` | Online game, waiting for opponent |
| `game-end.fxml` *(new)* | `GameEndController` *(new)* | Game state: checkmate/draw/stalemate |
| Inline leave-confirm overlay | inline in `GameController` | Leave Game button |

---

## Section 4: Board Scaling

### Fixes

1. Each board `StackPane` square's `prefWidth` and `prefHeight` bind to `grid.width / 8` and `grid.height / 8`.
2. Rank labels bind `prefHeight` to `squareSize`. File labels bind `prefWidth` to `squareSize`.
3. Piece `ImageView`: `fitWidth = squareSize * 0.78`, `fitHeight = squareSize * 0.78`.
4. Legal move dot radius binds to `squareSize * 0.19`.
5. `boardContainer` gets `minWidth="480"` and `minHeight="480"` in `game.fxml`.
6. Right sidebar: `minWidth="220" prefWidth="250"` — enough to always show "Leave Game" untruncated.

### FXML change

`game.fxml` right sidebar `VBox`: change `minWidth="180" maxWidth="280"` → `minWidth="220" prefWidth="250" maxWidth="300"`.

---

## Section 5: Game-End Overlay & Main Menu

### New `game-end.fxml` + `GameEndController`

Card contents (top to bottom):
- **Outcome badge** — colored `Label` with rounded background: green (win), amber (draw), gray (stalemate)
- **Outcome title** — e.g. "White wins!" — `28px bold`
- **Subtitle** — e.g. "Checkmate" — `app-subtext`
- **Divider**
- **Player names row** — white player name vs black player name
- **Move count** — e.g. "42 moves played"
- **Button row** — `btn-primary` "Return to Menu" + `btn-ghost` "Rematch" (offline only)

`GameEndController` receives: `GameState`, white name, black name, move count, `isOnline` flag.
"Rematch" calls `chess.startGame(...)` with same names/ruleset then `sceneManager.showGame()`.

### Leave-game confirmation

Inline overlay (no new FXML): `GameController.onLeaveGame()` calls `overlayManager.showConfirm(message)` which returns a `boolean`. Replaces the current `Alert` confirmation.

### Main menu visual upgrade

- Left panel: `LinearGradient` from `app-surface` (top) to `app-card` (bottom).
- King piece PNG (`king_white.png`) at `15% opacity` centered behind the title as a watermark `ImageView`.
- Title font `48px bold`.
- Nav buttons: hover shows `app-elevated` fill + `3px` left `app-primary` border instead of full color swap.
- Exit button: `.nav-button.btn-danger` — red fill on hover (destructive action, intentional distinction).

---

## Files Affected

| File | Change |
|---|---|
| `css/dark.css` | New color tokens, button classes, card shadow |
| `css/light.css` | Same token updates for light theme |
| `css/base.css` | Replace `primary-button`/`danger-button` with `btn-primary`/`btn-danger`; button base styles; overlay styles |
| `navigation/OverlayManager.java` | New class |
| `navigation/SceneManager.java` | Wrap root in `StackPane`; inject `OverlayManager`; replace `showDialog()` with `showOverlay()` |
| `controllers/GameController.java` | Bind board/label/piece sizes; inline leave-confirm; use `GameEndController` |
| `controllers/GameEndController.java` | New class |
| `controllers/MainMenuController.java` | King watermark wiring |
| `fxml/game.fxml` | Right sidebar width fix; `boardContainer` min size |
| `fxml/main-menu.fxml` | Gradient panel, watermark ImageView, button class renames |
| `fxml/game-end.fxml` | New file |
| `fxml/settings.fxml` | Button class renames |
| `fxml/offline-setup.fxml` | Button class renames |
| `fxml/online-setup.fxml` | Button class renames |
| `fxml/promotion.fxml` | Button class renames |
| `fxml/waiting.fxml` | Button class renames |

---

## Architecture Law Compliance

| Law | Status |
|---|---|
| 1. Module boundaries hard | COMPLIANT — no changes outside `application` |
| 2. Chess facade only API surface | COMPLIANT — `GameEndController` calls through `Chess` |
| 3. Observer pattern for state | COMPLIANT — game-end triggered by observer chain |
| 4. core is logic-only | NOT APPLICABLE |
| 5. Strategy pattern for rulesets | COMPLIANT — rematch passes same `RulesetOptions` |

---

## Out of Scope

- Animations between screens
- Board flip (playing black from bottom)
- Accessibility / keyboard navigation
- Any changes to `core` or `server`
