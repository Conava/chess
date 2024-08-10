package ptp.core.data.io;

public class MessageParser {
    public static Message parse(String input) {
        String[] parts = input.split(":", 2);
        MessageType type = MessageType.valueOf(parts[0]);
        String content = parts[1];
        return new Message(type, content);
    }

    public static String serialize(Message message) {
        return message.type().name() + ":" + message.content();
    }
}