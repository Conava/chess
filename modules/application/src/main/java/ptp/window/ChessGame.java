package ptp.window;

import ptp.core.data.board.Board;
import ptp.core.data.enums.GameState;
import ptp.core.data.enums.Pieces;
import ptp.core.data.enums.RulesetOptions;
import ptp.core.data.pieces.Piece;
import ptp.core.data.Player;
import ptp.core.data.Square;
import ptp.core.logic.game.GameObserver;
import ptp.core.logic.moves.Move;
import ptp.window.tasks.ExecuteMove;
import ptp.window.components.*;
import ptp.Chess;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
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
    private final ptp.window.components.ColorScheme colorScheme;

    private ptp.window.components.TopPanel topPanel;
    private ptp.window.components.BottomPanel bottomPanel;
    private ptp.window.components.BoardPanel boardPanel;
    private ptp.window.components.SidePanel sidePanelRight;
    private ptp.window.components.SidePanel sidePanelLeft;

    private Board localBoard; // Keep a local copy of the board to improve responsiveness of the UI

    private Square selectedSquare;
    private List<Square> legalSquaresForSelectedPiece;

    /**
     * Constructor for ChessGame.
     * Initializes the game window with the given mainFrame, chess game, colorScheme, and online status.
     *
     * @param mainFrame   The main frame of the application
     * @param chess       The chess game
     * @param colorScheme The color scheme
     * @param online      The online status of the game. 0 for offline, 1 for online.
     */
    public ChessGame(MainFrame mainFrame, Chess chess, ptp.window.components.ColorScheme colorScheme, int online, RulesetOptions rulesetOptions, String playerWhiteName, String playerBlackName) {
        this.mainFrame = mainFrame;
        this.chess = chess;
        this.colorScheme = colorScheme;
        this.setBackground(colorScheme.getBackgroundColor());

        initializeGUI();
        LOGGER.log(Level.INFO, "GameWindow GUI initialized");
        initializeGame(online, rulesetOptions, playerWhiteName, playerBlackName);
        LOGGER.log(Level.INFO, "Game started");
        chess.addObserver(this);
        update();
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
        sidePanelRight.activateList();
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
        BottomPanel bottomPanel = new BottomPanel(colorScheme, mainFrame, chess);
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
        SidePanel sidePanel = new SidePanel(colorScheme);
        sidePanel.setPreferredSize(new Dimension(300, sidePanel.getPreferredSize().height));
        return sidePanel;
    }

    /**
     * Initializes the game.
     *
     * @param online The online status of the game. 0 for offline, 1 for online.
     */
    private void initializeGame(int online, RulesetOptions selectedRuleset, String playerWhiteName, String playerBlackName) {
        chess.startGame(online, selectedRuleset, playerWhiteName, playerBlackName);
        LOGGER.log(Level.INFO, "Game started");
        chess.addObserver(this);

        Optional.ofNullable(chess.getPlayerBlack())
                .ifPresent(player -> topPanel.setBlackName(player.name()));
        Optional.ofNullable(chess.getPlayerWhite())
                .ifPresent(player -> bottomPanel.setWhiteName(player.name()));
        updateActivePlayerHighlight(chess.getCurrentPlayer());

        localBoard = chess.getBoard();
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
    public void updateFromRemote() {
        update(); //todo: implement the update from remote in a separate thread
    }

    /**
     * Updates the game window. This method is called from the SwingWorker ExecuteMove
     */
    public void update() {
        LOGGER.log(Level.INFO, "Update Method called");
        GameState state = getState();

        if (state == GameState.NO_GAME) {
            LOGGER.log(Level.WARNING, "No Game found\nSwitching to Menu");
            mainFrame.switchToMenu();
        } else if (state == GameState.RUNNING) {
            updateGameUI();
        } else {
            displayGameEndMessage(state);
        }
    }

    /**
     * Updates the game UI
     */
    public void updateGameUI() {
        updateBoard();
        updateList();
        updateActivePlayerHighlight(chess.getCurrentPlayer());
    }

    /**
     * Checks the status of the game.
     *
     * @return The status of the game as described in the Game class
     */
    private GameState getState() {
        return chess.getState();
    }

    /**
     * Updates the active player highlight.
     *
     * @param player The player that is currently active
     */
    private void updateActivePlayerHighlight(Player player) {
        if (player == chess.getPlayerWhite()) {
            topPanel.setBlackActive(false);
            bottomPanel.setWhiteActive(true);
        } else {
            topPanel.setBlackActive(true);
            bottomPanel.setWhiteActive(false);
        }
    }

    /**
     * Updates the board UI.
     */
    private void updateBoard() {
        // Check if the board from chess and the local board differ, then change it
        if (!chess.getBoard().equals(localBoard)) {
            localBoard = chess.getBoard();
            boardPanel.placePieces(localBoard);
        }
    }

    /**
     * Updates the list of previous moves.
     */
    private void updateList() {
        sidePanelRight.updateList(chess.getMoveList());
    }

    /**
     * Displays a message when the game ends.
     *
     * @param state The status of the game as described in the Game class
     */
    private void displayGameEndMessage(GameState state) {
        LOGGER.log(Level.INFO, "Game ended!");
        Player winner = switch (state) {
            case WHITE_WON_BY_CHECKMATE,
                 WHITE_WON_BY_RESIGNATION,
                 WHITE_WON_BY_TIMEOUT -> chess.getPlayerWhite();
            case BLACK_WON_BY_CHECKMATE,
                 BLACK_WON_BY_RESIGNATION,
                 BLACK_WON_BY_TIMEOUT -> chess.getPlayerBlack();
            case DRAW_BY_STALEMATE,
                 DRAW_BY_INSUFFICIENT_MATERIAL,
                 DRAW_BY_THREEFOLD_REPETITION,
                 DRAW_BY_FIFTY_MOVE_RULE -> null;
            default -> null;
        };
        String title = (winner != null) ? winner.name() + " gewinnt" : "Unentschieden";

        ConfirmDialog dialog = new ConfirmDialog(mainFrame, state.getMessage() + "\n\nZurück zum Menü?", title, colorScheme);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            chess.endGame();
            mainFrame.switchToMenu();
        }
    }

    /**
     * Handles a click on a clickedSquare.
     *
     * @param clickedSquare The clickedSquare that was clicked
     */
    public void clickedOn(Square clickedSquare) {
        Optional<Piece> clickedPieceOptional = Optional.ofNullable(chess.getPieceAt(clickedSquare));
        clickedPieceOptional.ifPresent(piece -> System.out.println("Piece on clickedSquare: " + piece.getClass() + " Player:" + piece.getPlayer() + " Color: " + piece.getPlayer().color()));

        if (clickedPieceOptional.isPresent() && clickedPieceOptional.get().getPlayer() == chess.getCurrentPlayer()) {
            // Update selectedSquare and legalSquaresForSelectedPiece only if the clicked square has a piece
            selectedSquare = clickedSquare;
            legalSquaresForSelectedPiece = chess.getLegalSquares(selectedSquare);
            boardPanel.setLegalSquares(legalSquaresForSelectedPiece); // Use local legal squares for UI update
        }
        if (selectedSquare != null && legalSquaresForSelectedPiece.contains(clickedSquare)) {
            boardPanel.switchIconToSelected(selectedSquare, clickedSquare);
            localBoard.executeMove(new Move(selectedSquare, clickedSquare));
            boardPanel.unsetLegalSquares();

            if (chess.getPieceAt(selectedSquare) instanceof ptp.core.data.pieces.Pawn && (clickedSquare.getY() == 0 || clickedSquare.getY() == 7)) {
                PromotionWindow promotionWindow = new PromotionWindow(mainFrame, colorScheme, chess.getCurrentPlayer().color());
                Pieces selectedPiece = promotionWindow.getSelectedPiece();
                new ExecuteMove(chess, this, selectedSquare, clickedSquare, selectedPiece).execute();
            } else {
                new ExecuteMove(chess, this, selectedSquare, clickedSquare, null).execute();
            }
        }
    }
}
