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
 * The Enum Color.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public enum Color {

    /** The black. */
    BLACK(0),
    /** The white. */
    WHITE(1),
    /** The red. */
    RED(2),
    /** The orange. */
    ORANGE(3),
    /** The yellow. */
    YELLOW(4),
    /** The green. */
    GREEN(5),
    /** The cyan. */
    CYAN(6),
    /** The blue. */
    BLUE(7),
    /** The purple. */
    PURPLE(8);

    /** The int value. */
    private int intValue;

    /**
     * Instantiates a new color.
     *
     * @param intValue
     */
    Color(int intValue) {
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
    public static Color valueOf(int intValue) {
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
