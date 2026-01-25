/*
 * Copyright (C) 2015 HaiYang Li
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * An abstract base class for writers that perform automatic character escaping based on
 * configurable replacement rules.
 * 
 * <p>This sealed class serves as the foundation for specialized writers that need to
 * escape certain characters during output, such as JSON, XML, or CSV writers. Each
 * subclass defines its own character replacement table that determines which characters
 * should be escaped and how.</p>
 * 
 * <p>The class provides efficient character-by-character and bulk writing operations
 * with automatic escaping. Characters that need escaping are replaced with their
 * corresponding escape sequences as defined in the replacement table.</p>
 * 
 * <p>This class is designed for high-performance output generation and is not thread-safe.
 * If multiple threads need to write concurrently, external synchronization is required.</p>
 * 
 * <p>Example of how subclasses use this class:</p>
 * <pre>{@code
 * // In a subclass like BufferedJsonWriter:
 * char[][] replacements = new char[128][];
 * replacements['"'] = "\\\"".toCharArray();
 * replacements['\\'] = "\\\\".toCharArray();
 * // ... more replacements
 * 
 * CharacterWriter writer = new BufferedJsonWriter();
 * writer.writeCharacter("Hello \"World\"");   // Outputs: Hello \"World\"
 * }</pre>
 * 
 * @see BufferedJsonWriter
 * @see BufferedXmlWriter
 * @see BufferedCsvWriter
 */
public abstract sealed class CharacterWriter extends BufferedWriter permits BufferedJsonWriter, BufferedXmlWriter, BufferedCsvWriter {

    /**
     * The character replacement table used for escaping.
     * Each index represents a character code, and the value at that index
     * (if not null) is the replacement character sequence.
     */
    protected final char[][] replacementsForChars;

    /**
     * The maximum character code that can be checked in the replacement table.
     * This is typically replacementsForChars.length - 1.
     */
    protected final int lengthOfReplacementsForChars;

    /**
     * Constructs a CharacterWriter with internal buffering using the specified
     * character replacement table.
     *
     * @param replacementsForChars the character replacement table for escaping
     */
    CharacterWriter(final char[][] replacementsForChars) {
        this.replacementsForChars = replacementsForChars;
        lengthOfReplacementsForChars = replacementsForChars.length - 1;
    }

    /**
     * Constructs a CharacterWriter that writes to the specified OutputStream
     * using the specified character replacement table.
     *
     * @param os the output stream to write to
     * @param replacementsForChars the character replacement table for escaping
     */
    CharacterWriter(final OutputStream os, final char[][] replacementsForChars) {
        super(os);
        this.replacementsForChars = replacementsForChars;
        lengthOfReplacementsForChars = replacementsForChars.length - 1;
    }

    /**
     * Constructs a CharacterWriter that writes to the specified Writer
     * using the specified character replacement table.
     *
     * @param writer the writer to write to
     * @param replacementsForChars the character replacement table for escaping
     */
    CharacterWriter(final Writer writer, final char[][] replacementsForChars) {
        super(writer);
        this.replacementsForChars = replacementsForChars;
        lengthOfReplacementsForChars = replacementsForChars.length - 1;
    }

    /**
     * Writes a single character with automatic escaping.
     * 
     * <p>If the character needs escaping according to the replacement table,
     * the escape sequence is written instead of the original character.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // In a JSON writer where " is escaped as \"
     * writer.writeCharacter('"');   // Writes: \"
     * writer.writeCharacter('A');   // Writes: A (no escaping needed)
     * }</pre>
     *
     * @param ch the character to write
     * @throws IOException if an I/O error occurs
     */
    public void writeCharacter(final char ch) throws IOException {
        if ((ch > lengthOfReplacementsForChars) || (replacementsForChars[ch] == null)) {
            write(ch);
        } else {
            write(replacementsForChars[ch]);
        }
    }

    /**
     * Writes a character array with automatic escaping.
     * 
     * <p>Each character in the array is checked against the replacement table,
     * and escaped if necessary. This method is optimized to minimize the number
     * of write operations by batching non-escaped characters.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "Hello \"World\"".toCharArray();
     * writer.writeCharacter(chars);   // Writes: Hello \"World\"
     * }</pre>
     *
     * @param cbuf the character array to write
     * @throws IOException if an I/O error occurs
     */
    public void writeCharacter(final char[] cbuf) throws IOException {
        final int len = cbuf.length;

        char ch = 0;
        int i = 0;
        int from = 0;

        for (@SuppressWarnings("UnnecessaryLocalVariable")
        final int end = len; i < end; i++) {
            ch = cbuf[i];

            //noinspection StatementWithEmptyBody
            if ((ch > lengthOfReplacementsForChars) || (replacementsForChars[ch] == null)) {
                // continue
            } else {
                if (i > from) {
                    write(cbuf, from, i - from);
                    from = i;
                }

                write(replacementsForChars[ch]);

                from++;
            }
        }

        if (i > from) {
            write(cbuf, from, i - from);
        }
    }

    /**
     * Writes a portion of a character array with automatic escaping.
     * 
     * <p>Only the specified portion of the array is processed for escaping.
     * Characters outside the specified range are not written.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "prefix<data>suffix".toCharArray();
     * writer.writeCharacter(chars, 6, 6);   // Writes: <data> (with escaping)
     * }</pre>
     *
     * @param cbuf the character array containing data to write
     * @param off the start offset in the array
     * @param len the number of characters to write
     * @throws IOException if an I/O error occurs
     * @throws IndexOutOfBoundsException if off or len are invalid
     */
    public void writeCharacter(final char[] cbuf, final int off, int len) throws IOException {
        len = Math.min(cbuf.length - off, len);

        char ch = 0;
        int i = off;
        int from = off;

        for (final int end = off + len; i < end; i++) {
            ch = cbuf[i];

            //noinspection StatementWithEmptyBody
            if ((ch > lengthOfReplacementsForChars) || (replacementsForChars[ch] == null)) {
                // continue
            } else {
                if (i > from) {
                    write(cbuf, from, i - from);
                    from = i;
                }

                write(replacementsForChars[ch]);

                from++;
            }
        }

        if (i > from) {
            write(cbuf, from, i - from);
        }
    }

    /**
     * Writes a string with automatic escaping.
     * 
     * <p>Each character in the string is checked against the replacement table,
     * and escaped if necessary. If the string is {@code null}, "null" is written.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * writer.writeCharacter("Hello \"World\"");   // Writes escaped version
     * writer.writeCharacter(null);                // Writes: null
     * }</pre>
     *
     * @param str the string to write
     * @throws IOException if an I/O error occurs
     */
    @SuppressWarnings("deprecation")
    public void writeCharacter(final String str) throws IOException {
        if (str == null) {
            write(Strings.NULL_CHAR_ARRAY);
        } else {
            writeCharacter(InternalUtil.getCharsForReadOnly(str));
        }
    }

    /**
     * Writes a portion of a string with automatic escaping.
     * 
     * <p>Only the specified portion of the string is processed for escaping.
     * Characters outside the specified range are not written.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String text = "prefix<data>suffix";
     * writer.writeCharacter(text, 6, 6);   // Writes: <data> (with escaping)
     * }</pre>
     *
     * @param str the string containing data to write
     * @param off the start offset in the string
     * @param len the number of characters to write
     * @throws IOException if an I/O error occurs
     * @throws IndexOutOfBoundsException if off or len are invalid
     */
    @SuppressWarnings("deprecation")
    public void writeCharacter(final String str, final int off, final int len) throws IOException {
        if (str == null) {
            write(Strings.NULL_CHAR_ARRAY, off, len);
        } else {
            writeCharacter(InternalUtil.getCharsForReadOnly(str), off, len);
        }
    }

    /**
     * Converts a character to its hexadecimal string representation.
     * Used for numeric character references in XML.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hex = getHexString(0x1F);   // Returns "&#x1f;"
     * }</pre>
     *
     * @param ch the character code to convert
     * @return the hexadecimal string representation
     */
    static String getHexString(final int ch) {
        return "&#x" + Integer.toHexString(ch) + ";";
    }

    /**
     * Converts a character to its Unicode escape sequence.
     * Used for JSON Unicode escapes.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String unicode = getCharNum('\u2028');   // Returns "\\u2028"
     * }</pre>
     *
     * @param ch the character to convert
     * @return the Unicode escape sequence
     */
    static String getCharNum(final char ch) {
        return String.format("\\u%04x", (int) ch);
    }
}
