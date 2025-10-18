package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BooleanToIntFunction2025Test extends TestBase {

    @Test
    public void testApplyAsInt() {
        BooleanToIntFunction function = value -> value ? 1 : 0;

        assertEquals(1, function.applyAsInt(true));
        assertEquals(0, function.applyAsInt(false));
    }

    @Test
    public void testApplyAsIntWithLambda() {
        BooleanToIntFunction function = value -> value ? 100 : -100;

        assertEquals(100, function.applyAsInt(true));
        assertEquals(-100, function.applyAsInt(false));
    }

    @Test
    public void testApplyAsIntWithMaxValues() {
        BooleanToIntFunction function = value -> value ? Integer.MAX_VALUE : Integer.MIN_VALUE;

        assertEquals(Integer.MAX_VALUE, function.applyAsInt(true));
        assertEquals(Integer.MIN_VALUE, function.applyAsInt(false));
    }

    @Test
    public void testApplyAsIntWithException() {
        BooleanToIntFunction function = value -> {
            throw new RuntimeException("Test exception");
        };

        assertThrows(RuntimeException.class, () -> function.applyAsInt(true));
    }

    @Test
    public void testAnonymousClass() {
        BooleanToIntFunction function = new BooleanToIntFunction() {
            @Override
            public int applyAsInt(boolean value) {
                return value ? 42 : 0;
            }
        };

        assertEquals(42, function.applyAsInt(true));
        assertEquals(0, function.applyAsInt(false));
    }

    @Test
    public void testConstantReturn() {
        BooleanToIntFunction function = value -> 5;

        assertEquals(5, function.applyAsInt(true));
        assertEquals(5, function.applyAsInt(false));
    }

    @Test
    public void testMultiplication() {
        BooleanToIntFunction function = value -> value ? 10 * 10 : 5 * 5;

        assertEquals(100, function.applyAsInt(true));
        assertEquals(25, function.applyAsInt(false));
    }
}
