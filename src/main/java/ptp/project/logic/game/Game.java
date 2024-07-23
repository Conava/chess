package ptp.project.logic.game;

import ptp.project.data.Player;
import ptp.project.data.Square;
import ptp.project.data.enums.GameState;
import ptp.project.data.enums.Pieces;
import ptp.project.data.enums.PlayerColor;
import ptp.project.data.enums.RulesetOptions;
import ptp.project.data.pieces.Piece;
import ptp.project.exceptions.IllegalMoveException;
import ptp.project.data.board.Board;
import ptp.project.logic.moves.Move;
import ptp.project.logic.moves.PromotionMove;
import ptp.project.logic.ruleset.Ruleset;
import ptp.project.logic.ruleset.standardChessRuleset.StandardChessRuleset;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public abstract class Game extends Observable {
    protected GameState gameState;
    protected Player player0;
    protected Player player1;
    protected Ruleset ruleset;
    protected Board board;
    protected int turnCount; //even turn count means white to move
    protected List<Move> moves;

    public Game(RulesetOptions selectedRuleset, String playerWhiteName, String playerBlackName) {
        String playerWhiteNameFinal = playerWhiteName.isBlank() ? "Spieler 0 (Weiß)" : playerWhiteName;
        String playerBlackNameFinal = playerBlackName.isBlank() ? "Spieler 1 (Schwarz)" : playerBlackName;

        this.player0 = new Player(playerWhiteNameFinal, PlayerColor.WHITE);
        this.player1 = new Player(playerBlackNameFinal, PlayerColor.BLACK);
        this.ruleset = switch (selectedRuleset) {
            case STANDARD -> new StandardChessRuleset();
            //Implement other rulesets here
        };
        this.board = new Board(ruleset.getStartBoard(player0, player1));
        this.turnCount = 0;
        this.moves = new ArrayList<>();
    }

    public abstract void startGame();

    public abstract void endGame();

    public Player getCurrentPlayer() {
        return turnCount % 2 == 0 ? player0 : player1;
    }

    public Player getPlayerWhite() {
        return player0;
    }

    public Player getPlayerBlack() {
        return player1;
    }

    public GameState getState() {
        return ruleset.getGameState(board, moves);
    }

    public List<String> getMoveList() {
        return getMovesAsStrings();
    }

    private List<String>getMovesAsStrings() {
        List<String> moveList = new ArrayList<>();
        for (Move move : moves) {
            moveList.add(move.toString());
        }
        return moveList;
    }

    public Piece getPieceAt(Square position) {
        return board.getPieceAt(position);
    }

    public Board getBoard() {
        return board.getCopy();
    }

    public List<Square> getLegalSquares(Square position) {
        List<Square> legalSquares = new ArrayList<>();
        if (position == null) {
            return legalSquares;
        }
        Square square = board.getSquare(position.getY(), position.getX());
        if (square.isOccupiedBy() == null) {
            return legalSquares;
        }
        else if (!square.isOccupiedBy().equals(getCurrentPlayer())) {
            return legalSquares;
        }
        return ruleset.getLegalSquares(square, board, moves, player0, player1);
    }

    public Piece getNewPiece(Pieces targetPiece, Player player) {
        try {
            Class<?> pieceClass = Class.forName("ptp.project.data.pieces." + targetPiece.name());
            Constructor<?> pieceConstructor = pieceClass.getConstructor(Player.class);
            return (Piece) pieceConstructor.newInstance(player);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    public void movePiece(Square squareStart, Square squareEnd) throws IllegalMoveException {
        Move move = new Move(toBoardSquare(squareStart), toBoardSquare(squareEnd));
        executeMove(squareStart, squareEnd, move);
    }

    public void promoteMove(Square squareStart, Square squareEnd, Pieces targetPiece) throws IllegalMoveException {
        Move move = new PromotionMove(toBoardSquare(squareStart), toBoardSquare(squareEnd), getNewPiece(targetPiece, this.getCurrentPlayer()));
        executeMove(squareStart, squareEnd, move);
    }

    protected abstract void executeMove(Square squareStart, Square squareEnd, Move move) throws IllegalMoveException;

    protected Square toBoardSquare(Square square) {
        return board.getSquare(square.getY(), square.getX());
    }
}
