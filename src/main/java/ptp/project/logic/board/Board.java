package ptp.project.logic.board;

import ptp.project.logic.moves.Move;

import java.util.List;

public class Board {
    private final int[] board;
    private final int row;
    private final int col;
    private int canCastleWhite;
    private int canCastleBlack;

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
        canCastleWhite = 3;
        canCastleBlack = 3;
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
     * @return #Entries for values of states of canCastleWhite: {@link ptp.project.logic.docs}
     */
    public int getCanCastle(int color) {
        if (color == 0) {
            return canCastleWhite;
        }
        return canCastleBlack;
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
        if (canCastleWhite != 0 && move.getSquareStart() == 4) {
            canCastleWhite = 0;
        }

        //when the left white rook moves, castling long is no longer possible.
        if (canCastleWhite >= 2 && (move.getSquareStart() == 0 || move.getSquareEnd() == 0)) {
            canCastleWhite -= 2;
        }

        //when the right white rook moves, castling short is no longer possible.
        if (canCastleWhite%2 == 1 && (move.getSquareStart() == row - 1 || move.getSquareEnd() == row - 1)) {
            canCastleWhite -= 1;
        }

        //when the black king moves
        if (canCastleBlack != 0 && move.getSquareStart() == 4 + col * (row - 1)) {
            canCastleBlack = 0;
        }

        //when the left black rook moves, castling long is no longer possible.
        if (canCastleBlack >= 2 && (move.getSquareStart() == col * (row - 1) || move.getSquareEnd() == col * (row - 1))) {
            canCastleBlack -= 2;
        }

        //when the right black rook moves, castling short is no longer possible.
        if (canCastleBlack%2 == 1 && (move.getSquareStart() == (col * row) - 1 || move.getSquareEnd() == (col * row) - 1)) {
            canCastleBlack -= 1;
        }

        board[move.getSquareEnd()] = move.getPiece();
        board[move.getSquareStart()] = 0;
    }
}
