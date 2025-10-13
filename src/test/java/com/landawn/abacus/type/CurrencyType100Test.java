package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class CurrencyType100Test extends TestBase {

    private CurrencyType currencyType;

    @BeforeEach
    public void setUp() {
        currencyType = (CurrencyType) createType(Currency.class.getSimpleName());
    }

    @Test
    public void testClazz() {
        assertEquals(Currency.class, currencyType.clazz());
    }

    @Test
    public void testIsImmutable() {
        assertTrue(currencyType.isImmutable());
    }

    @Test
    public void testStringOf() {
        Currency usd = Currency.getInstance("USD");
        assertEquals("USD", currencyType.stringOf(usd));

        Currency eur = Currency.getInstance("EUR");
        assertEquals("EUR", currencyType.stringOf(eur));

        assertNull(currencyType.stringOf(null));
    }

    @Test
    public void testValueOf() {
        Currency usd = currencyType.valueOf("USD");
        assertNotNull(usd);
        assertEquals("USD", usd.getCurrencyCode());

        Currency eur = currencyType.valueOf("EUR");
        assertNotNull(eur);
        assertEquals("EUR", eur.getCurrencyCode());

        assertNull(currencyType.valueOf(null));
        assertNull(currencyType.valueOf(""));

        assertThrows(IllegalArgumentException.class, () -> currencyType.valueOf("INVALID"));
    }
}
