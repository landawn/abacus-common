package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DigestUtil2025Test extends TestBase {

    private static final String TEST_STRING = "Hello World";
    private static final byte[] TEST_BYTES = TEST_STRING.getBytes();
    private File tempFile;
    private Path tempPath;

    @BeforeEach
    public void setUp() throws IOException {
        tempFile = File.createTempFile("digest_test_", ".tmp");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(TEST_BYTES);
        }
        tempPath = tempFile.toPath();
    }

    @AfterEach
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    public void test_digest_MessageDigest_byteArray() {
        MessageDigest md = DigestUtil.getMd5Digest();
        byte[] result = DigestUtil.digest(md, TEST_BYTES);
        assertNotNull(result);
        assertEquals(16, result.length);
    }

    @Test
    public void test_digest_MessageDigest_ByteBuffer() {
        MessageDigest md = DigestUtil.getSha256Digest();
        ByteBuffer buffer = ByteBuffer.wrap(TEST_BYTES);
        int originalPosition = buffer.position();
        byte[] result = DigestUtil.digest(md, buffer);
        assertNotNull(result);
        assertEquals(32, result.length);
        assertEquals(buffer.limit(), buffer.position());
    }

    @Test
    public void test_digest_MessageDigest_File() throws IOException {
        MessageDigest md = DigestUtil.getSha1Digest();
        byte[] result = DigestUtil.digest(md, tempFile);
        assertNotNull(result);
        assertEquals(20, result.length);
    }

    @Test
    public void test_digest_MessageDigest_InputStream() throws IOException {
        MessageDigest md = DigestUtil.getSha256Digest();
        try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
            byte[] result = DigestUtil.digest(md, is);
            assertNotNull(result);
            assertEquals(32, result.length);
        }
    }

    @Test
    public void test_digest_MessageDigest_Path() throws IOException {
        MessageDigest md = DigestUtil.getSha512Digest();
        byte[] result = DigestUtil.digest(md, tempPath, StandardOpenOption.READ);
        assertNotNull(result);
        assertEquals(64, result.length);
    }

    @Test
    public void test_digest_MessageDigest_RandomAccessFile() throws IOException {
        MessageDigest md = DigestUtil.getSha256Digest();
        try (RandomAccessFile raf = new RandomAccessFile(tempFile, "r")) {
            byte[] result = DigestUtil.digest(md, raf);
            assertNotNull(result);
            assertEquals(32, result.length);
        }
    }

    @Test
    public void test_getDigest_String() {
        MessageDigest md = DigestUtil.getDigest("SHA-256");
        assertNotNull(md);
        assertEquals("SHA-256", md.getAlgorithm());
    }

    @Test
    public void test_getDigest_String_invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            DigestUtil.getDigest("INVALID_ALGORITHM");
        });
    }

    @Test
    public void test_getDigest_String_MessageDigest() {
        MessageDigest fallback = DigestUtil.getSha256Digest();
        MessageDigest result = DigestUtil.getDigest("SHA-512", fallback);
        assertNotNull(result);
        assertEquals("SHA-512", result.getAlgorithm());
    }

    @Test
    public void test_getDigest_String_MessageDigest_withInvalidAlgorithm() {
        MessageDigest fallback = DigestUtil.getSha256Digest();
        MessageDigest result = DigestUtil.getDigest("INVALID_ALGORITHM", fallback);
        assertNotNull(result);
        assertEquals("SHA-256", result.getAlgorithm());
    }

    @Test
    public void test_getMd2Digest() {
        try {
            MessageDigest md = DigestUtil.getMd2Digest();
            assertNotNull(md);
            assertEquals("MD2", md.getAlgorithm());
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MD2") || e.getCause() instanceof java.security.NoSuchAlgorithmException);
        }
    }

    @Test
    public void test_getMd5Digest() {
        MessageDigest md = DigestUtil.getMd5Digest();
        assertNotNull(md);
        assertEquals("MD5", md.getAlgorithm());
    }

    @Test
    public void test_getSha1Digest() {
        MessageDigest md = DigestUtil.getSha1Digest();
        assertNotNull(md);
        assertEquals("SHA-1", md.getAlgorithm());
    }

    @Test
    public void test_getSha256Digest() {
        MessageDigest md = DigestUtil.getSha256Digest();
        assertNotNull(md);
        assertEquals("SHA-256", md.getAlgorithm());
    }

    @Test
    public void test_getSha3_224Digest() {
        if (DigestUtil.isAvailable("SHA3-224")) {
            MessageDigest md = DigestUtil.getSha3_224Digest();
            assertNotNull(md);
            assertEquals("SHA3-224", md.getAlgorithm());
        }
    }

    @Test
    public void test_getSha3_256Digest() {
        if (DigestUtil.isAvailable("SHA3-256")) {
            MessageDigest md = DigestUtil.getSha3_256Digest();
            assertNotNull(md);
            assertEquals("SHA3-256", md.getAlgorithm());
        }
    }

    @Test
    public void test_getSha3_384Digest() {
        if (DigestUtil.isAvailable("SHA3-384")) {
            MessageDigest md = DigestUtil.getSha3_384Digest();
            assertNotNull(md);
            assertEquals("SHA3-384", md.getAlgorithm());
        }
    }

    @Test
    public void test_getSha3_512Digest() {
        if (DigestUtil.isAvailable("SHA3-512")) {
            MessageDigest md = DigestUtil.getSha3_512Digest();
            assertNotNull(md);
            assertEquals("SHA3-512", md.getAlgorithm());
        }
    }

    @Test
    public void test_getSha384Digest() {
        MessageDigest md = DigestUtil.getSha384Digest();
        assertNotNull(md);
        assertEquals("SHA-384", md.getAlgorithm());
    }

    @Test
    public void test_getSha512_224Digest() {
        if (DigestUtil.isAvailable("SHA-512/224")) {
            MessageDigest md = DigestUtil.getSha512_224Digest();
            assertNotNull(md);
            assertEquals("SHA-512/224", md.getAlgorithm());
        }
    }

    @Test
    public void test_getSha512_256Digest() {
        if (DigestUtil.isAvailable("SHA-512/256")) {
            MessageDigest md = DigestUtil.getSha512_256Digest();
            assertNotNull(md);
            assertEquals("SHA-512/256", md.getAlgorithm());
        }
    }

    @Test
    public void test_getSha512Digest() {
        MessageDigest md = DigestUtil.getSha512Digest();
        assertNotNull(md);
        assertEquals("SHA-512", md.getAlgorithm());
    }

    @Test
    public void test_getShaDigest() {
        MessageDigest md = DigestUtil.getShaDigest();
        assertNotNull(md);
        assertEquals("SHA-1", md.getAlgorithm());
    }

    @Test
    public void test_isAvailable_validAlgorithm() {
        assertTrue(DigestUtil.isAvailable("SHA-256"));
        assertTrue(DigestUtil.isAvailable("MD5"));
        assertTrue(DigestUtil.isAvailable("SHA-1"));
    }

    @Test
    public void test_isAvailable_invalidAlgorithm() {
        assertFalse(DigestUtil.isAvailable("INVALID_ALGORITHM"));
        assertFalse(DigestUtil.isAvailable(null));
    }

    @Test
    public void test_md2_byteArray() {
        if (DigestUtil.isAvailable("MD2")) {
            byte[] result = DigestUtil.md2(TEST_BYTES);
            assertNotNull(result);
            assertEquals(16, result.length);
        }
    }

    @Test
    public void test_md2_InputStream() throws IOException {
        if (DigestUtil.isAvailable("MD2")) {
            try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
                byte[] result = DigestUtil.md2(is);
                assertNotNull(result);
                assertEquals(16, result.length);
            }
        }
    }

    @Test
    public void test_md2_String() {
        if (DigestUtil.isAvailable("MD2")) {
            byte[] result = DigestUtil.md2(TEST_STRING);
            assertNotNull(result);
            assertEquals(16, result.length);
        }
    }

    @Test
    public void test_md2Hex_byteArray() {
        if (DigestUtil.isAvailable("MD2")) {
            String result = DigestUtil.md2Hex(TEST_BYTES);
            assertNotNull(result);
            assertEquals(32, result.length());
        }
    }

    @Test
    public void test_md2Hex_InputStream() throws IOException {
        if (DigestUtil.isAvailable("MD2")) {
            try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
                String result = DigestUtil.md2Hex(is);
                assertNotNull(result);
                assertEquals(32, result.length());
            }
        }
    }

    @Test
    public void test_md2Hex_String() {
        if (DigestUtil.isAvailable("MD2")) {
            String result = DigestUtil.md2Hex(TEST_STRING);
            assertNotNull(result);
            assertEquals(32, result.length());
        }
    }

    @Test
    public void test_md5_byteArray() {
        byte[] result = DigestUtil.md5(TEST_BYTES);
        assertNotNull(result);
        assertEquals(16, result.length);
    }

    @Test
    public void test_md5_InputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
            byte[] result = DigestUtil.md5(is);
            assertNotNull(result);
            assertEquals(16, result.length);
        }
    }

    @Test
    public void test_md5_String() {
        byte[] result = DigestUtil.md5(TEST_STRING);
        assertNotNull(result);
        assertEquals(16, result.length);
    }

    @Test
    public void test_md5Hex_byteArray() {
        String result = DigestUtil.md5Hex(TEST_BYTES);
        assertNotNull(result);
        assertEquals(32, result.length());
        assertEquals("b10a8db164e0754105b7a99be72e3fe5", result);
    }

    @Test
    public void test_md5Hex_InputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
            String result = DigestUtil.md5Hex(is);
            assertNotNull(result);
            assertEquals(32, result.length());
            assertEquals("b10a8db164e0754105b7a99be72e3fe5", result);
        }
    }

    @Test
    public void test_md5Hex_String() {
        String result = DigestUtil.md5Hex(TEST_STRING);
        assertNotNull(result);
        assertEquals(32, result.length());
        assertEquals("b10a8db164e0754105b7a99be72e3fe5", result);
    }

    @Test
    public void test_sha_byteArray() {
        byte[] result = DigestUtil.sha(TEST_BYTES);
        assertNotNull(result);
        assertEquals(20, result.length);
    }

    @Test
    public void test_sha_InputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
            byte[] result = DigestUtil.sha(is);
            assertNotNull(result);
            assertEquals(20, result.length);
        }
    }

    @Test
    public void test_sha_String() {
        byte[] result = DigestUtil.sha(TEST_STRING);
        assertNotNull(result);
        assertEquals(20, result.length);
    }

    @Test
    public void test_sha1_byteArray() {
        byte[] result = DigestUtil.sha1(TEST_BYTES);
        assertNotNull(result);
        assertEquals(20, result.length);
    }

    @Test
    public void test_sha1_InputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
            byte[] result = DigestUtil.sha1(is);
            assertNotNull(result);
            assertEquals(20, result.length);
        }
    }

    @Test
    public void test_sha1_String() {
        byte[] result = DigestUtil.sha1(TEST_STRING);
        assertNotNull(result);
        assertEquals(20, result.length);
    }

    @Test
    public void test_sha1Hex_byteArray() {
        String result = DigestUtil.sha1Hex(TEST_BYTES);
        assertNotNull(result);
        assertEquals(40, result.length());
    }

    @Test
    public void test_sha1Hex_InputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
            String result = DigestUtil.sha1Hex(is);
            assertNotNull(result);
            assertEquals(40, result.length());
        }
    }

    @Test
    public void test_sha1Hex_String() {
        String result = DigestUtil.sha1Hex(TEST_STRING);
        assertNotNull(result);
        assertEquals(40, result.length());
    }

    @Test
    public void test_sha256_byteArray() {
        byte[] result = DigestUtil.sha256(TEST_BYTES);
        assertNotNull(result);
        assertEquals(32, result.length);
    }

    @Test
    public void test_sha256_InputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
            byte[] result = DigestUtil.sha256(is);
            assertNotNull(result);
            assertEquals(32, result.length);
        }
    }

    @Test
    public void test_sha256_String() {
        byte[] result = DigestUtil.sha256(TEST_STRING);
        assertNotNull(result);
        assertEquals(32, result.length);
    }

    @Test
    public void test_sha256Hex_byteArray() {
        String result = DigestUtil.sha256Hex(TEST_BYTES);
        assertNotNull(result);
        assertEquals(64, result.length());
    }

    @Test
    public void test_sha256Hex_InputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
            String result = DigestUtil.sha256Hex(is);
            assertNotNull(result);
            assertEquals(64, result.length());
        }
    }

    @Test
    public void test_sha256Hex_String() {
        String result = DigestUtil.sha256Hex(TEST_STRING);
        assertNotNull(result);
        assertEquals(64, result.length());
    }

    @Test
    public void test_sha3_224_byteArray() {
        if (DigestUtil.isAvailable("SHA3-224")) {
            byte[] result = DigestUtil.sha3_224(TEST_BYTES);
            assertNotNull(result);
            assertEquals(28, result.length);
        }
    }

    @Test
    public void test_sha3_224_InputStream() throws IOException {
        if (DigestUtil.isAvailable("SHA3-224")) {
            try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
                byte[] result = DigestUtil.sha3_224(is);
                assertNotNull(result);
                assertEquals(28, result.length);
            }
        }
    }

    @Test
    public void test_sha3_224_String() {
        if (DigestUtil.isAvailable("SHA3-224")) {
            byte[] result = DigestUtil.sha3_224(TEST_STRING);
            assertNotNull(result);
            assertEquals(28, result.length);
        }
    }

    @Test
    public void test_sha3_224Hex_byteArray() {
        if (DigestUtil.isAvailable("SHA3-224")) {
            String result = DigestUtil.sha3_224Hex(TEST_BYTES);
            assertNotNull(result);
            assertEquals(56, result.length());
        }
    }

    @Test
    public void test_sha3_224Hex_InputStream() throws IOException {
        if (DigestUtil.isAvailable("SHA3-224")) {
            try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
                String result = DigestUtil.sha3_224Hex(is);
                assertNotNull(result);
                assertEquals(56, result.length());
            }
        }
    }

    @Test
    public void test_sha3_224Hex_String() {
        if (DigestUtil.isAvailable("SHA3-224")) {
            String result = DigestUtil.sha3_224Hex(TEST_STRING);
            assertNotNull(result);
            assertEquals(56, result.length());
        }
    }

    @Test
    public void test_sha3_256_byteArray() {
        if (DigestUtil.isAvailable("SHA3-256")) {
            byte[] result = DigestUtil.sha3_256(TEST_BYTES);
            assertNotNull(result);
            assertEquals(32, result.length);
        }
    }

    @Test
    public void test_sha3_256_InputStream() throws IOException {
        if (DigestUtil.isAvailable("SHA3-256")) {
            try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
                byte[] result = DigestUtil.sha3_256(is);
                assertNotNull(result);
                assertEquals(32, result.length);
            }
        }
    }

    @Test
    public void test_sha3_256_String() {
        if (DigestUtil.isAvailable("SHA3-256")) {
            byte[] result = DigestUtil.sha3_256(TEST_STRING);
            assertNotNull(result);
            assertEquals(32, result.length);
        }
    }

    @Test
    public void test_sha3_256Hex_byteArray() {
        if (DigestUtil.isAvailable("SHA3-256")) {
            String result = DigestUtil.sha3_256Hex(TEST_BYTES);
            assertNotNull(result);
            assertEquals(64, result.length());
        }
    }

    @Test
    public void test_sha3_256Hex_InputStream() throws IOException {
        if (DigestUtil.isAvailable("SHA3-256")) {
            try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
                String result = DigestUtil.sha3_256Hex(is);
                assertNotNull(result);
                assertEquals(64, result.length());
            }
        }
    }

    @Test
    public void test_sha3_256Hex_String() {
        if (DigestUtil.isAvailable("SHA3-256")) {
            String result = DigestUtil.sha3_256Hex(TEST_STRING);
            assertNotNull(result);
            assertEquals(64, result.length());
        }
    }

    @Test
    public void test_sha3_384_byteArray() {
        if (DigestUtil.isAvailable("SHA3-384")) {
            byte[] result = DigestUtil.sha3_384(TEST_BYTES);
            assertNotNull(result);
            assertEquals(48, result.length);
        }
    }

    @Test
    public void test_sha3_384_InputStream() throws IOException {
        if (DigestUtil.isAvailable("SHA3-384")) {
            try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
                byte[] result = DigestUtil.sha3_384(is);
                assertNotNull(result);
                assertEquals(48, result.length);
            }
        }
    }

    @Test
    public void test_sha3_384_String() {
        if (DigestUtil.isAvailable("SHA3-384")) {
            byte[] result = DigestUtil.sha3_384(TEST_STRING);
            assertNotNull(result);
            assertEquals(48, result.length);
        }
    }

    @Test
    public void test_sha3_384Hex_byteArray() {
        if (DigestUtil.isAvailable("SHA3-384")) {
            String result = DigestUtil.sha3_384Hex(TEST_BYTES);
            assertNotNull(result);
            assertEquals(96, result.length());
        }
    }

    @Test
    public void test_sha3_384Hex_InputStream() throws IOException {
        if (DigestUtil.isAvailable("SHA3-384")) {
            try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
                String result = DigestUtil.sha3_384Hex(is);
                assertNotNull(result);
                assertEquals(96, result.length());
            }
        }
    }

    @Test
    public void test_sha3_384Hex_String() {
        if (DigestUtil.isAvailable("SHA3-384")) {
            String result = DigestUtil.sha3_384Hex(TEST_STRING);
            assertNotNull(result);
            assertEquals(96, result.length());
        }
    }

    @Test
    public void test_sha3_512_byteArray() {
        if (DigestUtil.isAvailable("SHA3-512")) {
            byte[] result = DigestUtil.sha3_512(TEST_BYTES);
            assertNotNull(result);
            assertEquals(64, result.length);
        }
    }

    @Test
    public void test_sha3_512_InputStream() throws IOException {
        if (DigestUtil.isAvailable("SHA3-512")) {
            try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
                byte[] result = DigestUtil.sha3_512(is);
                assertNotNull(result);
                assertEquals(64, result.length);
            }
        }
    }

    @Test
    public void test_sha3_512_String() {
        if (DigestUtil.isAvailable("SHA3-512")) {
            byte[] result = DigestUtil.sha3_512(TEST_STRING);
            assertNotNull(result);
            assertEquals(64, result.length);
        }
    }

    @Test
    public void test_sha3_512Hex_byteArray() {
        if (DigestUtil.isAvailable("SHA3-512")) {
            String result = DigestUtil.sha3_512Hex(TEST_BYTES);
            assertNotNull(result);
            assertEquals(128, result.length());
        }
    }

    @Test
    public void test_sha3_512Hex_InputStream() throws IOException {
        if (DigestUtil.isAvailable("SHA3-512")) {
            try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
                String result = DigestUtil.sha3_512Hex(is);
                assertNotNull(result);
                assertEquals(128, result.length());
            }
        }
    }

    @Test
    public void test_sha3_512Hex_String() {
        if (DigestUtil.isAvailable("SHA3-512")) {
            String result = DigestUtil.sha3_512Hex(TEST_STRING);
            assertNotNull(result);
            assertEquals(128, result.length());
        }
    }

    @Test
    public void test_sha384_byteArray() {
        byte[] result = DigestUtil.sha384(TEST_BYTES);
        assertNotNull(result);
        assertEquals(48, result.length);
    }

    @Test
    public void test_sha384_InputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
            byte[] result = DigestUtil.sha384(is);
            assertNotNull(result);
            assertEquals(48, result.length);
        }
    }

    @Test
    public void test_sha384_String() {
        byte[] result = DigestUtil.sha384(TEST_STRING);
        assertNotNull(result);
        assertEquals(48, result.length);
    }

    @Test
    public void test_sha384Hex_byteArray() {
        String result = DigestUtil.sha384Hex(TEST_BYTES);
        assertNotNull(result);
        assertEquals(96, result.length());
    }

    @Test
    public void test_sha384Hex_InputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
            String result = DigestUtil.sha384Hex(is);
            assertNotNull(result);
            assertEquals(96, result.length());
        }
    }

    @Test
    public void test_sha384Hex_String() {
        String result = DigestUtil.sha384Hex(TEST_STRING);
        assertNotNull(result);
        assertEquals(96, result.length());
    }

    @Test
    public void test_sha512_byteArray() {
        byte[] result = DigestUtil.sha512(TEST_BYTES);
        assertNotNull(result);
        assertEquals(64, result.length);
    }

    @Test
    public void test_sha512_InputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
            byte[] result = DigestUtil.sha512(is);
            assertNotNull(result);
            assertEquals(64, result.length);
        }
    }

    @Test
    public void test_sha512_String() {
        byte[] result = DigestUtil.sha512(TEST_STRING);
        assertNotNull(result);
        assertEquals(64, result.length);
    }

    @Test
    public void test_sha512_224_byteArray() {
        if (DigestUtil.isAvailable("SHA-512/224")) {
            byte[] result = DigestUtil.sha512_224(TEST_BYTES);
            assertNotNull(result);
            assertEquals(28, result.length);
        }
    }

    @Test
    public void test_sha512_224_InputStream() throws IOException {
        if (DigestUtil.isAvailable("SHA-512/224")) {
            try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
                byte[] result = DigestUtil.sha512_224(is);
                assertNotNull(result);
                assertEquals(28, result.length);
            }
        }
    }

    @Test
    public void test_sha512_224_String() {
        if (DigestUtil.isAvailable("SHA-512/224")) {
            byte[] result = DigestUtil.sha512_224(TEST_STRING);
            assertNotNull(result);
            assertEquals(28, result.length);
        }
    }

    @Test
    public void test_sha512_224Hex_byteArray() {
        if (DigestUtil.isAvailable("SHA-512/224")) {
            String result = DigestUtil.sha512_224Hex(TEST_BYTES);
            assertNotNull(result);
            assertEquals(56, result.length());
        }
    }

    @Test
    public void test_sha512_224Hex_InputStream() throws IOException {
        if (DigestUtil.isAvailable("SHA-512/224")) {
            try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
                String result = DigestUtil.sha512_224Hex(is);
                assertNotNull(result);
                assertEquals(56, result.length());
            }
        }
    }

    @Test
    public void test_sha512_224Hex_String() {
        if (DigestUtil.isAvailable("SHA-512/224")) {
            String result = DigestUtil.sha512_224Hex(TEST_STRING);
            assertNotNull(result);
            assertEquals(56, result.length());
        }
    }

    @Test
    public void test_sha512_256_byteArray() {
        if (DigestUtil.isAvailable("SHA-512/256")) {
            byte[] result = DigestUtil.sha512_256(TEST_BYTES);
            assertNotNull(result);
            assertEquals(32, result.length);
        }
    }

    @Test
    public void test_sha512_256_InputStream() throws IOException {
        if (DigestUtil.isAvailable("SHA-512/256")) {
            try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
                byte[] result = DigestUtil.sha512_256(is);
                assertNotNull(result);
                assertEquals(32, result.length);
            }
        }
    }

    @Test
    public void test_sha512_256_String() {
        if (DigestUtil.isAvailable("SHA-512/256")) {
            byte[] result = DigestUtil.sha512_256(TEST_STRING);
            assertNotNull(result);
            assertEquals(32, result.length);
        }
    }

    @Test
    public void test_sha512_256Hex_byteArray() {
        if (DigestUtil.isAvailable("SHA-512/256")) {
            String result = DigestUtil.sha512_256Hex(TEST_BYTES);
            assertNotNull(result);
            assertEquals(64, result.length());
        }
    }

    @Test
    public void test_sha512_256Hex_InputStream() throws IOException {
        if (DigestUtil.isAvailable("SHA-512/256")) {
            try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
                String result = DigestUtil.sha512_256Hex(is);
                assertNotNull(result);
                assertEquals(64, result.length());
            }
        }
    }

    @Test
    public void test_sha512_256Hex_String() {
        if (DigestUtil.isAvailable("SHA-512/256")) {
            String result = DigestUtil.sha512_256Hex(TEST_STRING);
            assertNotNull(result);
            assertEquals(64, result.length());
        }
    }

    @Test
    public void test_sha512Hex_byteArray() {
        String result = DigestUtil.sha512Hex(TEST_BYTES);
        assertNotNull(result);
        assertEquals(128, result.length());
    }

    @Test
    public void test_sha512Hex_InputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
            String result = DigestUtil.sha512Hex(is);
            assertNotNull(result);
            assertEquals(128, result.length());
        }
    }

    @Test
    public void test_sha512Hex_String() {
        String result = DigestUtil.sha512Hex(TEST_STRING);
        assertNotNull(result);
        assertEquals(128, result.length());
    }

    @Test
    public void test_shaHex_byteArray() {
        String result = DigestUtil.shaHex(TEST_BYTES);
        assertNotNull(result);
        assertEquals(40, result.length());
    }

    @Test
    public void test_shaHex_InputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
            String result = DigestUtil.shaHex(is);
            assertNotNull(result);
            assertEquals(40, result.length());
        }
    }

    @Test
    public void test_shaHex_String() {
        String result = DigestUtil.shaHex(TEST_STRING);
        assertNotNull(result);
        assertEquals(40, result.length());
    }

    @Test
    public void test_updateDigest_MessageDigest_byteArray() {
        MessageDigest md = DigestUtil.getSha256Digest();
        MessageDigest result = DigestUtil.updateDigest(md, TEST_BYTES);
        assertNotNull(result);
        assertEquals(md, result);
        byte[] hash = result.digest();
        assertEquals(32, hash.length);
    }

    @Test
    public void test_updateDigest_MessageDigest_ByteBuffer() {
        MessageDigest md = DigestUtil.getSha256Digest();
        ByteBuffer buffer = ByteBuffer.wrap(TEST_BYTES);
        MessageDigest result = DigestUtil.updateDigest(md, buffer);
        assertNotNull(result);
        assertEquals(md, result);
        assertEquals(buffer.limit(), buffer.position());
    }

    @Test
    public void test_updateDigest_MessageDigest_File() throws IOException {
        MessageDigest md = DigestUtil.getSha256Digest();
        MessageDigest result = DigestUtil.updateDigest(md, tempFile);
        assertNotNull(result);
        assertEquals(md, result);
        byte[] hash = result.digest();
        assertEquals(32, hash.length);
    }

    @Test
    public void test_updateDigest_MessageDigest_InputStream() throws IOException {
        MessageDigest md = DigestUtil.getSha256Digest();
        try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
            MessageDigest result = DigestUtil.updateDigest(md, is);
            assertNotNull(result);
            assertEquals(md, result);
            byte[] hash = result.digest();
            assertEquals(32, hash.length);
        }
    }

    @Test
    public void test_updateDigest_MessageDigest_Path() throws IOException {
        MessageDigest md = DigestUtil.getSha256Digest();
        MessageDigest result = DigestUtil.updateDigest(md, tempPath, StandardOpenOption.READ);
        assertNotNull(result);
        assertEquals(md, result);
        byte[] hash = result.digest();
        assertEquals(32, hash.length);
    }

    @Test
    public void test_updateDigest_MessageDigest_RandomAccessFile() throws IOException {
        MessageDigest md = DigestUtil.getSha256Digest();
        try (RandomAccessFile raf = new RandomAccessFile(tempFile, "r")) {
            MessageDigest result = DigestUtil.updateDigest(md, raf);
            assertNotNull(result);
            assertEquals(md, result);
            byte[] hash = result.digest();
            assertEquals(32, hash.length);
        }
    }

    @Test
    public void test_updateDigest_MessageDigest_String() {
        MessageDigest md = DigestUtil.getSha256Digest();
        MessageDigest result = DigestUtil.updateDigest(md, TEST_STRING);
        assertNotNull(result);
        assertEquals(md, result);
        byte[] hash = result.digest();
        assertEquals(32, hash.length);
    }

    @Test
    public void test_incrementalDigest() {
        MessageDigest md = DigestUtil.getSha256Digest();
        DigestUtil.updateDigest(md, "Hello ");
        DigestUtil.updateDigest(md, "World");
        byte[] hash = md.digest();

        byte[] expectedHash = DigestUtil.sha256("Hello World");
        assertArrayEquals(expectedHash, hash);
    }

    @Test
    public void test_consistency_byteArray_vs_String() {
        byte[] hashFromBytes = DigestUtil.sha256(TEST_BYTES);
        byte[] hashFromString = DigestUtil.sha256(TEST_STRING);
        assertArrayEquals(hashFromBytes, hashFromString);
    }

    @Test
    public void test_consistency_byteArray_vs_InputStream() throws IOException {
        byte[] hashFromBytes = DigestUtil.md5(TEST_BYTES);
        byte[] hashFromStream;
        try (InputStream is = new ByteArrayInputStream(TEST_BYTES)) {
            hashFromStream = DigestUtil.md5(is);
        }
        assertArrayEquals(hashFromBytes, hashFromStream);
    }

    @Test
    public void test_consistency_hex_vs_bytes() {
        byte[] hash = DigestUtil.sha256(TEST_BYTES);
        String hexHash = DigestUtil.sha256Hex(TEST_BYTES);
        String expectedHex = Hex.encodeToString(hash);
        assertEquals(expectedHex, hexHash);
    }

    @Test
    public void test_emptyInput() {
        byte[] emptyBytes = new byte[0];
        byte[] result = DigestUtil.md5(emptyBytes);
        assertNotNull(result);
        assertEquals(16, result.length);
    }

    @Test
    public void test_largeInput() throws IOException {
        byte[] largeBytes = new byte[10000];
        for (int i = 0; i < largeBytes.length; i++) {
            largeBytes[i] = (byte) (i % 256);
        }

        byte[] result = DigestUtil.sha256(largeBytes);
        assertNotNull(result);
        assertEquals(32, result.length);

        try (InputStream is = new ByteArrayInputStream(largeBytes)) {
            byte[] result2 = DigestUtil.sha256(is);
            assertArrayEquals(result, result2);
        }
    }

    @Test
    public void test_fileDigest_consistency() throws IOException {
        byte[] hashFromFile = DigestUtil.digest(DigestUtil.getSha256Digest(), tempFile);
        byte[] hashFromBytes = DigestUtil.sha256(TEST_BYTES);
        assertArrayEquals(hashFromBytes, hashFromFile);
    }

    @Test
    public void test_pathDigest_consistency() throws IOException {
        byte[] hashFromPath = DigestUtil.digest(DigestUtil.getSha256Digest(), tempPath);
        byte[] hashFromBytes = DigestUtil.sha256(TEST_BYTES);
        assertArrayEquals(hashFromBytes, hashFromPath);
    }

    @Test
    public void test_byteBuffer_position() {
        ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.put(TEST_BYTES);
        buffer.flip();

        MessageDigest md = DigestUtil.getSha256Digest();
        DigestUtil.digest(md, buffer);

        assertEquals(buffer.limit(), buffer.position());
    }

    @Test
    public void test_MessageDigestAlgorithms_values() {
        String[] algorithms = DigestUtil.MessageDigestAlgorithms.values();
        assertNotNull(algorithms);
        assertTrue(algorithms.length > 0);

        boolean hasMD5 = false;
        boolean hasSHA256 = false;
        for (String algo : algorithms) {
            if ("MD5".equals(algo))
                hasMD5 = true;
            if ("SHA-256".equals(algo))
                hasSHA256 = true;
        }
        assertTrue(hasMD5);
        assertTrue(hasSHA256);
    }
}
