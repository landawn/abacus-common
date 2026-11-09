package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ToShortFunctionTest extends TestBase {

    @Test
    public void testFromNum() {
        assertEquals((short) 0, ToShortFunction.FROM_NUM.applyAsShort(null));
        assertEquals((short) 100, ToShortFunction.FROM_NUM.applyAsShort(100));
        assertEquals((short) 7, ToShortFunction.FROM_NUM.applyAsShort(7.9d));
    }
}
