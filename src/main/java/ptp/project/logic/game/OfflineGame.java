package ptp.project.logic.game;

import ptp.project.data.Player;
import ptp.project.data.Square;
import ptp.project.data.enums.GameState;
import ptp.project.data.enums.RulesetOptions;
import ptp.project.exceptions.IllegalMoveException;
import ptp.project.logic.moves.Move;

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
    public void movePiece(Square squareStart, Square squareEnd) throws IllegalMoveException {
        Square squareStartBoard = toBoardSquare(squareStart);
        Square squareEndBoard = toBoardSquare(squareEnd);

        Player player = this.getCurrentPlayer();
        Player startSquarePlayer = squareStartBoard.isOccupiedBy();
        Move move =  new Move(squareStartBoard, squareEndBoard);

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

    private Square toBoardSquare(Square square) {
        return board.getSquare(square.getY(), square.getX());
    }
}