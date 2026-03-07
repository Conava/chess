package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.settings.SettingsService;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class OnlineSetupController {

    private final I18n            i18n;
    private final SettingsService settingsService;

    @FXML private ToggleButton createToggle;
    @FXML private ToggleButton joinToggle;
    @FXML private ToggleGroup  modeGroup;
    @FXML private TextField ipField;
    @FXML private TextField portField;
    @FXML private TextField joinCodeField;
    @FXML private Label     joinCodeLabel;
    @FXML private ComboBox<RulesetOptions> rulesetBox;
    @FXML private Label     errorLabel;

    private boolean confirmed = false;

    public OnlineSetupController(I18n i18n, SettingsService settingsService) {
        this.i18n            = i18n;
        this.settingsService = settingsService;
    }

    @FXML
    public void initialize() {
        rulesetBox.setItems(FXCollections.observableArrayList(RulesetOptions.values()));
        rulesetBox.getSelectionModel().selectFirst();

        updateJoinCodeVisibility();
        modeGroup.selectedToggleProperty().addListener((o, old, sel) -> updateJoinCodeVisibility());
    }

    private void updateJoinCodeVisibility() {
        boolean joining = joinToggle.isSelected();
        joinCodeField.setVisible(joining);
        joinCodeField.setManaged(joining);
        joinCodeLabel.setVisible(joining);
        joinCodeLabel.setManaged(joining);
    }

    @FXML
    private void onConnect() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        String ip   = ipField.getText().trim();
        String port = portField.getText().trim();

        if (!isValidIp(ip))     { showError("Invalid IP address."); return; }
        if (!isValidPort(port)) { showError("Port must be 1–65535."); return; }

        confirmed = true;
        close();
    }

    @FXML
    private void onCancel() { close(); }

    private void close() { ((Stage) ipField.getScene().getWindow()).close(); }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private boolean isValidIp(String ip) {
        if (ip.equals("localhost")) return true;
        if (ip.matches("(\\d{1,3}\\.){3}\\d{1,3}")) return true;
        if (ip.contains(":")) return true;
        return false;
    }

    private boolean isValidPort(String port) {
        try { int p = Integer.parseInt(port); return p >= 1 && p <= 65535; }
        catch (NumberFormatException e) { return false; }
    }

    public boolean isConfirmed()       { return confirmed; }
    public String  getIp()             { return ipField.getText().trim(); }
    public String  getPort()           { return portField.getText().trim(); }
    public String  getJoinCode()       { return joinCodeField.isVisible() ? joinCodeField.getText().trim() : ""; }
    public RulesetOptions getRuleset() { return rulesetBox.getValue(); }
    public String  getPlayerWhite()    { return joinToggle.isSelected() ? "Opponent" : "You"; }
    public String  getPlayerBlack()    { return joinToggle.isSelected() ? "You" : "Opponent"; }
}
