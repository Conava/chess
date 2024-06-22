package ptp.project.logic;

import ptp.project.logic.movesTemp.MoveTemp;
import ptp.project.logic.piecesTemp.PieceTemp;

import java.util.List;

public interface RulesetTemp {

    int getWidth();

    int getHeight();

    SquareTemp[][] getStartBoard(PlayerTemp playerTemp1, PlayerTemp playerTemp2);

    List<MoveTemp> getLegalMoves(SquareTemp squareTemp, BoardTemp boardTemp, List<MoveTemp> moveTemps, PlayerTemp playerTemp1, PlayerTemp playerTemp2);

    boolean isValidSquare(SquareTemp squareTemp);

    boolean verifyMove(MoveTemp moveTemp);

    boolean verifyMove(SquareTemp newPosition, PieceTemp pieceTemp);

    MoveTemp hasEnforcedMove(PlayerTemp playerTemp); //@todo: Find good way to return the enforced move

    boolean isCheck(BoardTemp boardTemp, PlayerTemp playerTemp, List<MoveTemp> moveTemps);
}