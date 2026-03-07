package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.util.Map;

public class MainMenuController {

    private final SceneManager sceneManager;
    private final I18n         i18n;

    @FXML private ImageView titleImage;
    @FXML private ImageView watermarkImage;
    @FXML private Button    exitBtn;

    public MainMenuController(SceneManager sceneManager, I18n i18n) {
        this.sceneManager = sceneManager;
        this.i18n         = i18n;
    }

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
    }

    @FXML
    private void onLocalGame() {
        OfflineSetupController setup = sceneManager.showOverlay(
                "/fxml/offline-setup.fxml",
                new OfflineSetupController(i18n, sceneManager.getSettingsService(),
                        sceneManager::dismissOverlay));
        if (!setup.isConfirmed()) return;

        sceneManager.getChess().startGame(
                false, setup.getRuleset(),
                setup.getPlayerWhite(), setup.getPlayerBlack(), null);
        sceneManager.showGame();
    }

    @FXML
    private void onOnlineGame() {
        OnlineSetupController setup = sceneManager.showOverlay(
                "/fxml/online-setup.fxml",
                new OnlineSetupController(i18n, sceneManager.getSettingsService(),
                        sceneManager::dismissOverlay));
        if (!setup.isConfirmed()) return;

        Map<String, String> opts = Map.of(
                "ip",       setup.getIp(),
                "port",     setup.getPort(),
                "joinCode", setup.getJoinCode());

        sceneManager.getChess().startGame(
                true, setup.getRuleset(),
                setup.getPlayerWhite(), setup.getPlayerBlack(), opts);
        sceneManager.showGame();
    }

    @FXML
    private void onSettings() {
        sceneManager.showSettings();
    }

    @FXML
    private void onExit() {
        ((Stage) exitBtn.getScene().getWindow()).close();
    }
}
