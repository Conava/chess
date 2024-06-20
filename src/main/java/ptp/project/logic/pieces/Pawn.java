package ptp.project.logic.pieces;

import ptp.project.logic.*;
import ptp.project.logic.moves.Move;

import java.util.List;

public class Pawn extends Piece {
    public Pawn(Player player) {
        super(player);
    }

    public boolean hasMoveJustMovedTwoSquares(List<Move> list) {
        if (list.get(list.size()-1).getStart().getPiece() instanceof Pawn) {
            int distance = (list.get(list.size()-1).getStart().getX() - list.get(list.size()-1).getEnd().getX());
            return (distance * distance) == 4;
        }
        return false;
    }
}