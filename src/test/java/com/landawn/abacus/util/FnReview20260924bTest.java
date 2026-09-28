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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.function.IntObjPredicate;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.TriPredicate;

import testfixtures.FnReviewMappedExceptions.MappedCheckedException;
import testfixtures.FnReviewMappedExceptions.MappedRuntimeException;
import testfixtures.FnReviewMappedExceptions.MappedRuntimeSubException;

/**
 * Cycle-2 review fixes 2026-09-24 for {@link Fn} and {@link Fnn}: C-095 (exception-conversion docs: abacus-typed
 * inputs, Errors, registered mappers - doc-only locks), C-096 ({@code ? super} widening of Fnn.not x3,
 * Fn.not(TriPredicate), Fn.indexed(IntObjPredicate), Fnn.c2f x6 / f2c x3), C-097 (Fnn class doc - pp/rr direction) and
 * R6-06 (parseFloat/parseDouble hex floats, numToInt/numToLong narrowing, Fnn.emptyAction example - doc-only locks).
 */
public class FnReview20260924bTest extends TestBase {

    @BeforeAll
    public static void registerMappers() {
        // force = true: the registry is process-wide, so a second run of this class in the same JVM must not fail
        ExceptionUtil.registerRuntimeExceptionMapper(MappedRuntimeException.class, e -> new IllegalStateException("mapped", e), true);
        ExceptionUtil.registerRuntimeExceptionMapper(MappedCheckedException.class, e -> new IllegalArgumentException("mappedChecked", e), true);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // C-095: abacus-typed inputs, Errors and registered mappers
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    public void c095_jc2c_abacusCallableIsReturnedAsIsWithoutConversion() {
        final com.landawn.abacus.util.function.Callable<String> abacus = () -> {
            throw new UndeclaredThrowableException(new IOException("io"));
        };

        assertSame(abacus, Fn.jc2c(abacus));
        // no unwrapping on the fast path ...
        final UndeclaredThrowableException ute = assertThrows(UndeclaredThrowableException.class, () -> Fn.jc2c(abacus).call());
        assertInstanceOf(IOException.class, ute.getCause());
        // ... while jc2r always wraps and converts
        assertThrows(UncheckedIOException.class, () -> Fn.jc2r(abacus).run());

        // the same body typed as a JDK Callable is wrapped and converted by jc2c
        final java.util.concurrent.Callable<String> jdk = () -> {
            throw new UndeclaredThrowableException(new IOException("io"));
        };
        assertThrows(UncheckedIOException.class, () -> Fn.jc2c(jdk).call());
    }

    @Test
    public void c095_jc2c_errorsPropagateUnchangedOnBothPaths() {
        final com.landawn.abacus.util.function.Callable<String> abacus = () -> {
            throw new AssertionError("abacus");
        };
        final java.util.concurrent.Callable<String> jdk = () -> {
            throw new AssertionError("jdk");
        };

        assertEquals("abacus", assertThrows(AssertionError.class, () -> Fn.jc2c(abacus).call()).getMessage());
        assertEquals("jdk", assertThrows(AssertionError.class, () -> Fn.jc2c(jdk).call()).getMessage());
    }

    @Test
    public void c095_r2jr_abacusRunnableIsReturnedAsIsSoErrorsAreNotWrapped() {
        final com.landawn.abacus.util.function.Runnable abacusError = () -> {
            throw new AssertionError("abacus");
        };
        assertSame(abacusError, Fnn.r2jr(abacusError));
        assertEquals("abacus", assertThrows(AssertionError.class, () -> Fnn.r2jr(abacusError).run()).getMessage());

        final com.landawn.abacus.util.function.Runnable abacusUte = () -> {
            throw new UndeclaredThrowableException(new IOException("io"));
        };
        assertThrows(UndeclaredThrowableException.class, () -> Fnn.r2jr(abacusUte).run());

        // the same bodies typed as Throwables.Runnable are wrapped: the Error is wrapped, the wrapper is peeled
        final Throwables.Runnable<RuntimeException> throwablesError = () -> {
            throw new AssertionError("throwables");
        };
        final RuntimeException wrapped = assertThrows(RuntimeException.class, () -> Fnn.r2jr(throwablesError).run());
        assertEquals(RuntimeException.class, wrapped.getClass());
        assertInstanceOf(AssertionError.class, wrapped.getCause());

        final Throwables.Runnable<RuntimeException> throwablesUte = () -> {
            throw new UndeclaredThrowableException(new IOException("io"));
        };
        assertThrows(UncheckedIOException.class, () -> Fnn.r2jr(throwablesUte).run());
    }

    @Test
    public void c095_plainAdaptersWrapAbacusTypedInputsToo() {
        final com.landawn.abacus.util.function.Runnable abacusRunnable = () -> {
            throw new UndeclaredThrowableException(new IOException("io"));
        };
        assertFalse(Fn.rr(abacusRunnable) == abacusRunnable);
        assertThrows(UncheckedIOException.class, () -> Fn.rr(abacusRunnable).run());

        final Predicate<String> abacusPredicate = s -> {
            throw new UndeclaredThrowableException(new IOException("io"));
        };
        assertFalse(Fn.pp(abacusPredicate) == abacusPredicate);
        assertThrows(UncheckedIOException.class, () -> Fn.pp(abacusPredicate).test("x"));
    }

    @Test
    public void c095_directErrorPropagatesFromAdaptersButIsWrappedByUnchecked() {
        final Throwables.Function<String, String, IOException> error = s -> {
            throw new AssertionError("direct");
        };

        assertEquals("direct", assertThrows(AssertionError.class, () -> Fn.ff(error).apply("x")).getMessage());
        assertThrows(AssertionError.class, () -> Fn.ff(error, "default").apply("x"));

        final RuntimeException viaUnchecked = assertThrows(RuntimeException.class, () -> error.unchecked().apply("x"));
        assertEquals(RuntimeException.class, viaUnchecked.getClass());
        assertInstanceOf(AssertionError.class, viaUnchecked.getCause());

        // an Error found by unwrapping a wrapper is wrapped by the plain adapter, untouched by the default overload
        final Throwables.Function<String, String, IOException> wrappedError = s -> {
            throw new UndeclaredThrowableException(new AssertionError("nested"));
        };
        final RuntimeException peeled = assertThrows(RuntimeException.class, () -> Fn.ff(wrappedError).apply("x"));
        assertEquals(RuntimeException.class, peeled.getClass());
        assertInstanceOf(AssertionError.class, peeled.getCause());
        assertThrows(UndeclaredThrowableException.class, () -> Fn.ff(wrappedError, "default").apply("x"));
    }

    @Test
    public void c095_registeredMapperReplacesRuntimeExceptionInPlainAdaptersOnly() {
        final Throwables.Function<String, String, RuntimeException> mappedRte = s -> {
            throw new MappedRuntimeException("m");
        };

        final IllegalStateException mapped = assertThrows(IllegalStateException.class, () -> Fn.ff(mappedRte).apply("x"));
        assertInstanceOf(MappedRuntimeException.class, mapped.getCause());
        assertThrows(IllegalStateException.class, () -> mappedRte.unchecked().apply("x"));

        // a subclass without its own mapper resolves to its superclass's mapper
        final Throwables.Predicate<String, RuntimeException> mappedSub = s -> {
            throw new MappedRuntimeSubException("sub");
        };
        assertInstanceOf(MappedRuntimeSubException.class, assertThrows(IllegalStateException.class, () -> Fn.pp(mappedSub).test("x")).getCause());

        // the defaultOnError overload rethrows the RuntimeException itself
        final MappedRuntimeException same = assertThrows(MappedRuntimeException.class, () -> Fn.ff(mappedRte, "default").apply("x"));
        assertEquals("m", same.getMessage());

        // a mapper for a checked exception replaces the default wrapping
        final Throwables.Function<String, String, MappedCheckedException> mappedChecked = s -> {
            throw new MappedCheckedException("c");
        };
        assertInstanceOf(MappedCheckedException.class, assertThrows(IllegalArgumentException.class, () -> Fn.ff(mappedChecked).apply("x")).getCause());
        assertEquals("default", Fn.ff(mappedChecked, "default").apply("x"));

        // the identity fast paths apply no mapper; the wrapping paths do
        final com.landawn.abacus.util.function.Callable<String> abacusCallable = () -> {
            throw new MappedRuntimeException("fast");
        };
        assertThrows(MappedRuntimeException.class, () -> Fn.jc2c(abacusCallable).call());
        assertThrows(IllegalStateException.class, () -> Fn.jc2r(abacusCallable).run());

        final com.landawn.abacus.util.function.Runnable abacusRunnable = () -> {
            throw new MappedRuntimeException("fast");
        };
        assertThrows(MappedRuntimeException.class, () -> Fnn.r2jr(abacusRunnable).run());
        final Throwables.Runnable<RuntimeException> throwablesRunnable = () -> {
            throw new MappedRuntimeException("slow");
        };
        assertThrows(IllegalStateException.class, () -> Fnn.r2jr(throwablesRunnable).run());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // C-096: ? super widening
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    public void c096_fnnNot_acceptsSuperTypedPredicates() throws IOException {
        final Throwables.Predicate<Object, IOException> isNull = o -> o == null;
        final Throwables.Predicate<String, IOException> notNull = Fnn.not(isNull);
        assertTrue(notNull.test("a"));
        assertFalse(notNull.test(null));

        final Throwables.BiPredicate<Object, Object, IOException> same = (a, b) -> a == b;
        final Throwables.BiPredicate<String, Integer, IOException> notSame = Fnn.not(same);
        assertTrue(notSame.test("a", 1));
        assertFalse(notSame.test(null, null));

        final Throwables.TriPredicate<Object, Object, Object, IOException> allNull = (a, b, c) -> a == null && b == null && c == null;
        final Throwables.TriPredicate<String, Integer, Long, IOException> notAllNull = Fnn.not(allNull);
        assertTrue(notAllNull.test("a", null, null));
        assertFalse(notAllNull.test(null, null, null));

        // exceptions of the delegate still propagate unchanged
        final Throwables.Predicate<Object, IOException> failing = o -> {
            throw new IOException("boom");
        };
        final Throwables.Predicate<String, IOException> negatedFailing = Fnn.not(failing);
        assertEquals("boom", assertThrows(IOException.class, () -> negatedFailing.test("x")).getMessage());

        // inference with implicit lambdas and in a Seq pipeline is unchanged
        assertEquals(Arrays.asList("a", "中"), Seq.<String, IOException> of("a", "", "中").filter(Fnn.not(String::isEmpty)).toList());
        assertEquals(Arrays.asList("b"), Seq.<String, IOException> of("a", "b").filter(Fnn.not(s -> s.equals("a"))).toList());
    }

    @Test
    public void c096_fnnNot_nullArgument() {
        assertThrows(IllegalArgumentException.class, () -> Fnn.not((Throwables.Predicate<Object, IOException>) null));
        assertThrows(IllegalArgumentException.class, () -> Fnn.not((Throwables.BiPredicate<Object, Object, IOException>) null));
        assertThrows(IllegalArgumentException.class, () -> Fnn.not((Throwables.TriPredicate<Object, Object, Object, IOException>) null));
    }

    @Test
    public void c096_fnNotTriPredicate_acceptsSuperTypedPredicate() {
        final TriPredicate<Object, Object, Object> anyNull = (a, b, c) -> a == null || b == null || c == null;
        final TriPredicate<String, Integer, Long> noneNull = Fn.not(anyNull);
        assertTrue(noneNull.test("a", 1, 2L));
        assertFalse(noneNull.test("a", null, 2L));

        final TriPredicate<CharSequence, CharSequence, CharSequence> concatEquals = (a, b, c) -> (a.toString() + b).contentEquals(c);
        final TriPredicate<String, String, String> notConcatEquals = Fn.not(concatEquals);
        assertFalse(notConcatEquals.test("中", "文", "中文"));
        assertTrue(notConcatEquals.test("", "", "x"));

        assertThrows(IllegalArgumentException.class, () -> Fn.not((TriPredicate<Object, Object, Object>) null));
    }

    @Test
    public void c096_fnIndexed_acceptsSuperTypedPredicate() {
        final IntObjPredicate<Object> evenIndexNonNull = (i, o) -> i % 2 == 0 && o != null;
        final Predicate<String> predicate = Fn.indexed(evenIndexNonNull);

        final List<Boolean> results = new ArrayList<>();
        for (final String s : Arrays.asList("a", "b", null, "中", "")) {
            results.add(predicate.test(s));
        }
        assertEquals(Arrays.asList(true, false, false, false, true), results);

        // implicit lambda still infers
        final Predicate<String> firstTwo = Fn.indexed((i, s) -> i < 2);
        assertTrue(firstTwo.test("x"));
        assertTrue(firstTwo.test("y"));
        assertFalse(firstTwo.test("z"));

        assertThrows(IllegalArgumentException.class, () -> Fn.indexed((IntObjPredicate<Object>) null));
    }

    @Test
    public void c096_fnnC2f_acceptsSuperTypedConsumers() throws IOException {
        final List<Object> seen = new ArrayList<>();

        final Throwables.Consumer<Object, IOException> consumer = seen::add;
        final Throwables.Function<String, Void, IOException> f1 = Fnn.c2f(consumer);
        assertNull(f1.apply("a"));
        final Throwables.Function<String, Integer, IOException> f2 = Fnn.c2f(consumer, 7);
        assertEquals(7, f2.apply("b"));

        final Throwables.BiConsumer<Object, Object, IOException> biConsumer = (a, b) -> seen.add(a + ":" + b);
        final Throwables.BiFunction<String, Integer, Void, IOException> bf1 = Fnn.c2f(biConsumer);
        assertNull(bf1.apply("c", 1));
        final Throwables.BiFunction<String, Integer, String, IOException> bf2 = Fnn.c2f(biConsumer, "r");
        assertEquals("r", bf2.apply("d", 2));

        final Throwables.TriConsumer<Object, Object, Object, IOException> triConsumer = (a, b, c) -> seen.add(a + ":" + b + ":" + c);
        final Throwables.TriFunction<String, Integer, Long, Void, IOException> tf1 = Fnn.c2f(triConsumer);
        assertNull(tf1.apply("e", 3, 4L));
        final Throwables.TriFunction<String, Integer, Long, Boolean, IOException> tf2 = Fnn.c2f(triConsumer, Boolean.TRUE);
        assertEquals(Boolean.TRUE, tf2.apply("中", 5, 6L));

        assertEquals(Arrays.asList("a", "b", "c:1", "d:2", "e:3:4", "中:5:6"), seen);

        // the delegate's checked exception propagates unchanged
        final Throwables.Consumer<Object, IOException> failing = o -> {
            throw new IOException("boom");
        };
        final Throwables.Function<String, Void, IOException> failingFunc = Fnn.c2f(failing);
        assertEquals("boom", assertThrows(IOException.class, () -> failingFunc.apply("x")).getMessage());

        assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.Consumer<Object, IOException>) null));
        assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.Consumer<Object, IOException>) null, 1));
        assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.BiConsumer<Object, Object, IOException>) null));
        assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.BiConsumer<Object, Object, IOException>) null, 1));
        assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.TriConsumer<Object, Object, Object, IOException>) null));
        assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.TriConsumer<Object, Object, Object, IOException>) null, 1));
    }

    @Test
    public void c096_fnnF2c_acceptsSuperTypedFunctions() throws IOException {
        final List<Object> seen = new ArrayList<>();

        final Throwables.Function<Object, Boolean, IOException> function = seen::add;
        final Throwables.Consumer<String, IOException> c1 = Fnn.f2c(function);
        c1.accept("a");

        final Throwables.BiFunction<Object, Object, Boolean, IOException> biFunction = (a, b) -> seen.add(a + ":" + b);
        final Throwables.BiConsumer<String, Integer, IOException> c2 = Fnn.f2c(biFunction);
        c2.accept("b", 1);

        final Throwables.TriFunction<Object, Object, Object, Boolean, IOException> triFunction = (a, b, c) -> seen.add(a + ":" + b + ":" + c);
        final Throwables.TriConsumer<String, Integer, Long, IOException> c3 = Fnn.f2c(triFunction);
        c3.accept("中", 2, 3L);

        assertEquals(Arrays.asList("a", "b:1", "中:2:3"), seen);

        assertThrows(IllegalArgumentException.class, () -> Fnn.f2c((Throwables.Function<Object, Object, IOException>) null));
        assertThrows(IllegalArgumentException.class, () -> Fnn.f2c((Throwables.BiFunction<Object, Object, Object, IOException>) null));
        assertThrows(IllegalArgumentException.class, () -> Fnn.f2c((Throwables.TriFunction<Object, Object, Object, Object, IOException>) null));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // C-097: Fnn.pp/rr go from an unchecked abacus interface to a Throwables one (doc-only lock)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    public void c097_fnnPpAndRrWidenAnAbacusInterfaceWithoutConversion() throws IOException {
        final Predicate<String> abacus = String::isEmpty;
        final Throwables.Predicate<String, IOException> widened = Fnn.pp(abacus);
        assertSame(abacus, widened);
        assertTrue(widened.test(""));

        final com.landawn.abacus.util.function.Runnable failing = () -> {
            throw new UndeclaredThrowableException(new IOException("io"));
        };
        final Throwables.Runnable<IOException> widenedRunnable = Fnn.rr(failing);
        assertSame(failing, widenedRunnable);
        assertThrows(UndeclaredThrowableException.class, widenedRunnable::run);

        // Fn.pp goes the other way: Throwables in, converting abacus Predicate out
        final Throwables.Predicate<String, IOException> checked = s -> {
            throw new IOException("io");
        };
        assertThrows(UncheckedIOException.class, () -> Fn.pp(checked).test("x"));
        // and a checked lambda is written with Fnn.p, not Fnn.pp
        final Throwables.Predicate<String, IOException> viaP = Fnn.p(checked);
        assertSame(checked, viaP);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // R6-06: parse/numTo docs and Fnn.emptyAction example (doc-only locks)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    public void r606_parseFloatAndDoubleAcceptHexFloatingLiteralsOnly() {
        assertEquals(8.0, Fn.parseDouble().applyAsDouble("0x1p3"));
        assertEquals(-8.0, Fn.parseDouble().applyAsDouble("-0x1p3"));
        assertEquals(8.0f, Fn.parseFloat().applyAsFloat("0x1p3"));
        assertEquals(3.0f, Fn.parseFloat().applyAsFloat("0X1.8P1"));

        assertThrows(NumberFormatException.class, () -> Fn.parseDouble().applyAsDouble("0x10"));
        assertThrows(NumberFormatException.class, () -> Fn.parseFloat().applyAsFloat("0x10"));
        assertThrows(NumberFormatException.class, () -> Fn.parseDouble().applyAsDouble("#10"));
        assertThrows(NumberFormatException.class, () -> Fn.parseFloat().applyAsFloat("#10"));
        assertThrows(NumberFormatException.class, () -> Fn.parseDouble().applyAsDouble("1L"));
    }

    @Test
    public void r606_numToIntAndNumToLongNarrowSilently() {
        assertEquals(-1294967296, Fn.numToInt().applyAsInt(3_000_000_000L));
        assertEquals(Integer.MAX_VALUE, Fn.numToInt().applyAsInt(1e10));
        assertEquals(Integer.MIN_VALUE, Fn.numToInt().applyAsInt(-1e10));
        assertEquals(0, Fn.numToInt().applyAsInt(Double.NaN));
        assertEquals(10, Fn.numToInt().applyAsInt(10.5));
        assertEquals(-10, Fn.numToInt().applyAsInt(-10.5));
        assertEquals(0, Fn.numToInt().applyAsInt(null));

        assertEquals(Long.MAX_VALUE, Fn.numToLong().applyAsLong(1e30));
        assertEquals(0L, Fn.numToLong().applyAsLong(Double.NaN));
        assertEquals(5L, Fn.numToLong().applyAsLong(BigInteger.ONE.shiftLeft(64).add(BigInteger.valueOf(5))));
        assertEquals(0L, Fn.numToLong().applyAsLong(null));

        // contrast: the parse functions reject an out-of-range value
        assertThrows(ArithmeticException.class, () -> Fn.parseInt().applyAsInt("3000000000"));
        assertThrows(ArithmeticException.class, () -> Fn.parseLong().applyAsLong("99999999999999999999"));
    }

    @Test
    public void r606_emptyActionJavadocExampleCompilesAndRuns() throws IOException {
        final List<String> log = new ArrayList<>();
        final Throwables.Runnable<IOException> actualCallback = () -> log.add("called");

        for (final boolean needsCallback : new boolean[] { false, true }) {
            final Throwables.Runnable<IOException> callback = needsCallback ? Fnn.r(actualCallback) : Fnn.emptyAction();
            callback.run();
        }

        final Throwables.Runnable<IOException> onClose = Fnn.emptyAction();
        onClose.run();

        assertEquals(Arrays.asList("called"), log);
    }
}
