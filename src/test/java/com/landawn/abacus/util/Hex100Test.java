package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Hex100Test extends TestBase {

    @Test
    public void testEncodeByteArray() {
        byte[] empty = new byte[0];
        char[] emptyResult = Hex.encode(empty);
        Assertions.assertEquals(0, emptyResult.length);

        byte[] single = { 0x4A };
        char[] singleResult = Hex.encode(single);
        Assertions.assertArrayEquals(new char[] { '4', 'a' }, singleResult);

        byte[] data = { 0x48, 0x65, 0x6C, 0x6C, 0x6F };
        char[] result = Hex.encode(data);
        Assertions.assertArrayEquals("48656c6c6f".toCharArray(), result);

        byte[] negative = { (byte) 0xFF, 0x00, 0x7F };
        char[] negResult = Hex.encode(negative);
        Assertions.assertArrayEquals("ff007f".toCharArray(), negResult);
    }

    @Test
    public void testEncodeByteArrayWithCase() {
        byte[] data = { 0x01, 0x23, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF };

        char[] lower = Hex.encode(data, true);
        Assertions.assertArrayEquals("0123abcdef".toCharArray(), lower);

        char[] upper = Hex.encode(data, false);
        Assertions.assertArrayEquals("0123ABCDEF".toCharArray(), upper);
    }

    @Test
    public void testEncodeToString() {
        byte[] empty = new byte[0];
        String emptyResult = Hex.encodeToString(empty);
        Assertions.assertEquals("", emptyResult);

        byte[] data = { 0x48, 0x65, 0x6C, 0x6C, 0x6F };
        String result = Hex.encodeToString(data);
        Assertions.assertEquals("48656c6c6f", result);

        byte[] allBytes = { 0x00, 0x0F, 0x10, 0x1F, (byte) 0xF0, (byte) 0xFF };
        String allResult = Hex.encodeToString(allBytes);
        Assertions.assertEquals("000f101ff0ff", allResult);
    }

    @Test
    public void testEncodeToStringWithCase() {
        byte[] data = { (byte) 0xFF, 0x00, 0x42 };

        String lower = Hex.encodeToString(data, true);
        Assertions.assertEquals("ff0042", lower);

        String upper = Hex.encodeToString(data, false);
        Assertions.assertEquals("FF0042", upper);
    }

    @Test
    public void testDecodeString() {
        byte[] empty = Hex.decode("");
        Assertions.assertEquals(0, empty.length);

        byte[] lower = Hex.decode("48656c6c6f");
        Assertions.assertArrayEquals(new byte[] { 0x48, 0x65, 0x6C, 0x6C, 0x6F }, lower);

        byte[] upper = Hex.decode("48656C6C6F");
        Assertions.assertArrayEquals(new byte[] { 0x48, 0x65, 0x6C, 0x6C, 0x6F }, upper);

        byte[] mixed = Hex.decode("DeadBeef");
        Assertions.assertArrayEquals(new byte[] { (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF }, mixed);

        byte[] ff = Hex.decode("FF00");
        Assertions.assertArrayEquals(new byte[] { (byte) 0xFF, 0x00 }, ff);
    }

    @Test
    public void testDecodeCharArray() {
        char[] empty = new char[0];
        byte[] emptyResult = Hex.decode(empty);
        Assertions.assertEquals(0, emptyResult.length);

        char[] hex = { '4', '8', '6', '5', '6', 'C', '6', 'C', '6', 'F' };
        byte[] result = Hex.decode(hex);
        Assertions.assertArrayEquals(new byte[] { 0x48, 0x65, 0x6C, 0x6C, 0x6F }, result);
    }

    @Test
    public void testDecodeInvalidInput() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.decode("48656");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.decode(new char[] { '4', '8', '6' });
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.decode("48GH");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.decode("48 65");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.decode("48-65");
        });
    }

    @Test
    public void testRoundTripConversion() {
        byte[][] testData = { { 0x00, 0x01, 0x02, 0x03 }, { (byte) 0xFE, (byte) 0xDC, (byte) 0xBA, (byte) 0x98 },
                { 0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0 }, "Hello, World!".getBytes() };

        for (byte[] original : testData) {
            String hexLower = Hex.encodeToString(original, true);
            byte[] decodedLower = Hex.decode(hexLower);
            Assertions.assertArrayEquals(original, decodedLower);

            String hexUpper = Hex.encodeToString(original, false);
            byte[] decodedUpper = Hex.decode(hexUpper);
            Assertions.assertArrayEquals(original, decodedUpper);

            char[] hexChars = Hex.encode(original);
            byte[] decodedChars = Hex.decode(hexChars);
            Assertions.assertArrayEquals(original, decodedChars);
        }
    }

    @Test
    public void testAllByteValues() {
        byte[] allBytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            allBytes[i] = (byte) i;
        }

        String hex = Hex.encodeToString(allBytes);
        byte[] decoded = Hex.decode(hex);

        Assertions.assertArrayEquals(allBytes, decoded);
    }

    @Test
    public void testLargeData() {
        byte[] largeData = new byte[1000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        String hex = Hex.encodeToString(largeData);
        Assertions.assertEquals(2000, hex.length());

        byte[] decoded = Hex.decode(hex);
        Assertions.assertArrayEquals(largeData, decoded);
    }
}
