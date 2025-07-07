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
import java.util.Map;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for Map objects with generic key and value types.
 * This class handles serialization and deserialization of Map instances.
 * Special support is provided for Spring's MultiValueMap where values are Lists.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @param <T> the specific Map implementation type
 */
@SuppressWarnings("java:S2160")
public class MapType<K, V, T extends Map<K, V>> extends AbstractType<T> {

    private final String declaringName;

    private final Class<T> typeClass;

    private final Type<?>[] parameterTypes;

    private final JSONDeserializationConfig jdc;

    MapType(final Class<T> typeClass, final String keyTypeName, final String valueTypeName) {
        super(getTypeName(typeClass, keyTypeName, valueTypeName, false));

        declaringName = getTypeName(typeClass.isInterface() ? typeClass : Map.class, keyTypeName, valueTypeName, true);

        this.typeClass = typeClass;

        boolean isSpringMultiValueMap = false;

        try {
            isSpringMultiValueMap = ClassUtil.forClass("org.springframework.util.MultiValueMap").isAssignableFrom(typeClass);
        } catch (final Throwable e) {
            // ignore
        }

        if (isSpringMultiValueMap) {
            parameterTypes = new Type[] { TypeFactory.getType(keyTypeName), TypeFactory.getType("List<" + valueTypeName + ">") };
        } else {
            parameterTypes = new Type[] { TypeFactory.getType(keyTypeName), TypeFactory.getType(valueTypeName) };
        }

        jdc = JDC.create().setMapKeyType(parameterTypes[0]).setMapValueType(parameterTypes[1]);
    }

    /**
     * Returns the declaring name of this Map type.
     * The declaring name includes the fully qualified class names of the key and value types.
     *
     * @return The declaring name in format "MapClass<KeyDeclaringName, ValueDeclaringName>"
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the specific Map implementation type.
     *
     * @return The Class object for the Map implementation
     */
    @Override
    public Class<T> clazz() {
        return typeClass;
    }

    /**
     * Gets the parameter types for this generic Map type.
     * The array contains two elements: the key type at index 0 and the value type at index 1.
     * For Spring MultiValueMap, the value type will be List<V> instead of V.
     *
     * @return An array containing the key type and value type
     */
    @Override
    public Type<?>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type represents a Map.
     * For MapType, this always returns true.
     *
     * @return true, indicating that this type represents a Map
     */
    @Override
    public boolean isMap() {
        return true;
    }

    /**
     * Indicates whether this is a generic type.
     * For MapType, this always returns true since Map is parameterized with key and value types.
     *
     * @return true, indicating that Map is a generic type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Indicates whether instances of this type can be serialized.
     * Map objects are not directly serializable through this type handler.
     *
     * @return false, indicating that Map is not serializable through this type
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Gets the serialization type category for Map.
     * This indicates how the Map should be treated during serialization processes.
     *
     * @return SerializationType.MAP
     */
    @Override
    public SerializationType getSerializationType() {
        return SerializationType.MAP;
    }

    /**
     * Converts a Map object to its JSON string representation.
     * Empty maps are represented as "{}".
     *
     * @param x The Map object to convert
     * @return The JSON string representation of the Map, or null if the input is null
     */
    @MayReturnNull
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
     * Parses a JSON string to create a Map object.
     * The method handles:
     * - null input returns null
     * - Empty string or "{}" returns an empty Map of the appropriate type
     * - Valid JSON object strings are deserialized into the Map
     *
     * @param str The JSON string to parse
     * @return The parsed Map object, or null if the input is null
     */
    @MayReturnNull
    @Override
    public T valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if ("{}".equals(str)) {
            return (T) N.newMap(typeClass);
        } else {
            return Utils.jsonParser.deserialize(str, jdc, typeClass);
        }
    }

    /**
     * Appends the string representation of a Map to an Appendable.
     * The Map is serialized as a JSON object. If the Appendable is a Writer,
     * the serialization is performed directly to the Writer for better performance.
     *
     * @param appendable The Appendable to write to
     * @param x The Map to append
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
     * Generates the type name for a Map with the specified implementation class, key and value types.
     *
     * @param typeClass The Map implementation class
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