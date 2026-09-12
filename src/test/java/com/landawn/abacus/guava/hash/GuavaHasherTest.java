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
}
