package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;

public class FnCoverageTest extends FnTestSupport {

    @Test
    public void testApplyIfNotNull_threeAndFourMappers() {
        final Function<String, Integer> len = String::length;
        final Function<Integer, Integer> times2 = n -> n * 2;
        final Function<Integer, Integer> plus1 = n -> n + 1;
        final Function<Integer, String> prefix = n -> "v" + n;

        assertEquals("v10", Fn.applyIfNotNullOrDefault(len, times2, prefix, "none").apply("hello"));
        assertEquals("none", Fn.applyIfNotNullOrDefault(len, times2, prefix, "none").apply(null));
        assertEquals("none", Fn.applyIfNotNullOrDefault(s -> null, times2, prefix, "none").apply("hello"));
        assertEquals("none", Fn.applyIfNotNullOrDefault(len, n -> null, prefix, "none").apply("hello"));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrDefault(null, times2, prefix, "none"));

        assertEquals("v11", Fn.applyIfNotNullOrDefault(len, times2, plus1, prefix, "none").apply("hello"));
        assertEquals("none", Fn.applyIfNotNullOrDefault(len, times2, plus1, prefix, "none").apply(null));
        assertEquals("none", Fn.applyIfNotNullOrDefault(s -> null, times2, plus1, prefix, "none").apply("x"));
        assertEquals("none", Fn.applyIfNotNullOrDefault(len, n -> null, plus1, prefix, "none").apply("x"));
        assertEquals("none", Fn.applyIfNotNullOrDefault(len, times2, n -> null, prefix, "none").apply("x"));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrDefault(len, times2, plus1, null, "none"));

        assertEquals("v10", Fn.applyIfNotNullOrElseGet(len, times2, prefix, () -> "none").apply("hello"));
        assertEquals("none", Fn.applyIfNotNullOrElseGet(len, times2, prefix, () -> "none").apply(null));
        assertEquals("none", Fn.applyIfNotNullOrElseGet(s -> null, times2, prefix, () -> "none").apply("hello"));
        assertEquals("none", Fn.applyIfNotNullOrElseGet(len, n -> null, prefix, () -> "none").apply("hello"));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrElseGet(len, times2, prefix, null));

        assertEquals("v11", Fn.applyIfNotNullOrElseGet(len, times2, plus1, prefix, () -> "none").apply("hello"));
        assertEquals("none", Fn.applyIfNotNullOrElseGet(len, times2, plus1, prefix, () -> "none").apply(null));
        assertEquals("none", Fn.applyIfNotNullOrElseGet(s -> null, times2, plus1, prefix, () -> "none").apply("x"));
        assertEquals("none", Fn.applyIfNotNullOrElseGet(len, n -> null, plus1, prefix, () -> "none").apply("x"));
        assertEquals("none", Fn.applyIfNotNullOrElseGet(len, times2, n -> null, prefix, () -> "none").apply("x"));
        assertThrows(IllegalArgumentException.class, () -> Fn.applyIfNotNullOrElseGet(len, times2, plus1, prefix, null));
    }

    @Test
    public void testAndOr_threeArgs() {
        final Predicate<String> notEmpty = s -> s != null && !s.isEmpty();
        final Predicate<String> shortEnough = s -> s.length() < 5;
        final Predicate<String> startsA = s -> s.startsWith("a");
        assertTrue(Fn.and(notEmpty, shortEnough, startsA).test("abc"));
        assertFalse(Fn.and(notEmpty, shortEnough, startsA).test("abcdz"));
        assertFalse(Fn.and(notEmpty, shortEnough, startsA).test("bcd"));
        assertThrows(IllegalArgumentException.class, () -> Fn.and(notEmpty, shortEnough, null));

        assertTrue(Fn.or(notEmpty, shortEnough, startsA).test("zzzzz"));
        assertTrue(Fn.or(s -> false, s -> false, startsA).test("a"));
        assertFalse(Fn.or(s -> false, s -> false, s -> false).test("x"));
        assertThrows(IllegalArgumentException.class, () -> Fn.or(notEmpty, shortEnough, null));

        final BooleanSupplier t = () -> true;
        final BooleanSupplier f = () -> false;
        assertTrue(Fn.or(f, f, t).getAsBoolean());
        assertFalse(Fn.or(f, f, f).getAsBoolean());
        assertTrue(Fn.and(t, t, t).getAsBoolean());
        assertFalse(Fn.and(t, t, f).getAsBoolean());
        assertThrows(IllegalArgumentException.class, () -> Fn.or(t, t, null));

        final BiPredicate<String, Integer> lenEq = (s, n) -> s.length() == n;
        final BiPredicate<String, Integer> positive = (s, n) -> n > 0;
        final BiPredicate<String, Integer> small = (s, n) -> n < 9;
        assertTrue(Fn.and(lenEq, positive, small).test("ab", 2));
        assertFalse(Fn.and(lenEq, positive, small).test("ab", 3));
        assertTrue(Fn.or(lenEq, positive).test("ab", 9));
        assertTrue(Fn.or(lenEq, (s, n) -> false, (s, n) -> n == 2).test("ab", 2));
        assertFalse(Fn.or((BiPredicate<String, Integer>) (s, n) -> false, (s, n) -> false, (s, n) -> false).test("ab", 2));
        assertThrows(IllegalArgumentException.class, () -> Fn.and(lenEq, positive, null));
        assertThrows(IllegalArgumentException.class, () -> Fn.or(lenEq, positive, null));
    }

    @Test
    public void testFrom_biPredicateAndBiConsumer() {
        final java.util.function.BiPredicate<String, Integer> javaBi = (s, n) -> s.length() == n;
        assertTrue(Fn.from(javaBi).test("ab", 2));
        assertTrue(Fn.from(Fn.from(javaBi)).test("ab", 2));
        assertThrows(IllegalArgumentException.class, () -> Fn.from((java.util.function.BiPredicate<String, Integer>) null));

        final List<String> seen = new ArrayList<>();
        final java.util.function.BiConsumer<String, Integer> javaC = (s, n) -> seen.add(s + n);
        Fn.from(javaC).accept("a", 1);
        assertEquals(List.of("a1"), seen);
        assertThrows(IllegalArgumentException.class, () -> Fn.from((java.util.function.BiConsumer<String, Integer>) null));
    }

    @Test
    public void testSpScSf_biAndTri() {
        final Object mutex = new Object();
        assertTrue(Fn.sp(mutex, "ab", (String prefix, String s) -> s.startsWith(prefix)).test("abc"));
        assertTrue(Fn.sp(mutex, (BiPredicate<String, Integer>) (s, n) -> s.length() == n).test("ab", 2));
        assertTrue(Fn.sp(mutex, (TriPredicate<String, Integer, Boolean>) (s, n, b) -> b && s.length() == n).test("ab", 2, true));

        final AtomicInteger n = new AtomicInteger();
        Fn.sc(mutex, "pre-", (String prefix, String v) -> n.addAndGet(prefix.length() + v.length())).accept("xy");
        assertEquals(6, n.get());
        Fn.sc(mutex, (BiConsumer<String, Integer>) (s, i) -> n.addAndGet(i)).accept("a", 4);
        assertEquals(10, n.get());
        Fn.sc(mutex, (TriConsumer<String, Integer, Boolean>) (s, i, b) -> n.addAndGet(b ? i : 0)).accept("a", 3, true);
        assertEquals(13, n.get());

        assertEquals("ab2", Fn.sf(mutex, "ab", (String prefix, Integer i) -> prefix + i).apply(2));
        assertEquals("a1", Fn.sf(mutex, (BiFunction<String, Integer, String>) (s, i) -> s + i).apply("a", 1));
        assertEquals("a1true", Fn.sf(mutex, (TriFunction<String, Integer, Boolean, String>) (s, i, b) -> s + i + b).apply("a", 1, true));

        assertThrows(IllegalArgumentException.class, () -> Fn.sp(mutex, "x", (BiPredicate<String, String>) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.sc(mutex, (BiConsumer<String, Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.sf(mutex, (TriFunction<String, Integer, Boolean, String>) null));
    }

    @Test
    public void testCloseQuietlyAndPrintlnSeparators() {
        final MyCloseable c = new MyCloseable();
        Fn.<MyCloseable> closeQuietly().accept(c);
        assertEquals(1, c.getCloseCount());

        Fn.println(": ").accept("k", "v");
        Fn.println("-").accept("k", "v");
        Fn.println("_").accept("k", "v");
        Fn.println(",").accept("k", "v");
        Fn.println("=").accept("k", "v");
        Fn.println(":").accept("k", "v");
        Fn.println(", ").accept("k", "v");
        Fn.println("").accept("k", "v");
        Fn.println("|").accept("k", "v");
    }

    @Test
    public void testGreaterThanOrEqualAndLessThan() {
        assertTrue(Fn.<Integer> greaterThanOrEqual().test(5, 5));
        assertTrue(Fn.<Integer> greaterThanOrEqual().test(6, 5));
        assertFalse(Fn.<Integer> greaterThanOrEqual().test(4, 5));
        assertTrue(Fn.<Integer> lessThan().test(4, 5));
        assertFalse(Fn.<Integer> lessThan().test(5, 5));
    }

    @Test
    public void testC2fTriFunction() throws Exception {
        final AtomicInteger n = new AtomicInteger();
        assertEquals("ok", Fn.c2f((TriConsumer<String, Integer, Boolean>) (s, i, b) -> n.addAndGet(i), "ok").apply("a", 3, true));
        assertEquals(3, n.get());
        Fn.f2c((TriFunction<String, Integer, Boolean, Integer>) (s, i, b) -> i).accept("a", 4, true);
    }

    @Test
    public void testFnFDAndFB() {
        assertTrue(Fn.FD.p(d -> d > 0).test(1.5));
        assertEquals(2.0, Fn.FD.f(d -> d * 2).apply(1.0));
        final double[] seen = { 0 };
        Fn.FD.c(d -> seen[0] = d).accept(3.5);
        assertEquals(3.5, seen[0]);
        final com.landawn.abacus.util.function.DoubleBiFunction<MergeResult> altD = Fn.FD.alternate();
        assertEquals(MergeResult.TAKE_FIRST, altD.apply(1.0, 2.0));
        assertEquals(MergeResult.TAKE_SECOND, altD.apply(1.0, 2.0));
        assertTrue(Fn.FD.notEqual().test(1.0, 2.0));
        assertTrue(Fn.FD.lessThan().test(1.0, 2.0));
        assertTrue(Fn.FD.lessThanOrEqual().test(2.0, 2.0));

        final com.landawn.abacus.util.function.ByteBiFunction<MergeResult> altB = Fn.FB.alternate();
        assertEquals(MergeResult.TAKE_FIRST, altB.apply((byte) 1, (byte) 2));
        assertEquals(MergeResult.TAKE_SECOND, altB.apply((byte) 1, (byte) 2));
        assertTrue(Fn.FB.lessThanOrEqual().test((byte) 1, (byte) 1));
        assertTrue(Fn.FB.greaterThanOrEqual().test((byte) 2, (byte) 1));
    }
}
