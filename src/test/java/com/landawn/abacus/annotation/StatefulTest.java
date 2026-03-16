package com.landawn.abacus.annotation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
public class StatefulTest extends TestBase {
    @Stateful
    static class TestClass {
        @Stateful
        private int field;

        @Stateful
        public void method() {
        }
    }

    @Test
    public void testTypeAnnotation() {
        assertFalse(TestClass.class.isAnnotationPresent(Stateful.class));
    }

    @Test
    public void testFieldAnnotation() throws NoSuchFieldException {
        Field field = TestClass.class.getDeclaredField("field");
        assertFalse(field.isAnnotationPresent(Stateful.class));
    }

    @Test
    public void testMethodAnnotation() throws NoSuchMethodException {
        Method method = TestClass.class.getDeclaredMethod("method");
        assertFalse(method.isAnnotationPresent(Stateful.class));
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = Stateful.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.CLASS, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = Stateful.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(new ElementType[] { ElementType.FIELD, ElementType.METHOD, ElementType.TYPE }, target.value());
    }

    @Test
    public void testDocumented() {
        assertTrue(Stateful.class.isAnnotationPresent(Documented.class));
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(Stateful.class.isAnnotation());
    }

    @Test
    public void testNoMethods() {
        Method[] methods = Stateful.class.getDeclaredMethods();
        assertEquals(0, methods.length);
    }
}
