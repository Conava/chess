package io.github.conava.chess.core.logic.game;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.data.io.MessageType;
import io.github.conava.chess.core.data.pieces.*;
import io.github.conava.chess.core.exceptions.IllegalMoveException;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OnlineGame's interaction with ServerConnection (Task 6).
 * <p>
 * The ServerConnection interface was introduced to extract networking from core.
 * These tests verify the contract between OnlineGame and ServerConnection:
 * <p>
 * - OnlineGame calls sendMessage() when connectToServerGame() is invoked
 * - OnlineGame calls sendMessage() when a local player makes a move
 * - OnlineGame calls closeConnection() when endGame() is called
 * - OnlineGame does NOT call closeConnection() during normal game-play moves
 * <p>
 * A simple stub implementing ServerConnection records all calls without
 * any real network I/O.  No Mockito required.
 */
class OnlineGameServerConnectionTest {

    // ---- stub implementation ----

    static class RecordingConnection implements ServerConnection {
        final List<String> sentMessages = new ArrayList<>();
        int closeConnectionCallCount = 0;

        @Override
        public void sendMessage(String message) {
            sentMessages.add(message);
        }

        @Override
        public void closeConnection() {
            closeConnectionCallCount++;
        }

        @Override
        public boolean isConnected() {
            return true;
        }
    }

    private RecordingConnection connection;
    private OnlineGame game;

    @BeforeEach
    void setUp() {
        connection = new RecordingConnection();
        // No joinCode → creating a new game; local player is WHITE
        game = OnlineGame.create(RulesetOptions.STANDARD, "Alice", "Bob", new HashMap<>(), connection);
        game.connectToServerGame();
    }

    // ---- sendMessage() is called on connectToServerGame() ----

    @Test
    void connectToServerGame_sendsAtLeastOneMessage() {
        assertFalse(connection.sentMessages.isEmpty(), "OnlineGame must send at least one message to the server when connectToServerGame() is called");
    }

    @Test
    void connectToServerGame_sendsCreateGameMessage() {
        boolean hasCREATE_GAME = connection.sentMessages.stream().anyMatch(m -> m.startsWith("CREATE_GAME"));
        assertTrue(hasCREATE_GAME, "When no joinCode is provided, OnlineGame must send CREATE_GAME to the server when connectToServerGame() is called");
    }

    // ---- sendMessage() is called when local player makes a move ----

    @Test
    void movePiece_sendsMessageToServer() throws IllegalMoveException {
        int messagesBefore = connection.sentMessages.size();

        // White pawn e2→e3 (y=1,x=4 → y=2,x=4); local player is WHITE
        game.movePiece(new Square(1, 4), new Square(2, 4));

        assertTrue(connection.sentMessages.size() > messagesBefore, "Making a valid move must result in at least one additional message sent to the server");
    }

    @Test
    void movePiece_sendsMoveMessage() throws IllegalMoveException {
        game.movePiece(new Square(1, 4), new Square(2, 4));

        boolean hasMOVE = connection.sentMessages.stream().anyMatch(m -> m.startsWith("MOVE"));
        assertTrue(hasMOVE, "After a local move, a MOVE message must be sent to the server");
    }

    // ---- closeConnection() is called on endGame() ----

    @Test
    void endGame_callsCloseConnection() {
        game.endGame();
        assertEquals(1, connection.closeConnectionCallCount, "endGame() must call closeConnection() exactly once");
    }

    @Test
    void endGame_sendsGameStatusMessage() {
        game.endGame();
        boolean hasGAME_STATUS = connection.sentMessages.stream().anyMatch(m -> m.startsWith("GAME_STATUS"));
        assertTrue(hasGAME_STATUS, "endGame() must send a GAME_STATUS message to the server before closing");
    }

    // ---- closeConnection() is NOT called during normal play ----

    @Test
    void movePiece_doesNotCallCloseConnection() throws IllegalMoveException {
        game.movePiece(new Square(1, 4), new Square(2, 4));
        assertEquals(0, connection.closeConnectionCallCount, "Making a move must not call closeConnection()");
    }

    // ---- joining an existing game sends JOIN_GAME message ----

    @Test
    void construction_withJoinCode_sendsJoinGameMessage() {
        RecordingConnection joiningConn = new RecordingConnection();
        Map<String, String> settings = new HashMap<>();
        settings.put("joinCode", "ABC123");
        OnlineGame joiningGame = OnlineGame.create(RulesetOptions.STANDARD, "Alice", "Bob", settings, joiningConn);
        joiningGame.connectToServerGame();

        boolean hasJOIN_GAME = joiningConn.sentMessages.stream().anyMatch(m -> m.startsWith("JOIN_GAME"));
        assertTrue(hasJOIN_GAME, "When a joinCode is provided, OnlineGame must send JOIN_GAME to the server");
    }

    // ---- handleMessage() GAME_STATUS propagates to observers ----

    @Test
    void handleMessage_gameStatus_notifiesObservers() {
        CapturingObserver observer = new CapturingObserver();
        game.addObserver(observer);

        // RUNNING is a valid GameState enum value
        Message gameStatusMsg = new Message(MessageType.GAME_STATUS, "gameState=RUNNING");
        game.handleMessage(gameStatusMsg);

        assertTrue(observer.notified,
                "onGameStateChanged() must be called when a GAME_STATUS message is received");
    }

    // ---- handleMessage() dispatches JOIN_CODE without throwing ----

    @Test
    void handleMessage_joinCode_updatesJoinCode() {
        Message msg = new Message(MessageType.JOIN_CODE, "joinCode=XYZ789");
        assertDoesNotThrow(() -> game.handleMessage(msg), "handleMessage() must not throw for JOIN_CODE messages");
        assertEquals("XYZ789", game.getJoinCode(), "Join code must be updated when JOIN_CODE message is received");
    }

    // ---- handleMessage() FAILURE path restores state and notifies observers ----

    @Test
    void handleMessage_failure_withMoveRejected_notifiesObservers() {
        // Backup state by making a move first (backup is taken in executeMove)
        game.backupGameState();

        CapturingObserver observer = new CapturingObserver();
        game.addObserver(observer);

        Message failureMsg = new Message(MessageType.FAILURE, "move=rejected");
        game.handleMessage(failureMsg);

        assertTrue(observer.notified, "onGameStateChanged() must be called when the server rejects a move");
    }

    // ---- handleMessage() MOVE path with malformed move does not crash ----

    @Test
    void handleMessage_malformedMove_doesNotThrow() {
        // The local player is WHITE; send a MOVE message attributed to BLACK so handleMove()
        // does not skip it via the early-return guard.  The MOVE_PARAM value is intentionally
        // garbled so Move.fromString() will throw a RuntimeException internally.
        Message malformedMsg = new Message(MessageType.MOVE,
                "move=THIS_IS_NOT_A_VALID_MOVE playerColor=BLACK");
        assertDoesNotThrow(() -> game.handleMessage(malformedMsg),
                "A malformed MOVE message must not propagate any exception out of handleMessage()");
    }

    // ---- OnlineGame.create() must not send any messages before connectToServerGame() ----

    @Test
    void construction_doesNotSendMessage() {
        RecordingConnection freshConn = new RecordingConnection();
        // Construct only — do NOT call connectToServerGame()
        OnlineGame.create(RulesetOptions.STANDARD, "Alice", "Bob", new java.util.HashMap<>(), freshConn);
        assertEquals(0, freshConn.sentMessages.size(),
                "OnlineGame.create() must not send any messages before connectToServerGame() is called");
    }

    // ---- backupGameState / restoreGameState cover halfMoveClock and positionHistory ----

    /** Reads a protected field from Game via reflection. */
    @SuppressWarnings("unchecked")
    private static <T> T readGameField(OnlineGame g, String fieldName) throws Exception {
        java.lang.reflect.Field f = Game.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (T) f.get(g);
    }

    /** Writes a protected field on Game via reflection. */
    private static void writeGameField(OnlineGame g, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field f = Game.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(g, value);
    }

    private static OnlineGame freshOnlineGame() {
        RecordingConnection conn = new RecordingConnection();
        return OnlineGame.create(RulesetOptions.STANDARD, "Alice", "Bob", new HashMap<>(), conn);
    }

    @Test
    void backupAndRestore_preserves_halfMoveClock() throws Exception {
        OnlineGame g = freshOnlineGame();

        writeGameField(g, "halfMoveClock", 7);
        g.backupGameState();

        // Mutate the field after backup.
        writeGameField(g, "halfMoveClock", 42);

        g.restoreGameState();

        int restored = readGameField(g, "halfMoveClock");
        assertEquals(7, restored,
                "restoreGameState() must restore halfMoveClock to the backed-up value");
    }

    @Test
    void backupAndRestore_preserves_positionHistory() throws Exception {
        OnlineGame g = freshOnlineGame();

        Map<String, Integer> history = readGameField(g, "positionHistory");
        history.put("KEY_A", 2);
        g.backupGameState();

        // Mutate the map after backup.
        history.put("KEY_B", 5);
        history.remove("KEY_A");

        g.restoreGameState();

        Map<String, Integer> restored = readGameField(g, "positionHistory");
        assertTrue(restored.containsKey("KEY_A"),
                "restoreGameState() must restore all backed-up position history entries");
        assertFalse(restored.containsKey("KEY_B"),
                "restoreGameState() must not include entries added after the backup");
        assertEquals(2, restored.get("KEY_A"),
                "restoreGameState() must restore the correct count for each position history entry");
    }

    @Test
    void backup_positionHistory_isDeepCopy() throws Exception {
        OnlineGame g = freshOnlineGame();

        Map<String, Integer> history = readGameField(g, "positionHistory");
        history.put("KEY_A", 1);
        g.backupGameState();

        // Mutating the live map after backup must NOT affect the backup.
        history.put("KEY_A", 99);

        g.restoreGameState();

        Map<String, Integer> restored = readGameField(g, "positionHistory");
        assertEquals(1, restored.get("KEY_A"),
                "The backed-up positionHistory must be a deep copy; mutating the live map after backup must not affect the restored value");
    }

    // ---- simple capturing observer ----

    private static class CapturingObserver implements io.github.conava.chess.core.logic.observer.GameObserver {
        boolean notified = false;

        @Override
        public void onGameStateChanged() {
            notified = true;
        }
    }
}
