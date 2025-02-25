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
 * Note: copied from <a href="https://commons.apache.org/proper/commons-codec">Apache commons-codec</a>.
 * <p />
 *
 * Operations to simplify common {@link java.security.MessageDigest} tasks. This class is immutable and thread-safe. However the MessageDigest instances it
 * creates generally won't be.
 * <p>
 * The {@link MessageDigestAlgorithms} class provides constants for standard digest algorithms that can be used with the {@link #getDigest(String)} method and
 * other methods that require the Digest algorithm name.
 * </p>
 * <p>
 * Note: The class has shorthand methods for all the algorithms present as standard in Java 6. This approach requires lots of methods for each algorithm, and
 * quickly becomes unwieldy. The following code works with all algorithms:
 * </p>
 *
 * <pre>
 * import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_224;
 * ...
 * byte [] digest = new DigestUtils(SHA_224).digest(dataToDigest);
 * String hdigest = new DigestUtils(SHA_224).digestAsHex(new File("pom.xml"));
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
     * Reads through a byte array and returns the digest for the data. Provided for symmetry with other methods.
     *
     * @param messageDigest The MessageDigest to use (for example MD5)
     * @param data          Data to digest
     * @return the digest
     * @since 1.11
     */
    public static byte[] digest(final MessageDigest messageDigest, final byte[] data) {
        return messageDigest.digest(data);
    }

    /**
     * Reads through a ByteBuffer and returns the digest for the data
     *
     * @param messageDigest The MessageDigest to use (for example MD5)
     * @param data          Data to digest
     * @return the digest
     * @since 1.11
     */
    public static byte[] digest(final MessageDigest messageDigest, final ByteBuffer data) {
        messageDigest.update(data);
        return messageDigest.digest();
    }

    /**
     * Reads through a File and returns the digest for the data
     *
     * @param messageDigest The MessageDigest to use (for example MD5)
     * @param data          Data to digest
     * @return the digest
     * @throws IOException On error reading from the stream
     * @since 1.11
     */
    public static byte[] digest(final MessageDigest messageDigest, final File data) throws IOException {
        return updateDigest(messageDigest, data).digest();
    }

    /**
     * Reads through an InputStream and returns the digest for the data
     *
     * @param messageDigest The MessageDigest to use (for example MD5)
     * @param data          Data to digest
     * @return the digest
     * @throws IOException On error reading from the stream
     * @since 1.11 (was private)
     */
    public static byte[] digest(final MessageDigest messageDigest, final InputStream data) throws IOException {
        return updateDigest(messageDigest, data).digest();
    }

    /**
     * Reads through a File and returns the digest for the data
     *
     * @param messageDigest The MessageDigest to use (for example MD5)
     * @param data          Data to digest
     * @param options       options How to open the file
     * @return the digest
     * @throws IOException On error reading from the stream
     * @since 1.14
     */
    public static byte[] digest(final MessageDigest messageDigest, final Path data, final OpenOption... options) throws IOException {
        return updateDigest(messageDigest, data, options).digest();
    }

    /**
     * Reads through a RandomAccessFile using non-blocking-io (NIO) and returns the digest for the data
     *
     * @param messageDigest The MessageDigest to use (for example MD5)
     * @param data          Data to digest
     * @return the digest
     * @throws IOException On error reading from the stream
     * @since 1.14
     */
    public static byte[] digest(final MessageDigest messageDigest, final RandomAccessFile data) throws IOException {
        return updateDigest(messageDigest, data).digest();
    }

    /**
     * Gets a {@code MessageDigest} for the given {@code algorithm}.
     *
     * @param algorithm the name of the algorithm requested. See
     *                  <a href="https://docs.oracle.com/javase/6/docs/technotes/guides/security/crypto/CryptoSpec.html#AppA">Appendix A in the Java
     *                  Cryptography Architecture Reference Guide</a> for information about standard algorithm names.
     * @return A digest instance.
     * @see MessageDigest#getInstance(String)
     * @throws IllegalArgumentException when a {@link NoSuchAlgorithmException} is caught.
     */
    public static MessageDigest getDigest(final String algorithm) {
        try {
            return getMessageDigest(algorithm);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Gets a {@code MessageDigest} for the given {@code algorithm} or a default if there is a problem getting the algorithm.
     *
     * @param algorithm            the name of the algorithm requested. See
     *                             <a href="https://docs.oracle.com/javase/6/docs/technotes/guides/security/crypto/CryptoSpec.html#AppA"> Appendix A in the Java
     *                             Cryptography Architecture Reference Guide</a> for information about standard algorithm names.
     * @param defaultMessageDigest The default MessageDigest.
     * @return A digest instance.
     * @see MessageDigest#getInstance(String)
     * @throws IllegalArgumentException when a {@link NoSuchAlgorithmException} is caught.
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
     * Gets a {@code MessageDigest} for the given {@code algorithm}.
     *
     * @param algorithm the name of the algorithm requested. See
     *                  <a href="https://docs.oracle.com/javase/6/docs/technotes/guides/security/crypto/CryptoSpec.html#AppA"> Appendix A in the Java
     *                  Cryptography Architecture Reference Guide</a> for information about standard algorithm names.
     * @return A digest instance.
     * @see MessageDigest#getInstance(String)
     * @throws NoSuchAlgorithmException if no Provider supports a MessageDigestSpi implementation for the specified algorithm.
     */
    private static MessageDigest getMessageDigest(final String algorithm) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(algorithm);
    }

    /**
     * Gets an MD2 MessageDigest.
     *
     * @return An MD2 digest instance.
     * @throws IllegalArgumentException when a {@link NoSuchAlgorithmException} is caught, which should never happen because MD2 is a built-in algorithm
     * @see MessageDigestAlgorithms#MD2
     * @since 1.7
     */
    public static MessageDigest getMd2Digest() {
        return getDigest(MessageDigestAlgorithms.MD2);
    }

    /**
     * Gets an MD5 MessageDigest.
     *
     * @return An MD5 digest instance.
     * @throws IllegalArgumentException when a {@link NoSuchAlgorithmException} is caught, which should never happen because MD5 is a built-in algorithm
     * @see MessageDigestAlgorithms#MD5
     */
    public static MessageDigest getMd5Digest() {
        return getDigest(MessageDigestAlgorithms.MD5);
    }

    /**
     * Gets an SHA-1 digest.
     *
     * @return An SHA-1 digest instance.
     * @throws IllegalArgumentException when a {@link NoSuchAlgorithmException} is caught, which should never happen because SHA-1 is a built-in algorithm
     * @see MessageDigestAlgorithms#SHA_1
     * @since 1.7
     */
    public static MessageDigest getSha1Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_1);
    }

    /**
     * Gets an SHA-256 digest.
     *
     * @return An SHA-256 digest instance.
     * @throws IllegalArgumentException when a {@link NoSuchAlgorithmException} is caught, which should never happen because SHA-256 is a built-in algorithm
     * @see MessageDigestAlgorithms#SHA_256
     */
    public static MessageDigest getSha256Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_256);
    }

    /**
     * Gets an SHA3-224 digest.
     *
     * @return An SHA3-224 digest instance.
     * @throws IllegalArgumentException when a {@link NoSuchAlgorithmException} is caught, which should not happen on Oracle Java 9 and greater.
     * @see MessageDigestAlgorithms#SHA3_224
     * @since 1.12
     */
    public static MessageDigest getSha3_224Digest() {
        return getDigest(MessageDigestAlgorithms.SHA3_224);
    }

    /**
     * Returns an SHA3-256 digest.
     *
     * @return An SHA3-256 digest instance.
     * @throws IllegalArgumentException when a {@link NoSuchAlgorithmException} is caught, which should not happen on Oracle Java 9 and greater.
     * @see MessageDigestAlgorithms#SHA3_256
     * @since 1.12
     */
    public static MessageDigest getSha3_256Digest() {
        return getDigest(MessageDigestAlgorithms.SHA3_256);
    }

    /**
     * Gets an SHA3-384 digest.
     *
     * @return An SHA3-384 digest instance.
     * @throws IllegalArgumentException when a {@link NoSuchAlgorithmException} is caught, which should not happen on Oracle Java 9 and greater.
     * @see MessageDigestAlgorithms#SHA3_384
     * @since 1.12
     */
    public static MessageDigest getSha3_384Digest() {
        return getDigest(MessageDigestAlgorithms.SHA3_384);
    }

    /**
     * Gets an SHA3-512 digest.
     *
     * @return An SHA3-512 digest instance.
     * @throws IllegalArgumentException when a {@link NoSuchAlgorithmException} is caught, which should not happen on Oracle Java 9 and greater.
     * @see MessageDigestAlgorithms#SHA3_512
     * @since 1.12
     */
    public static MessageDigest getSha3_512Digest() {
        return getDigest(MessageDigestAlgorithms.SHA3_512);
    }

    /**
     * Gets an SHA-384 digest.
     *
     * @return An SHA-384 digest instance.
     * @throws IllegalArgumentException when a {@link NoSuchAlgorithmException} is caught, which should never happen because SHA-384 is a built-in algorithm
     * @see MessageDigestAlgorithms#SHA_384
     */
    public static MessageDigest getSha384Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_384);
    }

    /**
     * Gets an SHA-512/224 digest.
     *
     * @return An SHA-512/224 digest instance.
     * @throws IllegalArgumentException when a {@link NoSuchAlgorithmException} is caught.
     * @see MessageDigestAlgorithms#SHA_512_224
     */
    public static MessageDigest getSha512_224Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_512_224);
    }

    /**
     * Gets an SHA-512/256 digest.
     *
     * @return An SHA-512/256 digest instance.
     * @throws IllegalArgumentException when a {@link NoSuchAlgorithmException} is caught.
     * @see MessageDigestAlgorithms#SHA_512_224
     */
    public static MessageDigest getSha512_256Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_512_256);
    }

    /**
     * Gets an SHA-512 digest.
     *
     * @return An SHA-512 digest instance.
     * @throws IllegalArgumentException when a {@link NoSuchAlgorithmException} is caught, which should never happen because SHA-512 is a built-in algorithm
     * @see MessageDigestAlgorithms#SHA_512
     */
    public static MessageDigest getSha512Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_512);
    }

    /**
     * Gets an SHA-1 digest.
     *
     * @return An SHA-1 digest instance.
     * @throws IllegalArgumentException when a {@link NoSuchAlgorithmException} is caught
     * @deprecated (1.11) Use {@link #getSha1Digest()}
     */
    @Deprecated
    public static MessageDigest getShaDigest() {
        return getSha1Digest();
    }

    /**
     * Test whether the algorithm is supported.
     *
     * @param messageDigestAlgorithm the algorithm name
     * @return {@code true} if the algorithm can be found
     * @since 1.11
     */
    public static boolean isAvailable(final String messageDigestAlgorithm) {
        return getDigest(messageDigestAlgorithm, null) != null;
    }

    /**
     * Calculates the MD2 digest and returns the value as a 16 element {@code byte[]}.
     *
     * @param data Data to digest
     * @return MD2 digest
     * @since 1.7
     */
    public static byte[] md2(final byte[] data) {
        return getMd2Digest().digest(data);
    }

    /**
     * Calculates the MD2 digest and returns the value as a 16 element {@code byte[]}.
     *
     * @param data Data to digest
     * @return MD2 digest
     * @throws IOException On error reading from the stream
     * @since 1.7
     */
    public static byte[] md2(final InputStream data) throws IOException {
        return digest(getMd2Digest(), data);
    }

    /**
     * Calculates the MD2 digest and returns the value as a 16 element {@code byte[]}.
     *
     * @param data Data to digest; converted to bytes using {@link StringUtils#getBytesUtf8(String)}
     * @return MD2 digest
     * @since 1.7
     */
    public static byte[] md2(final String data) {
        return md2(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the MD2 digest and returns the value as a 32 character hexadecimal string.
     *
     * @param data Data to digest
     * @return MD2 digest as a hexadecimal string
     * @since 1.7
     */
    public static String md2Hex(final byte[] data) {
        return Hex.encodeToString(md2(data));
    }

    /**
     * Calculates the MD2 digest and returns the value as a 32 character hexadecimal string.
     *
     * @param data Data to digest
     * @return MD2 digest as a hexadecimal string
     * @throws IOException On error reading from the stream
     * @since 1.7
     */
    public static String md2Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(md2(data));
    }

    /**
     * Calculates the MD2 digest and returns the value as a 32 character hexadecimal string.
     *
     * @param data Data to digest
     * @return MD2 digest as a hexadecimal string
     * @since 1.7
     */
    public static String md2Hex(final String data) {
        return Hex.encodeToString(md2(data));
    }

    /**
     * Calculates the MD5 digest and returns the value as a 16 element {@code byte[]}.
     *
     * @param data Data to digest
     * @return MD5 digest
     */
    public static byte[] md5(final byte[] data) {
        return getMd5Digest().digest(data);
    }

    /**
     * Calculates the MD5 digest and returns the value as a 16 element {@code byte[]}.
     *
     * @param data Data to digest
     * @return MD5 digest
     * @throws IOException On error reading from the stream
     * @since 1.4
     */
    public static byte[] md5(final InputStream data) throws IOException {
        return digest(getMd5Digest(), data);
    }

    /**
     * Calculates the MD5 digest and returns the value as a 16 element {@code byte[]}.
     *
     * @param data Data to digest; converted to bytes using {@link StringUtils#getBytesUtf8(String)}
     * @return MD5 digest
     */
    public static byte[] md5(final String data) {
        return md5(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the MD5 digest and returns the value as a 32 character hexadecimal string.
     *
     * @param data Data to digest
     * @return MD5 digest as a hexadecimal string
     */
    public static String md5Hex(final byte[] data) {
        return Hex.encodeToString(md5(data));
    }

    /**
     * Calculates the MD5 digest and returns the value as a 32 character hexadecimal string.
     *
     * @param data Data to digest
     * @return MD5 digest as a hexadecimal string
     * @throws IOException On error reading from the stream
     * @since 1.4
     */
    public static String md5Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(md5(data));
    }

    /**
     * Calculates the MD5 digest and returns the value as a 32 character hexadecimal string.
     *
     * @param data Data to digest
     * @return MD5 digest as a hexadecimal string
     */
    public static String md5Hex(final String data) {
        return Hex.encodeToString(md5(data));
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA-1 digest
     * @deprecated (1.11) Use {@link #sha1(byte[])}
     */
    @Deprecated
    public static byte[] sha(final byte[] data) {
        return sha1(data);
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA-1 digest
     * @throws IOException On error reading from the stream
     * @since 1.4
     * @deprecated (1.11) Use {@link #sha1(InputStream)}
     */
    @Deprecated
    public static byte[] sha(final InputStream data) throws IOException {
        return sha1(data);
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA-1 digest
     * @deprecated (1.11) Use {@link #sha1(String)}
     */
    @Deprecated
    public static byte[] sha(final String data) {
        return sha1(data);
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA-1 digest
     * @since 1.7
     */
    public static byte[] sha1(final byte[] data) {
        return getSha1Digest().digest(data);
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA-1 digest
     * @throws IOException On error reading from the stream
     * @since 1.7
     */
    public static byte[] sha1(final InputStream data) throws IOException {
        return digest(getSha1Digest(), data);
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest; converted to bytes using {@link StringUtils#getBytesUtf8(String)}
     * @return SHA-1 digest
     */
    public static byte[] sha1(final String data) {
        return sha1(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-1 digest as a hexadecimal string
     * @since 1.7
     */
    public static String sha1Hex(final byte[] data) {
        return Hex.encodeToString(sha1(data));
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-1 digest as a hexadecimal string
     * @throws IOException On error reading from the stream
     * @since 1.7
     */
    public static String sha1Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha1(data));
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-1 digest as a hexadecimal string
     * @since 1.7
     */
    public static String sha1Hex(final String data) {
        return Hex.encodeToString(sha1(data));
    }

    /**
     * Calculates the SHA-256 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA-256 digest
     * @since 1.4
     */
    public static byte[] sha256(final byte[] data) {
        return getSha256Digest().digest(data);
    }

    /**
     * Calculates the SHA-256 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA-256 digest
     * @throws IOException On error reading from the stream
     * @since 1.4
     */
    public static byte[] sha256(final InputStream data) throws IOException {
        return digest(getSha256Digest(), data);
    }

    /**
     * Calculates the SHA-256 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest; converted to bytes using {@link StringUtils#getBytesUtf8(String)}
     * @return SHA-256 digest
     * @since 1.4
     */
    public static byte[] sha256(final String data) {
        return sha256(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA-256 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-256 digest as a hexadecimal string
     * @since 1.4
     */
    public static String sha256Hex(final byte[] data) {
        return Hex.encodeToString(sha256(data));
    }

    /**
     * Calculates the SHA-256 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-256 digest as a hexadecimal string
     * @throws IOException On error reading from the stream
     * @since 1.4
     */
    public static String sha256Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha256(data));
    }

    /**
     * Calculates the SHA-256 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-256 digest as a hexadecimal string
     * @since 1.4
     */
    public static String sha256Hex(final String data) {
        return Hex.encodeToString(sha256(data));
    }

    /**
     * Calculates the SHA3-224 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA3-224 digest
     * @since 1.12
     */
    public static byte[] sha3_224(final byte[] data) {
        return getSha3_224Digest().digest(data);
    }

    /**
     * Calculates the SHA3-224 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA3-224 digest
     * @throws IOException On error reading from the stream
     * @since 1.12
     */
    public static byte[] sha3_224(final InputStream data) throws IOException {
        return digest(getSha3_224Digest(), data);
    }

    /**
     * Calculates the SHA3-224 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest; converted to bytes using {@link StringUtils#getBytesUtf8(String)}
     * @return SHA3-224 digest
     * @since 1.12
     */
    public static byte[] sha3_224(final String data) {
        return sha3_224(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA3-224 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA3-224 digest as a hexadecimal string
     * @since 1.12
     */
    public static String sha3_224Hex(final byte[] data) {
        return Hex.encodeToString(sha3_224(data));
    }

    /**
     * Calculates the SHA3-224 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA3-224 digest as a hexadecimal string
     * @throws IOException On error reading from the stream
     * @since 1.12
     */
    public static String sha3_224Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha3_224(data));
    }

    /**
     * Calculates the SHA3-224 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA3-224 digest as a hexadecimal string
     * @since 1.12
     */
    public static String sha3_224Hex(final String data) {
        return Hex.encodeToString(sha3_224(data));
    }

    /**
     * Calculates the SHA3-256 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA3-256 digest
     * @since 1.12
     */
    public static byte[] sha3_256(final byte[] data) {
        return getSha3_256Digest().digest(data);
    }

    /**
     * Calculates the SHA3-256 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA3-256 digest
     * @throws IOException On error reading from the stream
     * @since 1.12
     */
    public static byte[] sha3_256(final InputStream data) throws IOException {
        return digest(getSha3_256Digest(), data);
    }

    /**
     * Calculates the SHA3-256 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest; converted to bytes using {@link StringUtils#getBytesUtf8(String)}
     * @return SHA3-256 digest
     * @since 1.12
     */
    public static byte[] sha3_256(final String data) {
        return sha3_256(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA3-256 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA3-256 digest as a hexadecimal string
     * @since 1.12
     */
    public static String sha3_256Hex(final byte[] data) {
        return Hex.encodeToString(sha3_256(data));
    }

    /**
     * Calculates the SHA3-256 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA3-256 digest as a hexadecimal string
     * @throws IOException On error reading from the stream
     * @since 1.12
     */
    public static String sha3_256Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha3_256(data));
    }

    /**
     * Calculates the SHA3-256 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA3-256 digest as a hexadecimal string
     * @since 1.12
     */
    public static String sha3_256Hex(final String data) {
        return Hex.encodeToString(sha3_256(data));
    }

    /**
     * Calculates the SHA3-384 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA3-384 digest
     * @since 1.12
     */
    public static byte[] sha3_384(final byte[] data) {
        return getSha3_384Digest().digest(data);
    }

    /**
     * Calculates the SHA3-384 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA3-384 digest
     * @throws IOException On error reading from the stream
     * @since 1.12
     */
    public static byte[] sha3_384(final InputStream data) throws IOException {
        return digest(getSha3_384Digest(), data);
    }

    /**
     * Calculates the SHA3-384 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest; converted to bytes using {@link StringUtils#getBytesUtf8(String)}
     * @return SHA3-384 digest
     * @since 1.12
     */
    public static byte[] sha3_384(final String data) {
        return sha3_384(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA3-384 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA3-384 digest as a hexadecimal string
     * @since 1.12
     */
    public static String sha3_384Hex(final byte[] data) {
        return Hex.encodeToString(sha3_384(data));
    }

    /**
     * Calculates the SHA3-384 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA3-384 digest as a hexadecimal string
     * @throws IOException On error reading from the stream
     * @since 1.12
     */
    public static String sha3_384Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha3_384(data));
    }

    /**
     * Calculates the SHA3-384 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA3-384 digest as a hexadecimal string
     * @since 1.12
     */
    public static String sha3_384Hex(final String data) {
        return Hex.encodeToString(sha3_384(data));
    }

    /**
     * Calculates the SHA3-512 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA3-512 digest
     * @since 1.12
     */
    public static byte[] sha3_512(final byte[] data) {
        return getSha3_512Digest().digest(data);
    }

    /**
     * Calculates the SHA3-512 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA3-512 digest
     * @throws IOException On error reading from the stream
     * @since 1.12
     */
    public static byte[] sha3_512(final InputStream data) throws IOException {
        return digest(getSha3_512Digest(), data);
    }

    /**
     * Calculates the SHA3-512 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest; converted to bytes using {@link StringUtils#getBytesUtf8(String)}
     * @return SHA3-512 digest
     * @since 1.12
     */
    public static byte[] sha3_512(final String data) {
        return sha3_512(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA3-512 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA3-512 digest as a hexadecimal string
     * @since 1.12
     */
    public static String sha3_512Hex(final byte[] data) {
        return Hex.encodeToString(sha3_512(data));
    }

    /**
     * Calculates the SHA3-512 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA3-512 digest as a hexadecimal string
     * @throws IOException On error reading from the stream
     * @since 1.12
     */
    public static String sha3_512Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha3_512(data));
    }

    /**
     * Calculates the SHA3-512 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA3-512 digest as a hexadecimal string
     * @since 1.12
     */
    public static String sha3_512Hex(final String data) {
        return Hex.encodeToString(sha3_512(data));
    }

    /**
     * Calculates the SHA-384 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA-384 digest
     * @since 1.4
     */
    public static byte[] sha384(final byte[] data) {
        return getSha384Digest().digest(data);
    }

    /**
     * Calculates the SHA-384 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA-384 digest
     * @throws IOException On error reading from the stream
     * @since 1.4
     */
    public static byte[] sha384(final InputStream data) throws IOException {
        return digest(getSha384Digest(), data);
    }

    /**
     * Calculates the SHA-384 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest; converted to bytes using {@link StringUtils#getBytesUtf8(String)}
     * @return SHA-384 digest
     * @since 1.4
     */
    public static byte[] sha384(final String data) {
        return sha384(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA-384 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-384 digest as a hexadecimal string
     * @since 1.4
     */
    public static String sha384Hex(final byte[] data) {
        return Hex.encodeToString(sha384(data));
    }

    /**
     * Calculates the SHA-384 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-384 digest as a hexadecimal string
     * @throws IOException On error reading from the stream
     * @since 1.4
     */
    public static String sha384Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha384(data));
    }

    /**
     * Calculates the SHA-384 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-384 digest as a hexadecimal string
     * @since 1.4
     */
    public static String sha384Hex(final String data) {
        return Hex.encodeToString(sha384(data));
    }

    /**
     * Calculates the SHA-512 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA-512 digest
     * @since 1.4
     */
    public static byte[] sha512(final byte[] data) {
        return getSha512Digest().digest(data);
    }

    /**
     * Calculates the SHA-512 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA-512 digest
     * @throws IOException On error reading from the stream
     * @since 1.4
     */
    public static byte[] sha512(final InputStream data) throws IOException {
        return digest(getSha512Digest(), data);
    }

    /**
     * Calculates the SHA-512 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest; converted to bytes using {@link StringUtils#getBytesUtf8(String)}
     * @return SHA-512 digest
     * @since 1.4
     */
    public static byte[] sha512(final String data) {
        return sha512(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA-512/224 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA-512/224 digest
     * @since 1.14
     */
    public static byte[] sha512_224(final byte[] data) {
        return getSha512_224Digest().digest(data);
    }

    /**
     * Calculates the SHA-512/224 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA-512/224 digest
     * @throws IOException On error reading from the stream
     * @since 1.14
     */
    public static byte[] sha512_224(final InputStream data) throws IOException {
        return digest(getSha512_224Digest(), data);
    }

    /**
     * Calculates the SHA-512/224 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest; converted to bytes using {@link StringUtils#getBytesUtf8(String)}
     * @return SHA-512/224 digest
     * @since 1.14
     */
    public static byte[] sha512_224(final String data) {
        return sha512_224(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA-512/224 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-512/224 digest as a hexadecimal string
     * @since 1.14
     */
    public static String sha512_224Hex(final byte[] data) {
        return Hex.encodeToString(sha512_224(data));
    }

    /**
     * Calculates the SHA-512/224 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-512/224 digest as a hexadecimal string
     * @throws IOException On error reading from the stream
     * @since 1.14
     */
    public static String sha512_224Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha512_224(data));
    }

    /**
     * Calculates the SHA-512/224 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-512/224 digest as a hexadecimal string
     * @since 1.14
     */
    public static String sha512_224Hex(final String data) {
        return Hex.encodeToString(sha512_224(data));
    }

    /**
     * Calculates the SHA-512/256 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA-512/256 digest
     * @since 1.14
     */
    public static byte[] sha512_256(final byte[] data) {
        return getSha512_256Digest().digest(data);
    }

    /**
     * Calculates the SHA-512/256 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest
     * @return SHA-512/256 digest
     * @throws IOException On error reading from the stream
     * @since 1.14
     */
    public static byte[] sha512_256(final InputStream data) throws IOException {
        return digest(getSha512_256Digest(), data);
    }

    /**
     * Calculates the SHA-512/256 digest and returns the value as a {@code byte[]}.
     *
     * @param data Data to digest; converted to bytes using {@link StringUtils#getBytesUtf8(String)}
     * @return SHA-512/224 digest
     * @since 1.14
     */
    public static byte[] sha512_256(final String data) {
        return sha512_256(Strings.getBytesUtf8(data));
    }

    /**
     * Calculates the SHA-512/256 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-512/256 digest as a hexadecimal string
     * @since 1.14
     */
    public static String sha512_256Hex(final byte[] data) {
        return Hex.encodeToString(sha512_256(data));
    }

    /**
     * Calculates the SHA-512/256 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-512/256 digest as a hexadecimal string
     * @throws IOException On error reading from the stream
     * @since 1.14
     */
    public static String sha512_256Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha512_256(data));
    }

    /**
     * Calculates the SHA-512/256 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-512/256 digest as a hexadecimal string
     * @since 1.14
     */
    public static String sha512_256Hex(final String data) {
        return Hex.encodeToString(sha512_256(data));
    }

    /**
     * Calculates the SHA-512 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-512 digest as a hexadecimal string
     * @since 1.4
     */
    public static String sha512Hex(final byte[] data) {
        return Hex.encodeToString(sha512(data));
    }

    /**
     * Calculates the SHA-512 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-512 digest as a hexadecimal string
     * @throws IOException On error reading from the stream
     * @since 1.4
     */
    public static String sha512Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha512(data));
    }

    /**
     * Calculates the SHA-512 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-512 digest as a hexadecimal string
     * @since 1.4
     */
    public static String sha512Hex(final String data) {
        return Hex.encodeToString(sha512(data));
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-1 digest as a hexadecimal string
     * @deprecated (1.11) Use {@link #sha1Hex(byte[])}
     */
    @Deprecated
    public static String shaHex(final byte[] data) {
        return sha1Hex(data);
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-1 digest as a hexadecimal string
     * @throws IOException On error reading from the stream
     * @since 1.4
     * @deprecated (1.11) Use {@link #sha1Hex(InputStream)}
     */
    @Deprecated
    public static String shaHex(final InputStream data) throws IOException {
        return sha1Hex(data);
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a hexadecimal string.
     *
     * @param data Data to digest
     * @return SHA-1 digest as a hexadecimal string
     * @deprecated (1.11) Use {@link #sha1Hex(String)}
     */
    @Deprecated
    public static String shaHex(final String data) {
        return sha1Hex(data);
    }

    /**
     * Updates the given {@link MessageDigest}.
     *
     * @param messageDigest the {@link MessageDigest} to update
     * @param valueToDigest the value to update the {@link MessageDigest} with
     * @return the updated {@link MessageDigest}
     * @since 1.7
     */
    public static MessageDigest updateDigest(final MessageDigest messageDigest, final byte[] valueToDigest) {
        messageDigest.update(valueToDigest);
        return messageDigest;
    }

    /**
     * Updates the given {@link MessageDigest}.
     *
     * @param messageDigest the {@link MessageDigest} to update
     * @param valueToDigest the value to update the {@link MessageDigest} with
     * @return the updated {@link MessageDigest}
     * @since 1.11
     */
    public static MessageDigest updateDigest(final MessageDigest messageDigest, final ByteBuffer valueToDigest) {
        messageDigest.update(valueToDigest);
        return messageDigest;
    }

    /**
     * Reads through a File and updates the digest for the data
     *
     * @param digest The MessageDigest to use (for example MD5)
     * @param data   Data to digest
     * @return the digest
     * @throws IOException On error reading from the stream
     * @since 1.11
     */
    public static MessageDigest updateDigest(final MessageDigest digest, final File data) throws IOException {
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(data))) {
            return updateDigest(digest, inputStream);
        }
    }

    /**
     * Reads through a RandomAccessFile and updates the digest for the data using non-blocking-io (NIO).
     *
     * TODO Decide if this should be public.
     *
     * @param digest The MessageDigest to use (for example MD5)
     * @param data   Data to digest
     * @return the digest
     * @throws IOException On error reading from the stream
     * @since 1.14
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
     * Reads through an InputStream and updates the digest for the data
     *
     * @param digest      The MessageDigest to use (for example MD5)
     * @param inputStream Data to digest
     * @return the digest
     * @throws IOException On error reading from the stream
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
     * Reads through a Path and updates the digest for the data
     *
     * @param digest  The MessageDigest to use (for example MD5)
     * @param path    Data to digest
     * @param options options How to open the file
     * @return the digest
     * @throws IOException On error reading from the stream
     * @since 1.14
     */
    public static MessageDigest updateDigest(final MessageDigest digest, final Path path, final OpenOption... options) throws IOException {
        try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(path, options))) {
            return updateDigest(digest, inputStream);
        }
    }

    /**
     * Reads through a RandomAccessFile and updates the digest for the data using non-blocking-io (NIO)
     *
     * @param digest The MessageDigest to use (for example MD5)
     * @param data   Data to digest
     * @return the digest
     * @throws IOException On error reading from the stream
     * @since 1.14
     */
    @SuppressWarnings("resource") // Closing RandomAccessFile closes the channel.
    public static MessageDigest updateDigest(final MessageDigest digest, final RandomAccessFile data) throws IOException {
        return updateDigest(digest, data.getChannel());
    }

    /**
     * Updates the given {@link MessageDigest} from a String (converted to bytes using UTF-8).
     * <p>
     * To update the digest using a different charset for the conversion, convert the String to a byte array using
     * {@link String#getBytes(java.nio.charset.Charset)} and pass that to the {@link DigestUtils#updateDigest(MessageDigest, byte[])} method
     *
     * @param messageDigest the {@link MessageDigest} to update
     * @param valueToDigest the value to update the {@link MessageDigest} with; converted to bytes using {@link StringUtils#getBytesUtf8(String)}
     * @return the updated {@link MessageDigest}
     * @since 1.7
     */
    public static MessageDigest updateDigest(final MessageDigest messageDigest, final String valueToDigest) {
        messageDigest.update(Strings.getBytesUtf8(valueToDigest));
        return messageDigest;
    }

    /**
     * Standard {@link MessageDigest} algorithm names from the <cite>Java Cryptography Architecture Standard Algorithm Name
     * Documentation</cite>.
     * <p>
     * This class is immutable and thread-safe.
     * </p>
     * TODO This should be an enum.
     *
     * @version $Id: MessageDigestAlgorithms.java 1585867 2014-04-09 00:12:36Z ggregory $
     * @see <a href="http://docs.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html">Java Cryptography
     *      Architecture Standard Algorithm Name Documentation</a>
     */
    static class MessageDigestAlgorithms {

        /**
         * The MD2 message digest algorithm defined in RFC 1319.
         */
        public static final String MD2 = "MD2";

        /**
         * The MD5 message digest algorithm defined in RFC 1321.
         */
        public static final String MD5 = "MD5";

        /**
         * The SHA-1 hash algorithm defined in the FIPS PUB 180-2.
         */
        public static final String SHA_1 = "SHA-1";

        /**
         * The SHA-224 hash algorithm defined in the FIPS PUB 180-3.
         * <p>
         * Present in Oracle Java 8.
         * </p>
         *
         * @since 1.11
         */
        public static final String SHA_224 = "SHA-224";

        /**
         * The SHA-256 hash algorithm defined in the FIPS PUB 180-2.
         */
        public static final String SHA_256 = "SHA-256";

        /**
         * The SHA-384 hash algorithm defined in the FIPS PUB 180-2.
         */
        public static final String SHA_384 = "SHA-384";

        /**
         * The SHA-512 hash algorithm defined in the FIPS PUB 180-2.
         */
        public static final String SHA_512 = "SHA-512";

        /**
         * The SHA-512 hash algorithm defined in the FIPS PUB 180-4.
         * <p>
         * Included starting in Oracle Java 9.
         * </p>
         *
         * @since 1.14
         */
        public static final String SHA_512_224 = "SHA-512/224";

        /**
         * The SHA-512 hash algorithm defined in the FIPS PUB 180-4.
         * <p>
         * Included starting in Oracle Java 9.
         * </p>
         *
         * @since 1.14
         */
        public static final String SHA_512_256 = "SHA-512/256";

        /**
         * The SHA3-224 hash algorithm defined in the FIPS PUB 202.
         * <p>
         * Included starting in Oracle Java 9.
         * </p>
         *
         * @since 1.11
         */
        public static final String SHA3_224 = "SHA3-224";

        /**
         * The SHA3-256 hash algorithm defined in the FIPS PUB 202.
         * <p>
         * Included starting in Oracle Java 9.
         * </p>
         *
         * @since 1.11
         */
        public static final String SHA3_256 = "SHA3-256";

        /**
         * The SHA3-384 hash algorithm defined in the FIPS PUB 202.
         * <p>
         * Included starting in Oracle Java 9.
         * </p>
         *
         * @since 1.11
         */
        public static final String SHA3_384 = "SHA3-384";

        /**
         * The SHA3-512 hash algorithm defined in the FIPS PUB 202.
         * <p>
         * Included starting in Oracle Java 9.
         * </p>
         *
         * @since 1.11
         */
        public static final String SHA3_512 = "SHA3-512";

        /**
         * Gets all constant values defined in this class.
         *
         * @return all constant values defined in this class.
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