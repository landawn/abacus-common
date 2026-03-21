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

public class ColumnTest extends TestBase {

    static class TestEntity {
        @Column
        private String field1;

        @Column(name = "custom_name")
        private String field2;

        @Column(value = "old_value")
        @Deprecated
        private String field3;

        @Column(name = "new_name", value = "old_value")
        @Deprecated
        private String field4;
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = Column.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = Column.class.getAnnotation(Target.class);
        assertNotNull(target);
        ElementType[] expectedTargets = { ElementType.FIELD };
        assertArrayEquals(expectedTargets, target.value());
    }

    @Test
    public void testValueMethodExists() throws NoSuchMethodException {
        Method valueMethod = Column.class.getDeclaredMethod("value");
        assertNotNull(valueMethod);
        assertEquals(String.class, valueMethod.getReturnType());
    }

    @Test
    public void testDefaultValues() throws NoSuchFieldException {
        Field field = TestEntity.class.getDeclaredField("field1");
        Column annotation = field.getAnnotation(Column.class);
        assertNotNull(annotation);
        assertEquals("", annotation.name());
        assertEquals("", annotation.value());
    }

    @Test
    public void testCustomName() throws NoSuchFieldException {
        Field field = TestEntity.class.getDeclaredField("field2");
        Column annotation = field.getAnnotation(Column.class);
        assertNotNull(annotation);
        assertEquals("custom_name", annotation.name());
        assertEquals("", annotation.value());
    }

    @Test
    public void testDeprecatedValue() throws NoSuchFieldException {
        Field field = TestEntity.class.getDeclaredField("field3");
        Column annotation = field.getAnnotation(Column.class);
        assertNotNull(annotation);
        assertEquals("", annotation.name());
        assertEquals("old_value", annotation.value());
    }

    @Test
    public void testBothAttributes() throws NoSuchFieldException {
        Field field = TestEntity.class.getDeclaredField("field4");
        Column annotation = field.getAnnotation(Column.class);
        assertNotNull(annotation);
        assertEquals("new_name", annotation.name());
        assertEquals("old_value", annotation.value());
    }

    @Test
    public void testNameMethodExists() throws NoSuchMethodException {
        Method nameMethod = Column.class.getDeclaredMethod("name");
        assertNotNull(nameMethod);
        assertEquals(String.class, nameMethod.getReturnType());
    }

    @Test
    public void testFieldAnnotation() throws NoSuchFieldException {
        Field field = TestEntity.class.getDeclaredField("field1");
        assertTrue(field.isAnnotationPresent(Column.class));
    }

    @Test
    public void testDocumented() {
        assertTrue(Column.class.isAnnotationPresent(Documented.class));
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(Column.class.isAnnotation());
    }

    @Test
    public void testAnnotationType() throws NoSuchFieldException {
        Field field = TestEntity.class.getDeclaredField("field1");
        Column annotation = field.getAnnotation(Column.class);
        assertNotNull(annotation);
        assertEquals(Column.class, annotation.annotationType());
    }

    @Test
    public void testGetAnnotations() {
        Annotation[] annotations = Column.class.getAnnotations();
        assertTrue(annotations.length >= 3);
    }
}
