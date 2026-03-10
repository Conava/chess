package io.github.conava.chess.core.logic.ruleset.possibleMoves;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.pieces.King;
import io.github.conava.chess.core.data.pieces.Piece;
import io.github.conava.chess.core.data.pieces.Rook;
import io.github.conava.chess.core.data.player.Player;

import java.util.ArrayList;
import java.util.List;

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
 */
public class PossibleChess960KingMoves {

    private final Square square;
    private final Board board;
    private final int colCount;
    private final int rowCount;

    /**
     * Constructs a new Chess960 king move generator.
     *
     * @param square The square the king currently occupies.
     * @param board  The board state to scan.
     */
    public PossibleChess960KingMoves(Square square, Board board) {
        this.square   = square;
        this.board    = board;
        this.colCount = board.getColCount();
        this.rowCount = board.getRowCount();
    }

    /**
     * Returns all pseudo-legal squares the king can move to.
     *
     * <p>Includes one-step adjacency moves and, when castling conditions are met,
     * the rook's square for each eligible rook on the same rank.
     *
     * @return list of candidate squares (not yet filtered for check)
     */
    public List<Square> getPossibleSquares() {
        List<Square> possibleMoves = new ArrayList<>();
        Player owner = square.isOccupiedBy();

        // ---- one-step adjacency moves ----
        int[] arrY = {-1, 0, +1, +1, +1, 0, -1, -1};
        int[] arrX = {+1, +1, +1, 0, -1, -1, -1, 0};

        for (int i = 0; i < 8; i++) {
            int ny = square.getY() + arrY[i];
            int nx = square.getX() + arrX[i];
            if (isInBounds(ny, nx)) {
                Square candidate = board.getSquare(ny, nx);
                if (candidate.isEmpty() || !candidate.isOccupiedBy().equals(owner)) {
                    possibleMoves.add(candidate);
                }
            }
        }

        // ---- Chess960 castling: add the rook's square, not g/c file ----
        if (canCastleKingside()) {
            Square rookSquare = findRookSquare(+1);
            if (rookSquare != null) {
                possibleMoves.add(rookSquare);
            }
        }
        if (canCastleQueenside()) {
            Square rookSquare = findRookSquare(-1);
            if (rookSquare != null) {
                possibleMoves.add(rookSquare);
            }
        }

        return possibleMoves;
    }

    /**
     * Returns true when kingside castling preconditions are met (king unmoved,
     * unmoved friendly rook reachable to the right with no pieces between them).
     */
    private boolean canCastleKingside() {
        if (square.getPiece() instanceof King king && king.getHasMoved()) {
            return false;
        }
        return canCastleToward(+1);
    }

    /**
     * Returns true when queenside castling preconditions are met (king unmoved,
     * unmoved friendly rook reachable to the left with no pieces between them).
     */
    private boolean canCastleQueenside() {
        if (square.getPiece() instanceof King king && king.getHasMoved()) {
            return false;
        }
        return canCastleToward(-1);
    }

    /**
     * Walks from the king's file toward the board edge in the given direction,
     * checking whether castling is possible in that direction.
     *
     * <p>Starting one step away from the king, each square is inspected:
     * <ul>
     *   <li>Empty square: continue walking.</li>
     *   <li>Unmoved {@link Rook} owned by the same player as the king: castling
     *       is possible only if the post-castle destination corridor is also clear
     *       — verified by {@link #isCorridorClear}.</li>
     *   <li>Any other piece (including a moved rook, an enemy rook, or any
     *       non-rook piece): the path is blocked — return {@code false}.</li>
     * </ul>
     * If the board edge is reached without finding a rook, {@code false} is returned.
     *
     * @param direction {@code +1} for kingside (right) or {@code -1} for queenside (left)
     * @return {@code true} if an unmoved friendly rook is found with no pieces between
     *         it and the king and the post-castle destination corridor is also clear
     */
    private boolean canCastleToward(int direction) {
        Player owner = square.isOccupiedBy();
        int y = square.getY();
        int kingFile = square.getX();
        int x = kingFile + direction;

        while (x >= 0 && x < rowCount) {
            Piece piece = board.getSquare(y, x).getPiece();
            if (piece == null) {
                // empty square — keep walking
            } else if (piece instanceof Rook rook
                    && rook.getHasNotMoved()
                    && piece.getPlayer().equals(owner)) {
                // found an unmoved friendly rook with a clear king-to-rook path;
                // also verify the post-castle destination corridor is clear
                return isCorridorClear(y, kingFile, x, direction > 0);
            } else {
                // path is blocked
                return false;
            }
            x += direction;
        }
        return false;
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
     *         (ignoring the king's and rook's own squares)
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

    /**
     * Returns the square of the first unmoved friendly rook found walking in
     * {@code direction} from the king, or {@code null} if none is found.
     *
     * <p>This mirrors the walk in {@link #canCastleToward} but returns the
     * actual rook square rather than a boolean, so the caller can add it
     * directly to the candidate list.
     *
     * @param direction {@code +1} for kingside, {@code -1} for queenside
     * @return the rook's square, or {@code null}
     */
    private Square findRookSquare(int direction) {
        Player owner = square.isOccupiedBy();
        int y = square.getY();
        int x = square.getX() + direction;

        while (x >= 0 && x < rowCount) {
            Piece piece = board.getSquare(y, x).getPiece();
            if (piece == null) {
                x += direction;
                continue;
            }
            if (piece instanceof Rook rook
                    && rook.getHasNotMoved()
                    && piece.getPlayer().equals(owner)) {
                return board.getSquare(y, x);
            }
            // blocked
            return null;
        }
        return null;
    }

    /**
     * Returns {@code true} if the coordinates are within the board.
     */
    private boolean isInBounds(int y, int x) {
        return y >= 0 && y < colCount && x >= 0 && x < rowCount;
    }
}
