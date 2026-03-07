package io.github.conava.chess.application.navigation;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.application.theme.ThemeManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;

public class SceneManager {

    private static final String FXML_MAIN_MENU = "/fxml/main-menu.fxml";
    private static final String FXML_GAME      = "/fxml/game.fxml";
    private static final String FXML_SETTINGS  = "/fxml/settings.fxml";

    private final Stage primaryStage;
    private final Chess chess;
    private final ThemeManager themeManager;
    private final I18n i18n;
    private final SettingsService settingsService;

    public SceneManager(Stage primaryStage, Chess chess, ThemeManager themeManager,
                        I18n i18n, SettingsService settingsService) {
        this.primaryStage    = primaryStage;
        this.chess           = chess;
        this.themeManager    = themeManager;
        this.i18n            = i18n;
        this.settingsService = settingsService;
    }

    public void showMainMenu() {
        var controller = new io.github.conava.chess.application.controllers.MainMenuController(this, i18n);
        swapScene(FXML_MAIN_MENU, controller, 900, 650);
        primaryStage.setMaximized(false);
    }

    public void showGame() {
        var controller = new io.github.conava.chess.application.controllers.GameController(
                this, chess, themeManager, i18n);
        swapScene(FXML_GAME, controller, 1280, 860);
        primaryStage.setMaximized(true);
    }

    public void showSettings() {
        var controller = new io.github.conava.chess.application.controllers.SettingsController(
                this, themeManager, i18n, settingsService);
        swapScene(FXML_SETTINGS, controller, 900, 650);
    }

    public <C> C showDialog(String fxmlResourcePath, C controller) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlResourcePath), i18n.getBundle());
            loader.setController(controller);
            Parent root = loader.load();

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(primaryStage);
            dialog.setResizable(false);

            Scene scene = new Scene(root);
            themeManager.registerScene(scene);
            dialog.setScene(scene);
            dialog.showAndWait();
            themeManager.unregisterScene(scene);

            return controller;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load dialog: " + fxmlResourcePath, e);
        }
    }

    private void swapScene(String fxmlPath, Object controller, double w, double h) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath), i18n.getBundle());
            loader.setController(controller);
            Parent root = loader.load();

            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root, w, h);
                themeManager.registerScene(scene);
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML: " + fxmlPath, e);
        }
    }

    public Stage getPrimaryStage()             { return primaryStage; }
    public Chess getChess()                    { return chess; }
    public ThemeManager getThemeManager()      { return themeManager; }
    public I18n getI18n()                      { return i18n; }
    public SettingsService getSettingsService() { return settingsService; }
}
