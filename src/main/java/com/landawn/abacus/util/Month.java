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
 * An enumeration representing the twelve months of the Gregorian calendar.
 * Each month is associated with its corresponding numeric value (1-12).
 * 
 * <p>This enum provides a convenient way to work with months in a type-safe manner,
 * avoiding the use of raw integer values which can be error-prone.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * Month month = Month.JANUARY;
 * int monthNumber = month.intValue(); // returns 1
 * 
 * Month decoded = Month.valueOf(3); // returns MARCH
 * }</pre>
 * 
 * @author HaiYang Li
 * @since 1.0
 */
public enum Month {

    JANUARY(1), FEBRUARY(2), MARCH(3), APRIL(4), MAY(5), JUNE(6), JULY(7), AUGUST(8), SEPTEMBER(9), OCTOBER(10), NOVEMBER(11), DECEMBER(12);

    private final int intValue;

    Month(final int intValue) {
        this.intValue = intValue;
    }

    /**
     * Returns the numeric value of this month.
     * January is 1, February is 2, and so on through December which is 12.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Month.MARCH.intValue(); // returns 3
     * Month.DECEMBER.intValue(); // returns 12
     * }</pre>
     * 
     * @return the numeric value of this month (1-12)
     */
    public int intValue() {
        return intValue;
    }

    /**
     * Returns the Month enum constant corresponding to the specified numeric value.
     * 
     * <p>This method provides a way to convert from numeric month values (1-12) to
     * the corresponding Month enum constant. This is useful when working with
     * legacy code or external systems that use numeric month representations.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Month jan = Month.valueOf(1); // returns JANUARY
     * Month dec = Month.valueOf(12); // returns DECEMBER
     * }</pre>
     * 
     * @param intValue the numeric value of the month (must be between 1 and 12 inclusive)
     * @return the Month enum constant corresponding to the specified value
     * @throws IllegalArgumentException if the intValue is not between 1 and 12 inclusive
     */
    public static Month valueOf(final int intValue) {
        switch (intValue) {
            case 1:
                return JANUARY;

            case 2:
                return FEBRUARY;

            case 3:
                return MARCH;

            case 4:
                return APRIL;

            case 5:
                return MAY;

            case 6:
                return JUNE;

            case 7:
                return JULY;

            case 8:
                return AUGUST;

            case 9:
                return SEPTEMBER;

            case 10:
                return OCTOBER;

            case 11:
                return NOVEMBER;

            case 12:
                return DECEMBER;

            default:
                throw new IllegalArgumentException("No mapping instance found by int value: " + intValue);
        }
    }
}