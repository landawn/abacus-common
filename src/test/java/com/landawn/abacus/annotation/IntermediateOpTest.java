package com.landawn.abacus.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class IntermediateOpTest extends TestBase {

    static class TestStream {
        @IntermediateOp
        public TestStream filter() {
            return this;
        }

        @IntermediateOp
        public TestStream map() {
            return this;
        }

        public void collect() {
        }
    }

    @Test
    public void testNoMethods() {
        Method[] methods = IntermediateOp.class.getDeclaredMethods();
        assertEquals(0, methods.length);
    }

    @Test
    public void testGetAnnotations() {
        Annotation[] annotations = IntermediateOp.class.getAnnotations();
        assertTrue(annotations.length >= 3);
    }
}
