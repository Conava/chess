package ptp.project.logic.ruleset;

import ptp.project.logic.Piece;
import ptp.project.logic.Player;
import ptp.project.logic.Ruleset;
import ptp.project.logic.Square;

import java.util.List;
import java.util.ArrayList;

public class StandardChessRuleset implements Ruleset {

    @Override
    public List<Square> getLegalMoves(Piece piece) {
        List<Square> legalMoves = new ArrayList<>();
        //@TODO: Implement the logic for the legal moves of a piece
        return legalMoves;
    }

    @Override
    public boolean verifyMove(Square newPosition, Piece piece) {
        return false;
    }

    @Override
    public Square hasEnforcedMove(Player player) {
        return null;
    }

    @Override
    public boolean isCheck(Player player) {
        return false;
    }
}