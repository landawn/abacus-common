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
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Collection;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 *
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveCharArrayType extends AbstractPrimitiveArrayType<char[]> {

    public static final String CHAR_ARRAY = char[].class.getSimpleName();

    private final Type<Character> elementType;

    PrimitiveCharArrayType() {
        super(CHAR_ARRAY);

        elementType = TypeFactory.getType(char.class);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<char[]> clazz() {
        return char[].class;
    }

    /**
     * Gets the element type.
     *
     * @return
     */
    @Override
    public Type<Character> getElementType() {
        return elementType;
    }

    /**
     *
     * @param x
     * @return
     */
    @MayReturnNull
    @Override
    public String stringOf(final char[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(x.length, 5));

        sb.append(WD._BRACKET_L);

        for (int i = 0, len = x.length; i < len; i++) {
            if (i > 0) {
                sb.append(ELEMENT_SEPARATOR);
            }

            sb.append(WD.QUOTATION_S);
            sb.append(x[i]);
            sb.append(WD.QUOTATION_S);
        }

        sb.append(WD._BRACKET_R);

        final String str = sb.toString();

        Objectory.recycle(sb);

        return str;
    }

    /**
     *
     * @param str
     * @return
     */
    @MayReturnNull
    @Override
    public char[] valueOf(final String str) {
        if (str == null) {
            return null; // NOSONAR
        } else if (str.length() == 0 || STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_CHAR_ARRAY;
        }

        final String[] strs = split(str);
        final int len = strs.length;
        final char[] a = new char[len];

        if (len > 0) {
            boolean isQuoted = false;

            if ((strs[0].length() > 1) && ((strs[0].charAt(0) == WD._QUOTATION_S) || (strs[0].charAt(0) == WD._QUOTATION_D))) {
                isQuoted = true;
            }

            if (isQuoted) {
                for (int i = 0; i < len; i++) {
                    a[i] = elementType.valueOf(strs[i].substring(1, strs[i].length() - 1));
                }
            } else {
                for (int i = 0; i < len; i++) {
                    a[i] = elementType.valueOf(strs[i]);
                }
            }
        }

        return a;
    }

    /**
     *
     *
     * @param obj
     * @return
     */
    @MayReturnNull
    @SuppressFBWarnings
    @Override
    public char[] valueOf(final Object obj) {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof final Clob clob) {
            try {
                return clob.getSubString(1, (int) clob.length()).toCharArray();
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e);
            } finally {
                try {
                    clob.free();
                } catch (final SQLException e) {
                    throw new UncheckedSQLException(e); //NOSONAR
                }
            }
        } else {
            return valueOf(N.typeOf(obj.getClass()).stringOf(obj));
        }
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final char[] x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(WD._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                appendable.append(x[i]);
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
    public void writeCharacter(final CharacterWriter writer, final char[] x, final JSONXMLSerializationConfig<?> config) throws IOException {
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

                    writer.write(charQuotation);

                    if (x[i] == '\'' && charQuotation == '\'') {
                        writer.write('\\');
                    }

                    writer.writeCharacter(x[i]);
                    writer.write(charQuotation);
                }
            } else {
                for (int i = 0, len = x.length; i < len; i++) {
                    if (i > 0) {
                        writer.write(ELEMENT_SEPARATOR);
                    }

                    writer.writeCharacter(x[i]);
                }
            }

            writer.write(WD._BRACKET_R);
        }
    }

    /**
     * Collection 2 array.
     *
     * @param c
     * @return
     */
    @MayReturnNull
    @Override
    public char[] collection2Array(final Collection<?> c) {
        if (c == null) {
            return null; // NOSONAR
        }

        final char[] a = new char[c.size()];

        int i = 0;

        for (final Object e : c) {
            a[i++] = (Character) e;
        }

        return a;
    }

    /**
     * Array 2 collection.
     *
     * @param <E>
     * @param x
     * @param output
     */
    @Override
    public <E> void array2Collection(final char[] x, final Collection<E> output) {
        if (N.notEmpty(x)) {
            final Collection<Object> c = (Collection<Object>) output;

            for (final char element : x) {
                c.add(element);
            }
        }
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public int hashCode(final char[] x) {
        return N.hashCode(x);
    }

    /**
     *
     * @param x
     * @param y
     * @return {@code true}, if successful
     */
    @Override
    public boolean equals(final char[] x, final char[] y) {
        return N.equals(x, y);
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String toString(final char[] x) {
        if (x == null) {
            return NULL_STRING;
        }

        //    final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(x.length, 3));
        //
        //    sb.append(WD._BRACKET_L);
        //
        //    for (int i = 0, len = x.length; i < len; i++) {
        //        if (i > 0) {
        //            sb.append(ELEMENT_SEPARATOR);
        //        }
        //
        //        sb.append(x[i]);
        //    }
        //
        //    sb.append(WD._BRACKET_R);
        //
        //    final String str = sb.toString();
        //
        //    Objectory.recycle(sb);
        //
        //    return str;

        return Strings.join(x, 0, x.length, ELEMENT_SEPARATOR, WD.BRACKET_L, WD.BRACKET_R);
    }
}
