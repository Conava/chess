# Brainstorm Agent Memory

## Project Patterns

### Board coordinate convention
- `Square(y, x)` — y is row/rank (0 = white back rank), x is column/file (0 = a-file).
- `Board.getSquare(y, x)` follows the same convention.
- `Board.getRowCount()` returns `board[0].length` (columns) and `Board.getColCount()` returns
  `board.length` (rows) — SWAPPED names, but callers double-compensate. Do not rename without
  fixing all callers.

### Move execution contract
- `Move.getStart().getPiece()` is null after `Board.executeMove` commits the move.
  En passant and other pre-commit checks must read piece state BEFORE commit.
- `Move` captures `pieceType` at construction time — use `Move.getStart()` coordinates
  plus the history `Move` objects (not live board state) when detecting prior-move patterns.

### Check filtering pattern (chosen approach)
- Deep-copy board via `Board.getCopy()` (must be deep), simulate move, call `isCheck`.
- This is the only reliable approach; incremental undo is too risky given mutable `hasMoved` flags.
- See design doc: `docs/plans/2026-03-10-ruleset-refactor-and-missing-features-design.md`

### Castling correctness
- `getLeftEmpty` / `getRightEmpty` in `PossibleStandardKingMoves` are both buggy (boolean
  operator errors). The correct approach scans strictly between king and rook files.
- Castling-while-in-check and castling-through-check must be handled in the check filter,
  not in the move generator.

### En passant execution
- Detect at `Board.executeMove` time: pawn moves diagonally to an empty square = en passant.
- No `EnPassantMove` subclass needed; detection is implicit from board context.
