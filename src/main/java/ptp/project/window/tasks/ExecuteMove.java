package ptp.project.window.tasks;

import ptp.project.Chess;
import ptp.project.data.Square;
import ptp.project.data.enums.Pieces;
import ptp.project.window.ChessGame;

import javax.swing.SwingWorker;

public class ExecuteMove extends SwingWorker<Void, Void> {
    private final ChessGame chessGame;
    private final Chess chess;
    private final Square start;
    private final Square end;
    private final Pieces promotionPiece;

    public ExecuteMove(Chess chess, ChessGame chessGame, Square start, Square end, Pieces promotionPiece) {
        this.chess = chess;
        this.chessGame = chessGame;
        this.start = start;
        this.end = end;
        this.promotionPiece = promotionPiece;
    }

    @Override
    protected Void doInBackground() throws Exception {
        if (promotionPiece != null) {
            chess.promoteMove(start, end, promotionPiece);
            return null;
        }
        chess.movePiece(start, end);
        return null;
    }

    @Override
    protected void done() {
        chessGame.update();
    }
}