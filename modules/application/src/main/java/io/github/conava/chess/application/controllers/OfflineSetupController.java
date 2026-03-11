package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

/**
 * Controller for the offline (local) game setup screen.
 *
 * <p>Collects player names and ruleset selection, then starts a local game via
 * {@link SceneManager#getChess()} and navigates to the game screen.</p>
 */
public class OfflineSetupController {

    private final SceneManager sceneManager;
    private final I18n i18n;
    private final SettingsService settingsService;

    @FXML
    private TextField whiteField;
    @FXML
    private TextField blackField;
    @FXML
    private ComboBox<RulesetOptions> rulesetBox;

    /**
     * Constructs an {@code OfflineSetupController}.
     *
     * @param sceneManager   the navigation manager used to show other screens and access the Chess facade.
     * @param i18n           the internationalisation helper used for default player name lookup.
     * @param settingsService the settings service used to load saved player name defaults.
     */
    public OfflineSetupController(SceneManager sceneManager, I18n i18n, SettingsService settingsService) {
        this.sceneManager = sceneManager;
        this.i18n = i18n;
        this.settingsService = settingsService;
    }

    /**
     * Initialises the screen after FXML injection.
     *
     * <p>Populates the ruleset combo box and pre-fills player name fields from saved preferences.</p>
     */
    @FXML
    public void initialize() {
        rulesetBox.setItems(FXCollections.observableArrayList(RulesetOptions.values()));
        rulesetBox.getSelectionModel().selectFirst();
        whiteField.setText(settingsService.loadPlayerWhite());
        blackField.setText(settingsService.loadPlayerBlack());
    }

    /**
     * Handles the Start Game button.
     *
     * <p>Fills blank name fields with i18n defaults, starts the game via the Chess facade,
     * then navigates to the game screen.</p>
     */
    @FXML
    private void onStart() {
        if (whiteField.getText().isBlank()) whiteField.setText(i18n.get("dialog.offline.default.white"));
        if (blackField.getText().isBlank()) blackField.setText(i18n.get("dialog.offline.default.black"));
        RulesetOptions ruleset = rulesetBox.getValue();
        sceneManager.getChess().startGame(false, ruleset, whiteField.getText().trim(), blackField.getText().trim(), null);
        sceneManager.showGame(ruleset);
    }

    /**
     * Handles the Cancel button — navigates back to the main menu.
     */
    @FXML
    private void onCancel() {
        sceneManager.showMainMenu();
    }
}
