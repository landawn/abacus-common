/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link java.util.concurrent.atomic.AtomicInteger} values.
 * Provides serialization, deserialization, and JDBC operations for {@code AtomicInteger} instances.
 *
 * <p>String representation: the decimal string of the contained {@code int} value.</p>
 * <p>JDBC mapping: stored and retrieved as a SQL {@code INTEGER} column
 * ({@link java.sql.Types#INTEGER}).</p>
 *
 * @see java.util.concurrent.atomic.AtomicInteger
 */
public class AtomicIntegerType extends AbstractAtomicType<AtomicInteger> {

    /**
     * The type name constant used to identify this type within the type system
     * (value: {@code "AtomicInteger"}).
     */
    public static final String ATOMIC_INTEGER = AtomicInteger.class.getSimpleName();

    /**
     * Package-private constructor for {@code AtomicIntegerType}.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     */
    AtomicIntegerType() {
        super(ATOMIC_INTEGER);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code AtomicInteger.class}
     */
    @Override
    public Class<AtomicInteger> javaType() {
        return AtomicInteger.class;
    }

    /**
     * Converts an {@link java.util.concurrent.atomic.AtomicInteger} to its decimal string representation.
     * The contained {@code int} value is obtained via {@link java.util.concurrent.atomic.AtomicInteger#get()}.
     *
     * @param x the {@code AtomicInteger} to convert; may be {@code null}
     * @return the decimal string representation of the contained value,
     *         or {@code null} if {@code x} is {@code null}
     */
    @Override
    public String stringOf(final AtomicInteger x) {
        return (x == null) ? null : N.stringOf(x.get());
    }

    /**
     * Parses a decimal string and returns a new {@link java.util.concurrent.atomic.AtomicInteger}
     * containing the parsed value. Leading and trailing whitespace is trimmed before parsing.
     *
     * @param str the decimal string to parse; may be {@code null} or empty
     * @return a new {@code AtomicInteger} containing the parsed value,
     *         or {@code null} if {@code str} is {@code null} or empty
     * @throws NumberFormatException if {@code str} cannot be parsed as a valid {@code int}
     */
    @Override
    public AtomicInteger valueOf(final String str) {
        return Strings.isEmpty(str) ? null : new AtomicInteger(Integer.parseInt(str.trim()));
    }

    /**
     * Retrieves an {@link java.util.concurrent.atomic.AtomicInteger} from a {@link java.sql.ResultSet}
     * at the specified column index.
     * The column value is read as a SQL {@code INTEGER} and wrapped in a new {@code AtomicInteger}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column containing the integer value
     * @return a new {@code AtomicInteger} wrapping the retrieved value, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public AtomicInteger get(final ResultSet rs, final int columnIndex) throws SQLException {
        final int value = rs.getInt(columnIndex);

        return rs.wasNull() ? null : new AtomicInteger(value);
    }

    /**
     * Retrieves an {@link java.util.concurrent.atomic.AtomicInteger} from a {@link java.sql.ResultSet}
     * using the specified column label.
     * The column value is read as a SQL {@code INTEGER} and wrapped in a new {@code AtomicInteger}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label as specified in the SQL AS clause, or the column name if no AS clause was used
     * @return a new {@code AtomicInteger} wrapping the retrieved value, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public AtomicInteger get(final ResultSet rs, final String columnName) throws SQLException {
        final int value = rs.getInt(columnName);

        return rs.wasNull() ? null : new AtomicInteger(value);
    }

    /**
     * Sets an {@link java.util.concurrent.atomic.AtomicInteger} parameter on a
     * {@link java.sql.PreparedStatement} at the specified position.
     * If {@code x} is {@code null}, the parameter is set to SQL NULL
     * ({@link java.sql.Types#INTEGER}); otherwise the contained int value is used.
     *
     * @param stmt the {@code PreparedStatement} on which to set the parameter
     * @param columnIndex the 1-based parameter index to set
     * @param x the {@code AtomicInteger} value to set; {@code null} is stored as SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final AtomicInteger x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(columnIndex, x.get());
        }
    }

    /**
     * Sets a named {@link java.util.concurrent.atomic.AtomicInteger} parameter on a
     * {@link java.sql.CallableStatement}.
     * If {@code x} is {@code null}, the parameter is set to SQL NULL
     * ({@link java.sql.Types#INTEGER}); otherwise the contained int value is used.
     *
     * @param stmt the {@code CallableStatement} on which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@code AtomicInteger} value to set; {@code null} is stored as SQL NULL
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final AtomicInteger x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(parameterName, x.get());
        }
    }

    /**
     * Appends an {@link java.util.concurrent.atomic.AtomicInteger} value to an {@link Appendable}.
     * Appends {@code "null"} if {@code x} is {@code null}; otherwise appends the decimal string of
     * the contained integer value (equivalent to {@link java.util.concurrent.atomic.AtomicInteger#toString()}).
     *
     * @param appendable the target {@code Appendable}
     * @param x the {@code AtomicInteger} value to append; may be {@code null}
     * @throws IOException if an I/O error occurs during appending
     */
    @Override
    public void appendTo(final Appendable appendable, final AtomicInteger x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(x.toString());
        }
    }

    /**
     * Writes an {@link java.util.concurrent.atomic.AtomicInteger} value to a {@link CharacterWriter}.
     * Writes the literal {@code "null"} character array if {@code x} is {@code null}; otherwise
     * uses the writer's optimized {@code writeInt} method with the contained integer value.
     * {@code config} is not used.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code AtomicInteger} value to write; may be {@code null}
     * @param config the serialization configuration (unused for integer values); may be {@code null}
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final AtomicInteger x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.writeInt(x.get());
        }
    }
}
