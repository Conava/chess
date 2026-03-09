package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.application.tasks.ExecuteMove;
import io.github.conava.chess.application.theme.ThemeManager;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.pieces.Piece;
import io.github.conava.chess.core.data.pieces.Pieces;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.logic.game.GameState;
import io.github.conava.chess.core.logic.observer.GameObserver;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.scene.Scene;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class GameController implements GameObserver {

    private static final Logger LOGGER = Logger.getLogger(GameController.class.getName());

    private final SceneManager sceneManager;
    private final Chess        chess;
    private final ThemeManager themeManager;
    private final I18n         i18n;

    @FXML private Label    blackName;
    @FXML private Label    blackActive;
    @FXML private Label    whiteName;
    @FXML private Label    whiteActive;
    @FXML private ListView<String> moveList;
    @FXML private StackPane boardContainer;
    @FXML private VBox      leftPanel;
    @FXML private VBox      rightPanel;

    private final StackPane[][] boardSquares = new StackPane[8][8];
    private final List<StackPane> markedSquares = new ArrayList<>();
    private Square selectedSquare = null;
    private List<Square> legalSquares = List.of();
    private Board localBoard;
    private NumberBinding squareSize;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "chess-move-executor");
                t.setDaemon(true);
                return t;
            });

    public GameController(SceneManager sceneManager, Chess chess,
                          ThemeManager themeManager, I18n i18n) {
        this.sceneManager = sceneManager;
        this.chess        = chess;
        this.themeManager = themeManager;
        this.i18n         = i18n;
    }

    @FXML
    public void initialize() {
        buildBoard();
        buildLabels();
        bindPanelWidths();
        registerWithGame();
        updateAll();
    }

    // ── Board construction ────────────────────────────────────────────────────

    private void buildBoard() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("chess-board");

        squareSize = Bindings.min(
                boardContainer.widthProperty(), boardContainer.heightProperty()
        ).divide(8.0);

        for (int row = 7; row >= 0; row--) {
            for (int col = 0; col < 8; col++) {
                StackPane square = new StackPane();
                square.getStyleClass().addAll("board-square",
                        (row + col) % 2 == 0 ? "light-square" : "dark-square");
                square.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                square.prefWidthProperty().bind(squareSize);
                square.prefHeightProperty().bind(squareSize);

                final int r = row, c = col;
                square.setOnMouseClicked(e -> handleSquareClick(r, c));

                boardSquares[row][col] = square;
                grid.add(square, col, 7 - row);
            }
        }

        // Keep the grid at its natural (square) size so StackPane centres it.
        // Without this the StackPane stretches the GridPane to fill boardContainer,
        // leaving the 8×8 block anchored to the top-left corner.
        grid.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        boardContainer.getChildren().add(grid);
    }

    private void buildLabels() {
        // Rank numbers go in the top-left corner of every left-column square.
        // File letters go in the bottom-right corner of every bottom-row square.
        // Both use the opposite square colour so they're always readable.
        for (int row = 7; row >= 0; row--) {
            for (int col = 0; col < 8; col++) {
                StackPane sq = boardSquares[row][col];
                boolean lightSquare = (row + col) % 2 == 0;
                String colourClass = lightSquare ? "board-coord-label-on-light"
                                                 : "board-coord-label-on-dark";

                if (col == 0) {
                    Label rank = new Label(String.valueOf(row + 1));
                    rank.getStyleClass().addAll("board-coord-label", colourClass);
                    rank.styleProperty().bind(squareSize.multiply(0.22)
                            .asString("-fx-font-size: %.1fpx; -fx-font-weight: bold;"
                                    + " -fx-padding: 2;"));
                    rank.setMouseTransparent(true);
                    StackPane.setAlignment(rank, Pos.TOP_LEFT);
                    sq.getChildren().add(rank);
                }

                if (row == 0) {
                    Label file = new Label(String.valueOf((char) ('a' + col)));
                    file.getStyleClass().addAll("board-coord-label", colourClass);
                    file.styleProperty().bind(squareSize.multiply(0.22)
                            .asString("-fx-font-size: %.1fpx; -fx-font-weight: bold;"
                                    + " -fx-padding: 2;"));
                    file.setMouseTransparent(true);
                    StackPane.setAlignment(file, Pos.BOTTOM_RIGHT);
                    sq.getChildren().add(file);
                }
            }
        }
    }

    private void bindPanelWidths() {
        // Bind to SCENE width, not squareSize. Binding panels to squareSize
        // creates a cycle: squareSize depends on boardContainer.width, which
        // depends on panel widths, which would depend on squareSize → oscillation.
        // Scene width is externally driven (by the OS/user) so there is no loop.
        Runnable attach = () -> {
            Scene scene = leftPanel.getScene();
            if (scene == null) return;
            NumberBinding pw = Bindings.max(160.0,
                    Bindings.min(scene.widthProperty().multiply(0.13), 300.0));
            leftPanel.prefWidthProperty().bind(pw);
            rightPanel.prefWidthProperty().bind(pw);
        };
        // Scene may not exist yet at initialize() time — attach when it arrives.
        leftPanel.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) attach.run();
        });
        if (leftPanel.getScene() != null) attach.run();
    }

    // ── Game wiring ───────────────────────────────────────────────────────────

    private void registerWithGame() {
        chess.addObserver(this);
        Player white = chess.getPlayerWhite();
        Player black = chess.getPlayerBlack();
        if (white != null) whiteName.setText(white.name());
        if (black != null) blackName.setText(black.name());
        localBoard = chess.getBoard();
    }

    @Override
    public void onGameStateChanged() {
        Platform.runLater(this::update);
    }

    private void update() {
        GameState state = chess.getState();
        if (state == null || state == GameState.NO_GAME) return;

        switch (state) {
            case RUNNING -> {
                updateAll();
            }
            case WAITING_FOR_PLAYER -> showWaitingDialog();
            case SERVER_ERROR -> showErrorAndReturnToMenu(i18n.get("error.server"));
            default -> showGameEndDialog(state);
        }
    }

    // ── Waiting dialog ────────────────────────────────────────────────────────

    private void showWaitingDialog() {
        String code = chess.getJoinCode();
        WaitingController ctrl = new WaitingController(code, sceneManager::dismissOverlay);
        sceneManager.showOverlay("/fxml/waiting.fxml", ctrl);
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
            String path = "/icon/" + piece.getType().name().toLowerCase()
                    + "_" + piece.getPlayer().color().name().toLowerCase() + ".png";
            var url = getClass().getResource(path);
            if (url != null) {
                ImageView iv = new ImageView(new Image(url.toExternalForm()));
                iv.fitWidthProperty().bind(squareSize.multiply(0.78));
                iv.fitHeightProperty().bind(squareSize.multiply(0.78));
                iv.setPreserveRatio(true);
                iv.setUserData("piece");
                square.getChildren().add(iv);
            }
        }
    }

    private void updateMoveList() {
        moveList.setItems(FXCollections.observableArrayList(chess.getMoveList()));
        if (!moveList.getItems().isEmpty()) {
            moveList.scrollTo(moveList.getItems().size() - 1);
        }
    }

    private void updateActivePlayerIndicator() {
        Player current = chess.getCurrentPlayer();
        String activeText  = i18n.get("game.active");
        String waitingText = i18n.get("game.waiting");
        boolean isWhiteActive = current == chess.getPlayerWhite();
        whiteActive.setText(isWhiteActive ? activeText : waitingText);
        blackActive.setText(isWhiteActive ? waitingText : activeText);
    }

    // ── Board interaction ─────────────────────────────────────────────────────

    private void handleSquareClick(int row, int col) {
        Square clicked = new Square(row, col);
        Piece  piece   = chess.getPieceAt(clicked);

        if (piece != null && piece.getPlayer() == chess.getCurrentPlayer()) {
            clearLegalMoveMarkers();
            selectedSquare = clicked;
            legalSquares   = chess.getLegalSquares(clicked);
            showLegalMoveMarkers(legalSquares);
            return;
        }

        if (selectedSquare != null && legalSquares.contains(clicked)) {
            clearLegalMoveMarkers();
            Piece movingPiece = chess.getPieceAt(selectedSquare);
            if (movingPiece != null && movingPiece.getType() == Pieces.PAWN
                    && (clicked.getY() == 0 || clicked.getY() == 7)) {
                PromotionController promoCtrl =
                        new PromotionController(movingPiece.getPlayer().color(),
                                sceneManager::dismissOverlay);
                sceneManager.showOverlay("/fxml/promotion.fxml", promoCtrl);
                submitMove(selectedSquare, clicked, promoCtrl.getSelectedPiece());
            } else {
                submitMove(selectedSquare, clicked, null);
            }
            selectedSquare = null;
            legalSquares   = List.of();
        } else {
            clearLegalMoveMarkers();
            selectedSquare = null;
            legalSquares   = List.of();
        }
    }

    private void submitMove(Square from, Square to, Pieces promotion) {
        executor.submit(new ExecuteMove(chess, from, to, promotion));
    }

    private void showLegalMoveMarkers(List<Square> squares) {
        for (Square sq : squares) {
            StackPane pane = boardSquares[sq.getY()][sq.getX()];
            Circle dot = new Circle();
            dot.radiusProperty().bind(squareSize.multiply(0.20));
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

        GameEndController ctrl = new GameEndController(
                i18n, state, whitePlayerName, blackPlayerName, moveCount, isOnline,
                sceneManager::dismissOverlay
        );
        sceneManager.showOverlay("/fxml/game-end.fxml", ctrl);

        if (ctrl.getChoice() == GameEndController.Choice.RETURN) {
            chess.endGame();
            sceneManager.showMainMenu();
        } else if (ctrl.getChoice() == GameEndController.Choice.REMATCH) {
            chess.endGame();
            chess.startGame(false,
                    RulesetOptions.STANDARD,
                    whitePlayerName, blackPlayerName, Map.of());
            sceneManager.showGame();
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

    @FXML
    private void onLeaveGame() {
        boolean confirmed = sceneManager.showConfirm(i18n.get("game.leave.confirm"));
        if (confirmed) {
            chess.endGame();
            executor.shutdown();
            sceneManager.showMainMenu();
        }
    }
}
