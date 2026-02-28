# Core Module — CLAUDE.md

## Status

Active but with known violations. The logic backbone is stable and in regular use.
Two confirmed architecture law violations exist (see "Architecture Law Compliance" below)
that must be resolved before this module can be considered clean.

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
├── exceptions              IllegalMoveException, IsCheckException — checked exceptions
└── logic
    ├── game                Game (abstract), OfflineGame, OnlineGame, ServerGame,
    │                       GameState enum, GameType enum, ServerCommunicationTask
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
| `Square` | Immutable coordinates (x, y) with a mutable `Piece` slot. Equality is coordinate-only — does not consider piece. | `Piece`, `Player` |
| `Board` | Holds the `Square[][]` grid; maintains two live piece lists (white/black) updated on every `executeMove`. Provides `getCopy()` (shallow — same `Square` references). | `Square`, `Piece`, `Move`, `CastleMove`, `PromotionMove`, `King`, `Rook` |
| `Message` | Immutable record: a `MessageType` + a string content payload in `key=value` space-separated format. Provides `getParameterValue(String)`. | `MessageType` |
| `MessageParser` | Stateless utility: `parse(String)` splits on the first `:`, `serialize(Message)` reconstitutes the string. | `Message`, `MessageType` |
| `MessageType` | Enum of wire protocol verbs: `CREATE_GAME`, `JOIN_GAME`, `JOIN_CODE`, `MOVE`, `GAME_STATUS`, `SUCCESS`, `ERROR`, `FAILURE`. | — |
| `Piece` (abstract) | Holds owning `Player` and an `iconPath` string. Exposes `getIcon()` (returns a scaled `javax.swing.ImageIcon`) and `getType()` via reflection on class name. | `Player`, `javax.swing.ImageIcon` |
| `Bishop`, `Knight`, `Queen`, `Pawn`, `Rook`, `King` | Concrete pieces. Set `iconPath` by player color. `King` and `Rook` carry a `hasMoved` boolean for castling legality. `Pawn` adds `hasMoveJustMovedTwoSquares(List<Move>)` for en passant detection. | `Piece`, `Player`, `PlayerColor` |
| `Pieces` | Enum of the six piece type identifiers. Each carries a `className` string used in reflection. | — |
| `Player` | Immutable record: `name` + `PlayerColor`. | `PlayerColor` |
| `PlayerColor` | Enum: `WHITE`, `BLACK`. | — |

### `exceptions` layer

| Class | Responsibility |
|---|---|
| `IllegalMoveException` | Thrown by `Game.executeMove` when a move fails validation. Accepts a `Move` argument but does not store it — message is empty. |
| `IsCheckException` | Declared but **never thrown** in any current code path. Accepts a `Square` but does not store it. |

### `logic.game` layer

| Class | Responsibility | Key collaborators |
|---|---|---|
| `Game` (abstract) | Owns board, players, ruleset, turn counter, and move history. Provides `movePiece`, `promoteMove`, `getLegalSquares`, `getCurrentPlayer`, `getBoard`, `getMoveList`. Validates moves via ruleset and detects king-capture game-end. Extends `Observable`. | `Board`, `Ruleset`, `Move`, `Observable`, `Player` |
| `OfflineGame` | Concrete `Game` for local two-player play. `startGame()` sets state to `RUNNING`; `endGame()` is a no-op. | `Game`, `GameState` |
| `OnlineGame` | Concrete `Game` for networked play. On construction it spawns `ServerCommunicationTask` on a new thread and blocks on a `CountDownLatch` until connection is confirmed. Overrides `executeMove` to enforce local-player-turn gating, backup/restore state on server rejection, and forward moves via `sendMessageToServer`. Handles incoming `Message` objects from the server thread via `handleMessage`. | `Game`, `ServerCommunicationTask`, `Message`, `MessageType`, `Board`, `CountDownLatch` |
| `ServerGame` | Concrete `Game` intended for server-side use. `startGame()` sets state to `RUNNING`; `endGame()` is a no-op (has a `// todo` comment). | `Game`, `GameState`, `GameType` |
| `ServerCommunicationTask` | `Runnable` that opens a TCP socket to the server, reads lines in a loop, deduplicates messages using a `HashSet`, parses them with `MessageParser`, and dispatches to `OnlineGame.handleMessage`. Provides `sendMessage` and `closeConnection`. | `OnlineGame`, `MessageParser`, `Socket` |
| `GameState` | Enum of 14 game states (German-language display strings). Covers no-game, waiting, running, win-by-checkmate/resignation/timeout for each colour, and three draw variants. | — |
| `GameType` | Enum: `OFFLINE`, `ONLINE`, `SERVER`. Set only by `ServerGame`; the field in `Game` is never read back by the module itself. | — |

### `logic.moves` layer

| Class | Responsibility | Key collaborators |
|---|---|---|
| `Move` | Immutable pair of start/end `Square` references. Records piece type and capture flag at construction time. Serialises to algebraic notation in `toString()`. Static `fromString(String, Player)` reconstructs a `Move` from that notation using reflection for promotion piece instantiation (hardcoded stale package path — see Debt). | `Square`, `Pieces`, `CastleMove`, `PromotionMove` |
| `CastleMove` | Marker subclass of `Move`. `Board.executeMove` uses `instanceof CastleMove` to trigger rook relocation. | `Move` |
| `PromotionMove` | Subclass of `Move` that carries the `targetPiece` instance. `Board.executeMove` replaces the pawn with this piece. | `Move`, `Piece` |

### `logic.observer` layer

| Class | Responsibility | Key collaborators |
|---|---|---|
| `GameObserver` (interface) | Single-method contract: `updateFromRemote()`. Implemented by UI components that need to react to server-pushed state changes. | — |
| `Observable` (abstract) | Maintains a `List<GameObserver>`. Provides `addObserver`, `removeObserver`, `notifyObservers`. `Game` extends this. | `GameObserver` |

### `logic.ruleset` layer

| Class | Responsibility | Key collaborators |
|---|---|---|
| `Ruleset` (interface) | Strategy contract: `getWidth`, `getHeight`, `getStartBoard`, `getLegalMoves`, `getLegalSquares`, `isValidSquare`, `isCheck`. | `Square`, `Board`, `Move`, `Player` |
| `RulesetOptions` | Enum with a single value: `STANDARD`. | — |
| `StandardChessRuleset` | Implements `Ruleset`. Dispatches to per-piece move generators for pseudo-legal square lists. `getLegalSquares` is a direct pass-through to `getSudoLegalSquares` — no check-legality filtering. `isCheck` uses a reverse-attack scan from the king's square. | `Ruleset`, `PossibleStandard*Moves`, `PossibleStandardPosition` |
| `PossibleStandardBishopMoves` | Generates pseudo-legal diagonal ray squares for a bishop. Stops at occupied squares. | `Square`, `Board` |
| `PossibleStandardKingMoves` | Generates pseudo-legal one-step squares plus castling squares when both king and applicable rook have not moved and intervening squares are clear. | `Square`, `Board`, `King`, `Rook` |
| `PossibleStandardKnightMoves` | Generates pseudo-legal L-shape squares for a knight. | `Square`, `Board` |
| `PossibleStandardPawnMoves` | Generates pseudo-legal forward and capture squares for a pawn, including two-square initial advance. En passant is not implemented despite `moves` being accepted as a parameter. | `Square`, `Board`, `Move`, `PlayerColor` |
| `PossibleStandardQueenMoves` | Delegates entirely to `PossibleStandardRookMoves` + `PossibleStandardBishopMoves`. | `PossibleStandardRookMoves`, `PossibleStandardBishopMoves` |
| `PossibleStandardRookMoves` | Generates pseudo-legal horizontal/vertical ray squares for a rook. | `Square`, `Board` |
| `PossibleStandardPosition` | Builds the standard 8×8 starting `Square[][]`. | `Square`, all piece classes, `Player` |

---

## Design Patterns Identified

### Observer
- `Observable` (abstract class in `logic.observer`) maintains the observer list and calls
  `notifyObservers()`.
- `GameObserver` (interface) defines `updateFromRemote()`.
- `Game` extends `Observable`. `OnlineGame.executeMoveFromRemote` and
  `OnlineGame.handleFailure` both call `notifyObservers()` directly.
- Callers outside `core` implement `GameObserver` and register via `Observable.addObserver`.
- **Scope limitation:** `notifyObservers` is only called on remote move and server rejection
  paths inside `OnlineGame`. Offline moves do not trigger observer notification.

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
```

### `Observable` (inherited by `Game`)
```java
void addObserver(GameObserver observer)
void removeObserver(GameObserver observer)
void notifyObservers()
```

### `GameObserver` (interface to be implemented by callers)
```java
void updateFromRemote()
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
void handleMessage(Message message)
void sendMessageToServer(Message message)
void backupGameState()
void restoreGameState()
String getJoinCode()
```

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
    depends on --> data.pieces         (Game uses Piece, Pieces, King)
    depends on --> data.player         (Game uses Player, PlayerColor)
    depends on --> data.io             (OnlineGame, ServerCommunicationTask use Message/MessageParser)
    depends on --> exceptions          (Game throws IllegalMoveException)

logic.ruleset.standardChessRuleset
    depends on --> logic.ruleset.possibleMoves
    depends on --> logic.ruleset.possibleStartPositions
    depends on --> data.*

logic.moves
    depends on --> data.Square, data.pieces, data.player

data.board
    depends on --> data.pieces (instanceof checks on King/Rook)
    depends on --> data.player
    depends on --> data.Square
    depends on --> logic.moves

data.pieces
    depends on --> data.player
    depends on --> logic.moves (Pawn references Move for en passant check)
    depends on --> javax.swing (VIOLATION — see below)

exceptions
    depends on --> logic.moves (IllegalMoveException takes a Move)
    depends on --> data.Square (IsCheckException takes a Square)
```

---

## Architecture Law Compliance

### Law 1 — Module boundaries are hard
**COMPLIANT.** `core/pom.xml` has no dependency on `application` or `server`.

### Law 2 — Chess facade is the only API surface
**COMPLIANT** within `core`. Enforcement on the caller side is the `application`/`server`
modules' responsibility.

### Law 3 — Observer pattern for all state propagation
**PARTIAL VIOLATION.** The `GameObserver` / `Observable` infrastructure exists and is
correctly used for remote moves in `OnlineGame`. However, `notifyObservers()` is never
called after a local (offline) move completes. Any observer registered on an `OfflineGame`
instance will never receive a notification after `movePiece` or `promoteMove`.

### Law 4 — `core` is logic-only; no UI imports
**VIOLATION.** `Piece.java` imports `javax.swing.ImageIcon` and `java.awt.Image` and
contains a method `getIcon()` that scales and returns a Swing `ImageIcon`. This is a
direct, confirmed Swing import inside a `core` class.

### Law 5 — Strategy pattern owns ruleset variation
**COMPLIANT.** `StandardChessRuleset` implements `Ruleset`. New variants must implement
`Ruleset` and be registered in `Game.createRuleset`.

---

## Known Debt / Gotchas

1. **Swing import in `Piece.java`.**
   `Piece` imports `javax.swing.ImageIcon` and `java.awt.Image` and exposes `getIcon()`.
   This directly violates Architecture Law 4. The icon path field and `getIcon()` method
   belong in the UI layer, not in the domain model. The `iconPath` field is currently
   `protected` and set inside each concrete piece constructor, coupling piece construction
   to icon resource layout.

2. **Stale reflection package path in `Game.getNewPiece` and `Move.fromString`.**
   Both methods contain hardcoded class-name strings that reference the old package
   `ptp.core.data.pieces.*` rather than the current `io.github.conava.chess.core.data.pieces.*`.
   `Game.getNewPiece` will return `null` silently on every pawn promotion (caught exception
   is swallowed). `Move.fromString` will throw `ClassNotFoundException` on any promotion
   received over the network.

3. **`getLegalSquares` does not filter moves that leave the king in check.**
   `StandardChessRuleset.getLegalSquares` delegates directly to `getSudoLegalSquares`, which
   generates pseudo-legal moves only. The `isCheck` method exists and works, but it is never
   called as part of move generation or validation. Players can make moves that leave their
   own king in check.

4. **`IsCheckException` is declared but never thrown.**
   The exception class exists but has zero call sites. It is dead code.

5. **`GameType` field in `Game` is never read.**
   `ServerGame` sets `this.gameType = GameType.SERVER` in its constructor. No code in the
   module reads `gameType` back. `OfflineGame` and `OnlineGame` never set it. The field
   serves no runtime purpose.

6. **`Board.getCopy()` is a shallow copy.**
   `getCopy()` constructs a new `Board` from the same `Square[][]` reference, not a deep
   copy. Because `Square` is mutable (it has `setPiece`), the "copy" and the original share
   the same `Square` objects. Any mutation to the copy's squares will affect the original.
   This is only safe by accident because `executeMove` swaps piece references on squares
   but does not replace square instances. It is fragile and will break if any code path
   creates new squares during a move.

7. **`Board.handleCastleMove` uses hardcoded column indices.**
   Castling logic uses literal column values (0, 2, 3, 5, 7) rather than deriving them from
   board dimensions or rook positions. This ties castling logic to a standard 8×8 board and
   will produce incorrect results if the `Ruleset` ever returns a non-standard board width.

8. **En passant is not implemented.**
   `PossibleStandardPawnMoves` accepts the move history list and has a comment placeholder,
   but the en passant logic is entirely absent. `Pawn.hasMoveJustMovedTwoSquares` exists
   but is never called.

9. **`getLeftEmpty` / `getRightEmpty` logic in `PossibleStandardKingMoves` appears incorrect.**
   `getLeftEmpty` returns `false` unless every square from column 0 to `rowCount/2` either
   has an unmoved rook or is empty, but the condition uses `||` with `!board.getSquare(...).isEmpty()`,
   meaning any non-rook, non-empty square causes an immediate false return. In practice the
   first square (column 0) always has the rook at game start, so the loop returns false
   immediately for any non-rook piece. The result is that `canCastleLong` and `canCastleShort`
   will return false even when castling should be legal. Castling is likely broken in
   practice.

10. **`OnlineGame.isLocalPlayerPiece` will throw `NullPointerException` on an empty square.**
    The method calls `board.getSquare(...).getPiece().getPlayer().color()` without a null
    guard on `getPiece()`. If the UI requests legal squares for an empty square in an online
    game, this will throw unchecked.

11. **`ServerCommunicationTask` deduplicates messages using an unbounded `HashSet`.**
    `processedMessages` grows indefinitely for the lifetime of the connection. In a long
    game this is a memory leak. Identical move strings (e.g., the same square moved back
    and forth) will also be silently dropped after the first occurrence.

12. **No unit tests exist.**
    The `src/test` directory does not exist. Architecture Law (root CLAUDE.md) requires
    every public class in `logic/` to have unit tests before a PR is done. This requirement
    is completely unmet.

13. **Default player names are in German.**
    `Game.getDefaultPlayerName` returns `"Spieler 0 (Weiß)"` and `"Spieler 1 (Schwarz)"`.
    This is a localisation inconsistency with the rest of the codebase (English identifiers,
    English comments).
