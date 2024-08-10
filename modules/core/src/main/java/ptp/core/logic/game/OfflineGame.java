package ptp.core.logic.game;

import ptp.core.logic.ruleset.RulesetOptions;

public class OfflineGame extends Game {

    public OfflineGame(RulesetOptions selectedRuleset, String playerWhiteName, String playerBlackName) {
        super(selectedRuleset, playerWhiteName, playerBlackName);
    }

    @Override
    public void startGame() {
        gameState = GameState.RUNNING;
    }

    @Override
    public void endGame() {
    }
}