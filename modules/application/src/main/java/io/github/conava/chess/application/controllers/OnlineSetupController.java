package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.menu.CinematicPanelAnimator;
import io.github.conava.chess.application.menu.ResponsiveMenuLayout;
import io.github.conava.chess.application.navigation.PanelHost;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Map;

/**
 * Controller for the online game setup panel.
 *
 * <p>Collects server connection details, game mode (create / join / find match),
 * and ruleset selection, then starts an online game or transitions to the
 * matchmaking waiting panel.</p>
 *
 * <p>The panel uses a cinematic frosted-glass layout hosted inside the main menu
 * shell's panel container. Cancel navigation and panel transitions are delegated
 * to {@link PanelHost}. Game start still performs a full scene swap via
 * {@link SceneManager}. The find-match path calls
 * {@link PanelHost#switchPanel(PanelId)} with {@link PanelId#WAITING_FOR_MATCH}.</p>
 *
 * <h2>Responsive scaling</h2>
 * <p>Font sizes, panel padding, and panel spacing are bound to the stage width
 * via {@link ResponsiveMenuLayout} in {@link #initialize()}. A {@link SimpleDoubleProperty}
 * is used as the binding root so that bindings are active immediately at initialization
 * and then track the real stage width. Binding references are stored as instance fields
 * to prevent garbage collection from severing the reactive chain.</p>
 */
public class OnlineSetupController {

    /** Default stage width used as initial value for responsive bindings before the stage is known. */
    private static final double DEFAULT_STAGE_WIDTH = 1280.0;

    private final SceneManager sceneManager;
    private final Chess chess;
    private final I18n i18n;
    private final SettingsService settingsService;
    private final PanelHost panelHost;

    @FXML
    private StackPane rootPane;
    @FXML
    private VBox setupPanel;
    @FXML
    private ToggleButton createToggle;
    @FXML
    private ToggleButton joinToggle;
    @FXML
    private ToggleButton findMatchToggle;
    @FXML
    private ToggleGroup modeGroup;
    @FXML
    private TextField ipField;
    @FXML
    private TextField portField;
    @FXML
    private TextField joinCodeField;
    @FXML
    private Label joinCodeLabel;
    @FXML
    private ComboBox<RulesetOptions> rulesetBox;
    @FXML
    private Label rulesetLabel;
    @FXML
    private Label errorLabel;
    @FXML
    private Button connectButton;

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
     * Constructs an {@code OnlineSetupController}.
     *
     * @param sceneManager    the navigation manager used to show the game screen.
     * @param chess           the Chess facade used to start games and connect to servers.
     * @param i18n            the internationalisation helper for player name lookup and button labels.
     * @param settingsService the settings service used to load saved server defaults.
     * @param panelHost       the panel host used for cancel and panel-to-panel navigation.
     */
    public OnlineSetupController(SceneManager sceneManager, Chess chess, I18n i18n,
                                 SettingsService settingsService, PanelHost panelHost) {
        this.sceneManager = sceneManager;
        this.chess = chess;
        this.i18n = i18n;
        this.settingsService = settingsService;
        this.panelHost = panelHost;
    }

    /**
     * Initialises the panel after FXML injection.
     *
     * <p>Populates the ruleset combo box, sets up toggle visibility listeners,
     * pre-fills IP and port from saved settings if available, wires responsive
     * bindings to {@link #stageWidthSource}, and attaches a scene listener to
     * sync {@code stageWidthSource} with the real stage width once available.</p>
     */
    @FXML
    public void initialize() {
        rulesetBox.setItems(FXCollections.observableArrayList(RulesetOptions.values()));
        rulesetBox.getSelectionModel().selectFirst();

        updateJoinCodeVisibility();
        modeGroup.selectedToggleProperty().addListener((o, old, sel) -> updateJoinCodeVisibility());

        // Pre-fill IP and port from settings if available
        String savedHost = settingsService.loadServerHost();
        if (savedHost != null && !savedHost.isEmpty()) {
            ipField.setText(savedHost);
        }
        int savedPort = settingsService.loadServerPort();
        if (savedPort > 0) {
            portField.setText(String.valueOf(savedPort));
        }

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
     * Updates visibility of join-code and ruleset fields based on the selected
     * game mode toggle.
     */
    private void updateJoinCodeVisibility() {
        boolean joining = joinToggle.isSelected();
        boolean findingMatch = findMatchToggle.isSelected();

        joinCodeField.setVisible(joining);
        joinCodeField.setManaged(joining);
        joinCodeLabel.setVisible(joining);
        joinCodeLabel.setManaged(joining);

        // Show ruleset picker for create and find-match modes, hide for join
        boolean showRuleset = !joining;
        rulesetBox.setVisible(showRuleset);
        rulesetBox.setManaged(showRuleset);
        rulesetLabel.setVisible(showRuleset);
        rulesetLabel.setManaged(showRuleset);

        // Update connect button text for find-match mode
        if (connectButton != null) {
            if (findingMatch) {
                connectButton.setText(i18n.get("online.findMatch"));
            } else {
                connectButton.setText(i18n.get("dialog.online.start"));
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

        // ── Buttons (btn-primary and btn-ghost) ───────────────────────────
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

        // ── ToggleButtons ─────────────────────────────────────────────────
        for (var node : rootPane.lookupAll(".toggle-button")) {
            if (node instanceof ToggleButton tb) {
                tb.styleProperty().bind(
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

        // ── ComboBox ──────────────────────────────────────────────────────
        if (rulesetBox != null) {
            rulesetBox.styleProperty().bind(
                    Bindings.concat("-fx-font-size: ", bodySizeBinding, "px;"));
        }

        // ── Panel spacing ─────────────────────────────────────────────────
        setupPanel.spacingProperty().bind(panelSpaceBinding);

        // ── Panel padding ─────────────────────────────────────────────────
        panelPadBinding.addListener((o, ov, nv) -> {
            double p = nv.doubleValue();
            setupPanel.setPadding(new Insets(p, p * 0.875, p, p * 0.875));
        });
        double p0 = panelPadBinding.get();
        setupPanel.setPadding(new Insets(p0, p0 * 0.875, p0, p0 * 0.875));
    }

    /**
     * Handles the Connect / Find Match button.
     *
     * <p>Validates IP, port, and (for join mode) join code. On successful
     * validation, plays an exit animation and then either starts a game
     * directly (create/join modes) or transitions to the matchmaking waiting
     * panel (find-match mode) via {@link PanelHost#switchPanel(PanelId)}.</p>
     */
    @FXML
    private void onConnect() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        String ip = ipField.getText().trim();
        String port = portField.getText().trim();

        if (!isValidIp(ip)) {
            showError(i18n.get("dialog.online.error.invalid_ip"));
            return;
        }
        if (!isValidPort(port)) {
            showError(i18n.get("dialog.online.error.invalid_port"));
            return;
        }

        if (joinToggle.isSelected()) {
            String joinCode = joinCodeField.getText().trim();
            if (joinCode.isEmpty()) {
                showError(i18n.get("dialog.online.error.empty_join_code"));
                return;
            }
            // JOIN mode — full scene swap to game
            CinematicPanelAnimator.playExit(setupPanel, () -> {
                chess.startGame(true, rulesetBox.getValue(),
                        i18n.get("dialog.online.player.opponent"),
                        i18n.get("dialog.online.player.you"),
                        Map.of("ip", ip, "port", port, "joinCode", joinCode));
                sceneManager.showGame(rulesetBox.getValue());
            });
        } else if (findMatchToggle.isSelected()) {
            // FIND_MATCH mode — establish server connection, then transition to the waiting panel.
            int portNum = Integer.parseInt(port);
            if (!chess.connectToServer(ip, portNum)) {
                showError(i18n.get("dialog.online.error.connection_failed"));
                return;
            }
            RulesetOptions ruleset = rulesetBox.getValue();
            panelHost.showWaitingForMatch(ruleset, ip, portNum);
        } else {
            // CREATE mode — full scene swap to game
            CinematicPanelAnimator.playExit(setupPanel, () -> {
                chess.startGame(true, rulesetBox.getValue(),
                        i18n.get("dialog.online.player.you"),
                        i18n.get("dialog.online.player.opponent"),
                        Map.of("ip", ip, "port", port, "joinCode", ""));
                sceneManager.showGame(rulesetBox.getValue());
            });
        }
    }

    /**
     * Handles the Cancel button — delegates to {@link PanelHost#closePanel()} to
     * return to the clean main menu state. The exit animation is played by
     * {@link MainMenuController#closePanel()}, so no animation is started here.
     */
    @FXML
    private void onCancel() {
        panelHost.closePanel();
    }

    /**
     * Displays an error message below the form fields.
     *
     * @param msg the error message to display.
     */
    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    /**
     * Validates an IP address string.
     *
     * <p>Accepts {@code "localhost"}, dotted-quad IPv4 addresses, and IPv6
     * addresses (detected by the presence of a colon).</p>
     *
     * @param ip the IP address string to validate.
     * @return {@code true} if the IP is valid.
     */
    private boolean isValidIp(String ip) {
        return ip.equals("localhost") || ip.matches("(\\d{1,3}\\.){3}\\d{1,3}") || ip.contains(":");
    }

    /**
     * Validates a port number string.
     *
     * @param port the port string to validate.
     * @return {@code true} if the port is a valid integer between 1 and 65535.
     */
    private boolean isValidPort(String port) {
        try {
            int p = Integer.parseInt(port);
            return p >= 1 && p <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
