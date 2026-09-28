package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;

/**
 * Cycle-3 review fixes 2026-09-24 for {@link Fn} and {@link Fnn}: C-125 ({@code ? super} input widening of
 * Fn.pp/cc/ff/sp/sc/sf and Fnn.pp/cc/ff/sp/sc/sf - every assignment below failed to compile before, with
 * "inference variable T has incompatible equality constraints") and H4-02 (Fn.shutdown retries a failed shutdown -
 * doc-only lock).
 */
public class FnReview20260924cTest extends TestBase {

    private static final String UNICODE = "中文😀";

    // Delegates typed on a SUPERTYPE (Object / CharSequence) of the element type used by the callers (String).
    private static final Throwables.Predicate<Object, IOException> T_PRED = o -> {
        if (o == null) {
            throw new IOException("null");
        }
        return o.toString().length() > 1;
    };
    private static final Throwables.BiPredicate<Object, Object, IOException> T_BI_PRED = (a, b) -> {
        if (a == null) {
            throw new IOException("null");
        }
        return a.equals(b);
    };
    private static final Throwables.TriPredicate<Object, Object, Object, IOException> T_TRI_PRED = (a, b, c) -> {
        if (a == null) {
            throw new IOException("null");
        }
        return (String.valueOf(a) + b).equals(c);
    };
    private static final Throwables.Function<Object, String, IOException> T_FUNC = o -> {
        if (o == null) {
            throw new IOException("null");
        }
        return "<" + o + ">";
    };
    private static final Throwables.BiFunction<Object, Object, String, IOException> T_BI_FUNC = (a, b) -> {
        if (a == null) {
            throw new IOException("null");
        }
        return a + "+" + b;
    };
    private static final Throwables.TriFunction<Object, Object, Object, String, IOException> T_TRI_FUNC = (a, b, c) -> {
        if (a == null) {
            throw new IOException("null");
        }
        return a + "+" + b + "+" + c;
    };

    private static Throwables.Consumer<Object, IOException> tConsumer(final List<Object> sink) {
        return o -> {
            if (o == null) {
                throw new IOException("null");
            }
            sink.add(o);
        };
    }

    private static Throwables.BiConsumer<Object, Object, IOException> tBiConsumer(final List<Object> sink) {
        return (a, b) -> {
            if (a == null) {
                throw new IOException("null");
            }
            sink.add(a + "|" + b);
        };
    }

    private static Throwables.TriConsumer<Object, Object, Object, IOException> tTriConsumer(final List<Object> sink) {
        return (a, b, c) -> {
            if (a == null) {
                throw new IOException("null");
            }
            sink.add(a + "|" + b + "|" + c);
        };
    }

    // ---------------------------------------------------------------------------------------------------------------
    // C-125 Fn.pp / cc / ff: supertype-typed Throwables delegates adapt to narrower abacus interfaces
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    public void c125_fnPp_acceptsSuperTypedPredicates() {
        final Predicate<String> p1 = Fn.pp(T_PRED);
        final Predicate<String> p2 = Fn.pp("ab", T_BI_PRED);
        final Predicate<String> p3 = Fn.pp("a", "b", T_TRI_PRED);
        final BiPredicate<String, String> p4 = Fn.pp(T_BI_PRED);
        final BiPredicate<String, String> p5 = Fn.pp("a", T_TRI_PRED);
        final TriPredicate<String, String, String> p6 = Fn.pp(T_TRI_PRED);
        // explicit type arguments: A = String is now accepted for a delegate typed on Object
        final Predicate<String> p7 = Fn.<String, String> pp("ab", T_BI_PRED);

        assertTrue(p1.test(UNICODE));
        assertFalse(p1.test("a"));
        assertTrue(p2.test("ab"));
        assertFalse(p2.test(UNICODE));
        assertTrue(p3.test("ab"));
        assertTrue(p4.test(UNICODE, UNICODE));
        assertTrue(p5.test("b", "ab"));
        assertTrue(p6.test("a", "b", "ab"));
        assertTrue(p7.test("ab"));

        // composition with a String-typed predicate - the case that motivated the change
        assertTrue(p1.and(s -> s.startsWith("中")).test(UNICODE));

        // exception conversion is unchanged
        assertThrows(UncheckedIOException.class, () -> p1.test(null));
        assertThrows(UncheckedIOException.class, () -> p4.test(null, "x"));
        assertThrows(UncheckedIOException.class, () -> p6.test(null, "x", "y"));
    }

    @Test
    public void c125_fnCc_acceptsSuperTypedConsumers() {
        final List<Object> sink = new ArrayList<>();

        final Consumer<String> c1 = Fn.cc(tConsumer(sink));
        final Consumer<String> c2 = Fn.cc("a", tBiConsumer(sink));
        final Consumer<String> c3 = Fn.cc("a", "b", tTriConsumer(sink));
        final BiConsumer<String, String> c4 = Fn.cc(tBiConsumer(sink));
        final BiConsumer<String, String> c5 = Fn.cc("a", tTriConsumer(sink));
        final TriConsumer<String, String, String> c6 = Fn.cc(tTriConsumer(sink));

        c1.accept(UNICODE);
        c2.accept("x");
        c3.accept("x");
        c4.accept("x", "");
        c5.accept("x", "y");
        c6.accept("x", "y", "z");

        assertEquals(Arrays.asList(UNICODE, "a|x", "a|b|x", "x|", "a|x|y", "x|y|z"), sink);
        assertThrows(UncheckedIOException.class, () -> c1.accept(null));
        assertThrows(UncheckedIOException.class, () -> c4.accept(null, "x"));
    }

    @Test
    public void c125_fnFf_acceptsSuperTypedFunctions() {
        final Function<String, String> f1 = Fn.ff(T_FUNC);
        final Function<String, String> f2 = Fn.ff(T_FUNC, "dflt");
        final Function<String, String> f3 = Fn.ff("a", T_BI_FUNC);
        final Function<String, String> f4 = Fn.ff("a", "b", T_TRI_FUNC);
        final BiFunction<String, String, String> f5 = Fn.ff(T_BI_FUNC);
        final BiFunction<String, String, String> f6 = Fn.ff(T_BI_FUNC, "dflt");
        final BiFunction<String, String, String> f7 = Fn.ff("a", T_TRI_FUNC);
        final TriFunction<String, String, String, String> f8 = Fn.ff(T_TRI_FUNC);
        final TriFunction<String, String, String, String> f9 = Fn.ff(T_TRI_FUNC, "dflt");
        // result widening: a String-returning delegate adapts to a CharSequence-returning function
        final BiFunction<String, String, CharSequence> f10 = Fn.ff(T_BI_FUNC);
        final Function<String, CharSequence> f11 = Fn.ff("a", T_BI_FUNC);

        assertEquals("<" + UNICODE + ">", f1.apply(UNICODE));
        assertEquals("<x>", f2.apply("x"));
        assertEquals("a+x", f3.apply("x"));
        assertEquals("a+b+x", f4.apply("x"));
        assertEquals("x+y", f5.apply("x", "y"));
        assertEquals("x+y", f6.apply("x", "y"));
        assertEquals("a+x+y", f7.apply("x", "y"));
        assertEquals("x+y+z", f8.apply("x", "y", "z"));
        assertEquals("x+y+z", f9.apply("x", "y", "z"));
        assertEquals("x+", f10.apply("x", ""));
        assertEquals("a+x", f11.apply("x"));

        // conversion / default-on-error behaviour unchanged
        assertThrows(UncheckedIOException.class, () -> f1.apply(null));
        assertEquals("dflt", f2.apply(null));
        assertEquals("dflt", f6.apply(null, "y"));
        assertEquals("dflt", f9.apply(null, "y", "z"));
        assertThrows(UncheckedIOException.class, () -> f8.apply(null, "y", "z"));

        // and in a pipeline
        assertEquals(Arrays.asList("<a>", "<" + UNICODE + ">"), N.map(Arrays.asList("a", UNICODE), f1));
    }

    @Test
    public void c125_fnSpScSf_acceptSuperTypedJdkAndAbacusDelegates() {
        final Object mutex = new Object();
        final java.util.function.Predicate<Object> jp = Objects::nonNull;
        final java.util.function.BiPredicate<Object, Object> jbp = Objects::equals;
        final TriPredicate<Object, Object, Object> tp = (a, b, c) -> Objects.equals(a, b) && Objects.equals(b, c);
        final List<Object> sink = Collections.synchronizedList(new ArrayList<>());
        final java.util.function.Consumer<Object> jc = sink::add;
        final java.util.function.BiConsumer<Object, Object> jbc = (a, b) -> sink.add(a + "|" + b);
        final TriConsumer<Object, Object, Object> tc = (a, b, c) -> sink.add(a + "|" + b + "|" + c);
        final java.util.function.Function<Object, String> jf = String::valueOf;
        final java.util.function.BiFunction<Object, Object, String> jbf = (a, b) -> a + "+" + b;
        final TriFunction<Object, Object, Object, String> tf = (a, b, c) -> a + "+" + b + "+" + c;

        final Predicate<String> sp1 = Fn.sp(mutex, jp);
        final Predicate<String> sp2 = Fn.sp(mutex, "a", jbp);
        final BiPredicate<String, String> sp3 = Fn.sp(mutex, jbp);
        final TriPredicate<String, String, String> sp4 = Fn.sp(mutex, tp);
        assertTrue(sp1.test(UNICODE));
        assertFalse(sp1.test(null));
        assertTrue(sp2.test("a"));
        assertFalse(sp2.test("b"));
        assertTrue(sp3.test(UNICODE, UNICODE));
        assertTrue(sp4.test("x", "x", "x"));

        final Consumer<String> sc1 = Fn.sc(mutex, jc);
        final Consumer<String> sc2 = Fn.sc(mutex, "a", jbc);
        final BiConsumer<String, String> sc3 = Fn.sc(mutex, jbc);
        final TriConsumer<String, String, String> sc4 = Fn.sc(mutex, tc);
        sc1.accept(UNICODE);
        sc2.accept("x");
        sc3.accept("x", "y");
        sc4.accept("x", "y", "z");
        assertEquals(Arrays.asList(UNICODE, "a|x", "x|y", "x|y|z"), sink);

        final Function<String, String> sf1 = Fn.sf(mutex, jf);
        final Function<String, String> sf2 = Fn.sf(mutex, "a", jbf);
        final BiFunction<String, String, String> sf3 = Fn.sf(mutex, jbf);
        final TriFunction<String, String, String, String> sf4 = Fn.sf(mutex, tf);
        final BiFunction<String, String, CharSequence> sf5 = Fn.sf(mutex, jbf);
        assertEquals("null", sf1.apply(null));
        assertEquals("a+x", sf2.apply("x"));
        assertEquals("x+y", sf3.apply("x", "y"));
        assertEquals("x+y+z", sf4.apply("x", "y", "z"));
        assertEquals("x+", sf5.apply("x", ""));
    }

    @Test
    public void c125_fnSp_stillSynchronizesOnTheMutex() throws Exception {
        final Object mutex = new Object();
        final java.util.function.Predicate<Object> holdsLock = o -> Thread.holdsLock(mutex);
        final Predicate<String> sp = Fn.sp(mutex, holdsLock);

        assertTrue(sp.test("x"));
        assertFalse(holdsLock.test("x"));
    }

    @Test
    public void c125_nullArguments_stillRejectedWithIae() {
        final Object mutex = new Object();

        assertThrows(IllegalArgumentException.class, () -> Fn.pp((Throwables.Predicate<Object, IOException>) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.cc("a", (Throwables.BiConsumer<Object, Object, IOException>) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.ff((Throwables.TriFunction<Object, Object, Object, String, IOException>) null, "d"));
        assertThrows(IllegalArgumentException.class, () -> Fn.sp(mutex, (java.util.function.Predicate<Object>) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.sf(null, (java.util.function.Function<Object, String>) String::valueOf));
        assertThrows(IllegalArgumentException.class, () -> Fnn.pp((Predicate<Object>) null));
        assertThrows(IllegalArgumentException.class, () -> Fnn.ff("a", (java.util.function.BiFunction<Object, Object, String>) null));
        assertThrows(IllegalArgumentException.class, () -> Fnn.sc(mutex, (Throwables.Consumer<Object, IOException>) null));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // C-125 Fnn.pp / cc / ff / sp / sc / sf
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    public void c125_fnnPpCcFf_acceptSuperTypedDelegates() throws IOException {
        final Predicate<Object> ap = Objects::nonNull;
        final BiPredicate<Object, Object> abp = Objects::equals;
        final TriPredicate<Object, Object, Object> atp = (a, b, c) -> Objects.equals(a, b) && Objects.equals(b, c);
        final java.util.function.BiPredicate<Object, Object> jbp = Objects::equals;

        final Throwables.Predicate<String, IOException> p1 = Fnn.pp(ap);
        final Throwables.Predicate<String, IOException> p2 = Fnn.pp("a", jbp);
        final Throwables.Predicate<String, IOException> p3 = Fnn.pp("a", "a", atp);
        final Throwables.BiPredicate<String, String, IOException> p4 = Fnn.pp(abp);
        final Throwables.BiPredicate<String, String, IOException> p5 = Fnn.pp("a", atp);
        final Throwables.TriPredicate<String, String, String, IOException> p6 = Fnn.pp(atp);
        // the one-to-one casts still return the argument itself
        assertSame(ap, p1);
        assertSame(abp, p4);
        assertSame(atp, p6);
        assertTrue(p1.test(UNICODE));
        assertFalse(p1.test(null));
        assertTrue(p2.test("a"));
        assertTrue(p3.test("a"));
        assertFalse(p3.test("b"));
        assertTrue(p4.test(UNICODE, UNICODE));
        assertTrue(p5.test("a", "a"));
        assertTrue(p6.test("x", "x", "x"));

        final List<Object> sink = new ArrayList<>();
        final Consumer<Object> ac = sink::add;
        final java.util.function.BiConsumer<Object, Object> jbc = (a, b) -> sink.add(a + "|" + b);
        final BiConsumer<Object, Object> abc = (a, b) -> sink.add(a + "|" + b);
        final TriConsumer<Object, Object, Object> atc = (a, b, c) -> sink.add(a + "|" + b + "|" + c);

        final Throwables.Consumer<String, IOException> c1 = Fnn.cc(ac);
        final Throwables.Consumer<String, IOException> c2 = Fnn.cc("a", jbc);
        final Throwables.Consumer<String, IOException> c3 = Fnn.cc("a", "b", atc);
        final Throwables.BiConsumer<String, String, IOException> c4 = Fnn.cc(abc);
        final Throwables.BiConsumer<String, String, IOException> c5 = Fnn.cc("a", atc);
        final Throwables.TriConsumer<String, String, String, IOException> c6 = Fnn.cc(atc);
        assertSame(ac, c1);
        assertSame(abc, c4);
        assertSame(atc, c6);
        c1.accept(UNICODE);
        c2.accept("x");
        c3.accept("x");
        c4.accept("x", "y");
        c5.accept("x", "y");
        c6.accept("x", "y", "z");
        assertEquals(Arrays.asList(UNICODE, "a|x", "a|b|x", "x|y", "a|x|y", "x|y|z"), sink);

        final Function<Object, String> af = String::valueOf;
        final java.util.function.BiFunction<Object, Object, String> jbf = (a, b) -> a + "+" + b;
        final BiFunction<Object, Object, String> abf = (a, b) -> a + "+" + b;
        final TriFunction<Object, Object, Object, String> atf = (a, b, c) -> a + "+" + b + "+" + c;

        final Throwables.Function<String, String, IOException> f1 = Fnn.ff(af);
        final Throwables.Function<String, String, IOException> f2 = Fnn.ff("a", jbf);
        final Throwables.Function<String, String, IOException> f3 = Fnn.ff("a", "b", atf);
        final Throwables.BiFunction<String, String, String, IOException> f4 = Fnn.ff(abf);
        final Throwables.BiFunction<String, String, String, IOException> f5 = Fnn.ff("a", atf);
        final Throwables.TriFunction<String, String, String, String, IOException> f6 = Fnn.ff(atf);
        final Throwables.Function<String, CharSequence, IOException> f7 = Fnn.ff("a", jbf);
        assertSame(af, f1);
        assertSame(abf, f4);
        assertSame(atf, f6);
        assertEquals(UNICODE, f1.apply(UNICODE));
        assertEquals("a+x", f2.apply("x"));
        assertEquals("a+b+x", f3.apply("x"));
        assertEquals("x+y", f4.apply("x", "y"));
        assertEquals("a+x+y", f5.apply("x", "y"));
        assertEquals("x+y+z", f6.apply("x", "y", "z"));
        assertEquals("a+x", f7.apply("x"));
    }

    @Test
    public void c125_fnnSpScSf_acceptSuperTypedDelegates() throws IOException {
        final Object mutex = new Object();
        final Throwables.Predicate<String, IOException> sp1 = Fnn.sp(mutex, T_PRED);
        final Throwables.Predicate<String, IOException> sp2 = Fnn.sp(mutex, "ab", T_BI_PRED);
        final Throwables.BiPredicate<String, String, IOException> sp3 = Fnn.sp(mutex, T_BI_PRED);
        assertTrue(sp1.test(UNICODE));
        assertTrue(sp2.test("ab"));
        assertTrue(sp3.test("x", "x"));
        // Fnn does not convert: the checked exception reaches the caller as is
        assertThrows(IOException.class, () -> sp1.test(null));

        final List<Object> sink = new ArrayList<>();
        final Throwables.Consumer<String, IOException> sc1 = Fnn.sc(mutex, tConsumer(sink));
        final Throwables.Consumer<String, IOException> sc2 = Fnn.sc(mutex, "a", tBiConsumer(sink));
        final Throwables.BiConsumer<String, String, IOException> sc3 = Fnn.sc(mutex, tBiConsumer(sink));
        sc1.accept(UNICODE);
        sc2.accept("x");
        sc3.accept("x", "y");
        assertEquals(Arrays.asList(UNICODE, "a|x", "x|y"), sink);
        assertThrows(IOException.class, () -> sc1.accept(null));

        final Throwables.Function<String, String, IOException> sf1 = Fnn.sf(mutex, T_FUNC);
        final Throwables.Function<String, String, IOException> sf2 = Fnn.sf(mutex, "a", T_BI_FUNC);
        final Throwables.BiFunction<String, String, String, IOException> sf3 = Fnn.sf(mutex, T_BI_FUNC);
        final Throwables.BiFunction<String, String, CharSequence, IOException> sf4 = Fnn.sf(mutex, T_BI_FUNC);
        assertEquals("<x>", sf1.apply("x"));
        assertEquals("a+x", sf2.apply("x"));
        assertEquals("x+y", sf3.apply("x", "y"));
        assertEquals("x+y", sf4.apply("x", "y"));
        assertThrows(IOException.class, () -> sf1.apply(null));

        final Throwables.Predicate<String, IOException> holdsLock = Fnn.sp(mutex, (Throwables.Predicate<Object, IOException>) o -> Thread.holdsLock(mutex));
        assertTrue(holdsLock.test("x"));
    }

    @Test
    public void c125_lambdasAndMethodRefs_keepTheirOverloadAndInferredTypes() throws Exception {
        // implicitly typed lambdas pick the overload by arity and infer T from the target, exactly as before
        final Predicate<String> p1 = Fn.pp(s -> s.isEmpty());
        final BiPredicate<String, String> p2 = Fn.pp((a, b) -> a.equals(b));
        final Consumer<String> c1 = Fn.cc(s -> s.length());
        final Function<String, Integer> f1 = Fn.ff(String::length);
        final BiFunction<String, String, Boolean> f2 = Fn.ff(String::equals);
        final Throwables.Predicate<String, IOException> p3 = Fnn.pp(String::isEmpty);
        // explicitly typed lambdas still infer T from the lambda
        final Predicate<String> p4 = Fn.pp((final String s) -> s.startsWith("中"));
        final Function<String, Integer> f3 = Fn.ff((final String s) -> s.codePointCount(0, s.length()));

        assertTrue(p1.test(""));
        assertTrue(p2.test("a", "a"));
        c1.accept(UNICODE);
        assertEquals(4, f1.apply(UNICODE));
        assertTrue(f2.apply(UNICODE, UNICODE));
        assertTrue(p3.test(""));
        assertTrue(p4.test(UNICODE));
        assertEquals(3, f3.apply(UNICODE));

        // stream arguments are unaffected
        assertEquals(Arrays.asList("a", ""), N.filter(Arrays.asList("a", "", UNICODE), Fn.pp(T_PRED).negate()));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // H4-02 Fn.shutdown: a shutdown() that throws is not recorded, so a later run() tries again (doc-only lock)
    // ---------------------------------------------------------------------------------------------------------------

    /** An executor whose first {@code failures} shutdown() calls throw SecurityException. */
    private static final class FlakyShutdownExecutor extends AbstractExecutorService {
        final AtomicInteger shutdownCalls = new AtomicInteger();
        final AtomicInteger awaitCalls = new AtomicInteger();
        private final int failures;
        private volatile boolean shutdown;

        FlakyShutdownExecutor(final int failures) {
            this.failures = failures;
        }

        @Override
        public void shutdown() {
            if (shutdownCalls.incrementAndGet() <= failures) {
                throw new SecurityException("denied " + shutdownCalls.get());
            }
            shutdown = true;
        }

        @Override
        public List<java.lang.Runnable> shutdownNow() {
            shutdown();
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(final long timeout, final TimeUnit unit) {
            awaitCalls.incrementAndGet();
            return shutdown;
        }

        @Override
        public void execute(final java.lang.Runnable command) {
            command.run();
        }
    }

    @Test
    public void h402_shutdown_failedShutdownIsRetriedThenRunsOnlyOnce() {
        final FlakyShutdownExecutor service = new FlakyShutdownExecutor(1);
        final java.lang.Runnable task = Fn.shutdown(service);

        final SecurityException e = assertThrows(SecurityException.class, task::run);
        assertEquals("denied 1", e.getMessage());
        assertFalse(service.isShutdown());

        task.run(); // retried
        assertTrue(service.isShutdown());
        assertEquals(2, service.shutdownCalls.get());

        task.run(); // recorded now: no further call
        assertEquals(2, service.shutdownCalls.get());
    }

    @Test
    public void h402_shutdownWithTimeout_failedShutdownIsRetriedAndDoesNotWait() {
        final FlakyShutdownExecutor service = new FlakyShutdownExecutor(2);
        final java.lang.Runnable task = Fn.shutdown(service, 1, TimeUnit.MILLISECONDS);

        assertThrows(SecurityException.class, task::run);
        assertThrows(SecurityException.class, task::run);
        assertEquals(0, service.awaitCalls.get()); // a failed shutdown does not wait for termination

        task.run();
        assertTrue(service.isShutdown());
        assertEquals(3, service.shutdownCalls.get());
        assertEquals(1, service.awaitCalls.get());

        task.run();
        assertEquals(3, service.shutdownCalls.get());
        assertEquals(1, service.awaitCalls.get());
    }

    @Test
    public void h402_close_incontrast_doesNotRetryAFailedClose() {
        final AtomicInteger closeCalls = new AtomicInteger();
        final java.lang.Runnable task = Fn.close(() -> {
            closeCalls.incrementAndGet();
            throw new IllegalStateException("close failed");
        });

        assertThrows(IllegalStateException.class, task::run);
        task.run();
        assertEquals(1, closeCalls.get());
    }
}
