package ptp.project.logic.ruleset;

import ptp.project.logic.*;
import ptp.project.logic.pieces.*;

import java.util.List;
import java.util.ArrayList;

public class StandardChessRuleset implements Ruleset {

    //height and width start at 1 and go to 8

    @Override
    public int getWidth() {
        return 8;
    }

    @Override
    public int getHeight() {
        return 8;
    }

    @Override
    public Square[][] getStartBoard(Player player1, Player player2) {
        Square[][] startBoard = new Square[8][8];
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                startBoard[x][y] = new Square(x, y);
            }
        }

        startBoard[0][0].setPiece(new Rook(player1));
        startBoard[1][0].setPiece(new Knight(player1));
        startBoard[2][0].setPiece(new Bishop(player1));
        startBoard[3][0].setPiece(new Queen(player1));
        startBoard[4][0].setPiece(new King(player1));
        startBoard[5][0].setPiece(new Bishop(player1));
        startBoard[6][0].setPiece(new Knight(player1));
        startBoard[7][0].setPiece(new Rook(player1));

        for (int x = 0; x < 8; x++) {
            startBoard[x][1].setPiece(new Pawn(player1));
        }

        startBoard[0][7].setPiece(new Rook(player2));
        startBoard[1][7].setPiece(new Knight(player2));
        startBoard[2][7].setPiece(new Bishop(player2));
        startBoard[3][7].setPiece(new Queen(player2));
        startBoard[4][7].setPiece(new King(player2));
        startBoard[5][7].setPiece(new Bishop(player2));
        startBoard[6][7].setPiece(new Knight(player2));
        startBoard[7][7].setPiece(new Rook(player2));

        for (int x = 0; x < 8; x++) {
            startBoard[x][6].setPiece(new Pawn(player2));
        }



        return new Square[0][];
    }

    @Override
    public List<Square> getLegalMoves(Square square) {




        List<Square> legalMoves = new ArrayList<>();
        //@TODO: Implement the logic for the legal moves of a piece
        return legalMoves;
    }

    @Override
    public boolean verifyMove(Move move) {
        return false;
    }

    @Override
    public boolean verifyMove(Square newPosition, Piece piece) {
        return false;
    }

    @Override
    public Move hasEnforcedMove(Player player) {
        return null;
    }

    @Override
    public boolean isCheck(Player player) {
        return false;
    }
}