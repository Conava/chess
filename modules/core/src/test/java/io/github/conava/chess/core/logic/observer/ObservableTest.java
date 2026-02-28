package io.github.conava.chess.core.logic.observer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Observable}.
 *
 * <p>Covers null-guard enforcement on {@code addObserver}/{@code removeObserver} and
 * thread-safety of {@code notifyObservers()} against concurrent mutations of the
 * observer list.</p>
 */
class ObservableTest {

    /**
     * Minimal concrete subclass of {@link Observable} used only for testing.
     */
    static class TestObservable extends Observable {
        // No additional state; all behaviour under test lives in Observable.
    }

    /**
     * No-op {@link GameObserver} stub used by the concurrent test.
     */
    static class NoOpObserver implements GameObserver {
        @Override
        public void onGameStateChanged() {
            // intentionally empty
        }
    }

    private TestObservable observable;

    @BeforeEach
    void setUp() {
        observable = new TestObservable();
    }

    // ---- null-guard tests ----

    @Test
    void addObserver_nullThrowsNPE() {
        assertThrows(NullPointerException.class,
                () -> observable.addObserver(null),
                "addObserver(null) must throw NullPointerException");
    }

    @Test
    void removeObserver_nullThrowsNPE() {
        assertThrows(NullPointerException.class,
                () -> observable.removeObserver(null),
                "removeObserver(null) must throw NullPointerException");
    }

    // ---- concurrency test ----

    /**
     * Verifies that {@code notifyObservers()} does not throw a
     * {@link java.util.ConcurrentModificationException} (or any other exception)
     * when another thread concurrently adds observers while notification is in progress.
     *
     * <p>The test seeds one observer whose {@code onGameStateChanged()} signals the
     * writer thread to begin adding observers, then waits for the writer to finish
     * before returning. If the underlying list were a plain {@link java.util.ArrayList}
     * this would be racy; with {@link java.util.concurrent.CopyOnWriteArrayList} it is
     * safe by contract.</p>
     */
    @Test
    void notifyObservers_concurrentAddDoesNotThrow() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(1);
        AtomicBoolean writerFailed = new AtomicBoolean(false);

        // Register an observer that lets the writer thread run while we are iterating.
        observable.addObserver(() -> {
            startLatch.countDown();  // signal the writer to start
            try {
                doneLatch.await();   // wait until writer is done before returning
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Writer thread: concurrently adds new observers while notifyObservers() iterates.
        Thread writer = new Thread(() -> {
            try {
                startLatch.await();  // wait until iteration has started
                for (int i = 0; i < 50; i++) {
                    observable.addObserver(new NoOpObserver());
                }
            } catch (Exception e) {
                writerFailed.set(true);
            } finally {
                doneLatch.countDown();  // unblock the observer callback
            }
        });
        writer.start();

        // This must not throw.
        assertDoesNotThrow(() -> observable.notifyObservers(),
                "notifyObservers() must not throw when observers are added concurrently");

        writer.join(5_000);
        assertFalse(writerFailed.get(), "Writer thread must not encounter any exception");
    }
}
