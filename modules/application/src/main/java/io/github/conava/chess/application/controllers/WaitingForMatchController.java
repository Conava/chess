package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.Chess;
import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.application.navigation.SceneManager;
import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

/**
 * Controller for the matchmaking waiting screen.
 *
 * <p>Shown after the user requests to find a match. Displays an indeterminate spinner while
 * the server searches for an opponent. When the server responds with a {@code MATCHED} message
 * the controller transitions to the game screen. The user may also cancel at any time to
 * return to the main menu.</p>
 *
 * <p>To avoid a race condition between handler registration and queue enrollment, the
 * {@code MATCHED} handler is registered in the constructor <em>before</em>
 * {@link Chess#joinMatchmakingQueue(RulesetOptions)} is called. The queue join is therefore
 * deferred to {@link #initialize()}, which runs after FXML injection but before the scene
 * becomes visible.</p>
 */
public class WaitingForMatchController {

    private final SceneManager sceneManager;
    private final Chess chess;
    private final I18n i18n;
    private final RulesetOptions ruleset;
    private final String serverIp;
    private final int serverPort;

    @FXML
    private Label searchingLabel;
    @FXML
    private ProgressIndicator spinner;
    @FXML
    private Label rulesetLabel;
    @FXML
    private Button cancelBtn;

    /**
     * Constructs a {@code WaitingForMatchController} and immediately registers the
     * {@code MATCHED} message handler on the active {@link io.github.conava.chess.application.network.ServerCommunicationTask}.
     *
     * <p>The handler is registered here (rather than in {@link #initialize()}) so that it is
     * in place before {@link Chess#joinMatchmakingQueue(RulesetOptions)} is called, eliminating
     * the window where a fast server response could arrive before the handler is set.</p>
     *
     * @param sceneManager the navigation manager used to transition screens.
     * @param chess        the application façade used for matchmaking and game lifecycle.
     * @param i18n         the internationalisation helper used for message lookup.
     * @param ruleset      the ruleset the player wants to use for the matched game.
     * @param serverIp     the server IP address or hostname.
     * @param serverPort   the server port number.
     */
    public WaitingForMatchController(SceneManager sceneManager, Chess chess, I18n i18n,
                                     RulesetOptions ruleset, String serverIp, int serverPort) {
        this.sceneManager = sceneManager;
        this.chess = chess;
        this.i18n = i18n;
        this.ruleset = ruleset;
        this.serverIp = serverIp;
        this.serverPort = serverPort;

        // Register the MATCHED handler before joining the queue to prevent a race condition.
        if (chess.getActiveServerTask() != null) {
            chess.getActiveServerTask().setMatchHandler(msg -> Platform.runLater(() -> onMatched(msg)));
        }
    }

    /**
     * Initialises the screen after FXML injection and joins the matchmaking queue.
     *
     * <p>The queue join is performed here (after the handler is already registered in the
     * constructor) to guarantee the handler is in place before the server can respond.</p>
     */
    @FXML
    public void initialize() {
        if (rulesetLabel != null) {
            rulesetLabel.setText(ruleset.toString());
        }
        chess.joinMatchmakingQueue(ruleset);
    }

    /**
     * Handles a {@code MATCHED} server message.
     *
     * <p>Parses the join code from the message content, calls
     * {@link Chess#joinOnlineGame(String, int, String, RulesetOptions)} to start the online game,
     * and navigates to the game screen. Always called on the FX Application Thread.</p>
     *
     * @param msg the {@code MATCHED} message received from the server.
     */
    private void onMatched(Message msg) {
        String joinCode = msg.getParameterValue("joinCode");
        if (joinCode == null || joinCode.isEmpty()) {
            joinCode = msg.content();
        }
        chess.joinOnlineGame(serverIp, serverPort, joinCode, ruleset);
        sceneManager.showGame(ruleset);
    }

    /**
     * Handles the Cancel button action.
     *
     * <p>Sends a {@code DEQUEUE} message to leave the matchmaking queue and navigates back
     * to the main menu.</p>
     */
    @FXML
    private void onCancel() {
        try {
            chess.leaveMatchmakingQueue();
        } catch (IllegalStateException ignored) {
            // Connection may have dropped; safe to ignore here and return to menu.
        }
        sceneManager.showMainMenu();
    }
}
