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

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.WD;

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

    private final Type<E>[] parameterTypes;

    private final Type<E> elementType;

    private final Type<List<E>> listType;

    @SuppressWarnings("rawtypes")
    ImmutableListType(final String parameterTypeName) {
        super(getTypeName(ImmutableList.class, parameterTypeName, false));

        typeClass = (Class) ImmutableList.class;
        declaringName = getTypeName(ImmutableList.class, parameterTypeName, true);
        parameterTypes = new Type[] { TypeFactory.getType(parameterTypeName) };
        elementType = parameterTypes[0];
        listType = TypeFactory.getType("List<" + parameterTypeName + ">");
    }

    /**
     * Returns the declaring name of this immutable list type.
     * The declaring name represents the type in a simplified format suitable for type declarations,
     * using simple class names rather than fully qualified names.
     *
     * @return the declaring name of this type (e.g., "ImmutableList<String>")
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
    public Class<ImmutableList<E>> clazz() {
        return typeClass;
    }

    /**
     * Returns the type handler for the elements contained in this immutable list.
     *
     * @return the Type instance representing the element type of this immutable list
     */
    @Override
    public Type<E> getElementType() {
        return elementType;
    }

    /**
     * Returns an array containing the parameter types of this generic immutable list type.
     * For immutable list types, this array contains a single element representing the element type.
     *
     * @return an array containing the element type as the only parameter type
     */
    @Override
    public Type<E>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type represents a List or its subtype.
     * ImmutableList is considered a list type.
     *
     * @return true, as ImmutableList is a list type
     */
    @Override
    public boolean isList() {
        return true;
    }

    /**
     * Indicates whether this type represents a Collection or its subtype.
     * ImmutableList is a collection type.
     *
     * @return true, as ImmutableList is a collection type
     */
    @Override
    public boolean isCollection() {
        return true;
    }

    /**
     * Indicates that this is a generic type with type parameters.
     *
     * @return true as ImmutableList is a generic type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Checks whether the elements of this immutable list type are serializable.
     * The immutable list is considered serializable if its element type is serializable.
     *
     * @return true if the underlying list type is serializable, false otherwise
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
    public SerializationType getSerializationType() {
        return isSerializable() ? SerializationType.SERIALIZABLE : SerializationType.COLLECTION;
    }

    /**
     * Converts an immutable list to its string representation.
     * Delegates the serialization to the underlying list type handler.
     *
     * @param x the immutable list to convert to string
     * @return the string representation of the immutable list, or null if the input is null
     */
    @Override
    public String stringOf(final ImmutableList<E> x) {
        return listType.stringOf(x);
    }

    /**
     * Converts a string representation back to an immutable list instance.
     * First deserializes to a mutable list using the list type handler,
     * then wraps it in an ImmutableList to ensure immutability.
     *
     * @param str the string to parse
     * @return a new immutable list instance containing the parsed elements
     */
    @Override
    public ImmutableList<E> valueOf(final String str) {
        return ImmutableList.wrap(listType.valueOf(str));
    }

    /**
     * Appends the string representation of an immutable list to an Appendable.
     * Delegates to the underlying list type handler for the actual appending logic.
     *
     * @param writer the Appendable to write to
     * @param x the immutable list to append
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void appendTo(final Appendable writer, final ImmutableList<E> x) throws IOException {
        listType.appendTo(writer, x);
    }

    /**
     * Writes the character representation of an immutable list to a CharacterWriter.
     * Delegates to the underlying list type handler for the actual writing logic.
     *
     * @param writer the CharacterWriter to write to
     * @param x the immutable list to write
     * @param config the serialization configuration to use
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final ImmutableList<E> x, final JSONXMLSerializationConfig<?> config) throws IOException {
        listType.writeCharacter(writer, x, config);
    }

    /**
     * Generates a type name string for an ImmutableList type with the specified element type.
     * The format depends on whether a declaring name (simplified) or full name is requested.
     *
     * @param typeClass the ImmutableList class
     * @param parameterTypeName the name of the element type
     * @param isDeclaringName true to generate a declaring name with simple class names, false for fully qualified names
     * @return the formatted type name (e.g., "ImmutableList<String>" or "com.landawn.abacus.util.ImmutableList<java.lang.String>")
     */
    protected static String getTypeName(final Class<?> typeClass, final String parameterTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + WD.GREATER_THAN;
        }
    }
}