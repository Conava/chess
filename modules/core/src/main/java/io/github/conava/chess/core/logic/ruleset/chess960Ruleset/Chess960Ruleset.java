package io.github.conava.chess.core.logic.ruleset.chess960Ruleset;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.pieces.King;
import io.github.conava.chess.core.data.pieces.Piece;
import io.github.conava.chess.core.data.pieces.Rook;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.logic.moves.CastleMove;
import io.github.conava.chess.core.logic.moves.Move;
import io.github.conava.chess.core.logic.ruleset.AbstractChessRuleset;
import io.github.conava.chess.core.logic.ruleset.possibleMoves.PossibleChess960KingMoves;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Chess960 (Fischer Random Chess) ruleset.
 *
 * <p>This ruleset differs from {@code StandardChessRuleset} in exactly two ways:
 * <ol>
 *   <li>The back-rank starting position is randomised (960 valid positions exist), determined
 *       by a Scharnagl index in the range [0, 959]. Index 518 is the standard chess position.</li>
 *   <li>Castling uses "king moves to rook file" encoding: the king's pseudo-legal target is the
 *       rook's actual square, not the standard g/c-file offsets.</li>
 * </ol>
 *
 * <p>Everything else — pawn rules, en passant, promotion, check/checkmate/stalemate,
 * 50-move rule, threefold repetition, and insufficient material — is inherited from
 * {@link AbstractChessRuleset} unchanged.
 *
 * <p>Two constructors are provided:
 * <ul>
 *   <li>{@link #Chess960Ruleset()} — generates a random position; use for offline play.</li>
 *   <li>{@link #Chess960Ruleset(int)} — deterministic reconstruction from a Scharnagl index;
 *       use for online play where the server provides the index.</li>
 * </ul>
 */
public class Chess960Ruleset extends AbstractChessRuleset {

    /** The Scharnagl index (0–959) identifying this Chess960 starting position. */
    private final int scharnaglIndex;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Constructs a {@code Chess960Ruleset} with a randomly generated starting position.
     *
     * <p>A temporary {@link Player} pair is used to generate the back rank so the index
     * can be computed via {@link Chess960StartPosition#computeIndex(Piece[])}. The actual
     * board is rebuilt per {@link #getStartBoard(Player, Player)} call from the stored index,
     * ensuring each call receives pieces owned by the correct players.
     */
    public Chess960Ruleset() {
        // Generate with dummy players just to compute the back rank and derive the index.
        Player dummy1 = new Player("_gen1", PlayerColor.WHITE);
        Player dummy2 = new Player("_gen2", PlayerColor.BLACK);
        Square[][] generatedBoard = Chess960StartPosition.generate(dummy1, dummy2);
        Piece[] backRank = new Piece[8];
        for (int x = 0; x < 8; x++) {
            backRank[x] = generatedBoard[0][x].getPiece();
        }
        this.scharnaglIndex = Chess960StartPosition.computeIndex(backRank);
    }

    /**
     * Constructs a {@code Chess960Ruleset} for the given Scharnagl index.
     *
     * <p>This constructor is used for online play: the server generates a position, stores
     * the index, and sends it to both clients who then reconstruct the same position
     * deterministically.
     *
     * @param scharnaglIndex Scharnagl index in [0, 959]
     * @throws IllegalArgumentException if {@code scharnaglIndex} is outside [0, 959]
     */
    public Chess960Ruleset(int scharnaglIndex) {
        if (scharnaglIndex < 0 || scharnaglIndex > 959) {
            throw new IllegalArgumentException(
                    "Scharnagl index must be in [0, 959], got: " + scharnaglIndex);
        }
        this.scharnaglIndex = scharnaglIndex;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the Scharnagl index identifying this Chess960 starting position.
     *
     * @return an integer in [0, 959]
     */
    public int getIndex() {
        return scharnaglIndex;
    }

    // -------------------------------------------------------------------------
    // Ruleset overrides
    // -------------------------------------------------------------------------

    /**
     * Returns the Chess960 starting board for the given Scharnagl index.
     *
     * <p>Delegates to {@link Chess960StartPosition#fromIndex(int, Player, Player)} so that
     * pieces are created with the correct player ownership.
     *
     * @param player1 Player with the white pieces
     * @param player2 Player with the black pieces
     * @return the 8×8 starting {@link Square} array
     */
    @Override
    public Square[][] getStartBoard(Player player1, Player player2) {
        return Chess960StartPosition.fromIndex(scharnaglIndex, player1, player2);
    }

    /**
     * Returns the game label for display in the UI.
     *
     * @return a string of the form {@code "Chess 960 — Position N"} where N is the
     *         Scharnagl index
     */
    @Override
    public String getGameLabel() {
        return "Chess 960 \u2014 Position " + scharnaglIndex;
    }

    /**
     * Returns pseudo-legal squares for the king using Chess960 encoding.
     *
     * <p>Delegates to {@link PossibleChess960KingMoves}, which adds the rook's square (not
     * the fixed g/c-file offset) as the castling candidate. This "king moves to rook"
     * encoding is the FIDE-sanctioned Chess960 disambiguation convention.
     *
     * @param square the king's current square
     * @param board  the current board
     * @param moves  move history (used by {@link PossibleChess960KingMoves} to check castling rights)
     * @return pseudo-legal target squares for the king
     */
    @Override
    protected List<Square> getPseudoLegalKingSquares(Square square, Board board, List<Move> moves) {
        PossibleChess960KingMoves kingMoves = new PossibleChess960KingMoves(square, board);
        return kingMoves.getPossibleSquares();
    }

    /**
     * Constructs a Chess960-aware {@link CastleMove} with explicit rook-origin and king-dest
     * file values so that {@code Board.handleCastleMove960} places pieces on the correct
     * squares.
     *
     * @param source the king's current square
     * @param target the rook's current square (the king moves to the rook's file in Chess960)
     * @return a {@link CastleMove} with {@code rookOriginFile = target.getX()} and
     *         {@code kingDestFile} set to 2 (queenside) or 6 (kingside)
     */
    @Override
    public CastleMove buildCastleMove(Square source, Square target) {
        int rookOriginFile = target.getX();
        boolean kingside = target.getX() > source.getX();
        int kingDestFile = kingside ? 6 : 2;
        return new CastleMove(source, target, rookOriginFile, kingDestFile);
    }

    /**
     * Returns {@code true} if the move from {@code source} to {@code target} is a Chess960
     * castling candidate.
     *
     * <p>In Chess960, castling is encoded as "king moves to rook's current square". This
     * replaces the standard chess detection of "king moves exactly 2 squares horizontally".
     *
     * @param source the source square (must contain a non-null piece)
     * @param target the target square
     * @return {@code true} when the moving piece is a King and the target square contains
     *         an unmoved friendly Rook
     */
    @Override
    protected boolean isCastlingCandidate(Square source, Square target) {
        if (!(source.getPiece() instanceof King)) return false;
        Piece targetPiece = target.getPiece();
        return targetPiece instanceof Rook
                && ((Rook) targetPiece).getHasNotMoved()
                && targetPiece.getPlayer().equals(source.getPiece().getPlayer());
    }

    /**
     * Returns legal squares for the piece on {@code square}, with Chess960-specific
     * castling transit-square checks.
     *
     * <p>Overrides the parent implementation to correctly handle the Chess960 castling
     * encoding:
     * <ul>
     *   <li>A castling candidate is detected by {@link #isCastlingCandidate} (king moves to
     *       rook's file), not by a 2-square delta.</li>
     *   <li>The transit-square check walks all squares between the king's current file and
     *       its actual destination file (c or g), not just one step.</li>
     *   <li>The final-position simulation uses a {@link CastleMove} with explicit
     *       {@code rookOriginFile} and {@code kingDestFile} fields so that
     *       {@code Board.handleCastleMove} places pieces on the correct squares.</li>
     * </ul>
     *
     * @param square  source square (must contain a piece)
     * @param board   current board
     * @param moves   move history
     * @param player1 player to move
     * @param player2 opponent
     * @return list of legal target squares
     */
    @Override
    public List<Square> getLegalSquares(Square square, Board board, List<Move> moves,
                                        Player player1, Player player2) {
        if (square.getPiece() == null) return Collections.emptyList();

        List<Square> pseudoLegal = getPseudoLegalSquares(square, board, moves);
        List<Square> legal = new ArrayList<>();

        Player movingPlayer = square.getPiece().getPlayer();
        boolean currentlyInCheck = isCheck(board, movingPlayer, moves);

        for (Square targetSquare : pseudoLegal) {
            if (isCastlingCandidate(square, targetSquare)) {
                // Cannot castle while in check
                if (currentlyInCheck) {
                    continue;
                }

                // Determine castling direction and king's actual destination file
                int kingFile = square.getX();
                int rookFile = targetSquare.getX();
                int kingDestFile = rookFile > kingFile ? 6 : 2;

                // Check every square the king passes through from its current file
                // to its actual destination file (inclusive except for start square)
                if (isKingTransitAttacked(board, moves, movingPlayer, square, kingFile, kingDestFile)) {
                    continue;
                }

                // Simulate the final position: king on kingDestFile, rook relocated
                Board finalBoardCopy = board.getCopy();
                Square finalStart = finalBoardCopy.getSquare(square.getY(), square.getX());
                Square finalEnd   = finalBoardCopy.getSquare(targetSquare.getY(), targetSquare.getX());
                finalBoardCopy.executeMove(
                        new CastleMove(finalStart, finalEnd, rookFile, kingDestFile));
                if (!isCheck(finalBoardCopy, movingPlayer, moves)) {
                    legal.add(targetSquare);
                }
            } else {
                // Non-castling move: standard deep-copy check filter
                Board finalBoardCopy = board.getCopy();
                Square finalStart = finalBoardCopy.getSquare(square.getY(), square.getX());
                Square finalEnd   = finalBoardCopy.getSquare(targetSquare.getY(), targetSquare.getX());
                finalBoardCopy.executeMove(new Move(finalStart, finalEnd));
                if (!isCheck(finalBoardCopy, movingPlayer, moves)) {
                    legal.add(targetSquare);
                }
            }
        }

        return legal;
    }

    /**
     * Reconstructs a {@link Move} from its wire-protocol string, with Chess960-aware
     * castling handling.
     *
     * <p>For {@code "O-O"} (kingside) and {@code "O-O-O"} (queenside), this method scans
     * the board for the king's actual file and the rook's actual file, then constructs a
     * {@link CastleMove} with the correct {@code rookOriginFile} and {@code kingDestFile}
     * fields. For all other wire strings, delegates to {@link Move#fromString(String, Player)}.
     *
     * <p>This keeps all Chess960-specific move reconstruction inside the ruleset — no
     * switch statements or instanceof checks are needed in {@code OnlineGame} or
     * {@code GameInstance}.
     *
     * @param wireString the protocol string as produced by {@link Move#toProtocolString()}
     * @param board      the current board state (used to locate king and rooks)
     * @param player     the player making the move
     * @return the reconstructed {@link Move}
     */
    @Override
    public Move deserializeMove(String wireString, Board board, Player player) {
        if ("O-O".equals(wireString) || "O-O-O".equals(wireString)) {
            boolean kingside = "O-O".equals(wireString);
            int rank = player.color() == PlayerColor.WHITE ? 0 : 7;

            // Find the king's actual file on this rank
            int kingFile = findKingFile(board, rank, player);
            if (kingFile < 0) {
                throw new IllegalStateException(
                    "Chess960 castling deserialization failed: king or rook not found on rank "
                        + rank + " for move " + wireString);
            }

            // Find the rook's file (kingside: look right; queenside: look left)
            int rookFile = findRookFile(board, rank, player, kingFile, kingside);
            if (rookFile < 0) {
                throw new IllegalStateException(
                    "Chess960 castling deserialization failed: king or rook not found on rank "
                        + rank + " for move " + wireString);
            }

            int kingDestFile = kingside ? 6 : 2;
            Square kingSquare = new Square(rank, kingFile);
            Square rookSquare = new Square(rank, rookFile);
            return new CastleMove(kingSquare, rookSquare, rookFile, kingDestFile);
        }

        // All other wire strings: delegate to the default implementation
        return Move.fromString(wireString, player);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Checks whether any transit square the king passes through (from its current file
     * to its actual destination file, exclusive of the king's start) is attacked.
     *
     * @param board       the current board
     * @param moves       move history
     * @param movingPlayer the player whose king is castling
     * @param kingSquare  the king's current square
     * @param kingFile    the king's current file
     * @param kingDestFile the king's actual destination file (2 or 6)
     * @return {@code true} if any transit square is attacked (castling illegal)
     */
    private boolean isKingTransitAttacked(Board board, List<Move> moves, Player movingPlayer,
                                          Square kingSquare, int kingFile, int kingDestFile) {
        int direction = Integer.signum(kingDestFile - kingFile);
        int currentFile = kingFile + direction;
        while (currentFile != kingDestFile) {
            // Simulate the king stepping to this transit square
            Board transitCopy = board.getCopy();
            Square transitStart = transitCopy.getSquare(kingSquare.getY(), kingFile);
            Square transitEnd   = transitCopy.getSquare(kingSquare.getY(), currentFile);
            transitCopy.executeMove(new Move(transitStart, transitEnd));
            if (isCheck(transitCopy, movingPlayer, moves)) {
                return true;
            }
            currentFile += direction;
        }
        return false;
    }

    /**
     * Scans the given rank for the king belonging to {@code player}.
     *
     * @param board  the current board
     * @param rank   the rank (y-coordinate) to scan
     * @param player the player whose king to find
     * @return the king's file (x-coordinate), or {@code -1} if not found
     */
    private int findKingFile(Board board, int rank, Player player) {
        for (int x = 0; x < 8; x++) {
            Piece p = board.getSquare(rank, x).getPiece();
            if (p instanceof King && p.getPlayer().equals(player)) {
                return x;
            }
        }
        return -1;
    }

    /**
     * Finds the file of the first unmoved friendly Rook on the given rank in the specified
     * direction from the king.
     *
     * @param board    the current board
     * @param rank     the rank (y-coordinate) to scan
     * @param player   the player whose rook to find
     * @param kingFile the king's current file (search starts here + direction)
     * @param kingside {@code true} to search right (kingside), {@code false} to search left
     * @return the rook's file, or {@code -1} if not found
     */
    private int findRookFile(Board board, int rank, Player player, int kingFile, boolean kingside) {
        int direction = kingside ? 1 : -1;
        int x = kingFile + direction;
        while (x >= 0 && x < 8) {
            Piece p = board.getSquare(rank, x).getPiece();
            if (p instanceof Rook rook && rook.getHasNotMoved() && p.getPlayer().equals(player)) {
                return x;
            }
            x += direction;
        }
        return -1;
    }
}
