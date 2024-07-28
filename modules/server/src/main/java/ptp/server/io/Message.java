package ptp.server.io;

public record Message(String type, String content) {

    @Override
    public String toString() {
        return type + " " + content;
    }

    public static Message fromString(String messageString) {
        String[] parts = messageString.split(" ", 2);
        return new Message(parts[0], parts[1]);
    }
}