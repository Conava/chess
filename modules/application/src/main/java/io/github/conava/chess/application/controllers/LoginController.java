package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for the login screen ({@code login.fxml}).
 *
 * <p>Collects server IP, port, username, and password, then delegates to
 * {@link Chess#login} on a background thread. On success, navigates to the
 * main menu via {@link SceneManager#showMainMenu()}. On failure, displays an
 * error message and re-enables the login button.</p>
 */
public class LoginController {

    private final SceneManager sceneManager;
    private final Chess chess;
    private final I18n i18n;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField ipField;

    @FXML
    private TextField portField;

    @FXML
    private Button loginButton;

    @FXML
    private Hyperlink registerLink;

    @FXML
    private Label errorLabel;

    /**
     * Constructs a {@code LoginController} with its required collaborators.
     *
     * @param sceneManager the navigation manager used to switch screens.
     * @param chess        the application facade used for authentication.
     * @param i18n         the internationalisation service for message lookup.
     */
    public LoginController(SceneManager sceneManager, Chess chess, I18n i18n) {
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
     * Handles the Login button click.
     *
     * <p>Validates input, disables the button, and calls
     * {@link Chess#login}. The callback runs on the FX Application Thread;
     * on success it navigates to the main menu, on failure it shows an error.</p>
     */
    @FXML
    private void onLogin() {
        hideError();

        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String ip = ipField.getText().trim();
        String portText = portField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError(i18n.get("login.error.blank"));
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            showError(i18n.get("login.error.port"));
            return;
        }

        loginButton.setDisable(true);

        chess.login(ip, port, username, password, success -> {
            // Callback already runs on FX Application Thread via Chess.login
            loginButton.setDisable(false);
            if (success) {
                sceneManager.showMainMenu();
            } else {
                showError(i18n.get("login.error.failed"));
            }
        });
    }

    /**
     * Handles the "Create an account" hyperlink click.
     * Navigates to the registration screen.
     */
    @FXML
    private void onRegister() {
        sceneManager.showRegister();
    }

    /**
     * Handles the Cancel button click.
     * Returns to the main menu without logging in.
     */
    @FXML
    private void onCancel() {
        sceneManager.showMainMenu();
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
