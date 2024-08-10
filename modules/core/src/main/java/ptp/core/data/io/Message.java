package ptp.core.data.io;

public record Message(MessageType type, String content) {
    public Message {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
    }
}