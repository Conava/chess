package io.github.conava.chess.core.logic.ruleset.possibleMoves;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.data.pieces.Pieces;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.player.PlayerColor;
import io.github.conava.chess.core.logic.moves.Move;

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
        possibleMoves.addAll(enPassantMoves());

        return possibleMoves;
    }

    /**
     * Returns en passant target squares when conditions are met.
     * <p>
     * En passant is available when:
     * <ol>
     *   <li>This pawn is on the en passant rank (y=4 for white, y=3 for black).</li>
     *   <li>The last move in the history was a double pawn push (2 squares straight, same file).</li>
     *   <li>That pawn landed on the same rank as this pawn.</li>
     *   <li>That pawn's file is adjacent (x-1 or x+1) to this pawn.</li>
     * </ol>
     * The target square is the diagonal-forward square toward the enemy pawn's file.
     * Actual pawn capture (removing the enemy pawn) is handled at execution time in
     * {@code Board.executeMove} when a pawn moves diagonally to an empty square.
     *
     * @return list of en passant target squares (empty if en passant is not available)
     */
    private List<Square> enPassantMoves() {
        List<Square> result = new ArrayList<>();

        if (moves == null || moves.isEmpty()) {
            return result;
        }

        // En passant rank: y=4 for white (direction=+1), y=3 for black (direction=-1)
        int enPassantRank = (direction == 1) ? 4 : 3;
        if (square.getY() != enPassantRank) {
            return result;
        }

        Move lastMove = moves.get(moves.size() - 1);
        int startY = lastMove.getStart().getY();
        int endY = lastMove.getEnd().getY();
        int startX = lastMove.getStart().getX();
        int endX = lastMove.getEnd().getX();

        // Last move must be a pawn double push: piece type is PAWN, 2 squares straight (same file)
        if (lastMove.getPieceType() != Pieces.PAWN
                || Math.abs(endY - startY) != 2
                || startX != endX) {
            return result;
        }

        // That pawn must have landed on the same rank as this pawn
        if (endY != square.getY()) {
            return result;
        }

        // That pawn's file must be adjacent to this pawn
        int fileDiff = endX - square.getX();
        if (Math.abs(fileDiff) != 1) {
            return result;
        }

        // En passant target: one square forward, toward the enemy pawn's file
        int targetY = square.getY() + direction;
        int targetX = endX;
        if (isInBounds(targetY, targetX)) {
            result.add(board.getSquare(targetY, targetX));
        }

        return result;
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
