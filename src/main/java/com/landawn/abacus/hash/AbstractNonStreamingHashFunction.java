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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.function.BiConsumer;

// TODO: Auto-generated Javadoc
/**
 * Note: It's copied from Google Guava under Apache License 2.0
 * 
 * Skeleton implementation of {@link HashFunction}, appropriate for non-streaming algorithms. All
 * the hash computation done using {@linkplain #newHasher()} are delegated to the
 * {@linkplain #hash(byte[], int, int)} method.
 *
 * @author Dimitris Andreou
 */
abstract class AbstractNonStreamingHashFunction implements HashFunction {

    /**
     * New hasher.
     *
     * @return the hasher
     */
    @Override
    public Hasher newHasher() {
        return new BufferingHasher(32);
    }

    /**
     * New hasher.
     *
     * @param expectedInputSize the expected input size
     * @return the hasher
     */
    @Override
    public Hasher newHasher(int expectedInputSize) {
        N.checkArgument(expectedInputSize >= 0);
        return new BufferingHasher(expectedInputSize);
    }

    /**
     * Hash.
     *
     * @param <T> the generic type
     * @param instance the instance
     * @param funnel the funnel
     * @return the hash code
     */
    @Override
    public <T> HashCode hash(T instance, BiConsumer<? super T, ? super Hasher> funnel) {
        return newHasher().put(instance, funnel).hash();
    }

    /**
     * Hash.
     *
     * @param input the input
     * @return the hash code
     */
    @Override
    public HashCode hash(CharSequence input) {
        int len = input.length();
        Hasher hasher = newHasher(len * 2);
        for (int i = 0; i < len; i++) {
            hasher.put(input.charAt(i));
        }
        return hasher.hash();
    }

    /**
     * Hash.
     *
     * @param input the input
     * @param charset the charset
     * @return the hash code
     */
    @Override
    public HashCode hash(CharSequence input, Charset charset) {
        return hash(input.toString().getBytes(charset));
    }

    //    @Override
    //    public HashCode hash(boolean input) {
    //        return hash(input == false ? 0 : 1);
    //    }

    /**
     * Hash.
     *
     * @param input the input
     * @return the hash code
     */
    @Override
    public HashCode hash(int input) {
        return newHasher(4).put(input).hash();
    }

    /**
     * Hash.
     *
     * @param input the input
     * @return the hash code
     */
    @Override
    public HashCode hash(long input) {
        return newHasher(8).put(input).hash();
    }

    //    @Override
    //    public HashCode hash(float input) {
    //        return hash(Float.floatToRawIntBits(input));
    //    }
    //
    //    @Override
    //    public HashCode hash(double input) {
    //        return hash(Double.doubleToRawLongBits(input));
    //    }

    /**
     * Hash.
     *
     * @param input the input
     * @return the hash code
     */
    @Override
    public HashCode hash(byte[] input) {
        return hash(input, 0, input.length);
    }

    /**
     * In-memory stream-based implementation of Hasher.
     */
    private final class BufferingHasher extends AbstractHasher {

        /** The stream. */
        final ExposedByteArrayOutputStream stream;

        /** The Constant BOTTOM_BYTE. */
        static final int BOTTOM_BYTE = 0xFF;

        /**
         * Instantiates a new buffering hasher.
         *
         * @param expectedInputSize the expected input size
         */
        BufferingHasher(int expectedInputSize) {
            this.stream = new ExposedByteArrayOutputStream(expectedInputSize);
        }

        /**
         * Put.
         *
         * @param b the b
         * @return the hasher
         */
        @Override
        public Hasher put(byte b) {
            stream.write(b);
            return this;
        }

        /**
         * Put.
         *
         * @param bytes the bytes
         * @return the hasher
         */
        @Override
        public Hasher put(byte[] bytes) {
            try {
                stream.write(bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        /**
         * Put.
         *
         * @param bytes the bytes
         * @param off the off
         * @param len the len
         * @return the hasher
         */
        @Override
        public Hasher put(byte[] bytes, int off, int len) {
            stream.write(bytes, off, len);
            return this;
        }

        /**
         * Put.
         *
         * @param s the s
         * @return the hasher
         */
        @Override
        public Hasher put(short s) {
            stream.write(s & BOTTOM_BYTE);
            stream.write((s >>> 8) & BOTTOM_BYTE);
            return this;
        }

        /**
         * Put.
         *
         * @param i the i
         * @return the hasher
         */
        @Override
        public Hasher put(int i) {
            stream.write(i & BOTTOM_BYTE);
            stream.write((i >>> 8) & BOTTOM_BYTE);
            stream.write((i >>> 16) & BOTTOM_BYTE);
            stream.write((i >>> 24) & BOTTOM_BYTE);
            return this;
        }

        /**
         * Put.
         *
         * @param l the l
         * @return the hasher
         */
        @Override
        public Hasher put(long l) {
            for (int i = 0; i < 64; i += 8) {
                stream.write((byte) ((l >>> i) & BOTTOM_BYTE));
            }
            return this;
        }

        /**
         * Put.
         *
         * @param c the c
         * @return the hasher
         */
        @Override
        public Hasher put(char c) {
            stream.write(c & BOTTOM_BYTE);
            stream.write((c >>> 8) & BOTTOM_BYTE);
            return this;
        }

        /**
         * Put.
         *
         * @param <T> the generic type
         * @param instance the instance
         * @param funnel the funnel
         * @return the hasher
         */
        @Override
        public <T> Hasher put(T instance, BiConsumer<? super T, ? super Hasher> funnel) {
            funnel.accept(instance, this);
            return this;
        }

        /**
         * Hash.
         *
         * @return the hash code
         */
        @Override
        public HashCode hash() {
            return AbstractNonStreamingHashFunction.this.hash(stream.byteArray(), 0, stream.length());
        }
    }

    /**
     * The Class ExposedByteArrayOutputStream.
     */
    // Just to access the byte[] without introducing an unnecessary copy
    private static final class ExposedByteArrayOutputStream extends ByteArrayOutputStream {

        /**
         * Instantiates a new exposed byte array output stream.
         *
         * @param expectedInputSize the expected input size
         */
        ExposedByteArrayOutputStream(int expectedInputSize) {
            super(expectedInputSize);
        }

        /**
         * Byte array.
         *
         * @return the byte[]
         */
        byte[] byteArray() {
            return buf;
        }

        /**
         * Length.
         *
         * @return the int
         */
        int length() {
            return count;
        }
    }
}
