package io.github.conava.chess.core.logic.observer;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class that maintains a list of {@link GameObserver} instances and
 * provides methods to register, deregister, and notify them.
 *
 * <p>{@link io.github.conava.chess.core.logic.game.Game} extends this class so that
 * any caller can observe game state changes by registering a {@link GameObserver}.</p>
 */
public abstract class Observable {
    private final List<GameObserver> observers = new ArrayList<>();

    /**
     * Registers an observer to receive game state change notifications.
     *
     * @param observer the observer to add; must not be {@code null}
     */
    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    /**
     * Deregisters a previously registered observer.
     *
     * @param observer the observer to remove; no-op if not currently registered
     */
    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    /**
     * Invokes {@link GameObserver#onGameStateChanged()} on every registered observer
     * in registration order.
     */
    public void notifyObservers() {
        for (GameObserver observer : observers) {
            observer.onGameStateChanged();
        }
    }
}