package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Hex2025Test extends TestBase {

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

        byte[] zeros = { 0x00, 0x00, 0x00 };
        char[] zerosResult = Hex.encode(zeros);
        Assertions.assertArrayEquals("000000".toCharArray(), zerosResult);
    }

    @Test
    public void testEncodeByteArrayNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.encode(null);
        });
    }

    @Test
    public void testEncodeByteArrayWithCase() {
        byte[] data = { 0x01, 0x23, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF };

        char[] lower = Hex.encode(data, true);
        Assertions.assertArrayEquals("0123abcdef".toCharArray(), lower);

        char[] upper = Hex.encode(data, false);
        Assertions.assertArrayEquals("0123ABCDEF".toCharArray(), upper);

        byte[] oneByte = { (byte) 0xAF };
        char[] oneLower = Hex.encode(oneByte, true);
        Assertions.assertArrayEquals("af".toCharArray(), oneLower);

        char[] oneUpper = Hex.encode(oneByte, false);
        Assertions.assertArrayEquals("AF".toCharArray(), oneUpper);
    }

    @Test
    public void testEncodeByteArrayWithCaseNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.encode(null, true);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.encode(null, false);
        });
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

        byte[] boundary = { Byte.MIN_VALUE, Byte.MAX_VALUE };
        String boundaryResult = Hex.encodeToString(boundary);
        Assertions.assertEquals("807f", boundaryResult);
    }

    @Test
    public void testEncodeToStringNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.encodeToString(null);
        });
    }

    @Test
    public void testEncodeToStringWithCase() {
        byte[] data = { (byte) 0xFF, 0x00, 0x42 };

        String lower = Hex.encodeToString(data, true);
        Assertions.assertEquals("ff0042", lower);

        String upper = Hex.encodeToString(data, false);
        Assertions.assertEquals("FF0042", upper);

        byte[] complex = { (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF };
        String complexLower = Hex.encodeToString(complex, true);
        Assertions.assertEquals("deadbeef", complexLower);

        String complexUpper = Hex.encodeToString(complex, false);
        Assertions.assertEquals("DEADBEEF", complexUpper);
    }

    @Test
    public void testEncodeToStringWithCaseNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.encodeToString(null, true);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.encodeToString(null, false);
        });
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

        byte[] zeros = Hex.decode("000000");
        Assertions.assertArrayEquals(new byte[] { 0x00, 0x00, 0x00 }, zeros);

        byte[] single = Hex.decode("A5");
        Assertions.assertArrayEquals(new byte[] { (byte) 0xA5 }, single);
    }

    @Test
    public void testDecodeStringNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.decode((String) null);
        });
    }

    @Test
    public void testDecodeStringInvalidInput() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.decode("48656");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.decode("F");
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

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.decode("ZZZZ");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.decode("12!@");
        });
    }

    @Test
    public void testDecodeCharArray() {
        char[] empty = new char[0];
        byte[] emptyResult = Hex.decode(empty);
        Assertions.assertEquals(0, emptyResult.length);

        char[] hex = { '4', '8', '6', '5', '6', 'C', '6', 'C', '6', 'F' };
        byte[] result = Hex.decode(hex);
        Assertions.assertArrayEquals(new byte[] { 0x48, 0x65, 0x6C, 0x6C, 0x6F }, result);

        char[] upper = { 'F', 'F', '0', '0' };
        byte[] upperResult = Hex.decode(upper);
        Assertions.assertArrayEquals(new byte[] { (byte) 0xFF, 0x00 }, upperResult);

        char[] mixed = { 'A', 'b', 'C', 'd' };
        byte[] mixedResult = Hex.decode(mixed);
        Assertions.assertArrayEquals(new byte[] { (byte) 0xAB, (byte) 0xCD }, mixedResult);
    }

    @Test
    public void testDecodeCharArrayNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.decode((char[]) null);
        });
    }

    @Test
    public void testDecodeCharArrayInvalidInput() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.decode(new char[] { '4', '8', '6' });
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.decode(new char[] { 'A' });
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.decode(new char[] { '4', 'G' });
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.decode(new char[] { 'X', 'Y' });
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.decode(new char[] { '1', ' ' });
        });
    }

    @Test
    public void testRoundTripConversion() {
        byte[][] testData = { { 0x00, 0x01, 0x02, 0x03 }, { (byte) 0xFE, (byte) 0xDC, (byte) 0xBA, (byte) 0x98 },
                { 0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0 }, "Hello, World!".getBytes(), "Test123!@#".getBytes() };

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

        String hexLower = Hex.encodeToString(allBytes, true);
        byte[] decodedLower = Hex.decode(hexLower);
        Assertions.assertArrayEquals(allBytes, decodedLower);

        String hexUpper = Hex.encodeToString(allBytes, false);
        byte[] decodedUpper = Hex.decode(hexUpper);
        Assertions.assertArrayEquals(allBytes, decodedUpper);
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

        char[] hexChars = Hex.encode(largeData);
        Assertions.assertEquals(2000, hexChars.length);
        byte[] decodedChars = Hex.decode(hexChars);
        Assertions.assertArrayEquals(largeData, decodedChars);
    }

    @Test
    public void testSpecificBytePatterns() {
        byte[] pattern1 = { 0x00, (byte) 0xFF };
        String hex1 = Hex.encodeToString(pattern1);
        Assertions.assertEquals("00ff", hex1);
        Assertions.assertArrayEquals(pattern1, Hex.decode(hex1));

        byte[] pattern2 = { 0x0F, (byte) 0xF0 };
        String hex2 = Hex.encodeToString(pattern2);
        Assertions.assertEquals("0ff0", hex2);
        Assertions.assertArrayEquals(pattern2, Hex.decode(hex2));

        byte[] pattern3 = { 0x55, (byte) 0xAA };
        String hex3 = Hex.encodeToString(pattern3);
        Assertions.assertEquals("55aa", hex3);
        Assertions.assertArrayEquals(pattern3, Hex.decode(hex3));
    }

    @Test
    public void testCaseSensitivityInDecoding() {
        String lower = "abcdef123456";
        String upper = "ABCDEF123456";
        String mixed = "AbCdEf123456";

        byte[] resultLower = Hex.decode(lower);
        byte[] resultUpper = Hex.decode(upper);
        byte[] resultMixed = Hex.decode(mixed);

        Assertions.assertArrayEquals(resultLower, resultUpper);
        Assertions.assertArrayEquals(resultLower, resultMixed);
        Assertions.assertArrayEquals(new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF, 0x12, 0x34, 0x56 }, resultLower);
    }

    @Test
    public void testEncodingOutputLength() {
        for (int i = 0; i < 10; i++) {
            byte[] input = new byte[i];
            char[] output = Hex.encode(input);
            Assertions.assertEquals(i * 2, output.length);

            String stringOutput = Hex.encodeToString(input);
            Assertions.assertEquals(i * 2, stringOutput.length());
        }
    }

    @Test
    public void testDecodingOutputLength() {
        for (int i = 0; i < 10; i++) {
            String input = "00".repeat(i);
            byte[] output = Hex.decode(input);
            Assertions.assertEquals(i, output.length);

            char[] charInput = input.toCharArray();
            byte[] charOutput = Hex.decode(charInput);
            Assertions.assertEquals(i, charOutput.length);
        }
    }
}
