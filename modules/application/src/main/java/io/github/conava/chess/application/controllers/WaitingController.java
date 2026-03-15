package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;

/**
 * Controller for the waiting overlay shown when the local player is waiting for an opponent
 * to join via a join code.
 *
 * <p>Displays the join code, a copy-to-clipboard button, and a cancel button.
 * All user-visible strings are looked up via {@link I18n} to support localisation.</p>
 */
public class WaitingController {

    @FXML
    private TextField codeField;
    @FXML
    private Button copyBtn;

    private final String joinCode;
    private final Runnable closeAction;
    private final I18n i18n;
    private boolean cancelled = false;
    private String originalBtnText;

    /**
     * Constructs a {@code WaitingController}.
     *
     * @param joinCode    the join code to display; may be {@code null} to show a placeholder.
     * @param closeAction called when the overlay should be dismissed (cancel or external trigger).
     * @param i18n        the i18n service used to look up localised strings such as "Copied!".
     */
    public WaitingController(String joinCode, Runnable closeAction, I18n i18n) {
        this.joinCode = joinCode;
        this.closeAction = closeAction;
        this.i18n = i18n;
    }

    @FXML
    public void initialize() {
        codeField.setText(joinCode != null ? joinCode : "—");
        originalBtnText = copyBtn.getText();
    }

    @FXML
    private void onCopy() {
        ClipboardContent content = new ClipboardContent();
        content.putString(joinCode != null ? joinCode : "");
        Clipboard.getSystemClipboard().setContent(content);

        copyBtn.setText(i18n.get("game.joinCode.copied"));
        copyBtn.setDisable(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> {
            copyBtn.setText(originalBtnText);
            copyBtn.setDisable(false);
        });
        pause.play();
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
