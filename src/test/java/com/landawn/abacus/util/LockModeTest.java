/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class LockModeTest extends AbstractTest {

    @Test
    public void testValueOf() {
        for (LockMode e : LockMode.values()) {
            assertEquals(e, LockMode.valueOf(e.intValue()));
        }

        try {
            LockMode.valueOf(-1000);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }
}
