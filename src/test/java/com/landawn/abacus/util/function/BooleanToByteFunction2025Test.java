package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BooleanToByteFunction2025Test extends TestBase {

    @Test
    public void testApplyAsByte() {
        BooleanToByteFunction function = value -> value ? (byte) 1 : (byte) 0;

        assertEquals((byte) 1, function.applyAsByte(true));
        assertEquals((byte) 0, function.applyAsByte(false));
    }

    @Test
    public void testApplyAsByteWithLambda() {
        BooleanToByteFunction function = value -> value ? (byte) 100 : (byte) -100;

        assertEquals((byte) 100, function.applyAsByte(true));
        assertEquals((byte) -100, function.applyAsByte(false));
    }

    @Test
    public void testApplyAsByteWithMaxValues() {
        BooleanToByteFunction function = value -> value ? Byte.MAX_VALUE : Byte.MIN_VALUE;

        assertEquals(Byte.MAX_VALUE, function.applyAsByte(true));
        assertEquals(Byte.MIN_VALUE, function.applyAsByte(false));
    }

    @Test
    public void testApplyAsByteWithException() {
        BooleanToByteFunction function = value -> {
            throw new RuntimeException("Test exception");
        };

        assertThrows(RuntimeException.class, () -> function.applyAsByte(true));
    }

    @Test
    public void testAnonymousClass() {
        BooleanToByteFunction function = new BooleanToByteFunction() {
            @Override
            public byte applyAsByte(boolean value) {
                return value ? (byte) 42 : (byte) 0;
            }
        };

        assertEquals((byte) 42, function.applyAsByte(true));
        assertEquals((byte) 0, function.applyAsByte(false));
    }

    @Test
    public void testConstantReturn() {
        BooleanToByteFunction function = value -> (byte) 5;

        assertEquals((byte) 5, function.applyAsByte(true));
        assertEquals((byte) 5, function.applyAsByte(false));
    }
}
