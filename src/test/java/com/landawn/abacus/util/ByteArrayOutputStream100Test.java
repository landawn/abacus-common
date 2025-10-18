package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ByteArrayOutputStream100Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        assertEquals(0, baos.size());
        assertEquals(32, baos.capacity());
    }

    @Test
    public void testConstructorWithInitCapacity() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(100);
        assertEquals(0, baos.size());
        assertEquals(100, baos.capacity());
    }

    @Test
    public void testConstructorWithNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new ByteArrayOutputStream(-1));
    }

    @Test
    public void testWriteInt() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(65);
        baos.write(0x42);
        baos.write(300);

        assertEquals(3, baos.size());
        byte[] result = baos.toByteArray();
        assertEquals(65, result[0]);
        assertEquals(66, result[1]);
        assertEquals(44, result[2]);
    }

    @Test
    public void testWriteByteArray() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] data = "Hello".getBytes();
        baos.write(data, 0, data.length);

        assertEquals(5, baos.size());
        assertArrayEquals("Hello".getBytes(), baos.toByteArray());
    }

    @Test
    public void testWriteByteArrayPortion() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] data = "Hello World".getBytes();
        baos.write(data, 6, 5);

        assertEquals(5, baos.size());
        assertArrayEquals("World".getBytes(), baos.toByteArray());
    }

    @Test
    public void testWriteByteArrayInvalidRange() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] data = "Hello".getBytes();

        assertThrows(IndexOutOfBoundsException.class, () -> baos.write(data, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> baos.write(data, 0, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> baos.write(data, 5, 1));
    }

    @Test
    public void testWriteByte() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write((byte) 0xFF);
        baos.write((byte) 0x00);
        baos.write((byte) 0x7F);

        assertEquals(3, baos.size());
        byte[] result = baos.toByteArray();
        assertEquals((byte) 0xFF, result[0]);
        assertEquals((byte) 0x00, result[1]);
        assertEquals((byte) 0x7F, result[2]);
    }

    @Test
    public void testWriteTo() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write("Hello".getBytes());

        java.io.ByteArrayOutputStream target = new java.io.ByteArrayOutputStream();
        baos.writeTo(target);

        assertArrayEquals("Hello".getBytes(), target.toByteArray());
    }

    @Test
    public void testWriteToNull() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write("Hello".getBytes());

        assertThrows(NullPointerException.class, () -> baos.writeTo(null));
    }

    @Test
    public void testCapacity() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(50);
        assertEquals(50, baos.capacity());

        byte[] data = new byte[100];
        baos.write(data, 0, data.length);

        assertTrue(baos.capacity() >= 100);
    }

    @Test
    public void testCapacityWithNullBuffer() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.buf = null;
        assertEquals(0, baos.capacity());
    }

    @Test
    public void testArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write("Hello".getBytes());

        byte[] internalArray = baos.array();
        assertEquals(baos.capacity(), internalArray.length);

        byte[] validData = new byte[5];
        System.arraycopy(internalArray, 0, validData, 0, 5);
        assertArrayEquals("Hello".getBytes(), validData);
    }

    @Test
    public void testSize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        assertEquals(0, baos.size());

        baos.write("Hello".getBytes());
        assertEquals(5, baos.size());

        baos.write(" World".getBytes());
        assertEquals(11, baos.size());
    }

    @Test
    public void testReset() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write("Hello".getBytes());
        assertEquals(5, baos.size());

        baos.reset();
        assertEquals(0, baos.size());

        assertTrue(baos.capacity() > 0);
    }

    @Test
    public void testToByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write("Hello World".getBytes());

        byte[] result = baos.toByteArray();
        assertArrayEquals("Hello World".getBytes(), result);

        result[0] = 'h';
        byte[] result2 = baos.toByteArray();
        assertEquals('H', result2[0]);
    }

    @Test
    public void testToByteArrayEmpty() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] result = baos.toByteArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testToString() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write("Hello World".getBytes());

        assertEquals("Hello World", baos.toString());
    }

    @Test
    public void testToStringEmpty() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        assertEquals("", baos.toString());
    }

    @Test
    public void testToStringWithCharsetName() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String text = "Hello 世界";
        baos.write(text.getBytes("UTF-8"));

        assertEquals(text, baos.toString("UTF-8"));
    }

    @Test
    public void testToStringWithInvalidCharsetName() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write("Hello".getBytes());

        assertThrows(UnsupportedEncodingException.class, () -> baos.toString("INVALID-CHARSET"));
    }

    @Test
    public void testToStringWithCharset() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String text = "Hello 世界";
        baos.write(text.getBytes(StandardCharsets.UTF_8));

        assertEquals(text, baos.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void testToStringWithNullCharset() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write("Hello".getBytes());

        assertThrows(NullPointerException.class, () -> baos.toString((java.nio.charset.Charset) null));
    }

    @Test
    public void testClose() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write("Hello".getBytes());

        baos.close();

        baos.write(" World".getBytes());
        assertEquals("Hello World", baos.toString());
    }

    @Test
    public void testCapacityGrowth() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(10);
        assertEquals(10, baos.capacity());

        byte[] data = new byte[20];
        baos.write(data, 0, data.length);

        assertTrue(baos.capacity() >= 20);
        assertEquals(20, baos.size());
    }

    @Test
    public void testLargeWrite() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] largeData = new byte[10000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        baos.write(largeData, 0, largeData.length);

        assertEquals(10000, baos.size());
        assertArrayEquals(largeData, baos.toByteArray());
    }

    @Test
    public void testSequentialWrites() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (int i = 0; i < 256; i++) {
            baos.write(i);
        }

        assertEquals(256, baos.size());
        byte[] result = baos.toByteArray();
        for (int i = 0; i < 256; i++) {
            assertEquals((byte) i, result[i]);
        }
    }

    @Test
    public void testMixedWrites() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        baos.write(65);
        baos.write("BC".getBytes());
        baos.write((byte) 'D');
        baos.write("EFGH".getBytes(), 1, 2);

        assertEquals("ABCDFG", baos.toString());
        assertEquals(6, baos.size());
    }
}
