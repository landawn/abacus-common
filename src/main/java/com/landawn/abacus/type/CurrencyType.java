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
 * Type handler for Currency values.
 * This class provides serialization and deserialization for java.util.Currency objects.
 * Currencies are represented by their ISO 4217 currency codes (e.g., "USD", "EUR", "JPY").
 */
public class CurrencyType extends AbstractType<Currency> {

    public static final String CURRENCY = Currency.class.getSimpleName();

    CurrencyType() {
        super(CURRENCY);
    }

    /**
     * Returns the Java class type handled by this type handler.
     *
     * @return The Class object representing Currency.class
     */
    @Override
    public Class<Currency> clazz() {
        return Currency.class;
    }

    /**
     * Indicates whether instances of this type are immutable.
     * Currency objects are immutable in Java.
     *
     * @return {@code true}, indicating Currency objects are immutable
     */
    @Override
    public boolean isImmutable() {
        return true;
    }

    /**
     * Converts a Currency object to its string representation.
     * Uses the ISO 4217 currency code for serialization.
     *
     * @param x the Currency object to convert. Can be {@code null}.
     * @return The ISO 4217 currency code (e.g., "USD"), or {@code null} if input is null
     */
    @Override
    public String stringOf(final Currency x) {
        return x == null ? null : x.getCurrencyCode();
    }

    /**
     * Parses a string representation to create a Currency instance.
     * The string should be a valid ISO 4217 currency code.
     *
     * @param str the ISO 4217 currency code. Can be {@code null} or empty.
     * @return The Currency instance for the specified code, or {@code null} if input is null/empty
     * @throws IllegalArgumentException if the currency code is not a supported ISO 4217 code
     */
    @Override
    public Currency valueOf(final String str) {
        return Strings.isEmpty(str) ? null : Currency.getInstance(str);
    }
}
