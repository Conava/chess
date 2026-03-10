package io.github.conava.chess.core.data.io;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageTypeTest {

    @Test
    void allExpectedValuesExist() {
        assertEquals(19, MessageType.values().length);
        assertNotNull(MessageType.valueOf("REGISTER"));
        assertNotNull(MessageType.valueOf("LOGIN"));
        assertNotNull(MessageType.valueOf("AUTH_TOKEN"));
        assertNotNull(MessageType.valueOf("RESUME_GAME"));
        assertNotNull(MessageType.valueOf("GAME_HISTORY"));
        assertNotNull(MessageType.valueOf("CHAT"));
        assertNotNull(MessageType.valueOf("QUEUE"));
        assertNotNull(MessageType.valueOf("DEQUEUE"));
        assertNotNull(MessageType.valueOf("MATCHED"));
        assertNotNull(MessageType.valueOf("SAVE_GAME"));
        assertNotNull(MessageType.valueOf("SAVE_ACCEPTED"));
    }

    @Test
    void parseRoundTrip_newTypes() {
        for (MessageType type : new MessageType[]{
                MessageType.REGISTER, MessageType.LOGIN, MessageType.AUTH_TOKEN,
                MessageType.RESUME_GAME, MessageType.GAME_HISTORY, MessageType.CHAT,
                MessageType.QUEUE, MessageType.DEQUEUE, MessageType.MATCHED,
                MessageType.SAVE_GAME, MessageType.SAVE_ACCEPTED}) {
            Message original = new Message(type, "key=value");
            String serialized = MessageParser.serialize(original);
            Message parsed = MessageParser.parse(serialized);
            assertEquals(type, parsed.type(), "Round-trip failed for " + type);
        }
    }

    @Test
    void existingValuesUnchanged() {
        assertSame(MessageType.CREATE_GAME, MessageType.values()[0]);
        assertSame(MessageType.JOIN_GAME,   MessageType.values()[1]);
        assertSame(MessageType.JOIN_CODE,   MessageType.values()[2]);
        assertSame(MessageType.MOVE,        MessageType.values()[3]);
        assertSame(MessageType.GAME_STATUS, MessageType.values()[4]);
        assertSame(MessageType.SUCCESS,     MessageType.values()[5]);
        assertSame(MessageType.ERROR,       MessageType.values()[6]);
        assertSame(MessageType.FAILURE,     MessageType.values()[7]);
    }
}
