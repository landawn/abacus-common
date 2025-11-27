/*
 * Copyright (c) 2021, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.guava.hash;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;
import com.landawn.abacus.util.N;

/**
 * Package-private implementation of {@link Hasher} that wraps a Google Guava
 * {@link com.google.common.hash.Hasher}. This class serves as an adapter between
 * the abacus-common hashing API and the underlying Guava implementation.
 * 
 * <p>This class is stateful and not thread-safe, following the same contract as
 * the {@link Hasher} interface. Each instance should be used by only one thread
 * and for only one hash computation.
 * 
 * <p><b>Implementation Note:</b> This class is not intended for direct use by clients.
 * Hasher instances should be obtained from {@link HashFunction#newHasher()} methods.
 * 
 * @see Hasher
 * @see HashFunction
 */
@SuppressWarnings("ClassCanBeRecord")
final class GuavaHasher implements Hasher {

    /**
     * The wrapped Google Guava hasher that performs the actual hashing operations.
     */
    final com.google.common.hash.Hasher gHasher;

    /**
     * Constructs a new GuavaHasher wrapping the specified Guava hasher.
     * 
     * @param gHasher the Guava hasher to wrap
     */
    GuavaHasher(final com.google.common.hash.Hasher gHasher) {
        this.gHasher = gHasher;
    }

    /**
     * Static factory method that creates a new GuavaHasher wrapping the given
     * Guava hasher. This method provides a more convenient way to create
     * instances compared to using the constructor directly.
     *
     * <p>This method is used internally by {@link GuavaHashFunction} to adapt
     * Guava's hashers to the abacus-common API. Each wrapped hasher maintains
     * the same state and behavior as the underlying Guava hasher.
     *
     * <p><b>Note:</b> This is an internal method. Client code should obtain Hasher
     * instances through {@link HashFunction#newHasher()} instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * com.google.common.hash.Hasher guavaHasher = com.google.common.hash.Hashing.sha256().newHasher();
     * Hasher wrapped = GuavaHasher.wrap(guavaHasher);
     * }</pre>
     *
     * @param gHasher the Guava hasher to wrap (implicitly non-null)
     * @return a new GuavaHasher instance wrapping the given hasher
     */
    static GuavaHasher wrap(final com.google.common.hash.Hasher gHasher) {
        return new GuavaHasher(gHasher);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Adds a byte by delegating to the wrapped Guava hasher's {@code putByte()} method.
     */
    @Override
    public Hasher put(final byte b) {
        gHasher.putByte(b);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Adds a byte array by delegating to the wrapped Guava hasher's {@code putBytes()} method.
     */
    @Override
    public Hasher put(final byte[] bytes) {
        gHasher.putBytes(bytes);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Adds a portion of a byte array by delegating to the wrapped Guava hasher's
     * {@code putBytes(byte[], int, int)} method.
     */
    @Override
    public Hasher put(final byte[] bytes, final int off, final int len) {
        gHasher.putBytes(bytes, off, len);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Adds bytes from a ByteBuffer by delegating to the wrapped Guava hasher's
     * {@code putBytes(ByteBuffer)} method.
     */
    @Override
    public Hasher put(final ByteBuffer bytes) {
        gHasher.putBytes(bytes);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Adds a short value by delegating to the wrapped Guava hasher's {@code putShort()} method.
     */
    @Override
    public Hasher put(final short s) {
        gHasher.putShort(s);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Adds an integer value by delegating to the wrapped Guava hasher's {@code putInt()} method.
     */
    @Override
    public Hasher put(final int i) {
        gHasher.putInt(i);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Adds a long value by delegating to the wrapped Guava hasher's {@code putLong()} method.
     */
    @Override
    public Hasher put(final long l) {
        gHasher.putLong(l);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Adds a float value by delegating to the wrapped Guava hasher's {@code putFloat()} method.
     */
    @Override
    public Hasher put(final float f) {
        gHasher.putFloat(f);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Adds a double value by delegating to the wrapped Guava hasher's {@code putDouble()} method.
     */
    @Override
    public Hasher put(final double d) {
        gHasher.putDouble(d);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Adds a boolean value by delegating to the wrapped Guava hasher's {@code putBoolean()} method.
     */
    @Override
    public Hasher put(final boolean b) {
        gHasher.putBoolean(b);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Adds a character value by delegating to the wrapped Guava hasher's {@code putChar()} method.
     */
    @Override
    public Hasher put(final char c) {
        gHasher.putChar(c);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Adds all characters from an array. This implementation processes the entire array
     * by calling {@link #put(char[], int, int)} with appropriate parameters.
     */
    @Override
    public Hasher put(final char[] chars) {
        return put(chars, 0, N.len(chars));
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Adds a portion of a character array. Since Guava's Hasher doesn't have a direct
     * method for character arrays, this implementation adds each character individually
     * using {@link #put(char)}.
     * 
     * <p><b>Implementation Note:</b> This method validates the array bounds using
     * {@link N#checkFromIndexSize} before processing the characters.
     */
    @Override
    public Hasher put(final char[] chars, final int off, final int len) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(off, len, N.len(chars));

        for (int i = off, to = off + len; i < to; i++) {
            put(chars[i]);
        }

        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Adds a character sequence without encoding by delegating to the wrapped Guava
     * hasher's {@code putUnencodedChars()} method.
     */
    @Override
    public Hasher put(final CharSequence charSequence) {
        gHasher.putUnencodedChars(charSequence);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Adds an encoded character sequence by delegating to the wrapped Guava hasher's
     * {@code putString()} method.
     */
    @Override
    public Hasher put(final CharSequence charSequence, final Charset charset) {
        gHasher.putString(charSequence, charset);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Adds an object using a funnel by delegating to the wrapped Guava hasher's
     * {@code putObject()} method.
     */
    @Override
    public <T> Hasher put(final T instance, final Funnel<? super T> funnel) {
        gHasher.putObject(instance, funnel);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Computes the final hash code by delegating to the wrapped Guava hasher's
     * {@code hash()} method. After this method is called, the hasher instance should
     * not be used again.
     */
    @Override
    public HashCode hash() {
        return gHasher.hash();
    }
}
