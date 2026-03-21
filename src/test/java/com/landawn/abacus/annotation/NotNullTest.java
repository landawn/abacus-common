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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class NotNullTest extends TestBase {
    static class TestClass {
        @NotNull
        public String method(@NotNull String param) {
            return param;
        }

        @NotNull
        public TestClass(@NotNull String value) {
        }
    }

    @Test
    public void testMethodAnnotation() throws NoSuchMethodException {
        Method method = TestClass.class.getDeclaredMethod("method", String.class);
        assertTrue(method.isAnnotationPresent(NotNull.class));
    }

    @Test
    public void testParameterAnnotation() throws NoSuchMethodException {
        Method method = TestClass.class.getDeclaredMethod("method", String.class);
        Parameter[] params = method.getParameters();
        assertTrue(params[0].isAnnotationPresent(NotNull.class));
    }

    @Test
    public void testConstructorAnnotation() throws NoSuchMethodException {
        Constructor<?> constructor = TestClass.class.getDeclaredConstructor(String.class);
        assertTrue(constructor.isAnnotationPresent(NotNull.class));
    }

    @Test
    public void testConstructorParameterAnnotation() throws NoSuchMethodException {
        Constructor<?> constructor = TestClass.class.getDeclaredConstructor(String.class);
        Parameter[] params = constructor.getParameters();
        assertTrue(params[0].isAnnotationPresent(NotNull.class));
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = NotNull.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = NotNull.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(new ElementType[] { ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER }, target.value());
    }

    @Test
    public void testDocumented() {
        assertTrue(NotNull.class.isAnnotationPresent(Documented.class));
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(NotNull.class.isAnnotation());
    }

    @Test
    public void testAnnotationType() throws NoSuchMethodException {
        Method method = TestClass.class.getDeclaredMethod("method", String.class);
        NotNull annotation = method.getAnnotation(NotNull.class);
        assertNotNull(annotation);
        assertEquals(NotNull.class, annotation.annotationType());
    }

    @Test
    public void testNoMethods() {
        Method[] methods = NotNull.class.getDeclaredMethods();
        assertEquals(0, methods.length);
    }
}
