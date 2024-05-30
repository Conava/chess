package ptp.project.logic.game;

import ptp.project.logic.*;

import java.util.List;

public class OnlineGame implements Game {
    @Override
    public void start() {

    }

    @Override
    public Board getBoard() {
        return null;
    }

    @Override
    public Player getCurrentPlayer() {
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
    public List<Square> getLegalMoves(Piece piece) {
        return List.of();
    }

    @Override
    public List<Square> getLegalMoves(Square position) {
        return List.of();
    }

    @Override
    public void movePiece(Piece piece, Square newPosition) {

    }
}
