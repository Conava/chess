package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public class OfflineSetupController {

    private final I18n            i18n;
    private final SettingsService settingsService;
    private final Runnable        closeAction;

    @FXML private TextField whiteField;
    @FXML private TextField blackField;
    @FXML private ComboBox<RulesetOptions> rulesetBox;

    private boolean confirmed = false;

    public OfflineSetupController(I18n i18n, SettingsService settingsService, Runnable closeAction) {
        this.i18n            = i18n;
        this.settingsService = settingsService;
        this.closeAction     = closeAction;
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
        if (whiteField.getText().isBlank()) whiteField.setText(i18n.get("dialog.offline.default.white"));
        if (blackField.getText().isBlank()) blackField.setText(i18n.get("dialog.offline.default.black"));
        confirmed = true;
        if (closeAction != null) closeAction.run();
    }

    @FXML
    private void onCancel() { if (closeAction != null) closeAction.run(); }

    public boolean isConfirmed()       { return confirmed; }
    public String getPlayerWhite()     { return whiteField.getText().trim(); }
    public String getPlayerBlack()     { return blackField.getText().trim(); }
    public RulesetOptions getRuleset() { return rulesetBox.getValue(); }
}
