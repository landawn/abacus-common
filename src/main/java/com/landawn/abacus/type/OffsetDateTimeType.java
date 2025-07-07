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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;

/**
 * Type handler for {@link OffsetDateTime} objects, providing serialization, deserialization,
 * and database interaction capabilities for date-time values with timezone offset information.
 * This handler supports multiple date-time formats including ISO-8601 and epoch milliseconds.
 */
public class OffsetDateTimeType extends AbstractTemporalType<OffsetDateTime> {

    public static final String OFFSET_DATE_TIME = OffsetDateTime.class.getSimpleName();

    OffsetDateTimeType() {
        super(OFFSET_DATE_TIME);
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * @return the {@link OffsetDateTime} class object
     */
    @Override
    public Class<OffsetDateTime> clazz() {
        return OffsetDateTime.class;
    }

    /**
     * Converts an {@link OffsetDateTime} object to its ISO-8601 string representation.
     * The format used is "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" with UTC timezone.
     * 
     * @param x the OffsetDateTime object to convert
     * @return the ISO-8601 formatted string, or null if the input is null
     */
    @Override
    public String stringOf(final OffsetDateTime x) {
        return (x == null) ? null : iso8601TimestampDTF.format(x);
    }

    /**
     * Converts an object to an {@link OffsetDateTime}.
     * If the object is a Number, it's treated as epoch milliseconds.
     * Otherwise, the object is converted to string and parsed.
     *
     * @param obj the object to convert
     * @return the OffsetDateTime value, or null if the input is null
     */
    @Override
    public OffsetDateTime valueOf(final Object obj) {
        if (obj instanceof Number) {
            return OffsetDateTime.ofInstant(Instant.ofEpochMilli(((Number) obj).longValue()), DEFAULT_ZONE_ID);
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Converts a string representation to an {@link OffsetDateTime} object.
     * Supports multiple formats:
     * - Epoch milliseconds as a numeric string
     * - ISO-8601 date-time format (20 characters ending with 'Z')
     * - ISO-8601 timestamp format (24 characters ending with 'Z')
     * - Standard OffsetDateTime parse format
     * - Special value "SYS_TIME" returns current system time
     * 
     * @param str the string to parse
     * @return the parsed OffsetDateTime, or null if the input is null or represents a null date-time
     * @throws DateTimeParseException if the string cannot be parsed as an OffsetDateTime
     */
    @MayReturnNull
    @Override
    public OffsetDateTime valueOf(final String str) {
        if (isNullDateTime(str)) {
            return null; // NOSONAR
        }

        if (N.equals(str, SYS_TIME)) {
            return OffsetDateTime.now();
        }

        if (isPossibleLong(str)) {
            try {
                return OffsetDateTime.ofInstant(Instant.ofEpochMilli(Numbers.toLong(str)), DEFAULT_ZONE_ID);
            } catch (final NumberFormatException e2) {
                // ignore;
            }
        }

        final int len = str.length();

        return len == 20 && str.charAt(19) == 'Z' ? OffsetDateTime.parse(str, iso8601DateTimeDTF)
                : (len == 24 && str.charAt(23) == 'Z' ? OffsetDateTime.parse(str, iso8601TimestampDTF) : OffsetDateTime.parse(str));
    }

    /**
     * Converts a character array to an {@link OffsetDateTime} object.
     * This method creates a string from the character array and delegates to the string parsing method.
     * 
     * @param cbuf the character array containing the date-time string
     * @param offset the offset in the array where the date-time string starts
     * @param len the length of the date-time string
     * @return the parsed OffsetDateTime, or null if the input is null or empty
     */
    @MayReturnNull
    @Override
    public OffsetDateTime valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return OffsetDateTime.ofInstant(Instant.ofEpochMilli(parseLong(cbuf, offset, len)), DEFAULT_ZONE_ID);
            } catch (final NumberFormatException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves an {@link OffsetDateTime} value from a ResultSet at the specified column index.
     * The timestamp from the database is converted to OffsetDateTime using the default zone ID.
     * 
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return an OffsetDateTime representing the timestamp, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public OffsetDateTime get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);

        return ts == null ? null : OffsetDateTime.ofInstant(ts.toInstant(), DEFAULT_ZONE_ID);
    }

    /**
     * Retrieves an {@link OffsetDateTime} value from a ResultSet using the specified column name.
     * The timestamp from the database is converted to OffsetDateTime using the default zone ID.
     * 
     * @param rs the ResultSet to read from
     * @param columnName the name of the column to retrieve the value from
     * @return an OffsetDateTime representing the timestamp, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnName is invalid
     */
    @Override
    public OffsetDateTime get(final ResultSet rs, final String columnName) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnName);

        return ts == null ? null : OffsetDateTime.ofInstant(ts.toInstant(), DEFAULT_ZONE_ID);
    }

    /**
     * Sets a parameter in a PreparedStatement to an {@link OffsetDateTime} value.
     * The OffsetDateTime is converted to a Timestamp for database storage.
     * 
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the OffsetDateTime value to set, or null to set SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OffsetDateTime x) throws SQLException {
        stmt.setTimestamp(columnIndex, x == null ? null : Timestamp.from(x.toInstant()));
    }

    /**
     * Sets a named parameter in a CallableStatement to an {@link OffsetDateTime} value.
     * The OffsetDateTime is converted to a Timestamp for database storage.
     * 
     * @param stmt the CallableStatement to set the parameter on
     * @param columnName the name of the parameter to set
     * @param x the OffsetDateTime value to set, or null to set SQL NULL
     * @throws SQLException if a database access error occurs or the columnName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String columnName, final OffsetDateTime x) throws SQLException {
        stmt.setTimestamp(columnName, x == null ? null : Timestamp.from(x.toInstant()));
    }

    /**
     * Appends the string representation of an {@link OffsetDateTime} to an Appendable.
     * 
     * @param appendable the Appendable to write to
     * @param x the OffsetDateTime value to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final OffsetDateTime x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(stringOf(x));
        }
    }

    /**
     * Writes the character representation of an {@link OffsetDateTime} to a CharacterWriter.
     * The format depends on the serialization configuration, supporting:
     * - LONG format: epoch milliseconds as a number
     * - ISO_8601_DATE_TIME: "yyyy-MM-dd'T'HH:mm:ss'Z'"
     * - ISO_8601_TIMESTAMP: "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
     * - Default: standard string representation
     * 
     * @param writer the CharacterWriter to write to
     * @param x the OffsetDateTime value to write
     * @param config the serialization configuration specifying format and quoting
     * @throws IOException if an I/O error occurs during the write operation
     */
    @SuppressWarnings("null")
    @Override
    public void writeCharacter(final CharacterWriter writer, final OffsetDateTime x, final JSONXMLSerializationConfig<?> config) throws IOException {
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
                        writer.write(x.toInstant().toEpochMilli());

                        break;

                    case ISO_8601_DATE_TIME:
                        writer.write(iso8601DateTimeDTF.format(x));

                        break;

                    case ISO_8601_TIMESTAMP:
                        writer.write(iso8601TimestampDTF.format(x));

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