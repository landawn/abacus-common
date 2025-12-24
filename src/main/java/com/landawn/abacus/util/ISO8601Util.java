/*
 * Copyright (c) 2017,  Jackson Authors/Contributors.
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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import com.landawn.abacus.annotation.SuppressFBWarnings;

/**
 * Utility class for parsing and formatting dates in ISO8601 format.
 * 
 * <p>This class provides fast and GC-friendly methods for handling ISO8601 date/time strings,
 * making it much more efficient than using SimpleDateFormat, especially when processing
 * large numbers of date objects.</p>
 * 
 * <p>Supported parse format: {@code [yyyy-MM-dd|yyyyMMdd][T(hh:mm[:ss[.sss]]|hhmm[ss[.sss]])]?[Z|[+-]hh[:]mm]}</p>
 * 
 * <p>Note: This class is adapted from Jackson's date utilities.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Formatting
 * Date date = new Date();
 * String iso = ISO8601Util.format(date);   // "2023-12-25T10:30:45Z"
 * String isoWithMillis = ISO8601Util.format(date, true);   // "2023-12-25T10:30:45.123Z"
 * 
 * // Parsing
 * Date parsed = ISO8601Util.parse("2023-12-25T10:30:45.123Z");
 * Date parsed2 = ISO8601Util.parse("20231225T103045Z");
 * }</pre>
 *
 * @see <a href="http://www.w3.org/TR/NOTE-datetime">W3C NOTE-datetime specification</a>
 */
final class ISO8601Util {

    /**
     * The default length of an ISO8601 formatted string with milliseconds and timezone.
     */
    static final int DEF_8601_LEN = "yyyy-MM-ddThh:mm:ss.SSS+00:00".length();

    /**
     * The timezone used for 'Z' suffix in ISO8601 date/time values (UTC timezone).
     * Since version 2.7, this is {@link Dates#UTC_TIME_ZONE}.
     */
    static final TimeZone TIMEZONE_Z = Dates.UTC_TIME_ZONE;

    private ISO8601Util() {
        // singleton
    }

    /*
    /**********************************************************
    /* Formatting
    
    /**
     * Formats a date into ISO8601 format using default settings.
     * 
     * <p>The output format is {@code yyyy-MM-ddThh:mm:ssZ} (no millisecond precision, UTC timezone).</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * String formatted = ISO8601Util.format(date);   // "2023-12-25T10:30:45Z"
     * }</pre>
     *
     * @param date the date to format
     * @return the formatted date string in ISO8601 format
     */
    public static String format(final Date date) {
        return format(date, false, TIMEZONE_Z);
    }

    /**
     * Formats a date into ISO8601 format with optional millisecond precision.
     * 
     * <p>The output format is {@code yyyy-MM-ddThh:mm:ss[.sss]Z} (UTC timezone).</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * String withMillis = ISO8601Util.format(date, true);   // "2023-12-25T10:30:45.123Z"
     * String noMillis = ISO8601Util.format(date, false);    // "2023-12-25T10:30:45Z"
     * }</pre>
     *
     * @param date the date to format
     * @param millis {@code true} to include milliseconds, {@code false} otherwise
     * @return the formatted date string in ISO8601 format
     */
    public static String format(final Date date, final boolean millis) {
        return format(date, millis, TIMEZONE_Z);
    }

    /**
     * Formats a date into ISO8601 format with specified timezone.
     * 
     * @param date the date to format
     * @param millis {@code true} to include milliseconds
     * @param tz the timezone to use
     * @return the formatted date string
     * @deprecated Use {@link #format(Date, boolean, TimeZone, Locale)} instead
     */
    @Deprecated // since 2.9
    public static String format(final Date date, final boolean millis, final TimeZone tz) {
        return format(date, millis, tz, Locale.US);
    }

    /**
     * Formats a date into ISO8601 format with full control over formatting options.
     * 
     * <p>The output format is {@code yyyy-MM-ddThh:mm:ss[.sss][Z|[+-]hh:mm]}</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * TimeZone tz = TimeZone.getTimeZone("America/New_York");
     * String formatted = ISO8601Util.format(date, true, tz, Locale.US);
     * // "2023-12-25T10:30:45.123-05:00"
     * }</pre>
     *
     * @param date the date to format
     * @param millis {@code true} to include milliseconds, {@code false} otherwise
     * @param tz timezone to use for formatting (UTC produces 'Z', others produce offset)
     * @param loc locale to use for formatting
     * @return the formatted date string in ISO8601 format
     */
    public static String format(final Date date, final boolean millis, final TimeZone tz, final Locale loc) {
        final Calendar calendar = new GregorianCalendar(tz, loc);
        calendar.setTime(date);

        // estimate capacity of buffer as close as we can (yeah, that's pedantic ;)
        final StringBuilder sb = new StringBuilder(30);
        sb.append(String.format("%04d-%02d-%02dT%02d:%02d:%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND)));
        if (millis) {
            sb.append(String.format(".%03d", calendar.get(Calendar.MILLISECOND)));
        }

        final int offset = tz.getOffset(calendar.getTimeInMillis());
        if (offset != 0) {
            final int hours = Math.abs((offset / (60 * 1000)) / 60);
            final int minutes = Math.abs((offset / (60 * 1000)) % 60);
            sb.append(String.format("%c%02d:%02d", (offset < 0 ? '-' : '+'), hours, minutes));
        } else {
            sb.append('Z');
        }
        return sb.toString();
    }

    /*
    /**********************************************************
    /* Parsing
    
    /**
     * Parses a date from an ISO8601 formatted string.
     * 
     * <p>Supported formats include:</p>
     * <ul>
     * <li>{@code yyyy-MM-dd}</li>
     * <li>{@code yyyyMMdd}</li>
     * <li>{@code yyyy-MM-ddThh:mm:ss}</li>
     * <li>{@code yyyy-MM-ddThh:mm:ss.sss}</li>
     * <li>{@code yyyy-MM-ddThh:mm:ssZ}</li>
     * <li>{@code yyyy-MM-ddThh:mm:ss+hh:mm}</li>
     * <li>{@code yyyy-MM-ddThh:mm:ss-hh:mm}</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date1 = ISO8601Util.parse("2023-12-25");
     * Date date2 = ISO8601Util.parse("2023-12-25T10:30:45Z");
     * Date date3 = ISO8601Util.parse("2023-12-25T10:30:45.123+05:30");
     * Date date4 = ISO8601Util.parse("20231225T103045Z");
     * }</pre>
     *
     * @param date the ISO8601 string to parse
     * @return the parsed Date object
     * @throws IllegalArgumentException if the date string cannot be parsed
     */
    public static Date parse(final String date) {
        return parse(date, new ParsePosition(0));
    }

    /**
     * Parses a date from an ISO8601 formatted string with parse position tracking.
     * 
     * <p>This method allows partial parsing of a string by tracking the parse position.
     * The position will be updated to indicate where parsing stopped.</p>
     * 
     * <p>Supported formats include:</p>
     * <ul>
     * <li>{@code [yyyy-MM-dd|yyyyMMdd]}</li>
     * <li>{@code [yyyy-MM-dd|yyyyMMdd]T[hh:mm[:ss[.sss]]|hhmm[ss[.sss]]]}
     * <li>{@code [yyyy-MM-dd|yyyyMMdd]T[hh:mm[:ss[.sss]]|hhmm[ss[.sss]]][Z|[+-]hh:mm]}</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ParsePosition pos = new ParsePosition(0);
     * Date date = ISO8601Util.parse("2023-12-25T10:30:45Z extra text", pos);
     * int endIndex = pos.getIndex();   // Position after the parsed date
     * }</pre>
     *
     * @param date the ISO8601 string to parse
     * @param pos the ParsePosition to start parsing from and update with the end position
     * @return the parsed Date object
     * @throws IllegalArgumentException if the date string cannot be parsed or is malformed
     */
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    public static Date parse(final String date, final ParsePosition pos) {
        Exception fail = null;
        try {
            int offset = pos.getIndex();

            // extract year
            final int year = parseInt(date, offset, offset += 4);
            if (checkOffset(date, offset, '-')) {
                offset += 1;
            }

            // extract month
            final int month = parseInt(date, offset, offset += 2);
            if (checkOffset(date, offset, '-')) {
                offset += 1;
            }

            // extract day
            final int day = parseInt(date, offset, offset += 2);
            // default time value
            int hour = 0;
            int minutes = 0;
            int seconds = 0;
            int milliseconds = 0; // always use 0 otherwise returned date will include millis of current time

            // if the value has no time component (and no time zone), we are done
            final boolean hasT = checkOffset(date, offset, 'T');

            if (!hasT && (date.length() <= offset)) {
                //noinspection MagicConstant
                final Calendar calendar = new GregorianCalendar(TIMEZONE_Z);

                calendar.clear();
                calendar.set(year, month - 1, day);

                pos.setIndex(offset);
                return calendar.getTime();
            }

            if (hasT) {

                // extract hours, minutes, seconds and milliseconds
                hour = parseInt(date, offset += 1, offset += 2);
                if (checkOffset(date, offset, ':')) {
                    offset += 1;
                }

                minutes = parseInt(date, offset, offset += 2);
                if (checkOffset(date, offset, ':')) {
                    offset += 1;
                }
                // second and milliseconds can be optional
                if (date.length() > offset) {
                    final char c = date.charAt(offset);
                    if (c != 'Z' && c != '+' && c != '-') {
                        seconds = parseInt(date, offset, offset += 2);
                        if (seconds > 59 && seconds < 63) {
                            seconds = 59; // truncate up to 3 leap seconds
                        }
                        // milliseconds can be optional in the format
                        if (checkOffset(date, offset, '.')) {
                            offset += 1;
                            final int endOffset = indexOfNonDigit(date, offset + 1); // assume at least one digit
                            final int parseEndOffset = Math.min(endOffset, offset + 3); // parse up to 3 digits
                            final int fraction = parseInt(date, offset, parseEndOffset);
                            // compensate for "missing" digits
                            switch (parseEndOffset - offset) { // number of digits parsed
                                case 2:
                                    milliseconds = fraction * 10;
                                    break;
                                case 1:
                                    milliseconds = fraction * 100;
                                    break;
                                default:
                                    milliseconds = fraction;
                            }
                            offset = endOffset;
                        }
                    }
                }
            }

            // extract timezone
            if (date.length() <= offset) {
                throw new IllegalArgumentException("No time zone indicator");
            }

            TimeZone timezone = null;
            final char timezoneIndicator = date.charAt(offset);

            if (timezoneIndicator == 'Z') {
                timezone = TIMEZONE_Z;
                offset += 1;
            } else if (timezoneIndicator == '+' || timezoneIndicator == '-') {
                final String timezoneOffset = date.substring(offset);
                offset += timezoneOffset.length();
                // 18-Jun-2015, tatu: Minor simplification, skip offset of "+0000"/"+00:00"
                if ("+0000".equals(timezoneOffset) || "+00:00".equals(timezoneOffset)) {
                    timezone = TIMEZONE_Z;
                } else {
                    // 18-Jun-2015, tatu: Looks like offsets only work from GMT, not UTC...
                    //    not sure why, but that's the way it looks. Further, Javadocs for
                    //    `java.util.TimeZone` specifically instruct use of GMT as base for
                    //    custom timezones... odd.
                    final String timezoneId = "GMT" + timezoneOffset;
                    //                    String timezoneId = "UTC" + timezoneOffset;

                    timezone = TimeZone.getTimeZone(timezoneId);

                    final String act = timezone.getID();
                    if (!act.equals(timezoneId)) {
                        /* 22-Jan-2015, tatu: Looks like canonical version has colons, but we may be given
                         *    one without. If so, don't sweat.
                         *   Yes, very inefficient. Hopefully not hit often.
                         *   If it becomes a perf problem, add <i>loose</i> comparison instead.
                         */
                        final String cleaned = act.replace(":", "");
                        if (!cleaned.equals(timezoneId)) {
                            throw new IndexOutOfBoundsException("Mismatching time zone indicator: " + timezoneId + " given, resolves to " + timezone.getID());
                        }
                    }
                }
            } else {
                throw new IndexOutOfBoundsException("Invalid time zone indicator '" + timezoneIndicator + "'");
            }

            final Calendar calendar = new GregorianCalendar(timezone);

            calendar.clear();

            calendar.setLenient(false);
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month - 1);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minutes);
            calendar.set(Calendar.SECOND, seconds);
            calendar.set(Calendar.MILLISECOND, milliseconds);

            pos.setIndex(offset);
            return calendar.getTime();
            // If we get a ParseException it'll already have the right message/offset.
            // Other exception types can convert here.
        } catch (final Exception e) {
            fail = e;
        }
        final String input = (date == null) ? null : ('"' + date + '"');
        String msg = fail.getMessage();
        if (msg == null || msg.isEmpty()) {
            msg = "(" + fail.getClass().getName() + ")";
        }
        final IllegalArgumentException ex = new IllegalArgumentException("Failed to parse date " + input + ": " + msg + " at position: " + pos.getIndex());
        //noinspection UnnecessaryInitCause
        ex.initCause(fail);
        throw ex;
    }

    /**
     * Checks if the expected character exists at the given offset in the string.
     *
     * @param value the string to check
     * @param offset the position to check
     * @param expected the expected character
     * @return {@code true} if the character at offset matches expected
     */
    private static boolean checkOffset(final String value, final int offset, final char expected) {
        return (offset < value.length()) && (value.charAt(offset) == expected);
    }

    private static int parseInt(final String value, final int beginIndex, final int endIndex) throws NumberFormatException {
        if (beginIndex < 0 || endIndex > value.length() || beginIndex > endIndex) {
            throw new NumberFormatException(value);
        }
        // use the same logic as in Integer.parseInt() but less generic we're not supporting negative values
        int i = beginIndex;
        int result = 0;
        int digit;
        if (i < endIndex) {
            digit = Character.digit(value.charAt(i++), 10);
            if (digit < 0) {
                throw new NumberFormatException("Invalid number: " + value.substring(beginIndex, endIndex));
            }
            result = -digit;
        }
        while (i < endIndex) {
            digit = Character.digit(value.charAt(i++), 10);
            if (digit < 0) {
                throw new NumberFormatException("Invalid number: " + value.substring(beginIndex, endIndex));
            }
            result *= 10;
            result -= digit;
        }
        return -result;
    }

    /**
     * Finds the index of the first non-digit character in the string starting from the given offset.
     *
     * @param string the string to search
     * @param offset the starting position
     * @return the index of the first non-digit character, or the string length if all remaining characters are digits
     */
    private static int indexOfNonDigit(final String string, final int offset) {
        for (int i = offset; i < string.length(); i++) {
            final char c = string.charAt(i);
            if (c < '0' || c > '9') {
                return i;
            }
        }
        return string.length();
    }
}
