package io.github.conava.chess.core.data.pieces;

import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;

public class Queen extends Piece {
    public Queen(Player player) {
        super(player);
        if (player.color().equals(PlayerColor.WHITE)) {
            iconPath = "/icon/queen_white.png";
        } else {
            iconPath = "/icon/queen_black.png";
        }
    }
}