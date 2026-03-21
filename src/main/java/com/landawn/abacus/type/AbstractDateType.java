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
import java.util.Date;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Dates;

/**
 * The Abstract base class for {@code Date} types in the type system.
 * <p>
 * This class provides common functionality for handling {@code Date} objects and their subclasses,
 * including serialization, formatting, and type checking operations.
 * </p>
 *
 * @param <T> the specific {@code Date} type (e.g., {@link java.util.Date}, {@link java.sql.Date}, {@link java.sql.Timestamp})
 */
public abstract class AbstractDateType<T extends Date> extends AbstractType<T> {

    /**
     * Constructs an {@code AbstractDateType} with the specified type name.
     *
     * @param typeName the name of the date type (e.g., "Date", "Time", "Timestamp")
     */
    protected AbstractDateType(final String typeName) {
        super(typeName);
    }

    /**
     * Checks if this type represents a {@code Date} type.
     * <p>
     * This method always returns {@code true} for Date types.
     * </p>
     *
     * @return {@code true}, indicating this is a {@code Date} type
     */
    @Override
    public boolean isDate() {
        return true;
    }

    /**
     * Checks if this type is comparable.
     * <p>
     * Date types are comparable based on their time values.
     * </p>
     *
     * @return {@code true}, indicating that {@code Date} types support comparison
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Indicates whether values of this type require quoting in CSV format.
     * <p>
     * Date values are typically formatted as date strings that do not require quotes.
     * </p>
     *
     * @return {@code false}, as date values do not require quoting in CSV format
     */
    @Override
    public boolean isCsvQuoteRequired() {
        return false;
    }

    /**
     * Converts a {@code Date} value to its string representation.
     * <p>
     * Uses the default date format provided by {@link Dates#format(Date)}.
     * </p>
     *
     * @param x the {@code Date} value to convert
     * @return the formatted string representation of the date, or {@code null} if input is {@code null}
     */
    @Override
    public String stringOf(final T x) {
        return (x == null) ? null : Dates.format(x);
    }

    /**
     * Appends the string representation of a {@code Date} value to an {@code Appendable}.
     * <p>
     * Writes "null" if the value is {@code null}, otherwise writes the formatted date string.
     * </p>
     *
     * @param appendable the {@code Appendable} to write to
     * @param x the {@code Date} value to append
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
     * Writes a {@code Date} value to a {@code CharacterWriter} with optional configuration.
     * <p>
     * The output format depends on the configuration:
     * </p>
     * <ul>
     *   <li>{@link DateTimeFormat#LONG} - writes the time in milliseconds since epoch</li>
     *   <li>{@link DateTimeFormat#ISO_8601_DATE_TIME} - writes ISO 8601 date-time format</li>
     *   <li>{@link DateTimeFormat#ISO_8601_TIMESTAMP} - writes ISO 8601 timestamp format</li>
     *   <li>Default - uses the standard date format</li>
     * </ul>
     * <p>
     * String quotation is applied based on configuration unless using {@code LONG} format.
     * </p>
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code Date} value to write
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
     * @throws RuntimeException if an unsupported {@code DateTimeFormat} is specified
     */
    @SuppressWarnings("null")
    @Override
    public void writeCharacter(final CharacterWriter writer, final T x, final JsonXmlSerConfig<?> config) throws IOException {
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
                        writer.write(x.getTime());

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
