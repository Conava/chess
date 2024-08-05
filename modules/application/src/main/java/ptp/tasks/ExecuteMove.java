package ptp.tasks;

import ptp.Chess;
import ptp.core.data.Square;
import ptp.core.data.pieces.Pieces;
import ptp.window.ChessGame;

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