package com.landawn.abacus.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class InternalTest extends TestBase {

    @Internal
    static class TestInternalClass {
        @Internal
        private String field;

        @Internal
        public TestInternalClass() {
        }

        @Internal
        public void method() {
        }
    }

    @Internal
    @interface TestInternalAnnotation {
    }

    @Test
    public void testNoMethods() {
        Method[] methods = Internal.class.getDeclaredMethods();
        assertEquals(0, methods.length);
    }

    @Test
    public void testGetAnnotations() {
        Annotation[] annotations = Internal.class.getAnnotations();
        assertTrue(annotations.length >= 3);
    }
}
