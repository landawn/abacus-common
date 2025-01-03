/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Note: copied from <a href="https://commons.apache.org/proper/commons-codec">Apache commons-codec</a>.
 *
 * Operations to simplify common {@link MessageDigest} tasks.
 * This class is immutable and thread-safe.
 *
 * @version $Id: DigestUtils.java 1634433 2014-10-27 01:10:47Z ggregory $
 */
public final class DigestUtil {

    private static final int STREAM_BUFFER_LENGTH = 1024;

    private DigestUtil() {
        // Singleton for utility class
    }

    /**
     * Read through an InputStream and returns the digest for the data.
     *
     * @param digest The MessageDigest to use (e.g. MD5)
     * @param data Data to digest
     * @return
     * @throws IOException             On error reading from the stream
     */
    private static byte[] digest(final MessageDigest digest, final InputStream data) throws IOException {
        final byte[] buffer = new byte[STREAM_BUFFER_LENGTH];
        int read = data.read(buffer, 0, STREAM_BUFFER_LENGTH);

        while (read > -1) {
            digest.update(buffer, 0, read);
            read = data.read(buffer, 0, STREAM_BUFFER_LENGTH);
        }

        return digest.digest();
    }

    /**
     * Returns a {@code MessageDigest} for the given {@code algorithm}.
     *
     * @param algorithm the name of the algorithm requested. See <a
     *            href="http://docs.oracle.com/javase/6/docs/technotes/guides/security/crypto/CryptoSpec.html#AppA"
     *            >Appendix A in the Java Cryptography Architecture Reference Guide</a> for information about standard
     *            algorithm names.
     * @return A digest instance.
     * @throws IllegalArgumentException             when a {@link NoSuchAlgorithmException} is caught.
     * @see MessageDigest#getInstance(String)
     */
    public static MessageDigest getDigest(final String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns an MD2 MessageDigest.
     *
     * @return An MD2 digest instance.
     * @throws IllegalArgumentException
     *             when a {@link NoSuchAlgorithmException} is caught, which should never happen because MD2 is a
     *             built-in algorithm
     * @see MessageDigestAlgorithms#MD2
     */
    public static MessageDigest getMd2Digest() {
        return getDigest(MessageDigestAlgorithms.MD2);
    }

    /**
     * Returns an MD5 MessageDigest.
     *
     * @return An MD5 digest instance.
     * @throws IllegalArgumentException
     *             when a {@link NoSuchAlgorithmException} is caught, which should never happen because MD5 is a
     *             built-in algorithm
     * @see MessageDigestAlgorithms#MD5
     */
    public static MessageDigest getMd5Digest() {
        return getDigest(MessageDigestAlgorithms.MD5);
    }

    /**
     * Returns an SHA-1 digest.
     *
     * @return An SHA-1 digest instance.
     * @throws IllegalArgumentException
     *             when a {@link NoSuchAlgorithmException} is caught, which should never happen because SHA-1 is a
     *             built-in algorithm
     * @see MessageDigestAlgorithms#SHA_1
     */
    public static MessageDigest getSha1Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_1);
    }

    /**
     * Returns an SHA-256 digest.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @return An SHA-256 digest instance.
     * @throws IllegalArgumentException
     *             when a {@link NoSuchAlgorithmException} is caught, which should never happen because SHA-256 is a
     *             built-in algorithm
     * @see MessageDigestAlgorithms#SHA_256
     */
    public static MessageDigest getSha256Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_256);
    }

    /**
     * Returns an SHA-384 digest.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @return An SHA-384 digest instance.
     * @throws IllegalArgumentException
     *             when a {@link NoSuchAlgorithmException} is caught, which should never happen because SHA-384 is a
     *             built-in algorithm
     * @see MessageDigestAlgorithms#SHA_384
     */
    public static MessageDigest getSha384Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_384);
    }

    /**
     * Returns an SHA-512 digest.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @return An SHA-512 digest instance.
     * @throws IllegalArgumentException
     *             when a {@link NoSuchAlgorithmException} is caught, which should never happen because SHA-512 is a
     *             built-in algorithm
     * @see MessageDigestAlgorithms#SHA_512
     */
    public static MessageDigest getSha512Digest() {
        return getDigest(MessageDigestAlgorithms.SHA_512);
    }

    /**
     * Calculates the MD2 digest and returns the value as a 16 element {@code byte[]}.
     *
     * @param data
     *            Data to digest
     * @return MD2 digest
     */
    public static byte[] md2(final byte[] data) {
        return getMd2Digest().digest(data);
    }

    /**
     * Calculates the MD2 digest and returns the value as a 16 element {@code byte[]}.
     *
     * @param data
     *            Data to digest
     * @return MD2 digest
     * @throws IOException
     *             On error reading from the stream
     */
    public static byte[] md2(final InputStream data) throws IOException {
        return digest(getMd2Digest(), data);
    }

    /**
     * Calculates the MD2 digest and returns the value as a 16 element {@code byte[]}.
     *
     * @param data
     *            Data to digest; converted to bytes using {@link Strings#getBytesUtf8(String)}
     * @return MD2 digest
     */
    public static byte[] md2(final String data) {
        return md2(getBytes(data));
    }

    /**
     * Calculates the MD2 digest and returns the value as a 32 character hex string.
     *
     * @param data
     *            Data to digest
     * @return MD2 digest as a hex string
     */
    public static String md2Hex(final byte[] data) {
        return Hex.encodeToString(md2(data));
    }

    /**
     * Calculates the MD2 digest and returns the value as a 32 character hex string.
     *
     * @param data
     *            Data to digest
     * @return MD2 digest as a hex string
     * @throws IOException
     *             On error reading from the stream
     */
    public static String md2Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(md2(data));
    }

    /**
     * Calculates the MD2 digest and returns the value as a 32 character hex string.
     *
     * @param data
     *            Data to digest
     * @return MD2 digest as a hex string
     */
    public static String md2Hex(final String data) {
        return Hex.encodeToString(md2(data));
    }

    /**
     * Calculates the MD2 digest and returns the value as a base64 encoded string.
     *
     * @param data
     *            Data to digest
     * @return MD2 digest as a hex string
     */
    public static String md2Base64(final byte[] data) {
        return Strings.base64Encode(md2(data));
    }

    /**
     * Calculates the MD2 digest and returns the value as a base64 encoded string.
     *
     * @param data
     *            Data to digest
     * @return MD2 digest as a hex string
     * @throws IOException
     *             On error reading from the stream
     */
    public static String md2Base64(final InputStream data) throws IOException {
        return Strings.base64Encode(md2(data));
    }

    /**
     * Calculates the MD2 digest and returns the value as a base64 encoded string.
     *
     * @param data
     *            Data to digest
     * @return MD2 digest as a hex string
     */
    public static String md2Base64(final String data) {
        return Strings.base64Encode(md2(data));
    }

    /**
     * Calculates the MD5 digest and returns the value as a 16 element {@code byte[]}.
     *
     * @param data
     *            Data to digest
     * @return MD5 digest
     */
    public static byte[] md5(final byte[] data) {
        return getMd5Digest().digest(data);
    }

    /**
     * Calculates the MD5 digest and returns the value as a 16 element {@code byte[]}.
     *
     * @param data
     *            Data to digest
     * @return MD5 digest
     * @throws IOException
     *             On error reading from the stream
     */
    public static byte[] md5(final InputStream data) throws IOException {
        return digest(getMd5Digest(), data);
    }

    /**
     * Calculates the MD5 digest and returns the value as a 16 element {@code byte[]}.
     *
     * @param data
     *            Data to digest; converted to bytes using {@link Strings#getBytesUtf8(String)}
     * @return MD5 digest
     */
    public static byte[] md5(final String data) {
        return md5(getBytes(data));
    }

    /**
     * Calculates the MD5 digest and returns the value as a 32 character hex string.
     *
     * @param data
     *            Data to digest
     * @return MD5 digest as a hex string
     */
    public static String md5Hex(final byte[] data) {
        return Hex.encodeToString(md5(data));
    }

    /**
     * Calculates the MD5 digest and returns the value as a 32 character hex string.
     *
     * @param data
     *            Data to digest
     * @return MD5 digest as a hex string
     * @throws IOException
     *             On error reading from the stream
     */
    public static String md5Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(md5(data));
    }

    /**
     * Calculates the MD5 digest and returns the value as a 32 character hex string.
     *
     * @param data
     *            Data to digest
     * @return MD5 digest as a hex string
     */
    public static String md5Hex(final String data) {
        return Hex.encodeToString(md5(data));
    }

    /**
     * Calculates the MD5 digest and returns the value as a base64 encoded string.
     *
     * @param data
     *            Data to digest
     * @return MD5 digest as a hex string
     */
    public static String md5Base64(final byte[] data) {
        return Strings.base64Encode(md5(data));
    }

    /**
     * Calculates the MD5 digest and returns the value as a base64 encoded string.
     *
     * @param data
     *            Data to digest
     * @return MD5 digest as a hex string
     * @throws IOException
     *             On error reading from the stream
     */
    public static String md5Base64(final InputStream data) throws IOException {
        return Strings.base64Encode(md5(data));
    }

    /**
     * Calculates the MD5 digest and returns the value as a base64 encoded string.
     *
     * @param data
     *            Data to digest
     * @return MD5 digest as a hex string
     */
    public static String md5Base64(final String data) {
        return Strings.base64Encode(md5(data));
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a {@code byte[]}.
     *
     * @param data
     *            Data to digest
     * @return SHA-1 digest
     */
    public static byte[] sha1(final byte[] data) {
        return getSha1Digest().digest(data);
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a {@code byte[]}.
     *
     * @param data
     *            Data to digest
     * @return SHA-1 digest
     * @throws IOException
     *             On error reading from the stream
     */
    public static byte[] sha1(final InputStream data) throws IOException {
        return digest(getSha1Digest(), data);
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a {@code byte[]}.
     *
     * @param data
     *            Data to digest; converted to bytes using {@link Strings#getBytesUtf8(String)}
     * @return SHA-1 digest
     */
    public static byte[] sha1(final String data) {
        return sha1(getBytes(data));
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a hex string.
     *
     * @param data
     *            Data to digest
     * @return SHA-1 digest as a hex string
     */
    public static String sha1Hex(final byte[] data) {
        return Hex.encodeToString(sha1(data));
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a hex string.
     *
     * @param data
     *            Data to digest
     * @return SHA-1 digest as a hex string
     * @throws IOException
     *             On error reading from the stream
     */
    public static String sha1Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha1(data));
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a hex string.
     *
     * @param data
     *            Data to digest
     * @return SHA-1 digest as a hex string
     */
    public static String sha1Hex(final String data) {
        return Hex.encodeToString(sha1(data));
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a base64 encoded string.
     *
     * @param data
     *            Data to digest
     * @return SHA-1 digest as a hex string
     */
    public static String sha1Base64(final byte[] data) {
        return Strings.base64Encode(sha1(data));
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a base64 encoded string.
     *
     * @param data
     *            Data to digest
     * @return SHA-1 digest as a hex string
     * @throws IOException
     *             On error reading from the stream
     */
    public static String sha1Base64(final InputStream data) throws IOException {
        return Strings.base64Encode(sha1(data));
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a base64 encoded string.
     *
     * @param data
     *            Data to digest
     * @return SHA-1 digest as a hex string
     */
    public static String sha1Base64(final String data) {
        return Strings.base64Encode(sha1(data));
    }

    /**
     * Calculates the SHA-256 digest and returns the value as a {@code byte[]}.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @param data
     *            Data to digest
     * @return SHA-256 digest
     */
    public static byte[] sha256(final byte[] data) {
        return getSha256Digest().digest(data);
    }

    /**
     * Calculates the SHA-256 digest and returns the value as a {@code byte[]}.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @param data
     *            Data to digest
     * @return SHA-256 digest
     * @throws IOException
     *             On error reading from the stream
     */
    public static byte[] sha256(final InputStream data) throws IOException {
        return digest(getSha256Digest(), data);
    }

    /**
     * Calculates the SHA-256 digest and returns the value as a {@code byte[]}.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @param data
     *            Data to digest; converted to bytes using {@link Strings#getBytesUtf8(String)}
     * @return SHA-256 digest
     */
    public static byte[] sha256(final String data) {
        return sha256(getBytes(data));
    }

    /**
     * Calculates the SHA-256 digest and returns the value as a hex string.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @param data
     *            Data to digest
     * @return SHA-256 digest as a hex string
     */
    public static String sha256Hex(final byte[] data) {
        return Hex.encodeToString(sha256(data));
    }

    /**
     * Calculates the SHA-256 digest and returns the value as a hex string.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @param data
     *            Data to digest
     * @return SHA-256 digest as a hex string
     * @throws IOException
     *             On error reading from the stream
     */
    public static String sha256Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha256(data));
    }

    /**
     * Calculates the SHA-256 digest and returns the value as a hex string.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @param data
     *            Data to digest
     * @return SHA-256 digest as a hex string
     */
    public static String sha256Hex(final String data) {
        return Hex.encodeToString(sha256(data));
    }

    /**
     * Calculates the SHA-256 digest and returns the value as a base64 encoded string.
     *
     * @param data
     *            Data to digest
     * @return SHA-256 digest as a hex string
     */
    public static String sha256Base64(final byte[] data) {
        return Strings.base64Encode(sha256(data));
    }

    /**
     * Calculates the SHA-256 digest and returns the value as a base64 encoded string.
     *
     * @param data
     *            Data to digest
     * @return SHA-256 digest as a hex string
     * @throws IOException
     *             On error reading from the stream
     */
    public static String sha256Base64(final InputStream data) throws IOException {
        return Strings.base64Encode(sha256(data));
    }

    /**
     * Calculates the SHA-256 digest and returns the value as a base64 encoded string.
     *
     * @param data
     *            Data to digest
     * @return SHA-256 digest as a hex string
     */
    public static String sha256Base64(final String data) {
        return Strings.base64Encode(sha256(data));
    }

    /**
     * Calculates the SHA-384 digest and returns the value as a {@code byte[]}.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @param data
     *            Data to digest
     * @return SHA-384 digest
     */
    public static byte[] sha384(final byte[] data) {
        return getSha384Digest().digest(data);
    }

    /**
     * Calculates the SHA-384 digest and returns the value as a {@code byte[]}.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @param data
     *            Data to digest
     * @return SHA-384 digest
     * @throws IOException
     *             On error reading from the stream
     */
    public static byte[] sha384(final InputStream data) throws IOException {
        return digest(getSha384Digest(), data);
    }

    /**
     * Calculates the SHA-384 digest and returns the value as a {@code byte[]}.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @param data
     *            Data to digest; converted to bytes using {@link Strings#getBytesUtf8(String)}
     * @return SHA-384 digest
     */
    public static byte[] sha384(final String data) {
        return sha384(getBytes(data));
    }

    /**
     * Calculates the SHA-384 digest and returns the value as a hex string.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @param data
     *            Data to digest
     * @return SHA-384 digest as a hex string
     */
    public static String sha384Hex(final byte[] data) {
        return Hex.encodeToString(sha384(data));
    }

    /**
     * Calculates the SHA-384 digest and returns the value as a hex string.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @param data
     *            Data to digest
     * @return SHA-384 digest as a hex string
     * @throws IOException
     *             On error reading from the stream
     */
    public static String sha384Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha384(data));
    }

    /**
     * Calculates the SHA-384 digest and returns the value as a hex string.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @param data
     *            Data to digest
     * @return SHA-384 digest as a hex string
     */
    public static String sha384Hex(final String data) {
        return Hex.encodeToString(sha384(data));
    }

    /**
     * Calculates the SHA-384 digest and returns the value as a base64 encoded string.
     *
     * @param data
     *            Data to digest
     * @return SHA-384 digest as a hex string
     */
    public static String sha384Base64(final byte[] data) {
        return Strings.base64Encode(sha384(data));
    }

    /**
     * Calculates the SHA-384 digest and returns the value as a base64 encoded string.
     *
     * @param data
     *            Data to digest
     * @return SHA-384 digest as a hex string
     * @throws IOException
     *             On error reading from the stream
     */
    public static String sha384Base64(final InputStream data) throws IOException {
        return Strings.base64Encode(sha384(data));
    }

    /**
     * Calculates the SHA-384 digest and returns the value as a base64 encoded string.
     *
     * @param data
     *            Data to digest
     * @return SHA-384 digest as a hex string
     */
    public static String sha384Base64(final String data) {
        return Strings.base64Encode(sha384(data));
    }

    /**
     * Calculates the SHA-512 digest and returns the value as a {@code byte[]}.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @param data
     *            Data to digest
     * @return SHA-512 digest
     */
    public static byte[] sha512(final byte[] data) {
        return getSha512Digest().digest(data);
    }

    /**
     * Calculates the SHA-512 digest and returns the value as a {@code byte[]}.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @param data
     *            Data to digest
     * @return SHA-512 digest
     * @throws IOException
     *             On error reading from the stream
     */
    public static byte[] sha512(final InputStream data) throws IOException {
        return digest(getSha512Digest(), data);
    }

    /**
     * Calculates the SHA-512 digest and returns the value as a {@code byte[]}.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @param data
     *            Data to digest; converted to bytes using {@link Strings#getBytesUtf8(String)}
     * @return SHA-512 digest
     */
    public static byte[] sha512(final String data) {
        return sha512(getBytes(data));
    }

    /**
     * Calculates the SHA-512 digest and returns the value as a hex string.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @param data
     *            Data to digest
     * @return SHA-512 digest as a hex string
     */
    public static String sha512Hex(final byte[] data) {
        return Hex.encodeToString(sha512(data));
    }

    /**
     * Calculates the SHA-512 digest and returns the value as a hex string.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @param data
     *            Data to digest
     * @return SHA-512 digest as a hex string
     * @throws IOException
     *             On error reading from the stream
     */
    public static String sha512Hex(final InputStream data) throws IOException {
        return Hex.encodeToString(sha512(data));
    }

    /**
     * Calculates the SHA-512 digest and returns the value as a hex string.
     * <p>
     * Throws a {@code RuntimeException} on JRE versions prior to 1.4.0.
     * </p>
     *
     * @param data
     *            Data to digest
     * @return SHA-512 digest as a hex string
     */
    public static String sha512Hex(final String data) {
        return Hex.encodeToString(sha512(data));
    }

    /**
     * Calculates the SHA-512 digest and returns the value as a base64 encoded string.
     *
     * @param data
     *            Data to digest
     * @return SHA-512 digest as a hex string
     */
    public static String sha512Base64(final byte[] data) {
        return Strings.base64Encode(sha512(data));
    }

    /**
     * Calculates the SHA-512 digest and returns the value as a base64 encoded string.
     *
     * @param data
     *            Data to digest
     * @return SHA-512 digest as a hex string
     * @throws IOException
     *             On error reading from the stream
     */
    public static String sha512Base64(final InputStream data) throws IOException {
        return Strings.base64Encode(sha512(data));
    }

    /**
     * Calculates the SHA-512 digest and returns the value as a base64 encoded string.
     *
     * @param data
     *            Data to digest
     * @return SHA-512 digest as a hex string
     */
    public static String sha512Base64(final String data) {
        return Strings.base64Encode(sha512(data));
    }

    /**
     * Gets the bytes.
     *
     * @param data
     * @return
     */
    private static byte[] getBytes(final String data) {
        return data.getBytes(Charsets.DEFAULT);
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
         * Instantiates a new message digest algorithms.
         */
        private MessageDigestAlgorithms() {
            // cannot be instantiated.
        }

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

    }
}
