/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.landawn.abacus.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * WS-Security utility methods for cryptographic operations commonly used in web service security.
 * This class provides thread-safe implementations for generating cryptographically secure nonces,
 * computing SHA-1 digests, and creating password digests according to WS-Security standards.
 *
 * <p>The class uses a cached, thread-safe {@link SecureRandom} instance for nonce generation,
 * and allocates a fresh {@link MessageDigest} per call for digest operations to avoid contention.
 * All public methods are thread-safe and may be invoked concurrently.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Cryptographically secure nonce generation using SHA1PRNG</li>
 *   <li>SHA-1 digest computation (default) and configurable algorithms via overloads (e.g. SHA-256)</li>
 *   <li>WS-Security compliant password digest creation</li>
 *   <li>Thread-safe implementation for concurrent environments</li>
 * </ul>
 *
 * <p><b>Security Note:</b> The default digest methods in this class use SHA-1, which is
 * cryptographically broken for collision resistance. SHA-1 is retained as the default for
 * backward compatibility with the WS-Security UsernameToken Profile 1.0 specification.
 * For new applications or when the WS-Security peer supports it, prefer the overloaded
 * methods that accept an algorithm parameter (e.g. {@code "SHA-256"}).</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Generate a nonce
 * byte[] nonce = WSSecurityUtil.generateNonce(16);
 *
 * // Create WS-Security password digest
 * String timestamp = getCurrentTimestamp();
 * String passwordDigest = WSSecurityUtil.computePasswordDigest(
 *     nonce, timestamp.getBytes(StandardCharsets.UTF_8), password.getBytes(StandardCharsets.UTF_8)
 * );
 * }</pre>
 *
 * <p>Note: This class is adapted from Apache WSS4J developed at The Apache Software Foundation
 * under the Apache License 2.0. The methods may have been modified from their original implementation.</p>
 *
 * @author Davanum Srinivas (dims@yahoo.com)
 */
public final class WSSecurityUtil {

    private static final String RNG_ALGORITHM = "SHA1PRNG";

    private static final String HASH_ALGORITHM = "SHA-1";

    /**
     * A cached pseudo-random number generator NB. On some JVMs, caching this random number generator is required to
     * overcome punitive overhead.
     */
    private static final SecureRandom random;

    static {
        try {
            random = SecureRandom.getInstance(RNG_ALGORITHM);
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to initialize SecureRandom with algorithm: " + RNG_ALGORITHM, e);
        }
    }

    private WSSecurityUtil() {
        // Complete
    }

    /** Cap to prevent a hostile-length call from monopolizing the shared SecureRandom instance (whose nextBytes is internally synchronized) and from allocating a giant array. */
    private static final int MAX_NONCE_LENGTH = 1024;

    /**
     * Generates a cryptographically secure nonce (number used once) of the specified length
     * using the SHA1PRNG algorithm. The nonce is suitable for use in security protocols where
     * unpredictable random values are required, such as WS-Security authentication headers.
     *
     * <p>This method relies on the thread-safety of the cached {@link SecureRandom} instance
     * (which is cached for improved performance across multiple calls while maintaining cryptographic strength).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate a 16-byte nonce for WS-Security
     * byte[] nonce = WSSecurityUtil.generateNonce(16);
     * String base64Nonce = Base64.getEncoder().encodeToString(nonce);
     * }</pre>
     *
     * @param length the length of the nonce to be generated in bytes, must be non-negative
     *               and not exceed {@code 1024}
     * @return a byte array containing cryptographically secure random bytes of the specified length
     * @throws IllegalArgumentException if {@code length} is negative or exceeds {@code 1024}
     * @throws RuntimeException if an error occurs during the nonce generation
     */
    public static byte[] generateNonce(final int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Nonce length cannot be negative: " + length);
        }
        if (length > MAX_NONCE_LENGTH) {
            throw new IllegalArgumentException("Nonce length exceeds maximum of " + MAX_NONCE_LENGTH + ": " + length);
        }

        try {
            final byte[] temp = new byte[length];
            random.nextBytes(temp);
            return temp;
        } catch (final Exception ex) {
            throw new RuntimeException("Error in generating nonce of length " + length, ex);
        }
    }

    /**
     * Generates a SHA-1 digest (hash) of the input bytes. This method allocates a fresh
     * {@link MessageDigest} instance per call to avoid lock contention; the JCE provider's
     * own implementation cache makes {@code MessageDigest.getInstance} cheap.
     *
     * <p>The SHA-1 algorithm produces a 160-bit (20-byte) hash value. While SHA-1 is considered
     * cryptographically weak for collision resistance in some contexts (such as digital signatures),
     * it remains in use for backward compatibility in many protocols including WS-Security.</p>
     *
     * <p>This method is thread-safe.</p>
     *
     * <p><b>Security Note:</b> This method uses SHA-1, which is cryptographically broken for
     * collision resistance. For new applications, prefer {@link #generateDigest(byte[], String)}
     * with a stronger algorithm such as {@code "SHA-256"}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String data = "Hello World";
     * byte[] hash = WSSecurityUtil.generateDigest(data.getBytes(StandardCharsets.UTF_8));
     * String base64Hash = Base64.getEncoder().encodeToString(hash);
     * }</pre>
     *
     * @param inputBytes the bytes to be digested, must not be null
     * @return a byte array containing the SHA-1 hash of the input bytes (always 20 bytes)
     * @throws IllegalArgumentException if inputBytes is null
     * @throws RuntimeException if an unexpected error occurs during the digest operation
     * @see #generateDigest(byte[], String)
     */
    public static byte[] generateDigest(final byte[] inputBytes) {
        if (inputBytes == null) {
            throw new IllegalArgumentException("Input bytes cannot be null");
        }

        try {
            // Allocate a fresh MessageDigest per call. The cached instance + class-wide lock
            // serialized every hashing call across the process and made generateNonce a DoS
            // amplifier. SunJCE's MessageDigest implementation cache makes getInstance cheap.
            final MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            return md.digest(inputBytes);
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("Error in generating digest", e);
        }
    }

    /**
     * Generates a digest (hash) of the input bytes using the specified algorithm.
     * This method allows callers to choose a stronger algorithm than the default SHA-1.
     *
     * <p>Common algorithm names include:</p>
     * <ul>
     *   <li>{@code "SHA-256"} — 256-bit hash, recommended for most use cases</li>
     *   <li>{@code "SHA-384"} — 384-bit hash</li>
     *   <li>{@code "SHA-512"} — 512-bit hash</li>
     *   <li>{@code "SHA-1"} — 160-bit hash (legacy, not recommended for new use)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String data = "Hello World";
     * byte[] hash = WSSecurityUtil.generateDigest(
     *     data.getBytes(StandardCharsets.UTF_8), "SHA-256");
     * String base64Hash = Base64.getEncoder().encodeToString(hash);
     * }</pre>
     *
     * @param inputBytes the bytes to be digested, must not be {@code null}
     * @param algorithm the name of the digest algorithm (e.g. {@code "SHA-256"}), must not be {@code null}
     * @return a byte array containing the hash of the input bytes
     * @throws IllegalArgumentException if {@code inputBytes} or {@code algorithm} is {@code null},
     *         or if the algorithm is not available
     */
    public static byte[] generateDigest(final byte[] inputBytes, final String algorithm) {
        if (inputBytes == null) {
            throw new IllegalArgumentException("Input bytes cannot be null");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }

        try {
            final MessageDigest md = MessageDigest.getInstance(algorithm);
            return md.digest(inputBytes);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm, e);
        }
    }

    /**
     * Creates a WS-Security compliant password digest by computing the SHA-1 hash of
     * the concatenated nonce, created timestamp, and password, then encoding the result
     * in Base64 format. This implements the password digest mechanism defined in the
     * WS-Security UsernameToken Profile specification.
     *
     * <p>This method implements the WS-Security UsernameToken password digest algorithm:</p>
     * <pre>{@code
     * PasswordDigest = Base64(SHA-1(nonce + created + password))
     * }</pre>
     *
     * <p>The three components are concatenated in the specific order: nonce, then created,
     * then password. This order is defined by the WS-Security specification and must be
     * strictly followed for interoperability.</p>
     *
     * <p><b>Security Note:</b> This method uses SHA-1, which is cryptographically broken for
     * collision resistance. For new applications or when the WS-Security peer supports it,
     * prefer {@link #computePasswordDigest(byte[], byte[], byte[], String)} with a stronger
     * algorithm such as {@code "SHA-256"}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate components
     * byte[] nonce = WSSecurityUtil.generateNonce(16);
     * byte[] created = Instant.now().toString().getBytes(StandardCharsets.UTF_8);
     * byte[] password = "secretPassword".getBytes(StandardCharsets.UTF_8);
     *
     * // Create digest
     * String passwordDigest = WSSecurityUtil.computePasswordDigest(nonce, created, password);
     * }</pre>
     *
     * @param nonce the nonce byte array, a cryptographically random value that should only
     *              be used once, must not be null
     * @param created the created timestamp byte array, typically the current timestamp in
     *                ISO 8601 format, must not be null
     * @param password the password byte array to be digested, must not be null
     * @return a Base64-encoded string of the SHA-1 hash of the concatenated inputs
     * @throws IllegalArgumentException if any parameter is null
     * @throws RuntimeException if an unexpected error occurs during the digest operation
     * @see #computePasswordDigest(byte[], byte[], byte[], String)
     * @see #computePasswordDigest(String, String, String)
     * @see #generateNonce(int)
     * @see #generateDigest(byte[])
     */
    public static String computePasswordDigest(final byte[] nonce, final byte[] created, final byte[] password) {
        if (nonce == null) {
            throw new IllegalArgumentException("Nonce cannot be null");
        }
        if (created == null) {
            throw new IllegalArgumentException("Created timestamp cannot be null");
        }
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }

        final byte[] b4 = new byte[nonce.length + created.length + password.length];
        int offset = 0;
        N.copy(nonce, 0, b4, offset, nonce.length);
        offset += nonce.length;

        N.copy(created, 0, b4, offset, created.length);
        offset += created.length;

        N.copy(password, 0, b4, offset, password.length);

        final byte[] digestBytes = generateDigest(b4);
        return Strings.base64Encode(digestBytes);
    }

    /**
     * Creates a password digest by computing the hash of the concatenated nonce, created timestamp,
     * and password using the specified algorithm, then encoding the result in Base64 format.
     *
     * <p>This method follows the same concatenation order as the WS-Security UsernameToken
     * password digest algorithm, but allows a stronger hash algorithm:</p>
     * <pre>{@code
     * PasswordDigest = Base64(Hash(nonce + created + password))
     * }</pre>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] nonce = WSSecurityUtil.generateNonce(16);
     * byte[] created = Instant.now().toString().getBytes(StandardCharsets.UTF_8);
     * byte[] password = "secretPassword".getBytes(StandardCharsets.UTF_8);
     *
     * // Use SHA-256 instead of the default SHA-1
     * String passwordDigest = WSSecurityUtil.computePasswordDigest(
     *     nonce, created, password, "SHA-256");
     * }</pre>
     *
     * @param nonce the nonce byte array, must not be {@code null}
     * @param created the created timestamp byte array, must not be {@code null}
     * @param password the password byte array to be digested, must not be {@code null}
     * @param algorithm the name of the digest algorithm (e.g. {@code "SHA-256"}), must not be {@code null}
     * @return a Base64-encoded string of the hash of the concatenated inputs
     * @throws IllegalArgumentException if any parameter is {@code null}, or if the algorithm is not available
     * @see #computePasswordDigest(byte[], byte[], byte[])
     * @see #generateDigest(byte[], String)
     */
    public static String computePasswordDigest(final byte[] nonce, final byte[] created, final byte[] password, final String algorithm) {
        if (nonce == null) {
            throw new IllegalArgumentException("Nonce cannot be null");
        }
        if (created == null) {
            throw new IllegalArgumentException("Created timestamp cannot be null");
        }
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }

        final byte[] b4 = new byte[nonce.length + created.length + password.length];
        int offset = 0;
        N.copy(nonce, 0, b4, offset, nonce.length);
        offset += nonce.length;

        N.copy(created, 0, b4, offset, created.length);
        offset += created.length;

        N.copy(password, 0, b4, offset, password.length);

        final byte[] digestBytes = generateDigest(b4, algorithm);
        return Strings.base64Encode(digestBytes);
    }

    /**
     * Creates a WS-Security compliant password digest using string inputs. This is a convenience
     * method that converts the string parameters to bytes using UTF-8 before processing them
     * with the WS-Security password digest algorithm.
     *
     * <p>This method internally calls {@link #computePasswordDigest(byte[], byte[], byte[])} after
     * converting all string parameters to byte arrays using UTF-8 (as mandated by the WS-Security
     * UsernameToken Profile).</p>
     *
     * <p><b>Important — nonce encoding:</b> This overload hashes the {@code nonce} <i>string's</i> UTF-8 bytes
     * verbatim; it does <b>not</b> Base64-decode it. The WS-Security UsernameToken digest is defined over the
     * <i>raw</i> nonce bytes, so passing a Base64-encoded nonce string here produces a digest that a
     * spec-compliant peer (which Base64-decodes the nonce before hashing) will not match. For interoperable
     * WS-Security digests, use {@link #computePasswordDigest(byte[], byte[], byte[])} with the raw nonce bytes.
     * This string overload is a convenience for callers whose nonce is genuinely text.</p>
     *
     * <p><b>Security Note:</b> This method uses SHA-1, which is cryptographically broken for
     * collision resistance. For new applications or when the WS-Security peer supports it,
     * prefer {@link #computePasswordDigest(String, String, String, String)} with a stronger
     * algorithm such as {@code "SHA-256"}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For WS-Security interop, hash the RAW nonce bytes (not the Base64 text):
     * byte[] nonceBytes = WSSecurityUtil.generateNonce(16);
     * String created = Instant.now().toString();
     * String passwordDigest = WSSecurityUtil.computePasswordDigest(
     *         nonceBytes, created.getBytes(StandardCharsets.UTF_8), "secretPassword".getBytes(StandardCharsets.UTF_8));
     *
     * // This string overload is for text nonces only (it hashes the nonce text as-is):
     * String textDigest = WSSecurityUtil.computePasswordDigest(textNonce, created, "secretPassword");
     * }</pre>
     *
     * @param nonce the nonce string, a cryptographically random value that should only be
     *              used once, must not be null
     * @param created the created timestamp string, typically the current timestamp in ISO 8601
     *                format, must not be null
     * @param password the password string to be digested, must not be null
     * @return a Base64-encoded string of the SHA-1 hash of the concatenated inputs
     * @throws IllegalArgumentException if any parameter is null
     * @throws RuntimeException if an unexpected error occurs during the digest operation
     * @see #computePasswordDigest(String, String, String, String)
     * @see #computePasswordDigest(byte[], byte[], byte[])
     * @see #generateNonce(int)
     * @see #generateDigest(byte[])
     */
    public static String computePasswordDigest(final String nonce, final String created, final String password) {
        if (nonce == null) {
            throw new IllegalArgumentException("Nonce cannot be null");
        }
        if (created == null) {
            throw new IllegalArgumentException("Created timestamp cannot be null");
        }
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }

        // WS-Security UsernameToken Profile mandates UTF-8. Charsets.DEFAULT is the JVM platform
        // charset (e.g. cp1252 on Windows) and produced different digests on different OSes for
        // identical credentials containing non-ASCII characters.
        return computePasswordDigest(nonce.getBytes(Charsets.UTF_8), created.getBytes(Charsets.UTF_8), password.getBytes(Charsets.UTF_8));
    }

    /**
     * Creates a password digest using string inputs and the specified algorithm. This is a
     * convenience method that converts the string parameters to bytes using UTF-8 before
     * computing the digest.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String nonce = Base64.getEncoder().encodeToString(WSSecurityUtil.generateNonce(16));
     * String created = Instant.now().toString();
     * String password = "secretPassword";
     *
     * // Use SHA-256 instead of the default SHA-1
     * String passwordDigest = WSSecurityUtil.computePasswordDigest(
     *     nonce, created, password, "SHA-256");
     * }</pre>
     *
     * @param nonce the nonce string, must not be {@code null}
     * @param created the created timestamp string, must not be {@code null}
     * @param password the password string to be digested, must not be {@code null}
     * @param algorithm the name of the digest algorithm (e.g. {@code "SHA-256"}), must not be {@code null}
     * @return a Base64-encoded string of the hash of the concatenated inputs
     * @throws IllegalArgumentException if any parameter is {@code null}, or if the algorithm is not available
     * @see #computePasswordDigest(byte[], byte[], byte[], String)
     */
    public static String computePasswordDigest(final String nonce, final String created, final String password, final String algorithm) {
        if (nonce == null) {
            throw new IllegalArgumentException("Nonce cannot be null");
        }
        if (created == null) {
            throw new IllegalArgumentException("Created timestamp cannot be null");
        }
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }

        // See note on the SHA-1 overload: standardize on UTF-8 for cross-platform interop.
        return computePasswordDigest(nonce.getBytes(Charsets.UTF_8), created.getBytes(Charsets.UTF_8), password.getBytes(Charsets.UTF_8), algorithm);
    }
}
