package ptp.project.logic.ruleset;

import ptp.project.data.Square;
import ptp.project.data.board.Board;
import ptp.project.data.Player;
import ptp.project.logic.moves.Move;

import java.util.List;

public interface Ruleset {

    /**
     * Gets the width
     *
     * @return int of the number of the columns
     */
    int getWidth();

    /**
     * Gets the height
     *
     * @return int of the amount of rows
     */
    int getHeight();

    /**
     * Returns the start position
     *
     * @param player1 Player with the white pieces
     * @param player2 Player with the black pieces
     * @return Double array of the board.
     */
    Square[][] getStartBoard(Player player1, Player player2);

    /**
     * Provides a list of legal moves.
     *
     * @param square  Only moves from this square are shown
     * @param board   Current board
     * @param moves   List of moves already played in-game
     * @param player1 Player to move
     * @param player2 Player opponent
     * @return List of LEGAL moves.
     */
    List<Move> getLegalMoves(Square square, Board board, List<Move> moves, Player player1, Player player2);

    /**
     * Provides a list of legal squares.
     *
     * @param square  Only moves from this square are shown
     * @param board   Current board
     * @param moves   List of moves already played in-game
     * @param player1 Player to move
     * @param player2 Player opponent
     * @return List of LEGAL squares.
     */
    List<Square> getLegalSquares(Square square, Board board, List<Move> moves, Player player1, Player player2);

    /**
     * Returns a valid square
     *
     * @param square Square to check
     * @return If the square exists and is in bounds
     */
    boolean isValidSquare(Square square);

    /**
     * @param board  Board, where the check should be checked
     * @param player Player who can move
     * @return ?isCheck
     */
    boolean isCheck(Board board, Player player, List<Move> moves);
}