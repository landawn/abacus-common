package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.TriFunction;

public class FnFfTest extends FnTestSupport {

    @Test
    public void testFf_Function() {
        assertEquals(5, Fn.ff((Throwables.Function<String, Integer, Exception>) String::length).apply("hello"));

        final Throwables.Function<String, Integer, IOException> throwing = s -> {
            throw new IOException("e");
        };
        assertThrows(RuntimeException.class, () -> Fn.ff(throwing).apply("a"));
        assertEquals(-1, Fn.ff(throwing, -1).apply("a"));
        assertThrows(IllegalArgumentException.class, () -> Fn.ff((Throwables.Function<String, Integer, Exception>) null));
    }

    @Test
    public void testFf_Partial() {
        assertEquals("test:5", Fn.ff("test", (str, i) -> str + ":" + i).apply(5));
        assertEquals("hello", Fn.ff("hello", 5, (String s, Integer i, Boolean b) -> b ? s : String.valueOf(i)).apply(true));
        assertEquals("5", Fn.ff("hello", 5, (String s, Integer i, Boolean b) -> b ? s : String.valueOf(i)).apply(false));
        assertEquals("test-5", Fn.ff("-", (sep, s, i) -> s + sep + i).apply("test", 5));
        assertThrows(IllegalArgumentException.class, () -> Fn.ff("x", (Throwables.BiFunction<String, Integer, String, Exception>) null));
    }

    @Test
    public void testFf_BiAndTri() {
        final BiFunction<String, Integer, String> bi = Fn.ff((Throwables.BiFunction<String, Integer, String, Exception>) (s, i) -> s + i);
        assertEquals("a1", bi.apply("a", 1));
        final TriFunction<String, Integer, Boolean, String> tri = Fn.ff((s, i, b) -> s + i + b);
        assertEquals("a1true", tri.apply("a", 1, true));
        assertThrows(IllegalArgumentException.class, () -> Fn.ff((Throwables.BiFunction<String, Integer, String, Exception>) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.ff((Throwables.TriFunction<String, Integer, Boolean, String, Exception>) null));
    }

    @Test
    public void testFf_InterruptedRestoresInterrupt() {
        try {
            assertEquals("fallback", Fn.ff((Throwables.Function<String, String, InterruptedException>) value -> {
                throw new InterruptedException();
            }, "fallback").apply("value"));
            assertTrue(Thread.interrupted());

            assertEquals("fallback", Fn.ff((Throwables.BiFunction<String, String, String, InterruptedException>) (first, second) -> {
                throw new InterruptedException();
            }, "fallback").apply("first", "second"));
            assertTrue(Thread.interrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
