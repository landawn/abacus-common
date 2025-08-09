/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.entity.extendDirty.basic.Account;

public class EntityIdTest extends AbstractTest {

    @Test
    public void test_1() {
        Seid entityId = Seid.of(Account.ID, 1);
        assertEquals(1, entityId.get(Account.ID, int.class).intValue());

        entityId.set(Account.FIRST_NAME, "firstName");
        entityId.set(Account.LAST_NAME, "lastName");
        entityId.set(Account.BIRTH_DATE, Dates.currentDate());
        N.println(entityId);

        //    entityId.remove(Account.FIRST_NAME);
        //    entityId.remove(Account.LAST_NAME);

        entityId.set(N.asProps(Account.ID, 2));

        entityId = Seid.of(Account.FIRST_NAME, "firstName", Account.LAST_NAME, "lastName");

        N.println(entityId);

    }
}
