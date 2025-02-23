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
 * Note: It's copied from Google Guava under Apache License 2.0 and modified.
 *
 * Static methods to obtain {@link HashFunction} instances, and other static hashing-related
 * utilities.
 *
 * <p>A comparison of the various hash functions can be found
 * <a href="http://goo.gl/jS7HH">here</a>.
 *
 * @author Kevin Bourrillion
 * @author Dimitris Andreou
 * @author Kurt Alfred Kluever
 */
public final class Hashing {

    private Hashing() {
        // singleton for utility class.
    }

    /**
     * Returns a general-purpose, <b>temporary-use</b>, non-cryptographic hash function. The algorithm
     * the returned function implements is unspecified and subject to change without notice.
     *
     * <p><b>Warning:</b> a new random seed for these functions is chosen each time the {@code
     * Hashing} class is loaded. <b>Do not use this method</b> if hash codes may escape the current
     * process in any way, for example being sent over RPC, or saved to disk.
     *
     * <p>Repeated calls to this method on the same loaded {@code Hashing} class, using the same value
     * for {@code minimumBits}, will return identically-behaving {@link HashFunction} instances.
     *
     * @param minimumBits a positive integer (can be arbitrarily large)
     * @return a hash function, described above, that produces hash codes of length {@code
     *     minimumBits} or greater
     */
    public static HashFunction goodFastHash(final int minimumBits) {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.goodFastHash(minimumBits));
    }

    /**
     * Returns a hash function implementing the <a
     * href="https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp">32-bit murmur3
     * algorithm, x86 variant</a> (little-endian variant), using the given seed value.
     *
     * <p>The exact C++ equivalent is the MurmurHash3_x86_32 function (Murmur3A).
     *
     * <p>This method is called {@code murmur3_32_fixed} because it fixes a bug in the {@code
     * HashFunction} returned by the original {@code murmur3_32} method.
     *
     * @param seed
     * @return
     */
    public static HashFunction murmur3_32(final int seed) { //NOSONAR
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_32_fixed(seed));
    }

    /**
     * Returns a hash function implementing the <a
     * href="https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp">32-bit murmur3
     * algorithm, x86 variant</a> (little-endian variant), using a seed value of zero.
     *
     * <p>The exact C++ equivalent is the MurmurHash3_x86_32 function (Murmur3A).
     *
     * <p>This method is called {@code murmur3_32_fixed} because it fixes a bug in the {@code
     * HashFunction} returned by the original {@code murmur3_32} method.
     *
     * @return
     */
    public static HashFunction murmur3_32() { //NOSONAR
        return Hash_Holder.MURMUR3_32_FIXED;
    }

    /**
     * Returns a hash function implementing the
     * <a href="http://smhasher.googlecode.com/svn/trunk/MurmurHash3.cpp">128-bit murmur3 algorithm,
     * x64 variant</a> (little-endian variant), using the given seed value.
     *
     * <p>The exact C++ equivalent is the MurmurHash3_x64_128 function (Murmur3F).
     *
     * @param seed
     * @return
     */
    public static HashFunction murmur3_128(final int seed) { //NOSONAR
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.murmur3_128(seed));
    }

    /**
     * Returns a hash function implementing the
     * <a href="http://smhasher.googlecode.com/svn/trunk/MurmurHash3.cpp">128-bit murmur3 algorithm,
     * x64 variant</a> (little-endian variant), using a seed value of zero.
     *
     * <p>The exact C++ equivalent is the MurmurHash3_x64_128 function (Murmur3F).
     *
     * @return
     */
    public static HashFunction murmur3_128() { //NOSONAR
        return Hash_Holder.MURMUR3_128;
    }

    /**
     * Returns a hash function implementing the <a href="https://131002.net/siphash/">64-bit
     * SipHash-2-4 algorithm</a> using a seed value of {@code k = 00 01 02 ...}.
     *
     * @return
     */
    public static HashFunction sipHash24() {
        return Hash_Holder.SIP_HASH_24;
    }

    /**
     * Returns a hash function implementing the <a href="https://131002.net/siphash/">64-bit
     * SipHash-2-4 algorithm</a> using the given seed.
     *
     * @param k0
     * @param k1
     * @return
     */
    public static HashFunction sipHash24(final long k0, final long k1) {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.sipHash24(k0, k1));
    }

    /**
     * Returns a hash function implementing the MD5 hash algorithm (128 hash bits).
     *
     * @return
     * @deprecated If you must interoperate with a system that requires MD5, then use this method,
     *     despite its deprecation. But if you can choose your hash function, avoid MD5, which is
     *     neither fast nor secure. As of January 2017, we suggest:
     *     <ul>
     *       <li>For security:
     *           {@link Hashing#sha256} or a higher-level API.
     *       <li>For speed: {@link Hashing#goodFastHash}, though see its docs for caveats.
     *     </ul>
     */
    @Deprecated
    public static HashFunction md5() {
        return Hash_Holder.MD5;
    }

    /**
     * Returns a hash function implementing the SHA-1 algorithm (160 hash bits).
     *
     * @return
     * @deprecated If you must interoperate with a system that requires SHA-1, then use this method,
     *     despite its deprecation. But if you can choose your hash function, avoid SHA-1, which is
     *     neither fast nor secure. As of January 2017, we suggest:
     *     <ul>
     *       <li>For security:
     *           {@link Hashing#sha256} or a higher-level API.
     *       <li>For speed: {@link Hashing#goodFastHash}, though see its docs for caveats.
     *     </ul>
     */
    @Deprecated
    public static HashFunction sha1() {
        return Hash_Holder.SHA_1;
    }

    /**
     * Returns a hash function implementing the SHA-256 algorithm (256 hash bits) by delegating to the
     * SHA-256 {@link MessageDigest}.
     *
     * @return
     */
    public static HashFunction sha256() {
        return Hash_Holder.SHA_256;
    }

    /**
     * Returns a hash function implementing the SHA-384 algorithm (384 hash bits) by delegating to the
     * SHA-384 {@link MessageDigest}.
     *
     * @return
     */
    public static HashFunction sha384() {
        return Hash_Holder.SHA_384;
    }

    /**
     * Returns a hash function implementing the SHA-512 algorithm (512 hash bits) by delegating to the
     * SHA-512 {@link MessageDigest}.
     *
     * @return
     */
    public static HashFunction sha512() {
        return Hash_Holder.SHA_512;
    }

    /**
     * Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
     * MD5 (128 hash bits) hash function and the given secret key.
     *
     * @param key the secret key
     * @return
     * @throws IllegalArgumentException if the given key is inappropriate for initializing this MAC
     */
    public static HashFunction hmacMd5(final Key key) {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.hmacMd5(key));
    }

    /**
     * Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
     * MD5 (128 hash bits) hash function and a {@link SecretSpecKey} created from the given byte array
     * and the MD5 algorithm.
     *
     * @param key the key material of the secret key
     * @return
     */
    public static HashFunction hmacMd5(final byte[] key) {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.hmacMd5(key));
    }

    /**
     * Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
     * SHA-1 (160 hash bits) hash function and the given secret key.
     *
     * @param key the secret key
     * @return
     * @throws IllegalArgumentException if the given key is inappropriate for initializing this MAC
     */
    public static HashFunction hmacSha1(final Key key) {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.hmacSha1(key));
    }

    /**
     * Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
     * SHA-1 (160 hash bits) hash function and a {@link SecretSpecKey} created from the given byte
     * array and the SHA-1 algorithm.
     *
     * @param key the key material of the secret key
     * @return
     */
    public static HashFunction hmacSha1(final byte[] key) {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.hmacSha1(key));
    }

    /**
     * Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
     * SHA-256 (256 hash bits) hash function and the given secret key.
     *
     * @param key the secret key
     * @return
     * @throws IllegalArgumentException if the given key is inappropriate for initializing this MAC
     */
    public static HashFunction hmacSha256(final Key key) {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.hmacSha256(key));
    }

    /**
     * Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
     * SHA-256 (256 hash bits) hash function and a {@link SecretSpecKey} created from the given byte
     * array and the SHA-256 algorithm.
     *
     * @param key the key material of the secret key
     * @return
     */
    public static HashFunction hmacSha256(final byte[] key) {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.hmacSha256(key));
    }

    /**
     * Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
     * SHA-512 (512 hash bits) hash function and the given secret key.
     *
     * @param key the secret key
     * @return
     * @throws IllegalArgumentException if the given key is inappropriate for initializing this MAC
     */
    public static HashFunction hmacSha512(final Key key) {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.hmacSha512(key));
    }

    /**
     * Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
     * SHA-512 (512 hash bits) hash function and a {@link SecretSpecKey} created from the given byte
     * array and the SHA-512 algorithm.
     *
     * @param key the key material of the secret key
     * @return
     */
    public static HashFunction hmacSha512(final byte[] key) {
        return GuavaHashFunction.wrap(com.google.common.hash.Hashing.hmacSha512(key));
    }

    /**
     * Returns a hash function implementing the CRC32C checksum algorithm (32 hash bits) as described
     * by RFC 3720, Section 12.1.
     *
     * @return
     */
    public static HashFunction crc32c() {
        return Hash_Holder.CRC_32_C;
    }

    /**
     * Returns a hash function implementing the CRC-32 checksum algorithm (32 hash bits) by delegating
     * to the {@link CRC32} {@link Checksum}.
     *
     * <p>To get the {@code long} value equivalent to {@link Checksum#getValue()} for a
     * {@code HashCode} produced by this function, use {@link HashCode#padToLong()}.
     *
     * @return
     */
    public static HashFunction crc32() {
        return Hash_Holder.CRC_32;
    }

    /**
     * Returns a hash function implementing the Adler-32 checksum algorithm (32 hash bits) by
     * delegating to the {@link Adler32} {@link Checksum}.
     *
     * <p>To get the {@code long} value equivalent to {@link Checksum#getValue()} for a
     * {@code HashCode} produced by this function, use {@link HashCode#padToLong()}.
     *
     * @return
     */
    public static HashFunction adler32() {
        return Hash_Holder.ADLER_32;
    }

    /**
     * Returns a hash function implementing FarmHash's Fingerprint64, an open-source algorithm.
     *
     * <p>This is designed for generating persistent fingerprints of strings. It isn't
     * cryptographically secure, but it produces a high-quality hash with fewer collisions than some
     * alternatives we've used in the past. FarmHashFingerprints generated using this are byte-wise
     * identical to those created using the C++ version, but note that this uses unsigned integers
     * (see {@link com.google.common.primitives.UnsignedInts}). Comparisons between the two should
     * take this into account.
     *
     * @return
     */
    public static HashFunction farmHashFingerprint64() {
        return Hash_Holder.FARM_HASH_FINGERPRINT_64;
    }

    /**
     *
     * @param first
     * @param second
     * @return
     * @see #concatenating(Iterable)
     */
    public static HashFunction concatenating(final HashFunction first, final HashFunction second) {
        return concatenating(N.asList(first, second));
    }

    /**
     *
     * @param first
     * @param second
     * @param third
     * @return
     * @see #concatenating(Iterable)
     */
    public static HashFunction concatenating(final HashFunction first, final HashFunction second, final HashFunction third) {
        return concatenating(N.asList(first, second, third));
    }

    /**
     * Returns a hash function which computes its hash code by concatenating the hash codes of the
     * underlying hash functions together. This can be useful if you need to generate hash codes of a
     * specific length.
     *
     * <p>For example, if you need 1024-bit hash codes, you could join two {@link Hashing#sha512} hash
     * functions together: {@code Hashing.concatenating(Hashing.sha512(), Hashing.sha512())}.
     *
     * @param hashFunctions
     * @return
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
     *
     * @param first
     * @param second
     * @return
     * @see #combineOrdered(Iterable)
     */
    public static HashCode combineOrdered(final HashCode first, final HashCode second) {
        return combineOrdered(Arrays.asList(first, second));
    }

    /**
     *
     * @param first
     * @param second
     * @param third
     * @return
     * @see #combineOrdered(Iterable)
     */
    public static HashCode combineOrdered(final HashCode first, final HashCode second, final HashCode third) {
        return combineOrdered(Arrays.asList(first, second, third));
    }

    /**
     * Returns a hash code, having the same bit length as each of the input hash codes, that combines
     * the information of these hash codes in an ordered fashion. That is, whenever two equal hash
     * codes are produced by two calls to this method, it is <i>as likely as possible</i> that each
     * was computed from the <i>same</i> input hash codes in the <i>same</i> order.
     *
     * @param hashCodes
     * @return
     * @throws IllegalArgumentException if {@code hashCodes} is empty, or the hash codes do not all
     *     have the same bit length
     */
    public static HashCode combineOrdered(final Iterable<HashCode> hashCodes) {
        return com.google.common.hash.Hashing.combineOrdered(hashCodes);
    }

    /**
     *
     * @param first
     * @param second
     * @return
     * @see #combineUnordered(Iterable)
     */
    public static HashCode combineUnordered(final HashCode first, final HashCode second) {
        return combineUnordered(Arrays.asList(first, second));
    }

    /**
     *
     * @param first
     * @param second
     * @param third
     * @return
     * @see #combineUnordered(Iterable)
     */
    public static HashCode combineUnordered(final HashCode first, final HashCode second, final HashCode third) {
        return combineUnordered(Arrays.asList(first, second, third));
    }

    /**
     * Returns a hash code, having the same bit length as each of the input hash codes, that combines
     * the information of these hash codes in an unordered fashion. That is, whenever two equal hash
     * codes are produced by two calls to this method, it is <i>as likely as possible</i> that each
     * was computed from the <i>same</i> input hash codes in <i>some</i> order.
     *
     * @param hashCodes
     * @return
     * @throws IllegalArgumentException if {@code hashCodes} is empty, or the hash codes do not all
     *     have the same bit length
     */
    public static HashCode combineUnordered(final Iterable<HashCode> hashCodes) {
        return com.google.common.hash.Hashing.combineUnordered(hashCodes);
    }

    /**
     * Assigns to {@code hashCode} a "bucket" in the range {@code [0, buckets)}, in a uniform manner
     * that minimizes the need for remapping as {@code buckets} grows. That is, {@code
     * consistentHash(h, n)} equals:
     *
     * <ul>
     * <li>{@code n - 1}, with approximate probability {@code 1/n}
     * <li>{@code consistentHash(h, n - 1)}, otherwise (probability {@code 1 - 1/n})
     * </ul>
     *
     * <p>This method is suitable for the common use case of dividing work among buckets that meet the
     * following conditions:
     *
     * <ul>
     * <li>You want to assign the same fraction of inputs to each bucket.
     * <li>When you reduce the number of buckets, you can accept that the most recently added buckets
     * will be removed first. More concretely, if you are dividing traffic among tasks, you can
     * decrease the number of tasks from 15 and 10, killing off the final 5 tasks, and {@code
     * consistentHash} will handle it. If, however, you are dividing traffic among servers {@code
     * alpha}, {@code bravo}, and {@code charlie} and you occasionally need to take each of the
     * servers offline, {@code consistentHash} will be a poor fit: It provides no way for you to
     * specify which of the three buckets is disappearing. Thus, if your buckets change from {@code
     * [alpha, bravo, charlie]} to {@code [bravo, charlie]}, it will assign all the old {@code alpha}
     * traffic to {@code bravo} and all the old {@code bravo} traffic to {@code charlie}, rather than
     * letting {@code bravo} keep its traffic.
     * </ul>
     *
     *
     * <p>See the <a href="http://en.wikipedia.org/wiki/Consistent_hashing">Wikipedia article on
     * consistent hashing</a> for more information.
     *
     * @param hashCode
     * @param buckets
     * @return
     */
    public static int consistentHash(final HashCode hashCode, final int buckets) {
        return com.google.common.hash.Hashing.consistentHash(hashCode, buckets);
    }

    /**
     * Assigns to {@code input} a "bucket" in the range {@code [0, buckets)}, in a uniform manner that
     * minimizes the need for remapping as {@code buckets} grows. That is, {@code consistentHash(h,
     * n)} equals:
     *
     * <ul>
     * <li>{@code n - 1}, with approximate probability {@code 1/n}
     * <li>{@code consistentHash(h, n - 1)}, otherwise (probability {@code 1 - 1/n})
     * </ul>
     *
     * <p>This method is suitable for the common use case of dividing work among buckets that meet the
     * following conditions:
     *
     * <ul>
     * <li>You want to assign the same fraction of inputs to each bucket.
     * <li>When you reduce the number of buckets, you can accept that the most recently added buckets
     * will be removed first. More concretely, if you are dividing traffic among tasks, you can
     * decrease the number of tasks from 15 and 10, killing off the final 5 tasks, and {@code
     * consistentHash} will handle it. If, however, you are dividing traffic among servers {@code
     * alpha}, {@code bravo}, and {@code charlie} and you occasionally need to take each of the
     * servers offline, {@code consistentHash} will be a poor fit: It provides no way for you to
     * specify which of the three buckets is disappearing. Thus, if your buckets change from {@code
     * [alpha, bravo, charlie]} to {@code [bravo, charlie]}, it will assign all the old {@code alpha}
     * traffic to {@code bravo} and all the old {@code bravo} traffic to {@code charlie}, rather than
     * letting {@code bravo} keep its traffic.
     * </ul>
     *
     *
     * <p>See the <a href="http://en.wikipedia.org/wiki/Consistent_hashing">Wikipedia article on
     * consistent hashing</a> for more information.
     *
     * @param input
     * @param buckets
     * @return
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
