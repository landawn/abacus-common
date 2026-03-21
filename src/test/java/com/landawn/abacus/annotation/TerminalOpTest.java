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

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class TerminalOpTest extends TestBase {
    static class TestStream {
        @TerminalOp
        public void collect() {
        }

        @TerminalOp
        public int count() {
            return 0;
        }

        public TestStream filter() {
            return this;
        }
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = TerminalOp.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.CLASS, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = TerminalOp.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(new ElementType[] { ElementType.METHOD }, target.value());
    }

    @Test
    public void testDocumented() {
        assertTrue(TerminalOp.class.isAnnotationPresent(Documented.class));
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(TerminalOp.class.isAnnotation());
    }

    @Test
    public void testNoMethods() {
        Method[] methods = TerminalOp.class.getDeclaredMethods();
        assertEquals(0, methods.length);
    }
}
