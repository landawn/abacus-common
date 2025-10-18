/*
 * Copyright (C) 2025 HaiYang Li
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Currency;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CurrencyType2025Test extends TestBase {

    private final CurrencyType type = new CurrencyType();

    @Test
    public void test_clazz() {
        assertEquals(Currency.class, type.clazz());
    }

    @Test
    public void test_name() {
        assertEquals("Currency", type.name());
    }

    @Test
    public void test_isImmutable() {
        assertTrue(type.isImmutable());
    }

    @Test
    public void test_stringOf() {
        Currency usd = Currency.getInstance("USD");
        assertEquals("USD", type.stringOf(usd));

        Currency eur = Currency.getInstance("EUR");
        assertEquals("EUR", type.stringOf(eur));

        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        Currency usd = type.valueOf("USD");
        assertNotNull(usd);
        assertEquals("USD", usd.getCurrencyCode());

        Currency eur = type.valueOf("EUR");
        assertNotNull(eur);
        assertEquals("EUR", eur.getCurrencyCode());

        Currency jpy = type.valueOf("JPY");
        assertNotNull(jpy);
        assertEquals("JPY", jpy.getCurrencyCode());

        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_valueOf_String_InvalidCurrency() {
        assertThrows(IllegalArgumentException.class, () -> {
            type.valueOf("INVALID");
        });
    }

    @Test
    public void test_valueOf_String_CommonCurrencies() {
        assertNotNull(type.valueOf("USD"));
        assertNotNull(type.valueOf("EUR"));
        assertNotNull(type.valueOf("GBP"));
        assertNotNull(type.valueOf("JPY"));
        assertNotNull(type.valueOf("CNY"));
        assertNotNull(type.valueOf("CAD"));
        assertNotNull(type.valueOf("AUD"));
    }

    @Test
    public void test_defaultValue() {
        assertNull(type.defaultValue());
    }
}
