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
    void setThemeUpdatesProperty() {
        ThemeManager tm = new ThemeManager();
        tm.setTheme(Theme.LIGHT_ARCTIC);
        assertEquals(Theme.LIGHT_ARCTIC, tm.getTheme());
        assertEquals(Theme.LIGHT_ARCTIC, tm.currentThemeProperty().get());
    }

    @Test
    void themeCssFileReturnsCorrectPath() {
        assertEquals("/css/themes/dark-purple.css", Theme.DARK_PURPLE.cssFile());
        assertEquals("/css/themes/dark-charcoal.css", Theme.DARK_CHARCOAL.cssFile());
        assertEquals("/css/themes/light-paper.css", Theme.LIGHT_PAPER.cssFile());
        assertEquals("/css/themes/light-arctic.css", Theme.LIGHT_ARCTIC.cssFile());
        assertEquals("/css/themes/dark-abyss.css", Theme.DARK_ABYSS.cssFile());
        assertEquals("/css/themes/light-sakura.css", Theme.LIGHT_SAKURA.cssFile());
    }

    @Test
    void allThemesHaveDisplayNames() {
        for (Theme t : Theme.values()) {
            assertNotNull(t.displayName());
            assertFalse(t.displayName().isBlank());
        }
    }

    // NOTE: A test verifying ThemeManager applies exactly 2 stylesheets (not 3)
    // is not feasible here without TestFX, as it requires a live JavaFX Scene.
    // The applyTheme() method calls scene.getStylesheets().setAll(base, themeCss)
    // with exactly 2 arguments, which can be verified by code inspection.
}
