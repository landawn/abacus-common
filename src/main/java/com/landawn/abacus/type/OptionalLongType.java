/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.u.OptionalLong;

/**
 * Type handler for {@link OptionalLong} objects, providing serialization, deserialization,
 * and database interaction capabilities for optional long integer values. This handler manages
 * the conversion between database long values and OptionalLong wrapper objects.
 */
public class OptionalLongType extends AbstractOptionalType<OptionalLong> {

    public static final String OPTIONAL_LONG = OptionalLong.class.getSimpleName();

    /**
     * Constructs a new OptionalLongType instance.
     * This constructor is protected to allow subclassing while maintaining controlled instantiation
     * through the TypeFactory.
     */
    protected OptionalLongType() {
        super(OPTIONAL_LONG);
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalLong> type = TypeFactory.getType(OptionalLong.class);
     * Class<OptionalLong> clazz = type.clazz();
     * // Returns: OptionalLong.class
     * }</pre>
     *
     * @return the {@link OptionalLong} class object
     */
    @Override
    public Class<OptionalLong> clazz() {
        return OptionalLong.class;
    }

    /**
     * Indicates whether values of this type can be compared.
     * OptionalLong values support comparison operations.
     *
     * @return {@code true}, as OptionalLong values are comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Indicates whether values of this type should be quoted when written to CSV format.
     * Numeric values typically don't require quotes in CSV.
     *
     * @return {@code true}, indicating long values don't need quotes in CSV
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     * Returns the default value for OptionalLong type, which is an empty OptionalLong.
     *
     * @return OptionalLong.empty()
     */
    @Override
    public OptionalLong defaultValue() {
        return OptionalLong.empty();
    }

    /**
     * Converts an {@link OptionalLong} object to its string representation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalLong> type = TypeFactory.getType(OptionalLong.class);
     *
     * OptionalLong opt = OptionalLong.of(123456789L);
     * String result = type.stringOf(opt);
     * // Returns: "123456789"
     *
     * opt = OptionalLong.of(0L);
     * result = type.stringOf(opt);
     * // Returns: "0"
     *
     * opt = OptionalLong.empty();
     * result = type.stringOf(opt);
     * // Returns: null
     * }</pre>
     *
     * @param x the OptionalLong object to convert
     * @return the string representation of the long value, or {@code null} if empty or null
     */
    @Override
    public String stringOf(final OptionalLong x) {
        return x == null || x.isEmpty() ? null : N.stringOf(x.get());
    }

    /**
     * Converts a string representation to an {@link OptionalLong} object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalLong> type = TypeFactory.getType(OptionalLong.class);
     *
     * OptionalLong result = type.valueOf("123456789");
     * // Returns: OptionalLong.of(123456789L)
     *
     * result = type.valueOf("0");
     * // Returns: OptionalLong.of(0L)
     *
     * result = type.valueOf(null);
     * // Returns: OptionalLong.empty()
     *
     * result = type.valueOf("");
     * // Returns: OptionalLong.empty()
     * }</pre>
     *
     * @param str the string to convert
     * @return an OptionalLong containing the parsed long value, or empty if the input is empty or null
     * @throws NumberFormatException if the string cannot be parsed as a long
     */
    @Override
    public OptionalLong valueOf(final String str) {
        return Strings.isEmpty(str) ? OptionalLong.empty() : OptionalLong.of(Numbers.toLong(str));
    }

    /**
     * Retrieves a long value from a ResultSet at the specified column index and wraps it in an {@link OptionalLong}.
     * Handles type conversion if the database column is not a long type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalLong> type = TypeFactory.getType(OptionalLong.class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     *
     * // Column contains long value 123456789
     * OptionalLong opt = type.get(rs, 1);
     * // Returns: OptionalLong.of(123456789L)
     *
     * // Column contains SQL NULL
     * opt = type.get(rs, 2);
     * // Returns: OptionalLong.empty()
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return an OptionalLong containing the long value, or empty if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public OptionalLong get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object result = rs.getObject(columnIndex);

        return result == null ? OptionalLong.empty()
                : OptionalLong.of(result instanceof Long num ? num : (result instanceof Number num ? num.longValue() : Numbers.toLong(result.toString())));
    }

    /**
     * Retrieves a long value from a ResultSet using the specified column label and wraps it in an {@link OptionalLong}.
     * Handles type conversion if the database column is not a long type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalLong> type = TypeFactory.getType(OptionalLong.class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     *
     * // Column "user_id" contains long value 987654321
     * OptionalLong opt = type.get(rs, "user_id");
     * // Returns: OptionalLong.of(987654321L)
     *
     * // Column "last_login" contains SQL NULL
     * opt = type.get(rs, "last_login");
     * // Returns: OptionalLong.empty()
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label for the column specified with the SQL AS clause
     * @return an OptionalLong containing the long value, or empty if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public OptionalLong get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object result = rs.getObject(columnLabel);

        return result == null ? OptionalLong.empty()
                : OptionalLong.of(result instanceof Long num ? num : (result instanceof Number num ? num.longValue() : Numbers.toLong(result.toString())));
    }

    /**
     * Sets a parameter in a PreparedStatement to the value contained in an {@link OptionalLong}.
     * If the OptionalLong is {@code null} or empty, sets the parameter to SQL NULL.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalLong> type = TypeFactory.getType(OptionalLong.class);
     * Connection connection = org.mockito.Mockito.mock(Connection.class);
     * PreparedStatement stmt = org.mockito.Mockito.mock(PreparedStatement.class);
     *
     * OptionalLong opt = OptionalLong.of(123456789L);
     * type.set(stmt, 2, opt);
     * // Sets parameter to 123456789
     *
     * opt = OptionalLong.empty();
     * type.set(stmt, 2, opt);
     * // Sets parameter to SQL NULL
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the OptionalLong value to set
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OptionalLong x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(columnIndex, java.sql.Types.BIGINT);
        } else {
            stmt.setLong(columnIndex, x.get());
        }
    }

    /**
     * Sets a named parameter in a CallableStatement to the value contained in an {@link OptionalLong}.
     * If the OptionalLong is {@code null} or empty, sets the parameter to SQL NULL.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalLong> type = TypeFactory.getType(OptionalLong.class);
     * Connection connection = org.mockito.Mockito.mock(Connection.class);
     * CallableStatement stmt = org.mockito.Mockito.mock(CallableStatement.class);
     *
     * OptionalLong opt = OptionalLong.of(987654321L);
     * type.set(stmt, "p_user_id", opt);
     * // Sets parameter to 987654321
     *
     * opt = OptionalLong.empty();
     * type.set(stmt, "p_last_login", opt);
     * // Sets parameter to SQL NULL
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the OptionalLong value to set
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final OptionalLong x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.BIGINT);
        } else {
            stmt.setLong(parameterName, x.get());
        }
    }

    /**
     * Appends the string representation of an {@link OptionalLong} to an Appendable.
     *
     * @param appendable the Appendable to write to
     * @param x the OptionalLong value to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final OptionalLong x) throws IOException {
        if (x == null || x.isEmpty()) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.get()));
        }
    }

    /**
     * Writes the character representation of an {@link OptionalLong} to a CharacterWriter.
     * This method is typically used for JSON/XML serialization.
     *
     * @param writer the CharacterWriter to write to
     * @param x the OptionalLong value to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final OptionalLong x, final JsonXmlSerializationConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.get());
        }
    }
}