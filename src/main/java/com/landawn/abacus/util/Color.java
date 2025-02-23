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
 * The Enum Color.
 *
 */
public enum Color {

    BLACK(0), WHITE(1), RED(2), ORANGE(3), YELLOW(4), GREEN(5), CYAN(6), BLUE(7), PURPLE(8);

    private final int intValue;

    Color(final int intValue) {
        this.intValue = intValue;
    }

    public int intValue() {
        return intValue;
    }

    /**
     *
     * @param intValue
     * @return
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
