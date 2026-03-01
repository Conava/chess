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
 */
public class ServerCommunicationTask implements Runnable, ServerConnection {
    private static final Logger LOGGER = Logger.getLogger(ServerCommunicationTask.class.getName());

    private final String serverIP;
    private final int serverPort;
    private final CountDownLatch connectionLatch;
    private final Consumer<Message> messageHandler;

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
     * Opens the socket connection, then loops reading lines from the server.
     * Each line is parsed into a {@link Message} and forwarded to the registered message handler.
     * All resources are closed in a {@code finally} block on every exit path.
     * <p>
     * Per-message runtime exceptions thrown by {@link io.github.conava.chess.core.data.io.MessageParser#parse}
     * or by the message handler are caught, logged at {@link Level#SEVERE}, and skipped so that
     * a single malformed message does not kill the listener thread. The loop continues reading
     * the next line after any such exception.
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
                    messageHandler.accept(decoded);
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
