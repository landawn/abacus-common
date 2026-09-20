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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * A specialized writer for efficient JSON output with character escaping through the
 * {@code writeCharacter(...)} methods.
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
 * BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();
 * try {
 *     writer.write("{\"name\":\"");
 *     writer.writeCharacter("John \"Johnny\" Doe");   // Escaped: John \"Johnny\" Doe
 *     writer.write("\",\"data\":\"");
 *     writer.writeCharacter("Line1\nLine2");           // Escaped: Line1\nLine2
 *     writer.write("\"}");
 *     String json = writer.toString();
 *     // Result: {"name":"John \"Johnny\" Doe","data":"Line1\nLine2"}
 * } finally {
 *     Objectory.recycle(writer);
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

        // JSON escapes for quotes, backslashes, and ASCII control characters.
        REPLACEMENT_CHARS['"'] = "\\\"".toCharArray();
        // REPLACEMENT_CHARS['\''] = "\\\'".toCharArray();
        REPLACEMENT_CHARS['\\'] = "\\\\".toCharArray();
        REPLACEMENT_CHARS['\t'] = "\\t".toCharArray();
        REPLACEMENT_CHARS['\b'] = "\\b".toCharArray();
        REPLACEMENT_CHARS['\n'] = "\\n".toCharArray();
        REPLACEMENT_CHARS['\r'] = "\\r".toCharArray();
        REPLACEMENT_CHARS['\f'] = "\\f".toCharArray();

        // Escape Unicode line and paragraph separators for JavaScript compatibility.
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
     * BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();
     * try {
     *     writer.write("{\"key\":\"value\"}");
     *     String json = writer.toString();
     * } finally {
     *     Objectory.recycle(writer);
     * }
     * }</pre>
     *
     */
    BufferedJsonWriter() {
        super(REPLACEMENT_CHARS);
    }

    /**
     * Creates a new BufferedJsonWriter that writes to the specified OutputStream.
     * Characters are encoded as UTF-8 ({@link IOUtil#DEFAULT_CHARSET}), independent of the
     * JVM's platform-default charset.
     *
     * <p>The {@code writeCharacter(...)} methods escape JSON special characters; the ordinary
     * {@code write(...)} methods write verbatim. Closing this writer also closes the underlying
     * {@code OutputStream}.</p>
     *
     * <p>This constructor is package-private. Outside this package, obtain an
     * instance via {@link Objectory#createBufferedJsonWriter(OutputStream)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (FileOutputStream fos = new FileOutputStream("data.json")) {
     *     BufferedJsonWriter writer = Objectory.createBufferedJsonWriter(fos);
     *     try {
     *         writer.write("{\"message\":\"Hello, World!\"}");
     *     } finally {
     *         Objectory.recycle(writer);   // flushes, but does not close the underlying stream
     *     }
     * }
     * }</pre>
     *
     * @param os the OutputStream to write to
     * @throws IllegalArgumentException if {@code os} is {@code null}
     */
    BufferedJsonWriter(final OutputStream os) throws IllegalArgumentException {
        super(os, REPLACEMENT_CHARS);
    }

    /**
     * Creates a new BufferedJsonWriter that writes to the specified Writer.
     *
     * <p>The {@code writeCharacter(...)} methods escape JSON special characters; the ordinary
     * {@code write(...)} methods write verbatim. Closing this writer also closes the underlying
     * {@code Writer}.</p>
     *
     * <p>This constructor is package-private. Outside this package, obtain an
     * instance via {@link Objectory#createBufferedJsonWriter(Writer)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (FileWriter fw = new FileWriter("data.json")) {
     *     BufferedJsonWriter writer = Objectory.createBufferedJsonWriter(fw);
     *     try {
     *         writer.write("{\"status\":\"success\",\"code\":200}");
     *     } finally {
     *         Objectory.recycle(writer);   // flushes, but does not close the underlying writer
     *     }
     * }
     * }</pre>
     *
     * @param writer the Writer to write to
     * @throws NullPointerException if {@code writer} is {@code null}
     */
    BufferedJsonWriter(final Writer writer) throws NullPointerException {
        super(writer, REPLACEMENT_CHARS);
    }

    /**
     * {@inheritDoc}
     *
     * <p>A UTF-16 surrogate written on its own (it can never be half of a pair here) is emitted as a
     * <code>&#92;uXXXX</code> escape: a lone surrogate has no UTF-8 encoding, so the {@code OutputStream}-backed
     * writer would otherwise substitute {@code ?} while the String/Writer-backed writers passed it through raw.
     * RFC 8259 permits the escaped form and it decodes back to the same {@code char}.</p>
     *
     * @param ch the character to write
     * @throws IOException if this writer is closed, or writing the escaped character to the underlying
     *         output stream or writer fails
     */
    @Override
    public void writeCharacter(final char ch) throws IOException {
        if (Character.isSurrogate(ch)) {
            write(getCharNum(ch));
        } else {
            super.writeCharacter(ch);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Unpaired surrogates are escaped as <code>&#92;uXXXX</code>; well-formed surrogate pairs are copied through
     * unchanged (see {@link #writeCharacter(char)}).</p>
     *
     * @param cbuf the character array to write; must not be {@code null}
     * @throws IOException if this writer is closed, or writing the escaped characters to the underlying
     *         output stream or writer fails
     * @throws NullPointerException if {@code cbuf} is {@code null}
     */
    @Override
    public void writeCharacter(final char[] cbuf) throws IOException, NullPointerException {
        ensureOpen();

        writeCharacter(cbuf, 0, cbuf.length);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Unpaired surrogates are escaped as <code>&#92;uXXXX</code>; well-formed surrogate pairs are copied through
     * unchanged (see {@link #writeCharacter(char)}).</p>
     *
     * @param cbuf the character array containing the data to write; must not be {@code null}
     * @param off the start offset in the array; must be non-negative and not greater than {@code cbuf.length}
     * @param len the number of characters to write; must be non-negative and {@code off + len} must not
     *        exceed {@code cbuf.length}
     * @throws IOException if this writer is closed, or writing the escaped characters to the underlying
     *         output stream or writer fails
     * @throws NullPointerException if {@code cbuf} is {@code null}
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is negative, or {@code off + len}
     *         exceeds {@code cbuf.length}
     */
    @Override
    public void writeCharacter(final char[] cbuf, final int off, final int len) throws IOException, NullPointerException, IndexOutOfBoundsException {
        ensureOpen();
        N.requireNonNull(cbuf, cs.cbuf);

        if ((off < 0) || (len < 0) || (off > cbuf.length) || (len > cbuf.length - off)) {
            throw new IndexOutOfBoundsException();
        }

        final int end = off + len;
        int from = off;
        char ch = 0;

        for (int i = off; i < end; i++) {
            ch = cbuf[i];

            // The replacement table ends far below the surrogate block, so the two tests never overlap and the
            // common (non-surrogate, non-escaped) character costs the same two comparisons as before.
            if (ch < Character.MIN_SURROGATE) {
                if (ch <= lengthOfReplacementsForChars && replacementsForChars[ch] != null) {
                    if (i > from) {
                        write(cbuf, from, i - from);
                    }

                    write(replacementsForChars[ch]);
                    from = i + 1;
                }
            } else if (ch <= Character.MAX_SURROGATE) {
                if (Character.isHighSurrogate(ch) && i + 1 < end && Character.isLowSurrogate(cbuf[i + 1])) {
                    i++; // a valid pair: copy both units through
                } else {
                    if (i > from) {
                        write(cbuf, from, i - from);
                    }

                    write(getCharNum(ch));
                    from = i + 1;
                }
            }
        }

        if (end > from) {
            write(cbuf, from, end - from);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Unpaired surrogates are escaped as <code>&#92;uXXXX</code>; well-formed surrogate pairs are copied through
     * unchanged (see {@link #writeCharacter(char)}).</p>
     *
     * @param str the string to write; if {@code null}, the literal text {@code "null"} is written
     * @throws IOException if this writer is closed, or writing the escaped characters to the underlying
     *         output stream or writer fails
     */
    @Override
    public void writeCharacter(final String str) throws IOException {
        if (str == null) {
            write(Strings.NULL_CHAR_ARRAY);
        } else {
            writeCharacter(str, 0, str.length());
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Unpaired surrogates are escaped as <code>&#92;uXXXX</code>; well-formed surrogate pairs are copied through
     * unchanged (see {@link #writeCharacter(char)}). A pair split by the requested range boundary counts as
     * unpaired.</p>
     *
     * @param str the string containing the data to write; if {@code null}, the literal text {@code "null"}
     *        is used as the source
     * @param off the start offset in the string (or in {@code "null"} when {@code str} is {@code null});
     *        must be non-negative and not greater than the effective length
     * @param len the number of characters to write; must be non-negative and {@code off + len} must not
     *        exceed the effective length
     * @throws IOException if this writer is closed, or writing the escaped characters to the underlying
     *         output stream or writer fails
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is negative, or {@code off + len}
     *         exceeds the effective length
     */
    @Override
    public void writeCharacter(final String str, final int off, final int len) throws IOException, IndexOutOfBoundsException {
        if (str == null) {
            write(Strings.NULL_CHAR_ARRAY, off, len);

            return;
        }

        ensureOpen();

        if (off < 0 || len < 0 || off > str.length() || len > str.length() - off) {
            throw new IndexOutOfBoundsException();
        }

        final int end = off + len;
        int from = off;
        char ch = 0;

        for (int i = off; i < end; i++) {
            ch = str.charAt(i);

            if (ch < Character.MIN_SURROGATE) {
                if (ch <= lengthOfReplacementsForChars && replacementsForChars[ch] != null) {
                    if (i > from) {
                        write(str, from, i - from);
                    }

                    write(replacementsForChars[ch]);
                    from = i + 1;
                }
            } else if (ch <= Character.MAX_SURROGATE) {
                if (Character.isHighSurrogate(ch) && i + 1 < end && Character.isLowSurrogate(str.charAt(i + 1))) {
                    i++; // a valid pair: copy both units through
                } else {
                    if (i > from) {
                        write(str, from, i - from);
                    }

                    write(getCharNum(ch));
                    from = i + 1;
                }
            }
        }

        if (end > from) {
            write(str, from, end - from);
        }
    }
}
