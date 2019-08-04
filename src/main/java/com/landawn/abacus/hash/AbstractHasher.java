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
    @Override
    public final Hasher put(boolean b) {
        return put(b ? (byte) 1 : (byte) 0);
    }

    @Override
    public final Hasher put(double d) {
        return put(Double.doubleToRawLongBits(d));
    }

    @Override
    public final Hasher put(float f) {
        return put(Float.floatToRawIntBits(f));
    }

    @Override
    public Hasher put(char[] chars) {
        return put(chars, 0, chars.length);
    }

    @Override
    public Hasher put(char[] chars, int off, int len) {
        Util.checkPositionIndexes(off, off + len, chars.length);

        for (int i = off, to = off + len; i < to; i++) {
            put(chars[i]);
        }

        return this;
    }

    @Override
    public Hasher put(CharSequence charSequence) {
        for (int i = 0, len = charSequence.length(); i < len; i++) {
            put(charSequence.charAt(i));
        }

        return this;
    }

    @Override
    public Hasher put(CharSequence charSequence, Charset charset) {
        return put(charSequence.toString().getBytes(charset));
    }

    @Override
    public <T> Hasher put(T instance, BiConsumer<? super T, ? super Hasher> funnel) {
        N.checkArgNotNull(funnel);

        funnel.accept(instance, this);

        return this;
    }
}
