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
 */
public enum CalendarField {
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
     * it has the same int value (5) as {@code Calendar.DATE} and  {@code Calendar.DAY_OF_MONTH}
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

    private static Map<Integer, CalendarField> m = new HashMap<>();

    static {
        for (final CalendarField value : CalendarField.values()) {
            m.put(value.value, value);
        }
    }

    private final int value;

    CalendarField(final int value) {
        this.value = value;
    }

    /**
     *
     * @param intValue
     * @return
     */
    public static CalendarField valueOf(final int intValue) {
        final CalendarField result = m.get(intValue);

        if (result == null) {
            throw new IllegalArgumentException("No defined CalendarUnit mapping to value: " + intValue);
        }

        return result;
    }

    public int value() {
        return value;
    }
}
