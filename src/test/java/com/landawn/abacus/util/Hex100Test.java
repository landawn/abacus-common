package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Hex100Test extends TestBase {

    @Test
    public void testEncodeByteArray() {
        // Test empty array
        byte[] empty = new byte[0];
        char[] emptyResult = Hex.encode(empty);
        Assertions.assertEquals(0, emptyResult.length);
        
        // Test single byte
        byte[] single = {0x4A};
        char[] singleResult = Hex.encode(single);
        Assertions.assertArrayEquals(new char[]{'4', 'a'}, singleResult);
        
        // Test multiple bytes
        byte[] data = {0x48, 0x65, 0x6C, 0x6C, 0x6F}; // "Hello"
        char[] result = Hex.encode(data);
        Assertions.assertArrayEquals("48656c6c6f".toCharArray(), result);
        
        // Test negative bytes
        byte[] negative = {(byte)0xFF, 0x00, 0x7F};
        char[] negResult = Hex.encode(negative);
        Assertions.assertArrayEquals("ff007f".toCharArray(), negResult);
    }

    @Test
    public void testEncodeByteArrayWithCase() {
        byte[] data = {0x01, 0x23, (byte)0xAB, (byte)0xCD, (byte)0xEF};
        
        // Test lowercase
        char[] lower = Hex.encode(data, true);
        Assertions.assertArrayEquals("0123abcdef".toCharArray(), lower);
        
        // Test uppercase
        char[] upper = Hex.encode(data, false);
        Assertions.assertArrayEquals("0123ABCDEF".toCharArray(), upper);
    }

    @Test
    public void testEncodeToString() {
        // Test empty array
        byte[] empty = new byte[0];
        String emptyResult = Hex.encodeToString(empty);
        Assertions.assertEquals("", emptyResult);
        
        // Test normal data
        byte[] data = {0x48, 0x65, 0x6C, 0x6C, 0x6F}; // "Hello"
        String result = Hex.encodeToString(data);
        Assertions.assertEquals("48656c6c6f", result);
        
        // Test with all possible byte values
        byte[] allBytes = {0x00, 0x0F, 0x10, 0x1F, (byte)0xF0, (byte)0xFF};
        String allResult = Hex.encodeToString(allBytes);
        Assertions.assertEquals("000f101ff0ff", allResult);
    }

    @Test
    public void testEncodeToStringWithCase() {
        byte[] data = {(byte)0xFF, 0x00, 0x42};
        
        // Test lowercase
        String lower = Hex.encodeToString(data, true);
        Assertions.assertEquals("ff0042", lower);
        
        // Test uppercase
        String upper = Hex.encodeToString(data, false);
        Assertions.assertEquals("FF0042", upper);
    }

    @Test
    public void testDecodeString() {
        // Test empty string
        byte[] empty = Hex.decode("");
        Assertions.assertEquals(0, empty.length);
        
        // Test lowercase
        byte[] lower = Hex.decode("48656c6c6f");
        Assertions.assertArrayEquals(new byte[]{0x48, 0x65, 0x6C, 0x6C, 0x6F}, lower);
        
        // Test uppercase
        byte[] upper = Hex.decode("48656C6C6F");
        Assertions.assertArrayEquals(new byte[]{0x48, 0x65, 0x6C, 0x6C, 0x6F}, upper);
        
        // Test mixed case
        byte[] mixed = Hex.decode("DeadBeef");
        Assertions.assertArrayEquals(new byte[]{(byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF}, mixed);
        
        // Test with FF
        byte[] ff = Hex.decode("FF00");
        Assertions.assertArrayEquals(new byte[]{(byte)0xFF, 0x00}, ff);
    }

    @Test
    public void testDecodeCharArray() {
        // Test empty array
        char[] empty = new char[0];
        byte[] emptyResult = Hex.decode(empty);
        Assertions.assertEquals(0, emptyResult.length);
        
        // Test normal decode
        char[] hex = {'4', '8', '6', '5', '6', 'C', '6', 'C', '6', 'F'};
        byte[] result = Hex.decode(hex);
        Assertions.assertArrayEquals(new byte[]{0x48, 0x65, 0x6C, 0x6C, 0x6F}, result);
    }

    @Test
    public void testDecodeInvalidInput() {
        // Test odd number of characters
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.decode("48656");
        });
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Hex.decode(new char[]{'4', '8', '6'});
        });
        
        // Test invalid hex characters
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
        // Test various byte arrays
        byte[][] testData = {
            {0x00, 0x01, 0x02, 0x03},
            {(byte)0xFE, (byte)0xDC, (byte)0xBA, (byte)0x98},
            {0x12, 0x34, 0x56, 0x78, (byte)0x9A, (byte)0xBC, (byte)0xDE, (byte)0xF0},
            "Hello, World!".getBytes()
        };
        
        for (byte[] original : testData) {
            // Test lowercase round trip
            String hexLower = Hex.encodeToString(original, true);
            byte[] decodedLower = Hex.decode(hexLower);
            Assertions.assertArrayEquals(original, decodedLower);
            
            // Test uppercase round trip
            String hexUpper = Hex.encodeToString(original, false);
            byte[] decodedUpper = Hex.decode(hexUpper);
            Assertions.assertArrayEquals(original, decodedUpper);
            
            // Test char array round trip
            char[] hexChars = Hex.encode(original);
            byte[] decodedChars = Hex.decode(hexChars);
            Assertions.assertArrayEquals(original, decodedChars);
        }
    }

    @Test
    public void testAllByteValues() {
        // Test encoding/decoding all possible byte values
        byte[] allBytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            allBytes[i] = (byte)i;
        }
        
        String hex = Hex.encodeToString(allBytes);
        byte[] decoded = Hex.decode(hex);
        
        Assertions.assertArrayEquals(allBytes, decoded);
    }

    @Test
    public void testLargeData() {
        // Test with larger data
        byte[] largeData = new byte[1000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte)(i % 256);
        }
        
        String hex = Hex.encodeToString(largeData);
        Assertions.assertEquals(2000, hex.length()); // Each byte becomes 2 hex chars
        
        byte[] decoded = Hex.decode(hex);
        Assertions.assertArrayEquals(largeData, decoded);
    }
}