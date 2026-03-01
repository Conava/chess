package io.github.conava.chess.core.logic.game;

import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.data.io.MessageType;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the factory and default no-op methods added to Game in the
 * fix/application-architecture-violations branch (Tasks 1-2 of the plan).
 *
 * Covered behaviours:
 * - Game.createGame(false, ...) produces a non-null, working OfflineGame
 * - Game.createGame(true, ...)  produces a non-null OnlineGame via a stub connection
 * - Game.getJoinCode()          returns null for OfflineGame instances
 * - Game.connectToServerGame()  is a no-op (does not throw) for OfflineGame
 * - Game.handleMessage(Message) is a no-op (does not throw) for OfflineGame
 */
class GameFactoryTest {

    // ---- Minimal stub implementing ServerConnection so we can construct an OnlineGame ----

    private static class NoOpConnection implements ServerConnection {
        final List<String> sent = new ArrayList<>();

        @Override
        public void sendMessage(String message) {
            sent.add(message);
        }

        @Override
        public void closeConnection() {
            // no-op
        }

        @Override
        public boolean isConnected() {
            return true;
        }
    }

    // -------------------------------------------------------------------------
    // Game.createGame(false, ...) -- offline path
    // -------------------------------------------------------------------------

    @Test
    void testCreateOfflineGameViaFactory_returnsNonNull() {
        Game game = Game.createGame(false, RulesetOptions.STANDARD, "Alice", "Bob", null, null);
        assertNotNull(game, "createGame(false,...) must return a non-null Game instance");
    }

    @Test
    void testCreateOfflineGameViaFactory_afterStartGameStateIsRunning() {
        Game game = Game.createGame(false, RulesetOptions.STANDARD, "Alice", "Bob", null, null);
        game.startGame();
        assertEquals(GameState.RUNNING, game.getState(),
                "After startGame(), createGame(false,...) game state must be RUNNING");
    }

    @Test
    void testCreateOfflineGameViaFactory_getJoinCodeReturnsNull() {
        Game game = Game.createGame(false, RulesetOptions.STANDARD, "Alice", "Bob", null, null);
        assertNull(game.getJoinCode(),
                "createGame(false,...) must return a game whose getJoinCode() is null");
    }

    // -------------------------------------------------------------------------
    // Game.createGame(true, ...) -- online path
    // -------------------------------------------------------------------------

    @Test
    void testCreateOnlineGameViaFactory_returnsNonNull() {
        NoOpConnection conn = new NoOpConnection();
        Game game = Game.createGame(true, RulesetOptions.STANDARD, "Alice", "Bob", Map.of(), conn);
        assertNotNull(game, "createGame(true,...) must return a non-null Game instance");
    }

    @Test
    void testCreateOnlineGameViaFactory_getJoinCodeDoesNotThrow() {
        NoOpConnection conn = new NoOpConnection();
        Game game = Game.createGame(true, RulesetOptions.STANDARD, "Alice", "Bob", Map.of(), conn);
        assertDoesNotThrow(game::getJoinCode,
                "getJoinCode() on an OnlineGame created via the factory must not throw");
    }

    @Test
    void testCreateOnlineGameViaFactory_connectToServerGameDoesNotThrow() {
        NoOpConnection conn = new NoOpConnection();
        Game game = Game.createGame(true, RulesetOptions.STANDARD, "Alice", "Bob", Map.of(), conn);
        assertDoesNotThrow(game::connectToServerGame,
                "connectToServerGame() on an OnlineGame created via the factory must not throw");
    }

    // -------------------------------------------------------------------------
    // Game.getJoinCode() default -- returns null for OfflineGame
    // -------------------------------------------------------------------------

    @Test
    void testGetJoinCodeReturnsNullForOfflineGame() {
        OfflineGame offlineGame = new OfflineGame(RulesetOptions.STANDARD, "Alice", "Bob");
        assertNull(offlineGame.getJoinCode(),
                "OfflineGame.getJoinCode() must return null");
    }

    // -------------------------------------------------------------------------
    // Game.connectToServerGame() default no-op for OfflineGame
    // -------------------------------------------------------------------------

    @Test
    void testConnectToServerGameNoOpForOffline() {
        OfflineGame offlineGame = new OfflineGame(RulesetOptions.STANDARD, "Alice", "Bob");
        assertDoesNotThrow(offlineGame::connectToServerGame,
                "OfflineGame.connectToServerGame() must not throw (no-op default)");
    }

    // -------------------------------------------------------------------------
    // Game.handleMessage() default no-op for OfflineGame
    // -------------------------------------------------------------------------

    @Test
    void testHandleMessageNoOpForOffline() {
        OfflineGame offlineGame = new OfflineGame(RulesetOptions.STANDARD, "Alice", "Bob");
        Message msg = new Message(MessageType.SUCCESS, "move=accepted");
        assertDoesNotThrow(() -> offlineGame.handleMessage(msg),
                "OfflineGame.handleMessage() must not throw (no-op default)");
    }
}
