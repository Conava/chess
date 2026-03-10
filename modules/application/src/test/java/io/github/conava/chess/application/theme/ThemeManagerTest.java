package io.github.conava.chess.application.theme;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThemeManagerTest {

    @Test
    void defaultThemeIsDarkPurple() {
        ThemeManager tm = new ThemeManager();
        assertEquals(Theme.DARK_PURPLE, tm.getTheme());
    }

    @Test
    void defaultBoardThemeIsClassic() {
        ThemeManager tm = new ThemeManager();
        assertEquals(BoardTheme.CLASSIC, tm.getBoardTheme());
    }

    @Test
    void setThemeUpdatesProperty() {
        ThemeManager tm = new ThemeManager();
        tm.setTheme(Theme.LIGHT_ARCTIC);
        assertEquals(Theme.LIGHT_ARCTIC, tm.getTheme());
        assertEquals(Theme.LIGHT_ARCTIC, tm.currentThemeProperty().get());
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
        assertEquals("/css/themes/dark-purple.css", Theme.DARK_PURPLE.cssFile());
        assertEquals("/css/themes/dark-charcoal.css", Theme.DARK_CHARCOAL.cssFile());
        assertEquals("/css/themes/light-paper.css", Theme.LIGHT_PAPER.cssFile());
        assertEquals("/css/themes/light-arctic.css", Theme.LIGHT_ARCTIC.cssFile());
    }

    @Test
    void boardThemeCssFileReturnsCorrectPath() {
        assertEquals("/css/board/classic.css", BoardTheme.CLASSIC.cssFile());
        assertEquals("/css/board/ocean.css", BoardTheme.OCEAN.cssFile());
        assertEquals("/css/board/walnut.css", BoardTheme.WALNUT.cssFile());
    }

    @Test
    void allThemesHaveDisplayNames() {
        for (Theme t : Theme.values()) {
            assertNotNull(t.displayName());
            assertFalse(t.displayName().isBlank());
        }
    }
}
