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
     * @return the declaring name of this type (e.g., "HBaseColumn&lt;String&gt;")
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the HBaseColumn type handled by this type handler.
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
     * @return an array containing the value type as the only parameter type
     */
    @Override
    public Type<T>[] getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Converts an HBaseColumn to its string representation.
     * The format is "version:value" where version is the timestamp/version number
     * and value is the string representation of the stored value.
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
     * @param str the string to parse in format "version:value"
     * @return a new HBaseColumn instance with the parsed version and value, or {@code null} if the input is {@code null} or empty
     */
    @Override
    public HBaseColumn<T> valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final int index = str.indexOf(_SEPARATOR);

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
