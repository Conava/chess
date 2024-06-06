package ptp.project.window.components;

import javax.swing.*;
import java.awt.*;

public class ColorScheme {
    private final Font defaultFont;
    private final Color fontColor;
    private final Color accentColor;
    private final Color backgroundColor;
    private final Color buttonColor;
    private final Color buttonHoverColor;
    private final Color exitButtonColor;
    private final Color exitButtonHoverColor;

    public ColorScheme(Font defaultFont, Color backgroundColor, Color fontColor,  Color buttonColor, Color accentColor, Color exitButtonColor) {
        this.defaultFont = defaultFont;
        this.backgroundColor = backgroundColor;
        this.fontColor = fontColor;
        this.buttonColor = buttonColor;
        this.accentColor = accentColor;
        this.buttonHoverColor = buttonColor.brighter().brighter().brighter();
        this.exitButtonColor = exitButtonColor;
        this.exitButtonHoverColor = exitButtonColor.brighter().brighter().brighter();
    }

    public Color getButtonColor() {
        return buttonColor;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Color getFontColor() {
        return fontColor;
    }

    public Color getAccentColor() {
        return accentColor;
    }

    public Color getButtonHoverColor() {
        return buttonHoverColor;
    }

    public Color getExitButtonColor() {
        return exitButtonColor;
    }

    public Color getExitButtonHoverColor() {
        return exitButtonHoverColor;
    }

    public Font getFont() {
        return defaultFont;
    }
}
