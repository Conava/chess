package ptp.core.logic.ruleset.possibleStartPositions;

import ptp.core.data.Square;
import ptp.core.data.pieces.*;
import ptp.core.data.player.Player;

public class PossibleStandardPosition {

    private Player player1;
    private Player player2;


    /**
     * Initiates the standard position.
     *
     * @param player1 Player with the white pieces
     * @param player2 Player with the black pieces
     */
    public PossibleStandardPosition(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
    }

    /**
     * Returns the start position
     *
     * @return Double array of the board. Starting with y as the first position.
     */
    public Square[][] getStartBoard() {
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
}
