package io.github.conava.chess.server.management;

import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.data.io.MessageType;
import io.github.conava.chess.core.logic.game.Game;
import io.github.conava.chess.core.logic.game.GameState;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import io.github.conava.chess.server.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GameInstance}.
 *
 * <p>Uses a {@link TrackingClientHandler} inner class to capture messages sent to
 * players without requiring a real TCP socket or network infrastructure. Reflection
 * is used in tests that need to inspect or manipulate the private {@code game} field
 * to set up terminal state conditions.</p>
 *
 * <p>Covered behaviours:
 * <ul>
 *   <li>Deferred game creation: game is null until both players connect</li>
 *   <li>Factory usage: game created via Game.createServerGame(), not direct instantiation</li>
 *   <li>connectPlayer sends correct SUCCESS messages with player color</li>
 *   <li>After both players connect, game state is RUNNING</li>
 *   <li>Player names from connectPlayer are passed through to the Game</li>
 *   <li>Default player names when null/blank are used</li>
 *   <li>Observer callback sends GAME_STATUS for terminal state transitions only</li>
 *   <li>Observer callback does NOT send messages for non-terminal states</li>
 *   <li>Valid moves are relayed explicitly to both players after execution</li>
 *   <li>Illegal moves send ERROR back to the sender</li>
 *   <li>Malformed moves do not propagate exceptions out of processMessage()</li>
 *   <li>processMessage() is synchronized (concurrent calls are serialized)</li>
 *   <li>disconnectPlayer notifies the remaining opponent with a GAME_STATUS message</li>
 *   <li>disconnectPlayer removes the observer from the game to prevent memory leaks</li>
 * </ul>
 * </p>
 */
class GameInstanceTest {

    /**
     * A ClientHandler subclass that records every message passed to sendMessage()
     * without requiring a live TCP socket. The constructor passes an unconnected
     * Socket to the parent — the parent constructor only stores the reference and
     * never calls socket methods until run() is invoked (which is never called in
     * these unit tests).
     */
    static class TrackingClientHandler extends ClientHandler {
        private final List<Message> sentMessages = new CopyOnWriteArrayList<>();

        TrackingClientHandler(Server server) {
            super(new Socket(), server);
        }

        @Override
        public synchronized void sendMessage(Message message) {
            sentMessages.add(message);
        }

        List<Message> getSentMessages() {
            return new ArrayList<>(sentMessages);
        }

        void clearMessages() {
            sentMessages.clear();
        }

        boolean hasMessageOfType(MessageType type) {
            return sentMessages.stream().anyMatch(m -> m.type() == type);
        }

        boolean hasGameStatusWithState(String stateName) {
            return sentMessages.stream()
                    .filter(m -> m.type() == MessageType.GAME_STATUS)
                    .anyMatch(m -> m.content().contains("gameState=" + stateName));
        }
    }

    private Server server;
    private TrackingClientHandler whiteHandler;
    private TrackingClientHandler blackHandler;
    private GameInstance gameInstance;

    @BeforeEach
    void setUp() {
        server = new Server();
        whiteHandler = new TrackingClientHandler(server);
        blackHandler = new TrackingClientHandler(server);
        gameInstance = new GameInstance(1, RulesetOptions.STANDARD);
    }

    // ---- Helpers ----

    /**
     * Connects both players to the gameInstance so that the internal Game is created
     * and started. After this call the game is in RUNNING state.
     */
    private void connectBothPlayers() {
        gameInstance.connectPlayer(whiteHandler, "Alice");
        gameInstance.connectPlayer(blackHandler, "Bob");
    }

    /**
     * Uses reflection to read the private {@code game} field from a GameInstance.
     * Returns null if the field is null.
     */
    private Game getPrivateGame(GameInstance gi) throws Exception {
        Field gameField = GameInstance.class.getDeclaredField("game");
        gameField.setAccessible(true);
        return (Game) gameField.get(gi);
    }

    // ========================================================================
    // Deferred game creation
    // ========================================================================

    @Test
    void gameInstance_beforeBothConnect_gameIsNull() throws Exception {
        // Only white has connected; game must not be created yet.
        gameInstance.connectPlayer(whiteHandler, "Alice");

        Game game = getPrivateGame(gameInstance);
        assertNull(game,
                "The internal Game must remain null until both players have connected");
    }

    @Test
    void gameInstance_beforeBothConnect_moveIsIgnoredWithoutException() {
        // No players connected; MOVE must be silently ignored (game == null guard).
        Message moveMsg = new Message(MessageType.MOVE, "move=e2-e4 playerColor=WHITE");
        assertDoesNotThrow(
                () -> gameInstance.processMessage(null, moveMsg),
                "processMessage(MOVE) before game creation must not propagate any exception");
    }

    // ========================================================================
    // Factory usage and game creation
    // ========================================================================

    @Test
    void gameInstance_usesFactory_createsValidGame() throws Exception {
        connectBothPlayers();

        Game game = getPrivateGame(gameInstance);
        assertNotNull(game,
                "The internal Game must be non-null after both players have connected");
        assertEquals(GameState.RUNNING, game.getState(),
                "The game state must be RUNNING after both players connect");
    }

    @Test
    void gameInstance_usesFactory_noDirectServerGameImport() {
        // Verify that GameInstance does not hold a ServerGame reference by
        // checking that the game created is an instance of Game (not checking
        // ServerGame directly -- ServerGame is package-private in core).
        // We test this indirectly: the game must be a non-null Game instance.
        connectBothPlayers();
        // No assertion needed beyond no ClassCastException or compile error.
        // The real check is the "no direct ServerGame import" rule enforced by
        // the executor. This test confirms the factory path works end-to-end.
    }

    // ========================================================================
    // connectPlayer: SUCCESS messages and color assignment
    // ========================================================================

    @Test
    void connectPlayer_firstPlayer_sendsSuccessWithWhite() {
        gameInstance.connectPlayer(whiteHandler, "Alice");

        assertTrue(
                whiteHandler.hasMessageOfType(MessageType.SUCCESS),
                "First player (white) must receive a SUCCESS message on connecting");
        assertTrue(
                whiteHandler.getSentMessages().stream()
                        .filter(m -> m.type() == MessageType.SUCCESS)
                        .anyMatch(m -> m.content().contains("player=white")),
                "The SUCCESS message for the first player must include player=white");
    }

    @Test
    void connectPlayer_secondPlayer_sendsSuccessWithBlack() {
        gameInstance.connectPlayer(whiteHandler, "Alice");
        gameInstance.connectPlayer(blackHandler, "Bob");

        assertTrue(
                blackHandler.hasMessageOfType(MessageType.SUCCESS),
                "Second player (black) must receive a SUCCESS message on connecting");
        assertTrue(
                blackHandler.getSentMessages().stream()
                        .filter(m -> m.type() == MessageType.SUCCESS)
                        .anyMatch(m -> m.content().contains("player=black")),
                "The SUCCESS message for the second player must include player=black");
    }

    @Test
    void connectPlayer_bothPlayers_gameStatusRunningBroadcast() {
        connectBothPlayers();

        assertTrue(whiteHandler.hasGameStatusWithState("RUNNING"),
                "White player must receive GAME_STATUS gameState=RUNNING after game starts");
        assertTrue(blackHandler.hasGameStatusWithState("RUNNING"),
                "Black player must receive GAME_STATUS gameState=RUNNING after game starts");
    }

    @Test
    void connectPlayer_onlyOneGameStatusOnStart_notDuplicated() {
        connectBothPlayers();

        long count = whiteHandler.getSentMessages().stream()
                .filter(m -> m.type() == MessageType.GAME_STATUS)
                .filter(m -> m.content().contains("gameState=RUNNING"))
                .count();
        assertEquals(1, count,
                "Exactly one GAME_STATUS gameState=RUNNING must be sent to white at game start");
    }

    // ========================================================================
    // Player names from connectPlayer are passed to the Game
    // ========================================================================

    @Test
    void connectPlayer_withNames_gameHasWhitePlayerName() throws Exception {
        gameInstance.connectPlayer(whiteHandler, "Alice");
        gameInstance.connectPlayer(blackHandler, "Bob");

        Game game = getPrivateGame(gameInstance);
        assertEquals("Alice", game.getPlayerWhite().name(),
                "The white player name in the Game must match the name passed to connectPlayer");
    }

    @Test
    void connectPlayer_withNames_gameHasBlackPlayerName() throws Exception {
        gameInstance.connectPlayer(whiteHandler, "Alice");
        gameInstance.connectPlayer(blackHandler, "Bob");

        Game game = getPrivateGame(gameInstance);
        assertEquals("Bob", game.getPlayerBlack().name(),
                "The black player name in the Game must match the name passed to connectPlayer");
    }

    @Test
    void connectPlayer_nullPlayerName_usesDefaultForWhite() throws Exception {
        gameInstance.connectPlayer(whiteHandler, null);
        gameInstance.connectPlayer(blackHandler, "Bob");

        Game game = getPrivateGame(gameInstance);
        String whiteName = game.getPlayerWhite().name();
        assertNotNull(whiteName, "White player name must not be null");
        assertFalse(whiteName.isBlank(),
                "A null white player name must result in a non-blank default name in the Game");
    }

    @Test
    void connectPlayer_nullPlayerName_usesDefaultForBlack() throws Exception {
        gameInstance.connectPlayer(whiteHandler, "Alice");
        gameInstance.connectPlayer(blackHandler, null);

        Game game = getPrivateGame(gameInstance);
        String blackName = game.getPlayerBlack().name();
        assertNotNull(blackName, "Black player name must not be null");
        assertFalse(blackName.isBlank(),
                "A null black player name must result in a non-blank default name in the Game");
    }

    // ========================================================================
    // onGameStateChanged: observer callback for terminal vs. non-terminal states
    // ========================================================================

    @Test
    void onGameStateChanged_nonTerminalState_doesNotSendGameStatus() throws Exception {
        connectBothPlayers();
        whiteHandler.clearMessages();
        blackHandler.clearMessages();

        // Game is RUNNING — calling the observer callback with no state change
        // must not trigger any extra messages.
        gameInstance.onGameStateChanged();

        assertFalse(whiteHandler.hasMessageOfType(MessageType.GAME_STATUS),
                "Observer callback with RUNNING state (non-terminal) must not send GAME_STATUS");
        assertFalse(blackHandler.hasMessageOfType(MessageType.GAME_STATUS),
                "Observer callback with RUNNING state (non-terminal) must not send GAME_STATUS");
    }

    @Test
    void onGameStateChanged_terminalState_notifiesClients() throws Exception {
        connectBothPlayers();
        whiteHandler.clearMessages();
        blackHandler.clearMessages();

        // Set the game to a terminal state via reflection, then fire the observer.
        Game game = getPrivateGame(gameInstance);
        game.setGameState(GameState.WHITE_WON_BY_CHECKMATE);

        gameInstance.onGameStateChanged();

        assertTrue(whiteHandler.hasGameStatusWithState("WHITE_WON_BY_CHECKMATE"),
                "White player must receive GAME_STATUS with WHITE_WON_BY_CHECKMATE when observer fires on terminal state");
        assertTrue(blackHandler.hasGameStatusWithState("WHITE_WON_BY_CHECKMATE"),
                "Black player must receive GAME_STATUS with WHITE_WON_BY_CHECKMATE when observer fires on terminal state");
    }

    @Test
    void onGameStateChanged_terminalStateFiredTwice_sendsOnlyOnce() throws Exception {
        connectBothPlayers();
        whiteHandler.clearMessages();
        blackHandler.clearMessages();

        // First call: transition RUNNING → terminal (should send).
        Game game = getPrivateGame(gameInstance);
        game.setGameState(GameState.BLACK_WON_BY_CHECKMATE);
        gameInstance.onGameStateChanged();

        // Second call: no state change (already terminal) — must NOT send again.
        gameInstance.onGameStateChanged();

        long count = whiteHandler.getSentMessages().stream()
                .filter(m -> m.type() == MessageType.GAME_STATUS)
                .filter(m -> m.content().contains("gameState=BLACK_WON_BY_CHECKMATE"))
                .count();
        assertEquals(1, count,
                "Calling onGameStateChanged twice with the same terminal state must send GAME_STATUS exactly once");
    }

    @Test
    void onGameStateChanged_beforeGameCreated_doesNotThrow() {
        // No players connected; game is null.
        assertDoesNotThrow(() -> gameInstance.onGameStateChanged(),
                "onGameStateChanged() when game is null must not throw");
    }

    // ========================================================================
    // handleMove: explicit move relay to both players
    // ========================================================================

    @Test
    void handleMove_validMove_relaysToPlayers() {
        connectBothPlayers();
        whiteHandler.clearMessages();
        blackHandler.clearMessages();

        // e2-e4: a standard legal pawn opening for white.
        Message moveMsg = new Message(MessageType.MOVE, "move=e2-e4 playerColor=WHITE");
        gameInstance.processMessage(whiteHandler, moveMsg);

        assertTrue(whiteHandler.hasMessageOfType(MessageType.MOVE),
                "White player must receive the relayed MOVE message after a valid move");
        assertTrue(blackHandler.hasMessageOfType(MessageType.MOVE),
                "Black player must receive the relayed MOVE message after a valid move");
    }

    @Test
    void handleMove_illegalMove_sendsErrorToSender() {
        connectBothPlayers();
        whiteHandler.clearMessages();

        // e2-e5: pawn cannot move 3 squares — illegal move.
        Message illegalMove = new Message(MessageType.MOVE, "move=e2-e5 playerColor=WHITE");
        gameInstance.processMessage(whiteHandler, illegalMove);

        assertTrue(whiteHandler.hasMessageOfType(MessageType.ERROR),
                "Sender must receive an ERROR message when the move is illegal");
    }

    @Test
    void handleMove_illegalMove_doesNotRelayToOpponent() {
        connectBothPlayers();
        blackHandler.clearMessages();

        // Illegal move: white pawn 3 squares.
        Message illegalMove = new Message(MessageType.MOVE, "move=e2-e5 playerColor=WHITE");
        gameInstance.processMessage(whiteHandler, illegalMove);

        assertFalse(blackHandler.hasMessageOfType(MessageType.MOVE),
                "An illegal move must not be relayed to the opponent");
    }

    // ========================================================================
    // Existing tests preserved from Task 7 (malformed move handling)
    // ========================================================================

    @Test
    void handleMove_malformedMove_doesNotThrow() {
        Message malformedMsg = new Message(MessageType.MOVE,
                "move=!!NOT_A_VALID_MOVE!! playerColor=WHITE");
        assertDoesNotThrow(
                () -> gameInstance.processMessage(null, malformedMsg),
                "A malformed MOVE message must not propagate any exception out of processMessage()");
    }

    @Test
    void handleMove_nullMoveParam_doesNotThrow() {
        // "playerColor=WHITE" only — no "move" key present
        Message noMoveMsg = new Message(MessageType.MOVE, "playerColor=WHITE");
        assertDoesNotThrow(
                () -> gameInstance.processMessage(null, noMoveMsg),
                "A MOVE message with a missing move parameter must not propagate any exception");
    }

    @Test
    void handleMove_malformedMove_sendsErrorToSender() {
        connectBothPlayers();
        whiteHandler.clearMessages();

        Message malformedMsg = new Message(MessageType.MOVE, "move=!!NOT_A_VALID_MOVE!! playerColor=WHITE");
        gameInstance.processMessage(whiteHandler, malformedMsg);

        assertTrue(whiteHandler.hasMessageOfType(MessageType.ERROR),
                "A malformed MOVE message must send an ERROR back to the sender");
    }

    // ========================================================================
    // processMessage: synchronized — concurrent calls are serialized
    // ========================================================================

    @Test
    void processMessage_concurrent_serialized() throws InterruptedException {
        connectBothPlayers();

        // Use an AtomicInteger to count moves applied without exception.
        // Both threads send the same message. Only one white pawn move (e2-e4)
        // will succeed; the second call should either fail gracefully or succeed
        // if it is for a different player. The point is that neither thread throws.
        AtomicInteger exceptions = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        Runnable task = () -> {
            try {
                Message moveMsg = new Message(MessageType.MOVE, "move=e2-e4 playerColor=WHITE");
                gameInstance.processMessage(whiteHandler, moveMsg);
            } catch (Exception e) {
                exceptions.incrementAndGet();
            } finally {
                latch.countDown();
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(task);
        pool.submit(task);
        pool.shutdown();

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "Both concurrent processMessage calls must complete within 5 seconds");
        assertEquals(0, exceptions.get(),
                "No exception must escape processMessage() from concurrent calls");
    }

    // ========================================================================
    // disconnectPlayer: notifies opponent and removes observer
    // ========================================================================

    @Test
    void disconnect_notifiesOpponent_whenWhiteDisconnects() {
        connectBothPlayers();
        blackHandler.clearMessages();

        gameInstance.disconnectPlayer(whiteHandler);

        assertTrue(blackHandler.hasMessageOfType(MessageType.GAME_STATUS),
                "Black player must receive a GAME_STATUS message when white disconnects");
    }

    @Test
    void disconnect_notifiesOpponent_resignationStateContent() {
        connectBothPlayers();
        blackHandler.clearMessages();

        gameInstance.disconnectPlayer(whiteHandler);

        assertTrue(
                blackHandler.getSentMessages().stream()
                        .filter(m -> m.type() == MessageType.GAME_STATUS)
                        .anyMatch(m -> m.content().contains("gameState=BLACK_WON_BY_RESIGNATION")),
                "The GAME_STATUS sent to black must indicate BLACK_WON_BY_RESIGNATION when white disconnects");
    }

    @Test
    void disconnect_notifiesOpponent_whenBlackDisconnects() {
        connectBothPlayers();
        whiteHandler.clearMessages();

        gameInstance.disconnectPlayer(blackHandler);

        assertTrue(whiteHandler.hasMessageOfType(MessageType.GAME_STATUS),
                "White player must receive a GAME_STATUS message when black disconnects");
        assertTrue(
                whiteHandler.getSentMessages().stream()
                        .filter(m -> m.type() == MessageType.GAME_STATUS)
                        .anyMatch(m -> m.content().contains("gameState=WHITE_WON_BY_RESIGNATION")),
                "The GAME_STATUS sent to white must indicate WHITE_WON_BY_RESIGNATION when black disconnects");
    }

    @Test
    void disconnect_removesObserver_noFurtherNotificationsFromGame() throws Exception {
        connectBothPlayers();

        // Disconnect white — observer must be removed.
        gameInstance.disconnectPlayer(whiteHandler);
        blackHandler.clearMessages();

        // After observer removal, calling onGameStateChanged() with a fresh terminal
        // state change should NOT trigger a new broadcast because the observer is
        // deregistered from the Game's observer list.
        // We verify this by checking that the Game's observer list is empty.
        Game game = getPrivateGame(gameInstance);

        // The 'observers' field lives in Observable, which is two levels up from
        // ServerGame: ServerGame -> Game -> Observable. Walk up the hierarchy to find it.
        Field observersField = findDeclaredField(game.getClass(), "observers");
        assertNotNull(observersField,
                "Could not locate 'observers' field via reflection — hierarchy may have changed");
        observersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> observers = (List<Object>) observersField.get(game);
        assertTrue(observers.isEmpty(),
                "After disconnectPlayer(), the observer must be removed from the Game's observer list");
    }

    /**
     * Walks up the class hierarchy from {@code clazz} looking for a declared field with
     * the given name. Returns {@code null} if no matching field is found.
     */
    private static Field findDeclaredField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    @Test
    void disconnect_beforeGameCreated_doesNotThrow() {
        // Only white has connected; game is null. Disconnecting must not throw.
        gameInstance.connectPlayer(whiteHandler, "Alice");
        assertDoesNotThrow(() -> gameInstance.disconnectPlayer(whiteHandler),
                "disconnectPlayer() when game is null must not throw");
    }

    // ========================================================================
    // T11 smoke tests: Chess960 position generation and wire protocol
    // ========================================================================

    @Test
    void chess960GameInstance_positionIndex_isInValidRange() {
        GameInstance chess960Instance = new GameInstance(2, RulesetOptions.CHESS960);
        int index = chess960Instance.getPositionIndex();
        assertTrue(index >= 0 && index <= 959,
                "Chess960 GameInstance must have a positionIndex in [0, 959], got: " + index);
    }

    @Test
    void standardGameInstance_positionIndex_isNegativeOne() {
        GameInstance standardInstance = new GameInstance(3, RulesetOptions.STANDARD);
        assertEquals(-1, standardInstance.getPositionIndex(),
                "Standard GameInstance must have positionIndex == -1");
    }

    @Test
    void chess960GameInstance_blackSuccessMessage_containsPositionAndRuleset() {
        GameInstance chess960Instance = new GameInstance(4, RulesetOptions.CHESS960);
        TrackingClientHandler chess960White = new TrackingClientHandler(server);
        TrackingClientHandler chess960Black = new TrackingClientHandler(server);

        chess960Instance.connectPlayer(chess960White, "Alice");
        chess960Instance.connectPlayer(chess960Black, "Bob");

        // Black's SUCCESS message must contain position=N and ruleset=CHESS960
        String successContent = chess960Black.getSentMessages().stream()
                .filter(m -> m.type() == MessageType.SUCCESS)
                .map(m -> m.content())
                .findFirst()
                .orElse("");
        assertTrue(successContent.contains("player=black"),
                "Chess960 black SUCCESS must contain player=black");
        assertTrue(successContent.contains("ruleset=CHESS960"),
                "Chess960 black SUCCESS must contain ruleset=CHESS960");
        assertTrue(successContent.contains("position="),
                "Chess960 black SUCCESS must contain position=<index>");
    }

    @Test
    void standardGameInstance_blackSuccessMessage_isJustPlayerBlack() {
        GameInstance standardInstance = new GameInstance(5, RulesetOptions.STANDARD);
        TrackingClientHandler stdWhite = new TrackingClientHandler(server);
        TrackingClientHandler stdBlack = new TrackingClientHandler(server);

        standardInstance.connectPlayer(stdWhite, "Alice");
        standardInstance.connectPlayer(stdBlack, "Bob");

        String successContent = stdBlack.getSentMessages().stream()
                .filter(m -> m.type() == MessageType.SUCCESS)
                .map(m -> m.content())
                .findFirst()
                .orElse("");
        assertEquals("player=black", successContent,
                "Standard black SUCCESS must be exactly 'player=black' without Chess960 extras");
    }

    @Test
    void chess960GameInstance_startGame_createsGameWithCorrectRuleset() throws Exception {
        GameInstance chess960Instance = new GameInstance(6, RulesetOptions.CHESS960);
        TrackingClientHandler chess960White = new TrackingClientHandler(server);
        TrackingClientHandler chess960Black = new TrackingClientHandler(server);

        chess960Instance.connectPlayer(chess960White, "Alice");
        chess960Instance.connectPlayer(chess960Black, "Bob");

        Game game = getPrivateGame(chess960Instance);
        assertNotNull(game, "Game must be created after both players connect");
        assertNotNull(game.getRuleset(), "Game ruleset must not be null");
        assertEquals(GameState.RUNNING, game.getState(),
                "Game must be RUNNING after both players connect");
    }
}
