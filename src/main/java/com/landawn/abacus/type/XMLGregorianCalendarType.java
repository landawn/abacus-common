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

import javax.xml.datatype.XMLGregorianCalendar;

import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link javax.xml.datatype.XMLGregorianCalendar} instances.
 * <p>
 * This class provides conversion between XMLGregorianCalendar objects and their string representations,
 * supporting various date/time formats including ISO 8601 and epoch milliseconds. It also handles
 * database operations for storing and retrieving XMLGregorianCalendar values as JDBC Timestamps.
 * </p>
 *
 * <p><b>Supported Formats:</b></p>
 * <ul>
 *   <li>ISO 8601 date-time format</li>
 *   <li>ISO 8601 timestamp format</li>
 *   <li>Epoch milliseconds (LONG format)</li>
 *   <li>"SYS_TIME" keyword for current time</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Get the XMLGregorianCalendar type handler
 * Type<XMLGregorianCalendar> type = TypeFactory.getType(XMLGregorianCalendar.class);
 *
 * // Convert string to XMLGregorianCalendar
 * XMLGregorianCalendar cal = type.valueOf("2023-10-15T10:30:00");
 *
 * // Get current time
 * XMLGregorianCalendar now = type.valueOf("SYS_TIME");
 *
 * // Convert to string
 * String str = type.stringOf(cal);
 *
 * // Use with PreparedStatement
 * type.set(preparedStatement, 1, cal);
 *
 * // Retrieve from ResultSet
 * XMLGregorianCalendar result = type.get(resultSet, "created_date");
 * }</pre>
 *
 * @see javax.xml.datatype.XMLGregorianCalendar
 * @see com.landawn.abacus.util.Dates
 * @see AbstractType
 */
public class XMLGregorianCalendarType extends AbstractType<XMLGregorianCalendar> {

    public static final String XML_GREGORIAN_CALENDAR = XMLGregorianCalendar.class.getSimpleName();

    /**
     * Constructs an XMLGregorianCalendarType instance.
     * This constructor is package-private and should only be called by TypeFactory.
     */
    XMLGregorianCalendarType() {
        super(XML_GREGORIAN_CALENDAR);
    }

    /**
     * Returns the Class object representing the XMLGregorianCalendar class.
     * <p>
     * This method returns {@code XMLGregorianCalendar.class}, which is the Class object for the
     * {@link javax.xml.datatype.XMLGregorianCalendar} class that this type handles.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<XMLGregorianCalendar> type = TypeFactory.getType(XMLGregorianCalendar.class);
     * Class&lt;XMLGregorianCalendar&gt; clazz = type.clazz();   // Returns XMLGregorianCalendar.class
     * }</pre>
     *
     * @return the Class object for XMLGregorianCalendar.class
     */
    @Override
    public Class<XMLGregorianCalendar> clazz() {
        return XMLGregorianCalendar.class;
    }

    /**
     * Converts a string to an XMLGregorianCalendar instance.
     * <p>
     * This method handles several input formats:
     * <ul>
     *   <li>null or empty string returns null</li>
     *   <li>"SYS_TIME" returns the current time as XMLGregorianCalendar</li>
     *   <li>Standard date/time strings are parsed using the Dates utility</li>
     * </ul>
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar cal1 = type.valueOf("2023-10-15T10:30:00");
     * XMLGregorianCalendar cal2 = type.valueOf("SYS_TIME");   // Current time
     * }</pre>
     *
     * @param str the string to convert to XMLGregorianCalendar
     * @return an XMLGregorianCalendar instance, or {@code null} if the string is empty
     * @throws IllegalArgumentException if the string cannot be parsed as a valid date/time
     */
    @Override
    public XMLGregorianCalendar valueOf(final String str) {
        return Strings.isEmpty(str) ? null : (isSysTime(str) ? Dates.currentXMLGregorianCalendar() : Dates.parseXMLGregorianCalendar(str));
    }

    /**
     * Converts a character array to an XMLGregorianCalendar instance.
     * <p>
     * This method first checks if the character array represents a long value (epoch milliseconds).
     * If so, it creates an XMLGregorianCalendar from that timestamp. Otherwise, it converts the
     * character array to a string and delegates to {@link #valueOf(String)}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "1697364600000".toCharArray();
     * XMLGregorianCalendar cal = type.valueOf(chars, 0, chars.length);   // From epoch millis
     * }</pre>
     *
     * @param cbuf the character array containing the date/time representation
     * @param offset the starting position in the character array
     * @param len the number of characters to process
     * @return an XMLGregorianCalendar instance, or {@code null} if the input is {@code null} or empty
     */
    @Override
    public XMLGregorianCalendar valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return Dates.createXMLGregorianCalendar(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Converts an XMLGregorianCalendar instance to its string representation.
     * <p>
     * This method uses the Dates utility to format the XMLGregorianCalendar to a string.
     * If the input is {@code null}, this method returns {@code null}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar cal = Dates.currentXMLGregorianCalendar();
     * String str = type.stringOf(cal);   // Returns formatted date/time string
     * }</pre>
     *
     * @param x the XMLGregorianCalendar instance to convert to string
     * @return the string representation of the XMLGregorianCalendar, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final XMLGregorianCalendar x) {
        return (x == null) ? null : Dates.format(x);
    }

    /**
     * Retrieves an XMLGregorianCalendar value from a ResultSet at the specified column index.
     * <p>
     * This method reads a Timestamp value from the ResultSet and converts it to an
     * XMLGregorianCalendar using the Dates utility.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar cal = type.get(resultSet, 1);
     * // Retrieves timestamp from first column as XMLGregorianCalendar
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) of the timestamp value
     * @return the XMLGregorianCalendar value, or {@code null} if the database value is NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public XMLGregorianCalendar get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);
        return ts == null ? null : Dates.createXMLGregorianCalendar(ts);
    }

    /**
     * Retrieves an XMLGregorianCalendar value from a ResultSet using the specified column label.
     * <p>
     * This method reads a Timestamp value from the ResultSet and converts it to an
     * XMLGregorianCalendar using the Dates utility.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar cal = type.get(resultSet, "created_date");
     * // Retrieves timestamp from <i>created_date</i> column
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label of the column containing the timestamp value
     * @return the XMLGregorianCalendar value, or {@code null} if the database value is NULL
     * @throws SQLException if a database access error occurs or the column label is invalid
     */
    @Override
    public XMLGregorianCalendar get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnLabel);
        return ts == null ? null : Dates.createXMLGregorianCalendar(ts);
    }

    /**
     * Sets an XMLGregorianCalendar value in a PreparedStatement at the specified parameter index.
     * <p>
     * This method converts the XMLGregorianCalendar to a Timestamp and sets it in the
     * PreparedStatement. If the XMLGregorianCalendar is {@code null}, a NULL value is set.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar cal = Dates.currentXMLGregorianCalendar();
     * type.set(preparedStatement, 1, cal);   // Sets timestamp at first parameter
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the value in
     * @param columnIndex the parameter index (1-based) where to set the value
     * @param x the XMLGregorianCalendar value to set, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final XMLGregorianCalendar x) throws SQLException {
        stmt.setTimestamp(columnIndex, (x == null) ? null : Dates.createTimestamp(x.toGregorianCalendar()));
    }

    /**
     * Sets an XMLGregorianCalendar value in a CallableStatement using the specified parameter name.
     * <p>
     * This method converts the XMLGregorianCalendar to a Timestamp and sets it in the
     * CallableStatement. If the XMLGregorianCalendar is {@code null}, a NULL value is set.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar cal = Dates.currentXMLGregorianCalendar();
     * type.set(callableStatement, "created_date", cal);   // Sets timestamp parameter
     * }</pre>
     *
     * @param stmt the CallableStatement to set the value in
     * @param parameterName the name of the parameter where to set the value
     * @param x the XMLGregorianCalendar value to set, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final XMLGregorianCalendar x) throws SQLException {
        stmt.setTimestamp(parameterName, (x == null) ? null : Dates.createTimestamp(x.toGregorianCalendar()));
    }

    /**
     * Appends the string representation of an XMLGregorianCalendar to an Appendable.
     * <p>
     * This method formats the XMLGregorianCalendar and appends it to the provided Appendable.
     * If the XMLGregorianCalendar is {@code null}, it appends the string "null".
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = new StringBuilder();
     * type.appendTo(sb, xmlGregorianCalendar);   // Appends formatted date/time
     * }</pre>
     *
     * @param appendable the Appendable to write to
     * @param x the XMLGregorianCalendar value to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final XMLGregorianCalendar x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            Dates.formatTo(x, null, null, appendable);
        }
    }

    /**
     * Writes the character representation of an XMLGregorianCalendar to a CharacterWriter.
     * <p>
     * This method handles different date/time formats based on the provided configuration:
     * <ul>
     *   <li>LONG format: writes the epoch milliseconds</li>
     *   <li>ISO_8601_DATE_TIME: writes in ISO 8601 date-time format</li>
     *   <li>ISO_8601_TIMESTAMP: writes in ISO 8601 timestamp format</li>
     *   <li>Default: uses the standard date format</li>
     * </ul>
     * The output may be quoted based on the configuration settings.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharacterWriter writer = new CharacterWriter();
     * JsonXmlSerializationConfig<?> config = JsonXmlSerializationConfig.of();
     * type.writeCharacter(writer, xmlGregorianCalendar, config);   // Writes formatted date/time
     * }</pre>
     *
     * @param writer the CharacterWriter to write to
     * @param x the XMLGregorianCalendar value to write
     * @param config the serialization configuration controlling format and quoting
     * @throws IOException if an I/O error occurs during the write operation
     */
    @SuppressWarnings("null")
    @Override
    public void writeCharacter(final CharacterWriter writer, final XMLGregorianCalendar x, final JsonXmlSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final boolean isQuote = (config != null) && (config.getStringQuotation() != 0) && (config.getDateTimeFormat() != DateTimeFormat.LONG);

            if (isQuote) {
                writer.write(config.getStringQuotation());
            }

            if ((config == null) || (config.getDateTimeFormat() == null)) {
                Dates.formatTo(x, null, null, writer);
            } else {
                switch (config.getDateTimeFormat()) {
                    case LONG:
                        writer.write(String.valueOf(x.toGregorianCalendar().getTimeInMillis()));

                        break;

                    case ISO_8601_DATE_TIME:
                        Dates.formatTo(x, Dates.ISO_8601_DATE_TIME_FORMAT, null, writer);

                        break;

                    case ISO_8601_TIMESTAMP:
                        Dates.formatTo(x, Dates.ISO_8601_TIMESTAMP_FORMAT, null, writer);

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