package io.github.conava.chess.core.logic.ruleset.possibleMoves;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.pieces.King;
import io.github.conava.chess.core.data.pieces.Piece;
import io.github.conava.chess.core.data.pieces.Rook;
import io.github.conava.chess.core.data.player.Player;

import java.util.ArrayList;
import java.util.List;

public class PossibleStandardKingMoves {

    private final Square square;
    private final Board board;
    private final int colCount;
    private final int rowCount;

    /**
     * Initiates a new instance of a standard king move lookup table
     *
     * @param square The square the king is on
     * @param board  The board surrounding the king
     */
    public PossibleStandardKingMoves(Square square, Board board) {
        this.square = square;
        this.board = board;
        this.colCount = board.getColCount();
        this.rowCount = board.getRowCount();
    }

    /**
     * Generates a list of possible Squares the king can move to
     *
     * @return List of potentially possible moves
     */
    public List<Square> getPossibleSquares() {
        List<Square> possibleMoves = new ArrayList<>();
        Player owner = square.isOccupiedBy();
        Square possibleSquare;

        int[] arrY = {-1, 0, +1, +1, +1, 0, -1, -1};
        int[] arrX = {+1, +1, +1, 0, -1, -1, -1, 0};

        for (int i = 0; i < 8; i++) {
            if (isInBounds(square.getY() + arrY[i], square.getX() + arrX[i])) {
                possibleSquare = board.getSquare(square.getY() + arrY[i], square.getX() + arrX[i]);
                if (possibleSquare.isEmpty() || !possibleSquare.isOccupiedBy().equals(owner)) {
                    possibleMoves.add(possibleSquare);
                }
            }
        }

        if (canCastleLong()) {
            possibleMoves.add(board.getSquare(square.getY(), 2));
        }
        if (canCastleShort()) {
            possibleMoves.add(board.getSquare(square.getY(), rowCount - 2));
        }

        return possibleMoves;
    }

    /**
     * Checks if the coordinates lead to a square in bounds
     *
     * @param y Y coordinate of the target
     * @param x X coordinate of the target
     * @return ?isInBounds
     */
    private boolean isInBounds(int y, int x) {
        return y >= 0 && y < colCount && x >= 0 && x < rowCount;
    }

    /**
     * Checks if long castle (queenside) is possible.
     * The king must not have moved. There must be an unmoved rook reachable
     * by walking left from the king with no pieces between them.
     *
     * @return true if queenside castling is possible
     */
    private boolean canCastleLong() {
        if (square.getPiece() instanceof King king && king.getHasMoved()) {
            return false;
        }
        return canCastleToward(-1);
    }

    /**
     * Checks if short castle (kingside) is possible.
     * The king must not have moved. There must be an unmoved rook reachable
     * by walking right from the king with no pieces between them.
     *
     * @return true if kingside castling is possible
     */
    private boolean canCastleShort() {
        if (square.getPiece() instanceof King king && king.getHasMoved()) {
            return false;
        }
        return canCastleToward(+1);
    }

    /**
     * Walks from the king's file toward the board edge in the given direction,
     * checking whether castling is possible in that direction.
     *
     * <p>Starting one step away from the king, each square is inspected:
     * <ul>
     *   <li>Empty square: continue walking.</li>
     *   <li>Unmoved {@link Rook} owned by the same player as the king: castling is
     *       possible — return {@code true}.</li>
     *   <li>Any other piece (including a moved rook, an enemy rook, or any non-rook
     *       piece): the path is blocked — return {@code false}.</li>
     * </ul>
     * If the board edge is reached without finding a rook, {@code false} is returned.
     *
     * @param direction {@code -1} for queenside (left) or {@code +1} for kingside (right)
     * @return {@code true} if an unmoved friendly rook is found with no pieces between
     *         it and the king
     */
    private boolean canCastleToward(int direction) {
        Player owner = square.isOccupiedBy();
        int y = square.getY();
        int x = square.getX() + direction;

        while (x >= 0 && x < rowCount) {
            Piece piece = board.getSquare(y, x).getPiece();
            if (piece == null) {
                // square is empty — keep walking
            } else if (piece instanceof Rook rook
                    && rook.getHasNotMoved()
                    && piece.getPlayer().equals(owner)) {
                // found an unmoved friendly rook with a clear path — castling is allowed
                return true;
            } else {
                // path is blocked by a piece that is not an eligible rook
                return false;
            }
            x += direction;
        }
        // reached the board edge without finding a rook
        return false;
    }
}
