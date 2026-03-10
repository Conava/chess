package io.github.conava.chess.core.logic.moves;

import io.github.conava.chess.core.data.Square;

/**
 * Represents a castling move in a chess game.
 * Castling is a special move involving the king and a rook.
 *
 * <p>For standard chess, use {@link #CastleMove(Square, Square)}. The fields
 * {@code rookOriginFile} and {@code kingDestFile} will be {@code -1}, which tells
 * {@code Board.handleCastleMove} to use the hardcoded standard-chess logic.
 *
 * <p>For Chess960, use {@link #CastleMove(Square, Square, int, int)} and supply the
 * rook's actual file and the king's destination file. When {@code rookOriginFile >= 0},
 * {@code Board.handleCastleMove} uses these explicit values instead of the hardcoded
 * standard positions.
 */
public class CastleMove extends Move {

    /** File index of the rook involved in this castle, or {@code -1} for standard chess. */
    private final int rookOriginFile;

    /** File index where the king lands after castling, or {@code -1} for standard chess. */
    private final int kingDestFile;

    /**
     * Constructs a standard {@code CastleMove}.
     * Both {@code rookOriginFile} and {@code kingDestFile} default to {@code -1},
     * preserving the existing standard-chess behaviour in {@code Board.handleCastleMove}.
     *
     * @param start The starting square of the king.
     * @param end   The ending square of the king (standard: c-file or g-file).
     */
    public CastleMove(Square start, Square end) {
        super(start, end);
        this.rookOriginFile = -1;
        this.kingDestFile   = -1;
    }

    /**
     * Constructs a Chess960-aware {@code CastleMove} with explicit rook origin and king
     * destination files.
     *
     * <p>When {@code rookOriginFile} is {@code -1}, the standard-chess path in
     * {@code Board.handleCastleMove} is used (identical to calling the two-argument constructor).
     *
     * @param start          The starting square of the king.
     * @param end            The square the king moves to (Chess960: the rook's current square).
     * @param rookOriginFile The file index of the rook being castled with, or {@code -1}.
     * @param kingDestFile   The file index where the king will land after castling, or {@code -1}.
     */
    public CastleMove(Square start, Square end, int rookOriginFile, int kingDestFile) {
        super(start, end);
        this.rookOriginFile = rookOriginFile;
        this.kingDestFile   = kingDestFile;
    }

    /**
     * Returns the file index of the rook involved in this castle.
     *
     * @return the rook's file (0–7), or {@code -1} if this is a standard castle
     */
    public int getRookOriginFile() {
        return rookOriginFile;
    }

    /**
     * Returns the file index where the king lands after castling.
     *
     * @return the king's destination file (0–7), or {@code -1} if this is a standard castle
     */
    public int getKingDestFile() {
        return kingDestFile;
    }
}
