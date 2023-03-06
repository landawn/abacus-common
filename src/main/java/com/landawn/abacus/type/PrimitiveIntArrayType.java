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
import java.util.Collection;

import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveIntArrayType extends AbstractPrimitiveArrayType<int[]> {

    public static final String INT_ARRAY = int[].class.getSimpleName();

    private Type<Integer> elementType;

    PrimitiveIntArrayType() {
        super(INT_ARRAY);

        elementType = TypeFactory.getType(int.class);
    }

    @Override
    public Class<int[]> clazz() {
        return int[].class;
    }

    /**
     * Gets the element type.
     *
     * @return
     */
    @Override
    public Type<Integer> getElementType() {
        return elementType;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(int[] x) {
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
     * @return
     */
    @Override
    public int[] valueOf(String str) {
        if (str == null) {
            return null;
        } else if (str.length() == 0 || "[]".equals(str)) {
            return N.EMPTY_INT_ARRAY;
        }

        String[] strs = split(str);
        int len = strs.length;
        int[] a = new int[len];

        if (len > 0) {
            for (int i = 0; i < len; i++) {
                a[i] = elementType.valueOf(strs[i]);
            }
        }

        return a;
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, int[] x) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(WD._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                IOUtil.write(writer, x[i]);
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
    public void writeCharacter(CharacterWriter writer, int[] x, SerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(WD._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                writer.writeInt(x[i]);
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
    public int[] collection2Array(Collection<?> c) {
        if (c == null) {
            return null;
        }

        int[] a = new int[c.size()];

        int i = 0;

        for (Object e : c) {
            a[i++] = (Integer) e;
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
    public <E> Collection<E> array2Collection(Collection<E> resultCollection, int[] x) {
        if (N.isNullOrEmpty(x)) {
            return resultCollection;
        }

        Collection<Object> c = (Collection<Object>) resultCollection;

        for (int element : x) {
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
    public int hashCode(int[] x) {
        return N.hashCode(x);
    }

    /**
     *
     * @param x
     * @param y
     * @return true, if successful
     */
    @Override
    public boolean equals(int[] x, int[] y) {
        return N.equals(x, y);
    }
}
