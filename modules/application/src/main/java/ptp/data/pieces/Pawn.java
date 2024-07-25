package ptp.data.pieces;

import ptp.data.Player;
import ptp.data.enums.PlayerColor;
import ptp.logic.moves.Move;

import java.util.List;

public class Pawn extends Piece {
    public Pawn(Player player) {
        super(player);
        if (player.color().equals(PlayerColor.WHITE)) {
            iconPath = "/icon/pawn_white.png";
        } else {
            iconPath = "/icon/pawn_black.png";
        }
    }

    public boolean hasMoveJustMovedTwoSquares(List<Move> list) {
        if (list.get(list.size()-1).getStart().getPiece() instanceof Pawn) {
            int distance = (list.get(list.size()-1).getStart().getX() - list.get(list.size()-1).getEnd().getX());
            return (distance * distance) == 4;
        }
        return false;
    }
}