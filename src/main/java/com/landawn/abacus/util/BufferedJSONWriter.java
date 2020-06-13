
package com.landawn.abacus.util;

import java.io.OutputStream;
import java.io.Writer;

/**
 * The Class BufferedJSONWriter.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class BufferedJSONWriter extends CharacterWriter {
    // start
    // ======================================================================================================>>>
    /*
     * Copyright (C) 2010 Google Inc.
     * 
     * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
     * with the License. You may obtain a copy of the License at
     * 
     * http://www.apache.org/licenses/LICENSE-2.0
     * 
     * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
     * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
     * the specific language governing permissions and limitations under the License.
     */

    /** The Constant REPLACEMENT_CHARS. */
    /*
     * From RFC 4627, "All Unicode characters may be placed within the quotation marks except for the characters that
     * must be escaped: quotation mark, reverse solidus, and the control characters (U+0000 through U+001F)."
     * 
     * We also escape '\u2028' and '\u2029', which JavaScript interprets as newline characters. This prevents eval()
     * from failing with a syntax error. http://code.google.com/p/google-gson/issues/detail?id=341
     */
    static final char[][] REPLACEMENT_CHARS;

    /** The Constant HTML_SAFE_REPLACEMENT_CHARS. */
    static final char[][] HTML_SAFE_REPLACEMENT_CHARS;

    static {
        int length = 10000;
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
        REPLACEMENT_CHARS['\''] = "\\\'".toCharArray();
        REPLACEMENT_CHARS['\\'] = "\\\\".toCharArray();
        REPLACEMENT_CHARS['\t'] = "\\t".toCharArray();
        REPLACEMENT_CHARS['\b'] = "\\b".toCharArray();
        REPLACEMENT_CHARS['\n'] = "\\n".toCharArray();
        REPLACEMENT_CHARS['\r'] = "\\r".toCharArray();
        REPLACEMENT_CHARS['\f'] = "\\f".toCharArray();

        // ...
        REPLACEMENT_CHARS['\u2028'] = "\\u2028".toCharArray();
        REPLACEMENT_CHARS['\u2029'] = "\\u2029".toCharArray();
        //
        // // ...
        // REPLACEMENT_CHARS['{'] = getCharNum('{');
        // REPLACEMENT_CHARS['}'] = getCharNum('}');
        // REPLACEMENT_CHARS['['] = getCharNum('[');
        // REPLACEMENT_CHARS[']'] = getCharNum(']');
        // REPLACEMENT_CHARS[':'] = getCharNum(':');
        // REPLACEMENT_CHARS[','] = getCharNum(',');
        //
        //
        // // ...
        // REPLACEMENT_SINGLE_CHAR = REPLACEMENT_CHARS.clone();
        // REPLACEMENT_SINGLE_CHAR['{'] = getCharNum('{');
        // REPLACEMENT_SINGLE_CHAR['}'] = getCharNum('}');
        // REPLACEMENT_SINGLE_CHAR['['] = getCharNum('[');
        // REPLACEMENT_SINGLE_CHAR[']'] = getCharNum(']');
        // REPLACEMENT_SINGLE_CHAR[':'] = getCharNum(':');
        // REPLACEMENT_SINGLE_CHAR[','] = getCharNum(',');
        //
        // ...
        HTML_SAFE_REPLACEMENT_CHARS = REPLACEMENT_CHARS.clone();
        HTML_SAFE_REPLACEMENT_CHARS['<'] = "\\u003c".toCharArray();
        HTML_SAFE_REPLACEMENT_CHARS['>'] = "\\u003e".toCharArray();
        HTML_SAFE_REPLACEMENT_CHARS['&'] = "\\u0026".toCharArray();
        HTML_SAFE_REPLACEMENT_CHARS['='] = "\\u003d".toCharArray();
        HTML_SAFE_REPLACEMENT_CHARS['\''] = "\\u0027".toCharArray();
    }

    /** The Constant LENGTH_OF_REPLACEMENT_CHARS. */
    static final int LENGTH_OF_REPLACEMENT_CHARS = REPLACEMENT_CHARS.length - 1;

    // end
    /**
     * Instantiates a new buffered JSON writer.
     */
    // <<<======================================================================================================
    BufferedJSONWriter() {
        super(REPLACEMENT_CHARS);
    }

    /**
     * Instantiates a new buffered JSON writer.
     *
     * @param os
     */
    BufferedJSONWriter(OutputStream os) {
        super(os, REPLACEMENT_CHARS);
    }

    /**
     * Instantiates a new buffered JSON writer.
     *
     * @param writer
     */
    BufferedJSONWriter(Writer writer) {
        super(writer, REPLACEMENT_CHARS);
    }

    /**
     * Gets the char num.
     *
     * @param ch
     * @return
     */
    protected static String getCharNum(char ch) {
        return String.format("\\u%04x", (int) ch);
    }
}
