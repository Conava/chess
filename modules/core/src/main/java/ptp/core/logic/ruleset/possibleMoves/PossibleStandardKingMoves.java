package ptp.core.logic.ruleset.possibleMoves;

import ptp.core.data.Square;
import ptp.core.data.board.Board;
import ptp.core.data.pieces.King;
import ptp.core.data.pieces.Rook;
import ptp.core.data.player.Player;

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
     * Checks if long castle is possible
     *
     * @return ?isPossible
     */
    private boolean canCastleLong() {
        if (square.getPiece() instanceof King king && king.getHasMoved()) {
            return false;
        } else return getRookLeft() != null && getRookLeft().getHasNotMoved() && getLeftEmpty();
    }

    /**
     * Checks fi short castle is possible
     *
     * @return ?isPossible
     */
    private boolean canCastleShort() {
        if (square.getPiece() instanceof King king && king.getHasMoved()) {
            return false;
        } else return getRookRight() != null && getRookRight().getHasNotMoved() && getRightEmpty();
    }

    /**
     * Finds a rook on the left is eligible for castling
     *
     * @return Rook that has not moved, null if none
     */
    private Rook getRookLeft() {
        for (int x = 0; x <= rowCount / 2; x++) {
            if (board.getSquare(square.getY(), x).getPiece() instanceof Rook rook && rook.getHasNotMoved()) {
                return rook;
            }
        }
        return null;
    }

    /**
     * Finds a rook on the right is eligible for castling
     *
     * @return Rook that has not moved, null if none
     */
    private Rook getRookRight() {
        for (int x = rowCount - 1; x >= rowCount / 2 + 1; x--) {
            if (board.getSquare(square.getY(), x).getPiece() instanceof Rook rook && rook.getHasNotMoved()) {
                return rook;
            }
        }
        return null;
    }

    /**
     * Finds if there are pieces blocking potential castling on the left
     *
     * @return Boolean if there are pieces blocking castling
     */
    private boolean getLeftEmpty() {
        for (int x = 0; x <= rowCount / 2; x++) {
            if (!(board.getSquare(square.getY(), x).getPiece() instanceof Rook rook && rook.getHasNotMoved()) ||
                    !board.getSquare(square.getY(), x).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds if there are pieces blocking potential castling on the right
     *
     * @return Boolean if there are pieces blocking castling
     */
    private boolean getRightEmpty() {
        for (int x = rowCount - 1; x >= rowCount / 2 + 1; x--) {
            if ((board.getSquare(square.getY(), x).getPiece() instanceof Rook rook && rook.getHasNotMoved()) ||
                    !board.getSquare(square.getY(), x).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}

