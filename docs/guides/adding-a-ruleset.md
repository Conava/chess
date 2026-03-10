# Adding a Ruleset

This guide walks through adding a new chess variant to the project using the Strategy pattern.
The application, server, and game engine are all designed so that new rulesets require no
changes outside `core` except for wiring a new enum value.

## How rulesets work

`Ruleset` (`io.github.conava.chess.core.logic.ruleset.Ruleset`) is the Strategy interface:

```java
public interface Ruleset {
    int getWidth();
    int getHeight();
    Square[][] getStartBoard(Player player1, Player player2);
    List<Move>   getLegalMoves(Square, Board, List<Move>, Player, Player);
    List<Square> getLegalSquares(Square, Board, List<Move>, Player, Player);
    boolean isValidSquare(Square square);
    boolean isCheck(Board board, Player player, List<Move> moves);

    // Default methods — override as needed:
    default String getGameLabel() { return ""; }
    default Move deserializeMove(String wireString, Board board, Player player) {
        return Move.fromString(wireString, player);
    }
}
```

`AbstractChessRuleset` is an abstract base class that implements most of the `Ruleset`
interface for standard 8x8 chess logic (pseudo-legal move dispatch, check-legality
filtering, `isCheck`). If your new ruleset is a chess variant that shares standard move
rules, extend `AbstractChessRuleset` instead of implementing `Ruleset` from scratch.

`RulesetOptions` (`io.github.conava.chess.core.logic.ruleset.RulesetOptions`) is the enum
that callers use to select a ruleset:

```java
public enum RulesetOptions {
    STANDARD("Standard Chess"),
    CHESS960("Chess 960");

    private final String displayName;
    RulesetOptions(String displayName) { this.displayName = displayName; }

    @Override public String toString() { return displayName; }
}
```

`Game.createRuleset(RulesetOptions)` maps the enum value to a concrete implementation:

```java
private Ruleset createRuleset(RulesetOptions selectedRuleset) {
    return switch (selectedRuleset) {
        case STANDARD -> new StandardChessRuleset();
        case CHESS960  -> new Chess960Ruleset();
    };
}
```

The application UI already reads `RulesetOptions` values and passes the selection through
`chess.startGame(...)` to `Game.createGame(...)`. The `ComboBox` calls `toString()` on each
enum value, so the `displayName` appears automatically. No UI changes are needed to expose a
new option.

## Step-by-step

### 1. Add a value to `RulesetOptions`

File: `modules/core/src/main/java/io/github/conava/chess/core/logic/ruleset/RulesetOptions.java`

```java
public enum RulesetOptions {
    STANDARD("Standard Chess"),
    CHESS960("Chess 960"),
    MY_VARIANT("My Variant");   // <-- add here with display name

    private final String displayName;
    RulesetOptions(String displayName) { this.displayName = displayName; }

    @Override public String toString() { return displayName; }
}
```

### 2. Create the ruleset class

**Option A: Chess variant that shares standard move rules** -- extend `AbstractChessRuleset`.
This is what Chess960 does. You only need to override the methods that differ:

```java
package io.github.conava.chess.core.logic.ruleset.myVariant;

import io.github.conava.chess.core.logic.ruleset.AbstractChessRuleset;

public class MyVariantRuleset extends AbstractChessRuleset {

    @Override
    public Square[][] getStartBoard(Player player1, Player player2) {
        // Return your variant's starting position.
        // See Chess960StartPosition for an example.
        throw new UnsupportedOperationException("TODO");
    }

    // Override only what differs from standard chess:
    // - getPseudoLegalKingSquares() if king moves differ
    // - getLegalSquares() if castling/check filtering differs
    // - getGameLabel() to show a label in the UI
    // - deserializeMove() if move wire format needs special handling
}
```

**Option B: Entirely different game** -- implement `Ruleset` directly:

```java
package io.github.conava.chess.core.logic.ruleset;

public class MyVariantRuleset implements Ruleset {

    @Override public int getWidth() { return 8; }
    @Override public int getHeight() { return 8; }

    @Override
    public Square[][] getStartBoard(Player player1, Player player2) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<Move> getLegalMoves(Square square, Board board,
                                    List<Move> moves, Player player1, Player player2) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<Square> getLegalSquares(Square square, Board board,
                                        List<Move> moves, Player player1, Player player2) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean isValidSquare(Square square) {
        return square != null
            && square.getY() >= 0 && square.getY() < getHeight()
            && square.getX() >= 0 && square.getX() < getWidth();
    }

    @Override
    public boolean isCheck(Board board, Player player, List<Move> moves) {
        throw new UnsupportedOperationException("TODO");
    }
}
```

### 3. Wire the enum value in `Game.createRuleset`

File: `modules/core/src/main/java/io/github/conava/chess/core/logic/game/Game.java`

```java
private Ruleset createRuleset(RulesetOptions selectedRuleset) {
    return switch (selectedRuleset) {
        case STANDARD   -> new StandardChessRuleset();
        case CHESS960    -> new Chess960Ruleset();
        case MY_VARIANT -> new MyVariantRuleset();   // <-- add here
    };
}
```

### 4. Write a unit test

Architecture rule: every new public class in `core` must have a unit test.

Create a test file at:
`modules/core/src/test/java/io/github/conava/chess/core/logic/ruleset/MyVariantRulesetTest.java`

```java
package io.github.conava.chess.core.logic.ruleset;

import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MyVariantRulesetTest {

    private final Ruleset ruleset = new MyVariantRuleset();
    private final Player white = new Player("White", PlayerColor.WHITE);
    private final Player black = new Player("Black", PlayerColor.BLACK);

    @Test
    void boardDimensionsAreCorrect() {
        assertEquals(8, ruleset.getWidth());
        assertEquals(8, ruleset.getHeight());
    }

    @Test
    void startBoardHasCorrectPieceCount() {
        var board = ruleset.getStartBoard(white, black);
        // assert expected piece layout
    }
}
```

### 5. Verify the setup UI exposes the new option

The setup overlays (`OfflineSetupController`, `OnlineSetupController`) populate their ruleset
choice boxes from `RulesetOptions.values()`. Adding a new enum value automatically makes it
appear in the UI -- the `ComboBox` calls `toString()` on each value, which returns the
`displayName`. No controller changes are required.

### 6. (Optional) Online play with server-provided parameters

If your ruleset needs the server to provide initialization parameters (like Chess960 needs a
position index), use the deferred board initialization pattern:

- `OnlineGame` uses `deferBoardInit=true` in its `Game` constructor call. Board and ruleset
  remain `null` until the server responds.
- When the server's `JOIN_CODE` or `SUCCESS` message arrives, `OnlineGame.handleJoinCode` /
  `handleSuccess` reads the parameters, constructs the correct `Ruleset` instance, and calls
  `initializeBoard(Ruleset)`.
- The server (`GameInstance`) generates the parameters, stores them, and injects them into
  the `JOIN_CODE` and `SUCCESS` messages as key-value pairs.

See `Chess960Ruleset` and `OnlineGame.buildRulesetFromServerParams` for the reference
implementation of this pattern.

## Reference: class hierarchy

`AbstractChessRuleset` is the shared base class for chess variants that use standard 8x8
board logic. Both `StandardChessRuleset` and `Chess960Ruleset` extend it.

`StandardChessRuleset` is a minimal subclass -- it overrides only `getStartBoard` (28 lines).

`Chess960Ruleset` overrides `getStartBoard`, `getPseudoLegalKingSquares`, `getLegalSquares`,
`getGameLabel`, and `deserializeMove`.

Per-piece move generators are shared by both rulesets:

| Class | Location |
|-------|----------|
| `PossibleStandardBishopMoves` | `logic/ruleset/possibleMoves/` |
| `PossibleStandardKingMoves` | `logic/ruleset/possibleMoves/` |
| `PossibleStandardKnightMoves` | `logic/ruleset/possibleMoves/` |
| `PossibleStandardPawnMoves` | `logic/ruleset/possibleMoves/` |
| `PossibleStandardQueenMoves` | `logic/ruleset/possibleMoves/` |
| `PossibleStandardRookMoves` | `logic/ruleset/possibleMoves/` |
| `PossibleChess960KingMoves` | `logic/ruleset/possibleMoves/` |
| `PossibleStandardPosition` | `logic/ruleset/possibleStartPositions/` |
| `Chess960StartPosition` | `logic/ruleset/chess960Ruleset/` |

Each move-generator class takes a `Square`, `Board`, and move history list, and returns a
`List<Square>` of reachable squares for that piece.

## Coordinate system

`Square(y, x)` where:
- `y` is the rank (row): `0` = white's back rank (row 1), `7` = black's back rank (row 8)
- `x` is the file (column): `0` = a-file, `7` = h-file

```
y=7  [ a8 b8 c8 d8 e8 f8 g8 h8 ]   (black's back rank)
y=6  [ a7 b7 c7 d7 e7 f7 g7 h7 ]
...
y=1  [ a2 b2 c2 d2 e2 f2 g2 h2 ]
y=0  [ a1 b1 c1 d1 e1 f1 g1 h1 ]   (white's back rank)
      x=0 x=1 x=2 x=3 x=4 x=5 x=6 x=7
```

## Known constraints when implementing a new ruleset

- `getLegalSquares` is used both for move-legality checking and for UI highlight display.
  `StandardChessRuleset` filters pseudo-legal moves by simulating each candidate on a deep
  copy of the board and calling `isCheck`. Your ruleset should do the same if check-legality
  matters. `Board.getCopy()` produces a full deep copy (fresh `Square` instances and
  `Piece.copy()` for every piece, preserving state like `hasMoved`).
- The `CastleMove` and `PromotionMove` subclasses of `Move` are handled specially by
  `Board.executeMove`. If your variant includes castling or promotion, use these types.
- `Board.executeMove` detects en passant automatically: a diagonal pawn move to an empty
  square triggers removal of the captured pawn. If your variant does not want this behavior,
  you may need to override or bypass this logic.
- `Game.evaluateGameEnd()` detects checkmate, stalemate, 50-move rule, threefold repetition,
  and insufficient material. If your variant uses different end conditions, override
  `evaluateGameEnd` in your game subtype or provide alternative detection.
