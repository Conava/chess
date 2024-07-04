package ptp.project.logic.game;

import ptp.project.data.Player;
import ptp.project.data.Square;
import ptp.project.data.enums.RulesetOptions;
import ptp.project.exceptions.IllegalMoveException;
import ptp.project.logic.moves.Move;
import ptp.project.data.pieces.Piece;

import java.util.List;

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
}
