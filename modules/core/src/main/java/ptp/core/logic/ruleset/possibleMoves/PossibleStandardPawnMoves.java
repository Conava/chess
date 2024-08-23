package ptp.core.logic.ruleset.possibleMoves;

import ptp.core.data.Square;
import ptp.core.data.board.Board;
import ptp.core.data.player.Player;
import ptp.core.data.player.PlayerColor;
import ptp.core.logic.moves.Move;

import java.util.ArrayList;
import java.util.List;

public class PossibleStandardPawnMoves {

    private final Square square;
    private final Board board;
    private final List<Move> moves;
    private final int colCount;
    private final int rowCount;
    private final int direction;
    private final Player owner;

    /**
     * Initiates a new instance of a standard pawn move lookup table
     *
     * @param square The square the pawn is on
     * @param board  The board surrounding the pawn
     * @param moves List of Past moves, used for en passant
     */
    public PossibleStandardPawnMoves(Square square, Board board, List<Move> moves) {
        this.square = square;
        this.board = board;
        this.moves = moves;
        colCount = board.getColCount();
        rowCount = board.getRowCount();
        owner = square.isOccupiedBy();

        if (owner.color().equals(PlayerColor.WHITE)) {
            direction = 1;
        } else {
            direction = -1;
        }
    }

    public List<Square> possibleMoves() {
        List<Square> possibleMoves = new ArrayList<>();
        Square possibleSquare;

        //move 1 square
        if (isInBounds(square.getY() + direction, square.getX())) {
            possibleSquare = board.getSquare(square.getY() + direction, square.getX());
            if (possibleSquare.isEmpty()) {
                possibleMoves.add(possibleSquare);

                //move 2 squares
                if (isOnHomeSquare(owner.color()) && isInBounds(square.getY() + 2 * direction, square.getX())) {
                    possibleSquare = board.getSquare(square.getY() + 2 * direction, square.getX());
                    if (possibleSquare.isEmpty()) {
                        possibleMoves.add(possibleSquare);
                    }
                }
            }
        }

        possibleMoves.addAll(possibleCaptureMoves());

        return possibleMoves;
    }

    public List<Square> possibleCaptureMoves() {
        List<Square> possibleMoves = new ArrayList<>();
        Square possibleSquare;

        //capture left
        if (isInBounds(square.getY() + direction, square.getX() - 1)) {
            possibleSquare = board.getSquare(square.getY() + direction, square.getX() - 1);
            if (isCapture(possibleSquare, owner)) {
                possibleMoves.add(possibleSquare);
            }
        }

        //capture right
        if (isInBounds(square.getY() + direction, square.getX() + 1)) {
            possibleSquare = board.getSquare(square.getY() + direction, square.getX() + 1);
            if (isCapture(possibleSquare, owner)) {
                possibleMoves.add(possibleSquare);
            }
        }

        return possibleMoves;
    }

    /**
     * Checks if the coordinates lead to a square in bounds
     *
     * @param y Y coordinate of the target
     * @param x X coordinate of the target
     * @return ?isInBounds
     */
    private boolean isInBounds(int y, int x) {
        return y >= 0 && y < colCount && x >= 0 && x < rowCount;
    }

    /**
     * Checks if the pawn is on the expected home square.
     *
     * @param color the color of the Player owning the piece
     * @return ?isOnHomeSquare
     */
    private boolean isOnHomeSquare(PlayerColor color) {
        if (color.equals(PlayerColor.WHITE) && square.getY() == 1) {
            return true;
        } else return color.equals(PlayerColor.BLACK) && square.getY() == 6;
    }

    /**
     * Checks if the given square would result in capture.
     *
     * @param possibleSquare Square to check on
     * @param owner owner trying to capture a piece
     * @return ?isCapture
     */
    private boolean isCapture(Square possibleSquare, Player owner) {
        if (possibleSquare.isEmpty()) {
            return false;
        } else return !possibleSquare.isOccupiedBy().equals(owner);
    }
}
