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
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class JsonXmlValueTest extends TestBase {

    static class TestClass {
        @JsonXmlValue
        private String value;

        @JsonXmlValue
        public String getValue() {
            return value;
        }

        private String other;
    }

    @Test
    public void testFieldAnnotation() throws NoSuchFieldException {
        Field field = TestClass.class.getDeclaredField("value");
        assertTrue(field.isAnnotationPresent(JsonXmlValue.class));
    }

    @Test
    public void testMethodAnnotation() throws NoSuchMethodException {
        Method method = TestClass.class.getDeclaredMethod("getValue");
        assertTrue(method.isAnnotationPresent(JsonXmlValue.class));
    }

    @Test
    public void testNonAnnotatedField() throws NoSuchFieldException {
        Field field = TestClass.class.getDeclaredField("other");
        assertTrue(!field.isAnnotationPresent(JsonXmlValue.class));
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = JsonXmlValue.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = JsonXmlValue.class.getAnnotation(Target.class);
        assertNotNull(target);
        ElementType[] expectedTargets = { ElementType.METHOD, ElementType.FIELD };
        assertArrayEquals(expectedTargets, target.value());
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(JsonXmlValue.class.isAnnotation());
    }

    @Test
    public void testAnnotationType() throws NoSuchFieldException {
        Field field = TestClass.class.getDeclaredField("value");
        JsonXmlValue annotation = field.getAnnotation(JsonXmlValue.class);
        assertNotNull(annotation);
        assertEquals(JsonXmlValue.class, annotation.annotationType());
    }

    @Test
    public void testNoMethods() {
        Method[] methods = JsonXmlValue.class.getDeclaredMethods();
        assertEquals(0, methods.length);
    }

    @Test
    public void testGetAnnotations() {
        Annotation[] annotations = JsonXmlValue.class.getAnnotations();
        assertTrue(annotations.length >= 2);
    }
}
