package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.menu.CinematicBackground;
import io.github.conava.chess.application.menu.CinematicPanelAnimator;
import io.github.conava.chess.application.menu.MenuEntranceAnimation;
import io.github.conava.chess.application.menu.MenuExitTransition;
import io.github.conava.chess.application.menu.MenuLayoutTransition;
import io.github.conava.chess.application.menu.NavigationTarget;
import io.github.conava.chess.application.menu.ResponsiveMenuLayout;
import io.github.conava.chess.application.navigation.PanelHost;
import io.github.conava.chess.application.navigation.PanelId;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.application.theme.ThemeManager;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;

/**
 * Controller for the cinematic main menu screen and panel host for sub-screen panels.
 *
 * <p>Manages the foreground content of the main menu "shell": a title/tagline content
 * layer on the left, a right-aligned navigation panel defined in FXML, and a right-side
 * panel container that hosts sub-screen panels without a full scene swap.</p>
 *
 * <p>All background rendering (animated chessboard, particles, silhouettes) is handled by
 * {@link CinematicBackground}, which is managed by {@link SceneManager} as a
 * persistent layer shared across cinematic screens.</p>
 *
 * <p>Implements {@link PanelHost} so that sub-screen controllers can call back
 * into this controller to close or switch panels without a direct reference to
 * the concrete {@code MainMenuController} class.</p>
 *
 * <h2>Layout transition system</h2>
 * <p>When a panel opens, the nav panel slides left via {@code translateX} animation
 * and the content layer (title/tagline/accent) is animated in place via
 * {@code translateX}/{@code translateY}/{@code scaleX}/{@code scaleY} to a compact
 * position above the nav buttons. The content layer is never reparented — it always
 * stays in the root pane. After the animation completes, the nav panel's
 * {@code translateXProperty} and the content layer's translate/scale properties are
 * bound to reactive bindings from {@link ResponsiveMenuLayout} so that window resizes
 * keep everything correctly positioned without any snap. When the panel closes, all
 * properties animate back to their defaults (translate=0, scale=1.0). This is
 * coordinated by {@link MenuLayoutTransition}.</p>
 *
 * <h2>Responsive sizing</h2>
 * <p>All font sizes (title, tagline, nav buttons) and the panel container width are
 * computed from {@link ResponsiveMenuLayout} bindings that react to the root pane's
 * width/height. Inline {@code -fx-font-size} styles are applied via
 * {@code label.styleProperty().bind()} so they override the (now-removed) CSS
 * fixed sizes.</p>
 *
 * <h2>Panel lifecycle</h2>
 * <ul>
 *   <li>Nav button click calls {@link #showPanel(PanelId)}, which loads the FXML,
 *       adds it to {@code panelContainer}, and updates nav button styles.</li>
 *   <li>Sub-screen cancel/back calls {@link #closePanel()} through the
 *       {@link PanelHost} interface, which animates the panel out and restores
 *       the clean menu state.</li>
 *   <li>Auth-chained navigation (e.g. login to register) calls
 *       {@link #switchPanel(PanelId)}, which exits the current panel and enters
 *       the new one without returning to the clean menu state.</li>
 * </ul>
 *
 * <h2>Nav button dimming</h2>
 * <p>When a panel is open, non-active nav buttons receive
 * {@code cinematic-nav-dimmed} and the active button receives
 * {@code cinematic-nav-active}. All style classes are removed when the
 * panel is closed via {@link #restoreNavButtons()}.</p>
 */
public class MainMenuController implements PanelHost {

    // ── FXML-injected fields ────────────────────────────────────────────────

    /** Root pane of the main menu scene. Shared with the cinematic background layer. */
    @FXML
    private StackPane rootPane;

    /** Navigation button panel, right-aligned in the root pane. */
    @FXML
    private VBox navPanel;

    /** Local game navigation button. */
    @FXML
    private Button localGameBtn;

    /** Online game navigation button. */
    @FXML
    private Button onlineGameBtn;

    /** Settings navigation button. */
    @FXML
    private Button settingsBtn;

    /** Exit navigation button. */
    @FXML
    private Button exitBtn;

    /**
     * Right-side panel container. Hosts sub-screen panels (offline setup, settings,
     * login, etc.) in-place without a full scene swap. Width is bound responsively
     * via {@link ResponsiveMenuLayout#panelContainerWidth} in {@link #initialize()}.
     */
    @FXML
    private StackPane panelContainer;

    // ── Non-injected fields ─────────────────────────────────────────────────

    private final SceneManager sceneManager;
    private final Chess chess;
    private final I18n i18n;
    private final SettingsService settingsService;
    private final ThemeManager themeManager;

    /** Content layer containing the title, accent line, and tagline. Built in initialize(). */
    private VBox contentLayer;

    /** Kept as a field to prevent GC of the responsive margin binding. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding navMarginBinding;

    /** Kept as a field to prevent GC of the responsive left-padding binding. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding leftPaddingBinding;

    /** Kept as a field to prevent GC of the responsive nav panel padding binding. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding navPaddingBinding;

    /** Kept as a field to prevent GC of the responsive nav panel spacing binding. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding navSpacingBinding;

    /** Kept as a field to prevent GC of the responsive nav button icon size binding. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding navIconSizeBinding;

    /** Kept as a field to prevent GC of the responsive title font-size binding. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding titleFontSizeBinding;

    /** Kept as a field to prevent GC of the responsive tagline font-size binding. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding taglineFontSizeBinding;

    /** Kept as a field to prevent GC of the responsive nav-button font-size binding. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding navButtonFontSizeBinding;

    /** Kept as a field to prevent GC of the responsive nav-button min-height binding. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding navButtonMinHeightBinding;

    /** Kept as a field to prevent GC of the responsive accent-line max-width binding. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding accentWidthBinding;

    /** Kept as a field to prevent GC of the responsive panel-container width binding. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding panelContainerWidthBinding;

    /** Kept as a field to prevent GC of the responsive nav-panel width binding. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding navPanelWidthBinding;

    /** Kept as a field to prevent GC of the compact title scale binding. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding compactScaleBinding;

    /** Kept as a field to prevent GC of the compact title translateX binding. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding compactTranslateXBinding;

    /** Kept as a field to prevent GC of the compact title translateY binding. */
    @SuppressWarnings("FieldCanBeLocal")
    private DoubleBinding compactTranslateYBinding;

    /** Whether reduced-motion accessibility mode is active. */
    private boolean reducedMotion;

    /** Duration for the nav/title layout transition when panels open/close. */
    private static final Duration LAYOUT_TRANSITION_DURATION = Duration.millis(350);

    /**
     * Coordinates the translateX + scale animation when panels open/close.
     * Replaces the old StackPane.setAlignment()-based approach.
     * Created in {@link #initialize()} after the content layer is built.
     */
    private MenuLayoutTransition layoutTransition;

    /**
     * The currently active panel ID, or {@code null} when the menu is in its clean state.
     * Returned by {@link #getActivePanel()}.
     */
    private PanelId activePanel;

    /**
     * The currently displayed panel content node, or {@code null} when no panel is open.
     * Used to animate and remove the panel on close/switch.
     */
    private Node activePanelNode;

    // ── Constructor ─────────────────────────────────────────────────────────

    /**
     * Constructs a {@code MainMenuController}.
     *
     * @param sceneManager the navigation manager used to show the game screen
     *                     and access shared services
     * @param i18n         the internationalisation helper for message lookup
     */
    public MainMenuController(SceneManager sceneManager, I18n i18n) {
        this.sceneManager = sceneManager;
        this.chess = sceneManager.getChess();
        this.i18n = i18n;
        this.settingsService = sceneManager.getSettingsService();
        this.themeManager = sceneManager.getThemeManager();
    }

    // ── FXML initialisation ─────────────────────────────────────────────────

    /**
     * Initialises the menu after FXML injection.
     *
     * <p>Reads the reduced-motion setting, builds the title/tagline content layer,
     * inserts it at the top of the root StackPane z-order (above the nav panel), applies responsive
     * bindings for all font sizes and panel dimensions via
     * {@link ResponsiveMenuLayout}, creates the {@link MenuLayoutTransition}
     * instance, and plays the entrance animation.</p>
     *
     * <p>Background rendering is not managed here — it is handled by
     * {@link CinematicBackground} via {@link SceneManager}.</p>
     */
    @FXML
    public void initialize() {
        reducedMotion = settingsService.loadReducedMotion();

        // Build the title/tagline content layer (single persistent node — never replaced)
        contentLayer = buildContentLayer();

        // Insert content layer at the top of the z-order so the title renders above
        // the nav panel when it animates to the compact position.
        // mouseTransparent=true and pickOnBounds=false ensure it never blocks clicks.
        rootPane.getChildren().add(contentLayer);

        // ── Responsive bindings from ResponsiveMenuLayout ──────────────────
        titleFontSizeBinding = ResponsiveMenuLayout.titleFontSize(
                rootPane.widthProperty());
        taglineFontSizeBinding = ResponsiveMenuLayout.taglineFontSize(
                rootPane.widthProperty());
        navButtonFontSizeBinding = ResponsiveMenuLayout.navButtonFontSize(
                rootPane.widthProperty());
        navButtonMinHeightBinding = ResponsiveMenuLayout.navButtonMinHeight(
                rootPane.heightProperty());
        leftPaddingBinding = ResponsiveMenuLayout.contentLayerLeftPadding(
                rootPane.widthProperty());
        accentWidthBinding = ResponsiveMenuLayout.accentLineWidth(
                titleFontSizeBinding);
        panelContainerWidthBinding = ResponsiveMenuLayout.panelContainerWidth(
                rootPane.widthProperty());

        // ── Bind title font size via inline style ──────────────────────────
        Label title = lookupTitle();
        title.styleProperty().bind(
                Bindings.concat("-fx-font-size: ", titleFontSizeBinding, "px;"));

        // ── Bind tagline font size via inline style ────────────────────────
        Label tagline = lookupTagline();
        tagline.styleProperty().bind(
                Bindings.concat("-fx-font-size: ", taglineFontSizeBinding, "px;"));

        // ── Bind nav button font sizes and min-heights via inline style ────
        // Build a combined style string: font-size + min-height
        for (Button btn : getNavButtons()) {
            btn.styleProperty().bind(
                    Bindings.concat(
                            "-fx-font-size: ", navButtonFontSizeBinding, "px;",
                            " -fx-min-height: ", navButtonMinHeightBinding, "px;"));
        }

        // ── Bind nav button icon font sizes ────────────────────────────────
        // Each nav button has a chess-piece Label as its graphic. Bind its font-size
        // responsively so the icon scales with the window width, matching the button text.
        navIconSizeBinding = ResponsiveMenuLayout.navButtonIconSize(rootPane.widthProperty());
        for (Button btn : getNavButtons()) {
            Label icon = (Label) btn.getGraphic();
            if (icon != null) {
                icon.styleProperty().bind(
                        Bindings.concat("-fx-font-size: ", navIconSizeBinding, "px;"));
            }
        }

        // ── Bind nav panel padding and spacing ─────────────────────────────
        // Replaces the hardcoded spacing="8" and <padding> values removed from FXML.
        // Top/bottom padding is 1.5× side padding, matching the original 24/16 ratio.
        navSpacingBinding = ResponsiveMenuLayout.navPanelSpacing(rootPane.widthProperty());
        navPanel.spacingProperty().bind(navSpacingBinding);

        navPaddingBinding = ResponsiveMenuLayout.navPanelPadding(rootPane.widthProperty());
        navPaddingBinding.addListener((obs, oldVal, newVal) -> {
            double p = newVal.doubleValue();
            navPanel.setPadding(new Insets(p * 1.5, p, p * 1.5, p));
        });
        // Apply initial padding value synchronously so the first frame renders correctly
        double p0 = navPaddingBinding.get();
        navPanel.setPadding(new Insets(p0 * 1.5, p0, p0 * 1.5, p0));

        // ── Bind content layer left padding reactively ─────────────────────
        // The padding binding invalidates and re-applies whenever the binding changes.
        // No guard needed: contentLayer is never reparented in v2, so padding should
        // always be responsive. The translate+scale handles visual repositioning.
        leftPaddingBinding.addListener((obs, oldVal, newVal) ->
                contentLayer.setPadding(new Insets(0, 0, 0, newVal.doubleValue())));
        // Apply initial value
        contentLayer.setPadding(new Insets(0, 0, 0, leftPaddingBinding.get()));

        // ── Bind accent line max-width ─────────────────────────────────────
        Region accent = lookupAccent();
        accent.maxWidthProperty().bind(accentWidthBinding);

        // ── Bind panelContainer width responsively ─────────────────────────
        // Under the 25/75 split model, the panel container fills remaining space
        // after the nav panel (25%) and edge/gap padding.
        panelContainer.prefWidthProperty().bind(panelContainerWidthBinding);
        panelContainer.maxWidthProperty().bind(panelContainerWidthBinding);
        panelContainer.prefHeightProperty().bind(rootPane.heightProperty());
        // Constrain panel container height to prevent settings ScrollPane from clipping
        panelContainer.maxHeightProperty().bind(rootPane.heightProperty());

        // ── Bind navPanel to 25% of root width ──────────────────────────────
        // The nav panel always occupies exactly 25% of the root pane width, keeping
        // a consistent 25/75 split. All three constraints are bound so the StackPane
        // never overrides the intended width.
        navPanelWidthBinding = ResponsiveMenuLayout.navPanelWidth(
                rootPane.widthProperty());
        navPanel.prefWidthProperty().bind(navPanelWidthBinding);
        navPanel.maxWidthProperty().bind(navPanelWidthBinding);
        navPanel.minWidthProperty().bind(navPanelWidthBinding);

        // ── Bind nav panel right margin reactively ───────────────────────────
        // The FXML fixed margin is removed; the margin scales with window width
        // via the existing navPanelRightMargin binding.
        navMarginBinding = ResponsiveMenuLayout.navPanelRightMargin(
                rootPane.widthProperty());
        navMarginBinding.addListener((obs, oldVal, newVal) ->
                StackPane.setMargin(navPanel, new Insets(0, newVal.doubleValue(), 0, 0)));
        // Apply initial margin synchronously so the first frame renders correctly
        StackPane.setMargin(navPanel, new Insets(0, navMarginBinding.get(), 0, 0));

        // ── Compute nav translate target for MenuLayoutTransition ──────────
        // The nav panel slides left by (panelContainerWidth + NAV_PANEL_GAP) when open.
        // Pass navPanelWidthBinding (25% of root) instead of the measured navPanel.widthProperty()
        // so the translate target is reactive and consistent with the width binding.
        DoubleBinding navTranslateTargetBinding = ResponsiveMenuLayout.navPanelOpenTranslateX(
                rootPane.widthProperty(),
                panelContainerWidthBinding,
                navPanelWidthBinding);

        // ── Compact title bindings for translate+scale model ───────────────
        // These reactive bindings drive contentLayer.translateX/Y and scaleX/Y when
        // a panel is open. Stored as fields to prevent garbage collection.
        compactScaleBinding = ResponsiveMenuLayout.compactTitleScale(
                rootPane.widthProperty(), navPanelWidthBinding);

        compactTranslateXBinding = ResponsiveMenuLayout.compactTitleTranslateX(
                rootPane.widthProperty(),
                navPanelWidthBinding,
                navMarginBinding,
                navTranslateTargetBinding,
                leftPaddingBinding,
                contentLayer.widthProperty(),
                compactScaleBinding);

        compactTranslateYBinding = ResponsiveMenuLayout.compactTitleTranslateY(
                rootPane.heightProperty(),
                contentLayer.heightProperty(),
                compactScaleBinding);

        // ── Create MenuLayoutTransition ────────────────────────────────────
        // v2 translate+scale model: contentLayer stays in rootPane at all times.
        // The transition animates translateX/translateY/scaleX/scaleY on contentLayer
        // to move the title to a compact position above the nav buttons when open.
        layoutTransition = new MenuLayoutTransition(
                navPanel,
                contentLayer,
                navTranslateTargetBinding,
                compactTranslateXBinding,
                compactTranslateYBinding,
                compactScaleBinding,
                LAYOUT_TRANSITION_DURATION,
                reducedMotion);

        // ── Play entrance animation for nav buttons and content layer ──────
        MenuEntranceAnimation entrance = new MenuEntranceAnimation(
                title, accent, tagline, getNavButtons());
        if (reducedMotion) {
            entrance.skipToEnd();
        } else {
            entrance.play();
        }
    }

    // ── PanelHost implementation ────────────────────────────────────────────

    /**
     * Opens the specified panel inside the main menu's panel container.
     *
     * <p>If the same panel is already open (toggle behavior), calls {@link #closePanel()}.
     * If a different panel is already open, delegates to {@link #switchPanel(PanelId)}.</p>
     *
     * @param panelId the panel to open; must not be {@code null}
     */
    @Override
    public void showPanel(PanelId panelId) {
        if (activePanel != null) {
            if (activePanel == panelId) {
                // Toggle: clicking the same button again closes the panel
                closePanel();
            } else {
                switchPanel(panelId);
            }
            return;
        }

        // Load and display the panel
        Node panelContent = loadPanelFxml(panelId);
        if (panelContent == null) return;

        panelContainer.getChildren().add(panelContent);
        activePanelNode = panelContent;
        activePanel = panelId;

        // Animate panel entrance
        if (!reducedMotion) {
            CinematicPanelAnimator.playEntrance(panelContent);
        }

        // Animate layout transition (nav panel slides left, content layer scales down)
        animateLayoutForPanelOpen();

        // Update nav button styles
        dimNavButtons(panelId);
    }

    /**
     * Closes the currently active panel and returns the menu to its clean state.
     *
     * <p>In reduced-motion mode the panel is removed immediately without animation.
     * Otherwise, the exit animation plays before removing the panel node.</p>
     */
    @Override
    public void closePanel() {
        if (activePanel == null || activePanelNode == null) return;

        Node panelToClose = activePanelNode;
        activePanel = null;
        activePanelNode = null;

        restoreNavButtons();
        animateLayoutForPanelClose();

        if (reducedMotion) {
            panelContainer.getChildren().remove(panelToClose);
        } else {
            CinematicPanelAnimator.playExit(panelToClose, () ->
                    panelContainer.getChildren().remove(panelToClose));
        }
    }

    /**
     * Transitions from the currently active panel to the specified panel.
     *
     * <p>In reduced-motion mode the transition is immediate (exit and enter synchronously).
     * Otherwise, the exit animation plays first; when it completes, the new panel is
     * loaded and animated in.</p>
     *
     * @param panelId the panel to transition to; must not be {@code null}
     */
    @Override
    public void switchPanel(PanelId panelId) {
        Node panelToClose = activePanelNode;
        activePanel = null;
        activePanelNode = null;

        Runnable loadNewPanel = () -> {
            panelContainer.getChildren().clear();
            Node newContent = loadPanelFxml(panelId);
            if (newContent == null) return;

            panelContainer.getChildren().add(newContent);
            activePanelNode = newContent;
            activePanel = panelId;

            if (!reducedMotion) {
                CinematicPanelAnimator.playEntrance(newContent);
            }
            dimNavButtons(panelId);
        };

        if (panelToClose == null) {
            loadNewPanel.run();
            return;
        }

        if (reducedMotion) {
            panelContainer.getChildren().remove(panelToClose);
            loadNewPanel.run();
        } else {
            CinematicPanelAnimator.playExit(panelToClose, loadNewPanel);
        }
    }

    /**
     * Returns the {@link PanelId} of the currently open panel.
     *
     * @return the active panel identifier, or {@code null} if the menu is in its
     *         clean state (no panel open)
     */
    @Override
    public PanelId getActivePanel() {
        return activePanel;
    }

    /**
     * Transitions from the currently active panel to the matchmaking waiting panel,
     * constructing the {@link WaitingForMatchController} with the required server
     * connection parameters.
     *
     * <p>This method is the correct entry point for navigating to
     * {@link PanelId#WAITING_FOR_MATCH} because that panel requires an active
     * server connection whose parameters are only known at the moment the online
     * setup form is submitted. Using {@link #switchPanel(PanelId)} for this
     * transition would fail because {@link #createPanelController} cannot build
     * the controller without those parameters.</p>
     *
     * @param ruleset    the ruleset the player wants to use for the matched game
     * @param serverIp   the server IP address or hostname
     * @param serverPort the server port number (1–65535)
     */
    @Override
    public void showWaitingForMatch(RulesetOptions ruleset, String serverIp, int serverPort) {
        Node panelToClose = activePanelNode;
        activePanel = null;
        activePanelNode = null;

        Runnable loadWaiting = () -> {
            panelContainer.getChildren().clear();

            WaitingForMatchController controller =
                    new WaitingForMatchController(sceneManager, chess, i18n, ruleset, serverIp, serverPort, this);

            String fxmlPath = "/fxml/waiting-for-match.fxml";
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                        getClass().getResource(fxmlPath), i18n.getBundle());
                loader.setControllerFactory(type -> controller);
                Node newContent = loader.load();

                panelContainer.getChildren().add(newContent);
                activePanelNode = newContent;
                activePanel = PanelId.WAITING_FOR_MATCH;

                if (!reducedMotion) {
                    CinematicPanelAnimator.playEntrance(newContent);
                }
                dimNavButtons(PanelId.WAITING_FOR_MATCH);
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to load waiting-for-match panel", e);
            }
        };

        if (panelToClose == null) {
            loadWaiting.run();
            return;
        }

        if (reducedMotion) {
            panelContainer.getChildren().remove(panelToClose);
            loadWaiting.run();
        } else {
            CinematicPanelAnimator.playExit(panelToClose, loadWaiting);
        }
    }

    // ── Navigation handlers ─────────────────────────────────────────────────

    /**
     * Handles the Local Game button.
     *
     * <p>Opens the offline setup panel in the panel container instead of
     * performing a full scene swap.</p>
     */
    @FXML
    private void onLocalGame() {
        showPanel(PanelId.OFFLINE_SETUP);
    }

    /**
     * Handles the Online Game button.
     *
     * <p>Auth gate: unauthenticated users see the login panel. Authenticated
     * users see the online setup panel.</p>
     */
    @FXML
    private void onOnlineGame() {
        if (!chess.isAuthenticated()) {
            showPanel(PanelId.LOGIN);
        } else {
            showPanel(PanelId.ONLINE_SETUP);
        }
    }

    /**
     * Handles the Settings button.
     *
     * <p>Opens the settings panel in the panel container.</p>
     */
    @FXML
    private void onSettings() {
        showPanel(PanelId.SETTINGS);
    }

    /**
     * Handles the Exit button.
     *
     * <p>Closes any open panel immediately (using the layout transition's
     * skipToClose to avoid visual artifacts), then wraps the stage close in a
     * {@link MenuExitTransition}.</p>
     */
    @FXML
    private void onExit() {
        // Reset layout and panel state if a panel is open
        if (activePanel != null) {
            panelContainer.getChildren().clear();
            activePanel = null;
            activePanelNode = null;
            restoreNavButtons();
        }
        // Use skipToClose to instantly reset the layout transition state
        // without animation (exit animation handles the visual departure).
        // No padding restore needed: contentLayer is never reparented in v2.
        if (layoutTransition != null && layoutTransition.isOpen()) {
            layoutTransition.skipToClose();
        }

        playExitTransition(NavigationTarget.EXIT, () -> {
            Stage stage = (Stage) exitBtn.getScene().getWindow();
            stage.close();
        });
    }

    // ── Layout transitions (nav + title movement) ──────────────────────────

    /**
     * Animates the layout into the "panel open" state using {@link MenuLayoutTransition}.
     *
     * <p>The nav panel slides left via {@code translateX} while the content layer is
     * simultaneously translated and scaled to a compact position above the nav buttons.
     * After animation, the nav panel and content layer properties are bound reactively
     * so resize events keep everything correctly positioned without any snap.</p>
     */
    private void animateLayoutForPanelOpen() {
        if (layoutTransition == null || layoutTransition.isOpen()) return;
        if (reducedMotion) {
            layoutTransition.skipToOpen();
        } else {
            layoutTransition.animateOpen(null);
        }
    }

    /**
     * Animates the layout back to the "panel closed" state using {@link MenuLayoutTransition}.
     *
     * <p>The content layer is translated and scaled back to its default (zero translate,
     * scale 1.0) position while the nav panel slides right to its original position.
     * No padding restore is needed because contentLayer is never reparented in v2.</p>
     */
    private void animateLayoutForPanelClose() {
        if (layoutTransition == null || !layoutTransition.isOpen()) return;
        if (reducedMotion) {
            layoutTransition.skipToClose();
        } else {
            layoutTransition.animateClose(null);
        }
    }

    // ── Nav button state management ─────────────────────────────────────────

    /**
     * Dims all nav buttons except the one corresponding to the active panel.
     *
     * <p>Button-to-panel mapping:</p>
     * <ul>
     *   <li>{@link PanelId#OFFLINE_SETUP} → {@code localGameBtn} (active)</li>
     *   <li>{@link PanelId#ONLINE_SETUP}, {@link PanelId#LOGIN},
     *       {@link PanelId#REGISTER}, {@link PanelId#WAITING_FOR_MATCH}
     *       → {@code onlineGameBtn} (active)</li>
     *   <li>{@link PanelId#SETTINGS} → {@code settingsBtn} (active)</li>
     * </ul>
     *
     * @param activeId the panel that is currently being shown
     */
    private void dimNavButtons(PanelId activeId) {
        Button activeBtn = resolveNavButton(activeId);

        for (Button btn : List.of(localGameBtn, onlineGameBtn, settingsBtn)) {
            btn.getStyleClass().removeAll("cinematic-nav-dimmed", "cinematic-nav-active");
            if (btn == activeBtn) {
                btn.getStyleClass().add("cinematic-nav-active");
            } else {
                btn.getStyleClass().add("cinematic-nav-dimmed");
            }
        }
    }

    /**
     * Removes all dimming and active style classes from nav buttons,
     * restoring the clean menu state.
     */
    private void restoreNavButtons() {
        for (Button btn : List.of(localGameBtn, onlineGameBtn, settingsBtn)) {
            btn.getStyleClass().remove("cinematic-nav-dimmed");
            btn.getStyleClass().remove("cinematic-nav-active");
        }
    }

    /**
     * Maps a {@link PanelId} to the nav button that should be highlighted.
     *
     * @param panelId the panel ID to map
     * @return the nav button corresponding to the panel
     */
    private Button resolveNavButton(PanelId panelId) {
        return switch (panelId) {
            case OFFLINE_SETUP -> localGameBtn;
            case ONLINE_SETUP, LOGIN, REGISTER, WAITING_FOR_MATCH -> onlineGameBtn;
            case SETTINGS -> settingsBtn;
        };
    }

    // ── Panel FXML loading ──────────────────────────────────────────────────

    /**
     * Maps a {@link PanelId} to the FXML resource path for the panel.
     *
     * @param panelId the panel to resolve
     * @return the FXML resource path string
     */
    private String resolveFxmlPath(PanelId panelId) {
        return switch (panelId) {
            case OFFLINE_SETUP -> "/fxml/offline-setup.fxml";
            case ONLINE_SETUP -> "/fxml/online-setup.fxml";
            case SETTINGS -> "/fxml/settings.fxml";
            case LOGIN -> "/fxml/login.fxml";
            case REGISTER -> "/fxml/register.fxml";
            case WAITING_FOR_MATCH -> "/fxml/waiting-for-match.fxml";
        };
    }

    /**
     * Creates the controller for the given panel, passing {@code this} as the
     * {@link PanelHost} so that sub-screen controllers can call back for navigation.
     *
     * <p>{@link PanelId#WAITING_FOR_MATCH} returns {@code null} because that panel
     * requires an active server connection and is loaded by the online setup controller
     * via {@link #switchPanel(PanelId)} after the connection is established.</p>
     *
     * @param panelId the panel whose controller to create
     * @return the controller instance, or {@code null} if the panel cannot be loaded here
     */
    private Object createPanelController(PanelId panelId) {
        return switch (panelId) {
            case OFFLINE_SETUP ->
                    new OfflineSetupController(sceneManager, i18n, this);
            case ONLINE_SETUP ->
                    new OnlineSetupController(sceneManager, chess, i18n, settingsService, this);
            case SETTINGS ->
                    new SettingsController(sceneManager, themeManager, i18n, settingsService, this, chess);
            case LOGIN ->
                    new LoginController(sceneManager, chess, i18n, this, settingsService);
            case REGISTER ->
                    new RegisterController(sceneManager, chess, i18n, this, settingsService);
            case WAITING_FOR_MATCH ->
                    // Loaded externally after a server connection is established;
                    // OnlineSetupController calls switchPanel(WAITING_FOR_MATCH) which
                    // should be intercepted and handled differently. For now, return null.
                    null;
        };
    }

    /**
     * Loads the FXML for the given panel and returns the root node.
     *
     * <p>The controller is created via {@link #createPanelController(PanelId)} and
     * injected via the FXMLLoader's controller factory, following the same pattern
     * as {@link SceneManager#loadFxml}.</p>
     *
     * @param panelId the panel to load
     * @return the loaded root {@link Node}, or {@code null} on failure
     */
    private Node loadPanelFxml(PanelId panelId) {
        Object controller = createPanelController(panelId);
        if (controller == null) return null;

        String fxmlPath = resolveFxmlPath(panelId);
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath), i18n.getBundle());
            loader.setControllerFactory(type -> controller);
            Parent root = loader.load();
            return root;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load panel FXML: " + fxmlPath, e);
        }
    }

    // ── Exit transition helper ──────────────────────────────────────────────

    /**
     * Wraps a navigation action in a {@link MenuExitTransition}.
     *
     * <p>The {@link CinematicBackground} is accelerated during the transition to
     * create a sense of forward momentum, then reset before navigation occurs.</p>
     *
     * <p>The exit transition uses {@code setByX} (relative) for the nav panel slide,
     * so it works correctly regardless of the current {@code translateX} value
     * (whether a panel is open or not when exit is triggered).</p>
     *
     * @param target             the navigation target for sound hooks
     * @param navigationCallback the action to perform after the transition
     */
    private void playExitTransition(NavigationTarget target, Runnable navigationCallback) {
        Runnable wrappedCallback = () -> {
            // Reset acceleration before navigating
            CinematicBackground bg = sceneManager.getCinematicBackground();
            if (bg != null) {
                bg.setAccelerated(false);
            }
            navigationCallback.run();
        };

        // Accelerate the cinematic background during the exit transition
        CinematicBackground bg = sceneManager.getCinematicBackground();
        Runnable accelerateAll = bg != null ? () -> bg.setAccelerated(true) : null;

        MenuExitTransition exit = new MenuExitTransition(
                navPanel, contentLayer, wrappedCallback, target,
                accelerateAll, null, null);
        exit.play();
    }

    // ── Content layer helpers ───────────────────────────────────────────────

    /**
     * Builds the content layer containing the title, accent line, and tagline.
     *
     * <p>This is the single persistent node that always stays in the root pane. When
     * a panel opens, {@link MenuLayoutTransition} animates its {@code translateX},
     * {@code translateY}, {@code scaleX}, and {@code scaleY} to a compact position
     * above the nav buttons. It is never replaced and never reparented.</p>
     *
     * <p>Initial padding and accent width are set here as defaults; they are
     * overridden by responsive bindings applied in {@link #initialize()} after
     * this method returns.</p>
     *
     * @return a VBox positioned on the left side of the screen
     */
    private VBox buildContentLayer() {
        Label title = new Label(i18n.get("menu.title"));
        title.getStyleClass().add("cinematic-title");

        Region accent = new Region();
        accent.getStyleClass().add("cinematic-accent-line");
        accent.setPrefHeight(2);
        accent.setMaxWidth(72);  // overridden by accentWidthBinding in initialize()

        Label tagline = new Label(i18n.get("menu.tagline"));
        tagline.getStyleClass().add("cinematic-tagline");

        VBox content = new VBox(6, title, accent, tagline);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(0, 0, 0, 72));  // overridden by leftPaddingBinding
        content.setMouseTransparent(true);
        content.setPickOnBounds(false);
        StackPane.setAlignment(content, Pos.CENTER_LEFT);

        return content;
    }

    /**
     * Returns the title label from the content layer.
     *
     * @return the title {@link Label}
     */
    private Label lookupTitle() {
        return (Label) contentLayer.getChildren().get(0);
    }

    /**
     * Returns the accent region from the content layer.
     *
     * @return the accent {@link Region}
     */
    private Region lookupAccent() {
        return (Region) contentLayer.getChildren().get(1);
    }

    /**
     * Returns the tagline label from the content layer.
     *
     * @return the tagline {@link Label}
     */
    private Label lookupTagline() {
        return (Label) contentLayer.getChildren().get(2);
    }

    /**
     * Returns the navigation buttons in display order.
     *
     * @return an unmodifiable list of navigation buttons
     */
    private List<Button> getNavButtons() {
        return List.of(localGameBtn, onlineGameBtn, settingsBtn, exitBtn);
    }
}
