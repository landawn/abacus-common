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
 * @see com.landawn.abacus.util.CSVUtil
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
    BufferedCSVWriter() {
        super(CSVUtil.isBackSlashEscapeCharForWrite() ? REPLACEMENT_CHARS_BACK_SLASH : REPLACEMENT_CHARS);
    }

    BufferedCSVWriter(final OutputStream os) {
        super(os, CSVUtil.isBackSlashEscapeCharForWrite() ? REPLACEMENT_CHARS_BACK_SLASH : REPLACEMENT_CHARS);
    }

    BufferedCSVWriter(final Writer writer) {
        super(writer, CSVUtil.isBackSlashEscapeCharForWrite() ? REPLACEMENT_CHARS_BACK_SLASH : REPLACEMENT_CHARS);
    }

    boolean isBackSlash() {
        return replacementsForChars['"'] == BACK_SLASH_CHAR_ARRAY;
    }
}
