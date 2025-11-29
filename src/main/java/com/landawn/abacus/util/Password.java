/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import com.landawn.abacus.annotation.MayReturnNull;

/**
 * A utility class for password encryption and verification using various hashing algorithms.
 * This class provides thread-safe password encryption functionality using MessageDigest
 * and Base64 encoding. It supports any algorithm available through the Java security provider.
 * 
 * <p>This class is immutable and thread-safe for password operations.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Password password = new Password("SHA-256");
 * String encrypted = password.encrypt("myPassword123");
 * boolean matches = password.isEqual("myPassword123", encrypted);
 * }</pre>
 * 
 * @see MessageDigest
 * @see java.security.Security
 */
public final class Password {

    private final String algorithm;

    private final MessageDigest msgDigest;

    /**
     * Creates a new Password instance with the specified hashing algorithm.
     * The algorithm must be one supported by the Java security provider
     * (e.g., "MD5", "SHA-1", "SHA-256", "SHA-512").
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Password sha256Password = new Password("SHA-256");
     * Password md5Password = new Password("MD5");
     * }</pre>
     *
     * @param algorithm the name of the hashing algorithm to use
     * @throws RuntimeException if the specified algorithm is not available
     * @see MessageDigest#getInstance(String)
     */
    public Password(final String algorithm) {
        this.algorithm = algorithm;

        try {
            msgDigest = MessageDigest.getInstance(algorithm);
        } catch (final NoSuchAlgorithmException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Returns the name of the hashing algorithm used by this Password instance.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Password password = new Password("SHA-256");
     * String algo = password.getAlgorithm();  // Returns "SHA-256"
     * }</pre>
     *
     * @return the algorithm name (e.g., "SHA-256", "MD5")
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Encrypts the given password string using the configured hashing algorithm
     * and returns the result encoded in Base64 format. This method is thread-safe
     * due to synchronization.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Password password = new Password("SHA-256");
     * String encrypted = password.encrypt("mySecretPassword");
     * // encrypted contains the Base64-encoded hash
     * }</pre>
     *
     * @param x the plain text password to encrypt
     * @return the encrypted password encoded in Base64, or {@code null} if input is {@code null}
     */
    @MayReturnNull
    public synchronized String encrypt(final String x) {
        if (x == null) {
            return null;
        }

        try {
            return Strings.base64Encode(msgDigest.digest(x.getBytes(Charsets.UTF_8)));
        } finally {
            msgDigest.reset();
        }
    }

    /**
     * Verifies if a plain text password matches an encrypted password.
     * This method encrypts the plain password and compares it with the provided
     * encrypted password for equality.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Password password = new Password("SHA-256");
     * String encrypted = password.encrypt("myPassword");
     * 
     * boolean matches = password.isEqual("myPassword", encrypted);         // true
     * boolean notMatches = password.isEqual("wrongPassword", encrypted);   // false
     * }</pre>
     *
     * @param plainPassword the plain text password to verify
     * @param encryptedPassword the encrypted password to compare against
     * @return {@code true} if the passwords match, {@code false} otherwise.
     *         Returns {@code true} if both are {@code null}, {@code false} if only one is {@code null}
     */
    public boolean isEqual(final String plainPassword, final String encryptedPassword) {
        return (plainPassword == null) ? (encryptedPassword == null) : ((encryptedPassword != null) && encryptedPassword.equals(encrypt(plainPassword)));
    }

    /**
     * Returns a hash code value for this Password instance based on the algorithm name.
     * Two Password instances with the same algorithm will have the same hash code.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(algorithm);
    }

    /**
     * Compares this Password instance with the specified object for equality.
     * Two Password instances are considered equal if they use the same algorithm.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Password p1 = new Password("SHA-256");
     * Password p2 = new Password("SHA-256");
     * Password p3 = new Password("MD5");
     * 
     * p1.equals(p2);   // true
     * p1.equals(p3);   // false
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return obj == this || (obj instanceof Password && ((Password) obj).algorithm.equals(algorithm));
    }

    /**
     * Returns a string representation of this Password instance.
     * The string contains the algorithm name in the format: {@code {algorithm=SHA-256}}
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "{algorithm=" + algorithm + "}";
    }
}
