package io.github.conava.chess.core.logic.game;

import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.data.pieces.Bishop;
import io.github.conava.chess.core.data.pieces.King;
import io.github.conava.chess.core.data.pieces.Knight;
import io.github.conava.chess.core.data.pieces.Pawn;
import io.github.conava.chess.core.data.pieces.Queen;
import io.github.conava.chess.core.data.pieces.Rook;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.pieces.Piece;
import io.github.conava.chess.core.data.pieces.Pieces;
import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.exceptions.IllegalMoveException;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.logic.moves.CastleMove;
import io.github.conava.chess.core.logic.moves.Move;
import io.github.conava.chess.core.logic.moves.PromotionMove;
import io.github.conava.chess.core.logic.observer.Observable;
import io.github.conava.chess.core.logic.ruleset.Ruleset;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import io.github.conava.chess.core.logic.ruleset.standardChessRuleset.StandardChessRuleset;

import java.util.ArrayList;
import java.util.HashMap;
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
    protected int halfMoveClock = 0;
    protected Map<String, Integer> positionHistory = new HashMap<>();

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
        // Record the initial position for threefold repetition tracking.
        positionHistory.put(computePositionKey(), 1);
    }

    private Player createPlayer(String playerName, PlayerColor color) {
        return new Player(playerName.isBlank() ? getDefaultPlayerName(color) : playerName, color);
    }

    private String getDefaultPlayerName(PlayerColor color) {
        return color == PlayerColor.WHITE ? "Player 1 (White)" : "Player 2 (Black)";
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
     * Static factory method that creates a server-side game instance with the given ruleset
     * and player names. This is the only approved way for the {@code server} module to
     * construct a game; direct instantiation of {@link ServerGame} from outside the
     * {@code core} module is prohibited by Architecture Law 2.
     *
     * <p>The returned game is in an uninitialised state. The caller must invoke
     * {@link #startGame()} to transition the game to {@link GameState#RUNNING} before
     * accepting moves.
     *
     * @param selectedRuleset  The ruleset to use for this game; must not be {@code null}.
     * @param playerWhiteName  Display name for the white player. A blank string causes the
     *                         {@link Game} superclass to substitute a default name.
     * @param playerBlackName  Display name for the black player. A blank string causes the
     *                         {@link Game} superclass to substitute a default name.
     * @return A new {@link Game} instance backed by a {@link ServerGame}.
     */
    public static Game createServerGame(RulesetOptions selectedRuleset,
                                        String playerWhiteName,
                                        String playerBlackName) {
        return new ServerGame(selectedRuleset, playerWhiteName, playerBlackName);
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
        Square start = toBoardSquare(squareStart);
        Square end = toBoardSquare(squareEnd);
        Move move;
        if (start.getPiece() instanceof King && Math.abs(end.getX() - start.getX()) == 2) {
            move = new CastleMove(start, end);
        } else {
            move = new Move(start, end);
        }
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
     * <p>The halfmove clock is updated before the board executes the move (so that
     * capture detection can inspect the destination square's current occupant).
     * After the board executes the move, {@link #evaluateGameEnd()} checks for
     * checkmate, stalemate, and all draw conditions.
     *
     * @param move The move to execute.
     * @throws IllegalMoveException If the move is illegal.
     */
    protected void executeMove(Move move) throws IllegalMoveException {
        if (gameState != GameState.RUNNING) {
            throw new IllegalMoveException(move);
        }
        if (isMoveValid(move)) {
            updateHalfMoveClock(move);
            board.executeMove(move);
            moves.add(move);
            turnCount++;
            evaluateGameEnd();
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
     * Updates the halfmove clock before a move is executed on the board.
     *
     * <p>The clock resets to zero on any pawn move or capture (including en passant).
     * Otherwise it increments by one. Must be called before {@code board.executeMove}
     * so that the destination square still reflects the pre-move board state.
     *
     * @param move The move about to be executed.
     */
    private void updateHalfMoveClock(Move move) {
        Piece movingPiece = move.getStart().getPiece();
        boolean isPawnMove = movingPiece instanceof Pawn;
        boolean isEnPassant = isPawnMove
                && move.getStart().getX() != move.getEnd().getX()
                && move.getEnd().getPiece() == null;
        boolean isCapture = move.getEnd().getPiece() != null || isEnPassant;
        halfMoveClock = (isPawnMove || isCapture) ? 0 : halfMoveClock + 1;
    }

    /**
     * Evaluates whether the game has ended after a move has been executed.
     *
     * <p>Checks, in order:
     * <ol>
     *   <li>Whether the next player has any legal move. If not:
     *       checkmate (if in check) or stalemate (if not in check).</li>
     *   <li>50-move rule: halfmove clock at or above 100.</li>
     *   <li>Threefold repetition: same position key appearing three times.</li>
     *   <li>Insufficient material: only kings, or king+minor vs king
     *       (including same-colour bishop pairs).</li>
     * </ol>
     */
    private void evaluateGameEnd() {
        Player nextPlayer = getCurrentPlayer();
        Player previousPlayer = (nextPlayer == player0) ? player1 : player0;

        boolean nextHasLegalMove = hasAnyLegalMove(nextPlayer);

        if (!nextHasLegalMove) {
            boolean nextInCheck = ruleset.isCheck(board, nextPlayer, moves);
            if (nextInCheck) {
                gameState = previousPlayer == player0
                        ? GameState.WHITE_WON_BY_CHECKMATE
                        : GameState.BLACK_WON_BY_CHECKMATE;
            } else {
                gameState = GameState.DRAW_BY_STALEMATE;
            }
            return;
        }

        if (halfMoveClock >= 100) {
            gameState = GameState.DRAW_BY_FIFTY_MOVE_RULE;
            return;
        }

        String positionKey = computePositionKey();
        positionHistory.merge(positionKey, 1, Integer::sum);
        if (positionHistory.get(positionKey) >= 3) {
            gameState = GameState.DRAW_BY_THREEFOLD_REPETITION;
            return;
        }

        if (isInsufficientMaterial()) {
            gameState = GameState.DRAW_BY_INSUFFICIENT_MATERIAL;
        }
    }

    /**
     * Returns {@code true} if the given player has at least one legal move available.
     * Short-circuits on the first non-empty result from the ruleset.
     *
     * @param player The player to check.
     * @return {@code true} if the player can make at least one legal move.
     */
    private boolean hasAnyLegalMove(Player player) {
        Player opponent = (player == player0) ? player1 : player0;
        for (Square square : board.getPieces(player)) {
            if (square.getPiece() == null) {
                continue;
            }
            if (!ruleset.getLegalSquares(square, board, moves, player, opponent).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Computes a string key that uniquely identifies the current board position
     * for the purpose of threefold repetition detection.
     *
     * <p>The key encodes:
     * <ul>
     *   <li>Active colour (whose turn it is after the move)</li>
     *   <li>Piece placement on all 64 squares</li>
     *   <li>Castling rights (based on king/rook {@code hasMoved} state)</li>
     *   <li>En passant target file (if the last move was a double pawn push)</li>
     * </ul>
     *
     * @return A string fingerprint of the current position.
     */
    private String computePositionKey() {
        StringBuilder sb = new StringBuilder();

        // Active colour
        sb.append(getCurrentPlayer() == player0 ? 'W' : 'B');

        // Piece placement
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece piece = board.getSquare(y, x).getPiece();
                if (piece == null) {
                    sb.append('.');
                } else {
                    char c = switch (piece.getType()) {
                        case PAWN -> 'P';
                        case ROOK -> 'R';
                        case KNIGHT -> 'N';
                        case BISHOP -> 'B';
                        case QUEEN -> 'Q';
                        case KING -> 'K';
                    };
                    if (piece.getPlayer().color() == PlayerColor.BLACK) {
                        c = Character.toLowerCase(c);
                    }
                    sb.append(c);
                }
            }
        }

        // Castling rights
        sb.append(castlingChar(0, 4, 7)); // white kingside
        sb.append(castlingChar(0, 4, 0)); // white queenside
        sb.append(castlingChar(7, 4, 7)); // black kingside
        sb.append(castlingChar(7, 4, 0)); // black queenside

        // En passant target file
        int epFile = -1;
        if (!moves.isEmpty()) {
            Move lastMove = moves.get(moves.size() - 1);
            Piece lastPiece = lastMove.getEnd().getPiece();
            if (lastPiece instanceof Pawn
                    && Math.abs(lastMove.getEnd().getY() - lastMove.getStart().getY()) == 2) {
                epFile = lastMove.getEnd().getX();
            }
        }
        sb.append(epFile);

        return sb.toString();
    }

    /**
     * Returns a character indicating whether a specific castling right is available.
     * A castling right is available when neither the king on {@code kingX} nor the
     * rook on {@code rookX} at rank {@code rank} has moved.
     *
     * @param rank  The rank (y-coordinate) of the king and rook.
     * @param kingX The file (x-coordinate) of the king.
     * @param rookX The file (x-coordinate) of the rook.
     * @return {@code '1'} if castling is still available, {@code '0'} otherwise.
     */
    private char castlingChar(int rank, int kingX, int rookX) {
        Piece kingPiece = board.getSquare(rank, kingX).getPiece();
        Piece rookPiece = board.getSquare(rank, rookX).getPiece();
        if (kingPiece instanceof King king && !king.getHasMoved()
                && rookPiece instanceof Rook rook && rook.getHasNotMoved()
                && rook.getPlayer().equals(king.getPlayer())) {
            return '1';
        }
        return '0';
    }

    /**
     * Determines whether the remaining material on the board is insufficient for
     * either side to deliver checkmate.
     *
     * <p>FIDE draw conditions handled:
     * <ul>
     *   <li>King vs. King</li>
     *   <li>King + Bishop vs. King</li>
     *   <li>King + Knight vs. King</li>
     *   <li>King + Bishop vs. King + Bishop (same colour square bishops)</li>
     * </ul>
     *
     * @return {@code true} if the material is insufficient for checkmate.
     */
    private boolean isInsufficientMaterial() {
        List<Square> whitePieces = board.getPieces(player0);
        List<Square> blackPieces = board.getPieces(player1);

        List<Piece> whiteNonKing = new ArrayList<>();
        List<Piece> blackNonKing = new ArrayList<>();

        for (Square s : whitePieces) {
            Piece p = s.getPiece();
            if (p != null && !(p instanceof King)) {
                whiteNonKing.add(p);
            }
        }
        for (Square s : blackPieces) {
            Piece p = s.getPiece();
            if (p != null && !(p instanceof King)) {
                blackNonKing.add(p);
            }
        }

        // K vs K
        if (whiteNonKing.isEmpty() && blackNonKing.isEmpty()) {
            return true;
        }

        // K+minor vs K
        if (whiteNonKing.isEmpty() && blackNonKing.size() == 1) {
            Piece p = blackNonKing.get(0);
            if (p instanceof Bishop || p instanceof Knight) {
                return true;
            }
        }
        if (blackNonKing.isEmpty() && whiteNonKing.size() == 1) {
            Piece p = whiteNonKing.get(0);
            if (p instanceof Bishop || p instanceof Knight) {
                return true;
            }
        }

        // K+B vs K+B same colour bishops
        if (whiteNonKing.size() == 1 && blackNonKing.size() == 1
                && whiteNonKing.get(0) instanceof Bishop
                && blackNonKing.get(0) instanceof Bishop) {
            int whiteBishopColor = findBishopSquareColor(whitePieces);
            int blackBishopColor = findBishopSquareColor(blackPieces);
            if (whiteBishopColor == blackBishopColor) {
                return true;
            }
        }

        return false;
    }

    /**
     * Finds the square colour of the bishop in the given piece list.
     * Returns 0 for a dark square, 1 for a light square.
     *
     * @param pieces The list of occupied squares for a player.
     * @return The square colour of the bishop (0 or 1), or -1 if no bishop found.
     */
    private int findBishopSquareColor(List<Square> pieces) {
        for (Square s : pieces) {
            if (s.getPiece() instanceof Bishop) {
                return (s.getX() + s.getY()) % 2;
            }
        }
        return -1;
    }
}