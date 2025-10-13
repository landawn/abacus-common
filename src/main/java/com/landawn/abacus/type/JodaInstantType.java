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
     * This method returns the Class object representing org.joda.time.Instant, which is used
     * for type identification and reflection operations.
     *
     * <pre>
     * {@code
     * JodaInstantType type = new JodaInstantType();
     * Class<Instant> clazz = type.clazz();
     * System.out.println(clazz.getName()); // Outputs: org.joda.time.Instant
     * }
     * </pre>
     *
     * @return the Class object representing org.joda.time.Instant
     */
    @Override
    public Class<Instant> clazz() {
        return Instant.class;
    }

    /**
     * Converts a Joda Instant to its string representation.
     * This method formats the Instant using the ISO 8601 timestamp format with milliseconds precision,
     * providing a standardized string representation suitable for serialization and display.
     *
     * The Instant is formatted using the ISO 8601 timestamp format with milliseconds (yyyy-MM-dd'T'HH:mm:ss.SSS).
     *
     * <pre>
     * {@code
     * JodaInstantType type = new JodaInstantType();
     * Instant instant = Instant.now();
     * String str = type.stringOf(instant);
     * System.out.println(str); // Example output: 2021-01-01T10:30:00.123
     *
     * // null input returns null
     * String nullStr = type.stringOf(null); // returns null
     * }
     * </pre>
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
     * This method supports multiple string formats including ISO 8601 formats, numeric milliseconds,
     * and a special "SYS_TIME" keyword. It intelligently detects the format based on string length
     * and content, providing flexible parsing capabilities.
     *
     * This method handles the following string formats:
     * - Empty/null string: returns null
     * - "SYS_TIME": returns current system time as Instant
     * - Numeric string (not starting with '-'): parsed as milliseconds since epoch
     * - ISO 8601 date-time format (20 characters): parsed as yyyy-MM-dd'T'HH:mm:ss
     * - ISO 8601 timestamp format (24 characters): parsed as yyyy-MM-dd'T'HH:mm:ss.SSS
     * - Other formats: parsed using the default timestamp parser
     *
     * <pre>
     * {@code
     * JodaInstantType type = new JodaInstantType();
     *
     * // Parse milliseconds since epoch
     * Instant instant1 = type.valueOf("1609459200000");
     *
     * // Parse ISO 8601 date-time format
     * Instant instant2 = type.valueOf("2021-01-01T10:30:00");
     *
     * // Parse ISO 8601 timestamp format
     * Instant instant3 = type.valueOf("2021-01-01T10:30:00.123");
     *
     * // Get current system time
     * Instant instant4 = type.valueOf("SYS_TIME");
     *
     * // Empty or null returns null
     * Instant instant5 = type.valueOf(null); // returns null
     * }
     * </pre>
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
     * This method provides efficient parsing from character arrays, attempting numeric parsing first
     * for performance, then falling back to string parsing. This is useful for parsing data from
     * streams or buffers without creating intermediate String objects.
     *
     * This method first attempts to parse the character array as a long value (milliseconds since epoch).
     * If that fails, it converts the character array to a string and delegates to valueOf(String).
     *
     * <pre>
     * {@code
     * JodaInstantType type = new JodaInstantType();
     *
     * // Parse from character array containing milliseconds
     * char[] millisChars = "1609459200000".toCharArray();
     * Instant instant1 = type.valueOf(millisChars, 0, millisChars.length);
     *
     * // Parse from character array containing ISO 8601 format
     * char[] isoChars = "2021-01-01T10:30:00.123".toCharArray();
     * Instant instant2 = type.valueOf(isoChars, 0, isoChars.length);
     *
     * // Parse substring from character array
     * char[] buffer = "prefix2021-01-01T10:30:00.123suffix".toCharArray();
     * Instant instant3 = type.valueOf(buffer, 6, 24);
     *
     * // null or empty array returns null
     * Instant instant4 = type.valueOf(null, 0, 0); // returns null
     * }
     * </pre>
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
     * This method provides database-to-Java type conversion for Instant objects, reading
     * from SQL TIMESTAMP columns and converting them to Joda-Time Instant instances.
     *
     * <pre>
     * {@code
     * JodaInstantType type = new JodaInstantType();
     * try (ResultSet rs = stmt.executeQuery()) {
     *     if (rs.next()) {
     *         Instant timestamp = type.get(rs, 1);
     *     }
     * }
     * }
     * </pre>
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
     * Retrieves an Instant value from the specified column in a ResultSet using column name.
     * This method provides database-to-Java type conversion for Instant objects by column name.
     *
     * <pre>
     * {@code
     * JodaInstantType type = new JodaInstantType();
     * try (ResultSet rs = stmt.executeQuery()) {
     *     if (rs.next()) {
     *         Instant timestamp = type.get(rs, "created_at");
     *     }
     * }
     * }
     * </pre>
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
     * This method provides Java-to-database type conversion, transforming Joda-Time Instant
     * objects into SQL TIMESTAMP values for database operations.
     *
     * <pre>
     * {@code
     * JodaInstantType type = new JodaInstantType();
     * Instant now = Instant.now();
     * try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO events (timestamp) VALUES (?)")) {
     *     type.set(stmt, 1, now);
     *     stmt.executeUpdate();
     * }
     * }
     * </pre>
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
     * This method provides Java-to-database type conversion for stored procedure calls,
     * transforming Joda-Time Instant objects into SQL TIMESTAMP values using named parameters.
     *
     * <pre>
     * {@code
     * JodaInstantType type = new JodaInstantType();
     * Instant now = Instant.now();
     * try (CallableStatement stmt = conn.prepareCall("{call log_event(?)}")) {
     *     type.set(stmt, "event_time", now);
     *     stmt.execute();
     * }
     * }
     * </pre>
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
     * This method provides efficient text output for Instant values, useful for building
     * strings, writing to streams, or generating formatted output without intermediate String objects.
     *
     * <pre>
     * {@code
     * JodaInstantType type = new JodaInstantType();
     * Instant instant = Instant.now();
     * StringBuilder sb = new StringBuilder("Timestamp: ");
     * type.appendTo(sb, instant);
     * System.out.println(sb); // Outputs: Timestamp: 2021-01-01T10:30:00.123
     *
     * // null handling
     * type.appendTo(sb, null); // Appends "null"
     * }
     * </pre>
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
     * This method provides flexible serialization with configurable output formats,
     * supporting various date-time representations (numeric milliseconds, ISO 8601 formats).
     * The method intelligently handles quoting based on the output format.
     *
     * This method handles different serialization formats based on the configuration:
     * - LONG: writes the epoch milliseconds as a number
     * - ISO_8601_DATE_TIME: writes in yyyy-MM-dd'T'HH:mm:ss format
     * - ISO_8601_TIMESTAMP: writes in yyyy-MM-dd'T'HH:mm:ss.SSS format
     * - Default: uses the standard string representation
     *
     * The output may be quoted based on the serialization configuration settings.
     *
     * <pre>
     * {@code
     * JodaInstantType type = new JodaInstantType();
     * Instant instant = Instant.now();
     * CharacterWriter writer = new CharacterWriter();
     *
     * // Write with LONG format (no quotes)
     * JSONXMLSerializationConfig config1 = JSONXMLSerializationConfig.builder()
     *     .setDateTimeFormat(DateTimeFormat.LONG).build();
     * type.writeCharacter(writer, instant, config1); // Writes: 1609459200000
     *
     * // Write with ISO_8601_TIMESTAMP format (with quotes)
     * JSONXMLSerializationConfig config2 = JSONXMLSerializationConfig.builder()
     *     .setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP)
     *     .setStringQuotation('"').build();
     * type.writeCharacter(writer, instant, config2); // Writes: "2021-01-01T10:30:00.123"
     * }
     * </pre>
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