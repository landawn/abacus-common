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
import java.sql.SQLException;
import java.util.Calendar;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Dates;

/**
 * Abstract base class for Calendar types in the type system.
 * This class provides common functionality for handling Calendar objects,
 * including serialization, database operations, and date formatting.
 *
 * @param <T> the specific Calendar type (e.g., Calendar, GregorianCalendar)
 */
public abstract class AbstractCalendarType<T extends Calendar> extends AbstractType<T> {

    protected AbstractCalendarType(final String typeName) {
        super(typeName);
    }

    /**
     * Checks if this type represents a Calendar type.
     * This method always returns {@code true} for Calendar types.
     *
     * @return {@code true}, indicating this is a Calendar type
     */
    @Override
    public boolean isCalendar() {
        return true;
    }

    /**
     * Checks if this type is comparable.
     * Calendar types are comparable based on their time values.
     *
     * @return {@code true}, indicating that Calendar types support comparison
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Checks if this type represents values that should not be quoted in CSV format.
     * Calendar values are typically formatted as date strings that don't require quotes.
     *
     * @return {@code true}, indicating that Calendar values should not be quoted in CSV format
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     * Converts a Calendar value to its string representation.
     * Uses the default date format provided by {@link Dates#format(Calendar)}.
     *
     * @param calendar the Calendar value to convert
     * @return the formatted string representation of the calendar, or {@code null} if input is {@code null}
     */
    @Override
    public String stringOf(final Calendar calendar) {
        return (calendar == null) ? null : Dates.format(calendar);
    }

    /**
     * Sets a Calendar parameter in a PreparedStatement at the specified position.
     * Converts the Calendar to a Timestamp before setting it in the statement.
     * If the value is {@code null}, sets the parameter to SQL NULL.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the Calendar value to set, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Calendar x) throws SQLException {
        stmt.setTimestamp(columnIndex, (x == null) ? null : Dates.createTimestamp(x));
    }

    /**
     * Sets a Calendar parameter in a CallableStatement using the specified parameter name.
     * Converts the Calendar to a Timestamp before setting it in the statement.
     * If the value is {@code null}, sets the parameter to SQL NULL.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the parameter name
     * @param x the Calendar value to set, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Calendar x) throws SQLException {
        stmt.setTimestamp(parameterName, (x == null) ? null : Dates.createTimestamp(x));
    }

    /**
     * Appends the string representation of a Calendar value to an Appendable.
     * Writes "null" if the value is {@code null}, otherwise writes the formatted date string.
     *
     * @param appendable the Appendable to write to
     * @param x the Calendar value to append
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void appendTo(final Appendable appendable, final T x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            Dates.formatTo(x, appendable);
        }
    }

    /**
     * Writes a Calendar value to a CharacterWriter with optional configuration.
     * The output format depends on the configuration:
     * <ul>
     *   <li>{@link DateTimeFormat#LONG} - writes the time in milliseconds</li>
     *   <li>{@link DateTimeFormat#ISO_8601_DATE_TIME} - writes ISO 8601 date-time format</li>
     *   <li>{@link DateTimeFormat#ISO_8601_TIMESTAMP} - writes ISO 8601 timestamp format</li>
     *   <li>Default - uses the standard date format</li>
     * </ul>
     * String quotation is applied based on configuration unless using LONG format.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Calendar value to write
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
     * @throws RuntimeException if an unsupported DateTimeFormat is specified
     */
    @SuppressWarnings("null")
    @Override
    public void writeCharacter(final CharacterWriter writer, final T x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final boolean isQuote = (config != null) && (config.getStringQuotation() != 0) && (config.getDateTimeFormat() != DateTimeFormat.LONG);

            if (isQuote) {
                writer.write(config.getStringQuotation());
            }

            if ((config == null) || (config.getDateTimeFormat() == null)) {
                Dates.formatTo(x, writer);
            } else {
                switch (config.getDateTimeFormat()) {
                    case LONG:
                        writer.write(x.getTimeInMillis());

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