/*
 * Copyright (C) 2024 HaiYang Li
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
 * A specialized writer for efficient JSON output with automatic character escaping.
 * This class extends {@link CharacterWriter} and provides optimized writing of JSON content
 * with proper escaping of special JSON characters according to RFC 4627.
 *
 * <p>The following characters are automatically escaped when using the
 * {@code writeCharacter} methods:</p>
 * <ul>
 *   <li>Double quotes ({@code "}) are escaped as {@code \"}</li>
 *   <li>Backslashes ({@code \}) are escaped as {@code \\}</li>
 *   <li>Tab ({@code \t}), backspace ({@code \b}), newline ({@code \n}),
 *       carriage return ({@code \r}), and form feed ({@code \f}) use their
 *       standard JSON escape sequences</li>
 *   <li>Control characters (U+0000 through U+001F) and U+007F are escaped
 *       as <code>&#92;uXXXX</code> sequences</li>
 *   <li>Line separator (U+2028) and paragraph separator (U+2029) are escaped
 *       as <code>&#92;u2028</code> and <code>&#92;u2029</code> to prevent JavaScript syntax errors</li>
 * </ul>
 *
 * <p>An HTML-safe replacement table (with additional escaping for {@code <}, {@code >},
 * {@code &}, {@code =}, and single quotes) is defined for potential use when JSON is embedded
 * in HTML contexts, but it is not currently selected by any constructor; this writer always
 * uses the standard RFC 4627 replacements.</p>
 *
 * <p>Note that escaping is performed only by the {@code writeCharacter(...)} methods
 * inherited from {@link CharacterWriter}. The plain {@code write(...)} methods write
 * their argument verbatim without escaping.</p>
 *
 * <p>This writer is designed for high-performance JSON generation. It provides three modes
 * of operation: internal buffering, writing to an {@link java.io.OutputStream}, or writing
 * to another {@link java.io.Writer}.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * try (BufferedJsonWriter writer = new BufferedJsonWriter()) {
 *     writer.write("{\"name\":\"");
 *     writer.writeCharacter("John \"Johnny\" Doe");   // Escaped: John \"Johnny\" Doe
 *     writer.write("\",\"data\":\"");
 *     writer.writeCharacter("Line1\nLine2");           // Escaped: Line1\nLine2
 *     writer.write("\"}");
 *     String json = writer.toString();
 *     // Result: {"name":"John \"Johnny\" Doe","data":"Line1\nLine2"}
 * }
 * }</pre>
 *
 * @see CharacterWriter
 */
public final class BufferedJsonWriter extends CharacterWriter {
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
     * We also escape U+2028 and U+2029, which JavaScript interprets as newline characters. This prevents eval()
     * from failing with a syntax error. http://code.google.com/p/google-gson/issues/detail?id=341
     */

    /**
     * Standard JSON character replacement mappings according to RFC 4627.
     * This array contains escape sequences for characters that must be escaped in JSON.
     */
    static final char[][] REPLACEMENT_CHARS;

    /**
     * HTML-safe character replacement mappings for JSON embedded in HTML contexts.
     * Includes additional escaping for HTML special characters.
     */
    static final char[][] HTML_SAFE_REPLACEMENT_CHARS;

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
        REPLACEMENT_CHARS['"'] = "\\\"".toCharArray();
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
        HTML_SAFE_REPLACEMENT_CHARS = REPLACEMENT_CHARS.clone();
        HTML_SAFE_REPLACEMENT_CHARS['<'] = "\\u003c".toCharArray();
        HTML_SAFE_REPLACEMENT_CHARS['>'] = "\\u003e".toCharArray();
        HTML_SAFE_REPLACEMENT_CHARS['&'] = "\\u0026".toCharArray();
        HTML_SAFE_REPLACEMENT_CHARS['='] = "\\u003d".toCharArray();
        HTML_SAFE_REPLACEMENT_CHARS['\''] = "\\u0027".toCharArray();
    }

    /**
     * The maximum index in the REPLACEMENT_CHARS array.
     */
    static final int LENGTH_OF_REPLACEMENT_CHARS = REPLACEMENT_CHARS.length - 1;

    // end

    // <<<======================================================================================================

    /**
     * Creates a new BufferedJsonWriter with an internal buffer.
     * The content is stored in memory and can be retrieved using {@link #toString()}.
     *
     * <p>This constructor is package-private. Outside this package, obtain an
     * instance from the pool via {@link Objectory#createBufferedJsonWriter()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedJsonWriter writer = new BufferedJsonWriter();
     * writer.write("{\"key\":\"value\"}");
     * String json = writer.toString();
     * }</pre>
     *
     */
    BufferedJsonWriter() {
        super(REPLACEMENT_CHARS);
    }

    /**
     * Creates a new BufferedJsonWriter that writes to the specified OutputStream.
     * Characters are encoded using the default character encoding.
     *
     * <p>The writer will automatically escape JSON special characters as they
     * are written to the output stream. Closing this writer will also close
     * the underlying {@code OutputStream}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (FileOutputStream fos = new FileOutputStream("data.json");
     *      BufferedJsonWriter writer = new BufferedJsonWriter(fos)) {
     *     writer.write("{\"message\":\"Hello, World!\"}");
     * }
     * }</pre>
     *
     * @param os the OutputStream to write to
     */
    BufferedJsonWriter(final OutputStream os) {
        super(os, REPLACEMENT_CHARS);
    }

    /**
     * Creates a new BufferedJsonWriter that writes to the specified Writer.
     *
     * <p>The writer will automatically escape JSON special characters as they
     * are written. Closing this writer will also close the underlying {@code Writer}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (FileWriter fw = new FileWriter("data.json");
     *      BufferedJsonWriter writer = new BufferedJsonWriter(fw)) {
     *     writer.write("{\"status\":\"success\",\"code\":200}");
     * }
     * }</pre>
     *
     * @param writer the Writer to write to
     */
    BufferedJsonWriter(final Writer writer) {
        super(writer, REPLACEMENT_CHARS);
    }
}
