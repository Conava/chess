package ptp.project.logic.game;

import ptp.project.data.Player;
import ptp.project.data.Square;
import ptp.project.data.enums.GameState;
import ptp.project.data.enums.RulesetOptions;
import ptp.project.exceptions.IllegalMoveException;
import ptp.project.logic.moves.Move;

public class OfflineGame extends Game {

    public OfflineGame(RulesetOptions selectedRuleset) {
        super(selectedRuleset);
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
        Player player = this.getCurrentPlayer();
        Move move =  new Move(squareStart, squareEnd);

        if (ruleset.isValidSquare(squareStart)) {
            if (squareStart.isOccupiedBy() != null && squareStart.isOccupiedBy().equals(player)) {
                if (this.getLegalSquares(squareStart).contains(squareEnd)) {
                    board.executeMove(move);
                    moves.add(move);
                    turnCount++;
                    return; //update here
                }
            }
        }
        throw new IllegalMoveException(move);
    }

    private Square toBoardSquare(Square square) {
        return board.getSquare(square.getY(), square.getX());
    }
}