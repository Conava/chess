# Promotion Picker Redesign -- Implementation Plan

**Date:** 2026-03-10
**Design doc:** `docs/plans/2026-03-10-promotion-picker-redesign.md`
**Branch:** `refactor/ruleset-complete-rewrite`
**Complexity:** Small (5 tasks, all within `modules/application`, no architectural changes)

---

## Task List

### Parallel Group 1

---

#### task-1
**Name:** Add `.promotion-piece-btn` structural rules to `base.css`
**Depends on:** (none)
**Touches:**
- `modules/application/src/main/resources/css/base.css`

**Test approach:** smoke-test
**Domain skills:** javafx, css

**Description:**

Open `modules/application/src/main/resources/css/base.css`. After the existing `.nav-button` block (ends around line 55), add a new section comment and the `.promotion-piece-btn` class with these exact rules:

```css
/* -- Promotion picker --------------------------------------------------- */

.promotion-piece-btn {
    -fx-background-radius: 10;
    -fx-border-radius:     10;
    -fx-border-width:      1.5;
    -fx-padding:           0;
    -fx-cursor:            hand;
    -fx-min-width:         0;
    -fx-min-height:        0;
    -fx-alignment:         CENTER;
}

.promotion-piece-btn:hover {
    -fx-translate-y: -2;
}

.promotion-piece-btn:pressed {
    -fx-scale-x:   0.94;
    -fx-scale-y:   0.94;
    -fx-translate-y: 0;
}
```

Key points:
- `-fx-padding: 0` is intentional -- button dimensions are fully controlled by `prefWidth`/`prefHeight` bindings in Java code. Padding would break the sizing math.
- `-fx-min-width: 0` and `-fx-min-height: 0` override the default Button minimum size so the binding controls the actual size.
- No colour values in `base.css` -- colours go in theme files (task-2).

Do NOT modify any existing rules. This is purely additive.

---

#### task-2
**Name:** Add `.promotion-piece-btn` colour rules to all 4 theme CSS files
**Depends on:** (none)
**Touches:**
- `modules/application/src/main/resources/css/themes/dark-charcoal.css`
- `modules/application/src/main/resources/css/themes/dark-purple.css`
- `modules/application/src/main/resources/css/themes/light-arctic.css`
- `modules/application/src/main/resources/css/themes/light-paper.css`

**Test approach:** smoke-test
**Domain skills:** javafx, css

**Description:**

Append the following block to each of the four theme CSS files, placed after the last existing rule in each file. The block is identical across all four files because it uses the shared CSS lookup colour tokens (`app-card`, `app-border`, `app-elevated`, `app-primary`) which each theme defines differently in its `.root` block:

```css
/* -- Promotion picker --------------------------------------------------- */

.promotion-piece-btn {
    -fx-background-color: app-card;
    -fx-border-color:     app-border;
}

.promotion-piece-btn:hover {
    -fx-background-color: app-elevated;
    -fx-border-color:     app-primary;
    -fx-effect: dropshadow(gaussian, derive(app-primary, 0%), 12, 0, 0, 3);
}
```

This mirrors the pattern used by `.btn-ghost` in each theme (transparent background + `app-border` border, hover swaps to primary border). The difference is that `.promotion-piece-btn` uses `app-card` as the resting background (not transparent) so the buttons have a visible card-like surface, and the hover adds a primary-tinted drop shadow for depth.

Files to edit:
1. `modules/application/src/main/resources/css/themes/dark-charcoal.css` -- append after line 228 (end of file)
2. `modules/application/src/main/resources/css/themes/dark-purple.css` -- append after last rule
3. `modules/application/src/main/resources/css/themes/light-arctic.css` -- append after last rule
4. `modules/application/src/main/resources/css/themes/light-paper.css` -- append after last rule

Do NOT modify any existing rules. This is purely additive.

---

#### task-3
**Name:** Simplify `promotion.fxml` -- remove title, tighten spacing, drop minWidth
**Depends on:** (none)
**Touches:**
- `modules/application/src/main/resources/fxml/promotion.fxml`

**Test approach:** smoke-test
**Domain skills:** javafx

**Description:**

Replace the entire contents of `modules/application/src/main/resources/fxml/promotion.fxml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.layout.*?>
<?import javafx.geometry.*?>

<VBox xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="io.github.conava.chess.application.controllers.PromotionController"
      styleClass="overlay-card" spacing="8" alignment="CENTER">
    <padding><Insets top="28" right="32" bottom="28" left="32"/></padding>
    <HBox fx:id="pieceRow" spacing="12" alignment="CENTER"/>
</VBox>
```

Changes from current file:
1. Remove the `<Label text="%promotion.title" styleClass="dialog-title"/>` element entirely. The piece images are self-explanatory.
2. Remove `minWidth="300"` from the `<VBox>` attributes. The card sizes itself from content.
3. Change `VBox` `spacing` from `"20"` to `"8"`.
4. Change `HBox` `spacing` from `"16"` to `"12"`.
5. Remove the `<?import javafx.scene.control.*?>` import -- no longer needed since `Label` is removed.
6. Keep `padding` as-is (`28 32 28 32`).

---

### Parallel Group 2

---

#### task-4
**Name:** Update `PromotionController` -- `cellSize` binding, new style class, entry animation
**Depends on:** task-1, task-2, task-3
**Touches:**
- `modules/application/src/main/java/io/github/conava/chess/application/controllers/PromotionController.java`

**Test approach:** smoke-test
**Domain skills:** javafx, java

**Description:**

Edit `modules/application/src/main/java/io/github/conava/chess/application/controllers/PromotionController.java`. The current file is 52 lines. Apply these changes:

**1. Add imports** (add to existing import block):
```java
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.beans.binding.NumberBinding;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.util.Duration;
```

**2. Add `cellSize` field and update constructor.**

Change the constructor from:
```java
public PromotionController(PlayerColor playerColor, Runnable closeAction) {
    this.playerColor = playerColor;
    this.closeAction = closeAction;
}
```
to:
```java
private final NumberBinding cellSize;

public PromotionController(PlayerColor playerColor, Runnable closeAction,
                           NumberBinding cellSize) {
    this.playerColor = playerColor;
    this.closeAction = closeAction;
    this.cellSize    = cellSize;
}
```

Add the `cellSize` field declaration next to the existing `playerColor` and `closeAction` fields.

**3. Update `initialize()` method.**

Inside the `for` loop, make these changes:

a) Replace fixed-size `ImageView` setup:
```java
// OLD:
iv.setFitWidth(64);
iv.setFitHeight(64);
```
with binding-based sizing:
```java
// NEW:
iv.fitWidthProperty().bind(cellSize.multiply(0.78));
iv.fitHeightProperty().bind(cellSize.multiply(0.78));
```

b) Change the style class from `"nav-button"` to `"promotion-piece-btn"`:
```java
// OLD:
btn.getStyleClass().add("nav-button");
// NEW:
btn.getStyleClass().add("promotion-piece-btn");
```

c) Add button size bindings and max-size constraint immediately after creating the button (before setting the graphic):
```java
btn.prefWidthProperty().bind(cellSize);
btn.prefHeightProperty().bind(cellSize);
btn.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
```

**4. Add entry animation** at the end of `initialize()`, after the `for` loop:

```java
Node root = pieceRow.getParent();
root.setOpacity(0);
root.setScaleX(0.88);
root.setScaleY(0.88);

FadeTransition  fade  = new FadeTransition(Duration.millis(140), root);
fade.setToValue(1.0);
ScaleTransition scale = new ScaleTransition(Duration.millis(140), root);
scale.setToX(1.0);
scale.setToY(1.0);
new ParallelTransition(fade, scale).play();
```

This runs a 140ms parallel fade-in (0 to 1 opacity) and scale-up (0.88 to 1.0) on the `VBox` parent, matching the design doc spec. `pieceRow.getParent()` is safe because the FXML loader assigns the parent before calling `initialize()`.

---

#### task-5
**Name:** Pass `cellSize` binding from `GameController` to `PromotionController`
**Depends on:** task-4
**Touches:**
- `modules/application/src/main/java/io/github/conava/chess/application/controllers/GameController.java`

**Test approach:** smoke-test
**Domain skills:** javafx, java

**Description:**

Edit `modules/application/src/main/java/io/github/conava/chess/application/controllers/GameController.java`.

Locate the `PromotionController` instantiation (around line 297-299). The current code is:

```java
PromotionController promoCtrl =
        new PromotionController(movingPiece.getPlayer().color(),
                sceneManager::dismissOverlay);
```

Change it to pass `squareSize.multiply(0.9)` as the third argument:

```java
PromotionController promoCtrl =
        new PromotionController(movingPiece.getPlayer().color(),
                sceneManager::dismissOverlay,
                squareSize.multiply(0.9));
```

`squareSize` is an existing `NumberBinding` field on `GameController` (line 64), defined as `Bindings.min(boardContainer.widthProperty(), boardContainer.heightProperty()).divide(8.0)`. The factor `0.9` produces a cell 90% the size of one board square -- slightly smaller to leave breathing room inside the overlay card.

This is a one-line change. No other modifications to `GameController` are needed. The `showOverlay` call and `getSelectedPiece()` read on the following lines remain unchanged.

---

## Dependency Graph

```
task-1 (base.css)  ──┐
task-2 (theme CSS) ──┼──> task-4 (PromotionController.java) ──> task-5 (GameController.java)
task-3 (promotion.fxml) ┘
```

## Parallel Execution Guide

| Group | Tasks | Can run simultaneously |
|-------|-------|-----------------------|
| 1     | task-1, task-2, task-3 | Yes -- no file overlap, no dependencies |
| 2     | task-4 | Alone -- depends on group 1, touches PromotionController.java |
| 3     | task-5 | Alone -- depends on task-4, touches GameController.java |

Note: task-4 and task-5 could technically be in the same parallel group since they touch different files. However, task-5 depends on task-4 (the constructor signature change), so they must be sequential.

## Complexity Estimate

**Small.** Five focused tasks across 8 files, all additive CSS plus minor Java edits. No architectural changes, no new classes, no module boundary changes, no build config updates. The largest task (task-4) is roughly 20 lines of new/changed code.

## Concerns

1. **No automated visual testing.** All tasks use `smoke-test` (confirm `mvn test` passes and the app compiles). Visual correctness must be verified manually by running the app, starting a game, and promoting a pawn. Each theme should be checked.

2. **`promotion.title` i18n key becomes orphaned.** The design doc notes this is harmless -- the key stays in `messages_en.properties` and `messages_de.properties` but is no longer referenced. A future cleanup task could remove it.

3. **`derive(app-primary, 0%)` in hover shadow.** The design doc uses this to reference `app-primary` as a colour value inside a `dropshadow()` function. This is standard JavaFX CSS syntax (derive with 0% produces the original colour), but it has not been used elsewhere in this project's theme files. If any theme has an issue, the fix is to replace it with a hardcoded rgba value matching that theme's `app-primary`.
