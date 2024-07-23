package ptp.project.window;

import com.sun.tools.javac.Main;
import ptp.project.data.enums.Pieces;
import ptp.project.data.enums.PlayerColor;
import ptp.project.window.components.ColorScheme;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class PromotionWindow extends JDialog {

    private ColorScheme colorScheme;
    private Pieces selectedPiece;

    public PromotionWindow(MainFrame mainFrame, ColorScheme colorScheme, PlayerColor playerColor) {
        super(mainFrame, "Wähle eine Figur aus", true);
        this.colorScheme = colorScheme;
        setSize(400, 100);
        setLayout(new GridLayout(1, 4));
        setUndecorated(true);
        setLocationRelativeTo(mainFrame);
        setAlwaysOnTop(true);

        JButton queenButton = createPieceButton("Queen", Pieces.QUEEN, playerColor);
        JButton rookButton = createPieceButton("Rook", Pieces.ROOK, playerColor);
        JButton bishopButton = createPieceButton("Bishop", Pieces.BISHOP, playerColor);
        JButton knightButton = createPieceButton("Knight", Pieces.KNIGHT, playerColor);

        add(queenButton);
        add(rookButton);
        add(bishopButton);
        add(knightButton);

        setVisible(true);
    }

    private JButton createPieceButton(String label, Pieces piece, PlayerColor playerColor) {
        JButton button = new JButton(label);
        button.setIcon(loadPieceIcon(piece, playerColor));
        button.addActionListener(e -> {
            selectedPiece = piece;
            setVisible(false);
            dispose();
        });
        return button;
    }

    private Icon loadPieceIcon(Pieces piece, PlayerColor playerColor) {
        String color = playerColor == PlayerColor.WHITE ? "white" : "black";
        String iconPath = "/icon/" + piece.name().toLowerCase() + "_" + color +  ".png";
        return new ImageIcon(Objects.requireNonNull(getClass().getResource(iconPath)));
    }

    public Pieces getSelectedPiece() {
        return selectedPiece;
    }
}
