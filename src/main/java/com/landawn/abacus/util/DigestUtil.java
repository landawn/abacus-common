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

/**
 * Utility class for generating cryptographic message digests (hashes) using various algorithms.
 * This class provides convenient static methods for computing digests of byte arrays, files, 
 * input streams, and strings using standard algorithms like MD5, SHA-1, SHA-256, etc.
 * 
 * <p>This class is immutable and thread-safe. However, the MessageDigest instances it creates 
 * generally won't be thread-safe.</p>
 * 
 * <p>Note: This implementation is based on Apache Commons Codec.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * // Generate MD5 hash of a string
 * String hash = DigestUtil.md5Hex("Hello World");
 * 
 * // Generate SHA-256 hash of a file
 * byte[] digest = DigestUtil.sha256(new File("document.pdf"));
 * </pre>
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
     * This is a convenience method that simply calls digest() on the MessageDigest.
     *
     * @param messageDigest The MessageDigest algorithm to use (e.g., MD5, SHA-256)
     * @param data The byte array to digest
     * @return The computed digest as a byte array
     * 
     * <p>Example:</p>
     * <pre>
     * MessageDigest md = DigestUtil.getMd5Digest();
     * byte[] hash = DigestUtil.digest(md, "test".getBytes());
     * </pre>
     * 
     * @since 1.11
     */
    public static byte[] digest(final MessageDigest messageDigest, final byte[] data) {
        return messageDigest.digest(data);
    }

    /**
     * Reads through a ByteBuffer and computes the digest for the data.
     * The ByteBuffer's position will be advanced to its limit.
     *
     * @param messageDigest The MessageDigest algorithm to use (e.g., MD5, SHA-256)
     * @param data The ByteBuffer containing data to digest
     * @return The computed digest as a byte array
     * 
     * <p>Example:</p>
     * <pre>
     * ByteBuffer buffer = ByteBuffer.wrap("Hello".getBytes());
     * byte[] hash = DigestUtil.digest(DigestUtil.getSha1Digest(), buffer);
     * </pre>
     * 
     * @since 1.11
     */
    public static byte[] digest(final MessageDigest messageDigest, final ByteBuffer data) {
        messageDigest.update(data);
        return messageDigest.digest();
    }

    /**
     * Reads through a File and computes the digest for its contents.
     * The file is read using a buffered input stream for efficiency.
     *
     * @param messageDigest The MessageDigest algorithm to use (e.g., MD5, SHA-256)
     * @param data The File to read and digest
     * @return The computed digest as a byte array
     * @throws IOException If an error occurs while reading the file
     * 
     * <p>Example:</p>
     * <pre>
     * File file = new File("document.txt");
     * byte[] hash = DigestUtil.digest(DigestUtil.getSha256Digest(), file);
     * </pre>
     * 
     * @since 1.11
     */
    public static byte[] digest(final MessageDigest messageDigest, final File data) throws IOException {
        return updateDigest(messageDigest, data).digest();
    }

    /**
     * Reads through an InputStream and computes the digest for the data.
     * The stream is read completely but not closed by this method.
     *
     * @param messageDigest The MessageDigest algorithm to use (e.g., MD5, SHA-256)
     * @param data The InputStream to read and digest
     * @return The computed digest as a byte array
     * @throws IOException If an error occurs while reading from the stream
     * 
     * <p>Example:</p>
     * <pre>
     * try (InputStream is = new FileInputStream("data.bin")) {
     *     byte[] hash = DigestUtil.digest(DigestUtil.getMd5Digest(), is);
     * }
     * </pre>
     * 
     * @since 1.11
     */
    public static byte[] digest(final MessageDigest messageDigest, final InputStream data) throws IOException {
        return updateDigest(messageDigest, data).digest();
    }

    /**
     * Reads through a file at the specified Path and computes the digest for the data.
     * The file is opened according to the specified OpenOptions.
     *
     * @param messageDigest The MessageDigest algorithm to use (e.g., MD5, SHA-256)
     * @param data The Path to the file to digest
     * @param options Options for opening the file (e.g., StandardOpenOption.READ)
     * @return The computed digest as a byte array
     * @throws IOException If an error occurs while reading the file
     * 
     * <p>Example:</p>
     * <pre>
     * Path path = Paths.get("data.txt");
     * byte[] hash = DigestUtil.digest(DigestUtil.getSha512Digest(), path, StandardOpenOption.READ);
     * </pre>
     * 
     * @since 1.14
     */
    public static byte[] digest(final MessageDigest messageDigest, final Path data, final OpenOption... options) throws IOException {
        return updateDigest(messageDigest, data, options).digest();
    }

    /**
     * Reads through a RandomAccessFile using non-blocking I/O (NIO) and computes the digest.
     * This method is efficient for large files as it uses memory-mapped I/O.
     *
     * @param messageDigest The MessageDigest algorithm to use (e.g., MD5, SHA-256)
     * @param data The RandomAccessFile to read and digest
     * @return The computed digest as a byte array
     * @throws IOException If an error occurs while reading the file
     * 
     * <p>Example:</p>
     * <pre>
     * try (RandomAccessFile raf = new RandomAccessFile("large.dat", "r")) {
     *     byte[] hash = DigestUtil.digest(DigestUtil.getSha256Digest(), raf);
     * }
     * </pre>
     * 
     * @since 1.14
     */
    public static byte[] digest(final MessageDigest messageDigest, final RandomAccessFile data) throws IOException {
        return updateDigest(messageDigest, data).digest();
    }

    /**
     * Returns a MessageDigest instance for the specified algorithm.
     * This method wraps the checked NoSuchAlgorithmException in an unchecked IllegalArgumentException.
     *
     * @param algorithm The name of the algorithm (e.g., "MD5", "SHA-256"). 
     *                  See Java Cryptography Architecture documentation for standard names.
     * @return A MessageDigest instance for the specified algorithm
     * @throws IllegalArgumentException If the algorithm is not available
     * 
     * <p>Example:</p>
     * <pre>
     * MessageDigest md = DigestUtil.getDigest("SHA-512");
     * byte[] hash = md.digest("data".getBytes());
     * </pre>
     * 
     * @see MessageDigest#getInstance(String)
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
     * the algorithm is not available. This method never throws an exception.
     *
     * @param algorithm The name of the algorithm to attempt (e.g., "SHA-512")
     * @param defaultMessageDigest The MessageDigest to return if the algorithm is not available
     * @return A MessageDigest instance for the algorithm, or the default if unavailable
     * 
     * <p>Example:</p>
     * <pre>
     * MessageDigest fallback = DigestUtil.getSha256Digest();
     * MessageDigest md = DigestUtil.getDigest("SHA3-256", fallback);
     * </pre>
     * 
     * @since 1.11
     */
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
     * MD2 is an obsolete algorithm and should not be used for new applications.
     *
     * @return An MD2 MessageDigest instance
     * @throws IllegalArgumentException If MD2 is not available (should not happen with standard JRE)
     * 
     * <p>Example:</p>
     * <pre>
     * MessageDigest md2 = DigestUtil.getMd2Digest();
     * byte[] hash = md2.digest("legacy data".getBytes());
     * </pre>
     * 
     * @see MessageDigestAlgorithms#MD2
     * @since 1.7
     */
    public static MessageDigest getMd2Digest() {
        return getDigest(MessageDigestAlgorithms.MD2);
    }

    /**
     * Returns an MD5 MessageDigest instance.
     * Note: MD5 is cryptographically broken and should not be used for security purposes.
     * It may still be used for checksums and non-cryptographic purposes.
     *
     * @return An MD5 MessageDigest instance
     * @throws IllegalArgumentException If MD5 is not available (should not happen with standard JRE)
     * 
     * <p>Example:</p>
     * <pre>
     * MessageDigest md5 = DigestUtil.getMd5Digest();
     * byte[] checksum = md5.digest(fileData);
     * </pre>
     * 
     * @see MessageDigestAlgorithms#MD5
     */
    public static MessageDigest getMd5Digest() {
        return getDigest(MessageDigestAlgorithms.MD5);
    }

    /**
     * Returns an SHA-1 MessageDigest instance.
     * Note: SHA-1 is deprecated for cryptographic use due to collision vulnerabilities.
     *
     * @return An SHA-1 MessageDigest instance
     * @throws IllegalArgumentException If SHA-1 is not available (should not happen with standard JRE)
     * 
     * <p>Example:</p>
     * <pre>
     * MessageDigest sha1 = DigestUtil.getSha1Digest();
     * byte[] hash = sha1.digest(data);
     * </pre>
     * 
     * @see MessageDigestAlgorithms#SHA_1
     * @since 1.7
     */
    public static MessageDigest getSha1Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_1);
    }

    /**
     * Returns an SHA-256 MessageDigest instance.
     * SHA-256 is currently considered secure for most cryptographic purposes.
     *
     * @return An SHA-256 MessageDigest instance
     * @throws IllegalArgumentException If SHA-256 is not available (should not happen with standard JRE)
     * 
     * <p>Example:</p>
     * <pre>
     * MessageDigest sha256 = DigestUtil.getSha256Digest();
     * byte[] secureHash = sha256.digest(sensitiveData);
     * </pre>
     * 
     * @see MessageDigestAlgorithms#SHA_256
     */
    public static MessageDigest getSha256Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_256);
    }

    /**
     * Returns an SHA3-224 MessageDigest instance.
     * SHA-3 is the latest member of the Secure Hash Algorithm family.
     *
     * @return An SHA3-224 MessageDigest instance
     * @throws IllegalArgumentException If SHA3-224 is not available (requires Java 9+)
     * 
     * @see MessageDigestAlgorithms#SHA3_224
     * @since 1.12
     */
    public static MessageDigest getSha3_224Digest() {
        return getDigest(MessageDigestAlgorithms.SHA3_224);
    }

    /**
     * Returns an SHA3-256 MessageDigest instance.
     * SHA-3 provides an alternative to SHA-2 with different internal structure.
     *
     * @return An SHA3-256 MessageDigest instance
     * @throws IllegalArgumentException If SHA3-256 is not available (requires Java 9+)
     * 
     * @see MessageDigestAlgorithms#SHA3_256
     * @since 1.12
     */
    public static MessageDigest getSha3_256Digest() {
        return getDigest(MessageDigestAlgorithms.SHA3_256);
    }

    /**
     * Returns an SHA3-384 MessageDigest instance.
     *
     * @return An SHA3-384 MessageDigest instance
     * @throws IllegalArgumentException If SHA3-384 is not available (requires Java 9+)
     * 
     * @see MessageDigestAlgorithms#SHA3_384
     * @since 1.12
     */
    public static MessageDigest getSha3_384Digest() {
        return getDigest(MessageDigestAlgorithms.SHA3_384);
    }

    /**
     * Returns an SHA3-512 MessageDigest instance.
     * Provides the highest security level in the SHA-3 family.
     *
     * @return An SHA3-512 MessageDigest instance
     * @throws IllegalArgumentException If SHA3-512 is not available (requires Java 9+)
     * 
     * @see MessageDigestAlgorithms#SHA3_512
     * @since 1.12
     */
    public static MessageDigest getSha3_512Digest() {
        return getDigest(MessageDigestAlgorithms.SHA3_512);
    }

    /**
     * Returns an SHA-384 MessageDigest instance.
     * Provides higher security than SHA-256 with a 384-bit output.
     *
     * @return An SHA-384 MessageDigest instance
     * @throws IllegalArgumentException If SHA-384 is not available (should not happen with standard JRE)
     * 
     * @see MessageDigestAlgorithms#SHA_384
     */
    public static MessageDigest getSha384Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_384);
    }

    /**
     * Returns an SHA-512/224 MessageDigest instance.
     * A truncated variant of SHA-512 with 224-bit output.
     *
     * @return An SHA-512/224 MessageDigest instance
     * @throws IllegalArgumentException If SHA-512/224 is not available
     * 
     * @see MessageDigestAlgorithms#SHA_512_224
     */
    public static MessageDigest getSha512_224Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_512_224);
    }

    /**
     * Returns an SHA-512/256 MessageDigest instance.
     * A truncated variant of SHA-512 with 256-bit output, offering better performance
     * on 64-bit systems compared to SHA-256.
     *
     * @return An SHA-512/256 MessageDigest instance
     * @throws IllegalArgumentException If SHA-512/256 is not available
     * 
     * @see MessageDigestAlgorithms#SHA_512_256
     */
    public static MessageDigest getSha512_256Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_512_256);
    }

    /**
     * Returns an SHA-512 MessageDigest instance.
     * Provides the highest security level in the SHA-2 family with 512-bit output.
     *
     * @return An SHA-512 MessageDigest instance
     * @throws IllegalArgumentException If SHA-512 is not available (should not happen with standard JRE)
     * 
     * <p>Example:</p>
     * <pre>
     * MessageDigest sha512 = DigestUtil.getSha512Digest();
     * byte[] hash = sha512.digest(criticalData);
     * </pre>
     * 
     * @see MessageDigestAlgorithms#SHA_512
     */
    public static MessageDigest getSha512Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_512);
    }

    /**
     * Returns an SHA-1 MessageDigest instance.
     * 
     * @return An SHA-1 MessageDigest instance
     * @throws IllegalArgumentException If SHA-1 is not available
     * @deprecated Use {@link #getSha1Digest()} instead
     */
    @Deprecated
    public static MessageDigest getShaDigest() {
        return getSha1Digest();
    }

    /**
     * Tests whether the specified message digest algorithm is available in the current environment.
     *
     * @param messageDigestAlgorithm The algorithm name to test (e.g., "SHA-256", "MD5")
     * @return {@code true} if the algorithm is available, {@code false} otherwise
     * 
     * <p>Example:</p>
     * <pre>
     * if (DigestUtil.isAvailable("SHA3-256")) {
     *     // Use SHA3-256
     * } else {
     *     // Fall back to SHA-256
     * }
     * </pre>
     * 
     * @since 1.11
     */
    public static boolean isAvailable(final String messageDigestAlgorithm) {
        return getDigest(messageDigestAlgorithm, null) != null;
    }

    /**
     * Calculates the MD2 digest of the input data and returns it as a 16-byte array.
     *
     * @param data The data to digest
     * @return MD2 digest as a 16-byte array
     * 
     * <p>Example:</p>
     * <pre>
     * byte[] hash = DigestUtil.md2("Hello".getBytes());
     * </pre>
     * 
     * @since 1.7
     */
    public static byte[] md2(final byte[] data) {
        return getMd2Digest().digest(data);
    }

    /**
     * Calculates the MD2 digest of data from an InputStream.
     *
     * @param data The InputStream to read and digest
     * @return MD2 digest as a 16-byte array
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.7
     */
    public static byte[] md2(final InputStream data) throws IOException {
        return digest(getMd2Digest(), data);
    }

    /**
     * Calculates the MD2 digest of a string (converted to UTF-8 bytes).
     *
     * @param data The string to digest
     * @return MD2 digest as a 16-byte array
     * 
     * @since 1.7
     */
    public static byte[] md2(final String data) {
        return md2(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the MD2 digest and returns it as a 32-character hexadecimal string.
     *
     * @param data The data to digest
     * @return MD2 digest as a hex string
     * 
     * <p>Example:</p>
     * <pre>
     * String hash = DigestUtil.md2Hex("password".getBytes());
     * // Returns something like "f03881a88c6e39135f0ecc60efd609b9"
     * </pre>
     * 
     * @since 1.7
     */
    public static String md2Hex(final byte[] data) {
        return Hex.encodeToString(md2(data));
    }

    /**
     * Calculates the MD2 digest of stream data and returns it as a hex string.
     *
     * @param data The InputStream to read and digest
     * @return MD2 digest as a hex string
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.7
     */
    public static String md2Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(md2(data));
    }

    /**
     * Calculates the MD2 digest of a string and returns it as a hex string.
     *
     * @param data The string to digest
     * @return MD2 digest as a hex string
     * 
     * @since 1.7
     */
    public static String md2Hex(final String data) {
        return Hex.encodeToString(md2(data));
    }

    /**
     * Calculates the MD5 digest of the input data and returns it as a 16-byte array.
     * Note: MD5 should not be used for cryptographic purposes.
     *
     * @param data The data to digest
     * @return MD5 digest as a 16-byte array
     * 
     * <p>Example:</p>
     * <pre>
     * byte[] checksum = DigestUtil.md5(fileBytes);
     * </pre>
     */
    public static byte[] md5(final byte[] data) {
        return getMd5Digest().digest(data);
    }

    /**
     * Calculates the MD5 digest of data from an InputStream.
     *
     * @param data The InputStream to read and digest
     * @return MD5 digest as a 16-byte array
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.4
     */
    public static byte[] md5(final InputStream data) throws IOException {
        return digest(getMd5Digest(), data);
    }

    /**
     * Calculates the MD5 digest of a string (converted to UTF-8 bytes).
     *
     * @param data The string to digest
     * @return MD5 digest as a 16-byte array
     */
    public static byte[] md5(final String data) {
        return md5(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the MD5 digest and returns it as a 32-character hexadecimal string.
     * This is commonly used for file checksums and non-security fingerprinting.
     *
     * @param data The data to digest
     * @return MD5 digest as a hex string
     * 
     * <p>Example:</p>
     * <pre>
     * String checksum = DigestUtil.md5Hex("Hello World".getBytes());
     * // Returns "b10a8db164e0754105b7a99be72e3fe5"
     * </pre>
     */
    public static String md5Hex(final byte[] data) {
        return Hex.encodeToString(md5(data));
    }

    /**
     * Calculates the MD5 digest of stream data and returns it as a hex string.
     *
     * @param data The InputStream to read and digest
     * @return MD5 digest as a hex string
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.4
     */
    public static String md5Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(md5(data));
    }

    /**
     * Calculates the MD5 digest of a string and returns it as a hex string.
     *
     * @param data The string to digest
     * @return MD5 digest as a hex string
     * 
     * <p>Example:</p>
     * <pre>
     * String hash = DigestUtil.md5Hex("test@example.com");
     * </pre>
     */
    public static String md5Hex(final String data) {
        return Hex.encodeToString(md5(data));
    }

    /**
     * Calculates the SHA-1 digest of the input data.
     *
     * @param data The data to digest
     * @return SHA-1 digest as a byte array
     * @deprecated Use {@link #sha1(byte[])} instead
     */
    @Deprecated
    public static byte[] sha(final byte[] data) {
        return sha1(data);
    }

    /**
     * Calculates the SHA-1 digest of data from an InputStream.
     *
     * @param data The InputStream to read and digest
     * @return SHA-1 digest as a byte array
     * @throws IOException If an error occurs while reading the stream
     * @deprecated Use {@link #sha1(InputStream)} instead
     * @since 1.4
     */
    @Deprecated
    public static byte[] sha(final InputStream data) throws IOException {
        return sha1(data);
    }

    /**
     * Calculates the SHA-1 digest of a string.
     *
     * @param data The string to digest
     * @return SHA-1 digest as a byte array
     * @deprecated Use {@link #sha1(String)} instead
     */
    @Deprecated
    public static byte[] sha(final String data) {
        return sha1(data);
    }

    /**
     * Calculates the SHA-1 digest of the input data and returns it as a 20-byte array.
     * Note: SHA-1 is deprecated for cryptographic use.
     *
     * @param data The data to digest
     * @return SHA-1 digest as a 20-byte array
     * 
     * @since 1.7
     */
    public static byte[] sha1(final byte[] data) {
        return getSha1Digest().digest(data);
    }

    /**
     * Calculates the SHA-1 digest of data from an InputStream.
     *
     * @param data The InputStream to read and digest
     * @return SHA-1 digest as a 20-byte array
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.7
     */
    public static byte[] sha1(final InputStream data) throws IOException {
        return digest(getSha1Digest(), data);
    }

    /**
     * Calculates the SHA-1 digest of a string (converted to UTF-8 bytes).
     *
     * @param data The string to digest
     * @return SHA-1 digest as a 20-byte array
     */
    public static byte[] sha1(final String data) {
        return sha1(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA-1 digest and returns it as a 40-character hexadecimal string.
     *
     * @param data The data to digest
     * @return SHA-1 digest as a hex string
     * 
     * @since 1.7
     */
    public static String sha1Hex(final byte[] data) {
        return Hex.encodeToString(sha1(data));
    }

    /**
     * Calculates the SHA-1 digest of stream data and returns it as a hex string.
     *
     * @param data The InputStream to read and digest
     * @return SHA-1 digest as a hex string
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.7
     */
    public static String sha1Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha1(data));
    }

    /**
     * Calculates the SHA-1 digest of a string and returns it as a hex string.
     *
     * @param data The string to digest
     * @return SHA-1 digest as a hex string
     * 
     * @since 1.7
     */
    public static String sha1Hex(final String data) {
        return Hex.encodeToString(sha1(data));
    }

    /**
     * Calculates the SHA-256 digest of the input data and returns it as a 32-byte array.
     * SHA-256 is recommended for general cryptographic use.
     *
     * @param data The data to digest
     * @return SHA-256 digest as a 32-byte array
     * 
     * <p>Example:</p>
     * <pre>
     * byte[] secureHash = DigestUtil.sha256(sensitiveData);
     * </pre>
     * 
     * @since 1.4
     */
    public static byte[] sha256(final byte[] data) {
        return getSha256Digest().digest(data);
    }

    /**
     * Calculates the SHA-256 digest of data from an InputStream.
     *
     * @param data The InputStream to read and digest
     * @return SHA-256 digest as a 32-byte array
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.4
     */
    public static byte[] sha256(final InputStream data) throws IOException {
        return digest(getSha256Digest(), data);
    }

    /**
     * Calculates the SHA-256 digest of a string (converted to UTF-8 bytes).
     *
     * @param data The string to digest
     * @return SHA-256 digest as a 32-byte array
     * 
     * @since 1.4
     */
    public static byte[] sha256(final String data) {
        return sha256(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA-256 digest and returns it as a 64-character hexadecimal string.
     * This is commonly used for password hashing (with salt) and data integrity verification.
     *
     * @param data The data to digest
     * @return SHA-256 digest as a hex string
     * 
     * <p>Example:</p>
     * <pre>
     * String hash = DigestUtil.sha256Hex("password" + salt);
     * </pre>
     * 
     * @since 1.4
     */
    public static String sha256Hex(final byte[] data) {
        return Hex.encodeToString(sha256(data));
    }

    /**
     * Calculates the SHA-256 digest of stream data and returns it as a hex string.
     *
     * @param data The InputStream to read and digest
     * @return SHA-256 digest as a hex string
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.4
     */
    public static String sha256Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha256(data));
    }

    /**
     * Calculates the SHA-256 digest of a string and returns it as a hex string.
     *
     * @param data The string to digest
     * @return SHA-256 digest as a hex string
     * 
     * @since 1.4
     */
    public static String sha256Hex(final String data) {
        return Hex.encodeToString(sha256(data));
    }

    /**
     * Calculates the SHA3-224 digest of the input data.
     * SHA-3 is an alternative to SHA-2 with different internal structure.
     *
     * @param data The data to digest
     * @return SHA3-224 digest as a byte array
     * 
     * @since 1.12
     */
    public static byte[] sha3_224(final byte[] data) {
        return getSha3_224Digest().digest(data);
    }

    /**
     * Calculates the SHA3-224 digest of data from an InputStream.
     *
     * @param data The InputStream to read and digest
     * @return SHA3-224 digest as a byte array
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.12
     */
    public static byte[] sha3_224(final InputStream data) throws IOException {
        return digest(getSha3_224Digest(), data);
    }

    /**
     * Calculates the SHA3-224 digest of a string (converted to UTF-8 bytes).
     *
     * @param data The string to digest
     * @return SHA3-224 digest as a byte array
     * 
     * @since 1.12
     */
    public static byte[] sha3_224(final String data) {
        return sha3_224(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA3-224 digest and returns it as a hexadecimal string.
     *
     * @param data The data to digest
     * @return SHA3-224 digest as a hex string
     * 
     * @since 1.12
     */
    public static String sha3_224Hex(final byte[] data) {
        return Hex.encodeToString(sha3_224(data));
    }

    /**
     * Calculates the SHA3-224 digest of stream data and returns it as a hex string.
     *
     * @param data The InputStream to read and digest
     * @return SHA3-224 digest as a hex string
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.12
     */
    public static String sha3_224Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha3_224(data));
    }

    /**
     * Calculates the SHA3-224 digest of a string and returns it as a hex string.
     *
     * @param data The string to digest
     * @return SHA3-224 digest as a hex string
     * 
     * @since 1.12
     */
    public static String sha3_224Hex(final String data) {
        return Hex.encodeToString(sha3_224(data));
    }

    /**
     * Calculates the SHA3-256 digest of the input data.
     *
     * @param data The data to digest
     * @return SHA3-256 digest as a byte array
     * 
     * @since 1.12
     */
    public static byte[] sha3_256(final byte[] data) {
        return getSha3_256Digest().digest(data);
    }

    /**
     * Calculates the SHA3-256 digest of data from an InputStream.
     *
     * @param data The InputStream to read and digest
     * @return SHA3-256 digest as a byte array
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.12
     */
    public static byte[] sha3_256(final InputStream data) throws IOException {
        return digest(getSha3_256Digest(), data);
    }

    /**
     * Calculates the SHA3-256 digest of a string (converted to UTF-8 bytes).
     *
     * @param data The string to digest
     * @return SHA3-256 digest as a byte array
     * 
     * @since 1.12
     */
    public static byte[] sha3_256(final String data) {
        return sha3_256(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA3-256 digest and returns it as a hexadecimal string.
     *
     * @param data The data to digest
     * @return SHA3-256 digest as a hex string
     * 
     * @since 1.12
     */
    public static String sha3_256Hex(final byte[] data) {
        return Hex.encodeToString(sha3_256(data));
    }

    /**
     * Calculates the SHA3-256 digest of stream data and returns it as a hex string.
     *
     * @param data The InputStream to read and digest
     * @return SHA3-256 digest as a hex string
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.12
     */
    public static String sha3_256Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha3_256(data));
    }

    /**
     * Calculates the SHA3-256 digest of a string and returns it as a hex string.
     *
     * @param data The string to digest
     * @return SHA3-256 digest as a hex string
     * 
     * @since 1.12
     */
    public static String sha3_256Hex(final String data) {
        return Hex.encodeToString(sha3_256(data));
    }

    /**
     * Calculates the SHA3-384 digest of the input data.
     *
     * @param data The data to digest
     * @return SHA3-384 digest as a byte array
     * 
     * @since 1.12
     */
    public static byte[] sha3_384(final byte[] data) {
        return getSha3_384Digest().digest(data);
    }

    /**
     * Calculates the SHA3-384 digest of data from an InputStream.
     *
     * @param data The InputStream to read and digest
     * @return SHA3-384 digest as a byte array
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.12
     */
    public static byte[] sha3_384(final InputStream data) throws IOException {
        return digest(getSha3_384Digest(), data);
    }

    /**
     * Calculates the SHA3-384 digest of a string (converted to UTF-8 bytes).
     *
     * @param data The string to digest
     * @return SHA3-384 digest as a byte array
     * 
     * @since 1.12
     */
    public static byte[] sha3_384(final String data) {
        return sha3_384(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA3-384 digest and returns it as a hexadecimal string.
     *
     * @param data The data to digest
     * @return SHA3-384 digest as a hex string
     * 
     * @since 1.12
     */
    public static String sha3_384Hex(final byte[] data) {
        return Hex.encodeToString(sha3_384(data));
    }

    /**
     * Calculates the SHA3-384 digest of stream data and returns it as a hex string.
     *
     * @param data The InputStream to read and digest
     * @return SHA3-384 digest as a hex string
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.12
     */
    public static String sha3_384Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha3_384(data));
    }

    /**
     * Calculates the SHA3-384 digest of a string and returns it as a hex string.
     *
     * @param data The string to digest
     * @return SHA3-384 digest as a hex string
     * 
     * @since 1.12
     */
    public static String sha3_384Hex(final String data) {
        return Hex.encodeToString(sha3_384(data));
    }

    /**
     * Calculates the SHA3-512 digest of the input data.
     * Provides the highest security level in the SHA-3 family.
     *
     * @param data The data to digest
     * @return SHA3-512 digest as a byte array
     * 
     * @since 1.12
     */
    public static byte[] sha3_512(final byte[] data) {
        return getSha3_512Digest().digest(data);
    }

    /**
     * Calculates the SHA3-512 digest of data from an InputStream.
     *
     * @param data The InputStream to read and digest
     * @return SHA3-512 digest as a byte array
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.12
     */
    public static byte[] sha3_512(final InputStream data) throws IOException {
        return digest(getSha3_512Digest(), data);
    }

    /**
     * Calculates the SHA3-512 digest of a string (converted to UTF-8 bytes).
     *
     * @param data The string to digest
     * @return SHA3-512 digest as a byte array
     * 
     * @since 1.12
     */
    public static byte[] sha3_512(final String data) {
        return sha3_512(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA3-512 digest and returns it as a hexadecimal string.
     *
     * @param data The data to digest
     * @return SHA3-512 digest as a hex string
     * 
     * @since 1.12
     */
    public static String sha3_512Hex(final byte[] data) {
        return Hex.encodeToString(sha3_512(data));
    }

    /**
     * Calculates the SHA3-512 digest of stream data and returns it as a hex string.
     *
     * @param data The InputStream to read and digest
     * @return SHA3-512 digest as a hex string
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.12
     */
    public static String sha3_512Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha3_512(data));
    }

    /**
     * Calculates the SHA3-512 digest of a string and returns it as a hex string.
     *
     * @param data The string to digest
     * @return SHA3-512 digest as a hex string
     * 
     * @since 1.12
     */
    public static String sha3_512Hex(final String data) {
        return Hex.encodeToString(sha3_512(data));
    }

    /**
     * Calculates the SHA-384 digest of the input data and returns it as a 48-byte array.
     *
     * @param data The data to digest
     * @return SHA-384 digest as a 48-byte array
     * 
     * @since 1.4
     */
    public static byte[] sha384(final byte[] data) {
        return getSha384Digest().digest(data);
    }

    /**
     * Calculates the SHA-384 digest of data from an InputStream.
     *
     * @param data The InputStream to read and digest
     * @return SHA-384 digest as a 48-byte array
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.4
     */
    public static byte[] sha384(final InputStream data) throws IOException {
        return digest(getSha384Digest(), data);
    }

    /**
     * Calculates the SHA-384 digest of a string (converted to UTF-8 bytes).
     *
     * @param data The string to digest
     * @return SHA-384 digest as a 48-byte array
     * 
     * @since 1.4
     */
    public static byte[] sha384(final String data) {
        return sha384(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA-384 digest and returns it as a 96-character hexadecimal string.
     *
     * @param data The data to digest
     * @return SHA-384 digest as a hex string
     * 
     * @since 1.4
     */
    public static String sha384Hex(final byte[] data) {
        return Hex.encodeToString(sha384(data));
    }

    /**
     * Calculates the SHA-384 digest of stream data and returns it as a hex string.
     *
     * @param data The InputStream to read and digest
     * @return SHA-384 digest as a hex string
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.4
     */
    public static String sha384Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha384(data));
    }

    /**
     * Calculates the SHA-384 digest of a string and returns it as a hex string.
     *
     * @param data The string to digest
     * @return SHA-384 digest as a hex string
     * 
     * @since 1.4
     */
    public static String sha384Hex(final String data) {
        return Hex.encodeToString(sha384(data));
    }

    /**
     * Calculates the SHA-512 digest of the input data and returns it as a 64-byte array.
     * Provides maximum security in the SHA-2 family.
     *
     * @param data The data to digest
     * @return SHA-512 digest as a 64-byte array
     * 
     * @since 1.4
     */
    public static byte[] sha512(final byte[] data) {
        return getSha512Digest().digest(data);
    }

    /**
     * Calculates the SHA-512 digest of data from an InputStream.
     *
     * @param data The InputStream to read and digest
     * @return SHA-512 digest as a 64-byte array
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.4
     */
    public static byte[] sha512(final InputStream data) throws IOException {
        return digest(getSha512Digest(), data);
    }

    /**
     * Calculates the SHA-512 digest of a string (converted to UTF-8 bytes).
     *
     * @param data The string to digest
     * @return SHA-512 digest as a 64-byte array
     * 
     * @since 1.4
     */
    public static byte[] sha512(final String data) {
        return sha512(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA-512/224 digest of the input data.
     * This is a truncated variant of SHA-512.
     *
     * @param data The data to digest
     * @return SHA-512/224 digest as a byte array
     * 
     * @since 1.14
     */
    public static byte[] sha512_224(final byte[] data) {
        return getSha512_224Digest().digest(data);
    }

    /**
     * Calculates the SHA-512/224 digest of data from an InputStream.
     *
     * @param data The InputStream to read and digest
     * @return SHA-512/224 digest as a byte array
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.14
     */
    public static byte[] sha512_224(final InputStream data) throws IOException {
        return digest(getSha512_224Digest(), data);
    }

    /**
     * Calculates the SHA-512/224 digest of a string (converted to UTF-8 bytes).
     *
     * @param data The string to digest
     * @return SHA-512/224 digest as a byte array
     * 
     * @since 1.14
     */
    public static byte[] sha512_224(final String data) {
        return sha512_224(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA-512/224 digest and returns it as a hexadecimal string.
     *
     * @param data The data to digest
     * @return SHA-512/224 digest as a hex string
     * 
     * @since 1.14
     */
    public static String sha512_224Hex(final byte[] data) {
        return Hex.encodeToString(sha512_224(data));
    }

    /**
     * Calculates the SHA-512/224 digest of stream data and returns it as a hex string.
     *
     * @param data The InputStream to read and digest
     * @return SHA-512/224 digest as a hex string
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.14
     */
    public static String sha512_224Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha512_224(data));
    }

    /**
     * Calculates the SHA-512/224 digest of a string and returns it as a hex string.
     *
     * @param data The string to digest
     * @return SHA-512/224 digest as a hex string
     * 
     * @since 1.14
     */
    public static String sha512_224Hex(final String data) {
        return Hex.encodeToString(sha512_224(data));
    }

    /**
     * Calculates the SHA-512/256 digest of the input data.
     * This truncated variant offers better 64-bit performance than SHA-256.
     *
     * @param data The data to digest
     * @return SHA-512/256 digest as a byte array
     * 
     * @since 1.14
     */
    public static byte[] sha512_256(final byte[] data) {
        return getSha512_256Digest().digest(data);
    }

    /**
     * Calculates the SHA-512/256 digest of data from an InputStream.
     *
     * @param data The InputStream to read and digest
     * @return SHA-512/256 digest as a byte array
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.14
     */
    public static byte[] sha512_256(final InputStream data) throws IOException {
        return digest(getSha512_256Digest(), data);
    }

    /**
     * Calculates the SHA-512/256 digest of a string (converted to UTF-8 bytes).
     *
     * @param data The string to digest
     * @return SHA-512/224 digest as a byte array
     * 
     * @since 1.14
     */
    public static byte[] sha512_256(final String data) {
        return sha512_256(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA-512/256 digest and returns it as a hexadecimal string.
     *
     * @param data The data to digest
     * @return SHA-512/256 digest as a hex string
     * 
     * @since 1.14
     */
    public static String sha512_256Hex(final byte[] data) {
        return Hex.encodeToString(sha512_256(data));
    }

    /**
     * Calculates the SHA-512/256 digest of stream data and returns it as a hex string.
     *
     * @param data The InputStream to read and digest
     * @return SHA-512/256 digest as a hex string
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.14
     */
    public static String sha512_256Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha512_256(data));
    }

    /**
     * Calculates the SHA-512/256 digest of a string and returns it as a hex string.
     *
     * @param data The string to digest
     * @return SHA-512/256 digest as a hex string
     * 
     * @since 1.14
     */
    public static String sha512_256Hex(final String data) {
        return Hex.encodeToString(sha512_256(data));
    }

    /**
     * Calculates the SHA-512 digest and returns it as a 128-character hexadecimal string.
     *
     * @param data The data to digest
     * @return SHA-512 digest as a hex string
     * 
     * <p>Example:</p>
     * <pre>
     * String hash = DigestUtil.sha512Hex("critical data".getBytes());
     * </pre>
     * 
     * @since 1.4
     */
    public static String sha512Hex(final byte[] data) {
        return Hex.encodeToString(sha512(data));
    }

    /**
     * Calculates the SHA-512 digest of stream data and returns it as a hex string.
     *
     * @param data The InputStream to read and digest
     * @return SHA-512 digest as a hex string
     * @throws IOException If an error occurs while reading the stream
     * 
     * @since 1.4
     */
    public static String sha512Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha512(data));
    }

    /**
     * Calculates the SHA-512 digest of a string and returns it as a hex string.
     *
     * @param data The string to digest
     * @return SHA-512 digest as a hex string
     * 
     * @since 1.4
     */
    public static String sha512Hex(final String data) {
        return Hex.encodeToString(sha512(data));
    }

    /**
     * Calculates the SHA-1 digest and returns it as a hexadecimal string.
     *
     * @param data The data to digest
     * @return SHA-1 digest as a hex string
     * @deprecated Use {@link #sha1Hex(byte[])} instead
     */
    @Deprecated
    public static String shaHex(final byte[] data) {
        return sha1Hex(data);
    }

    /**
     * Calculates the SHA-1 digest of stream data and returns it as a hex string.
     *
     * @param data The InputStream to read and digest
     * @return SHA-1 digest as a hex string
     * @throws IOException If an error occurs while reading the stream
     * @deprecated Use {@link #sha1Hex(InputStream)} instead
     * @since 1.4
     */
    @Deprecated
    public static String shaHex(final InputStream data) throws IOException {
        return sha1Hex(data);
    }

    /**
     * Calculates the SHA-1 digest of a string and returns it as a hex string.
     *
     * @param data The string to digest
     * @return SHA-1 digest as a hex string
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
     * @param messageDigest The MessageDigest to update
     * @param valueToDigest The byte array to add to the digest
     * @return The updated MessageDigest (same instance as input)
     * 
     * <p>Example:</p>
     * <pre>
     * MessageDigest md = DigestUtil.getSha256Digest();
     * DigestUtil.updateDigest(md, part1);
     * DigestUtil.updateDigest(md, part2);
     * byte[] finalHash = md.digest();
     * </pre>
     * 
     * @since 1.7
     */
    public static MessageDigest updateDigest(final MessageDigest messageDigest, final byte[] valueToDigest) {
        messageDigest.update(valueToDigest);
        return messageDigest;
    }

    /**
     * Updates the given MessageDigest with data from a ByteBuffer.
     * The ByteBuffer's position will be advanced to its limit.
     *
     * @param messageDigest The MessageDigest to update
     * @param valueToDigest The ByteBuffer containing data to add to the digest
     * @return The updated MessageDigest (same instance as input)
     * 
     * @since 1.11
     */
    public static MessageDigest updateDigest(final MessageDigest messageDigest, final ByteBuffer valueToDigest) {
        messageDigest.update(valueToDigest);
        return messageDigest;
    }

    /**
     * Reads through a File and updates the MessageDigest with its contents.
     * The file is read using a buffered stream for efficiency.
     *
     * @param digest The MessageDigest to update
     * @param data The File to read
     * @return The updated MessageDigest (same instance as input)
     * @throws IOException If an error occurs while reading the file
     * 
     * @since 1.11
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
     * @param digest The MessageDigest to update
     * @param data The FileChannel to read from
     * @return The updated MessageDigest
     * @throws IOException If an error occurs while reading
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
     * @param digest The MessageDigest to update
     * @param inputStream The InputStream to read
     * @return The updated MessageDigest (same instance as input)
     * @throws IOException If an error occurs while reading the stream
     * 
     * <p>Example:</p>
     * <pre>
     * MessageDigest md = DigestUtil.getMd5Digest();
     * try (InputStream is = new FileInputStream("large.dat")) {
     *     DigestUtil.updateDigest(md, is);
     * }
     * byte[] hash = md.digest();
     * </pre>
     * 
     * @since 1.8
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
     * @param digest The MessageDigest to update
     * @param path The Path to the file to read
     * @param options Options for opening the file
     * @return The updated MessageDigest (same instance as input)
     * @throws IOException If an error occurs while reading the file
     * 
     * @since 1.14
     */
    public static MessageDigest updateDigest(final MessageDigest digest, final Path path, final OpenOption... options) throws IOException {
        try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(path, options))) {
            return updateDigest(digest, inputStream);
        }
    }

    /**
     * Reads through a RandomAccessFile using NIO and updates the MessageDigest.
     * This method is efficient for large files as it uses FileChannel.
     *
     * @param digest The MessageDigest to update
     * @param data The RandomAccessFile to read
     * @return The updated MessageDigest (same instance as input)
     * @throws IOException If an error occurs while reading
     * 
     * @since 1.14
     */
    public static MessageDigest updateDigest(final MessageDigest digest, final RandomAccessFile data) throws IOException {
        return updateDigest(digest, data.getChannel());
    }

    /**
     * Updates the given MessageDigest with a string value (converted to UTF-8 bytes).
     * This is a convenience method for updating a digest with text data.
     *
     * @param messageDigest The MessageDigest to update
     * @param valueToDigest The string value to add to the digest
     * @return The updated MessageDigest (same instance as input)
     * 
     * <p>Example:</p>
     * <pre>
     * MessageDigest md = DigestUtil.getSha256Digest();
     * DigestUtil.updateDigest(md, "Hello ");
     * DigestUtil.updateDigest(md, "World");
     * String hash = Hex.encodeToString(md.digest());
     * </pre>
     * 
     * @since 1.7
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
         * @since 1.11
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
         * @since 1.14
         */
        public static final String SHA_512_224 = "SHA-512/224";

        /**
         * The SHA-512/256 hash algorithm defined in FIPS PUB 180-4.
         * A truncated variant of SHA-512. Included starting in Oracle Java 9.
         * 
         * @since 1.14
         */
        public static final String SHA_512_256 = "SHA-512/256";

        /**
         * The SHA3-224 hash algorithm defined in FIPS PUB 202.
         * Part of the SHA-3 family. Included starting in Oracle Java 9.
         * 
         * @since 1.11
         */
        public static final String SHA3_224 = "SHA3-224";

        /**
         * The SHA3-256 hash algorithm defined in FIPS PUB 202.
         * Part of the SHA-3 family. Included starting in Oracle Java 9.
         * 
         * @since 1.11
         */
        public static final String SHA3_256 = "SHA3-256";

        /**
         * The SHA3-384 hash algorithm defined in FIPS PUB 202.
         * Part of the SHA-3 family. Included starting in Oracle Java 9.
         * 
         * @since 1.11
         */
        public static final String SHA3_384 = "SHA3-384";

        /**
         * The SHA3-512 hash algorithm defined in FIPS PUB 202.
         * Part of the SHA-3 family. Included starting in Oracle Java 9.
         * 
         * @since 1.11
         */
        public static final String SHA3_512 = "SHA3-512";

        /**
         * Returns an array containing all MessageDigest algorithm constants defined in this class.
         * This can be useful for testing algorithm availability or iterating through algorithms.
         *
         * @return An array of all algorithm name constants
         * 
         * <p>Example:</p>
         * <pre>
         * for (String algorithm : MessageDigestAlgorithms.values()) {
         *     if (DigestUtil.isAvailable(algorithm)) {
         *         System.out.println(algorithm + " is available");
         *     }
         * }
         * </pre>
         * 
         * @since 1.11
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