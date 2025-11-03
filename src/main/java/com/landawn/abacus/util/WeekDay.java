/*
 * Copyright (C) 2015 HaiYang Li
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
 * An enumeration representing the days of the week.
 * Each day is associated with an integer value from 0 (Sunday) to 6 (Saturday).
 * This enum provides a convenient way to work with weekdays in a type-safe manner.
 * 
 * <p>The integer values correspond to the day-of-week values used in many calendar systems,
 * where Sunday is traditionally the first day of the week (value 0).</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * WeekDay today = WeekDay.MONDAY;
 * int dayValue = today.intValue(); // Returns 1
 * 
 * WeekDay sunday = WeekDay.valueOf(0); // Returns WeekDay.SUNDAY
 * }</pre>
 *
 */
public enum WeekDay {

    /**
     * Sunday, represented by integer value 0.
     */
    SUNDAY(0),

    /**
     * Monday, represented by integer value 1.
     */
    MONDAY(1),

    /**
     * Tuesday, represented by integer value 2.
     */
    TUESDAY(2),

    /**
     * Wednesday, represented by integer value 3.
     */
    WEDNESDAY(3),

    /**
     * Thursday, represented by integer value 4.
     */
    THURSDAY(4),

    /**
     * Friday, represented by integer value 5.
     */
    FRIDAY(5),

    /**
     * Saturday, represented by integer value 6.
     */
    SATURDAY(6);

    private final int intValue;

    WeekDay(final int intValue) {
        this.intValue = intValue;
    }

    /**
     * Returns the integer value associated with this weekday.
     * The values range from 0 (Sunday) to 6 (Saturday).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int mondayValue = WeekDay.MONDAY.intValue(); // Returns 1
     * int saturdayValue = WeekDay.SATURDAY.intValue(); // Returns 6
     * }</pre>
     *
     * @return the integer value of this weekday (0-6)
     */
    public int intValue() {
        return intValue;
    }

    /**
     * Returns the WeekDay enum constant corresponding to the specified integer value.
     * This method provides a way to convert from integer representation to the enum constant.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * WeekDay day = WeekDay.valueOf(1); // Returns WeekDay.MONDAY
     * WeekDay weekend = WeekDay.valueOf(6); // Returns WeekDay.SATURDAY
     * }</pre>
     *
     * @param intValue the integer value to convert (must be 0-6)
     * @return the WeekDay enum constant corresponding to the integer value
     * @throws IllegalArgumentException if the integer value is not in the range 0-6
     */
    public static WeekDay valueOf(final int intValue) {
        switch (intValue) {
            case 0:
                return SUNDAY;

            case 1:
                return MONDAY;

            case 2:
                return TUESDAY;

            case 3:
                return WEDNESDAY;

            case 4:
                return THURSDAY;

            case 5:
                return FRIDAY;

            case 6:
                return SATURDAY;

            default:
                throw new IllegalArgumentException("No mapping instance found by int value: " + intValue);
        }
    }
}
