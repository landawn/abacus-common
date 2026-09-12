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

import java.io.IOException;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
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

    /** The type name constant for Fraction type identification, equal to {@code "Fraction"}. */
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
     * {@code "numerator/denominator"} (e.g., {@code "3/4"}).
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@link Fraction} to convert; may be {@code null}
     * @return the string representation of the fraction, or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
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
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse; may be {@code null} or empty. Leading and trailing whitespace is NOT
     *            trimmed: a space is the mixed-number separator, so {@code " 1/2"} fails to parse
     * @return the parsed {@link Fraction}, or {@code null} if {@code str} is {@code null} or empty
     * @throws NumberFormatException if the string is non-empty but cannot be parsed as a fraction
     * @throws ArithmeticException if the parsed fraction is invalid: a zero denominator, or terms outside the
     *         {@code int} range after normalization (for example {@code "1/0"} or {@code "1/-2147483648"})
     * @see #valueOf(Object)
     * @see #stringOf(Fraction)
     */
    @Override
    public Fraction valueOf(final String str) throws NumberFormatException, ArithmeticException {
        return Strings.isEmpty(str) ? null : Fraction.of(str);
    }

    /**
     * Serializes a fraction using its string form. A {@code null} fraction is serialized as if it were
     * {@link Fraction#ZERO} (that is, {@code "0/1"}, quoted exactly like any other fraction) when
     * {@code writeNullNumberAsZero} is enabled; otherwise it is written as {@code null}.
     *
     * <p>A fraction has no unquoted JSON number form - {@code 3/4} is not a JSON number - so the substituted zero
     * is written through the same quoting path as a non-null value. Emitting a bare {@code 0} instead would flip
     * the field between a JSON number and a JSON string depending on nullness.</p>
     *
     * @param writer the destination writer
     * @param x the fraction to serialize; may be {@code null}
     * @param config the serialization configuration; may be {@code null}
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     */
    @Override
    public void serializeTo(final CharacterWriter writer, Fraction x, final JsonXmlSerConfig<?> config) throws NullPointerException, IOException {
        x = x == null && config != null && config.isWriteNullNumberAsZero() ? Fraction.ZERO : x;

        super.serializeTo(writer, x, config);
    }
}
