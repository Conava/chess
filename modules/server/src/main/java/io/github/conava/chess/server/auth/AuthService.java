package io.github.conava.chess.server.auth;

import io.github.conava.chess.server.management.PlayerSession;
import io.github.conava.chess.server.persistence.SessionRepository;
import io.github.conava.chess.server.persistence.UserRepository;
import io.github.conava.chess.server.persistence.UserRepository.UserRecord;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Provides user authentication operations: registration, login, token verification,
 * and logout.
 *
 * <p>Passwords are hashed with PBKDF2-HMAC-SHA256 (600,000 iterations, 256-bit key,
 * 128-bit random salt). Salts and hashes are stored as Base64-encoded strings in
 * {@link UserRepository}.</p>
 *
 * <p>Session tokens are random UUIDs stored in {@link SessionRepository} with a
 * configurable expiry period. Token comparisons use constant-time comparison via
 * {@link MessageDigest#isEqual} to resist timing attacks on credential checks.</p>
 *
 * <p>This class is not thread-safe by itself; callers are responsible for
 * synchronisation if multiple threads share one instance.</p>
 *
 * @since 0.9
 */
public class AuthService {

    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 600_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final int sessionExpiryDays;
    private final SecureRandom secureRandom;

    /**
     * Constructs an {@code AuthService}.
     *
     * @param userRepository    the repository used to persist user records
     * @param sessionRepository the repository used to persist session tokens
     * @param sessionExpiryDays number of days before a newly created session expires;
     *                          must be &gt;= 0
     */
    public AuthService(UserRepository userRepository,
                       SessionRepository sessionRepository,
                       int sessionExpiryDays) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.sessionExpiryDays = sessionExpiryDays;
        this.secureRandom = new SecureRandom();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Registers a new user with the given credentials.
     *
     * <p>Validates that {@code username} is non-blank and {@code password} is at least
     * {@value #MIN_PASSWORD_LENGTH} characters long. Generates a random salt, hashes
     * the password with PBKDF2, persists the user, creates a session, and returns the
     * session token.</p>
     *
     * @param username the desired login name; must not be blank
     * @param password the plain-text password; must be at least 8 characters
     * @return a UUID session token for the newly created session
     * @throws IllegalArgumentException if {@code username} is blank, {@code password} is
     *                                  too short, or a user with {@code username} already exists
     * @throws SQLException             if a database error occurs
     */
    public String register(String username, String password) throws SQLException {
        validateUsername(username);
        validatePassword(password);

        byte[] saltBytes = generateSalt();
        String saltBase64 = Base64.getEncoder().encodeToString(saltBytes);
        String hashBase64 = hashPassword(password, saltBytes);

        int userId = userRepository.createUser(username, hashBase64, saltBase64);

        String token = UUID.randomUUID().toString();
        sessionRepository.createSession(userId, token, sessionExpiryDays);
        return token;
    }

    /**
     * Authenticates a user with the given credentials and creates a new session.
     *
     * <p>Looks up the user by {@code username}, verifies the supplied {@code password}
     * against the stored hash using constant-time comparison, creates a new session,
     * and returns the session token.</p>
     *
     * @param username the login name of the user
     * @param password the plain-text password to verify
     * @return a UUID session token for the newly created session
     * @throws IllegalArgumentException if {@code username} is unknown or {@code password}
     *                                  does not match the stored hash
     * @throws SQLException             if a database error occurs
     */
    public String login(String username, String password) throws SQLException {
        Optional<UserRecord> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        UserRecord user = userOpt.get();

        byte[] storedSalt = Base64.getDecoder().decode(user.passwordSalt());
        String computedHash = hashPassword(password, storedSalt);
        byte[] storedHashBytes = Base64.getDecoder().decode(user.passwordHash());
        byte[] computedHashBytes = Base64.getDecoder().decode(computedHash);

        if (!MessageDigest.isEqual(storedHashBytes, computedHashBytes)) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = UUID.randomUUID().toString();
        sessionRepository.createSession(user.id(), token, sessionExpiryDays);
        return token;
    }

    /**
     * Verifies a session token and returns the associated {@link PlayerSession} if valid.
     *
     * <p>Returns {@link Optional#empty()} when {@code token} is {@code null}, blank,
     * unknown, or expired.</p>
     *
     * @param token the session token to validate
     * @return an {@link Optional} containing a {@link PlayerSession} populated with
     *         {@code userId}, {@code username}, and {@code authToken}, or empty if invalid
     * @throws SQLException if a database error occurs
     */
    public Optional<PlayerSession> verifyToken(String token) throws SQLException {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        if (!sessionRepository.isTokenValid(token)) {
            return Optional.empty();
        }

        Optional<SessionRepository.SessionRecord> sessionOpt = sessionRepository.findByToken(token);
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }

        int userId = sessionOpt.get().userId();
        Optional<UserRecord> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        PlayerSession session = new PlayerSession();
        session.setUserId(userId);
        session.setUsername(userOpt.get().username());
        session.setAuthToken(token);
        return Optional.of(session);
    }

    /**
     * Invalidates the session identified by {@code token}.
     *
     * <p>If no such token exists, this method is a no-op.</p>
     *
     * @param token the session token to invalidate; {@code null} is silently ignored
     * @throws SQLException if a database error occurs
     */
    public void logout(String token) throws SQLException {
        sessionRepository.deleteByToken(token);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Validates that {@code username} is non-null and non-blank.
     *
     * @param username the value to validate
     * @throws IllegalArgumentException if {@code username} is null or blank
     */
    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
    }

    /**
     * Validates that {@code password} meets the minimum length requirement.
     *
     * @param password the value to validate
     * @throws IllegalArgumentException if {@code password} is null or shorter than
     *                                  {@value #MIN_PASSWORD_LENGTH} characters
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }
    }

    /**
     * Generates a random {@value #SALT_BYTES}-byte salt using {@link SecureRandom}.
     *
     * @return a new random salt byte array
     */
    private byte[] generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        return salt;
    }

    /**
     * Hashes {@code password} with PBKDF2-HMAC-SHA256 using the given {@code salt}.
     *
     * @param password  the plain-text password to hash
     * @param saltBytes the salt bytes to use
     * @return the Base64-encoded derived key
     * @throws IllegalStateException if the PBKDF2 algorithm or key spec is unavailable
     *                               (should not occur on standard JVMs)
     */
    private String hashPassword(String password, byte[] saltBytes) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    saltBytes,
                    ITERATIONS,
                    KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 hashing failed: " + e.getMessage(), e);
        }
    }
}
