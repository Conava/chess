package ptp.project.logic.game;

import ptp.project.logic.*;
import ptp.project.logic.ruleset.StandardChessRuleset;

import java.util.List;

public class OfflineGame implements Game {
    Player player1;
    Player player2;
    Ruleset ruleset;
    Board board;

    public OfflineGame() {
        player1 = new Player("Player 1", "white");
        player2 = new Player("Player 2", "black");
        this.ruleset = new StandardChessRuleset();
        this.board = new Board(ruleset.getStartBoard(player1, player2));
    }
    @Override
    public void start() {
    }

    @Override
    public Board getBoard() {
        return board;
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
    public List<Square> getLegalMoves(Square position) {
        return List.of();
    }

    @Override
    public void movePiece(Piece piece, Square newPosition) {

    }
}
