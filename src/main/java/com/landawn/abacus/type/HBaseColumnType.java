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

import java.util.List;

import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.HBaseColumn;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

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

    /**
     * Prefix for the versioned extension to the legacy {@code version:value} format.
     * It is deliberately specific so ordinary legacy values, including {@code null}
     * and values beginning with a backslash, continue through the element type unchanged.
     */
    private static final String ENCODING_PREFIX = "\\~abacus-hbase-column:v1~";

    private static final String NULL_VALUE = ENCODING_PREFIX + "N";

    private static final String NULL_REPRESENTATION = ENCODING_PREFIX + "R";

    private static final String ESCAPED_VALUE_PREFIX = ENCODING_PREFIX + "V";

    /** The type name constant for HBaseColumn type identification. */
    public static final String HBASE_COLUMN = "HBaseColumn";

    private final String declaringName;

    private final Class<HBaseColumn<T>> typeClass;

    private final List<Type<?>> parameterTypes;

    private final Type<T> elementType;

    /**
     * Package-private constructor for HBaseColumnType.
     * This constructor is called by the TypeFactory to create {@code HBaseColumn<T>} type instances.
     *
     * @param typeClass the Class object for HBaseColumn
     * @param parameterTypeName the name of the type for values stored in the HBaseColumn
     */
    HBaseColumnType(final Class<HBaseColumn<T>> typeClass, final String parameterTypeName) {
        super(getTypeName(typeClass, parameterTypeName, false));

        declaringName = getTypeName(typeClass, parameterTypeName, true);

        this.typeClass = typeClass;
        elementType = TypeFactory.getType(parameterTypeName);
        parameterTypes = List.of(elementType);
    }

    /**
     * Returns the declaring name of this HBaseColumn type, using simple class names
     * (e.g., {@code "HBaseColumn<String>"}).
     *
     * @return the declaring name of this type
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code HBaseColumn.class}
     */
    @Override
    public Class<HBaseColumn<T>> javaType() {
        return typeClass;
    }

    /**
     * Returns the type handler for the value element stored in the HBaseColumn.
     * This represents the type of the actual data value, not the version information.
     *
     * @return the {@link Type} instance representing the value type of this HBaseColumn
     */
    @Override
    public Type<T> elementType() {
        return elementType;
    }

    /**
     * Returns an immutable list containing the single parameter type of this generic HBaseColumn type
     * (the value type).
     *
     * @return an immutable list containing the value type as the only parameter type
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type is a generic type with type parameters.
     * HBaseColumn types are always generic types as they have a value type parameter.
     *
     * @return {@code true}, as HBaseColumn is a generic type
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Serializes an {@link HBaseColumn} to the {@code "version:value"} string format,
     * where {@code version} is the column's timestamp/version number and {@code value}
     * is the element type's string representation of the stored value. For compatibility,
     * ordinary values retain the legacy format without escaping. Nulls and otherwise
     * ambiguous representations use a reserved, versioned extension beginning with
     * {@code \~abacus-hbase-column:v1~}. A non-null representation beginning with that
     * prefix is escaped by the extension so it can still round-trip.
     *
     * <p>Legacy payloads are otherwise preserved. The unavoidable compatibility exception
     * is a pre-existing payload equal to {@code \~abacus-hbase-column:v1~N} or
     * {@code \~abacus-hbase-column:v1~R}, or beginning with
     * {@code \~abacus-hbase-column:v1~V}; those forms collide with the reserved extension.
     * Newly emitted values beginning with the reserved prefix are escaped automatically.</p>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the HBaseColumn to serialize; may be {@code null}
     * @return the serialized string, or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final HBaseColumn<T> x) {
        if (x == null) {
            return null;
        }

        if (x.value() == null) {
            return x.version() + SEPARATOR + NULL_VALUE;
        }

        final String value = elementType.stringOf(x.value());

        if (value == null) {
            return x.version() + SEPARATOR + NULL_REPRESENTATION;
        }

        return x.version() + SEPARATOR + (value.startsWith(ENCODING_PREFIX) ? ESCAPED_VALUE_PREFIX + value : value);
    }

    /**
     * Deserializes a {@code "version:value"} string into an {@link HBaseColumn} instance.
     * The part before the first {@code ':'} is parsed as the {@code long} version/timestamp,
     * and the remainder is parsed by the element type handler.
     * Legacy payloads are passed unchanged to the element type handler, including the
     * literal {@code null} and backslash-prefixed values. Only the reserved, versioned
     * extension emitted by {@link #stringOf(HBaseColumn)} is interpreted by this class.
     * Pre-existing payloads matching one of the reserved extension forms documented on
     * {@code stringOf} are the sole compatibility exception.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse in {@code "version:value"} format; may be {@code null} or empty
     * @return the deserialized column value
     *         or {@code null} if {@code str} is {@code null} or empty
     * @throws IllegalArgumentException if the string has no {@code ':'} separator or its version prefix is not a valid {@code long}
     * @see #valueOf(Object)
     * @see #stringOf(HBaseColumn)
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
        final String valueText = str.substring(index + 1);
        final T value;

        if (NULL_VALUE.equals(valueText)) {
            value = null;
        } else if (NULL_REPRESENTATION.equals(valueText)) {
            value = elementType.valueOf((String) null);
        } else if (valueText.startsWith(ESCAPED_VALUE_PREFIX)) {
            value = elementType.valueOf(valueText.substring(ESCAPED_VALUE_PREFIX.length()));
        } else {
            value = elementType.valueOf(valueText);
        }

        return new HBaseColumn<>(value, version);
    }

    /**
     * Generates a type name string for an HBaseColumn type with the specified value type.
     * The format depends on whether a declaring name (simplified) or a fully qualified name is requested.
     * Note: the {@code typeClass} parameter is accepted for API consistency but is unused;
     * the type name is always based on {@link com.landawn.abacus.util.HBaseColumn}.
     *
     * @param typeClass accepted for API consistency but not used; the name is always based on {@code HBaseColumn}
     * @param parameterTypeName the name of the value type stored in the HBaseColumn
     * @param isDeclaringName {@code true} to generate a simplified name (e.g., {@code "HBaseColumn<String>"}),
     *                        {@code false} for a fully qualified name
     * @return the formatted type name
     */
    @SuppressWarnings("unused")
    protected static String getTypeName(final Class<?> typeClass, final String parameterTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(HBaseColumn.class) + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(HBaseColumn.class) + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + SK.GREATER_THAN;
        }
    }
}
