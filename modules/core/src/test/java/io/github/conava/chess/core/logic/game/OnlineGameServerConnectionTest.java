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
 *
 * The ServerConnection interface was introduced to extract networking from core.
 * These tests verify the contract between OnlineGame and ServerConnection:
 *
 *  - OnlineGame calls sendMessage() on construction (to create/join the game)
 *  - OnlineGame calls sendMessage() when a local player makes a move
 *  - OnlineGame calls closeConnection() when endGame() is called
 *  - OnlineGame does NOT call closeConnection() during normal game-play moves
 *
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

    /**
     * Subclass of OnlineGame that exposes the protected board so we can
     * place pieces for move tests.
     */
    static class TestOnlineGame extends OnlineGame {
        TestOnlineGame(ServerConnection conn) {
            super(RulesetOptions.STANDARD, "Alice", "Bob", new HashMap<>(), conn);
        }

        void clearSquare(int y, int x) {
            board.getSquare(y, x).setPiece(null);
        }

        void placePiece(int y, int x, Piece piece) {
            board.getSquare(y, x).setPiece(piece);
        }
    }

    private RecordingConnection connection;
    private TestOnlineGame game;

    @BeforeEach
    void setUp() {
        connection = new RecordingConnection();
        // No joinCode → creating a new game; local player is WHITE
        game = new TestOnlineGame(connection);
    }

    // ---- sendMessage() is called on construction ----

    @Test
    void construction_sendsAtLeastOneMessage() {
        assertFalse(connection.sentMessages.isEmpty(),
                "OnlineGame must send at least one message to the server on construction");
    }

    @Test
    void construction_sendsCreateGameMessage() {
        boolean hasCREATE_GAME = connection.sentMessages.stream()
                .anyMatch(m -> m.startsWith("CREATE_GAME"));
        assertTrue(hasCREATE_GAME,
                "When no joinCode is provided, OnlineGame must send CREATE_GAME to the server");
    }

    // ---- sendMessage() is called when local player makes a move ----

    @Test
    void movePiece_sendsMessageToServer() throws IllegalMoveException {
        int messagesBefore = connection.sentMessages.size();

        // White pawn e2→e3 (y=1,x=4 → y=2,x=4); local player is WHITE
        game.movePiece(new Square(1, 4), new Square(2, 4));

        assertTrue(connection.sentMessages.size() > messagesBefore,
                "Making a valid move must result in at least one additional message sent to the server");
    }

    @Test
    void movePiece_sendsMoveMessage() throws IllegalMoveException {
        game.movePiece(new Square(1, 4), new Square(2, 4));

        boolean hasMOVE = connection.sentMessages.stream()
                .anyMatch(m -> m.startsWith("MOVE"));
        assertTrue(hasMOVE,
                "After a local move, a MOVE message must be sent to the server");
    }

    // ---- closeConnection() is called on endGame() ----

    @Test
    void endGame_callsCloseConnection() {
        game.endGame();
        assertEquals(1, connection.closeConnectionCallCount,
                "endGame() must call closeConnection() exactly once");
    }

    @Test
    void endGame_sendsGameStatusMessage() {
        game.endGame();
        boolean hasGAME_STATUS = connection.sentMessages.stream()
                .anyMatch(m -> m.startsWith("GAME_STATUS"));
        assertTrue(hasGAME_STATUS,
                "endGame() must send a GAME_STATUS message to the server before closing");
    }

    // ---- closeConnection() is NOT called during normal play ----

    @Test
    void movePiece_doesNotCallCloseConnection() throws IllegalMoveException {
        game.movePiece(new Square(1, 4), new Square(2, 4));
        assertEquals(0, connection.closeConnectionCallCount,
                "Making a move must not call closeConnection()");
    }

    // ---- joining an existing game sends JOIN_GAME message ----

    @Test
    void construction_withJoinCode_sendsJoinGameMessage() {
        RecordingConnection joiningConn = new RecordingConnection();
        Map<String, String> settings = new HashMap<>();
        settings.put("joinCode", "ABC123");
        OnlineGame joiningGame = new OnlineGame(
                RulesetOptions.STANDARD, "Alice", "Bob", settings, joiningConn);

        boolean hasJOIN_GAME = joiningConn.sentMessages.stream()
                .anyMatch(m -> m.startsWith("JOIN_GAME"));
        assertTrue(hasJOIN_GAME,
                "When a joinCode is provided, OnlineGame must send JOIN_GAME to the server");
    }

    // ---- handleMessage() dispatches JOIN_CODE without throwing ----

    @Test
    void handleMessage_joinCode_updatesJoinCode() {
        Message msg = new Message(MessageType.JOIN_CODE, "joinCode=XYZ789");
        assertDoesNotThrow(() -> game.handleMessage(msg),
                "handleMessage() must not throw for JOIN_CODE messages");
        assertEquals("XYZ789", game.getJoinCode(),
                "Join code must be updated when JOIN_CODE message is received");
    }

    // ---- handleMessage() FAILURE path restores state and notifies observers ----

    @Test
    void handleMessage_failure_withMoveRejected_notifiesObservers() throws IllegalMoveException {
        // Backup state by making a move first (backup is taken in executeMove)
        game.backupGameState();

        CapturingObserver observer = new CapturingObserver();
        game.addObserver(observer);

        Message failureMsg = new Message(MessageType.FAILURE, "move=rejected");
        game.handleMessage(failureMsg);

        assertTrue(observer.notified,
                "onGameStateChanged() must be called when the server rejects a move");
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
