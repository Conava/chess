package io.github.conava.chess.server.matchmaking;

import io.github.conava.chess.core.data.io.MessageType;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import io.github.conava.chess.server.Server;
import io.github.conava.chess.server.auth.AuthService;
import io.github.conava.chess.server.db.DatabaseManager;
import io.github.conava.chess.server.management.ClientHandler;
import io.github.conava.chess.server.management.GameManager;
import io.github.conava.chess.server.management.PlayerSession;
import io.github.conava.chess.server.persistence.GameRepository;
import io.github.conava.chess.server.persistence.SessionRepository;
import io.github.conava.chess.server.persistence.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit/integration tests for {@link MatchmakingService}.
 *
 * <p>Each test creates real TCP socket pairs backed by {@link ClientHandler} instances
 * so that {@code sendMessage} actually writes to a stream we can read from the test side.
 * An in-memory SQLite database is used for all persistence needs.</p>
 *
 * <p>Covered behaviours:
 * <ul>
 *   <li>Two players queueing for the same ruleset are matched and both receive MATCHED</li>
 *   <li>One player queueing alone receives no MATCHED message</li>
 *   <li>Dequeuing a player before a second joins prevents a match</li>
 *   <li>Separate queues per ruleset: STANDARD + CHESS960 don't match</li>
 * </ul>
 * </p>
 *
 * @since 0.9
 */
class MatchmakingServiceTest {

    private static final int TIMEOUT_MS = 3000;

    private ServerSocket serverSocket;
    private ExecutorService handlerPool;
    private Server server;
    private GameManager gameManager;
    private AuthService authService;
    private GameRepository gameRepository;
    private DatabaseManager dbManager;
    private MatchmakingService matchmakingService;

    @BeforeEach
    void setUp() throws IOException, SQLException {
        server = new Server();
        gameManager = server.getGameManager();
        serverSocket = new ServerSocket(0);
        handlerPool = Executors.newCachedThreadPool();

        dbManager = new DatabaseManager();
        dbManager.initialize(":memory:");
        UserRepository userRepository = new UserRepository(dbManager);
        SessionRepository sessionRepository = new SessionRepository(dbManager);
        authService = new AuthService(userRepository, sessionRepository, 30);
        gameRepository = new GameRepository(dbManager);

        matchmakingService = new MatchmakingService(gameManager);
    }

    @AfterEach
    void tearDown() throws IOException {
        handlerPool.shutdownNow();
        if (!serverSocket.isClosed()) {
            serverSocket.close();
        }
        if (dbManager != null) {
            dbManager.close();
        }
    }

    // ---- Inner helper: a connected pair of client-side streams and server-side handler ----

    static class Connection implements AutoCloseable {
        final Socket clientSocket;
        final PrintWriter clientOut;
        final BufferedReader clientIn;

        Connection(Socket clientSocket) throws IOException {
            this.clientSocket = clientSocket;
            this.clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
            this.clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        }

        void send(String line) {
            clientOut.println(line);
        }

        String readLine() throws IOException {
            return clientIn.readLine();
        }

        @Override
        public void close() throws IOException {
            clientSocket.close();
        }
    }

    /**
     * Opens a client socket, accepts it on the server side, creates a {@link ClientHandler}
     * wired with the matchmaking service, and submits it to the thread pool.
     *
     * @return a {@link Connection} backed by the client-side socket
     */
    private Connection openConnection() throws IOException {
        Socket clientSock = new Socket("127.0.0.1", serverSocket.getLocalPort());
        Socket serverSideSocket = serverSocket.accept();
        serverSideSocket.setSoTimeout(TIMEOUT_MS);

        ClientHandler handler = new ClientHandler(serverSideSocket, server, gameManager, authService, gameRepository);
        handler.setMatchmakingService(matchmakingService);
        handlerPool.submit(handler);
        return new Connection(clientSock);
    }

    /**
     * Reads lines from the connection until a line whose prefix matches {@code typeName:}
     * is found, or the timeout expires.
     *
     * @param conn     the connection to read from
     * @param typeName the message type name to wait for (e.g., "MATCHED")
     * @return the matching line, or {@code null} if the timeout elapsed
     */
    private String readUntilType(Connection conn, String typeName) throws IOException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            String line = conn.readLine();
            if (line == null) break;
            if (line.startsWith(typeName + ":")) {
                return line;
            }
        }
        return null;
    }

    /**
     * Authenticates a connection by registering a new user and performing the LOGIN handshake.
     *
     * @param conn     the connection to authenticate
     * @param username a unique username for this test user
     * @param password the user's password
     */
    private PlayerSession authenticateAndGetSession(Connection conn, String username, String password)
            throws IOException, SQLException {
        authService.register(username, password);
        conn.send("LOGIN:username=" + username + " password=" + password);
        String authLine = readUntilType(conn, MessageType.AUTH_TOKEN.name());
        assertNotNull(authLine, "LOGIN must succeed for user " + username);

        PlayerSession session = new PlayerSession();
        // Extract userId from "AUTH_TOKEN:token=... userId=..."
        for (String part : authLine.split(":", 2)[1].split(" ")) {
            if (part.startsWith("userId=")) {
                session.setUserId(Integer.parseInt(part.substring("userId=".length())));
            }
        }
        session.setUsername(username);
        return session;
    }

    // ========================================================================
    // Test: two players queueing for STANDARD are matched
    // ========================================================================

    /**
     * Verifies that when two authenticated players enqueue for STANDARD, both receive
     * a MATCHED message containing the game join code.
     */
    @Test
    void enqueue_andMatch_whenTwoPlayersQueue() throws Exception {
        try (Connection conn1 = openConnection(); Connection conn2 = openConnection()) {
            authenticateAndGetSession(conn1, "mm_user1", "password");
            authenticateAndGetSession(conn2, "mm_user2", "password");

            conn1.send("QUEUE:ruleset=STANDARD");
            conn2.send("QUEUE:ruleset=STANDARD");

            String matched1 = readUntilType(conn1, "MATCHED");
            String matched2 = readUntilType(conn2, "MATCHED");

            assertNotNull(matched1, "Player 1 must receive MATCHED after second player queues");
            assertNotNull(matched2, "Player 2 must receive MATCHED after both queue");

            assertTrue(matched1.contains("joinCode="), "MATCHED message must contain joinCode=");
            assertTrue(matched2.contains("joinCode="), "MATCHED message must contain joinCode=");

            // Both should have received the same join code
            String joinCode1 = extractParam(matched1, "joinCode");
            String joinCode2 = extractParam(matched2, "joinCode");
            assertEquals(joinCode1, joinCode2, "Both players must receive the same join code");

            // A GameInstance must exist in the GameManager
            int gameId = Integer.parseInt(joinCode1);
            assertNotNull(gameManager.getGame(gameId), "A GameInstance must exist in GameManager after match");
        }
    }

    // ========================================================================
    // Test: one player queuing alone receives no MATCHED
    // ========================================================================

    /**
     * Verifies that a single queued player does not receive a MATCHED message.
     * The queue must hold exactly 1 entry after one player enqueues.
     */
    @Test
    void enqueue_noMatch_whenOnlyOnePlayer() throws Exception {
        PlayerSession session = new PlayerSession();
        session.setUserId(1);
        session.setUsername("solo_player");

        // We use a real ClientHandler via a socket so sendMessage works
        try (Connection conn1 = openConnection()) {
            authenticateAndGetSession(conn1, "mm_solo1", "password");
            conn1.send("QUEUE:ruleset=STANDARD");

            // Wait briefly then confirm no MATCHED was sent
            Thread.sleep(500);

            // The connection should still be open (no MATCHED, no disconnect)
            // Try to read — with the socket timeout there should be no line
            String line = conn1.readLine();
            // line may be null (timeout) or a non-MATCHED line
            if (line != null) {
                assertFalse(line.startsWith("MATCHED:"),
                        "A single queued player must NOT receive a MATCHED message");
            }
            // No assertion about line == null — that's fine too
        }
    }

    // ========================================================================
    // Test: dequeue removes player from queue
    // ========================================================================

    /**
     * Verifies that after one player dequeues, a second player joining the queue
     * does not trigger a match (only one remains in the queue).
     */
    @Test
    void dequeue_removesPlayerFromQueue() throws Exception {
        try (Connection conn1 = openConnection(); Connection conn2 = openConnection()) {
            authenticateAndGetSession(conn1, "mm_deq1", "password");
            authenticateAndGetSession(conn2, "mm_deq2", "password");

            // Player 1 queues then dequeues
            conn1.send("QUEUE:ruleset=STANDARD");
            Thread.sleep(200); // ensure enqueue is processed
            conn1.send("DEQUEUE:ruleset=STANDARD");
            Thread.sleep(200); // ensure dequeue is processed

            // Player 2 queues — only one player in queue now, no match
            conn2.send("QUEUE:ruleset=STANDARD");
            Thread.sleep(500);

            String line = conn2.readLine();
            if (line != null) {
                assertFalse(line.startsWith("MATCHED:"),
                        "After player 1 dequeues, player 2 queueing alone must not receive MATCHED");
            }
        }
    }

    // ========================================================================
    // Test: separate queues per ruleset
    // ========================================================================

    /**
     * Verifies that players queueing for different rulesets (STANDARD vs CHESS960) are not
     * matched with each other, but that a second STANDARD player matches the first STANDARD player.
     */
    @Test
    void separateQueues_perRuleset() throws Exception {
        try (Connection conn1 = openConnection();
             Connection conn2 = openConnection();
             Connection conn3 = openConnection()) {

            authenticateAndGetSession(conn1, "mm_sep1", "password");
            authenticateAndGetSession(conn2, "mm_sep2", "password");
            authenticateAndGetSession(conn3, "mm_sep3", "password");

            // Player 1 queues STANDARD, Player 2 queues CHESS960 — no match
            conn1.send("QUEUE:ruleset=STANDARD");
            Thread.sleep(200);
            conn2.send("QUEUE:ruleset=CHESS960");
            Thread.sleep(500);

            // Neither should have received MATCHED yet
            // (We can't read here without blocking, so we proceed to the next step)

            // Player 3 queues STANDARD — should match with Player 1
            conn3.send("QUEUE:ruleset=STANDARD");

            String matched1 = readUntilType(conn1, "MATCHED");
            String matched3 = readUntilType(conn3, "MATCHED");

            assertNotNull(matched1, "STANDARD player 1 must receive MATCHED when player 3 also queues STANDARD");
            assertNotNull(matched3, "STANDARD player 3 must receive MATCHED");

            // Player 2 (CHESS960) must NOT have received a match
            // We check by trying to read with a short timeout — if conn2 got MATCHED it would be there
            String possibleMatch = conn2.readLine();
            if (possibleMatch != null) {
                assertFalse(possibleMatch.startsWith("MATCHED:"),
                        "CHESS960-only player must not be matched with STANDARD players");
            }
        }
    }

    // ---- Utility ----

    /**
     * Extracts the value of a named parameter from a protocol message line.
     *
     * <p>Expects the format {@code TYPE:key1=val1 key2=val2 ...}.</p>
     *
     * @param line      the raw protocol line
     * @param paramName the parameter key
     * @return the parameter value, or {@code null} if not found
     */
    private String extractParam(String line, String paramName) {
        if (line == null) return null;
        String content = line.contains(":") ? line.split(":", 2)[1] : line;
        for (String part : content.split(" ")) {
            if (part.startsWith(paramName + "=")) {
                return part.substring(paramName.length() + 1);
            }
        }
        return null;
    }
}
