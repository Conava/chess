package io.github.conava.chess.core.logic.game;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.pieces.Piece;
import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.data.io.MessageParser;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.logic.ruleset.Ruleset;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import io.github.conava.chess.core.logic.ruleset.chess960Ruleset.Chess960Ruleset;
import io.github.conava.chess.core.exceptions.IllegalMoveException;
import io.github.conava.chess.core.logic.moves.CastleMove;
import io.github.conava.chess.core.logic.moves.Move;
import io.github.conava.chess.core.logic.moves.PromotionMove;
import io.github.conava.chess.core.data.io.MessageType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The OnlineGame class represents an online game session.
 * It handles communication with the server, game state management, and player interactions.
 * The networking connection is provided externally via the {@link ServerConnection} interface,
 * keeping I/O out of the core module.
 *
 * <p>Board initialization is deferred: the board and ruleset are {@code null} after construction
 * and are set only when the server's first response ({@code JOIN_CODE} for the creator,
 * {@code SUCCESS} for the joiner) is received. This ensures the server is always the authority
 * for game parameters (including the Chess 960 position index).
 */
public class OnlineGame extends Game {
    private static final Logger LOGGER = Logger.getLogger(OnlineGame.class.getName());
    private static final String JOIN_CODE_PARAM = "joinCode";
    private static final String PLAYER_COLOR_PARAM = "playerColor";
    private static final String MOVE_PARAM = "move";
    private static final String GAME_STATE_PARAM = "gameState";
    private static final String SENDER_PARAM = "sender";
    private static final String CONTENT_PARAM = "content";
    private static final String MOVES_PARAM = "moves";
    private static final String RULESET_PARAM = "ruleset";
    private static final String COLOR_PARAM = "color";

    /** Whether the opponent (or server) has proposed saving the game for later. */
    private volatile boolean saveOffered = false;

    private final ServerConnection connection;
    private String joinCode;
    private PlayerColor localPlayerColor;
    private final RulesetOptions selectedRuleset;

    private Board backupBoard;
    private List<Move> backupMoves;
    private GameState backupGameState;
    private int backupHalfMoveClock;
    private Map<String, Integer> backupPositionHistory;

    /**
     * Private constructor — use {@link #create} to obtain an instance.
     *
     * <p>The board is NOT initialized here. It will be initialized when the server's first
     * response ({@code JOIN_CODE} or {@code SUCCESS}) is received.
     *
     * @param selectedRuleset    The selected ruleset for the game (used as fallback hint).
     * @param playerWhiteName    The name of the white player.
     * @param playerBlackName    The name of the black player.
     * @param onlineGameSettings The settings for the online game, including join code.
     * @param connection         An already-established {@link ServerConnection} to the game server.
     */
    private OnlineGame(RulesetOptions selectedRuleset, String playerWhiteName, String playerBlackName, Map<String, String> onlineGameSettings, ServerConnection connection) {
        super(selectedRuleset, playerWhiteName, playerBlackName, true); // deferBoardInit=true
        this.gameState = GameState.NO_GAME;
        this.joinCode = onlineGameSettings.get("joinCode");
        this.selectedRuleset = selectedRuleset;
        this.connection = connection;
    }

    /**
     * Static factory method that constructs an {@link OnlineGame} without sending any network
     * messages. The caller must invoke {@link #connectToServerGame()} separately once the
     * connection has been confirmed to be live.
     *
     * @param selectedRuleset    The selected ruleset for the game.
     * @param playerWhiteName    The name of the white player.
     * @param playerBlackName    The name of the black player.
     * @param onlineGameSettings The settings for the online game, including join code.
     * @param connection         An already-established {@link ServerConnection} to the game server.
     * @return A newly constructed {@link OnlineGame} instance.
     */
    public static OnlineGame create(RulesetOptions selectedRuleset, String playerWhiteName, String playerBlackName, Map<String, String> onlineGameSettings, ServerConnection connection) {
        return new OnlineGame(selectedRuleset, playerWhiteName, playerBlackName, onlineGameSettings, connection);
    }

    /**
     * Sends the initial handshake message to the server: either {@code CREATE_GAME} (when no
     * join code is present) or {@code JOIN_GAME} (when a join code was supplied).
     *
     * <p>This method must be called by the application facade <em>after</em>:
     * <ol>
     *   <li>The {@link ServerConnection} has been confirmed live (i.e. {@code isConnected()} is true).</li>
     *   <li>The message handler that forwards server responses to {@link #handleMessage} has been
     *       registered with the underlying transport so that no server replies are lost.</li>
     * </ol>
     * It is intentionally not called from the constructor — see {@link #create} for the
     * two-phase construction contract.
     *
     * <p>The ruleset is sent using the enum constant name (e.g. {@code STANDARD}, {@code CHESS960})
     * so the server can parse it with {@code RulesetOptions.valueOf(...)}.
     */
    public void connectToServerGame() {
        Message connectMessage;
        if (joinCode != null && !joinCode.isEmpty()) {
            connectMessage = new Message(MessageType.JOIN_GAME, JOIN_CODE_PARAM + "=" + joinCode);
            localPlayerColor = PlayerColor.BLACK;
            gameState = GameState.RUNNING;
        } else {
            // Use enum name (STANDARD / CHESS960), not toString(), so server can valueOf() it.
            connectMessage = new Message(MessageType.CREATE_GAME, "ruleset=" + selectedRuleset.name());
            localPlayerColor = PlayerColor.WHITE;
            gameState = GameState.WAITING_FOR_PLAYER;
        }
        sendMessageToServer(connectMessage);
    }

    /**
     * Handles incoming messages from the server.
     *
     * @param message The message received from the server.
     */
    public void handleMessage(Message message) {
        switch (message.type()) {
            case JOIN_CODE      -> handleJoinCode(message);
            case MOVE           -> handleMove(message);
            case GAME_STATUS    -> handleGameStatus(message);
            case SUCCESS        -> handleSuccess(message);
            case ERROR          -> handleError(message);
            case FAILURE        -> handleFailure(message);
            case CHAT           -> handleChat(message);
            case GAME_HISTORY   -> handleGameHistory(message);
            case SAVE_GAME      -> handleSaveGame();
            case SAVE_ACCEPTED  -> handleSaveAccepted();
            case MATCHED        -> handleMatched(message);
            default             -> LOGGER.log(Level.WARNING, "Unhandled message type: {0}", message.type());
        }
    }

    /**
     * Builds the appropriate {@link Ruleset} from server-provided parameters.
     *
     * <p>If the message carries {@code ruleset=CHESS960} and a {@code position} parameter,
     * a {@link Chess960Ruleset} is constructed with the given Scharnagl index. Otherwise,
     * the locally-selected ruleset is used as a fallback (typically {@code StandardChessRuleset}).
     *
     * @param message The server message containing optional {@code ruleset} and {@code position} keys.
     * @return The constructed {@link Ruleset}.
     */
    private Ruleset buildRulesetFromServerParams(Message message) {
        String rulesetParam = message.getParameterValue("ruleset");
        String positionParam = message.getParameterValue("position");
        if ("CHESS960".equals(rulesetParam) && positionParam != null) {
            try {
                int index = Integer.parseInt(positionParam);
                return new Chess960Ruleset(index);
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, "Invalid position index in server message: " + positionParam);
            }
        }
        return createRulesetFromSelected();
    }

    /**
     * Creates a {@link Ruleset} from the locally-selected {@link RulesetOptions}.
     * Used as a fallback when the server message carries no ruleset parameters.
     */
    private Ruleset createRulesetFromSelected() {
        return createRuleset(selectedRuleset);
    }

    /**
     * Handles the JOIN_CODE message type.
     *
     * <p>For the game creator, this message is the server's first response. It carries the
     * join code and, for Chess 960 games, also {@code position=N} and {@code ruleset=CHESS960}.
     * Board initialization happens here — after this call the game is ready for play.
     *
     * @param message The message containing the join code.
     */
    private void handleJoinCode(Message message) {
        joinCode = message.getParameterValue(JOIN_CODE_PARAM);
        LOGGER.info("Join code received: " + joinCode + " - Please share this code with your friend to join the game");
        if (board == null) {
            Ruleset ruleset = buildRulesetFromServerParams(message);
            initializeBoard(ruleset);
        }
    }

    /**
     * Handles the MOVE message type.
     *
     * <p>Uses {@link Ruleset#deserializeMove} so that Chess 960 rulesets can reconstruct
     * {@link CastleMove} instances with the correct rook-origin and king-dest files.
     *
     * @param message The message containing the move information.
     */
    private void handleMove(Message message) {
        if (Objects.equals(message.getParameterValue(PLAYER_COLOR_PARAM), localPlayerColor.toString())) {
            return;
        }
        if (board == null) {
            LOGGER.log(Level.WARNING, "Received MOVE before board was initialized; ignoring.");
            return;
        }

        try {
            String wireString = Objects.requireNonNull(message.getParameterValue(MOVE_PARAM));
            Player player = Objects.equals(message.getParameterValue(PLAYER_COLOR_PARAM), "WHITE") ? player0 : player1;
            Move move = ruleset.deserializeMove(wireString, board, player);
            executeMoveFromRemote(move);
        } catch (IllegalMoveException e) {
            LOGGER.log(Level.SEVERE, "Illegal move received: " + message.content(), e);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Failed to parse move from server: " + message.content(), e);
        }
    }

    /**
     * Handles the GAME_STATUS message type.
     *
     * @param message The message containing the game status.
     */
    private void handleGameStatus(Message message) {
        LOGGER.log(Level.INFO, "Game status update: " + message.getParameterValue(GAME_STATE_PARAM));
        this.gameState = GameState.valueOf(message.getParameterValue(GAME_STATE_PARAM));
        notifyObservers();
    }

    /**
     * Handles the SUCCESS message type.
     *
     * <p>For the game joiner, a {@code SUCCESS player=black} message is the server's first
     * response. It may also carry {@code position=N ruleset=CHESS960} for Chess 960 games.
     * Board initialization happens here when the {@code player=black} parameter is present
     * and the board has not yet been initialized.
     *
     * @param message The message indicating success.
     */
    private void handleSuccess(Message message) {
        LOGGER.log(Level.INFO, "Success: " + message);
        if (Objects.equals(message.getParameterValue(MOVE_PARAM), "accepted")) {
            LOGGER.log(Level.INFO, "Move accepted by server");
        }
        // Initialize board for the joiner when SUCCESS player=black arrives.
        if ("black".equals(message.getParameterValue("player")) && board == null) {
            Ruleset ruleset = buildRulesetFromServerParams(message);
            initializeBoard(ruleset);
        }
    }

    /**
     * Handles the ERROR message type.
     *
     * @param message The message indicating an error.
     */
    private void handleError(Message message) {
        LOGGER.log(Level.SEVERE, "Error: " + message.content());
    }

    /**
     * Handles the FAILURE message type.
     *
     * @param message The message indicating a failure.
     */
    private void handleFailure(Message message) {
        LOGGER.log(Level.SEVERE, "Failure: " + message.content());
        if (Objects.equals(message.getParameterValue(MOVE_PARAM), "rejected")) {
            restoreGameState();
            notifyObservers();
        }
    }

    /**
     * Handles a CHAT message relayed by the server.
     * Notifies all registered observers via {@link #notifyChatObservers}.
     *
     * @param message the incoming chat message (must carry {@code sender} and {@code content} params)
     */
    private void handleChat(Message message) {
        String sender  = message.getParameterValue(SENDER_PARAM);
        String content = message.getParameterValue(CONTENT_PARAM);
        if (sender == null)  sender  = "?";
        if (content == null) content = "";
        notifyChatObservers(sender, content);
    }

    /**
     * Handles a GAME_HISTORY message sent by the server when reconnecting to a paused game.
     *
     * <p>The message carries {@code moves=<csv>} — a comma-separated list of move protocol
     * strings. Replays each move in order to restore the board to its saved state. Moves that
     * fail to parse or execute are skipped with a warning.</p>
     *
     * @param message the history message from the server
     */
    private void handleGameHistory(Message message) {
        if (board == null) {
            // Board may not be initialised yet if this arrives before JOIN_CODE/SUCCESS.
            String rulesetStr = message.getParameterValue(RULESET_PARAM);
            RulesetOptions opts = selectedRuleset;
            if (rulesetStr != null) {
                try { opts = RulesetOptions.valueOf(rulesetStr); } catch (IllegalArgumentException ignored) {}
            }
            initializeBoard(createRuleset(opts));
        }

        String movesCsv = message.getParameterValue(MOVES_PARAM);
        if (movesCsv == null || movesCsv.isBlank()) {
            notifyObservers();
            return;
        }

        for (String wireString : movesCsv.split(",")) {
            if (wireString.isBlank()) continue;
            try {
                Player player = getCurrentPlayer();
                Move move = ruleset.deserializeMove(wireString.trim(), board, player);
                Square boardStart = toBoardSquare(move.getStart());
                Square boardEnd   = toBoardSquare(move.getEnd());
                Move canonical;
                if (move instanceof CastleMove cm) {
                    canonical = new CastleMove(boardStart, boardEnd, cm.getRookOriginFile(), cm.getKingDestFile());
                } else if (move instanceof PromotionMove pm) {
                    canonical = new PromotionMove(boardStart, boardEnd, pm.getTargetPiece());
                } else {
                    canonical = new Move(boardStart, boardEnd);
                }
                super.executeMove(canonical);
            } catch (IllegalMoveException | RuntimeException e) {
                LOGGER.log(Level.WARNING, "Skipping invalid history move ''{0}'': {1}", new Object[]{wireString, e.getMessage()});
            }
        }

        // Apply the persisted terminal state if provided
        String stateStr = message.getParameterValue(GAME_STATE_PARAM);
        if (stateStr != null) {
            try { gameState = GameState.valueOf(stateStr); } catch (IllegalArgumentException ignored) {}
        }

        notifyObservers();
    }

    /**
     * Handles a SAVE_GAME signal from the server, indicating that the opponent (or the server
     * on their behalf) has proposed saving the game for later. Sets the {@code saveOffered} flag
     * and notifies observers so that the UI can present an accept/decline dialog.
     */
    private void handleSaveGame() {
        saveOffered = true;
        notifyObservers();
    }

    /**
     * Handles a SAVE_ACCEPTED message: the server confirms the game has been persisted.
     * Transitions state to {@link GameState#SAVED} and notifies observers.
     */
    private void handleSaveAccepted() {
        gameState = GameState.SAVED;
        saveOffered = false;
        notifyObservers();
    }

    /**
     * Handles a MATCHED message from the matchmaking service.
     *
     * <p>Content format: {@code gameId=<id> color=WHITE|BLACK opponentName=<name> ruleset=<RULESET>
     * [position=<n>]}</p>
     *
     * <p>Initializes the board with the server-provided ruleset and sets the local player colour.
     * After this call the game is in {@link GameState#RUNNING} and ready for play.</p>
     *
     * @param message the MATCHED message from the server
     */
    private void handleMatched(Message message) {
        String colorStr = message.getParameterValue(COLOR_PARAM);
        localPlayerColor = "BLACK".equalsIgnoreCase(colorStr) ? PlayerColor.BLACK : PlayerColor.WHITE;

        if (board == null) {
            Ruleset r = buildRulesetFromServerParams(message);
            initializeBoard(r);
        }

        gameState = GameState.RUNNING;
        notifyObservers();
    }

    // ── Public helpers ─────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the opponent (or server) has proposed saving the current game.
     * The UI should query this flag after receiving an {@link GameObserver#onGameStateChanged()}
     * notification and present an accept/decline dialog when it is {@code true}.
     */
    public boolean isSaveOffered() {
        return saveOffered;
    }

    /**
     * Sends a CHAT message to the server.
     *
     * @param content the text the local player wants to send; must not be null or blank
     */
    public void sendChatMessage(String content) {
        if (content == null || content.isBlank()) return;
        Message msg = new Message(MessageType.CHAT, CONTENT_PARAM + "=" + content);
        sendMessageToServer(msg);
    }

    /**
     * Proposes saving the current game for later. The server will coordinate with the
     * opponent; if they also agree, a {@link MessageType#SAVE_ACCEPTED} confirmation follows.
     */
    public void requestSaveGame() {
        sendMessageToServer(new Message(MessageType.SAVE_GAME, ""));
    }

    /**
     * Sends a message to the server.
     *
     * @param message The message to be sent to the server.
     */
    public void sendMessageToServer(Message message) {
        if (connection != null) {
            connection.sendMessage(MessageParser.serialize(message));
        }
    }

    /**
     * Ends the game and notifies the server.
     */
    @Override
    public void endGame() {
        gameState = localPlayerColor == PlayerColor.WHITE ? GameState.WHITE_WON_BY_RESIGNATION : GameState.BLACK_WON_BY_RESIGNATION;
        if (connection != null) {
            Message endGameMessage = new Message(MessageType.GAME_STATUS, GAME_STATE_PARAM + "=" + gameState);
            sendMessageToServer(endGameMessage);
            connection.closeConnection();
        }
    }

    /**
     * Executes a move by the local player and sends it to the server.
     * It backs up the current game state to quickly revert if the server rejects the move.
     *
     * <p>Returns early with a warning if the board has not yet been initialized (the
     * deferred-init window between construction and the server's first response), preventing
     * a {@link NullPointerException} when board access is attempted.
     *
     * @param move The move to be executed.
     * @throws IllegalMoveException If the move is illegal.
     */
    @Override
    protected void executeMove(Move move) throws IllegalMoveException {
        if (board == null) {
            LOGGER.log(Level.WARNING, "executeMove called before board was initialized; ignoring.");
            return;
        }
        if (isLocalPlayerTurn()) {
            backupGameState();
            super.executeMove(move);
            sendMoveToServer(move);
        } else {
            throw new IllegalMoveException();
        }
    }

    /**
     * Checks if it is the local player's turn.
     *
     * @return true if it is the local player's turn, false otherwise.
     */
    private boolean isLocalPlayerTurn() {
        return (localPlayerColor == PlayerColor.WHITE && getCurrentPlayer() == player0) || (localPlayerColor == PlayerColor.BLACK && getCurrentPlayer() == player1);
    }

    /**
     * Sends a move to the server.
     *
     * @param move The move to be sent to the server.
     */
    private void sendMoveToServer(Move move) {
        Message moveMessage = new Message(MessageType.MOVE, MOVE_PARAM + "=" + move.toProtocolString() + " " + PLAYER_COLOR_PARAM + "=" + localPlayerColor);
        sendMessageToServer(moveMessage);
    }

    /**
     * Executes a move received from the server.
     * <p>
     * {@link Move#fromString} produces fresh {@link Square} instances that are not the same
     * objects as the squares held in the board's grid. Passing those disconnected squares
     * directly to {@link Game#executeMove} would mutate the wrong objects and leave the
     * board state unchanged. This method therefore translates the move's start and end
     * squares to the board's canonical {@link Square} instances via {@link #toBoardSquare},
     * then reconstructs the correct {@link Move} subtype ({@link CastleMove},
     * {@link PromotionMove}, or plain {@link Move}) before delegating to
     * {@link Game#executeMove}.
     *
     * <p>For {@link CastleMove} instances, the {@code rookOriginFile} and {@code kingDestFile}
     * fields are preserved from the deserialized move — these carry Chess 960 rook/king
     * destination information that must not be discarded.
     *
     * @param move The move received from the server (with fresh, non-canonical squares).
     * @throws IllegalMoveException If the move is illegal according to the current board state.
     */
    private void executeMoveFromRemote(Move move) throws IllegalMoveException {
        Square boardStart = toBoardSquare(move.getStart());
        Square boardEnd = toBoardSquare(move.getEnd());
        Move canonical;
        if (move instanceof CastleMove cm) {
            canonical = new CastleMove(boardStart, boardEnd, cm.getRookOriginFile(), cm.getKingDestFile());
        } else if (move instanceof PromotionMove pm) {
            canonical = new PromotionMove(boardStart, boardEnd, pm.getTargetPiece());
        } else {
            canonical = new Move(boardStart, boardEnd);
        }
        super.executeMove(canonical);
    }

    /**
     * Gets the legal squares for a given position.
     * Does not return the legal square for the online opponent if clicked on their piece.
     * Returns an empty list if the board has not yet been initialized (deferred-init window).
     *
     * @param position The position to get legal squares for.
     * @return A list of legal squares.
     */
    @Override
    public List<Square> getLegalSquares(Square position) {
        if (board == null) {
            return new ArrayList<>();
        }
        if (isLocalPlayerPiece(position)) {
            return super.getLegalSquares(position);
        }
        return new ArrayList<>();
    }

    /**
     * Checks if a piece at a given position belongs to the local player.
     *
     * @param position The position to check.
     * @return true if the piece belongs to the local player, false otherwise.
     */
    private boolean isLocalPlayerPiece(Square position) {
        if (board == null) {
            return false;
        }
        Piece p = board.getSquare(position.getY(), position.getX()).getPiece();
        return p != null && p.getPlayer().color() == localPlayerColor;
    }

    /**
     * Starts the game.
     */
    @Override
    public void startGame() {
        // Implementation for starting the game not needed in online game as the start is handled by the server
    }

    /**
     * Backs up the current game state.
     *
     * <p>Saves the board, move list, game state, halfmove clock, and position history.
     * The position history map is deep-copied so that subsequent mutations to the live map
     * cannot corrupt the backup.
     * If the board has not yet been initialized (deferred-init window), the board backup
     * is set to {@code null}.
     */
    public void backupGameState() {
        this.backupBoard = (board != null) ? this.getBoard().getCopy() : null;
        this.backupMoves = new ArrayList<>(this.moves);
        this.backupGameState = this.getState();
        this.backupHalfMoveClock = this.halfMoveClock;
        this.backupPositionHistory = new HashMap<>(this.positionHistory);
    }

    /**
     * Restores the game state from the backup.
     *
     * <p>Restores the board, move list, game state, halfmove clock, and position history
     * to the values captured by the most recent call to {@link #backupGameState()}.
     */
    public void restoreGameState() {
        this.board = (backupBoard != null) ? this.backupBoard.getCopy() : null;
        this.moves = new ArrayList<>(this.backupMoves);
        this.setGameState(this.backupGameState);
        this.halfMoveClock = this.backupHalfMoveClock;
        this.positionHistory = new HashMap<>(this.backupPositionHistory);
    }

    /**
     * Gets the join code for the game.
     *
     * @return The join code.
     */
    public String getJoinCode() {
        return joinCode;
    }
}
