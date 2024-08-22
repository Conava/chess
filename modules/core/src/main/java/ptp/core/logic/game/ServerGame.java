package ptp.core.logic.game;

import ptp.core.logic.ruleset.RulesetOptions;

/**
 * The ServerGame class represents a game that is managed by a server.
 * It extends the Game class and provides implementations for starting and ending the game.
 */
public class ServerGame extends Game {

    /**
     * Constructs a ServerGame instance.
     *
     * @param selectedRuleset The ruleset options for the game.
     */
    public ServerGame(RulesetOptions selectedRuleset) {
        super(selectedRuleset, "Player 1", "Player 2");
        this.gameType = GameType.SERVER;
    }

    /**
     * Starts the server game by setting the game state to RUNNING.
     */
    @Override
    public void startGame() {
        gameState = GameState.RUNNING;
    }

    /**
     * Ends the server game by setting the game state to ENDED.
     */
    @Override
    public void endGame() {
        // todo: Not sure if implementation is needed here
    }
}