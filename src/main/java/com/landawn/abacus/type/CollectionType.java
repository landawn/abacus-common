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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.BufferedJSONWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.WD;

/**
 *
 * @param <E>
 * @param <T>
 */
@SuppressWarnings("java:S2160")
public class CollectionType<E, T extends Collection<E>> extends AbstractType<T> {

    private final String declaringName;

    private final Class<T> typeClass;

    private final Type<E>[] parameterTypes;

    private final Type<E> elementType;

    private final boolean isList;

    private final boolean isSet;

    private final JSONDeserializationConfig jdc;

    CollectionType(final Class<T> typeClass, final String parameterTypeName) {
        super(getTypeName(typeClass, parameterTypeName, false));

        String tmp = null;

        if (typeClass.isInterface()) {
            tmp = getTypeName(typeClass, parameterTypeName, true);
        } else {
            if (List.class.isAssignableFrom(typeClass)) {
                tmp = getTypeName(List.class, parameterTypeName, true);
            } else if (Set.class.isAssignableFrom(typeClass)) {
                tmp = getTypeName(Set.class, parameterTypeName, true);
            } else if (Queue.class.isAssignableFrom(typeClass)) {
                tmp = getTypeName(Queue.class, parameterTypeName, true);
            } else {
                tmp = getTypeName(Collection.class, parameterTypeName, true);

                final Class<?>[] interfaceClasses = typeClass.getInterfaces();

                for (final Class<?> interfaceClass : interfaceClasses) {
                    if (Collection.class.isAssignableFrom(interfaceClass) && !interfaceClass.equals(Collection.class)) {
                        tmp = getTypeName(interfaceClass, parameterTypeName, true);

                        break;
                    }
                }
            }
        }

        declaringName = tmp;

        this.typeClass = typeClass;
        parameterTypes = new Type[] { TypeFactory.getType(parameterTypeName) };
        elementType = parameterTypes[0];

        jdc = JDC.create().setElementType(elementType);

        isList = List.class.isAssignableFrom(this.typeClass);
        isSet = Set.class.isAssignableFrom(this.typeClass);
    }

    @Override
    public String declaringName() {
        return declaringName;
    }

    @Override
    public Class<T> clazz() {
        return typeClass;
    }

    /**
     * Gets the parameter types.
     *
     * @return
     */
    @Override
    public Type<E>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Gets the element type.
     *
     * @return
     */
    @Override
    public Type<E> getElementType() {
        return elementType;
    }

    @Override
    public boolean isList() {
        return isList;
    }

    @Override
    public boolean isSet() {
        return isSet;
    }

    @Override
    public boolean isCollection() {
        return true;
    }

    /**
     * Checks if is generic type.
     *
     * @return {@code true}, if is generic type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Checks if is serializable.
     *
     * @return {@code true}, if is serializable
     */
    @Override
    public boolean isSerializable() {
        return elementType.isSerializable();
    }

    /**
     * Gets the serialization type.
     *
     * @return
     */
    @Override
    public SerializationType getSerializationType() {
        return isSerializable() ? SerializationType.SERIALIZABLE : SerializationType.COLLECTION;
    }

    /**
     *
     * @param x
     * @return
     */
    @MayReturnNull
    @Override
    public String stringOf(final T x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.size() == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        if (this.isSerializable()) {
            final BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();

            try {
                bw.write(WD._BRACKET_L);

                int i = 0;
                for (final E e : x) {
                    if (i++ > 0) {
                        bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }

                    if (e == null) {
                        bw.write(NULL_CHAR_ARRAY);
                    } else {
                        elementType.writeCharacter(bw, e, Utils.jsc);
                    }
                }

                bw.write(WD._BRACKET_R);

                return bw.toString();
            } catch (final IOException e) {
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
    @MayReturnNull
    @Override
    public T valueOf(final String str) {
        if (str == null) {
            return null; // NOSONAR
        } else if (str.isEmpty() || STR_FOR_EMPTY_ARRAY.equals(str)) {
            return (T) N.newCollection(typeClass);
        } else {
            return Utils.jsonParser.deserialize(str, jdc, typeClass);
        }
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final T x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer writer) {
                final boolean isBufferedWriter = IOUtil.isBufferedWriter(writer);
                final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer); //NOSONAR

                try {
                    bw.write(WD._BRACKET_L);

                    int i = 0;
                    for (final E e : x) {
                        if (i++ > 0) {
                            bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                        }

                        if (e == null) {
                            bw.write(NULL_CHAR_ARRAY);
                        } else {
                            elementType.appendTo(bw, e);
                        }
                    }

                    bw.write(WD._BRACKET_R);

                    if (!isBufferedWriter) {
                        bw.flush();
                    }
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                } finally {
                    if (!isBufferedWriter) {
                        Objectory.recycle((BufferedWriter) bw);
                    }
                }
            } else {
                appendable.append(WD._BRACKET_L);

                int i = 0;
                for (final E e : x) {
                    if (i++ > 0) {
                        appendable.append(ELEMENT_SEPARATOR);
                    }

                    if (e == null) {
                        appendable.append(NULL_STRING);
                    } else {
                        elementType.appendTo(appendable, e);
                    }
                }

                appendable.append(WD._BRACKET_R);
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
    public void writeCharacter(final CharacterWriter writer, final T x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            try {
                writer.write(WD._BRACKET_L);

                int i = 0;
                for (final E e : x) {
                    if (i++ > 0) {
                        writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }

                    if (e == null) {
                        writer.write(NULL_CHAR_ARRAY);
                    } else {
                        elementType.writeCharacter(writer, e, config);
                    }
                }

                writer.write(WD._BRACKET_R);

            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Gets the type name.
     *
     * @param typeClass
     * @param parameterTypeName
     * @param isDeclaringName
     * @return
     */
    protected static String getTypeName(final Class<?> typeClass, final String parameterTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + WD.GREATER_THAN;
        }
    }
}
