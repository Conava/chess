package ptp.server.management;

import ptp.core.data.Player;
import ptp.core.data.Square;
import ptp.core.data.enums.GameState;
import ptp.core.data.enums.RulesetOptions;
import ptp.core.exceptions.IllegalMoveException;
import ptp.core.logic.moves.Move;
import ptp.core.logic.game.Game;

/**
 * The GameInstance class represents an instance of a game.
 * It manages the connection state of players and initializes the game state based on the provided ruleset.
 */
public class GameInstance extends Game {
    private boolean isWhitePlayerConnected = false;
    private boolean isBlackPlayerConnected = false;

    /**
     * Constructs a GameInstance with the specified ruleset.
     *
     * @param ruleset The ruleset options for the game.
     */
    public GameInstance(RulesetOptions ruleset) {
        super(ruleset, "", "");
    }

    /**
     * Connects a player to the game. The first player to connect is assigned as white,
     * and the second player is assigned as black.
     *
     * @return 1 if the player is connected as white, 2 if the player is connected as black, 0 if both players are already connected.
     */
    public synchronized int connectPlayer() {
        if (!isWhitePlayerConnected) {
            isWhitePlayerConnected = true;
            return 1;
        } else if (!isBlackPlayerConnected) {
            isBlackPlayerConnected = true;
            return 2;
        }
        return 0;
    }

    @Override
    public void startGame() {
        gameState = GameState.RUNNING;
    }

    @Override
    public void endGame() {

    }

    public synchronized boolean submitMove(Move move) {
        try {
            processMove(move);
            return true;
        } catch (IllegalMoveException e) {
            return false;
        }
    }

    @Override
    protected void executeMove(Square squareStart, Square squareEnd, Move move) throws IllegalMoveException {
        Square squareStartBoard = toBoardSquare(squareStart);
        Square squareEndBoard = toBoardSquare(squareEnd);

        Player player = this.getCurrentPlayer();
        Player startSquarePlayer = squareStartBoard.isOccupiedBy();

        if (ruleset.isValidSquare(squareStart)) {
            if (squareStartBoard.isOccupiedBy() != null && startSquarePlayer == player) {
                if (this.getLegalSquares(squareStartBoard).contains(squareEndBoard)) {
                    board.executeMove(move);
                    moves.add(move);
                    turnCount++;
                    return;
                }
            }
        }
        throw new IllegalMoveException(move);
    }

    protected void processMove(Move move) throws IllegalMoveException {
        Square squareStart = move.getStart();
        Square squareEnd = move.getEnd();
        executeMove(squareStart, squareEnd, move);
    }
}
