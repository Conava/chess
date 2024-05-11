package ptp.project.logic.ruleset;

import ptp.project.logic.*;

import java.util.List;
import java.util.ArrayList;

public class StandardChessRuleset implements Ruleset {

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public Square[][] getStartBoard() {
        return new Square[0][];
    }

    @Override
    public List<Square> getLegalMoves(Piece piece) {
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