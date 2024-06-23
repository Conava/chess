package ptp.project.logic.game;

import ptp.project.logic.Board;
import ptp.project.logic.Player;
import ptp.project.logic.Ruleset;
import ptp.project.logic.Square;
import ptp.project.logic.pieces.Piece;

import ptp.project.exceptions.IllegalMoveException;
import ptp.project.logic.moves.Move;

import java.util.List;

public abstract class Game extends Observable {

    public abstract void start();

    public abstract Player getCurrentPlayer();

    public abstract Player getPlayerWhite();

    public abstract Player getPlayerBlack();

    public abstract Piece getPieceAt(Square notation);

    public abstract List<Square> getLegalSquares(Square position);

    public abstract List<Move> getMoveList();

    public abstract void movePiece(Square squareStart, Square squareEnd) throws IllegalMoveException;

    public abstract int getStatus();
}
