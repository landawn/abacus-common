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

public abstract sealed class CharacterWriter extends BufferedWriter permits BufferedJSONWriter, BufferedXMLWriter, BufferedCSVWriter {

    protected final char[][] replacementsForChars;

    protected final int lengthOfReplacementsForChars;

    CharacterWriter(final char[][] replacementsForChars) {
        this.replacementsForChars = replacementsForChars;
        lengthOfReplacementsForChars = replacementsForChars.length - 1;
    }

    CharacterWriter(final OutputStream os, final char[][] replacementsForChars) {
        super(os);
        this.replacementsForChars = replacementsForChars;
        lengthOfReplacementsForChars = replacementsForChars.length - 1;
    }

    CharacterWriter(final Writer writer, final char[][] replacementsForChars) {
        super(writer);
        this.replacementsForChars = replacementsForChars;
        lengthOfReplacementsForChars = replacementsForChars.length - 1;
    }

    /**
     *
     * @param ch
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void writeCharacter(final char ch) throws IOException {
        if ((ch > lengthOfReplacementsForChars) || (replacementsForChars[ch] == null)) {
            write(ch);
        } else {
            write(replacementsForChars[ch]);
        }
    }

    /**
     *
     * @param cbuf
     * @throws IOException Signals that an I/O exception has occurred.
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
     *
     * @param cbuf
     * @param off
     * @param len
     * @throws IOException Signals that an I/O exception has occurred.
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
     *
     * @param str
     * @throws IOException Signals that an I/O exception has occurred.
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
     *
     * @param str
     * @param off
     * @param len
     * @throws IOException Signals that an I/O exception has occurred.
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
     * Gets the hex string.
     *
     * @param ch
     * @return
     */
    static String getHexString(final int ch) {
        return "&#x" + Integer.toHexString(ch) + ";";
    }

    /**
     * Gets the char number.
     *
     * @param ch
     * @return
     */
    static String getCharNum(final char ch) {
        return String.format("\\u%04x", (int) ch);
    }
}
