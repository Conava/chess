package ptp.project.logic;

import ptp.project.exceptions.IsCheckException;
import ptp.project.logic.moves.Move;
import ptp.project.logic.pieces.Piece;

import java.util.List;

public interface Ruleset {

    int getWidth();

    int getHeight();

    Square[][] getStartBoard(Player player1, Player player2);

    List<Square> getLegalSquares(Square square, Board board, List<Move> moves) throws IsCheckException;

    boolean isValidSquare(Square square);

    boolean verifyMove(Move move);

    boolean verifyMove(Square newPosition, Piece piece);

    Move hasEnforcedMove(Player player); //@todo: Find good way to return the enforced move

    boolean isCheck(Board board, Player player, List<Move> moves);
}