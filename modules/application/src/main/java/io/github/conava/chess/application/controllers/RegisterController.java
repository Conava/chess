package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.menu.ResponsiveMenuLayout;
import io.github.conava.chess.application.navigation.PanelHost;
import io.github.conava.chess.application.navigation.PanelId;
import io.github.conava.chess.application.navigation.SceneManager;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the account registration panel ({@code register.fxml}).
 *
 * <p>Collects server IP, port, username, password, and password confirmation,
 * validates them locally, then delegates to {@link Chess#register} on a
 * background thread. On success, closes the panel via
 * {@link PanelHost#closePanel()}, returning to the clean main menu. On failure,
 * displays an error message and re-enables the register button.</p>
 *
 * <p>The "Already have an account?" link transitions to the login panel via
 * {@link PanelHost#switchPanel(PanelId)} with {@link PanelId#LOGIN}.</p>
 *
 * <h2>Responsive scaling</h2>
 * <p>Font sizes, panel padding, and panel spacing are bound to the stage width
 * via {@link ResponsiveMenuLayout} in {@link #initialize()}. A {@link SimpleDoubleProperty}
 * is used as the binding root so that bindings are active immediately at initialization
 * and then track the real stage width. Binding references are stored as instance fields
 * to prevent garbage collection from severing the reactive chain.</p>
 */
public class RegisterController {

    private static final int MIN_PASSWORD_LENGTH = 8;

    /** Default stage width used as initial value for responsive bindings before the stage is known. */
    private static final double DEFAULT_STAGE_WIDTH = 1280.0;

    private final SceneManager sceneManager;
    private final Chess chess;
    private final I18n i18n;
    private final PanelHost panelHost;

    @FXML
    private StackPane rootPane;

    @FXML
    private VBox registerPanel;

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

    // ── Responsive binding infrastructure ───────────────────────────────────

    /**
     * Mutable width source for all responsive bindings. Initialized to a sensible default
     * and updated to the actual stage width once the panel is in the scene graph.
     */
    private final DoubleProperty stageWidthSource = new SimpleDoubleProperty(DEFAULT_STAGE_WIDTH);

    /** Dialog title font-size binding — stored to prevent GC. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding titleSizeBinding;

    /** Section heading font-size binding — stored to prevent GC. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding headingSizeBinding;

    /** Body / label font-size binding — stored to prevent GC. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding bodySizeBinding;

    /** Button font-size binding — stored to prevent GC. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding btnSizeBinding;

    /** Panel padding binding — stored to prevent GC. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding panelPadBinding;

    /** Panel spacing binding — stored to prevent GC. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding panelSpaceBinding;

    /** Text field padding binding — stored to prevent GC. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding fieldPadBinding;

    /**
     * Constructs a {@code RegisterController} with its required collaborators.
     *
     * @param sceneManager the navigation manager (retained for potential game-screen navigation).
     * @param chess        the application facade used for authentication.
     * @param i18n         the internationalisation service for message lookup.
     * @param panelHost    the panel host used for cancel, register success, and login-link navigation.
     */
    public RegisterController(SceneManager sceneManager, Chess chess, I18n i18n, PanelHost panelHost) {
        this.sceneManager = sceneManager;
        this.chess = chess;
        this.i18n = i18n;
        this.panelHost = panelHost;
    }

    /**
     * JavaFX initialisation callback. Sets default field values, hides the error
     * label, wires responsive bindings to {@link #stageWidthSource}, and attaches
     * a scene listener to sync {@code stageWidthSource} with the real stage width
     * once available.
     */
    @FXML
    public void initialize() {
        if (ipField != null) ipField.setText("localhost");
        if (portField != null) portField.setText("54321");
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }

        if (rootPane == null) return; // guard for unit tests that do not load FXML

        // Wire all bindings to stageWidthSource (immediately active at DEFAULT_STAGE_WIDTH).
        wireBindings();

        // Sync stageWidthSource to real stage width once the panel enters the scene graph.
        // A window listener is chained inside the scene listener to handle the case where
        // the scene exists but its window is not yet attached (scene created but not shown).
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                if (newScene.getWindow() != null) {
                    stageWidthSource.bind(newScene.getWindow().widthProperty());
                } else {
                    newScene.windowProperty().addListener((wObs, oldW, newW) -> {
                        if (newW != null) {
                            stageWidthSource.bind(newW.widthProperty());
                        }
                    });
                }
            }
        });
        if (rootPane.getScene() != null) {
            if (rootPane.getScene().getWindow() != null) {
                stageWidthSource.bind(rootPane.getScene().getWindow().widthProperty());
            } else {
                rootPane.getScene().windowProperty().addListener((wObs, oldW, newW) -> {
                    if (newW != null) {
                        stageWidthSource.bind(newW.widthProperty());
                    }
                });
            }
        }
    }

    /**
     * Creates all responsive bindings from {@link #stageWidthSource} and applies
     * them to UI elements. Called once from {@link #initialize()}.
     */
    private void wireBindings() {
        titleSizeBinding  = ResponsiveMenuLayout.panelDialogTitleFontSize(stageWidthSource);
        headingSizeBinding = ResponsiveMenuLayout.panelSectionHeadingFontSize(stageWidthSource);
        bodySizeBinding   = ResponsiveMenuLayout.panelBodyFontSize(stageWidthSource);
        btnSizeBinding    = ResponsiveMenuLayout.panelButtonFontSize(stageWidthSource);
        panelPadBinding   = ResponsiveMenuLayout.panelPadding(stageWidthSource);
        panelSpaceBinding = ResponsiveMenuLayout.panelSpacing(stageWidthSource);
        fieldPadBinding   = ResponsiveMenuLayout.panelFieldPadding(stageWidthSource);

        // ── Dialog title ──────────────────────────────────────────────────
        Label dialogTitle = (Label) rootPane.lookup(".dialog-title");
        if (dialogTitle != null) {
            dialogTitle.styleProperty().bind(
                    Bindings.concat("-fx-font-size: ", titleSizeBinding, "px; -fx-font-weight: bold;"));
        }

        // ── Section headings ──────────────────────────────────────────────
        for (var node : rootPane.lookupAll(".section-heading")) {
            if (node instanceof Label heading) {
                heading.styleProperty().bind(
                        Bindings.concat("-fx-font-size: ", headingSizeBinding,
                                "px; -fx-font-weight: bold;"));
            }
        }

        // ── Buttons ───────────────────────────────────────────────────────
        for (var node : rootPane.lookupAll(".btn-primary")) {
            if (node instanceof Button btn) {
                btn.styleProperty().bind(
                        Bindings.concat("-fx-font-size: ", btnSizeBinding, "px;"));
            }
        }
        for (var node : rootPane.lookupAll(".btn-ghost")) {
            if (node instanceof Button btn) {
                btn.styleProperty().bind(
                        Bindings.concat("-fx-font-size: ", btnSizeBinding, "px;"));
            }
        }

        // ── Text fields ───────────────────────────────────────────────────
        for (var node : rootPane.lookupAll(".text-field")) {
            if (node instanceof TextField tf) {
                tf.styleProperty().bind(
                        Bindings.concat("-fx-font-size: ", bodySizeBinding, "px;",
                                " -fx-padding: ", fieldPadBinding, "px;"));
            }
        }

        // ── Panel spacing ─────────────────────────────────────────────────
        if (registerPanel != null) {
            registerPanel.spacingProperty().bind(panelSpaceBinding);

            // ── Panel padding ─────────────────────────────────────────────
            panelPadBinding.addListener((o, ov, nv) -> {
                double p = nv.doubleValue();
                registerPanel.setPadding(new Insets(p, p * 0.875, p, p * 0.875));
            });
            double p0 = panelPadBinding.get();
            registerPanel.setPadding(new Insets(p0, p0 * 0.875, p0, p0 * 0.875));
        }
    }

    /**
     * Handles the Register button click.
     *
     * <p>Validates all inputs, disables the button, and calls {@link Chess#register}.
     * The callback runs on the FX Application Thread; on success it calls
     * {@link PanelHost#closePanel()}, on failure it shows an error.</p>
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
                panelHost.closePanel();
            } else {
                showError(i18n.get("register.error.failed"));
            }
        });
    }

    /**
     * Handles the "Already have an account? Login" hyperlink click.
     * Transitions to the login panel within the main menu shell.
     */
    @FXML
    private void onLogin() {
        panelHost.switchPanel(PanelId.LOGIN);
    }

    /**
     * Handles the Cancel button click.
     * Closes the registration panel and returns to the clean main menu state.
     */
    @FXML
    private void onCancel() {
        panelHost.closePanel();
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
