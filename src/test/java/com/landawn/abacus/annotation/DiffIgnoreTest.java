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

public class DiffIgnoreTest extends TestBase {

    static class TestClass {
        private String includedField;

        @DiffIgnore
        private String ignoredField;

        @DiffIgnore
        private int ignoredPrimitive;

        @DiffIgnore
        private Object ignoredObject;
    }

    @Test
    public void testFieldAnnotation() throws NoSuchFieldException {
        Field field = TestClass.class.getDeclaredField("ignoredField");
        assertTrue(field.isAnnotationPresent(DiffIgnore.class));
    }

    @Test
    public void testPrimitiveFieldAnnotation() throws NoSuchFieldException {
        Field field = TestClass.class.getDeclaredField("ignoredPrimitive");
        assertTrue(field.isAnnotationPresent(DiffIgnore.class));
    }

    @Test
    public void testObjectFieldAnnotation() throws NoSuchFieldException {
        Field field = TestClass.class.getDeclaredField("ignoredObject");
        assertTrue(field.isAnnotationPresent(DiffIgnore.class));
    }

    @Test
    public void testFieldWithoutAnnotation() throws NoSuchFieldException {
        Field field = TestClass.class.getDeclaredField("includedField");
        assertTrue(!field.isAnnotationPresent(DiffIgnore.class));
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = DiffIgnore.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = DiffIgnore.class.getAnnotation(Target.class);
        assertNotNull(target);
        ElementType[] expectedTargets = { ElementType.FIELD };
        assertArrayEquals(expectedTargets, target.value());
    }

    @Test
    public void testDocumented() {
        assertTrue(DiffIgnore.class.isAnnotationPresent(Documented.class));
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(DiffIgnore.class.isAnnotation());
    }

    @Test
    public void testAnnotationType() throws NoSuchFieldException {
        Field field = TestClass.class.getDeclaredField("ignoredField");
        DiffIgnore annotation = field.getAnnotation(DiffIgnore.class);
        assertNotNull(annotation);
        assertEquals(DiffIgnore.class, annotation.annotationType());
    }

    @Test
    public void testNoMethods() {
        Method[] methods = DiffIgnore.class.getDeclaredMethods();
        assertEquals(0, methods.length);
    }

    @Test
    public void testGetAnnotations() {
        Annotation[] annotations = DiffIgnore.class.getAnnotations();
        assertTrue(annotations.length >= 3);
    }
}
