package io.github.conava.chess.server.management;

import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.data.io.MessageType;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for {@link GameInstance}.
 *
 * <p>Covers the defensive exception handling introduced in Task 7: malformed move messages
 * that would cause {@code Move.fromString()} to throw a {@code RuntimeException} must not
 * propagate out of {@code processMessage()} and must not crash the handler thread.</p>
 */
class GameInstanceTest {

    private GameInstance gameInstance;

    @BeforeEach
    void setUp() {
        gameInstance = new GameInstance(1, RulesetOptions.STANDARD);
        // No players are connected — sendMessageToPlayers() will be a no-op.
    }

    // ---- Task 7: malformed move must not crash the handler thread ----

    /**
     * A MOVE message with a garbled {@code move} parameter must be caught internally
     * and must not throw any exception out of {@code processMessage()}.
     *
     * <p>The {@code clientHandler} argument is {@code null} because the MOVE dispatch
     * path in {@code handleMove()} never uses the handler reference.</p>
     */
    @Test
    void handleMove_malformedMove_doesNotThrow() {
        Message malformedMsg = new Message(MessageType.MOVE,
                "move=!!NOT_A_VALID_MOVE!! playerColor=WHITE");
        assertDoesNotThrow(
                () -> gameInstance.processMessage(null, malformedMsg),
                "A malformed MOVE message must not propagate any exception out of processMessage()");
    }

    /**
     * A MOVE message whose {@code move} parameter is completely missing (null from
     * {@code getParameterValue}) must also be handled without throwing.
     *
     * <p>{@code Move.fromString()} receives {@code Objects.requireNonNull(null)}, which
     * throws {@code NullPointerException} — this must be caught as a {@code RuntimeException}.</p>
     */
    @Test
    void handleMove_nullMoveParam_doesNotThrow() {
        // "playerColor=WHITE" only — no "move" key present
        Message noMoveMsg = new Message(MessageType.MOVE, "playerColor=WHITE");
        assertDoesNotThrow(
                () -> gameInstance.processMessage(null, noMoveMsg),
                "A MOVE message with a missing move parameter must not propagate any exception");
    }
}
