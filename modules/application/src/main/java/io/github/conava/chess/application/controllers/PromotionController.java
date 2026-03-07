package io.github.conava.chess.application.controllers;

import io.github.conava.chess.core.data.pieces.Pieces;
import io.github.conava.chess.core.data.player.PlayerColor;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class PromotionController {

    @FXML private HBox pieceRow;

    private final PlayerColor playerColor;
    private final Runnable    closeAction;
    private Pieces selectedPiece = Pieces.QUEEN;

    private static final Pieces[] PROMOTION_OPTIONS = {
        Pieces.QUEEN, Pieces.ROOK, Pieces.BISHOP, Pieces.KNIGHT
    };

    public PromotionController(PlayerColor playerColor, Runnable closeAction) {
        this.playerColor = playerColor;
        this.closeAction = closeAction;
    }

    @FXML
    public void initialize() {
        for (Pieces piece : PROMOTION_OPTIONS) {
            String iconPath = "/icon/" + piece.name().toLowerCase()
                    + "_" + playerColor.name().toLowerCase() + ".png";
            var url = getClass().getResource(iconPath);
            Button btn = new Button();
            if (url != null) {
                ImageView iv = new ImageView(new Image(url.toExternalForm()));
                iv.setFitWidth(64);
                iv.setFitHeight(64);
                iv.setPreserveRatio(true);
                btn.setGraphic(iv);
            } else {
                btn.setText(piece.name());
            }
            btn.getStyleClass().add("nav-button");
            Pieces p = piece;
            btn.setOnAction(e -> { selectedPiece = p; closeAction.run(); });
            pieceRow.getChildren().add(btn);
        }
    }

    public Pieces getSelectedPiece() { return selectedPiece; }
}
