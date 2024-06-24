package ptp.project.logic.game;

import ptp.project.data.Player;
import ptp.project.data.Square;
import ptp.project.data.enums.GameState;
import ptp.project.data.enums.RulesetOptions;
import ptp.project.data.pieces.Piece;
import ptp.project.exceptions.IllegalMoveException;
import ptp.project.data.board.Board;
import ptp.project.logic.moves.Move;
import ptp.project.logic.ruleset.Ruleset;
import ptp.project.logic.ruleset.StandardChessRuleset;

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

    public Game(RulesetOptions selectedRuleset) {
        this.player0 = new Player("Spieler 0 (Weiß)", "white");
        this.player1 = new Player("Spieler 1 (Schwarz)", "black");
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
        return gameState;
    }

    public List<Move> getMoveList() {
        return moves;
    }

    public Piece getPieceAt(Square position) {
        return board.getPieceAt(position);
    }

    public Board getBoard() {
        return board.getCopy();
    }

    public List<Square> getLegalSquares(Square position) {
        List<Square> legalSquares = new ArrayList<>();
        Square square = board.getSquare(position.getY(), position.getX());
        if (square.isOccupiedBy() == null) {
            return legalSquares;
        }
        else if (!square.isOccupiedBy().equals(getCurrentPlayer())) {
            return legalSquares;
        }
        int moveAmount = ruleset.getLegalMoves(square, board, moves, player0, player1).size();
        Move moveTemp;
        for (int i = 0; i < moveAmount; i++) {
            moveTemp = ruleset.getLegalMoves(square, board, moves, player0, player1).get(i);
            legalSquares.add(moveTemp.getEnd());
        }
        return legalSquares;
    }

    public abstract void movePiece(Square squareStart, Square squareEnd) throws IllegalMoveException;
}
