package io.github.conava.chess.core.data.pieces;

import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.logic.moves.Move;

import java.util.List;

public class Pawn extends Piece {
    public Pawn(Player player) {
        super(player);
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