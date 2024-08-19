package ptp.core.logic.ruleset.possibleMoves;

import ptp.core.data.Square;
import ptp.core.data.board.Board;
import ptp.core.data.player.Player;

import java.util.ArrayList;
import java.util.List;

public class PossibleStandardRookMoves {

    private final Square square;
    private final Board board;

    /**
     * Initiates a new instance of a standard rook move lookup table
     *
     * @param square The square the rook is on
     * @param board  The board surrounding the rook
     */
    public PossibleStandardRookMoves(Square square, Board board) {
        this.square = square;
        this.board = board;
    }

    /**
     * Generates a list of possible Squares the rook can move to
     *
     * @return List of potentially possible moves
     */
    public List<Square> getPossibleSquares() {
        List<Square> possibleMoves = new ArrayList<>();
        Player owner = square.isOccupiedBy();
        Square possibleSquare;

        //moves up
        for (int y = square.getY() + 1; y <= board.getColCount() - 1; y++) {
            possibleSquare = board.getSquare(y, square.getX());
            if (possibleSquare.isEmpty()) {
                possibleMoves.add(possibleSquare);
            } else if (isSquareSelfCapture(possibleSquare, board, owner)) {
                break;
            } else {
                possibleMoves.add(possibleSquare);
                break;
            }
        }

        //moves left
        for (int x = square.getX() - 1; x >= 0; x--) {
            possibleSquare = board.getSquare(square.getY(), x);
            if (possibleSquare.isEmpty()) {
                possibleMoves.add(possibleSquare);
            } else if (isSquareSelfCapture(possibleSquare, board, owner)) {
                break;
            } else {
                possibleMoves.add(possibleSquare);
                break;
            }
        }

        //moves down
        for (int y = square.getY() - 1; y >= 0; y--) {
            possibleSquare = board.getSquare(y, square.getX());
            if (possibleSquare.isEmpty()) {
                possibleMoves.add(possibleSquare);
            } else if (isSquareSelfCapture(possibleSquare, board, owner)) {
                break;
            } else {
                possibleMoves.add(possibleSquare);
                break;
            }
        }

        //moves right
        for (int x = square.getX() + 1; x <= board.getRowCount() - 1; x++) {
            possibleSquare = board.getSquare(square.getY(), x);
            if (possibleSquare.isEmpty()) {
                possibleMoves.add(possibleSquare);
            } else if (isSquareSelfCapture(possibleSquare, board, owner)) {
                break;
            } else {
                possibleMoves.add(possibleSquare);
                break;
            }
        }

        return possibleMoves;
    }

    /**
     * Checks if square is legal to move to
     *
     * @param square Square to check
     * @param board  Board to check on
     * @param owner  owner of the piece
     * @return ?isLegal
     */
    private boolean isSquareSelfCapture(Square square, Board board, Player owner) {
        if (square.isEmpty()) {
            return false;
        }
        return square.isOccupiedBy().equals(owner);
    }
}
