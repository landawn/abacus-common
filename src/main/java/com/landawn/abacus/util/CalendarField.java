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
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Calendar cal = Calendar.getInstance();
 *
 * // Using CalendarField enum
 * cal.add(CalendarField.DAY_OF_MONTH.value(), 5); // Add 5 days
 * cal.set(CalendarField.HOUR_OF_DAY.value(), 14); // Set hour to 14 (2 PM)
 *
 * // Converting from int to CalendarField
 * CalendarField field = CalendarField.of(Calendar.MONTH);
 * System.out.println(field); // MONTH
 * }</pre>
 * 
 * @see java.util.Calendar
 */
public enum CalendarField {
    /**
     * Field for milliseconds within a second (0-999).
     * Has the same int value as {@link Calendar#MILLISECOND}.
     * 
     * <p><b>Usage Examples:</b></p>
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
     * <p><b>Usage Examples:</b></p>
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
     * <p><b>Usage Examples:</b></p>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * calendar.set(CalendarField.HOUR_OF_DAY.value(), 15); // Set to 3 PM
     * }</pre>
     *
     * @see Calendar#HOUR_OF_DAY
     */
    HOUR_OF_DAY(Calendar.HOUR_OF_DAY),

    /**
     * Field for day of the month (1-31).
     * Has the same int value (5) as {@link Calendar#DATE} and {@link Calendar#DAY_OF_MONTH}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * calendar.add(CalendarField.DAY_OF_MONTH.value(), 7); // Add one week
     * int dayOfMonth = calendar.get(CalendarField.DAY_OF_MONTH.value());
     * }</pre>
     *
     * @see Calendar#DAY_OF_MONTH
     */
    DAY_OF_MONTH(Calendar.DAY_OF_MONTH),

    /**
     * Field for week of the year (1-53).
     * Has the same int value as {@link Calendar#WEEK_OF_YEAR}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int weekNumber = calendar.get(CalendarField.WEEK_OF_YEAR.value());
     * calendar.add(CalendarField.WEEK_OF_YEAR.value(), 2); // Add 2 weeks
     * }</pre>
     *
     * @see Calendar#WEEK_OF_YEAR
     */
    WEEK_OF_YEAR(Calendar.WEEK_OF_YEAR),

    /**
     * Field for month of the year (0-11, where 0 is January).
     * Has the same int value as {@link Calendar#MONTH}.
     * 
     * <p><b>Usage Examples:</b></p>
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
     * <p><b>Usage Examples:</b></p>
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
     * the corresponding CalendarField enum values. It uses an internal lookup map
     * for efficient conversion.</p>
     *
     * <p>Supported field values include:
     * <ul>
     *   <li>{@link Calendar#MILLISECOND} (14) - maps to {@link #MILLISECOND}</li>
     *   <li>{@link Calendar#SECOND} (13) - maps to {@link #SECOND}</li>
     *   <li>{@link Calendar#MINUTE} (12) - maps to {@link #MINUTE}</li>
     *   <li>{@link Calendar#HOUR_OF_DAY} (11) - maps to {@link #HOUR_OF_DAY}</li>
     *   <li>{@link Calendar#DAY_OF_MONTH} (5) - maps to {@link #DAY_OF_MONTH}</li>
     *   <li>{@link Calendar#WEEK_OF_YEAR} (3) - maps to {@link #WEEK_OF_YEAR}</li>
     *   <li>{@link Calendar#MONTH} (2) - maps to {@link #MONTH}</li>
     *   <li>{@link Calendar#YEAR} (1) - maps to {@link #YEAR}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert Calendar constant to CalendarField
     * CalendarField field = CalendarField.of(Calendar.MONTH);
     * System.out.println(field); // Output: MONTH
     *
     * // Using numeric value directly
     * CalendarField dayField = CalendarField.of(5); // Calendar.DAY_OF_MONTH = 5
     * System.out.println(dayField); // Output: DAY_OF_MONTH
     *
     * // Dynamic conversion
     * Calendar cal = Calendar.getInstance();
     * int fieldConstant = Calendar.YEAR;
     * CalendarField yearField = CalendarField.of(fieldConstant);
     * }</pre>
     *
     * @param intValue the Calendar field constant value to convert
     * @return the corresponding CalendarField enum constant, never {@code null}
     * @throws IllegalArgumentException if no CalendarField maps to the given value
     *         (e.g., unsupported Calendar constants like {@link Calendar#HOUR},
     *         {@link Calendar#AM_PM}, {@link Calendar#DAY_OF_WEEK}, etc.)
     * @see Calendar
     * @see #value()
     * @see #valueOf(int)
     */
    public static CalendarField of(final int intValue) {
        final CalendarField result = m.get(intValue);

        if (result == null) {
            throw new IllegalArgumentException("No defined CalendarField mapping to value: " + intValue);
        }

        return result;
    }

    /**
     * Returns the CalendarField enum constant corresponding to the given Calendar field value.
     *
     * <p>This method is deprecated in favor of {@link #of(int)}, which follows
     * the standard factory method naming convention. The behavior is identical
     * to {@code of(int)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Deprecated way
     * CalendarField field = CalendarField.valueOf(Calendar.MONTH);
     *
     * // Preferred way
     * CalendarField field = CalendarField.of(Calendar.MONTH);
     * }</pre>
     *
     * @param intValue the Calendar field constant value to convert
     * @return the corresponding CalendarField enum constant, never {@code null}
     * @throws IllegalArgumentException if no CalendarField maps to the given value
     * @deprecated Use {@link #of(int)} instead for better naming consistency
     * @see #of(int)
     */
    @Deprecated
    public static CalendarField valueOf(final int intValue) {
        return of(intValue);
    }

    /**
     * Returns the Calendar field constant value for this CalendarField.
     *
     * <p>The returned value can be used directly with {@link Calendar} methods that
     * expect field constants, such as {@link Calendar#get(int)}, {@link Calendar#set(int, int)},
     * {@link Calendar#add(int, int)}, and {@link Calendar#roll(int, int)}.</p>
     *
     * <p>Each CalendarField enum constant maps to exactly one Calendar field constant:
     * <ul>
     *   <li>{@link #MILLISECOND}.value() returns {@link Calendar#MILLISECOND} (14)</li>
     *   <li>{@link #SECOND}.value() returns {@link Calendar#SECOND} (13)</li>
     *   <li>{@link #MINUTE}.value() returns {@link Calendar#MINUTE} (12)</li>
     *   <li>{@link #HOUR_OF_DAY}.value() returns {@link Calendar#HOUR_OF_DAY} (11)</li>
     *   <li>{@link #DAY_OF_MONTH}.value() returns {@link Calendar#DAY_OF_MONTH} (5)</li>
     *   <li>{@link #WEEK_OF_YEAR}.value() returns {@link Calendar#WEEK_OF_YEAR} (3)</li>
     *   <li>{@link #MONTH}.value() returns {@link Calendar#MONTH} (2)</li>
     *   <li>{@link #YEAR}.value() returns {@link Calendar#YEAR} (1)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * CalendarField monthField = CalendarField.MONTH;
     *
     * // Setting values - these are equivalent:
     * cal.set(monthField.value(), 5);        // Using CalendarField
     * cal.set(Calendar.MONTH, 5);            // Using Calendar constant directly
     *
     * // Getting values
     * int currentMonth = cal.get(CalendarField.MONTH.value());
     *
     * // Adding values
     * cal.add(CalendarField.DAY_OF_MONTH.value(), 7); // Add 7 days
     *
     * // Rolling values (wraps without changing larger fields)
     * cal.roll(CalendarField.HOUR_OF_DAY.value(), 3); // Add 3 hours (wraps at 24)
     * }</pre>
     *
     * @return the Calendar field constant value (an integer between 1 and 14)
     * @see Calendar#get(int)
     * @see Calendar#set(int, int)
     * @see Calendar#add(int, int)
     * @see Calendar#roll(int, int)
     * @see #of(int)
     */
    public int value() {
        return value;
    }
}
