package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class AccountStatus100Test extends TestBase {

    @Test
    public void testIntValue() {
        Assertions.assertEquals(0, AccountStatus.BLANK.code());
        Assertions.assertEquals(1, AccountStatus.ACTIVE.code());
        Assertions.assertEquals(2, AccountStatus.SUSPENDED.code());
        Assertions.assertEquals(3, AccountStatus.RETIRED.code());
        Assertions.assertEquals(4, AccountStatus.CLOSED.code());
        Assertions.assertEquals(5, AccountStatus.DELETED.code());
    }

    @Test
    public void testValueOf() {
        Assertions.assertEquals(AccountStatus.BLANK, AccountStatus.fromCode(0));
        Assertions.assertEquals(AccountStatus.ACTIVE, AccountStatus.fromCode(1));
        Assertions.assertEquals(AccountStatus.SUSPENDED, AccountStatus.fromCode(2));
        Assertions.assertEquals(AccountStatus.RETIRED, AccountStatus.fromCode(3));
        Assertions.assertEquals(AccountStatus.CLOSED, AccountStatus.fromCode(4));
        Assertions.assertEquals(AccountStatus.DELETED, AccountStatus.fromCode(5));
    }

    @Test
    public void testValueOfInvalid() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AccountStatus.fromCode(10);
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AccountStatus.fromCode(-1);
        });
    }

    @Test
    public void testEnumValues() {
        AccountStatus[] values = AccountStatus.values();
        Assertions.assertEquals(6, values.length);
        Assertions.assertEquals(AccountStatus.BLANK, values[0]);
        Assertions.assertEquals(AccountStatus.ACTIVE, values[1]);
        Assertions.assertEquals(AccountStatus.SUSPENDED, values[2]);
        Assertions.assertEquals(AccountStatus.RETIRED, values[3]);
        Assertions.assertEquals(AccountStatus.CLOSED, values[4]);
        Assertions.assertEquals(AccountStatus.DELETED, values[5]);
    }
}
