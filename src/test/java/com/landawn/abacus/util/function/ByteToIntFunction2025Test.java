package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ByteToIntFunction2025Test extends TestBase {

    @Test
    public void testApplyAsInt() {
        ByteToIntFunction toInt = value -> (int) value;

        assertEquals(5, toInt.applyAsInt((byte) 5));
        assertEquals(-10, toInt.applyAsInt((byte) -10));
        assertEquals(0, toInt.applyAsInt((byte) 0));
    }

    @Test
    public void testApplyAsIntWithLambda() {
        ByteToIntFunction multiply = value -> value * 10;

        assertEquals(50, multiply.applyAsInt((byte) 5));
        assertEquals(-100, multiply.applyAsInt((byte) -10));
    }

    @Test
    public void testApplyAsIntWithAnonymousClass() {
        ByteToIntFunction toUnsigned = new ByteToIntFunction() {
            @Override
            public int applyAsInt(byte value) {
                return value & 0xFF;
            }
        };

        assertEquals(255, toUnsigned.applyAsInt((byte) -1));
        assertEquals(128, toUnsigned.applyAsInt((byte) -128));
        assertEquals(127, toUnsigned.applyAsInt((byte) 127));
    }

    @Test
    public void testDefault() {
        assertEquals(5, ByteToIntFunction.DEFAULT.applyAsInt((byte) 5));
        assertEquals(-10, ByteToIntFunction.DEFAULT.applyAsInt((byte) -10));
        assertEquals(0, ByteToIntFunction.DEFAULT.applyAsInt((byte) 0));
    }

    @Test
    public void testDefaultWithBoundaryValues() {
        assertEquals(127, ByteToIntFunction.DEFAULT.applyAsInt(Byte.MAX_VALUE));
        assertEquals(-128, ByteToIntFunction.DEFAULT.applyAsInt(Byte.MIN_VALUE));
    }

    @Test
    public void testWideningConversion() {
        ByteToIntFunction widen = value -> value;

        assertEquals(127, widen.applyAsInt((byte) 127));
        assertEquals(-128, widen.applyAsInt((byte) -128));
    }

    @Test
    public void testUnsignedConversion() {
        ByteToIntFunction toUnsigned = value -> value & 0xFF;

        assertEquals(0, toUnsigned.applyAsInt((byte) 0));
        assertEquals(127, toUnsigned.applyAsInt((byte) 127));
        assertEquals(128, toUnsigned.applyAsInt((byte) -128));
        assertEquals(255, toUnsigned.applyAsInt((byte) -1));
    }

    @Test
    public void testArithmeticOperation() {
        ByteToIntFunction square = value -> value * value;

        assertEquals(25, square.applyAsInt((byte) 5));
        assertEquals(100, square.applyAsInt((byte) -10));
    }

    @Test
    public void testWithBoundaryValues() {
        ByteToIntFunction toInt = value -> (int) value;

        assertEquals(Byte.MAX_VALUE, toInt.applyAsInt(Byte.MAX_VALUE));
        assertEquals(Byte.MIN_VALUE, toInt.applyAsInt(Byte.MIN_VALUE));
    }

    @Test
    public void testMethodReference() {
        ByteToIntFunction toInt = ByteToIntFunction2025Test::byteToInt;

        assertEquals(5, toInt.applyAsInt((byte) 5));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ByteToIntFunction.class.getAnnotation(FunctionalInterface.class));
    }

    private static int byteToInt(byte value) {
        return (int) value;
    }
}
