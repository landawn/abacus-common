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
 * a non-null value. Empty optionals are serialized as {@code null}; a non-null string is parsed
 * by the element type and the result wrapped in {@code Optional.ofNullable}.
 *
 * <p>Serialization uses the runtime type of the contained value for accuracy, rather than
 * the declared element type.</p>
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
     */
    protected JdkOptionalType(final String parameterTypeName) {
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
     * Otherwise, returns the string representation of the contained value.
     * Uses the runtime type of the value for accurate serialization.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the Optional to convert to string
     * @return the string representation of the contained value, or {@code null} if empty or null
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Optional<T> x) {
        return (x == null || x.isEmpty()) ? null : N.stringOf(x.get()); // elementType.stringOf(x.get());   //NOSONAR
    }

    /**
     * Parses a string representation into an Optional.
     * A {@code null} string returns an empty Optional. Non-null strings (including empty strings)
     * are parsed according to the element type and wrapped in an {@code Optional.ofNullable}.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse; may be {@code null}
     * @return {@code Optional.empty()} if the string is {@code null}; otherwise
     *         {@code Optional.ofNullable} wrapping the parsed element value
     * @see #valueOf(Object)
     * @see #stringOf(Optional)
     */
    @Override
    public Optional<T> valueOf(final String str) {
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
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Optional<T> get(final ResultSet rs, final int columnIndex) throws SQLException {
        final T result = getColumnValue(rs, columnIndex, elementType.javaType());

        return result == null ? (Optional<T>) Optional.empty()
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
     * @throws SQLException if a database access error occurs or the columnName is not found
     */
    @Override
    public Optional<T> get(final ResultSet rs, final String columnName) throws SQLException {
        final T result = getColumnValue(rs, columnName, elementType.javaType());

        return result == null ? (Optional<T>) Optional.empty()
                : Optional.of(elementType.javaType().isAssignableFrom(result.getClass()) ? result : N.convert(result, elementType));
    }

    /**
     * Sets an Optional parameter in a PreparedStatement.
     * If the Optional is {@code null} or empty, sets the parameter to SQL NULL.
     * Otherwise, sets the contained value as an object parameter.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the index of the parameter to set (1-based)
     * @param x the Optional to set
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Optional<T> x) throws SQLException {
        stmt.setObject(columnIndex, (x == null || x.isEmpty()) ? null : x.get()); //NOSONAR
    }

    /**
     * Sets an Optional parameter in a CallableStatement using a parameter name.
     * If the Optional is {@code null} or empty, sets the parameter to SQL NULL.
     * Otherwise, sets the contained value as an object parameter.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Optional to set
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Optional<T> x) throws SQLException {
        stmt.setObject(parameterName, (x == null || x.isEmpty()) ? null : x.get()); //NOSONAR
    }

    /**
     * Appends the string representation of an Optional to an Appendable.
     * Empty optionals are written as "null".
     * Present values are appended by the runtime type handler for the contained value.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} delegates to the contained value's plain append contract,
     * whereas {@code serializeTo} delegates to the contained value's serialization contract.
     *
     * @param appendable the Appendable to write to
     * @param x the Optional to append
     * @throws IOException if an I/O error occurs during writing
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
    @Override
    public void appendTo(final Appendable appendable, final Optional<T> x) throws IOException {
        if (x == null || x.isEmpty()) { //NOSONAR
            appendable.append(NULL_STRING);
        } else {
            // elementType.write(writer, x.get());
            Type.<Object> of(x.get().getClass()).appendTo(appendable, x.get());
        }
    }

    /**
     * Writes the serialized representation of an {@link Optional} to a {@link CharacterWriter}.
     * Empty optionals are written as {@code null}. Present values are serialized by the runtime
     * type handler for the contained value.
     * <p>
     * Any string quotation or character escaping is performed by the delegated value type handler according to the
     * supplied serialization config.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} delegates to the contained value's serialization contract,
     * whereas {@code appendTo} delegates to the contained value's plain append contract.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Optional to write
     * @param config the serialization configuration to use
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final Optional<T> x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) { //NOSONAR
            writer.write(NULL_CHAR_ARRAY);
        } else {
            // elementType.serializeTo(writer, x.get(), config);
            Type.<Object> of(x.get().getClass()).serializeTo(writer, x.get(), config);
        }
    }
}
