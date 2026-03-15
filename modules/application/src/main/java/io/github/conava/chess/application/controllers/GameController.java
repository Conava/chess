package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.application.network.ServerCommunicationTask;
import io.github.conava.chess.application.tasks.ExecuteMove;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.pieces.Piece;
import io.github.conava.chess.core.data.pieces.Pieces;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.logic.game.GameState;
import io.github.conava.chess.core.logic.observer.GameObserver;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class GameController implements GameObserver {

    private static final Logger LOGGER = Logger.getLogger(GameController.class.getName());

    private final SceneManager sceneManager;
    private final Chess chess;
    private final I18n i18n;
    private final RulesetOptions ruleset;

    @FXML
    private Label blackName;
    @FXML
    private Label blackActive;
    @FXML
    private Label whiteName;
    @FXML
    private Label whiteActive;
    @FXML
    private Label gameLabelDisplay;
    @FXML
    private ListView<MoveRow> moveList;
    @FXML
    private StackPane boardContainer;
    @FXML
    private VBox leftPanel;
    @FXML
    private VBox rightPanel;
    @FXML
    private VBox chatPanel;
    @FXML
    private ListView<String> chatList;
    @FXML
    private TextField chatInput;
    @FXML
    private Button saveExitBtn;

    private final StackPane[][] boardSquares = new StackPane[8][8];
    private final List<StackPane> markedSquares = new ArrayList<>();
    private Square selectedSquare = null;
    private List<Square> legalSquares = List.of();
    private Board localBoard;

    /**
     * Current board square size in pixels. Starts at 40px and is rebound in
     * {@link #bindPanelWidths()} once the scene is available, using
     * {@code (sceneHeight - verticalPadding) / 8}.
     *
     * <p>All size-dependent UI elements (square panes, piece images, coordinate
     * labels, legal-move dots, promotion picker) bind to this single property so
     * they all update atomically when the window is resized.
     */
    private final DoubleProperty squareSizeProp = new SimpleDoubleProperty(40.0);

    /**
     * Whether the board is rendered from black's perspective (black pieces at the bottom).
     * Set once in {@link #initialize()} based on {@code chess.getLocalPlayerColor()}.
     */
    private boolean boardFlipped;
    private boolean waitingDialogShowing;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "chess-move-executor");
        t.setDaemon(true);
        return t;
    });

    // ── MoveRow record ────────────────────────────────────────────────────────

    /**
     * Immutable data carrier for a single row in the scoresheet move list.
     *
     * <p>Each row corresponds to one full move in chess notation: a move number,
     * the white player's move, and (optionally) the black player's response. When
     * the game ends on white's move, {@code blackMove} is {@code null}.
     *
     * <p>Package-private so that tests in the same package can access it directly
     * without requiring the JavaFX toolkit.
     *
     * @param moveNumber 1-based full-move number (1, 2, 3, …)
     * @param whiteMove  algebraic notation for white's move; never {@code null}
     * @param blackMove  algebraic notation for black's response, or {@code null} if
     *                   black has not yet moved in this round
     */
    record MoveRow(int moveNumber, String whiteMove, String blackMove) {}

    // ── pairMoves ─────────────────────────────────────────────────────────────

    /**
     * Converts a flat list of individual move strings into paired scoresheet rows.
     *
     * <p>The input list contains alternating white and black moves in play order:
     * index 0 is white's first move, index 1 is black's first response, index 2 is
     * white's second move, and so on. This method groups consecutive pairs into
     * {@link MoveRow} instances with 1-based move numbers.
     *
     * <p>When the total number of moves is odd (white has just moved but black has not
     * yet responded), the final row's {@code blackMove()} is {@code null}.
     *
     * <p>Package-private so that tests in the same package can call it directly without
     * requiring the JavaFX toolkit.
     *
     * @param moves the flat list of move strings from {@code chess.getMoveList()};
     *              may be {@code null} or empty
     * @return an unmodifiable list of {@link MoveRow} instances; never {@code null}
     */
    static List<MoveRow> pairMoves(List<String> moves) {
        if (moves == null || moves.isEmpty()) {
            return List.of();
        }
        int size = moves.size();
        List<MoveRow> rows = new ArrayList<>((size + 1) / 2);
        for (int i = 0; i < size; i += 2) {
            String blackMove = (i + 1 < size) ? moves.get(i + 1) : null;
            rows.add(new MoveRow((i / 2) + 1, moves.get(i), blackMove));
        }
        return List.copyOf(rows);
    }

    public GameController(SceneManager sceneManager, Chess chess, I18n i18n, RulesetOptions ruleset) {
        this.sceneManager = sceneManager;
        this.chess = chess;
        this.i18n = i18n;
        this.ruleset = ruleset;
    }

    @FXML
    public void initialize() {
        PlayerColor localColor = chess.getLocalPlayerColor();
        boardFlipped = (localColor == PlayerColor.BLACK);
        buildBoard();
        buildLabels();
        moveList.setCellFactory(lv -> new ScoresheetCell());
        buildScoresheetHeader();
        bindPanelWidths();
        registerWithGame();
        configureOnlineFeatures();
        // Defer update() — it may open a nested event loop (waiting dialog),
        // which JavaFX forbids during FXML loading / layout processing.
        Platform.runLater(this::update);
    }

    /**
     * Detects whether the current game is online and, if so, shows the chat panel and the
     * "Save &amp; Exit" button, then registers the chat, save-accepted, and save-game handlers
     * on the active {@link ServerCommunicationTask}.
     */
    private void configureOnlineFeatures() {
        boolean isOnline = chess.getJoinCode() != null;
        if (!isOnline) return;

        chatPanel.setVisible(true);
        chatPanel.setManaged(true);
        saveExitBtn.setVisible(true);
        saveExitBtn.setManaged(true);

        // Allow sending chat messages by pressing Enter in the text field.
        chatInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                onSendChat();
            }
        });

        ServerCommunicationTask task = chess.getActiveServerTask();
        if (task == null) return;

        task.setChatHandler(msg -> {
            String text = formatChatMessage(msg.content());
            Platform.runLater(() -> chatList.getItems().add(text));
        });

        task.setSaveAcceptedHandler(() ->
                Platform.runLater(() -> {
                    chess.endGame();
                    sceneManager.showMainMenu();
                }));

        task.setSaveGameHandler(msg ->
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            i18n.get("game.save.opponentRequest"),
                            ButtonType.YES, ButtonType.NO);
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.YES) {
                        chess.requestSaveGame();
                    }
                }));
    }

    /**
     * Parses a server chat message content string and formats it for display.
     *
     * <p>The server encodes chat messages as {@code "sender=<name> content=<text>"} where
     * {@code content} is always the last parameter and may contain spaces. This method
     * extracts the sender name (up to the first space after {@code sender=}) and the full
     * content text (everything after {@code content=}), then returns {@code "name: text"}.
     *
     * <p>Fallback behaviour:
     * <ul>
     *   <li>If no {@code sender=} key is found, the output omits the sender prefix.</li>
     *   <li>If no {@code content=} key is found, the raw string is returned as-is.</li>
     * </ul>
     *
     * @param rawContent the message content string from the server; must not be {@code null}.
     * @return the human-readable chat text in the form {@code "Sender: message"}, or just
     *         the content / raw text when the sender is unavailable.
     */
    static String formatChatMessage(String rawContent) {
        if (rawContent == null) return "";
        String sender = null;
        String content = rawContent;

        // Extract sender name — it ends at the first space after "sender="
        int senderIdx = rawContent.indexOf("sender=");
        if (senderIdx >= 0) {
            int valueStart = senderIdx + "sender=".length();
            int valueEnd = rawContent.indexOf(' ', valueStart);
            sender = valueEnd >= 0
                    ? rawContent.substring(valueStart, valueEnd)
                    : rawContent.substring(valueStart);
        }

        // Extract content — everything after "content=" to end-of-string (may contain spaces)
        int contentIdx = rawContent.indexOf("content=");
        if (contentIdx >= 0) {
            content = rawContent.substring(contentIdx + "content=".length());
        }

        if (sender != null && !sender.isEmpty()) {
            return sender + ": " + content;
        }
        return content;
    }

    // ── Board construction ────────────────────────────────────────────────────

    /**
     * Constructs the 8×8 board of {@link StackPane} squares and adds them to the
     * {@link GridPane} inside {@code boardContainer}.
     *
     * <p>The logical mapping {@code boardSquares[row][col]} always corresponds to the board
     * square at rank {@code row+1} and file {@code col} (0 = a-file). Only the <em>visual</em>
     * placement in the GridPane varies: when {@link #boardFlipped} is {@code true} (black's
     * perspective), row 0 is placed at GridPane row 0 (top) and columns are reversed so the
     * h-file appears on the left. Click handlers capture logical coordinates and are unaffected
     * by the flip.</p>
     */
    private void buildBoard() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("chess-board");

        // squareSizeProp is initially 40px; it will be rebound to a height-based
        // expression in bindPanelWidths() once the scene is available.
        for (int row = 7; row >= 0; row--) {
            for (int col = 0; col < 8; col++) {
                StackPane square = new StackPane();
                square.getStyleClass().addAll("board-square", (row + col) % 2 == 0 ? "light-square" : "dark-square");
                square.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                square.prefWidthProperty().bind(squareSizeProp);
                square.prefHeightProperty().bind(squareSizeProp);

                final int r = row, c = col;
                square.setOnMouseClicked(e -> handleSquareClick(r, c));

                boardSquares[row][col] = square;

                // Visual placement: when flipped (black's perspective), row 0 appears at the
                // top of the GridPane and columns are reversed so h-file is on the left.
                int gridRow = boardFlipped ? row : 7 - row;
                int gridCol = boardFlipped ? 7 - col : col;
                grid.add(square, gridCol, gridRow);
            }
        }

        // Keep the grid at its natural (square) size so StackPane centres it.
        // Without this the StackPane stretches the GridPane to fill boardContainer,
        // leaving the 8×8 block anchored to the top-left corner.
        grid.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        boardContainer.getChildren().add(grid);
    }

    /**
     * Adds coordinate labels (rank numbers and file letters) to the appropriate board squares.
     *
     * <p>Rank numbers appear in the top-left corner of every square in the <em>left-most
     * visual column</em>. File letters appear in the bottom-right corner of every square in
     * the <em>bottom-most visual row</em>. When {@link #boardFlipped} is {@code true}
     * (black's perspective), the left-most visual column corresponds to logical col 7
     * (h-file) and the bottom-most visual row corresponds to logical row 7.</p>
     */
    private void buildLabels() {
        // When flipped: rank labels go on col 7 (which visually becomes the left column),
        //               file labels go on row 7 (which visually becomes the bottom row).
        int rankLabelCol = boardFlipped ? 7 : 0;
        int fileLabelRow = boardFlipped ? 7 : 0;

        for (int row = 7; row >= 0; row--) {
            for (int col = 0; col < 8; col++) {
                StackPane sq = boardSquares[row][col];
                boolean lightSquare = (row + col) % 2 == 0;
                String colourClass = lightSquare ? "board-coord-label-on-light" : "board-coord-label-on-dark";

                if (col == rankLabelCol) {
                    Label rank = new Label(String.valueOf(row + 1));
                    rank.getStyleClass().addAll("board-coord-label", colourClass);
                    rank.styleProperty().bind(squareSizeProp.multiply(0.22).asString("-fx-font-size: %.1fpx; -fx-font-weight: bold;" + " -fx-padding: 2;"));
                    rank.setMouseTransparent(true);
                    StackPane.setAlignment(rank, Pos.TOP_LEFT);
                    sq.getChildren().add(rank);
                }

                if (row == fileLabelRow) {
                    Label file = new Label(String.valueOf((char) ('a' + col)));
                    file.getStyleClass().addAll("board-coord-label", colourClass);
                    file.styleProperty().bind(squareSizeProp.multiply(0.22).asString("-fx-font-size: %.1fpx; -fx-font-weight: bold;" + " -fx-padding: 2;"));
                    file.setMouseTransparent(true);
                    StackPane.setAlignment(file, Pos.BOTTOM_RIGHT);
                    sq.getChildren().add(file);
                }
            }
        }
    }

    /**
     * Binds the board square size and side panel widths to the current scene dimensions.
     *
     * <p>Board sizing derives from the scene <em>height</em> so it is independent of
     * panel widths. This breaks the circular dependency that would arise if the board
     * derived its size from {@code boardContainer.width}, which in turn depends on how
     * much space is left after the panels are sized.
     *
     * <ul>
     *   <li>{@code squareSizeProp} → {@code max(40, (sceneHeight - 40) / 8)}</li>
     *   <li>Each panel prefWidth → {@code max(160, (sceneWidth - boardWidth) / 2)}</li>
     * </ul>
     *
     * <p>The binding is deferred until the scene is attached because
     * {@code leftPanel.getScene()} returns {@code null} during {@code initialize()}.
     */
    private void bindPanelWidths() {
        Runnable attach = () -> {
            Scene scene = leftPanel.getScene();
            if (scene == null) return;

            // Board height-based sizing: squareSize = max(40, (sceneHeight - 40) / 8).
            // The 40px vertical padding matches the VBox insets (20px top + 20px bottom).
            double verticalPadding = 40.0;
            squareSizeProp.bind(Bindings.createDoubleBinding(
                    () -> computeSquareSize(scene.getHeight(), verticalPadding),
                    scene.heightProperty()));

            // Panel width = half of (sceneWidth - boardWidth), minimum 160px.
            double minPanelWidth = 160.0;
            NumberBinding panelWidth = Bindings.createDoubleBinding(
                    () -> computePanelWidth(scene.getWidth(), squareSizeProp.get() * 8.0, minPanelWidth),
                    scene.widthProperty(), squareSizeProp);

            leftPanel.prefWidthProperty().bind(panelWidth);
            rightPanel.prefWidthProperty().bind(panelWidth);
        };
        // Scene may not exist yet at initialize() time — attach when it arrives.
        leftPanel.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) attach.run();
        });
        if (leftPanel.getScene() != null) attach.run();
    }

    // ── Game wiring ───────────────────────────────────────────────────────────

    /**
     * Wires this controller to the active game as an observer and populates player name labels.
     *
     * <p>When the board is flipped (black's perspective), the label positions are swapped so
     * that the local player's name always appears at the bottom and the opponent's at the top,
     * regardless of which label element ({@code whiteName} / {@code blackName}) is physically
     * at the bottom of the FXML layout.</p>
     */
    private void registerWithGame() {
        chess.addObserver(this);
        Player white = chess.getPlayerWhite();
        Player black = chess.getPlayerBlack();
        if (boardFlipped) {
            // Local player is black — show black's name at the bottom label (whiteName position)
            // and white's name at the top label (blackName position).
            if (black != null) whiteName.setText(black.name());
            if (white != null) blackName.setText(white.name());
        } else {
            if (white != null) whiteName.setText(white.name());
            if (black != null) blackName.setText(black.name());
        }
        localBoard = chess.getBoard();
    }

    @Override
    public void onGameStateChanged() {
        Platform.runLater(() -> {
            if (waitingDialogShowing && chess.getState() != GameState.WAITING_FOR_PLAYER) {
                sceneManager.dismissOverlay();
            }
            update();
        });
    }

    private void update() {
        GameState state = chess.getState();
        if (state == null || state == GameState.NO_GAME) return;

        switch (state) {
            case RUNNING -> updateAll();
            case WAITING_FOR_PLAYER -> showWaitingDialog();
            case SERVER_ERROR -> showErrorAndReturnToMenu(i18n.get("error.server"));
            case PAUSED -> handlePausedState();
            case SAVED -> handleSavedState();
            default -> {
                if (isGameEndState(state)) {
                    showGameEndDialog(state);
                }
                // Non-terminal states that don't match any explicit case: do nothing.
                // This prevents future non-terminal GameState values from accidentally
                // triggering the game-end dialog.
            }
        }
    }

    /**
     * Returns {@code true} if the given {@link GameState} represents a terminal game outcome
     * that should trigger the game-end overlay dialog.
     *
     * <p>Terminal states follow a naming convention:
     * <ul>
     *   <li>Win states start with {@code "WHITE_WON"} or {@code "BLACK_WON"}</li>
     *   <li>Draw states start with {@code "DRAW"}</li>
     * </ul>
     * Non-terminal states (RUNNING, PAUSED, SAVED, WAITING_FOR_PLAYER, etc.) return {@code false}.
     *
     * <p>Package-private for testability without requiring the JavaFX toolkit.
     *
     * @param state the game state to check
     * @return {@code true} for win and draw terminal states, {@code false} otherwise
     */
    static boolean isGameEndState(GameState state) {
        String name = state.name();
        return name.startsWith("WHITE_WON")
                || name.startsWith("BLACK_WON")
                || name.startsWith("DRAW");
    }

    // ── Waiting dialog ────────────────────────────────────────────────────────

    private void showWaitingDialog() {
        String code = chess.getJoinCode();
        WaitingController ctrl = new WaitingController(code, sceneManager::dismissOverlay, i18n);
        waitingDialogShowing = true;
        sceneManager.showOverlay("/fxml/waiting.fxml", ctrl);
        waitingDialogShowing = false;
        if (ctrl.isCancelled()) {
            chess.endGame();
            sceneManager.showMainMenu();
        }
    }

    // ── UI update ─────────────────────────────────────────────────────────────

    private void updateAll() {
        updateBoard();
        updateMoveList();
        updateActivePlayerIndicator();
        updateGameLabel();
    }

    /**
     * Updates the game label display based on the current game label from the model.
     *
     * <p>When a label is present (e.g., the ruleset name), it is shown. When no label
     * is available — which happens when the game transitions back to RUNNING after a
     * PAUSED state — the label is hidden so the "waiting for reconnection" text set
     * by {@link #handlePausedState()} does not persist after the opponent reconnects.</p>
     */
    private void updateGameLabel() {
        String label = chess.getGameLabel();
        if (label != null && !label.isEmpty()) {
            gameLabelDisplay.setText(label);
            gameLabelDisplay.setVisible(true);
            gameLabelDisplay.setManaged(true);
        } else {
            gameLabelDisplay.setVisible(false);
            gameLabelDisplay.setManaged(false);
        }
    }

    private void updateBoard() {
        Board board = chess.getBoard();
        if (board == null) return;
        if (board.equals(localBoard)) return;
        localBoard = board;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                setPieceOnSquare(row, col, board.getPieceAt(new Square(row, col)));
            }
        }
    }

    private void setPieceOnSquare(int row, int col, Piece piece) {
        StackPane square = boardSquares[row][col];
        square.getChildren().removeIf(n -> "piece".equals(n.getUserData()));
        if (piece != null) {
            String path = "/icon/" + piece.getType().name().toLowerCase() + "_" + piece.getPlayer().color().name().toLowerCase() + ".png";
            var url = getClass().getResource(path);
            if (url != null) {
                ImageView iv = new ImageView(new Image(url.toExternalForm()));
                iv.fitWidthProperty().bind(squareSizeProp.multiply(0.78));
                iv.fitHeightProperty().bind(squareSizeProp.multiply(0.78));
                iv.setPreserveRatio(true);
                iv.setUserData("piece");
                square.getChildren().add(iv);
            }
        }
    }

    /**
     * Refreshes the scoresheet move list from the current game state.
     *
     * <p>Converts the flat list of individual move strings (from {@link Chess#getMoveList()})
     * into paired {@link MoveRow} objects via {@link #pairMoves(List)}, then populates the
     * {@link #moveList} and scrolls to the bottom so the latest move is always visible.
     */
    private void updateMoveList() {
        List<MoveRow> rows = pairMoves(chess.getMoveList());
        moveList.setItems(FXCollections.observableArrayList(rows));
        if (!moveList.getItems().isEmpty()) {
            moveList.scrollTo(moveList.getItems().size() - 1);
        }
    }

    /**
     * Updates the "active" / "waiting" indicator labels next to each player's name.
     *
     * <p>When the board is flipped (black's perspective), the {@code whiteName} label is at
     * the bottom and represents the local player (black). The indicator must follow the same
     * swap applied in {@link #registerWithGame()}: the {@code whiteActive} label corresponds
     * to black's status and vice-versa.</p>
     */
    private void updateActivePlayerIndicator() {
        Player current = chess.getCurrentPlayer();
        String activeText = i18n.get("game.active");
        String waitingText = i18n.get("game.waiting");
        boolean isWhiteActive = current == chess.getPlayerWhite();
        if (boardFlipped) {
            // whiteActive label is next to the bottom name label, which shows black's name.
            whiteActive.setText(isWhiteActive ? waitingText : activeText);
            blackActive.setText(isWhiteActive ? activeText : waitingText);
        } else {
            whiteActive.setText(isWhiteActive ? activeText : waitingText);
            blackActive.setText(isWhiteActive ? waitingText : activeText);
        }
    }

    // ── Board interaction ─────────────────────────────────────────────────────

    private void handleSquareClick(int row, int col) {
        Square clicked = new Square(row, col);
        Piece piece = chess.getPieceAt(clicked);

        if (piece != null && piece.getPlayer() == chess.getCurrentPlayer()) {
            clearLegalMoveMarkers();
            selectedSquare = clicked;
            legalSquares = chess.getLegalSquares(clicked);
            showLegalMoveMarkers(legalSquares);
            return;
        }

        if (selectedSquare != null && legalSquares.contains(clicked)) {
            clearLegalMoveMarkers();
            Piece movingPiece = chess.getPieceAt(selectedSquare);
            if (movingPiece != null && movingPiece.getType() == Pieces.PAWN && (clicked.getY() == 0 || clicked.getY() == 7)) {
                PromotionController promoCtrl = new PromotionController(movingPiece.getPlayer().color(), sceneManager::dismissOverlay, squareSizeProp.multiply(0.9));
                sceneManager.showOverlay("/fxml/promotion.fxml", promoCtrl);
                submitMove(selectedSquare, clicked, promoCtrl.getSelectedPiece());
            } else {
                submitMove(selectedSquare, clicked, null);
            }
            selectedSquare = null;
            legalSquares = List.of();
        } else {
            clearLegalMoveMarkers();
            selectedSquare = null;
            legalSquares = List.of();
        }
    }

    private void submitMove(Square from, Square to, Pieces promotion) {
        executor.submit(new ExecuteMove(chess, from, to, promotion));
    }

    private void showLegalMoveMarkers(List<Square> squares) {
        for (Square sq : squares) {
            StackPane pane = boardSquares[sq.getY()][sq.getX()];
            Circle dot = new Circle();
            dot.radiusProperty().bind(squareSizeProp.multiply(0.20));
            dot.getStyleClass().add("legal-move-dot");
            dot.setUserData("dot");
            dot.setMouseTransparent(true);
            dot.setOpacity(0);
            dot.setScaleX(0.5);
            dot.setScaleY(0.5);
            pane.getChildren().add(dot);
            markedSquares.add(pane);

            FadeTransition fade = new FadeTransition(Duration.millis(90), dot);
            fade.setToValue(0.65);
            ScaleTransition scale = new ScaleTransition(Duration.millis(90), dot);
            scale.setToX(1.0);
            scale.setToY(1.0);
            new ParallelTransition(fade, scale).play();
        }
    }

    private void clearLegalMoveMarkers() {
        for (StackPane pane : markedSquares) {
            pane.getChildren().removeIf(n -> "dot".equals(n.getUserData()));
        }
        markedSquares.clear();
    }

    // ── End-game dialogs ──────────────────────────────────────────────────────

    private void showGameEndDialog(GameState state) {
        boolean isOnline = chess.getJoinCode() != null;
        int moveCount = chess.getMoveList().size();
        String whitePlayerName = chess.getPlayerWhite() != null ? chess.getPlayerWhite().name() : "";
        String blackPlayerName = chess.getPlayerBlack() != null ? chess.getPlayerBlack().name() : "";
        // Pass the local player's color so GameEndController can show the correct outcome
        // ("You Win!" vs "You Lose") for online games.
        PlayerColor localPlayerColor = chess.getLocalPlayerColor();

        GameEndController ctrl = new GameEndController(i18n, state, whitePlayerName, blackPlayerName,
                moveCount, isOnline, localPlayerColor, sceneManager::dismissOverlay);
        sceneManager.showOverlay("/fxml/game-end.fxml", ctrl);

        if (ctrl.getChoice() == GameEndController.Choice.RETURN) {
            chess.endGame();
            sceneManager.showMainMenu();
        } else if (ctrl.getChoice() == GameEndController.Choice.REMATCH) {
            chess.endGame();
            chess.startGame(isOnline, this.ruleset, whitePlayerName, blackPlayerName, Map.of());
            sceneManager.showGame(this.ruleset);
        }
        // NONE = player dismissed without choosing (shouldn't happen in practice)
    }

    private void showErrorAndReturnToMenu(String message) {
        boolean confirmed = sceneManager.showConfirm(message);
        if (confirmed) {
            chess.endGame();
            sceneManager.showMainMenu();
        }
    }

    /**
     * Handles the PAUSED state, which occurs when the opponent disconnects.
     *
     * <p>Keeps the board visible (so the local player can review the position) and
     * shows a non-blocking status label informing the player that the opponent has
     * disconnected and the game is waiting for reconnection. A blocking dialog would
     * prevent the user from inspecting the board.</p>
     *
     * <p>When the opponent reconnects, the server will send a GAME_STATUS RUNNING message,
     * which triggers {@code case RUNNING -> updateAll()}, which calls {@link #updateGameLabel()}
     * to clear the paused label (since the game label from the model will reflect the new state).</p>
     */
    private void handlePausedState() {
        updateAll();  // Keep the board current and visible
        gameLabelDisplay.setText(i18n.get("game.paused.label"));
        gameLabelDisplay.setVisible(true);
        gameLabelDisplay.setManaged(true);
    }

    /**
     * Handles the SAVED state: the game has been persisted and both players agreed to save.
     * Navigates back to the main menu.
     */
    private void handleSavedState() {
        sceneManager.showMainMenu();
    }

    /**
     * Sends the text in {@link #chatInput} to the opponent via the server.
     * No-op if the input is blank.
     */
    @FXML
    private void onSendChat() {
        String text = chatInput.getText();
        if (text == null || text.isBlank()) return;
        chess.sendChat(text);
        chatInput.clear();
    }

    /**
     * Initiates a save-and-exit request. Sends a {@code SAVE_GAME} message to the server
     * and shows a status label informing the local player that the request was sent.
     * The actual navigation occurs when the opponent accepts (via {@link #configureOnlineFeatures()}'s
     * {@code saveAcceptedHandler}).
     */
    @FXML
    private void onSaveAndExit() {
        chess.requestSaveGame();
        saveExitBtn.setText(i18n.get("game.save.waiting"));
        saveExitBtn.setDisable(true);
    }

    @FXML
    private void onLeaveGame() {
        boolean confirmed = sceneManager.showConfirm(i18n.get("game.leave.confirm"));
        if (confirmed) {
            chess.endGame();
            executor.shutdown();
            sceneManager.showMainMenu();
        }
    }

    // ── Scoresheet header ─────────────────────────────────────────────────────

    /**
     * Inserts a column-header row above the scoresheet move list in the right panel.
     *
     * <p>The header uses the same CSS classes as the cell rows (scoresheet-move-number,
     * scoresheet-white-move, scoresheet-black-move) so the column widths align visually.
     * It is built programmatically rather than in FXML to avoid adding an extra
     * {@code @FXML} field and keep the FXML file stable.
     */
    private void buildScoresheetHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("scoresheet-header");

        Label numHeader = new Label(i18n.get("game.moves.number"));
        numHeader.getStyleClass().addAll("scoresheet-header-label", "scoresheet-move-number");

        Label whiteHeader = new Label(i18n.get("game.moves.white"));
        whiteHeader.getStyleClass().addAll("scoresheet-header-label", "scoresheet-white-move");
        HBox.setHgrow(whiteHeader, Priority.ALWAYS);
        whiteHeader.setMaxWidth(Double.MAX_VALUE);

        Label blackHeader = new Label(i18n.get("game.moves.black"));
        blackHeader.getStyleClass().addAll("scoresheet-header-label", "scoresheet-black-move");
        HBox.setHgrow(blackHeader, Priority.ALWAYS);
        blackHeader.setMaxWidth(Double.MAX_VALUE);

        header.getChildren().addAll(numHeader, whiteHeader, blackHeader);

        // Insert the header into the right panel immediately before the moveList.
        int moveListIndex = rightPanel.getChildren().indexOf(moveList);
        if (moveListIndex >= 0) {
            rightPanel.getChildren().add(moveListIndex, header);
        }
    }

    // ── Sizing math helpers (static for testability) ──────────────────────────

    /**
     * Computes the board square size in pixels from the scene height.
     *
     * <p>Formula: {@code max(40, (sceneHeight - verticalPadding) / 8)}.
     * The 40px minimum ensures the board is never smaller than 320x320px.
     *
     * <p>Package-private so unit tests can call it without the JavaFX toolkit.
     *
     * @param sceneHeight     total scene height in pixels
     * @param verticalPadding top + bottom padding around the board (typically 40px)
     * @return the computed square size, at least 40px
     */
    static double computeSquareSize(double sceneHeight, double verticalPadding) {
        return Math.max(40.0, (sceneHeight - verticalPadding) / 8.0);
    }

    /**
     * Computes the width of each side panel given the total scene width and board width.
     *
     * <p>Formula: {@code max(minWidth, (sceneWidth - boardWidth) / 2)}.
     * Equal space is given to both panels; the minimum prevents the panels from
     * becoming unreadably narrow on small windows.
     *
     * <p>Package-private so unit tests can call it without the JavaFX toolkit.
     *
     * @param sceneWidth total scene width in pixels
     * @param boardWidth board pixel width ({@code squareSize * 8})
     * @param minWidth   minimum panel width (typically 160px)
     * @return the computed panel width, at least {@code minWidth}
     */
    static double computePanelWidth(double sceneWidth, double boardWidth, double minWidth) {
        return Math.max(minWidth, (sceneWidth - boardWidth) / 2.0);
    }

    // ── ScoresheetCell ────────────────────────────────────────────────────────

    /**
     * Custom {@link ListCell} that renders a {@link MoveRow} as a three-column row:
     * move number, white move, and black move.
     *
     * <p>The move number column uses a fixed min-width monospace label so it never
     * wraps. The white and black columns grow equally to fill the remaining space.
     * An empty cell (for virtual cells beyond the list size) sets no graphic so the
     * ListView shows the correct background.
     */
    private static class ScoresheetCell extends ListCell<MoveRow> {

        private final HBox row = new HBox();
        private final Label numberLabel = new Label();
        private final Label whiteLabel = new Label();
        private final Label blackLabel = new Label();

        ScoresheetCell() {
            row.getStyleClass().add("scoresheet-row");
            numberLabel.getStyleClass().add("scoresheet-move-number");
            whiteLabel.getStyleClass().add("scoresheet-white-move");
            blackLabel.getStyleClass().add("scoresheet-black-move");
            // Allow white and black columns to expand equally.
            HBox.setHgrow(whiteLabel, Priority.ALWAYS);
            HBox.setHgrow(blackLabel, Priority.ALWAYS);
            whiteLabel.setMaxWidth(Double.MAX_VALUE);
            blackLabel.setMaxWidth(Double.MAX_VALUE);
            row.getChildren().addAll(numberLabel, whiteLabel, blackLabel);
        }

        @Override
        protected void updateItem(MoveRow item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                numberLabel.setText(item.moveNumber() + ".");
                whiteLabel.setText(item.whiteMove());
                blackLabel.setText(item.blackMove() != null ? item.blackMove() : "");
                setGraphic(row);
                setText(null);
            }
        }
    }
}
