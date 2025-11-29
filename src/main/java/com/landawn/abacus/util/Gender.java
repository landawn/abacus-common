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
 * Represents gender as an enumeration with associated integer values.
 * This enum provides three states: BLANK (unknown/unspecified), FEMALE, MALE and X.
 * 
 * <p>Each gender value has an associated integer representation that can be used 
 * for database storage or legacy system integration.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Get gender from integer value
 * Gender gender = Gender.valueOf(1);  // Returns FEMALE
 * 
 * // Get integer value from gender
 * int value = Gender.MALE.intValue();  // Returns 2
 * 
 * // Get integer value from gender
 * int value = Gender.X.intValue();  // Returns 3
 * 
 * // Use in switch statements
 * switch(gender) {
 *     case FEMALE:
 *         // Handle female case
 *         break;
 *     case MALE:
 *         // Handle male case
 *         break;
 *     case X:
 *         // Handle x case
 *         break;
 *     default:
 *         // Handle blank/unknown
 * }
 * }</pre>
 */
public enum Gender {

    /** Represents unknown or unspecified gender (integer value: 0) */
    BLANK(0),

    /** Represents female gender (integer value: 1) */
    FEMALE(1),

    /** Represents male gender (integer value: 2) */
    MALE(2),

    /** Represents X gender (integer value: 3) */
    X(3);

    private final int intValue;

    /**
     * Constructs a Gender enum constant with the specified integer value.
     *
     * @param intValue the integer representation of this gender
     */
    Gender(final int intValue) {
        this.intValue = intValue;
    }

    /**
     * Returns the integer value associated with this gender.
     * This can be useful for database storage or integration with systems
     * that use numeric gender codes.
     *
     * @return the integer value of this gender (0 for BLANK, 1 for FEMALE, 2 for MALE, 3 for X)
     */
    public int intValue() {
        return intValue;
    }

    /**
     * Returns the Gender enum constant associated with the specified integer value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Gender female = Gender.valueOf(1);   // Returns FEMALE
     * Gender male = Gender.valueOf(2);     // Returns MALE
     * Gender x = Gender.valueOf(3);        // Returns X
     * Gender blank = Gender.valueOf(0);    // Returns BLANK
     * }</pre>
     *
     * @param intValue the integer value to convert to Gender (0, 1, 2 or 3)
     * @return the corresponding Gender enum constant
     * @throws IllegalArgumentException if the intValue is not 0, 1, 2 or 3
     */
    public static Gender valueOf(final int intValue) {
        switch (intValue) {
            case 0:
                return BLANK;

            case 1:
                return FEMALE;

            case 2:
                return MALE;

            case 3:
                return X;

            default:
                throw new IllegalArgumentException("No mapping instance found by int value: " + intValue);
        }
    }
}
