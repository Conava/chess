package io.github.conava.chess.application.theme;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import java.util.ArrayList;
import java.util.List;

public class ThemeManager {

    private final ObjectProperty<Theme> currentTheme =
            new SimpleObjectProperty<>(Theme.DARK_PURPLE);
    private final ObjectProperty<BoardTheme> currentBoardTheme =
            new SimpleObjectProperty<>(BoardTheme.CLASSIC);
    private final List<Scene> managedScenes = new ArrayList<>();

    public void registerScene(Scene scene) {
        managedScenes.add(scene);
        applyTheme(scene);
    }

    public void unregisterScene(Scene scene) {
        managedScenes.remove(scene);
    }

    public void setTheme(Theme theme) {
        currentTheme.set(theme);
        managedScenes.forEach(this::applyTheme);
    }

    public void setBoardTheme(BoardTheme boardTheme) {
        currentBoardTheme.set(boardTheme);
        managedScenes.forEach(this::applyTheme);
    }

    private void applyTheme(Scene scene) {
        var base      = getClass().getResource("/css/base.css");
        var themeCss  = getClass().getResource(currentTheme.get().cssFile());
        var boardCss  = getClass().getResource(currentBoardTheme.get().cssFile());
        if (base == null || themeCss == null || boardCss == null) return;
        scene.getStylesheets().setAll(
            base.toExternalForm(),
            themeCss.toExternalForm(),
            boardCss.toExternalForm()
        );
    }

    public ObjectProperty<Theme> currentThemeProperty()           { return currentTheme; }
    public ObjectProperty<BoardTheme> currentBoardThemeProperty() { return currentBoardTheme; }
    public Theme getTheme()           { return currentTheme.get(); }
    public BoardTheme getBoardTheme() { return currentBoardTheme.get(); }
}
