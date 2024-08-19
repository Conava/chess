package ptp.core.logic.game;

import ptp.core.data.io.Message;
import ptp.core.data.io.MessageParser;
import ptp.core.data.io.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

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


    public ServerCommunicationTask(String serverIP, int serverPort, OnlineGame onlineGame, CountDownLatch connectionLatch) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.onlineGame = onlineGame;
        this.connectionLatch = connectionLatch;
    }


    @Override
    public void run() {
        try {
            socket = new Socket(serverIP, serverPort);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            connected = true;
            connectionLatch.countDown();

            // Loop to keep listening for messages from the server
            String message = in.readLine();

            while (running && message != null) {
                // Decode the message using MessageParser
                Message decodedMessage = MessageParser.parse(message);
                // Pass the decoded message to the OnlineGame class
                onlineGame.handleMessage(decodedMessage);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to server", e);
        }
    }

    public void sendMessage(Message message) {
        if (out != null) {
            out.println(MessageParser.serialize(message));
        }
    }

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

    public boolean isConnected() {
        return connected;
    }
}