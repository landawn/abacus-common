/*
 * Copyright (c) 2021, Haiyang Li.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.guava.hash;

import java.nio.charset.Charset;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;

final class GuavaHashFunction implements HashFunction {

    final com.google.common.hash.HashFunction gHashFunction;

    GuavaHashFunction(final com.google.common.hash.HashFunction gHashFunction) {
        this.gHashFunction = gHashFunction;
    }

    static GuavaHashFunction from(final com.google.common.hash.HashFunction gHashFunction) {
        return new GuavaHashFunction(gHashFunction);
    }

    @Override
    public Hasher newHasher() {
        return GuavaHasher.from(gHashFunction.newHasher());
    }

    @Override
    public Hasher newHasher(final int expectedInputSize) {
        return GuavaHasher.from(gHashFunction.newHasher(expectedInputSize));
    }

    @Override
    public HashCode hash(final int input) {
        return gHashFunction.hashInt(input);
    }

    @Override
    public HashCode hash(final long input) {
        return gHashFunction.hashLong(input);
    }

    @Override
    public HashCode hash(final byte[] input) {
        return gHashFunction.hashBytes(input);
    }

    @Override
    public HashCode hash(final byte[] input, final int off, final int len) {
        return gHashFunction.hashBytes(input, off, len);
    }

    @Override
    public HashCode hash(final CharSequence input) {
        return gHashFunction.hashUnencodedChars(input);
    }

    @Override
    public HashCode hash(final CharSequence input, final Charset charset) {
        return gHashFunction.hashString(input, charset);
    }

    @Override
    public <T> HashCode hash(final T instance, final Funnel<? super T> funnel) {
        return gHashFunction.hashObject(instance, funnel);
    }

    @Override
    public int bits() {
        return gHashFunction.bits();
    }
}
