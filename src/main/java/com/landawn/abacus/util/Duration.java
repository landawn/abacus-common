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

/** 
 *
 */
@com.landawn.abacus.annotation.Immutable
public final class Duration implements Comparable<Duration>, Immutable {

    public static final Duration ZERO = new Duration(0);

    private static final long MILLIS_PER_SECOND = 1000L;

    private static final long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60;

    private static final long MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60;

    private static final long MILLIS_PER_DAY = MILLIS_PER_HOUR * 24;

    private final long milliseconds;

    Duration(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    /**
     *
     * @param milliseconds
     * @return
     */
    private static Duration create(long milliseconds) {
        if (milliseconds == 0) {
            return ZERO;
        }

        return new Duration(milliseconds);
    }

    /**
     *
     * @param days
     * @return
     */
    public static Duration ofDays(long days) {
        return create(Numbers.multiplyExact(days, MILLIS_PER_DAY));
    }

    /**
     *
     * @param hours
     * @return
     */
    public static Duration ofHours(long hours) {
        return create(Numbers.multiplyExact(hours, MILLIS_PER_HOUR));
    }

    /**
     *
     * @param minutes
     * @return
     */
    public static Duration ofMinutes(long minutes) {
        return create(Numbers.multiplyExact(minutes, MILLIS_PER_MINUTE));
    }

    /**
     *
     * @param seconds
     * @return
     */
    public static Duration ofSeconds(long seconds) {
        return create(Numbers.multiplyExact(seconds, MILLIS_PER_SECOND));
    }

    /**
     *
     * @param millis
     * @return
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
     *
     * @param duration
     * @return
     */
    public Duration plus(Duration duration) {
        return plusMillis(duration.milliseconds);
    }

    /**
     *
     * @param daysToAdd
     * @return
     */
    public Duration plusDays(long daysToAdd) {
        return plusMillis(Numbers.multiplyExact(daysToAdd, MILLIS_PER_DAY));
    }

    /**
     *
     * @param hoursToAdd
     * @return
     */
    public Duration plusHours(long hoursToAdd) {
        return plusMillis(Numbers.multiplyExact(hoursToAdd, MILLIS_PER_HOUR));
    }

    /**
     *
     * @param minutesToAdd
     * @return
     */
    public Duration plusMinutes(long minutesToAdd) {
        return plusMillis(Numbers.multiplyExact(minutesToAdd, MILLIS_PER_MINUTE));
    }

    /**
     *
     * @param secondsToAdd
     * @return
     */
    public Duration plusSeconds(long secondsToAdd) {
        return plusMillis(Numbers.multiplyExact(secondsToAdd, MILLIS_PER_SECOND));
    }

    /**
     *
     * @param millisToAdd
     * @return
     */
    public Duration plusMillis(long millisToAdd) {
        if (millisToAdd == 0) {
            return this;
        }

        return create(Numbers.addExact(milliseconds, millisToAdd));
    }

    /**
     *
     * @param duration
     * @return
     */
    public Duration minus(Duration duration) {
        return minusMillis(duration.milliseconds);
    }

    /**
     *
     * @param daysToSubtract
     * @return
     */
    public Duration minusDays(long daysToSubtract) {
        return minusMillis(Numbers.multiplyExact(daysToSubtract, MILLIS_PER_DAY));
    }

    /**
     *
     * @param hoursToSubtract
     * @return
     */
    public Duration minusHours(long hoursToSubtract) {
        return minusMillis(Numbers.multiplyExact(hoursToSubtract, MILLIS_PER_HOUR));
    }

    /**
     *
     * @param minutesToSubtract
     * @return
     */
    public Duration minusMinutes(long minutesToSubtract) {
        return minusMillis(Numbers.multiplyExact(minutesToSubtract, MILLIS_PER_MINUTE));
    }

    /**
     *
     * @param secondsToSubtract
     * @return
     */
    public Duration minusSeconds(long secondsToSubtract) {
        return minusMillis(Numbers.multiplyExact(secondsToSubtract, MILLIS_PER_SECOND));
    }

    /**
     *
     * @param millisToSubtract
     * @return
     */
    public Duration minusMillis(long millisToSubtract) {
        if (millisToSubtract == 0) {
            return this;
        }

        return create(Numbers.subtractExact(milliseconds, millisToSubtract));
    }

    /**
     *
     * @param multiplicand
     * @return
     */
    public Duration multipliedBy(long multiplicand) {
        if (multiplicand == 0) {
            return ZERO;
        } else if (multiplicand == 1) {
            return this;
        }

        return create(Numbers.multiplyExact(milliseconds, multiplicand));
    }

    /**
     *
     * @param divisor
     * @return
     */
    public Duration dividedBy(long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        } else if (divisor == 1) {
            return this;
        }

        return create(milliseconds / divisor);
    }

    public Duration negated() {
        return multipliedBy(-1);
    }

    public Duration abs() {
        return isNegative() ? negated() : this;
    }

    public long toDays() {
        return milliseconds / MILLIS_PER_DAY;
    }

    public long toHours() {
        return milliseconds / MILLIS_PER_HOUR;
    }

    public long toMinutes() {
        return milliseconds / MILLIS_PER_MINUTE;
    }

    public long toSeconds() {
        return milliseconds / MILLIS_PER_SECOND;
    }

    public long toMillis() {
        return milliseconds;
    }

    /**
     *
     * @param other
     * @return
     */
    @Override
    public int compareTo(Duration other) {
        return (milliseconds > other.milliseconds) ? 1 : ((milliseconds == other.milliseconds) ? 0 : -1);
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        return obj instanceof Duration && ((Duration) obj).milliseconds == this.milliseconds;
    }

    @Override
    public int hashCode() {
        return ((int) (milliseconds ^ (milliseconds >>> 32)));
    }

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
