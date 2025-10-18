package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class AccountStatus2025Test extends TestBase {

    @Test
    public void testIntValue() {
        assertEquals(0, AccountStatus.DEFAULT.intValue());
        assertEquals(1, AccountStatus.ACTIVE.intValue());
        assertEquals(2, AccountStatus.SUSPENDED.intValue());
        assertEquals(3, AccountStatus.RETIRED.intValue());
        assertEquals(4, AccountStatus.CLOSED.intValue());
        assertEquals(5, AccountStatus.DELETED.intValue());
    }

    @Test
    public void testValueOf_withValidIntValues() {
        assertEquals(AccountStatus.DEFAULT, AccountStatus.valueOf(0));
        assertEquals(AccountStatus.ACTIVE, AccountStatus.valueOf(1));
        assertEquals(AccountStatus.SUSPENDED, AccountStatus.valueOf(2));
        assertEquals(AccountStatus.RETIRED, AccountStatus.valueOf(3));
        assertEquals(AccountStatus.CLOSED, AccountStatus.valueOf(4));
        assertEquals(AccountStatus.DELETED, AccountStatus.valueOf(5));
    }

    @Test
    public void testValueOf_withInvalidIntValue() {
        assertThrows(IllegalArgumentException.class, () -> AccountStatus.valueOf(-1));
        assertThrows(IllegalArgumentException.class, () -> AccountStatus.valueOf(6));
        assertThrows(IllegalArgumentException.class, () -> AccountStatus.valueOf(100));
    }

    @Test
    public void testValueOf_withStringName() {
        assertEquals(AccountStatus.DEFAULT, AccountStatus.valueOf("DEFAULT"));
        assertEquals(AccountStatus.ACTIVE, AccountStatus.valueOf("ACTIVE"));
        assertEquals(AccountStatus.SUSPENDED, AccountStatus.valueOf("SUSPENDED"));
        assertEquals(AccountStatus.RETIRED, AccountStatus.valueOf("RETIRED"));
        assertEquals(AccountStatus.CLOSED, AccountStatus.valueOf("CLOSED"));
        assertEquals(AccountStatus.DELETED, AccountStatus.valueOf("DELETED"));
    }

    @Test
    public void testValues() {
        AccountStatus[] values = AccountStatus.values();
        assertEquals(6, values.length);
        assertEquals(AccountStatus.DEFAULT, values[0]);
        assertEquals(AccountStatus.ACTIVE, values[1]);
        assertEquals(AccountStatus.SUSPENDED, values[2]);
        assertEquals(AccountStatus.RETIRED, values[3]);
        assertEquals(AccountStatus.CLOSED, values[4]);
        assertEquals(AccountStatus.DELETED, values[5]);
    }

    @Test
    public void testEnumName() {
        assertEquals("DEFAULT", AccountStatus.DEFAULT.name());
        assertEquals("ACTIVE", AccountStatus.ACTIVE.name());
        assertEquals("SUSPENDED", AccountStatus.SUSPENDED.name());
        assertEquals("RETIRED", AccountStatus.RETIRED.name());
        assertEquals("CLOSED", AccountStatus.CLOSED.name());
        assertEquals("DELETED", AccountStatus.DELETED.name());
    }

    @Test
    public void testEnumToString() {
        assertEquals("DEFAULT", AccountStatus.DEFAULT.toString());
        assertEquals("ACTIVE", AccountStatus.ACTIVE.toString());
        assertEquals("SUSPENDED", AccountStatus.SUSPENDED.toString());
        assertEquals("RETIRED", AccountStatus.RETIRED.toString());
        assertEquals("CLOSED", AccountStatus.CLOSED.toString());
        assertEquals("DELETED", AccountStatus.DELETED.toString());
    }
}
