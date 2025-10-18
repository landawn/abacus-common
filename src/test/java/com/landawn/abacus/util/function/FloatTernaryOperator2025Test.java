package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class FloatTernaryOperator2025Test extends TestBase {

    @Test
    public void testApplyAsFloat() {
        final FloatTernaryOperator operator = (a, b, c) -> a + b + c;
        final float result = operator.applyAsFloat(10.5f, 20.3f, 30.2f);
        assertEquals(61.0f, result, 0.001f);
    }

    @Test
    public void testApplyAsFloat_sum() {
        final FloatTernaryOperator sum = (a, b, c) -> a + b + c;
        assertEquals(61.0f, sum.applyAsFloat(10.5f, 20.3f, 30.2f), 0.001f);
    }

    @Test
    public void testApplyAsFloat_product() {
        final FloatTernaryOperator product = (a, b, c) -> a * b * c;
        assertEquals(120.0f, product.applyAsFloat(4.0f, 5.0f, 6.0f), 0.001f);
    }

    @Test
    public void testApplyAsFloat_max() {
        final FloatTernaryOperator max = (a, b, c) -> Math.max(a, Math.max(b, c));
        assertEquals(100.0f, max.applyAsFloat(5.0f, 100.0f, 50.0f), 0.001f);
    }

    @Test
    public void testApplyAsFloat_min() {
        final FloatTernaryOperator min = (a, b, c) -> Math.min(a, Math.min(b, c));
        assertEquals(5.0f, min.applyAsFloat(5.0f, 100.0f, 50.0f), 0.001f);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final FloatTernaryOperator operator = (a, b, c) -> a + b + c;
        assertNotNull(operator);
        assertEquals(60.0f, operator.applyAsFloat(10.0f, 20.0f, 30.0f), 0.001f);
    }
}
