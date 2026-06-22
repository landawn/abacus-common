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
import java.text.ParsePosition;
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
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.LongFunction;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.function.LongObjFunction;

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
 *   <li><b>Type Conversion:</b> Conversion among the legacy date/time types, plus parsing/formatting of modern Java time types via the nested {@link DTF} formatter class</li>
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
 * <p><b>Naming Convention:</b> in this class, the unqualified names {@code Date}, {@code Time} and
 * {@code Timestamp} always refer to the {@code java.sql} types, which get the short method names
 * ({@code currentDate}, {@code parseDate}, {@code createDate}, ...), while {@code java.util.Date}
 * is consistently named {@code JUDate} ({@code currentJUDate}, {@code parseJUDate}, {@code createJUDate}, ...).
 * The {@link DateUtil} subclass is a name-only alias for these same utilities; {@code Dates} is the
 * canonical name and should be preferred in new code.
 *
 * <p><b>java.sql Date/Time Parsing Rules:</b> formatted inputs parsed by {@code parseDate} and
 * {@code parseTime} are normalized per the JDBC contracts ({@code java.sql.Date} is truncated to
 * midnight in the parsing zone; {@code java.sql.Time} has its date portion normalized to 1970-01-01).
 * Pure numeric epoch-millisecond strings are treated as raw instants and preserve their
 * {@code getTime()} value exactly, matching {@code createDate(long)} and {@code createTime(long)}.
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
 *   <li><b>Arithmetic:</b> add*, set* methods for date/time calculations</li>
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
 * String formatted = Dates.format(Dates.currentJUDate());
 * String custom = Dates.format(Dates.currentJUDate(), "dd-MMM-yyyy");
 * String utc = Dates.format(Dates.currentJUDate(), Dates.ISO_8601_DATE_TIME_FORMAT, Dates.UTC_TIME_ZONE);
 *
 * // Date arithmetic
 * Date nextWeek = Dates.addDays(today, 7);
 * Date nextMonth = Dates.addMonths(today, 1);
 * Date startOfDay = Dates.truncate(today, Calendar.DAY_OF_MONTH);
 *
 * // Comparisons and checks
 * boolean sameDay = Dates.isSameDay(date1, date2);
 * boolean isLastDay = Dates.isLastDayOfMonth(today);
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
 * // Date/time arithmetic by field
 * Date future = Dates.addDays(date, 30);
 * Calendar laterCal = Dates.addMonths(calendar, 6);
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
 *   <li><b>Controlled Mutable State:</b> Internal caches and creator registries use concurrent collections</li>
 *   <li><b>Concurrent Access:</b> Safe for concurrent access from multiple threads</li>
 * </ul>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li><b>Null Safety:</b> Most methods handle null inputs gracefully, returning null or appropriate defaults</li>
 *   <li><b>Parse Failure Handling:</b> Parsing methods return {@code null} for {@code null}, empty, or {@code "null"} input, and throw {@code IllegalArgumentException} when a non-empty string cannot be parsed. Explicit ISO-8601 UTC formats must match the entire input; other {@link java.text.SimpleDateFormat}-backed explicit formats keep their legacy prefix parsing behavior</li>
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
 *   <li><b>Conversion Support:</b> Parsing/formatting of modern types via the nested {@code DTF} class</li>
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
 *   <li>Manual date arithmetic → Use {@code add*()} and {@code set*()} methods</li>
 *   <li>Custom parsing logic → Use {@code parse*()} methods with format detection</li>
 *   <li>Time zone conversions → Use methods with explicit {@code TimeZone} parameters</li>
 * </ul>
 *
 * <p><b>Attribution:</b>
 * This class includes code adapted from Apache Commons Lang, Google Guava, and other
 * open source projects under the Apache License 2.0. Methods from these libraries may have been
 * modified for consistency, performance optimization, and null-safety enhancement.
 *
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
     * Default {@code TimeZone} of the Java virtual machine.
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
     * When comparing other time zones, you can express them as offsets from GMT/UTC, such as GMT+2 or GMT-5.
     */
    public static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");

    /**
     * Default {@code ZoneId} of the Java virtual machine.
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
     * Date/Time format: {@code yyyy}.
     *
     * <p>Year-only format. Useful for grouping or filtering by year.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String yearStr = "2023";
     *
     * // Represents January 1, 2023
     * Date date = Dates.parseDate(yearStr, Dates.LOCAL_YEAR_FORMAT);
     *
     * java.util.Date today = Dates.currentJUDate();
     * // Result: "2023"
     * String year = Dates.format(today, Dates.LOCAL_YEAR_FORMAT);
     *
     * Calendar calendar = Calendar.getInstance();
     * String formattedYear = Dates.format(calendar, Dates.LOCAL_YEAR_FORMAT);
     * }</pre>
     *
     */
    public static final String LOCAL_YEAR_FORMAT = "yyyy";

    /**
     * Date/Time format: {@code MM-dd}.
     *
     * <p>Month and day format without year. Useful for recurring dates, anniversaries, and seasonal data.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String monthDayStr = "12-25";
     *
     * // Represents December 25, 1970 (the pattern carries no year, so 1970 is assumed)
     * Date date = Dates.parseDate(monthDayStr, Dates.LOCAL_MONTH_DAY_FORMAT);
     *
     * java.util.Date birthday = Dates.currentJUDate();
     * // Result: "12-25" for December 25th
     * String monthDay = Dates.format(birthday, Dates.LOCAL_MONTH_DAY_FORMAT);
     * // Useful for anniversaries without year
     * Calendar anniversary = Calendar.getInstance();
     * String anniversaryStr = Dates.format(anniversary, Dates.LOCAL_MONTH_DAY_FORMAT);
     * }</pre>
     *
     */
    public static final String LOCAL_MONTH_DAY_FORMAT = "MM-dd";

    // static final String LOCAL_MONTH_DAY_FORMAT_SLASH = "MM/dd";

    /**
     * Date/Time format: {@code yyyy-MM-dd}.
     *
     * <p>ISO 8601 date format without time. Standard format for representing dates in data exchange and databases.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String dateStr = "2023-12-25";
     * Date date = Dates.parseDate(dateStr, Dates.LOCAL_DATE_FORMAT);
     *
     * java.util.Date today = Dates.currentJUDate();
     * // Result: "2023-12-25"
     * String formatted = Dates.format(today, Dates.LOCAL_DATE_FORMAT);
     *
     * java.util.Calendar calendar = java.util.Calendar.getInstance();
     * String calendarDate = Dates.format(calendar, Dates.LOCAL_DATE_FORMAT);
     * // Using DTF instance for convenience
     * String dtfFormatted = DTF.LOCAL_DATE.format(today);
     * }</pre>
     *
     */
    public static final String LOCAL_DATE_FORMAT = "yyyy-MM-dd";

    // static final String LOCAL_DATE_FORMAT_SLASH = "yyyy/MM/dd";

    /**
     * Date/Time format: {@code HH:mm:ss}.
     *
     * <p>Time-only format in 24-hour notation. Useful for representing times without date context.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String timeStr = "14:30:45";
     * Time time = Dates.parseTime(timeStr, Dates.LOCAL_TIME_FORMAT);
     *
     * java.util.Date now = Dates.currentJUDate();
     * // Result: "14:30:45"
     * String formatted = Dates.format(now, Dates.LOCAL_TIME_FORMAT);
     *
     * java.util.Calendar calendar = java.util.Calendar.getInstance();
     * String calendarTime = Dates.format(calendar, Dates.LOCAL_TIME_FORMAT);
     * // Using DTF instance
     * String dtfFormatted = DTF.LOCAL_TIME.format(now);
     * }</pre>
     *
     */
    public static final String LOCAL_TIME_FORMAT = "HH:mm:ss";

    /**
     * Date/Time format: {@code yyyy-MM-dd HH:mm:ss}.
     *
     * <p>Local date and time format without timezone. Useful for database timestamps and local system times.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String dateTimeStr = "2023-12-25 14:30:45";
     * Date date = Dates.parseDate(dateTimeStr, Dates.LOCAL_DATE_TIME_FORMAT);
     *
     * java.util.Date now = Dates.currentJUDate();
     * // Result: "2023-12-25 14:30:45"
     * String formatted = Dates.format(now, Dates.LOCAL_DATE_TIME_FORMAT);
     *
     * java.util.Calendar calendar = java.util.Calendar.getInstance();
     * String calendarDateTime = Dates.format(calendar, Dates.LOCAL_DATE_TIME_FORMAT);
     * // Using DTF instance
     * String dtfFormatted = DTF.LOCAL_DATE_TIME.format(now);
     * }</pre>
     *
     */
    public static final String LOCAL_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * Date/Time format: {@code yyyy-MM-dd HH:mm:ss.SSS}.
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
     * // Result: "2023-12-25 14:30:45.123"
     * String formatted = Dates.format(now, Dates.LOCAL_TIMESTAMP_FORMAT);
     *
     * java.util.Calendar calendar = java.util.Calendar.getInstance();
     * String calendarTimestamp = Dates.format(calendar, Dates.LOCAL_TIMESTAMP_FORMAT);
     * // High precision logging
     * System.out.println("Event occurred at: " + Dates.format(Dates.currentJUDate(), Dates.LOCAL_TIMESTAMP_FORMAT));
     * }</pre>
     *
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
     * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ss}.
     *
     * <p>ISO 8601 local date and time format without timezone. Standard format for data interchange
     * where the timezone is implicit or handled separately.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String isoStr = "2023-12-25T14:30:45";
     * Date date = Dates.parseDate(isoStr, Dates.ISO_LOCAL_DATE_TIME_FORMAT);
     *
     * java.util.Date now = Dates.currentJUDate();
     * // Result: "2023-12-25T14:30:45"
     * String formatted = Dates.format(now, Dates.ISO_LOCAL_DATE_TIME_FORMAT);
     * // Using DTF instance for convenience
     * String dtfFormatted = DTF.ISO_LOCAL_DATE_TIME.format(now);
     *
     * java.time.LocalDateTime localDateTime = java.time.LocalDateTime.now();
     * String isoFormatted = DTF.ISO_LOCAL_DATE_TIME.format(localDateTime);
     * }</pre>
     *
     * @see DateTimeFormatter#ISO_LOCAL_DATE_TIME
     */
    public static final String ISO_LOCAL_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ssXXX}.
     *
     * <p>ISO 8601 date and time with UTC offset. Useful for APIs and services that need to represent
     * a moment in time with explicit offset but without timezone identification.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String offsetStr = "2023-12-25T14:30:45+05:30";
     * Date date = Dates.parseDate(offsetStr, Dates.ISO_OFFSET_DATE_TIME_FORMAT);
     * // Using DTF instance with OffsetDateTime
     * java.time.OffsetDateTime offsetDT = java.time.OffsetDateTime.now();
     * // Result: "2023-12-25T14:30:45+05:30"
     * String formatted = DTF.ISO_OFFSET_DATE_TIME.format(offsetDT);
     *
     * // Parse with offset
     * String utcStr = "2023-12-25T14:30:45Z";
     * java.time.OffsetDateTime parsed = DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime(utcStr);
     * }</pre>
     *
     * @see DateTimeFormatter#ISO_OFFSET_DATE_TIME
     */
    public static final String ISO_OFFSET_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

    /**
     * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'}.
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
     * // Result: "2023-12-25T15:30:45-05:00[America/New_York]"
     * String formatted = DTF.ISO_ZONED_DATE_TIME.format(zonedDT);
     *
     * String zonedStr = "2023-12-25T14:25:30+05:30[Asia/Kolkata]";
     * ZonedDateTime parsed = DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime(zonedStr);
     * }</pre>
     *
     * <p>Unlike the sibling {@link SimpleDateFormat}-compatible constants, the {@code XXX'['VV']'}
     * portion is only meaningful to {@link DateTimeFormatter}. Prefer {@code DTF.ISO_ZONED_DATE_TIME}
     * for parsing or formatting this pattern.</p>
     *
     * @see DateTimeFormatter#ISO_ZONED_DATE_TIME
     */
    public static final String ISO_ZONED_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'";

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
     * java.util.Date now = Dates.currentJUDate();
     * // Result: "2023-12-25T14:30:45Z"
     * String formatted = Dates.format(now, Dates.ISO_8601_DATE_TIME_FORMAT);
     * // Using DTF instance
     * java.time.ZonedDateTime utcTime = java.time.ZonedDateTime.now(java.time.ZoneId.of("UTC"));
     * String utcFormatted = DTF.ISO_8601_DATE_TIME.format(utcTime);
     * }</pre>
     *
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
     * // Result: "2023-12-25T14:30:45.123Z"
     * String formatted = Dates.format(now, Dates.ISO_8601_TIMESTAMP_FORMAT);
     * // Default format for system logging
     * System.out.println("Server started at: " + Dates.format(Dates.currentJUDate(), Dates.ISO_8601_TIMESTAMP_FORMAT));
     * }</pre>
     *
     */
    public static final String ISO_8601_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    // static final String ISO_8601_TIMESTAMP_FORMAT_SLASH = "yyyy/MM/dd'T'HH:mm:ss.SSS'Z'";

    private static final String ISO_8601_TIMESTAMP_FORMAT_2 = "yyyy-MM-dd'T'HH:mm:ss.SSS";

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
     * java.util.Date now = Dates.currentJUDate();
     * // Result: "Mon, 25 Dec 2023 14:30:45 GMT"
     * String formatted = Dates.format(now, Dates.RFC_1123_DATE_TIME_FORMAT);
     * // Using DTF instance for HTTP headers
     * java.util.Calendar calendar = java.util.Calendar.getInstance();
     * String httpDate = DTF.RFC_1123_DATE_TIME.format(calendar);
     * // Set HTTP header with RFC 1123 date
     * String lastModified = Dates.format(fileLastModified, Dates.RFC_1123_DATE_TIME_FORMAT);
     * // Can be used in: Last-Modified, Date, Expires headers
     * }</pre>
     *
     */
    public static final String RFC_1123_DATE_TIME_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

    /**
     * Sentinel value representing half a month; used internally to indicate whether a date
     * falls in the first or second half of the month during rounding operations.
     *
     * @see #round(java.util.Date, int)
     * @see #truncate(java.util.Date, int)
     * @see #ceiling(java.util.Date, int)
     */
    public static final int SEMI_MONTH = 1001;

    private static final int[][] fields = { { Calendar.MILLISECOND }, { Calendar.SECOND }, { Calendar.MINUTE }, { Calendar.HOUR_OF_DAY, Calendar.HOUR },
            { Calendar.DATE, Calendar.DAY_OF_MONTH, Calendar.AM_PM
            /* Calendar.DAY_OF_YEAR, Calendar.DAY_OF_WEEK, Calendar.DAY_OF_WEEK_IN_MONTH */ }, { Calendar.MONTH, SEMI_MONTH }, { Calendar.YEAR },
            { Calendar.ERA } };

    @SuppressWarnings("deprecation")
    private static final int POOL_SIZE = InternalUtil.POOL_SIZE;

    private static final Map<String, Queue<DateFormat>> dfPool = new ObjectPool<>(64);

    private static final ConcurrentHashMap<TimeZone, Queue<Calendar>> calendarPool = new ConcurrentHashMap<>(64);

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

    static final Map<Class<? extends java.util.Calendar>, LongObjFunction<? super Calendar, ? extends java.util.Calendar>> calendarCreatorPool = new ConcurrentHashMap<>();

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
            // Must be `new GregorianCalendar()`, not `Calendar.getInstance()`: under a default locale
            // that selects a non-Gregorian calendar (e.g. ja-JP-u-ca-japanese), getInstance() returns a
            // JapaneseImperialCalendar, which would ClassCastException at the GregorianCalendar caller.
            final GregorianCalendar ret = new GregorianCalendar();

            if (!N.equals(ret.getTimeZone(), c.getTimeZone()) && c.getTimeZone() != null) {
                ret.setTimeZone(c.getTimeZone());
            }

            ret.setTimeInMillis(millis);

            return ret;
        });
    }

    Dates() {
        // Utility class - prevent instantiation
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
     *
     * class MyDate extends java.util.Date {
     *     MyDate(long millis) { super(millis); }
     * }
     * Dates.registerDateCreator(MyDate.class, MyDate::new);   // returns true (first registration)
     * Dates.registerDateCreator(MyDate.class, MyDate::new);   // returns false (already registered)
     *
     * Dates.registerDateCreator(java.util.Date.class, java.util.Date::new);   // returns false (java.* is restricted)
     * Dates.registerDateCreator(java.sql.Timestamp.class, java.sql.Timestamp::new); // returns false (java.* is restricted)
     * }</pre>
     *
     * @param <T> the type of the date class extending {@code java.util.Date}.
     * @param dateClass the class of the date to register the creator for, must not be in restricted packages. Not {@code null}.
     * @param dateCreator the function that creates instances of the date class, taking one argument:
     *        the time in milliseconds. Not {@code null}.
     * @return {@code true} if the date creator was successfully registered, {@code false} if the class was already registered
     *         or is from a restricted package ({@code java.*}, {@code javax.*}, or {@code com.landawn.abacus.*}).
     * @throws IllegalArgumentException if {@code dateClass} or {@code dateCreator} is {@code null}.
     * @see #registerCalendarCreator(Class, LongObjFunction)
     */
    public static <T extends java.util.Date> boolean registerDateCreator(final Class<? extends T> dateClass, final LongFunction<? extends T> dateCreator) {
        N.checkArgNotNull(dateClass, cs.dateClass);
        N.checkArgNotNull(dateCreator, cs.dateCreator);

        if (dateCreatorPool.containsKey(dateClass) || isRestrictedCreatorPackage(dateClass)) {
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
     *
     * class MyCal extends java.util.GregorianCalendar {
     *     MyCal(long millis, Calendar template) { super(); setTimeInMillis(millis); }
     * }
     * Dates.registerCalendarCreator(MyCal.class, (millis, tmpl) -> new MyCal(millis, tmpl));   // returns true
     * Dates.registerCalendarCreator(MyCal.class, (millis, tmpl) -> new MyCal(millis, tmpl));   // returns false (already registered)
     *
     * Dates.registerCalendarCreator(java.util.GregorianCalendar.class,
     *         (millis, tmpl) -> new java.util.GregorianCalendar());   // returns false (java.* is restricted)
     * }</pre>
     *
     * @param <T> the type of the calendar class extending {@code java.util.Calendar}.
     * @param calendarClass the class of the calendar to register the creator for, must not be in restricted packages. Not {@code null}.
     * @param calendarCreator the function that creates instances of the calendar class, taking two arguments:
     *        the time in milliseconds and a {@code Calendar} template. Not {@code null}.
     * @return {@code true} if the calendar creator was successfully registered, {@code false} if the class was already registered
     *         or is from a restricted package ({@code java.*}, {@code javax.*}, or {@code com.landawn.abacus.*}).
     * @throws IllegalArgumentException if {@code calendarClass} or {@code calendarCreator} is {@code null}.
     * @see #registerDateCreator(Class, LongFunction)
     */
    public static <T extends java.util.Calendar> boolean registerCalendarCreator(final Class<? extends T> calendarClass,
            final LongObjFunction<? super Calendar, ? extends T> calendarCreator) {
        N.checkArgNotNull(calendarClass, cs.calendarClass);
        N.checkArgNotNull(calendarCreator, cs.calendarCreator);

        if (calendarCreatorPool.containsKey(calendarClass) || isRestrictedCreatorPackage(calendarClass)) {
            return false;
        }

        calendarCreatorPool.put(calendarClass, calendarCreator);

        return true;
    }

    private static boolean isRestrictedCreatorPackage(final Class<?> cls) {
        final String packageName = ClassUtil.getPackageName(cls);
        return Strings.startsWithAny(packageName, "java.", "javax.") || N.equals(packageName, "com.landawn.abacus")
                || packageName.startsWith("com.landawn.abacus.");
    }

    /**
     * Returns the current time in milliseconds.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long millis = Dates.currentTimeMillis();   // returns current epoch millis
     * Date date = new Date(millis);
     * (millis > 0L);                             // returns true (current time is after 1970-01-01)
     * }</pre>
     *
     * <p>Note: this method is exactly equivalent to {@link System#currentTimeMillis()}; it exists
     * only for discoverability alongside the other {@code current*} methods. It is not to be
     * confused with {@link #currentTime()}, which returns a {@code java.sql.Time} object.</p>
     *
     * @return the current time in milliseconds since the epoch (01-01-1970).
     * @deprecated this method is byte-for-byte identical to {@link System#currentTimeMillis()} and
     *             exists only as a confusion hazard with {@link #currentTime()}; call
     *             {@link System#currentTimeMillis()} directly instead.
     * @see System#currentTimeMillis()
     * @see #currentTime()
     */
    @Beta
    @Deprecated
    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Returns a new instance of {@code java.sql.Time} based on the current time in the default time zone with the default locale.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Time t = Dates.currentTime();      // returns a new Time for now
     * (t != null);                       // returns true (never null)
     * Time t2 = Dates.currentTime();     // returns another Time instance
     * (t != t2);                         // returns true (each call returns a new object)
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
     * Date d = Dates.currentDate();      // returns a new Date for today
     * (d != null);                       // returns true (never null)
     * Date d2 = Dates.currentDate();     // returns another Date instance
     * (d != d2);                         // returns true (each call returns a new object)
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
     * Timestamp ts = Dates.currentTimestamp();   // returns a new Timestamp for now
     * (ts != null);                              // returns true (never null)
     * Timestamp ts2 = Dates.currentTimestamp();  // returns another Timestamp instance
     * (ts != ts2);                               // returns true (each call returns a new object)
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
     * java.util.Date d = Dates.currentJUDate();   // returns a new java.util.Date for now
     * (d != null);                                // returns true (never null)
     * java.util.Date d2 = Dates.currentJUDate();  // returns another java.util.Date instance
     * (d != d2);                                  // returns true (each call returns a new object)
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
     * int year = cal.get(Calendar.YEAR);   // year is the current year
     * (cal != null);                       // returns true (never null)
     * (year >= 1970);                      // returns true (current year is after the epoch)
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
     * int year = cal.get(Calendar.YEAR);   // year is the current year
     * (cal != null);                       // returns true (never null)
     * (year >= 1970);                      // returns true (current year is after the epoch)
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
     * int year = xmlCal.getYear();   // year is the current year
     * (xmlCal != null);              // returns true (never null when DatatypeFactory is available)
     * (year >= 1970);                // returns true (current year is after the epoch)
     * }</pre>
     *
     * @return a new {@code XMLGregorianCalendar} instance representing the current date and time.
     * @throws UnsupportedOperationException if the {@code DatatypeFactory} is not available.
     */
    public static XMLGregorianCalendar currentXMLGregorianCalendar() {
        if (dataTypeFactory == null) {
            throw new UnsupportedOperationException("DatatypeFactory is not available. XMLGregorianCalendar operations are not supported.");
        }

        return dataTypeFactory.newXMLGregorianCalendar(currentGregorianCalendar());
    }

    /**
     * Calculates the current time in milliseconds with the specified time amount added or subtracted.
     * This method adds or subtracts the given amount in the specified time unit to the current system time.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long futureTime = Dates.currentTimeMillisPlus(5, TimeUnit.MINUTES);   // futureTime is about 5 minutes from now
     * long pastTime = Dates.currentTimeMillisPlus(-2, TimeUnit.HOURS);      // pastTime is about 2 hours ago
     * }</pre>
     *
     * @param amount the amount of time to add (positive) or subtract (negative).
     * @param unit the time unit of the amount parameter (e.g., TimeUnit.SECONDS, TimeUnit.MINUTES). Not {@code null}.
     * @return the current time in milliseconds with the specified amount applied.
     * @throws IllegalArgumentException if {@code unit} is {@code null}.
     */
    @Beta
    static long currentTimeMillisPlus(final long amount, final TimeUnit unit) {
        N.checkArgNotNull(unit, cs.unit);

        return System.currentTimeMillis() + unit.toMillis(amount);
    }

    /**
     * Returns a new {@code java.sql.Time} instance representing the current time with the specified time amount added or subtracted.
     * This method creates a new Time object by applying the given amount in the specified time unit to the current system time.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Time future = Dates.currentTimePlus(30, TimeUnit.MINUTES);   // future is about 30 minutes from now
     * Time past = Dates.currentTimePlus(-1, TimeUnit.HOURS);       // past is about 1 hour ago
     * (future.getTime() > past.getTime());                         // returns true (future is later)
     * Time same = Dates.currentTimePlus(0, TimeUnit.SECONDS);      // same is based on the current time
     * (same != null);                                              // returns true (never null)
     * }</pre>
     *
     * @param amount the amount of time to add (positive) or subtract (negative).
     * @param unit the time unit of the amount parameter (e.g., TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS).
     * @return a new {@code java.sql.Time} instance representing the current time with the specified amount applied.
     * @throws IllegalArgumentException if {@code unit} is {@code null}.
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
     * Date tomorrow = Dates.currentDatePlus(1, TimeUnit.DAYS);    // tomorrow is about 1 day from now
     * Date lastWeek = Dates.currentDatePlus(-7, TimeUnit.DAYS);   // lastWeek is about 7 days ago
     * (tomorrow.getTime() > lastWeek.getTime());                  // returns true (tomorrow is later)
     * Date now = Dates.currentDatePlus(0, TimeUnit.DAYS);         // now is based on the current date
     * (now != null);                                              // returns true (never null)
     * }</pre>
     *
     * @param amount the amount of time to add (positive) or subtract (negative).
     * @param unit the time unit of the amount parameter (e.g., TimeUnit.DAYS, TimeUnit.HOURS).
     * @return a new {@code java.sql.Date} instance representing the current date with the specified amount applied.
     * @throws IllegalArgumentException if {@code unit} is {@code null}.
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
     * Timestamp future = Dates.currentTimestampPlus(5, TimeUnit.MINUTES);   // future is about 5 minutes from now
     * Timestamp past = Dates.currentTimestampPlus(-3, TimeUnit.HOURS);      // past is about 3 hours ago
     * (future.getTime() > past.getTime());                                  // returns true (future is later)
     * Timestamp now = Dates.currentTimestampPlus(0, TimeUnit.SECONDS);      // now is based on the current time
     * (now != null);                                                        // returns true (never null)
     * }</pre>
     *
     * @param amount the amount of time to add (positive) or subtract (negative).
     * @param unit the time unit of the amount parameter (e.g., TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS).
     * @return a new {@code java.sql.Timestamp} instance representing the current timestamp with the specified amount applied.
     * @throws IllegalArgumentException if {@code unit} is {@code null}.
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
     * java.util.Date future = Dates.currentJUDatePlus(2, TimeUnit.DAYS);   // future is about 2 days from now
     * java.util.Date past = Dates.currentJUDatePlus(-7, TimeUnit.DAYS);    // past is about 7 days ago
     * (future.getTime() > past.getTime());                                 // returns true (future is later)
     * java.util.Date now = Dates.currentJUDatePlus(0, TimeUnit.DAYS);      // now is based on the current time
     * (now != null);                                                       // returns true (never null)
     * }</pre>
     *
     * @param amount the amount of time to add (positive) or subtract (negative).
     * @param unit the time unit of the amount parameter (e.g., TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS).
     * @return a new {@code java.util.Date} instance representing the current date/time with the specified amount applied.
     * @throws IllegalArgumentException if {@code unit} is {@code null}.
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
     * Calendar future = Dates.currentCalendarPlus(3, TimeUnit.HOURS);   // future is about 3 hours from now
     * Calendar past = Dates.currentCalendarPlus(-10, TimeUnit.DAYS);    // past is about 10 days ago
     * (future.getTimeInMillis() > past.getTimeInMillis());              // returns true (future is later)
     * Calendar now = Dates.currentCalendarPlus(0, TimeUnit.HOURS);      // now is based on the current time
     * (now != null);                                                    // returns true (never null)
     * }</pre>
     *
     * @param amount the amount of time to add (positive) or subtract (negative).
     * @param unit the time unit of the amount parameter (e.g., TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS).
     * @return a new {@code java.util.Calendar} instance representing the current date/time with the specified amount applied.
     * @throws IllegalArgumentException if {@code unit} is {@code null}.
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
     * Calendar cal = Dates.createCalendar(0L);                 // cal is at epoch millis
     * java.util.Date d = Dates.createJUDate(cal);              // d is at the same instant as cal
     * (d.getTime() == cal.getTimeInMillis());                  // returns true
     * (d.getTime() == 0L);                                     // returns true
     *
     * Dates.createJUDate((Calendar) null);                    // throws IllegalArgumentException
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
     * java.util.Date original = new java.util.Date(1000L);
     * java.util.Date copy = Dates.createJUDate(original);   // copy is a new instance with the same time
     * (copy.getTime() == 1000L);                            // returns true
     * (copy != original);                                   // returns true (distinct object)
     *
     * Dates.createJUDate((java.util.Date) null);           // throws IllegalArgumentException
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
     * java.util.Date d = Dates.createJUDate(0L);    // d is at epoch millis
     * (d.getTime() == 0L);                          // returns true
     *
     * java.util.Date d2 = Dates.createJUDate(1736937045000L);
     * (d2.getTime() == 1736937045000L);             // returns true (echoes the input millis)
     * (Dates.createJUDate(-1L).getTime() == -1L);   // returns true (negative millis = before the epoch)
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
     * Calendar cal = Dates.createCalendar(0L);            // cal is at epoch millis
     * java.sql.Date sqlDate = Dates.createDate(cal);      // sqlDate is at the same instant as cal
     * (sqlDate.getTime() == cal.getTimeInMillis());       // returns true
     * (sqlDate.getTime() == 0L);                          // returns true
     *
     * Dates.createDate((Calendar) null);                 // throws IllegalArgumentException
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
     * java.util.Date utilDate = new java.util.Date(1000L);
     * java.sql.Date sqlDate = Dates.createDate(utilDate);   // converts to java.sql.Date, same instant
     * (sqlDate.getTime() == 1000L);                         // returns true
     *
     * Dates.createDate((java.util.Date) null);              // throws IllegalArgumentException
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
     * java.sql.Date d = Dates.createDate(0L);    // d is at epoch millis
     * (d.getTime() == 0L);                       // returns true
     *
     * java.sql.Date d2 = Dates.createDate(1736937045000L);
     * (d2.getTime() == 1736937045000L);          // returns true (echoes the input millis, no truncation)
     * (Dates.createDate(-1L).getTime() == -1L);  // returns true (negative millis = before the epoch)
     * }</pre>
     *
     * <p>Note: the given milliseconds are used as-is. Formatted-string parsing through
     * {@link #parseDate(String, String)} truncates to midnight per the JDBC contract; pure numeric
     * strings preserve their epoch-millisecond value for serialization round-trips.</p>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @return a new {@code java.sql.Date} instance representing the specified point in time.
     * @see #createDate(Calendar)
     * @see #createDate(java.util.Date)
     * @see #createJUDate(long)
     * @see #createTime(long)
     * @see #createTimestamp(long)
     * @see #parseDate(String)
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
     * Calendar cal = Dates.createCalendar(0L);       // cal is at epoch millis
     * Time time = Dates.createTime(cal);             // time is at the same instant as cal
     * (time.getTime() == cal.getTimeInMillis());     // returns true
     * (time.getTime() == 0L);                        // returns true
     *
     * Dates.createTime((Calendar) null);             // throws IllegalArgumentException
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
     * java.util.Date utilDate = new java.util.Date(1000L);
     * Time time = Dates.createTime(utilDate);   // converts to java.sql.Time, same instant
     * (time.getTime() == 1000L);                // returns true
     *
     * Dates.createTime((java.util.Date) null);  // throws IllegalArgumentException
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
     * Time t = Dates.createTime(0L);             // t is at epoch millis
     * (t.getTime() == 0L);                       // returns true
     *
     * Time t2 = Dates.createTime(52245000L);     // t2 is 52,245,000 ms after epoch
     * (t2.getTime() == 52245000L);               // returns true (echoes the input millis)
     * (Dates.createTime(-1L).getTime() == -1L);  // returns true (negative millis = before the epoch)
     * }</pre>
     *
     * <p>Note: the given milliseconds are used as-is. Formatted-string parsing through
     * {@link #parseTime(String, String)} normalizes the date portion to 1970-01-01 per the JDBC
     * contract; pure numeric strings preserve their epoch-millisecond value for serialization
     * round-trips.</p>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @return a new {@code java.sql.Time} instance representing the specified point in time.
     * @see #createTime(Calendar)
     * @see #createTime(java.util.Date)
     * @see #createDate(long)
     * @see #createTimestamp(long)
     * @see #parseTime(String)
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
     * Calendar cal = Dates.createCalendar(0L);                // cal is at epoch millis
     * Timestamp ts = Dates.createTimestamp(cal);              // ts is at the same instant as cal
     * (ts.getTime() == cal.getTimeInMillis());                // returns true
     * (ts.getTime() == 0L);                                   // returns true
     *
     * Dates.createTimestamp((Calendar) null);                // throws IllegalArgumentException
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
     * java.util.Date utilDate = new java.util.Date(1000L);
     * Timestamp ts = Dates.createTimestamp(utilDate);   // converts to java.sql.Timestamp, same instant
     * (ts.getTime() == 1000L);                          // returns true
     *
     * Dates.createTimestamp((java.util.Date) null);     // throws IllegalArgumentException
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
     * Timestamp ts = Dates.createTimestamp(0L);    // ts is at epoch millis
     * (ts.getTime() == 0L);                        // returns true
     *
     * Timestamp ts2 = Dates.createTimestamp(1736937045123L);
     * (ts2.getTime() == 1736937045123L);             // returns true (millisecond precision preserved)
     * (Dates.createTimestamp(-1L).getTime() == -1L); // returns true (negative millis = before the epoch)
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
     * Calendar source = Dates.createCalendar(0L);            // source is at epoch millis
     * Calendar copy = Dates.createCalendar(source);          // copy is at the same instant as source
     * (copy.getTimeInMillis() == source.getTimeInMillis());  // returns true
     * (copy.getTimeInMillis() == 0L);                        // returns true
     *
     * Dates.createCalendar((Calendar) null);                 // throws IllegalArgumentException
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
     * java.util.Date date = new java.util.Date(1000L);
     * Calendar cal = Dates.createCalendar(date);   // cal is at the same instant as date
     * (cal.getTimeInMillis() == 1000L);            // returns true
     *
     * Dates.createCalendar((java.util.Date) null); // throws IllegalArgumentException
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
     * Calendar cal = Dates.createCalendar(0L);     // cal is at epoch millis in the default time zone
     * (cal.getTimeInMillis() == 0L);               // returns true
     *
     * Calendar cal2 = Dates.createCalendar(1736937045000L);
     * (cal2.getTimeInMillis() == 1736937045000L);  // returns true (echoes the input millis)
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
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(0L, utc);   // cal is at epoch millis in UTC
     * (cal.getTimeInMillis() == 0L);                  // returns true
     * (cal.get(Calendar.YEAR) == 1970);               // returns true (1970-01-01 in UTC)
     * (cal.get(Calendar.HOUR_OF_DAY) == 0);           // returns true
     *
     * Calendar def = Dates.createCalendar(0L, null);  // uses the default time zone
     * (def.getTimeInMillis() == 0L);                  // returns true
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
     * Calendar source = Dates.createCalendar(0L);                          // source is at epoch millis
     * GregorianCalendar gregCal = Dates.createGregorianCalendar(source);   // gregCal is at the same instant as source
     * (gregCal.getTimeInMillis() == source.getTimeInMillis());             // returns true
     * (gregCal.getTimeInMillis() == 0L);                                   // returns true
     *
     * Dates.createGregorianCalendar((Calendar) null);              // throws IllegalArgumentException
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
     * java.util.Date date = new java.util.Date(1000L);
     * GregorianCalendar gregCal = Dates.createGregorianCalendar(date);   // gregCal is at the same instant as date
     * (gregCal.getTimeInMillis() == 1000L);                              // returns true
     *
     * Dates.createGregorianCalendar((java.util.Date) null);             // throws IllegalArgumentException
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
     * GregorianCalendar gregCal = Dates.createGregorianCalendar(0L);   // gregCal is at epoch millis
     * (gregCal.getTimeInMillis() == 0L);                               // returns true
     *
     * GregorianCalendar g2 = Dates.createGregorianCalendar(1736937045000L);
     * (g2.getTimeInMillis() == 1736937045000L);                       // returns true (echoes the input millis)
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
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * GregorianCalendar gregCal = Dates.createGregorianCalendar(0L, utc);   // gregCal is at epoch millis in UTC
     * (gregCal.getTimeInMillis() == 0L);                                    // returns true
     * (gregCal.get(Calendar.YEAR) == 1970);                                 // returns true (1970-01-01 in UTC)
     *
     * GregorianCalendar def = Dates.createGregorianCalendar(0L, null);     // uses the default time zone
     * (def.getTimeInMillis() == 0L);                                       // returns true
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
     * Calendar source = Dates.createCalendar(0L, TimeZone.getTimeZone("UTC"));
     * XMLGregorianCalendar xmlCal = Dates.createXMLGregorianCalendar(source);
     * (xmlCal.toGregorianCalendar().getTimeInMillis() == 0L);   // returns true (same instant)
     * (xmlCal != null);                                         // returns true
     *
     * Dates.createXMLGregorianCalendar((Calendar) null);        // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the calendar providing the time value, not {@code null}.
     * @return a new {@code XMLGregorianCalendar} instance representing the same point in time.
     * @throws IllegalArgumentException if calendar is {@code null}.
     * @throws UnsupportedOperationException if the {@code DatatypeFactory} is not available.
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
     * java.util.Date date = new java.util.Date(0L);
     * XMLGregorianCalendar xmlCal = Dates.createXMLGregorianCalendar(date);
     * (xmlCal.toGregorianCalendar().getTimeInMillis() == 0L);   // returns true (same instant)
     *
     * Dates.createXMLGregorianCalendar((java.util.Date) null);  // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date providing the time value, not {@code null}.
     * @return a new {@code XMLGregorianCalendar} instance representing the same point in time.
     * @throws IllegalArgumentException if date is {@code null}.
     * @throws UnsupportedOperationException if the {@code DatatypeFactory} is not available.
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
     * XMLGregorianCalendar xmlCal = Dates.createXMLGregorianCalendar(0L);   // xmlCal is at epoch millis
     * (xmlCal.toGregorianCalendar().getTimeInMillis() == 0L);               // returns true
     *
     * XMLGregorianCalendar x2 = Dates.createXMLGregorianCalendar(1736937045000L);
     * (x2.toGregorianCalendar().getTimeInMillis() == 1736937045000L);      // returns true (echoes the input millis)
     * }</pre>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @return a new {@code XMLGregorianCalendar} instance representing the specified point in time.
     * @throws UnsupportedOperationException if the {@code DatatypeFactory} is not available.
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

        if (dataTypeFactory == null) {
            throw new UnsupportedOperationException("DatatypeFactory is not available. XMLGregorianCalendar operations are not supported.");
        }

        return dataTypeFactory.newXMLGregorianCalendar(createGregorianCalendar(timeInMillis));
    }

    /**
     * Creates a new instance of {@code XMLGregorianCalendar} based on the provided time in milliseconds and the specified time zone.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * XMLGregorianCalendar xmlCal = Dates.createXMLGregorianCalendar(0L, utc);
     * (xmlCal.getYear() == 1970);                                 // returns true (1970-01-01 in UTC)
     * (xmlCal.toString().equals("1970-01-01T00:00:00.000Z"));     // returns true
     *
     * XMLGregorianCalendar def = Dates.createXMLGregorianCalendar(0L, null);   // uses the default time zone
     * (def != null);                                                           // returns true
     * }</pre>
     *
     * @param timeInMillis the time in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * @param tz the time zone for the calendar; if {@code null}, the default time zone is used.
     * @return a new {@code XMLGregorianCalendar} instance with the specified time and time zone.
     * @throws UnsupportedOperationException if the {@code DatatypeFactory} is not available.
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

        if (dataTypeFactory == null) {
            throw new UnsupportedOperationException("DatatypeFactory is not available. XMLGregorianCalendar operations are not supported.");
        }

        return dataTypeFactory.newXMLGregorianCalendar(createGregorianCalendar(timeInMillis, tz));
    }

    /**
     * Parses a string representation of a date into a {@code java.util.Date} object.
     * Attempts to automatically detect the date format from common patterns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseJUDate("2025-01-15T10:30:45Z").getTime();   // returns 1736937045000 (ISO-8601 UTC)
     * Dates.parseJUDate("1736937045000").getTime();          // returns 1736937045000 (numeric = epoch millis)
     *
     * Dates.parseJUDate((String) null);                      // returns null
     * Dates.parseJUDate("");                                 // returns null
     * Dates.parseJUDate("null");                             // returns null (the literal string "null")
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
     * // parse and format in the same (default) zone, so the round-trip is stable
     * Dates.format(Dates.parseJUDate("22/10/2025", "dd/MM/yyyy"), "yyyy-MM-dd");
     *                                                  // returns "2025-10-22"
     * Dates.parseJUDate("2025-01-15T10:30:45Z", Dates.ISO_8601_DATE_TIME_FORMAT).getTime();
     *                                                  // returns 1736937045000
     *
     * Dates.parseJUDate((String) null, "dd/MM/yyyy");  // returns null
     * Dates.parseJUDate("not-a-date", "yyyy-MM-dd");   // throws IllegalArgumentException
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
     * Dates.parseJUDate("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss", utc).getTime();
     *                                                          // returns 1736937045000
     * Dates.parseJUDate("2025-10-22", "yyyy-MM-dd", utc).getTime();
     *                                                          // returns 1761091200000 (midnight UTC)
     *
     * Dates.parseJUDate((String) null, "yyyy-MM-dd", utc);     // returns null
     * Dates.parseJUDate("bad", "yyyy-MM-dd", utc);             // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Note: input carrying the ISO 8601 UTC designator {@code 'Z'} is always interpreted in UTC.
     * Combining a format pattern ending in the literal {@code 'Z'} (explicit or auto-detected), or a
     * date string ending in {@code 'Z'}, with a non-UTC {@code timeZone} throws an
     * {@code IllegalArgumentException}; a {@code null} or UTC-equivalent zone is accepted and resolved
     * to UTC. The {@code format} counterpart now rejects the same conflict; see
     * {@link #format(java.util.Date, String, TimeZone)}.</p>
     *
     * <p>Note: a purely numeric input (a {@code null} format with an epoch-millisecond string) is
     * interpreted as absolute epoch milliseconds, so the supplied {@code timeZone} does not affect the
     * result.</p>
     *
     * @param date the string representation of the date to be parsed.
     * @param format the date format pattern; if {@code null}, common formats are attempted automatically.
     * @param timeZone the time zone for parsing; if {@code null}, the default time zone is used.
     * @return the parsed {@code java.util.Date} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @throws IllegalArgumentException if the date string cannot be parsed using the specified format,
     *         or if an ISO 8601 {@code 'Z'} format is combined with a time zone other than UTC.
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
                // Numeric input is absolute epoch milliseconds; run the zone-conflict gate for uniformity
                // with the formatted path (it never triggers for a numeric string), then interpret the value
                // as epoch millis — the supplied timeZone does not affect the result.
                checkTimeZone(date, format, timeZone);

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
            return parseISO8601(date, formatToUse, format, timeZone);
        }

        final TimeZone timeZoneToUse = checkTimeZone(date, formatToUse, timeZone);

        final long timeInMillis = fastDateParse(date, formatToUse, timeZoneToUse);

        if (timeInMillis != Long.MIN_VALUE) {
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
     * Dates.parseDate("2025-01-15T10:30:45Z");      // formatted input: truncated to midnight in the default time zone
     * Dates.parseDate("1736937045000").getTime();   // numeric input: returns 1736937045000
     *
     * Dates.parseDate((String) null);               // returns null
     * Dates.parseDate("");                          // returns null
     * Dates.parseDate("null");                      // returns null
     * }</pre>
     *
     * <p>Note: formatted date/time strings are normalized to midnight (00:00:00) in the parsing zone
     * per the JDBC contract. Pure numeric epoch-millisecond strings are treated as raw instants and
     * preserve their {@code getTime()} value exactly, matching {@link #createDate(long)}.</p>
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
     * // parse and format in the same (default) zone, so the round-trip is stable
     * Dates.format(Dates.parseDate("22/10/2025", "dd/MM/yyyy"), "yyyy-MM-dd");
     *                                                  // returns "2025-10-22"
     *
     * Dates.parseDate((String) null, "dd/MM/yyyy");    // returns null
     * Dates.parseDate("bad", "dd/MM/yyyy");            // throws IllegalArgumentException
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
     * Dates.parseDate("2025-01-15", "yyyy-MM-dd", utc).getTime();
     *                                                          // returns 1736899200000 (midnight UTC; time-of-day truncated)
     * Dates.parseDate("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss", utc).getTime();
     *                                                          // returns 1736899200000 (still truncated to midnight)
     *
     * Dates.parseDate((String) null, "yyyy-MM-dd", utc);       // returns null
     * Dates.parseDate("bad", "yyyy-MM-dd", utc);               // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Note: input carrying the ISO 8601 UTC designator {@code 'Z'} is always interpreted in UTC;
     * combining an ISO 8601 {@code 'Z'} format (explicit or auto-detected) with a non-UTC
     * {@code timeZone} throws an {@code IllegalArgumentException}.</p>
     *
     * <p>Note: a purely numeric input (a {@code null} format with an epoch-millisecond string) is
     * interpreted as absolute epoch milliseconds, so the supplied {@code timeZone} does not affect the
     * result.</p>
     *
     * @param date the string representation of the date to be parsed.
     * @param format the date format pattern; if {@code null}, common formats are attempted automatically.
     * @param timeZone the time zone for parsing; if {@code null}, the default time zone is used.
     * @return the parsed {@code java.sql.Date} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @throws IllegalArgumentException if the date string cannot be parsed using the specified format,
     *         or if an ISO 8601 {@code 'Z'} format is combined with a time zone other than UTC.
     * @see #parseDate(String)
     * @see #parseDate(String, String)
     * @see #parseJUDate(String, String, TimeZone)
     */
    @MayReturnNull
    public static Date parseDate(final String date, final String format, final TimeZone timeZone) {
        if (isNullDateTime(date)) {
            return null;
        }

        if ((format == null) && isPossibleLong(date)) {
            try {
                return createDate(Long.parseLong(date));
            } catch (final NumberFormatException e) {
                // ignore. Fall through to the formatted parser for consistent invalid-input handling.
            }
        }

        final long millis = parse(date, format, timeZone);

        // Normalize formatted input to 00:00:00 in the parsing zone per the JDBC java.sql.Date contract.
        return createDate(truncateMillisToDate(millis, timeZone));
    }

    /**
     * Truncates a UTC-millisecond value to midnight in the supplied zone. Used to normalize
     * java.sql.Date returns per JDBC contract.
     */
    private static long truncateMillisToDate(final long millis, final TimeZone timeZone) {
        // DEFAULT_TIME_ZONE (not the live default): the parsing step resolved a null zone to
        // DEFAULT_TIME_ZONE, and one parse call must use a single zone end to end.
        final TimeZone zone = timeZone == null ? DEFAULT_TIME_ZONE : timeZone;
        final Calendar cal = Calendar.getInstance(zone);
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Parses a string representation of a time into a {@code java.sql.Time} object.
     * Attempts to automatically detect the time format from common patterns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseTime("2025-01-15T10:30:45Z");      // formatted input: date portion normalized to 1970-01-01
     * Dates.parseTime("1736937045000").getTime();   // numeric input: returns 1736937045000
     *
     * Dates.parseTime((String) null);               // returns null
     * Dates.parseTime("");                          // returns null
     * Dates.parseTime("null");                      // returns null
     * }</pre>
     *
     * <p>Note: formatted date/time strings are normalized to 1970-01-01 in the parsing zone (per the
     * {@link java.sql.Time} contract) while the time-of-day is preserved. Pure numeric
     * epoch-millisecond strings are treated as raw instants and preserve their {@code getTime()} value
     * exactly, matching {@link #createTime(long)}.</p>
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
     * // parse and format in the same (default) zone, so the round-trip is stable
     * Dates.format(Dates.parseTime("14:30:45", "HH:mm:ss"), "HH:mm:ss");
     *                                                  // returns "14:30:45"
     *
     * Dates.parseTime((String) null, "HH:mm:ss");      // returns null
     * Dates.parseTime("bad", "HH:mm:ss");              // throws IllegalArgumentException
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
     * Dates.parseTime("14:30:45", "HH:mm:ss", utc).getTime();
     *                                                  // returns 52245000 (time-of-day on 1970-01-01 in UTC)
     * Dates.format(Dates.parseTime("14:30:45", "HH:mm:ss", utc), "HH:mm:ss", utc);
     *                                                  // returns "14:30:45"
     *
     * Dates.parseTime((String) null, "HH:mm:ss", utc); // returns null
     * Dates.parseTime("bad", "HH:mm:ss", utc);         // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Note: input carrying the ISO 8601 UTC designator {@code 'Z'} is always interpreted in UTC;
     * combining an ISO 8601 {@code 'Z'} format (explicit or auto-detected) with a non-UTC
     * {@code timeZone} throws an {@code IllegalArgumentException}.</p>
     *
     * <p>Note: a purely numeric input (a {@code null} format with an epoch-millisecond string) is
     * interpreted as absolute epoch milliseconds, so the supplied {@code timeZone} does not affect the
     * result.</p>
     *
     * @param date the string representation of the time to be parsed.
     * @param format the time format pattern; if {@code null}, common formats are attempted automatically.
     * @param timeZone the time zone for parsing; if {@code null}, the default time zone is used.
     * @return the parsed {@code java.sql.Time} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @throws IllegalArgumentException if the time string cannot be parsed using the specified format,
     *         or if an ISO 8601 {@code 'Z'} format is combined with a time zone other than UTC.
     * @see #parseTime(String)
     * @see #parseTime(String, String)
     * @see #parseDate(String, String, TimeZone)
     */
    @MayReturnNull
    public static Time parseTime(final String date, final String format, final TimeZone timeZone) {
        if (isNullDateTime(date)) {
            return null;
        }

        if ((format == null) && isPossibleLong(date)) {
            try {
                return createTime(Long.parseLong(date));
            } catch (final NumberFormatException e) {
                // ignore. Fall through to the formatted parser for consistent invalid-input handling.
            }
        }

        final long millis = parse(date, format, timeZone);

        // Normalize formatted input to 1970-01-01 in the parsing zone per the JDBC java.sql.Time contract.
        return createTime(normalizeMillisToTime(millis, timeZone));
    }

    /**
     * Normalizes the date portion of {@code millis} to 1970-01-01 in the supplied zone, while
     * preserving hour/min/sec/ms. Matches {@link java.sql.Time#valueOf(String)} convention,
     * where {@code getTime()} returns 1970-01-01 hh:mm:ss interpreted in the local zone.
     */
    private static long normalizeMillisToTime(final long millis, final TimeZone timeZone) {
        // DEFAULT_TIME_ZONE (not the live default): the parsing step resolved a null zone to
        // DEFAULT_TIME_ZONE, and one parse call must use a single zone end to end.
        final TimeZone zone = timeZone == null ? DEFAULT_TIME_ZONE : timeZone;
        final Calendar cal = Calendar.getInstance(zone);
        cal.setTimeInMillis(millis);
        if (cal.get(Calendar.YEAR) == 1970 && cal.get(Calendar.MONTH) == Calendar.JANUARY && cal.get(Calendar.DAY_OF_MONTH) == 1) {
            return millis; // already normalized
        }
        cal.set(Calendar.YEAR, 1970);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        return cal.getTimeInMillis();
    }

    /**
     * Parses a string representation of a timestamp into a {@code java.sql.Timestamp} object.
     * Attempts to automatically detect the timestamp format from common patterns.
     *
     * <p>The JDBC timestamp escape format {@code "yyyy-mm-dd hh:mm:ss.fffffffff"} produced by
     * {@link Timestamp#toString()} is also supported. Its fractional second may contain 1 to 9 digits and is
     * interpreted as a fraction of a second (e.g. {@code ".5"} is half a second), preserving the full nanosecond
     * precision; such values are interpreted in the default time zone, just as {@code Timestamp.toString()} renders them.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseTimestamp("2025-01-15T10:30:45.123Z").getTime();        // returns 1736937045123 (ISO-8601 UTC, with millis)
     * Dates.parseTimestamp("1736937045123").getTime();                   // returns 1736937045123 (numeric = epoch millis)
     * Dates.parseTimestamp("2025-01-15 10:30:45.123456789").getNanos();  // returns 123456789 (Timestamp.toString() format, nanosecond precision)
     * Dates.parseTimestamp("2025-01-15 10:30:45.5").getNanos();          // returns 500000000 (fractional second, not milliseconds)
     *
     * Dates.parseTimestamp((String) null);                          // returns null
     * Dates.parseTimestamp("");                                     // returns null
     * Dates.parseTimestamp("null");                                 // returns null
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
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.format(Dates.parseTimestamp("2025-01-15 10:30:45.123", "yyyy-MM-dd HH:mm:ss.SSS"),
     *         "yyyy-MM-dd HH:mm:ss.SSS");                       // returns "2025-01-15 10:30:45.123" (default zone round-trip)
     *
     * Dates.parseTimestamp((String) null, "yyyy-MM-dd HH:mm:ss.SSS"); // returns null
     * Dates.parseTimestamp("bad", "yyyy-MM-dd HH:mm:ss.SSS");         // throws IllegalArgumentException
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
     * Dates.parseTimestamp("2025-01-15 10:30:45.123", "yyyy-MM-dd HH:mm:ss.SSS", utc).getTime();
     *                                                          // returns 1736937045123
     *
     * Dates.parseTimestamp((String) null, "yyyy-MM-dd HH:mm:ss.SSS", utc); // returns null
     * Dates.parseTimestamp("bad", "yyyy-MM-dd HH:mm:ss.SSS", utc);         // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Note: input carrying the ISO 8601 UTC designator {@code 'Z'} is always interpreted in UTC;
     * combining an ISO 8601 {@code 'Z'} format (explicit or auto-detected) with a non-UTC
     * {@code timeZone} throws an {@code IllegalArgumentException}.</p>
     *
     * <p>Note: a purely numeric input (a {@code null} format with an epoch-millisecond string) is
     * interpreted as absolute epoch milliseconds, so the supplied {@code timeZone} does not affect the
     * result.</p>
     *
     * @param date the string representation of the timestamp to be parsed.
     * @param format the timestamp format pattern; if {@code null}, common formats are attempted automatically.
     * @param timeZone the time zone for parsing; if {@code null}, the default time zone is used.
     * @return the parsed {@code java.sql.Timestamp} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @throws IllegalArgumentException if the timestamp string cannot be parsed using the specified format,
     *         or if an ISO 8601 {@code 'Z'} format is combined with a time zone other than UTC.
     * @see #parseTimestamp(String)
     * @see #parseTimestamp(String, String)
     * @see #parseTime(String, String, TimeZone)
     */
    @MayReturnNull
    public static Timestamp parseTimestamp(final String date, final String format, final TimeZone timeZone) {
        if (isNullDateTime(date)) {
            return null;
        }

        // Support the JDBC timestamp escape format produced by Timestamp.toString(): "yyyy-mm-dd hh:mm:ss.fffffffff".
        // Timestamp.valueOf is the exact inverse of Timestamp.toString and preserves the full (up to nanosecond)
        // fractional second, which the generic, millisecond-based parse(...) path cannot represent. It interprets the
        // value in the default time zone, matching Timestamp.toString(), so it is only used when no explicit non-default
        // time zone is requested.
        if (format == null && (timeZone == null || timeZone.equals(TimeZone.getDefault())) && isJdbcTimestampString(date)) {
            // Compare against the LIVE default: Timestamp.valueOf interprets the wall time in the
            // current default zone, so an explicitly requested zone must match that one.
            return Timestamp.valueOf(date);
        }

        return createTimestamp(parse(date, format, timeZone));
    }

    /**
     * Parses a string representation of a date/time into a {@code java.util.Calendar} object.
     * Attempts to automatically detect the format from common patterns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dates.parseCalendar("2025-01-15T10:30:45Z").getTimeInMillis();   // returns 1736937045000
     * Dates.parseCalendar("1736937045000").getTimeInMillis();          // returns 1736937045000 (numeric = epoch millis)
     *
     * Dates.parseCalendar((String) null);                             // returns null
     * Dates.parseCalendar("");                                        // returns null
     * Dates.parseCalendar("null");                                    // returns null
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
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.format(Dates.parseCalendar("22/10/2025 14:30", "dd/MM/yyyy HH:mm"), "dd/MM/yyyy HH:mm");
     *                                                  // returns "22/10/2025 14:30" (default zone round-trip)
     *
     * Dates.parseCalendar((String) null, "dd/MM/yyyy HH:mm"); // returns null
     * Dates.parseCalendar("bad", "dd/MM/yyyy HH:mm");         // throws IllegalArgumentException
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
     * Dates.parseCalendar("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss", utc).getTimeInMillis();
     *                                                          // returns 1736937045000
     *
     * Dates.parseCalendar((String) null, "yyyy-MM-dd HH:mm:ss", utc); // returns null
     * Dates.parseCalendar("bad", "yyyy-MM-dd HH:mm:ss", utc);         // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Note: input carrying the ISO 8601 UTC designator {@code 'Z'} is always interpreted in UTC;
     * combining an ISO 8601 {@code 'Z'} format (explicit or auto-detected) with a non-UTC
     * {@code timeZone} throws an {@code IllegalArgumentException}.</p>
     *
     * <p>Note: a purely numeric input (a {@code null} format with an epoch-millisecond string) is
     * interpreted as absolute epoch milliseconds, so the supplied {@code timeZone} does not affect the
     * result.</p>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @param format the date/time format pattern; if {@code null}, common formats are attempted automatically.
     * @param timeZone the time zone for parsing and for the returned calendar; if {@code null}, the default time zone is used.
     * @return the parsed {@code java.util.Calendar} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @throws IllegalArgumentException if the date/time string cannot be parsed using the specified format,
     *         or if an ISO 8601 {@code 'Z'} format is combined with a time zone other than UTC.
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
     * Dates.parseGregorianCalendar("2025-01-15T10:30:45Z").getTimeInMillis();   // returns 1736937045000
     * Dates.parseGregorianCalendar("1736937045000").getTimeInMillis();          // returns 1736937045000
     *
     * Dates.parseGregorianCalendar((String) null);                            // returns null
     * Dates.parseGregorianCalendar("");                                       // returns null
     * Dates.parseGregorianCalendar("null");                                   // returns null
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
     * Dates.format(Dates.parseGregorianCalendar("22/10/2025 14:30", "dd/MM/yyyy HH:mm"), "dd/MM/yyyy HH:mm");
     *                                                  // returns "22/10/2025 14:30" (default zone round-trip)
     *
     * Dates.parseGregorianCalendar((String) null, "dd/MM/yyyy HH:mm"); // returns null
     * Dates.parseGregorianCalendar("bad", "dd/MM/yyyy HH:mm");         // throws IllegalArgumentException
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
     * Dates.parseGregorianCalendar("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss", utc).getTimeInMillis();
     *                                                          // returns 1736937045000
     *
     * Dates.parseGregorianCalendar((String) null, "yyyy-MM-dd HH:mm:ss", utc); // returns null
     * Dates.parseGregorianCalendar("bad", "yyyy-MM-dd HH:mm:ss", utc);         // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Note: input carrying the ISO 8601 UTC designator {@code 'Z'} is always interpreted in UTC;
     * combining an ISO 8601 {@code 'Z'} format (explicit or auto-detected) with a non-UTC
     * {@code timeZone} throws an {@code IllegalArgumentException}.</p>
     *
     * <p>Note: a purely numeric input (a {@code null} format with an epoch-millisecond string) is
     * interpreted as absolute epoch milliseconds, so the supplied {@code timeZone} does not affect the
     * result.</p>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @param format the date/time format pattern; if {@code null}, common formats are attempted automatically.
     * @param timeZone the time zone for parsing and for the returned calendar; if {@code null}, the default time zone is used.
     * @return the parsed {@code java.util.GregorianCalendar} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @throws IllegalArgumentException if the date/time string cannot be parsed using the specified format,
     *         or if an ISO 8601 {@code 'Z'} format is combined with a time zone other than UTC.
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
     * Dates.parseXMLGregorianCalendar("2025-01-15T10:30:45Z").toGregorianCalendar().getTimeInMillis();
     *                                                  // returns 1736937045000
     * Dates.parseXMLGregorianCalendar("1736937045000").toGregorianCalendar().getTimeInMillis();
     *                                                  // returns 1736937045000
     *
     * Dates.parseXMLGregorianCalendar((String) null); // returns null
     * Dates.parseXMLGregorianCalendar("");            // returns null
     * Dates.parseXMLGregorianCalendar("null");        // returns null
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
     * Dates.format(Dates.parseXMLGregorianCalendar("22/10/2025 14:30", "dd/MM/yyyy HH:mm").toGregorianCalendar(),
     *         "dd/MM/yyyy HH:mm");                     // returns "22/10/2025 14:30" (default zone round-trip)
     *
     * Dates.parseXMLGregorianCalendar((String) null, "dd/MM/yyyy HH:mm"); // returns null
     * Dates.parseXMLGregorianCalendar("bad", "dd/MM/yyyy HH:mm");         // throws IllegalArgumentException
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
     * Dates.parseXMLGregorianCalendar("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss", utc)
     *         .toGregorianCalendar().getTimeInMillis();        // returns 1736937045000
     *
     * Dates.parseXMLGregorianCalendar((String) null, "yyyy-MM-dd HH:mm:ss", utc); // returns null
     * Dates.parseXMLGregorianCalendar("bad", "yyyy-MM-dd HH:mm:ss", utc);         // throws IllegalArgumentException
     * }</pre>
     *
     * <p>Note: input carrying the ISO 8601 UTC designator {@code 'Z'} is always interpreted in UTC;
     * combining an ISO 8601 {@code 'Z'} format (explicit or auto-detected) with a non-UTC
     * {@code timeZone} throws an {@code IllegalArgumentException}.</p>
     *
     * <p>Note: a purely numeric input (a {@code null} format with an epoch-millisecond string) is
     * interpreted as absolute epoch milliseconds, so the supplied {@code timeZone} does not affect the
     * result.</p>
     *
     * @param calendar the string representation of the date/time to be parsed.
     * @param format the date/time format pattern; if {@code null}, common formats are attempted automatically.
     * @param timeZone the time zone for parsing and for the returned calendar; if {@code null}, the default time zone is used.
     * @return the parsed {@code javax.xml.datatype.XMLGregorianCalendar} instance, or {@code null} if the input is {@code null}, empty, or the string "null".
     * @throws IllegalArgumentException if the date/time string cannot be parsed using the specified format,
     *         or if an ISO 8601 {@code 'Z'} format is combined with a time zone other than UTC.
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
        if (dateTime == null || dateTime.isEmpty()) {
            return false;
        }

        final int fromIndex = dateTime.charAt(0) == '-' ? 1 : 0;

        if (fromIndex == dateTime.length()) {
            return false;
        }

        for (int i = fromIndex, len = dateTime.length(); i < len; i++) {
            final char ch = dateTime.charAt(i);

            if (ch < '0' || ch > '9') {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks whether the specified string is in the JDBC timestamp escape format with a fractional second part, i.e.
     * {@code "yyyy-MM-dd HH:mm:ss.f"} through {@code "yyyy-MM-dd HH:mm:ss.fffffffff"} (1 to 9 fraction digits), as
     * produced by {@link Timestamp#toString()}.
     *
     * @param str the candidate string; never {@code null}.
     * @return {@code true} if {@code str} matches the JDBC timestamp escape format with a fractional second.
     */
    private static boolean isJdbcTimestampString(final String str) {
        final int len = str.length();

        // "yyyy-MM-dd HH:mm:ss" (19 chars) + '.' + 1 to 9 fraction digits => length 21 to 29.
        if (len < 21 || len > 29) {
            return false;
        }

        if (str.charAt(4) != '-' || str.charAt(7) != '-' || str.charAt(10) != ' ' || str.charAt(13) != ':' || str.charAt(16) != ':' || str.charAt(19) != '.') {
            return false;
        }

        return isAllDigits(str, 0, 4) && isAllDigits(str, 5, 7) && isAllDigits(str, 8, 10) && isAllDigits(str, 11, 13) && isAllDigits(str, 14, 16)
                && isAllDigits(str, 17, 19) && isAllDigits(str, 20, len);
    }

    private static boolean isAllDigits(final String str, final int fromIndex, final int toIndex) {
        if (fromIndex >= toIndex) {
            return false;
        }

        for (int i = fromIndex; i < toIndex; i++) {
            final char ch = str.charAt(i);

            if (ch < '0' || ch > '9') {
                return false;
            }
        }

        return true;
    }

    /**
     * Parses {@code str} with {@code sdf} and rejects any non-whitespace characters left unconsumed.
     * Used only for explicit ISO UTC formats where the literal trailing Z should be the actual input end.
     */
    private static java.util.Date parseFully(final DateFormat sdf, final String str) throws ParseException {
        final ParsePosition pos = new ParsePosition(0);
        final java.util.Date result = sdf.parse(str, pos);

        if (result == null) {
            throw new ParseException("Unparseable date: \"" + str + "\"", pos.getErrorIndex() < 0 ? pos.getIndex() : pos.getErrorIndex());
        }

        int idx = pos.getIndex();
        final int len = str.length();

        while (idx < len && Character.isWhitespace(str.charAt(idx))) {
            idx++;
        }

        if (idx != len) {
            throw new ParseException("Unparseable date \"" + str + "\": unexpected trailing characters at index " + pos.getIndex(), pos.getIndex());
        }

        return result;
    }

    private static java.util.Date parseISO8601(final String dateTime, final String formatToUse, final String explicitFormat, final TimeZone timeZone) {
        // hasSameRules: any zero-offset zone (GMT, Etc/UTC, ...) is acceptable for 'Z' strings,
        // whose suffix fully determines the instant.
        if (timeZone != null && !timeZone.hasSameRules(ISO8601Util.TIMEZONE_Z)) {
            checkTimeZone(dateTime, formatToUse, timeZone);
        }

        if (explicitFormat != null && (ISO_8601_DATE_TIME_FORMAT.equals(formatToUse) || ISO_8601_TIMESTAMP_FORMAT.equals(formatToUse))) {
            final DateFormat sdf = getSDF(formatToUse, UTC_TIME_ZONE);

            try {
                return parseFully(sdf, dateTime);
            } catch (final ParseException e) {
                throw new IllegalArgumentException(e);
            } finally {
                recycleSDF(formatToUse, UTC_TIME_ZONE, sdf);
            }
        }

        return ISO8601Util.parse(dateTime);
    }

    static long parse(final String dateTime, final String format, final TimeZone timezone) {
        if ((format == null) && isPossibleLong(dateTime)) {
            try {
                // Numeric input is absolute epoch milliseconds; run the zone-conflict gate for uniformity
                // with the formatted path (it never triggers for a numeric string), then interpret the value
                // as epoch millis — the supplied timeZone does not affect the result.
                checkTimeZone(dateTime, format, timezone);

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
            return parseISO8601(dateTime, formatToUse, format, timezone).getTime();
        }

        final TimeZone timeZoneToUse = checkTimeZone(dateTime, formatToUse, timezone);

        final long timeInMillis = fastDateParse(dateTime, formatToUse, timeZoneToUse);

        if (timeInMillis != Long.MIN_VALUE) {
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
     * String s = Dates.formatCurrentLocalDate();   // returns e.g. "2025-10-22" (today's date, current zone)
     * (s.length() == 10);                          // returns true (yyyy-MM-dd is always 10 chars)
     * (s.matches("\\d{4}-\\d{2}-\\d{2}"));         // returns true (matches the yyyy-MM-dd shape)
     * (s.charAt(4) == '-');                        // returns true
     * }</pre>
     *
     * @return a non-null string representation of the current date in {@code yyyy-MM-dd} format, rendered in the default time zone.
     * @see #formatCurrentLocalDateTime()
     * @see #formatCurrentDateTime()
     */
    @Beta
    public static String formatCurrentLocalDate() {
        return format(currentDate(), LOCAL_DATE_FORMAT);
    }

    /**
     * Formats the current local date and time using the format {@code yyyy-MM-dd HH:mm:ss}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String s = Dates.formatCurrentLocalDateTime();           // returns e.g. "2025-10-22 14:30:45" (now, current zone)
     * (s.length() == 19);                                       // returns true (yyyy-MM-dd HH:mm:ss is always 19 chars)
     * (s.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")); // returns true (matches the pattern shape)
     * (s.charAt(10) == ' ');                                    // returns true (space between date and time)
     * }</pre>
     *
     * @return a non-null string representation of the current date and time in {@code yyyy-MM-dd HH:mm:ss} format, rendered in the default time zone.
     * @see #formatCurrentLocalDate()
     * @see #formatCurrentDateTime()
     */
    @Beta
    public static String formatCurrentLocalDateTime() {
        return format(currentDate(), LOCAL_DATE_TIME_FORMAT);
    }

    /**
     * Formats the current date and time using the ISO 8601 format {@code yyyy-MM-dd'T'HH:mm:ss'Z'}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String s = Dates.formatCurrentDateTime();                  // returns e.g. "2025-10-22T14:30:45Z" (now, in UTC)
     * (s.length() == 20);                                        // returns true
     * (s.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")); // returns true (ISO-8601 'Z' shape)
     * (s.endsWith("Z"));                                         // returns true (UTC designator)
     * }</pre>
     *
     * @return a non-null string representation of the current date and time in ISO 8601 format {@code yyyy-MM-dd'T'HH:mm:ss'Z'}, rendered in UTC.
     * @see #formatCurrentLocalDateTime()
     * @see #formatCurrentTimestamp()
     * @see #format(java.util.Date)
     */
    public static String formatCurrentDateTime() {
        return format(currentDate(), ISO_8601_DATE_TIME_FORMAT);
    }

    /**
     * Formats the current date and time including milliseconds using the ISO 8601 format {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String s = Dates.formatCurrentTimestamp();                          // returns e.g. "2025-10-22T14:30:45.123Z" (now, in UTC, with millis)
     * (s.length() == 24);                                                 // returns true
     * (s.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z")); // returns true (with .SSS millis)
     * (s.endsWith("Z"));                                                  // returns true (UTC designator)
     * }</pre>
     *
     * @return a non-null string representation of the current timestamp in ISO 8601 format {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}, rendered in UTC.
     * @see #formatCurrentDateTime()
     * @see #format(java.util.Date)
     */
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
     * // The default 'Z' format renders the instant in UTC, independent of the default time zone.
     * Dates.format(new java.util.Date(1736937045000L));           // returns "2025-01-15T10:30:45Z"
     * Dates.format(new java.sql.Timestamp(1736937045123L));       // returns "2025-01-15T10:30:45.123Z" (Timestamp adds millis)
     *
     * Dates.format(new java.util.Date(0L));                       // returns "1970-01-01T00:00:00Z"
     * Dates.format((java.util.Date) null);                        // returns null
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
     * If no format is provided, {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} is used for {@link Timestamp};
     * {@code yyyy-MM-dd'T'HH:mm:ss'Z'} is used for other {@code java.util.Date} instances.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = new java.util.Date(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT);        // returns "2025-01-15T10:30:45Z" (UTC, format-independent of zone)
     * Dates.format(date, null);                                   // returns "2025-01-15T10:30:45Z" (null format = default 'Z' format)
     *
     * Dates.format((java.util.Date) null, "yyyy-MM-dd");          // returns null
     * }</pre>
     *
     * <p>Note: patterns without a {@code 'Z'} literal (e.g. {@code "yyyy-MM-dd HH:mm:ss"}) render in the
     * default time zone; pass an explicit {@link TimeZone} via {@link #format(java.util.Date, String, TimeZone)}
     * for zone-stable output.</p>
     *
     * @param date the date to be formatted.
     * @param format the date format pattern; if {@code null}, the default format depends on the date type.
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
     * If no format is provided, {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} is used for {@link Timestamp};
     * {@code yyyy-MM-dd'T'HH:mm:ss'Z'} is used for other {@code java.util.Date} instances.
     * If no time zone is provided, the default time zone is used.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = new java.util.Date(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss", utc);             // returns "2025-01-15 10:30:45"
     * Dates.format(date, "yyyy-MM-dd", utc);                      // returns "2025-01-15"
     *
     * Dates.format((java.util.Date) null, "yyyy-MM-dd", utc);     // returns null
     * }</pre>
     *
     * <p>Note: a format pattern ending with the literal {@code 'Z'} (e.g.
     * {@link #ISO_8601_DATE_TIME_FORMAT}, including the default format) fixes the output to UTC.
     * Passing a non-UTC {@code timeZone} together with such a pattern throws an
     * {@code IllegalArgumentException}; a {@code null} or UTC-equivalent zone is accepted and resolved
     * to UTC. This mirrors the {@code parse*(String, String, TimeZone)} counterparts, which reject the
     * same combination; see {@link #parseJUDate(String, String, TimeZone)}.</p>
     *
     * @param date the date to be formatted.
     * @param format the date format pattern; if {@code null}, the default format depends on the date type.
     * @param timeZone the time zone for formatting; if {@code null}, the default time zone is used;
     *        must be {@code null} or UTC-equivalent when the format pattern ends with the literal {@code 'Z'}.
     * @return a string representation of the date, or {@code null} if the date is {@code null}.
     * @throws IllegalArgumentException if the format pattern ends with the literal {@code 'Z'} and a
     *         non-UTC {@code timeZone} is supplied.
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
     * Calendar cal = Dates.createCalendar(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * Dates.format(cal);                                     // returns "2025-01-15T10:30:45Z" (default 'Z' format, UTC-rendered)
     * Dates.format(Dates.createCalendar(0L));                // returns "1970-01-01T00:00:00Z"
     *
     * Dates.format((Calendar) null);                         // returns null
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
     * Calendar cal = Dates.createCalendar(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * Dates.format(cal, Dates.ISO_8601_DATE_TIME_FORMAT);    // returns "2025-01-15T10:30:45Z"
     * Dates.format(cal, null);                               // returns "2025-01-15T10:30:45Z" (null format = default)
     *
     * Dates.format((Calendar) null, "yyyy-MM-dd");           // returns null
     * }</pre>
     *
     * <p>Note: patterns without a {@code 'Z'} literal render in the JVM default time zone (the
     * calendar's own time zone is not used); use {@link #format(Calendar, String, TimeZone)} for
     * zone-stable output.</p>
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
     * Calendar cal = Dates.createCalendar(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.format(cal, "yyyy-MM-dd HH:mm:ss", utc);         // returns "2025-01-15 10:30:45"
     * Dates.format(cal, "yyyy-MM-dd", utc);                  // returns "2025-01-15"
     *
     * Dates.format((Calendar) null, "yyyy-MM-dd", utc);      // returns null
     * }</pre>
     *
     * <p>Note: a format pattern ending with the literal {@code 'Z'} (including the default format)
     * fixes the output to UTC. Passing a non-UTC {@code timeZone} together with such a pattern throws
     * an {@code IllegalArgumentException}; a {@code null} or UTC-equivalent zone is accepted and
     * resolved to UTC.</p>
     *
     * @param calendar the calendar to be formatted.
     * @param format the date format pattern; if {@code null}, the default format is used.
     * @param timeZone the time zone for formatting; if {@code null}, the default time zone is used;
     *        must be {@code null} or UTC-equivalent when the format pattern ends with the literal {@code 'Z'}.
     * @return a string representation of the calendar, or {@code null} if the calendar is {@code null}.
     * @throws IllegalArgumentException if the format pattern ends with the literal {@code 'Z'} and a
     *         non-UTC {@code timeZone} is supplied.
     * @see #format(Calendar, String)
     * @see #format(Calendar)
     * @see #parseCalendar(String, String, TimeZone)
     */
    @MayReturnNull
    public static String format(final Calendar calendar, final String format, final TimeZone timeZone) {
        if (calendar == null) {
            return null;
        }

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
     * XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * Dates.format(cal);                                                             // returns "2025-01-15T10:30:45Z"
     * Dates.format(Dates.createXMLGregorianCalendar(0L));                            // returns "1970-01-01T00:00:00Z"
     *
     * Dates.format((XMLGregorianCalendar) null);                 // returns null
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
     * XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * Dates.format(cal, Dates.ISO_8601_DATE_TIME_FORMAT);                            // returns "2025-01-15T10:30:45Z"
     * Dates.format(cal, null);                                                       // returns "2025-01-15T10:30:45Z" (null format = default)
     *
     * Dates.format((XMLGregorianCalendar) null, "yyyy-MM-dd");   // returns null
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
     * XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Dates.format(cal, "yyyy-MM-dd HH:mm:ss", utc);             // returns "2025-01-15 10:30:45"
     * Dates.format(cal, "yyyy-MM-dd", utc);                      // returns "2025-01-15"
     *
     * Dates.format((XMLGregorianCalendar) null, "yyyy-MM-dd", utc); // returns null
     * }</pre>
     *
     * <p>Note: a format pattern ending with the literal {@code 'Z'} (including the default format)
     * fixes the output to UTC. Passing a non-UTC {@code timeZone} together with such a pattern throws
     * an {@code IllegalArgumentException}; a {@code null} or UTC-equivalent zone is accepted and
     * resolved to UTC.</p>
     *
     * @param calendar the XMLGregorianCalendar instance to be formatted; may be {@code null}.
     * @param format the date format pattern; if {@code null}, the default format is used.
     * @param timeZone the time zone for formatting; if {@code null}, the default time zone is used;
     *        must be {@code null} or UTC-equivalent when the format pattern ends with the literal {@code 'Z'}.
     * @return a string representation of the XMLGregorianCalendar instance, or {@code null} if the calendar is {@code null}.
     * @throws IllegalArgumentException if the format pattern ends with the literal {@code 'Z'} and a
     *         non-UTC {@code timeZone} is supplied.
     * @see #format(XMLGregorianCalendar)
     * @see #format(XMLGregorianCalendar, String)
     * @see #format(Calendar, String, TimeZone)
     */
    @MayReturnNull
    public static String format(final XMLGregorianCalendar calendar, final String format, final TimeZone timeZone) {
        if (calendar == null) {
            return null;
        }

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
     * StringBuilder sb = new StringBuilder("Date: ");
     * Dates.formatTo(new java.util.Date(1736937045000L), sb);
     * sb.toString();                                       // returns "Date: 2025-01-15T10:30:45Z"
     *
     * StringBuilder ts = new StringBuilder();
     * Dates.formatTo(new java.sql.Timestamp(1736937045123L), ts);
     * ts.toString();                                       // returns "2025-01-15T10:30:45.123Z" (Timestamp adds millis)
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((java.util.Date) null, nb);
     * nb.toString();                                       // returns "null" (literal appended for null input)
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
     * java.util.Date date = new java.util.Date(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(date, Dates.ISO_8601_DATE_TIME_FORMAT, sb);
     * sb.toString();                                       // returns "2025-01-15T10:30:45Z"
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((java.util.Date) null, "yyyy-MM-dd", nb);
     * nb.toString();                                       // returns "null" (literal appended for null input)
     * }</pre>
     *
     * <p>Note: patterns without a {@code 'Z'} literal render in the default time zone; use the
     * {@code (Date, String, TimeZone, Appendable)} overload for zone-stable output.</p>
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
     * java.util.Date date = new java.util.Date(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(date, "yyyy-MM-dd HH:mm:ss", utc, sb);
     * sb.toString();                                       // returns "2025-01-15 10:30:45"
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((java.util.Date) null, "yyyy-MM-dd", utc, nb);
     * nb.toString();                                       // returns "null" (literal appended for null input)
     * }</pre>
     *
     * <p>Note: a format pattern ending with the literal {@code 'Z'} (including the default format)
     * fixes the output to UTC. Passing a non-UTC {@code timeZone} together with such a pattern throws
     * an {@code IllegalArgumentException}; a {@code null} or UTC-equivalent zone is accepted and
     * resolved to UTC.</p>
     *
     * @param date the java.util.Date instance to be formatted; may be {@code null}.
     * @param format the date format pattern; if {@code null}, the default format is used.
     * @param timeZone the time zone for formatting; if {@code null}, the default time zone is used;
     *        must be {@code null} or UTC-equivalent when the format pattern ends with the literal {@code 'Z'}.
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws IllegalArgumentException if the format pattern ends with the literal {@code 'Z'} and a
     *         non-UTC {@code timeZone} is supplied.
     * @throws UncheckedIOException if an I/O error occurs during the append operation.
     * @see #format(java.util.Date, String, TimeZone)
     * @see #formatTo(java.util.Date, Appendable)
     * @see #formatTo(java.util.Date, String, Appendable)
     */
    public static void formatTo(final java.util.Date date, final String format, final TimeZone timeZone, final Appendable appendable) {
        N.checkArgNotNull(appendable, cs.appendable);

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
     * Calendar cal = Dates.createCalendar(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * StringBuilder sb = new StringBuilder("Calendar: ");
     * Dates.formatTo(cal, sb);
     * sb.toString();                                  // returns "Calendar: 2025-01-15T10:30:45Z"
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((Calendar) null, nb);
     * nb.toString();                                  // returns "null" (literal appended for null input)
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
     * Calendar cal = Dates.createCalendar(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(cal, Dates.ISO_8601_DATE_TIME_FORMAT, sb);
     * sb.toString();                                  // returns "2025-01-15T10:30:45Z"
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((Calendar) null, "yyyy-MM-dd", nb);
     * nb.toString();                                  // returns "null" (literal appended for null input)
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
     * Calendar cal = Dates.createCalendar(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(cal, "yyyy-MM-dd HH:mm:ss", utc, sb);
     * sb.toString();                                  // returns "2025-01-15 10:30:45"
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((Calendar) null, "yyyy-MM-dd", utc, nb);
     * nb.toString();                                  // returns "null" (literal appended for null input)
     * }</pre>
     *
     * <p>Note: a format pattern ending with the literal {@code 'Z'} (including the default format)
     * fixes the output to UTC. Passing a non-UTC {@code timeZone} together with such a pattern throws
     * an {@code IllegalArgumentException}; a {@code null} or UTC-equivalent zone is accepted and
     * resolved to UTC.</p>
     *
     * @param calendar the java.util.Calendar instance to be formatted; may be {@code null}.
     * @param format the date format pattern; if {@code null}, the default format is used.
     * @param timeZone the time zone for formatting; if {@code null}, the default time zone is used;
     *        must be {@code null} or UTC-equivalent when the format pattern ends with the literal {@code 'Z'}.
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws IllegalArgumentException if the format pattern ends with the literal {@code 'Z'} and a
     *         non-UTC {@code timeZone} is supplied.
     * @throws UncheckedIOException if an I/O error occurs during the append operation.
     * @see #format(java.util.Calendar, String, TimeZone)
     * @see #formatTo(Calendar, Appendable)
     * @see #formatTo(Calendar, String, Appendable)
     */
    public static void formatTo(final Calendar calendar, final String format, final TimeZone timeZone, final Appendable appendable) {
        N.checkArgNotNull(appendable, cs.appendable);

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
     * XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * StringBuilder sb = new StringBuilder("XML Calendar: ");
     * Dates.formatTo(cal, sb);
     * sb.toString();                                  // returns "XML Calendar: 2025-01-15T10:30:45Z"
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((XMLGregorianCalendar) null, nb);
     * nb.toString();                                  // returns "null" (literal appended for null input)
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
     * XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(cal, Dates.ISO_8601_DATE_TIME_FORMAT, sb);
     * sb.toString();                                  // returns "2025-01-15T10:30:45Z"
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((XMLGregorianCalendar) null, "yyyy-MM-dd", nb);
     * nb.toString();                                  // returns "null" (literal appended for null input)
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
     * XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * StringBuilder sb = new StringBuilder();
     * Dates.formatTo(cal, "yyyy-MM-dd HH:mm:ss", utc, sb);
     * sb.toString();                                  // returns "2025-01-15 10:30:45"
     *
     * StringBuilder nb = new StringBuilder();
     * Dates.formatTo((XMLGregorianCalendar) null, "yyyy-MM-dd", utc, nb);
     * nb.toString();                                  // returns "null" (literal appended for null input)
     * }</pre>
     *
     * <p>Note: a format pattern ending with the literal {@code 'Z'} (including the default format)
     * fixes the output to UTC. Passing a non-UTC {@code timeZone} together with such a pattern throws
     * an {@code IllegalArgumentException}; a {@code null} or UTC-equivalent zone is accepted and
     * resolved to UTC.</p>
     *
     * @param calendar the XMLGregorianCalendar instance to be formatted; may be {@code null}.
     * @param format the date format pattern; if {@code null}, the default format is used.
     * @param timeZone the time zone for formatting; if {@code null}, the default time zone is used;
     *        must be {@code null} or UTC-equivalent when the format pattern ends with the literal {@code 'Z'}.
     * @param appendable the Appendable to which the formatted date string is to be appended; must not be {@code null}.
     * @throws IllegalArgumentException if the format pattern ends with the literal {@code 'Z'} and a
     *         non-UTC {@code timeZone} is supplied.
     * @throws UncheckedIOException if an I/O error occurs during the append operation.
     * @see #format(XMLGregorianCalendar, String, TimeZone)
     * @see #formatTo(XMLGregorianCalendar, Appendable)
     * @see #formatTo(XMLGregorianCalendar, String, Appendable)
     */
    public static void formatTo(final XMLGregorianCalendar calendar, final String format, final TimeZone timeZone, final Appendable appendable) {
        N.checkArgNotNull(appendable, cs.appendable);

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
        if (date == null) {
            if (appendable != null) {
                formatToForNull(appendable);
            }

            return null;
        }

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

        // Years outside [0, 9999] don't fit the fixed-width fast-format buffer: fall back to the
        // SimpleDateFormat slow path (the explicit-format siblings support such dates).
        if (year < 0 || year >= 10000) {
            utcCalendarPool.offer(c);

            final String formatToUse = isTimestamp ? ISO_8601_TIMESTAMP_FORMAT : ISO_8601_DATE_TIME_FORMAT;
            final DateFormat sdf = getSDF(formatToUse, UTC_TIME_ZONE);
            final String str;

            try {
                str = sdf.format(new java.util.Date(timeInMillis));
            } finally {
                recycleSDF(formatToUse, UTC_TIME_ZONE, sdf);
            }

            try {
                if (sb == null) {
                    appendable.append(str);
                } else {
                    sb.append(str);
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
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
            utcCalendarPool.offer(c);
            utcTimestampFormatCharsPool.offer(utcTimestamp);
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
     * java.util.Date date = Dates.parseJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.setYears(date, 2025), "yyyy-MM-dd HH:mm:ss");   // returns "2025-11-24 10:30:45"
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                         // returns "2024-11-24 10:30:45" (original unchanged)
     *
     * Dates.setYears((java.util.Date) null, 2025);                       // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not null.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is null, or if {@code amount} is out of range for the
     *         field (this method uses a non-lenient calendar).
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
     * java.util.Date date = Dates.parseJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.setMonths(date, 0), "yyyy-MM-dd HH:mm:ss");   // returns "2024-01-24 10:30:45" (0 = January)
     * Dates.format(Dates.setMonths(date, 5), "yyyy-MM-dd HH:mm:ss");   // returns "2024-06-24 10:30:45" (5 = June)
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                       // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.setMonths((java.util.Date) null, 0);                       // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not null.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is null, or if {@code amount} is out of range for the
     *         field (this method uses a non-lenient calendar).
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
     * java.util.Date date = Dates.parseJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.setDays(date, 15), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-15 10:30:45"
     * Dates.format(Dates.setDays(date, 1), "yyyy-MM-dd HH:mm:ss");    // returns "2024-11-01 10:30:45"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                      // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.setDays((java.util.Date) null, 15);                       // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not null.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is null, or if {@code amount} is out of range for the
     *         field (this method uses a non-lenient calendar).
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
     * java.util.Date date = Dates.parseJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.setHours(date, 14), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-24 14:30:45"
     * Dates.format(Dates.setHours(date, 0), "yyyy-MM-dd HH:mm:ss");    // returns "2024-11-24 00:30:45" (midnight hour)
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                      // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.setHours((java.util.Date) null, 14);                      // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not null.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is null, or if {@code amount} is out of range for the
     *         field (this method uses a non-lenient calendar).
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
     * java.util.Date date = Dates.parseJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.setMinutes(date, 15), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-24 10:15:45"
     * Dates.format(Dates.setMinutes(date, 0), "yyyy-MM-dd HH:mm:ss");    // returns "2024-11-24 10:00:45"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                        // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.setMinutes((java.util.Date) null, 15);                      // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not null.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is null, or if {@code amount} is out of range for the
     *         field (this method uses a non-lenient calendar).
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
     * java.util.Date date = Dates.parseJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.setSeconds(date, 0), "yyyy-MM-dd HH:mm:ss");    // returns "2024-11-24 10:30:00"
     * Dates.format(Dates.setSeconds(date, 59), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-24 10:30:59"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                        // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.setSeconds((java.util.Date) null, 0);                       // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not null.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is null, or if {@code amount} is out of range for the
     *         field (this method uses a non-lenient calendar).
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
     * java.util.Date date = Dates.parseJUDate("2024-11-24 10:30:45.123", "yyyy-MM-dd HH:mm:ss.SSS");
     * Dates.format(Dates.setMilliseconds(date, 500), "yyyy-MM-dd HH:mm:ss.SSS");   // returns "2024-11-24 10:30:45.500"
     * Dates.format(Dates.setMilliseconds(date, 0), "yyyy-MM-dd HH:mm:ss.SSS");     // returns "2024-11-24 10:30:45.000"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss.SSS");                              // returns "2024-11-24 10:30:45.123" (original unchanged)
     * Dates.setMilliseconds((java.util.Date) null, 500);                          // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date, not null.
     * @param amount the amount to set.
     * @return a new {@code Date} set with the specified value.
     * @throws IllegalArgumentException if the date is null, or if {@code amount} is out of range for the
     *         field (this method uses a non-lenient calendar).
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
     * @param <T> the concrete {@code java.util.Date} subtype returned by this method
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
     * Adds or subtracts the specified amount of time, expressed in the given
     * {@link TimeUnit}, to the supplied date and returns a new object of the same
     * concrete type. The original date is unchanged. The amount is converted to
     * milliseconds via {@link TimeUnit#toMillis(long)} and applied as plain
     * millisecond arithmetic (it does not account for daylight-saving-time
     * transitions).
     *
     * <p><b>Difference from {@link Calendar#roll(int, int)}:</b> despite its name, this method performs
     * plain <i>addition</i> that carries into larger fields &mdash; it does <b>not</b> have
     * {@code Calendar.roll} semantics, which change a single field without altering larger fields. For
     * example, adding 5 days to Jan 30 advances into February, whereas {@code Calendar.roll(DAY_OF_MONTH, 5)}
     * would wrap within January and leave the month unchanged. The field-specific {@code add*} methods are
     * the preferred replacements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * java.util.Date date = new java.util.Date(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * Dates.format(Dates.addDays(date, 5), Dates.ISO_8601_DATE_TIME_FORMAT, utc);
     *                                                  // returns "2025-01-20T10:30:45Z" (+5 days)
     * Dates.format(Dates.addHours(date, -2), Dates.ISO_8601_DATE_TIME_FORMAT, utc);
     *                                                  // returns "2025-01-15T08:30:45Z" (-2 hours)
     *
     * Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, utc);   // returns "2025-01-15T10:30:45Z" (original unchanged)
     * }</pre>
     *
     * @param <T> the concrete {@code java.util.Date} subtype of {@code date} which is also the return type.
     * @param date the date to add to, must not be {@code null}.
     * @param amount the amount of time to add or subtract (negative values subtract).
     * @param unit the time unit of the {@code amount} parameter, must not be {@code null}.
     * @return a new date of the same type as {@code date} with the specified amount applied.
     * @throws IllegalArgumentException if {@code date} or {@code unit} is {@code null}.
     * @deprecated misleadingly named (it adds, it does not {@link Calendar#roll(int, int) roll}); use the
     *             field-specific {@code add*} methods instead, e.g. {@link #addDays(java.util.Date, int)},
     *             {@link #addHours(java.util.Date, int)}, {@link #addMinutes(java.util.Date, int)},
     *             {@link #addSeconds(java.util.Date, int)}, {@link #addMilliseconds(java.util.Date, int)}.
     *             Behavior is unchanged.
     */
    @Beta
    @Deprecated
    public static <T extends java.util.Date> T roll(final T date, final long amount, final TimeUnit unit) throws IllegalArgumentException {
        return addToDate(date, amount, unit);
    }

    /**
     * Adds or subtracts the specified amount of the given {@link CalendarField}
     * to the supplied date and returns a new object of the same concrete type.
     * The original date is unchanged. For {@code MONTH}, {@code YEAR},
     * {@code DAY_OF_MONTH} and {@code WEEK_OF_YEAR} the arithmetic is performed
     * through {@link Calendar#add(int, int)} so that daylight-saving-time
     * transitions are honoured; for finer fields plain millisecond arithmetic
     * is used.
     *
     * <p><b>Difference from {@link Calendar#roll(int, int)}:</b> despite its name, this method performs
     * plain <i>addition</i> that carries into larger fields &mdash; it does <b>not</b> have
     * {@code Calendar.roll} semantics, which change a single field without altering larger fields. For
     * example, adding 2 months to Nov 15 advances the year into the next January, whereas
     * {@code Calendar.roll(MONTH, 2)} would wrap within the same year. The field-specific {@code add*}
     * methods are the preferred replacements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * java.util.Date date = new java.util.Date(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * Dates.format(Dates.addDays(date, 5), Dates.ISO_8601_DATE_TIME_FORMAT, utc);
     *                                                  // returns "2025-01-20T10:30:45Z" (+5 days)
     * Dates.format(Dates.addMonths(date, -2), Dates.ISO_8601_DATE_TIME_FORMAT, utc);
     *                                                  // returns "2024-11-15T10:30:45Z" (-2 months)
     *
     * Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, utc);         // returns "2025-01-15T10:30:45Z" (original unchanged)
     * }</pre>
     *
     * @param <T> the concrete {@code java.util.Date} subtype of {@code date} which is also the return type.
     * @param date the date to add to, must not be {@code null}.
     * @param amount the amount to add or subtract (negative values subtract).
     * @param unit the calendar field unit to add by, must not be {@code null}.
     * @return a new date of the same type as {@code date} with the specified amount applied.
     * @throws IllegalArgumentException if {@code date} or {@code unit} is {@code null}.
     * @deprecated misleadingly named (it adds, it does not {@link Calendar#roll(int, int) roll}); use the
     *             field-specific {@code add*} methods instead, e.g. {@link #addYears(java.util.Date, int)},
     *             {@link #addMonths(java.util.Date, int)}, {@link #addWeeks(java.util.Date, int)},
     *             {@link #addDays(java.util.Date, int)}. Behavior is unchanged.
     */
    @Beta
    @Deprecated
    public static <T extends java.util.Date> T roll(final T date, final int amount, final CalendarField unit) throws IllegalArgumentException {
        return addToDate(date, amount, unit);
    }

    /**
     * Adds or subtracts the specified amount of time, expressed in the given
     * {@link TimeUnit}, to the supplied calendar and returns a new calendar of
     * the same concrete type. The original calendar is unchanged. The amount is
     * converted to milliseconds via {@link TimeUnit#toMillis(long)} and applied
     * as plain millisecond arithmetic (it does not account for daylight-saving-time
     * transitions).
     *
     * <p><b>Difference from {@link Calendar#roll(int, int)}:</b> despite its name, this method performs
     * plain <i>addition</i> that carries into larger fields &mdash; it does <b>not</b> have
     * {@code Calendar.roll} semantics, which change a single field without altering larger fields. The
     * field-specific {@code add*} methods are the preferred replacements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.createCalendar(1736937045000L);        // returns 2025-01-15T10:30:45Z
     * Dates.addDays(cal, 5).getTimeInMillis();                    // returns 1737369045000 (+5 days)
     * Dates.addHours(cal, -2).getTimeInMillis();                  // returns 1736929845000 (-2 hours)
     *
     * cal.getTimeInMillis();                                     // returns 1736937045000 (original unchanged)
     * }</pre>
     *
     * @param <T> the concrete {@code Calendar} subtype of {@code calendar} which is also the return type.
     * @param calendar the calendar to add to, must not be {@code null}.
     * @param amount the amount of time to add or subtract (negative values subtract).
     * @param unit the time unit of the {@code amount} parameter, must not be {@code null}.
     * @return a new calendar of the same type as {@code calendar} with the specified amount applied.
     * @throws IllegalArgumentException if {@code calendar} or {@code unit} is {@code null}.
     * @deprecated misleadingly named (it adds, it does not {@link Calendar#roll(int, int) roll}); use the
     *             field-specific {@code add*} methods instead, e.g. {@link #addDays(Calendar, int)},
     *             {@link #addHours(Calendar, int)}, {@link #addMinutes(Calendar, int)},
     *             {@link #addSeconds(Calendar, int)}, {@link #addMilliseconds(Calendar, int)}.
     *             Behavior is unchanged.
     */
    @Beta
    @Deprecated
    public static <T extends Calendar> T roll(final T calendar, final long amount, final TimeUnit unit) throws IllegalArgumentException {
        return addToCalendar(calendar, amount, unit);
    }

    /**
     * Adds or subtracts the specified amount of the given {@link CalendarField}
     * to the supplied calendar and returns a new calendar of the same concrete
     * type. The original calendar is unchanged. The arithmetic is performed
     * through {@link Calendar#add(int, int)}, honouring the calendar's rules
     * (including daylight-saving-time transitions).
     *
     * <p><b>Difference from {@link Calendar#roll(int, int)}:</b> despite its name, this method performs
     * plain <i>addition</i> via {@link Calendar#add(int, int)} that carries into larger fields &mdash; it
     * does <b>not</b> have {@code Calendar.roll} semantics, which change a single field without altering
     * larger fields. The field-specific {@code add*} methods are the preferred replacements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);   // returns 2025-01-15T10:30:45Z (UTC, no DST)
     * Dates.format(Dates.addDays(cal, 5), "yyyy-MM-dd HH:mm:ss", utc);
     *                                                  // returns "2025-01-20 10:30:45" (+5 days)
     * Dates.format(Dates.addHours(cal, -2), "yyyy-MM-dd HH:mm:ss", utc);
     *                                                  // returns "2025-01-15 08:30:45" (-2 hours)
     *
     * cal.getTimeInMillis();                                      // returns 1736937045000 (original unchanged)
     * }</pre>
     *
     * @param <T> the concrete {@code Calendar} subtype of {@code calendar} which is also the return type.
     * @param calendar the calendar to add to, must not be {@code null}.
     * @param amount the amount to add or subtract (negative values subtract).
     * @param unit the calendar field unit to add by, must not be {@code null}.
     * @return a new calendar of the same type as {@code calendar} with the specified amount applied.
     * @throws IllegalArgumentException if {@code calendar} or {@code unit} is {@code null}.
     * @deprecated misleadingly named (it adds, it does not {@link Calendar#roll(int, int) roll}); use the
     *             field-specific {@code add*} methods instead, e.g. {@link #addYears(Calendar, int)},
     *             {@link #addMonths(Calendar, int)}, {@link #addWeeks(Calendar, int)},
     *             {@link #addDays(Calendar, int)}. Behavior is unchanged.
     */
    @Beta
    @Deprecated
    public static <T extends Calendar> T roll(final T calendar, final int amount, final CalendarField unit) throws IllegalArgumentException {
        return addToCalendar(calendar, amount, unit);
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

    // Internal implementation of the add* family (and the deprecated roll* family). Kept private so the
    // public add* methods never delegate through the deprecated roll* methods.

    private static <T extends java.util.Date> T addToDate(final T date, final long amount, final TimeUnit unit) {
        N.checkArgNotNull(date, cs.date);
        N.checkArgNotNull(unit, cs.unit);

        return createDate(date.getTime() + unit.toMillis(amount), date.getClass());
    }

    private static <T extends java.util.Date> T addToDate(final T date, final int amount, final CalendarField unit) {
        N.checkArgNotNull(date, cs.date);
        N.checkArgNotNull(unit, cs.unit);

        // DAY_OF_MONTH and WEEK_OF_YEAR also need calendar-rule arithmetic so that crossing a
        // DST boundary preserves wall-clock time of day. Pre-fix the millisecond-arithmetic
        // path treated a day as exactly 86_400_000ms, so addDays(d, 1) on a spring-forward day
        // landed an hour off in zones that observe DST. Routing through Calendar.add fixes it.
        if (unit == CalendarField.MONTH || unit == CalendarField.YEAR || unit == CalendarField.DAY_OF_MONTH || unit == CalendarField.WEEK_OF_YEAR) {
            final Calendar c = createCalendar(date);
            //noinspection MagicConstant
            c.add(unit.value(), amount);

            return createDate(c.getTimeInMillis(), date.getClass());
        } else {
            return createDate(date.getTime() + toMillis(unit, amount), date.getClass());
        }
    }

    private static <T extends Calendar> T addToCalendar(final T calendar, final long amount, final TimeUnit unit) {
        N.checkArgNotNull(calendar, cs.calendar); //NOSONAR
        N.checkArgNotNull(unit, cs.unit);

        return createCalendar(calendar, calendar.getTimeInMillis() + unit.toMillis(amount));
    }

    private static <T extends Calendar> T addToCalendar(final T calendar, final int amount, final CalendarField unit) {
        N.checkArgNotNull(calendar, cs.calendar);
        N.checkArgNotNull(unit, cs.unit);

        final T result = createCalendar(calendar, calendar.getTimeInMillis());

        //noinspection MagicConstant
        result.add(unit.value(), amount);

        return result;
    }

    /**
     * Adds a number of years to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // parse and format in the same (default) zone so the wall-clock result is stable
     * java.util.Date date = Dates.parseJUDate("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.addYears(date, 1), "yyyy-MM-dd HH:mm:ss");    // returns "2026-01-15 10:30:45"
     * Dates.format(Dates.addYears(date, -1), "yyyy-MM-dd HH:mm:ss");   // returns "2024-01-15 10:30:45"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                       // returns "2025-01-15 10:30:45" (original unchanged)
     * Dates.addYears((java.util.Date) null, 1);                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add years to, not null.
     * @param amount the amount of years to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of years added.
     * @throws IllegalArgumentException if the date is null.
     */
    public static <T extends java.util.Date> T addYears(final T date, final int amount) {
        return addToDate(date, amount, CalendarField.YEAR);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of months to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseJUDate("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.addMonths(date, 3), "yyyy-MM-dd HH:mm:ss");    // returns "2025-04-15 10:30:45"
     * Dates.format(Dates.addMonths(date, -2), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-15 10:30:45"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                       // returns "2025-01-15 10:30:45" (original unchanged)
     * Dates.addMonths((java.util.Date) null, 3);                       // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add months to, not null.
     * @param amount the amount of months to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of months added.
     * @throws IllegalArgumentException if the date is null.
     */
    public static <T extends java.util.Date> T addMonths(final T date, final int amount) {
        return addToDate(date, amount, CalendarField.MONTH);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of weeks to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseJUDate("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.addWeeks(date, 2), "yyyy-MM-dd HH:mm:ss");    // returns "2025-01-29 10:30:45"
     * Dates.format(Dates.addWeeks(date, -1), "yyyy-MM-dd HH:mm:ss");   // returns "2025-01-08 10:30:45"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                       // returns "2025-01-15 10:30:45" (original unchanged)
     * Dates.addWeeks((java.util.Date) null, 2);                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add weeks to, not null.
     * @param amount the amount of weeks to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of weeks added.
     * @throws IllegalArgumentException if the date is null.
     */
    public static <T extends java.util.Date> T addWeeks(final T date, final int amount) {
        return addToDate(date, amount, CalendarField.WEEK_OF_YEAR);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of days to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseJUDate("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.addDays(date, 1), "yyyy-MM-dd HH:mm:ss");     // returns "2025-01-16 10:30:45"
     * Dates.format(Dates.addDays(date, -5), "yyyy-MM-dd HH:mm:ss");    // returns "2025-01-10 10:30:45"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                       // returns "2025-01-15 10:30:45" (original unchanged)
     * Dates.addDays((java.util.Date) null, 1);                         // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add days to, not null.
     * @param amount the amount of days to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of days added.
     * @throws IllegalArgumentException if the date is null.
     */
    public static <T extends java.util.Date> T addDays(final T date, final int amount) {
        return addToDate(date, amount, CalendarField.DAY_OF_MONTH);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of hours to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     *
     * // hour/minute/second arithmetic is plain millisecond math (UTC-stable)
     * java.util.Date date = new java.util.Date(1736937045000L);   // returns 2025-01-15T10:30:45Z
     *
     * Dates.format(Dates.addHours(date, 5), Dates.ISO_8601_DATE_TIME_FORMAT, utc);   // returns "2025-01-15T15:30:45Z"
     * Dates.format(Dates.addHours(date, -3), Dates.ISO_8601_DATE_TIME_FORMAT, utc);  // returns "2025-01-15T07:30:45Z"
     *
     * Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, utc);   // returns "2025-01-15T10:30:45Z" (original unchanged)
     * Dates.addHours((java.util.Date) null, 3);                   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add hours to, not null.
     * @param amount the amount of hours to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of hours added.
     * @throws IllegalArgumentException if the date is null.
     */
    public static <T extends java.util.Date> T addHours(final T date, final int amount) {
        return addToDate(date, amount, CalendarField.HOUR_OF_DAY);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of minutes to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * java.util.Date date = new java.util.Date(1736937045000L);                         // returns 2025-01-15T10:30:45Z
     * Dates.format(Dates.addMinutes(date, 30), Dates.ISO_8601_DATE_TIME_FORMAT, utc);   // returns "2025-01-15T11:00:45Z"
     * Dates.format(Dates.addMinutes(date, -15), Dates.ISO_8601_DATE_TIME_FORMAT, utc);  // returns "2025-01-15T10:15:45Z"
     *
     * Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, utc);   // returns "2025-01-15T10:30:45Z" (original unchanged)
     * Dates.addMinutes((java.util.Date) null, 30);                // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add minutes to, not null.
     * @param amount the amount of minutes to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of minutes added.
     * @throws IllegalArgumentException if the date is null.
     */
    public static <T extends java.util.Date> T addMinutes(final T date, final int amount) {
        return addToDate(date, amount, CalendarField.MINUTE);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of seconds to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * java.util.Date date = new java.util.Date(1736937045000L);                         // returns 2025-01-15T10:30:45Z
     * Dates.format(Dates.addSeconds(date, 15), Dates.ISO_8601_DATE_TIME_FORMAT, utc);   // returns "2025-01-15T10:31:00Z"
     * Dates.format(Dates.addSeconds(date, -45), Dates.ISO_8601_DATE_TIME_FORMAT, utc);  // returns "2025-01-15T10:30:00Z"
     *
     * Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, utc);   // returns "2025-01-15T10:30:45Z" (original unchanged)
     * Dates.addSeconds((java.util.Date) null, 45);                // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add seconds to, not null.
     * @param amount the amount of seconds to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of seconds added.
     * @throws IllegalArgumentException if the date is null.
     */
    public static <T extends java.util.Date> T addSeconds(final T date, final int amount) {
        return addToDate(date, amount, CalendarField.SECOND);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of milliseconds to a date returning a new object.
     * The original {@code Date} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = new java.util.Date(1736937045000L);  // returns 2025-01-15T10:30:45Z
     * Dates.addMilliseconds(date, 500).getTime();                // returns 1736937045500
     * Dates.addMilliseconds(date, -1000).getTime();              // returns 1736937044000
     *
     * date.getTime();                                    // returns 1736937045000 (original unchanged)
     * Dates.addMilliseconds((java.util.Date) null, 500); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add milliseconds to, not null.
     * @param amount the amount of milliseconds to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of milliseconds added.
     * @throws IllegalArgumentException if the date is null.
     */
    public static <T extends java.util.Date> T addMilliseconds(final T date, final int amount) {
        return addToDate(date, amount, CalendarField.MILLISECOND);
    }

    /**
     * Adds a number of milliseconds to a date returning a new object, accepting a {@code long} amount.
     * The original {@code Date} is unchanged.
     *
     * <p>This {@code long} overload complements {@link #addMilliseconds(java.util.Date, int)}: a millisecond
     * count near {@code 2^31} (about 24.8 days) overflows an {@code int}, so use this overload when the amount
     * may exceed the {@code int} range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = new java.util.Date(1736937045000L);  // 2025-01-15T10:30:45Z
     * Dates.addMilliseconds(date, 5_000_000_000L).getTime();     // returns 1741937045000 (~57.9 days later)
     * }</pre>
     *
     * @param <T> the type of the date.
     * @param date the date to add milliseconds to, not null.
     * @param amount the amount of milliseconds to add, may be negative to subtract.
     * @return a new {@code Date} instance with the specified number of milliseconds added.
     * @throws IllegalArgumentException if the date is null.
     */
    public static <T extends java.util.Date> T addMilliseconds(final T date, final long amount) {
        return addToDate(date, amount, TimeUnit.MILLISECONDS);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of years to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);            // returns 2025-01-15T10:30:45Z
     * Dates.format(Dates.addYears(cal, 1), "yyyy-MM-dd HH:mm:ss", utc);    // returns "2026-01-15 10:30:45"
     * Dates.format(Dates.addYears(cal, -1), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2024-01-15 10:30:45"
     *
     * cal.getTimeInMillis();                                     // returns 1736937045000 (original unchanged)
     * Dates.addYears((Calendar) null, 1);                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add years to, not null.
     * @param amount the amount of years to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of years added.
     * @throws IllegalArgumentException if the calendar is null.
     */
    public static <T extends Calendar> T addYears(final T calendar, final int amount) {
        return addToCalendar(calendar, amount, CalendarField.YEAR);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of months to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);             // returns 2025-01-15T10:30:45Z
     * Dates.format(Dates.addMonths(cal, 6), "yyyy-MM-dd HH:mm:ss", utc);    // returns "2025-07-15 10:30:45"
     * Dates.format(Dates.addMonths(cal, -2), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2024-11-15 10:30:45"
     *
     * cal.getTimeInMillis();                                     // returns 1736937045000 (original unchanged)
     * Dates.addMonths((Calendar) null, 6);                       // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add months to, not null.
     * @param amount the amount of months to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of months added.
     * @throws IllegalArgumentException if the calendar is null.
     */
    public static <T extends Calendar> T addMonths(final T calendar, final int amount) {
        return addToCalendar(calendar, amount, CalendarField.MONTH);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of weeks to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);            // returns 2025-01-15T10:30:45Z
     * Dates.format(Dates.addWeeks(cal, 2), "yyyy-MM-dd HH:mm:ss", utc);    // returns "2025-01-29 10:30:45"
     * Dates.format(Dates.addWeeks(cal, -1), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2025-01-08 10:30:45"
     *
     * cal.getTimeInMillis();                                     // returns 1736937045000 (original unchanged)
     * Dates.addWeeks((Calendar) null, 2);                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add weeks to, not null.
     * @param amount the amount of weeks to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of weeks added.
     * @throws IllegalArgumentException if the calendar is null.
     */
    public static <T extends Calendar> T addWeeks(final T calendar, final int amount) {
        return addToCalendar(calendar, amount, CalendarField.WEEK_OF_YEAR);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of days to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);           // returns 2025-01-15T10:30:45Z
     * Dates.format(Dates.addDays(cal, 1), "yyyy-MM-dd HH:mm:ss", utc);    // returns "2025-01-16 10:30:45"
     * Dates.format(Dates.addDays(cal, -5), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2025-01-10 10:30:45"
     *
     * cal.getTimeInMillis();                                     // returns 1736937045000 (original unchanged)
     * Dates.addDays((Calendar) null, 1);                         // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add days to, not null.
     * @param amount the amount of days to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of days added.
     * @throws IllegalArgumentException if the calendar is null.
     */
    public static <T extends Calendar> T addDays(final T calendar, final int amount) {
        return addToCalendar(calendar, amount, CalendarField.DAY_OF_MONTH);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of hours to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);            // returns 2025-01-15T10:30:45Z
     * Dates.format(Dates.addHours(cal, 5), "yyyy-MM-dd HH:mm:ss", utc);    // returns "2025-01-15 15:30:45"
     * Dates.format(Dates.addHours(cal, -3), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2025-01-15 07:30:45"
     *
     * cal.getTimeInMillis();                                     // returns 1736937045000 (original unchanged)
     * Dates.addHours((Calendar) null, 5);                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add hours to, not null.
     * @param amount the amount of hours to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of hours added.
     * @throws IllegalArgumentException if the calendar is null.
     */
    public static <T extends Calendar> T addHours(final T calendar, final int amount) {
        return addToCalendar(calendar, amount, CalendarField.HOUR_OF_DAY);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of minutes to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);              // returns 2025-01-15T10:30:45Z
     * Dates.format(Dates.addMinutes(cal, 15), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2025-01-15 10:45:45"
     * Dates.format(Dates.addMinutes(cal, -30), "yyyy-MM-dd HH:mm:ss", utc);  // returns "2025-01-15 10:00:45"
     *
     * cal.getTimeInMillis();                                     // returns 1736937045000 (original unchanged)
     * Dates.addMinutes((Calendar) null, 15);                     // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add minutes to, not null.
     * @param amount the amount of minutes to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of minutes added.
     * @throws IllegalArgumentException if the calendar is null.
     */
    public static <T extends Calendar> T addMinutes(final T calendar, final int amount) {
        return addToCalendar(calendar, amount, CalendarField.MINUTE);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of seconds to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.createCalendar(1736937045000L, utc);              // returns 2025-01-15T10:30:45Z
     * Dates.format(Dates.addSeconds(cal, 15), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2025-01-15 10:31:00"
     * Dates.format(Dates.addSeconds(cal, -45), "yyyy-MM-dd HH:mm:ss", utc);  // returns "2025-01-15 10:30:00"
     *
     * cal.getTimeInMillis();                                     // returns 1736937045000 (original unchanged)
     * Dates.addSeconds((Calendar) null, 30);                     // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add seconds to, not null.
     * @param amount the amount of seconds to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of seconds added.
     * @throws IllegalArgumentException if the calendar is null.
     */
    public static <T extends Calendar> T addSeconds(final T calendar, final int amount) {
        return addToCalendar(calendar, amount, CalendarField.SECOND);
    }

    //-----------------------------------------------------------------------

    /**
     * Adds a number of milliseconds to a calendar returning a new object.
     * The original {@code Calendar} is unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.createCalendar(1736937045000L);   // returns 2025-01-15T10:30:45Z
     * Dates.addMilliseconds(cal, 250).getTimeInMillis();     // returns 1736937045250
     * Dates.addMilliseconds(cal, -1000).getTimeInMillis();   // returns 1736937044000
     *
     * cal.getTimeInMillis();                                 // returns 1736937045000 (original unchanged)
     * Dates.addMilliseconds((Calendar) null, 250);           // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add milliseconds to, not null.
     * @param amount the amount of milliseconds to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of milliseconds added.
     * @throws IllegalArgumentException if the calendar is null.
     */
    public static <T extends Calendar> T addMilliseconds(final T calendar, final int amount) {
        return addToCalendar(calendar, amount, CalendarField.MILLISECOND);
    }

    /**
     * Adds a number of milliseconds to a calendar returning a new object, accepting a {@code long} amount.
     * The original {@code Calendar} is unchanged.
     *
     * <p>This {@code long} overload complements {@link #addMilliseconds(Calendar, int)}: a millisecond count
     * near {@code 2^31} (about 24.8 days) overflows an {@code int}, so use this overload when the amount may
     * exceed the {@code int} range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Dates.createCalendar(1736937045000L);            // 2025-01-15T10:30:45Z
     * Dates.addMilliseconds(cal, 5_000_000_000L).getTimeInMillis();   // returns 1741937045000 (~57.9 days later)
     * }</pre>
     *
     * @param <T> the type of the calendar.
     * @param calendar the calendar to add milliseconds to, not null.
     * @param amount the amount of milliseconds to add, may be negative to subtract.
     * @return a new {@code Calendar} instance with the specified number of milliseconds added.
     * @throws IllegalArgumentException if the calendar is null.
     */
    public static <T extends Calendar> T addMilliseconds(final T calendar, final long amount) {
        return addToCalendar(calendar, amount, TimeUnit.MILLISECONDS);
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
     * // parse and format in the same (default) zone so the result is stable
     * java.util.Date date = Dates.parseJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.round(date, Calendar.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-24 11:00:00" (rounds up, 30 min)
     * Dates.format(Dates.round(date, Calendar.DAY_OF_MONTH), "yyyy-MM-dd HH:mm:ss");  // returns "2024-11-24 00:00:00" (rounds down)
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                                      // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.round((java.util.Date) null, Calendar.HOUR_OF_DAY);                       // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date to work with, not null.
     * @param field the field from {@code Calendar} or {@code SEMI_MONTH}.
     * @return a new date object of type T, rounded to the nearest whole unit as specified by the field.
     * @throws IllegalArgumentException if the date is null, or if {@code field} is not a supported Calendar field.
     * @throws ArithmeticException if the year is over 280 million.
     * @see #round(java.util.Date, CalendarField)
     * @see #truncate(java.util.Date, int)
     * @see #ceiling(java.util.Date, int)
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
     * java.util.Date date = Dates.parseJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.round(date, CalendarField.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss");  // returns "2024-11-24 11:00:00"
     * Dates.format(Dates.round(date, CalendarField.MONTH), "yyyy-MM-dd HH:mm:ss");        // returns "2024-12-01 00:00:00" (rounds up)
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                                          // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.round((java.util.Date) null, CalendarField.HOUR_OF_DAY);                      // throws IllegalArgumentException
     * }</pre>
     *
     * <p><b>&#9888;</b> {@link CalendarField#WEEK_OF_YEAR} is not supported by this overload and throws
     * {@link IllegalArgumentException}; the underlying rounding algorithm supports calendar fields with
     * fixed intra-month/year ordering, not week-based boundaries.</p>
     *
     * @param <T> the type of the date object, which must be a subclass of java.util.Date.
     * @param date the date to be rounded. Must not be {@code null}.
     * @param field the CalendarField to which the date is to be rounded. Must not be {@code null}.
     * @return a new date object of type T, rounded to the nearest whole unit as specified by the field.
     * @throws IllegalArgumentException if the date or field is {@code null}, or if the field is not supported.
     * @throws ArithmeticException if the year is over 280 million.
     * @see #round(java.util.Date, int)
     * @see #truncate(java.util.Date, CalendarField)
     * @see #ceiling(java.util.Date, CalendarField)
     */
    public static <T extends java.util.Date> T round(final T date, final CalendarField field) {
        N.checkArgNotNull(date, cs.date);
        N.checkArgNotNull(field, cs.field);

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
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseCalendar("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss", utc);
     * Dates.format(Dates.round(cal, Calendar.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2024-11-24 11:00:00"
     * Dates.format(Dates.round(cal, Calendar.DAY_OF_MONTH), "yyyy-MM-dd HH:mm:ss", utc);  // returns "2024-11-24 00:00:00"
     *
     * Dates.format(cal, "yyyy-MM-dd HH:mm:ss", utc);                                      // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.round((Calendar) null, Calendar.HOUR_OF_DAY);                                 // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the calendar object, which must extend java.util.Calendar.
     * @param calendar the calendar to work with, not null.
     * @param field the field from {@code Calendar} or {@code SEMI_MONTH}.
     * @return a new calendar object of type T, rounded to the nearest whole unit as specified by the field.
     * @throws IllegalArgumentException if the calendar is {@code null}, or if {@code field} is not a supported Calendar field.
     * @throws ArithmeticException if the year is over 280 million.
     * @see #round(Calendar, CalendarField)
     * @see #truncate(Calendar, int)
     * @see #ceiling(Calendar, int)
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
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseCalendar("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss", utc);
     * Dates.format(Dates.round(cal, CalendarField.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2024-11-24 11:00:00"
     * Dates.format(Dates.round(cal, CalendarField.MONTH), "yyyy-MM-dd HH:mm:ss", utc);         // returns "2024-12-01 00:00:00"
     *
     * Dates.format(cal, "yyyy-MM-dd HH:mm:ss", utc);                                           // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.round((Calendar) null, CalendarField.HOUR_OF_DAY);                                 // throws IllegalArgumentException
     * }</pre>
     *
     * <p><b>&#9888;</b> {@link CalendarField#WEEK_OF_YEAR} is not supported by this overload and throws
     * {@link IllegalArgumentException}; the underlying rounding algorithm supports calendar fields with
     * fixed intra-month/year ordering, not week-based boundaries.</p>
     *
     * @param <T> the type of the calendar object, which must be a subclass of java.util.Calendar.
     * @param calendar the calendar to be rounded. Must not be {@code null}.
     * @param field the CalendarField to which the calendar is to be rounded. Must not be {@code null}.
     * @return a new calendar object of type T, rounded to the nearest whole unit as specified by the field.
     * @throws IllegalArgumentException if the calendar or field is {@code null}, or if the field is not supported.
     * @throws ArithmeticException if the year is over 280 million.
     * @see #round(Calendar, int)
     * @see #truncate(Calendar, CalendarField)
     * @see #ceiling(Calendar, CalendarField)
     */
    public static <T extends Calendar> T round(final T calendar, final CalendarField field) {
        N.checkArgNotNull(calendar, cs.calendar);
        N.checkArgNotNull(field, cs.field);

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
     * java.util.Date date = Dates.parseJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.truncate(date, Calendar.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-24 10:00:00" (clears below hour)
     * Dates.format(Dates.truncate(date, Calendar.MONTH), "yyyy-MM-dd HH:mm:ss");         // returns "2024-11-01 00:00:00"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                                         // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.truncate((java.util.Date) null, Calendar.HOUR_OF_DAY);                       // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date to work with, not null.
     * @param field the field from {@code Calendar} or {@code SEMI_MONTH}.
     * @return a new date object of type T, truncated to the specified field.
     * @throws IllegalArgumentException if the date is {@code null}, or if {@code field} is not a supported Calendar field.
     * @throws ArithmeticException if the year is over 280 million.
     * @see #truncate(java.util.Date, CalendarField)
     * @see #round(java.util.Date, int)
     * @see #ceiling(java.util.Date, int)
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
     * java.util.Date date = Dates.parseJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.truncate(date, CalendarField.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-24 10:00:00"
     * Dates.format(Dates.truncate(date, CalendarField.MONTH), "yyyy-MM-dd HH:mm:ss");         // returns "2024-11-01 00:00:00"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                                              // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.truncate((java.util.Date) null, CalendarField.HOUR_OF_DAY);                       // throws IllegalArgumentException
     * }</pre>
     *
     * <p><b>&#9888;</b> {@link CalendarField#WEEK_OF_YEAR} is not supported by this overload and throws
     * {@link IllegalArgumentException}; the underlying truncation algorithm supports calendar fields with
     * fixed intra-month/year ordering, not week-based boundaries.</p>
     *
     * @param <T> the type of the date object, which must be a subclass of java.util.Date.
     * @param date the date to be truncated. Must not be {@code null}.
     * @param field the CalendarField to which the date is to be truncated. Must not be {@code null}.
     * @return a new date object of type T, truncated to the specified field.
     * @throws IllegalArgumentException if the date or field is {@code null}, or if the field is not supported.
     * @throws ArithmeticException if the year is over 280 million.
     * @see #truncate(java.util.Date, int)
     * @see #round(java.util.Date, CalendarField)
     * @see #ceiling(java.util.Date, CalendarField)
     */
    public static <T extends java.util.Date> T truncate(final T date, final CalendarField field) {
        N.checkArgNotNull(date, cs.date);
        N.checkArgNotNull(field, cs.field);

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
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseCalendar("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss", utc);
     * Dates.format(Dates.truncate(cal, Calendar.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2024-11-24 10:00:00"
     * Dates.format(Dates.truncate(cal, Calendar.MONTH), "yyyy-MM-dd HH:mm:ss", utc);         // returns "2024-11-01 00:00:00"
     *
     * Dates.format(cal, "yyyy-MM-dd HH:mm:ss", utc);                                         // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.truncate((Calendar) null, Calendar.HOUR_OF_DAY);                                 // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the calendar object, which must extend java.util.Calendar.
     * @param calendar the calendar to work with, not null.
     * @param field the field from {@code Calendar} or {@code SEMI_MONTH}.
     * @return a new calendar object of type T, truncated to the specified field.
     * @throws IllegalArgumentException if the calendar is {@code null}, or if {@code field} is not a supported Calendar field.
     * @throws ArithmeticException if the year is over 280 million.
     * @see #truncate(Calendar, CalendarField)
     * @see #round(Calendar, int)
     * @see #ceiling(Calendar, int)
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
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseCalendar("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss", utc);
     * Dates.format(Dates.truncate(cal, CalendarField.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2024-11-24 10:00:00"
     * Dates.format(Dates.truncate(cal, CalendarField.MONTH), "yyyy-MM-dd HH:mm:ss", utc);         // returns "2024-11-01 00:00:00"
     *
     * Dates.format(cal, "yyyy-MM-dd HH:mm:ss", utc);                                              // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.truncate((Calendar) null, CalendarField.HOUR_OF_DAY);                                 // throws IllegalArgumentException
     * }</pre>
     *
     * <p><b>&#9888;</b> {@link CalendarField#WEEK_OF_YEAR} is not supported by this overload and throws
     * {@link IllegalArgumentException}; the underlying truncation algorithm supports calendar fields with
     * fixed intra-month/year ordering, not week-based boundaries.</p>
     *
     * @param <T> the type of the calendar object, which must be a subclass of java.util.Calendar.
     * @param calendar the calendar to be truncated. Must not be {@code null}.
     * @param field the CalendarField to which the calendar is to be truncated. Must not be {@code null}.
     * @return a new calendar object of type T, truncated to the specified field.
     * @throws IllegalArgumentException if the calendar or field is {@code null}, or if the field is not supported.
     * @throws ArithmeticException if the year is over 280 million.
     * @see #truncate(Calendar, int)
     * @see #round(Calendar, CalendarField)
     * @see #ceiling(Calendar, CalendarField)
     */
    public static <T extends Calendar> T truncate(final T calendar, final CalendarField field) {
        N.checkArgNotNull(calendar, cs.calendar);
        N.checkArgNotNull(field, cs.field);

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
     * java.util.Date date = Dates.parseJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.ceiling(date, Calendar.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-24 11:00:00" (always rounds up)
     * Dates.format(Dates.ceiling(date, Calendar.DAY_OF_MONTH), "yyyy-MM-dd HH:mm:ss");  // returns "2024-11-25 00:00:00"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                                        // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.ceiling((java.util.Date) null, Calendar.HOUR_OF_DAY);                       // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the date object, which must extend java.util.Date.
     * @param date the date to work with, not null.
     * @param field the field from {@code Calendar} or {@code SEMI_MONTH}.
     * @return a new date object of type T, adjusted to the ceiling of the specified field.
     * @throws IllegalArgumentException if the date is {@code null}, or if {@code field} is not a supported Calendar field.
     * @throws ArithmeticException if the year is over 280 million.
     * @see #ceiling(java.util.Date, CalendarField)
     * @see #round(java.util.Date, int)
     * @see #truncate(java.util.Date, int)
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
     * java.util.Date date = Dates.parseJUDate("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss");
     * Dates.format(Dates.ceiling(date, CalendarField.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss");   // returns "2024-11-24 11:00:00"
     * Dates.format(Dates.ceiling(date, CalendarField.MONTH), "yyyy-MM-dd HH:mm:ss");         // returns "2024-12-01 00:00:00"
     *
     * Dates.format(date, "yyyy-MM-dd HH:mm:ss");                                             // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.ceiling((java.util.Date) null, CalendarField.HOUR_OF_DAY);                       // throws IllegalArgumentException
     * }</pre>
     *
     * <p><b>&#9888;</b> {@link CalendarField#WEEK_OF_YEAR} is not supported by this overload and throws
     * {@link IllegalArgumentException}; the underlying ceiling algorithm supports calendar fields with
     * fixed intra-month/year ordering, not week-based boundaries.</p>
     *
     * @param <T> the type of the date object, which must be a subclass of {@code java.util.Date}.
     * @param date the date to be adjusted. Must not be {@code null}.
     * @param field the CalendarField to which the date is to be adjusted. Must not be {@code null}.
     * @return a new date object of type T, adjusted to the nearest future unit as specified by the field.
     * @throws IllegalArgumentException if the date or field is {@code null}, or if the field is not supported.
     * @throws ArithmeticException if the year is over 280 million.
     * @see #ceiling(java.util.Date, int)
     * @see #round(java.util.Date, CalendarField)
     * @see #truncate(java.util.Date, CalendarField)
     */
    public static <T extends java.util.Date> T ceiling(final T date, final CalendarField field) {
        N.checkArgNotNull(date, cs.date);
        N.checkArgNotNull(field, cs.field);

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
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseCalendar("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss", utc);
     * Dates.format(Dates.ceiling(cal, Calendar.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2024-11-24 11:00:00"
     * Dates.format(Dates.ceiling(cal, Calendar.DAY_OF_MONTH), "yyyy-MM-dd HH:mm:ss", utc);  // returns "2024-11-25 00:00:00"
     *
     * Dates.format(cal, "yyyy-MM-dd HH:mm:ss", utc);                                        // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.ceiling((Calendar) null, Calendar.HOUR_OF_DAY);                                 // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the calendar object, which must extend java.util.Calendar.
     * @param calendar the calendar to work with, not null.
     * @param field the field from {@code Calendar} or {@code SEMI_MONTH}.
     * @return a new calendar object of type T, adjusted to the ceiling of the specified field.
     * @throws IllegalArgumentException if the calendar is {@code null}, or if {@code field} is not a supported Calendar field.
     * @throws ArithmeticException if the year is over 280 million.
     * @see #ceiling(Calendar, CalendarField)
     * @see #round(Calendar, int)
     * @see #truncate(Calendar, int)
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
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseCalendar("2024-11-24 10:30:45", "yyyy-MM-dd HH:mm:ss", utc);
     * Dates.format(Dates.ceiling(cal, CalendarField.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss", utc);   // returns "2024-11-24 11:00:00"
     * Dates.format(Dates.ceiling(cal, CalendarField.MONTH), "yyyy-MM-dd HH:mm:ss", utc);         // returns "2024-12-01 00:00:00"
     *
     * Dates.format(cal, "yyyy-MM-dd HH:mm:ss", utc);                                             // returns "2024-11-24 10:30:45" (original unchanged)
     * Dates.ceiling((Calendar) null, CalendarField.HOUR_OF_DAY);                                 // throws IllegalArgumentException
     * }</pre>
     *
     * <p><b>&#9888;</b> {@link CalendarField#WEEK_OF_YEAR} is not supported by this overload and throws
     * {@link IllegalArgumentException}; the underlying ceiling algorithm supports calendar fields with
     * fixed intra-month/year ordering, not week-based boundaries.</p>
     *
     * @param <T> the type of the calendar object, which must extend {@code java.util.Calendar}.
     * @param calendar the original calendar object to be adjusted. Must not be {@code null}.
     * @param field the field to be used for the ceiling operation, as a CalendarField. Must not be {@code null}.
     * @return a new calendar object representing the adjusted time.
     * @throws IllegalArgumentException if the calendar or field is {@code null}, or if the field is not supported.
     * @throws ArithmeticException if the year is over 280 million.
     * @see #ceiling(Calendar, int)
     * @see #round(Calendar, CalendarField)
     * @see #truncate(Calendar, CalendarField)
     */
    public static <T extends Calendar> T ceiling(final T calendar, final CalendarField field) {
        N.checkArgNotNull(calendar, cs.calendar);
        N.checkArgNotNull(field, cs.field);

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
     * Calendar cal1 = Dates.parseCalendar("2023-03-28 13:45:30", "yyyy-MM-dd HH:mm:ss");
     * Calendar cal2 = Dates.parseCalendar("2023-03-28 18:20:15", "yyyy-MM-dd HH:mm:ss");
     * Dates.truncatedEquals(cal1, cal2, CalendarField.DAY_OF_MONTH);   // returns true (same day)
     * Dates.truncatedEquals(cal1, cal2, CalendarField.HOUR_OF_DAY);    // returns false (13:00 vs 18:00)
     *
     * Dates.truncatedEquals(cal1, cal1, CalendarField.SECOND);                  // returns true (identical)
     * Dates.truncatedEquals((Calendar) null, cal2, CalendarField.DAY_OF_MONTH); // throws IllegalArgumentException
     * }</pre>
     *
     * @param cal1 the first calendar, not null.
     * @param cal2 the second calendar, not null.
     * @param field the field from {@code CalendarField} to be the most significant field for comparison.
     * @return {@code true} if cal1 and cal2 are equal up to the specified field; {@code false} otherwise.
     * @throws IllegalArgumentException if any argument is {@code null}.
     * @see #truncate(Calendar, CalendarField)
     * @see #truncatedEquals(java.util.Date, java.util.Date, CalendarField)
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
     * Calendar cal1 = Dates.parseCalendar("2023-03-28 13:45:30", "yyyy-MM-dd HH:mm:ss");
     * Calendar cal2 = Dates.parseCalendar("2023-03-28 18:20:15", "yyyy-MM-dd HH:mm:ss");
     * Dates.truncatedEquals(cal1, cal2, Calendar.DAY_OF_MONTH);   // returns true (same day)
     * Dates.truncatedEquals(cal1, cal2, Calendar.HOUR_OF_DAY);    // returns false (13:00 vs 18:00)
     *
     * Dates.truncatedEquals(cal1, cal1, Calendar.SECOND);                  // returns true (identical)
     * Dates.truncatedEquals((Calendar) null, cal2, Calendar.DAY_OF_MONTH); // throws IllegalArgumentException
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
     * java.util.Date date1 = Dates.parseJUDate("2023-03-28 13:45:30", "yyyy-MM-dd HH:mm:ss");
     * java.util.Date date2 = Dates.parseJUDate("2023-03-28 18:20:15", "yyyy-MM-dd HH:mm:ss");
     * Dates.truncatedEquals(date1, date2, CalendarField.DAY_OF_MONTH);   // returns true (same day)
     * Dates.truncatedEquals(date1, date2, CalendarField.HOUR_OF_DAY);    // returns false (13:00 vs 18:00)
     *
     * Dates.truncatedEquals(date1, date1, CalendarField.SECOND);                       // returns true (identical)
     * Dates.truncatedEquals((java.util.Date) null, date2, CalendarField.DAY_OF_MONTH); // throws IllegalArgumentException
     * }</pre>
     *
     * @param date1 the first date, not {@code null}.
     * @param date2 the second date, not {@code null}.
     * @param field the field from {@code CalendarField} to be the most significant field for comparison.
     * @return {@code true} if date1 and date2 are equal up to the specified field; {@code false} otherwise.
     * @throws IllegalArgumentException if any argument is {@code null}.
     * @see #truncate(java.util.Date, CalendarField)
     * @see #truncatedEquals(Calendar, Calendar, CalendarField)
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
     * java.util.Date date1 = Dates.parseJUDate("2023-03-28 13:45:30", "yyyy-MM-dd HH:mm:ss");
     * java.util.Date date2 = Dates.parseJUDate("2023-03-28 18:20:15", "yyyy-MM-dd HH:mm:ss");
     * Dates.truncatedEquals(date1, date2, Calendar.DAY_OF_MONTH);   // returns true (same day)
     * Dates.truncatedEquals(date1, date2, Calendar.HOUR_OF_DAY);    // returns false (13:00 vs 18:00)
     *
     * Dates.truncatedEquals(date1, date1, Calendar.SECOND);                       // returns true (identical)
     * Dates.truncatedEquals((java.util.Date) null, date2, Calendar.DAY_OF_MONTH); // throws IllegalArgumentException
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
     * Calendar cal1 = Dates.parseCalendar("2023-03-28 13:45:30", "yyyy-MM-dd HH:mm:ss");
     * Calendar cal2 = Dates.parseCalendar("2023-03-28 18:20:15", "yyyy-MM-dd HH:mm:ss");
     * Dates.truncatedCompareTo(cal1, cal2, CalendarField.DAY_OF_MONTH);       // returns 0 (same day)
     * (Dates.truncatedCompareTo(cal1, cal2, CalendarField.HOUR_OF_DAY) < 0);  // returns true (13:00 < 18:00)
     * (Dates.truncatedCompareTo(cal2, cal1, CalendarField.HOUR_OF_DAY) > 0);  // returns true (18:00 > 13:00)
     *
     * Dates.truncatedCompareTo((Calendar) null, cal2, CalendarField.DAY_OF_MONTH); // throws IllegalArgumentException
     * }</pre>
     *
     * @param cal1 the first Calendar instance to be compared, not {@code null}.
     * @param cal2 the second Calendar instance to be compared, not {@code null}.
     * @param field the field from {@code CalendarField} to be the most significant field for comparison.
     * @return a negative integer, zero, or a positive integer as the first Calendar is less than, equal to, or greater than the second.
     * @throws IllegalArgumentException if any argument is {@code null}.
     * @see #truncate(Calendar, CalendarField)
     * @see #truncatedCompareTo(java.util.Date, java.util.Date, CalendarField)
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
     * Calendar cal1 = Dates.parseCalendar("2023-03-28 13:45:30", "yyyy-MM-dd HH:mm:ss");
     * Calendar cal2 = Dates.parseCalendar("2023-03-28 18:20:15", "yyyy-MM-dd HH:mm:ss");
     * Dates.truncatedCompareTo(cal1, cal2, Calendar.DAY_OF_MONTH);       // returns 0 (same day)
     * (Dates.truncatedCompareTo(cal1, cal2, Calendar.HOUR_OF_DAY) < 0);  // returns true (13:00 < 18:00)
     * (Dates.truncatedCompareTo(cal2, cal1, Calendar.HOUR_OF_DAY) > 0);  // returns true (18:00 > 13:00)
     *
     * Dates.truncatedCompareTo((Calendar) null, cal2, Calendar.DAY_OF_MONTH); // throws IllegalArgumentException
     * }</pre>
     *
     * @param cal1 the first calendar, not {@code null}.
     * @param cal2 the second calendar, not {@code null}.
     * @param field the field from {@code Calendar}.
     * @return a negative integer, zero, or a positive integer as the first
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
     * java.util.Date date1 = Dates.parseJUDate("2023-03-28 13:45:30", "yyyy-MM-dd HH:mm:ss");
     * java.util.Date date2 = Dates.parseJUDate("2023-03-28 18:20:15", "yyyy-MM-dd HH:mm:ss");
     * Dates.truncatedCompareTo(date1, date2, CalendarField.DAY_OF_MONTH);       // returns 0 (same day)
     * (Dates.truncatedCompareTo(date1, date2, CalendarField.HOUR_OF_DAY) < 0);  // returns true (13:00 < 18:00)
     * (Dates.truncatedCompareTo(date2, date1, CalendarField.HOUR_OF_DAY) > 0);  // returns true (18:00 > 13:00)
     *
     * Dates.truncatedCompareTo((java.util.Date) null, date2, CalendarField.DAY_OF_MONTH); // throws IllegalArgumentException
     * }</pre>
     *
     * @param date1 the first Date instance to be compared, not {@code null}.
     * @param date2 the second Date instance to be compared, not {@code null}.
     * @param field the field from {@code CalendarField} to be the most significant field for comparison.
     * @return a negative integer, zero, or a positive integer as the first Date is less than, equal to, or greater than the second.
     * @throws IllegalArgumentException if any argument is {@code null}.
     * @see #truncate(java.util.Date, CalendarField)
     * @see #truncatedCompareTo(Calendar, Calendar, CalendarField)
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
     * java.util.Date date1 = Dates.parseJUDate("2023-03-28 13:45:30", "yyyy-MM-dd HH:mm:ss");
     * java.util.Date date2 = Dates.parseJUDate("2023-03-28 18:20:15", "yyyy-MM-dd HH:mm:ss");
     * Dates.truncatedCompareTo(date1, date2, Calendar.DAY_OF_MONTH);       // returns 0 (same day)
     * (Dates.truncatedCompareTo(date1, date2, Calendar.HOUR_OF_DAY) < 0);  // returns true (13:00 < 18:00)
     * (Dates.truncatedCompareTo(date2, date1, Calendar.HOUR_OF_DAY) > 0);  // returns true (18:00 > 13:00)
     *
     * Dates.truncatedCompareTo((java.util.Date) null, date2, Calendar.DAY_OF_MONTH); // throws IllegalArgumentException
     * }</pre>
     *
     * @param date1 the first date, not {@code null}.
     * @param date2 the second date, not {@code null}.
     * @param field the field from {@code Calendar}.
     * @return a negative integer, zero, or a positive integer as the first
     * date is less than, equal to, or greater than the second.
     * @throws IllegalArgumentException if any argument is {@code null}.
     * @see #truncate(java.util.Date, int)
     * @see #truncatedCompareTo(Calendar, Calendar, int)
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
     * your fragment is {@code Calendar.DATE} or {@code Calendar.DAY_OF_YEAR}. The result will
     * be all milliseconds of the past hour(s), minute(s) and second(s).</p>
     *
     * <p>Valid fragments are: {@code Calendar.YEAR}, {@code Calendar.MONTH}, both
     * {@code Calendar.DAY_OF_YEAR} and {@code Calendar.DATE}, {@code Calendar.HOUR_OF_DAY},
     * {@code Calendar.MINUTE}, {@code Calendar.SECOND} and {@code Calendar.MILLISECOND}.
     * A fragment less than or equal to a MILLISECOND field will return 0.</p>
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
     * java.util.Date date = Dates.parseJUDate("2023-01-06 07:15:10.538", "yyyy-MM-dd HH:mm:ss.SSS");
     * Dates.getFragmentInMilliseconds(date, CalendarField.SECOND);        // returns 538 (millis within the current second)
     * Dates.getFragmentInMilliseconds(date, CalendarField.MINUTE);        // returns 10538 (10*1000 + 538)
     *
     * Dates.getFragmentInMilliseconds(date, CalendarField.MILLISECOND);             // returns 0 (cannot split a ms into ms)
     * Dates.getFragmentInMilliseconds((java.util.Date) null, CalendarField.SECOND); // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date to work with, not {@code null}.
     * @param fragment the {@code CalendarField} fragment of {@code date} to calculate.
     * @return the number of milliseconds within the fragment of {@code date}.
     * @throws IllegalArgumentException if {@code date} is {@code null} or the fragment is not supported.
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
     * your fragment is {@code Calendar.DATE} or {@code Calendar.DAY_OF_YEAR}. The result will
     * be all seconds of the past hour(s) and minute(s).</p>
     *
     * <p>Valid fragments are: {@code Calendar.YEAR}, {@code Calendar.MONTH}, both
     * {@code Calendar.DAY_OF_YEAR} and {@code Calendar.DATE}, {@code Calendar.HOUR_OF_DAY},
     * {@code Calendar.MINUTE}, {@code Calendar.SECOND} and {@code Calendar.MILLISECOND}.
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
     * java.util.Date date = Dates.parseJUDate("2023-01-06 07:15:10.538", "yyyy-MM-dd HH:mm:ss.SSS");
     * Dates.getFragmentInSeconds(date, CalendarField.MINUTE);        // returns 10 (seconds within the current minute)
     * Dates.getFragmentInSeconds(date, CalendarField.HOUR_OF_DAY);   // returns 910 (15*60 + 10)
     *
     * Dates.getFragmentInSeconds(date, CalendarField.SECOND);                  // returns 0 (fragment <= SECOND yields 0)
     * Dates.getFragmentInSeconds((java.util.Date) null, CalendarField.MINUTE); // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date to work with, not {@code null}.
     * @param fragment the {@code CalendarField} fragment of {@code date} to calculate.
     * @return the number of seconds within the fragment of {@code date}.
     * @throws IllegalArgumentException if {@code date} is {@code null} or the fragment is not supported.
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
     * your fragment is {@code Calendar.MONTH}. The result will be all minutes of the
     * past day(s) and hour(s).</p>
     *
     * <p>Valid fragments are: {@code Calendar.YEAR}, {@code Calendar.MONTH}, both
     * {@code Calendar.DAY_OF_YEAR} and {@code Calendar.DATE}, {@code Calendar.HOUR_OF_DAY},
     * {@code Calendar.MINUTE}, {@code Calendar.SECOND} and {@code Calendar.MILLISECOND}.
     * A fragment less than or equal to a MINUTE field will return 0.</p>
     *
     * <ul>
     *  <li>January 1, 2008 7:15:10.538 with Calendar.HOUR_OF_DAY as fragment will return 15
     *   (equivalent to deprecated date.getMinutes())</li>
     *  <li>January 6, 2008 7:15:10.538 with Calendar.HOUR_OF_DAY as fragment will return 15
     *   (equivalent to deprecated date.getMinutes())</li>
     *  <li>January 1, 2008 7:15:10.538 with Calendar.MONTH as fragment will return 435 (7*60 + 15)</li>
     *  <li>January 6, 2008 7:15:10.538 with Calendar.MONTH as fragment will return 7635 (5*24*60 + 7*60 + 15)</li>
     *  <li>January 16, 2008 7:15:10.538 with Calendar.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in minutes)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseJUDate("2023-01-06 07:15:10.538", "yyyy-MM-dd HH:mm:ss.SSS");
     * Dates.getFragmentInMinutes(date, CalendarField.HOUR_OF_DAY);   // returns 15 (minutes within the current hour)
     * Dates.getFragmentInMinutes(date, CalendarField.DAY_OF_MONTH);  // returns 435 (7*60 + 15)
     *
     * Dates.getFragmentInMinutes(date, CalendarField.MINUTE);                       // returns 0 (fragment <= MINUTE yields 0)
     * Dates.getFragmentInMinutes((java.util.Date) null, CalendarField.HOUR_OF_DAY); // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date to work with, not {@code null}.
     * @param fragment the {@code CalendarField} fragment of {@code date} to calculate.
     * @return the number of minutes within the fragment of {@code date}.
     * @throws IllegalArgumentException if {@code date} is {@code null} or the fragment is not supported.
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
     * your fragment is {@code Calendar.MONTH}. The result will be all hours of the
     * past day(s).</p>
     *
     * <p>Valid fragments are: {@code Calendar.YEAR}, {@code Calendar.MONTH}, both
     * {@code Calendar.DAY_OF_YEAR} and {@code Calendar.DATE}, {@code Calendar.HOUR_OF_DAY},
     * {@code Calendar.MINUTE}, {@code Calendar.SECOND} and {@code Calendar.MILLISECOND}.
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
     * java.util.Date date = Dates.parseJUDate("2023-01-06 07:15:10.538", "yyyy-MM-dd HH:mm:ss.SSS");
     * Dates.getFragmentInHours(date, CalendarField.DAY_OF_MONTH);    // returns 7 (hours within the current day)
     * Dates.getFragmentInHours(date, CalendarField.MONTH);           // returns 127 (5 full days * 24 + 7)
     *
     * Dates.getFragmentInHours(date, CalendarField.HOUR_OF_DAY);                   // returns 0 (fragment <= HOUR yields 0)
     * Dates.getFragmentInHours((java.util.Date) null, CalendarField.DAY_OF_MONTH); // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date to work with, not {@code null}.
     * @param fragment the {@code CalendarField} fragment of {@code date} to calculate.
     * @return the number of hours within the fragment of {@code date}.
     * @throws IllegalArgumentException if {@code date} is {@code null} or the fragment is not supported.
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
     * your fragment is {@code Calendar.YEAR}. The result will be all days of the
     * past month(s).</p>
     *
     * <p>Valid fragments are: {@code Calendar.YEAR}, {@code Calendar.MONTH}, both
     * {@code Calendar.DAY_OF_YEAR} and {@code Calendar.DATE}, {@code Calendar.HOUR_OF_DAY},
     * {@code Calendar.MINUTE}, {@code Calendar.SECOND} and {@code Calendar.MILLISECOND}.
     * A fragment less than or equal to a DAY field will return 0.</p>
     *
     * <ul>
     *  <li>January 28, 2008 with Calendar.MONTH as fragment will return 28
     *   (equivalent to deprecated date.getDate())</li>
     *  <li>February 28, 2008 with Calendar.MONTH as fragment will return 28
     *   (equivalent to deprecated date.getDate())</li>
     *  <li>January 28, 2008 with Calendar.YEAR as fragment will return 28</li>
     *  <li>February 28, 2008 with Calendar.YEAR as fragment will return 59</li>
     *  <li>January 28, 2008 with Calendar.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in days)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date date = Dates.parseJUDate("2023-02-28 12:00:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.getFragmentInDays(date, CalendarField.MONTH);            // returns 28 (day-of-month)
     * Dates.getFragmentInDays(date, CalendarField.YEAR);             // returns 59 (31 in Jan + 28 = day-of-year)
     *
     * Dates.getFragmentInDays(date, CalendarField.DAY_OF_MONTH);          // returns 0 (fragment <= DAY yields 0)
     * Dates.getFragmentInDays((java.util.Date) null, CalendarField.YEAR); // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date to work with, not {@code null}.
     * @param fragment the {@code CalendarField} fragment of {@code date} to calculate.
     * @return the number of days within the fragment of {@code date}.
     * @throws IllegalArgumentException if {@code date} is {@code null} or the fragment is not supported.
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
     * @param date the date to work with, not {@code null}.
     * @param fragment the {@code Calendar} field part of {@code date} to calculate.
     * @param unit the time unit.
     * @return the number of units within the fragment of {@code date}.
     * @throws IllegalArgumentException if {@code date} is {@code null} or the specified fragment is not supported.
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
     * For example, if you want to calculate the number of milliseconds past today,
     * your fragment is {@code Calendar.DATE} or {@code Calendar.DAY_OF_YEAR}. The result will
     * be all milliseconds of the past hour(s), minute(s) and second(s).</p>
     *
     * <p>Valid fragments are: {@code Calendar.YEAR}, {@code Calendar.MONTH}, both
     * {@code Calendar.DAY_OF_YEAR} and {@code Calendar.DATE}, {@code Calendar.HOUR_OF_DAY},
     * {@code Calendar.MINUTE}, {@code Calendar.SECOND} and {@code Calendar.MILLISECOND}.
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
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseCalendar("2023-01-06 07:15:10.538", "yyyy-MM-dd HH:mm:ss.SSS", utc);
     * Dates.getFragmentInMilliseconds(cal, CalendarField.SECOND);        // returns 538 (millis within the current second)
     * Dates.getFragmentInMilliseconds(cal, CalendarField.MINUTE);        // returns 10538 (10*1000 + 538)
     *
     * Dates.getFragmentInMilliseconds(cal, CalendarField.MILLISECOND);        // returns 0 (cannot split a ms into ms)
     * Dates.getFragmentInMilliseconds((Calendar) null, CalendarField.SECOND); // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the calendar to work with, not {@code null}.
     * @param fragment the {@code CalendarField} fragment of {@code calendar} to calculate.
     * @return the number of milliseconds within the fragment of {@code calendar}.
     * @throws IllegalArgumentException if {@code calendar} is {@code null} or the fragment is not supported.
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
     * your fragment is {@code Calendar.DATE} or {@code Calendar.DAY_OF_YEAR}. The result will
     * be all seconds of the past hour(s) and minute(s).</p>
     *
     * <p>Valid fragments are: {@code Calendar.YEAR}, {@code Calendar.MONTH}, both
     * {@code Calendar.DAY_OF_YEAR} and {@code Calendar.DATE}, {@code Calendar.HOUR_OF_DAY},
     * {@code Calendar.MINUTE}, {@code Calendar.SECOND} and {@code Calendar.MILLISECOND}.
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
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseCalendar("2023-01-06 07:15:10.538", "yyyy-MM-dd HH:mm:ss.SSS", utc);
     * Dates.getFragmentInSeconds(cal, CalendarField.MINUTE);          // returns 10 (seconds within the current minute)
     * Dates.getFragmentInSeconds(cal, CalendarField.DAY_OF_MONTH);    // returns 26110 (7*3600 + 15*60 + 10)
     *
     * Dates.getFragmentInSeconds(cal, CalendarField.SECOND);             // returns 0 (fragment <= SECOND yields 0)
     * Dates.getFragmentInSeconds((Calendar) null, CalendarField.MINUTE); // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the calendar to work with, not {@code null}.
     * @param fragment the {@code CalendarField} fragment of {@code calendar} to calculate.
     * @return the number of seconds within the fragment of {@code calendar}.
     * @throws IllegalArgumentException if {@code calendar} is {@code null} or the fragment is not supported.
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
     * your fragment is {@code Calendar.MONTH}. The result will be all minutes of the
     * past day(s) and hour(s).</p>
     *
     * <p>Valid fragments are: {@code Calendar.YEAR}, {@code Calendar.MONTH}, both
     * {@code Calendar.DAY_OF_YEAR} and {@code Calendar.DATE}, {@code Calendar.HOUR_OF_DAY},
     * {@code Calendar.MINUTE}, {@code Calendar.SECOND} and {@code Calendar.MILLISECOND}.
     * A fragment less than or equal to a MINUTE field will return 0.</p>
     *
     * <ul>
     *  <li>January 1, 2008 7:15:10.538 with Calendar.HOUR_OF_DAY as fragment will return 15
     *   (equivalent to calendar.get(Calendar.MINUTE))</li>
     *  <li>January 6, 2008 7:15:10.538 with Calendar.HOUR_OF_DAY as fragment will return 15
     *   (equivalent to calendar.get(Calendar.MINUTE))</li>
     *  <li>January 1, 2008 7:15:10.538 with Calendar.MONTH as fragment will return 435 (7*60 + 15)</li>
     *  <li>January 6, 2008 7:15:10.538 with Calendar.MONTH as fragment will return 7635 (5*24*60 + 7*60 + 15)</li>
     *  <li>January 16, 2008 7:15:10.538 with Calendar.MILLISECOND as fragment will return 0
     *   (a millisecond cannot be split in minutes)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseCalendar("2023-01-06 07:15:10.538", "yyyy-MM-dd HH:mm:ss.SSS", utc);
     * Dates.getFragmentInMinutes(cal, CalendarField.HOUR_OF_DAY);     // returns 15 (minutes within the current hour)
     * Dates.getFragmentInMinutes(cal, CalendarField.MONTH);           // returns 7635 (5*1440 + 7*60 + 15)
     *
     * Dates.getFragmentInMinutes(cal, CalendarField.MINUTE);                  // returns 0 (fragment <= MINUTE yields 0)
     * Dates.getFragmentInMinutes((Calendar) null, CalendarField.HOUR_OF_DAY); // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the calendar to work with, not {@code null}.
     * @param fragment the {@code CalendarField} fragment of {@code calendar} to calculate.
     * @return the number of minutes within the fragment of {@code calendar}.
     * @throws IllegalArgumentException if {@code calendar} is {@code null} or the fragment is not supported.
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
     * your fragment is {@code Calendar.MONTH}. The result will be all hours of the
     * past day(s).</p>
     *
     * <p>Valid fragments are: {@code Calendar.YEAR}, {@code Calendar.MONTH}, both
     * {@code Calendar.DAY_OF_YEAR} and {@code Calendar.DATE}, {@code Calendar.HOUR_OF_DAY},
     * {@code Calendar.MINUTE}, {@code Calendar.SECOND} and {@code Calendar.MILLISECOND}.
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
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseCalendar("2023-01-06 07:15:10.538", "yyyy-MM-dd HH:mm:ss.SSS", utc);
     * Dates.getFragmentInHours(cal, CalendarField.DAY_OF_MONTH);      // returns 7 (hours within the current day)
     * Dates.getFragmentInHours(cal, CalendarField.MONTH);             // returns 127 (5 full days * 24 + 7)
     *
     * Dates.getFragmentInHours(cal, CalendarField.HOUR_OF_DAY);              // returns 0 (fragment <= HOUR yields 0)
     * Dates.getFragmentInHours((Calendar) null, CalendarField.DAY_OF_MONTH); // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the calendar to work with, not {@code null}.
     * @param fragment the {@code CalendarField} fragment of {@code calendar} to calculate.
     * @return the number of hours within the fragment of {@code calendar}.
     * @throws IllegalArgumentException if {@code calendar} is {@code null} or the fragment is not supported.
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
     * your fragment is {@code Calendar.YEAR}. The result will be all days of the
     * past month(s).</p>
     *
     * <p>Valid fragments are: {@code Calendar.YEAR}, {@code Calendar.MONTH}, both
     * {@code Calendar.DAY_OF_YEAR} and {@code Calendar.DATE}, {@code Calendar.HOUR_OF_DAY},
     * {@code Calendar.MINUTE}, {@code Calendar.SECOND} and {@code Calendar.MILLISECOND}.
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
     * TimeZone utc = TimeZone.getTimeZone("UTC");
     * Calendar cal = Dates.parseCalendar("2023-02-28 12:00:00", "yyyy-MM-dd HH:mm:ss", utc);
     * Dates.getFragmentInDays(cal, CalendarField.MONTH);             // returns 28 (day-of-month)
     * Dates.getFragmentInDays(cal, CalendarField.YEAR);              // returns 59 (31 in Jan + 28 = day-of-year)
     *
     * Dates.getFragmentInDays(cal, CalendarField.DAY_OF_MONTH);     // returns 0 (fragment <= DAY yields 0)
     * Dates.getFragmentInDays((Calendar) null, CalendarField.YEAR); // throws IllegalArgumentException
     * }</pre>
     *
     * @param calendar the calendar to work with, not {@code null}.
     * @param fragment the {@code CalendarField} fragment of {@code calendar} to calculate.
     * @return the number of days within the fragment of {@code calendar}.
     * @throws IllegalArgumentException if {@code calendar} is {@code null} or the fragment is not supported.
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
     * @param calendar the calendar to work with, not {@code null}.
     * @param fragment the {@code Calendar} field part of {@code calendar} to calculate.
     * @param unit the time unit.
     * @return the number of units within the fragment of {@code calendar}.
     * @throws IllegalArgumentException if {@code calendar} is {@code null} or the specified fragment is not supported.
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
     * java.util.Date d1 = Dates.parseJUDate("2023-03-28 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * java.util.Date d2 = Dates.parseJUDate("2023-03-28 06:01:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameDay(d1, d2);   // returns true (same day, time ignored)
     *
     * java.util.Date d3 = Dates.parseJUDate("2023-03-12 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameDay(d1, d3);                       // returns false (different day)
     * Dates.isSameDay((java.util.Date) null, d2);    // throws IllegalArgumentException
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
     * Calendar c1 = Dates.parseCalendar("2023-03-28 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * Calendar c2 = Dates.parseCalendar("2023-03-28 06:01:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameDay(c1, c2);   // returns true (same day, time ignored)
     *
     * Calendar c3 = Dates.parseCalendar("2023-03-12 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameDay(c1, c3);                   // returns false (different day)
     * Dates.isSameDay((Calendar) null, c2);      // throws IllegalArgumentException
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
     * java.util.Date d1 = Dates.parseJUDate("2023-03-15 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * java.util.Date d2 = Dates.parseJUDate("2023-03-28 06:01:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameMonth(d1, d2);   // returns true (same month and year)
     *
     * java.util.Date d3 = Dates.parseJUDate("2023-04-15 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameMonth(d1, d3);                     // returns false (April vs March)
     * Dates.isSameMonth((java.util.Date) null, d2);  // throws IllegalArgumentException
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
     * Calendar c1 = Dates.parseCalendar("2023-03-15 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * Calendar c2 = Dates.parseCalendar("2023-03-28 06:01:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameMonth(c1, c2);   // returns true (same month and year)
     *
     * Calendar c3 = Dates.parseCalendar("2023-04-15 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameMonth(c1, c3);                 // returns false (April vs March)
     * Dates.isSameMonth((Calendar) null, c2);    // throws IllegalArgumentException
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
     * java.util.Date d1 = Dates.parseJUDate("2023-03-15 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * java.util.Date d2 = Dates.parseJUDate("2023-11-28 06:01:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameYear(d1, d2);   // returns true (same year, month ignored)
     *
     * java.util.Date d3 = Dates.parseJUDate("2024-03-15 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameYear(d1, d3);                      // returns false (2024 vs 2023)
     * Dates.isSameYear((java.util.Date) null, d2);   // throws IllegalArgumentException
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
     * Calendar c1 = Dates.parseCalendar("2023-03-15 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * Calendar c2 = Dates.parseCalendar("2023-11-28 06:01:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameYear(c1, c2);   // returns true (same year, month ignored)
     *
     * Calendar c3 = Dates.parseCalendar("2024-03-15 13:45:00", "yyyy-MM-dd HH:mm:ss");
     * Dates.isSameYear(c1, c3);                  // returns false (2024 vs 2023)
     * Dates.isSameYear((Calendar) null, c2);     // throws IllegalArgumentException
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
     * Dates.isSameInstant(new java.util.Date(1000L), new java.util.Date(1000L));   // returns true (same millis)
     * Dates.isSameInstant(new java.util.Date(1000L), new java.util.Date(2000L));   // returns false (different millis)
     *
     * // a java.util.Date and a java.sql.Timestamp at the same instant are equal here
     * Dates.isSameInstant(new java.util.Date(1000L), new java.sql.Timestamp(1000L)); // returns true
     * Dates.isSameInstant((java.util.Date) null, new java.util.Date(1000L));         // throws IllegalArgumentException
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
     * Calendar c1 = Dates.createCalendar(1672585530123L);
     * Calendar c2 = Dates.createCalendar(1672585530123L);
     * Dates.isSameInstant(c1, c2);   // returns true (same millisecond instant)
     *
     * Calendar c3 = Dates.createCalendar(1672585530124L);
     * Dates.isSameInstant(c1, c3);                 // returns false (1 ms apart)
     * Dates.isSameInstant((Calendar) null, c2);    // throws IllegalArgumentException
     * }</pre>
     *
     * @param cal1 the first calendar, not altered, not null.
     * @param cal2 the second calendar, not altered, not null.
     * @return {@code true} if they represent the same millisecond instant.
     * @throws IllegalArgumentException if either calendar is {@code null}.
     * @see #isSameInstant(java.util.Date, java.util.Date)
     */
    public static boolean isSameInstant(final Calendar cal1, final Calendar cal2) throws IllegalArgumentException {
        N.checkArgNotNull(cal1, cs.calendar1);
        N.checkArgNotNull(cal2, cs.calendar2);

        return cal1.getTime().getTime() == cal2.getTime().getTime();
    }

    //-----------------------------------------------------------------------

    /**
     * Checks if two {@code java.util.Date} objects represent the same local time, comparing their
     * wall-clock fields in the default time zone.
     *
     * <p>The two dates are converted to {@link Calendar} instances in the default time zone and compared
     * field by field; this is the {@code java.util.Date} counterpart of
     * {@link #isSameLocalTime(Calendar, Calendar)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date d1 = new java.util.Date(1672585530123L);
     * java.util.Date d2 = new java.util.Date(1672585530123L);
     * Dates.isSameLocalTime(d1, d2);                     // returns true (same instant, same local fields)
     * Dates.isSameLocalTime((java.util.Date) null, d2);  // throws IllegalArgumentException
     * }</pre>
     *
     * @param date1 the first date, not altered, not {@code null}.
     * @param date2 the second date, not altered, not {@code null}.
     * @return {@code true} if they represent the same local time in the default time zone.
     * @throws IllegalArgumentException if either date is {@code null}.
     * @see #isSameLocalTime(Calendar, Calendar)
     */
    public static boolean isSameLocalTime(final java.util.Date date1, final java.util.Date date2) throws IllegalArgumentException {
        N.checkArgNotNull(date1, cs.date1);
        N.checkArgNotNull(date2, cs.date2);

        final Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);

        final Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);

        return isSameLocalTime(cal1, cal2);
    }

    //-----------------------------------------------------------------------

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     * <br />
     *
     * <p>Checks if two calendar objects represent the same local time.</p>
     *
     * <p>This method compares the values of the fields of the two objects.
     * In addition, both calendars must be of the same type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar c1 = Dates.createCalendar(1672585530123L);
     * Calendar c2 = Dates.createCalendar(1672585530123L);
     * Dates.isSameLocalTime(c1, c2);   // returns true (identical fields and type)
     *
     * Calendar c3 = Dates.createCalendar(1672585531123L);   // returns 1 second later
     * Dates.isSameLocalTime(c1, c3);                        // returns false (different second field)
     * Dates.isSameLocalTime((Calendar) null, c2);           // throws IllegalArgumentException
     * }</pre>
     *
     * @param cal1 the first calendar, not altered, not null.
     * @param cal2 the second calendar, not altered, not null.
     * @return {@code true} if they represent the same local time.
     * @throws IllegalArgumentException if either calendar is {@code null}.
     * @see #isSameLocalTime(java.util.Date, java.util.Date)
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

        if (UTC_TIME_ZONE.equals(timeZone)) {
            //noinspection ConditionCoveredByFurtherCondition
            if ((format.length() == 28) && format.equals(ISO_8601_TIMESTAMP_FORMAT)) {
                sdf = utcTimestampDFPool.poll();

                if (sdf == null) {
                    sdf = new SimpleDateFormat(format, Locale.US);
                    sdf.setTimeZone(timeZone);
                }

                return sdf;
            } else //noinspection ConditionCoveredByFurtherCondition
            if ((format.length() == 24) && format.equals(ISO_8601_DATE_TIME_FORMAT)) {
                sdf = utcDateTimeDFPool.poll();

                if (sdf == null) {
                    sdf = new SimpleDateFormat(format, Locale.US);
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
            // Locale.US so cached SimpleDateFormat instances produce stable, RFC-conformant output
            // for patterns containing EEE / MMM / a (AM/PM). The default-locale variant gave
            // localized day/month names that broke HTTP Date headers etc. on non-English JVMs.
            sdf = new SimpleDateFormat(format, Locale.US);
        }

        sdf.setTimeZone(timeZone);

        return sdf;
    }

    private static void recycleSDF(final String format, final TimeZone timeZone, final DateFormat sdf) {
        if (UTC_TIME_ZONE.equals(timeZone)) {
            //noinspection ConditionCoveredByFurtherCondition
            if ((format.length() == 28) && format.equals(ISO_8601_TIMESTAMP_FORMAT)) {
                utcTimestampDFPool.offer(sdf);
            } else //noinspection ConditionCoveredByFurtherCondition
            if ((format.length() == 24) && format.equals(ISO_8601_DATE_TIME_FORMAT)) {
                utcDateTimeDFPool.offer(sdf);
            } else {
                dfPool.get(format).offer(sdf);
            }
        } else {
            dfPool.get(format).offer(sdf);
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

                    case 23:
                        if (str.charAt(10) == 'T') {
                            return ISO_8601_TIMESTAMP_FORMAT_2;
                        } else {
                            return LOCAL_TIMESTAMP_FORMAT;
                        }

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
            } else if (len == 29 || (len >= 4 && str.charAt(3) == ',')) {
                return RFC_1123_DATE_TIME_FORMAT;
            }
        }

        return format;
    }

    private static TimeZone checkTimeZone(final String dateTime, final String format, final TimeZone timeZone) {
        if ((Strings.isNotEmpty(dateTime) && dateTime.endsWith("Z")) || (Strings.isNotEmpty(format) && format.endsWith("'Z'"))) {
            // The UTC designator 'Z' fixes the zone to UTC. Reject a conflicting non-UTC zone rather than
            // silently overriding it, so the format side matches the parse side (both throw IAE on the
            // same caller mistake). A null or zero-offset (UTC-equivalent) zone is accepted and resolved to UTC.
            if (timeZone != null && !timeZone.hasSameRules(UTC_TIME_ZONE)) {
                throw new IllegalArgumentException("A pattern or value ending with the UTC designator 'Z' requires a UTC-equivalent time zone; format: "
                        + format + ", time zone: " + timeZone);
            }

            return UTC_TIME_ZONE;
        }

        return timeZone == null ? DEFAULT_TIME_ZONE : timeZone;
    }

    private static long fastDateParse(final String str, final String format, final TimeZone timeZone) {
        final int len = str.length();

        if (!((len == 19) || (len == 20) || (len == 24)) || !(ISO_8601_TIMESTAMP_FORMAT.equals(format) || ISO_8601_DATE_TIME_FORMAT.equals(format)
                || ISO_LOCAL_DATE_TIME_FORMAT.equals(format) || LOCAL_DATE_TIME_FORMAT.equals(format))) {
            return Long.MIN_VALUE;
        }

        if (!hasExpectedFastDateParseSeparators(str, format, len)) {
            return Long.MIN_VALUE;
        }

        final int year = parseInt(str, 0, 4);
        final int month = parseInt(str, 5, 7) - 1;
        final int date = parseInt(str, 8, 10);
        final int hourOfDay = parseInt(str, 11, 13);
        final int minute = parseInt(str, 14, 16);
        final int second = parseInt(str, 17, 19);
        final int milliSecond = len == 24 ? parseInt(str, 20, 23) : 0;

        if (year < 0 || month < 0 || date < 0 || hourOfDay < 0 || minute < 0 || second < 0 || milliSecond < 0) {
            return Long.MIN_VALUE; // malformed input, fall back to SimpleDateFormat
        }

        Calendar c = null;
        Queue<Calendar> timeZoneCalendarQueue = null;

        if (UTC_TIME_ZONE.equals(timeZone)) {
            c = utcCalendarPool.poll();
        } else {
            timeZoneCalendarQueue = calendarPool.computeIfAbsent(timeZone, tz -> new ArrayBlockingQueue<>(POOL_SIZE));
            c = timeZoneCalendarQueue.poll();
        }

        if (c == null) {
            c = Calendar.getInstance(timeZone);
        }

        c.clear();
        //noinspection MagicConstant
        c.set(year, month, date, hourOfDay, minute, second);
        c.set(Calendar.MILLISECOND, milliSecond);

        final long timeInMillis = c.getTimeInMillis();

        if (UTC_TIME_ZONE.equals(timeZone)) {
            utcCalendarPool.offer(c);
        } else {
            timeZoneCalendarQueue.offer(c);
        }

        return timeInMillis;
    }

    private static boolean hasExpectedFastDateParseSeparators(final String str, final String format, final int len) {
        if (LOCAL_DATE_TIME_FORMAT.equals(format)) {
            return len == 19 && str.charAt(4) == '-' && str.charAt(7) == '-' && str.charAt(10) == ' ' && str.charAt(13) == ':' && str.charAt(16) == ':';
        } else if (ISO_LOCAL_DATE_TIME_FORMAT.equals(format)) {
            return len == 19 && str.charAt(4) == '-' && str.charAt(7) == '-' && str.charAt(10) == 'T' && str.charAt(13) == ':' && str.charAt(16) == ':';
        } else if (ISO_8601_DATE_TIME_FORMAT.equals(format)) {
            return len == 20 && str.charAt(4) == '-' && str.charAt(7) == '-' && str.charAt(10) == 'T' && str.charAt(13) == ':' && str.charAt(16) == ':'
                    && str.charAt(19) == 'Z';
        } else if (ISO_8601_TIMESTAMP_FORMAT.equals(format)) {
            return len == 24 && str.charAt(4) == '-' && str.charAt(7) == '-' && str.charAt(10) == 'T' && str.charAt(13) == ':' && str.charAt(16) == ':'
                    && str.charAt(19) == '.' && str.charAt(23) == 'Z';
        }

        return false;
    }

    private static int parseInt(final String str, int fromIndex, final int toIndex) {
        int result = 0;

        while (fromIndex < toIndex) {
            final char ch = str.charAt(fromIndex++);

            if (ch < '0' || ch > '9') {
                return -1;
            }

            result = (result * 10) + (ch - 48);
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
        final LongObjFunction<? super Calendar, ? extends java.util.Calendar> creator = calendarCreatorPool.get(cls);

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
     * Dates.isLastDayOfMonth(Dates.parseDate("2023-01-31"));   // returns true (Jan has 31 days)
     * Dates.isLastDayOfMonth(Dates.parseDate("2024-02-29"));   // returns true (Feb 29 in a leap year)
     *
     * Dates.isLastDayOfMonth(Dates.parseDate("2023-01-15"));   // returns false (mid-month)
     * Dates.isLastDayOfMonth((java.util.Date) null);           // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date to check. Must not be {@code null}.
     * @return {@code true} if the provided date is the last date of its month.
     * @throws IllegalArgumentException if the date is {@code null}.
     */
    public static boolean isLastDayOfMonth(final java.util.Date date) throws IllegalArgumentException {
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
     * Dates.isLastDayOfYear(Dates.parseDate("2023-12-31"));   // returns true (Dec 31)
     * Dates.isLastDayOfYear(Dates.parseDate("2024-12-31"));   // returns true (Dec 31 of a leap year)
     *
     * Dates.isLastDayOfYear(Dates.parseDate("2023-12-15"));   // returns false (mid-December)
     * Dates.isLastDayOfYear((java.util.Date) null);           // throws IllegalArgumentException
     * }</pre>
     *
     * @param date the date to check. Must not be {@code null}.
     * @return {@code true} if the provided date is the last date of its year.
     * @throws IllegalArgumentException if the date is {@code null}.
     */
    public static boolean isLastDayOfYear(final java.util.Date date) throws IllegalArgumentException {
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
     * Dates.getLastDayOfMonth(Dates.parseDate("2023-01-15"));   // returns 31 (January)
     * Dates.getLastDayOfMonth(Dates.parseDate("2023-02-15"));   // returns 28 (February, non-leap year)
     *
     * Dates.getLastDayOfMonth(Dates.parseDate("2024-02-15"));   // returns 29 (February, leap year)
     * Dates.getLastDayOfMonth((java.util.Date) null);           // throws IllegalArgumentException
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
     * Dates.getLastDayOfYear(Dates.parseDate("2023-01-15"));   // returns 365 (non-leap year)
     * Dates.getLastDayOfYear(Dates.parseDate("2024-02-15"));   // returns 366 (leap year)
     *
     * Dates.getLastDayOfYear(Dates.parseDate("2023-12-31"));   // returns 365 (any date in the year)
     * Dates.getLastDayOfYear((java.util.Date) null);           // throws IllegalArgumentException
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
     * java.util.Date d1 = new java.util.Date(1000L), d5 = new java.util.Date(5000L);
     * java.util.Date d10 = new java.util.Date(10000L), d15 = new java.util.Date(15000L);
     * java.util.Date d20 = new java.util.Date(20000L);
     *
     * Dates.isOverlapping(d1, d10, d5, d15);    // returns true (overlap between 5000 and 10000)
     * Dates.isOverlapping(d1, d10, d15, d20);   // returns false (disjoint ranges)
     * Dates.isOverlapping(d1, d10, d10, d20);   // returns false (adjacent; endpoints are exclusive)
     *
     * Dates.isOverlapping(null, d10, d5, d15);  // throws IllegalArgumentException (null argument)
     * }</pre>
     *
     * @param startDate1 start of the first range, not {@code null}.
     * @param endDate1 end of the first range, not {@code null}.
     * @param startDate2 start of the second range, not {@code null}.
     * @param endDate2 end of the second range, not {@code null}.
     * @return {@code true} if the two date ranges overlap (exclusive of the endpoints).
     * @throws IllegalArgumentException if any argument is {@code null}, or if a start date is after its corresponding end date.
     * @see #isBetween(java.util.Date, java.util.Date, java.util.Date)
     */
    public static boolean isOverlapping(final java.util.Date startDate1, final java.util.Date endDate1, final java.util.Date startDate2,
            final java.util.Date endDate2) {
        N.checkArgNotNull(startDate1, cs.startDate1);
        N.checkArgNotNull(endDate1, cs.endDate1);
        N.checkArgNotNull(startDate2, cs.startDate2);
        N.checkArgNotNull(endDate2, cs.endDate2);

        if (startDate1.after(endDate1) || startDate2.after(endDate2)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }

        return startDate1.before(endDate2) && startDate2.before(endDate1);
    }

    /**
     * Checks if two calendar ranges overlap (exclusive of the endpoints). This is the {@link Calendar}
     * counterpart of {@link #isOverlapping(java.util.Date, java.util.Date, java.util.Date, java.util.Date)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar c1 = Dates.createCalendar(1000L), c5 = Dates.createCalendar(5000L);
     * Calendar c10 = Dates.createCalendar(10000L), c15 = Dates.createCalendar(15000L);
     * Dates.isOverlapping(c1, c10, c5, c15);    // returns true (overlap between 5000 and 10000)
     * Dates.isOverlapping(c1, c10, c10, c15);   // returns false (adjacent; endpoints are exclusive)
     * }</pre>
     *
     * @param startDate1 start of the first range, not {@code null}.
     * @param endDate1 end of the first range, not {@code null}.
     * @param startDate2 start of the second range, not {@code null}.
     * @param endDate2 end of the second range, not {@code null}.
     * @return {@code true} if the two calendar ranges overlap (exclusive of the endpoints).
     * @throws IllegalArgumentException if any argument is {@code null}, or if a start is after its corresponding end.
     * @see #isOverlapping(java.util.Date, java.util.Date, java.util.Date, java.util.Date)
     */
    public static boolean isOverlapping(final Calendar startDate1, final Calendar endDate1, final Calendar startDate2, final Calendar endDate2) {
        N.checkArgNotNull(startDate1, cs.startDate1);
        N.checkArgNotNull(endDate1, cs.endDate1);
        N.checkArgNotNull(startDate2, cs.startDate2);
        N.checkArgNotNull(endDate2, cs.endDate2);

        if (startDate1.after(endDate1) || startDate2.after(endDate2)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }

        return startDate1.before(endDate2) && startDate2.before(endDate1);
    }

    /**
     * Checks if the given date is between the specified start date and end date, inclusive.
     * It means {@code startDate <= date <= endDate}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.util.Date start = new java.util.Date(1000L), end = new java.util.Date(3000L);
     *
     * Dates.isBetween(new java.util.Date(2000L), start, end);   // returns true (within the range)
     * Dates.isBetween(start, start, end);                       // returns true (start boundary is inclusive)
     * Dates.isBetween(end, start, end);                         // returns true (end boundary is inclusive)
     *
     * Dates.isBetween(new java.util.Date(4000L), start, end);   // returns false (after the end)
     * Dates.isBetween(null, start, end);                        // throws IllegalArgumentException (null argument)
     * }</pre>
     *
     * @param date the date to check. Must not be {@code null}.
     * @param startDate the start of the range (inclusive). Must not be {@code null}.
     * @param endDate the end of the range (inclusive). Must not be {@code null}.
     * @return {@code true} if the date is within the specified range (inclusive).
     * @throws IllegalArgumentException if any argument is {@code null}, or if {@code startDate} is after {@code endDate}.
     * @see N#geAndLe(Comparable, Comparable, Comparable)
     * @see N#gtAndLt(Comparable, Comparable, Comparable)
     */
    @Beta
    public static boolean isBetween(final java.util.Date date, final java.util.Date startDate, final java.util.Date endDate) {
        N.checkArgNotNull(date, cs.date);
        N.checkArgNotNull(startDate, cs.startDate);
        N.checkArgNotNull(endDate, cs.endDate);

        if (startDate.after(endDate)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }

        return N.geAndLe(date, startDate, endDate);
    }

    /**
     * Checks if the given calendar is between the specified start and end calendars, inclusive.
     * It means {@code startDate <= date <= endDate}. This is the {@link Calendar} counterpart of
     * {@link #isBetween(java.util.Date, java.util.Date, java.util.Date)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar start = Dates.createCalendar(1000L), end = Dates.createCalendar(3000L);
     * Dates.isBetween(Dates.createCalendar(2000L), start, end);   // returns true (within the range)
     * Dates.isBetween(start, start, end);                         // returns true (start boundary is inclusive)
     * Dates.isBetween(Dates.createCalendar(4000L), start, end);   // returns false (after the end)
     * }</pre>
     *
     * @param date the calendar to check. Must not be {@code null}.
     * @param startDate the start of the range (inclusive). Must not be {@code null}.
     * @param endDate the end of the range (inclusive). Must not be {@code null}.
     * @return {@code true} if the calendar is within the specified range (inclusive).
     * @throws IllegalArgumentException if any argument is {@code null}, or if {@code startDate} is after {@code endDate}.
     * @see #isBetween(java.util.Date, java.util.Date, java.util.Date)
     */
    @Beta
    public static boolean isBetween(final Calendar date, final Calendar startDate, final Calendar endDate) {
        N.checkArgNotNull(date, cs.date);
        N.checkArgNotNull(startDate, cs.startDate);
        N.checkArgNotNull(endDate, cs.endDate);

        if (startDate.after(endDate)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }

        return N.geAndLe(date, startDate, endDate);
    }

    static void formatToForNull(final Appendable appendable) {
        N.checkArgNotNull(appendable, cs.appendable);

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
     *   <li><b>Numeric Input:</b> Every {@code parseTo*} method (both the modern {@code java.time} and the
     *       legacy {@code java.util}/{@code java.sql} variants) interprets a purely numeric value as epoch
     *       milliseconds</li>
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
     *   <li><b>Input Handling:</b> Parse methods return {@code null} for null or empty input and throw for invalid non-empty input</li>
     *   <li><b>Predefined Formatters:</b> Public constants for common patterns, plus {@link DTF#of(String)}
     *       for custom patterns</li>
     * </ul>
     *
     * <p><b>Common Usage Patterns:</b>
     * <pre>{@code
     * // Parsing different temporal types (each parser is invoked on a DTF instance)
     * ZonedDateTime zdt = DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime("2023-12-25T15:30:45-05:00[America/New_York]");
     * OffsetDateTime odt = DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime("2023-12-25T15:30:45+05:30");
     * LocalDateTime ldt = DTF.ISO_LOCAL_DATE_TIME.parseToLocalDateTime("2023-12-25T15:30:45");
     * LocalDate ld = DTF.LOCAL_DATE.parseToLocalDate("2023-12-25");
     * LocalTime lt = DTF.LOCAL_TIME.parseToLocalTime("15:30:45");
     * Instant instant = DTF.ISO_8601_DATE_TIME.parseToInstant("2023-12-25T20:30:45Z");
     *
     * // Formatting temporal objects
     * String formatted = DTF.ISO_ZONED_DATE_TIME.format(ZonedDateTime.now());
     * String localFormatted = DTF.LOCAL_DATE_TIME.format(LocalDateTime.now());
     *
     * // Custom patterns
     * DTF custom = DTF.of("MM/dd/yyyy HH:mm");
     * String customFormatted = custom.format(LocalDateTime.now());
     * }</pre>
     *
     * <p><b>Relationship to Enclosing Class:</b>
     * This class complements {@link Dates} by providing modern Java Time API support,
     * while the enclosing class focuses on legacy date/time types. Use this class for:
     * <ul>
     *   <li>New code requiring Java 8+ time types</li>
     *   <li>Applications needing strong type safety</li>
     *   <li>Systems requiring immutable temporal objects</li>
     *   <li>APIs exposing time-zone aware operations</li>
     * </ul>
     *
     * <p><b>Thread Safety:</b>
     * {@code DTF} instances are immutable and thread-safe (the backing {@link DateTimeFormatter} is
     * immutable), so a single instance can be safely shared across threads without synchronization. The
     * {@code formatTo} methods write into a caller-supplied {@link Appendable}, whose own thread-safety
     * is the caller's responsibility.
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
         * Date/Time format: {@code yyyy-MM-dd}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * LocalDate date = LocalDate.of(2023, 12, 25);
         * // Result: "2023-12-25"
         * String formatted = DTF.LOCAL_DATE.format(date);
         *
         * String dateStr = "2023-12-25";
         * // Result: LocalDate representing December 25, 2023
         * LocalDate parsed = DTF.LOCAL_DATE.parseToLocalDate(dateStr);
         *
         * java.util.Date javaDate = new java.util.Date();
         * String dateFormatted = DTF.LOCAL_DATE.format(javaDate);
         * }</pre>
         *
         * @see Dates#LOCAL_DATE_FORMAT
         */
        public static final DTF LOCAL_DATE = new DTF(Dates.LOCAL_DATE_FORMAT);

        /**
         * Date/Time format: {@code HH:mm:ss}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * LocalTime time = LocalTime.of(15, 30, 45);
         * // Result: "15:30:45"
         * String formatted = DTF.LOCAL_TIME.format(time);
         *
         * String timeStr = "14:25:30";
         * // Result: LocalTime representing 14:25:30
         * LocalTime parsed = DTF.LOCAL_TIME.parseToLocalTime(timeStr);
         *
         * java.util.Date javaDate = new java.util.Date();
         * String timeFormatted = DTF.LOCAL_TIME.format(javaDate);
         * }</pre>
         *
         * @see Dates#LOCAL_TIME_FORMAT
         */
        public static final DTF LOCAL_TIME = new DTF(Dates.LOCAL_TIME_FORMAT);

        /**
         * Date/Time format: {@code yyyy-MM-dd HH:mm:ss}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * LocalDateTime dateTime = LocalDateTime.of(2023, 12, 25, 15, 30, 45);
         * // Result: "2023-12-25 15:30:45"
         * String formatted = DTF.LOCAL_DATE_TIME.format(dateTime);
         *
         * String dateTimeStr = "2023-12-25 14:25:30";
         * // Result: LocalDateTime representing December 25, 2023 at 14:25:30
         * LocalDateTime parsed = DTF.LOCAL_DATE_TIME.parseToLocalDateTime(dateTimeStr);
         *
         * java.util.Calendar calendar = java.util.Calendar.getInstance();
         * String calendarFormatted = DTF.LOCAL_DATE_TIME.format(calendar);
         * }</pre>
         *
         * @see Dates#LOCAL_DATE_TIME_FORMAT
         */
        public static final DTF LOCAL_DATE_TIME = new DTF(Dates.LOCAL_DATE_TIME_FORMAT);

        /**
         * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ss}.
         *
         * <p>ISO 8601 format without timezone information. Useful for standard date-time representation
         * in APIs and data exchange where local time (without offset) is sufficient.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * LocalDateTime dateTime = LocalDateTime.of(2023, 12, 25, 15, 30, 45);
         * // Result: "2023-12-25T15:30:45"
         * String formatted = DTF.ISO_LOCAL_DATE_TIME.format(dateTime);
         *
         * String isoStr = "2023-12-25T14:25:30";
         * LocalDateTime parsed = DTF.ISO_LOCAL_DATE_TIME.parseToLocalDateTime(isoStr);
         * // Result: LocalDateTime representing December 25, 2023 at 14:25:30
         * }</pre>
         *
         * @see Dates#ISO_LOCAL_DATE_TIME_FORMAT
         */
        public static final DTF ISO_LOCAL_DATE_TIME = new DTF(Dates.ISO_LOCAL_DATE_TIME_FORMAT);

        /**
         * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ssXXX}.
         *
         * <p>ISO 8601 format with UTC offset. Useful for representing moment in time with offset
         * information, but without timezone ID. Format includes offset like +05:30, -08:00, or Z for UTC.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * OffsetDateTime offsetDT = OffsetDateTime.of(
         *     2023, 12, 25, 15, 30, 45, 0,
         *     java.time.ZoneOffset.of("+05:30"));
         * // Result: "2023-12-25T15:30:45+05:30"
         * String formatted = DTF.ISO_OFFSET_DATE_TIME.format(offsetDT);
         *
         * String offsetStr = "2023-12-25T14:25:30-08:00";
         * OffsetDateTime parsed = DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime(offsetStr);
         * }</pre>
         *
         * @see Dates#ISO_OFFSET_DATE_TIME_FORMAT
         */
        public static final DTF ISO_OFFSET_DATE_TIME = new DTF(Dates.ISO_OFFSET_DATE_TIME_FORMAT);

        /**
         * Date/Time format: {@code yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'}.
         *
         * <p>ISO 8601 format with timezone ID. Provides complete information including offset and
         * timezone identifier. Most comprehensive format for full datetime representation.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * ZonedDateTime zonedDT = ZonedDateTime.of(
         *     2023, 12, 25, 15, 30, 45, 0,
         *     java.time.ZoneId.of("Asia/Kolkata"));
         * // Result: "2023-12-25T15:30:45+05:30[Asia/Kolkata]"
         * String formatted = DTF.ISO_ZONED_DATE_TIME.format(zonedDT);
         *
         * String zonedStr = "2023-12-25T14:25:30-08:00[America/Los_Angeles]";
         * ZonedDateTime parsed = DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime(zonedStr);
         * }</pre>
         *
         * @see Dates#ISO_ZONED_DATE_TIME_FORMAT
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
         * String utcStr = "2023-12-25T14:25:30Z";
         * Instant parsed = DTF.ISO_8601_DATE_TIME.parseToInstant(utcStr);
         *
         * ZonedDateTime utcTime = ZonedDateTime.of(
         *     2023, 12, 25, 15, 30, 45, 0,
         *     java.time.ZoneId.of("UTC"));
         * // Result: "2023-12-25T15:30:45Z"
         * String formatted = DTF.ISO_8601_DATE_TIME.format(utcTime);
         * }</pre>
         *
         * @see Dates#ISO_8601_DATE_TIME_FORMAT
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
         * String timestampStr = "2023-12-25T14:25:30.456Z";
         * Instant parsed = DTF.ISO_8601_TIMESTAMP.parseToInstant(timestampStr);
         *
         * long currentTimeMillis = System.currentTimeMillis();
         * java.util.Date date = new java.util.Date(currentTimeMillis);
         * // Result like: "2023-12-25T15:30:45.123Z"
         * String formatted = DTF.ISO_8601_TIMESTAMP.format(date);
         * }</pre>
         *
         * @see Dates#ISO_8601_TIMESTAMP_FORMAT
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
         * ZonedDateTime zonedDT = ZonedDateTime.now(java.time.ZoneId.of("GMT"));
         * // Result: "Mon, 25 Dec 2023 15:30:45 GMT"
         * String formatted = DTF.RFC_1123_DATE_TIME.format(zonedDT);
         *
         * String rfcStr = "Mon, 25 Dec 2023 14:25:30 GMT";
         * ZonedDateTime parsed = DTF.RFC_1123_DATE_TIME.parseToZonedDateTime(rfcStr);
         *
         * java.util.Calendar calendar = java.util.Calendar.getInstance();
         * String httpHeader = DTF.RFC_1123_DATE_TIME.format(calendar);
         * // Can be used directly in HTTP headers like Date or Expires
         * }</pre>
         *
         * @see Dates#RFC_1123_DATE_TIME_FORMAT
         */
        public static final DTF RFC_1123_DATE_TIME = new DTF(Dates.RFC_1123_DATE_TIME_FORMAT);

        private final String format;
        private final boolean utcZFormat; // patterns with the literal 'Z' suffix: values are UTC instants
        private final DateTimeFormatter dateTimeFormatter;

        DTF(final String format) {
            this.format = format;
            utcZFormat = ISO_8601_DATE_TIME_FORMAT.equals(format) || ISO_8601_TIMESTAMP_FORMAT.equals(format);

            // Locale.US for stable EEE/MMM/a formatting; see Dates.getSDF for the same rationale.
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format, Locale.US);

            // The 'Z' in these patterns is a quoted literal meaning UTC: instant-bearing temporals
            // (Zoned/Offset/Instant) must be converted to UTC when formatting, not stamped with 'Z'
            // on their local wall-clock fields. Local temporals are unaffected.
            if (utcZFormat) {
                dtf = dtf.withZone(UTC_TIME_ZONE.toZoneId());
            }

            dateTimeFormatter = dtf;
        }

        //    DTF(final DateTimeFormatter dtf) {

        /**
         * Creates a {@code DTF} formatter for the specified date/time pattern.
         *
         * <p>The pattern syntax is that of {@link DateTimeFormatter#ofPattern(String)}, and the returned
         * instance formats/parses with {@code Locale.US} for stable {@code EEE}/{@code MMM}/{@code a} text.
         * Instances are immutable and thread-safe, so formatters for frequently-used patterns should be
         * created once and reused.</p>
         *
         * <p>Note: the {@code format(java.util.Date)} and {@code format(Calendar)} overloads generally
         * format through the legacy {@link Dates#format(java.util.Date, String)} path, so a pattern used
         * with those overloads must also be a valid {@code SimpleDateFormat} pattern. The predefined
         * {@link #ISO_ZONED_DATE_TIME} formatter is handled by the {@code java.time} path because its
         * {@code VV} zone-id pattern is {@link DateTimeFormatter}-only.</p>
         *
         * <p>Note: the automatic UTC conversion applied by {@link #ISO_8601_DATE_TIME} and
         * {@link #ISO_8601_TIMESTAMP} (whose quoted {@code 'Z'} literal denotes UTC) is enabled only when
         * the specified pattern is exactly one of those two patterns. Any other pattern containing a
         * quoted {@code 'Z'} literal formats instant-bearing values with their local wall-clock fields
         * and appends the literal {@code Z} unchanged.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF dtf = DTF.of("MM/dd/yyyy HH:mm");
         * String formatted = dtf.format(LocalDateTime.of(2023, 12, 25, 15, 30));   // returns "12/25/2023 15:30"
         * LocalDateTime parsed = dtf.parseToLocalDateTime("12/25/2023 15:30");
         * }</pre>
         *
         * @param pattern the date/time pattern as defined by {@link DateTimeFormatter#ofPattern(String)}
         * @return a {@code DTF} instance for the specified pattern
         * @throws IllegalArgumentException if {@code pattern} is {@code null} or empty, or is not a valid date/time pattern
         * @see DateTimeFormatter#ofPattern(String)
         */
        public static DTF of(final String pattern) throws IllegalArgumentException {
            N.checkArgNotEmpty(pattern, cs.pattern);

            return new DTF(pattern);
        }

        private static void appendFormatted(final Appendable appendable, final String str) {
            N.checkArgNotNull(appendable, cs.appendable);

            try {
                appendable.append(str);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /**
         * Formats the provided {@code java.util.Date} instance into a string representation using this formatter's pattern.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.ISO_8601_DATE_TIME.format(new java.util.Date(0L));             // returns "1970-01-01T00:00:00Z" (UTC 'Z' format)
         * DTF.ISO_8601_DATE_TIME.format(new java.util.Date(1736937045000L)); // returns "2025-01-15T10:30:45Z"
         *
         * DTF.ISO_8601_DATE_TIME.format((java.util.Date) null);    // returns null
         * }</pre>
         *
         * @param date the {@code java.util.Date} instance to format; may be {@code null}.
         * @return a string representation of the provided date, or {@code null} if {@code date} is {@code null}.
         * @throws IllegalArgumentException if an error occurs during formatting.
         * @see Dates#format(java.util.Date, String)
         */
        @MayReturnNull
        public String format(final java.util.Date date) {
            if (date == null) {
                return null;
            }

            if (this.format.equals(ISO_ZONED_DATE_TIME_FORMAT)) {
                // The zoned pattern (with 'VV') is DateTimeFormatter-only; SimpleDateFormat cannot render it.
                // Render the instant in the live default zone via the java.time path instead of the legacy path.
                return dateTimeFormatter.format(ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
            }

            return Dates.format(date, format);
        }

        /**
         * Formats the provided java.util.Calendar instance into a string representation.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Calendar cal = Dates.createCalendar(0L);
         * DTF.ISO_8601_DATE_TIME.format(cal);                                  // returns "1970-01-01T00:00:00Z"
         * DTF.ISO_8601_DATE_TIME.format(Dates.createCalendar(1736937045000L)); // returns "2025-01-15T10:30:45Z"
         *
         * DTF.LOCAL_DATE_TIME.format((java.util.Calendar) null);   // returns null
         * }</pre>
         *
         * @param calendar the {@code java.util.Calendar} instance to format; may be {@code null}.
         * @return a string representation of the provided calendar, or {@code null} if {@code calendar} is {@code null}.
         * @throws IllegalArgumentException if an error occurs during formatting.
         * @see Dates#format(java.util.Calendar, String)
         */
        @MayReturnNull
        public String format(final java.util.Calendar calendar) {
            if (calendar == null) {
                return null;
            }

            if (this.format.equals(ISO_ZONED_DATE_TIME_FORMAT)) {
                // The zoned pattern is DateTimeFormatter-only; render via java.time in the calendar's own zone.
                return dateTimeFormatter.format(ZonedDateTime.ofInstant(calendar.toInstant(), calendar.getTimeZone().toZoneId()));
            }

            return Dates.format(calendar, format);
        }

        /**
         * Formats the provided TemporalAccessor instance into a string representation.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.LOCAL_DATE_TIME.format(LocalDateTime.of(2023, 12, 25, 14, 30, 45));   // returns "2023-12-25 14:30:45"
         * DTF.LOCAL_DATE.format(LocalDate.of(2023, 12, 25));                        // returns "2023-12-25"
         *
         * DTF.LOCAL_DATE_TIME.format((TemporalAccessor) null);   // returns null
         *
         * // Note: the temporal must supply every field the pattern needs; e.g. a bare Instant cannot be
         * // formatted with a yyyy-MM-dd pattern and throws DateTimeException.
         * }</pre>
         *
         * @param temporal the {@code TemporalAccessor} instance to format; may be {@code null}.
         * @return a string representation of the provided temporal, or {@code null} if {@code temporal} is {@code null}.
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
         * StringBuilder sb = new StringBuilder();
         * DTF.ISO_8601_DATE_TIME.formatTo(new java.util.Date(0L), sb);
         * sb.toString();                                  // returns "1970-01-01T00:00:00Z"
         *
         * StringBuilder nb = new StringBuilder();
         * DTF.ISO_8601_DATE_TIME.formatTo((java.util.Date) null, nb);
         * nb.toString();                                  // returns "null" (literal appended for null input)
         * }</pre>
         *
         * @param date the {@code java.util.Date} instance to format; if {@code null}, the string {@code "null"} is appended.
         * @param appendable the Appendable to which the formatted string will be appended; must not be {@code null}.
         * @throws IllegalArgumentException if an error occurs during formatting.
         * @throws UncheckedIOException if an I/O error occurs while appending.
         * @see Dates#formatTo(java.util.Date, String, Appendable)
         */
        public void formatTo(final java.util.Date date, final Appendable appendable) {
            N.checkArgNotNull(appendable, cs.appendable);

            if (date == null) {
                formatToForNull(appendable);
                return;
            }

            if (this.format.equals(ISO_ZONED_DATE_TIME_FORMAT)) {
                appendFormatted(appendable, format(date));
                return;
            }

            Dates.formatTo(date, format, appendable);
        }

        /**
         * Formats the provided java.util.Calendar instance into a string representation and appends it to the provided Appendable.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * StringBuilder sb = new StringBuilder();
         * DTF.ISO_8601_DATE_TIME.formatTo(Dates.createCalendar(0L), sb);
         * sb.toString();                                  // returns "1970-01-01T00:00:00Z"
         *
         * StringBuilder nb = new StringBuilder();
         * DTF.ISO_8601_DATE_TIME.formatTo((java.util.Calendar) null, nb);
         * nb.toString();                                  // returns "null" (literal appended for null input)
         * }</pre>
         *
         * @param calendar the {@code java.util.Calendar} instance to format; if {@code null}, the string {@code "null"} is appended.
         * @param appendable the Appendable to which the formatted string will be appended; must not be {@code null}.
         * @throws IllegalArgumentException if an error occurs during formatting.
         * @throws UncheckedIOException if an I/O error occurs while appending.
         * @see Dates#formatTo(java.util.Calendar, String, Appendable)
         */
        public void formatTo(final java.util.Calendar calendar, final Appendable appendable) {
            N.checkArgNotNull(appendable, cs.appendable);

            if (calendar == null) {
                formatToForNull(appendable);
                return;
            }

            if (this.format.equals(ISO_ZONED_DATE_TIME_FORMAT)) {
                appendFormatted(appendable, format(calendar));
                return;
            }

            Dates.formatTo(calendar, format, appendable);
        }

        /**
         * Formats the provided TemporalAccessor instance into a string representation and appends it to the provided Appendable.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * StringBuilder sb = new StringBuilder();
         * DTF.LOCAL_DATE_TIME.formatTo(LocalDateTime.of(2023, 12, 25, 14, 30, 45), sb);
         * sb.toString();                                  // returns "2023-12-25 14:30:45"
         *
         * StringBuilder nb = new StringBuilder();
         * DTF.LOCAL_DATE.formatTo((TemporalAccessor) null, nb);
         * nb.toString();                                  // returns "null" (literal appended for null input)
         * }</pre>
         *
         * @param temporal the {@code TemporalAccessor} instance to format; if {@code null}, the string {@code "null"} is appended.
         * @param appendable the Appendable to which the formatted string will be appended; must not be {@code null}.
         * @throws DateTimeException if an error occurs during formatting.
         * @throws UncheckedIOException if an I/O error occurs while appending.
         * @see DateTimeFormatter#formatTo(TemporalAccessor, Appendable)
         */
        public void formatTo(final TemporalAccessor temporal, final Appendable appendable) {
            N.checkArgNotNull(appendable, cs.appendable);

            if (temporal == null) {
                formatToForNull(appendable);
                return;
            }

            appendFormatted(appendable, format(temporal));
        }

        /**
         * Parses the provided CharSequence into a LocalDate instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.LOCAL_DATE.parseToLocalDate("2023-12-25");    // returns LocalDate 2023-12-25
         * DTF.LOCAL_DATE.parseToLocalDate("1672585530123"); // returns numeric = epoch millis -> the local date at that instant
         *
         * DTF.LOCAL_DATE.parseToLocalDate("");                  // returns null
         * DTF.LOCAL_DATE.parseToLocalDate((CharSequence) null); // returns null
         * }</pre>
         *
         * <p>Note: for the UTC {@code 'Z'} formats ({@code ISO_8601_DATE_TIME}, {@code ISO_8601_TIMESTAMP})
         * the result is the UTC wall-clock value exactly as written in the text, not the value shifted
         * into the default time zone.</p>
         *
         * @param text the CharSequence to parse; may be {@code null} or empty. A purely numeric value is interpreted as epoch milliseconds.
         * @return a LocalDate instance representing the parsed date, or {@code null} if {@code text} is {@code null} or empty.
         * @throws DateTimeParseException if the text is non-empty and cannot be parsed to a date.
         * @throws IllegalArgumentException if the text cannot be parsed via the fallback {@code Dates} parsing path
         *         (used for formats other than the natively supported ones).
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
                    // For the UTC 'Z' formats, return the wall-clock value as written (UTC), matching
                    // the ZONED/OFFSET siblings, instead of shifting it into the default zone.
                    return cal == null ? null : LocalDate.ofInstant(cal.toInstant(), utcZFormat ? UTC_TIME_ZONE.toZoneId() : cal.getTimeZone().toZoneId());
                }
            }
        }

        /**
         * Parses the provided CharSequence into a LocalTime instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.LOCAL_TIME.parseToLocalTime("14:30:45");      // returns LocalTime 14:30:45
         * DTF.LOCAL_TIME.parseToLocalTime("1672585530123"); // returns numeric = epoch millis -> the local time at that instant
         *
         * DTF.LOCAL_TIME.parseToLocalTime("");                  // returns null
         * DTF.LOCAL_TIME.parseToLocalTime((CharSequence) null); // returns null
         * }</pre>
         *
         * <p>Note: for the UTC {@code 'Z'} formats ({@code ISO_8601_DATE_TIME}, {@code ISO_8601_TIMESTAMP})
         * the result is the UTC wall-clock value exactly as written in the text, not the value shifted
         * into the default time zone.</p>
         *
         * @param text the CharSequence to parse; may be {@code null} or empty. A purely numeric value is interpreted as epoch milliseconds.
         * @return a LocalTime instance representing the parsed time, or {@code null} if {@code text} is {@code null} or empty.
         * @throws DateTimeParseException if the text is non-empty and cannot be parsed to a time.
         * @throws IllegalArgumentException if the text cannot be parsed via the fallback {@code Dates} parsing path
         *         (used for formats other than the natively supported ones).
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
                    // For the UTC 'Z' formats, return the wall-clock value as written (UTC), matching
                    // the ZONED/OFFSET siblings, instead of shifting it into the default zone.
                    return cal == null ? null : LocalTime.ofInstant(cal.toInstant(), utcZFormat ? UTC_TIME_ZONE.toZoneId() : cal.getTimeZone().toZoneId());
                }
            }
        }

        /**
         * Parses the provided CharSequence into a LocalDateTime instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.LOCAL_DATE_TIME.parseToLocalDateTime("2023-12-25 14:30:45");   // returns LocalDateTime 2023-12-25T14:30:45
         * DTF.LOCAL_DATE_TIME.parseToLocalDateTime("1672585530123");         // returns numeric = epoch millis -> local date-time at that instant
         *
         * DTF.LOCAL_DATE_TIME.parseToLocalDateTime("");                     // returns null
         * DTF.LOCAL_DATE_TIME.parseToLocalDateTime((CharSequence) null);    // returns null
         * }</pre>
         *
         * <p>Note: for the UTC {@code 'Z'} formats ({@code ISO_8601_DATE_TIME}, {@code ISO_8601_TIMESTAMP})
         * the result is the UTC wall-clock value exactly as written in the text, not the value shifted
         * into the default time zone.</p>
         *
         * @param text the CharSequence to parse; may be {@code null} or empty. A purely numeric value is interpreted as epoch milliseconds.
         * @return a LocalDateTime instance representing the parsed date and time, or {@code null} if {@code text} is {@code null} or empty.
         * @throws DateTimeParseException if the text is non-empty and cannot be parsed to a date and time.
         * @throws IllegalArgumentException if the text cannot be parsed via the fallback {@code Dates} parsing path
         *         (used for formats other than the natively supported ones).
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
                    // For the UTC 'Z' formats, return the wall-clock value as written (UTC), matching
                    // the ZONED/OFFSET siblings, instead of shifting it into the default zone.
                    return cal == null ? null : LocalDateTime.ofInstant(cal.toInstant(), utcZFormat ? UTC_TIME_ZONE.toZoneId() : cal.getTimeZone().toZoneId());
                }
            }
        }

        /**
         * Parses the provided CharSequence into an OffsetDateTime instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime("2023-12-25T14:30:45+05:30");
         *                                                  // returns OffsetDateTime 2023-12-25T14:30:45+05:30
         * DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime("1672585530123"); // returns numeric = epoch millis
         *
         * DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime("");                  // returns null
         * DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime((CharSequence) null); // returns null
         * }</pre>
         *
         * <p>Note: for the UTC {@code 'Z'} formats ({@code ISO_8601_DATE_TIME}, {@code ISO_8601_TIMESTAMP})
         * the result carries the {@code +00:00} offset, matching the {@code 'Z'} designator. For any other
         * format that is neither {@code ISO_OFFSET_DATE_TIME} nor {@code ISO_ZONED_DATE_TIME}, parsing falls
         * back to the calendar-based path and the result carries the JVM default time zone's offset (the
         * instant is always preserved).</p>
         *
         * @param text the CharSequence to parse; may be {@code null} or empty. A purely numeric value is interpreted as epoch milliseconds.
         * @return an OffsetDateTime instance representing the parsed date and time, or {@code null} if {@code text} is {@code null} or empty.
         * @throws DateTimeParseException if the text is non-empty and cannot be parsed to a date and time.
         * @throws IllegalArgumentException if the text cannot be parsed via the fallback {@code Dates} parsing path
         *         (used for formats other than the natively supported ones).
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
                // For the UTC 'Z' formats the value is a UTC instant, so stamp +00:00 rather than the
                // default zone's offset (mirrors parseToLocalDate/Time/DateTime).
                return cal == null ? null : OffsetDateTime.ofInstant(cal.toInstant(), utcZFormat ? UTC_TIME_ZONE.toZoneId() : cal.getTimeZone().toZoneId());
            }
        }

        /**
         * Parses the provided CharSequence into a ZonedDateTime instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime("2023-12-25T14:30:45+05:30[Asia/Kolkata]");
         *                                                  // returns ZonedDateTime 2023-12-25T14:30:45+05:30[Asia/Kolkata]
         * DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime("1672585530123"); // returns numeric = epoch millis
         *
         * DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime("");                  // returns null
         * DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime((CharSequence) null); // returns null
         * }</pre>
         *
         * <p>Note: for the UTC {@code 'Z'} formats ({@code ISO_8601_DATE_TIME}, {@code ISO_8601_TIMESTAMP})
         * the result carries the UTC zone, matching the {@code 'Z'} designator. For any other format that is
         * neither {@code ISO_ZONED_DATE_TIME} nor {@code ISO_OFFSET_DATE_TIME}, parsing falls back to the
         * calendar-based path and the result carries the JVM default time zone (the instant is always
         * preserved).</p>
         *
         * @param text the CharSequence to parse; may be {@code null} or empty. A purely numeric value is interpreted as epoch milliseconds.
         * @return a ZonedDateTime instance representing the parsed date and time, or {@code null} if {@code text} is {@code null} or empty.
         * @throws DateTimeParseException if the text is non-empty and cannot be parsed to a date and time.
         * @throws IllegalArgumentException if the text cannot be parsed via the fallback {@code Dates} parsing path
         *         (used for formats other than the natively supported ones).
         * @see Instant#ofEpochMilli(long)
         * @see ZonedDateTime#ofInstant(Instant, ZoneId)
         * @see ZonedDateTime#from(TemporalAccessor)
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
                // For the UTC 'Z' formats the value is a UTC instant, so stamp the UTC zone rather than the
                // default zone (mirrors parseToLocalDate/Time/DateTime).
                return cal == null ? null : ZonedDateTime.ofInstant(cal.toInstant(), utcZFormat ? UTC_TIME_ZONE.toZoneId() : cal.getTimeZone().toZoneId());
            }
        }

        /**
         * Parses the provided CharSequence into an Instant instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.ISO_8601_DATE_TIME.parseToInstant("2023-12-25T14:30:45Z").toEpochMilli();   // returns 1703514645000
         * DTF.ISO_8601_DATE_TIME.parseToInstant("1672585530123").toEpochMilli();          // returns 1672585530123 (numeric = epoch millis)
         *
         * DTF.ISO_8601_DATE_TIME.parseToInstant("");                  // returns null
         * DTF.ISO_8601_DATE_TIME.parseToInstant((CharSequence) null); // returns null
         * }</pre>
         *
         * @param text the CharSequence to parse; may be {@code null} or empty. A purely numeric value is interpreted as epoch milliseconds.
         * @return an Instant instance representing the parsed date and time, or {@code null} if {@code text} is {@code null} or empty.
         * @throws DateTimeParseException if the text is non-empty and cannot be parsed to a date and time.
         * @throws IllegalArgumentException if the text cannot be parsed via the fallback {@code Dates} parsing path
         *         (used for formats other than the natively supported ones).
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
                final Timestamp ts = parseToTimestamp(text);
                return ts == null ? null : ts.toInstant();
            }
        }

        /**
         * Parses the provided CharSequence into a {@code java.util.Date} instance using this formatter's pattern.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.ISO_8601_DATE_TIME.parseToJUDate("2023-12-25T14:30:45Z").getTime();   // returns 1703514645000
         *
         * DTF.ISO_8601_DATE_TIME.parseToJUDate("");                  // returns null
         * DTF.ISO_8601_DATE_TIME.parseToJUDate((CharSequence) null); // returns null
         * }</pre>
         *
         * @param text the CharSequence to parse; may be {@code null} or empty.
         * @return a {@code java.util.Date} instance representing the parsed date and time, or {@code null} if {@code text} is {@code null} or empty.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed using this formatter's pattern.
         * @see #parseToJUDate(CharSequence, TimeZone)
         * @see Dates#parseJUDate(String, String)
         */
        @MayReturnNull
        public java.util.Date parseToJUDate(final CharSequence text) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            if (isPossibleLong(text)) {
                try {
                    return Dates.createJUDate(Numbers.toLong(text));
                } catch (final NumberFormatException e) {
                    // ignore;
                    if (logger.isWarnEnabled()) {
                        logger.warn(FAILED_TO_PARSE_TO_LONG, text);
                    }
                }
            }

            if (this.format.equals(ISO_ZONED_DATE_TIME_FORMAT)) {
                // The zoned pattern is DateTimeFormatter-only; parse via java.time and convert to the instant.
                return java.util.Date.from(ZonedDateTime.parse(text, dateTimeFormatter).toInstant());
            }

            return Dates.parseJUDate(text.toString(), format);
        }

        /**
         * Parses the provided CharSequence into a java.util.Date instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * TimeZone utc = TimeZone.getTimeZone("UTC");
         * DTF.LOCAL_DATE_TIME.parseToJUDate("2023-12-25 14:30:45", utc).getTime();   // returns 1703514645000
         *
         * DTF.LOCAL_DATE_TIME.parseToJUDate("", utc);                  // returns null
         * DTF.LOCAL_DATE_TIME.parseToJUDate((CharSequence) null, utc); // returns null
         * }</pre>
         *
         * <p>Note: pure numeric epoch-millisecond input preserves the raw {@code getTime()} value and ignores {@code tz}.</p>
         *
         * @param text the CharSequence to parse; may be {@code null} or empty.
         * @param tz the time zone to use for parsing the date and time; if {@code null}, the default time zone is used.
         * @return a {@code java.util.Date} instance representing the parsed date and time, or {@code null} if {@code text} is {@code null} or empty.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed using this formatter's pattern.
         * @see #parseToJUDate(CharSequence)
         * @see Dates#parseJUDate(String, String, TimeZone)
         */
        @MayReturnNull
        public java.util.Date parseToJUDate(final CharSequence text, final TimeZone tz) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            if (isPossibleLong(text)) {
                try {
                    return Dates.createJUDate(Numbers.toLong(text));
                } catch (final NumberFormatException e) {
                    // ignore;
                    if (logger.isWarnEnabled()) {
                        logger.warn(FAILED_TO_PARSE_TO_LONG, text);
                    }
                }
            }

            if (this.format.equals(ISO_ZONED_DATE_TIME_FORMAT)) {
                return java.util.Date.from(ZonedDateTime.parse(text, dateTimeFormatter).toInstant());
            }

            return Dates.parseJUDate(text.toString(), format, tz);
        }

        /**
         * Parses the provided CharSequence into a {@code java.sql.Date} instance using this formatter's pattern.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Dates.format(DTF.LOCAL_DATE.parseToDate("2023-12-25"), "yyyy-MM-dd");   // returns "2023-12-25" (default-zone round-trip)
         *
         * DTF.LOCAL_DATE.parseToDate("");                  // returns null
         * DTF.LOCAL_DATE.parseToDate((CharSequence) null); // returns null
         * }</pre>
         *
         * <p>Note: pure numeric epoch-millisecond input preserves the raw {@code getTime()} value for serialization round-trips.</p>
         *
         * @param text the CharSequence to parse; may be {@code null} or empty.
         * @return a {@code java.sql.Date} instance representing the parsed date, or {@code null} if {@code text} is {@code null} or empty.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed using this formatter's pattern.
         * @see #parseToDate(CharSequence, TimeZone)
         * @see Dates#parseDate(String, String)
         */
        @MayReturnNull
        public java.sql.Date parseToDate(final CharSequence text) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            if (isPossibleLong(text)) {
                try {
                    // Numeric epoch millis preserve their raw value for serialization round-trips.
                    return Dates.createDate(Numbers.toLong(text));
                } catch (final NumberFormatException e) {
                    // ignore;
                    if (logger.isWarnEnabled()) {
                        logger.warn(FAILED_TO_PARSE_TO_LONG, text);
                    }
                }
            }

            if (this.format.equals(ISO_ZONED_DATE_TIME_FORMAT)) {
                return Dates.createDate(truncateMillisToDate(ZonedDateTime.parse(text, dateTimeFormatter).toInstant().toEpochMilli(), null));
            }

            return Dates.parseDate(text.toString(), format);
        }

        /**
         * Parses the provided CharSequence into a java.sql.Date instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * TimeZone utc = TimeZone.getTimeZone("UTC");
         * DTF.LOCAL_DATE.parseToDate("2023-12-25", utc).getTime();   // returns 1703462400000 (midnight UTC)
         *
         * DTF.LOCAL_DATE.parseToDate("", utc);                  // returns null
         * DTF.LOCAL_DATE.parseToDate((CharSequence) null, utc); // returns null
         * }</pre>
         *
         * <p>Note: pure numeric epoch-millisecond input preserves the raw {@code getTime()} value and ignores {@code tz}.</p>
         *
         * @param text the CharSequence to parse; may be {@code null} or empty.
         * @param tz the time zone to use for parsing the date; if {@code null}, the default time zone is used.
         * @return a {@code java.sql.Date} instance representing the parsed date, or {@code null} if {@code text} is {@code null} or empty.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed using this formatter's pattern.
         * @see #parseToDate(CharSequence)
         * @see Dates#parseDate(String, String, TimeZone)
         */
        @MayReturnNull
        public java.sql.Date parseToDate(final CharSequence text, final TimeZone tz) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            if (isPossibleLong(text)) {
                try {
                    // Numeric epoch millis preserve their raw value for serialization round-trips.
                    return Dates.createDate(Numbers.toLong(text));
                } catch (final NumberFormatException e) {
                    // ignore;
                    if (logger.isWarnEnabled()) {
                        logger.warn(FAILED_TO_PARSE_TO_LONG, text);
                    }
                }
            }

            if (this.format.equals(ISO_ZONED_DATE_TIME_FORMAT)) {
                return Dates.createDate(truncateMillisToDate(ZonedDateTime.parse(text, dateTimeFormatter).toInstant().toEpochMilli(), tz));
            }

            return Dates.parseDate(text.toString(), format, tz);
        }

        /**
         * Parses the provided CharSequence into a java.sql.Time instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Dates.format(DTF.LOCAL_TIME.parseToTime("14:30:45"), "HH:mm:ss");   // returns "14:30:45" (default zone round-trip)
         *
         * DTF.LOCAL_TIME.parseToTime("");                  // returns null
         * DTF.LOCAL_TIME.parseToTime((CharSequence) null); // returns null
         * }</pre>
         *
         * <p>Note: pure numeric epoch-millisecond input preserves the raw {@code getTime()} value for serialization round-trips.</p>
         *
         * @param text the CharSequence to parse; may be {@code null} or empty.
         * @return a {@code java.sql.Time} instance representing the parsed time, or {@code null} if {@code text} is {@code null} or empty.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed using this formatter's pattern.
         * @see #parseToTime(CharSequence, TimeZone)
         * @see Dates#parseTime(String, String)
         */
        @MayReturnNull
        public Time parseToTime(final CharSequence text) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            if (isPossibleLong(text)) {
                try {
                    // Numeric epoch millis preserve their raw value for serialization round-trips.
                    return Dates.createTime(Numbers.toLong(text));
                } catch (final NumberFormatException e) {
                    // ignore;
                    if (logger.isWarnEnabled()) {
                        logger.warn(FAILED_TO_PARSE_TO_LONG, text);
                    }
                }
            }

            if (this.format.equals(ISO_ZONED_DATE_TIME_FORMAT)) {
                return Dates.createTime(normalizeMillisToTime(ZonedDateTime.parse(text, dateTimeFormatter).toInstant().toEpochMilli(), null));
            }

            return Dates.parseTime(text.toString(), format);
        }

        /**
         * Parses the provided CharSequence into a java.sql.Time instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * TimeZone utc = TimeZone.getTimeZone("UTC");
         * DTF.LOCAL_TIME.parseToTime("14:30:45", utc).getTime();   // returns 52245000 (time-of-day on 1970-01-01 in UTC)
         *
         * DTF.LOCAL_TIME.parseToTime("", utc);                  // returns null
         * DTF.LOCAL_TIME.parseToTime((CharSequence) null, utc); // returns null
         * }</pre>
         *
         * <p>Note: pure numeric epoch-millisecond input preserves the raw {@code getTime()} value and ignores {@code tz}.</p>
         *
         * @param text the CharSequence to parse; may be {@code null} or empty.
         * @param tz the time zone to use for parsing the time; if {@code null}, the default time zone is used.
         * @return a {@code java.sql.Time} instance representing the parsed time, or {@code null} if {@code text} is {@code null} or empty.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed using this formatter's pattern.
         * @see #parseToTime(CharSequence)
         * @see Dates#parseTime(String, String, TimeZone)
         */
        @MayReturnNull
        public Time parseToTime(final CharSequence text, final TimeZone tz) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            if (isPossibleLong(text)) {
                try {
                    // Numeric epoch millis preserve their raw value for serialization round-trips.
                    return Dates.createTime(Numbers.toLong(text));
                } catch (final NumberFormatException e) {
                    // ignore;
                    if (logger.isWarnEnabled()) {
                        logger.warn(FAILED_TO_PARSE_TO_LONG, text);
                    }
                }
            }

            if (this.format.equals(ISO_ZONED_DATE_TIME_FORMAT)) {
                return Dates.createTime(normalizeMillisToTime(ZonedDateTime.parse(text, dateTimeFormatter).toInstant().toEpochMilli(), tz));
            }

            return Dates.parseTime(text.toString(), format, tz);
        }

        /**
         * Parses the provided CharSequence into a java.sql.Timestamp instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.ISO_8601_TIMESTAMP.parseToTimestamp("2023-12-25T14:30:45.123Z").getTime();   // returns 1703514645123
         *
         * DTF.ISO_8601_TIMESTAMP.parseToTimestamp("");                  // returns null
         * DTF.ISO_8601_TIMESTAMP.parseToTimestamp((CharSequence) null); // returns null
         * }</pre>
         *
         * @param text the CharSequence to parse; may be {@code null} or empty.
         * @return a {@code java.sql.Timestamp} instance representing the parsed date and time, or {@code null} if {@code text} is {@code null} or empty.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed using this formatter's pattern.
         * @see #parseToTimestamp(CharSequence, TimeZone)
         * @see Dates#parseTimestamp(String, String)
         */
        @MayReturnNull
        public Timestamp parseToTimestamp(final CharSequence text) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            if (isPossibleLong(text)) {
                try {
                    return Dates.createTimestamp(Numbers.toLong(text));
                } catch (final NumberFormatException e) {
                    // ignore;
                    if (logger.isWarnEnabled()) {
                        logger.warn(FAILED_TO_PARSE_TO_LONG, text);
                    }
                }
            }

            if (this.format.equals(ISO_ZONED_DATE_TIME_FORMAT)) {
                return Dates.createTimestamp(ZonedDateTime.parse(text, dateTimeFormatter).toInstant().toEpochMilli());
            }

            return Dates.parseTimestamp(text.toString(), format);
        }

        /**
         * Parses the provided CharSequence into a java.sql.Timestamp instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * TimeZone utc = TimeZone.getTimeZone("UTC");
         * DTF.ISO_8601_TIMESTAMP.parseToTimestamp("2023-12-25T14:30:45.123Z", utc).getTime();   // returns 1703514645123
         *
         * DTF.ISO_8601_TIMESTAMP.parseToTimestamp("", utc);                  // returns null
         * DTF.ISO_8601_TIMESTAMP.parseToTimestamp((CharSequence) null, utc); // returns null
         * }</pre>
         *
         * <p>Note: pure numeric epoch-millisecond input preserves the raw {@code getTime()} value and ignores {@code tz}.</p>
         *
         * @param text the CharSequence to parse; may be {@code null} or empty.
         * @param tz the time zone to use for parsing the date and time; if {@code null}, the default time zone is used.
         * @return a {@code java.sql.Timestamp} instance representing the parsed date and time, or {@code null} if {@code text} is {@code null} or empty.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed using this formatter's pattern.
         * @see #parseToTimestamp(CharSequence)
         * @see Dates#parseTimestamp(String, String, TimeZone)
         */
        @MayReturnNull
        public Timestamp parseToTimestamp(final CharSequence text, final TimeZone tz) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            if (isPossibleLong(text)) {
                try {
                    return Dates.createTimestamp(Numbers.toLong(text));
                } catch (final NumberFormatException e) {
                    // ignore;
                    if (logger.isWarnEnabled()) {
                        logger.warn(FAILED_TO_PARSE_TO_LONG, text);
                    }
                }
            }

            if (this.format.equals(ISO_ZONED_DATE_TIME_FORMAT)) {
                return Dates.createTimestamp(ZonedDateTime.parse(text, dateTimeFormatter).toInstant().toEpochMilli());
            }

            return Dates.parseTimestamp(text.toString(), format, tz);
        }

        /**
         * Parses the provided CharSequence into a java.util.Calendar instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Dates.format(DTF.LOCAL_DATE_TIME.parseToCalendar("2023-12-25 14:30:45"), "yyyy-MM-dd HH:mm:ss");
         *                                                  // returns "2023-12-25 14:30:45" (default zone round-trip)
         *
         * DTF.LOCAL_DATE_TIME.parseToCalendar("");                  // returns null
         * DTF.LOCAL_DATE_TIME.parseToCalendar((CharSequence) null); // returns null
         * }</pre>
         *
         * @param text the CharSequence to parse; may be {@code null} or empty.
         * @return a {@code java.util.Calendar} instance representing the parsed date and time, or {@code null} if {@code text} is {@code null} or empty.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed using this formatter's pattern.
         * @see #parseToCalendar(CharSequence, TimeZone)
         * @see Dates#parseCalendar(String, String)
         */
        @MayReturnNull
        public Calendar parseToCalendar(final CharSequence text) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            if (isPossibleLong(text)) {
                try {
                    return Dates.createCalendar(Numbers.toLong(text));
                } catch (final NumberFormatException e) {
                    // ignore;
                    if (logger.isWarnEnabled()) {
                        logger.warn(FAILED_TO_PARSE_TO_LONG, text);
                    }
                }
            }

            if (this.format.equals(ISO_ZONED_DATE_TIME_FORMAT)) {
                return GregorianCalendar.from(ZonedDateTime.parse(text, dateTimeFormatter));
            }

            return Dates.parseCalendar(text.toString(), format);
        }

        /**
         * Parses the provided CharSequence into a java.util.Calendar instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * TimeZone utc = TimeZone.getTimeZone("UTC");
         * DTF.LOCAL_DATE_TIME.parseToCalendar("2023-12-25 14:30:45", utc).getTimeInMillis();   // returns 1703514645000
         *
         * DTF.LOCAL_DATE_TIME.parseToCalendar("", utc);                  // returns null
         * DTF.LOCAL_DATE_TIME.parseToCalendar((CharSequence) null, utc); // returns null
         * }</pre>
         *
         * @param text the CharSequence to parse; may be {@code null} or empty.
         * @param tz the time zone to use for parsing and for the returned calendar; if {@code null}, the default time zone is used.
         * @return a {@code java.util.Calendar} instance representing the parsed date and time, or {@code null} if {@code text} is {@code null} or empty.
         * @throws IllegalArgumentException if the text is non-empty and cannot be parsed using this formatter's pattern.
         * @see #parseToCalendar(CharSequence)
         * @see Dates#parseCalendar(String, String, TimeZone)
         */
        @MayReturnNull
        public Calendar parseToCalendar(final CharSequence text, final TimeZone tz) {
            if (Strings.isEmpty(text)) {
                return null;
            }

            if (isPossibleLong(text)) {
                try {
                    return Dates.createCalendar(Numbers.toLong(text), tz);
                } catch (final NumberFormatException e) {
                    // ignore;
                    if (logger.isWarnEnabled()) {
                        logger.warn(FAILED_TO_PARSE_TO_LONG, text);
                    }
                }
            }

            if (this.format.equals(ISO_ZONED_DATE_TIME_FORMAT)) {
                final ZonedDateTime zdt = ZonedDateTime.parse(text, dateTimeFormatter);
                return GregorianCalendar.from(tz == null ? zdt : zdt.withZoneSameInstant(tz.toZoneId()));
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
         * DTF formatter = DTF.LOCAL_DATE_TIME;   // pattern "yyyy-MM-dd HH:mm:ss"
         * TemporalAccessor temporal = formatter.parseToTemporalAccessor("2023-12-25 15:30:45");
         * int year = temporal.get(ChronoField.YEAR);             // returns 2023
         * int month = temporal.get(ChronoField.MONTH_OF_YEAR);   // returns 12
         * int day = temporal.get(ChronoField.DAY_OF_MONTH);      // returns 25
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

        /**
         * Returns this formatter's underlying date/time pattern string.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DTF.LOCAL_DATE_TIME.toString();    // returns "yyyy-MM-dd HH:mm:ss"
         * DTF.LOCAL_DATE.toString();         // returns "yyyy-MM-dd"
         * DTF.LOCAL_TIME.toString();         // returns "HH:mm:ss"
         * DTF.ISO_8601_TIMESTAMP.toString(); // returns "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
         * }</pre>
         *
         * @return the pattern string backing this {@code DTF}.
         */
        @Override
        public String toString() {
            return format;
        }
    }

    /**
     * A concrete, named subclass of {@link Dates} that exposes the full set of inherited
     * static date/time utility methods under the alternative name {@code DateUtil}.
     *
     * <p>It is the sole permitted subtype of the sealed {@code Dates} class and adds no
     * behavior of its own; it exists purely so callers may reference these utilities as
     * {@code DateUtil} (for example, in code bases that already use that name) while sharing
     * a single implementation. This class cannot be instantiated.</p>
     *
     * <p>{@code Dates} is the canonical name: prefer {@code Dates} in new code and reserve this
     * alias for code bases already standardized on the {@code DateUtil} name.</p>
     *
     * @see Dates
     */
    @Beta
    public static final class DateUtil extends Dates {

        private DateUtil() {
            // Utility class - prevent instantiation
        }
    }

    //    /**

}
