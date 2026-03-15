package io.github.conava.chess.application.settings;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.theme.Theme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

class SettingsServiceTest {

    private SettingsService service;

    @BeforeEach
    void setUp() {
        Preferences testPrefs = Preferences.userRoot().node("chess-test-" + System.nanoTime());
        service = new SettingsService(testPrefs);
    }

    @Test
    void defaultThemeIsDarkPurple() {
        assertEquals(Theme.DARK_PURPLE, service.loadTheme());
    }

    @Test
    void saveAndLoadTheme() {
        service.saveTheme(Theme.LIGHT_ARCTIC);
        assertEquals(Theme.LIGHT_ARCTIC, service.loadTheme());
    }

    @Test
    void defaultLanguageIsEn() {
        assertEquals(I18n.Language.EN, service.loadLanguage());
    }

    @Test
    void saveAndLoadLanguage() {
        service.saveLanguage(I18n.Language.DE);
        assertEquals(I18n.Language.DE, service.loadLanguage());
    }

    @Test
    void defaultServerHostIsEmpty() {
        assertEquals("", service.loadServerHost());
    }

    @Test
    void saveAndLoadServerHost() {
        service.saveServerHost("192.168.1.1");
        assertEquals("192.168.1.1", service.loadServerHost());
    }

    @Test
    void defaultServerPortIs54321() {
        assertEquals(54321, service.loadServerPort());
    }

    @Test
    void saveAndLoadServerPort() {
        service.saveServerPort(12345);
        assertEquals(12345, service.loadServerPort());
    }
}
