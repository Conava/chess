package ptp.project.logic;

import ptp.project.Chess;
import ptp.project.logic.moves.CastleMove;
import ptp.project.logic.moves.Move;
import ptp.project.logic.moves.PromotionMove;
import ptp.project.logic.pieces.King;
import ptp.project.logic.pieces.Piece;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Board {
    private Square[][] board;
    private static final Logger LOGGER = Logger.getLogger(Board.class.getName());


    public Board(Square[][] board) {
        this.board = board;
    }

    public Square getSquare(int y, int x) {
        return board[y][x];
    }

    public void executeMove(Move move) {
        Square square1 = move.getStart();
        Square square2 = move.getEnd();
        Piece piece = square1.getPiece();
        if (move instanceof CastleMove) {
            Piece rook;
            if (square2.getY() == 2) {//castle long
                rook = this.getSquare(0, square1.getX()).getPiece();
                this.getSquare(3, square1.getX()).setPiece(rook);
                this.getSquare(0, square1.getX()).setPiece(null);
            }
            else { //castle must be short
                rook = this.getSquare(7, square1.getX()).getPiece();
                this.getSquare(5, square1.getX()).setPiece(rook);
                this.getSquare(7, square1.getX()).setPiece(null);
            }
        } else if (move instanceof PromotionMove promotionMove) {
            piece = promotionMove.getTargetPiece();
        }
        square2.setPiece(piece);
        square1.setPiece(null);
    }

    public Piece getPieceAt(Square square) {
        return square.getPiece();
    }

    public List<Square> getLegalMoves(Piece piece) {
        // Implement the logic to get the legal moves for a piece
        return null;
    }

    public void move(Move move) {
        // Implement the logic to move a piece
    }

    public List<Piece> getPieces(Player player) {
        // Implement the logic to get all pieces for a player
        return null;
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

    public boolean isLegalMove(Move move) {
        // Implement the logic to check if a move is legal
        return false;
    }

    public boolean isEnPassant(Move move) {
        // Implement the logic to check if a move is en passant
        return false;
    }

    public boolean isPromotion(Move move) {
        // Implement the logic to check if a move is a promotion
        return false;
    }

    public boolean isCastling(Move move) {
        // Implement the logic to check if a move is castling
        return false;
    }

    public boolean isCapture(Move move) {
        // Implement the logic to check if a move is a capture
        return false;
    }
}