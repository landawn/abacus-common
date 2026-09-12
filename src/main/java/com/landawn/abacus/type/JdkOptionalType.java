/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SK;

/**
 * Type handler for {@link java.util.Optional} with a generic type parameter.
 * This class provides serialization, deserialization, and database access capabilities for
 * {@code Optional} instances. An {@code Optional} is a container that may or may not contain
 * a {@code non-null} value. Empty optionals are serialized as {@code null} unless the element handler maps
 * {@code null} differently under the serialization config (see
 * {@link #serializeTo(CharacterWriter, Optional, JsonXmlSerConfig)}); a {@code non-null} string is parsed
 * by the element type and the result wrapped in {@code Optional.ofNullable}.
 *
 * <p>{@link #stringOf(Optional)}, {@link #valueOf(String)}, {@link #appendTo(Appendable, Optional)} and
 * {@link #serializeTo(CharacterWriter, Optional, JsonXmlSerConfig)} all use the declared
 * {@linkplain #elementType() element type}, so a registered single-value subtype is formatted as its declared base
 * type. {@code appendTo} and {@code serializeTo} both fall back to the value's runtime class when the declared element
 * type is {@code Object}. {@code serializeTo} also writes a structured element - one whose handler is not
 * {@linkplain Type#isSerializable() serializable}, such as a bean, a map or a {@code List<Object>} - as embedded JSON,
 * and the JSON serializer takes that element's shape from its runtime class exactly as it does for a bare property of
 * the same declared type.</p>
 *
 * @param <T> the type of value that may be present in the Optional
 */
@SuppressWarnings("java:S2160")
public class JdkOptionalType<T> extends AbstractOptionalType<Optional<T>> {

    /** The type name constant for {@link Optional} (JDK) type identification. */
    public static final String OPTIONAL = "JdkOptional";

    private final String declaringName;

    private final List<Type<?>> parameterTypes;

    private final Type<T> elementType;

    /**
     * Constructor for JdkOptionalType.
     * This constructor is called by the TypeFactory to create {@code JdkOptional<T>} type instances.
     *
     * @param parameterTypeName the name of the element type parameter
     * @throws IllegalArgumentException if a supplied type name is {@code null}, blank, or structurally invalid.
     */
    protected JdkOptionalType(final String parameterTypeName) throws IllegalArgumentException {
        super(OPTIONAL + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + SK.GREATER_THAN);

        declaringName = OPTIONAL + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + SK.GREATER_THAN;
        elementType = TypeFactory.getType(parameterTypeName);
        parameterTypes = List.of(elementType);
    }

    /**
     * Returns the declaring name of this optional type.
     * The declaring name represents the type in a simplified format suitable for type declarations.
     *
     * @return the declaring name of this type (e.g., "JdkOptional&lt;String&gt;")
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code Optional.class}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Class<Optional<T>> javaType() {
        return (Class) Optional.class;
    }

    /**
     * Returns the type handler for the element that may be contained in the Optional.
     *
     * @return the Type instance representing the element type of this Optional
     */
    @Override
    public Type<T> elementType() {
        return elementType;
    }

    /**
     * Returns an immutable list containing the parameter types of this generic optional type.
     * For optional types, this list contains a single element representing the value type.
     *
     * @return an immutable list containing the value type as the only parameter type
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type is a parameterized type.
     * {@code JdkOptionalType} is always a parameterized type as it wraps another type parameter.
     *
     * @return {@code true}, indicating this is a parameterized type
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Returns the default value for Optional type, which is an empty Optional.
     *
     * @return Optional.empty()
     */
    @Override
    public Optional<T> defaultValue() {
        return Optional.<T> empty();
    }

    /**
     * Converts an Optional to its string representation.
     * If the optional is empty or {@code null}, returns {@code null}.
     * Otherwise, delegates to the declared {@linkplain #elementType() element type}.
     * This keeps the output symmetric with {@link #valueOf(String)}, which parses it
     * with that same type handler.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the Optional to convert to string
     * @return the string representation of the contained value, or {@code null} if empty or null
     * @throws RuntimeException if a contained value is incompatible with its declared type or its type handler fails to produce a string.
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Optional<T> x) throws RuntimeException {
        return (x == null || x.isEmpty()) ? null : elementType.stringOf(x.get());
    }

    /**
     * Parses a string representation into an Optional.
     * A {@code null} string returns an empty Optional. Non-null strings (including empty strings)
     * are parsed according to the element type and wrapped in an {@code Optional.ofNullable}.
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse; may be {@code null}
     * @return {@code Optional.empty()} if the string is {@code null}; otherwise
     *         {@code Optional.ofNullable} wrapping the parsed element value
     * @throws RuntimeException if the declared element type rejects the non-null input during conversion.
     * @see #valueOf(Object)
     * @see #stringOf(Optional)
     */
    @Override
    public Optional<T> valueOf(final String str) throws RuntimeException {
        return str == null ? (Optional<T>) Optional.empty() : Optional.ofNullable(elementType.valueOf(str));
    }

    /**
     * Retrieves an Optional value from the specified column in a ResultSet.
     * If the column value is {@code null}, returns an empty Optional.
     * Otherwise, converts the value to the appropriate type and wraps it in Optional.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the index of the column to read (1-based)
     * @return Optional.empty() if the column is {@code null}, otherwise Optional containing the converted value
     * @throws NullPointerException if {@code rs} is {@code null} and the selected type handler accesses it.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     * @throws RuntimeException if the declared element type cannot convert the column value.
     */
    @Override
    public Optional<T> get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException, RuntimeException {
        // Use the declared handler to retain nested generic metadata. Primitive JDBC getters return
        // default values for SQL NULL, so inspect wasNull before wrapping that value.
        final T result = elementType.get(rs, columnIndex);

        return result == null || rs.wasNull() ? (Optional<T>) Optional.empty()
                : Optional.of(elementType.javaType().isAssignableFrom(result.getClass()) ? result : N.convert(result, elementType));
    }

    /**
     * Retrieves an Optional value from the specified column in a ResultSet using the column label.
     * If the column value is {@code null}, returns an empty Optional.
     * Otherwise, converts the value to the appropriate type and wraps it in Optional.
     *
     * @param rs the ResultSet to read from
     * @param columnName the label of the column to read
     * @return Optional.empty() if the column is {@code null}, otherwise Optional containing the converted value
     * @throws NullPointerException if {@code rs} is {@code null} and the selected type handler accesses it.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     * @throws RuntimeException if the declared element type cannot convert the column value.
     */
    @Override
    public Optional<T> get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException, RuntimeException {
        // Use the declared handler to retain nested generic metadata. Primitive JDBC getters return
        // default values for SQL NULL, so inspect wasNull before wrapping that value.
        final T result = elementType.get(rs, columnName);

        return result == null || rs.wasNull() ? (Optional<T>) Optional.empty()
                : Optional.of(elementType.javaType().isAssignableFrom(result.getClass()) ? result : N.convert(result, elementType));
    }

    /**
     * Sets an Optional parameter in a PreparedStatement.
     * If the Optional is {@code null} or empty, sets the parameter to SQL NULL.
     * Otherwise, binds the contained value using its declared element type.
     *
     * <p>The declared element handler performs the binding, including its null mapping and JDBC representation.</p>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the index of the parameter to set (1-based)
     * @param x the Optional to set
     * @throws NullPointerException if {@code stmt} is {@code null} and the selected type handler accesses it.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     * @throws RuntimeException if the declared element type rejects or cannot convert the contained value for JDBC binding.
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Optional<T> x) throws NullPointerException, SQLException, RuntimeException {
        elementType.set(stmt, columnIndex, (x == null || x.isEmpty()) ? null : x.get()); //NOSONAR
    }

    /**
     * Sets an Optional parameter in a CallableStatement using a parameter name.
     * If the Optional is {@code null} or empty, sets the parameter to SQL NULL.
     * Otherwise, binds the contained value using its declared element type.
     *
     * <p>The declared element handler performs the binding, including its null mapping and JDBC representation.</p>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Optional to set
     * @throws NullPointerException if {@code stmt} is {@code null} and the selected type handler accesses it.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     * @throws RuntimeException if the declared element type rejects or cannot convert the contained value for JDBC binding.
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Optional<T> x) throws NullPointerException, SQLException, RuntimeException {
        elementType.set(stmt, parameterName, (x == null || x.isEmpty()) ? null : x.get()); //NOSONAR
    }

    /**
     * Appends the string representation of an Optional to an Appendable.
     * Empty optionals are written as "null".
     * Present values are appended by the declared element type handler; when the declared element type is
     * {@code Object} the handler of the value's runtime class is used instead, exactly as
     * {@link #serializeTo(CharacterWriter, Optional, JsonXmlSerConfig)} does, so a map, collection or bean element
     * keeps the {@code toString()}-style form rather than falling back to {@code ObjectType}'s JSON {@code stringOf}.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} delegates to the declared element type's plain append contract,
     * whereas {@code serializeTo} delegates to the element type's serialization contract.
     *
     * @param appendable the Appendable to write to
     * @param x the Optional to append
     * @throws NullPointerException if {@code appendable} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     * @throws RuntimeException if a contained value is incompatible with its declared type or its selected type handler fails while writing it.
     * @implNote
     * This method appends a string representation of {@code x} to {@code appendable} (the literal {@code "null"} for a
     * {@code null} value). Conceptually this is the human-readable form produced by {@code toString()}, <i>not</i> the
     * value returned by {@code stringOf}, which is a formatted, serializable representation (typically a JSON string)
     * that {@link #valueOf(String)} can convert back into an equivalent value. For values whose nested structure makes
     * the two forms differ (collections, maps, arrays), {@code appendTo} emits the unquoted, {@code toString()}-style
     * form; it is therefore not, in the general contract, a plain
     * {@code appendable.append(x == null ? NULL_STRING : stringOf(x))}. (For value types whose human-readable and
     * serialized forms coincide, the appended text is naturally identical to {@code stringOf(x)}.)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void appendTo(final Appendable appendable, final Optional<T> x) throws NullPointerException, IOException, RuntimeException {
        if (x == null || x.isEmpty()) { //NOSONAR
            appendable.append(NULL_STRING);
        } else {
            final Object value = x.get();
            // An Object slot has no usable declared handler (ObjectType has no appendTo of its own, so it falls back
            // to stringOf, i.e. the JSON form); dispatch on the runtime class the way serializeTo does.
            final Type type = elementType.isObject() ? TypeFactory.getType(value.getClass()) : elementType;

            type.appendTo(appendable, value);
        }
    }

    /**
     * Writes the serialized representation of an {@link Optional} to a {@link CharacterWriter}.
     * <p>
     * A {@code null} or empty optional is written by the declared element type handler as a {@code null} value, so
     * that handler's null-substitution flags apply: {@code Optional<Integer>} honours
     * {@code config.isWriteNullNumberAsZero()} (written as {@code 0}), {@code Optional<Boolean>} honours
     * {@code writeNullBooleanAsFalse} ({@code false}) and {@code Optional<String>} honours
     * {@code writeNullStringAsEmpty} ({@code ""}); without such a flag the literal {@code null} is written. A substituted
     * value reads back as a <i>present</i> optional. The XML serializers represent an empty optional property with the
     * {@code isNull="true"} attribute form rather than with the text written here.
     * <p>
     * A present value is written by the declared element type handler. When the declared element type is
     * {@code Object} the handler of the value's runtime class is used instead, so {@code Optional.of(1)} inside a
     * {@code List<Object>} is written as {@code 1}, not {@code "1"}. A value whose handler is not
     * {@linkplain Type#isSerializable() serializable} - a bean, a map, a {@code List<Object>} - is written as embedded
     * JSON when {@code config} is a {@code JsonSerConfig}, and as its escaped {@code stringOf} text under any other
     * config. Any string quotation or character escaping is performed by the element type handler according to the
     * supplied serialization config.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} delegates to the element type's serialization contract,
     * whereas {@code appendTo} delegates to the element type's plain append contract.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Optional to write
     * @param config the serialization configuration to use, may be {@code null}
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     * @throws RuntimeException if a contained value is incompatible with its declared type or its selected type handler fails while writing it.
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final Optional<T> x, final JsonXmlSerConfig<?> config)
            throws NullPointerException, IOException, RuntimeException {
        AbstractTupleType.serializeSlot(writer, elementType, (x == null || x.isEmpty()) ? null : x.get(), config); //NOSONAR
    }
}
