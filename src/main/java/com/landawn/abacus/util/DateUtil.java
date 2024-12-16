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
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
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
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

/**
 * <p>
 * Note: This class includes codes copied from Apache Commons Lang, Google Guava and other open source projects under the Apache License 2.0.
 * The methods copied from other libraries/frameworks/projects may be modified in this class.
 * </p>
 * @see DateTimeFormatter
 * @see ISO8601Util
 */
@SuppressWarnings({ "java:S1694", "java:S2143" })
public abstract sealed class DateUtil permits DateUtil.DateTimeUtil, DateUtil.Dates {
    private static final Logger logger = LoggerFactory.getLogger(DateUtil.class);

    private static final String FAILED_TO_PARSE_TO_LONG = "Failed to parse: {} to Long";

    // ...

    /**
     * Default {@code TimeZone} of the Java virtual machine
     */
    public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault();

    /**
     * UTC, or Coordinated Universal Time, is the time standard that the world uses to regulate clocks and time.
     * It does not change with the seasons (i.e., it doesn't observe Daylight Saving Time) and is the same everywhere.
     * It's often used as a reference point for time zones around the world.
     */
    public static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    /**
     * GMT, or Greenwich Mean Time, is a time zone that is used as a reference point for time keeping around the world.
     * It is located at the prime meridian (0 degrees longitude) and does not observe daylight saving time.
     * GMT is often used in various contexts, including aviation, computing, and international communications.
     * In many regions, GMT is replaced by Coordinated Universal Time (UTC), which is similar but more precise.
     * When comparing other time zones, you can express them as offsets from GMT/UTC, such as GMT+2 or GMT-5
     */
    public static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");

    /**
     * Default {@code ZoneId} of the Java virtual machine
     */
    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();

    /**
     * {@code ZionId} of UTC time zone.
     * @see #UTC_TIME_ZONE
     * @see TimeZone#toZoneId()
     */
    public static final ZoneId UTC_ZONE_ID = UTC_TIME_ZONE.toZoneId();

    /**
     * {@code ZionId} of GMT time zone.
     * @see #GMT_TIME_ZONE
     * @see TimeZone#toZoneId()
     */
    public static final ZoneId GMT_ZONE_ID = GMT_TIME_ZONE.toZoneId();

    /**
     * Date/Time format: {@code yyyy}
     */
    public static final String LOCAL_YEAR_FORMAT = "yyyy";

    /**
     * Date/Time format: {@code MM-dd}
     */
    public static final String LOCAL_MONTH_DAY_FORMAT = "MM-dd";

    // static final String LOCAL_MONTH_DAY_FORMAT_SLASH = "MM/dd";

    /**
     * Date/Time format: {@code yyyy-MM-dd}
     */
    public static final String LOCAL_DATE_FORMAT = "yyyy-MM-dd";

    // static final String LOCAL_DATE_FORMAT_SLASH = "yyyy/MM/dd";

    /**
     * Date/Time format: {@code HH:mm:ss}
     */
    public static final String LOCAL_TIME_FORMAT = "HH:mm:ss";

    /**
     * Date/Time format: {@code yyyy-MM-dd HH:mm:ss}
     */
    public static final String LOCAL_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // static final String LOCAL_DATE_TIME_FORMAT_SLASH = "yyyy/MM/dd HH:mm:ss";

    //    /**
    //     * Date/Time format: {@code yyyy-MM-dd HH:mm:ss.SSS}
    //     * @deprecated use {@link #ISO_8601_TIMESTAMP_FORMAT}
    //     */
    //    @Deprecated
    //    public static final String LOCAL_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    // static final String LOCAL_TIMESTAMP_FORMAT_SLASH = "yyyy/MM/dd HH:mm:ss.SSS";

    /**
     * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ss}
     * @see {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}
     */
    public static final String ISO_LOCAL_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ssXXX}
     * @see {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}
     */
    public static final String ISO_OFFSET_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

    /**
     * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ss'Z'}.
     * It's default date/time format.
     */
    public static final String ISO_8601_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    // static final String ISO_8601_DATE_TIME_FORMAT_SLASH = "yyyy/MM/dd'T'HH:mm:ss'Z'";

    /**
     * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}.
     *
     * It's default timestamp format.
     */
    public static final String ISO_8601_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    // static final String ISO_8601_TIMESTAMP_FORMAT_SLASH = "yyyy/MM/dd'T'HH:mm:ss.SSS'Z'";

    /**
     * This constant defines the date format specified by
     * RFC 1123 / RFC 822. Used for parsing via <i>SimpleDateFormat</i> as well as
     * error messages.
     */
    public static final String RFC_1123_DATE_TIME_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

    /**
     * This is half a month, so this represents whether a date is in the top or bottom half of the month.
     * @see CalendarField
     */
    public static final int SEMI_MONTH = 1001;

    private static final int[][] fields = { { Calendar.MILLISECOND }, { Calendar.SECOND }, { Calendar.MINUTE }, { Calendar.HOUR_OF_DAY, Calendar.HOUR },
            { Calendar.DATE, Calendar.DAY_OF_MONTH, Calendar.AM_PM
            /* Calendar.DAY_OF_YEAR, Calendar.DAY_OF_WEEK, Calendar.DAY_OF_WEEK_IN_MONTH */ }, { Calendar.MONTH, SEMI_MONTH }, { Calendar.YEAR },
            { Calendar.ERA } };

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
        } catch (final Exception e) {
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
     * Returns the current time in milliseconds.
     *
     * @return The current time in milliseconds since the epoch (01-01-1970).
     */
    @Beta
    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Returns a new instance of {@code java.sql.Time} based on the current time in the default time zone with the default locale.
     *
     * @return
     */
    public static Time currentTime() {
        return new Time(System.currentTimeMillis());
    }

    /**
     * Returns a new instance of {@code java.sql.Date} based on the current time in the default time zone with the default locale.
     *
     * @return
     */
    public static Date currentDate() {
        return new Date(System.currentTimeMillis());
    }

    /**
     * Returns a new instance of {@code java.sql.Timestamp} based on the current time in the default time zone with the default locale.
     *
     * @return
     */
    public static Timestamp currentTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    /**
     * Returns a new instance of {@code java.util.Date} based on the current time in the default time zone with the default locale.
     *
     * @return
     */
    public static java.util.Date currentJUDate() {
        return new java.util.Date();
    }

    /**
     * Returns a new instance of {@code java.util.Calendar} based on the current time in the default time zone with the default locale.
     *
     * @return a Calendar.
     */
    public static Calendar currentCalendar() {
        return Calendar.getInstance();
    }

    /**
     * Returns a new instance of {@code GregorianCalendar} based on the current time in the default time zone with the default locale.
     *
     * @return A new GregorianCalendar object representing the current date and time.
     */
    public static GregorianCalendar currentGregorianCalendar() {
        return new GregorianCalendar();
    }

    /**
     * Returns a new instance of {@code XMLGregorianCalendar} instance based on the current Gregorian Calendar.
     *
     * @return XMLGregorianCalendar instance representing the current date and time.
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
     * Creates a new instance of {@code java.util.Date} based on the time value represented by the {@code Calendar} object.
     *
     * @param calendar The {@code Calendar} object used to create the Date object.
     * @return A new Date object representing the same point in time as the provided {@code Calendar} object.
     * @throws IllegalArgumentException if the provided {@code Calendar} object is {@code null}.
     */
    public static java.util.Date createJUDate(final Calendar calendar) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createJUDate(calendar.getTimeInMillis());
    }

    /**
     * Creates a new instance of {@code java.util.Date} based on the time value represented by the Date object.
     *
     * @param date The Date object used to create the new Date object.
     * @return A new Date object representing the same point in time as the provided Date object.
     * @throws IllegalArgumentException if the provided date object is {@code null}.
     */
    public static java.util.Date createJUDate(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return createJUDate(date.getTime());
    }

    /**
     * Creates a new instance of {@code java.util.Date} based on the provided time in milliseconds.
     *
     * @param timeInMillis The time in milliseconds since the epoch (01-01-1970).
     * @return A new Date object representing the same point in time as the provided time in milliseconds.
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
     * Creates a new instance of {@code java.sql.Date} based on the time value represented by the {@code Calendar} object.
     *
     * @param calendar The {@code Calendar} object used to create the Date object.
     * @return A new Date object representing the same point in time as the provided {@code Calendar} object.
     * @throws IllegalArgumentException if the provided {@code Calendar} object is {@code null}.
     */
    public static Date createDate(final Calendar calendar) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createDate(calendar.getTimeInMillis());
    }

    /**
     * Creates a new instance of {@code java.sql.Date} based on the time value represented by the {@code java.util.Date} object.
     *
     * @param date The {@code java.util.Date} object used to create the new java.sql.Date object.
     * @return A new java.sql.Date object representing the same point in time as the provided {@code java.util.Date} object.
     * @throws IllegalArgumentException if the provided {@code java.util.Date} object is {@code null}.
     */
    public static Date createDate(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return createDate(date.getTime());
    }

    /**
     * Creates a new instance of {@code java.sql.Date} based on the provided time in milliseconds.
     *
     * @param timeInMillis The time in milliseconds since the epoch (01-01-1970).
     * @return A new Date object representing the same point in time as the provided time in milliseconds.
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
     * Creates a new instance of java.sql.Time based on the time value represented by the {@code Calendar} object.
     *
     * @param calendar The {@code Calendar} object used to create the Time object.
     * @return A new Time object representing the same point in time as the provided {@code Calendar} object.
     * @throws IllegalArgumentException if the provided {@code Calendar} object is {@code null}.
     */
    public static Time createdTime(final Calendar calendar) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createdTime(calendar.getTimeInMillis());
    }

    /**
     * Creates a new instance of java.sql.Time based on the time value represented by the {@code java.util.Date} object.
     *
     * @param date The {@code java.util.Date} object used to create the new java.sql.Time object.
     * @return A new java.sql.Time object representing the same point in time as the provided {@code java.util.Date} object.
     * @throws IllegalArgumentException if the provided {@code java.util.Date} object is {@code null}.
     */
    public static Time createdTime(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return createdTime(date.getTime());
    }

    /**
     * Creates a new instance of java.sql.Time based on the provided time in milliseconds.
     *
     * @param timeInMillis The time in milliseconds since the epoch (01-01-1970).
     * @return A new Time object representing the same point in time as the provided time in milliseconds.
     */
    public static Time createdTime(final long timeInMillis) {
        //    N.checkArgPositive(timeInMillis, "timeInMillis");
        //
        //    if (timeInMillis == 0) {
        //        return null;
        //    }

        return new Time(timeInMillis);
    }

    /**
     * Creates a new instance of java.sql.Timestamp based on the time value represented by the {@code Calendar} object.
     *
     * @param calendar The {@code Calendar} object used to create the Timestamp object.
     * @return A new Timestamp object representing the same point in time as the provided {@code Calendar} object.
     * @throws IllegalArgumentException if the provided {@code Calendar} object is {@code null}.
     */
    public static Timestamp createdTimestamp(final Calendar calendar) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createdTimestamp(calendar.getTimeInMillis());
    }

    /**
     * Creates a new instance of java.sql.Timestamp based on the time value represented by the {@code java.util.Date} object.
     *
     * @param date The {@code java.util.Date} object used to create the new java.sql.Timestamp object.
     * @return A new java.sql.Timestamp object representing the same point in time as the provided {@code java.util.Date} object.
     * @throws IllegalArgumentException if the provided {@code java.util.Date} object is {@code null}.
     */
    public static Timestamp createdTimestamp(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return createdTimestamp(date.getTime());
    }

    /**
     * Creates a new instance of java.sql.Timestamp based on the provided time in milliseconds.
     *
     * @param timeInMillis The time in milliseconds since the epoch (01-01-1970).
     * @return A new Timestamp object representing the same point in time as the provided time in milliseconds.
     */
    public static Timestamp createdTimestamp(final long timeInMillis) {
        //    N.checkArgPositive(timeInMillis, "timeInMillis");
        //
        //    if (timeInMillis == 0) {
        //        return null;
        //    }

        return new Timestamp(timeInMillis);
    }

    /**
     * Creates a new instance of {@code java.util.Calendar} based on the time value represented by the provided {@code Calendar} object.
     *
     * @param calendar The {@code Calendar} object used to create the new {@code Calendar} object.
     * @return A new {@code Calendar} object representing the same point in time as the provided {@code Calendar} object.
     * @throws IllegalArgumentException if the provided {@code Calendar} object is {@code null}.
     */
    public static Calendar createCalendar(final Calendar calendar) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createCalendar(calendar.getTimeInMillis());
    }

    /**
     * Creates a new instance of {@code java.util.Calendar} based on the time value represented by the {@code java.util.Date} object.
     *
     * @param date The {@code java.util.Date} object used to create the new java.util.Calendar object.
     * @return A new java.util.Calendar object representing the same point in time as the provided {@code java.util.Date} object.
     * @throws IllegalArgumentException if the provided {@code java.util.Date} object is {@code null}.
     */
    public static Calendar createCalendar(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return createCalendar(date.getTime());
    }

    /**
     * Creates a new instance of {@code java.util.Calendar} based on the provided time in milliseconds.
     *
     * @param timeInMillis The time in milliseconds since the epoch (01-01-1970).
     * @return A new java.util.Calendar object representing the same point in time as the provided time in milliseconds.
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
     * Creates a new instance of {@code java.util.Calendar} based on the provided time in milliseconds and the specified TimeZone.
     *
     * @param timeInMillis The time in milliseconds since the epoch (01-01-1970).
     * @param tz The TimeZone to be used for the new {@code Calendar} object.
     * @return A new java.util.Calendar object representing the same point in time as the provided time in milliseconds, using the specified TimeZone.
     * @throws IllegalArgumentException if the provided TimeZone object is {@code null}.
     */
    public static Calendar createCalendar(final long timeInMillis, final TimeZone tz) throws IllegalArgumentException {
        //    N.checkArgPositive(timeInMillis, "timeInMillis");
        //
        //    if (timeInMillis == 0) {
        //        return null;
        //    }

        final Calendar c = tz == null ? Calendar.getInstance() : Calendar.getInstance(tz);

        c.setTimeInMillis(timeInMillis);

        return c;
    }

    /**
     * Creates a new instance of {@code java.util.GregorianCalendar} based on the time value represented by the provided {@code Calendar} object.
     *
     * @param calendar The {@code Calendar} object used to create the new GregorianCalendar object.
     * @return A new GregorianCalendar object representing the same point in time as the provided {@code Calendar} object.
     * @throws IllegalArgumentException if the provided {@code Calendar} object is {@code null}.
     */
    public static GregorianCalendar createGregorianCalendar(final Calendar calendar) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createGregorianCalendar(calendar.getTimeInMillis());
    }

    /**
     * Creates a new instance of {@code java.util.GregorianCalendar} based on the time value represented by the provided {@code java.util.Date} object.
     *
     * @param date The {@code java.util.Date} object used to create the new GregorianCalendar object.
     * @return A new GregorianCalendar object representing the same point in time as the provided {@code java.util.Date} object.
     * @throws IllegalArgumentException if the provided {@code java.util.Date} object is {@code null}.
     */
    public static GregorianCalendar createGregorianCalendar(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return createGregorianCalendar(date.getTime());
    }

    /**
     * Creates a new instance of {@code java.util.GregorianCalendar} based on the provided time in milliseconds.
     *
     * @param timeInMillis The time in milliseconds since the epoch (01-01-1970).
     * @return A new java.util.GregorianCalendar object representing the same point in time as the provided time in milliseconds.
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
     * Creates a new instance of {@code java.util.GregorianCalendar} based on the provided time in milliseconds and the specified TimeZone.
     *
     * @param timeInMillis The time in milliseconds since the epoch (01-01-1970).
     * @param tz The TimeZone to be used for the new GregorianCalendar object.
     * @return A new java.util.GregorianCalendar object representing the same point in time as the provided time in milliseconds, using the specified TimeZone.
     */
    public static GregorianCalendar createGregorianCalendar(final long timeInMillis, final TimeZone tz) {
        //    N.checkArgPositive(timeInMillis, "timeInMillis");
        //
        //    if (timeInMillis == 0) {
        //        return null;
        //    }

        final GregorianCalendar c = tz == null ? new GregorianCalendar() : new GregorianCalendar(tz);

        c.setTimeInMillis(timeInMillis);

        return c;
    }

    /**
     * Creates a new instance of {@code XMLGregorianCalendar} based on the time value represented by the provided {@code Calendar} object.
     *
     * @param calendar The {@code Calendar} object used to create the XMLGregorianCalendar object.
     * @return A new XMLGregorianCalendar object representing the same point in time as the provided {@code Calendar} object.
     * @throws IllegalArgumentException if the provided {@code Calendar} object is {@code null}.
     */
    public static XMLGregorianCalendar createXMLGregorianCalendar(final Calendar calendar) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createXMLGregorianCalendar(calendar.getTimeInMillis());
    }

    /**
     * Creates a new instance of {@code XMLGregorianCalendar} based on the time value represented by the provided {@code java.util.Date} object.
     *
     * @param date The {@code java.util.Date} object used to create the new XMLGregorianCalendar object.
     * @return A new XMLGregorianCalendar object representing the same point in time as the provided {@code java.util.Date} object.
     * @throws IllegalArgumentException if the provided {@code java.util.Date} object is {@code null}.
     */
    public static XMLGregorianCalendar createXMLGregorianCalendar(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return createXMLGregorianCalendar(date.getTime());
    }

    /**
     * Creates a new instance of {@code XMLGregorianCalendar} based on the provided time in milliseconds.
     *
     * @param timeInMillis The time in milliseconds since the epoch (01-01-1970).
     * @return A new XMLGregorianCalendar object representing the same point in time as the provided time in milliseconds.
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
     * Creates a new instance of {@code XMLGregorianCalendar} based on the provided time in milliseconds and the specified TimeZone.
     *
     * @param timeInMillis The time in milliseconds since the epoch (01-01-1970).
     * @param tz The TimeZone to be used for the new XMLGregorianCalendar object.
     * @return A new XMLGregorianCalendar object representing the same point in time as the provided time in milliseconds, using the specified TimeZone.
     */
    public static XMLGregorianCalendar createXMLGregorianCalendar(final long timeInMillis, final TimeZone tz) {
        //    N.checkArgPositive(timeInMillis, "timeInMillis");
        //
        //    if (timeInMillis == 0) {
        //        return null;
        //    }

        return dataTypeFactory.newXMLGregorianCalendar(createGregorianCalendar(timeInMillis, tz));
    }

    /**
     * Parses a string representation of a date into a {@code java.util.Date} object.
     *
     * @param date The string representation of the date to be parsed.
     * @return The parsed {@code java.util.Date} object or {@code null} if the provided date string is {@code null} or empty or "null".
     */
    public static java.util.Date parseJUDate(final String date) {
        return parseJUDate(date, null);
    }

    /**
     * Parses a string representation of a date into a {@code java.util.Date} object.
     *
     * @param date The string representation of the date to be parsed.
     * @param format The format of the date string. If {@code null}, the method will try to parse the date using common date formats.
     * @return The parsed {@code java.util.Date} object or {@code null} if the provided date string is {@code null} or empty or "null".
     * @throws IllegalArgumentException if the provided date string cannot be parsed.
     */
    public static java.util.Date parseJUDate(final String date, final String format) {
        return parseJUDate(date, format, null);
    }

    /**
     * Parses a string representation of a date into a {@code java.util.Date} object.
     *
     * @param date The string representation of the date to be parsed.
     * @param format The format of the date string. If {@code null}, the method will try to parse the date using common date formats.
     * @param timeZone The TimeZone to be used for parsing the date string. If {@code null}, the method will use the default TimeZone.
     * @return The parsed {@code java.util.Date} object or {@code null} if the provided date string is {@code null} or empty or "null".
     * @throws IllegalArgumentException if the provided date string cannot be parsed.
     */
    @MayReturnNull
    public static java.util.Date parseJUDate(final String date, final String format, final TimeZone timeZone) {
        if (isNullDateTime(date)) {
            return null;
        }

        // return createJUDate(parse(date, format, timeZone));

        if ((format == null) && isPossibleLong(date)) {
            try {
                return createJUDate(Long.parseLong(date));
            } catch (final NumberFormatException e) {
                // ignore.
                if (logger.isWarnEnabled()) {
                    logger.warn(FAILED_TO_PARSE_TO_LONG, date);
                }
            }
        }

        final String formatToUse = checkDateFormat(date, format);

        // use ISO8601Util.parse for better performance.
        if (Strings.isEmpty(formatToUse) || ISO_8601_DATE_TIME_FORMAT.equals(formatToUse) || ISO_8601_TIMESTAMP_FORMAT.equals(formatToUse)) {
            if (timeZone == null || timeZone.equals(ISO8601Util.TIMEZONE_Z)) {
                return ISO8601Util.parse(date);
            } else {
                throw new RuntimeException("Unsupported date format: " + formatToUse + " with time zone: " + timeZone);
            }
        }

        final TimeZone timeZoneToUse = checkTimeZone(date, formatToUse, timeZone);

        final long timeInMillis = fastDateParse(date, formatToUse, timeZoneToUse);

        if (timeInMillis != 0) {
            return createJUDate(timeInMillis);
        }

        final DateFormat sdf = getSDF(formatToUse, timeZoneToUse);

        try {
            return sdf.parse(date);
        } catch (final ParseException e) {
            throw new IllegalArgumentException(e);
        } finally {
            recycleSDF(formatToUse, timeZoneToUse, sdf);
        }
    }

    /**
     * Parses a string representation of a date into a java.sql.Date object.
     *
     * @param date The string representation of the date to be parsed.
     * @return The parsed java.sql.Date object or {@code null} if the provided date string is {@code null} or empty or "null".
     */
    public static Date parseDate(final String date) {
        return parseDate(date, null);
    }

    /**
     * Parses a string representation of a date into a java.sql.Date object.
     *
     * @param date The string representation of the date to be parsed.
     * @param format The format of the date string. If {@code null}, the method will try to parse the date using common date formats.
     * @return The parsed java.sql.Date object or {@code null} if the provided date string is {@code null} or empty or "null".
     * @throws IllegalArgumentException if the provided date string cannot be parsed.
     */
    public static Date parseDate(final String date, final String format) {
        return parseDate(date, format, null);
    }

    /**
     * Parses a string representation of a date into a java.sql.Date object.
     *
     * @param date The string representation of the date to be parsed.
     * @param format The format of the date string. If {@code null}, the method will try to parse the date using common date formats.
     * @param timeZone The TimeZone to be used for parsing the date string. If {@code null}, the method will use the default TimeZone.
     * @return The parsed java.sql.Date object or {@code null} if the provided date string is {@code null} or empty or "null".
     * @throws IllegalArgumentException if the provided date string cannot be parsed.
     */
    @MayReturnNull
    public static Date parseDate(final String date, final String format, final TimeZone timeZone) {
        if (isNullDateTime(date)) {
            return null;
        }

        return createDate(parse(date, format, timeZone));
    }

    /**
     * Parses a string representation of a time into a java.sql.Time object.
     *
     * @param date The string representation of the time to be parsed.
     * @return The parsed java.sql.Time object or {@code null} if the provided date string is {@code null} or empty or "null".
     */
    public static Time parseTime(final String date) {
        return parseTime(date, null);
    }

    /**
     * Parses a string representation of a time into a java.sql.Time object.
     *
     * @param date The string representation of the time to be parsed.
     * @param format The format of the time string. If {@code null}, the method will try to parse the time using common time formats.
     * @return The parsed java.sql.Time object or {@code null} if the provided date string is {@code null} or empty or "null".
     * @throws IllegalArgumentException if the provided time string cannot be parsed.
     */
    public static Time parseTime(final String date, final String format) {
        return parseTime(date, format, null);
    }

    /**
     * Parses a string representation of a time into a java.sql.Time object.
     *
     * @param date The string representation of the time to be parsed.
     * @param format The format of the time string. If {@code null}, the method will try to parse the time using common time formats.
     * @param timeZone The TimeZone to be used for parsing the time string. If {@code null}, the method will use the default TimeZone.
     * @return The parsed java.sql.Time object or {@code null} if the provided date string is {@code null} or empty or "null".
     * @throws IllegalArgumentException if the provided time string cannot be parsed.
     */
    @MayReturnNull
    public static Time parseTime(final String date, final String format, final TimeZone timeZone) {
        if (isNullDateTime(date)) {
            return null;
        }

        return createdTime(parse(date, format, timeZone));
    }

    /**
     * Parses a string representation of a timestamp into a java.sql.Timestamp object.
     *
     * @param date The string representation of the timestamp to be parsed.
     * @return The parsed java.sql.Timestamp object or {@code null} if the provided timestamp string is {@code null} or empty or "null".
     * @throws IllegalArgumentException if the provided timestamp string cannot be parsed.
     */
    public static Timestamp parseTimestamp(final String date) {
        return parseTimestamp(date, null);
    }

    /**
     * Parses a string representation of a timestamp into a java.sql.Timestamp object.
     *
     * @param date The string representation of the timestamp to be parsed.
     * @param format The format of the timestamp string. If {@code null}, the method will try to parse the timestamp using common timestamp formats.
     * @return The parsed java.sql.Timestamp object or {@code null} if the provided timestamp string is {@code null} or empty or "null".
     * @throws IllegalArgumentException if the provided timestamp string cannot be parsed.
     */
    public static Timestamp parseTimestamp(final String date, final String format) {
        return parseTimestamp(date, format, null);
    }

    /**
     * Parses a string representation of a timestamp into a java.sql.Timestamp object.
     *
     * @param date The string representation of the timestamp to be parsed.
     * @param format The format of the timestamp string. If {@code null}, the method will try to parse the timestamp using common timestamp formats.
     * @param timeZone The TimeZone to be used for parsing the timestamp string. If {@code null}, the method will use the default TimeZone.
     * @return The parsed java.sql.Timestamp object or {@code null} if the provided timestamp string is {@code null} or empty or "null".
     * @throws IllegalArgumentException if the provided timestamp string cannot be parsed.
     */
    @MayReturnNull
    public static Timestamp parseTimestamp(final String date, final String format, final TimeZone timeZone) {
        if (isNullDateTime(date)) {
            return null;
        }

        return createdTimestamp(parse(date, format, timeZone));
    }

    /**
     * Parses a string representation of a calendar into a java.util.Calendar object.
     *
     * @param calendar The string representation of the calendar to be parsed.
     * @return The parsed java.util.Calendar object or {@code null} if the provided calendar string is {@code null} or empty or "null".
     */
    @Beta
    public static Calendar parseCalendar(final String calendar) {
        return parseCalendar(calendar, null);
    }

    /**
     * Parses a string representation of a calendar into a java.util.Calendar object.
     *
     * @param calendar The string representation of the calendar to be parsed.
     * @param format The format of the calendar string. If {@code null}, the method will try to parse the calendar using common calendar formats.
     * @return The parsed java.util.Calendar object or {@code null} if the provided calendar string is {@code null} or empty or "null".
     */
    @Beta
    public static Calendar parseCalendar(final String calendar, final String format) {
        return parseCalendar(calendar, format, null);
    }

    /**
     * Parses a string representation of a calendar into a java.util.Calendar object.
     *
     * @param calendar The string representation of the calendar to be parsed.
     * @param format The format of the calendar string. If {@code null}, the method will try to parse the calendar using common calendar formats.
     * @param timeZone The TimeZone to be used for parsing the calendar string. If {@code null}, the method will use the default TimeZone.
     * @return The parsed java.util.Calendar object or {@code null} if the provided calendar string is {@code null} or empty or "null".
     */
    @MayReturnNull
    @Beta
    public static Calendar parseCalendar(final String calendar, final String format, final TimeZone timeZone) {
        if (isNullDateTime(calendar)) {
            return null;
        }

        return createCalendar(parse(calendar, format, timeZone), timeZone);
    }

    /**
     * Parses a string representation of a Gregorian calendar into a java.util.GregorianCalendar object.
     *
     * @param calendar The string representation of the Gregorian calendar to be parsed.
     * @return The parsed java.util.GregorianCalendar object or {@code null} if the provided calendar string is {@code null} or empty or "null".
     */
    @Beta
    public static GregorianCalendar parseGregorianCalendar(final String calendar) {
        return parseGregorianCalendar(calendar, null);
    }

    /**
     * Parses a string representation of a Gregorian calendar into a java.util.GregorianCalendar object.
     *
     * @param calendar The string representation of the Gregorian calendar to be parsed.
     * @param format The format of the calendar string. If {@code null}, the method will try to parse the calendar using common calendar formats.
     * @return The parsed java.util.GregorianCalendar object or {@code null} if the provided calendar string is {@code null} or empty or "null".
     */
    @Beta
    public static GregorianCalendar parseGregorianCalendar(final String calendar, final String format) {
        return parseGregorianCalendar(calendar, format, null);
    }

    /**
     * Parses a string representation of a Gregorian calendar into a java.util.GregorianCalendar object.
     *
     * @param calendar The string representation of the Gregorian calendar to be parsed.
     * @param format The format of the calendar string. If {@code null}, the method will try to parse the calendar using common calendar formats.
     * @param timeZone The TimeZone to be used for parsing the calendar string. If {@code null}, the method will use the default TimeZone.
     * @return The parsed java.util.GregorianCalendar object or {@code null} if the provided calendar string is {@code null} or empty or "null".
     */
    @MayReturnNull
    @Beta
    public static GregorianCalendar parseGregorianCalendar(final String calendar, final String format, final TimeZone timeZone) {
        if (isNullDateTime(calendar)) {
            return null;
        }

        return createGregorianCalendar(parse(calendar, format, timeZone), timeZone);
    }

    /**
     * Parses a string representation of an XML Gregorian calendar into a javax.xml.datatype.XMLGregorianCalendar object.
     *
     * @param calendar The string representation of the XML Gregorian calendar to be parsed.
     * @return The parsed javax.xml.datatype.XMLGregorianCalendar object or {@code null} if the provided calendar string is {@code null} or empty or "null".
     */
    @Beta
    public static XMLGregorianCalendar parseXMLGregorianCalendar(final String calendar) {
        return parseXMLGregorianCalendar(calendar, null);
    }

    /**
     * Parses a string representation of an XML Gregorian calendar into a javax.xml.datatype.XMLGregorianCalendar object.
     *
     * @param calendar The string representation of the XML Gregorian calendar to be parsed.
     * @param format The format of the calendar string. If {@code null}, the method will try to parse the calendar using common calendar formats.
     * @return The parsed javax.xml.datatype.XMLGregorianCalendar object or {@code null} if the provided calendar string is {@code null} or empty or "null".
     */
    @Beta
    public static XMLGregorianCalendar parseXMLGregorianCalendar(final String calendar, final String format) {
        return parseXMLGregorianCalendar(calendar, format, null);
    }

    /**
     * Parses a string representation of an XML Gregorian calendar into a javax.xml.datatype.XMLGregorianCalendar object.
     *
     * @param calendar The string representation of the XML Gregorian calendar to be parsed.
     * @param format The format of the calendar string. If {@code null}, the method will try to parse the calendar using common calendar formats.
     * @param timeZone The TimeZone to be used for parsing the calendar string. If {@code null}, the method will use the default TimeZone.
     * @return The parsed javax.xml.datatype.XMLGregorianCalendar object or {@code null} if the provided calendar string is {@code null} or empty or "null".
     */
    @MayReturnNull
    @Beta
    public static XMLGregorianCalendar parseXMLGregorianCalendar(final String calendar, final String format, final TimeZone timeZone) {
        if (isNullDateTime(calendar)) {
            return null;
        }

        return createXMLGregorianCalendar(parse(calendar, format, timeZone), timeZone);
    }

    private static boolean isNullDateTime(final String date) {
        return Strings.isEmpty(date) || (date.length() == 4 && "null".equalsIgnoreCase(date));
    }

    private static boolean isPossibleLong(final CharSequence dateTime) {
        if (dateTime.length() > 4) {
            char ch = dateTime.charAt(2);

            if (ch >= '0' && ch <= '9') {
                ch = dateTime.charAt(4);

                return ch >= '0' && ch <= '9';
            }
        }

        return false;
    }

    private static long parse(final String dateTime, final String format, final TimeZone timezone) {
        if ((format == null) && isPossibleLong(dateTime)) {
            try {
                return Long.parseLong(dateTime);
            } catch (final NumberFormatException e) {
                // ignore.
                if (logger.isWarnEnabled()) {
                    logger.warn(FAILED_TO_PARSE_TO_LONG, dateTime);
                }
            }
        }

        final String formatToUse = checkDateFormat(dateTime, format);

        // use ISO8601Util.parse for better performance.
        if (Strings.isEmpty(formatToUse) || ISO_8601_DATE_TIME_FORMAT.equals(formatToUse) || ISO_8601_TIMESTAMP_FORMAT.equals(formatToUse)) {
            if (timezone == null || timezone.equals(ISO8601Util.TIMEZONE_Z)) {
                return ISO8601Util.parse(dateTime).getTime();
            } else {
                throw new RuntimeException("Unsupported date format: " + formatToUse + " with time zone: " + timezone);
            }
        }

        final TimeZone timeZoneToUse = checkTimeZone(dateTime, formatToUse, timezone);

        final long timeInMillis = fastDateParse(dateTime, formatToUse, timeZoneToUse);

        if (timeInMillis != 0) {
            return timeInMillis;
        }

        final DateFormat sdf = getSDF(formatToUse, timeZoneToUse);

        try {
            return sdf.parse(dateTime).getTime();
        } catch (final ParseException e) {
            throw new IllegalArgumentException(e);
        } finally {
            recycleSDF(formatToUse, timeZoneToUse, sdf);
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
    //                && Strings.isAsciiDigitalInteger(dateTime)) {
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
     * Formats current LocalDate with format {@code yyyy-MM-dd}.
     *
     * @return A string representation of the current LocalDate.
     */
    @Beta
    public static String formatLocalDate() {
        return format(currentDate(), LOCAL_DATE_FORMAT);
    }

    /**
     * Formats current LocalDateTime with format with specified {@code yyyy-MM-dd HH:mm:ss}.
     *
     * @return A string representation of the current LocalDateTime.
     */
    @Beta
    public static String formatLocalDateTime() {
        return format(currentDate(), LOCAL_DATE_TIME_FORMAT);
    }

    /**
     * Formats current DateTime with format with specified {@code yyyy-MM-dd'T'HH:mm:ss'Z'}.
     *
     * @return A string representation of the current DateTime.
     */
    public static String formatCurrentDateTime() {
        return format(currentDate(), ISO_8601_DATE_TIME_FORMAT);
    }

    /**
     * Formats current Timestamp with format with specified {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}.
     *
     * @return A string representation of the current Timestamp.
     */
    public static String formatCurrentTimestamp() {
        return format(currentTimestamp(), ISO_8601_TIMESTAMP_FORMAT);
    }

    /**
     * Formats specified {@code date} with format {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} if it's a {@code Timestamp}, otherwise format it with format {@code yyyy-MM-dd'T'HH:mm:ss'Z'}.
     *
     * @param date The java.util.Date instance to be formatted.
     * @return A string representation of the specified date.
     */
    public static String format(final java.util.Date date) {
        return format(date, null, null);
    }

    /**
     * Formats the provided java.util.Date instance into a string representation according to the provided format.
     * If no format is provided, a default format is used.
     *
     * @param date The java.util.Date instance to be formatted.
     * @param format The format to be used for formatting the java.util.Date instance. If {@code null}, a default format is used.
     * @return A string representation of the java.util.Date instance.
     */
    public static String format(final java.util.Date date, final String format) {
        return format(date, format, null);
    }

    /**
     * Formats the provided java.util.Date instance into a string representation according to the provided format and timezone.
     * If no format is provided, a default format is used.
     * If no timezone is provided, the default timezone of the system is used.
     *
     * @param date The java.util.Date instance to be formatted.
     * @param format The format to be used for formatting the java.util.Date instance. If {@code null}, a default format is used.
     * @param timeZone The timezone to be used for formatting the java.util.Date instance. If {@code null}, the system's default timezone is used.
     * @return A string representation of the java.util.Date instance.
     */
    public static String format(final java.util.Date date, final String format, final TimeZone timeZone) {
        return formatDate(null, date, format, timeZone);
    }

    /**
     * Formats specified {@code calendar} with format {@code yyyy-MM-dd'T'HH:mm:ss'Z'}.
     *
     * @param calendar The Calendar instance to be formatted.
     * @return A string representation of the Calendar instance.
     */
    public static String format(final Calendar calendar) {
        return format(calendar, null, null);
    }

    /**
     * Formats the provided Calendar instance into a string representation according to the provided format.
     * If no format is provided, a default format is used.
     *
     * @param calendar The Calendar instance to be formatted.
     * @param format The format to be used for formatting the Calendar instance. If {@code null}, a default format is used.
     * @return A string representation of the Calendar instance.
     */
    public static String format(final Calendar calendar, final String format) {
        return format(calendar, format, null);
    }

    /**
     * Formats the provided Calendar instance into a string representation according to the provided format and timezone.
     * If no format is provided, a default format is used.
     * If no timezone is provided, the default timezone of the system is used.
     *
     * @param calendar The Calendar instance to be formatted.
     * @param format The format to be used for formatting the Calendar instance. If {@code null}, a default format is used.
     * @param timeZone The timezone to be used for formatting the Calendar instance. If {@code null}, the system's default timezone is used.
     * @return A string representation of the Calendar instance.
     */
    public static String format(final Calendar calendar, final String format, final TimeZone timeZone) {
        if ((format == null) && (timeZone == null)) {
            final StringBuilder sb = Objectory.createStringBuilder();

            fastDateFormat(sb, null, calendar.getTimeInMillis(), false);

            final String str = sb.toString();

            Objectory.recycle(sb);

            return str;
        }

        return format(createJUDate(calendar), format, timeZone);
    }

    /**
     * Formats specified {@code XMLGregorianCalendar} with format {@code yyyy-MM-dd'T'HH:mm:ss'Z'}.
     *
     * @param calendar The XMLGregorianCalendar instance to be formatted.
     * @return A string representation of the XMLGregorianCalendar instance.
     * @see #format(Calendar)
     */
    public static String format(final XMLGregorianCalendar calendar) {
        return format(calendar, null, null);
    }

    /**
     * Formats the provided XMLGregorianCalendar instance into a string representation according to the provided format.
     * If no format is provided, a default format is used.
     *
     * @param calendar The XMLGregorianCalendar instance to be formatted.
     * @param format The format to be used for formatting the XMLGregorianCalendar instance.
     * @return A string representation of the XMLGregorianCalendar instance.
     * @see #format(Calendar, String)
     */
    public static String format(final XMLGregorianCalendar calendar, final String format) {
        return format(calendar, format, null);
    }

    /**
     * Formats the provided XMLGregorianCalendar instance into a string representation according to the provided format and timezone.
     * If no format is provided, a default format is used.
     * If no timezone is provided, the default timezone of the system is used.
     *
     * @param calendar The XMLGregorianCalendar instance to be formatted.
     * @param format The format to be used for formatting the XMLGregorianCalendar instance.
     * @param timeZone The timezone to be used for formatting the XMLGregorianCalendar instance.
     * @return A string representation of the XMLGregorianCalendar instance.
     * @see #format(Calendar, String, TimeZone)
     */
    public static String format(final XMLGregorianCalendar calendar, final String format, final TimeZone timeZone) {
        if ((format == null) && (timeZone == null)) {
            final StringBuilder sb = Objectory.createStringBuilder();

            fastDateFormat(sb, null, calendar.toGregorianCalendar().getTimeInMillis(), false);

            final String str = sb.toString();

            Objectory.recycle(sb);

            return str;
        }

        return format(createJUDate(calendar.toGregorianCalendar()), format, timeZone);
    }

    /**
     * Formats the provided java.util.Date instance into a string representation and appends it to the provided Appendable.
     *
     * @param appendable The Appendable to which the formatted date string is to be appended.
     * @param date The java.util.Date instance to be formatted.
     * @see #format(java.util.Date)
     */
    public static void formatTo(final Appendable appendable, final java.util.Date date) {
        formatTo(appendable, date, null, null);
    }

    /**
     * Formats the provided java.util.Date instance into a string representation according to the provided format.
     * The string representation is appended to the provided Appendable instance.
     *
     * @param appendable The Appendable to which the formatted date string is to be appended.
     * @param date The java.util.Date instance to be formatted.
     * @param format The format to be used for formatting the java.util.Date instance.
     * @see #format(java.util.Date, String)
     */
    public static void formatTo(final Appendable appendable, final java.util.Date date, final String format) {
        formatTo(appendable, date, format, null);
    }

    /**
     * Formats the provided java.util.Date instance into a string representation according to the provided format and timezone.
     *
     * @param appendable The Appendable to which the formatted date string is to be appended.
     * @param date The java.util.Date instance to be formatted.
     * @param format The format to be used for formatting the java.util.Date instance.
     * @param timeZone The timezone to be used for formatting the java.util.Date instance.
     * @see #format(java.util.Date, String, TimeZone)
     */
    public static void formatTo(final Appendable appendable, final java.util.Date date, final String format, final TimeZone timeZone) {
        formatDate(appendable, date, format, timeZone);
    }

    /**
     * Formats the provided java.util.Calendar instance into a string representation and appends it to the provided Appendable.
     *
     * @param appendable The Appendable to which the formatted date string is to be appended.
     * @param calendar The java.util.Calendar instance to be formatted.
     * @see #format(java.util.Calendar)
     */
    public static void formatTo(final Appendable appendable, final Calendar calendar) {
        formatTo(appendable, calendar, null, null);
    }

    /**
     * Formats the provided java.util.Calendar instance into a string representation according to the provided format.
     *
     * @param appendable The Appendable to which the formatted date string is to be appended.
     * @param calendar The java.util.Calendar instance to be formatted.
     * @param format The format to be used for formatting the java.util.Calendar instance.
     * @see #format(java.util.Calendar, String)
     */
    public static void formatTo(final Appendable appendable, final Calendar calendar, final String format) {
        formatTo(appendable, calendar, format, null);
    }

    /**
     * Formats the provided java.util.Calendar instance into a string representation according to the provided format and timezone.
     * The string representation is appended to the provided Appendable instance.
     *
     * @param appendable The Appendable to which the formatted date string is to be appended.
     * @param calendar The java.util.Calendar instance to be formatted.
     * @param format The format to be used for formatting the java.util.Calendar instance.
     * @param timeZone The timezone to be used for formatting the java.util.Calendar instance.
     * @see #format(java.util.Calendar, String, TimeZone)
     */
    public static void formatTo(final Appendable appendable, final Calendar calendar, final String format, final TimeZone timeZone) {
        if ((format == null) && (timeZone == null)) {
            fastDateFormat(null, appendable, calendar.getTimeInMillis(), false);
        } else {
            formatTo(appendable, createJUDate(calendar), format, timeZone);
        }
    }

    /**
     * Formats the provided XMLGregorianCalendar instance into a string representation and appends it to the provided Appendable.
     * The default format is used for formatting the XMLGregorianCalendar instance.
     *
     * @param appendable The Appendable to which the formatted date string is to be appended.
     * @param calendar The XMLGregorianCalendar instance to be formatted.
     * @see #format(XMLGregorianCalendar)
     */
    public static void formatTo(final Appendable appendable, final XMLGregorianCalendar calendar) {
        formatTo(appendable, calendar, null, null);
    }

    /**
     * Formats the provided XMLGregorianCalendar instance into a string representation according to the provided format.
     *
     * @param appendable The Appendable to which the formatted date string is to be appended.
     * @param calendar The XMLGregorianCalendar instance to be formatted.
     * @param format The format to be used for formatting the XMLGregorianCalendar instance.
     * @see #format(XMLGregorianCalendar, String)
     */
    public static void formatTo(final Appendable appendable, final XMLGregorianCalendar calendar, final String format) {
        formatTo(appendable, calendar, format, null);
    }

    /**
     * Formats the provided XMLGregorianCalendar instance into a string representation according to the provided format and timezone.
     *
     * @param appendable The Appendable to which the formatted date string is to be appended.
     * @param calendar The XMLGregorianCalendar instance to be formatted.
     * @param format The format to be used for formatting the XMLGregorianCalendar instance.
     * @param timeZone The timezone to be used for formatting the XMLGregorianCalendar instance.
     * @see #format(XMLGregorianCalendar, String, TimeZone)
     */
    public static void formatTo(final Appendable appendable, final XMLGregorianCalendar calendar, final String format, final TimeZone timeZone) {
        if ((format == null) && (timeZone == null)) {
            fastDateFormat(null, appendable, calendar.toGregorianCalendar().getTimeInMillis(), false);
        } else {
            formatTo(appendable, createJUDate(calendar.toGregorianCalendar()), format, timeZone);
        }
    }

    private static String formatDate(final Appendable appendable, final java.util.Date date, String format, TimeZone timeZone) {
        final boolean isTimestamp = date instanceof Timestamp;

        if ((format == null) && (timeZone == null)) {
            if (appendable == null) {
                final StringBuilder sb = Objectory.createStringBuilder();

                fastDateFormat(sb, null, date.getTime(), isTimestamp);

                final String str = sb.toString();

                Objectory.recycle(sb);

                return str;
            } else {
                fastDateFormat(null, appendable, date.getTime(), isTimestamp);

                return null;
            }
        }

        if (format == null) {
            format = isTimestamp ? ISO_8601_TIMESTAMP_FORMAT : ISO_8601_DATE_TIME_FORMAT;
        }

        timeZone = checkTimeZone(null, format, timeZone);

        final DateFormat sdf = getSDF(format, timeZone);

        try {
            final String str = sdf.format(date);

            if (appendable != null) {
                try {
                    appendable.append(str);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            return str;
        } finally {
            recycleSDF(format, timeZone, sdf);
        }
    }

    private static void fastDateFormat(final StringBuilder sb, final Appendable appendable, final long timeInMillis, final boolean isTimestamp) {
        Calendar c = utcCalendarPool.poll();

        if (c == null) {
            c = Calendar.getInstance(UTC_TIME_ZONE);
        }

        c.setTimeInMillis(timeInMillis);

        final int year = c.get(Calendar.YEAR);
        final int month = c.get(Calendar.MONTH) + 1;
        final int day = c.get(Calendar.DAY_OF_MONTH);
        final int hour = c.get(Calendar.HOUR_OF_DAY);
        final int minute = c.get(Calendar.MINUTE);
        final int second = c.get(Calendar.SECOND);

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

            final int milliSecond = c.get(Calendar.MILLISECOND);
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
        } catch (final IOException e) {
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
     */
    private static <T extends java.util.Date> T set(final T date, final int calendarField, final int amount) {
        N.checkArgNotNull(date, cs.date);

        // getInstance() returns a new object, so this method is thread safe.
        final Calendar c = Calendar.getInstance();
        c.setLenient(false);
        c.setTime(date);
        //noinspection MagicConstant
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
     * @throws IllegalArgumentException
     */
    @Beta
    public static <T extends java.util.Date> T roll(final T date, final long amount, final TimeUnit unit) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

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
     * @throws IllegalArgumentException
     */
    @Beta
    public static <T extends java.util.Date> T roll(final T date, final int amount, final CalendarField unit) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);
        N.checkArgNotNull(unit, cs.CalendarField);

        //    if (amount > Integer.MAX_VALUE || amount < Integer.MIN_VALUE) {
        //        throw new IllegalArgumentException("The amount :" + amount + " is too big for unit: " + unit);
        //    }

        if (unit == CalendarField.MONTH || unit == CalendarField.YEAR) {
            final Calendar c = createCalendar(date);
            //noinspection MagicConstant
            c.add(unit.value(), amount);

            return createDate(c.getTimeInMillis(), date.getClass());
        } else {
            return createDate(date.getTime() + toMillis(unit, amount), date.getClass());
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
     * @throws IllegalArgumentException
     */
    @Beta
    public static <T extends Calendar> T roll(final T calendar, final long amount, final TimeUnit unit) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar); //NOSONAR
        N.checkArgNotNull(unit, cs.CalendarField);

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
     * @throws IllegalArgumentException
     */
    @Beta
    public static <T extends Calendar> T roll(final T calendar, final int amount, final CalendarField unit) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);
        N.checkArgNotNull(unit, cs.CalendarField);

        //    if (amount > Integer.MAX_VALUE || amount < Integer.MIN_VALUE) {
        //        throw new IllegalArgumentException("The amount :" + amount + " is too big for unit: " + unit);
        //    }

        final T result = createCalendar(calendar, calendar.getTimeInMillis());

        //noinspection MagicConstant
        result.add(unit.value(), amount);

        return result;
    }

    //-----------------------------------------------------------------------

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
     * @param field the field from {@code Calendar} or {@code SEMI_MONTH}
     * @return
     * @throws IllegalArgumentException
     * @throws ArithmeticException if the year is over 280 million
     */
    public static <T extends java.util.Date> T round(final T date, final int field) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        final Calendar gval = Calendar.getInstance();
        gval.setTime(date);
        modify(gval, field, ModifyType.ROUND);

        return createDate(gval.getTimeInMillis(), date.getClass());
    }

    /**
     * Rounds the given date to the nearest whole unit as specified by the CalendarField.
     * The original date object is unchanged.
     *
     * @param <T> The type of the date object, which must be a subclass of java.util.Date.
     * @param date The date to be rounded. Must not be {@code null}.
     * @param field The CalendarField to which the date is to be rounded. Must not be {@code null}.
     * @return A new date object of type T, rounded to the nearest whole unit as specified by the field.
     * @throws IllegalArgumentException if the date or field is {@code null}.
     */
    public static <T extends java.util.Date> T round(final T date, final CalendarField field) {
        N.checkArgNotNull(date, cs.date);
        N.checkArgNotNull(field, cs.CalendarField);

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
     * @param field the field from {@code Calendar} or {@code SEMI_MONTH}
     * @return
     * @throws IllegalArgumentException if the date is {@code null}
     * @throws ArithmeticException if the year is over 280 million
     */
    public static <T extends Calendar> T round(final T calendar, final int field) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        final Calendar rounded = (Calendar) calendar.clone();
        modify(rounded, field, ModifyType.ROUND);

        if (rounded.getClass().equals(calendar.getClass())) {
            return (T) rounded;
        }

        return createCalendar(calendar, rounded.getTimeInMillis());
    }

    /**
     * Rounds the given calendar to the nearest whole unit as specified by the CalendarField.
     * The original calendar object is unchanged.
     *
     * @param <T> The type of the calendar object, which must be a subclass of java.util.Calendar.
     * @param calendar The calendar to be rounded. Must not be {@code null}.
     * @param field The CalendarField to which the calendar is to be rounded. Must not be {@code null}.
     * @return A new calendar object of type T, rounded to the nearest whole unit as specified by the field.
     * @throws IllegalArgumentException if the calendar or field is {@code null}.
     */
    public static <T extends Calendar> T round(final T calendar, final CalendarField field) {
        N.checkArgNotNull(calendar, cs.calendar);
        N.checkArgNotNull(field, cs.CalendarField);

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
     * @param field the field from {@code Calendar} or {@code SEMI_MONTH}
     * @return
     * @throws IllegalArgumentException if the date is {@code null}
     * @throws ArithmeticException if the year is over 280 million
     */
    public static <T extends java.util.Date> T truncate(final T date, final int field) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        final Calendar gval = Calendar.getInstance();
        gval.setTime(date);
        modify(gval, field, ModifyType.TRUNCATE);

        return createDate(gval.getTimeInMillis(), date.getClass());
    }

    /**
     * Truncates the given date, leaving the field specified as the most significant field.
     * The original date object is unchanged.
     *
     * @param <T> The type of the date object, which must be a subclass of java.util.Date.
     * @param date The date to be truncated. Must not be {@code null}.
     * @param field The CalendarField to which the date is to be truncated. Must not be {@code null}.
     * @return A new date object of type T, truncated to the nearest whole unit as specified by the field.
     * @throws IllegalArgumentException if the date or field is {@code null}.
     */
    public static <T extends java.util.Date> T truncate(final T date, final CalendarField field) {
        N.checkArgNotNull(date, cs.date);
        N.checkArgNotNull(field, cs.CalendarField);

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
     * @param field the field from {@code Calendar} or {@code SEMI_MONTH}
     * @return
     * @throws IllegalArgumentException if the date is {@code null}
     * @throws ArithmeticException if the year is over 280 million
     */
    public static <T extends Calendar> T truncate(final T calendar, final int field) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        final Calendar truncated = (Calendar) calendar.clone();
        modify(truncated, field, ModifyType.TRUNCATE);

        if (truncated.getClass().equals(calendar.getClass())) {
            return (T) truncated;
        }

        return createCalendar(calendar, truncated.getTimeInMillis());
    }

    /**
     * Truncates the given calendar, leaving the field specified as the most significant field.
     * The original calendar object is unchanged.
     *
     * @param <T> The type of the calendar object, which must be a subclass of java.util.Calendar.
     * @param calendar The calendar to be truncated. Must not be {@code null}.
     * @param field The CalendarField to which the calendar is to be truncated. Must not be {@code null}.
     * @return A new calendar object of type T, truncated to the nearest whole unit as specified by the field.
     * @throws IllegalArgumentException if the calendar or field is {@code null}.
     */
    public static <T extends Calendar> T truncate(final T calendar, final CalendarField field) {
        N.checkArgNotNull(calendar, cs.calendar);
        N.checkArgNotNull(field, cs.CalendarField);

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
     * @param field the field from {@code Calendar} or {@code SEMI_MONTH}
     * @return
     * @throws IllegalArgumentException if the date is {@code null}
     * @throws ArithmeticException if the year is over 280 million
     */
    public static <T extends java.util.Date> T ceiling(final T date, final int field) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        final Calendar gval = Calendar.getInstance();
        gval.setTime(date);
        modify(gval, field, ModifyType.CEILING);

        return createDate(gval.getTimeInMillis(), date.getClass());
    }

    /**
     * Returns a new date object of the same type as the input, but adjusted to the nearest future unit as specified by the CalendarField.
     * The original date object is unchanged.
     *
     * @param <T> The type of the date object, which must be a subclass of java.util.Date.
     * @param date The date to be adjusted. Must not be {@code null}.
     * @param field The CalendarField to which the date is to be adjusted. Must not be {@code null}.
     * @return A new date object of type T, adjusted to the nearest future unit as specified by the field.
     * @throws IllegalArgumentException if the date or field is {@code null}.
     */
    public static <T extends java.util.Date> T ceiling(final T date, final CalendarField field) {
        N.checkArgNotNull(date, cs.date);
        N.checkArgNotNull(field, cs.CalendarField);

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
     * @param field the field from {@code Calendar} or {@code SEMI_MONTH}
     * @return
     * @throws IllegalArgumentException if the date is {@code null}
     * @throws ArithmeticException if the year is over 280 million
     */
    public static <T extends Calendar> T ceiling(final T calendar, final int field) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        final Calendar ceiled = (Calendar) calendar.clone();

        modify(ceiled, field, ModifyType.CEILING);

        if (ceiled.getClass().equals(calendar.getClass())) {
            return (T) ceiled;
        }

        return createCalendar(calendar, ceiled.getTimeInMillis());
    }

    /**
     * Adjusts the given calendar to the ceiling of the specified field.
     * The original calendar object is unchanged; a new calendar object representing the adjusted time is returned.
     * This method can be used to round up the calendar to the nearest value of the specified field.
     *
     * @param <T> The type of the calendar object, which must extend java.util.Calendar.
     * @param calendar The original calendar object to be adjusted.
     * @param field The field to be used for the ceiling operation, as a CalendarField.
     * @return A new calendar object representing the adjusted time.
     * @throws IllegalArgumentException if the calendar is {@code null}.
     */
    public static <T extends Calendar> T ceiling(final T calendar, final CalendarField field) {
        N.checkArgNotNull(calendar, cs.calendar);
        N.checkArgNotNull(field, cs.CalendarField);

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
        final int millis = val.get(Calendar.MILLISECOND);
        if (ModifyType.TRUNCATE == modType || millis < 500) {
            time = time - millis;
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
                            //noinspection MagicConstant
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
                @SuppressWarnings("MagicConstant")
                final int min = val.getActualMinimum(aField[0]);
                @SuppressWarnings("MagicConstant")
                final int max = val.getActualMaximum(aField[0]);
                //Calculate the offset from the minimum allowed value
                //noinspection MagicConstant
                offset = val.get(aField[0]) - min;
                //Set roundUp if this is more than halfway between the minimum and maximum
                roundUp = offset > ((max - min) / 2);
            }
            //We need to remove this field
            if (offset != 0) {
                //noinspection MagicConstant
                val.set(aField[0], val.get(aField[0]) - offset);
            }
        }

        throw new IllegalArgumentException("The field " + field + " is not supported");
    }

    /**
     * Determines if two calendars are equal up to no more than the specified most significant field.
     *
     * @param cal1 the first calendar, not null
     * @param cal2 the second calendar, not null
     * @param field the field from {@code CalendarField} to be the most significant field for comparison
     * @return {@code true} if cal1 and cal2 are equal up to the specified field; {@code false} otherwise
     * @throws IllegalArgumentException if any argument is {@code null}
     */
    public static boolean truncatedEquals(final Calendar cal1, final Calendar cal2, final CalendarField field) {
        return truncatedCompareTo(cal1, cal2, field) == 0;
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Determines if two calendars are equal up to no more than the specified most significant field.
     *
     * @param cal1 the first calendar, not {@code null}
     * @param cal2 the second calendar, not {@code null}
     * @param field the field from {@code Calendar}
     * @return {@code true} if equal; otherwise {@code false}
     * @throws IllegalArgumentException if any argument is {@code null}
     * @see #truncate(Calendar, int)
     * @see #truncatedEquals(java.util.Date, java.util.Date, int)
     */
    public static boolean truncatedEquals(final Calendar cal1, final Calendar cal2, final int field) {
        return truncatedCompareTo(cal1, cal2, field) == 0;
    }

    /**
     * Determines if two dates are equal up to no more than the specified most significant field.
     *
     * @param date1 The first date, not {@code null}.
     * @param date2 The second date, not {@code null}.
     * @param field The field from {@code CalendarField} to be the most significant field for comparison.
     * @return {@code true} if date1 and date2 are equal up to the specified field; {@code false} otherwise.
     * @throws IllegalArgumentException if any argument is {@code null}
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
     * @param date1 the first date, not {@code null}
     * @param date2 the second date, not {@code null}
     * @param field the field from {@code Calendar}
     * @return {@code true} if equal; otherwise {@code false}
     * @throws IllegalArgumentException if any argument is {@code null}
     * @see #truncate(java.util.Date, int)
     * @see #truncatedEquals(Calendar, Calendar, int)
     */
    public static boolean truncatedEquals(final java.util.Date date1, final java.util.Date date2, final int field) {
        return truncatedCompareTo(date1, date2, field) == 0;
    }

    /**
     * Compares two Calendar instances up to the specified field.
     * The comparison is based on the most significant field, meaning that it compares
     * the Calendar instances year by year, month by month, day by day, etc., depending on the specified field.
     *
     * @param cal1 The first Calendar instance to be compared, not {@code null}.
     * @param cal2 The second Calendar instance to be compared, not {@code null}.
     * @param field The field from {@code CalendarField} to be the most significant field for comparison.
     * @return A negative integer, zero, or a positive integer as the first Calendar is less than, equal to, or greater than the second.
     * @throws IllegalArgumentException if any argument is {@code null}.
     */
    public static int truncatedCompareTo(final Calendar cal1, final Calendar cal2, final CalendarField field) {
        return truncate(cal1, field).compareTo(truncate(cal2, field));
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Determines how two calendars compare up to no more than the specified most significant field.
     *
     * @param cal1 the first calendar, not {@code null}
     * @param cal2 the second calendar, not {@code null}
     * @param field the field from {@code Calendar}
     * @return a negative integer, zero, or a positive integer as the first
     * calendar is less than, equal to, or greater than the second.
     * @throws IllegalArgumentException if any argument is {@code null}
     * @see #truncate(Calendar, int)
     * @see #truncatedCompareTo(java.util.Date, java.util.Date, int)
     */
    public static int truncatedCompareTo(final Calendar cal1, final Calendar cal2, final int field) {
        return truncate(cal1, field).compareTo(truncate(cal2, field));
    }

    /**
     * Compares two Date instances up to the specified field.
     * The comparison is based on the most significant field, meaning that it compares
     * the Date instances year by year, month by month, day by day, etc., depending on the specified field.
     *
     * @param date1 The first Date instance to be compared, not {@code null}.
     * @param date2 The second Date instance to be compared, not {@code null}.
     * @param field The field from {@code CalendarField} to be the most significant field for comparison.
     * @return A negative integer, zero, or a positive integer as the first Date is less than, equal to, or greater than the second.
     * @throws IllegalArgumentException if any argument is {@code null}.
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
     * @param date1 the first date, not {@code null}
     * @param date2 the second date, not {@code null}
     * @param field the field from {@code Calendar}
     * @return a negative integer, zero, or a positive integer as the first
     * date is less than, equal to, or greater than the second.
     * @throws IllegalArgumentException if any argument is {@code null}
     * @see #truncate(Calendar, int)
     * @see #truncatedCompareTo(java.util.Date, java.util.Date, int)
     */
    public static int truncatedCompareTo(final java.util.Date date1, final java.util.Date date2, final int field) {
        return truncate(date1, field).compareTo(truncate(date2, field));
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of milliseconds within the
     * fragment. All date fields greater than the fragment will be ignored.</p>
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
     */
    public static long getFragmentInMilliseconds(final java.util.Date date, final CalendarField fragment) {
        return getFragment(date, fragment.value(), TimeUnit.MILLISECONDS);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of seconds within the
     * fragment. All date fields greater than the fragment will be ignored.</p>
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
     */
    public static long getFragmentInSeconds(final java.util.Date date, final CalendarField fragment) {
        return getFragment(date, fragment.value(), TimeUnit.SECONDS);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of minutes within the
     * fragment. All date fields greater than the fragment will be ignored.</p>
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
     */
    public static long getFragmentInMinutes(final java.util.Date date, final CalendarField fragment) {
        return getFragment(date, fragment.value(), TimeUnit.MINUTES);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of hours within the
     * fragment. All date fields greater than the fragment will be ignored.</p>
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
     * A fragment less than or equal to an HOUR field will return 0.</p>
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
     */
    public static long getFragmentInHours(final java.util.Date date, final CalendarField fragment) {
        return getFragment(date, fragment.value(), TimeUnit.HOURS);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of days within the
     * fragment. All date fields greater than the fragment will be ignored.</p>
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
     * @throws IllegalArgumentException if the date is {@code null} or specified fragment is not supported
     */
    private static long getFragment(final java.util.Date date, final int fragment, final TimeUnit unit) {
        N.checkArgNotNull(date, cs.date);

        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return getFragment(calendar, fragment, unit);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of milliseconds within the
     * fragment. All date fields greater than the fragment will be ignored.</p>
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
     */
    public static long getFragmentInMilliseconds(final Calendar calendar, final CalendarField fragment) {
        return getFragment(calendar, fragment.value(), TimeUnit.MILLISECONDS);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of seconds within the
     * fragment. All date fields greater than the fragment will be ignored.</p>
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
     */
    public static long getFragmentInSeconds(final Calendar calendar, final CalendarField fragment) {
        return getFragment(calendar, fragment.value(), TimeUnit.SECONDS);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of minutes within the
     * fragment. All date fields greater than the fragment will be ignored.</p>
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
     */
    public static long getFragmentInMinutes(final Calendar calendar, final CalendarField fragment) {
        return getFragment(calendar, fragment.value(), TimeUnit.MINUTES);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of hours within the
     * fragment. All date fields greater than the fragment will be ignored.</p>
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
     * A fragment less than or equal to an HOUR field will return 0.</p>
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
     */
    public static long getFragmentInHours(final Calendar calendar, final CalendarField fragment) {
        return getFragment(calendar, fragment.value(), TimeUnit.HOURS);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Returns the number of days within the
     * fragment. All date fields greater than the fragment will be ignored.</p>
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
     */
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    private static long getFragment(final Calendar calendar, final int fragment, final TimeUnit unit) {
        N.checkArgNotNull(calendar, cs.calendar);

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
     * <p>28 Mar 2002 13:45 and 28 Mar 2002 06:01 would return {@code true}.
     * 28 Mar 2002 13:45 and 12 Mar 2002 13:45 would return {@code false}.
     * </p>
     *
     * @param date1 the first date, not altered, not null
     * @param date2 the second date, not altered, not null
     * @return {@code true} if they represent the same day
     * @throws IllegalArgumentException if either date is {@code null}
     */
    public static boolean isSameDay(final java.util.Date date1, final java.util.Date date2) throws IllegalArgumentException {
        N.checkArgNotNull(date1, cs.date1);
        N.checkArgNotNull(date2, cs.date2);

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
     * <p>28 Mar 2002 13:45 and 28 Mar 2002 06:01 would return {@code true}.
     * 28 Mar 2002 13:45 and 12 Mar 2002 13:45 would return {@code false}.
     * </p>
     *
     * @param cal1 the first calendar, not altered, not null
     * @param cal2 the second calendar, not altered, not null
     * @return {@code true} if they represent the same day
     * @throws IllegalArgumentException if either calendar is {@code null}
     */
    public static boolean isSameDay(final Calendar cal1, final Calendar cal2) throws IllegalArgumentException {
        N.checkArgNotNull(cal1, cs.calendar1);
        N.checkArgNotNull(cal2, cs.calendar2);

        return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Checks if two date objects are in the same month ignoring day and time.
     *
     * @param date1 The first date to compare. Must not be {@code null}.
     * @param date2 The second date to compare. Must not be {@code null}.
     * @return {@code true} if the two dates are in the same month of the same year.
     * @throws IllegalArgumentException if either date is {@code null}.
     */
    public static boolean isSameMonth(final java.util.Date date1, final java.util.Date date2) throws IllegalArgumentException {
        N.checkArgNotNull(date1, cs.date1);
        N.checkArgNotNull(date2, cs.date2);

        final Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);

        final Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);

        return isSameMonth(cal1, cal2);
    }

    /**
     * Checks if two calendar objects are in the same month ignoring day and time.
     *
     * @param cal1 The first calendar to compare. Must not be {@code null}.
     * @param cal2 The second calendar to compare. Must not be {@code null}.
     * @return {@code true} if the two calendars are in the same month of the same year.
     * @throws IllegalArgumentException if either calendar is {@code null}.
     */
    public static boolean isSameMonth(final Calendar cal1, final Calendar cal2) throws IllegalArgumentException {
        N.checkArgNotNull(cal1, cs.calendar1);
        N.checkArgNotNull(cal2, cs.calendar2);

        return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH);
    }

    /**
     * Checks if two date objects are in the same year ignoring month, day and time.
     *
     * @param date1 The first date to compare. Must not be {@code null}.
     * @param date2 The second date to compare. Must not be {@code null}.
     * @return {@code true} if the two dates are in the same year.
     * @throws IllegalArgumentException if either date is {@code null}.
     */
    public static boolean isSameYear(final java.util.Date date1, final java.util.Date date2) throws IllegalArgumentException {
        N.checkArgNotNull(date1, cs.date1);
        N.checkArgNotNull(date2, cs.date2);

        final Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);

        final Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);

        return isSameYear(cal1, cal2);
    }

    /**
     * Checks if two calendar objects are in the same year ignoring month, day and time.
     *
     * @param cal1 The first calendar to compare. Must not be {@code null}.
     * @param cal2 The second calendar to compare. Must not be {@code null}.
     * @return {@code true} if the two calendars are in the same year.
     * @throws IllegalArgumentException if either calendar is {@code null}.
     */
    public static boolean isSameYear(final Calendar cal1, final Calendar cal2) throws IllegalArgumentException {
        N.checkArgNotNull(cal1, cs.calendar1);
        N.checkArgNotNull(cal2, cs.calendar2);

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
     * @param date1 the first date, not altered, not null
     * @param date2 the second date, not altered, not null
     * @return {@code true} if they represent the same millisecond instant
     * @throws IllegalArgumentException if either date is {@code null}
     */
    public static boolean isSameInstant(final java.util.Date date1, final java.util.Date date2) throws IllegalArgumentException {
        N.checkArgNotNull(date1, cs.date1);
        N.checkArgNotNull(date2, cs.date2);
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
     * @param cal1 the first calendar, not altered, not null
     * @param cal2 the second calendar, not altered, not null
     * @return {@code true} if they represent the same millisecond instant
     * @throws IllegalArgumentException if either date is {@code null}
     */
    public static boolean isSameInstant(final Calendar cal1, final Calendar cal2) throws IllegalArgumentException {
        N.checkArgNotNull(cal1, cs.calendar1);
        N.checkArgNotNull(cal2, cs.calendar2);

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
     * @param cal1 the first calendar, not altered, not null
     * @param cal2 the second calendar, not altered, not null
     * @return {@code true} if they represent the same millisecond instant
     * @throws IllegalArgumentException if either date is {@code null}
     */
    public static boolean isSameLocalTime(final Calendar cal1, final Calendar cal2) throws IllegalArgumentException {
        N.checkArgNotNull(cal1, cs.calendar1);
        N.checkArgNotNull(cal2, cs.calendar2);

        return cal1.get(Calendar.MILLISECOND) == cal2.get(Calendar.MILLISECOND) && cal1.get(Calendar.SECOND) == cal2.get(Calendar.SECOND)
                && cal1.get(Calendar.MINUTE) == cal2.get(Calendar.MINUTE) && cal1.get(Calendar.HOUR_OF_DAY) == cal2.get(Calendar.HOUR_OF_DAY)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) && cal1.getClass() == cal2.getClass();
    }

    private static DateFormat getSDF(final String format, final TimeZone timeZone) {
        DateFormat sdf = null;

        if (timeZone == UTC_TIME_ZONE) {
            //noinspection ConditionCoveredByFurtherCondition
            if ((format.length() == 28) && format.equals(ISO_8601_TIMESTAMP_FORMAT)) {
                sdf = utcTimestampDFPool.poll();

                if (sdf == null) {
                    sdf = new SimpleDateFormat(format);
                    sdf.setTimeZone(timeZone);
                }

                return sdf;
            } else //noinspection ConditionCoveredByFurtherCondition
            if ((format.length() == 24) && format.equals(ISO_8601_DATE_TIME_FORMAT)) {
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

    private static void recycleSDF(final String format, final TimeZone timeZone, final DateFormat sdf) {
        if (timeZone == UTC_TIME_ZONE) {
            //noinspection ConditionCoveredByFurtherCondition
            if ((format.length() == 28) && format.equals(ISO_8601_TIMESTAMP_FORMAT)) {
                utcTimestampDFPool.add(sdf);
            } else //noinspection ConditionCoveredByFurtherCondition
            if ((format.length() == 24) && format.equals(ISO_8601_DATE_TIME_FORMAT)) {
                utcDateTimeDFPool.add(sdf);
            } else {
                dfPool.get(format).add(sdf);
            }
        } else {
            dfPool.get(format).add(sdf);
        }
    }

    private static String checkDateFormat(final String str, final String format) {
        if (Strings.isEmpty(format)) {
            final int len = str.length();

            if (len == 4) {
                return LOCAL_YEAR_FORMAT;
            } else if (len == 5 || str.charAt(2) == '-') {
                return LOCAL_MONTH_DAY_FORMAT;
            } else if (len > 4 && str.charAt(4) == '-') {
                switch (len) {
                    case 8:
                        return LOCAL_TIME_FORMAT;

                    case 10:
                        return LOCAL_DATE_FORMAT;

                    case 19:
                        if (str.charAt(10) == 'T') {
                            return ISO_LOCAL_DATE_TIME_FORMAT;
                        } else {
                            return LOCAL_DATE_TIME_FORMAT;
                        }

                    case 20:
                        if (str.charAt(19) == 'Z') {
                            return ISO_8601_DATE_TIME_FORMAT;
                        }
                        break;

                    case 24:
                        if (str.charAt(23) == 'Z') {
                            return ISO_8601_TIMESTAMP_FORMAT;
                        }

                        break;

                    case 25:
                        final char ch = str.charAt(19);

                        if (ch == '-' || ch == '+') {
                            return ISO_OFFSET_DATE_TIME_FORMAT;
                        }

                        break;

                    default:
                        // throw new RuntimeException("No valid date format found for: " + str);
                        return null;
                }
            } else if (len == 29 || str.charAt(3) == ',') {
                return RFC_1123_DATE_TIME_FORMAT;
            }
        }

        return format;
    }

    private static TimeZone checkTimeZone(final String dateTime, final String format, final TimeZone timeZone) {
        if ((Strings.isNotEmpty(dateTime) && dateTime.endsWith("Z")) || (Strings.isNotEmpty(format) && format.endsWith("'Z'"))) {
            return UTC_TIME_ZONE;
        }

        return timeZone == null ? DEFAULT_TIME_ZONE : timeZone;
    }

    private static long fastDateParse(final String str, final String format, final TimeZone timeZone) {
        final int len = str.length();

        if (!((len == 19) || (len == 20) || (len == 24)) || !(format.equals(ISO_8601_TIMESTAMP_FORMAT) || format.equals(ISO_8601_DATE_TIME_FORMAT)
                || format.equals(ISO_LOCAL_DATE_TIME_FORMAT) || format.equals(LOCAL_DATE_TIME_FORMAT))) {
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

        final int year = parseInt(str, 0, 4);
        final int month = parseInt(str, 5, 7) - 1;
        final int date = parseInt(str, 8, 10);
        final int hourOfDay = parseInt(str, 11, 13);
        final int minute = parseInt(str, 14, 16);
        final int second = parseInt(str, 17, 19);
        final int milliSecond = len == 24 ? parseInt(str, 20, 23) : 0;

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

        //noinspection MagicConstant
        c.set(year, month, date, hourOfDay, minute, second);
        c.set(Calendar.MILLISECOND, milliSecond);

        final long timeInMillis = c.getTimeInMillis();

        if (timeZone == UTC_TIME_ZONE) {
            utcCalendarPool.add(c);
        } else {
            timeZoneCalendarQueue.add(c);
        }

        return timeInMillis;
    }

    private static int parseInt(final String str, int fromIndex, final int toIndex) {
        int result = 0;

        while (fromIndex < toIndex) {
            result = (result * 10) + (str.charAt(fromIndex++) - 48);
        }

        return result;
    }

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

    private static <T extends Calendar> T createCalendar(final T c, final long millis) {
        final Class<T> cls = (Class<T>) c.getClass();

        Calendar result = null;

        //noinspection ConstantValue
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
     * Checks if the provided date is the last date of its month.
     *
     * @param date The date to check. Must not be {@code null}.
     * @return {@code true} if the provided date is the last date of its month.
     * @throws IllegalArgumentException if the date is {@code null}.
     */
    public static boolean isLastDateOfMonth(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        return cal.get(Calendar.DAY_OF_MONTH) == cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /**
     * Checks if the provided date is the last date of its year.
     *
     * @param date The date to check. Must not be {@code null}.
     * @return {@code true} if the provided date is the last date of its year.
     * @throws IllegalArgumentException if the date is {@code null}.
     */
    public static boolean isLastDateOfYear(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        return cal.get(Calendar.DAY_OF_YEAR) == cal.getActualMaximum(Calendar.DAY_OF_YEAR);
    }

    /**
     * Returns the last day of the month for the given date.
     *
     * @param date The date to be evaluated. Must not be {@code null}.
     * @return The last day of the month for the given date as an integer.
     * @throws IllegalArgumentException if the date is {@code null}.
     */
    public static int getLastDateOfMonth(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /**
     * Returns the last day of the year for the given date.
     *
     * @param date The date to be evaluated. Must not be {@code null}.
     * @return The last day of the year for the given date as an integer.
     * @throws IllegalArgumentException if the date is {@code null}.
     */
    public static int getLastDateOfYear(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        return cal.getActualMaximum(Calendar.DAY_OF_YEAR);
    }

    //    /**
    //     * The Class Times.
    //     */
    //    @Beta
    //    public static final class Times extends DateUtil {
    //
    //        private Times() {
    //            // singleton.
    //        }
    //    }

    /**
     * The major purpose of this class is to get rid of the milliseconds part: {@code .SSS} or nanoseconds part: {@code .SSSSSS}
     *
     * @see {@code DateTimeFormatter}
     */
    public static final class DTF {

        /**
         * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'}
         * @see {@link DateTimeFormatter#ISO_ZONED_DATE_TIME}
         */
        static final String ISO_ZONED_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'";

        //    /**
        //     * Date/Time format: {@code yyyy}
        //     * @see {@link #LOCAL_YEAR_FORMAT}
        //     */
        //    public static final DTF LOCAL_YEAR = new DTF(DateTimeUtil.LOCAL_YEAR_FORMAT);
        //
        //    /**
        //     * Date/Time format: {@code MM-dd}
        //     * @see {@link #LOCAL_MONTH_DAY_FORMAT}
        //     */
        //    public static final DTF LOCAL_MONTH_DAY = new DTF(DateTimeUtil.LOCAL_MONTH_DAY_FORMAT);

        /**
         * Date/Time format: {@code yyyy-MM-dd}
         * @see {@link #LOCAL_DATE_FORMAT}
         */
        public static final DTF LOCAL_DATE = new DTF(DateTimeUtil.LOCAL_DATE_FORMAT);

        /**
         * Date/Time format: {@code HH:mm:ss}
         * @see {@link #LOCAL_TIME_FORMAT}
         */
        public static final DTF LOCAL_TIME = new DTF(DateTimeUtil.LOCAL_TIME_FORMAT);

        /**
         * Date/Time format: {@code yyyy-MM-dd HH:mm:ss}
         * @see {@link #LOCAL_DATE_TIME_FORMAT}
         */
        public static final DTF LOCAL_DATE_TIME = new DTF(DateTimeUtil.LOCAL_DATE_TIME_FORMAT);

        /**
         * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ss}
         * @see {@link #ISO_LOCAL_DATE_TIME_FORMAT}
         */
        public static final DTF ISO_LOCAL_DATE_TIME = new DTF(DateTimeUtil.ISO_LOCAL_DATE_TIME_FORMAT);

        /**
         * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ssXXX}
         * @see {@link #ISO_OFFSET_DATE_TIME_FORMAT}
         */
        public static final DTF ISO_OFFSET_DATE_TIME = new DTF(DateTimeUtil.ISO_OFFSET_DATE_TIME_FORMAT);

        /**
         * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'}
         * @see {@link #ISO_ZONED_DATE_TIME_FORMAT}
         */
        public static final DTF ISO_ZONED_DATE_TIME = new DTF(ISO_ZONED_DATE_TIME_FORMAT);

        /**
         * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ss'Z'}.
         * @see {@link #ISO_8601_DATE_TIME_FORMAT}
         */
        public static final DTF ISO_8601_DATE_TIME = new DTF(DateTimeUtil.ISO_8601_DATE_TIME_FORMAT);

        /**
         * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}.
         * @see {@link #ISO_8601_TIMESTAMP_FORMAT}
         */
        public static final DTF ISO_8601_TIMESTAMP = new DTF(DateTimeUtil.ISO_8601_TIMESTAMP_FORMAT);

        /**
         * Date/Time format: {@code EEE, dd MMM yyyy HH:mm:ss zzz}.
         * @see {@link #RFC_1123_DATE_TIME_FORMAT}
         */
        public static final DTF RFC_1123_DATE_TIME = new DTF(DateTimeUtil.RFC_1123_DATE_TIME_FORMAT);

        private final String format;
        private final DateTimeFormatter dateTimeFormatter;

        DTF(final String format) {
            this.format = format;
            dateTimeFormatter = DateTimeFormatter.ofPattern(format);
        }

        //    DTF(final DateTimeFormatter dtf) {
        //        this.format = dtf.toString();
        //        this.dtf = dtf;
        //    }
        //
        //    public static DTF of(DateTimeFormatter dtf) {
        //        return new DTF(dtf);
        //    }

        /**
         * Formats the provided TemporalAccessor instance into a string representation.
         *
         * @param temporal The TemporalAccessor instance to format.
         * @return A string representation of the provided TemporalAccessor instance.
         * @throws DateTimeException if an error occurs during formatting.
         * @see {@link DateTimeFormatter#format(TemporalAccessor)}
         */
        public String format(final TemporalAccessor temporal) {
            return dateTimeFormatter.format(temporal);
        }

        /**
         * Formats the provided TemporalAccessor instance into a string representation and appends it to the provided Appendable.
         *
         * @param temporal The TemporalAccessor instance to format.
         * @param appendable The Appendable to which the formatted string will be appended.
         * @throws DateTimeException if an error occurs during formatting.
         * @see {@link DateTimeFormatter#formatTo(TemporalAccessor, Appendable)}
         */
        public void formatTo(final TemporalAccessor temporal, final Appendable appendable) {
            dateTimeFormatter.formatTo(temporal, appendable);
        }

        /**
         * Parses the provided CharSequence into a LocalDate instance.
         *
         * @param text The CharSequence to parse. Must not be {@code null}.
         * @return A LocalDate instance representing the parsed date.
         * @throws DateTimeParseException if the text cannot be parsed to a date.
         * @see Instant#ofEpochMilli(long)
         * @see LocalDate#ofInstant(Instant, ZoneId)
         * @see LocalDate#from(TemporalAccessor)
         */
        public LocalDate parseToLocalDate(final CharSequence text) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            if (isPossibleLong(text)) {
                try {
                    return LocalDate.ofInstant(Instant.ofEpochMilli(Numbers.toLong(text)), DEFAULT_ZONE_ID);
                } catch (final NumberFormatException e) {
                    // ignore;
                    if (logger.isWarnEnabled()) {
                        logger.warn(FAILED_TO_PARSE_TO_LONG, text);
                    }
                }
            }

            return LocalDate.from(parse(text));
        }

        /**
         * Parses the provided CharSequence into a LocalTime instance.
         *
         * @param text The CharSequence to parse. Must not be {@code null}.
         * @return A LocalTime instance representing the parsed time.
         * @throws DateTimeParseException if the text cannot be parsed to a time.
         * @see Instant#ofEpochMilli(long)
         * @see LocalTime#ofInstant(Instant, ZoneId)
         * @see LocalTime#from(TemporalAccessor)
         */
        public LocalTime parseToLocalTime(final CharSequence text) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            if (isPossibleLong(text)) {
                try {
                    return LocalTime.ofInstant(Instant.ofEpochMilli(Numbers.toLong(text)), DEFAULT_ZONE_ID);
                } catch (final NumberFormatException e) {
                    // ignore;
                    if (logger.isWarnEnabled()) {
                        logger.warn(FAILED_TO_PARSE_TO_LONG, text);
                    }
                }
            }

            return LocalTime.from(parse(text));
        }

        /**
         * Parses the provided CharSequence into a LocalDateTime instance.
         *
         * @param text The CharSequence to parse. Must not be {@code null}.
         * @return A LocalDateTime instance representing the parsed date and time.
         * @throws DateTimeParseException if the text cannot be parsed to a date and time.
         * @see Instant#ofEpochMilli(long)
         * @see LocalDateTime#ofInstant(Instant, ZoneId)
         * @see LocalDateTime#from(TemporalAccessor)
         */
        public LocalDateTime parseToLocalDateTime(final CharSequence text) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            if (isPossibleLong(text)) {
                try {
                    return LocalDateTime.ofInstant(Instant.ofEpochMilli(Numbers.toLong(text)), DEFAULT_ZONE_ID);
                } catch (final NumberFormatException e) {
                    // ignore;
                    if (logger.isWarnEnabled()) {
                        logger.warn(FAILED_TO_PARSE_TO_LONG, text);
                    }
                }
            }

            return LocalDateTime.from(parse(text));
        }

        /**
         * Parses the provided CharSequence into an OffsetDateTime instance.
         *
         * @param text The CharSequence to parse. Must not be {@code null}.
         * @return An OffsetDateTime instance representing the parsed date and time.
         * @throws DateTimeParseException if the text cannot be parsed to a date and time.
         * @see Instant#ofEpochMilli(long)
         * @see OffsetDateTime#ofInstant(Instant, ZoneId)
         * @see OffsetDateTime#from(TemporalAccessor
         */
        public OffsetDateTime parseToOffsetDateTime(final CharSequence text) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            if (isPossibleLong(text)) {
                try {
                    return OffsetDateTime.ofInstant(Instant.ofEpochMilli(Numbers.toLong(text)), DEFAULT_ZONE_ID);
                } catch (final NumberFormatException e) {
                    // ignore;
                    if (logger.isWarnEnabled()) {
                        logger.warn(FAILED_TO_PARSE_TO_LONG, text);
                    }
                }
            }

            return OffsetDateTime.from(parse(text));
        }

        /**
         * Parses the provided CharSequence into a ZonedDateTime instance.
         *
         * @param text The CharSequence to parse. Must not be {@code null}.
         * @return A ZonedDateTime instance representing the parsed date and time.
         * @throws DateTimeParseException if the text cannot be parsed to a date and time.
         * @see Instant#ofEpochMilli(long)
         * @see OffsetDateTime#ofInstant(Instant, ZoneId)
         * @see OffsetDateTime#from(TemporalAccessor)
         */
        public ZonedDateTime parseToZonedDateTime(final CharSequence text) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            if (isPossibleLong(text)) {
                try {
                    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(Numbers.toLong(text)), DEFAULT_ZONE_ID);
                } catch (final NumberFormatException e) {
                    // ignore;
                    if (logger.isWarnEnabled()) {
                        logger.warn(FAILED_TO_PARSE_TO_LONG, text);
                    }
                }
            }

            return ZonedDateTime.from(parse(text));
        }

        /**
         * Parses the provided CharSequence into an Instant instance.
         *
         * @param text The CharSequence to parse. Must not be {@code null}.
         * @return An Instant instance representing the parsed date and time.
         * @throws DateTimeParseException if the text cannot be parsed to a date and time.
         * @see Instant#ofEpochMilli(long)
         * @see Instant#from(TemporalAccessor)
         */
        public Instant parseToInstant(final CharSequence text) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            if (isPossibleLong(text)) {
                try {
                    return Instant.ofEpochMilli(Numbers.toLong(text));
                } catch (final NumberFormatException e) {
                    // ignore;
                    if (logger.isWarnEnabled()) {
                        logger.warn(FAILED_TO_PARSE_TO_LONG, text);
                    }
                }
            }

            return Instant.from(parse(text));
        }

        private TemporalAccessor parse(final CharSequence text) {
            final int len = text.length();
            char ch = 0;

            if (len > 25 && text.charAt(len - 1) == ']') {
                return ISO_ZONED_DATE_TIME.dateTimeFormatter.parse(text);
            } else if (len >= 25 && ((ch = text.charAt(len - 5)) == '+' || ch == '-')) {
                return ISO_OFFSET_DATE_TIME.dateTimeFormatter.parse(text);
            } else if (len == 19) {
                if (text.charAt(10) == 'T') {
                    return ISO_LOCAL_DATE_TIME.dateTimeFormatter.parse(text);
                } else {
                    return LOCAL_DATE_TIME.dateTimeFormatter.parse(text);
                }
            } else if (len == 20 && text.charAt(19) == 'Z') {
                return ISO_8601_DATE_TIME.dateTimeFormatter.parse(text);
            } else if (len == 24 && text.charAt(23) == 'Z') {
                return ISO_8601_TIMESTAMP.dateTimeFormatter.parse(text);
            } else if (len == 29 && text.charAt(3) == ',') {
                return RFC_1123_DATE_TIME.dateTimeFormatter.parse(text);
            }

            return dateTimeFormatter.parse(text);
        }

        /**
         * Returns a string representation of the DTF object.
         *
         * @return A string that represents the format of the DTF object.
         */
        @Override
        public String toString() {
            return format;
        }
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

    //    /**
    //     * The Class Times.
    //     */
    //    @Beta
    //    public static final class Times extends DateUtil {
    //
    //        private Times() {
    //            // singleton.
    //        }
    //    }

}
