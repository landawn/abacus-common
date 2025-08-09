package com.landawn.abacus.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Utf100Test extends TestBase {

    @Test
    public void testEncodedLength() {
        // Test pure ASCII
        Assertions.assertEquals(11, Utf8.encodedLength("Hello World"));
        Assertions.assertEquals(0, Utf8.encodedLength(""));
        Assertions.assertEquals(1, Utf8.encodedLength("A"));
        Assertions.assertEquals(26, Utf8.encodedLength("abcdefghijklmnopqrstuvwxyz"));
        
        // Test 2-byte UTF-8 characters
        Assertions.assertEquals(2, Utf8.encodedLength("é")); // U+00E9
        Assertions.assertEquals(2, Utf8.encodedLength("ñ")); // U+00F1
        Assertions.assertEquals(4, Utf8.encodedLength("éé"));
        
        // Test 3-byte UTF-8 characters (Chinese, Japanese)
        Assertions.assertEquals(3, Utf8.encodedLength("中")); // U+4E2D
        Assertions.assertEquals(6, Utf8.encodedLength("中文")); // Chinese
        Assertions.assertEquals(15, Utf8.encodedLength("こんにちは")); // Japanese hiragana (3 chars)
        Assertions.assertEquals(12, Utf8.encodedLength("Hello 世界")); // Mixed ASCII and Chinese
        
        // Test 4-byte UTF-8 characters (emoji with surrogate pairs)
        Assertions.assertEquals(4, Utf8.encodedLength("😀")); // U+1F600
        Assertions.assertEquals(4, Utf8.encodedLength("👋")); // U+1F44B
        Assertions.assertEquals(10, Utf8.encodedLength("Hello 👋")); // ASCII + emoji
        
        // Test mixed content
        String mixed = "Hello 世界! 😀";
        int expectedLength = 5 + 1 + 3 + 3 + 1 + 1 + 4; // Hello + space + 世 + 界 + ! + space + emoji
        Assertions.assertEquals(expectedLength, Utf8.encodedLength(mixed));
        
        // Verify against actual encoding
        Assertions.assertEquals("Hello".getBytes(StandardCharsets.UTF_8).length, 
                              Utf8.encodedLength("Hello"));
        Assertions.assertEquals("世界".getBytes(StandardCharsets.UTF_8).length, 
                              Utf8.encodedLength("世界"));
        Assertions.assertEquals("😀".getBytes(StandardCharsets.UTF_8).length, 
                              Utf8.encodedLength("😀"));
    }

    @Test
    public void testEncodedLengthWithUnpairedSurrogates() {
        // Test unpaired high surrogate
        String unpairedHigh = "Hello" + '\uD800';
        Assertions.assertThrows(IllegalArgumentException.class, 
            () -> Utf8.encodedLength(unpairedHigh));
        
        // Test unpaired low surrogate
        String unpairedLow = "Hello" + '\uDC00';
        Assertions.assertThrows(IllegalArgumentException.class, 
            () -> Utf8.encodedLength(unpairedLow));
        
        // Test high surrogate at end
        String highAtEnd = "Test" + '\uD83D';
        Assertions.assertThrows(IllegalArgumentException.class, 
            () -> Utf8.encodedLength(highAtEnd));
        
        // Test low surrogate followed by high surrogate (wrong order)
        String wrongOrder = "Test" + '\uDC00' + '\uD800';
        Assertions.assertThrows(IllegalArgumentException.class, 
            () -> Utf8.encodedLength(wrongOrder));
    }

    @Test
    public void testIsWellFormedByteArray() {
        // Test empty array
        Assertions.assertTrue(Utf8.isWellFormed(new byte[0]));
        
        // Test pure ASCII
        byte[] ascii = "Hello World".getBytes(StandardCharsets.UTF_8);
        Assertions.assertTrue(Utf8.isWellFormed(ascii));
        
        // Test valid 2-byte UTF-8
        byte[] twoBytes = "café".getBytes(StandardCharsets.UTF_8);
        Assertions.assertTrue(Utf8.isWellFormed(twoBytes));
        
        // Test valid 3-byte UTF-8
        byte[] threeBytes = "世界".getBytes(StandardCharsets.UTF_8);
        Assertions.assertTrue(Utf8.isWellFormed(threeBytes));
        
        // Test valid 4-byte UTF-8 (emoji)
        byte[] fourBytes = "😀".getBytes(StandardCharsets.UTF_8);
        Assertions.assertTrue(Utf8.isWellFormed(fourBytes));
        
        // Test mixed valid UTF-8
        byte[] mixed = "Hello 世界! 😀".getBytes(StandardCharsets.UTF_8);
        Assertions.assertTrue(Utf8.isWellFormed(mixed));
        
        // Test invalid sequences
        // Invalid continuation byte
        byte[] invalid1 = {(byte)0xC2, (byte)0x00}; // Should be 0x80-0xBF
        Assertions.assertFalse(Utf8.isWellFormed(invalid1));
        
        // Incomplete sequence
        byte[] invalid2 = {(byte)0xC2}; // Missing continuation byte
        Assertions.assertFalse(Utf8.isWellFormed(invalid2));
        
        // Overlong encoding (null character)
        byte[] invalid3 = {(byte)0xC0, (byte)0x80}; // Overlong encoding of U+0000
        Assertions.assertFalse(Utf8.isWellFormed(invalid3));
        
        // Invalid start of 3-byte sequence
        byte[] invalid4 = {(byte)0xE0, (byte)0x80, (byte)0x80}; // Overlong
        Assertions.assertFalse(Utf8.isWellFormed(invalid4));
        
        // Surrogate code point (U+D800)
        byte[] invalid5 = {(byte)0xED, (byte)0xA0, (byte)0x80}; // U+D800
        Assertions.assertFalse(Utf8.isWellFormed(invalid5));
        
        // Invalid 4-byte sequence
        byte[] invalid6 = {(byte)0xF4, (byte)0x90, (byte)0x80, (byte)0x80}; // > U+10FFFF
        Assertions.assertFalse(Utf8.isWellFormed(invalid6));
        
        // 5-byte sequence (not valid in UTF-8)
        byte[] invalid7 = {(byte)0xF8, (byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80};
        Assertions.assertFalse(Utf8.isWellFormed(invalid7));
    }

    @Test
    public void testIsWellFormedWithOffsetAndLength() {
        byte[] buffer = new byte[100];
        String text = "Hello 世界!";
        byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(utf8, 0, buffer, 10, utf8.length);
        
        // Test valid range
        Assertions.assertTrue(Utf8.isWellFormed(buffer, 10, utf8.length));
        
        // Test partial ranges
        Assertions.assertTrue(Utf8.isWellFormed(buffer, 10, 5)); // "Hello"
        
        // Test empty range
        Assertions.assertTrue(Utf8.isWellFormed(buffer, 50, 0));
        
        // Test range that splits a multi-byte character
        Assertions.assertFalse(Utf8.isWellFormed(buffer, 10, 7)); // Splits 世
        
        // Test with invalid UTF-8 in range
        buffer[20] = (byte)0xFF; // Invalid UTF-8 byte
        Assertions.assertFalse(Utf8.isWellFormed(buffer, 15, 10));
        
        // Test boundary conditions
        Assertions.assertThrows(IndexOutOfBoundsException.class, 
            () -> Utf8.isWellFormed(buffer, -1, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, 
            () -> Utf8.isWellFormed(buffer, 0, 101));
        Assertions.assertThrows(IndexOutOfBoundsException.class, 
            () -> Utf8.isWellFormed(buffer, 95, 10));
    }

    @Test
    public void testIsWellFormedSpecialCases() {
        // Test all single-byte values (0x00-0x7F are valid)
        for (int i = 0; i < 0x80; i++) {
            byte[] single = {(byte)i};
            Assertions.assertTrue(Utf8.isWellFormed(single), 
                "Single byte 0x" + Integer.toHexString(i) + " should be valid");
        }
        
        // Test invalid single bytes (0x80-0xFF cannot start a sequence)
        for (int i = 0x80; i < 0x100; i++) {
            byte[] single = {(byte)i};
            if (i < 0xC2) {
                // 0x80-0xC1 are never valid as start bytes
                Assertions.assertFalse(Utf8.isWellFormed(single), 
                    "Single byte 0x" + Integer.toHexString(i) + " should be invalid");
            }
        }
        
        // Test maximum valid Unicode code point (U+10FFFF)
        byte[] maxValid = {(byte)0xF4, (byte)0x8F, (byte)0xBF, (byte)0xBF};
        Assertions.assertTrue(Utf8.isWellFormed(maxValid));
        
        // Test just beyond maximum (U+110000)
        byte[] beyondMax = {(byte)0xF4, (byte)0x90, (byte)0x80, (byte)0x80};
        Assertions.assertFalse(Utf8.isWellFormed(beyondMax));
        
        // Test valid 3-byte boundary cases
        byte[] validE0 = {(byte)0xE0, (byte)0xA0, (byte)0x80}; // Minimum valid E0
        Assertions.assertTrue(Utf8.isWellFormed(validE0));
        
        byte[] validED = {(byte)0xED, (byte)0x9F, (byte)0xBF}; // Maximum valid ED (< surrogates)
        Assertions.assertTrue(Utf8.isWellFormed(validED));
        
        // Test valid 4-byte boundary cases
        byte[] validF0 = {(byte)0xF0, (byte)0x90, (byte)0x80, (byte)0x80}; // Minimum valid F0
        Assertions.assertTrue(Utf8.isWellFormed(validF0));
        
        byte[] validF4 = {(byte)0xF4, (byte)0x8F, (byte)0xBF, (byte)0xBF}; // Maximum valid F4
        Assertions.assertTrue(Utf8.isWellFormed(validF4));
    }

    @Test
    public void testPerformanceOptimization() {
        // Test that ASCII-only strings are handled efficiently
        String longAscii = "a".repeat(1000);
        Assertions.assertEquals(1000, Utf8.encodedLength(longAscii));
        
        byte[] longAsciiBytes = longAscii.getBytes(StandardCharsets.UTF_8);
        Assertions.assertTrue(Utf8.isWellFormed(longAsciiBytes));
        
        // Test mixed content with ASCII prefix
        String mixedWithAsciiPrefix = "a".repeat(100) + "世界";
        Assertions.assertEquals(106, Utf8.encodedLength(mixedWithAsciiPrefix));
        
        // Test that validation stops at first invalid byte
        byte[] mostlyValid = new byte[1000];
        Arrays.fill(mostlyValid, (byte)'A');
        mostlyValid[999] = (byte)0xFF; // Invalid byte at end
        Assertions.assertFalse(Utf8.isWellFormed(mostlyValid));
    }
}