package io.github.conava.chess.application.controllers;

import io.github.conava.chess.core.data.pieces.Pieces;
import io.github.conava.chess.core.data.player.PlayerColor;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.beans.binding.NumberBinding;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.Duration;

public class PromotionController {

    @FXML private HBox pieceRow;

    private final PlayerColor  playerColor;
    private final Runnable     closeAction;
    private final NumberBinding cellSize;
    private Pieces selectedPiece = Pieces.QUEEN;

    private static final Pieces[] PROMOTION_OPTIONS = {
        Pieces.QUEEN, Pieces.ROOK, Pieces.BISHOP, Pieces.KNIGHT
    };

    public PromotionController(PlayerColor playerColor, Runnable closeAction,
                               NumberBinding cellSize) {
        this.playerColor = playerColor;
        this.closeAction = closeAction;
        this.cellSize    = cellSize;
    }

    @FXML
    public void initialize() {
        for (Pieces piece : PROMOTION_OPTIONS) {
            String iconPath = "/icon/" + piece.name().toLowerCase()
                    + "_" + playerColor.name().toLowerCase() + ".png";
            var url = getClass().getResource(iconPath);
            Button btn = new Button();
            btn.prefWidthProperty().bind(cellSize);
            btn.prefHeightProperty().bind(cellSize);
            btn.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            if (url != null) {
                ImageView iv = new ImageView(new Image(url.toExternalForm()));
                iv.fitWidthProperty().bind(cellSize.multiply(0.78));
                iv.fitHeightProperty().bind(cellSize.multiply(0.78));
                iv.setPreserveRatio(true);
                btn.setGraphic(iv);
            } else {
                btn.setText(piece.name());
            }
            btn.getStyleClass().add("promotion-piece-btn");
            Pieces p = piece;
            btn.setOnAction(e -> { selectedPiece = p; closeAction.run(); });
            pieceRow.getChildren().add(btn);
        }

        Node root = pieceRow.getParent();
        root.setOpacity(0);
        root.setScaleX(0.88);
        root.setScaleY(0.88);

        FadeTransition fade = new FadeTransition(Duration.millis(140), root);
        fade.setToValue(1.0);
        ScaleTransition scale = new ScaleTransition(Duration.millis(140), root);
        scale.setToX(1.0);
        scale.setToY(1.0);
        new ParallelTransition(fade, scale).play();
    }

    public Pieces getSelectedPiece() { return selectedPiece; }
}
