package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BooleanNFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        BooleanNFunction<String> function = args -> {
            StringBuilder sb = new StringBuilder();
            for (boolean arg : args) {
                sb.append(arg).append(",");
            }
            return sb.toString();
        };

        String result = function.apply(true, false, true);

        assertEquals("true,false,true,", result);
    }

    @Test
    public void testApplyWithEmptyArray() {
        BooleanNFunction<Integer> function = args -> args.length;

        Integer result = function.apply();

        assertEquals(0, result);
    }

    @Test
    public void testApplyWithSingleElement() {
        BooleanNFunction<Boolean> function = args -> args.length > 0 && args[0];

        assertEquals(true, function.apply(true));
        assertEquals(false, function.apply(false));
    }

    @Test
    public void testApplyCountingTrue() {
        BooleanNFunction<Integer> function = args -> {
            int count = 0;
            for (boolean arg : args) {
                if (arg) count++;
            }
            return count;
        };

        assertEquals(3, function.apply(true, false, true, true, false));
        assertEquals(0, function.apply(false, false, false));
        assertEquals(5, function.apply(true, true, true, true, true));
    }

    @Test
    public void testAndThen() {
        BooleanNFunction<Integer> function = args -> args.length;
        java.util.function.Function<Integer, String> afterFunction = Object::toString;

        BooleanNFunction<String> chainedFunction = function.andThen(afterFunction);
        String result = chainedFunction.apply(true, false, true);

        assertEquals("3", result);
    }

    @Test
    public void testAndThenMultipleChains() {
        BooleanNFunction<Integer> function = args -> {
            int count = 0;
            for (boolean arg : args) {
                if (arg) count++;
            }
            return count;
        };
        java.util.function.Function<Integer, Integer> doubleIt = x -> x * 2;
        java.util.function.Function<Integer, String> toString = x -> "Count: " + x;

        BooleanNFunction<String> chainedFunction = function.andThen(doubleIt).andThen(toString);
        String result = chainedFunction.apply(true, true, false);

        assertEquals("Count: 4", result);  // 2 trues * 2 = 4
    }

    @Test
    public void testAndThenWithException() {
        BooleanNFunction<String> function = args -> "test";
        java.util.function.Function<String, Integer> afterFunction = s -> {
            throw new RuntimeException("Test exception");
        };

        BooleanNFunction<Integer> chainedFunction = function.andThen(afterFunction);

        assertThrows(RuntimeException.class, () -> chainedFunction.apply(true, false));
    }

    @Test
    public void testApplyWithException() {
        BooleanNFunction<String> function = args -> {
            throw new RuntimeException("Apply exception");
        };

        assertThrows(RuntimeException.class, () -> function.apply(true));
    }

    @Test
    public void testAnonymousClass() {
        BooleanNFunction<String> function = new BooleanNFunction<String>() {
            @Override
            public String apply(boolean... args) {
                return "Length: " + args.length;
            }
        };

        String result = function.apply(true, false, true, false, true);

        assertEquals("Length: 5", result);
    }

    @Test
    public void testReturnsNull() {
        BooleanNFunction<String> function = args -> null;

        String result = function.apply(true, false);

        assertNull(result);
    }

    @Test
    public void testAllTrue() {
        BooleanNFunction<Boolean> function = args -> {
            for (boolean arg : args) {
                if (!arg) return false;
            }
            return true;
        };

        assertEquals(true, function.apply(true, true, true));
        assertEquals(false, function.apply(true, false, true));
        assertEquals(true, function.apply());  // Empty array: all true
    }

    @Test
    public void testAnyTrue() {
        BooleanNFunction<Boolean> function = args -> {
            for (boolean arg : args) {
                if (arg) return true;
            }
            return false;
        };

        assertEquals(true, function.apply(false, true, false));
        assertEquals(false, function.apply(false, false, false));
        assertEquals(false, function.apply());  // Empty array: none true
    }

    @Test
    public void testVarArgs() {
        BooleanNFunction<String> function = args -> "count:" + args.length;

        assertEquals("count:0", function.apply());
        assertEquals("count:1", function.apply(true));
        assertEquals("count:2", function.apply(true, false));
        assertEquals("count:4", function.apply(true, false, true, false));
    }

    @Test
    public void testReturnDifferentTypes() {
        BooleanNFunction<Double> function = args -> {
            int trueCount = 0;
            for (boolean arg : args) {
                if (arg) trueCount++;
            }
            return args.length == 0 ? 0.0 : (double) trueCount / args.length;
        };

        assertEquals(0.0, function.apply(), 0.001);                // 0/0
        assertEquals(1.0, function.apply(true, true, true), 0.001); // 3/3
        assertEquals(0.5, function.apply(true, false), 0.001);      // 1/2
        assertEquals(0.6, function.apply(true, true, false, true, false), 0.001); // 3/5
    }
}
