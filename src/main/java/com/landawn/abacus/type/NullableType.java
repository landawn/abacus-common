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
import com.landawn.abacus.util.u.Nullable;

/**
 * Generic type handler for {@link Nullable} wrapper objects from the {@code com.landawn.abacus.util.u}
 * package, providing serialization, deserialization, and database interaction capabilities for
 * nullable values of any type.
 * <p>
 * Unlike {@link com.landawn.abacus.util.u.Optional Optional} (handled by {@link OptionalType}),
 * a {@code Nullable} can carry {@code null} as a valid present value:
 * an "empty" {@code Nullable} (no value) and a {@code Nullable} holding {@code null} are
 * distinct states. SQL {@code NULL} columns are mapped here to a present {@code Nullable}
 * holding {@code null}, not to {@link Nullable#empty()}.
 * <p>
 * This type handler supports generic type parameters of the form {@code NullableType<T>}
 * and delegates element serialization/deserialization to the appropriate element type handler.
 *
 * @param <T> the type of value wrapped by the {@code Nullable}
 */
@SuppressWarnings("java:S2160")
public class NullableType<T> extends AbstractOptionalType<Nullable<T>> {

    /** The type name constant for Nullable type identification, equal to {@code "Nullable"}. */
    public static final String NULLABLE = Nullable.class.getSimpleName();

    private final String declaringName;

    private final List<Type<?>> parameterTypes;

    private final Type<T> elementType;

    /**
     * Constructs a NullableType for the specified parameter type.
     * This constructor initializes the type handler for Nullable wrapper objects with a specific element type.
     *
     * @param parameterTypeName the fully qualified or simple name of the element type contained in the Nullable
     */
    protected NullableType(final String parameterTypeName) {
        super(NULLABLE + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + SK.GREATER_THAN);

        declaringName = NULLABLE + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + SK.GREATER_THAN;
        elementType = TypeFactory.getType(parameterTypeName);
        parameterTypes = List.of(elementType);
    }

    /**
     * Returns the declaring name of this type, which includes the full generic type declaration.
     * For example, "Nullable&lt;String&gt;" for a {@code Nullable} containing String values.
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
     * @return the {@link Nullable} class object
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Class<Nullable<T>> javaType() {
        return (Class) Nullable.class;
    }

    /**
     * Gets the type handler for the element type contained within the {@code Nullable}.
     *
     * @return the Type handler for the wrapped element type
     */
    @Override
    public Type<T> elementType() {
        return elementType;
    }

    /**
     * Gets the immutable list of parameter types for this generic type.
     * For {@code Nullable}, this returns a single-element list containing the element type.
     *
     * @return an immutable list containing the element type
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates that {@code Nullable} is a parameterized (generic) type.
     *
     * @return {@code true}
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Returns the default value for {@code Nullable} type, which is an empty {@code Nullable}.
     *
     * @return {@code Nullable.empty()}
     */
    @Override
    public Nullable<T> defaultValue() {
        return Nullable.<T> empty();
    }

    /**
     * Converts a {@link Nullable} object to its string representation.
     * If the {@code Nullable} is {@code null} or holds a {@code null}/empty value
     * (as reported by {@link Nullable#isNull()}), returns {@code null}. Otherwise,
     * delegates to {@link com.landawn.abacus.util.N#stringOf(Object)}, which selects
     * a converter based on the runtime class of the contained value.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@code Nullable} object to convert
     * @return the string representation of the contained value, or {@code null} if empty or null-valued
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Nullable<T> x) {
        return (x == null || x.isNull()) ? null : N.stringOf(x.get()); // elementType.stringOf(x.get());   //NOSONAR
    }

    /**
     * Converts a string representation to a {@link Nullable} object.
     * If the string is {@code null}, returns an empty {@code Nullable}. Otherwise,
     * delegates to the element type's valueOf method and wraps the result.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to convert
     * @return a {@code Nullable} containing the parsed value, or empty {@code Nullable} if input is null
     * @see #valueOf(Object)
     * @see #stringOf(Nullable)
     */
    @Override
    public Nullable<T> valueOf(final String str) {
        return str == null ? (Nullable<T>) Nullable.empty() : Nullable.of(elementType.valueOf(str));
    }

    /**
     * Retrieves a value from the specified column in a {@link ResultSet} and wraps it in a {@link Nullable}.
     * If necessary, the retrieved value is converted to the element type.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column to retrieve
     * @return a {@code Nullable} containing the retrieved value (which may hold {@code null} if the column is SQL {@code NULL})
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public Nullable<T> get(final ResultSet rs, final int columnIndex) throws SQLException {
        final T result = getColumnValue(rs, columnIndex, elementType.javaType());

        return result == null ? Nullable.of((T) null)
                : Nullable.of(elementType.javaType().isAssignableFrom(result.getClass()) ? result : N.convert(result, elementType));
    }

    /**
     * Retrieves a value from the specified column in a {@link ResultSet} and wraps it in a {@link Nullable}.
     * If necessary, the retrieved value is converted to the element type.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the label of the column to retrieve (as specified in the SQL AS clause)
     * @return a {@code Nullable} containing the retrieved value (which may hold {@code null} if the column is SQL {@code NULL})
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public Nullable<T> get(final ResultSet rs, final String columnName) throws SQLException {
        final T result = getColumnValue(rs, columnName, elementType.javaType());

        return result == null ? Nullable.of((T) null)
                : Nullable.of(elementType.javaType().isAssignableFrom(result.getClass()) ? result : N.convert(result, elementType));
    }

    /**
     * Sets a parameter in a {@link PreparedStatement} at the specified index to the value contained
     * in a {@link Nullable}. If {@code x} is {@code null} or wraps a {@code null} value, SQL {@code NULL} is set.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the {@code Nullable} value to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Nullable<T> x) throws SQLException {
        stmt.setObject(columnIndex, (x == null || x.isNull()) ? null : x.get());
    }

    /**
     * Sets a parameter in a {@link CallableStatement} by name to the value contained in a {@link Nullable}.
     * If {@code x} is {@code null} or wraps a {@code null} value, SQL {@code NULL} is set.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the {@code Nullable} value to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Nullable<T> x) throws SQLException {
        stmt.setObject(parameterName, (x == null || x.isNull()) ? null : x.get());
    }

    /**
     * Appends the string representation of a {@link Nullable} to an {@link Appendable}.
     * Writes {@code "null"} if {@code x} is {@code null} or wraps a {@code null} value;
     * otherwise delegates to the runtime type handler of the contained value.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the target to write to
     * @param x the {@code Nullable} value to append, may be {@code null}
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
    public void appendTo(final Appendable appendable, final Nullable<T> x) throws IOException {
        if (x == null || x.isNull()) {
            appendable.append(NULL_STRING);
        } else {
            // elementType.write(writer, x.get());
            Type.<Object> of(x.get().getClass()).appendTo(appendable, x.get());
        }
    }

    /**
     * Writes the character representation of a {@link Nullable} to a {@link CharacterWriter}.
     * Writes {@code NULL_CHAR_ARRAY} if {@code x} is {@code null} or wraps a {@code null} value;
     * otherwise delegates to the runtime type handler of the contained value.
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
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code Nullable} value to write, may be {@code null}
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final Nullable<T> x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null || x.isNull()) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            // elementType.serializeTo(writer, x.get(), config);
            Type.<Object> of(x.get().getClass()).serializeTo(writer, x.get(), config);
        }
    }
}
