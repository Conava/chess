package io.github.conava.chess.application.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class WaitingController {

    @FXML
    private Label codeLabel;

    private final String joinCode;
    private final Runnable closeAction;
    private boolean cancelled = false;

    public WaitingController(String joinCode, Runnable closeAction) {
        this.joinCode = joinCode;
        this.closeAction = closeAction;
    }

    @FXML
    public void initialize() {
        codeLabel.setText(joinCode != null ? joinCode : "—");
    }

    @FXML
    private void onCancel() {
        cancelled = true;
        closeAction.run();
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
