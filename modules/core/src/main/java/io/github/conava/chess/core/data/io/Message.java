package io.github.conava.chess.core.data.io;

public record Message(MessageType type, String content) {
    public Message {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
    }

    /**
     * Returns the value of the specified parameter in the message content.
     *
     * <p>Contract: The content is a string of space-separated key-value pairs.
     * Each pair uses the <em>first</em> equals sign as the delimiter between key and value;
     * subsequent equals signs are treated as part of the value and are preserved verbatim.
     * This means values may themselves contain {@code =} characters (e.g. promotion moves
     * produce values such as {@code "a7-a8=QUEEN"}).
     * Parameter keys are case-sensitive.
     *
     * @param parameter the parameter name to look up
     * @return the value associated with {@code parameter}, or {@code null} if the parameter
     *         is not present in the content
     */
    public String getParameterValue(String parameter) {
        String[] keyValuePairs = content.split(" ");
        for (String pair : keyValuePairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2 && keyValue[0].equals(parameter)) {
                return keyValue[1];
            }
        }
        return null; // Return null if the parameter is not found
    }
}