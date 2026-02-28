package io.github.conava.chess.application.network;

import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.data.io.MessageParser;
import io.github.conava.chess.core.logic.game.ServerConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles TCP socket communication with the chess server.
 * Implements {@link ServerConnection} so it can be passed into the core {@code OnlineGame}
 * without introducing I/O dependencies into the {@code core} module.
 *
 * <p>After construction, call {@link #setMessageHandler(Consumer)} to register the callback
 * that will receive parsed {@link Message} objects from the server, then start this task on
 * a new {@link Thread}. The provided {@link CountDownLatch} will be counted down once the
 * socket connection is established (or fails).</p>
 */
public class ServerCommunicationTask implements Runnable, ServerConnection {
    private static final Logger LOGGER = Logger.getLogger(ServerCommunicationTask.class.getName());

    private final String serverIP;
    private final int serverPort;
    private final CountDownLatch connectionLatch;
    private Consumer<Message> messageHandler;

    private Socket socket;
    private PrintWriter out;
    private volatile boolean running = true;
    private volatile boolean connected = false;

    /**
     * Constructs a {@code ServerCommunicationTask}.
     *
     * @param serverIP        The IP address of the server.
     * @param serverPort      The port number of the server.
     * @param connectionLatch Counted down to 0 once the connection is established or has failed,
     *                        allowing the caller to block until connectivity is known.
     */
    public ServerCommunicationTask(String serverIP, int serverPort, CountDownLatch connectionLatch) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.connectionLatch = connectionLatch;
    }

    /**
     * Sets the handler that will be invoked for each {@link Message} received from the server.
     * Must be called before the task thread starts reading, or message delivery may be missed.
     *
     * @param messageHandler A {@link Consumer} that accepts parsed {@link Message} objects.
     */
    public void setMessageHandler(Consumer<Message> messageHandler) {
        this.messageHandler = messageHandler;
    }

    /**
     * Opens the socket connection, then loops reading lines from the server.
     * Each line is parsed into a {@link Message} and forwarded to the registered message handler.
     * Duplicate messages (same raw string) within a connection are silently discarded.
     */
    @Override
    public void run() {
        Set<String> processedMessages = new HashSet<>();
        try {
            socket = new Socket(serverIP, serverPort);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            connected = true;
            connectionLatch.countDown();

            String rawMessage = in.readLine();
            while (running && rawMessage != null) {
                if (!processedMessages.contains(rawMessage)) {
                    Message decoded = MessageParser.parse(rawMessage);
                    if (messageHandler != null) {
                        messageHandler.accept(decoded);
                    }
                    processedMessages.add(rawMessage);
                }
                rawMessage = in.readLine();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to server", e);
            connectionLatch.countDown();
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
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to close socket", e);
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
