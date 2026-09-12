package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Runnable;

public class FnTest extends FnTestSupport {

    @Test
    public void testUtilityConstructorIsPrivate() throws NoSuchMethodException {
        assertTrue(Modifier.isPrivate(Fn.class.getDeclaredConstructor().getModifiers()));
    }

    @Test
    public void testClose() throws Exception {
        final MyCloseable c = new MyCloseable();
        final Runnable closer = Fn.close(c);
        closer.run();
        closer.run();
        assertEquals(1, c.getCloseCount());
        Fn.<MyCloseable> close().accept(c);
        assertEquals(2, c.getCloseCount());

        final AutoCloseable throwing = () -> {
            throw new Exception("boom");
        };
        assertThrows(RuntimeException.class, () -> Fn.close(throwing).run());
        assertDoesNotThrow(() -> Fn.closeQuietly(throwing).run());
    }

    @Test
    public void testCloseAll() {
        final MyCloseable c1 = new MyCloseable();
        final MyCloseable c2 = new MyCloseable();
        final Runnable closer = Fn.closeAll(c1, c2);
        closer.run();
        closer.run();
        assertEquals(1, c1.getCloseCount());
        assertEquals(1, c2.getCloseCount());

        final MyCloseable c3 = new MyCloseable();
        Fn.closeAll(List.of(c3)).run();
        assertEquals(1, c3.getCloseCount());

        final MyCloseable q1 = new MyCloseable();
        final MyCloseable q2 = new MyCloseable();
        final Runnable quietly = Fn.closeAllQuietly(q1, q2);
        quietly.run();
        quietly.run();
        assertEquals(1, q1.getCloseCount());
        final MyCloseable q3 = new MyCloseable();
        Fn.closeAllQuietly(List.of(q3)).run();
        assertEquals(1, q3.getCloseCount());
    }

    @Test
    public void testClose_WaitsForInFlightClose() throws Exception {
        final AtomicBoolean closed = new AtomicBoolean();
        final CountDownLatch inClose = new CountDownLatch(1);
        final AutoCloseable slow = () -> {
            inClose.countDown();
            Thread.sleep(200);
            closed.set(true);
        };
        final Runnable closer = Fn.close(slow);
        final Thread first = new Thread(closer::run);
        first.start();
        assertTrue(inClose.await(5, TimeUnit.SECONDS));
        closer.run();
        assertTrue(closed.get());
        first.join(5_000);
    }

    @Test
    public void testEmptyActionAndDoNothing() {
        assertDoesNotThrow(() -> Fn.emptyAction().run());
        assertDoesNotThrow(() -> Fn.doNothing().accept("x"));
        assertDoesNotThrow(() -> Fn.emptyConsumer().accept("x"));
        assertSame(Fn.doNothing(), Fn.emptyConsumer());
    }

    @Test
    public void testShutdown() throws InterruptedException {
        final ExecutorService es = Executors.newSingleThreadExecutor();
        try {
            Fn.shutdown(es).run();
            assertTrue(es.isShutdown());
            Fn.shutdown(es).run();
        } finally {
            es.shutdownNow();
        }
        final ExecutorService es2 = Executors.newSingleThreadExecutor();
        try {
            Fn.shutdown(es2, 100, TimeUnit.MILLISECONDS).run();
            assertTrue(es2.isShutdown());
        } finally {
            es2.shutdownNow();
        }
        assertThrows(IllegalArgumentException.class, () -> Fn.shutdown(null));
        final ExecutorService unused = Executors.newSingleThreadExecutor();
        try {
            assertThrows(IllegalArgumentException.class, () -> Fn.shutdown(unused, -1, TimeUnit.MILLISECONDS));
            // the rejected argument must be reported under its own name, not 'unit'
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Fn.shutdown(unused, 1, null));
            assertTrue(ex.getMessage().contains("timeUnit"), "expected the message to name 'timeUnit' but got: " + ex.getMessage());
        } finally {
            unused.shutdownNow();
        }
    }

    @Test
    public void testThrowAndToRuntimeException() {
        assertThrows(RuntimeException.class, () -> Fn.throwRuntimeException("boom").accept("x"));
        assertThrows(IllegalStateException.class, () -> Fn.throwException(IllegalStateException::new).accept("x"));
        final RuntimeException wrapped = Fn.toRuntimeException().apply(new Exception("e"));
        assertTrue(wrapped.getCause() instanceof Exception || wrapped.getMessage().contains("e") || wrapped instanceof RuntimeException);
        assertThrows(IllegalArgumentException.class, () -> Fn.throwException(null));

        final IllegalStateException original = new IllegalStateException("original");
        assertSame(original, Fn.toRuntimeException().apply(original));
        assertSame(original, Fn.toRuntimeException().apply(new java.lang.reflect.UndeclaredThrowableException(original)));
        final InterruptedException interruption = new InterruptedException("interrupted");
        try {
            Thread.interrupted();
            assertSame(interruption, Fn.toRuntimeException().apply(new java.util.concurrent.ExecutionException(interruption)).getCause());
            assertFalse(Thread.currentThread().isInterrupted());
            assertSame(interruption, Fn.toRuntimeException().apply(interruption).getCause());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void testSleepAndRateLimiter() {
        final long start = System.currentTimeMillis();
        Fn.sleep(20).accept("x");
        assertTrue(System.currentTimeMillis() - start >= 15);
        Fn.sleepUninterruptibly(10).accept("x");
        assertDoesNotThrow(() -> Fn.rateLimiter(1000).accept("x"));
        assertThrows(IllegalArgumentException.class, () -> Fn.rateLimiter((RateLimiter) null));
        assertDoesNotThrow(() -> Fn.sleep(-1).accept("x"));
    }

    @Test
    public void testPrintln() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PrintStream original = System.out;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        try {
            Fn.println().accept("hello");
            Fn.println("=").accept("key", "value");
            Fn.println(" -> ").accept(new int[] { 1, 2 }, "x");
        } finally {
            System.setOut(original);
        }
        final String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("hello"));
        assertTrue(printed.contains("key=value"));
        assertTrue(printed.contains("[1, 2] -> x"));
        assertThrows(IllegalArgumentException.class, () -> Fn.println(null));
        assertDoesNotThrow(() -> Fn.println(":").accept("a", "b"));
        assertDoesNotThrow(() -> Fn.println(", ").accept("a", "b"));
        assertDoesNotThrow(() -> Fn.println("").accept("a", "b"));
    }

    @Test
    public void testToStringCaseAndFormat() {
        assertEquals("123", Fn.toStr().apply(123));
        assertEquals("null", Fn.toStr().apply(null));
        assertEquals("foo", Fn.toLowerCase().apply("FOO"));
        assertEquals("FOO", Fn.toUpperCase().apply("foo"));
        assertEquals("helloWorld", Fn.toCamelCase().apply("hello_world"));
        assertEquals("hello_world", Fn.toSnakeCase().apply("helloWorld"));
        assertEquals("HELLO_WORLD", Fn.toScreamingSnakeCase().apply("helloWorld"));
        assertTrue(Fn.toJson().apply(Map.of("a", 1)).contains("a"));
        assertTrue(Fn.toXml().apply("x").length() > 0);
    }

    @Test
    public void testIdentityKeyedWrap() {
        assertEquals("x", Fn.identity().apply("x"));
        assertNull(Fn.identity().apply(null));
        final Keyed<Integer, String> keyed = Fn.<Integer, String> keyed(String::length).apply("abc");
        assertEquals(3, keyed.key());
        assertEquals("abc", Fn.<Integer, String> val().apply(keyed));
        final Map.Entry<Keyed<Integer, String>, String> entry = CommonUtil.newImmutableEntry(keyed, "v");
        assertEquals("abc", Fn.<Integer, String, String> kkv().apply(entry));

        final Wrapper<String> wrapped = Fn.<String> wrap().apply("hi");
        assertEquals("hi", Fn.<String> unwrap().apply(wrapped));
        final Wrapper<String> custom = Fn.wrap(String::length, (a, b) -> a.equals(b)).apply("ab");
        assertEquals("ab", custom.value());
        final java.util.function.ToIntFunction<String> hashFunction = String::length;
        final java.util.function.BiPredicate<String, String> equalsFunction = String::equals;
        final Wrapper<String> fromFirstFactory = Fn.wrap(hashFunction, equalsFunction).apply("ab");
        final Wrapper<String> fromSecondFactory = Fn.wrap(hashFunction, equalsFunction).apply(new String("ab"));
        assertEquals(fromFirstFactory, fromSecondFactory);
        assertEquals(fromFirstFactory.hashCode(), fromSecondFactory.hashCode());
        assertFalse(fromFirstFactory.equals(Fn.wrap(hashFunction, equalsFunction.negate().negate()).apply("ab")));
        assertThrows(IllegalArgumentException.class, () -> Fn.keyed(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.wrap(null, (a, b) -> true));
    }

    @Test
    public void testKeyValueLeftRight() {
        final Map.Entry<String, Integer> e = CommonUtil.newImmutableEntry("k", 1);
        assertEquals("k", Fn.<String, Integer> key().apply(e));
        assertEquals(1, Fn.<String, Integer> value().apply(e));
        final Pair<String, Integer> p = Pair.of("L", 2);
        assertEquals("L", Fn.<String, Integer> left().apply(p));
        assertEquals(2, Fn.<String, Integer> right().apply(p));
        assertEquals("k", Fn.<String, Integer> invert().apply(e).getValue());
        assertEquals(1, Fn.<String, Integer> invert().apply(e).getKey());
    }

    @Test
    public void testEntryPairTripleTuple() {
        assertEquals("k", Fn.<String, Integer> entry().apply("k", 1).getKey());
        assertEquals("k", Fn.<String, Integer> entry("k").apply(1).getKey());
        assertEquals("ab", Fn.<String, String> entry(s -> s.substring(0, 2)).apply("abc").getKey());
        assertEquals("fixed", Fn.<String, Integer> entryWithKey("fixed").apply(1).getKey());
        assertEquals("ab", Fn.<String, String> entryByKeyMapper(s -> s.substring(0, 2)).apply("abc").getKey());
        assertEquals(9, Fn.<String, Integer> entryWithValue(9).apply("k").getValue());
        assertEquals(3, Fn.<String, Integer> entryByValueMapper(String::length).apply("abc").getValue());
        assertEquals("L", Fn.<String, Integer> pair().apply("L", 1).left());
        assertEquals("M", Fn.<String, String, Integer> triple().apply("L", "M", 1).middle());
        assertEquals("a", Fn.<String> tuple1().apply("a")._1);
        assertEquals("b", Fn.<String, String> tuple2().apply("a", "b")._2);
        assertEquals("c", Fn.<String, String, String> tuple3().apply("a", "b", "c")._3);
        assertEquals("d", Fn.<String, String, String, String> tuple4().apply("a", "b", "c", "d")._4);
        assertThrows(IllegalArgumentException.class, () -> Fn.entryByKeyMapper(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.entryByValueMapper(null));
    }

    @Test
    public void testStringOperators() {
        assertEquals("x", Fn.trim().apply(" x "));
        assertEquals("", Fn.trimToEmpty().apply(null));
        assertNull(Fn.trimToNull().apply("  "));
        assertEquals("x", Fn.strip().apply(" x "));
        assertEquals("", Fn.stripToEmpty().apply(null));
        assertNull(Fn.stripToNull().apply("  "));
        assertEquals("", Fn.nullToEmpty().apply(null));
        assertEquals("x", Fn.nullToEmpty().apply("x"));
        assertEquals(List.of(), Fn.nullToEmptyList().apply(null));
        assertEquals(Set.of(), Fn.nullToEmptySet().apply(null));
        assertEquals(Map.of(), Fn.nullToEmptyMap().apply(null));
        final List<String> nonempty = List.of("a");
        assertSame(nonempty, Fn.<String> nullToEmptyList().apply(nonempty));
    }

    @Test
    public void testLenSizeCast() {
        assertEquals(2, Fn.len().apply(new String[] { "a", "b" }));
        assertEquals(0, Fn.len().apply(null));
        assertEquals(3, Fn.length().apply("abc"));
        assertEquals(0, Fn.length().apply(null));
        assertEquals(2, Fn.size().apply(List.of("a", "b")));
        assertEquals(0, Fn.size().apply(null));
        assertEquals(1, Fn.mapSize().apply(Map.of("a", 1)));
        assertEquals(0, Fn.mapSize().apply(null));
        assertEquals(1, Fn.cast(Integer.class).apply(1));
        assertThrows(ClassCastException.class, () -> Fn.cast(Integer.class).apply("x"));
        assertThrows(IllegalArgumentException.class, () -> Fn.cast(null));
    }

    @Test
    public void testAlwaysAndEqual() {
        assertTrue(Fn.alwaysTrue().test(null));
        assertFalse(Fn.alwaysFalse().test("x"));
        assertTrue(Fn.equal("a").test("a"));
        assertFalse(Fn.equal("a").test("b"));
        assertTrue(Fn.eqOr("a", "b").test("b"));
        assertFalse(Fn.eqOr("a", "b").test("c"));
        assertTrue(Fn.eqOr("a", "b", "c").test("c"));
        assertTrue(Fn.<String, String> equal().test("a", "a"));
        assertFalse(Fn.<String, String> equal().test("a", "b"));
    }

    @Test
    public void testComparisonPredicates() {
        assertTrue(Fn.greaterThan(1).test(2));
        assertFalse(Fn.greaterThan(1).test(1));
        assertTrue(Fn.greaterThanOrEqual(1).test(1));
        assertTrue(Fn.lessThan(2).test(1));
        assertTrue(Fn.lessThanOrEqual(1).test(1));
        assertTrue(Fn.<Integer> greaterThan().test(2, 1));
        assertTrue(Fn.<Integer> lessThanOrEqual().test(1, 1));
        assertTrue(Fn.gtAndLt(1, 5).test(3));
        assertFalse(Fn.gtAndLt(1, 5).test(1));
        assertTrue(Fn.geAndLt(1, 5).test(1));
        assertTrue(Fn.geAndLe(1, 5).test(5));
        assertTrue(Fn.gtAndLe(1, 5).test(5));
        assertTrue(Fn.between(1, 5).test(3));
        assertFalse(Fn.between(1, 5).test(1));
        assertFalse(Fn.between(1, 5).test(5));
    }

    @Test
    public void testInInstanceOfAndStrings() {
        assertTrue(Fn.in(List.of("a", "b")).test("a"));
        assertFalse(Fn.in(List.of("a")).test("c"));
        assertThrows(IllegalArgumentException.class, () -> Fn.in(null));
        assertTrue(Fn.instanceOf(Number.class).test(1));
        assertFalse(Fn.instanceOf(Number.class).test("x"));
        assertTrue(Fn.subtypeOf(Number.class).test(Integer.class));
        assertFalse(Fn.subtypeOf(Number.class).test(String.class));
        assertTrue(Fn.startsWith("ab").test("abc"));
        assertTrue(Fn.endsWith("bc").test("abc"));
        assertTrue(Fn.contains("b").test("abc"));
        assertTrue(Fn.matches(Pattern.compile("\\d+")).test("12"));
        assertFalse(Fn.matches(Pattern.compile("\\d+")).test("ab"));
        assertThrows(IllegalArgumentException.class, () -> Fn.startsWith(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.matches(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.instanceOf(null));
    }

    // R07 (2026-09-08): the r9506 memoize javadoc said a swallowed recursion means "the enclosing call fails and
    // nothing is cached". Poisoning is per input, so in a two-input cycle the OTHER input is computed and cached
    // as usual; only the re-entered input fails. FnMemoizeTest covers same-key poisoning and distinct-key
    // recursion, not the cycle back to the outer key. This pins what the corrected javadoc now promises.
    @Test
    public void testMemoizePoisonsOnlyTheReEnteredInputInATwoInputCycle() {
        final List<String> computed = new ArrayList<>();
        @SuppressWarnings("unchecked")
        final com.landawn.abacus.util.function.Function<String, String>[] holder = new com.landawn.abacus.util.function.Function[1];

        holder[0] = Fn.memoize(key -> {
            computed.add(key);

            if ("A".equals(key)) {
                holder[0].apply("B");
            } else if ("B".equals(key)) {
                try {
                    holder[0].apply("A"); // cycles back to the outer input, which is poisoned
                } catch (final IllegalStateException ignored) {
                    // deliberately swallowed: A must still fail, B must not
                }
            }

            return "v-" + key;
        });

        assertEquals("Recursive computation of memoized value", assertThrows(IllegalStateException.class, () -> holder[0].apply("A")).getMessage());
        assertEquals(List.of("A", "B"), computed);

        // B was published even though the computation that asked for it failed ...
        assertEquals("v-B", holder[0].apply("B"));
        assertEquals(List.of("A", "B"), computed);

        // ... and A cached nothing, so it is recomputed - but this time the cached B answers without re-entering
        // A, so no recursion happens and the very same call that just threw now succeeds. A caller that retries
        // after the IllegalStateException therefore gets a value, not a second failure.
        assertEquals("v-A", holder[0].apply("A"));
        assertEquals(List.of("A", "B", "A"), computed);
        assertEquals("v-A", holder[0].apply("A"));
        assertEquals(List.of("A", "B", "A"), computed);
    }

    @Test
    public void testInAndNotInEvaluateContainmentLive() {
        final List<String> live = new ArrayList<>();
        final Predicate<String> in = Fn.in(live);
        final Predicate<String> notIn = Fn.notIn(live);

        // built on an empty collection: emptiness must not be frozen at construction time
        assertFalse(in.test("a"));
        assertTrue(notIn.test("a"));

        live.add("a");
        assertTrue(in.test("a"));
        assertFalse(notIn.test("a"));

        live.remove("a");
        assertFalse(in.test("a"));
        assertTrue(notIn.test("a"));

        // a null-hostile collection propagates its own NullPointerException, empty or not
        assertThrows(NullPointerException.class, () -> Fn.in(List.of()).test(null));
        assertThrows(NullPointerException.class, () -> Fn.notIn(List.of()).test(null));
        assertThrows(NullPointerException.class, () -> Fn.in(List.of("a")).test(null));
        assertThrows(NullPointerException.class, () -> Fn.notIn(List.of("a")).test(null));
    }

    @Test
    public void testAndOr() {
        assertTrue(Fn.and(() -> true, () -> true).getAsBoolean());
        assertFalse(Fn.and(() -> true, () -> false).getAsBoolean());
        assertTrue(Fn.and(() -> true, () -> true, () -> true).getAsBoolean());
        assertTrue(Fn.and((Predicate<String>) s -> s.startsWith("a"), (Predicate<String>) s -> s.length() > 1).test("ab"));
        assertTrue(Fn.and(List.of((Predicate<String>) s -> true, (Predicate<String>) s -> s.length() > 0)).test("x"));
        assertTrue(Fn.or(() -> false, () -> true).getAsBoolean());
        assertTrue(Fn.or((Predicate<String>) s -> s.startsWith("z"), (Predicate<String>) s -> s.length() == 1).test("x"));
        final BiPredicate<Integer, Integer> andBi = Fn.and((Integer a, Integer b) -> a > 0, (Integer a, Integer b) -> b > 0);
        assertTrue(andBi.test(1, 2));
        assertFalse(andBi.test(1, -1));
        assertThrows(IllegalArgumentException.class, () -> Fn.and((BooleanSupplier) null, () -> true));
        assertThrows(IllegalArgumentException.class, () -> Fn.and(Collections.<Predicate<String>> emptyList()));

        final List<java.util.function.Predicate<String>> predicates = new ArrayList<>();
        predicates.add(value -> value.startsWith("a"));
        predicates.add(value -> { throw new AssertionError("short-circuited predicate"); });
        final Predicate<String> snapshotAnd = Fn.and(predicates);
        final Predicate<String> snapshotOr = Fn.or(predicates);
        predicates.clear();
        predicates.add(value -> { throw new AssertionError("later-added predicate"); });
        assertFalse(snapshotAnd.test("b"));
        assertTrue(snapshotOr.test("a"));

        final List<java.util.function.BiPredicate<String, String>> biPredicates = new ArrayList<>();
        biPredicates.add(String::equals);
        biPredicates.add((left, right) -> { throw new AssertionError("short-circuited bi-predicate"); });
        final BiPredicate<String, String> snapshotBiAnd = Fn.and(biPredicates);
        final BiPredicate<String, String> snapshotBiOr = Fn.or(biPredicates);
        biPredicates.clear();
        assertFalse(snapshotBiAnd.test("a", "b"));
        assertTrue(snapshotBiOr.test("a", "a"));
    }

    @Test
    public void testParseAndNumbers() {
        assertEquals((byte) 1, Fn.parseByte().applyAsByte("1"));
        assertEquals((short) 2, Fn.parseShort().applyAsShort("2"));
        assertEquals(3, Fn.parseInt().applyAsInt("3"));
        assertEquals(4L, Fn.parseLong().applyAsLong("4"));
        assertEquals(1.5f, Fn.parseFloat().applyAsFloat("1.5"));
        assertEquals(2.5, Fn.parseDouble().applyAsDouble("2.5"));
        assertEquals(10, Fn.createNumber().apply("10").intValue());
        assertEquals(7, Fn.numToInt().applyAsInt(7L));
        assertEquals(7L, Fn.numToLong().applyAsLong(7));
        assertEquals(7.0, Fn.numToDouble().applyAsDouble(7));
        assertFalse(Fn.FF.notNegative().test(Float.NaN));
        assertTrue(Fn.FF.notNegative().test(-0.0f));
        assertTrue(Fn.FF.notNegative().test(0.0f));
        assertFalse(Fn.FD.notNegative().test(Double.NaN));
        assertTrue(Fn.FD.notNegative().test(-0.0d));
        assertTrue(Fn.FD.notNegative().test(0.0d));
    }

    @Test
    public void testOptionalHelpers() {
        assertEquals(true, Fn.GET_AS_BOOLEAN.applyAsBoolean(OptionalBoolean.of(true)));
        assertEquals(3, Fn.GET_AS_INT.applyAsInt(OptionalInt.of(3)));
        assertEquals(3, Fn.GET_AS_INT_JDK.applyAsInt(java.util.OptionalInt.of(3)));
        assertEquals("x", Fn.<String> getIfPresentOrElseNull().apply(com.landawn.abacus.util.u.Optional.of("x")));
        assertNull(Fn.<String> getIfPresentOrElseNull().apply(com.landawn.abacus.util.u.Optional.empty()));
        assertEquals("x", Fn.<String> getIfPresentOrElseNullJdk().apply(java.util.Optional.of("x")));
        assertNull(Fn.<String> getIfPresentOrElseNullJdk().apply(java.util.Optional.empty()));
    }

    @Test
    public void testFutureGet() {
        assertEquals("ok", Fn.futureGet().apply(CompletableFuture.completedFuture("ok")));
        assertEquals("d", Fn.futureGetOrDefaultOnError("d").apply(CompletableFuture.failedFuture(new RuntimeException("e"))));
        final AssertionError taskError = new AssertionError("task error");
        final RuntimeException wrappedError = assertThrows(RuntimeException.class,
                () -> Fn.futureGet().apply(CompletableFuture.failedFuture(taskError)));
        assertSame(taskError, wrappedError.getCause());
        final IllegalStateException taskFailure = new IllegalStateException("task failure");
        assertSame(taskFailure, assertThrows(IllegalStateException.class,
                () -> Fn.futureGet().apply(CompletableFuture.failedFuture(taskFailure))));
    }

    // FINDING G20-006: the eight close/shutdown runnables used to synchronize on `this` - the object handed to
    // the caller - so caller code holding that monitor could block, or deadlock against, a close in flight. The
    // project already fixed this class of defect for memoize*/LazyInitializer (finding R09-5); these sites were
    // missed. Each now locks a private monitor.
    @Test
    public void testClose_DoesNotSynchronizeOnTheReturnedRunnable() throws Exception {
        final MyCloseable c = new MyCloseable();
        assertTrue(runWhileHoldingMonitorOf(Fn.close(c)), "Fn.close(..).run() blocked on the caller-held monitor of the returned Runnable");
        assertEquals(1, c.getCloseCount());

        final MyCloseable q = new MyCloseable();
        assertTrue(runWhileHoldingMonitorOf(Fn.closeQuietly(q)), "Fn.closeQuietly(..).run() blocked on the caller-held monitor of the returned Runnable");
        assertEquals(1, q.getCloseCount());
    }

    @Test
    public void testCloseAll_DoesNotSynchronizeOnTheReturnedRunnable() throws Exception {
        final MyCloseable a1 = new MyCloseable();
        assertTrue(runWhileHoldingMonitorOf(Fn.closeAll(a1)), "Fn.closeAll(array).run() blocked on the caller-held monitor of the returned Runnable");
        assertEquals(1, a1.getCloseCount());

        final MyCloseable a2 = new MyCloseable();
        assertTrue(runWhileHoldingMonitorOf(Fn.closeAll(List.of(a2))), "Fn.closeAll(Collection).run() blocked on the caller-held monitor of the returned Runnable");
        assertEquals(1, a2.getCloseCount());

        final MyCloseable a3 = new MyCloseable();
        assertTrue(runWhileHoldingMonitorOf(Fn.closeAllQuietly(a3)), "Fn.closeAllQuietly(array).run() blocked on the caller-held monitor of the returned Runnable");
        assertEquals(1, a3.getCloseCount());

        final MyCloseable a4 = new MyCloseable();
        assertTrue(runWhileHoldingMonitorOf(Fn.closeAllQuietly(List.of(a4))),
                "Fn.closeAllQuietly(Collection).run() blocked on the caller-held monitor of the returned Runnable");
        assertEquals(1, a4.getCloseCount());
    }

    @Test
    public void testShutdown_DoesNotSynchronizeOnTheReturnedRunnable() throws Exception {
        final ExecutorService first = Executors.newSingleThreadExecutor();
        final ExecutorService second = Executors.newSingleThreadExecutor();

        try {
            assertTrue(runWhileHoldingMonitorOf(Fn.shutdown(first)), "Fn.shutdown(..).run() blocked on the caller-held monitor of the returned Runnable");
            assertTrue(first.isShutdown());

            assertTrue(runWhileHoldingMonitorOf(Fn.shutdown(second, 1, TimeUnit.SECONDS)),
                    "Fn.shutdown(.., timeout, unit).run() blocked on the caller-held monitor of the returned Runnable");
            assertTrue(second.isShutdown());
        } finally {
            first.shutdownNow();
            second.shutdownNow();
        }
    }

    /**
     * Runs {@code task} on another thread while this thread holds {@code task}'s own monitor and reports whether
     * it finished. A library-owned runnable must not be lockable by caller code: if it synchronizes on itself,
     * the caller-held monitor blocks the run.
     */
    private static boolean runWhileHoldingMonitorOf(final java.lang.Runnable task) throws InterruptedException {
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final Thread worker = new Thread(() -> {
            try {
                task.run();
            } catch (final Throwable e) { // NOSONAR - the failure has to reach the assertion in the caller
                failure.set(e);
            } finally {
                done.countDown();
            }
        });
        worker.setDaemon(true);

        final boolean finished;

        synchronized (task) {
            worker.start();
            finished = done.await(5, TimeUnit.SECONDS);
        }

        worker.join(TimeUnit.SECONDS.toMillis(5));
        assertNull(failure.get());

        return finished;
    }

    // FINDING G20-005 (doc-only contract pin): closeAll*/closeAllQuietly read the caller's array or collection
    // live at run() time, which the javadoc now states explicitly. Passes on both sides of the fix by design.
    @Test
    public void testCloseAll_ReadsTheResourcesLiveAtRunTime() {
        final MyCloseable a1 = new MyCloseable();
        final MyCloseable a2 = new MyCloseable();
        final List<AutoCloseable> open = new ArrayList<>(List.of(a1, a2));
        final Runnable closer = Fn.closeAll(open);
        open.clear();
        closer.run();
        assertEquals(0, a1.getCloseCount());
        assertEquals(0, a2.getCloseCount());

        final MyCloseable lateAdded = new MyCloseable();
        final List<AutoCloseable> registerThenFill = new ArrayList<>();
        final Runnable lateCloser = Fn.closeAllQuietly(registerThenFill);
        registerThenFill.add(lateAdded);
        lateCloser.run();
        assertEquals(1, lateAdded.getCloseCount());

        final MyCloseable replaced = new MyCloseable();
        final MyCloseable replacement = new MyCloseable();
        final AutoCloseable[] array = { replaced };
        final Runnable arrayCloser = Fn.closeAll(array);
        array[0] = replacement;
        arrayCloser.run();
        assertEquals(0, replaced.getCloseCount());
        assertEquals(1, replacement.getCloseCount());
    }

    // FINDING G20-001 (doc-only contract pin): numToDouble widens a Float through its decimal spelling rather
    // than Number#doubleValue(), and all three numTo* functions map null to 0.
    @Test
    public void testNumToDouble_FloatIsWidenedViaItsDecimalSpelling() {
        assertEquals(1.21d, Fn.<Number> numToDouble().applyAsDouble(Float.valueOf(1.21f)));
        assertEquals(1.2100000381469727d, Float.valueOf(1.21f).doubleValue());
        assertEquals(0.1d, Fn.<Number> numToDouble().applyAsDouble(Float.valueOf(0.1f)));
        assertEquals(39.5d, Fn.<Number> numToDouble().applyAsDouble(Float.valueOf(39.50f)));
        assertEquals(2.5d, Fn.<Number> numToDouble().applyAsDouble(Double.valueOf(2.5d)));

        assertEquals(0.0d, Fn.<Number> numToDouble().applyAsDouble(null));
        assertEquals(0, Fn.<Number> numToInt().applyAsInt(null));
        assertEquals(0L, Fn.<Number> numToLong().applyAsLong(null));
    }

    // FINDING G20-004 (doc-only contract pin): a cancelled task's CancellationException is unchecked and is not
    // folded into defaultValue; the InterruptedException path does return it and restores the interrupt flag.
    @Test
    public void testFutureGetOrDefaultOnError_CancellationPropagates() throws Exception {
        final CompletableFuture<String> cancelled = new CompletableFuture<>();
        assertTrue(cancelled.cancel(true));
        assertThrows(CancellationException.class, () -> Fn.futureGetOrDefaultOnError("FAILED").apply(cancelled));

        assertEquals("FAILED", Fn.futureGetOrDefaultOnError("FAILED").apply(CompletableFuture.failedFuture(new IllegalStateException("boom"))));

        final FutureTask<String> neverRun = new FutureTask<>(() -> "x");
        final AtomicReference<String> result = new AtomicReference<>();
        final AtomicBoolean interruptRestored = new AtomicBoolean();
        final Thread worker = new Thread(() -> {
            Thread.currentThread().interrupt();
            result.set(Fn.futureGetOrDefaultOnError("FAILED").apply(neverRun));
            interruptRestored.set(Thread.currentThread().isInterrupted());
        });
        worker.setDaemon(true);
        worker.start();
        worker.join(TimeUnit.SECONDS.toMillis(5));
        assertFalse(worker.isAlive());
        assertEquals("FAILED", result.get());
        assertTrue(interruptRestored.get());
    }
}
