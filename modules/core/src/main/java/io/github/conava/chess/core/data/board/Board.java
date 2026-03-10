package io.github.conava.chess.core.data.board;

import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.data.pieces.King;
import io.github.conava.chess.core.data.pieces.Pawn;
import io.github.conava.chess.core.data.pieces.Rook;
import io.github.conava.chess.core.logic.moves.CastleMove;
import io.github.conava.chess.core.logic.moves.Move;
import io.github.conava.chess.core.logic.moves.PromotionMove;
import io.github.conava.chess.core.data.pieces.Piece;

import java.util.ArrayList;
import java.util.List;

/**
 * The Board class represents a chess board and provides methods to manipulate and query the board state.
 */
public class Board {
    private final Square[][] board;
    private final List<Square> piecesWhite = new ArrayList<>();
    private final List<Square> piecesBlack = new ArrayList<>();

    /**
     * Constructs a Board with the given squares.
     * @param board A 2D array of Square objects representing the board.
     */
    public Board(Square[][] board) {
        this.board = board;

        for (Square[] row : board) {
            for (Square square : row) {
                if (square.isOccupiedBy() != null) {
                    if (square.getPiece().getPlayer().color().equals(PlayerColor.WHITE)) {
                        piecesWhite.add(square);
                    } else {
                        piecesBlack.add(square);
                    }
                }
            }
        }
    }

    /**
     * Returns the amount of rows (x)
     *
     * @return the amount of rows
     */
    public int getRowCount() {
        return board[0].length;
    }

    /**
     * Returns the amount of columns (y)
     *
     * @return the amount of columns
     */
    public int getColCount() {
        return board.length;
    }

    /**
     * Returns the square at the specified coordinates.
     * @param y The y-coordinate of the square.
     * @param x The x-coordinate of the square.
     * @return The Square at the specified coordinates.
     */
    public Square getSquare(int y, int x) {
        return board[y][x];
    }

    /**
     * Executes a move on the board.
     * @param move The move to be executed.
     */
    public void executeMove(Move move) {
        Square startSquare = move.getStart();
        Square endSquare = move.getEnd();
        Piece piece = startSquare.getPiece();
        if (piece instanceof Rook rook) {
            rook.setHasMoved();
        } else if (piece instanceof King king) {
            king.setHasMoved();
        }

        if (move instanceof CastleMove) {
            handleCastleMove(startSquare, endSquare);
        } else if (move instanceof PromotionMove promotionMove) {
            piece = promotionMove.getTargetPiece();
        }

        // En passant: pawn moves diagonally to an empty square — capture the opponent pawn
        // that sits on the same rank as the start square, same file as the destination.
        // This check must happen before endSquare.setPiece() so the empty-destination
        // condition is evaluated against the board state prior to this move.
        boolean isEnPassant = piece instanceof Pawn
                && startSquare.getX() != endSquare.getX()
                && endSquare.getPiece() == null;

        endSquare.setPiece(piece);
        removePiece(startSquare);
        startSquare.setPiece(null);
        updatePieceLists(startSquare, endSquare, piece);

        if (isEnPassant) {
            Square capturedPawnSquare = getSquare(startSquare.getY(), endSquare.getX());
            removePiece(capturedPawnSquare);
            capturedPawnSquare.setPiece(null);
        }
    }

    /**
     * Returns a copy of the board.
     * @return A new Board object that is a copy of the current board.
     */
    public Board getCopy() {
        return new Board(board);
    }

    /**
     * Returns a list of squares occupied by the pieces of the specified player.
     * @param player The player whose pieces are to be returned.
     * @return A list of squares occupied by the player's pieces.
     */
    public List<Square> getPieces(Player player) {
        if (player == null) {
            return null;
        }
        return player.color().equals(PlayerColor.WHITE) ? piecesWhite : piecesBlack;
    }

    /**
     * Returns the piece at the specified square.
     * @param square The square whose piece is to be returned.
     * @return The piece at the specified square.
     */
    public Piece getPieceAt(Square square) {
        return board[square.getY()][square.getX()].getPiece();
    }

    /**
     * Handles the logic for a castle move by relocating the rook to its post-castling square.
     * <p>
     * Coordinate convention: {@code getSquare(y, x)} where {@code y} = row/rank and
     * {@code x} = column/file (0 = a-file, 7 = h-file). The king's rank is
     * {@code startSquare.getY()} and the castling direction is determined by the king's
     * destination file ({@code endSquare.getX()}):
     * <ul>
     *   <li>Queenside (castle long): king ends on the c-file ({@code x=2}). The rook moves
     *       from the a-file ({@code x=0}) to the d-file ({@code x=3}).</li>
     *   <li>Kingside (castle short): king ends on the g-file ({@code x=6}). The rook moves
     *       from the h-file ({@code x=7}) to the f-file ({@code x=5}).</li>
     * </ul>
     *
     * @param startSquare The king's starting square.
     * @param endSquare   The king's destination square.
     */
    private void handleCastleMove(Square startSquare, Square endSquare) {
        Piece rook;
        if (endSquare.getX() == 2) { // castle long (queenside): king ends on c-file
            rook = getSquare(startSquare.getY(), 0).getPiece(); // rook on a-file
            getSquare(startSquare.getY(), 3).setPiece(rook);    // rook moves to d-file
            getSquare(startSquare.getY(), 0).setPiece(null);    // clear a-file
        } else { // castle short (kingside): king ends on g-file
            rook = getSquare(startSquare.getY(), 7).getPiece(); // rook on h-file
            getSquare(startSquare.getY(), 5).setPiece(rook);    // rook moves to f-file
            getSquare(startSquare.getY(), 7).setPiece(null);    // clear h-file
        }
    }

    /**
     * Removes the piece from the specified square.
     * @param square The square from which the piece is to be removed.
     */
    private void removePiece(Square square) {
        if (square.getPiece() == null) {
            return;
        }
        Player player = square.getPiece().getPlayer();
        if (player.color().equals(PlayerColor.WHITE)) {
            piecesWhite.remove(square);
        } else {
            piecesBlack.remove(square);
        }
    }

    /**
     * Updates the piece lists when a piece is moved.
     * @param startSquare The starting square of the move.
     * @param endSquare The ending square of the move.
     * @param piece The piece being moved.
     */
    private void updatePieceLists(Square startSquare, Square endSquare, Piece piece) {
        if (piece == null) {
            return;
        }
        if (piece.getPlayer().color().equals(PlayerColor.WHITE)) {
            piecesWhite.remove(startSquare);
            piecesWhite.add(endSquare);
        } else {
            piecesBlack.remove(startSquare);
            piecesBlack.add(endSquare);
        }
    }
}