package ptp.core.data.pieces;

import ptp.core.data.player.Player;
import ptp.core.data.player.PlayerColor;
import ptp.core.logic.moves.Move;

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
        if (list == null || list.isEmpty()) {
            return false;
        } else if (list.get(list.size() - 1).getStart().getPiece() instanceof Pawn) {
            int distance = (list.get(list.size() - 1).getStart().getY() - list.get(list.size() - 1).getEnd().getY());
            return (distance * distance) == 4;
        }
        return false;
    }
}