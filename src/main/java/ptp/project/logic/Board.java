package ptp.project.logic;

import ptp.project.logic.moves.CastleMove;
import ptp.project.logic.moves.Move;
import ptp.project.logic.moves.PromotionMove;

import ptp.project.logic.pieces.*;
import ptp.project.logic.pieces.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Board {
    private final Square[][] board;
    private static final Logger LOGGER = Logger.getLogger(Board.class.getName());
    private final List<Square> piecesWhite = new ArrayList<Square>();
    private final List<Square> piecesBlack = new ArrayList<Square>();

    public Board(Square[][] board) {
        this.board = board;
        recountPieces();
    }

    public Square getSquare(int y, int x) {
        return board[y][x];
    }

    public void executeMove(Move move) {
        Square square1 = move.getStart();
        Square square2 = move.getEnd();
        Piece piece = square1.getPiece();
        if (piece instanceof Rook rook) {
            rook.setHasMoved();
        } else if (piece instanceof King king) {
            king.setHasMoved();
        }
        if (move instanceof CastleMove) {
            Piece rook;
            if (square2.getY() == 2) {//castle long
                rook = this.getSquare(0, square1.getX()).getPiece();
                this.getSquare(3, square1.getX()).setPiece(rook);
                this.getSquare(0, square1.getX()).setPiece(null);
            } else { //castle must be short
                rook = this.getSquare(7, square1.getX()).getPiece();
                this.getSquare(5, square1.getX()).setPiece(rook);
                this.getSquare(7, square1.getX()).setPiece(null);
            }
        } else if (move instanceof PromotionMove promotionMove) {
            piece = promotionMove.getTargetPiece();
        }
        square2.setPiece(piece);
        removePiece(square1);
        square1.setPiece(null);

    }

    public Board getCopy() {
        return new Board(board);
    }

    public void recountPieces() {
        for (int y = 0; y < board.length; y++) {
            for (int x = 0; x < board[y].length; x++) {
                if (board[y][x].isOccupiedBy() == null) {
                    continue;
                } else if (board[y][x].getPiece().getPlayer().getColor().equals("white")) {//white
                    piecesWhite.add(board[y][x]);
                } else {
                    piecesBlack.add(board[y][x]);
                }
            }
        }
    }

    private void removePiece (Square square) {
        Player player = square.getPiece().getPlayer();
        if (player.getColor().equals("white")) {
            piecesWhite.remove(square);
        } else {
            piecesBlack.remove(square);
        }
    }

    public List<Square> getPieces(Player player) {
        if (player == null) {
            return null;
        } else if (player.getColor().equals("white")) {
            return piecesWhite;
        } else {
            return piecesBlack;
        }
    }

    public boolean isCheck(Player player) {
        // Implement the logic to check if a player is in check
        return false;
    }

    public boolean isCheckmate(Player player) {
        // Implement the logic to check if a player is in checkmate
        return false;
    }

    public boolean isStalemate(Player player) {
        // Implement the logic to check if a player is in stalemate
        return false;
    }

    public boolean isDraw(Player player) {
        // Implement the logic to check if the game is a draw
        return false;
    }

    public boolean isGameOver(Player player) {
        // Implement the logic to check if the game is over
        return false;
    }

    public boolean isCapture(Move move) {
        // Implement the logic to check if a move is a capture
        return false;
    }
}