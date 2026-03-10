# Design Patterns

This document explains the three primary design patterns used in this codebase, where each one
lives, and how the pieces fit together. Understanding these patterns is essential before making
any change to game state propagation, ruleset logic, or the façade API.

## Façade — `Chess.java`

### Intent

Provide a single, stable entry point between the UI layer and the `core` module so that
controllers never need to know which `Game` subtype is active.

### Implementation

`Chess` (`modules/application/src/main/java/io/github/conava/chess/application/Chess.java`)
extends `javafx.application.Application` and holds a single private `Game` field:

```java
public class Chess extends Application {
    private Game game;

    public void startGame(boolean online, RulesetOptions selectedRuleset,
                          String playerWhiteName, String playerBlackName,
                          Map<String, String> onlineGameSettings) { ... }

    public GameState getState()           { return game == null ? null : game.getState(); }
    public Board     getBoard()           { return game == null ? null : game.getBoard(); }
    public void      movePiece(Square start, Square end) throws IllegalMoveException { ... }
    // ... all other query and action methods
}
```

Controllers (e.g., `GameController`) are constructed with a `Chess` reference and call only
`chess.*` methods. They never import or cast to `OfflineGame`, `OnlineGame`, or `ServerGame`.

### Rules enforced by this pattern

- Architecture Law 2: "Application and server code must go through `Chess.java`."
- `Chess.startGame()` delegates to `Game.createGame()` or `Game.createServerGame()` —
  the two approved factory methods.
- When no game is active, query methods return `null` or empty collections. Action methods
  throw `IllegalStateException`. This prevents NPEs from cascading into controllers.

### Where to look in the code

| File | What to read |
|------|-------------|
| `Chess.java` | All public methods — this is the complete façade surface |
| `GameController.java` | A controller that uses the façade correctly |
| `Game.java` | The wrapped type; controllers never reference this directly |

---

## Observer — `GameObserver` / `Observable`

### Intent

Decouple game state changes from UI rendering. When a move completes — whether locally,
remotely, or after a server rejection — all registered listeners are notified without the
game engine knowing anything about the UI.

### Implementation

`Observable` (`modules/core/src/main/java/.../logic/observer/Observable.java`) maintains an
observer list:

```java
public abstract class Observable {
    private final List<GameObserver> observers = new CopyOnWriteArrayList<>();

    public void addObserver(GameObserver observer) { ... }
    public void removeObserver(GameObserver observer) { ... }
    public void notifyObservers() {
        for (GameObserver observer : observers) {
            observer.onGameStateChanged();
        }
    }
}
```

`Game` extends `Observable`. Every successful `executeMove` call in `Game` ends with
`notifyObservers()`:

```java
protected void executeMove(Move move) throws IllegalMoveException {
    if (isMoveValid(move)) {
        board.executeMove(move);       // handles en passant, castling, promotion
        moves.add(move);
        evaluateGameEnd();             // checkmate, stalemate, draws
        turnCount++;
        notifyObservers();             // <-- notification point
    } else {
        throw new IllegalMoveException(move);
    }
}
```

`GameObserver` defines the single-method contract:

```java
public interface GameObserver {
    void onGameStateChanged();
}
```

`GameController` implements `GameObserver` and registers itself after the game starts:

```java
// GameController.initialize()
chess.addObserver(this);

// GameController.onGameStateChanged()
@Override
public void onGameStateChanged() {
    Platform.runLater(this::update);   // always via Platform.runLater
}
```

On the server side, `GameInstance` also implements `GameObserver`. It registers when the
game starts and uses the callback to detect terminal states (checkmate, resignation, etc.)
and relay them to both players.

### Notification points

| Where | When |
|-------|------|
| `Game.executeMove` | After every successful local move (offline and online) |
| `OnlineGame.handleFailure` | After a server-rejected move is rolled back |
| `OnlineGame.handleGameStatus` | After a `GAME_STATUS` message updates state |

### The `Platform.runLater` rule

The `onGameStateChanged()` callback arrives on whatever thread called `notifyObservers()`.
For online games this can be the `ServerCommunicationTask` network thread. All JavaFX node
mutations must happen on the FX Application Thread. Therefore every `GameObserver`
implementation in the `application` module wraps its update logic in `Platform.runLater(...)`.

```java
// Correct
@Override
public void onGameStateChanged() {
    Platform.runLater(this::update);
}

// Banned
@Override
public void onGameStateChanged() {
    update();   // may be called from a non-FX thread
}
```

### Rules enforced by this pattern

- Architecture Law 3: "UI components must implement `GameObserver` and register via
  `chess.addObserver()`. Polling game state in loops is banned."
- The `CopyOnWriteArrayList` backing in `Observable` allows `addObserver` / `removeObserver`
  to be called concurrently from any thread without corrupting the iteration in
  `notifyObservers()`.

### Where to look in the code

| File | What to read |
|------|-------------|
| `Observable.java` | Observer list implementation |
| `GameObserver.java` | The interface contract |
| `Game.java` — `executeMove` | When `notifyObservers()` is called |
| `GameController.java` | The UI-side implementation with `Platform.runLater` |
| `GameInstance.java` — `onGameStateChanged` | The server-side implementation |

---

## Strategy — `Ruleset` interface

### Intent

Allow different chess rulesets (standard, variants, future modes) to be plugged in without
branching logic inside `Game`. Each variant is a self-contained class that implements the
`Ruleset` interface.

### Implementation

`Ruleset` (`modules/core/src/main/java/.../logic/ruleset/Ruleset.java`) defines the contract:

```java
public interface Ruleset {
    int getWidth();
    int getHeight();
    Square[][] getStartBoard(Player player1, Player player2);
    List<Move>   getLegalMoves(Square, Board, List<Move>, Player, Player);
    List<Square> getLegalSquares(Square, Board, List<Move>, Player, Player);
    boolean isValidSquare(Square square);
    boolean isCheck(Board board, Player player, List<Move> moves);
}
```

`Game` holds a `Ruleset` field selected at construction time via a switch on `RulesetOptions`:

```java
private Ruleset createRuleset(RulesetOptions selectedRuleset) {
    return switch (selectedRuleset) {
        case STANDARD -> new StandardChessRuleset();
        case CHESS960  -> new Chess960Ruleset();
    };
}
```

`RulesetOptions` is the enum that callers (setup controllers) use to choose a ruleset. It
has two values: `STANDARD` and `CHESS960`. Each value carries a `displayName` field
(e.g., `"Standard Chess"`, `"Chess 960"`) used by `toString()` for UI display. The
`ComboBox` in setup controllers calls `toString()` automatically; server-side parsing
uses `valueOf()` (the constant name), so display names do not affect the wire protocol.

### Class hierarchy

`AbstractChessRuleset` is a shared abstract base class extracted from the original
`StandardChessRuleset`. It owns all shared chess logic: pseudo-legal move dispatch,
check-legality filtering via deep-copy simulation, `isCheck`, and board validation.
Protected template-method hooks allow subclasses to customize behavior:

- `getPseudoLegalKingSquares` -- which king move generator to use
- `isCastlingCandidate` -- how to detect a castling move in the legal-squares filter
- `buildCastleMove` -- how to construct the `CastleMove` for castling

`StandardChessRuleset` extends `AbstractChessRuleset` and overrides only `getStartBoard`.

`Chess960Ruleset` extends `AbstractChessRuleset` and overrides `getStartBoard`,
`getPseudoLegalKingSquares`, `getLegalSquares` (castling transit-square check),
`getGameLabel`, and `deserializeMove`.

Both `StandardChessRuleset` and `Chess960Ruleset` delegate to per-piece move generators:

| Generator class | Piece |
|----------------|-------|
| `PossibleStandardBishopMoves` | Bishop — diagonal rays |
| `PossibleStandardKingMoves` | King — one-step + castling |
| `PossibleStandardKnightMoves` | Knight — L-shapes |
| `PossibleStandardPawnMoves` | Pawn — forward + capture + two-step advance + en passant |
| `PossibleStandardQueenMoves` | Queen — delegates to rook + bishop generators |
| `PossibleStandardRookMoves` | Rook — horizontal/vertical rays |
| `PossibleChess960KingMoves` | King — one-step + Chess960 castling (king-to-rook-file encoding) |

### Chess960 as a concrete example

Chess960 (Fischer Random Chess) is the second ruleset implementation and a real-world
demonstration of the Strategy pattern. It differs from standard chess in two ways:

1. The back-rank starting position is randomized (960 valid positions).
2. Castling encodes the intent as "king moves to the rook's file" rather than "king moves
   two squares", because the king and rooks can start on any file.

`Chess960Ruleset` overrides just the methods that differ. All shared logic (pawn moves,
en passant, check detection, stalemate, etc.) is inherited from `AbstractChessRuleset`.
The `Ruleset` interface's default methods (`getGameLabel()`, `deserializeMove()`) provide
extension points that Chess960 overrides without changing any consuming code.

### Adding a new ruleset

1. Create a new class that extends `AbstractChessRuleset` (if it shares standard chess
   move logic) or implements `Ruleset` directly (if entirely different).
2. Add a new value to `RulesetOptions` with a `displayName`.
3. Add a `case` in `Game.createRuleset()` that returns an instance of the new class.
4. The setup controllers in `application` already read `RulesetOptions` values and pass them
   through. No changes are needed in the UI layer.
5. If the ruleset needs server-provided parameters for online play, use the deferred board
   initialization pattern (`deferBoardInit=true` in the `Game` constructor) as Chess960 does.

See [guides/adding-a-ruleset.md](../guides/adding-a-ruleset.md) for a step-by-step walkthrough.

### Rules enforced by this pattern

- Architecture Law 5: "New rule variants must implement `Ruleset`, not branch inside `Game`
  or `Chess`."

### `Ruleset` interface default methods

The `Ruleset` interface provides default methods that rulesets can override:

| Method | Default | Purpose |
|--------|---------|---------|
| `getGameLabel()` | `""` | Display label for the game (e.g., "Chess 960 — Position 518") |
| `deserializeMove(String, Board, Player)` | `Move.fromString(...)` | Reconstruct a `Move` from its wire string; Chess960 overrides for castling |
| `isCastlingMove(Square, Square)` | `abs(deltaX) == 2` | Detect whether a king move is a castling attempt |
| `buildCastleMove(Square, Square)` | `new CastleMove(start, end)` | Construct the appropriate `CastleMove` subtype |

### Check-legality filtering

`AbstractChessRuleset.getLegalSquares` filters pseudo-legal moves through a deep-copy simulation.
For each candidate move, it creates a deep copy of the board via `Board.getCopy()`, applies the
move on the copy, and calls `isCheck` to verify the player's king is not left in check. Moves
that fail this test are excluded. Castling is further restricted: castling while in check and
castling through check are both prevented. The player color is derived from the piece itself
(not passed as a parameter), so both colors are filtered correctly.

This approach trades CPU time for implementation simplicity and correctness. The known
performance debt is that `hasAnyLegalMove` (used for checkmate/stalemate detection) is
O(moves x pieces) per turn. See ADR 0006 for the decision rationale.

### Where to look in the code

| File | What to read |
|------|-------------|
| `Ruleset.java` | The interface every ruleset must implement |
| `RulesetOptions.java` | The enum of available rulesets (`STANDARD`, `CHESS960`) |
| `AbstractChessRuleset.java` | Shared base class with template-method hooks |
| `StandardChessRuleset.java` | Standard chess -- extends `AbstractChessRuleset`, overrides `getStartBoard` only |
| `Chess960Ruleset.java` | Chess960 -- extends `AbstractChessRuleset`, overrides castling + start position |
| `Chess960StartPosition.java` | Random position generator + Scharnagl index codec |
| `Game.java` -- `createRuleset` | How the enum is mapped to an implementation |
| `PossibleStandard*Moves.java` | Per-piece pseudo-legal move generation (shared) |
| `PossibleChess960KingMoves.java` | Chess960-specific king move generation |

---

## Secondary pattern: Template Method

`Game.executeMove` is a template method that defines a fixed sequence:

1. Validate the move via `isMoveValid` (which calls the ruleset, including check-legality filtering)
2. Apply the move on the board via `board.executeMove` (handles en passant capture removal, castling, promotion)
3. Evaluate game-end conditions via `evaluateGameEnd` (checkmate, stalemate, 50-move rule, threefold repetition, insufficient material)
4. Increment the turn counter
5. Notify observers

`OnlineGame` overrides `executeMove` to wrap this sequence with backup/restore logic
(rollback if the server rejects the move) and to forward the move to the server via
`sendMessageToServer`.

---

## Secondary pattern: Factory Method

`Game.createGame(boolean online, ...)` and `Game.createServerGame(...)` are static factory
methods that hide which concrete subtype is constructed. This is the only approved way to
create a game instance from outside `core`. Direct instantiation of `OfflineGame`, `OnlineGame`,
or `ServerGame` from `application` or `server` is banned by Architecture Law 2.
