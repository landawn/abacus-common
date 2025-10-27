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
import java.util.concurrent.atomic.AtomicBoolean;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for AtomicBoolean operations.
 * This class provides serialization/deserialization and database operations
 * for java.util.concurrent.atomic.AtomicBoolean instances.
 */
public class AtomicBooleanType extends AbstractAtomicType<AtomicBoolean> {

    /**
     * The type name constant for AtomicBoolean type identification.
     */
    public static final String ATOMIC_BOOLEAN = AtomicBoolean.class.getSimpleName();

    AtomicBooleanType() {
        super(ATOMIC_BOOLEAN);
    }

    /**
     * Returns the Class object representing the AtomicBoolean class.
     *
     * @return the Class object for {@code AtomicBoolean}
     */
    @Override
    public Class<AtomicBoolean> clazz() {
        return AtomicBoolean.class;
    }

    /**
     * Converts an AtomicBoolean value to its string representation.
     * The boolean value is extracted from the AtomicBoolean and converted to
     * either "true" or "false" string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicBooleanType type = (AtomicBooleanType) TypeFactory.getType(AtomicBoolean.class);
     *
     * AtomicBoolean trueValue = new AtomicBoolean(true);
     * String str = type.stringOf(trueValue);
     * // str: "true"
     *
     * AtomicBoolean falseValue = new AtomicBoolean(false);
     * String strFalse = type.stringOf(falseValue);
     * // strFalse: "false"
     *
     * String nullStr = type.stringOf(null);
     * // nullStr: null
     * }</pre>
     *
     * @param x the AtomicBoolean value to convert
     * @return "true" if the AtomicBoolean contains {@code true}, "false" if it contains {@code false},
     *         or {@code null} if the input is null
     @MayReturnNull
     */
    @Override
    public String stringOf(final AtomicBoolean x) {
        return (x == null) ? null : (x.get() ? TRUE_STRING : FALSE_STRING);
    }

    /**
     * Converts a string representation to an AtomicBoolean value.
     * Parses the string using Boolean.parseBoolean() and wraps the result
     * in a new AtomicBoolean instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicBooleanType type = (AtomicBooleanType) TypeFactory.getType(AtomicBoolean.class);
     *
     * AtomicBoolean trueValue = type.valueOf("true");
     * // trueValue.get(): true
     *
     * AtomicBoolean trueValue2 = type.valueOf("TRUE");
     * // trueValue2.get(): true (case-insensitive)
     *
     * AtomicBoolean falseValue = type.valueOf("false");
     * // falseValue.get(): false
     *
     * AtomicBoolean nullValue = type.valueOf(null);
     * // nullValue: null
     * }</pre>
     *
     * @param str the string to parse (case-insensitive "true" results in {@code true}, all else false)
     * @return a new AtomicBoolean containing the parsed value, or {@code null} if str is {@code null} or empty
     @MayReturnNull
     */
    @Override
    public AtomicBoolean valueOf(final String str) {
        return Strings.isEmpty(str) ? null : new AtomicBoolean(Boolean.parseBoolean(str));
    }

    /**
     * Retrieves an AtomicBoolean value from a ResultSet at the specified column index.
     * The boolean value is read from the database and wrapped in a new AtomicBoolean instance.
     *
     * @param rs the ResultSet to retrieve the value from
     * @param columnIndex the column index (1-based) of the boolean value
     * @return a new AtomicBoolean containing the retrieved value (false if SQL NULL)
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     @MayReturnNull
     */
    @Override
    public AtomicBoolean get(final ResultSet rs, final int columnIndex) throws SQLException {
        return new AtomicBoolean(rs.getBoolean(columnIndex));
    }

    /**
     * Retrieves an AtomicBoolean value from a ResultSet using the specified column label.
     * The boolean value is read from the database and wrapped in a new AtomicBoolean instance.
     *
     * @param rs the ResultSet to retrieve the value from
     * @param columnLabel the label for the column specified with the SQL AS clause,
     *                    or the column name if no AS clause was specified
     * @return a new AtomicBoolean containing the retrieved value (false if SQL NULL)
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     @MayReturnNull
     */
    @Override
    public AtomicBoolean get(final ResultSet rs, final String columnLabel) throws SQLException {
        return new AtomicBoolean(rs.getBoolean(columnLabel));
    }

    /**
     * Sets an AtomicBoolean parameter in a PreparedStatement at the specified position.
     * The boolean value is extracted from the AtomicBoolean before setting.
     * If the AtomicBoolean is {@code null}, sets {@code false} as the parameter value.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the AtomicBoolean value to set, may be {@code null} (treated as false)
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final AtomicBoolean x) throws SQLException {
        stmt.setBoolean(columnIndex, x != null && x.get());
    }

    /**
     * Sets a named AtomicBoolean parameter in a CallableStatement.
     * The boolean value is extracted from the AtomicBoolean before setting.
     * If the AtomicBoolean is {@code null}, sets {@code false} as the parameter value.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the AtomicBoolean value to set, may be {@code null} (treated as false)
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final AtomicBoolean x) throws SQLException {
        stmt.setBoolean(parameterName, x != null && x.get());
    }

    /**
     * Appends an AtomicBoolean value to an Appendable object.
     * The boolean value is extracted and appended as either "true" or "false".
     *
     * @param appendable the Appendable object to append to
     * @param x the AtomicBoolean value to append, may be null
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final AtomicBoolean x) throws IOException {
        appendable.append((x == null) ? NULL_STRING : (x.get() ? TRUE_STRING : FALSE_STRING));
    }

    /**
     * Writes an AtomicBoolean value to a CharacterWriter.
     * This method delegates to appendTo() for the actual writing logic.
     *
     * @param writer the CharacterWriter to write to
     * @param x the AtomicBoolean value to write, may be null
     * @param config the serialization configuration (not used for boolean values)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final AtomicBoolean x, final JSONXMLSerializationConfig<?> config) throws IOException {
        appendTo(writer, x);
    }
}
