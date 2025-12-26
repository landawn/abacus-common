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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.SuppressFBWarnings;
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
 * <p>EnumType instances are typically obtained through the TypeFactory and support conversion
 * between enum values and their string/numeric representations. The type handler can be configured
 * to use either the enum's ordinal value or its name for serialization.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Define an enum
 * enum Status { PENDING, ACTIVE, COMPLETED }
 *
 * // Get EnumType through TypeFactory
 * Type<Status> statusType = TypeFactory.getType(Status.class);
 *
 * // Convert enum to string (by name)
 * String str = statusType.stringOf(Status.ACTIVE);
 * // Result: "ACTIVE"
 *
 * // Convert string to enum
 * Status status = statusType.valueOf("COMPLETED");
 * // Result: Status.COMPLETED
 *
 * // Get enum by ordinal
 * Status firstStatus = statusType.valueOf("0");
 * // Result: Status.PENDING
 * }</pre>
 *
 * @param <T> the enum type, must extend Enum&lt;T&gt;
 */
@SuppressWarnings("java:S2160")
public final class EnumType<T extends Enum<T>> extends SingleValueType<T> {
    public static final String ENUM = Enum.class.getSimpleName();

    private static final String NULL = "null";
    private final BiMap<Number, T> numberEnum = new BiMap<>();
    private final Map<T, String> enumJsonXmlNameMap;
    private final Map<String, T> jsonXmlNameEnumMap;
    private final com.landawn.abacus.util.EnumType enumRepresentation;

    private boolean hasNull = false;

    EnumType(final String enumClassName) {
        this(enumClassName, com.landawn.abacus.util.EnumType.NAME);
    }

    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
    EnumType(final String className, final com.landawn.abacus.util.EnumType enumRepresentation) {
        super(enumRepresentation == null ? className + "(NAME)" : className + "(" + enumRepresentation.name() + ")",
                (Class<T>) getEnumClass(ClassUtil.forClass(className)));

        enumJsonXmlNameMap = new EnumMap<>(typeClass);
        jsonXmlNameEnumMap = new HashMap<>();

        if (enumRepresentation == com.landawn.abacus.util.EnumType.CODE) {
            final Method getCodeMethod = ClassUtil.getDeclaredMethod(typeClass, "code");

            if (getCodeMethod == null || !Modifier.isPublic(getCodeMethod.getModifiers()) || !int.class.equals(getCodeMethod.getReturnType())) {
                throw new RuntimeException("No method: public int code() found in enum class: " + ClassUtil.getCanonicalClassName(typeClass));
            }

            //    final Method fromCodeMethod = ClassUtil.getDeclaredMethod(typeClass, "fromCode", int.class);
            //
            //    if (fromCodeMethod == null || !Modifier.isPublic(fromCodeMethod.getModifiers()) || !Modifier.isStatic(fromCodeMethod.getModifiers())
            //            || !typeClass.equals(fromCodeMethod.getReturnType())) {
            //        throw new RuntimeException("No method: public static " + typeClass.getSimpleName() + " fromCode(int) found in enum class: "
            //                + ClassUtil.getCanonicalClassName(typeClass));
            //    }

            for (final T enumConstant : typeClass.getEnumConstants()) {
                int code = ClassUtil.invokeMethod(enumConstant, getCodeMethod);
                numberEnum.put(code, enumConstant);

                final String jsonXmlName = getJsonXmlName(enumConstant);
                enumJsonXmlNameMap.put(enumConstant, jsonXmlName);
                jsonXmlNameEnumMap.put(jsonXmlName, enumConstant);
                jsonXmlNameEnumMap.put(enumConstant.name(), enumConstant);
            }
        } else {
            for (final T enumConstant : typeClass.getEnumConstants()) {
                numberEnum.put(enumConstant.ordinal(), enumConstant);

                final String jsonXmlName = getJsonXmlName(enumConstant);
                enumJsonXmlNameMap.put(enumConstant, jsonXmlName);
                jsonXmlNameEnumMap.put(jsonXmlName, enumConstant);
                jsonXmlNameEnumMap.put(enumConstant.name(), enumConstant);
            }
        }

        try {
            //noinspection ConstantValue
            hasNull = Enum.valueOf(typeClass, NULL) != null;
        } catch (final Exception e) {
            // ignore;
        }

        this.enumRepresentation = enumRepresentation == null ? com.landawn.abacus.util.EnumType.NAME : enumRepresentation;
    }

    /**
     * Returns the enumeration strategy used by this type handler.
     * Indicates whether enums are stored by ordinal (numeric), name (string), or code (numeric).
     *
     * @return the configured enum representation
     */
    public com.landawn.abacus.util.EnumType enumerated() {
        return enumRepresentation;
    }

    /**
     * Indicates whether this enum type is serializable.
     * Enums are always serializable.
     *
     * @return {@code true}, as enums are always serializable
     */
    @Override
    public boolean isSerializable() {
        return true;
    }

    /**
     * Indicates whether instances of this enum type are immutable.
     * Enums are always immutable in Java.
     *
     * @return {@code true}, as enums are immutable
     */
    @Override
    public boolean isImmutable() {
        return true;
    }

    /**
     * Converts an enum value to its string representation.
     * If a custom JSON value type is defined, uses the parent class implementation.
     * Otherwise, returns the enum constant name.
     *
     * @param x the enum value to convert; may be {@code null}
     * @return the enum constant name, or {@code null} if input is null
     */
    @Override
    public String stringOf(final T x) {
        return (jsonValueType == null) ? (x == null ? null : x.name()) : super.stringOf(x);
    }

    /**
     * Converts a string representation back to an enum value.
     * Supports enum names, JSON/XML field names from annotations, and numeric strings.
     * Numeric strings are interpreted as ordinals (or codes when CODE is configured) unless the
     * same string is defined as a JSON/XML name.
     * Empty strings return {@code null}. The literal string {@code "null"} returns {@code null}
     * when the enum does not define a constant named {@code "null"}.
     *
     * @param str the string to convert; may be {@code null} or empty
     * @return the enum value corresponding to the string, or {@code null} if input is null/empty
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
                final T value = jsonXmlNameEnumMap.get(str);

                return Objects.requireNonNullElseGet(value, () -> Enum.valueOf(typeClass, str));
            }
        } else {
            return super.valueOf(str);
        }
    }

    /**
     * Converts an integer ordinal or code value to its corresponding enum constant.
     * For CODE representation, the value is matched against the {@code code()} values; otherwise,
     * it is matched against ordinal values. A value of 0 returns {@code null} when no constant
     * is mapped to 0.
     *
     * @param value the ordinal or code value
     * @return the enum constant for the specified value, or {@code null} if value is 0 and no constant maps to 0
     * @throws IllegalArgumentException if no enum constant exists with the given value (except for 0)
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
     * The retrieval method depends on the enumRepresentation setting:
     * - ORDINAL or CODE: reads as integer
     * - NAME: reads as string
     *
     * @param rs the ResultSet containing the data
     * @param columnIndex the column index (1-based) of the enum value
     * @return the enum value at the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public T get(final ResultSet rs, final int columnIndex) throws SQLException {
        if (jsonValueType == null) {
            if (enumRepresentation == com.landawn.abacus.util.EnumType.ORDINAL || enumRepresentation == com.landawn.abacus.util.EnumType.CODE) {
                final Object intValue = rs.getObject(columnIndex);
                return intValue == null ? null : valueOf(Numbers.toInt(intValue));
            } else {
                return valueOf(rs.getString(columnIndex));
            }
        } else {
            return super.get(rs, columnIndex);
        }
    }

    /**
     * Retrieves an enum value from a ResultSet using the specified column label.
     * The retrieval method depends on the enumRepresentation setting:
     * - ORDINAL or CODE: reads as integer
     * - NAME: reads as string
     *
     * @param rs the ResultSet containing the data
     * @param columnLabel the label of the column containing the enum value
     * @return the enum value in the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public T get(final ResultSet rs, final String columnLabel) throws SQLException {
        if (jsonValueType == null) {
            if (enumRepresentation == com.landawn.abacus.util.EnumType.ORDINAL || enumRepresentation == com.landawn.abacus.util.EnumType.CODE) {
                final int intValue = rs.getInt(columnLabel);
                return rs.wasNull() ? null : valueOf(intValue);
            } else {
                return valueOf(rs.getString(columnLabel));
            }
        } else {
            return super.get(rs, columnLabel);
        }
    }

    /**
     * Sets an enum value as a parameter in a PreparedStatement.
     * The storage method depends on the enumRepresentation setting:
     * - ORDINAL or CODE: stores as integer (ordinal or code value)
     * - NAME: stores as string (enum name)
     *
     * @param stmt the PreparedStatement in which to set the parameter
     * @param columnIndex the parameter index (1-based) to set
     * @param x the enum value to set; may be {@code null}
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final T x) throws SQLException {
        if (jsonValueType == null) {
            if (enumRepresentation == com.landawn.abacus.util.EnumType.ORDINAL || enumRepresentation == com.landawn.abacus.util.EnumType.CODE) {
                if (x == null) {
                    stmt.setNull(columnIndex, Types.INTEGER);
                } else {
                    stmt.setInt(columnIndex, numberEnum.getByValue(x).intValue());
                }
            } else {
                stmt.setString(columnIndex, (x == null) ? null : x.name());
            }
        } else {
            super.set(stmt, columnIndex, x);
        }
    }

    /**
     * Sets an enum value as a named parameter in a CallableStatement.
     * The storage method depends on the enumRepresentation setting:
     * - ORDINAL or CODE: stores as integer (ordinal or code value)
     * - NAME: stores as string (enum name)
     *
     * @param stmt the CallableStatement in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the enum value to set; may be {@code null}
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final T x) throws SQLException {
        if (jsonValueType == null) {
            if (enumRepresentation == com.landawn.abacus.util.EnumType.ORDINAL || enumRepresentation == com.landawn.abacus.util.EnumType.CODE) {
                if (x == null) {
                    stmt.setNull(parameterName, Types.INTEGER);
                } else {
                    stmt.setInt(parameterName, numberEnum.getByValue(x).intValue());
                }
            } else {
                stmt.setString(parameterName, (x == null) ? null : x.name());
            }
        } else {
            super.set(stmt, parameterName, x);
        }
    }

    /**
     * Writes an enum value to a CharacterWriter with the specified serialization configuration.
     * The output format depends on the enumRepresentation setting:
     * - ORDINAL or CODE: writes the ordinal or code value as an integer
     * - NAME: writes the JSON/XML field name (possibly quoted based on config)
     *
     * @param writer the CharacterWriter to write to
     * @param x the enum value to write; may be {@code null}
     * @param config the serialization configuration for quotation settings
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final T x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            if (jsonValueType == null) {
                if (enumRepresentation == com.landawn.abacus.util.EnumType.ORDINAL || enumRepresentation == com.landawn.abacus.util.EnumType.CODE) {
                    writer.writeInt(numberEnum.getByValue(x).intValue());
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

    private static Class<?> getEnumClass(final Class<?> clazz) {
        return clazz.isEnum() ? clazz : clazz.getEnclosingClass();
    }
}
