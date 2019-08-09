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

import java.nio.charset.Charset;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.function.BiConsumer;

// TODO: Auto-generated Javadoc
/**
 * Note: It's copied from Google Guava under Apache License 2.0
 * 
 * An abstract hasher, implementing {@link #put(boolean)}, {@link #put(double)},
 * {@link #put(float)}, {@link #put(CharSequence)}, and
 * {@link #put(CharSequence, Charset)} as prescribed by {@link Hasher}.
 *
 * @author Dimitris Andreou
 */

abstract class AbstractHasher implements Hasher {

    /**
     * Put.
     *
     * @param b the b
     * @return the hasher
     */
    @Override
    public final Hasher put(boolean b) {
        return put(b ? (byte) 1 : (byte) 0);
    }

    /**
     * Put.
     *
     * @param d the d
     * @return the hasher
     */
    @Override
    public final Hasher put(double d) {
        return put(Double.doubleToRawLongBits(d));
    }

    /**
     * Put.
     *
     * @param f the f
     * @return the hasher
     */
    @Override
    public final Hasher put(float f) {
        return put(Float.floatToRawIntBits(f));
    }

    /**
     * Put.
     *
     * @param chars the chars
     * @return the hasher
     */
    @Override
    public Hasher put(char[] chars) {
        return put(chars, 0, chars.length);
    }

    /**
     * Put.
     *
     * @param chars the chars
     * @param off the off
     * @param len the len
     * @return the hasher
     */
    @Override
    public Hasher put(char[] chars, int off, int len) {
        Util.checkPositionIndexes(off, off + len, chars.length);

        for (int i = off, to = off + len; i < to; i++) {
            put(chars[i]);
        }

        return this;
    }

    /**
     * Put.
     *
     * @param charSequence the char sequence
     * @return the hasher
     */
    @Override
    public Hasher put(CharSequence charSequence) {
        for (int i = 0, len = charSequence.length(); i < len; i++) {
            put(charSequence.charAt(i));
        }

        return this;
    }

    /**
     * Put.
     *
     * @param charSequence the char sequence
     * @param charset the charset
     * @return the hasher
     */
    @Override
    public Hasher put(CharSequence charSequence, Charset charset) {
        return put(charSequence.toString().getBytes(charset));
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
        N.checkArgNotNull(funnel);

        funnel.accept(instance, this);

        return this;
    }
}
