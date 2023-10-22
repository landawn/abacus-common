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
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.util.BufferedJSONWriter;
import com.landawn.abacus.util.BufferedWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 */
public class ObjectArrayType<T> extends AbstractArrayType<T[]> { //NOSONAR

    protected final Class<T[]> typeClass;

    protected final Type<T> elementType;

    protected final JSONDeserializationConfig jdc;

    ObjectArrayType(Class<T[]> arrayClass) {
        super(ClassUtil.getCanonicalClassName(arrayClass));

        this.typeClass = arrayClass;
        this.elementType = TypeFactory.getType(arrayClass.getComponentType());

        this.jdc = JDC.create().setElementType(elementType);
    }

    ObjectArrayType(Type<T> elementType) {
        super(elementType.name() + "[]");

        this.typeClass = (Class<T[]>) N.newArray(elementType.clazz(), 0).getClass();
        this.elementType = elementType;

        this.jdc = JDC.create().setElementType(elementType);
    }

    /**
     * 
     *
     * @return 
     */
    @Override
    public Class<T[]> clazz() {
        return typeClass;
    }

    /**
     * Gets the element type.
     *
     * @return
     */
    @Override
    public Type<T> getElementType() {
        return elementType;
    }

    /**
     * Checks if is object array.
     *
     * @return true, if is object array
     */
    @Override
    public boolean isObjectArray() {
        return true;
    }

    /**
     * Checks if is serializable.
     *
     * @return true, if is serializable
     */
    @Override
    public boolean isSerializable() {
        return elementType.isSerializable();
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(T[] x) {
        if (x == null) {
            return null;
        } else if (x.length == 0) {
            return "[]";
        }

        if (this.isSerializable()) {
            final BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();

            try {
                bw.write(WD._BRACKET_L);

                for (int i = 0, len = x.length; i < len; i++) {
                    if (i > 0) {
                        bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }

                    if (x[i] == null) {
                        bw.write(NULL_CHAR_ARRAY);
                    } else {
                        elementType.writeCharacter(bw, x[i], Utils.jsc);
                    }
                }

                bw.write(WD._BRACKET_R);

                return bw.toString();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                Objectory.recycle(bw);
            }
        } else {
            return Utils.jsonParser.serialize(x, Utils.jsc);
        }

    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public T[] valueOf(String str) {
        if (str == null) {
            return null;
        } else if (str.length() == 0 || "[]".equals(str)) {
            return (T[]) Array.newInstance(elementType.clazz(), 0);
        } else {
            return Utils.jsonParser.deserialize(typeClass, str, jdc);
        }
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, T[] x) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            boolean isBufferedWriter = writer instanceof BufferedWriter || writer instanceof java.io.BufferedWriter;
            final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer); //NOSONAR

            try {
                bw.write(WD._BRACKET_L);

                for (int i = 0, len = x.length; i < len; i++) {
                    if (i > 0) {
                        bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }

                    if (x[i] == null) {
                        bw.write(NULL_CHAR_ARRAY);
                    } else {
                        elementType.write(bw, x[i]);
                    }
                }

                bw.write(WD._BRACKET_R);

                if (!isBufferedWriter) {
                    bw.flush();
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                if (!isBufferedWriter) {
                    Objectory.recycle((BufferedWriter) bw);
                }
            }
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
    public void writeCharacter(CharacterWriter writer, T[] x, SerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            try {
                writer.write(WD._BRACKET_L);

                for (int i = 0, len = x.length; i < len; i++) {
                    if (i > 0) {
                        writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }

                    if (x[i] == null) {
                        writer.write(NULL_CHAR_ARRAY);
                    } else {
                        elementType.writeCharacter(writer, x[i], config);
                    }
                }

                writer.write(WD._BRACKET_R);

            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Collection 2 array.
     *
     * @param c
     * @return
     */
    @Override
    public T[] collection2Array(Collection<?> c) {
        if (c == null) {
            return null;
        }

        Object[] a = N.newArray(typeClass.getComponentType(), c.size());

        int i = 0;

        for (Object e : c) {
            a[i++] = e;
        }

        return (T[]) a;
    }

    /**
     * Array 2 collection.
     *
     * @param <E2>
     * @param resultCollection
     * @param x
     * @return
     */
    @Override
    public <E2> Collection<E2> array2Collection(Collection<E2> resultCollection, T[] x) {
        if (N.isEmpty(x)) {
            return resultCollection;
        }

        Collection<Object> c = (Collection<Object>) resultCollection;

        Collections.addAll(c, x);

        return resultCollection;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public int hashCode(Object[] x) {
        return N.hashCode(x);
    }

    /**
     * Deep hash code.
     *
     * @param x
     * @return
     */
    @Override
    public int deepHashCode(Object[] x) {
        return N.deepHashCode(x);
    }

    /**
     *
     * @param x
     * @param y
     * @return true, if successful
     */
    @Override
    public boolean equals(Object[] x, Object[] y) {
        return N.equals(x, y);
    }

    /**
     *
     * @param x
     * @param y
     * @return true, if successful
     */
    @Override
    public boolean deepEquals(Object[] x, Object[] y) {
        return N.deepEquals(x, y);
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String toString(Object[] x) {
        if (x == null) {
            return NULL_STRING;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(WD._BRACKET_L);

            Object[] a = x;

            for (int i = 0, len = a.length; i < len; i++) {
                if (i > 0) {
                    sb.append(ELEMENT_SEPARATOR);
                }

                if (a[i] == null) {
                    sb.append(NULL_CHAR_ARRAY);
                } else {
                    sb.append(a[i].toString());
                }
            }

            sb.append(WD._BRACKET_R);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Deep to string.
     *
     * @param x
     * @return
     */
    @Override
    public String deepToString(Object[] x) {
        if (x == null) {
            return NULL_STRING;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(WD._BRACKET_L);

            Object[] a = x;

            for (int i = 0, len = a.length; i < len; i++) {
                if (i > 0) {
                    sb.append(ELEMENT_SEPARATOR);
                }

                if (a[i] == null) {
                    sb.append(NULL_CHAR_ARRAY);
                } else {
                    sb.append(TypeFactory.getType(a[i].getClass()).deepToString(a[i]));
                }
            }

            sb.append(WD._BRACKET_R);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }
}
