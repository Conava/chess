package io.github.conava.chess.application.controllers;

import io.github.conava.chess.application.i18n.I18n;
import io.github.conava.chess.core.data.player.PlayerColor;
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

    /**
     * Possible user choices when dismissing the game-end overlay.
     */
    public enum Choice {
        NONE, RETURN, REMATCH
    }

    @FXML
    private Label outcomeBadge;
    @FXML
    private Label outcomeTitle;
    @FXML
    private Label outcomeSubtitle;
    @FXML
    private Label playerWhiteLabel;
    @FXML
    private Label playerBlackLabel;
    @FXML
    private Label moveCountLabel;
    @FXML
    private Button rematchBtn;

    private final I18n i18n;
    private final GameState state;
    private final String whiteName;
    private final String blackName;
    private final int moveCount;
    private final boolean isOnline;

    /**
     * The local player's color for an online game, or {@code null} for offline games.
     * Used to determine whether to display "You Win!" or "You Lose" for online games
     * where the win/loss perspective depends on which side the local player is playing.
     */
    private final PlayerColor localPlayerColor;

    private final Runnable closeAction;

    private Choice choice = Choice.NONE;

    /**
     * Creates a new game-end controller.
     *
     * @param i18n            internationalisation bundle.
     * @param state           the terminal game state to display.
     * @param whiteName       white player's display name.
     * @param blackName       black player's display name.
     * @param moveCount       total number of moves played.
     * @param isOnline        {@code true} for online games (disables rematch, uses
     *                        "You Win/Lose" phrasing).
     * @param localPlayerColor the local player's color for online games, or {@code null}
     *                         for offline games. Determines whether to show "You Win!" or
     *                         "You Lose" in online mode.
     * @param closeAction     runnable invoked when the overlay should be dismissed.
     */
    public GameEndController(I18n i18n, GameState state, String whiteName, String blackName,
                             int moveCount, boolean isOnline, PlayerColor localPlayerColor,
                             Runnable closeAction) {
        this.i18n = i18n;
        this.state = state;
        this.whiteName = whiteName;
        this.blackName = blackName;
        this.moveCount = moveCount;
        this.isOnline = isOnline;
        this.localPlayerColor = localPlayerColor;
        this.closeAction = closeAction;
    }

    @FXML
    public void initialize() {
        // --- Outcome badge ---
        String badgeText = resolveStateLabel();
        outcomeBadge.setText(badgeText);

        boolean isWin = state.name().startsWith("WHITE_WON") || state.name().startsWith("BLACK_WON");
        boolean isDraw = state.name().startsWith("DRAW");

        if (isWin) {
            outcomeBadge.getStyleClass().add("outcome-badge-win");
        } else if (isDraw) {
            outcomeBadge.getStyleClass().add("outcome-badge-draw");
        }

        // --- Title ---
        if (isWin) {
            if (isOnline) {
                // Determine whether the LOCAL player is the winner.
                // localPlayerColor is null only in the unlikely case the facade returns null —
                // treat an unknown side as a loss to avoid a false "You Win!" message.
                boolean localPlayerWon = isLocalPlayerWinner(state, localPlayerColor);
                outcomeTitle.setText(localPlayerWon
                        ? i18n.get("game.end.title.win")
                        : i18n.get("game.end.title.loss"));
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

    /**
     * Returns the user's choice after the dialog has been dismissed.
     */
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

    /**
     * Returns {@code true} if the local player is the winner given the terminal game state
     * and the local player's color.
     *
     * <p>Used by online game mode to determine whether to show "You Win!" or "You Lose":
     * <ul>
     *   <li>If {@code localColor} is {@code null}, returns {@code false} (safe default —
     *       prefer "You Lose" over a false "You Win!" if the side is unknown).</li>
     *   <li>If {@code localColor} is {@code WHITE} and the state starts with
     *       {@code "WHITE_WON"}, returns {@code true}.</li>
     *   <li>If {@code localColor} is {@code BLACK} and the state starts with
     *       {@code "BLACK_WON"}, returns {@code true}.</li>
     * </ul>
     *
     * <p>Package-private for unit testing.
     *
     * @param state      the terminal game state; must be a win state ({@code WHITE_WON_*} or
     *                   {@code BLACK_WON_*}).
     * @param localColor the local player's color, or {@code null} if unknown.
     * @return {@code true} if the local player won, {@code false} otherwise.
     */
    static boolean isLocalPlayerWinner(GameState state, PlayerColor localColor) {
        if (localColor == null) return false;
        return (localColor == PlayerColor.WHITE && state.name().startsWith("WHITE_WON"))
                || (localColor == PlayerColor.BLACK && state.name().startsWith("BLACK_WON"));
    }

    private String resolveStateLabel() {
        try {
            return i18n.get("state." + state.name());
        } catch (MissingResourceException e) {
            return state.name();
        }
    }
}
