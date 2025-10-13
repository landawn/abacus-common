package com.landawn.abacus.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class DigestUtil100Test extends TestBase {

    private static final String TEST_STRING = "Hello, World!";
    private static final byte[] TEST_BYTES = TEST_STRING.getBytes();

    @Test
    public void testDigest_ByteArray() {
        MessageDigest md = DigestUtil.getMd5Digest();
        byte[] result = DigestUtil.digest(md, TEST_BYTES);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.length > 0);
    }

    @Test
    public void testDigest_ByteBuffer() {
        MessageDigest md = DigestUtil.getMd5Digest();
        ByteBuffer buffer = ByteBuffer.wrap(TEST_BYTES);
        byte[] result = DigestUtil.digest(md, buffer);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.length > 0);
    }

    @Test
    public void testDigest_File() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();
        Files.write(tempFile.toPath(), TEST_BYTES);

        MessageDigest md = DigestUtil.getMd5Digest();
        byte[] result = DigestUtil.digest(md, tempFile);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.length > 0);
    }

    @Test
    public void testDigest_InputStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_BYTES);
        MessageDigest md = DigestUtil.getMd5Digest();
        byte[] result = DigestUtil.digest(md, stream);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.length > 0);
    }

    @Test
    public void testDigest_Path() throws IOException {
        Path tempPath = Files.createTempFile("test", ".txt");
        Files.write(tempPath, TEST_BYTES);

        MessageDigest md = DigestUtil.getMd5Digest();
        byte[] result = DigestUtil.digest(md, tempPath, StandardOpenOption.READ);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.length > 0);

        Files.delete(tempPath);
    }

    @Test
    public void testDigest_RandomAccessFile() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();
        Files.write(tempFile.toPath(), TEST_BYTES);

        try (RandomAccessFile raf = new RandomAccessFile(tempFile, "r")) {
            MessageDigest md = DigestUtil.getMd5Digest();
            byte[] result = DigestUtil.digest(md, raf);
            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.length > 0);
        }
    }

    @Test
    public void testGetDigest() {
        MessageDigest md = DigestUtil.getDigest("MD5");
        Assertions.assertNotNull(md);
        Assertions.assertEquals("MD5", md.getAlgorithm());
    }

    @Test
    public void testGetDigest_InvalidAlgorithm() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DigestUtil.getDigest("INVALID_ALGORITHM");
        });
    }

    @Test
    public void testGetDigest_WithDefault() {
        MessageDigest defaultMd = DigestUtil.getMd5Digest();
        MessageDigest md = DigestUtil.getDigest("SHA-256", defaultMd);
        Assertions.assertNotNull(md);
        Assertions.assertEquals("SHA-256", md.getAlgorithm());

        MessageDigest fallback = DigestUtil.getDigest("INVALID", defaultMd);
        Assertions.assertSame(defaultMd, fallback);
    }

    @Test
    public void testGetMd2Digest() {
        MessageDigest md = DigestUtil.getMd2Digest();
        Assertions.assertNotNull(md);
        Assertions.assertEquals("MD2", md.getAlgorithm());
    }

    @Test
    public void testGetMd5Digest() {
        MessageDigest md = DigestUtil.getMd5Digest();
        Assertions.assertNotNull(md);
        Assertions.assertEquals("MD5", md.getAlgorithm());
    }

    @Test
    public void testGetSha1Digest() {
        MessageDigest md = DigestUtil.getSha1Digest();
        Assertions.assertNotNull(md);
        Assertions.assertEquals("SHA-1", md.getAlgorithm());
    }

    @Test
    public void testGetSha256Digest() {
        MessageDigest md = DigestUtil.getSha256Digest();
        Assertions.assertNotNull(md);
        Assertions.assertEquals("SHA-256", md.getAlgorithm());
    }

    @Test
    public void testGetSha384Digest() {
        MessageDigest md = DigestUtil.getSha384Digest();
        Assertions.assertNotNull(md);
        Assertions.assertEquals("SHA-384", md.getAlgorithm());
    }

    @Test
    public void testGetSha512Digest() {
        MessageDigest md = DigestUtil.getSha512Digest();
        Assertions.assertNotNull(md);
        Assertions.assertEquals("SHA-512", md.getAlgorithm());
    }

    @Test
    public void testGetShaDigest() {
        MessageDigest md = DigestUtil.getShaDigest();
        Assertions.assertNotNull(md);
        Assertions.assertEquals("SHA-1", md.getAlgorithm());
    }

    @Test
    public void testIsAvailable() {
        Assertions.assertTrue(DigestUtil.isAvailable("MD5"));
        Assertions.assertTrue(DigestUtil.isAvailable("SHA-256"));
        Assertions.assertFalse(DigestUtil.isAvailable("INVALID_ALGORITHM"));
    }

    @Test
    public void testMd2_ByteArray() {
        byte[] result = DigestUtil.md2(TEST_BYTES);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(16, result.length);
    }

    @Test
    public void testMd2_InputStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_BYTES);
        byte[] result = DigestUtil.md2(stream);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(16, result.length);
    }

    @Test
    public void testMd2_String() {
        byte[] result = DigestUtil.md2(TEST_STRING);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(16, result.length);
    }

    @Test
    public void testMd2Hex_ByteArray() {
        String result = DigestUtil.md2Hex(TEST_BYTES);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(32, result.length());
    }

    @Test
    public void testMd2Hex_InputStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_BYTES);
        String result = DigestUtil.md2Hex(stream);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(32, result.length());
    }

    @Test
    public void testMd2Hex_String() {
        String result = DigestUtil.md2Hex(TEST_STRING);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(32, result.length());
    }

    @Test
    public void testMd5_ByteArray() {
        byte[] result = DigestUtil.md5(TEST_BYTES);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(16, result.length);
    }

    @Test
    public void testMd5_InputStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_BYTES);
        byte[] result = DigestUtil.md5(stream);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(16, result.length);
    }

    @Test
    public void testMd5_String() {
        byte[] result = DigestUtil.md5(TEST_STRING);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(16, result.length);
    }

    @Test
    public void testMd5Hex_ByteArray() {
        String result = DigestUtil.md5Hex(TEST_BYTES);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(32, result.length());
    }

    @Test
    public void testMd5Hex_InputStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_BYTES);
        String result = DigestUtil.md5Hex(stream);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(32, result.length());
    }

    @Test
    public void testMd5Hex_String() {
        String result = DigestUtil.md5Hex(TEST_STRING);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(32, result.length());
    }

    @Test
    public void testSha_ByteArray() {
        byte[] result = DigestUtil.sha(TEST_BYTES);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(20, result.length);
    }

    @Test
    public void testSha_InputStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_BYTES);
        byte[] result = DigestUtil.sha(stream);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(20, result.length);
    }

    @Test
    public void testSha_String() {
        byte[] result = DigestUtil.sha(TEST_STRING);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(20, result.length);
    }

    @Test
    public void testSha1_ByteArray() {
        byte[] result = DigestUtil.sha1(TEST_BYTES);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(20, result.length);
    }

    @Test
    public void testSha1_InputStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_BYTES);
        byte[] result = DigestUtil.sha1(stream);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(20, result.length);
    }

    @Test
    public void testSha1_String() {
        byte[] result = DigestUtil.sha1(TEST_STRING);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(20, result.length);
    }

    @Test
    public void testSha1Hex_ByteArray() {
        String result = DigestUtil.sha1Hex(TEST_BYTES);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(40, result.length());
    }

    @Test
    public void testSha1Hex_InputStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_BYTES);
        String result = DigestUtil.sha1Hex(stream);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(40, result.length());
    }

    @Test
    public void testSha1Hex_String() {
        String result = DigestUtil.sha1Hex(TEST_STRING);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(40, result.length());
    }

    @Test
    public void testSha256_ByteArray() {
        byte[] result = DigestUtil.sha256(TEST_BYTES);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(32, result.length);
    }

    @Test
    public void testSha256_InputStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_BYTES);
        byte[] result = DigestUtil.sha256(stream);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(32, result.length);
    }

    @Test
    public void testSha256_String() {
        byte[] result = DigestUtil.sha256(TEST_STRING);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(32, result.length);
    }

    @Test
    public void testSha256Hex_ByteArray() {
        String result = DigestUtil.sha256Hex(TEST_BYTES);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(64, result.length());
    }

    @Test
    public void testSha256Hex_InputStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_BYTES);
        String result = DigestUtil.sha256Hex(stream);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(64, result.length());
    }

    @Test
    public void testSha256Hex_String() {
        String result = DigestUtil.sha256Hex(TEST_STRING);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(64, result.length());
    }

    @Test
    public void testSha384_ByteArray() {
        byte[] result = DigestUtil.sha384(TEST_BYTES);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(48, result.length);
    }

    @Test
    public void testSha384_InputStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_BYTES);
        byte[] result = DigestUtil.sha384(stream);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(48, result.length);
    }

    @Test
    public void testSha384_String() {
        byte[] result = DigestUtil.sha384(TEST_STRING);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(48, result.length);
    }

    @Test
    public void testSha384Hex_ByteArray() {
        String result = DigestUtil.sha384Hex(TEST_BYTES);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(96, result.length());
    }

    @Test
    public void testSha384Hex_InputStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_BYTES);
        String result = DigestUtil.sha384Hex(stream);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(96, result.length());
    }

    @Test
    public void testSha384Hex_String() {
        String result = DigestUtil.sha384Hex(TEST_STRING);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(96, result.length());
    }

    @Test
    public void testSha512_ByteArray() {
        byte[] result = DigestUtil.sha512(TEST_BYTES);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(64, result.length);
    }

    @Test
    public void testSha512_InputStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_BYTES);
        byte[] result = DigestUtil.sha512(stream);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(64, result.length);
    }

    @Test
    public void testSha512_String() {
        byte[] result = DigestUtil.sha512(TEST_STRING);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(64, result.length);
    }

    @Test
    public void testSha512Hex_ByteArray() {
        String result = DigestUtil.sha512Hex(TEST_BYTES);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(128, result.length());
    }

    @Test
    public void testSha512Hex_InputStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_BYTES);
        String result = DigestUtil.sha512Hex(stream);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(128, result.length());
    }

    @Test
    public void testSha512Hex_String() {
        String result = DigestUtil.sha512Hex(TEST_STRING);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(128, result.length());
    }

    @Test
    public void testShaHex_ByteArray() {
        String result = DigestUtil.shaHex(TEST_BYTES);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(40, result.length());
    }

    @Test
    public void testShaHex_InputStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_BYTES);
        String result = DigestUtil.shaHex(stream);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(40, result.length());
    }

    @Test
    public void testShaHex_String() {
        String result = DigestUtil.shaHex(TEST_STRING);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(40, result.length());
    }

    @Test
    public void testUpdateDigest_ByteArray() {
        MessageDigest md = DigestUtil.getMd5Digest();
        MessageDigest result = DigestUtil.updateDigest(md, TEST_BYTES);
        Assertions.assertSame(md, result);
    }

    @Test
    public void testUpdateDigest_ByteBuffer() {
        MessageDigest md = DigestUtil.getMd5Digest();
        ByteBuffer buffer = ByteBuffer.wrap(TEST_BYTES);
        MessageDigest result = DigestUtil.updateDigest(md, buffer);
        Assertions.assertSame(md, result);
    }

    @Test
    public void testUpdateDigest_File() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();
        Files.write(tempFile.toPath(), TEST_BYTES);

        MessageDigest md = DigestUtil.getMd5Digest();
        MessageDigest result = DigestUtil.updateDigest(md, tempFile);
        Assertions.assertSame(md, result);
    }

    @Test
    public void testUpdateDigest_InputStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_BYTES);
        MessageDigest md = DigestUtil.getMd5Digest();
        MessageDigest result = DigestUtil.updateDigest(md, stream);
        Assertions.assertSame(md, result);
    }

    @Test
    public void testUpdateDigest_Path() throws IOException {
        Path tempPath = Files.createTempFile("test", ".txt");
        Files.write(tempPath, TEST_BYTES);

        MessageDigest md = DigestUtil.getMd5Digest();
        MessageDigest result = DigestUtil.updateDigest(md, tempPath, StandardOpenOption.READ);
        Assertions.assertSame(md, result);

        Files.delete(tempPath);
    }
}
