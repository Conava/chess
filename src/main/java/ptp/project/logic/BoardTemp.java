package ptp.project.logic;

import ptp.project.logic.movesTemp.CastleMoveTemp;
import ptp.project.logic.movesTemp.MoveTemp;
import ptp.project.logic.movesTemp.PromotionMoveTemp;

import ptp.project.logic.piecesTemp.*;
import ptp.project.logic.piecesTemp.PieceTemp;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class BoardTemp {
    private final SquareTemp[][] board;
    private static final Logger LOGGER = Logger.getLogger(BoardTemp.class.getName());
    private final List<SquareTemp> piecesWhite = new ArrayList<SquareTemp>();
    private final List<SquareTemp> piecesBlack = new ArrayList<SquareTemp>();

    public BoardTemp(SquareTemp[][] board) {
        this.board = board;
        recountPieces();
    }

    public SquareTemp getSquare(int y, int x) {
        return board[y][x];
    }

    public void executeMove(MoveTemp moveTemp) {
        SquareTemp squareTemp1 = moveTemp.getStart();
        SquareTemp squareTemp2 = moveTemp.getEnd();
        PieceTemp pieceTemp = squareTemp1.getPiece();
        if (pieceTemp instanceof Rook rook) {
            rook.setHasMoved();
        } else if (pieceTemp instanceof King king) {
            king.setHasMoved();
        }
        if (moveTemp instanceof CastleMoveTemp) {
            PieceTemp rook;
            if (squareTemp2.getY() == 2) {//castle long
                rook = this.getSquare(0, squareTemp1.getX()).getPiece();
                this.getSquare(3, squareTemp1.getX()).setPiece(rook);
                this.getSquare(0, squareTemp1.getX()).setPiece(null);
            } else { //castle must be short
                rook = this.getSquare(7, squareTemp1.getX()).getPiece();
                this.getSquare(5, squareTemp1.getX()).setPiece(rook);
                this.getSquare(7, squareTemp1.getX()).setPiece(null);
            }
        } else if (moveTemp instanceof PromotionMoveTemp promotionMove) {
            pieceTemp = promotionMove.getTargetPiece();
        }
        squareTemp2.setPiece(pieceTemp);
        removePiece(squareTemp1);
        squareTemp1.setPiece(null);

    }

    public BoardTemp getCopy() {
        return new BoardTemp(board);
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

    private void removePiece (SquareTemp squareTemp) {
        PlayerTemp playerTemp = squareTemp.getPiece().getPlayer();
        if (playerTemp.getColor().equals("white")) {
            piecesWhite.remove(squareTemp);
        } else {
            piecesBlack.remove(squareTemp);
        }
    }

    public List<SquareTemp> getPieces(PlayerTemp playerTemp) {
        if (playerTemp == null) {
            return null;
        } else if (playerTemp.getColor().equals("white")) {
            return piecesWhite;
        } else {
            return piecesBlack;
        }
    }

    public boolean isCheck(PlayerTemp playerTemp) {
        // Implement the logic to check if a player is in check
        return false;
    }

    public boolean isCheckmate(PlayerTemp playerTemp) {
        // Implement the logic to check if a player is in checkmate
        return false;
    }

    public boolean isStalemate(PlayerTemp playerTemp) {
        // Implement the logic to check if a player is in stalemate
        return false;
    }

    public boolean isDraw(PlayerTemp playerTemp) {
        // Implement the logic to check if the game is a draw
        return false;
    }

    public boolean isGameOver(PlayerTemp playerTemp) {
        // Implement the logic to check if the game is over
        return false;
    }

    public boolean isCapture(MoveTemp moveTemp) {
        // Implement the logic to check if a move is a capture
        return false;
    }
}