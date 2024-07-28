package ptp.core.logic.ruleset;

import ptp.core.data.Square;
import ptp.core.data.board.Board;
import ptp.core.data.Player;
import ptp.core.data.enums.GameState;
import ptp.core.logic.moves.Move;

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

    /**
     * Returns the current GameState, should be called after every move.
     * @param board The current board
     * @param moves The list of all prior moves.
     * @return ENUM of GameState
     */
    GameState getGameState(Board board, List<Move> moves);
}