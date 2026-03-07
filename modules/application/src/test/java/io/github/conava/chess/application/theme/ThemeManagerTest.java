package io.github.conava.chess.application.theme;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ThemeManagerTest {

    @Test
    void defaultThemeIsDark() {
        ThemeManager tm = new ThemeManager();
        assertEquals(Theme.DARK, tm.getTheme());
    }

    @Test
    void defaultBoardThemeIsClassic() {
        ThemeManager tm = new ThemeManager();
        assertEquals(BoardTheme.CLASSIC, tm.getBoardTheme());
    }

    @Test
    void setThemeUpdatesProperty() {
        ThemeManager tm = new ThemeManager();
        tm.setTheme(Theme.LIGHT);
        assertEquals(Theme.LIGHT, tm.getTheme());
        assertEquals(Theme.LIGHT, tm.currentThemeProperty().get());
    }

    @Test
    void setBoardThemeUpdatesProperty() {
        ThemeManager tm = new ThemeManager();
        tm.setBoardTheme(BoardTheme.OCEAN);
        assertEquals(BoardTheme.OCEAN, tm.getBoardTheme());
        assertEquals(BoardTheme.OCEAN, tm.currentBoardThemeProperty().get());
    }

    @Test
    void themeCssFileReturnsCorrectPath() {
        assertEquals("/css/dark.css", Theme.DARK.cssFile());
        assertEquals("/css/light.css", Theme.LIGHT.cssFile());
    }

    @Test
    void boardThemeCssFileReturnsCorrectPath() {
        assertEquals("/css/board/classic.css", BoardTheme.CLASSIC.cssFile());
        assertEquals("/css/board/ocean.css", BoardTheme.OCEAN.cssFile());
        assertEquals("/css/board/walnut.css", BoardTheme.WALNUT.cssFile());
    }
}
