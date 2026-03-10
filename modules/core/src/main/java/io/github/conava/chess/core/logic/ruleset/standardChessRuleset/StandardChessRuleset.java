package io.github.conava.chess.core.logic.ruleset.standardChessRuleset;

import io.github.conava.chess.core.data.pieces.*;
import io.github.conava.chess.core.data.player.Player;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.board.Board;
import io.github.conava.chess.core.logic.ruleset.possibleMoves.*;
import io.github.conava.chess.core.logic.moves.Move;
import io.github.conava.chess.core.logic.ruleset.Ruleset;
import io.github.conava.chess.core.logic.ruleset.possibleStartPositions.PossibleStandardPosition;

import java.util.List;
import java.util.ArrayList;

// todo: refactor the ruleset.
//        - Operations should update the game state, it does not need to be calculated

/**
 * Standard chess ruleset.
 * Delivers the starting board and the legal moves for the pieces.
 */
public class StandardChessRuleset implements Ruleset {

    /**
     * Gets the width of the chess board
     *
     * @return int of the number of the columns
     */
    @Override
    public int getWidth() {
        return 8;
    }

    /**
     * Gets the height of the chess board
     *
     * @return int of the amount of rows
     */
    @Override
    public int getHeight() {
        return 8;
    }

    /**
     * Returns the start position
     *
     * @param player1 Player with the white pieces
     * @param player2 Player with the black pieces
     * @return Double array of the board. Starting with y as the first position.
     */
    @Override
    public Square[][] getStartBoard(Player player1, Player player2) {
        PossibleStandardPosition position = new PossibleStandardPosition(player1, player2);
        return position.getStartBoard();
    }

    /**
     * Provides a list of legal moves.
     *
     * @param square  Only moves from this square are shown
     * @param board   Current board
     * @param moves   List of moves already played in-game
     * @param player1 Player to move
     * @param player2 Player opponent
     * @return List of LEGAL moves.
     */
    @Override
    public List<Move> getLegalMoves(Square square, Board board, List<Move> moves, Player player1, Player player2) {
        List<Square> sudoLegalSquares;
        List<Move> legalMoves = new ArrayList<>();

        sudoLegalSquares = getSudoLegalSquares(square, board, moves);

        for (Square squareTemp : sudoLegalSquares) {
            legalMoves.add(new Move(square, squareTemp));
        }

        return legalMoves;
    }

    /**
     * Provides a list of legal squares, filtered to exclude any move that would leave
     * {@code player1}'s king in check.
     *
     * <p>For each pseudo-legal target square, a deep copy of the board is created, the move is
     * simulated on the copy, and {@link #isCheck} is called on the resulting position. Only
     * target squares where the moving player's king is not in check after the move are returned.
     *
     * <p>Special castling handling:
     * <ul>
     *   <li>Castling is illegal when the king is currently in check.</li>
     *   <li>Castling is illegal when the king would pass through an attacked transit square.</li>
     * </ul>
     *
     * @param square  Only moves from this square are shown
     * @param board   Current board
     * @param moves   List of moves already played in-game
     * @param player1 Player to move (whose king must not be left in check)
     * @param player2 Player opponent
     * @return List of LEGAL squares — moves that do not leave {@code player1}'s king in check.
     */
    @Override
    public List<Square> getLegalSquares(Square square, Board board, List<Move> moves, Player player1, Player player2) {
        List<Square> pseudoLegal = getSudoLegalSquares(square, board, moves);
        List<Square> legal = new ArrayList<>();

        boolean currentlyInCheck = isCheck(board, player1, moves);

        for (Square targetSquare : pseudoLegal) {
            // Castling candidate: king moves exactly 2 squares horizontally
            if (square.getPiece() instanceof King
                    && Math.abs(targetSquare.getX() - square.getX()) == 2) {
                // Cannot castle while in check
                if (currentlyInCheck) {
                    continue;
                }
                // Cannot castle through an attacked transit square
                int transitX = square.getX() + Integer.signum(targetSquare.getX() - square.getX());
                Board transitBoardCopy = board.getCopy();
                Square transitStart = transitBoardCopy.getSquare(square.getY(), square.getX());
                Square transitEnd = transitBoardCopy.getSquare(square.getY(), transitX);
                transitBoardCopy.executeMove(new Move(transitStart, transitEnd));
                if (isCheck(transitBoardCopy, player1, moves)) {
                    continue;
                }
            }

            // Check final square: simulate the move and verify the king is not in check
            Board finalBoardCopy = board.getCopy();
            Square finalStart = finalBoardCopy.getSquare(square.getY(), square.getX());
            Square finalEnd = finalBoardCopy.getSquare(targetSquare.getY(), targetSquare.getX());
            finalBoardCopy.executeMove(new Move(finalStart, finalEnd));
            if (!isCheck(finalBoardCopy, player1, moves)) {
                legal.add(targetSquare);
            }
        }

        return legal;
    }

    private List<Square> getSudoLegalSquares(Square square, Board board, List<Move> moves) {
        if (square.getPiece().getClass().equals(Rook.class)) {
            PossibleStandardRookMoves rookMoves = new PossibleStandardRookMoves(square, board);
            return rookMoves.getPossibleSquares();
        } else if (square.getPiece().getClass().equals(Knight.class)) {
            PossibleStandardKnightMoves knightMoves = new PossibleStandardKnightMoves(square, board);
            return knightMoves.getPossibleSquares();
        } else if (square.getPiece().getClass().equals(Bishop.class)) {
            PossibleStandardBishopMoves bishopMoves = new PossibleStandardBishopMoves(square, board);
            return bishopMoves.getPossibleSquares();
        } else if (square.getPiece().getClass().equals(Queen.class)) {
            PossibleStandardQueenMoves queenMoves = new PossibleStandardQueenMoves(square, board);
            return queenMoves.getPossibleSquares();
        } else if (square.getPiece().getClass().equals(King.class)) {
            PossibleStandardKingMoves kingMoves = new PossibleStandardKingMoves(square, board);
            return kingMoves.getPossibleSquares();
        } else if (square.getPiece().getClass().equals(Pawn.class)) {
            PossibleStandardPawnMoves pawnMoves = new PossibleStandardPawnMoves(square, board, moves);
            return pawnMoves.possibleMoves();
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Returns a valid square
     *
     * @param square Square to check
     * @return If the square exists and is in bounds
     */
    public boolean isValidSquare(Square square) {
        return square != null && isInBoundsY(square.getY()) && isInBoundsX(square.getX());
    }

    /**
     * @param board  Board, where the check should be checked
     * @param player Player who can move
     * @return ?isCheck
     */
    @Override
    public boolean isCheck(Board board, Player player, List<Move> moves) {
        return canBeCaptured(board, lookForKing(board, player), moves);
    }

    private Square lookForKing(Board board, Player player) {
        List<Square> potentialSquares = board.getPieces(player);
        for (Square square : potentialSquares) {
            if (square.getPiece() instanceof King) {
                return square;
            }
        }
        return null;
    }

    private boolean canBeCaptured(Board board, Square square, List<Move> moves) {
        List<Square> squaresToCheck;

        //Knight can capture
        PossibleStandardKnightMoves knightMoves = new PossibleStandardKnightMoves(square, board);
        squaresToCheck = knightMoves.getPossibleSquares();
        for (Square squareToCheck : squaresToCheck) {
            if (squareToCheck.getPiece() instanceof Knight) {
                return true;
            }
        }

        //Rook can capture
        PossibleStandardRookMoves rookMoves = new PossibleStandardRookMoves(square, board);
        squaresToCheck = rookMoves.getPossibleSquares();
        for (Square squareToCheck : squaresToCheck) {
            if (squareToCheck.getPiece() instanceof Rook || squareToCheck.getPiece() instanceof Queen) {
                return true;
            }
        }

        //Bishop can capture
        PossibleStandardBishopMoves bishopMoves = new PossibleStandardBishopMoves(square, board);
        squaresToCheck = bishopMoves.getPossibleSquares();
        for (Square squareToCheck : squaresToCheck) {
            if (squareToCheck.getPiece() instanceof Bishop || squareToCheck.getPiece() instanceof Queen) {
                return true;
            }
        }

        //Pawn can capture
        PossibleStandardPawnMoves pawnMoves = new PossibleStandardPawnMoves(square, board, moves);
        squaresToCheck = pawnMoves.possibleCaptureMoves();
        for (Square squareToCheck : squaresToCheck) {
            if (squareToCheck.getPiece() instanceof Pawn) {
                return true;
            }
        }

        return false;
    }

    private boolean isInBoundsX(int x) {
        return x >= 0 && x < getWidth();
    }

    private boolean isInBoundsY(int y) {
        return y >= 0 && y < getHeight();
    }
}