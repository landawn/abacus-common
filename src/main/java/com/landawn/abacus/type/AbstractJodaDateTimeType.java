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

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Dates;

/**
 * The abstract base class for Joda-Time instant types in the type system.
 * <p>
 * This class provides common functionality for handling Joda-Time instant-based types
 * (such as {@code org.joda.time.DateTime} and {@code org.joda.time.Instant}),
 * including serialization, deserialization, and output formatting operations.
 * It uses pre-configured {@link org.joda.time.format.DateTimeFormatter}s for
 * ISO 8601 date-time and timestamp formats.
 * </p>
 *
 * @param <T> the specific Joda-Time instant subtype (e.g., {@link org.joda.time.DateTime},
 *            {@link org.joda.time.Instant})
 */
public abstract class AbstractJodaDateTimeType<T extends AbstractInstant> extends AbstractType<T> {

    /** Pre-configured Joda {@code DateTimeFormatter} for ISO 8601 date-time format */
    protected static final DateTimeFormatter jodaISO8601DateTimeFT = org.joda.time.format.DateTimeFormat.forPattern(Dates.ISO_8601_DATE_TIME_FORMAT);

    /** Pre-configured Joda {@code DateTimeFormatter} for ISO 8601 timestamp format */
    protected static final DateTimeFormatter jodaISO8601TimestampFT = org.joda.time.format.DateTimeFormat.forPattern(Dates.ISO_8601_TIMESTAMP_FORMAT);

    /**
     * Constructs an {@code AbstractJodaDateTimeType} with the specified type name.
     *
     * @param typeName the name of the Joda DateTime type (e.g., "DateTime", "Instant")
     */
    protected AbstractJodaDateTimeType(final String typeName) {
        super(typeName);
    }

    /**
     * Returns {@code true} because this type represents a Joda {@code DateTime} type.
     *
     * @return {@code true}
     */
    @Override
    public boolean isJodaDateTime() {
        return true;
    }

    /**
     * Returns {@code true} because Joda {@code DateTime} types are comparable based on
     * their millisecond instant values.
     *
     * @return {@code true}
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Returns {@code false} because Joda {@code DateTime} values do not require quoting in CSV format.
     *
     * @return {@code false}
     */
    @Override
    public boolean isCsvQuoteRequired() {
        return false;
    }

    /**
     * Converts a Joda {@code DateTime} value to its string representation.
     * <p>
     * Uses the ISO 8601 timestamp format for consistent serialization.
     * </p>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the Joda {@code DateTime} instant value to convert
     * @return the ISO 8601 timestamp string representation, or {@code null} if input is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final T x) {
        return (x == null) ? null : jodaISO8601TimestampFT.print(x);
    }

    /**
     * Appends the string representation of a Joda {@code DateTime} value to an {@code Appendable}.
     * <p>
     * Writes {@code "null"} if the value is {@code null}, otherwise writes the ISO 8601 timestamp format.
     * </p>
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the {@code Appendable} to write to
     * @param x the Joda {@code DateTime} instant value to append
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
            jodaISO8601TimestampFT.printTo(appendable, x);
        }
    }

    /**
     * Writes a Joda {@code DateTime} value to a {@code CharacterWriter} with optional configuration.
     * <p>
     * The output format depends on the configuration:
     * </p>
     * <ul>
     *   <li>{@link DateTimeFormat#LONG} - writes the time in milliseconds since epoch</li>
     *   <li>{@link DateTimeFormat#ISO_8601_DATE_TIME} - writes ISO 8601 date-time format</li>
     *   <li>{@link DateTimeFormat#ISO_8601_TIMESTAMP} - writes ISO 8601 timestamp format</li>
     *   <li>Default - uses ISO 8601 timestamp format</li>
     * </ul>
     * <p>
     * String quotation is applied based on configuration unless using {@code LONG} format.
     * </p>
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
     * @param x the Joda {@code DateTime} instant value to write
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
