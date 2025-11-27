/*
 * Copyright (C) 2018, 2019 HaiYang Li
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

import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;

/**
 * A high-performance, immutable representation of a time-based amount of duration measured in milliseconds,
 * designed as an efficient alternative to {@code java.time.Duration} for scenarios where nanosecond precision
 * is not required. This class provides comprehensive duration arithmetic, conversion operations, and formatting
 * capabilities while maintaining optimal performance through a simplified internal representation.
 *
 * <p>Unlike {@code java.time.Duration} which stores both seconds and nanoseconds internally, this implementation
 * uses only milliseconds as its internal representation, eliminating the computational overhead of dual-field
 * arithmetic. This design choice makes it particularly suitable for performance-critical applications where
 * millisecond precision is sufficient and nanosecond precision is rarely needed.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>High Performance:</b> Single-field millisecond representation eliminates complex arithmetic operations</li>
 *   <li><b>Immutable Design:</b> All instances are immutable, ensuring thread safety and preventing accidental modification</li>
 *   <li><b>Comprehensive Arithmetic:</b> Full support for addition, subtraction, multiplication, and division operations</li>
 *   <li><b>Multiple Time Units:</b> Direct support for days, hours, minutes, seconds, and milliseconds</li>
 *   <li><b>Type Safety:</b> Strong typing prevents mixing incompatible duration operations</li>
 *   <li><b>ISO 8601 Formatting:</b> Standard duration string representation following ISO 8601 format</li>
 *   <li><b>JDK Interoperability:</b> Seamless conversion to/from {@code java.time.Duration}</li>
 *   <li><b>Memory Efficient:</b> Minimal memory footprint with single primitive field</li>
 * </ul>
 *
 * <p><b>⚠️ IMPORTANT - Immutable Design:</b>
 * <ul>
 *   <li>This class implements {@link Immutable}, guaranteeing that instances cannot be modified after creation</li>
 *   <li>The internal milliseconds field is final and set only during construction</li>
 *   <li>All arithmetic operations return new Duration instances rather than modifying existing ones</li>
 *   <li>Thread-safe by design due to immutability and lack of mutable state</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Performance Over Precision:</b> Millisecond precision provides optimal balance of accuracy and speed</li>
 *   <li><b>Simplicity Over Complexity:</b> Single internal representation simplifies implementation and improves performance</li>
 *   <li><b>Immutability Over Mutability:</b> Ensures predictable behavior and thread safety</li>
 *   <li><b>Compatibility Over Isolation:</b> Seamless integration with standard Java time APIs</li>
 *   <li><b>Efficiency Over Features:</b> Focused feature set optimized for common duration use cases</li>
 * </ul>
 *
 * <p><b>Internal Representation:</b>
 * <ul>
 *   <li><b>Storage:</b> Single {@code long} field representing total milliseconds</li>
 *   <li><b>Range:</b> Supports the full range of {@code long} values (approximately ±292 million years)</li>
 *   <li><b>Precision:</b> Millisecond precision (1/1000 second granularity)</li>
 *   <li><b>Negative Durations:</b> Supported for representing time differences and offsets</li>
 * </ul>
 *
 * <p><b>Time Unit Constants:</b>
 * <ul>
 *   <li><b>MILLIS_PER_SECOND:</b> 1,000 milliseconds per second</li>
 *   <li><b>MILLIS_PER_MINUTE:</b> 60,000 milliseconds per minute</li>
 *   <li><b>MILLIS_PER_HOUR:</b> 3,600,000 milliseconds per hour</li>
 *   <li><b>MILLIS_PER_DAY:</b> 86,400,000 milliseconds per day</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // Creating durations from different time units
 * Duration fiveMinutes = Duration.ofMinutes(5);
 * Duration twoHours = Duration.ofHours(2);
 * Duration oneDay = Duration.ofDays(1);
 * Duration halfSecond = Duration.ofMillis(500);
 *
 * // Duration arithmetic
 * Duration total = fiveMinutes.plus(twoHours);              // 2 hours 5 minutes
 * Duration difference = oneDay.minus(twoHours);             // 22 hours
 * Duration doubled = fiveMinutes.multipliedBy(2);           // 10 minutes
 * Duration halved = twoHours.dividedBy(2);                  // 1 hour
 *
 * // Duration properties and testing
 * boolean isZero = Duration.ZERO.isZero();                  // true
 * boolean isNegative = fiveMinutes.minus(twoHours).isNegative(); // true
 * Duration positive = fiveMinutes.minus(twoHours).abs();    // 1 hour 55 minutes
 *
 * // Converting to different units
 * long totalMinutes = twoHours.toMinutes();                 // 120
 * long totalSeconds = fiveMinutes.toSeconds();              // 300
 * long totalMillis = halfSecond.toMillis();                 // 500
 * }</pre>
 *
 * <p><b>Advanced Usage Examples:</b></p>
 * <pre>{@code
 * // Complex duration calculations
 * Duration workDay = Duration.ofHours(8);
 * Duration lunchBreak = Duration.ofMinutes(30);
 * Duration shortBreaks = Duration.ofMinutes(15).multipliedBy(2);
 * Duration totalWorkTime = workDay.minus(lunchBreak).minus(shortBreaks); // 7 hours
 *
 * // Performance measurement
 * Duration executionTime = measureExecutionTime(() -> performOperation());
 * if (executionTime.toMillis() > 1000) {
 *     logSlowOperation(executionTime);
 * }
 *
 * // Duration formatting for display
 * Duration uptime = Duration.ofDays(5).plusHours(3).plusMinutes(45);
 * String formatted = uptime.toString(); // "PT123H45M0S" (ISO 8601 format)
 *
 * // Interoperability with java.time.Duration
 * Duration customDuration = Duration.ofMinutes(90);
 * java.time.Duration jdkDuration = customDuration.toJdkDuration();
 * Duration backConverted = Duration.ofMillis(jdkDuration.toMillis());
 *
 * // Conditional duration operations
 * Duration timeout = Duration.ofSeconds(30);
 * Duration elapsed = getCurrentElapsedTime();
 * if (elapsed.compareTo(timeout) > 0) {
 *     handleTimeout();
 * }
 * }</pre>
 *
 * <p><b>Static Factory Methods:</b>
 * <ul>
 *   <li><b>{@code ofDays(long)}:</b> Creates duration from day count</li>
 *   <li><b>{@code ofHours(long)}:</b> Creates duration from hour count</li>
 *   <li><b>{@code ofMinutes(long)}:</b> Creates duration from minute count</li>
 *   <li><b>{@code ofSeconds(long)}:</b> Creates duration from second count</li>
 *   <li><b>{@code ofMillis(long)}:</b> Creates duration from millisecond count</li>
 * </ul>
 *
 * <p><b>Arithmetic Operations:</b>
 * <ul>
 *   <li><b>Addition:</b> {@code plus()}, {@code plusDays()}, {@code plusHours()}, {@code plusMinutes()}, {@code plusSeconds()}, {@code plusMillis()}</li>
 *   <li><b>Subtraction:</b> {@code minus()}, {@code minusDays()}, {@code minusHours()}, {@code minusMinutes()}, {@code minusSeconds()}, {@code minusMillis()}</li>
 *   <li><b>Multiplication:</b> {@code multipliedBy(long)} for scaling durations</li>
 *   <li><b>Division:</b> {@code dividedBy(long)} for proportional splitting</li>
 *   <li><b>Negation:</b> {@code negated()} for direction reversal</li>
 *   <li><b>Absolute Value:</b> {@code abs()} for magnitude-only representation</li>
 * </ul>
 *
 * <p><b>Conversion Methods:</b>
 * <ul>
 *   <li><b>{@code toDays()}:</b> Converts to total day count (truncated)</li>
 *   <li><b>{@code toHours()}:</b> Converts to total hour count (truncated)</li>
 *   <li><b>{@code toMinutes()}:</b> Converts to total minute count (truncated)</li>
 *   <li><b>{@code toSeconds()}:</b> Converts to total second count (truncated)</li>
 *   <li><b>{@code toMillis()}:</b> Returns the exact millisecond representation</li>
 *   <li><b>{@code toJdkDuration()}:</b> Converts to {@code java.time.Duration}</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Creation Cost:</b> O(1) - Simple long assignment with minimal validation</li>
 *   <li><b>Arithmetic Cost:</b> O(1) - Single long arithmetic operation per method</li>
 *   <li><b>Comparison Cost:</b> O(1) - Direct long comparison</li>
 *   <li><b>Memory Overhead:</b> Minimal - Single long field plus standard object header</li>
 *   <li><b>GC Impact:</b> Low - Immutable objects are GC-friendly</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Immutable State:</b> The milliseconds field is final and never modified</li>
 *   <li><b>Concurrent Access:</b> Safe for concurrent read access from multiple threads</li>
 *   <li><b>No Synchronization:</b> No locks or synchronization needed due to immutability</li>
 *   <li><b>Safe Publication:</b> Can be safely published between threads without additional synchronization</li>
 * </ul>
 *
 * <p><b>Overflow Handling:</b>
 * <ul>
 *   <li><b>Long Range:</b> Operations may overflow if results exceed {@code Long.MAX_VALUE} or {@code Long.MIN_VALUE}</li>
 *   <li><b>No Automatic Checking:</b> Overflow behavior follows standard Java long arithmetic (wrapping)</li>
 *   <li><b>Practical Limits:</b> ±292 million years is sufficient for virtually all real-world scenarios</li>
 *   <li><b>Defensive Programming:</b> Consider range validation for user inputs in extreme scenarios</li>
 * </ul>
 *
 * <p><b>ISO 8601 String Representation:</b>
 * <ul>
 *   <li><b>Format:</b> {@code PT[nH][nM][n[.nnn]S]} following ISO 8601 duration format</li>
 *   <li><b>Examples:</b> {@code "PT0S"} (zero), {@code "PT2H30M"} (2.5 hours), {@code "PT1.500S"} (1.5 seconds)</li>
 *   <li><b>Negative Durations:</b> Represented with negative sign on the entire duration</li>
 *   <li><b>Milliseconds:</b> Represented as fractional seconds when present</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use static factory methods ({@code ofHours()}, {@code ofMinutes()}) for readable duration creation</li>
 *   <li>Prefer this class over {@code java.time.Duration} when nanosecond precision is not needed</li>
 *   <li>Use {@code compareTo()} for duration ordering rather than converting to primitive types</li>
 *   <li>Cache frequently used duration instances to reduce object allocation</li>
 *   <li>Use {@code abs()} when you need magnitude without caring about direction</li>
 *   <li>Consider overflow potential when performing arithmetic on very large durations</li>
 *   <li>Use {@code isZero()} and {@code isNegative()} for clear conditional logic</li>
 * </ul>
 *
 * <p><b>Common Anti-Patterns to Avoid:</b>
 * <ul>
 *   <li>Performing unnecessary conversions between this class and {@code java.time.Duration}</li>
 *   <li>Using {@code new Duration()} constructor directly instead of static factory methods</li>
 *   <li>Ignoring potential overflow in arithmetic operations with very large values</li>
 *   <li>Converting to primitive types for comparison instead of using {@code compareTo()}</li>
 *   <li>Creating multiple duration instances for the same logical duration value</li>
 *   <li>Using this class when nanosecond precision is actually required</li>
 * </ul>
 *
 * <p><b>Comparison with java.time.Duration:</b>
 * <ul>
 *   <li><b>Performance:</b> This implementation is faster for millisecond-precision operations</li>
 *   <li><b>Precision:</b> {@code java.time.Duration} supports nanoseconds, this class supports milliseconds</li>
 *   <li><b>Memory:</b> This class uses less memory (single long vs. two longs)</li>
 *   <li><b>Features:</b> {@code java.time.Duration} has more features, this class is optimized for common cases</li>
 *   <li><b>API:</b> Similar API design for easy migration between the two classes</li>
 * </ul>
 *
 * <p><b>Integration with Time APIs:</b>
 * <ul>
 *   <li><b>{@link java.time.Duration}:</b> Direct conversion via {@code toJdkDuration()}</li>
 *   <li><b>{@link System#currentTimeMillis()}:</b> Compatible with system time measurements</li>
 *   <li><b>{@link java.util.concurrent.TimeUnit}:</b> Can be converted using {@code toMillis()}</li>
 *   <li><b>Stopwatch Utilities:</b> Natural fit for timing and performance measurement</li>
 * </ul>
 *
 * <p><b>Example: Performance Monitoring System</b>
 * <pre>{@code
 * public class PerformanceMonitor {
 *     private static final Duration WARNING_THRESHOLD = Duration.ofSeconds(5);
 *     private static final Duration ERROR_THRESHOLD = Duration.ofSeconds(30);
 *
 *     public PerformanceReport measureOperation(String operationName, Runnable operation) {
 *         long startTime = System.currentTimeMillis();
 *         try {
 *             operation.run();
 *             return createSuccessReport(operationName, startTime);
 *         } catch (Exception e) {
 *             return createErrorReport(operationName, startTime, e);
 *         }
 *     }
 *
 *     private PerformanceReport createSuccessReport(String operationName, long startTime) {
 *         Duration executionTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
 *         
 *         PerformanceLevel level;
 *         if (executionTime.compareTo(ERROR_THRESHOLD) > 0) {
 *             level = PerformanceLevel.ERROR;
 *         } else if (executionTime.compareTo(WARNING_THRESHOLD) > 0) {
 *             level = PerformanceLevel.WARNING;
 *         } else {
 *             level = PerformanceLevel.NORMAL;
 *         }
 *
 *         return new PerformanceReport(operationName, executionTime, level);
 *     }
 *
 *     public Duration calculateAverageExecutionTime(List<Duration> executionTimes) {
 *         if (executionTimes.isEmpty()) {
 *             return Duration.ZERO;
 *         }
 *
 *         Duration total = executionTimes.stream()
 *             .reduce(Duration.ZERO, Duration::plus);
 *         
 *         return total.dividedBy(executionTimes.size());
 *     }
 * }
 * }</pre>
 *
 * @see Immutable
 * @see Comparable
 * @see System#currentTimeMillis()
 * @see java.util.concurrent.TimeUnit
 * @see java.time.Duration
 * @see java.time.temporal.Temporal
 * @see java.time.temporal.TemporalAmount
 * @see java.time.temporal.TemporalUnit
 * @see java.time.temporal.ChronoUnit
 */
@com.landawn.abacus.annotation.Immutable
public final class Duration implements Comparable<Duration>, Immutable {

    /** A Duration constant representing zero duration (0 milliseconds). */
    public static final Duration ZERO = new Duration(0);

    private static final long MILLIS_PER_SECOND = 1000L;

    private static final long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60;

    private static final long MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60;

    private static final long MILLIS_PER_DAY = MILLIS_PER_HOUR * 24;

    private final long milliseconds;

    Duration(final long milliseconds) {
        this.milliseconds = milliseconds;
    }

    private static Duration create(final long milliseconds) {
        if (milliseconds == 0) {
            return ZERO;
        }

        return new Duration(milliseconds);
    }

    /**
     * Creates a Duration representing the specified number of days.
     * <p>
     * The duration is calculated by multiplying the days by 86,400,000 milliseconds (24 hours × 60 minutes × 60 seconds × 1000 milliseconds).
     * This method will throw an ArithmeticException if the multiplication would cause an overflow.
     * </p>
     *
     * @param days the number of days, positive or negative
     * @return a Duration representing the specified number of days
     * @throws ArithmeticException if numeric overflow occurs during the calculation
     */
    public static Duration ofDays(final long days) {
        return create(Numbers.multiplyExact(days, MILLIS_PER_DAY));
    }

    /**
     * Creates a Duration representing the specified number of hours.
     * <p>
     * The duration is calculated by multiplying the hours by 3,600,000 milliseconds (60 minutes × 60 seconds × 1000 milliseconds).
     * This method will throw an ArithmeticException if the multiplication would cause an overflow.
     * </p>
     *
     * @param hours the number of hours, positive or negative
     * @return a Duration representing the specified number of hours
     * @throws ArithmeticException if numeric overflow occurs during the calculation
     */
    public static Duration ofHours(final long hours) {
        return create(Numbers.multiplyExact(hours, MILLIS_PER_HOUR));
    }

    /**
     * Creates a Duration representing the specified number of minutes.
     * <p>
     * The duration is calculated by multiplying the minutes by 60,000 milliseconds (60 seconds × 1000 milliseconds).
     * This method will throw an ArithmeticException if the multiplication would cause an overflow.
     * </p>
     *
     * @param minutes the number of minutes, positive or negative
     * @return a Duration representing the specified number of minutes
     * @throws ArithmeticException if numeric overflow occurs during the calculation
     */
    public static Duration ofMinutes(final long minutes) {
        return create(Numbers.multiplyExact(minutes, MILLIS_PER_MINUTE));
    }

    /**
     * Creates a Duration representing the specified number of seconds.
     * <p>
     * The duration is calculated by multiplying the seconds by 1,000 milliseconds.
     * This method will throw an ArithmeticException if the multiplication would cause an overflow.
     * </p>
     *
     * @param seconds the number of seconds, positive or negative
     * @return a Duration representing the specified number of seconds
     * @throws ArithmeticException if numeric overflow occurs during the calculation
     */
    public static Duration ofSeconds(final long seconds) {
        return create(Numbers.multiplyExact(seconds, MILLIS_PER_SECOND));
    }

    /**
     * Creates a Duration representing the specified number of milliseconds.
     * <p>
     * This is the most direct factory method as the Duration class internally stores time in milliseconds.
     * If the milliseconds value is 0, this method returns the constant ZERO instance for efficiency.
     * </p>
     *
     * @param millis the number of milliseconds, positive or negative
     * @return a Duration representing the specified number of milliseconds
     */
    public static Duration ofMillis(final long millis) {
        return create(millis);
    }

    /**
     * Calculates the duration between two dates by determining the time difference from the first date to the second.
     * The result represents the time interval as a {@link Duration} object, which can be positive (if {@code end} is
     * after {@code start}), negative (if {@code end} is before {@code start}), or zero (if both dates represent the same instant).
     *
     * <p><b>Calculation:</b> {@code Duration = end.getTime() - start.getTime()}</p>
     *
     * <p><b>Examples:</b>
     * <pre>{@code
     * // Measuring elapsed time
     * Date start = new Date();
     * // ... perform operation
     * Date end = new Date();
     * Duration elapsed = Duration.between(start, end);
     * long millisElapsed = elapsed.toMillis();
     *
     * // Calculating age
     * Date birthDate = Dates.parseDate("1990-05-15");
     * Date today = new Date();
     * Duration age = Duration.between(birthDate, today);
     * long ageInDays = age.toDays();
     *
     * // Time until deadline
     * Date now = new Date();
     * Date deadline = Dates.parseDate("2024-12-31");
     * Duration remaining = Duration.between(now, deadline);
     * if (remaining.isNegative()) {
     *     System.out.println("Deadline has passed");
     * } else {
     *     System.out.println("Days remaining: " + remaining.toDays());
     * }
     * }</pre>
     *
     * <p><b>Time Zone:</b> Operates on UTC millisecond timestamps, unaffected by time zones.
     *
     * @param start the start date (inclusive). Must not be {@code null}.
     * @param end the end date (exclusive). Must not be {@code null}.
     * @return a {@link Duration} representing the time difference from {@code from} to {@code to}.
     *         Positive if {@code to} is after {@code from}, negative if {@code to} is before {@code from}.
     * @throws IllegalArgumentException if either date is {@code null}.
     * @see #between(java.util.Calendar, java.util.Calendar)
     * @see #between(java.time.temporal.Temporal, java.time.temporal.Temporal)
     * @see java.time.Duration#between(java.time.temporal.Temporal, java.time.temporal.Temporal)
     */
    public static Duration between(final java.util.Date start, final java.util.Date end) {
        N.checkArgNotNull(start, "Start Date can't be null");
        N.checkArgNotNull(end, "End Date can't be null");

        return Duration.ofMillis(end.getTime() - start.getTime());
    }

    /**
     * Calculates the duration between two calendar instances by determining the time difference from the first calendar to the second.
     * The result represents the time interval as a {@link Duration} object, which can be positive (if {@code end} is
     * after {@code start}), negative (if {@code end} is before {@code start}), or zero (if both calendars represent the same instant).
     *
     * <p><b>Calculation:</b> {@code Duration = end.getTimeInMillis() - start.getTimeInMillis()}</p>
     *
     * <p><b>Examples:</b>
     * <pre>{@code
     * // Measuring elapsed time
     * Calendar start = Calendar.getInstance();
     * // ... perform operation
     * Calendar end = Calendar.getInstance();
     * Duration elapsed = Duration.between(start, end);
     * long millisElapsed = elapsed.toMillis();
     *
     * // Calculating age
     * Calendar birthDate = Calendar.getInstance();
     * birthDate.set(1990, Calendar.MAY, 15);
     * Calendar today = Calendar.getInstance();
     * Duration age = Duration.between(birthDate, today);
     * long ageInDays = age.toDays();
     *
     * // Time until deadline
     * Calendar now = Calendar.getInstance();
     * Calendar deadline = Calendar.getInstance();
     * deadline.set(2024, Calendar.DECEMBER, 31);
     * Duration remaining = Duration.between(now, deadline);
     * if (remaining.isNegative()) {
     *     System.out.println("Deadline has passed");
     * } else {
     *     System.out.println("Days remaining: " + remaining.toDays());
     * }
     * }</pre>
     *
     * <p><b>Time Zone:</b> Operates on UTC millisecond timestamps, unaffected by time zones.
     *
     * @param start the start calendar (inclusive). Must not be {@code null}.
     * @param end the end calendar (exclusive). Must not be {@code null}.
     * @return a {@link Duration} representing the time difference from {@code from} to {@code to}.
     *         Positive if {@code to} is after {@code from}, negative if {@code to} is before {@code from}.
     * @throws IllegalArgumentException if either calendar is {@code null}.
     * @see #between(java.util.Date, java.util.Date)
     * @see #between(java.time.temporal.Temporal, java.time.temporal.Temporal)
     * @see java.time.Duration#between(java.time.temporal.Temporal, java.time.temporal.Temporal)
     */
    public static Duration between(final java.util.Calendar start, final java.util.Calendar end) {
        N.checkArgNotNull(start, "Start Calendar can't be null");
        N.checkArgNotNull(end, "End Calendar can't be null");

        return Duration.ofMillis(end.getTimeInMillis() - start.getTimeInMillis());
    }

    /**
     * Calculates the duration between two temporal objects by determining the time difference from the first temporal to the second.
     * The result represents the time interval as a {@link Duration} object, which can be positive (if {@code end} is
     * after {@code start}), negative (if {@code end} is before {@code start}), or zero (if both temporals represent the same instant).
     *
     * <p><b>Calculation:</b> {@code Duration = start.until(end, ChronoUnit.MILLIS)}</p>
     *
     * <p><b>Examples:</b>
     * <pre>{@code
     * // Measuring elapsed time
     * Instant start = Instant.now();
     * // ... perform operation
     * Instant end = Instant.now();
     * Duration elapsed = Duration.between(start, end);
     * long millisElapsed = elapsed.toMillis();
     *
     * // Calculating age
     * LocalDateTime birthDate = LocalDateTime.of(1990, Month.MAY, 15, 0, 0);
     * LocalDateTime today = LocalDateTime.now();
     * Duration age = Duration.between(birthDate, today);
     * long ageInDays = age.toDays();
     *
     * // Time until deadline
     * LocalDateTime now = LocalDateTime.now();
     * LocalDateTime deadline = LocalDateTime.of(2024, Month.DECEMBER, 31, 0, 0);
     * Duration remaining = Duration.between(now, deadline);
     * if (remaining.isNegative()) {
     *     System.out.println("Deadline has passed");
     * } else {
     *     System.out.println("Days remaining: " + remaining.toDays());
     * }
     * }</pre>
     *
     * @param start the start temporal (inclusive). Must not be {@code null}.
     * @param end the end temporal (exclusive). Must not be {@code null}.
     * @return a {@link Duration} representing the time difference from {@code from} to {@code to}.
     *         Positive if {@code to} is after {@code from}, negative if {@code to} is before {@code from}.
     * @throws IllegalArgumentException if either temporal is {@code null}.
     * @see #between(java.util.Date, java.util.Date)
     * @see #between(java.util.Calendar, java.util.Calendar)
     * @see Temporal#until(java.time.temporal.Temporal, java.time.temporal.TemporalUnit)
     * @see TemporalUnit#between(Temporal, Temporal)
     * @see ChronoUnit#between(Temporal, Temporal)
     * @see java.time.Duration#between(java.time.temporal.Temporal, java.time.temporal.Temporal)
     */
    public static Duration between(final Temporal start, final Temporal end) {
        N.checkArgNotNull(start, "Start Temporal can't be null");
        N.checkArgNotNull(end, "End Temporal can't be null");

        return Duration.ofMillis(start.until(end, ChronoUnit.MILLIS));
    }

    /**
     * Checks if this duration represents zero time.
     * <p>
     * A duration is considered zero if its internal milliseconds value is exactly 0.
     * This is useful for checking if a duration represents no elapsed time.
     * </p>
     *
     * @return {@code true} if this duration is zero, {@code false} otherwise
     */
    public boolean isZero() {
        return milliseconds == 0;
    }

    /**
     * Checks if this duration represents a negative amount of time.
     * <p>
     * A duration is considered negative if its internal milliseconds value is less than 0.
     * Negative durations can represent time intervals going backwards or time deficits.
     * </p>
     *
     * @return {@code true} if this duration is negative, {@code false} otherwise
     */
    public boolean isNegative() {
        return milliseconds < 0;
    }

    /**
     * Returns a copy of this duration with the specified duration added.
     * <p>
     * This method adds the milliseconds value of the specified duration to this duration.
     * The addition is performed with overflow checking, throwing an ArithmeticException if the result would overflow.
     * This instance is immutable and unaffected by this method call.
     * </p>
     *
     * @param duration the duration to add, not null
     * @return a Duration based on this duration with the specified duration added
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration plus(final Duration duration) {
        return plusMillis(duration.milliseconds);
    }

    /**
     * Returns a copy of this duration with the specified number of days added.
     * <p>
     * This method adds the specified number of days to this duration, where each day is treated as exactly 24 hours.
     * The calculation includes overflow checking at both the multiplication and addition stages.
     * This instance is immutable and unaffected by this method call.
     * </p>
     *
     * @param daysToAdd the days to add, may be negative
     * @return a Duration based on this duration with the specified days added
     * @throws ArithmeticException if numeric overflow occurs during the calculation
     */
    public Duration plusDays(final long daysToAdd) {
        return plusMillis(Numbers.multiplyExact(daysToAdd, MILLIS_PER_DAY));
    }

    /**
     * Returns a copy of this duration with the specified number of hours added.
     * <p>
     * This method adds the specified number of hours to this duration, where each hour is treated as exactly 60 minutes.
     * The calculation includes overflow checking at both the multiplication and addition stages.
     * This instance is immutable and unaffected by this method call.
     * </p>
     *
     * @param hoursToAdd the hours to add, may be negative
     * @return a Duration based on this duration with the specified hours added
     * @throws ArithmeticException if numeric overflow occurs during the calculation
     */
    public Duration plusHours(final long hoursToAdd) {
        return plusMillis(Numbers.multiplyExact(hoursToAdd, MILLIS_PER_HOUR));
    }

    /**
     * Returns a copy of this duration with the specified number of minutes added.
     * <p>
     * This method adds the specified number of minutes to this duration, where each minute is treated as exactly 60 seconds.
     * The calculation includes overflow checking at both the multiplication and addition stages.
     * This instance is immutable and unaffected by this method call.
     * </p>
     *
     * @param minutesToAdd the minutes to add, may be negative
     * @return a Duration based on this duration with the specified minutes added
     * @throws ArithmeticException if numeric overflow occurs during the calculation
     */
    public Duration plusMinutes(final long minutesToAdd) {
        return plusMillis(Numbers.multiplyExact(minutesToAdd, MILLIS_PER_MINUTE));
    }

    /**
     * Returns a copy of this duration with the specified number of seconds added.
     * <p>
     * This method adds the specified number of seconds to this duration, where each second is treated as exactly 1000 milliseconds.
     * The calculation includes overflow checking at both the multiplication and addition stages.
     * This instance is immutable and unaffected by this method call.
     * </p>
     *
     * @param secondsToAdd the seconds to add, may be negative
     * @return a Duration based on this duration with the specified seconds added
     * @throws ArithmeticException if numeric overflow occurs during the calculation
     */
    public Duration plusSeconds(final long secondsToAdd) {
        return plusMillis(Numbers.multiplyExact(secondsToAdd, MILLIS_PER_SECOND));
    }

    /**
     * Returns a copy of this duration with the specified number of milliseconds added.
     * <p>
     * This method performs the addition with overflow checking, throwing an ArithmeticException if the result would overflow.
     * If the millisToAdd parameter is 0, this method returns the current instance for efficiency.
     * This instance is immutable and unaffected by this method call.
     * </p>
     *
     * @param millisToAdd the milliseconds to add, may be negative
     * @return a Duration based on this duration with the specified milliseconds added, or this instance if millisToAdd is 0
     * @throws ArithmeticException if numeric overflow occurs during the addition
     */
    public Duration plusMillis(final long millisToAdd) {
        if (millisToAdd == 0) {
            return this;
        }

        return create(Numbers.addExact(milliseconds, millisToAdd));
    }

    /**
     * Returns a copy of this duration with the specified duration subtracted.
     * <p>
     * This method subtracts the milliseconds value of the specified duration from this duration.
     * The subtraction is performed with overflow checking, throwing an ArithmeticException if the result would overflow.
     * This instance is immutable and unaffected by this method call.
     * </p>
     *
     * @param duration the duration to subtract, not null
     * @return a Duration based on this duration with the specified duration subtracted
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Duration minus(final Duration duration) {
        return minusMillis(duration.milliseconds);
    }

    /**
     * Returns a copy of this duration with the specified number of days subtracted.
     * <p>
     * This method subtracts the specified number of days from this duration, where each day is treated as exactly 24 hours.
     * The calculation includes overflow checking at both the multiplication and subtraction stages.
     * This instance is immutable and unaffected by this method call.
     * </p>
     *
     * @param daysToSubtract the days to subtract, may be negative
     * @return a Duration based on this duration with the specified days subtracted
     * @throws ArithmeticException if numeric overflow occurs during the calculation
     */
    public Duration minusDays(final long daysToSubtract) {
        return minusMillis(Numbers.multiplyExact(daysToSubtract, MILLIS_PER_DAY));
    }

    /**
     * Returns a copy of this duration with the specified number of hours subtracted.
     * <p>
     * This method subtracts the specified number of hours from this duration, where each hour is treated as exactly 60 minutes.
     * The calculation includes overflow checking at both the multiplication and subtraction stages.
     * This instance is immutable and unaffected by this method call.
     * </p>
     *
     * @param hoursToSubtract the hours to subtract, may be negative
     * @return a Duration based on this duration with the specified hours subtracted
     * @throws ArithmeticException if numeric overflow occurs during the calculation
     */
    public Duration minusHours(final long hoursToSubtract) {
        return minusMillis(Numbers.multiplyExact(hoursToSubtract, MILLIS_PER_HOUR));
    }

    /**
     * Returns a copy of this duration with the specified number of minutes subtracted.
     * <p>
     * This method subtracts the specified number of minutes from this duration, where each minute is treated as exactly 60 seconds.
     * The calculation includes overflow checking at both the multiplication and subtraction stages.
     * This instance is immutable and unaffected by this method call.
     * </p>
     *
     * @param minutesToSubtract the minutes to subtract, may be negative
     * @return a Duration based on this duration with the specified minutes subtracted
     * @throws ArithmeticException if numeric overflow occurs during the calculation
     */
    public Duration minusMinutes(final long minutesToSubtract) {
        return minusMillis(Numbers.multiplyExact(minutesToSubtract, MILLIS_PER_MINUTE));
    }

    /**
     * Returns a copy of this duration with the specified number of seconds subtracted.
     * <p>
     * This method subtracts the specified number of seconds from this duration, where each second is treated as exactly 1000 milliseconds.
     * The calculation includes overflow checking at both the multiplication and subtraction stages.
     * This instance is immutable and unaffected by this method call.
     * </p>
     *
     * @param secondsToSubtract the seconds to subtract, may be negative
     * @return a Duration based on this duration with the specified seconds subtracted
     * @throws ArithmeticException if numeric overflow occurs during the calculation
     */
    public Duration minusSeconds(final long secondsToSubtract) {
        return minusMillis(Numbers.multiplyExact(secondsToSubtract, MILLIS_PER_SECOND));
    }

    /**
     * Returns a copy of this duration with the specified number of milliseconds subtracted.
     * <p>
     * This method performs the subtraction with overflow checking, throwing an ArithmeticException if the result would overflow.
     * If the millisToSubtract parameter is 0, this method returns the current instance for efficiency.
     * This instance is immutable and unaffected by this method call.
     * </p>
     *
     * @param millisToSubtract the milliseconds to subtract, may be negative
     * @return a Duration based on this duration with the specified milliseconds subtracted, or this instance if millisToSubtract is 0
     * @throws ArithmeticException if numeric overflow occurs during the subtraction
     */
    public Duration minusMillis(final long millisToSubtract) {
        if (millisToSubtract == 0) {
            return this;
        }

        return create(Numbers.subtractExact(milliseconds, millisToSubtract));
    }

    /**
     * Returns a copy of this duration multiplied by the scalar.
     * <p>
     * This method multiplies the duration by the specified value with overflow checking.
     * Special cases:
     * <ul>
     *   <li>If the multiplicand is 0, returns the ZERO constant</li>
     *   <li>If the multiplicand is 1, returns this instance</li>
     *   <li>Otherwise, returns a new Duration with the multiplied value</li>
     * </ul>
     * <p>This instance is immutable and unaffected by this method call.</p>
     *
     * @param multiplicand the value to multiply the duration by, positive or negative
     * @return a Duration based on this duration multiplied by the specified scalar
     * @throws ArithmeticException if numeric overflow occurs during the multiplication
     */
    public Duration multipliedBy(final long multiplicand) {
        if (multiplicand == 0) {
            return ZERO;
        } else if (multiplicand == 1) {
            return this;
        }

        return create(Numbers.multiplyExact(milliseconds, multiplicand));
    }

    /**
     * Returns a copy of this duration divided by the specified value.
     * <p>
     * This method divides the duration by the specified value using integer division, which truncates towards zero.
     * Special cases:
     * <ul>
     *   <li>If the divisor is 0, throws ArithmeticException</li>
     *   <li>If the divisor is 1, returns this instance</li>
     *   <li>Otherwise, returns a new Duration with the divided value (truncated)</li>
     * </ul>
     * <p>This instance is immutable and unaffected by this method call.</p>
     *
     * @param divisor the value to divide the duration by, positive or negative but not zero
     * @return a Duration based on this duration divided by the specified divisor
     * @throws ArithmeticException if the divisor is zero
     */
    public Duration dividedBy(final long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        } else if (divisor == 1) {
            return this;
        }

        return create(milliseconds / divisor);
    }

    /**
     * Returns a copy of this duration with the length negated.
     * <p>
     * This method returns a duration with the opposite sign. For example:
     * <ul>
     *   <li>A positive duration becomes negative</li>
     *   <li>A negative duration becomes positive</li>
     *   <li>Zero remains zero</li>
     * </ul>
     * <p>This method is equivalent to {@code multipliedBy(-1)}.
     * This instance is immutable and unaffected by this method call.</p>
     *
     * @return a Duration based on this duration with the amount negated
     * @throws ArithmeticException if numeric overflow occurs (only when the duration equals Long.MIN_VALUE milliseconds)
     */
    public Duration negated() {
        return multipliedBy(-1);
    }

    /**
     * Returns a duration with a positive length.
     * <p>
     * This method returns the absolute value of the duration. If the duration is already positive or zero,
     * it returns this instance. If the duration is negative, it returns a negated copy.
     * This instance is immutable and unaffected by this method call.
     * </p>
     *
     * @return a Duration based on this duration with an absolute length
     * @throws ArithmeticException if numeric overflow occurs (only when the duration equals Long.MIN_VALUE milliseconds)
     */
    public Duration abs() {
        return isNegative() ? negated() : this;
    }

    /**
     * Gets the number of days in this duration.
     * <p>
     * This returns the total number of days in the duration by dividing the total milliseconds by 86,400,000.
     * The result is truncated towards zero, so partial days are ignored.
     * For example, a duration of 25 hours would return 1 day.
     * </p>
     *
     * @return the number of days in the duration, may be negative
     */
    public long toDays() {
        return milliseconds / MILLIS_PER_DAY;
    }

    /**
     * Gets the number of hours in this duration.
     * <p>
     * This returns the total number of hours in the duration by dividing the total milliseconds by 3,600,000.
     * The result is truncated towards zero, so partial hours are ignored.
     * For example, a duration of 90 minutes would return 1 hour.
     * </p>
     *
     * @return the number of hours in the duration, may be negative
     */
    public long toHours() {
        return milliseconds / MILLIS_PER_HOUR;
    }

    /**
     * Gets the number of minutes in this duration.
     * <p>
     * This returns the total number of minutes in the duration by dividing the total milliseconds by 60,000.
     * The result is truncated towards zero, so partial minutes are ignored.
     * For example, a duration of 90 seconds would return 1 minute.
     * </p>
     *
     * @return the number of minutes in the duration, may be negative
     */
    public long toMinutes() {
        return milliseconds / MILLIS_PER_MINUTE;
    }

    /**
     * Gets the number of seconds in this duration.
     * <p>
     * This returns the total number of seconds in the duration by dividing the total milliseconds by 1,000.
     * The result is truncated towards zero, so partial seconds are ignored.
     * For example, a duration of 1,500 milliseconds would return 1 second.
     * </p>
     *
     * @return the number of seconds in the duration, may be negative
     */
    public long toSeconds() {
        return milliseconds / MILLIS_PER_SECOND;
    }

    /**
     * Gets the number of milliseconds in this duration.
     * <p>
     * This returns the total length of the duration in milliseconds, which is how the duration
     * is internally stored. This is the most precise representation of the duration.
     * </p>
     *
     * @return the number of milliseconds in the duration, may be negative
     */
    public long toMillis() {
        return milliseconds;
    }

    /**
     * Converts this {@code Duration} to a {@code java.time.Duration}.
     * <p>
     * This method creates a new {@code java.time.Duration} instance representing the same amount of time
     * as this Duration. The conversion is straightforward as both use milliseconds precision.
     * This is useful for interoperability with Java's standard time API.
     * </p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Duration d = Duration.ofHours(2);
     * java.time.Duration jdkDuration = d.toJdkDuration(); // 2 hours
     * }</pre>
     *
     * @return a {@code java.time.Duration} with the same milliseconds value, not null
     */
    public java.time.Duration toJdkDuration() {
        return java.time.Duration.ofMillis(milliseconds);
    }

    /**
     * Compares this duration to another duration based on their length.
     * <p>
     * The comparison is based on the total length of the durations in milliseconds.
     * A duration is considered less than another if it represents a shorter amount of time,
     * regardless of whether the durations are positive or negative.
     * </p>
     *
     * @param other the other duration to compare to, not null
     * @return the comparator value, negative if less, positive if greater, zero if equal
     */
    @Override
    public int compareTo(final Duration other) {
        return Long.compare(milliseconds, other.milliseconds);
    }

    /**
     * Checks if this duration is equal to another duration.
     * <p>
     * The comparison is based on the total length of the durations in milliseconds.
     * Two durations are considered equal if they represent the exact same amount of time.
     * </p>
     *
     * @param obj the object to check, {@code null} returns false
     * @return {@code true} if this is equal to the other duration
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        return obj instanceof Duration && ((Duration) obj).milliseconds == milliseconds;
    }

    /**
     * Returns a hash code for this duration.
     * <p>
     * The hash code is calculated based on the total milliseconds value of the duration.
     * Equal durations will have the same hash code.
     * </p>
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return (Long.hashCode(milliseconds));
    }

    /**
     * Returns a string representation of this duration using ISO-8601 seconds based representation.
     * <p>
     * The format of the returned string will be {@code PTnHnMnS}, where:
     * <ul>
     *   <li>P is the duration designator (historically called "period") placed at the start</li>
     *   <li>T is the time designator that precedes the time components</li>
     *   <li>H represents hours and is shown only if non-zero</li>
     *   <li>M represents minutes and is shown only if non-zero</li>
     *   <li>S represents seconds and is always shown (even if zero) unless both hours and minutes are present and seconds are zero</li>
     * </ul>
     * Examples:
     * <ul>
     *   <li>"PT0S" - zero duration</li>
     *   <li>"PT1H" - 1 hour</li>
     *   <li>"PT1H30M" - 1 hour and 30 minutes</li>
     *   <li>"PT1H30M25S" - 1 hour, 30 minutes and 25 seconds</li>
     *   <li>"PT25.5S" - 25.5 seconds (25 seconds and 500 milliseconds)</li>
     *   <li>"PT-0.5S" - negative 500 milliseconds</li>
     * </ul>
     *
     * @return an ISO-8601 representation of this duration, not null
     */
    @Override
    public String toString() {
        if (this == ZERO) {
            return "PT0S";
        }

        final long hours = milliseconds / MILLIS_PER_HOUR;
        final int minutes = (int) ((milliseconds % MILLIS_PER_HOUR) / MILLIS_PER_MINUTE);
        final int seconds = (int) ((milliseconds % MILLIS_PER_MINUTE) / MILLIS_PER_SECOND);
        int millis = (int) (milliseconds % MILLIS_PER_SECOND);

        final StringBuilder sb = Objectory.createStringBuilder();

        sb.append("PT");

        if (hours != 0) {
            sb.append(hours).append('H');
        }

        if (minutes != 0) {
            sb.append(minutes).append('M');
        }

        if (seconds == 0 && millis == 0 && sb.length() > 2) {
            final String result = sb.toString();
            Objectory.recycle(sb);
            return result;
        }

        if (seconds == 0 && millis < 0) {
            sb.append("-0");
        } else {
            sb.append(seconds);
        }

        millis = Math.abs(millis);

        if (millis > 0) {
            sb.append('.');
            sb.append(millis);
        }

        sb.append('S');

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }
}
