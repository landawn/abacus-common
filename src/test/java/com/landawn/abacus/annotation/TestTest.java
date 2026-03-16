package com.landawn.abacus.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class TestTest extends TestBase {
    @com.landawn.abacus.annotation.Test
    static class TestClass {
        @com.landawn.abacus.annotation.Test
        private String field;

        @com.landawn.abacus.annotation.Test
        public TestClass() {
        }

        @com.landawn.abacus.annotation.Test
        public void method() {
        }
    }

    @com.landawn.abacus.annotation.Test
    @interface TestAnnotation {
    }

    @Test
    public void testNoMethods() {
        Method[] methods = com.landawn.abacus.annotation.Test.class.getDeclaredMethods();
        assertEquals(0, methods.length);
    }
}
