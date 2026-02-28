package io.github.conava.chess.core.logic.game;

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

/**
 * Abstract class representing a game instance.
 */
public abstract class Game extends Observable {
    protected GameState gameState;
    protected Player player0;
    protected Player player1;
    protected Ruleset ruleset;
    protected Board board;
    protected int turnCount;
    protected List<Move> moves;
    protected GameType gameType;

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
     * @param targetPiece The type of piece to create.
     * @param player      The player for whom the piece is created.
     * @return The new piece.
     */
    private Piece getNewPiece(Pieces targetPiece, Player player) {
        return switch (targetPiece) {
            case QUEEN  -> new Queen(player);
            case ROOK   -> new Rook(player);
            case BISHOP -> new Bishop(player);
            case KNIGHT -> new Knight(player);
            default     -> null;
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
            System.out.println("King captured, changing gameState");
            gameState = king.getPlayer().color() == PlayerColor.WHITE ? GameState.BLACK_WON_BY_CHECKMATE : GameState.WHITE_WON_BY_CHECKMATE;
        }
    }
}