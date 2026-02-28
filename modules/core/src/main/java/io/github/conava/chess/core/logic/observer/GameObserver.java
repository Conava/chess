package io.github.conava.chess.core.logic.observer;

/**
 * Observer contract for game state changes.
 *
 * <p>Implement this interface and register via {@link Observable#addObserver(GameObserver)}
 * to receive a callback whenever the game state changes — after every local move, every
 * remote move applied by the server, or after a server-rejected move is rolled back.</p>
 *
 * <p>Implementations must not mutate game state from inside the callback. UI implementations
 * should re-read state via the {@code Chess} facade and schedule rendering updates
 * appropriately (e.g., via {@code SwingUtilities.invokeLater}).</p>
 */
public interface GameObserver {

    /**
     * Called by {@link Observable#notifyObservers()} whenever the game state has changed.
     * This includes: a move being executed (offline or online), or a server-rejected move
     * being rolled back in {@code OnlineGame}.
     */
    void onGameStateChanged();
}
