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
 * Generic type handler for {@link Holder} wrapper objects from the {@code com.landawn.abacus.util}
 * package, providing serialization, deserialization, and database interaction capabilities for
 * Holder values of any type.
 * <p>
 * Note: this handles the abacus-specific {@code Holder<T>}, not {@code java.util.Optional<T>}
 * (which is handled by {@link JdkOptionalType}).
 * <p>
 * Unlike {@link com.landawn.abacus.util.u.Nullable Nullable} (handled by {@link NullableType}),
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
     */
    protected HolderType(final String parameterTypeName) {
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
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the Holder object to convert; may be {@code null}
     * @return the string representation of the contained value,
     *         or {@code null} if {@code x} is {@code null} or holds a {@code null} value
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Holder<T> x) {
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
     * Holder<Integer> opt = type.valueOf("42");   // Returns Holder.of(42)
     *
     * Holder<Integer> nullHolder = type.valueOf(null);   // Returns a new Holder holding null
     * }</pre>
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to convert; may be {@code null}
     * @return a Holder containing the parsed value, or a Holder holding {@code null} if {@code str} is {@code null}
     * @see #valueOf(Object)
     * @see #stringOf(Holder)
     */
    @Override
    public Holder<T> valueOf(final String str) {
        return str == null ? new Holder<>() : Holder.of(elementType.valueOf(str));
    }

    /**
     * Retrieves a value from a ResultSet at the specified column index and wraps it in a {@link Holder}.
     * The method attempts to convert the retrieved value to the element type if necessary.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Holder<String>> type = TypeFactory.getType("Holder<String>");
     * PreparedStatement stmt = org.mockito.Mockito.mock(PreparedStatement.class);
     * stmt.setInt(1, 123);
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
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return a Holder containing the retrieved value, or a Holder holding {@code null} if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Holder<T> get(final ResultSet rs, final int columnIndex) throws SQLException {
        final T result = getColumnValue(rs, columnIndex, elementType.javaType());

        return result == null ? new Holder<>()
                : Holder.of(elementType.javaType().isAssignableFrom(result.getClass()) ? result : N.convert(result, elementType));
    }

    /**
     * Retrieves a value from a ResultSet using the specified column label and wraps it in a {@link Holder}.
     * The method attempts to convert the retrieved value to the element type if necessary.
     *
     * @param rs the ResultSet to read from
     * @param columnName the label for the column specified with the SQL AS clause
     * @return a Holder containing the retrieved value, or a Holder holding {@code null} if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnName is invalid
     */
    @Override
    public Holder<T> get(final ResultSet rs, final String columnName) throws SQLException {
        final T result = getColumnValue(rs, columnName, elementType.javaType());

        return result == null ? new Holder<>()
                : Holder.of(elementType.javaType().isAssignableFrom(result.getClass()) ? result : N.convert(result, elementType));
    }

    /**
     * Sets a parameter in a PreparedStatement to the value contained in a {@link Holder}.
     * If the Holder is {@code null} or holds a {@code null} value, sets the parameter to SQL NULL.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the Holder value to set
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Holder<T> x) throws SQLException {
        stmt.setObject(columnIndex, (x == null || x.isNull()) ? null : x.value());
    }

    /**
     * Sets a named parameter in a CallableStatement to the value contained in a {@link Holder}.
     * If the Holder is {@code null} or holds a {@code null} value, sets the parameter to SQL NULL.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Holder value to set
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Holder<T> x) throws SQLException {
        stmt.setObject(parameterName, (x == null || x.isNull()) ? null : x.value());
    }

    /**
     * Appends the string representation of a {@link Holder} to an Appendable.
     * If the Holder is {@code null} or holds a {@code null} value, appends the {@code NULL_STRING} constant.
     * Otherwise, delegates to the actual type handler of the contained value.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the Appendable to write to
     * @param x the Holder value to append
     * @throws IOException if an I/O error occurs during the append operation
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
    public void appendTo(final Appendable appendable, final Holder<T> x) throws IOException {
        if (x == null || x.isNull()) {
            appendable.append(NULL_STRING);
        } else {
            // elementType.write(writer, x.value());
            Type.<Object> of(x.value().getClass()).appendTo(appendable, x.value());
        }
    }

    /**
     * Writes the character representation of a {@link Holder} to a CharacterWriter.
     * If the Holder is {@code null} or holds a {@code null} value, writes the {@code NULL_CHAR_ARRAY}.
     * Otherwise, delegates to the actual type handler of the contained value.
     * This method is typically used for JSON/XML serialization.
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
     * @param writer the CharacterWriter to write to
     * @param x the Holder value to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final Holder<T> x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null || x.isNull()) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            // elementType.serializeTo(writer, x.value(), config);
            Type.<Object> of(x.value().getClass()).serializeTo(writer, x.value(), config);
        }
    }
}
