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

public class FractionType extends AbstractType<Fraction> {

    public static final String FRACTION = Fraction.class.getSimpleName();

    FractionType() {
        super(FRACTION);
    }

    @Override
    public Class<Fraction> clazz() {
        return Fraction.class;
    }

    /**
     * Checks if is number.
     *
     * @return {@code true}, if is number
     */
    @Override
    public boolean isNumber() {
        return true;
    }

    /**
     * Checks if is immutable.
     *
     * @return {@code true}, if is immutable
     */
    @Override
    public boolean isImmutable() {
        return true;
    }

    /**
     * Checks if is comparable.
     *
     * @return {@code true}, if is comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Checks if is non quoted csv type.
     *
     * @return {@code true}, if is non quoted csv type
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final Fraction x) {
        return x == null ? null : x.toString();
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public Fraction valueOf(final String str) {
        return Strings.isEmpty(str) ? null : Fraction.of(str);
    }
}
