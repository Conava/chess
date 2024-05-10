package ptp.project.logic;

import java.util.List;

public interface Ruleset {
    List<Square> getLegalMoves(Piece piece);

    boolean verifyMove(Square newPosition, Piece piece);

    Square hasEnforcedMove(Player player); //@todo: Find good way to return the enforced move

    boolean isCheck(Player player);
}