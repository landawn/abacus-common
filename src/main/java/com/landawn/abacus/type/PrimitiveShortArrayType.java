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

package com.landawn.abacus.type;

import java.io.IOException;
import java.util.Collection;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

@SuppressWarnings("java:S2160")
public final class PrimitiveShortArrayType extends AbstractPrimitiveArrayType<short[]> {

    public static final String SHORT_ARRAY = short[].class.getSimpleName();

    private final Type<Short> elementType;

    PrimitiveShortArrayType() {
        super(SHORT_ARRAY);

        elementType = TypeFactory.getType(short.class);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return short[].class;
    }

    /**
     * Gets the element type.
     *
     * @return
     */
    @Override
    public Type<Short> getElementType() {
        return elementType;
    }

    /**
     *
     * @param x
     * @return
     */
    @MayReturnNull
    @Override
    public String stringOf(final short[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        //    final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(x.length, 7));
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

    /**
     *
     * @param str
     * @return
     */
    @MayReturnNull
    @Override
    public short[] valueOf(final String str) {
        if (str == null) {
            return null; // NOSONAR
        } else if (str.isEmpty() || STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_SHORT_ARRAY;
        }

        final String[] strs = split(str);
        final int len = strs.length;
        final short[] a = new short[len];

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
    public void appendTo(final Appendable appendable, final short[] x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(WD._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                appendable.append(N.stringOf(x[i]));
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
    public void writeCharacter(final CharacterWriter writer, final short[] x, final JSONXMLSerializationConfig<?> config) throws IOException {
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
     * Collection 2 array.
     *
     * @param c
     * @return
     */
    @MayReturnNull
    @Override
    public short[] collection2Array(final Collection<?> c) {
        if (c == null) {
            return null; // NOSONAR
        }

        final short[] a = new short[c.size()];

        int i = 0;

        for (final Object e : c) {
            a[i++] = (Short) e;
        }

        return a;
    }

    @Override
    public <E> void array2Collection(final short[] x, final Collection<E> output) {
        if (N.notEmpty(x)) {
            final Collection<Object> c = (Collection<Object>) output;

            for (final short element : x) {
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
    public int hashCode(final short[] x) {
        return N.hashCode(x);
    }

    /**
     *
     * @param x
     * @param y
     * @return {@code true}, if successful
     */
    @Override
    public boolean equals(final short[] x, final short[] y) {
        return N.equals(x, y);
    }
}
