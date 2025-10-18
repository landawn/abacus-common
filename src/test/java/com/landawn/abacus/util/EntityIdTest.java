/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.entity.extendDirty.basic.ExtendDirtyBasicPNL.AccountPNL;

public class EntityIdTest extends AbstractTest {

    @Test
    public void test_1() {
        Seid entityId = Seid.of(AccountPNL.ID, 1);
        assertEquals(1, entityId.get(AccountPNL.ID, int.class).intValue());

        entityId.set(AccountPNL.FIRST_NAME, "firstName");
        entityId.set(AccountPNL.LAST_NAME, "lastName");
        entityId.set(AccountPNL.BIRTH_DATE, Dates.currentDate());
        N.println(entityId);

        //    entityId.remove(Account.FIRST_NAME);
        //    entityId.remove(Account.LAST_NAME);

        entityId.set(CommonUtil.asProps(AccountPNL.ID, 2));

        entityId = Seid.of(AccountPNL.FIRST_NAME, "firstName", AccountPNL.LAST_NAME, "lastName");

        N.println(entityId);

    }
}
