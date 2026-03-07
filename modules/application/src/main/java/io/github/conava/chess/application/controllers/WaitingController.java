package io.github.conava.chess.application.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class WaitingController {

    @FXML private Label codeLabel;

    private final String joinCode;
    private boolean cancelled = false;

    public WaitingController(String joinCode) {
        this.joinCode = joinCode;
    }

    @FXML
    public void initialize() {
        codeLabel.setText(joinCode != null ? joinCode : "—");
    }

    @FXML
    private void onCancel() {
        cancelled = true;
        ((Stage) codeLabel.getScene().getWindow()).close();
    }

    public boolean isCancelled() { return cancelled; }
}
