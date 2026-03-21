package com.landawn.abacus.annotation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.Exclusion;
import com.landawn.abacus.util.EnumType;
import com.landawn.abacus.util.NamingPolicy;

public class JsonXmlConfigTest extends TestBase {
    @JsonXmlConfig
    static class TestClass1 {
    }

    @JsonXmlConfig(namingPolicy = NamingPolicy.SCREAMING_SNAKE_CASE, dateFormat = "yyyy-MM-dd", timeZone = "UTC", numberFormat = "#.##", enumerated = EnumType.ORDINAL, exclusion = Exclusion.NONE, ignoredFields = {
            "password", "secret" })
    static class TestClass2 {
    }

    @Test
    public void testDefaultValues() {
        JsonXmlConfig annotation = TestClass1.class.getAnnotation(JsonXmlConfig.class);
        assertNotNull(annotation);
        assertEquals(NamingPolicy.CAMEL_CASE, annotation.namingPolicy());
        assertEquals("", annotation.dateFormat());
        assertEquals("", annotation.timeZone());
        assertEquals("", annotation.numberFormat());
        assertEquals(EnumType.NAME, annotation.enumerated());
        assertEquals(Exclusion.NULL, annotation.exclusion());
        assertArrayEquals(new String[] {}, annotation.ignoredFields());
    }

    @Test
    public void testCustomValues() {
        JsonXmlConfig annotation = TestClass2.class.getAnnotation(JsonXmlConfig.class);
        assertNotNull(annotation);
        assertEquals(NamingPolicy.SCREAMING_SNAKE_CASE, annotation.namingPolicy());
        assertEquals("yyyy-MM-dd", annotation.dateFormat());
        assertEquals("UTC", annotation.timeZone());
        assertEquals("#.##", annotation.numberFormat());
        assertEquals(EnumType.ORDINAL, annotation.enumerated());
        assertEquals(Exclusion.NONE, annotation.exclusion());
        assertArrayEquals(new String[] { "password", "secret" }, annotation.ignoredFields());
    }

    @Test
    public void testTypeAnnotation() {
        assertTrue(TestClass1.class.isAnnotationPresent(JsonXmlConfig.class));
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = JsonXmlConfig.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = JsonXmlConfig.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(new ElementType[] { ElementType.TYPE }, target.value());
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(JsonXmlConfig.class.isAnnotation());
    }

    @Test
    public void testAnnotationType() {
        JsonXmlConfig annotation = TestClass1.class.getAnnotation(JsonXmlConfig.class);
        assertNotNull(annotation);
        assertEquals(JsonXmlConfig.class, annotation.annotationType());
    }
}
