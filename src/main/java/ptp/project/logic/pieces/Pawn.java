package ptp.project.logic.pieces;

import ptp.project.logic.*;
import ptp.project.logic.moves.Move;

import java.util.List;

public class Pawn extends Piece {
    public Pawn(Player player) {
        super(player);
    }

    public boolean hasMoveJustMovedTwoSquares(List<Move> list) {
        return false;
    }
}