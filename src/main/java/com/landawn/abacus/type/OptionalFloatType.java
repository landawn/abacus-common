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
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.u.OptionalFloat;

/**
 * Type handler for {@link OptionalFloat} objects, providing serialization, deserialization,
 * and database interaction capabilities for optional single-precision floating-point values.
 * This handler manages the conversion between database numeric values and OptionalFloat wrapper objects.
 */
public class OptionalFloatType extends AbstractOptionalType<OptionalFloat> {

    public static final String OPTIONAL_FLOAT = OptionalFloat.class.getSimpleName();

    /**
     * Constructs a new OptionalFloatType instance.
     * This constructor is protected to allow subclassing while maintaining controlled instantiation
     * through the TypeFactory.
     */
    protected OptionalFloatType() {
        super(OPTIONAL_FLOAT);
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalFloat> type = TypeFactory.getType(OptionalFloat.class);
     * Class<OptionalFloat> clazz = type.clazz();
     * // Returns: OptionalFloat.class
     * }</pre>
     *
     * @return the {@link OptionalFloat} class object
     */
    @Override
    public Class<OptionalFloat> clazz() {
        return OptionalFloat.class;
    }

    /**
     * Indicates whether values of this type can be compared.
     * OptionalFloat values support comparison operations.
     *
     * @return {@code true}, as OptionalFloat values are comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Indicates whether values of this type should be quoted when written to CSV format.
     * Numeric values typically don't require quotes in CSV.
     *
     * @return {@code true}, indicating float values don't need quotes in CSV
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     * Returns the default value for OptionalFloat type, which is an empty OptionalFloat.
     *
     * @return OptionalFloat.empty()
     */
    @Override
    public OptionalFloat defaultValue() {
        return OptionalFloat.empty();
    }

    /**
     * Converts an {@link OptionalFloat} object to its string representation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalFloat> type = TypeFactory.getType(OptionalFloat.class);
     *
     * OptionalFloat opt = OptionalFloat.of(3.14f);
     * String result = type.stringOf(opt);
     * // Returns: "3.14"
     *
     * opt = OptionalFloat.of(0.0f);
     * result = type.stringOf(opt);
     * // Returns: "0.0"
     *
     * opt = OptionalFloat.empty();
     * result = type.stringOf(opt);
     * // Returns: null
     * }</pre>
     *
     * @param x the OptionalFloat object to convert
     * @return the string representation of the float value, or {@code null} if empty or null
     */
    @Override
    public String stringOf(final OptionalFloat x) {
        return x == null || x.isEmpty() ? null : N.stringOf(x.get());
    }

    /**
     * Converts a string representation to an {@link OptionalFloat} object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalFloat> type = TypeFactory.getType(OptionalFloat.class);
     *
     * OptionalFloat result = type.valueOf("3.14");
     * // Returns: OptionalFloat.of(3.14f)
     *
     * result = type.valueOf("0.0");
     * // Returns: OptionalFloat.of(0.0f)
     *
     * result = type.valueOf(null);
     * // Returns: OptionalFloat.empty()
     *
     * result = type.valueOf("");
     * // Returns: OptionalFloat.empty()
     * }</pre>
     *
     * @param str the string to convert
     * @return an OptionalFloat containing the parsed float value, or empty if the input is empty or null
     * @throws NumberFormatException if the string cannot be parsed as a float
     */
    @Override
    public OptionalFloat valueOf(final String str) {
        return Strings.isEmpty(str) ? OptionalFloat.empty() : OptionalFloat.of(Numbers.toFloat(str));
    }

    /**
     * Retrieves a float value from a ResultSet at the specified column index and wraps it in an {@link OptionalFloat}.
     * Handles type conversion if the database column is not a float type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalFloat> type = TypeFactory.getType(OptionalFloat.class);
     * ResultSet rs = ...;  // obtained from database query
     *
     * // Column contains float value 3.14
     * OptionalFloat opt = type.get(rs, 1);
     * // Returns: OptionalFloat.of(3.14f)
     *
     * // Column contains SQL NULL
     * opt = type.get(rs, 2);
     * // Returns: OptionalFloat.empty()
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return an OptionalFloat containing the float value, or empty if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public OptionalFloat get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object obj = rs.getObject(columnIndex);

        return obj == null ? OptionalFloat.empty() : OptionalFloat.of(obj instanceof Float ? (Float) obj : Numbers.toFloat(obj));
    }

    /**
     * Retrieves a float value from a ResultSet using the specified column label and wraps it in an {@link OptionalFloat}.
     * Handles type conversion if the database column is not a float type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalFloat> type = TypeFactory.getType(OptionalFloat.class);
     * ResultSet rs = ...;  // obtained from database query
     *
     * // Column "temperature" contains float value 98.6
     * OptionalFloat opt = type.get(rs, "temperature");
     * // Returns: OptionalFloat.of(98.6f)
     *
     * // Column "humidity" contains SQL NULL
     * opt = type.get(rs, "humidity");
     * // Returns: OptionalFloat.empty()
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label for the column specified with the SQL AS clause
     * @return an OptionalFloat containing the float value, or empty if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public OptionalFloat get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object obj = rs.getObject(columnLabel);

        return obj == null ? OptionalFloat.empty() : OptionalFloat.of(obj instanceof Float ? (Float) obj : Numbers.toFloat(obj));
    }

    /**
     * Sets a parameter in a PreparedStatement to the value contained in an {@link OptionalFloat}.
     * If the OptionalFloat is {@code null} or empty, sets the parameter to SQL NULL.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalFloat> type = TypeFactory.getType(OptionalFloat.class);
     * PreparedStatement stmt = connection.prepareStatement(
     *     "INSERT INTO measurements (id, temperature) VALUES (?, ?)");
     *
     * OptionalFloat opt = OptionalFloat.of(98.6f);
     * type.set(stmt, 2, opt);
     * // Sets parameter to 98.6
     *
     * opt = OptionalFloat.empty();
     * type.set(stmt, 2, opt);
     * // Sets parameter to SQL NULL
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the OptionalFloat value to set
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OptionalFloat x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(columnIndex, java.sql.Types.FLOAT);
        } else {
            stmt.setFloat(columnIndex, x.get());
        }
    }

    /**
     * Sets a named parameter in a CallableStatement to the value contained in an {@link OptionalFloat}.
     * If the OptionalFloat is {@code null} or empty, sets the parameter to SQL NULL.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalFloat> type = TypeFactory.getType(OptionalFloat.class);
     * CallableStatement stmt = connection.prepareCall("{call record_temperature(?, ?)}");
     *
     * OptionalFloat opt = OptionalFloat.of(37.5f);
     * type.set(stmt, "p_temperature", opt);
     * // Sets parameter to 37.5
     *
     * opt = OptionalFloat.empty();
     * type.set(stmt, "p_humidity", opt);
     * // Sets parameter to SQL NULL
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the OptionalFloat value to set
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final OptionalFloat x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.FLOAT);
        } else {
            stmt.setFloat(parameterName, x.get());
        }
    }

    /**
     * Appends the string representation of an {@link OptionalFloat} to an Appendable.
     *
     * @param appendable the Appendable to write to
     * @param x the OptionalFloat value to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final OptionalFloat x) throws IOException {
        if (x == null || x.isEmpty()) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.get()));
        }
    }

    /**
     * Writes the character representation of an {@link OptionalFloat} to a CharacterWriter.
     * This method is typically used for JSON/XML serialization.
     *
     * @param writer the CharacterWriter to write to
     * @param x the OptionalFloat value to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final OptionalFloat x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.get());
        }
    }
}
