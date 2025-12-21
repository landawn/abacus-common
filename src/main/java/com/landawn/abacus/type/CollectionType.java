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
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for Collection implementations including List, Set, Queue and their concrete implementations.
 * This class provides serialization and deserialization capabilities for collection types with generic element types.
 *
 * <p>CollectionType instances are created by the TypeFactory and handle the conversion between collection
 * objects and their string representations (typically JSON format). The handler preserves generic type
 * information for proper serialization and deserialization of collection elements.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Get CollectionType for List<String>
 * Type<List<String>> listType = TypeFactory.getType("List<String>");
 *
 * // Serialize collection to string
 * List<String> names = N.asList("Alice", "Bob", "Charlie");
 * String json = listType.stringOf(names);
 * // Result: ["Alice","Bob","Charlie"]
 *
 * // Deserialize string to collection
 * String jsonInput = "[\"David\",\"Eve\",\"Frank\"]";
 * List<String> parsedNames = listType.valueOf(jsonInput);
 *
 * // Works with complex element types
 * Type<Set<Integer>> setType = TypeFactory.getType("Set<Integer>");
 * Set<Integer> numbers = new HashSet<>();
 * numbers.add(1);
 * numbers.add(2);
 * numbers.add(3);
 * String setJson = setType.stringOf(numbers);
 * }</pre>
 *
 * @param <E> the element type of the collection
 * @param <T> the collection type (must extend Collection&lt;E&gt;)
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

        String declaringNameValue;

        if (typeClass.isInterface()) {
            declaringNameValue = getTypeName(typeClass, parameterTypeName, true);
        } else {
            if (List.class.isAssignableFrom(typeClass)) {
                declaringNameValue = getTypeName(List.class, parameterTypeName, true);
            } else if (Set.class.isAssignableFrom(typeClass)) {
                declaringNameValue = getTypeName(Set.class, parameterTypeName, true);
            } else if (Queue.class.isAssignableFrom(typeClass)) {
                declaringNameValue = getTypeName(Queue.class, parameterTypeName, true);
            } else {
                declaringNameValue = getTypeName(Collection.class, parameterTypeName, true);

                final Class<?>[] interfaceClasses = typeClass.getInterfaces();

                for (final Class<?> interfaceClass : interfaceClasses) {
                    if (Collection.class.isAssignableFrom(interfaceClass) && !interfaceClass.equals(Collection.class)) {
                        declaringNameValue = getTypeName(interfaceClass, parameterTypeName, true);

                        break;
                    }
                }
            }
        }

        declaringName = declaringNameValue;

        this.typeClass = typeClass;
        parameterTypes = new Type[] { TypeFactory.getType(parameterTypeName) };
        elementType = parameterTypes[0];

        jdc = JDC.create().setElementType(elementType);

        isList = List.class.isAssignableFrom(this.typeClass);
        isSet = Set.class.isAssignableFrom(this.typeClass);
    }

    /**
     * Returns the declaring name of this collection type.
     * The declaring name represents the type in a simplified format suitable for type declarations,
     * using simple class names rather than fully qualified names.
     *
     * @return the declaring name of this type (e.g., "List&lt;String&gt;" instead of "java.util.List&lt;java.lang.String&gt;")
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the collection type handled by this type handler.
     *
     * @return the Class object for the collection type
     */
    @Override
    public Class<T> clazz() {
        return typeClass;
    }

    /**
     * Returns the type handler for the elements contained in this collection.
     *
     * @return the Type instance representing the element type of this collection
     */
    @Override
    public Type<E> getElementType() {
        return elementType;
    }

    /**
     * Returns an array containing the parameter types of this generic collection type.
     * For collection types, this array contains a single element representing the element type.
     *
     * @return an array containing the element type as the only parameter type
     */
    @Override
    public Type<E>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Checks whether this collection type represents a List or its subtype.
     *
     * @return {@code true} if this type represents a List, {@code false} otherwise
     */
    @Override
    public boolean isList() {
        return isList;
    }

    /**
     * Checks whether this collection type represents a Set or its subtype.
     *
     * @return {@code true} if this type represents a Set, {@code false} otherwise
     */
    @Override
    public boolean isSet() {
        return isSet;
    }

    /**
     * Always returns {@code true} as this type handler specifically handles Collection types.
     *
     * @return true
     */
    @Override
    public boolean isCollection() {
        return true;
    }

    /**
     * Always returns {@code true} as collection types are parameterized with an element type.
     *
     * @return true
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Checks whether the elements of this collection type are serializable.
     * The collection itself is considered serializable if its element type is serializable.
     *
     * @return {@code true} if the element type is serializable, {@code false} otherwise
     */
    @Override
    public boolean isSerializable() {
        return elementType.isSerializable();
    }

    /**
     * Returns the serialization type category for this collection.
     * If the element type is serializable, returns SERIALIZABLE; otherwise returns COLLECTION.
     *
     * @return SerializationType.SERIALIZABLE if elements are serializable, SerializationType.COLLECTION otherwise
     */
    @Override
    public SerializationType getSerializationType() {
        return isSerializable() ? SerializationType.SERIALIZABLE : SerializationType.COLLECTION;
    }

    /**
     * Converts a collection to its string representation.
     * If the collection is {@code null}, returns {@code null}.
     * If the collection is empty, returns "[]".
     * Otherwise, serializes the collection to a JSON array string where each element is serialized according to its type.
     *
     * @param x the collection to convert to string
     * @return the string representation of the collection, or {@code null} if the input is null
     */
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
                for (final E element : x) {
                    if (i++ > 0) {
                        bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }

                    if (element == null) {
                        bw.write(NULL_CHAR_ARRAY);
                    } else {
                        elementType.writeCharacter(bw, element, Utils.jsc);
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
     * Converts a string representation back to a collection instance.
     * Handles {@code null} and empty strings by returning {@code null} or an empty collection respectively.
     * The string should be in JSON array format.
     *
     * @param str the string to parse
     * @return a new collection instance containing the parsed elements, or {@code null} if the input is null
     */
    @Override
    public T valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return (T) N.newCollection(typeClass);
        } else {
            return Utils.jsonParser.deserialize(str, jdc, typeClass);
        }
    }

    /**
     * Appends the string representation of a collection to an Appendable.
     * The output format is a JSON array with elements separated by commas.
     * Handles Writer instances specially for better performance with buffering.
     *
     * @param appendable the Appendable to write to
     * @param x the collection to append
     * @throws IOException if an I/O error occurs during writing
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
                    for (final E element : x) {
                        if (i++ > 0) {
                            bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                        }

                        if (element == null) {
                            bw.write(NULL_CHAR_ARRAY);
                        } else {
                            elementType.appendTo(bw, element);
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
                for (final E element : x) {
                    if (i++ > 0) {
                        appendable.append(ELEMENT_SEPARATOR);
                    }

                    if (element == null) {
                        appendable.append(NULL_STRING);
                    } else {
                        elementType.appendTo(appendable, element);
                    }
                }

                appendable.append(WD._BRACKET_R);
            }
        }
    }

    /**
     * Writes the character representation of a collection to a CharacterWriter.
     * This method is optimized for performance when writing to character-based outputs.
     * The collection is serialized as a JSON array.
     *
     * @param writer the CharacterWriter to write to
     * @param x the collection to write
     * @param config the serialization configuration to use
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final T x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            try {
                writer.write(WD._BRACKET_L);

                int i = 0;
                for (final E element : x) {
                    if (i++ > 0) {
                        writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }

                    if (element == null) {
                        writer.write(NULL_CHAR_ARRAY);
                    } else {
                        elementType.writeCharacter(writer, element, config);
                    }
                }

                writer.write(WD._BRACKET_R);

            } catch (final IOException ioException) {
                throw new UncheckedIOException(ioException);
            }
        }
    }

    /**
     * Generates a type name string for a collection type with the specified element type.
     * The format depends on whether a declaring name (simplified) or full name is requested.
     * This is an internal method used by the type system.
     *
     * @param typeClass the collection class
     * @param parameterTypeName the name of the element type
     * @param isDeclaringName {@code true} to generate a declaring name with simple class names, {@code false} for fully qualified names
     * @return the formatted type name (e.g., "List&lt;String&gt;" or "java.util.List&lt;java.lang.String&gt;")
     */
    protected static String getTypeName(final Class<?> typeClass, final String parameterTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + WD.GREATER_THAN;
        }
    }
}
