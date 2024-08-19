package ptp.core.logic.ruleset.possibleMoves;

import ptp.core.data.Square;
import ptp.core.data.board.Board;
import ptp.core.data.player.Player;

import java.util.ArrayList;
import java.util.List;

public class PossibleStandardKnightMoves {

    private final Square square;
    private final Board board;
    private final int colCount;
    private final int rowCount;

    /**
     * Initiates a new instance of a standard knight move lookup table
     *
     * @param square The square the knight is on
     * @param board  The board surrounding the knight
     */
    public PossibleStandardKnightMoves(Square square, Board board) {
        this.square = square;
        this.board = board;
        this.colCount = board.getColCount();
        this.rowCount = board.getRowCount();
    }

    /**
     * Generates a list of possible Squares the bishop can move to
     *
     * @return List of potentially possible moves
     */
    public List<Square> getPossibleSquares() {
        List<Square> possibleMoves = new ArrayList<>();
        Player owner = square.isOccupiedBy();
        Square possibleSquare;
        /*
            -2 / +1
            -2 / -1
            -1 / +2
            -1 / -2
            +1 / +2
            +1 / -2
            +2 / +1
            +2 / -1
         */
        int[] arrY = {-2, -2, -1, -1, +1, +1, +2, +2};
        int[] arrX = {+1, -1, +2, -2, +2, -2, +1, -1};

        for (int i = 0; i < 8; i++) {
            if(isInBounds(square.getY()+ arrY[i], square.getX() + arrX[i])) {
                possibleSquare = board.getSquare(square.getY()+ arrY[i], square.getX() + arrX[i]);
                if (possibleSquare.isEmpty() || !possibleSquare.isOccupiedBy().equals(owner)) {
                    possibleMoves.add(possibleSquare);
                }
            }
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
}
