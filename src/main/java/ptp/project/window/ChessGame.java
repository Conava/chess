package ptp.project.window;

import ptp.project.Chess;
import ptp.project.exceptions.IllegalMoveException;
import ptp.project.logic.Player;
import ptp.project.logic.Square;
import ptp.project.logic.game.GameObserver;
import ptp.project.window.components.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ChessGame is a JPanel that represents the game window.
 * It implements the GameObserver interface to observe changes in the game.
 */
public class ChessGame extends JPanel implements GameObserver {
    private static final Logger LOGGER = Logger.getLogger(ChessGame.class.getName());
    private final MainFrame mainFrame;
    private final Chess chess;
    private final ColorScheme colorScheme;

    private TopPanel topPanel;
    private BottomPanel bottomPanel;
    private BoardPanel boardPanel;
    private SidePanel sidePanelRight;
    private SidePanel sidePanelLeft;

    private Square clickedSquare;

    /**
     * Constructor for ChessGame.
     * Initializes the game window with the given mainFrame, chess game, colorScheme, and online status.
     *
     * @param mainFrame   The main frame of the application
     * @param chess       The chess game
     * @param colorScheme The color scheme
     * @param online      The online status of the game. 0 for offline, 1 for online.
     */
    public ChessGame(MainFrame mainFrame, Chess chess, ColorScheme colorScheme, int online) {
        this.mainFrame = mainFrame;
        this.chess = chess;
        this.colorScheme = colorScheme;
        this.setBackground(colorScheme.getBackgroundColor());

        initializeGUI();
        LOGGER.log(Level.INFO, "GameWindow GUI initialized");
        initializeGame(online);
        LOGGER.log(Level.INFO, "Game started");

    }

    /**
     * Initializes the GUI of the game window.
     */
    private void initializeGUI() {
        this.setLayout(new BorderLayout());
        topPanel = initializeTopPanel();
        bottomPanel = initializeBottomPanel();
        boardPanel = initializeBoardPanel();
        sidePanelRight = initializeSidePanel();
        sidePanelLeft = initializeSidePanel();

        this.add(topPanel, BorderLayout.NORTH);
        this.add(bottomPanel, BorderLayout.SOUTH);
        this.add(sidePanelRight, BorderLayout.EAST);
        this.add(sidePanelLeft, BorderLayout.WEST);
        this.add(boardPanel, BorderLayout.CENTER);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                adjustPanelSizes();
            }
        });
    }

    /**
     * Initializes the top panel.
     *
     * @return The initialized top panel
     */
    private TopPanel initializeTopPanel() {
        TopPanel topPanel = new TopPanel(colorScheme, mainFrame);
        topPanel.setPreferredSize(new Dimension(this.getWidth(), 50));
        return topPanel;
    }

    /**
     * Initializes the bottom panel.
     *
     * @return The initialized bottom panel
     */
    private BottomPanel initializeBottomPanel() {
        BottomPanel bottomPanel = new BottomPanel(colorScheme, mainFrame);
        bottomPanel.setPreferredSize(new Dimension(this.getWidth(), 50));
        return bottomPanel;
    }

    /**
     * Initializes the board panel.
     *
     * @return The initialized board panel
     */
    private BoardPanel initializeBoardPanel() {
        BoardPanel boardPanel = new BoardPanel(colorScheme, this);
        boardPanel.setPreferredSize(new Dimension(800, 800));
        return boardPanel;
    }

    /**
     * Initializes the side panel.
     *
     * @return The initialized side panel
     */
    private SidePanel initializeSidePanel() {
        SidePanel sidePanel = new SidePanel(colorScheme, mainFrame);
        sidePanel.setPreferredSize(new Dimension(300, sidePanel.getPreferredSize().height));
        return sidePanel;
    }

    /**
     * Initializes the game.
     *
     * @param online The online status of the game. 0 for offline, 1 for online.
     */
    private void initializeGame(int online) {
        chess.startGame(online);
        LOGGER.log(Level.INFO, "Game started");
        chess.addObserver(this);

        Optional.ofNullable(chess.getPlayerWhite())
                .ifPresent(player -> topPanel.setBlack(player.getName()));
        Optional.ofNullable(chess.getPlayerBlack())
                .ifPresent(player -> bottomPanel.setPlayer2Name(player.getName()));
        updateActivePlayerHighlight(chess.getCurrentPlayer());
    }

    /**
     * Adjusts the sizes of the panels to fit the window size.
     */
    private void adjustPanelSizes() {
        int windowHeight = this.getHeight();
        int topBottomPanelHeight = 50;
        int boardPanelHeight = windowHeight - topBottomPanelHeight;

        // Set the size of the board panel
        boardPanel.setPreferredSize(new Dimension(boardPanelHeight, boardPanelHeight));

        // Set the size of the side panel
        int remainingWidth = this.getWidth() - boardPanelHeight;
        sidePanelRight.setPreferredSize(new Dimension(remainingWidth / 2, this.getHeight()));
        sidePanelLeft.setPreferredSize(new Dimension(remainingWidth / 2, this.getHeight()));

        // Revalidate and repaint the panel to apply the changes
        this.revalidate();
        this.repaint();
    }

    @Override
    public void update() {
        int status = checkStatus();

        if (status == 0) {
            LOGGER.log(Level.WARNING, "No Game found\nSwitching to Menu");
            mainFrame.switchToMenu();
        } else if (status == 1) {
            updateActivePlayerHighlight(chess.getCurrentPlayer());
            updateBoard();
            updateList();
        } else {
            displayGameEndMessage(status);
        }
    }

    /**
     * Checks the status of the game.
     *
     * @return The status of the game as described in the Game class
     */
    private int checkStatus() {
        return chess.getStatus();
    }

    /**
     * Updates the active player highlight.
     *
     * @param player The player that is currently active
     */
    private void updateActivePlayerHighlight(Player player) {
        if (player == chess.getPlayerWhite()) {
            topPanel.setBlack(true);
            bottomPanel.setPlayer2(false);
        } else {
            topPanel.setBlack(false);
            bottomPanel.setPlayer2(true);
        }
    }

    /**
     * Updates the board UI.
     */
    private void updateBoard() {
        //todo: Show the game status on the board UI
    }

    /**
     * Updates the list of previous moves.
     */
    private void updateList() {
        sidePanelRight.updateList();
    }

    /**
     * Displays a message when the game ends.
     *
     * @param status The status of the game as described in the Game class
     */
    private void displayGameEndMessage(int status) {
        LOGGER.log(Level.INFO, "Game ended!");
        if (status < 4 && status > 1) {
            LOGGER.log(Level.INFO, chess.getPlayerWhite() + " (White) won");
            ConfirmDialog dialog = new ConfirmDialog(mainFrame, chess.getPlayerWhite() + " (Weiß) hat gewonnen!\n"
                    + (status == 2 ? "Der Gegner hat aufgegeben!" : "Schachmatt" + "\n\nZurück zum Menü?"),
                    "Game Over", colorScheme);
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                mainFrame.switchToMenu();
            }
        } else if (status < 6) {
            LOGGER.log(Level.INFO, chess.getPlayerBlack() + " (Black) won");
            ConfirmDialog dialog = new ConfirmDialog(mainFrame, chess.getPlayerBlack() + " (Schwarz) hat gewonnen!\n"
                    + (status == 4 ? "Der Gegner hat aufgegeben!" : "Schachmatt" + "\n\nZurück zum Menü?"),
                    "Game Over", colorScheme);
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                mainFrame.switchToMenu();
            }
        } else {
            LOGGER.log(Level.INFO, "Draw!");
            String reason = switch (status) {
                case 6 -> "Vereinbarung";
                case 7 -> "Unzureichendes Material";
                case 8 -> "Patt";
                case 9 -> "50 Züge Regel";
                default -> "Dreifache Stellungswiederholung";
            };
            ConfirmDialog dialog = new ConfirmDialog(mainFrame, "Unentschieden!\n" + reason + "\n\nZurück zum Menü?",
                    "Game Over", colorScheme);
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                mainFrame.switchToMenu();
            }
        }
    }

    /**
     * Handles a click on a square.
     *
     * @param square The square that was clicked
     */
    public void clickedOn(Square square) {
        if (clickedSquare == null) {
            clickedSquare = square;
            boardPanel.setLegalSquares(chess.getLegalSquares(square));
        } else {
            if (chess.getLegalSquares(clickedSquare).contains(square)) {
                try {
                    chess.movePiece(clickedSquare, square);
                    clickedSquare = null;
                    boardPanel.setLegalSquares(null);
                } catch (IllegalMoveException e) {
                    LOGGER.log(Level.WARNING, "Illegal move");
                }
            } else {
                if (chess.getPieceAt(square) != null) {
                    if (chess.getPieceAt(square).getPlayer() == chess.getCurrentPlayer()) {
                        clickedSquare = square;
                        boardPanel.setLegalSquares(chess.getLegalSquares(square));
                    }
                }
            }
        }
        if (chess.getPieceAt(square) != null) {
            if (chess.getPieceAt(square).getPlayer() == chess.getCurrentPlayer()) {
                clickedSquare = square;
                boardPanel.setLegalSquares(chess.getLegalSquares(square));
            }
        }
    }
}