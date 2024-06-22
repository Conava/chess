package ptp.project.logic.piecesTemp;

import ptp.project.logic.*;
import ptp.project.logic.movesTemp.MoveTemp;

import java.util.List;

public class Pawn extends PieceTemp {
    public Pawn(PlayerTemp playerTemp) {
        super(playerTemp);
    }

    public boolean hasMoveJustMovedTwoSquares(List<MoveTemp> list) {
        if (list.get(list.size()-1).getStart().getPiece() instanceof Pawn) {
            int distance = (list.get(list.size()-1).getStart().getX() - list.get(list.size()-1).getEnd().getX());
            return (distance * distance) == 4;
        }
        return false;
    }
}