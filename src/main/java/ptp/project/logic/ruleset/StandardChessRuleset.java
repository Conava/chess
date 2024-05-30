package ptp.project.logic.ruleset;

import ptp.project.logic.*;
import ptp.project.logic.pieces.Rook;

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
    public Square[][] getStartBoard() {
        Square[][] startBoard = new Square[8][8];
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                startBoard[x][y] = new Square(x, y);
            }
        }

        startBoard[1][1].setPiece(new Rook(null, null));

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