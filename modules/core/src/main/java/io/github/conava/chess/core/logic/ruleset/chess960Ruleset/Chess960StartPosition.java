package io.github.conava.chess.core.logic.ruleset.chess960Ruleset;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.pieces.*;
import io.github.conava.chess.core.data.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates and encodes Chess960 starting positions using the Scharnagl numbering system.
 *
 * <p>The Scharnagl index (0–959) uniquely identifies each of the 960 valid Fischer Random
 * starting positions. This class provides:
 * <ul>
 *   <li>{@link #generate(Player, Player)} — constrained-random generation</li>
 *   <li>{@link #fromIndex(int, Player, Player)} — deterministic reconstruction from index</li>
 *   <li>{@link #computeIndex(Piece[])} — compute the Scharnagl index of a given back rank</li>
 * </ul>
 *
 * <p>The generation algorithm guarantees by construction:
 * <ul>
 *   <li>Dark-square bishop on an even file (0, 2, 4, or 6)</li>
 *   <li>Light-square bishop on an odd file (1, 3, 5, or 7)</li>
 *   <li>King placed between its two rooks</li>
 * </ul>
 */
public class Chess960StartPosition {

    /**
     * N5 encoding table for knight placement.
     *
     * <p>Maps n4 (0–9) to a pair of positions (0-indexed) among the 5 remaining squares
     * after placing both bishops and the queen. The two positions indicate where the knights go.
     */
    private static final int[][] N5_TABLE = {
            {0, 1}, {0, 2}, {0, 3}, {0, 4},
            {1, 2}, {1, 3}, {1, 4},
            {2, 3}, {2, 4},
            {3, 4}
    };

    private Chess960StartPosition() {
        // utility class — no instances
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Generates a valid Chess960 starting position using constrained-random placement.
     *
     * <p>Algorithm (5 steps):
     * <ol>
     *   <li>Place bishop on a random dark square (files 0, 2, 4, 6)</li>
     *   <li>Place bishop on a random light square (files 1, 3, 5, 7)</li>
     *   <li>Place queen on a random remaining square</li>
     *   <li>Place two knights on two random remaining squares</li>
     *   <li>Fill the three remaining squares left-to-right: Rook, King, Rook</li>
     * </ol>
     * Steps 1–4 guarantee that the king always ends up between its rooks (step 5).
     *
     * @param white the white player
     * @param black the black player
     * @return an 8×8 {@link Square} array representing the starting position
     */
    public static Square[][] generate(Player white, Player black) {
        Random rng = new Random();

        // Build back rank as a list of piece types
        Piece[] whiteBackRank = new Piece[8];

        // Step 1: dark-square bishop (even files: 0, 2, 4, 6)
        int darkBishopFile = rng.nextInt(4) * 2; // 0, 2, 4, or 6
        whiteBackRank[darkBishopFile] = new Bishop(white);

        // Step 2: light-square bishop (odd files: 1, 3, 5, 7)
        int lightBishopFile = rng.nextInt(4) * 2 + 1; // 1, 3, 5, or 7
        whiteBackRank[lightBishopFile] = new Bishop(white);

        // Collect remaining (empty) files after placing bishops
        List<Integer> remaining = new ArrayList<>();
        for (int x = 0; x < 8; x++) {
            if (whiteBackRank[x] == null) remaining.add(x);
        }
        // remaining has 6 files

        // Step 3: queen on a random remaining file
        int queenPos = rng.nextInt(remaining.size());
        whiteBackRank[remaining.remove(queenPos)] = new Queen(white);
        // remaining now has 5 files

        // Step 4: two knights on two random remaining files
        int knight1Pos = rng.nextInt(remaining.size());
        whiteBackRank[remaining.remove(knight1Pos)] = new Knight(white);
        int knight2Pos = rng.nextInt(remaining.size());
        whiteBackRank[remaining.remove(knight2Pos)] = new Knight(white);
        // remaining now has 3 files

        // Step 5: fill Rook, King, Rook left-to-right (remaining is already sorted ascending)
        whiteBackRank[remaining.get(0)] = new Rook(white);
        whiteBackRank[remaining.get(1)] = new King(white);
        whiteBackRank[remaining.get(2)] = new Rook(white);

        return buildBoard(whiteBackRank, white, black);
    }

    /**
     * Reconstructs a Chess960 starting position from a Scharnagl index.
     *
     * <p>The Scharnagl encoding:
     * <ul>
     *   <li>{@code n1 = index % 4} → light-square bishop at file {@code 2*n1 + 1}</li>
     *   <li>{@code n2 = (index / 4) % 4} → dark-square bishop at file {@code 2*n2}</li>
     *   <li>{@code n3 = (index / 16) % 6} → queen at position n3 among 6 remaining files</li>
     *   <li>{@code n4 = index / 96} (0–9) → knight positions via N5 encoding table</li>
     *   <li>Last 3 files: Rook, King, Rook (left to right)</li>
     * </ul>
     *
     * @param index Scharnagl index in [0, 959]
     * @param white the white player
     * @param black the black player
     * @return an 8×8 {@link Square} array representing the starting position
     * @throws IllegalArgumentException if {@code index} is outside [0, 959]
     */
    public static Square[][] fromIndex(int index, Player white, Player black) {
        if (index < 0 || index > 959) {
            throw new IllegalArgumentException(
                    "Scharnagl index must be in [0, 959], got: " + index);
        }

        Piece[] whiteBackRank = new Piece[8];

        // Step 1: light-square bishop (odd files: 1, 3, 5, 7)
        int n1 = index % 4;
        int lightBishopFile = 2 * n1 + 1;
        whiteBackRank[lightBishopFile] = new Bishop(white);

        // Step 2: dark-square bishop (even files: 0, 2, 4, 6)
        int n2 = (index / 4) % 4;
        int darkBishopFile = 2 * n2;
        whiteBackRank[darkBishopFile] = new Bishop(white);

        // Collect remaining 6 files (sorted ascending)
        List<Integer> remaining = new ArrayList<>();
        for (int x = 0; x < 8; x++) {
            if (whiteBackRank[x] == null) remaining.add(x);
        }

        // Step 3: queen at position n3 among the 6 remaining files
        int n3 = (index / 16) % 6;
        whiteBackRank[remaining.remove(n3)] = new Queen(white);
        // remaining now has 5 files

        // Step 4: knights via N5 encoding table
        int n4 = index / 96;
        int[] knightPositions = N5_TABLE[n4];
        // Place knights at the two specified positions (remove higher index first to preserve ordering)
        int kp2 = knightPositions[1];
        int kp1 = knightPositions[0];
        whiteBackRank[remaining.get(kp2)] = new Knight(white);
        whiteBackRank[remaining.get(kp1)] = new Knight(white);
        // Remove from remaining in descending order to avoid index shifting
        remaining.remove(kp2);
        remaining.remove(kp1);
        // remaining now has 3 files

        // Step 5: Rook, King, Rook (left to right)
        whiteBackRank[remaining.get(0)] = new Rook(white);
        whiteBackRank[remaining.get(1)] = new King(white);
        whiteBackRank[remaining.get(2)] = new Rook(white);

        return buildBoard(whiteBackRank, white, black);
    }

    /**
     * Computes the Scharnagl index for a given white back rank.
     *
     * <p>This is the inverse of {@link #fromIndex}. The round-trip
     * {@code computeIndex(fromIndex(i)[0]) == i} holds for all i in [0, 959].
     *
     * @param backRank array of 8 pieces representing white's back rank (index = file)
     * @return the Scharnagl index in [0, 959]
     */
    public static int computeIndex(Piece[] backRank) {
        // Find light-square bishop file (odd)
        int lightBishopFile = -1;
        // Find dark-square bishop file (even)
        int darkBishopFile = -1;
        for (int x = 0; x < 8; x++) {
            if (backRank[x] instanceof Bishop) {
                if (x % 2 == 1) lightBishopFile = x;
                else darkBishopFile = x;
            }
        }

        int n1 = (lightBishopFile - 1) / 2;  // 1->0, 3->1, 5->2, 7->3
        int n2 = darkBishopFile / 2;           // 0->0, 2->1, 4->2, 6->3

        // Build list of remaining files after removing bishop files
        List<Integer> remaining = new ArrayList<>();
        for (int x = 0; x < 8; x++) {
            if (x != lightBishopFile && x != darkBishopFile) remaining.add(x);
        }

        // Find queen position among remaining 6
        int queenFile = -1;
        for (int x = 0; x < 8; x++) {
            if (backRank[x] instanceof Queen) { queenFile = x; break; }
        }
        int n3 = remaining.indexOf(queenFile);
        remaining.remove(Integer.valueOf(queenFile));
        // remaining now has 5 files

        // Find knight files
        List<Integer> knightFiles = new ArrayList<>();
        for (int x = 0; x < 8; x++) {
            if (backRank[x] instanceof Knight) knightFiles.add(x);
        }

        // Find positions of knights within remaining 5-slot list
        int kp1 = remaining.indexOf(knightFiles.get(0));
        int kp2 = remaining.indexOf(knightFiles.get(1));
        // Ensure kp1 < kp2
        if (kp1 > kp2) {
            int tmp = kp1; kp1 = kp2; kp2 = tmp;
        }

        // Find n4 from N5 table
        int n4 = -1;
        for (int i = 0; i < N5_TABLE.length; i++) {
            if (N5_TABLE[i][0] == kp1 && N5_TABLE[i][1] == kp2) {
                n4 = i;
                break;
            }
        }

        return n1 + 4 * n2 + 16 * n3 + 96 * n4;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a complete 8×8 board from white's back rank.
     *
     * <p>White occupies rows 0 (back rank) and 1 (pawns).
     * Black occupies rows 7 (back rank, mirrored) and 6 (pawns).
     * Rows 2–5 are empty.
     */
    private static Square[][] buildBoard(Piece[] whiteBackRank, Player white, Player black) {
        Square[][] board = new Square[8][8];

        // Initialize all squares
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                board[y][x] = new Square(y, x);
            }
        }

        // White back rank (row 0)
        for (int x = 0; x < 8; x++) {
            board[0][x].setPiece(whiteBackRank[x]);
        }

        // White pawns (row 1)
        for (int x = 0; x < 8; x++) {
            board[1][x].setPiece(new Pawn(white));
        }

        // Black back rank (row 7) — mirror of white's layout
        for (int x = 0; x < 8; x++) {
            Piece whitePiece = whiteBackRank[x];
            board[7][x].setPiece(mirrorPiece(whitePiece, black));
        }

        // Black pawns (row 6)
        for (int x = 0; x < 8; x++) {
            board[6][x].setPiece(new Pawn(black));
        }

        return board;
    }

    /**
     * Creates a new piece of the same type for the given player.
     */
    private static Piece mirrorPiece(Piece template, Player player) {
        if (template instanceof Rook)   return new Rook(player);
        if (template instanceof Knight) return new Knight(player);
        if (template instanceof Bishop) return new Bishop(player);
        if (template instanceof Queen)  return new Queen(player);
        if (template instanceof King)   return new King(player);
        if (template instanceof Pawn)   return new Pawn(player);
        throw new IllegalArgumentException("Unknown piece type: " + template.getClass());
    }
}
