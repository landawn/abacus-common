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
import java.util.Arrays;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Type.Scope;
import com.landawn.abacus.util.EnumType;

@Tag("2025")
public class TypeTest extends TestBase {
    static class TestClass {
        @Type
        private String field1;

        @Type(name = "CustomType", enumerated = EnumType.ORDINAL, scope = Scope.PERSISTENCE)
        private String field2;

        @Type(value = "legacy")
        @Deprecated
        private String field3;

        @Type
        public String getField() {
            return null;
        }
    }

    @Test
    public void testFieldAnnotation() throws NoSuchFieldException {
        Field field = TestClass.class.getDeclaredField("field1");
        assertTrue(field.isAnnotationPresent(Type.class));
    }

    @Test
    public void testMethodAnnotation() throws NoSuchMethodException {
        Method method = TestClass.class.getDeclaredMethod("getField");
        assertTrue(method.isAnnotationPresent(Type.class));
    }

    @Test
    public void testDefaultValues() throws NoSuchFieldException {
        Field field = TestClass.class.getDeclaredField("field1");
        Type annotation = field.getAnnotation(Type.class);
        assertNotNull(annotation);
        assertEquals("", annotation.name());
        assertEquals("", annotation.value());
        assertEquals(com.landawn.abacus.type.Type.class, annotation.clazz());
        assertEquals(EnumType.NAME, annotation.enumerated());
        assertEquals(Scope.ALL, annotation.scope());
    }

    @Test
    public void testCustomValues() throws NoSuchFieldException {
        Field field = TestClass.class.getDeclaredField("field2");
        Type annotation = field.getAnnotation(Type.class);
        assertNotNull(annotation);
        assertEquals("CustomType", annotation.name());
        assertEquals(EnumType.ORDINAL, annotation.enumerated());
        assertEquals(Scope.PERSISTENCE, annotation.scope());
    }

    @Test
    public void testDeprecatedValue() throws NoSuchFieldException {
        Field field = TestClass.class.getDeclaredField("field3");
        Type annotation = field.getAnnotation(Type.class);
        assertNotNull(annotation);
        assertEquals("legacy", annotation.value());
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = Type.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = Type.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(new ElementType[] { ElementType.FIELD, ElementType.METHOD }, target.value());
    }

    @Test
    public void testDocumented() {
        assertTrue(Type.class.isAnnotationPresent(Documented.class));
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(Type.class.isAnnotation());
    }

    @Test
    public void testEnumTypeEnum() {
        assertEquals(3, EnumType.values().length);
        assertTrue(Arrays.asList(EnumType.values()).contains(EnumType.NAME));
        assertTrue(Arrays.asList(EnumType.values()).contains(EnumType.ORDINAL));
        assertTrue(Arrays.asList(EnumType.values()).contains(EnumType.CODE));
    }

    @Test
    public void testScopeEnum() {
        assertEquals(3, Scope.values().length);
        assertTrue(Arrays.asList(Scope.values()).contains(Scope.SERIALIZATION));
        assertTrue(Arrays.asList(Scope.values()).contains(Scope.PERSISTENCE));
        assertTrue(Arrays.asList(Scope.values()).contains(Scope.ALL));
    }
}
