package com.landawn.abacus.guava.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;
import com.google.common.hash.PrimitiveSink;
import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Hasher100Test extends TestBase {

    private Hasher hasher;

    @BeforeEach
    public void setUp() {
        hasher = Hashing.sha256().newHasher();
    }

    @Test
    public void testPutByte() {
        Hasher result = hasher.put((byte) 0xFF);
        assertSame(hasher, result);

        HashCode hash1 = hasher.hash();

        Hasher hasher2 = Hashing.sha256().newHasher();
        hasher2.put((byte) 0xFF);
        HashCode hash2 = hasher2.hash();

        assertEquals(hash1, hash2);

        Hasher hasher3 = Hashing.sha256().newHasher();
        hasher3.put((byte) 0xFF).put((byte) 0x00).put((byte) 0x42);
        assertNotNull(hasher3.hash());
    }

    @Test
    public void testPutByteArray() {
        byte[] data = "Hello".getBytes(StandardCharsets.UTF_8);
        Hasher result = hasher.put(data);
        assertSame(hasher, result);

        HashCode hash1 = hasher.hash();

        Hasher hasher2 = Hashing.sha256().newHasher();
        hasher2.put(data);
        HashCode hash2 = hasher2.hash();

        assertEquals(hash1, hash2);

        Hasher hasher3 = Hashing.sha256().newHasher();
        hasher3.put(new byte[0]);
        assertNotNull(hasher3.hash());

        Hasher hasher4 = Hashing.sha256().newHasher();
        assertThrows(NullPointerException.class, () -> hasher4.put((byte[]) null));
    }

    @Test
    public void testPutByteArrayWithOffsetAndLength() {
        byte[] buffer = "Hello World".getBytes();

        Hasher hasher1 = Hashing.sha256().newHasher();
        hasher1.put(buffer, 0, 5);
        HashCode hash1 = hasher1.hash();

        Hasher hasher2 = Hashing.sha256().newHasher();
        hasher2.put(buffer, 6, 5);
        HashCode hash2 = hasher2.hash();

        assertNotEquals(hash1, hash2);

        Hasher hasher3 = Hashing.sha256().newHasher();
        Hasher result = hasher3.put(buffer, 0, 5);
        assertSame(hasher3, result);

        Hasher hasher4 = Hashing.sha256().newHasher();
        hasher4.put(buffer, 0, 0);
        assertNotNull(hasher4.hash());

        Hasher hasher5 = Hashing.sha256().newHasher();
        assertThrows(IndexOutOfBoundsException.class, () -> hasher5.put(buffer, -1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> hasher5.put(buffer, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> hasher5.put(buffer, 0, buffer.length + 1));
        assertThrows(IndexOutOfBoundsException.class, () -> hasher5.put(buffer, buffer.length, 1));
        assertThrows(NullPointerException.class, () -> hasher5.put((byte[]) null, 0, 0));
    }

    @Test
    public void testPutByteBuffer() {
        byte[] data = "test data".getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(data);

        Hasher result = hasher.put(buffer);
        assertSame(hasher, result);
        assertEquals(data.length, buffer.position());

        HashCode hash1 = hasher.hash();

        Hasher hasher2 = Hashing.sha256().newHasher();
        hasher2.put(data);
        HashCode hash2 = hasher2.hash();

        assertEquals(hash1, hash2);

        ByteBuffer buffer2 = ByteBuffer.wrap(data);
        buffer2.position(5);
        Hasher hasher3 = Hashing.sha256().newHasher();
        hasher3.put(buffer2);
        assertNotNull(hasher3.hash());

        Hasher hasher4 = Hashing.sha256().newHasher();
        assertThrows(NullPointerException.class, () -> hasher4.put((ByteBuffer) null));
    }

    @Test
    public void testPutShort() {
        Hasher result = hasher.put((short) 12345);
        assertSame(hasher, result);

        HashCode hash1 = hasher.hash();

        Hasher hasher2 = Hashing.sha256().newHasher();
        hasher2.put((short) 12345);
        HashCode hash2 = hasher2.hash();

        assertEquals(hash1, hash2);

        Hasher hasher3 = Hashing.sha256().newHasher();
        hasher3.put(Short.MIN_VALUE).put(Short.MAX_VALUE).put((short) 0);
        assertNotNull(hasher3.hash());
    }

    @Test
    public void testPutInt() {
        Hasher result = hasher.put(42);
        assertSame(hasher, result);

        HashCode hash1 = hasher.hash();

        Hasher hasher2 = Hashing.sha256().newHasher();
        hasher2.put(42);
        HashCode hash2 = hasher2.hash();

        assertEquals(hash1, hash2);

        Hasher hasher3 = Hashing.sha256().newHasher();
        hasher3.put(Integer.MIN_VALUE).put(Integer.MAX_VALUE).put(0);
        assertNotNull(hasher3.hash());
    }

    @Test
    public void testPutLong() {
        Hasher result = hasher.put(1234567890L);
        assertSame(hasher, result);

        HashCode hash1 = hasher.hash();

        Hasher hasher2 = Hashing.sha256().newHasher();
        hasher2.put(1234567890L);
        HashCode hash2 = hasher2.hash();

        assertEquals(hash1, hash2);

        Hasher hasher3 = Hashing.sha256().newHasher();
        hasher3.put(Long.MIN_VALUE).put(Long.MAX_VALUE).put(0L);
        assertNotNull(hasher3.hash());
    }

    @Test
    public void testPutFloat() {
        Hasher result = hasher.put(3.14159f);
        assertSame(hasher, result);

        HashCode hash1 = hasher.hash();

        Hasher hasher2 = Hashing.sha256().newHasher();
        hasher2.put(3.14159f);
        HashCode hash2 = hasher2.hash();

        assertEquals(hash1, hash2);

        Hasher hasher3 = Hashing.sha256().newHasher();
        hasher3.put(Float.NaN).put(Float.POSITIVE_INFINITY).put(Float.NEGATIVE_INFINITY);
        assertNotNull(hasher3.hash());

        float f = 42.0f;
        int intBits = Float.floatToRawIntBits(f);

        Hasher hasher4 = Hashing.sha256().newHasher();
        hasher4.put(f);
        HashCode hash4 = hasher4.hash();

        Hasher hasher5 = Hashing.sha256().newHasher();
        hasher5.put(intBits);
        HashCode hash5 = hasher5.hash();

        assertEquals(hash4, hash5);
    }

    @Test
    public void testPutDouble() {
        Hasher result = hasher.put(Math.PI);
        assertSame(hasher, result);

        HashCode hash1 = hasher.hash();

        Hasher hasher2 = Hashing.sha256().newHasher();
        hasher2.put(Math.PI);
        HashCode hash2 = hasher2.hash();

        assertEquals(hash1, hash2);

        Hasher hasher3 = Hashing.sha256().newHasher();
        hasher3.put(Double.NaN).put(Double.POSITIVE_INFINITY).put(Double.NEGATIVE_INFINITY);
        assertNotNull(hasher3.hash());

        double d = 42.0;
        long longBits = Double.doubleToRawLongBits(d);

        Hasher hasher4 = Hashing.sha256().newHasher();
        hasher4.put(d);
        HashCode hash4 = hasher4.hash();

        Hasher hasher5 = Hashing.sha256().newHasher();
        hasher5.put(longBits);
        HashCode hash5 = hasher5.hash();

        assertEquals(hash4, hash5);
    }

    @Test
    public void testPutBoolean() {
        Hasher result = hasher.put(true);
        assertSame(hasher, result);

        HashCode hashTrue = hasher.hash();

        Hasher hasher2 = Hashing.sha256().newHasher();
        hasher2.put(false);
        HashCode hashFalse = hasher2.hash();

        assertNotEquals(hashTrue, hashFalse);

        Hasher hasher3 = Hashing.sha256().newHasher();
        hasher3.put((byte) 1);
        assertEquals(hashTrue, hasher3.hash());

        Hasher hasher4 = Hashing.sha256().newHasher();
        hasher4.put((byte) 0);
        assertEquals(hashFalse, hasher4.hash());
    }

    @Test
    public void testPutChar() {
        Hasher result = hasher.put('A');
        assertSame(hasher, result);

        HashCode hash1 = hasher.hash();

        Hasher hasher2 = Hashing.sha256().newHasher();
        hasher2.put('A');
        HashCode hash2 = hasher2.hash();

        assertEquals(hash1, hash2);

        Hasher hasher3 = Hashing.sha256().newHasher();
        hasher3.put('A').put('\n').put('中');
        assertNotNull(hasher3.hash());
    }

    @Test
    public void testPutCharArray() {
        char[] chars = { 's', 'e', 'c', 'r', 'e', 't' };
        Hasher result = hasher.put(chars);
        assertSame(hasher, result);

        HashCode hash1 = hasher.hash();

        Hasher hasher2 = Hashing.sha256().newHasher();
        hasher2.put(chars);
        HashCode hash2 = hasher2.hash();

        assertEquals(hash1, hash2);

        Hasher hasher3 = Hashing.sha256().newHasher();
        hasher3.put(new char[0]);
        assertNotNull(hasher3.hash());

        Hasher hasher4 = Hashing.sha256().newHasher();
        hasher4.put((char[]) null);
    }

    @Test
    public void testPutCharArrayWithOffsetAndLength() {
        char[] buffer = "Hello World".toCharArray();

        Hasher hasher1 = Hashing.sha256().newHasher();
        hasher1.put(buffer, 0, 5);
        HashCode hash1 = hasher1.hash();

        Hasher hasher2 = Hashing.sha256().newHasher();
        hasher2.put(buffer, 6, 5);
        HashCode hash2 = hasher2.hash();

        assertNotEquals(hash1, hash2);

        Hasher hasher3 = Hashing.sha256().newHasher();
        Hasher result = hasher3.put(buffer, 0, 5);
        assertSame(hasher3, result);

        Hasher hasher4 = Hashing.sha256().newHasher();
        hasher4.put(buffer, 0, 0);
        assertNotNull(hasher4.hash());

        Hasher hasher5 = Hashing.sha256().newHasher();
        assertThrows(IndexOutOfBoundsException.class, () -> hasher5.put(buffer, -1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> hasher5.put(buffer, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> hasher5.put(buffer, 0, buffer.length + 1));
        assertThrows(IndexOutOfBoundsException.class, () -> hasher5.put(buffer, buffer.length, 1));
        hasher5.put((char[]) null, 0, 0);
    }

    @Test
    public void testPutCharSequence() {
        CharSequence text = "fast hash";
        Hasher result = hasher.put(text);
        assertSame(hasher, result);

        HashCode hash1 = hasher.hash();

        Hasher hasher2 = Hashing.sha256().newHasher();
        hasher2.put(new StringBuilder("fast hash"));
        HashCode hash2 = hasher2.hash();

        assertEquals(hash1, hash2);

        Hasher hasher3 = Hashing.sha256().newHasher();
        hasher3.put("");
        assertNotNull(hasher3.hash());

        Hasher hasher4 = Hashing.sha256().newHasher();
        assertThrows(NullPointerException.class, () -> hasher4.put((CharSequence) null));
    }

    @Test
    public void testPutCharSequenceWithCharset() {
        String text = "Hello 世界";
        Hasher result = hasher.put(text, StandardCharsets.UTF_8);
        assertSame(hasher, result);

        HashCode hashUtf8 = hasher.hash();

        Hasher hasher2 = Hashing.sha256().newHasher();
        hasher2.put(text, StandardCharsets.UTF_16);
        HashCode hashUtf16 = hasher2.hash();

        assertNotEquals(hashUtf8, hashUtf16);

        Hasher hasher3 = Hashing.sha256().newHasher();
        hasher3.put(text, StandardCharsets.UTF_8);
        assertEquals(hashUtf8, hasher3.hash());

        Hasher hasher4 = Hashing.sha256().newHasher();
        assertThrows(NullPointerException.class, () -> hasher4.put(text, (Charset) null));
        assertThrows(NullPointerException.class, () -> hasher4.put((String) null, StandardCharsets.UTF_8));
    }

    @Test
    public void testPutObjectWithFunnel() {
        Person person = new Person("Alice", 30);

        Funnel<Person> personFunnel = new Funnel<Person>() {
            @Override
            public void funnel(Person from, PrimitiveSink into) {
                into.putString(from.name, StandardCharsets.UTF_8).putInt(from.age);
            }
        };

        Hasher result = hasher.put(person, personFunnel);
        assertSame(hasher, result);

        HashCode hash1 = hasher.hash();

        Hasher hasher2 = Hashing.sha256().newHasher();
        hasher2.put(person, personFunnel);
        HashCode hash2 = hasher2.hash();

        assertEquals(hash1, hash2);

        Hasher hasher3 = Hashing.sha256().newHasher();
        hasher3.put(person.name, StandardCharsets.UTF_8).put(person.age);
        HashCode hash3 = hasher3.hash();

        assertEquals(hash1, hash3);

        Hasher hasher4 = Hashing.sha256().newHasher();
        assertThrows(NullPointerException.class, () -> hasher4.put(person, null));
        assertThrows(NullPointerException.class, () -> hasher4.put(null, personFunnel));
    }

    @Test
    public void testHash() {
        hasher.put("test data".getBytes());
        HashCode hash1 = hasher.hash();
        assertNotNull(hash1);
        assertTrue(hash1.bits() > 0);

        Hasher hasher2 = Hashing.sha256().newHasher();
        HashCode hash2 = hasher2.put("data".getBytes()).put(42).put(true).put('X').hash();
        assertNotNull(hash2);

        Hasher hasher3 = Hashing.sha256().newHasher();
        HashCode hashEmpty = hasher3.hash();
        assertNotNull(hashEmpty);
    }

    @Test
    public void testComplexChaining() {
        HashCode hash = Hashing.sha256()
                .newHasher()
                .put((byte) 1)
                .put(new byte[] { 2, 3, 4 })
                .put(new byte[] { 5, 6, 7, 8 }, 1, 2)
                .put(ByteBuffer.wrap(new byte[] { 9, 10 }))
                .put((short) 11)
                .put(12)
                .put(13L)
                .put(14.0f)
                .put(15.0)
                .put(true)
                .put('A')
                .put(new char[] { 'B', 'C' })
                .put(new char[] { 'D', 'E', 'F' }, 0, 2)
                .put("text")
                .put("encoded", StandardCharsets.UTF_8)
                .hash();

        assertNotNull(hash);
    }

    @Test
    public void testConsistencyAcrossDataTypes() {

        HashCode hashTrue1 = Hashing.sha256().newHasher().put(true).hash();
        HashCode hashTrue2 = Hashing.sha256().newHasher().put((byte) 1).hash();
        assertEquals(hashTrue1, hashTrue2);

        float f = 123.456f;
        HashCode hashFloat = Hashing.sha256().newHasher().put(f).hash();
        HashCode hashIntBits = Hashing.sha256().newHasher().put(Float.floatToRawIntBits(f)).hash();
        assertEquals(hashFloat, hashIntBits);

        double d = 123.456789;
        HashCode hashDouble = Hashing.sha256().newHasher().put(d).hash();
        HashCode hashLongBits = Hashing.sha256().newHasher().put(Double.doubleToRawLongBits(d)).hash();
        assertEquals(hashDouble, hashLongBits);
    }

    private static class Person {
        final String name;
        final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }
}
