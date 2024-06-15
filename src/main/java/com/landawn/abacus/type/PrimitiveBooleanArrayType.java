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
import java.util.Collection;

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
@SuppressWarnings("java:S2160")
public final class PrimitiveBooleanArrayType extends AbstractPrimitiveArrayType<boolean[]> {

    public static final String BOOLEAN_ARRAY = boolean[].class.getSimpleName();

    private final Type<Boolean> elementType;

    PrimitiveBooleanArrayType() {
        super(BOOLEAN_ARRAY);

        elementType = TypeFactory.getType(boolean.class);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<boolean[]> clazz() {
        return boolean[].class;
    }

    /**
     * Gets the element type.
     *
     * @return
     */
    @Override
    public Type<Boolean> getElementType() {
        return elementType;
    }

    /**
     *
     * @param x
     * @return {@code null} if {@code (x == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Override
    public String stringOf(boolean[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return "[]";
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

    /**
     *
     * @param str
     * @return {@code null} if {@code (str == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Override
    public boolean[] valueOf(String str) {
        if (str == null) {
            return null; // NOSONAR
        } else if (str.length() == 0 || "[]".equals(str)) {
            return N.EMPTY_BOOLEAN_ARRAY;
        }

        String[] strs = split(str);
        int len = strs.length;
        boolean[] a = new boolean[len];

        if (len > 0) {
            for (int i = 0; i < len; i++) {
                a[i] = elementType.valueOf(strs[i]);
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
    public void appendTo(Appendable appendable, boolean[] x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(WD._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                appendable.append(x[i] ? TRUE_STRING : FALSE_STRING);
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
    public void writeCharacter(CharacterWriter writer, boolean[] x, JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(WD._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                writer.write(x[i] ? TRUE_CHAR_ARRAY : FALSE_CHAR_ARRAY);
            }

            writer.write(WD._BRACKET_R);
        }
    }

    /**
     * Collection 2 array.
     *
     * @param c
     * @return {@code null} if {@code (c == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Override
    public boolean[] collection2Array(Collection<?> c) {
        if (c == null) {
            return null; // NOSONAR
        }

        boolean[] a = new boolean[c.size()];

        int i = 0;

        for (Object e : c) {
            a[i++] = (Boolean) e;
        }

        return a;
    }

    /**
     * Array 2 collection.
     * @param x
     * @param output
     *
     * @param <E>
     * @return
     */
    @Override
    public <E> Collection<E> array2Collection(final boolean[] x, final Collection<E> output) {
        if (N.isEmpty(x)) {
            return output;
        }

        Collection<Object> c = (Collection<Object>) output;

        for (boolean element : x) {
            c.add(element);
        }

        return output;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public int hashCode(boolean[] x) {
        return N.hashCode(x);
    }

    /**
     *
     * @param x
     * @param y
     * @return true, if successful
     */
    @Override
    public boolean equals(boolean[] x, boolean[] y) {
        return N.equals(x, y);
    }
}
