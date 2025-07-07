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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration representing the various calendar fields used for date and time manipulation.
 * 
 * <p>This enum provides a type-safe way to work with calendar fields, mapping directly
 * to the corresponding {@link Calendar} field constants. Each enum value has the same
 * integer value as its corresponding Calendar constant, ensuring compatibility.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * Calendar cal = Calendar.getInstance();
 * 
 * // Using CalendarField enum
 * cal.add(CalendarField.DAY.value(), 5); // Add 5 days
 * cal.set(CalendarField.HOUR.value(), 14); // Set hour to 14 (2 PM)
 * 
 * // Converting from int to CalendarField
 * CalendarField field = CalendarField.of(Calendar.MONTH);
 * System.out.println(field); // MONTH
 * }</pre>
 * 
 * @see java.util.Calendar
 * @since 1.0
 */
public enum CalendarField {
    /**
     * Field for milliseconds within a second (0-999).
     * Has the same int value as {@link Calendar#MILLISECOND}.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * calendar.set(CalendarField.MILLISECOND.value(), 500); // Set to 500ms
     * }</pre>
     *
     * @see Calendar#MILLISECOND
     */
    MILLISECOND(Calendar.MILLISECOND),

    /**
     * Field for seconds within a minute (0-59).
     * Has the same int value as {@link Calendar#SECOND}.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * calendar.add(CalendarField.SECOND.value(), 30); // Add 30 seconds
     * }</pre>
     *
     * @see Calendar#SECOND
     */
    SECOND(Calendar.SECOND),

    /**
     * Field for minutes within an hour (0-59).
     * Has the same int value as {@link Calendar#MINUTE}.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * calendar.set(CalendarField.MINUTE.value(), 45); // Set to 45 minutes
     * }</pre>
     *
     * @see Calendar#MINUTE
     */
    MINUTE(Calendar.MINUTE),

    /**
     * Field for hour of the day in 24-hour format (0-23).
     * Has the same int value as {@link Calendar#HOUR_OF_DAY}.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * calendar.set(CalendarField.HOUR.value(), 15); // Set to 3 PM
     * }</pre>
     *
     * @see Calendar#HOUR_OF_DAY
     */
    HOUR(Calendar.HOUR_OF_DAY),

    /**
     * Field for day of the month (1-31).
     * Has the same int value (5) as {@link Calendar#DATE} and {@link Calendar#DAY_OF_MONTH}.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * calendar.add(CalendarField.DAY.value(), 7); // Add one week
     * int dayOfMonth = calendar.get(CalendarField.DAY.value());
     * }</pre>
     *
     * @see Calendar#DAY_OF_MONTH
     */
    DAY(Calendar.DAY_OF_MONTH),

    /**
     * Field for week of the year (1-53).
     * Has the same int value as {@link Calendar#WEEK_OF_YEAR}.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int weekNumber = calendar.get(CalendarField.WEEK.value());
     * calendar.add(CalendarField.WEEK.value(), 2); // Add 2 weeks
     * }</pre>
     *
     * @see Calendar#WEEK_OF_YEAR
     */
    WEEK(Calendar.WEEK_OF_YEAR),

    /**
     * Field for month of the year (0-11, where 0 is January).
     * Has the same int value as {@link Calendar#MONTH}.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * calendar.set(CalendarField.MONTH.value(), Calendar.DECEMBER);
     * calendar.add(CalendarField.MONTH.value(), 3); // Add 3 months
     * }</pre>
     *
     * @see Calendar#MONTH
     */
    MONTH(Calendar.MONTH),

    /**
     * Field for year.
     * Has the same int value as {@link Calendar#YEAR}.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * calendar.set(CalendarField.YEAR.value(), 2024);
     * calendar.add(CalendarField.YEAR.value(), 1); // Next year
     * }</pre>
     *
     * @see Calendar#YEAR
     */
    YEAR(Calendar.YEAR);

    private static final Map<Integer, CalendarField> m = new HashMap<>();

    static {
        for (final CalendarField value : CalendarField.values()) {
            m.put(value.value, value);
        }
    }

    private final int value;

    /**
     * Constructs a CalendarField with the specified Calendar constant value.
     *
     * @param value the corresponding Calendar field constant
     */
    CalendarField(final int value) {
        this.value = value;
    }

    /**
     * Returns the CalendarField enum constant corresponding to the given Calendar field value.
     * 
     * <p>This method provides a way to convert from Calendar field constants to
     * the corresponding CalendarField enum values.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * CalendarField field = CalendarField.of(Calendar.MONTH);
     * System.out.println(field); // MONTH
     * 
     * CalendarField dayField = CalendarField.of(5); // Calendar.DAY_OF_MONTH
     * System.out.println(dayField); // DAY
     * }</pre>
     *
     * @param intValue the Calendar field constant value
     * @return the corresponding CalendarField enum constant
     * @throws IllegalArgumentException if no CalendarField maps to the given value
     */
    public static CalendarField of(final int intValue) {
        final CalendarField result = m.get(intValue);

        if (result == null) {
            throw new IllegalArgumentException("No defined CalendarUnit mapping to value: " + intValue);
        }

        return result;
    }

    /**
     * Returns the CalendarField enum constant corresponding to the given Calendar field value.
     * 
     * @param intValue the Calendar field constant value
     * @return the corresponding CalendarField enum constant
     * @throws IllegalArgumentException if no CalendarField maps to the given value
     * @deprecated Use {@link #of(int)} instead
     */
    @Deprecated
    public static CalendarField valueOf(final int intValue) {
        return of(intValue);
    }

    /**
     * Returns the Calendar field constant value for this CalendarField.
     * 
     * <p>The returned value can be used directly with Calendar methods that
     * expect field constants.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * CalendarField monthField = CalendarField.MONTH;
     * 
     * // These are equivalent:
     * cal.set(monthField.value(), 5);
     * cal.set(Calendar.MONTH, 5);
     * 
     * // Getting current month
     * int currentMonth = cal.get(CalendarField.MONTH.value());
     * }</pre>
     * 
     * @return the Calendar field constant value
     */
    public int value() {
        return value;
    }
}