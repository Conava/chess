package ptp.project.logic.game;

import ptp.project.exceptions.IllegalMoveException;
import ptp.project.logic.board.Board;
import ptp.project.logic.moves.Move;
import ptp.project.logic.player.Player;
import ptp.project.logic.rulesets.Ruleset;

import java.util.ArrayList;
import java.util.List;

public class GameOffline extends Observable implements Game {
    private final Player player0;
    private final Player player1;
    private final Ruleset ruleset;
    private final Board board;
    int gameStatus;
    int turnCounter;
    List<Move> moves;

    /**
     * Initialize an offline game.
     * @param ruleset Ruleset used to play the game on.
     */
    public GameOffline(Ruleset ruleset) {
        player0 = new Player("PlayerWhite", 0);
        player1 = new Player("PlayerBlack", 1);
        this.ruleset = ruleset;
        this.board = new Board(ruleset.getWidth(), ruleset.getHeight());
        this.gameStatus = 1;
        this.turnCounter = 0;
        this.moves = new ArrayList<>();
        notifyObservers();
    }

    /**
     * Gets the status of the game.
     *
     * @return #Entries for values of status of the game: {@link ptp.project.logic.docs}
     */
    @Override
    public int getGameStatus() {
        return gameStatus;
    }

    /**
     * Starts the game.
     */
    @Override
    public void startGame() {
        //todo
    }

    /**
     * Gets all board positions.
     *
     * @return Array of board positions: #Example for how a 8x8 board is numerated:
     * #Entries of values of squares follow this pattern:{@link ptp.project.logic.docs}
     */
    @Override
    public int[] getBoard() {
        return board.getBoard();
    }

    /**
     * Gets the height of the board.
     *
     * @return The amount of rows of the board.
     */
    @Override
    public int getBoardHeight() {
        return ruleset.getHeight();
    }

    /**
     * Gets the width of the board.
     *
     * @return The amount of columns of the board.
     */
    @Override
    public int getBoardWidth() {
        return ruleset.getWidth();
    }

    /**
     * Gets the value of the desired square.
     *
     * @param row Row of the square.
     * @param col Column of the square.
     * @return Int value of the square:
     * #Entries of values of squares follow this pattern:{@link ptp.project.logic.docs}
     */
    @Override
    public int getSquare(int row, int col) {
        return getBoardWidth() * row + col;
    }

    /**
     * Gets current player.
     *
     * @return Player that can move next.
     */
    @Override
    public Player getCurrentPlayer() {
        if (turnCounter % 2 == 0) {
            return player0;
        }
        return player1;
    }

    /**
     * Get Player 0
     *
     * @return Player with the white pieces.
     */
    @Override
    public Player getPlayer0() {
        return player0;
    }

    /**
     * Get Player 1
     *
     * @return Player with the black pieces.
     */
    @Override
    public Player getPlayer1() {
        return player1;
    }

    /**
     * Change the name of the player.
     *
     * @param name   New name of the player.
     * @param player Player whose name should be changed.
     */
    @Override
    public void setPlayerName(String name, Player player) {
        player.setName(name);
        notifyObservers();
    }

    /**
     * Gets the list of all moves played so far.
     *
     * @return A list of moves, can be empty.
     */
    @Override
    public List<Move> getMoveList() {
        return moves;
    }

    /**
     * Returns possible squares that can be reached from the piece on the square.
     *
     * @param row Row of the square.
     * @param col Column of the square.
     * @return List of squares as int based on board size, can be empty.
     */
    @Override
    public List<Integer> getPossibleSquares(int row, int col) {
        return getPossibleSquares(getSquare(row, col));
    }

    /**
     * Returns possible squares that can be reached from the piece on the square.
     *
     * @param square #Example for how a 8x8 board is numerated: {@link ptp.project.logic.docs}
     * @return List of squares as int based on board size, can be empty.
     */
    @Override
    public List<Integer> getPossibleSquares(int square) {
        return ruleset.getLegalSquares(square, board, getCurrentPlayer(), moves);
    }

    /**
     * Execute a move, can fail if move is illegal.
     *
     * @param rowStart Row of the start square.
     * @param colStart Column of the start square.
     * @param rowEnd   Row of the end square.
     * @param colEnd   Column of the end square.
     * @throws IllegalMoveException If move is illegal.
     */
    @Override
    public void movePiece(int rowStart, int colStart, int rowEnd, int colEnd) throws IllegalMoveException {
        int squareStart = getSquare(rowStart, colStart);
        Move move = new Move(squareStart, getSquare(rowEnd, colEnd), board.getPiece(squareStart));
        movePiece(move);
    }

    /**
     * Execute a move, can fail if move is illegal.
     *
     * @param move The move to execute.
     * @throws IllegalMoveException If move is illegal.
     */
    @Override
    public void movePiece(Move move) throws IllegalMoveException {
        if (ruleset.isLegalMove(move, board)) {
            board.executeMove(move);
            moves.add(move);
            turnCounter++;
        } else {
            throw new IllegalMoveException(move);
        }
        notifyObservers();
    }
}
