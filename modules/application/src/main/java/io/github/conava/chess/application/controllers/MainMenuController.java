package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.util.Map;

/**
 * Controller for the main menu screen.
 *
 * <p>Renders the navigation buttons for starting a local or online game, opening settings,
 * and exiting the application. When the user is authenticated, a welcome label and a logout
 * button are shown. The online game button is auth-gated: unauthenticated users are redirected
 * to the login screen instead of proceeding to the online setup dialog.</p>
 */
public class MainMenuController {

    private final SceneManager sceneManager;
    private final Chess chess;
    private final I18n i18n;

    @FXML
    private ImageView titleImage;
    @FXML
    private ImageView watermarkImage;
    @FXML
    private Label welcomeLabel;
    @FXML
    private Button logoutBtn;
    @FXML
    private Button exitBtn;

    /**
     * Constructs a {@code MainMenuController}.
     *
     * @param sceneManager the navigation manager used to show other screens.
     * @param i18n         the internationalisation helper used for message lookup.
     */
    public MainMenuController(SceneManager sceneManager, I18n i18n) {
        this.sceneManager = sceneManager;
        this.chess = sceneManager.getChess();
        this.i18n = i18n;
    }

    /**
     * Initialises the screen after FXML injection.
     *
     * <p>Loads the title and watermark images. If the user is authenticated, the welcome
     * label is populated with the username and both the label and logout button are made
     * visible. Otherwise both remain hidden.</p>
     */
    @FXML
    public void initialize() {
        var imgUrl = getClass().getResource("/titleImage/chessTitleImage.jpg");
        if (imgUrl != null) {
            titleImage.setImage(new Image(imgUrl.toExternalForm()));
        }
        var kingUrl = getClass().getResource("/icon/king_white.png");
        if (kingUrl != null && watermarkImage != null) {
            watermarkImage.setImage(new Image(kingUrl.toExternalForm()));
        }

        if (chess.isAuthenticated()) {
            welcomeLabel.setText(i18n.get("menu.welcome", chess.getUsername()));
            welcomeLabel.setVisible(true);
            welcomeLabel.setManaged(true);
            logoutBtn.setVisible(true);
            logoutBtn.setManaged(true);
        }
    }

    @FXML
    private void onLocalGame() {
        sceneManager.showLocalSetup();
    }

    /**
     * Handles the Online Game button.
     *
     * <p>Auth gate: if the user is not authenticated, navigates to the login screen and
     * returns immediately. Otherwise opens the online setup dialog.</p>
     */
    @FXML
    private void onOnlineGame() {
        if (!chess.isAuthenticated()) {
            sceneManager.showLogin();
            return;
        }

        OnlineSetupController setup = sceneManager.showOverlay("/fxml/online-setup.fxml", new OnlineSetupController(i18n, sceneManager::dismissOverlay));
        if (!setup.isConfirmed()) return;

        String mode = setup.getMode();

        if ("FIND_MATCH".equals(mode)) {
            int port = Integer.parseInt(setup.getPort());
            sceneManager.showWaitingForMatch(setup.getRuleset(), setup.getIp(), port);
            return;
        }

        Map<String, String> opts = Map.of("ip", setup.getIp(), "port", setup.getPort(), "joinCode", setup.getJoinCode());

        sceneManager.getChess().startGame(true, setup.getRuleset(), setup.getPlayerWhite(), setup.getPlayerBlack(), opts);
        sceneManager.showGame(setup.getRuleset());
    }

    @FXML
    private void onSettings() {
        sceneManager.showSettings();
    }

    /**
     * Handles the Logout button.
     *
     * <p>Clears auth state via {@link Chess#logout()} and refreshes the main menu so
     * that the welcome label and logout button are hidden.</p>
     */
    @FXML
    private void onLogout() {
        chess.logout();
        sceneManager.showMainMenu();
    }

    @FXML
    private void onExit() {
        ((Stage) exitBtn.getScene().getWindow()).close();
    }
}
