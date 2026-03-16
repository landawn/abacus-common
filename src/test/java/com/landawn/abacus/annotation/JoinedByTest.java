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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class JoinedByTest extends TestBase {

    static class TestEntity {
        @JoinedBy
        private Object field1;

        @JoinedBy("id=userId")
        private Object field2;

        @JoinedBy({ "id=accountId" })
        private List<Object> field3;

        @JoinedBy({ "id=UserRole.userId", "UserRole.roleId=id" })
        private List<Object> field4;
    }

    @Test
    public void testFieldAnnotation() throws NoSuchFieldException {
        Field field = TestEntity.class.getDeclaredField("field1");
        assertTrue(field.isAnnotationPresent(JoinedBy.class));
    }

    @Test
    public void testDefaultValue() throws NoSuchFieldException {
        Field field = TestEntity.class.getDeclaredField("field1");
        JoinedBy annotation = field.getAnnotation(JoinedBy.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[] {}, annotation.value());
    }

    @Test
    public void testSingleJoinCondition() throws NoSuchFieldException {
        Field field = TestEntity.class.getDeclaredField("field2");
        JoinedBy annotation = field.getAnnotation(JoinedBy.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[] { "id=userId" }, annotation.value());
    }

    @Test
    public void testArrayJoinCondition() throws NoSuchFieldException {
        Field field = TestEntity.class.getDeclaredField("field3");
        JoinedBy annotation = field.getAnnotation(JoinedBy.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[] { "id=accountId" }, annotation.value());
    }

    @Test
    public void testManyToManyJoinCondition() throws NoSuchFieldException {
        Field field = TestEntity.class.getDeclaredField("field4");
        JoinedBy annotation = field.getAnnotation(JoinedBy.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[] { "id=UserRole.userId", "UserRole.roleId=id" }, annotation.value());
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = JoinedBy.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = JoinedBy.class.getAnnotation(Target.class);
        assertNotNull(target);
        ElementType[] expectedTargets = { ElementType.FIELD };
        assertArrayEquals(expectedTargets, target.value());
    }

    @Test
    public void testDocumented() {
        assertTrue(JoinedBy.class.isAnnotationPresent(Documented.class));
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(JoinedBy.class.isAnnotation());
    }

    @Test
    public void testAnnotationType() throws NoSuchFieldException {
        Field field = TestEntity.class.getDeclaredField("field1");
        JoinedBy annotation = field.getAnnotation(JoinedBy.class);
        assertNotNull(annotation);
        assertEquals(JoinedBy.class, annotation.annotationType());
    }

    @Test
    public void testValueMethodExists() throws NoSuchMethodException {
        Method valueMethod = JoinedBy.class.getDeclaredMethod("value");
        assertNotNull(valueMethod);
        assertEquals(String[].class, valueMethod.getReturnType());
    }

    @Test
    public void testGetAnnotations() {
        Annotation[] annotations = JoinedBy.class.getAnnotations();
        assertTrue(annotations.length >= 3);
    }
}
