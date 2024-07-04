package ptp.project.window.components;

import javax.swing.JComboBox;

public class CustomComboBox<T> extends JComboBox<T> {
    public CustomComboBox(T[] items) {
        super(items);
        // Apply custom styling here
    }
}