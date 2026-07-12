package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class LockModeTest extends AbstractTest {

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

    @Test
    public void testOf_withValidIntValues() {
        assertEquals(LockMode.R, LockMode.of(1));
        assertEquals(LockMode.A, LockMode.of(2));
        assertEquals(LockMode.U, LockMode.of(4));
        assertEquals(LockMode.D, LockMode.of(8));
        assertEquals(LockMode.RA, LockMode.of(3));
        assertEquals(LockMode.RU, LockMode.of(5));
        assertEquals(LockMode.RD, LockMode.of(9));
        assertEquals(LockMode.AU, LockMode.of(6));
        assertEquals(LockMode.AD, LockMode.of(10));
        assertEquals(LockMode.UD, LockMode.of(12));
        assertEquals(LockMode.RAU, LockMode.of(7));
        assertEquals(LockMode.RAD, LockMode.of(11));
        assertEquals(LockMode.RUD, LockMode.of(13));
        assertEquals(LockMode.AUD, LockMode.of(14));
        assertEquals(LockMode.RAUD, LockMode.of(15));
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
    public void testOf() {
        for (LockMode e : LockMode.values()) {
            assertEquals(e, LockMode.of(e.intValue()));
        }

        try {
            LockMode.of(-1000);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testOf_withInvalidIntValue() {
        assertThrows(IllegalArgumentException.class, () -> LockMode.of(0));
        assertThrows(IllegalArgumentException.class, () -> LockMode.of(16));
        assertThrows(IllegalArgumentException.class, () -> LockMode.of(-1));
        assertThrows(IllegalArgumentException.class, () -> LockMode.of(100));
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
    public void testIsXLockOf_singleLock() {
        assertTrue(LockMode.R.isXLockOf(LockMode.R));
        assertTrue(LockMode.U.isXLockOf(LockMode.U));
        assertTrue(LockMode.D.isXLockOf(LockMode.D));
        assertFalse(LockMode.R.isXLockOf(LockMode.U));
        assertFalse(LockMode.R.isXLockOf(LockMode.D));
        assertFalse(LockMode.U.isXLockOf(LockMode.D));
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

}
