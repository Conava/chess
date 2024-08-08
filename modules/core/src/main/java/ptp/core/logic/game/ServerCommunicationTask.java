package ptp.core.logic.game;

import ptp.core.data.io.Message;
import ptp.core.data.io.MessageParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerCommunicationTask implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ServerCommunicationTask.class.getName());
    private final String serverIP;
    private final int serverPort;
    private final String joinCode;
    private final OnlineGame onlineGame;
    private Socket socket;
    private PrintWriter out;
    private volatile boolean running = true;

    public ServerCommunicationTask(String serverIP, int serverPort, String joinCode, OnlineGame onlineGame) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.joinCode = joinCode;
        this.onlineGame = onlineGame;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(serverIP, serverPort);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Optionally, handle joinCode if needed
            if (joinCode != null && !joinCode.isEmpty()) {
                // Send joinCode to the server to join a game
            } else {
                // Send a request to the server to create a game
            }

            // Loop to keep listening for messages from the server
            while (running) {
                String message = in.readLine();
                if (message != null) {
                    // Decode the message using MessageParser
                    Message decodedMessage = MessageParser.parse(message);
                    // Pass the decoded message to the OnlineGame class
                    onlineGame.handleMessage(decodedMessage);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to server", e);
        } finally {
            closeConnection();
        }
    }

    public void sendMessage(Message message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void closeConnection() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to close socket", e);
        }
    }
}