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
 *
 * Copyright (c) 2017, Jackson Authors/Contributors.
 *
 * Utilities methods for manipulating dates in iso8601 format. This is much, much faster and GC friendly than using SimpleDateFormat so
 * highly suitable if you (un)serialize lots of date objects.
 *
 * Supported parse format: [yyyy-MM-dd|yyyyMMdd][T(hh:mm[:ss[.sss]]|hhmm[ss[.sss]])]?[Z|[+-]hh[:]mm]]
 *
 * @see <a href="http://www.w3.org/TR/NOTE-datetime">this specification</a>
 */
final class ISO8601Util {

    static final int DEF_8601_LEN = "yyyy-MM-ddThh:mm:ss.SSS+00:00".length();

    /**
     * Timezone we use for 'Z' in ISO-8601 date/time values: since 2.7
     * {@link Dates#UTC_TIME_ZONE}; with earlier versions up to 2.7 was {@link Dates#GMT_TIME_ZONE}.
     */
    static final TimeZone TIMEZONE_Z = Dates.UTC_TIME_ZONE;

    private ISO8601Util() {
        // singleton
    }

    /*
    /**********************************************************
    /* Formatting
    
    /**
     * Format a date into <i>yyyy-MM-ddThh:mm:ssZ</i> (default timezone, no millisecond precision).
     *
     * @param date the date to format
     * @return
     */
    public static String format(final Date date) {
        return format(date, false, TIMEZONE_Z);
    }

    /**
     * Format a date into <i>yyyy-MM-ddThh:mm:ss[.sss]Z</i> (GMT timezone)
     *
     * @param date the date to format
     * @param millis {@code true} to include millis precision otherwise false
     * @return
     */
    public static String format(final Date date, final boolean millis) {
        return format(date, millis, TIMEZONE_Z);
    }

    /**
     *
     * @param date
     * @param millis
     * @param tz
     * @return
     * @deprecated
     */
    @Deprecated // since 2.9
    public static String format(final Date date, final boolean millis, final TimeZone tz) {
        return format(date, millis, tz, Locale.US);
    }

    /**
     * Format date into yyyy-MM-ddThh:mm:ss[.sss][Z|[+-]hh:mm]
     *
     * @param date the date to format
     * @param millis {@code true} to include millis precision otherwise false
     * @param tz timezone to use for the formatting (UTC will produce 'Z')
     * @param loc
     * @return
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
     *
     * @param date
     * @return
     */
    public static Date parse(final String date) {
        return parse(date, new ParsePosition(0));
    }

    /**
     * Parse a date from ISO-8601 formatted string. It expects a format
     * [yyyy-MM-dd|yyyyMMdd][T(hh:mm[:ss[.sss]]|hhmm[ss[.sss]])]?[Z|[+-]hh:mm]]
     *
     * @param date ISO string to parse in the appropriate format.
     * @param pos The position to start parsing from, updated to where parsing stopped.
     * @return
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
                final Calendar calendar = new GregorianCalendar(year, month - 1, day);

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
     * Check if the expected character exists at the given offset in the value.
     *
     * @param value the string to check at the specified offset
     * @param offset the offset to look for the expected character
     * @param expected the expected character
     * @return {@code true} if the expected character exists at the given offset
     */
    private static boolean checkOffset(final String value, final int offset, final char expected) {
        return (offset < value.length()) && (value.charAt(offset) == expected);
    }

    /**
     * Parse an integer located between 2 given offsets in a string.
     *
     * @param value the string to parse
     * @param beginIndex the start index for the integer in the string
     * @param endIndex the end index for the integer in the string
     * @return
     * @throws NumberFormatException if the value is not a number
     */
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
     * Returns the index of the first character in the string that is not a digit, starting at offset.
     *
     * @param string
     * @param offset
     * @return
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
