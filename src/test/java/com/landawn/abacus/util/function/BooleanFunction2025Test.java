package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BooleanFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        BooleanFunction<String> function = value -> value ? "true" : "false";

        assertEquals("true", function.apply(true));
        assertEquals("false", function.apply(false));
    }

    @Test
    public void testApplyWithLambda() {
        BooleanFunction<Integer> function = value -> value ? 1 : 0;

        assertEquals(1, function.apply(true));
        assertEquals(0, function.apply(false));
    }

    @Test
    public void testBox() {
        BooleanFunction<Boolean> boxFunction = BooleanFunction.BOX;

        Boolean trueResult = boxFunction.apply(true);
        Boolean falseResult = boxFunction.apply(false);

        assertTrue(trueResult);
        assertFalse(falseResult);
    }

    @Test
    public void testIdentity() {
        BooleanFunction<Boolean> identity = BooleanFunction.identity();

        assertTrue(identity.apply(true));
        assertFalse(identity.apply(false));
    }

    @Test
    public void testAndThen() {
        BooleanFunction<String> function = value -> value ? "yes" : "no";
        java.util.function.Function<String, Integer> afterFunction = String::length;

        BooleanFunction<Integer> chainedFunction = function.andThen(afterFunction);

        assertEquals(3, chainedFunction.apply(true)); // "yes" has length 3
        assertEquals(2, chainedFunction.apply(false)); // "no" has length 2
    }

    @Test
    public void testAndThenMultipleChains() {
        BooleanFunction<Integer> function = value -> value ? 100 : 0;
        java.util.function.Function<Integer, Integer> doubleIt = x -> x * 2;
        java.util.function.Function<Integer, String> toString = Object::toString;

        BooleanFunction<String> chainedFunction = function.andThen(doubleIt).andThen(toString);

        assertEquals("200", chainedFunction.apply(true)); // 100 * 2 = 200
        assertEquals("0", chainedFunction.apply(false)); // 0 * 2 = 0
    }

    @Test
    public void testAndThenWithException() {
        BooleanFunction<String> function = value -> "test";
        java.util.function.Function<String, Integer> afterFunction = s -> {
            throw new RuntimeException("Test exception");
        };

        BooleanFunction<Integer> chainedFunction = function.andThen(afterFunction);

        assertThrows(RuntimeException.class, () -> chainedFunction.apply(true));
    }

    @Test
    public void testApplyWithException() {
        BooleanFunction<String> function = value -> {
            throw new RuntimeException("Apply exception");
        };

        assertThrows(RuntimeException.class, () -> function.apply(true));
    }

    @Test
    public void testAnonymousClass() {
        BooleanFunction<String> function = new BooleanFunction<String>() {
            @Override
            public String apply(boolean value) {
                return value ? "POSITIVE" : "NEGATIVE";
            }
        };

        assertEquals("POSITIVE", function.apply(true));
        assertEquals("NEGATIVE", function.apply(false));
    }

    @Test
    public void testReturnsNull() {
        BooleanFunction<String> function = value -> null;

        assertNull(function.apply(true));
        assertNull(function.apply(false));
    }

    @Test
    public void testConditionalReturn() {
        BooleanFunction<String> function = value -> {
            if (value) {
                return "affirmative";
            } else {
                return "negative";
            }
        };

        assertEquals("affirmative", function.apply(true));
        assertEquals("negative", function.apply(false));
    }

    @Test
    public void testComplexTransformation() {
        BooleanFunction<String> function = value -> {
            if (value) {
                return "Value is TRUE";
            }
            return "Value is FALSE";
        };

        assertEquals("Value is TRUE", function.apply(true));
        assertEquals("Value is FALSE", function.apply(false));
    }

    @Test
    public void testReturningDifferentTypes() {
        BooleanFunction<Double> function = value -> value ? 1.0 : 0.0;

        assertEquals(1.0, function.apply(true), 0.001);
        assertEquals(0.0, function.apply(false), 0.001);
    }

    @Test
    public void testBoxNotNull() {
        assertNotNull(BooleanFunction.BOX);
        assertNotNull(BooleanFunction.BOX.apply(true));
        assertNotNull(BooleanFunction.BOX.apply(false));
    }

    @Test
    public void testIdentityNotNull() {
        assertNotNull(BooleanFunction.identity());
        assertNotNull(BooleanFunction.identity().apply(true));
        assertNotNull(BooleanFunction.identity().apply(false));
    }
}
