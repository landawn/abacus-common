package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ObjIntFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        ObjIntFunction<String, String> function = (t, u) -> t + ":" + u;
        assertEquals("test:5", function.apply("test", 5));
    }

    @Test
    public void testAndThen() {
        ObjIntFunction<String, String> function = (t, u) -> t + ":" + u;
        assertEquals("TEST:5", function.andThen(String::toUpperCase).apply("test", 5));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ObjIntFunction.class.getAnnotation(FunctionalInterface.class));
    }
}
