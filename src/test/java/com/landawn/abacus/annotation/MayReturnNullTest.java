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
import java.lang.reflect.Method;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class MayReturnNullTest extends TestBase {

    static class TestClass {
        @MayReturnNull
        public String findValue() {
            return null;
        }

        public String getValue() {
            return "value";
        }
    }

    @Test
    public void testMethodAnnotation() throws NoSuchMethodException {
        Method method = TestClass.class.getDeclaredMethod("findValue");
        assertTrue(method.isAnnotationPresent(MayReturnNull.class));
    }

    @Test
    public void testNonAnnotatedMethod() throws NoSuchMethodException {
        Method method = TestClass.class.getDeclaredMethod("getValue");
        assertTrue(!method.isAnnotationPresent(MayReturnNull.class));
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = MayReturnNull.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = MayReturnNull.class.getAnnotation(Target.class);
        assertNotNull(target);
        ElementType[] expectedTargets = { ElementType.METHOD };
        assertArrayEquals(expectedTargets, target.value());
    }

    @Test
    public void testDocumented() {
        assertTrue(MayReturnNull.class.isAnnotationPresent(Documented.class));
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(MayReturnNull.class.isAnnotation());
    }

    @Test
    public void testAnnotationType() throws NoSuchMethodException {
        Method method = TestClass.class.getDeclaredMethod("findValue");
        MayReturnNull annotation = method.getAnnotation(MayReturnNull.class);
        assertNotNull(annotation);
        assertEquals(MayReturnNull.class, annotation.annotationType());
    }

    @Test
    public void testNoMethods() {
        Method[] methods = MayReturnNull.class.getDeclaredMethods();
        assertEquals(0, methods.length);
    }

    @Test
    public void testGetAnnotations() {
        Annotation[] annotations = MayReturnNull.class.getAnnotations();
        assertTrue(annotations.length >= 3);
    }
}
