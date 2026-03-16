package com.landawn.abacus.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class TerminalOpTriggeredTest extends TestBase {
    static class TestStream {
        @TerminalOpTriggered
        public TestStream sorted() {
            return this;
        }

        @TerminalOpTriggered
        public TestStream reversed() {
            return this;
        }
    }

    @Test
    public void testNoMethods() {
        Method[] methods = TerminalOpTriggered.class.getDeclaredMethods();
        assertEquals(0, methods.length);
    }
}
