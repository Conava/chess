package ptp.project.logic.ruleset;

import ptp.project.data.Square;
import ptp.project.data.board.Board;
import ptp.project.data.Player;
import ptp.project.logic.moves.Move;
import ptp.project.data.pieces.Piece;

import java.util.List;

public interface Ruleset {

    int getWidth();

    int getHeight();

    Square[][] getStartBoard(Player player1, Player player2);

    List<Move> getLegalMoves(Square square, Board board, List<Move> moves, Player player1, Player player2);

    List<Square> getLegalSquares(Square square, Board board, List<Move> moves, Player player1, Player player2);

    boolean isValidSquare(Square square);

    boolean verifyMove(Move move);

    boolean verifyMove(Square newPosition, Piece piece);

    Move hasEnforcedMove(Player player); //@todo: Find good way to return the enforced move

    boolean isCheck(Board board, Player player, List<Move> moves);
}