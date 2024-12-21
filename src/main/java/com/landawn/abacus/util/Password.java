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
import java.util.Objects;

import com.landawn.abacus.annotation.MayReturnNull;

public final class Password {

    private final String algorithm;

    private final MessageDigest msgDigest;

    /**
     *
     * @param algorithm
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
     * Checks if is equal.
     *
     * @param plainPassword
     * @param encryptedPassword
     * @return {@code true}, if is equal
     */
    public boolean isEqual(final String plainPassword, final String encryptedPassword) {
        return (plainPassword == null) ? (encryptedPassword == null) : ((encryptedPassword != null) && encryptedPassword.equals(encrypt(plainPassword)));
    }

    @Override
    public int hashCode() {
        return Objects.hash(algorithm);
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(final Object obj) {
        return obj == this || (obj instanceof Password && ((Password) obj).algorithm.equals(algorithm));
    }

    @Override
    public String toString() {
        return "{algorithm=" + algorithm + "}";
    }
}
