# Pawn Promotion Picker — Redesign

Date: 2026-03-10
Status: accepted

---

## 1. Analysis of the Current Implementation

### How promotion is triggered

`GameController.handleSquareClick` detects promotion inline: when the moving piece is a
`PAWN` and the destination rank is 0 or 7 it constructs a `PromotionController`, calls
`sceneManager.showOverlay("/fxml/promotion.fxml", promoCtrl)`, and immediately reads
`promoCtrl.getSelectedPiece()` after the overlay returns. The call is synchronous because
`OverlayManager.showOverlay` blocks the FX Application Thread via
`Platform.enterNestedEventLoop`. The selected `Pieces` value is passed straight to
`submitMove(from, to, promoCtrl.getSelectedPiece())`.

### What the current picker looks like

`promotion.fxml` is a `VBox` with style class `overlay-card`. It contains:
- A `Label` with key `promotion.title` ("Promote Pawn") styled as `dialog-title`
  (22 px bold, theme `app-text`).
- An `HBox` (`fx:id="pieceRow"`, spacing 16) into which `PromotionController.initialize()`
  appends four `Button` nodes at runtime.

Each button gets style class `nav-button`, which in `base.css` is 52 px tall, min-width
220 px, left-aligned, and designed for the vertical main-menu list — not for a row of
piece icons. The result is four wide rectangular nav-menu buttons placed side by side.
The piece image sits in a 64×64 `ImageView` inside each button, but the button's own
padding and minimum-width rules make every button much wider than the image needs to be,
producing an awkward, oversized row. The `overlay-card` itself has no `minWidth` override
in the FXML (it falls back to the base CSS `min-width: 420`), so the dialog is at minimum
420 px wide regardless of content. There is no animation, no hover feedback beyond the
nav-button hover style (which is a primary-colour left stripe, designed for menu items),
and no visual affordance that clicking selects a piece.

### What assets are available

- Piece PNGs at `/icon/{piece}_{color}.png` for all 12 combinations (queen, rook, bishop,
  knight, king, pawn × white, black). The four promotion pieces (queen, rook, bishop,
  knight) all have images for both colours.
- Four CSS themes: dark-charcoal, dark-purple, light-arctic, light-paper. All expose the
  same CSS lookup tokens (`app-primary`, `app-surface`, `app-border`, `app-elevated`,
  `app-text`, `overlay-card`, etc.). A promotion picker styled against these tokens will
  automatically match every theme.
- Existing overlay infrastructure: `OverlayManager` already places a dimmed backdrop
  (`overlay-dim`) behind content and enters a nested event loop. The controller pattern
  (construct → pass to `showOverlay` → read state after return) is well established.

### Root causes of the visual problem

1. `nav-button` is the wrong style class. It was designed for 220 px-wide menu items, not
   compact icon buttons.
2. The 64×64 `ImageView` inside a 220 px-wide button is surrounded by dead space on both
   sides.
3. Fixed pixel sizing (64 px) looks wrong at non-standard screen sizes or zoom levels.
4. No hover animation or selection highlight tells the user which piece is under the cursor.
5. The title "Promote Pawn" above the row adds height without adding clarity — the piece
   images are self-explanatory once the button shape is appropriate.
6. No entry animation: the overlay snaps in without any transition, which feels abrupt.

---

## 2. Approach Considered and Rejected

### Approach A — Board-aligned inline strip (deferred, not discarded)

Replace the full-window overlay with a strip of four piece cells rendered directly on top
of the board inside `boardContainer`, sized to `squareSize`. No dim backdrop. No nested
event loop — the move is submitted from the cell's click handler.

This is the right long-term direction and matches how Lichess and chess.com present
promotion. It is deferred because it requires restructuring the synchronous `showOverlay`
pattern used by `GameController`, which is a medium-effort change best addressed when the
overlay pattern is reviewed more broadly.

When implemented, Approach A should:
- Build an `HBox` of four `StackPane` cells in `GameController`, each bound to
  `squareSize`, and add it to `boardContainer`.
- Position the strip over the destination column, clamped so it never overflows the
  8-column grid.
- Reuse the `promotion-piece-btn` CSS class defined by the current implementation.
- Write a `selectedPiece` field from the cell click handler and call `submitMove` there.

This is tracked as a future task. The current implementation (Approach B) leaves the door
open because it introduces `promotion-piece-btn` as a standalone class that Approach A
can reuse without modification.

---

## 3. Chosen Approach — Approach B: Polished Modal Card

**Summary:** Keep the existing `showOverlay` flow intact. Replace `nav-button` with a new
dedicated `promotion-piece-btn` style class, make all sizing responsive via JavaFX
property bindings passed from `GameController`, remove the title label, and add a
fade+scale entry animation.

This approach has the smallest change surface — `GameController`, `OverlayManager`, and
`SceneManager` are untouched — and directly eliminates every root cause identified above.

---

## 4. Implementation Specification

### 4.1 Responsive sizing strategy

Fixed pixel values (`fitWidth=64`, `fitHeight=64`) break at non-standard resolutions. The
correct approach is to bind button size and image size to `squareSize`, the `NumberBinding`
that `GameController` already maintains:

```
squareSize = Bindings.min(
        boardContainer.widthProperty(), boardContainer.heightProperty()
).divide(8.0);
```

`GameController` passes a `cellSize` binding to `PromotionController` at construction
time:

```java
NumberBinding cellSize = squareSize.multiply(0.9);
PromotionController promoCtrl =
        new PromotionController(movingPiece.getPlayer().color(),
                sceneManager::dismissOverlay,
                cellSize);
```

The factor 0.9 produces a button that is 90% of one board square — slightly smaller than
a square to leave breathing room inside the overlay card. On an 800 px-wide game window
each square is approximately 80 px, so a cell is ~72 px. On a 2560 px-wide display each
square is ~240 px, so a cell is ~216 px. The picker scales correctly across the full range.

Inside `PromotionController.initialize()`:

```java
// Button: square cell, no min-width enforced by CSS
btn.prefWidthProperty().bind(cellSize);
btn.prefHeightProperty().bind(cellSize);

// ImageView: 78% of cell, matching setPieceOnSquare in GameController
iv.fitWidthProperty().bind(cellSize.multiply(0.78));
iv.fitHeightProperty().bind(cellSize.multiply(0.78));
```

The `PromotionController` constructor signature changes from:

```java
public PromotionController(PlayerColor playerColor, Runnable closeAction)
```

to:

```java
public PromotionController(PlayerColor playerColor, Runnable closeAction,
                           NumberBinding cellSize)
```

The field `cellSize` is stored as an instance variable and consumed in `initialize()`.

### 4.2 CSS — new class `promotion-piece-btn`

Add to `base.css` (structure only, no colour values):

```css
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

`-fx-padding: 0` is intentional. The cell size is entirely controlled by the
`prefWidth`/`prefHeight` binding; padding would add to the button dimensions and break the
binding math. The `ImageView` occupies 78% of the cell and is centred by `-fx-alignment: CENTER`.

Add to each of the four theme CSS files (`dark-charcoal.css`, `dark-purple.css`,
`light-arctic.css`, `light-paper.css`), following the same pattern as each file's
existing `.btn-ghost` block:

```css
/* ── Promotion picker ────────────────────────────────────────────────────── */

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

No `-fx-text-fill` is needed because the buttons contain only an `ImageView`, never text
(the fallback `.setText(piece.name())` path is a debug safeguard, not normal use).

### 4.3 FXML changes (`promotion.fxml`)

Remove the `<Label text="%promotion.title" .../>` entirely — the piece images are
self-explanatory and no subtitle is needed.

Remove `minWidth="300"` from the `VBox` root — the card should size itself around its
content.

Reduce `VBox` `spacing` from 20 to 8 (only the `HBox` remains, so spacing has no visual
effect, but keeping it minimal is correct). Reduce `HBox` `spacing` from 16 to 12.

The `VBox` padding of `28 32 28 32` provides the card's breathing room around the button
row and can stay as-is.

Final `promotion.fxml`:

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

### 4.4 Entry animation

In `PromotionController.initialize()`, after all buttons are added to `pieceRow`, animate
the `VBox` root in. The root is `pieceRow.getParent()` — the FXML parent is assigned
before `initialize()` is called by the `FXMLLoader`.

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

Duration of 140 ms mirrors the feel of the existing legal-move dot animation (90 ms) while
being appropriate for a larger element. The scale origin is the node's centre, so the
overlay card grows outward from its own centre — matching the visual weight of appearing
from behind the dim backdrop.

### 4.5 Full `PromotionController` after changes

The controller changes are:
- Constructor gains `NumberBinding cellSize` parameter.
- Style class changes from `"nav-button"` to `"promotion-piece-btn"`.
- `iv.setFitWidth(64)` / `iv.setFitHeight(64)` replaced by property bindings.
- `btn.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE)` added so the button does
  not grow beyond its bound pref size.
- Entry animation added at the end of `initialize()`.

### 4.6 No changes required to

- `GameController` — except the one-line constructor call that adds the `cellSize` argument
  (`squareSize.multiply(0.9)`). The `showOverlay` call and `getSelectedPiece()` read are
  unchanged.
- `OverlayManager` — no changes.
- `SceneManager` — no changes.
- `Chess` facade / `core` — zero changes (Architecture Law 4 respected).
- `messages_en.properties` / `messages_de.properties` — the `promotion.title` key is no
  longer referenced but leaving it in the bundle is harmless.

---

## 5. Resolved Questions

1. **Title label**: Removed. No subtitle needed. The piece images are sufficient context.

2. **Icon size**: Fixed pixel values are not used. Button size and image size are bound to
   `squareSize.multiply(0.9)` and `cellSize.multiply(0.78)` respectively, where `squareSize`
   is `GameController`'s existing responsive binding. The picker scales correctly from
   small windows (~800 px wide) to 4K displays (~3840 px wide) with no hardcoded pixels.

---

## 6. Affected Files

| File | Change type | Notes |
|---|---|---|
| `modules/application/src/main/resources/fxml/promotion.fxml` | Edit | Remove title label, adjust spacing, remove minWidth |
| `modules/application/src/main/java/io/github/conava/chess/application/controllers/PromotionController.java` | Edit | Add `cellSize` param, bind sizes, change style class, add entry animation |
| `modules/application/src/main/java/io/github/conava/chess/application/controllers/GameController.java` | Edit | Pass `squareSize.multiply(0.9)` to `PromotionController` constructor |
| `modules/application/src/main/resources/css/base.css` | Edit | Add `.promotion-piece-btn` structure rules |
| `modules/application/src/main/resources/css/themes/dark-charcoal.css` | Edit | Add `.promotion-piece-btn` colour rules |
| `modules/application/src/main/resources/css/themes/dark-purple.css` | Edit | Add `.promotion-piece-btn` colour rules |
| `modules/application/src/main/resources/css/themes/light-arctic.css` | Edit | Add `.promotion-piece-btn` colour rules |
| `modules/application/src/main/resources/css/themes/light-paper.css` | Edit | Add `.promotion-piece-btn` colour rules |
