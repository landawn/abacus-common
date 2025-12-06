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
 * Represents the days of the week as an enumeration with associated integer values.
 * This enum provides a mapping between day names and numeric representations (0-6),
 * where Sunday is 0 and Saturday is 6.
 * 
 * <p>This enum can be useful for calendar operations, scheduling systems, or any
 * application that needs to work with days of the week using numeric codes.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Get day from integer value
 * DayOfWeek monday = DayOfWeek.valueOf(1);   // Returns MONDAY
 * 
 * // Get integer value from day
 * int dayValue = DayOfWeek.FRIDAY.intValue();   // Returns 5
 * 
 * // Use in switch statements
 * DayOfWeek today = DayOfWeek.WEDNESDAY;
 * switch(today) {
 *     case MONDAY:
 *     case FRIDAY:
 *         System.out.println("Busy day!");
 *         break;
 *     case WEDNESDAY:
 *         System.out.println("Midweek!");
 *         break;
 *     default:
 *         System.out.println("Regular day");
 * }
 * 
 * // Iterate through all days
 * for (DayOfWeek day : DayOfWeek.values()) {
 *     System.out.println(day + " = " + day.intValue());
 * }
 * }</pre>
 */
public enum DayOfWeek {

    /** Represents Sunday (integer value: 0) */
    SUNDAY(0),

    /** Represents Monday (integer value: 1) */
    MONDAY(1),

    /** Represents Tuesday (integer value: 2) */
    TUESDAY(2),

    /** Represents Wednesday (integer value: 3) */
    WEDNESDAY(3),

    /** Represents Thursday (integer value: 4) */
    THURSDAY(4),

    /** Represents Friday (integer value: 5) */
    FRIDAY(5),

    /** Represents Saturday (integer value: 6) */
    SATURDAY(6);

    private final int intValue;

    /**
     * Constructs a DayOfWeek enum constant with the specified integer value.
     *
     * @param intValue the integer representation of this day (0-6)
     */
    DayOfWeek(final int intValue) {
        this.intValue = intValue;
    }

    /**
     * Returns the integer value associated with this day of the week.
     * This can be useful for database storage, array indexing, or integration
     * with systems that use numeric day codes.
     * 
     * <p>The mapping is:</p>
     * <ul>
     * <li>SUNDAY = 0</li>
     * <li>MONDAY = 1</li>
     * <li>TUESDAY = 2</li>
     * <li>WEDNESDAY = 3</li>
     * <li>THURSDAY = 4</li>
     * <li>FRIDAY = 5</li>
     * <li>SATURDAY = 6</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int fridayValue = DayOfWeek.FRIDAY.intValue();   // Returns 5
     * int sundayValue = DayOfWeek.SUNDAY.intValue();   // Returns 0
     * }</pre>
     *
     * @return the integer value of this day (0 for Sunday through 6 for Saturday)
     */
    public int intValue() {
        return intValue;
    }

    /**
     * Returns the DayOfWeek enum constant associated with the specified integer value.
     * This method provides a way to convert numeric day representations back to
     * DayOfWeek enum constants.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DayOfWeek monday = DayOfWeek.valueOf(1);     // Returns MONDAY
     * DayOfWeek sunday = DayOfWeek.valueOf(0);     // Returns SUNDAY
     * DayOfWeek saturday = DayOfWeek.valueOf(6);   // Returns SATURDAY
     * 
     * // Useful for database operations
     * int storedDay = resultSet.getInt("day_of_week");
     * DayOfWeek day = DayOfWeek.valueOf(storedDay);
     * }</pre>
     *
     * @param intValue the integer value to convert (must be 0-6)
     * @return the corresponding DayOfWeek enum constant
     * @throws IllegalArgumentException if intValue is not in the range 0-6
     */
    public static DayOfWeek valueOf(final int intValue) {
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
