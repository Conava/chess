package ptp.project.logic;

import java.util.List;

public interface Ruleset {

    int getWidth();

    int getHeight();

    Square[][] getStartBoard(Player player1, Player player2);

    List<Square> getLegalMoves(Square square);

    boolean verifyMove(Move move);

    boolean verifyMove(Square newPosition, Piece piece);

    Move hasEnforcedMove(Player player); //@todo: Find good way to return the enforced move

    boolean isCheck(Player player);
}