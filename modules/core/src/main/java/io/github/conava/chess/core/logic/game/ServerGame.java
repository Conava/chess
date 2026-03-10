package io.github.conava.chess.core.logic.game;

import io.github.conava.chess.core.logic.ruleset.Ruleset;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;

/**
 * Concrete {@link Game} subclass managed entirely by the server-side process.
 *
 * <p>{@code ServerGame} is instantiated exclusively through the
 * {@link Game#createServerGame(RulesetOptions, String, String)} factory method.
 * Direct construction from outside the {@code core} module is prohibited by
 * Architecture Law 2. The game begins in an uninitialised state; callers must
 * invoke {@link #startGame()} before accepting moves.
 */
public class ServerGame extends Game {

    /**
     * Constructs a {@code ServerGame} with the given ruleset and player names.
     *
     * <p>This constructor has package-private visibility; external callers must use
     * {@link Game#createServerGame(RulesetOptions, String, String)} to obtain an
     * instance. Both player-name arguments are forwarded to the {@link Game}
     * superclass, which applies default names when the supplied strings are blank.
     *
     * @param selectedRuleset  The ruleset to use for this game; must not be {@code null}.
     * @param playerWhiteName  Display name for the white player; blank strings receive
     *                         the default name defined by {@link Game}.
     * @param playerBlackName  Display name for the black player; blank strings receive
     *                         the default name defined by {@link Game}.
     */
    ServerGame(RulesetOptions selectedRuleset, String playerWhiteName, String playerBlackName) {
        super(selectedRuleset, playerWhiteName, playerBlackName);
    }

    /**
     * Constructs a {@code ServerGame} with a pre-built {@link Ruleset} instance.
     *
     * <p>Used when the server module already holds a fully configured ruleset (e.g. a
     * {@code Chess960Ruleset} for a known Scharnagl index). The supplied ruleset is used
     * directly without any {@code createRuleset} lookup.
     *
     * <p>This constructor has package-private visibility; external callers must use
     * {@link Game#createServerGame(Ruleset, String, String)} to obtain an instance.
     *
     * @param ruleset          The pre-built ruleset; must not be {@code null}.
     * @param playerWhiteName  Display name for the white player.
     * @param playerBlackName  Display name for the black player.
     */
    ServerGame(Ruleset ruleset, String playerWhiteName, String playerBlackName) {
        super(ruleset, playerWhiteName, playerBlackName);
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