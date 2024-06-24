package ptp.project.logic.board;

import ptp.project.logic.enums.CastleOptions;
import ptp.project.logic.moves.Move;

import java.util.List;

public class Board {
    private int[] board;
    private int row;
    private int col;
    private CastleOptions castleOptionsWhite;
    private CastleOptions castleOptionsBlack;

    List<Move> moves;

    /**
     * Initialize new board.
     *
     * @param row Amount of rows of the board.
     * @param col Amount of columns of the board.
     */
    public Board(int row, int col) {
        board = new int[row * col];
        this.row = row;
        this.col = col;
        castleOptionsWhite = CastleOptions.SHORT_AND_LONG;
        castleOptionsBlack = CastleOptions.SHORT_AND_LONG;
    }

    public Board(int row, int col, int[] boardPosition, CastleOptions canCastleWhite, CastleOptions canCastleBlack) {
        if (boardPosition.length == row * col) {
            this.board = boardPosition;
            this.row = row;
            this.col = col;
            this.castleOptionsWhite = canCastleWhite;
            this.castleOptionsBlack = canCastleBlack;
        } else {
            System.out.println("Board size of board position and board size do not match");
        }
    }

    /**
     * Gets the board.
     *
     * @return Returns the board as an array.
     * #Example for how a 8x8 board is numerated: {@link ptp.project.logic.docs}
     * #Entries for values of status of the game: {@link ptp.project.logic.docs}
     */
    public int[] getBoard() {
        return board;
    }

    /**
     * Gets the amount of rows.
     *
     * @return Number of rows,
     */
    public int getRowSize() {
        return row;
    }

    /**
     * Gets the amount of columns.
     *
     * @return Number of columns.
     */
    public int getColSize() {
        return col;
    }

    /**
     * Whether castling is possible.
     *
     * @param color #Entries for values of colors: {@link ptp.project.logic.docs}
     * @return enum of CastleOptions
     */
    public CastleOptions getCanCastle(int color) {
        if (color == 0) {
            return castleOptionsWhite;
        }
        return castleOptionsBlack;
    }

    /**
     * Gets the piece on the square.
     *
     * @param square #Example for how a 8x8 board is numerated: {@link ptp.project.logic.docs}
     * @return #Entries of values of squares follow this pattern: {@link ptp.project.logic.docs}
     */
    public int getPiece(int square) {
        return board[square];
    }

    /**
     * Executes a move, does not check correctness.
     *
     * @param move Move to execute.
     */
    public void executeMove(Move move) {
        if (move.getSquareStart() >= board.length || move.getSquareEnd() >= board.length) {
            System.out.println(move.getMoveNotation(col) + " is not inside the board");
            return;
        }

        //when the white king moves there can no longer be castling
        if (castleOptionsWhite != CastleOptions.NONE && move.getSquareStart() == 4) {
            castleOptionsWhite = CastleOptions.SHORT_AND_LONG;
        }

        //when the left white rook moves, castling long is no longer possible.
        if (castleOptionsWhite == CastleOptions.SHORT_AND_LONG && (move.getSquareStart() == 0 || move.getSquareEnd() == 0)) {
            castleOptionsWhite = CastleOptions.SHORT;
        }
        if (castleOptionsWhite == CastleOptions.LONG && (move.getSquareStart() == 0 || move.getSquareEnd() == 0)) {
            castleOptionsWhite = CastleOptions.NONE;
        }

        //when the right white rook moves, castling short is no longer possible.
        if (castleOptionsWhite == CastleOptions.SHORT_AND_LONG && (move.getSquareStart() == row - 1 || move.getSquareEnd() == row - 1)) {
            castleOptionsWhite = CastleOptions.LONG;
        }
        if (castleOptionsWhite == CastleOptions.SHORT && (move.getSquareStart() == row - 1 || move.getSquareEnd() == row - 1)) {
            castleOptionsWhite = CastleOptions.NONE;
        }

        //when the black king moves
        if (castleOptionsBlack != CastleOptions.NONE && move.getSquareStart() == 4 + col * (row - 1)) {
            castleOptionsBlack = CastleOptions.NONE;
        }

        //when the left black rook moves, castling long is no longer possible.
        if (castleOptionsBlack == CastleOptions.SHORT_AND_LONG && (move.getSquareStart() == col * (row - 1) || move.getSquareEnd() == col * (row - 1))) {
            castleOptionsBlack = CastleOptions.SHORT;
        }
        if (castleOptionsBlack == CastleOptions.LONG && (move.getSquareStart() == col * (row - 1) || move.getSquareEnd() == col * (row - 1))) {
            castleOptionsBlack = CastleOptions.NONE;
        }

        //when the right black rook moves, castling short is no longer possible.
        if (castleOptionsBlack == CastleOptions.SHORT_AND_LONG && (move.getSquareStart() == (col * row) - 1 || move.getSquareEnd() == (col * row) - 1)) {
            castleOptionsBlack = CastleOptions.LONG;
        }
        if (castleOptionsBlack == CastleOptions.SHORT && (move.getSquareStart() == (col * row) - 1 || move.getSquareEnd() == (col * row) - 1)) {
            castleOptionsBlack = CastleOptions.NONE;
        }

        board[move.getSquareEnd()] = move.getPiece();
        board[move.getSquareStart()] = 0;
    }
}
