package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.Callable;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Runnable;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.UnaryOperator;

public class FnFromTest extends FnTestSupport {

    @Test
    public void testFrom() {
        final java.util.function.Supplier<String> javaSupplier = () -> "x";
        assertEquals("x", Fn.from(javaSupplier).get());
        final Supplier<String> already = () -> "y";
        assertSame(already, Fn.from(already));

        assertEquals(5, Fn.from((java.util.function.Function<String, Integer>) String::length).apply("hello"));
        assertTrue(Fn.from((java.util.function.Predicate<String>) s -> s.isEmpty()).test(""));
        Fn.from((java.util.function.Consumer<String>) s -> {
        }).accept("x");
        assertEquals("ab", Fn.from((java.util.function.BiFunction<String, String, String>) String::concat).apply("a", "b"));
        assertEquals("HI", Fn.from((java.util.function.UnaryOperator<String>) String::toUpperCase).apply("hi"));
        assertEquals("ab", Fn.from((java.util.function.BinaryOperator<String>) String::concat).apply("a", "b"));
        assertEquals("x", Fn.from((java.util.function.IntFunction<String>) i -> "x").apply(1));
        assertThrows(IllegalArgumentException.class, () -> Fn.from((java.util.function.Function<String, String>) null));
    }

    @Test
    public void testS() {
        final Supplier<String> s = () -> "value";
        assertSame(s, Fn.s(s));
        assertEquals(4, Fn.s("test", String::length).get());
        assertThrows(IllegalArgumentException.class, () -> Fn.s(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.s("x", null));
    }

    @Test
    public void testP() {
        final Predicate<String> p = String::isEmpty;
        assertSame(p, Fn.p(p));
        assertTrue(Fn.p("ab", (String prefix, String s) -> s.startsWith(prefix)).test("abc"));
        assertTrue(Fn.p("a", "b", (x, y, z) -> (x + y).equals(z)).test("ab"));
        final BiPredicate<String, Integer> bi = (a, b) -> a.length() == b;
        assertSame(bi, Fn.p(bi));
        assertThrows(IllegalArgumentException.class, () -> Fn.p((Predicate<String>) null));
    }

    @Test
    public void testC() {
        final List<String> seen = new ArrayList<>();
        final Consumer<String> c = seen::add;
        assertSame(c, Fn.c(c));
        seen.clear();
        Fn.c("pre-", (String prefix, String v) -> seen.add(prefix + v)).accept("x");
        assertEquals("pre-x", seen.get(0));
        assertThrows(IllegalArgumentException.class, () -> Fn.c((Consumer<String>) null));
    }

    @Test
    public void testF() {
        final Function<String, Integer> f = String::length;
        assertSame(f, Fn.f(f));
        assertEquals("test-1", Fn.f("test-", (prefix, i) -> prefix + i).apply(1));
        assertEquals("a-1-true", Fn.f("a", 1, (x, y, z) -> x + "-" + y + "-" + z).apply(true));
        final BiFunction<String, Integer, String> bi = (s, i) -> s + i;
        assertSame(bi, Fn.f(bi));
        final TriFunction<String, Integer, Boolean, String> tri = (s, i, b) -> s + i + b;
        assertSame(tri, Fn.f(tri));
        assertThrows(IllegalArgumentException.class, () -> Fn.f((Function<String, String>) null));
    }

    @Test
    public void testO() {
        final UnaryOperator<String> upper = Fn.o((String s) -> s.toUpperCase());
        assertEquals("HELLO", upper.apply("hello"));
        final BinaryOperator<String> concat = Fn.o((String a, String b) -> a + b);
        assertEquals("ab", concat.apply("a", "b"));
        assertThrows(IllegalArgumentException.class, () -> Fn.o((UnaryOperator<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.o((BinaryOperator<String>) null));
    }

    @Test
    public void testMc() {
        final java.util.function.BiConsumer<String, java.util.function.Consumer<Character>> mapper = (s, c) -> {
            for (final char ch : s.toCharArray()) {
                c.accept(ch);
            }
        };
        final List<Character> chars = new ArrayList<>();
        Fn.mc(mapper).accept("ab", chars::add);
        assertEquals(List.of('a', 'b'), chars);
        assertThrows(IllegalArgumentException.class, () -> Fn.mc(null));
    }

    @Test
    public void testSsPpCc() throws Exception {
        assertEquals("hello", Fn.ss(() -> "hello").get());
        assertEquals(123, Fn.ss("123", Integer::parseInt).get());
        assertThrows(RuntimeException.class, () -> Fn.ss(() -> {
            throw new IOException("e");
        }).get());

        assertTrue(Fn.pp((Throwables.Predicate<String, Exception>) String::isEmpty).test(""));
        Fn.cc((Throwables.Consumer<String, Exception>) s -> {
        }).accept("x");
        assertEquals(5, Fn.ff((Throwables.Function<String, Integer, Exception>) String::length).apply("hello"));
    }

    @Test
    public void testSpScSf() {
        final Object mutex = new Object();
        assertTrue(Fn.sp(mutex, (Predicate<String>) String::isEmpty).test(""));
        final AtomicInteger n = new AtomicInteger();
        Fn.sc(mutex, (Consumer<Integer>) n::set).accept(3);
        assertEquals(3, n.get());
        assertEquals(5, Fn.sf(mutex, (Function<String, Integer>) String::length).apply("hello"));
        assertThrows(IllegalArgumentException.class, () -> Fn.sp(null, (Predicate<String>) s -> true));
        assertThrows(IllegalArgumentException.class, () -> Fn.sc(mutex, (Consumer<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.sf(mutex, (Function<String, String>) null));
    }

    @Test
    public void testC2fF2c() {
        final AtomicInteger n = new AtomicInteger();
        assertEquals(null, Fn.c2f((Consumer<String>) s -> n.incrementAndGet()).apply("x"));
        assertEquals(42, Fn.c2f((Consumer<String>) s -> n.incrementAndGet(), 42).apply("x"));
        Fn.f2c((Function<String, Integer>) String::length).accept("ab");
        assertEquals(2, n.get());
        assertThrows(IllegalArgumentException.class, () -> Fn.c2f((Consumer<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.f2c((Function<String, String>) null));
    }

    @Test
    public void testRunnableCallableAdapters() throws Exception {
        final AtomicInteger n = new AtomicInteger();
        Fn.rr(() -> n.incrementAndGet()).run();
        Fn.r((Runnable) () -> n.incrementAndGet()).run();
        Fn.jr(() -> n.incrementAndGet()).run();
        assertEquals("y", Fn.jc((java.util.concurrent.Callable<String>) () -> "y").call());
        assertEquals(null, Fn.r2c(() -> n.incrementAndGet()).call());
        assertEquals("done", Fn.r2c(() -> n.incrementAndGet(), "done").call());
        Fn.c2r((Callable<String>) () -> "ignored").run();
        Fn.jr2r(() -> n.incrementAndGet()).run();
        assertEquals("z", Fn.jc2c((java.util.concurrent.Callable<String>) () -> "z").call());
        Fn.jc2r((java.util.concurrent.Callable<String>) () -> "z").run();
        assertTrue(n.get() >= 1);
        assertThrows(IllegalArgumentException.class, () -> Fn.r2c((java.lang.Runnable) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.r((Runnable) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.jr(null));
    }

    // FINDING G20-003 (doc-only contract pin): the class-javadoc conversion table now names the DECLARED source
    // type of each shorthand. c2f/f2c/r2c declare the JDK interfaces (so the table's "leading j marks the JDK
    // source" rule does not apply to them); c2r is the one shorthand whose source really is the abacus type.
    @Test
    public void testConversionShorthandsDeclareJdkSourceTypes() throws Exception {
        assertEquals(java.util.function.Consumer.class, Fn.class.getMethod("c2f", java.util.function.Consumer.class).getParameterTypes()[0]);
        assertEquals(java.util.function.Function.class, Fn.class.getMethod("f2c", java.util.function.Function.class).getParameterTypes()[0]);
        assertEquals(java.lang.Runnable.class, Fn.class.getMethod("r2c", java.lang.Runnable.class).getParameterTypes()[0]);
        assertEquals(Callable.class, Fn.class.getMethod("c2r", Callable.class).getParameterTypes()[0]);

        final AtomicInteger n = new AtomicInteger();
        assertEquals(null, Fn.c2f((java.util.function.Consumer<String>) s -> n.incrementAndGet()).apply("x"));
        Fn.f2c((java.util.function.Function<String, Integer>) String::length).accept("ab");
        assertEquals(null, Fn.r2c((java.lang.Runnable) n::incrementAndGet).call());
        assertEquals(2, n.get());
    }
}
