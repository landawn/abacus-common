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
import java.io.Writer;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Collection;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveCharArrayType extends AbstractPrimitiveArrayType<char[]> {

    public static final String CHAR_ARRAY = char[].class.getSimpleName();

    private Type<Character> elementType;

    PrimitiveCharArrayType() {
        super(CHAR_ARRAY);

        elementType = TypeFactory.getType(char.class);
    }

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
    @Override
    public String stringOf(char[] x) {
        if (x == null) {
            return null;
        } else if (x.length == 0) {
            return "[]";
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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

        String str = sb.toString();

        Objectory.recycle(sb);

        return str;
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public char[] valueOf(String str) {
        if (str == null) {
            return null;
        } else if (str.length() == 0 || "[]".equals(str)) {
            return N.EMPTY_CHAR_ARRAY;
        }

        String[] strs = split(str);
        int len = strs.length;
        char[] a = new char[len];

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

    @SuppressFBWarnings
    @Override
    public char[] valueOf(final Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Clob clob) {
            try {
                return clob.getSubString(1, (int) clob.length()).toCharArray();
            } catch (SQLException e) {
                throw new UncheckedSQLException(e);
            } finally {
                try {
                    clob.free();
                } catch (SQLException e) {
                    throw new UncheckedSQLException(e); //NOSONAR
                }
            }
        } else {
            return valueOf(N.typeOf(obj.getClass()).stringOf(obj));
        }
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, char[] x) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(WD._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                writer.write(x[i]);
            }

            writer.write(WD._BRACKET_R);
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
    public void writeCharacter(CharacterWriter writer, char[] x, SerializationConfig<?> config) throws IOException {
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
    @Override
    public char[] collection2Array(Collection<?> c) {
        if (c == null) {
            return null;
        }

        char[] a = new char[c.size()];

        int i = 0;

        for (Object e : c) {
            a[i++] = (Character) e;
        }

        return a;
    }

    /**
     * Array 2 collection.
     *
     * @param <E>
     * @param resultCollection
     * @param x
     * @return
     */
    @Override
    public <E> Collection<E> array2Collection(Collection<E> resultCollection, char[] x) {
        if (N.isNullOrEmpty(x)) {
            return resultCollection;
        }

        Collection<Object> c = (Collection<Object>) resultCollection;

        for (char element : x) {
            c.add(element);
        }

        return resultCollection;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public int hashCode(char[] x) {
        return N.hashCode(x);
    }

    /**
     *
     * @param x
     * @param y
     * @return true, if successful
     */
    @Override
    public boolean equals(char[] x, char[] y) {
        return N.equals(x, y);
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String toString(char[] x) {
        if (x == null) {
            return NULL_STRING;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        sb.append(WD._BRACKET_L);

        for (int i = 0, len = x.length; i < len; i++) {
            if (i > 0) {
                sb.append(ELEMENT_SEPARATOR);
            }

            sb.append(x[i]);
        }

        sb.append(WD._BRACKET_R);

        String str = sb.toString();

        Objectory.recycle(sb);

        return str;
    }
}
