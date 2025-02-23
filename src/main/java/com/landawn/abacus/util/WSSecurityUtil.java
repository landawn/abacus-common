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
 * <p>
 * Note: it's copied from Apache WSS4J developed at <a href="http://www.apache.org/">The Apache Software Foundation</a>, or
 * under the Apache License 2.0. The methods copied from other products/frameworks may be modified in this class.
 * </p>
 *
 * WS-Security Utility methods.
 * <p/>
 *
 * @author Davanum Srinivas (dims@yahoo.com).
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
     * Generate a nonce of the given length using the SHA1PRNG algorithm. The SecureRandom instance that backs this method is cached for efficiency.
     *
     * @param length The length of the nonce to be generated.
     * @return A byte array representing the nonce.
     * @throws RuntimeException If an error occurs during the nonce generation.
     */
    public static byte[] generateNonce(final int length) {
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
     * Generate a (SHA1) digest of the input bytes. The MessageDigest instance that backs this method is cached for efficiency.
     *
     * @param inputBytes The bytes to be digested.
     * @return A byte array representing the SHA-1 hash of the input bytes.
     * @throws RuntimeException If an error occurs during the digest operation.
     */
    public static byte[] generateDigest(final byte[] inputBytes) {
        synchronized (WSSecurityUtil.class) {
            try {
                return digest.digest(inputBytes);
            } catch (final Exception e) {
                throw new RuntimeException("Error in generating digest", e);
            }
        }
    }

    /**
     * Returns a base64 encoded string of the SHA-1 hash of the concatenated nonce, created time, and password.
     *
     * @param nonce The nonce byte array, which is a random value that should only be used once.
     * @param created The created time byte array, which is typically the current time.
     * @param password The password byte array to be digested.
     * @return A base64 encoded string of the SHA-1 hash of the concatenated nonce, created time, and password.
     */
    public static String doPasswordDigest(final byte[] nonce, final byte[] created, final byte[] password) {
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
     * Returns a base64 encoded string of the SHA-1 hash of the concatenated nonce, created time, and password.
     *
     * @param nonce The nonce string, which is a random value that should only be used once.
     * @param created The created time string, which is typically the current time.
     * @param password The password string to be digested.
     * @return A base64 encoded string of the SHA-1 hash of the concatenated nonce, created time, and password.
     */
    public static String doPasswordDigest(final String nonce, final String created, final String password) {
        return doPasswordDigest(nonce.getBytes(Charsets.DEFAULT), created.getBytes(Charsets.DEFAULT), password.getBytes(Charsets.DEFAULT));
    }
}
