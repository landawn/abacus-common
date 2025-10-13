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
 * An enumeration representing binary yes/no values.
 * Each value is associated with an integer: NO (0) and YES (1).
 * This enum provides a type-safe way to work with boolean-like values
 * that need numeric representation, such as database flags or configuration values.
 * 
 * <p>This enum is particularly useful when:</p>
 * <ul>
 *   <li>Interfacing with databases that store boolean values as integers</li>
 *   <li>Working with legacy systems that use 0/1 for false/true</li>
 *   <li>Needing a more explicit representation than boolean for yes/no choices</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * YesNo answer = YesNo.YES;
 * int dbValue = answer.intValue(); // Returns 1
 * 
 * // Converting from database
 * int storedValue = resultSet.getInt("active");
 * YesNo isActive = YesNo.valueOf(storedValue);
 * }</pre>
 *
 * @since 0.8
 */
public enum YesNo {

    /**
     * Represents "No" or false, with integer value 0.
     */
    NO(0),

    /**
     * Represents "Yes" or true, with integer value 1.
     */
    YES(1);

    private final int intValue;

    YesNo(final int intValue) {
        this.intValue = intValue;
    }

    /**
     * Returns the integer value associated with this YesNo constant.
     * NO returns 0, YES returns 1.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * YesNo consent = YesNo.YES;
     * int value = consent.intValue(); // Returns 1
     * 
     * // Storing in database
     * preparedStatement.setInt(1, userConsent.intValue());
     * }</pre>
     *
     * @return 0 for NO, 1 for YES
     */
    public int intValue() {
        return intValue;
    }

    /**
     * Returns the YesNo enum constant corresponding to the specified integer value.
     * This method provides a way to convert from numeric representation back to the enum.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Reading from database
     * int dbValue = resultSet.getInt("is_active");
     * YesNo isActive = YesNo.valueOf(dbValue);
     * 
     * if (isActive == YesNo.YES) {
     *     // Process active record
     * }
     * }</pre>
     *
     * @param intValue the integer value to convert (must be 0 or 1)
     * @return NO for 0, YES for 1
     * @throws IllegalArgumentException if the integer value is not 0 or 1
     */
    public static YesNo valueOf(final int intValue) {
        switch (intValue) {
            case 0:
                return NO;

            case 1:
                return YES;

            default:
                throw new IllegalArgumentException("No mapping instance found by int value: " + intValue);
        }
    }
}