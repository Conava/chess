package ptp.project.logic.moves;

import ptp.project.logic.Square;
import ptp.project.logic.pieces.Piece;

public class PromotionMove extends Move {
    public final Piece targetPiece;
    public PromotionMove(Square start, Square end, Piece targetPiece) {
        super(start, end);
        this.targetPiece = targetPiece;
    }

    public Piece getTargetPiece() {
        return targetPiece;
    }
}
