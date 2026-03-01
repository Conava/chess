package io.github.conava.chess.application.tasks;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.data.pieces.Pieces;

import javax.swing.SwingWorker;

/**
 * Background task that executes a chess move off the Event Dispatch Thread.
 *
 * <p>This {@link SwingWorker} delegates to either {@link Chess#movePiece} or
 * {@link Chess#promoteMove} in {@code doInBackground()}, keeping potentially
 * blocking calls (e.g., network I/O for online games) off the EDT.
 *
 * <p>UI refresh is handled entirely by the observer pattern: after a successful
 * move, the core {@code Game} calls {@code notifyObservers()}, which triggers
 * {@code ChessGame.onGameStateChanged()}, which in turn dispatches
 * {@code SwingUtilities.invokeLater(this::update)}. No {@code done()} override
 * is needed here -- doing so would cause a duplicate update.
 */
public class ExecuteMove extends SwingWorker<Void, Void> {
    private final Chess chess;
    private final Square start;
    private final Square end;
    private final Pieces promotionPiece;

    /**
     * Constructs a new {@code ExecuteMove} task.
     *
     * @param chess          the application facade used to submit the move
     * @param start          the square the piece is moving from
     * @param end            the square the piece is moving to
     * @param promotionPiece the piece type to promote to, or {@code null} if this
     *                       is a regular (non-promotion) move
     */
    public ExecuteMove(Chess chess, Square start, Square end, Pieces promotionPiece) {
        this.chess = chess;
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
}
