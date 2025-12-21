/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.u.Optional;

/**
 * Generic type handler for {@link Optional} wrapper objects, providing serialization,
 * deserialization, and database interaction capabilities for optional values of any type.
 * This type handler supports generic type parameters and delegates operations to the
 * appropriate element type handler.
 *
 * @param <T> the type of value wrapped by the Optional
 */
@SuppressWarnings("java:S2160")
public class OptionalType<T> extends AbstractOptionalType<Optional<T>> {

    public static final String OPTIONAL = Optional.class.getSimpleName();

    private final String declaringName;

    private final Type<T>[] parameterTypes;

    private final Type<T> elementType;

    /**
     * Constructs a new OptionalType with the specified parameter type.
     * This constructor is protected to allow subclassing while maintaining controlled instantiation
     * through the TypeFactory.
     *
     * @param parameterTypeName the name of the type parameter for the Optional (e.g., "String", "Integer")
     */
    protected OptionalType(final String parameterTypeName) {
        super(OPTIONAL + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + WD.GREATER_THAN);

        declaringName = OPTIONAL + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + WD.GREATER_THAN;
        parameterTypes = new Type[] { TypeFactory.getType(parameterTypeName) };
        elementType = parameterTypes[0];
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
    public Class<Optional<T>> clazz() {
        return (Class) Optional.class;
    }

    /**
     * Gets the type handler for the element type contained within the Optional.
     *
     * @return the Type handler for the wrapped element type
     */
    @Override
    public Type<T> getElementType() {
        return elementType;
    }

    /**
     * Gets the array of parameter types for this generic type.
     * For Optional, this returns a single-element array containing the element type.
     *
     * @return an array containing the element type
     */
    @Override
    public Type<T>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type is a generic type.
     * OptionalType is always generic as it is parameterized with a type T.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Optional<String>> type = TypeFactory.getType("Optional<String>");
     * boolean isGeneric = type.isGenericType();   // Returns true
     * }</pre>
     *
     * @return {@code true}, indicating this is a generic type
     */
    @Override
    public boolean isGenericType() {
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
     * delegates to the element type's string conversion.
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
     * @param x the Optional object to convert
     * @return the string representation of the contained value, or {@code null} if empty
     */
    @Override
    public String stringOf(final Optional<T> x) {
        return (x == null || x.isEmpty()) ? null : N.stringOf(x.get()); // elementType.stringOf(x.get());
    }

    /**
     * Converts a string representation to an {@link Optional} object.
     * If the string is {@code null}, returns an empty Optional. Otherwise,
     * delegates to the element type's valueOf method and wraps the result.
     * The result may be an Optional containing {@code null} if the element type's
     * valueOf returns {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Optional<Integer>> type = TypeFactory.getType("Optional<Integer>");
     * Optional<Integer> opt = type.valueOf("42");   // Returns Optional.of(42)
     *
     * Optional<Integer> empty = type.valueOf(null);   // Returns Optional.empty()
     * }</pre>
     *
     * @param str the string to convert
     * @return an Optional containing the parsed value, or empty Optional if input is null
     */
    @Override
    public Optional<T> valueOf(final String str) {
        return str == null ? (Optional<T>) Optional.empty() : Optional.ofNullable(elementType.valueOf(str));
    }

    /**
     * Retrieves a value from a ResultSet at the specified column index and wraps it in an {@link Optional}.
     * The method attempts to convert the retrieved value to the element type if necessary.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Optional<String>> type = TypeFactory.getType("Optional<String>");
     * PreparedStatement stmt = connection.prepareStatement("SELECT name FROM users WHERE id = ?");
     * stmt.setInt(1, 123);
     * try (ResultSet rs = stmt.executeQuery()) {
     *     if (rs.next()) {
     *         Optional<String> name = type.get(rs, 1);
     *         if (name.isPresent()) {
     *             System.out.println("Name: " + name.get());
     *         }
     *     }
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return an Optional containing the retrieved value, or empty Optional if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Optional<T> get(final ResultSet rs, final int columnIndex) throws SQLException {
        final T obj = getColumnValue(rs, columnIndex, elementType.clazz());

        return obj == null ? (Optional<T>) Optional.empty()
                : Optional.of(elementType.clazz().isAssignableFrom(obj.getClass()) ? obj : N.convert(obj, elementType));
    }

    /**
     * Retrieves a value from a ResultSet using the specified column label and wraps it in an {@link Optional}.
     * The method attempts to convert the retrieved value to the element type if necessary.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label for the column specified with the SQL AS clause
     * @return an Optional containing the retrieved value, or empty Optional if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public Optional<T> get(final ResultSet rs, final String columnLabel) throws SQLException {
        final T obj = getColumnValue(rs, columnLabel, elementType.clazz());

        return obj == null ? (Optional<T>) Optional.empty()
                : Optional.of(elementType.clazz().isAssignableFrom(obj.getClass()) ? obj : N.convert(obj, elementType));
    }

    /**
     * Sets a parameter in a PreparedStatement to the value contained in an {@link Optional}.
     * If the Optional is {@code null} or empty, sets the parameter to SQL NULL.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the Optional value to set
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Optional<T> x) throws SQLException {
        stmt.setObject(columnIndex, (x == null || x.isEmpty()) ? null : x.get());
    }

    /**
     * Sets a named parameter in a CallableStatement to the value contained in an {@link Optional}.
     * If the Optional is {@code null} or empty, sets the parameter to SQL NULL.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Optional value to set
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Optional<T> x) throws SQLException {
        stmt.setObject(parameterName, (x == null || x.isEmpty()) ? null : x.get());
    }

    /**
     * Appends the string representation of an {@link Optional} to an Appendable.
     * If the Optional is {@code null} or empty, appends the NULL_STRING constant.
     * Otherwise, delegates to the actual type handler of the contained value.
     *
     * @param appendable the Appendable to write to
     * @param x the Optional value to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final Optional<T> x) throws IOException {
        if (x == null || x.isEmpty()) {
            appendable.append(NULL_STRING);
        } else {
            // elementType.write(writer, x.get());
            Type.<Object> of(x.get().getClass()).appendTo(appendable, x.get());
        }
    }

    /**
     * Writes the character representation of an {@link Optional} to a CharacterWriter.
     * If the Optional is {@code null} or empty, writes the NULL_CHAR_ARRAY.
     * Otherwise, delegates to the actual type handler of the contained value.
     * This method is typically used for JSON/XML serialization.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Optional value to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Optional<T> x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            // elementType.writeCharacter(writer, x.get(), config);
            Type.<Object> of(x.get().getClass()).writeCharacter(writer, x.get(), config);
        }
    }
}
