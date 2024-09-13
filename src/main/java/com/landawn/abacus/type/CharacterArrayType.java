/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.type;

import java.io.IOException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class CharacterArrayType extends ObjectArrayType<Character> {

    CharacterArrayType() {
        super(Character[].class);
    }

    /**
     *
     * @param x
     * @return {@code null} if {@code (x == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Override
    public String stringOf(final Character[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return "[]";
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(x.length, 5));
        sb.append(WD._BRACKET_L);

        for (int i = 0, len = x.length; i < len; i++) {
            if (i > 0) {
                sb.append(ELEMENT_SEPARATOR);
            }

            if (x[i] == null) {
                sb.append(NULL_CHAR_ARRAY);
            } else {
                sb.append(WD.QUOTATION_S);
                sb.append(x[i]);
                sb.append(WD.QUOTATION_S);
            }
        }

        sb.append(WD._BRACKET_R);

        final String str = sb.toString();

        Objectory.recycle(sb);

        return str;
    }

    /**
     *
     * @param str
     * @return {@code null} if {@code (str == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Override
    public Character[] valueOf(final String str) {
        if (str == null) {
            return null; // NOSONAR
        } else if (str.length() == 0 || "[]".equals(str)) {
            return N.EMPTY_CHAR_OBJ_ARRAY;
        }

        final String[] strs = split(str);
        final int len = strs.length;
        final Character[] a = new Character[len];

        if (len > 0) {
            boolean isQuoted = false;

            if ((strs[0].length() > 1) && ((strs[0].charAt(0) == WD._QUOTATION_S) || (strs[0].charAt(0) == WD._QUOTATION_D))) {
                isQuoted = true;
            }

            if (isQuoted) {
                for (int i = 0; i < len; i++) {
                    if (strs[i].length() == 4 && strs[i].equals(NULL_STRING)) {
                        a[i] = null;
                    } else {
                        a[i] = elementType.valueOf(strs[i].substring(1, strs[i].length() - 1));
                    }
                }
            } else {
                for (int i = 0; i < len; i++) {
                    if (strs[i].length() == 4 && strs[i].equals(NULL_STRING)) {
                        a[i] = null;
                    } else {
                        a[i] = elementType.valueOf(strs[i]);
                    }
                }
            }
        }

        return a;
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final Character[] x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(WD._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                if (x[i] == null) {
                    appendable.append(NULL_STRING);
                } else {
                    appendable.append(x[i]);
                }
            }

            appendable.append(WD._BRACKET_R);
        }
    }

    /**
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Character[] x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(WD._BRACKET_L);

            final char charQuotation = (config == null) ? WD.CHAR_ZERO : config.getCharQuotation();

            if (charQuotation > 0) {
                for (int i = 0, len = x.length; i < len; i++) {
                    if (i > 0) {
                        writer.write(ELEMENT_SEPARATOR);
                    }

                    if (x[i] == null) {
                        writer.write(NULL_CHAR_ARRAY);
                    } else {
                        writer.write(charQuotation);

                        if (x[i] == '\'' && charQuotation == '\'') {
                            writer.write('\\');
                        }

                        writer.writeCharacter(x[i]);
                        writer.write(charQuotation);
                    }
                }
            } else {
                for (int i = 0, len = x.length; i < len; i++) {
                    if (i > 0) {
                        writer.write(ELEMENT_SEPARATOR);
                    }

                    if (x[i] == null) {
                        writer.write(NULL_CHAR_ARRAY);
                    } else {
                        writer.writeCharacter(x[i]);
                    }
                }
            }

            writer.write(WD._BRACKET_R);
        }
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String toString(final Character[] x) {
        if (x == null) {
            return NULL_STRING;
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(x.length, 3));

        sb.append(WD._BRACKET_L);

        for (int i = 0, len = x.length; i < len; i++) {
            if (i > 0) {
                sb.append(ELEMENT_SEPARATOR);
            }

            if (x[i] == null) {
                sb.append(NULL_CHAR_ARRAY);
            } else {
                sb.append(x[i]);
            }
        }

        sb.append(WD._BRACKET_R);

        final String str = sb.toString();

        Objectory.recycle(sb);

        return str;
    }
}
