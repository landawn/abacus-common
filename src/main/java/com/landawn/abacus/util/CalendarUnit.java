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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * The Enum CalendarUnit.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public enum CalendarUnit {
    /**
     * it has the same int value as {@code Calendar.MILLISECOND}
     * 
     * @see Calendar#MILLISECOND
     */
    MILLISECOND(Calendar.MILLISECOND),
    /**
     * it has the same int value as {@code Calendar.SECOND}
     * 
     * @see Calendar#SECOND
     */
    SECOND(Calendar.SECOND),
    /**
     * it has the same int value as {@code Calendar.MINUTE}
     * 
     * @see Calendar#MINUTE
     */
    MINUTE(Calendar.MINUTE),
    /**
     * it has the same int value as {@code Calendar.HOUR_OF_DAY}
     * 
     * @see Calendar#HOUR
     */
    HOUR(Calendar.HOUR_OF_DAY),
    /**
     * it has the same int value as {@code Calendar.DAY_OF_MONTH}
     * 
     * @see Calendar#DAY_OF_MONTH
     */
    DAY(Calendar.DAY_OF_MONTH),
    /**
     * it has the same int value as {@code Calendar.WEEK_OF_YEAR}
     * 
     * @see Calendar#WEEK_OF_YEAR
     */
    WEEK(Calendar.WEEK_OF_YEAR),
    /**
     * it has the same int value as {@code Calendar.MONTH}
     * 
     * @see Calendar#MONTH
     */
    MONTH(Calendar.MONTH),
    /**
     * it has the same int value as {@code Calendar.YEAR}
     * 
     * @see Calendar#YEAR
     */
    YEAR(Calendar.YEAR);

    /** The m. */
    private static Map<Integer, CalendarUnit> m = new HashMap<Integer, CalendarUnit>();

    static {
        for (CalendarUnit value : CalendarUnit.values()) {
            m.put(value.intValue, value);
        }
    }

    /** The int value. */
    private int intValue;

    /**
     * Instantiates a new calendar unit.
     *
     * @param intValue
     */
    CalendarUnit(int intValue) {
        this.intValue = intValue;
    }

    /**
     *
     * @param intValue
     * @return
     */
    public static CalendarUnit valueOf(int intValue) {
        CalendarUnit result = m.get(intValue);

        if (result == null) {
            throw new IllegalArgumentException("No defined CalendarUnit mapping to value: " + intValue);
        }

        return result;
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
     * @param amount
     * @return
     */
    public long toMillis(long amount) {
        switch (this) {
            case MILLISECOND:
                return amount;

            case SECOND:
                return amount * 1000L;

            case MINUTE:
                return amount * 60 * 1000L;

            case HOUR:
                return amount * 60 * 60 * 1000L;

            case DAY:
                return amount * 24 * 60 * 60 * 1000L;

            case WEEK:
                return amount * 7 * 24 * 60 * 60 * 1000L;

            default:
                throw new IllegalArgumentException("Unsupported unit: " + this);
        }
    }
}
