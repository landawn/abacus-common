/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * Note: it's copied from Apache WSS4J developed at The Apache Software Foundation (http://www.apache.org/), or
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
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /** A cached MessageDigest object. */
    private final static MessageDigest digest;
    static {
        try {
            digest = MessageDigest.getInstance(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private WSSecurityUtil() {
        // Complete
    }

    /**
     * Generate a nonce of the given length using the SHA1PRNG algorithm. The SecureRandom instance that backs this
     * method is cached for efficiency.
     *
     * @param length
     * @return a nonce of the given length
     */
    public static byte[] generateNonce(int length) {
        synchronized (WSSecurityUtil.class) {
            try {
                byte[] temp = new byte[length];
                random.nextBytes(temp);

                return temp;
            } catch (Exception ex) {
                throw new RuntimeException("Error in generating nonce of length " + length, ex);
            }
        }
    }

    /**
     * Generate a (SHA1) digest of the input bytes. The MessageDigest instance that backs this method is cached for
     * efficiency.
     *
     * @param inputBytes
     *            the bytes to digest
     * @return
    
     */
    public static byte[] generateDigest(byte[] inputBytes) {
        synchronized (WSSecurityUtil.class) {
            try {
                return digest.digest(inputBytes);
            } catch (Exception e) {
                throw new RuntimeException("Error in generating digest", e);
            }
        }
    }

    /**
     * Do password digest.
     *
     * @param nonce
     * @param created
     * @param password
     * @return
     */
    public static String doPasswordDigest(byte[] nonce, byte[] created, byte[] password) {
        byte[] b4 = new byte[nonce.length + created.length + password.length];
        int offset = 0;
        N.copy(nonce, 0, b4, offset, nonce.length);
        offset += nonce.length;

        N.copy(created, 0, b4, offset, created.length);
        offset += created.length;

        N.copy(password, 0, b4, offset, password.length);

        byte[] digestBytes = generateDigest(b4);
        return N.base64Encode(digestBytes);
    }

    /**
     * Do password digest.
     *
     * @param nonce
     * @param created
     * @param password
     * @return
     */
    public static String doPasswordDigest(String nonce, String created, String password) {
        return doPasswordDigest(nonce.getBytes(Charsets.UTF_8), created.getBytes(Charsets.UTF_8), password.getBytes(Charsets.UTF_8));
    }
}
