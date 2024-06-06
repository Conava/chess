package ptp.project.logic.ruleset;

import ptp.project.exceptions.IsCheckException;
import ptp.project.logic.*;
import ptp.project.logic.pieces.*;

//import java.lang.reflect.Array;
import java.util.List;
import java.util.ArrayList;

public class StandardChessRuleset implements Ruleset {

    //height and width start at 1 and go to 8

    @Override
    public int getWidth() {
        return 8;
    }

    @Override
    public int getHeight() {
        return 8;
    }

    @Override
    public Square[][] getStartBoard(Player player1, Player player2) {
        Square[][] startBoard = new Square[8][8];
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                startBoard[x][y] = new Square(y, x);
            }
        }

        startBoard[0][0].setPiece(new Rook(player1));
        startBoard[1][0].setPiece(new Knight(player1));
        startBoard[2][0].setPiece(new Bishop(player1));
        startBoard[3][0].setPiece(new Queen(player1));
        startBoard[4][0].setPiece(new King(player1));
        startBoard[5][0].setPiece(new Bishop(player1));
        startBoard[6][0].setPiece(new Knight(player1));
        startBoard[7][0].setPiece(new Rook(player1));

        for (int x = 0; x < 8; x++) {
            startBoard[x][1].setPiece(new Pawn(player1));
        }

        startBoard[0][7].setPiece(new Rook(player2));
        startBoard[1][7].setPiece(new Knight(player2));
        startBoard[2][7].setPiece(new Bishop(player2));
        startBoard[3][7].setPiece(new Queen(player2));
        startBoard[4][7].setPiece(new King(player2));
        startBoard[5][7].setPiece(new Bishop(player2));
        startBoard[6][7].setPiece(new Knight(player2));
        startBoard[7][7].setPiece(new Rook(player2));

        for (int x = 0; x < 8; x++) {
            startBoard[x][6].setPiece(new Pawn(player2));
        }

        return startBoard;
    }

    @Override
    public List<Square> getLegalMoves(Square square, Board board) throws IsCheckException {
        List<Square> legalMoves;
        if (square.getPiece() instanceof Rook) {
            legalMoves = getLegalMovesRook(square, board);
        } else if (square.getPiece() instanceof Knight) {
            legalMoves = getLegalMovesKnight(square, board);
        } else if (square.getPiece() instanceof Bishop) {
            legalMoves = getLegalMovesBishop(square, board);
        } else if (square.getPiece() instanceof Queen) {
            legalMoves = getLegalMovesBishop(square, board);
            legalMoves.addAll(getLegalMovesRook(square, board));
        } else if (square.getPiece() instanceof King) {
            legalMoves = getLegalMovesKing(square, board);
        } else if (square.getPiece() instanceof Pawn) {
            legalMoves = getLegalMovesPawn(square, board);
        } else { //space is empty
            return null;
        }
        //@TODO: Rochade
        //@TODO Umwandlung zuordnen zu wo moves ausgeführt werden
        //@TODO TTests
        return legalMoves;
    }

    /**
     * Returns legal moves for the rook.
     */
    private List<Square> getLegalMovesRook(Square square, Board board) throws IsCheckException {
        Player owner = square.isOccupiedBy();
        List<Square> legalMoves = new ArrayList<>();
        Square possibleSquare;
        //checks column down
        for (int y = square.getY(); y >= 0; y--) {
            possibleSquare = board.getSquare(y, square.getX());
            if (possibleSquare.isOccupiedBy() == null || isCapture(possibleSquare, owner)) {
                legalMoves.add(possibleSquare);
                if (isCapturePiece(possibleSquare, owner) != null &&
                        isCapturePiece(possibleSquare, owner) instanceof King) {
                    throw new IsCheckException(square);
                }
            } else break;
        }
        //checks column up
        for (int y = square.getY(); y < 8; y++) {
            possibleSquare = board.getSquare(y, square.getX());
            if (possibleSquare.isOccupiedBy() == null || isCapture(possibleSquare, owner)) {
                legalMoves.add(possibleSquare);
                if (isCapturePiece(possibleSquare, owner) != null &&
                        isCapturePiece(possibleSquare, owner) instanceof King) {
                    throw new IsCheckException(square);
                }
            } else break;
        }
        //checks row left
        for (int x = square.getX(); x >= 0; x--) {
            possibleSquare = board.getSquare(square.getY(), x);
            if (possibleSquare.isOccupiedBy() == null || isCapture(possibleSquare, owner)) {
                legalMoves.add(possibleSquare);
                if (isCapturePiece(possibleSquare, owner) != null &&
                        isCapturePiece(possibleSquare, owner) instanceof King) {
                    throw new IsCheckException(square);
                }
            } else break;
        }
        //checks row right
        for (int x = square.getX(); x < 8; x++) {
            possibleSquare = board.getSquare(square.getY(), x);
            if (possibleSquare.isOccupiedBy() == null || isCapture(possibleSquare, owner)) {
                legalMoves.add(possibleSquare);
                if (isCapturePiece(possibleSquare, owner) != null &&
                        isCapturePiece(possibleSquare, owner) instanceof King) {
                    throw new IsCheckException(square);
                }
            } else break;
        }
        return legalMoves;
    }

    /**
     * Returns legal moves for the bishop.
     */
    private List<Square> getLegalMovesBishop(Square square, Board board) throws IsCheckException {
        Player owner = square.isOccupiedBy();
        List<Square> legalMoves = new ArrayList<>();
        Square possibleSquare;
        //checks diagonal down left
        for (int i = 0; i < 8; i++) {
            possibleSquare = board.getSquare(square.getY() - i, square.getX() - i);
            if (isValidSquare(possibleSquare) && possibleSquare.isOccupiedBy() == null ||
                    isCapture(possibleSquare, owner)) {
                legalMoves.add(possibleSquare);
                if (isCapturePiece(possibleSquare, owner) != null &&
                        isCapturePiece(possibleSquare, owner) instanceof King) {
                    throw new IsCheckException(square);
                }
            } else break;
        }
        //checks diagonal up left
        for (int i = 0; i < 8; i++) {
            possibleSquare = board.getSquare(square.getY() + i, square.getX() - i);
            if (isValidSquare(possibleSquare) && possibleSquare.isOccupiedBy() == null ||
                    isCapture(possibleSquare, owner)) {
                legalMoves.add(possibleSquare);
                if (isCapturePiece(possibleSquare, owner) != null &&
                        isCapturePiece(possibleSquare, owner) instanceof King) {
                    throw new IsCheckException(square);
                }
            } else break;
        }
        //checks diagonal down right
        for (int i = 0; i < 8; i++) {
            possibleSquare = board.getSquare(square.getY() - i, square.getX() + i);
            if (isValidSquare(possibleSquare) && possibleSquare.isOccupiedBy() == null ||
                    isCapture(possibleSquare, owner)) {
                legalMoves.add(possibleSquare);
                if (isCapturePiece(possibleSquare, owner) != null &&
                        isCapturePiece(possibleSquare, owner) instanceof King) {
                    throw new IsCheckException(square);
                }
            } else break;
        }
        //checks diagonal down left
        for (int i = 0; i < 8; i++) {
            possibleSquare = board.getSquare(square.getY() + i, square.getX() + i);
            if (isValidSquare(possibleSquare) && possibleSquare.isOccupiedBy() == null ||
                    isCapture(possibleSquare, owner)) {
                legalMoves.add(possibleSquare);
                if (isCapturePiece(possibleSquare, owner) != null &&
                        isCapturePiece(possibleSquare, owner) instanceof King) {
                    throw new IsCheckException(square);
                }
            } else break;
        }

        return legalMoves;
    }

    /**
     * Returns legal moves for the knight.
     */
    private List<Square> getLegalMovesKnight(Square square, Board board) throws IsCheckException {
        Player owner = square.isOccupiedBy();
        List<Square> legalMoves = new ArrayList<>();

        /*
            -2 / +1
            -2 / -1
            -1 / +2
            -1 / -2
            +1 / +2
            +1 / -2
            +2 / +1
            +2 / -1
         */
        int[] arrY = {-2, -2, -1, -1, +1, +1, +2, +2};
        int[] arrX = {+1, -1, +2, -2, +2, -2, +1, -1};

        for (int i = 0; i < 8; i++) {
            Square possibleSquare = board.getSquare(square.getY() + arrY[i], square.getX() + arrX[i]);
            if (isValidSquare(possibleSquare) && possibleSquare.isOccupiedBy() == null ||
                    isCapture(possibleSquare, owner)) {
                legalMoves.add(possibleSquare);
                if (isCapturePiece(possibleSquare, owner) != null &&
                        isCapturePiece(possibleSquare, owner) instanceof King) {
                    throw new IsCheckException(square);
                }
            }
        }

        return legalMoves;
    }

    /**
     * Returns legal moves for the king.
     */
    private List<Square> getLegalMovesKing(Square square, Board board) throws IsCheckException {
        Player owner = square.isOccupiedBy();
        List<Square> legalMoves = new ArrayList<>();

        int[] arrY = {-1, 0, +1, +1, +1, 0, -1, -1};
        int[] arrX = {+1, +1, +1, 0, -1, -1, -1, 0};

        for (int i = 0; i < 8; i++) {
            Square possibleSquare = board.getSquare(square.getY() + arrY[i], square.getX() + arrX[i]);
            if (isValidSquare(possibleSquare) && possibleSquare.isOccupiedBy() == null ||
                    isCapture(possibleSquare, owner)) {
                legalMoves.add(possibleSquare);
                if (isCapturePiece(possibleSquare, owner) != null &&
                        isCapturePiece(possibleSquare, owner) instanceof King) {
                    throw new IsCheckException(square);
                }
            }
        }

        return legalMoves;
    }

    /**
     * Returns legal moves for the pawn.
     */
    private List<Square> getLegalMovesPawn(Square square, Board board) throws IsCheckException {
        Player owner = square.isOccupiedBy();
        List<Square> legalMoves = new ArrayList<>();
        Square possibleSquare;
        int direction;

        if (owner.getColor().equals("white")) {
            direction = 1;
        } else {
            direction = -1;
        }
        //move 1 square
        possibleSquare = board.getSquare(square.getY(), square.getX() + direction);
        if (possibleSquare.isOccupiedBy() == null) {
            legalMoves.add(possibleSquare);
            if (isCapturePiece(possibleSquare, owner) != null &&
                    isCapturePiece(possibleSquare, owner) instanceof King) {
                throw new IsCheckException(square);
            }
        }
        //taking left
        possibleSquare = board.getSquare(square.getY() - 1, square.getX() + direction);
        if (isCapture(possibleSquare, owner)) {
            legalMoves.add(possibleSquare);
            if (isCapturePiece(possibleSquare, owner) != null &&
                    isCapturePiece(possibleSquare, owner) instanceof King) {
                throw new IsCheckException(square);
            }
        }
        //taking right
        possibleSquare = board.getSquare(square.getY() + 1, square.getX() + direction);
        if (isCapture(possibleSquare, owner)) {
            legalMoves.add(possibleSquare);
            if (isCapturePiece(possibleSquare, owner) != null &&
                    isCapturePiece(possibleSquare, owner) instanceof King) {
                throw new IsCheckException(square);
            }
        }
        //moving 2 squares at the start
        if (isOnBaseRank(square, owner) && board.getSquare(square.getY(), square.getX() + direction).isOccupiedBy() == null
                && board.getSquare(square.getY(), square.getX() + 2 * direction).isOccupiedBy() == null) {
            legalMoves.add(board.getSquare(square.getY() + 1, square.getX() + 2 * direction));
        }
        return legalMoves;
    }

    private boolean isValidSquare(Square square) {
        return square.getY() >= 0 && square.getY() <= 7 &&
                square.getX() >= 0 && square.getX() <= 7;
    }

    /**
     * @param square Square where to move to
     * @param player Owner of the capturing peace
     * @return If the move is a possible capture
     */
    private boolean isCapture(Square square, Player player) {
        return isValidSquare(square) && !(square.isOccupiedBy() == null) && !square.isOccupiedBy().equals(player);
    }

    private Piece isCapturePiece(Square square, Player player) {
        if (isCapture(square, player)) {
            return square.getPiece();
        }
        return null;
    }

    private boolean isOnBaseRank(Square square, Player player) {
        int rank = -1; // there should be no piece on rank -1
        if (player.getColor().equals("white")) {
            rank = 1;
        } else {
            rank = 6;
        }
        return square.getX() == rank;
    }

    /**
     * Checks if move is a promotion.
     * @param square Square where the piece moves to
     * @return ?isPromotion
     */
    public boolean isPromotion(Square square) {
        return square.getPiece() instanceof Pawn && square.getX() == 0 ||
                square.getPiece() instanceof Pawn && square.getX() == 7;
    }

    /**
     * @param board  Board, where the check should be checked
     * @param player Player who can move
     * @return ?isCheck
     */
    @Override
    public boolean isCheck(Board board, Player player) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                try {
                    getLegalMoves(board.getSquare(y, x),board);
                } catch (IsCheckException e) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean verifyMove(Move move) {
        return false;
    }

    @Override
    public boolean verifyMove(Square newPosition, Piece piece) {
        return false;
    }

    @Override
    public Move hasEnforcedMove(Player player) {
        return null;
    }
}