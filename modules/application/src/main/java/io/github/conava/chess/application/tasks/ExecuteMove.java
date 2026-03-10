package io.github.conava.chess.application.tasks;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.pieces.Pieces;
import javafx.concurrent.Task;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background task that executes a chess move off the JavaFX Application Thread.
 *
 * <p>Delegates to {@link Chess#movePiece} or {@link Chess#promoteMove} in {@link #call()}.
 * UI refresh is driven by the observer chain — this task holds no reference to
 * {@code GameController} and does not mutate UI state directly.
 */
public class ExecuteMove extends Task<Void> {

    private static final Logger LOGGER = Logger.getLogger(ExecuteMove.class.getName());

    private final Chess chess;
    private final Square start;
    private final Square end;
    private final Pieces promotionPiece;

    public ExecuteMove(Chess chess, Square start, Square end, Pieces promotionPiece) {
        this.chess = chess;
        this.start = start;
        this.end = end;
        this.promotionPiece = promotionPiece;
    }

    @Override
    protected Void call() throws Exception {
        if (promotionPiece != null) {
            chess.promoteMove(start, end, promotionPiece);
        } else {
            chess.movePiece(start, end);
        }
        return null;
    }

    @Override
    protected void failed() {
        LOGGER.log(Level.WARNING, "Move execution failed", getException());
    }
}
