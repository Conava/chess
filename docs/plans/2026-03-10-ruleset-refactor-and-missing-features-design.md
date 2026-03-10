# Design: Chess Ruleset Refactor and Missing Feature Implementation

Date: 2026-03-10
Author: brainstorm agent (autonomous)
Status: proposed

---

## Overview

The `core` module contains a `StandardChessRuleset` that generates pseudo-legal moves but
lacks the filtering and state-tracking logic required for a correct chess game. This design
covers all identified bugs and missing features, orders them by dependency, specifies the
precise architectural choices needed to implement each, and flags the deferred items that
are outside the current scope.

### Problem Statement

The game is currently not a valid chess implementation:

1. Moves that leave the king in check are accepted.
2. The game ends on king capture, not checkmate — a fundamentally wrong termination condition.
3. Castling logic is broken due to boolean operator errors in `getLeftEmpty` and `getRightEmpty`.
4. En passant is completely absent despite infrastructure stubs existing.
5. Stalemate, the 50-move rule, threefold repetition, and insufficient material are never detected.
6. `Board.getCopy()` is a shallow copy — dangerous for the move simulation required by check filtering.
7. Minor: magic constant `8` hardcoded in bounds checks, German default player names.

### Scope: In vs. Out

**In scope (this plan):**

| Item | Priority |
|---|---|
| Fix castling (`getLeftEmpty` / `getRightEmpty`) | Critical bug |
| Implement en passant in `PossibleStandardPawnMoves` | Missing rule |
| Deep copy `Board.getCopy()` | Prerequisite for check filtering |
| Implement check-legality filtering in `getLegalSquares` | Correctness |
| Implement checkmate detection | Correctness |
| Implement stalemate detection | Correctness |
| Add `EnPassantMove` subclass (marker for board execution) | Structural |
| Implement 50-move rule (halfmove clock in `Game`) | Draw rule |
| Implement threefold repetition (position hashing) | Draw rule |
| Implement insufficient material detection | Draw rule |
| Fix magic constant `8` in bounds checks | Minor cleanup |
| Fix German default player names | Minor cleanup |

**Deferred (YAGNI — not required for a correct game):**

| Item | Reason |
|---|---|
| `Board.getRowCount` / `getColCount` naming swap | Works by double-cancellation; renaming risks callers. Address in a separate cleanup ADR. |
| `OnlineGame.isLocalPlayerPiece` null guard | Pre-existing bug, unrelated to ruleset. Fix in a separate task. |
| New `Ruleset` variants (Chess960, etc.) | No requirement stated. The Strategy pattern already accommodates this. |
| Time controls / clocks | Not part of the core game logic layer. |
| Persistent move history / PGN export | No requirement stated. |

---

## Architecture

### Key Architectural Decision: How to Filter Check-Illegal Moves

This is the most consequential decision in this design because it affects `Board`, `Ruleset`,
and the move-generation pipeline.

**The requirement:** `getLegalSquares` must return only moves that do not leave the mover's
own king in check. To evaluate this, we must simulate each candidate move and ask "is the king
in check on the resulting board?"

**Three approaches were considered:**

#### Approach A (Recommended): Deep-copy Board, simulate on copy, query `isCheck`

For each pseudo-legal candidate move, create a deep copy of the current `Board`, apply the
candidate move on the copy, and call `isCheck(copy, movingPlayer, moves)`. If not in check,
the move is legal. Discard the copy afterwards.

- Pros: Pure — no mutation of live state. The existing `isCheck` implementation works
  unchanged. No new state machine needed. Easy to reason about correctness.
- Cons: Allocates 8x8 = 64 `Square` objects and up to 32 `Piece` references per candidate
  move, per piece, per turn. On a standard starting position with ~20 pseudo-legal moves per
  side this is acceptable for a local game. It would need profiling for a server handling
  many concurrent games.
- Effort: Medium. Requires fixing `Board.getCopy()` first.

#### Approach B: Incremental apply/undo on live board

Modify `Board` to support reversible `applyMove` / `undoMove` pairs. For each candidate,
apply, check, undo.

- Pros: Zero allocation for simulation.
- Cons: The `Piece.hasMoved` flag on `King` and `Rook` is not reversible without storing
  prior state. `Board.executeMove` mutates piece-list structure. Undo logic is complex and
  error-prone, especially for promotions and en passant. Significant risk of subtle bugs.
- Effort: Large.

#### Approach C: Pre-compute attacked squares without simulation

Extend `isCheck` to build a full attack-map for the opposing player. Filter out any candidate
move whose end square (or the king's resulting square) falls inside that set.

- Pros: Very fast — one pass over all opponent pieces.
- Cons: Attack-map approach does not handle discovered checks (king moves into a line that
  is opened by the king's own departure). Getting this right is equivalently complex to full
  simulation. Standard chess engines universally use simulation for this reason.
- Effort: Large and error-prone.

**Chosen approach: A (deep-copy and simulate).** The correctness argument is decisive. The
performance profile is acceptable for this application. The implementation is straightforward
once `Board.getCopy()` is fixed.

### Key Architectural Decision: Where Does Check Filtering Live?

`getLegalSquares` is defined on the `Ruleset` interface. The filtering must be inside
`StandardChessRuleset.getLegalSquares` — not in `Game.getLegalSquares`. This preserves the
Strategy pattern contract: all legality determination is the ruleset's responsibility. `Game`
remains a coordinator that trusts the ruleset's output.

### Key Architectural Decision: Where Does Game-End Detection Live?

Currently `Game.checkForGameEnd` detects king capture (wrong). It must be replaced with
post-move detection that operates after every legal move:

1. Let the opponent's `getLegalSquares` over all their pieces return all legal moves.
2. If the set is empty AND the opponent is in check: checkmate — current mover wins.
3. If the set is empty AND the opponent is NOT in check: stalemate — draw.
4. Otherwise, game continues (check the draw counters too).

This logic belongs in `Game`, not in `Ruleset`. It calls `ruleset.getLegalSquares` and
`ruleset.isCheck`. This is consistent with the existing pattern where `Game` orchestrates
and `Ruleset` evaluates.

The helper method signature in `Game`:

```
private boolean hasAnyLegalMove(Player player)
```

This iterates `board.getPieces(player)` and calls `ruleset.getLegalSquares` for each square,
returning true on the first non-empty result (short-circuits).

### Key Architectural Decision: En Passant Board Execution

En passant requires removing a pawn from a square that is not the move's end square. The
existing move execution in `Board.executeMove` does not handle this case. A new marker
subclass `EnPassantMove` (parallel to `CastleMove` and `PromotionMove`) is introduced.

`Board.executeMove` gains an `instanceof EnPassantMove` branch that removes the captured
pawn from the board at the appropriate square (same file as end square, same rank as start
square).

This follows the existing structural pattern and does not require modifying any public API.

### Key Architectural Decision: 50-Move and Repetition State in `Game`

The 50-move rule requires a halfmove clock: an int reset to zero on any pawn move or
capture, incremented otherwise. This state must persist across moves, so it belongs in `Game`
as a field `halfMoveClock`.

Threefold repetition requires a position hash. A position for this purpose is defined as
the board state (piece placement) combined with active colour, castling rights, and en passant
target file. A `Map<String, Integer>` in `Game` stores position fingerprint to occurrence
count. After each move, the fingerprint is computed and the count incremented; if it reaches
3, draw by repetition.

Neither of these require changes to the `Ruleset` interface.

### Assumption: `isCheck` in the existing `StandardChessRuleset` is correct

The `canBeCaptured` method in `StandardChessRuleset` performs a reverse-attack scan from
the king's square and correctly identifies all piece types that can threaten the king.
However, it does not check for pawn check using the opponent's attack direction. The
`possibleCaptureMoves()` method in `PossibleStandardPawnMoves` generates captures from
the perspective of the king's side, not the attacker. This means the pawn check direction
needs careful validation during implementation.

**Assumption:** The pawn check detection in `canBeCaptured` is currently correct for the
white king (pawns attacking from rank+1) and incorrect for the black king (mirror direction
not handled). This must be verified during implementation. The design treats this as a
potential defect to confirm, not a known bug.

### Assumption: `Pawn.hasMoveJustMovedTwoSquares` has a latent bug

The method reads `list.get(list.size() - 1).getStart().getPiece()` to check if the last
move was a pawn move. After `Board.executeMove`, the pawn piece is on the end square, not
the start square. The start square's piece has been set to null. Therefore, after any move
is committed, `getStart().getPiece()` will always return null, and the method will always
return false. This method cannot be used for en passant detection post-commit.

**Design decision:** En passant eligibility must be checked from the move record itself
(the `Move` object in the history list), not from the start square's current piece. The
`Move` constructor already captures `pieceType` at construction time via
`start.getPiece().getType()`, so the `moves` list records the piece type at time-of-move.
En passant detection in `PossibleStandardPawnMoves` must use `move.getStart().getPiece() instanceof Pawn`
only when checking during move generation (before the move is committed), which is how the
current call site in `getSudoLegalSquares` works. The `Pawn.hasMoveJustMovedTwoSquares`
method can be kept for this pre-commit context but must be documented clearly. The en
passant implementation in `PossibleStandardPawnMoves` will use the last `Move` in the
history list's `pieceType` field via `move.getStart()` coordinates plus the move distance,
not the `Pawn` instance method.

---

## Components

### 1. `Board.getCopy()` — Make It a Deep Copy

**File:** `modules/core/src/main/java/io/github/conava/chess/core/data/board/Board.java`

**Change:** Replace `new Board(board)` with a constructor that allocates new `Square`
objects and copies their piece references. Pieces themselves are not deep-copied because
they are effectively immutable during a simulation (we never mutate a `Piece` field during
simulation — `hasMoved` is only set in `executeMove` on the live board, not during
simulation). The deep copy must also copy `King.hasMoved` and `Rook.hasMoved` flags so that
simulated castling availability is correct.

Since pieces are stateful (`hasMoved`), the deep copy must copy the piece instances
themselves, not just the references, for any piece that carries mutable flags (`King`,
`Rook`). A clean approach: add a `copy()` method to `Piece` (abstract), implemented by
each subclass. `King.copy()` returns a new `King` with `hasMoved` copied. `Rook.copy()`
similarly. Other pieces return `new PieceType(player)`.

**New method on `Piece` (abstract):**
```java
public abstract Piece copy();
```

**`Board.getCopy()` behaviour after change:**
- Allocates a new `Square[height][width]`.
- For each cell, creates `new Square(y, x)` and calls `square.getPiece().copy()` if non-null.
- Constructs `new Board(newSquares)` — the `Board` constructor already rebuilds piece lists.

### 2. Fix Castling in `PossibleStandardKingMoves`

**File:** `modules/core/src/main/java/io/github/conava/chess/core/logic/ruleset/possibleMoves/PossibleStandardKingMoves.java`

**Bug analysis:**

`getLeftEmpty()` iterates x from 0 to `rowCount/2` inclusive. For each x, it checks:
```java
!(rook-at-x && unmoved) || !isEmpty(x)
```
Due to De Morgan, this is equivalent to:
```java
!(rook-at-x && unmoved && isEmpty(x))
```
Since a square cannot simultaneously contain a rook and be empty, the inner expression is
always false, so `getLeftEmpty()` always returns false. Result: castling queenside is
always rejected.

`getRightEmpty()` has the inverse problem: the condition uses `||` where it should use
`&&`. It returns false if the square has an unmoved rook OR is occupied — the rook itself
causes it to return false.

**Correct logic:**

`getLeftEmpty()` must return `true` if every square between the rook and the king
(exclusive of both) is empty. The rook itself (x=0 for standard queenside) is not required
to be empty. The king's square is not in the range.

For queenside castling on an 8x8 board:
- King is on e1 (x=4). Rook is on a1 (x=0).
- Squares that must be empty: b1 (x=1), c1 (x=2), d1 (x=3).

For kingside castling:
- King is on e1 (x=4). Rook is on h1 (x=7).
- Squares that must be empty: f1 (x=5), g1 (x=6).

The correct implementation does not scan the entire half of the board. It scans the squares
strictly between the king's file and the rook's file. The rook's file is determined by
`getRookLeft()` / `getRookRight()` — the x-coordinate of the found rook.

**Revised approach:** Combine the rook-find and empty-check into a single method
`canCastleToward(int direction)` that:
1. Starting from the king's x, walks toward the board edge in the given direction.
2. The first non-empty square it hits must be an unmoved `Rook`. All squares before it
   must be empty.
3. If found, castling is possible.

This is cleaner and eliminates the split `getRookLeft`/`getLeftEmpty` methods entirely.

Additionally, castling must not be allowed through check — the king may not pass through a
square that is under attack, and may not castle while in check. This requires passing the
move history to `PossibleStandardKingMoves` so that `isCheck` can be called for the
transit squares. However, `isCheck` lives on `Ruleset`, and `PossibleStandardKingMoves` does
not have a `Ruleset` reference.

**Architectural decision for castling through check:** Pass the `List<Move>` history and a
`Ruleset` reference into `PossibleStandardKingMoves`. The constructor is already called from
`StandardChessRuleset.getSudoLegalSquares`. Add parameters for check-through-castling
validation. Alternatively, move the castling check from the move-generator into the
`getLegalSquares` filter (after deep-copy simulation). The simulation approach already
handles castling-while-in-check correctly because if the king is in check before castling,
the simulated result also leaves it in check. However, castling-through-check requires
checking intermediate squares, not just the final position.

**Chosen approach for castling through check:** The check-legality filter (Approach A) will
simulate the king on the transit square as an intermediate step. Concretely:
1. When castling kingside, generate two candidate squares: f1 (transit) and g1 (destination).
2. Filter: simulate king on f1, check if in check. If yes, remove g1 from legal moves.
3. Also filter g1 itself as normal via the check simulation.

This is implemented inside the check-legality filter in `getLegalSquares`, not in the
move generator. The move generator only produces destination squares; the filter runs
simulations for transit squares for castling moves.

To identify castling moves, the filter inspects whether the candidate square is 2 files
away from the king's current position (the king never moves 2 squares except in castling).

### 3. `EnPassantMove` Subclass

**File:** `modules/core/src/main/java/io/github/conava/chess/core/logic/moves/EnPassantMove.java`

New class, parallel to `CastleMove`. Fields: start square, end square (already in `Move`),
plus `capturedPawnSquare` (the square of the pawn being captured by en passant).

`Board.executeMove` gains an `instanceof EnPassantMove` branch:
- Execute the move normally (pawn moves from start to end).
- Additionally, `board.getSquare(capturedPawnSquare.getY(), capturedPawnSquare.getX()).setPiece(null)`.
- Update piece lists to remove the captured pawn.

`Move.toProtocolString()` and `Move.fromString()` must be updated to handle en passant
wire format. The format `"e5-d6 ep"` or simply the standard coordinate `"e5-d6"` suffices
since the type can be inferred from the board state. Using the normal coordinate format and
reconstructing as `EnPassantMove` on parse (when the board context shows it is an en passant)
is one option. A simpler option: use the suffix `"=EP"` in the protocol string analogous
to `"=QUEEN"`. The online game mode needs this to reconstruct the right `Move` subtype from
the server.

**Chosen wire format for en passant:** `"e5-d6=EP"`. Added to `Move.fromString` with a
new branch. `EnPassantMove.toProtocolString()` returns `squareToProtocol(start) + "-" + squareToProtocol(end) + "=EP"`.

### 4. En Passant in `PossibleStandardPawnMoves`

**File:** `modules/core/src/main/java/io/github/conava/chess/core/logic/ruleset/possibleMoves/PossibleStandardPawnMoves.java`

**Logic:**

En passant is legal when:
1. This pawn is on rank 5 (white) or rank 4 (black) — the en passant rank (using y=4 for
   white, y=3 for black with 0=white back rank convention).
2. The last move in the move history was a pawn moving two squares.
3. That pawn landed on the same rank as this pawn (confirmed by condition 1).
4. That pawn's landing column is adjacent (x-1 or x+1) to this pawn.

The en passant end square is diagonally forward (one square in the pawn's direction, one
square toward the adjacent pawn's file).

**Implementation detail:** The `moves` list holds committed `Move` objects. After a move is
committed, `move.getStart()` still has its original coordinates (squares are immutable for
their coordinates). The piece at `move.getStart()` is null after commit (board has updated),
but `move.getStart().getPiece()` at the time `Move` was constructed captured the `pieceType`
field. Use `move.getStart()` coordinates + `move.getEnd()` coordinates + the stored
`pieceType` field (which is `Pieces.PAWN`) to determine eligibility.

The distance check: `|move.getEnd().getY() - move.getStart().getY()| == 2`.

When en passant is available, `possibleMoves()` adds the target square as an `EnPassantMove`
target (just adds the square to the list — `getLegalMoves` wraps it; the actual
`EnPassantMove` construction happens in `getLegalMoves`, not in `possibleMoves`).

Actually, since `getSudoLegalSquares` returns `List<Square>` and `getLegalMoves` wraps
each square into a plain `Move`, en passant capture squares cannot be distinguished from
regular squares at the move-list level. Two options:

**Option 1:** Change `getLegalMoves` to return a pre-constructed list that includes
`EnPassantMove` objects directly from the generator, requiring `PossibleStandardPawnMoves`
to return `List<Move>` rather than `List<Square>`. This is a deeper refactor.

**Option 2:** Keep the `List<Square>` return but annotate en passant squares with a flag,
e.g., a wrapper `record EnPassantTarget(Square square, Square capturedPawnSquare)`. The
generator returns these in a separate list; `getLegalMoves` converts them to `EnPassantMove`.

**Option 3:** When the player makes a move via `Game.movePiece`, reconstruct whether it is
en passant by inspecting the board and move history at execution time in `Game.executeMove`
or `Board.executeMove`. If the pawn moves diagonally and the destination square is empty,
it must be en passant.

**Chosen approach: Option 3** — detect en passant at execution time in `Board.executeMove`.
When a pawn moves diagonally (end.x != start.x) and the destination square is empty, the
move is en passant. Remove the captured pawn from the square at (start.y, end.x). This
removes the need for `EnPassantMove` entirely and simplifies the generator return type.

The wire protocol still needs to distinguish en passant from a regular diagonal capture for
online games. In practice, the server reconstructs the move from the board state, and both
server and client apply the same en passant detection logic at execution time. No special
protocol flag is needed.

**Revised decision:** Remove `EnPassantMove` class from scope. En passant detection at
execution time in `Board.executeMove` is sufficient. The `possibleMoves()` method in
`PossibleStandardPawnMoves` adds the en passant target square to the regular square list;
the move is identified at execution time by the diagonal pawn capture onto an empty square.

### 5. Check-Legality Filtering in `StandardChessRuleset.getLegalSquares`

**File:** `modules/core/src/main/java/io/github/conava/chess/core/logic/ruleset/standardChessRuleset/StandardChessRuleset.java`

**New flow for `getLegalSquares`:**

```
getLegalSquares(square, board, moves, player1, player2):
  pseudoLegal = getSudoLegalSquares(square, board, moves)
  legal = new ArrayList<>()
  for each targetSquare in pseudoLegal:
    boardCopy = board.getCopy()
    simulateMove(boardCopy, square, targetSquare)
    if not isCheck(boardCopy, player1, moves):
      legal.add(targetSquare)
  return legal
```

`simulateMove` applies a move on `boardCopy` by directly calling `boardCopy.executeMove(move)`.
Since this is a copy, the live board is unaffected.

For castling candidates (king moving 2 squares), also simulate the king on the intermediate
square and check for check there; if in check on the intermediate square, exclude the
castling target.

**Castling-through-check detection inside filter:**

```
if piece is King and |targetSquare.x - square.x| == 2:
  transitX = square.x + sign(targetSquare.x - square.x)
  transitSquare = board.getSquare(square.y, transitX)
  boardCopyTransit = board.getCopy()
  simulateMove(boardCopyTransit, square, transitSquare)  // king on transit square
  if isCheck(boardCopyTransit, player1, moves):
    skip this move (do not add targetSquare to legal)
    continue
```

Additionally: if the king is currently in check before the castling move, the castling
candidate must be excluded. This is automatically handled by the post-move simulation (if
the king was in check before castling, it remains in check unless it escapes — but castling
while in check is illegal. The simulation would pass only if the king escapes, which for
castling-while-in-check is never the case since the king ends on a different square anyway).
Actually: castling while in check is a special rule beyond "king ends in check." To handle
it correctly, check if the king is currently in check before adding castling candidates.
Move-generator (`PossibleStandardKingMoves`) does not currently check this. The filter
should also check: if the current position already has the moving player in check, exclude
castling candidates entirely.

**Revised filter algorithm (pseudocode):**

```
currentlyInCheck = isCheck(board, player1, moves)
for each targetSquare in pseudoLegal:
  if piece is King and |targetSquare.x - square.x| == 2:
    if currentlyInCheck: skip (cannot castle while in check)
    // check transit square
    transitBoardCopy = board.getCopy()
    simulateMove(transitBoardCopy, square, transitSquare)
    if isCheck(transitBoardCopy, player1, moves): skip
  // check final square
  finalBoardCopy = board.getCopy()
  simulateMove(finalBoardCopy, square, targetSquare)
  if not isCheck(finalBoardCopy, player1, moves):
    legal.add(targetSquare)
```

**The `player1` and `player2` naming confusion:** In `getLegalSquares`, `player1` is the
player whose piece is moving, and `player2` is the opponent. `isCheck(board, player1, moves)`
asks: is `player1`'s king under attack? This is correct — we want to ensure the mover's
king is not in check after the move.

### 6. Checkmate and Stalemate Detection in `Game`

**File:** `modules/core/src/main/java/io/github/conava/chess/core/logic/game/Game.java`

**Replace `checkForGameEnd` method:**

After `board.executeMove(move)`, increment `turnCount`, then call the new `evaluateGameEnd()`.

```
private void evaluateGameEnd() {
  Player nextPlayer = getCurrentPlayer();  // already incremented turnCount
  Player previousPlayer = (nextPlayer == player0) ? player1 : player0;

  boolean nextInCheck = ruleset.isCheck(board, nextPlayer, moves);
  boolean nextHasLegalMove = hasAnyLegalMove(nextPlayer);

  if (!nextHasLegalMove) {
    if (nextInCheck) {
      // Checkmate: previous player wins
      gameState = previousPlayer == player0
        ? GameState.WHITE_WON_BY_CHECKMATE
        : GameState.BLACK_WON_BY_CHECKMATE;
    } else {
      // Stalemate
      gameState = GameState.DRAW_BY_STALEMATE;
    }
    return;
  }

  // Draw checks
  if (halfMoveClock >= 100) {  // 50 full moves = 100 half-moves
    gameState = GameState.DRAW_BY_FIFTY_MOVE_RULE;
    return;
  }
  String positionKey = computePositionKey();
  positionHistory.merge(positionKey, 1, Integer::sum);
  if (positionHistory.get(positionKey) >= 3) {
    gameState = GameState.DRAW_BY_THREEFOLD_REPETITION;
    return;
  }
  if (isInsufficientMaterial()) {
    gameState = GameState.DRAW_BY_INSUFFICIENT_MATERIAL;
  }
}
```

**New fields in `Game`:**

```java
protected int halfMoveClock = 0;
protected Map<String, Integer> positionHistory = new HashMap<>();
```

**`hasAnyLegalMove(Player player)`:**

```java
private boolean hasAnyLegalMove(Player player) {
  Player opponent = (player == player0) ? player1 : player0;
  for (Square square : board.getPieces(player)) {
    List<Square> legal = ruleset.getLegalSquares(square, board, moves, player, opponent);
    if (!legal.isEmpty()) return true;
  }
  return false;
}
```

Note: `board.getPieces(player)` returns the live list. Iterating it while the board is
stable (no move in progress) is safe.

**Halfmove clock update** in `executeMove`, before `evaluateGameEnd`:

```java
Piece movingPiece = move.getStart().getPiece();
boolean isPawnMove = movingPiece instanceof Pawn;
boolean isCapture = !move.getEnd().isEmpty();
halfMoveClock = (isPawnMove || isCapture) ? 0 : halfMoveClock + 1;
```

Note: `move.getEnd().isEmpty()` must be checked before `board.executeMove` modifies the
board. The current code calls `checkForGameEnd(move)` before `board.executeMove(move)`.
The ordering must be preserved: halfmove clock update and en passant detection happen
before `board.executeMove`, then `board.executeMove`, then `evaluateGameEnd`.

Actually, en passant is a capture even though the destination square is empty at the moment
the move is generated. Detect it the same way as the board does: pawn moving diagonally to
an empty square. Add this condition to the capture detection:

```java
boolean isEnPassant = (movingPiece instanceof Pawn)
    && move.getStart().getX() != move.getEnd().getX()
    && move.getEnd().getPiece() == null;
boolean isCapture = move.getEnd().getPiece() != null || isEnPassant;
```

### 7. Position Key for Threefold Repetition

**Method `computePositionKey()` in `Game`:**

The position key encodes:
- Active colour (whose turn it is — the player whose move follows).
- Piece placement: for each of the 64 squares, type and colour of piece (or empty).
- Castling rights: which of the four king/rook pairs are still eligible (based on `hasMoved`).
- En passant target file: if the last move was a double pawn push, the file of the pawn
  (for potential en passant next move); otherwise `-1`.

A simple string concatenation is sufficient for a non-performance-critical implementation.
Example: `"W|wR.wN.wB.wQ.wK.wB.wN.wR|...|...|...e3"` where each position is encoded as
piece abbreviation, and the en passant file appended.

A more compact approach: build a 64-character string using a fixed encoding per square:
`'.', 'P','R','N','B','Q','K','p','r','n','b','q','k'` for empty/white/black pieces.
Append active colour and castling availability as a short suffix. Use `String.intern()` to
reduce heap overhead if there are many repeated positions.

### 8. Insufficient Material Detection

**Method `isInsufficientMaterial()` in `Game`:**

Standard FIDE insufficient material cases:
1. King vs. King.
2. King + Bishop vs. King.
3. King + Knight vs. King.
4. King + Bishop vs. King + Bishop where both bishops are on the same colour square.

Count material on both sides. If the total non-king material on either side exceeds one
minor piece, or if both sides have one bishop on different coloured squares, return false.
Otherwise return true.

This is purely `board.getPieces(player0)` and `board.getPieces(player1)` inspection — no
ruleset changes needed.

### 9. Fix Magic Numbers in Bounds Checks

**Files affected:**
- `StandardChessRuleset.isInBoundsX` / `isInBoundsY` — hardcode `8`. Replace with
  `getWidth()` and `getHeight()`.
- `PossibleStandardPawnMoves.isOnHomeSquare` — hardcodes `y == 1` and `y == 6`. Replace
  with `1` and `board.getColCount() - 2` (using board dimensions).
- `PossibleStandardKingMoves.isInBounds` — already uses `colCount` and `rowCount` from
  the board. Already clean; no change needed here.

### 10. Fix German Default Player Names

**File:** `Game.getDefaultPlayerName`

Change:
- `"Spieler 0 (Weiß)"` → `"Player 1 (White)"`
- `"Spieler 1 (Schwarz)"` → `"Player 2 (Black)"`

Note: Player 1 for white (display "1") vs. internal index 0. Match the existing pattern
of `player0` = white and `player1` = black by naming them "Player 1" and "Player 2".

---

## Data Model

No new tables or persistent schema. Changes to in-memory state:

| Change | Location | Reason |
|---|---|---|
| `halfMoveClock: int` field | `Game` | 50-move rule |
| `positionHistory: Map<String, Integer>` field | `Game` | Threefold repetition |
| `Piece.copy(): abstract Piece` | `Piece` and all subclasses | Deep copy for simulation |
| En passant execution branch | `Board.executeMove` | En passant capture |

---

## API Design

No changes to the public `Ruleset` interface or the `Chess` facade. The `Ruleset` interface
`isCheck` signature already exists. `getLegalSquares` already exists. This refactor is
entirely within `core`'s internal implementation.

The only observable behaviour change at the API surface is:
- `getLegalSquares` now returns genuinely legal moves (fewer squares in check situations).
- `getState()` now correctly returns `DRAW_BY_STALEMATE`, `WHITE_WON_BY_CHECKMATE`,
  `BLACK_WON_BY_CHECKMATE`, `DRAW_BY_FIFTY_MOVE_RULE`, `DRAW_BY_THREEFOLD_REPETITION`,
  `DRAW_BY_INSUFFICIENT_MATERIAL` at the right times.
- `movesPiece` now throws `IllegalMoveException` for moves that leave the king in check.

---

## Error Handling

| Failure mode | Current | After |
|---|---|---|
| Move that leaves king in check | Accepted | Rejected with `IllegalMoveException` |
| Castling attempt (broken) | Silently unavailable | Correctly available or unavailable |
| King capture | Triggers wrong game end | Never happens (filtered before this) |
| Null king in `lookForKing` | Returns null, causes NPE in `canBeCaptured` | Can only happen if a king is captured — no longer possible since check filtering prevents king capture |

`lookForKing` returning null: this is a defensive programming concern. After the refactor,
the king should never be capturable. Add an assertion or log warning if `lookForKing`
returns null, but do not change the method's contract (it already returns null for missing
king scenarios, which should not occur in valid game play).

---

## Testing Strategy

Every new or modified class requires tests. The existing test classes are empty stubs or
thin; they must be expanded.

### New or extended test classes

| Class | Tests |
|---|---|
| `BoardTest` | Deep copy does not share squares; deep copy does not share king/rook `hasMoved` state; en passant capture removes opponent pawn |
| `PossibleStandardKingMovesTest` | Castling kingside available, queenside available; castling blocked by intervening piece; castling blocked after king moved; castling blocked after rook moved |
| `PossibleStandardPawnMovesTest` | En passant left; en passant right; en passant not available if last move was not double push; en passant not available after different piece moves |
| `StandardChessRulesetTest` | `getLegalSquares` excludes moves that leave king in check; castling while in check excluded; castling through check excluded; pinned piece cannot move off pin line |
| `CheckmateDetectionTest` (new) | Fool's mate produces checkmate; scholar's mate produces checkmate; stalemate detected correctly |
| `DrawDetectionTest` (new) | 50-move rule triggers at move 50; threefold repetition triggers; insufficient material: K vs K, K+B vs K, K+N vs K |
| `PieceTest` (extend existing) | `Piece.copy()` for each piece type; `King.copy()` preserves `hasMoved`; `Rook.copy()` preserves `hasMoved` |

### Integration tests

An integration test that plays a full game through `OfflineGame` (via moves) and asserts
correct terminal states is the highest-value test. Consider a minimal integration test:
Fool's mate (4 half-moves) and Stalemate (Sam Loyd position or similar simple setup).

---

## Migration Plan

No public API changes. No database schema changes. No configuration changes.

The refactor is purely internal to `core`. Callers in `application` and `server` observe:
- More moves rejected (good — previously silent illegal moves now throw `IllegalMoveException`).
- `getState()` returns terminal states earlier and correctly.
- UI code must already handle `IllegalMoveException` (the exception was already declared).
- UI code must already handle all `GameState` values (they were already defined in the enum).

Check that `application` handles `DRAW_BY_*` states in the UI (display draw result). If the
current UI only handles win states visually, add draw state display as part of the
application layer work — but this is not blocking the `core` refactor.

---

## Ordered Implementation Tasks

The following ordering respects dependencies. Tasks 1 and 2 are prerequisites for everything
that follows.

| # | Task | Files Changed | Depends On |
|---|---|---|---|
| 1 | Add `Piece.copy()` abstract method and implement in all 6 piece subclasses | `Piece.java`, `King.java`, `Rook.java`, `Bishop.java`, `Knight.java`, `Queen.java`, `Pawn.java` | none |
| 2 | Fix `Board.getCopy()` to perform a deep copy using `Piece.copy()` | `Board.java` | Task 1 |
| 3 | Fix castling: rewrite `PossibleStandardKingMoves.getLeftEmpty` / `getRightEmpty` with correct logic | `PossibleStandardKingMoves.java` | none |
| 4 | Implement en passant in `PossibleStandardPawnMoves.possibleMoves()` | `PossibleStandardPawnMoves.java` | none |
| 5 | Implement en passant capture in `Board.executeMove` (detect diagonal pawn move to empty square) | `Board.java` | none |
| 6 | Implement check-legality filter in `StandardChessRuleset.getLegalSquares`, including castling-through-check guard | `StandardChessRuleset.java` | Tasks 2, 3 |
| 7 | Replace `Game.checkForGameEnd` with `evaluateGameEnd()` including checkmate, stalemate, halfmove clock, position history, and insufficient material | `Game.java` | Task 6 |
| 8 | Fix magic constants in `StandardChessRuleset.isInBoundsX/Y` and `PossibleStandardPawnMoves.isOnHomeSquare` | `StandardChessRuleset.java`, `PossibleStandardPawnMoves.java` | none |
| 9 | Fix German default player names in `Game.getDefaultPlayerName` | `Game.java` | none |
| 10 | Write/expand unit tests for all changed classes | `*Test.java` files | Tasks 1-9 |

| 11 | Extend `OnlineGame.backupGameState()` / `restoreGameState()` to cover `halfMoveClock` and `positionHistory` | `OnlineGame.java` | Task 7 |
| 12 | Apply i18n: translate all German strings in `GameState` enum and `Game.getDefaultPlayerName`; extract display strings to `messages.properties` | `GameState.java`, `Game.java`, `messages.properties` (new) | Task 9 |

Tasks 3, 4, 5, 8, and 9 have no inter-dependencies and can be executed in parallel.
Tasks 1 and 2 must complete before Task 6.
Task 6 must complete before Task 7.
Task 7 must complete before Task 11.
Task 9 can be merged into Task 12 (fix player name + extract i18n together).

---

## Open Questions — Resolved

1. **Pawn check direction in `canBeCaptured`:** Verify during implementation. If wrong,
   fix it as part of this task.

2. **`hasAnyLegalMove` performance:** Accepted for now. Document as known performance
   debt in a new ADR and in `Game.java` Javadoc. Track for future profiling.

3. **Online game compatibility:** **In scope.** `OnlineGame.backupGameState()` and
   `restoreGameState()` must be extended to back up and restore `halfMoveClock` and
   `positionHistory`. Add Task 11 (see below).

4. **`GameState` messages in German:** **In scope.** All German strings in `GameState`
   enum messages must be replaced with English. Additionally, apply basic i18n: extract
   display strings to a `messages.properties` resource bundle so the UI layer can localize
   them. Add Task 12 (see below).

5. **`Board.getRowCount` / `getColCount` swap:** Left deferred as documented. No action in
   this task.

---

## Affected Files Summary

| File | Nature of Change |
|---|---|
| `core/data/pieces/Piece.java` | Add abstract `copy()` |
| `core/data/pieces/King.java` | Implement `copy()` |
| `core/data/pieces/Rook.java` | Implement `copy()` |
| `core/data/pieces/Bishop.java` | Implement `copy()` |
| `core/data/pieces/Knight.java` | Implement `copy()` |
| `core/data/pieces/Queen.java` | Implement `copy()` |
| `core/data/pieces/Pawn.java` | Implement `copy()` |
| `core/data/board/Board.java` | Fix `getCopy()` (deep); add en passant capture branch in `executeMove` |
| `core/logic/ruleset/possibleMoves/PossibleStandardKingMoves.java` | Rewrite castling emptiness check |
| `core/logic/ruleset/possibleMoves/PossibleStandardPawnMoves.java` | Add en passant to `possibleMoves()`; fix home-square magic constant |
| `core/logic/ruleset/standardChessRuleset/StandardChessRuleset.java` | Filter check-illegal moves in `getLegalSquares`; fix bounds-check magic constants |
| `core/logic/game/Game.java` | Replace `checkForGameEnd`; add `halfMoveClock`, `positionHistory`, `evaluateGameEnd`, `hasAnyLegalMove`, `isInsufficientMaterial`, `computePositionKey`; fix player names |
| All test files listed in Testing Strategy | New or expanded tests |
| `core/logic/game/OnlineGame.java` | Extend backup/restore to cover `halfMoveClock` and `positionHistory` |
| `core/logic/game/GameState.java` | Translate German messages to English; reference `messages.properties` keys |
| `core/resources/messages.properties` (new) | All display strings for game state and player names |
