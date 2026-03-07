package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.application.theme.BoardTheme;
import io.github.conava.chess.application.theme.Theme;
import io.github.conava.chess.application.theme.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

public class SettingsController {

    private final SceneManager    sceneManager;
    private final ThemeManager    themeManager;
    private final I18n            i18n;
    private final SettingsService settingsService;

    @FXML private ToggleButton darkToggle;
    @FXML private ToggleButton lightToggle;
    @FXML private ToggleGroup  themeGroup;
    @FXML private ToggleButton classicSwatch;
    @FXML private ToggleButton oceanSwatch;
    @FXML private ToggleButton walnutSwatch;
    @FXML private ToggleGroup  boardGroup;
    @FXML private ToggleButton enToggle;
    @FXML private ToggleButton deToggle;
    @FXML private ToggleGroup  langGroup;
    @FXML private TextField    whiteNameField;
    @FXML private TextField    blackNameField;

    public SettingsController(SceneManager sceneManager, ThemeManager themeManager,
                               I18n i18n, SettingsService settingsService) {
        this.sceneManager    = sceneManager;
        this.themeManager    = themeManager;
        this.i18n            = i18n;
        this.settingsService = settingsService;
    }

    @FXML
    public void initialize() {
        if (themeManager.getTheme() == Theme.DARK) darkToggle.setSelected(true);
        else lightToggle.setSelected(true);

        switch (themeManager.getBoardTheme()) {
            case CLASSIC -> classicSwatch.setSelected(true);
            case OCEAN   -> oceanSwatch.setSelected(true);
            case WALNUT  -> walnutSwatch.setSelected(true);
        }

        if (i18n.getLanguage() == I18n.Language.EN) enToggle.setSelected(true);
        else deToggle.setSelected(true);

        whiteNameField.setText(settingsService.loadPlayerWhite());
        blackNameField.setText(settingsService.loadPlayerBlack());

        themeGroup.selectedToggleProperty().addListener((o, old, sel) -> {
            if (sel == darkToggle)       themeManager.setTheme(Theme.DARK);
            else if (sel == lightToggle) themeManager.setTheme(Theme.LIGHT);
        });

        boardGroup.selectedToggleProperty().addListener((o, old, sel) -> {
            if      (sel == classicSwatch) themeManager.setBoardTheme(BoardTheme.CLASSIC);
            else if (sel == oceanSwatch)   themeManager.setBoardTheme(BoardTheme.OCEAN);
            else if (sel == walnutSwatch)  themeManager.setBoardTheme(BoardTheme.WALNUT);
        });

        langGroup.selectedToggleProperty().addListener((o, old, sel) -> {
            if      (sel == enToggle) i18n.setLanguage(I18n.Language.EN);
            else if (sel == deToggle) i18n.setLanguage(I18n.Language.DE);
        });
    }

    @FXML
    private void onSave() {
        settingsService.saveTheme(themeManager.getTheme());
        settingsService.saveBoardTheme(themeManager.getBoardTheme());
        settingsService.saveLanguage(i18n.getLanguage());
        settingsService.savePlayerWhite(whiteNameField.getText().trim());
        settingsService.savePlayerBlack(blackNameField.getText().trim());
        sceneManager.showMainMenu();
    }

    @FXML
    private void onCancel() {
        themeManager.setTheme(settingsService.loadTheme());
        themeManager.setBoardTheme(settingsService.loadBoardTheme());
        i18n.setLanguage(settingsService.loadLanguage());
        sceneManager.showMainMenu();
    }
}
