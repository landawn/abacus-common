package com.landawn.abacus.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Utf100Test extends TestBase {

    @Test
    public void testEncodedLength() {
        Assertions.assertEquals(11, Utf8.encodedLength("Hello World"));
        Assertions.assertEquals(0, Utf8.encodedLength(""));
        Assertions.assertEquals(1, Utf8.encodedLength("A"));
        Assertions.assertEquals(26, Utf8.encodedLength("abcdefghijklmnopqrstuvwxyz"));

        Assertions.assertEquals(2, Utf8.encodedLength("é"));
        Assertions.assertEquals(2, Utf8.encodedLength("ñ"));
        Assertions.assertEquals(4, Utf8.encodedLength("éé"));

        Assertions.assertEquals(3, Utf8.encodedLength("中"));
        Assertions.assertEquals(6, Utf8.encodedLength("中文"));
        Assertions.assertEquals(15, Utf8.encodedLength("こんにちは"));
        Assertions.assertEquals(12, Utf8.encodedLength("Hello 世界"));

        Assertions.assertEquals(4, Utf8.encodedLength("😀"));
        Assertions.assertEquals(4, Utf8.encodedLength("👋"));
        Assertions.assertEquals(10, Utf8.encodedLength("Hello 👋"));

        String mixed = "Hello 世界! 😀";
        int expectedLength = 5 + 1 + 3 + 3 + 1 + 1 + 4;
        Assertions.assertEquals(expectedLength, Utf8.encodedLength(mixed));

        Assertions.assertEquals("Hello".getBytes(StandardCharsets.UTF_8).length, Utf8.encodedLength("Hello"));
        Assertions.assertEquals("世界".getBytes(StandardCharsets.UTF_8).length, Utf8.encodedLength("世界"));
        Assertions.assertEquals("😀".getBytes(StandardCharsets.UTF_8).length, Utf8.encodedLength("😀"));
    }

    @Test
    public void testEncodedLengthWithUnpairedSurrogates() {
        String unpairedHigh = "Hello" + '\uD800';
        Assertions.assertThrows(IllegalArgumentException.class, () -> Utf8.encodedLength(unpairedHigh));

        String unpairedLow = "Hello" + '\uDC00';
        Assertions.assertThrows(IllegalArgumentException.class, () -> Utf8.encodedLength(unpairedLow));

        String highAtEnd = "Test" + '\uD83D';
        Assertions.assertThrows(IllegalArgumentException.class, () -> Utf8.encodedLength(highAtEnd));

        String wrongOrder = "Test" + '\uDC00' + '\uD800';
        Assertions.assertThrows(IllegalArgumentException.class, () -> Utf8.encodedLength(wrongOrder));
    }

    @Test
    public void testIsWellFormedByteArray() {
        Assertions.assertTrue(Utf8.isWellFormed(new byte[0]));

        byte[] ascii = "Hello World".getBytes(StandardCharsets.UTF_8);
        Assertions.assertTrue(Utf8.isWellFormed(ascii));

        byte[] twoBytes = "café".getBytes(StandardCharsets.UTF_8);
        Assertions.assertTrue(Utf8.isWellFormed(twoBytes));

        byte[] threeBytes = "世界".getBytes(StandardCharsets.UTF_8);
        Assertions.assertTrue(Utf8.isWellFormed(threeBytes));

        byte[] fourBytes = "😀".getBytes(StandardCharsets.UTF_8);
        Assertions.assertTrue(Utf8.isWellFormed(fourBytes));

        byte[] mixed = "Hello 世界! 😀".getBytes(StandardCharsets.UTF_8);
        Assertions.assertTrue(Utf8.isWellFormed(mixed));

        byte[] invalid1 = { (byte) 0xC2, (byte) 0x00 };
        Assertions.assertFalse(Utf8.isWellFormed(invalid1));

        byte[] invalid2 = { (byte) 0xC2 };
        Assertions.assertFalse(Utf8.isWellFormed(invalid2));

        byte[] invalid3 = { (byte) 0xC0, (byte) 0x80 };
        Assertions.assertFalse(Utf8.isWellFormed(invalid3));

        byte[] invalid4 = { (byte) 0xE0, (byte) 0x80, (byte) 0x80 };
        Assertions.assertFalse(Utf8.isWellFormed(invalid4));

        byte[] invalid5 = { (byte) 0xED, (byte) 0xA0, (byte) 0x80 };
        Assertions.assertFalse(Utf8.isWellFormed(invalid5));

        byte[] invalid6 = { (byte) 0xF4, (byte) 0x90, (byte) 0x80, (byte) 0x80 };
        Assertions.assertFalse(Utf8.isWellFormed(invalid6));

        byte[] invalid7 = { (byte) 0xF8, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80 };
        Assertions.assertFalse(Utf8.isWellFormed(invalid7));
    }

    @Test
    public void testIsWellFormedWithOffsetAndLength() {
        byte[] buffer = new byte[100];
        String text = "Hello 世界!";
        byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(utf8, 0, buffer, 10, utf8.length);

        Assertions.assertTrue(Utf8.isWellFormed(buffer, 10, utf8.length));

        Assertions.assertTrue(Utf8.isWellFormed(buffer, 10, 5));

        Assertions.assertTrue(Utf8.isWellFormed(buffer, 50, 0));

        Assertions.assertFalse(Utf8.isWellFormed(buffer, 10, 7));

        buffer[20] = (byte) 0xFF;
        Assertions.assertFalse(Utf8.isWellFormed(buffer, 15, 10));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Utf8.isWellFormed(buffer, -1, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Utf8.isWellFormed(buffer, 0, 101));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Utf8.isWellFormed(buffer, 95, 10));
    }

    @Test
    public void testIsWellFormedSpecialCases() {
        for (int i = 0; i < 0x80; i++) {
            byte[] single = { (byte) i };
            Assertions.assertTrue(Utf8.isWellFormed(single), "Single byte 0x" + Integer.toHexString(i) + " should be valid");
        }

        for (int i = 0x80; i < 0x100; i++) {
            byte[] single = { (byte) i };
            if (i < 0xC2) {
                Assertions.assertFalse(Utf8.isWellFormed(single), "Single byte 0x" + Integer.toHexString(i) + " should be invalid");
            }
        }

        byte[] maxValid = { (byte) 0xF4, (byte) 0x8F, (byte) 0xBF, (byte) 0xBF };
        Assertions.assertTrue(Utf8.isWellFormed(maxValid));

        byte[] beyondMax = { (byte) 0xF4, (byte) 0x90, (byte) 0x80, (byte) 0x80 };
        Assertions.assertFalse(Utf8.isWellFormed(beyondMax));

        byte[] validE0 = { (byte) 0xE0, (byte) 0xA0, (byte) 0x80 };
        Assertions.assertTrue(Utf8.isWellFormed(validE0));

        byte[] validED = { (byte) 0xED, (byte) 0x9F, (byte) 0xBF };
        Assertions.assertTrue(Utf8.isWellFormed(validED));

        byte[] validF0 = { (byte) 0xF0, (byte) 0x90, (byte) 0x80, (byte) 0x80 };
        Assertions.assertTrue(Utf8.isWellFormed(validF0));

        byte[] validF4 = { (byte) 0xF4, (byte) 0x8F, (byte) 0xBF, (byte) 0xBF };
        Assertions.assertTrue(Utf8.isWellFormed(validF4));
    }

    @Test
    public void testPerformanceOptimization() {
        String longAscii = "a".repeat(1000);
        Assertions.assertEquals(1000, Utf8.encodedLength(longAscii));

        byte[] longAsciiBytes = longAscii.getBytes(StandardCharsets.UTF_8);
        Assertions.assertTrue(Utf8.isWellFormed(longAsciiBytes));

        String mixedWithAsciiPrefix = "a".repeat(100) + "世界";
        Assertions.assertEquals(106, Utf8.encodedLength(mixedWithAsciiPrefix));

        byte[] mostlyValid = new byte[1000];
        Arrays.fill(mostlyValid, (byte) 'A');
        mostlyValid[999] = (byte) 0xFF;
        Assertions.assertFalse(Utf8.isWellFormed(mostlyValid));
    }
}
