package ptp.core.logic.game;

import ptp.core.data.io.Message;
import ptp.core.data.io.MessageParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ServerCommunicationTask class handles communication with the server.
 * It listens for messages from the server and processes them using the OnlineGame class.
 */
public class ServerCommunicationTask implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ServerCommunicationTask.class.getName());
    private final String serverIP;
    private final int serverPort;
    private final OnlineGame onlineGame;
    private Socket socket;
    private PrintWriter out;
    private volatile boolean running = true;
    private volatile boolean connected = false;
    private final CountDownLatch connectionLatch;

    /**
     * Constructs a ServerCommunicationTask instance.
     *
     * @param serverIP The IP address of the server.
     * @param serverPort The port number of the server.
     * @param onlineGame The OnlineGame instance to handle messages.
     * @param connectionLatch The CountDownLatch to signal connection establishment.
     */
    public ServerCommunicationTask(String serverIP, int serverPort, OnlineGame onlineGame, CountDownLatch connectionLatch) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.onlineGame = onlineGame;
        this.connectionLatch = connectionLatch;
    }


    /**
     * The run method establishes a connection to the server and listens for messages.
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

            // Loop to keep listening for messages from the server
            String message = in.readLine();

            while (running && message != null) {
                if (!processedMessages.contains(message)) {
                    Message decodedMessage = MessageParser.parse(message);
                    onlineGame.handleMessage(decodedMessage);
                    processedMessages.add(message);
                }
                message = in.readLine();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to server", e);
        }
    }

    /**
     * Sends a message to the server.
     *
     * @param message The message to be sent.
     */
    public void sendMessage(Message message) {
        if (out != null) {
            out.println(MessageParser.serialize(message));
        }
    }

    /**
     * Closes the connection to the server.
     */
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
     * Checks if the task is connected to the server.
     *
     * @return true if connected, false otherwise.
     */
    public boolean isConnected() {
        return connected;
    }
}