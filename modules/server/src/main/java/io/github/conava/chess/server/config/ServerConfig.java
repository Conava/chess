package io.github.conava.chess.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Loads server configuration from {@code server.properties} on the classpath.
 * All values have safe defaults so the server runs even when the file is absent.
 */
public class ServerConfig {

    private static final Logger LOG = Logger.getLogger(ServerConfig.class.getName());
    private static final String RESOURCE_PATH = "server.properties";

    private final int port;
    private final int maxGames;
    private final int disconnectTimeoutSeconds;
    private final String dbPath;
    private final int sessionExpiryDays;

    public ServerConfig() {
        Properties props = new Properties();
        try (InputStream in = ServerConfig.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            if (in != null) {
                props.load(in);
            } else {
                LOG.warning("server.properties not found on classpath — using defaults");
            }
        } catch (IOException e) {
            LOG.warning("Failed to read server.properties: " + e.getMessage() + " — using defaults");
        }

        this.port                    = parseInt(props, "port",                     54321);
        this.maxGames                = parseInt(props, "max_games",                40);
        this.disconnectTimeoutSeconds= parseInt(props, "disconnect_timeout_seconds", 300);
        this.dbPath                  = props.getProperty("db_path",               "chess.db");
        this.sessionExpiryDays       = parseInt(props, "session_expiry_days",      30);
    }

    /**
     * Constructs a {@code ServerConfig} by injecting properties directly.
     *
     * <p>Intended for tests and other programmatic construction where loading from a
     * classpath resource is not desired. All standard keys and defaults apply.</p>
     *
     * @param props the properties to read; unrecognised keys are ignored
     */
    public ServerConfig(Properties props) {
        this.port                     = parseInt(props, "port",                     54321);
        this.maxGames                 = parseInt(props, "max_games",                40);
        this.disconnectTimeoutSeconds = parseInt(props, "disconnect_timeout_seconds", 300);
        this.dbPath                   = props.getProperty("db_path",               "chess.db");
        this.sessionExpiryDays        = parseInt(props, "session_expiry_days",      30);
    }

    private static int parseInt(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            LOG.warning("Invalid integer for '" + key + "': " + value + " — using default " + defaultValue);
            return defaultValue;
        }
    }

    public int getPort()                     { return port; }
    public int getMaxGames()                 { return maxGames; }
    public int getDisconnectTimeoutSeconds() { return disconnectTimeoutSeconds; }
    public String getDbPath()                { return dbPath; }
    public int getSessionExpiryDays()        { return sessionExpiryDays; }
}
