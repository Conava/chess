package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for the account registration screen ({@code register.fxml}).
 *
 * <p>Collects server IP, port, username, password, and password confirmation,
 * validates them locally, then delegates to {@link Chess#register} on a
 * background thread. On success, navigates to the main menu via
 * {@link SceneManager#showMainMenu()}. On failure, displays an error message
 * and re-enables the register button.</p>
 */
public class RegisterController {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final SceneManager sceneManager;
    private final Chess chess;
    private final I18n i18n;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private TextField ipField;

    @FXML
    private TextField portField;

    @FXML
    private Button registerButton;

    @FXML
    private Hyperlink loginLink;

    @FXML
    private Label errorLabel;

    /**
     * Constructs a {@code RegisterController} with its required collaborators.
     *
     * @param sceneManager the navigation manager used to switch screens.
     * @param chess        the application facade used for authentication.
     * @param i18n         the internationalisation service for message lookup.
     */
    public RegisterController(SceneManager sceneManager, Chess chess, I18n i18n) {
        this.sceneManager = sceneManager;
        this.chess = chess;
        this.i18n = i18n;
    }

    /**
     * JavaFX initialisation callback. Sets default field values.
     */
    @FXML
    public void initialize() {
        ipField.setText("localhost");
        portField.setText("54321");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    /**
     * Handles the Register button click.
     *
     * <p>Validates all inputs, disables the button, and calls
     * {@link Chess#register}. The callback runs on the FX Application Thread;
     * on success it navigates to the main menu, on failure it shows an error.</p>
     */
    @FXML
    private void onRegister() {
        hideError();

        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        String ip = ipField.getText().trim();
        String portText = portField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showError(i18n.get("register.error.blank"));
            return;
        }

        if (!password.equals(confirm)) {
            showError(i18n.get("register.error.passwordMismatch"));
            return;
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            showError(i18n.get("register.error.passwordShort"));
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            showError(i18n.get("register.error.port"));
            return;
        }

        registerButton.setDisable(true);

        chess.register(ip, port, username, password, success -> {
            // Callback already runs on FX Application Thread via Chess.register
            registerButton.setDisable(false);
            if (success) {
                sceneManager.showMainMenu();
            } else {
                showError(i18n.get("register.error.failed"));
            }
        });
    }

    /**
     * Handles the "Already have an account? Login" hyperlink click.
     * Navigates to the login screen.
     */
    @FXML
    private void onLogin() {
        sceneManager.showLogin();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
