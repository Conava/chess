package ptp.project.logic.gameTemp;

import ptp.project.logic.PlayerTemp;
import ptp.project.logic.RulesetTemp;
import ptp.project.logic.SquareTemp;
import ptp.project.logic.piecesTemp.PieceTemp;

import ptp.project.exceptions.IllegalMoveException;
import ptp.project.logic.movesTemp.MoveTemp;

import java.util.List;

public interface GameTemp {

    void start();

    PlayerTemp getCurrentPlayer();

    PlayerTemp getPlayerWhite();

    PlayerTemp getPlayerBlack();

    RulesetTemp getRuleset();

    PieceTemp getPieceAt(SquareTemp notation);

    List<SquareTemp> getLegalSquares(SquareTemp position);

    List<MoveTemp> getMoveList();

    void movePiece(SquareTemp squareTempStart, SquareTemp squareTempEnd) throws IllegalMoveException;
}
