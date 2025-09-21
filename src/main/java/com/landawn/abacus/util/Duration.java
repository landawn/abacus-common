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

/**
 * The reason to design/create this class is just to be a bit more efficient than {@code java.time.Duration}.
 * It seems that nanoseconds are only used in rare case.
 */
@com.landawn.abacus.annotation.Immutable
public final class Duration implements Comparable<Duration>, Immutable {

    public static final Duration ZERO = new Duration(0);

    private static final long MILLIS_PER_SECOND = 1000L;

    private static final long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60;

    private static final long MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60;

    private static final long MILLIS_PER_DAY = MILLIS_PER_HOUR * 24;

    private final long milliseconds;

    Duration(final long milliseconds) {
        this.milliseconds = milliseconds;
    }

    /**
     *
     * @param milliseconds
     * @return
     */
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
     * This instance is immutable and unaffected by this method call.
     * </p>
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
     * This instance is immutable and unaffected by this method call.
     * </p>
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
     * This method is equivalent to {@code multipliedBy(-1)}.
     * This instance is immutable and unaffected by this method call.
     * </p>
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
     * Converts this {@code Duration} to {@code java.time.Duration}.
     * 
     * @return a {@code java.time.Duration} with the same milliseconds.
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
     * @param obj the object to check, null returns false
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
     * </p>
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
            return sb.toString();
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
