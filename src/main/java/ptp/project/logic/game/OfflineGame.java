package ptp.project.logic.game;

import ptp.project.data.Player;
import ptp.project.data.Square;
import ptp.project.data.enums.GameState;
import ptp.project.data.enums.Pieces;
import ptp.project.data.enums.RulesetOptions;
import ptp.project.data.pieces.Piece;
import ptp.project.exceptions.IllegalMoveException;
import ptp.project.logic.moves.Move;
import ptp.project.logic.moves.PromotionMove;

public class OfflineGame extends Game {

    public OfflineGame(RulesetOptions selectedRuleset, String playerWhiteName, String playerBlackName) {
        super(selectedRuleset, playerWhiteName, playerBlackName);
    }

    @Override
    public void startGame() {
        gameState = GameState.RUNNING;
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