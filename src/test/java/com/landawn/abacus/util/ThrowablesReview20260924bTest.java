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
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the cycle-2 review of {@link Throwables} (C-087, C-108, R7-03, D2-06).
 */
public class ThrowablesReview20260924bTest extends TestBase {

    @AfterEach
    public void clearInterrupt() {
        Thread.interrupted();
    }

    /** A source iterator whose {@code hasNext()} runs a hook first (used to close an enclosing wrapper). */
    private static <T> Throwables.Iterator<T, Exception> hooked(final List<T> values, final java.lang.Runnable onHasNext) {
        final java.util.Iterator<T> it = values.iterator();

        return new Throwables.Iterator<>() {
            @Override
            public boolean hasNext() {
                onHasNext.run();
                return it.hasNext();
            }

            @Override
            public T next() {
                return it.next();
            }
        };
    }

    /** Closes {@code holder[0]} on the {@code n}-th call. */
    private static java.lang.Runnable closeOnCall(final Object[] holder, final int n) {
        final AtomicInteger calls = new AtomicInteger();

        return () -> {
            if (calls.incrementAndGet() == n && holder[0] != null) {
                ((Throwables.Iterator<?, ?>) holder[0]).closeResource();
            }
        };
    }

    // ---------------------------------------------------------------- C-087 map: source hasNext() closes the wrapper

    @Test
    public void testMap_sourceHasNextClosesWrapper_endsCleanly() throws Exception {
        final Object[] holder = new Object[1];
        final Throwables.Iterator<Integer, Exception> mapped = hooked(Arrays.asList(1, 2, 3), closeOnCall(holder, 2)).map(x -> x * 10);
        holder[0] = mapped;

        assertTrue(mapped.hasNext());
        assertEquals(10, mapped.next());
        assertFalse(mapped.hasNext()); // was true, then next() threw NoSuchElementException
        assertFalse(mapped.hasNext());
        assertThrows(NoSuchElementException.class, mapped::next);
    }

    @Test
    public void testMap_sourceClosesButReportsTrue_whileLoopEndsCleanly() throws Exception {
        final Object[] holder = new Object[1];
        final Throwables.Iterator<String, Exception> mapped = hooked(Arrays.asList("a", "中", "😀"), closeOnCall(holder, 3)).map(s -> s + "!");
        holder[0] = mapped;

        final List<String> seen = new ArrayList<>();
        while (mapped.hasNext()) {
            seen.add(mapped.next());
        }

        assertEquals(Arrays.asList("a!", "中!"), seen);
        assertThrows(NoSuchElementException.class, mapped::next);
    }

    @Test
    public void testMap_sourceClosesOnFirstHasNext_emptyResult() throws Exception {
        final Object[] holder = new Object[1];
        final Throwables.Iterator<Integer, Exception> mapped = hooked(Arrays.asList(1, 2), closeOnCall(holder, 1)).map(x -> x);
        holder[0] = mapped;

        assertTrue(mapped.toList().isEmpty());
        assertFalse(mapped.hasNext());
    }

    @Test
    public void testMap_sourceClosesOnEmptySource_reportsFalse() throws Exception {
        final Object[] holder = new Object[1];
        final Throwables.Iterator<Integer, Exception> mapped = hooked(Collections.<Integer> emptyList(), closeOnCall(holder, 1)).map(x -> x);
        holder[0] = mapped;

        assertFalse(mapped.hasNext());
        assertThrows(NoSuchElementException.class, mapped::next);
    }

    @Test
    public void testMap_mapperClosesWrapper_valueStillReturnedThenExhausted() throws Exception {
        final Object[] holder = new Object[1];
        final Throwables.Iterator<Integer, Exception> mapped = Throwables.Iterator.<Integer, Exception> of(1, 2, 3).map(x -> {
            ((Throwables.Iterator<?, ?>) holder[0]).closeResource();
            return x * 2;
        });
        holder[0] = mapped;

        assertEquals(2, mapped.next());
        assertFalse(mapped.hasNext());
        assertThrows(NoSuchElementException.class, mapped::next);
    }

    @Test
    public void testMap_normalBehaviourUnchanged() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3), Throwables.Iterator.<String, Exception> of("1", "2", "3").map(Integer::parseInt).toList());
        assertTrue(Throwables.Iterator.<String, Exception> empty().map(s -> s).toList().isEmpty());
        assertEquals(Arrays.asList((String) null, "x"), Throwables.Iterator.<String, Exception> of(null, "x").map(s -> s).toList());
        assertEquals(3, Throwables.Iterator.<Integer, Exception> of(1, 2, 3).map(x -> x).count());
        assertThrows(IllegalArgumentException.class, () -> Throwables.Iterator.<Integer, Exception> of(1).map(null));

        final Throwables.Iterator<Integer, Exception> closed = Throwables.Iterator.<Integer, Exception> of(1, 2).map(x -> x);
        closed.closeResource();
        assertFalse(closed.hasNext());
        assertThrows(NoSuchElementException.class, closed::next);
    }

    // ---------------------------------------------------------------- C-087 defer: source hasNext() closes the wrapper

    @Test
    public void testDefer_sourceHasNextClosesWrapper_endsCleanly() throws Exception {
        final Object[] holder = new Object[1];
        final Throwables.Iterator<Integer, Exception> deferred = Throwables.Iterator.defer(() -> hooked(Arrays.asList(1, 2, 3), closeOnCall(holder, 2)));
        holder[0] = deferred;

        assertTrue(deferred.hasNext());
        assertEquals(1, deferred.next());
        assertFalse(deferred.hasNext()); // was true, then next() threw NoSuchElementException
        assertFalse(deferred.hasNext());
        assertThrows(NoSuchElementException.class, deferred::next);
        assertEquals(0, deferred.count());
    }

    @Test
    public void testDefer_sourceClosesButReportsTrue_whileLoopEndsCleanly() throws Exception {
        final Object[] holder = new Object[1];
        final Throwables.Iterator<String, Exception> deferred = Throwables.Iterator
                .defer(() -> hooked(Arrays.asList("a", "中", "😀"), closeOnCall(holder, 3)));
        holder[0] = deferred;

        final List<String> seen = new ArrayList<>();
        while (deferred.hasNext()) {
            seen.add(deferred.next());
        }

        assertEquals(Arrays.asList("a", "中"), seen);
        assertThrows(NoSuchElementException.class, deferred::next);
    }

    @Test
    public void testDefer_sourceClosesOnFirstHasNext_emptyResult() throws Exception {
        final Object[] holder = new Object[1];
        final Throwables.Iterator<Integer, Exception> deferred = Throwables.Iterator.defer(() -> hooked(Arrays.asList(1, 2), closeOnCall(holder, 1)));
        holder[0] = deferred;

        assertFalse(deferred.hasNext()); // the source reported true, but the wrapper was closed by that call
        assertThrows(NoSuchElementException.class, deferred::next);
    }

    @Test
    public void testDefer_normalBehaviourUnchanged() throws Exception {
        assertEquals(Arrays.asList("a", "中"), Throwables.Iterator.<String, Exception> defer(() -> Throwables.Iterator.of("a", "中")).toList());
        assertFalse(Throwables.Iterator.<String, Exception> defer(Throwables.Iterator::empty).hasNext());
        assertEquals(2, Throwables.Iterator.<Integer, Exception> defer(() -> Throwables.Iterator.of(1, 2)).count());

        final AtomicInteger created = new AtomicInteger();
        final Throwables.Iterator<Integer, Exception> closed = Throwables.Iterator.defer(() -> {
            created.incrementAndGet();
            return Throwables.Iterator.<Integer, Exception> of(1);
        });
        closed.closeResource();
        assertFalse(closed.hasNext());
        assertThrows(NoSuchElementException.class, closed::next);
        assertEquals(0, created.get());
    }

    // ---------------------------------------------------------------- C-108 call(..) overload selection (doc locks)

    private static String failString() throws IOException {
        throw new IOException("fail");
    }

    private static java.util.function.Supplier<String> failSupplier() throws IOException {
        throw new IOException("fail");
    }

    @Test
    public void testCall_abacusSupplierVariable_isInvoked() {
        final com.landawn.abacus.util.function.Supplier<String> t = () -> "fallback";
        assertEquals("fallback", Throwables.call(() -> failString(), t));
        assertEquals("fallback", Throwables.call(() -> failString(), e -> true, t));
    }

    @Test
    public void testCall_extendsWildcardSupplier_dependsOnCommand() {
        final java.util.function.Supplier<? extends String> s = () -> "fallback";

        // A command that only throws puts no constraint on R, so the Supplier overload applies and s is invoked.
        final Object a = Throwables.call(() -> {
            throw new IOException();
        }, s);
        assertEquals("fallback", a);

        // A String-returning command cannot fit s's unknown subtype, so s becomes the default value.
        final Object b = Throwables.call(() -> failString(), s);
        assertSame(s, b);

        final Object c = Throwables.call(() -> {
            throw new IOException();
        }, e -> true, s);
        assertEquals("fallback", c);
        assertSame(s, Throwables.call(() -> failString(), e -> true, s));

        // v::get always selects the Supplier overload.
        assertEquals("fallback", Throwables.call(() -> failString(), s::get));
    }

    @Test
    public void testCall_supplierOfWiderOrSuperType_isInvoked() {
        final java.util.function.Supplier<Object> o = () -> "fallback";
        final Object r = Throwables.call(() -> failString(), o);
        assertEquals("fallback", r);

        final java.util.function.Supplier<? super String> sup = () -> "fallback";
        final Object r2 = Throwables.call(() -> failString(), sup);
        assertEquals("fallback", r2);

        final java.util.function.Supplier<CharSequence> cs = () -> "fallback";
        final CharSequence r3 = Throwables.call(() -> failString(), cs);
        assertEquals("fallback", r3);
    }

    @Test
    public void testCall_nonMatchingSupplierVariables_areTheDefaultValue() {
        final Throwables.Supplier<String, RuntimeException> u = () -> "fallback";
        assertSame(u, Throwables.call(() -> failString(), u));
        assertSame(u, Throwables.call(() -> failString(), e -> true, u));

        final java.util.function.Supplier<Integer> i = () -> 1;
        assertSame(i, Throwables.call(() -> failString(), i));
        assertSame(i, Throwables.call(() -> failString(), e -> true, i));

        // The command itself returns a Supplier<String>: the supplier argument is the intended default value.
        final java.util.function.Supplier<String> v = () -> "never";
        final java.util.function.Supplier<String> r = Throwables.call(() -> failSupplier(), v);
        assertSame(v, r);
        assertSame(v, Throwables.call(() -> failSupplier(), e -> true, v));
    }

    @Test
    public void testCall_functionVariables_invokedOnlyWhenTheyAcceptThrowable() {
        final com.landawn.abacus.util.function.Function<Throwable, String> af = e -> "handled:" + e.getMessage();
        assertEquals("handled:fail", Throwables.call(() -> failString(), af));

        final java.util.function.Function<Object, String> of = e -> "handled";
        assertEquals("handled", Throwables.call(() -> failString(), of));

        // Function<Throwable, Integer> with a String command: R is inferred from both, and the handler is invoked.
        final java.util.function.Function<Throwable, Integer> intHandler = e -> 7;
        final Object r = Throwables.call(() -> failString(), intHandler);
        assertEquals(7, r);

        final java.util.function.Function<Exception, String> f = e -> "handled";
        assertSame(f, Throwables.call(() -> failString(), f));

        final Throwables.Function<Throwable, String, RuntimeException> tf = e -> "handled";
        assertSame(tf, Throwables.call(() -> failString(), tf));
    }

    @Test
    public void testCall_matchingVariables_successfulCommandIgnoresThem() {
        final com.landawn.abacus.util.function.Supplier<String> t = () -> "fallback";
        assertEquals("ok", Throwables.call(() -> "ok", t));

        final java.util.function.Supplier<? extends String> s = () -> "fallback";
        assertEquals("ok", Throwables.call(() -> "ok", s));
    }

    // ---------------------------------------------------------------- D2-06 interrupt: only ITE/UTE are looked through

    private static void assertNotInterruptedAfter(final java.lang.Runnable r) {
        Thread.interrupted();
        try {
            r.run();
        } catch (final RuntimeException e) {
            // the rethrow paths convert the failure; only the interrupt flag matters here
        }
        assertFalse(Thread.interrupted());
    }

    @Test
    public void testInterrupt_causeOfOtherException_notRestored() {
        final Exception e = new RuntimeException(new InterruptedException());

        assertNotInterruptedAfter(() -> Throwables.run(() -> {
            throw e;
        }));
        assertNotInterruptedAfter(() -> Throwables.run(() -> {
            throw e;
        }, x -> {
        }));
        assertNotInterruptedAfter(() -> Throwables.call(() -> {
            throw e;
        }));
        assertNotInterruptedAfter(() -> Throwables.call(() -> {
            throw e;
        }, x -> "h"));
        assertNotInterruptedAfter(() -> Throwables.call(() -> {
            throw e;
        }, () -> "s"));
        assertNotInterruptedAfter(() -> Throwables.call(() -> {
            throw e;
        }, "v"));
        assertNotInterruptedAfter(() -> Throwables.call(() -> {
            throw e;
        }, x -> true, () -> "s"));
        assertNotInterruptedAfter(() -> Throwables.call(() -> {
            throw e;
        }, x -> false, "v"));
    }

    @Test
    public void testInterrupt_suppressedInterrupt_notRestored() {
        final IOException e = new IOException("io");
        e.addSuppressed(new InterruptedException());

        assertNotInterruptedAfter(() -> Throwables.run(() -> {
            throw e;
        }, x -> {
        }));
        assertNotInterruptedAfter(() -> Throwables.call(() -> {
            throw e;
        }, "v"));
        assertNotInterruptedAfter(() -> Throwables.call(() -> {
            throw e;
        }));
    }

    @Test
    public void testInterrupt_invocationTargetWrapped_stillRestored() {
        Thread.interrupted();
        assertEquals("v", Throwables.call(() -> {
            throw new java.lang.reflect.InvocationTargetException(new InterruptedException());
        }, "v"));
        assertTrue(Thread.interrupted());
    }
}
