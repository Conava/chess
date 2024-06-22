package ptp.project.logic.rulesets;

import ptp.project.logic.board.Board;
import ptp.project.logic.moves.Move;
import ptp.project.logic.player.Player;

import java.util.ArrayList;
import java.util.List;

public class RulesetClassic implements Ruleset {

    /**
     * Gets the height of the board.
     *
     * @return Amount of rows.
     */
    @Override
    public int getHeight() {
        return 8;
    }

    /**
     * Gets the width of the board.
     *
     * @return Amount of columns.
     */
    @Override
    public int getWidth() {
        return 8;
    }

    /**
     * Gets the state of the board at the start of the game.
     *
     * @return Array of board positions: #Example for how a 8x8 board is numerated:
     * #Entries of values of squares follow this pattern:{@link ptp.project.logic.docs}
     */
    @Override
    public int[] getStartBoard() {
        return new int[]{7, 3, 5, 9, 11, 5, 3, 7,
                1, 1, 1, 1, 1, 1, 1, 1,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                2, 2, 2, 2, 2, 2, 2, 2,
                8, 4, 6, 10, 12, 6, 4, 8};
    }

    /**
     * Gets all legal Moves
     *
     * @param board The board, where the moves happen.
     * @return List of Legal moves.
     */
    @Override
    public List<Move> getLegalMoves(Board board) {
        return List.of();
        //todo
    }

    /**
     * Gets all legal Squares accessible from given square.
     *
     * @param square start square #Example for how a 8x8 board is numerated: {@link ptp.project.logic.docs}
     * @param board  The board, where the moves happen.
     * @param player The player to move.
     * @param moves List of prior moves.
     * @return #Example for how a 8x8 board is numerated: {@link ptp.project.logic.docs}
     */
    @Override
    public List<Integer> getLegalSquares(int square, Board board, Player player, List<Move> moves) {
        return getSudoLegalSquares(square, board, player, moves);
        //todo
    }

    private List<Integer> getSudoLegalSquares(int square, Board board, Player player, List<Move> moves) {
        List<Integer> sudoLegalSquares = new ArrayList<>();

        //only pieces of the given player can be moved.
        if (board.getPiece(square) == 0 || board.getPiece(square) % 2 == player.getOpponentColor()) {
            return sudoLegalSquares;

        }
        switch (board.getPiece(square)) {
            case 1:
                sudoLegalSquares.addAll(getSudoLegalSquaresPawnWhite(square, board, player, moves));
                break;
            case 2:
                sudoLegalSquares.addAll(getSudoLegalSquaresPawnBlack(square, board, player, moves));
                break;
            case 3:
            case 4:
                sudoLegalSquares.addAll(getSudoLegalSquaresKnight(square, board, player));
                break;
            case 5:
            case 6:
                sudoLegalSquares.addAll(getSudoLegalSquaresBishop(square, board, player));
                break;
            case 7:
            case 8:
                sudoLegalSquares.addAll(getSudoLegalSquaresRook(square, board, player));
                break;
            case 9:
            case 10:
                sudoLegalSquares.addAll(getSudoLegalSquaresQueen(square, board, player));
                break;
            case 11:
            case 12:
                sudoLegalSquares.addAll(getSudoLegalSquaresKing(square, board, player));
                break;
            case 0:
            default:
                System.out.println("There is no piece");
                break;
        }
        return sudoLegalSquares;
    }

    private List<Integer> getSudoLegalSquaresPawnWhite(int square, Board board, Player player, List<Move> moves) {
        List<Integer> sudoLegalSquares = new ArrayList<>();
        if (board.getPiece(square + getWidth()) == 0) {
            sudoLegalSquares.add(square + getWidth());
            if (square % getWidth() == 1 && board.getPiece(square + 2 * getWidth()) == 0) {
                sudoLegalSquares.add(square + 2 * getWidth());
            }
        }
        if (board.getPiece(square + getWidth() - 1) % 2 == player.getOpponentColor()) {
            sudoLegalSquares.add(square + getWidth() - 1);
        }
        if (board.getPiece(square + getWidth() + 1) % 2 == player.getOpponentColor()) {
            sudoLegalSquares.add(square + getWidth() + 1);
        }
        return sudoLegalSquares; //todo en passant
    }

    private List<Integer> getSudoLegalSquaresPawnBlack(int square, Board board, Player player, List<Move> moves) {
        List<Integer> sudoLegalSquares = new ArrayList<>();
        if (board.getPiece(square - getWidth()) == 0) {
            sudoLegalSquares.add(square - getWidth());
            if (square % getWidth() == 6 && board.getPiece(square - 2 * getWidth()) == 0) {
                sudoLegalSquares.add(square - 2 * getWidth());
            }
        }
        if (board.getPiece(square - getWidth() - 1) % 2 == player.getOpponentColor()) {
            sudoLegalSquares.add(square - getWidth() - 1);
        }
        if (board.getPiece(square - getWidth() + 1) % 2 == player.getOpponentColor()) {
            sudoLegalSquares.add(square - getWidth() + 1);
        }
        return sudoLegalSquares; //todo en passant
    }

    private List<Integer> getSudoLegalSquaresKnight(int square, Board board, Player player) {
        List<Integer> sudoLegalSquares = new ArrayList<>();
        int[] offsets = {2 * getWidth() - 1, 2 * getWidth() + 1, getWidth() - 2, getWidth() + 2, -(getWidth() - 2), -(getWidth() + 2), -(2 * getWidth() - 1), -(2 * getWidth() + 1)};
        for (int offset : offsets) {
            if (!isValidSquare(getGoalSquare(square, offset)) || isSkippingRows(square, offset)) {
                continue;
            }
            if (board.getPiece(getGoalSquare(square, offset)) == 0 || board.getPiece(getGoalSquare(square, offset)) % 2 == player.getOpponentColor()) {
                sudoLegalSquares.add(getGoalSquare(square, offset));
            }
        }
        return sudoLegalSquares;
    }

    private List<Integer> getSudoLegalSquaresBishop(int square, Board board, Player player) {
        List<Integer> sudoLegalSquares = new ArrayList<>();
        int[] offsets = {getWidth() + 1, getWidth() - 1, -(getWidth() + 1), -(getWidth() - 1)};
        for (int offset : offsets) {
            for (int i = 0; i < 8; i++) {
                if (!isValidSquare(getGoalSquare(square, offset)) || isSkippingRows(square, offset)) {
                    break;
                }
                if (board.getPiece(getGoalSquare(square, offset)) == 0 || board.getPiece(getGoalSquare(square, offset)) % 2 == player.getOpponentColor()) {
                    sudoLegalSquares.add(getGoalSquare(square, offset));
                }
            }
        }

        return sudoLegalSquares;
    }

    private List<Integer> getSudoLegalSquaresRook(int square, Board board, Player player) {
        List<Integer> sudoLegalSquares = new ArrayList<>();
        int[] offsets = {getWidth(), -1, +1, -getWidth()};
        for (int offset : offsets) {
            for (int i = 0; i < 8; i++) {
                if (!isValidSquare(getGoalSquare(square, offset)) || isSkippingRows(square, offset)) {
                    break;
                }
                if (board.getPiece(getGoalSquare(square, offset)) == 0 || board.getPiece(getGoalSquare(square, offset)) % 2 == player.getOpponentColor()) {
                    sudoLegalSquares.add(getGoalSquare(square, offset));
                }
            }
        }

        return sudoLegalSquares;
    }

    private List<Integer> getSudoLegalSquaresQueen(int square, Board board, Player player) {
        List<Integer> sudoLegalSquares = new ArrayList<>();
        sudoLegalSquares.addAll(getSudoLegalSquaresRook(square, board, player));
        sudoLegalSquares.addAll(getSudoLegalSquaresBishop(square, board, player));
        return sudoLegalSquares;
    }

    private List<Integer> getSudoLegalSquaresKing(int square, Board board, Player player) {
        List<Integer> sudoLegalSquares = new ArrayList<>();
        int[] offsets = {getWidth() - 1, getWidth(), getWidth() + 1, -1, +1, -(getWidth() - 1), -getWidth(), -(getWidth() + 1)};
        for (int offset : offsets) {
            if (!isValidSquare(getGoalSquare(square, offset)) || isSkippingRows(square, offset)) {
                continue;
            }
            if (board.getPiece(getGoalSquare(square, offset)) == 0 || board.getPiece(getGoalSquare(square, offset)) % 2 == player.getOpponentColor()) {
                sudoLegalSquares.add(getGoalSquare(square, offset));
            }
        }

        //castling long
        if (sudoLegalSquares.contains(getGoalSquare(square, -1)) && board.getCanCastle(player.getColor()) >= 2) {
            if (board.getPiece(getGoalSquare(square, -2)) == 0 || board.getPiece(getGoalSquare(square, -2)) % 2 == player.getOpponentColor()) {
                sudoLegalSquares.add(getGoalSquare(square, -2));
            }
        }

        //castling short
        if (sudoLegalSquares.contains(getGoalSquare(square, 1)) && board.getCanCastle(player.getColor()) % 2 == 1) {
            if (board.getPiece(getGoalSquare(square, 2)) == 0 || board.getPiece(getGoalSquare(square, 2)) % 2 == player.getOpponentColor()) {
                sudoLegalSquares.add(getGoalSquare(square, 2));
            }
        }

        return sudoLegalSquares;
    }

    /**
     * Returns if the square is valid.
     *
     * @param square #Example for how a 8x8 board is numerated: {@link ptp.project.logic.docs}
     * @return bool if the square exists.
     */
    @Override
    public boolean isValidSquare(int square) {
        return square < getHeight() * getWidth();
    }

    /**
     * Whether the move is legal.
     *
     * @param rowStart Row of the start square.
     * @param colStart Column of the start square.
     * @param rowEnd   Row of the end square.
     * @param colEnd   Column of the end square.
     * @param board    The board, where the moves happen.
     * @return bool if move is legal.
     */
    @Override
    public boolean isLegalMove(int rowStart, int colStart, int rowEnd, int colEnd, Board board) {
        Move move = new Move(getSquare(rowStart, colStart), getSquare(rowEnd, colEnd));
        return isLegalMove(move, board);
    }

    /**
     * Whether the move is legal.
     *
     * @param move  Move to play.
     * @param board The board, where the moves happen.
     * @return bool if move is legal.
     */
    @Override
    public boolean isLegalMove(Move move, Board board) {
        return getLegalMoves(board).contains(move);
    }

    /**
     * Gets the value of the desired square.
     *
     * @param row Row of the square.
     * @param col Column of the square.
     * @return Int value of the square:
     * #Entries of values of squares follow this pattern:{@link ptp.project.logic.docs}
     */
    private int getSquare(int row, int col) {
        return getWidth() * row + col;
    }

    /**
     * Puts out the goal square reachable from square with given offset. Does not check out of bounds.
     *
     * @param square The square to start.
     * @param offset The offset from the start point.
     * @return The goal square #Example for how a 8x8 board is numerated: {@link ptp.project.logic.docs}
     */
    private int getGoalSquare(int square, int offset) {
        return square + offset;
    }

    /**
     * As there are no hard outlines for columns. This method checks if a moves unintentionally goes beyond.
     *
     * @param squareStart The square to start.
     * @param offset      The offset from the start square.
     * @return Whether the move from
     */
    private boolean isSkippingRows(int squareStart, int offset) {
        return squareStart % getWidth() + offset % getWidth() >= getWidth();
    }
}

