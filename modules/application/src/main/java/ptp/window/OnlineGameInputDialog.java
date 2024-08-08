package ptp.window;

import ptp.components.*;
import ptp.core.logic.ruleset.RulesetOptions;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class OnlineGameInputDialog extends JDialog {
    private static final int DIALOG_WIDTH = 460;
    private static final int DIALOG_HEIGHT = 430;
    private static final int BORDER_RADIUS = 30;
    private static final int FONT_SIZE = 26;

    private CustomTextField ipField;
    private CustomTextField portField;
    private CustomTextField joinCodeField;
    private CustomComboBox<RulesetOptions> comboBox;
    private boolean confirmed = false;
    private String ip, port, joinCode;
    private RulesetOptions selection;

    public OnlineGameInputDialog(JFrame parent, String title, ColorScheme colorScheme) {
        super(parent, title, true);
        initializeDialog(colorScheme);
        addComponentsToDialog(title, colorScheme);
    }

    private void initializeDialog(ColorScheme colorScheme) {
        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setResizable(false);
        setUndecorated(true);
        getContentPane().setBackground(colorScheme.getDarkerBackgroundColor());
        setLayout(new BorderLayout());
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), BORDER_RADIUS, BORDER_RADIUS));
        setLocationRelativeTo(getParent());
    }

    private void addComponentsToDialog(String title, ColorScheme colorScheme) {
        JPanel contentPanel = createContentPanel(colorScheme);
        contentPanel.add(createTitlePanel(title, colorScheme), BorderLayout.NORTH);
        contentPanel.add(createInputPanelContainer(colorScheme), BorderLayout.CENTER);
        contentPanel.add(createButtonPanel(colorScheme), BorderLayout.SOUTH);
        add(contentPanel);
    }

    private JPanel createContentPanel(ColorScheme colorScheme) {
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPanel.setBackground(colorScheme.getDarkerBackgroundColor());
        return contentPanel;
    }

    private JPanel createTitlePanel(String title, ColorScheme colorScheme) {
        JPanel topLabelPanel = new JPanel(new BorderLayout());
        topLabelPanel.setBackground(colorScheme.getDarkerBackgroundColor());

        JPanel titleLabelPanel = new JPanel(new BorderLayout());
        titleLabelPanel.setBackground(colorScheme.getDarkerBackgroundColor());
        CustomLabel titleLabel = new CustomLabel(title, colorScheme);
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, FONT_SIZE));
        titleLabelPanel.add(titleLabel, BorderLayout.CENTER);
        titleLabelPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, colorScheme.getBorderColor()));

        topLabelPanel.add(titleLabelPanel, BorderLayout.CENTER);
        topLabelPanel.add(Box.createVerticalStrut(10), BorderLayout.SOUTH); // Add gap below the title border

        JPanel radioPanel = createRadioPanel(colorScheme);
        radioPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0)); // Add gap above the radio panel
        topLabelPanel.add(radioPanel, BorderLayout.SOUTH);

        return topLabelPanel;
    }

    private JPanel createRadioPanel(ColorScheme colorScheme) {
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        radioPanel.setBackground(colorScheme.getDarkerBackgroundColor());
        JRadioButton joinGameButton = new CustomJRadioButton("Join Game", colorScheme);
        JRadioButton createGameButton = new CustomJRadioButton("Create Game", colorScheme);
        ButtonGroup group = new ButtonGroup();
        group.add(joinGameButton);
        group.add(createGameButton);
        joinGameButton.setSelected(true);
        radioPanel.add(joinGameButton);
        radioPanel.add(createGameButton);
        joinGameButton.addActionListener(e -> toggleJoinCreateFields(true));
        createGameButton.addActionListener(e -> toggleJoinCreateFields(false));
        return radioPanel;
    }

    private JPanel createInputPanelContainer(ColorScheme colorScheme) {
        JPanel inputPanelContainer = new JPanel(new BorderLayout());
        inputPanelContainer.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        inputPanelContainer.setBackground(colorScheme.getDarkerBackgroundColor());
        inputPanelContainer.add(createInputPanel(colorScheme), BorderLayout.NORTH);
        return inputPanelContainer;
    }

    private JPanel createInputPanel(ColorScheme colorScheme) {
        JPanel contentPanel = new ControlPanel();
        contentPanel.setLayout(new BorderLayout());
        contentPanel.add(new CustomLabel("Farb- und Namenswahl wird für Online Spiele noch nicht unterstützt.", colorScheme) {
            {
                setFont(getFont().deriveFont(13f)); // Set font size to 12
                setForeground(Color.GRAY); // Set text color to grey
            }
        }, BorderLayout.NORTH);
        JPanel inputPanel = new ControlPanel();
        inputPanel.setLayout(new GridLayout(4, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0)); // Add 10px gap at the top

        inputPanel.add(new CustomLabel("IP:", colorScheme));
        ipField = new CustomTextField(colorScheme);
        inputPanel.add(ipField);
        inputPanel.add(new CustomLabel("Port:", colorScheme));
        portField = new CustomTextField(colorScheme);
        inputPanel.add(portField);
        inputPanel.add(new CustomLabel("Join Code:", colorScheme));
        joinCodeField = new CustomTextField(colorScheme);
        inputPanel.add(joinCodeField);
        comboBox = new CustomComboBox<>(RulesetOptions.values(), colorScheme);
        comboBox.setSelectedItem(RulesetOptions.STANDARD);
        comboBox.setFocusable(false);
        comboBox.setToolTipText("Wähle eine Spielvariante aus. Weitere werden in Kürze implementiert");
        inputPanel.add(new CustomLabel("Ruleset:", colorScheme));
        inputPanel.add(comboBox);

        contentPanel.add(inputPanel, BorderLayout.SOUTH);

        return contentPanel;
    }

    private JPanel createButtonPanel(ColorScheme colorScheme) {
        JPanel buttonPanel = new ControlPanel();
        buttonPanel.setBackground(colorScheme.getBackgroundColor().darker());
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new CustomButton("OK", colorScheme);
        JButton cancelButton = new ExitButton("Cancel", colorScheme);
        okButton.addActionListener(e -> onOkButtonPressed());
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        return buttonPanel;
    }

    private void onOkButtonPressed() {
        boolean valid = true;
        StringBuilder errorMessage = new StringBuilder("Please correct the following errors:\n");

        // Validate IP address
        ip = ipField.getText();
        if (ip.isEmpty() || !isValidIpAddress(ip)) {
            valid = false;
            errorMessage.append("- Invalid IP address\n");
            ipField.setBorder(BorderFactory.createLineBorder(Color.RED));
        } else {
            ipField.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        }

        // Validate port
        port = portField.getText();
        if (port.isEmpty() || !isValidPort(port)) {
            valid = false;
            errorMessage.append("- Invalid port number\n");
            portField.setBorder(BorderFactory.createLineBorder(Color.RED));
        } else {
            portField.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        }

        // Validate join code
        joinCode = joinCodeField.getText();
        if (joinCode.isEmpty()) {
            valid = false;
            errorMessage.append("- Join code cannot be empty\n");
            joinCodeField.setBorder(BorderFactory.createLineBorder(Color.RED));
        } else {
            joinCodeField.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        }

        if (valid) {
            confirmed = true;
            selection = (RulesetOptions) comboBox.getSelectedItem();
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, errorMessage.toString(), "Validation Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isValidIpAddress(String ip) {
        String ipv4Pattern =
                "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        String ipv6Pattern =
                "(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))";
        String localhostPattern = "^(localhost|127\\.0\\.0\\.1)$";
        return ip.matches(ipv4Pattern) || ip.matches(ipv6Pattern) || ip.matches(localhostPattern);
    }

    private boolean isValidPort(String port) {
        try {
            int portNumber = Integer.parseInt(port);
            return portNumber > 0 && portNumber <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void toggleJoinCreateFields(boolean isJoin) {
        joinCodeField.setEnabled(isJoin);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public RulesetOptions getRulesetSelection() {
        return selection;
    }
}