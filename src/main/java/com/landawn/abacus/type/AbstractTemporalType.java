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
 * Abstract base class for temporal type handling in the type system.
 * This class provides the foundation for date/time types that implement the Temporal interface,
 * including shared formatters and configuration for temporal serialization/deserialization.
 *
 * @param <T> the specific temporal type that extends java.time.temporal.Temporal
 */
public abstract class AbstractTemporalType<T extends Temporal> extends AbstractType<T> {

    /**
     * The default time zone ID used for temporal operations.
     * This is initialized from the Dates utility class default zone.
     */
    protected static final ZoneId DEFAULT_ZONE_ID = Dates.DEFAULT_ZONE_ID;

    /**
     * DateTimeFormatter for ISO 8601 date-time format with offset.
     * <p>
     * This formatter adheres to the ISO-8601 standard for representing date and time with
     * an offset from UTC (e.g., "2023-04-15T14:30:45+01:00"). It's used for consistent
     * formatting and parsing of temporal values throughout the type system.
     * <p>
     * The formatter uses the standard {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME} which
     * formats or parses a date-time with an offset, such as <i>2011-12-03T10:15:30+01:00</i>.
     * <p>
     * This field is protected and shared across subclasses to ensure consistent handling
     * of temporal data in ISO format.
     *
     * @see DateTimeFormatter#ISO_OFFSET_DATE_TIME
     * @see #iso8601TimestampDTF
     * @see java.time.format.DateTimeFormatter
     */
    protected static final DateTimeFormatter iso8601DateTimeDTF = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * DateTimeFormatter for ISO 8601 date-time format with offset.
     * <p>
     * This formatter adheres to the ISO-8601 standard for representing date and time with
     * an offset from UTC (e.g., "2023-04-15T14:30:45+01:00"). It's used for consistent
     * formatting and parsing of temporal values throughout the type system.
     * <p>
     * The formatter uses the standard {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME} which
     * formats or parses a date-time with an offset, such as <i>2011-12-03T10:15:30+01:00</i>.
     * <p>
     * This field is protected and shared across subclasses to ensure consistent handling
     * of temporal data in ISO format.
     *
     * @see DateTimeFormatter#ISO_OFFSET_DATE_TIME
     * @see #iso8601TimestampDTF
     * @see java.time.format.DateTimeFormatter
     */
    protected static final DateTimeFormatter iso8601TimestampDTF = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * Constructs an AbstractTemporalType with the specified type name.
     *
     * @param typeName the name of the temporal type (e.g., "LocalDate", "LocalDateTime", "Instant")
     */
    protected AbstractTemporalType(final String typeName) {
        super(typeName);
    }

    /**
     * Determines whether this temporal type should be quoted in CSV format.
     * Temporal types are typically not quoted in CSV files as they represent
     * structured date/time values that don't contain CSV delimiters.
     *
     * @return {@code true} indicating that temporal values should not be quoted in CSV format
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }
}