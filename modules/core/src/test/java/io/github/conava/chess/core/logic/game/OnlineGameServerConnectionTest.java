package io.github.conava.chess.core.logic.game;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.data.io.MessageType;
import io.github.conava.chess.core.data.pieces.*;
import io.github.conava.chess.core.exceptions.IllegalMoveException;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import io.github.conava.chess.core.logic.ruleset.chess960Ruleset.Chess960Ruleset;
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
        // Simulate server's JOIN_CODE response — this initializes the board (deferred init).
        game.handleMessage(new Message(MessageType.JOIN_CODE, "joinCode=99"));
        // Simulate a second player joining so the game transitions to RUNNING
        game.setGameState(GameState.RUNNING);
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

        assertTrue(observer.notified, "onGameStateChanged() must be called when a GAME_STATUS message is received");
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
        Message malformedMsg = new Message(MessageType.MOVE, "move=THIS_IS_NOT_A_VALID_MOVE playerColor=BLACK");
        assertDoesNotThrow(() -> game.handleMessage(malformedMsg), "A malformed MOVE message must not propagate any exception out of handleMessage()");
    }

    // ---- OnlineGame.create() must not send any messages before connectToServerGame() ----

    @Test
    void construction_doesNotSendMessage() {
        RecordingConnection freshConn = new RecordingConnection();
        // Construct only — do NOT call connectToServerGame()
        OnlineGame.create(RulesetOptions.STANDARD, "Alice", "Bob", new java.util.HashMap<>(), freshConn);
        assertEquals(0, freshConn.sentMessages.size(), "OnlineGame.create() must not send any messages before connectToServerGame() is called");
    }

    // ---- backupGameState / restoreGameState cover halfMoveClock and positionHistory ----

    /**
     * Reads a protected field from Game via reflection.
     */
    @SuppressWarnings("unchecked")
    private static <T> T readGameField(OnlineGame g, String fieldName) throws Exception {
        java.lang.reflect.Field f = Game.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (T) f.get(g);
    }

    /**
     * Writes a protected field on Game via reflection.
     */
    private static void writeGameField(OnlineGame g, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field f = Game.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(g, value);
    }

    private static OnlineGame freshOnlineGame() {
        RecordingConnection conn = new RecordingConnection();
        OnlineGame g = OnlineGame.create(RulesetOptions.STANDARD, "Alice", "Bob", new HashMap<>(), conn);
        // Initialize the board so backup/restore tests have valid state.
        g.handleMessage(new Message(MessageType.JOIN_CODE, "joinCode=1"));
        return g;
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
        assertEquals(7, restored, "restoreGameState() must restore halfMoveClock to the backed-up value");
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
        assertTrue(restored.containsKey("KEY_A"), "restoreGameState() must restore all backed-up position history entries");
        assertFalse(restored.containsKey("KEY_B"), "restoreGameState() must not include entries added after the backup");
        assertEquals(2, restored.get("KEY_A"), "restoreGameState() must restore the correct count for each position history entry");
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
        assertEquals(1, restored.get("KEY_A"), "The backed-up positionHistory must be a deep copy; mutating the live map after backup must not affect the restored value");
    }

    // ---- T12: NumberFormatException fallback for invalid position param ----

    @Test
    void handleJoinCode_invalidPositionParam_fallsBackGracefully() {
        RecordingConnection conn = new RecordingConnection();
        OnlineGame g = OnlineGame.create(RulesetOptions.STANDARD, "Alice", "Bob", new HashMap<>(), conn);
        g.connectToServerGame();

        // position=abc is non-numeric — NumberFormatException must be caught, and the game
        // must fall back to the standard ruleset and still have a non-null board.
        assertDoesNotThrow(() -> g.handleMessage(new Message(MessageType.JOIN_CODE, "joinCode=5 position=abc ruleset=CHESS960")), "A non-numeric position param must not propagate any exception");

        assertNotNull(g.getBoard(), "Board must be initialized even when position= is non-numeric (fallback to standard)");
    }

    // ---- T10: deferred board initialization ----

    @Test
    void boardIsNullBeforeJoinCodeReceived() {
        RecordingConnection conn = new RecordingConnection();
        OnlineGame freshGame = OnlineGame.create(RulesetOptions.STANDARD, "Alice", "Bob", new HashMap<>(), conn);
        freshGame.connectToServerGame();
        // board must be null until JOIN_CODE arrives
        assertNull(freshGame.getBoard(), "OnlineGame board must be null after construction, before JOIN_CODE is received");
    }

    @Test
    void handleJoinCode_chess960_initializesBoard() {
        RecordingConnection conn = new RecordingConnection();
        OnlineGame g = OnlineGame.create(RulesetOptions.CHESS960, "Alice", "Bob", new HashMap<>(), conn);
        g.connectToServerGame();

        assertNull(g.getBoard(), "Board must be null before JOIN_CODE");

        g.handleMessage(new Message(MessageType.JOIN_CODE, "joinCode=7 position=518 ruleset=CHESS960"));

        assertNotNull(g.getBoard(), "Board must be initialized after JOIN_CODE with Chess960 params");
        assertNotNull(g.getRuleset(), "Ruleset must be set after JOIN_CODE with Chess960 params");
        assertInstanceOf(Chess960Ruleset.class, g.getRuleset(), "Ruleset must be Chess960Ruleset when JOIN_CODE carries ruleset=CHESS960");
        assertEquals(518, ((Chess960Ruleset) g.getRuleset()).getIndex(), "Chess960Ruleset must use position index from JOIN_CODE message");
    }

    @Test
    void handleJoinCode_standard_initializesStandardBoard() {
        RecordingConnection conn = new RecordingConnection();
        OnlineGame g = OnlineGame.create(RulesetOptions.STANDARD, "Alice", "Bob", new HashMap<>(), conn);
        g.connectToServerGame();

        assertNull(g.getBoard(), "Board must be null before JOIN_CODE");

        g.handleMessage(new Message(MessageType.JOIN_CODE, "joinCode=3"));

        assertNotNull(g.getBoard(), "Board must be initialized after JOIN_CODE (standard game)");
    }

    @Test
    void handleSuccess_chess960_initializesBoard() {
        RecordingConnection conn = new RecordingConnection();
        Map<String, String> settings = new HashMap<>();
        settings.put("joinCode", "ABC");
        OnlineGame g = OnlineGame.create(RulesetOptions.CHESS960, "Alice", "Bob", settings, conn);
        g.connectToServerGame();

        assertNull(g.getBoard(), "Board must be null before SUCCESS");

        g.handleMessage(new Message(MessageType.SUCCESS, "player=black position=100 ruleset=CHESS960"));

        assertNotNull(g.getBoard(), "Board must be initialized after SUCCESS with Chess960 params");
        assertInstanceOf(Chess960Ruleset.class, g.getRuleset(), "Ruleset must be Chess960Ruleset when SUCCESS carries ruleset=CHESS960");
        assertEquals(100, ((Chess960Ruleset) g.getRuleset()).getIndex(), "Chess960Ruleset must use position index from SUCCESS message");
    }

    @Test
    void handleSuccess_standard_initializesStandardBoard() {
        RecordingConnection conn = new RecordingConnection();
        Map<String, String> settings = new HashMap<>();
        settings.put("joinCode", "XYZ");
        OnlineGame g = OnlineGame.create(RulesetOptions.STANDARD, "Alice", "Bob", settings, conn);
        g.connectToServerGame();

        assertNull(g.getBoard(), "Board must be null before SUCCESS");

        g.handleMessage(new Message(MessageType.SUCCESS, "player=black"));

        assertNotNull(g.getBoard(), "Board must be initialized after SUCCESS (standard game)");
    }

    @Test
    void getLegalSquares_returnsEmpty_whenBoardNull() {
        RecordingConnection conn = new RecordingConnection();
        OnlineGame g = OnlineGame.create(RulesetOptions.STANDARD, "Alice", "Bob", new HashMap<>(), conn);
        // Do NOT call handleJoinCode — board remains null

        List<Square> result = g.getLegalSquares(new Square(1, 4));
        assertNotNull(result, "getLegalSquares must never return null");
        assertTrue(result.isEmpty(), "getLegalSquares must return an empty list when board is null (deferred init window)");
    }

    @Test
    void connectToServerGame_sendsRulesetByEnumName() {
        RecordingConnection conn = new RecordingConnection();
        OnlineGame g = OnlineGame.create(RulesetOptions.STANDARD, "Alice", "Bob", new HashMap<>(), conn);
        g.connectToServerGame();

        boolean hasRulesetSTANDARD = conn.sentMessages.stream().anyMatch(m -> m.contains("ruleset=STANDARD"));
        assertTrue(hasRulesetSTANDARD, "CREATE_GAME must send ruleset=STANDARD (enum name), not the display name, so the server can parse it");
    }

    // ---- getLocalPlayerColor() ----

    /**
     * When a game is created without a joinCode, the local player is WHITE.
     * The setUp() already creates such a game (no joinCode in settings).
     */
    @Test
    void getLocalPlayerColor_returnsWhite_whenCreatingGame() {
        // game was created in setUp() without a joinCode → local player is WHITE
        assertEquals(io.github.conava.chess.core.data.player.PlayerColor.WHITE,
                game.getLocalPlayerColor(),
                "getLocalPlayerColor() must return WHITE when the local player created the game (no joinCode)");
    }

    /**
     * When a game is joined with a joinCode, the local player is BLACK.
     */
    @Test
    void getLocalPlayerColor_returnsBlack_whenJoiningGame() {
        RecordingConnection joiningConn = new RecordingConnection();
        Map<String, String> settings = new HashMap<>();
        settings.put("joinCode", "XYZ123");
        OnlineGame joiningGame = OnlineGame.create(RulesetOptions.STANDARD, "Alice", "Bob", settings, joiningConn);
        joiningGame.connectToServerGame();

        assertEquals(io.github.conava.chess.core.data.player.PlayerColor.BLACK,
                joiningGame.getLocalPlayerColor(),
                "getLocalPlayerColor() must return BLACK when the local player joined via a joinCode");
    }

    // ============================================================
    // Work Package 1: Fix Remote Move Validation (Bug 1) Tests
    // ============================================================

    /**
     * Test 1: Remote move from the opponent (BLACK) is accepted without exception
     * when the local player is WHITE. Verifies the board state reflects the move.
     *
     * Before the fix, this throws IllegalMoveException because OnlineGame.getLegalSquares()
     * returns empty for opponent pieces, causing isMoveValid() to return false.
     */
    @Test
    void remoteMoveFromBlack_executesSuccessfully_whenLocalPlayerIsWhite() {
        // Local player is WHITE (set up in setUp()).
        // First play white e2-e4 so it's black's turn.
        assertDoesNotThrow(() -> game.movePiece(new Square(1, 4), new Square(3, 4)),
                "White e2-e4 should be legal");

        // Now simulate receiving black's e7-e5 move from the server.
        // playerColor=BLACK triggers handleMove (not skipped — local player is WHITE).
        Message moveMsg = new Message(MessageType.MOVE, "move=e7-e5 playerColor=BLACK");
        assertDoesNotThrow(() -> game.handleMessage(moveMsg),
                "Remote MOVE message for opponent (BLACK) must not throw an exception");

        // The black pawn should now be at e5 (y=4, x=4).
        Piece pieceAtE5 = game.getBoard().getPieceAt(new Square(4, 4));
        assertNotNull(pieceAtE5, "A piece must be present at e5 after black's e7-e5 move");
        assertEquals(Pieces.PAWN, pieceAtE5.getType(),
                "The piece at e5 must be a pawn after black's e7-e5");
    }

    /**
     * Test 2: Remote move from the server is accepted when the local player is BLACK.
     * White's e2-e4 move should execute on the black client's board.
     */
    @Test
    void remoteMoveFromWhite_executesSuccessfully_whenLocalPlayerIsBlack() {
        // Create a joining game (local player is BLACK).
        RecordingConnection joiningConn = new RecordingConnection();
        Map<String, String> settings = new HashMap<>();
        settings.put("joinCode", "ABC");
        OnlineGame joiningGame = OnlineGame.create(RulesetOptions.STANDARD, "Alice", "Bob", settings, joiningConn);
        joiningGame.connectToServerGame();
        // Initialize board via SUCCESS message (joiner path).
        joiningGame.handleMessage(new Message(MessageType.SUCCESS, "player=black"));
        // Game is already RUNNING after connectToServerGame when joinCode provided.

        // Simulate receiving WHITE's e2-e4 from the server.
        Message moveMsg = new Message(MessageType.MOVE, "move=e2-e4 playerColor=WHITE");
        assertDoesNotThrow(() -> joiningGame.handleMessage(moveMsg),
                "Remote MOVE message for opponent (WHITE) on the black client must not throw");

        // The white pawn should now be at e4 (y=3, x=4).
        Piece pieceAtE4 = joiningGame.getBoard().getPieceAt(new Square(3, 4));
        assertNotNull(pieceAtE4, "A piece must be present at e4 after white's e2-e4 move");
        assertEquals(Pieces.PAWN, pieceAtE4.getType(),
                "The piece at e4 must be a pawn after white's e2-e4");
    }

    /**
     * Test 3: After a remote move executes, registered GameObservers are notified.
     */
    @Test
    void remoteMove_notifiesObservers() {
        // Play white e2-e4 first so it's black's turn.
        assertDoesNotThrow(() -> game.movePiece(new Square(1, 4), new Square(3, 4)));

        CapturingObserver observer = new CapturingObserver();
        game.addObserver(observer);

        Message moveMsg = new Message(MessageType.MOVE, "move=e7-e5 playerColor=BLACK");
        game.handleMessage(moveMsg);

        assertTrue(observer.notified,
                "onGameStateChanged() must be called after a remote move executes");
    }

    /**
     * Test 4: Remote non-pawn, non-capture move increments the halfmove clock.
     * Remote pawn move resets it to zero.
     */
    @Test
    void remoteMove_updatesHalfMoveClock() throws Exception {
        // Play white e2-e4 (pawn move — clock resets to 0).
        game.movePiece(new Square(1, 4), new Square(3, 4));
        // Clock should be 0 after white pawn move.
        int clockAfterWhitePawn = readGameField(game, "halfMoveClock");
        assertEquals(0, clockAfterWhitePawn, "halfMoveClock must be 0 after white pawn move");

        // Simulate black knight g8-f6 (non-pawn, non-capture — clock should increment).
        // Black knight at g8 = (y=7, x=6), f6 = (y=5, x=5).
        Message knightMove = new Message(MessageType.MOVE, "move=g8-f6 playerColor=BLACK");
        game.handleMessage(knightMove);
        int clockAfterBlackKnight = readGameField(game, "halfMoveClock");
        assertEquals(1, clockAfterBlackKnight,
                "halfMoveClock must increment to 1 after remote black knight move (non-pawn, non-capture)");

        // Now simulate black pawn d7-d5 (pawn move — clock resets to 0).
        // White d2-d3 first to advance (d-pawn to d3).
        game.movePiece(new Square(1, 3), new Square(2, 3));
        Message blackPawnMove = new Message(MessageType.MOVE, "move=d7-d5 playerColor=BLACK");
        game.handleMessage(blackPawnMove);
        int clockAfterBlackPawn = readGameField(game, "halfMoveClock");
        assertEquals(0, clockAfterBlackPawn,
                "halfMoveClock must reset to 0 after remote black pawn move");
    }

    /**
     * Test 5: Game-end detection (checkmate) works for a remote move.
     * This uses Fool's Mate: 1. f2-f3 e7-e5 2. g2-g4 d8-h4#
     */
    @Test
    void remoteMove_evaluatesGameEnd_checkmate() {
        // Play fool's mate sequence (local WHITE plays f3 and g4, remote BLACK delivers mate).
        // 1. f2-f3 (white)
        assertDoesNotThrow(() -> game.movePiece(new Square(1, 5), new Square(2, 5)));
        // 1...e7-e5 (black remote)
        game.handleMessage(new Message(MessageType.MOVE, "move=e7-e5 playerColor=BLACK"));
        // 2. g2-g4 (white)
        assertDoesNotThrow(() -> game.movePiece(new Square(1, 6), new Square(3, 6)));
        // 2...d8-h4# (black remote queen delivers checkmate)
        game.handleMessage(new Message(MessageType.MOVE, "move=d8-h4 playerColor=BLACK"));

        GameState state = game.getState();
        assertEquals(GameState.BLACK_WON_BY_CHECKMATE, state,
                "After fool's mate delivered remotely by BLACK, game state must be BLACK_WON_BY_CHECKMATE");
    }

    /**
     * Test 6: GAME_HISTORY replay applies all moves, including those of the remote player.
     * Sends a history CSV of 3 moves (e2-e4, e7-e5, d2-d4) and verifies all are on the board.
     */
    @Test
    void historyReplay_executesAllMoves_includingRemotePlayerMoves() {
        // Send a GAME_HISTORY message with alternating white/black moves.
        // e2-e4 (white), e7-e5 (black), d2-d4 (white)
        Message historyMsg = new Message(MessageType.GAME_HISTORY, "moves=e2-e4,e7-e5,d2-d4");
        assertDoesNotThrow(() -> game.handleMessage(historyMsg),
                "GAME_HISTORY must not throw an exception");

        // After replay: white e-pawn at e4 (y=3, x=4)
        Piece pieceAtE4 = game.getBoard().getPieceAt(new Square(3, 4));
        assertNotNull(pieceAtE4, "White e-pawn must be at e4 after history replay");
        assertEquals(Pieces.PAWN, pieceAtE4.getType(), "Piece at e4 must be a pawn");

        // After replay: black e-pawn at e5 (y=4, x=4) — this is the remote player's move
        Piece pieceAtE5 = game.getBoard().getPieceAt(new Square(4, 4));
        assertNotNull(pieceAtE5, "Black e-pawn must be at e5 after history replay (remote move)");
        assertEquals(Pieces.PAWN, pieceAtE5.getType(), "Piece at e5 must be a pawn");

        // After replay: white d-pawn at d4 (y=3, x=3)
        Piece pieceAtD4 = game.getBoard().getPieceAt(new Square(3, 3));
        assertNotNull(pieceAtD4, "White d-pawn must be at d4 after history replay");
        assertEquals(Pieces.PAWN, pieceAtD4.getType(), "Piece at d4 must be a pawn");

        // And the original e2, e7, d2 squares should now be empty
        assertNull(game.getBoard().getPieceAt(new Square(1, 4)),
                "e2 must be empty after white's e2-e4");
        assertNull(game.getBoard().getPieceAt(new Square(6, 4)),
                "e7 must be empty after black's e7-e5");
        assertNull(game.getBoard().getPieceAt(new Square(1, 3)),
                "d2 must be empty after white's d2-d4");
    }

    /**
     * Test 7: Local player moves still go through normal validation.
     * An illegal local move must still throw IllegalMoveException.
     */
    @Test
    void localPlayerMove_stillValidatesNormally() {
        // Attempt an illegal move for white: pawn from e2 to e5 (three squares — illegal).
        assertThrows(IllegalMoveException.class,
                () -> game.movePiece(new Square(1, 4), new Square(4, 4)),
                "An illegal local move must still throw IllegalMoveException even after the remote-move fix");
    }

    // ============================================================
    // Work Package 2: Fix Disconnect Result Display (Bug 2b) Tests
    // ============================================================

    /**
     * When white resigns (local player is WHITE), black should win.
     * Regression: previous code set WHITE_WON_BY_RESIGNATION when white resigned,
     * which is backwards — the resigning player loses, not wins.
     */
    @Test
    void endGame_whenLocalPlayerIsWhite_setsBlackWonByResignation() {
        // game was created in setUp() without joinCode -> local is WHITE
        game.endGame();
        // White resigned, so black should win
        assertEquals(GameState.BLACK_WON_BY_RESIGNATION, game.getState(),
                "When white resigns, BLACK_WON_BY_RESIGNATION must be set (the resigning player loses)");
    }

    /**
     * When black resigns (local player is BLACK), white should win.
     */
    @Test
    void endGame_whenLocalPlayerIsBlack_setsWhiteWonByResignation() {
        RecordingConnection joiningConn = new RecordingConnection();
        Map<String, String> settings = new HashMap<>();
        settings.put("joinCode", "ABC");
        OnlineGame joiningGame = OnlineGame.create(RulesetOptions.STANDARD, "Alice", "Bob", settings, joiningConn);
        joiningGame.connectToServerGame();
        joiningGame.handleMessage(new Message(MessageType.SUCCESS, "player=black"));
        joiningGame.endGame();
        // Black resigned, so white should win
        assertEquals(GameState.WHITE_WON_BY_RESIGNATION, joiningGame.getState(),
                "When black resigns, WHITE_WON_BY_RESIGNATION must be set (the resigning player loses)");
    }

    /**
     * When white resigns, the GAME_STATUS message sent to the server must contain
     * BLACK_WON_BY_RESIGNATION (not WHITE_WON_BY_RESIGNATION).
     */
    @Test
    void endGame_whenWhiteResigns_sendsBlackWonByResignationToServer() {
        // game was created in setUp() without joinCode -> local is WHITE
        int messagesBefore = connection.sentMessages.size();
        game.endGame();
        // Find the GAME_STATUS message sent after endGame was called
        String gameStatusMessage = connection.sentMessages.stream()
                .skip(messagesBefore)
                .filter(m -> m.startsWith("GAME_STATUS"))
                .findFirst()
                .orElse("");
        assertTrue(gameStatusMessage.contains("BLACK_WON_BY_RESIGNATION"),
                "GAME_STATUS sent to server when white resigns must contain BLACK_WON_BY_RESIGNATION, got: "
                        + gameStatusMessage);
    }

    /**
     * Finding 5 (WARNING): Missing test for handleGameHistory when game is NOT RUNNING.
     *
     * Creates a fresh game (board initialized via JOIN_CODE), does NOT set gameState to RUNNING,
     * sends a GAME_HISTORY message, and asserts all moves are applied to the board correctly.
     *
     * Before the fix, executeMoveWithoutLocalValidation() throws IllegalStateException("Game is not
     * running") when gameState != RUNNING. The catch block in handleGameHistory silently discards
     * ALL moves. After the fix, handleGameHistory forces gameState to RUNNING for replay and then
     * restores the original state, so all moves are applied even when the initial state is PAUSED.
     */
    @Test
    void historyReplay_worksWhenGameIsNotYetRunning() {
        // Create a fresh game: board initialized but state is WAITING_FOR_PLAYER (not RUNNING).
        RecordingConnection conn = new RecordingConnection();
        OnlineGame freshGame = OnlineGame.create(RulesetOptions.STANDARD, "Alice", "Bob", new HashMap<>(), conn);
        freshGame.connectToServerGame();
        // Board is initialized via JOIN_CODE but game state is WAITING_FOR_PLAYER (not RUNNING).
        freshGame.handleMessage(new Message(MessageType.JOIN_CODE, "joinCode=42"));
        // Verify the precondition: game is NOT RUNNING
        assertNotEquals(GameState.RUNNING, freshGame.getState(),
                "Precondition: game must NOT be RUNNING before the test");

        // Send GAME_HISTORY — replay must work even though gameState is not RUNNING.
        Message historyMsg = new Message(MessageType.GAME_HISTORY, "moves=e2-e4,e7-e5,d2-d4");
        assertDoesNotThrow(() -> freshGame.handleMessage(historyMsg),
                "GAME_HISTORY must not throw when game state is not RUNNING");

        // After replay, all three moves must be on the board.
        Piece pieceAtE4 = freshGame.getBoard().getPieceAt(new Square(3, 4));
        assertNotNull(pieceAtE4, "White e-pawn must be at e4 after history replay (game was not RUNNING)");
        assertEquals(Pieces.PAWN, pieceAtE4.getType(), "Piece at e4 must be a pawn");

        Piece pieceAtE5 = freshGame.getBoard().getPieceAt(new Square(4, 4));
        assertNotNull(pieceAtE5, "Black e-pawn must be at e5 after history replay (remote move, game was not RUNNING)");
        assertEquals(Pieces.PAWN, pieceAtE5.getType(), "Piece at e5 must be a pawn");

        Piece pieceAtD4 = freshGame.getBoard().getPieceAt(new Square(3, 3));
        assertNotNull(pieceAtD4, "White d-pawn must be at d4 after history replay (game was not RUNNING)");
        assertEquals(Pieces.PAWN, pieceAtD4.getType(), "Piece at d4 must be a pawn");
    }

    /**
     * Finding 3 (WARNING): Missing idempotency test.
     *
     * Calls handleMessage with the same GAME_HISTORY message TWICE and asserts that the
     * board state after the second call is identical to the board state after the first call
     * (same piece positions and same turnCount). This proves that the board-reset fix in
     * handleGameHistory makes replay truly idempotent — without the reset the second call
     * would replay moves onto an already-mutated board, producing double-moved pieces and
     * an incorrect turnCount.
     */
    @Test
    void historyReplay_isIdempotent_onRetry() {
        Message historyMsg = new Message(MessageType.GAME_HISTORY, "moves=e2-e4,e7-e5,d2-d4");

        // First call.
        assertDoesNotThrow(() -> game.handleMessage(historyMsg),
                "First GAME_HISTORY call must not throw");

        // Capture board state after the first call.
        Piece e4After1st = game.getBoard().getPieceAt(new Square(3, 4));
        Piece e5After1st = game.getBoard().getPieceAt(new Square(4, 4));
        Piece d4After1st = game.getBoard().getPieceAt(new Square(3, 3));
        Piece e2After1st = game.getBoard().getPieceAt(new Square(1, 4));
        Piece e7After1st = game.getBoard().getPieceAt(new Square(6, 4));
        Piece d2After1st = game.getBoard().getPieceAt(new Square(1, 3));

        // Read turnCount after the first call.
        int turnCountField;
        try {
            java.lang.reflect.Field f = Game.class.getDeclaredField("turnCount");
            f.setAccessible(true);
            turnCountField = (int) f.get(game);
        } catch (Exception e) {
            throw new RuntimeException("Could not read turnCount via reflection", e);
        }
        int turnCountAfter1st = turnCountField;

        // Second call with the same message — must produce an identical result.
        assertDoesNotThrow(() -> game.handleMessage(historyMsg),
                "Second (retry) GAME_HISTORY call must not throw");

        // Verify board positions are identical after the second call.
        Piece e4After2nd = game.getBoard().getPieceAt(new Square(3, 4));
        Piece e5After2nd = game.getBoard().getPieceAt(new Square(4, 4));
        Piece d4After2nd = game.getBoard().getPieceAt(new Square(3, 3));
        Piece e2After2nd = game.getBoard().getPieceAt(new Square(1, 4));
        Piece e7After2nd = game.getBoard().getPieceAt(new Square(6, 4));
        Piece d2After2nd = game.getBoard().getPieceAt(new Square(1, 3));

        assertNotNull(e4After2nd,  "e4 must still contain a pawn after idempotent retry");
        assertNotNull(e5After2nd,  "e5 must still contain a pawn after idempotent retry");
        assertNotNull(d4After2nd,  "d4 must still contain a pawn after idempotent retry");
        assertNull(e2After2nd,     "e2 must still be empty after idempotent retry");
        assertNull(e7After2nd,     "e7 must still be empty after idempotent retry");
        assertNull(d2After2nd,     "d2 must still be empty after idempotent retry");

        assertEquals(e4After1st.getType(), e4After2nd.getType(),
                "Piece type at e4 must be the same after idempotent retry");
        assertEquals(e5After1st.getType(), e5After2nd.getType(),
                "Piece type at e5 must be the same after idempotent retry");
        assertEquals(d4After1st.getType(), d4After2nd.getType(),
                "Piece type at d4 must be the same after idempotent retry");

        // Verify turnCount is the same.
        try {
            java.lang.reflect.Field f = Game.class.getDeclaredField("turnCount");
            f.setAccessible(true);
            int turnCountAfter2nd = (int) f.get(game);
            assertEquals(turnCountAfter1st, turnCountAfter2nd,
                    "turnCount must be identical after idempotent retry — the second call must not double-count moves");
        } catch (Exception e) {
            throw new RuntimeException("Could not read turnCount via reflection", e);
        }
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
