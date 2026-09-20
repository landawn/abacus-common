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
import com.landawn.abacus.util.cs;

/**
 * Package-private implementation of {@link HashFunction} that wraps a Google Guava
 * {@link com.google.common.hash.HashFunction}. This class serves as an adapter between
 * the abacus-common hashing API and the underlying Guava implementation.
 *
 * <p>This class is immutable and thread-safe, as it delegates all operations to the
 * wrapped Guava hash function which maintains these properties. {@code equals}, {@code hashCode} and
 * {@code toString} are delegated too, so a wrapper compares and prints exactly as the Guava hash function
 * it wraps does - which is value-based for some algorithms and identity-based for others; see
 * {@link #equals(Object)}.
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
     * @param gHashFunction the Guava hash function to wrap, must not be {@code null}
     * @throws NullPointerException if {@code gHashFunction} is {@code null}
     */
    GuavaHashFunction(final com.google.common.hash.HashFunction gHashFunction) throws NullPointerException {
        N.requireNonNull(gHashFunction, cs.gHashFunction);
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
     * @param gHashFunction the Guava hash function to wrap, must not be {@code null}
     * @return a new GuavaHashFunction instance wrapping the given function
     * @throws NullPointerException if {@code gHashFunction} is {@code null}
     */
    static GuavaHashFunction wrap(final com.google.common.hash.HashFunction gHashFunction) throws NullPointerException {
        return new GuavaHashFunction(gHashFunction);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new hasher by delegating to the wrapped Guava hash function's
     * {@code newHasher()} method. The returned hasher is wrapped in a {@link GuavaHasher}
     * to adapt it to the abacus-common API.
     *
     * @return a new hasher backed by the wrapped Guava hash function
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
     *
     * @param expectedInputSize the expected number of bytes to be hashed
     * @return a new hasher backed by the wrapped Guava hash function
     * @throws IllegalArgumentException if {@code expectedInputSize} is negative.
     */
    @Override
    public Hasher newHasher(final int expectedInputSize) throws IllegalArgumentException {
        return GuavaHasher.wrap(gHashFunction.newHasher(expectedInputSize));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Computes the hash of an integer by delegating to the wrapped Guava hash
     * function's {@code hashInt()} method.
     *
     * @param input the integer value to hash
     * @return the hash code for the supplied integer
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
     *
     * @param input the long value to hash
     * @return the hash code for the supplied long
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
     *
     * @param input the byte array to hash
     * @return the hash code for the supplied bytes
     * @throws NullPointerException if {@code input} is {@code null}.
     */
    @Override
    public HashCode hash(final byte[] input) throws NullPointerException {
        return gHashFunction.hashBytes(input);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Computes the hash of a portion of a byte array by delegating to the wrapped
     * Guava hash function's {@code hashBytes(byte[], int, int)} method.
     *
     * @param input the byte array containing the bytes to hash
     * @param off the start offset in the array
     * @param len the number of bytes to hash
     * @return the hash code for the requested byte range
     * @throws NullPointerException if {@code input} is {@code null}.
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is negative, or the requested range exceeds {@code input.length}.
     */
    @Override
    public HashCode hash(final byte[] input, final int off, final int len) throws NullPointerException, IndexOutOfBoundsException {
        return gHashFunction.hashBytes(input, off, len);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Computes the hash of a character sequence without encoding by delegating to
     * the wrapped Guava hash function's {@code hashUnencodedChars()} method.
     *
     * @param input the character sequence to hash
     * @return the hash code for the supplied character sequence
     * @throws NullPointerException if {@code input} is {@code null}.
     */
    @Override
    public HashCode hash(final CharSequence input) throws NullPointerException {
        return gHashFunction.hashUnencodedChars(input);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Computes the hash of an encoded character sequence by delegating to the
     * wrapped Guava hash function's {@code hashString()} method.
     *
     * @param input the character sequence to hash
     * @param charset the charset used to encode the characters before hashing
     * @return the hash code for the supplied encoded character sequence
     * @throws NullPointerException if {@code input} or {@code charset} is {@code null}.
     */
    @Override
    public HashCode hash(final CharSequence input, final Charset charset) throws NullPointerException {
        return gHashFunction.hashString(input, charset);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Computes the hash of an object using a funnel by delegating to the wrapped
     * Guava hash function's {@code hashObject()} method.
     *
     * @param <T> the type of the instance to hash
     * @param instance the object instance to hash
     * @param funnel the funnel that translates the object into bytes
     * @return the hash code for the supplied object
     * @throws IllegalArgumentException if {@code funnel} is {@code null}.
     */
    @Override
    public <T> HashCode hash(final T instance, final Funnel<? super T> funnel) throws IllegalArgumentException {
        N.checkArgNotNull(funnel, cs.funnel);

        return gHashFunction.hashObject(instance, funnel);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the number of bits in hash codes produced by this function by
     * delegating to the wrapped Guava hash function's {@code bits()} method.
     *
     * @return the bit length of hash codes produced by this function
     */
    @Override
    public int bits() {
        return gHashFunction.bits();
    }

    /**
     * Compares two wrappers by the Guava hash function they delegate to.
     *
     * <p>Equality is whatever the wrapped Guava function defines. Seeded {@code murmur3_32(seed)} /
     * {@code murmur3_128(seed)}, {@code sipHash24(k0, k1)}, {@code goodFastHash(n)} and {@code concatenating(...)}
     * results are value-equal, so {@code Hashing.murmur3_128(42)} equals another {@code Hashing.murmur3_128(42)}.
     * The singleton factories ({@code sha256()}, {@code md5()}, {@code crc32()}, {@code farmHashFingerprint64()},
     * ...) are equal because the same instance is returned each time. The {@code hmac*} functions are
     * identity-equal: two calls with the same key are NOT equal, so they do not work as map keys across calls.
     * Without this override the wrapper inherited identity equality from {@link Object} for every function.</p>
     *
     * @param obj the object to compare with
     * @return {@code true} if {@code obj} is a wrapper over an equal Guava hash function
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof GuavaHashFunction other && gHashFunction.equals(other.gHashFunction);
    }

    /**
     * @return the wrapped Guava hash function's hash code, keeping this consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return gHashFunction.hashCode();
    }

    /**
     * Returns the wrapped function's own description, for example {@code "Hashing.murmur3_128(42)"}. Without
     * this the wrapper printed {@code GuavaHashFunction@1b6d3586}, naming neither the algorithm nor the seed.
     * Functions whose Guava implementation does not override {@code toString} (notably the result of
     * {@code concatenating(...)}) still print in Guava's identity form, e.g.
     * {@code com.google.common.hash.Hashing$ConcatenatedHashFunction@88ade00b}.
     *
     * @return the wrapped Guava hash function's string representation
     */
    @Override
    public String toString() {
        return gHashFunction.toString();
    }
}
