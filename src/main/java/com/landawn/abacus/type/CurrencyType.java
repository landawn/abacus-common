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

import java.util.Currency;

import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link Currency} values.
 * This class provides serialization and deserialization for {@link java.util.Currency} objects.
 *
 * <p>Currency instances are serialized to and from their ISO 4217 currency codes
 * (e.g., {@code "USD"}, {@code "EUR"}, {@code "JPY"}).</p>
 *
 * @see AbstractType
 * @see java.util.Currency
 */
public class CurrencyType extends AbstractType<Currency> {

    /** The type name constant for Currency type identification, equal to {@code "Currency"}. */
    public static final String CURRENCY = Currency.class.getSimpleName();

    /**
     * Package-private constructor for {@code CurrencyType}.
     * Instances are created by the {@code TypeFactory}.
     */
    CurrencyType() {
        super(CURRENCY);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code Currency.class}
     */
    @Override
    public Class<Currency> javaType() {
        return Currency.class;
    }

    /**
     * Indicates whether instances of this type are immutable.
     * {@link Currency} objects are immutable in Java.
     *
     * @return {@code true}, always, because {@link Currency} instances are immutable
     */
    @Override
    public boolean isImmutable() {
        return true;
    }

    /**
     * Converts a {@link Currency} to its ISO 4217 currency code string (e.g., {@code "USD"}).
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@link Currency} to convert; may be {@code null}
     * @return the ISO 4217 currency code, or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Currency x) {
        return x == null ? null : x.getCurrencyCode();
    }

    /**
     * Creates a {@link Currency} from its ISO 4217 currency code string (e.g., {@code "USD"}, {@code "EUR"}).
     * Delegates to {@link Currency#getInstance(String)}.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str a valid ISO 4217 currency code; may be {@code null} or empty
     * @return the corresponding {@link Currency} instance,
     *         or {@code null} if {@code str} is {@code null} or empty
     * @throws IllegalArgumentException if {@code str} is not a supported ISO 4217 currency code
     * @see #valueOf(Object)
     * @see #stringOf(Currency)
     */
    @Override
    public Currency valueOf(final String str) {
        return Strings.isEmpty(str) ? null : Currency.getInstance(str);
    }
}
