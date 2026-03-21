package com.landawn.abacus.annotation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class UnsupportedOperationTest extends TestBase {
    static class TestClass {
        @UnsupportedOperation
        public void unsupportedMethod() {
            throw new java.lang.UnsupportedOperationException();
        }

        public void supportedMethod() {
        }
    }

    @Test
    public void testMethodAnnotation() throws NoSuchMethodException {
        Method method = TestClass.class.getDeclaredMethod("unsupportedMethod");
        assertTrue(method.isAnnotationPresent(UnsupportedOperation.class));
    }

    @Test
    public void testNonAnnotatedMethod() throws NoSuchMethodException {
        Method method = TestClass.class.getDeclaredMethod("supportedMethod");
        assertTrue(!method.isAnnotationPresent(UnsupportedOperation.class));
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = UnsupportedOperation.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = UnsupportedOperation.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(new ElementType[] { ElementType.METHOD }, target.value());
    }

    @Test
    public void testDocumented() {
        assertTrue(UnsupportedOperation.class.isAnnotationPresent(Documented.class));
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(UnsupportedOperation.class.isAnnotation());
    }

    @Test
    public void testAnnotationType() throws NoSuchMethodException {
        Method method = TestClass.class.getDeclaredMethod("unsupportedMethod");
        UnsupportedOperation annotation = method.getAnnotation(UnsupportedOperation.class);
        assertNotNull(annotation);
        assertEquals(UnsupportedOperation.class, annotation.annotationType());
    }

    @Test
    public void testNoMethods() {
        Method[] methods = UnsupportedOperation.class.getDeclaredMethods();
        assertEquals(0, methods.length);
    }
}
