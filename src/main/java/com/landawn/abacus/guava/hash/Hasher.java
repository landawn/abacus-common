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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;

/**
 * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
 * 
 * A stateful object for computing hash codes incrementally. Hasher instances are created
 * by {@link HashFunction#newHasher()} and accumulate data through their various put methods
 * before generating a final {@link HashCode}.
 *
 * <p><b>Usage Pattern</b></p>
 * <ol>
 *   <li>Obtain a new Hasher from a HashFunction</li>
 *   <li>Add data using put methods (can be chained)</li>
 *   <li>Call {@link #hash()} to get the final HashCode</li>
 * </ol>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * long timestamp = System.currentTimeMillis();
 * byte[] sessionToken = "token".getBytes();
 * HashCode hash = Hashing.sha256()
 *     .newHasher()
 *     .put("username", StandardCharsets.UTF_8)
 *     .put(timestamp)
 *     .put(sessionToken)
 *     .hash();
 * }</pre>
 *
 * <p><b>Important Notes</b></p>
 * <ul>
 *   <li><b>Single use:</b> Each Hasher instance should be used for exactly one hash computation</li>
 *   <li><b>Order matters:</b> Data must be added in a consistent order for reproducible results</li>
 *   <li><b>No delimiters:</b> Consecutive put operations are not delimited. {@code putBytes(new byte[] {1, 2})} 
 *       followed by {@code putBytes(new byte[] {3, 4})} produces the same result as 
 *       {@code putBytes(new byte[] {1, 2, 3, 4})}</li>
 *   <li><b>Multibyte values:</b> All multibyte values (int, long, etc.) are interpreted in 
 *       little-endian order</li>
 * </ul>
 *
 * <p><b>Avoiding Collisions</b></p>
 * <p>Since data chunks are not delimited, be careful to avoid unintended collisions:
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // These produce the same hash:
 * hasher1.put("foo", StandardCharsets.UTF_8).put("bar", StandardCharsets.UTF_8).hash();
 * hasher2.put("foobar", StandardCharsets.UTF_8).hash();
 *
 * // To avoid this, consider adding delimiters:
 * hasher3.put("foo", StandardCharsets.UTF_8).put('\0').put("bar", StandardCharsets.UTF_8).hash();
 * }</pre>
 * 
 * <p><b>Warning:</b> The result of calling any methods after {@link #hash()} is undefined.
 * Do not reuse a Hasher instance after calling hash().
 *
 * @author Kevin Bourrillion
 * @see HashFunction
 * @see HashCode
 */
public interface Hasher {

    /**
     * Adds a single byte to this hasher's internal state.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * hasher.put((byte) 0xFF).put((byte) 0x00);
     * }</pre>
     *
     * @param b the byte value to add to the hash computation
     * @return this hasher instance for method chaining
     */
    Hasher put(byte b);

    /**
     * Adds all bytes in the given array to this hasher's internal state.
     * This is equivalent to {@code put(bytes, 0, bytes.length)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = "Hello".getBytes(StandardCharsets.UTF_8);
     * hasher.put(data);
     * }</pre>
     *
     * @param bytes the byte array containing data to add to the hash computation
     * @return this hasher instance for method chaining
     */
    Hasher put(byte[] bytes);

    /**
     * Adds a portion of the given byte array to this hasher's internal state.
     * Only bytes from {@code off} to {@code off + len - 1} (inclusive) are processed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] buffer = new byte[1024];
     * int bytesRead = inputStream.read(buffer);
     * hasher.put(buffer, 0, bytesRead);
     * }</pre>
     *
     * @param bytes the byte array containing data to add to the hash computation
     * @param off the starting offset in the array (inclusive, zero-based)
     * @param len the number of bytes to process from the array
     * @return this hasher instance for method chaining
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is negative, or if
     *         {@code off + len > bytes.length}
     */
    Hasher put(byte[] bytes, int off, int len);

    /**
     * Adds all remaining bytes from the given ByteBuffer to this hasher's internal state.
     * This method reads from the buffer's current position to its limit, and advances
     * the buffer's position accordingly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteBuffer buffer = ByteBuffer.wrap("data".getBytes());
     * hasher.put(buffer); // Buffer position is now at limit
     * }</pre>
     *
     * @param bytes the ByteBuffer containing data to add to the hash computation
     * @return this hasher instance for method chaining
     */
    Hasher put(ByteBuffer bytes);

    /**
     * Adds a short value to this hasher's internal state. The short is interpreted
     * in little-endian byte order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * hasher.put((short) 12345).put((short) -1);
     * }</pre>
     *
     * @param s the short value to add to the hash computation (interpreted in little-endian byte order)
     * @return this hasher instance for method chaining
     */
    Hasher put(short s);

    /**
     * Adds an integer value to this hasher's internal state. The integer is interpreted
     * in little-endian byte order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * hasher.put(42).put(Integer.MAX_VALUE);
     * }</pre>
     *
     * @param i the integer value to add to the hash computation (interpreted in little-endian byte order)
     * @return this hasher instance for method chaining
     */
    Hasher put(int i);

    /**
     * Adds a long value to this hasher's internal state. The long is interpreted
     * in little-endian byte order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * hasher.put(System.currentTimeMillis()).put(userId);
     * }</pre>
     *
     * @param l the long value to add to the hash computation (interpreted in little-endian byte order)
     * @return this hasher instance for method chaining
     */
    Hasher put(long l);

    /**
     * Adds a float value to this hasher's internal state. This is equivalent to
     * {@code put(Float.floatToRawIntBits(f))}, which means the float's binary
     * representation is hashed, not its numeric value.
     *
     * <p><b>Note:</b> NaN values may have different binary representations but the
     * same numeric meaning. Use {@link Float#floatToRawIntBits(float)} explicitly
     * if you need consistent handling of NaN values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * hasher.put(3.14159f).put(Float.POSITIVE_INFINITY);
     * }</pre>
     *
     * @param f the float value to add to the hash computation (using its raw binary representation)
     * @return this hasher instance for method chaining
     */
    Hasher put(float f);

    /**
     * Adds a double value to this hasher's internal state. This is equivalent to
     * {@code put(Double.doubleToRawLongBits(d))}, which means the double's binary
     * representation is hashed, not its numeric value.
     *
     * <p><b>Note:</b> NaN values may have different binary representations but the
     * same numeric meaning. Use {@link Double#doubleToRawLongBits(double)} explicitly
     * if you need consistent handling of NaN values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * hasher.put(Math.PI).put(Double.NaN);
     * }</pre>
     *
     * @param d the double value to add to the hash computation (using its raw binary representation)
     * @return this hasher instance for method chaining
     */
    Hasher put(double d);

    /**
     * Adds a boolean value to this hasher's internal state. This is equivalent to
     * {@code put((byte) 1)} for {@code true} and {@code put((byte) 0)} for {@code false}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * hasher.put(user.isActive()).put(user.isVerified());
     * }</pre>
     *
     * @param b the boolean value to add to the hash computation (true=1, false=0)
     * @return this hasher instance for method chaining
     */
    Hasher put(boolean b);

    /**
     * Adds a character value to this hasher's internal state. The character is
     * processed by first adding its low byte, then its high byte (little-endian order).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * hasher.put('A').put('\n').put('中');
     * }</pre>
     *
     * @param c the character value to add to the hash computation (interpreted in little-endian byte order)
     * @return this hasher instance for method chaining
     */
    Hasher put(char c);

    /**
     * Adds all characters from the given array to this hasher's internal state.
     * This is equivalent to {@code put(chars, 0, chars.length)}.
     *
     * <p>Each character is processed in little-endian order (low byte first, then high byte).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] password = {'s', 'e', 'c', 'r', 'e', 't'};
     * hasher.put(password);
     * }</pre>
     *
     * @param chars the character array containing data to add to the hash computation
     * @return this hasher instance for method chaining
     */
    Hasher put(char[] chars);

    /**
     * Adds a portion of the given character array to this hasher's internal state.
     * Only characters from {@code off} to {@code off + len - 1} (inclusive) are processed.
     *
     * <p>Each character is processed in little-endian order (low byte first, then high byte).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] buffer = new char[100];
     * int charsRead = reader.read(buffer);
     * hasher.put(buffer, 0, charsRead);
     * }</pre>
     *
     * @param chars the character array containing data to add to the hash computation
     * @param off the starting offset in the array (inclusive, zero-based)
     * @param len the number of characters to process from the array
     * @return this hasher instance for method chaining
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is negative, or if
     *         {@code off + len > chars.length}
     */
    Hasher put(char[] chars, int off, int len);

    /**
     * Adds all characters from the given CharSequence to this hasher's internal state
     * without performing any character encoding. Each character is processed by hashing
     * its low byte followed by its high byte (little-endian order).
     *
     * <p>This method is faster than {@link #put(CharSequence, Charset)} and produces
     * consistent results across Java versions, but the output will differ from most
     * other programming languages when hashing the same string.
     *
     * <p><b>Warning:</b> For cross-language compatibility, use {@link #put(CharSequence, Charset)}
     * with a specific encoding (usually UTF-8) instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * hasher.put("fast hash").put(new StringBuilder("builder"));
     * }</pre>
     *
     * @param charSequence the character sequence to add to the hash computation (String, StringBuilder, etc.)
     * @return this hasher instance for method chaining
     */
    Hasher put(CharSequence charSequence);

    /**
     * Adds the given CharSequence to this hasher's internal state after encoding it
     * using the specified charset. This method first converts the characters to bytes
     * using the charset, then hashes those bytes.
     *
     * <p>This method is useful for cross-language compatibility as it produces the same
     * hash values as other languages when using the same character encoding. UTF-8 is
     * the most commonly used encoding for this purpose.
     *
     * <p><b>Warning:</b> This method is slower than {@link #put(CharSequence)} due to
     * the encoding step. Use the unencoded version for better performance when
     * cross-language compatibility is not required.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * hasher.put("Hello 世界", StandardCharsets.UTF_8)
     *       .put("Здравствуй", StandardCharsets.UTF_8);
     * }</pre>
     *
     * @param charSequence the character sequence to encode and add to the hash computation
     * @param charset the character encoding to use for converting the sequence to bytes
     * @return this hasher instance for method chaining
     */
    Hasher put(CharSequence charSequence, Charset charset);

    /**
     * Adds an arbitrary object to this hasher's internal state using a {@link Funnel}
     * to decompose the object into primitive values. This is a convenience method
     * equivalent to {@code funnel.funnel(instance, this)}.
     *
     * <p>The funnel is responsible for breaking down the object into a sequence of
     * primitive values that can be hashed. This ensures consistent hashing of complex
     * objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Person {
     *     String name;
     *     int age;
     *     long id;
     *     Person(String name, int age, long id) {
     *         this.name = name;
     *         this.age = age;
     *         this.id = id;
     *     }
     * }
     *
     * Funnel<Person> personFunnel = (person, into) -> {
     *     into.putString(person.name, StandardCharsets.UTF_8)
     *         .putInt(person.age)
     *         .putLong(person.id);
     * };
     *
     * Person person = new Person("Alice", 30, 12345L);
     * hasher.put(person, personFunnel);
     * }</pre>
     *
     * @param <T> the type of object to hash
     * @param instance the object instance to add to the hash computation
     * @param funnel the funnel used to decompose the object into primitive values
     * @return this hasher instance for method chaining
     */
    <T> Hasher put(T instance, Funnel<? super T> funnel);

    /**
     * Computes and returns the final hash code based on all data that has been added
     * to this hasher. After calling this method, the hasher instance should not be used
     * again.
     * 
     * <p>The returned {@link HashCode} contains the computed hash value and provides
     * various methods to access it (as bytes, as int, as long, as hex string, etc.).
     * 
     * <p><b>Warning:</b> The behavior of calling any methods on this hasher after
     * calling hash() is undefined. Create a new hasher for each hash computation.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long timestamp = System.currentTimeMillis();
     * HashCode result = hasher
     *     .put("data", StandardCharsets.UTF_8)
     *     .put(timestamp)
     *     .hash();
     *
     * byte[] hashBytes = result.asBytes();
     * String hashHex = result.toString();
     * }</pre>
     *
     * @return the computed hash code
     */
    HashCode hash();
}
