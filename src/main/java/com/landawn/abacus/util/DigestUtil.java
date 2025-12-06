/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.landawn.abacus.annotation.MayReturnNull;

/**
 * Utility class for generating cryptographic message digests (hashes) using various algorithms.
 * This class provides convenient static methods for computing digests of byte arrays, files, 
 * input streams, and strings using standard algorithms like MD5, SHA-1, SHA-256, etc.
 * 
 * <p>This class is immutable and thread-safe. However, the MessageDigest instances it creates 
 * generally won't be thread-safe.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Generate MD5 hash of a string
 * String hash = DigestUtil.md5Hex("Hello World");
 * 
 * // Generate SHA-256 hash of a file
 * byte[] digest = DigestUtil.sha256(new File("document.pdf"));
 * }</pre>
 *
 * <p><b>Attribution:</b>
 * This class includes code adapted from Apache Commons Codec under the Apache License 2.0. 
 * Methods from these libraries may have been modified for consistency, performance optimization, and null-safety enhancement.
 * 
 * @see MessageDigestAlgorithms
 */
public class DigestUtil {

    static final int BUFFER_SIZE = 1024;

    private DigestUtil() {
        // Singleton for utility class
    }

    /**
     * Computes the digest of the given byte array using the specified MessageDigest.
     * This is a convenience method that directly computes and returns the final digest hash.
     * The MessageDigest is reset after this operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest md = DigestUtil.getMd5Digest();
     * byte[] hash = DigestUtil.digest(md, "test".getBytes());
     * // hash contains the 16-byte MD5 digest
     * }</pre>
     *
     * @param messageDigest The MessageDigest algorithm to use (must not be {@code null})
     * @param data The byte array to digest (must not be {@code null})
     * @return The computed digest as a byte array, length depends on the algorithm used
     */
    public static byte[] digest(final MessageDigest messageDigest, final byte[] data) {
        return messageDigest.digest(data);
    }

    /**
     * Reads through a ByteBuffer and computes the digest for the data.
     * The ByteBuffer's position will be advanced to its limit after this operation.
     * All bytes from the current position to the limit are included in the digest calculation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteBuffer buffer = ByteBuffer.wrap("Hello".getBytes());
     * byte[] hash = DigestUtil.digest(DigestUtil.getSha1Digest(), buffer);
     * // buffer.position() is now equal to buffer.limit()
     * }</pre>
     *
     * @param messageDigest The MessageDigest algorithm to use (must not be {@code null})
     * @param data The ByteBuffer containing data to digest (must not be {@code null})
     * @return The computed digest as a byte array, length depends on the algorithm used
     *
     */
    public static byte[] digest(final MessageDigest messageDigest, final ByteBuffer data) {
        messageDigest.update(data);
        return messageDigest.digest();
    }

    /**
     * Reads through a File and computes the digest for its contents.
     * The file is read using a buffered input stream for efficiency (1024-byte buffer).
     * The entire file content is processed regardless of size.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("document.txt");
     * byte[] hash = DigestUtil.digest(DigestUtil.getSha256Digest(), file);
     * // Produces a 32-byte SHA-256 hash of the file contents
     * }</pre>
     *
     * @param messageDigest The MessageDigest algorithm to use (must not be {@code null})
     * @param data The File to read and digest (must exist and be readable)
     * @return The computed digest as a byte array, length depends on the algorithm used
     * @throws IOException If an I/O error occurs while reading the file
     * @throws java.io.FileNotFoundException if the file does not exist or cannot be opened
     *
     */
    public static byte[] digest(final MessageDigest messageDigest, final File data) throws IOException {
        return updateDigest(messageDigest, data).digest();
    }

    /**
     * Reads through an InputStream and computes the digest for the data.
     * The stream is read completely until EOF but is NOT closed by this method.
     * It is the caller's responsibility to close the stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("data.bin")) {
     *     byte[] hash = DigestUtil.digest(DigestUtil.getMd5Digest(), is);
     *     // Stream is still open here, closed by try-with-resources
     * }
     * }</pre>
     *
     * @param messageDigest The MessageDigest algorithm to use (must not be {@code null})
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return The computed digest as a byte array, length depends on the algorithm used
     * @throws IOException If an I/O error occurs while reading from the stream
     *
     */
    public static byte[] digest(final MessageDigest messageDigest, final InputStream data) throws IOException {
        return updateDigest(messageDigest, data).digest();
    }

    /**
     * Reads through a file at the specified Path and computes the digest for the data.
     * The file is opened according to the specified OpenOptions (if no options are provided,
     * the file is opened with default read options). The file stream is automatically closed
     * after reading.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Path path = Paths.get("data.txt");
     * byte[] hash = DigestUtil.digest(DigestUtil.getSha512Digest(), path, StandardOpenOption.READ);
     * // Or simply: DigestUtil.digest(DigestUtil.getSha512Digest(), path)
     * }</pre>
     *
     * @param messageDigest The MessageDigest algorithm to use (must not be {@code null})
     * @param data The Path to the file to digest (must not be {@code null})
     * @param options Optional open options for the file (e.g., StandardOpenOption.READ).
     *                If not specified, default options are used.
     * @return The computed digest as a byte array, length depends on the algorithm used
     * @throws IOException If an I/O error occurs while reading the file
     *
     */
    public static byte[] digest(final MessageDigest messageDigest, final Path data, final OpenOption... options) throws IOException {
        return updateDigest(messageDigest, data, options).digest();
    }

    /**
     * Reads through a RandomAccessFile using non-blocking I/O (NIO) and computes the digest.
     * This method uses FileChannel for efficient reading with a 1024-byte buffer.
     * The RandomAccessFile is read from its current position to the end.
     * The file position is modified during reading.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (RandomAccessFile raf = new RandomAccessFile("large.dat", "r")) {
     *     byte[] hash = DigestUtil.digest(DigestUtil.getSha256Digest(), raf);
     * }
     * }</pre>
     *
     * @param messageDigest The MessageDigest algorithm to use (must not be {@code null})
     * @param data The RandomAccessFile to read and digest (must not be {@code null})
     * @return The computed digest as a byte array, length depends on the algorithm used
     * @throws IOException If an I/O error occurs while reading the file
     *
     */
    public static byte[] digest(final MessageDigest messageDigest, final RandomAccessFile data) throws IOException {
        return updateDigest(messageDigest, data).digest();
    }

    /**
     * Returns a MessageDigest instance for the specified algorithm.
     * This method wraps the checked NoSuchAlgorithmException in an unchecked IllegalArgumentException,
     * making it more convenient to use when you know the algorithm is available.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest md = DigestUtil.getDigest("SHA-512");
     * byte[] hash = md.digest("data".getBytes());
     * }</pre>
     *
     * @param algorithm The name of the algorithm (e.g., "MD5", "SHA-256", "SHA-512").
     *                  See Java Cryptography Architecture documentation for standard algorithm names.
     *                  Must not be {@code null}.
     * @return A new MessageDigest instance for the specified algorithm
     * @throws IllegalArgumentException If the algorithm is not available in the current JVM
     *
     * @see MessageDigest#getInstance(String)
     * @see MessageDigestAlgorithms
     */
    public static MessageDigest getDigest(final String algorithm) {
        try {
            return getMessageDigest(algorithm);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns a MessageDigest instance for the specified algorithm, or a default digest if
     * the algorithm is not available. This method provides a safe fallback mechanism and
     * never throws an exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest fallback = DigestUtil.getSha256Digest();
     * MessageDigest md = DigestUtil.getDigest("SHA3-256", fallback);
     * // Returns SHA3-256 if available (Java 9+), otherwise SHA-256
     * }</pre>
     *
     * @param algorithm The name of the algorithm to attempt (e.g., "SHA3-512", "BLAKE2b").
     *                  Can be {@code null}, in which case the default is returned.
     * @param defaultMessageDigest The MessageDigest to return if the algorithm is not available.
     *                            Can be {@code null}.
     * @return A MessageDigest instance for the algorithm if available, otherwise returns
     *         defaultMessageDigest (which may be {@code null})
     *
     */
    @MayReturnNull
    public static MessageDigest getDigest(final String algorithm, final MessageDigest defaultMessageDigest) {
        try {
            return getMessageDigest(algorithm);
        } catch (final Exception e) {
            return defaultMessageDigest;
        }
    }

    /**
     * Internal method to get a MessageDigest instance.
     *
     * @param algorithm the name of the algorithm
     * @return A MessageDigest instance
     * @throws NoSuchAlgorithmException if the algorithm is not available
     */
    private static MessageDigest getMessageDigest(final String algorithm) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(algorithm);
    }

    /**
     * Returns an MD2 MessageDigest instance.
     *
     * <p><strong>WARNING:</strong> MD2 is an obsolete and cryptographically broken algorithm.
     * It should NOT be used for any security purposes or new applications.
     * This method is provided only for compatibility with legacy systems.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest md2 = DigestUtil.getMd2Digest();
     * byte[] hash = md2.digest("legacy data".getBytes());
     * }</pre>
     *
     * @return A new MD2 MessageDigest instance
     * @throws IllegalArgumentException If MD2 is not available (may occur in modern JVMs
     *                                  where MD2 support has been removed)
     *
     * @see MessageDigestAlgorithms#MD2
     */
    public static MessageDigest getMd2Digest() {
        return getDigest(MessageDigestAlgorithms.MD2);
    }

    /**
     * Returns an MD5 MessageDigest instance.
     *
     * <p><strong>WARNING:</strong> MD5 is cryptographically broken and should NOT be used
     * for security purposes such as password hashing, digital signatures, or certificates.
     * However, it may still be acceptable for non-security purposes like checksums,
     * file integrity verification, or hash-based data structures where collision resistance
     * is not critical.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest md5 = DigestUtil.getMd5Digest();
     * byte[] checksum = md5.digest(fileData);
     * }</pre>
     *
     * @return A new MD5 MessageDigest instance
     * @throws IllegalArgumentException If MD5 is not available (should not happen with standard JRE)
     *
     * @see MessageDigestAlgorithms#MD5
     */
    public static MessageDigest getMd5Digest() {
        return getDigest(MessageDigestAlgorithms.MD5);
    }

    /**
     * Returns an SHA-1 MessageDigest instance.
     *
     * <p><strong>WARNING:</strong> SHA-1 is deprecated for cryptographic use due to known
     * collision vulnerabilities. It should NOT be used for digital signatures, certificates,
     * or other security-critical applications. Consider using SHA-256 or SHA-512 instead.
     * SHA-1 may still be acceptable for HMAC and non-security purposes.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest sha1 = DigestUtil.getSha1Digest();
     * byte[] hash = sha1.digest(data);
     * }</pre>
     *
     * @return A new SHA-1 MessageDigest instance (produces 20-byte/160-bit hashes)
     * @throws IllegalArgumentException If SHA-1 is not available (should not happen with standard JRE)
     *
     * @see MessageDigestAlgorithms#SHA_1
     */
    public static MessageDigest getSha1Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_1);
    }

    /**
     * Returns an SHA-256 MessageDigest instance.
     *
     * <p>SHA-256 is part of the SHA-2 family and is currently considered secure and widely
     * recommended for most cryptographic purposes including digital signatures, certificates,
     * and data integrity verification. It provides a good balance between security and performance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest sha256 = DigestUtil.getSha256Digest();
     * byte[] secureHash = sha256.digest(sensitiveData);
     * }</pre>
     *
     * @return A new SHA-256 MessageDigest instance (produces 32-byte/256-bit hashes)
     * @throws IllegalArgumentException If SHA-256 is not available (should not happen with standard JRE)
     *
     * @see MessageDigestAlgorithms#SHA_256
     */
    public static MessageDigest getSha256Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_256);
    }

    /**
     * Returns an SHA3-224 MessageDigest instance.
     *
     * <p>SHA-3 is the latest member of the Secure Hash Algorithm family, standardized in 2015.
     * It uses a different internal structure (sponge construction) compared to SHA-2, providing
     * an alternative security approach. SHA3-224 produces a 224-bit hash output.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest sha3 = DigestUtil.getSha3_224Digest();
     * byte[] hash = sha3.digest(data);
     * }</pre>
     *
     * @return A new SHA3-224 MessageDigest instance (produces 28-byte/224-bit hashes)
     * @throws IllegalArgumentException If SHA3-224 is not available (requires Java 9 or later)
     *
     * @see MessageDigestAlgorithms#SHA3_224
     */
    public static MessageDigest getSha3_224Digest() {
        return getDigest(MessageDigestAlgorithms.SHA3_224);
    }

    /**
     * Returns an SHA3-256 MessageDigest instance.
     *
     * <p>SHA3-256 is part of the SHA-3 family and provides an alternative to SHA-256 with
     * a different internal structure based on the Keccak algorithm. It offers similar security
     * level to SHA-256 but with different mathematical foundations, providing diversity in
     * cryptographic primitives.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest sha3 = DigestUtil.getSha3_256Digest();
     * byte[] hash = sha3.digest(message.getBytes());
     * }</pre>
     *
     * @return A new SHA3-256 MessageDigest instance (produces 32-byte/256-bit hashes)
     * @throws IllegalArgumentException If SHA3-256 is not available (requires Java 9 or later)
     *
     * @see MessageDigestAlgorithms#SHA3_256
     */
    public static MessageDigest getSha3_256Digest() {
        return getDigest(MessageDigestAlgorithms.SHA3_256);
    }

    /**
     * Returns an SHA3-384 MessageDigest instance.
     *
     * <p>SHA3-384 is part of the SHA-3 family and provides higher security strength than
     * SHA3-256. It is suitable for applications requiring enhanced security margins.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest sha3 = DigestUtil.getSha3_384Digest();
     * byte[] hash = sha3.digest(data);
     * }</pre>
     *
     * @return A new SHA3-384 MessageDigest instance (produces 48-byte/384-bit hashes)
     * @throws IllegalArgumentException If SHA3-384 is not available (requires Java 9 or later)
     *
     * @see MessageDigestAlgorithms#SHA3_384
     */
    public static MessageDigest getSha3_384Digest() {
        return getDigest(MessageDigestAlgorithms.SHA3_384);
    }

    /**
     * Returns an SHA3-512 MessageDigest instance.
     *
     * <p>SHA3-512 provides the highest security level in the SHA-3 family with 512-bit output.
     * It offers maximum collision resistance and is suitable for applications requiring the
     * highest level of security assurance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest sha3 = DigestUtil.getSha3_512Digest();
     * byte[] hash = sha3.digest(criticalData);
     * }</pre>
     *
     * @return A new SHA3-512 MessageDigest instance (produces 64-byte/512-bit hashes)
     * @throws IllegalArgumentException If SHA3-512 is not available (requires Java 9 or later)
     *
     * @see MessageDigestAlgorithms#SHA3_512
     */
    public static MessageDigest getSha3_512Digest() {
        return getDigest(MessageDigestAlgorithms.SHA3_512);
    }

    /**
     * Returns an SHA-384 MessageDigest instance.
     *
     * <p>SHA-384 is part of the SHA-2 family and provides higher security than SHA-256 with
     * a 384-bit output. It is actually a truncated version of SHA-512, offering strong security
     * while producing smaller output than SHA-512. Suitable for applications requiring enhanced
     * security margins.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest sha384 = DigestUtil.getSha384Digest();
     * byte[] hash = sha384.digest(data);
     * }</pre>
     *
     * @return A new SHA-384 MessageDigest instance (produces 48-byte/384-bit hashes)
     * @throws IllegalArgumentException If SHA-384 is not available (should not happen with standard JRE)
     *
     * @see MessageDigestAlgorithms#SHA_384
     */
    public static MessageDigest getSha384Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_384);
    }

    /**
     * Returns an SHA-512/224 MessageDigest instance.
     *
     * <p>SHA-512/224 is a truncated variant of SHA-512 that produces 224-bit output.
     * It provides the security benefits of SHA-512's internal structure while producing
     * a smaller hash output. This can be more efficient on 64-bit platforms compared to
     * SHA-224.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest digest = DigestUtil.getSha512_224Digest();
     * byte[] hash = digest.digest(data);
     * }</pre>
     *
     * @return A new SHA-512/224 MessageDigest instance (produces 28-byte/224-bit hashes)
     * @throws IllegalArgumentException If SHA-512/224 is not available (requires Java 9 or later)
     *
     * @see MessageDigestAlgorithms#SHA_512_224
     */
    public static MessageDigest getSha512_224Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_512_224);
    }

    /**
     * Returns an SHA-512/256 MessageDigest instance.
     *
     * <p>SHA-512/256 is a truncated variant of SHA-512 that produces 256-bit output.
     * It combines the security benefits of SHA-512's internal structure with the same
     * output size as SHA-256. It can offer better performance on 64-bit systems compared
     * to SHA-256 due to its use of 64-bit operations.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest digest = DigestUtil.getSha512_256Digest();
     * byte[] hash = digest.digest(data);
     * }</pre>
     *
     * @return A new SHA-512/256 MessageDigest instance (produces 32-byte/256-bit hashes)
     * @throws IllegalArgumentException If SHA-512/256 is not available (requires Java 9 or later)
     *
     * @see MessageDigestAlgorithms#SHA_512_256
     */
    public static MessageDigest getSha512_256Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_512_256);
    }

    /**
     * Returns an SHA-512 MessageDigest instance.
     *
     * <p>SHA-512 is part of the SHA-2 family and provides the highest security level
     * with 512-bit output. It is suitable for applications requiring maximum security
     * assurance and is more efficient on 64-bit platforms due to its use of 64-bit
     * operations. Widely used in security-critical applications.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest sha512 = DigestUtil.getSha512Digest();
     * byte[] hash = sha512.digest(criticalData);
     * }</pre>
     *
     * @return A new SHA-512 MessageDigest instance (produces 64-byte/512-bit hashes)
     * @throws IllegalArgumentException If SHA-512 is not available (should not happen with standard JRE)
     *
     * @see MessageDigestAlgorithms#SHA_512
     */
    public static MessageDigest getSha512Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_512);
    }

    /**
     * Returns an SHA-1 MessageDigest instance.
     *
     * <p><strong>WARNING:</strong> SHA-1 is deprecated for cryptographic use. Consider using SHA-256 or SHA-512.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest sha = DigestUtil.getShaDigest();     // Deprecated
     * MessageDigest sha1 = DigestUtil.getSha1Digest();   // Preferred
     * }</pre>
     *
     * @return An SHA-1 MessageDigest instance (produces 20-byte/160-bit hashes)
     * @throws IllegalArgumentException If SHA-1 is not available (should not happen with standard JRE)
     * @deprecated Use {@link #getSha1Digest()} instead
     */
    @Deprecated
    public static MessageDigest getShaDigest() {
        return getSha1Digest();
    }

    /**
     * Tests whether the specified message digest algorithm is available in the current JVM environment.
     * This method is useful for checking algorithm availability before attempting to use it,
     * allowing for graceful fallback to alternative algorithms.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (DigestUtil.isAvailable("SHA3-256")) {
     *     // Use SHA3-256 (available in Java 9+)
     *     return DigestUtil.getSha3_256Digest();
     * } else {
     *     // Fall back to SHA-256
     *     return DigestUtil.getSha256Digest();
     * }
     * }</pre>
     *
     * @param messageDigestAlgorithm The algorithm name to test (e.g., "SHA-256", "SHA3-512", "BLAKE2b").
     *                               Can be {@code null}, which will return {@code false}.
     * @return {@code true} if the algorithm is available and can be used, {@code false} otherwise
     *
     */
    public static boolean isAvailable(final String messageDigestAlgorithm) {
        return getDigest(messageDigestAlgorithm, null) != null;
    }

    /**
     * Calculates the MD2 digest of the input data and returns it as a 16-byte array.
     *
     * <p><strong>WARNING:</strong> MD2 is cryptographically broken. Use only for legacy compatibility.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.md2("Hello".getBytes());
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return MD2 digest as a 16-byte array
     *
     */
    public static byte[] md2(final byte[] data) {
        return getMd2Digest().digest(data);
    }

    /**
     * Calculates the MD2 digest of data from an InputStream.
     * The stream is read completely but NOT closed.
     *
     * <p><strong>WARNING:</strong> MD2 is cryptographically broken. Use only for legacy compatibility.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("legacy.dat")) {
     *     byte[] hash = DigestUtil.md2(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return MD2 digest as a 16-byte array
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static byte[] md2(final InputStream data) throws IOException {
        return digest(getMd2Digest(), data);
    }

    /**
     * Calculates the MD2 digest of a string (converted to UTF-8 bytes).
     *
     * <p><strong>WARNING:</strong> MD2 is cryptographically broken. Use only for legacy compatibility.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.md2("legacy text");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return MD2 digest as a 16-byte array
     *
     */
    public static byte[] md2(final String data) {
        return md2(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the MD2 digest and returns it as a 32-character hexadecimal string.
     *
     * <p><strong>WARNING:</strong> MD2 is cryptographically broken. Use only for legacy compatibility.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.md2Hex("password".getBytes());
     * // Returns something like "f03881a88c6e39135f0ecc60efd609b9"
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return MD2 digest as a 32-character lowercase hexadecimal string
     *
     */
    public static String md2Hex(final byte[] data) {
        return Hex.encodeToString(md2(data));
    }

    /**
     * Calculates the MD2 digest of stream data and returns it as a hex string.
     * The stream is read completely but NOT closed.
     *
     * <p><strong>WARNING:</strong> MD2 is cryptographically broken. Use only for legacy compatibility.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("legacy.dat")) {
     *     String hash = DigestUtil.md2Hex(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return MD2 digest as a 32-character lowercase hexadecimal string
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static String md2Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(md2(data));
    }

    /**
     * Calculates the MD2 digest of a string and returns it as a hex string.
     *
     * <p><strong>WARNING:</strong> MD2 is cryptographically broken. Use only for legacy compatibility.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.md2Hex("legacy text");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return MD2 digest as a 32-character lowercase hexadecimal string
     *
     */
    public static String md2Hex(final String data) {
        return Hex.encodeToString(md2(data));
    }

    /**
     * Calculates the MD5 digest of the input data and returns it as a 16-byte array.
     *
     * <p><strong>WARNING:</strong> MD5 is cryptographically broken. Do NOT use for security purposes.
     * Acceptable only for checksums and non-cryptographic uses.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] checksum = DigestUtil.md5(fileBytes);
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return MD5 digest as a 16-byte array
     */
    public static byte[] md5(final byte[] data) {
        return getMd5Digest().digest(data);
    }

    /**
     * Calculates the MD5 digest of data from an InputStream.
     * The stream is read completely but NOT closed.
     *
     * <p><strong>WARNING:</strong> MD5 is cryptographically broken. Do NOT use for security purposes.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("document.txt")) {
     *     byte[] checksum = DigestUtil.md5(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return MD5 digest as a 16-byte array
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static byte[] md5(final InputStream data) throws IOException {
        return digest(getMd5Digest(), data);
    }

    /**
     * Calculates the MD5 digest of a string (converted to UTF-8 bytes).
     *
     * <p><strong>WARNING:</strong> MD5 is cryptographically broken. Do NOT use for security purposes.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] checksum = DigestUtil.md5("file contents");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return MD5 digest as a 16-byte array
     */
    public static byte[] md5(final String data) {
        return md5(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the MD5 digest and returns it as a 32-character hexadecimal string.
     * Commonly used for file checksums and non-security fingerprinting.
     *
     * <p><strong>WARNING:</strong> MD5 is cryptographically broken. Do NOT use for security purposes.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String checksum = DigestUtil.md5Hex("Hello World".getBytes());
     * // Returns "b10a8db164e0754105b7a99be72e3fe5"
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return MD5 digest as a 32-character lowercase hexadecimal string
     */
    public static String md5Hex(final byte[] data) {
        return Hex.encodeToString(md5(data));
    }

    /**
     * Calculates the MD5 digest of stream data and returns it as a hex string.
     * The stream is read completely but NOT closed.
     *
     * <p><strong>WARNING:</strong> MD5 is cryptographically broken. Do NOT use for security purposes.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("document.txt")) {
     *     String checksum = DigestUtil.md5Hex(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return MD5 digest as a 32-character lowercase hexadecimal string
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static String md5Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(md5(data));
    }

    /**
     * Calculates the MD5 digest of a string and returns it as a hex string.
     *
     * <p><strong>WARNING:</strong> MD5 is cryptographically broken. Do NOT use for security purposes.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.md5Hex("test@example.com");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return MD5 digest as a 32-character lowercase hexadecimal string
     */
    public static String md5Hex(final String data) {
        return Hex.encodeToString(md5(data));
    }

    /**
     * Calculates the SHA-1 digest of the input data.
     *
     * <p><strong>WARNING:</strong> SHA-1 is deprecated for cryptographic use.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha("data".getBytes());    // Deprecated
     * byte[] hash = DigestUtil.sha1("data".getBytes());   // Preferred
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA-1 digest as a 20-byte array
     * @deprecated Use {@link #sha1(byte[])} instead
     */
    @Deprecated
    public static byte[] sha(final byte[] data) {
        return sha1(data);
    }

    /**
     * Calculates the SHA-1 digest of data from an InputStream.
     * The stream is read completely but NOT closed.
     *
     * <p><strong>WARNING:</strong> SHA-1 is deprecated for cryptographic use.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.dat")) {
     *     byte[] hash = DigestUtil.sha(is);  // Deprecated
     *     // Prefer: DigestUtil.sha1(is)
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA-1 digest as a 20-byte array
     * @throws IOException If an I/O error occurs while reading the stream
     * @deprecated Use {@link #sha1(InputStream)} instead
     */
    @Deprecated
    public static byte[] sha(final InputStream data) throws IOException {
        return sha1(data);
    }

    /**
     * Calculates the SHA-1 digest of a string.
     *
     * <p><strong>WARNING:</strong> SHA-1 is deprecated for cryptographic use.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha("message");    // Deprecated
     * byte[] hash = DigestUtil.sha1("message");   // Preferred
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA-1 digest as a 20-byte array
     * @deprecated Use {@link #sha1(String)} instead
     */
    @Deprecated
    public static byte[] sha(final String data) {
        return sha1(data);
    }

    /**
     * Calculates the SHA-1 digest of the input data and returns it as a 20-byte array.
     *
     * <p><strong>WARNING:</strong> SHA-1 is deprecated for cryptographic use due to known
     * collision vulnerabilities. Consider using SHA-256 or SHA-512 instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha1("data".getBytes());
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA-1 digest as a 20-byte array
     *
     */
    public static byte[] sha1(final byte[] data) {
        return getSha1Digest().digest(data);
    }

    /**
     * Calculates the SHA-1 digest of data from an InputStream.
     * The stream is read completely but NOT closed.
     *
     * <p><strong>WARNING:</strong> SHA-1 is deprecated for cryptographic use due to known
     * collision vulnerabilities. Consider using SHA-256 or SHA-512 instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.dat")) {
     *     byte[] hash = DigestUtil.sha1(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA-1 digest as a 20-byte array
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static byte[] sha1(final InputStream data) throws IOException {
        return digest(getSha1Digest(), data);
    }

    /**
     * Calculates the SHA-1 digest of a string (converted to UTF-8 bytes).
     *
     * <p><strong>WARNING:</strong> SHA-1 is deprecated for cryptographic use due to known
     * collision vulnerabilities. Consider using SHA-256 or SHA-512 instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha1("message");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA-1 digest as a 20-byte array
     */
    public static byte[] sha1(final String data) {
        return sha1(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA-1 digest and returns it as a 40-character hexadecimal string.
     *
     * <p><strong>WARNING:</strong> SHA-1 is deprecated for cryptographic use due to known
     * collision vulnerabilities. Consider using SHA-256 or SHA-512 instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.sha1Hex("data".getBytes());
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA-1 digest as a 40-character lowercase hexadecimal string
     *
     */
    public static String sha1Hex(final byte[] data) {
        return Hex.encodeToString(sha1(data));
    }

    /**
     * Calculates the SHA-1 digest of stream data and returns it as a hex string.
     * The stream is read completely but NOT closed.
     *
     * <p><strong>WARNING:</strong> SHA-1 is deprecated for cryptographic use due to known
     * collision vulnerabilities. Consider using SHA-256 or SHA-512 instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.dat")) {
     *     String hash = DigestUtil.sha1Hex(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA-1 digest as a 40-character lowercase hexadecimal string
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static String sha1Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha1(data));
    }

    /**
     * Calculates the SHA-1 digest of a string and returns it as a hex string.
     *
     * <p><strong>WARNING:</strong> SHA-1 is deprecated for cryptographic use due to known
     * collision vulnerabilities. Consider using SHA-256 or SHA-512 instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.sha1Hex("message");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA-1 digest as a 40-character lowercase hexadecimal string
     *
     */
    public static String sha1Hex(final String data) {
        return Hex.encodeToString(sha1(data));
    }

    /**
     * Calculates the SHA-256 digest of the input data and returns it as a 32-byte array.
     * SHA-256 is recommended for general cryptographic use.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] secureHash = DigestUtil.sha256(sensitiveData);
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA-256 digest as a 32-byte array
     */
    public static byte[] sha256(final byte[] data) {
        return getSha256Digest().digest(data);
    }

    /**
     * Calculates the SHA-256 digest of data from an InputStream.
     * The stream is read completely but NOT closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("document.pdf")) {
     *     byte[] hash = DigestUtil.sha256(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA-256 digest as a 32-byte array
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static byte[] sha256(final InputStream data) throws IOException {
        return digest(getSha256Digest(), data);
    }

    /**
     * Calculates the SHA-256 digest of a string (converted to UTF-8 bytes).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha256("sensitive data");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA-256 digest as a 32-byte array
     *
     */
    public static byte[] sha256(final String data) {
        return sha256(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA-256 digest and returns it as a 64-character hexadecimal string.
     * This is commonly used for password hashing (with salt) and data integrity verification.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.sha256Hex("password" + salt);
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA-256 digest as a 64-character lowercase hexadecimal string
     */
    public static String sha256Hex(final byte[] data) {
        return Hex.encodeToString(sha256(data));
    }

    /**
     * Calculates the SHA-256 digest of stream data and returns it as a hex string.
     * The stream is read completely but NOT closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("document.pdf")) {
     *     String hash = DigestUtil.sha256Hex(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA-256 digest as a 64-character lowercase hexadecimal string
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static String sha256Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha256(data));
    }

    /**
     * Calculates the SHA-256 digest of a string and returns it as a hex string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.sha256Hex("password" + salt);
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA-256 digest as a 64-character lowercase hexadecimal string
     *
     */
    public static String sha256Hex(final String data) {
        return Hex.encodeToString(sha256(data));
    }

    /**
     * Calculates the SHA3-224 digest of the input data.
     * SHA-3 is an alternative to SHA-2 with different internal structure based on the Keccak algorithm.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha3_224("data".getBytes());
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA3-224 digest as a 28-byte array
     *
     */
    public static byte[] sha3_224(final byte[] data) {
        return getSha3_224Digest().digest(data);
    }

    /**
     * Calculates the SHA3-224 digest of data from an InputStream.
     * The stream is read completely but NOT closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.dat")) {
     *     byte[] hash = DigestUtil.sha3_224(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA3-224 digest as a 28-byte array
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static byte[] sha3_224(final InputStream data) throws IOException {
        return digest(getSha3_224Digest(), data);
    }

    /**
     * Calculates the SHA3-224 digest of a string (converted to UTF-8 bytes).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha3_224("message");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA3-224 digest as a 28-byte array
     *
     */
    public static byte[] sha3_224(final String data) {
        return sha3_224(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA3-224 digest and returns it as a hexadecimal string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.sha3_224Hex("data".getBytes());
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA3-224 digest as a 56-character lowercase hexadecimal string
     *
     */
    public static String sha3_224Hex(final byte[] data) {
        return Hex.encodeToString(sha3_224(data));
    }

    /**
     * Calculates the SHA3-224 digest of stream data and returns it as a hex string.
     * The stream is read completely but NOT closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.dat")) {
     *     String hash = DigestUtil.sha3_224Hex(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA3-224 digest as a 56-character lowercase hexadecimal string
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static String sha3_224Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha3_224(data));
    }

    /**
     * Calculates the SHA3-224 digest of a string and returns it as a hex string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.sha3_224Hex("message");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA3-224 digest as a 56-character lowercase hexadecimal string
     *
     */
    public static String sha3_224Hex(final String data) {
        return Hex.encodeToString(sha3_224(data));
    }

    /**
     * Calculates the SHA3-256 digest of the input data.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha3_256("data".getBytes());
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA3-256 digest as a 32-byte array
     *
     */
    public static byte[] sha3_256(final byte[] data) {
        return getSha3_256Digest().digest(data);
    }

    /**
     * Calculates the SHA3-256 digest of data from an InputStream.
     * The stream is read completely but NOT closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.dat")) {
     *     byte[] hash = DigestUtil.sha3_256(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA3-256 digest as a 32-byte array
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static byte[] sha3_256(final InputStream data) throws IOException {
        return digest(getSha3_256Digest(), data);
    }

    /**
     * Calculates the SHA3-256 digest of a string (converted to UTF-8 bytes).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha3_256("message");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA3-256 digest as a 32-byte array
     *
     */
    public static byte[] sha3_256(final String data) {
        return sha3_256(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA3-256 digest and returns it as a hexadecimal string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.sha3_256Hex("data".getBytes());
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA3-256 digest as a 64-character lowercase hexadecimal string
     *
     */
    public static String sha3_256Hex(final byte[] data) {
        return Hex.encodeToString(sha3_256(data));
    }

    /**
     * Calculates the SHA3-256 digest of stream data and returns it as a hex string.
     * The stream is read completely but NOT closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.dat")) {
     *     String hash = DigestUtil.sha3_256Hex(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA3-256 digest as a 64-character lowercase hexadecimal string
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static String sha3_256Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha3_256(data));
    }

    /**
     * Calculates the SHA3-256 digest of a string and returns it as a hex string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.sha3_256Hex("message");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA3-256 digest as a 64-character lowercase hexadecimal string
     *
     */
    public static String sha3_256Hex(final String data) {
        return Hex.encodeToString(sha3_256(data));
    }

    /**
     * Calculates the SHA3-384 digest of the input data.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha3_384("data".getBytes());
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA3-384 digest as a 48-byte array
     *
     */
    public static byte[] sha3_384(final byte[] data) {
        return getSha3_384Digest().digest(data);
    }

    /**
     * Calculates the SHA3-384 digest of data from an InputStream.
     * The stream is read completely but NOT closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.dat")) {
     *     byte[] hash = DigestUtil.sha3_384(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA3-384 digest as a 48-byte array
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static byte[] sha3_384(final InputStream data) throws IOException {
        return digest(getSha3_384Digest(), data);
    }

    /**
     * Calculates the SHA3-384 digest of a string (converted to UTF-8 bytes).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha3_384("message");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA3-384 digest as a 48-byte array
     *
     */
    public static byte[] sha3_384(final String data) {
        return sha3_384(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA3-384 digest and returns it as a hexadecimal string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.sha3_384Hex("data".getBytes());
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA3-384 digest as a 96-character lowercase hexadecimal string
     *
     */
    public static String sha3_384Hex(final byte[] data) {
        return Hex.encodeToString(sha3_384(data));
    }

    /**
     * Calculates the SHA3-384 digest of stream data and returns it as a hex string.
     * The stream is read completely but NOT closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.dat")) {
     *     String hash = DigestUtil.sha3_384Hex(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA3-384 digest as a 96-character lowercase hexadecimal string
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static String sha3_384Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha3_384(data));
    }

    /**
     * Calculates the SHA3-384 digest of a string and returns it as a hex string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.sha3_384Hex("message");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA3-384 digest as a 96-character lowercase hexadecimal string
     *
     */
    public static String sha3_384Hex(final String data) {
        return Hex.encodeToString(sha3_384(data));
    }

    /**
     * Calculates the SHA3-512 digest of the input data.
     * Provides the highest security level in the SHA-3 family.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha3_512("critical data".getBytes());
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA3-512 digest as a 64-byte array
     *
     */
    public static byte[] sha3_512(final byte[] data) {
        return getSha3_512Digest().digest(data);
    }

    /**
     * Calculates the SHA3-512 digest of data from an InputStream.
     * The stream is read completely but NOT closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.dat")) {
     *     byte[] hash = DigestUtil.sha3_512(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA3-512 digest as a 64-byte array
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static byte[] sha3_512(final InputStream data) throws IOException {
        return digest(getSha3_512Digest(), data);
    }

    /**
     * Calculates the SHA3-512 digest of a string (converted to UTF-8 bytes).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha3_512("message");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA3-512 digest as a 64-byte array
     *
     */
    public static byte[] sha3_512(final String data) {
        return sha3_512(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA3-512 digest and returns it as a hexadecimal string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.sha3_512Hex("critical data".getBytes());
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA3-512 digest as a 128-character lowercase hexadecimal string
     *
     */
    public static String sha3_512Hex(final byte[] data) {
        return Hex.encodeToString(sha3_512(data));
    }

    /**
     * Calculates the SHA3-512 digest of stream data and returns it as a hex string.
     * The stream is read completely but NOT closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.dat")) {
     *     String hash = DigestUtil.sha3_512Hex(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA3-512 digest as a 128-character lowercase hexadecimal string
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static String sha3_512Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha3_512(data));
    }

    /**
     * Calculates the SHA3-512 digest of a string and returns it as a hex string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.sha3_512Hex("message");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA3-512 digest as a 128-character lowercase hexadecimal string
     *
     */
    public static String sha3_512Hex(final String data) {
        return Hex.encodeToString(sha3_512(data));
    }

    /**
     * Calculates the SHA-384 digest of the input data and returns it as a 48-byte array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha384("data".getBytes());
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA-384 digest as a 48-byte array
     *
     */
    public static byte[] sha384(final byte[] data) {
        return getSha384Digest().digest(data);
    }

    /**
     * Calculates the SHA-384 digest of data from an InputStream.
     * The stream is read completely but NOT closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.dat")) {
     *     byte[] hash = DigestUtil.sha384(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA-384 digest as a 48-byte array
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static byte[] sha384(final InputStream data) throws IOException {
        return digest(getSha384Digest(), data);
    }

    /**
     * Calculates the SHA-384 digest of a string (converted to UTF-8 bytes).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha384("message");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA-384 digest as a 48-byte array
     *
     */
    public static byte[] sha384(final String data) {
        return sha384(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA-384 digest and returns it as a 96-character hexadecimal string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.sha384Hex("data".getBytes());
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA-384 digest as a 96-character lowercase hexadecimal string
     *
     */
    public static String sha384Hex(final byte[] data) {
        return Hex.encodeToString(sha384(data));
    }

    /**
     * Calculates the SHA-384 digest of stream data and returns it as a hex string.
     * The stream is read completely but NOT closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.dat")) {
     *     String hash = DigestUtil.sha384Hex(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA-384 digest as a 96-character lowercase hexadecimal string
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static String sha384Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha384(data));
    }

    /**
     * Calculates the SHA-384 digest of a string and returns it as a hex string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.sha384Hex("message");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA-384 digest as a 96-character lowercase hexadecimal string
     *
     */
    public static String sha384Hex(final String data) {
        return Hex.encodeToString(sha384(data));
    }

    /**
     * Calculates the SHA-512 digest of the input data and returns it as a 64-byte array.
     * Provides maximum security in the SHA-2 family.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha512("critical data".getBytes());
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA-512 digest as a 64-byte array
     *
     */
    public static byte[] sha512(final byte[] data) {
        return getSha512Digest().digest(data);
    }

    /**
     * Calculates the SHA-512 digest of data from an InputStream.
     * The stream is read completely but NOT closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.dat")) {
     *     byte[] hash = DigestUtil.sha512(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA-512 digest as a 64-byte array
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static byte[] sha512(final InputStream data) throws IOException {
        return digest(getSha512Digest(), data);
    }

    /**
     * Calculates the SHA-512 digest of a string (converted to UTF-8 bytes).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha512("message");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA-512 digest as a 64-byte array
     *
     */
    public static byte[] sha512(final String data) {
        return sha512(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA-512/224 digest of the input data.
     * This is a truncated variant of SHA-512 that produces 224-bit output.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha512_224("data".getBytes());
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA-512/224 digest as a 28-byte array
     *
     */
    public static byte[] sha512_224(final byte[] data) {
        return getSha512_224Digest().digest(data);
    }

    /**
     * Calculates the SHA-512/224 digest of data from an InputStream.
     * The stream is read completely but NOT closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.dat")) {
     *     byte[] hash = DigestUtil.sha512_224(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA-512/224 digest as a 28-byte array
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static byte[] sha512_224(final InputStream data) throws IOException {
        return digest(getSha512_224Digest(), data);
    }

    /**
     * Calculates the SHA-512/224 digest of a string (converted to UTF-8 bytes).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha512_224("message");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA-512/224 digest as a 28-byte array
     *
     */
    public static byte[] sha512_224(final String data) {
        return sha512_224(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA-512/224 digest and returns it as a hexadecimal string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.sha512_224Hex("data".getBytes());
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA-512/224 digest as a 56-character lowercase hexadecimal string
     *
     */
    public static String sha512_224Hex(final byte[] data) {
        return Hex.encodeToString(sha512_224(data));
    }

    /**
     * Calculates the SHA-512/224 digest of stream data and returns it as a hex string.
     * The stream is read completely but NOT closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.dat")) {
     *     String hash = DigestUtil.sha512_224Hex(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA-512/224 digest as a 56-character lowercase hexadecimal string
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static String sha512_224Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha512_224(data));
    }

    /**
     * Calculates the SHA-512/224 digest of a string and returns it as a hex string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.sha512_224Hex("message");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA-512/224 digest as a 56-character lowercase hexadecimal string
     *
     */
    public static String sha512_224Hex(final String data) {
        return Hex.encodeToString(sha512_224(data));
    }

    /**
     * Calculates the SHA-512/256 digest of the input data.
     * This truncated variant offers better 64-bit performance than SHA-256.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha512_256("data".getBytes());
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA-512/256 digest as a 32-byte array
     *
     */
    public static byte[] sha512_256(final byte[] data) {
        return getSha512_256Digest().digest(data);
    }

    /**
     * Calculates the SHA-512/256 digest of data from an InputStream.
     * The stream is read completely but NOT closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.dat")) {
     *     byte[] hash = DigestUtil.sha512_256(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA-512/256 digest as a 32-byte array
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static byte[] sha512_256(final InputStream data) throws IOException {
        return digest(getSha512_256Digest(), data);
    }

    /**
     * Calculates the SHA-512/256 digest of a string (converted to UTF-8 bytes).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] hash = DigestUtil.sha512_256("message");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA-512/256 digest as a 32-byte array
     *
     */
    public static byte[] sha512_256(final String data) {
        return sha512_256(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA-512/256 digest and returns it as a hexadecimal string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.sha512_256Hex("data".getBytes());
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA-512/256 digest as a 64-character lowercase hexadecimal string
     *
     */
    public static String sha512_256Hex(final byte[] data) {
        return Hex.encodeToString(sha512_256(data));
    }

    /**
     * Calculates the SHA-512/256 digest of stream data and returns it as a hex string.
     * The stream is read completely but NOT closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.dat")) {
     *     String hash = DigestUtil.sha512_256Hex(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA-512/256 digest as a 64-character lowercase hexadecimal string
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static String sha512_256Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha512_256(data));
    }

    /**
     * Calculates the SHA-512/256 digest of a string and returns it as a hex string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.sha512_256Hex("message");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA-512/256 digest as a 64-character lowercase hexadecimal string
     *
     */
    public static String sha512_256Hex(final String data) {
        return Hex.encodeToString(sha512_256(data));
    }

    /**
     * Calculates the SHA-512 digest and returns it as a 128-character hexadecimal string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.sha512Hex("critical data".getBytes());
     * }</pre>
     *
     * @param data The data to digest.
     * @return SHA-512 digest as a hex string.
     */
    public static String sha512Hex(final byte[] data) {
        return Hex.encodeToString(sha512(data));
    }

    /**
     * Calculates the SHA-512 digest of stream data and returns it as a hex string.
     * The stream is read completely but NOT closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.dat")) {
     *     String hash = DigestUtil.sha512Hex(is);
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA-512 digest as a 128-character lowercase hexadecimal string
     * @throws IOException If an I/O error occurs while reading the stream
     *
     */
    public static String sha512Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha512(data));
    }

    /**
     * Calculates the SHA-512 digest of a string and returns it as a hex string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.sha512Hex("message");
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA-512 digest as a 128-character lowercase hexadecimal string
     *
     */
    public static String sha512Hex(final String data) {
        return Hex.encodeToString(sha512(data));
    }

    /**
     * Calculates the SHA-1 digest and returns it as a hexadecimal string.
     *
     * <p><strong>WARNING:</strong> SHA-1 is deprecated for cryptographic use.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.shaHex("data".getBytes());    // Deprecated
     * String hash = DigestUtil.sha1Hex("data".getBytes());   // Preferred
     * }</pre>
     *
     * @param data The data to digest (must not be {@code null})
     * @return SHA-1 digest as a 40-character lowercase hexadecimal string
     * @deprecated Use {@link #sha1Hex(byte[])} instead
     */
    @Deprecated
    public static String shaHex(final byte[] data) {
        return sha1Hex(data);
    }

    /**
     * Calculates the SHA-1 digest of stream data and returns it as a hex string.
     * The stream is read completely but NOT closed.
     *
     * <p><strong>WARNING:</strong> SHA-1 is deprecated for cryptographic use.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.dat")) {
     *     String hash = DigestUtil.shaHex(is);  // Deprecated
     *     // Prefer: DigestUtil.sha1Hex(is)
     * }
     * }</pre>
     *
     * @param data The InputStream to read and digest (must not be {@code null})
     * @return SHA-1 digest as a 40-character lowercase hexadecimal string
     * @throws IOException If an I/O error occurs while reading the stream
     * @deprecated Use {@link #sha1Hex(InputStream)} instead
     */
    @Deprecated
    public static String shaHex(final InputStream data) throws IOException {
        return sha1Hex(data);
    }

    /**
     * Calculates the SHA-1 digest of a string and returns it as a hex string.
     *
     * <p><strong>WARNING:</strong> SHA-1 is deprecated for cryptographic use.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hash = DigestUtil.shaHex("message");    // Deprecated
     * String hash = DigestUtil.sha1Hex("message");   // Preferred
     * }</pre>
     *
     * @param data The string to digest (must not be {@code null})
     * @return SHA-1 digest as a 40-character lowercase hexadecimal string
     * @deprecated Use {@link #sha1Hex(String)} instead
     */
    @Deprecated
    public static String shaHex(final String data) {
        return sha1Hex(data);
    }

    /**
     * Updates the given MessageDigest with the specified byte array data.
     * This method is useful when computing digests incrementally.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest md = DigestUtil.getSha256Digest();
     * DigestUtil.updateDigest(md, part1);
     * DigestUtil.updateDigest(md, part2);
     * byte[] finalHash = md.digest();
     * }</pre>
     *
     * @param messageDigest The MessageDigest to update.
     * @param valueToDigest The byte array to add to the digest.
     * @return The updated MessageDigest (same instance as input).
     */
    public static MessageDigest updateDigest(final MessageDigest messageDigest, final byte[] valueToDigest) {
        messageDigest.update(valueToDigest);
        return messageDigest;
    }

    /**
     * Updates the given MessageDigest with data from a ByteBuffer.
     * The ByteBuffer's position will be advanced to its limit.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest md = DigestUtil.getSha256Digest();
     * ByteBuffer buffer = ByteBuffer.wrap("data".getBytes());
     * DigestUtil.updateDigest(md, buffer);
     * byte[] hash = md.digest();
     * }</pre>
     *
     * @param messageDigest The MessageDigest to update (must not be {@code null})
     * @param valueToDigest The ByteBuffer containing data to add to the digest (must not be {@code null})
     * @return The updated MessageDigest (same instance as input)
     *
     */
    public static MessageDigest updateDigest(final MessageDigest messageDigest, final ByteBuffer valueToDigest) {
        messageDigest.update(valueToDigest);
        return messageDigest;
    }

    /**
     * Reads through a File and updates the MessageDigest with its contents.
     * The file is read using a buffered stream for efficiency.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest md = DigestUtil.getSha256Digest();
     * DigestUtil.updateDigest(md, new File("document.pdf"));
     * byte[] hash = md.digest();
     * }</pre>
     *
     * @param digest The MessageDigest to update (must not be {@code null})
     * @param data The File to read (must exist and be readable)
     * @return The updated MessageDigest (same instance as input)
     * @throws IOException If an I/O error occurs while reading the file
     *
     */
    public static MessageDigest updateDigest(final MessageDigest digest, final File data) throws IOException {
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(data))) {
            return updateDigest(digest, inputStream);
        }
    }

    /**
     * Updates a MessageDigest from data read from a FileChannel using NIO.
     * This is efficient for large files.
     *
     * @param digest The MessageDigest to update.
     * @param data The FileChannel to read from.
     * @return The updated MessageDigest.
     * @throws IOException If an error occurs while reading.
     */
    private static MessageDigest updateDigest(final MessageDigest digest, final FileChannel data) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        while (data.read(buffer) > 0) {
            buffer.flip();
            digest.update(buffer);
            buffer.clear();
        }
        return digest;
    }

    /**
     * Reads through an InputStream and updates the MessageDigest with the data.
     * The stream is read completely but not closed. This method uses a buffer
     * for efficient reading of large streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest md = DigestUtil.getMd5Digest();
     * try (InputStream is = new FileInputStream("large.dat")) {
     *     DigestUtil.updateDigest(md, is);
     * }
     * byte[] hash = md.digest();
     * }</pre>
     *
     * @param digest The MessageDigest to update.
     * @param inputStream The InputStream to read.
     * @return The updated MessageDigest (same instance as input).
     * @throws IOException If an error occurs while reading the stream.
     */
    public static MessageDigest updateDigest(final MessageDigest digest, final InputStream inputStream) throws IOException {
        final byte[] buffer = new byte[BUFFER_SIZE];
        int read = inputStream.read(buffer, 0, BUFFER_SIZE);

        while (read > -1) {
            digest.update(buffer, 0, read);
            read = inputStream.read(buffer, 0, BUFFER_SIZE);
        }

        return digest;
    }

    /**
     * Reads through a file at the specified Path and updates the MessageDigest.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest md = DigestUtil.getSha256Digest();
     * DigestUtil.updateDigest(md, Paths.get("data.txt"));
     * byte[] hash = md.digest();
     * }</pre>
     *
     * @param digest The MessageDigest to update (must not be {@code null})
     * @param path The Path to the file to read (must not be {@code null})
     * @param options Optional open options for the file (e.g., StandardOpenOption.READ).
     *                If not specified, default options are used.
     * @return The updated MessageDigest (same instance as input)
     * @throws IOException If an I/O error occurs while reading the file
     *
     */
    public static MessageDigest updateDigest(final MessageDigest digest, final Path path, final OpenOption... options) throws IOException {
        try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(path, options))) {
            return updateDigest(digest, inputStream);
        }
    }

    /**
     * Reads through a RandomAccessFile using NIO and updates the MessageDigest.
     * This method is efficient for large files as it uses FileChannel.
     * The RandomAccessFile is read from its current position to the end.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest md = DigestUtil.getSha256Digest();
     * try (RandomAccessFile raf = new RandomAccessFile("large.dat", "r")) {
     *     DigestUtil.updateDigest(md, raf);
     * }
     * byte[] hash = md.digest();
     * }</pre>
     *
     * @param digest The MessageDigest to update (must not be {@code null})
     * @param data The RandomAccessFile to read (must not be {@code null})
     * @return The updated MessageDigest (same instance as input)
     * @throws IOException If an I/O error occurs while reading
     *
     */
    public static MessageDigest updateDigest(final MessageDigest digest, final RandomAccessFile data) throws IOException {
        return updateDigest(digest, data.getChannel());
    }

    /**
     * Updates the given MessageDigest with a string value (converted to UTF-8 bytes).
     * This is a convenience method for updating a digest with text data.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MessageDigest md = DigestUtil.getSha256Digest();
     * DigestUtil.updateDigest(md, "Hello ");
     * DigestUtil.updateDigest(md, "World");
     * String hash = Hex.encodeToString(md.digest());
     * }</pre>
     *
     * @param messageDigest The MessageDigest to update.
     * @param valueToDigest The string value to add to the digest.
     * @return The updated MessageDigest (same instance as input).
     */
    public static MessageDigest updateDigest(final MessageDigest messageDigest, final String valueToDigest) {
        messageDigest.update(Strings.getBytesUtf8(valueToDigest));
        return messageDigest;
    }

    /**
     * Contains standard algorithm names for MessageDigest that can be used with the
     * {@link DigestUtil#getDigest(String)} method and other methods requiring algorithm names.
     * 
     * <p>These constants represent the standard names as defined in the Java Cryptography
     * Architecture Standard Algorithm Name Documentation.</p>
     * 
     * @see <a href="http://docs.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html">
     *      Java Cryptography Architecture Standard Algorithm Name Documentation</a>
     */
    static class MessageDigestAlgorithms {

        /**
         * The MD2 message digest algorithm defined in RFC 1319.
         * This algorithm is obsolete and should not be used.
         */
        public static final String MD2 = "MD2";

        /**
         * The MD5 message digest algorithm defined in RFC 1321.
         * This algorithm is cryptographically broken and should not be used for security.
         */
        public static final String MD5 = "MD5";

        /**
         * The SHA-1 hash algorithm defined in FIPS PUB 180-2.
         * This algorithm is deprecated for cryptographic use.
         */
        public static final String SHA_1 = "SHA-1";

        /**
         * The SHA-224 hash algorithm defined in FIPS PUB 180-3.
         * Present in Oracle Java 8.
         * 
         */
        public static final String SHA_224 = "SHA-224";

        /**
         * The SHA-256 hash algorithm defined in FIPS PUB 180-2.
         * This is currently recommended for general cryptographic use.
         */
        public static final String SHA_256 = "SHA-256";

        /**
         * The SHA-384 hash algorithm defined in FIPS PUB 180-2.
         * Provides higher security than SHA-256.
         */
        public static final String SHA_384 = "SHA-384";

        /**
         * The SHA-512 hash algorithm defined in FIPS PUB 180-2.
         * Provides maximum security in the SHA-2 family.
         */
        public static final String SHA_512 = "SHA-512";

        /**
         * The SHA-512/224 hash algorithm defined in FIPS PUB 180-4.
         * A truncated variant of SHA-512. Included starting in Oracle Java 9.
         * 
         */
        public static final String SHA_512_224 = "SHA-512/224";

        /**
         * The SHA-512/256 hash algorithm defined in FIPS PUB 180-4.
         * A truncated variant of SHA-512. Included starting in Oracle Java 9.
         * 
         */
        public static final String SHA_512_256 = "SHA-512/256";

        /**
         * The SHA3-224 hash algorithm defined in FIPS PUB 202.
         * Part of the SHA-3 family. Included starting in Oracle Java 9.
         * 
         */
        public static final String SHA3_224 = "SHA3-224";

        /**
         * The SHA3-256 hash algorithm defined in FIPS PUB 202.
         * Part of the SHA-3 family. Included starting in Oracle Java 9.
         * 
         */
        public static final String SHA3_256 = "SHA3-256";

        /**
         * The SHA3-384 hash algorithm defined in FIPS PUB 202.
         * Part of the SHA-3 family. Included starting in Oracle Java 9.
         * 
         */
        public static final String SHA3_384 = "SHA3-384";

        /**
         * The SHA3-512 hash algorithm defined in FIPS PUB 202.
         * Part of the SHA-3 family. Included starting in Oracle Java 9.
         * 
         */
        public static final String SHA3_512 = "SHA3-512";

        /**
         * Returns an array containing all MessageDigest algorithm constants defined in this class.
         * This can be useful for testing algorithm availability or iterating through algorithms.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * for (String algorithm : MessageDigestAlgorithms.values()) {
         *     if (DigestUtil.isAvailable(algorithm)) {
         *         System.out.println(algorithm + " is available");
         *     }
         * }
         * }</pre>
         *
         * @return An array of all algorithm name constants.
         *
         *
         */
        public static String[] values() {
            // Do not use a constant array here as that can be changed externally by accident or design
            return new String[] { MD2, MD5, SHA_1, SHA_224, SHA_256, SHA_384, SHA_512, SHA_512_224, SHA_512_256, SHA3_224, SHA3_256, SHA3_384, SHA3_512 };
        }

        private MessageDigestAlgorithms() {
            // cannot be instantiated.
        }
    }
}
