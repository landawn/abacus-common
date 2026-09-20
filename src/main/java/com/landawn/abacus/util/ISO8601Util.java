/*
 * Copyright (c) 2017, Jackson Authors/Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.zone.ZoneRules;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Internal codec for the legacy ISO-8601 profile used by {@link Dates}.
 *
 * <p>The parser accepts a complete calendar date in basic or extended form, optionally followed by
 * an uppercase {@code T}, a basic or extended hour/minute value, optional seconds in the same style,
 * an optional 1-9 digit fractional second, and an optional UTC/numeric-offset suffix:</p>
 *
 * <pre>{@code
 * date       = yyyy-MM-dd | yyyyMMdd
 * time       = HH:mm[:ss[.fraction]] | HHmm[ss[.fraction]]
 * offset     = Z | (+|-)HH:mm | (+|-)HHmm
 * date-time  = date [T time [offset]]
 * }</pre>
 *
 * <p>All digits are ASCII. Years are four-digit Common Era years from {@code 0001} through
 * {@code 9999}; hours are {@code 00} through {@code 23}; minutes and seconds are {@code 00} through
 * {@code 59}; numeric offsets are from {@code -18:00} through {@code +18:00}. Fractional seconds
 * retain nanosecond precision. A caller that converts the returned {@link Instant} to a legacy
 * millisecond type is responsible for that explicit precision reduction.</p>
 *
 * <p>The date and time components choose their basic/extended form independently, so
 * {@code 20231225T10:30:45} and {@code 2023-12-25T103045} are both accepted; only the separators
 * <i>within</i> one component must agree. Leap seconds ({@code :60}) are rejected, as
 * {@link Instant} cannot represent them.</p>
 *
 * <p>Zone-less values use the immutable zone supplied by the caller, or UTC in the convenience
 * overloads. A date-only value resolves at start of day in that zone. Nonexistent and ambiguous
 * local times are rejected rather than adjusted or resolved implicitly.</p>
 */
final class ISO8601Util {

    private static final ZoneOffset UTC = ZoneOffset.UTC;

    private static final Supplier<ZoneId> UTC_ZONE_SUPPLIER = () -> UTC;

    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final int MAX_FRACTION_DIGITS = 9;

    private ISO8601Util() {
        // Utility class.
    }

    // -------------------------------------------------------------------------
    // Formatting

    /**
     * Formats an instant in UTC without losing fractional-second precision.
     *
     * @param instant the instant to format
     * @return an extended ISO date-time ending in {@code Z}
     * @throws IllegalArgumentException if {@code instant} is {@code null}, or the instant cannot be
     *         represented in the UTC civil-year range {@code 0001}-{@code 9999}
     */
    static String format(final Instant instant) throws IllegalArgumentException {
        return format(instant, UTC);
    }

    /**
     * Formats an instant using the effective offset of {@code zone}. Zero offset is written as
     * {@code Z}; other offsets are written as {@code [+-]HH:mm}. A zero fractional second is omitted,
     * while a non-zero fraction is emitted without losing nanosecond precision.
     *
     * @param instant the instant to format
     * @param zone the zone whose effective offset and civil fields are used
     * @return the formatted extended ISO date-time
     * @throws IllegalArgumentException if {@code instant} or {@code zone} is {@code null}, the instant
     *         cannot be represented as a civil date-time, the civil year is outside {@code 0001}-{@code 9999},
     *         or the effective offset has sub-minute precision
     */
    static String format(final Instant instant, final ZoneId zone) throws IllegalArgumentException {
        N.checkArgNotNull(instant, cs.instant);
        N.checkArgNotNull(zone, cs.zone);

        final ZonedDateTime dateTime;

        try {
            dateTime = instant.atZone(zone);
        } catch (final DateTimeException e) {
            throw new IllegalArgumentException("Instant cannot be represented as an ISO civil date-time in zone " + zone + ": " + instant, e);
        }

        final int year = dateTime.getYear();

        if (year < 1 || year > 9999) {
            throw new IllegalArgumentException("ISO formatting supports Common Era years from 0001 through 9999; got proleptic year " + year);
        }

        final int offsetSeconds = dateTime.getOffset().getTotalSeconds();

        if (offsetSeconds % 60 != 0) {
            throw new IllegalArgumentException("ISO formatting requires a whole-minute UTC offset; got " + dateTime.getOffset());
        }

        return OUTPUT_FORMATTER.format(dateTime.toOffsetDateTime());
    }

    // -------------------------------------------------------------------------
    // Complete parsing

    /**
     * Parses one complete value, interpreting zone-less text as UTC.
     *
     * @param text the text to parse
     * @return the exact parsed instant
     * @throws IllegalArgumentException if {@code text} is {@code null}
     * @throws DateTimeParseException if the text is malformed or has trailing characters
     */
    static Instant parseInstant(final String text) throws IllegalArgumentException, DateTimeParseException {
        N.checkArgNotNull(text, cs.text);
        return parseComplete(text, UTC_ZONE_SUPPLIER);
    }

    /**
     * Parses one complete value, using {@code defaultZone} only when the text has no explicit offset.
     *
     * @param text the text to parse
     * @param defaultZone the immutable zone used for zone-less text
     * @return the exact parsed instant
     * @throws IllegalArgumentException if {@code text} or {@code defaultZone} is {@code null}
     * @throws DateTimeParseException if the text is malformed, resolves to a gap or overlap, or has
     *         trailing characters
     */
    static Instant parseInstant(final String text, final ZoneId defaultZone) throws IllegalArgumentException, DateTimeParseException {
        N.checkArgNotNull(text, cs.text);
        N.checkArgNotNull(defaultZone, cs.defaultZone);
        return parseComplete(text, () -> defaultZone);
    }

    /**
     * Legacy-boundary variant whose supplier is evaluated only for zone-less text. The caller must
     * capture mutable state before calling this method. The supplier must return a non-null immutable
     * zone. Exceptions thrown by the supplier propagate unchanged.
     *
     * @param text the text to parse
     * @param defaultZoneSupplier a lazy fallback-zone supplier
     * @return the exact parsed instant
     * @throws IllegalArgumentException if {@code text} or {@code defaultZoneSupplier} is {@code null}
     * @throws NullPointerException if the supplier returns {@code null} when the text needs a fallback zone
     * @throws DateTimeParseException if the text is malformed, resolves to a gap or overlap, or has
     *         trailing characters
     */
    static Instant parseInstantWithDefaultZone(final String text, final Supplier<? extends ZoneId> defaultZoneSupplier)
            throws IllegalArgumentException, NullPointerException, DateTimeParseException {
        N.checkArgNotNull(text, cs.text);
        N.checkArgNotNull(defaultZoneSupplier, cs.defaultZoneSupplier);
        return parseComplete(text, defaultZoneSupplier);
    }

    /**
     * @throws DateTimeParseException if the input has invalid ISO fields, an ambiguous or nonexistent local time, or trailing characters
     * @throws NullPointerException if no explicit offset is present and the default-zone supplier returns {@code null}
     */
    private static Instant parseComplete(final String text, final Supplier<? extends ZoneId> defaultZoneSupplier)
            throws DateTimeParseException, NullPointerException {
        final Parsed parsed;

        try {
            parsed = parsePrefix(text, 0, defaultZoneSupplier);
        } catch (final ParseFailure e) {
            throw parseException(text, e);
        }

        if (parsed.endIndex != text.length()) {
            throw new DateTimeParseException("Unexpected trailing characters", text, parsed.endIndex);
        }

        return parsed.instant;
    }

    // -------------------------------------------------------------------------
    // Prefix parsing

    /**
     * Parses one value prefix from {@code text}, interpreting zone-less text as UTC.
     *
     * <p>This follows the JDK {@link ParsePosition} convention. On success, {@code index} is advanced
     * to the first unconsumed character and {@code errorIndex} is reset to {@code -1}. On malformed
     * input, this returns {@code null}, leaves {@code index} unchanged, and records the failure in
     * {@code errorIndex}. An invalid initial index throws {@link IndexOutOfBoundsException}.</p>
     *
     * @param text the text containing an ISO value prefix
     * @param position the starting position and output cursor
     * @return the parsed instant, or {@code null} on malformed input
     * @throws IllegalArgumentException if {@code text} or {@code position} is {@code null}
     * @throws IndexOutOfBoundsException if the initial position is outside the input
     */
    static Instant parseInstant(final String text, final ParsePosition position) throws IllegalArgumentException, IndexOutOfBoundsException {
        return parseInstant(text, position, UTC_ZONE_SUPPLIER);
    }

    /**
     * Parses one prefix with an explicit immutable fallback zone for zone-less text.
     *
     * @param text the text containing an ISO value prefix
     * @param position the starting position and output cursor
     * @param defaultZone the immutable zone used for zone-less text
     * @return the parsed instant, or {@code null} on malformed input
     * @throws IllegalArgumentException if {@code text}, {@code position}, or {@code defaultZone} is {@code null}
     * @throws IndexOutOfBoundsException if the initial position is outside the input
     */
    static Instant parseInstant(final String text, final ParsePosition position, final ZoneId defaultZone)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkArgNotNull(text, cs.text);
        N.checkArgNotNull(position, cs.position);
        N.checkArgNotNull(defaultZone, cs.defaultZone);
        return parseInstant(text, position, () -> defaultZone);
    }

    /**
     * @throws IllegalArgumentException if {@code text} or {@code position} is {@code null}
     * @throws NullPointerException if an offset-free input causes the default-zone supplier to return {@code null}
     * @throws IndexOutOfBoundsException if the initial parse position is negative or greater than the input length
     */
    private static Instant parseInstant(final String text, final ParsePosition position, final Supplier<? extends ZoneId> defaultZoneSupplier)
            throws IllegalArgumentException, NullPointerException, IndexOutOfBoundsException {
        N.checkArgNotNull(text, cs.text);
        N.checkArgNotNull(position, cs.position);

        final int start = position.getIndex();

        if (start < 0 || start > text.length()) {
            throw new IndexOutOfBoundsException("ParsePosition index " + start + " is outside input length " + text.length());
        }

        try {
            final Parsed parsed = parsePrefix(text, start, defaultZoneSupplier);
            position.setIndex(parsed.endIndex);
            position.setErrorIndex(-1);
            return parsed.instant;
        } catch (final ParseFailure e) {
            position.setErrorIndex(e.errorIndex);
            return null;
        }
    }

    /**
     * Parses one value starting at {@code start}, returning the resolved instant and the index of the
     * first unconsumed character. The fallback-zone supplier is consulted only when the value carries
     * no explicit offset.
     * @throws ParseFailure if the ISO date, time, fraction, or offset is malformed, or an offset-free local time falls in a zone gap or overlap
     * @throws NullPointerException if an offset-free input causes the default-zone supplier to return {@code null}
     */
    private static Parsed parsePrefix(final String text, final int start, final Supplier<? extends ZoneId> defaultZoneSupplier)
            throws ParseFailure, NullPointerException {
        int offset = start;

        final int yearIndex = offset;
        final int year = parseDigits(text, offset, 4, "four-digit year");
        offset += 4;

        if (year == 0) {
            throw failure(yearIndex, "Year must be in the range 0001 through 9999");
        }

        final boolean separatedDate = hasChar(text, offset, '-');

        if (separatedDate) {
            offset++;
        }

        final int monthIndex = offset;
        final int month = parseDigits(text, offset, 2, "two-digit month");
        offset += 2;

        if (hasChar(text, offset, '-') != separatedDate) {
            throw failure(offset, "Inconsistent date separators");
        }

        if (separatedDate) {
            offset++;
        }

        final int dayIndex = offset;
        final int day = parseDigits(text, offset, 2, "two-digit day");
        offset += 2;

        final LocalDate localDate;

        try {
            localDate = LocalDate.of(year, month, day);
        } catch (final DateTimeException e) {
            final int errorIndex = month < 1 || month > 12 ? monthIndex : dayIndex;
            throw failure(errorIndex, "Invalid calendar date", e);
        }

        if (!hasChar(text, offset, 'T')) {
            return new Parsed(resolveStrict(localDate.atStartOfDay(), suppliedDefaultZone(defaultZoneSupplier), dayIndex), offset);
        }

        offset++;
        final int hourIndex = offset;
        final int hour = parseDigits(text, offset, 2, "two-digit hour");
        offset += 2;

        if (hour > 23) {
            throw failure(hourIndex, "Hour must be in the range 00 through 23");
        }

        final boolean separatedTime = hasChar(text, offset, ':');

        if (separatedTime) {
            offset++;
        }

        final int minuteIndex = offset;
        final int minute = parseDigits(text, offset, 2, "two-digit minute");
        offset += 2;

        if (minute > 59) {
            throw failure(minuteIndex, "Minute must be in the range 00 through 59");
        }

        boolean hasSeconds = false;

        if (separatedTime) {
            if (hasChar(text, offset, ':')) {
                hasSeconds = true;
                offset++;
            } else if (hasAsciiDigit(text, offset)) {
                throw failure(offset, "Seconds in an extended time must be preceded by ':'");
            }
        } else if (hasChar(text, offset, ':')) {
            throw failure(offset, "A compact time must not contain ':' before seconds");
        } else if (hasAsciiDigit(text, offset)) {
            hasSeconds = true;
        }

        int second = 0;
        int nano = 0;

        if (hasSeconds) {
            final int secondIndex = offset;
            second = parseDigits(text, offset, 2, "two-digit second");
            offset += 2;

            if (second > 59) {
                throw failure(secondIndex, "Second must be in the range 00 through 59");
            }

            if (hasChar(text, offset, '.')) {
                final int fractionIndex = ++offset;

                while (hasAsciiDigit(text, offset)) {
                    offset++;
                }

                final int fractionDigits = offset - fractionIndex;

                if (fractionDigits == 0) {
                    throw failure(fractionIndex, "At least one fractional-second digit is required after '.'");
                }

                if (fractionDigits > MAX_FRACTION_DIGITS) {
                    throw failure(fractionIndex + MAX_FRACTION_DIGITS, "At most nine fractional-second digits are supported");
                }

                nano = parseDigits(text, fractionIndex, fractionDigits, "fractional second");

                for (int i = fractionDigits; i < MAX_FRACTION_DIGITS; i++) {
                    nano *= 10;
                }
            }
        } else if (hasChar(text, offset, '.')) {
            throw failure(offset, "Fractional seconds require an explicit seconds field");
        }

        ZoneOffset explicitOffset = null;

        if (hasChar(text, offset, 'Z')) {
            explicitOffset = UTC;
            offset++;
        } else if (hasChar(text, offset, '+') || hasChar(text, offset, '-')) {
            final int offsetIndex = offset;
            final int sign = text.charAt(offset++) == '-' ? -1 : 1;
            final int offsetHour = parseDigits(text, offset, 2, "two-digit offset hour");
            offset += 2;

            if (hasChar(text, offset, ':')) {
                offset++;
            }

            final int offsetMinute = parseDigits(text, offset, 2, "two-digit offset minute");
            offset += 2;

            if (offsetMinute > 59 || offsetHour > 18 || offsetHour == 18 && offsetMinute != 0) {
                throw failure(offsetIndex, "UTC offset must be in the range -18:00 through +18:00");
            }

            try {
                explicitOffset = ZoneOffset.ofHoursMinutes(sign * offsetHour, sign * offsetMinute);
            } catch (final DateTimeException e) {
                throw failure(offsetIndex, "Invalid UTC offset", e);
            }
        }

        final LocalDateTime localDateTime = localDate.atTime(hour, minute, second, nano);
        final Instant instant = explicitOffset == null ? resolveStrict(localDateTime, suppliedDefaultZone(defaultZoneSupplier), hourIndex)
                : localDateTime.toInstant(explicitOffset);
        return new Parsed(instant, offset);
    }

    /**
     * @throws NullPointerException if {@code defaultZoneSupplier} returns {@code null}
     */
    private static ZoneId suppliedDefaultZone(final Supplier<? extends ZoneId> defaultZoneSupplier) throws NullPointerException {
        return Objects.requireNonNull(defaultZoneSupplier.get(), "defaultZoneSupplier returned null");
    }

    /**
     * @throws ParseFailure if the local date-time has no valid offset or has multiple valid offsets in the specified zone
     */
    private static Instant resolveStrict(final LocalDateTime localDateTime, final ZoneId zone, final int errorIndex) throws ParseFailure {
        final ZoneRules rules = zone.getRules();
        final List<ZoneOffset> validOffsets = rules.getValidOffsets(localDateTime);

        if (validOffsets.isEmpty()) {
            throw failure(errorIndex, "Nonexistent local date-time " + localDateTime + " in zone " + zone + " (DST gap)");
        }

        if (validOffsets.size() > 1) {
            throw failure(errorIndex, "Ambiguous local date-time " + localDateTime + " in zone " + zone + " (DST overlap); valid offsets are " + validOffsets);
        }

        return localDateTime.toInstant(validOffsets.get(0));
    }

    /**
     * @throws ParseFailure if the requested digit span is invalid, incomplete, or contains a non-ASCII digit
     */
    private static int parseDigits(final String text, final int index, final int digitCount, final String description) throws ParseFailure {
        if (index < 0 || digitCount < 1 || index > text.length() - digitCount) {
            throw failure(Math.max(0, Math.min(index, text.length())), "Expected " + description);
        }

        int result = 0;

        for (int i = index; i < index + digitCount; i++) {
            final char ch = text.charAt(i);

            if (ch < '0' || ch > '9') {
                throw failure(i, "Expected ASCII digit in " + description);
            }

            result = result * 10 + ch - '0';
        }

        return result;
    }

    private static boolean hasChar(final String text, final int index, final char expected) {
        return index >= 0 && index < text.length() && text.charAt(index) == expected;
    }

    private static boolean hasAsciiDigit(final String text, final int index) {
        return index >= 0 && index < text.length() && text.charAt(index) >= '0' && text.charAt(index) <= '9';
    }

    private static ParseFailure failure(final int errorIndex, final String message) {
        return new ParseFailure(errorIndex, message, null);
    }

    private static ParseFailure failure(final int errorIndex, final String message, final Throwable cause) {
        return new ParseFailure(errorIndex, message, cause);
    }

    private static DateTimeParseException parseException(final String text, final ParseFailure failure) {
        return new DateTimeParseException(failure.getMessage(), text, failure.errorIndex, failure.getCause());
    }

    private static final class Parsed {
        final Instant instant;
        final int endIndex;

        Parsed(final Instant instant, final int endIndex) {
            this.instant = instant;
            this.endIndex = endIndex;
        }
    }

    /**
     * Internal control-flow signal carrying the failure index. It never escapes this class: callers
     * either translate it into a {@link DateTimeParseException} (propagating only its message and
     * cause) or report it through a {@link ParsePosition}, so its own stack trace is never observed.
     */
    private static final class ParseFailure extends Exception {
        private static final long serialVersionUID = 1L;

        final int errorIndex;

        ParseFailure(final int errorIndex, final String message, final Throwable cause) {
            super(message, cause);
            this.errorIndex = errorIndex;
        }
    }
}
