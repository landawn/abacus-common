package com.landawn.abacus.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class MutableTest extends TestBase {

    @Mutable
    static class TestMutableClass {
        private int value;

        @Mutable
        public void setValue(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @Test
    public void testNoMethods() {
        Method[] methods = Mutable.class.getDeclaredMethods();
        assertEquals(0, methods.length);
    }

    @Test
    public void testGetAnnotations() {
        Annotation[] annotations = Mutable.class.getAnnotations();
        assertTrue(annotations.length >= 3);
    }
}
