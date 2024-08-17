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

    /**
     * Returns the value of the specified parameter in the message content.
     * Contract: The content is a string of key-value pairs separated by spaces. Each key-value pair is separated by an equals sign. All parameters and values are lowercase.
     * @param parameter The parameter to get the value of
     * @return The value of the parameter, or null if the parameter is not found
     */
    public String getParameterValue(String parameter) {
        String[] keyValuePairs = content.split(" ");
        for (String pair : keyValuePairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(parameter)) {
                return keyValue[1];
            }
        }
        return null; // Return null if the parameter is not found
    }
}