package ptp.server.management;

import ptp.core.logic.ruleset.RulesetOptions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The GameManager class is responsible for managing active game instances.
 * It allows for creating new games and joining existing games using a join code.
 */
public class GameManager {
    private final Map<String, GameInstance> activeGames = new ConcurrentHashMap<>();

    /**
     * Creates a new game with the specified ruleset and returns a unique join code.
     *
     * @param ruleset The ruleset options for the new game.
     * @return A unique join code for the new game.
     */
    public String createGame(RulesetOptions ruleset) {
        String joinCode = generateJoinCode();
        GameInstance gameInstance = new GameInstance();
        activeGames.put(joinCode, gameInstance);
        return joinCode;
    }

    /**
     * Joins an existing game using the provided join code.
     *
     * @param joinCode The join code of the game to join.
     * @return The GameInstance associated with the join code, or null if no such game exists.
     */
    public GameInstance joinGame(String joinCode) {
        return activeGames.get(joinCode);
    }

    /**
     * Generates a unique join code for a new game.
     *
     * @return A unique join code.
     */
    private String generateJoinCode() {
        return Long.toHexString(Double.doubleToLongBits(Math.random()));
    }
}