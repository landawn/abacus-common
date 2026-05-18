/*
 * Copyright (C) 2025 HaiYang Li
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

import java.io.OutputStream;
import java.io.Writer;

/**
 * A specialized writer for efficient CSV output with automatic character escaping.
 * This class extends CharacterWriter and provides optimized writing of CSV content
 * with proper escaping of special CSV characters according to RFC 4180.
 *
 * <p>The writer handles the following CSV escaping rules:</p>
 * <ul>
 *   <li>Double quotes ({@code "}) are escaped as {@code ""} or {@code \"} depending on configuration</li>
 *   <li>Backslashes, tabs, newlines, carriage returns, backspaces, and form-feeds are passed through literally
 *       (they are part of the quoted field's value per RFC 4180)</li>
 *   <li>Control characters (U+0000 through U+001F, except those listed above, plus U+007F) are escaped as Unicode escapes</li>
 *   <li>Special Unicode line separators (U+2028, U+2029) are escaped as {@code \u2028} and {@code \u2029}</li>
 * </ul>
 *
 * <p>The escape mode (double-quote vs backslash) is determined by the
 * {@code CsvUtil.isBackSlashEscapeCharForWrite()} setting.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * try (BufferedCsvWriter writer = new BufferedCsvWriter()) {
 *     writer.write("Name,Description\n");
 *     writer.write("Product \"A\",Contains special chars: \t and \n");
 *     String csv = writer.toString();
 * }
 * }</pre>
 *
 * @see CsvUtil
 * @see CharacterWriter
 */
public final class BufferedCsvWriter extends CharacterWriter {
    /** Backslash-escape sequence used for double quotes when backslash escaping is enabled ({@code \"}). */
    private static final char[] BACK_SLASH_CHAR_ARRAY = "\\\"".toCharArray();
    // start
    // ======================================================================================================>>>
    /*
     * Copyright (C) 2010 Google Inc.
     *
     * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
     * with the License. You may obtain a copy of the License at
     *
     * https://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
     * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
     * the specific language governing permissions and limitations under the License.
     */

    /*
     * From RFC 4627, "All Unicode characters may be placed within the quotation marks except for the characters that
     * must be escaped: quotation mark, reverse solidus, and the control characters (U+0000 through U+001F)."
     *
     * We also escape '\u2028' and '\u2029', which JavaScript interprets as newline characters. This prevents eval()
     * from failing with a syntax error. http://code.google.com/p/google-gson/issues/detail?id=341
     */
    /**
     * Standard CSV character replacement mappings (RFC 4180 double-quote escaping).
     * Double quotes are escaped as {@code ""}, while other special characters are passed through literally.
     */
    static final char[][] REPLACEMENT_CHARS;

    /**
     * CSV character replacement mappings using backslash escaping.
     * Double quotes are escaped as {@code \"} instead of the RFC 4180 {@code ""} form.
     */
    static final char[][] REPLACEMENT_CHARS_BACK_SLASH;

    static {
        final int length = 10000;
        REPLACEMENT_CHARS = new char[length][];

        // for (int i = 0; i <= 0x1f; i++) {
        // REPLACEMENT_CHARS[i] = String.format("\\u%04x", (int) i);
        // }
        for (int i = 0; i < length; i++) {
            if ((i < 32) || (i == 127)) {
                REPLACEMENT_CHARS[i] = getCharNum((char) i).toCharArray();
            }
        }

        // RFC 4180 escaping: only the quote character is escaped (by doubling). CR, LF, TAB,
        // backspace, form-feed inside a quoted field must be passed through literally — they
        // are part of the field's value, not control sequences. Setting REPLACEMENT_CHARS[c]
        // to null leaves the character unmodified by writeCharacter().
        REPLACEMENT_CHARS['"'] = "\"\"".toCharArray();
        // REPLACEMENT_CHARS['\''] = "\\\'".toCharArray();
        REPLACEMENT_CHARS['\\'] = null;
        REPLACEMENT_CHARS['\t'] = null;
        REPLACEMENT_CHARS['\b'] = null;
        REPLACEMENT_CHARS['\n'] = null;
        REPLACEMENT_CHARS['\r'] = null;
        REPLACEMENT_CHARS['\f'] = null;

        // ...
        REPLACEMENT_CHARS['\u2028'] = "\\u2028".toCharArray();
        REPLACEMENT_CHARS['\u2029'] = "\\u2029".toCharArray();

        REPLACEMENT_CHARS_BACK_SLASH = REPLACEMENT_CHARS.clone();
        REPLACEMENT_CHARS_BACK_SLASH['"'] = BACK_SLASH_CHAR_ARRAY;

    }

    /** The maximum index in the {@code REPLACEMENT_CHARS} array. */
    static final int LENGTH_OF_REPLACEMENT_CHARS = REPLACEMENT_CHARS.length - 1;

    // end

    // <<<======================================================================================================

    /**
     * Creates a new BufferedCsvWriter with an internal buffer.
     * The escape mode (double quote vs backslash) is determined by the
     * current CsvUtil configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedCsvWriter writer = new BufferedCsvWriter();
     * writer.write("field1,field2\n");
     * writer.write("\"quoted value\",normal value\n");
     * String csv = writer.toString();
     * }</pre>
     */
    BufferedCsvWriter() {
        super(CsvUtil.isBackSlashEscapeCharForWrite() ? REPLACEMENT_CHARS_BACK_SLASH : REPLACEMENT_CHARS);
    }

    /**
     * Creates a new BufferedCsvWriter that writes to the specified OutputStream.
     * Characters are encoded using the default character encoding.
     * The escape mode is determined by the current CsvUtil configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (FileOutputStream fos = new FileOutputStream("data.csv");
     *      BufferedCsvWriter writer = new BufferedCsvWriter(fos)) {
     *     writer.write("Name,Age,City\n");
     *     writer.write("John Doe,30,New York\n");
     * }
     * }</pre>
     *
     * @param os the OutputStream to write to
     */
    BufferedCsvWriter(final OutputStream os) {
        super(os, CsvUtil.isBackSlashEscapeCharForWrite() ? REPLACEMENT_CHARS_BACK_SLASH : REPLACEMENT_CHARS);
    }

    /**
     * Creates a new BufferedCsvWriter that writes to the specified Writer.
     * The escape mode is determined by the current CsvUtil configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (FileWriter fw = new FileWriter("data.csv");
     *      BufferedCsvWriter writer = new BufferedCsvWriter(fw)) {
     *     writer.write("Product,Price,Description\n");
     *     writer.write("Widget,19.99,\"A useful widget\"\n");
     * }
     * }</pre>
     *
     * @param writer the Writer to write to
     */
    BufferedCsvWriter(final Writer writer) {
        super(writer, CsvUtil.isBackSlashEscapeCharForWrite() ? REPLACEMENT_CHARS_BACK_SLASH : REPLACEMENT_CHARS);
    }

    /**
     * Checks if the writer is using backslash escaping for double quotes.
     *
     * <p>When {@code true}, double quotes are escaped as \" (backslash-quote).
     * When {@code false}, double quotes are escaped as "" (double-double-quote),
     * which is the standard CSV escaping method.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedCsvWriter writer = new BufferedCsvWriter();
     * if (writer.isBackSlash()) {
     *     // Double quotes will be escaped as \"
     * } else {
     *     // Double quotes will be escaped as ""
     * }
     * }</pre>
     *
     * @return {@code true} if using backslash escaping, {@code false} if using double-quote escaping
     */
    boolean isBackSlash() {
        return replacementsForChars['"'] == BACK_SLASH_CHAR_ARRAY;
    }
}
