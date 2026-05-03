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
 * Type handler for {@link Fraction} values.
 * This class provides serialization and deserialization capabilities for {@link Fraction} instances,
 * which represent rational numbers as a ratio of two integers.
 *
 * <p>Fractions are serialized to and from their string form via {@link Fraction#toString()} and
 * {@link Fraction#of(String)}, which supports decimal, fraction ({@code "Y/Z"}),
 * mixed-number ({@code "X Y/Z"}), and plain integer formats.
 *
 * @see AbstractType
 * @see Fraction
 */
public class FractionType extends AbstractType<Fraction> {

    /** The type name constant for Fraction type identification. */
    public static final String FRACTION = Fraction.class.getSimpleName();

    /**
     * Package-private constructor for FractionType.
     * This constructor is called by the TypeFactory to create Fraction type instances.
     */
    FractionType() {
        super(FRACTION);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code Fraction.class}
     */
    @Override
    public Class<Fraction> javaType() {
        return Fraction.class;
    }

    /**
     * Indicates whether this type represents a numeric value.
     * {@link Fraction} is a mathematical representation of a rational number.
     *
     * @return {@code true}, always, because {@link Fraction} represents a rational number
     */
    @Override
    public boolean isNumber() {
        return true;
    }

    /**
     * Indicates whether instances of this type are immutable.
     * {@link Fraction} objects are immutable once created.
     *
     * @return {@code true}, always, because {@link Fraction} instances are immutable
     */
    @Override
    public boolean isImmutable() {
        return true;
    }

    /**
     * Indicates whether instances of this type implement the {@link Comparable} interface.
     * {@link Fraction} implements {@code Comparable<Fraction>}, allowing fractions to be compared
     * and sorted.
     *
     * @return {@code true}, always, because {@link Fraction} implements {@link Comparable}
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Indicates whether values of this type require quoting in CSV format.
     * {@link Fraction} values are stored as numeric strings and do not need quotes.
     *
     * @return {@code false}, always, because fraction values are plain numbers in CSV
     */
    @Override
    public boolean isCsvQuoteRequired() {
        return false;
    }

    /**
     * Converts a {@link Fraction} to its string representation.
     * Uses {@link Fraction#toString()}, which produces a string in the form
     * {@code "numerator/denominator"} (e.g., {@code "3/4"}) or a plain integer if the
     * denominator is 1.
     *
     * @param x the {@link Fraction} to convert; may be {@code null}
     * @return the string representation of the fraction, or {@code null} if {@code x} is {@code null}
     */
    @Override
    public String stringOf(final Fraction x) {
        return x == null ? null : x.toString();
    }

    /**
     * Parses a string representation into a {@link Fraction} object.
     * Delegates to {@link Fraction#of(String)}, which accepts:
     * <ul>
     *   <li>Decimal format, e.g., {@code "0.5"}</li>
     *   <li>Fraction format {@code "Y/Z"}, e.g., {@code "3/4"}</li>
     *   <li>Mixed number format {@code "X Y/Z"}, e.g., {@code "1 1/2"}</li>
     *   <li>Integer format, e.g., {@code "5"}</li>
     * </ul>
     *
     * @param str the string to parse; may be {@code null} or empty
     * @return the parsed {@link Fraction}, or {@code null} if {@code str} is {@code null} or empty
     * @throws NumberFormatException if the string is non-empty but cannot be parsed as a fraction
     */
    @Override
    public Fraction valueOf(final String str) {
        return Strings.isEmpty(str) ? null : Fraction.of(str);
    }
}
