package ptp.project.logic.gameTemp;

import ptp.project.exceptions.IllegalMoveException;
import ptp.project.logic.*;
import ptp.project.logic.game.Observable;
import ptp.project.logic.movesTemp.MoveTemp;
import ptp.project.logic.piecesTemp.PieceTemp;

import java.util.List;

public class OnlineGameTemp extends Observable implements GameTemp {
    @Override
    public void start() {

    }

    @Override
    public PlayerTemp getCurrentPlayer() {
        return null;
    }

    @Override
    public PlayerTemp getPlayerWhite() {
        return null;
    }

    @Override
    public PlayerTemp getPlayerBlack() {
        return null;
    }

    @Override
    public RulesetTemp getRuleset() {
        return null;
    }

    @Override
    public PieceTemp getPieceAt(SquareTemp notation) {
        return null;
    }

    @Override
    public List<SquareTemp> getLegalSquares(SquareTemp position) {
        return List.of();
    }

    @Override
    public void movePiece(SquareTemp squareTempStart, SquareTemp squareTempEnd) throws IllegalMoveException {
    }

    @Override
    public List<MoveTemp> getMoveList() {
        return null;
    }
}
