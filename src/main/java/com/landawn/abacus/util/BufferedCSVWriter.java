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
 *   <li>Double quotes (") are escaped as "" or \" depending on configuration</li>
 *   <li>Backslashes (\) are escaped as \\</li>
 *   <li>Tab characters (\t) are escaped as \t</li>
 *   <li>Newline characters (\n) are escaped as \n</li>
 *   <li>Carriage returns (\r) are escaped as \r</li>
 *   <li>Control characters and special Unicode characters are properly escaped</li>
 * </ul>
 * 
 * <p>The escape mode (double quote vs backslash) is determined by the
 * CSVUtil.isBackSlashEscapeCharForWrite() setting.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * try (BufferedCSVWriter writer = new BufferedCSVWriter()) {
 *     writer.write("Name,Description\n");
 *     writer.write("Product \"A\",Contains special chars: \t and \n");
 *     String csv = writer.toString();
 * }
 * }</pre>
 * 
 * @see com.landawn.abacus.util.CSVUtil
 * @see CharacterWriter
 * @since 1.0
 */
public final class BufferedCSVWriter extends CharacterWriter {
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
    static final char[][] REPLACEMENT_CHARS;
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

        // ...
        REPLACEMENT_CHARS['"'] = "\"\"".toCharArray();
        // REPLACEMENT_CHARS['\''] = "\\\'".toCharArray();
        REPLACEMENT_CHARS['\\'] = "\\\\".toCharArray();
        REPLACEMENT_CHARS['\t'] = "\\t".toCharArray();
        REPLACEMENT_CHARS['\b'] = "\\b".toCharArray();
        REPLACEMENT_CHARS['\n'] = "\\n".toCharArray();
        REPLACEMENT_CHARS['\r'] = "\\r".toCharArray();
        REPLACEMENT_CHARS['\f'] = "\\f".toCharArray();

        // ...
        REPLACEMENT_CHARS['\u2028'] = "\\u2028".toCharArray();
        REPLACEMENT_CHARS['\u2029'] = "\\u2029".toCharArray();

        REPLACEMENT_CHARS_BACK_SLASH = REPLACEMENT_CHARS.clone();
        REPLACEMENT_CHARS_BACK_SLASH['"'] = BACK_SLASH_CHAR_ARRAY;

    }

    static final int LENGTH_OF_REPLACEMENT_CHARS = REPLACEMENT_CHARS.length - 1;

    // end

    // <<<======================================================================================================

    /**
     * Creates a new BufferedCSVWriter with an internal buffer.
     * The escape mode (double quote vs backslash) is determined by the
     * current CSVUtil configuration.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BufferedCSVWriter writer = new BufferedCSVWriter();
     * writer.write("field1,field2\n");
     * writer.write("\"quoted value\",normal value\n");
     * String csv = writer.toString();
     * }</pre>
     */
    BufferedCSVWriter() {
        super(CSVUtil.isBackSlashEscapeCharForWrite() ? REPLACEMENT_CHARS_BACK_SLASH : REPLACEMENT_CHARS);
    }

    /**
     * Creates a new BufferedCSVWriter that writes to the specified OutputStream.
     * Characters are encoded using the default character encoding.
     * The escape mode is determined by the current CSVUtil configuration.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * try (FileOutputStream fos = new FileOutputStream("data.csv");
     *      BufferedCSVWriter writer = new BufferedCSVWriter(fos)) {
     *     writer.write("Name,Age,City\n");
     *     writer.write("John Doe,30,New York\n");
     * }
     * }</pre>
     *
     * @param os the OutputStream to write to
     */
    BufferedCSVWriter(final OutputStream os) {
        super(os, CSVUtil.isBackSlashEscapeCharForWrite() ? REPLACEMENT_CHARS_BACK_SLASH : REPLACEMENT_CHARS);
    }

    /**
     * Creates a new BufferedCSVWriter that writes to the specified Writer.
     * The escape mode is determined by the current CSVUtil configuration.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * try (FileWriter fw = new FileWriter("data.csv");
     *      BufferedCSVWriter writer = new BufferedCSVWriter(fw)) {
     *     writer.write("Product,Price,Description\n");
     *     writer.write("Widget,19.99,\"A useful widget\"\n");
     * }
     * }</pre>
     *
     * @param writer the Writer to write to
     */
    BufferedCSVWriter(final Writer writer) {
        super(writer, CSVUtil.isBackSlashEscapeCharForWrite() ? REPLACEMENT_CHARS_BACK_SLASH : REPLACEMENT_CHARS);
    }

    /**
     * Checks if the writer is using backslash escaping for double quotes.
     * 
     * <p>When true, double quotes are escaped as \" (backslash-quote).
     * When false, double quotes are escaped as "" (double-double-quote),
     * which is the standard CSV escaping method.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BufferedCSVWriter writer = new BufferedCSVWriter();
     * if (writer.isBackSlash()) {
     *     // Double quotes will be escaped as \"
     * } else {
     *     // Double quotes will be escaped as ""
     * }
     * }</pre>
     *
     * @return true if using backslash escaping, false if using double-quote escaping
     */
    boolean isBackSlash() {
        return replacementsForChars['"'] == BACK_SLASH_CHAR_ARRAY;
    }
}