package io.github.conava.chess.application.navigation;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.controllers.GameController;
import io.github.conava.chess.application.controllers.LoginController;
import io.github.conava.chess.application.controllers.MainMenuController;
import io.github.conava.chess.application.controllers.RegisterController;
import io.github.conava.chess.application.controllers.SettingsController;
import io.github.conava.chess.application.controllers.WaitingForMatchController;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.application.theme.ThemeManager;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneManager {

    private static final String FXML_MAIN_MENU = "/fxml/main-menu.fxml";
    private static final String FXML_GAME = "/fxml/game.fxml";
    private static final String FXML_SETTINGS = "/fxml/settings.fxml";
    private static final String FXML_LOGIN = "/fxml/login.fxml";
    private static final String FXML_REGISTER = "/fxml/register.fxml";
    private static final String FXML_WAITING_FOR_MATCH = "/fxml/waiting-for-match.fxml";

    private final Stage primaryStage;
    private final Chess chess;
    private final ThemeManager themeManager;
    private final I18n i18n;
    private final SettingsService settingsService;

    private StackPane rootStack;
    private OverlayManager overlayManager;

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
        swapScene(FXML_MAIN_MENU, controller, 900, 650);
        primaryStage.setMaximized(false);
    }

    public void showGame(RulesetOptions ruleset) {
        var controller = new GameController(this, chess, i18n, ruleset);
        swapScene(FXML_GAME, controller, 1280, 860);
        primaryStage.setMaximized(true);
    }

    public void showSettings() {
        var controller = new SettingsController(this, themeManager, i18n, settingsService);
        primaryStage.setMaximized(false);
        swapScene(FXML_SETTINGS, controller, 760, 920);
    }

    /**
     * Shows the login screen, replacing the current scene.
     */
    public void showLogin() {
        var controller = new LoginController(this, chess, i18n);
        swapScene(FXML_LOGIN, controller, 900, 650);
        primaryStage.setMaximized(false);
    }

    /**
     * Shows the account registration screen, replacing the current scene.
     */
    public void showRegister() {
        var controller = new RegisterController(this, chess, i18n);
        swapScene(FXML_REGISTER, controller, 900, 650);
        primaryStage.setMaximized(false);
    }

    /**
     * Navigates to the matchmaking waiting screen where the client searches for an
     * available opponent with the given ruleset on the specified server.
     *
     * <p>A persistent TCP connection to the server is opened before the waiting screen is
     * shown. If the connection cannot be established the method returns without navigating
     * (the current screen remains visible). Once connected, the {@link WaitingForMatchController}
     * is constructed — which registers the {@code MATCHED} handler — and then
     * {@link io.github.conava.chess.application.Chess#joinMatchmakingQueue(RulesetOptions)}
     * is called from {@link WaitingForMatchController#initialize()} to avoid a race condition
     * where the server's response could arrive before the handler is set.</p>
     *
     * @param ruleset the ruleset the player wants to use for the matched game
     * @param ip      the server IP address or hostname
     * @param port    the server port number
     */
    public void showWaitingForMatch(RulesetOptions ruleset, String ip, int port) {
        if (!chess.connectToServer(ip, port)) {
            return;
        }
        var controller = new WaitingForMatchController(this, chess, i18n, ruleset, ip, port);
        swapScene(FXML_WAITING_FOR_MATCH, controller, 900, 650);
        primaryStage.setMaximized(false);
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

    // ── Internal scene swap ───────────────────────────────────────────────────

    private void swapScene(String fxmlPath, Object controller, double w, double h) {
        Parent root = loadFxml(fxmlPath, controller);
        if (primaryStage.getScene() == null) {
            rootStack = new StackPane(root);
            overlayManager = new OverlayManager(rootStack, i18n);
            Scene scene = new Scene(rootStack, w, h);
            themeManager.registerScene(scene);
            primaryStage.setScene(scene);
        } else {
            rootStack.getChildren().set(0, root);
            primaryStage.setWidth(w);
            primaryStage.setHeight(h);
        }
        primaryStage.show();
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
