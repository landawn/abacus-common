/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Calendar;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class CalendarUnitTest extends AbstractTest {

    @Test
    public void test1() {
        assertEquals(CalendarField.WEEK, CalendarField.of(Calendar.WEEK_OF_YEAR));

        try {
            CalendarField.of(Calendar.WEEK_OF_MONTH);
            fail("show throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }
}
