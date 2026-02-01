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

package com.landawn.abacus.type;

import com.landawn.abacus.util.Fraction;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for Fraction objects.
 * This class provides serialization and deserialization capabilities for Fraction instances,
 * which represent rational numbers as a ratio of two integers.
 */
public class FractionType extends AbstractType<Fraction> {

    public static final String FRACTION = Fraction.class.getSimpleName();

    FractionType() {
        super(FRACTION);
    }

    /**
     * Returns the Class object representing the Fraction type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Fraction> type = TypeFactory.getType(Fraction.class);
     * Class<Fraction> clazz = type.clazz();
     * // Returns: Fraction.class
     * }</pre>
     *
     * @return Fraction.class
     */
    @Override
    public Class<Fraction> clazz() {
        return Fraction.class;
    }

    /**
     * Indicates whether this type represents a numeric value.
     * Fractions are mathematical representations of rational numbers, so this returns {@code true}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Fraction> type = TypeFactory.getType(Fraction.class);
     * boolean isNum = type.isNumber();
     * // Returns: true
     * }</pre>
     *
     * @return {@code true}, as Fraction represents numeric values
     */
    @Override
    public boolean isNumber() {
        return true;
    }

    /**
     * Indicates whether instances of this type are immutable.
     * Fraction objects are immutable once created, so this returns {@code true}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Fraction> type = TypeFactory.getType(Fraction.class);
     * boolean immutable = type.isImmutable();
     * // Returns: true
     * }</pre>
     *
     * @return {@code true}, as Fraction instances are immutable
     */
    @Override
    public boolean isImmutable() {
        return true;
    }

    /**
     * Indicates whether instances of this type implement the Comparable interface.
     * Fraction implements Comparable&lt;Fraction&gt;, allowing fractions to be compared and sorted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Fraction> type = TypeFactory.getType(Fraction.class);
     * boolean comparable = type.isComparable();
     * // Returns: true
     * }</pre>
     *
     * @return {@code true}, as Fraction implements Comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Indicates whether this type should be written without quotes in CSV format.
     * Numeric types like Fraction are typically not quoted in CSV files.
     *
     * @return {@code true}, indicating that Fraction values should not be quoted in CSV output
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     * Converts a Fraction object to its string representation.
     * The string format is determined by the Fraction's toString() method,
     * typically in the form "numerator/denominator".
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Fraction> type = TypeFactory.getType(Fraction.class);
     *
     * Fraction fraction = Fraction.of(3, 4);
     * String result = type.stringOf(fraction);
     * // Returns: "3/4"
     *
     * fraction = Fraction.of(5, 1);
     * result = type.stringOf(fraction);
     * // Returns: "5"
     *
     * result = type.stringOf(null);
     * // Returns: null
     * }</pre>
     *
     * @param x the Fraction to convert to string
     * @return the string representation of the fraction, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final Fraction x) {
        return x == null ? null : x.toString();
    }

    /**
     * Parses a string representation into a Fraction object.
     * The string should be in a format that can be parsed by Fraction.of(),
     * typically "numerator/denominator" or a decimal number.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Fraction> type = TypeFactory.getType(Fraction.class);
     *
     * Fraction result = type.valueOf("3/4");
     * // Returns: Fraction representing 3/4
     *
     * result = type.valueOf("0.5");
     * // Returns: Fraction representing 1/2
     *
     * result = type.valueOf("5");
     * // Returns: Fraction representing 5/1
     *
     * result = type.valueOf(null);
     * // Returns: null
     *
     * result = type.valueOf("");
     * // Returns: null
     * }</pre>
     *
     * @param str the string to parse into a Fraction
     * @return the parsed Fraction object, or {@code null} if the input string is {@code null} or empty
     */
    @Override
    public Fraction valueOf(final String str) {
        return Strings.isEmpty(str) ? null : Fraction.of(str);
    }
}