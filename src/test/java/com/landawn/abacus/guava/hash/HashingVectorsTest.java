package com.landawn.abacus.guava.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.google.common.hash.HashCode;
import com.landawn.abacus.TestBase;

public class HashingVectorsTest extends TestBase {

    private static final byte[] EMPTY = new byte[0];
    private static final byte[] ABC = "abc".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] FOX = "the quick brown fox".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CRC_SAMPLE = "123456789".getBytes(StandardCharsets.US_ASCII);

    @Test
    @SuppressWarnings("deprecation")
    public void testMd5KnownVectors() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", Hashing.md5().hash(EMPTY).toString());
        assertEquals("900150983cd24fb0d6963f7d28e17f72", Hashing.md5().hash(ABC).toString());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSha1KnownVectors() {
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", Hashing.sha1().hash(EMPTY).toString());
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", Hashing.sha1().hash(ABC).toString());
    }

    @Test
    public void testSha256KnownVectors() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Hashing.sha256().hash(EMPTY).toString());
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Hashing.sha256().hash(ABC).toString());
    }

    @Test
    public void testSha384AndSha512KnownVectors() {
        assertEquals("38b060a751ac96384cd9327eb1b1e36a21fdb71114be07434c0cc7bf63f6e1da274edebfe76f65fbd51ad2f14898b95b",
                Hashing.sha384().hash(EMPTY).toString());
        assertEquals("cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed8086072ba1e7cc2358baeca134c825a7", Hashing.sha384().hash(ABC).toString());
        assertEquals("cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
                Hashing.sha512().hash(EMPTY).toString());
        assertEquals("ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
                Hashing.sha512().hash(ABC).toString());
    }

    @Test
    public void testChecksumKnownVectors() {
        assertEquals(0L, Hashing.crc32().hash(EMPTY).padToLong());
        assertEquals(0xcbf43926L, Hashing.crc32().hash(CRC_SAMPLE).padToLong());
        assertEquals(0L, Hashing.crc32c().hash(EMPTY).padToLong());
        assertEquals(0xe3069283L, Hashing.crc32c().hash(CRC_SAMPLE).padToLong());
        assertEquals(1L, Hashing.adler32().hash(EMPTY).padToLong());
        assertEquals(0x091e01deL, Hashing.adler32().hash(CRC_SAMPLE).padToLong());
    }

    @Test
    public void testHmacRfcVectors() {
        byte[] hmacMd5Key = new byte[16];
        Arrays.fill(hmacMd5Key, (byte) 0x0b);
        byte[] hmacSha256Key = new byte[20];
        Arrays.fill(hmacSha256Key, (byte) 0x0b);
        byte[] hiThere = "Hi There".getBytes(StandardCharsets.US_ASCII);

        assertEquals("9294727a3638bb1c13f48ef8158bfc9d", Hashing.hmacMd5(hmacMd5Key).hash(hiThere).toString());
        assertEquals("b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7", Hashing.hmacSha256(hmacSha256Key).hash(hiThere).toString());
        assertEquals("b617318655057264e28bc0b6fb378c8ef146be00", Hashing.hmacSha1(hmacSha256Key).hash(hiThere).toString());
    }

    @Test
    public void testWrapperMatchesGuavaVectors() {
        assertEquals(guava(com.google.common.hash.Hashing.murmur3_32_fixed(), FOX), Hashing.murmur3_32().hash(FOX).toString());
        assertEquals(guava(com.google.common.hash.Hashing.murmur3_32_fixed(42), FOX), Hashing.murmur3_32(42).hash(FOX).toString());
        assertEquals(guava(com.google.common.hash.Hashing.murmur3_128(), FOX), Hashing.murmur3_128().hash(FOX).toString());
        assertEquals(guava(com.google.common.hash.Hashing.murmur3_128(7), FOX), Hashing.murmur3_128(7).hash(FOX).toString());
        assertEquals(guava(com.google.common.hash.Hashing.sipHash24(), FOX), Hashing.sipHash24().hash(FOX).toString());
        assertEquals(guava(com.google.common.hash.Hashing.sipHash24(1L, 2L), FOX), Hashing.sipHash24(1L, 2L).hash(FOX).toString());
        assertEquals(guava(com.google.common.hash.Hashing.farmHashFingerprint64(), FOX), Hashing.farmHashFingerprint64().hash(FOX).toString());
        assertNotEquals(Hashing.murmur3_32(0).hash(FOX), Hashing.murmur3_32(1).hash(FOX));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testConcatenatedHashIsBytewiseConcatenation() {
        byte[] md5 = Hashing.md5().hash(ABC).asBytes();
        byte[] sha256 = Hashing.sha256().hash(ABC).asBytes();
        byte[] expected = new byte[md5.length + sha256.length];
        System.arraycopy(md5, 0, expected, 0, md5.length);
        System.arraycopy(sha256, 0, expected, md5.length, sha256.length);
        assertEquals(HashCode.fromBytes(expected), Hashing.concatenating(Hashing.md5(), Hashing.sha256()).hash(ABC));
    }

    private static String guava(final com.google.common.hash.HashFunction hf, final byte[] data) {
        return hf.hashBytes(data).toString();
    }
}
