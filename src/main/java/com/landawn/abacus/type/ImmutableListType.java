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
import java.util.List;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.SK;

/**
 * Type handler for ImmutableList objects.
 * This class provides serialization and deserialization capabilities for ImmutableList instances,
 * delegating most operations to the underlying List type handler while ensuring immutability.
 *
 * @param <E> the element type of the immutable list
 */
@SuppressWarnings("java:S2160")
public class ImmutableListType<E> extends AbstractType<ImmutableList<E>> {

    private final String declaringName;

    private final Class<ImmutableList<E>> typeClass;

    private final List<Type<?>> parameterTypes;

    private final Type<E> elementType;

    private final Type<List<E>> listType;

    /**
     * Package-private constructor for ImmutableListType.
     * This constructor is called by the TypeFactory to create ImmutableList&lt;E&gt; type instances.
     *
     * @param parameterTypeName the name of the element type parameter
     */
    @SuppressWarnings("rawtypes")
    ImmutableListType(final String parameterTypeName) {
        super(getTypeName(ImmutableList.class, parameterTypeName, false));

        typeClass = (Class) ImmutableList.class;
        declaringName = getTypeName(ImmutableList.class, parameterTypeName, true);
        elementType = TypeFactory.getType(parameterTypeName);
        parameterTypes = List.of(elementType);
        listType = TypeFactory.getType("List<" + parameterTypeName + ">");
    }

    /**
     * Returns the declaring name of this immutable list type.
     * The declaring name represents the type in a simplified format suitable for type declarations,
     * using simple class names rather than fully qualified names.
     *
     * @return the declaring name of this type (e.g., "ImmutableList&lt;String&gt;")
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the ImmutableList type handled by this type handler.
     *
     * @return the Class object for ImmutableList
     */
    @Override
    public Class<ImmutableList<E>> javaType() {
        return typeClass;
    }

    /**
     * Returns the type handler for the elements contained in this immutable list.
     *
     * @return the Type instance representing the element type of this immutable list
     */
    @Override
    public Type<E> elementType() {
        return elementType;
    }

    /**
     * Returns an immutable list containing the parameter types of this generic immutable list type.
     * For immutable list types, this list contains a single element representing the element type.
     *
     * @return an immutable list containing the element type as the only parameter type
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type represents a List or its subtype.
     * ImmutableList is considered a list type.
     *
     * @return {@code true}, as ImmutableList is a list type
     */
    @Override
    public boolean isList() {
        return true;
    }

    /**
     * Indicates whether this type represents a Collection or its subtype.
     * ImmutableList is a collection type.
     *
     * @return {@code true}, as ImmutableList is a collection type
     */
    @Override
    public boolean isCollection() {
        return true;
    }

    /**
     * Indicates whether this type is a generic type with type parameters.
     * ImmutableList types are always parameterized with an element type.
     *
     * @return {@code true}, as ImmutableList is a generic type
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Checks whether the elements of this immutable list type are serializable.
     * The immutable list is considered serializable if its element type is serializable.
     *
     * @return {@code true} if the underlying list type is serializable, {@code false} otherwise
     */
    @Override
    public boolean isSerializable() {
        return listType.isSerializable();
    }

    /**
     * Returns the serialization type category for this immutable list.
     * Delegates to the underlying list type for serialization categorization.
     *
     * @return SerializationType.SERIALIZABLE if elements are serializable, SerializationType.COLLECTION otherwise
     */
    @Override
    public SerializationType serializationType() {
        return isSerializable() ? SerializationType.SERIALIZABLE : SerializationType.COLLECTION;
    }

    /**
     * Serializes an {@link ImmutableList} to its string representation by delegating to the
     * underlying list type handler.
     *
     * @param x the immutable list to serialize; may be {@code null}
     * @return the string representation, or {@code null} if {@code x} is {@code null}
     */
    @Override
    public String stringOf(final ImmutableList<E> x) {
        return listType.stringOf(x);
    }

    /**
     * Deserializes a string into an {@link ImmutableList} instance.
     * First deserializes to a mutable list via the underlying list type handler,
     * then wraps it in an {@link ImmutableList}.
     *
     * @param str the string to parse; may be {@code null}
     * @return a new {@link ImmutableList} containing the parsed elements, or {@code null} if the input is {@code null}
     */
    @Override
    public ImmutableList<E> valueOf(final String str) {
        final List<E> list = listType.valueOf(str);

        return list == null ? null : ImmutableList.wrap(list);
    }

    /**
     * Appends the string representation of an {@link ImmutableList} to an {@link Appendable},
     * delegating to the underlying list type handler.
     *
     * @param writer the {@link Appendable} to write to
     * @param x the immutable list to append; may be {@code null}
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void appendTo(final Appendable writer, final ImmutableList<E> x) throws IOException {
        listType.appendTo(writer, x);
    }

    /**
     * Writes the character representation of an {@link ImmutableList} to a {@link CharacterWriter},
     * delegating to the underlying list type handler.
     *
     * @param writer the {@link CharacterWriter} to write to
     * @param x the immutable list to write; may be {@code null}
     * @param config the serialization configuration to use; may be {@code null}
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final ImmutableList<E> x, final JsonXmlSerConfig<?> config) throws IOException {
        listType.writeCharacter(writer, x, config);
    }

    /**
     * Generates a type name string for an ImmutableList type with the specified element type.
     * The format depends on whether a declaring name (simplified) or full name is requested.
     *
     * @param typeClass the ImmutableList class
     * @param parameterTypeName the name of the element type
     * @param isDeclaringName {@code true} to generate a declaring name with simple class names, {@code false} for fully qualified names
     * @return the formatted type name (e.g., "ImmutableList&lt;String&gt;" or "com.landawn.abacus.util.ImmutableList&lt;java.lang.String&gt;")
     */
    protected static String getTypeName(final Class<?> typeClass, final String parameterTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + SK.GREATER_THAN;
        }
    }
}
