package ptp.project.window.tasks;

import ptp.project.window.ChessGame;

import javax.swing.SwingWorker;

public class UpdateGame extends SwingWorker<Void, Void> {
    private final ChessGame chessGame;

    public UpdateGame(ChessGame chessGame) {
        this.chessGame = chessGame;
    }

    @Override
    protected Void doInBackground() throws Exception {
        chessGame.updateGame();
        return null;
    }
//
//    @Override
//    protected void done() {
//
//    }
}