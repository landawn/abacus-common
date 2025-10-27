/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.guava.hash;

import java.nio.charset.Charset;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;

/**
 * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
 * 
 * A hash function is a collision-averse pure function that maps an arbitrary block of
 * data to a fixed-size number called a hash code. This interface defines the contract
 * for all hash functions in this library.
 *
 * <p><b>Core Properties</b></p>
 * <ul>
 *   <li><b>Deterministic:</b> The same input always produces the same output</li>
 *   <li><b>Fixed output size:</b> All hash codes from a function have the same bit length</li>
 *   <li><b>Collision-averse:</b> Different inputs should rarely produce the same output</li>
 *   <li><b>Stateless:</b> Hash functions maintain no state between invocations</li>
 * </ul>
 *
 * <p><b>Types of Hash Functions</b></p>
 * <ul>
 *   <li><b>Cryptographic:</b> Designed for security (e.g., SHA-256, SHA-512)</li>
 *   <li><b>Non-cryptographic:</b> Designed for speed and distribution (e.g., Murmur3, CityHash)</li>
 *   <li><b>Checksums:</b> Designed for error detection (e.g., CRC32, Adler32)</li>
 * </ul>
 *
 * <p><b>Usage Patterns</b></p>
 *
 * <p><b>Simple hashing:</b>
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * HashFunction hf = Hashing.sha256();
 * HashCode hash = hf.hash("hello world".getBytes());
 * }</pre>
 * 
 * <p><b>Incremental hashing with Hasher:</b>
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * HashFunction hf = Hashing.murmur3_128();
 * HashCode hash = hf.newHasher()
 *     .putLong(userId)
 *     .putString(userName, StandardCharsets.UTF_8)
 *     .putInt(timestamp)
 *     .hash();
 * }</pre>
 * 
 * <p><b>Hashing complex objects with Funnel:</b>
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Funnel<Person> personFunnel = (person, hasher) -> {
 *     hasher.putString(person.firstName, Charsets.UTF_8)
 *           .putString(person.lastName, Charsets.UTF_8)
 *           .putInt(person.age);
 * };
 * HashCode hash = hf.hash(person, personFunnel);
 * }</pre>
 *
 * <p><b>Implementation Notes</b></p>
 * <ul>
 *   <li>All multibyte values are interpreted in little-endian order</li>
 *   <li>Implementations should be thread-safe</li>
 *   <li>The quality of hash distribution affects performance in hash tables</li>
 * </ul>
 *
 * @author Kevin Bourrillion
 * @see Hashing
 * @see HashCode
 * @see Hasher
 */
public interface HashFunction {

    /**
     * Creates a new {@link Hasher} instance for incremental hashing. The returned hasher
     * is stateful and collects data through its various put methods before generating
     * a final hash code.
     * 
     * <p>Each hasher instance should be used for exactly one hash computation. After
     * calling {@link Hasher#hash()}, the hasher should not be used again.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashFunction hf = Hashing.md5();
     * HashCode hc = hf.newHasher()
     *     .putLong(id)
     *     .putBoolean(isActive)
     *     .putString(name, StandardCharsets.UTF_8)
     *     .hash();
     * }</pre>
     *
     * @return a new, empty hasher instance ready to receive data
     */
    Hasher newHasher();

    /**
     * Creates a new {@link Hasher} instance with a hint about the expected input size.
     * This can improve performance for non-streaming hash functions that need to buffer
     * their entire input before processing.
     * 
     * <p>The hint is only an optimization; providing an incorrect value will not affect
     * correctness, only performance. If unsure, use {@link #newHasher()} instead.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] largeData = new byte[1024 * 1024]; // 1MB
     * HashCode hash = hashFunction.newHasher(largeData.length)
     *     .putBytes(largeData)
     *     .hash();
     * }</pre>
     *
     * @param expectedInputSize a hint about the expected total size of input in bytes
     *                          (must be non-negative)
     * @return a new hasher instance optimized for the expected input size
     * @throws IllegalArgumentException if expectedInputSize is negative
     */
    Hasher newHasher(int expectedInputSize);

    /**
     * Computes the hash code for a single integer value. This is a convenience method
     * equivalent to {@code newHasher().putInt(input).hash()}.
     * 
     * <p>The integer is interpreted in little-endian byte order. The implementation may
     * be optimized compared to using a Hasher.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashCode hash = Hashing.murmur3_32().hash(42);
     * }</pre>
     *
     * @param input the integer value to hash
     * @return the hash code for the input value
     */
    HashCode hash(int input);

    /**
     * Computes the hash code for a single long value. This is a convenience method
     * equivalent to {@code newHasher().putLong(input).hash()}.
     * 
     * <p>The long is interpreted in little-endian byte order. The implementation may
     * be optimized compared to using a Hasher.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashCode hash = Hashing.sha256().hash(System.currentTimeMillis());
     * }</pre>
     *
     * @param input the long value to hash
     * @return the hash code for the input value
     */
    HashCode hash(long input);

    /**
     * Computes the hash code for a byte array. This is a convenience method equivalent
     * to {@code newHasher().putBytes(input).hash()}.
     * 
     * <p>This is one of the most commonly used methods, as many data types can be
     * converted to byte arrays. The implementation may be optimized compared to using
     * a Hasher.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);
     * HashCode hash = Hashing.sha256().hash(data);
     * }</pre>
     *
     * @param input the byte array to hash
     * @return the hash code for the input bytes
     */
    HashCode hash(byte[] input);

    /**
     * Computes the hash code for a portion of a byte array. This is a convenience method
     * equivalent to {@code newHasher().putBytes(input, off, len).hash()}.
     * 
     * <p>Only the bytes from {@code input[off]} through {@code input[off + len - 1]}
     * are hashed. This is useful when working with buffers or when only part of an
     * array contains valid data.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] buffer = new byte[1024];
     * int bytesRead = inputStream.read(buffer);
     * HashCode hash = Hashing.md5().hash(buffer, 0, bytesRead);
     * }</pre>
     *
     * @param input the byte array containing data to hash
     * @param off the starting offset in the array
     * @param len the number of bytes to hash
     * @return the hash code for the specified bytes
     * @throws IndexOutOfBoundsException if {@code off < 0} or {@code off + len > input.length}
     *                                   or {@code len < 0}
     */
    HashCode hash(byte[] input, int off, int len);

    /**
     * Computes the hash code for a character sequence without encoding. This is a
     * convenience method equivalent to {@code newHasher().putUnencodedChars(input).hash()}.
     * 
     * <p>Each character is hashed directly by hashing its low byte followed by its high
     * byte (little-endian order). No character encoding is performed, making this method
     * fast and consistent across Java versions.
     * 
     * <p><b>Warning:</b> This method produces different output than most other languages
     * when hashing strings. For cross-language compatibility, use {@link #hash(CharSequence, Charset)}
     * with UTF-8 encoding instead.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashCode hash = Hashing.murmur3_128().hash("fast hash");
     * }</pre>
     *
     * @param input the character sequence to hash
     * @return the hash code for the input characters
     */
    HashCode hash(CharSequence input);

    /**
     * Computes the hash code for a character sequence using the specified character
     * encoding. This is a convenience method equivalent to
     * {@code newHasher().putString(input, charset).hash()}.
     * 
     * <p>The characters are first encoded to bytes using the specified charset, then
     * those bytes are hashed. This method is useful for cross-language compatibility
     * as it produces consistent results when the same encoding is used.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String text = "Hello 世界";
     * HashCode hash = Hashing.sha256().hash(text, StandardCharsets.UTF_8);
     * }</pre>
     *
     * @param input the character sequence to hash
     * @param charset the character encoding to use
     * @return the hash code for the encoded input
     */
    HashCode hash(CharSequence input, Charset charset);

    /**
     * Computes the hash code for an arbitrary object using a {@link Funnel} to decompose
     * the object into primitive values. This is a convenience method equivalent to
     * {@code newHasher().putObject(instance, funnel).hash()}.
     *
     * <p>The funnel defines how to extract data from the object and feed it to the hasher.
     * This approach ensures consistent hashing of complex objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Person {
     *     String name;
     *     int age;
     * }
     *
     * Funnel<Person> personFunnel = (person, into) -> {
     *     into.putString(person.name, StandardCharsets.UTF_8)
     *         .putInt(person.age);
     * };
     *
     * Person person = new Person("Alice", 30);
     * HashCode hash = Hashing.sha256().hash(person, personFunnel);
     * }</pre>
     *
     * @param <T> the type of object to hash
     * @param instance the object instance to hash
     * @param funnel the funnel to decompose the object with
     * @return the hash code for the funneled object data
     */
    <T> HashCode hash(T instance, Funnel<? super T> funnel);

    /**
     * Returns the number of bits in each hash code produced by this hash function.
     * This value is constant for a given hash function instance.
     * 
     * <p>Common bit lengths include:
     * <ul>
     *   <li>32 bits: CRC32, Adler32, Murmur3_32</li>
     *   <li>64 bits: SipHash24, FarmHash Fingerprint64</li>
     *   <li>128 bits: MD5, Murmur3_128</li>
     *   <li>160 bits: SHA-1</li>
     *   <li>256 bits: SHA-256</li>
     *   <li>512 bits: SHA-512</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashFunction sha256 = Hashing.sha256();
     * System.out.println(sha256.bits()); // prints: 256
     * }</pre>
     *
     * @return the number of bits in hash codes produced by this function (always positive
     *         and typically a multiple of 32)
     */
    int bits();
}
