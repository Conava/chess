package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.core.logic.game.GameState;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.text.MessageFormat;
import java.util.MissingResourceException;

/**
 * Controller for the game-end overlay dialog (game-end.fxml).
 *
 * <p>Displayed when a game concludes. Shows the outcome, player names, move count,
 * and offers rematch or return-to-menu options. Closes itself by invoking the
 * {@code closeAction} runnable provided at construction time.
 */
public class GameEndController {

    /** Possible user choices when dismissing the game-end overlay. */
    public enum Choice {
        NONE, RETURN, REMATCH
    }

    @FXML private Label  outcomeBadge;
    @FXML private Label  outcomeTitle;
    @FXML private Label  outcomeSubtitle;
    @FXML private Label  playerWhiteLabel;
    @FXML private Label  playerBlackLabel;
    @FXML private Label  moveCountLabel;
    @FXML private Button rematchBtn;

    private final I18n         i18n;
    private final GameState    state;
    private final String       whiteName;
    private final String       blackName;
    private final int          moveCount;
    private final boolean      isOnline;
    private final Runnable     closeAction;

    private Choice choice = Choice.NONE;

    public GameEndController(I18n i18n, GameState state, String whiteName, String blackName,
                             int moveCount, boolean isOnline, Runnable closeAction) {
        this.i18n         = i18n;
        this.state        = state;
        this.whiteName    = whiteName;
        this.blackName    = blackName;
        this.moveCount    = moveCount;
        this.isOnline     = isOnline;
        this.closeAction  = closeAction;
    }

    @FXML
    public void initialize() {
        // --- Outcome badge ---
        String badgeText = resolveStateLabel();
        outcomeBadge.setText(badgeText);

        boolean isWin  = state.name().startsWith("WHITE_WON") || state.name().startsWith("BLACK_WON");
        boolean isDraw = state.name().startsWith("DRAW");

        if (isWin) {
            outcomeBadge.getStyleClass().add("outcome-badge-win");
        } else if (isDraw) {
            outcomeBadge.getStyleClass().add("outcome-badge-draw");
        }

        // --- Title ---
        if (isWin) {
            if (isOnline) {
                outcomeTitle.setText(i18n.get("game.end.title.win"));
            } else {
                String winnerName = resolveWinnerName(state, whiteName, blackName);
                outcomeTitle.setText(MessageFormat.format(i18n.get("game.end.title.win.local"), winnerName));
            }
        } else if (isDraw) {
            outcomeTitle.setText(i18n.get("game.end.title.draw"));
        } else {
            outcomeTitle.setText(i18n.get("game.end.title.loss"));
        }

        // --- Subtitle ---
        outcomeSubtitle.setText(badgeText);

        // --- Player names ---
        playerWhiteLabel.setText(whiteName);
        playerBlackLabel.setText(blackName);

        // --- Move count ---
        moveCountLabel.setText(MessageFormat.format(i18n.get("game.end.moves"), moveCount));

        // --- Online: disable rematch ---
        if (isOnline) {
            rematchBtn.setDisable(true);
        }
    }

    @FXML
    public void onReturn() {
        choice = Choice.RETURN;
        closeAction.run();
    }

    @FXML
    public void onRematch() {
        choice = Choice.REMATCH;
        closeAction.run();
    }

    /** Returns the user's choice after the dialog has been dismissed. */
    public Choice getChoice() {
        return choice;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Returns the name of the winning player derived from the terminal {@link GameState}.
     * Package-private for unit testing.
     */
    static String resolveWinnerName(GameState state, String whiteName, String blackName) {
        return state.name().startsWith("WHITE_WON") ? whiteName : blackName;
    }

    private String resolveStateLabel() {
        try {
            return i18n.get("state." + state.name());
        } catch (MissingResourceException e) {
            return state.name();
        }
    }
}
