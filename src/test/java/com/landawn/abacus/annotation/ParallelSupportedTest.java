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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ParallelSupportedTest extends TestBase {
    @ParallelSupported
    static class TestClass {
        @ParallelSupported
        public void method() {
        }
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = ParallelSupported.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.CLASS, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = ParallelSupported.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(new ElementType[] { ElementType.METHOD, ElementType.TYPE }, target.value());
    }

    @Test
    public void testDocumented() {
        assertTrue(ParallelSupported.class.isAnnotationPresent(Documented.class));
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(ParallelSupported.class.isAnnotation());
    }

    @Test
    public void testNoMethods() {
        Method[] methods = ParallelSupported.class.getDeclaredMethods();
        assertEquals(0, methods.length);
    }
}
