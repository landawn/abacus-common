/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The Class Password.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class Password {

    /** The algorithm. */
    private final String algorithm;

    /** The md. */
    private final MessageDigest MD;

    /**
     * Instantiates a new password.
     *
     * @param algorithm
     */
    public Password(String algorithm) {
        this.algorithm = algorithm;

        try {
            MD = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Gets the algorithm.
     *
     * @return
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the encrypted password encoded with Base64.
     *
     * @param x
     * @return
     */
    public synchronized String encrypt(String x) {
        if (x == null) {
            return null;
        }

        try {
            return N.base64Encode(MD.digest(x.getBytes()));
        } finally {
            MD.reset();
        }
    }

    /**
     * Checks if is equal.
     *
     * @param plainPassword
     * @param encryptedPassword
     * @return true, if is equal
     */
    public boolean isEqual(String plainPassword, String encryptedPassword) {
        return (plainPassword == null) ? (encryptedPassword == null) : ((encryptedPassword != null) && encryptedPassword.equals(encrypt(plainPassword)));
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((algorithm == null) ? 0 : algorithm.hashCode());

        return result;
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof Password && ((Password) obj).algorithm.equals(algorithm));
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return "{algorithm=" + algorithm + "}";
    }
}
