package io.github.conava.chess.core.logic.ruleset;

import io.github.conava.chess.core.data.pieces.*;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.logic.ruleset.possibleMoves.*;
import io.github.conava.chess.core.logic.moves.CastleMove;
import io.github.conava.chess.core.logic.moves.Move;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Abstract base class for chess rulesets.
 *
 * <p>Contains all shared chess logic: move generation dispatch, check detection,
 * the legal-move filter (including castling through/out-of-check guards), and board
 * bounds helpers. Subclasses need only override {@link #getStartBoard} and, if they
 * use a different castling encoding, {@link #getPseudoLegalKingSquares}.
 *
 * <p>The {@link #getPseudoLegalSquares} dispatch is {@code protected} so subclasses
 * can reuse it when building their own override of {@link #getLegalSquares}.
 */
public abstract class AbstractChessRuleset implements Ruleset {

    /**
     * Gets the width of the chess board.
     *
     * @return int of the number of columns
     */
    @Override
    public int getWidth() {
        return 8;
    }

    /**
     * Gets the height of the chess board.
     *
     * @return int of the number of rows
     */
    @Override
    public int getHeight() {
        return 8;
    }

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
    @Override
    public List<Move> getLegalMoves(Square square, Board board, List<Move> moves, Player player1, Player player2) {
        List<Square> legalSquares = getLegalSquares(square, board, moves, player1, player2);
        return legalSquares.stream().map(target -> new Move(square, target)).collect(Collectors.toList());
    }

    /**
     * Provides a list of legal squares, filtered to exclude any move that would leave
     * the moving player's king (derived from the piece on {@code square}) in check.
     *
     * <p>For each pseudo-legal target square, a deep copy of the board is created, the move is
     * simulated on the copy, and {@link #isCheck} is called on the resulting position. Only
     * target squares where the moving player's king is not in check after the move are returned.
     *
     * <p>Special castling handling:
     * <ul>
     *   <li>Castling is illegal when the king is currently in check.</li>
     *   <li>Castling is illegal when the king would pass through an attacked transit square.</li>
     * </ul>
     *
     * @param square  Only moves from this square are shown
     * @param board   Current board
     * @param moves   List of moves already played in-game
     * @param player1 Player to move (used for pseudo-legal generation; the check filter derives
     *                the moving player from {@code square.getPiece().getPlayer()})
     * @param player2 Player opponent
     * @return List of LEGAL squares — moves that do not leave the moving player's king
     * (derived from the piece on {@code square}) in check.
     */
    @Override
    public List<Square> getLegalSquares(Square square, Board board, List<Move> moves, Player player1, Player player2) {
        if (square.getPiece() == null) return Collections.emptyList();

        List<Square> pseudoLegal = getPseudoLegalSquares(square, board, moves);
        List<Square> legal = new ArrayList<>();

        // Derive the moving player from the piece on the source square so that
        // the check filter works correctly for both colours regardless of which
        // Player references are passed in as player1/player2.
        Player movingPlayer = square.getPiece().getPlayer();

        boolean currentlyInCheck = isCheck(board, movingPlayer, moves);

        for (Square targetSquare : pseudoLegal) {
            // Castling candidate: king moves exactly 2 squares horizontally
            if (isCastlingCandidate(square, targetSquare)) {
                // Cannot castle while in check
                if (currentlyInCheck) {
                    continue;
                }
                // Cannot castle through an attacked transit square
                int transitX = square.getX() + Integer.signum(targetSquare.getX() - square.getX());
                Board transitBoardCopy = board.getCopy();
                Square transitStart = transitBoardCopy.getSquare(square.getY(), square.getX());
                Square transitEnd = transitBoardCopy.getSquare(square.getY(), transitX);
                transitBoardCopy.executeMove(new Move(transitStart, transitEnd));
                if (isCheck(transitBoardCopy, movingPlayer, moves)) {
                    continue;
                }
            }

            // Check final square: simulate the move and verify the king is not in check
            Board finalBoardCopy = board.getCopy();
            Square finalStart = finalBoardCopy.getSquare(square.getY(), square.getX());
            Square finalEnd = finalBoardCopy.getSquare(targetSquare.getY(), targetSquare.getX());
            finalBoardCopy.executeMove(isCastlingCandidate(square, targetSquare) ? new CastleMove(finalStart, finalEnd) : new Move(finalStart, finalEnd));
            if (!isCheck(finalBoardCopy, movingPlayer, moves)) {
                legal.add(targetSquare);
            }
        }

        return legal;
    }

    /**
     * Returns {@code true} if moving a piece from {@code from} to {@code to} does not leave
     * the moving player's king in check.
     *
     * <p>Deep-copies the board, simulates a plain {@link Move} from {@code from} to {@code to},
     * and calls {@link #isCheck} on the resulting position. This helper is used by both the
     * standard and Chess960 legal-move filters for non-castling moves.
     *
     * @param board  the current board
     * @param from   the source square
     * @param to     the target square
     * @param player the player whose king must not be in check after the move
     * @param moves  move history (forwarded to {@link #isCheck} for en passant detection)
     * @return {@code true} if the position after the move is not check for {@code player}
     */
    protected boolean isLegalAfterSimulation(Board board, Square from, Square to, Player player, List<Move> moves) {
        Board copy = board.getCopy();
        Square copyFrom = copy.getSquare(from.getY(), from.getX());
        Square copyTo = copy.getSquare(to.getY(), to.getX());
        copy.executeMove(new Move(copyFrom, copyTo));
        return !isCheck(copy, player, moves);
    }

    /**
     * Dispatches pseudo-legal square generation to the appropriate per-piece move generator.
     *
     * <p>The King case delegates to {@link #getPseudoLegalKingSquares}, which subclasses
     * may override to change the castling encoding (e.g. Chess960).
     *
     * @param square The source square (must have a non-null piece)
     * @param board  The current board
     * @param moves  Move history (used by pawn for en passant, by king for castling)
     * @return list of pseudo-legal target squares
     */
    protected List<Square> getPseudoLegalSquares(Square square, Board board, List<Move> moves) {
        if (square.getPiece().getClass().equals(Rook.class)) {
            PossibleStandardRookMoves rookMoves = new PossibleStandardRookMoves(square, board);
            return rookMoves.getPossibleSquares();
        } else if (square.getPiece().getClass().equals(Knight.class)) {
            PossibleStandardKnightMoves knightMoves = new PossibleStandardKnightMoves(square, board);
            return knightMoves.getPossibleSquares();
        } else if (square.getPiece().getClass().equals(Bishop.class)) {
            PossibleStandardBishopMoves bishopMoves = new PossibleStandardBishopMoves(square, board);
            return bishopMoves.getPossibleSquares();
        } else if (square.getPiece().getClass().equals(Queen.class)) {
            PossibleStandardQueenMoves queenMoves = new PossibleStandardQueenMoves(square, board);
            return queenMoves.getPossibleSquares();
        } else if (square.getPiece().getClass().equals(King.class)) {
            return getPseudoLegalKingSquares(square, board, moves);
        } else if (square.getPiece().getClass().equals(Pawn.class)) {
            PossibleStandardPawnMoves pawnMoves = new PossibleStandardPawnMoves(square, board, moves);
            return pawnMoves.possibleMoves();
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Returns pseudo-legal squares for a king, including castling candidates.
     *
     * <p>The default implementation uses {@link PossibleStandardKingMoves}, which encodes
     * castling as "king moves exactly 2 squares horizontally". Subclasses (e.g. Chess960)
     * may override this to use a different castling encoding.
     *
     * @param square The king's current square
     * @param board  The current board
     * @param moves  Move history (used to detect castling rights)
     * @return list of pseudo-legal target squares for the king
     */
    protected List<Square> getPseudoLegalKingSquares(Square square, Board board, List<Move> moves) {
        PossibleStandardKingMoves kingMoves = new PossibleStandardKingMoves(square, board);
        return kingMoves.getPossibleSquares();
    }

    /**
     * Returns true if the move from {@code source} to {@code target} is a castling candidate.
     *
     * <p>The default implementation identifies castling as a king moving exactly 2 squares
     * horizontally. Subclasses (e.g. Chess960) may override this to recognise a different
     * castling encoding, such as "king moves to rook file".
     *
     * @param source the source square (must contain a non-null piece)
     * @param target the target square
     * @return true if this move is a castling candidate
     */
    protected boolean isCastlingCandidate(Square source, Square target) {
        return source.getPiece() instanceof King && Math.abs(target.getX() - source.getX()) == 2;
    }

    /**
     * Delegates the public {@link Ruleset#isCastlingMove} contract to the protected
     * {@link #isCastlingCandidate} template method, so subclasses need only override
     * {@code isCastlingCandidate} to change castling detection everywhere.
     *
     * @param source the source square
     * @param target the target square
     * @return {@code true} if this is a castling candidate according to this ruleset
     */
    @Override
    public boolean isCastlingMove(Square source, Square target) {
        return isCastlingCandidate(source, target);
    }

    /**
     * Returns a valid square.
     *
     * @param square Square to check
     * @return If the square exists and is in bounds
     */
    @Override
    public boolean isValidSquare(Square square) {
        return square != null && isInBoundsY(square.getY()) && isInBoundsX(square.getX());
    }

    /**
     * Checks whether the given player's king is in check on the given board.
     *
     * @param board  Board to check on
     * @param player Player whose king we are checking
     * @param moves  Move history (used for en passant detection in reverse-attack scan)
     * @return true if the player's king is in check
     */
    @Override
    public boolean isCheck(Board board, Player player, List<Move> moves) {
        return canBeCaptured(board, lookForKing(board, player), moves);
    }

    private Square lookForKing(Board board, Player player) {
        List<Square> potentialSquares = board.getPieces(player);
        for (Square square : potentialSquares) {
            if (square.getPiece() instanceof King) {
                return square;
            }
        }
        return null;
    }

    private boolean canBeCaptured(Board board, Square square, List<Move> moves) {
        List<Square> squaresToCheck;

        //Knight can capture
        PossibleStandardKnightMoves knightMoves = new PossibleStandardKnightMoves(square, board);
        squaresToCheck = knightMoves.getPossibleSquares();
        for (Square squareToCheck : squaresToCheck) {
            if (squareToCheck.getPiece() instanceof Knight) {
                return true;
            }
        }

        //Rook can capture
        PossibleStandardRookMoves rookMoves = new PossibleStandardRookMoves(square, board);
        squaresToCheck = rookMoves.getPossibleSquares();
        for (Square squareToCheck : squaresToCheck) {
            if (squareToCheck.getPiece() instanceof Rook || squareToCheck.getPiece() instanceof Queen) {
                return true;
            }
        }

        //Bishop can capture
        PossibleStandardBishopMoves bishopMoves = new PossibleStandardBishopMoves(square, board);
        squaresToCheck = bishopMoves.getPossibleSquares();
        for (Square squareToCheck : squaresToCheck) {
            if (squareToCheck.getPiece() instanceof Bishop || squareToCheck.getPiece() instanceof Queen) {
                return true;
            }
        }

        //Pawn can capture
        PossibleStandardPawnMoves pawnMoves = new PossibleStandardPawnMoves(square, board, moves);
        squaresToCheck = pawnMoves.possibleCaptureMoves();
        for (Square squareToCheck : squaresToCheck) {
            if (squareToCheck.getPiece() instanceof Pawn) {
                return true;
            }
        }

        // Check king adjacency: opposing king cannot stand adjacent
        int ky = square.getY(), kx = square.getX();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dy == 0 && dx == 0) continue;
                if (isInBoundsY(ky + dy) && isInBoundsX(kx + dx)) {
                    Piece p = board.getSquare(ky + dy, kx + dx).getPiece();
                    if (p instanceof King && !p.getPlayer().equals(square.getPiece().getPlayer())) return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns true if the given x coordinate is within the board's horizontal bounds.
     *
     * @param x file index (0-based)
     * @return true if in bounds
     */
    protected boolean isInBoundsX(int x) {
        return x >= 0 && x < getWidth();
    }

    /**
     * Returns true if the given y coordinate is within the board's vertical bounds.
     *
     * @param y rank index (0-based, 0 = white's back rank)
     * @return true if in bounds
     */
    protected boolean isInBoundsY(int y) {
        return y >= 0 && y < getHeight();
    }
}
