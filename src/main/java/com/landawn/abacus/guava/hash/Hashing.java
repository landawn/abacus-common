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

import java.security.Key;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import com.google.common.hash.HashCode;
import com.landawn.abacus.util.N;

/**
 * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
 * 
 * Utility class providing static factory methods to obtain various {@link HashFunction} instances
 * and other hashing-related utilities. This class serves as the main entry point for accessing
 * different hashing algorithms including cryptographic (SHA, MD5) and non-cryptographic 
 * (Murmur3, CRC32) hash functions.
 * 
 * <p>This class is a wrapper around Google Guava's hashing utilities, providing a convenient
 * API for common hashing operations. It includes support for:
 * <ul>
 *   <li>General-purpose hash functions (goodFastHash)</li>
 *   <li>Murmur3 hash functions (32-bit and 128-bit variants)</li>
 *   <li>SipHash-2-4 algorithm</li>
 *   <li>Cryptographic hash functions (MD5, SHA-1, SHA-256, SHA-384, SHA-512)</li>
 *   <li>HMAC variants</li>
 *   <li>Checksum algorithms (CRC32, CRC32C, Adler32)</li>
 *   <li>FarmHash Fingerprint64</li>
 *   <li>Hash function composition (concatenating)</li>
 *   <li>Hash code combination methods</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple hashing
 * HashCode hash = Hashing.sha256().hash("Hello World".getBytes());
 *
 * // Using a Hasher for multiple inputs
 * long userId = 12345L;
 * String userName = "john";
 * HashCode combined = Hashing.murmur3_128()
 *     .newHasher()
 *     .put(userId)
 *     .put(userName, StandardCharsets.UTF_8)
 *     .hash();
 * }</pre>
 *
 * @author Kevin Bourrillion
 * @author Dimitris Andreou
 * @author Kurt Alfred Kluever
 * @see HashFunction
 * @see HashCode
 * @see Hasher
 */
public final class Hashing {

    private Hashing() {
        // singleton for utility class.
    }

    /**
     * Returns a general-purpose, temporary-use, non-cryptographic hash function that produces
     * hash codes of at least the specified number of bits. The returned function is suitable
     * for in-memory data structures but should not be used for persistent storage or
     * cross-process communication.
     *
     * <p>The algorithm implemented by the returned function is unspecified and may change
     * between different executions of the program. A new random seed is chosen each time
     * the Hashing class is loaded.
     *
     * <p><b>Warning:</b> Do not use this method if hash codes may escape the current process
     * in any way (e.g., being sent over RPC or saved to disk). The hash codes will not be
     * consistent across different JVM instances.
     *
     * <p>Multiple calls to this method with the same {@code minimumBits} value within the
     * same JVM instance will return identically-behaving HashFunction instances.
     *
     * <p><b>When to Use goodFastHash() vs. Specific Algorithms:</b></p>
     * <table border="1">
     * <caption>Hash Function Selection Guide</caption>
     * <tr>
     *   <th>Use Case</th>
     *   <th>Recommended Function</th>
     *   <th>Reason</th>
     * </tr>
     * <tr>
     *   <td>In-memory hash tables, caches</td>
     *   <td>{@code goodFastHash(32)} or {@code goodFastHash(64)}</td>
     *   <td>Optimized for speed, good distribution</td>
     * </tr>
     * <tr>
     *   <td>Persistent storage, cross-process</td>
     *   <td>{@link #murmur3_128()} or {@link #murmur3_32()}</td>
     *   <td>Consistent across JVM restarts</td>
     * </tr>
     * <tr>
     *   <td>Security, data integrity</td>
     *   <td>{@link #sha256()} or {@link #sha512()}</td>
     *   <td>Cryptographically secure</td>
     * </tr>
     * <tr>
     *   <td>Checksums, error detection</td>
     *   <td>{@link #crc32c()} or {@link #crc32()}</td>
     *   <td>Designed for error detection</td>
     * </tr>
     * <tr>
     *   <td>Hash table DoS protection</td>
     *   <td>{@link #sipHash24(long, long)}</td>
     *   <td>Keyed hashing resists attacks</td>
     * </tr>
     * </table>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For temporary in-memory use
     * HashFunction hashFunc = Hashing.goodFastHash(128);
     * HashCode hash = hashFunc.hash("data".getBytes());
     *
     * // For persistent use, choose a specific algorithm instead:
     * HashFunction persistentHash = Hashing.murmur3_128();
     * HashCode stableHash = persistentHash.hash("data".getBytes());
     * }</pre>
     *
     * @param minimumBits a positive integer specifying the minimum number of bits the
     *                    hash code should have (the actual number of bits may be higher,
     *                    typically rounded up to a multiple of 32 or 64)
     * @return a hash function that produces hash codes of length {@code minimumBits} or greater
     * @throws IllegalArgumentException if {@code minimumBits} is not positive
     * @see #murmur3_128()
     * @see #murmur3_32()
     * @see #sha256()
     * @see #sipHash24(long, long)
     */
    public static HashFunction goodFastHash(final int minimumBits) {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.goodFastHash(minimumBits));
    }

    /**
     * Returns a hash function implementing the 32-bit Murmur3 algorithm (x86 variant) with
     * the specified seed value. This is a non-cryptographic hash function known for its
     * speed and good distribution properties.
     *
     * <p>The implementation corresponds to the MurmurHash3_x86_32 function (Murmur3A) from
     * the original C++ implementation. This is the little-endian variant.
     *
     * <p>This method returns the fixed version that corrects a bug in the original
     * implementation, hence the internal name {@code murmur3_32_fixed}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashFunction murmur = Hashing.murmur3_32(42); // seed = 42
     * HashCode hash = murmur.hash("example".getBytes());
     * }</pre>
     *
     * @param seed the seed value to initialize the hash function (different seeds produce different hash values for the same input)
     * @return a Murmur3 32-bit hash function initialized with the given seed
     */
    public static HashFunction murmur3_32(final int seed) { //NOSONAR
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_32_fixed(seed));
    }

    /**
     * Returns a hash function implementing the 32-bit Murmur3 algorithm (x86 variant) with
     * a seed value of zero. This is equivalent to calling {@code murmur3_32(0)}.
     * 
     * <p>This is a convenient method for when you don't need a specific seed value.
     * The zero seed is commonly used as a default.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashCode hash = Hashing.murmur3_32().hash("data".getBytes());
     * }</pre>
     *
     * @return a Murmur3 32-bit hash function with seed value 0
     * @see #murmur3_32(int)
     */
    public static HashFunction murmur3_32() { //NOSONAR
        return Hash_Holder.MURMUR3_32_FIXED;
    }

    /**
     * Returns a hash function implementing the 128-bit Murmur3 algorithm (x64 variant) with
     * the specified seed value. This produces a 128-bit hash value and is suitable for
     * applications requiring larger hash codes.
     *
     * <p>The implementation corresponds to the MurmurHash3_x64_128 function (Murmur3F) from
     * the original C++ implementation. This is the little-endian variant.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashFunction murmur128 = Hashing.murmur3_128(12345);
     * HashCode hash = murmur128.hash("large data set".getBytes());
     * }</pre>
     *
     * @param seed the seed value to initialize the hash function (different seeds produce different hash values for the same input)
     * @return a Murmur3 128-bit hash function initialized with the given seed
     */
    public static HashFunction murmur3_128(final int seed) { //NOSONAR
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128(seed));
    }

    /**
     * Returns a hash function implementing the 128-bit Murmur3 algorithm (x64 variant) with
     * a seed value of zero. This is equivalent to calling {@code murmur3_128(0)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashCode hash128 = Hashing.murmur3_128().hash("data".getBytes());
     * }</pre>
     *
     * @return a Murmur3 128-bit hash function with seed value 0
     * @see #murmur3_128(int)
     */
    public static HashFunction murmur3_128() { //NOSONAR
        return Hash_Holder.MURMUR3_128;
    }

    /**
     * Returns a hash function implementing the 64-bit SipHash-2-4 algorithm using a default
     * seed value of {@code k = 00 01 02 ...}. SipHash is a cryptographically strong
     * pseudo-random function optimized for short inputs.
     * 
     * <p>SipHash-2-4 refers to 2 compression rounds and 4 finalization rounds. It provides
     * a good balance between security and performance.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashCode sipHash = Hashing.sipHash24().hash("secure data".getBytes());
     * }</pre>
     *
     * @return a SipHash-2-4 hash function with default seed
     * @see #sipHash24(long, long)
     */
    public static HashFunction sipHash24() {
        return Hash_Holder.SIP_HASH_24;
    }

    /**
     * Returns a hash function implementing the 64-bit SipHash-2-4 algorithm using the
     * specified 128-bit key (provided as two 64-bit values). This allows for keyed hashing
     * which can be useful for hash tables with untrusted input.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long k0 = 0x0706050403020100L;
     * long k1 = 0x0f0e0d0c0b0a0908L;
     * HashFunction sipHash = Hashing.sipHash24(k0, k1);
     * HashCode hash = sipHash.hash("keyed data".getBytes());
     * }</pre>
     *
     * @param k0 the first 64 bits of the 128-bit key (low order bits)
     * @param k1 the second 64 bits of the 128-bit key (high order bits)
     * @return a SipHash-2-4 hash function initialized with the given key
     */
    public static HashFunction sipHash24(final long k0, final long k1) {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.sipHash24(k0, k1));
    }

    /**
     * Returns a hash function implementing the MD5 hash algorithm (128 hash bits).
     * MD5 is a widely used cryptographic hash function that produces a 128-bit hash value.
     * 
     * <p><b>Warning:</b> MD5 is deprecated for security purposes as it is vulnerable to
     * collision attacks. Use this method only for compatibility with legacy systems.
     * For new applications, prefer {@link #sha256()} for security or {@link #goodFastHash(int)}
     * for performance.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashCode md5Hash = Hashing.md5().hash("legacy data".getBytes());
     * }</pre>
     *
     * @return a hash function implementing MD5
     * @deprecated MD5 is neither fast nor secure. For security use {@link #sha256()} or higher.
     *             For speed use {@link #goodFastHash(int)}.
     */
    @Deprecated
    public static HashFunction md5() {
        return Hash_Holder.MD5;
    }

    /**
     * Returns a hash function implementing the SHA-1 algorithm (160 hash bits).
     * SHA-1 is a cryptographic hash function that produces a 160-bit hash value.
     * 
     * <p><b>Warning:</b> SHA-1 is deprecated for security purposes as practical collision
     * attacks have been demonstrated. Use this method only for compatibility with legacy systems.
     * For new applications, prefer {@link #sha256()} or higher.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashCode sha1Hash = Hashing.sha1().hash("legacy data".getBytes());
     * }</pre>
     *
     * @return a hash function implementing SHA-1
     * @deprecated SHA-1 is neither fast nor secure. For security use {@link #sha256()} or higher.
     *             For speed use {@link #goodFastHash(int)}.
     */
    @Deprecated
    public static HashFunction sha1() {
        return Hash_Holder.SHA_1;
    }

    /**
     * Returns a hash function implementing the SHA-256 algorithm (256 hash bits).
     * SHA-256 is part of the SHA-2 family and is widely used for cryptographic applications.
     * It provides a good balance between security and performance.
     * 
     * <p>This implementation delegates to the SHA-256 {@link MessageDigest} provided by
     * the Java security framework.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashCode sha256Hash = Hashing.sha256().hash("secure data".getBytes());
     * byte[] hashBytes = sha256Hash.asBytes(); // 32 bytes
     * }</pre>
     *
     * @return a hash function implementing SHA-256
     */
    public static HashFunction sha256() {
        return Hash_Holder.SHA_256;
    }

    /**
     * Returns a hash function implementing the SHA-384 algorithm (384 hash bits).
     * SHA-384 is part of the SHA-2 family and provides higher security than SHA-256
     * at the cost of increased computation time and hash size.
     * 
     * <p>This implementation delegates to the SHA-384 {@link MessageDigest} provided by
     * the Java security framework.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashCode sha384Hash = Hashing.sha384().hash("high security data".getBytes());
     * }</pre>
     *
     * @return a hash function implementing SHA-384
     */
    public static HashFunction sha384() {
        return Hash_Holder.SHA_384;
    }

    /**
     * Returns a hash function implementing the SHA-512 algorithm (512 hash bits).
     * SHA-512 is part of the SHA-2 family and provides the highest level of security
     * among the standard SHA-2 variants, producing a 512-bit hash value.
     * 
     * <p>This implementation delegates to the SHA-512 {@link MessageDigest} provided by
     * the Java security framework.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashCode sha512Hash = Hashing.sha512().hash("maximum security data".getBytes());
     * }</pre>
     *
     * @return a hash function implementing SHA-512
     */
    public static HashFunction sha512() {
        return Hash_Holder.SHA_512;
    }

    /**
     * Returns a hash function implementing the HMAC (Hash-based Message Authentication Code)
     * algorithm using MD5 as the underlying hash function (128 hash bits). HMAC provides
     * message authentication using a secret key.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Key secretKey = new SecretKeySpec("secret".getBytes(), "HmacMD5");
     * HashFunction hmac = Hashing.hmacMd5(secretKey);
     * HashCode mac = hmac.hash("message".getBytes());
     * }</pre>
     *
     * @param key the secret key for HMAC computation
     * @return a hash function implementing HMAC-MD5 with the given key
     * @throws IllegalArgumentException if the given key is inappropriate for initializing
     *                                  this MAC (e.g., wrong algorithm or {@code null} key)
     */
    public static HashFunction hmacMd5(final Key key) {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.hmacMd5(key));
    }

    /**
     * Returns a hash function implementing HMAC-MD5 using a {@link javax.crypto.spec.SecretKeySpec}
     * created from the given byte array. This is a convenience method that creates the
     * key specification internally.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] keyBytes = "secret key".getBytes(StandardCharsets.UTF_8);
     * HashFunction hmac = Hashing.hmacMd5(keyBytes);
     * HashCode mac = hmac.hash("message".getBytes());
     * }</pre>
     *
     * @param key the key material for the secret key as a byte array
     * @return a hash function implementing HMAC-MD5 with a key created from the given bytes
     * @throws IllegalArgumentException if {@code key} is empty or null
     */
    public static HashFunction hmacMd5(final byte[] key) {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.hmacMd5(key));
    }

    /**
     * Returns a hash function implementing the HMAC algorithm using SHA-1 as the underlying
     * hash function (160 hash bits). Provides better security than HMAC-MD5.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Key secretKey = new SecretKeySpec("secret".getBytes(), "HmacSHA1");
     * HashFunction hmac = Hashing.hmacSha1(secretKey);
     * HashCode mac = hmac.hash("message".getBytes());
     * }</pre>
     *
     * @param key the secret key for HMAC computation
     * @return a hash function implementing HMAC-SHA1 with the given key
     * @throws IllegalArgumentException if the given key is inappropriate for initializing this MAC
     */
    public static HashFunction hmacSha1(final Key key) {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.hmacSha1(key));
    }

    /**
     * Returns a hash function implementing HMAC-SHA1 using a {@link javax.crypto.spec.SecretKeySpec}
     * created from the given byte array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashFunction hmac = Hashing.hmacSha1("mySecretKey".getBytes());
     * HashCode mac = hmac.hash("authenticated message".getBytes());
     * }</pre>
     *
     * @param key the key material for the secret key as a byte array
     * @return a hash function implementing HMAC-SHA1 with a key created from the given bytes
     * @throws IllegalArgumentException if {@code key} is empty or null
     */
    public static HashFunction hmacSha1(final byte[] key) {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.hmacSha1(key));
    }

    /**
     * Returns a hash function implementing the HMAC algorithm using SHA-256 as the underlying
     * hash function (256 hash bits). This provides strong message authentication and is
     * recommended for new applications.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Key secretKey = new SecretKeySpec("secret".getBytes(), "HmacSHA256");
     * HashFunction hmac = Hashing.hmacSha256(secretKey);
     * HashCode mac = hmac.hash("secure message".getBytes());
     * }</pre>
     *
     * @param key the secret key for HMAC computation
     * @return a hash function implementing HMAC-SHA256 with the given key
     * @throws IllegalArgumentException if the given key is inappropriate for initializing this MAC
     */
    public static HashFunction hmacSha256(final Key key) {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.hmacSha256(key));
    }

    /**
     * Returns a hash function implementing HMAC-SHA256 using a {@link javax.crypto.spec.SecretKeySpec}
     * created from the given byte array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] key = "strongSecretKey".getBytes(StandardCharsets.UTF_8);
     * HashFunction hmac = Hashing.hmacSha256(key);
     * HashCode mac = hmac.hash("important data".getBytes());
     * }</pre>
     *
     * @param key the key material for the secret key as a byte array
     * @return a hash function implementing HMAC-SHA256 with a key created from the given bytes
     * @throws IllegalArgumentException if {@code key} is empty or null
     */
    public static HashFunction hmacSha256(final byte[] key) {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.hmacSha256(key));
    }

    /**
     * Returns a hash function implementing the HMAC algorithm using SHA-512 as the underlying
     * hash function (512 hash bits). This provides the highest level of security among the
     * HMAC variants offered.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Key secretKey = new SecretKeySpec("topsecret".getBytes(), "HmacSHA512");
     * HashFunction hmac = Hashing.hmacSha512(secretKey);
     * HashCode mac = hmac.hash("critical data".getBytes());
     * }</pre>
     *
     * @param key the secret key for HMAC computation
     * @return a hash function implementing HMAC-SHA512 with the given key
     * @throws IllegalArgumentException if the given key is inappropriate for initializing this MAC
     */
    public static HashFunction hmacSha512(final Key key) {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.hmacSha512(key));
    }

    /**
     * Returns a hash function implementing HMAC-SHA512 using a {@link javax.crypto.spec.SecretKeySpec}
     * created from the given byte array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] key = new byte[64]; // 512-bit key
     * new SecureRandom().nextBytes(key);
     * HashFunction hmac = Hashing.hmacSha512(key);
     * HashCode mac = hmac.hash("top secret data".getBytes());
     * }</pre>
     *
     * @param key the key material for the secret key as a byte array
     * @return a hash function implementing HMAC-SHA512 with a key created from the given bytes
     * @throws IllegalArgumentException if {@code key} is empty or null
     */
    public static HashFunction hmacSha512(final byte[] key) {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.hmacSha512(key));
    }

    /**
     * Returns a hash function implementing the CRC32C checksum algorithm (32 hash bits).
     * CRC32C uses the Castagnoli polynomial and is used in various storage systems and
     * network protocols for error detection.
     * 
     * <p>CRC32C generally has better error detection properties than the standard CRC32
     * and is optimized for modern CPU architectures with hardware support.
     * 
     * <p>As described by RFC 3720, Section 12.1.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashCode checksum = Hashing.crc32c().hash("data to check".getBytes());
     * int crc32cValue = checksum.asInt();
     * }</pre>
     *
     * @return a hash function implementing CRC32C
     */
    public static HashFunction crc32c() {
        return Hash_Holder.CRC_32_C;
    }

    /**
     * Returns a hash function implementing the standard CRC-32 checksum algorithm (32 hash bits).
     * This implementation delegates to the {@link CRC32} {@link Checksum} class.
     * 
     * <p>CRC32 is commonly used for error detection in network communications and file storage.
     * Note that CRC32 is not cryptographically secure and should not be used for security purposes.
     * 
     * <p>To get the {@code long} value equivalent to {@link Checksum#getValue()} for a
     * {@code HashCode} produced by this function, use {@link HashCode#padToLong()}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashCode crc = Hashing.crc32().hash("file contents".getBytes());
     * long crcValue = crc.padToLong();
     * }</pre>
     *
     * @return a hash function implementing CRC-32
     */
    public static HashFunction crc32() {
        return Hash_Holder.CRC_32;
    }

    /**
     * Returns a hash function implementing the Adler-32 checksum algorithm (32 hash bits).
     * This implementation delegates to the {@link Adler32} {@link Checksum} class.
     * 
     * <p>Adler-32 is a checksum algorithm that is faster than CRC32 but provides weaker
     * error detection. It is used in the zlib compression library.
     * 
     * <p>To get the {@code long} value equivalent to {@link Checksum#getValue()} for a
     * {@code HashCode} produced by this function, use {@link HashCode#padToLong()}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashCode adler = Hashing.adler32().hash("compressed data".getBytes());
     * long adlerValue = adler.padToLong();
     * }</pre>
     *
     * @return a hash function implementing Adler-32
     */
    public static HashFunction adler32() {
        return Hash_Holder.ADLER_32;
    }

    /**
     * Returns a hash function implementing FarmHash's Fingerprint64, an open-source
     * algorithm designed for generating persistent fingerprints of strings.
     * 
     * <p>FarmHash Fingerprint64 produces high-quality 64-bit hash values with fewer
     * collisions than many alternatives. While not cryptographically secure, it is
     * suitable for hash tables, checksums, and fingerprinting.
     * 
     * <p>The hash values generated are byte-wise identical to those created using the
     * C++ version of FarmHash. Note that this implementation uses unsigned integers
     * (see {@link com.google.common.primitives.UnsignedInts}), which should be considered
     * when comparing with signed integer implementations.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashCode fingerprint = Hashing.farmHashFingerprint64()
     *     .hash("unique identifier".getBytes());
     * }</pre>
     *
     * @return a hash function implementing FarmHash Fingerprint64
     */
    public static HashFunction farmHashFingerprint64() {
        return Hash_Holder.FARM_HASH_FINGERPRINT_64;
    }

    /**
     * Returns a hash function that computes its hash code by concatenating the hash codes
     * of two underlying hash functions. This is useful when you need a hash code of a
     * specific length that is not directly available.
     * 
     * <p>The resulting hash function will have a bit length equal to the sum of the bit
     * lengths of the input functions.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a 512-bit hash function by combining two SHA-256 functions
     * HashFunction hash512 = Hashing.concatenating(
     *     Hashing.sha256(), 
     *     Hashing.sha256()
     * );
     * }</pre>
     *
     * @param first the first hash function
     * @param second the second hash function
     * @return a hash function that concatenates the results of the two input functions
     * @see #concatenating(Iterable)
     */
    public static HashFunction concatenating(final HashFunction first, final HashFunction second) {
        return concatenating(N.asList(first, second));
    }

    /**
     * Returns a hash function that computes its hash code by concatenating the hash codes
     * of three underlying hash functions.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a 384-bit hash by combining three 128-bit functions
     * HashFunction hash384 = Hashing.concatenating(
     *     Hashing.murmur3_128(),
     *     Hashing.murmur3_128(42),
     *     Hashing.murmur3_128(123)
     * );
     * }</pre>
     *
     * @param first the first hash function
     * @param second the second hash function
     * @param third the third hash function
     * @return a hash function that concatenates the results of the three input functions
     * @see #concatenating(Iterable)
     */
    public static HashFunction concatenating(final HashFunction first, final HashFunction second, final HashFunction third) {
        return concatenating(N.asList(first, second, third));
    }

    /**
     * Returns a hash function that computes its hash code by concatenating the hash codes
     * of the underlying hash functions in the provided iterable. This can be useful for
     * generating hash codes of specific lengths or combining multiple hash algorithms.
     *
     * <p>The resulting hash function will have a bit length equal to the sum of the bit
     * lengths of all input functions. The hash codes are concatenated in the order the
     * functions appear in the iterable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a 1024-bit hash function
     * List<HashFunction> functions = Arrays.asList(
     *     Hashing.sha512(),  // 512 bits
     *     Hashing.sha512()   // 512 bits
     * );
     * HashFunction hash1024 = Hashing.concatenating(functions);
     * }</pre>
     *
     * @param hashFunctions an iterable of hash functions to concatenate (must not be empty)
     * @return a hash function that concatenates the results of all input functions
     * @throws IllegalArgumentException if {@code hashFunctions} is empty
     */
    public static HashFunction concatenating(final Iterable<HashFunction> hashFunctions) {
        final Iterator<HashFunction> iter = hashFunctions.iterator();
        final List<com.google.common.hash.HashFunction> gHashFunctionList = new ArrayList<>();

        while (iter.hasNext()) {
            gHashFunctionList.add(((GuavaHashFunction) iter.next()).gHashFunction);
        }

        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.concatenating(gHashFunctionList));
    }

    /**
     * Combines two hash codes in an ordered fashion to produce a new hash code with the
     * same bit length as the input hash codes. The combination is order-dependent, meaning
     * that {@code combineOrdered(a, b)} is different from {@code combineOrdered(b, a)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashCode hash1 = Hashing.sha256().hash("first".getBytes());
     * HashCode hash2 = Hashing.sha256().hash("second".getBytes());
     * HashCode combined = Hashing.combineOrdered(hash1, hash2);
     * }</pre>
     *
     * @param first the first hash code to combine
     * @param second the second hash code to combine
     * @return a hash code combining the input hash codes in order
     * @throws IllegalArgumentException if the hash codes have different bit lengths
     * @see #combineOrdered(Iterable)
     */
    public static HashCode combineOrdered(final HashCode first, final HashCode second) {
        return combineOrdered(Arrays.asList(first, second));
    }

    /**
     * Combines three hash codes in an ordered fashion to produce a new hash code with the
     * same bit length as the input hash codes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashCode hash1 = Hashing.md5().hash("part1".getBytes());
     * HashCode hash2 = Hashing.md5().hash("part2".getBytes());
     * HashCode hash3 = Hashing.md5().hash("part3".getBytes());
     * HashCode combined = Hashing.combineOrdered(hash1, hash2, hash3);
     * }</pre>
     *
     * @param first the first hash code to combine
     * @param second the second hash code to combine
     * @param third the third hash code to combine
     * @return a hash code combining the input hash codes in order
     * @throws IllegalArgumentException if the hash codes have different bit lengths
     * @see #combineOrdered(Iterable)
     */
    public static HashCode combineOrdered(final HashCode first, final HashCode second, final HashCode third) {
        return combineOrdered(Arrays.asList(first, second, third));
    }

    /**
     * Returns a hash code that combines the information from multiple hash codes in an
     * ordered fashion. The resulting hash code has the same bit length as each of the
     * input hash codes.
     * 
     * <p>This method is designed such that if two equal hash codes are produced by two
     * calls to this method, it is as likely as possible that each was computed from the
     * same input hash codes in the same order. This makes it suitable for scenarios where
     * the order of inputs matters.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<HashCode> hashes = new ArrayList<>();
     * for (String part : dataParts) {
     *     hashes.add(Hashing.sha256().hash(part.getBytes()));
     * }
     * HashCode combined = Hashing.combineOrdered(hashes);
     * }</pre>
     *
     * @param hashCodes an iterable of hash codes to combine
     * @return a hash code combining all input hash codes in order
     * @throws IllegalArgumentException if {@code hashCodes} is empty or if the hash codes
     *                                  do not all have the same bit length
     */
    public static HashCode combineOrdered(final Iterable<HashCode> hashCodes) {
        return com.google.common.hash.Hashing.combineOrdered(hashCodes);
    }

    /**
     * Combines two hash codes in an unordered fashion to produce a new hash code with the
     * same bit length as the input hash codes. The combination is order-independent, meaning
     * that {@code combineUnordered(a, b)} equals {@code combineUnordered(b, a)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashCode hash1 = Hashing.sha256().hash("item1".getBytes());
     * HashCode hash2 = Hashing.sha256().hash("item2".getBytes());
     * HashCode combined = Hashing.combineUnordered(hash1, hash2);
     * }</pre>
     *
     * @param first the first hash code to combine
     * @param second the second hash code to combine
     * @return a hash code combining the input hash codes without regard to order
     * @throws IllegalArgumentException if the hash codes have different bit lengths
     * @see #combineUnordered(Iterable)
     */
    public static HashCode combineUnordered(final HashCode first, final HashCode second) {
        return combineUnordered(Arrays.asList(first, second));
    }

    /**
     * Combines three hash codes in an unordered fashion to produce a new hash code with the
     * same bit length as the input hash codes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashCode hash1 = Hashing.murmur3_128().hash("a".getBytes());
     * HashCode hash2 = Hashing.murmur3_128().hash("b".getBytes());
     * HashCode hash3 = Hashing.murmur3_128().hash("c".getBytes());
     * HashCode combined = Hashing.combineUnordered(hash1, hash2, hash3);
     * }</pre>
     *
     * @param first the first hash code to combine
     * @param second the second hash code to combine
     * @param third the third hash code to combine
     * @return a hash code combining the input hash codes without regard to order
     * @throws IllegalArgumentException if the hash codes have different bit lengths
     * @see #combineUnordered(Iterable)
     */
    public static HashCode combineUnordered(final HashCode first, final HashCode second, final HashCode third) {
        return combineUnordered(Arrays.asList(first, second, third));
    }

    /**
     * Returns a hash code that combines the information from multiple hash codes in an
     * unordered fashion. The resulting hash code has the same bit length as each of the
     * input hash codes.
     * 
     * <p>This method is designed such that if two equal hash codes are produced by two
     * calls to this method, it is as likely as possible that each was computed from the
     * same set of input hash codes, regardless of order. This makes it suitable for
     * combining hash codes of set elements or other unordered collections.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> items = new HashSet<>(Arrays.asList("x", "y", "z"));
     * List<HashCode> hashes = new ArrayList<>();
     * for (String item : items) {
     *     hashes.add(Hashing.sha256().hash(item.getBytes()));
     * }
     * HashCode setHash = Hashing.combineUnordered(hashes);
     * }</pre>
     *
     * @param hashCodes an iterable of hash codes to combine
     * @return a hash code combining all input hash codes without regard to order
     * @throws IllegalArgumentException if {@code hashCodes} is empty or if the hash codes
     *                                  do not all have the same bit length
     */
    public static HashCode combineUnordered(final Iterable<HashCode> hashCodes) {
        return com.google.common.hash.Hashing.combineUnordered(hashCodes);
    }

    /**
     * Assigns a "bucket" index in the range {@code [0, buckets)} to the given hash code
     * using consistent hashing. This method minimizes the need for remapping as the number
     * of buckets grows.
     *
     * <p>Specifically, {@code consistentHash(h, n)} equals:
     * <ul>
     *   <li>{@code n - 1}, with approximate probability {@code 1/n}</li>
     *   <li>{@code consistentHash(h, n - 1)}, otherwise (probability {@code 1 - 1/n})</li>
     * </ul>
     *
     * <p>This property makes consistent hashing ideal for distributed systems where you want
     * to minimize redistribution when adding or removing nodes. When the number of buckets
     * increases from n-1 to n, only approximately 1/n of the items need to be remapped.
     *
     * <p><b>Important limitation:</b> This algorithm assumes that buckets are added/removed
     * from the end. It's not suitable for scenarios where arbitrary buckets might be removed
     * (e.g., specific servers going offline in a cluster).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HashCode itemHash = Hashing.sha256().hash("user123".getBytes());
     * int serverCount = 10;
     * int assignedServer = Hashing.consistentHash(itemHash, serverCount);
     * // assignedServer will be in range [0, 10)
     * }</pre>
     *
     * @param hashCode the hash code to assign to a bucket
     * @param buckets the number of buckets available (must be positive)
     * @return a bucket index in the range {@code [0, buckets)}
     * @throws IllegalArgumentException if {@code buckets} is not positive
     * @see <a href="http://en.wikipedia.org/wiki/Consistent_hashing">Consistent hashing on Wikipedia</a>
     */
    public static int consistentHash(final HashCode hashCode, final int buckets) {
        return com.google.common.hash.Hashing.consistentHash(hashCode, buckets);
    }

    /**
     * Assigns a "bucket" index in the range {@code [0, buckets)} to the given long value
     * using consistent hashing. This is a convenience method equivalent to
     * {@code consistentHash(HashCode.fromLong(input), buckets)}.
     *
     * <p>This method provides the same consistent hashing properties as
     * {@link #consistentHash(HashCode, int)}, minimizing redistribution when the number
     * of buckets changes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long userId = 12345L;
     * int shardCount = 100;
     * int assignedShard = Hashing.consistentHash(userId, shardCount);
     * // assignedShard will consistently map userId to a shard in [0, 100)
     * }</pre>
     *
     * @param input the input value to assign to a bucket
     * @param buckets the number of buckets available (must be positive)
     * @return a bucket index in the range {@code [0, buckets)}
     * @throws IllegalArgumentException if {@code buckets} is not positive
     * @see #consistentHash(HashCode, int)
     */
    public static int consistentHash(final long input, final int buckets) {
        return com.google.common.hash.Hashing.consistentHash(input, buckets);
    }

    private static final class Hash_Holder { //NOSONAR
        static final HashFunction MURMUR3_32_FIXED = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_32_fixed());
        static final HashFunction MURMUR3_128 = GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128());

        static final HashFunction SIP_HASH_24 = GuavaHashFunction.wrap(com.google.common.hash.Hashing.sipHash24());

        @SuppressWarnings("deprecation")
        static final HashFunction MD5 = GuavaHashFunction.wrap(com.google.common.hash.Hashing.md5());

        @SuppressWarnings("deprecation")
        static final HashFunction SHA_1 = GuavaHashFunction.wrap(com.google.common.hash.Hashing.sha1());
        static final HashFunction SHA_256 = GuavaHashFunction.wrap(com.google.common.hash.Hashing.sha256());
        static final HashFunction SHA_384 = GuavaHashFunction.wrap(com.google.common.hash.Hashing.sha384());
        static final HashFunction SHA_512 = GuavaHashFunction.wrap(com.google.common.hash.Hashing.sha512());

        static final HashFunction CRC_32_C = GuavaHashFunction.wrap(com.google.common.hash.Hashing.crc32c());
        static final HashFunction CRC_32 = GuavaHashFunction.wrap(com.google.common.hash.Hashing.crc32());

        static final HashFunction ADLER_32 = GuavaHashFunction.wrap(com.google.common.hash.Hashing.adler32());

        static final HashFunction FARM_HASH_FINGERPRINT_64 = GuavaHashFunction.wrap(com.google.common.hash.Hashing.farmHashFingerprint64());

        private Hash_Holder() {
            // singleton for utility class.
        }
    }
}
