package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.menu.ResponsiveMenuLayout;
import io.github.conava.chess.application.navigation.PanelHost;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Controller for the offline (local) game setup panel.
 *
 * <p>Collects player names and ruleset selection, then starts a local game via
 * {@link SceneManager#getChess()} and navigates to the game screen.</p>
 *
 * <p>The panel uses a cinematic frosted-glass layout hosted inside the main menu
 * shell's panel container. Cancel navigation is delegated to {@link PanelHost}
 * so that the main menu shell can animate the panel out and restore its clean
 * state. Game start still performs a full scene swap via {@link SceneManager}.</p>
 *
 * <h2>Responsive scaling</h2>
 * <p>Font sizes, panel padding, and panel spacing are bound to the stage width
 * via {@link ResponsiveMenuLayout} in {@link #initialize()}. A {@link SimpleDoubleProperty}
 * ({@code stageWidthSource}) is used as the binding root so that bindings are
 * established immediately during initialization (using a default 1280px value) and
 * then updated as soon as the real stage width is available. Binding references are
 * stored as instance fields to prevent garbage collection from severing the reactive
 * chain.</p>
 */
public class OfflineSetupController {

    /** Default stage width used as initial value for responsive bindings before the stage is known. */
    private static final double DEFAULT_STAGE_WIDTH = 1280.0;

    private final SceneManager sceneManager;
    private final I18n i18n;
    private final PanelHost panelHost;

    @FXML
    private StackPane rootPane;
    @FXML
    private VBox setupPanel;
    @FXML
    private TextField whiteField;
    @FXML
    private TextField blackField;
    @FXML
    private ComboBox<RulesetOptions> rulesetBox;

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
     * Constructs an {@code OfflineSetupController}.
     *
     * @param sceneManager the navigation manager used to show other screens and access the Chess facade.
     * @param i18n         the internationalisation helper used for default player name lookup.
     * @param panelHost    the panel host used to close this panel when cancel is pressed.
     */
    public OfflineSetupController(SceneManager sceneManager, I18n i18n, PanelHost panelHost) {
        this.sceneManager = sceneManager;
        this.i18n = i18n;
        this.panelHost = panelHost;
    }

    /**
     * Initialises the panel after FXML injection.
     *
     * <p>Populates the ruleset combo box, wires responsive bindings to
     * {@link #stageWidthSource}, and attaches a scene listener to sync
     * {@code stageWidthSource} with the real stage width once available.
     * Player name fields start empty; the user fills them in before starting a game.</p>
     */
    @FXML
    public void initialize() {
        rulesetBox.setItems(FXCollections.observableArrayList(RulesetOptions.values()));
        rulesetBox.getSelectionModel().selectFirst();
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
        for (TextField tf : List.of(whiteField, blackField)) {
            tf.styleProperty().bind(
                    Bindings.concat("-fx-font-size: ", bodySizeBinding, "px;",
                            " -fx-padding: ", fieldPadBinding, "px;"));
        }

        // ── ComboBox ──────────────────────────────────────────────────────
        rulesetBox.styleProperty().bind(
                Bindings.concat("-fx-font-size: ", bodySizeBinding, "px;"));

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
     * Handles the Start Game button.
     *
     * <p>Fills blank name fields with i18n defaults, then starts the game via the
     * Chess facade and navigates to the game screen. No panel exit animation is
     * started here — the scene swap acts as the visual transition.</p>
     */
    @FXML
    private void onStart() {
        if (whiteField.getText().isBlank()) whiteField.setText(i18n.get("dialog.offline.default.white"));
        if (blackField.getText().isBlank()) blackField.setText(i18n.get("dialog.offline.default.black"));
        RulesetOptions ruleset = rulesetBox.getValue();

        sceneManager.getChess().startGame(false, ruleset,
                whiteField.getText().trim(), blackField.getText().trim(), null);
        sceneManager.showGame(ruleset);
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
}
