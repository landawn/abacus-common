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
 * <p>The class uses cached instances of SecureRandom and MessageDigest for improved performance
 * while maintaining thread safety through synchronization. All methods are synchronized on the
 * class level to ensure safe concurrent access.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Cryptographically secure nonce generation using SHA1PRNG</li>
 *   <li>SHA-1 digest computation with cached MessageDigest instance</li>
 *   <li>WS-Security compliant password digest creation</li>
 *   <li>Thread-safe implementation for concurrent environments</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Generate a nonce
 * byte[] nonce = WSSecurityUtil.generateNonce(16);
 * 
 * // Create WS-Security password digest
 * String timestamp = getCurrentTimestamp();
 * String passwordDigest = WSSecurityUtil.doPasswordDigest(
 *     nonce, timestamp.getBytes(), password.getBytes()
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
            throw new RuntimeException(e);
        }
    }

    /** A cached MessageDigest object. */
    private static final MessageDigest digest;

    static {
        try {
            digest = MessageDigest.getInstance(HASH_ALGORITHM);
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private WSSecurityUtil() {
        // Complete
    }

    /**
     * Generates a cryptographically secure nonce (number used once) of the specified length
     * using the SHA1PRNG algorithm. The nonce is suitable for use in security protocols where
     * unpredictable random values are required, such as WS-Security authentication headers.
     *
     * <p>This method is thread-safe through class-level synchronization. The SecureRandom instance
     * is cached for improved performance across multiple calls while maintaining cryptographic strength.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate a 16-byte nonce for WS-Security
     * byte[] nonce = WSSecurityUtil.generateNonce(16);
     * String base64Nonce = Base64.getEncoder().encodeToString(nonce);
     * }</pre>
     *
     * @param length the length of the nonce to be generated in bytes, must be positive
     * @return a byte array containing cryptographically secure random bytes of the specified length
     * @throws IllegalArgumentException if length is negative
     * @throws RuntimeException if an error occurs during the nonce generation
     */
    public static byte[] generateNonce(final int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Nonce length cannot be negative: " + length);
        }

        synchronized (WSSecurityUtil.class) {
            try {
                final byte[] temp = new byte[length];
                random.nextBytes(temp);

                return temp;
            } catch (final Exception ex) {
                throw new RuntimeException("Error in generating nonce of length " + length, ex);
            }
        }
    }

    /**
     * Generates a SHA-1 digest (hash) of the input bytes. This method provides a thread-safe
     * way to compute SHA-1 hashes using a cached MessageDigest instance, which improves
     * performance by avoiding repeated MessageDigest instantiation.
     *
     * <p>The SHA-1 algorithm produces a 160-bit (20-byte) hash value. While SHA-1 is considered
     * cryptographically weak for collision resistance in some contexts (such as digital signatures),
     * it remains in use for backward compatibility in many protocols including WS-Security.</p>
     *
     * <p>This method is thread-safe through class-level synchronization.</p>
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
     */
    public static byte[] generateDigest(final byte[] inputBytes) {
        if (inputBytes == null) {
            throw new IllegalArgumentException("Input bytes cannot be null");
        }

        synchronized (WSSecurityUtil.class) {
            try {
                digest.reset();
                return digest.digest(inputBytes);
            } catch (final Exception e) {
                throw new RuntimeException("Error in generating digest", e);
            }
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate components
     * byte[] nonce = WSSecurityUtil.generateNonce(16);
     * byte[] created = Instant.now().toString().getBytes(StandardCharsets.UTF_8);
     * byte[] password = "secretPassword".getBytes(StandardCharsets.UTF_8);
     *
     * // Create digest
     * String passwordDigest = WSSecurityUtil.doPasswordDigest(nonce, created, password);
     * }</pre>
     *
     * @param nonce the nonce byte array, a cryptographically random value that should only
     *              be used once, must not be null
     * @param created the created timestamp byte array, typically the current timestamp in
     *                ISO 8601 format, must not be null
     * @param password the password byte array to be digested, must not be null
     * @return a Base64-encoded string of the SHA-1 hash of the concatenated inputs
     * @throws IllegalArgumentException if any parameter is null
     */
    public static String doPasswordDigest(final byte[] nonce, final byte[] created, final byte[] password) {
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
     * Creates a WS-Security compliant password digest using string inputs. This is a convenience
     * method that converts the string parameters to bytes using the default charset before
     * processing them with the WS-Security password digest algorithm.
     *
     * <p>This method internally calls {@link #doPasswordDigest(byte[], byte[], byte[])} after
     * converting all string parameters to byte arrays using the default charset. The resulting
     * digest follows the WS-Security UsernameToken specification.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Using string inputs
     * byte[] nonceBytes = WSSecurityUtil.generateNonce(16);
     * String nonce = Base64.getEncoder().encodeToString(nonceBytes);
     * String created = Instant.now().toString();
     * String password = "secretPassword";
     *
     * String passwordDigest = WSSecurityUtil.doPasswordDigest(nonce, created, password);
     *
     * // Use in WS-Security header
     * wsSecurityHeader.setPasswordDigest(passwordDigest);
     * }</pre>
     *
     * @param nonce the nonce string, a cryptographically random value that should only be
     *              used once, must not be null
     * @param created the created timestamp string, typically the current timestamp in ISO 8601
     *                format, must not be null
     * @param password the password string to be digested, must not be null
     * @return a Base64-encoded string of the SHA-1 hash of the concatenated inputs
     * @throws IllegalArgumentException if any parameter is null
     */
    public static String doPasswordDigest(final String nonce, final String created, final String password) {
        if (nonce == null) {
            throw new IllegalArgumentException("Nonce cannot be null");
        }
        if (created == null) {
            throw new IllegalArgumentException("Created timestamp cannot be null");
        }
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }

        return doPasswordDigest(nonce.getBytes(Charsets.DEFAULT), created.getBytes(Charsets.DEFAULT), password.getBytes(Charsets.DEFAULT));
    }
}
