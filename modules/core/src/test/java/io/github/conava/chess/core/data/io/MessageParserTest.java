package io.github.conava.chess.core.data.io;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MessageParser} and the non-trivial behaviour of {@link Message}.
 *
 * <p>Covered behaviours:
 * <ul>
 *   <li>{@code MessageParser.parse(String)} correctly splits on the first colon and maps the
 *       left-hand side to a {@link MessageType}.</li>
 *   <li>{@code MessageParser.serialize(Message)} reconstructs the original wire string.</li>
 *   <li>Parse-then-serialize and serialize-then-parse are mutual inverses (round-trip).</li>
 *   <li>{@code Message.getParameterValue(String)} returns the correct value for a key that
 *       is present and {@code null} for a key that is absent.</li>
 *   <li>{@code Message} rejects {@code null} type or {@code null} content at construction.</li>
 * </ul>
 */
class MessageParserTest {

    // -------------------------------------------------------------------------
    // MessageParser.parse
    // -------------------------------------------------------------------------

    @Test
    void parse_extractsCorrectMessageType() {
        Message msg = MessageParser.parse("MOVE:from=e2 to=e4");
        assertEquals(MessageType.MOVE, msg.type(),
                "parse() must map the prefix to the correct MessageType");
    }

    @Test
    void parse_extractsCorrectContent() {
        Message msg = MessageParser.parse("MOVE:from=e2 to=e4");
        assertEquals("from=e2 to=e4", msg.content(),
                "parse() must preserve the content portion unchanged");
    }

    @Test
    void parse_handlesEmptyContent() {
        Message msg = MessageParser.parse("SUCCESS:");
        assertEquals(MessageType.SUCCESS, msg.type());
        assertEquals("", msg.content(),
                "parse() must accept an empty content string after the colon");
    }

    @Test
    void parse_contentWithColonIsNotFurtherSplit() {
        // split(":", 2) means only the FIRST colon separates type from content
        Message msg = MessageParser.parse("GAME_STATUS:key=val:extra");
        assertEquals(MessageType.GAME_STATUS, msg.type());
        assertEquals("key=val:extra", msg.content(),
                "parse() must not split on colons inside the content portion");
    }

    @Test
    void parse_unknownMessageTypeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> MessageParser.parse("UNKNOWN:something"),
                "parse() must throw when the type prefix is not a valid MessageType name");
    }

    // -------------------------------------------------------------------------
    // MessageParser.serialize
    // -------------------------------------------------------------------------

    @Test
    void serialize_producesCorrectWireString() {
        Message msg = new Message(MessageType.MOVE, "from=e2 to=e4");
        assertEquals("MOVE:from=e2 to=e4", MessageParser.serialize(msg),
                "serialize() must produce '<TYPE>:<content>'");
    }

    @Test
    void serialize_emptyContentProducesTrailingColon() {
        Message msg = new Message(MessageType.SUCCESS, "");
        assertEquals("SUCCESS:", MessageParser.serialize(msg),
                "serialize() of empty-content message must end with a colon");
    }

    // -------------------------------------------------------------------------
    // Round-trip: parse -> serialize -> parse
    // -------------------------------------------------------------------------

    @Test
    void parseAndSerialize_roundTrip() {
        String wire = "JOIN_CODE:code=ABC123";
        Message parsed = MessageParser.parse(wire);
        String serialized = MessageParser.serialize(parsed);
        assertEquals(wire, serialized,
                "serialize(parse(wire)) must reproduce the original wire string");
    }

    @Test
    void serializeAndParse_roundTrip() {
        Message original = new Message(MessageType.CREATE_GAME, "ruleset=STANDARD");
        String wire = MessageParser.serialize(original);
        Message reparsed = MessageParser.parse(wire);
        assertEquals(original.type(), reparsed.type(),
                "parse(serialize(msg)) must reproduce the original MessageType");
        assertEquals(original.content(), reparsed.content(),
                "parse(serialize(msg)) must reproduce the original content");
    }

    // -------------------------------------------------------------------------
    // Message.getParameterValue
    // -------------------------------------------------------------------------

    @Test
    void getParameterValue_returnsCorrectValueForExistingKey() {
        Message msg = new Message(MessageType.MOVE, "from=e2 to=e4");
        assertEquals("e2", msg.getParameterValue("from"),
                "getParameterValue('from') must return 'e2'");
    }

    @Test
    void getParameterValue_returnsCorrectValueForSecondKey() {
        Message msg = new Message(MessageType.MOVE, "from=e2 to=e4");
        assertEquals("e4", msg.getParameterValue("to"),
                "getParameterValue('to') must return 'e4'");
    }

    @Test
    void getParameterValue_returnsNullForMissingKey() {
        Message msg = new Message(MessageType.MOVE, "from=e2 to=e4");
        assertNull(msg.getParameterValue("piece"),
                "getParameterValue() must return null when the key is not present");
    }

    @Test
    void getParameterValue_emptyContentReturnsNull() {
        Message msg = new Message(MessageType.SUCCESS, "");
        assertNull(msg.getParameterValue("anything"),
                "getParameterValue() on empty content must return null");
    }

    @Test
    void getParameterValue_singlePair() {
        Message msg = new Message(MessageType.JOIN_CODE, "code=XYZ99");
        assertEquals("XYZ99", msg.getParameterValue("code"),
                "getParameterValue() must work with a single key-value pair");
    }

    // -------------------------------------------------------------------------
    // Message construction guards
    // -------------------------------------------------------------------------

    @Test
    void message_nullTypeThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Message(null, "content"),
                "Message constructor must reject a null type");
    }

    @Test
    void message_nullContentThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Message(MessageType.SUCCESS, null),
                "Message constructor must reject null content");
    }
}
