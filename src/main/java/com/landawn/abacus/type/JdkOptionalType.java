/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.WD;

/**
 * Type handler for java.util.Optional with generic type parameter.
 * This class provides serialization, deserialization, and database access capabilities for Optional instances.
 * Optional is a container that may or may not contain a {@code non-null} value.
 * Empty optionals are represented as {@code null} in serialized form.
 *
 * @param <T> the type of value that may be present in the Optional
 */
@SuppressWarnings("java:S2160")
public class JdkOptionalType<T> extends AbstractOptionalType<Optional<T>> {

    public static final String OPTIONAL = "JdkOptional";

    private final String declaringName;

    private final Type<T>[] parameterTypes;

    private final Type<T> elementType;

    protected JdkOptionalType(final String parameterTypeName) {
        super(OPTIONAL + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + WD.GREATER_THAN);

        declaringName = OPTIONAL + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + WD.GREATER_THAN;
        parameterTypes = new Type[] { TypeFactory.getType(parameterTypeName) };
        elementType = parameterTypes[0];
    }

    /**
     * Returns the declaring name of this optional type.
     * The declaring name represents the type in a simplified format suitable for type declarations.
     *
     * @return the declaring name of this type (e.g., "JdkOptional&lt;String&gt;")
     @MayReturnNull
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the Optional type.
     *
     * @return Optional.class with appropriate generic type casting
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Class<Optional<T>> clazz() {
        return (Class) Optional.class;
    }

    /**
     * Returns the type handler for the element that may be contained in the Optional.
     *
     * @return the Type instance representing the element type of this Optional
     */
    @Override
    public Type<T> getElementType() {
        return elementType;
    }

    /**
     * Returns an array containing the parameter types of this generic optional type.
     * For optional types, this array contains a single element representing the value type.
     *
     * @return an array containing the value type as the only parameter type
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
     * @param x the Optional to convert to string
     * @return the string representation of the contained value, or {@code null} if empty or null
     @MayReturnNull
     */
    @Override
    public String stringOf(final Optional<T> x) {
        return (x == null || x.isEmpty()) ? null : N.stringOf(x.get()); // elementType.stringOf(x.get()); //NOSONAR
    }

    /**
     * Parses a string representation into an Optional.
     * Null strings result in an empty Optional.
     * Non-null strings are parsed according to the element type and wrapped in Optional.
     *
     * @param str the string to parse
     * @return Optional.empty() if the string is {@code null}, otherwise Optional.ofNullable() of the parsed value
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
        final T obj = getColumnValue(rs, columnIndex, elementType.clazz());

        return obj == null ? (Optional<T>) Optional.empty()
                : Optional.of(elementType.clazz().isAssignableFrom(obj.getClass()) ? obj : N.convert(obj, elementType));
    }

    /**
     * Retrieves an Optional value from the specified column in a ResultSet using the column label.
     * If the column value is {@code null}, returns an empty Optional.
     * Otherwise, converts the value to the appropriate type and wraps it in Optional.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label of the column to read
     * @return Optional.empty() if the column is {@code null}, otherwise Optional containing the converted value
     * @throws SQLException if a database access error occurs or the columnLabel is not found
     */
    @Override
    public Optional<T> get(final ResultSet rs, final String columnLabel) throws SQLException {
        final T obj = getColumnValue(rs, columnLabel, elementType.clazz());

        return obj == null ? (Optional<T>) Optional.empty()
                : Optional.of(elementType.clazz().isAssignableFrom(obj.getClass()) ? obj : N.convert(obj, elementType));
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
     * Present values are serialized using their runtime type for accuracy.
     *
     * @param appendable the Appendable to write to
     * @param x the Optional to append
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void appendTo(final Appendable appendable, final Optional<T> x) throws IOException {
        if (x == null || x.isEmpty()) { //NOSONAR
            appendable.append(NULL_STRING);
        } else {
            // elementType.write(writer, x.get());
            N.typeOf(x.get().getClass()).appendTo(appendable, x.get());
        }
    }

    /**
     * Writes the character representation of an Optional to a CharacterWriter.
     * Empty optionals are written as {@code null}.
     * Present values are serialized using their runtime type for accuracy.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Optional to write
     * @param config the serialization configuration to use
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Optional<T> x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) { //NOSONAR
            writer.write(NULL_CHAR_ARRAY);
        } else {
            // elementType.writeCharacter(writer, x.get(), config);
            N.typeOf(x.get().getClass()).writeCharacter(writer, x.get(), config);
        }
    }
}
