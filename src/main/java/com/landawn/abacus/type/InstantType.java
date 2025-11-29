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

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;

/**
 * Type handler for java.time.Instant.
 * This class provides serialization, deserialization, and database access capabilities for Instant instances.
 * Instant represents a point on the time-line in UTC timezone. The default string format is ISO-8601 timestamp.
 */
public class InstantType extends AbstractTemporalType<Instant> {

    public static final String INSTANT = Instant.class.getSimpleName();

    InstantType() {
        super(INSTANT);
    }

    /**
     * Returns the Class object representing the Instant type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Instant> type = TypeFactory.getType(Instant.class);
     * Class<Instant> clazz = type.clazz();
     * // Returns: Instant.class
     * }</pre>
     *
     * @return Instant.class
     */
    @Override
    public Class<Instant> clazz() {
        return Instant.class;
    }

    /**
     * Converts an Instant to its string representation.
     * Uses ISO-8601 timestamp format with millisecond precision (e.g., "2023-12-25T10:30:45.123Z").
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Instant> type = TypeFactory.getType(Instant.class);
     * Instant instant = Instant.ofEpochMilli(1703502645123L);
     * String result = type.stringOf(instant);
     * // Returns: "2023-12-25T10:30:45.123Z"
     *
     * result = type.stringOf(null);
     * // Returns: null
     * }</pre>
     *
     * @param x the Instant to convert to string
     * @return the ISO-8601 timestamp string representation, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final Instant x) {
        return (x == null) ? null : iso8601TimestampDTF.format(x.atZone(Dates.UTC_ZONE_ID));
    }

    /**
     * Converts various object types to an Instant.
     * Supported input types include:
     * - Number: interpreted as milliseconds since epoch
     * - Other types: converted to string and then parsed
     *
     * @param obj the object to convert to Instant
     * @return an Instant instance, or {@code null} if the input is null
     */
    @Override
    public Instant valueOf(final Object obj) {
        if (obj instanceof Number) {
            return Instant.ofEpochMilli(((Number) obj).longValue());
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Parses a string representation into an Instant.
     * The method handles:
     * - {@code null} or null-like strings: returns null
     * - "sysTime": returns current instant
     * - numeric strings: interpreted as milliseconds since epoch
     * - ISO-8601 formatted strings: parsed according to standard formats
     * - Standard Instant.parse() format for other strings
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Instant> type = TypeFactory.getType(Instant.class);
     * Instant instant1 = type.valueOf("2023-12-25T10:30:45Z");
     * // Parses ISO-8601 date-time
     *
     * Instant instant2 = type.valueOf("1703502645123");
     * // Parses milliseconds since epoch
     *
     * Instant instant3 = type.valueOf("sysTime");
     * // Returns current instant
     * }</pre>
     *
     * @param str the string to parse into an Instant
     * @return the parsed Instant instance, or {@code null} if the input is {@code null} or represents a {@code null} datetime
     */
    @Override
    public Instant valueOf(final String str) {
        if (isNullDateTime(str)) {
            return null; // NOSONAR
        }

        if (N.equals(str, SYS_TIME)) {
            return Instant.now();
        }

        if (isPossibleLong(str)) {
            try {
                return Instant.ofEpochMilli(Numbers.toLong(str));
            } catch (final NumberFormatException e2) {
                // ignore;
            }
        }

        final int len = str.length();

        return len == 20 && str.charAt(19) == 'Z' ? iso8601DateTimeDTF.parse(str, Instant::from)
                : (len == 24 && str.charAt(23) == 'Z' ? iso8601TimestampDTF.parse(str, Instant::from) : Instant.parse(str));
    }

    /**
     * Parses a character array into an Instant.
     * This method is optimized for performance when parsing from character buffers.
     * If the character sequence appears to be a long number, it's interpreted as milliseconds since epoch.
     * Otherwise, the characters are converted to a string and parsed using standard instant parsing.
     *
     * @param cbuf the character array containing the instant representation
     * @param offset the start offset in the character array
     * @param len the number of characters to parse
     * @return the parsed Instant instance, or {@code null} if the input is {@code null} or empty
     */
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
     * The method reads a Timestamp from the database and converts it to an Instant.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Instant> type = TypeFactory.getType(Instant.class);
     * ResultSet rs = ...;  // obtained from database query
     * Instant instant = type.get(rs, 1);
     * // Returns: Instant value from column 1
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the index of the column to read (1-based)
     * @return the Instant value from the column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Instant get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);

        return ts == null ? null : ts.toInstant();
    }

    /**
     * Retrieves an Instant value from the specified column in a ResultSet using the column name.
     * The method reads a Timestamp from the database and converts it to an Instant.
     *
     * @param rs the ResultSet to read from
     * @param columnName the name of the column to read
     * @return the Instant value from the column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnName is not found
     */
    @Override
    public Instant get(final ResultSet rs, final String columnName) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnName);

        return ts == null ? null : ts.toInstant();
    }

    /**
     * Sets an Instant parameter in a PreparedStatement.
     * The Instant is converted to a Timestamp for database storage.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Instant> type = TypeFactory.getType(Instant.class);
     * PreparedStatement stmt = connection.prepareStatement(
     *     "INSERT INTO events (id, created_at) VALUES (?, ?)");
     * Instant now = Instant.now();
     * type.set(stmt, 2, now);
     * // Sets parameter to current instant
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the index of the parameter to set (1-based)
     * @param x the Instant to set, or null
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Instant x) throws SQLException {
        stmt.setTimestamp(columnIndex, x == null ? null : Timestamp.from(x));
    }

    /**
     * Sets an Instant parameter in a CallableStatement using a parameter name.
     * The Instant is converted to a Timestamp for database storage.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param columnName the name of the parameter to set
     * @param x the Instant to set, or null
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String columnName, final Instant x) throws SQLException {
        stmt.setTimestamp(columnName, x == null ? null : Timestamp.from(x));
    }

    /**
     * Appends the string representation of an Instant to an Appendable.
     * Uses the default ISO-8601 timestamp format.
     *
     * @param appendable the Appendable to write to
     * @param x the Instant to append
     * @throws IOException if an I/O error occurs during writing
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
     * The format depends on the serialization configuration:
     * - LONG format: writes milliseconds since epoch as a number
     * - ISO_8601_DATE_TIME: writes in "yyyy-MM-dd'T'HH:mm:ssZ" format
     * - ISO_8601_TIMESTAMP: writes in "yyyy-MM-dd'T'HH:mm:ss.SSSZ" format
     * - Default: uses the standard stringOf() format
     * String formats are quoted if specified in the configuration.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Instant to write
     * @param config the serialization configuration to use
     * @throws IOException if an I/O error occurs during writing
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
                        writer.write(x.toEpochMilli());

                        break;

                    case ISO_8601_DATE_TIME:
                        writer.write(iso8601DateTimeDTF.format(x.atZone(Dates.UTC_ZONE_ID)));

                        break;

                    case ISO_8601_TIMESTAMP:
                        writer.write(iso8601TimestampDTF.format(x.atZone(Dates.UTC_ZONE_ID)));

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
