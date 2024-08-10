package ptp.window;

import ptp.components.*;
import ptp.core.logic.ruleset.RulesetOptions;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class InputDialog extends JDialog {
    private final CustomTextField textField1;
    private final CustomTextField textField2;
    private final CustomComboBox<RulesetOptions> comboBox;
    private boolean confirmed = false;
    private String input1, input2;
    private RulesetOptions selection;

    public InputDialog(JFrame parent, String title, ColorScheme colorScheme, String label1, String label2) {
        super(parent, title, true);
        setSize(460, 300);
        setResizable(false);
        setUndecorated(true);
        getContentPane().setBackground(colorScheme.getDarkerBackgroundColor());
        setLayout(new BorderLayout());
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 30, 30));
        setLocationRelativeTo(parent);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPanel.setBackground(colorScheme.getDarkerBackgroundColor());

        JPanel topLabelPanel = new JPanel(new BorderLayout());
        topLabelPanel.setBackground(colorScheme.getDarkerBackgroundColor());
        CustomLabel titleLabel = new CustomLabel(title, colorScheme);
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 26));
        topLabelPanel.add(titleLabel, BorderLayout.CENTER);
        topLabelPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, colorScheme.getBorderColor()));
        contentPanel.add(topLabelPanel, BorderLayout.NORTH);

        JPanel inputPanelContainer = new JPanel(new BorderLayout());
        inputPanelContainer.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        inputPanelContainer.setBackground(colorScheme.getDarkerBackgroundColor());

        JPanel inputPanel = new ControlPanel();
        inputPanel.setLayout(new GridLayout(3, 2, 10, 10));
        inputPanel.add(new CustomLabel(label1, colorScheme));
        textField1 = new CustomTextField(colorScheme);
        inputPanel.add(textField1);

        inputPanel.add(new CustomLabel(label2, colorScheme));
        textField2 = new CustomTextField(colorScheme);
        inputPanel.add(textField2);

        comboBox = new CustomComboBox<>(RulesetOptions.values(), colorScheme);
        comboBox.setSelectedItem(RulesetOptions.STANDARD);
        comboBox.setFocusable(false);
        comboBox.setToolTipText("Wähle eine Spielvariante aus. Weitere werden in Kürze implementiert");
        inputPanel.add(new CustomLabel("Ruleset:", colorScheme));
        inputPanel.add(comboBox);

        inputPanelContainer.add(inputPanel, BorderLayout.NORTH);
        contentPanel.add(inputPanelContainer, BorderLayout.CENTER);

        JPanel buttonPanel = getButtonPanel(colorScheme);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(contentPanel);
    }

    private JPanel getButtonPanel(ColorScheme colorScheme) {
        JPanel buttonPanel = new ControlPanel();
        buttonPanel.setBackground(colorScheme.getBackgroundColor().darker());
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER)); // Center the buttons
        JButton okButton = new CustomButton("OK", colorScheme);
        JButton cancelButton = new ExitButton("Cancel", colorScheme); // Use ExitButton for cancel

        okButton.addActionListener(e -> {
            confirmed = true;
            input1 = textField1.getText();
            input2 = textField2.getText();
            selection = (RulesetOptions) comboBox.getSelectedItem();
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        return buttonPanel;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getInput1() {
        return input1;
    }

    public String getInput2() {
        return input2;
    }

    public RulesetOptions getRulesetSelection() {
        return selection;
    }
}