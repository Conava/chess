package io.github.conava.chess.core.logic.ruleset.possibleMoves;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.pieces.Piece;
import io.github.conava.chess.core.data.pieces.Rook;
import io.github.conava.chess.core.data.player.Player;

/**
 * Pseudo-legal move generator for the king in Chess960 (Fischer Random Chess).
 *
 * <p>Normal one-step king moves are identical to standard chess. The castling logic
 * differs: instead of adding a fixed destination square (g-file for kingside, c-file
 * for queenside), this class adds the <em>rook's actual square</em> as the castling
 * candidate. This "king moves to the rook" encoding is the FIDE-sanctioned
 * disambiguation convention for Chess960 and is unambiguous regardless of where the
 * king and rooks started.
 *
 * <p>The castling pre-conditions remain the same as standard chess:
 * <ul>
 *   <li>The king must not have moved.</li>
 *   <li>The rook must not have moved.</li>
 *   <li>No pieces may stand between the king and the rook.</li>
 * </ul>
 *
 * <p>Check-through-transit-square filtering is handled by the ruleset layer
 * ({@code Chess960Ruleset.getLegalSquares}), not here.
 *
 * <p>Extends {@link AbstractKingMoveGenerator} which provides all shared logic:
 * adjacency moves, king/rook hasMoved checks, and the directional castling walk.
 */
public class PossibleChess960KingMoves extends AbstractKingMoveGenerator {

    /**
     * Constructs a new Chess960 king move generator.
     *
     * @param square The square the king currently occupies.
     * @param board  The board state to scan.
     */
    public PossibleChess960KingMoves(Square square, Board board) {
        super(square, board);
    }

    /**
     * After finding an unmoved rook with a clear king-to-rook path, also verifies that
     * the post-castle destination corridor is clear.
     *
     * @param rank     the rank of both king and rook
     * @param kingFile the king's file
     * @param rookFile the rook's file
     * @param kingside {@code true} for kingside, {@code false} for queenside
     * @return {@code true} if the post-castle corridor is also clear
     */
    @Override
    protected boolean onCastlingCandidateFound(int rank, int kingFile, int rookFile, boolean kingside) {
        return isCorridorClear(rank, kingFile, rookFile, kingside);
    }

    /**
     * Returns the rook's actual square as the castling target (Chess960 "king moves to
     * rook" encoding). Walks in the given direction to find the first unmoved friendly
     * rook — the same walk already performed by {@link #canCastleToward}, but returns
     * the square rather than a boolean. This single scan replaces the previously
     * redundant second walk in {@code findRookSquare}.
     *
     * @param direction {@code +1} for kingside, {@code -1} for queenside
     * @return the rook's square, or {@code null} if no eligible rook found
     */
    @Override
    protected Square getCastlingTargetSquare(int direction) {
        Player owner = square.isOccupiedBy();
        int y = square.getY();
        int x = square.getX() + direction;

        while (x >= 0 && x < rowCount) {
            Piece piece = board.getSquare(y, x).getPiece();
            if (piece == null) {
                x += direction;
                continue;
            }
            if (piece instanceof Rook rook && rook.getHasNotMoved() && piece.getPlayer().equals(owner)) {
                return board.getSquare(y, x);
            }
            // blocked
            return null;
        }
        return null;
    }

    /**
     * Checks that every square the king or rook must pass through or land on
     * (excluding their current positions) is empty.
     *
     * <p>Per FIDE Chess960 rules:
     * <ul>
     *   <li>Kingside: king moves to file 6, rook moves to file 5.
     *       Range to check: {@code [min(kingFile, 5), max(rookFile, 6)]},
     *       excluding {@code kingFile} and {@code rookFile} themselves.</li>
     *   <li>Queenside: king moves to file 2, rook moves to file 3.
     *       Range to check: {@code [min(rookFile, 2), max(kingFile, 3)]},
     *       excluding {@code kingFile} and {@code rookFile} themselves.</li>
     * </ul>
     *
     * @param rank     the rank (y-coordinate) of both king and rook
     * @param kingFile the king's current file (x-coordinate)
     * @param rookFile the rook's current file (x-coordinate)
     * @param kingside {@code true} for kingside castling, {@code false} for queenside
     * @return {@code true} if every square in the destination corridor is unoccupied
     * (ignoring the king's and rook's own squares)
     */
    private boolean isCorridorClear(int rank, int kingFile, int rookFile, boolean kingside) {
        int lo, hi;
        if (kingside) {
            // king lands on file 6, rook lands on file 5
            lo = Math.min(kingFile, 5);
            hi = Math.max(rookFile, 6);
        } else {
            // king lands on file 2, rook lands on file 3
            lo = Math.min(rookFile, 2);
            hi = Math.max(kingFile, 3);
        }

        for (int f = lo; f <= hi; f++) {
            if (f == kingFile || f == rookFile) {
                continue; // ignore the pieces' own squares
            }
            if (f < 0 || f >= rowCount) {
                continue; // out of bounds — no piece there
            }
            if (!board.getSquare(rank, f).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
