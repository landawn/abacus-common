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

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;

/**
 * The abstract base class for temporal type handling in the type system.
 * <p>
 * This class provides the foundation for date/time types that implement the {@link Temporal} interface,
 * including shared formatters and configuration for temporal serialization/deserialization.
 * </p>
 *
 * @param <T> the specific temporal type that extends {@link java.time.temporal.Temporal}
 */
public abstract class AbstractTemporalType<T extends Temporal> extends AbstractType<T> {

    /**
     * The default time zone ID used for temporal operations.
     * <p>
     * This is initialized from the JVM default zone.
     * </p>
     */
    protected static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();

    /**
     * {@link DateTimeFormatter} for the ISO 8601 date-time format with UTC offset.
     * <p>
     * Emits whole-second precision and an offset, such as
     * {@code 2011-12-03T10:15:30+01:00}; UTC is emitted as {@code Z}.
     * Used by subclasses when the {@link com.landawn.abacus.util.DateTimeFormat#ISO_8601_DATE_TIME}
     * serialization option is selected, and as the parsing fast path for that shape. Parsing resolves with
     * {@link ResolverStyle#STRICT}, so an impossible calendar value ({@code 2023-02-30}, {@code 2023-04-31},
     * {@code 2023-02-29}, {@code 24:00}) is rejected instead of being rounded to a nearby valid date, exactly as
     * {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}, {@link DateTimeFormatter#ISO_ZONED_DATE_TIME} and the legacy
     * {@code Date}/{@code Calendar} handlers do. ({@link DateTimeFormatter#ISO_INSTANT}, which
     * {@code InstantType} falls back to, still accepts {@code 24:00} by JDK design.)
     * </p>
     *
     * @see #iso8601TimestampDTF
     */
    // STRICT: DateTimeFormatterBuilder.toFormatter() defaults to SMART, which silently moved Feb 30 -> Feb 28,
    // Apr 31 -> Apr 30, 24:00 -> next-day midnight on the very shapes stringOf/serializeTo emit, while the same text
    // one character longer or shorter went to the STRICT JDK parsers and was rejected. Safe here because the date part
    // is ISO_LOCAL_DATE (proleptic YEAR, not the era-bound 'yyyy'), so STRICT needs no era; formatting is unaffected.
    protected static final DateTimeFormatter iso8601DateTimeDTF = new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendPattern("HH:mm:ss")
            .appendOffsetId()
            .toFormatter()
            .withResolverStyle(ResolverStyle.STRICT);

    /**
     * {@link DateTimeFormatter} for the ISO 8601 timestamp format with UTC offset.
     * <p>
     * Emits exactly millisecond precision and an offset, such as
     * {@code 2011-12-03T10:15:30.123+01:00}; UTC is emitted as {@code Z}.
     * Used by subclasses when the {@link com.landawn.abacus.util.DateTimeFormat#ISO_8601_TIMESTAMP}
     * serialization option is selected, and as the parsing fast path for that shape. Parsing resolves with
     * {@link ResolverStyle#STRICT} (impossible calendar values are rejected), see {@link #iso8601DateTimeDTF}.
     * </p>
     *
     * @see #iso8601DateTimeDTF
     */
    protected static final DateTimeFormatter iso8601TimestampDTF = new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendPattern("HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 3, 3, true)
            .appendOffsetId()
            .toFormatter()
            .withResolverStyle(ResolverStyle.STRICT);

    /**
     * Constructs an {@code AbstractTemporalType} with the specified type name.
     *
     * @param typeName the name of the temporal type (e.g., "LocalDate", "LocalDateTime", "Instant")
     * @throws IllegalArgumentException if {@code typeName} is {@code null}.
     */
    protected AbstractTemporalType(final String typeName) throws IllegalArgumentException {
        super(typeName);
    }

    /**
     * Indicates whether values of this type require quoting in CSV format.
     * <p>
     * Temporal types represent structured date/time values that do not contain CSV delimiters.
     * </p>
     *
     * @return {@code false}, as temporal values do not require quoting in CSV format
     */
    @Override
    public boolean isCsvQuoteRequired() {
        return false;
    }

    /**
     * Indicates whether values of this type are comparable.
     * <p>
     * The temporal implementations supported by this hierarchy (e.g., {@code Instant}, {@code LocalDate},
     * {@code LocalDateTime}, {@code OffsetDateTime}, {@code ZonedDateTime}) implement {@link Comparable},
     * so values of this type are naturally orderable.
     * </p>
     *
     * @return {@code true}, as temporal values implement {@link Comparable}
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Indicates whether this type is a {@link java.time.temporal.Temporal} type.
     * <p>
     * Every type in this hierarchy represents a {@code Temporal} value (e.g., {@code Instant},
     * {@code LocalDate}, {@code LocalDateTime}, {@code LocalTime}, {@code OffsetDateTime}, {@code ZonedDateTime}).
     * </p>
     *
     * @return {@code true}, as all types in this hierarchy implement {@link Temporal}
     */
    @Override
    public boolean isTemporal() {
        return true;
    }
}
