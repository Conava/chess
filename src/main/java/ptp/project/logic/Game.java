package ptp.project.logic;

import java.util.List;

public interface Game {

    void start();

    Board getBoard();

    Player getCurrentPlayer();

    Ruleset getRuleset();

    Piece getPieceAt(Square notation);

    List<Square> getLegalMoves(Piece piece);

    List<Square> getLegalMoves(Square position);

    void movePiece(Piece piece, Square newPosition);
}
