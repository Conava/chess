package ptp.project.logic.moves;

public class Move {
    private final int squareStart;
    private final int squareEnd;
    private final int piece;
    //only for promotion purposes.
    private int piecePromotion;
    private boolean isPromotion = false;

    /**
     * Initialize a move.
     *
     * @param squareStart The start square of the move.
     * @param squareEnd   The end square of the move.
     */
    public Move(int squareStart, int squareEnd, int piece) {
        this.squareStart = squareStart;
        this.squareEnd = squareEnd;
        this.piece = piece;
    }

    /**
     * Initialize a move (only for promotion)
     *
     * @param squareStart    Square the move starts from.
     * @param squareEnd      Square the move goes to.
     * @param piecePromotion The piece the pawn promotes to.
     */
    public Move(int squareStart, int squareEnd, int piece, int piecePromotion) {
        this.squareStart = squareStart;
        this.squareEnd = squareEnd;
        this.piece = piece;
        this.isPromotion = true;
        this.piecePromotion = piecePromotion;
    }

    /**
     * Gets the start square.
     *
     * @return The square. #Example for how a 8x8 board is numerated: {@link ptp.project.logic.docs}
     */
    public int getSquareStart() {
        return squareStart;
    }

    /**
     * Gets the end square.
     *
     * @return The square. #Example for how a 8x8 board is numerated: {@link ptp.project.logic.docs}
     */
    public int getSquareEnd() {
        return squareEnd;
    }

    /**
     * Gets the moving piece.
     *
     * @return #Entries of values of squares follow this pattern: {@link ptp.project.logic.docs}
     */
    public int getPiece() {
        return piece;
    }

    /**
     * Returns the desired piece to promote to.
     *
     * @return #Entries for values of status of the game: {@link ptp.project.logic.docs}
     */
    public int getTransformationPiece() {
        return piecePromotion;
    }

    /**
     * Whether the move is a promotion move.
     *
     * @return bool whether it is a promotion move.
     */
    public boolean isPromotion() {
        return isPromotion;
    }

    /**
     * Returns a notation of the move.
     *
     * @param col Amount of columns of the board.
     * @return Notation as string.
     */
    public String getMoveNotation(int col) {
        return getPieceAbbr(piece) + getSquareNotation(squareStart, col) + " - " + getSquareNotation(squareEnd, col);
    }

    /**
     * Returns a notation of a square.
     *
     * @param col Amount of columns of the board.
     * @return Notation as a string.
     */
    private String getSquareNotation(int square, int col) {
        String columnNotation;
        String rowNotation;
        columnNotation = getChar(square / col);
        rowNotation = square % col + "";
        return columnNotation + rowNotation;
    }

    /**
     * Gets a char.
     *
     * @param number The number of the char in the alphabet.
     * @return The corresponding char as string.
     */
    private String getChar(int number) {
        return switch (number) {
            case 0 -> "a";
            case 1 -> "b";
            case 2 -> "c";
            case 3 -> "d";
            case 4 -> "e";
            case 5 -> "f";
            case 6 -> "g";
            case 7 -> "h";
            case 8 -> "i";
            case 9 -> "j";
            case 10 -> "k";
            case 11 -> "l";
            case 12 -> "m";
            case 13 -> "n";
            case 14 -> "o";
            case 15 -> "p";
            case 16 -> "q";
            case 17 -> "r";
            case 18 -> "s";
            case 19 -> "t";
            case 20 -> "u";
            case 21 -> "v";
            case 22 -> "w";
            case 23 -> "x";
            case 24 -> "y";
            case 25 -> "z";
            default -> "ERROR";
        };
    }

    /**
     * Gets the abbreviation of the piece.
     *
     * @param piece #Entries of values of squares follow this pattern: {@link ptp.project.logic.docs}
     * @return The corresponding char as a string.
     */
    private String getPieceAbbr(int piece) {
        return switch (piece) {
            case 1, 2 -> "";
            case 3, 4 -> "N";
            case 5, 6 -> "B";
            case 7, 8 -> "R";
            case 9, 10 -> "Q";
            case 11, 12 -> "K";
            default -> "ERROR";
        };
    }
}
