package io.github.conava.chess.server.matchmaking;

import io.github.conava.chess.core.data.io.Message;
import io.github.conava.chess.core.data.io.MessageType;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import io.github.conava.chess.server.management.ClientHandler;
import io.github.conava.chess.server.management.GameInstance;
import io.github.conava.chess.server.management.GameManager;
import io.github.conava.chess.server.management.PlayerSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages per-ruleset FIFO matchmaking queues and pairs players when two are waiting
 * for the same ruleset.
 *
 * <p>Players are placed in a queue by calling {@link #enqueue}. Immediately after
 * enqueueing, {@link #tryMatch} is called to see whether a pairing is now possible.
 * If two or more players are waiting for the same ruleset, the two oldest entries are
 * removed from the queue, a game slot is acquired from the {@link GameManager}, a new
 * {@link GameInstance} is created, and both players are connected to it. A
 * {@code MATCHED} message is then sent to both handlers so the clients know which game
 * to join.</p>
 *
 * <p>If the server has no free game slots at match time, an
 * {@code ERROR:Server full} message is sent to both polled players and neither is
 * re-enqueued.</p>
 *
 * <p>{@link #tryMatch} is {@code synchronized} to prevent a race condition in which
 * two threads both observe two entries in the queue and create two separate games from
 * the same pair of players.</p>
 *
 * <p>{@link #dequeue} removes a player from whichever ruleset queue they are currently
 * in. It is safe to call even if the player is not in any queue.</p>
 *
 * @since 0.9
 */
public class MatchmakingService {

    private static final Logger LOGGER = Logger.getLogger(MatchmakingService.class.getName());

    /**
     * Immutable record capturing a single queued player.
     *
     * @param handler the {@link ClientHandler} for the queued player
     * @param session the authenticated {@link PlayerSession} for the queued player;
     *                may be {@code null} if no session context is available
     */
    private record QueueEntry(ClientHandler handler, PlayerSession session) {}

    /** Per-ruleset FIFO queues. Created on demand when the first player enqueues. */
    private final Map<RulesetOptions, ConcurrentLinkedDeque<QueueEntry>> queues =
            new ConcurrentHashMap<>();

    private final GameManager gameManager;

    /**
     * Constructs a {@code MatchmakingService} backed by the given {@link GameManager}.
     *
     * @param gameManager the server-wide game manager used to acquire slots and register
     *                    new games; must not be {@code null}
     */
    public MatchmakingService(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Adds the given player to the matchmaking queue for the specified ruleset, then
     * immediately attempts to form a match.
     *
     * @param handler the {@link ClientHandler} representing the player's connection;
     *                must not be {@code null}
     * @param session the authenticated session for the player; may be {@code null}
     * @param ruleset the ruleset the player wants to play; must not be {@code null}
     */
    public void enqueue(ClientHandler handler, PlayerSession session, RulesetOptions ruleset) {
        ConcurrentLinkedDeque<QueueEntry> queue =
                queues.computeIfAbsent(ruleset, k -> new ConcurrentLinkedDeque<>());
        queue.addLast(new QueueEntry(handler, session));
        LOGGER.log(Level.INFO, "Player enqueued for {0}. Queue size: {1}",
                new Object[]{ruleset, queue.size()});
        tryMatch(ruleset);
    }

    /**
     * Removes the given {@link ClientHandler} from whichever ruleset queue it is currently
     * in, if any.
     *
     * <p>This method is safe to call even when the handler is not present in any queue;
     * in that case it is a no-op.</p>
     *
     * @param handler the handler to remove; must not be {@code null}
     */
    public void dequeue(ClientHandler handler) {
        for (Map.Entry<RulesetOptions, ConcurrentLinkedDeque<QueueEntry>> entry : queues.entrySet()) {
            boolean removed = entry.getValue().removeIf(e -> e.handler() == handler);
            if (removed) {
                LOGGER.log(Level.INFO, "Player dequeued from {0}", entry.getKey());
                return;
            }
        }
    }

    /**
     * Attempts to match the two oldest players in the queue for the given ruleset.
     *
     * <p>This method is {@code synchronized} to prevent concurrent threads from
     * both observing two entries and creating two separate games from the same pair.</p>
     *
     * <p>If the queue contains fewer than two entries, the method returns without any
     * side effects. If a game slot cannot be acquired (server full), an
     * {@code ERROR:Server full} message is sent to both polled players and they are
     * not re-enqueued.</p>
     *
     * @param ruleset the ruleset queue to inspect; must not be {@code null}
     */
    public synchronized void tryMatch(RulesetOptions ruleset) {
        ConcurrentLinkedDeque<QueueEntry> queue = queues.get(ruleset);
        if (queue == null || queue.size() < 2) {
            return;
        }

        QueueEntry first = queue.pollFirst();
        QueueEntry second = queue.pollFirst();

        if (first == null || second == null) {
            // Put back any that were polled if the other turned out to be null
            if (first != null) queue.addFirst(first);
            if (second != null) queue.addFirst(second);
            return;
        }

        if (!gameManager.tryAcquireGameSlot()) {
            LOGGER.log(Level.WARNING, "Matchmaking: server full — cannot create game for {0}", ruleset);
            first.handler().sendMessage(new Message(MessageType.ERROR, "Server full"));
            second.handler().sendMessage(new Message(MessageType.ERROR, "Server full"));
            return;
        }

        int gameId = gameManager.nextGameId();
        GameInstance gameInstance = new GameInstance(gameId, ruleset);
        gameManager.addGame(gameId, gameInstance);

        LOGGER.log(Level.INFO, "Matched players for {0}, gameId={1}", new Object[]{ruleset, gameId});

        // Build the MATCHED payload in JOIN_CODE format
        String matchedContent = buildMatchedContent(gameId, gameInstance);

        // Connect both players — connectPlayer sends SUCCESS internally
        gameInstance.connectPlayer(first.handler(), first.session());
        gameInstance.connectPlayer(second.handler(), second.session());

        // Send MATCHED to both so clients know they have been paired
        first.handler().sendMessage(new Message(MessageType.MATCHED, matchedContent));
        second.handler().sendMessage(new Message(MessageType.MATCHED, matchedContent));
    }

    /**
     * Builds the payload for the {@code MATCHED} message, mirroring the {@code JOIN_CODE}
     * payload format.
     *
     * <p>For standard chess the payload is {@code "joinCode=<gameId>"}. For Chess960 it
     * includes the position index and ruleset tag:
     * {@code "joinCode=<gameId> position=<N> ruleset=CHESS960"}.</p>
     *
     * @param gameId       the numeric game identifier assigned to the matched game
     * @param gameInstance the newly created game instance (used to read the position index)
     * @return the formatted payload string
     */
    private String buildMatchedContent(int gameId, GameInstance gameInstance) {
        String content = "joinCode=" + gameId;
        if (gameInstance.getPositionIndex() >= 0) {
            content += " position=" + gameInstance.getPositionIndex() + " ruleset=CHESS960";
        }
        return content;
    }
}
