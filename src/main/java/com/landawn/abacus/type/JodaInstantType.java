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
import java.sql.Timestamp;

import org.joda.time.Instant;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

public class JodaInstantType extends AbstractJodaDateTimeType<Instant> {

    public static final String INSTANT = "JodaInstant";

    JodaInstantType() {
        super(INSTANT);
    }

    /**
     * Gets the class type for Joda Instant.
     *
     * @return the Class object representing org.joda.time.Instant
     */
    @Override
    public Class<Instant> clazz() {
        return Instant.class;
    }

    /**
     * Converts a Joda Instant to its string representation.
     * 
     * The Instant is formatted using the ISO 8601 timestamp format with milliseconds (yyyy-MM-dd'T'HH:mm:ss.SSS).
     *
     * @param x the Instant to convert
     * @return the string representation of the Instant, or null if the input is null
     */
    @Override
    public String stringOf(final Instant x) {
        return (x == null) ? null : x.toString(jodaISO8601TimestampFT);
    }

    /**
     * Parses a string representation into a Joda Instant instance.
     * 
     * This method handles the following string formats:
     * - Empty/null string: returns null
     * - "SYS_TIME": returns current system time as Instant
     * - Numeric string (not starting with '-'): parsed as milliseconds since epoch
     * - ISO 8601 date-time format (20 characters): parsed as yyyy-MM-dd'T'HH:mm:ss
     * - ISO 8601 timestamp format (24 characters): parsed as yyyy-MM-dd'T'HH:mm:ss.SSS
     * - Other formats: parsed using the default timestamp parser
     *
     * @param str the string to parse
     * @return an Instant instance, or null if the string is empty or null
     * @throws IllegalArgumentException if the string format is invalid
     */
    @MayReturnNull
    @Override
    public Instant valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        if (N.equals(str, SYS_TIME)) {
            return Instant.now();
        }

        if (str.charAt(4) != '-') {
            try {
                return Instant.ofEpochMilli(Numbers.toLong(str));
            } catch (final NumberFormatException e2) {
                // ignore;
            }
        }

        return str.length() == 20 ? Instant.parse(str, jodaISO8601DateTimeFT)
                : (str.length() == 24 ? Instant.parse(str, jodaISO8601TimestampFT) : Instant.ofEpochMilli(Dates.parseTimestamp(str).getTime()));
    }

    /**
     * Parses a character array into a Joda Instant instance.
     * 
     * This method first attempts to parse the character array as a long value (milliseconds since epoch).
     * If that fails, it converts the character array to a string and delegates to valueOf(String).
     *
     * @param cbuf the character buffer containing the value to parse
     * @param offset the start offset in the character buffer
     * @param len the number of characters to parse
     * @return an Instant instance, or null if the character buffer is null or length is 0
     */
    @MayReturnNull
    @Override
    public Instant valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return Instant.ofEpochMilli(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves an Instant value from the specified column in a ResultSet.
     * 
     * This method reads a Timestamp from the ResultSet and converts it to a Joda Instant.
     * If the timestamp is null, this method returns null.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return an Instant instance created from the timestamp, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Instant get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);

        return ts == null ? null : Instant.ofEpochMilli(ts.getTime());
    }

    /**
     * Retrieves an Instant value from the specified column in a ResultSet.
     * 
     * This method reads a Timestamp from the ResultSet and converts it to a Joda Instant.
     * If the timestamp is null, this method returns null.
     *
     * @param rs the ResultSet to read from
     * @param columnName the column name to retrieve the value from
     * @return an Instant instance created from the timestamp, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column name is invalid
     */
    @Override
    public Instant get(final ResultSet rs, final String columnName) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnName);

        return ts == null ? null : Instant.ofEpochMilli(ts.getTime());
    }

    /**
     * Sets an Instant parameter in a PreparedStatement.
     * 
     * This method converts the Joda Instant to a SQL Timestamp before setting it in the statement.
     * If the Instant is null, a SQL NULL is set for the parameter.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the Instant value to set, or null for SQL NULL
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Instant x) throws SQLException {
        stmt.setTimestamp(columnIndex, x == null ? null : new Timestamp(x.getMillis()));
    }

    /**
     * Sets a named Instant parameter in a CallableStatement.
     * 
     * This method converts the Joda Instant to a SQL Timestamp before setting it in the statement.
     * If the Instant is null, a SQL NULL is set for the parameter.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param columnName the name of the parameter to set
     * @param x the Instant value to set, or null for SQL NULL
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String columnName, final Instant x) throws SQLException {
        stmt.setTimestamp(columnName, x == null ? null : new Timestamp(x.getMillis()));
    }

    /**
     * Appends the string representation of an Instant to an Appendable.
     * 
     * If the Instant is null, appends the null string representation.
     * Otherwise, appends the string representation obtained from stringOf(Instant).
     *
     * @param appendable the Appendable to write to
     * @param x the Instant to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final Instant x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(stringOf(x));
        }
    }

    /**
     * Writes the character representation of an Instant to a CharacterWriter.
     * 
     * This method handles different serialization formats based on the configuration:
     * - LONG: writes the epoch milliseconds as a number
     * - ISO_8601_DATE_TIME: writes in yyyy-MM-dd'T'HH:mm:ss format
     * - ISO_8601_TIMESTAMP: writes in yyyy-MM-dd'T'HH:mm:ss.SSS format
     * - Default: uses the standard string representation
     * 
     * The output may be quoted based on the serialization configuration settings.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Instant to write
     * @param config the serialization configuration specifying format and quoting options
     * @throws IOException if an I/O error occurs during the write operation
     */
    @SuppressWarnings("null")
    @Override
    public void writeCharacter(final CharacterWriter writer, final Instant x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final boolean isQuote = (config != null) && (config.getStringQuotation() != 0) && (config.getDateTimeFormat() != DateTimeFormat.LONG);

            if (isQuote) {
                writer.write(config.getStringQuotation());
            }

            if ((config == null) || (config.getDateTimeFormat() == null)) {
                writer.write(stringOf(x));
            } else {
                switch (config.getDateTimeFormat()) {
                    case LONG:
                        writer.write(x.getMillis());

                        break;

                    case ISO_8601_DATE_TIME:
                        writer.write(x.toString(jodaISO8601DateTimeFT));

                        break;

                    case ISO_8601_TIMESTAMP:
                        writer.write(x.toString(jodaISO8601TimestampFT));

                        break;

                    default:
                        throw new RuntimeException("Unsupported operation");
                }
            }

            if (isQuote) {
                writer.write(config.getStringQuotation());
            }
        }
    }
}