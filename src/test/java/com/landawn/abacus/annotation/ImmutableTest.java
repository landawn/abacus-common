package com.landawn.abacus.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ImmutableTest extends TestBase {

    @Immutable
    static class TestImmutableClass {
        private final String value;

        public TestImmutableClass(String value) {
            this.value = value;
        }

        @Immutable
        public String getValue() {
            return value;
        }

        @Immutable
        public TestImmutableClass withNewValue(String newValue) {
            return new TestImmutableClass(newValue);
        }
    }

    @Test
    public void testNoMethods() {
        Method[] methods = Immutable.class.getDeclaredMethods();
        assertEquals(0, methods.length);
    }

    @Test
    public void testGetAnnotations() {
        Annotation[] annotations = Immutable.class.getAnnotations();
        assertTrue(annotations.length >= 3);
    }
}
