# Chess Façade API Reference

`Chess` (`io.github.conava.chess.application.Chess`) is the sole entry point between the UI
layer and the `core` game engine. All controllers and services in the `application` module
interact with `core` exclusively through this class.

**Source file:** `modules/application/src/main/java/io/github/conava/chess/application/Chess.java`

---

## Game lifecycle

### `startGame`

```java
public void startGame(
    boolean online,
    RulesetOptions selectedRuleset,
    String playerWhiteName,
    String playerBlackName,
    Map<String, String> onlineGameSettings
)
```

Starts a new game. If a game is already running, this method logs a warning and returns
without doing anything (the caller cannot detect a rejected start).

**Offline game:**

```java
chess.startGame(false, RulesetOptions.STANDARD, "Alice", "Bob", null);
```

The `onlineGameSettings` parameter is ignored when `online` is `false`. Pass `null`.

**Online game:**

```java
Map<String, String> settings = Map.of("ip", "192.168.1.10", "port", "54321");
chess.startGame(true, RulesetOptions.STANDARD, "Alice", "Bob", settings);
```

Required keys in `onlineGameSettings`:
- `"ip"` — server IP address or hostname
- `"port"` — server port as a decimal string

The online path performs a three-step setup internally:
1. Constructs `ServerCommunicationTask` and starts it on a daemon thread.
2. Awaits the connection latch; if the connection fails, sets `GameState.SERVER_ERROR` and
   returns.
3. Calls `onlineGame.connectToServerGame()` to send the `CREATE_GAME` or `JOIN_GAME`
   handshake.

The `onlineGameSettings` map may contain an optional `"joinCode"` key. When present, the
online game joins an existing lobby; when absent, a new game is created and the join code is
available via `getJoinCode()` after the `GAME_START` notification.

---

### `endGame`

```java
public void endGame()
```

Terminates the active game and releases all resources. Sets the internal `game` field to
`null`. No-op if no game is active.

---

## Move execution

### `movePiece`

```java
public void movePiece(Square start, Square end) throws IllegalMoveException
```

Executes a standard move from `start` to `end`.

Throws:
- `IllegalMoveException` — the move is not in the legal-move list for the piece on `start`
- `IllegalStateException` — no game is active

In `GameController`, this is always called from `ExecuteMove` (a `Task<Void>`) on a
background thread, not the FX Application Thread.

---

### `promoteMove`

```java
public void promoteMove(Square start, Square end, Pieces targetPiece) throws IllegalMoveException
```

Executes a pawn promotion move. The pawn on `start` moves to `end` and is replaced by
`targetPiece`.

Valid values for `targetPiece`: `Pieces.QUEEN`, `Pieces.ROOK`, `Pieces.BISHOP`,
`Pieces.KNIGHT`. Passing `Pieces.KING` or `Pieces.PAWN` throws `IllegalArgumentException`
from the underlying game logic.

Throws:
- `IllegalMoveException` — the move is illegal
- `IllegalStateException` — no game is active

---

## State queries

All query methods return `null` or an empty collection when no game is active. They never
throw.

### `getState`

```java
public GameState getState()
```

Returns the current game state. Returns `null` when no game is active.

**`GameState` enum values:**

| Value | Meaning |
|-------|---------|
| `NO_GAME` | No game object exists |
| `WAITING_FOR_PLAYER` | Online game created; waiting for opponent to join |
| `RUNNING` | Game is in progress |
| `SERVER_ERROR` | Connection to server failed |
| `WHITE_WON_BY_CHECKMATE` | White wins by checkmate |
| `BLACK_WON_BY_CHECKMATE` | Black wins by checkmate |
| `WHITE_WON_BY_RESIGNATION` | Black resigned or disconnected |
| `BLACK_WON_BY_RESIGNATION` | White resigned or disconnected |
| `WHITE_WON_BY_TIMEOUT` | Black ran out of time (not yet enforced) |
| `BLACK_WON_BY_TIMEOUT` | White ran out of time (not yet enforced) |
| `DRAW_BY_STALEMATE` | No legal moves available; player is not in check |
| `DRAW_BY_INSUFFICIENT_MATERIAL` | Neither side has enough material to force checkmate |
| `DRAW_BY_THREEFOLD_REPETITION` | Same position has occurred three times |
| `DRAW_BY_FIFTY_MOVE_RULE` | 50 full moves without a capture or pawn advance |

Note: timeout states exist in the enum but are not yet detected by the game engine.
All other states (checkmate, stalemate, draws) are detected automatically by `Game.evaluateGameEnd()`
after each move.

---

### `getBoard`

```java
public Board getBoard()
```

Returns a deep copy of the current board. The copy contains fresh `Square` instances and
deep-copied `Piece` objects (via `Piece.copy()`), preserving piece state such as `hasMoved`
on kings and rooks. Returns `null` when no game is active.

---

### `getCurrentPlayer`

```java
public Player getCurrentPlayer()
```

Returns the player whose turn it is. Returns `null` when no game is active.

---

### `getPlayerWhite`

```java
public Player getPlayerWhite()
```

Returns the white player. Returns `null` when no game is active.

---

### `getPlayerBlack`

```java
public Player getPlayerBlack()
```

Returns the black player. Returns `null` when no game is active.

---

### `getPieceAt`

```java
public Piece getPieceAt(Square position)
```

Returns the piece at the given square, or `null` if the square is empty or no game is
active. `Square` objects are constructed as `new Square(y, x)` where `y` is the rank
(row) and `x` is the file (column), both 0-indexed.

---

### `getLegalSquares`

```java
public List<Square> getLegalSquares(Square position)
```

Returns the list of squares the piece on `position` can legally move to. Returns an empty
list if no game is active, the square is empty, or the piece does not belong to the current
player.

The underlying `StandardChessRuleset.getLegalSquares` filters pseudo-legal moves through
deep-copy simulation to enforce check legality. Moves that would leave the player's king
in check are excluded. En passant captures are included when applicable. Castling is
excluded when the king is in check or would pass through check.

---

### `getMoveList`

```java
public List<String> getMoveList()
```

Returns all moves played so far as protocol strings (e.g., `"e2-e4"`, `"O-O"`,
`"a7-a8=QUEEN"`). Returns an empty list when no game is active.

---

### `getJoinCode`

```java
public String getJoinCode()
```

Returns the join code for the hosted online game (the game ID assigned by the server).
Returns `null` for offline games or when no game is active. The join code is available
after `WAITING_FOR_PLAYER` state is reached via the observer callback.

---

## Observer registration

```java
public void addObserver(GameObserver observer)
public void removeObserver(GameObserver observer)
```

Register or unregister a `GameObserver`. Both methods throw `IllegalStateException` when
no game is active. Register after calling `startGame()`.

```java
public interface GameObserver {
    void onGameStateChanged();
}
```

The callback is invoked on whichever thread called `notifyObservers()`. For online games
this may be the network thread. Always dispatch UI updates via `Platform.runLater()`:

```java
chess.addObserver(this);           // register (GameController implements GameObserver)

@Override
public void onGameStateChanged() {
    Platform.runLater(this::update);   // update is safe to call on the FX thread
}
```

---

## Data types used at the boundary

These types are defined in `core` and used as parameters or return values of the façade:

| Type | Package | Notes |
|------|---------|-------|
| `Square` | `core.data` | Coordinate pair `(y, x)`. `new Square(y, x)`. |
| `Board` | `core.data.board` | 8×8 grid. Use `getSquare(y, x)`, `getPieces(player)`. |
| `Piece` | `core.data.pieces` | Abstract. `getType()` returns `Pieces` enum; `getPlayer()` returns `Player`. |
| `Pieces` | `core.data.pieces` | Enum: `PAWN`, `ROOK`, `KNIGHT`, `BISHOP`, `QUEEN`, `KING`. |
| `Player` | `core.data.player` | Immutable record: `name()`, `color()` (`PlayerColor.WHITE` / `BLACK`). |
| `PlayerColor` | `core.data.player` | Enum: `WHITE`, `BLACK`. |
| `GameState` | `core.logic.game` | Enum of 14 states. |
| `RulesetOptions` | `core.logic.ruleset` | Enum: `STANDARD` (only value). |
| `IllegalMoveException` | `core.exceptions` | Thrown by `movePiece` and `promoteMove`. |

---

## Error behavior summary

| Condition | Behaviour |
|-----------|-----------|
| `startGame()` called when game is already running | Logs warning; returns without starting new game |
| Query methods called with no active game | Return `null` or empty collection |
| `movePiece` / `promoteMove` with no active game | Throws `IllegalStateException` |
| `addObserver` / `removeObserver` with no active game | Throws `IllegalStateException` |
| Online game: server unreachable | Sets `GameState.SERVER_ERROR`; returns the game object |
| Illegal move | Throws `IllegalMoveException` |
| Promotion to `KING` or `PAWN` | Throws `IllegalArgumentException` from core |
