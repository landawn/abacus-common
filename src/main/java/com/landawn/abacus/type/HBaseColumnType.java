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

import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.HBaseColumn;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for HBaseColumn objects.
 * HBaseColumn represents a value with an associated version/timestamp, commonly used in HBase data modeling.
 * This class provides serialization and deserialization capabilities for HBaseColumn instances.
 *
 * @param <T> the type of value stored in the HBaseColumn
 */
@SuppressWarnings("java:S2160")
public class HBaseColumnType<T> extends AbstractType<HBaseColumn<T>> {

    private static final String SEPARATOR = ":";

    private static final char _SEPARATOR = ':';

    public static final String HBASE_COLUMN = "HBaseColumn";

    private final String declaringName;

    private final Class<HBaseColumn<T>> typeClass;

    private final Type<T>[] parameterTypes;

    private final Type<T> elementType;

    /**
     * Package-private constructor for HBaseColumnType.
     * Creates a type handler for HBaseColumn objects containing values of the specified type.
     * This constructor is called by the TypeFactory to create HBaseColumn&lt;T&gt; type instances.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Obtained via TypeFactory
     * Type<HBaseColumn<String>> type = TypeFactory.getType("HBaseColumn<String>");
     * HBaseColumn<String> column = new HBaseColumn<>("value", 1234567890L);
     * String serialized = type.stringOf(column);  // "1234567890:value"
     * HBaseColumn<String> parsed = type.valueOf("1234567890:value");
     * }</pre>
     *
     * @param typeClass the Class object for HBaseColumn
     * @param parameterTypeName the name of the type for values stored in the HBaseColumn
     */
    HBaseColumnType(final Class<HBaseColumn<T>> typeClass, final String parameterTypeName) {
        super(getTypeName(typeClass, parameterTypeName, false));

        declaringName = getTypeName(typeClass, parameterTypeName, true);

        this.typeClass = typeClass;
        parameterTypes = new Type[] { TypeFactory.getType(parameterTypeName) };
        elementType = parameterTypes[0];
    }

    /**
     * Returns the declaring name of this HBaseColumn type.
     * The declaring name represents the type in a simplified format suitable for type declarations,
     * using simple class names rather than fully qualified names.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<HBaseColumn<String>> type = TypeFactory.getType("HBaseColumn<String>");
     * String declaringName = type.declaringName();
     * // Returns: "HBaseColumn<String>"
     * }</pre>
     *
     * @return the declaring name of this type (e.g., "HBaseColumn&lt;String&gt;")
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the HBaseColumn type handled by this type handler.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<HBaseColumn<String>> type = TypeFactory.getType("HBaseColumn<String>");
     * Class<?> clazz = type.clazz();
     * // Returns: HBaseColumn.class
     * }</pre>
     *
     * @return the Class object for HBaseColumn
     */
    @Override
    public Class<HBaseColumn<T>> clazz() {
        return typeClass;
    }

    /**
     * Returns the type handler for the value element stored in the HBaseColumn.
     * This represents the type of the actual data value, not including the version information.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<HBaseColumn<String>> type = TypeFactory.getType("HBaseColumn<String>");
     * Type<String> elementType = type.getElementType();
     * // Returns: Type instance for String
     * }</pre>
     *
     * @return the Type instance representing the value type of this HBaseColumn
     */
    @Override
    public Type<T> getElementType() {
        return elementType;
    }

    /**
     * Returns an array containing the parameter types of this generic HBaseColumn type.
     * For HBaseColumn types, this array contains a single element representing the value type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<HBaseColumn<String>> type = TypeFactory.getType("HBaseColumn<String>");
     * Type<?>[] paramTypes = type.getParameterTypes();
     * // Returns: [Type<String>]
     * }</pre>
     *
     * @return an array containing the value type as the only parameter type
     */
    @Override
    public Type<T>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type is a generic type with type parameters.
     * HBaseColumn types are always generic types as they have a value type parameter.
     *
     * @return {@code true}, as HBaseColumn is a generic type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Converts an HBaseColumn to its string representation.
     * The format is "version:value" where version is the timestamp/version number
     * and value is the string representation of the stored value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<HBaseColumn<String>> type = TypeFactory.getType("HBaseColumn<String>");
     * HBaseColumn<String> column = new HBaseColumn<>("value1", 1234567890L);
     * String result = type.stringOf(column);
     * // Returns: "1234567890:value1"
     *
     * HBaseColumn<Integer> intColumn = new HBaseColumn<>(42, 9999999L);
     * Type<HBaseColumn<Integer>> intType = TypeFactory.getType("HBaseColumn<Integer>");
     * result = intType.stringOf(intColumn);
     * // Returns: "9999999:42"
     *
     * result = type.stringOf(null);
     * // Returns: null
     * }</pre>
     *
     * @param x the HBaseColumn to convert to string
     * @return the string representation in format "version:value", or {@code null} if the input is null
     */
    @Override
    public String stringOf(final HBaseColumn<T> x) {
        return x == null ? null : x.version() + SEPARATOR + elementType.stringOf(x.value());
    }

    /**
     * Parses a string representation into an HBaseColumn instance.
     * The string should be in the format "version:value" where version is a long number
     * representing the timestamp/version, and value is parsed according to the element type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<HBaseColumn<String>> type = TypeFactory.getType("HBaseColumn<String>");
     * HBaseColumn<String> result = type.valueOf("1234567890:value1");
     * // Returns: HBaseColumn with version=1234567890, value="value1"
     *
     * Type<HBaseColumn<Integer>> intType = TypeFactory.getType("HBaseColumn<Integer>");
     * HBaseColumn<Integer> intResult = intType.valueOf("9999999:42");
     * // Returns: HBaseColumn with version=9999999, value=42
     *
     * result = type.valueOf(null);
     * // Returns: null
     *
     * result = type.valueOf("");
     * // Returns: null
     * }</pre>
     *
     * @param str the string to parse in format "version:value"
     * @return a new HBaseColumn instance with the parsed version and value, or {@code null} if the input is {@code null} or empty
     */
    @Override
    public HBaseColumn<T> valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final int index = str.indexOf(_SEPARATOR);

        if (index < 0) {
            throw new IllegalArgumentException("Invalid HBaseColumn format. Expected 'version:value' but got: " + str);
        }

        final long version = Long.parseLong(str.substring(0, index));
        final T value = elementType.valueOf(str.substring(index + 1));

        return new HBaseColumn<>(value, version);
    }

    /**
     * Generates a type name string for an HBaseColumn type with the specified value type.
     * The format depends on whether a declaring name (simplified) or full name is requested.
     *
     * @param typeClass the HBaseColumn class (not used in current implementation but kept for consistency)
     * @param parameterTypeName the name of the value type
     * @param isDeclaringName {@code true} to generate a declaring name with simple class names, {@code false} for fully qualified names
     * @return the formatted type name (e.g., "HBaseColumn&lt;String&gt;" or "com.landawn.abacus.util.HBaseColumn&lt;java.lang.String&gt;")
     */
    @SuppressWarnings("unused")
    protected static String getTypeName(final Class<?> typeClass, final String parameterTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(HBaseColumn.class) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(HBaseColumn.class) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + WD.GREATER_THAN;
        }
    }
}
