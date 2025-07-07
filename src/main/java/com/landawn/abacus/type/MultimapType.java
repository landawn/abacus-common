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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SetMultimap;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for Multimap objects with generic key, element, and collection value types.
 * A Multimap is a map where each key can be associated with multiple values stored in a Collection.
 * This class handles serialization and deserialization of Multimap instances.
 *
 * @param <K> the key type
 * @param <E> the element type (individual values in the collection)
 * @param <V> the value collection type (e.g., List<E> or Set<E>)
 * @param <T> the specific Multimap implementation type
 */
@SuppressWarnings("java:S2160")
public class MultimapType<K, E, V extends Collection<E>, T extends Multimap<K, E, V>> extends AbstractType<T> {

    private final Class<?> typeClass;

    private final String declaringName;

    private final Type<?>[] parameterTypes;

    protected final JSONDeserializationConfig jdc;

    MultimapType(final Class<?> typeClass, final String keyTypeName, final String valueElementTypeName, final String valueTypeName) {
        super(getTypeName(typeClass, keyTypeName, valueElementTypeName, valueTypeName, false));

        parameterTypes = Strings.isEmpty(valueElementTypeName) ? new Type[] { TypeFactory.getType(keyTypeName), TypeFactory.getType(valueTypeName) }
                : (Strings.isEmpty(valueTypeName) ? new Type[] { TypeFactory.getType(keyTypeName), TypeFactory.getType(valueElementTypeName) }
                        : new Type[] { TypeFactory.getType(keyTypeName), TypeFactory.getType(valueElementTypeName), TypeFactory.getType(valueTypeName) });

        this.typeClass = typeClass;

        declaringName = getTypeName(typeClass, keyTypeName, valueElementTypeName, valueTypeName, true);

        jdc = JDC.create().setMapKeyType(parameterTypes[0]);

        if (Strings.isEmpty(valueElementTypeName)) {
            if (!(parameterTypes[1] instanceof CollectionType)) {
                throw new IllegalArgumentException("The value type of Multimap must be a collection type: " + valueTypeName);
            }

            jdc.setMapValueType(parameterTypes[1]);
            jdc.setElementType(parameterTypes[1].getElementType());
        } else if (Strings.isEmpty(valueTypeName)) {
            if (ListMultimap.class.isAssignableFrom(typeClass)) {
                jdc.setMapValueType(TypeFactory.getType("List<" + parameterTypes[1].name() + ">"));
            } else if (SetMultimap.class.isAssignableFrom(typeClass)) {
                jdc.setMapValueType(TypeFactory.getType("Set<" + parameterTypes[1].name() + ">"));
            } else {
                throw new IllegalArgumentException("Unsupported Multimap type: " + typeClass);
            }

            jdc.setElementType(parameterTypes[1]);
        } else {
            if (!(parameterTypes[2] instanceof CollectionType)) {
                throw new IllegalArgumentException("The value type of Multimap must be a collection type: " + valueTypeName);
            }

            jdc.setMapValueType(parameterTypes[2]);
            jdc.setElementType(parameterTypes[1]);
        }
    }

    /**
     * Returns the declaring name of this Multimap type.
     * The declaring name includes the fully qualified class names of the key, element, and value types.
     *
     * @return The declaring name in format "MultimapClass<KeyDeclaringName[, ElementDeclaringName][, ValueDeclaringName]>"
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the specific Multimap implementation type.
     *
     * @return The Class object for the Multimap implementation
     */
    @SuppressWarnings({ "rawtypes" })
    @Override
    public Class<T> clazz() {
        return (Class) typeClass;
    }

    /**
     * Gets the parameter types for this generic Multimap type.
     * The array contains:
     * - Two elements if either valueElementTypeName or valueTypeName was empty: [keyType, valueType/elementType]
     * - Three elements if both were provided: [keyType, elementType, valueType]
     *
     * @return An array containing the parameter types
     */
    @Override
    public Type<?>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this is a generic type.
     * For MultimapType, this always returns true since Multimap is parameterized with key and value types.
     *
     * @return true, indicating that Multimap is a generic type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Indicates whether instances of this type can be serialized.
     * Multimap objects can be serialized through this type handler.
     *
     * @return true, indicating that Multimap is serializable through this type
     */
    @Override
    public boolean isSerializable() {
        return true;
    }

    /**
     * Converts a Multimap object to its JSON string representation.
     * The Multimap is first converted to a Map where each key maps to a Collection of values,
     * then serialized as JSON.
     *
     * @param x The Multimap object to convert
     * @return The JSON string representation of the Multimap, or null if the input is null
     */
    @Override
    public String stringOf(final T x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x.toMap(), Utils.jsc);
    }

    /**
     * Parses a JSON string to create a Multimap object.
     * The string should represent a JSON object where each key maps to an array of values.
     * The specific Multimap implementation (LinkedSetMultimap or LinkedListMultimap) is chosen
     * based on whether the value type is a Set or not.
     *
     * @param str The JSON string to parse
     * @return The parsed Multimap object, or null if the input is null or empty
     */
    @MayReturnNull
    @Override
    public T valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        }

        final Map<K, Collection<E>> map = Utils.jsonParser.deserialize(str, jdc, Map.class);

        if (Set.class.isAssignableFrom(parameterTypes[2].clazz())) {
            final Multimap<K, E, V> multiMap = (Multimap<K, E, V>) N.newLinkedSetMultimap(map.size());

            for (final Map.Entry<K, Collection<E>> entry : map.entrySet()) {
                multiMap.putMany(entry.getKey(), entry.getValue());
            }

            return (T) multiMap;
        } else {
            final Multimap<K, E, V> multimap = (Multimap<K, E, V>) N.newLinkedListMultimap(map.size());

            for (final Map.Entry<K, Collection<E>> entry : map.entrySet()) {
                multimap.putMany(entry.getKey(), entry.getValue());
            }

            return (T) multimap;
        }
    }

    /**
     * Generates the type name for a Multimap with the specified implementation class, key, element and value types.
     *
     * @param typeClass The Multimap implementation class
     * @param keyTypeName The name of the key type
     * @param valueElementTypeName The name of the element type (can be empty)
     * @param valueTypeName The name of the value collection type (can be empty)
     * @param isDeclaringName Whether to use declaring names (true) or regular names (false)
     * @return The formatted type name string
     */
    protected static String getTypeName(final Class<?> typeClass, final String keyTypeName, final String valueElementTypeName, final String valueTypeName,
            final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(keyTypeName).declaringName()
                    + (Strings.isEmpty(valueElementTypeName) ? "" : (WD.COMMA_SPACE + TypeFactory.getType(valueElementTypeName).declaringName()))
                    + (Strings.isEmpty(valueTypeName) ? "" : (WD.COMMA_SPACE + TypeFactory.getType(valueTypeName).declaringName())) + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(keyTypeName).name()
                    + (Strings.isEmpty(valueElementTypeName) ? "" : (WD.COMMA_SPACE + TypeFactory.getType(valueElementTypeName).name()))
                    + (Strings.isEmpty(valueTypeName) ? "" : (WD.COMMA_SPACE + TypeFactory.getType(valueTypeName).name())) + WD.GREATER_THAN;

        }
    }
}