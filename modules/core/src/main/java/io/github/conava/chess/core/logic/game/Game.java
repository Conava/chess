package io.github.conava.chess.core.logic.game;

import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.data.pieces.Bishop;
import io.github.conava.chess.core.data.pieces.King;
import io.github.conava.chess.core.data.pieces.Knight;
import io.github.conava.chess.core.data.pieces.Queen;
import io.github.conava.chess.core.data.pieces.Rook;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.pieces.Piece;
import io.github.conava.chess.core.data.pieces.Pieces;
import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.exceptions.IllegalMoveException;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.logic.moves.Move;
import io.github.conava.chess.core.logic.moves.PromotionMove;
import io.github.conava.chess.core.logic.observer.Observable;
import io.github.conava.chess.core.logic.ruleset.Ruleset;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import io.github.conava.chess.core.logic.ruleset.standardChessRuleset.StandardChessRuleset;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Abstract class representing a game instance.
 */
public abstract class Game extends Observable {
    private static final Logger LOGGER = Logger.getLogger(Game.class.getName());
    protected GameState gameState;
    protected Player player0;
    protected Player player1;
    protected Ruleset ruleset;
    protected Board board;
    protected int turnCount;
    protected List<Move> moves;

    /**
     * Constructor for the Game class.
     *
     * @param selectedRuleset The ruleset to be used for the game.
     * @param playerWhiteName The name of the white player.
     * @param playerBlackName The name of the black player.
     */
    public Game(RulesetOptions selectedRuleset, String playerWhiteName, String playerBlackName) {
        this.player0 = createPlayer(playerWhiteName, PlayerColor.WHITE);
        this.player1 = createPlayer(playerBlackName, PlayerColor.BLACK);
        this.ruleset = createRuleset(selectedRuleset);
        this.board = new Board(ruleset.getStartBoard(player0, player1));
        this.turnCount = 0;
        this.moves = new ArrayList<>();
    }

    private Player createPlayer(String playerName, PlayerColor color) {
        return new Player(playerName.isBlank() ? getDefaultPlayerName(color) : playerName, color);
    }

    private String getDefaultPlayerName(PlayerColor color) {
        return color == PlayerColor.WHITE ? "Spieler 0 (Weiß)" : "Spieler 1 (Schwarz)";
    }

    private Ruleset createRuleset(RulesetOptions selectedRuleset) {
        return switch (selectedRuleset) {
            case STANDARD -> new StandardChessRuleset();
            // Implement other rulesets here
        };
    }

    /**
     * Static factory method that creates either an {@link OfflineGame} or an {@link OnlineGame}
     * depending on the {@code online} flag. This is the only approved way to construct a game
     * instance from outside the {@code core} module; direct subclass instantiation by callers
     * in {@code application} or {@code server} is prohibited (Architecture Law 2).
     *
     * <p>When {@code online} is {@code false}, the {@code onlineGameSettings} and
     * {@code connection} parameters are ignored and may be {@code null}.
     *
     * <p>When {@code online} is {@code true}, callers must invoke
     * {@link #connectToServerGame()} on the returned instance after registering a message
     * handler — see {@link OnlineGame#create} for the two-phase construction contract.
     *
     * @param online               {@code true} to create an online game, {@code false} for offline.
     * @param selectedRuleset      The ruleset to use for this game.
     * @param playerWhiteName      The name of the white player.
     * @param playerBlackName      The name of the black player.
     * @param onlineGameSettings   Key-value settings for online games (e.g. join code).
     *                             Ignored when {@code online} is {@code false}.
     *                             Must not be {@code null} when {@code online} is {@code true}.
     * @param connection           The {@link ServerConnection} for online communication.
     *                             Ignored when {@code online} is {@code false}.
     *                             Must not be {@code null} when {@code online} is {@code true}.
     * @return A new {@link Game} instance of the appropriate subtype.
     * @throws IllegalArgumentException if {@code online} is {@code true} and either
     *                                  {@code onlineGameSettings} or {@code connection} is {@code null}.
     */
    public static Game createGame(boolean online,
                                  RulesetOptions selectedRuleset,
                                  String playerWhiteName,
                                  String playerBlackName,
                                  Map<String, String> onlineGameSettings,
                                  ServerConnection connection) {
        if (online) {
            if (onlineGameSettings == null) {
                throw new IllegalArgumentException("onlineGameSettings must not be null for an online game");
            }
            if (connection == null) {
                throw new IllegalArgumentException("connection must not be null for an online game");
            }
            return OnlineGame.create(selectedRuleset, playerWhiteName, playerBlackName, onlineGameSettings, connection);
        } else {
            return new OfflineGame(selectedRuleset, playerWhiteName, playerBlackName);
        }
    }

    /**
     * Returns the join code for this game session.
     *
     * <p>The default implementation returns {@code null}, indicating that this game has no
     * server-assigned join code. {@link OnlineGame} overrides this method to return the
     * actual join code received from the server.
     *
     * @return The join code string, or {@code null} if this is not an online game.
     */
    public String getJoinCode() {
        return null;
    }

    /**
     * Sends the initial handshake to the game server, establishing participation in the game
     * session (either creating a new game or joining an existing one via join code).
     *
     * <p>The default implementation is a no-op for game types that do not require server
     * communication (e.g. {@link OfflineGame}). {@link OnlineGame} overrides this method
     * with the real two-phase connection logic. Callers must invoke this method after
     * registering a message handler and confirming that the connection is live.
     */
    public void connectToServerGame() {
        // No-op for non-online games.
    }

    /**
     * Dispatches an incoming server message to the appropriate handler within this game instance.
     *
     * <p>The default implementation is a no-op for game types that do not communicate with a
     * server (e.g. {@link OfflineGame}). {@link OnlineGame} overrides this method to process
     * {@code JOIN_CODE}, {@code MOVE}, {@code GAME_STATUS}, {@code SUCCESS}, {@code ERROR},
     * and {@code FAILURE} messages.
     *
     * @param message The {@link Message} received from the server.
     */
    public void handleMessage(Message message) {
        // No-op for non-online games.
    }

    /**
     * Moves a piece from one square to another.
     *
     * @param squareStart The starting square.
     * @param squareEnd   The ending square.
     * @throws IllegalMoveException If the move is illegal.
     */
    public void movePiece(Square squareStart, Square squareEnd) throws IllegalMoveException {
        Move move = new Move(toBoardSquare(squareStart), toBoardSquare(squareEnd));
        executeMove(move);
    }

    /**
     * Gets the legal squares for a piece at a given position.
     *
     * @param position The position to check.
     * @return The list of legal squares.
     */
    public List<Square> getLegalSquares(Square position) {
        if (position == null) {
            return new ArrayList<>();
        }
        Square square = board.getSquare(position.getY(), position.getX());
        if (square.isOccupiedBy() == null || !square.isOccupiedBy().equals(getCurrentPlayer())) {
            return new ArrayList<>();
        }
        return ruleset.getLegalSquares(square, board, moves, player0, player1);
    }

    /**
     * Gets the current player.
     *
     * @return The current player.
     */
    public Player getCurrentPlayer() {
        return turnCount % 2 == 0 ? player0 : player1;
    }

    /**
     * Gets the white player.
     *
     * @return The white player.
     */
    public Player getPlayerWhite() {
        return player0;
    }

    /**
     * Gets the black player.
     *
     * @return The black player.
     */
    public Player getPlayerBlack() {
        return player1;
    }

    /**
     * Gets the current game state.
     *
     * @return The current game state.
     */
    public GameState getState() {
        return gameState;
    }

    /**
     * Gets the list of moves as strings.
     *
     * @return The list of moves as strings.
     */
    public List<String> getMoveList() {
        return getMovesAsStrings();
    }

    /**
     * Gets the piece at a given position.
     *
     * @param position The position to check.
     * @return The piece at the given position.
     */
    public Piece getPieceAt(Square position) {
        return board.getPieceAt(toBoardSquare(position));
    }

    /**
     * Gets a copy of the board.
     *
     * @return A copy of the board.
     */
    public Board getBoard() {
        return board.getCopy();
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    /**
     * Ends the game.
     */
    public abstract void endGame();

    /**
     * Promotes a piece and moves it from one square to another.
     *
     * @param squareStart The starting square.
     * @param squareEnd   The ending square.
     * @param targetPiece The piece to promote to.
     * @throws IllegalMoveException If the move is illegal.
     */
    public void promoteMove(Square squareStart, Square squareEnd, Pieces targetPiece) throws IllegalMoveException {
        Move move = new PromotionMove(toBoardSquare(squareStart), toBoardSquare(squareEnd), getNewPiece(targetPiece, this.getCurrentPlayer()));
        executeMove(move);
    }

    /**
     * Starts the game.
     */
    public abstract void startGame();

    /**
     * Converts a square to a board square.
     *
     * @param square The square to convert.
     * @return The board square.
     */
    protected Square toBoardSquare(Square square) {
        return board.getSquare(square.getY(), square.getX());
    }

    /**
     * Executes a move.
     *
     * @param move The move to execute.
     * @throws IllegalMoveException If the move is illegal.
     */
    protected void executeMove(Move move) throws IllegalMoveException {
        if (isMoveValid(move)) {
            checkForGameEnd(move);
            board.executeMove(move);
            moves.add(move);
            turnCount++;
            notifyObservers();
        } else {
            throw new IllegalMoveException(move);
        }
    }

    /**
     * Validates if a move is legal.
     *
     * @param move The move to validate.
     * @return true if the move is valid, false otherwise.
     */
    private boolean isMoveValid(Move move) {
        Square squareStart = move.getStart();
        Square squareEnd = move.getEnd();
        Player player = this.getCurrentPlayer();
        Player startSquarePlayer = squareStart.isOccupiedBy();

        return ruleset.isValidSquare(squareStart) &&
                startSquarePlayer != null &&
                startSquarePlayer == player &&
                this.getLegalSquares(squareStart).contains(squareEnd);
    }

    /**
     * Converts the list of moves to strings.
     *
     * @return The list of moves as strings.
     */
    private List<String> getMovesAsStrings() {
        List<String> moveList = new ArrayList<>();
        for (Move move : moves) {
            moveList.add(move.toString());
        }
        return moveList;
    }

    /**
     * Creates a new piece of the given type for the given player.
     *
     * @param targetPiece The type of piece to create. Must be one of {@code QUEEN}, {@code ROOK},
     *                    {@code BISHOP}, or {@code KNIGHT}. Passing {@code KING} or {@code PAWN}
     *                    is not valid for promotion.
     * @param player      The player for whom the piece is created.
     * @return The new piece.
     * @throws IllegalArgumentException if {@code targetPiece} is {@code KING} or {@code PAWN},
     *                                  as neither is a valid promotion target.
     */
    private Piece getNewPiece(Pieces targetPiece, Player player) {
        return switch (targetPiece) {
            case QUEEN  -> new Queen(player);
            case ROOK   -> new Rook(player);
            case BISHOP -> new Bishop(player);
            case KNIGHT -> new Knight(player);
            default     -> throw new IllegalArgumentException(
                    "Cannot promote to " + targetPiece + "; only QUEEN, ROOK, BISHOP, KNIGHT are valid");
        };
    }

    /**
     * Checks if the game will be ending after the execution of move and sets the game state accordingly.
     *
     * @param move The move to check.
     */
    private void checkForGameEnd(Move move) {
        Piece piece = toBoardSquare(move.getEnd()).getPiece();
        if (piece instanceof King king) {
            LOGGER.info("King captured, changing gameState");
            gameState = king.getPlayer().color() == PlayerColor.WHITE ? GameState.BLACK_WON_BY_CHECKMATE : GameState.WHITE_WON_BY_CHECKMATE;
        }
    }
}