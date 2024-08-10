package ptp.core.logic.game;

import ptp.core.data.player.Player;
import ptp.core.data.Square;
import ptp.core.data.pieces.Piece;
import ptp.core.data.pieces.Pieces;
import ptp.core.data.player.PlayerColor;
import ptp.core.exceptions.IllegalMoveException;
import ptp.core.data.board.Board;
import ptp.core.logic.moves.Move;
import ptp.core.logic.moves.PromotionMove;
import ptp.core.logic.observer.Observable;
import ptp.core.logic.ruleset.Ruleset;
import ptp.core.logic.ruleset.RulesetOptions;
import ptp.core.logic.ruleset.standardChessRuleset.StandardChessRuleset;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
        String playerWhiteNameFinal = playerWhiteName.isBlank() ? "Spieler 0 (Weiß)" : playerWhiteName;
        String playerBlackNameFinal = playerBlackName.isBlank() ? "Spieler 1 (Schwarz)" : playerBlackName;

        this.player0 = new Player(playerWhiteNameFinal, PlayerColor.WHITE);
        this.player1 = new Player(playerBlackNameFinal, PlayerColor.BLACK);
        this.ruleset = switch (selectedRuleset) {
            case STANDARD -> new StandardChessRuleset();
            // Implement other rulesets here
        };
        this.board = new Board(ruleset.getStartBoard(player0, player1));
        this.turnCount = 0;
        this.moves = new ArrayList<>();
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
        List<Square> legalSquares = new ArrayList<>();
        if (position == null) {
            return legalSquares;
        }
        Square square = board.getSquare(position.getY(), position.getX());
        if (square.isOccupiedBy() == null) {
            return legalSquares;
        } else if (!square.isOccupiedBy().equals(getCurrentPlayer())) {
            return legalSquares;
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
        return ruleset.getGameState(board, moves);
        //todo: Implement a persistent game state in Game that gets updated by the ruleset after moves if necessary
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
        return board.getPieceAt(position);
    }

    /**
     * Gets a copy of the board.
     *
     * @return A copy of the board.
     */
    public Board getBoard() {
        return board.getCopy();
    }

    /**
     * Starts the game.
     */
    public void startGame() {
        gameState = GameState.RUNNING;
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
        Square squareStart = move.getStart();
        Square squareEnd = move.getEnd();

        Player player = this.getCurrentPlayer();
        Player startSquarePlayer = squareStart.isOccupiedBy();

        if (ruleset.isValidSquare(squareStart)) {
            if (squareStart.isOccupiedBy() != null && startSquarePlayer == player) {
                if (this.getLegalSquares(squareStart).contains(squareEnd)) {
                    board.executeMove(move);
                    moves.add(move);
                    turnCount++;
                    return;
                }
            }
        }
        throw new IllegalMoveException(move);
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
        try {
            Class<?> pieceClass = Class.forName("ptp.core.data.pieces." + targetPiece.getClassName());
            Constructor<?> pieceConstructor = pieceClass.getConstructor(Player.class);
            return (Piece) pieceConstructor.newInstance(player);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }
}