package io.github.conava.chess.application.network;

import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.data.io.MessageParser;
import io.github.conava.chess.core.logic.game.ServerConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles TCP socket communication with the chess server.
 * Implements {@link ServerConnection} so it can be passed into the core {@code OnlineGame}
 * without introducing I/O dependencies into the {@code core} module.
 *
 * <p>The {@link Consumer} message handler is injected at construction time so that no
 * messages can arrive before the handler is registered. The provided {@link CountDownLatch}
 * is counted down once the socket connection is established (or fails). All resources
 * (socket, reader, writer) are closed in a {@code finally} block on every exit path.</p>
 *
 * <p>Additional optional callbacks may be registered after construction via setter methods:
 * {@link #setChatHandler}, {@link #setMatchHandler}, {@link #setSaveAcceptedHandler},
 * {@link #setGameHistoryHandler}, and {@link #setSaveGameHandler}. All callback fields
 * are {@code volatile} so that assignments made after the task thread has started are
 * visible immediately. If no specific handler is registered for a message type, the message
 * falls through to the default {@code messageHandler}.</p>
 */
public class ServerCommunicationTask implements Runnable, ServerConnection {
    private static final Logger LOGGER = Logger.getLogger(ServerCommunicationTask.class.getName());

    private final String serverIP;
    private final int serverPort;
    private final CountDownLatch connectionLatch;
    private final Consumer<Message> messageHandler;

    private volatile Consumer<Message> chatHandler;
    private volatile Consumer<Message> matchHandler;
    private volatile Runnable saveAcceptedHandler;
    private volatile Consumer<Message> gameHistoryHandler;
    private volatile Consumer<Message> saveGameHandler;
    private volatile Runnable authTokenHandler;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean running = true;
    private volatile boolean connected = false;

    /**
     * Constructs a {@code ServerCommunicationTask}.
     *
     * @param serverIP        The IP address of the server.
     * @param serverPort      The port number of the server.
     * @param connectionLatch Counted down to 0 once the connection is established or has failed,
     *                        allowing the caller to block until connectivity is known.
     * @param messageHandler  A {@link Consumer} that accepts parsed {@link Message} objects
     *                        received from the server.
     */
    public ServerCommunicationTask(String serverIP, int serverPort, CountDownLatch connectionLatch,
                                   Consumer<Message> messageHandler) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.connectionLatch = connectionLatch;
        this.messageHandler = messageHandler;
    }

    /**
     * Sets the handler called when a {@code CHAT} message is received.
     * If {@code null} or unset, {@code CHAT} messages fall through to the default message handler.
     *
     * @param chatHandler a {@link Consumer} that accepts the incoming {@link Message}, or {@code null}
     *                    to clear a previously registered handler.
     */
    public void setChatHandler(Consumer<Message> chatHandler) {
        this.chatHandler = chatHandler;
    }

    /**
     * Sets the handler called when a {@code MATCHED} message is received.
     * If {@code null} or unset, {@code MATCHED} messages fall through to the default message handler.
     *
     * @param matchHandler a {@link Consumer} that accepts the incoming {@link Message}, or {@code null}
     *                     to clear a previously registered handler.
     */
    public void setMatchHandler(Consumer<Message> matchHandler) {
        this.matchHandler = matchHandler;
    }

    /**
     * Sets the handler called when a {@code SAVE_ACCEPTED} message is received.
     * If {@code null} or unset, {@code SAVE_ACCEPTED} messages fall through to the default message handler.
     *
     * @param saveAcceptedHandler a {@link Runnable} that is invoked upon receipt of the message,
     *                            or {@code null} to clear a previously registered handler.
     */
    public void setSaveAcceptedHandler(Runnable saveAcceptedHandler) {
        this.saveAcceptedHandler = saveAcceptedHandler;
    }

    /**
     * Sets the handler called when a {@code GAME_HISTORY} message is received.
     * If {@code null} or unset, {@code GAME_HISTORY} messages fall through to the default message handler.
     *
     * @param gameHistoryHandler a {@link Consumer} that accepts the incoming {@link Message}, or {@code null}
     *                           to clear a previously registered handler.
     */
    public void setGameHistoryHandler(Consumer<Message> gameHistoryHandler) {
        this.gameHistoryHandler = gameHistoryHandler;
    }

    /**
     * Sets the handler called when a {@code SAVE_GAME} message is received.
     * If {@code null} or unset, {@code SAVE_GAME} messages fall through to the default message handler.
     *
     * @param saveGameHandler a {@link Consumer} that accepts the incoming {@link Message}, or {@code null}
     *                        to clear a previously registered handler.
     */
    public void setSaveGameHandler(Consumer<Message> saveGameHandler) {
        this.saveGameHandler = saveGameHandler;
    }

    /**
     * Sets a one-shot handler called when an {@code AUTH_TOKEN} response is received from the server.
     *
     * <p>Used to synchronise the re-authentication handshake on a new TCP connection: the caller
     * registers a {@link Runnable} (typically a {@link java.util.concurrent.CountDownLatch#countDown}
     * invocation) before sending the auth message, and the handler is invoked as soon as the server
     * confirms authentication. Set to {@code null} to clear the handler after it fires.</p>
     *
     * @param authTokenHandler a {@link Runnable} invoked when an {@code AUTH_TOKEN} message arrives,
     *                         or {@code null} to clear a previously registered handler.
     */
    public void setAuthTokenHandler(Runnable authTokenHandler) {
        this.authTokenHandler = authTokenHandler;
    }

    /**
     * Opens the socket connection, then loops reading lines from the server.
     * Each line is parsed into a {@link Message} and forwarded to the registered message handler.
     * All resources are closed in a {@code finally} block on every exit path.
     * <p>
     * Per-message runtime exceptions thrown by {@link io.github.conava.chess.core.data.io.MessageParser#parse}
     * or by the message handler are caught, logged at {@link Level#SEVERE}, and skipped so that
     * a single malformed message does not kill the listener thread. The loop continues reading
     * the next line after any such exception.
     * <p>
     * Messages are dispatched based on their type: {@code CHAT}, {@code MATCHED},
     * {@code SAVE_ACCEPTED}, {@code GAME_HISTORY}, and {@code SAVE_GAME} are forwarded to their
     * respective optional callbacks when set, otherwise they fall through to the default
     * {@code messageHandler}. All other message types go directly to the default handler.
     */
    @Override
    public void run() {
        try {
            socket = new Socket(serverIP, serverPort);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            connected = true;
            connectionLatch.countDown();

            String rawMessage = in.readLine();
            while (running && rawMessage != null) {
                try {
                    Message decoded = MessageParser.parse(rawMessage);
                    dispatch(decoded);
                } catch (RuntimeException e) {
                    LOGGER.log(Level.SEVERE, "Failed to process server message: " + rawMessage, e);
                }
                rawMessage = in.readLine();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to server", e);
            connectionLatch.countDown();
        } finally {
            connected = false;
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ignored) {
                    // silently ignored
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    // silently ignored
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // silently ignored
                }
            }
        }
    }

    /**
     * Routes a decoded {@link Message} to the appropriate registered callback.
     * Falls through to the default {@code messageHandler} when no specific handler is set
     * for the message type, ensuring no message is silently dropped.
     *
     * @param decoded the parsed {@link Message} to dispatch.
     */
    private void dispatch(Message decoded) {
        switch (decoded.type()) {
            case CHAT -> {
                if (chatHandler != null) chatHandler.accept(decoded);
                else messageHandler.accept(decoded);
            }
            case MATCHED -> {
                if (matchHandler != null) matchHandler.accept(decoded);
                else messageHandler.accept(decoded);
            }
            case SAVE_ACCEPTED -> {
                if (saveAcceptedHandler != null) saveAcceptedHandler.run();
                else messageHandler.accept(decoded);
            }
            case GAME_HISTORY -> {
                if (gameHistoryHandler != null) gameHistoryHandler.accept(decoded);
                else messageHandler.accept(decoded);
            }
            case SAVE_GAME -> {
                if (saveGameHandler != null) saveGameHandler.accept(decoded);
                else messageHandler.accept(decoded);
            }
            case AUTH_TOKEN -> {
                if (authTokenHandler != null) authTokenHandler.run();
                else messageHandler.accept(decoded);
            }
            default -> messageHandler.accept(decoded);
        }
    }

    /**
     * Sends a pre-serialized message string to the server.
     *
     * @param message The raw serialized message to send.
     */
    @Override
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    /**
     * Closes the connection to the server.
     */
    @Override
    public void closeConnection() {
        LOGGER.info("Closing connection to server");
        running = false;
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to close socket", e);
            }
        }
    }

    /**
     * Returns whether the connection is currently active.
     *
     * @return true if the socket was successfully connected, false otherwise.
     */
    @Override
    public boolean isConnected() {
        return connected;
    }
}
