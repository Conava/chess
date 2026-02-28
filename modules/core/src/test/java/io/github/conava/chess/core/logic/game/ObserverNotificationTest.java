package io.github.conava.chess.core.logic.game;

import io.github.conava.chess.core.data.Square;
import io.github.conava.chess.core.exceptions.IllegalMoveException;
import io.github.conava.chess.core.logic.observer.GameObserver;
import io.github.conava.chess.core.logic.ruleset.RulesetOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the Task 3 fix: Game.executeMove() now calls notifyObservers() after
 * incrementing turnCount, so all registered GameObserver instances receive
 * onGameStateChanged() exactly once per successfully executed move.
 *
 * Uses OfflineGame (concrete, no networking) and the standard starting position.
 * A white pawn advance from e2 (y=1,x=4) to e3 (y=2,x=4) is used as the
 * canonical "first valid move" throughout.
 */
class ObserverNotificationTest {

    // The concrete OfflineGame exposes 'board' (protected) to subclasses;
    // here we simply use the standard starting position through public API.
    private OfflineGame game;

    @BeforeEach
    void setUp() {
        game = new OfflineGame(RulesetOptions.STANDARD, "Alice", "Bob");
        game.startGame();
    }

    @Test
    void observer_isNotifiedAfterValidMove() throws IllegalMoveException {
        CountingObserver observer = new CountingObserver();
        game.addObserver(observer);

        // White pawn e2 → e3 (y=1,x=4 → y=2,x=4)
        game.movePiece(new Square(1, 4), new Square(2, 4));

        assertEquals(1, observer.callCount,
                "onGameStateChanged() must be called exactly once after a valid move");
    }

    @Test
    void observer_isNotifiedExactlyOncePerMove() throws IllegalMoveException {
        CountingObserver observer = new CountingObserver();
        game.addObserver(observer);

        // Two moves: white e2→e3, then black e7→e6 (y=6,x=4 → y=5,x=4)
        game.movePiece(new Square(1, 4), new Square(2, 4));
        game.movePiece(new Square(6, 4), new Square(5, 4));

        assertEquals(2, observer.callCount,
                "onGameStateChanged() must be called once per executed move");
    }

    @Test
    void multipleObservers_allNotifiedAfterSingleMove() throws IllegalMoveException {
        CountingObserver obs1 = new CountingObserver();
        CountingObserver obs2 = new CountingObserver();
        CountingObserver obs3 = new CountingObserver();
        game.addObserver(obs1);
        game.addObserver(obs2);
        game.addObserver(obs3);

        game.movePiece(new Square(1, 4), new Square(2, 4));

        assertAll(
                () -> assertEquals(1, obs1.callCount, "Observer 1 must be notified"),
                () -> assertEquals(1, obs2.callCount, "Observer 2 must be notified"),
                () -> assertEquals(1, obs3.callCount, "Observer 3 must be notified")
        );
    }

    @Test
    void observer_isNotNotifiedWhenIllegalMoveAttempted() {
        CountingObserver observer = new CountingObserver();
        game.addObserver(observer);

        // White pawn cannot jump from e2 to e6 in one move
        assertThrows(IllegalMoveException.class,
                () -> game.movePiece(new Square(1, 4), new Square(5, 4)));

        assertEquals(0, observer.callCount,
                "onGameStateChanged() must not be called when a move is rejected");
    }

    @Test
    void removedObserver_isNotNotifiedAfterRemoval() throws IllegalMoveException {
        CountingObserver observer = new CountingObserver();
        game.addObserver(observer);
        game.removeObserver(observer);

        game.movePiece(new Square(1, 4), new Square(2, 4));

        assertEquals(0, observer.callCount,
                "Removed observer must not receive any notifications");
    }

    @Test
    void observer_notifiedAfterSecondMoveButNotAddedBeforeFirst() throws IllegalMoveException {
        CountingObserver observer = new CountingObserver();

        // First move with no observer yet
        game.movePiece(new Square(1, 4), new Square(2, 4));

        // Register observer after the first move
        game.addObserver(observer);

        // Second move
        game.movePiece(new Square(6, 4), new Square(5, 4));

        assertEquals(1, observer.callCount,
                "Observer registered after first move must only be notified from second move onwards");
    }

    // ---- simple counting observer ----

    private static class CountingObserver implements GameObserver {
        int callCount = 0;

        @Override
        public void onGameStateChanged() {
            callCount++;
        }
    }
}
