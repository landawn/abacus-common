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
import java.util.Set;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.WD;

/**
 * Type handler for ImmutableSet objects.
 * This class provides serialization and deserialization capabilities for ImmutableSet instances,
 * delegating most operations to the underlying Set type handler while ensuring immutability.
 *
 * @param <E> the element type of the immutable set
 */
@SuppressWarnings("java:S2160")
public class ImmutableSetType<E> extends AbstractType<ImmutableSet<E>> {

    private final String declaringName;

    private final Class<ImmutableSet<E>> typeClass;

    private final Type<E>[] parameterTypes;

    private final Type<E> elementType;

    private final Type<Set<E>> setType;

    @SuppressWarnings("rawtypes")
    ImmutableSetType(final String parameterTypeName) {
        super(getTypeName(ImmutableSet.class, parameterTypeName, false));

        typeClass = (Class) ImmutableSet.class;
        declaringName = getTypeName(ImmutableSet.class, parameterTypeName, true);
        parameterTypes = new Type[] { TypeFactory.getType(parameterTypeName) };
        elementType = parameterTypes[0];
        setType = TypeFactory.getType("Set<" + parameterTypeName + ">");
    }

    /**
     * Returns the declaring name of this immutable set type.
     * The declaring name represents the type in a simplified format suitable for type declarations,
     * using simple class names rather than fully qualified names.
     *
     * @return the declaring name of this type (e.g., "ImmutableSet&lt;String&gt;")
     @MayReturnNull
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the ImmutableSet type handled by this type handler.
     *
     * @return the Class object for ImmutableSet
     */
    @Override
    public Class<ImmutableSet<E>> clazz() {
        return typeClass;
    }

    /**
     * Returns the type handler for the elements contained in this immutable set.
     *
     * @return the Type instance representing the element type of this immutable set
     */
    @Override
    public Type<E> getElementType() {
        return elementType;
    }

    /**
     * Returns an array containing the parameter types of this generic immutable set type.
     * For immutable set types, this array contains a single element representing the element type.
     *
     * @return an array containing the element type as the only parameter type
     */
    @Override
    public Type<E>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type represents a Set or its subtype.
     * ImmutableSet is considered a set type.
     *
     * @return {@code true}, as ImmutableSet is a set type
     */
    @Override
    public boolean isSet() {
        return true;
    }

    /**
     * Indicates whether this type represents a Collection or its subtype.
     * ImmutableSet is a collection type.
     *
     * @return {@code true}, as ImmutableSet is a collection type
     */
    @Override
    public boolean isCollection() {
        return true;
    }

    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Checks whether the elements of this immutable set type are serializable.
     * The immutable set is considered serializable if its element type is serializable.
     *
     * @return {@code true} if the underlying set type is serializable, {@code false} otherwise
     */
    @Override
    public boolean isSerializable() {
        return setType.isSerializable();
    }

    /**
     * Returns the serialization type category for this immutable set.
     * Delegates to the underlying set type for serialization categorization.
     *
     * @return SerializationType.SERIALIZABLE if elements are serializable, SerializationType.COLLECTION otherwise
     @MayReturnNull
     */
    @Override
    public SerializationType getSerializationType() {
        return isSerializable() ? SerializationType.SERIALIZABLE : SerializationType.COLLECTION;
    }

    /**
     * Converts an immutable set to its string representation.
     * Delegates the serialization to the underlying set type handler.
     *
     * @param x the immutable set to convert to string
     * @return the string representation of the immutable set, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final ImmutableSet<E> x) {
        return setType.stringOf(x);
    }

    /**
     * Converts a string representation back to an immutable set instance.
     * First deserializes to a mutable set using the set type handler,
     * then wraps it in an ImmutableSet to ensure immutability.
     *
     * @param str the string to parse
     * @return a new immutable set instance containing the parsed elements
     */
    @Override
    public ImmutableSet<E> valueOf(final String str) {
        return ImmutableSet.wrap(setType.valueOf(str));
    }

    /**
     * Appends the string representation of an immutable set to an Appendable.
     * Delegates to the underlying set type handler for the actual appending logic.
     *
     * @param writer the Appendable to write to
     * @param x the immutable set to append
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void appendTo(final Appendable writer, final ImmutableSet<E> x) throws IOException {
        setType.appendTo(writer, x);
    }

    /**
     * Writes the character representation of an immutable set to a CharacterWriter.
     * Delegates to the underlying set type handler for the actual writing logic.
     *
     * @param writer the CharacterWriter to write to
     * @param x the immutable set to write
     * @param config the serialization configuration to use
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final ImmutableSet<E> x, final JSONXMLSerializationConfig<?> config) throws IOException {
        setType.writeCharacter(writer, x, config);
    }

    /**
     * Generates a type name string for an ImmutableSet type with the specified element type.
     * The format depends on whether a declaring name (simplified) or full name is requested.
     *
     * @param typeClass the ImmutableSet class
     * @param parameterTypeName the name of the element type
     * @param isDeclaringName {@code true} to generate a declaring name with simple class names, {@code false} for fully qualified names
     * @return the formatted type name (e.g., "ImmutableSet&lt;String&gt;" or "com.landawn.abacus.util.ImmutableSet&lt;java.lang.String&gt;")
     */
    protected static String getTypeName(final Class<?> typeClass, final String parameterTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + WD.GREATER_THAN;
        }
    }
}
