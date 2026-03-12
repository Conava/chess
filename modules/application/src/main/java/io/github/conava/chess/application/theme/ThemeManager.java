package io.github.conava.chess.application.theme;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;

import java.util.ArrayList;
import java.util.List;

public class ThemeManager {

    private final ObjectProperty<Theme> currentTheme =
            new SimpleObjectProperty<>(Theme.DARK_PURPLE);
    private final List<Scene> managedScenes = new ArrayList<>();

    public void registerScene(Scene scene) {
        managedScenes.add(scene);
        applyTheme(scene);
    }

    public void setTheme(Theme theme) {
        currentTheme.set(theme);
        managedScenes.forEach(this::applyTheme);
    }

    private void applyTheme(Scene scene) {
        var base = getClass().getResource("/css/base.css");
        var themeCss = getClass().getResource(currentTheme.get().cssFile());
        if (base == null || themeCss == null) return;
        scene.getStylesheets().setAll(
                base.toExternalForm(),
                themeCss.toExternalForm()
        );
    }

    public ObjectProperty<Theme> currentThemeProperty() {
        return currentTheme;
    }

    public Theme getTheme() {
        return currentTheme.get();
    }
}
