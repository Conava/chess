package ptp.project.window.components;

import java.awt.*;

public class ColorScheme {
    private final Color fontColor;
    private final Color accentColor;
    private final Color backgroundColor;
    private final Color buttonColor;

    public ColorScheme(Color backgroundColor, Color fontColor,  Color buttonColor, Color accentColor) {
        this.backgroundColor = backgroundColor;
        this.fontColor = fontColor;
        this.buttonColor = buttonColor;
        this.accentColor = accentColor;
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

}
