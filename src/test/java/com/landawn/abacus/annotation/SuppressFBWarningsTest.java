package com.landawn.abacus.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class SuppressFBWarningsTest extends TestBase {
    @SuppressFBWarnings
    static class TestClass {
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH")
    static class TestClass2 {
    }

    @SuppressFBWarnings(value = { "EI_EXPOSE_REP", "EI_EXPOSE_REP2" }, justification = "Intentional")
    static class TestClass3 {
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = SuppressFBWarnings.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.CLASS, retention.value());
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(SuppressFBWarnings.class.isAnnotation());
    }

    @Test
    public void testValueMethodExists() throws NoSuchMethodException {
        Method valueMethod = SuppressFBWarnings.class.getDeclaredMethod("value");
        assertNotNull(valueMethod);
        assertEquals(String[].class, valueMethod.getReturnType());
    }

    @Test
    public void testJustificationMethodExists() throws NoSuchMethodException {
        Method justificationMethod = SuppressFBWarnings.class.getDeclaredMethod("justification");
        assertNotNull(justificationMethod);
        assertEquals(String.class, justificationMethod.getReturnType());
    }
}
