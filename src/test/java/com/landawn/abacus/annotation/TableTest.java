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
public class TableTest extends TestBase {
    @Table
    static class TestEntity1 {
    }

    @Table(name = "users")
    static class TestEntity2 {
    }

    @Table(name = "orders", alias = "o")
    static class TestEntity3 {
    }

    @Table(name = "products", columnFields = { "id", "name" })
    static class TestEntity4 {
    }

    @Table(name = "customers", nonColumnFields = { "temp" })
    static class TestEntity5 {
    }

    @Table(value = "legacy", name = "new_table", alias = "nt", columnFields = { "id" }, nonColumnFields = { "old" })
    @Deprecated
    static class TestEntity6 {
    }

    @Test
    public void testTypeAnnotation() {
        assertTrue(TestEntity1.class.isAnnotationPresent(Table.class));
    }

    @Test
    public void testDefaultValues() {
        Table annotation = TestEntity1.class.getAnnotation(Table.class);
        assertNotNull(annotation);
        assertEquals("", annotation.name());
        assertEquals("", annotation.alias());
        assertEquals("", annotation.value());
        assertArrayEquals(new String[] {}, annotation.columnFields());
        assertArrayEquals(new String[] {}, annotation.nonColumnFields());
    }

    @Test
    public void testTableName() {
        Table annotation = TestEntity2.class.getAnnotation(Table.class);
        assertNotNull(annotation);
        assertEquals("users", annotation.name());
    }

    @Test
    public void testTableNameAndAlias() {
        Table annotation = TestEntity3.class.getAnnotation(Table.class);
        assertNotNull(annotation);
        assertEquals("orders", annotation.name());
        assertEquals("o", annotation.alias());
    }

    @Test
    public void testColumnFields() {
        Table annotation = TestEntity4.class.getAnnotation(Table.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[] { "id", "name" }, annotation.columnFields());
    }

    @Test
    public void testNonColumnFields() {
        Table annotation = TestEntity5.class.getAnnotation(Table.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[] { "temp" }, annotation.nonColumnFields());
    }

    @Test
    public void testAllAttributes() {
        Table annotation = TestEntity6.class.getAnnotation(Table.class);
        assertNotNull(annotation);
        assertEquals("legacy", annotation.value());
        assertEquals("new_table", annotation.name());
        assertEquals("nt", annotation.alias());
        assertArrayEquals(new String[] { "id" }, annotation.columnFields());
        assertArrayEquals(new String[] { "old" }, annotation.nonColumnFields());
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = Table.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = Table.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(new ElementType[] { ElementType.TYPE }, target.value());
    }

    @Test
    public void testDocumented() {
        assertTrue(Table.class.isAnnotationPresent(Documented.class));
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(Table.class.isAnnotation());
    }

    @Test
    public void testAnnotationType() {
        Table annotation = TestEntity1.class.getAnnotation(Table.class);
        assertNotNull(annotation);
        assertEquals(Table.class, annotation.annotationType());
    }

    @Test
    public void testValueMethodExists() throws NoSuchMethodException {
        Method method = Table.class.getDeclaredMethod("value");
        assertNotNull(method);
        assertEquals(String.class, method.getReturnType());
    }

    @Test
    public void testNameMethodExists() throws NoSuchMethodException {
        Method method = Table.class.getDeclaredMethod("name");
        assertNotNull(method);
        assertEquals(String.class, method.getReturnType());
    }

    @Test
    public void testAliasMethodExists() throws NoSuchMethodException {
        Method method = Table.class.getDeclaredMethod("alias");
        assertNotNull(method);
        assertEquals(String.class, method.getReturnType());
    }

    @Test
    public void testColumnFieldsMethodExists() throws NoSuchMethodException {
        Method method = Table.class.getDeclaredMethod("columnFields");
        assertNotNull(method);
        assertEquals(String[].class, method.getReturnType());
    }

    @Test
    public void testNonColumnFieldsMethodExists() throws NoSuchMethodException {
        Method method = Table.class.getDeclaredMethod("nonColumnFields");
        assertNotNull(method);
        assertEquals(String[].class, method.getReturnType());
    }
}
