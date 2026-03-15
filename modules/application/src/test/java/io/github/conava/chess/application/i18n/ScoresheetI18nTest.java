package io.github.conava.chess.application.i18n;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the scoresheet column header i18n keys exist and resolve to
 * non-empty values in both the English and German resource bundles.
 *
 * <p>Keys under test:
 * <ul>
 *   <li>{@code game.moves.number} -- column header for the move number (e.g. "#")</li>
 *   <li>{@code game.moves.white}  -- column header for White's move (e.g. "White" / "Weiß")</li>
 *   <li>{@code game.moves.black}  -- column header for Black's move (e.g. "Black" / "Schwarz")</li>
 * </ul>
 * </p>
 */
class ScoresheetI18nTest {

    // ── English bundle ─────────────────────────────────────────────────────────

    @Test
    void englishBundle_hasScoresheetMoveNumberHeader() {
        I18n i18n = new I18n(I18n.Language.EN);
        String value = i18n.get("game.moves.number");
        assertNotNull(value, "EN key 'game.moves.number' must exist");
        assertFalse(value.isBlank(), "EN key 'game.moves.number' must not be blank");
        assertEquals("#", value, "EN 'game.moves.number' should be '#'");
    }

    @Test
    void englishBundle_hasScoresheetWhiteHeader() {
        I18n i18n = new I18n(I18n.Language.EN);
        String value = i18n.get("game.moves.white");
        assertNotNull(value, "EN key 'game.moves.white' must exist");
        assertFalse(value.isBlank(), "EN key 'game.moves.white' must not be blank");
        assertEquals("White", value, "EN 'game.moves.white' should be 'White'");
    }

    @Test
    void englishBundle_hasScoresheetBlackHeader() {
        I18n i18n = new I18n(I18n.Language.EN);
        String value = i18n.get("game.moves.black");
        assertNotNull(value, "EN key 'game.moves.black' must exist");
        assertFalse(value.isBlank(), "EN key 'game.moves.black' must not be blank");
        assertEquals("Black", value, "EN 'game.moves.black' should be 'Black'");
    }

    // ── German bundle ──────────────────────────────────────────────────────────

    @Test
    void germanBundle_hasScoresheetMoveNumberHeader() {
        I18n i18n = new I18n(I18n.Language.DE);
        String value = i18n.get("game.moves.number");
        assertNotNull(value, "DE key 'game.moves.number' must exist");
        assertFalse(value.isBlank(), "DE key 'game.moves.number' must not be blank");
        assertEquals("#", value, "DE 'game.moves.number' should be '#'");
    }

    @Test
    void germanBundle_hasScoresheetWhiteHeader() {
        I18n i18n = new I18n(I18n.Language.DE);
        String value = i18n.get("game.moves.white");
        assertNotNull(value, "DE key 'game.moves.white' must exist");
        assertFalse(value.isBlank(), "DE key 'game.moves.white' must not be blank");
        assertEquals("Wei\u00df", value, "DE 'game.moves.white' should be 'Weiß'");
    }

    @Test
    void germanBundle_hasScoresheetBlackHeader() {
        I18n i18n = new I18n(I18n.Language.DE);
        String value = i18n.get("game.moves.black");
        assertNotNull(value, "DE key 'game.moves.black' must exist");
        assertFalse(value.isBlank(), "DE key 'game.moves.black' must not be blank");
        assertEquals("Schwarz", value, "DE 'game.moves.black' should be 'Schwarz'");
    }
}
