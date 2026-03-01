package io.github.conava.chess.core.logic.game;

/**
 * Interface representing a connection to the chess server.
 * Decouples the networking I/O implementation from the core game logic.
 * Concrete implementations live in the application module.
 */
public interface ServerConnection {
    /**
     * Sends a raw string message to the server.
     *
     * @param message The serialized message to send.
     */
    void sendMessage(String message);

    /**
     * Closes the connection to the server.
     */
    void closeConnection();

    /**
     * Returns whether the connection is currently active.
     *
     * @return true if connected, false otherwise.
     */
    boolean isConnected();
}
