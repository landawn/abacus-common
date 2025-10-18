package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DoubleToLongFunction2025Test extends TestBase {
    @Test
    public void testApplyAsLong_Floor() {
        DoubleToLongFunction function = value -> (long) Math.floor(value);
        assertEquals(5L, function.applyAsLong(5.9));
        assertEquals(-6L, function.applyAsLong(-5.1));
    }

    @Test
    public void testApplyAsLong_Ceiling() {
        DoubleToLongFunction function = value -> (long) Math.ceil(value);
        assertEquals(6L, function.applyAsLong(5.1));
        assertEquals(-5L, function.applyAsLong(-5.9));
    }

    @Test
    public void testApplyAsLong_WithZero() {
        DoubleToLongFunction function = DoubleToLongFunction.DEFAULT;
        assertEquals(0L, function.applyAsLong(0.0));
        assertEquals(0L, function.applyAsLong(-0.0));
    }

    @Test
    public void testApplyAsLong_WithNegativeValues() {
        DoubleToLongFunction function = DoubleToLongFunction.DEFAULT;
        assertEquals(-5L, function.applyAsLong(-5.7));
        assertEquals(-10L, function.applyAsLong(-10.2));
    }

    @Test
    public void testApplyAsLong_WithLargeValues() {
        DoubleToLongFunction function = DoubleToLongFunction.DEFAULT;
        assertEquals(1000000L, function.applyAsLong(1000000.5));
        assertEquals(-1000000L, function.applyAsLong(-1000000.5));
    }

    @Test
    public void testJavaUtilFunctionCompatibility() {
        DoubleToLongFunction function = value -> (long) value;
        java.util.function.DoubleToLongFunction javaFunction = function;
        assertEquals(5L, javaFunction.applyAsLong(5.7));
    }

    @Test
    public void testDEFAULT_IsConstant() {
        DoubleToLongFunction default1 = DoubleToLongFunction.DEFAULT;
        DoubleToLongFunction default2 = DoubleToLongFunction.DEFAULT;
        assertSame(default1, default2);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        DoubleToLongFunction lambda = value -> (long) value;
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.applyAsLong(1.0));
    }
}
