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
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SK;

/**
 * Type handler for {@link Holder} wrapper objects, providing serialization, deserialization, and
 * database interaction for held values of any type.
 * <p>
 * A {@code Holder} is a plain mutable single-value container, not an optional wrapper; the JDK's
 * {@code java.util.Optional<T>} is handled by {@link JdkOptionalType} instead.
 * <p>
 * Unlike {@link com.landawn.abacus.util.u.Optional Optional} (which disallows {@code null} values),
 * a {@code Holder} may hold {@code null} as its current value. SQL {@code NULL} columns are
 * mapped to a {@code null}-valued {@code Holder} on read, and a {@code null}-valued
 * {@code Holder} maps to SQL {@code NULL} on write.
 * <p>
 * This type handler supports generic type parameters of the form {@code Holder<T>}
 * and delegates element serialization/deserialization to the appropriate element type handler.
 *
 * @param <T> the type of value wrapped by the {@code Holder}
 */
@SuppressWarnings("java:S2160")
public class HolderType<T> extends AbstractType<Holder<T>> {

    /** The type name constant for Holder type identification, equal to {@code "Holder"}. */
    public static final String HOLDER = Holder.class.getSimpleName();

    private final String declaringName;

    private final List<Type<?>> parameterTypes;

    private final Type<T> elementType;

    /**
     * Constructs a new HolderType with the specified parameter type.
     * This constructor is protected to allow subclassing while maintaining controlled instantiation
     * through the TypeFactory.
     *
     * @param parameterTypeName the name of the type parameter for the Holder (e.g., "String", "Integer")
     * @throws IllegalArgumentException if a supplied type name is {@code null}, blank, or structurally invalid.
     */
    protected HolderType(final String parameterTypeName) throws IllegalArgumentException {
        super(HOLDER + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + SK.GREATER_THAN);

        declaringName = HOLDER + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + SK.GREATER_THAN;
        elementType = TypeFactory.getType(parameterTypeName);
        parameterTypes = List.of(elementType);
    }

    /**
     * Returns the declaring name of this type, which includes the full generic type declaration.
     * For example, "Holder&lt;String&gt;" for a Holder containing String values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Holder<String>> type = TypeFactory.getType("Holder<String>");
     * String name = type.declaringName();   // Returns "Holder<String>"
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
     * @return the {@link Holder} class object
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Class<Holder<T>> javaType() {
        return (Class) Holder.class;
    }

    /**
     * Gets the type handler for the element type contained within the Holder.
     *
     * @return the Type handler for the wrapped element type
     */
    @Override
    public Type<T> elementType() {
        return elementType;
    }

    /**
     * Gets the immutable list of parameter types for this generic type.
     * For Holder, this returns a single-element list containing the element type.
     *
     * @return an immutable list containing the element type
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type is a parameterized type.
     * HolderType is always parameterized as it wraps a value of type T.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Holder<String>> type = TypeFactory.getType("Holder<String>");
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
     * Returns the default value for Holder type, which is a new {@link Holder}
     * holding a {@code null} value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Holder<String>> type = TypeFactory.getType("Holder<String>");
     * Holder<String> defaultVal = type.defaultValue();   // Returns a new Holder holding null
     * }</pre>
     *
     * @return a new {@link Holder} holding {@code null}
     */
    @Override
    public Holder<T> defaultValue() {
        return new Holder<>();
    }

    /**
     * Converts a {@link Holder} object to its string representation.
     * If the Holder is {@code null} or holds a {@code null} value, returns {@code null}. Otherwise,
     * delegates to the element type's string conversion.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Holder<String>> type = TypeFactory.getType("Holder<String>");
     * Holder<String> opt = Holder.of("hello");
     * String str = type.stringOf(opt);   // Returns "hello"
     *
     * Holder<String> nullHolder = Holder.of(null);
     * String str2 = type.stringOf(nullHolder);   // Returns null
     * }</pre>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the Holder object to convert; may be {@code null}
     * @return the string representation of the contained value,
     *         or {@code null} if {@code x} is {@code null} or holds a {@code null} value
     * @throws RuntimeException if a contained value is incompatible with its declared type or its type handler fails to produce a string.
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Holder<T> x) throws RuntimeException {
        return (x == null || x.isNull()) ? null : elementType.stringOf(x.value());
    }

    /**
     * Converts a string representation to a {@link Holder} object.
     * If the string is {@code null}, returns a new {@link Holder} holding {@code null}. Otherwise,
     * delegates to the element type's valueOf method and wraps the result.
     * The result may be a Holder containing {@code null} if the element type's
     * valueOf returns {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Holder<Integer>> type = TypeFactory.getType("Holder<Integer>");
     * Holder<Integer> opt = type.valueOf("42");          // Returns Holder.of(42)
     *
     * Holder<Integer> nullHolder = type.valueOf(null);   // Returns a new Holder holding null
     * }</pre>
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to convert; may be {@code null}
     * @return a Holder containing the parsed value, or a Holder holding {@code null} if {@code str} is {@code null}
     * @throws RuntimeException if the declared element type rejects the non-null input during conversion.
     * @see #valueOf(Object)
     * @see #stringOf(Holder)
     */
    @Override
    public Holder<T> valueOf(final String str) throws RuntimeException {
        return str == null ? new Holder<>() : Holder.of(elementType.valueOf(str));
    }

    /**
     * Retrieves a value from a ResultSet at the specified column index and wraps it in a {@link Holder}.
     * The method attempts to convert the retrieved value to the element type if necessary.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Holder<String>> type = TypeFactory.getType("Holder<String>");
     * try (ResultSet rs = stmt.executeQuery()) {
     *     if (rs.next()) {
     *         Holder<String> name = type.get(rs, 1);
     *         if (!name.isNull()) {
     *             System.out.println("Name: " + name.value());
     *         }
     *     }
     * }
     * }</pre>
     *
     * @param rs the {@link ResultSet} to read from
     * @param columnIndex the 1-based column index to retrieve the value from
     * @return a {@code Holder} containing the retrieved value, or a {@code Holder} holding {@code null} if the value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is {@code null} and the selected type handler accesses it.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     * @throws RuntimeException if the declared element type cannot convert the column value.
     */
    @Override
    public Holder<T> get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException, RuntimeException {
        // Use the declared handler to retain nested generic metadata. Primitive JDBC getters return
        // default values for SQL NULL, so inspect wasNull before wrapping that value.
        final T result = elementType.get(rs, columnIndex);

        return result == null || rs.wasNull() ? new Holder<>()
                : Holder.of(elementType.javaType().isAssignableFrom(result.getClass()) ? result : N.convert(result, elementType));
    }

    /**
     * Retrieves a value from a ResultSet using the specified column label and wraps it in a {@link Holder}.
     * The method attempts to convert the retrieved value to the element type if necessary.
     *
     * @param rs the ResultSet to read from
     * @param columnName the label for the column specified with the SQL AS clause
     * @return a Holder containing the retrieved value, or a Holder holding {@code null} if the value is SQL NULL
     * @throws NullPointerException if {@code rs} is {@code null} and the selected type handler accesses it.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     * @throws RuntimeException if the declared element type cannot convert the column value.
     */
    @Override
    public Holder<T> get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException, RuntimeException {
        // Use the declared handler to retain nested generic metadata. Primitive JDBC getters return
        // default values for SQL NULL, so inspect wasNull before wrapping that value.
        final T result = elementType.get(rs, columnName);

        return result == null || rs.wasNull() ? new Holder<>()
                : Holder.of(elementType.javaType().isAssignableFrom(result.getClass()) ? result : N.convert(result, elementType));
    }

    /**
     * Sets a parameter in a PreparedStatement to the value contained in a {@link Holder}.
     * If the Holder is {@code null} or holds a {@code null} value, sets the parameter to SQL NULL.
     *
     * <p>The declared element handler performs the binding, including its null mapping and JDBC representation.</p>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the Holder value to set
     * @throws NullPointerException if {@code stmt} is {@code null} and the selected type handler accesses it.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     * @throws RuntimeException if the declared element type rejects or cannot convert the contained value for JDBC binding.
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Holder<T> x) throws NullPointerException, SQLException, RuntimeException {
        elementType.set(stmt, columnIndex, (x == null || x.isNull()) ? null : x.value());
    }

    /**
     * Sets a named parameter in a CallableStatement to the value contained in a {@link Holder}.
     * If the Holder is {@code null} or holds a {@code null} value, sets the parameter to SQL NULL.
     *
     * <p>The declared element handler performs the binding, including its null mapping and JDBC representation.</p>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Holder value to set
     * @throws NullPointerException if {@code stmt} is {@code null} and the selected type handler accesses it.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     * @throws RuntimeException if the declared element type rejects or cannot convert the contained value for JDBC binding.
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Holder<T> x) throws NullPointerException, SQLException, RuntimeException {
        elementType.set(stmt, parameterName, (x == null || x.isNull()) ? null : x.value());
    }

    /**
     * Appends the string representation of a {@link Holder} to an Appendable.
     * If the Holder is {@code null} or holds a {@code null} value, appends the {@code NULL_STRING} constant.
     * Otherwise, delegates to the declared element type handler - the handler for the {@code T} of
     * {@code Holder<T>}, not the handler of the contained value's runtime class; when the declared element type is
     * {@code Object} the handler of the value's runtime class is used instead, exactly as
     * {@link #serializeTo(CharacterWriter, Holder, JsonXmlSerConfig)} does, so a map, collection or bean value
     * keeps the {@code toString()}-style form rather than falling back to {@code ObjectType}'s JSON {@code stringOf}.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} delegates to the declared element type's plain append
     * contract, whereas {@code serializeTo} delegates to its serialization contract.
     *
     * @param appendable the Appendable to write to
     * @param x the Holder value to append
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
    public void appendTo(final Appendable appendable, final Holder<T> x) throws NullPointerException, IOException, RuntimeException {
        if (x == null || x.isNull()) {
            appendable.append(NULL_STRING);
        } else {
            final Object value = x.value();
            // An Object slot has no usable declared handler (ObjectType has no appendTo of its own, so it falls back
            // to stringOf, i.e. the JSON form); dispatch on the runtime class the way serializeTo does.
            final Type type = elementType.isObject() ? TypeFactory.getType(value.getClass()) : elementType;

            type.appendTo(appendable, value);
        }
    }

    /**
     * Writes the serialized representation of a {@link Holder} to a {@link CharacterWriter}.
     * <p>
     * A {@code null} Holder, and one holding a {@code null} value, are both written by the declared element type
     * handler, so that handler's null-substitution flags apply: {@code Holder<Integer>} honours
     * {@code config.isWriteNullNumberAsZero()} (written as {@code 0}), {@code Holder<Boolean>} honours
     * {@code writeNullBooleanAsFalse} ({@code false}) and {@code Holder<String>} honours
     * {@code writeNullStringAsEmpty} ({@code ""}); without such a flag the literal {@code null} is written.
     * <p>
     * A non-{@code null} value is written by the declared element type handler - the handler for the {@code T} of
     * {@code Holder<T>}, not the handler of the value's runtime class. When the declared element type is
     * {@code Object} the handler of the value's runtime class is used instead, so {@code Holder.of(1)} inside a
     * {@code List<Object>} is written as {@code 1}, not {@code "1"}. A value whose handler is not
     * {@linkplain Type#isSerializable() serializable} - a bean, a map, a {@code List<Object>} - is written as embedded
     * JSON when {@code config} is a {@code JsonSerConfig}, and as its escaped {@code stringOf} text under any other
     * config. Any string quotation or character escaping is performed by the delegated handler according to the
     * supplied serialization config.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML using the declared
     * element type's serializer, whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style
     * rendering.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Holder value to write, may be {@code null}
     * @param config the serialization configuration, may be {@code null}
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     * @throws RuntimeException if a contained value is incompatible with its declared type or its selected type handler fails while writing it.
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final Holder<T> x, final JsonXmlSerConfig<?> config)
            throws NullPointerException, IOException, RuntimeException {
        AbstractTupleType.serializeSlot(writer, elementType, x == null ? null : x.value(), config);
    }
}
