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
import com.landawn.abacus.util.u.Nullable;

/**
 * Generic type handler for {@link Nullable} wrapper objects, providing serialization,
 * deserialization, and database interaction capabilities for {@code nullable} values of any type.
 * This type handler supports generic type parameters and delegates operations to the
 * appropriate element type handler.
 *
 * @param <T> the type of value wrapped by the Nullable
 */
@SuppressWarnings("java:S2160")
public class NullableType<T> extends AbstractOptionalType<Nullable<T>> {

    public static final String NULLABLE = Nullable.class.getSimpleName();

    private final String declaringName;

    private final Type<T>[] parameterTypes;

    private final Type<T> elementType;

    /**
     * Constructs a NullableType for the specified parameter type.
     * This constructor initializes the type handler for Nullable wrapper objects with a specific element type.
     *
     * @param parameterTypeName the fully qualified or simple name of the element type contained in the Nullable
     */
    protected NullableType(final String parameterTypeName) {
        super(NULLABLE + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + WD.GREATER_THAN);

        declaringName = NULLABLE + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + WD.GREATER_THAN;
        parameterTypes = new Type[] { TypeFactory.getType(parameterTypeName) };
        elementType = parameterTypes[0];
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
    public Class<Nullable<T>> clazz() {
        return (Class) Nullable.class;
    }

    /**
     * Gets the type handler for the element type contained within the {@code Nullable}.
     *
     * @return the Type handler for the wrapped element type
     */
    @Override
    public Type<T> getElementType() {
        return elementType;
    }

    /**
     * Gets the array of parameter types for this generic type.
     * For {@code Nullable}, this returns a single-element array containing the element type.
     *
     * @return an array containing the element type
     */
    @Override
    public Type<T>[] getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Returns the default value for {@code Nullable} type, which is an empty {@code Nullable}.
     *
     * @return {@code Nullable}.empty()
     */
    @Override
    public Nullable<T> defaultValue() {
        return Nullable.<T> empty();
    }

    /**
     * Converts a {@link Nullable} object to its string representation.
     * If the {@code Nullable} is {@code null} or contains {@code null}, returns {@code null}. Otherwise,
     * delegates to the element type's string conversion.
     *
     * @param x the {@code Nullable} object to convert
     * @return the string representation of the contained value, or {@code null} if empty
     */
    @Override
    public String stringOf(final Nullable<T> x) {
        return (x == null || x.isNull()) ? null : N.stringOf(x.get()); // elementType.stringOf(x.get());
    }

    /**
     * Converts a string representation to a {@link Nullable} object.
     * If the string is {@code null}, returns an empty {@code Nullable}. Otherwise,
     * delegates to the element type's valueOf method and wraps the result.
     *
     * @param str the string to convert
     * @return a {@code Nullable} containing the parsed value, or empty {@code Nullable} if input is null
     */
    @Override
    public Nullable<T> valueOf(final String str) {
        return str == null ? (Nullable<T>) Nullable.empty() : Nullable.of(elementType.valueOf(str));
    }

    /**
     * Retrieves a value from a ResultSet at the specified column index and wraps it in a {@link Nullable}.
     * The method attempts to convert the retrieved value to the element type if necessary.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return a {@code Nullable} containing the retrieved value, or empty {@code Nullable} if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Nullable<T> get(final ResultSet rs, final int columnIndex) throws SQLException {
        final T obj = getColumnValue(rs, columnIndex, elementType.clazz());

        return obj == null ? (Nullable<T>) Nullable.empty()
                : Nullable.of(elementType.clazz().isAssignableFrom(obj.getClass()) ? obj : N.convert(obj, elementType));
    }

    /**
     * Retrieves a value from a ResultSet using the specified column label and wraps it in a {@link Nullable}.
     * The method attempts to convert the retrieved value to the element type if necessary.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label for the column specified with the SQL AS clause
     * @return a {@code Nullable} containing the retrieved value, or empty {@code Nullable} if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public Nullable<T> get(final ResultSet rs, final String columnLabel) throws SQLException {
        final T obj = getColumnValue(rs, columnLabel, elementType.clazz());

        return obj == null ? (Nullable<T>) Nullable.empty()
                : Nullable.of(elementType.clazz().isAssignableFrom(obj.getClass()) ? obj : N.convert(obj, elementType));
    }

    /**
     * Sets a parameter in a PreparedStatement to the value contained in a {@link Nullable}.
     * If the {@code Nullable} is {@code null} or empty, sets the parameter to SQL NULL.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the {@code Nullable} value to set
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Nullable<T> x) throws SQLException {
        stmt.setObject(columnIndex, (x == null || x.isNull()) ? null : x.get());
    }

    /**
     * Sets a named parameter in a CallableStatement to the value contained in a {@link Nullable}.
     * If the {@code Nullable} is {@code null} or empty, sets the parameter to SQL NULL.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the {@code Nullable} value to set
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Nullable<T> x) throws SQLException {
        stmt.setObject(parameterName, (x == null || x.isNull()) ? null : x.get());
    }

    /**
     * Appends the string representation of a {@link Nullable} to an Appendable.
     * If the {@code Nullable} is {@code null} or empty, appends the NULL_STRING constant.
     * Otherwise, delegates to the actual type handler of the contained value.
     *
     * @param appendable the Appendable to write to
     * @param x the {@code Nullable} value to append
     * @throws IOException if an I/O error occurs during the append operation
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
     * Writes the character representation of a {@link Nullable} to a CharacterWriter.
     * If the {@code Nullable} is {@code null} or empty, writes the NULL_CHAR_ARRAY.
     * Otherwise, delegates to the actual type handler of the contained value.
     * This method is typically used for JSON/XML serialization.
     *
     * @param writer the CharacterWriter to write to
     * @param x the {@code Nullable} value to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Nullable<T> x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null || x.isNull()) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            // elementType.writeCharacter(writer, x.get(), config);
            Type.<Object> of(x.get().getClass()).writeCharacter(writer, x.get(), config);
        }
    }
}
