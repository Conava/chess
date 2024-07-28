package ptp.core.data.board;

import ptp.core.data.Player;
import ptp.core.data.Square;
import ptp.core.data.enums.PlayerColor;
import ptp.core.data.pieces.King;
import ptp.core.data.pieces.Rook;
import ptp.core.logic.moves.CastleMove;
import ptp.core.logic.moves.Move;
import ptp.core.logic.moves.PromotionMove;
import ptp.core.data.pieces.Piece;

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

        endSquare.setPiece(piece);
        removePiece(startSquare);
        startSquare.setPiece(null);
        updatePieceLists(startSquare, endSquare, piece);
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
     * Handles the logic for a castle move.
     * @param startSquare The starting square of the move.
     * @param endSquare The ending square of the move.
     */
    private void handleCastleMove(Square startSquare, Square endSquare) {
        Piece rook;
        if (endSquare.getY() == 2) { // castle long
            rook = getSquare(0, startSquare.getX()).getPiece();
            getSquare(3, startSquare.getX()).setPiece(rook);
            getSquare(0, startSquare.getX()).setPiece(null);
        } else { // castle short
            rook = getSquare(7, startSquare.getX()).getPiece();
            getSquare(5, startSquare.getX()).setPiece(rook);
            getSquare(7, startSquare.getX()).setPiece(null);
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