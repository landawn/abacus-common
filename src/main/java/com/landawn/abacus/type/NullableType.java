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

import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.parser.ParserUtil.XmlEmbeddedJsonConfig;
import com.landawn.abacus.parser.XmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.u.Nullable;

/**
 * Generic type handler for {@link Nullable} wrapper objects (a nested class of the
 * {@code com.landawn.abacus.util.u} utility class), providing serialization, deserialization,
 * and database interaction capabilities for {@code nullable} values of any type.
 * <p>
 * Unlike {@link com.landawn.abacus.util.u.Optional Optional} (handled by {@link OptionalType}),
 * a {@code Nullable} can carry {@code null} as a valid present value:
 * an "empty" {@code Nullable} (no value) and a {@code Nullable} holding {@code null} are
 * distinct states. SQL {@code NULL} columns are mapped here to a present {@code Nullable}
 * holding {@code null}, not to {@link Nullable#empty()}.
 * <p>
 * This type handler supports generic type parameters of the form {@code Nullable<T>}.
 * {@link #stringOf(Nullable)}, {@link #valueOf(String)}, {@link #appendTo(Appendable, Nullable)} and
 * {@link #serializeTo(CharacterWriter, Nullable, JsonXmlSerConfig)} all use the declared
 * {@linkplain #elementType() element type}, so a registered single-value subtype is formatted as its declared base
 * type. {@code appendTo} and {@code serializeTo} both fall back to the value's runtime class when the declared element
 * type is {@code Object}; {@code serializeTo} also writes a structured element - one whose handler is not
 * {@linkplain Type#isSerializable() serializable}, such as a bean, a map or a {@code List<Object>} - as embedded JSON,
 * and the JSON serializer takes that element's shape from its runtime class exactly as it does for a bare property of
 * the same declared type.
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
     * This constructor initializes the type handler for {@code Nullable} wrapper objects with a specific element type.
     *
     * @param parameterTypeName the fully qualified or simple name of the element type contained in the Nullable
     * @throws IllegalArgumentException if a supplied type name is {@code null}, blank, or structurally invalid.
     */
    protected NullableType(final String parameterTypeName) throws IllegalArgumentException {
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
     * delegates to the declared {@linkplain #elementType() element type}. Using the
     * declared type keeps this method symmetric with {@link #valueOf(String)}, which
     * parses the result with that same type handler.
     *
     * <p>For {@code non-null} contained values, the returned string is a serializable representation designed to be parsed
     * back into an equivalent value via {@link #valueOf(String)}. This is the key distinction from
     * {@link Object#toString()}, whose result is not guaranteed to be convertible back into the original value.</p>
     * <p><b>&#9888;&#65039;</b> Serializing {@code Nullable.of(null)} and {@code Nullable.empty()} both returns {@code null};
     * {@link #valueOf(String) valueOf(null)} returns {@code Nullable.empty()}, so a present {@code null} value does
     * not round-trip through the string form.</p>
     *
     * @param x the {@code Nullable} object to convert
     * @return the string representation of the contained value, or {@code null} if empty or null-valued
     * @throws RuntimeException if a contained value is incompatible with its declared type or its type handler fails to produce a string.
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Nullable<T> x) throws RuntimeException {
        return (x == null || x.isNull()) ? null : elementType.stringOf(x.get());
    }

    /**
     * Converts a string representation to a {@link Nullable} object.
     * If the string is {@code null}, returns an empty {@code Nullable}. Otherwise,
     * delegates to the element type's valueOf method and wraps the result.
     * <p>
     * A {@code non-null} string is always wrapped with {@link Nullable#of(Object)}: if the element type parses it to
     * {@code null} (for example {@code ""} for a {@code Nullable<Integer>}, whose element handler answers {@code null}
     * for an empty string) the result is a <i>present</i> {@code Nullable} holding {@code null}
     * ({@code isPresent() == true}, {@code isNull() == true}), unlike {@link OptionalType} and {@link JdkOptionalType}
     * which return an empty optional for the same input.
     *
     * <p>This method round-trips {@code non-null} values written by {@code stringOf}. Strings produced by
     * {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to convert
     * @return a {@code Nullable} containing the parsed value (possibly a present {@code null}), or an empty
     *         {@code Nullable} if {@code str} is {@code null}
     * @throws RuntimeException if the declared element type rejects the non-null input during conversion.
     * @see #valueOf(Object)
     * @see #stringOf(Nullable)
     */
    @Override
    public Nullable<T> valueOf(final String str) throws RuntimeException {
        return str == null ? (Nullable<T>) Nullable.empty() : Nullable.of(elementType.valueOf(str));
    }

    /**
     * Retrieves a value from the specified column in a {@link ResultSet} and wraps it in a {@link Nullable}.
     * If necessary, the retrieved value is converted to the element type.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column to retrieve
     * @return a {@code Nullable} containing the retrieved value (which may hold {@code null} if the column is SQL {@code NULL})
     * @throws NullPointerException if {@code rs} is {@code null} and the selected type handler accesses it.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     * @throws RuntimeException if the declared element type cannot convert the column value.
     */
    @Override
    public Nullable<T> get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException, RuntimeException {
        // Use the declared handler to retain nested generic metadata. Primitive JDBC getters return
        // default values for SQL NULL, so inspect wasNull before wrapping that value.
        final T result = elementType.get(rs, columnIndex);

        return result == null || rs.wasNull() ? Nullable.of((T) null)
                : Nullable.of(elementType.javaType().isAssignableFrom(result.getClass()) ? result : N.convert(result, elementType));
    }

    /**
     * Retrieves a value from the specified column in a {@link ResultSet} and wraps it in a {@link Nullable}.
     * If necessary, the retrieved value is converted to the element type.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the label of the column to retrieve (as specified in the SQL AS clause)
     * @return a {@code Nullable} containing the retrieved value (which may hold {@code null} if the column is SQL {@code NULL})
     * @throws NullPointerException if {@code rs} is {@code null} and the selected type handler accesses it.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     * @throws RuntimeException if the declared element type cannot convert the column value.
     */
    @Override
    public Nullable<T> get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException, RuntimeException {
        // Use the declared handler to retain nested generic metadata. Primitive JDBC getters return
        // default values for SQL NULL, so inspect wasNull before wrapping that value.
        final T result = elementType.get(rs, columnName);

        return result == null || rs.wasNull() ? Nullable.of((T) null)
                : Nullable.of(elementType.javaType().isAssignableFrom(result.getClass()) ? result : N.convert(result, elementType));
    }

    /**
     * Sets a parameter in a {@link PreparedStatement} at the specified index to the value contained
     * in a {@link Nullable}. If {@code x} is {@code null} or wraps a {@code null} value, SQL {@code NULL} is set.
     *
     * <p>The declared element handler performs the binding, including its null mapping and JDBC representation.</p>
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the {@code Nullable} value to set, or {@code null} to set SQL {@code NULL}
     * @throws NullPointerException if {@code stmt} is {@code null} and the selected type handler accesses it.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     * @throws RuntimeException if the declared element type rejects or cannot convert the contained value for JDBC binding.
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Nullable<T> x) throws NullPointerException, SQLException, RuntimeException {
        elementType.set(stmt, columnIndex, (x == null || x.isNull()) ? null : x.get());
    }

    /**
     * Sets a parameter in a {@link CallableStatement} by name to the value contained in a {@link Nullable}.
     * If {@code x} is {@code null} or wraps a {@code null} value, SQL {@code NULL} is set.
     *
     * <p>The declared element handler performs the binding, including its null mapping and JDBC representation.</p>
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the {@code Nullable} value to set, or {@code null} to set SQL {@code NULL}
     * @throws NullPointerException if {@code stmt} is {@code null} and the selected type handler accesses it.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     * @throws RuntimeException if the declared element type rejects or cannot convert the contained value for JDBC binding.
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Nullable<T> x) throws NullPointerException, SQLException, RuntimeException {
        elementType.set(stmt, parameterName, (x == null || x.isNull()) ? null : x.get());
    }

    /**
     * Appends the string representation of a {@link Nullable} to an {@link Appendable}.
     * Writes {@code "null"} if {@code x} is {@code null} or wraps a {@code null} value;
     * otherwise delegates to the declared element type handler; when the declared element type is {@code Object} the
     * handler of the value's runtime class is used instead, exactly as
     * {@link #serializeTo(CharacterWriter, Nullable, JsonXmlSerConfig)} does, so a map, collection or bean element
     * keeps the {@code toString()}-style form rather than falling back to {@code ObjectType}'s JSON {@code stringOf}.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} writes the JSON/XML
     * serialized form by delegating to the declared element type handler with the supplied config.
     *
     * @param appendable the target to write to
     * @param x the {@code Nullable} value to append, may be {@code null}
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
    public void appendTo(final Appendable appendable, final Nullable<T> x) throws NullPointerException, IOException, RuntimeException {
        if (x == null || x.isNull()) {
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
     * Writes the character representation of a {@link Nullable} to a {@link CharacterWriter}.
     * This method is specifically designed for JSON/XML serialization.
     * <p>
     * With the default JSON policy, a {@code null} {@code Nullable}, an empty one and one holding {@code null} are all written by the declared
     * element type handler as a {@code null} value, so that handler's null-substitution flags apply:
     * {@code Nullable<Integer>} honours {@code config.isWriteNullNumberAsZero()} (written as {@code 0}),
     * {@code Nullable<Boolean>} honours {@code writeNullBooleanAsFalse} ({@code false}) and {@code Nullable<String>}
     * honours {@code writeNullStringAsEmpty} ({@code ""}); without such a flag the literal {@code null} is written.
     * A substituted value reads back as a {@code Nullable} holding that value. The XML serializers represent an empty
     * {@code Nullable} property with the {@code isNull="true"} attribute form rather than with the text written here.
     * A present-null Nullable is rejected for XML, including embedded JSON, because its presence state cannot be
     * preserved. Ordinary JSON serialization retains its default null handling.
     * <p>
     * A present {@code non-null} value is written by the declared element type handler. When the declared element
     * type is {@code Object} the handler of the value's runtime class is used instead, so {@code Nullable.of(1)} inside
     * a {@code List<Object>} is written as {@code 1}, not {@code "1"}. A value whose handler is not
     * {@linkplain Type#isSerializable() serializable} - a bean, a map, a {@code List<Object>} - is written as embedded
     * JSON when {@code config} is a {@code JsonSerConfig}, and as its escaped {@code stringOf} text under any other
     * config. The output therefore matches what the JSON serializer writes for the bare value.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML using the element
     * type's serializer, whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code Nullable} value to write, may be {@code null}
     * @param config the serialization configuration, may be {@code null}
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     * @throws ParsingException if this is a present-null Nullable and the configuration is XML or its internal embedded-JSON configuration
     * @throws RuntimeException if a contained value is incompatible with its declared type or its selected type handler fails while writing it.
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final Nullable<T> x, final JsonXmlSerConfig<?> config)
            throws NullPointerException, IOException, RuntimeException {
        // Check at the value serializer so nested JSON payloads enforce XML's policy in the same traversal.
        if (x != null && x.isPresent() && x.get() == null && (config instanceof XmlSerConfig || config instanceof XmlEmbeddedJsonConfig)) {
            throw new ParsingException("Cannot serialize Nullable.of(null) distinctly from Nullable.empty()");
        }
        AbstractTupleType.serializeSlot(writer, elementType, (x == null || x.isNull()) ? null : x.get(), config);
    }
}
