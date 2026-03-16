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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class IdTest extends TestBase {

    @Id
    static class TestEntity1 {
        @Id
        private Long id;
    }

    @Id({ "company_id", "employee_id" })
    static class TestEntity2 {
        @Id({ "user_id" })
        private Long userId;
    }

    @Test
    public void testTypeAnnotation() {
        assertTrue(TestEntity1.class.isAnnotationPresent(Id.class));
    }

    @Test
    public void testFieldAnnotation() throws NoSuchFieldException {
        Field field = TestEntity1.class.getDeclaredField("id");
        assertTrue(field.isAnnotationPresent(Id.class));
    }

    @Test
    public void testDefaultValue() throws NoSuchFieldException {
        Field field = TestEntity1.class.getDeclaredField("id");
        Id annotation = field.getAnnotation(Id.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[] {}, annotation.value());
    }

    @Test
    public void testSingleColumnValue() throws NoSuchFieldException {
        Field field = TestEntity2.class.getDeclaredField("userId");
        Id annotation = field.getAnnotation(Id.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[] { "user_id" }, annotation.value());
    }

    @Test
    public void testCompositeKeyValue() {
        Id annotation = TestEntity2.class.getAnnotation(Id.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[] { "company_id", "employee_id" }, annotation.value());
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = Id.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = Id.class.getAnnotation(Target.class);
        assertNotNull(target);
        ElementType[] expectedTargets = { ElementType.FIELD, ElementType.TYPE };
        assertArrayEquals(expectedTargets, target.value());
    }

    @Test
    public void testDocumented() {
        assertTrue(Id.class.isAnnotationPresent(Documented.class));
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(Id.class.isAnnotation());
    }

    @Test
    public void testAnnotationType() {
        Id annotation = TestEntity1.class.getAnnotation(Id.class);
        assertNotNull(annotation);
        assertEquals(Id.class, annotation.annotationType());
    }

    @Test
    public void testValueMethodExists() throws NoSuchMethodException {
        Method valueMethod = Id.class.getDeclaredMethod("value");
        assertNotNull(valueMethod);
        assertEquals(String[].class, valueMethod.getReturnType());
    }

    @Test
    public void testGetAnnotations() {
        Annotation[] annotations = Id.class.getAnnotations();
        assertTrue(annotations.length >= 3);
    }

    @Test
    public void testEmptyArrayDefault() {
        Id annotation = TestEntity1.class.getAnnotation(Id.class);
        assertNotNull(annotation);
        assertEquals(0, annotation.value().length);
    }
}
