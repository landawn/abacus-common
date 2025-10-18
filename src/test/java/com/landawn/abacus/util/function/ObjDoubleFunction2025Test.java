package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ObjDoubleFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        ObjDoubleFunction<String, String> function = (t, u) -> t + ":" + u;
        assertEquals("test:5.5", function.apply("test", 5.5));
    }

    @Test
    public void testApplyWithLambda() {
        ObjDoubleFunction<String, Integer> function = (t, u) -> (int)(t.length() * u);
        assertEquals(25, function.apply("hello", 5.0));
    }

    @Test
    public void testAndThen() {
        ObjDoubleFunction<String, String> function = (t, u) -> t + ":" + u;
        java.util.function.Function<String, String> after = String::toUpperCase;
        assertEquals("TEST:5.5", function.andThen(after).apply("test", 5.5));
    }

    @Test
    public void testWithNullObject() {
        ObjDoubleFunction<String, String> function = (t, u) -> t + ":" + u;
        assertEquals("null:5.5", function.apply(null, 5.5));
    }

    @Test
    public void testReturningNull() {
        ObjDoubleFunction<String, String> function = (t, u) -> null;
        assertNull(function.apply("test", 5.5));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ObjDoubleFunction.class.getAnnotation(FunctionalInterface.class));
    }
}
