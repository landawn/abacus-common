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

import org.joda.time.base.AbstractInstant;
import org.joda.time.format.DateTimeFormatter;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Dates;

/**
 * Abstract base class for Joda-Time DateTime types in the type system.
 * This class provides common functionality for handling Joda-Time instant types
 * (such as DateTime, Instant) including serialization and formatting operations.
 * It uses pre-configured DateTimeFormatters for ISO 8601 date formats.
 *
 * @param <T> the specific Joda-Time instant type (e.g., DateTime, Instant)
 */
public abstract class AbstractJodaDateTimeType<T extends AbstractInstant> extends AbstractType<T> {

    /** Pre-configured Joda DateTimeFormatter for ISO 8601 date-time format */
    protected static final DateTimeFormatter jodaISO8601DateTimeFT = org.joda.time.format.DateTimeFormat.forPattern(Dates.ISO_8601_DATE_TIME_FORMAT);

    /** Pre-configured Joda DateTimeFormatter for ISO 8601 timestamp format */
    protected static final DateTimeFormatter jodaISO8601TimestampFT = org.joda.time.format.DateTimeFormat.forPattern(Dates.ISO_8601_TIMESTAMP_FORMAT);

    protected AbstractJodaDateTimeType(final String typeName) {
        super(typeName);
    }

    /**
     * Checks if this type represents a Joda DateTime type.
     * This method always returns {@code true} for Joda DateTime types.
     *
     * @return {@code true}, indicating this is a Joda DateTime type
     */
    @Override
    public boolean isJodaDateTime() {
        return true;
    }

    /**
     * Checks if this type is comparable.
     * Joda DateTime types are comparable based on their millisecond instant values.
     *
     * @return {@code true}, indicating that Joda DateTime types support comparison
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Checks if this type represents values that should not be quoted in CSV format.
     * DateTime values are typically formatted as timestamp strings that don't require quotes.
     *
     * @return {@code true}, indicating that Joda DateTime values should not be quoted in CSV format
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     * Converts a Joda DateTime value to its string representation.
     * Uses the ISO 8601 timestamp format for consistent serialization.
     *
     * @param x the Joda DateTime instant value to convert
     * @return the ISO 8601 timestamp string representation, or {@code null} if input is {@code null}
     */
    @Override
    public String stringOf(final T x) {
        return (x == null) ? null : jodaISO8601TimestampFT.print(x);
    }

    /**
     * Appends the string representation of a Joda DateTime value to an Appendable.
     * Writes "null" if the value is {@code null}, otherwise writes the ISO 8601 timestamp format.
     *
     * @param appendable the Appendable to write to
     * @param x the Joda DateTime instant value to append
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void appendTo(final Appendable appendable, final T x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            jodaISO8601TimestampFT.printTo(appendable, x);
        }
    }

    /**
     * Writes a Joda DateTime value to a CharacterWriter with optional configuration.
     * The output format depends on the configuration:
     * <ul>
     *   <li>{@link DateTimeFormat#LONG} - writes the time in milliseconds since epoch</li>
     *   <li>{@link DateTimeFormat#ISO_8601_DATE_TIME} - writes ISO 8601 date-time format</li>
     *   <li>{@link DateTimeFormat#ISO_8601_TIMESTAMP} - writes ISO 8601 timestamp format</li>
     *   <li>Default - uses ISO 8601 timestamp format</li>
     * </ul>
     * String quotation is applied based on configuration unless using LONG format.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Joda DateTime instant value to write
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
                jodaISO8601TimestampFT.printTo(writer, x);
            } else {
                switch (config.getDateTimeFormat()) {
                    case LONG:
                        writer.write(x.getMillis());

                        break;

                    case ISO_8601_DATE_TIME:
                        jodaISO8601DateTimeFT.printTo(writer, x);

                        break;

                    case ISO_8601_TIMESTAMP:
                        jodaISO8601TimestampFT.printTo(writer, x);

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
