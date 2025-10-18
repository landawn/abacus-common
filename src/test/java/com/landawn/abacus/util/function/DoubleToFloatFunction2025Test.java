package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DoubleToFloatFunction2025Test extends TestBase {

    @Test
    public void testApplyAsFloat() {
        DoubleToFloatFunction function = value -> (float) value;
        float result = function.applyAsFloat(5.7);
        assertEquals(5.7f, result, 0.0001f);
    }

    @Test
    public void testApplyAsFloat_WithAnonymousClass() {
        DoubleToFloatFunction function = new DoubleToFloatFunction() {
            @Override
            public float applyAsFloat(double value) {
                return (float) (value * 2);
            }
        };

        float result = function.applyAsFloat(5.0);
        assertEquals(10.0f, result, 0.0001f);
    }

    @Test
    public void testDEFAULT() {
        DoubleToFloatFunction function = DoubleToFloatFunction.DEFAULT;
        assertEquals(5.7f, function.applyAsFloat(5.7), 0.0001f);
        assertEquals(-5.7f, function.applyAsFloat(-5.7), 0.0001f);
        assertEquals(0.0f, function.applyAsFloat(0.0), 0.0001f);
    }

    @Test
    public void testApplyAsFloat_WithZero() {
        DoubleToFloatFunction function = DoubleToFloatFunction.DEFAULT;
        assertEquals(0.0f, function.applyAsFloat(0.0), 0.0001f);
        assertEquals(0.0f, function.applyAsFloat(-0.0), 0.0001f);
    }

    @Test
    public void testApplyAsFloat_WithNegativeValues() {
        DoubleToFloatFunction function = DoubleToFloatFunction.DEFAULT;
        assertEquals(-5.7f, function.applyAsFloat(-5.7), 0.0001f);
        assertEquals(-10.2f, function.applyAsFloat(-10.2), 0.0001f);
    }

    @Test
    public void testApplyAsFloat_WithLargeValues() {
        DoubleToFloatFunction function = DoubleToFloatFunction.DEFAULT;
        assertEquals(1000000.5f, function.applyAsFloat(1000000.5), 0.1f);
    }

    @Test
    public void testApplyAsFloat_PrecisionLoss() {
        DoubleToFloatFunction function = DoubleToFloatFunction.DEFAULT;
        double largeDouble = 123456789.123456789;
        float result = function.applyAsFloat(largeDouble);
        // Float has less precision than double, so we expect some loss
        assertTrue(result > 0);
    }

    @Test
    public void testApplyAsFloat_SpecialValues() {
        DoubleToFloatFunction function = DoubleToFloatFunction.DEFAULT;

        assertEquals(Float.POSITIVE_INFINITY, function.applyAsFloat(Double.POSITIVE_INFINITY), 0.0001f);
        assertEquals(Float.NEGATIVE_INFINITY, function.applyAsFloat(Double.NEGATIVE_INFINITY), 0.0001f);
        assertTrue(Float.isNaN(function.applyAsFloat(Double.NaN)));
    }

    @Test
    public void testDEFAULT_IsConstant() {
        DoubleToFloatFunction default1 = DoubleToFloatFunction.DEFAULT;
        DoubleToFloatFunction default2 = DoubleToFloatFunction.DEFAULT;
        assertSame(default1, default2);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        DoubleToFloatFunction lambda = value -> (float) value;
        assertNotNull(lambda);
        assertDoesNotThrow(() -> lambda.applyAsFloat(1.0));
    }
}
