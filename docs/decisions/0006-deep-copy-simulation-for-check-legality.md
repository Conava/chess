# 0006: Deep-Copy Simulation for Check-Legality Filtering

Status: accepted
Date: 2026-03-10
Deciders: Project maintainer

## Context and Problem Statement

`StandardChessRuleset.getLegalSquares` generated pseudo-legal moves only -- it did not verify
whether a candidate move would leave the player's own king in check. Players could move into
check, and there was no checkmate or stalemate detection. The ruleset needed a mechanism to
filter illegal moves and enable proper game-end detection.

Three general approaches exist for filtering pseudo-legal moves down to legal moves:

1. **Deep-copy simulation** -- for each candidate move, deep-copy the board, apply the move
   on the copy, and test whether the king is in check on the resulting position.
2. **Undo/redo (make/unmake)** -- apply the move on the live board, test for check, then
   undo the move. Avoids allocation but requires every board mutation to be reversible.
3. **Attack-map precomputation** -- maintain a bitboard or attack map that tracks which
   squares are attacked. Filter moves by consulting the map instead of simulating.

## Considered Options

1. Deep-copy simulation
2. Undo/redo (make/unmake move)
3. Attack-map precomputation

## Decision Outcome

Chosen option: "Deep-copy simulation", because it is the simplest to implement correctly
given the existing codebase, introduces no new invariants on `Board`, and the correctness
risk of undo/redo (forgetting to reverse a side effect) is high for a codebase that already
has complex move execution (castling, en passant, promotion, captured-piece-list maintenance).

### Consequences

- Good: Implementation is straightforward. Each candidate is tested in isolation on a
  disposable copy. No risk of corrupting the live board state.
- Good: `Piece.copy()` (new abstract method on `Piece`, implemented by all 6 subclasses)
  and `Board.getCopy()` (deep copy with fresh `Square` instances) are reusable for other
  features (e.g., AI search, position analysis).
- Bad: Performance cost. `hasAnyLegalMove` (used for checkmate/stalemate detection) is
  O(moves x pieces) per turn because it generates legal moves for every piece of the
  current player, each requiring a deep copy. This is acceptable for human-speed play
  but would need optimization for AI or high-speed automated games.
- Neutral: En passant detection during `Board.executeMove` (diagonal pawn move to empty
  square removes the captured pawn) works correctly on both the live board and deep copies
  because the copy preserves full board state.

## Pros and Cons of the Options

### Deep-copy simulation

- Good: Simplest to implement and verify. No new invariants.
- Good: Each simulation is fully isolated -- no risk of state leakage.
- Good: `Piece.copy()` and `Board.getCopy()` are useful beyond check filtering.
- Bad: O(N) allocations per legal-move query where N is the number of pseudo-legal moves.
- Bad: `hasAnyLegalMove` multiplies this cost across all pieces.

### Undo/redo (make/unmake)

- Good: Zero allocation overhead -- operates on the live board.
- Good: Standard technique in chess engines for performance-critical search.
- Bad: Every side effect of `Board.executeMove` must be perfectly reversible: piece capture,
  en passant removal, castling rook movement, promotion replacement, piece-list updates,
  `hasMoved` flag changes. Missing any reversal corrupts the board silently.
- Bad: High implementation complexity and bug risk in this codebase.

### Attack-map precomputation

- Good: O(1) check queries after map construction.
- Good: Enables fast pin detection and move filtering.
- Bad: Requires a parallel data structure (attack map) kept in sync with the board at all
  times. Significant refactoring effort.
- Bad: Does not eliminate the need to simulate special moves (castling, en passant) for
  legality.

## Known Performance Debt

`hasAnyLegalMove` iterates all pieces of the current player, generates legal squares for
each (which involves deep-copy simulation per candidate move), and returns true as soon as
any piece has at least one legal move. Worst case (stalemate check) is O(P x M) deep copies
where P is the number of pieces and M is the average pseudo-legal move count per piece.

This is documented as known debt. If performance becomes a concern (e.g., for AI integration),
the recommended path is to switch to undo/redo for the inner simulation loop while keeping
the current deep-copy approach for the public API (`getLegalSquares` called by the UI).
