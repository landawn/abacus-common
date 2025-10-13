package com.landawn.abacus.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class DigestUtil101Test extends TestBase {

    @Test
    public void testDigestWithByteArray() {
        MessageDigest md = DigestUtil.getMd5Digest();
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);

        byte[] digest = DigestUtil.digest(md, data);

        Assertions.assertNotNull(digest);
        Assertions.assertEquals(16, digest.length);
    }

    @Test
    public void testDigestWithByteBuffer() {
        MessageDigest md = DigestUtil.getSha1Digest();
        ByteBuffer buffer = ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8));

        byte[] digest = DigestUtil.digest(md, buffer);

        Assertions.assertNotNull(digest);
        Assertions.assertEquals(20, digest.length);
    }

    @Test
    public void testDigestWithInputStream() throws IOException {
        MessageDigest md = DigestUtil.getSha256Digest();
        ByteArrayInputStream is = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));

        byte[] digest = DigestUtil.digest(md, is);

        Assertions.assertNotNull(digest);
        Assertions.assertEquals(32, digest.length);
    }

    @Test
    public void testGetDigest() {
        MessageDigest md = DigestUtil.getDigest("SHA-256");
        Assertions.assertNotNull(md);
        Assertions.assertEquals("SHA-256", md.getAlgorithm());
    }

    @Test
    public void testGetDigestInvalidAlgorithm() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DigestUtil.getDigest("INVALID_ALGORITHM");
        });
    }

    @Test
    public void testGetDigestWithDefault() {
        MessageDigest defaultMd = DigestUtil.getSha256Digest();
        MessageDigest md = DigestUtil.getDigest("INVALID_ALGORITHM", defaultMd);

        Assertions.assertSame(defaultMd, md);
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
    public void testMd5ByteArray() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        byte[] digest = DigestUtil.md5(data);

        Assertions.assertNotNull(digest);
        Assertions.assertEquals(16, digest.length);
    }

    @Test
    public void testMd5InputStream() throws IOException {
        ByteArrayInputStream is = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        byte[] digest = DigestUtil.md5(is);

        Assertions.assertNotNull(digest);
        Assertions.assertEquals(16, digest.length);
    }

    @Test
    public void testMd5String() {
        byte[] digest = DigestUtil.md5("test");

        Assertions.assertNotNull(digest);
        Assertions.assertEquals(16, digest.length);
    }

    @Test
    public void testMd5Hex() {
        String hex = DigestUtil.md5Hex("test");

        Assertions.assertNotNull(hex);
        Assertions.assertEquals(32, hex.length());
        Assertions.assertTrue(hex.matches("[0-9a-f]+"));
    }

    @Test
    public void testSha1() {
        byte[] digest = DigestUtil.sha1("test");

        Assertions.assertNotNull(digest);
        Assertions.assertEquals(20, digest.length);
    }

    @Test
    public void testSha1Hex() {
        String hex = DigestUtil.sha1Hex("test");

        Assertions.assertNotNull(hex);
        Assertions.assertEquals(40, hex.length());
        Assertions.assertTrue(hex.matches("[0-9a-f]+"));
    }

    @Test
    public void testSha256() {
        byte[] digest = DigestUtil.sha256("test");

        Assertions.assertNotNull(digest);
        Assertions.assertEquals(32, digest.length);
    }

    @Test
    public void testSha256Hex() {
        String hex = DigestUtil.sha256Hex("test");

        Assertions.assertNotNull(hex);
        Assertions.assertEquals(64, hex.length());
        Assertions.assertTrue(hex.matches("[0-9a-f]+"));
    }

    @Test
    public void testSha512() {
        byte[] digest = DigestUtil.sha512("test");

        Assertions.assertNotNull(digest);
        Assertions.assertEquals(64, digest.length);
    }

    @Test
    public void testSha512Hex() {
        String hex = DigestUtil.sha512Hex("test");

        Assertions.assertNotNull(hex);
        Assertions.assertEquals(128, hex.length());
        Assertions.assertTrue(hex.matches("[0-9a-f]+"));
    }

    @Test
    public void testUpdateDigestByteArray() {
        MessageDigest md = DigestUtil.getMd5Digest();
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);

        MessageDigest updated = DigestUtil.updateDigest(md, data);

        Assertions.assertSame(md, updated);
    }

    @Test
    public void testUpdateDigestString() {
        MessageDigest md = DigestUtil.getMd5Digest();

        MessageDigest updated = DigestUtil.updateDigest(md, "test");

        Assertions.assertSame(md, updated);
    }
}
