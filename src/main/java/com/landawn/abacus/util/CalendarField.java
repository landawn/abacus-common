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
 * <p><b>Note on {@code SEMI_MONTH}:</b> this enum intentionally has no {@code SEMI_MONTH} constant.
 * {@code SEMI_MONTH} ({@code Dates.SEMI_MONTH = 1001}) is an Apache-Commons-Lang-compatible extension
 * that has no backing {@link Calendar} field constant, so it cannot satisfy this enum's invariant that
 * every value mirrors a real {@code Calendar} field. To round/truncate/ceiling by the semi-month
 * boundary, use the {@code int}-field overloads of {@link Dates#round(java.util.Date, int)},
 * {@link Dates#truncate(java.util.Date, int)} and {@link Dates#ceiling(java.util.Date, int)} with
 * {@code Dates.SEMI_MONTH}; the {@code CalendarField} overloads of those methods deliberately cover the
 * standard fields only.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Calendar cal = Calendar.getInstance();
 *
 * // Using CalendarField enum
 * cal.add(CalendarField.DAY_OF_MONTH.value(), 5);   // adds 5 days
 * cal.set(CalendarField.HOUR_OF_DAY.value(), 14);   // hour is set to 14 (2 PM)
 *
 * // Converting from int to CalendarField
 * CalendarField field = CalendarField.of(Calendar.MONTH);
 * System.out.println(field);   // prints MONTH
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
     * calendar.set(CalendarField.MILLISECOND.value(), 500);   // milliseconds is set to 500
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
     * calendar.add(CalendarField.SECOND.value(), 30);   // adds 30 seconds
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
     * calendar.set(CalendarField.MINUTE.value(), 45);   // minutes is set to 45
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
     * calendar.set(CalendarField.HOUR_OF_DAY.value(), 15);   // hour-of-day is set to 15 (3 PM)
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
     * calendar.add(CalendarField.DAY_OF_MONTH.value(), 7);   // adds one week
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
     * calendar.add(CalendarField.WEEK_OF_YEAR.value(), 2);   // adds 2 weeks
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
     * calendar.add(CalendarField.MONTH.value(), 3);   // adds 3 months
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
     * calendar.add(CalendarField.YEAR.value(), 1);   // adds one year
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
     * <p>Supported field values include:</p>
     * <ul>
     *   <li>{@link Calendar#MILLISECOND} - maps to {@link #MILLISECOND}</li>
     *   <li>{@link Calendar#SECOND} - maps to {@link #SECOND}</li>
     *   <li>{@link Calendar#MINUTE} - maps to {@link #MINUTE}</li>
     *   <li>{@link Calendar#HOUR_OF_DAY} - maps to {@link #HOUR_OF_DAY}</li>
     *   <li>{@link Calendar#DAY_OF_MONTH} - maps to {@link #DAY_OF_MONTH}</li>
     *   <li>{@link Calendar#WEEK_OF_YEAR} - maps to {@link #WEEK_OF_YEAR}</li>
     *   <li>{@link Calendar#MONTH} - maps to {@link #MONTH}</li>
     *   <li>{@link Calendar#YEAR} - maps to {@link #YEAR}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert Calendar constant to CalendarField
     * CalendarField field = CalendarField.of(Calendar.MONTH);
     * System.out.println(field);   // prints MONTH
     *
     * // Using numeric value directly
     * CalendarField dayField = CalendarField.of(5);   // 5 is Calendar.DAY_OF_MONTH
     * System.out.println(dayField);                   // prints DAY_OF_MONTH
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
     * <p>Each {@code CalendarField} constant's {@code value()} returns the integer of
     * the corresponding {@link Calendar} field constant (e.g., {@link #MONTH}.value()
     * returns {@link Calendar#MONTH}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Calendar cal = Calendar.getInstance();
     * CalendarField monthField = CalendarField.MONTH;
     *
     * // Setting values - these are equivalent:
     * cal.set(monthField.value(), 5);   // uses CalendarField
     * cal.set(Calendar.MONTH, 5);       // uses Calendar constant directly
     *
     * // Getting values
     * int currentMonth = cal.get(CalendarField.MONTH.value());
     *
     * // Adding values
     * cal.add(CalendarField.DAY_OF_MONTH.value(), 7);   // adds 7 days
     *
     * // Rolling values (wraps without changing larger fields)
     * cal.roll(CalendarField.HOUR_OF_DAY.value(), 3);   // adds 3 hours (wraps at 24)
     * }</pre>
     *
     * @return the {@link Calendar} field constant integer corresponding to this enum constant
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
