package com.landawn.abacus.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class BetaTest extends TestBase {

    @Beta
    static class TestClassWithBeta {
        @Beta
        private String field;

        @Beta
        public TestClassWithBeta() {
        }

        @Beta
        public void method() {
        }
    }

    @Beta
    @interface TestAnnotationWithBeta {
    }

    @Test
    public void testGetAnnotations() {
        Annotation[] annotations = Beta.class.getAnnotations();
        assertTrue(annotations.length >= 3);
    }

    @Test
    public void testNoMethods() {
        Method[] methods = Beta.class.getDeclaredMethods();
        assertEquals(0, methods.length);
    }
}
