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
import java.util.Objects;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.cs;

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
     * Character ranges shorter than this are fed to the Guava hasher one {@code putChar} call at a time; longer ranges
     * are encoded into a temporary little-endian byte buffer and fed with bulk {@code putBytes} calls.
     */
    private static final int BULK_CHARS_THRESHOLD = 16;

    /**
     * The maximum number of characters encoded into the temporary byte buffer per bulk {@code putBytes} call.
     */
    private static final int BULK_CHARS_CHUNK_SIZE = 512;

    /**
     * The wrapped Google Guava hasher that performs the actual hashing operations.
     */
    final com.google.common.hash.Hasher gHasher;

    /**
     * Constructs a new GuavaHasher wrapping the specified Guava hasher.
     *
     * @param guavaHasher the Guava hasher to wrap, must not be {@code null}
     * @throws NullPointerException if {@code guavaHasher} is {@code null}
     */
    GuavaHasher(final com.google.common.hash.Hasher guavaHasher) throws NullPointerException {
        N.requireNonNull(guavaHasher, cs.guavaHasher);
        this.gHasher = guavaHasher;
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
     * @param guavaHasher the Guava hasher to wrap, must not be {@code null}
     * @return a new GuavaHasher instance wrapping the given hasher
     * @throws NullPointerException if {@code guavaHasher} is {@code null}
     */
    static GuavaHasher wrap(final com.google.common.hash.Hasher guavaHasher) throws NullPointerException {
        return new GuavaHasher(guavaHasher);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds a byte by delegating to the wrapped Guava hasher's {@code putByte()} method.
     *
     * @param b the byte to add to the hash state
     * @return this hasher instance
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
     *
     * @param bytes the bytes to add to the hash state
     * @return this hasher instance
     * @throws NullPointerException if {@code bytes} is {@code null}.
     */
    @Override
    public Hasher put(final byte[] bytes) throws NullPointerException {
        gHasher.putBytes(bytes);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Validates the range up front with {@link Objects#checkFromIndexSize(int, int, int)} and then
     * adds a portion of a byte array by delegating to the wrapped Guava hasher's
     * {@code putBytes(byte[], int, int)} method.
     *
     * @param bytes the source byte array
     * @param off the start offset in the array
     * @param length the number of bytes to add
     * @return this hasher instance
     * @throws NullPointerException if {@code bytes} is {@code null}
     * @throws IndexOutOfBoundsException if {@code off} or {@code length} is negative, or if {@code off + len > bytes.length}
     */
    @Override
    public Hasher put(final byte[] bytes, final int off, final int length) throws NullPointerException, IndexOutOfBoundsException {
        // Guava's non-streaming hashers (farmHashFingerprint64) allocate `len` bytes BEFORE bounds-checking,
        // so an oversized len became an OutOfMemoryError instead of the documented IndexOutOfBoundsException.
        Objects.checkFromIndexSize(off, length, bytes.length);
        gHasher.putBytes(bytes, off, length);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds bytes from a ByteBuffer by delegating to the wrapped Guava hasher's
     * {@code putBytes(ByteBuffer)} method.
     *
     * @param bytes the buffer containing bytes to add
     * @return this hasher instance
     * @throws NullPointerException if {@code bytes} is {@code null}.
     */
    @Override
    public Hasher put(final ByteBuffer bytes) throws NullPointerException {
        gHasher.putBytes(bytes);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds a short value by delegating to the wrapped Guava hasher's {@code putShort()} method.
     *
     * @param s the short value to add
     * @return this hasher instance
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
     *
     * @param i the integer value to add
     * @return this hasher instance
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
     *
     * @param l the long value to add
     * @return this hasher instance
     */
    @Override
    public Hasher put(final long l) {
        gHasher.putLong(l);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds a float value by delegating to the wrapped Guava hasher's {@code putFloat()} method,
     * which uses {@link Float#floatToRawIntBits(float)} and therefore preserves NaN payload bits.
     *
     * @param f the float value to add
     * @return this hasher instance
     */
    @Override
    public Hasher put(final float f) {
        gHasher.putFloat(f);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds a double value by delegating to the wrapped Guava hasher's {@code putDouble()} method,
     * which uses {@link Double#doubleToRawLongBits(double)} and therefore preserves NaN payload bits.
     *
     * @param d the double value to add
     * @return this hasher instance
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
     *
     * @param b the boolean value to add
     * @return this hasher instance
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
     *
     * @param c the character value to add
     * @return this hasher instance
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
     *
     * @param chars the character array to add
     * @return this hasher instance
     */
    @Override
    public Hasher put(final char[] chars) {
        return put(chars, 0, N.len(chars));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds a portion of a character array. Since Guava's Hasher doesn't have a direct
     * method for character arrays, this implementation feeds the same byte stream that calling
     * {@link #put(char)} for each character would: the low byte and then the high byte of every character.
     * Short ranges are added one character at a time; longer ranges are encoded in chunks into a temporary
     * byte buffer that is passed to the wrapped hasher's {@code putBytes}, which is much faster for
     * digest- and checksum-backed functions and produces the identical hash code.
     *
     * <p><b>Implementation Note:</b> This method validates the array bounds using
     * {@link N#checkFromIndexSize} before processing the characters.
     *
     * @param chars the source character array
     * @param off the start offset in the array
     * @param length the number of characters to add
     * @return this hasher instance
     * @throws IllegalArgumentException if {@code length} is negative.
     * @throws IndexOutOfBoundsException if {@code off} is negative or the requested range exceeds the array length, treating a {@code null} array as empty.
     */
    @Override
    public Hasher put(final char[] chars, final int off, final int length) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromIndexSize(off, length, N.len(chars));

        if (length < BULK_CHARS_THRESHOLD) {
            for (int i = off, to = off + length; i < to; i++) {
                put(chars[i]);
            }
        } else {
            // Every Guava hasher treats putChar(c) as the two little-endian bytes of c, and consecutive puts are not
            // delimited, so one putBytes per chunk yields the same hash while avoiding a per-char MessageDigest /
            // Checksum update (and a per-char buffer round trip in the streaming hashers).
            final byte[] buffer = new byte[Math.min(length, BULK_CHARS_CHUNK_SIZE) * 2];

            for (int i = off, to = off + length; i < to;) {
                final int count = Math.min(to - i, BULK_CHARS_CHUNK_SIZE);

                for (int k = 0, j = 0; k < count; k++) {
                    final char c = chars[i + k];
                    buffer[j++] = (byte) c;
                    buffer[j++] = (byte) (c >>> 8);
                }

                gHasher.putBytes(buffer, 0, count * 2);
                i += count;
            }
        }

        return this;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds a character sequence without encoding by delegating to the wrapped Guava
     * hasher's {@code putUnencodedChars()} method.
     *
     * @param charSequence the character sequence to add
     * @return this hasher instance
     * @throws NullPointerException if {@code charSequence} is {@code null}.
     */
    @Override
    public Hasher put(final CharSequence charSequence) throws NullPointerException {
        gHasher.putUnencodedChars(charSequence);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds an encoded character sequence by delegating to the wrapped Guava hasher's
     * {@code putString()} method.
     *
     * @param charSequence the character sequence to add
     * @param charset the charset used to encode the characters
     * @return this hasher instance
     * @throws NullPointerException if {@code charSequence} or {@code charset} is {@code null}.
     */
    @Override
    public Hasher put(final CharSequence charSequence, final Charset charset) throws NullPointerException {
        gHasher.putString(charSequence, charset);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds an object using a funnel by delegating to the wrapped Guava hasher's
     * {@code putObject()} method.
     *
     * @param <T> the type of the instance to add
     * @param instance the object instance to add
     * @param funnel the funnel that translates the object into bytes
     * @return this hasher instance
     * @throws IllegalArgumentException if {@code funnel} is {@code null}.
     */
    @Override
    public <T> Hasher put(final T instance, final Funnel<? super T> funnel) throws IllegalArgumentException {
        N.checkArgNotNull(funnel, cs.funnel);

        gHasher.putObject(instance, funnel);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Computes the final hash code by delegating to the wrapped Guava hasher's
     * {@code hash()} method. After this method is called, the hasher instance should
     * not be used again.
     *
     * @return the computed hash code
     */
    @Override
    public HashCode hash() {
        return gHasher.hash();
    }
}
