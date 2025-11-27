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

import java.nio.charset.Charset;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;
import com.landawn.abacus.util.N;

/**
 * Package-private implementation of {@link HashFunction} that wraps a Google Guava
 * {@link com.google.common.hash.HashFunction}. This class serves as an adapter between
 * the abacus-common hashing API and the underlying Guava implementation.
 * 
 * <p>This class is immutable and thread-safe, as it delegates all operations to the
 * wrapped Guava hash function which maintains these properties.
 * 
 * <p><b>Implementation Note:</b> This class is not intended for direct use by clients.
 * Use the factory methods in {@link Hashing} to obtain hash function instances.
 * 
 * @see HashFunction
 * @see Hashing
 */
@SuppressWarnings("ClassCanBeRecord")
final class GuavaHashFunction implements HashFunction {

    /**
     * The wrapped Google Guava hash function that performs the actual hashing operations.
     */
    final com.google.common.hash.HashFunction gHashFunction;

    /**
     * Constructs a new GuavaHashFunction wrapping the specified Guava hash function.
     * 
     * @param gHashFunction the Guava hash function to wrap
     */
    GuavaHashFunction(final com.google.common.hash.HashFunction gHashFunction) {
        N.requireNonNull(gHashFunction, "gHashFunction");
        this.gHashFunction = gHashFunction;
    }

    /**
     * Static factory method that creates a new GuavaHashFunction wrapping the given
     * Guava hash function. This method provides a more convenient way to create
     * instances compared to using the constructor directly.
     *
     * <p>This method is used internally by the {@link Hashing} utility class to adapt
     * Guava's hash functions to the abacus-common API. It ensures proper wrapping and
     * validation of the provided hash function.
     *
     * <p><b>Note:</b> This is an internal method. Client code should obtain HashFunction
     * instances through the factory methods in {@link Hashing} instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * com.google.common.hash.HashFunction guavaHash = com.google.common.hash.Hashing.sha256();
     * HashFunction wrapped = GuavaHashFunction.wrap(guavaHash);
     * }</pre>
     *
     * @param gHashFunction the Guava hash function to wrap (must not be null)
     * @return a new GuavaHashFunction instance wrapping the given function
     */
    static GuavaHashFunction wrap(final com.google.common.hash.HashFunction gHashFunction) {
        return new GuavaHashFunction(gHashFunction);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Creates a new hasher by delegating to the wrapped Guava hash function's
     * {@code newHasher()} method. The returned hasher is wrapped in a {@link GuavaHasher}
     * to adapt it to the abacus-common API.
     */
    @Override
    public Hasher newHasher() {
        return GuavaHasher.wrap(gHashFunction.newHasher());
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Creates a new hasher optimized for the expected input size by delegating to
     * the wrapped Guava hash function's {@code newHasher(int)} method. The returned
     * hasher is wrapped in a {@link GuavaHasher}.
     */
    @Override
    public Hasher newHasher(final int expectedInputSize) {
        return GuavaHasher.wrap(gHashFunction.newHasher(expectedInputSize));
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Computes the hash of an integer by delegating to the wrapped Guava hash
     * function's {@code hashInt()} method.
     */
    @Override
    public HashCode hash(final int input) {
        return gHashFunction.hashInt(input);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Computes the hash of a long by delegating to the wrapped Guava hash
     * function's {@code hashLong()} method.
     */
    @Override
    public HashCode hash(final long input) {
        return gHashFunction.hashLong(input);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Computes the hash of a byte array by delegating to the wrapped Guava hash
     * function's {@code hashBytes()} method.
     */
    @Override
    public HashCode hash(final byte[] input) {
        return gHashFunction.hashBytes(input);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Computes the hash of a portion of a byte array by delegating to the wrapped
     * Guava hash function's {@code hashBytes(byte[], int, int)} method.
     */
    @Override
    public HashCode hash(final byte[] input, final int off, final int len) {
        return gHashFunction.hashBytes(input, off, len);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Computes the hash of a character sequence without encoding by delegating to
     * the wrapped Guava hash function's {@code hashUnencodedChars()} method.
     */
    @Override
    public HashCode hash(final CharSequence input) {
        return gHashFunction.hashUnencodedChars(input);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Computes the hash of an encoded character sequence by delegating to the
     * wrapped Guava hash function's {@code hashString()} method.
     */
    @Override
    public HashCode hash(final CharSequence input, final Charset charset) {
        return gHashFunction.hashString(input, charset);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Computes the hash of an object using a funnel by delegating to the wrapped
     * Guava hash function's {@code hashObject()} method.
     */
    @Override
    public <T> HashCode hash(final T instance, final Funnel<? super T> funnel) {
        return gHashFunction.hashObject(instance, funnel);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Returns the number of bits in hash codes produced by this function by
     * delegating to the wrapped Guava hash function's {@code bits()} method.
     */
    @Override
    public int bits() {
        return gHashFunction.bits();
    }
}
