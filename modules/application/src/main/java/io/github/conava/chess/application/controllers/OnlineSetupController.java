package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class OnlineSetupController {

    private final I18n i18n;
    private final Runnable closeAction;

    @FXML
    private ToggleButton createToggle;
    @FXML
    private ToggleButton joinToggle;
    @FXML
    private ToggleGroup modeGroup;
    @FXML
    private TextField ipField;
    @FXML
    private TextField portField;
    @FXML
    private TextField joinCodeField;
    @FXML
    private Label joinCodeLabel;
    @FXML
    private ComboBox<RulesetOptions> rulesetBox;
    @FXML
    private Label rulesetLabel;
    @FXML
    private Label errorLabel;

    private boolean confirmed = false;

    public OnlineSetupController(I18n i18n, Runnable closeAction) {
        this.i18n = i18n;
        this.closeAction = closeAction;
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
        rulesetBox.setVisible(!joining);
        rulesetBox.setManaged(!joining);
        rulesetLabel.setVisible(!joining);
        rulesetLabel.setManaged(!joining);
    }

    @FXML
    private void onConnect() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        String ip = ipField.getText().trim();
        String port = portField.getText().trim();
        if (!isValidIp(ip)) {
            showError(i18n.get("dialog.online.error.invalid_ip"));
            return;
        }
        if (!isValidPort(port)) {
            showError(i18n.get("dialog.online.error.invalid_port"));
            return;
        }
        if (joinToggle.isSelected() && joinCodeField.getText().trim().isEmpty()) {
            showError(i18n.get("dialog.online.error.empty_join_code"));
            return;
        }
        confirmed = true;
        if (closeAction != null) closeAction.run();
    }

    @FXML
    private void onCancel() {
        if (closeAction != null) closeAction.run();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private boolean isValidIp(String ip) {
        return ip.equals("localhost") || ip.matches("(\\d{1,3}\\.){3}\\d{1,3}") || ip.contains(":");
    }

    private boolean isValidPort(String port) {
        try {
            int p = Integer.parseInt(port);
            return p >= 1 && p <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getIp() {
        return ipField.getText().trim();
    }

    public String getPort() {
        return portField.getText().trim();
    }

    public String getJoinCode() {
        return joinCodeField.isVisible() ? joinCodeField.getText().trim() : "";
    }

    public RulesetOptions getRuleset() {
        return rulesetBox.getValue();
    }

    public String getPlayerWhite() {
        return joinToggle.isSelected() ? i18n.get("dialog.online.player.opponent") : i18n.get("dialog.online.player.you");
    }

    public String getPlayerBlack() {
        return joinToggle.isSelected() ? i18n.get("dialog.online.player.you") : i18n.get("dialog.online.player.opponent");
    }

    public ToggleButton getCreateToggle() {
        return createToggle;
    }

    public void setCreateToggle(ToggleButton createToggle) {
        this.createToggle = createToggle;
    }
}
