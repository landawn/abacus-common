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

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.u.Optional;

/**
 * Generic type handler for {@link Optional} wrapper objects (a nested class of the
 * {@code com.landawn.abacus.util.u} utility class), providing serialization, deserialization,
 * and database interaction capabilities for optional values of any type.
 * <p>
 * Note: this handles the abacus-specific {@code Optional<T>}, not {@code java.util.Optional<T>}
 * (which is handled by {@code JdkOptionalType}).
 * <p>
 * Unlike {@link com.landawn.abacus.util.u.Nullable Nullable} (handled by {@link NullableType}),
 * an {@code Optional} cannot carry {@code null} as a present value: an absent value and a
 * {@code null} value are conflated as {@link Optional#empty()}. SQL {@code NULL} columns are
 * mapped to {@code Optional.empty()} on read, and {@code Optional.empty()} maps to SQL
 * {@code NULL} on write.
 * <p>
 * This type handler supports generic type parameters of the form {@code Optional<T>}.
 * {@link #stringOf(Optional)}, {@link #valueOf(String)}, {@link #appendTo(Appendable, Optional)} and
 * {@link #serializeTo(CharacterWriter, Optional, JsonXmlSerConfig)} all use the declared
 * {@linkplain #elementType() element type}, so a registered single-value subtype is formatted as its declared base
 * type. {@code appendTo} and {@code serializeTo} both fall back to the value's runtime class when the declared element
 * type is {@code Object}. {@code serializeTo} also writes a structured element - one whose handler is not
 * {@linkplain Type#isSerializable() serializable}, such as a bean, a map or a {@code List<Object>} - as embedded JSON,
 * and the JSON serializer takes that element's shape from its runtime class exactly as it does for a bare property of
 * the same declared type.
 *
 * @param <T> the type of value wrapped by the {@code Optional}
 */
@SuppressWarnings("java:S2160")
public class OptionalType<T> extends AbstractOptionalType<Optional<T>> {

    /** The type name constant for Optional type identification, equal to {@code "Optional"}. */
    public static final String OPTIONAL = Optional.class.getSimpleName();

    private final String declaringName;

    private final List<Type<?>> parameterTypes;

    private final Type<T> elementType;

    /**
     * Constructs a new OptionalType with the specified parameter type.
     * This constructor is protected to allow subclassing while maintaining controlled instantiation
     * through the TypeFactory.
     *
     * @param parameterTypeName the name of the type parameter for the Optional (e.g., "String", "Integer")
     * @throws IllegalArgumentException if a supplied type name is {@code null}, blank, or structurally invalid.
     */
    protected OptionalType(final String parameterTypeName) throws IllegalArgumentException {
        super(OPTIONAL + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + SK.GREATER_THAN);

        declaringName = OPTIONAL + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + SK.GREATER_THAN;
        elementType = TypeFactory.getType(parameterTypeName);
        parameterTypes = List.of(elementType);
    }

    /**
     * Returns the declaring name of this type, which includes the full generic type declaration.
     * For example, "Optional&lt;String&gt;" for an Optional containing String values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Optional<String>> type = TypeFactory.getType("Optional<String>");
     * String name = type.declaringName();   // Returns "Optional<String>"
     * }</pre>
     *
     * @return the declaring name with generic type information
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * @return the {@link Optional} class object
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Class<Optional<T>> javaType() {
        return (Class) Optional.class;
    }

    /**
     * Gets the type handler for the element type contained within the Optional.
     *
     * @return the Type handler for the wrapped element type
     */
    @Override
    public Type<T> elementType() {
        return elementType;
    }

    /**
     * Gets the immutable list of parameter types for this generic type.
     * For Optional, this returns a single-element list containing the element type.
     *
     * @return an immutable list containing the element type
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type is a parameterized type.
     * {@code OptionalType} is always parameterized, as it is parameterized with an element type {@code T}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Optional<String>> type = TypeFactory.getType("Optional<String>");
     * boolean isParameterized = type.isParameterizedType();   // Returns true
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Optional<String>> type = TypeFactory.getType("Optional<String>");
     * Optional<String> defaultVal = type.defaultValue();   // Returns Optional.empty()
     * }</pre>
     *
     * @return Optional.empty()
     */
    @Override
    public Optional<T> defaultValue() {
        return Optional.<T> empty();
    }

    /**
     * Converts an {@link Optional} object to its string representation.
     * If the Optional is {@code null} or empty, returns {@code null}. Otherwise,
     * delegates to the declared {@linkplain #elementType() element type}. Using the
     * declared type keeps this method symmetric with {@link #valueOf(String)}, which
     * parses the result with that same type handler.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Optional<String>> type = TypeFactory.getType("Optional<String>");
     * Optional<String> opt = Optional.of("hello");
     * String str = type.stringOf(opt);   // Returns "hello"
     *
     * Optional<String> empty = Optional.empty();
     * String str2 = type.stringOf(empty);   // Returns null
     * }</pre>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the Optional object to convert
     * @return the string representation of the contained value, or {@code null} if {@code x} is {@code null} or empty
     * @throws RuntimeException if a contained value is incompatible with its declared type or its type handler fails to produce a string.
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Optional<T> x) throws RuntimeException {
        return (x == null || x.isEmpty()) ? null : elementType.stringOf(x.get());
    }

    /**
     * Converts a string representation to an {@link Optional} object.
     * If the string is {@code null}, returns an empty Optional. Otherwise,
     * delegates to the element type's {@code valueOf} method and wraps the result
     * via {@link Optional#ofNullable(Object)} &mdash; so a {@code null} return value
     * from the element type yields {@link Optional#empty()} rather than a present
     * {@code Optional} containing {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Optional<Integer>> type = TypeFactory.getType("Optional<Integer>");
     * Optional<Integer> opt = type.valueOf("42");     // Returns Optional.of(42)
     *
     * Optional<Integer> empty = type.valueOf(null);   // Returns Optional.empty()
     * }</pre>
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to convert
     * @return an Optional containing the parsed value, or an empty Optional if the input is {@code null} or the element type parses it to {@code null}
     * @throws RuntimeException if the declared element type rejects the non-null input during conversion.
     * @see #valueOf(Object)
     * @see #stringOf(Optional)
     */
    @Override
    public Optional<T> valueOf(final String str) throws RuntimeException {
        return str == null ? (Optional<T>) Optional.empty() : Optional.ofNullable(elementType.valueOf(str));
    }

    /**
     * Retrieves a value from a ResultSet at the specified column index and wraps it in an {@link Optional}.
     * The method attempts to convert the retrieved value to the element type if necessary.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Optional<String>> type = TypeFactory.getType("Optional<String>");
     * ResultSet rs = Mockito.mock(ResultSet.class);
     * Mockito.when(rs.getString(1)).thenReturn("hello");
     *
     * // Column 1 contains the value "hello"
     * Optional<String> name = type.get(rs, 1);
     * if (name.isPresent()) {
     *     System.out.println("Name: " + name.get());
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return an Optional containing the retrieved value, or empty Optional if the value is SQL NULL
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
     * Retrieves a value from a ResultSet using the specified column label and wraps it in an {@link Optional}.
     * The method attempts to convert the retrieved value to the element type if necessary.
     *
     * @param rs the ResultSet to read from
     * @param columnName the label for the column specified with the SQL AS clause
     * @return an Optional containing the retrieved value, or empty Optional if the value is SQL NULL
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
     * Sets a parameter in a PreparedStatement to the value contained in an {@link Optional}.
     * If the Optional is {@code null} or empty, sets the parameter to SQL NULL.
     *
     * <p>The declared element handler performs the binding, including its null mapping and JDBC representation.</p>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the Optional value to set
     * @throws NullPointerException if {@code stmt} is {@code null} and the selected type handler accesses it.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     * @throws RuntimeException if the declared element type rejects or cannot convert the contained value for JDBC binding.
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Optional<T> x) throws NullPointerException, SQLException, RuntimeException {
        elementType.set(stmt, columnIndex, (x == null || x.isEmpty()) ? null : x.get());
    }

    /**
     * Sets a named parameter in a CallableStatement to the value contained in an {@link Optional}.
     * If the Optional is {@code null} or empty, sets the parameter to SQL NULL.
     *
     * <p>The declared element handler performs the binding, including its null mapping and JDBC representation.</p>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Optional value to set
     * @throws NullPointerException if {@code stmt} is {@code null} and the selected type handler accesses it.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     * @throws RuntimeException if the declared element type rejects or cannot convert the contained value for JDBC binding.
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Optional<T> x) throws NullPointerException, SQLException, RuntimeException {
        elementType.set(stmt, parameterName, (x == null || x.isEmpty()) ? null : x.get());
    }

    /**
     * Appends the string representation of an {@link Optional} to an Appendable.
     * If the Optional is {@code null} or empty, appends the NULL_STRING constant.
     * Otherwise, delegates to the declared element type handler; when the declared element type is {@code Object} the
     * handler of the value's runtime class is used instead, exactly as
     * {@link #serializeTo(CharacterWriter, Optional, JsonXmlSerConfig)} does, so a map, collection or bean element
     * keeps the {@code toString()}-style form rather than falling back to {@code ObjectType}'s JSON {@code stringOf}.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} writes the JSON/XML
     * serialized form by delegating to the declared element type handler with the supplied config.
     *
     * @param appendable the Appendable to write to
     * @param x the Optional value to append
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
        if (x == null || x.isEmpty()) {
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
     * Writes the character representation of an {@link Optional} to a CharacterWriter.
     * This method is typically used for JSON/XML serialization.
     * <p>
     * A {@code null} or empty optional is written by the declared element type handler as a {@code null} value, so
     * that handler's null-substitution flags apply: {@code Optional<Integer>} honours
     * {@code config.isWriteNullNumberAsZero()} (written as {@code 0}), {@code Optional<Boolean>} honours
     * {@code writeNullBooleanAsFalse} ({@code false}) and {@code Optional<String>} honours
     * {@code writeNullStringAsEmpty} ({@code ""}); without such a flag the literal {@code null} is written. A substituted
     * value reads back as a <i>present</i> optional ({@code {"oi": 0}} parses to {@code Optional.of(0)}), which is what
     * those flags ask for. The XML serializers represent an empty optional property with the {@code isNull="true"}
     * attribute form rather than with the text written here.
     * <p>
     * A present value is written by the declared element type handler. When the declared element type is
     * {@code Object} (an optional inside a {@code List<Object>}, {@code Map<String, Object>} or {@code Object} field) the
     * handler of the value's runtime class is used instead, so {@code Optional.of(1)} is written as {@code 1}, not
     * {@code "1"}. A value whose handler is not {@linkplain Type#isSerializable() serializable} - a bean, a map, a
     * {@code List<Object>} - is written as embedded JSON ({@code {"k": 1}}, not the quoted string) when
     * {@code config} is a {@code JsonSerConfig}, and as its escaped {@code stringOf} text under any other config.
     * The output therefore matches what the JSON serializer writes for the bare value.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML using the element
     * type's serializer, whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Optional value to write
     * @param config the serialization configuration, may be {@code null}
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     * @throws RuntimeException if a contained value is incompatible with its declared type or its selected type handler fails while writing it.
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final Optional<T> x, final JsonXmlSerConfig<?> config)
            throws NullPointerException, IOException, RuntimeException {
        AbstractTupleType.serializeSlot(writer, elementType, (x == null || x.isEmpty()) ? null : x.get(), config);
    }
}
