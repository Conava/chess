package ptp.gui.game;

import ptp.core.data.Square;
import ptp.core.data.enums.RulesetOptions;
import ptp.core.exceptions.IllegalMoveException;
import ptp.core.logic.game.Game;
import ptp.core.logic.moves.Move;

public class OnlineGame extends Game {

    public OnlineGame(RulesetOptions selectedRuleset, String playerWhiteName, String playerBlackName) {
        super(selectedRuleset, playerWhiteName, playerBlackName);
    }

    @Override
    public void startGame() {
    }

    @Override
    public void endGame() {
    }

    @Override
    public void movePiece(Square squareStart, Square squareEnd) throws IllegalMoveException {
    }

    @Override
    protected void executeMove(Square squareStart, Square squareEnd, Move move) throws IllegalMoveException {
    }
}
