/*
 * Copyright (C) 2013 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import static java.lang.Character.MAX_SURROGATE;
import static java.lang.Character.MIN_SURROGATE;

/**
 * Low-level, high-performance utility methods for working with UTF-8 character encoding.
 * This class provides optimized implementations for UTF-8 validation and length calculation
 * that are more efficient than using {@code String.getBytes(UTF_8)}.
 * 
 * <p>The implementation follows the restricted definition of UTF-8 introduced in Unicode 3.1,
 * which means it rejects "non-shortest form" byte sequences. This is stricter than some
 * JDK decoders which may accept such sequences.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Highly optimized for performance with special handling for ASCII characters</li>
 *   <li>Validates proper UTF-8 encoding including surrogate pair validation</li>
 *   <li>Rejects overlong encodings and invalid byte sequences</li>
 *   <li>More efficient than standard Java UTF-8 operations for validation and length calculation</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * String text = "Hello 世界";
 * int utf8Length = Utf8.encodedLength(text);   // More efficient than text.getBytes("UTF-8").length
 * 
 * byte[] bytes = getDataFromNetwork();
 * if (Utf8.isWellFormed(bytes)) {
 *     String decoded = new String(bytes, StandardCharsets.UTF_8);
 * }
 * }</pre>
 * 
 * <p>Note: This class is adapted from Google Guava under Apache License 2.0.</p>
 *
 * @author Martin Buchholz
 * @author Clément Roux
 */
public class Utf8 {

    /**
     * Returns the number of bytes in the UTF-8-encoded form of the given character sequence.
     * This method is equivalent to {@code string.getBytes(UTF_8).length}, but is more efficient
     * in both time and space.
     * 
     * <p>The implementation is optimized with fast paths for:</p>
     * <ul>
     *   <li>Pure ASCII strings (single pass, no expansion)</li>
     *   <li>Characters less than 0x800 (2-byte UTF-8)</li>
     *   <li>General Unicode including surrogate pairs</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String ascii = "Hello World";
     * int length1 = Utf8.encodedLength(ascii);   // Returns 11 (1 byte per char)
     * 
     * String unicode = "Hello 世界";
     * int length2 = Utf8.encodedLength(unicode);   // Returns 12 (6 + 3 + 3)
     * 
     * String emoji = "Hello 👋";
     * int length3 = Utf8.encodedLength(emoji);   // Accounts for surrogate pairs
     * }</pre>
     *
     * @param sequence the character sequence to measure
     * @return the number of bytes needed to encode the sequence in UTF-8
     * @throws IllegalArgumentException if the sequence contains ill-formed UTF-16 (unpaired surrogates)
     */
    public static int encodedLength(final CharSequence sequence) {
        // Warning to maintainers: this implementation is highly optimized.
        final int utf16Length = sequence.length();
        int utf8Length = utf16Length;
        int i = 0;

        // This loop optimizes for pure ASCII.
        while (i < utf16Length && sequence.charAt(i) < 0x80) {
            i++;
        }

        // This loop optimizes for chars less than 0x800.
        for (; i < utf16Length; i++) {
            final char c = sequence.charAt(i);
            if (c < 0x800) {
                utf8Length += ((0x7f - c) >>> 31); // branch free!
            } else {
                utf8Length += encodedLengthGeneral(sequence, i);
                break;
            }
        }

        if (utf8Length < utf16Length) {
            // Necessary and sufficient condition for overflow because of maximum 3x expansion
            throw new IllegalArgumentException("UTF-8 length does not fit in int: " + (utf8Length + (1L << 32)));
        }
        return utf8Length;
    }

    private static int encodedLengthGeneral(final CharSequence sequence, final int start) {
        final int utf16Length = sequence.length();
        int utf8Length = 0;
        for (int i = start; i < utf16Length; i++) {
            final char c = sequence.charAt(i);
            if (c < 0x800) {
                utf8Length += (0x7f - c) >>> 31; // branch free!
            } else {
                utf8Length += 2;
                // jdk7+: if (Character.isSurrogate(c)) {
                if (MIN_SURROGATE <= c && c <= MAX_SURROGATE) {
                    // Check that we have a well-formed surrogate pair.
                    if (Character.codePointAt(sequence, i) == c) {
                        throw new IllegalArgumentException(unpairedSurrogateMsg(i));
                    }
                    i++;
                }
            }
        }
        return utf8Length;
    }

    /**
     * Returns {@code true} if the given byte array is a well-formed UTF-8 byte sequence
     * according to Unicode 6.0 standards. This method performs stricter validation than
     * simple decoding - it ensures the bytes follow proper UTF-8 encoding rules.
     * 
     * <p>The validation checks for:</p>
     * <ul>
     *   <li>Proper continuation byte sequences</li>
     *   <li>No overlong encodings (non-shortest form)</li>
     *   <li>No invalid surrogate code points</li>
     *   <li>Valid Unicode code point ranges</li>
     * </ul>
     * 
     * <p>This method returns {@code true} if and only if 
     * {@code Arrays.equals(bytes, new String(bytes, UTF_8).getBytes(UTF_8))} would return {@code true},
     * but is more efficient in both time and space.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] validUtf8 = "Hello 世界".getBytes(StandardCharsets.UTF_8);
     * boolean valid1 = Utf8.isWellFormed(validUtf8);   // Returns true
     * 
     * byte[] invalid = {(byte)0xC0, (byte)0x80};  // Overlong encoding of NULL
     * boolean valid2 = Utf8.isWellFormed(invalid);   // Returns false
     * }</pre>
     *
     * @param bytes the byte array to validate
     * @return {@code true} if the bytes form a valid UTF-8 sequence, {@code false} otherwise
     */
    public static boolean isWellFormed(final byte[] bytes) {
        return isWellFormed(bytes, 0, bytes.length);
    }

    /**
     * Returns whether the given byte array slice is a well-formed UTF-8 byte sequence,
     * as defined by {@link #isWellFormed(byte[])}. 
     * 
     * <p>This method allows validation of a portion of a byte array without copying.
     * Note that this can return {@code false} even when {@code isWellFormed(bytes)} 
     * would return {@code true} if the slice boundaries split a multi-byte character.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] buffer = new byte[1024];
     * int bytesRead = inputStream.read(buffer);
     * if (Utf8.isWellFormed(buffer, 0, bytesRead)) {
     *     String text = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
     * }
     * }</pre>
     *
     * @param bytes the input buffer containing the bytes to validate
     * @param off the offset in the buffer of the first byte to validate
     * @param len the number of bytes to validate from the buffer
     * @return {@code true} if the specified byte range forms a valid UTF-8 sequence
     * @throws IndexOutOfBoundsException if offset and length are out of bounds
     */
    public static boolean isWellFormed(final byte[] bytes, final int off, final int len) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(off, len, bytes.length);

        final int end = off + len;
        // Look for the first non-ASCII character.
        for (int i = off; i < end; i++) {
            if (bytes[i] < 0) {
                return isWellFormedSlowPath(bytes, i, end);
            }
        }
        return true;
    }

    private static boolean isWellFormedSlowPath(final byte[] bytes, final int off, final int end) {
        int index = off;
        while (true) {
            int byte1;

            // Optimize for interior runs of ASCII bytes.
            do {
                if (index >= end) {
                    return true;
                }
            } while ((byte1 = bytes[index++]) >= 0);

            if (byte1 < (byte) 0xE0) {
                // Two-byte form.
                // Simultaneously check for illegal trailing-byte in leading position
                // and overlong 2-byte form.
                if ((index == end) || byte1 < (byte) 0xC2 || bytes[index++] > (byte) 0xBF) {
                    return false;
                }
            } else if (byte1 < (byte) 0xF0) {
                // Three-byte form.
                if (index + 1 >= end) {
                    return false;
                }
                final int byte2 = bytes[index++];
                if (byte2 > (byte) 0xBF
                        // Overlong? 5 most significant bits must not all be zero.
                        || (byte1 == (byte) 0xE0 && byte2 < (byte) 0xA0)
                        // Check for illegal surrogate codepoints.
                        || (byte1 == (byte) 0xED && (byte) 0xA0 <= byte2)
                        // Third byte trailing-byte test.
                        || bytes[index++] > (byte) 0xBF) {
                    return false;
                }
            } else {
                // Four-byte form.
                if (index + 2 >= end) {
                    return false;
                }
                final int byte2 = bytes[index++];
                if (byte2 > (byte) 0xBF
                        // Check that 1 <= plane <= 16. Tricky optimized form of:
                        // if (byte1 > (byte) 0xF4
                        //     || byte1 == (byte) 0xF0 && byte2 < (byte) 0x90
                        //     || byte1 == (byte) 0xF4 && byte2 > (byte) 0x8F)
                        || (((byte1 << 28) + (byte2 - (byte) 0x90)) >> 30) != 0
                        // Third byte trailing-byte test
                        || bytes[index++] > (byte) 0xBF
                        // Fourth byte trailing-byte test
                        || bytes[index++] > (byte) 0xBF) { //NOSONAR
                    return false;
                }
            }
        }
    }

    private static String unpairedSurrogateMsg(final int i) {
        return "Unpaired surrogate at index " + i;
    }

    private Utf8() {
    }
}
