package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.application.theme.Theme;
import io.github.conava.chess.application.theme.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

public class SettingsController {

    private final SceneManager sceneManager;
    private final ThemeManager themeManager;
    private final I18n i18n;
    private final SettingsService settingsService;

    @FXML
    private ToggleButton midnightSwatch;
    @FXML
    private ToggleButton emberSwatch;
    @FXML
    private ToggleButton manuscriptSwatch;
    @FXML
    private ToggleButton fjordSwatch;
    @FXML
    private ToggleGroup themeGroup;
    @FXML
    private ToggleButton enToggle;
    @FXML
    private ToggleButton deToggle;
    @FXML
    private ToggleGroup langGroup;
    @FXML
    private TextField whiteNameField;
    @FXML
    private TextField blackNameField;

    public SettingsController(SceneManager sceneManager, ThemeManager themeManager, I18n i18n, SettingsService settingsService) {
        this.sceneManager = sceneManager;
        this.themeManager = themeManager;
        this.i18n = i18n;
        this.settingsService = settingsService;
    }

    @FXML
    public void initialize() {
        switch (themeManager.getTheme()) {
            case DARK_PURPLE -> midnightSwatch.setSelected(true);
            case DARK_CHARCOAL -> emberSwatch.setSelected(true);
            case LIGHT_PAPER -> manuscriptSwatch.setSelected(true);
            case LIGHT_ARCTIC -> fjordSwatch.setSelected(true);
        }

        if (i18n.getLanguage() == I18n.Language.EN) enToggle.setSelected(true);
        else deToggle.setSelected(true);

        whiteNameField.setText(settingsService.loadPlayerWhite());
        blackNameField.setText(settingsService.loadPlayerBlack());

        themeGroup.selectedToggleProperty().addListener((o, old, sel) -> {
            if (sel == midnightSwatch) themeManager.setTheme(Theme.DARK_PURPLE);
            else if (sel == emberSwatch) themeManager.setTheme(Theme.DARK_CHARCOAL);
            else if (sel == manuscriptSwatch) themeManager.setTheme(Theme.LIGHT_PAPER);
            else if (sel == fjordSwatch) themeManager.setTheme(Theme.LIGHT_ARCTIC);
        });

        langGroup.selectedToggleProperty().addListener((o, old, sel) -> {
            if (sel == enToggle) i18n.setLanguage(I18n.Language.EN);
            else if (sel == deToggle) i18n.setLanguage(I18n.Language.DE);
        });
    }

    @FXML
    private void onSave() {
        settingsService.saveTheme(themeManager.getTheme());
        settingsService.saveLanguage(i18n.getLanguage());
        settingsService.savePlayerWhite(whiteNameField.getText().trim());
        settingsService.savePlayerBlack(blackNameField.getText().trim());
        sceneManager.showMainMenu();
    }

    @FXML
    private void onCancel() {
        themeManager.setTheme(settingsService.loadTheme());
        i18n.setLanguage(settingsService.loadLanguage());
        sceneManager.showMainMenu();
    }
}
