package ptp.project.logic.game;

import ptp.project.logic.Board;
import ptp.project.logic.Player;
import ptp.project.logic.Ruleset;
import ptp.project.logic.Square;
import ptp.project.logic.pieces.Piece;

import ptp.project.exceptions.IllegalMoveException;
import ptp.project.logic.moves.Move;

import java.util.List;

public interface Game {

    void start();

    Board getBoard();

    Player getCurrentPlayer();

    Player getPlayerWhite();

    Player getPlayerBlack();

    Ruleset getRuleset();

    Piece getPieceAt(Square notation);

    List<Square> getLegalSquares(Square position);

    List<Move> getMoveList();

    void movePiece(Move move) throws IllegalMoveException;
}
//todo rochade und umwandlung  wer dran ist