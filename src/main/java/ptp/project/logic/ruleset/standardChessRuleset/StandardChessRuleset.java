package ptp.project.logic.ruleset.standardChessRuleset;

import ptp.project.data.Player;
import ptp.project.data.Square;
import ptp.project.data.enums.PlayerColor;
import ptp.project.data.pieces.*;
import ptp.project.exceptions.IsCheckException;
import ptp.project.data.board.Board;
import ptp.project.logic.moves.Move;
import ptp.project.logic.ruleset.Ruleset;

//import java.lang.reflect.Array;
import java.util.List;
import java.util.ArrayList;

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
        List<Move> legalMoves = new ArrayList<>();
        List<Square> sudoLegalSquares;
        List<Move> sudoLegalMoves = new ArrayList<>();
        try {
            sudoLegalSquares = getSudoLegalSquares(square, board, moves);
        } catch (IsCheckException e) {
            System.out.println(e.getMessage()); //todo logger
            return null;
        }
        for (Square squareTemp : sudoLegalSquares) {
            sudoLegalMoves.add(new Move(square, squareTemp));
        }
        for (Move move : sudoLegalMoves) {
            if (isMoveLegal(move, board, moves, player2)) {
                legalMoves.add(move);
            }
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
        List<Square> legalSquares = new ArrayList<>();
        //legalSquares.add(new Square(4,4));
        /* todo: wenn man die legalSquares auf 4,4 setzt, giben nur 4 figuren dieses feld an (fixed)
        List<Move> legalMoves = getLegalMoves(square, board, moves, player1, player2);
        for (Move legalMove : legalMoves) {
            legalSquares.add(legalMove.getEnd());
        }
        System.out.println("#LegaleZüge" + legalSquares.size());
        */
        try {
            legalSquares = getSudoLegalSquares(square, board, moves);
        } catch (IsCheckException e) {
            return legalSquares;
        }
        return legalSquares;
    }

    /**
     * Checks if given move is legal.
     *
     * @param move    Move to check.
     * @param board   Current board. Does not get changed.
     * @param moves   List of moves already played in-game
     * @param player2 Player opponent
     * @return Is the move legal?
     */
    private boolean isMoveLegal(Move move, Board board, List<Move> moves, Player player2) {
        Board boardTemp = board.getCopy();
        boardTemp.executeMove(move);
        try {
            getAllSudoLegalMoves(board, player2, moves);
        } catch (IsCheckException e) {
            return false;
        }
        return true;
    }

    private List<Square> getSudoLegalSquares(Square square, Board board, List<Move> moves) throws IsCheckException {
        List<Square> legalMoves;
        if (square.getPiece().getClass().equals(Rook.class)) {
            legalMoves = getLegalSquaresRook(square, board);
        } else if (square.getPiece().getClass().equals(Knight.class)) {
            legalMoves = getLegalSquaresKnight(square, board);
        } else if (square.getPiece().getClass().equals(Bishop.class)) {
            legalMoves = getLegalSquaresBishop(square, board);
        } else if (square.getPiece().getClass().equals(Queen.class)) {
            legalMoves = getLegalSquaresBishop(square, board);
            legalMoves.addAll(getLegalSquaresRook(square, board));
        } else if (square.getPiece().getClass().equals(King.class)) {
            legalMoves = getLegalSquaresKing(square, board);
        } else if (square.getPiece().getClass().equals(Pawn.class)) {
            legalMoves = getLegalSquaresPawn(square, board, moves);
        } else { //space is empty
            legalMoves = new ArrayList<>();
        }
        //@TODO: Check
        //@TODO TTests
        return legalMoves;
    }

    /**
     * Returns legal moves for the rook.
     */
    private List<Square> getLegalSquaresRook(Square square, Board board) throws IsCheckException {
        System.out.println("CHecks rook moves");
        Player owner = square.isOccupiedBy();
        List<Square> legalMoves = new ArrayList<>();
        Square possibleSquare;
        //checks column down
        for (int y = square.getY() - 1; y >= 0; y--) {
            if (isInBoundsY(y) && isInBoundsX(square.getX())) {
                possibleSquare = board.getSquare(y, square.getX());
                if (possibleSquare.isEmpty()) {
                    legalMoves.add(possibleSquare);
                } else if (isCapture(possibleSquare, owner)) {
                    legalMoves.add(possibleSquare);
                    if (isCapturePiece(possibleSquare, owner) != null &&
                            isCapturePiece(possibleSquare, owner) instanceof King) {
                        throw new IsCheckException(square);
                    }
                    break;
                } else break;
            }
        }
        //checks column up
        for (int y = square.getY() + 1; y < 8; y++) {
            if (isInBoundsY(y) && isInBoundsX(square.getX())) {
                possibleSquare = board.getSquare(y, square.getX());
                if (possibleSquare.isEmpty()) {
                    legalMoves.add(possibleSquare);
                } else if (isCapture(possibleSquare, owner)) {
                    legalMoves.add(possibleSquare);
                    if (isCapturePiece(possibleSquare, owner) != null &&
                            isCapturePiece(possibleSquare, owner) instanceof King) {
                        throw new IsCheckException(square);
                    }
                    break;
                } else break;
            }
        }
        //checks row left
        for (int x = square.getX() - 1; x >= 0; x--) {
            if (isInBoundsY(square.getY()) && isInBoundsX(x)) {
                possibleSquare = board.getSquare(square.getY(), x);
                if (possibleSquare.isEmpty()) {
                    legalMoves.add(possibleSquare);
                } else if (isCapture(possibleSquare, owner)) {
                    legalMoves.add(possibleSquare);
                    if (isCapturePiece(possibleSquare, owner) != null &&
                            isCapturePiece(possibleSquare, owner) instanceof King) {
                        throw new IsCheckException(square);
                    }
                    break;
                } else break;
            }
        }
        //checks row right
        for (int x = square.getX() + 1; x < 8; x++) {
            if (isInBoundsY(square.getY()) && isInBoundsX(x)) {
                possibleSquare = board.getSquare(square.getY(), x);
                if (possibleSquare.isEmpty()) {
                    legalMoves.add(possibleSquare);
                } else if (isCapture(possibleSquare, owner)) {
                    legalMoves.add(possibleSquare);
                    if (isCapturePiece(possibleSquare, owner) != null &&
                            isCapturePiece(possibleSquare, owner) instanceof King) {
                        throw new IsCheckException(square);
                    }
                    break;
                } else break;
            }
        }
        return legalMoves;
    }

    /**
     * Returns legal moves for the bishop.
     */
    private List<Square> getLegalSquaresBishop(Square square, Board board) throws IsCheckException {
        Player owner = square.isOccupiedBy();
        List<Square> legalMoves = new ArrayList<>();
        Square possibleSquare;
        //checks diagonal down left
        for (int i = 1; i < 8; i++) {
            if (isInBoundsY(square.getY() - i) && isInBoundsX(square.getX() - i)) {
                possibleSquare = board.getSquare(square.getY() - i, square.getX() - i);
                if (possibleSquare.isEmpty()) {
                    legalMoves.add(possibleSquare);
                } else if (isCapture(possibleSquare, owner)) {
                    legalMoves.add(possibleSquare);
                    if (isCapturePiece(possibleSquare, owner) != null &&
                            isCapturePiece(possibleSquare, owner) instanceof King) {
                        throw new IsCheckException(square);
                    }
                    break;
                } else break;
            }
        }
        //checks diagonal up left
        for (int i = 1; i < 8; i++) {
            if (isInBoundsY(square.getY() + i) && isInBoundsX(square.getX() - i)) {
                possibleSquare = board.getSquare(square.getY() + i, square.getX() - i);
                if (possibleSquare.isEmpty()) {
                    legalMoves.add(possibleSquare);
                } else if (isCapture(possibleSquare, owner)) {
                    legalMoves.add(possibleSquare);
                    if (isCapturePiece(possibleSquare, owner) != null &&
                            isCapturePiece(possibleSquare, owner) instanceof King) {
                        throw new IsCheckException(square);
                    }
                    break;
                } else break;
            }
        }
        //checks diagonal down right
        for (int i = 1; i < 8; i++) {
            if (isInBoundsY(square.getY() - i) && isInBoundsX(square.getX() + i)) {
                possibleSquare = board.getSquare(square.getY() - i, square.getX() + i);
                if (possibleSquare.isEmpty()) {
                    legalMoves.add(possibleSquare);
                } else if (isCapture(possibleSquare, owner)) {
                    legalMoves.add(possibleSquare);
                    if (isCapturePiece(possibleSquare, owner) != null &&
                            isCapturePiece(possibleSquare, owner) instanceof King) {
                        throw new IsCheckException(square);
                    }
                    break;
                } else break;
            }
        }
        //checks diagonal down left
        for (int i = 1; i < 8; i++) {
            if (isInBoundsY(square.getY() + i) && isInBoundsX(square.getX() + i)) {
                possibleSquare = board.getSquare(square.getY() + i, square.getX() + i);
                if (possibleSquare.isEmpty()) {
                    legalMoves.add(possibleSquare);
                } else if (isCapture(possibleSquare, owner)) {
                    legalMoves.add(possibleSquare);
                    if (isCapturePiece(possibleSquare, owner) != null &&
                            isCapturePiece(possibleSquare, owner) instanceof King) {
                        throw new IsCheckException(square);
                    }
                    break;
                } else break;
            }
        }

        return legalMoves;
    }

    /**
     * Returns legal moves for the knight.
     */
    private List<Square> getLegalSquaresKnight(Square square, Board board) throws IsCheckException {
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

        tryArrayMoves(square, board, owner, legalMoves, arrY, arrX);

        return legalMoves;
    }

    /**
     * Returns legal moves for the king.
     */
    private List<Square> getLegalSquaresKing(Square square, Board board) throws IsCheckException {
        Player owner = square.isOccupiedBy();
        List<Square> legalMoves = new ArrayList<>();

        int[] arrY = {-1, 0, +1, +1, +1, 0, -1, -1};
        int[] arrX = {+1, +1, +1, 0, -1, -1, -1, 0};

        tryArrayMoves(square, board, owner, legalMoves, arrY, arrX);

        if (square.getPiece() instanceof King king && !king.getHasMoved()) {
            if (canCastle(square, board) == 1 || canCastle(square, board) == 3) {
                legalMoves.add(board.getSquare(6, square.getX()));
            }
            if (canCastle(square, board) == 2 || canCastle(square, board) == 3) {
                legalMoves.add(board.getSquare(1, square.getX()));
            }
        }

        return legalMoves;
    }

    /**
     * Returns legal moves for the pawn.
     */
    private List<Square> getLegalSquaresPawn(Square square, Board board, List<Move> moves) throws IsCheckException {
        Player owner = square.isOccupiedBy();
        List<Square> legalMoves = new ArrayList<>();
        Square possibleSquare;
        int direction;

        if (owner.color().equals(PlayerColor.WHITE)) {
            direction = 1;
        } else {
            direction = -1;
        }
        //move 1 square
        if (isInBoundsY(square.getY() + direction) && isInBoundsX(square.getX())) {
            possibleSquare = board.getSquare(square.getY() + direction, square.getX());
            if (possibleSquare.isOccupiedBy() == null) {
                legalMoves.add(possibleSquare);
                if (isCapturePiece(possibleSquare, owner) != null &&
                        isCapturePiece(possibleSquare, owner) instanceof King) {
                    throw new IsCheckException(square);
                }
            }
        }
        //taking left
        if (isInBoundsY(square.getY() + direction) && isInBoundsX(square.getX() - 1)) {
            possibleSquare = board.getSquare(square.getY() + direction, square.getX() - 1);
            if (isCapture(possibleSquare, owner)) {
                legalMoves.add(possibleSquare);
                if (isCapturePiece(possibleSquare, owner) != null &&
                        isCapturePiece(possibleSquare, owner) instanceof King) {
                    throw new IsCheckException(square);
                }
            }
        }
        //taking right
        if (isInBoundsY(square.getY() + direction) && isInBoundsX(square.getX() + 1)) {
            possibleSquare = board.getSquare(square.getY() + direction, square.getX() + 1);
            if (isCapture(possibleSquare, owner)) {
                legalMoves.add(possibleSquare);
                if (isCapturePiece(possibleSquare, owner) != null &&
                        isCapturePiece(possibleSquare, owner) instanceof King) {
                    throw new IsCheckException(square);
                }
            }
        }

        //moving 2 squares at the start
        if (isInBoundsY(square.getY() + direction) && isInBoundsX(square.getX())) {
            if (isOnRank(square, owner, 1) && board.getSquare(square.getY() + direction, square.getX()).isOccupiedBy() == null
                    && board.getSquare(square.getY() + 2 * direction, square.getX()).isOccupiedBy() == null) {
                legalMoves.add(board.getSquare(square.getY() + 2 * direction, square.getX()));
            }
        }
        //en passant left
        if (isInBoundsY(square.getY() - 1) && isInBoundsX(square.getX())) {
            possibleSquare = board.getSquare(square.getY() - 1, square.getX());
            if (isOnRank(square, owner, 5) && isValidSquare(possibleSquare)
                    && possibleSquare.getPiece() != null && possibleSquare.getPiece() instanceof Pawn pawn) {
                if (pawn.hasMoveJustMovedTwoSquares(moves)) {
                    legalMoves.add(board.getSquare(square.getY(), square.getX() + direction));
                }
            }
        }
        //en passant right
        if (isInBoundsY(square.getY() + 1) && isInBoundsX(square.getX())) {
            possibleSquare = board.getSquare(square.getY() + 1, square.getX());
            if (isOnRank(square, owner, 5) && isValidSquare(possibleSquare)
                    && possibleSquare.getPiece() != null && possibleSquare.getPiece() instanceof Pawn pawn) {
                if (pawn.hasMoveJustMovedTwoSquares(moves)) {
                    legalMoves.add(board.getSquare(square.getY(), square.getX() + direction));
                }
            }
        }

        return legalMoves;
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
     * @param square Square where to move to
     * @param player Owner of the capturing peace
     * @return If the move is a possible capture
     */
    private boolean isCapture(Square square, Player player) {
        return isValidSquare(square) && !(square.isOccupiedBy() == null) && !square.isOccupiedBy().equals(player);
    }

    /**
     * @param square Square where to capture.
     * @param player Owner of the capturing piece.
     * @return null if not a capture
     * #Piece if there is a piece.
     */
    private Piece isCapturePiece(Square square, Player player) {
        if (isCapture(square, player)) {
            return square.getPiece();
        }
        return null;
    }

    /**
     * How far a piece is base on Player baseline.
     *
     * @param square The square the piece is on.
     * @param player The player owning the piece.
     * @param isRank The rank the piece might be on counted from the baseline
     * @return if the rank of the piece on the square counted from baseline and the rank match.
     */
    private boolean isOnRank(Square square, Player player, int isRank) {
        int rank = -1; // there should be no piece on rank -1
        if (player.color() == PlayerColor.WHITE) {
            rank = square.getY();
        } else {
            rank = 7 - square.getY();
        }
        return isRank == rank;
    }

    /**
     * Checks if move is a promotion.
     * Legacy
     *
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
    public boolean isCheck(Board board, Player player, List<Move> moves) { //todo is redundant
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                try {
                    getSudoLegalSquares(board.getSquare(y, x), board, moves);
                } catch (IsCheckException e) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks what ways one can castle.
     *
     * @param square The square the King should be on.
     * @param board  The board the castling should happen.
     * @return 0 for no castle
     * 1 for only 0-0
     * 2 for only 0-0-0
     * 3 for both ways
     */
    private int canCastle(Square square, Board board) {
        int canCastle = 0;
        if (square.getPiece() != null && square.getPiece() instanceof King king) {
            if (!king.getHasMoved()) {
                if (board.getSquare(7, square.getX()).getPiece() != null
                        && board.getSquare(7, square.getX()).getPiece() instanceof Rook rook) {
                    if (rook.getHasNotMoved() && board.getSquare(6, square.getX()).isEmpty()
                            && board.getSquare(5, square.getX()).isEmpty()) {
                        canCastle++;
                    }
                }
                if (board.getSquare(0, square.getX()).getPiece() != null
                        && board.getSquare(0, square.getX()).getPiece() instanceof Rook rook) {
                    if (rook.getHasNotMoved() && board.getSquare(1, square.getX()).isEmpty()
                            && board.getSquare(2, square.getX()).isEmpty()
                            && board.getSquare(3, square.getX()).isEmpty()) {
                        canCastle += 2;
                    }
                }
            }
        }
        return canCastle;
    }

    private boolean getLegalSquaresBeamHelp(Square square, Player owner, List<Square> legalMoves, Square possibleSquare) throws IsCheckException {
        if (isValidSquare(possibleSquare) && possibleSquare.isOccupiedBy() == null) {
            legalMoves.add(possibleSquare);
            if (isCapturePiece(possibleSquare, owner) != null &&
                    isCapturePiece(possibleSquare, owner) instanceof King) {
                throw new IsCheckException(square);
            }
            return false;
        } else return true;
    }

    private void tryArrayMoves(Square square, Board board, Player owner, List<Square> legalMoves, int[] arrY, int[] arrX) throws IsCheckException {
        for (int i = 0; i < 8; i++) {
            if ((square.getY() + arrY[i]) < 0 || (square.getY() + arrY[i]) > 7) {
                continue;
            }
            if ((square.getX() + arrX[i]) < 0 || (square.getX() + arrX[i]) > 7) {
                continue;
            }
            Square possibleSquare = board.getSquare(square.getY() + arrY[i], square.getX() + arrX[i]);
            if (isValidSquare(possibleSquare) && possibleSquare.isOccupiedBy() == null ||
                    isCapture(possibleSquare, owner)) {
                legalMoves.add(possibleSquare);
                if (isCapturePiece(possibleSquare, owner) != null &&
                        isCapturePiece(possibleSquare, owner) instanceof King) {
                    throw new IsCheckException(square);
                }
            } else {
            }
        }
    }

    private List<Move> getAllSudoLegalMoves(Board board, Player player, List<Move> moves) throws IsCheckException {
        List<Move> legalMoves = new ArrayList<>();
        List<Square> squaresWithPieces = board.getPieces(player);
        for (Square square : squaresWithPieces) {
            List<Square> squares = getSudoLegalSquares(square, board, moves);
            for (Square square2 : squares) {
                if (square2 == null) {
                    continue;
                }
                legalMoves.add(new Move(square, square2));
            }
        }
        return legalMoves;
    }

    private boolean isInBoundsX(int x) {
        return x >= 0 && x < 8;
    }

    private boolean isInBoundsY(int y) {
        return y >= 0 && y < 8;
    }
}