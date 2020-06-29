
package com.landawn.abacus.util;

import java.io.OutputStream;
import java.io.Writer;

/**
 *
 * @author Haiyang Li
 * @since 0.8
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
     * http://www.apache.org/licenses/LICENSE-2.0
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
        int length = 128;
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

    BufferedXMLWriter() {
        super(REPLACEMENT_CHARS);
    }

    BufferedXMLWriter(OutputStream os) {
        super(os, REPLACEMENT_CHARS);
    }

    BufferedXMLWriter(Writer writer) {
        super(writer, REPLACEMENT_CHARS);
    }

    /**
     * Gets the char quotation.
     *
     * @return
     */
    char getCharQuotation() {
        return N.CHAR_0;
    }

    /**
     * Gets the hex string.
     *
     * @param ch
     * @return
     */
    protected static String getHexString(int ch) {
        return "&#x" + Integer.toHexString(ch) + ";";
    }
}
