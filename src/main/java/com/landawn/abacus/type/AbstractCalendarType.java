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

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Dates;

/**
 * The abstract base class for {@code Calendar} types in the type system.
 * <p>
 * This class provides common functionality for handling {@code java.util.Calendar} objects,
 * including string formatting, JDBC write operations (storing as SQL {@code TIMESTAMP}),
 * and JSON/XML serialization with configurable date-time formats.
 * </p>
 *
 * @param <T> the specific {@code Calendar} subtype (e.g., {@link java.util.Calendar}, {@link java.util.GregorianCalendar})
 * @see CalendarType
 * @see GregorianCalendarType
 */
public abstract class AbstractCalendarType<T extends Calendar> extends AbstractType<T> {

    /**
     * Constructs a new {@code AbstractCalendarType} with the specified type name.
     *
     * @param typeName the name of the {@code Calendar} type (e.g., "Calendar", "GregorianCalendar")
     */
    protected AbstractCalendarType(final String typeName) {
        super(typeName);
    }

    /**
     * Returns {@code true} because this type represents a {@code Calendar} type.
     *
     * @return {@code true}
     */
    @Override
    public boolean isCalendar() {
        return true;
    }

    /**
     * Returns {@code true} because {@code Calendar} types are comparable.
     *
     * @return {@code true}
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Returns {@code false} because calendar values do not require quoting in CSV format.
     *
     * @return {@code false}
     */
    @Override
    public boolean isCsvQuoteRequired() {
        return false;
    }

    /**
     * Converts the specified {@code Calendar} value to its string representation.
     * Uses the default date format provided by {@link Dates#format(Calendar)}.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param calendar the {@code Calendar} value to convert
     * @return the formatted string representation of the calendar, or {@code null} if the input is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Calendar calendar) {
        return (calendar == null) ? null : Dates.format(calendar);
    }

    /**
     * Sets the specified {@code Calendar} parameter in a {@code PreparedStatement} at the given position.
     * <p>Converts the {@code Calendar} to a {@code Timestamp} before setting it in the statement.</p>
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the {@code Calendar} value to set, or {@code null} for SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Calendar x) throws SQLException {
        stmt.setTimestamp(columnIndex, (x == null) ? null : Dates.createTimestamp(x));
    }

    /**
     * Sets the specified {@code Calendar} parameter in a {@code CallableStatement} using the given parameter name.
     * <p>Converts the {@code Calendar} to a {@code Timestamp} before setting it in the statement.</p>
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the parameter name
     * @param x the {@code Calendar} value to set, or {@code null} for SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Calendar x) throws SQLException {
        stmt.setTimestamp(parameterName, (x == null) ? null : Dates.createTimestamp(x));
    }

    /**
     * Appends the string representation of the specified {@code Calendar} value to the given {@code Appendable}.
     * Writes "null" if the value is {@code null}, otherwise writes the formatted date string.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the {@code Appendable} to write to
     * @param x the {@code Calendar} value to append
     * @throws IOException if an I/O error occurs
     * @implNote
     * This method appends a string representation of {@code x} to {@code appendable} (the literal {@code "null"} for a
     * {@code null} value). Conceptually this is the human-readable form produced by {@code toString()}, <i>not</i> the
     * value returned by {@code stringOf}, which is a formatted, serializable representation (typically a JSON string)
     * that {@link #valueOf(String)} can convert back into an equivalent value. For values whose nested structure makes
     * the two forms differ (collections, maps, arrays), {@code appendTo} emits the unquoted, {@code toString()}-style
     * form; it is therefore not, in the general contract, a plain
     * {@code appendable.append(x == null ? NULL_STRING : stringOf(x))}. (For value types whose human-readable and
     * serialized forms coincide, the appended text is naturally identical to {@code stringOf(x)}.)
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
     * Writes the specified {@code Calendar} value to the given {@code CharacterWriter} with optional configuration.
     * The output format depends on the configuration:
     * <ul>
     *   <li>{@link DateTimeFormat#LONG} - writes the time in milliseconds.</li>
     *   <li>{@link DateTimeFormat#ISO_8601_DATE_TIME} - writes ISO 8601 date-time format.</li>
     *   <li>{@link DateTimeFormat#ISO_8601_TIMESTAMP} - writes ISO 8601 timestamp format.</li>
     *   <li>Default - uses the standard date format.</li>
     * </ul>
     * String quotation is applied based on configuration unless using {@code LONG} format.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes the serialized form of {@code x} to the
     * {@code CharacterWriter}, applying string quotation and character escaping according to the supplied serialization
     * config (a {@code null} config means no surrounding quotation). It is the streaming counterpart of {@code stringOf}
     * and is invoked by the JSON/XML serializers.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML (quoted and escaped),
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code Calendar} value to write
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
     * @throws RuntimeException if an unsupported {@code DateTimeFormat} is specified
     */
    @SuppressWarnings("null")
    @Override
    public void serializeTo(final CharacterWriter writer, final T x, final JsonXmlSerConfig<?> config) throws IOException {
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
