package com.landawn.abacus.guava.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.common.hash.Funnel;
import com.landawn.abacus.TestBase;

public class GuavaHasherTest extends TestBase {

    private static class Person {
        final String name;
        final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    private static GuavaHasher wrapMurmur() {
        return GuavaHasher.wrap(com.google.common.hash.Hashing.murmur3_128().newHasher());
    }

    private static GuavaHasher farmHasher() {
        return GuavaHasher.wrap(com.google.common.hash.Hashing.farmHashFingerprint64().newHasher());
    }

    private static GuavaHasher concatFarmSha256Hasher() {
        return GuavaHasher.wrap(
                com.google.common.hash.Hashing.concatenating(com.google.common.hash.Hashing.farmHashFingerprint64(), com.google.common.hash.Hashing.sha256())
                        .newHasher());
    }

    private static void assertIndexOutOfBoundsNotOom(final Runnable call) {
        Throwable thrown = null;
        try {
            call.run();
        } catch (final Throwable t) {
            thrown = t;
        }
        assertNotNull(thrown, "expected IndexOutOfBoundsException but nothing was thrown");
        assertTrue(thrown instanceof IndexOutOfBoundsException, "expected IndexOutOfBoundsException but got " + thrown);
    }

    @Test
    public void testWrap() {
        com.google.common.hash.Hasher murmur = com.google.common.hash.Hashing.murmur3_128().newHasher();
        com.google.common.hash.Hasher sha = com.google.common.hash.Hashing.sha256().newHasher();
        GuavaHasher hasher = GuavaHasher.wrap(murmur);

        assertNotNull(hasher);
        assertSame(murmur, hasher.gHasher);
        assertNotSame(hasher, GuavaHasher.wrap(sha));
        assertThrows(NullPointerException.class, () -> GuavaHasher.wrap(null));
    }

    @Test
    public void testPutOverloadsHashAndChain() {
        byte[] bytes = { 1, 2, 3, 4, 5 };
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] { 6, 7, 8 });
        char[] chars = { 'A', 'B', 'C' };
        Person person = new Person("Alice", 30);
        Funnel<Person> funnel = (from, into) -> into.putString(from.name, StandardCharsets.UTF_8).putInt(from.age);

        GuavaHasher hasher = wrapMurmur();
        Hasher chained = hasher.put((byte) 42)
                .put(bytes)
                .put(bytes, 1, 3)
                .put(buffer)
                .put((short) 10)
                .put(20)
                .put(30L)
                .put(40.5f)
                .put(50.5)
                .put(true)
                .put('Z')
                .put(chars)
                .put(chars, 1, 1)
                .put("text")
                .put("text", StandardCharsets.UTF_8)
                .put(person, funnel);
        assertSame(hasher, chained);
        assertEquals(128, chained.hash().bits());

        assertEquals(wrapMurmur().put(bytes).hash(), wrapMurmur().put(bytes, 0, bytes.length).hash());
        assertEquals(wrapMurmur().put("hello".toCharArray()).hash(), wrapMurmur().put("hello".toCharArray(), 0, 5).hash());
        assertNotEquals(wrapMurmur().put("first").put("second").hash(), wrapMurmur().put("second").put("first").hash());
        assertNotEquals(wrapMurmur().put("Test", StandardCharsets.UTF_8).hash(), wrapMurmur().put("Test", StandardCharsets.UTF_16).hash());
        assertEquals(wrapMurmur().put(true).hash(), wrapMurmur().put(true).hash());
        assertNotEquals(wrapMurmur().put(true).hash(), wrapMurmur().put(false).hash());
        assertNotNull(wrapMurmur().put(new byte[0]).put(new char[0]).put("").put(ByteBuffer.allocate(0)).hash());
        assertNotNull(wrapMurmur().put((char[]) null).hash());
    }

    @Test
    public void testPutSpecialValuesAndBounds() {
        assertNotNull(wrapMurmur().put(Byte.MIN_VALUE).put(Short.MIN_VALUE).put(Integer.MIN_VALUE).put(Long.MIN_VALUE).hash());
        assertNotNull(wrapMurmur().put(Float.NaN).put(Float.POSITIVE_INFINITY).put(-0.0f).put(Double.NaN).put(Double.NEGATIVE_INFINITY).hash());
        assertNotNull(wrapMurmur().put('\n').put('\0').put('中').put(Character.MAX_VALUE).hash());

        ByteBuffer sliced = ByteBuffer.wrap(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        sliced.position(2);
        sliced.limit(7);
        assertNotNull(wrapMurmur().put(sliced).hash());

        ByteBuffer direct = ByteBuffer.allocateDirect(10);
        direct.put(new byte[] { 1, 2, 3, 4, 5 }).flip();
        assertNotNull(wrapMurmur().put(direct).hash());

        char[] chars = { 'a', 'b', 'c' };
        GuavaHasher hasher = wrapMurmur();
        assertThrows(IndexOutOfBoundsException.class, () -> hasher.put(chars, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> hasher.put(chars, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> hasher.put(chars, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> wrapMurmur().put(chars, 0, -1));
    }

    @Test
    public void testFarmHashPutByteArrayHugeLenIsIndexOutOfBounds() {
        assertIndexOutOfBoundsNotOom(() -> farmHasher().put(new byte[3], 2, Integer.MAX_VALUE));
        assertIndexOutOfBoundsNotOom(() -> farmHasher().put(new byte[3], 0, Integer.MAX_VALUE));
        assertIndexOutOfBoundsNotOom(() -> farmHasher().put(new byte[3], 0, Integer.MAX_VALUE - 1));
        assertIndexOutOfBoundsNotOom(() -> concatFarmSha256Hasher().put(new byte[3], 2, Integer.MAX_VALUE));
        assertIndexOutOfBoundsNotOom(() -> concatFarmSha256Hasher().put(new byte[3], 0, Integer.MAX_VALUE));
        assertThrows(IndexOutOfBoundsException.class, () -> farmHasher().put(new byte[3], 0, 200_000_000));
        assertThrows(IndexOutOfBoundsException.class, () -> concatFarmSha256Hasher().put(new byte[3], 0, 200_000_000));

        assertThrows(IndexOutOfBoundsException.class, () -> farmHasher().put(new byte[3], 1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> farmHasher().put(new byte[3], 3, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> farmHasher().put(new byte[3], -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> farmHasher().put(new byte[3], 0, -1));
        assertThrows(NullPointerException.class, () -> farmHasher().put((byte[]) null, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> concatFarmSha256Hasher().put(new byte[3], 1, 3));
        assertThrows(NullPointerException.class, () -> concatFarmSha256Hasher().put((byte[]) null, 0, 0));

        byte[] bytes = { 9, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        byte[] slice = java.util.Arrays.copyOfRange(bytes, 2, 7);
        assertEquals(farmHasher().put(slice).hash(), farmHasher().put(bytes, 2, 5).hash());
        assertEquals(farmHasher().put(new byte[0]).hash(), farmHasher().put(bytes, 0, 0).hash());
        assertEquals(com.google.common.hash.Hashing.farmHashFingerprint64().hashBytes(bytes, 2, 5), farmHasher().put(bytes, 2, 5).hash());

        GuavaHasher sha = GuavaHasher.wrap(com.google.common.hash.Hashing.sha256().newHasher());
        assertThrows(IndexOutOfBoundsException.class, () -> sha.put(new byte[3], 2, Integer.MAX_VALUE));
        assertThrows(IndexOutOfBoundsException.class, () -> sha.put(new byte[3], 0, 4));
        assertThrows(NullPointerException.class, () -> sha.put((byte[]) null, 0, 0));
    }

    // ---- perf review 2026-09-26 G002 begin ----
    // G002-01: put(char[], int, int) must feed exactly the bytes of one putChar per character, for short and bulk ranges
    @Test
    public void testPutCharArrayRange_matchesPerCharPutForAllFunctionsAndLengths() {
        final com.google.common.hash.HashFunction[] functions = { com.google.common.hash.Hashing.murmur3_128(), com.google.common.hash.Hashing.murmur3_128(7),
                com.google.common.hash.Hashing.murmur3_32_fixed(), com.google.common.hash.Hashing.sipHash24(), com.google.common.hash.Hashing.sha256(),
                com.google.common.hash.Hashing.sha512(), com.google.common.hash.Hashing.crc32(), com.google.common.hash.Hashing.crc32c(),
                com.google.common.hash.Hashing.adler32(), com.google.common.hash.Hashing.farmHashFingerprint64(),
                com.google.common.hash.Hashing.hmacSha256(new byte[] { 1, 2, 3 }), com.google.common.hash.Hashing.goodFastHash(256),
                com.google.common.hash.Hashing.concatenating(com.google.common.hash.Hashing.farmHashFingerprint64(), com.google.common.hash.Hashing.sha256()) };
        final int[] lengths = { 0, 1, 15, 16, 17, 63, 511, 512, 513, 1024, 1025, 3001 };
        final java.util.Random random = new java.util.Random(20260926L);

        for (final com.google.common.hash.HashFunction function : functions) {
            for (final int length : lengths) {
                final int offset = length % 5;
                final char[] chars = new char[offset + length + 3];

                for (int i = 0; i < chars.length; i++) {
                    chars[i] = (char) random.nextInt(Character.MAX_VALUE + 1);
                }

                chars[offset] = Character.MAX_VALUE;
                chars[chars.length - 1] = '\0';

                final com.google.common.hash.Hasher expected = function.newHasher().putInt(length);

                for (int i = offset; i < offset + length; i++) {
                    expected.putChar(chars[i]);
                }

                expected.putLong(-1L);

                final com.google.common.hash.HashCode actual = GuavaHasher.wrap(function.newHasher()).put(length).put(chars, offset, length).put(-1L).hash();
                assertEquals(expected.hash(), actual, function + " length " + length);

                final char[] exact = java.util.Arrays.copyOfRange(chars, offset, offset + length);
                assertEquals(function.hashUnencodedChars(new String(exact)), GuavaHasher.wrap(function.newHasher()).put(exact).hash(),
                        function + " length " + length);
            }
        }
    }

    // G002-01: range validation still happens before anything is hashed, also for ranges long enough for the bulk path
    @Test
    public void testPutCharArrayRange_bulkLengthBoundsCheckedFirst() {
        final char[] chars = new char[100];
        assertThrows(IndexOutOfBoundsException.class, () -> wrapMurmur().put(chars, 1, 100));
        assertThrows(IndexOutOfBoundsException.class, () -> wrapMurmur().put(chars, 0, Integer.MAX_VALUE));
        assertThrows(IndexOutOfBoundsException.class, () -> wrapMurmur().put((char[]) null, 0, 16));
        assertThrows(IllegalArgumentException.class, () -> wrapMurmur().put(chars, 0, -16));
        assertEquals(wrapMurmur().put(chars, 0, 0).hash(), wrapMurmur().put((char[]) null, 0, 0).hash());
    }
    // ---- perf review 2026-09-26 G002 end ----

    // ---- deep review 2026-11-02 G002 begin ----
    @SuppressWarnings("deprecation")
    private static java.util.List<Object[]> unencodedCharsFunctionPairs() {
        final byte[] key = { 9, 8, 7, 6, 5 };
        final java.util.List<Object[]> pairs = new java.util.ArrayList<>();
        // byte-oriented functions (bulk put(CharSequence) path) ...
        pairs.add(new Object[] { Hashing.md5(), com.google.common.hash.Hashing.md5() });
        pairs.add(new Object[] { Hashing.sha1(), com.google.common.hash.Hashing.sha1() });
        pairs.add(new Object[] { Hashing.sha256(), com.google.common.hash.Hashing.sha256() });
        pairs.add(new Object[] { Hashing.sha384(), com.google.common.hash.Hashing.sha384() });
        pairs.add(new Object[] { Hashing.sha512(), com.google.common.hash.Hashing.sha512() });
        pairs.add(new Object[] { Hashing.crc32(), com.google.common.hash.Hashing.crc32() });
        pairs.add(new Object[] { Hashing.crc32c(), com.google.common.hash.Hashing.crc32c() });
        pairs.add(new Object[] { Hashing.adler32(), com.google.common.hash.Hashing.adler32() });
        pairs.add(new Object[] { Hashing.hmacMd5(key), com.google.common.hash.Hashing.hmacMd5(key) });
        pairs.add(new Object[] { Hashing.hmacSha1(key), com.google.common.hash.Hashing.hmacSha1(key) });
        pairs.add(new Object[] { Hashing.hmacSha256(key), com.google.common.hash.Hashing.hmacSha256(key) });
        pairs.add(new Object[] { Hashing.hmacSha512(key), com.google.common.hash.Hashing.hmacSha512(key) });
        pairs.add(new Object[] { Hashing.hmacSha256(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256")), com.google.common.hash.Hashing.hmacSha256(key) });
        // ... and the others (plain delegation)
        pairs.add(new Object[] { Hashing.murmur3_32(), com.google.common.hash.Hashing.murmur3_32_fixed() });
        pairs.add(new Object[] { Hashing.murmur3_128(7), com.google.common.hash.Hashing.murmur3_128(7) });
        pairs.add(new Object[] { Hashing.sipHash24(), com.google.common.hash.Hashing.sipHash24() });
        pairs.add(new Object[] { Hashing.farmHashFingerprint64(), com.google.common.hash.Hashing.farmHashFingerprint64() });
        pairs.add(new Object[] { Hashing.concatenating(Hashing.sha256(), Hashing.crc32()),
                com.google.common.hash.Hashing.concatenating(com.google.common.hash.Hashing.sha256(), com.google.common.hash.Hashing.crc32()) });
        return pairs;
    }

    // G002-01: pin - hash(CharSequence) / put(CharSequence) equal Guava's hashUnencodedChars / putUnencodedChars for every
    // function, input type and length around the bulk threshold (16) and chunk size (512), incl. unpaired surrogates.
    @Test
    public void testPutCharSequence_matchesGuavaUnencodedCharsAcrossBulkThreshold() {
        final java.util.Random random = new java.util.Random(20261102L);
        final int[] lengths = { 0, 1, 2, 15, 16, 17, 31, 64, 511, 512, 513, 1024, 1025, 2000 };

        for (final Object[] pair : unencodedCharsFunctionPairs()) {
            final HashFunction function = (HashFunction) pair[0];
            final com.google.common.hash.HashFunction guavaFunction = (com.google.common.hash.HashFunction) pair[1];

            for (final int length : lengths) {
                final char[] chars = new char[length];

                for (int i = 0; i < length; i++) {
                    final int kind = random.nextInt(3);
                    chars[i] = kind == 0 ? (char) random.nextInt(128) : kind == 1 ? (char) (0xD800 + random.nextInt(0x800)) : (char) random.nextInt(65536);
                }

                final String str = new String(chars);
                final com.google.common.hash.HashCode expected = guavaFunction.hashUnencodedChars(str);
                final com.google.common.hash.HashCode expectedMixed = guavaFunction.newHasher()
                        .putInt(7)
                        .putUnencodedChars(str)
                        .putChar('x')
                        .putUnencodedChars(str)
                        .hash();

                for (final CharSequence input : new CharSequence[] { str, new StringBuilder(str), java.nio.CharBuffer.wrap(chars) }) {
                    final String msg = function + " length=" + length + " " + input.getClass().getSimpleName();
                    assertEquals(expected, function.hash(input), msg);
                    assertEquals(expected, function.newHasher().put(input).hash(), msg);
                    assertEquals(expected, function.newHasher(3).put(input).hash(), msg);
                    assertEquals(expected, function.newHasher().put(chars).hash(), msg);
                    assertEquals(expectedMixed, function.newHasher().put(7).put(input).put('x').put(input).hash(), msg);
                }
            }

            assertThrows(NullPointerException.class, () -> function.hash((CharSequence) null));
            assertThrows(NullPointerException.class, () -> function.newHasher().put((CharSequence) null));
        }
    }

    // G002-01: pin - a CharSequence whose length * 2 overflows int keeps Guava's IllegalArgumentException from
    // hashUnencodedChars -> newHasher(negative) for the byte-oriented functions (thrown before any char is read).
    @Test
    @SuppressWarnings("deprecation")
    public void testHashCharSequence_lengthTimesTwoOverflowStillIllegalArgument() {
        final CharSequence huge = new CharSequence() {
            @Override
            public int length() {
                return 1 << 30;
            }

            @Override
            public char charAt(final int index) {
                throw new AssertionError("must not be read");
            }

            @Override
            public CharSequence subSequence(final int start, final int end) {
                throw new UnsupportedOperationException();
            }
        };

        for (final HashFunction function : new HashFunction[] { Hashing.md5(), Hashing.sha256(), Hashing.crc32(), Hashing.crc32c(), Hashing.adler32(),
                Hashing.hmacSha256(new byte[] { 1 }) }) {
            assertThrows(IllegalArgumentException.class, () -> function.hash(huge), function.toString());
        }
    }
    // ---- deep review 2026-11-02 G002 end ----

    // ---- deep review 2026-11-02b G002 begin ----
    // G002-01: pin - put(char[]) / put(char[], int, int) of the Murmur3_32 based functions (murmur3_32(), murmur3_32(seed),
    // goodFastHash(<= 32); goodFastHash(33) as a non-Murmur3_32 control) equal one Guava putChar per char for every length
    // (incl. the bulk threshold 16 and the chunk size 512), check the range before anything is hashed and treat null as empty.
    @Test
    public void testPutCharArray_murmur3_32FamilyMatchesPerCharPut_r116() {
        final Object[][] pairs = { { Hashing.murmur3_32(), com.google.common.hash.Hashing.murmur3_32_fixed() },
                { Hashing.murmur3_32(42), com.google.common.hash.Hashing.murmur3_32_fixed(42) },
                { Hashing.goodFastHash(32), com.google.common.hash.Hashing.goodFastHash(32) },
                { Hashing.goodFastHash(1), com.google.common.hash.Hashing.goodFastHash(1) },
                { Hashing.goodFastHash(33), com.google.common.hash.Hashing.goodFastHash(33) } };
        final int[] lengths = { 0, 1, 2, 15, 16, 17, 64, 511, 512, 513, 1025, 3001 };
        final java.util.Random random = new java.util.Random(202611022L);

        for (final Object[] pair : pairs) {
            final HashFunction function = (HashFunction) pair[0];
            final com.google.common.hash.HashFunction guavaFunction = (com.google.common.hash.HashFunction) pair[1];

            for (final int length : lengths) {
                final int offset = length % 3;
                final char[] chars = new char[offset + length + 2];

                for (int i = 0; i < chars.length; i++) {
                    chars[i] = (char) random.nextInt(Character.MAX_VALUE + 1);
                }

                final String msg = function + " length " + length;
                final com.google.common.hash.Hasher expected = guavaFunction.newHasher().putInt(length);

                for (int i = offset; i < offset + length; i++) {
                    expected.putChar(chars[i]);
                }

                expected.putLong(-1L);
                assertEquals(expected.hash(), function.newHasher().put(length).put(chars, offset, length).put(-1L).hash(), msg);

                final char[] exact = java.util.Arrays.copyOfRange(chars, offset, offset + length);
                assertEquals(guavaFunction.hashUnencodedChars(new String(exact)), function.newHasher().put(exact).hash(), msg);
                assertEquals(function.hash(new String(exact)), function.newHasher(7).put(exact).hash(), msg);
            }

            final char[] chars = new char[100];
            assertThrows(IndexOutOfBoundsException.class, () -> function.newHasher().put(chars, 1, 100));
            assertThrows(IndexOutOfBoundsException.class, () -> function.newHasher().put(chars, -1, 16));
            assertThrows(IndexOutOfBoundsException.class, () -> function.newHasher().put(chars, 0, Integer.MAX_VALUE));
            assertThrows(IndexOutOfBoundsException.class, () -> function.newHasher().put((char[]) null, 0, 16));
            assertThrows(IllegalArgumentException.class, () -> function.newHasher().put(chars, 0, -16));
            assertEquals(function.newHasher().hash(), function.newHasher().put((char[]) null).hash());
            assertEquals(function.newHasher().hash(), function.newHasher().put((char[]) null, 0, 0).hash());
        }
    }
    // ---- deep review 2026-11-02b G002 end ----
}
