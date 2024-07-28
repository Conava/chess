package ptp.server.management;

import ptp.server.io.MessageParser;

import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ClientHandler class implements the Runnable interface and is responsible for handling
 * client connections. It reads incoming messages from the client, processes them using
 * the MessageParser, and sends back responses.
 */
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final MessageParser messageParser;
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    /**
     * Constructs a ClientHandler with the specified client socket and message parser.
     *
     * @param clientSocket The socket connected to the client.
     * @param messageParser The parser to handle incoming messages.
     */
    public ClientHandler(Socket clientSocket, MessageParser messageParser) {
        this.clientSocket = clientSocket;
        this.messageParser = messageParser;
    }

    /**
     * The run method is executed when the thread is started. It reads a request from the client,
     * processes it using the message parser, and sends back a response.
     */
    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String request = in.readLine();
            String response = messageParser.handleIncomingMessage(request);
            out.println(response);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error handling client request", e);
        }
    }
}