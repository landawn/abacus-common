/*
 * Copyright (C) 2012 The Guava Authors
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
import java.util.zip.Checksum;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.function.Supplier;

/**
 * Note: It's copied from Google Guava under Apache License 2.0
 * 
 * {@link HashFunction} adapter for {@link Checksum} instances.
 *
 * @author Colin Decker
 */
final class ChecksumHashFunction extends AbstractStreamingHashFunction implements Serializable {

    private final Supplier<? extends Checksum> checksumSupplier;

    private final int bits;

    private final String toString;

    ChecksumHashFunction(Supplier<? extends Checksum> checksumSupplier, int bits, String toString) {
        this.checksumSupplier = N.checkArgNotNull(checksumSupplier);
        N.checkArgument(bits == 32 || bits == 64, "bits (%s) must be either 32 or 64", bits);
        this.bits = bits;
        this.toString = N.checkArgNotNull(toString);
    }

    @Override
    public int bits() {
        return bits;
    }

    @Override
    public Hasher newHasher() {
        return new ChecksumHasher(checksumSupplier.get());
    }

    @Override
    public String toString() {
        return toString;
    }

    /**
     * Hasher that updates a checksum.
     */
    private final class ChecksumHasher extends AbstractByteHasher {

        /** The checksum. */
        private final Checksum checksum;

        /**
         * Instantiates a new checksum hasher.
         *
         * @param checksum
         */
        private ChecksumHasher(Checksum checksum) {
            this.checksum = N.checkArgNotNull(checksum);
        }

        /**
         *
         * @param b
         */
        @Override
        protected void update(byte b) {
            checksum.update(b);
        }

        /**
         *
         * @param bytes
         * @param off
         * @param len
         */
        @Override
        protected void update(byte[] bytes, int off, int len) {
            checksum.update(bytes, off, len);
        }

        /**
         *
         * @return
         */
        @Override
        public HashCode hash() {
            long value = checksum.getValue();
            if (bits == 32) {
                /*
                 * The long returned from a 32-bit Checksum will have all 0s for its second word, so the
                 * cast won't lose any information and is necessary to return a HashCode of the correct
                 * size.
                 */
                return HashCode.fromInt((int) value);
            } else {
                return HashCode.fromLong(value);
            }
        }
    }

    private static final long serialVersionUID = 0L;
}
