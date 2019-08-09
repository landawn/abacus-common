/*
 * Copyright (C) 2018, 2019 HaiYang Li
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

// TODO: Auto-generated Javadoc
/** 
 *
 */
public final class Duration implements Comparable<Duration> {

    /** The Constant ZERO. */
    public static final Duration ZERO = new Duration(0);

    /** The Constant MILLIS_PER_SECOND. */
    private static final long MILLIS_PER_SECOND = 1000L;

    /** The Constant MILLIS_PER_MINUTE. */
    private static final long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60;

    /** The Constant MILLIS_PER_HOUR. */
    private static final long MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60;

    /** The Constant MILLIS_PER_DAY. */
    private static final long MILLIS_PER_DAY = MILLIS_PER_HOUR * 24;

    /** The milliseconds. */
    private final long milliseconds;

    /**
     * Instantiates a new duration.
     *
     * @param milliseconds the milliseconds
     */
    Duration(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    /**
     * Creates the.
     *
     * @param milliseconds the milliseconds
     * @return the duration
     */
    private static Duration create(long milliseconds) {
        if (milliseconds == 0) {
            return ZERO;
        }

        return new Duration(milliseconds);
    }

    /**
     * Of days.
     *
     * @param days the days
     * @return the duration
     */
    public static Duration ofDays(long days) {
        return create(Matth.multiplyExact(days, MILLIS_PER_DAY));
    }

    /**
     * Of hours.
     *
     * @param hours the hours
     * @return the duration
     */
    public static Duration ofHours(long hours) {
        return create(Matth.multiplyExact(hours, MILLIS_PER_HOUR));
    }

    /**
     * Of minutes.
     *
     * @param minutes the minutes
     * @return the duration
     */
    public static Duration ofMinutes(long minutes) {
        return create(Matth.multiplyExact(minutes, MILLIS_PER_MINUTE));
    }

    /**
     * Of seconds.
     *
     * @param seconds the seconds
     * @return the duration
     */
    public static Duration ofSeconds(long seconds) {
        return create(Matth.multiplyExact(seconds, MILLIS_PER_SECOND));
    }

    /**
     * Of millis.
     *
     * @param millis the millis
     * @return the duration
     */
    public static Duration ofMillis(long millis) {
        return create(millis);
    }

    /**
     * Checks if is zero.
     *
     * @return true, if is zero
     */
    public boolean isZero() {
        return milliseconds == 0;
    }

    /**
     * Checks if is negative.
     *
     * @return true, if is negative
     */
    public boolean isNegative() {
        return milliseconds < 0;
    }

    /**
     * Plus.
     *
     * @param duration the duration
     * @return the duration
     */
    public Duration plus(Duration duration) {
        return plusMillis(duration.milliseconds);
    }

    /**
     * Plus days.
     *
     * @param daysToAdd the days to add
     * @return the duration
     */
    public Duration plusDays(long daysToAdd) {
        return plusMillis(Matth.multiplyExact(daysToAdd, MILLIS_PER_DAY));
    }

    /**
     * Plus hours.
     *
     * @param hoursToAdd the hours to add
     * @return the duration
     */
    public Duration plusHours(long hoursToAdd) {
        return plusMillis(Matth.multiplyExact(hoursToAdd, MILLIS_PER_HOUR));
    }

    /**
     * Plus minutes.
     *
     * @param minutesToAdd the minutes to add
     * @return the duration
     */
    public Duration plusMinutes(long minutesToAdd) {
        return plusMillis(Matth.multiplyExact(minutesToAdd, MILLIS_PER_MINUTE));
    }

    /**
     * Plus seconds.
     *
     * @param secondsToAdd the seconds to add
     * @return the duration
     */
    public Duration plusSeconds(long secondsToAdd) {
        return plusMillis(Matth.multiplyExact(secondsToAdd, MILLIS_PER_SECOND));
    }

    /**
     * Plus millis.
     *
     * @param millisToAdd the millis to add
     * @return the duration
     */
    public Duration plusMillis(long millisToAdd) {
        if (millisToAdd == 0) {
            return this;
        }

        return create(Matth.addExact(milliseconds, millisToAdd));
    }

    /**
     * Minus.
     *
     * @param duration the duration
     * @return the duration
     */
    public Duration minus(Duration duration) {
        return minusMillis(duration.milliseconds);
    }

    /**
     * Minus days.
     *
     * @param daysToSubtract the days to subtract
     * @return the duration
     */
    public Duration minusDays(long daysToSubtract) {
        return minusMillis(Matth.multiplyExact(daysToSubtract, MILLIS_PER_DAY));
    }

    /**
     * Minus hours.
     *
     * @param hoursToSubtract the hours to subtract
     * @return the duration
     */
    public Duration minusHours(long hoursToSubtract) {
        return minusMillis(Matth.multiplyExact(hoursToSubtract, MILLIS_PER_HOUR));
    }

    /**
     * Minus minutes.
     *
     * @param minutesToSubtract the minutes to subtract
     * @return the duration
     */
    public Duration minusMinutes(long minutesToSubtract) {
        return minusMillis(Matth.multiplyExact(minutesToSubtract, MILLIS_PER_MINUTE));
    }

    /**
     * Minus seconds.
     *
     * @param secondsToSubtract the seconds to subtract
     * @return the duration
     */
    public Duration minusSeconds(long secondsToSubtract) {
        return minusMillis(Matth.multiplyExact(secondsToSubtract, MILLIS_PER_SECOND));
    }

    /**
     * Minus millis.
     *
     * @param millisToSubtract the millis to subtract
     * @return the duration
     */
    public Duration minusMillis(long millisToSubtract) {
        if (millisToSubtract == 0) {
            return this;
        }

        return create(Matth.subtractExact(milliseconds, millisToSubtract));
    }

    /**
     * Multiplied by.
     *
     * @param multiplicand the multiplicand
     * @return the duration
     */
    public Duration multipliedBy(long multiplicand) {
        if (multiplicand == 0) {
            return ZERO;
        } else if (multiplicand == 1) {
            return this;
        }

        return create(Matth.multiplyExact(milliseconds, multiplicand));
    }

    /**
     * Divided by.
     *
     * @param divisor the divisor
     * @return the duration
     */
    public Duration dividedBy(long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        } else if (divisor == 1) {
            return this;
        }

        return create(milliseconds / divisor);
    }

    /**
     * Negated.
     *
     * @return the duration
     */
    public Duration negated() {
        return multipliedBy(-1);
    }

    /**
     * Abs.
     *
     * @return the duration
     */
    public Duration abs() {
        return isNegative() ? negated() : this;
    }

    /**
     * To days.
     *
     * @return the long
     */
    public long toDays() {
        return milliseconds / MILLIS_PER_DAY;
    }

    /**
     * To hours.
     *
     * @return the long
     */
    public long toHours() {
        return milliseconds / MILLIS_PER_HOUR;
    }

    /**
     * To minutes.
     *
     * @return the long
     */
    public long toMinutes() {
        return milliseconds / MILLIS_PER_MINUTE;
    }

    /**
     * To seconds.
     *
     * @return the long
     */
    public long toSeconds() {
        return milliseconds / MILLIS_PER_SECOND;
    }

    /**
     * To millis.
     *
     * @return the long
     */
    public long toMillis() {
        return milliseconds;
    }

    /**
     * Compare to.
     *
     * @param other the other
     * @return the int
     */
    @Override
    public int compareTo(Duration other) {
        return (milliseconds > other.milliseconds) ? 1 : ((milliseconds == other.milliseconds) ? 0 : -1);
    }

    /**
     * Equals.
     *
     * @param obj the obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        return obj instanceof Duration && ((Duration) obj).milliseconds == this.milliseconds;
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        return ((int) (milliseconds ^ (milliseconds >>> 32)));
    }

    /**
     * To string.
     *
     * @return the value as an ISO8601 string
     */
    @Override
    public String toString() {
        if (this == ZERO) {
            return "PT0S";
        }

        long hours = milliseconds / MILLIS_PER_HOUR;
        int minutes = (int) ((milliseconds % MILLIS_PER_HOUR) / MILLIS_PER_MINUTE);
        int seconds = (int) ((milliseconds % MILLIS_PER_MINUTE) / MILLIS_PER_SECOND);
        int millis = (int) ((milliseconds % MILLIS_PER_SECOND));

        final StringBuilder sb = Objectory.createStringBuilder(24);

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
