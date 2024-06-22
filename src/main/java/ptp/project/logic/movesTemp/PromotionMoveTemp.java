package ptp.project.logic.movesTemp;

import ptp.project.logic.SquareTemp;
import ptp.project.logic.piecesTemp.PieceTemp;

public class PromotionMoveTemp extends MoveTemp {
    public final PieceTemp targetPieceTemp;
    public PromotionMoveTemp(SquareTemp start, SquareTemp end, PieceTemp targetPieceTemp) {
        super(start, end);
        this.targetPieceTemp = targetPieceTemp;
    }

    public PieceTemp getTargetPiece() {
        return targetPieceTemp;
    }
}
