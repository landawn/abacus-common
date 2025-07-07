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
import java.sql.Types;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Abstract base class for short types in the type system.
 * This class provides common functionality for handling short values,
 * including conversion, database operations, and serialization.
 * Note that this class uses Number as its generic type to allow for both
 * primitive short and Short wrapper handling.
 */
public abstract class AbstractShortType extends NumberType<Number> {

    protected AbstractShortType(final String typeName) {
        super(typeName);
    }

    /**
     * Converts a Number value to its string representation as a short.
     * Returns {@code null} if the input is {@code null}, otherwise returns
     * the string representation of the short value.
     *
     * @param x the Number value to convert
     * @return the string representation of the short value, or {@code null} if input is {@code null}
     */
    @MayReturnNull
    @Override
    public String stringOf(final Number x) {
        if (x == null) {
            return null; // NOSONAR
        }

        return N.stringOf(x.shortValue());
    }

    /**
     * Converts a string to a Short value.
     * This method handles various string formats:
     * <ul>
     *   <li>Empty or {@code null} strings return the default value</li>
     *   <li>Strings ending with 'l', 'L', 'f', 'F', 'd', or 'D' have the suffix stripped before parsing</li>
     *   <li>Valid numeric strings are parsed to short values</li>
     * </ul>
     *
     * @param str the string to convert
     * @return the Short value
     * @throws NumberFormatException if the string cannot be parsed as a short
     */
    @Override
    public Short valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return (Short) defaultValue();
        }

        try {
            return Numbers.toShort(str);
        } catch (final NumberFormatException e) {
            if (str.length() > 1) {
                final char ch = str.charAt(str.length() - 1);

                if ((ch == 'l') || (ch == 'L') || (ch == 'f') || (ch == 'F') || (ch == 'd') || (ch == 'D')) {
                    return Numbers.toShort(str.substring(0, str.length() - 1));
                }
            }

            throw e;
        }
    }

    /**
     * Converts a character array to a Short value.
     * Parses the character array as an integer and checks if it's within short range
     * (Short.MIN_VALUE to Short.MAX_VALUE).
     *
     * @param cbuf the character array to convert
     * @param offset the starting position in the array
     * @param len the number of characters to read
     * @return the Short value, or default value if input is {@code null} or empty
     * @throws NumberFormatException if the value is out of short range or not a valid number
     */
    @Override
    public Short valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return (Short) defaultValue();
        }

        final int i = parseInt(cbuf, offset, len);

        if ((i < Short.MIN_VALUE) || (i > Short.MAX_VALUE)) {
            throw new NumberFormatException("Value out of range. Value:\"" + i + "\" Radix:" + 10);
        }

        return (short) i;
    }

    /**
     * Retrieves a short value from a ResultSet at the specified column index.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based)
     * @return the short value at the specified column
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Short get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getShort(columnIndex);
    }

    /**
     * Retrieves a short value from a ResultSet using the specified column label.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the column label
     * @return the short value at the specified column
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Short get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getShort(columnLabel);
    }

    /**
     * Sets a short parameter in a PreparedStatement at the specified position.
     * If the value is {@code null}, sets the parameter to SQL NULL.
     * Otherwise, converts the Number to a short value.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the Number value to set as short, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Number x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.SMALLINT);
        } else {
            stmt.setShort(columnIndex, x.shortValue());
        }
    }

    /**
     * Sets a short parameter in a CallableStatement using the specified parameter name.
     * If the value is {@code null}, sets the parameter to SQL NULL.
     * Otherwise, converts the Number to a short value.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the parameter name
     * @param x the Number value to set as short, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Number x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.SMALLINT);
        } else {
            stmt.setShort(parameterName, x.shortValue());
        }
    }

    /**
     * Appends the string representation of a short value to an Appendable.
     * Writes "null" if the value is {@code null}, otherwise writes the numeric value.
     *
     * @param appendable the Appendable to write to
     * @param x the Number value to append as short
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void appendTo(final Appendable appendable, final Number x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(x.toString());
        }
    }

    /**
     * Writes a short value to a CharacterWriter with optional configuration.
     * If the configuration specifies {@code writeNullNumberAsZero} and the value is {@code null},
     * writes 0 instead of {@code null}.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Number value to write as short
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, Number x, final JSONXMLSerializationConfig<?> config) throws IOException {
        x = x == null && config != null && config.writeNullNumberAsZero() ? Numbers.SHORT_ZERO : x;

        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.shortValue());
        }
    }
}