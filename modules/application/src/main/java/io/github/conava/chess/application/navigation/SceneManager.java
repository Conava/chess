package io.github.conava.chess.application.navigation;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.controllers.GameController;
import io.github.conava.chess.application.controllers.MainMenuController;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.menu.CinematicBackground;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.application.theme.ThemeManager;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Set;

/**
 * Manages scene navigation for the chess application.
 *
 * <p>Supports two categories of screens:
 * <ul>
 *   <li><strong>Cinematic screen</strong> (main menu only) — shares a persistent
 *       {@link CinematicBackground} layer that renders floating particles and drifting
 *       silhouettes behind the foreground FXML content. Sub-screens such as offline setup,
 *       online setup, settings, login, register, and waiting-for-match are loaded as
 *       panels <em>inside</em> the main menu shell by {@link MainMenuController} and never
 *       replace the main menu scene.</li>
 *   <li><strong>Plain screen</strong> (game) — displays only the FXML content with no
 *       background animation. The cinematic background is paused and removed from the scene
 *       graph while the game screen is active.</li>
 * </ul>
 *
 * <p>Stage dimensions are set only on the <strong>first</strong> call to any show method.
 * Subsequent scene swaps (e.g., returning from game to main menu) preserve the current
 * stage dimensions so that user window resizes are not lost.</p>
 *
 * <p>The cinematic background is created lazily on the first cinematic screen and reused
 * across subsequent cinematic navigations. Foreground content is tracked by identity
 * (not index) to avoid misaligned child removal when the StackPane contains varying
 * numbers of children.</p>
 */
public class SceneManager {

    private static final String FXML_MAIN_MENU = "/fxml/main-menu.fxml";
    private static final String FXML_GAME = "/fxml/game.fxml";

    /**
     * FXML paths that use the shared cinematic background layer.
     *
     * <p>Only the main menu is a cinematic scene. Sub-screens (offline setup, online setup,
     * settings, login, register, waiting-for-match) are now loaded as panels inside the main
     * menu shell by {@link MainMenuController} and do not get their own scenes.</p>
     */
    private static final Set<String> CINEMATIC_SCREENS = Set.of(FXML_MAIN_MENU);

    private final Stage primaryStage;
    private final Chess chess;
    private final ThemeManager themeManager;
    private final I18n i18n;
    private final SettingsService settingsService;

    private StackPane rootStack;
    private OverlayManager overlayManager;

    /** Persistent cinematic background (created lazily on first cinematic screen). */
    private CinematicBackground cinematicBackground;

    /** The current foreground content node, tracked for identity-based removal. */
    private Parent currentContent;

    public SceneManager(Stage primaryStage, Chess chess, ThemeManager themeManager,
                        I18n i18n, SettingsService settingsService) {
        this.primaryStage = primaryStage;
        this.chess = chess;
        this.themeManager = themeManager;
        this.i18n = i18n;
        this.settingsService = settingsService;
    }

    public void showMainMenu() {
        var controller = new MainMenuController(this, i18n);
        swapScene(FXML_MAIN_MENU, controller, 1100, 780);
        primaryStage.setMaximized(false);
    }

    /**
     * No-op stub kept for source compatibility with tests that verify this method is
     * never called. Navigation to offline setup is now handled by {@link MainMenuController}
     * via panel hosting ({@link PanelId#OFFLINE_SETUP}).
     *
     * @deprecated Navigation is fully delegated to {@link MainMenuController}. This method
     *             will be removed once all callers are eliminated.
     */
    @Deprecated
    public void showLocalSetup() {
        // No-op — MainMenuController handles offline setup via panel hosting
    }

    /**
     * No-op stub kept for source compatibility. Navigation to online setup is now handled
     * by {@link MainMenuController} via panel hosting ({@link PanelId#ONLINE_SETUP}).
     *
     * @deprecated Navigation is fully delegated to {@link MainMenuController}.
     */
    @Deprecated
    public void showOnlineSetup() {
        // No-op — MainMenuController handles online setup via panel hosting
    }

    public void showGame(RulesetOptions ruleset) {
        var controller = new GameController(this, chess, i18n, ruleset);
        swapScene(FXML_GAME, controller, 1280, 860);
        primaryStage.setMaximized(true);
    }

    /**
     * No-op stub. Navigation to settings is now handled by {@link MainMenuController}
     * via panel hosting ({@link PanelId#SETTINGS}).
     *
     * @deprecated Navigation is fully delegated to {@link MainMenuController}.
     */
    @Deprecated
    public void showSettings() {
        // No-op — MainMenuController handles settings via panel hosting
    }

    /**
     * No-op stub kept for source compatibility with tests that verify this method is
     * never called. Navigation to login is now handled by {@link MainMenuController}
     * via panel hosting ({@link PanelId#LOGIN}).
     *
     * @deprecated Navigation is fully delegated to {@link MainMenuController}.
     */
    @Deprecated
    public void showLogin() {
        // No-op — MainMenuController handles login via panel hosting
    }

    /**
     * No-op stub. Navigation to registration is now handled by {@link MainMenuController}
     * via panel hosting ({@link PanelId#REGISTER}).
     *
     * @deprecated Navigation is fully delegated to {@link MainMenuController}.
     */
    @Deprecated
    public void showRegister() {
        // No-op — MainMenuController handles registration via panel hosting
    }

    /**
     * No-op stub. Navigation to the matchmaking waiting screen is now handled by
     * {@link MainMenuController} via panel hosting ({@link PanelId#WAITING_FOR_MATCH}).
     *
     * <p>Previously this method established a server connection before navigating.
     * That responsibility now belongs to {@link io.github.conava.chess.application.controllers.OnlineSetupController}
     * before calling {@code panelHost.switchPanel(PanelId.WAITING_FOR_MATCH)}.</p>
     *
     * @param ruleset the ruleset (ignored — kept for source compatibility)
     * @param ip      the server IP (ignored — kept for source compatibility)
     * @param port    the server port (ignored — kept for source compatibility)
     * @deprecated Navigation is fully delegated to {@link MainMenuController}.
     */
    @Deprecated
    public void showWaitingForMatch(RulesetOptions ruleset, String ip, int port) {
        // No-op — MainMenuController handles waiting-for-match via panel hosting
    }

    /**
     * @deprecated Use {@link #showOverlay(String, Object)} instead.
     * Kept for source compatibility until Task 5 updates all callers.
     */
    @Deprecated
    public <C> C showDialog(String fxmlPath, C controller) {
        return showOverlay(fxmlPath, controller);
    }

    /**
     * Loads {@code fxmlPath} as a dimmed in-window overlay, blocking until
     * the controller calls {@link #dismissOverlay()}. Returns the controller.
     */
    public <C> C showOverlay(String fxmlPath, C controller) {
        return requireOverlay().showOverlay(fxmlPath, controller);
    }

    /**
     * Shows an inline confirmation overlay. Returns {@code true} if the user
     * clicked Yes.
     */
    public boolean showConfirm(String message) {
        return requireOverlay().showConfirm(message);
    }

    /**
     * Dismisses the topmost overlay. Called by dialog controllers.
     */
    public void dismissOverlay() {
        requireOverlay().dismiss();
    }

    // ── Cinematic screen support ─────────────────────────────────────────────

    /**
     * Returns whether the given FXML path corresponds to a cinematic screen
     * that uses the shared animated background.
     *
     * @param fxmlPath the FXML resource path to check
     * @return {@code true} if the screen is cinematic
     */
    public boolean isCinematicScreen(String fxmlPath) {
        return CINEMATIC_SCREENS.contains(fxmlPath);
    }

    /**
     * Returns the persistent cinematic background, or {@code null} if no
     * cinematic screen has been shown yet.
     *
     * @return the shared {@link CinematicBackground} instance, or {@code null}
     */
    public CinematicBackground getCinematicBackground() {
        return cinematicBackground;
    }

    // ── Internal scene swap ───────────────────────────────────────────────────

    /**
     * Swaps the current foreground content, managing the cinematic background layer
     * based on whether the target screen is cinematic or plain.
     *
     * <p>Stage dimensions ({@code w} and {@code h}) are applied <em>only</em> on the
     * very first call (when no scene exists yet). Subsequent scene swaps — for example,
     * returning from the game screen to the main menu — do <strong>not</strong> reset the
     * stage size so that user window resizes are preserved. The game screen still sets
     * {@link Stage#setMaximized(boolean) maximized} via the caller.</p>
     *
     * <p>Child nodes are tracked by identity (not index) to avoid misaligned removal
     * when the StackPane contains a variable number of layers.</p>
     *
     * @param fxmlPath   the FXML resource path to load
     * @param controller the controller instance for the FXML
     * @param w          the desired initial scene/stage width (used only on first show)
     * @param h          the desired initial scene/stage height (used only on first show)
     */
    private void swapScene(String fxmlPath, Object controller, double w, double h) {
        Parent root = loadFxml(fxmlPath, controller);
        boolean cinematic = isCinematicScreen(fxmlPath);

        if (primaryStage.getScene() == null) {
            // First scene creation — set initial dimensions
            rootStack = new StackPane();
            overlayManager = new OverlayManager(rootStack, i18n);

            if (cinematic) {
                ensureCinematicBackground();
                rootStack.getChildren().addAll(cinematicBackground.getRoot(), root);
            } else {
                rootStack.getChildren().add(root);
            }
            currentContent = root;

            Scene scene = new Scene(rootStack, w, h);
            themeManager.registerScene(scene);
            primaryStage.setScene(scene);

            // Resolve theme colors after CSS is applied
            if (cinematicBackground != null) {
                Platform.runLater(() -> cinematicBackground.updateThemeColors());
            }
        } else {
            // Subsequent swap — preserve user window size, do NOT call setWidth/setHeight
            if (currentContent != null) {
                rootStack.getChildren().remove(currentContent);
            }

            if (cinematic) {
                ensureCinematicBackground();
                // Ensure background is in the stack
                if (!rootStack.getChildren().contains(cinematicBackground.getRoot())) {
                    rootStack.getChildren().add(0, cinematicBackground.getRoot());
                }
                // Add content after background
                int bgIndex = rootStack.getChildren().indexOf(cinematicBackground.getRoot());
                rootStack.getChildren().add(bgIndex + 1, root);
            } else {
                // Remove and pause background for non-cinematic screens
                if (cinematicBackground != null) {
                    cinematicBackground.stop();
                    rootStack.getChildren().remove(cinematicBackground.getRoot());
                }
                rootStack.getChildren().add(0, root);
            }
            currentContent = root;
            // NOTE: stage.setWidth/setHeight intentionally omitted here to preserve
            // user-resized window dimensions across scene transitions.
        }
        primaryStage.show();
    }

    /**
     * Lazily creates the cinematic background on first use, or restarts it
     * if it was previously stopped.
     */
    private void ensureCinematicBackground() {
        if (cinematicBackground == null) {
            cinematicBackground = new CinematicBackground(themeManager);
            cinematicBackground.start();
        } else if (!cinematicBackground.isRunning()) {
            cinematicBackground.start();
        }
    }

    private Parent loadFxml(String fxmlPath, Object controller) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath), i18n.getBundle());
            loader.setControllerFactory(type -> controller);
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML: " + fxmlPath, e);
        }
    }

    private OverlayManager requireOverlay() {
        if (overlayManager == null) {
            throw new IllegalStateException(
                    "OverlayManager is not ready — call showMainMenu/showGame/showSettings first");
        }
        return overlayManager;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public Chess getChess() {
        return chess;
    }

    public ThemeManager getThemeManager() {
        return themeManager;
    }

    public I18n getI18n() {
        return i18n;
    }

    public SettingsService getSettingsService() {
        return settingsService;
    }
}
