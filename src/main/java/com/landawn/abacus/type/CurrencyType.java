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

package com.landawn.abacus.type;

import java.util.Currency;

import com.landawn.abacus.util.Strings;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class CurrencyType extends AbstractType<Currency> {

    public static final String CURRENCY = Currency.class.getSimpleName();

    CurrencyType() {
        super(CURRENCY);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<Currency> clazz() {
        return Currency.class;
    }

    /**
     * Checks if is immutable.
     *
     * @return true, if is immutable
     */
    @Override
    public boolean isImmutable() {
        return true;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final Currency x) {
        return x == null ? null : x.getCurrencyCode();
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public Currency valueOf(final String str) {
        return Strings.isEmpty(str) ? null : Currency.getInstance(str);
    }
}
