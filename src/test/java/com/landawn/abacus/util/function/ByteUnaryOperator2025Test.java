package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ByteUnaryOperator2025Test extends TestBase {

    @Test
    public void testApplyAsByte() {
        ByteUnaryOperator increment = operand -> (byte) (operand + 1);

        assertEquals((byte) 6, increment.applyAsByte((byte) 5));
        assertEquals((byte) 0, increment.applyAsByte((byte) -1));
    }

    @Test
    public void testApplyAsByteWithLambda() {
        ByteUnaryOperator negate = operand -> (byte) -operand;

        assertEquals((byte) -5, negate.applyAsByte((byte) 5));
        assertEquals((byte) 5, negate.applyAsByte((byte) -5));
    }

    @Test
    public void testApplyAsByteWithAnonymousClass() {
        ByteUnaryOperator double_it = new ByteUnaryOperator() {
            @Override
            public byte applyAsByte(byte operand) {
                return (byte) (operand * 2);
            }
        };

        assertEquals((byte) 10, double_it.applyAsByte((byte) 5));
        assertEquals((byte) -10, double_it.applyAsByte((byte) -5));
    }

    @Test
    public void testCompose() {
        ByteUnaryOperator addOne = operand -> (byte) (operand + 1);
        ByteUnaryOperator multiplyTwo = operand -> (byte) (operand * 2);

        ByteUnaryOperator composed = multiplyTwo.compose(addOne);

        assertEquals((byte) 12, composed.applyAsByte((byte) 5));
    }

    @Test
    public void testAndThen() {
        ByteUnaryOperator addOne = operand -> (byte) (operand + 1);
        ByteUnaryOperator multiplyTwo = operand -> (byte) (operand * 2);

        ByteUnaryOperator composed = addOne.andThen(multiplyTwo);

        assertEquals((byte) 12, composed.applyAsByte((byte) 5));
    }

    @Test
    public void testComposeAndAndThen() {
        ByteUnaryOperator addOne = operand -> (byte) (operand + 1);
        ByteUnaryOperator multiplyTwo = operand -> (byte) (operand * 2);
        ByteUnaryOperator subtractThree = operand -> (byte) (operand - 3);

        ByteUnaryOperator composed = addOne.compose(multiplyTwo).andThen(subtractThree);

        assertEquals((byte) 8, composed.applyAsByte((byte) 5));
    }

    @Test
    public void testIdentity() {
        ByteUnaryOperator identity = ByteUnaryOperator.identity();

        assertEquals((byte) 5, identity.applyAsByte((byte) 5));
        assertEquals((byte) -10, identity.applyAsByte((byte) -10));
        assertEquals((byte) 0, identity.applyAsByte((byte) 0));
    }

    @Test
    public void testWithBoundaryValues() {
        ByteUnaryOperator identity = ByteUnaryOperator.identity();

        assertEquals(Byte.MAX_VALUE, identity.applyAsByte(Byte.MAX_VALUE));
        assertEquals(Byte.MIN_VALUE, identity.applyAsByte(Byte.MIN_VALUE));
    }

    @Test
    public void testMethodReference() {
        ByteUnaryOperator increment = ByteUnaryOperator2025Test::incrementByte;

        assertEquals((byte) 6, increment.applyAsByte((byte) 5));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ByteUnaryOperator.class.getAnnotation(FunctionalInterface.class));
    }

    private static byte incrementByte(byte value) {
        return (byte) (value + 1);
    }
}
