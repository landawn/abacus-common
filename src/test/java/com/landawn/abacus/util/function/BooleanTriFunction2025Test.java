package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BooleanTriFunction2025Test extends TestBase {
    @Test
    public void testAndThenWithException() {
        BooleanTriFunction<String> function = (a, b, c) -> "test";
        java.util.function.Function<String, Integer> afterFunction = s -> {
            throw new RuntimeException("Test exception");
        };

        BooleanTriFunction<Integer> chainedFunction = function.andThen(afterFunction);

        assertThrows(RuntimeException.class, () -> chainedFunction.apply(true, false, true));
    }

    @Test
    public void testApplyWithException() {
        BooleanTriFunction<String> function = (a, b, c) -> {
            throw new RuntimeException("Apply exception");
        };

        assertThrows(RuntimeException.class, () -> function.apply(true, false, true));
    }

    @Test
    public void testAnonymousClass() {
        BooleanTriFunction<String> function = new BooleanTriFunction<String>() {
            @Override
            public String apply(boolean a, boolean b, boolean c) {
                return String.format("a=%b, b=%b, c=%b", a, b, c);
            }
        };

        String result = function.apply(true, false, true);

        assertEquals("a=true, b=false, c=true", result);
    }

    @Test
    public void testReturnsNull() {
        BooleanTriFunction<String> function = (a, b, c) -> null;

        String result = function.apply(true, false, true);

        assertNull(result);
    }

    @Test
    public void testMajorityVote() {
        BooleanTriFunction<String> function = (a, b, c) -> {
            int count = (a ? 1 : 0) + (b ? 1 : 0) + (c ? 1 : 0);
            return count >= 2 ? "Majority true" : "Majority false";
        };

        assertEquals("Majority true", function.apply(true, true, false));
        assertEquals("Majority false", function.apply(true, false, false));
    }
}
