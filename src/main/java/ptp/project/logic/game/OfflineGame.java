package ptp.project.logic.game;

import ptp.project.exceptions.IllegalMoveException;
import ptp.project.exceptions.IsCheckException;
import ptp.project.logic.*;
import ptp.project.logic.moves.Move;
import ptp.project.logic.pieces.Piece;
import ptp.project.logic.ruleset.StandardChessRuleset;

import java.util.ArrayList;
import java.util.List;

public class OfflineGame implements Game {
    Player player1;
    Player player2;
    Ruleset ruleset;
    Board board;
    int turnCount; //even turn count means white to move
    List<Move> moves;

    public OfflineGame() {
        player1 = new Player("Player 1", "white");
        player2 = new Player("Player 2", "black");
        this.ruleset = new StandardChessRuleset(); //current default
        this.board = new Board(ruleset.getStartBoard(player1, player2));
        this.turnCount = 0;
        this.moves = new ArrayList<Move>();
    }
    @Override
    public void start() {

    }

    private void game() {

    }

    @Override
    public Board getBoard() {
        return board;
    }

    @Override
    public Player getCurrentPlayer() {
        return turnCount%2 == 0 ? player1 : player2;
    }

    @Override
    public Player getPlayer1() {
        return player1;
    }

    @Override
    public Player getPlayer2() {
        return player2;
    }

    @Override
    public Ruleset getRuleset() {
        return ruleset;
    }

    @Override
    public Piece getPieceAt(Square notation) {
        return null;
    }

    @Override
    public List<Square> getLegalSquares(Square square) {
        try {
            return ruleset.getLegalSquares(square, board, moves);
        } catch (IsCheckException e) {
            return null;
        }
    }

    @Override
    public void movePiece(Move move) throws IllegalMoveException{
        Square square1 = move.getStart();
        Square square2 = move.getEnd();
        Player player = this.getCurrentPlayer();

        if(ruleset.isValidSquare(square1)) {
            if (square1.isOccupiedBy() != null && square1.isOccupiedBy().equals(player)) {
                if(this.getLegalSquares(square1).contains(square2)){
                    board.executeMove(move);
                    moves.add(move);
                    turnCount++;
                    return; //update here
                }
            }
        }
        throw new IllegalMoveException(move);
    }

    private void addMoves(Move move) {
        moves.add(move);
    }

    @Override
    public List<Move> getMoveList() {
        return moves;
    }
}
