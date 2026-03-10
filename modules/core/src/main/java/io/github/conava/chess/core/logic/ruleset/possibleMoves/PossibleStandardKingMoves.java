package io.github.conava.chess.core.logic.ruleset.possibleMoves;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;

/**
 * Pseudo-legal move generator for the king in standard chess.
 *
 * <p>Normal one-step king moves are identical to Chess960. The castling logic
 * adds the fixed g-file (kingside) or c-file (queenside) destination square when
 * castling conditions are met.
 *
 * <p>Extends {@link AbstractKingMoveGenerator} which provides all shared logic:
 * adjacency moves, king/rook hasMoved checks, and the directional castling walk.
 */
public class PossibleStandardKingMoves extends AbstractKingMoveGenerator {

    /**
     * Initiates a new instance of a standard king move lookup table.
     *
     * @param square The square the king is on
     * @param board  The board surrounding the king
     */
    public PossibleStandardKingMoves(Square square, Board board) {
        super(square, board);
    }

    /**
     * In standard chess, finding an unmoved rook with a clear path is sufficient;
     * no extra corridor check is needed.
     *
     * @param rank     the rank of both king and rook
     * @param kingFile the king's file
     * @param rookFile the rook's file
     * @param kingside {@code true} for kingside, {@code false} for queenside
     * @return always {@code true}
     */
    @Override
    protected boolean onCastlingCandidateFound(int rank, int kingFile, int rookFile,
                                               boolean kingside) {
        return true;
    }

    /**
     * Returns the fixed castling destination square: g-file ({@code rowCount - 2}) for
     * kingside, c-file ({@code 2}) for queenside.
     *
     * @param direction {@code +1} for kingside, {@code -1} for queenside
     * @return the target square at the fixed destination file
     */
    @Override
    protected Square getCastlingTargetSquare(int direction) {
        int destFile = (direction > 0) ? (rowCount - 2) : 2;
        return board.getSquare(square.getY(), destFile);
    }
}
