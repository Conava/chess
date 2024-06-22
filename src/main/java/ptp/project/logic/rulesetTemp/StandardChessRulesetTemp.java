package ptp.project.logic.rulesetTemp;

import ptp.project.exceptions.IsCheckException;
import ptp.project.logic.*;
import ptp.project.logic.movesTemp.MoveTemp;
import ptp.project.logic.piecesTemp.*;

//import java.lang.reflect.Array;
import java.util.List;
import java.util.ArrayList;

public class StandardChessRulesetTemp implements RulesetTemp {

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
    public SquareTemp[][] getStartBoard(PlayerTemp playerTemp1, PlayerTemp playerTemp2) {
        SquareTemp[][] startBoard = new SquareTemp[8][8];
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                startBoard[x][y] = new SquareTemp(y, x);
            }
        }

        startBoard[0][0].setPiece(new Rook(playerTemp1));
        startBoard[1][0].setPiece(new Knight(playerTemp1));
        startBoard[2][0].setPiece(new Bishop(playerTemp1));
        startBoard[3][0].setPiece(new Queen(playerTemp1));
        startBoard[4][0].setPiece(new King(playerTemp1));
        startBoard[5][0].setPiece(new Bishop(playerTemp1));
        startBoard[6][0].setPiece(new Knight(playerTemp1));
        startBoard[7][0].setPiece(new Rook(playerTemp1));

        for (int x = 0; x < 8; x++) {
            startBoard[x][1].setPiece(new Pawn(playerTemp1));
        }

        startBoard[0][7].setPiece(new Rook(playerTemp2));
        startBoard[1][7].setPiece(new Knight(playerTemp2));
        startBoard[2][7].setPiece(new Bishop(playerTemp2));
        startBoard[3][7].setPiece(new Queen(playerTemp2));
        startBoard[4][7].setPiece(new King(playerTemp2));
        startBoard[5][7].setPiece(new Bishop(playerTemp2));
        startBoard[6][7].setPiece(new Knight(playerTemp2));
        startBoard[7][7].setPiece(new Rook(playerTemp2));

        for (int x = 0; x < 8; x++) {
            startBoard[x][6].setPiece(new Pawn(playerTemp2));
        }

        return startBoard;
    }


    /**
     * Provides a list of legal moves.
     * @param square Only moves from this square are shown
     * @param boardTemp Current board
     * @param moveTemps List of moves already played in-game
     * @param playerTemp1 Player to move
     * @param playerTemp2 Player opponent
     * @return List of LEGAL moves.
     */
    @Override
    public List<MoveTemp> getLegalMoves(SquareTemp square, BoardTemp boardTemp, List<MoveTemp> moveTemps, PlayerTemp playerTemp1, PlayerTemp playerTemp2) {
        List<MoveTemp> legalMoveTemps = new ArrayList<>();
        List<SquareTemp> sudoLegalSquareTemps;
        List<MoveTemp> sudoLegalMoveTemps = new ArrayList<>();
        try {
            sudoLegalSquareTemps = getSudoLegalSquares(square, boardTemp, moveTemps);
        } catch (IsCheckException e) {
            System.out.println(e.getMessage());
            return null;
        }
        for (SquareTemp squareTemp : sudoLegalSquareTemps) {
            sudoLegalMoveTemps.add(new MoveTemp(square, squareTemp));
        }
        for (MoveTemp moveTemp : sudoLegalMoveTemps) {
            if (isMoveLegal(moveTemp, boardTemp, moveTemps, playerTemp2)) {
                legalMoveTemps.add(moveTemp);
            }
        }
        return legalMoveTemps;
    }

    /**
     * Checks if given move is legal.
     * @param moveTemp Move to check.
     * @param board Current board. Does not get changed.
     * @param moveTemps List of moves already played in-game
     * @param playerTemp2 Player opponent
     * @return Is the move legal?
     */
    private boolean isMoveLegal(MoveTemp moveTemp, BoardTemp board, List<MoveTemp> moveTemps, PlayerTemp playerTemp2) {
        BoardTemp boardTemp = board.getCopy();
        boardTemp.executeMove(moveTemp);
        try {
            getAllSudoLegalMoves(board, playerTemp2, moveTemps);
        } catch (IsCheckException e) {
            return false;
        }
        return true;
    }

    public List<SquareTemp> getSudoLegalSquares(SquareTemp squareTemp, BoardTemp boardTemp, List<MoveTemp> moveTemps) throws IsCheckException {
        List<SquareTemp> legalMoves;
        if (squareTemp.getPiece() instanceof Rook) {
            legalMoves = getLegalSquaresRook(squareTemp, boardTemp);
        } else if (squareTemp.getPiece() instanceof Knight) {
            legalMoves = getLegalSquaresKnight(squareTemp, boardTemp);
        } else if (squareTemp.getPiece() instanceof Bishop) {
            legalMoves = getLegalSquaresBishop(squareTemp, boardTemp);
        } else if (squareTemp.getPiece() instanceof Queen) {
            legalMoves = getLegalSquaresBishop(squareTemp, boardTemp);
            legalMoves.addAll(getLegalSquaresRook(squareTemp, boardTemp));
        } else if (squareTemp.getPiece() instanceof King) {
            legalMoves = getLegalSquaresKing(squareTemp, boardTemp);
        } else if (squareTemp.getPiece() instanceof Pawn) {
            legalMoves = getLegalSquaresPawn(squareTemp, boardTemp, moveTemps);
        } else { //space is empty
            return null;
        }
        //@TODO: Check
        //@TODO TTests
        return legalMoves;
    }

    /**
     * Returns legal moves for the rook.
     */
    private List<SquareTemp> getLegalSquaresRook(SquareTemp squareTemp, BoardTemp boardTemp) throws IsCheckException {
        PlayerTemp owner = squareTemp.isOccupiedBy();
        List<SquareTemp> legalMoves = new ArrayList<>();
        SquareTemp possibleSquareTemp;
        //checks column down
        for (int y = squareTemp.getY(); y >= 0; y--) {
            possibleSquareTemp = boardTemp.getSquare(y, squareTemp.getX());
            if (possibleSquareTemp.isOccupiedBy() == null || isCapture(possibleSquareTemp, owner)) {
                legalMoves.add(possibleSquareTemp);
                if (isCapturePiece(possibleSquareTemp, owner) != null &&
                        isCapturePiece(possibleSquareTemp, owner) instanceof King) {
                    throw new IsCheckException(squareTemp);
                }
            } else break;
        }
        //checks column up
        for (int y = squareTemp.getY(); y < 8; y++) {
            possibleSquareTemp = boardTemp.getSquare(y, squareTemp.getX());
            if (possibleSquareTemp.isOccupiedBy() == null || isCapture(possibleSquareTemp, owner)) {
                legalMoves.add(possibleSquareTemp);
                if (isCapturePiece(possibleSquareTemp, owner) != null &&
                        isCapturePiece(possibleSquareTemp, owner) instanceof King) {
                    throw new IsCheckException(squareTemp);
                }
            } else break;
        }
        //checks row left
        for (int x = squareTemp.getX(); x >= 0; x--) {
            possibleSquareTemp = boardTemp.getSquare(squareTemp.getY(), x);
            if (possibleSquareTemp.isOccupiedBy() == null || isCapture(possibleSquareTemp, owner)) {
                legalMoves.add(possibleSquareTemp);
                if (isCapturePiece(possibleSquareTemp, owner) != null &&
                        isCapturePiece(possibleSquareTemp, owner) instanceof King) {
                    throw new IsCheckException(squareTemp);
                }
            } else break;
        }
        //checks row right
        for (int x = squareTemp.getX(); x < 8; x++) {
            possibleSquareTemp = boardTemp.getSquare(squareTemp.getY(), x);
            if (possibleSquareTemp.isOccupiedBy() == null || isCapture(possibleSquareTemp, owner)) {
                legalMoves.add(possibleSquareTemp);
                if (isCapturePiece(possibleSquareTemp, owner) != null &&
                        isCapturePiece(possibleSquareTemp, owner) instanceof King) {
                    throw new IsCheckException(squareTemp);
                }
            } else break;
        }
        return legalMoves;
    }

    /**
     * Returns legal moves for the bishop.
     */
    private List<SquareTemp> getLegalSquaresBishop(SquareTemp squareTemp, BoardTemp boardTemp) throws IsCheckException {
        PlayerTemp owner = squareTemp.isOccupiedBy();
        List<SquareTemp> legalMoves = new ArrayList<>();
        SquareTemp possibleSquareTemp;
        //checks diagonal down left
        for (int i = 0; i < 8; i++) {
            possibleSquareTemp = boardTemp.getSquare(squareTemp.getY() - i, squareTemp.getX() - i);
            if (getLegalSquaresBeamHelp(squareTemp, owner, legalMoves, possibleSquareTemp)) break;
        }
        //checks diagonal up left
        for (int i = 0; i < 8; i++) {
            possibleSquareTemp = boardTemp.getSquare(squareTemp.getY() + i, squareTemp.getX() - i);
            if (getLegalSquaresBeamHelp(squareTemp, owner, legalMoves, possibleSquareTemp)) break;
        }
        //checks diagonal down right
        for (int i = 0; i < 8; i++) {
            possibleSquareTemp = boardTemp.getSquare(squareTemp.getY() - i, squareTemp.getX() + i);
            if (getLegalSquaresBeamHelp(squareTemp, owner, legalMoves, possibleSquareTemp)) break;
        }
        //checks diagonal down left
        for (int i = 0; i < 8; i++) {
            possibleSquareTemp = boardTemp.getSquare(squareTemp.getY() + i, squareTemp.getX() + i);
            if (getLegalSquaresBeamHelp(squareTemp, owner, legalMoves, possibleSquareTemp)) break;
        }

        return legalMoves;
    }

    /**
     * Returns legal moves for the knight.
     */
    private List<SquareTemp> getLegalSquaresKnight(SquareTemp squareTemp, BoardTemp boardTemp) throws IsCheckException {
        PlayerTemp owner = squareTemp.isOccupiedBy();
        List<SquareTemp> legalMoves = new ArrayList<>();

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

        tryArrayMoves(squareTemp, boardTemp, owner, legalMoves, arrY, arrX);

        return legalMoves;
    }

    /**
     * Returns legal moves for the king.
     */
    private List<SquareTemp> getLegalSquaresKing(SquareTemp squareTemp, BoardTemp boardTemp) throws IsCheckException {
        PlayerTemp owner = squareTemp.isOccupiedBy();
        List<SquareTemp> legalMoves = new ArrayList<>();

        int[] arrY = {-1, 0, +1, +1, +1, 0, -1, -1};
        int[] arrX = {+1, +1, +1, 0, -1, -1, -1, 0};

        tryArrayMoves(squareTemp, boardTemp, owner, legalMoves, arrY, arrX);

        if (squareTemp.getPiece() instanceof King king && king.getHasMoved()) {
            if (canCastle(squareTemp, boardTemp) == 1 || canCastle(squareTemp, boardTemp) == 3) {
                legalMoves.add(boardTemp.getSquare(6, squareTemp.getX()));
            }
            if (canCastle(squareTemp, boardTemp) == 2 || canCastle(squareTemp, boardTemp) == 3) {
                legalMoves.add(boardTemp.getSquare(1, squareTemp.getX()));
            }
        }

        return legalMoves;
    }

    /**
     * Returns legal moves for the pawn.
     */
    private List<SquareTemp> getLegalSquaresPawn(SquareTemp squareTemp, BoardTemp boardTemp, List<MoveTemp> moveTemps) throws IsCheckException {
        PlayerTemp owner = squareTemp.isOccupiedBy();
        List<SquareTemp> legalMoves = new ArrayList<>();
        SquareTemp possibleSquareTemp;
        int direction;

        if (owner.getColor().equals("white")) {
            direction = 1;
        } else {
            direction = -1;
        }
        //move 1 square
        possibleSquareTemp = boardTemp.getSquare(squareTemp.getY(), squareTemp.getX() + direction);
        if (possibleSquareTemp.isOccupiedBy() == null) {
            legalMoves.add(possibleSquareTemp);
            if (isCapturePiece(possibleSquareTemp, owner) != null &&
                    isCapturePiece(possibleSquareTemp, owner) instanceof King) {
                throw new IsCheckException(squareTemp);
            }
        }
        //taking left
        possibleSquareTemp = boardTemp.getSquare(squareTemp.getY() - 1, squareTemp.getX() + direction);
        if (isCapture(possibleSquareTemp, owner)) {
            legalMoves.add(possibleSquareTemp);
            if (isCapturePiece(possibleSquareTemp, owner) != null &&
                    isCapturePiece(possibleSquareTemp, owner) instanceof King) {
                throw new IsCheckException(squareTemp);
            }
        }
        //taking right
        possibleSquareTemp = boardTemp.getSquare(squareTemp.getY() + 1, squareTemp.getX() + direction);
        if (isCapture(possibleSquareTemp, owner)) {
            legalMoves.add(possibleSquareTemp);
            if (isCapturePiece(possibleSquareTemp, owner) != null &&
                    isCapturePiece(possibleSquareTemp, owner) instanceof King) {
                throw new IsCheckException(squareTemp);
            }
        }
        //moving 2 squares at the start
        if (isOnRank(squareTemp, owner, 1) && boardTemp.getSquare(squareTemp.getY(), squareTemp.getX() + direction).isOccupiedBy() == null
                && boardTemp.getSquare(squareTemp.getY(), squareTemp.getX() + 2 * direction).isOccupiedBy() == null) {
            legalMoves.add(boardTemp.getSquare(squareTemp.getY() + 1, squareTemp.getX() + 2 * direction));
        }
        //en passant left
        possibleSquareTemp = boardTemp.getSquare(squareTemp.getY() - 1, squareTemp.getX());
        if (isOnRank(squareTemp, owner, 5) && isValidSquare(possibleSquareTemp)
                && possibleSquareTemp.getPiece() != null && possibleSquareTemp.getPiece() instanceof Pawn pawn) {
            if (pawn.hasMoveJustMovedTwoSquares(moveTemps)){
                legalMoves.add(boardTemp.getSquare(squareTemp.getY(), squareTemp.getX() + direction));
            }
        }
        //en passant right
        possibleSquareTemp = boardTemp.getSquare(squareTemp.getY() + 1, squareTemp.getX());
        if (isOnRank(squareTemp, owner, 5) && isValidSquare(possibleSquareTemp)
                && possibleSquareTemp.getPiece() != null && possibleSquareTemp.getPiece() instanceof Pawn pawn) {
            if (pawn.hasMoveJustMovedTwoSquares(moveTemps)){
                legalMoves.add(boardTemp.getSquare(squareTemp.getY(), squareTemp.getX() + direction));
            }
        }

        return legalMoves;
    }

    public boolean isValidSquare(SquareTemp squareTemp) {
        if (squareTemp == null) {
            return false;
        }
        return squareTemp.getY() >= 0 && squareTemp.getY() <= 7 &&
                squareTemp.getX() >= 0 && squareTemp.getX() <= 7;
    }

    /**
     * @param squareTemp Square where to move to
     * @param playerTemp Owner of the capturing peace
     * @return If the move is a possible capture
     */
    private boolean isCapture(SquareTemp squareTemp, PlayerTemp playerTemp) {
        return isValidSquare(squareTemp) && !(squareTemp.isOccupiedBy() == null) && !squareTemp.isOccupiedBy().equals(playerTemp);
    }

    /**
     *
     * @param squareTemp Square where to capture.
     * @param playerTemp Owner of the capturing piece.
     * @return null if not a capture
     *         #Piece if there is a piece.
     */
    private PieceTemp isCapturePiece(SquareTemp squareTemp, PlayerTemp playerTemp) {
        if (isCapture(squareTemp, playerTemp)) {
            return squareTemp.getPiece();
        }
        return null;
    }

    /**
     * How far a piece is base on Player baseline.
     * @param squareTemp The square the piece is on.
     * @param playerTemp The player owning the piece.
     * @param isRank The rank the piece might be on counted from the baseline
     * @return if the rank of the piece on the square counted from baseline and the rank match.
     */
    private boolean isOnRank(SquareTemp squareTemp, PlayerTemp playerTemp, int isRank) {
        int rank = -1; // there should be no piece on rank -1
        if (playerTemp.getColor().equals("white")) {
            rank = squareTemp.getX();
        } else {
            rank = 7 - squareTemp.getX();
        }
        return isRank == rank;
    }

    /**
     * Checks if move is a promotion.
     * Legacy
     * @param squareTemp Square where the piece moves to
     * @return ?isPromotion
     */
    public boolean isPromotion(SquareTemp squareTemp) {
        return squareTemp.getPiece() instanceof Pawn && squareTemp.getX() == 0 ||
                squareTemp.getPiece() instanceof Pawn && squareTemp.getX() == 7;
    }

    /**
     * @param boardTemp  Board, where the check should be checked
     * @param playerTemp Player who can move
     * @return ?isCheck
     */
    @Override
    public boolean isCheck(BoardTemp boardTemp, PlayerTemp playerTemp, List<MoveTemp> moveTemps) { //todo is redundant
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                try {
                    getSudoLegalSquares(boardTemp.getSquare(y, x), boardTemp, moveTemps);
                } catch (IsCheckException e) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks what ways one can castle.
     * @param squareTemp The square the King should be on.
     * @param boardTemp The board the castling should happen.
     * @return 0 for no castle
     * 1 for only 0-0
     * 2 for only 0-0-0
     * 3 for both ways
     */
    private int canCastle(SquareTemp squareTemp, BoardTemp boardTemp) {
        int canCastle = 0;
        if (squareTemp.getPiece() != null && squareTemp.getPiece() instanceof King king) {
            if (!king.getHasMoved()) {
                if (boardTemp.getSquare(7, squareTemp.getX()).getPiece() != null
                        && boardTemp.getSquare(7, squareTemp.getX()).getPiece() instanceof Rook rook) {
                    if (rook.getHasNotMoved() && boardTemp.getSquare(6, squareTemp.getX()).isEmpty()
                            && boardTemp.getSquare(5, squareTemp.getX()).isEmpty()) {
                        canCastle++;
                    }
                }
                if (boardTemp.getSquare(0, squareTemp.getX()).getPiece() != null
                        && boardTemp.getSquare(0, squareTemp.getX()).getPiece() instanceof Rook rook) {
                    if (rook.getHasNotMoved() && boardTemp.getSquare(1, squareTemp.getX()).isEmpty()
                            && boardTemp.getSquare(2, squareTemp.getX()).isEmpty()
                            && boardTemp.getSquare(3, squareTemp.getX()).isEmpty()) {
                        canCastle += 2;
                    }
                }
            }
        }
        return canCastle;
    }

    private boolean getLegalSquaresBeamHelp(SquareTemp squareTemp, PlayerTemp owner, List<SquareTemp> legalMoves, SquareTemp possibleSquareTemp) throws IsCheckException {
        if (isValidSquare(possibleSquareTemp) && possibleSquareTemp.isOccupiedBy() == null ||
                isCapture(possibleSquareTemp, owner)) {
            legalMoves.add(possibleSquareTemp);
            if (isCapturePiece(possibleSquareTemp, owner) != null &&
                    isCapturePiece(possibleSquareTemp, owner) instanceof King) {
                throw new IsCheckException(squareTemp);
            }
        } else return true;
        return false;
    }

    private void tryArrayMoves(SquareTemp squareTemp, BoardTemp boardTemp, PlayerTemp owner, List<SquareTemp> legalMoves, int[] arrY, int[] arrX) throws IsCheckException {
        for (int i = 0; i < 8; i++) {
            SquareTemp possibleSquareTemp = boardTemp.getSquare(squareTemp.getY() + arrY[i], squareTemp.getX() + arrX[i]);
            if (isValidSquare(possibleSquareTemp) && possibleSquareTemp.isOccupiedBy() == null ||
                    isCapture(possibleSquareTemp, owner)) {
                legalMoves.add(possibleSquareTemp);
                if (isCapturePiece(possibleSquareTemp, owner) != null &&
                        isCapturePiece(possibleSquareTemp, owner) instanceof King) {
                    throw new IsCheckException(squareTemp);
                }
            }
        }
    }

    private List<MoveTemp> getAllSudoLegalMoves(BoardTemp boardTemp, PlayerTemp playerTemp, List<MoveTemp> moveTemps) throws IsCheckException {
        List<MoveTemp> legalMoveTemps = new ArrayList<>();
        List<SquareTemp> squaresWithPieces = boardTemp.getPieces(playerTemp);
            for (SquareTemp squareTemp : squaresWithPieces) {
                List<SquareTemp> squareTemps = getSudoLegalSquares(squareTemp, boardTemp, moveTemps);
                for (SquareTemp squareTemp2 : squareTemps) {
                    legalMoveTemps.add(new MoveTemp(squareTemp, squareTemp2));
                }
            }
        return legalMoveTemps;
    }

    private boolean verifyMove() {
        return false;
    }

    @Override
    public boolean verifyMove(MoveTemp moveTemp) {
        return false;
    }

    @Override
    public boolean verifyMove(SquareTemp newPosition, PieceTemp pieceTemp) {
        return false;
    }

    @Override
    public MoveTemp hasEnforcedMove(PlayerTemp playerTemp) {
        return null;
    }
}