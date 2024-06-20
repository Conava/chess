package ptp.project.logic.game;

import ptp.project.exceptions.IllegalMoveException;
import ptp.project.logic.*;
import ptp.project.logic.moves.Move;
import ptp.project.logic.pieces.Piece;

import java.util.List;

public class OnlineGame extends Observable implements Game {
    @Override
    public void start() {

    }

    @Override
    public Player getCurrentPlayer() {
        return null;
    }

    @Override
    public Player getPlayerWhite() {
        return null;
    }

    @Override
    public Player getPlayerBlack() {
        return null;
    }

    @Override
    public Ruleset getRuleset() {
        return null;
    }

    @Override
    public Piece getPieceAt(Square notation) {
        return null;
    }

    @Override
    public List<Square> getLegalSquares(Square position) {
        return List.of();
    }

    @Override
    public void movePiece(Square squareStart, Square squareEnd) throws IllegalMoveException {
    }

    @Override
    public List<Move> getMoveList() {
        return null;
    }
}
