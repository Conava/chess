package ptp.core.logic.game;

import ptp.core.logic.ruleset.RulesetOptions;

/**
 * The OfflineGame class represents a game that is played offline.
 * It extends the Game class and provides implementations for starting and ending the game.
 */
public class OfflineGame extends Game {

    /**
     * Constructs an OfflineGame instance.
     *
     * @param selectedRuleset The ruleset options for the game.
     * @param playerWhiteName The name of the white player.
     * @param playerBlackName The name of the black player.
     */
    public OfflineGame(RulesetOptions selectedRuleset, String playerWhiteName, String playerBlackName) {
        super(selectedRuleset, playerWhiteName, playerBlackName);
    }

    /**
     * Starts the offline game by setting the game state to RUNNING.
     */
    @Override
    public void startGame() {
        gameState = GameState.RUNNING;
    }

    /**
     * Ends the offline game. No special conditions are required to end a local game.
     */
    @Override
    public void endGame() {
        // Local game can be ended without any special conditions therefore no implementation is needed.
    }
}