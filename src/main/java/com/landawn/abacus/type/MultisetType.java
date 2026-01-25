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

import java.util.Map;

import com.landawn.abacus.parser.JsonDeserializationConfig;
import com.landawn.abacus.parser.JsonDeserializationConfig.JDC;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for Multiset objects with a generic element type.
 * A Multiset is a collection that allows duplicate elements and keeps track of their count.
 * This class handles serialization and deserialization of Multiset instances.
 *
 * @param <E> the element type
 */
@SuppressWarnings("java:S2160")
public class MultisetType<E> extends AbstractType<Multiset<E>> {

    private final String declaringName;

    private static final Class<?> typeClass = Multiset.class;

    private final Type<E>[] parameterTypes;

    private final Type<E> elementType;

    private final JsonDeserializationConfig jdc;

    @SuppressWarnings("unchecked")
    MultisetType(final String parameterTypeName) {
        super(getTypeName(typeClass, parameterTypeName, false));

        declaringName = getTypeName(typeClass, parameterTypeName, true);
        parameterTypes = new Type[] { TypeFactory.getType(parameterTypeName) };
        elementType = parameterTypes[0];

        jdc = JDC.create().setMapKeyType(elementType).setMapValueType(Integer.class);
    }

    /**
     * Returns the declaring name of this Multiset type.
     * The declaring name includes the fully qualified class name of the element type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Multiset<String>> type = TypeFactory.getType("Multiset<String>");
     * String name = type.declaringName();
     * // Returns: "Multiset<String>"
     * }</pre>
     *
     * @return The declaring name in format "Multiset&lt;ElementDeclaringName&gt;"
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the Multiset type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Multiset<String>> type = TypeFactory.getType("Multiset<String>");
     * Class<Multiset<String>> clazz = type.clazz();
     * // Returns: Multiset.class
     * }</pre>
     *
     * @return The Class object for Multiset
     */
    @Override
    public Class<Multiset<E>> clazz() {
        return (Class<Multiset<E>>) typeClass;
    }

    /**
     * Returns the type handler for the elements contained in this multiset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Multiset<String>> type = TypeFactory.getType("Multiset<String>");
     * Type<String> elemType = type.getElementType();
     * // Returns: StringType instance
     * }</pre>
     *
     * @return the Type instance representing the element type of this multiset
     */
    @Override
    public Type<E> getElementType() {
        return elementType;
    }

    /**
     * Gets the parameter types for this generic Multiset type.
     * The array contains a single element: the element type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Multiset<String>> type = TypeFactory.getType("Multiset<String>");
     * Type<?>[] paramTypes = type.getParameterTypes();
     * // Returns: [StringType]
     * // paramTypes[0] is the element type (String)
     * }</pre>
     *
     * @return An array containing the element type
     */
    @Override
    public Type<E>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this is a generic type.
     * For MultisetType, this always returns {@code true} since Multiset is parameterized with an element type.
     *
     * @return {@code true}, indicating that Multiset is a generic type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Indicates whether instances of this type can be serialized.
     * Multiset objects can be serialized through this type handler.
     *
     * @return {@code true}, indicating that Multiset is serializable through this type
     */
    @Override
    public boolean isSerializable() {
        return true;
    }

    /**
     * Converts a Multiset object to its JSON string representation.
     * The Multiset is first converted to a Map where each element maps to its count,
     * then serialized as JSON.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Multiset<String>> type = TypeFactory.getType("Multiset<String>");
     * Multiset<String> multiset = N.newMultiset();
     * multiset.add("apple", 3);
     * multiset.add("orange", 2);
     * multiset.add("banana", 1);
     *
     * String json = type.stringOf(multiset);
     * // Returns: {"apple":3,"orange":2,"banana":1}
     *
     * json = type.stringOf(null);
     * // Returns: null
     * }</pre>
     *
     * @param x The Multiset object to convert
     * @return The JSON string representation of the Multiset as a map of element to count, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final Multiset<E> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x.toMap(), Utils.jsc);
    }

    /**
     * Parses a JSON string to create a Multiset object.
     * The string should represent a JSON object where each key is an element and the value is its count.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Multiset<String>> type = TypeFactory.getType("Multiset<String>");
     *
     * Multiset<String> multiset = type.valueOf("{\"apple\":3,\"orange\":2,\"banana\":1}");
     * // Returns: Multiset with "apple" having count 3, "orange" count 2, "banana" count 1
     * // multiset.count("apple") returns 3
     *
     * multiset = type.valueOf(null);
     * // Returns: null
     *
     * multiset = type.valueOf("{}");
     * // Returns: empty Multiset
     * }</pre>
     *
     * @param str The JSON string to parse
     * @return The parsed Multiset object, or {@code null} if the input is {@code null} or empty
     */
    @Override
    public Multiset<E> valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        }

        final Map<E, Integer> map = Utils.jsonParser.deserialize(str, jdc, Map.class);

        if (map == null) {
            return null; // NOSONAR
        }

        final Multiset<E> multiset = N.newMultiset(map.size());

        for (final Map.Entry<E, Integer> entry : map.entrySet()) {
            multiset.add(entry.getKey(), entry.getValue());
        }

        return multiset;
    }

    /**
     * Generates the type name for a Multiset with the specified element type.
     * This is an internal method used by the type system.
     *
     * @param typeClass The Multiset class
     * @param parameterTypeName The name of the element type
     * @param isDeclaringName Whether to use declaring names (true) or regular names (false)
     * @return The formatted type name string
     */
    @SuppressWarnings("hiding")
    protected static String getTypeName(final Class<?> typeClass, final String parameterTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + WD.GREATER_THAN;

        }
    }
}
