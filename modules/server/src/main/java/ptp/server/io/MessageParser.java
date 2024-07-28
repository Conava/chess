package ptp.server.io;

import ptp.core.data.enums.RulesetOptions;
import ptp.core.logic.moves.Move;
import ptp.server.management.GameInstance;
import ptp.server.management.GameManager;

import java.lang.reflect.InvocationTargetException;

public class MessageParser {
    private final GameManager gameManager;

    public MessageParser(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public String handleIncomingMessage(String messageString) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Message message = Message.fromString(messageString);
        return switch (message.type()) {
            case "CREATE_GAME" -> handleCreateGame(message.content());
            case "JOIN_GAME" -> handleJoinGame(message.content());
            case "SUBMIT_MOVE" -> handleSubmitMove(message.content());
            // todo: add leaving game, making moves, etc.
            default -> "ERROR Unknown message type";
        };
    }

    private String handleCreateGame(String ruleset) {
        RulesetOptions rulesetOption = RulesetOptions.valueOf(ruleset);
        String joinCode = gameManager.createGame(rulesetOption);
        return new Message("JOIN_CODE", joinCode).toString();
    }

    private String handleJoinGame(String joinCode) {
        GameInstance gameInstance = gameManager.joinGame(joinCode);
        if (gameInstance != null) {
            int playerNumber = gameInstance.connectPlayer();
            if (playerNumber != 0) {
                return new Message("CONNECTED", playerNumber == 1 ? "WHITE" : "BLACK").toString();
            } else {
                return new Message("ERROR", "Game full").toString();
            }
        } else {
            return new Message("ERROR", "Invalid join code").toString();
        }
    }

    private String handleSubmitMove(String moveContent) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // todo: not working!

        String[] parts = moveContent.split(" ", 2);
        String joinCode = parts[0];
        String moveString = parts[1];

        GameInstance gameInstance = gameManager.joinGame(joinCode);
        if (gameInstance != null) {
            Move move = Move.fromString(moveString, gameInstance.getCurrentPlayer());
            boolean moveSuccess = gameInstance.submitMove(move);
            if (moveSuccess) {
                // Notify the player who made the move
                String confirmation = new Message("MOVE_CONFIRMED", move.toString()).toString();
                // Notify the opponent
                String opponentNotification = new Message("OPPONENT_MOVE", move.toString()).toString();
                return confirmation + "\n" + opponentNotification;
            } else {
                return new Message("ERROR", "Invalid move").toString();
            }
        } else {
            return new Message("ERROR", "Invalid join code").toString();
        }
    }
}