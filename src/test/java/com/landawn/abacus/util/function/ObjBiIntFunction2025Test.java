package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ObjBiIntFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        ObjBiIntFunction<String, String> function = (t, i, j) -> t.substring(i, j);

        String result = function.apply("Hello World", 0, 5);

        assertEquals("Hello", result);
    }

    @Test
    public void testApplyWithLambda() {
        ObjBiIntFunction<String, Integer> function = (t, i, j) -> t.substring(i, j).length();

        Integer result = function.apply("Testing", 1, 4);

        assertEquals(3, result);
    }

    @Test
    public void testApplyWithAnonymousClass() {
        ObjBiIntFunction<String, String> function = new ObjBiIntFunction<String, String>() {
            @Override
            public String apply(String t, int i, int j) {
                return String.format("%s[%d to %d]", t, i, j);
            }
        };

        String result = function.apply("test", 10, 20);

        assertEquals("test[10 to 20]", result);
    }

    @Test
    public void testAndThen() {
        ObjBiIntFunction<String, String> function = (t, i, j) -> t.substring(i, j);
        java.util.function.Function<String, String> after = String::toUpperCase;

        ObjBiIntFunction<String, String> combined = function.andThen(after);
        String result = combined.apply("hello world", 0, 5);

        assertEquals("HELLO", result);
    }

    @Test
    public void testAndThenChaining() {
        ObjBiIntFunction<String, String> function = (t, i, j) -> t.substring(i, j);
        java.util.function.Function<String, String> after1 = String::toUpperCase;
        java.util.function.Function<String, String> after2 = s -> "Result: " + s;

        ObjBiIntFunction<String, String> combined = function.andThen(after1).andThen(after2);
        String result = combined.apply("hello", 0, 3);

        assertEquals("Result: HEL", result);
    }

    @Test
    public void testWithNegativeInts() {
        ObjBiIntFunction<String, String> function = (t, i, j) -> t + ":" + i + ":" + j;

        String result = function.apply("test", -5, -10);

        assertEquals("test:-5:-10", result);
    }

    @Test
    public void testWithZeroInts() {
        ObjBiIntFunction<String, String> function = (t, i, j) -> t + ":" + i + ":" + j;

        String result = function.apply("test", 0, 0);

        assertEquals("test:0:0", result);
    }

    @Test
    public void testWithBoundaryValues() {
        ObjBiIntFunction<String, String> function = (t, i, j) -> t + ":" + i + ":" + j;

        String result = function.apply("max", Integer.MAX_VALUE, Integer.MIN_VALUE);

        assertEquals("max:" + Integer.MAX_VALUE + ":" + Integer.MIN_VALUE, result);
    }

    @Test
    public void testWithNullObject() {
        ObjBiIntFunction<String, String> function = (t, i, j) -> t + ":" + i + ":" + j;

        String result = function.apply(null, 1, 2);

        assertEquals("null:1:2", result);
    }

    @Test
    public void testReturningNull() {
        ObjBiIntFunction<String, String> function = (t, i, j) -> null;

        String result = function.apply("test", 1, 2);

        assertNull(result);
    }

    @Test
    public void testWithDifferentReturnType() {
        ObjBiIntFunction<String, Boolean> function = (t, i, j) -> j > i && t.length() >= j;

        Boolean result1 = function.apply("Hello", 0, 3);
        Boolean result2 = function.apply("Hi", 0, 5);

        assertEquals(true, result1);
        assertEquals(false, result2);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ObjBiIntFunction.class.getAnnotation(FunctionalInterface.class));
    }
}
