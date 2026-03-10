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
 * Abstract base class for king pseudo-legal move generators.
 *
 * <p>Provides shared logic for one-step adjacency moves and the castling precondition
 * checks (king unmoved, unmoved friendly rook reachable with no pieces between them).
 * Subclasses differ in:
 * <ul>
 *   <li>Whether an additional corridor-clear check is needed before castling is allowed
 *       — controlled by {@link #onCastlingCandidateFound}.</li>
 *   <li>What square is added to the candidate list when castling is possible
 *       — controlled by {@link #getCastlingTargetSquare}.</li>
 * </ul>
 */
abstract class AbstractKingMoveGenerator {

    /**
     * The square the king currently occupies.
     */
    protected final Square square;
    /**
     * The board to inspect.
     */
    protected final Board board;
    /**
     * Board column count (used for horizontal bounds checking).
     */
    protected final int colCount;
    /**
     * Board row count (used for bounds checking in the castling walk).
     */
    protected final int rowCount;

    /**
     * Constructs a king move generator for the given king square and board.
     *
     * @param square the square the king currently occupies
     * @param board  the board state to inspect
     */
    protected AbstractKingMoveGenerator(Square square, Board board) {
        this.square = square;
        this.board = board;
        this.colCount = board.getColCount();
        this.rowCount = board.getRowCount();
    }

    /**
     * Returns all pseudo-legal squares the king can move to.
     *
     * <p>Includes one-step adjacency moves (all 8 directions) and, when castling
     * preconditions are met, the castling target square for each eligible direction.
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

        // ---- castling candidates ----
        if (canCastleKingside()) {
            Square target = getCastlingTargetSquare(+1);
            if (target != null) {
                possibleMoves.add(target);
            }
        }
        if (canCastleQueenside()) {
            Square target = getCastlingTargetSquare(-1);
            if (target != null) {
                possibleMoves.add(target);
            }
        }

        return possibleMoves;
    }

    /**
     * Returns {@code true} if the coordinates are within the board bounds.
     *
     * @param y rank index (0-based)
     * @param x file index (0-based)
     * @return {@code true} if in bounds
     */
    protected boolean isInBounds(int y, int x) {
        return y >= 0 && y < colCount && x >= 0 && x < rowCount;
    }

    /**
     * Returns {@code true} when kingside castling preconditions are met
     * (king unmoved, unmoved friendly rook reachable to the right with no pieces between them).
     */
    protected boolean canCastleKingside() {
        if (square.getPiece() instanceof King king && king.getHasMoved()) {
            return false;
        }
        return canCastleToward(+1);
    }

    /**
     * Returns {@code true} when queenside castling preconditions are met
     * (king unmoved, unmoved friendly rook reachable to the left with no pieces between them).
     */
    protected boolean canCastleQueenside() {
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
     *   <li>Unmoved {@link Rook} owned by the same player as the king: delegates to
     *       {@link #onCastlingCandidateFound} for any additional validation.</li>
     *   <li>Any other piece: the path is blocked — return {@code false}.</li>
     * </ul>
     *
     * @param direction {@code +1} for kingside (right) or {@code -1} for queenside (left)
     * @return {@code true} if castling is possible in the given direction
     */
    protected boolean canCastleToward(int direction) {
        Player owner = square.isOccupiedBy();
        int y = square.getY();
        int kingFile = square.getX();
        int x = kingFile + direction;

        while (x >= 0 && x < rowCount) {
            Piece piece = board.getSquare(y, x).getPiece();
            if (piece == null) {
                x += direction;
                continue;
            }
            if (piece instanceof Rook rook && rook.getHasNotMoved() && piece.getPlayer().equals(owner)) {
                return onCastlingCandidateFound(y, kingFile, x, direction > 0);
            }
            // path blocked
            return false;
        }
        return false;
    }

    /**
     * Called when an eligible rook is found during the castling walk.
     * Subclasses may perform additional validation (e.g. corridor-clear check for Chess960).
     *
     * @param rank     the rank (y-coordinate) of both king and rook
     * @param kingFile the king's current file (x-coordinate)
     * @param rookFile the rook's current file (x-coordinate)
     * @param kingside {@code true} if walking towards the kingside (right), {@code false} for queenside
     * @return {@code true} if castling is allowed; {@code false} if additional checks fail
     */
    protected abstract boolean onCastlingCandidateFound(int rank, int kingFile, int rookFile, boolean kingside);

    /**
     * Returns the square to add to the candidate list when castling in the given direction
     * is allowed.
     *
     * <p>Standard implementation returns the fixed target square (g-file for kingside,
     * c-file for queenside). Chess960 implementation returns the rook's current square.
     *
     * @param direction {@code +1} for kingside, {@code -1} for queenside
     * @return the castling target square, or {@code null} if none
     */
    protected abstract Square getCastlingTargetSquare(int direction);
}
