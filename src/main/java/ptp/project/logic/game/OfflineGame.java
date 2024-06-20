package ptp.project.logic.game;

import ptp.project.exceptions.IllegalMoveException;
import ptp.project.exceptions.IsCheckException;
import ptp.project.logic.*;
import ptp.project.logic.moves.Move;
import ptp.project.logic.pieces.Piece;
import ptp.project.logic.ruleset.StandardChessRuleset;

import java.util.ArrayList;
import java.util.List;

public class OfflineGame extends Observable implements Game {
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
        this.moves = new ArrayList<>();
    }

    @Override
    public void start() {

    }

    @Override
    public Player getCurrentPlayer() {
        return turnCount % 2 == 0 ? player1 : player2;
    }

    @Override
    public Player getPlayerWhite() {
        return player1;
    }

    @Override
    public Player getPlayerBlack() {
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

        List<Square> legalSquares = new ArrayList<>();
        if (square != null && !square.isOccupiedBy().equals(getCurrentPlayer())) {
            return legalSquares;
        }
        int moveAmount = ruleset.getLegalMoves(square, board, moves, player1, player2).size();
        Move moveTemp;
        for (int i = 0; i < moveAmount; i++) {
            moveTemp = ruleset.getLegalMoves(square, board, moves, player1, player2).get(i);
            legalSquares.add(moveTemp.getEnd());
        }
        return legalSquares;
    }

    @Override
    public void movePiece(Move move) throws IllegalMoveException {
        Square square1 = move.getStart();
        Square square2 = move.getEnd();
        Player player = this.getCurrentPlayer();

        if (ruleset.isValidSquare(square1)) {
            if (square1.isOccupiedBy() != null && square1.isOccupiedBy().equals(player)) {
                if (this.getLegalSquares(square1).contains(square2)) {
                    board.executeMove(move);
                    moves.add(move);
                    turnCount++;
                    return; //update here
                }
            }
        }
        throw new IllegalMoveException(move);
    }

    @Override
    public List<Move> getMoveList() {
        return moves;
    }

    private Square toBoardSquare(Square square) {
        return board.getSquare(square.getY(), square.getX());
    }
}