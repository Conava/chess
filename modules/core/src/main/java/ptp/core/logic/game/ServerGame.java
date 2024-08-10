package ptp.core.logic.game;

import ptp.core.data.Square;
import ptp.core.logic.ruleset.RulesetOptions;
import ptp.core.exceptions.IllegalMoveException;
import ptp.core.data.io.Message;
import ptp.core.logic.moves.Move;

public class ServerGame extends Game{

    /**
     * Constructor for the Game class.
     *
     * @param selectedRuleset The ruleset to be used for the game.
     */
    public ServerGame(RulesetOptions selectedRuleset) {
        super(selectedRuleset, "Player 1", "Player 2");
        this.gameType = GameType.SERVER;
    }

    @Override
    public void startGame() {

    }

    @Override
    public void endGame() {
    }

    @Override
    protected void executeMove(Move move) throws IllegalMoveException {

    }

    public void processMessage(Message message) {
    }
}
