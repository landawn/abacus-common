/*
 * Copyright (C) 2025 HaiYang Li
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for Google Guava Multiset implementations.
 * This class provides serialization and deserialization capabilities for various Guava Multiset types
 * including HashMultiset, LinkedHashMultiset, and TreeMultiset.
 * Multisets are serialized as Map&lt;E, Integer&gt; structures where the value represents the count of each element.
 *
 * @param <E> the element type of the multiset
 * @param <T> the multiset type (must extend Multiset&lt;E&gt;)
 */
@SuppressWarnings("java:S2160")
public class GuavaMultisetType<E, T extends Multiset<E>> extends AbstractType<T> {

    private final String declaringName;

    private final Class<T> typeClass;

    private final Type<E>[] parameterTypes;

    private final Type<E> elementType;

    private final boolean isOrdered;

    private final JSONDeserializationConfig jdc;

    @SuppressWarnings("unchecked")
    GuavaMultisetType(final Class<T> typeClass, final String parameterTypeName) {
        super(getTypeName(typeClass, parameterTypeName, false));

        this.typeClass = typeClass;
        declaringName = getTypeName(typeClass, parameterTypeName, true);
        parameterTypes = new Type[] { TypeFactory.getType(parameterTypeName) };
        elementType = parameterTypes[0];
        isOrdered = declaringName.startsWith("Linked") || declaringName.startsWith("Sorted");

        jdc = JDC.create().setMapKeyType(elementType).setMapValueType(Integer.class).setElementType(elementType);
    }

    /**
     * Returns the declaring name of this multiset type.
     * The declaring name represents the type in a format suitable for type declarations,
     * using canonical class names with type parameters.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Multiset<String>> type = TypeFactory.getType("Multiset<String>");
     * String declaringName = type.declaringName();
     * // Returns: "com.google.common.collect.Multiset<java.lang.String>"
     * }</pre>
     *
     * @return the declaring name of this type (e.g., "com.google.common.collect.Multiset&lt;String&gt;")
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the multiset type handled by this type handler.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<HashMultiset<String>> type = TypeFactory.getType("HashMultiset<String>");
     * Class<?> clazz = type.clazz();
     * // Returns: HashMultiset.class
     * }</pre>
     *
     * @return the Class object for the multiset type
     */
    @Override
    public Class<T> clazz() {
        return typeClass;
    }

    /**
     * Returns the type handler for the elements contained in this multiset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Multiset<String>> type = TypeFactory.getType("Multiset<String>");
     * Type<String> elementType = type.getElementType();
     * // Returns: Type instance for String
     * }</pre>
     *
     * @return the Type instance representing the element type of this multiset
     */
    @Override
    public Type<E> getElementType() {
        return elementType;
    }

    /**
     * Returns an array containing the parameter types of this generic multiset type.
     * For multiset types, this array contains a single element representing the element type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Multiset<String>> type = TypeFactory.getType("Multiset<String>");
     * Type<?>[] paramTypes = type.getParameterTypes();
     * // Returns: [Type<String>]
     * }</pre>
     *
     * @return an array containing the element type as the only parameter type
     */
    @Override
    public Type<E>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type is a generic type with type parameters.
     * Multiset types are always generic types as they have an element type parameter.
     *
     * @return {@code true}, as Multiset is a generic type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Indicates whether instances of this type can be serialized.
     * Guava multisets are serializable through their Map representation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Multiset<String>> type = TypeFactory.getType("Multiset<String>");
     * boolean serializable = type.isSerializable();
     * // Returns: true
     * }</pre>
     *
     * @return {@code true}, as multisets can be serialized
     */
    @Override
    public boolean isSerializable() {
        return true;
    }

    /**
     * Converts a multiset to its string representation.
     * The multiset is serialized as a JSON object where each element maps to its count.
     * For ordered multisets (LinkedHashMultiset, TreeMultiset), the order is preserved using LinkedHashMap.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Multiset<String>> type = TypeFactory.getType("Multiset<String>");
     * Multiset<String> multiset = HashMultiset.create();
     * multiset.add("apple", 3);
     * multiset.add("banana", 2);
     * String result = type.stringOf(multiset);
     * // Returns: {"apple":3,"banana":2}
     *
     * result = type.stringOf(null);
     * // Returns: null
     * }</pre>
     *
     * @param x the multiset to convert to string
     * @return the JSON string representation of the multiset as a map of elements to counts, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final T x) {
        if (x == null) {
            return null;
        }

        final Map<E, Integer> map = isOrdered ? N.newLinkedHashMap(x.size()) : N.newHashMap(x.size());

        for (final E e : x.elementSet()) {
            map.put(e, x.count(e));
        }

        return Utils.jsonParser.serialize(map, Utils.jsc);
    }

    /**
     * Converts a string representation back to a multiset instance.
     * The string should be in JSON format representing a Map&lt;E, Integer&gt; where values are element counts.
     * Creates the appropriate multiset implementation based on the type class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Multiset<String>> type = TypeFactory.getType("Multiset<String>");
     * Multiset<String> result = type.valueOf("{\"apple\":3,\"banana\":2}");
     * // Returns: Multiset with apple (count=3), banana (count=2)
     *
     * result = type.valueOf(null);
     * // Returns: null
     *
     * result = type.valueOf("");
     * // Returns: null
     * }</pre>
     *
     * @param str the JSON string to parse
     * @return a new multiset instance containing the parsed elements with their counts, or {@code null} if the input is {@code null} or empty
     */
    @Override
    public T valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final Map<E, Integer> map = Utils.jsonParser.deserialize(str, jdc, Map.class);

        if (map == null) {
            return null;
        }

        final T multiset = newInstance(map.size());

        for (final Map.Entry<E, Integer> entry : map.entrySet()) {
            multiset.add(entry.getKey(), entry.getValue());
        }

        return multiset;
    }

    /**
     * Generates a type name string for a multiset type with the specified element type.
     * The format depends on whether a declaring name or full name is requested.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String typeName = GuavaMultisetType.getTypeName(
     *     Multiset.class, "String", false);
     * // Returns: "com.google.common.collect.Multiset<String>"
     *
     * String declaringName = GuavaMultisetType.getTypeName(
     *     Multiset.class, "String", true);
     * // Returns: "com.google.common.collect.Multiset<java.lang.String>"
     * }</pre>
     *
     * @param typeClass the multiset class
     * @param parameterTypeName the name of the element type
     * @param isDeclaringName {@code true} to generate a declaring name, {@code false} for the full name
     * @return the formatted type name (e.g., "Multiset&lt;String&gt;")
     */
    protected static String getTypeName(final Class<?> typeClass, final String parameterTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + WD.GREATER_THAN;
        }
    }

    /**
     * Creates a new instance of the appropriate multiset implementation.
     * Selects the concrete implementation based on the type class:
     * - HashMultiset for unordered multisets and abstract types
     * - LinkedHashMultiset for ordered multisets
     * - TreeMultiset for sorted multisets
     * Falls back to reflection-based instantiation for custom implementations.
     *
     * @param size the expected number of distinct elements
     * @return a new multiset instance
     * @throws IllegalArgumentException if no suitable constructor or factory method is found
     */
    protected T newInstance(int size) {
        if (HashMultiset.class.isAssignableFrom(typeClass) || Modifier.isAbstract(typeClass.getModifiers())) {
            return (T) HashMultiset.create(size);
        } else if (LinkedHashMultiset.class.isAssignableFrom(typeClass)) {
            return (T) LinkedHashMultiset.create(size);
        } else if (TreeMultiset.class.isAssignableFrom(typeClass)) {
            return (T) TreeMultiset.create();
        } else {
            Constructor<T> constructor = ClassUtil.getDeclaredConstructor(typeClass);

            if (constructor != null) {
                return ClassUtil.invokeConstructor(constructor);
            } else {
                constructor = ClassUtil.getDeclaredConstructor(typeClass, int.class);

                if (constructor != null) {
                    return ClassUtil.invokeConstructor(constructor, size);
                } else {
                    Method method = ClassUtil.getDeclaredMethod(typeClass, "create");

                    if (method != null && Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())
                            && typeClass.isAssignableFrom(method.getReturnType())) {
                        return ClassUtil.invokeMethod(method);
                    } else {
                        method = ClassUtil.getDeclaredMethod(typeClass, "create", int.class);

                        if (method != null && Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())
                                && typeClass.isAssignableFrom(method.getReturnType())) {
                            return ClassUtil.invokeMethod(method, size);
                        }
                    }
                }
            }

            throw new IllegalArgumentException("Unsupported Multiset type: " + typeClass.getName() + ". No constructor or static factory method found.");
        }
    }
}
