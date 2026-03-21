package com.landawn.abacus.annotation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class JsonXmlCreatorTest extends TestBase {

    static class TestClass {
        @JsonXmlCreator
        public TestClass() {
        }

        @JsonXmlCreator
        public static TestClass create() {
            return new TestClass();
        }

        public TestClass(String value) {
        }
    }

    @Test
    public void testConstructorAnnotation() throws NoSuchMethodException {
        Constructor<?> constructor = TestClass.class.getDeclaredConstructor();
        assertTrue(constructor.isAnnotationPresent(JsonXmlCreator.class));
    }

    @Test
    public void testMethodAnnotation() throws NoSuchMethodException {
        Method method = TestClass.class.getDeclaredMethod("create");
        assertTrue(method.isAnnotationPresent(JsonXmlCreator.class));
    }

    @Test
    public void testNonAnnotatedConstructor() throws NoSuchMethodException {
        Constructor<?> constructor = TestClass.class.getDeclaredConstructor(String.class);
        assertTrue(!constructor.isAnnotationPresent(JsonXmlCreator.class));
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = JsonXmlCreator.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = JsonXmlCreator.class.getAnnotation(Target.class);
        assertNotNull(target);
        ElementType[] expectedTargets = { ElementType.METHOD, ElementType.CONSTRUCTOR };
        assertArrayEquals(expectedTargets, target.value());
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(JsonXmlCreator.class.isAnnotation());
    }

    @Test
    public void testAnnotationType() throws NoSuchMethodException {
        Constructor<?> constructor = TestClass.class.getDeclaredConstructor();
        JsonXmlCreator annotation = constructor.getAnnotation(JsonXmlCreator.class);
        assertNotNull(annotation);
        assertEquals(JsonXmlCreator.class, annotation.annotationType());
    }

    @Test
    public void testNoMethods() {
        Method[] methods = JsonXmlCreator.class.getDeclaredMethods();
        assertEquals(0, methods.length);
    }

    @Test
    public void testGetAnnotations() {
        Annotation[] annotations = JsonXmlCreator.class.getAnnotations();
        assertTrue(annotations.length >= 2);
    }
}
