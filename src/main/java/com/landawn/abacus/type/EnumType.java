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
import java.lang.reflect.Field;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.annotation.Type.EnumBy;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.BiMap;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for Enum types.
 * This class provides serialization, deserialization, and database operations for Java Enum types.
 * It supports both ordinal (numeric) and name (string) representations of enums,
 * and handles custom JSON/XML field names through annotations.
 *
 * @param <T> the enum type, must extend Enum<T>
 */
@SuppressWarnings("java:S2160")
public final class EnumType<T extends Enum<T>> extends SingleValueType<T> {
    public static final String ENUM = Enum.class.getSimpleName();

    private static final String NULL = "null";
    private final BiMap<Number, T> numberEnum = new BiMap<>();
    private final Map<T, String> enumJsonXmlNameMap;
    private final Map<String, T> jsonXmlNameEnumMap;
    private final EnumBy enumBy;

    private boolean hasNull = false;

    EnumType(final String enumClassName) {
        this(enumClassName, false);
    }

    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
    EnumType(final String clsName, final boolean ordinal) {
        super(ordinal ? clsName + "(true)" : clsName, (Class<T>) getEnumClass(ClassUtil.forClass(clsName)));

        enumJsonXmlNameMap = new EnumMap<>(typeClass);
        jsonXmlNameEnumMap = new HashMap<>();

        for (final T enumConstant : typeClass.getEnumConstants()) {
            numberEnum.put(enumConstant.ordinal(), enumConstant);

            final String jsonXmlName = getJsonXmlName(enumConstant);
            enumJsonXmlNameMap.put(enumConstant, jsonXmlName);
            jsonXmlNameEnumMap.put(jsonXmlName, enumConstant);
            jsonXmlNameEnumMap.put(enumConstant.name(), enumConstant);
        }

        try {
            //noinspection ConstantValue
            hasNull = Enum.valueOf(typeClass, NULL) != null;
        } catch (final Exception e) {
            // ignore;
        }

        enumBy = ordinal ? EnumBy.ORDINAL : EnumBy.NAME;
    }

    /**
     * Returns the enumeration strategy used by this type handler.
     * Indicates whether enums are stored by ordinal (numeric) or name (string).
     *
     * @return EnumBy.ORDINAL if using numeric representation, EnumBy.NAME if using string representation
     */
    public EnumBy enumerated() {
        return enumBy;
    }

    /**
     * Indicates whether this enum type is serializable.
     * Enums are always serializable.
     *
     * @return true, as enums are always serializable
     */
    @Override
    public boolean isSerializable() {
        return true;
    }

    /**
     * Indicates whether instances of this enum type are immutable.
     * Enums are always immutable in Java.
     *
     * @return true, as enums are immutable
     */
    @Override
    public boolean isImmutable() {
        return true;
    }

    /**
     * Converts an enum value to its string representation.
     * If a custom JSON value type is defined, uses the parent class implementation.
     * Otherwise, returns the enum's name.
     *
     * @param x the enum value to convert. Can be null.
     * @return The string representation of the enum, or null if input is null
     */
    @Override
    public String stringOf(final T x) {
        return (jsonValueType == null) ? (x == null ? null : x.name()) : super.stringOf(x);
    }

    /**
     * Converts a string representation back to an enum value.
     * Supports multiple formats:
     * - Enum name (exact match)
     * - Custom JSON/XML field names from annotations
     * - Numeric strings (interpreted as ordinals)
     * - Empty/null strings return null
     * - Special handling for "null" string if no enum constant named "null" exists
     *
     * @param str the string to convert. Can be null or empty.
     * @return The enum value corresponding to the string, or null if input is null/empty
     * @throws IllegalArgumentException if the string doesn't match any enum value
     */
    @Override
    public T valueOf(final String str) {
        if (jsonValueType == null) {
            if (Strings.isEmpty(str) || (!hasNull && NULL.equals(str))) {
                return null; // NOSONAR
            }

            if (Strings.isAsciiDigitalInteger(str) && !jsonXmlNameEnumMap.containsKey(str)) {
                return valueOf(Numbers.toInt(str));
            } else {
                final T val = jsonXmlNameEnumMap.get(str);

                return Objects.requireNonNullElseGet(val, () -> Enum.valueOf(typeClass, str));
            }
        } else {
            return super.valueOf(str);
        }
    }

    /**
     * Converts an integer ordinal value to its corresponding enum constant.
     * Zero is treated as null unless there's an enum with ordinal 0.
     *
     * @param value the ordinal value of the enum
     * @return The enum constant with the specified ordinal, or null if value is 0 and no enum has ordinal 0
     * @throws IllegalArgumentException if no enum constant exists with the given ordinal (except for 0)
     */
    public T valueOf(final int value) {
        final T result = numberEnum.get(value);

        if ((result == null) && (value != 0)) {
            throw new IllegalArgumentException("No " + typeClass.getName() + " for int value: " + value);
        }

        return result;
    }

    /**
     * Retrieves an enum value from a ResultSet at the specified column index.
     * The retrieval method depends on the enumBy setting:
     * - ORDINAL: reads as integer
     * - NAME: reads as string
     *
     * @param rs the ResultSet containing the data
     * @param columnIndex the column index (1-based) of the enum value
     * @return The enum value at the specified column, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public T get(final ResultSet rs, final int columnIndex) throws SQLException {
        if (jsonValueType == null) {
            if (enumBy == EnumBy.ORDINAL) {
                return valueOf(rs.getInt(columnIndex));
            } else {
                return valueOf(rs.getString(columnIndex));
            }
        } else {
            return super.get(rs, columnIndex);
        }
    }

    /**
     * Retrieves an enum value from a ResultSet using the specified column label.
     * The retrieval method depends on the enumBy setting:
     * - ORDINAL: reads as integer
     * - NAME: reads as string
     *
     * @param rs the ResultSet containing the data
     * @param columnLabel the label of the column containing the enum value
     * @return The enum value in the specified column, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public T get(final ResultSet rs, final String columnLabel) throws SQLException {
        if (jsonValueType == null) {
            if (enumBy == EnumBy.ORDINAL) {
                return valueOf(rs.getInt(columnLabel));
            } else {
                return valueOf(rs.getString(columnLabel));
            }
        } else {
            return super.get(rs, columnLabel);
        }
    }

    /**
     * Sets an enum value as a parameter in a PreparedStatement.
     * The storage method depends on the enumBy setting:
     * - ORDINAL: stores as integer (ordinal value)
     * - NAME: stores as string (enum name)
     *
     * @param stmt the PreparedStatement in which to set the parameter
     * @param columnIndex the parameter index (1-based) to set
     * @param x the enum value to set. Can be null.
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final T x) throws SQLException {
        if (jsonValueType == null) {
            if (enumBy == EnumBy.ORDINAL) {
                stmt.setInt(columnIndex, (x == null) ? 0 : numberEnum.getByValue(x).intValue());
            } else {
                stmt.setString(columnIndex, (x == null) ? null : x.name());
            }
        } else {
            super.set(stmt, columnIndex, x);
        }
    }

    /**
     * Sets an enum value as a named parameter in a CallableStatement.
     * The storage method depends on the enumBy setting:
     * - ORDINAL: stores as integer (ordinal value)
     * - NAME: stores as string (enum name)
     *
     * @param stmt the CallableStatement in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the enum value to set. Can be null.
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final T x) throws SQLException {
        if (jsonValueType == null) {
            if (enumBy == EnumBy.ORDINAL) {
                stmt.setInt(parameterName, (x == null) ? 0 : numberEnum.getByValue(x).intValue());
            } else {
                stmt.setString(parameterName, (x == null) ? null : x.name());
            }
        } else {
            super.set(stmt, parameterName, x);
        }
    }

    /**
     * Writes an enum value to a CharacterWriter with the specified serialization configuration.
     * The output format depends on the enumBy setting:
     * - ORDINAL: writes the ordinal value as an integer
     * - NAME: writes the JSON/XML field name (possibly quoted based on config)
     *
     * @param writer the CharacterWriter to write to
     * @param x the enum value to write. Can be null.
     * @param config the serialization configuration for quotation settings
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final T x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            if (jsonValueType == null) {
                if (enumBy == EnumBy.ORDINAL) {
                    writer.writeInt(x.ordinal());
                } else {
                    final char ch = config == null ? 0 : config.getStringQuotation();

                    if (ch == 0) {
                        writer.writeCharacter(enumJsonXmlNameMap.get(x));
                    } else {
                        writer.write(ch);
                        writer.writeCharacter(enumJsonXmlNameMap.get(x));
                        writer.write(ch);
                    }
                }
            } else {
                super.writeCharacter(writer, x, config);
            }
        }
    }

    private String getJsonXmlName(final T enumConstant) {
        try {
            final Field field = enumConstant.getClass().getField(enumConstant.name());

            if (field.isAnnotationPresent(JsonXmlField.class) && Strings.isNotEmpty(field.getAnnotation(JsonXmlField.class).name())) {
                return field.getAnnotation(JsonXmlField.class).name();
            }

            try { //NOSONAR
                if (field.isAnnotationPresent(com.alibaba.fastjson2.annotation.JSONField.class)
                        && Strings.isNotEmpty(field.getAnnotation(com.alibaba.fastjson2.annotation.JSONField.class).name())) {
                    return field.getAnnotation(com.alibaba.fastjson2.annotation.JSONField.class).name();
                }
            } catch (final Throwable e) {
                // ignore
            }

            try { //NOSONAR
                if (field.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonProperty.class)
                        && Strings.isNotEmpty(field.getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class).value())) {
                    return field.getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class).value();
                }
            } catch (final Throwable e) {
                // ignore
            }
        } catch (NoSuchFieldException | SecurityException e) {
            // should never happen.
        }

        return enumConstant.name();
    }

    private static Class<?> getEnumClass(final Class<?> cls) {
        return cls.isEnum() ? cls : cls.getEnclosingClass();
    }
}