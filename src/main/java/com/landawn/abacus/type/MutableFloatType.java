/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.MutableFloat;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link com.landawn.abacus.util.MutableFloat} objects, providing
 * serialization, deserialization, and database interaction capabilities for
 * mutable float wrapper objects.
 *
 * @see com.landawn.abacus.util.MutableFloat
 * @see NumberType
 */
public class MutableFloatType extends NumberType<MutableFloat> {

    public static final String MUTABLE_FLOAT = MutableFloat.class.getSimpleName();

    /**
     * Package-private constructor for MutableFloatType.
     * This constructor is called by subclasses to create MutableFloat type instances.
     */
    protected MutableFloatType() {
        super(MUTABLE_FLOAT);
    }

    /**
     * Returns the {@link Class} object representing the {@link MutableFloat} type.
     *
     * @return {@code MutableFloat.class}
     */
    @Override
    public Class<MutableFloat> javaType() {
        return MutableFloat.class;
    }

    /**
     * Converts a {@link MutableFloat} object to its decimal string representation.
     *
     * @param x the {@code MutableFloat} object to convert, may be {@code null}
     * @return the string representation of the float value, or {@code null} if the input is {@code null}
     */
    @Override
    public String stringOf(final MutableFloat x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     * Parses a string to create a {@link MutableFloat} object.
     *
     * @param str the string to parse, may be {@code null} or empty
     * @return a {@code MutableFloat} containing the parsed float value,
     *         or {@code null} if the input is {@code null} or empty
     * @throws NumberFormatException if the string cannot be parsed as a float
     */
    @Override
    public MutableFloat valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableFloat.of(Numbers.toFloat(str));
    }

    /**
     * Retrieves a float value from the specified column in the {@link ResultSet}
     * and wraps it in a {@link MutableFloat}.
     * Returns {@code null} if the column value is SQL {@code NULL} (detected via {@link ResultSet#wasNull()}).
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column to retrieve
     * @return a {@code MutableFloat} wrapping the retrieved value,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public MutableFloat get(final ResultSet rs, final int columnIndex) throws SQLException {
        final float value = rs.getFloat(columnIndex);

        return rs.wasNull() ? null : MutableFloat.of(value);
    }

    /**
     * Retrieves a float value from the specified column in the {@link ResultSet}
     * and wraps it in a {@link MutableFloat}.
     * Returns {@code null} if the column value is SQL {@code NULL} (detected via {@link ResultSet#wasNull()}).
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the label of the column to retrieve (as specified in the SQL AS clause)
     * @return a {@code MutableFloat} wrapping the retrieved value,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public MutableFloat get(final ResultSet rs, final String columnName) throws SQLException {
        final float value = rs.getFloat(columnName);

        return rs.wasNull() ? null : MutableFloat.of(value);
    }

    /**
     * Sets a {@link MutableFloat} parameter in a {@link PreparedStatement} at the specified index.
     * If {@code x} is {@code null}, SQL {@code NULL} ({@link java.sql.Types#FLOAT}) is set;
     * otherwise the wrapped float value is stored.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the {@code MutableFloat} value to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableFloat x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.FLOAT);
        } else {
            stmt.setFloat(columnIndex, x.value());
        }
    }

    /**
     * Sets a {@link MutableFloat} parameter in a {@link CallableStatement} by name.
     * If {@code x} is {@code null}, SQL {@code NULL} ({@link java.sql.Types#FLOAT}) is set;
     * otherwise the wrapped float value is stored.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the {@code MutableFloat} value to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableFloat x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.FLOAT);
        } else {
            stmt.setFloat(parameterName, x.value());
        }
    }

    /**
     * Appends the decimal string representation of a {@link MutableFloat} to an {@link Appendable}.
     * Writes {@code "null"} when {@code x} is {@code null}.
     *
     * @param appendable the target to write to
     * @param x the {@code MutableFloat} to append, may be {@code null}
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final MutableFloat x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.value()));
        }
    }

    /**
     * Writes the float value of a {@link MutableFloat} to a {@link CharacterWriter}.
     * Writes the {@code NULL_CHAR_ARRAY} when {@code x} is {@code null}.
     * The {@code config} parameter is not used for float values.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code MutableFloat} to write, may be {@code null}
     * @param config the serialization configuration (unused for float values)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final MutableFloat x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.value());
        }
    }
}
