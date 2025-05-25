package io.github.conava.chess.core.data.pieces;

import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;

public class Knight extends Piece {
    public Knight(Player player) {
        super(player);
        if (player.color().equals(PlayerColor.WHITE)) {
            iconPath = "/icon/knight_white.png";
        } else {
            iconPath = "/icon/knight_black.png";
        }
    }
}