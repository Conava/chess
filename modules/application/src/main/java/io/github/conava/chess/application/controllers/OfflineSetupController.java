package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class OfflineSetupController {

    private final I18n            i18n;
    private final SettingsService settingsService;

    @FXML private TextField whiteField;
    @FXML private TextField blackField;
    @FXML private ComboBox<RulesetOptions> rulesetBox;

    private boolean confirmed = false;

    public OfflineSetupController(I18n i18n, SettingsService settingsService) {
        this.i18n            = i18n;
        this.settingsService = settingsService;
    }

    @FXML
    public void initialize() {
        rulesetBox.setItems(FXCollections.observableArrayList(RulesetOptions.values()));
        rulesetBox.getSelectionModel().selectFirst();
        whiteField.setText(settingsService.loadPlayerWhite());
        blackField.setText(settingsService.loadPlayerBlack());
    }

    @FXML
    private void onStart() {
        if (whiteField.getText().isBlank()) whiteField.setText("Player White");
        if (blackField.getText().isBlank()) blackField.setText("Player Black");
        confirmed = true;
        close();
    }

    @FXML
    private void onCancel() { close(); }

    private void close() {
        ((Stage) whiteField.getScene().getWindow()).close();
    }

    public boolean isConfirmed()       { return confirmed; }
    public String getPlayerWhite()     { return whiteField.getText().trim(); }
    public String getPlayerBlack()     { return blackField.getText().trim(); }
    public RulesetOptions getRuleset() { return rulesetBox.getValue(); }
}
