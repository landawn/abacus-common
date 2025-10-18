package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ByteTernaryOperator2025Test extends TestBase {

    @Test
    public void testApplyAsByte() {
        ByteTernaryOperator sum = (a, b, c) -> (byte) (a + b + c);

        assertEquals((byte) 6, sum.applyAsByte((byte) 1, (byte) 2, (byte) 3));
        assertEquals((byte) 15, sum.applyAsByte((byte) 5, (byte) 5, (byte) 5));
    }

    @Test
    public void testApplyAsByteWithLambda() {
        ByteTernaryOperator multiply = (a, b, c) -> (byte) (a * b * c);

        assertEquals((byte) 24, multiply.applyAsByte((byte) 2, (byte) 3, (byte) 4));
        assertEquals((byte) 120, multiply.applyAsByte((byte) 5, (byte) 4, (byte) 6));
    }

    @Test
    public void testApplyAsByteWithAnonymousClass() {
        ByteTernaryOperator max = new ByteTernaryOperator() {
            @Override
            public byte applyAsByte(byte a, byte b, byte c) {
                return (byte) Math.max(a, Math.max(b, c));
            }
        };

        assertEquals((byte) 10, max.applyAsByte((byte) 5, (byte) 10, (byte) 3));
        assertEquals((byte) 15, max.applyAsByte((byte) 10, (byte) 5, (byte) 15));
    }

    @Test
    public void testSum() {
        ByteTernaryOperator sum = (a, b, c) -> (byte) (a + b + c);

        assertEquals((byte) 0, sum.applyAsByte((byte) 0, (byte) 0, (byte) 0));
        assertEquals((byte) 30, sum.applyAsByte((byte) 10, (byte) 10, (byte) 10));
        assertEquals((byte) -15, sum.applyAsByte((byte) -5, (byte) -5, (byte) -5));
    }

    @Test
    public void testAverage() {
        ByteTernaryOperator average = (a, b, c) -> (byte) ((a + b + c) / 3);

        assertEquals((byte) 2, average.applyAsByte((byte) 1, (byte) 2, (byte) 3));
        assertEquals((byte) 5, average.applyAsByte((byte) 5, (byte) 5, (byte) 5));
    }

    @Test
    public void testMin() {
        ByteTernaryOperator min = (a, b, c) -> (byte) Math.min(a, Math.min(b, c));

        assertEquals((byte) 1, min.applyAsByte((byte) 5, (byte) 1, (byte) 10));
        assertEquals((byte) -10, min.applyAsByte((byte) -5, (byte) 0, (byte) -10));
    }

    @Test
    public void testMax() {
        ByteTernaryOperator max = (a, b, c) -> (byte) Math.max(a, Math.max(b, c));

        assertEquals((byte) 10, max.applyAsByte((byte) 5, (byte) 10, (byte) 3));
        assertEquals((byte) 5, max.applyAsByte((byte) -5, (byte) 0, (byte) 5));
    }

    @Test
    public void testWithNegativeValues() {
        ByteTernaryOperator sum = (a, b, c) -> (byte) (a + b + c);

        assertEquals((byte) -12, sum.applyAsByte((byte) -5, (byte) -3, (byte) -4));
        assertEquals((byte) 2, sum.applyAsByte((byte) -5, (byte) 3, (byte) 4));
    }

    @Test
    public void testWithBoundaryValues() {
        ByteTernaryOperator max = (a, b, c) -> (byte) Math.max(a, Math.max(b, c));

        assertEquals(Byte.MAX_VALUE, max.applyAsByte(Byte.MIN_VALUE, (byte) 0, Byte.MAX_VALUE));
        assertEquals(Byte.MIN_VALUE, max.applyAsByte(Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ByteTernaryOperator.class.getAnnotation(FunctionalInterface.class));
    }
}
