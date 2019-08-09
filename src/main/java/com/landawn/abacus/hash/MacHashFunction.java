/*
 * Copyright (C) 2015 The Guava Authors
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

package com.landawn.abacus.hash;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;

import com.landawn.abacus.util.N;

// TODO: Auto-generated Javadoc
/**
 * Note: It's copied from Google Guava under Apache License 2.0
 * 
 * {@link HashFunction} adapter for {@link Mac} instances.
 *
 * @author Kurt Alfred Kluever
 */
final class MacHashFunction extends AbstractStreamingHashFunction {

    /** The prototype. */
    private final Mac prototype;

    /** The key. */
    private final Key key;

    /** The to string. */
    private final String toString;

    /** The bits. */
    private final int bits;

    /** The supports clone. */
    private final boolean supportsClone;

    /**
     * Instantiates a new mac hash function.
     *
     * @param algorithmName the algorithm name
     * @param key the key
     * @param toString the to string
     */
    MacHashFunction(String algorithmName, Key key, String toString) {
        this.prototype = getMac(algorithmName, key);
        this.key = N.checkArgNotNull(key);
        this.toString = N.checkArgNotNull(toString);
        this.bits = prototype.getMacLength() * Byte.SIZE;
        this.supportsClone = supportsClone(prototype);
    }

    /**
     * Bits.
     *
     * @return the int
     */
    @Override
    public int bits() {
        return bits;
    }

    /**
     * Supports clone.
     *
     * @param mac the mac
     * @return true, if successful
     */
    private static boolean supportsClone(Mac mac) {
        try {
            mac.clone();
            return true;
        } catch (CloneNotSupportedException e) {
            return false;
        }
    }

    /**
     * Gets the mac.
     *
     * @param algorithmName the algorithm name
     * @param key the key
     * @return the mac
     */
    private static Mac getMac(String algorithmName, Key key) {
        try {
            Mac mac = Mac.getInstance(algorithmName);
            mac.init(key);
            return mac;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * New hasher.
     *
     * @return the hasher
     */
    @Override
    public Hasher newHasher() {
        if (supportsClone) {
            try {
                return new MacHasher((Mac) prototype.clone());
            } catch (CloneNotSupportedException e) {
                // falls through
            }
        }
        return new MacHasher(getMac(prototype.getAlgorithm(), key));
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return toString;
    }

    /**
     * Hasher that updates a {@link Mac} (message authentication code).
     */
    private static final class MacHasher extends AbstractByteHasher {

        /** The mac. */
        private final Mac mac;

        /** The done. */
        private boolean done;

        /**
         * Instantiates a new mac hasher.
         *
         * @param mac the mac
         */
        private MacHasher(Mac mac) {
            this.mac = mac;
        }

        /**
         * Update.
         *
         * @param b the b
         */
        @Override
        protected void update(byte b) {
            checkNotDone();
            mac.update(b);
        }

        /**
         * Update.
         *
         * @param b the b
         */
        @Override
        protected void update(byte[] b) {
            checkNotDone();
            mac.update(b);
        }

        /**
         * Update.
         *
         * @param b the b
         * @param off the off
         * @param len the len
         */
        @Override
        protected void update(byte[] b, int off, int len) {
            checkNotDone();
            mac.update(b, off, len);
        }

        /**
         * Check not done.
         */
        private void checkNotDone() {
            N.checkState(!done, "Cannot re-use a Hasher after calling hash() on it");
        }

        /**
         * Hash.
         *
         * @return the hash code
         */
        @Override
        public HashCode hash() {
            checkNotDone();
            done = true;
            return HashCode.fromBytesNoCopy(mac.doFinal());
        }
    }
}
