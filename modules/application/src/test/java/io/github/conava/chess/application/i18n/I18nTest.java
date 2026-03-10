package io.github.conava.chess.application.i18n;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class I18nTest {

    @Test
    void englishKeyReturnsEnglishValue() {
        I18n i18n = new I18n(I18n.Language.EN);
        assertEquals("Local Game", i18n.get("menu.local"));
    }

    @Test
    void germanKeyReturnsGermanValue() {
        I18n i18n = new I18n(I18n.Language.DE);
        assertEquals("Lokales Spiel", i18n.get("menu.local"));
    }

    @Test
    void switchingLanguageUpdatesBundle() {
        I18n i18n = new I18n(I18n.Language.EN);
        i18n.setLanguage(I18n.Language.DE);
        assertEquals("Lokales Spiel", i18n.get("menu.local"));
        assertEquals(I18n.Language.DE, i18n.getLanguage());
    }

    @Test
    void languagePropertyReflectsCurrentLanguage() {
        I18n i18n = new I18n(I18n.Language.EN);
        assertEquals(I18n.Language.EN, i18n.currentLanguageProperty().get());
        i18n.setLanguage(I18n.Language.DE);
        assertEquals(I18n.Language.DE, i18n.currentLanguageProperty().get());
    }
}
