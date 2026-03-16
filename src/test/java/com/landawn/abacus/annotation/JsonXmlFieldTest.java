package com.landawn.abacus.annotation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlField.Direction;
import com.landawn.abacus.util.EnumType;

@Tag("2025")
public class JsonXmlFieldTest extends TestBase {
    static class TestClass {
        @JsonXmlField
        private String field1;

        @JsonXmlField(name = "custom_name", aliases = { "alt1",
                "alt2" }, type = "String", enumerated = EnumType.ORDINAL, dateFormat = "yyyy-MM-dd", timeZone = "UTC", numberFormat = "#.##", ignore = true, isJsonRawValue = true, direction = Direction.SERIALIZE_ONLY)
        private String field2;
    }

    @Test
    public void testFieldAnnotation() throws NoSuchFieldException {
        Field field = TestClass.class.getDeclaredField("field1");
        assertTrue(field.isAnnotationPresent(JsonXmlField.class));
    }

    @Test
    public void testDefaultValues() throws NoSuchFieldException {
        Field field = TestClass.class.getDeclaredField("field1");
        JsonXmlField annotation = field.getAnnotation(JsonXmlField.class);
        assertNotNull(annotation);
        assertEquals("", annotation.name());
        assertArrayEquals(new String[] {}, annotation.aliases());
        assertEquals("", annotation.type());
        assertEquals(EnumType.NAME, annotation.enumerated());
        assertEquals("", annotation.dateFormat());
        assertEquals("", annotation.timeZone());
        assertEquals("", annotation.numberFormat());
        assertFalse(annotation.ignore());
        assertFalse(annotation.isJsonRawValue());
        assertEquals(Direction.BOTH, annotation.direction());
    }

    @Test
    public void testCustomValues() throws NoSuchFieldException {
        Field field = TestClass.class.getDeclaredField("field2");
        JsonXmlField annotation = field.getAnnotation(JsonXmlField.class);
        assertNotNull(annotation);
        assertEquals("custom_name", annotation.name());
        assertArrayEquals(new String[] { "alt1", "alt2" }, annotation.aliases());
        assertEquals("String", annotation.type());
        assertEquals(EnumType.ORDINAL, annotation.enumerated());
        assertEquals("yyyy-MM-dd", annotation.dateFormat());
        assertEquals("UTC", annotation.timeZone());
        assertEquals("#.##", annotation.numberFormat());
        assertTrue(annotation.ignore());
        assertTrue(annotation.isJsonRawValue());
        assertEquals(Direction.SERIALIZE_ONLY, annotation.direction());
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = JsonXmlField.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = JsonXmlField.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(new ElementType[] { ElementType.FIELD }, target.value());
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(JsonXmlField.class.isAnnotation());
    }

    @Test
    public void testExposeEnum() {
        assertEquals(3, Direction.values().length);
        assertTrue(Arrays.asList(Direction.values()).contains(Direction.BOTH));
        assertTrue(Arrays.asList(Direction.values()).contains(Direction.SERIALIZE_ONLY));
        assertTrue(Arrays.asList(Direction.values()).contains(Direction.DESERIALIZE_ONLY));
    }
}
