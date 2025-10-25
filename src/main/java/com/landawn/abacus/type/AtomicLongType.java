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
import java.util.concurrent.atomic.AtomicLong;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for AtomicLong operations.
 * This class provides serialization/deserialization and database operations
 * for java.util.concurrent.atomic.AtomicLong instances.
 */
public class AtomicLongType extends AbstractAtomicType<AtomicLong> {

    /**
     * The type name constant for AtomicLong type identification.
     */
    public static final String ATOMIC_LONG = AtomicLong.class.getSimpleName();

    AtomicLongType() {
        super(ATOMIC_LONG);
    }

    /**
     * Returns the Class object representing the AtomicLong class.
     *
     * @return the Class object for {@code AtomicLong}
     */
    @Override
    public Class<AtomicLong> clazz() {
        return AtomicLong.class;
    }

    /**
     * Retrieves an AtomicLong value from a ResultSet at the specified column index.
     * The long value is read from the database and wrapped in a new AtomicLong instance.
     *
     * @param rs the ResultSet to retrieve the value from
     * @param columnIndex the column index (1-based) of the long value
     * @return a new AtomicLong containing the retrieved value (0L if SQL NULL)
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public AtomicLong get(final ResultSet rs, final int columnIndex) throws SQLException {
        return new AtomicLong(rs.getLong(columnIndex));
    }

    /**
     * Retrieves an AtomicLong value from a ResultSet using the specified column label.
     * The long value is read from the database and wrapped in a new AtomicLong instance.
     *
     * @param rs the ResultSet to retrieve the value from
     * @param columnLabel the label for the column specified with the SQL AS clause,
     *                    or the column name if no AS clause was specified
     * @return a new AtomicLong containing the retrieved value (0L if SQL NULL)
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public AtomicLong get(final ResultSet rs, final String columnLabel) throws SQLException {
        return new AtomicLong(rs.getLong(columnLabel));
    }

    /**
     * Sets an AtomicLong parameter in a PreparedStatement at the specified position.
     * The long value is extracted from the AtomicLong before setting.
     * If the AtomicLong is null, sets 0L as the parameter value.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the AtomicLong value to set, may be null (treated as 0L)
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final AtomicLong x) throws SQLException {
        stmt.setLong(columnIndex, (x == null) ? 0 : x.get());
    }

    /**
     * Sets a named AtomicLong parameter in a CallableStatement.
     * The long value is extracted from the AtomicLong before setting.
     * If the AtomicLong is null, sets 0L as the parameter value.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the AtomicLong value to set, may be null (treated as 0L)
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final AtomicLong x) throws SQLException {
        stmt.setLong(parameterName, (x == null) ? 0 : x.get());
    }

    /**
     * Converts an AtomicLong value to its string representation.
     * The long value is extracted from the AtomicLong and converted to string.
     *
     * @param x the AtomicLong value to convert
     * @return the string representation of the long value, or null if input is null
     */
    @Override
    public String stringOf(final AtomicLong x) {
        return (x == null) ? null : N.stringOf(x.get());
    }

    /**
     * Converts a string representation to an AtomicLong value.
     * Parses the string as a long and wraps it in a new AtomicLong instance.
     *
     * @param str the string to parse as a long
     * @return a new AtomicLong containing the parsed value, or null if str is null or empty
     * @throws NumberFormatException if the string cannot be parsed as a long
     */
    @Override
    public AtomicLong valueOf(final String str) {
        return Strings.isEmpty(str) ? null : new AtomicLong(Long.parseLong(str));
    }

    /**
     * Appends an AtomicLong value to an Appendable object.
     * Uses the AtomicLong's toString() method for the string representation.
     *
     * @param appendable the Appendable object to append to
     * @param x the AtomicLong value to append, may be null
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final AtomicLong x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(x.toString());
        }
    }

    /**
     * Writes an AtomicLong value to a CharacterWriter.
     * Extracts the long value and uses the writer's optimized write method for longs.
     *
     * @param writer the CharacterWriter to write to
     * @param x the AtomicLong value to write, may be null
     * @param config the serialization configuration (not used for long values)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final AtomicLong x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.get());
        }
    }
}