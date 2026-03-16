package com.landawn.abacus.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class RecordTest extends TestBase {
    @Record
    static class TestRecordClass {
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(Record.class.isAnnotation());
    }

    @Test
    public void testAnnotationType() {
        Record annotation = TestRecordClass.class.getAnnotation(Record.class);
        assertNotNull(annotation);
        assertEquals(Record.class, annotation.annotationType());
    }

    @Test
    public void testNoMethods() {
        Method[] methods = Record.class.getDeclaredMethods();
        assertEquals(0, methods.length);
    }
}
