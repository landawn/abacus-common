package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BooleanBiFunction2025Test extends TestBase {
    @Test
    public void testAndThenMultipleChains() {
        BooleanBiFunction<Integer> function = (t, u) -> (t ? 1 : 0) + (u ? 1 : 0);
        java.util.function.Function<Integer, Integer> doubleIt = x -> x * 2;
        java.util.function.Function<Integer, String> toString = Object::toString;

        BooleanBiFunction<String> chainedFunction = function.andThen(doubleIt).andThen(toString);
        String result = chainedFunction.apply(true, false);

        assertEquals("2", result);  // (1 + 0) * 2 = 2
    }

    @Test
    public void testAndThenWithException() {
        BooleanBiFunction<String> function = (t, u) -> "test";
        java.util.function.Function<String, Integer> afterFunction = s -> {
            throw new RuntimeException("Test exception");
        };

        BooleanBiFunction<Integer> chainedFunction = function.andThen(afterFunction);

        assertThrows(RuntimeException.class, () -> chainedFunction.apply(true, false));
    }

    @Test
    public void testApplyWithException() {
        BooleanBiFunction<String> function = (t, u) -> {
            throw new RuntimeException("Apply exception");
        };

        assertThrows(RuntimeException.class, () -> function.apply(true, false));
    }

    @Test
    public void testAnonymousClass() {
        BooleanBiFunction<String> function = new BooleanBiFunction<String>() {
            @Override
            public String apply(boolean t, boolean u) {
                return String.format("t=%b, u=%b", t, u);
            }
        };

        String result = function.apply(true, false);

        assertEquals("t=true, u=false", result);
    }

    @Test
    public void testReturnsNull() {
        BooleanBiFunction<String> function = (t, u) -> null;

        String result = function.apply(true, false);

        assertNull(result);
    }

    @Test
    public void testLogicalOperations() {
        BooleanBiFunction<String> function = (t, u) ->
            String.format("AND=%b, OR=%b, XOR=%b", t && u, t || u, t != u);

        assertEquals("AND=false, OR=true, XOR=true", function.apply(true, false));
        assertEquals("AND=true, OR=true, XOR=false", function.apply(true, true));
        assertEquals("AND=false, OR=false, XOR=false", function.apply(false, false));
    }

    @Test
    public void testAllCombinations() {
        BooleanBiFunction<String> function = (t, u) -> t + "&" + u;

        assertEquals("true&true", function.apply(true, true));
        assertEquals("true&false", function.apply(true, false));
        assertEquals("false&true", function.apply(false, true));
        assertEquals("false&false", function.apply(false, false));
    }

    @Test
    public void testComplexLogic() {
        BooleanBiFunction<String> function = (t, u) -> {
            if (t && u) return "BOTH";
            if (t) return "FIRST";
            if (u) return "SECOND";
            return "NEITHER";
        };

        assertEquals("BOTH", function.apply(true, true));
        assertEquals("FIRST", function.apply(true, false));
        assertEquals("SECOND", function.apply(false, true));
        assertEquals("NEITHER", function.apply(false, false));
    }
}
