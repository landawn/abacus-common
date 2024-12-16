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

package com.landawn.abacus.guava.hash;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;

/**
 * Note: It's copied from Google Guava under Apache License 2.0 and modified.
 *
 * Each hasher should translate all multibyte values ({@link #put(int)}, {@link #put(long)}, etc.) to bytes in
 * little-endian order.
 *
 * <p><b>Warning:</b> The result of calling any methods after calling {@link #hash} is undefined.
 *
 * <p><b>Warning:</b> Using a specific character encoding when hashing a {@link CharSequence} with
 * {@link #put(CharSequence, Charset)} is generally only useful for cross-language
 * compatibility (otherwise prefer {@link #putUnencodedChars}). However, the character encodings
 * must be identical across languages. Also beware that {@link Charset} definitions may occasionally
 * change between Java releases.
 *
 * <p><b>Warning:</b> Chunks of data that are put into the {@link Hasher} are not delimited. The
 * resulting {@link HashCode} is dependent only on the bytes inserted, and the order in which they
 * were inserted, not how those bytes were chunked into discrete put() operations. For example, the
 * following three expressions all generate colliding hash codes: <pre>   {@code
 *
 *   newHasher().putByte(b1).putByte(b2).putByte(b3).hash()
 *   newHasher().putByte(b1).putBytes(new byte[] { b2, b3 }).hash()
 *   newHasher().putBytes(new byte[] { b1, b2, b3 }).hash()}</pre>
 *
 * <p>If you wish to avoid this, you should either prepend or append the size of each chunk. Keep in
 * mind that when dealing with char sequences, the encoded form of two concatenated char sequences
 * is not equivalent to the concatenation of their encoded form. Therefore,
 * {@link #put(CharSequence, Charset)} should only be used consistently with <i>complete</i>
 * sequences and not broken into chunks.
 *
 * @author Kevin Bourrillion
 */
public interface Hasher {

    /**
     *
     * @param b
     * @return
     */
    Hasher put(byte b);

    /**
     *
     * @param bytes
     * @return
     */
    Hasher put(byte[] bytes);

    /**
     *
     * @param bytes
     * @param off
     * @param len
     * @return
     */
    Hasher put(byte[] bytes, int off, int len);

    /**
     *
     * @param bytes
     * @return
     */
    Hasher put(ByteBuffer bytes);

    /**
     *
     * @param s
     * @return
     */
    Hasher put(short s);

    /**
     *
     * @param i
     * @return
     */
    Hasher put(int i);

    /**
     *
     * @param l
     * @return
     */
    Hasher put(long l);

    /**
     * Equivalent to {@code putInt(Float.floatToRawIntBits(f))}.
     *
     * @param f
     * @return
     */
    Hasher put(float f);

    /**
     * Equivalent to {@code putLong(Double.doubleToRawLongBits(d))}.
     *
     * @param d
     * @return
     */
    Hasher put(double d);

    /**
     * Equivalent to {@code putByte(b ? (byte) 1 : (byte) 0)}.
     *
     * @param b
     * @return
     */
    Hasher put(boolean b);

    /**
     *
     * @param c
     * @return
     */
    Hasher put(char c);

    /**
     *
     * @param chars
     * @return
     */
    Hasher put(char[] chars);

    /**
     *
     * @param chars
     * @param off
     * @param len
     * @return
     */
    Hasher put(char[] chars, int off, int len);

    /**
     * Equivalent to processing each {@code char} value in the {@code CharSequence}, in order. In
     * other words, no character encoding is performed; the low byte and high byte of each {@code
     * char} are hashed directly (in that order). The input must not be updated while this method is
     * in progress.
     *
     * <p><b>Warning:</b> This method will produce different output than most other languages do when
     * running the same hash function on the equivalent input. For cross-language compatibility, use
     * {@link #putString}, usually with a charset of UTF-8. For other use cases, use {@code
     * putUnencodedChars}.
     *
     * @param charSequence
     * @return
     */
    Hasher put(CharSequence charSequence);

    /**
     * Equivalent to {@code putBytes(charSequence.toString().getBytes(charset))}.
     *
     * <p><b>Warning:</b> This method, which reencodes the input before hashing it, is useful only for
     * cross-language compatibility. For other use cases, prefer {@link #putUnencodedChars}, which is
     * faster, produces the same output across Java releases, and hashes every {@code char} in the
     * input, even if some are invalid.
     *
     * @param charSequence
     * @param charset
     * @return
     */
    Hasher put(CharSequence charSequence, Charset charset);

    /**
     * A simple convenience for {@code funnel.funnel(object, this)}.
     *
     * @param <T>
     * @param instance
     * @param funnel
     * @return
     */
    <T> Hasher put(T instance, Funnel<? super T> funnel);

    /**
     * Computes a hash code based on the data that have been provided to this hasher. The result is
     * unspecified if this method is called more than once on the same instance.
     *
     * @return
     */
    HashCode hash();
}
