package io.github.conava.chess.core.data.pieces;

import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;

public class Bishop extends Piece {
    public Bishop(Player player) {
        super(player);
        if (player.color().equals(PlayerColor.WHITE)) {
            iconPath = "/icon/bishop_white.png";
        } else {
            iconPath = "/icon/bishop_black.png";
        }
    }
}