package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.menu.ResponsiveMenuLayout;
import io.github.conava.chess.application.navigation.PanelHost;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the matchmaking waiting panel.
 *
 * <p>Shown after the user requests to find a match. Displays an indeterminate
 * spinner while the server searches for an opponent. When the server responds with
 * a {@code MATCHED} message the controller transitions to the game screen via a
 * full scene swap ({@link SceneManager#showGame(RulesetOptions)}). The user may
 * also cancel at any time, which leaves the matchmaking queue and closes the panel
 * via {@link PanelHost#closePanel()}.</p>
 *
 * <p>To avoid a race condition between handler registration and queue enrollment,
 * the {@code MATCHED} handler is registered in the constructor <em>before</em>
 * {@link Chess#joinMatchmakingQueue(RulesetOptions)} is called. The queue join is
 * therefore deferred to {@link #initialize()}, which runs after FXML injection but
 * before the scene becomes visible.</p>
 *
 * <h2>Responsive scaling</h2>
 * <p>Font sizes, panel padding, and panel spacing are bound to the stage width
 * via {@link ResponsiveMenuLayout} in {@link #initialize()}. A {@link SimpleDoubleProperty}
 * is used as the binding root so that bindings are active immediately at initialization
 * and then track the real stage width. Binding references are stored as instance fields
 * to prevent garbage collection from severing the reactive chain.</p>
 */
public class WaitingForMatchController {

    /** Default stage width used as initial value for responsive bindings before the stage is known. */
    private static final double DEFAULT_STAGE_WIDTH = 1280.0;

    private final SceneManager sceneManager;
    private final Chess chess;
    private final I18n i18n;
    private final RulesetOptions ruleset;
    private final String serverIp;
    private final int serverPort;
    private final PanelHost panelHost;

    @FXML
    private StackPane rootPane;
    @FXML
    private VBox waitingPanel;
    @FXML
    private Label searchingLabel;
    @FXML
    private ProgressIndicator spinner;
    @FXML
    private Label rulesetLabel;
    @FXML
    private Button cancelBtn;

    // ── Responsive binding infrastructure ───────────────────────────────────

    /**
     * Mutable width source for all responsive bindings. Initialized to a sensible default
     * and updated to the actual stage width once the panel is in the scene graph.
     */
    private final DoubleProperty stageWidthSource = new SimpleDoubleProperty(DEFAULT_STAGE_WIDTH);

    /** Dialog title font-size binding — stored to prevent GC. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding titleSizeBinding;

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

    /**
     * Constructs a {@code WaitingForMatchController} without a panel host.
     *
     * <p>This overload is provided for backward compatibility with callers
     * (e.g. {@link SceneManager#showWaitingForMatch}) that have not yet been
     * updated to supply a {@link PanelHost}. When no panel host is supplied, the
     * cancel action will be a no-op for panel navigation — the caller is
     * responsible for any resulting navigation.</p>
     *
     * @param sceneManager the navigation manager used to perform the full scene swap to the game screen.
     * @param chess        the application facade used for matchmaking and game lifecycle.
     * @param i18n         the internationalisation helper used for message lookup.
     * @param ruleset      the ruleset the player wants to use for the matched game.
     * @param serverIp     the server IP address or hostname.
     * @param serverPort   the server port number.
     */
    public WaitingForMatchController(SceneManager sceneManager, Chess chess, I18n i18n,
                                     RulesetOptions ruleset, String serverIp, int serverPort) {
        this(sceneManager, chess, i18n, ruleset, serverIp, serverPort, null);
    }

    /**
     * Constructs a {@code WaitingForMatchController} and immediately registers the
     * {@code MATCHED} message handler on the active server communication task.
     *
     * <p>The handler is registered here (rather than in {@link #initialize()}) so
     * that it is in place before {@link Chess#joinMatchmakingQueue(RulesetOptions)}
     * is called, eliminating the window where a fast server response could arrive
     * before the handler is set.</p>
     *
     * @param sceneManager the navigation manager used to perform the full scene swap to the game screen.
     * @param chess        the application facade used for matchmaking and game lifecycle.
     * @param i18n         the internationalisation helper used for message lookup.
     * @param ruleset      the ruleset the player wants to use for the matched game.
     * @param serverIp     the server IP address or hostname.
     * @param serverPort   the server port number.
     * @param panelHost    the panel host used to close the panel on cancel (may be {@code null} for
     *                     legacy callers; cancel will be a no-op for panel navigation in that case).
     */
    public WaitingForMatchController(SceneManager sceneManager, Chess chess, I18n i18n,
                                     RulesetOptions ruleset, String serverIp, int serverPort,
                                     PanelHost panelHost) {
        this.sceneManager = sceneManager;
        this.chess = chess;
        this.i18n = i18n;
        this.ruleset = ruleset;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.panelHost = panelHost;

        // Register the MATCHED handler before joining the queue to prevent a race condition.
        if (chess.getActiveServerTask() != null) {
            chess.getActiveServerTask().setMatchHandler(msg -> Platform.runLater(() -> onMatched(msg)));
        }
    }

    /**
     * Initialises the panel after FXML injection, joins the matchmaking queue,
     * and sets up responsive bindings.
     *
     * <p>The queue join is performed here (after the handler is already registered in
     * the constructor) to guarantee the handler is in place before the server can
     * respond.</p>
     */
    @FXML
    public void initialize() {
        if (rulesetLabel != null) {
            rulesetLabel.setText(ruleset.toString());
        }
        chess.joinMatchmakingQueue(ruleset);

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
        bodySizeBinding   = ResponsiveMenuLayout.panelBodyFontSize(stageWidthSource);
        btnSizeBinding    = ResponsiveMenuLayout.panelButtonFontSize(stageWidthSource);
        panelPadBinding   = ResponsiveMenuLayout.panelPadding(stageWidthSource);
        panelSpaceBinding = ResponsiveMenuLayout.panelSpacing(stageWidthSource);

        // ── Searching label (dialog title) ────────────────────────────────
        if (searchingLabel != null) {
            searchingLabel.styleProperty().bind(
                    Bindings.concat("-fx-font-size: ", titleSizeBinding, "px; -fx-font-weight: bold;"));
        }

        // ── Ruleset label (body text) ─────────────────────────────────────
        if (rulesetLabel != null) {
            rulesetLabel.styleProperty().bind(
                    Bindings.concat("-fx-font-size: ", bodySizeBinding, "px;"));
        }

        // ── Cancel button ─────────────────────────────────────────────────
        if (cancelBtn != null) {
            cancelBtn.styleProperty().bind(
                    Bindings.concat("-fx-font-size: ", btnSizeBinding, "px;"));
        }

        // ── Panel spacing ─────────────────────────────────────────────────
        if (waitingPanel != null) {
            waitingPanel.spacingProperty().bind(panelSpaceBinding);

            // ── Panel padding ─────────────────────────────────────────────
            // waiting panel has larger top/bottom (original ratio 48/28 ≈ 1.71x side padding)
            panelPadBinding.addListener((o, ov, nv) -> {
                double p = nv.doubleValue();
                waitingPanel.setPadding(new Insets(p * 1.5, p * 0.875, p * 1.5, p * 0.875));
            });
            double p0 = panelPadBinding.get();
            waitingPanel.setPadding(new Insets(p0 * 1.5, p0 * 0.875, p0 * 1.5, p0 * 0.875));
        }
    }

    /**
     * Handles a {@code MATCHED} server message.
     *
     * <p>Parses the join code from the message content, calls
     * {@link Chess#joinOnlineGame(String, int, String, RulesetOptions)} to start the
     * online game, and navigates to the game screen via a full scene swap. Always
     * called on the FX Application Thread.</p>
     *
     * @param msg the {@code MATCHED} message received from the server.
     */
    private void onMatched(Message msg) {
        String joinCode = msg.getParameterValue("joinCode");
        if (joinCode == null || joinCode.isEmpty()) {
            joinCode = msg.content();
        }
        chess.joinOnlineGame(serverIp, serverPort, joinCode, ruleset);
        sceneManager.showGame(ruleset);
    }

    /**
     * Handles the Cancel button action.
     *
     * <p>Sends a {@code DEQUEUE} message to leave the matchmaking queue, then
     * closes the panel via {@link PanelHost#closePanel()} to return to the clean
     * main menu state.</p>
     */
    @FXML
    private void onCancel() {
        try {
            chess.leaveMatchmakingQueue();
        } catch (IllegalStateException ignored) {
            // Connection may have dropped; safe to ignore here and return to menu.
        }
        if (panelHost != null) {
            panelHost.closePanel();
        }
    }
}
