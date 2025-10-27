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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.BufferedJSONWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for object arrays, providing serialization, deserialization,
 * and collection conversion capabilities for arrays of any object type.
 * This handler supports JSON serialization and handles nested array elements
 * by delegating to their respective type handlers.
 *
 * @param <T> the component type of the array
 */
public class ObjectArrayType<T> extends AbstractArrayType<T[]> { //NOSONAR

    protected final Class<T[]> typeClass;

    protected final Type<T> elementType;
    protected final Type<T>[] parameterTypes;

    protected final JSONDeserializationConfig jdc;

    ObjectArrayType(final Class<T[]> arrayClass) {
        super(ClassUtil.getCanonicalClassName(arrayClass));

        typeClass = arrayClass;
        elementType = TypeFactory.getType(arrayClass.getComponentType());
        this.parameterTypes = new Type[] { elementType };

        jdc = JDC.create().setElementType(elementType);
    }

    ObjectArrayType(final Type<T> elementType) {
        super(elementType.name() + "[]");

        typeClass = (Class<T[]>) N.newArray(elementType.clazz(), 0).getClass();
        this.elementType = elementType;
        this.parameterTypes = new Type[] { elementType };

        jdc = JDC.create().setElementType(elementType);
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * @return the array class object
     */
    @Override
    public Class<T[]> clazz() {
        return typeClass;
    }

    /**
     * Gets the type handler for the array's element type.
     *
     * @return the Type handler for array elements
     */
    @Override
    public Type<T> getElementType() {
        return elementType;
    }

    @Override
    public Type<T>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type represents an object array.
     *
     * @return {@code true}, as this is an object array type
     */
    @Override
    public boolean isObjectArray() {
        return true;
    }

    /**
     * Indicates whether arrays of this type can be serialized.
     * Serialization capability depends on whether the element type is serializable.
     *
     * @return {@code true} if the element type is serializable, {@code false} otherwise
     */
    @Override
    public boolean isSerializable() {
        return elementType.isSerializable();
    }

    /**
     * Converts an object array to its JSON string representation.
     * If the element type is serializable, performs custom JSON serialization.
     * Otherwise, delegates to the JSON parser.
     *
     * @param x the array to convert
     * @return JSON string representation, {@code null} if input is {@code null}, or "[]" for empty arrays
     * @throws UncheckedIOException if an I/O error occurs during serialization
     */
    @MayReturnNull
    @Override

    public String stringOf(final T[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
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
     * Converts a JSON string representation to an object array.
     * Handles {@code null}, empty strings, and the special "[]" representation for empty arrays.
     *
     * @param str the JSON string to parse
     * @return the parsed array, {@code null} if input is {@code null}, or empty array for empty representations
     */
    @MayReturnNull
    @Override

    public T[] valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return Array.newInstance(elementType.clazz(), 0);
        } else {
            return Utils.jsonParser.deserialize(str, jdc, typeClass);
        }
    }

    /**
     * Appends the JSON representation of an object array to an Appendable.
     * Optimizes performance by using buffered writers when appropriate.
     *
     * @param appendable the Appendable to write to
     * @param x the array to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final T[] x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer writer) {
                final boolean isBufferedWriter = IOUtil.isBufferedWriter(writer);
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
                            elementType.appendTo(bw, x[i]);
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
                for (final T e : x) {
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
     * Writes the JSON character representation of an object array to a CharacterWriter.
     * This method is typically used for JSON/XML serialization and handles {@code null} elements.
     *
     * @param writer the CharacterWriter to write to
     * @param x the array to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final T[] x, final JSONXMLSerializationConfig<?> config) throws IOException {
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

            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Converts a Collection to an array of the appropriate type.
     * Creates a new array with the same size as the collection and copies all elements.
     *
     * @param c the collection to convert
     * @return an array containing all elements from the collection, or {@code null} if the collection is null
     */
    @MayReturnNull
    @Override

    public T[] collection2Array(final Collection<?> c) {
        if (c == null) {
            return null; // NOSONAR
        }

        final Object[] a = N.newArray(typeClass.getComponentType(), c.size());

        int i = 0;

        for (final Object e : c) {
            a[i++] = e;
        }

        return (T[]) a;
    }

    /**
     * Converts an array to a Collection by adding all array elements to the provided collection.
     *
     * @param <E> the element type of the collection
     * @param x the array to convert
     * @param output the collection to add elements to
     */
    @Override
    public <E> void array2Collection(final T[] x, final Collection<E> output) {
        if (N.notEmpty(x)) {
            final Collection<Object> c = (Collection<Object>) output;

            c.addAll(Array.asList(x));
        }
    }

    /**
     * Computes a hash code for the given array.
     * This method computes a shallow hash code based on array elements.
     *
     * @param x the array to hash
     * @return the computed hash code
     */
    @Override
    public int hashCode(final Object[] x) {
        return N.hashCode(x);
    }

    /**
     * Computes a deep hash code for the given array.
     * This method recursively computes hash codes for nested arrays and objects.
     *
     * @param x the array to hash
     * @return the computed deep hash code
     */
    @Override
    public int deepHashCode(final Object[] x) {
        return N.deepHashCode(x);
    }

    /**
     * Compares two arrays for equality.
     * This method performs a shallow equality check on array elements.
     *
     * @param x the first array
     * @param y the second array
     * @return {@code true} if the arrays are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object[] x, final Object[] y) {
        return N.equals(x, y);
    }

    /**
     * Performs a deep comparison of two arrays for equality.
     * This method recursively compares nested arrays and objects.
     *
     * @param x the first array
     * @param y the second array
     * @return {@code true} if the arrays are deeply equal, {@code false} otherwise
     */
    @Override
    public boolean deepEquals(final Object[] x, final Object[] y) {
        return N.deepEquals(x, y);
    }

    /**
     * Creates a string representation of the array.
     * This method produces a shallow string representation using toString() on elements.
     *
     * @param x the array to convert to string
     * @return string representation of the array, {@code null} if input is {@code null}, or "[]" for empty arrays
     */
    @Override
    @MayReturnNull
    public String toString(final Object[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return N.toString(x);
    }

    /**
     * Creates a deep string representation of the array.
     * This method recursively converts nested arrays and objects to strings.
     *
     * @param x the array to convert to string
     * @return deep string representation of the array, {@code null} if input is {@code null}, or "[]" for empty arrays
     */
    @Override
    @MayReturnNull
    public String deepToString(final Object[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return N.deepToString(x);
    }
}
