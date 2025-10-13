package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class AccountStatus100Test extends TestBase {

    @Test
    public void testIntValue() {
        Assertions.assertEquals(0, AccountStatus.DEFAULT.intValue());
        Assertions.assertEquals(1, AccountStatus.ACTIVE.intValue());
        Assertions.assertEquals(2, AccountStatus.SUSPENDED.intValue());
        Assertions.assertEquals(3, AccountStatus.RETIRED.intValue());
        Assertions.assertEquals(4, AccountStatus.CLOSED.intValue());
        Assertions.assertEquals(5, AccountStatus.DELETED.intValue());
    }

    @Test
    public void testValueOf() {
        Assertions.assertEquals(AccountStatus.DEFAULT, AccountStatus.valueOf(0));
        Assertions.assertEquals(AccountStatus.ACTIVE, AccountStatus.valueOf(1));
        Assertions.assertEquals(AccountStatus.SUSPENDED, AccountStatus.valueOf(2));
        Assertions.assertEquals(AccountStatus.RETIRED, AccountStatus.valueOf(3));
        Assertions.assertEquals(AccountStatus.CLOSED, AccountStatus.valueOf(4));
        Assertions.assertEquals(AccountStatus.DELETED, AccountStatus.valueOf(5));
    }

    @Test
    public void testValueOfInvalid() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AccountStatus.valueOf(10);
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AccountStatus.valueOf(-1);
        });
    }

    @Test
    public void testEnumValues() {
        AccountStatus[] values = AccountStatus.values();
        Assertions.assertEquals(6, values.length);
        Assertions.assertEquals(AccountStatus.DEFAULT, values[0]);
        Assertions.assertEquals(AccountStatus.ACTIVE, values[1]);
        Assertions.assertEquals(AccountStatus.SUSPENDED, values[2]);
        Assertions.assertEquals(AccountStatus.RETIRED, values[3]);
        Assertions.assertEquals(AccountStatus.CLOSED, values[4]);
        Assertions.assertEquals(AccountStatus.DELETED, values[5]);
    }
}
