package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import com.landawn.abacus.exception.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the 2026-09-24 review of {@link Throwables} (C-054 .. C-058, T1-06, T1-08, T1-10).
 */
public class ThrowablesReview20260924Test extends TestBase {

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

    // ---------------------------------------------------------------- C-054 filter: re-entrant close

    @Test
    public void testFilter_predicateClosesAndRejects_endsCleanly() throws Exception {
        final Object[] holder = new Object[1];
        final Throwables.Iterator<Integer, Exception> filtered = Throwables.Iterator.<Integer, Exception> of(1, 2, 3, 4).filter(x -> {
            if (x == 2) {
                ((Throwables.Iterator<?, ?>) holder[0]).closeResource();
                return false;
            }
            return x % 2 == 1;
        });
        holder[0] = filtered;

        assertEquals(1, filtered.next());
        assertFalse(filtered.hasNext()); // was NullPointerException on "this.iter"
        assertFalse(filtered.hasNext());
        assertThrows(NoSuchElementException.class, filtered::next);
    }

    @Test
    public void testFilter_predicateClosesAndAccepts_neverHasNextTrueThenNoSuchElement() throws Exception {
        final Object[] holder = new Object[1];
        final Throwables.Iterator<String, Exception> filtered = Throwables.Iterator.<String, Exception> of("a", "é中", "c").filter(x -> {
            if (x.equals("é中")) {
                ((Throwables.Iterator<?, ?>) holder[0]).closeResource();
            }
            return true;
        });
        holder[0] = filtered;

        assertEquals("a", filtered.next());
        assertFalse(filtered.hasNext()); // was true, followed by NoSuchElementException from next()
        assertThrows(NoSuchElementException.class, filtered::next);
    }

    @Test
    public void testFilter_predicateClosesOnFirstElement_emptyResult() throws Exception {
        final Object[] holder = new Object[1];
        final Throwables.Iterator<Integer, Exception> filtered = Throwables.Iterator.<Integer, Exception> of(1).filter(x -> {
            ((Throwables.Iterator<?, ?>) holder[0]).closeResource();
            return true;
        });
        holder[0] = filtered;

        assertEquals(0, filtered.count());
        assertTrue(filtered.toList().isEmpty());
    }

    @Test
    public void testFilter_sourceHasNextClosesWrapper_endsCleanly() throws Exception {
        final Object[] holder = new Object[1];
        final AtomicInteger calls = new AtomicInteger();
        final Throwables.Iterator<Integer, Exception> filtered = hooked(Arrays.asList(1, 2, 3), () -> {
            if (calls.incrementAndGet() == 2) {
                ((Throwables.Iterator<?, ?>) holder[0]).closeResource();
            }
        }).filter(x -> true);
        holder[0] = filtered;

        assertEquals(1, filtered.next());
        assertFalse(filtered.hasNext()); // source reported true after the close; element 2 must not be surfaced
        assertThrows(NoSuchElementException.class, filtered::next);
    }

    @Test
    public void testFilter_normalBehaviourUnchanged() throws Exception {
        assertEquals(Arrays.asList(2, 4), Throwables.Iterator.<Integer, Exception> of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0).toList());
        assertTrue(Throwables.Iterator.<Integer, Exception> empty().filter(x -> true).toList().isEmpty());
        assertEquals(Arrays.asList((String) null), Throwables.Iterator.<String, Exception> of(null, "x").filter(x -> x == null).toList());
        assertThrows(IllegalArgumentException.class, () -> Throwables.Iterator.<Integer, Exception> of(1).filter(null));

        final Throwables.Iterator<Integer, Exception> closed = Throwables.Iterator.<Integer, Exception> of(1, 2).filter(x -> true);
        closed.closeResource();
        assertFalse(closed.hasNext());
    }

    // ---------------------------------------------------------------- C-054 concat: re-entrant close

    @Test
    public void testConcat_sourceHasNextClosesConcat_noConcurrentModification() throws Exception {
        final Object[] holder = new Object[1];
        final Throwables.Iterator<Integer, Exception> closer = hooked(new ArrayList<Integer>(), () -> ((Throwables.Iterator<?, ?>) holder[0]).closeResource());
        final Throwables.Iterator<Integer, Exception> concat = Throwables.Iterator.concat(closer, Throwables.Iterator.<Integer, Exception> of(7),
                Throwables.Iterator.<Integer, Exception> of(8));
        holder[0] = concat;

        assertFalse(concat.hasNext()); // was ConcurrentModificationException
        assertFalse(concat.hasNext());
        assertThrows(NoSuchElementException.class, concat::next);
    }

    @Test
    public void testConcat_sourceClosesConcatButReportsTrue_endsCleanly() throws Exception {
        final Object[] holder = new Object[1];
        final AtomicInteger calls = new AtomicInteger();
        final Throwables.Iterator<Integer, Exception> first = hooked(Arrays.asList(1, 2), () -> {
            // the second hasNext() call closes the concat while element 2 is still available
            if (calls.incrementAndGet() == 2) {
                ((Throwables.Iterator<?, ?>) holder[0]).closeResource();
            }
        });
        final Throwables.Iterator<Integer, Exception> concat = Throwables.Iterator.concat(first, Throwables.Iterator.<Integer, Exception> of(9));
        holder[0] = concat;

        final List<Integer> seen = new ArrayList<>();
        while (concat.hasNext()) {
            seen.add(concat.next());
        }

        assertEquals(Arrays.asList(1), seen); // was ConcurrentModificationException
        assertThrows(NoSuchElementException.class, concat::next);
    }

    @Test
    public void testConcat_nextAfterCloseFromInsideHasNext_throwsNoSuchElementNotNpe() throws Exception {
        final Object[] holder = new Object[1];
        final Throwables.Iterator<Integer, Exception> first = hooked(Arrays.asList(1, 2), () -> {
            if (holder[0] != null) {
                ((Throwables.Iterator<?, ?>) holder[0]).closeResource();
            }
        });
        final Throwables.Iterator<Integer, Exception> concat = Throwables.Iterator.concat(Arrays.asList(first));
        holder[0] = concat;

        assertThrows(NoSuchElementException.class, concat::next); // boundary lock (base already ended here)
    }

    @Test
    public void testConcat_normalBehaviourUnchanged() throws Exception {
        final Throwables.Iterator<String, Exception> c = Throwables.Iterator.concat(Throwables.Iterator.<String, Exception> of("a"), null,
                Throwables.Iterator.<String, Exception> empty(), Throwables.Iterator.<String, Exception> of("b", "中"));
        assertEquals(Arrays.asList("a", "b", "中"), c.toList());
        assertFalse(c.hasNext());
        assertThrows(NoSuchElementException.class, c::next);

        assertFalse(Throwables.Iterator.<String, Exception> concat(new ArrayList<Throwables.Iterator<String, Exception>>()).hasNext());
        assertEquals(3, Throwables.Iterator.concat(Throwables.Iterator.<Integer, Exception> of(1, 2), Throwables.Iterator.<Integer, Exception> of(3)).count());

        final Throwables.Iterator<Integer, Exception> closed = Throwables.Iterator.concat(Throwables.Iterator.<Integer, Exception> of(1));
        closed.closeResource();
        assertFalse(closed.hasNext());
    }

    // ---------------------------------------------------------------- C-055 interrupt flag on handler/fallback paths

    private static Throwables.Callable<String, Exception> throwing(final Exception e) {
        return () -> {
            throw e;
        };
    }

    @Test
    public void testRunWithHandler_invocationTargetWrappedInterrupt_restoresFlag() {
        final List<Throwable> seen = new ArrayList<>();
        final InvocationTargetException ite = new InvocationTargetException(new InterruptedException());

        Throwables.run(() -> {
            throw ite;
        }, seen::add);

        assertTrue(Thread.interrupted());
        assertSame(ite, seen.get(0)); // the handler still receives the un-peeled wrapper
    }

    @Test
    public void testCallWithFunction_undeclaredWrappedInterrupt_restoresFlag() {
        assertEquals("h", Throwables.call(throwing(new UndeclaredThrowableException(new InterruptedException())),
                (java.util.function.Function<Throwable, String>) e -> "h"));
        assertTrue(Thread.interrupted());
    }

    @Test
    public void testCallWithSupplier_wrappedInterrupt_restoresFlag() {
        assertEquals("s", Throwables.call(throwing(new InvocationTargetException(new InterruptedException())),
                (java.util.function.Supplier<String>) () -> "s"));
        assertTrue(Thread.interrupted());
    }

    @Test
    public void testCallWithDefault_wrappedInterrupt_restoresFlag() {
        assertEquals("d", Throwables.call(throwing(new InvocationTargetException(new InterruptedException())), "d"));
        assertTrue(Thread.interrupted());
    }

    @Test
    public void testCallPredicateSupplier_acceptingBranch_wrappedInterrupt_restoresFlag() {
        assertEquals("s", Throwables.call(throwing(new UndeclaredThrowableException(new InterruptedException())), e -> true,
                (java.util.function.Supplier<String>) () -> "s"));
        assertTrue(Thread.interrupted());
    }

    @Test
    public void testCallPredicateDefault_acceptingBranch_wrappedInterrupt_restoresFlag() {
        assertEquals("d", Throwables.call(throwing(new InvocationTargetException(new InterruptedException())), e -> true, "d"));
        assertTrue(Thread.interrupted());
    }

    @Test
    public void testCallPredicate_rejectingBranch_stillRestoresFlag() {
        assertThrows(RuntimeException.class,
                () -> Throwables.call(throwing(new InvocationTargetException(new InterruptedException())), e -> false, "d"));
        assertTrue(Thread.interrupted());
    }

    @Test
    public void testHandler_nestedWrappers_restoresFlag() {
        Throwables.run(() -> {
            throw new InvocationTargetException(new UndeclaredThrowableException(new InvocationTargetException(new InterruptedException())));
        }, e -> {
        });
        assertTrue(Thread.interrupted());
    }

    @Test
    public void testHandler_bareInterrupt_stillRestoresFlag() {
        Throwables.run(() -> {
            throw new InterruptedException();
        }, e -> {
        });
        assertTrue(Thread.interrupted());
    }

    @Test
    public void testHandler_executionExceptionWrappedInterrupt_doesNotInterrupt() {
        // Matches ExceptionUtil.toRuntimeException: an ExecutionException reports another thread's interrupt.
        Throwables.run(() -> {
            throw new ExecutionException(new InterruptedException());
        }, e -> {
        });
        assertFalse(Thread.interrupted());

        assertEquals("d", Throwables.call(throwing(new InvocationTargetException(new ExecutionException(new InterruptedException()))), "d"));
        assertFalse(Thread.interrupted());

        // and the rethrow path agrees
        assertThrows(RuntimeException.class, () -> Throwables.run(() -> {
            throw new InvocationTargetException(new ExecutionException(new InterruptedException()));
        }));
        assertFalse(Thread.interrupted());
    }

    @Test
    public void testHandler_nonInterruptFailures_doNotInterrupt() {
        assertEquals("d", Throwables.call(throwing(new InvocationTargetException(new IOException())), "d"));
        assertEquals("d", Throwables.call(throwing(new InvocationTargetException(null)), "d"));
        assertEquals("d", Throwables.call(throwing(new UndeclaredThrowableException(null)), "d"));
        assertEquals("d", Throwables.call(() -> {
            throw new StackOverflowError();
        }, "d"));
        assertFalse(Thread.interrupted());
    }

    @Test
    public void testHandler_cyclicWrapperChain_terminatesWithoutInterrupt() {
        final InvocationTargetException cyclic = new InvocationTargetException(null) {
            private static final long serialVersionUID = 1L;

            @Override
            public synchronized Throwable getCause() {
                return this;
            }
        };

        assertEquals("d", Throwables.call(throwing(cyclic), "d"));
        assertFalse(Thread.interrupted());
    }

    // ---------------------------------------------------------------- C-005 no Comparable bound on the default-value overloads

    private static String failString() throws IOException {
        throw new IOException("fail");
    }

    private static List<String> failList() throws IOException {
        throw new IOException("fail");
    }

    private static Object failObject() throws IOException {
        throw new IOException("fail");
    }

    private static String fallback() {
        return "mref";
    }

    @Test
    public void testCall_untypedHandlerLambda_bindsFunctionOverload() {
        final AtomicInteger handled = new AtomicInteger();
        final String r = Throwables.call(() -> failString(), e -> {
            handled.incrementAndGet();
            return "h:" + e.getMessage();
        });
        assertEquals("h:fail", r);
        assertEquals(1, handled.get());

        assertEquals("s", Throwables.call(() -> "s", e -> "h"));
        assertEquals("fail", Throwables.call(() -> failString(), ex -> ex.getMessage()));
    }

    @Test
    public void testCall_untypedSupplierLambdaAndMethodRef_bindSupplierOverload() {
        assertEquals("x", Throwables.call(() -> failString(), () -> "x"));
        assertEquals("mref", Throwables.call(() -> failString(), ThrowablesReview20260924Test::fallback));
        assertEquals("中", Throwables.call(() -> "中", () -> "x"));
        assertEquals("x", Throwables.call(() -> failString(), e -> true, () -> "x"));
        assertEquals("mref", Throwables.call(() -> failString(), e -> e instanceof IOException, ThrowablesReview20260924Test::fallback));
    }

    @Test
    public void testCall_nonComparableDefault() {
        final List<String> empty = java.util.Collections.emptyList();
        assertSame(empty, Throwables.call(() -> failList(), empty));
        assertEquals(Arrays.asList("a"), Throwables.call(() -> Arrays.asList("a"), java.util.Collections.<String> emptyList()));

        final List<String> dflt = new ArrayList<>();
        assertSame(dflt, Throwables.call(() -> failList(), e -> true, dflt));
        assertThrows(RuntimeException.class, () -> Throwables.call(() -> failList(), e -> false, dflt));

        final int[] arr = { 1 };
        assertSame(arr, Throwables.call(() -> {
            throw new IOException();
        }, arr));
    }

    @Test
    public void testCall_nullDefaults() {
        assertNull(Throwables.call(() -> failString(), (String) null));
        assertNull(Throwables.call(() -> failString(), e -> true, (String) null));

        // A bare null third argument binds to the Supplier overload, which rejects it.
        assertThrows(IllegalArgumentException.class, () -> Throwables.call(() -> failString(), e -> true, null));
    }

    @Test
    public void testCall_supplierVariableOfOtherStaticType_isTheDefaultValue() {
        final java.util.function.Supplier<? extends String> wildcard = () -> "never";
        final Object r = Throwables.call(() -> failObject(), wildcard);
        assertSame(wildcard, r);

        final java.util.function.Supplier<String> exact = () -> "invoked";
        assertEquals("invoked", Throwables.call(() -> failString(), exact));
    }

    // ---------------------------------------------------------------- C-056 wildcard type parameters (compile-time locks)

    @Test
    public void testDefer_acceptsNarrowerExceptionAndElementTypes() throws Exception {
        final Throwables.Iterator<String, Exception> lines = Throwables.Iterator.defer(() -> Throwables.Iterator.ofLines(new StringReader("x\n中")));
        assertEquals(Arrays.asList("x", "中"), lines.toList());

        final java.util.function.Supplier<Throwables.Iterator<Integer, IOException>> sup = () -> Throwables.Iterator.of(Arrays.asList(1, 2));
        final Throwables.Iterator<Number, Exception> numbers = Throwables.Iterator.defer(sup);
        assertEquals(Arrays.<Number> asList(1, 2), numbers.toList());
        numbers.closeResource();
        assertFalse(numbers.hasNext());
    }

    @Test
    public void testFilter_acceptsNarrowerExceptionPredicate() throws Exception {
        final Throwables.Predicate<String, IOException> nonEmpty = s -> {
            if (s == null) {
                throw new IOException("null");
            }
            return !s.isEmpty();
        };
        final Throwables.Iterator<String, Exception> it = Throwables.Iterator.<String, Exception> of("a", "", "b");
        assertEquals(Arrays.asList("a", "b"), it.filter(nonEmpty).toList());

        final Throwables.Iterator<String, Exception> bad = Throwables.Iterator.<String, Exception> of("a", null).filter(nonEmpty);
        assertEquals("a", bad.next());
        assertThrows(IOException.class, bad::hasNext);
    }

    @Test
    public void testMap_acceptsNarrowerExceptionAndCovariantResult() throws Exception {
        final Throwables.Function<String, Integer, IOException> len = String::length;
        final Throwables.Iterator<Integer, Exception> lengths = Throwables.Iterator.<String, Exception> of("ab", "中").map(len);
        assertEquals(Arrays.asList(2, 1), lengths.toList());

        final Throwables.Function<Object, Integer, Exception> hash = o -> 7;
        final Throwables.Iterator<Number, Exception> nums = Throwables.Iterator.<String, Exception> of("x").map(hash);
        assertEquals(Arrays.<Number> asList(7), nums.toList());
    }

    // ---------------------------------------------------------------- C-057 covariant unchecked() on operators

    @Test
    public void testUnaryOperatorUnchecked_isAnOperator() {
        final Throwables.UnaryOperator<String, IOException> upper = s -> s.toUpperCase();
        final com.landawn.abacus.util.function.UnaryOperator<String> adapter = upper.unchecked();
        assertTrue(adapter instanceof java.util.function.UnaryOperator);

        final List<String> list = new ArrayList<>(Arrays.asList("a", "é"));
        list.replaceAll(upper.unchecked());
        assertEquals(Arrays.asList("A", "É"), list);

        final Throwables.UnaryOperator<Integer, Exception> inc = x -> x + 1;
        assertEquals(Arrays.asList(0, 1, 2), Stream.iterate(0, inc.unchecked()).limit(3).collect(Collectors.toList()));
    }

    @Test
    public void testUnaryOperatorUnchecked_convertsFailures() {
        final Throwables.UnaryOperator<String, IOException> failing = s -> {
            throw new IOException("boom");
        };
        final UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> failing.unchecked().apply("x"));
        assertEquals("boom", e.getCause().getMessage());

        final IllegalStateException ise = new IllegalStateException();
        final Throwables.UnaryOperator<String, RuntimeException> rte = s -> {
            throw ise;
        };
        assertSame(ise, assertThrows(IllegalStateException.class, () -> rte.unchecked().apply("x")));

        // An Error is converted to a runtime exception, exactly like Function.unchecked() (pinned elsewhere).
        final Throwables.UnaryOperator<String, RuntimeException> err = s -> {
            throw new AssertionError("e");
        };
        assertThrows(RuntimeException.class, () -> err.unchecked().apply("x"));

        final Throwables.UnaryOperator<String, RuntimeException> nul = s -> null;
        assertNull(nul.unchecked().apply("x"));
    }

    @Test
    public void testBinaryOperatorUnchecked_isAnOperator() {
        final Throwables.BinaryOperator<Integer, IOException> add = (a, b) -> a + b;
        assertTrue(add.unchecked() instanceof java.util.function.BinaryOperator);
        assertEquals(6, Stream.of(1, 2, 3).reduce(add.unchecked()).get());
        assertEquals(6, Stream.of(1, 2, 3).reduce(0, add.unchecked()));
        assertFalse(Stream.<Integer> empty().reduce(add.unchecked()).isPresent());

        final Throwables.BinaryOperator<Integer, IOException> failing = (a, b) -> {
            throw new IOException("x");
        };
        assertThrows(UncheckedIOException.class, () -> Stream.of(1, 2).reduce(failing.unchecked()));
    }

    @Test
    public void testFunctionSideOperators_inheritCovariantUnchecked() {
        final com.landawn.abacus.util.function.UnaryOperator<String> u = s -> s + "!";
        final com.landawn.abacus.util.function.UnaryOperator<String> uu = u.unchecked();
        assertEquals("a!", uu.apply("a"));

        final com.landawn.abacus.util.function.BinaryOperator<String> b = (x, y) -> x + y;
        final com.landawn.abacus.util.function.BinaryOperator<String> bb = b.unchecked();
        assertEquals("xy", bb.apply("x", "y"));
    }

    // ---------------------------------------------------------------- T1-08 LazyInitializer does not retain a rejected value

    @Test
    public void testLazyInit_rejectedAttemptIsNotRetained() throws Exception {
        final Object rejected = new Object();
        final AtomicInteger attempts = new AtomicInteger();
        final Object[] self = new Object[1];
        @SuppressWarnings("unchecked")
        final Throwables.Supplier<Object, Exception> lazy = N.lazyInitChecked(() -> {
            if (attempts.incrementAndGet() == 1) {
                try {
                    ((Throwables.Supplier<Object, Exception>) self[0]).get();
                } catch (final IllegalStateException swallowed) {
                    // the supplier swallows the recursive-access failure
                }
                return rejected;
            }
            return "ok";
        });
        self[0] = lazy;

        assertThrows(IllegalStateException.class, lazy::get);

        final Field value = lazy.getClass().getDeclaredField("value");
        value.setAccessible(true);
        assertNull(value.get(lazy)); // the rejected object used to stay referenced here

        assertEquals("ok", lazy.get());
        assertEquals("ok", value.get(lazy));
        assertEquals(2, attempts.get());
    }

    // ---------------------------------------------------------------- T1-10 / T1-06 documentation locks

    @Test
    public void testToArray_nullTerminatorContract() throws Exception {
        final String[] big = { "x", "x", "x", "x" };
        final String[] result = Throwables.Iterator.<String, Exception> of("a", "b").toArray(big);
        assertSame(big, result);
        assertArrayEquals(new String[] { "a", "b", null, "x" }, result);

        final String[] exact = new String[2];
        assertArrayEquals(new String[] { "a", "b" }, Throwables.Iterator.<String, Exception> of("a", "b").toArray(exact));

        final String[] grown = Throwables.Iterator.<String, Exception> of("a", "b").toArray(new String[0]);
        assertEquals(String[].class, grown.getClass());
        assertArrayEquals(new String[] { "a", "b" }, grown);
    }

    @Test
    public void testCloseResource_resourceFreeFactoriesKeepIterating() throws Exception {
        final Throwables.Iterator<Integer, Exception> arr = Throwables.Iterator.of(1, 2);
        arr.closeResource();
        assertEquals(Arrays.asList(1, 2), arr.toList());

        final Throwables.Iterator<Integer, Exception> range = Throwables.Iterator.of(new Integer[] { 1, 2, 3 }, 1, 3);
        range.closeResource();
        assertEquals(Arrays.asList(2, 3), range.toList());

        final Throwables.Iterator<Integer, Exception> one = Throwables.Iterator.just(1);
        one.closeResource();
        assertEquals(Arrays.asList(1), one.toList());

        final Throwables.Iterator<Integer, Exception> iterable = Throwables.Iterator.of(Arrays.asList(1, 2));
        iterable.closeResource();
        assertEquals(Arrays.asList(1, 2), iterable.toList());

        final Throwables.Iterator<Integer, Exception> wrapped = Throwables.Iterator.of(Arrays.asList(1, 2).iterator());
        wrapped.closeResource();
        assertEquals(Arrays.asList(1, 2), wrapped.toList());
    }

    @Test
    public void testCloseResource_resourceBackedFactoriesReportExhaustion() throws Exception {
        final Throwables.Iterator<String, IOException> lines = Throwables.Iterator.ofLines(new StringReader("a\nb"));
        lines.closeResource();
        assertFalse(lines.hasNext());

        final Throwables.Iterator<Integer, Exception> mapped = Throwables.Iterator.<Integer, Exception> of(1, 2).map(x -> x);
        mapped.closeResource();
        assertFalse(mapped.hasNext());

        final Throwables.Iterator<Integer, Exception> deferred = Throwables.Iterator.defer(() -> Throwables.Iterator.<Integer, Exception> of(1));
        deferred.closeResource();
        assertEquals(0, deferred.count());
    }
}
