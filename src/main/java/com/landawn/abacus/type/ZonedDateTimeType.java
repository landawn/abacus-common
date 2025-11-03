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
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;

class ZonedDateTimeType extends AbstractTemporalType<ZonedDateTime> {

    public static final String ZONED_DATE_TIME = ZonedDateTime.class.getSimpleName();

    ZonedDateTimeType() {
        super(ZONED_DATE_TIME);
    }

    /**
     * Returns the Class object representing the ZonedDateTime class.
     * <p>
     * This method returns {@code ZonedDateTime.class}, which is the Class object for the
     * {@link java.time.ZonedDateTime} class that this type handles.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ZonedDateTime> type = TypeFactory.getType(ZonedDateTime.class);
     * Class&lt;ZonedDateTime&gt; clazz = type.clazz(); // Returns ZonedDateTime.class
     * }</pre>
     *
     * @return the Class object for ZonedDateTime.class
     */
    @Override
    public Class<ZonedDateTime> clazz() {
        return ZonedDateTime.class;
    }

    /**
     * Converts a ZonedDateTime instance to its string representation.
     * <p>
     * This method formats the ZonedDateTime using the ISO 8601 timestamp format with timezone information.
     * If the input is {@code null}, this method returns {@code null}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ZonedDateTime zdt = ZonedDateTime.now();
     * String str = type.stringOf(zdt); // Returns "2023-10-15T10:30:00.123Z"
     * }</pre>
     *
     * @param x the ZonedDateTime instance to convert to string
     * @return the ISO 8601 timestamp string representation, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final ZonedDateTime x) {
        return (x == null) ? null : iso8601TimestampDTF.format(x);
    }

    /**
     * Converts an object to a ZonedDateTime instance.
     * <p>
     * This method handles conversion from:
     * <ul>
     *   <li>Number types - interpreted as epoch milliseconds in the default timezone</li>
     *   <li>String types - parsed according to supported date/time formats</li>
     *   <li>null - returns null</li>
     * </ul>
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ZonedDateTime zdt1 = type.valueOf(1697364600000L); // From epoch milliseconds
     * ZonedDateTime zdt2 = type.valueOf("2023-10-15T10:30:00Z"); // From string
     * }</pre>
     *
     * @param obj the object to convert to ZonedDateTime
     * @return a ZonedDateTime instance, or {@code null} if the input is null
     */
    @Override
    public ZonedDateTime valueOf(final Object obj) {
        if (obj instanceof Number) {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(((Number) obj).longValue()), DEFAULT_ZONE_ID);
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Converts a string to a ZonedDateTime instance.
     * <p>
     * This method handles several input formats:
     * <ul>
     *   <li>null or empty string returns null</li>
     *   <li>"SYS_TIME" returns the current ZonedDateTime</li>
     *   <li>Numeric strings are interpreted as epoch milliseconds</li>
     *   <li>ISO 8601 date-time strings with 'Z' suffix (20 chars) are parsed as ISO date-time</li>
     *   <li>ISO 8601 timestamp strings with 'Z' suffix (24 chars) are parsed as ISO timestamp</li>
     *   <li>Other formats are parsed using the default ZonedDateTime parser</li>
     * </ul>
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ZonedDateTime zdt1 = type.valueOf("2023-10-15T10:30:00Z"); // ISO 8601 format
     * ZonedDateTime zdt2 = type.valueOf("SYS_TIME"); // Current time
     * }</pre>
     *
     * @param str the string to convert to ZonedDateTime
     * @return a ZonedDateTime instance, or {@code null} if the string is empty
     * @throws DateTimeParseException if the string cannot be parsed as a valid date/time
     */
    @Override
    public ZonedDateTime valueOf(final String str) {
        if (isNullDateTime(str)) {
            return null; // NOSONAR
        }

        if (N.equals(str, SYS_TIME)) {
            return ZonedDateTime.now();
        }

        if (isPossibleLong(str)) {
            try {
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(Numbers.toLong(str)), DEFAULT_ZONE_ID);
            } catch (final NumberFormatException e2) {
                // ignore;
            }
        }

        final int len = str.length();

        return len == 20 && str.charAt(19) == 'Z' ? ZonedDateTime.parse(str, iso8601DateTimeDTF)
                : (len == 24 && str.charAt(23) == 'Z' ? ZonedDateTime.parse(str, iso8601TimestampDTF) : ZonedDateTime.parse(str));
    }

    /**
     * Converts a character array to a ZonedDateTime instance.
     * <p>
     * This method first checks if the character array represents a long value (epoch milliseconds).
     * If so, it creates a ZonedDateTime from that timestamp. Otherwise, it converts the
     * character array to a string and delegates to {@link #valueOf(String)}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "1697364600000".toCharArray();
     * ZonedDateTime zdt = type.valueOf(chars, 0, chars.length); // From epoch millis
     * }</pre>
     *
     * @param cbuf the character array containing the date/time representation
     * @param offset the starting position in the character array
     * @param len the number of characters to process
     * @return a ZonedDateTime instance, or {@code null} if the input is {@code null} or empty
     */
    @Override
    public ZonedDateTime valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(parseLong(cbuf, offset, len)), DEFAULT_ZONE_ID);
            } catch (final NumberFormatException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a ZonedDateTime value from a ResultSet at the specified column index.
     * <p>
     * This method reads a Timestamp value from the ResultSet and converts it to a
     * ZonedDateTime in the default timezone.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ZonedDateTime zdt = type.get(resultSet, 1);
     * // Retrieves timestamp from first column as ZonedDateTime
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) of the timestamp value
     * @return the ZonedDateTime value, or {@code null} if the database value is NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public ZonedDateTime get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);

        return ts == null ? null : ZonedDateTime.ofInstant(ts.toInstant(), DEFAULT_ZONE_ID);
    }

    /**
     * Retrieves a ZonedDateTime value from a ResultSet using the specified column label.
     * <p>
     * This method reads a Timestamp value from the ResultSet and converts it to a
     * ZonedDateTime in the default timezone.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ZonedDateTime zdt = type.get(resultSet, "created_date");
     * // Retrieves timestamp from <i>created_date</i> column
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label for the column specified with the SQL AS clause
     * @return the ZonedDateTime value, or {@code null} if the database value is NULL
     * @throws SQLException if a database access error occurs or the column label is invalid
     */
    @Override
    public ZonedDateTime get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnLabel);

        return ts == null ? null : ZonedDateTime.ofInstant(ts.toInstant(), DEFAULT_ZONE_ID);
    }

    /**
     * Sets a ZonedDateTime value in a PreparedStatement at the specified parameter index.
     * <p>
     * This method converts the ZonedDateTime to a Timestamp and sets it in the
     * PreparedStatement. If the ZonedDateTime is {@code null}, a NULL value is set.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ZonedDateTime zdt = ZonedDateTime.now();
     * type.set(preparedStatement, 1, zdt); // Sets timestamp at first parameter
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the value in
     * @param columnIndex the parameter index (1-based) where to set the value
     * @param x the ZonedDateTime value to set, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final ZonedDateTime x) throws SQLException {
        stmt.setTimestamp(columnIndex, x == null ? null : Timestamp.from(x.toInstant()));
    }

    /**
     * Sets a ZonedDateTime value in a CallableStatement using the specified parameter name.
     * <p>
     * This method converts the ZonedDateTime to a Timestamp and sets it in the
     * CallableStatement. If the ZonedDateTime is {@code null}, a NULL value is set.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ZonedDateTime zdt = ZonedDateTime.now();
     * type.set(callableStatement, "created_date", zdt); // Sets timestamp parameter
     * }</pre>
     *
     * @param stmt the CallableStatement to set the value in
     * @param parameterName the name of the parameter where to set the value
     * @param x the ZonedDateTime value to set, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final ZonedDateTime x) throws SQLException {
        stmt.setTimestamp(parameterName, x == null ? null : Timestamp.from(x.toInstant()));
    }

    /**
     * Appends the string representation of a ZonedDateTime to an Appendable.
     * <p>
     * This method formats the ZonedDateTime using the ISO 8601 timestamp format and appends it
     * to the provided Appendable. If the ZonedDateTime is {@code null}, it appends the string "null".
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = new StringBuilder();
     * type.appendTo(sb, ZonedDateTime.now()); // Appends formatted date/time
     * }</pre>
     *
     * @param appendable the Appendable to write to
     * @param x the ZonedDateTime value to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final ZonedDateTime x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(stringOf(x));
        }
    }

    /**
     * Writes the character representation of a ZonedDateTime to a CharacterWriter.
     * <p>
     * This method handles different date/time formats based on the provided configuration:
     * <ul>
     *   <li>LONG format: writes the epoch milliseconds as a number</li>
     *   <li>ISO_8601_DATE_TIME: writes in ISO 8601 date-time format</li>
     *   <li>ISO_8601_TIMESTAMP: writes in ISO 8601 timestamp format</li>
     *   <li>Default: uses the ISO 8601 timestamp format</li>
     * </ul>
     * The output may be quoted based on the configuration settings, except for LONG format.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharacterWriter writer = new CharacterWriter();
     * JSONXMLSerializationConfig<?> config = JSONXMLSerializationConfig.of();
     * type.writeCharacter(writer, ZonedDateTime.now(), config); // Writes formatted date/time
     * }</pre>
     *
     * @param writer the CharacterWriter to write to
     * @param x the ZonedDateTime value to write
     * @param config the serialization configuration controlling format and quoting
     * @throws IOException if an I/O error occurs during the write operation
     */
    @SuppressWarnings("null")
    @Override
    public void writeCharacter(final CharacterWriter writer, final ZonedDateTime x, final JSONXMLSerializationConfig<?> config) throws IOException {
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
