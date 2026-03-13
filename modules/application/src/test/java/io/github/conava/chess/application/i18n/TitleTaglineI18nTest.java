package io.github.conava.chess.application.i18n;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the updated title and tagline i18n keys resolve to the
 * expected cinematic branding strings for all supported languages.
 */
class TitleTaglineI18nTest {

    @Test
    void englishBundle_hasExpectedTitle() {
        I18n i18n = new I18n(I18n.Language.EN);
        assertEquals("The King's Game", i18n.get("menu.title"));
    }

    @Test
    void englishBundle_hasExpectedTagline() {
        I18n i18n = new I18n(I18n.Language.EN);
        assertEquals("Play your way.", i18n.get("menu.tagline"));
    }

    @Test
    void germanBundle_hasTitle() {
        I18n i18n = new I18n(I18n.Language.DE);
        assertNotNull(i18n.get("menu.title"));
        assertFalse(i18n.get("menu.title").isEmpty());
    }

    @Test
    void germanBundle_hasTagline() {
        I18n i18n = new I18n(I18n.Language.DE);
        assertNotNull(i18n.get("menu.tagline"));
        assertFalse(i18n.get("menu.tagline").isEmpty());
    }
}
