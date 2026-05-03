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
import java.time.temporal.Temporal;

import com.landawn.abacus.util.Dates;

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
     * This is initialized from the {@code Dates} utility class default zone.
     * </p>
     */
    protected static final ZoneId DEFAULT_ZONE_ID = Dates.DEFAULT_ZONE_ID;

    /**
     * {@link DateTimeFormatter} for the ISO 8601 date-time format with UTC offset.
     * <p>
     * Backed by {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}, which formats and parses
     * date-times with an offset such as {@code 2011-12-03T10:15:30+01:00}.
     * Used by subclasses when the {@link com.landawn.abacus.util.DateTimeFormat#ISO_8601_DATE_TIME}
     * serialization option is selected.
     * </p>
     *
     * @see DateTimeFormatter#ISO_OFFSET_DATE_TIME
     * @see #iso8601TimestampDTF
     */
    protected static final DateTimeFormatter iso8601DateTimeDTF = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * {@link DateTimeFormatter} for the ISO 8601 timestamp format with UTC offset.
     * <p>
     * Backed by {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}, which formats and parses
     * date-times with an offset such as {@code 2011-12-03T10:15:30.999+01:00}.
     * Used by subclasses when the {@link com.landawn.abacus.util.DateTimeFormat#ISO_8601_TIMESTAMP}
     * serialization option is selected. Kept separate from {@link #iso8601DateTimeDTF} to
     * allow independent customization if finer granularity is needed in the future.
     * </p>
     *
     * @see DateTimeFormatter#ISO_OFFSET_DATE_TIME
     * @see #iso8601DateTimeDTF
     */
    protected static final DateTimeFormatter iso8601TimestampDTF = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * Constructs an {@code AbstractTemporalType} with the specified type name.
     *
     * @param typeName the name of the temporal type (e.g., "LocalDate", "LocalDateTime", "Instant")
     */
    protected AbstractTemporalType(final String typeName) {
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
     * All {@link java.time.temporal.Temporal} subtypes (e.g., {@code Instant}, {@code LocalDate},
     * {@code LocalDateTime}, {@code OffsetDateTime}, {@code ZonedDateTime}) implement
     * {@link Comparable}, so values of this type are naturally orderable.
     * </p>
     *
     * @return {@code true}, as temporal values implement {@link Comparable}
     */
    @Override
    public boolean isComparable() {
        return true;
    }
}
