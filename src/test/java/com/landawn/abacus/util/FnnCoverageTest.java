package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Runnable;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;

public class FnnCoverageTest extends TestBase {

    @Test
    public void testRateLimiter() throws Throwable {
        Fnn.<String, Exception> rateLimiter(1000.0).accept("a");
        Fnn.<String, Exception> rateLimiter(RateLimiter.create(1000.0)).accept("b");
        assertThrows(IllegalArgumentException.class, () -> Fnn.rateLimiter((RateLimiter) null));
    }

    @Test
    public void testAcceptAndApplyByKeyValue() throws Throwable {
        final List<String> keys = new ArrayList<>();
        final List<Integer> values = new ArrayList<>();
        Fnn.<String, Integer, Exception> acceptByKey(keys::add).accept(Map.entry("a", 1));
        Fnn.<String, Integer, Exception> acceptByValue(values::add).accept(Map.entry("a", 7));
        assertEquals(List.of("a"), keys);
        assertEquals(List.of(7), values);
        assertEquals(1, Fnn.<String, Integer, Integer, Exception> applyByKey(String::length).apply(Map.entry("x", 9)));
        assertEquals(18, Fnn.<String, Integer, Integer, Exception> applyByValue(v -> v * 2).apply(Map.entry("x", 9)));
        assertThrows(IllegalArgumentException.class, () -> Fnn.acceptByKey(null));
        assertThrows(IllegalArgumentException.class, () -> Fnn.applyByValue(null));
    }

    @Test
    public void testSelectMinMaxBy() throws Throwable {
        assertEquals("a", Fnn.<String, Exception> selectFirst().apply("a", "b"));
        assertEquals("b", Fnn.<String, Exception> selectSecond().apply("a", "b"));
        assertEquals("hi", Fnn.<String, Exception> minBy(String::length).apply("hi", "hello"));
        assertEquals("hello", Fnn.<String, Exception> maxBy(String::length).apply("hi", "hello"));
        assertEquals("a", Fnn.<String, Integer, Exception> minByKey().apply(Map.entry("a", 2), Map.entry("b", 1)).getKey());
        assertEquals("b", Fnn.<String, Integer, Exception> minByValue().apply(Map.entry("a", 2), Map.entry("b", 1)).getKey());
        assertEquals("b", Fnn.<String, Integer, Exception> maxByKey().apply(Map.entry("a", 2), Map.entry("b", 1)).getKey());
        assertEquals("a", Fnn.<String, Integer, Exception> maxByValue().apply(Map.entry("a", 2), Map.entry("b", 1)).getKey());
        assertThrows(IllegalArgumentException.class, () -> Fnn.minBy(null));
        assertThrows(IllegalArgumentException.class, () -> Fnn.maxBy(null));
    }

    @Test
    public void testNot() throws Throwable {
        assertFalse(Fnn.not((Throwables.Predicate<String, Exception>) String::isEmpty).test(""));
        assertTrue(Fnn.not((Throwables.Predicate<String, Exception>) String::isEmpty).test("x"));
        assertTrue(Fnn.not((Throwables.BiPredicate<String, Integer, Exception>) (s, i) -> s.length() > i).test("hi", 5));
        assertTrue(Fnn.not((Throwables.TriPredicate<String, Integer, Boolean, Exception>) (s, i, b) -> b).test("a", 1, false));
        assertThrows(IllegalArgumentException.class, () -> Fnn.not((Throwables.Predicate<String, Exception>) null));
        assertThrows(IllegalArgumentException.class, () -> Fnn.not((Throwables.BiPredicate<String, Integer, Exception>) null));
        assertThrows(IllegalArgumentException.class, () -> Fnn.not((Throwables.TriPredicate<String, Integer, Boolean, Exception>) null));
    }

    @Test
    public void testIdentityAdapters() throws Throwable {
        final Throwables.Predicate<String, Exception> p = String::isEmpty;
        assertSame(p, Fnn.p(p));
        assertTrue(Fnn.p("ab", (Throwables.BiPredicate<String, String, Exception>) (prefix, s) -> s.startsWith(prefix)).test("abc"));
        assertTrue(Fnn.p("a", "b", (Throwables.TriPredicate<String, String, String, Exception>) (x, y, z) -> (x + y).equals(z)).test("ab"));
        assertTrue(Fnn.p((Throwables.BiPredicate<String, Integer, Exception>) (s, n) -> s.length() == n).test("ab", 2));
        assertTrue(Fnn.p("pre", (Throwables.TriPredicate<String, Integer, String, Exception>) (a, n, s) -> s.startsWith(a)).test(1, "preX"));
        assertTrue(Fnn.p((Throwables.TriPredicate<String, Integer, Boolean, Exception>) (s, n, b) -> b).test("a", 1, true));

        final Throwables.Function<String, Integer, Exception> f = String::length;
        assertSame(f, Fnn.f(f));
        assertEquals("x1", Fnn.f("x", (Throwables.BiFunction<String, Integer, String, Exception>) (pref, i) -> pref + i).apply(1));
        assertEquals("a-1-true", Fnn.f("a", 1, (Throwables.TriFunction<String, Integer, Boolean, String, Exception>) (x, y, z) -> x + "-" + y + "-" + z).apply(true));
        assertEquals("ab2", Fnn.f((Throwables.BiFunction<String, Integer, String, Exception>) (s, i) -> s + i).apply("ab", 2));
        assertEquals("p-2-z", Fnn.f("p", (Throwables.TriFunction<String, Integer, String, String, Exception>) (a, n, s) -> a + "-" + n + "-" + s).apply(2, "z"));
        assertEquals("a1t", Fnn.f((Throwables.TriFunction<String, Integer, Boolean, String, Exception>) (s, i, b) -> s + i + (b ? "t" : "f")).apply("a", 1, true));

        final List<String> seen = new ArrayList<>();
        final Throwables.Consumer<String, Exception> c = seen::add;
        assertSame(c, Fnn.c(c));
        Fnn.c("pre-", (Throwables.BiConsumer<String, String, Exception>) (pref, v) -> seen.add(pref + v)).accept("x");
        Fnn.c("a", 1, (Throwables.TriConsumer<String, Integer, String, Exception>) (x, y, z) -> seen.add(x + y + z)).accept("z");
        Fnn.c((Throwables.BiConsumer<String, Integer, Exception>) (s, i) -> seen.add(s + i)).accept("b", 2);
        Fnn.c("k", (Throwables.TriConsumer<String, Integer, Boolean, Exception>) (a, n, b) -> seen.add(a + n + b)).accept(3, true);
        Fnn.c((Throwables.TriConsumer<String, Integer, Boolean, Exception>) (s, i, b) -> seen.add(s + i)).accept("c", 4, true);
        assertTrue(seen.contains("pre-x"));
        assertTrue(seen.contains("a1z"));
        assertTrue(seen.contains("b2"));
        assertTrue(seen.contains("k3true"));
        assertTrue(seen.contains("c4"));
    }

    @Test
    public void testPpFfCcRrAndConverters() throws Throwable {
        assertTrue(Fnn.pp((Predicate<String>) String::isEmpty).test(""));
        assertTrue(Fnn.pp((BiPredicate<String, Integer>) (s, n) -> s.length() == n).test("ab", 2));
        assertTrue(Fnn.pp((TriPredicate<String, Integer, Boolean>) (s, n, b) -> b).test("a", 1, true));
        assertTrue(Fnn.pp("ab", (BiPredicate<String, String>) (prefix, s) -> s.startsWith(prefix)).test("abc"));
        assertTrue(Fnn.pp("a", "b", (TriPredicate<String, String, String>) (x, y, z) -> (x + y).equals(z)).test("ab"));

        assertEquals(5, Fnn.ff((Function<String, Integer>) String::length).apply("hello"));
        assertEquals("a1", Fnn.ff((BiFunction<String, Integer, String>) (s, i) -> s + i).apply("a", 1));
        assertEquals("a1t", Fnn.ff((TriFunction<String, Integer, Boolean, String>) (s, i, b) -> s + i + (b ? "t" : "")).apply("a", 1, true));
        assertEquals("pre1", Fnn.ff("pre", (BiFunction<String, Integer, String>) (p, i) -> p + i).apply(1));
        assertEquals("a-1-z", Fnn.ff("a", 1, (TriFunction<String, Integer, String, String>) (x, y, z) -> x + "-" + y + "-" + z).apply("z"));

        final List<String> seen = new ArrayList<>();
        Fnn.cc((Consumer<String>) seen::add).accept("x");
        Fnn.cc((BiConsumer<String, Integer>) (s, i) -> seen.add(s + i)).accept("a", 1);
        Fnn.cc((TriConsumer<String, Integer, Boolean>) (s, i, b) -> seen.add(s + i)).accept("b", 2, true);
        Fnn.cc("pre-", (BiConsumer<String, String>) (p, v) -> seen.add(p + v)).accept("y");
        Fnn.cc("k", 3, (TriConsumer<String, Integer, String>) (a, n, z) -> seen.add(a + n + z)).accept("z");
        assertEquals("v", Fnn.cc((com.landawn.abacus.util.function.Callable<String>) () -> "v").call());
        assertTrue(seen.contains("x"));
        assertTrue(seen.contains("a1"));
        assertTrue(seen.contains("pre-y"));

        final AtomicInteger n = new AtomicInteger();
        Fnn.rr((Runnable) n::incrementAndGet).run();
        assertEquals(1, n.get());

        assertEquals(null, Fnn.c2f((Throwables.Consumer<String, Exception>) s -> n.incrementAndGet()).apply("x"));
        assertEquals(42, Fnn.c2f((Throwables.Consumer<String, Exception>) s -> n.incrementAndGet(), 42).apply("x"));
        assertEquals(99, Fnn.c2f((Throwables.BiConsumer<String, Integer, Exception>) (s, i) -> n.addAndGet(i), 99).apply("a", 3));
        assertEquals("ok", Fnn.c2f((Throwables.TriConsumer<String, Integer, Boolean, Exception>) (s, i, b) -> n.addAndGet(i), "ok").apply("a", 2, true));
        Fnn.c2f((Throwables.BiConsumer<String, Integer, Exception>) (s, i) -> n.addAndGet(i)).apply("a", 1);
        Fnn.c2f((Throwables.TriConsumer<String, Integer, Boolean, Exception>) (s, i, b) -> n.addAndGet(i)).apply("a", 1, true);

        Fnn.f2c((Throwables.Function<String, Integer, Exception>) String::length).accept("ab");
        Fnn.f2c((Throwables.BiFunction<String, Integer, Integer, Exception>) (s, i) -> i).accept("a", 4);
        Fnn.f2c((Throwables.TriFunction<String, Integer, Boolean, Integer, Exception>) (s, i, b) -> i).accept("a", 5, true);

        Fnn.r2c((Throwables.Runnable<Exception>) n::incrementAndGet).call();
        assertEquals("x", Fnn.r2c((Throwables.Runnable<Exception>) n::incrementAndGet, "x").call());
        Fnn.c2r((Throwables.Callable<String, Exception>) () -> "y").run();

        final Object mutex = new Object();
        assertTrue(Fnn.sp(mutex, (Throwables.Predicate<String, Exception>) String::isEmpty).test(""));
        assertTrue(Fnn.sp(mutex, (Throwables.BiPredicate<String, Integer, Exception>) (s, i) -> s.length() == i).test("ab", 2));
        assertTrue(Fnn.sp(mutex, "ab", (Throwables.BiPredicate<String, String, Exception>) (prefix, s) -> s.startsWith(prefix)).test("abc"));
        Fnn.sc(mutex, (Throwables.Consumer<Integer, Exception>) n::set).accept(8);
        assertEquals(8, n.get());
        Fnn.sc(mutex, (Throwables.BiConsumer<String, Integer, Exception>) (s, i) -> n.addAndGet(i)).accept("a", 2);
        Fnn.sc(mutex, "p", (Throwables.BiConsumer<String, String, Exception>) (a, b) -> n.incrementAndGet()).accept("q");
        assertEquals(5, Fnn.sf(mutex, (Throwables.Function<String, Integer, Exception>) String::length).apply("hello"));
        assertEquals("a1", Fnn.sf(mutex, (Throwables.BiFunction<String, Integer, String, Exception>) (s, i) -> s + i).apply("a", 1));
        assertEquals("p2", Fnn.sf(mutex, "p", (Throwables.BiFunction<String, Integer, String, Exception>) (a, i) -> a + i).apply(2));
    }

    @Test
    public void testOMcAndS() throws Throwable {
        assertEquals("HELLO", Fnn.o((Throwables.UnaryOperator<String, Exception>) String::toUpperCase).apply("hello"));
        assertEquals("ab", Fnn.o((Throwables.BinaryOperator<String, Exception>) (a, b) -> a + b).apply("a", "b"));
        final List<Character> chars = new ArrayList<>();
        Fnn.mc((Throwables.BiConsumer<String, java.util.function.Consumer<Character>, Exception>) (s, c) -> {
            for (final char ch : s.toCharArray()) {
                c.accept(ch);
            }
        }).accept("ab", chars::add);
        assertEquals(List.of('a', 'b'), chars);
        assertEquals("v", Fnn.s((Throwables.Supplier<String, Exception>) () -> "v").get());
        assertEquals(4, Fnn.s("test", (Throwables.Function<String, Integer, Exception>) String::length).get());
    }
}
