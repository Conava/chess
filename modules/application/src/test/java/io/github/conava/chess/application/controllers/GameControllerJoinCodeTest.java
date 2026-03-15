package io.github.conava.chess.application.controllers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the static utility methods in {@link GameController} that can be exercised
 * without initialising the JavaFX toolkit.
 *
 * <p>In particular, these tests cover {@link GameController#formatChatMessage(String)}, which
 * parses the server-side {@code "sender=<name> content=<text>"} chat format and produces a
 * human-readable {@code "Name: message"} string.</p>
 */
class GameControllerJoinCodeTest {

    /**
     * Full happy-path: both {@code sender} and {@code content} parameters present.
     * Expected output is {@code "Alice: hello world"}.
     */
    @Test
    void parseChatDisplay_extractsSenderAndContent() {
        String result = GameController.formatChatMessage("sender=Alice content=hello world");
        assertEquals("Alice: hello world", result);
    }

    /**
     * Only {@code content} is present (no {@code sender} parameter).
     * The method should fall back gracefully and return just the content text.
     */
    @Test
    void parseChatDisplay_handlesMissingSender() {
        String result = GameController.formatChatMessage("content=hello");
        assertEquals("hello", result);
    }

    /**
     * Raw text with no key=value structure at all.
     * The method should return the input unchanged.
     */
    @Test
    void parseChatDisplay_handlesRawText() {
        String result = GameController.formatChatMessage("hello");
        assertEquals("hello", result);
    }

    /**
     * Content itself contains an {@code '='} character (e.g. {@code "x=5"}).
     * Everything after the {@code content=} key must be preserved, including embedded equals signs.
     */
    @Test
    void parseChatDisplay_handlesContentWithEquals() {
        String result = GameController.formatChatMessage("sender=Bob content=x=5");
        assertEquals("Bob: x=5", result);
    }

    /**
     * Multi-word sender name (unusual but should not crash) — sender is parsed up to the
     * first space after {@code sender=}, so the result follows the spec even for edge inputs.
     */
    @Test
    void parseChatDisplay_multiWordContentPreserved() {
        String result = GameController.formatChatMessage("sender=Alice content=how are you doing?");
        assertEquals("Alice: how are you doing?", result);
    }
}
