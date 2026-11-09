package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ToFloatFunctionTest extends TestBase {

    @Test
    public void testFromNum() {
        assertEquals(0f, ToFloatFunction.FROM_NUM.applyAsFloat(null));
        assertEquals(2.5f, ToFloatFunction.FROM_NUM.applyAsFloat(2.5d));
        assertEquals(3f, ToFloatFunction.FROM_NUM.applyAsFloat(3));
    }
}
