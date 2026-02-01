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

import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for AtomicInteger operations.
 * This class provides serialization/deserialization and database operations
 * for java.util.concurrent.atomic.AtomicInteger instances.
 */
public class AtomicIntegerType extends AbstractAtomicType<AtomicInteger> {

    /**
     * The type name constant for AtomicInteger type identification.
     */
    public static final String ATOMIC_INTEGER = AtomicInteger.class.getSimpleName();

    /**
     * Package-private constructor for AtomicIntegerType.
     * This constructor is called by the TypeFactory to create AtomicInteger type instances.
     */
    AtomicIntegerType() {
        super(ATOMIC_INTEGER);
    }

    /**
     * Returns the Class object representing the AtomicInteger class.
     *
     * @return the Class object for {@code AtomicInteger}
     */
    @Override
    public Class<AtomicInteger> clazz() {
        return AtomicInteger.class;
    }

    /**
     * Converts an AtomicInteger value to its string representation.
     * The integer value is extracted from the AtomicInteger and converted to string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicIntegerType type = (AtomicIntegerType) TypeFactory.getType(AtomicInteger.class);
     *
     * AtomicInteger value = new AtomicInteger(42);
     * String str = type.stringOf(value);
     * // str: "42"
     *
     * AtomicInteger zero = new AtomicInteger(0);
     * String zeroStr = type.stringOf(zero);
     * // zeroStr: "0"
     *
     * String nullStr = type.stringOf(null);
     * // nullStr: null
     * }</pre>
     *
     * @param x the AtomicInteger value to convert
     * @return the string representation of the integer value, or {@code null} if input is null
     */
    @Override
    public String stringOf(final AtomicInteger x) {
        return (x == null) ? null : N.stringOf(x.get());
    }

    /**
     * Converts a string representation to an AtomicInteger value.
     * Parses the string as an integer and wraps it in a new AtomicInteger instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicIntegerType type = (AtomicIntegerType) TypeFactory.getType(AtomicInteger.class);
     *
     * AtomicInteger value = type.valueOf("42");
     * // value.get(): 42
     *
     * AtomicInteger negative = type.valueOf("-100");
     * // negative.get(): -100
     *
     * AtomicInteger nullValue = type.valueOf(null);
     * // nullValue: null
     *
     * // Throws NumberFormatException:
     * // type.valueOf("not a number");
     * }</pre>
     *
     * @param str the string to parse as an integer
     * @return a new AtomicInteger containing the parsed value, or {@code null} if str is {@code null} or empty
     * @throws NumberFormatException if the string cannot be parsed as an integer
     */
    @Override
    public AtomicInteger valueOf(final String str) {
        return Strings.isEmpty(str) ? null : new AtomicInteger(Integer.parseInt(str.trim()));
    }

    /**
     * Retrieves an AtomicInteger value from a ResultSet at the specified column index.
     * The integer value is read from the database and wrapped in a new AtomicInteger instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicIntegerType type = (AtomicIntegerType) TypeFactory.getType(AtomicInteger.class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     * AtomicInteger counter = type.get(rs, 1);   // retrieves AtomicInteger from column 1
     * int value = counter.get();  // get the underlying int value
     * }</pre>
     *
     * @param rs the ResultSet to retrieve the value from
     * @param columnIndex the column index (1-based) of the integer value
     * @return a new AtomicInteger containing the retrieved value (0 if SQL NULL)
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public AtomicInteger get(final ResultSet rs, final int columnIndex) throws SQLException {
        return new AtomicInteger(rs.getInt(columnIndex));
    }

    /**
     * Retrieves an AtomicInteger value from a ResultSet using the specified column label.
     * The integer value is read from the database and wrapped in a new AtomicInteger instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicIntegerType type = (AtomicIntegerType) TypeFactory.getType(AtomicInteger.class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     * AtomicInteger counter = type.get(rs, "counter");   // retrieves AtomicInteger from "counter" column
     * int value = counter.get();  // get the underlying int value
     * }</pre>
     *
     * @param rs the ResultSet to retrieve the value from
     * @param columnLabel the label for the column specified with the SQL AS clause,
     *                    or the column name if no AS clause was specified
     * @return a new AtomicInteger containing the retrieved value (0 if SQL NULL)
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public AtomicInteger get(final ResultSet rs, final String columnLabel) throws SQLException {
        return new AtomicInteger(rs.getInt(columnLabel));
    }

    /**
     * Sets an AtomicInteger parameter in a PreparedStatement at the specified position.
     * The integer value is extracted from the AtomicInteger before setting.
     * If the AtomicInteger is {@code null}, sets 0 as the parameter value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicIntegerType type = (AtomicIntegerType) TypeFactory.getType(AtomicInteger.class);
     * AtomicInteger counter = new AtomicInteger(42);
     * try (PreparedStatement stmt = conn.prepareStatement("UPDATE stats SET counter = ? WHERE id = ?")) {
     *     type.set(stmt, 1, counter);
     *     stmt.setInt(2, 1);
     *     stmt.executeUpdate();
     * }
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the AtomicInteger value to set, may be {@code null} (treated as 0)
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final AtomicInteger x) throws SQLException {
        stmt.setInt(columnIndex, (x == null) ? 0 : x.get());
    }

    /**
     * Sets a named AtomicInteger parameter in a CallableStatement.
     * The integer value is extracted from the AtomicInteger before setting.
     * If the AtomicInteger is {@code null}, sets 0 as the parameter value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicIntegerType type = (AtomicIntegerType) TypeFactory.getType(AtomicInteger.class);
     * AtomicInteger counter = new AtomicInteger(100);
     * try (CallableStatement stmt = conn.prepareCall("{call updateCounter(?)}")) {
     *     type.set(stmt, "counter", counter);
     *     stmt.execute();
     * }
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the AtomicInteger value to set, may be {@code null} (treated as 0)
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final AtomicInteger x) throws SQLException {
        stmt.setInt(parameterName, (x == null) ? 0 : x.get());
    }

    /**
     * Appends an AtomicInteger value to an Appendable object.
     * Uses the AtomicInteger's toString() method for the string representation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicIntegerType type = (AtomicIntegerType) TypeFactory.getType(AtomicInteger.class);
     * StringBuilder sb = new StringBuilder();
     * AtomicInteger counter = new AtomicInteger(42);
     * type.appendTo(sb, counter);
     * // sb.toString() equals "42"
     * }</pre>
     *
     * @param appendable the Appendable object to append to
     * @param x the AtomicInteger value to append, may be null
     * @throws IOException if an I/O error occurs during the append operation
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
     * Writes an AtomicInteger value to a CharacterWriter.
     * Extracts the integer value and uses the writer's optimized writeInt method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicIntegerType type = (AtomicIntegerType) TypeFactory.getType(AtomicInteger.class);
     * CharacterWriter writer = new CharacterWriter();
     * AtomicInteger counter = new AtomicInteger(42);
     * type.writeCharacter(writer, counter, null);
     * // Writes: 42
     * }</pre>
     *
     * @param writer the CharacterWriter to write to
     * @param x the AtomicInteger value to write, may be null
     * @param config the serialization configuration (not used for integer values)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final AtomicInteger x, final JsonXmlSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.writeInt(x.get());
        }
    }
}