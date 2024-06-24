package ptp.project.logic.game;

import ptp.project.exceptions.IllegalMoveException;
import ptp.project.logic.enums.GameStates;
import ptp.project.logic.moves.Move;
import ptp.project.logic.player.Player;

import java.util.List;

public interface Game {

    /**
     * Starts the game.
     */
    void startGame();

    /**
     * Gets the status of the game.
     *
     * @return enum for GameStates.
     */
    GameStates getGameStatus();

    /**
     * Gets all board positions.
     *
     * @return Array of board positions: #Example for how a 8x8 board is numerated:
     * #Entries of values of squares follow this pattern:{@link ptp.project.logic.docs}
     */
    int[] getBoard();

    /**
     * Gets the height of the board.
     *
     * @return The amount of rows of the board.
     */
    int getBoardHeight();

    /**
     * Gets the width of the board.
     *
     * @return The amount of columns of the board.
     */
    int getBoardWidth();

    /**
     * Gets the value of the desired square.
     *
     * @param row Row of the square.
     * @param col Column of the square.
     * @return Int value of the square:
     * #Entries of values of squares follow this pattern:{@link ptp.project.logic.docs}
     */
    int getSquare(int row, int col);

    /**
     * Gets current player.
     *
     * @return Player that can move next.
     */
    Player getCurrentPlayer();

    /**
     * Get Player 0
     *
     * @return Player with the white pieces.
     */
    Player getPlayer0();

    /**
     * Get Player 1
     *
     * @return Player with the black pieces.
     */
    Player getPlayer1();

    /**
     * Change the name of the player.
     *
     * @param name   New name of the player.
     * @param player Player whose name should be changed.
     */
    void setPlayerName(String name, Player player);

    /**
     * Gets the list of all moves played so far.
     *
     * @return A list of moves, can be empty.
     */
    List<Move> getMoveList();

    /**
     * Returns possible squares that can be reached from the piece on the square.
     *
     * @param row Row of the square.
     * @param col Column of the square.
     * @return List of squares as int based on board size, can be empty.
     */
    List<Integer> getPossibleSquares(int row, int col);

    /**
     * Returns possible squares that can be reached from the piece on the square.
     *
     * @param square #Example for how a 8x8 board is numerated: {@link ptp.project.logic.docs}
     * @return List of squares as int based on board size, can be empty.
     */
    List<Integer> getPossibleSquares(int square);


    /**
     * Execute a move, can fail if move is illegal.
     *
     * @param rowStart Row of the start square.
     * @param colStart Column of the start square.
     * @param rowEnd   Row of the end square.
     * @param colEnd   Column of the end square.
     * @throws IllegalMoveException If move is illegal.
     */
    void movePiece(int rowStart, int colStart, int rowEnd, int colEnd) throws IllegalMoveException;

    /**
     * Execute a move, can fail if move is illegal.
     *
     * @param move The move to execute.
     * @throws IllegalMoveException If move is illegal.
     */
    void movePiece(Move move) throws IllegalMoveException;
}
