# Core Module — CLAUDE.md

## Status

Active. The logic backbone is stable and in regular use.

---

## Responsibility

The `core` module owns all chess domain logic: board state, piece movement rules, move
validation, game lifecycle (offline and online), the observer contract, the ruleset
abstraction, and the wire protocol data types used for server communication. It does
**not** own any UI rendering, application entry points, server-side socket listeners, or
persistence. The `Chess` facade that bridges `core` to `application` and `server` lives
outside this module.

---

## Package Structure

```
io.github.conava.chess.core
├── data
│   ├── (root)              Square — the single addressable cell on the board
│   ├── board               Board — 2-D board state, piece list tracking, move execution
│   ├── io                  Message, MessageParser, MessageType — wire protocol types
│   ├── pieces              Piece (abstract) + six concrete piece classes + Pieces enum
│   └── player              Player record + PlayerColor enum
├── exceptions              IllegalMoveException — checked exception
└── logic
    ├── game                Game (abstract), OfflineGame, OnlineGame, ServerGame,
    │                       GameState enum, ServerConnection interface
    ├── moves               Move, CastleMove, PromotionMove
    ├── observer            GameObserver interface, Observable abstract class
    └── ruleset
        ├── (root)          Ruleset interface, RulesetOptions enum
        ├── possibleMoves   Six per-piece pseudo-legal move generators
        ├── possibleStartPositions  PossibleStandardPosition
        └── standardChessRuleset    StandardChessRuleset
```

---

## Key Classes

### `data` layer

| Class | Responsibility | Key collaborators |
|---|---|---|
| `Square` | Immutable coordinates with a mutable `Piece` slot. Constructor is `Square(y, x)` where `y` = row/rank (0 = white's back rank) and `x` = column/file (0 = a-file). Equality is coordinate-only — does not consider piece. | `Piece`, `Player` |
| `Board` | Holds the `Square[][]` grid; maintains two live piece lists (white/black) updated on every `executeMove`. Provides `getCopy()` (shallow — same `Square` references). | `Square`, `Piece`, `Move`, `CastleMove`, `PromotionMove`, `King`, `Rook` |
| `Message` | Immutable record: a `MessageType` + a string content payload in `key=value` space-separated format. Provides `getParameterValue(String)`. | `MessageType` |
| `MessageParser` | Stateless utility: `parse(String)` splits on the first `:`, `serialize(Message)` reconstitutes the string. | `Message`, `MessageType` |
| `MessageType` | Enum of wire protocol verbs: `CREATE_GAME`, `JOIN_GAME`, `JOIN_CODE`, `MOVE`, `GAME_STATUS`, `SUCCESS`, `ERROR`, `FAILURE`. | — |
| `Piece` (abstract) | Holds owning `Player`. Exposes `getPlayer()` and `getType()` (derived via `Pieces.valueOf(getClass().getSimpleName().toUpperCase())`). No icon fields or UI imports. | `Player` |
| `Bishop`, `Knight`, `Queen`, `Pawn`, `Rook`, `King` | Concrete pieces. `King` and `Rook` carry a `hasMoved` boolean for castling legality. `Pawn` adds `hasMoveJustMovedTwoSquares(List<Move>)` for en passant detection. No `iconPath` field or icon loading. | `Piece`, `Player`, `PlayerColor` |
| `Pieces` | Enum of the six piece type identifiers. | — |
| `Player` | Immutable record: `name` + `PlayerColor`. | `PlayerColor` |
| `PlayerColor` | Enum: `WHITE`, `BLACK`. | — |

### `exceptions` layer

| Class | Responsibility |
|---|---|
| `IllegalMoveException` | Thrown by `Game.executeMove` when a move fails validation. Accepts a `Move` argument but does not store it — message is empty. |

### `logic.game` layer

| Class | Responsibility | Key collaborators |
|---|---|---|
| `Game` (abstract) | Owns board, players, ruleset, turn counter, and move history. Provides `movePiece`, `promoteMove`, `getLegalSquares`, `getCurrentPlayer`, `getBoard`, `getMoveList`. Validates moves via ruleset and detects king-capture game-end. Calls `notifyObservers()` after every successful `executeMove`. Extends `Observable`. Promotion piece instantiation uses an enum switch on `Pieces` (no reflection). | `Board`, `Ruleset`, `Move`, `Observable`, `Player` |
| `OfflineGame` | Concrete `Game` for local two-player play. `startGame()` sets state to `RUNNING`; `endGame()` is a no-op. | `Game`, `GameState` |
| `OnlineGame` | Concrete `Game` for networked play. Uses a static factory method: `OnlineGame.create(...)` constructs the instance with a private constructor without sending any network messages. The application facade must then call `connectToServerGame()` after confirming the connection is live — this two-phase construction ensures the message handler is registered before the server's first reply can arrive. Overrides `executeMove` to enforce local-player-turn gating, backup/restore state on server rejection, and forward moves via `sendMessageToServer` (using `Move.toProtocolString()` for wire serialization). Handles incoming `Message` objects dispatched by the application layer via `handleMessage`. `handleMove` catches both `IllegalMoveException` and `RuntimeException` to prevent malformed server messages from crashing the handler thread. `handleGameStatus` calls `notifyObservers()` after updating state. Promotion piece instantiation uses an enum switch on `Pieces` (no reflection). | `Game`, `ServerConnection`, `Message`, `MessageType`, `Board` |
| `ServerGame` | Concrete `Game` intended for server-side use. Constructor is package-private: `ServerGame(RulesetOptions, String playerWhiteName, String playerBlackName)`. External callers must use `Game.createServerGame()`. `startGame()` sets state to `RUNNING`; `endGame()` body is empty. | `Game`, `GameState` |
| `ServerConnection` | Interface that abstracts the networking transport. Methods: `sendMessage(String)`, `closeConnection()`, `isConnected()`. Allows `OnlineGame` to send/receive messages without importing any I/O classes. Implemented in the `application` module by `ServerCommunicationTask`. | — |
| `GameState` | Enum of 14 game states (German-language display strings). Covers no-game, waiting, running, win-by-checkmate/resignation/timeout for each colour, and three draw variants. | — |

### `logic.moves` layer

| Class | Responsibility | Key collaborators |
|---|---|---|
| `Move` | Immutable pair of start/end `Square` references. Records piece type and capture flag at construction time. Serialises to algebraic notation in `toString()` (for display only). `toProtocolString()` produces an unambiguous wire-safe format (`"e2-e4"`, `"O-O"`, `"O-O-O"`, `"a7-a8=QUEEN"`) that round-trips through `fromString(String, Player)`. Static `fromString` reconstructs a `Move` from the protocol format; promotion piece instantiation uses an enum switch on `Pieces` (no reflection). Passing `KING` or `PAWN` as a promotion target in either `fromString` or `Game.getNewPiece` throws `IllegalArgumentException`. | `Square`, `Pieces`, `CastleMove`, `PromotionMove` |
| `CastleMove` | Marker subclass of `Move`. `Board.executeMove` uses `instanceof CastleMove` to trigger rook relocation. | `Move` |
| `PromotionMove` | Subclass of `Move` that carries the `targetPiece` instance. `Board.executeMove` replaces the pawn with this piece. | `Move`, `Piece` |

### `logic.observer` layer

| Class | Responsibility | Key collaborators |
|---|---|---|
| `GameObserver` (interface) | Single-method contract: `onGameStateChanged()`. Implemented by UI components that need to react to any game state change (local move, remote move, or server rejection). | — |
| `Observable` (abstract) | Maintains an observer list backed by `CopyOnWriteArrayList<GameObserver>`, which allows `notifyObservers()` to iterate safely while another thread concurrently adds or removes observers. Provides `addObserver`, `removeObserver`, `notifyObservers`. Both `addObserver` and `removeObserver` throw `NullPointerException` for a `null` argument (enforced via `Objects.requireNonNull`). `Game` extends this. | `GameObserver` |

### `logic.ruleset` layer

| Class | Responsibility | Key collaborators |
|---|---|---|
| `Ruleset` (interface) | Strategy contract: `getWidth`, `getHeight`, `getStartBoard`, `getLegalMoves`, `getLegalSquares`, `isValidSquare`, `isCheck`. | `Square`, `Board`, `Move`, `Player` |
| `RulesetOptions` | Enum with a single value: `STANDARD`. | — |
| `StandardChessRuleset` | Implements `Ruleset`. Dispatches to per-piece move generators for pseudo-legal square lists. `getLegalSquares` filters pseudo-legal moves by deep-copying the board, simulating each candidate move, and calling `isCheck` to exclude any move that leaves the moving player's king in check. Castling is additionally filtered for moving out of or through check. `isCheck` uses a reverse-attack scan from the king's square. | `Ruleset`, `PossibleStandard*Moves`, `PossibleStandardPosition` |
| `PossibleStandardBishopMoves` | Generates pseudo-legal diagonal ray squares for a bishop. Stops at occupied squares. | `Square`, `Board` |
| `PossibleStandardKingMoves` | Generates pseudo-legal one-step squares plus castling squares when both king and applicable rook have not moved and intervening squares are clear. | `Square`, `Board`, `King`, `Rook` |
| `PossibleStandardKnightMoves` | Generates pseudo-legal L-shape squares for a knight. | `Square`, `Board` |
| `PossibleStandardPawnMoves` | Generates pseudo-legal forward and capture squares for a pawn, including two-square initial advance and en passant. En passant is detected by inspecting the last move in the history for a double pawn push to an adjacent file. | `Square`, `Board`, `Move`, `PlayerColor` |
| `PossibleStandardQueenMoves` | Delegates entirely to `PossibleStandardRookMoves` + `PossibleStandardBishopMoves`. | `PossibleStandardRookMoves`, `PossibleStandardBishopMoves` |
| `PossibleStandardRookMoves` | Generates pseudo-legal horizontal/vertical ray squares for a rook. | `Square`, `Board` |
| `PossibleStandardPosition` | Builds the standard 8×8 starting `Square[][]`. | `Square`, all piece classes, `Player` |

---

## Design Patterns Identified

### Observer
- `Observable` (abstract class in `logic.observer`) maintains the observer list and calls
  `notifyObservers()`.
- `GameObserver` (interface) defines `onGameStateChanged()`.
- `Game` extends `Observable`. `Game.executeMove` calls `notifyObservers()` after every
  successful move (both offline and online). `OnlineGame.handleFailure` also calls
  `notifyObservers()` directly after a server-rejected move is rolled back.
- Callers outside `core` implement `GameObserver` and register via `Observable.addObserver`.

### Strategy
- `Ruleset` interface is the strategy contract.
- `StandardChessRuleset` is the only concrete implementation.
- `Game` holds a `Ruleset` field; ruleset selection is done via the `RulesetOptions` enum
  switch in `Game.createRuleset`. Adding a new variant requires adding an enum value and a
  new implementation class.

### Template Method
- `Game` is abstract with `startGame()` and `endGame()` declared abstract. `Game.executeMove`
  is a concrete template method that calls `isMoveValid`, `checkForGameEnd`, and
  `board.executeMove` in a fixed sequence. `OnlineGame` overrides `executeMove` to wrap
  the template with backup/restore and server forwarding.

---

## Public API

The following are the methods callers outside `core` depend on. There is no `Chess` facade
inside this module — the facade lives in the `application` module. These are the methods that
facade wraps.

### `Game` (via its concrete subclasses)
```java
static Game createGame(boolean online, RulesetOptions selectedRuleset,
                       String playerWhiteName, String playerBlackName,
                       Map<String, String> onlineGameSettings,
                       ServerConnection connection)
static Game createServerGame(RulesetOptions selectedRuleset,
                             String playerWhiteName, String playerBlackName)
void startGame()
void endGame()
void movePiece(Square squareStart, Square squareEnd) throws IllegalMoveException
void promoteMove(Square squareStart, Square squareEnd, Pieces targetPiece) throws IllegalMoveException
List<Square> getLegalSquares(Square position)
Player getCurrentPlayer()
Player getPlayerWhite()
Player getPlayerBlack()
GameState getState()
List<String> getMoveList()
Piece getPieceAt(Square position)
Board getBoard()
void setGameState(GameState gameState)
String getJoinCode()                          // returns null for non-online games
void connectToServerGame()                    // no-op for non-online games
void handleMessage(Message message)           // no-op for non-online games
```

### `Observable` (inherited by `Game`)
```java
void addObserver(GameObserver observer)
void removeObserver(GameObserver observer)
void notifyObservers()
```

### `GameObserver` (interface to be implemented by callers)
```java
void onGameStateChanged()
```

### `Ruleset` (interface)
```java
int getWidth()
int getHeight()
Square[][] getStartBoard(Player player1, Player player2)
List<Move> getLegalMoves(Square square, Board board, List<Move> moves, Player player1, Player player2)
List<Square> getLegalSquares(Square square, Board board, List<Move> moves, Player player1, Player player2)
boolean isValidSquare(Square square)
boolean isCheck(Board board, Player player, List<Move> moves)
```

### `OnlineGame` (additional public surface)
```java
static OnlineGame create(RulesetOptions, String, String, Map<String,String>, ServerConnection)
void sendMessageToServer(Message message)
void backupGameState()
void restoreGameState()
```
`connectToServerGame()`, `handleMessage(Message)`, and `getJoinCode()` are overrides of `Game`
base methods and are callable through `Game` references without casting.

### Data types used at the boundary
- `Square(int y, int x)` — coordinate pair; callers construct these to address board cells.
- `Pieces` enum — passed to `promoteMove`.
- `GameState` enum — returned by `getState()`.
- `PlayerColor` enum — used to identify sides.
- `Message`, `MessageParser`, `MessageType` — used by `server` module and `OnlineGame`.

---

## Internal Dependencies

```
logic.game
    depends on --> logic.observer      (Game extends Observable)
    depends on --> logic.ruleset       (Game holds Ruleset)
    depends on --> logic.moves         (Game uses Move / CastleMove / PromotionMove)
    depends on --> data.board          (Game holds Board)
    depends on --> data.pieces         (Game uses Piece, Pieces, King, Bishop, Knight, Queen, Rook)
    depends on --> data.player         (Game uses Player, PlayerColor)
    depends on --> data.io             (OnlineGame uses Message/MessageParser/MessageType)
    depends on --> exceptions          (Game throws IllegalMoveException)
    ServerConnection (interface in logic.game) — no external deps; implemented in application module

logic.ruleset.standardChessRuleset
    depends on --> logic.ruleset.possibleMoves
    depends on --> logic.ruleset.possibleStartPositions
    depends on --> data.*

logic.moves
    depends on --> data.Square, data.pieces (Bishop/Knight/Queen/Rook for fromString switch), data.player

data.board
    depends on --> data.pieces (instanceof checks on King/Rook)
    depends on --> data.player
    depends on --> data.Square
    depends on --> logic.moves

data.pieces
    depends on --> data.player
    depends on --> logic.moves (Pawn references Move for en passant check)
    No UI or I/O imports.

exceptions
    depends on --> logic.moves (IllegalMoveException takes a Move)
```

---

## Architecture Law Compliance

### Law 1 — Module boundaries are hard
**COMPLIANT.**

### Law 2 — Chess facade is the only API surface
**COMPLIANT**

### Law 3 — Observer pattern for all state propagation
**COMPLIANT.** `Game.executeMove` calls `notifyObservers()` after every successful move,
covering both offline and online paths. `OnlineGame.handleFailure` calls `notifyObservers()`
after rolling back a server-rejected move. `OnlineGame.handleGameStatus` calls `notifyObservers()`
after updating state from a `GAME_STATUS` message.

### Law 4 — `core` is logic-only; no UI imports
**COMPLIANT.**

### Law 5 — Strategy pattern owns ruleset variation
**COMPLIANT.**

---

## Known Debt / Gotchas

1. ~~`getLegalSquares` does not filter moves that leave the king in check.~~
   **RESOLVED.** `StandardChessRuleset.getLegalSquares` now deep-copies the board for each
   candidate square, simulates the move, and calls `isCheck` to exclude moves that leave
   the moving player's king in check. Castling through or out of check is also filtered.

2. ~~`Board.getCopy()` is a shallow copy.~~
   **RESOLVED.** `getCopy()` now creates fresh `Square` instances and uses `Piece.copy()`
   (abstract, overridden by all six piece classes) to faithfully reproduce stateful fields
   such as `King.hasMoved` and `Rook.hasMoved`.

3. ~~En passant is not implemented.~~
   **RESOLVED.** `PossibleStandardPawnMoves.enPassantMoves()` checks the last move for a
   double pawn push and adds the diagonal target square when conditions are met.
   `Board.executeMove` handles the captured-pawn removal when a pawn moves diagonally
   to an empty square.

4. ~~Castling logic in `PossibleStandardKingMoves` is broken.~~
   **RESOLVED.** `canCastleToward(int direction)` walks from the king toward the board edge,
   checking for an unmoved friendly rook with no pieces between them. Both kingside and
   queenside castling work correctly.

5. **`OnlineGame.isLocalPlayerPiece` will throw `NullPointerException` on an empty square.**
   The method calls `board.getSquare(...).getPiece().getPlayer().color()` without a null
   guard on `getPiece()`. If the UI requests legal squares for an empty square in an online
   game, this will throw unchecked.

6. **Unit test coverage is partial.**
   Tests cover `Observable`, `Game.getNewPiece`, `Move.fromString`/`toProtocolString`, and
   `OnlineGame` server-connection behaviour. Many public classes in `logic/` and `data/` still
   have no tests (e.g. `Board`, `StandardChessRuleset`, individual piece generators). Full
   coverage required by Architecture Law is not yet achieved.

7. **`Board.getRowCount()` and `Board.getColCount()` names are swapped relative to what they return.**
   `getRowCount()` returns `board[0].length` (the inner array = columns) and `getColCount()`
   returns `board.length` (the outer array = rows). `PossibleStandardKingMoves` double-swaps
   them which cancels out, so behaviour is currently correct by accident. Out of scope for
   this branch — document only.

8. **Default player names are in German.**
   `Game.getDefaultPlayerName` returns `"Spieler 0 (Weiß)"` and `"Spieler 1 (Schwarz)"`.
   This is a localisation inconsistency with the rest of the codebase (English identifiers,
   English comments).
