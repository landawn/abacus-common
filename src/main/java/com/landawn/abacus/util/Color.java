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
 * An enumeration representing common colors with their associated integer values.
 * This enum provides a mapping between color names and numeric codes, useful for
 * serialization, database storage, or systems that require numeric color representations.
 * 
 * <p>Each color is assigned a unique integer value from 0 to 8, allowing for
 * consistent numeric representation across different systems.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Color myColor = Color.RED;
 * int colorCode = myColor.intValue();   // returns 2
 * Color fromCode = Color.valueOf(2);   // returns Color.RED
 * }</pre>
 * 
 * @see WeekDay
 * @see Status
 * @see ServiceStatus
 * @see AccountStatus
 */
public enum Color {

    /**
     * The color black, represented by integer value 0.
     */
    BLACK(0),

    /**
     * The color white, represented by integer value 1.
     */
    WHITE(1),

    /**
     * The color red, represented by integer value 2.
     */
    RED(2),

    /**
     * The color orange, represented by integer value 3.
     */
    ORANGE(3),

    /**
     * The color yellow, represented by integer value 4.
     */
    YELLOW(4),

    /**
     * The color green, represented by integer value 5.
     */
    GREEN(5),

    /**
     * The color cyan, represented by integer value 6.
     */
    CYAN(6),

    /**
     * The color blue, represented by integer value 7.
     */
    BLUE(7),

    /**
     * The color purple, represented by integer value 8.
     */
    PURPLE(8);

    private final int intValue;

    Color(final int intValue) {
        this.intValue = intValue;
    }

    /**
     * Returns the integer value associated with this color.
     * This method is useful when you need to store or transmit colors as numeric values.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Color color = Color.BLUE;
     * int value = color.intValue();   // returns 7
     * }</pre>
     * 
     * @return the integer value representing this color (0-8)
     */
    public int intValue() {
        return intValue;
    }

    /**
     * Returns the Color enum constant corresponding to the specified integer value.
     * This method performs a reverse lookup from integer code to Color instance.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Color red = Color.valueOf(2);     // returns Color.RED
     * Color green = Color.valueOf(5);   // returns Color.GREEN
     * }</pre>
     * 
     * @param intValue the integer value to look up (must be between 0 and 8 inclusive)
     * @return the Color enum constant associated with the specified integer value
     * @throws IllegalArgumentException if the intValue is not between 0 and 8, or
     *         if no Color is mapped to the specified value
     */
    public static Color valueOf(final int intValue) {
        switch (intValue) {
            case 0:
                return BLACK;

            case 1:
                return WHITE;

            case 2:
                return RED;

            case 3:
                return ORANGE;

            case 4:
                return YELLOW;

            case 5:
                return GREEN;

            case 6:
                return CYAN;

            case 7:
                return BLUE;

            case 8:
                return PURPLE;

            default:
                throw new IllegalArgumentException("No mapping instance found by int value: " + intValue);
        }
    }
}
