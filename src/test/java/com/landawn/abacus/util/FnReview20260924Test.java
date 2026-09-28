package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.Fn.FB;
import com.landawn.abacus.util.Fn.FC;
import com.landawn.abacus.util.Fn.FD;
import com.landawn.abacus.util.Fn.FF;
import com.landawn.abacus.util.Fn.FI;
import com.landawn.abacus.util.Fn.FL;
import com.landawn.abacus.util.Fn.FS;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Predicate;

/**
 * Review fixes 2026-09-24 for {@link Fn}: C-021, C-022, C-023, C-024, C-025, C-026, C-027, F1-08, F1-09, F2-05,
 * F2-06.
 */
public class FnReview20260924Test extends TestBase {

    // ---------------------------------------------------------------------------------------------------------
    // C-021: and/or(Collection<Predicate>) and and/or(List<BiPredicate>) reject null elements eagerly with IAE
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void c021_andCollection_nullElementRejectedAtConstruction() {
        final java.util.function.Predicate<String> p = s -> !s.isEmpty();

        assertThrows(IllegalArgumentException.class, () -> Fn.and(Arrays.asList(p, null)));
        assertThrows(IllegalArgumentException.class, () -> Fn.and(Arrays.asList(null, p)));
        assertThrows(IllegalArgumentException.class, () -> Fn.and(Arrays.asList(p, null, p)));
        assertThrows(IllegalArgumentException.class, () -> Fn.and(Collections.singletonList((java.util.function.Predicate<String>) null)));
    }

    @Test
    public void c021_orCollection_nullElementRejectedEvenWhenShortCircuitWouldHideIt() {
        // Before the fix the null was only reached for inputs that got past the earlier predicates.
        final java.util.function.Predicate<String> alwaysTrue = s -> true;

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Fn.or(Arrays.asList(alwaysTrue, null)));
        assertTrue(e.getMessage().contains("null element"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Fn.or(Arrays.asList(null, alwaysTrue)));
        assertThrows(IllegalArgumentException.class, () -> Fn.or(Collections.singletonList((java.util.function.Predicate<String>) null)));
    }

    @Test
    public void c021_andOrBiPredicateList_nullElementRejectedAtConstruction() {
        final java.util.function.BiPredicate<String, Integer> bp = (s, n) -> s.length() == n;

        assertThrows(IllegalArgumentException.class, () -> Fn.and(Arrays.asList(bp, null)));
        assertThrows(IllegalArgumentException.class, () -> Fn.and(Arrays.asList(null, bp)));
        assertThrows(IllegalArgumentException.class, () -> Fn.or(Arrays.asList(bp, null)));
        assertThrows(IllegalArgumentException.class, () -> Fn.or(Arrays.asList(null, bp)));
    }

    @Test
    public void c021_validCollectionsStillWorkAndAreSnapshotted() {
        final List<java.util.function.Predicate<String>> preds = new ArrayList<>();
        preds.add(s -> !s.isEmpty());
        preds.add(s -> s.length() < 5);

        final Predicate<String> and = Fn.and(preds);
        final Predicate<String> or = Fn.or(preds);
        preds.add(null); // later mutation - including adding a null - does not affect the built predicates

        assertTrue(and.test("abc"));
        assertFalse(and.test(""));
        assertTrue(or.test("")); // "" has length < 5
        assertTrue(or.test("中文字符串串")); // non-empty Unicode string
        assertThrows(IllegalArgumentException.class, () -> Fn.and(new ArrayList<java.util.function.Predicate<String>>()));
        assertThrows(IllegalArgumentException.class, () -> Fn.or((Collection<java.util.function.Predicate<String>>) null));

        final BiPredicate<String, Integer> bAnd = Fn.and(Arrays.<java.util.function.BiPredicate<String, Integer>> asList((s, n) -> s.length() == n, (s, n) -> n > 0));
        assertTrue(bAnd.test("ab", 2));
        assertFalse(bAnd.test("", 0));
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-022: exact T widened to ? super T (compile-time). BEGIN-WIDENING (these do not compile against r9619)
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void c022_notAcceptsSuperTypedPredicates() {
        final java.util.function.Predicate<Object> isNullObj = o -> o == null;
        final Predicate<String> notNullString = Fn.not(isNullObj);
        assertTrue(notNullString.test("x"));
        assertFalse(notNullString.test(null));

        final java.util.function.BiPredicate<Object, Object> same = (a, b) -> a == b;
        final BiPredicate<String, Integer> notSame = Fn.not(same);
        assertTrue(notSame.test("a", 1));
        assertFalse(notSame.test(null, null));
    }

    @Test
    public void c022_valueExtractorPredicatesAcceptSuperTypedFunctions() {
        final java.util.function.Function<Object, Object> idObj = o -> o;
        final java.util.function.Function<Object, String> toStrOrNull = o -> o == null ? null : o.toString();

        final Predicate<String> isNull = Fn.isNull(idObj);
        final Predicate<String> notNull = Fn.notNull(idObj);
        final Predicate<String> isEmpty = Fn.isEmpty(toStrOrNull);
        final Predicate<String> notEmpty = Fn.notEmpty(toStrOrNull);
        final Predicate<String> isBlank = Fn.isBlank(toStrOrNull);
        final Predicate<String> notBlank = Fn.notBlank(toStrOrNull);

        assertTrue(isNull.test(null));
        assertFalse(notNull.test(null));
        assertTrue(isEmpty.test(""));
        assertTrue(notEmpty.test(" "));
        assertTrue(isBlank.test(" "));
        assertTrue(notBlank.test("é"));
    }

    @Test
    public void c022_limitAndFilterAcceptSuperTypedPredicates() {
        final java.util.function.Predicate<Object> nonNull = o -> o != null;
        final java.util.function.BiPredicate<Object, Object> bothNonNull = (a, b) -> a != null && b != null;

        final Predicate<String> ltf = Fn.limitThenFilter(1, nonNull);
        assertTrue(ltf.test("a"));
        assertFalse(ltf.test("b"));

        final Predicate<String> ftl = Fn.filterThenLimit(nonNull, 1);
        assertFalse(ftl.test(null));
        assertTrue(ftl.test("a"));
        assertFalse(ftl.test("b"));

        final BiPredicate<String, Integer> bltf = Fn.limitThenFilter(1, bothNonNull);
        assertTrue(bltf.test("a", 1));
        assertFalse(bltf.test("a", 1));

        final BiPredicate<String, Integer> bftl = Fn.filterThenLimit(bothNonNull, 1);
        assertFalse(bftl.test(null, 1));
        assertTrue(bftl.test("a", 1));
        assertFalse(bftl.test("a", 1));
    }

    @Test
    public void c022_applyIfNotNullChainsAcceptSuperTypedMappers() {
        final java.util.function.Function<Object, List<Integer>> toList = o -> Arrays.asList(o.hashCode());
        final Function<String, Collection<Integer>> orEmpty = Fn.applyIfNotNullOrEmpty(toList);
        assertEquals(Arrays.asList("a".hashCode()), orEmpty.apply("a"));
        assertTrue(orEmpty.apply(null).isEmpty());

        final java.util.function.Function<Object, String> str = Object::toString;
        final java.util.function.Function<CharSequence, Integer> len = CharSequence::length;
        final java.util.function.Function<Number, Integer> twice = n -> n.intValue() * 2;
        final java.util.function.Function<Object, String> tag = o -> "v" + o;

        final Function<Integer, Integer> d2 = Fn.applyIfNotNullOrDefault(str, len, -1);
        final Function<Integer, Integer> d3 = Fn.applyIfNotNullOrDefault(str, len, twice, -1);
        final Function<Integer, String> d4 = Fn.applyIfNotNullOrDefault(str, len, twice, tag, "none");
        assertEquals(3, d2.apply(123));
        assertEquals(-1, d2.apply(null));
        assertEquals(6, d3.apply(123));
        assertEquals("v6", d4.apply(123));
        assertEquals("none", d4.apply(null));

        final Function<Integer, Integer> g2 = Fn.applyIfNotNullOrElseGet(str, len, () -> -1);
        final Function<Integer, Integer> g3 = Fn.applyIfNotNullOrElseGet(str, len, twice, () -> -1);
        final Function<Integer, String> g4 = Fn.applyIfNotNullOrElseGet(str, len, twice, tag, () -> "none");
        assertEquals(3, g2.apply(123));
        assertEquals(-1, g3.apply(null));
        assertEquals("v6", g4.apply(123));
        assertEquals("none", g4.apply(null));
    }

    // END-WIDENING

    // ---------------------------------------------------------------------------------------------------------
    // C-023: limitThenFilter(int, BiPredicate) limits the number of pairs TESTED (regression lock, doc-only fix)
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void c023_limitThenFilterBiPredicate_limitsTestedPairsNotPassingPairs() {
        final AtomicInteger invocations = new AtomicInteger();
        final BiPredicate<String, Integer> p = Fn.limitThenFilter(2, (String s, Integer n) -> {
            invocations.incrementAndGet();
            return n > 0;
        });

        assertTrue(p.test("a", 1));
        assertFalse(p.test("b", -2)); // tested, fails, still consumes a slot
        assertFalse(p.test("c", 3)); // limit exhausted: matching pair rejected without being tested
        assertEquals(2, invocations.get());

        assertFalse(Fn.limitThenFilter(0, (String s, Integer n) -> true).test("x", 1));
        assertThrows(IllegalArgumentException.class, () -> Fn.limitThenFilter(-1, (String s, Integer n) -> true));
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-024: parse* follow Numbers.toX (regression lock, doc-only fix)
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void c024_parseIntegerFamily_numbersToXContract() {
        assertEquals(0, Fn.parseByte().applyAsByte(null));
        assertEquals(0, Fn.parseShort().applyAsShort(""));
        assertEquals(0, Fn.parseInt().applyAsInt(null));
        assertEquals(0L, Fn.parseLong().applyAsLong(""));

        assertEquals(16, Fn.parseByte().applyAsByte("0x10"));
        assertEquals(-16, Fn.parseInt().applyAsInt("-0x10"));
        assertEquals(31, Fn.parseShort().applyAsShort("0X1F"));
        assertEquals(16L, Fn.parseLong().applyAsLong("#10"));
        assertEquals(10, Fn.parseInt().applyAsInt("010")); // not octal
        assertEquals(1, Fn.parseInt().applyAsInt("1L"));
        assertEquals(1L, Fn.parseLong().applyAsLong("1l"));

        assertThrows(NumberFormatException.class, () -> Fn.parseInt().applyAsInt(" 1"));
        assertThrows(NumberFormatException.class, () -> Fn.parseLong().applyAsLong("1 "));
        assertThrows(NumberFormatException.class, () -> Fn.parseInt().applyAsInt("1.5"));
        assertThrows(NumberFormatException.class, () -> Fn.parseInt().applyAsInt("1e3"));
        assertThrows(NumberFormatException.class, () -> Fn.parseInt().applyAsInt("١")); // Arabic-Indic digit one

        assertThrows(ArithmeticException.class, () -> Fn.parseByte().applyAsByte("128"));
        assertThrows(ArithmeticException.class, () -> Fn.parseShort().applyAsShort("32768"));
        assertThrows(ArithmeticException.class, () -> Fn.parseInt().applyAsInt("2147483648"));
        assertThrows(ArithmeticException.class, () -> Fn.parseLong().applyAsLong("9223372036854775808"));
        assertEquals(Byte.MIN_VALUE, Fn.parseByte().applyAsByte("-128"));
        assertEquals(Integer.MAX_VALUE, Fn.parseInt().applyAsInt("2147483647"));
    }

    @Test
    public void c024_parseFloatingFamily_numbersToXContract() {
        assertEquals(0.0f, Fn.parseFloat().applyAsFloat(null));
        assertEquals(0.0d, Fn.parseDouble().applyAsDouble(""));
        assertEquals(1.5f, Fn.parseFloat().applyAsFloat(" 1.5 "));
        assertEquals(1.5d, Fn.parseDouble().applyAsDouble(" 1.5 "));
        assertEquals(1.5d, Fn.parseDouble().applyAsDouble("1.5f"));
        assertEquals(1000.0d, Fn.parseDouble().applyAsDouble("1e3"));
        assertTrue(Double.isNaN(Fn.parseDouble().applyAsDouble("NaN")));
        assertEquals(Float.POSITIVE_INFINITY, Fn.parseFloat().applyAsFloat("1e40")); // no ArithmeticException

        assertThrows(NumberFormatException.class, () -> Fn.parseDouble().applyAsDouble(" "));
        assertThrows(NumberFormatException.class, () -> Fn.parseDouble().applyAsDouble("0x10"));
        assertThrows(NumberFormatException.class, () -> Fn.parseFloat().applyAsFloat("1L"));
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-025: plain throwing adapters peel wrapper exceptions (regression lock, doc-only fix)
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void c025_plainAdaptersUnwrapWrappersButKeepOrdinaryRuntimeExceptions() {
        final UndeclaredThrowableException ute = new UndeclaredThrowableException(new IOException("io"));
        final RuntimeException uteThrown = assertThrows(RuntimeException.class, () -> Fn.ff((String s) -> {
            throw ute;
        }).apply("x"));
        assertInstanceOf(UncheckedIOException.class, uteThrown);

        final IllegalStateException ise = new IllegalStateException("plain");
        assertSame(ise, assertThrows(IllegalStateException.class, () -> Fn.pp((String s) -> {
            throw ise;
        }).test("x")));

        final AssertionError ae = new AssertionError("bare error");
        assertSame(ae, assertThrows(AssertionError.class, () -> Fn.rr(() -> {
            throw ae;
        }).run()));

        final IllegalStateException inner = new IllegalStateException("inner");
        assertSame(inner, assertThrows(IllegalStateException.class, () -> Fn.cc((String s) -> {
            throw new UndeclaredThrowableException(inner);
        }).accept("x")));

        // the defaultOnError overload does not peel
        assertSame(ute, assertThrows(UndeclaredThrowableException.class, () -> Fn.ff((String s) -> {
            throw ute;
        }, "d").apply("x")));
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-026: futureGetOrDefaultOnError no longer hides a task's Error
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void c026_errorCauseIsRethrownLikeFutureGet() {
        final AssertionError ae = new AssertionError("boom");
        final Future<String> failed = CompletableFuture.failedFuture(ae);

        final RuntimeException viaDefault = assertThrows(RuntimeException.class, () -> Fn.futureGetOrDefaultOnError("D").apply(failed));
        final RuntimeException viaGet = assertThrows(RuntimeException.class, () -> Fn.<String> futureGet().apply(failed));
        assertSame(ae, viaDefault.getCause());
        assertEquals(viaGet.getClass(), viaDefault.getClass());
        assertSame(viaGet.getCause(), viaDefault.getCause());
    }

    @Test
    public void c026_stackOverflowAndOutOfMemoryCausesAreRethrown() {
        final FutureTask<String> task = new FutureTask<>(() -> {
            throw new StackOverflowError("so");
        });
        task.run();
        final RuntimeException so = assertThrows(RuntimeException.class, () -> Fn.futureGetOrDefaultOnError("D").apply(task));
        assertInstanceOf(StackOverflowError.class, so.getCause());

        final Future<String> oom = CompletableFuture.failedFuture(new OutOfMemoryError("simulated"));
        final RuntimeException oomThrown = assertThrows(RuntimeException.class, () -> Fn.futureGetOrDefaultOnError("D").apply(oom));
        assertInstanceOf(OutOfMemoryError.class, oomThrown.getCause());
    }

    @Test
    public void c026_exceptionCausesStillYieldDefault() {
        assertEquals("D", Fn.futureGetOrDefaultOnError("D").apply(CompletableFuture.failedFuture(new RuntimeException("rt"))));
        assertEquals("D", Fn.futureGetOrDefaultOnError("D").apply(CompletableFuture.failedFuture(new IOException("checked"))));
        assertEquals("D", Fn.futureGetOrDefaultOnError("D").apply(CompletableFuture.failedFuture(new UndeclaredThrowableException(new AssertionError()))));
        assertNull(Fn.<String> futureGetOrDefaultOnError(null).apply(CompletableFuture.failedFuture(new IllegalStateException())));
        assertEquals("ok", Fn.futureGetOrDefaultOnError("D").apply(CompletableFuture.completedFuture("ok")));
        assertEquals("中", Fn.futureGetOrDefaultOnError("D").apply(CompletableFuture.completedFuture("中")));
    }

    @Test
    public void c026_cancelledAndInterruptedBehaviourUnchanged() {
        final CompletableFuture<String> cancelled = new CompletableFuture<>();
        cancelled.cancel(true);
        assertThrows(CancellationException.class, () -> Fn.futureGetOrDefaultOnError("D").apply(cancelled));

        final Future<String> interrupting = new Future<>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public String get() throws InterruptedException {
                throw new InterruptedException("interrupted");
            }

            @Override
            public String get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                throw new InterruptedException("interrupted");
            }
        };

        try {
            assertEquals("D", Fn.futureGetOrDefaultOnError("D").apply(interrupting));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted(); // clear the flag for the rest of the suite
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-027: F*.unbox() map null to the zero value (regression lock, doc-only fix)
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void c027_unboxMapsNullToZero() {
        assertEquals('\0', FC.unbox().applyAsChar(null));
        assertEquals((byte) 0, FB.unbox().applyAsByte(null));
        assertEquals((short) 0, FS.unbox().applyAsShort(null));
        assertEquals(0, FI.unbox().applyAsInt(null));
        assertEquals(0L, FL.unbox().applyAsLong(null));
        assertEquals(0.0f, FF.unbox().applyAsFloat(null));
        assertEquals(0.0d, FD.unbox().applyAsDouble(null));

        assertEquals('中', FC.unbox().applyAsChar('中'));
        assertEquals(Integer.MIN_VALUE, FI.unbox().applyAsInt(Integer.MIN_VALUE));
        assertEquals(-0.0d, FD.unbox().applyAsDouble(-0.0d));
    }

    // ---------------------------------------------------------------------------------------------------------
    // F1-08: timeLimit(Duration) names the argument the caller passed
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void f108_timeLimitDuration_negativeMessageNamesDuration() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Fn.timeLimit(Duration.ofMillis(-1)));
        assertTrue(e.getMessage().contains("duration"), e.getMessage());
        assertFalse(e.getMessage().contains("timeInMillis"), e.getMessage());

        assertThrows(IllegalArgumentException.class, () -> Fn.timeLimit((Duration) null));
        assertFalse(Fn.timeLimit(Duration.ofMillis(0)).test("x"));
        assertTrue(Fn.timeLimit(Duration.ofMinutes(10)).test("x"));
    }

    // ---------------------------------------------------------------------------------------------------------
    // F1-09 / F2-05 / F2-06 regression locks for the clarified docs
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void f109_applyIfNotNullOrEmpty_onlyNullInputIsReplaced() {
        final Function<String, Collection<Integer>> f = Fn.applyIfNotNullOrEmpty(s -> null);
        assertNull(f.apply("x"));

        final Collection<Integer> empty = f.apply(null);
        assertTrue(empty.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> empty.add(1));
    }

    @Test
    public void f205_compareToAndCompare_nullSortsFirst() {
        assertTrue(Fn.compareTo(5).apply(null) < 0);
        assertTrue(Fn.<Integer> compareTo(null).apply(5) > 0);
        assertEquals(0, Fn.<Integer> compareTo(null).apply(null));
        assertTrue(Fn.<Integer> compare().apply(null, 5) < 0);
        assertEquals(0, Fn.<String> compare().apply(null, null));
    }

    @Test
    public void f206_fiSumChecksOnlyTheFinalTotal() {
        assertEquals(Integer.MAX_VALUE, FI.sum().apply(new int[] { Integer.MAX_VALUE, 1, -1 }));
        assertThrows(ArithmeticException.class, () -> FI.sum().apply(new int[] { Integer.MAX_VALUE, 1 }));
        assertThrows(ArithmeticException.class, () -> FI.sum().apply(new int[] { Integer.MIN_VALUE, -1 }));
        assertEquals(Long.MIN_VALUE, FL.sum().apply(new long[] { Long.MAX_VALUE, 1 }));
        assertEquals(6L, FL.sum().apply(new long[] { 1, 2, 3 }));
        assertEquals(0, FI.sum().apply(null));
    }
}
