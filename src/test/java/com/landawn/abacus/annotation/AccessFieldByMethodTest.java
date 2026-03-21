package com.landawn.abacus.annotation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class AccessFieldByMethodTest extends TestBase {

    @AccessFieldByMethod
    static class TestClassWithAnnotation {
        @AccessFieldByMethod("customValue")
        private String field1;

        @AccessFieldByMethod
        private String field2;

        @AccessFieldByMethod
        public String getMethod() {
            return "test";
        }
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = AccessFieldByMethod.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = AccessFieldByMethod.class.getAnnotation(Target.class);
        assertNotNull(target);
        ElementType[] expectedTargets = { ElementType.TYPE, ElementType.METHOD, ElementType.FIELD };
        assertArrayEquals(expectedTargets, target.value());
    }

    @Test
    public void testDefaultValue() throws NoSuchFieldException {
        Field field = TestClassWithAnnotation.class.getDeclaredField("field2");
        AccessFieldByMethod annotation = field.getAnnotation(AccessFieldByMethod.class);
        assertNotNull(annotation);
        assertEquals("", annotation.value());
    }

    @Test
    public void testCustomValue() throws NoSuchFieldException {
        Field field = TestClassWithAnnotation.class.getDeclaredField("field1");
        AccessFieldByMethod annotation = field.getAnnotation(AccessFieldByMethod.class);
        assertNotNull(annotation);
        assertEquals("customValue", annotation.value());
    }

    @Test
    public void testValueMethod() throws NoSuchMethodException {
        Method valueMethod = AccessFieldByMethod.class.getDeclaredMethod("value");
        assertNotNull(valueMethod);
        assertEquals(String.class, valueMethod.getReturnType());
    }

    @Test
    public void testAnnotationIsPresent() {
        assertTrue(TestClassWithAnnotation.class.isAnnotationPresent(AccessFieldByMethod.class));
    }

    @Test
    public void testFieldAnnotation() throws NoSuchFieldException {
        Field field1 = TestClassWithAnnotation.class.getDeclaredField("field1");
        assertTrue(field1.isAnnotationPresent(AccessFieldByMethod.class));

        Field field2 = TestClassWithAnnotation.class.getDeclaredField("field2");
        assertTrue(field2.isAnnotationPresent(AccessFieldByMethod.class));
    }

    @Test
    public void testMethodAnnotation() throws NoSuchMethodException {
        Method method = TestClassWithAnnotation.class.getDeclaredMethod("getMethod");
        assertTrue(method.isAnnotationPresent(AccessFieldByMethod.class));
    }

    @Test
    public void testDocumented() {
        assertTrue(AccessFieldByMethod.class.isAnnotationPresent(Documented.class));
    }

    @Test
    public void testAnnotationType() {
        AccessFieldByMethod annotation = TestClassWithAnnotation.class.getAnnotation(AccessFieldByMethod.class);
        assertNotNull(annotation);
        assertEquals(AccessFieldByMethod.class, annotation.annotationType());
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(AccessFieldByMethod.class.isAnnotation());
    }

    @Test
    public void testGetAnnotations() {
        Annotation[] annotations = AccessFieldByMethod.class.getAnnotations();
        assertTrue(annotations.length >= 3); // At least Documented, Retention, Target
    }
}
