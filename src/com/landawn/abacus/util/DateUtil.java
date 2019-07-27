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
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.landawn.abacus.exception.UncheckedIOException;

/**
 * 
 * @since 1.2.6
 * 
 * @author Haiyang Li
 */
public abstract class DateUtil {

    // ...
    public static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");
    /**
     * The system default time zone
     */
    public static final TimeZone LOCAL_TIME_ZONE = TimeZone.getDefault();

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
    public final static String RFC1123_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

    /**
     * This is half a month, so this represents whether a date is in the top
     * or bottom half of the month.
     */
    public static final int SEMI_MONTH = 1001;

    private static final int[][] fields = { { Calendar.MILLISECOND }, { Calendar.SECOND }, { Calendar.MINUTE }, { Calendar.HOUR_OF_DAY, Calendar.HOUR },
            { Calendar.DATE, Calendar.DAY_OF_MONTH, Calendar.AM_PM
            /* Calendar.DAY_OF_YEAR, Calendar.DAY_OF_WEEK, Calendar.DAY_OF_WEEK_IN_MONTH */
            }, { Calendar.MONTH, SEMI_MONTH }, { Calendar.YEAR }, { Calendar.ERA } };

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

    public static GregorianCalendar currentGregorianCalendar() {
        return new GregorianCalendar();
    }

    public static XMLGregorianCalendar currentXMLGregorianCalendar() {
        return dataTypeFactory.newXMLGregorianCalendar(currentGregorianCalendar());
    }

    private static final Map<String, Queue<DateFormat>> dfPool = new ObjectPool<>(64);
    private static final Map<TimeZone, Queue<Calendar>> calendarPool = new ObjectPool<>(64);
    private static final Queue<DateFormat> utcTimestampDFPool = new ArrayBlockingQueue<>(Internals.POOL_SIZE);
    private static final Queue<DateFormat> utcDateTimeDFPool = new ArrayBlockingQueue<>(Internals.POOL_SIZE);
    private static final Queue<Calendar> utcCalendarPool = new ArrayBlockingQueue<>(Internals.POOL_SIZE);
    // ...
    private static final Queue<char[]> utcTimestampFormatCharsPool = new ArrayBlockingQueue<>(Internals.POOL_SIZE);
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
                        cbufOfSTDInt[i][k] = ("0" + String.valueOf(k)).toCharArray();
                    } else {
                        cbufOfSTDInt[i][k] = String.valueOf(k).toCharArray();
                    }
                } else if (i == 3) {
                    if (k < 10) {
                        cbufOfSTDInt[i][k] = ("00" + String.valueOf(k)).toCharArray();
                    } else if (k < 100) {
                        cbufOfSTDInt[i][k] = ("0" + String.valueOf(k)).toCharArray();
                    } else {
                        cbufOfSTDInt[i][k] = String.valueOf(k).toCharArray();
                    }
                } else if (i == 4) {
                    if (k < 10) {
                        cbufOfSTDInt[i][k] = ("000" + String.valueOf(k)).toCharArray();
                    } else if (k < 100) {
                        cbufOfSTDInt[i][k] = ("00" + String.valueOf(k)).toCharArray();
                    } else if (k < 1000) {
                        cbufOfSTDInt[i][k] = ("0" + String.valueOf(k)).toCharArray();
                    } else {
                        cbufOfSTDInt[i][k] = String.valueOf(k).toCharArray();
                    }
                }
            }
        }
    }

    DateUtil() {
        // singleton
    }

    public static java.util.Date createJUDate(final Calendar calendar) {
        return (calendar == null) ? null : createJUDate(calendar.getTimeInMillis());
    }

    public static java.util.Date createJUDate(final java.util.Date date) {
        return (date == null) ? null : createJUDate(date.getTime());
    }

    public static java.util.Date createJUDate(final long timeInMillis) {
        N.checkArgPositive(timeInMillis, "timeInMillis");

        return (timeInMillis == 0) ? null : new java.util.Date(timeInMillis);
    }

    public static Date createDate(final Calendar calendar) {
        return (calendar == null) ? null : createDate(calendar.getTimeInMillis());
    }

    public static Date createDate(final java.util.Date date) {
        return (date == null) ? null : createDate(date.getTime());
    }

    public static Date createDate(final long timeInMillis) {
        N.checkArgPositive(timeInMillis, "timeInMillis");

        return (timeInMillis == 0) ? null : new Date(timeInMillis);
    }

    public static Time createTime(final Calendar calendar) {
        return (calendar == null) ? null : createTime(calendar.getTimeInMillis());
    }

    public static Time createTime(final java.util.Date date) {
        return (date == null) ? null : createTime(date.getTime());
    }

    public static Time createTime(final long timeInMillis) {
        N.checkArgPositive(timeInMillis, "timeInMillis");

        return (timeInMillis == 0) ? null : new Time(timeInMillis);
    }

    public static Timestamp createTimestamp(final Calendar calendar) {
        return (calendar == null) ? null : createTimestamp(calendar.getTimeInMillis());
    }

    public static Timestamp createTimestamp(final java.util.Date date) {
        return (date == null) ? null : createTimestamp(date.getTime());
    }

    public static Timestamp createTimestamp(final long timeInMillis) {
        N.checkArgPositive(timeInMillis, "timeInMillis");

        return (timeInMillis == 0) ? null : new Timestamp(timeInMillis);
    }

    public static Calendar createCalendar(final Calendar calendar) {
        return (calendar == null) ? null : createCalendar(calendar.getTimeInMillis());
    }

    public static Calendar createCalendar(final java.util.Date date) {
        return (date == null) ? null : createCalendar(date.getTime());
    }

    public static Calendar createCalendar(final long timeInMillis) {
        N.checkArgPositive(timeInMillis, "timeInMillis");

        if (timeInMillis == 0) {
            return null;
        }

        final Calendar c = Calendar.getInstance();

        c.setTimeInMillis(timeInMillis);

        return c;
    }

    public static Calendar createCalendar(final long timeInMillis, final TimeZone tz) {
        N.checkArgPositive(timeInMillis, "timeInMillis");

        if (timeInMillis == 0) {
            return null;
        }

        final Calendar c = Calendar.getInstance(tz);

        c.setTimeInMillis(timeInMillis);

        return c;
    }

    public static GregorianCalendar createGregorianCalendar(final Calendar calendar) {
        return (calendar == null) ? null : createGregorianCalendar(calendar.getTimeInMillis());
    }

    public static GregorianCalendar createGregorianCalendar(final java.util.Date date) {
        return (date == null) ? null : createGregorianCalendar(date.getTime());
    }

    public static GregorianCalendar createGregorianCalendar(final long timeInMillis) {
        N.checkArgPositive(timeInMillis, "timeInMillis");

        if (timeInMillis == 0) {
            return null;
        }

        final GregorianCalendar c = new GregorianCalendar();

        c.setTimeInMillis(timeInMillis);

        return c;
    }

    public static GregorianCalendar createGregorianCalendar(final long timeInMillis, final TimeZone tz) {
        N.checkArgPositive(timeInMillis, "timeInMillis");

        if (timeInMillis == 0) {
            return null;
        }

        final GregorianCalendar c = new GregorianCalendar(tz);

        c.setTimeInMillis(timeInMillis);

        return c;
    }

    public static XMLGregorianCalendar createXMLGregorianCalendar(final Calendar calendar) {
        return (calendar == null) ? null : createXMLGregorianCalendar(calendar.getTimeInMillis());
    }

    public static XMLGregorianCalendar createXMLGregorianCalendar(final java.util.Date date) {
        return (date == null) ? null : createXMLGregorianCalendar(date.getTime());
    }

    public static XMLGregorianCalendar createXMLGregorianCalendar(final long timeInMillis) {
        N.checkArgPositive(timeInMillis, "timeInMillis");

        if (timeInMillis == 0) {
            return null;
        }

        return dataTypeFactory.newXMLGregorianCalendar(createGregorianCalendar(timeInMillis));
    }

    public static java.util.Date parseJUDate(final String date) {
        return parseJUDate(date, null);
    }

    public static java.util.Date parseJUDate(final String date, final String format) {
        return parseJUDate(date, format, null);
    }

    /**
     * Converts the specified <code>date</code> with the specified {@code format} to a new instance of java.util.Date.
     * <code>null</code> is returned if the specified <code>date</code> is null or empty.
     *
     * @param date
     * @param format
     * @throws IllegalArgumentException
     *             if the date given can't be parsed with specified format.
     */
    public static java.util.Date parseJUDate(final String date, final String format, final TimeZone timeZone) {
        if (N.isNullOrEmpty(date) || (date.length() == 4 && "null".equalsIgnoreCase(date))) {
            return null;
        }

        return createJUDate(parse(date, format, timeZone));
    }

    public static Date parseDate(final String date) {
        return parseDate(date, null);
    }

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
     * @return
     */
    public static Date parseDate(final String date, final String format, final TimeZone timeZone) {
        if (N.isNullOrEmpty(date) || (date.length() == 4 && "null".equalsIgnoreCase(date))) {
            return null;
        }

        return createDate(parse(date, format, timeZone));
    }

    public static Time parseTime(final String date) {
        return parseTime(date, null);
    }

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
     * @return
     */
    public static Time parseTime(final String date, final String format, final TimeZone timeZone) {
        if (N.isNullOrEmpty(date) || (date.length() == 4 && "null".equalsIgnoreCase(date))) {
            return null;
        }

        return createTime(parse(date, format, timeZone));
    }

    public static Timestamp parseTimestamp(final String date) {
        return parseTimestamp(date, null);
    }

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
     * @return
     */
    public static Timestamp parseTimestamp(final String date, final String format, final TimeZone timeZone) {
        if (N.isNullOrEmpty(date) || (date.length() == 4 && "null".equalsIgnoreCase(date))) {
            return null;
        }

        return createTimestamp(parse(date, format, timeZone));
    }

    public static Calendar parseCalendar(final String calendar) {
        return parseCalendar(calendar, null);
    }

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
     * @return
     */
    public static Calendar parseCalendar(final String calendar, final String format, final TimeZone timeZone) {
        if (N.isNullOrEmpty(calendar) || (calendar.length() == 4 && "null".equalsIgnoreCase(calendar))) {
            return null;
        }

        return createCalendar(parse(calendar, format, timeZone));
    }

    public static GregorianCalendar parseGregorianCalendar(final String calendar) {
        return parseGregorianCalendar(calendar, null);
    }

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
     * @return
     */
    public static GregorianCalendar parseGregorianCalendar(final String calendar, final String format, final TimeZone timeZone) {
        if (N.isNullOrEmpty(calendar) || (calendar.length() == 4 && "null".equalsIgnoreCase(calendar))) {
            return null;
        }

        return createGregorianCalendar(parse(calendar, format, timeZone));
    }

    public static XMLGregorianCalendar parseXMLGregorianCalendar(final String calendar) {
        return parseXMLGregorianCalendar(calendar, null);
    }

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
     * @return
     */
    public static XMLGregorianCalendar parseXMLGregorianCalendar(final String calendar, final String format, final TimeZone timeZone) {
        if (N.isNullOrEmpty(calendar) || (calendar.length() == 4 && "null".equalsIgnoreCase(calendar))) {
            return null;
        }

        return createXMLGregorianCalendar(parse(calendar, format, timeZone));
    }

    private static long parse(final String date, String format, TimeZone timeZone) {
        if ((format == null) && date.length() > 4 && (date.charAt(2) >= '0' && date.charAt(2) <= '9' && date.charAt(4) >= '0' && date.charAt(4) <= '9')) {
            try {
                return Long.parseLong(date);
            } catch (NumberFormatException e) {
                // ignore.
            }
        }

        format = checkDateFormat(date, format);

        if (N.isNullOrEmpty(format)) {
            if (timeZone == null) {
                return ISO8601Util.parse(date).getTime();
            } else {
                throw new RuntimeException("Unsupported date format: " + format + " with time zone: " + timeZone);
            }
        }

        timeZone = checkTimeZone(format, timeZone);

        long timeInMillis = fastDateParse(date, format, timeZone);

        if (timeInMillis > 0) {
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

    public static String format(final java.util.Date date) {
        return format(date, null, null);
    }

    public static String format(final java.util.Date date, final String format) {
        return format(date, format, null);
    }

    public static String format(final java.util.Date date, final String format, final TimeZone timeZone) {
        return formatDate(null, date, format, timeZone);
    }

    public static void format(final Writer writer, final java.util.Date date) {
        format(writer, date, null, null);
    }

    public static void format(final Writer writer, final java.util.Date date, final String format) {
        formatDate(writer, date, format, null);
    }

    public static void format(final Writer writer, final java.util.Date date, final String format, final TimeZone timeZone) {
        formatDate(writer, date, format, timeZone);
    }

    public static String format(final Calendar c) {
        return format(c, null, null);
    }

    public static String format(final Calendar c, final String format) {
        return format(c, format, null);
    }

    public static String format(final Calendar c, final String format, final TimeZone timeZone) {
        if ((format == null) && (timeZone == null)) {
            final BufferedWriter cbuff = Objectory.createBufferedWriter();
            fastDateFormat(cbuff, c.getTimeInMillis(), false);

            String str = cbuff.toString();

            Objectory.recycle(cbuff);

            return str;
        }

        return format(createJUDate(c), format, timeZone);
    }

    public static void format(final Writer writer, final Calendar c) {
        format(writer, c, null, null);
    }

    public static void format(final Writer writer, final Calendar c, final String format) {
        format(writer, c, format, null);
    }

    public static void format(final Writer writer, final Calendar c, final String format, final TimeZone timeZone) {
        if ((format == null) && (timeZone == null)) {
            fastDateFormat(writer, c.getTimeInMillis(), false);
        } else {
            format(writer, createJUDate(c), format, timeZone);
        }
    }

    public static String format(final XMLGregorianCalendar c) {
        return format(c, null, null);
    }

    public static String format(final XMLGregorianCalendar c, final String format) {
        return format(c, format, null);
    }

    public static String format(final XMLGregorianCalendar c, final String format, final TimeZone timeZone) {
        if ((format == null) && (timeZone == null)) {
            final BufferedWriter cbuff = Objectory.createBufferedWriter();

            fastDateFormat(cbuff, c.toGregorianCalendar().getTimeInMillis(), false);

            String str = cbuff.toString();

            Objectory.recycle(cbuff);

            return str;
        }

        return format(createJUDate(c.toGregorianCalendar()), format, timeZone);
    }

    public static void format(final Writer writer, final XMLGregorianCalendar c) {
        format(writer, c, null, null);
    }

    public static void format(final Writer writer, final XMLGregorianCalendar c, final String format) {
        format(writer, c, format, null);
    }

    public static void format(final Writer writer, final XMLGregorianCalendar c, final String format, final TimeZone timeZone) {
        if ((format == null) && (timeZone == null)) {
            fastDateFormat(writer, c.toGregorianCalendar().getTimeInMillis(), false);
        } else {
            format(writer, createJUDate(c.toGregorianCalendar()), format, timeZone);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     * 
     * Sets the years field to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param date  the date, not null
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
     * @param date  the date, not null
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
     * @param date  the date, not null
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
     * @param date  the date, not null
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
     * @param date  the date, not null
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
     * @param date  the date, not null
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
     * @param date  the date, not null
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
     * @param date  the date, not null
     * @param calendarField  the {@code Calendar} field to set the amount to
     * @param amount the amount to set
     * @return a new {@code Date} set with the specified value
     * @throws IllegalArgumentException if the date is null
     * @since 2.4
     */
    private static <T extends java.util.Date> T set(final T date, final int calendarField, final int amount) {
        N.checkArgNotNull(date, "date");

        // getInstance() returns a new object, so this method is thread safe.
        final Calendar c = Calendar.getInstance();
        c.setLenient(false);
        c.setTime(date);
        c.set(calendarField, amount);

        return createDate(date.getClass(), c.getTimeInMillis());
    }

    /**
     * Adds or subtracts the specified amount of time to the given time unit,
     * based on the calendar's rules. For example, to subtract 5 days from the
     * current time of the calendar, you can achieve it by calling:
     * <p>
     * <code>N.roll(date, -5, TimeUnit.DAYS)</code>.
     *
     * @param date
     * @param amount
     * @param unit
     * @return a new instance of Date with the specified amount rolled.
     * @deprecated replaced by {@code addYears/addMonths/addWeeks/...}
     */
    @Deprecated
    public static <T extends java.util.Date> T roll(final T date, final long amount, final TimeUnit unit) {
        N.checkArgNotNull(date, "date");

        return createDate(date.getClass(), date.getTime() + unit.toMillis(amount));
    }

    /**
     * Adds or subtracts the specified amount of time to the given calendar
     * unit, based on the calendar's rules. For example, to subtract 5 days from
     * the current time of the calendar, you can achieve it by calling:
     * <p>
     * <code>N.roll(date, -5, CalendarUnit.DAY)</code>.
     *
     * @param date
     * @param amount
     * @param unit
     * @return a new instance of Date with the specified amount rolled.
     * @deprecated replaced by {@code addYears/addMonths/addWeeks/...}
     */
    @Deprecated
    public static <T extends java.util.Date> T roll(final T date, final long amount, final CalendarUnit unit) {
        N.checkArgNotNull(date, "date");

        if (amount > Integer.MAX_VALUE || amount < Integer.MIN_VALUE) {
            throw new IllegalArgumentException("The amount :" + amount + " is too big for unit: " + unit);
        }

        switch (unit) {
            case MONTH:
            case YEAR:
                final Calendar c = createCalendar(date);
                c.add(unit.intValue(), (int) amount);

                return createDate(date.getClass(), c.getTimeInMillis());

            default:
                return createDate(date.getClass(), date.getTime() + unit.toMillis(amount));
        }
    }

    /**
     * Adds or subtracts the specified amount of time to the given time unit,
     * based on the calendar's rules. For example, to subtract 5 days from the
     * current time of the calendar, you can achieve it by calling:
     * <p>
     * <code>N.roll(c, -5, TimeUnit.DAYS)</code>.
     *
     * @param calendar
     * @param amount
     * @param unit
     * @return a new instance of Calendar with the specified amount rolled.
     * @deprecated replaced by {@code addYears/addMonths/addWeeks/...}
     */
    @Deprecated
    public static <T extends Calendar> T roll(final T calendar, final long amount, final TimeUnit unit) {
        N.checkArgNotNull(calendar, "calendar");

        return createCalendar(calendar, calendar.getTimeInMillis() + unit.toMillis(amount));
    }

    /**
     * Adds or subtracts the specified amount of time to the given calendar
     * unit, based on the calendar's rules. For example, to subtract 5 days from
     * the current time of the calendar, you can achieve it by calling:
     * <p>
     * <code>N.roll(c, -5, CalendarUnit.DAY)</code>.
     *
     * @param calendar
     * @param amount
     * @param unit
     * @return a new instance of Calendar with the specified amount rolled.
     * @deprecated replaced by {@code addYears/addMonths/addWeeks/...}
     */
    @Deprecated
    public static <T extends Calendar> T roll(final T calendar, final long amount, final CalendarUnit unit) {
        N.checkArgNotNull(calendar, "calendar");

        if (amount > Integer.MAX_VALUE || amount < Integer.MIN_VALUE) {
            throw new IllegalArgumentException("The amount :" + amount + " is too big for unit: " + unit);
        }

        final T result = createCalendar(calendar, calendar.getTimeInMillis());

        result.add(unit.intValue(), (int) amount);

        return result;
    }

    //-----------------------------------------------------------------------

    //-----------------------------------------------------------------------
    /**
     * Adds a number of years to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param date  the date, not null
     * @param amount  the amount to add, may be negative
     * @return the new {@code Date} with the amount added
     * @throws IllegalArgumentException if the date is null
     */
    public static <T extends java.util.Date> T addYears(final T date, final int amount) {
        return roll(date, amount, CalendarUnit.YEAR);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of months to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param date  the date, not null
     * @param amount  the amount to add, may be negative
     * @return the new {@code Date} with the amount added
     * @throws IllegalArgumentException if the date is null
     */
    public static <T extends java.util.Date> T addMonths(final T date, final int amount) {
        return roll(date, amount, CalendarUnit.MONTH);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of weeks to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param date  the date, not null
     * @param amount  the amount to add, may be negative
     * @return the new {@code Date} with the amount added
     * @throws IllegalArgumentException if the date is null
     */
    public static <T extends java.util.Date> T addWeeks(final T date, final int amount) {
        return roll(date, amount, CalendarUnit.WEEK);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of days to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param date  the date, not null
     * @param amount  the amount to add, may be negative
     * @return the new {@code Date} with the amount added
     * @throws IllegalArgumentException if the date is null
     */
    public static <T extends java.util.Date> T addDays(final T date, final int amount) {
        return roll(date, amount, CalendarUnit.DAY);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of hours to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param date  the date, not null
     * @param amount  the amount to add, may be negative
     * @return the new {@code Date} with the amount added
     * @throws IllegalArgumentException if the date is null
     */
    public static <T extends java.util.Date> T addHours(final T date, final int amount) {
        return roll(date, amount, CalendarUnit.HOUR);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of minutes to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param date  the date, not null
     * @param amount  the amount to add, may be negative
     * @return the new {@code Date} with the amount added
     * @throws IllegalArgumentException if the date is null
     */
    public static <T extends java.util.Date> T addMinutes(final T date, final int amount) {
        return roll(date, amount, CalendarUnit.MINUTE);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of seconds to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param date  the date, not null
     * @param amount  the amount to add, may be negative
     * @return the new {@code Date} with the amount added
     * @throws IllegalArgumentException if the date is null
     */
    public static <T extends java.util.Date> T addSeconds(final T date, final int amount) {
        return roll(date, amount, CalendarUnit.SECOND);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of milliseconds to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param date  the date, not null
     * @param amount  the amount to add, may be negative
     * @return the new {@code Date} with the amount added
     * @throws IllegalArgumentException if the date is null
     */
    public static <T extends java.util.Date> T addMilliseconds(final T date, final int amount) {
        return roll(date, amount, CalendarUnit.MILLISECOND);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of years to a calendar returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param calendar  the calendar, not null
     * @param amount  the amount to add, may be negative
     * @return the new {@code Date} with the amount added
     * @throws IllegalArgumentException if the calendar is null
     */
    public static <T extends Calendar> T addYears(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarUnit.YEAR);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of months to a calendar returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param calendar  the calendar, not null
     * @param amount  the amount to add, may be negative
     * @return the new {@code Date} with the amount added
     * @throws IllegalArgumentException if the calendar is null
     */
    public static <T extends Calendar> T addMonths(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarUnit.MONTH);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of weeks to a calendar returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param calendar  the calendar, not null
     * @param amount  the amount to add, may be negative
     * @return the new {@code Date} with the amount added
     * @throws IllegalArgumentException if the calendar is null
     */
    public static <T extends Calendar> T addWeeks(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarUnit.WEEK);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of days to a calendar returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param calendar  the calendar, not null
     * @param amount  the amount to add, may be negative
     * @return the new {@code Date} with the amount added
     * @throws IllegalArgumentException if the calendar is null
     */
    public static <T extends Calendar> T addDays(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarUnit.DAY);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of hours to a calendar returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param calendar  the calendar, not null
     * @param amount  the amount to add, may be negative
     * @return the new {@code Date} with the amount added
     * @throws IllegalArgumentException if the calendar is null
     */
    public static <T extends Calendar> T addHours(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarUnit.HOUR);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of minutes to a calendar returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param calendar  the calendar, not null
     * @param amount  the amount to add, may be negative
     * @return the new {@code Date} with the amount added
     * @throws IllegalArgumentException if the calendar is null
     */
    public static <T extends Calendar> T addMinutes(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarUnit.MINUTE);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of seconds to a calendar returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param calendar  the calendar, not null
     * @param amount  the amount to add, may be negative
     * @return the new {@code Date} with the amount added
     * @throws IllegalArgumentException if the calendar is null
     */
    public static <T extends Calendar> T addSeconds(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarUnit.SECOND);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a number of milliseconds to a calendar returning a new object.
     * The original {@code Date} is unchanged.
     *
     * @param calendar  the calendar, not null
     * @param amount  the amount to add, may be negative
     * @return the new {@code Date} with the amount added
     * @throws IllegalArgumentException if the calendar is null
     */
    public static <T extends Calendar> T addMilliseconds(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarUnit.MILLISECOND);
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
     * @param date  the date to work with, not null
     * @param field  the field from {@code Calendar} or <code>SEMI_MONTH</code>
     * @return the different rounded date, not null
     * @throws ArithmeticException if the year is over 280 million
     */
    public static <T extends java.util.Date> T round(final T date, final int field) {
        N.checkArgNotNull(date, "date");

        final Calendar gval = Calendar.getInstance();
        gval.setTime(date);
        modify(gval, field, ModifyType.ROUND);

        return createDate(date.getClass(), gval.getTimeInMillis());
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
     * @param calendar  the date to work with, not null
     * @param field  the field from {@code Calendar} or <code>SEMI_MONTH</code>
     * @return the different rounded date, not null
     * @throws IllegalArgumentException if the date is <code>null</code>
     * @throws ArithmeticException if the year is over 280 million
     */
    public static <T extends Calendar> T round(final T calendar, final int field) {
        N.checkArgNotNull(calendar, "calendar");

        final Calendar rounded = (Calendar) calendar.clone();
        modify(rounded, field, ModifyType.ROUND);

        if (rounded.getClass().equals(calendar.getClass())) {
            return (T) rounded;
        }

        return createCalendar(calendar, rounded.getTimeInMillis());
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
     * @param date  the date to work with, not null
     * @param field  the field from {@code Calendar} or <code>SEMI_MONTH</code>
     * @return the different truncated date, not null
     * @throws IllegalArgumentException if the date is <code>null</code>
     * @throws ArithmeticException if the year is over 280 million
     */
    public static <T extends java.util.Date> T truncate(final T date, final int field) {
        N.checkArgNotNull(date, "date");

        final Calendar gval = Calendar.getInstance();
        gval.setTime(date);
        modify(gval, field, ModifyType.TRUNCATE);

        return createDate(date.getClass(), gval.getTimeInMillis());
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
     * @param calendar  the date to work with, not null
     * @param field  the field from {@code Calendar} or <code>SEMI_MONTH</code>
     * @return the different truncated date, not null
     * @throws IllegalArgumentException if the date is <code>null</code>
     * @throws ArithmeticException if the year is over 280 million
     */
    public static <T extends Calendar> T truncate(final T calendar, final int field) {
        N.checkArgNotNull(calendar, "calendar");

        final Calendar truncated = (Calendar) calendar.clone();
        modify(truncated, field, ModifyType.TRUNCATE);

        if (truncated.getClass().equals(calendar.getClass())) {
            return (T) truncated;
        }

        return createCalendar(calendar, truncated.getTimeInMillis());
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
     * @param date  the date to work with, not null
     * @param field  the field from {@code Calendar} or <code>SEMI_MONTH</code>
     * @return the different ceil date, not null
     * @throws IllegalArgumentException if the date is <code>null</code>
     * @throws ArithmeticException if the year is over 280 million
     * @since 2.5
     */
    public static <T extends java.util.Date> T ceiling(final T date, final int field) {
        N.checkArgNotNull(date, "date");

        final Calendar gval = Calendar.getInstance();
        gval.setTime(date);
        modify(gval, field, ModifyType.CEILING);

        return createDate(date.getClass(), gval.getTimeInMillis());
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
     * @param calendar  the date to work with, not null
     * @param field  the field from {@code Calendar} or <code>SEMI_MONTH</code>
     * @return the different ceil date, not null
     * @throws IllegalArgumentException if the date is <code>null</code>
     * @throws ArithmeticException if the year is over 280 million
     * @since 2.5
     */
    public static <T extends Calendar> T ceiling(final T calendar, final int field) {
        N.checkArgNotNull(calendar, "calendar");

        final Calendar ceiled = (Calendar) calendar.clone();

        modify(ceiled, field, ModifyType.CEILING);

        if (ceiled.getClass().equals(calendar.getClass())) {
            return (T) ceiled;
        }

        return createCalendar(calendar, ceiled.getTimeInMillis());
    }

    //-----------------------------------------------------------------------
    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     * 
     * <p>Internal calculation method.</p>
     *
     * @param val  the calendar, not null
     * @param unit  the field constant
     * @param modType  type to truncate, round or ceiling
     * @throws ArithmeticException if the year is over 280 million
     */
    private static void modify(final Calendar val, final int field, final ModifyType modType) {
        if (val.get(Calendar.YEAR) > 280000000) {
            throw new ArithmeticException("Calendar value too large for accurate calculations");
        }

        if (field == Calendar.MILLISECOND) {
            return;
        }

        // ----------------- Fix for LANG-59 ---------------------- START ---------------
        // see http://issues.apache.org/jira/browse/LANG-59
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
                        //We need to subtrace 1 since the date has a minimum of 1
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
     * @see #truncate(Date, int)
     * @see #truncatedEquals(Calendar, Calendar, int)
     * @since 3.0
     */
    public static boolean truncatedEquals(final java.util.Date date1, final java.util.Date date2, final int field) {
        return truncatedCompareTo(date1, date2, field) == 0;
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

    private static DateFormat getSDF(final String format, final TimeZone timeZone) {
        DateFormat sdf = null;

        if (timeZone == UTC_TIME_ZONE) {
            if ((format.length() == 28) && (format == ISO_8601_TIMESTAMP_FORMAT)) {
                sdf = utcTimestampDFPool.poll();

                if (sdf == null) {
                    sdf = new SimpleDateFormat(format);
                    sdf.setTimeZone(timeZone);
                }

                return sdf;
            } else if ((format.length() == 24) && (format == ISO_8601_DATETIME_FORMAT)) {
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
            queue = new ArrayBlockingQueue<>(Internals.POOL_SIZE);
            dfPool.put(format, queue);
        }

        sdf = queue.poll();

        if (sdf == null) {
            sdf = new SimpleDateFormat(format);
        }

        sdf.setTimeZone(timeZone);

        return sdf;
    }

    private static void recycleSDF(final String format, final TimeZone timeZone, final DateFormat sdf) {
        if (timeZone == UTC_TIME_ZONE) {
            if ((format.length() == 28) && (format == ISO_8601_TIMESTAMP_FORMAT)) {
                utcTimestampDFPool.add(sdf);
            } else if ((format.length() == 24) && (format == ISO_8601_DATETIME_FORMAT)) {
                utcDateTimeDFPool.add(sdf);
            } else {
                dfPool.get(format).add(sdf);
            }
        } else {
            dfPool.get(format).add(sdf);
        }
    }

    private static String checkDateFormat(final String str, final String format) {
        if (N.isNullOrEmpty(format)) {
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
                    // throw new AbacusException("No valid date format found for: " + str);
                    return null;
            }
        }

        return format;
    }

    private static TimeZone checkTimeZone(final String format, TimeZone timeZone) {
        if (timeZone == null) {
            timeZone = format.endsWith("'Z'") ? UTC_TIME_ZONE : LOCAL_TIME_ZONE;
        }

        return timeZone;
    }

    private static long fastDateParse(final String str, final String format, final TimeZone timeZone) {
        if (!((str.length() == 24) || (str.length() == 20) || (str.length() == 19) || (str.length() == 23))) {
            return 0;
        }

        if (!(format.equals(ISO_8601_TIMESTAMP_FORMAT) || format.equals(ISO_8601_DATETIME_FORMAT) || format.equals(LOCAL_DATETIME_FORMAT)
                || format.equals(LOCAL_TIMESTAMP_FORMAT))) {
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
                timeZoneCalendarQueue = new ArrayBlockingQueue<>(Internals.POOL_SIZE);
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

    private static int parseInt(final String str, int from, final int to) {
        int result = 0;

        while (from < to) {
            result = (result * 10) + (str.charAt(from++) - 48);
        }

        return result;
    }

    private static <T extends java.util.Date> T createDate(final Class<? extends java.util.Date> cls, final long millis) {
        java.util.Date result = null;

        if (cls.equals(java.util.Date.class)) {
            result = new java.util.Date(millis);
        } else if (cls.equals(java.sql.Date.class)) {
            result = new java.sql.Date(millis);
        } else if (cls.equals(Time.class)) {
            result = new Time(millis);
        } else if (cls.equals(Timestamp.class)) {
            result = new Timestamp(millis);
        } else {
            result = ClassUtil.invokeConstructor(ClassUtil.getDeclaredConstructor(cls, long.class), millis);
        }

        return (T) result;
    }

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

    private static String formatDate(final Writer writer, final java.util.Date date, String format, TimeZone timeZone) {
        boolean isTimestamp = date instanceof Timestamp;

        if ((format == null) && (timeZone == null)) {
            if (writer == null) {
                final BufferedWriter bw = Objectory.createBufferedWriter();

                fastDateFormat(bw, date.getTime(), isTimestamp);

                String str = bw.toString();

                Objectory.recycle(bw);

                return str;
            } else {
                fastDateFormat(writer, date.getTime(), isTimestamp);

                return null;
            }
        }

        if (format == null) {
            format = isTimestamp ? ISO_8601_TIMESTAMP_FORMAT : ISO_8601_DATETIME_FORMAT;
        }

        timeZone = checkTimeZone(format, timeZone);

        DateFormat sdf = getSDF(format, timeZone);

        String str = sdf.format(date);

        if (writer != null) {
            try {
                writer.write(str);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        recycleSDF(format, timeZone, sdf);

        return str;
    }

    private static void fastDateFormat(final Writer writer, final long timeInMillis, final boolean isTimestamp) {
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
                writer.write(utcTimestamp);
            } else {
                writer.write(utcTimestamp, 0, 20);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            utcCalendarPool.add(c);
            utcTimestampFormatCharsPool.add(utcTimestamp);
        }
    }

    /**
     * Calendar modification types.
     */
    private static enum ModifyType {
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

    public static class DateTimeUtil extends DateUtil {
        private DateTimeUtil() {
            // singleton.
        }
    }
}
