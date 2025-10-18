package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BooleanTernaryOperator2025Test extends TestBase {
    @Test
    public void testAlwaysTrue() {
        BooleanTernaryOperator operator = (a, b, c) -> true;

        assertTrue(operator.applyAsBoolean(true, true, true));
        assertTrue(operator.applyAsBoolean(false, false, false));
    }

    @Test
    public void testAlwaysFalse() {
        BooleanTernaryOperator operator = (a, b, c) -> false;

        assertFalse(operator.applyAsBoolean(true, true, true));
        assertFalse(operator.applyAsBoolean(false, false, false));
    }

    @Test
    public void testApplyAsBooleanWithException() {
        BooleanTernaryOperator operator = (a, b, c) -> {
            throw new RuntimeException("Test exception");
        };

        assertThrows(RuntimeException.class, () -> operator.applyAsBoolean(true, false, true));
    }

    @Test
    public void testAnonymousClass() {
        BooleanTernaryOperator operator = new BooleanTernaryOperator() {
            @Override
            public boolean applyAsBoolean(boolean a, boolean b, boolean c) {
                return (a && b) || c;
            }
        };

        assertTrue(operator.applyAsBoolean(true, true, false));
        assertTrue(operator.applyAsBoolean(false, false, true));
        assertFalse(operator.applyAsBoolean(false, false, false));
    }

    @Test
    public void testEvenNumberOfTrues() {
        BooleanTernaryOperator operator = (a, b, c) -> {
            int count = (a ? 1 : 0) + (b ? 1 : 0) + (c ? 1 : 0);
            return count % 2 == 0;
        };

        assertFalse(operator.applyAsBoolean(true, false, false));  // 1 true
        assertTrue(operator.applyAsBoolean(true, true, false));    // 2 trues
        assertFalse(operator.applyAsBoolean(true, true, true));    // 3 trues
        assertTrue(operator.applyAsBoolean(false, false, false));  // 0 trues
    }
}
