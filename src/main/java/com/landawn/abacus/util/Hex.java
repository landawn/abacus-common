/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

/**
 * Utility class for converting between byte arrays and hexadecimal string representations.
 * 
 * <p>This class provides methods to encode byte arrays into hexadecimal character arrays
 * or strings, and to decode hexadecimal strings back into byte arrays. All methods are
 * thread-safe as they operate on immutable data and don't maintain any state.</p>
 * 
 * <p>Hexadecimal encoding represents each byte as two hexadecimal characters. For example,
 * the byte value 255 (0xFF) is represented as "FF" or "ff" depending on the case setting.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Encoding bytes to hex
 * byte[] data = {0x48, 0x65, 0x6C, 0x6C, 0x6F}; // "Hello" in ASCII
 * String hexString = Hex.encodeToString(data);   // returns "48656c6c6f"
 * String hexUpper = Hex.encodeToString(data, false); // returns "48656C6C6F"
 * 
 * // Decoding hex to bytes
 * byte[] decoded = Hex.decode("48656c6c6f");     // returns original byte array
 * }</pre>
 * 
 * <p><b>Attribution:</b>
 * This class includes code adapted from Apache Commons Lang under the Apache License 2.0. 
 * Methods from these libraries may have been modified for consistency, performance optimization, and null-safety enhancement.
 * 
 * @version $Id: Hex.java 1619948 2014-08-22 22:53:55Z ggregory $
 */
public final class Hex {

    /** Used to build output as Hex. */
    private static final char[] DIGITS_LOWER = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /** Used to build output as Hex. */
    private static final char[] DIGITS_UPPER = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    private Hex() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts an array of bytes into an array of lowercase hexadecimal characters.
     *
     * <p>Each byte in the input array is converted to two hexadecimal characters.
     * The returned array will be exactly double the length of the input array.
     * This method is equivalent to calling {@code encode(data, true)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = {0x01, 0x23, (byte)0xAB, (byte)0xCD};
     * char[] hex = Hex.encode(data);  // returns ['0','1','2','3','a','b','c','d']
     * }</pre>
     *
     * @param data the byte array to convert to hexadecimal characters
     * @return a char array containing lowercase hexadecimal characters representing the input bytes
     * @throws IllegalArgumentException if data is null
     */
    public static char[] encode(final byte[] data) {
        return encode(data, true);
    }

    /**
     * Converts an array of bytes into an array of hexadecimal characters with specified case.
     *
     * <p>Each byte in the input array is converted to two hexadecimal characters.
     * The returned array will be exactly double the length of the input array.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = {0x01, (byte)0xAB};
     * char[] lower = Hex.encode(data, true);   // returns ['0','1','a','b']
     * char[] upper = Hex.encode(data, false);  // returns ['0','1','A','B']
     * }</pre>
     *
     * @param data the byte array to convert to hexadecimal characters
     * @param toLowerCase if {@code true}, returns lowercase hex digits; if {@code false}, returns uppercase
     * @return a char array containing hexadecimal characters representing the input bytes
     * @throws IllegalArgumentException if data is null
     */
    public static char[] encode(final byte[] data, final boolean toLowerCase) {
        if (data == null) {
            throw new IllegalArgumentException("Data array cannot be null");
        }
        return encode(data, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER);
    }

    /**
     * Converts an array of bytes into a lowercase hexadecimal string.
     *
     * <p>This is a convenience method that combines {@link #encode(byte[])} with
     * string conversion. Each byte is represented by exactly two hexadecimal characters
     * in the resulting string.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = {0x48, 0x65, 0x6C, 0x6C, 0x6F}; // "Hello" in ASCII
     * String hex = Hex.encodeToString(data);         // returns "48656c6c6f"
     * }</pre>
     *
     * @param data the byte array to convert to a hexadecimal string
     * @return a string containing lowercase hexadecimal characters representing the input bytes
     * @throws IllegalArgumentException if data is null
     */
    public static String encodeToString(final byte[] data) {
        return String.valueOf(encode(data));
    }

    /**
     * Converts an array of bytes into a hexadecimal string with specified case.
     *
     * <p>This is a convenience method that combines {@link #encode(byte[], boolean)} with
     * string conversion. Each byte is represented by exactly two hexadecimal characters
     * in the resulting string.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = {(byte)0xFF, 0x00, 0x42};
     * String lower = Hex.encodeToString(data, true);   // returns "ff0042"
     * String upper = Hex.encodeToString(data, false);  // returns "FF0042"
     * }</pre>
     *
     * @param data the byte array to convert to a hexadecimal string
     * @param toLowerCase if {@code true}, returns lowercase hex digits; if {@code false}, returns uppercase
     * @return a string containing hexadecimal characters representing the input bytes
     * @throws IllegalArgumentException if data is null
     */
    public static String encodeToString(final byte[] data, final boolean toLowerCase) {
        return String.valueOf(encode(data, toLowerCase));
    }

    /**
     * Converts an array of bytes into an array of characters representing the hexadecimal values of each byte in order.
     * The returned array will be double the length of the passed array, as it takes two characters to represent any
     * given byte.
     *
     * @param data a byte[] to convert to Hex characters
     * @param toDigits the output alphabet (either DIGITS_LOWER or DIGITS_UPPER)
     * @return a char[] containing hexadecimal characters
     */
    static char[] encode(final byte[] data, final char[] toDigits) {
        final int l = data.length;
        final char[] out = new char[l << 1];
        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
            out[j++] = toDigits[0x0F & data[i]];
        }
        return out;
    }

    /**
     * Converts a hexadecimal string into an array of bytes.
     *
     * <p>The input string must contain an even number of hexadecimal characters.
     * Each pair of characters represents one byte in the output array. Both uppercase
     * and lowercase hexadecimal digits are accepted.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data1 = Hex.decode("48656c6c6f");     // returns bytes for "Hello"
     * byte[] data2 = Hex.decode("FF00");           // returns {(byte)0xFF, 0x00}
     * byte[] data3 = Hex.decode("DeadBeef");       // mixed case is accepted
     * }</pre>
     *
     * @param data a string containing hexadecimal digits (0-9, A-F, a-f)
     * @return a byte array containing the binary data decoded from the hexadecimal string
     * @throws IllegalArgumentException if data is {@code null}, the string has an odd number of characters, or contains non-hexadecimal characters
     * @see #decode(char[])
     */
    public static byte[] decode(final String data) throws IllegalArgumentException {
        if (data == null) {
            throw new IllegalArgumentException("Data string cannot be null");
        }
        return decode(data.toCharArray());
    }

    /**
     * Converts an array of hexadecimal characters into an array of bytes.
     *
     * <p>The input array must contain an even number of hexadecimal characters.
     * Each pair of characters represents one byte in the output array. Both uppercase
     * and lowercase hexadecimal digits are accepted.</p>
     *
     * <p>The conversion process:</p>
     * <ul>
     *   <li>Characters <i>0'-'9</i> represent values 0-9</li>
     *   <li>Characters <i>A'-'F</i> represent values 10-15</li>
     *   <li>Characters <i>a'-'f</i> represent values 10-15</li>
     *   <li>Each pair of characters forms one byte: first char = high nibble, second char = low nibble</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] hex = {'4', '8', '6', '5', '6', 'C', '6', 'C', '6', 'F'};
     * byte[] data = Hex.decode(hex);  // returns bytes for "Hello"
     * }</pre>
     *
     * @param data an array of characters containing hexadecimal digits
     * @return a byte array containing the binary data decoded from the hexadecimal characters
     * @throws IllegalArgumentException if data is {@code null}, the array has an odd number of elements, or contains non-hexadecimal characters
     */
    public static byte[] decode(final char[] data) throws IllegalArgumentException {
        if (data == null) {
            throw new IllegalArgumentException("Data array cannot be null");
        }

        final int len = data.length;

        if ((len & 0x01) != 0) {
            throw new IllegalArgumentException("Odd number of characters.");
        }

        final byte[] out = new byte[len >> 1];

        // two characters form the hex value.
        for (int i = 0, j = 0; j < len; i++) {
            int f = toDigit(data[j], j) << 4;
            j++;
            f = f | toDigit(data[j], j);
            j++;
            out[i] = (byte) (f & 0xFF);
        }

        return out;
    }

    /**
     * Converts a hexadecimal character to its integer value (0-15).
     *
     * <p>This method validates that the character is a valid hexadecimal digit and converts it
     * to its corresponding integer value. Valid characters are <i>0'-'9</i>, <i>A'-'F</i>, and 'a'-'f'.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int value1 = toDigit('5', 0);  // returns 5
     * int value2 = toDigit('A', 1);  // returns 10
     * int value3 = toDigit('f', 2);  // returns 15
     * }</pre>
     *
     * @param ch the hexadecimal character to convert (0-9, A-F, a-f)
     * @param index the position of the character in the source string (used for error reporting)
     * @return the integer value (0-15) represented by the hexadecimal character
     * @throws IllegalArgumentException if the character is not a valid hexadecimal digit
     */
    static int toDigit(final char ch, final int index) throws IllegalArgumentException {
        final int digit = Character.digit(ch, 16);
        if (digit == -1) {
            throw new IllegalArgumentException("Illegal hexadecimal character " + ch + " at index " + index);
        }
        return digit;
    }
}
