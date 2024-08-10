package ptp.core.data.pieces;

import ptp.core.data.player.Player;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.Objects;

public abstract class Piece {
    protected final Player player;
    protected String iconPath;

    public Piece(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public ImageIcon getIcon() {
        URL url = getClass().getResource(iconPath);
        ImageIcon imageIcon = new ImageIcon(Objects.requireNonNull(url));

        // Scale the image
        Image image = imageIcon.getImage();
        Image scaledImage = image.getScaledInstance(60, 60, Image.SCALE_SMOOTH);

        return new ImageIcon(scaledImage);
    }

    public Pieces getType() {
        return Pieces.valueOf(this.getClass().getSimpleName().toUpperCase());
    }
}
