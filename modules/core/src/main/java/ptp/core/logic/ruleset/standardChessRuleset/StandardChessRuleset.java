package ptp.core.logic.ruleset.standardChessRuleset;

import ptp.core.data.player.Player;
import ptp.core.data.Square;
import ptp.core.logic.game.GameState;
import ptp.core.data.player.PlayerColor;
import ptp.core.data.board.Board;
import ptp.core.data.pieces.*;
import ptp.core.logic.moves.Move;
import ptp.core.logic.ruleset.Ruleset;
import ptp.core.logic.ruleset.possibleMoves.*;

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
        Square[][] startBoard = new Square[8][8];
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                startBoard[y][x] = new Square(y, x);
            }
        }

        startBoard[0][0].setPiece(new Rook(player1));
        startBoard[0][1].setPiece(new Knight(player1));
        startBoard[0][2].setPiece(new Bishop(player1));
        startBoard[0][3].setPiece(new Queen(player1));
        startBoard[0][4].setPiece(new King(player1));
        startBoard[0][5].setPiece(new Bishop(player1));
        startBoard[0][6].setPiece(new Knight(player1));
        startBoard[0][7].setPiece(new Rook(player1));

        for (int x = 0; x < 8; x++) {
            startBoard[1][x].setPiece(new Pawn(player1));
        }

        startBoard[7][0].setPiece(new Rook(player2));
        startBoard[7][1].setPiece(new Knight(player2));
        startBoard[7][2].setPiece(new Bishop(player2));
        startBoard[7][3].setPiece(new Queen(player2));
        startBoard[7][4].setPiece(new King(player2));
        startBoard[7][5].setPiece(new Bishop(player2));
        startBoard[7][6].setPiece(new Knight(player2));
        startBoard[7][7].setPiece(new Rook(player2));

        for (int x = 0; x < 8; x++) {
            startBoard[6][x].setPiece(new Pawn(player2));
        }

        return startBoard;
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
     * Provides a list of legal squares.
     *
     * @param square  Only moves from this square are shown
     * @param board   Current board
     * @param moves   List of moves already played in-game
     * @param player1 Player to move
     * @param player2 Player opponent
     * @return List of LEGAL squares.
     */
    @Override
    public List<Square> getLegalSquares(Square square, Board board, List<Move> moves, Player player1, Player player2) {
        return getSudoLegalSquares(square, board, moves);
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
        return x >= 0 && x < 8;
    }

    private boolean isInBoundsY(int y) {
        return y >= 0 && y < 8;
    }

    public GameState getGameState(Board board, List<Move> moves) {
        boolean kingWhiteExists = false;
        boolean kingBlackExists = false;
        Piece tempPiece;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                tempPiece = board.getSquare(x, y).getPiece();
                if (tempPiece == null || !(tempPiece instanceof King)) {
                    continue;
                } else if (tempPiece.getPlayer().color() == PlayerColor.WHITE) {
                    kingWhiteExists = true;
                } else if (tempPiece.getPlayer().color() == PlayerColor.BLACK) {
                    kingBlackExists = true;
                } else {
                    //this should never be reached
                    return GameState.NO_GAME;
                }
            }
        }
        if (kingWhiteExists && kingBlackExists) {
            return GameState.RUNNING;
        } else if (kingWhiteExists) { // no black king
            return GameState.WHITE_WON_BY_CHECKMATE;
        } else if (kingBlackExists) {
            return GameState.BLACK_WON_BY_CHECKMATE;
        } else {
            return GameState.DRAW_BY_INSUFFICIENT_MATERIAL;
        }
    }
}