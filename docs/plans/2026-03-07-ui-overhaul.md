# UI Overhaul Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Overhaul the JavaFX application module: richer color system, unified button styles, in-window overlay dialogs, responsive board scaling, and a proper themed game-end screen.

**Architecture:** CSS-first approach — new color tokens and button classes in CSS, persistent root `StackPane` in `SceneManager` to host overlays, new `OverlayManager` class to replace all `Stage`-based dialogs with in-scene dimmed overlays using `Platform.enterNestedEventLoop`. Board squares/labels/pieces bind their sizes to the grid via `DoubleBinding`. No changes to `core` or `server`.

**Tech Stack:** JavaFX 21, FXML + Controller pattern, JavaFX CSS (looked-up colors, layered backgrounds), `javafx.application.Platform.enterNestedEventLoop` / `exitNestedEventLoop`, `javafx.beans.binding.Bindings`, JUnit 5, Mockito 5.

---

### Task 1: CSS overhaul — color system, button classes, overlay styles

**Files:**
- Modify: `modules/application/src/main/resources/css/base.css`
- Modify: `modules/application/src/main/resources/css/dark.css`
- Modify: `modules/application/src/main/resources/css/light.css`

No automated test for this task — verify visually after Task 9.

**Step 1: Replace `base.css` entirely**

```css
/* base.css — structure, layout, no color values */

.root {
    -fx-font-family: "Segoe UI", "Helvetica Neue", Arial, sans-serif;
    -fx-font-size: 14px;
}

/* ── Buttons ─────────────────────────────────────────────────────────────── */

.btn-primary {
    -fx-font-size: 14px;
    -fx-padding: 10 28 10 28;
    -fx-background-radius: 8;
    -fx-cursor: hand;
}

.btn-primary:hover {
    -fx-translate-y: -1;
}

.btn-ghost {
    -fx-font-size: 14px;
    -fx-padding: 10 28 10 28;
    -fx-background-radius: 8;
    -fx-cursor: hand;
    -fx-background-color: transparent;
}

.btn-ghost:hover {
    -fx-translate-y: -1;
}

.btn-danger {
    -fx-font-size: 14px;
    -fx-padding: 10 24 10 24;
    -fx-background-radius: 8;
    -fx-cursor: hand;
}

.btn-danger:hover {
    -fx-translate-y: -1;
}

.nav-button {
    -fx-font-size: 16px;
    -fx-padding: 14 40 14 40;
    -fx-background-radius: 8;
    -fx-cursor: hand;
    -fx-min-width: 220px;
    -fx-alignment: CENTER_LEFT;
}

.nav-button:hover {
    -fx-background-color: app-elevated;
    -fx-border-color: app-primary transparent transparent transparent;
    -fx-border-width: 0 0 0 3;
    -fx-translate-y: -1;
}

/* ── Cards ───────────────────────────────────────────────────────────────── */

.card {
    -fx-background-radius: 12;
    -fx-padding: 16;
}

.section-heading {
    -fx-font-size: 13px;
    -fx-font-weight: bold;
    -fx-padding: 0 0 8 0;
}

/* ── Overlay ─────────────────────────────────────────────────────────────── */

.overlay-dim {
    -fx-background-color: rgba(0, 0, 0, 0.55);
}

.overlay-card {
    -fx-background-radius: 12;
    -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.6), 24, 0, 0, 4);
}

/* ── Form controls ───────────────────────────────────────────────────────── */

.text-field {
    -fx-background-radius: 6;
    -fx-padding: 8 12 8 12;
    -fx-font-size: 14px;
}

.combo-box {
    -fx-background-radius: 6;
}

/* ── Player cards ────────────────────────────────────────────────────────── */

.player-name {
    -fx-font-size: 18px;
    -fx-font-weight: bold;
}

.player-active-indicator {
    -fx-font-size: 12px;
}

/* ── Move list ───────────────────────────────────────────────────────────── */

.move-list-view {
    -fx-background-insets: 0;
    -fx-padding: 4;
    -fx-background-radius: 6;
}

/* ── Board ───────────────────────────────────────────────────────────────── */

.chess-board {
    -fx-background-color: transparent;
}

.board-square {
    -fx-min-width: 0;
    -fx-min-height: 0;
    -fx-cursor: hand;
}

.board-label {
    -fx-font-size: 11px;
    -fx-alignment: center;
}

.legal-move-dot {
    -fx-opacity: 0.55;
}

/* ── Settings ────────────────────────────────────────────────────────────── */

.settings-section {
    -fx-spacing: 10;
    -fx-padding: 12 0 4 0;
}

.theme-swatch {
    -fx-min-width: 36px;
    -fx-min-height: 36px;
    -fx-background-radius: 6;
    -fx-cursor: hand;
    -fx-border-radius: 6;
    -fx-border-width: 2;
}

.theme-swatch:selected {
    -fx-border-width: 3;
}

/* ── Dialog titles ───────────────────────────────────────────────────────── */

.dialog-title {
    -fx-font-size: 20px;
    -fx-font-weight: bold;
    -fx-padding: 0 0 12 0;
}

/* ── Game-end overlay ────────────────────────────────────────────────────── */

.game-end-title {
    -fx-font-size: 28px;
    -fx-font-weight: bold;
}

.outcome-badge-win {
    -fx-background-color: #16a34a;
    -fx-text-fill: white;
    -fx-padding: 4 14 4 14;
    -fx-background-radius: 20;
    -fx-font-weight: bold;
    -fx-font-size: 13px;
}

.outcome-badge-draw {
    -fx-background-color: #d97706;
    -fx-text-fill: white;
    -fx-padding: 4 14 4 14;
    -fx-background-radius: 20;
    -fx-font-weight: bold;
    -fx-font-size: 13px;
}
```

**Step 2: Replace `dark.css` entirely**

```css
/* dark.css */

.root {
    app-bg:           #0f0f1a;
    app-surface:      #1a1b2e;
    app-card:         #22243a;
    app-elevated:     #2a2c45;
    app-border:       #2e3058;
    app-primary:      #7c3aed;
    app-primary-hover:#6d28d9;
    app-danger:       #e53e3e;
    app-danger-dark:  #c53030;
    app-text:         #e8e8f0;
    app-subtext:      #8585a0;
    app-dot:          #a0a0ff;

    -fx-background: app-bg;
    -fx-base: app-surface;
    -fx-control-inner-background: app-card;
    -fx-text-fill: app-text;
    -fx-accent: app-primary;
}

.btn-primary {
    -fx-background-color: app-primary;
    -fx-text-fill: white;
}
.btn-primary:hover { -fx-background-color: app-primary-hover; }

.btn-ghost {
    -fx-border-color: app-border;
    -fx-border-width: 1;
    -fx-border-radius: 8;
    -fx-text-fill: app-text;
}
.btn-ghost:hover {
    -fx-border-color: app-primary;
    -fx-text-fill: app-primary;
}

.btn-danger { -fx-background-color: app-danger; -fx-text-fill: white; }
.btn-danger:hover { -fx-background-color: app-danger-dark; }

.nav-button {
    -fx-background-color: app-surface;
    -fx-text-fill: app-text;
    -fx-border-color: app-border;
    -fx-border-radius: 8;
    -fx-border-width: 1;
}

.card {
    -fx-background-color: app-card;
    -fx-border-color: app-border;
    -fx-border-width: 1;
    -fx-border-radius: 12;
    -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.35), 12, 0, 0, 3);
}

.overlay-card { -fx-background-color: app-surface; }

.section-heading { -fx-text-fill: app-subtext; }
.player-name { -fx-text-fill: app-text; }
.player-active-indicator { -fx-text-fill: app-primary; }
.game-end-title { -fx-text-fill: app-text; }

.text-field {
    -fx-background-color: app-surface;
    -fx-text-fill: app-text;
    -fx-border-color: app-border;
    -fx-border-width: 1;
    -fx-border-radius: 6;
    -fx-prompt-text-fill: app-subtext;
}
.text-field:focused { -fx-border-color: app-primary; }

.move-list-view { -fx-background-color: app-surface; }
.move-list-view .list-cell {
    -fx-background-color: transparent;
    -fx-text-fill: app-text;
    -fx-padding: 4 8 4 8;
}
.move-list-view .list-cell:odd { -fx-background-color: app-card; }

.legal-move-dot { -fx-fill: app-dot; }
.board-label { -fx-text-fill: app-subtext; }
.dialog-title { -fx-text-fill: app-text; }

.theme-swatch { -fx-border-color: app-border; }
.theme-swatch:selected { -fx-border-color: app-primary; }
```

**Step 3: Replace `light.css` entirely**

```css
/* light.css */

.root {
    app-bg:           #f5f5f7;
    app-surface:      #ffffff;
    app-card:         #ebebf0;
    app-elevated:     #dcdce6;
    app-border:       #d0d0dc;
    app-primary:      #7c3aed;
    app-primary-hover:#6d28d9;
    app-danger:       #e53e3e;
    app-danger-dark:  #c53030;
    app-text:         #1a1a2e;
    app-subtext:      #606080;
    app-dot:          #7c3aed;

    -fx-background: app-bg;
    -fx-base: app-surface;
    -fx-control-inner-background: app-surface;
    -fx-text-fill: app-text;
    -fx-accent: app-primary;
}

.btn-primary { -fx-background-color: app-primary; -fx-text-fill: white; }
.btn-primary:hover { -fx-background-color: app-primary-hover; }

.btn-ghost {
    -fx-border-color: app-border;
    -fx-border-width: 1;
    -fx-border-radius: 8;
    -fx-text-fill: app-text;
}
.btn-ghost:hover {
    -fx-border-color: app-primary;
    -fx-text-fill: app-primary;
}

.btn-danger { -fx-background-color: app-danger; -fx-text-fill: white; }
.btn-danger:hover { -fx-background-color: app-danger-dark; }

.nav-button {
    -fx-background-color: app-surface;
    -fx-text-fill: app-text;
    -fx-border-color: app-border;
    -fx-border-radius: 8;
    -fx-border-width: 1;
}

.card {
    -fx-background-color: app-surface;
    -fx-border-color: app-border;
    -fx-border-width: 1;
    -fx-border-radius: 12;
    -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.08), 8, 0, 0, 2);
}

.overlay-card {
    -fx-background-color: app-surface;
    -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 24, 0, 0, 4);
}

.section-heading { -fx-text-fill: app-subtext; }
.player-name { -fx-text-fill: app-text; }
.player-active-indicator { -fx-text-fill: app-primary; }
.game-end-title { -fx-text-fill: app-text; }

.text-field {
    -fx-background-color: app-surface;
    -fx-text-fill: app-text;
    -fx-border-color: app-border;
    -fx-border-width: 1;
    -fx-border-radius: 6;
    -fx-prompt-text-fill: app-subtext;
}
.text-field:focused { -fx-border-color: app-primary; }

.move-list-view { -fx-background-color: app-surface; }
.move-list-view .list-cell {
    -fx-background-color: transparent;
    -fx-text-fill: app-text;
    -fx-padding: 4 8 4 8;
}
.move-list-view .list-cell:odd { -fx-background-color: app-card; }

.legal-move-dot { -fx-fill: app-dot; }
.board-label { -fx-text-fill: app-subtext; }
.dialog-title { -fx-text-fill: app-text; }

.theme-swatch { -fx-border-color: app-border; }
.theme-swatch:selected { -fx-border-color: app-primary; }
```

**Step 4: Run tests**

```bash
mvn test -pl modules/application -q
```

Expected: 30/30 passing.

**Step 5: Commit**

```bash
git add modules/application/src/main/resources/css/
git commit -m "style: overhaul CSS color tokens, button system, overlay styles"
```

---

### Task 2: FXML button class renames and overlay-card root styles

**Files:**
- Modify: `modules/application/src/main/resources/fxml/offline-setup.fxml`
- Modify: `modules/application/src/main/resources/fxml/online-setup.fxml`
- Modify: `modules/application/src/main/resources/fxml/waiting.fxml`
- Modify: `modules/application/src/main/resources/fxml/promotion.fxml`
- Modify: `modules/application/src/main/resources/fxml/settings.fxml`
- Modify: `modules/application/src/main/resources/fxml/main-menu.fxml`

Renames: `primary-button` → `btn-primary`, `danger-button` → `btn-danger`.
Also add `overlay-card` to each dialog FXML root (offline-setup, online-setup, waiting, promotion).

**Step 1: `offline-setup.fxml`** — Change root VBox, rename button class

Old root: `<VBox xmlns:fx="http://javafx.com/fxml/1" spacing="14" minWidth="380">`
New root: `<VBox xmlns:fx="http://javafx.com/fxml/1" styleClass="overlay-card" spacing="14" minWidth="380">`

Change `styleClass="primary-button"` → `styleClass="btn-primary"` on the Start button.
Change Cancel button: add `styleClass="btn-ghost"` (currently has no styleClass).

**Step 2: `online-setup.fxml`** — Same pattern

Add `styleClass="overlay-card"` to root VBox.
Change Connect button: `styleClass="primary-button"` → `styleClass="btn-primary"`.
Change Cancel button: add `styleClass="btn-ghost"`.

**Step 3: `waiting.fxml`** — Same pattern

Add `styleClass="overlay-card"` to root VBox.
Change Cancel button: `styleClass="danger-button"` → `styleClass="btn-danger"`.

**Step 4: `promotion.fxml`** — Add `overlay-card`

Add `styleClass="overlay-card"` to root VBox. No button class changes (buttons built programmatically in controller).

**Step 5: `settings.fxml`** — Rename button classes

Change Save button: `styleClass="primary-button"` → `styleClass="btn-primary"`.
Change Cancel button: add `styleClass="btn-ghost"`.

**Step 6: `main-menu.fxml`** — Rename exit button class

Change Exit button: remove `danger-button` from styleClass, leave only `nav-button`. Add a new CSS class in dark.css for the exit variant later (Task 9).

**Full updated FXML files after edits:**

`offline-setup.fxml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.*?>

<VBox xmlns:fx="http://javafx.com/fxml/1"
      styleClass="overlay-card" spacing="14" minWidth="380">
    <padding><Insets top="32" right="40" bottom="32" left="40"/></padding>

    <Label text="%dialog.offline.title" styleClass="dialog-title"/>

    <Label text="%dialog.offline.white"/>
    <TextField fx:id="whiteField" promptText="%dialog.offline.white"/>

    <Label text="%dialog.offline.black"/>
    <TextField fx:id="blackField" promptText="%dialog.offline.black"/>

    <Label text="%dialog.offline.ruleset"/>
    <ComboBox fx:id="rulesetBox" maxWidth="Infinity"/>

    <HBox spacing="12" alignment="CENTER_RIGHT" style="-fx-padding: 8 0 0 0;">
        <Button text="%dialog.offline.cancel" onAction="#onCancel" styleClass="btn-ghost"/>
        <Button text="%dialog.offline.start"  onAction="#onStart"  styleClass="btn-primary"/>
    </HBox>
</VBox>
```

`online-setup.fxml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.*?>

<VBox xmlns:fx="http://javafx.com/fxml/1"
      styleClass="overlay-card" spacing="12" minWidth="400">
    <fx:define>
        <ToggleGroup fx:id="modeGroup"/>
    </fx:define>
    <padding><Insets top="32" right="40" bottom="32" left="40"/></padding>

    <Label text="%dialog.online.title" styleClass="dialog-title"/>

    <HBox spacing="12">
        <ToggleButton fx:id="createToggle" text="%dialog.online.create"
                      toggleGroup="$modeGroup" selected="true"/>
        <ToggleButton fx:id="joinToggle"   text="%dialog.online.join"
                      toggleGroup="$modeGroup"/>
    </HBox>

    <Label text="%dialog.online.ip"/>
    <TextField fx:id="ipField" promptText="127.0.0.1"/>

    <Label text="%dialog.online.port"/>
    <TextField fx:id="portField" promptText="8080"/>

    <Label fx:id="joinCodeLabel" text="%dialog.online.joincode"/>
    <TextField fx:id="joinCodeField" promptText="%dialog.online.joincode"/>

    <Label text="%dialog.offline.ruleset"/>
    <ComboBox fx:id="rulesetBox" maxWidth="Infinity"/>

    <Label fx:id="errorLabel" style="-fx-text-fill: red;" visible="false" managed="false"/>

    <HBox spacing="12" alignment="CENTER_RIGHT" style="-fx-padding: 8 0 0 0;">
        <Button text="%dialog.online.cancel" onAction="#onCancel" styleClass="btn-ghost"/>
        <Button text="%dialog.online.start"  onAction="#onConnect" styleClass="btn-primary"/>
    </HBox>
</VBox>
```

`waiting.fxml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.*?>

<VBox xmlns:fx="http://javafx.com/fxml/1"
      styleClass="overlay-card" spacing="16" alignment="CENTER" minWidth="320">
    <padding><Insets top="36" right="48" bottom="36" left="48"/></padding>
    <Label text="%waiting.title" styleClass="dialog-title"/>
    <Label text="%waiting.code" styleClass="section-heading"/>
    <Label fx:id="codeLabel" style="-fx-font-size:28px; -fx-font-weight:bold;"/>
    <Label text="%waiting.instruction" wrapText="true" alignment="CENTER"/>
    <Button text="%waiting.cancel" onAction="#onCancel" styleClass="btn-danger"/>
</VBox>
```

`promotion.fxml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.*?>

<VBox xmlns:fx="http://javafx.com/fxml/1"
      styleClass="overlay-card" spacing="20" alignment="CENTER">
    <padding><Insets top="28" right="32" bottom="28" left="32"/></padding>
    <Label text="%promotion.title" styleClass="dialog-title"/>
    <HBox fx:id="pieceRow" spacing="16" alignment="CENTER"/>
</VBox>
```

`settings.fxml` — only button class changes (keep existing structure, just change styleClass attributes):
- Save button: `styleClass="primary-button"` → `styleClass="btn-primary"`
- Cancel button: add `styleClass="btn-ghost"`

**Step 7: Run tests**

```bash
mvn test -pl modules/application -q
```

Expected: 30/30 passing.

**Step 8: Commit**

```bash
git add modules/application/src/main/resources/fxml/
git commit -m "style(fxml): rename button classes, add overlay-card to dialog roots"
```

---

### Task 3: OverlayManager — in-window dialog host

**Files:**
- Create: `modules/application/src/main/java/io/github/conava/chess/application/navigation/OverlayManager.java`

No automated test — requires a live JavaFX stage. Tested end-to-end in Task 9.

**Step 1: Create `OverlayManager.java`**

```java
package io.github.conava.chess.application.navigation;

import io.github.conava.chess.application.i18n.I18n;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Hosts modal dialogs as in-window overlays on top of the primary scene's
 * root {@link StackPane}.
 *
 * <p>Dialogs are loaded from FXML, wrapped in a semi-transparent dim backdrop,
 * and pushed onto the root stack. The calling thread (FX Application Thread)
 * is "blocked" via {@link Platform#enterNestedEventLoop} — the UI remains
 * responsive during the wait because nested event loops still process events.
 *
 * <p>Dialog controllers dismiss themselves by calling {@link #dismiss()},
 * which exits the nested loop and returns control to the caller.
 */
public class OverlayManager {

    private final StackPane rootStack;
    private final I18n      i18n;
    private final Deque<Object> nestedLoopKeys = new ArrayDeque<>();

    public OverlayManager(StackPane rootStack, I18n i18n) {
        this.rootStack = rootStack;
        this.i18n      = i18n;
    }

    /**
     * Loads {@code fxmlPath}, sets {@code controller}, adds a dimmed overlay
     * to the root stack, then blocks via nested event loop until
     * {@link #dismiss()} is called. Returns the controller after dismissal.
     */
    public <C> C showOverlay(String fxmlPath, C controller) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath), i18n.getBundle());
            loader.setController(controller);
            Parent content = loader.load();

            Region dim = new Region();
            dim.getStyleClass().add("overlay-dim");
            dim.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            StackPane overlay = new StackPane(dim, content);
            overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            rootStack.getChildren().add(overlay);

            Object key = new Object();
            nestedLoopKeys.push(key);
            Platform.enterNestedEventLoop(key);

            rootStack.getChildren().remove(overlay);
            return controller;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load overlay: " + fxmlPath, e);
        }
    }

    /**
     * Shows an inline confirmation card (no FXML) and returns {@code true}
     * if the user clicked Yes, {@code false} for No.
     */
    public boolean showConfirm(String message) {
        boolean[] result = {false};

        Label msg = new Label(message);
        msg.setWrapText(true);
        msg.setStyle("-fx-font-size: 15px;");

        Button yes = new Button(i18n.get("dialog.confirm.yes"));
        yes.getStyleClass().add("btn-primary");
        yes.setOnAction(e -> { result[0] = true; dismiss(); });

        Button no = new Button(i18n.get("dialog.confirm.no"));
        no.getStyleClass().add("btn-ghost");
        no.setOnAction(e -> dismiss());

        HBox buttons = new HBox(12, no, yes);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(16, msg, buttons);
        card.getStyleClass().add("overlay-card");
        card.setPadding(new Insets(28));
        card.setMaxWidth(400);

        Region dim = new Region();
        dim.getStyleClass().add("overlay-dim");
        dim.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        StackPane overlay = new StackPane(dim, card);
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        rootStack.getChildren().add(overlay);

        Object key = new Object();
        nestedLoopKeys.push(key);
        Platform.enterNestedEventLoop(key);

        rootStack.getChildren().remove(overlay);
        return result[0];
    }

    /**
     * Dismisses the topmost overlay and unblocks the corresponding
     * {@link #showOverlay} or {@link #showConfirm} call.
     */
    public void dismiss() {
        if (!nestedLoopKeys.isEmpty()) {
            Platform.exitNestedEventLoop(nestedLoopKeys.pop(), null);
        }
    }
}
```

**Step 2: Run tests**

```bash
mvn test -pl modules/application -q
```

Expected: 30/30 passing (new class compiles, no existing tests affected).

**Step 3: Commit**

```bash
git add modules/application/src/main/java/io/github/conava/chess/application/navigation/OverlayManager.java
git commit -m "feat(nav): add OverlayManager for in-window dimmed overlay dialogs"
```

---

### Task 4: SceneManager — persistent root StackPane and overlay wiring

**Files:**
- Modify: `modules/application/src/main/java/io/github/conava/chess/application/navigation/SceneManager.java`

**Step 1: Update `SceneManager.java`**

Key changes:
1. Add `StackPane rootStack` and `OverlayManager overlayManager` fields (created once on first scene load).
2. `swapScene()` sets `rootStack.getChildren().set(0, root)` instead of `scene.setRoot(root)`.
3. Remove `showDialog()`, add `showOverlay()` and `dismissOverlay()` delegating to `overlayManager`.

Full replacement:

```java
package io.github.conava.chess.application.navigation;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.controllers.GameController;
import io.github.conava.chess.application.controllers.MainMenuController;
import io.github.conava.chess.application.controllers.SettingsController;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.application.theme.ThemeManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneManager {

    private static final String FXML_MAIN_MENU = "/fxml/main-menu.fxml";
    private static final String FXML_GAME      = "/fxml/game.fxml";
    private static final String FXML_SETTINGS  = "/fxml/settings.fxml";

    private final Stage          primaryStage;
    private final Chess          chess;
    private final ThemeManager   themeManager;
    private final I18n           i18n;
    private final SettingsService settingsService;

    private StackPane      rootStack;
    private OverlayManager overlayManager;

    public SceneManager(Stage primaryStage, Chess chess, ThemeManager themeManager,
                        I18n i18n, SettingsService settingsService) {
        this.primaryStage    = primaryStage;
        this.chess           = chess;
        this.themeManager    = themeManager;
        this.i18n            = i18n;
        this.settingsService = settingsService;
    }

    public void showMainMenu() {
        var controller = new MainMenuController(this, i18n);
        swapScene(FXML_MAIN_MENU, controller, 900, 650);
        primaryStage.setMaximized(false);
    }

    public void showGame() {
        var controller = new GameController(this, chess, themeManager, i18n);
        swapScene(FXML_GAME, controller, 1280, 860);
        primaryStage.setMaximized(true);
    }

    public void showSettings() {
        var controller = new SettingsController(this, themeManager, i18n, settingsService);
        swapScene(FXML_SETTINGS, controller, 900, 650);
    }

    /**
     * Loads {@code fxmlPath} as a dimmed in-window overlay, blocking until
     * the controller calls {@link #dismissOverlay()}. Returns the controller.
     */
    public <C> C showOverlay(String fxmlPath, C controller) {
        return overlayManager.showOverlay(fxmlPath, controller);
    }

    /**
     * Shows an inline confirmation overlay. Returns {@code true} if the user
     * clicked Yes.
     */
    public boolean showConfirm(String message) {
        return overlayManager.showConfirm(message);
    }

    /** Dismisses the topmost overlay. Called by dialog controllers. */
    public void dismissOverlay() {
        overlayManager.dismiss();
    }

    // ── Internal scene swap ───────────────────────────────────────────────────

    private void swapScene(String fxmlPath, Object controller, double w, double h) {
        Parent root = loadFxml(fxmlPath, controller);
        if (primaryStage.getScene() == null) {
            rootStack      = new StackPane(root);
            overlayManager = new OverlayManager(rootStack, i18n);
            Scene scene    = new Scene(rootStack, w, h);
            themeManager.registerScene(scene);
            primaryStage.setScene(scene);
        } else {
            rootStack.getChildren().set(0, root);
        }
        primaryStage.show();
    }

    private Parent loadFxml(String fxmlPath, Object controller) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath), i18n.getBundle());
            loader.setController(controller);
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML: " + fxmlPath, e);
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Stage          getPrimaryStage()      { return primaryStage; }
    public Chess          getChess()             { return chess; }
    public ThemeManager   getThemeManager()      { return themeManager; }
    public I18n           getI18n()              { return i18n; }
    public SettingsService getSettingsService()  { return settingsService; }
}
```

**Step 2: Run tests**

```bash
mvn test -pl modules/application -q
```

Expected: 30/30 passing.

**Step 3: Commit**

```bash
git add modules/application/src/main/java/io/github/conava/chess/application/navigation/SceneManager.java
git commit -m "feat(nav): persistent root StackPane, wire OverlayManager into SceneManager"
```

---

### Task 5: Dialog controllers — replace Stage.close() with dismissOverlay()

**Files:**
- Modify: `modules/application/src/main/java/io/github/conava/chess/application/controllers/OfflineSetupController.java`
- Modify: `modules/application/src/main/java/io/github/conava/chess/application/controllers/OnlineSetupController.java`
- Modify: `modules/application/src/main/java/io/github/conava/chess/application/controllers/WaitingController.java`
- Modify: `modules/application/src/main/java/io/github/conava/chess/application/controllers/PromotionController.java`
- Modify: `modules/application/src/main/java/io/github/conava/chess/application/controllers/MainMenuController.java`

Each dialog controller currently calls `((Stage) node.getScene().getWindow()).close()`. Replace with `closeAction.run()` where `closeAction` is a `Runnable` passed at construction time. `MainMenuController` passes `sceneManager::dismissOverlay` when creating each dialog controller.

**Step 1: Update `OfflineSetupController.java`**

```java
package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public class OfflineSetupController {

    private final I18n            i18n;
    private final SettingsService settingsService;
    private final Runnable        closeAction;

    @FXML private TextField whiteField;
    @FXML private TextField blackField;
    @FXML private ComboBox<RulesetOptions> rulesetBox;

    private boolean confirmed = false;

    public OfflineSetupController(I18n i18n, SettingsService settingsService,
                                  Runnable closeAction) {
        this.i18n            = i18n;
        this.settingsService = settingsService;
        this.closeAction     = closeAction;
    }

    @FXML
    public void initialize() {
        rulesetBox.setItems(FXCollections.observableArrayList(RulesetOptions.values()));
        rulesetBox.getSelectionModel().selectFirst();
        whiteField.setText(settingsService.loadPlayerWhite());
        blackField.setText(settingsService.loadPlayerBlack());
    }

    @FXML
    private void onStart() {
        if (whiteField.getText().isBlank()) whiteField.setText("Player White");
        if (blackField.getText().isBlank()) blackField.setText("Player Black");
        confirmed = true;
        closeAction.run();
    }

    @FXML
    private void onCancel() { closeAction.run(); }

    public boolean isConfirmed()       { return confirmed; }
    public String getPlayerWhite()     { return whiteField.getText().trim(); }
    public String getPlayerBlack()     { return blackField.getText().trim(); }
    public RulesetOptions getRuleset() { return rulesetBox.getValue(); }
}
```

**Step 2: Update `OnlineSetupController.java`**

Replace `private void close() { ((Stage) ipField.getScene().getWindow()).close(); }` with `closeAction.run()`. Add `Runnable closeAction` to constructor.

```java
package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class OnlineSetupController {

    private final I18n            i18n;
    private final SettingsService settingsService;
    private final Runnable        closeAction;

    @FXML private ToggleButton createToggle;
    @FXML private ToggleButton joinToggle;
    @FXML private ToggleGroup  modeGroup;
    @FXML private TextField    ipField;
    @FXML private TextField    portField;
    @FXML private TextField    joinCodeField;
    @FXML private Label        joinCodeLabel;
    @FXML private ComboBox<RulesetOptions> rulesetBox;
    @FXML private Label        errorLabel;

    private boolean confirmed = false;

    public OnlineSetupController(I18n i18n, SettingsService settingsService,
                                 Runnable closeAction) {
        this.i18n            = i18n;
        this.settingsService = settingsService;
        this.closeAction     = closeAction;
    }

    @FXML
    public void initialize() {
        rulesetBox.setItems(FXCollections.observableArrayList(RulesetOptions.values()));
        rulesetBox.getSelectionModel().selectFirst();
        updateJoinCodeVisibility();
        modeGroup.selectedToggleProperty().addListener((o, old, sel) -> updateJoinCodeVisibility());
    }

    private void updateJoinCodeVisibility() {
        boolean joining = joinToggle.isSelected();
        joinCodeField.setVisible(joining);
        joinCodeField.setManaged(joining);
        joinCodeLabel.setVisible(joining);
        joinCodeLabel.setManaged(joining);
    }

    @FXML
    private void onConnect() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        String ip   = ipField.getText().trim();
        String port = portField.getText().trim();
        if (!isValidIp(ip))     { showError("Invalid IP address."); return; }
        if (!isValidPort(port)) { showError("Port must be 1–65535."); return; }
        confirmed = true;
        closeAction.run();
    }

    @FXML
    private void onCancel() { closeAction.run(); }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private boolean isValidIp(String ip) {
        return ip.equals("localhost")
                || ip.matches("(\\d{1,3}\\.){3}\\d{1,3}")
                || ip.contains(":");
    }

    private boolean isValidPort(String port) {
        try { int p = Integer.parseInt(port); return p >= 1 && p <= 65535; }
        catch (NumberFormatException e) { return false; }
    }

    public boolean isConfirmed()       { return confirmed; }
    public String  getIp()             { return ipField.getText().trim(); }
    public String  getPort()           { return portField.getText().trim(); }
    public String  getJoinCode()       { return joinCodeField.isVisible() ? joinCodeField.getText().trim() : ""; }
    public RulesetOptions getRuleset() { return rulesetBox.getValue(); }
    public String  getPlayerWhite()    { return joinToggle.isSelected() ? "Opponent" : "You"; }
    public String  getPlayerBlack()    { return joinToggle.isSelected() ? "You" : "Opponent"; }
}
```

**Step 3: Update `WaitingController.java`**

```java
package io.github.conava.chess.application.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class WaitingController {

    @FXML private Label codeLabel;

    private final String   joinCode;
    private final Runnable closeAction;
    private boolean cancelled = false;

    public WaitingController(String joinCode, Runnable closeAction) {
        this.joinCode    = joinCode;
        this.closeAction = closeAction;
    }

    @FXML
    public void initialize() {
        codeLabel.setText(joinCode != null ? joinCode : "—");
    }

    @FXML
    private void onCancel() {
        cancelled = true;
        closeAction.run();
    }

    public boolean isCancelled() { return cancelled; }
}
```

**Step 4: Update `PromotionController.java`**

Replace `private void close(Node node) { ((Stage) node.getScene().getWindow()).close(); }` with `closeAction.run()`.

```java
package io.github.conava.chess.application.controllers;

import io.github.conava.chess.core.data.pieces.Pieces;
import io.github.conava.chess.core.data.player.PlayerColor;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class PromotionController {

    @FXML private HBox pieceRow;

    private final PlayerColor playerColor;
    private final Runnable    closeAction;
    private Pieces selectedPiece = Pieces.QUEEN;

    private static final Pieces[] PROMOTION_OPTIONS = {
        Pieces.QUEEN, Pieces.ROOK, Pieces.BISHOP, Pieces.KNIGHT
    };

    public PromotionController(PlayerColor playerColor, Runnable closeAction) {
        this.playerColor = playerColor;
        this.closeAction = closeAction;
    }

    @FXML
    public void initialize() {
        for (Pieces piece : PROMOTION_OPTIONS) {
            String iconPath = "/icon/" + piece.name().toLowerCase()
                    + "_" + playerColor.name().toLowerCase() + ".png";
            var url = getClass().getResource(iconPath);
            Button btn = new Button();
            if (url != null) {
                ImageView iv = new ImageView(new Image(url.toExternalForm()));
                iv.setFitWidth(64);
                iv.setFitHeight(64);
                iv.setPreserveRatio(true);
                btn.setGraphic(iv);
            } else {
                btn.setText(piece.name());
            }
            btn.getStyleClass().add("nav-button");
            Pieces p = piece;
            btn.setOnAction(e -> { selectedPiece = p; closeAction.run(); });
            pieceRow.getChildren().add(btn);
        }
    }

    public Pieces getSelectedPiece() { return selectedPiece; }
}
```

**Step 5: Update `MainMenuController.java`**

Pass `sceneManager::dismissOverlay` as `closeAction`. Use `showOverlay` instead of `showDialog`.

```java
package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.util.Map;

public class MainMenuController {

    private final SceneManager sceneManager;
    private final I18n         i18n;

    @FXML private ImageView titleImage;
    @FXML private ImageView watermarkImage;
    @FXML private Button    exitBtn;

    public MainMenuController(SceneManager sceneManager, I18n i18n) {
        this.sceneManager = sceneManager;
        this.i18n         = i18n;
    }

    @FXML
    public void initialize() {
        var imgUrl = getClass().getResource("/titleImage/chessTitleImage.jpg");
        if (imgUrl != null) {
            titleImage.setImage(new Image(imgUrl.toExternalForm()));
        }
        var kingUrl = getClass().getResource("/icon/king_white.png");
        if (kingUrl != null && watermarkImage != null) {
            watermarkImage.setImage(new Image(kingUrl.toExternalForm()));
        }
    }

    @FXML
    private void onLocalGame() {
        OfflineSetupController setup = sceneManager.showOverlay(
                "/fxml/offline-setup.fxml",
                new OfflineSetupController(i18n, sceneManager.getSettingsService(),
                        sceneManager::dismissOverlay));
        if (!setup.isConfirmed()) return;

        sceneManager.getChess().startGame(
                false, setup.getRuleset(),
                setup.getPlayerWhite(), setup.getPlayerBlack(), null);
        sceneManager.showGame();
    }

    @FXML
    private void onOnlineGame() {
        OnlineSetupController setup = sceneManager.showOverlay(
                "/fxml/online-setup.fxml",
                new OnlineSetupController(i18n, sceneManager.getSettingsService(),
                        sceneManager::dismissOverlay));
        if (!setup.isConfirmed()) return;

        Map<String, String> opts = Map.of(
                "ip",       setup.getIp(),
                "port",     setup.getPort(),
                "joinCode", setup.getJoinCode());

        sceneManager.getChess().startGame(
                true, setup.getRuleset(),
                setup.getPlayerWhite(), setup.getPlayerBlack(), opts);
        sceneManager.showGame();
    }

    @FXML
    private void onSettings() {
        sceneManager.showSettings();
    }

    @FXML
    private void onExit() {
        ((Stage) exitBtn.getScene().getWindow()).close();
    }
}
```

**Step 6: Run tests**

```bash
mvn test -pl modules/application -q
```

Expected: 30/30 passing.

**Step 7: Commit**

```bash
git add modules/application/src/main/java/io/github/conava/chess/application/controllers/
git commit -m "feat(controllers): replace Stage.close() with Runnable closeAction, wire showOverlay"
```

---

### Task 6: game-end.fxml and GameEndController

**Files:**
- Create: `modules/application/src/main/resources/fxml/game-end.fxml`
- Create: `modules/application/src/main/java/io/github/conava/chess/application/controllers/GameEndController.java`
- Modify: `modules/application/src/main/resources/i18n/messages_en.properties`
- Modify: `modules/application/src/main/resources/i18n/messages_de.properties`

**Step 1: Add new i18n keys to `messages_en.properties`**

Add after the existing `game.end.return=Return to menu?` line (replace that line too):

```properties
game.end.return=Return to Menu
game.end.wins=wins!
game.end.draw=Draw!
game.end.moves=moves played
game.end.rematch=Rematch
game.leave.confirm=Leave the current game?
```

**Step 2: Add the same keys to `messages_de.properties`**

```properties
game.end.return=Zum Hauptmenü
game.end.wins=gewinnt!
game.end.draw=Unentschieden!
game.end.moves=Züge gespielt
game.end.rematch=Revanche
game.leave.confirm=Das aktuelle Spiel verlassen?
```

**Step 3: Create `game-end.fxml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.*?>

<VBox xmlns:fx="http://javafx.com/fxml/1"
      styleClass="overlay-card" spacing="12" alignment="CENTER"
      minWidth="360" maxWidth="480">
    <padding><Insets top="36" right="44" bottom="36" left="44"/></padding>

    <Label fx:id="outcomeBadge"/>

    <Label fx:id="outcomeTitle" styleClass="game-end-title" wrapText="true"
           alignment="CENTER" maxWidth="400"/>

    <Label fx:id="outcomeSubtitle" styleClass="section-heading"/>

    <Separator style="-fx-padding: 4 0 4 0;"/>

    <HBox spacing="16" alignment="CENTER">
        <Label fx:id="whiteNameLabel" styleClass="player-name"/>
        <Label text="vs" styleClass="section-heading"/>
        <Label fx:id="blackNameLabel" styleClass="player-name"/>
    </HBox>

    <Label fx:id="moveCountLabel" styleClass="section-heading"/>

    <HBox spacing="12" alignment="CENTER_RIGHT" style="-fx-padding: 12 0 0 0;">
        <Button fx:id="rematchBtn" text="%game.end.rematch"
                onAction="#onRematch" styleClass="btn-ghost"/>
        <Button text="%game.end.return"
                onAction="#onReturn"  styleClass="btn-primary"/>
    </HBox>
</VBox>
```

**Step 4: Create `GameEndController.java`**

```java
package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.core.logic.game.GameState;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.MissingResourceException;

public class GameEndController {

    public enum Choice { NONE, RETURN, REMATCH }

    @FXML private Label  outcomeBadge;
    @FXML private Label  outcomeTitle;
    @FXML private Label  outcomeSubtitle;
    @FXML private Label  whiteNameLabel;
    @FXML private Label  blackNameLabel;
    @FXML private Label  moveCountLabel;
    @FXML private Button rematchBtn;

    private final GameState state;
    private final String    whiteName;
    private final String    blackName;
    private final int       moveCount;
    private final boolean   isOnline;
    private final Runnable  closeAction;
    private final I18n      i18n;
    private Choice choice = Choice.NONE;

    public GameEndController(GameState state, String whiteName, String blackName,
                             int moveCount, boolean isOnline,
                             Runnable closeAction, I18n i18n) {
        this.state       = state;
        this.whiteName   = whiteName;
        this.blackName   = blackName;
        this.moveCount   = moveCount;
        this.isOnline    = isOnline;
        this.closeAction = closeAction;
        this.i18n        = i18n;
    }

    @FXML
    public void initialize() {
        whiteNameLabel.setText(whiteName);
        blackNameLabel.setText(blackName);
        moveCountLabel.setText(moveCount + " " + i18n.get("game.end.moves"));

        String stateName = state.name();

        if (stateName.startsWith("WHITE_WON")) {
            outcomeBadge.setText(whiteName + " " + i18n.get("game.end.wins"));
            outcomeBadge.getStyleClass().add("outcome-badge-win");
            outcomeTitle.setText(whiteName + " " + i18n.get("game.end.wins"));
        } else if (stateName.startsWith("BLACK_WON")) {
            outcomeBadge.setText(blackName + " " + i18n.get("game.end.wins"));
            outcomeBadge.getStyleClass().add("outcome-badge-win");
            outcomeTitle.setText(blackName + " " + i18n.get("game.end.wins"));
        } else {
            outcomeBadge.setText(i18n.get("game.end.draw"));
            outcomeBadge.getStyleClass().add("outcome-badge-draw");
            outcomeTitle.setText(i18n.get("game.end.draw"));
        }

        try {
            outcomeSubtitle.setText(i18n.get("state." + stateName));
        } catch (MissingResourceException e) {
            outcomeSubtitle.setText(stateName);
        }

        rematchBtn.setVisible(!isOnline);
        rematchBtn.setManaged(!isOnline);
    }

    @FXML private void onReturn()  { choice = Choice.RETURN;  closeAction.run(); }
    @FXML private void onRematch() { choice = Choice.REMATCH; closeAction.run(); }

    public Choice getChoice() { return choice; }
}
```

**Step 5: Run tests**

```bash
mvn test -pl modules/application -q
```

Expected: 30/30 passing.

**Step 6: Commit**

```bash
git add modules/application/src/main/resources/fxml/game-end.fxml \
        modules/application/src/main/java/io/github/conava/chess/application/controllers/GameEndController.java \
        modules/application/src/main/resources/i18n/
git commit -m "feat(game): add GameEndController and game-end.fxml overlay, add i18n keys"
```

---

### Task 7: GameController — board scaling and overlay-based dialogs

**Files:**
- Modify: `modules/application/src/main/java/io/github/conava/chess/application/controllers/GameController.java`

Key changes:
1. Add `DoubleBinding squareSize` field computed from `boardContainer` size.
2. Bind square `prefWidth/Height`, label sizes, piece `ImageView` sizes, and dot radius to `squareSize`.
3. Replace `Alert` game-end dialog with `GameEndController` overlay via `sceneManager.showOverlay()`.
4. Replace `Alert` leave-game confirmation with `sceneManager.showConfirm()`.
5. Pass `sceneManager::dismissOverlay` as `closeAction` to `WaitingController` and `PromotionController`.

**Step 1: Replace `GameController.java`**

```java
package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.application.tasks.ExecuteMove;
import io.github.conava.chess.application.theme.ThemeManager;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.pieces.Piece;
import io.github.conava.chess.core.data.pieces.Pieces;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.logic.game.GameState;
import io.github.conava.chess.core.logic.observer.GameObserver;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class GameController implements GameObserver {

    private static final Logger LOGGER = Logger.getLogger(GameController.class.getName());

    private final SceneManager sceneManager;
    private final Chess        chess;
    private final ThemeManager themeManager;
    private final I18n         i18n;

    @FXML private Label    blackName;
    @FXML private Label    blackActive;
    @FXML private Label    whiteName;
    @FXML private Label    whiteActive;
    @FXML private ListView<String> moveList;
    @FXML private StackPane boardContainer;
    @FXML private VBox      rankLabels;
    @FXML private HBox      fileLabels;

    private final StackPane[][] boardSquares  = new StackPane[8][8];
    private final List<StackPane> markedSquares = new ArrayList<>();
    private Square selectedSquare = null;
    private List<Square> legalSquares = List.of();
    private Board localBoard;
    private DoubleBinding squareSize;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "chess-move-executor");
                t.setDaemon(true);
                return t;
            });

    public GameController(SceneManager sceneManager, Chess chess,
                          ThemeManager themeManager, I18n i18n) {
        this.sceneManager = sceneManager;
        this.chess        = chess;
        this.themeManager = themeManager;
        this.i18n         = i18n;
    }

    @FXML
    public void initialize() {
        buildBoard();
        buildLabels();
        registerWithGame();
        updateAll();
    }

    // ── Board construction ────────────────────────────────────────────────────

    private void buildBoard() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("chess-board");

        DoubleBinding boardSize = (DoubleBinding) Bindings.min(
                boardContainer.widthProperty(),
                boardContainer.heightProperty());
        squareSize = boardSize.divide(8.0);

        grid.prefWidthProperty().bind(boardSize);
        grid.prefHeightProperty().bind(boardSize);

        for (int row = 7; row >= 0; row--) {
            for (int col = 0; col < 8; col++) {
                StackPane square = new StackPane();
                square.getStyleClass().addAll("board-square",
                        (row + col) % 2 == 0 ? "light-square" : "dark-square");
                square.prefWidthProperty().bind(squareSize);
                square.prefHeightProperty().bind(squareSize);

                final int r = row, c = col;
                square.setOnMouseClicked(e -> handleSquareClick(r, c));

                boardSquares[row][col] = square;
                grid.add(square, col, 7 - row);
            }
        }

        boardContainer.getChildren().add(grid);
    }

    private void buildLabels() {
        for (int row = 8; row >= 1; row--) {
            Label lbl = new Label(String.valueOf(row));
            lbl.getStyleClass().add("board-label");
            lbl.prefHeightProperty().bind(squareSize);
            rankLabels.getChildren().add(lbl);
        }
        for (char c = 'a'; c <= 'h'; c++) {
            Label lbl = new Label(String.valueOf(c));
            lbl.getStyleClass().add("board-label");
            lbl.prefWidthProperty().bind(squareSize);
            fileLabels.getChildren().add(lbl);
        }
    }

    // ── Game wiring ───────────────────────────────────────────────────────────

    private void registerWithGame() {
        chess.addObserver(this);
        Player white = chess.getPlayerWhite();
        Player black = chess.getPlayerBlack();
        if (white != null) whiteName.setText(white.name());
        if (black != null) blackName.setText(black.name());
        localBoard = chess.getBoard();
    }

    @Override
    public void onGameStateChanged() {
        Platform.runLater(this::update);
    }

    private void update() {
        GameState state = chess.getState();
        if (state == null || state == GameState.NO_GAME) return;

        switch (state) {
            case RUNNING           -> updateAll();
            case WAITING_FOR_PLAYER -> showWaitingOverlay();
            case SERVER_ERROR      -> showErrorAndReturnToMenu(
                    i18n.get("error.server.title"), i18n.get("error.server"));
            default                -> showGameEndOverlay(state);
        }
    }

    // ── Waiting overlay ───────────────────────────────────────────────────────

    private void showWaitingOverlay() {
        String code = chess.getJoinCode();
        WaitingController ctrl = new WaitingController(code, sceneManager::dismissOverlay);
        sceneManager.showOverlay("/fxml/waiting.fxml", ctrl);
        if (ctrl.isCancelled()) {
            chess.endGame();
            sceneManager.showMainMenu();
        }
    }

    // ── UI update ─────────────────────────────────────────────────────────────

    private void updateAll() {
        updateBoard();
        updateMoveList();
        updateActivePlayerIndicator();
    }

    private void updateBoard() {
        Board board = chess.getBoard();
        if (board == null) return;
        if (board.equals(localBoard)) return;
        localBoard = board;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                setPieceOnSquare(row, col, board.getPieceAt(new Square(row, col)));
            }
        }
    }

    private void setPieceOnSquare(int row, int col, Piece piece) {
        StackPane square = boardSquares[row][col];
        square.getChildren().removeIf(n -> "piece".equals(n.getUserData()));
        if (piece != null) {
            String path = "/icon/" + piece.getType().name().toLowerCase()
                    + "_" + piece.getPlayer().color().name().toLowerCase() + ".png";
            var url = getClass().getResource(path);
            if (url != null) {
                ImageView iv = new ImageView(new Image(url.toExternalForm()));
                iv.fitWidthProperty().bind(squareSize.multiply(0.78));
                iv.fitHeightProperty().bind(squareSize.multiply(0.78));
                iv.setPreserveRatio(true);
                iv.setUserData("piece");
                square.getChildren().add(iv);
            }
        }
    }

    private void updateMoveList() {
        moveList.setItems(FXCollections.observableArrayList(chess.getMoveList()));
        if (!moveList.getItems().isEmpty()) {
            moveList.scrollTo(moveList.getItems().size() - 1);
        }
    }

    private void updateActivePlayerIndicator() {
        Player current = chess.getCurrentPlayer();
        String activeText  = i18n.get("game.active");
        String waitingText = i18n.get("game.waiting");
        boolean isWhiteActive = current == chess.getPlayerWhite();
        whiteActive.setText(isWhiteActive ? activeText : waitingText);
        blackActive.setText(isWhiteActive ? waitingText : activeText);
    }

    // ── Board interaction ─────────────────────────────────────────────────────

    private void handleSquareClick(int row, int col) {
        Square clicked = new Square(row, col);
        Piece  piece   = chess.getPieceAt(clicked);

        if (piece != null && piece.getPlayer() == chess.getCurrentPlayer()) {
            clearLegalMoveMarkers();
            selectedSquare = clicked;
            legalSquares   = chess.getLegalSquares(clicked);
            showLegalMoveMarkers(legalSquares);
            return;
        }

        if (selectedSquare != null && legalSquares.contains(clicked)) {
            clearLegalMoveMarkers();
            Piece movingPiece = chess.getPieceAt(selectedSquare);
            if (movingPiece != null && movingPiece.getType() == Pieces.PAWN
                    && (clicked.getY() == 0 || clicked.getY() == 7)) {
                PromotionController promoCtrl =
                        new PromotionController(movingPiece.getPlayer().color(),
                                sceneManager::dismissOverlay);
                sceneManager.showOverlay("/fxml/promotion.fxml", promoCtrl);
                submitMove(selectedSquare, clicked, promoCtrl.getSelectedPiece());
            } else {
                submitMove(selectedSquare, clicked, null);
            }
            selectedSquare = null;
            legalSquares   = List.of();
        } else {
            clearLegalMoveMarkers();
            selectedSquare = null;
            legalSquares   = List.of();
        }
    }

    private void submitMove(Square from, Square to, Pieces promotion) {
        executor.submit(new ExecuteMove(chess, from, to, promotion));
    }

    private void showLegalMoveMarkers(List<Square> squares) {
        for (Square sq : squares) {
            StackPane pane = boardSquares[sq.getY()][sq.getX()];
            Circle dot = new Circle();
            dot.radiusProperty().bind(squareSize.multiply(0.19));
            dot.getStyleClass().add("legal-move-dot");
            dot.setUserData("dot");
            dot.setMouseTransparent(true);
            pane.getChildren().add(dot);
            markedSquares.add(pane);
        }
    }

    private void clearLegalMoveMarkers() {
        for (StackPane pane : markedSquares) {
            pane.getChildren().removeIf(n -> "dot".equals(n.getUserData()));
        }
        markedSquares.clear();
    }

    // ── Game-end overlay ──────────────────────────────────────────────────────

    private void showGameEndOverlay(GameState state) {
        Player white = chess.getPlayerWhite();
        Player black = chess.getPlayerBlack();
        String wName = white != null ? white.name() : "White";
        String bName = black != null ? black.name() : "Black";
        int moves = chess.getMoveList().size();
        boolean online = chess.getJoinCode() != null;

        GameEndController ctrl = new GameEndController(
                state, wName, bName, moves, online,
                sceneManager::dismissOverlay, i18n);

        sceneManager.showOverlay("/fxml/game-end.fxml", ctrl);

        if (ctrl.getChoice() == GameEndController.Choice.REMATCH) {
            // Rematch: keep same players, restart
            chess.endGame();
            chess.startGame(false, null, wName, bName, null);
        } else {
            chess.endGame();
            executor.shutdown();
            sceneManager.showMainMenu();
        }
    }

    private void showErrorAndReturnToMenu(String title, String message) {
        sceneManager.showConfirm(title + "\n" + message);
        chess.endGame();
        sceneManager.showMainMenu();
    }

    @FXML
    private void onLeaveGame() {
        boolean confirmed = sceneManager.showConfirm(i18n.get("game.leave.confirm"));
        if (confirmed) {
            chess.endGame();
            executor.shutdown();
            sceneManager.showMainMenu();
        }
    }
}
```

**Important note on Rematch:** The `startGame()` call passes `null` for `RulesetOptions` in the rematch case. This is a simplification — the game was already set up with a valid ruleset. If `Chess.startGame()` requires a non-null `RulesetOptions`, store the last-used ruleset in a field in `GameController` set when `initialize()` runs. Inspect `Chess.startGame()` and adjust if needed.

**Step 2: Run tests**

```bash
mvn test -pl modules/application -q
```

Expected: 30/30 passing.

**Step 3: Commit**

```bash
git add modules/application/src/main/java/io/github/conava/chess/application/controllers/GameController.java
git commit -m "feat(game): bind board to window size, replace Alert dialogs with overlays"
```

---

### Task 8: game.fxml — sidebar width and board minimum size

**Files:**
- Modify: `modules/application/src/main/resources/fxml/game.fxml`

**Step 1: Update `game.fxml`**

Change the right sidebar `VBox`: `minWidth="180" maxWidth="280"` → `minWidth="220" prefWidth="250" maxWidth="300"`.
Add `minWidth="480" minHeight="480"` to `boardContainer` StackPane.

Full replacement:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.geometry.*?>

<BorderPane xmlns:fx="http://javafx.com/fxml/1"
            styleClass="root">

    <!-- Left: black player card -->
    <left>
        <VBox styleClass="card" alignment="TOP_CENTER" spacing="8"
              minWidth="180" maxWidth="240" BorderPane.alignment="CENTER">
            <padding><Insets top="20" right="16" bottom="20" left="16"/></padding>
            <Label fx:id="blackName"   styleClass="player-name" text="Black"/>
            <Label fx:id="blackActive" styleClass="player-active-indicator"/>
        </VBox>
    </left>

    <!-- Center: board with rank/file labels -->
    <center>
        <VBox alignment="CENTER" BorderPane.alignment="CENTER">
            <HBox fx:id="boardRoot" alignment="CENTER" VBox.vgrow="ALWAYS">
                <VBox fx:id="rankLabels" alignment="CENTER" minWidth="20"/>
                <StackPane fx:id="boardContainer" VBox.vgrow="ALWAYS" HBox.hgrow="ALWAYS"
                           minWidth="480" minHeight="480"/>
            </HBox>
            <HBox fx:id="fileLabels" alignment="CENTER"/>
        </VBox>
    </center>

    <!-- Right: white player card + move list + leave button -->
    <right>
        <VBox spacing="12" minWidth="220" prefWidth="250" maxWidth="300"
              BorderPane.alignment="CENTER">
            <padding><Insets top="20" right="16" bottom="20" left="16"/></padding>

            <VBox styleClass="card" alignment="TOP_CENTER" spacing="8">
                <padding><Insets top="16" right="12" bottom="16" left="12"/></padding>
                <Label fx:id="whiteName"   styleClass="player-name" text="White"/>
                <Label fx:id="whiteActive" styleClass="player-active-indicator"/>
            </VBox>

            <Label text="%game.moves" styleClass="section-heading"/>
            <ListView fx:id="moveList" styleClass="move-list-view" VBox.vgrow="ALWAYS"/>

            <Button text="%game.leave" onAction="#onLeaveGame"
                    styleClass="btn-danger" maxWidth="Infinity"/>
        </VBox>
    </right>
</BorderPane>
```

**Step 2: Run tests**

```bash
mvn test -pl modules/application -q
```

Expected: 30/30 passing.

**Step 3: Commit**

```bash
git add modules/application/src/main/resources/fxml/game.fxml
git commit -m "fix(fxml): widen right sidebar to prevent button truncation, add board min size"
```

---

### Task 9: Main menu visual upgrade

**Files:**
- Modify: `modules/application/src/main/resources/fxml/main-menu.fxml`
- Modify: `modules/application/src/main/resources/css/dark.css` (add `.menu-brand-panel`, exit button hover)
- Modify: `modules/application/src/main/resources/css/light.css` (same)

**Step 1: Update `main-menu.fxml`**

Change left panel from `VBox` (`.card`) to `StackPane` (`.menu-brand-panel`) with the `watermarkImage` behind the title content. Increase title font size. Add `nav-button-exit` class to Exit button.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.scene.image.*?>
<?import javafx.geometry.*?>

<HBox xmlns:fx="http://javafx.com/fxml/1">

    <!-- Left: branding panel with gradient and king watermark -->
    <StackPane styleClass="menu-brand-panel" HBox.hgrow="ALWAYS" minWidth="300">
        <ImageView fx:id="watermarkImage" opacity="0.12" fitWidth="220"
                   preserveRatio="true" mouseTransparent="true"/>
        <VBox alignment="CENTER">
            <padding><Insets top="60" right="40" bottom="60" left="40"/></padding>
            <ImageView fx:id="titleImage" fitWidth="260" preserveRatio="true"
                       pickOnBounds="true"/>
            <Label fx:id="appTitle" text="%menu.title"
                   style="-fx-font-size:48px; -fx-font-weight:bold;" wrapText="true"/>
        </VBox>
    </StackPane>

    <!-- Right: navigation buttons -->
    <VBox alignment="CENTER" spacing="16" HBox.hgrow="ALWAYS">
        <padding><Insets top="80" right="60" bottom="80" left="60"/></padding>
        <Button fx:id="localBtn"    text="%menu.local"    onAction="#onLocalGame"
                styleClass="nav-button" maxWidth="Infinity"/>
        <Button fx:id="onlineBtn"   text="%menu.online"   onAction="#onOnlineGame"
                styleClass="nav-button" maxWidth="Infinity"/>
        <Button fx:id="settingsBtn" text="%menu.settings" onAction="#onSettings"
                styleClass="nav-button" maxWidth="Infinity"/>
        <Button fx:id="exitBtn"     text="%menu.exit"     onAction="#onExit"
                styleClass="nav-button nav-button-exit" maxWidth="Infinity"/>
    </VBox>
</HBox>
```

**Step 2: Add `.menu-brand-panel` and `.nav-button-exit` to `dark.css`**

Append to `dark.css`:

```css
.menu-brand-panel {
    -fx-background-color: linear-gradient(to bottom, #1a1b2e, #22243a);
    -fx-background-radius: 0;
}

.nav-button-exit:hover {
    -fx-background-color: app-danger;
    -fx-border-color: app-danger;
    -fx-text-fill: white;
}
```

**Step 3: Append to `light.css`**

```css
.menu-brand-panel {
    -fx-background-color: linear-gradient(to bottom, #ffffff, #ebebf0);
    -fx-background-radius: 0;
}

.nav-button-exit:hover {
    -fx-background-color: app-danger;
    -fx-border-color: app-danger;
    -fx-text-fill: white;
}
```

**Step 4: Run full build**

```bash
mvn clean install -q
```

Expected: `BUILD SUCCESS`, 187 tests passing.

**Step 5: Manual smoke test**

```bash
cd modules/application && mvn org.openjfx:javafx-maven-plugin:0.0.8:run
```

Verify:
- Main menu: gradient left panel, faint king watermark, purple border accent on nav button hover
- Exit button hover: red fill
- Settings: opens as in-window overlay (no OS title bar)
- Local Game setup: opens as in-window overlay
- Game screen: board scales when resizing the window
- Right sidebar "Leave Game" button fully visible
- Leave Game: shows in-window confirm overlay (no OS dialog)
- Checkmate/draw: shows styled game-end overlay card with player names and move count

**Step 6: Commit**

```bash
git add modules/application/src/main/resources/fxml/main-menu.fxml \
        modules/application/src/main/resources/css/dark.css \
        modules/application/src/main/resources/css/light.css
git commit -m "feat(ui): gradient brand panel, king watermark, exit button danger hover"
```

---

## Testing Requirements

Most changes are visual. Automated tests that can run headlessly:

| Test class | What to verify |
|---|---|
| `ChessTest` | Existing 10 tests still pass (no regressions from constructor/API changes) |
| `SettingsServiceTest` | Unchanged — still passes |
| `I18nTest` | New keys (`game.end.return`, `game.end.wins`, etc.) are present in both locales |
| `ThemeManagerTest` | Unchanged — still passes |
| `ExecuteMoveTest` | Unchanged — still passes |

For `I18nTest`, add assertions that the new keys resolve:
```java
// Add to existing I18nTest:
@Test
void newGameEndKeysExistInBothLocales() {
    I18n en = new I18n(I18n.Language.EN);
    I18n de = new I18n(I18n.Language.DE);
    assertNotNull(en.get("game.end.wins"));
    assertNotNull(en.get("game.end.draw"));
    assertNotNull(en.get("game.end.moves"));
    assertNotNull(en.get("game.end.rematch"));
    assertNotNull(en.get("game.leave.confirm"));
    assertNotNull(de.get("game.end.wins"));
    assertNotNull(de.get("game.leave.confirm"));
}
```

## Documentation Updates

Update `modules/application/CLAUDE.md`:
- Add `OverlayManager` to the Key Classes section
- Add `GameEndController` to the Key Classes section
- Update `SceneManager` description to mention persistent root `StackPane`
- Update `OfflineSetupController`, `OnlineSetupController`, `WaitingController`, `PromotionController` — note `Runnable closeAction` constructor parameter
- Update Known Debt / Gotchas — remove item 5 ("Settings window is a stub") and note the completed game-end overlay

## Ordered Implementation Tasks

| # | Task | Risk | Parallelism |
|---|---|---|---|
| 1 | CSS overhaul | LOW | Independent |
| 2 | FXML button renames + overlay-card | LOW | Independent of 1 |
| 3 | OverlayManager (new class) | LOW | Independent |
| 4 | SceneManager (persistent root, wire overlay) | MEDIUM | After 3 |
| 5 | Dialog controllers (Runnable closeAction) | MEDIUM | After 4 |
| 6 | game-end.fxml + GameEndController | LOW | After 3 |
| 7 | GameController (scaling + overlays) | HIGH | After 4, 5, 6 |
| 8 | game.fxml layout fixes | LOW | Independent |
| 9 | Main menu visual upgrade | LOW | After 1 |

## Affected Files

| File | Change type |
|---|---|
| `css/base.css` | Rewrite |
| `css/dark.css` | Rewrite + append |
| `css/light.css` | Rewrite + append |
| `fxml/offline-setup.fxml` | Modify |
| `fxml/online-setup.fxml` | Modify |
| `fxml/waiting.fxml` | Modify |
| `fxml/promotion.fxml` | Modify |
| `fxml/settings.fxml` | Modify |
| `fxml/game.fxml` | Modify |
| `fxml/main-menu.fxml` | Modify |
| `fxml/game-end.fxml` | Create |
| `navigation/OverlayManager.java` | Create |
| `navigation/SceneManager.java` | Rewrite |
| `controllers/GameController.java` | Rewrite |
| `controllers/GameEndController.java` | Create |
| `controllers/MainMenuController.java` | Modify |
| `controllers/OfflineSetupController.java` | Modify |
| `controllers/OnlineSetupController.java` | Modify |
| `controllers/WaitingController.java` | Modify |
| `controllers/PromotionController.java` | Modify |
| `i18n/messages_en.properties` | Modify |
| `i18n/messages_de.properties` | Modify |
