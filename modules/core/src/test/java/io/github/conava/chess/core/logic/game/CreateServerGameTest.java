package io.github.conava.chess.core.logic.game;

import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Game#createServerGame(RulesetOptions, String, String)}.
 * <p>
 * Covered behaviours:
 * - createServerGame returns a non-null Game; after startGame() the state is RUNNING
 * - Player names supplied to the factory are visible via getPlayerWhite/getPlayerBlack
 * - Blank white name falls back to the default name (non-blank) supplied by Game superclass
 * - Blank black name falls back to the default name (non-blank) supplied by Game superclass
 */
class CreateServerGameTest {

    // ---- createServerGame returns a usable Game ----

    @Test
    void createServerGame_returnsValidGame() {
        Game game = Game.createServerGame(RulesetOptions.STANDARD, "Alice", "Bob");
        assertNotNull(game, "createServerGame must return a non-null Game instance");
    }

    @Test
    void createServerGame_afterStartGame_stateIsRunning() {
        Game game = Game.createServerGame(RulesetOptions.STANDARD, "Alice", "Bob");
        game.startGame();
        assertEquals(GameState.RUNNING, game.getState(), "After startGame(), a server game must be in RUNNING state");
    }

    // ---- Player names are passed through to the Game ----

    @Test
    void createServerGame_playerNamesSetCorrectly_whiteName() {
        Game game = Game.createServerGame(RulesetOptions.STANDARD, "Alice", "Bob");
        assertEquals("Alice", game.getPlayerWhite().name(), "getPlayerWhite().name() must equal the white player name supplied to createServerGame()");
    }

    @Test
    void createServerGame_playerNamesSetCorrectly_blackName() {
        Game game = Game.createServerGame(RulesetOptions.STANDARD, "Alice", "Bob");
        assertEquals("Bob", game.getPlayerBlack().name(), "getPlayerBlack().name() must equal the black player name supplied to createServerGame()");
    }

    // ---- Blank names use defaults (Game superclass substitution) ----

    @Test
    void createServerGame_blankWhiteName_usesDefault() {
        Game game = Game.createServerGame(RulesetOptions.STANDARD, "", "Bob");
        String whiteName = game.getPlayerWhite().name();
        assertNotNull(whiteName, "White player name must not be null");
        assertFalse(whiteName.isBlank(), "A blank white name must cause the Game superclass to substitute a non-blank default");
    }

    @Test
    void createServerGame_blankBlackName_usesDefault() {
        Game game = Game.createServerGame(RulesetOptions.STANDARD, "Alice", "");
        String blackName = game.getPlayerBlack().name();
        assertNotNull(blackName, "Black player name must not be null");
        assertFalse(blackName.isBlank(), "A blank black name must cause the Game superclass to substitute a non-blank default");
    }
}
