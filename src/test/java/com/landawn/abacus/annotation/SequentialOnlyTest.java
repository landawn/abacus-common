package com.landawn.abacus.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class SequentialOnlyTest extends TestBase {
    @SequentialOnly
    static class TestClass {
        @SequentialOnly
        public void method() {
        }
    }

    @Test
    public void testNoMethods() {
        Method[] methods = SequentialOnly.class.getDeclaredMethods();
        assertEquals(0, methods.length);
    }
}
