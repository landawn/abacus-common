/*
 * Copyright (C) 2011 The Guava Authors
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

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import com.landawn.abacus.util.N;

// TODO: Auto-generated Javadoc
/**
 * Note: It's copied from Google Guava under Apache License 2.0
 * 
 * {@link HashFunction} adapter for {@link MessageDigest} instances.
 *
 * @author Kevin Bourrillion
 * @author Dimitris Andreou
 */
final class MessageDigestHashFunction extends AbstractStreamingHashFunction {

    /** The prototype. */
    private final MessageDigest prototype;

    /** The bytes. */
    private final int bytes;

    /** The supports clone. */
    private final boolean supportsClone;

    /** The to string. */
    private final String toString;

    /**
     * Instantiates a new message digest hash function.
     *
     * @param algorithmName
     * @param toString
     */
    MessageDigestHashFunction(String algorithmName, String toString) {
        this.prototype = getMessageDigest(algorithmName);
        this.bytes = prototype.getDigestLength();
        this.toString = N.checkArgNotNull(toString);
        this.supportsClone = supportsClone(prototype);
    }

    /**
     * Instantiates a new message digest hash function.
     *
     * @param algorithmName
     * @param bytes
     * @param toString
     */
    MessageDigestHashFunction(String algorithmName, int bytes, String toString) {
        this.toString = N.checkArgNotNull(toString);
        this.prototype = getMessageDigest(algorithmName);
        int maxLength = prototype.getDigestLength();
        N.checkArgument(bytes >= 4 && bytes <= maxLength, "bytes (%s) must be >= 4 and < %s", bytes, maxLength);
        this.bytes = bytes;
        this.supportsClone = supportsClone(prototype);
    }

    /**
     * Supports clone.
     *
     * @param digest
     * @return true, if successful
     */
    private static boolean supportsClone(MessageDigest digest) {
        try {
            digest.clone();
            return true;
        } catch (CloneNotSupportedException e) {
            return false;
        }
    }

    /**
     * Bits.
     *
     * @return
     */
    @Override
    public int bits() {
        return bytes * Byte.SIZE;
    }

    /**
     * To string.
     *
     * @return
     */
    @Override
    public String toString() {
        return toString;
    }

    /**
     * Gets the message digest.
     *
     * @param algorithmName
     * @return
     */
    private static MessageDigest getMessageDigest(String algorithmName) {
        try {
            return MessageDigest.getInstance(algorithmName);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * New hasher.
     *
     * @return
     */
    @Override
    public Hasher newHasher() {
        if (supportsClone) {
            try {
                return new MessageDigestHasher((MessageDigest) prototype.clone(), bytes);
            } catch (CloneNotSupportedException e) {
                // falls through
            }
        }
        return new MessageDigestHasher(getMessageDigest(prototype.getAlgorithm()), bytes);
    }

    /**
     * The Class SerializedForm.
     */
    private static final class SerializedForm implements Serializable {

        /** The algorithm name. */
        private final String algorithmName;

        /** The bytes. */
        private final int bytes;

        /** The to string. */
        private final String toString;

        /**
         * Instantiates a new serialized form.
         *
         * @param algorithmName
         * @param bytes
         * @param toString
         */
        private SerializedForm(String algorithmName, int bytes, String toString) {
            this.algorithmName = algorithmName;
            this.bytes = bytes;
            this.toString = toString;
        }

        /**
         * Read resolve.
         *
         * @return
         */
        private Object readResolve() {
            return new MessageDigestHashFunction(algorithmName, bytes, toString);
        }

        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 0;
    }

    /**
     * Write replace.
     *
     * @return
     */
    Object writeReplace() {
        return new SerializedForm(prototype.getAlgorithm(), bytes, toString);
    }

    /**
     * Hasher that updates a message digest.
     */
    private static final class MessageDigestHasher extends AbstractByteHasher {

        /** The digest. */
        private final MessageDigest digest;

        /** The bytes. */
        private final int bytes;

        /** The done. */
        private boolean done;

        /**
         * Instantiates a new message digest hasher.
         *
         * @param digest
         * @param bytes
         */
        private MessageDigestHasher(MessageDigest digest, int bytes) {
            this.digest = digest;
            this.bytes = bytes;
        }

        /**
         * Update.
         *
         * @param b
         */
        @Override
        protected void update(byte b) {
            checkNotDone();
            digest.update(b);
        }

        /**
         * Update.
         *
         * @param b
         */
        @Override
        protected void update(byte[] b) {
            checkNotDone();
            digest.update(b);
        }

        /**
         * Update.
         *
         * @param b
         * @param off
         * @param len
         */
        @Override
        protected void update(byte[] b, int off, int len) {
            checkNotDone();
            digest.update(b, off, len);
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
         * @return
         */
        @Override
        public HashCode hash() {
            checkNotDone();
            done = true;
            return (bytes == digest.getDigestLength()) ? HashCode.fromBytesNoCopy(digest.digest())
                    : HashCode.fromBytesNoCopy(Arrays.copyOf(digest.digest(), bytes));
        }
    }
}
