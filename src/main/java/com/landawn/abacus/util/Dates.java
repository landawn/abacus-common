/*
 * Copyright (C) 2018 HaiYang Li
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

package com.landawn.abacus.util;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.LongFunction;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

/**
 * A comprehensive utility class providing date and time manipulation, parsing, formatting, and conversion operations
 * for both legacy Java date/time types (java.util.Date, Calendar, SQL date types) and modern Java 8+ time types.
 * This class serves as the foundation for date/time operations in the Abacus framework with extensive support
 * for various date formats, time zones, and high-performance operations.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Multi-Format Support:</b> Extensive support for ISO-8601, RFC-1123, local, and custom date formats</li>
 *   <li><b>Time Zone Handling:</b> Comprehensive time zone support with UTC, GMT, and system default zones</li>
 *   <li><b>Type Conversion:</b> Seamless conversion between legacy and modern Java date/time types</li>
 *   <li><b>High Performance:</b> Optimized parsing and formatting with object pooling for thread safety</li>
 *   <li><b>Null Safety:</b> Graceful handling of null inputs with appropriate return values</li>
 *   <li><b>SQL Integration:</b> Native support for SQL Date, Time, and Timestamp types</li>
 *   <li><b>Calendar Operations:</b> Rich set of calendar arithmetic and manipulation operations</li>
 *   <li><b>Fragment Analysis:</b> Detailed time fragment extraction and analysis capabilities</li>
 * </ul>
 *
 * <p><b>Supported Date/Time Types:</b>
 * <ul>
 *   <li><b>Legacy Types:</b> {@code java.util.Date}, {@code java.util.Calendar}, {@code GregorianCalendar}</li>
 *   <li><b>SQL Types:</b> {@code java.sql.Date}, {@code java.sql.Time}, {@code java.sql.Timestamp}</li>
 *   <li><b>XML Types:</b> {@code XMLGregorianCalendar} for XML schema compatibility</li>
 *   <li><b>Modern Types:</b> Integration with {@code LocalDate}, {@code LocalDateTime}, {@code ZonedDateTime}</li>
 *   <li><b>Primitive Types:</b> Long timestamps in milliseconds since epoch</li>
 * </ul>
 *
 * <p><b>Standard Date Format Constants:</b>
 * <ul>
 *   <li><b>Local Formats:</b> {@code LOCAL_DATE_FORMAT} ("yyyy-MM-dd"), {@code LOCAL_TIME_FORMAT} ("HH:mm:ss")</li>
 *   <li><b>ISO Formats:</b> {@code ISO_LOCAL_DATE_TIME_FORMAT} ("yyyy-MM-dd'T'HH:mm:ss")</li>
 *   <li><b>UTC Formats:</b> {@code ISO_8601_DATE_TIME_FORMAT} ("yyyy-MM-dd'T'HH:mm:ss'Z'")</li>
 *   <li><b>RFC Formats:</b> {@code RFC_1123_DATE_TIME_FORMAT} ("EEE, dd MMM yyyy HH:mm:ss zzz")</li>
 *   <li><b>Timestamp Formats:</b> {@code LOCAL_TIMESTAMP_FORMAT} ("yyyy-MM-dd HH:mm:ss.SSS")</li>
 * </ul>
 *
 * <p><b>Time Zone Support:</b>
 * <ul>
 *   <li><b>System Default:</b> {@code DEFAULT_TIME_ZONE} and {@code DEFAULT_ZONE_ID}</li>
 *   <li><b>UTC Support:</b> {@code UTC_TIME_ZONE} and {@code UTC_ZONE_ID} for coordinated universal time</li>
 *   <li><b>GMT Support:</b> {@code GMT_TIME_ZONE} and {@code GMT_ZONE_ID} for Greenwich Mean Time</li>
 *   <li><b>Custom Zones:</b> Support for any valid time zone identifier</li>
 * </ul>
 *
 * <p><b>Core Operation Categories:</b>
 * <ul>
 *   <li><b>Creation:</b> create*, current* methods for object instantiation</li>
 *   <li><b>Parsing:</b> parse* methods with automatic format detection and custom format support</li>
 *   <li><b>Formatting:</b> format* and formatTo* methods for string representation</li>
 *   <li><b>Arithmetic:</b> add*, set*, roll* methods for date/time calculations</li>
 *   <li><b>Rounding:</b> round, truncate, ceiling operations for precision control</li>
 *   <li><b>Comparison:</b> isSame*, truncatedEquals, truncatedCompareTo for date comparisons</li>
 *   <li><b>Fragment Analysis:</b> getFragment* methods for extracting time components</li>
 *   <li><b>Utility Operations:</b> overlap detection, range checking, last date calculations</li>
 * </ul>
 *
 * <p><b>Performance Optimizations:</b>
 * <ul>
 *   <li><b>Object Pooling:</b> Thread-safe pooling of {@code DateFormat} and {@code Calendar} instances</li>
 *   <li><b>Fast Parsing:</b> Optimized parsing for common formats without regex overhead</li>
 *   <li><b>Cache Systems:</b> Cached formatters and creators for frequently used operations</li>
 *   <li><b>Memory Efficiency:</b> Reuse of char arrays and buffers for string operations</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // Current date/time creation
 * Date today = Dates.currentDate();
 * Timestamp now = Dates.currentTimestamp();
 * Calendar calendar = Dates.currentCalendar();
 *
 * // Parsing with automatic format detection
 * Date parsed = Dates.parseDate("2023-12-25");
 * Timestamp ts = Dates.parseTimestamp("2023-12-25 15:30:00.123");
 *
 * // Custom format parsing
 * Date custom = Dates.parseDate("25/12/2023", "dd/MM/yyyy");
 * Date withTZ = Dates.parseDate("2023-12-25 15:30", "yyyy-MM-dd HH:mm", TimeZone.getTimeZone("UTC"));
 *
 * // Formatting operations
 * String formatted = Dates.format(new Date());
 * String custom = Dates.format(new Date(), "dd-MMM-yyyy");
 * String utc = Dates.format(new Date(), Dates.ISO_8601_DATE_TIME_FORMAT, Dates.UTC_TIME_ZONE);
 *
 * // Date arithmetic
 * Date nextWeek = Dates.addDays(today, 7);
 * Date nextMonth = Dates.addMonths(today, 1);
 * Date startOfDay = Dates.truncate(today, Calendar.DAY_OF_MONTH);
 *
 * // Comparisons and checks
 * boolean sameDay = Dates.isSameDay(date1, date2);
 * boolean isLastDay = Dates.isLastDateOfMonth(today);
 * boolean overlaps = Dates.isOverlapping(start1, end1, start2, end2);
 *
 * // SQL type conversions
 * java.sql.Date sqlDate = Dates.createDate(System.currentTimeMillis());
 * java.sql.Time sqlTime = Dates.createTime(calendar);
 * java.sql.Timestamp sqlTimestamp = Dates.createTimestamp(javaUtilDate);
 * }</pre>
 *
 * <p><b>Advanced Operations:</b>
 * <pre>{@code
 * // Calendar field manipulations
 * Calendar cal = Dates.currentCalendar();
 * Calendar rounded = Dates.round(cal, Calendar.HOUR_OF_DAY);
 * Calendar ceiling = Dates.ceiling(cal, Calendar.MINUTE);
 *
 * // Fragment analysis
 * long millisInDay = Dates.getFragmentInMilliseconds(date, CalendarField.DAY_OF_MONTH);
 * long minutesInHour = Dates.getFragmentInMinutes(date, CalendarField.HOUR_OF_DAY);
 *
 * // Time rolling operations (Beta)
 * Date future = Dates.roll(date, 30, TimeUnit.DAYS);
 * Calendar rolledCal = Dates.roll(calendar, 6, CalendarField.MONTH);
 *
 * // Registration of custom creators
 * Dates.registerDateCreator(CustomDate.class, CustomDate::new);
 * Dates.registerCalendarCreator(CustomCalendar.class, (millis, cal) -> new CustomCalendar(millis));
 * }</pre>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Stateless Design:</b> All static methods are stateless and thread-safe</li>
 *   <li><b>Object Pooling:</b> Internal object pools use thread-safe concurrent collections</li>
 *   <li><b>No Shared Mutable State:</b> No static mutable fields that could cause race conditions</li>
 *   <li><b>Concurrent Access:</b> Safe for concurrent access from multiple threads</li>
 * </ul>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li><b>Null Safety:</b> Most methods handle null inputs gracefully, returning null or appropriate defaults</li>
 *   <li><b>ParseException Handling:</b> Parsing methods catch exceptions and return null for invalid input</li>
 *   <li><b>IllegalArgumentException:</b> Thrown for invalid parameters that violate method contracts</li>
 *   <li><b>Logging:</b> Internal operations are logged for debugging and monitoring purposes</li>
 * </ul>
 *
 * <p><b>Memory Management:</b>
 * <ul>
 *   <li><b>Object Pooling:</b> Reuses expensive objects like DateFormat and Calendar instances</li>
 *   <li><b>Pool Size Control:</b> Configurable pool sizes based on system capacity</li>
 *   <li><b>Automatic Cleanup:</b> Pools automatically manage object lifecycle</li>
 *   <li><b>Memory Efficient:</b> Minimizes object allocation in high-frequency operations</li>
 * </ul>
 *
 * <p><b>Integration with Java Time API:</b>
 * <ul>
 *   <li><b>Conversion Support:</b> Methods for converting between legacy and modern types</li>
 *   <li><b>DateTimeFormatter Integration:</b> Compatible with Java 8+ formatter patterns</li>
 *   <li><b>ZoneId Support:</b> Seamless integration with modern time zone handling</li>
 *   <li><b>Temporal Accessor:</b> Support for parsing to TemporalAccessor types</li>
 * </ul>
 *
 * <p><b>XML and Web Service Integration:</b>
 * <ul>
 *   <li><b>XMLGregorianCalendar:</b> Full support for XML Schema date/time types</li>
 *   <li><b>ISO-8601 Compliance:</b> Standard formats for web service interoperability</li>
 *   <li><b>RFC-1123 Support:</b> HTTP date header format support</li>
 *   <li><b>Timezone Serialization:</b> Proper handling of timezone information in XML</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use standard format constants when possible for consistency and performance</li>
 *   <li>Specify explicit time zones when working with distributed systems</li>
 *   <li>Prefer the modern Java Time API for new code, use these utilities for legacy integration</li>
 *   <li>Use the parsing methods with automatic format detection for user input</li>
 *   <li>Leverage the object pooling by reusing format strings and time zones</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Parsing:</b> O(1) for fast formats, O(n) for complex patterns</li>
 *   <li><b>Formatting:</b> O(1) for cached formatters, minimal string allocation</li>
 *   <li><b>Arithmetic:</b> O(1) for most operations, calendar field dependent</li>
 *   <li><b>Comparison:</b> O(1) for instant comparisons, O(1) for truncated comparisons</li>
 * </ul>
 *
 * <p><b>Related Classes:</b>
 * <ul>
 *   <li><b>{@link DateTimeFormatter}:</b> Java 8+ date/time formatting</li>
 *   <li><b>{@link ISO8601Util}:</b> Specialized ISO-8601 utilities</li>
 *   <li><b>{@link CalendarField}:</b> Type-safe calendar field enumeration</li>
 *   <li><b>{@link java.time.LocalDateTime}:</b> Modern local date/time representation</li>
 *   <li><b>{@link java.time.ZonedDateTime}:</b> Modern zoned date/time representation</li>
 * </ul>
 *
 * <p><b>Extension Points:</b>
 * <ul>
 *   <li><b>Custom Date Types:</b> Register custom date class creators</li>
 *   <li><b>Custom Calendar Types:</b> Register custom calendar class creators</li>
 *   <li><b>Format Extensions:</b> Add support for additional date formats</li>
 *   <li><b>Time Zone Extensions:</b> Custom time zone handling logic</li>
 * </ul>
 *
 * <p><b>Migration Guide:</b>
 * <ul>
 *   <li>Legacy {@code SimpleDateFormat} → Use {@code Dates.format()} methods</li>
 *   <li>Manual date arithmetic → Use {@code add*()}, {@code set*()}, {@code roll*()} methods</li>
 *   <li>Custom parsing logic → Use {@code parse*()} methods with format detection</li>
 *   <li>Time zone conversions → Use methods with explicit {@code TimeZone} parameters</li>
 * </ul>
 *
 * <p><b>Attribution:</b>
 * This class includes code adapted from Apache Commons Lang, Google Guava, and other
 * open source projects under the Apache License 2.0. Methods from these libraries may have been
 * modified for consistency, performance optimization, and null-safety enhancement.
 *
 * @see Comparable
 * @see DateTimeFormatter
 * @see ISO8601Util
 * @see CalendarField
 * @see java.util.Date
 * @see java.util.Calendar
 * @see java.util.TimeZone
 * @see java.util.concurrent.TimeUnit
 * @see java.sql.Date
 * @see java.sql.Time
 * @see java.sql.Timestamp
 * @see java.time.LocalDateTime
 * @see java.time.ZonedDateTime
 * @see java.time.ZoneId
 * @see java.time.Duration
 * @see java.time.temporal.Temporal
 * @see java.time.temporal.TemporalAmount
 * @see java.time.temporal.TemporalUnit
 * @see java.time.temporal.ChronoUnit
 * @see javax.xml.datatype.XMLGregorianCalendar
 */
@SuppressWarnings({ "java:S1694", "java:S2143" })
public abstract sealed class Dates permits Dates.DateUtil {
    private static final Logger logger = LoggerFactory.getLogger(Dates.class);

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
     * GMT, or Greenwich Mean Time, is a time zone used as a reference point for time keeping around the world.
     * It is located at the prime meridian (0 degrees longitude) and does not observe daylight-saving time.
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
     * {@code ZoneId} of UTC time zone.
     * @see #UTC_TIME_ZONE
     * @see TimeZone#toZoneId()
     */
    public static final ZoneId UTC_ZONE_ID = UTC_TIME_ZONE.toZoneId();

    /**
     * {@code ZoneId} of GMT time zone.
     * @see #GMT_TIME_ZONE
     * @see TimeZone#toZoneId()
     */
    public static final ZoneId GMT_ZONE_ID = GMT_TIME_ZONE.toZoneId();

    /**
     * Date/Time format: {@code yyyy}
     *
     * <p>Year-only format. Useful for grouping or filtering by year.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String yearStr = "2023";
     * Date date = Dates.parseDate(yearStr, Dates.LOCAL_YEAR_FORMAT);
     * // Represents January 1, 2023
     *
     * Date today = new Date();
     * String year = Dates.format(today, Dates.LOCAL_YEAR_FORMAT);
     * // Result: "2023"
     *
     * Calendar calendar = Calendar.getInstance();
     * String formattedYear = Dates.format(calendar, Dates.LOCAL_YEAR_FORMAT);
     * }</pre>
     */
    public static final String LOCAL_YEAR_FORMAT = "yyyy";

    /**
     * Date/Time format: {@code MM-dd}
     *
     * <p>Month and day format without year. Useful for recurring dates, anniversaries, and seasonal data.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String monthDayStr = "12-25";
     * Date date = Dates.parseDate(monthDayStr, Dates.LOCAL_MONTH_DAY_FORMAT);
     * // Represents December 25th of the current year
     *
     * Date birthday = new Date();
     * String monthDay = Dates.format(birthday, Dates.LOCAL_MONTH_DAY_FORMAT);
     * // Result: "12-25" for December 25th
     *
     * // Useful for anniversaries without year
     * Calendar anniversary = Calendar.getInstance();
     * String anniversaryStr = Dates.format(anniversary, Dates.LOCAL_MONTH_DAY_FORMAT);
     * }</pre>
     */
    public static final String LOCAL_MONTH_DAY_FORMAT = "MM-dd";

    // static final String LOCAL_MONTH_DAY_FORMAT_SLASH = "MM/dd";

    /**
     * Date/Time format: {@code yyyy-MM-dd}
     *
     * <p>ISO 8601 date format without time. Standard format for representing dates in data exchange and databases.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String dateStr = "2023-12-25";
     * Date date = Dates.parseDate(dateStr, Dates.LOCAL_DATE_FORMAT);
     *
     * Date today = new Date();
     * String formatted = Dates.format(today, Dates.LOCAL_DATE_FORMAT);
     * // Result: "2023-12-25"
     *
     * java.util.Calendar calendar = java.util.Calendar.getInstance();
     * String calendarDate = Dates.format(calendar, Dates.LOCAL_DATE_FORMAT);
     *
     * // Using DTF instance for convenience
     * String dtfFormatted = DTF.LOCAL_DATE.format(today);
     * }</pre>
     */
    public static final String LOCAL_DATE_FORMAT = "yyyy-MM-dd";

    // static final String LOCAL_DATE_FORMAT_SLASH = "yyyy/MM/dd";

    /**
     * Date/Time format: {@code HH:mm:ss}
     *
     * <p>Time-only format in 24-hour notation. Useful for representing times without date context.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String timeStr = "14:30:45";
     * Date date = Dates.parseDate(timeStr, Dates.LOCAL_TIME_FORMAT);
     *
     * Date now = new Date();
     * String formatted = Dates.format(now, Dates.LOCAL_TIME_FORMAT);
     * // Result: "14:30:45"
     *
     * java.util.Calendar calendar = java.util.Calendar.getInstance();
     * String calendarTime = Dates.format(calendar, Dates.LOCAL_TIME_FORMAT);
     *
     * // Using DTF instance
     * String dtfFormatted = DTF.LOCAL_TIME.format(now);
     * }</pre>
     */
    public static final String LOCAL_TIME_FORMAT = "HH:mm:ss";

    /**
     * Date/Time format: {@code yyyy-MM-dd HH:mm:ss}
     *
     * <p>Local date and time format without timezone. Useful for database timestamps and local system times.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String dateTimeStr = "2023-12-25 14:30:45";
     * Date date = Dates.parseDate(dateTimeStr, Dates.LOCAL_DATE_TIME_FORMAT);
     *
     * Date now = new Date();
     * String formatted = Dates.format(now, Dates.LOCAL_DATE_TIME_FORMAT);
     * // Result: "2023-12-25 14:30:45"
     *
     * java.util.Calendar calendar = java.util.Calendar.getInstance();
     * String calendarDateTime = Dates.format(calendar, Dates.LOCAL_DATE_TIME_FORMAT);
     *
     * // Using DTF instance
     * String dtfFormatted = DTF.LOCAL_DATE_TIME.format(now);
     * }</pre>
     */
    public static final String LOCAL_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * Date/Time format: {@code yyyy-MM-dd HH:mm:ss.SSS}
     *
     * <p>Local date and time format with milliseconds. Provides high precision for local timestamps,
     * commonly used in application logging and database records.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String timestampStr = "2023-12-25 14:30:45.123";
     * Date date = Dates.parseDate(timestampStr, Dates.LOCAL_TIMESTAMP_FORMAT);
     *
     * long currentTimeMillis = System.currentTimeMillis();
     * Date now = new Date(currentTimeMillis);
     * String formatted = Dates.format(now, Dates.LOCAL_TIMESTAMP_FORMAT);
     * // Result: "2023-12-25 14:30:45.123"
     *
     * java.util.Calendar calendar = java.util.Calendar.getInstance();
     * String calendarTimestamp = Dates.format(calendar, Dates.LOCAL_TIMESTAMP_FORMAT);
     *
     * // High precision logging
     * System.out.println("Event occurred at: " + Dates.format(new Date(), Dates.LOCAL_TIMESTAMP_FORMAT));
     * }</pre>
     */
    public static final String LOCAL_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

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
     *
     * <p>ISO 8601 local date and time format without timezone. Standard format for data interchange
     * where the timezone is implicit or handled separately.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String isoStr = "2023-12-25T14:30:45";
     * Date date = Dates.parseDate(isoStr, Dates.ISO_LOCAL_DATE_TIME_FORMAT);
     *
     * Date now = new Date();
     * String formatted = Dates.format(now, Dates.ISO_LOCAL_DATE_TIME_FORMAT);
     * // Result: "2023-12-25T14:30:45"
     *
     * // Using DTF instance for convenience
     * String dtfFormatted = DTF.ISO_LOCAL_DATE_TIME.format(now);
     *
     * java.time.LocalDateTime localDateTime = java.time.LocalDateTime.now();
     * String isoFormatted = Dates.format(localDateTime, Dates.ISO_LOCAL_DATE_TIME_FORMAT);
     * }</pre>
     *
     * @see DateTimeFormatter#ISO_LOCAL_DATE_TIME
     */
    public static final String ISO_LOCAL_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ssXXX}
     *
     * <p>ISO 8601 date and time with UTC offset. Useful for APIs and services that need to represent
     * a moment in time with explicit offset but without timezone identification.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String offsetStr = "2023-12-25T14:30:45+05:30";
     * Date date = Dates.parseDate(offsetStr, Dates.ISO_OFFSET_DATE_TIME_FORMAT);
     *
     * // Using DTF instance with OffsetDateTime
     * java.time.OffsetDateTime offsetDT = java.time.OffsetDateTime.now();
     * String formatted = DTF.ISO_OFFSET_DATE_TIME.format(offsetDT);
     * // Result: "2023-12-25T14:30:45+05:30"
     *
     * // Parse with offset
     * String utcStr = "2023-12-25T14:30:45Z";
     * java.time.OffsetDateTime parsed = Dates.parseOffsetDateTime(utcStr);
     * }</pre>
     *
     * @see DateTimeFormatter#ISO_OFFSET_DATE_TIME
     */
    public static final String ISO_OFFSET_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

    /**
     * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ss'Z'}.
     *
     * <p>ISO 8601 UTC date and time format (Zulu time). This is the default date/time format
     * for UTC representation. Widely used in APIs, logs, and communication protocols.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String utcStr = "2023-12-25T14:30:45Z";
     * Date date = Dates.parseDate(utcStr, Dates.ISO_8601_DATE_TIME_FORMAT);
     *
     * Date now = new Date();
     * String formatted = Dates.format(now, Dates.ISO_8601_DATE_TIME_FORMAT);
     * // Result: "2023-12-25T14:30:45Z"
     *
     * // Using DTF instance
     * String dtfFormatted = DTF.ISO_8601_DATE_TIME.format(java.time.Instant.now());
     *
     * java.time.ZonedDateTime utcTime = java.time.ZonedDateTime.now(java.time.ZoneId.of("UTC"));
     * String utcFormatted = Dates.format(utcTime, Dates.ISO_8601_DATE_TIME_FORMAT);
     * }</pre>
     */
    public static final String ISO_8601_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    // static final String ISO_8601_DATE_TIME_FORMAT_SLASH = "yyyy/MM/dd'T'HH:mm:ss'Z'";

    /**
     * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}.
     *
     * <p>ISO 8601 UTC timestamp format with milliseconds. This is the default timestamp format
     * for high-precision UTC representation. Ideal for precise logging, monitoring, and distributed systems.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String timestampStr = "2023-12-25T14:30:45.123Z";
     * Date date = Dates.parseDate(timestampStr, Dates.ISO_8601_TIMESTAMP_FORMAT);
     *
     * long currentTimeMillis = System.currentTimeMillis();
     * Date now = new Date(currentTimeMillis);
     * String formatted = Dates.format(now, Dates.ISO_8601_TIMESTAMP_FORMAT);
     * // Result: "2023-12-25T14:30:45.123Z"
     *
     * // Using DTF instance for high precision
     * String dtfFormatted = DTF.ISO_8601_TIMESTAMP.format(java.time.Instant.now());
     *
     * // Default format for system logging
     * System.out.println("Server started at: " + Dates.format(new Date(), Dates.ISO_8601_TIMESTAMP_FORMAT));
     * }</pre>
     */
    public static final String ISO_8601_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    // static final String ISO_8601_TIMESTAMP_FORMAT_SLASH = "yyyy/MM/dd'T'HH:mm:ss.SSS'Z'";

    /**
     * This constant defines the date format specified by RFC 1123 / RFC 822.
     *
     * <p>RFC 1123 date format (HTTP date format). Human-readable format used in HTTP headers,
     * email messages, and other internet protocols. Format includes day of week, abbreviated month,
     * and timezone abbreviation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String rfcStr = "Mon, 25 Dec 2023 14:30:45 GMT";
     * Date date = Dates.parseDate(rfcStr, Dates.RFC_1123_DATE_TIME_FORMAT);
     *
     * Date now = new Date();
     * String formatted = Dates.format(now, Dates.RFC_1123_DATE_TIME_FORMAT);
     * // Result: "Mon, 25 Dec 2023 14:30:45 GMT"
     *
     * // Using DTF instance for HTTP headers
     * java.util.Calendar calendar = java.util.Calendar.getInstance();
     * String httpDate = DTF.RFC_1123_DATE_TIME.format(calendar);
     *
     * // Set HTTP header with RFC 1123 date
     * String lastModified = Dates.format(fileLastModified, Dates.RFC_1123_DATE_TIME_FORMAT);
     * // Can be used in: Last-Modified, Date, Expires headers
     * }</pre>
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

    static final Map<Class<? extends java.util.Date>, LongFunction<? extends java.util.Date>> dateCreatorPool = new ConcurrentHashMap<>();

    static {
        dateCreatorPool.put(java.util.Date.class, java.util.Date::new);
        dateCreatorPool.put(java.sql.Date.class, java.sql.Date::new);
        dateCreatorPool.put(Time.class, Time::new);
        dateCreatorPool.put(Timestamp.class, Timestamp::new);
    }

    static final Map<Class<? extends java.util.Calendar>, BiFunction<? super Long, ? super Calendar, ? extends java.util.Calendar>> calendarCreatorPool = new ConcurrentHashMap<>();

    static {
        calendarCreatorPool.put(java.util.Calendar.class, (millis, c) -> {
            final Calendar ret = Calendar.getInstance();

            if (!N.equals(ret.getTimeZone(), c.getTimeZone()) && c.getTimeZone() != null) {
                ret.setTimeZone(c.getTimeZone());
            }

            ret.setTimeInMillis(millis);

            return ret;
        });

        calendarCreatorPool.put(java.util.GregorianCalendar.class, (millis, c) -> {
            final Calendar ret = GregorianCalendar.getInstance();

            if (!N.equals(ret.getTimeZone(), c.getTimeZone()) && c.getTimeZone() != null) {
                ret.setTimeZone(c.getTimeZone());
            }

            ret.setTimeInMillis(millis);

            return ret;
        });
    }

    Dates() {
        // singleton
    }

    /**
     * Registers a custom date creator for a specific date class.
     *
     * This method implements a pluggable factory pattern that allows third-party date classes to be created by the Dates utility.
     * The date creator function must accept one argument: the time in milliseconds since the epoch (01-01-1970).
     *
     * <p>Only custom date classes (those not in the {@code java.*}, {@code javax.*}, or {@code com.landawn.abacus.*} packages)
     * can be registered. Built-in classes cannot be overridden for security and stability reasons.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Custom date class
     * class CustomDate extends java.util.Date {
     *     public CustomDate(long timeInMillis) {
     *         super(timeInMillis);
     *     }
     * }
     *
     * // Register custom creator with LongFunction signature
     * LongFunction<CustomDate> creator = CustomDate::new;
     * boolean registered = Dates.registerDateCreator(CustomDate.class, creator);
     *
     * if (registered) {
     *     System.out.println("CustomDate creator registered successfully");
     * } else {
     *     System.out.println("Failed to register CustomDate creator");
     * }
     * }</pre>
     *
     * @param <T> the type of the date class extending {@code java.util.Date}.
     * @param dateClass the class of the date to register the creator for, must not be in restricted packages.
     * @param dateCreator the function that creates instances of the date class, taking one argument:
     *        the time in milliseconds. Not {@code null}.
     * @return {@code true} if the date creator was successfully registered, {@code false} if the class was already registered
     *         or is from a restricted package ({@code java.*}, {@code javax.*}, or {@code com.landawn.abacus.*}).
     * @throws IllegalArgumentException if dateClass or dateCreator is {@code null}.
     * @see #registerCalendarCreator(Class, BiFunction)
     */
    public static <T extends java.util.Date> boolean registerDateCreator(final Class<? extends T> dateClass, final LongFunction<? extends T> dateCreator) {
        if (dateCreatorPool.containsKey(dateClass) || Strings.startsWithAny(ClassUtil.getPackageName(dateClass), "java.", "javax.", "com.landawn.abacus")) {
            return false;
        }

        dateCreatorPool.put(dateClass, dateCreator);

        return true;
    }

    /**
     * Registers a custom calendar creator for a specific calendar class.
     *
     * This method implements a pluggable factory pattern that allows third-party calendar classes to be created by the Dates utility.
     * The calendar creator function must accept two arguments: the time in milliseconds since the epoch (01-01-1970) and a
     * {@code Calendar} instance to use as a template.
     *
     * <p>Only custom calendar classes (those not in the {@code java.*}, {@code javax.*}, or {@code com.landawn.abacus.*} packages)
     * can be registered. Built-in classes cannot be overridden for security and stability reasons.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Custom calendar class
     * class CustomCalendar extends java.util.GregorianCalendar {
     *     public CustomCalendar(long timeInMillis, Calendar template) {
     *         super();
     *         this.setTimeInMillis(timeInMillis);
     *     }
     * }
     *
     * // Register custom creator with BiFunction signature
     * BiFunction<Long, Calendar, CustomCalendar> creator =
     *     (time, template) -> new CustomCalendar(time, template);
     * boolean registered = Dates.registerCalendarCreator(CustomCalendar.class, creator);
     *
     * if (registered) {
     *     System.out.println("CustomCalendar creator registered successfully");
     * } else {
     *     System.out.println("Failed to register CustomCalendar creator");
     * }
     * }</pre>
     *
     * @param <T> the type of the calendar class extending {@code java.util.Calendar}.
     * @param calendarClass the class of the calendar to register the creator for, must not be in restricted packages.
     * @param dateCreator the function that creates instances of the calendar class, taking two arguments:
     *        the time in milliseconds and a {@code Calendar} template. Not {@code null}.
     * @return {@code true} if the calendar creator was successfully registered, {@code false} if the class was already registered
     *         or is from a restricted package ({@code java.*}, {@code javax.*}, or {@code com.landawn.abacus.*}).
     * @throws IllegalArgumentException if calendarClass or calendarCreator is {@code null}.
     * @see #registerDateCreator(Class, LongFunction)
     */
    public static <T extends java.util.Calendar> boolean registerCalendarCreator(final Class<? extends T> calendarClass,
            final BiFunction<? super Long, ? super Calendar, ? extends T> dateCreator) {
        if (calendarCreatorPool.containsKey(calendarClass)
                || Strings.startsWithAny(ClassUtil.getPackageName(calendarClass), "java.", "javax.", "com.landawn.abacus")) {
            return false;
        }

        calendarCreatorPool.put(calendarClass, dateCreator);

        return true;
    }

    /**
     * Returns the current time in milliseconds.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long currentMillis = Dates.currentTimeMillis();
     * Date date = new Date(currentMillis);
     * }</pre>
     *
     * @return the current time in milliseconds since the epoch (01-01-1970).
     */
    @Beta
    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Returns a new instance of {@code java.sql.Time} based on the current time in the default time zone with the default locale.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Time currentTime = Dates.currentTime();   // e.g., 14:30:45
     * }</pre>
     *
     * @return a new {@code java.sql.Time} instance representing the current time.
     */
    public static Time currentTime() {
        return new Time(System.currentTimeMillis());
    }

    /**
     * Returns a new instance of {@code java.sql.Date} based on the current time in the default time zone with the default locale.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date currentDate = Dates.currentDate();   // e.g., 2025-10-04
     * }</pre>
     *
     * @return a new {@code java.sql.Date} instance representing the current date.
     */
    public static Date currentDate() {
        return new Date(System.currentTimeMillis());
    }

    /**
     * Returns a new instance of {@code java.sql.Timestamp} based on the current time in the default time zone with the default locale.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Timestamp currentTimestamp = Dates.currentTimestamp();   // e.g., 2025-10-04 14:30:45.123
     * }</pre>
     *
     * @return a new {@code java.sql.Timestamp} instance representing the current date and time with millisecond precision.
     */
    public static Timestamp currentTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    /**
     * Returns a new instance of {@code java.util.Date} based on the current time in the default time zone with the default locale.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date currentDate = Dates.currentJUDate();   // e.g., Sat Oct 04 14:30:45 UTC 2025
     * }</pre>
     *
     * @return a new {@code java.util.Date} instance representing the current date and time.
     */
    public static java.util.Date currentJUDate() {
        return new java.util.Date();
    }

    /**
     * Returns a new instance of {@code java.util.Calendar} based on the current time in the default time zone with the default locale.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.currentCalendar();
     * int year = cal.get(Calendar.YEAR);   // e.g., 2025
     * }</pre>
     *
     * @return a new {@code Calendar} instance representing the current date and time.
     */
    public static Calendar currentCalendar() {
        return Calendar.getInstance();
    }

    /**
     * Returns a new instance of {@code GregorianCalendar} based on the current time in the default time zone with the default locale.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GregorianCalendar cal = Dates.currentGregorianCalendar();
     * int year = cal.get(Calendar.YEAR);   // e.g., 2025
     * }</pre>
     *
     * @return a new {@code GregorianCalendar} instance representing the current date and time.
     */
    public static GregorianCalendar currentGregorianCalendar() {
        return new GregorianCalendar();
    }

    /**
     * Returns a new instance of {@code XMLGregorianCalendar} based on the current Gregorian Calendar.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar xmlCal = Dates.currentXMLGregorianCalendar();
     * int year = xmlCal.getYear();   // e.g., 2025
     * }</pre>
     *
     * @return a new {@code XMLGregorianCalendar} instance representing the current date and time.
     */
    public static XMLGregorianCalendar currentXMLGregorianCalendar() {
        return dataTypeFactory.newXMLGregorianCalendar(currentGregorianCalendar());
    }

    /**
     * Calculates the current time in milliseconds with the specified time amount added or subtracted.
     * This method adds or subtracts the given amount in the specified time unit to the current system time.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long futureTime = Dates.currentTimeMillisPlus(5, TimeUnit.MINUTES);   // 5 minutes from now
     * long pastTime = Dates.currentTimeMillisPlus(-2, TimeUnit.HOURS);      // 2 hours ago
     * }</pre>
     *
     * @param amount the amount of time to add (positive) or subtract (negative).
     * @param unit the time unit of the amount parameter (e.g., TimeUnit.SECONDS, TimeUnit.MINUTES).
     * @return the current time in milliseconds with the specified amount applied.
     */
    @Beta
    static long currentTimeMillisPlus(final long amount, final TimeUnit unit) {
        return System.currentTimeMillis() + unit.toMillis(amount);
    }

    /**
     * Returns a new {@code java.sql.Time} instance representing the current time with the specified time amount added or subtracted.
     * This method creates a new Time object by applying the given amount in the specified time unit to the current system time.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Time futureTime = Dates.currentTimePlus(30, TimeUnit.MINUTES);   // Current time + 30 minutes
     * Time pastTime = Dates.currentTimePlus(-1, TimeUnit.HOURS);       // Current time - 1 hour
     * }</pre>
     *
     * @param amount the amount of time to add (positive) or subtract (negative).
     * @param unit the time unit of the amount parameter (e.g., TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS).
     * @return a new {@code java.sql.Time} instance representing the current time with the specified amount applied.
     */
    @Beta
    public static Time currentTimePlus(final long amount, final TimeUnit unit) {
        return new Time(currentTimeMillisPlus(amount, unit));
    }

    /**
     * Returns a new {@code java.sql.Date} instance representing the current date with the specified time amount added or subtracted.
     * This method creates a new Date object by applying the given amount in the specified time unit to the current system time.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date tomorrow = Dates.currentDatePlus(1, TimeUnit.DAYS);    // Current date + 1 day
     * Date lastWeek = Dates.currentDatePlus(-7, TimeUnit.DAYS);   // Current date - 7 days
     * }</pre>
     *
     * @param amount the amount of time to add (positive) or subtract (negative).
     * @param unit the time unit of the amount parameter (e.g., TimeUnit.DAYS, TimeUnit.HOURS).
     * @return a new {@code java.sql.Date} instance representing the current date with the specified amount applied.
     */
    @Beta
    public static Date currentDatePlus(final long amount, final TimeUnit unit) {
        return new Date(currentTimeMillisPlus(amount, unit));
    }

    /**
     * Returns a new {@code java.sql.Timestamp} instance representing the current timestamp with the specified time amount added or subtracted.
     * This method creates a new Timestamp object by applying the given amount in the specified time unit to the current system time.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Timestamp future = Dates.currentTimestampPlus(5, TimeUnit.MINUTES);   // Current timestamp + 5 minutes
     * Timestamp past = Dates.currentTimestampPlus(-3, TimeUnit.HOURS);      // Current timestamp - 3 hours
     * }</pre>
     *
     * @param amount the amount of time to add (positive) or subtract (negative).
     * @param unit the time unit of the amount parameter (e.g., TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS).
     * @return a new {@code java.sql.Timestamp} instance representing the current timestamp with the specified amount applied.
     */
    @Beta
    public static Timestamp currentTimestampPlus(final long amount, final TimeUnit unit) {
        return new Timestamp(currentTimeMillisPlus(amount, unit));
    }

    /**
     * Returns a new {@code java.util.Date} instance representing the current date/time with the specified time amount added or subtracted.
     * This method creates a new Date object by applying the given amount in the specified time unit to the current system time.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date future = Dates.currentJUDatePlus(2, TimeUnit.DAYS);   // Current date + 2 days
     * Date past = Dates.currentJUDatePlus(-1, TimeUnit.WEEKS);   // Current date - 1 week
     * }</pre>
     *
     * @param amount the amount of time to add (positive) or subtract (negative).
     * @param unit the time unit of the amount parameter (e.g., TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS).
     * @return a new {@code java.util.Date} instance representing the current date/time with the specified amount applied.
     */
    @Beta
    public static java.util.Date currentJUDatePlus(final long amount, final TimeUnit unit) {
        return new java.util.Date(currentTimeMillisPlus(amount, unit));
    }

    /**
     * Returns a new {@code java.util.Calendar} instance representing the current date/time with the specified time amount added or subtracted.
     * This method creates a new Calendar object by applying the given amount in the specified time unit to the current system time.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar future = Dates.currentCalendarPlus(3, TimeUnit.HOURS);   // Current time + 3 hours
     * Calendar past = Dates.currentCalendarPlus(-10, TimeUnit.DAYS);    // Current time - 10 days
     * }</pre>
     *
     * @param amount the amount of time to add (positive) or subtract (negative).
     * @param unit the time unit of the amount parameter (e.g., TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS).
     * @return a new {@code java.util.Calendar} instance representing the current date/time with the specified amount applied.
     */
    @Beta
    public static Calendar currentCalendarPlus(final long amount, final TimeUnit unit) {
        final Calendar ret = Calendar.getInstance();
        ret.setTimeInMillis(currentTimeMillisPlus(amount, unit));
        return ret;
    }

    /**
     * Creates a new instance of {@code java.util.Date} based on the provided calendar's time value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * Date date = Dates.createJUDate(cal);   // e.g., Sat Oct 22 14:30:45 UTC 2025
     * }</pre>
     *
     * @param calendar the calendar providing the time value, not {@code null}.
     * @return a new {@code java.util.Date} instance representing the same point in time.
     * @throws IllegalArgumentException if calendar is {@code null}.
     * @see #createJUDate(java.util.Date)
     * @see #createJUDate(long)
     */
    public static java.util.Date createJUDate(final Calendar calendar) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createJUDate(calendar.getTimeInMillis());
    }

    /**
     * Creates a new instance of {@code java.util.Date} based on the provided date's time value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date original = new Date();
     * Date copy = Dates.createJUDate(original);   // Creates a new instance with same time
     * }</pre>
     *
     * @param date the date providing the time value, not {@code null}.
     * @return a new {@code java.util.Date} instance representing the same point in time.
     * @throws IllegalArgumentException if date is {@code null}.
     * @see #createJUDate(Calendar)
     * @see #createJUDate(long)
     */
    public static java.util.Date createJUDate(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return createJUDate(date.getTime());
    }

    /**
     * Creates a new instance of {@code java.util.Date} based on the provided time in milliseconds.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long millis = System.currentTimeMillis();
     * Date date = Dates.createJUDate(millis);   // Creates date from epoch milliseconds
     * }</pre>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @return a new {@code java.util.Date} instance representing the specified point in time.
     * @see #createJUDate(Calendar)
     * @see #createJUDate(java.util.Date)
     * @see #createDate(long)
     * @see #createTime(long)
     * @see #createTimestamp(long)
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
     * Creates a new instance of {@code java.sql.Date} based on the provided calendar's time value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * Date sqlDate = Dates.createDate(cal);   // e.g., 2025-10-22
     * }</pre>
     *
     * @param calendar the calendar providing the time value, not {@code null}.
     * @return a new {@code java.sql.Date} instance representing the same point in time.
     * @throws IllegalArgumentException if calendar is {@code null}.
     * @see #createDate(java.util.Date)
     * @see #createDate(long)
     * @see #createJUDate(Calendar)
     */
    public static Date createDate(final Calendar calendar) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createDate(calendar.getTimeInMillis());
    }

    /**
     * Creates a new instance of {@code java.sql.Date} based on the provided date's time value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date utilDate = new Date();
     * Date sqlDate = Dates.createDate(utilDate);   // Converts to java.sql.Date
     * }</pre>
     *
     * @param date the date providing the time value, not {@code null}.
     * @return a new {@code java.sql.Date} instance representing the same point in time.
     * @throws IllegalArgumentException if date is {@code null}.
     * @see #createDate(Calendar)
     * @see #createDate(long)
     * @see #createJUDate(java.util.Date)
     */
    public static Date createDate(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return createDate(date.getTime());
    }

    /**
     * Creates a new instance of {@code java.sql.Date} based on the provided time in milliseconds.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long millis = System.currentTimeMillis();
     * Date sqlDate = Dates.createDate(millis);   // e.g., 2025-10-22
     * }</pre>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @return a new {@code java.sql.Date} instance representing the specified point in time.
     * @see #createDate(Calendar)
     * @see #createDate(java.util.Date)
     * @see #createJUDate(long)
     * @see #createTime(long)
     * @see #createTimestamp(long)
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
     * Creates a new instance of {@code java.sql.Time} based on the provided calendar's time value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * Time time = Dates.createTime(cal);   // e.g., 14:30:45
     * }</pre>
     *
     * @param calendar the calendar providing the time value, not {@code null}.
     * @return a new {@code java.sql.Time} instance representing the same point in time.
     * @throws IllegalArgumentException if calendar is {@code null}.
     * @see #createTime(java.util.Date)
     * @see #createTime(long)
     * @see #createDate(Calendar)
     */
    public static Time createTime(final Calendar calendar) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createTime(calendar.getTimeInMillis());
    }

    /**
     * Creates a new instance of {@code java.sql.Time} based on the provided date's time value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date utilDate = new Date();
     * Time time = Dates.createTime(utilDate);   // Converts to java.sql.Time
     * }</pre>
     *
     * @param date the date providing the time value, not {@code null}.
     * @return a new {@code java.sql.Time} instance representing the same point in time.
     * @throws IllegalArgumentException if date is {@code null}.
     * @see #createTime(Calendar)
     * @see #createTime(long)
     * @see #createDate(java.util.Date)
     */
    public static Time createTime(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return createTime(date.getTime());
    }

    /**
     * Creates a new instance of {@code java.sql.Time} based on the provided time in milliseconds.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long millis = System.currentTimeMillis();
     * Time time = Dates.createTime(millis);   // e.g., 14:30:45
     * }</pre>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @return a new {@code java.sql.Time} instance representing the specified point in time.
     * @see #createTime(Calendar)
     * @see #createTime(java.util.Date)
     * @see #createDate(long)
     * @see #createTimestamp(long)
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
     * Creates a new instance of {@code java.sql.Timestamp} based on the provided calendar's time value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * Timestamp timestamp = Dates.createTimestamp(cal);   // e.g., 2025-10-22 14:30:45.123
     * }</pre>
     *
     * @param calendar the calendar providing the time value, not {@code null}.
     * @return a new {@code java.sql.Timestamp} instance representing the same point in time.
     * @throws IllegalArgumentException if calendar is {@code null}.
     * @see #createTimestamp(java.util.Date)
     * @see #createTimestamp(long)
     * @see #createTime(Calendar)
     */
    public static Timestamp createTimestamp(final Calendar calendar) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createTimestamp(calendar.getTimeInMillis());
    }

    /**
     * Creates a new instance of {@code java.sql.Timestamp} based on the provided date's time value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date utilDate = new Date();
     * Timestamp timestamp = Dates.createTimestamp(utilDate);   // Converts to java.sql.Timestamp
     * }</pre>
     *
     * @param date the date providing the time value, not {@code null}.
     * @return a new {@code java.sql.Timestamp} instance representing the same point in time.
     * @throws IllegalArgumentException if date is {@code null}.
     * @see #createTimestamp(Calendar)
     * @see #createTimestamp(long)
     * @see #createTime(java.util.Date)
     */
    public static Timestamp createTimestamp(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return createTimestamp(date.getTime());
    }

    /**
     * Creates a new instance of {@code java.sql.Timestamp} based on the provided time in milliseconds.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long millis = System.currentTimeMillis();
     * Timestamp timestamp = Dates.createTimestamp(millis);   // e.g., 2025-10-22 14:30:45.123
     * }</pre>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @return a new {@code java.sql.Timestamp} instance representing the specified point in time.
     * @see #createTimestamp(Calendar)
     * @see #createTimestamp(java.util.Date)
     * @see #createTime(long)
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
     * Creates a new instance of {@code java.util.Calendar} based on the provided calendar's time value.
     * The returned calendar instance uses the default time zone, not the time zone of the provided calendar.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar source = Calendar.getInstance();
     * Calendar copy = Dates.createCalendar(source);   // Uses default time zone
     * }</pre>
     *
     * @param calendar the calendar providing the time value, not {@code null}.
     * @return a new {@code java.util.Calendar} instance representing the same point in time.
     * @throws IllegalArgumentException if calendar is {@code null}.
     * @see #createCalendar(java.util.Date)
     * @see #createCalendar(long)
     * @see #createCalendar(long, TimeZone)
     */
    public static Calendar createCalendar(final Calendar calendar) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createCalendar(calendar.getTimeInMillis());
    }

    /**
     * Creates a new instance of {@code java.util.Calendar} based on the provided date's time value.
     * The returned calendar instance uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * Calendar cal = Dates.createCalendar(date);   // Converts to Calendar with default time zone
     * }</pre>
     *
     * @param date the date providing the time value, not {@code null}.
     * @return a new {@code java.util.Calendar} instance representing the same point in time.
     * @throws IllegalArgumentException if date is {@code null}.
     * @see #createCalendar(Calendar)
     * @see #createCalendar(long)
     * @see #createCalendar(long, TimeZone)
     */
    public static Calendar createCalendar(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return createCalendar(date.getTime());
    }

    /**
     * Creates a new instance of {@code java.util.Calendar} based on the provided time in milliseconds.
     * The returned calendar instance uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long millis = System.currentTimeMillis();
     * Calendar cal = Dates.createCalendar(millis);   // Creates calendar with default time zone
     * }</pre>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @return a new {@code java.util.Calendar} instance representing the specified point in time.
     * @see #createCalendar(Calendar)
     * @see #createCalendar(java.util.Date)
     * @see #createCalendar(long, TimeZone)
     * @see #createGregorianCalendar(long)
     * @see #createXMLGregorianCalendar(long)
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
     * Creates a new instance of {@code java.util.Calendar} based on the provided time in milliseconds and the specified time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long millis = System.currentTimeMillis();
     * Calendar cal = Dates.createCalendar(millis, TimeZone.getTimeZone("UTC"));   // Uses UTC time zone
     * }</pre>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @param tz the time zone for the calendar; if {@code null}, the default time zone is used.
     * @return a new {@code java.util.Calendar} instance with the specified time and time zone.
     * @see #createCalendar(long)
     * @see #createCalendar(Calendar)
     * @see #createCalendar(java.util.Date)
     */
    public static Calendar createCalendar(final long timeInMillis, final TimeZone tz) {
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
     * Creates a new instance of {@code java.util.GregorianCalendar} based on the provided calendar's time value.
     * The returned calendar instance uses the default time zone, not the time zone of the provided calendar.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar source = Calendar.getInstance();
     * GregorianCalendar gregCal = Dates.createGregorianCalendar(source);   // Uses default time zone
     * }</pre>
     *
     * @param calendar the calendar providing the time value, not {@code null}.
     * @return a new {@code java.util.GregorianCalendar} instance representing the same point in time.
     * @throws IllegalArgumentException if calendar is {@code null}.
     * @see #createGregorianCalendar(java.util.Date)
     * @see #createGregorianCalendar(long)
     * @see #createGregorianCalendar(long, TimeZone)
     */
    public static GregorianCalendar createGregorianCalendar(final Calendar calendar) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createGregorianCalendar(calendar.getTimeInMillis());
    }

    /**
     * Creates a new instance of {@code java.util.GregorianCalendar} based on the provided date's time value.
     * The returned calendar instance uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * GregorianCalendar gregCal = Dates.createGregorianCalendar(date);   // Uses default time zone
     * }</pre>
     *
     * @param date the date providing the time value, not {@code null}.
     * @return a new {@code java.util.GregorianCalendar} instance representing the same point in time.
     * @throws IllegalArgumentException if date is {@code null}.
     * @see #createGregorianCalendar(Calendar)
     * @see #createGregorianCalendar(long)
     * @see #createGregorianCalendar(long, TimeZone)
     */
    public static GregorianCalendar createGregorianCalendar(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return createGregorianCalendar(date.getTime());
    }

    /**
     * Creates a new instance of {@code java.util.GregorianCalendar} based on the provided time in milliseconds.
     * The returned calendar instance uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long millis = System.currentTimeMillis();
     * GregorianCalendar gregCal = Dates.createGregorianCalendar(millis);   // Uses default time zone
     * }</pre>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @return a new {@code java.util.GregorianCalendar} instance representing the specified point in time.
     * @see #createGregorianCalendar(Calendar)
     * @see #createGregorianCalendar(java.util.Date)
     * @see #createGregorianCalendar(long, TimeZone)
     * @see #createCalendar(long)
     * @see #createXMLGregorianCalendar(long)
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
     * Creates a new instance of {@code java.util.GregorianCalendar} based on the provided time in milliseconds and the specified time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long millis = System.currentTimeMillis();
     * GregorianCalendar gregCal = Dates.createGregorianCalendar(millis, TimeZone.getTimeZone("UTC"));
     * }</pre>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @param tz the time zone for the calendar; if {@code null}, the default time zone is used.
     * @return a new {@code java.util.GregorianCalendar} instance with the specified time and time zone.
     * @see #createGregorianCalendar(long)
     * @see #createGregorianCalendar(Calendar)
     * @see #createGregorianCalendar(java.util.Date)
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
     * Creates a new instance of {@code XMLGregorianCalendar} based on the provided calendar's time value.
     * The returned calendar instance uses the default time zone, not the time zone of the provided calendar.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar source = Calendar.getInstance();
     * XMLGregorianCalendar xmlCal = Dates.createXMLGregorianCalendar(source);
     * }</pre>
     *
     * @param calendar the calendar providing the time value, not {@code null}.
     * @return a new {@code XMLGregorianCalendar} instance representing the same point in time.
     * @throws IllegalArgumentException if calendar is {@code null}.
     * @see #createXMLGregorianCalendar(java.util.Date)
     * @see #createXMLGregorianCalendar(long)
     * @see #createXMLGregorianCalendar(long, TimeZone)
     */
    public static XMLGregorianCalendar createXMLGregorianCalendar(final Calendar calendar) throws IllegalArgumentException {
        N.checkArgNotNull(calendar, cs.calendar);

        return createXMLGregorianCalendar(calendar.getTimeInMillis());
    }

    /**
     * Creates a new instance of {@code XMLGregorianCalendar} based on the provided date's time value.
     * The returned calendar instance uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * XMLGregorianCalendar xmlCal = Dates.createXMLGregorianCalendar(date);
     * }</pre>
     *
     * @param date the date providing the time value, not {@code null}.
     * @return a new {@code XMLGregorianCalendar} instance representing the same point in time.
     * @throws IllegalArgumentException if date is {@code null}.
     * @see #createXMLGregorianCalendar(Calendar)
     * @see #createXMLGregorianCalendar(long)
     * @see #createXMLGregorianCalendar(long, TimeZone)
     */
    public static XMLGregorianCalendar createXMLGregorianCalendar(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        return createXMLGregorianCalendar(date.getTime());
    }

    /**
     * Creates a new instance of {@code XMLGregorianCalendar} based on the provided time in milliseconds.
     * The returned calendar instance uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long millis = System.currentTimeMillis();
     * XMLGregorianCalendar xmlCal = Dates.createXMLGregorianCalendar(millis);
     * }</pre>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @return a new {@code XMLGregorianCalendar} instance representing the specified point in time.
     * @see #createXMLGregorianCalendar(Calendar)
     * @see #createXMLGregorianCalendar(java.util.Date)
     * @see #createXMLGregorianCalendar(long, TimeZone)
     * @see #createCalendar(long)
     * @see #createGregorianCalendar(long)
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
     * Creates a new instance of {@code XMLGregorianCalendar} based on the provided time in milliseconds and the specified time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long millis = System.currentTimeMillis();
     * XMLGregorianCalendar xmlCal = Dates.createXMLGregorianCalendar(millis, TimeZone.getTimeZone("UTC"));
     * }</pre>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @param tz the time zone for the calendar; if {@code null}, the default time zone is used.
     * @return a new {@code XMLGregorianCalendar} instance with the specified time and time zone.
     * @see #createXMLGregorianCalendar(long)
     * @see #createXMLGregorianCalendar(Calendar)
     * @see #createXMLGregorianCalendar(java.util.Date)
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
     * Attempts to automatically detect the date format from common patterns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date1 = Dates.parseJUDate("2025-10-22");
     * Date date2 = Dates.parseJUDate("2025-10-22T14:30:45Z");
     * Date date3 = Dates.parseJUDate("1729608645000");   // Parses milliseconds
     * }</pre>
     *
     * @param date the string representation of the date to be parsed.
     * @return the parsed {@code java.util.Date} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @see #parseJUDate(String, String)
     * @see #parseJUDate(String, String, TimeZone)
     * @see #createJUDate(long)
     */
    @MayReturnNull
    public static java.util.Date parseJUDate(final String date) {
        return parseJUDate(date, null);
    }

    /**
     * Parses a string representation of a date into a {@code java.util.Date} object using the specified format.
     * If the format is {@code null}, attempts to automatically detect the format.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = Dates.parseJUDate("22/10/2025", "dd/MM/yyyy");
     * Date isoDate = Dates.parseJUDate("2025-10-22T14:30:45Z", Dates.ISO_8601_DATE_TIME_FORMAT);
     * }</pre>
     *
     * @param date the string representation of the date to be parsed.
     * @param format the date format pattern; if {@code null}, common formats are attempted automatically.
     * @return the parsed {@code java.util.Date} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @throws IllegalArgumentException if the date string cannot be parsed using the specified format.
     * @see #parseJUDate(String)
     * @see #parseJUDate(String, String, TimeZone)
     * @see SimpleDateFormat
     */
    @MayReturnNull
    public static java.util.Date parseJUDate(final String date, final String format) {
        return parseJUDate(date, format, null);
    }

    /**
     * Parses a string representation of a date into a {@code java.util.Date} object using the specified format and time zone.
     * If the format is {@code null}, attempts to automatically detect the format.
     * If the time zone is {@code null}, uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Date date = Dates.parseJUDate("2025-10-22 14:30:45", "yyyy-MM-dd HH:mm:ss", utc);
     * }</pre>
     *
     * @param date the string representation of the date to be parsed.
     * @param format the date format pattern; if {@code null}, common formats are attempted automatically.
     * @param timeZone the time zone for parsing; if {@code null}, the default time zone is used.
     * @return the parsed {@code java.util.Date} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @throws IllegalArgumentException if the date string cannot be parsed using the specified format.
     * @see #parseJUDate(String)
     * @see #parseJUDate(String, String)
     * @see SimpleDateFormat
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
     * Parses a string representation of a date into a {@code java.sql.Date} object.
     * Attempts to automatically detect the date format from common patterns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date sqlDate1 = Dates.parseDate("2025-10-22");
     * Date sqlDate2 = Dates.parseDate("2025-10-22T14:30:45Z");
     * }</pre>
     *
     * @param date the string representation of the date to be parsed.
     * @return the parsed {@code java.sql.Date} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @see #parseDate(String, String)
     * @see #parseDate(String, String, TimeZone)
     * @see #parseJUDate(String)
     */
    @MayReturnNull
    public static Date parseDate(final String date) {
        return parseDate(date, null);
    }

    /**
     * Parses a string representation of a date into a {@code java.sql.Date} object using the specified format.
     * If the format is {@code null}, attempts to automatically detect the format.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date sqlDate = Dates.parseDate("22/10/2025", "dd/MM/yyyy");
     * }</pre>
     *
     * @param date the string representation of the date to be parsed.
     * @param format the date format pattern; if {@code null}, common formats are attempted automatically.
     * @return the parsed {@code java.sql.Date} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @throws IllegalArgumentException if the date string cannot be parsed using the specified format.
     * @see #parseDate(String)
     * @see #parseDate(String, String, TimeZone)
     * @see #parseJUDate(String, String)
     */
    @MayReturnNull
    public static Date parseDate(final String date, final String format) {
        return parseDate(date, format, null);
    }

    /**
     * Parses a string representation of a date into a {@code java.sql.Date} object using the specified format and time zone.
     * If the format is {@code null}, attempts to automatically detect the format.
     * If the time zone is {@code null}, uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Date sqlDate = Dates.parseDate("2025-10-22", "yyyy-MM-dd", utc);
     * }</pre>
     *
     * @param date the string representation of the date to be parsed.
     * @param format the date format pattern; if {@code null}, common formats are attempted automatically.
     * @param timeZone the time zone for parsing; if {@code null}, the default time zone is used.
     * @return the parsed {@code java.sql.Date} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @throws IllegalArgumentException if the date string cannot be parsed using the specified format.
     * @see #parseDate(String)
     * @see #parseDate(String, String)
     * @see #parseJUDate(String, String, TimeZone)
     */
    @MayReturnNull
    public static Date parseDate(final String date, final String format, final TimeZone timeZone) {
        if (isNullDateTime(date)) {
            return null;
        }

        return createDate(parse(date, format, timeZone));
    }

    /**
     * Parses a string representation of a time into a {@code java.sql.Time} object.
     * Attempts to automatically detect the time format from common patterns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Time time1 = Dates.parseTime("14:30:45");
     * Time time2 = Dates.parseTime("2025-10-22T14:30:45Z");   // Extracts time part
     * }</pre>
     *
     * @param date the string representation of the time to be parsed.
     * @return the parsed {@code java.sql.Time} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @see #parseTime(String, String)
     * @see #parseTime(String, String, TimeZone)
     * @see #parseDate(String)
     */
    @MayReturnNull
    public static Time parseTime(final String date) {
        return parseTime(date, null);
    }

    /**
     * Parses a string representation of a time into a {@code java.sql.Time} object using the specified format.
     * If the format is {@code null}, attempts to automatically detect the format.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Time time = Dates.parseTime("02:30:45 PM", "hh:mm:ss a");
     * }</pre>
     *
     * @param date the string representation of the time to be parsed.
     * @param format the time format pattern; if {@code null}, common formats are attempted automatically.
     * @return the parsed {@code java.sql.Time} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @throws IllegalArgumentException if the time string cannot be parsed using the specified format.
     * @see #parseTime(String)
     * @see #parseTime(String, String, TimeZone)
     * @see #parseDate(String, String)
     */
    @MayReturnNull
    public static Time parseTime(final String date, final String format) {
        return parseTime(date, format, null);
    }

    /**
     * Parses a string representation of a time into a {@code java.sql.Time} object using the specified format and time zone.
     * If the format is {@code null}, attempts to automatically detect the format.
     * If the time zone is {@code null}, uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Time time = Dates.parseTime("14:30:45", "HH:mm:ss", utc);
     * }</pre>
     *
     * @param date the string representation of the time to be parsed.
     * @param format the time format pattern; if {@code null}, common formats are attempted automatically.
     * @param timeZone the time zone for parsing; if {@code null}, the default time zone is used.
     * @return the parsed {@code java.sql.Time} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @throws IllegalArgumentException if the time string cannot be parsed using the specified format.
     * @see #parseTime(String)
     * @see #parseTime(String, String)
     * @see #parseDate(String, String, TimeZone)
     */
    @MayReturnNull
    public static Time parseTime(final String date, final String format, final TimeZone timeZone) {
        if (isNullDateTime(date)) {
            return null;
        }

        return createTime(parse(date, format, timeZone));
    }

    /**
     * Parses a string representation of a timestamp into a {@code java.sql.Timestamp} object.
     * Attempts to automatically detect the timestamp format from common patterns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Timestamp ts1 = Dates.parseTimestamp("2025-10-22T14:30:45.123Z");
     * Timestamp ts2 = Dates.parseTimestamp("2025-10-22 14:30:45.123");
     * Timestamp ts3 = Dates.parseTimestamp("1729608645123");   // Parses milliseconds
     * }</pre>
     *
     * @param date the string representation of the timestamp to be parsed.
     * @return the parsed {@code java.sql.Timestamp} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @see #parseTimestamp(String, String)
     * @see #parseTimestamp(String, String, TimeZone)
     * @see #parseDate(String)
     */
    @MayReturnNull
    public static Timestamp parseTimestamp(final String date) {
        return parseTimestamp(date, null);
    }

    /**
     * Parses a string representation of a timestamp into a {@code java.sql.Timestamp} object using the specified format.
     * If the format is {@code null}, attempts to automatically detect the format.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Timestamp ts = Dates.parseTimestamp("2025-10-22 14:30:45.123", "yyyy-MM-dd HH:mm:ss.SSS");
     * }</pre>
     *
     * @param date the string representation of the timestamp to be parsed.
     * @param format the timestamp format pattern; if {@code null}, common formats are attempted automatically.
     * @return the parsed {@code java.sql.Timestamp} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @throws IllegalArgumentException if the timestamp string cannot be parsed using the specified format.
     * @see #parseTimestamp(String)
     * @see #parseTimestamp(String, String, TimeZone)
     * @see #parseTime(String, String)
     */
    @MayReturnNull
    public static Timestamp parseTimestamp(final String date, final String format) {
        return parseTimestamp(date, format, null);
    }

    /**
     * Parses a string representation of a timestamp into a {@code java.sql.Timestamp} object using the specified format and time zone.
     * If the format is {@code null}, attempts to automatically detect the format.
     * If the time zone is {@code null}, uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Timestamp ts = Dates.parseTimestamp("2025-10-22 14:30:45.123", "yyyy-MM-dd HH:mm:ss.SSS", utc);
     * }</pre>
     *
     * @param date the string representation of the timestamp to be parsed.
     * @param format the timestamp format pattern; if {@code null}, common formats are attempted automatically.
     * @param timeZone the time zone for parsing; if {@code null}, the default time zone is used.
     * @return the parsed {@code java.sql.Timestamp} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @throws IllegalArgumentException if the timestamp string cannot be parsed using the specified format.
     * @see #parseTimestamp(String)
     * @see #parseTimestamp(String, String)
     * @see #parseTime(String, String, TimeZone)
     */
    @MayReturnNull
    public static Timestamp parseTimestamp(final String date, final String format, final TimeZone timeZone) {
        if (isNullDateTime(date)) {
            return null;
        }

        return createTimestamp(parse(date, format, timeZone));
    }

    /**
     * Parses a string representation of a date/time into a {@code java.util.Calendar} object.
     * Attempts to automatically detect the format from common patterns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal1 = Dates.parseCalendar("2025-10-22");
     * Calendar cal2 = Dates.parseCalendar("2025-10-22T14:30:45Z");
     * }</pre>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @return the parsed {@code java.util.Calendar} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @see #parseCalendar(String, String)
     * @see #parseCalendar(String, String, TimeZone)
     * @see #createCalendar(long)
     */
    @MayReturnNull
    @Beta
    public static Calendar parseCalendar(final String calendar) {
        return parseCalendar(calendar, null);
    }

    /**
     * Parses a string representation of a date/time into a {@code java.util.Calendar} object using the specified format.
     * If the format is {@code null}, attempts to automatically detect the format.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.parseCalendar("22/10/2025 14:30", "dd/MM/yyyy HH:mm");
     * }</pre>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @param format the date/time format pattern; if {@code null}, common formats are attempted automatically.
     * @return the parsed {@code java.util.Calendar} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @see #parseCalendar(String)
     * @see #parseCalendar(String, String, TimeZone)
     * @see #parseJUDate(String, String)
     */
    @MayReturnNull
    @Beta
    public static Calendar parseCalendar(final String calendar, final String format) {
        return parseCalendar(calendar, format, null);
    }

    /**
     * Parses a string representation of a date/time into a {@code java.util.Calendar} object using the specified format and time zone.
     * If the format is {@code null}, attempts to automatically detect the format.
     * If the time zone is {@code null}, uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseCalendar("2025-10-22 14:30:45", "yyyy-MM-dd HH:mm:ss", utc);
     * }</pre>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @param format the date/time format pattern; if {@code null}, common formats are attempted automatically.
     * @param timeZone the time zone for parsing and for the returned calendar; if {@code null}, the default time zone is used.
     * @return the parsed {@code java.util.Calendar} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @see #parseCalendar(String)
     * @see #parseCalendar(String, String)
     * @see #createCalendar(long, TimeZone)
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
     * Parses a string representation of a date/time into a {@code java.util.GregorianCalendar} object.
     * Attempts to automatically detect the format from common patterns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GregorianCalendar gregCal1 = Dates.parseGregorianCalendar("2025-10-22");
     * GregorianCalendar gregCal2 = Dates.parseGregorianCalendar("2025-10-22T14:30:45Z");
     * }</pre>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @return the parsed {@code java.util.GregorianCalendar} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @see #parseGregorianCalendar(String, String)
     * @see #parseGregorianCalendar(String, String, TimeZone)
     * @see #parseCalendar(String)
     */
    @MayReturnNull
    @Beta
    public static GregorianCalendar parseGregorianCalendar(final String calendar) {
        return parseGregorianCalendar(calendar, null);
    }

    /**
     * Parses a string representation of a date/time into a {@code java.util.GregorianCalendar} object using the specified format.
     * If the format is {@code null}, attempts to automatically detect the format.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GregorianCalendar gregCal = Dates.parseGregorianCalendar("22/10/2025 14:30", "dd/MM/yyyy HH:mm");
     * }</pre>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @param format the date/time format pattern; if {@code null}, common formats are attempted automatically.
     * @return the parsed {@code java.util.GregorianCalendar} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @see #parseGregorianCalendar(String)
     * @see #parseGregorianCalendar(String, String, TimeZone)
     * @see #parseCalendar(String, String)
     */
    @MayReturnNull
    @Beta
    public static GregorianCalendar parseGregorianCalendar(final String calendar, final String format) {
        return parseGregorianCalendar(calendar, format, null);
    }

    /**
     * Parses a string representation of a date/time into a {@code java.util.GregorianCalendar} object using the specified format and time zone.
     * If the format is {@code null}, attempts to automatically detect the format.
     * If the time zone is {@code null}, uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * GregorianCalendar gregCal = Dates.parseGregorianCalendar("2025-10-22 14:30:45", "yyyy-MM-dd HH:mm:ss", utc);
     * }</pre>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @param format the date/time format pattern; if {@code null}, common formats are attempted automatically.
     * @param timeZone the time zone for parsing and for the returned calendar; if {@code null}, the default time zone is used.
     * @return the parsed {@code java.util.GregorianCalendar} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @see #parseGregorianCalendar(String)
     * @see #parseGregorianCalendar(String, String)
     * @see #parseCalendar(String, String, TimeZone)
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
     * Parses a string representation of a date/time into a {@code javax.xml.datatype.XMLGregorianCalendar} object.
     * Attempts to automatically detect the format from common patterns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar xmlCal1 = Dates.parseXMLGregorianCalendar("2025-10-22");
     * XMLGregorianCalendar xmlCal2 = Dates.parseXMLGregorianCalendar("2025-10-22T14:30:45Z");
     * }</pre>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @return the parsed {@code javax.xml.datatype.XMLGregorianCalendar} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @see #parseXMLGregorianCalendar(String, String)
     * @see #parseXMLGregorianCalendar(String, String, TimeZone)
     * @see #parseGregorianCalendar(String)
     */
    @MayReturnNull
    @Beta
    public static XMLGregorianCalendar parseXMLGregorianCalendar(final String calendar) {
        return parseXMLGregorianCalendar(calendar, null);
    }

    /**
     * Parses a string representation of a date/time into a {@code javax.xml.datatype.XMLGregorianCalendar} object using the specified format.
     * If the format is {@code null}, attempts to automatically detect the format.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar xmlCal = Dates.parseXMLGregorianCalendar("22/10/2025 14:30", "dd/MM/yyyy HH:mm");
     * }</pre>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @param format the date/time format pattern; if {@code null}, common formats are attempted automatically.
     * @return the parsed {@code javax.xml.datatype.XMLGregorianCalendar} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @see #parseXMLGregorianCalendar(String)
     * @see #parseXMLGregorianCalendar(String, String, TimeZone)
     * @see #parseGregorianCalendar(String, String)
     */
    @MayReturnNull
    @Beta
    public static XMLGregorianCalendar parseXMLGregorianCalendar(final String calendar, final String format) {
        return parseXMLGregorianCalendar(calendar, format, null);
    }

    /**
     * Parses a string representation of a date/time into a {@code javax.xml.datatype.XMLGregorianCalendar} object using the specified format and time zone.
     * If the format is {@code null}, attempts to automatically detect the format.
     * If the time zone is {@code null}, uses the default time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * XMLGregorianCalendar xmlCal = Dates.parseXMLGregorianCalendar("2025-10-22 14:30:45", "yyyy-MM-dd HH:mm:ss", utc);
     * }</pre>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @param format the date/time format pattern; if {@code null}, common formats are attempted automatically.
     * @param timeZone the time zone for parsing and for the returned calendar; if {@code null}, the default time zone is used.
     * @return the parsed {@code javax.xml.datatype.XMLGregorianCalendar} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @see #parseXMLGregorianCalendar(String)
     * @see #parseXMLGregorianCalendar(String, String)
     * @see #parseGregorianCalendar(String, String, TimeZone)
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

    /**
     * Formats the current local date using the format {@code yyyy-MM-dd}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String formatted = Dates.formatLocalDate();   // e.g., "2025-10-22"
     * }</pre>
     *
     * @return a string representation of the current local date.
     * @see #formatLocalDateTime()
     * @see #formatCurrentDateTime()
     */
    @Beta
    @MayReturnNull
    public static String formatLocalDate() {
        return format(currentDate(), LOCAL_DATE_FORMAT);
    }

    /**
     * Formats the current local date and time using the format {@code yyyy-MM-dd HH:mm:ss}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String formatted = Dates.formatLocalDateTime();   // e.g., "2025-10-22 14:30:45"
     * }</pre>
     *
     * @return a string representation of the current local date and time.
     * @see #formatLocalDate()
     * @see #formatCurrentDateTime()
     */
    @Beta
    @MayReturnNull
    public static String formatLocalDateTime() {
        return format(currentDate(), LOCAL_DATE_TIME_FORMAT);
    }

    /**
     * Formats the current date and time using the ISO 8601 format {@code yyyy-MM-dd'T'HH:mm:ss'Z'}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String formatted = Dates.formatCurrentDateTime();   // e.g., "2025-10-22T14:30:45Z"
     * }</pre>
     *
     * @return a string representation of the current date and time in ISO 8601 format.
     * @see #formatLocalDateTime()
     * @see #formatCurrentTimestamp()
     * @see #format(java.util.Date)
     */
    @MayReturnNull
    public static String formatCurrentDateTime() {
        return format(currentDate(), ISO_8601_DATE_TIME_FORMAT);
    }

    /**
     * Formats the current date and time including milliseconds using the ISO 8601 format {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String formatted = Dates.formatCurrentTimestamp();   // e.g., "2025-10-22T14:30:45.123Z"
     * }</pre>
     *
     * @return a string representation of the current timestamp in ISO 8601 format with milliseconds.
     * @see #formatCurrentDateTime()
     * @see #format(java.util.Date)
     */
    @MayReturnNull
    public static String formatCurrentTimestamp() {
        return format(currentTimestamp(), ISO_8601_TIMESTAMP_FORMAT);
    }

    /**
     * Formats the provided date using a default format that depends on the date type.
     * For {@code Timestamp} instances, the format {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} (with milliseconds) is used.
     * For other {@code Date} instances, the format {@code yyyy-MM-dd'T'HH:mm:ss'Z'} is used.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * String formatted = Dates.format(date);   // e.g., "2025-10-22T14:30:45Z"
     *
     * Timestamp timestamp = new Timestamp(System.currentTimeMillis());
     * String timestampFormatted = Dates.format(timestamp);   // e.g., "2025-10-22T14:30:45.123Z"
     * }</pre>
     *
     * @param date the java.util.Date instance to be formatted; may be {@code null}.
     * @return a string representation of the date, or {@code null} if the date is {@code null}.
     * @see #format(java.util.Date, String)
     * @see #format(java.util.Date, String, TimeZone)
     * @see #formatCurrentDateTime()
     * @see #formatCurrentTimestamp()
     */
    @MayReturnNull
    public static String format(final java.util.Date date) {
        return format(date, null, null);
    }

    /**
     * Formats the provided date into a string representation using the specified format.
     * If no format is provided, a default format ({@code yyyy-MM-dd'T'HH:mm:ss'Z'}) is used.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * String formatted = Dates.format(date, "yyyy-MM-dd");      // e.g., "2025-10-22"
     * String custom = Dates.format(date, "dd/MM/yyyy HH:mm");   // e.g., "22/10/2025 14:30"
     * }</pre>
     *
     * @param date the date to be formatted.
     * @param format the date format pattern; if {@code null}, the default format is used.
     * @return a string representation of the date, or {@code null} if the date is {@code null}.
     * @see #format(java.util.Date, String, TimeZone)
     * @see #parseJUDate(String, String)
     * @see SimpleDateFormat
     */
    @MayReturnNull
    public static String format(final java.util.Date date, final String format) {
        return format(date, format, null);
    }

    /**
     * Formats the provided date into a string representation using the specified format and time zone.
     * If no format is provided, a default format ({@code yyyy-MM-dd'T'HH:mm:ss'Z'}) is used.
     * If no time zone is provided, the default time zone is used.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * String formatted = Dates.format(date, "yyyy-MM-dd HH:mm:ss", utc);   // e.g., "2025-10-22 14:30:45"
     * }</pre>
     *
     * @param date the date to be formatted.
     * @param format the date format pattern; if {@code null}, the default format is used.
     * @param timeZone the time zone for formatting; if {@code null}, the default time zone is used.
     * @return a string representation of the date, or {@code null} if the date is {@code null}.
     * @see #format(java.util.Date, String)
     * @see #parseJUDate(String, String, TimeZone)
     * @see SimpleDateFormat
     */
    @MayReturnNull
    public static String format(final java.util.Date date, final String format, final TimeZone timeZone) {
        return formatDate(null, date, format, timeZone);
    }

    /**
     * Formats the provided calendar into a string representation using the default format ({@code yyyy-MM-dd'T'HH:mm:ss'Z'}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * String formatted = Dates.format(cal);   // e.g., "2025-10-22T14:30:45Z"
     * }</pre>
     *
     * @param calendar the calendar to be formatted.
     * @return a string representation of the calendar, or {@code null} if the calendar is {@code null}.
     * @see #format(Calendar, String)
     * @see #format(Calendar, String, TimeZone)
     * @see #format(java.util.Date)
     */
    @MayReturnNull
    public static String format(final Calendar calendar) {
        return format(calendar, null, null);
    }

    /**
     * Formats the provided calendar into a string representation using the specified format.
     * If no format is provided, a default format ({@code yyyy-MM-dd'T'HH:mm:ss'Z'}) is used.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * String formatted = Dates.format(cal, "yyyy-MM-dd");      // e.g., "2025-10-22"
     * String custom = Dates.format(cal, "dd/MM/yyyy HH:mm");   // e.g., "22/10/2025 14:30"
     * }</pre>
     *
     * @param calendar the calendar to be formatted.
     * @param format the date format pattern; if {@code null}, the default format is used.
     * @return a string representation of the calendar, or {@code null} if the calendar is {@code null}.
     * @see #format(Calendar)
     * @see #format(Calendar, String, TimeZone)
     * @see #parseCalendar(String, String)
     */
    @MayReturnNull
    public static String format(final Calendar calendar, final String format) {
        return format(calendar, format, null);
    }

    /**
     * Formats the provided calendar into a string representation using the specified format and time zone.
     * If no format is provided, a default format ({@code yyyy-MM-dd'T'HH:mm:ss'Z'}) is used.
     * If no time zone is provided, the default time zone is used.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * String formatted = Dates.format(cal, "yyyy-MM-dd HH:mm:ss", utc);   // e.g., "2025-10-22 14:30:45"
     * }</pre>
     *
     * @param calendar the calendar to be formatted.
     * @param format the date format pattern; if {@code null}, the default format is used.
     * @param timeZone the time zone for formatting; if {@code null}, the default time zone is used.
     * @return a string representation of the calendar, or {@code null} if the calendar is {@code null}.
     * @see #format(Calendar, String)
     * @see #format(Calendar)
     * @see #parseCalendar(String, String, TimeZone)
     */
    @MayReturnNull
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
     * Formats the provided XMLGregorianCalendar instance using the default ISO 8601 format {@code yyyy-MM-dd'T'HH:mm:ss'Z'}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar();
     * String formatted = Dates.format(calendar);   // e.g., "2025-10-22T14:30:45Z"
     * }</pre>
     *
     * @param calendar the XMLGregorianCalendar instance to be formatted; may be {@code null}.
     * @return a string representation of the XMLGregorianCalendar instance, or {@code null} if the calendar is {@code null}.
     * @see #format(Calendar)
     * @see #format(XMLGregorianCalendar, String)
     * @see #format(XMLGregorianCalendar, String, TimeZone)
     */
    @MayReturnNull
    public static String format(final XMLGregorianCalendar calendar) {
        return format(calendar, null, null);
    }

    /**
     * Formats the provided XMLGregorianCalendar instance into a string representation using the specified format.
     * If no format is provided, the default format {@code yyyy-MM-dd'T'HH:mm:ss'Z'} is used.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar();
     * String formatted = Dates.format(calendar, "yyyy-MM-dd");      // e.g., "2025-10-22"
     * String custom = Dates.format(calendar, "dd/MM/yyyy HH:mm");   // e.g., "22/10/2025 14:30"
     * }</pre>
     *
     * @param calendar the XMLGregorianCalendar instance to be formatted; may be {@code null}.
     * @param format the date format pattern; if {@code null}, the default format is used.
     * @return a string representation of the XMLGregorianCalendar instance, or {@code null} if the calendar is {@code null}.
     * @see #format(XMLGregorianCalendar)
     * @see #format(XMLGregorianCalendar, String, TimeZone)
     * @see #format(Calendar, String)
     */
    @MayReturnNull
    public static String format(final XMLGregorianCalendar calendar, final String format) {
        return format(calendar, format, null);
    }

    /**
     * Formats the provided XMLGregorianCalendar instance into a string representation using the specified format and time zone.
     * If no format is provided, the default format {@code yyyy-MM-dd'T'HH:mm:ss'Z'} is used.
     * If no time zone is provided, the default time zone of the system is used.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar();
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * String formatted = Dates.format(calendar, "yyyy-MM-dd HH:mm:ss", utc);   // e.g., "2025-10-22 14:30:45"
     * }</pre>
     *
     * @param calendar the XMLGregorianCalendar instance to be formatted; may be {@code null}.
     * @param format the date format pattern; if {@code null}, the default format is used.
     * @param timeZone the time zone for formatting; if {@code null}, the default time zone is used.
     * @return a string representation of the XMLGregorianCalendar instance, or {@code null} if the calendar is {@code null}.
     * @see #format(XMLGregorianCalendar)
     * @see #format(XMLGregorianCalendar, String)
     * @see #format(Calendar, String, TimeZone)
     */
    @MayReturnNull
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
     * Formats the provided date using a default format and appends the result to the specified Appendable.
     * For {@code Timestamp} instances, the format {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} (with milliseconds) is used.
     * For other {@code Date} instances, the format {@code yyyy-MM-dd'T'HH:mm:ss'Z'} is used.
     * If the date is {@code null}, the string "null" is appended to the Appendable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * StringBuilder sb = new StringBuilder("Date: ");
     * Dates.formatTo(date, sb);   // sb now contains "Date: 2025-10-22T14:30:45Z"
     *
     * Timestamp timestamp = new Timestamp(System.currentTimeMillis());
     * StringBuffer buffer = new StringBuffer();
     * Dates.formatTo(timestamp, buffer);   // buffer contains "2025-10-22T14:30:45.123Z"
     * }</pre>
     *
     * @param date the java.util.Date instance to be formatted; may be {@code null}.
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws UncheckedIOException if an I/O error occurs during the append operation.
     * @see #format(java.util.Date)
     * @see #formatTo(java.util.Date, String, Appendable)
     * @see #formatTo(java.util.Date, String, TimeZone, Appendable)
     */
    public static void formatTo(final java.util.Date date, final Appendable appendable) {
        formatTo(date, null, null, appendable);
    }

    /**
     * Formats the provided date into a string representation using the specified format and appends the result to the specified Appendable.
     * If no format is provided, a default format ({@code yyyy-MM-dd'T'HH:mm:ss'Z'} or {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} for Timestamp) is used.
     * If the date is {@code null}, the string "null" is appended to the Appendable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(date, "yyyy-MM-dd", sb);   // sb contains "2025-10-22"
     *
     * StringWriter writer = new StringWriter();
     * Dates.formatTo(date, "dd/MM/yyyy HH:mm", writer);   // writer contains "22/10/2025 14:30"
     * }</pre>
     *
     * @param date the java.util.Date instance to be formatted; may be {@code null}.
     * @param format the date format pattern; if {@code null}, the default format is used.
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws UncheckedIOException if an I/O error occurs during the append operation.
     * @see #format(java.util.Date, String)
     * @see #formatTo(java.util.Date, Appendable)
     * @see #formatTo(java.util.Date, String, TimeZone, Appendable)
     */
    public static void formatTo(final java.util.Date date, final String format, final Appendable appendable) {
        formatTo(date, format, null, appendable);
    }

    /**
     * Formats the provided date into a string representation using the specified format and time zone, and appends the result to the specified Appendable.
     * If no format is provided, a default format ({@code yyyy-MM-dd'T'HH:mm:ss'Z'} or {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} for Timestamp) is used.
     * If no time zone is provided, the default time zone is used.
     * If the date is {@code null}, the string "null" is appended to the Appendable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(date, "yyyy-MM-dd HH:mm:ss", utc, sb);   // sb contains "2025-10-22 14:30:45"
     *
     * TimeZone pst = TimeZone.getTimeZone("America/Los_Angeles");
     * StringBuffer buffer = new StringBuffer();
     * Dates.formatTo(date, "MM/dd/yyyy hh:mm a", pst, buffer);   // buffer contains "10/22/2025 06:30 AM"
     * }</pre>
     *
     * @param date the java.util.Date instance to be formatted; may be {@code null}.
     * @param format the date format pattern; if {@code null}, the default format is used.
     * @param timeZone the time zone for formatting; if {@code null}, the default time zone is used.
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws UncheckedIOException if an I/O error occurs during the append operation.
     * @see #format(java.util.Date, String, TimeZone)
     * @see #formatTo(java.util.Date, Appendable)
     * @see #formatTo(java.util.Date, String, Appendable)
     */
    public static void formatTo(final java.util.Date date, final String format, final TimeZone timeZone, final Appendable appendable) {
        if (date == null) {
            formatToForNull(appendable);
            return;
        }

        formatDate(appendable, date, format, timeZone);
    }

    /**
     * Formats the provided calendar using the default format ({@code yyyy-MM-dd'T'HH:mm:ss'Z'}) and appends the result to the specified Appendable.
     * If the calendar is {@code null}, the string "null" is appended to the Appendable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * StringBuilder sb = new StringBuilder("Calendar: ");
     * Dates.formatTo(cal, sb);   // sb now contains "Calendar: 2025-10-22T14:30:45Z"
     *
     * StringBuffer buffer = new StringBuffer();
     * Dates.formatTo(Calendar.getInstance(), buffer);   // buffer contains "2025-10-22T14:30:45Z"
     * }</pre>
     *
     * @param calendar the java.util.Calendar instance to be formatted; may be {@code null}.
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws UncheckedIOException if an I/O error occurs during the append operation.
     * @see #format(java.util.Calendar)
     * @see #formatTo(Calendar, String, Appendable)
     * @see #formatTo(Calendar, String, TimeZone, Appendable)
     */
    public static void formatTo(final Calendar calendar, final Appendable appendable) {
        formatTo(calendar, null, null, appendable);
    }

    /**
     * Formats the provided calendar into a string representation using the specified format and appends the result to the specified Appendable.
     * If no format is provided, the default format ({@code yyyy-MM-dd'T'HH:mm:ss'Z'}) is used.
     * If the calendar is {@code null}, the string "null" is appended to the Appendable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(cal, "yyyy-MM-dd", sb);   // sb contains "2025-10-22"
     *
     * StringWriter writer = new StringWriter();
     * Dates.formatTo(cal, "dd/MM/yyyy HH:mm", writer);   // writer contains "22/10/2025 14:30"
     * }</pre>
     *
     * @param calendar the java.util.Calendar instance to be formatted; may be {@code null}.
     * @param format the date format pattern; if {@code null}, the default format is used.
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws UncheckedIOException if an I/O error occurs during the append operation.
     * @see #format(java.util.Calendar, String)
     * @see #formatTo(Calendar, Appendable)
     * @see #formatTo(Calendar, String, TimeZone, Appendable)
     */
    public static void formatTo(final Calendar calendar, final String format, final Appendable appendable) {
        formatTo(calendar, format, null, appendable);
    }

    /**
     * Formats the provided calendar into a string representation using the specified format and time zone, and appends the result to the specified Appendable.
     * If no format is provided, the default format ({@code yyyy-MM-dd'T'HH:mm:ss'Z'}) is used.
     * If no time zone is provided, the default time zone is used.
     * If the calendar is {@code null}, the string "null" is appended to the Appendable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(cal, "yyyy-MM-dd HH:mm:ss", utc, sb);   // sb contains "2025-10-22 14:30:45"
     *
     * TimeZone est = TimeZone.getTimeZone("America/New_York");
     * StringBuffer buffer = new StringBuffer();
     * Dates.formatTo(cal, "MM/dd/yyyy hh:mm a", est, buffer);   // buffer contains "10/22/2025 09:30 AM"
     * }</pre>
     *
     * @param calendar the java.util.Calendar instance to be formatted; may be {@code null}.
     * @param format the date format pattern; if {@code null}, the default format is used.
     * @param timeZone the time zone for formatting; if {@code null}, the default time zone is used.
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws UncheckedIOException if an I/O error occurs during the append operation.
     * @see #format(java.util.Calendar, String, TimeZone)
     * @see #formatTo(Calendar, Appendable)
     * @see #formatTo(Calendar, String, Appendable)
     */
    public static void formatTo(final Calendar calendar, final String format, final TimeZone timeZone, final Appendable appendable) {
        if (calendar == null) {
            formatToForNull(appendable);
            return;
        }

        if ((format == null) && (timeZone == null)) {
            fastDateFormat(null, appendable, calendar.getTimeInMillis(), false);
        } else {
            formatTo(createJUDate(calendar), format, timeZone, appendable);
        }
    }

    /**
     * Formats the provided XMLGregorianCalendar using the default ISO 8601 format ({@code yyyy-MM-dd'T'HH:mm:ss'Z'}) and appends the result to the specified Appendable.
     * If the calendar is {@code null}, the string "null" is appended to the Appendable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar();
     * StringBuilder sb = new StringBuilder("XML Calendar: ");
     * Dates.formatTo(calendar, sb);   // sb now contains "XML Calendar: 2025-10-22T14:30:45Z"
     *
     * StringBuffer buffer = new StringBuffer();
     * Dates.formatTo(calendar, buffer);   // buffer contains "2025-10-22T14:30:45Z"
     * }</pre>
     *
     * @param calendar the XMLGregorianCalendar instance to be formatted; may be {@code null}.
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws UncheckedIOException if an I/O error occurs during the append operation.
     * @see #format(XMLGregorianCalendar)
     * @see #formatTo(XMLGregorianCalendar, String, Appendable)
     * @see #formatTo(XMLGregorianCalendar, String, TimeZone, Appendable)
     */
    public static void formatTo(final XMLGregorianCalendar calendar, final Appendable appendable) {
        formatTo(calendar, null, null, appendable);
    }

    /**
     * Formats the provided XMLGregorianCalendar into a string representation using the specified format and appends the result to the specified Appendable.
     * If no format is provided, the default format ({@code yyyy-MM-dd'T'HH:mm:ss'Z'}) is used.
     * If the calendar is {@code null}, the string "null" is appended to the Appendable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar();
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(calendar, "yyyy-MM-dd", sb);   // sb contains "2025-10-22"
     *
     * StringWriter writer = new StringWriter();
     * Dates.formatTo(calendar, "dd/MM/yyyy HH:mm", writer);   // writer contains "22/10/2025 14:30"
     * }</pre>
     *
     * @param calendar the XMLGregorianCalendar instance to be formatted; may be {@code null}.
     * @param format the date format pattern; if {@code null}, the default format is used.
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws UncheckedIOException if an I/O error occurs during the append operation.
     * @see #format(XMLGregorianCalendar, String)
     * @see #formatTo(XMLGregorianCalendar, Appendable)
     * @see #formatTo(XMLGregorianCalendar, String, TimeZone, Appendable)
     */
    public static void formatTo(final XMLGregorianCalendar calendar, final String format, final Appendable appendable) {
        formatTo(calendar, format, null, appendable);
    }

    /**
     * Formats the provided XMLGregorianCalendar into a string representation using the specified format and time zone, and appends the result to the specified Appendable.
     * If no format is provided, the default format ({@code yyyy-MM-dd'T'HH:mm:ss'Z'}) is used.
     * If no time zone is provided, the default time zone of the system is used.
     * If the calendar is {@code null}, the string "null" is appended to the Appendable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar();
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(calendar, "yyyy-MM-dd HH:mm:ss", utc, sb);   // sb contains "2025-10-22 14:30:45"
     *
     * TimeZone jst = TimeZone.getTimeZone("Asia/Tokyo");
     * StringBuffer buffer = new StringBuffer();
     * Dates.formatTo(calendar, "MM/dd/yyyy hh:mm a", jst, buffer);   // buffer contains "10/22/2025 11:30 PM"
     * }</pre>
     *
     * @param calendar the XMLGregorianCalendar instance to be formatted; may be {@code null}.
     * @param format the date format pattern; if {@code null}, the default format is used.
     * @param timeZone the time zone for formatting; if {@code null}, the default time zone is used.
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws UncheckedIOException if an I/O error occurs during the append operation.
     * @see #format(XMLGregorianCalendar, String, TimeZone)
     * @see #formatTo(XMLGregorianCalendar, Appendable)
     * @see #formatTo(XMLGregorianCalendar, String, Appendable)
     */
    public static void formatTo(final XMLGregorianCalendar calendar, final String format, final TimeZone timeZone, final Appendable appendable) {
        if (calendar == null) {
            formatToForNull(appendable);
            return;
        }

        if ((format == null) && (timeZone == null)) {
            fastDateFormat(null, appendable, calendar.toGregorianCalendar().getTimeInMillis(), false);
        } else {
            formatTo(createJUDate(calendar.toGregorianCalendar()), format, timeZone, appendable);
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

        // Validate year is within bounds for the cbufOfSTDInt array
        if (year < 0 || year >= 10000) {
            throw new IllegalArgumentException("Year " + year + " is outside the supported range [0, 9999] for fast formatting");
        }

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

    //    /**

    //-----------------------------------------------------------------------

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Sets the years field to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();                     // 2024-11-24 10:30:45
     * Date result = Dates.setYears(date, 2025);   // 2025-11-24 10:30:45
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not null.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is null.
     * @see Calendar#YEAR
     * @see Calendar#set(int, int)
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();                   // 2024-11-24 10:30:45
     * Date result = Dates.setMonths(date, 0);   // 2024-01-24 10:30:45 (January is 0)
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not null.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is null.
     * @see Calendar#MONTH
     * @see Calendar#set(int, int)
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();                  // 2024-11-24 10:30:45
     * Date result = Dates.setDays(date, 15);   // 2024-11-15 10:30:45
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not null.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is null.
     * @see Calendar#DAY_OF_MONTH
     * @see Calendar#set(int, int)
     */
    public static <T extends java.util.Date> T setDays(final T date, final int amount) {
        return set(date, Calendar.DAY_OF_MONTH, amount);
    }

    //-----------------------------------------------------------------------

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * Sets the hours field to a date returning a new object.
     * Hours range from 0-23.
     * The original {@code Date} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();                   // 2024-11-24 10:30:45
     * Date result = Dates.setHours(date, 14);   // 2024-11-24 14:30:45
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not null.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is null.
     * @see Calendar#HOUR_OF_DAY
     * @see Calendar#set(int, int)
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();                     // 2024-11-24 10:30:45
     * Date result = Dates.setMinutes(date, 15);   // 2024-11-24 10:15:45
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not null.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is null.
     * @see Calendar#MINUTE
     * @see Calendar#set(int, int)
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();                    // 2024-11-24 10:30:45
     * Date result = Dates.setSeconds(date, 0);   // 2024-11-24 10:30:00
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not null.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is null.
     * @see Calendar#SECOND
     * @see Calendar#set(int, int)
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();                           // 2024-11-24 10:30:45.123
     * Date result = Dates.setMilliseconds(date, 500);   // 2024-11-24 10:30:45.500
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not null.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is null.
     * @see Calendar#MILLISECOND
     * @see Calendar#set(int, int)
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
     * @param <T>.
     * @param date the date, not null.
     * @param calendarField the {@code Calendar} field to set the amount to.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is null.
     * @see Calendar#set(int, int)
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
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * Date future = Dates.roll(date, 5, TimeUnit.DAYS);   // Add 5 days
     * Date past = Dates.roll(date, -2, TimeUnit.HOURS);   // Subtract 2 hours
     * }</pre>
     *
     * @param <T> the type of Date to return.
     * @param date the date to roll, must not be {@code null}.
     * @param amount the amount of time to add or subtract (negative values subtract).
     * @param unit the time unit to use for rolling.
     * @return a new instance of Date with the specified amount rolled.
     * @throws IllegalArgumentException if the date is null.
     * @see Calendar#set(int, int)
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
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * Date future = Dates.roll(date, 5, CalendarField.DAY_OF_MONTH);   // Add 5 days
     * Date past = Dates.roll(date, -2, CalendarField.HOUR_OF_DAY);     // Subtract 2 hours
     * }</pre>
     *
     * @param <T> the type of Date to return.
     * @param date the date to roll, must not be {@code null}.
     * @param amount the amount to add or subtract (negative values subtract).
     * @param unit the calendar field unit to use for rolling.
     * @return a new instance of Date with the specified amount rolled.
     * @throws IllegalArgumentException if the date or unit is null.
     * @see Calendar#set(int, int)
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
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar calendar = Calendar.getInstance();
     * Calendar future = Dates.roll(calendar, 5, TimeUnit.DAYS);   // Add 5 days
     * Calendar past = Dates.roll(calendar, -2, TimeUnit.HOURS);   // Subtract 2 hours
     * }</pre>
     *
     * @param <T> the type of Calendar to return.
     * @param calendar the calendar to roll, must not be {@code null}.
     * @param amount the amount of time to add or subtract (negative values subtract).
     * @param unit the time unit to use for rolling.
     * @return a new instance of Calendar with the specified amount rolled.
     * @throws IllegalArgumentException if the calendar or unit is null.
     * @see Calendar#set(int, int)
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
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar calendar = Calendar.getInstance();
     * Calendar future = Dates.roll(calendar, 5, CalendarField.DAY_OF_MONTH);   // Add 5 days
     * Calendar past = Dates.roll(calendar, -2, CalendarField.HOUR_OF_DAY);     // Subtract 2 hours
     * }</pre>
     *
     * @param <T> the type of Calendar to return.
     * @param calendar the calendar to roll, must not be {@code null}.
     * @param amount the amount to add or subtract (negative values subtract).
     * @param unit the calendar field unit to use for rolling.
     * @return a new instance of Calendar with the specified amount rolled.
     * @throws IllegalArgumentException if the calendar or unit is null.
     * @see Calendar#set(int, int)
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

            case HOUR_OF_DAY:
                return amount * 60 * 60 * 1000L;

            case DAY_OF_MONTH:
                return amount * 24 * 60 * 60 * 1000L;

            case WEEK_OF_YEAR:
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * Date nextYear = Dates.addYears(date, 1);    // Add 1 year
     * Date lastYear = Dates.addYears(date, -1);   // Subtract 1 year
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add years to, not null.
     * @param amount the amount of years to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of years added.
     * @throws IllegalArgumentException if the date is null.
     */
    public static <T extends java.util.Date> T addYears(final T date, final int amount) {
        return roll(date, amount, CalendarField.YEAR);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of months to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * Date nextMonth = Dates.addMonths(date, 3);   // Add 3 months
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add months to, not null.
     * @param amount the amount of months to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of months added.
     * @throws IllegalArgumentException if the date is null.
     */
    public static <T extends java.util.Date> T addMonths(final T date, final int amount) {
        return roll(date, amount, CalendarField.MONTH);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of weeks to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * Date nextWeek = Dates.addWeeks(date, 2);   // Add 2 weeks
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add weeks to, not null.
     * @param amount the amount of weeks to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of weeks added.
     * @throws IllegalArgumentException if the date is null.
     */
    public static <T extends java.util.Date> T addWeeks(final T date, final int amount) {
        return roll(date, amount, CalendarField.WEEK_OF_YEAR);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of days to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * Date tomorrow = Dates.addDays(date, 1);   // Add 1 day
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add days to, not null.
     * @param amount the amount of days to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of days added.
     * @throws IllegalArgumentException if the date is null.
     */
    public static <T extends java.util.Date> T addDays(final T date, final int amount) {
        return roll(date, amount, CalendarField.DAY_OF_MONTH);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of hours to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * Date future = Dates.addHours(date, 3);   // Add 3 hours
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add hours to, not null.
     * @param amount the amount of hours to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of hours added.
     * @throws IllegalArgumentException if the date is null.
     */
    public static <T extends java.util.Date> T addHours(final T date, final int amount) {
        return roll(date, amount, CalendarField.HOUR_OF_DAY);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of minutes to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * Date future = Dates.addMinutes(date, 30);   // Add 30 minutes
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add minutes to, not null.
     * @param amount the amount of minutes to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of minutes added.
     * @throws IllegalArgumentException if the date is null.
     */
    public static <T extends java.util.Date> T addMinutes(final T date, final int amount) {
        return roll(date, amount, CalendarField.MINUTE);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of seconds to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * Date future = Dates.addSeconds(date, 45);   // Add 45 seconds
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add seconds to, not null.
     * @param amount the amount of seconds to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of seconds added.
     * @throws IllegalArgumentException if the date is null.
     */
    public static <T extends java.util.Date> T addSeconds(final T date, final int amount) {
        return roll(date, amount, CalendarField.SECOND);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of milliseconds to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * Date future = Dates.addMilliseconds(date, 500);   // Add 500 milliseconds
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add milliseconds to, not null.
     * @param amount the amount of milliseconds to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of milliseconds added.
     * @throws IllegalArgumentException if the date is null.
     */
    public static <T extends java.util.Date> T addMilliseconds(final T date, final int amount) {
        return roll(date, amount, CalendarField.MILLISECOND);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of years to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * Calendar nextYear = Dates.addYears(cal, 1);   // Add 1 year
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add years to, not null.
     * @param amount the amount of years to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of years added.
     * @throws IllegalArgumentException if the calendar is null.
     */
    public static <T extends Calendar> T addYears(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarField.YEAR);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of months to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * Calendar future = Dates.addMonths(cal, 6);   // Add 6 months
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add months to, not null.
     * @param amount the amount of months to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of months added.
     * @throws IllegalArgumentException if the calendar is null.
     */
    public static <T extends Calendar> T addMonths(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarField.MONTH);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of weeks to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * Calendar nextWeek = Dates.addWeeks(cal, 2);   // Add 2 weeks
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add weeks to, not null.
     * @param amount the amount of weeks to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of weeks added.
     * @throws IllegalArgumentException if the calendar is null.
     */
    public static <T extends Calendar> T addWeeks(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarField.WEEK_OF_YEAR);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of days to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * Calendar tomorrow = Dates.addDays(cal, 1);   // Add 1 day
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add days to, not null.
     * @param amount the amount of days to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of days added.
     * @throws IllegalArgumentException if the calendar is null.
     */
    public static <T extends Calendar> T addDays(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarField.DAY_OF_MONTH);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of hours to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * Calendar future = Dates.addHours(cal, 5);   // Add 5 hours
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add hours to, not null.
     * @param amount the amount of hours to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of hours added.
     * @throws IllegalArgumentException if the calendar is null.
     */
    public static <T extends Calendar> T addHours(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarField.HOUR_OF_DAY);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of minutes to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * Calendar future = Dates.addMinutes(cal, 15);   // Add 15 minutes
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add minutes to, not null.
     * @param amount the amount of minutes to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of minutes added.
     * @throws IllegalArgumentException if the calendar is null.
     */
    public static <T extends Calendar> T addMinutes(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarField.MINUTE);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of seconds to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * Calendar future = Dates.addSeconds(cal, 30);   // Add 30 seconds
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add seconds to, not null.
     * @param amount the amount of seconds to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of seconds added.
     * @throws IllegalArgumentException if the calendar is null.
     */
    public static <T extends Calendar> T addSeconds(final T calendar, final int amount) {
        return roll(calendar, amount, CalendarField.SECOND);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of milliseconds to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * Calendar future = Dates.addMilliseconds(cal, 250);   // Add 250 milliseconds
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add milliseconds to, not null.
     * @param amount the amount of milliseconds to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of milliseconds added.
     * @throws IllegalArgumentException if the calendar is null.
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
     * <p>For a date in a timezone that handles the change to daylight-saving time, rounding to Calendar.HOUR_OF_DAY will behave as follows.
     * Suppose daylight-saving time begins at 02:00 on March 30. Rounding a
     * date that crosses this time would produce the following values:
     * </p>
     * <ul>
     * <li>March 30, 2003 01:10 rounds to March 30, 2003 01:00</li>
     * <li>March 30, 2003 01:40 rounds to March 30, 2003 03:00</li>
     * <li>March 30, 2003 02:10 rounds to March 30, 2003 03:00</li>
     * <li>March 30, 2003 02:40 rounds to March 30, 2003 04:00</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * Date rounded = Dates.round(date, Calendar.HOUR_OF_DAY);
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date to work with, not null.
     * @param field the field from {@code Calendar} or {@code SEMI_MONTH}.
     * @return a new date object of type T, rounded to the nearest whole unit as specified by the field.
     * @throws IllegalArgumentException if the date is null.
     * @throws ArithmeticException if the year is over 280 million.
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
     * <p>For example, if you had the date-time of 28 Mar 2002
     * 13:45:01.231, if this was passed with HOUR_OF_DAY, it would return
     * 28 Mar 2002 14:00:00.000. If this was passed with MONTH, it
     * would return 1 April 2002 0:00:00.000.</p>
     *
     * <p>For a date in a timezone that handles the change to daylight-saving time, rounding to CalendarField.HOUR_OF_DAY will behave as follows.
     * Suppose daylight-saving time begins at 02:00 on March 30. Rounding a
     * date that crosses this time would produce the following values:
     * </p>
     * <ul>
     * <li>March 30, 2003 01:10 rounds to March 30, 2003 01:00</li>
     * <li>March 30, 2003 01:40 rounds to March 30, 2003 03:00</li>
     * <li>March 30, 2003 02:10 rounds to March 30, 2003 03:00</li>
     * <li>March 30, 2003 02:40 rounds to March 30, 2003 04:00</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * Date rounded = Dates.round(date, CalendarField.HOUR_OF_DAY);
     * }</pre>
     *
     * @param <T> the type of the date object, which must be a subclass of java.util.Date.
     * @param date the date to be rounded. Must not be {@code null}.
     * @param field the CalendarField to which the date is to be rounded. Must not be {@code null}.
     * @return a new date object of type T, rounded to the nearest whole unit as specified by the field.
     * @throws IllegalArgumentException if the date or field is {@code null}.
     * @throws ArithmeticException if the year is over 280 million.
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
     * <p>For a date in a timezone that handles the change to daylight-saving time, rounding to Calendar.HOUR_OF_DAY will behave as follows.
     * Suppose daylight-saving time begins at 02:00 on March 30. Rounding a
     * date that crosses this time would produce the following values:
     * </p>
     * <ul>
     * <li>March 30, 2003 01:10 rounds to March 30, 2003 01:00</li>
     * <li>March 30, 2003 01:40 rounds to March 30, 2003 03:00</li>
     * <li>March 30, 2003 02:10 rounds to March 30, 2003 03:00</li>
     * <li>March 30, 2003 02:40 rounds to March 30, 2003 04:00</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar calendar = Calendar.getInstance();
     * Calendar rounded = Dates.round(calendar, Calendar.HOUR_OF_DAY);
     * }</pre>
     *
     * @param <T> the type of the calendar object, which must extend java.util.Calendar.
     * @param calendar the date to work with, not null.
     * @param field the field from {@code Calendar} or {@code SEMI_MONTH}.
     * @return a new calendar object of type T, rounded to the nearest whole unit as specified by the field.
     * @throws IllegalArgumentException if the date is {@code null}.
     * @throws ArithmeticException if the year is over 280 million.
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
     * <p>For example, if you had the date-time of 28 Mar 2002
     * 13:45:01.231, if this was passed with HOUR_OF_DAY, it would return
     * 28 Mar 2002 14:00:00.000. If this was passed with MONTH, it
     * would return 1 April 2002 0:00:00.000.</p>
     *
     * <p>For a date in a timezone that handles the change to daylight-saving time, rounding to CalendarField.HOUR_OF_DAY will behave as follows.
     * Suppose daylight-saving time begins at 02:00 on March 30. Rounding a
     * date that crosses this time would produce the following values:
     * </p>
     * <ul>
     * <li>March 30, 2003 01:10 rounds to March 30, 2003 01:00</li>
     * <li>March 30, 2003 01:40 rounds to March 30, 2003 03:00</li>
     * <li>March 30, 2003 02:10 rounds to March 30, 2003 03:00</li>
     * <li>March 30, 2003 02:40 rounds to March 30, 2003 04:00</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar calendar = Calendar.getInstance();
     * Calendar rounded = Dates.round(calendar, CalendarField.HOUR_OF_DAY);
     * }</pre>
     *
     * @param <T> the type of the calendar object, which must be a subclass of java.util.Calendar.
     * @param calendar the calendar to be rounded. Must not be {@code null}.
     * @param field the CalendarField to which the calendar is to be rounded. Must not be {@code null}.
     * @return a new calendar object of type T, rounded to the nearest whole unit as specified by the field.
     * @throws IllegalArgumentException if the calendar or field is {@code null}.
     * @throws ArithmeticException if the year is over 280 million.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * Date truncated = Dates.truncate(date, Calendar.HOUR_OF_DAY);
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date to work with, not null.
     * @param field the field from {@code Calendar} or {@code SEMI_MONTH}.
     * @return a new date object of type T, truncated to the specified field.
     * @throws IllegalArgumentException if the date is {@code null}.
     * @throws ArithmeticException if the year is over 280 million.
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
     * <p>For example, if you had the date-time of 28 Mar 2002
     * 13:45:01.231, if you passed with HOUR_OF_DAY, it would return 28 Mar
     * 2002 13:00:00.000.  If this was passed with MONTH, it would
     * return 1 Mar 2002 0:00:00.000.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = new Date();
     * Date truncated = Dates.truncate(date, CalendarField.HOUR_OF_DAY);
     * }</pre>
     *
     * @param <T> the type of the date object, which must be a subclass of java.util.Date.
     * @param date the date to be truncated. Must not be {@code null}.
     * @param field the CalendarField to which the date is to be truncated. Must not be {@code null}.
     * @return a new date object of type T, truncated to the specified field.
     * @throws IllegalArgumentException if the date or field is {@code null}.
     * @throws ArithmeticException if the year is over 280 million.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar calendar = Calendar.getInstance();
     * Calendar truncated = Dates.truncate(calendar, Calendar.HOUR_OF_DAY);
     * }</pre>
     *
     * @param <T> the type of the calendar object, which must extend java.util.Calendar.
     * @param calendar the date to work with, not null.
     * @param field the field from {@code Calendar} or {@code SEMI_MONTH}.
     * @return a new calendar object of type T, truncated to the specified field.
     * @throws IllegalArgumentException if the date is {@code null}.
     * @throws ArithmeticException if the year is over 280 million.
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
     * <p>For example, if you had the date-time of 28 Mar 2002
     * 13:45:01.231, if you passed with HOUR_OF_DAY, it would return 28 Mar
     * 2002 13:00:00.000.  If this was passed with MONTH, it would
     * return 1 Mar 2002 0:00:00.000.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar calendar = Calendar.getInstance();
     * Calendar truncated = Dates.truncate(calendar, CalendarField.HOUR_OF_DAY);
     * }</pre>
     *
     * @param <T> the type of the calendar object, which must be a subclass of java.util.Calendar.
     * @param calendar the calendar to be truncated. Must not be {@code null}.
     * @param field the CalendarField to which the calendar is to be truncated. Must not be {@code null}.
     * @return a new calendar object of type T, truncated to the specified field.
     * @throws IllegalArgumentException if the calendar or field is {@code null}.
     * @throws ArithmeticException if the year is over 280 million.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = Dates.parseTimestamp("2023-03-28 13:45:01");
     * Date ceiled = Dates.ceiling(date, Calendar.HOUR_OF_DAY);
     * // Result: 2023-03-28 14:00:00
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date to work with, not null.
     * @param field the field from {@code Calendar} or {@code SEMI_MONTH}.
     * @return a new date object of type T, adjusted to the ceiling of the specified field.
     * @throws IllegalArgumentException if the date is {@code null}.
     * @throws ArithmeticException if the year is over 280 million.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = Dates.parseTimestamp("2023-03-28 13:45:01");
     * Date ceiled = Dates.ceiling(date, CalendarField.HOUR_OF_DAY);
     * // Result: 2023-03-28 14:00:00
     * }</pre>
     *
     * @param <T> the type of the date object, which must be a subclass of java.util.Date.
     * @param date the date to be adjusted. Must not be {@code null}.
     * @param field the CalendarField to which the date is to be adjusted. Must not be {@code null}.
     * @return a new date object of type T, adjusted to the nearest future unit as specified by the field.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar calendar = Calendar.getInstance();
     * Calendar ceiled = Dates.ceiling(calendar, Calendar.HOUR_OF_DAY);
     * // Result: calendar rounded up to the next hour
     * }</pre>
     *
     * @param <T> the type of the calendar object, which must extend java.util.Calendar.
     * @param calendar the date to work with, not null.
     * @param field the field from {@code Calendar} or {@code SEMI_MONTH}.
     * @return a new calendar object of type T, adjusted to the ceiling of the specified field.
     * @throws IllegalArgumentException if the date is {@code null}.
     * @throws ArithmeticException if the year is over 280 million.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar calendar = Calendar.getInstance();
     * Calendar ceiled = Dates.ceiling(calendar, CalendarField.HOUR_OF_DAY);
     * // Result: calendar rounded up to the next hour
     * }</pre>
     *
     * @param <T> the type of the calendar object, which must extend java.util.Calendar.
     * @param calendar the original calendar object to be adjusted.
     * @param field the field to be used for the ceiling operation, as a CalendarField.
     * @return a new calendar object representing the adjusted time.
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
     * @param val the calendar, not null.
     * @param field the calendar field to modify.
     * @param modType type to truncate, round or ceiling.
     * @throws ArithmeticException if the year is over 280 million.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal1 = Dates.parseCalendar("2023-03-28 13:45:30");
     * Calendar cal2 = Dates.parseCalendar("2023-03-28 18:20:15");
     * boolean sameDay = Dates.truncatedEquals(cal1, cal2, CalendarField.DAY_OF_MONTH);   // true
     * boolean sameHour = Dates.truncatedEquals(cal1, cal2, CalendarField.HOUR_OF_DAY);   // false
     * }</pre>
     *
     * @param cal1 the first calendar, not null.
     * @param cal2 the second calendar, not null.
     * @param field the field from {@code CalendarField} to be the most significant field for comparison.
     * @return {@code true} if cal1 and cal2 are equal up to the specified field; {@code false} otherwise.
     * @throws IllegalArgumentException if any argument is {@code null}.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal1 = Dates.parseCalendar("2023-03-28 13:45:30");
     * Calendar cal2 = Dates.parseCalendar("2023-03-28 18:20:15");
     * boolean sameDay = Dates.truncatedEquals(cal1, cal2, Calendar.DAY_OF_MONTH);   // true
     * boolean sameHour = Dates.truncatedEquals(cal1, cal2, Calendar.HOUR_OF_DAY);   // false
     * }</pre>
     *
     * @param cal1 the first calendar, not {@code null}.
     * @param cal2 the second calendar, not {@code null}.
     * @param field the field from {@code Calendar}.
     * @return {@code true} if equal; otherwise {@code false}.
     * @throws IllegalArgumentException if any argument is {@code null}.
     * @see #truncate(Calendar, int)
     * @see #truncatedEquals(java.util.Date, java.util.Date, int)
     */
    public static boolean truncatedEquals(final Calendar cal1, final Calendar cal2, final int field) {
        return truncatedCompareTo(cal1, cal2, field) == 0;
    }

    /**
     * Determines if two dates are equal up to no more than the specified most significant field.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date1 = Dates.parseTimestamp("2023-03-28 13:45:30");
     * Date date2 = Dates.parseTimestamp("2023-03-28 18:20:15");
     * boolean sameDay = Dates.truncatedEquals(date1, date2, CalendarField.DAY_OF_MONTH);   // true
     * boolean sameHour = Dates.truncatedEquals(date1, date2, CalendarField.HOUR_OF_DAY);   // false
     * }</pre>
     *
     * @param date1 the first date, not {@code null}.
     * @param date2 the second date, not {@code null}.
     * @param field the field from {@code CalendarField} to be the most significant field for comparison.
     * @return {@code true} if date1 and date2 are equal up to the specified field; {@code false} otherwise.
     * @throws IllegalArgumentException if any argument is {@code null}.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date1 = Dates.parseTimestamp("2023-03-28 13:45:30");
     * Date date2 = Dates.parseTimestamp("2023-03-28 18:20:15");
     * boolean sameDay = Dates.truncatedEquals(date1, date2, Calendar.DAY_OF_MONTH);   // true
     * boolean sameHour = Dates.truncatedEquals(date1, date2, Calendar.HOUR_OF_DAY);   // false
     * }</pre>
     *
     * @param date1 the first date, not {@code null}.
     * @param date2 the second date, not {@code null}.
     * @param field the field from {@code Calendar}.
     * @return {@code true} if equal; otherwise {@code false}.
     * @throws IllegalArgumentException if any argument is {@code null}.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal1 = Dates.parseCalendar("2023-03-28 13:45:30");
     * Calendar cal2 = Dates.parseCalendar("2023-03-28 18:20:15");
     * int dayComparison = Dates.truncatedCompareTo(cal1, cal2, CalendarField.DAY_OF_MONTH);   // 0 (same day)
     * int hourComparison = Dates.truncatedCompareTo(cal1, cal2, CalendarField.HOUR_OF_DAY);   // negative (13 < 18)
     * }</pre>
     *
     * @param cal1 the first Calendar instance to be compared, not {@code null}.
     * @param cal2 the second Calendar instance to be compared, not {@code null}.
     * @param field the field from {@code CalendarField} to be the most significant field for comparison.
     * @return a negative integer, zero, or a positive integer as the first Calendar is less than, equal to, or greater than the second.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal1 = Dates.parseCalendar("2023-03-28 13:45:30");
     * Calendar cal2 = Dates.parseCalendar("2023-03-28 18:20:15");
     * int dayComparison = Dates.truncatedCompareTo(cal1, cal2, Calendar.DAY_OF_MONTH);   // 0 (same day)
     * int hourComparison = Dates.truncatedCompareTo(cal1, cal2, Calendar.HOUR_OF_DAY);   // negative (13 < 18)
     * }</pre>
     *
     * @param cal1 the first calendar, not {@code null}.
     * @param cal2 the second calendar, not {@code null}.
     * @param field the field from {@code Calendar}.
     * @return a negative integer, zero, or a positive integer as the first.
     * calendar is less than, equal to, or greater than the second.
     * @throws IllegalArgumentException if any argument is {@code null}.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date1 = Dates.parseTimestamp("2023-03-28 13:45:30");
     * Date date2 = Dates.parseTimestamp("2023-03-28 18:20:15");
     * int dayComparison = Dates.truncatedCompareTo(date1, date2, CalendarField.DAY_OF_MONTH);   // 0 (same day)
     * int hourComparison = Dates.truncatedCompareTo(date1, date2, CalendarField.HOUR_OF_DAY);   // negative (13 < 18)
     * }</pre>
     *
     * @param date1 the first Date instance to be compared, not {@code null}.
     * @param date2 the second Date instance to be compared, not {@code null}.
     * @param field the field from {@code CalendarField} to be the most significant field for comparison.
     * @return a negative integer, zero, or a positive integer as the first Date is less than, equal to, or greater than the second.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date1 = Dates.parseTimestamp("2023-03-28 13:45:30");
     * Date date2 = Dates.parseTimestamp("2023-03-28 18:20:15");
     * int dayComparison = Dates.truncatedCompareTo(date1, date2, Calendar.DAY_OF_MONTH);   // 0 (same day)
     * int hourComparison = Dates.truncatedCompareTo(date1, date2, Calendar.HOUR_OF_DAY);   // negative (13 < 18)
     * }</pre>
     *
     * @param date1 the first date, not {@code null}.
     * @param date2 the second date, not {@code null}.
     * @param field the field from {@code Calendar}.
     * @return a negative integer, zero, or a positive integer as the first.
     * date is less than, equal to, or greater than the second.
     * @throws IllegalArgumentException if any argument is {@code null}.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = Dates.parseTimestamp("2023-01-06 07:15:10.538");
     * long millis = Dates.getFragmentInMilliseconds(date, CalendarField.SECOND);
     * // Result: 538 (milliseconds within the current second)
     * }</pre>
     *
     * @param date the date to work with, not null.
     * @param fragment the {@code Calendar} field part of date to calculate.
     * @return number of milliseconds within the fragment of date.
     * @throws IllegalArgumentException if the date is {@code null} or.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = Dates.parseTimestamp("2023-01-06 07:15:10.538");
     * long seconds = Dates.getFragmentInSeconds(date, CalendarField.MINUTE);
     * // Result: 10 (seconds within the current minute)
     * }</pre>
     *
     * @param date the date to work with, not null.
     * @param fragment the {@code Calendar} field part of date to calculate.
     * @return number of seconds within the fragment of date.
     * @throws IllegalArgumentException if the date is {@code null} or.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = Dates.parseTimestamp("2023-01-06 07:15:10.538");
     * long minutes = Dates.getFragmentInMinutes(date, CalendarField.HOUR_OF_DAY);
     * // Result: 15 (minutes within the current hour)
     * }</pre>
     *
     * @param date the date to work with, not null.
     * @param fragment the {@code Calendar} field part of date to calculate.
     * @return number of minutes within the fragment of date.
     * @throws IllegalArgumentException if the date is {@code null} or.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = Dates.parseTimestamp("2023-01-06 07:15:10.538");
     * long hours = Dates.getFragmentInHours(date, CalendarField.DAY_OF_YEAR);
     * // Result: 7 (hours within the current day)
     * }</pre>
     *
     * @param date the date to work with, not null.
     * @param fragment the {@code Calendar} field part of date to calculate.
     * @return number of hours within the fragment of date.
     * @throws IllegalArgumentException if the date is {@code null} or.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date = Dates.parseDate("2023-02-28");
     * long days = Dates.getFragmentInDays(date, CalendarField.YEAR);
     * // Result: 59 (days within the current year)
     * }</pre>
     *
     * @param date the date to work with, not null.
     * @param fragment the {@code Calendar} field part of date to calculate.
     * @return number of days within the fragment of date.
     * @throws IllegalArgumentException if the date is {@code null} or.
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
     * @param date the date to work with, not null.
     * @param fragment the Calendar field part of date to calculate.
     * @param unit the time unit.
     * @return number of units within the fragment of the date.
     * @throws IllegalArgumentException if the date is {@code null} or specified fragment is not supported.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.parseCalendar("2023-01-06 07:15:10.538");
     * long millis = Dates.getFragmentInMilliseconds(cal, CalendarField.SECOND);
     * // Result: 538 (milliseconds within the current second)
     * }</pre>
     *
     * @param calendar the calendar to work with, not null.
     * @param fragment the {@code Calendar} field part of calendar to calculate.
     * @return number of milliseconds within the fragment of date.
     * @throws IllegalArgumentException if the date is {@code null} or.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.parseCalendar("2023-01-06 07:15:10.538");
     * long seconds = Dates.getFragmentInSeconds(cal, CalendarField.MINUTE);
     * // Result: 10 (seconds within the current minute)
     * }</pre>
     *
     * @param calendar the calendar to work with, not null.
     * @param fragment the {@code Calendar} field part of calendar to calculate.
     * @return number of seconds within the fragment of date.
     * @throws IllegalArgumentException if the date is {@code null} or.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.parseCalendar("2023-01-06 07:15:10.538");
     * long minutes = Dates.getFragmentInMinutes(cal, CalendarField.HOUR_OF_DAY);
     * // Result: 15 (minutes within the current hour)
     * }</pre>
     *
     * @param calendar the calendar to work with, not null.
     * @param fragment the {@code Calendar} field part of calendar to calculate.
     * @return number of minutes within the fragment of date.
     * @throws IllegalArgumentException if the date is {@code null} or.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.parseCalendar("2023-01-06 07:15:10.538");
     * long hours = Dates.getFragmentInHours(cal, CalendarField.DAY_OF_YEAR);
     * // Result: 7 (hours within the current day)
     * }</pre>
     *
     * @param calendar the calendar to work with, not null.
     * @param fragment the {@code Calendar} field part of calendar to calculate.
     * @return number of hours within the fragment of date.
     * @throws IllegalArgumentException if the date is {@code null} or.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.parseCalendar("2023-02-28");
     * long days = Dates.getFragmentInDays(cal, CalendarField.YEAR);
     * // Result: 59 (days within the current year)
     * }</pre>
     *
     * @param calendar the calendar to work with, not null.
     * @param fragment the {@code Calendar} field part of calendar to calculate.
     * @return number of days within the fragment of date.
     * @throws IllegalArgumentException if the date is {@code null} or.
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
     * @param calendar the calendar to work with, not null.
     * @param fragment the Calendar field part of calendar to calculate.
     * @param unit the time unit.
     * @return number of units within the fragment of the calendar.
     * @throws IllegalArgumentException if the date is {@code null} or.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date1 = Dates.parseDate("2023-03-28");
     * Date date2 = Dates.parseDate("2023-03-28");
     * boolean sameDay = Dates.isSameDay(date1, date2);   // true
     * }</pre>
     *
     * @param date1 the first date, not altered, not null.
     * @param date2 the second date, not altered, not null.
     * @return {@code true} if they represent the same day.
     * @throws IllegalArgumentException if either date is {@code null}.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal1 = Dates.parseCalendar("2023-03-28");
     * Calendar cal2 = Dates.parseCalendar("2023-03-28");
     * boolean sameDay = Dates.isSameDay(cal1, cal2);   // true
     * }</pre>
     *
     * @param cal1 the first calendar, not altered, not null.
     * @param cal2 the second calendar, not altered, not null.
     * @return {@code true} if they represent the same day.
     * @throws IllegalArgumentException if either calendar is {@code null}.
     * @see #isSameDay(java.util.Date, java.util.Date)
     */
    public static boolean isSameDay(final Calendar cal1, final Calendar cal2) throws IllegalArgumentException {
        N.checkArgNotNull(cal1, cs.calendar1);
        N.checkArgNotNull(cal2, cs.calendar2);

        return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Checks if two date objects are in the same month ignoring day and time.</p>
     *
     * <p>15 Mar 2023 13:45 and 28 Mar 2023 06:01 would return {@code true}.
     * 15 Mar 2023 13:45 and 15 Apr 2023 13:45 would return {@code false}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date1 = Dates.parseDate("2023-03-15");
     * Date date2 = Dates.parseDate("2023-03-28");
     * boolean sameMonth = Dates.isSameMonth(date1, date2);   // true
     * }</pre>
     *
     * @param date1 the first date, not altered, not null.
     * @param date2 the second date, not altered, not null.
     * @return {@code true} if they represent the same month of the same year.
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
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Checks if two calendar objects are in the same month ignoring day and time.</p>
     *
     * <p>15 Mar 2023 13:45 and 28 Mar 2023 06:01 would return {@code true}.
     * 15 Mar 2023 13:45 and 15 Apr 2023 13:45 would return {@code false}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal1 = Calendar.getInstance();
     * cal1.set(2023, Calendar.MARCH, 15, 13, 45);
     *
     * Calendar cal2 = Calendar.getInstance();
     * cal2.set(2023, Calendar.MARCH, 28, 6, 1);
     *
     * boolean sameMonth = Dates.isSameMonth(cal1, cal2);   // true
     * }</pre>
     *
     * @param cal1 the first calendar, not altered, not null.
     * @param cal2 the second calendar, not altered, not null.
     * @return {@code true} if they represent the same month of the same year.
     * @throws IllegalArgumentException if either calendar is {@code null}.
     */
    public static boolean isSameMonth(final Calendar cal1, final Calendar cal2) throws IllegalArgumentException {
        N.checkArgNotNull(cal1, cs.calendar1);
        N.checkArgNotNull(cal2, cs.calendar2);

        return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH);
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Checks if two date objects are in the same year ignoring month, day and time.</p>
     *
     * <p>15 Mar 2023 13:45 and 28 Nov 2023 06:01 would return {@code true}.
     * 15 Mar 2023 13:45 and 15 Mar 2024 13:45 would return {@code false}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date1 = Dates.parseDate("2023-03-15");
     * Date date2 = Dates.parseDate("2023-11-28");
     * boolean sameYear = Dates.isSameYear(date1, date2);   // true
     * }</pre>
     *
     * @param date1 the first date, not altered, not null.
     * @param date2 the second date, not altered, not null.
     * @return {@code true} if they represent the same year.
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
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Checks if two calendar objects are in the same year ignoring month, day and time.</p>
     *
     * <p>15 Mar 2023 13:45 and 28 Nov 2023 06:01 would return {@code true}.
     * 15 Mar 2023 13:45 and 15 Mar 2024 13:45 would return {@code false}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal1 = Calendar.getInstance();
     * cal1.set(2023, Calendar.MARCH, 15, 13, 45);
     *
     * Calendar cal2 = Calendar.getInstance();
     * cal2.set(2023, Calendar.NOVEMBER, 28, 6, 1);
     *
     * boolean sameYear = Dates.isSameYear(cal1, cal2);   // true
     * }</pre>
     *
     * @param cal1 the first calendar, not altered, not null.
     * @param cal2 the second calendar, not altered, not null.
     * @return {@code true} if they represent the same year.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date date1 = Dates.parseTimestamp("2023-03-28 13:45:30.123");
     * Date date2 = Dates.parseTimestamp("2023-03-28 13:45:30.123");
     * boolean sameInstant = Dates.isSameInstant(date1, date2);   // true
     * }</pre>
     *
     * @param date1 the first date, not altered, not null.
     * @param date2 the second date, not altered, not null.
     * @return {@code true} if they represent the same millisecond instant.
     * @throws IllegalArgumentException if either date is {@code null}.
     * @see #isSameInstant(Calendar, Calendar)
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal1 = Calendar.getInstance();
     * cal1.setTimeInMillis(1672585530123L);
     *
     * Calendar cal2 = Calendar.getInstance();
     * cal2.setTimeInMillis(1672585530123L);
     *
     * boolean sameInstant = Dates.isSameInstant(cal1, cal2);   // true
     * }</pre>
     *
     * @param cal1 the first calendar, not altered, not null.
     * @param cal2 the second calendar, not altered, not null.
     * @return {@code true} if they represent the same millisecond instant.
     * @throws IllegalArgumentException if either date is {@code null}.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal1 = Calendar.getInstance();
     * cal1.set(2023, Calendar.MARCH, 15, 13, 45, 30);
     * cal1.set(Calendar.MILLISECOND, 123);
     *
     * Calendar cal2 = Calendar.getInstance();
     * cal2.set(2023, Calendar.MARCH, 15, 13, 45, 30);
     * cal2.set(Calendar.MILLISECOND, 123);
     *
     * boolean sameLocalTime = Dates.isSameLocalTime(cal1, cal2);   // true
     * }</pre>
     *
     * @param cal1 the first calendar, not altered, not null.
     * @param cal2 the second calendar, not altered, not null.
     * @return {@code true} if they represent the same local time.
     * @throws IllegalArgumentException if either date is {@code null}.
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
            } else if (len == 8 && str.charAt(2) == ':' && str.charAt(5) == ':') {
                return LOCAL_TIME_FORMAT;
            } else if (len == 5 && str.charAt(2) == '-') {
                return LOCAL_MONTH_DAY_FORMAT;
            } else if (len > 4 && str.charAt(4) == '-') {
                switch (len) {
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

        c.clear();
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
        final LongFunction<? extends java.util.Date> creator = dateCreatorPool.get(cls);

        if (creator != null) {
            return (T) creator.apply(millis);
        } else {
            return (T) ClassUtil.invokeConstructor(ClassUtil.getDeclaredConstructor(cls, long.class), millis);
        }
    }

    private static <T extends Calendar> T createCalendar(final T source, final long millis) {
        final Class<T> cls = (Class<T>) source.getClass();
        final BiFunction<? super Long, ? super Calendar, ? extends java.util.Calendar> creator = calendarCreatorPool.get(cls);

        if (creator != null) {
            return (T) creator.apply(millis, source);
        } else {
            T result = null;
            Constructor<T> constructor = ClassUtil.getDeclaredConstructor(cls, long.class);

            if (constructor != null && Modifier.isPublic(constructor.getModifiers())) {
                result = ClassUtil.invokeConstructor(constructor, millis);
            } else {
                constructor = ClassUtil.getDeclaredConstructor(cls);
                result = ClassUtil.invokeConstructor(constructor);
                result.setTimeInMillis(millis);
            }

            if (!N.equals(source.getTimeZone(), result.getTimeZone())) {
                result.setTimeZone(source.getTimeZone());
            }

            return result;
        }
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date jan31 = Dates.parseDate("2023-01-31");
     * boolean isLast = Dates.isLastDateOfMonth(jan31);   // true
     *
     * Date jan15 = Dates.parseDate("2023-01-15");
     * boolean isNotLast = Dates.isLastDateOfMonth(jan15);   // false
     * }</pre>
     *
     * @param date the date to check. Must not be {@code null}.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date dec31 = Dates.parseDate("2023-12-31");
     * boolean isLast = Dates.isLastDateOfYear(dec31);   // true
     *
     * Date dec15 = Dates.parseDate("2023-12-15");
     * boolean isNotLast = Dates.isLastDateOfYear(dec15);   // false
     * }</pre>
     *
     * @param date the date to check. Must not be {@code null}.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date jan15 = Dates.parseDate("2023-01-15");
     * int lastDay = Dates.getLastDayOfMonth(jan15);   // 31
     *
     * Date feb15 = Dates.parseDate("2023-02-15");
     * int febLastDay = Dates.getLastDayOfMonth(feb15);   // 28 (non-leap year)
     *
     * Date feb15Leap = Dates.parseDate("2024-02-15");
     * int febLeapLastDay = Dates.getLastDayOfMonth(feb15Leap);   // 29 (leap year)
     * }</pre>
     *
     * @param date the date to be evaluated. Must not be {@code null}.
     * @return the last day of the month for the given date as an integer.
     * @throws IllegalArgumentException if the date is {@code null}.
     */
    public static int getLastDayOfMonth(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /**
     * Returns the last day of the year for the given date.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Date jan15 = Dates.parseDate("2023-01-15");
     * int lastDay = Dates.getLastDayOfYear((jan15);   // 365 (non-leap year)
     *
     * Date feb15Leap = Dates.parseDate("2024-02-15");
     * int lastDayLeap = Dates.getLastDayOfYear((feb15Leap);   // 366 (leap year)
     * }</pre>
     *
     * @param date the date to be evaluated. Must not be {@code null}.
     * @return the last day of the year for the given date as an integer.
     * @throws IllegalArgumentException if the date is {@code null}.
     */
    public static int getLastDayOfYear(final java.util.Date date) throws IllegalArgumentException {
        N.checkArgNotNull(date, cs.date);

        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        return cal.getActualMaximum(Calendar.DAY_OF_YEAR);
    }

    /**
     * Checks if two date ranges overlap.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Check if two date ranges overlap
     * java.util.Date start1 = Dates.parse("2023-01-01");
     * java.util.Date end1 = Dates.parse("2023-01-10");
     * java.util.Date start2 = Dates.parse("2023-01-05");
     * java.util.Date end2 = Dates.parse("2023-01-15");
     * boolean overlaps = Dates.isOverlapping(start1, end1, start2, end2);
     * // Result: true (ranges overlap from Jan 5 to Jan 10)
     *
     * // Example 2: Check non-overlapping ranges
     * java.util.Date start1 = Dates.parse("2023-01-01");
     * java.util.Date end1 = Dates.parse("2023-01-10");
     * java.util.Date start2 = Dates.parse("2023-01-15");
     * java.util.Date end2 = Dates.parse("2023-01-20");
     * boolean overlaps = Dates.isOverlapping(start1, end1, start2, end2);
     * // Result: false (no overlap, there's a gap)
     *
     * // Example 3: Check adjacent ranges (touching but not overlapping)
     * java.util.Date start1 = Dates.parse("2023-01-01");
     * java.util.Date end1 = Dates.parse("2023-01-10");
     * java.util.Date start2 = Dates.parse("2023-01-10");
     * java.util.Date end2 = Dates.parse("2023-01-20");
     * boolean overlaps = Dates.isOverlapping(start1, end1, start2, end2);
     * // Result: false (ranges are adjacent but don't overlap)
     * }</pre>
     *
     * @param startTimeOne Start time of the first range. Must not be {@code null}.
     * @param endTimeOne End time of the first range. Must not be {@code null}.
     * @param startTimeTwo Start time of the second range. Must not be {@code null}.
     * @param endTimeTwo End time of the second range. Must not be {@code null}.
     * @return {@code true} if the two date ranges overlap.
     * @throws IllegalArgumentException if any date is {@code null} or invalid.
     * @see #isBetween(java.util.Date, java.util.Date, java.util.Date)
     */
    public static boolean isOverlapping(java.util.Date startTimeOne, java.util.Date endTimeOne, java.util.Date startTimeTwo, java.util.Date endTimeTwo) {
        if (startTimeOne == null || endTimeOne == null || startTimeTwo == null || endTimeTwo == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        if (startTimeOne.after(endTimeOne) || startTimeTwo.after(endTimeTwo)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        return startTimeOne.before(endTimeTwo) && startTimeTwo.before(endTimeOne);
    }

    /**
     * Checks if the given date is between the specified start date and end date, inclusive.
     * It means {@code startDate <= date <= endDate}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: Check if a date is within a range (inclusive)
     * java.util.Date checkDate = Dates.parse("2023-01-15");
     * java.util.Date startDate = Dates.parse("2023-01-01");
     * java.util.Date endDate = Dates.parse("2023-01-31");
     * boolean isWithin = Dates.isBetween(checkDate, startDate, endDate);
     * // Result: true (Jan 15 is between Jan 1 and Jan 31)
     *
     * // Example 2: Check boundary dates (inclusive)
     * java.util.Date checkDate = Dates.parse("2023-01-01");
     * java.util.Date startDate = Dates.parse("2023-01-01");
     * java.util.Date endDate = Dates.parse("2023-01-31");
     * boolean isWithin = Dates.isBetween(checkDate, startDate, endDate);
     * // Result: true (boundary dates are included)
     *
     * // Example 3: Check date outside the range
     * java.util.Date checkDate = Dates.parse("2023-02-15");
     * java.util.Date startDate = Dates.parse("2023-01-01");
     * java.util.Date endDate = Dates.parse("2023-01-31");
     * boolean isWithin = Dates.isBetween(checkDate, startDate, endDate);
     * // Result: false (Feb 15 is after Jan 31)
     *
     * // Example 4: Validate if an event date falls within a campaign period
     * java.util.Date eventDate = Dates.parse("2023-12-25");
     * java.util.Date campaignStart = Dates.parse("2023-12-01");
     * java.util.Date campaignEnd = Dates.parse("2023-12-31");
     * if (Dates.isBetween(eventDate, campaignStart, campaignEnd)) {
     *     // Event is within campaign period
     * }
     * }</pre>
     *
     * @param date the date to check. Must not be {@code null}.
     * @param startDate the start time of the range. Must not be {@code null}.
     * @param endDate the end time of the range. Must not be {@code null}.
     * @return {@code true} if the date is within the specified range.
     * @throws IllegalArgumentException if any date is {@code null} or invalid.
     * @see N#geAndLe(Comparable, Comparable, Comparable)
     * @see N#gtAndLt(Comparable, Comparable, Comparable)
     */
    @Beta
    public static boolean isBetween(java.util.Date date, java.util.Date startDate, java.util.Date endDate) {
        if (date == null || startDate == null || endDate == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        if (startDate.after(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        return N.geAndLe(date, startDate, endDate);
    }

    static void formatToForNull(final Appendable appendable) {
        try {
            appendable.append(Strings.NULL);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A comprehensive utility class providing date/time formatter constants and operations for modern Java 8+ time types.
     * This class serves as a specialized facade for {@link DateTimeFormatter} operations with extensive support
     * for parsing, formatting, and converting between different temporal types.
     *
     * <p><b>Key Features:</b>
     * <ul>
     *   <li><b>Standard Format Constants:</b> Pre-defined formatters for common date/time patterns</li>
     *   <li><b>ISO-8601 Compliance:</b> Full support for ISO standard formats</li>
     *   <li><b>Type-Safe Parsing:</b> Dedicated methods for each temporal type</li>
     *   <li><b>Null Safety:</b> Graceful handling of null inputs with {@code @MayReturnNull} annotations</li>
     *   <li><b>Performance Optimized:</b> Cached formatter instances for efficient reuse</li>
     *   <li><b>Time Zone Support:</b> Comprehensive handling of offsets and zone IDs</li>
     * </ul>
     *
     * <p><b>Supported Formats:</b>
     * <ul>
     *   <li><b>Zoned DateTime:</b> {@code ISO_ZONED_DATE_TIME_FORMAT} with timezone ID</li>
     *   <li><b>Offset DateTime:</b> {@code ISO_OFFSET_DATE_TIME_FORMAT} with UTC offset</li>
     *   <li><b>Local DateTime:</b> Standard local date-time without timezone</li>
     *   <li><b>Local Date:</b> Date-only formats without time component</li>
     *   <li><b>Local Time:</b> Time-only formats without date component</li>
     *   <li><b>Instant:</b> UTC-based instant representation</li>
     * </ul>
     *
     * <p><b>Core Operations:</b>
     * <ul>
     *   <li><b>Parsing:</b> {@code parseTo*} methods for converting strings to temporal objects</li>
     *   <li><b>Formatting:</b> {@code format} methods for temporal object to string conversion</li>
     *   <li><b>Validation:</b> Safe parsing with null return on invalid input</li>
     *   <li><b>Custom Formats:</b> Support for application-specific date/time patterns</li>
     * </ul>
     *
     * <p><b>Common Usage Patterns:</b>
     * <pre>{@code
     * // Parsing different temporal types
     * ZonedDateTime zdt = DTF.parseToZonedDateTime("2023-12-25T15:30:45-05:00[America/New_York]");
     * OffsetDateTime odt = DTF.parseToOffsetDateTime("2023-12-25T15:30:45+05:30");
     * LocalDateTime ldt = DTF.parseToLocalDateTime("2023-12-25T15:30:45");
     * LocalDate ld = DTF.parseToLocalDate("2023-12-25");
     * LocalTime lt = DTF.parseToLocalTime("15:30:45");
     * Instant instant = DTF.parseToInstant("2023-12-25T20:30:45Z");
     *
     * // Formatting temporal objects
     * String formatted = DTF.format(ZonedDateTime.now());
     * String custom = DTF.format(LocalDateTime.now(), "dd-MMM-yyyy HH:mm");
     *
     * // Custom format parsing
     * LocalDate customDate = DTF.parseToLocalDate("25/12/2023", "dd/MM/yyyy");
     * }</pre>
     *
     * <p><b>Relationship to Parent Class:</b>
     * This class extends the capabilities of {@link Dates} by providing modern Java Time API support,
     * while the parent class focuses on legacy date/time types. Use this class for:
     * <ul>
     *   <li>New code requiring Java 8+ time types</li>
     *   <li>Applications needing strong type safety</li>
     *   <li>Systems requiring immutable temporal objects</li>
     *   <li>APIs exposing time-zone aware operations</li>
     * </ul>
     *
     * <p><b>Thread Safety:</b>
     * All methods are thread-safe. {@link DateTimeFormatter} instances are immutable and can be
     * safely shared across threads without synchronization.
     * 
     * <p><b>DTF ==> Date Time Formatter</b></p>
     *
     * @see DateTimeFormatter
     * @see ZonedDateTime
     * @see OffsetDateTime
     * @see LocalDateTime
     * @see LocalDate
     * @see LocalTime
     * @see Instant
     * @see Dates
     */
    public static final class DTF {

        /**
         * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'}
         *
         * <p>ISO 8601 format with complete timezone information including offset and timezone ID.
         * Most comprehensive format for representing a moment in time with full context.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * ZonedDateTime zonedDT = ZonedDateTime.of(
         *     2023, 12, 25, 15, 30, 45, 0,
         *     java.time.ZoneId.of("America/New_York"));
         *
         * String formatted = DTF.ISO_ZONED_DATE_TIME_FORMAT.format(zonedDT);
         * // Result: "2023-12-25T15:30:45-05:00[America/New_York]"
         *
         * String zonedStr = "2023-12-25T14:25:30+05:30[Asia/Kolkata]";
         * ZonedDateTime parsed = DTF.ISO_ZONED_DATE_TIME_FORMAT.parseToZonedDateTime(zonedStr);
         * }</pre>
         *
         * @see DateTimeFormatter#ISO_ZONED_DATE_TIME
         */
        static final String ISO_ZONED_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'";

        /**
         * Date/Time format: {@code yyyy-MM-dd}
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * LocalDate date = LocalDate.of(2023, 12, 25);
         * String formatted = DTF.LOCAL_DATE.format(date);
         * // Result: "2023-12-25"
         *
         * String dateStr = "2023-12-25";
         * LocalDate parsed = DTF.LOCAL_DATE.parseToLocalDate(dateStr);
         * // Result: LocalDate representing December 25, 2023
         *
         * java.util.Date javaDate = new java.util.Date();
         * String dateFormatted = DTF.LOCAL_DATE.format(javaDate);
         * }</pre>
         *
         * @see #LOCAL_DATE_FORMAT
         */
        public static final DTF LOCAL_DATE = new DTF(Dates.LOCAL_DATE_FORMAT);

        /**
         * Date/Time format: {@code HH:mm:ss}
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * LocalTime time = LocalTime.of(15, 30, 45);
         * String formatted = DTF.LOCAL_TIME.format(time);
         * // Result: "15:30:45"
         *
         * String timeStr = "14:25:30";
         * LocalTime parsed = DTF.LOCAL_TIME.parseToLocalTime(timeStr);
         * // Result: LocalTime representing 14:25:30
         *
         * java.util.Date javaDate = new java.util.Date();
         * String timeFormatted = DTF.LOCAL_TIME.format(javaDate);
         * }</pre>
         *
         * @see #LOCAL_TIME_FORMAT
         */
        public static final DTF LOCAL_TIME = new DTF(Dates.LOCAL_TIME_FORMAT);

        /**
         * Date/Time format: {@code yyyy-MM-dd HH:mm:ss}
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * LocalDateTime dateTime = LocalDateTime.of(2023, 12, 25, 15, 30, 45);
         * String formatted = DTF.LOCAL_DATE_TIME.format(dateTime);
         * // Result: "2023-12-25 15:30:45"
         *
         * String dateTimeStr = "2023-12-25 14:25:30";
         * LocalDateTime parsed = DTF.LOCAL_DATE_TIME.parseToLocalDateTime(dateTimeStr);
         * // Result: LocalDateTime representing December 25, 2023 at 14:25:30
         *
         * java.util.Calendar calendar = java.util.Calendar.getInstance();
         * String formatted = DTF.LOCAL_DATE_TIME.format(calendar);
         * }</pre>
         *
         * @see #LOCAL_DATE_TIME_FORMAT
         */
        public static final DTF LOCAL_DATE_TIME = new DTF(Dates.LOCAL_DATE_TIME_FORMAT);

        /**
         * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ss}
         *
         * <p>ISO 8601 format without timezone information. Useful for standard date-time representation
         * in APIs and data exchange where local time (without offset) is sufficient.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * LocalDateTime dateTime = LocalDateTime.of(2023, 12, 25, 15, 30, 45);
         * String formatted = DTF.ISO_LOCAL_DATE_TIME.format(dateTime);
         * // Result: "2023-12-25T15:30:45"
         *
         * String isoStr = "2023-12-25T14:25:30";
         * LocalDateTime parsed = DTF.ISO_LOCAL_DATE_TIME.parseToLocalDateTime(isoStr);
         * // Result: LocalDateTime representing December 25, 2023 at 14:25:30
         * }</pre>
         *
         * @see #ISO_LOCAL_DATE_TIME_FORMAT
         */
        public static final DTF ISO_LOCAL_DATE_TIME = new DTF(Dates.ISO_LOCAL_DATE_TIME_FORMAT);

        /**
         * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ssXXX}
         *
         * <p>ISO 8601 format with UTC offset. Useful for representing moment in time with offset
         * information, but without timezone ID. Format includes offset like +05:30, -08:00, or Z for UTC.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * OffsetDateTime offsetDT = OffsetDateTime.of(
         *     2023, 12, 25, 15, 30, 45, 0,
         *     java.time.ZoneOffset.of("+05:30"));
         * String formatted = DTF.ISO_OFFSET_DATE_TIME.format(offsetDT);
         * // Result: "2023-12-25T15:30:45+05:30"
         *
         * String offsetStr = "2023-12-25T14:25:30-08:00";
         * OffsetDateTime parsed = DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime(offsetStr);
         * }</pre>
         *
         * @see #ISO_OFFSET_DATE_TIME_FORMAT
         */
        public static final DTF ISO_OFFSET_DATE_TIME = new DTF(Dates.ISO_OFFSET_DATE_TIME_FORMAT);

        /**
         * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'}
         *
         * <p>ISO 8601 format with timezone ID. Provides complete information including offset and
         * timezone identifier. Most comprehensive format for full datetime representation.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * ZonedDateTime zonedDT = ZonedDateTime.of(
         *     2023, 12, 25, 15, 30, 45, 0,
         *     java.time.ZoneId.of("Asia/Kolkata"));
         * String formatted = DTF.ISO_ZONED_DATE_TIME.format(zonedDT);
         * // Result: "2023-12-25T15:30:45+05:30[Asia/Kolkata]"
         *
         * String zonedStr = "2023-12-25T14:25:30-08:00[America/Los_Angeles]";
         * ZonedDateTime parsed = DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime(zonedStr);
         * }</pre>
         *
         * @see #ISO_ZONED_DATE_TIME_FORMAT
         */
        public static final DTF ISO_ZONED_DATE_TIME = new DTF(ISO_ZONED_DATE_TIME_FORMAT);

        /**
         * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ss'Z'}.
         *
         * <p>ISO 8601 UTC format (Zulu time). Always represents UTC time with the 'Z' suffix.
         * Useful for API communication and logging where UTC time is preferred.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Instant instant = Instant.now();
         * String formatted = DTF.ISO_8601_DATE_TIME.format(instant);
         * // Result: "2023-12-25T15:30:45Z"
         *
         * String utcStr = "2023-12-25T14:25:30Z";
         * Instant parsed = DTF.ISO_8601_DATE_TIME.parseToInstant(utcStr);
         *
         * ZonedDateTime utcTime = ZonedDateTime.of(
         *     2023, 12, 25, 15, 30, 45, 0,
         *     java.time.ZoneId.of("UTC"));
         * String formatted = DTF.ISO_8601_DATE_TIME.format(utcTime);
         * }</pre>
         *
         * @see #ISO_8601_DATE_TIME_FORMAT
         */
        public static final DTF ISO_8601_DATE_TIME = new DTF(Dates.ISO_8601_DATE_TIME_FORMAT);

        /**
         * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}.
         *
         * <p>ISO 8601 UTC timestamp format with milliseconds. Provides higher precision than ISO_8601_DATE_TIME
         * by including milliseconds. Ideal for high-precision logging and timing operations.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Instant instant = Instant.now();
         * String formatted = DTF.ISO_8601_TIMESTAMP.format(instant);
         * // Result: "2023-12-25T15:30:45.123Z"
         *
         * String timestampStr = "2023-12-25T14:25:30.456Z";
         * Instant parsed = DTF.ISO_8601_TIMESTAMP.parseToInstant(timestampStr);
         *
         * long currentTimeMillis = System.currentTimeMillis();
         * java.util.Date date = new java.util.Date(currentTimeMillis);
         * String formatted = DTF.ISO_8601_TIMESTAMP.format(date);
         * }</pre>
         *
         * @see #ISO_8601_TIMESTAMP_FORMAT
         */
        public static final DTF ISO_8601_TIMESTAMP = new DTF(Dates.ISO_8601_TIMESTAMP_FORMAT);

        /**
         * Date/Time format: {@code EEE, dd MMM yyyy HH:mm:ss zzz}.
         *
         * <p>RFC 1123 date-time format (HTTP date format). Human-readable format used in HTTP headers,
         * emails, and other internet protocols. Includes day of week, abbreviated month name, and timezone.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * ZonedDateTime zonedDT = ZonedDateTime.now(java.time.ZoneId.of("UTC"));
         * String formatted = DTF.RFC_1123_DATE_TIME.format(zonedDT);
         * // Result: "Mon, 25 Dec 2023 15:30:45 GMT"
         *
         * String rfcStr = "Mon, 25 Dec 2023 14:25:30 GMT";
         * ZonedDateTime parsed = DTF.RFC_1123_DATE_TIME.parseToZonedDateTime(rfcStr);
         *
         * java.util.Calendar calendar = java.util.Calendar.getInstance();
         * String httpHeader = DTF.RFC_1123_DATE_TIME.format(calendar);
         * // Can be used directly in HTTP headers like Date or Expires
         * }</pre>
         *
         * @see #RFC_1123_DATE_TIME_FORMAT
         */
        public static final DTF RFC_1123_DATE_TIME = new DTF(Dates.RFC_1123_DATE_TIME_FORMAT);

        private final String format;
        private final DateTimeFormatter dateTimeFormatter;

        DTF(final String format) {
            this.format = format;
            dateTimeFormatter = DateTimeFormatter.ofPattern(format);
        }

        //    DTF(final DateTimeFormatter dtf) {

        /**
         * Formats the provided java.util.Date instance into a string representation.
         *
         * @param date the java.util.Date instance to format.
         * @return a string representation of the provided java.util.Date instance.
         * @throws DateTimeException if an error occurs during formatting.
         * @see DateTimeFormatter#format(TemporalAccessor)
         */
        @MayReturnNull
        public String format(final java.util.Date date) {
            if (date == null) {
                return null;
            }

            return Dates.format(date, format);
        }

        /**
         * Formats the provided java.util.Calendar instance into a string representation.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Calendar calendar = Calendar.getInstance();
         * String formatted = DTF.LOCAL_DATE_TIME.format(calendar);   // "2023-12-25 14:30:45"
         * }</pre>
         *
         * @param calendar the java.util.Calendar instance to format.
         * @return a string representation of the provided java.util.Calendar instance.
         * @throws DateTimeException if an error occurs during formatting.
         * @see DateTimeFormatter#format(TemporalAccessor)
         */
        @MayReturnNull
        public String format(final java.util.Calendar calendar) {
            if (calendar == null) {
                return null;
            }

            return Dates.format(calendar, format);
        }

        /**
         * Formats the provided TemporalAccessor instance into a string representation.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * LocalDateTime ldt = LocalDateTime.now();
         * String formatted = DTF.LOCAL_DATE_TIME.format(ldt);   // "2023-12-25 14:30:45"
         *
         * ZonedDateTime zdt = ZonedDateTime.now();
         * String isoFormatted = DTF.ISO_8601_DATE_TIME.format(zdt);   // "2023-12-25T14:30:45Z"
         * }</pre>
         *
         * @param temporal the TemporalAccessor instance to format.
         * @return a string representation of the provided TemporalAccessor instance.
         * @throws DateTimeException if an error occurs during formatting.
         * @see DateTimeFormatter#format(TemporalAccessor)
         */
        @MayReturnNull
        public String format(final TemporalAccessor temporal) {
            if (temporal == null) {
                return null;
            }

            return dateTimeFormatter.format(temporal);
        }

        /**
         * Formats the provided java.util.Date instance into a string representation and appends it to the provided Appendable.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Date date = new Date();
         * StringBuilder sb = new StringBuilder();
         * DTF.LOCAL_DATE_TIME.formatTo(date, sb);
         * // sb now contains: "2023-12-25 14:30:45"
         * }</pre>
         *
         * @param date the java.util.Date instance to format.
         * @param appendable the Appendable to which the formatted string will be appended.
         * @throws DateTimeException if an error occurs during formatting.
         * @see DateTimeFormatter#formatTo(TemporalAccessor, Appendable)
         */
        @MayReturnNull
        public void formatTo(final java.util.Date date, final Appendable appendable) {
            if (date == null) {
                formatToForNull(appendable);
                return;
            }

            Dates.formatTo(date, format, appendable);
        }

        /**
         * Formats the provided java.util.Calendar instance into a string representation and appends it to the provided Appendable.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Calendar calendar = Calendar.getInstance();
         * StringBuilder sb = new StringBuilder();
         * DTF.LOCAL_DATE_TIME.formatTo(calendar, sb);
         * // sb now contains: "2023-12-25 14:30:45"
         * }</pre>
         *
         * @param calendar the java.util.Calendar instance to format.
         * @param appendable the Appendable to which the formatted string will be appended.
         * @throws DateTimeException if an error occurs during formatting.
         * @see DateTimeFormatter#formatTo(TemporalAccessor, Appendable)
         */
        @MayReturnNull
        public void formatTo(final java.util.Calendar calendar, final Appendable appendable) {
            if (calendar == null) {
                formatToForNull(appendable);
                return;
            }

            Dates.formatTo(calendar, format, appendable);
        }

        /**
         * Formats the provided TemporalAccessor instance into a string representation and appends it to the provided Appendable.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * LocalDateTime ldt = LocalDateTime.now();
         * StringBuilder sb = new StringBuilder();
         * DTF.LOCAL_DATE_TIME.formatTo(ldt, sb);
         * // sb now contains: "2023-12-25 14:30:45"
         * }</pre>
         *
         * @param temporal the TemporalAccessor instance to format.
         * @param appendable the Appendable to which the formatted string will be appended.
         * @throws DateTimeException if an error occurs during formatting.
         * @see DateTimeFormatter#formatTo(TemporalAccessor, Appendable)
         */
        @MayReturnNull
        public void formatTo(final TemporalAccessor temporal, final Appendable appendable) {
            if (temporal == null) {
                formatToForNull(appendable);
                return;
            }

            dateTimeFormatter.formatTo(temporal, appendable);
        }

        /**
         * Parses the provided CharSequence into a LocalDate instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String dateStr = "2023-12-25";
         * LocalDate date = DTF.LOCAL_DATE.parseToLocalDate(dateStr);   // 2023-12-25
         *
         * // Can also parse from milliseconds
         * String millis = "1672585530123";
         * LocalDate fromMillis = DTF.LOCAL_DATE.parseToLocalDate(millis);
         * }</pre>
         *
         * @param text the CharSequence to parse. Must not be {@code null}.
         * @return a LocalDate instance representing the parsed date.
         * @throws DateTimeParseException if the text cannot be parsed to a date.
         * @see Instant#ofEpochMilli(long)
         * @see LocalDate#ofInstant(Instant, ZoneId)
         * @see LocalDate#from(TemporalAccessor)
         */
        @MayReturnNull
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

            switch (this.format) {
                case LOCAL_DATE_FORMAT -> {
                    return LocalDate.parse(text, dateTimeFormatter);
                }
                case ISO_ZONED_DATE_TIME_FORMAT -> {
                    return ZonedDateTime.parse(text, dateTimeFormatter).toLocalDate();
                }
                case ISO_OFFSET_DATE_TIME_FORMAT -> {
                    return OffsetDateTime.parse(text, dateTimeFormatter).toLocalDate();
                }
                default -> {
                    // return LocalDate.from(parseToTemporalAccessor(text));;
                    final Calendar cal = parseToCalendar(text);
                    return LocalDate.ofInstant(cal.toInstant(), cal.getTimeZone().toZoneId());
                }
            }
        }

        /**
         * Parses the provided CharSequence into a LocalTime instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String timeStr = "14:30:45";
         * LocalTime time = DTF.LOCAL_TIME.parseToLocalTime(timeStr);   // 14:30:45
         *
         * // Can also parse from milliseconds
         * String millis = "1672585530123";
         * LocalTime fromMillis = DTF.LOCAL_TIME.parseToLocalTime(millis);
         * }</pre>
         *
         * @param text the CharSequence to parse. Must not be {@code null}.
         * @return a LocalTime instance representing the parsed time.
         * @throws DateTimeParseException if the text cannot be parsed to a time.
         * @see Instant#ofEpochMilli(long)
         * @see LocalTime#ofInstant(Instant, ZoneId)
         * @see LocalTime#from(TemporalAccessor)
         */
        @MayReturnNull
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

            switch (this.format) {
                case LOCAL_TIME_FORMAT -> {
                    return LocalTime.parse(text, dateTimeFormatter);
                }
                case ISO_ZONED_DATE_TIME_FORMAT -> {
                    return ZonedDateTime.parse(text, dateTimeFormatter).toLocalTime();
                }
                case ISO_OFFSET_DATE_TIME_FORMAT -> {
                    return OffsetDateTime.parse(text, dateTimeFormatter).toLocalTime();
                }
                default -> {
                    // return LocalTime.from(parseToTemporalAccessor(text));
                    final Calendar cal = parseToCalendar(text);
                    return LocalTime.ofInstant(cal.toInstant(), cal.getTimeZone().toZoneId());
                }
            }
        }

        /**
         * Parses the provided CharSequence into a LocalDateTime instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String dateTimeStr = "2023-12-25 14:30:45";
         * LocalDateTime ldt = DTF.LOCAL_DATE_TIME.parseToLocalDateTime(dateTimeStr);
         *
         * // Can also parse from milliseconds
         * String millis = "1672585530123";
         * LocalDateTime fromMillis = DTF.LOCAL_DATE_TIME.parseToLocalDateTime(millis);
         * }</pre>
         *
         * @param text the CharSequence to parse. Must not be {@code null}.
         * @return a LocalDateTime instance representing the parsed date and time.
         * @throws DateTimeParseException if the text cannot be parsed to a date and time.
         * @see Instant#ofEpochMilli(long)
         * @see LocalDateTime#ofInstant(Instant, ZoneId)
         * @see LocalDateTime#from(TemporalAccessor)
         */
        @MayReturnNull
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

            switch (this.format) {
                case LOCAL_DATE_TIME_FORMAT -> {
                    return LocalDateTime.parse(text, dateTimeFormatter);
                }
                case ISO_ZONED_DATE_TIME_FORMAT -> {
                    return ZonedDateTime.parse(text, dateTimeFormatter).toLocalDateTime();
                }
                case ISO_OFFSET_DATE_TIME_FORMAT -> {
                    return OffsetDateTime.parse(text, dateTimeFormatter).toLocalDateTime();
                }
                default -> {
                    // return LocalDateTime.from(parseToTemporalAccessor(text));
                    final Calendar cal = parseToCalendar(text);
                    return LocalDateTime.ofInstant(cal.toInstant(), cal.getTimeZone().toZoneId());
                }
            }
        }

        /**
         * Parses the provided CharSequence into an OffsetDateTime instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String offsetStr = "2023-12-25T14:30:45+05:30";
         * OffsetDateTime odt = DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime(offsetStr);
         *
         * // Can also parse from milliseconds
         * String millis = "1672585530123";
         * OffsetDateTime fromMillis = DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime(millis);
         * }</pre>
         *
         * @param text the CharSequence to parse. Must not be {@code null}.
         * @return an OffsetDateTime instance representing the parsed date and time.
         * @throws DateTimeParseException if the text cannot be parsed to a date and time.
         * @see Instant#ofEpochMilli(long)
         * @see OffsetDateTime#ofInstant(Instant, ZoneId)
         * @see OffsetDateTime#from(TemporalAccessor)
         */
        @MayReturnNull
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

            if (this.format.equals(ISO_OFFSET_DATE_TIME_FORMAT)) {
                return OffsetDateTime.parse(text, dateTimeFormatter);
            } else if (this.format.equals(ISO_ZONED_DATE_TIME_FORMAT)) {
                return ZonedDateTime.parse(text, dateTimeFormatter).toOffsetDateTime(); // Convert ZonedDateTime to OffsetDateTime
            } else {
                // return OffsetDateTime.from(parseToTemporalAccessor(text));
                final Calendar cal = parseToCalendar(text);
                return OffsetDateTime.ofInstant(cal.toInstant(), cal.getTimeZone().toZoneId());
            }
        }

        /**
         * Parses the provided CharSequence into a ZonedDateTime instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String zonedStr = "2023-12-25T14:30:45+05:30[Asia/Kolkata]";
         * ZonedDateTime zdt = DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime(zonedStr);
         *
         * // Can also parse from milliseconds
         * String millis = "1672585530123";
         * ZonedDateTime fromMillis = DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime(millis);
         * }</pre>
         *
         * @param text the CharSequence to parse. Must not be {@code null}.
         * @return a ZonedDateTime instance representing the parsed date and time.
         * @throws DateTimeParseException if the text cannot be parsed to a date and time.
         * @see Instant#ofEpochMilli(long)
         * @see OffsetDateTime#ofInstant(Instant, ZoneId)
         * @see OffsetDateTime#from(TemporalAccessor)
         */
        @MayReturnNull
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

            if (this.format.equals(ISO_ZONED_DATE_TIME_FORMAT)) {
                return ZonedDateTime.parse(text, dateTimeFormatter);
            } else if (this.format.equals(ISO_OFFSET_DATE_TIME_FORMAT)) {
                return OffsetDateTime.parse(text, dateTimeFormatter).toZonedDateTime();
            } else {
                // return ZonedDateTime.from(parseToTemporalAccessor(text));
                final Calendar cal = parseToCalendar(text);
                return ZonedDateTime.ofInstant(cal.toInstant(), cal.getTimeZone().toZoneId());
            }
        }

        /**
         * Parses the provided CharSequence into an Instant instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String instantStr = "2023-12-25T14:30:45Z";
         * Instant instant = DTF.ISO_8601_DATE_TIME.parseToInstant(instantStr);
         *
         * // Can also parse from milliseconds
         * String millis = "1672585530123";
         * Instant fromMillis = DTF.ISO_8601_DATE_TIME.parseToInstant(millis);
         * }</pre>
         *
         * @param text the CharSequence to parse. Must not be {@code null}.
         * @return an Instant instance representing the parsed date and time.
         * @throws DateTimeParseException if the text cannot be parsed to a date and time.
         * @see Instant#ofEpochMilli(long)
         * @see Instant#from(TemporalAccessor)
         */
        @MayReturnNull
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

            if (this.format.equals(ISO_ZONED_DATE_TIME_FORMAT)) {
                return ZonedDateTime.parse(text, dateTimeFormatter).toInstant();
            } else if (this.format.equals(ISO_OFFSET_DATE_TIME_FORMAT)) {
                return OffsetDateTime.parse(text, dateTimeFormatter).toInstant();
            } else {
                return parseToTimestamp(text).toInstant();
            }
        }

        /**
         * Parses the provided CharSequence into a java.util.Date instance.
         *
         * @param text the CharSequence to parse. Must not be {@code null}.
         * @return a java.util.Date instance representing the parsed date and time.
         * @throws DateTimeParseException if the text cannot be parsed to a date and time.
         * @see Instant#ofEpochMilli(long)
         * @see java.util.Date#from(Instant)
         */
        @MayReturnNull
        public java.util.Date parseToJUDate(final CharSequence text) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            return Dates.parseJUDate(text.toString(), format);
        }

        /**
         * Parses the provided CharSequence into a java.util.Date instance.
         *
         * @param text the CharSequence to parse. Must not be {@code null}.
         * @param tz the time zone to use for parsing the date and time.
         * @return a java.util.Date instance representing the parsed date and time.
         * @throws DateTimeParseException if the text cannot be parsed to a date and time.
         * @see Instant#ofEpochMilli(long)
         * @see java.util.Date#from(Instant)
         */
        @MayReturnNull
        public java.util.Date parseToJUDate(final CharSequence text, final TimeZone tz) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            return Dates.parseJUDate(text.toString(), format, tz);
        }

        /**
         * Parses the provided CharSequence into a java.sql.Date instance.
         *
         * @param text the CharSequence to parse. Must not be {@code null}.
         * @return a java.sql.Date instance representing the parsed date and time.
         * @throws DateTimeParseException if the text cannot be parsed to a date and time.
         * @see Instant#ofEpochMilli(long)
         * @see java.sql.Date#from(Instant)
         */
        @MayReturnNull
        public java.sql.Date parseToDate(final CharSequence text) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            return Dates.parseDate(text.toString(), format);
        }

        /**
         * Parses the provided CharSequence into a java.sql.Date instance.
         *
         * @param text the CharSequence to parse. Must not be {@code null}.
         * @param tz the time zone to use for parsing the date and time.
         * @return a java.sql.Date instance representing the parsed date and time.
         * @throws DateTimeParseException if the text cannot be parsed to a date and time.
         * @see Instant#ofEpochMilli(long)
         * @see java.sql.Date#from(Instant)
         */
        @MayReturnNull
        public java.sql.Date parseToDate(final CharSequence text, final TimeZone tz) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            return Dates.parseDate(text.toString(), format, tz);
        }

        /**
         * Parses the provided CharSequence into a java.sql.Time instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String timeStr = "14:30:45";
         * Time time = DTF.LOCAL_TIME.parseToTime(timeStr);
         * }</pre>
         *
         * @param text the CharSequence to parse. Must not be {@code null}.
         * @return a java.sql.Time instance representing the parsed date and time.
         * @throws DateTimeParseException if the text cannot be parsed to a date and time.
         * @see Instant#ofEpochMilli(long)
         * @see java.sql.Time#from(Instant)
         */
        @MayReturnNull
        public Time parseToTime(final CharSequence text) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            return Dates.parseTime(text.toString(), format);
        }

        /**
         * Parses the provided CharSequence into a java.sql.Time instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String timeStr = "14:30:45";
         * TimeZone utc = TimeZone.getTimeZone("UTC");
         * Time time = DTF.LOCAL_TIME.parseToTime(timeStr, utc);
         * }</pre>
         *
         * @param text the CharSequence to parse. Must not be {@code null}.
         * @param tz the time zone to use for parsing the date and time.
         * @return a java.sql.Time instance representing the parsed date and time.
         * @throws DateTimeParseException if the text cannot be parsed to a date and time.
         * @see Instant#ofEpochMilli(long)
         * @see java.sql.Time#from(Instant)
         */
        @MayReturnNull
        public Time parseToTime(final CharSequence text, final TimeZone tz) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            return Dates.parseTime(text.toString(), format, tz);
        }

        /**
         * Parses the provided CharSequence into a java.sql.Timestamp instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String timestampStr = "2023-12-25 14:30:45.123";
         * Timestamp timestamp = DTF.LOCAL_TIMESTAMP.parseToTimestamp(timestampStr);
         * }</pre>
         *
         * @param text the CharSequence to parse. Must not be {@code null}.
         * @return a java.sql.Timestamp instance representing the parsed date and time.
         * @throws DateTimeParseException if the text cannot be parsed to a date and time.
         * @see Instant#ofEpochMilli(long)
         * @see java.sql.Timestamp#from(Instant)
         */
        @MayReturnNull
        public Timestamp parseToTimestamp(final CharSequence text) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            return Dates.parseTimestamp(text.toString(), format);
        }

        /**
         * Parses the provided CharSequence into a java.sql.Timestamp instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String timestampStr = "2023-12-25 14:30:45.123";
         * TimeZone utc = TimeZone.getTimeZone("UTC");
         * Timestamp timestamp = DTF.LOCAL_TIMESTAMP.parseToTimestamp(timestampStr, utc);
         * }</pre>
         *
         * @param text the CharSequence to parse. Must not be {@code null}.
         * @param tz the time zone to use for parsing the date and time.
         * @return a java.sql.Timestamp instance representing the parsed date and time.
         * @throws DateTimeParseException if the text cannot be parsed to a date and time.
         * @see Instant#ofEpochMilli(long)
         * @see java.sql.Timestamp#from(Instant)
         */
        @MayReturnNull
        public Timestamp parseToTimestamp(final CharSequence text, final TimeZone tz) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            return Dates.parseTimestamp(text.toString(), format, tz);
        }

        /**
         * Parses the provided CharSequence into a java.util.Calendar instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String dateTimeStr = "2023-12-25 14:30:45";
         * Calendar calendar = DTF.LOCAL_DATE_TIME.parseToCalendar(dateTimeStr);
         * }</pre>
         *
         * @param text the CharSequence to parse. Must not be {@code null}.
         * @return a java.util.Calendar instance representing the parsed date and time.
         * @throws DateTimeParseException if the text cannot be parsed to a date and time.
         */
        @MayReturnNull
        public Calendar parseToCalendar(final CharSequence text) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            return Dates.parseCalendar(text.toString(), format);
        }

        /**
         * Parses the provided CharSequence into a java.util.Calendar instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String dateTimeStr = "2023-12-25 14:30:45";
         * TimeZone utc = TimeZone.getTimeZone("UTC");
         * Calendar calendar = DTF.LOCAL_DATE_TIME.parseToCalendar(dateTimeStr, utc);
         * }</pre>
         *
         * @param text the CharSequence to parse. Must not be {@code null}.
         * @param tz the time zone to use for parsing the date and time.
         * @return a java.util.Calendar instance representing the parsed date and time.
         * @throws DateTimeParseException if the text cannot be parsed to a date and time.
         */
        @MayReturnNull
        public Calendar parseToCalendar(final CharSequence text, final TimeZone tz) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            return Dates.parseCalendar(text.toString(), format, tz);
        }

        /**
         * Parses the provided CharSequence into a TemporalAccessor instance using the underlying DateTimeFormatter.
         * This is a low-level parsing method that returns a TemporalAccessor, which can represent various temporal types
         * depending on the format pattern (e.g., LocalDate, LocalDateTime, ZonedDateTime).
         *
         * <p>This method is typically used internally or when you need direct access to the parsed temporal fields
         * without converting to a specific date/time type. For most use cases, prefer the more specific parse methods
         * like {@link #parseToLocalDateTime(CharSequence)}, {@link #parseToZonedDateTime(CharSequence)}, etc.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Parse to a TemporalAccessor and query specific fields
         * DTF formatter = new DTF("yyyy-MM-dd HH:mm:ss");
         * TemporalAccessor temporal = formatter.parseToTemporalAccessor("2023-12-25 15:30:45");
         * int year = temporal.get(ChronoField.YEAR);             // 2023
         * int month = temporal.get(ChronoField.MONTH_OF_YEAR);   // 12
         * int day = temporal.get(ChronoField.DAY_OF_MONTH);      // 25
         *
         * // Convert to specific type if needed
         * LocalDateTime ldt = LocalDateTime.from(temporal);
         * }</pre>
         *
         * @param text the CharSequence to parse. Must not be {@code null}.
         * @return a TemporalAccessor instance representing the parsed date and time.
         * @throws DateTimeParseException if the text cannot be parsed according to the format pattern.
         * @see DateTimeFormatter#parse(CharSequence)
         * @see TemporalAccessor
         * @see #parseToLocalDateTime(CharSequence)
         * @see #parseToZonedDateTime(CharSequence)
         */
        TemporalAccessor parseToTemporalAccessor(final CharSequence text) {

            return dateTimeFormatter.parse(text);
        }

        @Override
        public String toString() {
            return format;
        }
    }

    /**
     * The Class DateUtil.
     */
    @Beta
    public static final class DateUtil extends Dates {

        private DateUtil() {
            // singleton.
        }
    }

    //    /**

}
