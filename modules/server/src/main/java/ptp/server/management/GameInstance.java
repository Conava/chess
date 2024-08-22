package ptp.server.management;

import ptp.core.data.io.Message;
import ptp.core.data.io.MessageType;
import ptp.core.exceptions.IllegalMoveException;
import ptp.core.logic.game.GameState;
import ptp.core.logic.game.ServerGame;
import ptp.core.logic.moves.Move;
import ptp.core.logic.ruleset.RulesetOptions;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The GameInstance class represents an instance of a game.
 * It manages the connection state of players and initializes the game state based on the provided ruleset.
 */
public class GameInstance {
    private static final Logger LOGGER = Logger.getLogger(GameInstance.class.getName());
    private final ServerGame game;
    private final int gameId;
    private ClientHandler whitePlayerHandler;
    private ClientHandler blackPlayerHandler;

    /**
     * Constructs a GameInstance with the specified ruleset.
     *
     * @param gameId The unique ID of the game.
     * @param ruleset The ruleset options for the game.
     */
    public GameInstance(int gameId, RulesetOptions ruleset) {
        this.gameId = gameId;
        this.game = new ServerGame(ruleset);
        this.game.setGameState(GameState.WAITING_FOR_PLAYER);
    }

    /**
     * Connects a player to the game instance.
     *
     * @param clientHandler The client handler for the player.
     */
    public synchronized void connectPlayer(ClientHandler clientHandler) {
        if (whitePlayerHandler == null) {
            whitePlayerHandler = clientHandler;
            clientHandler.sendMessage(new Message(MessageType.SUCCESS, "player=white"));
        } else if (blackPlayerHandler == null) {
            blackPlayerHandler = clientHandler;
            clientHandler.sendMessage(new Message(MessageType.SUCCESS, "player=black"));
            startGame();
        }
    }

    /**
     * Starts the game and notifies players.
     */
    private void startGame() {
        game.startGame();
        sendMessageToPlayers(new Message(MessageType.GAME_STATUS, "gameState=running"));
    }

    /**
     * Processes a message from a client.
     *
     * @param clientHandler The client handler sending the message.
     * @param message The message to process.
     */
    public void processMessage(ClientHandler clientHandler, Message message) {
        switch (message.type()) {
            case JOIN_CODE:
                handleJoinCode(message);
                break;
            case MOVE:
                handleMove(message);
                break;
            case GAME_STATUS:
                handleGameStatus(clientHandler);
                break;
            case SUCCESS:
                handleSuccess(message);
                break;
            case ERROR:
                handleError(message);
                break;
            case FAILURE:
                handleFailure(message);
                break;
            default:
                LOGGER.log(Level.WARNING, "Unsupported message type: " + message.type());
        }
    }

    /**
     * Sends a message to both players.
     *
     * @param message The message to send.
     */
    private void sendMessageToPlayers(Message message) {
        if (whitePlayerHandler != null) {
            whitePlayerHandler.sendMessage(message);
        }
        if (blackPlayerHandler != null) {
            blackPlayerHandler.sendMessage(message);
        }
    }

    /**
     * Handles a join code message.
     *
     * @param message The message containing the join code.
     */
    private void handleJoinCode(Message message) {
        LOGGER.log(Level.INFO, "Join code received: " + message.content());
    }

    /**
     * Handles a move message.
     *
     * @param message The message containing the move details.
     */
    private void handleMove(Message message) {
        try {
            Move move = Move.fromString(Objects.requireNonNull(message.getParameterValue("move")),
                    Objects.equals(message.getParameterValue("playerColor"), "WHITE") ? game.getPlayerWhite() : game.getPlayerBlack());
            game.movePiece(move.getStart(), move.getEnd());
            LOGGER.log(Level.INFO, "Move executed: " + message.content());
        } catch (IllegalMoveException | ClassNotFoundException | NoSuchMethodException |
                 InstantiationException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.log(Level.SEVERE, "Illegal move received: " + message.content(), e);
        }
    }

    /**
     * Handles a game status request.
     *
     * @param clientHandler The client handler requesting the game status.
     */
    private void handleGameStatus(ClientHandler clientHandler) {
        LOGGER.log(Level.INFO, "Game status requested");
        clientHandler.sendMessage(new Message(MessageType.GAME_STATUS, "gameState=" + game.getState()));
    }

    /**
     * Handles a success message.
     *
     * @param message The success message.
     */
    private void handleSuccess(Message message) {
        LOGGER.log(Level.INFO, "Success: " + message);
    }

    /**
     * Handles an error message.
     *
     * @param message The error message.
     */
    private void handleError(Message message) {
        LOGGER.log(Level.SEVERE, "Error: " + message.content());
    }

    /**
     * Handles a failure message.
     *
     * @param message The failure message.
     */
    private void handleFailure(Message message) {
        LOGGER.log(Level.SEVERE, "Failure: " + message.content());
    }

    /**
     * Gets the game ID.
     *
     * @return The game ID.
     */
    public int getGameId() {
        return gameId;
    }

    /**
     * Gets the white player handler.
     *
     * @return The white player handler.
     */
    public ClientHandler getWhitePlayerHandler() {
        return whitePlayerHandler;
    }

    /**
     * Gets the black player handler.
     *
     * @return The black player handler.
     */
    public ClientHandler getBlackPlayerHandler() {
        return blackPlayerHandler;
    }
}