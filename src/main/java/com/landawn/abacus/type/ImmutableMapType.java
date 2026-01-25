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
import java.io.Writer;

import com.landawn.abacus.parser.JsonDeserializationConfig;
import com.landawn.abacus.parser.JsonDeserializationConfig.JDC;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for ImmutableMap objects with generic key and value types.
 * This class handles serialization and deserialization of ImmutableMap instances.
 * Special support is provided for Spring's MultiValueMap where values are Lists.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @param <T> the specific ImmutableMap implementation type
 */
@SuppressWarnings("java:S2160")
public class ImmutableMapType<K, V, T extends ImmutableMap<K, V>> extends AbstractType<T> {

    private final String declaringName;

    private final Class<T> typeClass;

    private final Type<?>[] parameterTypes;

    private final JsonDeserializationConfig jdc;

    @SuppressWarnings("rawtypes")
    ImmutableMapType(final String keyTypeName, final String valueTypeName) {
        super(getTypeName(ImmutableMap.class, keyTypeName, valueTypeName, false));

        declaringName = getTypeName(ImmutableMap.class, keyTypeName, valueTypeName, true);

        this.typeClass = (Class) ImmutableMap.class;

        parameterTypes = new Type[] { TypeFactory.getType(keyTypeName), TypeFactory.getType(valueTypeName) };

        jdc = JDC.create().setMapKeyType(parameterTypes[0]).setMapValueType(parameterTypes[1]);
    }

    /**
     * Returns the declaring name of this ImmutableMap type.
     * The declaring name includes the fully qualified class names of the key and value types.
     *
     * @return The declaring name in format "MapClass&lt;KeyDeclaringName, ValueDeclaringName&gt;"
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the specific ImmutableMap implementation type.
     *
     * @return The Class object for the ImmutableMap implementation
     */
    @Override
    public Class<T> clazz() {
        return typeClass;
    }

    /**
     * Gets the parameter types for this generic ImmutableMap type.
     * The array contains two elements: the key type at index 0 and the value type at index 1.
     * For Spring MultiValueMap, the value type will be List&lt;V&gt; instead of V.
     *
     * @return An array containing the key type and value type
     */
    @Override
    public Type<?>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type represents a ImmutableMap.
     * For ImmutableMapType, this always returns {@code true}.
     *
     * @return {@code true}, indicating that this type represents a ImmutableMap
     */
    @Override
    public boolean isMap() {
        return true;
    }

    /**
     * Indicates whether this is a generic type.
     * For ImmutableMapType, this always returns {@code true} since ImmutableMap is parameterized with key and value types.
     *
     * @return {@code true}, indicating that ImmutableMap is a generic type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Indicates whether instances of this type can be serialized.
     * ImmutableMap objects are not directly serializable through this type handler.
     *
     * @return {@code false}, indicating that ImmutableMap is not serializable through this type
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Gets the serialization type category for ImmutableMap.
     * This indicates how the ImmutableMap should be treated during serialization processes.
     *
     * @return SerializationType.MAP
     */
    @Override
    public SerializationType getSerializationType() {
        return SerializationType.MAP;
    }

    /**
     * Converts a ImmutableMap object to its JSON string representation.
     * Empty ImmutableMaps are represented as "{}".
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ImmutableMap<String, Integer>> type =
     *     TypeFactory.getType("ImmutableMap<String, Integer>");
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
     * String result = type.stringOf(map);
     * // Returns: {"a":1,"b":2}
     *
     * String empty = type.stringOf(ImmutableMap.empty());
     * // Returns: {}
     * }</pre>
     *
     * @param x The ImmutableMap object to convert
     * @return The JSON string representation of the ImmutableMap, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final T x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.isEmpty()) {
            return "{}";
        }

        return Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     * Parses a JSON string to create a ImmutableMap object.
     * The method handles:
     * - {@code null} input returns null
     * - Empty string or "{}" returns an empty ImmutableMap of the appropriate type
     * - Valid JSON object strings are deserialized into the ImmutableMap
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ImmutableMap<String, Integer>> type =
     *     TypeFactory.getType("ImmutableMap<String, Integer>");
     * ImmutableMap<String, Integer> map = type.valueOf("{\"a\":1,\"b\":2}");
     * // map contains {"a"=1, "b"=2}
     *
     * ImmutableMap<String, Integer> empty = type.valueOf("{}");
     * // Returns empty ImmutableMap
     * }</pre>
     *
     * @param str The JSON string to parse
     * @return The parsed ImmutableMap object, or {@code null} if the input is null
     */
    @Override
    public T valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if ("{}".equals(str)) {
            return (T) ImmutableMap.empty();
        } else {
            final ImmutableMap<K, V> map = Utils.jsonParser.deserialize(str, jdc, ImmutableMap.class);

            return map == null ? null : (T) ImmutableMap.wrap(map);
        }
    }

    /**
     * Appends the string representation of a ImmutableMap to an Appendable.
     * The ImmutableMap is serialized as a JSON object. If the Appendable is a Writer,
     * the serialization is performed directly to the Writer for better performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ImmutableMap<String, Integer>> type =
     *     TypeFactory.getType("ImmutableMap<String, Integer>");
     * StringBuilder sb = new StringBuilder();
     * ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
     * type.appendTo(sb, map);
     * // sb contains: {"a":1,"b":2}
     * }</pre>
     *
     * @param appendable The Appendable to write to
     * @param x The ImmutableMap to append
     * @throws IOException if an I/O error occurs while appending
     */
    @Override
    public void appendTo(final Appendable appendable, final T x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            // writer.write(stringOf(x));

            if (appendable instanceof Writer writer) {
                Utils.jsonParser.serialize(x, Utils.jsc, writer);
            } else {
                appendable.append(Utils.jsonParser.serialize(x, Utils.jsc));
            }
        }
    }

    /**
     * Generates the type name for a ImmutableMap with the specified implementation class, key and value types.
     *
     * @param typeClass The ImmutableMap implementation class
     * @param keyTypeName The name of the key type
     * @param valueTypeName The name of the value type
     * @param isDeclaringName Whether to use declaring names (true) or regular names (false)
     * @return The formatted type name string
     */
    protected static String getTypeName(final Class<?> typeClass, final String keyTypeName, final String valueTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(keyTypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(keyTypeName).name() + WD.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).name() + WD.GREATER_THAN;
        }
    }
}
