package ptp.project.logic.rulesets;

import ptp.project.logic.board.Board;
import ptp.project.logic.moves.Move;
import ptp.project.logic.player.Player;

import java.util.List;

public interface Ruleset {

    /**
     * Gets the height of the board.
     *
     * @return Amount of rows.
     */
    int getHeight();

    /**
     * Gets the width of the board.
     *
     * @return Amount of columns.
     */
    int getWidth();

    /**
     * Gets the state of the board at the start of the game.
     *
     * @return Array of board positions: #Example for how a 8x8 board is numerated:
     * #Entries of values of squares follow this pattern:{@link ptp.project.logic.docs}
     */
    int[] getStartBoard();

    /**
     * Gets all legal Moves
     *
     * @param board The board, where the moves happen.
     * @return List of Legal moves.
     */
    List<Move> getLegalMoves(Board board);

    /**
     * Gets all legal Squares accessible from given square.
     *
     * @param square start square #Example for how a 8x8 board is numerated: {@link ptp.project.logic.docs}
     * @param board  The board, where the moves happen.
     * @param player The player to move.
     * @param moves The list of all prior moves.
     * @return #Example for how a 8x8 board is numerated: {@link ptp.project.logic.docs}
     */
    List<Integer> getLegalSquares(int square, Board board, Player player, List<Move> moves);

    /**
     * Returns if the square is valid.
     *
     * @param square #Example for how a 8x8 board is numerated: {@link ptp.project.logic.docs}
     * @return bool if the square exists.
     */
    boolean isValidSquare(int square);

    /**
     * Whether the move is legal.
     *
     * @param rowStart Row of the start square.
     * @param colStart Column of the start square.
     * @param rowEnd   Row of the end square.
     * @param colEnd   Column of the end square.
     * @param board    The board, where the moves happen.
     * @return bool if move is legal.
     */
    boolean isLegalMove(int rowStart, int colStart, int rowEnd, int colEnd, Board board);

    /**
     * Whether the move is legal.
     *
     * @param move  Move to play.
     * @param board The board, where the moves happen.
     * @return bool if move is legal.
     */
    boolean isLegalMove(Move move, Board board);
}
