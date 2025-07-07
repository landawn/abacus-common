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
 * A specialized writer for efficient XML output with automatic character escaping.
 * This class extends CharacterWriter and provides optimized writing of XML content
 * with proper escaping of special XML characters.
 * 
 * <p>The following characters are automatically escaped:</p>
 * <ul>
 *   <li>&amp; becomes &amp;amp;</li>
 *   <li>&lt; becomes &amp;lt;</li>
 *   <li>&gt; becomes &amp;gt;</li>
 *   <li>" becomes &amp;quot;</li>
 *   <li>' becomes &amp;apos;</li>
 *   <li>Control characters (0x00-0x1F) are escaped as numeric character references</li>
 * </ul>
 * 
 * <p>This writer is designed for high-performance XML generation and automatically
 * handles character escaping to ensure valid XML output. It provides three modes
 * of operation: internal buffering, writing to an OutputStream, or writing to
 * another Writer.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * try (BufferedXMLWriter writer = new BufferedXMLWriter()) {
 *     writer.write("<root>");
 *     writer.write("<item>Value with & special < > characters</item>");
 *     writer.write("</root>");
 *     String xml = writer.toString();
 *     // Result: <root><item>Value with &amp; special &lt; &gt; characters</item></root>
 * }
 * }</pre>
 * 
 * @see CharacterWriter
 * @since 1.0
 */
public final class BufferedXMLWriter extends CharacterWriter {

    // start
    // ======================================================================================================>>>
    /*
     * Copyright (C) 2010 Google Inc.
     *
     * Licensed under the Apache License, Version 2.0 (the "License"); you may
     * not use this file except in compliance with the License. You may obtain a
     * copy of the License at
     *
     * https://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
     * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
     * License for the specific language governing permissions and limitations
     * under the License.
     */

    /*
     * From RFC 4627, "All Unicode characters may be placed within the quotation
     * marks except for the characters that must be escaped: quotation mark,
     * reverse solidus, and the control characters (U+0000 through U+001F)."
     *
     * We also escape '\u2028' and '\u2029', which JavaScript interprets as
     * newline characters. This prevents eval() from failing with a syntax
     * error. http://code.google.com/p/google-gson/issues/detail?id=341
     */
    private static final char[][] REPLACEMENT_CHARS;

    static {
        final int length = 128;
        REPLACEMENT_CHARS = new char[length][];

        // for (int i = 0; i < 128; i++) {
        // REPLACEMENT_CHARS[i] = getHexString(i);
        // }
        for (int i = 0; i < length; i++) {
            if ((i < 32) || (i == 127)) {
                REPLACEMENT_CHARS[i] = getHexString(i).toCharArray();
            }
        }

        REPLACEMENT_CHARS['"'] = "&quot;".toCharArray();
        REPLACEMENT_CHARS['\''] = "&apos;".toCharArray();
        REPLACEMENT_CHARS['<'] = "&lt;".toCharArray();
        REPLACEMENT_CHARS['>'] = "&gt;".toCharArray();
        REPLACEMENT_CHARS['&'] = "&amp;".toCharArray();

        // REPLACEMENT_CHARS['\\'] = getHexString('\\');
        // REPLACEMENT_CHARS['\n'] = getHexString('\n');
        // REPLACEMENT_CHARS['\r'] = getHexString('\r');
        // REPLACEMENT_CHARS['\t'] = getHexString('\t');
        // REPLACEMENT_CHARS['\b'] = getHexString('\b');
        // REPLACEMENT_CHARS['\f'] = getHexString('\f');
    }

    /**
     * Creates a new BufferedXMLWriter with an internal buffer.
     * The content is stored in memory and can be retrieved using toString().
     * 
     * <p>This constructor is package-private. Use factory methods or builder
     * patterns to create instances.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BufferedXMLWriter writer = new BufferedXMLWriter();
     * writer.write("<element>content</element>");
     * String xml = writer.toString();
     * }</pre>
     */
    BufferedXMLWriter() {
        super(REPLACEMENT_CHARS);
    }

    /**
     * Creates a new BufferedXMLWriter that writes to the specified OutputStream.
     * Characters are encoded using the default character encoding.
     * 
     * <p>The writer will automatically escape XML special characters as they
     * are written to the output stream. The stream is not closed when the
     * writer is closed; this is the caller's responsibility.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * try (FileOutputStream fos = new FileOutputStream("output.xml");
     *      BufferedXMLWriter writer = new BufferedXMLWriter(fos)) {
     *     writer.write("<?xml version=\"1.0\"?>");
     *     writer.write("<root>content</root>");
     * }
     * }</pre>
     *
     * @param os the OutputStream to write to
     */
    BufferedXMLWriter(final OutputStream os) {
        super(os, REPLACEMENT_CHARS);
    }

    /**
     * Creates a new BufferedXMLWriter that writes to the specified Writer.
     * 
     * <p>The writer will automatically escape XML special characters as they
     * are written. The underlying Writer is not closed when this writer
     * is closed; this is the caller's responsibility.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * try (FileWriter fw = new FileWriter("output.xml");
     *      BufferedXMLWriter writer = new BufferedXMLWriter(fw)) {
     *     writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
     *     writer.write("<root>content</root>");
     * }
     * }</pre>
     *
     * @param writer the Writer to write to
     */
    BufferedXMLWriter(final Writer writer) {
        super(writer, REPLACEMENT_CHARS);
    }

    /**
     * Returns the character used for quotation in XML attributes.
     * This implementation always returns CHAR_ZERO (0) as XML doesn't require
     * a specific quotation character for element content.
     * 
     * <p>This method is used internally by the writer infrastructure and
     * typically should not be called directly by client code.</p>
     * 
     * @return WD.CHAR_ZERO (the null character)
     */
    @SuppressWarnings("SameReturnValue")
    char getCharQuotation() {
        return WD.CHAR_ZERO;
    }
}