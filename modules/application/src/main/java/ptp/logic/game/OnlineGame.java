package ptp.logic.game;

import ptp.data.Square;
import ptp.data.enums.RulesetOptions;
import ptp.exceptions.IllegalMoveException;
import ptp.logic.moves.Move;

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
