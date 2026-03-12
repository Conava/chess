package io.github.conava.chess.core.logic.observer;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract base class that maintains a list of {@link GameObserver} instances and
 * provides methods to register, deregister, and notify them.
 *
 * <p>{@link io.github.conava.chess.core.logic.game.Game} extends this class so that
 * any caller can observe game state changes by registering a {@link GameObserver}.</p>
 *
 * <p>The observer list is backed by a {@link CopyOnWriteArrayList} so that
 * {@link #notifyObservers()} can safely iterate while another thread concurrently
 * adds or removes observers.</p>
 */
public abstract class Observable {
    private final List<GameObserver> observers = new CopyOnWriteArrayList<>();

    /**
     * Registers an observer to receive game state change notifications.
     *
     * @param observer the observer to add; must not be {@code null}
     * @throws NullPointerException if {@code observer} is {@code null}
     */
    public void addObserver(GameObserver observer) {
        Objects.requireNonNull(observer, "observer must not be null");
        observers.add(observer);
    }

    /**
     * Deregisters a previously registered observer.
     *
     * @param observer the observer to remove; no-op if not currently registered
     * @throws NullPointerException if {@code observer} is {@code null}
     */
    public void removeObserver(GameObserver observer) {
        Objects.requireNonNull(observer, "observer must not be null");
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

    /**
     * Invokes {@link GameObserver#onChatMessage(String, String)} on every registered observer.
     *
     * @param sender  display name of the player who sent the message
     * @param content message text
     */
    public void notifyChatObservers(String sender, String content) {
        for (GameObserver observer : observers) {
            observer.onChatMessage(sender, content);
        }
    }
}