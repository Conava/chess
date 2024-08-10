package ptp.components;

import ptp.core.data.Square;
import ptp.core.data.pieces.Piece;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class BoardButton extends JButton {
    private final int row;
    private final int col;
    private final ColorScheme colorScheme;
    private ImageIcon originalPieceIcon = null;


    public BoardButton(int row, int col, ColorScheme colorScheme) {
        this.row = row;
        this.col = col;
        this.colorScheme = colorScheme;
        setBackgroundColor();
        this.setFocusable(false);
    }

    private void setBackgroundColor() {
        Color backgroundColor = (row + col) % 2 == 0 ? colorScheme.getBrighterBackgroundColor() : colorScheme.getDarkerBackgroundColor();
        this.setBackground(backgroundColor);
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public Square getSquare() {
        return new Square(row, col);
    }

    private ImageIcon createDotIcon(Color dotColor, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Enable antialiasing for smoother circle
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Set transparent background
        g2d.setColor(new Color(0, 0, 0, 0));
        g2d.fillRect(0, 0, width, height);

        // Draw the colored dot in the center
        g2d.setColor(dotColor);
        int diameter = Math.min(width, height) / 2;
        g2d.fillOval((width - diameter) / 2, (height - diameter) / 2, diameter, diameter);

        g2d.dispose();
        return new ImageIcon(image);
    }

    public void setMarker() {
        // Store the current icon as the original if not already stored
        if (originalPieceIcon == null) {
            originalPieceIcon = (ImageIcon) this.getIcon();
        }

        ImageIcon dotIcon = createDotIcon(colorScheme.getBoardDotColor(), 100, 100);
        if (originalPieceIcon != null) {
            // Combine original icon with the dot marker
            this.setIcon(combineIcons(originalPieceIcon, dotIcon));
        } else {
            // Just set the dot marker if there's no original icon
            this.setIcon(dotIcon);
        }
    }

    private ImageIcon combineIcons(ImageIcon originalIcon, ImageIcon dotIcon) {
        int width = originalIcon.getIconWidth();
        int height = originalIcon.getIconHeight();

        BufferedImage combinedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = combinedImage.createGraphics();

        // Calculate the center of the original icon
        int centerX = width / 2;
        int centerY = height / 2;

        // Calculate the top-left corner for the dot to be centered
        int dotX = centerX - (dotIcon.getIconWidth() / 2);
        int dotY = centerY - (dotIcon.getIconHeight() / 2);

        // Draw the dot icon centered
        g2.drawImage(dotIcon.getImage(), dotX, dotY, null);

        // Overlay the original piece icon
        g2.drawImage(originalIcon.getImage(), 0, 0, null);

        g2.dispose();

        return new ImageIcon(combinedImage);
    }

    // Modified removeMarker method
    public void removeMarker() {
        if (originalPieceIcon != null) {
            this.setIcon(originalPieceIcon);
            originalPieceIcon = null; // Reset after reverting
        }
        else {
            this.setIcon(null);
        }
    }

    public void setPieceIcon(Piece piece) {
        if (piece == null) {
            originalPieceIcon = null;
            this.setIcon(null);
            return;
        }
        originalPieceIcon = piece.getIcon();
        this.setIcon(originalPieceIcon);
    }
}
