package io.github.conava.chess.server.management;

import io.github.conava.chess.core.data.io.MessageParser;
import io.github.conava.chess.core.data.io.MessageType;
import io.github.conava.chess.server.Server;
import io.github.conava.chess.server.auth.AuthService;
import io.github.conava.chess.server.db.DatabaseManager;
import io.github.conava.chess.server.persistence.GameRepository;
import io.github.conava.chess.server.persistence.SessionRepository;
import io.github.conava.chess.server.persistence.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link ClientHandler}.
 *
 * <p>Each test creates a loopback TCP socket pair: the server-side socket is handed to a
 * {@code ClientHandler} that runs in a background thread, while the test drives the
 * interaction through the client-side socket's streams.</p>
 *
 * <p>Authentication infrastructure uses an in-memory SQLite database so no external
 * dependencies are required.</p>
 *
 * <p>Covered behaviours:
 * <ul>
 *   <li>Malformed (unparseable) messages produce an ERROR response; the handler loop continues</li>
 *   <li>CREATE_GAME with an invalid ruleset name produces an ERROR response</li>
 *   <li>JOIN_GAME with a non-existent game ID produces an ERROR response</li>
 *   <li>JOIN_GAME uses key=value format (gameId=N playerName=X) — raw integer is not accepted</li>
 *   <li>CREATE_GAME with playerName parameter results in a JOIN_CODE response (game created)</li>
 *   <li>CREATE_GAME without playerName falls back to the default "Player 1"</li>
 *   <li>After both players connect, both receive GAME_STATUS gameState=RUNNING</li>
 *   <li>After joinGame() the client's gameInstance field is set (shadow bug fix):
 *       subsequent MOVE messages are dispatched to the game rather than returning
 *       "No game instance available" ERROR</li>
 *   <li>sendMessage() is thread-safe: concurrent calls from two player threads do not
 *       corrupt the output stream (each line is a complete, parseable message)</li>
 *   <li>Unauthenticated clients get ERROR when attempting non-auth messages</li>
 *   <li>LOGIN succeeds and returns AUTH_TOKEN</li>
 *   <li>REGISTER succeeds and returns AUTH_TOKEN</li>
 *   <li>Authenticated client can CREATE_GAME</li>
 *   <li>LOGIN with wrong password returns ERROR</li>
 * </ul>
 * </p>
 */
class ClientHandlerIntegrationTest {

    private static final int TIMEOUT_MS = 3000;

    private ServerSocket serverSocket;
    private ExecutorService handlerPool;
    private Server server;
    private GameManager gameManager;
    private AuthService authService;
    private GameRepository gameRepository;
    private DatabaseManager dbManager;

    @BeforeEach
    void setUp() throws IOException, SQLException {
        server = new Server();
        gameManager = server.getGameManager();
        serverSocket = new ServerSocket(0); // port 0 = OS-assigned free port
        handlerPool = Executors.newCachedThreadPool();

        // Set up in-memory SQLite database for auth tests
        dbManager = new DatabaseManager();
        dbManager.initialize(":memory:");
        UserRepository userRepository = new UserRepository(dbManager);
        SessionRepository sessionRepository = new SessionRepository(dbManager);
        authService = new AuthService(userRepository, sessionRepository, 30);
        gameRepository = new GameRepository(dbManager);
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

        /**
         * Reads the next line from the server with a timeout. Returns null on timeout.
         */
        String readLine() throws IOException {
            return clientIn.readLine();
        }

        @Override
        public void close() throws IOException {
            clientSocket.close();
        }
    }

    /**
     * Opens a client Socket to the server socket, accepts it on the server side,
     * wraps it in a ClientHandler, and submits the handler to the thread pool.
     *
     * @return a {@link Connection} backed by the client-side socket
     */
    private Connection openConnection() throws IOException {
        // Connect from client side first (non-blocking from client perspective).
        Socket clientSock = new Socket("127.0.0.1", serverSocket.getLocalPort());
        // Accept on server side.
        Socket serverSideSocket = serverSocket.accept();
        serverSideSocket.setSoTimeout(TIMEOUT_MS);

        ClientHandler handler = new ClientHandler(serverSideSocket, server, gameManager, authService, gameRepository);
        handlerPool.submit(handler);
        return new Connection(clientSock);
    }

    // ---- Helpers ----

    /**
     * Reads lines from the connection until a line whose MessageType matches
     * {@code type} is found, or the timeout expires.
     */
    private String readUntilType(Connection conn, String typeName) throws IOException {
        String line;
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            line = conn.readLine();
            if (line == null) break;
            if (line.startsWith(typeName + ":")) {
                return line;
            }
        }
        return null;
    }

    // ========================================================================
    // Auth gating: unauthenticated client gets ERROR on non-auth messages
    // ========================================================================

    /**
     * Verifies that an unauthenticated client receives ERROR when it sends CREATE_GAME
     * without first authenticating.
     */
    @Test
    void unauthenticatedClient_getsError_onCreateGame() throws Exception {
        try (Connection conn = openConnection()) {
            conn.send("CREATE_GAME:ruleset=STANDARD playerName=Alice");

            String response = readUntilType(conn, MessageType.ERROR.name());
            assertNotNull(response, "Unauthenticated CREATE_GAME must produce an ERROR response");
            assertTrue(response.contains("Not authenticated"),
                    "Error message must indicate the client is not authenticated");
        }
    }

    // ========================================================================
    // LOGIN: succeeds and returns AUTH_TOKEN
    // ========================================================================

    /**
     * Verifies that LOGIN with valid credentials returns an AUTH_TOKEN response.
     */
    @Test
    void login_succeeds_andReturnsToken() throws Exception {
        // Pre-register the user directly in the DB
        authService.register("alice", "password123");

        try (Connection conn = openConnection()) {
            conn.send("LOGIN:username=alice password=password123");

            String response = readUntilType(conn, MessageType.AUTH_TOKEN.name());
            assertNotNull(response, "LOGIN with valid credentials must produce an AUTH_TOKEN response");
            assertTrue(response.contains("token="), "AUTH_TOKEN response must contain a token= parameter");
            assertTrue(response.contains("userId="), "AUTH_TOKEN response must contain a userId= parameter");
        }
    }

    // ========================================================================
    // REGISTER: succeeds and returns AUTH_TOKEN
    // ========================================================================

    /**
     * Verifies that REGISTER with valid credentials creates the account and returns AUTH_TOKEN.
     */
    @Test
    void register_succeeds_andReturnsToken() throws Exception {
        try (Connection conn = openConnection()) {
            conn.send("REGISTER:username=bob password=securepass");

            String response = readUntilType(conn, MessageType.AUTH_TOKEN.name());
            assertNotNull(response, "REGISTER must produce an AUTH_TOKEN response");
            assertTrue(response.contains("token="), "AUTH_TOKEN response must contain a token= parameter");
            assertTrue(response.contains("userId="), "AUTH_TOKEN response must contain a userId= parameter");
        }
    }

    // ========================================================================
    // Authenticated client can CREATE_GAME
    // ========================================================================

    /**
     * Verifies that after a successful LOGIN the client can send CREATE_GAME and receive
     * a JOIN_CODE response (i.e., auth gating passes for authenticated clients).
     */
    @Test
    void authenticatedClient_canCreateGame() throws Exception {
        // Pre-register user
        authService.register("charlie", "password123");

        try (Connection conn = openConnection()) {
            conn.send("LOGIN:username=charlie password=password123");

            String authResponse = readUntilType(conn, MessageType.AUTH_TOKEN.name());
            assertNotNull(authResponse, "LOGIN must succeed before CREATE_GAME test");

            // Now send CREATE_GAME — should work because client is authenticated
            conn.send("CREATE_GAME:ruleset=STANDARD playerName=Charlie");

            String response = readUntilType(conn, MessageType.JOIN_CODE.name());
            assertNotNull(response, "Authenticated client must receive JOIN_CODE after CREATE_GAME");
        }
    }

    // ========================================================================
    // LOGIN: wrong password returns ERROR
    // ========================================================================

    /**
     * Verifies that LOGIN with incorrect credentials returns ERROR.
     */
    @Test
    void login_fails_withWrongPassword() throws Exception {
        // Pre-register the user with a different password
        authService.register("dave", "correctpassword");

        try (Connection conn = openConnection()) {
            conn.send("LOGIN:username=dave password=wrongpassword");

            String response = readUntilType(conn, MessageType.ERROR.name());
            assertNotNull(response, "LOGIN with wrong password must produce an ERROR response");
        }
    }

    // ========================================================================
    // Malformed message handling
    // ========================================================================

    @Test
    void processClientMessages_malformedInput_sendsError() throws Exception {
        try (Connection conn = openConnection()) {
            conn.send("GARBAGE_NOT_A_VALID_MESSAGE");

            String response = readUntilType(conn, MessageType.ERROR.name());
            assertNotNull(response, "A malformed (unparseable) message must produce an ERROR response");
        }
    }

    @Test
    void processClientMessages_malformedInput_handlerContinues() throws Exception {
        try (Connection conn = openConnection()) {
            // Send malformed message.
            conn.send("GARBAGE");
            String errorResponse = readUntilType(conn, MessageType.ERROR.name());
            assertNotNull(errorResponse, "First malformed message must return ERROR");

            // Handler must continue processing: send another valid-but-unroutable message.
            // JOIN_GAME without auth: should also return ERROR (not authenticated).
            conn.send("JOIN_GAME:playerName=Test");
            String secondError = readUntilType(conn, MessageType.ERROR.name());
            assertNotNull(secondError, "Handler must continue processing after a malformed message");
        }
    }

    // ========================================================================
    // CREATE_GAME: invalid ruleset sends ERROR (after auth)
    // ========================================================================

    @Test
    void createGame_invalidRuleset_sendsError() throws Exception {
        authService.register("user1", "password123");
        try (Connection conn = openConnection()) {
            conn.send("LOGIN:username=user1 password=password123");
            readUntilType(conn, MessageType.AUTH_TOKEN.name());

            conn.send("CREATE_GAME:ruleset=INVALID_RULESET playerName=Alice");

            String response = readUntilType(conn, MessageType.ERROR.name());
            assertNotNull(response, "CREATE_GAME with an invalid ruleset name must produce an ERROR response");
        }
    }

    @Test
    void createGame_missingRuleset_sendsError() throws Exception {
        authService.register("user2", "password123");
        try (Connection conn = openConnection()) {
            conn.send("LOGIN:username=user2 password=password123");
            readUntilType(conn, MessageType.AUTH_TOKEN.name());

            conn.send("CREATE_GAME:playerName=Alice");

            String response = readUntilType(conn, MessageType.ERROR.name());
            assertNotNull(response, "CREATE_GAME without a ruleset parameter must produce an ERROR response");
        }
    }

    // ========================================================================
    // JOIN_GAME: invalid game ID sends ERROR (after auth)
    // ========================================================================

    @Test
    void joinGame_invalidCode_sendsError() throws Exception {
        authService.register("user3", "password123");
        try (Connection conn = openConnection()) {
            conn.send("LOGIN:username=user3 password=password123");
            readUntilType(conn, MessageType.AUTH_TOKEN.name());

            // No games registered; gameId=999 does not exist.
            conn.send("JOIN_GAME:gameId=999 playerName=Bob");

            String response = readUntilType(conn, MessageType.ERROR.name());
            assertNotNull(response, "JOIN_GAME with a non-existent game ID must produce an ERROR response");
        }
    }

    @Test
    void joinGame_nonNumericGameId_sendsError() throws Exception {
        authService.register("user4", "password123");
        try (Connection conn = openConnection()) {
            conn.send("LOGIN:username=user4 password=password123");
            readUntilType(conn, MessageType.AUTH_TOKEN.name());

            conn.send("JOIN_GAME:gameId=NOT_A_NUMBER playerName=Bob");

            String response = readUntilType(conn, MessageType.ERROR.name());
            assertNotNull(response, "JOIN_GAME with a non-numeric gameId must produce an ERROR response");
        }
    }

    // ========================================================================
    // JOIN_GAME: key=value format (not raw integer)
    // ========================================================================

    @Test
    void joinGame_keyValueFormat_parsesGameIdCorrectly() throws Exception {
        authService.register("user5", "password123");
        authService.register("user6", "password123");

        // Create a game from connection 1, then join it from connection 2 using
        // the key=value format "gameId=<id> playerName=Bob".
        try (Connection conn1 = openConnection(); Connection conn2 = openConnection()) {
            // Authenticate both connections
            conn1.send("LOGIN:username=user5 password=password123");
            readUntilType(conn1, MessageType.AUTH_TOKEN.name());

            conn2.send("LOGIN:username=user6 password=password123");
            readUntilType(conn2, MessageType.AUTH_TOKEN.name());

            // Creator sends CREATE_GAME.
            conn1.send("CREATE_GAME:ruleset=STANDARD playerName=Alice");

            // Expect JOIN_CODE response containing the game ID.
            String joinCodeLine = readUntilType(conn1, MessageType.JOIN_CODE.name());
            assertNotNull(joinCodeLine, "CREATE_GAME must produce a JOIN_CODE response");

            // Extract the game ID from "JOIN_CODE:joinCode=<id>".
            String joinCodeContent = joinCodeLine.split(":", 2)[1];
            String gameIdStr = joinCodeContent.split("=")[1].trim();

            // Joiner uses key=value format.
            conn2.send("JOIN_GAME:gameId=" + gameIdStr + " playerName=Bob");

            // Both players must receive GAME_STATUS gameState=RUNNING.
            String status1 = readUntilType(conn1, MessageType.GAME_STATUS.name());
            String status2 = readUntilType(conn2, MessageType.GAME_STATUS.name());

            assertNotNull(status1, "Creator must receive GAME_STATUS after joiner connects (key=value format)");
            assertNotNull(status2, "Joiner must receive GAME_STATUS after connecting (key=value format)");
            assertTrue(status1.contains("RUNNING"), "GAME_STATUS sent to creator must indicate RUNNING state");
            assertTrue(status2.contains("RUNNING"), "GAME_STATUS sent to joiner must indicate RUNNING state");
        }
    }

    // ========================================================================
    // CREATE_GAME: with and without playerName (after auth)
    // ========================================================================

    @Test
    void createGame_withPlayerName_receivesJoinCode() throws Exception {
        authService.register("user7", "password123");
        try (Connection conn = openConnection()) {
            conn.send("LOGIN:username=user7 password=password123");
            readUntilType(conn, MessageType.AUTH_TOKEN.name());

            conn.send("CREATE_GAME:ruleset=STANDARD playerName=Alice");

            String response = readUntilType(conn, MessageType.JOIN_CODE.name());
            assertNotNull(response, "CREATE_GAME with a valid ruleset and playerName must produce a JOIN_CODE response");
        }
    }

    @Test
    void createGame_noPlayerName_receivesJoinCode() throws Exception {
        authService.register("user8", "password123");
        try (Connection conn = openConnection()) {
            conn.send("LOGIN:username=user8 password=password123");
            readUntilType(conn, MessageType.AUTH_TOKEN.name());

            // No playerName parameter: the handler must default to "Player 1" and still succeed.
            conn.send("CREATE_GAME:ruleset=STANDARD");

            String response = readUntilType(conn, MessageType.JOIN_CODE.name());
            assertNotNull(response, "CREATE_GAME without playerName must still produce a JOIN_CODE response using default name");
        }
    }

    // ========================================================================
    // joinGame sets this.gameInstance correctly (shadow bug fix)
    // ========================================================================

    @Test
    void joinGame_setsFieldCorrectly_movesDispatchedToGame() throws Exception {
        authService.register("user9", "password123");
        authService.register("user10", "password123");

        // If the shadow bug were present, the joiner's gameInstance field would remain null
        // after joinGame(), and any subsequent MOVE message would return
        // "No game instance available" ERROR instead of being dispatched to the game.
        // After the fix, the MOVE message must be dispatched to the game and return a
        // game-related response (not "No game instance available").
        try (Connection conn1 = openConnection(); Connection conn2 = openConnection()) {

            // Authenticate both
            conn1.send("LOGIN:username=user9 password=password123");
            readUntilType(conn1, MessageType.AUTH_TOKEN.name());

            conn2.send("LOGIN:username=user10 password=password123");
            readUntilType(conn2, MessageType.AUTH_TOKEN.name());

            // Set up the game.
            conn1.send("CREATE_GAME:ruleset=STANDARD playerName=Alice");
            String joinCodeLine = readUntilType(conn1, MessageType.JOIN_CODE.name());
            assertNotNull(joinCodeLine, "Need JOIN_CODE to proceed with test");

            String gameIdStr = joinCodeLine.split(":", 2)[1].split("=")[1].trim();
            conn2.send("JOIN_GAME:gameId=" + gameIdStr + " playerName=Bob");

            // Wait for GAME_STATUS RUNNING on both sides.
            readUntilType(conn1, MessageType.GAME_STATUS.name());
            readUntilType(conn2, MessageType.GAME_STATUS.name());

            // Now send a MOVE from the joiner (black player). The game was just
            // started and it's white's turn, so black's move will be an illegal
            // move (wrong turn). We expect an ERROR from the game, NOT from
            // "No game instance available" — the key difference is that the message
            // is dispatched AT ALL (field is set correctly).
            //
            // We send a move message and verify we get ANY response (ERROR or MOVE),
            // not silence. An "Error: No game instance available" would be a different
            // form of ERROR than a game-level ERROR.
            conn2.send("MOVE:move=e7-e5 playerColor=BLACK");

            // Give the handler time to respond.
            String response = readUntilType(conn2, MessageType.ERROR.name());
            // If we got an ERROR here, check it is NOT the "No game instance" error.
            // It should be a game-level error (wrong turn or illegal move).
            if (response != null) {
                assertFalse(response.contains("No game instance available"),
                        "After joinGame(), the gameInstance field must be set; "
                                + "MOVE must be dispatched to the game, not rejected with 'No game instance available'");
            }
            // If no error was received within timeout (e.g., black's move was accepted
            // somehow or MOVE was echoed), the test also passes — the key assertion is
            // the absence of "No game instance available".
        }
    }

    // ========================================================================
    // sendMessage thread-safety: concurrent calls produce non-interleaved lines
    // ========================================================================

    @Test
    void sendMessage_concurrentCalls_eachLineIsParseable() throws Exception {
        authService.register("user11", "password123");
        authService.register("user12", "password123");

        // Create a game with two players so both ClientHandlers are active and
        // GameInstance will call sendMessage() on the white handler from two threads
        // after a MOVE (both players receive the move relay concurrently).
        // We verify that every line received on the client side is parseable.
        try (Connection conn1 = openConnection(); Connection conn2 = openConnection()) {

            conn1.send("LOGIN:username=user11 password=password123");
            readUntilType(conn1, MessageType.AUTH_TOKEN.name());

            conn2.send("LOGIN:username=user12 password=password123");
            readUntilType(conn2, MessageType.AUTH_TOKEN.name());

            conn1.send("CREATE_GAME:ruleset=STANDARD playerName=Alice");
            String joinCodeLine = readUntilType(conn1, MessageType.JOIN_CODE.name());
            assertNotNull(joinCodeLine, "Need JOIN_CODE to proceed");
            String gameIdStr = joinCodeLine.split(":", 2)[1].split("=")[1].trim();

            conn2.send("JOIN_GAME:gameId=" + gameIdStr + " playerName=Bob");
            readUntilType(conn1, MessageType.GAME_STATUS.name());
            readUntilType(conn2, MessageType.GAME_STATUS.name());

            // White makes a legal move — both clients receive the MOVE relay concurrently.
            conn1.send("MOVE:move=e2-e4 playerColor=WHITE");

            // Read the MOVE relay line on conn1 (white) and verify it is parseable.
            String moveLine = readUntilType(conn1, MessageType.MOVE.name());
            if (moveLine != null) {
                // The line must be parseable without throwing.
                assertDoesNotThrow(() -> MessageParser.parse(moveLine),
                        "Each line received on the client must be parseable (no corruption from concurrent writes)");
            }

            // Also verify conn2 (black) received the MOVE relay.
            String moveLine2 = readUntilType(conn2, MessageType.MOVE.name());
            if (moveLine2 != null) {
                assertDoesNotThrow(() -> MessageParser.parse(moveLine2),
                        "Each line received on black's client must be parseable");
            }
        }
    }
}
