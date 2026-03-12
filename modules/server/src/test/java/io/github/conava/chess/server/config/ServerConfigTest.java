package io.github.conava.chess.server.config;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ServerConfigTest {

    private ServerConfig configFrom(String... pairs) {
        Properties props = new Properties();
        for (int i = 0; i < pairs.length - 1; i += 2) {
            props.setProperty(pairs[i], pairs[i + 1]);
        }
        return new ServerConfig(props);
    }

    @Test
    void defaultsApplyWhenPropertiesAreEmpty() {
        ServerConfig cfg = configFrom();
        assertEquals(54321, cfg.getPort());
        assertEquals(40,    cfg.getMaxGames());
        assertEquals(300,   cfg.getDisconnectTimeoutSeconds());
        assertEquals("chess.db", cfg.getDbPath());
        assertEquals(30,    cfg.getSessionExpiryDays());
    }

    @Test
    void customValuesAreLoaded() {
        ServerConfig cfg = configFrom(
                "port",                     "12345",
                "max_games",                "10",
                "disconnect_timeout_seconds","60",
                "db_path",                  "/var/chess/data.db",
                "session_expiry_days",      "7"
        );
        assertEquals(12345, cfg.getPort());
        assertEquals(10,    cfg.getMaxGames());
        assertEquals(60,    cfg.getDisconnectTimeoutSeconds());
        assertEquals("/var/chess/data.db", cfg.getDbPath());
        assertEquals(7,     cfg.getSessionExpiryDays());
    }

    @Test
    void invalidIntegerFallsBackToDefault() {
        ServerConfig cfg = configFrom("port", "not-a-number");
        assertEquals(54321, cfg.getPort());
    }
}
