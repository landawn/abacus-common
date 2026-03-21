package com.landawn.abacus.annotation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class LazyEvaluationTest extends TestBase {

    @LazyEvaluation
    static class TestLazyClass {
        @LazyEvaluation
        public String compute() {
            return "result";
        }
    }

    @Test
    public void testTypeAnnotation() {
        assertTrue(TestLazyClass.class.isAnnotationPresent(LazyEvaluation.class));
    }

    @Test
    public void testMethodAnnotation() throws NoSuchMethodException {
        Method method = TestLazyClass.class.getDeclaredMethod("compute");
        assertTrue(method.isAnnotationPresent(LazyEvaluation.class));
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = LazyEvaluation.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = LazyEvaluation.class.getAnnotation(Target.class);
        assertNotNull(target);
        ElementType[] expectedTargets = { ElementType.METHOD, ElementType.TYPE };
        assertArrayEquals(expectedTargets, target.value());
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(LazyEvaluation.class.isAnnotation());
    }

    @Test
    public void testAnnotationType() {
        LazyEvaluation annotation = TestLazyClass.class.getAnnotation(LazyEvaluation.class);
        assertNotNull(annotation);
        assertEquals(LazyEvaluation.class, annotation.annotationType());
    }

    @Test
    public void testNoMethods() {
        Method[] methods = LazyEvaluation.class.getDeclaredMethods();
        assertEquals(0, methods.length);
    }

    @Test
    public void testGetAnnotations() {
        Annotation[] annotations = LazyEvaluation.class.getAnnotations();
        assertTrue(annotations.length >= 2);
    }
}
