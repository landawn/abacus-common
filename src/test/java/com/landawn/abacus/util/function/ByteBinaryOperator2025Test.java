package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ByteBinaryOperator2025Test extends TestBase {

    @Test
    public void testApplyAsByte() {
        ByteBinaryOperator add = (left, right) -> (byte) (left + right);

        assertEquals((byte) 15, add.applyAsByte((byte) 5, (byte) 10));
        assertEquals((byte) 4, add.applyAsByte((byte) -3, (byte) 7));
    }

    @Test
    public void testApplyAsByteWithLambda() {
        ByteBinaryOperator multiply = (left, right) -> (byte) (left * right);

        assertEquals((byte) 50, multiply.applyAsByte((byte) 5, (byte) 10));
        assertEquals((byte) -21, multiply.applyAsByte((byte) -3, (byte) 7));
    }

    @Test
    public void testApplyAsByteWithAnonymousClass() {
        ByteBinaryOperator max = new ByteBinaryOperator() {
            @Override
            public byte applyAsByte(byte left, byte right) {
                return left > right ? left : right;
            }
        };

        assertEquals((byte) 10, max.applyAsByte((byte) 5, (byte) 10));
        assertEquals((byte) 7, max.applyAsByte((byte) -3, (byte) 7));
    }

    @Test
    public void testAddition() {
        ByteBinaryOperator add = (left, right) -> (byte) (left + right);

        assertEquals((byte) 0, add.applyAsByte((byte) 0, (byte) 0));
        assertEquals((byte) 100, add.applyAsByte((byte) 50, (byte) 50));
        assertEquals((byte) -10, add.applyAsByte((byte) -5, (byte) -5));
    }

    @Test
    public void testSubtraction() {
        ByteBinaryOperator subtract = (left, right) -> (byte) (left - right);

        assertEquals((byte) 5, subtract.applyAsByte((byte) 10, (byte) 5));
        assertEquals((byte) -10, subtract.applyAsByte((byte) -3, (byte) 7));
    }

    @Test
    public void testMultiplication() {
        ByteBinaryOperator multiply = (left, right) -> (byte) (left * right);

        assertEquals((byte) 0, multiply.applyAsByte((byte) 0, (byte) 5));
        assertEquals((byte) 25, multiply.applyAsByte((byte) 5, (byte) 5));
    }

    @Test
    public void testDivision() {
        ByteBinaryOperator divide = (left, right) -> (byte) (left / right);

        assertEquals((byte) 2, divide.applyAsByte((byte) 10, (byte) 5));
        assertEquals((byte) 3, divide.applyAsByte((byte) 15, (byte) 5));
    }

    @Test
    public void testMin() {
        ByteBinaryOperator min = (left, right) -> left < right ? left : right;

        assertEquals((byte) 5, min.applyAsByte((byte) 5, (byte) 10));
        assertEquals((byte) -3, min.applyAsByte((byte) -3, (byte) 7));
        assertEquals((byte) 0, min.applyAsByte((byte) 0, (byte) 5));
    }

    @Test
    public void testMax() {
        ByteBinaryOperator max = (left, right) -> left > right ? left : right;

        assertEquals((byte) 10, max.applyAsByte((byte) 5, (byte) 10));
        assertEquals((byte) 7, max.applyAsByte((byte) -3, (byte) 7));
        assertEquals((byte) 5, max.applyAsByte((byte) 0, (byte) 5));
    }

    @Test
    public void testWithBoundaryValues() {
        ByteBinaryOperator max = (left, right) -> left > right ? left : right;

        assertEquals(Byte.MAX_VALUE, max.applyAsByte(Byte.MIN_VALUE, Byte.MAX_VALUE));
        assertEquals(Byte.MIN_VALUE, max.applyAsByte(Byte.MIN_VALUE, Byte.MIN_VALUE));
    }

    @Test
    public void testMethodReference() {
        ByteBinaryOperator add = ByteBinaryOperator2025Test::addBytes;

        assertEquals((byte) 15, add.applyAsByte((byte) 5, (byte) 10));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ByteBinaryOperator.class.getAnnotation(FunctionalInterface.class));
    }

    private static byte addBytes(byte left, byte right) {
        return (byte) (left + right);
    }
}
