package ptp.project.window;

import ptp.project.Chess;
import ptp.project.exceptions.IllegalMoveException;
import ptp.project.logic.PlayerTemp;
import ptp.project.logic.SquareTemp;
import ptp.project.logic.game.GameObserver;
import ptp.project.window.components.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private SquareTemp clickedSquareTemp;

    public ChessGame(MainFrame mainFrame, Chess chess, ColorScheme colorScheme, int online) {
        this.mainFrame = mainFrame;
        this.chess = chess;
        this.colorScheme = colorScheme;
        this.setBackground(colorScheme.getBackgroundColor());
        if (online == 1) {
            LOGGER.log(Level.INFO, "Online game initiated");
            chess.createOnlineGame();
        } else {
            LOGGER.log(Level.INFO, "Local game initiated");
            chess.createOfflineGame();
        }
        initializeGUI();
        LOGGER.log(Level.INFO, "GameWindow GUI initialized");
        initializeGame();
        LOGGER.log(Level.INFO, "Game started");

    }

    private void initializeGUI() {
        this.setLayout(new BorderLayout());
        // Create the top panel
        topPanel = new TopPanel(colorScheme, mainFrame);
        topPanel.setPreferredSize(new Dimension(this.getWidth(), 50));

        // Create the bottom panel
        bottomPanel = new BottomPanel(colorScheme, mainFrame);
        bottomPanel.setPreferredSize(new Dimension(this.getWidth(), 50));

        //Create chess board panel
        boardPanel = new BoardPanel(colorScheme, this);
        boardPanel.setPreferredSize(new Dimension(800, 800));

        // Create the side panel
        sidePanelRight = new SidePanel(colorScheme, mainFrame);
        sidePanelRight.setPreferredSize(new Dimension(300, sidePanelRight.getPreferredSize().height));

        sidePanelLeft = new SidePanel(colorScheme, mainFrame);
        sidePanelLeft.setPreferredSize(new Dimension(300, sidePanelLeft.getPreferredSize().height));

        // Add the panels to the frame
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

    private void adjustPanelSizes() {
        int windowHeight = this.getHeight();
        int topBottomPanelHeight = 50;
        int boardPanelHeight = windowHeight - topBottomPanelHeight;

        // Set the size of the board panel
        boardPanel.setPreferredSize(new Dimension(boardPanelHeight, boardPanelHeight));

        // Set the size of the side panel
        int remainingWidth = this.getWidth() - boardPanelHeight;
        sidePanelRight.setPreferredSize(new Dimension(remainingWidth/2, this.getHeight()));
        sidePanelLeft.setPreferredSize(new Dimension(remainingWidth/2, this.getHeight()));

        // Revalidate and repaint the panel to apply the changes
        this.revalidate();
        this.repaint();
    }

    private void initializeGame() {
        chess.startGame();
        topPanel.setBlack(chess.getPlayerWhite().getName());
        bottomPanel.setPlayer2Name(chess.getPlayerBlack().getName());
        updateActivePlayerHighlight(chess.getCurrentPlayer());
    }

    public void update() {
        checkStatus();
        updateActivePlayerHighlight(chess.getCurrentPlayer());
        updateBoard();
        updateList();
    }

    /**
     * Method for testing only
     */
    public void demo() {
        updateActivePlayerHighlight(chess.getPlayerBlack());
    }

    private void updateActivePlayerHighlight(PlayerTemp playerTemp) {
        if (playerTemp == chess.getPlayerWhite()) {
            topPanel.setBlack(true);
            bottomPanel.setPlayer2(false);
        } else {
            topPanel.setBlack(false);
            bottomPanel.setPlayer2(true);
        }
    }

    private void updateBoard() {
    }

    private void updateList() {
        sidePanelRight.updateList();
    }

    private void checkStatus() {
    }

    public void clickedOn(SquareTemp squareTemp) {
        if (clickedSquareTemp == null) {
            setClickedSquare(squareTemp);
            boardPanel.setLegalSquares(chess.getLegalSquares(squareTemp));
        } else {
            if (chess.getLegalSquares(clickedSquareTemp).contains(squareTemp)) {
                try {
                    chess.movePiece(clickedSquareTemp, squareTemp);
                    setClickedSquare(null);
                    boardPanel.setLegalSquares(null);
                } catch (IllegalMoveException e) {
                    LOGGER.log(Level.WARNING, "Illegal move");
                }

            } else {
                if (chess.getPieceAt(squareTemp) != null) {
                    if (chess.getPieceAt(squareTemp).getPlayer() == chess.getCurrentPlayer()) {
                        setClickedSquare(squareTemp);
                        boardPanel.setLegalSquares(chess.getLegalSquares(squareTemp));
                    }
                }
            }
        }
        if (chess.getPieceAt(squareTemp) != null) {
            if (chess.getPieceAt(squareTemp).getPlayer() == chess.getCurrentPlayer()) {
                setClickedSquare(squareTemp);
                boardPanel.setLegalSquares(chess.getLegalSquares(squareTemp));
            }
        }
    }

    private void setClickedSquare(SquareTemp squareTemp) {
        clickedSquareTemp = squareTemp;
    }
}