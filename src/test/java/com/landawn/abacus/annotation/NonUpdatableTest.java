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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class NonUpdatableTest extends TestBase {
    static class TestEntity {
        @NonUpdatable
        private String field;
    }

    @Test
    public void testFieldAnnotation() throws NoSuchFieldException {
        Field field = TestEntity.class.getDeclaredField("field");
        assertTrue(field.isAnnotationPresent(NonUpdatable.class));
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = NonUpdatable.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = NonUpdatable.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(new ElementType[] { ElementType.FIELD }, target.value());
    }

    @Test
    public void testDocumented() {
        assertTrue(NonUpdatable.class.isAnnotationPresent(Documented.class));
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(NonUpdatable.class.isAnnotation());
    }

    @Test
    public void testAnnotationType() throws NoSuchFieldException {
        Field field = TestEntity.class.getDeclaredField("field");
        NonUpdatable annotation = field.getAnnotation(NonUpdatable.class);
        assertNotNull(annotation);
        assertEquals(NonUpdatable.class, annotation.annotationType());
    }

    @Test
    public void testNoMethods() {
        Method[] methods = NonUpdatable.class.getDeclaredMethods();
        assertEquals(0, methods.length);
    }
}
