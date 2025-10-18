package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class FloatSupplier2025Test extends TestBase {

    @Test
    public void testGetAsFloat() {
        final FloatSupplier supplier = () -> 42.5f;
        final float result = supplier.getAsFloat();
        assertEquals(42.5f, result, 0.001f);
    }

    @Test
    public void testGetAsFloat_withLambda() {
        final FloatSupplier supplier = () -> 100.5f;
        assertEquals(100.5f, supplier.getAsFloat(), 0.001f);
    }

    @Test
    public void testGetAsFloat_returnsNegative() {
        final FloatSupplier supplier = () -> -42.5f;
        assertEquals(-42.5f, supplier.getAsFloat(), 0.001f);
    }

    @Test
    public void testGetAsFloat_returnsZero() {
        final FloatSupplier supplier = () -> 0f;
        assertEquals(0f, supplier.getAsFloat(), 0.001f);
    }

    @Test
    public void testGetAsFloat_stateful() {
        final AtomicReference<Float> counter = new AtomicReference<>(0f);
        final FloatSupplier supplier = () -> {
            counter.set(counter.get() + 1.0f);
            return counter.get();
        };

        assertEquals(1.0f, supplier.getAsFloat(), 0.001f);
        assertEquals(2.0f, supplier.getAsFloat(), 0.001f);
        assertEquals(3.0f, supplier.getAsFloat(), 0.001f);
    }

    @Test
    public void testConstant_ZERO() {
        assertEquals(0f, FloatSupplier.ZERO.getAsFloat(), 0.001f);
        assertEquals(0f, FloatSupplier.ZERO.getAsFloat(), 0.001f);
    }

    @Test
    public void testConstant_RANDOM() {
        final float value1 = FloatSupplier.RANDOM.getAsFloat();
        final float value2 = FloatSupplier.RANDOM.getAsFloat();

        assertNotNull(value1);
        assertNotNull(value2);
        assertTrue(value1 >= 0 && value1 < 1.0f);
        assertTrue(value2 >= 0 && value2 < 1.0f);
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final FloatSupplier supplier = () -> 1.0f;
        assertNotNull(supplier);
        assertEquals(1.0f, supplier.getAsFloat(), 0.001f);
    }
}
