package ptp.core.logic.game;

import ptp.core.data.player.Player;
import ptp.core.data.Square;
import ptp.core.logic.ruleset.RulesetOptions;
import ptp.core.exceptions.IllegalMoveException;
import ptp.core.logic.moves.Move;

public class OfflineGame extends Game {

    public OfflineGame(RulesetOptions selectedRuleset, String playerWhiteName, String playerBlackName) {
        super(selectedRuleset, playerWhiteName, playerBlackName);
    }

    @Override
    public void endGame() {
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
}