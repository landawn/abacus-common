package com.landawn.abacus.eventbus;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ThreadMode;

@Tag("2025")
public class Subscribe2025Test extends TestBase {

    static class TestHandler {
        @Subscribe
        public void handleDefault(String event) {
        }

        @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR)
        public void handleAsync(String event) {
        }

        @Subscribe(eventId = "myEvents")
        public void handleWithEventId(String event) {
        }

        @Subscribe(sticky = true)
        public void handleSticky(String event) {
        }

        @Subscribe(strictEventType = true)
        public void handleStrict(String event) {
        }

        @Subscribe(interval = 1000)
        public void handleWithInterval(String event) {
        }

        @Subscribe(deduplicate = true)
        public void handleDeduplicate(String event) {
        }

        @Subscribe(threadMode = ThreadMode.THREAD_POOL_EXECUTOR, eventId = "testId", sticky = true, strictEventType = true, interval = 5000, deduplicate = true)
        public void handleAll(String event) {
        }
    }

    @Test
    public void testMethodAnnotation() throws NoSuchMethodException {
        Method method = TestHandler.class.getDeclaredMethod("handleDefault", String.class);
        assertTrue(method.isAnnotationPresent(Subscribe.class));
    }

    @Test
    public void testDefaultValues() throws NoSuchMethodException {
        Method method = TestHandler.class.getDeclaredMethod("handleDefault", String.class);
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        assertNotNull(annotation);
        assertEquals(ThreadMode.DEFAULT, annotation.threadMode());
        assertFalse(annotation.strictEventType());
        assertFalse(annotation.sticky());
        assertEquals("", annotation.eventId());
        assertEquals(0, annotation.interval());
        assertFalse(annotation.deduplicate());
    }

    @Test
    public void testThreadMode() throws NoSuchMethodException {
        Method method = TestHandler.class.getDeclaredMethod("handleAsync", String.class);
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        assertEquals(ThreadMode.THREAD_POOL_EXECUTOR, annotation.threadMode());
    }

    @Test
    public void testEventId() throws NoSuchMethodException {
        Method method = TestHandler.class.getDeclaredMethod("handleWithEventId", String.class);
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        assertEquals("myEvents", annotation.eventId());
    }

    @Test
    public void testSticky() throws NoSuchMethodException {
        Method method = TestHandler.class.getDeclaredMethod("handleSticky", String.class);
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        assertTrue(annotation.sticky());
    }

    @Test
    public void testStrictEventType() throws NoSuchMethodException {
        Method method = TestHandler.class.getDeclaredMethod("handleStrict", String.class);
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        assertTrue(annotation.strictEventType());
    }

    @Test
    public void testInterval() throws NoSuchMethodException {
        Method method = TestHandler.class.getDeclaredMethod("handleWithInterval", String.class);
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        assertEquals(1000, annotation.interval());
    }

    @Test
    public void testDeduplicate() throws NoSuchMethodException {
        Method method = TestHandler.class.getDeclaredMethod("handleDeduplicate", String.class);
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        assertTrue(annotation.deduplicate());
    }

    @Test
    public void testAllAttributes() throws NoSuchMethodException {
        Method method = TestHandler.class.getDeclaredMethod("handleAll", String.class);
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        assertNotNull(annotation);
        assertEquals(ThreadMode.THREAD_POOL_EXECUTOR, annotation.threadMode());
        assertEquals("testId", annotation.eventId());
        assertTrue(annotation.sticky());
        assertTrue(annotation.strictEventType());
        assertEquals(5000, annotation.interval());
        assertTrue(annotation.deduplicate());
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = Subscribe.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = Subscribe.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(new ElementType[] { ElementType.METHOD }, target.value());
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(Subscribe.class.isAnnotation());
    }

    @Test
    public void testAnnotationType() throws NoSuchMethodException {
        Method method = TestHandler.class.getDeclaredMethod("handleDefault", String.class);
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        assertNotNull(annotation);
        assertEquals(Subscribe.class, annotation.annotationType());
    }
}
