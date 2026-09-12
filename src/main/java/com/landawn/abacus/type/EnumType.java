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
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BiMap;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for Java {@link Enum} types.
 * This class provides serialization, deserialization, and database operations for enum constants.
 * It supports name (string), ordinal (numeric), and code (numeric, via an inherited or directly declared
 * public {@code int code()} method, or a public {@code int intValue()} method when {@code code()} is absent)
 * representations of enums, and handles custom JSON/XML field names through annotations.
 *
 * <p>{@code EnumType} instances are typically obtained through {@link TypeFactory} and support conversion
 * between enum values and their string/numeric representations. The configured representation controls
 * JDBC persistence and streaming JSON/XML output; {@link #stringOf(Enum)} itself returns the constant name
 * unless a JSON value accessor is configured.</p>
 *
 * <p>An enum annotated with a {@code @JsonXmlValue}/{@code @JsonXmlCreator} (or Jackson {@code @JsonValue}/
 * {@code @JsonCreator}) pair is written and read through those members and the pair takes precedence over the
 * configured representation, even {@code ORDINAL} or {@code CODE}. An enum carrying only a Jackson
 * {@code @JsonValue} (no creator) is also supported: its constants' values form a reverse map, so
 * {@link #valueOf(String)} and the {@code ResultSet} reads accept the value string first and the constant name
 * as a fallback. Two constants with the same value are rejected when the handler is built.</p>
 *
 * <p>The handler always describes the enum class itself: asking for the runtime class of a constant with a body
 * ({@code E.X.getClass()} when {@code X { ... }} overrides a method) yields a handler named after and equal to
 * {@code Type.of(E.class)}, not one named after the synthetic {@code E$1} class.</p>
 *
 * @param <T> the enum type, must extend {@code Enum<T>}
 */
@SuppressWarnings("java:S2160")
public final class EnumType<T extends Enum<T>> extends SingleValueType<T> {
    /** The type name constant for Enum type identification, equal to {@code "Enum"}. */
    public static final String ENUM = Enum.class.getSimpleName();

    private static final String NULL = "null";
    private final BiMap<Number, T> numberEnum = new BiMap<>();
    private final Map<T, String> enumJsonXmlNameMap;
    private final Map<String, T> jsonXmlNameEnumMap;
    private final com.landawn.abacus.util.EnumType enumRepresentation;

    /**
     * Reverse map {@code jsonValueType.stringOf(value) -> constant} for an enum with a Jackson {@code @JsonValue}
     * but no creator; {@code null} for every other enum (those with a creator read through it).
     */
    private final Map<String, T> jsonValueEnumMap;

    private boolean hasNull = false;

    /**
     * {@code true} if some constant's annotated JSON value is the literal string {@code "null"}; always
     * {@code false} for an enum with no annotated value member. Unlike {@link #hasNull} (which asks for a
     * constant <i>named</i> {@code "null"}, impossible for a Java-compiled enum), this can actually be true,
     * and it is what lets such a constant win over the literal-null rule in {@link #valueOf(String)}.
     */
    private boolean hasNullValue = false;

    /**
     * Package-private constructor for EnumType using the default NAME representation.
     * This constructor is called by the TypeFactory when no explicit representation is specified.
     *
     * @param enumClassName the fully qualified class name of the enum type
     */
    EnumType(final String enumClassName) {
        this(enumClassName, com.landawn.abacus.util.EnumType.NAME);
    }

    /**
     * Package-private constructor for EnumType with the specified enum representation.
     * This constructor is called by the TypeFactory to create enum type instances.
     *
     * @param className the fully qualified class name of the enum type, or of a constant's body class (which
     *                  resolves to, and is named after, its enclosing enum)
     * @param enumRepresentation the representation strategy to use ({@code NAME}, {@code ORDINAL}, or {@code CODE});
     *                           if {@code null}, defaults to {@code NAME}
     * @throws RuntimeException if {@code CODE} representation is configured but the enum class has no
     *         public {@code int code()} or {@code int intValue()} method.
     * @throws IllegalArgumentException if numeric codes, JSON/XML names or creator-less {@code @JsonValue} values
     *         are ambiguous between constants.
     */
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
    EnumType(final String className, final com.landawn.abacus.util.EnumType enumRepresentation) throws RuntimeException, IllegalArgumentException {
        super(enumTypeName(className, enumRepresentation), (Class<T>) getEnumClass(ClassUtil.forName(className)));

        enumJsonXmlNameMap = new EnumMap<>(typeClass);
        jsonXmlNameEnumMap = new HashMap<>();
        jsonValueEnumMap = jsonValueType != null && jsonCreatorMethod == null ? new HashMap<>() : null;

        if (enumRepresentation == com.landawn.abacus.util.EnumType.CODE) {
            // Prefer a public int code() accessor. Fall back to public int intValue(): the numeric enums in
            // com.landawn.abacus.util (Color/Gender/MediaType/OperationType/LockMode/Month/
            // CalendarField/YesNo) expose their numeric code via intValue() rather than code(), yet are exactly
            // the enums CODE representation is meant for. Without this fallback every one of them threw at
            // construction time when configured with EnumType.CODE.
            Method getCodeMethod = getPublicIntMethod(typeClass, "code");

            if (getCodeMethod == null) {
                getCodeMethod = getPublicIntMethod(typeClass, "intValue");
            }

            if (getCodeMethod == null) {
                throw new RuntimeException(
                        "No method: public int code() (or public int intValue()) found in enum class: " + ClassUtil.getCanonicalClassName(typeClass));
            }

            for (final T enumConstant : typeClass.getEnumConstants()) {
                int code = ClassUtil.invokeMethod(enumConstant, getCodeMethod);
                final T previous = numberEnum.get(code);

                if (previous != null) {
                    throw new IllegalArgumentException("Duplicate code " + code + " in enum class " + ClassUtil.getCanonicalClassName(typeClass) + ": "
                            + previous.name() + " and " + enumConstant.name());
                }

                numberEnum.put(code, enumConstant);

                final String jsonXmlName = getJsonXmlName(enumConstant);
                registerJsonXmlNames(enumConstant, jsonXmlName);
            }
        } else {
            for (final T enumConstant : typeClass.getEnumConstants()) {
                numberEnum.put(enumConstant.ordinal(), enumConstant);

                final String jsonXmlName = getJsonXmlName(enumConstant);
                registerJsonXmlNames(enumConstant, jsonXmlName);
            }
        }

        if (jsonValueType != null) {
            // The annotated value accessor is read once per constant here. It also answers whether some
            // constant claims the literal "null" as its value: 'null' is a reserved word and can never be a
            // constant NAME, so hasNull below cannot cover that case, and without this flag the literal-null
            // shortcut in valueOf(String) would hide such a constant from the creator.
            for (final T enumConstant : typeClass.getEnumConstants()) {
                final String valueStr = super.stringOf(enumConstant);

                if (NULL.equals(valueStr)) {
                    hasNullValue = true;
                }

                if (jsonValueEnumMap != null) {
                    // Lone Jackson @JsonValue: the constants' values are the only way back, so they must be unambiguous.
                    final T previous = jsonValueEnumMap.putIfAbsent(valueStr, enumConstant);

                    if (previous != null) {
                        throw new IllegalArgumentException("Duplicate 'JsonValue' value '" + valueStr + "' in enum class "
                                + ClassUtil.getCanonicalClassName(typeClass) + ": " + previous.name() + " and " + enumConstant.name());
                    }
                }
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
     * Returns the enumeration strategy used by this type handler:
     * {@code NAME}, {@code ORDINAL}, or {@code CODE}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EnumType<TimeUnit> nameType = (EnumType<TimeUnit>) (Type) TypeFactory.getType("java.util.concurrent.TimeUnit");
     * nameType.enumerated();   // returns com.landawn.abacus.util.EnumType.NAME
     *
     * EnumType<TimeUnit> ordType = (EnumType<TimeUnit>) (Type) TypeFactory.getType("java.util.concurrent.TimeUnit(ORDINAL)");
     * ordType.enumerated();    // returns com.landawn.abacus.util.EnumType.ORDINAL
     * }</pre>
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
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the enum value to convert; may be {@code null}
     * @return the enum constant name (or, when a JSON value accessor is configured, that accessor's
     *         string form), or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final T x) {
        return (jsonValueType == null) ? (x == null ? null : x.name()) : super.stringOf(x);
    }

    /**
     * Converts a string representation back to an enum value.
     * Supports enum names, JSON/XML field names from annotations, and numeric strings.
     * Numeric strings are interpreted as ordinals (or codes when CODE is configured) unless the
     * same string is defined as a JSON/XML name; a numeric string outside the {@code int} range matches
     * no constant.
     * Empty strings return {@code null}. The literal string {@code "null"} returns {@code null} unless some
     * constant claims it - as its name, its JSON/XML name, or its annotated JSON value - in which case that
     * constant is returned instead.
     *
     * <p>For an enum with a {@code @JsonValue}/{@code @JsonCreator} pair the (non-empty) string is converted to the
     * value type and handed to the creator; whatever the creator throws is propagated unwrapped. The literal-null
     * rule is applied before the creator, but only while no constant's annotated value is {@code "null"}; when one
     * is, {@code "null"} reaches the creator like any other value. For an enum with a
     * lone Jackson {@code @JsonValue} the string is looked up in the reverse map of the constants' values, then
     * matched against the constant names (and JSON/XML names) as a fallback; as in the name-based branch, a constant
     * that claims the value {@code "null"} wins over the literal-null rule.</p>
     *
     * <p>An <i>empty</i> string always yields {@code null}, in every branch: a constant cannot claim {@code ""}.</p>
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to convert; may be {@code null} or empty
     * @return the enum value corresponding to the string, or {@code null} if input is null/empty
     * @throws IllegalArgumentException if the string matches no constant (name, JSON/XML name, ordinal/code or
     *         annotated value); for an enum with an annotated creator, whatever that creator throws is propagated
     *         unwrapped instead
     * @see #valueOf(Object)
     * @see #stringOf(Enum)
     */
    @Override
    public T valueOf(final String str) throws IllegalArgumentException {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        if (jsonValueType == null) {
            final T value = jsonXmlNameEnumMap.get(str);

            if (value != null) {
                return value;
            } else if (!hasNull && NULL.equals(str)) {
                return null; // NOSONAR
            }

            if (Strings.isAsciiInteger(str)) {
                final int intValue;

                try {
                    intValue = Numbers.toInt(str);
                } catch (final ArithmeticException e) {
                    // "99999999999" is "no such constant", not an arithmetic failure.
                    throw new IllegalArgumentException("No " + typeClass.getName() + " for value: " + str, e);
                }

                return valueOf(intValue);
            } else {
                // 'value' is guaranteed null here (the non-null case already returned above).
                return Enum.valueOf(typeClass, str);
            }
        } else if (jsonValueEnumMap != null) {
            T value = jsonValueEnumMap.get(str);

            if (value == null) {
                value = jsonXmlNameEnumMap.get(str);
            }

            if (value != null) {
                return value;
            }

            // Same precedence as the name-based branch above: a constant claiming "null" wins over the literal-null rule.
            if (!hasNull && NULL.equals(str)) {
                return null; // NOSONAR
            }

            throw new IllegalArgumentException("No " + typeClass.getName() + " for value: " + str);
        } else if (!hasNull && !hasNullValue && NULL.equals(str)) {
            // Same precedence as the two branches above: a constant claiming "null" wins over the
            // literal-null rule, here by letting the creator see the string.
            return null; // NOSONAR
        } else {
            return super.valueOf(str);
        }
    }

    /**
     * Converts an integer ordinal or code value to its corresponding enum constant.
     * For {@code CODE} representation, the value is matched against the {@code code()} (or {@code intValue()}) values;
     * otherwise it is matched against ordinal values.
     * Returns {@code null} when {@code value} is {@code 0} and no constant is mapped to {@code 0}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EnumType<TimeUnit> t = (EnumType<TimeUnit>) (Type) TypeFactory.getType("java.util.concurrent.TimeUnit(ORDINAL)");
     * t.valueOf(0);    // returns TimeUnit.NANOSECONDS
     * t.valueOf(2);    // returns TimeUnit.MILLISECONDS
     * t.valueOf(99);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param value the ordinal or code value
     * @return the enum constant for the specified value, or {@code null} if the value is {@code 0} and no constant maps to {@code 0}
     * @throws IllegalArgumentException if no enum constant exists with the given value (and the value is not
     *         {@code 0}).
     */
    public T valueOf(final int value) throws IllegalArgumentException {
        final T result = numberEnum.get(value);

        if ((result == null) && (value != 0)) {
            throw new IllegalArgumentException("No " + typeClass.getName() + " for int value: " + value);
        }

        return result;
    }

    /**
     * Retrieves an enum value from a {@link ResultSet} at the specified column index.
     * The retrieval method depends on the {@code enumRepresentation} setting:
     * <ul>
     *   <li>ORDINAL or CODE: reads the column as an integer and maps it to the enum constant</li>
     *   <li>NAME: reads the column as a string and maps it to the enum constant by name</li>
     *   <li>annotated value member: reads the column as the value type and maps it through the creator, or
     *       through the reverse map of the constants' values when the enum has no creator</li>
     * </ul>
     *
     * @param rs          the {@link ResultSet} containing the data
     * @param columnIndex the 1-based column index of the enum value
     * @return the enum value at the specified column, or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is null when this method or the selected value type accesses the JDBC resource
     * @throws SQLException if a database access error occurs or the column index is invalid
     * @throws NumberFormatException if an ORDINAL or CODE column value cannot be parsed as an integer
     * @throws ArithmeticException if an ORDINAL or CODE column value cannot be represented exactly as an int
     * @throws IllegalArgumentException if the stored name, ordinal, code or annotated value has no matching enum constant
     */
    @Override
    public T get(final ResultSet rs, final int columnIndex)
            throws NullPointerException, SQLException, NumberFormatException, ArithmeticException, IllegalArgumentException {
        if (jsonValueType == null) {
            if (enumRepresentation == com.landawn.abacus.util.EnumType.ORDINAL || enumRepresentation == com.landawn.abacus.util.EnumType.CODE) {
                final Object intValue = rs.getObject(columnIndex);
                return intValue == null ? null : valueOf(Numbers.toInt(intValue));
            } else {
                return valueOf(rs.getString(columnIndex));
            }
        } else if (jsonValueEnumMap != null) {
            // The reverse map is keyed by the value's stringOf form, which is what getString yields for a numeric column too.
            return valueOf(rs.getString(columnIndex));
        } else {
            return super.get(rs, columnIndex);
        }
    }

    /**
     * Retrieves an enum value from a {@link ResultSet} using the specified column label.
     * The retrieval method depends on the {@code enumRepresentation} setting:
     * <ul>
     *   <li>ORDINAL or CODE: reads the column as an integer and maps it to the enum constant</li>
     *   <li>NAME: reads the column as a string and maps it to the enum constant by name</li>
     *   <li>annotated value member: reads the column as the value type and maps it through the creator, or
     *       through the reverse map of the constants' values when the enum has no creator</li>
     * </ul>
     *
     * @param rs         the {@link ResultSet} containing the data
     * @param columnName the label of the column containing the enum value
     * @return the enum value in the specified column, or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is null when this method or the selected value type accesses the JDBC resource
     * @throws SQLException if a database access error occurs or the column label is not found
     * @throws NumberFormatException if an ORDINAL or CODE column value cannot be parsed as an integer
     * @throws ArithmeticException if an ORDINAL or CODE column value cannot be represented exactly as an int
     * @throws IllegalArgumentException if the stored name, ordinal, code or annotated value has no matching enum constant
     */
    @Override
    public T get(final ResultSet rs, final String columnName)
            throws NullPointerException, SQLException, NumberFormatException, ArithmeticException, IllegalArgumentException {
        if (jsonValueType == null) {
            if (enumRepresentation == com.landawn.abacus.util.EnumType.ORDINAL || enumRepresentation == com.landawn.abacus.util.EnumType.CODE) {
                final Object intValue = rs.getObject(columnName);
                return intValue == null ? null : valueOf(Numbers.toInt(intValue));
            } else {
                return valueOf(rs.getString(columnName));
            }
        } else if (jsonValueEnumMap != null) {
            return valueOf(rs.getString(columnName));
        } else {
            return super.get(rs, columnName);
        }
    }

    /**
     * Sets an enum value as a parameter in a {@link PreparedStatement}.
     * The storage method depends on the {@code enumRepresentation} setting:
     * <ul>
     *   <li>ORDINAL or CODE: stores as integer (ordinal or code value); SQL {@code NULL} when {@code x} is {@code null}</li>
     *   <li>NAME: stores as string (enum constant name)</li>
     * </ul>
     *
     * @param stmt        the {@link PreparedStatement} in which to set the parameter
     * @param columnIndex the 1-based parameter index
     * @param x           the enum value to set; may be {@code null}
     * @throws NullPointerException if {@code stmt} is null when this method or the selected value type accesses the JDBC resource
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final T x) throws NullPointerException, SQLException {
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
     * Sets an enum value as a named parameter in a {@link CallableStatement}.
     * The storage method depends on the {@code enumRepresentation} setting:
     * <ul>
     *   <li>ORDINAL or CODE: stores as integer (ordinal or code value); SQL {@code NULL} when {@code x} is {@code null}</li>
     *   <li>NAME: stores as string (enum constant name)</li>
     * </ul>
     *
     * @param stmt          the {@link CallableStatement} in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x             the enum value to set; may be {@code null}
     * @throws NullPointerException if {@code stmt} is null when this method or the selected value type accesses the JDBC resource
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final T x) throws NullPointerException, SQLException {
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
     * Writes an enum value to a {@link CharacterWriter} with the specified serialization configuration.
     * The output format depends on the {@code enumRepresentation} setting:
     * <ul>
     *   <li>ORDINAL or CODE: writes the ordinal or code value as an unquoted integer</li>
     *   <li>NAME: writes the JSON/XML field name, optionally quoted based on {@code config}</li>
     * </ul>
     * A {@code null} value writes the literal {@code null}.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes the serialized form of {@code x} to the
     * {@code CharacterWriter}, applying string quotation and character escaping according to the supplied serialization
     * config (a {@code null} config means no surrounding quotation). It is the streaming counterpart of {@code stringOf}
     * and is invoked by the JSON/XML serializers.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML (quoted and escaped),
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the {@link CharacterWriter} to write to
     * @param x      the enum value to write; may be {@code null}
     * @param config the serialization configuration for quotation settings; may be {@code null}
     * @throws IOException if writing the enum name, ordinal, code, annotated value, quotation marks or null literal to {@code writer}
     *         fails
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final T x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            if (jsonValueType == null) {
                if (enumRepresentation == com.landawn.abacus.util.EnumType.ORDINAL || enumRepresentation == com.landawn.abacus.util.EnumType.CODE) {
                    writer.writeInt(numberEnum.getByValue(x).intValue());
                } else {
                    final char ch = config == null ? 0 : config.getStringQuotation();

                    if (ch == 0) {
                        Utils.writeStringContent(writer, enumJsonXmlNameMap.get(x), ch);
                    } else {
                        writer.write(ch);
                        Utils.writeStringContent(writer, enumJsonXmlNameMap.get(x), ch);
                        writer.write(ch);
                    }
                }
            } else {
                super.serializeTo(writer, x, config);
            }
        }
    }

    /**
     * Builds the handler name from the RESOLVED enum class: a constant body class ({@code E$1}) must not leak its
     * synthetic binary name into the type name, or two handlers for the same enum compare unequal.
     */
    private static String enumTypeName(final String className, final com.landawn.abacus.util.EnumType enumRepresentation) {
        final Class<?> requested = ClassUtil.forName(className);
        final Class<?> enumClass = getEnumClass(requested);
        final String baseName = enumClass == requested ? className : ClassUtil.getCanonicalClassName(enumClass);

        return baseName + "(" + (enumRepresentation == null ? com.landawn.abacus.util.EnumType.NAME : enumRepresentation).name() + ")";
    }

    private static Method getPublicIntMethod(final Class<?> enumClass, final String methodName) {
        try {
            final Method method = enumClass.getMethod(methodName);
            return int.class.equals(method.getReturnType()) ? method : null;
        } catch (final NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Resolves the JSON/XML field name for the given enum constant.
     * Checks for {@link com.landawn.abacus.annotation.JsonXmlField}, then
     * {@code com.alibaba.fastjson2.annotation.JSONField}, then
     * {@code com.fasterxml.jackson.annotation.JsonProperty}. If none of these annotations
     * are present or their name is empty, the enum constant's natural name is returned.
     *
     * @param enumConstant the enum constant whose serialization name to resolve
     * @return the JSON/XML name for the enum constant (never {@code null})
     */
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

    private void registerJsonXmlNames(final T enumConstant, final String jsonXmlName) {
        enumJsonXmlNameMap.put(enumConstant, jsonXmlName);
        registerJsonXmlName(jsonXmlName, enumConstant);
        registerJsonXmlName(enumConstant.name(), enumConstant);
    }

    /**
     * @throws IllegalArgumentException if another constant in the enum already uses the supplied JSON/XML name
     */
    private void registerJsonXmlName(final String name, final T enumConstant) throws IllegalArgumentException {
        final T previous = jsonXmlNameEnumMap.putIfAbsent(name, enumConstant);

        if (previous != null && previous != enumConstant) {
            throw new IllegalArgumentException("Duplicate JSON/XML name '" + name + "' in enum class " + ClassUtil.getCanonicalClassName(typeClass) + ": "
                    + previous.name() + " and " + enumConstant.name());
        }
    }

    /**
     * Returns the enum class for the given class.
     * If the class is itself an enum, it is returned directly. If the class is an inner class
     * whose enclosing class is an enum (e.g., an anonymous subclass of an enum constant),
     * the enclosing class is returned. Otherwise an exception is thrown.
     *
     * @param clazz the class to resolve as an enum class
     * @return the enum class
     * @throws IllegalArgumentException if {@code clazz} is not an enum and has no enclosing enum class.
     */
    private static Class<?> getEnumClass(final Class<?> clazz) throws IllegalArgumentException {
        if (clazz.isEnum()) {
            return clazz;
        }

        final Class<?> enclosing = clazz.getEnclosingClass();

        if (enclosing != null && enclosing.isEnum()) {
            return enclosing;
        }

        throw new IllegalArgumentException("Not an enum class: " + clazz);
    }
}
