package ptp.project.logic.game;

import ptp.project.data.Player;
import ptp.project.data.Square;
import ptp.project.data.enums.RulesetOptions;
import ptp.project.exceptions.IllegalMoveException;
import ptp.project.logic.moves.Move;
import ptp.project.data.pieces.Piece;

import java.util.List;

public class OnlineGame extends Game {
    Player localPlayer;

    public OnlineGame(RulesetOptions selectedRuleset) {
        super(selectedRuleset);
        localPlayer = player0;        //remotePlayer is always player1
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
