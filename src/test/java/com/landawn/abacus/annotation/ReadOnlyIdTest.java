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
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ReadOnlyIdTest extends TestBase {
    @ReadOnlyId
    static class TestEntity1 {
        @ReadOnlyId
        private Long id;
    }

    @ReadOnlyId({ "order_id", "line_number" })
    static class TestEntity2 {
    }

    @Test
    public void testCompositeKey() {
        ReadOnlyId annotation = TestEntity2.class.getAnnotation(ReadOnlyId.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[] { "order_id", "line_number" }, annotation.value());
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = ReadOnlyId.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = ReadOnlyId.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(new ElementType[] { ElementType.FIELD, ElementType.TYPE }, target.value());
    }

    @Test
    public void testDefaultValue() throws NoSuchFieldException {
        Field field = TestEntity1.class.getDeclaredField("id");
        ReadOnlyId annotation = field.getAnnotation(ReadOnlyId.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[] {}, annotation.value());
    }

    @Test
    public void testValueMethodExists() throws NoSuchMethodException {
        Method valueMethod = ReadOnlyId.class.getDeclaredMethod("value");
        assertNotNull(valueMethod);
        assertEquals(String[].class, valueMethod.getReturnType());
    }

    @Test
    public void testTypeAnnotation() {
        assertTrue(TestEntity1.class.isAnnotationPresent(ReadOnlyId.class));
    }

    @Test
    public void testFieldAnnotation() throws NoSuchFieldException {
        Field field = TestEntity1.class.getDeclaredField("id");
        assertTrue(field.isAnnotationPresent(ReadOnlyId.class));
    }

    @Test
    public void testDocumented() {
        assertTrue(ReadOnlyId.class.isAnnotationPresent(Documented.class));
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(ReadOnlyId.class.isAnnotation());
    }

    @Test
    public void testAnnotationType() {
        ReadOnlyId annotation = TestEntity1.class.getAnnotation(ReadOnlyId.class);
        assertNotNull(annotation);
        assertEquals(ReadOnlyId.class, annotation.annotationType());
    }
}
