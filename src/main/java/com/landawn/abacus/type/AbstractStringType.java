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
import java.io.Reader;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * Abstract base class for String type handling in the type system.
 * This class provides the foundation for String serialization/deserialization,
 * database operations, and character-based I/O operations.
 */
public abstract class AbstractStringType extends AbstractCharSequenceType<String> {

    protected AbstractStringType(final String typeName) {
        super(typeName);
    }

    /**
     * Returns the Class object representing the String class.
     *
     * @return the Class object for String.class
     */
    @Override
    public Class<String> clazz() {
        return String.class;
    }

    /**
     * Determines whether this type represents a String type.
     * Always returns true for AbstractStringType implementations.
     *
     * @return {@code true} indicating this is a string type
     */
    @Override
    public boolean isString() {
        return true;
    }

    /**
     * Converts a String value to its string representation.
     * Since the input is already a String, this method simply returns the input value unchanged.
     *
     * @param str the String value to convert
     * @return the same String value passed as input, or null if input is null
     */
    @Override
    public String stringOf(final String str) {
        return str;
    }

    /**
     * Converts a string representation to a String value.
     * Since the input is already a String, this method simply returns the input value unchanged.
     *
     * @param str the string representation to convert
     * @return the same String value passed as input, or null if input is null
     */
    @Override
    public String valueOf(final String str) {
        return str;
    }

    /**
     * Creates a String from a character array subset.
     * Constructs a new String from the specified subset of the character array.
     *
     * @param cbuf the character array containing the characters to convert
     * @param offset the starting position in the character array
     * @param len the number of characters to include
     * @return a new String created from the specified characters, or null if cbuf is null,
     *         or an empty string if cbuf is empty or len is 0
     */
    @Override
    public String valueOf(final char[] cbuf, final int offset, final int len) {
        return cbuf == null ? null : ((cbuf.length == 0 || len == 0) ? Strings.EMPTY : String.valueOf(cbuf, offset, len));
    }

    /**
     * Converts an Object to a String value.
     * This method handles special cases including Clob objects and uses type-specific
     * string conversion for other object types.
     *
     * @param obj the object to convert to String
     * @return the String representation of the object, or null if obj is null.
     *         For Clob objects, extracts and returns the character data.
     *         For other objects, uses their type-specific string conversion.
     * @throws UncheckedSQLException if there's an error reading from a Clob or freeing Clob resources
     */
    @MayReturnNull
    @SuppressFBWarnings
    @Override
    public String valueOf(final Object obj) {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof Reader reader) {
            return IOUtil.readAllToString(reader);
        } else if (obj instanceof Clob clob) {
            try {
                return clob.getSubString(1, (int) clob.length());
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e);
            } finally {
                try {
                    clob.free();
                } catch (final SQLException e) {
                    // Log and ignore - don't mask the original exception
                    // Freeing resources should not override the main exception
                }
            }
        } else {
            return valueOf(N.typeOf(obj.getClass()).stringOf(obj));
        }
    }

    /**
     * Retrieves a String value from a ResultSet at the specified column index.
     *
     * @param rs the ResultSet to retrieve the value from
     * @param columnIndex the column index (1-based) of the value to retrieve
     * @return the String value at the specified column, or null if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public String get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    /**
     * Retrieves a String value from a ResultSet using the specified column label.
     *
     * @param rs the ResultSet to retrieve the value from
     * @param columnLabel the label for the column specified with the SQL AS clause,
     *                    or the column name if no AS clause was specified
     * @return the String value in the specified column, or null if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public String get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getString(columnLabel);
    }

    /**
     * Sets a String parameter in a PreparedStatement at the specified position.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the String value to set, may be null
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final String x) throws SQLException {
        stmt.setString(columnIndex, x);
    }

    /**
     * Sets a named String parameter in a CallableStatement.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the String value to set, may be null
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final String x) throws SQLException {
        stmt.setString(parameterName, x);
    }

    /**
     * Appends a String value to an Appendable object.
     * If the String is null, appends the string "null" instead.
     *
     * @param appendable the Appendable object to append to
     * @param x the String value to append, may be null
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final String x) throws IOException {
        appendable.append(Objects.requireNonNullElse(x, NULL_STRING));
    }

    /**
     * Writes a String value to a CharacterWriter with optional quotation based on configuration.
     * This method handles null values and applies string quotation marks if specified in the configuration.
     *
     * @param writer the CharacterWriter to write to
     * @param x the String value to write, may be null
     * @param config the serialization configuration that may specify string quotation preferences
     *               and null string handling options
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, String x, final JSONXMLSerializationConfig<?> config) throws IOException {
        x = x == null && config != null && config.writeNullStringAsEmpty() ? Strings.EMPTY : x;

        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final char ch = config == null ? 0 : config.getStringQuotation();

            if (ch == 0) {
                writer.writeCharacter(x);
            } else {
                writer.write(ch);
                writer.writeCharacter(x);
                writer.write(ch);
            }
        }
    }
}