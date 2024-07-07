/*
 * Copyright (C) 2018 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.UncheckedIOException;

/**
 * <p>
 * Note: This class includes codes copied from Apache Commons Lang, Google Guava and other open source projects under the Apache License 2.0.
 * The methods copied from other libraries/frameworks/projects may be modified in this class.
 * </p>
 * @see DateTimeFormatter
 * @author Haiyang Li
 * @since 1.2.6
 */
@SuppressWarnings({ "java:S1694", "java:S2143" })
public abstract sealed class DateUtil permits DateUtil.DateTimeUtil, DateUtil.Dates {

    private static final String DATE_STR = "date";
    private static final String DATE1_STR = "date1";
    private static final String DATE2_STR = "date2";

    private static final String CALENDAR_STR = "calendar";
    private static final String CAL1_STR = "cal1";
    private static final String CAL2_STR = "cal2";

    // ...
    public static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    static final TimeZone DEFAULT_TIME_ZONE = Calendar.getInstance().getTimeZone();

    /**
     * Date format.
     */
    public static final String LOCAL_YEAR_FORMAT = "yyyy";

    public static final String LOCAL_MONTH_DAY_FORMAT = "MM-dd";

    static final String LOCAL_MONTH_DAY_FORMAT_SLASH = "MM/dd";

    public static final String LOCAL_DATE_FORMAT = "yyyy-MM-dd";

    static final String LOCAL_DATE_FORMAT_SLASH = "yyyy/MM/dd";

    public static final String LOCAL_TIME_FORMAT = "HH:mm:ss";

    public static final String LOCAL_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    static final String LOCAL_DATETIME_FORMAT_SLASH = "yyyy/MM/dd HH:mm:ss";

    public static final String LOCAL_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    static final String LOCAL_TIMESTAMP_FORMAT_SLASH = "yyyy/MM/dd HH:mm:ss.SSS";

    /**
     * It's default date/time format.
     */
    public static final String ISO_8601_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    static final String ISO_8601_DATETIME_FORMAT_SLASH = "yyyy/MM/dd'T'HH:mm:ss'Z'";
    /**
     * It's default timestamp format.
     */
    public static final String ISO_8601_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    static final String ISO_8601_TIMESTAMP_FORMAT_SLASH = "yyyy/MM/dd'T'HH:mm:ss.SSS'Z'";

    /**
     * This constant defines the date format specified by
     * RFC 1123 / RFC 822. Used for parsing via `SimpleDateFormat` as well as
     * error messages.
     */
    public static final String RFC1123_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

    /**
     * This is half a month, so this represents whether a date is in the top
     * or bottom half of the month.
     */
    public static final int SEMI_MONTH = 1001;

    private static final int[][] fields = { { Calendar.MILLISECOND }, { Calendar.SECOND }, { Calendar.MINUTE }, { Calendar.HOUR_OF_DAY, Calendar.HOUR },
            { Calendar.DATE, Calendar.DAY_OF_MONTH, Calendar.AM_PM
            /* Calendar.DAY_OF_YEAR, Calendar.DAY_OF_WEEK, Calendar.DAY_OF_WEEK_IN_MONTH */
            }, { Calendar.MONTH, SEMI_MONTH }, { Calendar.YEAR }, { Calendar.ERA } };

    @SuppressWarnings("deprecation")
    private static final int POOL_SIZE = InternalUtil.POOL_SIZE;

    private static final Map<String, Queue<DateFormat>> dfPool = new ObjectPool<>(64);

    private static final Map<TimeZone, Queue<Calendar>> calendarPool = new ObjectPool<>(64);

    private static final Queue<DateFormat> utcTimestampDFPool = new ArrayBlockingQueue<>(POOL_SIZE);

    private static final Queue<DateFormat> utcDateTimeDFPool = new ArrayBlockingQueue<>(POOL_SIZE);

    private static final Queue<Calendar> utcCalendarPool = new ArrayBlockingQueue<>(POOL_SIZE);

    // ...
    private static final Queue<char[]> utcTimestampFormatCharsPool = new ArrayBlockingQueue<>(POOL_SIZE);

    private static final DatatypeFactory dataTypeFactory;

    static {
        DatatypeFactory temp = null;

        try {
            temp = DatatypeFactory.newInstance();
        } catch (Exception e) {
            // ignore.
            // logger.error("Failed to initialize XMLGregorianCalendarType: " +
            // e.getMessage(), e);
        }

        dataTypeFactory = temp;
    }

    // ...
    private static final char[][][] cbufOfSTDInt = new char[5][][];

    static {
        for (int i = 0, j = 1; i < 5; i++, j = j * 10) {
            cbufOfSTDInt[i] = new char[j][];

            for (int k = 0; k < j; k++) {
                if (i == 1) {
                    cbufOfSTDInt[i][k] = String.valueOf(k).toCharArray();
                } else if (i == 2) {
                    if (k < 10) {
                        cbufOfSTDInt[i][k] = ("0" + k).toCharArray();
                    } else {
                        cbufOfSTDInt[i][k] = String.valueOf(k).toCharArray();
                    }
                } else if (i == 3) {
                    if (k < 10) {
                        cbufOfSTDInt[i][k] = ("00" + k).toCharArray();
                    } else if (k < 100) {
                        cbufOfSTDInt[i][k] = ("0" + k).toCharArray();
                    } else {
                        cbufOfSTDInt[i][k] = String.valueOf(k).toCharArray();
                    }
                } else if (i == 4) {
                    if (k < 10) {
                        cbufOfSTDInt[i][k] = ("000" + k).toCharArray();
                    } else if (k < 100) {
                        cbufOfSTDInt[i][k] = ("00" + k).toCharArray();
                    } else if (k < 1000) {
                        cbufOfSTDInt[i][k] = ("0" + k).toCharArray();
                    } else {
                        cbufOfSTDInt[i][k] = String.valueOf(k).toCharArray();
                    }
                }
            }
        }
    }

    static final Map<String, DateTimeFormatter> dtfPool = ImmutableMap.<String, DateTimeFormatter> builder()
            .put("uuuu-MM-dd", DateTimeFormatter.ISO_LOCAL_DATE)
            .build();

    DateUtil() {
        // singleton
    }

    /**
     *
     * @return
     * @see System#currentTimeMillis()
     */
    @Beta
    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * A new instance of <code>java.sql.Time</code> returned is based on the
     * current time in the default time zone with the default locale.
     *
     * @return
     */
    public static Time currentTime() {
        return new Time(System.currentTimeMillis());
    }

    /**
     * A new instance of <code>java.sql.Date</code> returned is based on the
     * current time in the default time zone with the default locale.
     *
     * @return
     */
    public static Date currentDate() {
        return new Date(System.currentTimeMillis());
    }

    /**
     * A new instance of <code>java.sql.Timestamp</code> returned is based on
     * the current time in the default time zone with the default locale.
     *
     * @return
     */
    public static Timestamp currentTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    /**
     * A new instance of <code>java.util.Date</code> returned is based on the
     * current time in the default time zone with the default locale.
     *
     * @return
     */
    public static java.util.Date currentJUDate() {
        return new java.util.Date();
    }

    /**
     * A new instance of <code>java.util.Calendar</code> returned is based on
     * the current time in the default time zone with the default locale.
     *
     * @return a Calendar.
     */
    public static Calendar currentCalendar() {
        return Calendar.getInstance();
    }

    /**
     * Current gregorian calendar.
     *
     * @return
     */
    public static GregorianCalendar currentGregorianCalendar() {
        return new GregorianCalendar();
    }

    /**
     * Current XML gregorian calendar.
     *
     * @return
     */
    public static XMLGregorianCalendar currentXMLGregorianCalendar() {
        return dataTypeFactory.newXMLGregorianCalendar(currentGregorianCalendar());
    }

    /**
     * Adds or subtracts the specified amount of time with the given time unit to current {@code java.sql.Time}
     *
     * @param amount
     * @param unit
     * @return
     * @return a new {@code Time} by Adding or subtracting the specified amount of time to current {@code java.sql.Time}.
     */
    @Beta
    static long currentTimeMillisRolled(final long amount, final TimeUnit unit) {
        return System.currentTimeMillis() + unit.toMillis(amount);
    }

    /**
     * Adds or subtracts the specified amount of time with the given time unit to current {@code java.sql.Time}
     *
     * @param amount
     * @param unit
     * @return a new {@code Time} by Adding or subtracting the specified amount of time to current {@code java.sql.Time}.
     */
    @Beta
    public static Time currentTimeRolled(final long amount, final TimeUnit unit) {
        return new Time(currentTimeMillisRolled(amount, unit));
    }

    /**
     * Adds or subtracts the specified amount of time with the given time unit to current {@code java.sql.Date}
     *
     * @param amount
     * @param unit
     * @return a new {@code Date} by Adding or subtracting the specified amount of time to current {@code java.sql.Date}.
     */
    @Beta
    public static Date currentDateRolled(final long amount, final TimeUnit unit) {
        return new Date(currentTimeMillisRolled(amount, unit));
    }

    /**
     * Adds or subtracts the specified amount of time with the given time unit to current {@code java.sql.Timestamp}
     *
     * @param amount
     * @param unit
     * @return a new {@code Timestamp} by Adding or subtracting the specified amount of time to current {@code java.sql.Timestamp}.
     */
    @Beta
    public static Timestamp currentTimestampRolled(final long amount, final TimeUnit unit) {
        return new Timestamp(currentTimeMillisRolled(amount, unit));
    }

    /**
     * Adds or subtracts the specified amount of time with the given time unit to current {@code java.util.Date}
     *
     *
     * @param amount
     * @param unit
     * @return a new {@code Date} by Adding or subtracting the specified amount of time to current {@code java.util.Date}.
     */
    @Beta
    public static java.util.Date currentJUDateRolled(final long amount, final TimeUnit unit) {
        return new java.util.Date(currentTimeMillisRolled(amount, unit));
    }

    /**
     * Adds or subtracts the specified amount of time with the given time unit to current {@code java.util.Calendar}.
     *
     *
     * @param amount
     * @param unit
     * @return a new {@code Calendar} by Adding or subtracting the specified amount of time to current {@code java.util.Calendar}.
     */
    @Beta
    public static Calendar currentCalendarRolled(final long amount, final TimeUnit unit) {
        final Calendar ret = Calendar.getInstance();
        ret.setTimeInMillis(currentTimeMillisRolled(amount, unit));
        return ret;
    }

    /**
     * Creates the JU date.
     *
     * @param calendar
     * @return
     */
    public static java.util.Date createJUDate(final Calendar calendar) {
        return (calendar == null) ? null : createJUDate(calendar.getTimeInMillis());
    }

    /**
     * Creates the JU date.
     *
     * @param date
     * @return
     */
    public static java.util.Date createJUDate(final java.util.Date date) {
        return (date == null) ? null : createJUDate(date.getTime());
    }

    /**
     * Creates the JU date.
     *
     * @param timeInMillis
     * @return
     */
    public static java.util.Date createJUDate(final long timeInMillis) {
        //    N.checkArgPositive(timeInMillis, "timeInMillis");
        //
        //    if (timeInMillis == 0) {
        //        return null;
        //    }

        return new java.util.Date(timeInMillis);
    }

    /**
     * Creates the date.
     *
     * @param calendar
     * @return
     */
    public static Date createDate(final Calendar calendar) {
        return (calendar == null) ? null : createDate(calendar.getTimeInMillis());
    }

    /**
     * Creates the date.
     *
     * @param date
     * @return
     */
    public static Date createDate(final java.util.Date date) {
        return (date == null) ? null : createDate(date.getTime());
    }

    /**
     * Creates the date.
     *
     * @param timeInMillis
     * @return
     */
    public static Date createDate(final long timeInMillis) {
        //    N.checkArgPositive(timeInMillis, "timeInMillis");
        //
        //    if (timeInMillis == 0) {
        //        return null;
        //    }

        return new Date(timeInMillis);
    }

    /**
     * Creates the time.
     *
     * @param calendar
     * @return
     */
    public static Time createTime(final Calendar calendar) {
        return (calendar == null) ? null : createTime(calendar.getTimeInMillis());
    }

    /**
     * Creates the time.
     *
     * @param date
     * @return
     */
    public static Time createTime(final java.util.Date date) {
        return (date == null) ? null : createTime(date.getTime());
    }

    /**
     * Creates the time.
     *
     * @param timeInMillis
     * @return
     */
    public static Time createTime(final long timeInMillis) {
        //    N.checkArgPositive(timeInMillis, "timeInMillis");
        //
        //    if (timeInMillis == 0) {
        //        return null;
        //    }

        return new Time(timeInMillis);
    }

    /**
     * Creates the timestamp.
     *
     * @param calendar
     * @return
     */
    public static Timestamp createTimestamp(final Calendar calendar) {
        return (calendar == null) ? null : createTimestamp(calendar.getTimeInMillis());
    }

    /**
     * Creates the timestamp.
     *
     * @param date
     * @return
     */
    public static Timestamp createTimestamp(final java.util.Date date) {
        return (date == null) ? null : createTimestamp(date.getTime());
    }

    /**
     * Creates the timestamp.
     *
     * @param timeInMillis
     * @return
     */
    public static Timestamp createTimestamp(final long timeInMillis) {
        //    N.checkArgPositive(timeInMillis, "timeInMillis");
        //
        //    if (timeInMillis == 0) {
        //        return null;
        //    }

        return new Timestamp(timeInMillis);
    }

    /**
     * Creates the calendar.
     *
     * @param calendar
     * @return
     */
    public static Calendar createCalendar(final Calendar calendar) {
        return (calendar == null) ? null : createCalendar(calendar.getTimeInMillis());
    }

    /**
     * Creates the calendar.
     *
     * @param date
     * @return
     */
    public static Calendar createCalendar(final java.util.Date date) {
        return (date == null) ? null : createCalendar(date.getTime());
    }

    /**
     * Creates the calendar.
     *
     * @param timeInMillis
     * @return
     */
    public static Calendar createCalendar(final long timeInMillis) {
        //    N.checkArgPositive(timeInMillis, "timeInMillis");
        //
        //    if (timeInMillis == 0) {
        //        return null;
        //    }

        final Calendar c = Calendar.getInstance();

        c.setTimeInMillis(timeInMillis);

        return c;
    }

    /**
     * Creates the calendar.
     *
     * @param timeInMillis
     * @param tz
     * @return
     */
    public static Calendar createCalendar(final long timeInMillis, final TimeZone tz) {
        //    N.checkArgPositive(timeInMillis, "timeInMillis");
        //
        //    if (timeInMillis == 0) {
        //        return null;
        //    }

        final Calendar c = Calendar.getInstance(tz);

        c.setTimeInMillis(timeInMillis);

        return c;
    }

    /**
     * Creates the gregorian calendar.
     *
     * @param calendar
     * @return
     */
    public static GregorianCalendar createGregorianCalendar(final Calendar calendar) {
        return (calendar == null) ? null : createGregorianCalendar(calendar.getTimeInMillis());
    }

    /**
     * Creates the gregorian calendar.
     *
     * @param date
     * @return
     */
    public static GregorianCalendar createGregorianCalendar(final java.util.Date date) {
        return (date == null) ? null : createGregorianCalendar(date.getTime());
    }

    /**
     * Creates the gregorian calendar.
     *
     * @param timeInMillis
     * @return
     */
    public static GregorianCalendar createGregorianCalendar(final long timeInMillis) {
        //    N.checkArgPositive(timeInMillis, "timeInMillis");
        //
        //    if (timeInMillis == 0) {
        //        return null;
        //    }

        final GregorianCalendar c = new GregorianCalendar();

        c.setTimeInMillis(timeInMillis);

        return c;
    }

    /**
     * Creates the gregorian calendar.
     *
     * @param timeInMillis
     * @param tz
     * @return
     */
    public static GregorianCalendar createGregorianCalendar(final long timeInMillis, final TimeZone tz) {
        //    N.checkArgPositive(timeInMillis, "timeInMillis");
        //
        //    if (timeInMillis == 0) {
        //        return null;
        //    }

        final GregorianCalendar c = new GregorianCalendar(tz);

        c.setTimeInMillis(timeInMillis);

        return c;
    }

    /**
     * Creates the XML gregorian calendar.
     *
     * @param calendar
     * @return
     */
    public static XMLGregorianCalendar createXMLGregorianCalendar(final Calendar calendar) {
        return (calendar == null) ? null : createXMLGregorianCalendar(calendar.getTimeInMillis());
    }

    /**
     * Creates the XML gregorian calendar.
     *
     * @param date
     * @return
     */
    public static XMLGregorianCalendar createXMLGregorianCalendar(final java.util.Date date) {
        return (date == null) ? null : createXMLGregorianCalendar(date.getTime());
    }

    /**
     * Creates the XML gregorian calendar.
     *
     * @param timeInMillis
     * @return
     */
    public static XMLGregorianCalendar createXMLGregorianCalendar(final long timeInMillis) {
        //    N.checkArgPositive(timeInMillis, "timeInMillis");
        //
        //    if (timeInMillis == 0) {
        //        return null;
        //    }

        return dataTypeFactory.newXMLGregorianCalendar(createGregorianCalendar(timeInMillis));
    }

    /**
     * Parses the JU date.
     *
     * @param date
     * @return
     */
    public static java.util.Date parseJUDate(final String date) {
        return parseJUDate(date, null);
    }

    /**
     * Parses the JU date.
     *
     * @param date
     * @param format
     * @return
     */
    public static java.util.Date parseJUDate(final String date, final String format) {
        return parseJUDate(date, format, null);
    }

    /**
     * Converts the specified <code>date</code> with the specified {@code format} to a new instance of java.util.Date.
     * <code>null</code> is returned if the specified <code>date</code> is null or empty.
     *
     * @param date
     * @param format
     * @param timeZone
     * @return {@code null} if {@code (Strings.isEmpty(date) || (date.length() == 4 && "null".equalsIgnoreCase(date)))}. (auto-generated java doc for return)
     * @throws IllegalArgumentException             if the date given can't be parsed with specified format.
     */
    @MayReturnNull
    public static java.util.Date parseJUDate(final String date, final String format, final TimeZone timeZone) {
        if (Strings.isEmpty(date) || (date.length() == 4 && "null".equalsIgnoreCase(date))) {
            return null;
        }

        return createJUDate(parse(date, format, timeZone));
    }

    /**
     * Parses the date.
     *
     * @param date
     * @return
     */
    public static Date parseDate(final String date) {
        return parseDate(date, null);
    }

    /**
     * Parses the date.
     *
     * @param date
     * @param format
     * @return
     */
    public static Date parseDate(final String date, final String format) {
        return parseDate(date, format, null);
    }

    /**
     * Converts the specified <code>date</code> with the specified {@code format} to a new instance of java.sql.Date.
     * <code>null</code> is returned if the specified <code>date</code> is null or empty.
     *
     * @param date
     * @param format
     * @param timeZone
     * @return {@code null} if {@code (Strings.isEmpty(date) || (date.length() == 4 && "null".equalsIgnoreCase(date)))}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static Date parseDate(final String date, final String format, final TimeZone timeZone) {
        if (Strings.isEmpty(date) || (date.length() == 4 && "null".equalsIgnoreCase(date))) {
            return null;
        }

        return createDate(parse(date, format, timeZone));
    }

    /**
     * Parses the time.
     *
     * @param date
     * @return
     */
    public static Time parseTime(final String date) {
        return parseTime(date, null);
    }

    /**
     * Parses the time.
     *
     * @param date
     * @param format
     * @return
     */
    public static Time parseTime(final String date, final String format) {
        return parseTime(date, format, null);
    }

    /**
     * Converts the specified <code>date</code> with the specified {@code format} to a new instance of Time.
     * <code>null</code> is returned if the specified <code>date</code> is null or empty.
     *
     * @param date
     * @param format
     * @param timeZone
     * @return {@code null} if {@code (Strings.isEmpty(date) || (date.length() == 4 && "null".equalsIgnoreCase(date)))}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static Time parseTime(final String date, final String format, final TimeZone timeZone) {
        if (Strings.isEmpty(date) || (date.length() == 4 && "null".equalsIgnoreCase(date))) {
            return null;
        }

        return createTime(parse(date, format, timeZone));
    }

    /**
     * Parses the timestamp.
     *
     * @param date
     * @return
     */
    public static Timestamp parseTimestamp(final String date) {
        return parseTimestamp(date, null);
    }

    /**
     * Parses the timestamp.
     *
     * @param date
     * @param format
     * @return
     */
    public static Timestamp parseTimestamp(final String date, final String format) {
        return parseTimestamp(date, format, null);
    }

    /**
     * Converts the specified <code>date</code> with the specified {@code format} to a new instance of Timestamp.
     * <code>null</code> is returned if the specified <code>date</code> is null or empty.
     *
     * @param date
     * @param format
     * @param timeZone
     * @return {@code null} if {@code (Strings.isEmpty(date) || (date.length() == 4 && "null".equalsIgnoreCase(date)))}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static Timestamp parseTimestamp(final String date, final String format, final TimeZone timeZone) {
        if (Strings.isEmpty(date) || (date.length() == 4 && "null".equalsIgnoreCase(date))) {
            return null;
        }

        return createTimestamp(parse(date, format, timeZone));
    }

    /**
     * Parses the calendar.
     *
     * @param calendar
     * @return
     */
    @Beta
    public static Calendar parseCalendar(final String calendar) {
        return parseCalendar(calendar, null);
    }

    /**
     * Parses the calendar.
     *
     * @param calendar
     * @param format
     * @return
     */
    @Beta
    public static Calendar parseCalendar(final String calendar, final String format) {
        return parseCalendar(calendar, format, null);
    }

    /**
     * Converts the specified <code>calendar</code> with the specified {@code format} to a new instance of Calendar.
     * <code>null</code> is returned if the specified <code>date</code> is null or empty.
     *
     * @param calendar
     * @param format
     * @param timeZone
     * @return {@code null} if {@code (Strings.isEmpty(calendar) || (calendar.length() == 4 && "null".equalsIgnoreCase(calendar)))}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Beta
    public static Calendar parseCalendar(final String calendar, final String format, final TimeZone timeZone) {
        if (Strings.isEmpty(calendar) || (calendar.length() == 4 && "null".equalsIgnoreCase(calendar))) {
            return null;
        }

        return createCalendar(parse(calendar, format, timeZone));
    }

    /**
     * Parses the gregorian calendar.
     *
     * @param calendar
     * @return
     */
    @Beta
    public static GregorianCalendar parseGregorianCalendar(final String calendar) {
        return parseGregorianCalendar(calendar, null);
    }

    /**
     * Parses the gregorian calendar.
     *
     * @param calendar
     * @param format
     * @return
     */
    @Beta
    public static GregorianCalendar parseGregorianCalendar(final String calendar, final String format) {
        return parseGregorianCalendar(calendar, format, null);
    }

    /**
     * Converts the specified <code>calendar</code> with the specified {@code format} to a new instance of GregorianCalendar.
     * <code>null</code> is returned if the specified <code>date</code> is null or empty.
     *
     * @param calendar
     * @param format
     * @param timeZone
     * @return {@code null} if {@code (Strings.isEmpty(calendar) || (calendar.length() == 4 && "null".equalsIgnoreCase(calendar)))}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Beta
    public static GregorianCalendar parseGregorianCalendar(final String calendar, final String format, final TimeZone timeZone) {
        if (Strings.isEmpty(calendar) || (calendar.length() == 4 && "null".equalsIgnoreCase(calendar))) {
            return null;
        }

        return createGregorianCalendar(parse(calendar, format, timeZone));
    }

    /**
     * Parses the XML gregorian calendar.
     *
     * @param calendar
     * @return
     */
    @Beta
    public static XMLGregorianCalendar parseXMLGregorianCalendar(final String calendar) {
        return parseXMLGregorianCalendar(calendar, null);
    }

    /**
     * Parses the XML gregorian calendar.
     *
     * @param calendar
     * @param format
     * @return
     */
    @Beta
    public static XMLGregorianCalendar parseXMLGregorianCalendar(final String calendar, final String format) {
        return parseXMLGregorianCalendar(calendar, format, null);
    }

    /**
     * Converts the specified <code>calendar</code> with the specified {@code format} to a new instance of XMLGregorianCalendar.
     * <code>null</code> is returned if the specified <code>date</code> is null or empty.
     *
     * @param calendar
     * @param format
     * @param timeZone
     * @return {@code null} if {@code (Strings.isEmpty(calendar) || (calendar.length() == 4 && "null".equalsIgnoreCase(calendar)))}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Beta
    public static XMLGregorianCalendar parseXMLGregorianCalendar(final String calendar, final String format, final TimeZone timeZone) {
        if (Strings.isEmpty(calendar) || (calendar.length() == 4 && "null".equalsIgnoreCase(calendar))) {
            return null;
        }

        return createXMLGregorianCalendar(parse(calendar, format, timeZone));
    }

    /**
     *
     * @param date
     * @param format
     * @param timeZone
     * @return
     */
    private static long parse(final String date, String format, TimeZone timeZone) {
        if ((format == null) && date.length() > 4 && (date.charAt(2) >= '0' && date.charAt(2) <= '9' && date.charAt(4) >= '0' && date.charAt(4) <= '9')) {
            try {
                return Long.parseLong(date);
            } catch (NumberFormatException e) {
                // ignore.
            }
        }

        format = checkDateFormat(date, format);

        if (Strings.isEmpty(format)) {
            if (timeZone == null || timeZone.equals(ISO8601Util.TIMEZONE_Z)) {
                return ISO8601Util.parse(date).getTime();
            } else {
                throw new RuntimeException("Unsupported date format: " + format + " with time zone: " + timeZone);
            }
        }

        timeZone = checkTimeZone(format, timeZone);

        long timeInMillis = fastDateParse(date, format, timeZone);

        if (timeInMillis != 0) {
            return timeInMillis;
        }

        DateFormat sdf = getSDF(format, timeZone);

        try {
            return sdf.parse(date).getTime();
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        } finally {
            recycleSDF(format, timeZone, sdf);
        }
    }

    //    /**
    //     * Parses the ZonedDateTime.
    //     *
    //     * @param dateTime
    //     * @return
    //     */
    //    public static ZonedDateTime parseZonedDateTime(final String dateTime) {
    //        return parseZonedDateTime(dateTime, null);
    //    }
    //
    //    /**
    //     * Parses the ZonedDateTime.
    //     *
    //     * @param dateTime
    //     * @param format
    //     * @return
    //     */
    //    public static ZonedDateTime parseZonedDateTime(final String dateTime, final String format) {
    //        return parseZonedDateTime(dateTime, format, null);
    //    }
    //
    //    /**
    //     * Converts the specified <code>date</code> with the specified {@code format} to a new instance of ZonedDateTime.
    //     * <code>null</code> is returned if the specified <code>date</code> is null or empty.
    //     *
    //     * @param dateTime
    //     * @param format
    //     * @param timeZone
    //     * @return
    //     */
    //    public static ZonedDateTime parseZonedDateTime(final String dateTime, String format, TimeZone timeZone) {
    //        if (N.isEmpty(dateTime) || (dateTime.length() == 4 && "null".equalsIgnoreCase(dateTime))) {
    //            return null;
    //        }
    //
    //        if ((format == null) && dateTime.length() > 4 && (dateTime.charAt(2) >= '0' && dateTime.charAt(2) <= '9' && dateTime.charAt(4) >= '0' && dateTime.charAt(4) <= '9')
    //                && Strings.isAsciiDigtalInteger(dateTime)) {
    //
    //            if (timeZone == null) {
    //                timeZone = LOCAL_TIME_ZONE;
    //            }
    //
    //            return Instant.ofEpochMilli(Numbers.toLong(dateTime)).atZone(timeZone.toZoneId());
    //        }
    //
    //        format = checkDateFormat(dateTime, format);
    //
    //        if (N.isEmpty(format)) {
    //            if (dateTime.charAt(dateTime.length() - 1) == ']' && dateTime.lastIndexOf('[') > 0) {
    //                ZonedDateTime ret = ZonedDateTime.parse(dateTime);
    //
    //                if (!(timeZone == null || timeZone.toZoneId().equals(ret.getZone()))) {
    //                    ret = ret.withZoneSameInstant(timeZone.toZoneId());
    //                }
    //
    //                return ret;
    //            } else {
    //                if (timeZone == null) {
    //                    timeZone = LOCAL_TIME_ZONE;
    //                }
    //
    //                return ISO8601Util.parse(dateTime).toInstant().atZone(timeZone.toZoneId());
    //            }
    //        }
    //
    //        timeZone = checkTimeZone(format, timeZone);
    //
    //        final DateTimeFormatter dtf = getDateTimeFormatter(format, timeZone.toZoneId());
    //
    //        return ZonedDateTime.parse(dateTime, dtf);
    //    }
    //
    //    private static final Map<String, Map<ZoneId, DateTimeFormatter>> dateTimeFormatterMap = new ConcurrentHashMap<>();
    //
    //    private static DateTimeFormatter getDateTimeFormatter(final String format, final ZoneId zoneId) {
    //        DateTimeFormatter dtf = null;
    //        Map<ZoneId, DateTimeFormatter> tzdftMap = dateTimeFormatterMap.get(format);
    //
    //        if (tzdftMap == null) {
    //            tzdftMap = new ConcurrentHashMap<>();
    //            dateTimeFormatterMap.put(format, tzdftMap);
    //        } else {
    //            dtf = tzdftMap.get(zoneId);
    //        }
    //
    //        if (dtf == null) {
    //            dtf = DateTimeFormatter.ofPattern(format).withZone(zoneId);
    //            tzdftMap.put(zoneId, dtf);
    //        }
    //
    //        return dtf;
    //    }

    /**
     * Format current date with format {@code yyyy-MM-dd}.
     *
     * @return
     * @see DateTimeFormatter#format(java.time.temporal.TemporalAccessor)
     */
    @Beta
    public static String formatLocalDate() {
        return format(currentDate(), LOCAL_DATE_FORMAT);
    }

    /**
     * Format current date with format with specified {@code yyyy-MM-dd HH:mm:ss}.
     *
     * @return
     * @see DateTimeFormatter#format(java.time.temporal.TemporalAccessor)
     */
    @Beta
    public static String formatLocalDateTime() {
        return format(currentDate(), LOCAL_DATETIME_FORMAT);
    }

    /**
     * Format current date with format with specified {@code yyyy-MM-dd'T'HH:mm:ss'Z'}.
     *
     * @return
     * @see DateTimeFormatter#format(java.time.temporal.TemporalAccessor)
     */
    public static String formatCurrentDateTime() {
        return format(currentDate(), ISO_8601_DATETIME_FORMAT);
    }

    /**
     * Format current date with format with specified {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}.
     *
     * @return
     * @see DateTimeFormatter#format(java.time.temporal.TemporalAccessor)
     */
    public static String formatCurrentTimestamp() {
        return format(currentTimestamp(), ISO_8601_TIMESTAMP_FORMAT);
    }

    /**
     * Format specified {@code date} with format {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} if it's a {@code Timestamp}, otherwise format it with format {@code yyyy-MM-dd'T'HH:mm:ss'Z'}.
     *
     * @param date
     * @return
     * @see DateTimeFormatter#format(java.time.temporal.TemporalAccessor)
     */
    public static String format(final java.util.Date date) {
        return format(date, null, null);
    }

    /**
     *
     * @param date
     * @param format
     * @return
     * @see DateTimeFormatter#format(java.time.temporal.TemporalAccessor)
     */
    public static String format(final java.util.Date date, final String format) {
        return format(date, format, null);
    }

    /**
     *
     * @param date
     * @param format
     * @param timeZone
     * @return
     * @see DateTimeFormatter#format(java.time.temporal.TemporalAccessor)
     */
    public static String format(final java.util.Date date, final String format, final TimeZone timeZone) {
        return formatDate(null, date, format, timeZone);
    }

    /**
     *
     * @param c
     * @return
     * @see DateTimeFormatter#format(java.time.temporal.TemporalAccessor)
     */
    public static String format(final Calendar c) {
        return format(c, null, null);
    }

    /**
     *
     * @param c
     * @param format
     * @return
     * @see DateTimeFormatter#format(java.time.temporal.TemporalAccessor)
     */
    public static String format(final Calendar c, final String format) {
        return format(c, format, null);
    }

    /**
     *
     * @param c
     * @param format
     * @param timeZone
     * @return
     * @see DateTimeFormatter#format(java.time.temporal.TemporalAccessor)
     */
    public static String format(final Calendar c, final String format, final TimeZone timeZone) {
        if ((format == null) && (timeZone == null)) {
            final StringBuilder sb = Objectory.createStringBuilder();

            fastDateFormat(sb, null, c.getTimeInMillis(), false);

            String str = sb.toString();

            Objectory.recycle(sb);

            return str;
        }

        return format(createJUDate(c), format, timeZone);
    }

    /**
     *
     * @param c
     * @return
     * @see DateTimeFormatter#format(java.time.temporal.TemporalAccessor)
     */
    public static String format(final XMLGregorianCalendar c) {
        return format(c, null, null);
    }

    /**
     *
     * @param c
     * @param format
     * @return
     * @see DateTimeFormatter#format(java.time.temporal.TemporalAccessor)
     */
    public static String format(final XMLGregorianCalendar c, final String format) {
        return format(c, format, null);
    }

    /**
     *
     * @param c
     * @param format
     * @param timeZone
     * @return
     * @see DateTimeFormatter#format(java.time.temporal.TemporalAccessor)
     */
    public static String format(final XMLGregorianCalendar c, final String format, final TimeZone timeZone) {
        if ((format == null) && (timeZone == null)) {
            final StringBuilder sb = Objectory.createStringBuilder();

            fastDateFormat(sb, null, c.toGregorianCalendar().getTimeInMillis(), false);

            String str = sb.toString();

            Objectory.recycle(sb);

            return str;
        }

        return format(createJUDate(c.toGregorianCalendar()), format, timeZone);
    }

    /**
     *
     * @param appendable
     * @param date
     * @see DateTimeFormatter#formatTo(java.time.temporal.TemporalAccessor, Appendable)
     */
    public static void formatTo(final Appendable appendable, final java.util.Date date) {
        formatTo(appendable, date, null, null);
    }

    /**
     *
     * @param appendable
     * @param date
     * @param format
     * @see DateTimeFormatter#formatTo(java.time.temporal.TemporalAccessor, Appendable)
     */
    public static void formatTo(final Appendable appendable, final java.util.Date date, final String format) {
        formatTo(appendable, date, format, null);
    }

    /**
     *
     * @param appendable
     * @param date
     * @param format
     * @param timeZone
     * @see DateTimeFormatter#formatTo(java.time.temporal.TemporalAccessor, Appendable)
     */
    public static void formatTo(final Appendable appendable, final java.util.Date date, final String format, final TimeZone timeZone) {
        formatDate(appendable, date, format, timeZone);
    }

    /**
     *
     * @param appendable
     * @param c
     * @see DateTimeFormatter#formatTo(java.time.temporal.TemporalAccessor, Appendable)
     */
    public static void formatTo(final Appendable appendable, final Calendar c) {
        formatTo(appendable, c, null, null);
    }

    /**
     *
     * @param appendable
     * @param c
     * @param format
     * @see DateTimeFormatter#formatTo(java.time.temporal.TemporalAccessor, Appendable)
     */
    public static void formatTo(final Appendable appendable, final Calendar c, final String format) {
        formatTo(appendable, c, format, null);
    }

    /**
     *
     * @param appendable
     * @param c
     * @param format
     * @param timeZone
     * @see DateTimeFormatter#formatTo(java.time.temporal.TemporalAccessor, Appendable)
     */
    public static void formatTo(final Appendable appendable, final Calendar c, final String format, final TimeZone timeZone) {
        if ((format == null) && (timeZone == null)) {
            fastDateFormat(null, appendable, c.getTimeInMillis(), false);
        } else {
            formatTo(appendable, createJUDate(c), format, timeZone);
        }
    }

    /**
     *
     * @param appendable
     * @param c
     * @see DateTimeFormatter#formatTo(java.time.temporal.TemporalAccessor, Appendable)
     */
    public static void formatTo(final Appendable appendable, final XMLGregorianCalendar c) {
        formatTo(appendable, c, null, null);
    }

    /**
     *
     * @param appendable
     * @param c
     * @param format
     * @see DateTimeFormatter#formatTo(java.time.temporal.TemporalAccessor, Appendable)
     */
    public static void formatTo(final Appendable appendable, final XMLGregorianCalendar c, final String format) {
        formatTo(appendable, c, format, null);
    }

    /**
     *
     * @param appendable
     * @param c
     * @param format
     * @param timeZone
     * @see DateTimeFormatter#formatTo(java.time.temporal.TemporalAccessor, Appendable)
     */
    public static void formatTo(final Appendable appendable, final XMLGregorianCalendar c, final String format, final TimeZone timeZone) {
        if ((format == null) && (timeZone == null)) {
            fastDateFormat(null, appendable, c.toGregorianCalendar().getTimeInMillis(), false);
        } else {
            formatTo(appendable, createJUDate(c.toGregorianCalendar()), format, timeZone);
        }
    }

    /**
     *
     * @param appendable
     * @param date
     * @param format
     * @param timeZone
     * @return
     */
    private static String formatDate(final Appendable appendable, final java.util.Date date, String format, TimeZone timeZone) {
        boolean isTimestamp = date instanceof Timestamp;

        if ((format == null) && (timeZone == null)) {
            if (appendable == null) {
                final StringBuilder sb = Objectory.createStringBuilder();

                fastDateFormat(sb, null, date.getTime(), isTimestamp);

                String str = sb.toString();

                Objectory.recycle(sb);

                return str;
            } else {
                fastDateFormat(null, appendable, date.getTime(), isTimestamp);

                return null;
            }
        }

        if (format == null) {
            format = isTimestamp ? ISO_8601_TIMESTAMP_FORMAT : ISO_8601_DATETIME_FORMAT;
        }

        timeZone = checkTimeZone(format, timeZone);

        DateFormat sdf = getSDF(format, timeZone);

        String str = sdf.format(date);

        if (appendable != null) {
            try {
                appendable.append(str);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        recycleSDF(format, timeZone, sdf);

        return str;
    }

    /**
     * Fast date format.
     * @param sb TODO
     * @param appendable
     * @param timeInMillis
     * @param isTimestamp
     */
    private static void fastDateFormat(final StringBuilder sb, final Appendable appendable, final long timeInMillis, final boolean isTimestamp) {
        Calendar c = utcCalendarPool.poll();

        if (c == null) {
            c = Calendar.getInstance(UTC_TIME_ZONE);
        }

        c.setTimeInMillis(timeInMillis);

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        int day = c.get(Calendar.DAY_OF_MONTH);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        int second = c.get(Calendar.SECOND);

        char[] utcTimestamp = utcTimestampFormatCharsPool.poll();

        if (utcTimestamp == null) {
            utcTimestamp = new char[24];
            utcTimestamp[4] = '-';
            utcTimestamp[7] = '-';
            utcTimestamp[10] = 'T';
            utcTimestamp[13] = ':';
            utcTimestamp[16] = ':';
            utcTimestamp[19] = '.';
            utcTimestamp[23] = 'Z';
        }
        //
        // copy(cbufOfSTDInt[4][year], 0, utcTimestamp, 0, 4);
        // copy(cbufOfSTDInt[2][month], 0, utcTimestamp, 5, 2);
        // copy(cbufOfSTDInt[2][day], 0, utcTimestamp, 8, 2);
        // copy(cbufOfSTDInt[2][hour], 0, utcTimestamp, 11, 2);
        // copy(cbufOfSTDInt[2][minute], 0, utcTimestamp, 14, 2);
        // copy(cbufOfSTDInt[2][second], 0, utcTimestamp, 17, 2);
        //
        utcTimestamp[0] = cbufOfSTDInt[4][year][0];
        utcTimestamp[1] = cbufOfSTDInt[4][year][1];
        utcTimestamp[2] = cbufOfSTDInt[4][year][2];
        utcTimestamp[3] = cbufOfSTDInt[4][year][3];

        utcTimestamp[5] = cbufOfSTDInt[2][month][0];
        utcTimestamp[6] = cbufOfSTDInt[2][month][1];

        utcTimestamp[8] = cbufOfSTDInt[2][day][0];
        utcTimestamp[9] = cbufOfSTDInt[2][day][1];

        utcTimestamp[11] = cbufOfSTDInt[2][hour][0];
        utcTimestamp[12] = cbufOfSTDInt[2][hour][1];

        utcTimestamp[14] = cbufOfSTDInt[2][minute][0];
        utcTimestamp[15] = cbufOfSTDInt[2][minute][1];

        utcTimestamp[17] = cbufOfSTDInt[2][second][0];
        utcTimestamp[18] = cbufOfSTDInt[2][second][1];

        if (isTimestamp) {
            utcTimestamp[19] = '.';

            int milliSecond = c.get(Calendar.MILLISECOND);
            // copy(cbufOfSTDInt[3][milliSecond], 0, utcTimestamp,
            // 20, 3);
            utcTimestamp[20] = cbufOfSTDInt[3][milliSecond][0];
            utcTimestamp[21] = cbufOfSTDInt[3][milliSecond][1];
            utcTimestamp[22] = cbufOfSTDInt[3][milliSecond][2];
        } else {
            utcTimestamp[19] = 'Z';
        }

        try {
            if (isTimestamp) {
                if (sb == null) {
                    if (appendable instanceof Writer) {
                        ((Writer) appendable).write(utcTimestamp);
                    } else {
                        appendable.append(String.valueOf(utcTimestamp));
                    }
                } else {
                    sb.append(utcTimestamp);
                }
            } else {
                if (sb == null) {
                    if (appendable instanceof Writer) {
                        ((Writer) appendable).write(utcTimestamp, 0, 20);
                    } else {
                        appendable.append(CharBuffer.wrap(utcTimestamp), 0, 20);
                    }
                } else {
                    sb.append(utcTimestamp, 0, 20);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            utcCalendarPool.add(c);
            utcTimestampFormatCharsPool.add(utcTimestamp);
        }
    }

    //    // https://stackoverflow.com/questions/47698046/datetimeformatter-iso-local-date-vs-datetimeformatter-ofpatternyyyy-mm-dd-in
    //    /**
    //     *
    //     * @param temporal
    //     * @param pattern
    //     * @return
    //     */
    //    public static String format(final TemporalAccessor temporal, String pattern) {
    //        DateTimeFormatter dtf = dtfPool.get(pattern);
    //
    //        if (dtf == null) {
    //            dtf = DateTimeFormatter.ofPattern(pattern);
    //        }
    //
    //        return dtf.format(temporal);
    //    }
    //
    //    /**
    //    *
    //    * @param temporal
    //    * @param pattern
    //    * @param appendable
    //    * @return
    //    */
    //    public static void formatTo(final TemporalAccessor temporal, String pattern, final Appendable appendable) {
    //        DateTimeFormatter dtf = dtfPool.get(pattern);
    //
    //        if (dtf == null) {
    //            dtf = DateTimeFormatter.ofPattern(pattern);
    //        }
    //
    //        dtf.formatTo(temporal, appendable);
    //    }

    //    /**
    //     *
    //     * @param zonedDateTime
    //     * @return
    //     */
    //    public static String format(final ZonedDateTime zonedDateTime) {
    //        return format(zonedDateTime, null, null);
    //    }
    //
    //    /**
    //     *
    //     * @param zonedDateTime
    //     * @param format
    //     * @return
    //     */
    //    public static String format(final ZonedDateTime zonedDateTime, final String format) {
    //        return format(zonedDateTime, format, null);
    //    }
    //
    //    /**
    //     *
    //     * @param zonedDateTime
    //     * @param format
    //     * @param timeZone
    //     * @return
    //     */
    //    public static String format(final ZonedDateTime zonedDateTime, final String format, final TimeZone timeZone) {
    //        return formatZonedDateTime(null, zonedDateTime, format, timeZone);
    //    }
    //
    //    /**
    //     *
    //     * @param appendable
    //     * @param zonedDateTime
    //     */
    //    public static void formatTo(final Appendable appendable, final ZonedDateTime zonedDateTime) {
    //        format(appendable, zonedDateTime, null, null);
    //    }
    //
    //    /**
    //     *
    //     * @param appendable
    //     * @param zonedDateTime
    //     * @param format
    //     */
    //    public static void formatTo(final Appendable appendable, final ZonedDateTime zonedDateTime, final String format) {
    //        formatZonedDateTime(appendable, zonedDateTime, format, null);
    //    }
    //
    //    /**
    //     *
    //     * @param appendable
    //     * @param zonedDateTime
    //     * @param format
    //     * @param timeZone
    //     */
    //    public static void formatTo(final Appendable appendable, final ZonedDateTime zonedDateTime, final String format, final TimeZone timeZone) {
    //        formatZonedDateTime(appendable, zonedDateTime, format, timeZone);
    //    }
    //
    //    /**
    //     *
    //     * @param appendable
    //     * @param date
    //     * @param format
    //     * @param timeZone
    //     * @return
    //     */
    //    private static String formatZonedDateTime(final Appendable appendable, final ZonedDateTime zonedDateTime, String format, TimeZone timeZone) {
    //        String ret = null;
    //
    //        if (format == null) {
    //            final ZoneId zoneId = timeZone == null ? zonedDateTime.getZone() : timeZone.toZoneId();
    //            ret = zonedDateTime.getZone().equals(zoneId) ? zonedDateTime.toString() : zonedDateTime.withZoneSameInstant(zoneId).toString();
    //        } else {
    //            final ZoneId zoneId = timeZone == null ? (format.endsWith("'Z'") ? UTC_TIME_ZONE.toZoneId() : zonedDateTime.getZone()) : timeZone.toZoneId();
    //            final DateTimeFormatter dtf = getDateTimeFormatter(format, zoneId);
    //
    //            ret = zonedDateTime.format(dtf);
    //        }
    //
    //        if (writer != null) {
    //            try {
    //                writer.write(ret);
    //            } catch (IOException e) {
    //                throw new UncheckedIOException(e);
    //            }
    //        }
    //
    //        return ret;
    //    }

    //-----------------------------------------------------------------------
    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Sets the years field to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param date the date, not null
     * @param amount the amount to set
     * @return a new {@code Date} set with the specified value
     * @throws IllegalArgumentException if the date is null
     * @since 2.4
     */
    public static <T extends java.util.Date> T setYears(final T date, final int amount) {
        return set(date, Calendar.YEAR, amount);
    }

    //-----------------------------------------------------------------------
    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Sets the months field to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param date the date, not null
     * @param amount the amount to set
     * @return a new {@code Date} set with the specified value
     * @throws IllegalArgumentException if the date is null
     * @since 2.4
     */
    public static <T extends java.util.Date> T setMonths(final T date, final int amount) {
        return set(date, Calendar.MONTH, amount);
    }

    //-----------------------------------------------------------------------
    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Sets the day of month field to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param date the date, not null
     * @param amount the amount to set
     * @return a new {@code Date} set with the specified value
     * @throws IllegalArgumentException if the date is null
     * @since 2.4
     */
    public static <T extends java.util.Date> T setDays(final T date, final int amount) {
        return set(date, Calendar.DAY_OF_MONTH, amount);
    }

    //-----------------------------------------------------------------------
    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Sets the hours field to a date returning a new object.  Hours range
     * from  0-23.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param date the date, not null
     * @param amount the amount to set
     * @return a new {@code Date} set with the specified value
     * @throws IllegalArgumentException if the date is null
     * @since 2.4
     */
    public static <T extends java.util.Date> T setHours(final T date, final int amount) {
        return set(date, Calendar.HOUR_OF_DAY, amount);
    }

    //-----------------------------------------------------------------------
    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Sets the minute field to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param date the date, not null
     * @param amount the amount to set
     * @return a new {@code Date} set with the specified value
     * @throws IllegalArgumentException if the date is null
     * @since 2.4
     */
    public static <T extends java.util.Date> T setMinutes(final T date, final int amount) {
        return set(date, Calendar.MINUTE, amount);
    }

    //-----------------------------------------------------------------------
    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Sets the seconds field to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param date the date, not null
     * @param amount the amount to set
     * @return a new {@code Date} set with the specified value
     * @throws IllegalArgumentException if the date is null
     * @since 2.4
     */
    public static <T extends java.util.Date> T setSeconds(final T date, final int amount) {
        return set(date, Calendar.SECOND, amount);
    }

    //-----------------------------------------------------------------------
    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Sets the milliseconds field to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param date the date, not null
     * @param amount the amount to set
     * @return a new {@code Date} set with the specified value
     * @throws IllegalArgumentException if the date is null
     * @since 2.4
     */
    public static <T extends java.util.Date> T setMilliseconds(final T date, final int amount) {
        return set(date, Calendar.MILLISECOND, amount);
    }

    //-----------------------------------------------------------------------
    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Sets the specified field to a date returning a new object.
     * This does not use a lenient calendar.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param date the date, not null
     * @param calendarField the {@code Calendar} field to set the amount to
     * @param amount the amount to set
     * @return a new {@code Date} set with the specified value
     * @throws IllegalArgumentException if the date is null
     * @since 2.4
     */
    private static <T extends java.util.Date> T set(final T date, final int calendarField, final int amount) {
        N.checkArgNotNull(date, DATE_STR);

        // getInstance() returns a new object, so this method is thread safe.
        final Calendar c = Calendar.getInstance();
        c.setLenient(false);
        c.setTime(date);
        c.set(calendarField, amount);

        return createDate(c.getTimeInMillis(), date.getClass());
    }

    /**
     * Adds or subtracts the specified amount of time to the given time unit,
     * based on the calendar's rules. For example, to subtract 5 days from the
     * current time of the calendar, you can achieve it by calling:
     * <p>
     * <code>N.roll(date, -5, TimeUnit.DAYS)</code>.
     *
     * @param <T>
     * @param date
     * @param amount
     * @param unit
     * @return a new instance of Date with the specified amount rolled.
     */
    @Beta
    public static <T extends java.util.Date> T roll(final T date, final long amount, final TimeUnit unit) {
        N.checkArgNotNull(date, DATE_STR);

        return createDate(date.getTime() + unit.toMillis(amount), date.getClass());
    }

    /**
     * Adds or subtracts the specified amount of time to the given calendar
     * unit, based on the calendar's rules. For example, to subtract 5 days from
     * the current time of the calendar, you can achieve it by calling:
     * <p>
     * <code>N.roll(date, -5, CalendarUnit.DAY)</code>.
     *
     * @param <T>
     * @param date
     * @param amount
     * @param unit
     * @return a new instance of Date with the specified amount rolled.
     */
    @Beta
    public static <T extends java.util.Date> T roll(final T date, final int amount, final CalendarField unit) {
        N.checkArgNotNull(date, DATE_STR);

        //    if (amount > Integer.MAX_VALUE || amount < Integer.MIN_VALUE) {
        //        throw new IllegalArgumentException("The amount :" + amount + " is too big for unit: " + unit);
        //    }

        if (unit == CalendarField.MONTH || unit == CalendarField.YEAR) {
            final Calendar c = createCalendar(date);
            c.add(unit.value(), amount);

            return createDate(c.getTimeInMillis(), date.getClass());
        } else {
            return createDate(date.getTime() + toMillis(unit, amount), date.getClass());
        }
    }

    /**
     *
     * @param amount
     * @return
     */
    private static long toMillis(final CalendarField field, final long amount) {
        switch (field) {
            case MILLISECOND:
                return amount;

            case SECOND:
                return amount * 1000L;

            case MINUTE:
                return amount * 60 * 1000L;

            case HOUR:
                return amount * 60 * 60 * 1000L;

            case DAY:
                return amount * 24 * 60 * 60 * 1000L;

            case WEEK:
                return amount * 7 * 24 * 60 * 60 * 1000L;

            default:
                throw new IllegalArgumentException("Unsupported unit: " + field);
        }
    }

    /**
     * Adds or subtracts the specified amount of time to the given time unit,
     * based on the calendar's rules. For example, to subtract 5 days from the
     * current time of the calendar, you can achieve it by calling:
     * <p>
     * <code>N.roll(c, -5, TimeUnit.DAYS)</code>.
     *
     * @param <T>
     * @param calendar
     * @param amount
     * @param unit
     * @return a new instance of Calendar with the specified amount rolled.
     */
    @Beta
    public static <T extends Calendar> T roll(final T calendar, final long amount, final TimeUnit unit) {
        N.checkArgNotNull(calendar, CALENDAR_STR); //NOSONAR

        return createCalendar(calendar, calendar.getTimeInMillis() + unit.toMillis(amount));
    }

    /**
     * Adds or subtracts the specified amount of time to the given calendar
     * unit, based on the calendar's rules. For example, to subtract 5 days from
     * the current time of the calendar, you can achieve it by calling:
     * <p>
     * <code>N.roll(c, -5, CalendarUnit.DAY)</code>.
     *
     * @param <T>
     * @param calendar
     * @param amount
     * @param unit
     * @return a new instance of Calendar with the specified amount rolled.
     */
    @Beta
    public static <T extends Calendar> T roll(final T calendar, final int amount, final CalendarField unit) {
        N.checkArgNotNull(calendar, CALENDAR_STR);

        //    if (amount > Integer.MAX_VALUE || amount < Integer.MIN_VALUE) {
        //        throw new IllegalArgumentException("The amount :" + amount + " is too big for unit: " + unit);
        //    }

        final T result = createCalendar(calendar, calendar.getTimeInMillis());

        result.add(unit.value(), amount);

        return result;
    }

    //-----------------------------------------------------------------------

    //-----------------------------------------------------------------------
    /**
     * Adds a number of years to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param date the date, not null
     * @param amount the amount to add, may be negative
     * @return
     * @throws IllegalArgumentException if the date is null
     */
    public static <T extends java.util.Date> T addYears(final T date, final int amount) {
        return roll(date, amount, CalendarField.YEAR);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of months to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param date the date, not null
     * @param amount the amount to add, may be negative
     * @return
     * @throws IllegalArgumentException if the date is null
     */
    public static <T extends java.util.Date> T addMonths(final T date, final int amount) {
        return roll(date, amount, CalendarField.MONTH);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of weeks to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param date the date, not null
     * @param amount the amount to add, may be negative
     * @return
     * @throws IllegalArgumentException if the date is null
     */
    public static <T extends java.util.Date> T addWeeks(final T date, final int amount) {
        return roll(date, amount, CalendarField.WEEK);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of days to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param date the date, not null
     * @param amount the amount to add, may be negative
     * @return
     * @throws IllegalArgumentException if the date is null
     */
    public static <T extends java.util.Date> T addDays(final T date, final int amount) {
        return roll(date, amount, CalendarField.DAY);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of hours to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param date the date, not null
     * @param amount the amount to add, may be negative
     * @return
     * @throws IllegalArgumentException if the date is null
     */
    public static <T extends java.util.Date> T addHours(final T date, final int amount) {
        return roll(date, amount, CalendarField.HOUR);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of minutes to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param date the date, not null
     * @param amount the amount to add, may be negative
     * @return
     * @throws IllegalArgumentException if the date is null
     */
    public static <T extends java.util.Date> T addMinutes(final T date, final int amount) {
        return roll(date, amount, CalendarField.MINUTE);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of seconds to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param date the date, not null
     * @param amount the amount to add, may be negative
     * @return
     * @throws IllegalArgumentException if the date is null
     */
    public static <T extends java.util.Date> T addSeconds(final T date, final int amount) {
        return roll(date, amount, CalendarField.SECOND);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of milliseconds to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param date the date, not null
     * @param amount the amount to add, may be negative
     * @return
     * @throws IllegalArgumentException if the date is null
     */
    public static <T extends java.util.Date> T addMilliseconds(final T date, final int amount) {
        return roll(date, amount, CalendarField.MILLISECOND);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of years to a calendar returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param calendar the calendar, not null
     * @param amount the amount to add, may be negative
     * @return
     * @throws IllegalArgumentException if the calendar is null
     */
    public static <T extends Calendar> T addYears(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarField.YEAR);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of months to a calendar returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param calendar the calendar, not null
     * @param amount the amount to add, may be negative
     * @return
     * @throws IllegalArgumentException if the calendar is null
     */
    public static <T extends Calendar> T addMonths(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarField.MONTH);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of weeks to a calendar returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param calendar the calendar, not null
     * @param amount the amount to add, may be negative
     * @return
     * @throws IllegalArgumentException if the calendar is null
     */
    public static <T extends Calendar> T addWeeks(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarField.WEEK);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of days to a calendar returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param calendar the calendar, not null
     * @param amount the amount to add, may be negative
     * @return
     * @throws IllegalArgumentException if the calendar is null
     */
    public static <T extends Calendar> T addDays(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarField.DAY);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of hours to a calendar returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param calendar the calendar, not null
     * @param amount the amount to add, may be negative
     * @return
     * @throws IllegalArgumentException if the calendar is null
     */
    public static <T extends Calendar> T addHours(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarField.HOUR);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of minutes to a calendar returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param calendar the calendar, not null
     * @param amount the amount to add, may be negative
     * @return
     * @throws IllegalArgumentException if the calendar is null
     */
    public static <T extends Calendar> T addMinutes(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarField.MINUTE);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of seconds to a calendar returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param calendar the calendar, not null
     * @param amount the amount to add, may be negative
     * @return
     * @throws IllegalArgumentException if the calendar is null
     */
    public static <T extends Calendar> T addSeconds(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarField.SECOND);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of milliseconds to a calendar returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param <T>
     * @param calendar the calendar, not null
     * @param amount the amount to add, may be negative
     * @return
     * @throws IllegalArgumentException if the calendar is null
     */
    public static <T extends Calendar> T addMilliseconds(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarField.MILLISECOND);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Rounds a date, leaving the field specified as the most
     * significant field.</p>
     *
     * <p>For example, if you had the date-time of 28 Mar 2002
     * 13:45:01.231, if this was passed with HOUR, it would return
     * 28 Mar 2002 14:00:00.000. If this was passed with MONTH, it
     * would return 1 April 2002 0:00:00.000.</p>
     *
     * <p>For a date in a timezone that handles the change to daylight
     * saving time, rounding to Calendar.HOUR_OF_DAY will behave as follows.
     * Suppose daylight saving time begins at 02:00 on March 30. Rounding a
     * date that crosses this time would produce the following values:
     * </p>
     * <ul>
     * <li>March 30, 2003 01:10 rounds to March 30, 2003 01:00</li>
     * <li>March 30, 2003 01:40 rounds to March 30, 2003 03:00</li>
     * <li>March 30, 2003 02:10 rounds to March 30, 2003 03:00</li>
     * <li>March 30, 2003 02:40 rounds to March 30, 2003 04:00</li>
     * </ul>
     *
     * @param <T>
     * @param date the date to work with, not null
     * @param field the field from {@code Calendar} or <code>SEMI_MONTH</code>
     * @return
     * @throws ArithmeticException if the year is over 280 million
     */
    public static <T extends java.util.Date> T round(final T date, final int field) {
        N.checkArgNotNull(date, DATE_STR);

        final Calendar gval = Calendar.getInstance();
        gval.setTime(date);
        modify(gval, field, ModifyType.ROUND);

        return createDate(gval.getTimeInMillis(), date.getClass());
    }

    /**
     *
     *
     * @param <T>
     * @param date
     * @param field
     * @return
     */
    public static <T extends java.util.Date> T round(final T date, final CalendarField field) {
        return round(date, field.value());
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Rounds a date, leaving the field specified as the most
     * significant field.</p>
     *
     * <p>For example, if you had the date-time of 28 Mar 2002
     * 13:45:01.231, if this was passed with HOUR, it would return
     * 28 Mar 2002 14:00:00.000. If this was passed with MONTH, it
     * would return 1 April 2002 0:00:00.000.</p>
     *
     * <p>For a date in a timezone that handles the change to daylight
     * saving time, rounding to Calendar.HOUR_OF_DAY will behave as follows.
     * Suppose daylight saving time begins at 02:00 on March 30. Rounding a
     * date that crosses this time would produce the following values:
     * </p>
     * <ul>
     * <li>March 30, 2003 01:10 rounds to March 30, 2003 01:00</li>
     * <li>March 30, 2003 01:40 rounds to March 30, 2003 03:00</li>
     * <li>March 30, 2003 02:10 rounds to March 30, 2003 03:00</li>
     * <li>March 30, 2003 02:40 rounds to March 30, 2003 04:00</li>
     * </ul>
     *
     * @param <T>
     * @param calendar the date to work with, not null
     * @param field the field from {@code Calendar} or <code>SEMI_MONTH</code>
     * @return
     * @throws IllegalArgumentException if the date is <code>null</code>
     * @throws ArithmeticException if the year is over 280 million
     */
    public static <T extends Calendar> T round(final T calendar, final int field) {
        N.checkArgNotNull(calendar, CALENDAR_STR);

        final Calendar rounded = (Calendar) calendar.clone();
        modify(rounded, field, ModifyType.ROUND);

        if (rounded.getClass().equals(calendar.getClass())) {
            return (T) rounded;
        }

        return createCalendar(calendar, rounded.getTimeInMillis());
    }

    /**
     *
     *
     * @param <T>
     * @param calendar
     * @param field
     * @return
     */
    public static <T extends Calendar> T round(final T calendar, final CalendarField field) {
        return round(calendar, field.value());
    }

    //-----------------------------------------------------------------------
    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Truncates a date, leaving the field specified as the most
     * significant field.</p>
     *
     * <p>For example, if you had the date-time of 28 Mar 2002
     * 13:45:01.231, if you passed with HOUR, it would return 28 Mar
     * 2002 13:00:00.000.  If this was passed with MONTH, it would
     * return 1 Mar 2002 0:00:00.000.</p>
     *
     * @param <T>
     * @param date the date to work with, not null
     * @param field the field from {@code Calendar} or <code>SEMI_MONTH</code>
     * @return
     * @throws IllegalArgumentException if the date is <code>null</code>
     * @throws ArithmeticException if the year is over 280 million
     */
    public static <T extends java.util.Date> T truncate(final T date, final int field) {
        N.checkArgNotNull(date, DATE_STR);

        final Calendar gval = Calendar.getInstance();
        gval.setTime(date);
        modify(gval, field, ModifyType.TRUNCATE);

        return createDate(gval.getTimeInMillis(), date.getClass());
    }

    /**
     *
     * @param <T>
     * @param date
     * @param field
     * @return
     */
    public static <T extends java.util.Date> T truncate(final T date, final CalendarField field) {
        return truncate(date, field.value());
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Truncates a date, leaving the field specified as the most
     * significant field.</p>
     *
     * <p>For example, if you had the date-time of 28 Mar 2002
     * 13:45:01.231, if you passed with HOUR, it would return 28 Mar
     * 2002 13:00:00.000.  If this was passed with MONTH, it would
     * return 1 Mar 2002 0:00:00.000.</p>
     *
     * @param <T>
     * @param calendar the date to work with, not null
     * @param field the field from {@code Calendar} or <code>SEMI_MONTH</code>
     * @return
     * @throws IllegalArgumentException if the date is <code>null</code>
     * @throws ArithmeticException if the year is over 280 million
     */
    public static <T extends Calendar> T truncate(final T calendar, final int field) {
        N.checkArgNotNull(calendar, CALENDAR_STR);

        final Calendar truncated = (Calendar) calendar.clone();
        modify(truncated, field, ModifyType.TRUNCATE);

        if (truncated.getClass().equals(calendar.getClass())) {
            return (T) truncated;
        }

        return createCalendar(calendar, truncated.getTimeInMillis());
    }

    /**
     *
     * @param <T>
     * @param calendar
     * @param field
     * @return
     */
    public static <T extends Calendar> T truncate(final T calendar, final CalendarField field) {
        return truncate(calendar, field.value());
    }

    //-----------------------------------------------------------------------
    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Gets a date ceiling, leaving the field specified as the most
     * significant field.</p>
     *
     * <p>For example, if you had the date-time of 28 Mar 2002
     * 13:45:01.231, if you passed with HOUR, it would return 28 Mar
     * 2002 14:00:00.000.  If this was passed with MONTH, it would
     * return 1 Apr 2002 0:00:00.000.</p>
     *
     * @param <T>
     * @param date the date to work with, not null
     * @param field the field from {@code Calendar} or <code>SEMI_MONTH</code>
     * @return
     * @throws IllegalArgumentException if the date is <code>null</code>
     * @throws ArithmeticException if the year is over 280 million
     * @since 2.5
     */
    public static <T extends java.util.Date> T ceiling(final T date, final int field) {
        N.checkArgNotNull(date, DATE_STR);

        final Calendar gval = Calendar.getInstance();
        gval.setTime(date);
        modify(gval, field, ModifyType.CEILING);

        return createDate(gval.getTimeInMillis(), date.getClass());
    }

    /**
     *
     * @param <T>
     * @param date
     * @param field
     * @return
     */
    public static <T extends java.util.Date> T ceiling(final T date, final CalendarField field) {
        return ceiling(date, field.value());
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Gets a date ceiling, leaving the field specified as the most
     * significant field.</p>
     *
     * <p>For example, if you had the date-time of 28 Mar 2002
     * 13:45:01.231, if you passed with HOUR, it would return 28 Mar
     * 2002 14:00:00.000.  If this was passed with MONTH, it would
     * return 1 Apr 2002 0:00:00.000.</p>
     *
     * @param <T>
     * @param calendar the date to work with, not null
     * @param field the field from {@code Calendar} or <code>SEMI_MONTH</code>
     * @return
     * @throws IllegalArgumentException if the date is <code>null</code>
     * @throws ArithmeticException if the year is over 280 million
     * @since 2.5
     */
    public static <T extends Calendar> T ceiling(final T calendar, final int field) {
        N.checkArgNotNull(calendar, CALENDAR_STR);

        final Calendar ceiled = (Calendar) calendar.clone();

        modify(ceiled, field, ModifyType.CEILING);

        if (ceiled.getClass().equals(calendar.getClass())) {
            return (T) ceiled;
        }

        return createCalendar(calendar, ceiled.getTimeInMillis());
    }

    /**
     *
     * @param <T>
     * @param calendar
     * @param field
     * @return
     */
    public static <T extends Calendar> T ceiling(final T calendar, final CalendarField field) {
        return ceiling(calendar, field.value());
    }

    //-----------------------------------------------------------------------
    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Internal calculation method.</p>
     *
     * @param val the calendar, not null
     * @param field
     * @param modType type to truncate, round or ceiling
     * @throws ArithmeticException if the year is over 280 million
     */
    private static void modify(final Calendar val, final int field, final ModifyType modType) { //NOSONAR
        if (val.get(Calendar.YEAR) > 280_000_000) {
            throw new ArithmeticException("Calendar value too large for accurate calculations");
        }

        if (field == Calendar.MILLISECOND) {
            return;
        }

        // ----------------- Fix for LANG-59 ---------------------- START ---------------
        // see https://issues.apache.org/jira/browse/LANG-59
        //
        // Manually truncate milliseconds, seconds and minutes, rather than using
        // Calendar methods.

        final java.util.Date date = val.getTime();
        long time = date.getTime();
        boolean done = false;

        // truncate milliseconds
        final int millisecs = val.get(Calendar.MILLISECOND);
        if (ModifyType.TRUNCATE == modType || millisecs < 500) {
            time = time - millisecs;
        }
        if (field == Calendar.SECOND) {
            done = true;
        }

        // truncate seconds
        final int seconds = val.get(Calendar.SECOND);
        if (!done && (ModifyType.TRUNCATE == modType || seconds < 30)) {
            time = time - (seconds * 1000L);
        }
        if (field == Calendar.MINUTE) {
            done = true;
        }

        // truncate minutes
        final int minutes = val.get(Calendar.MINUTE);
        if (!done && (ModifyType.TRUNCATE == modType || minutes < 30)) {
            time = time - (minutes * 60000L);
        }

        // reset time
        if (date.getTime() != time) {
            date.setTime(time);
            val.setTime(date);
        }
        // ----------------- Fix for LANG-59 ----------------------- END ----------------

        boolean roundUp = false;
        for (final int[] aField : fields) {
            for (final int element : aField) {
                if (element == field) {
                    //This is our field... we stop looping
                    if (modType == ModifyType.CEILING || modType == ModifyType.ROUND && roundUp) {
                        if (field == SEMI_MONTH) {
                            //This is a special case that's hard to generalize
                            //If the date is 1, we round up to 16, otherwise
                            //  we subtract 15 days and add 1 month
                            if (val.get(Calendar.DATE) == 1) {
                                val.add(Calendar.DATE, 15);
                            } else {
                                val.add(Calendar.DATE, -15);
                                val.add(Calendar.MONTH, 1);
                            }
                            // ----------------- Fix for LANG-440 ---------------------- START ---------------
                        } else if (field == Calendar.AM_PM) {
                            // This is a special case
                            // If the time is 0, we round up to 12, otherwise
                            //  we subtract 12 hours and add 1 day
                            if (val.get(Calendar.HOUR_OF_DAY) == 0) {
                                val.add(Calendar.HOUR_OF_DAY, 12);
                            } else {
                                val.add(Calendar.HOUR_OF_DAY, -12);
                                val.add(Calendar.DATE, 1);
                            }
                            // ----------------- Fix for LANG-440 ---------------------- END ---------------
                        } else {
                            //We need at add one to this field since the
                            //  last number causes us to round up
                            val.add(aField[0], 1);
                        }
                    }
                    return;
                }
            }
            //We have various fields that are not easy roundings
            int offset = 0;
            boolean offsetSet = false;
            //These are special types of fields that require different rounding rules
            switch (field) {
                case SEMI_MONTH:
                    if (aField[0] == Calendar.DATE) {
                        //If we're going to drop the DATE field's value,
                        //  we want to do this our own way.
                        //We need to subtract 1 since the date has a minimum of 1
                        offset = val.get(Calendar.DATE) - 1;
                        //If we're above 15 days adjustment, that means we're in the
                        //  bottom half of the month and should stay accordingly.
                        if (offset >= 15) {
                            offset -= 15;
                        }
                        //Record whether we're in the top or bottom half of that range
                        roundUp = offset > 7;
                        offsetSet = true;
                    }
                    break;
                case Calendar.AM_PM:
                    if (aField[0] == Calendar.HOUR_OF_DAY) {
                        //If we're going to drop the HOUR field's value,
                        //  we want to do this our own way.
                        offset = val.get(Calendar.HOUR_OF_DAY);
                        if (offset >= 12) {
                            offset -= 12;
                        }
                        roundUp = offset >= 6;
                        offsetSet = true;
                    }
                    break;
                default:
                    break;
            }
            if (!offsetSet) {
                final int min = val.getActualMinimum(aField[0]);
                final int max = val.getActualMaximum(aField[0]);
                //Calculate the offset from the minimum allowed value
                offset = val.get(aField[0]) - min;
                //Set roundUp if this is more than half way between the minimum and maximum
                roundUp = offset > ((max - min) / 2);
            }
            //We need to remove this field
            if (offset != 0) {
                val.set(aField[0], val.get(aField[0]) - offset);
            }
        }

        throw new IllegalArgumentException("The field " + field + " is not supported");
    }

    /**
     *
     * @param cal1
     * @param cal2
     * @param field
     * @return
     * @see #truncatedEquals(java.util.Calendar, java.util.Calendar, int)
     */
    public static boolean truncatedEquals(final Calendar cal1, final Calendar cal2, final CalendarField field) {
        return truncatedCompareTo(cal1, cal2, field) == 0;
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Determines if two calendars are equal up to no more than the specified
     * most significant field.
     *
     * @param cal1 the first calendar, not <code>null</code>
     * @param cal2 the second calendar, not <code>null</code>
     * @param field the field from {@code Calendar}
     * @return <code>true</code> if equal; otherwise <code>false</code>
     * @throws IllegalArgumentException if any argument is <code>null</code>
     * @see #truncate(Calendar, int)
     * @see #truncatedEquals(Date, Date, int)
     * @since 3.0
     */
    public static boolean truncatedEquals(final Calendar cal1, final Calendar cal2, final int field) {
        return truncatedCompareTo(cal1, cal2, field) == 0;
    }

    /**
     *
     * @param date1
     * @param date2
     * @param field
     * @return
     * @see #truncatedEquals(java.util.Date, java.util.Date, int)
     */
    public static boolean truncatedEquals(final java.util.Date date1, final java.util.Date date2, final CalendarField field) {
        return truncatedCompareTo(date1, date2, field) == 0;
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Determines if two dates are equal up to no more than the specified
     * most significant field.
     *
     * @param date1 the first date, not <code>null</code>
     * @param date2 the second date, not <code>null</code>
     * @param field the field from {@code Calendar}
     * @return <code>true</code> if equal; otherwise <code>false</code>
     * @throws IllegalArgumentException if any argument is <code>null</code>
     * @see #truncate(java.util.Date, int)
     * @see #truncatedEquals(Calendar, Calendar, int)
     * @since 3.0
     */
    public static boolean truncatedEquals(final java.util.Date date1, final java.util.Date date2, final int field) {
        return truncatedCompareTo(date1, date2, field) == 0;
    }

    /**
     *
     * @param cal1
     * @param cal2
     * @param field
     * @return
     * @see #truncatedCompareTo(Calendar, Calendar, int)
     */
    public static int truncatedCompareTo(final Calendar cal1, final Calendar cal2, final CalendarField field) {
        return truncate(cal1, field).compareTo(truncate(cal2, field));
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Determines how two calendars compare up to no more than the specified
     * most significant field.
     *
     * @param cal1 the first calendar, not <code>null</code>
     * @param cal2 the second calendar, not <code>null</code>
     * @param field the field from {@code Calendar}
     * @return a negative integer, zero, or a positive integer as the first
     * calendar is less than, equal to, or greater than the second.
     * @throws IllegalArgumentException if any argument is <code>null</code>
     * @see #truncate(Calendar, int)
     * @see #truncatedCompareTo(Date, Date, int)
     * @since 3.0
     */
    public static int truncatedCompareTo(final Calendar cal1, final Calendar cal2, final int field) {
        return truncate(cal1, field).compareTo(truncate(cal2, field));
    }

    /**
     *
     * @param date1
     * @param date2
     * @param field
     * @return
     * @see #truncatedCompareTo(java.util.Date, java.util.Date, int)
     */
    public static int truncatedCompareTo(final java.util.Date date1, final java.util.Date date2, final CalendarField field) {
        return truncate(date1, field).compareTo(truncate(date2, field));
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Determines how two dates compare up to no more than the specified
     * most significant field.
     *
     * @param date1 the first date, not <code>null</code>
     * @param date2 the second date, not <code>null</code>
     * @param field the field from <code>Calendar</code>
     * @return a negative integer, zero, or a positive integer as the first
     * date is less than, equal to, or greater than the second.
     * @throws IllegalArgumentException if any argument is <code>null</code>
     * @see #truncate(Calendar, int)
     * @see #truncatedCompareTo(Date, Date, int)
     * @since 3.0
     */
    public static int truncatedCompareTo(final java.util.Date date1, final java.util.Date date2, final int field) {
        return truncate(date1, field).compareTo(truncate(date2, field));
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of milliseconds within the
     * fragment. All datefields greater than the fragment will be ignored.</p>
     *
     * <p>Asking the milliseconds of any date will only return the number of milliseconds
     * of the current second (resulting in a number between 0 and 999). This
     * method will retrieve the number of milliseconds for any fragment.
     * For example, if you want to calculate the number of milliseconds past today,
     * your fragment is Calendar.DATE or Calendar.DAY_OF_YEAR. The result will
     * be all milliseconds of the past hour(s), minutes(s) and second(s).</p>
     *
     * <p>Valid fragments are: Calendar.YEAR, Calendar.MONTH, both
     * Calendar.DAY_OF_YEAR and Calendar.DATE, Calendar.HOUR_OF_DAY,
     * Calendar.MINUTE, Calendar.SECOND and Calendar.MILLISECOND
     * A fragment less than or equal to a SECOND field will return 0.</p>
     *
     * <ul>
     *  <li>January 1, 2008 7:15:10.538 with Calendar.SECOND as fragment will return 538</li>
     *  <li>January 6, 2008 7:15:10.538 with Calendar.SECOND as fragment will return 538</li>
     *  <li>January 6, 2008 7:15:10.538 with Calendar.MINUTE as fragment will return 10538 (10*1000 + 538)</li>
     *  <li>January 16, 2008 7:15:10.538 with Calendar.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in milliseconds)</li>
     * </ul>
     *
     * @param date the date to work with, not null
     * @param fragment the {@code Calendar} field part of date to calculate
     * @return number of milliseconds within the fragment of date
     * @throws IllegalArgumentException if the date is {@code null} or
     * fragment is not supported
     * @since 2.4
     */
    public static long getFragmentInMilliseconds(final java.util.Date date, final CalendarField fragment) {
        return getFragment(date, fragment.value(), TimeUnit.MILLISECONDS);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of seconds within the
     * fragment. All datefields greater than the fragment will be ignored.</p>
     *
     * <p>Asking the seconds of any date will only return the number of seconds
     * of the current minute (resulting in a number between 0 and 59). This
     * method will retrieve the number of seconds for any fragment.
     * For example, if you want to calculate the number of seconds past today,
     * your fragment is Calendar.DATE or Calendar.DAY_OF_YEAR. The result will
     * be all seconds of the past hour(s) and minutes(s).</p>
     *
     * <p>Valid fragments are: Calendar.YEAR, Calendar.MONTH, both
     * Calendar.DAY_OF_YEAR and Calendar.DATE, Calendar.HOUR_OF_DAY,
     * Calendar.MINUTE, Calendar.SECOND and Calendar.MILLISECOND
     * A fragment less than or equal to a SECOND field will return 0.</p>
     *
     * <ul>
     *  <li>January 1, 2008 7:15:10.538 with Calendar.MINUTE as fragment will return 10
     *   (equivalent to deprecated date.getSeconds())</li>
     *  <li>January 6, 2008 7:15:10.538 with Calendar.MINUTE as fragment will return 10
     *   (equivalent to deprecated date.getSeconds())</li>
     *  <li>January 6, 2008 7:15:10.538 with Calendar.DAY_OF_YEAR as fragment will return 26110
     *   (7*3600 + 15*60 + 10)</li>
     *  <li>January 16, 2008 7:15:10.538 with Calendar.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in seconds)</li>
     * </ul>
     *
     * @param date the date to work with, not null
     * @param fragment the {@code Calendar} field part of date to calculate
     * @return number of seconds within the fragment of date
     * @throws IllegalArgumentException if the date is {@code null} or
     * fragment is not supported
     * @since 2.4
     */
    public static long getFragmentInSeconds(final java.util.Date date, final CalendarField fragment) {
        return getFragment(date, fragment.value(), TimeUnit.SECONDS);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of minutes within the
     * fragment. All datefields greater than the fragment will be ignored.</p>
     *
     * <p>Asking the minutes of any date will only return the number of minutes
     * of the current hour (resulting in a number between 0 and 59). This
     * method will retrieve the number of minutes for any fragment.
     * For example, if you want to calculate the number of minutes past this month,
     * your fragment is Calendar.MONTH. The result will be all minutes of the
     * past day(s) and hour(s).</p>
     *
     * <p>Valid fragments are: Calendar.YEAR, Calendar.MONTH, both
     * Calendar.DAY_OF_YEAR and Calendar.DATE, Calendar.HOUR_OF_DAY,
     * Calendar.MINUTE, Calendar.SECOND and Calendar.MILLISECOND
     * A fragment less than or equal to a MINUTE field will return 0.</p>
     *
     * <ul>
     *  <li>January 1, 2008 7:15:10.538 with Calendar.HOUR_OF_DAY as fragment will return 15
     *   (equivalent to deprecated date.getMinutes())</li>
     *  <li>January 6, 2008 7:15:10.538 with Calendar.HOUR_OF_DAY as fragment will return 15
     *   (equivalent to deprecated date.getMinutes())</li>
     *  <li>January 1, 2008 7:15:10.538 with Calendar.MONTH as fragment will return 15</li>
     *  <li>January 6, 2008 7:15:10.538 with Calendar.MONTH as fragment will return 435 (7*60 + 15)</li>
     *  <li>January 16, 2008 7:15:10.538 with Calendar.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in minutes)</li>
     * </ul>
     *
     * @param date the date to work with, not null
     * @param fragment the {@code Calendar} field part of date to calculate
     * @return number of minutes within the fragment of date
     * @throws IllegalArgumentException if the date is {@code null} or
     * fragment is not supported
     * @since 2.4
     */
    public static long getFragmentInMinutes(final java.util.Date date, final CalendarField fragment) {
        return getFragment(date, fragment.value(), TimeUnit.MINUTES);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of hours within the
     * fragment. All datefields greater than the fragment will be ignored.</p>
     *
     * <p>Asking the hours of any date will only return the number of hours
     * of the current day (resulting in a number between 0 and 23). This
     * method will retrieve the number of hours for any fragment.
     * For example, if you want to calculate the number of hours past this month,
     * your fragment is Calendar.MONTH. The result will be all hours of the
     * past day(s).</p>
     *
     * <p>Valid fragments are: Calendar.YEAR, Calendar.MONTH, both
     * Calendar.DAY_OF_YEAR and Calendar.DATE, Calendar.HOUR_OF_DAY,
     * Calendar.MINUTE, Calendar.SECOND and Calendar.MILLISECOND
     * A fragment less than or equal to a HOUR field will return 0.</p>
     *
     * <ul>
     *  <li>January 1, 2008 7:15:10.538 with Calendar.DAY_OF_YEAR as fragment will return 7
     *   (equivalent to deprecated date.getHours())</li>
     *  <li>January 6, 2008 7:15:10.538 with Calendar.DAY_OF_YEAR as fragment will return 7
     *   (equivalent to deprecated date.getHours())</li>
     *  <li>January 1, 2008 7:15:10.538 with Calendar.MONTH as fragment will return 7</li>
     *  <li>January 6, 2008 7:15:10.538 with Calendar.MONTH as fragment will return 127 (5*24 + 7)</li>
     *  <li>January 16, 2008 7:15:10.538 with Calendar.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in hours)</li>
     * </ul>
     *
     * @param date the date to work with, not null
     * @param fragment the {@code Calendar} field part of date to calculate
     * @return number of hours within the fragment of date
     * @throws IllegalArgumentException if the date is {@code null} or
     * fragment is not supported
     * @since 2.4
     */
    public static long getFragmentInHours(final java.util.Date date, final CalendarField fragment) {
        return getFragment(date, fragment.value(), TimeUnit.HOURS);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of days within the
     * fragment. All datefields greater than the fragment will be ignored.</p>
     *
     * <p>Asking the days of any date will only return the number of days
     * of the current month (resulting in a number between 1 and 31). This
     * method will retrieve the number of days for any fragment.
     * For example, if you want to calculate the number of days past this year,
     * your fragment is Calendar.YEAR. The result will be all days of the
     * past month(s).</p>
     *
     * <p>Valid fragments are: Calendar.YEAR, Calendar.MONTH, both
     * Calendar.DAY_OF_YEAR and Calendar.DATE, Calendar.HOUR_OF_DAY,
     * Calendar.MINUTE, Calendar.SECOND and Calendar.MILLISECOND
     * A fragment less than or equal to a DAY field will return 0.</p>
     *
     * <ul>
     *  <li>January 28, 2008 with Calendar.MONTH as fragment will return 28
     *   (equivalent to deprecated date.getDay())</li>
     *  <li>February 28, 2008 with Calendar.MONTH as fragment will return 28
     *   (equivalent to deprecated date.getDay())</li>
     *  <li>January 28, 2008 with Calendar.YEAR as fragment will return 28</li>
     *  <li>February 28, 2008 with Calendar.YEAR as fragment will return 59</li>
     *  <li>January 28, 2008 with Calendar.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in days)</li>
     * </ul>
     *
     * @param date the date to work with, not null
     * @param fragment the {@code Calendar} field part of date to calculate
     * @return number of days  within the fragment of date
     * @throws IllegalArgumentException if the date is {@code null} or
     * fragment is not supported
     * @since 2.4
     */
    public static long getFragmentInDays(final java.util.Date date, final CalendarField fragment) {
        return getFragment(date, fragment.value(), TimeUnit.DAYS);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Gets a Date fragment for any unit.
     *
     * @param date the date to work with, not null
     * @param fragment the Calendar field part of date to calculate
     * @param unit the time unit
     * @return number of units within the fragment of the date
     * @throws NullPointerException if the date is {@code null}
     * @throws IllegalArgumentException if fragment is not supported
     * @since 2.4
     */
    private static long getFragment(final java.util.Date date, final int fragment, final TimeUnit unit) {
        N.checkArgNotNull(date, DATE_STR);

        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return getFragment(calendar, fragment, unit);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of milliseconds within the
     * fragment. All datefields greater than the fragment will be ignored.</p>
     *
     * <p>Asking the milliseconds of any date will only return the number of milliseconds
     * of the current second (resulting in a number between 0 and 999). This
     * method will retrieve the number of milliseconds for any fragment.
     * For example, if you want to calculate the number of seconds past today,
     * your fragment is Calendar.DATE or Calendar.DAY_OF_YEAR. The result will
     * be all seconds of the past hour(s), minutes(s) and second(s).</p>
     *
     * <p>Valid fragments are: Calendar.YEAR, Calendar.MONTH, both
     * Calendar.DAY_OF_YEAR and Calendar.DATE, Calendar.HOUR_OF_DAY,
     * Calendar.MINUTE, Calendar.SECOND and Calendar.MILLISECOND
     * A fragment less than or equal to a MILLISECOND field will return 0.</p>
     *
     * <ul>
     *  <li>January 1, 2008 7:15:10.538 with Calendar.SECOND as fragment will return 538
     *   (equivalent to calendar.get(Calendar.MILLISECOND))</li>
     *  <li>January 6, 2008 7:15:10.538 with Calendar.SECOND as fragment will return 538
     *   (equivalent to calendar.get(Calendar.MILLISECOND))</li>
     *  <li>January 6, 2008 7:15:10.538 with Calendar.MINUTE as fragment will return 10538
     *   (10*1000 + 538)</li>
     *  <li>January 16, 2008 7:15:10.538 with Calendar.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in milliseconds)</li>
     * </ul>
     *
     * @param calendar the calendar to work with, not null
     * @param fragment the {@code Calendar} field part of calendar to calculate
     * @return number of milliseconds within the fragment of date
     * @throws IllegalArgumentException if the date is {@code null} or
     * fragment is not supported
     * @since 2.4
     */
    public static long getFragmentInMilliseconds(final Calendar calendar, final CalendarField fragment) {
        return getFragment(calendar, fragment.value(), TimeUnit.MILLISECONDS);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of seconds within the
     * fragment. All datefields greater than the fragment will be ignored.</p>
     *
     * <p>Asking the seconds of any date will only return the number of seconds
     * of the current minute (resulting in a number between 0 and 59). This
     * method will retrieve the number of seconds for any fragment.
     * For example, if you want to calculate the number of seconds past today,
     * your fragment is Calendar.DATE or Calendar.DAY_OF_YEAR. The result will
     * be all seconds of the past hour(s) and minutes(s).</p>
     *
     * <p>Valid fragments are: Calendar.YEAR, Calendar.MONTH, both
     * Calendar.DAY_OF_YEAR and Calendar.DATE, Calendar.HOUR_OF_DAY,
     * Calendar.MINUTE, Calendar.SECOND and Calendar.MILLISECOND
     * A fragment less than or equal to a SECOND field will return 0.</p>
     *
     * <ul>
     *  <li>January 1, 2008 7:15:10.538 with Calendar.MINUTE as fragment will return 10
     *   (equivalent to calendar.get(Calendar.SECOND))</li>
     *  <li>January 6, 2008 7:15:10.538 with Calendar.MINUTE as fragment will return 10
     *   (equivalent to calendar.get(Calendar.SECOND))</li>
     *  <li>January 6, 2008 7:15:10.538 with Calendar.DAY_OF_YEAR as fragment will return 26110
     *   (7*3600 + 15*60 + 10)</li>
     *  <li>January 16, 2008 7:15:10.538 with Calendar.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in seconds)</li>
     * </ul>
     *
     * @param calendar the calendar to work with, not null
     * @param fragment the {@code Calendar} field part of calendar to calculate
     * @return number of seconds within the fragment of date
     * @throws IllegalArgumentException if the date is {@code null} or
     * fragment is not supported
     * @since 2.4
     */
    public static long getFragmentInSeconds(final Calendar calendar, final CalendarField fragment) {
        return getFragment(calendar, fragment.value(), TimeUnit.SECONDS);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of minutes within the
     * fragment. All datefields greater than the fragment will be ignored.</p>
     *
     * <p>Asking the minutes of any date will only return the number of minutes
     * of the current hour (resulting in a number between 0 and 59). This
     * method will retrieve the number of minutes for any fragment.
     * For example, if you want to calculate the number of minutes past this month,
     * your fragment is Calendar.MONTH. The result will be all minutes of the
     * past day(s) and hour(s).</p>
     *
     * <p>Valid fragments are: Calendar.YEAR, Calendar.MONTH, both
     * Calendar.DAY_OF_YEAR and Calendar.DATE, Calendar.HOUR_OF_DAY,
     * Calendar.MINUTE, Calendar.SECOND and Calendar.MILLISECOND
     * A fragment less than or equal to a MINUTE field will return 0.</p>
     *
     * <ul>
     *  <li>January 1, 2008 7:15:10.538 with Calendar.HOUR_OF_DAY as fragment will return 15
     *   (equivalent to calendar.get(Calendar.MINUTES))</li>
     *  <li>January 6, 2008 7:15:10.538 with Calendar.HOUR_OF_DAY as fragment will return 15
     *   (equivalent to calendar.get(Calendar.MINUTES))</li>
     *  <li>January 1, 2008 7:15:10.538 with Calendar.MONTH as fragment will return 15</li>
     *  <li>January 6, 2008 7:15:10.538 with Calendar.MONTH as fragment will return 435 (7*60 + 15)</li>
     *  <li>January 16, 2008 7:15:10.538 with Calendar.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in minutes)</li>
     * </ul>
     *
     * @param calendar the calendar to work with, not null
     * @param fragment the {@code Calendar} field part of calendar to calculate
     * @return number of minutes within the fragment of date
     * @throws IllegalArgumentException if the date is {@code null} or
     * fragment is not supported
     * @since 2.4
     */
    public static long getFragmentInMinutes(final Calendar calendar, final CalendarField fragment) {
        return getFragment(calendar, fragment.value(), TimeUnit.MINUTES);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of hours within the
     * fragment. All datefields greater than the fragment will be ignored.</p>
     *
     * <p>Asking the hours of any date will only return the number of hours
     * of the current day (resulting in a number between 0 and 23). This
     * method will retrieve the number of hours for any fragment.
     * For example, if you want to calculate the number of hours past this month,
     * your fragment is Calendar.MONTH. The result will be all hours of the
     * past day(s).</p>
     *
     * <p>Valid fragments are: Calendar.YEAR, Calendar.MONTH, both
     * Calendar.DAY_OF_YEAR and Calendar.DATE, Calendar.HOUR_OF_DAY,
     * Calendar.MINUTE, Calendar.SECOND and Calendar.MILLISECOND
     * A fragment less than or equal to a HOUR field will return 0.</p>
     *
     * <ul>
     *  <li>January 1, 2008 7:15:10.538 with Calendar.DAY_OF_YEAR as fragment will return 7
     *   (equivalent to calendar.get(Calendar.HOUR_OF_DAY))</li>
     *  <li>January 6, 2008 7:15:10.538 with Calendar.DAY_OF_YEAR as fragment will return 7
     *   (equivalent to calendar.get(Calendar.HOUR_OF_DAY))</li>
     *  <li>January 1, 2008 7:15:10.538 with Calendar.MONTH as fragment will return 7</li>
     *  <li>January 6, 2008 7:15:10.538 with Calendar.MONTH as fragment will return 127 (5*24 + 7)</li>
     *  <li>January 16, 2008 7:15:10.538 with Calendar.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in hours)</li>
     * </ul>
     *
     * @param calendar the calendar to work with, not null
     * @param fragment the {@code Calendar} field part of calendar to calculate
     * @return number of hours within the fragment of date
     * @throws IllegalArgumentException if the date is {@code null} or
     * fragment is not supported
     * @since 2.4
     */
    public static long getFragmentInHours(final Calendar calendar, final CalendarField fragment) {
        return getFragment(calendar, fragment.value(), TimeUnit.HOURS);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of days within the
     * fragment. All datefields greater than the fragment will be ignored.</p>
     *
     * <p>Asking the days of any date will only return the number of days
     * of the current month (resulting in a number between 1 and 31). This
     * method will retrieve the number of days for any fragment.
     * For example, if you want to calculate the number of days past this year,
     * your fragment is Calendar.YEAR. The result will be all days of the
     * past month(s).</p>
     *
     * <p>Valid fragments are: Calendar.YEAR, Calendar.MONTH, both
     * Calendar.DAY_OF_YEAR and Calendar.DATE, Calendar.HOUR_OF_DAY,
     * Calendar.MINUTE, Calendar.SECOND and Calendar.MILLISECOND
     * A fragment less than or equal to a DAY field will return 0.</p>
     *
     * <ul>
     *  <li>January 28, 2008 with Calendar.MONTH as fragment will return 28
     *   (equivalent to calendar.get(Calendar.DAY_OF_MONTH))</li>
     *  <li>February 28, 2008 with Calendar.MONTH as fragment will return 28
     *   (equivalent to calendar.get(Calendar.DAY_OF_MONTH))</li>
     *  <li>January 28, 2008 with Calendar.YEAR as fragment will return 28
     *   (equivalent to calendar.get(Calendar.DAY_OF_YEAR))</li>
     *  <li>February 28, 2008 with Calendar.YEAR as fragment will return 59
     *   (equivalent to calendar.get(Calendar.DAY_OF_YEAR))</li>
     *  <li>January 28, 2008 with Calendar.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in days)</li>
     * </ul>
     *
     * @param calendar the calendar to work with, not null
     * @param fragment the {@code Calendar} field part of calendar to calculate
     * @return number of days within the fragment of date
     * @throws IllegalArgumentException if the date is {@code null} or
     * fragment is not supported
     * @since 2.4
     */
    public static long getFragmentInDays(final Calendar calendar, final CalendarField fragment) {
        return getFragment(calendar, fragment.value(), TimeUnit.DAYS);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Gets a Calendar fragment for any unit.
     *
     * @param calendar the calendar to work with, not null
     * @param fragment the Calendar field part of calendar to calculate
     * @param unit the time unit
     * @return number of units within the fragment of the calendar
     * @throws IllegalArgumentException if the date is {@code null} or
     * fragment is not supported
     * @since 2.4
     */
    private static long getFragment(final Calendar calendar, final int fragment, final TimeUnit unit) {
        N.checkArgNotNull(calendar, CALENDAR_STR);

        long result = 0;

        final int offset = (unit == TimeUnit.DAYS) ? 0 : 1;

        // Fragments bigger than a day require a breakdown to days
        switch (fragment) {
            case Calendar.YEAR:
                result += unit.convert(calendar.get(Calendar.DAY_OF_YEAR) - offset, TimeUnit.DAYS); //NOSONAR
                break;
            case Calendar.MONTH:
                result += unit.convert(calendar.get(Calendar.DAY_OF_MONTH) - offset, TimeUnit.DAYS); //NOSONAR
                break;
            default:
                break;
        }

        switch (fragment) {
            // Number of days already calculated for these cases
            case Calendar.YEAR:
            case Calendar.MONTH:

                // The rest of the valid cases
            case Calendar.DAY_OF_YEAR:
            case Calendar.DATE:
                result += unit.convert(calendar.get(Calendar.HOUR_OF_DAY), TimeUnit.HOURS);
                //$FALL-THROUGH$
            case Calendar.HOUR_OF_DAY:
                result += unit.convert(calendar.get(Calendar.MINUTE), TimeUnit.MINUTES);
                //$FALL-THROUGH$
            case Calendar.MINUTE:
                result += unit.convert(calendar.get(Calendar.SECOND), TimeUnit.SECONDS);
                //$FALL-THROUGH$
            case Calendar.SECOND:
                result += unit.convert(calendar.get(Calendar.MILLISECOND), TimeUnit.MILLISECONDS);
                break;
            case Calendar.MILLISECOND:
                break; //never useful
            default:
                throw new IllegalArgumentException("The fragment " + fragment + " is not supported");
        }
        return result;
    }

    //-----------------------------------------------------------------------
    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Checks if two date objects are on the same day ignoring time.</p>
     *
     * <p>28 Mar 2002 13:45 and 28 Mar 2002 06:01 would return true.
     * 28 Mar 2002 13:45 and 12 Mar 2002 13:45 would return false.
     * </p>
     *
     * @param date1 the first date, not altered, not null
     * @param date2 the second date, not altered, not null
     * @return true if they represent the same day
     * @throws IllegalArgumentException if either date is {@code null}
     * @since 2.1
     */
    public static boolean isSameDay(final java.util.Date date1, final java.util.Date date2) {
        N.checkArgNotNull(date1, DATE1_STR);
        N.checkArgNotNull(date2, DATE2_STR);

        final Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);

        final Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);

        return isSameDay(cal1, cal2);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Checks if two calendar objects are on the same day ignoring time.</p>
     *
     * <p>28 Mar 2002 13:45 and 28 Mar 2002 06:01 would return true.
     * 28 Mar 2002 13:45 and 12 Mar 2002 13:45 would return false.
     * </p>
     *
     * @param cal1 the first calendar, not altered, not null
     * @param cal2 the second calendar, not altered, not null
     * @return true if they represent the same day
     * @throws IllegalArgumentException if either calendar is {@code null}
     * @since 2.1
     */
    public static boolean isSameDay(final Calendar cal1, final Calendar cal2) {
        N.checkArgNotNull(cal1, CAL1_STR);
        N.checkArgNotNull(cal2, CAL2_STR);

        return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     *
     *
     * @param date1
     * @param date2
     * @return
     */
    public static boolean isSameMonth(final java.util.Date date1, final java.util.Date date2) {
        N.checkArgNotNull(date1, DATE1_STR);
        N.checkArgNotNull(date2, DATE2_STR);

        final Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);

        final Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);

        return isSameMonth(cal1, cal2);
    }

    /**
     *
     *
     * @param cal1
     * @param cal2
     * @return
     */
    public static boolean isSameMonth(final Calendar cal1, final Calendar cal2) {
        N.checkArgNotNull(cal1, CAL1_STR);
        N.checkArgNotNull(cal2, CAL2_STR);

        return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH);
    }

    /**
     *
     *
     * @param date1
     * @param date2
     * @return
     */
    public static boolean isSameYear(final java.util.Date date1, final java.util.Date date2) {
        N.checkArgNotNull(date1, DATE1_STR);
        N.checkArgNotNull(date2, DATE2_STR);

        final Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);

        final Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);

        return isSameYear(cal1, cal2);
    }

    /**
     *
     *
     * @param cal1
     * @param cal2
     * @return
     */
    public static boolean isSameYear(final Calendar cal1, final Calendar cal2) {
        N.checkArgNotNull(cal1, CAL1_STR);
        N.checkArgNotNull(cal2, CAL2_STR);

        return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR);
    }

    //-----------------------------------------------------------------------
    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Checks if two date objects represent the same instant in time.</p>
     *
     * <p>This method compares the long millisecond time of the two objects.</p>
     *
     * @param date1  the first date, not altered, not null
     * @param date2  the second date, not altered, not null
     * @return true if they represent the same millisecond instant
     * @throws IllegalArgumentException if either date is {@code null}
     * @since 2.1
     */
    public static boolean isSameInstant(final java.util.Date date1, final java.util.Date date2) {
        N.checkArgNotNull(date1, DATE1_STR);
        N.checkArgNotNull(date2, DATE2_STR);
        return date1.getTime() == date2.getTime();
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Checks if two calendar objects represent the same instant in time.</p>
     *
     * <p>This method compares the long millisecond time of the two objects.</p>
     *
     * @param cal1  the first calendar, not altered, not null
     * @param cal2  the second calendar, not altered, not null
     * @return true if they represent the same millisecond instant
     * @throws IllegalArgumentException if either date is {@code null}
     * @since 2.1
     */
    public static boolean isSameInstant(final Calendar cal1, final Calendar cal2) {
        N.checkArgNotNull(cal1, CAL1_STR);
        N.checkArgNotNull(cal2, CAL2_STR);

        return cal1.getTime().getTime() == cal2.getTime().getTime();
    }

    //-----------------------------------------------------------------------
    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Checks if two calendar objects represent the same local time.</p>
     *
     * <p>This method compares the values of the fields of the two objects.
     * In addition, both calendars must be the same of the same type.</p>
     *
     * @param cal1  the first calendar, not altered, not null
     * @param cal2  the second calendar, not altered, not null
     * @return true if they represent the same millisecond instant
     * @throws IllegalArgumentException if either date is {@code null}
     * @since 2.1
     */
    public static boolean isSameLocalTime(final Calendar cal1, final Calendar cal2) {
        N.checkArgNotNull(cal1, CAL1_STR);
        N.checkArgNotNull(cal2, CAL2_STR);

        return cal1.get(Calendar.MILLISECOND) == cal2.get(Calendar.MILLISECOND) && cal1.get(Calendar.SECOND) == cal2.get(Calendar.SECOND)
                && cal1.get(Calendar.MINUTE) == cal2.get(Calendar.MINUTE) && cal1.get(Calendar.HOUR_OF_DAY) == cal2.get(Calendar.HOUR_OF_DAY)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) && cal1.getClass() == cal2.getClass();
    }

    /**
     * Gets the sdf.
     *
     * @param format
     * @param timeZone
     * @return
     */
    private static DateFormat getSDF(final String format, final TimeZone timeZone) {
        DateFormat sdf = null;

        if (timeZone == UTC_TIME_ZONE) {
            if ((format.length() == 28) && format.equals(ISO_8601_TIMESTAMP_FORMAT)) {
                sdf = utcTimestampDFPool.poll();

                if (sdf == null) {
                    sdf = new SimpleDateFormat(format);
                    sdf.setTimeZone(timeZone);
                }

                return sdf;
            } else if ((format.length() == 24) && format.equals(ISO_8601_DATETIME_FORMAT)) {
                sdf = utcDateTimeDFPool.poll();

                if (sdf == null) {
                    sdf = new SimpleDateFormat(format);
                    sdf.setTimeZone(timeZone);
                }

                return sdf;
            }
        }

        Queue<DateFormat> queue = dfPool.get(format);

        if (queue == null) {
            queue = new ArrayBlockingQueue<>(POOL_SIZE);
            dfPool.put(format, queue);
        }

        sdf = queue.poll();

        if (sdf == null) {
            sdf = new SimpleDateFormat(format);
        }

        sdf.setTimeZone(timeZone);

        return sdf;
    }

    /**
     *
     * @param format
     * @param timeZone
     * @param sdf
     */
    private static void recycleSDF(final String format, final TimeZone timeZone, final DateFormat sdf) {
        if (timeZone == UTC_TIME_ZONE) {
            if ((format.length() == 28) && format.equals(ISO_8601_TIMESTAMP_FORMAT)) {
                utcTimestampDFPool.add(sdf);
            } else if ((format.length() == 24) && format.equals(ISO_8601_DATETIME_FORMAT)) {
                utcDateTimeDFPool.add(sdf);
            } else {
                dfPool.get(format).add(sdf);
            }
        } else {
            dfPool.get(format).add(sdf);
        }
    }

    /**
     * Check date format.
     *
     * @param str
     * @param format
     * @return
     */
    @SuppressWarnings("fallthrough")
    private static String checkDateFormat(final String str, final String format) {
        if (Strings.isEmpty(format)) {
            int len = str.length();

            switch (len) {
                case 4:
                    return LOCAL_YEAR_FORMAT;

                case 5:
                    if (str.charAt(2) == '/') {
                        return LOCAL_MONTH_DAY_FORMAT_SLASH;
                    } else {
                        return LOCAL_MONTH_DAY_FORMAT;
                    }

                case 8:
                    return LOCAL_TIME_FORMAT;

                case 10:
                    if (str.charAt(4) == '/') {
                        return LOCAL_DATE_FORMAT_SLASH;
                    } else {
                        return LOCAL_DATE_FORMAT;
                    }

                case 19:
                    if (str.charAt(4) == '/') {
                        return LOCAL_DATETIME_FORMAT_SLASH;
                    } else {
                        return LOCAL_DATETIME_FORMAT;
                    }

                case 23:
                    if (str.charAt(4) == '/') {
                        return LOCAL_TIMESTAMP_FORMAT_SLASH;
                    } else {
                        return LOCAL_TIMESTAMP_FORMAT;
                    }

                case 24:
                    if (str.charAt(4) == '/') {
                        return ISO_8601_DATETIME_FORMAT_SLASH;
                    } else {
                        return ISO_8601_DATETIME_FORMAT;
                    }

                case 28:
                    if (str.charAt(4) == '/') {
                        return ISO_8601_TIMESTAMP_FORMAT_SLASH;
                    } else {
                        return ISO_8601_TIMESTAMP_FORMAT;
                    }

                case 29:
                    if (str.charAt(3) == ',') {
                        return RFC1123_DATE_FORMAT;
                    }

                default:
                    // throw new RuntimeException("No valid date format found for: " + str);
                    return null;
            }
        }

        return format;
    }

    /**
     * Check time zone.
     *
     * @param format
     * @param timeZone
     * @return
     */
    private static TimeZone checkTimeZone(final String format, TimeZone timeZone) {
        if (timeZone == null) {
            timeZone = format != null && format.endsWith("'Z'") ? UTC_TIME_ZONE : DEFAULT_TIME_ZONE;
        }

        return timeZone;
    }

    /**
     * Fast date parse.
     *
     * @param str
     * @param format
     * @param timeZone
     * @return
     */
    private static long fastDateParse(final String str, final String format, final TimeZone timeZone) {
        if (!((str.length() == 24) || (str.length() == 20) || (str.length() == 19) || (str.length() == 23)) || !(format.equals(ISO_8601_TIMESTAMP_FORMAT)
                || format.equals(ISO_8601_DATETIME_FORMAT) || format.equals(LOCAL_DATETIME_FORMAT) || format.equals(LOCAL_TIMESTAMP_FORMAT))) {
            return 0;
        }

        //
        // if (!((str.charAt(4) == '-') && (str.charAt(7) == '-') &&
        // (str.charAt(10) == 'T') && (str.charAt(13) == ':') && (str.charAt(16)
        // == ':') && (str
        // .charAt(str.length() - 1) == 'Z'))) {
        // return 0;
        // }
        //
        // int year = Integer.valueOf(str.substring(0, 4));
        // int month = Integer.valueOf(str.substring(5, 7)) - 1;
        // int date = Integer.valueOf(str.substring(8, 10));
        // int hourOfDay = Integer.valueOf(str.substring(11, 13));
        // int minute = Integer.valueOf(str.substring(14, 16));
        // int second = Integer.valueOf(str.substring(17, 19));
        // int milliSecond = (str.length() == 24) ?
        // Integer.valueOf(str.substring(20, 23)) : 0;
        //
        //

        int year = parseInt(str, 0, 4);
        int month = parseInt(str, 5, 7) - 1;
        int date = parseInt(str, 8, 10);
        int hourOfDay = parseInt(str, 11, 13);
        int minute = parseInt(str, 14, 16);
        int second = parseInt(str, 17, 19);
        int milliSecond = ((str.length() == 24) || (str.length() == 23)) ? parseInt(str, 20, 23) : 0;

        Calendar c = null;
        Queue<Calendar> timeZoneCalendarQueue = null;

        if (timeZone == UTC_TIME_ZONE) {
            c = utcCalendarPool.poll();
        } else {
            timeZoneCalendarQueue = calendarPool.get(timeZone);

            if (timeZoneCalendarQueue == null) {
                timeZoneCalendarQueue = new ArrayBlockingQueue<>(POOL_SIZE);
                calendarPool.put(timeZone, timeZoneCalendarQueue);
            } else {
                c = timeZoneCalendarQueue.poll();
            }
        }

        if (c == null) {
            c = Calendar.getInstance(timeZone);
        }

        c.set(year, month, date, hourOfDay, minute, second);
        c.set(Calendar.MILLISECOND, milliSecond);

        long timeInMillis = c.getTimeInMillis();

        if (timeZone == UTC_TIME_ZONE) {
            utcCalendarPool.add(c);
        } else {
            timeZoneCalendarQueue.add(c);
        }

        return timeInMillis;
    }

    /**
     * Parses the int.
     *
     * @param str
     * @param fromIndex
     * @param toIndex
     * @return
     */
    private static int parseInt(final String str, int fromIndex, final int toIndex) {
        int result = 0;

        while (fromIndex < toIndex) {
            result = (result * 10) + (str.charAt(fromIndex++) - 48);
        }

        return result;
    }

    /**
     * Creates the date.
     * @param millis
     * @param cls
     *
     * @param <T>
     * @return
     */
    private static <T extends java.util.Date> T createDate(final long millis, final Class<? extends java.util.Date> cls) {
        java.util.Date result = null;

        if (cls.equals(java.util.Date.class)) {
            result = new java.util.Date(millis);
        } else if (cls.equals(java.sql.Date.class)) { // NOSONAR
            result = new java.sql.Date(millis); // NOSONAR
        } else if (cls.equals(Time.class)) {
            result = new Time(millis);
        } else if (cls.equals(Timestamp.class)) {
            result = new Timestamp(millis);
        } else {
            result = ClassUtil.invokeConstructor(ClassUtil.getDeclaredConstructor(cls, long.class), millis);
        }

        return (T) result;
    }

    /**
     * Creates the calendar.
     *
     * @param <T>
     * @param c
     * @param millis
     * @return
     */
    private static <T extends Calendar> T createCalendar(final T c, final long millis) {
        final Class<T> cls = (Class<T>) c.getClass();

        Calendar result = null;

        if (cls.equals(Calendar.class)) {
            result = Calendar.getInstance();
        } else if (cls.equals(GregorianCalendar.class)) {
            result = GregorianCalendar.getInstance();
        } else {
            result = ClassUtil.invokeConstructor(ClassUtil.getDeclaredConstructor(cls, long.class), millis);
        }

        result.setTimeInMillis(millis);

        if (!N.equals(c.getTimeZone(), result.getTimeZone())) {
            result.setTimeZone(c.getTimeZone());
        }

        return (T) result;
    }

    /**
     * Calendar modification types.
     */
    private enum ModifyType {
        /**
         * Truncation.
         */
        TRUNCATE,

        /**
         * Rounding.
         */
        ROUND,

        /**
         * Ceiling.
         */
        CEILING
    }

    /**
     *
     *
     * @param date
     * @return
     */
    public static boolean isLastDateOfMonth(final java.util.Date date) {
        N.checkArgNotNull(date, DATE_STR);

        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        return cal.get(Calendar.DAY_OF_MONTH) == cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /**
     *
     *
     * @param date
     * @return
     */
    public static boolean isLastDateOfYear(final java.util.Date date) {
        N.checkArgNotNull(date, DATE_STR);

        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        return cal.get(Calendar.DAY_OF_YEAR) == cal.getActualMaximum(Calendar.DAY_OF_YEAR);
    }

    /**
     *
     *
     * @param date
     * @return
     */
    public static int getLastDateOfMonth(final java.util.Date date) {
        N.checkArgNotNull(date, DATE_STR);

        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /**
     *
     *
     * @param date
     * @return
     */
    public static int getLastDateOfYear(final java.util.Date date) {
        N.checkArgNotNull(date, DATE_STR);

        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        return cal.getActualMaximum(Calendar.DAY_OF_YEAR);
    }

    /**
     * The Class DateTimeUtil.
     */
    @Beta
    public static final class DateTimeUtil extends DateUtil {

        private DateTimeUtil() {
            // singleton.
        }
    }

    /**
     * The Class Dates.
     */
    @Beta
    public static final class Dates extends DateUtil {

        private Dates() {
            // singleton.
        }
    }
}
