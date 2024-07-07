package ptp.project.window;

import ptp.project.data.enums.RulesetOptions;
import ptp.project.window.components.*;

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
        setSize(420, 300);
        setResizable(false);
        setUndecorated(true);
        getContentPane().setBackground(colorScheme.getDarkerBackgroundColor());
        setLayout(new BorderLayout());
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 30, 30));
        setLocationRelativeTo(null);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 20, 20, 20));
        contentPanel.setBackground(colorScheme.getDarkerBackgroundColor());

        JPanel topLabelPanel = new JPanel(new BorderLayout());
        topLabelPanel.setBackground(colorScheme.getDarkerBackgroundColor());
        CustomLabel titleLabel = new CustomLabel(title, colorScheme);
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 26)); // Increase font size
        topLabelPanel.add(titleLabel, BorderLayout.CENTER);
        topLabelPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, colorScheme.getBorderColor())); // Add border
        contentPanel.add(topLabelPanel, BorderLayout.NORTH);

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
        inputPanel.add(new CustomLabel("Ruleset:", colorScheme));
        inputPanel.add(comboBox);

        contentPanel.add(inputPanel, BorderLayout.CENTER);

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
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(contentPanel);
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