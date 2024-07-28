package ptp.core;

import ptp.core.data.board.Board;
import ptp.core.data.enums.GameState;
import ptp.core.data.enums.Pieces;
import ptp.core.data.enums.RulesetOptions;
import ptp.core.exceptions.IllegalMoveException;
import ptp.core.data.Player;
import ptp.core.data.Square;
import ptp.core.logic.game.Game;
import ptp.core.logic.game.GameObserver;
import ptp.core.logic.game.OfflineGame;
import ptp.core.logic.game.OnlineGame;
import ptp.core.data.pieces.Piece;

import java.util.List;
import java.util.logging.*;

public class ChessGame {
    private static final Logger LOGGER = Logger.getLogger(ChessGame.class.getName());
    private Game game;

    public void startGame(int online, RulesetOptions selectedRuleset, String playerWhiteName, String playerBlackName) {
        if (game == null) {
            game = online == 1 ? new OnlineGame(selectedRuleset, playerWhiteName, playerBlackName) : new OfflineGame(selectedRuleset, playerWhiteName, playerBlackName);
            game.startGame();
            LOGGER.log(Level.INFO, "Game started");
        } else {
            LOGGER.log(Level.WARNING, "Game is already running");
        }
    }

    public GameState getState() {
        return game.getState();
    }

    public Board getBoard() {
        return game.getBoard();
    }

    public void addObserver(GameObserver observer) {
        game.addObserver(observer);
    }

    public void removeObserver(GameObserver observer) {
        game.removeObserver(observer);
    }

    public void endGame() {
        game.endGame();
        game = null;
    }

    public Player getCurrentPlayer() {
        return game.getCurrentPlayer();
    }

    public Player getPlayerWhite() {
        return game.getPlayerWhite();
    }

    public Player getPlayerBlack() {
        return game.getPlayerBlack();
    }

    public Piece getPieceAt(Square position) {
        return game.getPieceAt(position);
    }

    public List<Square> getLegalSquares(Square position) {
        return game.getLegalSquares(position);
    }

    public List<String> getMoveList() {
        return game.getMoveList();
    }

    public void movePiece(Square start, Square end) throws IllegalMoveException {
        game.movePiece(start, end);
    }

    public void promoteMove(Square start, Square end, Pieces targetPiece) throws IllegalMoveException {
        game.promoteMove(start, end, targetPiece);
    }
}