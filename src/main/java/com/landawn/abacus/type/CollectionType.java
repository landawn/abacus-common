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
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link Collection} implementations, including {@link List}, {@link Set},
 * {@link Queue}, and their concrete subtypes.
 *
 * <p>Instances are created by the {@code TypeFactory} for parameterized collection type names such as
 * {@code "List<String>"} or {@code "Set<Integer>"}. The handler converts between collection objects and
 * their JSON array string representations, preserving generic element-type information for proper
 * serialization and deserialization.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * // Obtain a typed handler
 * Type<List<String>> listType = TypeFactory.getType("List<String>");
 *
 * // Serialize
 * List<String> names = N.asList("Alice", "Bob", "Charlie");
 * String json = listType.stringOf(names);  // ["Alice", "Bob", "Charlie"]
 *
 * // Deserialize
 * List<String> parsed = listType.valueOf("[\"Alice\", \"Bob\", \"Charlie\"]");
 * }</pre>
 *
 * @param <E> the element type of the collection
 * @param <T> the concrete collection type (must extend {@link Collection}{@code <E>})
 */
@SuppressWarnings("java:S2160")
public class CollectionType<E, T extends Collection<E>> extends AbstractType<T> {

    private final String declaringName;

    private final Class<T> typeClass;

    private final List<Type<?>> parameterTypes;

    private final Type<E> elementType;

    private final boolean isList;

    private final boolean isSet;

    private final JsonDeserConfig jdc;

    /**
     * Package-private constructor for {@code CollectionType}.
     * Instances are created by the {@code TypeFactory} using a collection class and its element type name.
     * The declaring name is resolved to the most specific collection interface implemented by
     * {@code typeClass} (e.g., {@code List}, {@code Set}, or {@code Queue}).
     *
     * @param typeClass         the concrete or interface collection class to handle
     * @param parameterTypeName the name of the element type (e.g., {@code "String"} or {@code "java.lang.Integer"})
     */
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
        elementType = TypeFactory.getType(parameterTypeName);
        parameterTypes = List.of(elementType);

        jdc = JsonDeserConfig.create().setElementType(elementType);

        isList = List.class.isAssignableFrom(this.typeClass);
        isSet = Set.class.isAssignableFrom(this.typeClass);
    }

    /**
     * Returns the declaring name of this collection type.
     * The declaring name represents the type in a simplified format suitable for type declarations,
     * using simple class names rather than fully qualified names.
     *
     * @return the declaring name of this type (e.g., {@code "List<String>"} instead of {@code "java.util.List<java.lang.String>"})
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
    public Class<T> javaType() {
        return typeClass;
    }

    /**
     * Returns the type handler for the elements contained in this collection.
     *
     * @return the Type instance representing the element type of this collection
     */
    @Override
    public Type<E> elementType() {
        return elementType;
    }

    /**
     * Returns an immutable list containing the parameter types of this generic collection type.
     * For collection types, this list contains a single element representing the element type.
     *
     * @return an immutable list containing the element type as the only parameter type
     */
    @Override
    public List<Type<?>> parameterTypes() {
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
     * @return {@code true} always
     */
    @Override
    public boolean isCollection() {
        return true;
    }

    /**
     * Always returns {@code true} as collection types are parameterized with an element type.
     *
     * @return {@code true} always
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Checks whether the element type of this collection is directly serializable by the type system.
     * If the element type is serializable, the collection as a whole can be written without delegating
     * to a full JSON serializer.
     *
     * @return {@code true} if the element type is serializable, {@code false} otherwise
     */
    @Override
    public boolean isSerializable() {
        return elementType.isSerializable();
    }

    /**
     * Returns the serialization type category for this collection.
     *
     * @return {@link SerializationType#SERIALIZABLE} if the element type is directly serializable,
     *         or {@link SerializationType#COLLECTION} otherwise
     */
    @Override
    public SerializationType serializationType() {
        return isSerializable() ? SerializationType.SERIALIZABLE : SerializationType.COLLECTION;
    }

    /**
     * Converts a collection to its JSON array string representation.
     * <ul>
     *   <li>{@code null} input returns {@code null}.</li>
     *   <li>An empty collection returns {@code "[]"}.</li>
     *   <li>Otherwise each element is serialized according to its element type and the results are
     *       joined in a JSON array.</li>
     * </ul>
     *
     * @param x the collection to serialize; may be {@code null}
     * @return the JSON array string, or {@code null} if {@code x} is {@code null}
     */
    @Override
    public String stringOf(final T x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.size() == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        if (this.isSerializable()) {
            final BufferedJsonWriter bw = Objectory.createBufferedJsonWriter();

            try {
                bw.write(SK._BRACKET_L);

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

                bw.write(SK._BRACKET_R);

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
     * Parses a JSON array string back into a collection instance.
     * <ul>
     *   <li>{@code null}, blank, or empty string returns {@code null}.</li>
     *   <li>{@code "[]"} returns an empty collection of the appropriate type.</li>
     *   <li>Otherwise the string is deserialized by the JSON parser with the configured element type.</li>
     * </ul>
     *
     * @param str the JSON array string to parse; may be {@code null}
     * @return a new collection containing the parsed elements, or {@code null} if {@code str} is {@code null} or blank
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
     * Appends the JSON array representation of a collection to an {@link Appendable}.
     * When the {@link Appendable} is a {@link java.io.Writer}, a buffered wrapper is used for
     * better I/O performance. If {@code x} is {@code null}, the literal {@code null} is appended.
     *
     * @param appendable the {@link Appendable} to write to
     * @param x          the collection to append; may be {@code null}
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
                    bw.write(SK._BRACKET_L);

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

                    bw.write(SK._BRACKET_R);

                    if (!isBufferedWriter) {
                        bw.flush();
                    }
                } finally {
                    if (!isBufferedWriter) {
                        Objectory.recycle((BufferedWriter) bw);
                    }
                }
            } else {
                appendable.append(SK._BRACKET_L);

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

                appendable.append(SK._BRACKET_R);
            }
        }
    }

    /**
     * Writes the JSON array representation of a collection to a {@link CharacterWriter}.
     * Each element is written using its own type's {@code writeCharacter} method, so element-level
     * quotation and escaping are applied correctly. If {@code x} is {@code null}, the literal
     * {@code null} is written.
     *
     * @param writer the {@link CharacterWriter} to write to
     * @param x      the collection to write; may be {@code null}
     * @param config serialization configuration forwarded to each element's writer; may be {@code null}
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final T x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(SK._BRACKET_L);

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

            writer.write(SK._BRACKET_R);
        }
    }

    /**
     * Generates the type name string for a collection type parameterized with the given element type.
     *
     * @param typeClass         the collection class (e.g., {@code List.class})
     * @param parameterTypeName the element type name (e.g., {@code "String"})
     * @param isDeclaringName   {@code true} to produce a simple (declaring) name using
     *                          {@link com.landawn.abacus.util.ClassUtil#getSimpleClassName(Class)}
     *                          (e.g., {@code "List<String>"}); {@code false} for the fully qualified
     *                          form (e.g., {@code "java.util.List<java.lang.String>"})
     * @return the formatted type name
     */
    protected static String getTypeName(final Class<?> typeClass, final String parameterTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + SK.GREATER_THAN;
        }
    }
}
