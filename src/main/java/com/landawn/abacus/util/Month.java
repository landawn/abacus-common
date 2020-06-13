/*
 * Copyright (C) 2015 HaiYang Li
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
 * The Enum Month.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public enum Month {

    /** The january. */
    JANUARY(1),
    /** The february. */
    FEBRUARY(2),
    /** The march. */
    MARCH(3),
    /** The april. */
    APRIL(4),
    /** The may. */
    MAY(5),
    /** The june. */
    JUNE(6),
    /** The july. */
    JULY(7),
    /** The august. */
    AUGUST(8),
    /** The september. */
    SEPTEMBER(9),
    /** The october. */
    OCTOBER(10),
    /** The november. */
    NOVEMBER(11),
    /** The december. */
    DECEMBER(12);

    /** The int value. */
    private int intValue;

    /**
     * Instantiates a new month.
     *
     * @param intValue
     */
    Month(int intValue) {
        this.intValue = intValue;
    }

    /**
     *
     * @return
     */
    public int intValue() {
        return intValue;
    }

    /**
     *
     * @param intValue
     * @return
     */
    public static Month valueOf(int intValue) {
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
