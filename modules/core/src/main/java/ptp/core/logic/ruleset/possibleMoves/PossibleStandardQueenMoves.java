package ptp.core.logic.ruleset.possibleMoves;

import ptp.core.data.Square;
import ptp.core.data.board.Board;

import java.util.List;

public class PossibleStandardQueenMoves {

    private final Square square;
    private final Board board;

    /**
     * Initiates a new instance of a standard queen move lookup table
     *
     * @param square The square the queen is on
     * @param board  The board surrounding the queen
     */
    public PossibleStandardQueenMoves(Square square, Board board) {
        this.square = square;
        this.board = board;
    }

    /**
     * Generates a list of possible Squares the queen can move to
     *
     * @return List of potentially possible moves
     */
    public List<Square> getLegalSquares() {
        PossibleStandardRookMoves rookMoves = new PossibleStandardRookMoves(square, board);
        PossibleStandardBishopMoves bishopMoves = new PossibleStandardBishopMoves(square, board);

        List<Square> possibleMoves = rookMoves.getPossibleSquares();
        possibleMoves.addAll(bishopMoves.getPossibleSquares());

        return possibleMoves;
    }
}
