package ptp.core.logic.ruleset.possibleMoves;

import ptp.core.data.Square;
import ptp.core.data.board.Board;
import ptp.core.data.player.Player;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

public class PossibleStandardBishopMoves {

    private final Square square;
    private final Board board;
    private final int colCount;
    private final int rowCount;

    /**
     * Initiates a new instance of a standard bishop move lookup table
     *
     * @param square The square the bishop is on
     * @param board  The board surrounding the bishop
     */
    public PossibleStandardBishopMoves(Square square, Board board) {
        this.square = square;
        this.board = board;
        colCount = board.getColCount();
        rowCount = board.getRowCount();
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

        //move up right y+ x+
        for (int i = 1; i < Math.min(colCount, rowCount); i++) {
            if (isInBounds(square.getY() + i, square.getX() + i)) {
                possibleSquare = board.getSquare(square.getY() + i, square.getX() + i);
                if (possibleSquare.isEmpty()) {
                    possibleMoves.add(possibleSquare);
                } else if (isSquareSelfCapture(possibleSquare, owner)) {
                    break;
                } else {
                    possibleMoves.add(possibleSquare);
                    break;
                }
            } else {
                break;
            }
        }

        //move down right y- x+
        for (int i = 1; i < Math.min(colCount, rowCount); i++) {
            if (isInBounds(square.getY() - i, square.getX() + i)) {
                possibleSquare = board.getSquare(square.getY() - i, square.getX() + i);
                if (possibleSquare.isEmpty()) {
                    possibleMoves.add(possibleSquare);
                } else if (isSquareSelfCapture(possibleSquare, owner)) {
                    break;
                } else {
                    possibleMoves.add(possibleSquare);
                    break;
                }
            } else {
                break;
            }
        }

        //move down left y- x-
        for (int i = 1; i < Math.min(colCount, rowCount); i++) {
            if (isInBounds(square.getY() - i, square.getX() - i)) {
                possibleSquare = board.getSquare(square.getY() - i, square.getX() - i);
                if (possibleSquare.isEmpty()) {
                    possibleMoves.add(possibleSquare);
                } else if (isSquareSelfCapture(possibleSquare, owner)) {
                    break;
                } else {
                    possibleMoves.add(possibleSquare);
                    break;
                }
            } else {
                break;
            }
        }

        //move up left y+ x-
        for (int i = 1; i < Math.min(colCount, rowCount); i++) {
            if (isInBounds(square.getY() + i, square.getX() - i)) {
                possibleSquare = board.getSquare(square.getY() + i, square.getX() - i);
                if (possibleSquare.isEmpty()) {
                    possibleMoves.add(possibleSquare);
                } else if (isSquareSelfCapture(possibleSquare, owner)) {
                    break;
                } else {
                    possibleMoves.add(possibleSquare);
                    break;
                }
            } else {
                break;
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

    /**
     * Checks if square is legal to move to
     *
     * @param square Square to check
     * @param owner  owner of the piece
     * @return ?isLegal
     */
    private boolean isSquareSelfCapture(Square square, Player owner) {
        if (square.isEmpty()) {
            return false;
        }
        return square.isOccupiedBy().equals(owner);
    }
}

