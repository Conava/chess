package ptp.window;

import ptp.data.enums.Pieces;
import ptp.data.enums.PlayerColor;
import ptp.window.components.BoardButton;
import ptp.window.components.ColorScheme;
import ptp.window.components.ControlPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;

public class PromotionWindow extends JDialog {

    private final ColorScheme colorScheme;
    private Pieces selectedPiece;

    public PromotionWindow(MainFrame mainFrame, ColorScheme colorScheme, PlayerColor playerColor) {
        super(mainFrame, "Wähle eine Figur aus", true);
        this.colorScheme = colorScheme;
        setSize(600, 240);
        setUndecorated(true);
        setLocationRelativeTo(mainFrame);
        setAlwaysOnTop(true);
        getContentPane().setBackground(colorScheme.getBackgroundColor());

        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        containerPanel.setBackground(colorScheme.getBackgroundColor());

        JLabel titleLabel = new JLabel("Umwandlung verfügbar, wähle deine Figur");
        titleLabel.setFont(colorScheme.getFont().deriveFont(colorScheme.getFont().getSize2D() + 4));
        titleLabel.setForeground(colorScheme.getFontColor());
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setBorder(new MatteBorder(0, 0, 1, 0, Color.GRAY));
        titleLabel.setBorder(new EmptyBorder(0, 0, 20, 0)); // Add 20px space below the title label
        containerPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new ControlPanel();
        JButton queenButton = createPieceButton(Pieces.QUEEN, playerColor);
        JButton rookButton = createPieceButton(Pieces.ROOK, playerColor);
        JButton bishopButton = createPieceButton(Pieces.BISHOP, playerColor);
        JButton knightButton = createPieceButton(Pieces.KNIGHT, playerColor);

        buttonPanel.setLayout(new GridLayout(1, 4));
        buttonPanel.add(queenButton);
        buttonPanel.add(rookButton);
        buttonPanel.add(bishopButton);
        buttonPanel.add(knightButton);
        containerPanel.add(buttonPanel, BorderLayout.CENTER);

        add(containerPanel);
        setVisible(true);
    }

    private JButton createPieceButton(Pieces piece, PlayerColor playerColor) {
        JButton button = new BoardButton(0, 1, colorScheme);
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
        String iconPath = "/icon/" + piece.name().toLowerCase() + "_" + color + ".png";
        ImageIcon baseIcon = new ImageIcon(Objects.requireNonNull(getClass().getResource(iconPath)));
        Image baseImage = baseIcon.getImage();
        Image scaledImage = baseImage.getScaledInstance(60, 60, Image.SCALE_SMOOTH);

        return new ImageIcon(scaledImage);
    }

    public Pieces getSelectedPiece() {
        return selectedPiece;
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            SwingUtilities.invokeLater(() -> setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 30, 30)));
        }
        super.setVisible(b);
    }
}