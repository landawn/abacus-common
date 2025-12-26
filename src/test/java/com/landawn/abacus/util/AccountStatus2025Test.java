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
        assertEquals(0, AccountStatus.BLANK.code());
        assertEquals(1, AccountStatus.ACTIVE.code());
        assertEquals(2, AccountStatus.SUSPENDED.code());
        assertEquals(3, AccountStatus.RETIRED.code());
        assertEquals(4, AccountStatus.CLOSED.code());
        assertEquals(5, AccountStatus.DELETED.code());
    }

    @Test
    public void testValueOf_withValidIntValues() {
        assertEquals(AccountStatus.BLANK, AccountStatus.fromCode(0));
        assertEquals(AccountStatus.ACTIVE, AccountStatus.fromCode(1));
        assertEquals(AccountStatus.SUSPENDED, AccountStatus.fromCode(2));
        assertEquals(AccountStatus.RETIRED, AccountStatus.fromCode(3));
        assertEquals(AccountStatus.CLOSED, AccountStatus.fromCode(4));
        assertEquals(AccountStatus.DELETED, AccountStatus.fromCode(5));
    }

    @Test
    public void testValueOf_withInvalidIntValue() {
        assertThrows(IllegalArgumentException.class, () -> AccountStatus.fromCode(-1));
        assertThrows(IllegalArgumentException.class, () -> AccountStatus.fromCode(6));
        assertThrows(IllegalArgumentException.class, () -> AccountStatus.fromCode(100));
    }

    @Test
    public void testValueOf_withStringName() {
        assertEquals(AccountStatus.BLANK, AccountStatus.valueOf("BLANK"));
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
        assertEquals(AccountStatus.BLANK, values[0]);
        assertEquals(AccountStatus.ACTIVE, values[1]);
        assertEquals(AccountStatus.SUSPENDED, values[2]);
        assertEquals(AccountStatus.RETIRED, values[3]);
        assertEquals(AccountStatus.CLOSED, values[4]);
        assertEquals(AccountStatus.DELETED, values[5]);
    }

    @Test
    public void testEnumName() {
        assertEquals("BLANK", AccountStatus.BLANK.name());
        assertEquals("ACTIVE", AccountStatus.ACTIVE.name());
        assertEquals("SUSPENDED", AccountStatus.SUSPENDED.name());
        assertEquals("RETIRED", AccountStatus.RETIRED.name());
        assertEquals("CLOSED", AccountStatus.CLOSED.name());
        assertEquals("DELETED", AccountStatus.DELETED.name());
    }

    @Test
    public void testEnumToString() {
        assertEquals("BLANK", AccountStatus.BLANK.toString());
        assertEquals("ACTIVE", AccountStatus.ACTIVE.toString());
        assertEquals("SUSPENDED", AccountStatus.SUSPENDED.toString());
        assertEquals("RETIRED", AccountStatus.RETIRED.toString());
        assertEquals("CLOSED", AccountStatus.CLOSED.toString());
        assertEquals("DELETED", AccountStatus.DELETED.toString());
    }
}
