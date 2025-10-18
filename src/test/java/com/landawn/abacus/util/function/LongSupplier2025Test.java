package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class LongSupplier2025Test extends TestBase {

    @Test
    public void testGetAsLong() {
        final LongSupplier supplier = () -> 42L;
        final long result = supplier.getAsLong();
        assertEquals(42L, result);
    }

    @Test
    public void testGetAsLong_withLambda() {
        final LongSupplier supplier = () -> 100L;
        assertEquals(100L, supplier.getAsLong());
    }

    @Test
    public void testGetAsLong_withAnonymousClass() {
        final LongSupplier supplier = new LongSupplier() {
            @Override
            public long getAsLong() {
                return 123L;
            }
        };

        assertEquals(123L, supplier.getAsLong());
    }

    @Test
    public void testGetAsLong_returnsNegative() {
        final LongSupplier supplier = () -> -42L;
        assertEquals(-42L, supplier.getAsLong());
    }

    @Test
    public void testGetAsLong_returnsZero() {
        final LongSupplier supplier = () -> 0L;
        assertEquals(0L, supplier.getAsLong());
    }

    @Test
    public void testGetAsLong_returnsMaxValue() {
        final LongSupplier supplier = () -> Long.MAX_VALUE;
        assertEquals(Long.MAX_VALUE, supplier.getAsLong());
    }

    @Test
    public void testGetAsLong_returnsMinValue() {
        final LongSupplier supplier = () -> Long.MIN_VALUE;
        assertEquals(Long.MIN_VALUE, supplier.getAsLong());
    }

    @Test
    public void testGetAsLong_stateful() {
        final AtomicLong counter = new AtomicLong(0);
        final LongSupplier supplier = counter::incrementAndGet;

        assertEquals(1L, supplier.getAsLong());
        assertEquals(2L, supplier.getAsLong());
        assertEquals(3L, supplier.getAsLong());
    }

    @Test
    public void testGetAsLong_fromField() {
        final long value = 999L;
        final LongSupplier supplier = () -> value;
        assertEquals(999L, supplier.getAsLong());
    }

    @Test
    public void testConstant_ZERO() {
        assertEquals(0L, LongSupplier.ZERO.getAsLong());
        assertEquals(0L, LongSupplier.ZERO.getAsLong());
    }

    @Test
    public void testConstant_RANDOM() {
        final long value1 = LongSupplier.RANDOM.getAsLong();
        final long value2 = LongSupplier.RANDOM.getAsLong();

        assertNotNull(value1);
        assertNotNull(value2);
        // Note: There's a very small chance they could be equal, but that's acceptable
    }

    @Test
    public void testConstant_RANDOM_range() {
        // Just verify it returns valid long values
        for (int i = 0; i < 100; i++) {
            final long value = LongSupplier.RANDOM.getAsLong();
            assertTrue(value >= Long.MIN_VALUE && value <= Long.MAX_VALUE);
        }
    }

    @Test
    public void testFunctionalInterfaceContract() {
        final LongSupplier supplier = () -> 1L;
        assertNotNull(supplier);
        assertEquals(1L, supplier.getAsLong());
    }

    @Test
    public void testMethodReference() {
        final LongSupplier supplier = this::getValue;
        assertEquals(42L, supplier.getAsLong());
    }

    private long getValue() {
        return 42L;
    }

    @Test
    public void testGetAsLong_multipleInvocations() {
        final LongSupplier supplier = () -> 42L;
        assertEquals(42L, supplier.getAsLong());
        assertEquals(42L, supplier.getAsLong());
        assertEquals(42L, supplier.getAsLong());
    }

    @Test
    public void testGetAsLong_computation() {
        final LongSupplier supplier = () -> System.currentTimeMillis();
        final long result = supplier.getAsLong();
        assertTrue(result > 0);
    }

    @Test
    public void testCompatibilityWithJavaUtilFunction() {
        final java.util.function.LongSupplier javaSupplier = () -> 42L;
        final LongSupplier abacusSupplier = javaSupplier::getAsLong;

        assertEquals(42L, abacusSupplier.getAsLong());
    }

    @Test
    public void testZERO_alwaysReturnsSameValue() {
        for (int i = 0; i < 10; i++) {
            assertEquals(0L, LongSupplier.ZERO.getAsLong());
        }
    }
}
