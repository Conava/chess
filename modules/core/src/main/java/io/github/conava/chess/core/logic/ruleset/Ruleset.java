package io.github.conava.chess.core.logic.ruleset;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.logic.moves.Move;

import java.util.List;

public interface Ruleset {

    /**
     * Gets the width
     *
     * @return int of the number of the columns
     */
    int getWidth();

    /**
     * Gets the height
     *
     * @return int of the amount of rows
     */
    int getHeight();

    /**
     * Returns the start position
     *
     * @param player1 Player with the white pieces
     * @param player2 Player with the black pieces
     * @return Double array of the board.
     */
    Square[][] getStartBoard(Player player1, Player player2);

    /**
     * Provides a list of legal moves.
     *
     * @param square  Only moves from this square are shown
     * @param board   Current board
     * @param moves   List of moves already played in-game
     * @param player1 Player to move
     * @param player2 Player opponent
     * @return List of LEGAL moves.
     */
    List<Move> getLegalMoves(Square square, Board board, List<Move> moves, Player player1, Player player2);

    /**
     * Provides a list of legal squares.
     *
     * @param square  Only moves from this square are shown
     * @param board   Current board
     * @param moves   List of moves already played in-game
     * @param player1 Player to move
     * @param player2 Player opponent
     * @return List of LEGAL squares.
     */
    List<Square> getLegalSquares(Square square, Board board, List<Move> moves, Player player1, Player player2);

    /**
     * Returns a valid square
     *
     * @param square Square to check
     * @return If the square exists and is in bounds
     */
    boolean isValidSquare(Square square);

    /**
     * @param board  Board, where the check should be checked
     * @param player Player who can move
     * @return ?isCheck
     */
    boolean isCheck(Board board, Player player, List<Move> moves);

    /**
     * Returns a short label describing the current game variant, for display in the UI.
     * <p>
     * Returns an empty string by default, which causes the UI to show no label.
     * Ruleset implementations that want to surface a label (e.g. Chess 960) should override
     * this method.
     *
     * @return display label for the active game variant, or {@code ""} if none
     */
    default String getGameLabel() {
        return "";
    }

    /**
     * Reconstructs a {@link Move} from its wire-protocol string representation.
     * <p>
     * The default implementation delegates to {@link Move#fromString(String, Player)},
     * which handles standard algebraic protocol strings ({@code "e2-e4"}, {@code "O-O"},
     * {@code "O-O-O"}, {@code "a7-a8=QUEEN"}). The {@code board} parameter is provided
     * for ruleset overrides that need board context — for example, Chess 960 castling,
     * where the rook's actual file must be read from the current position.
     *
     * @param wireString the protocol string as produced by {@link Move#toProtocolString()}
     * @param board      the current board state (may be ignored by the default implementation)
     * @param player     the player making the move
     * @return the reconstructed {@link Move}
     */
    default Move deserializeMove(String wireString, Board board, Player player) {
        return Move.fromString(wireString, player);
    }
}