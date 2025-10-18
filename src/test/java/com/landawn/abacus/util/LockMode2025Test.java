package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LockMode2025Test extends TestBase {

    @Test
    public void testIntValue() {
        assertEquals(1, LockMode.R.intValue());
        assertEquals(2, LockMode.A.intValue());
        assertEquals(4, LockMode.U.intValue());
        assertEquals(8, LockMode.D.intValue());
        assertEquals(3, LockMode.RA.intValue());
        assertEquals(5, LockMode.RU.intValue());
        assertEquals(9, LockMode.RD.intValue());
        assertEquals(6, LockMode.AU.intValue());
        assertEquals(10, LockMode.AD.intValue());
        assertEquals(12, LockMode.UD.intValue());
        assertEquals(7, LockMode.RAU.intValue());
        assertEquals(11, LockMode.RAD.intValue());
        assertEquals(13, LockMode.RUD.intValue());
        assertEquals(14, LockMode.AUD.intValue());
        assertEquals(15, LockMode.RAUD.intValue());
    }

    @Test
    public void testValueOf_withValidIntValues() {
        assertEquals(LockMode.R, LockMode.valueOf(1));
        assertEquals(LockMode.A, LockMode.valueOf(2));
        assertEquals(LockMode.U, LockMode.valueOf(4));
        assertEquals(LockMode.D, LockMode.valueOf(8));
        assertEquals(LockMode.RA, LockMode.valueOf(3));
        assertEquals(LockMode.RU, LockMode.valueOf(5));
        assertEquals(LockMode.RD, LockMode.valueOf(9));
        assertEquals(LockMode.AU, LockMode.valueOf(6));
        assertEquals(LockMode.AD, LockMode.valueOf(10));
        assertEquals(LockMode.UD, LockMode.valueOf(12));
        assertEquals(LockMode.RAU, LockMode.valueOf(7));
        assertEquals(LockMode.RAD, LockMode.valueOf(11));
        assertEquals(LockMode.RUD, LockMode.valueOf(13));
        assertEquals(LockMode.AUD, LockMode.valueOf(14));
        assertEquals(LockMode.RAUD, LockMode.valueOf(15));
    }

    @Test
    public void testValueOf_withInvalidIntValue() {
        assertThrows(IllegalArgumentException.class, () -> LockMode.valueOf(0));
        assertThrows(IllegalArgumentException.class, () -> LockMode.valueOf(16));
        assertThrows(IllegalArgumentException.class, () -> LockMode.valueOf(-1));
        assertThrows(IllegalArgumentException.class, () -> LockMode.valueOf(100));
    }

    @Test
    public void testValueOf_withStringName() {
        assertEquals(LockMode.R, LockMode.valueOf("R"));
        assertEquals(LockMode.U, LockMode.valueOf("U"));
        assertEquals(LockMode.D, LockMode.valueOf("D"));
        assertEquals(LockMode.RU, LockMode.valueOf("RU"));
        assertEquals(LockMode.UD, LockMode.valueOf("UD"));
        assertEquals(LockMode.RAUD, LockMode.valueOf("RAUD"));
    }

    @Test
    public void testIsXLockOf_singleLock() {
        assertTrue(LockMode.R.isXLockOf(LockMode.R));
        assertTrue(LockMode.U.isXLockOf(LockMode.U));
        assertTrue(LockMode.D.isXLockOf(LockMode.D));
        assertFalse(LockMode.R.isXLockOf(LockMode.U));
        assertFalse(LockMode.R.isXLockOf(LockMode.D));
        assertFalse(LockMode.U.isXLockOf(LockMode.D));
    }

    @Test
    public void testIsXLockOf_combinedLock() {
        assertTrue(LockMode.R.isXLockOf(LockMode.RU));
        assertTrue(LockMode.U.isXLockOf(LockMode.RU));
        assertTrue(LockMode.R.isXLockOf(LockMode.RD));
        assertTrue(LockMode.D.isXLockOf(LockMode.RD));
        assertTrue(LockMode.U.isXLockOf(LockMode.UD));
        assertTrue(LockMode.D.isXLockOf(LockMode.UD));
    }

    @Test
    public void testIsXLockOf_fullLock() {
        assertTrue(LockMode.R.isXLockOf(LockMode.RAUD));
        assertTrue(LockMode.A.isXLockOf(LockMode.RAUD));
        assertTrue(LockMode.U.isXLockOf(LockMode.RAUD));
        assertTrue(LockMode.D.isXLockOf(LockMode.RAUD));
        assertTrue(LockMode.RU.isXLockOf(LockMode.RAUD));
        assertTrue(LockMode.UD.isXLockOf(LockMode.RAUD));
    }

    @Test
    public void testIsXLockOf_notLocked() {
        assertFalse(LockMode.R.isXLockOf(LockMode.UD));
        assertFalse(LockMode.U.isXLockOf(LockMode.RD));
        assertFalse(LockMode.D.isXLockOf(LockMode.RU));
    }

    @Test
    public void testValues() {
        LockMode[] values = LockMode.values();
        assertEquals(15, values.length);
        assertEquals(LockMode.R, values[0]);
        assertEquals(LockMode.RAUD, values[values.length - 1]);
    }

    @Test
    public void testEnumName() {
        assertEquals("R", LockMode.R.name());
        assertEquals("A", LockMode.A.name());
        assertEquals("U", LockMode.U.name());
        assertEquals("D", LockMode.D.name());
        assertEquals("RU", LockMode.RU.name());
        assertEquals("RAUD", LockMode.RAUD.name());
    }

    @Test
    public void testEnumToString() {
        assertEquals("R", LockMode.R.toString());
        assertEquals("RU", LockMode.RU.toString());
        assertEquals("RAUD", LockMode.RAUD.toString());
    }

    @Test
    public void testBitmaskPattern() {
        // Test that the int values follow power of 2 pattern for basic locks
        assertEquals(1, LockMode.R.intValue());
        assertEquals(2, LockMode.A.intValue());
        assertEquals(4, LockMode.U.intValue());
        assertEquals(8, LockMode.D.intValue());

        // Test that combined locks are sums of their components
        assertEquals(LockMode.R.intValue() + LockMode.U.intValue(), LockMode.RU.intValue());
        assertEquals(LockMode.U.intValue() + LockMode.D.intValue(), LockMode.UD.intValue());
        assertEquals(LockMode.R.intValue() + LockMode.A.intValue() + LockMode.U.intValue() + LockMode.D.intValue(), LockMode.RAUD.intValue());
    }
}
