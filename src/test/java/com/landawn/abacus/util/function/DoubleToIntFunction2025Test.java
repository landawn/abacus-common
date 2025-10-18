package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DoubleToIntFunction2025Test extends TestBase {

    @Test
    public void testApplyAsInt() {
        DoubleToIntFunction function = value -> (int) Math.round(value);
        int result = function.applyAsInt(5.7);
        assertEquals(6, result);
    }

    @Test
    public void testApplyAsInt_WithAnonymousClass() {
        DoubleToIntFunction function = new DoubleToIntFunction() {
            @Override
            public int applyAsInt(double value) {
                return (int) Math.ceil(value);
            }
        };

        int result = function.applyAsInt(5.3);
        assertEquals(6, result);
    }

    @Test
    public void testDEFAULT() {
        DoubleToIntFunction function = DoubleToIntFunction.DEFAULT;
        assertEquals(5, function.applyAsInt(5.7));
        assertEquals(-5, function.applyAsInt(-5.7));
        assertEquals(0, function.applyAsInt(0.9));
    }

    @Test
    public void testApplyAsInt_Truncation() {
        DoubleToIntFunction function = value -> (int) value;
        assertEquals(5, function.applyAsInt(5.9));
        assertEquals(-5, function.applyAsInt(-5.9));
    }

    @Test
    public void testApplyAsInt_Rounding() {
        DoubleToIntFunction function = value -> (int) Math.round(value);
        assertEquals(6, function.applyAsInt(5.5));
        assertEquals(5, function.applyAsInt(5.4));
        assertEquals(-5, function.applyAsInt(-5.5));
    }

    @Test
    public void testApplyAsInt_Floor() {
        DoubleToIntFunction function = value -> (int) Math.floor(value);
        assertEquals(5, function.applyAsInt(5.9));
        assertEquals(-6, function.applyAsInt(-5.1));
    }

    @Test
    public void testApplyAsInt_Ceiling() {
        DoubleToIntFunction function = value -> (int) Math.ceil(value);
        assertEquals(6, function.applyAsInt(5.1));
        assertEquals(-5, function.applyAsInt(-5.9));
    }

    @Test
    public void testApplyAsInt_WithZero() {
        DoubleToIntFunction function = DoubleToIntFunction.DEFAULT;
        assertEquals(0, function.applyAsInt(0.0));
        assertEquals(0, function.applyAsInt(-0.0));
    }

    @Test
    public void testApplyAsInt_WithNegativeValues() {
        DoubleToIntFunction function = DoubleToIntFunction.DEFAULT;
        assertEquals(-5, function.applyAsInt(-5.7));
        assertEquals(-10, function.applyAsInt(-10.2));
    }

    @Test
    public void testJavaUtilFunctionCompatibility() {
        DoubleToIntFunction function = value -> (int) value;
        java.util.function.DoubleToIntFunction javaFunction = function;
        assertEquals(5, javaFunction.applyAsInt(5.7));
    }

    @Test
    public void testDEFAULT_IsConstant() {
        DoubleToIntFunction default1 = DoubleToIntFunction.DEFAULT;
        DoubleToIntFunction default2 = DoubleToIntFunction.DEFAULT;
        assertSame(default1, default2);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        DoubleToIntFunction lambda = value -> (int) value;
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.applyAsInt(1.0));
    }
}
