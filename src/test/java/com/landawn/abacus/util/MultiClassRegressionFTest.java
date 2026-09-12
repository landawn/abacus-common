package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.Stream;

/**
 * Regression tests for the second review pass of Hashing, Hex, DigestUtil, RegExUtil, URLEncodedUtil,
 * CsvUtil, ExcelUtil, ClassUtil, AsyncExecutor, ContinuableFuture, Futures and Profiler.
 *
 * <p>Each test is named after the finding it locks down and fails against the pre-fix code. The Excel and
 * Profiler findings live in their own classes because they need POI / {@code System.out} fixtures.</p>
 */
public class MultiClassRegressionFTest extends TestBase {

    // ---------------------------------------------------------------------------------------------
    // B6 - AsyncExecutor must never shut down an externally supplied executor
    // ---------------------------------------------------------------------------------------------

    @Test
    @Timeout(30)
    public void testB6_shutdownDoesNotShutDownBorrowedExecutor() throws Exception {
        final ExecutorService borrowed = Executors.newFixedThreadPool(2);

        try {
            final AsyncExecutor asyncExecutor = new AsyncExecutor(borrowed);
            assertEquals("done", asyncExecutor.execute(() -> "done").get());

            asyncExecutor.shutdown();

            // The wrapper refuses new work...
            assertThrows(IllegalStateException.class, asyncExecutor::getExecutor);

            // ...but the borrowed executor is still fully usable by its owner.
            assertFalse(borrowed.isShutdown());
            assertEquals("still alive", borrowed.submit(() -> "still alive").get());
        } finally {
            borrowed.shutdownNow();
        }
    }

    @Test
    @Timeout(30)
    public void testB6_shutdownAndAwaitDoesNotShutDownBorrowedExecutor() throws Exception {
        final ExecutorService borrowed = Executors.newFixedThreadPool(2);

        try {
            final AsyncExecutor asyncExecutor = new AsyncExecutor(borrowed);
            asyncExecutor.execute(() -> {
            }).get();

            asyncExecutor.shutdownAndAwait(5, TimeUnit.SECONDS);

            assertFalse(borrowed.isShutdown());
            assertEquals(1, (int) borrowed.submit(() -> 1).get());
        } finally {
            borrowed.shutdownNow();
        }
    }

    @Test
    @Timeout(30)
    public void testB6_ownedExecutorIsStillShutDown() {
        final AsyncExecutor asyncExecutor = new AsyncExecutor(1, 1, 1L, TimeUnit.SECONDS);
        final java.util.concurrent.Executor owned = asyncExecutor.getExecutor();

        asyncExecutor.shutdown();

        assertTrue(((ExecutorService) owned).isShutdown());
        assertThrows(IllegalStateException.class, asyncExecutor::getExecutor);
    }

    @Test
    @Timeout(60)
    public void testB6_isTerminatedTracksThisWrappersTasksForABorrowedExecutor() throws Exception {
        final ExecutorService borrowed = Executors.newFixedThreadPool(2);

        try {
            final AsyncExecutor asyncExecutor = new AsyncExecutor(borrowed);
            final CountDownLatch release = new CountDownLatch(1);
            final CountDownLatch started = new CountDownLatch(1);

            final ContinuableFuture<Void> future = asyncExecutor.execute(() -> {
                started.countDown();
                release.await();
            });

            assertTrue(started.await(10, TimeUnit.SECONDS));
            assertFalse(asyncExecutor.isTerminated(), "a running task means not terminated");

            asyncExecutor.shutdown();
            // Still not terminated: the wrapper's own task has not finished.
            assertFalse(asyncExecutor.isTerminated());

            release.countDown();
            future.get();

            // The borrowed executor is alive and will never report termination; the wrapper still does.
            assertFalse(borrowed.isTerminated());
            assertTrue(waitFor(asyncExecutor::isTerminated), "the wrapper terminates once its own task finishes");
        } finally {
            borrowed.shutdownNow();
        }
    }

    @Test
    @Timeout(60)
    public void testB6_shutdownAndAwaitWaitsForThisWrappersTasks() throws Exception {
        final ExecutorService borrowed = Executors.newFixedThreadPool(2);

        try {
            final AsyncExecutor asyncExecutor = new AsyncExecutor(borrowed);
            final AtomicInteger finished = new AtomicInteger();
            final CountDownLatch started = new CountDownLatch(1);

            asyncExecutor.execute(() -> {
                started.countDown();
                N.sleep(200);
                finished.incrementAndGet();
            });

            assertTrue(started.await(10, TimeUnit.SECONDS));
            asyncExecutor.shutdownAndAwait(20, TimeUnit.SECONDS);

            assertEquals(1, finished.get(), "shutdownAndAwait must wait for the wrapper's own task");
            assertTrue(asyncExecutor.isTerminated());
        } finally {
            borrowed.shutdownNow();
        }
    }

    @Test
    @Timeout(30)
    public void testB6_aRejectedSubmissionDoesNotLeakTheInFlightCount() {
        // execute(..) increments the in-flight counter before submitting, so a rejecting executor must
        // decrement it again on the way out or the instance would never report termination.
        final java.util.concurrent.Executor rejecting = command -> {
            throw new java.util.concurrent.RejectedExecutionException("full");
        };

        final AsyncExecutor asyncExecutor = new AsyncExecutor(rejecting);

        for (int i = 0; i < 3; i++) {
            assertThrows(java.util.concurrent.RejectedExecutionException.class, () -> asyncExecutor.execute(() -> {
            }));
        }

        asyncExecutor.shutdown();
        assertTrue(asyncExecutor.isTerminated(), "a rejected submission must not be counted as in flight");
    }

    @Test
    @Timeout(30)
    public void testB6_taskResultsAndCancellationSurviveTheSubmissionWrapper() throws Exception {
        // Tasks are submitted wrapped now; the ContinuableFuture must still resolve and cancel normally.
        final ExecutorService borrowed = Executors.newFixedThreadPool(2);

        try {
            final AsyncExecutor asyncExecutor = new AsyncExecutor(borrowed);

            assertEquals(42, (int) asyncExecutor.execute(() -> 42).get());

            final ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> asyncExecutor.execute((java.util.concurrent.Callable<Integer>) () -> {
                        throw new IllegalStateException("boom");
                    }).get());
            assertEquals("boom", failure.getCause().getMessage());

            final CountDownLatch release = new CountDownLatch(1);
            final ContinuableFuture<Void> pending = asyncExecutor.execute(() -> release.await());
            assertTrue(pending.cancel(true));
            assertTrue(pending.isCancelled());
            release.countDown();

            asyncExecutor.shutdownAndAwait(10, TimeUnit.SECONDS);
            assertTrue(asyncExecutor.isTerminated());
        } finally {
            borrowed.shutdownNow();
        }
    }

    private static boolean waitFor(final java.util.function.BooleanSupplier condition) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);

        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }

            Thread.sleep(5);
        }

        return condition.getAsBoolean();
    }

    // ---------------------------------------------------------------------------------------------
    // B12 - ...AfterFirstSuccess(.., Bi*) dropped the second failure
    // ---------------------------------------------------------------------------------------------

    @Test
    @Timeout(60)
    public void testB12_runAsyncAfterFirstSuccessBiConsumerKeepsSecondFailureSuppressed() throws Exception {
        final Exception first = new IllegalStateException("first");
        final Exception second = new IllegalArgumentException("second");

        final ContinuableFuture<String> f1 = ContinuableFuture.call(() -> {
            throw first;
        });
        final ContinuableFuture<String> f2 = ContinuableFuture.call(() -> {
            N.sleep(50);
            throw second;
        });

        final Exception[] reported = new Exception[1];
        f1.runAsyncAfterFirstSuccess(f2, (value, error) -> reported[0] = error).get();

        assertNotNull(reported[0]);
        assertEquals(1, reported[0].getSuppressed().length, "the losing failure must be kept as suppressed");
    }

    @Test
    @Timeout(60)
    public void testB12_callAsyncAfterFirstSuccessBiFunctionKeepsSecondFailureSuppressed() throws Exception {
        final Exception first = new IllegalStateException("first");
        final Exception second = new IllegalArgumentException("second");

        final ContinuableFuture<String> f1 = ContinuableFuture.call(() -> {
            throw first;
        });
        final ContinuableFuture<String> f2 = ContinuableFuture.call(() -> {
            N.sleep(50);
            throw second;
        });

        final Exception reported = f1.callAsyncAfterFirstSuccess(f2, (value, error) -> error).get();

        assertNotNull(reported);
        assertEquals(1, reported.getSuppressed().length);
    }

    @Test
    @Timeout(60)
    public void testB12_successStillWins() throws Exception {
        final ContinuableFuture<String> failing = ContinuableFuture.call(() -> {
            throw new IllegalStateException("boom");
        });
        final ContinuableFuture<String> ok = ContinuableFuture.call(() -> {
            N.sleep(50);
            return "ok";
        });

        assertEquals("ok", failing.callAsyncAfterFirstSuccess(ok, (value, error) -> value).get());
    }

    // ---------------------------------------------------------------------------------------------
    // B15 - decode(q, charset, Map.class) lost the insertion order decode(q) preserves
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testB15_decodeToBareMapPreservesOrder() {
        final String query = "c=3&a=1&b=2";

        final Map<String, String> viaOverload = URLEncodedUtil.decode(query, StandardCharsets.UTF_8, Map.class);
        assertEquals(LinkedHashMap.class, viaOverload.getClass());
        assertEquals(List.of("c", "a", "b"), List.copyOf(viaOverload.keySet()));
        assertEquals(URLEncodedUtil.decode(query), viaOverload);

        final Map<String, String> viaAbstractMap = URLEncodedUtil.decode(query, StandardCharsets.UTF_8, AbstractMap.class);
        assertEquals(List.of("c", "a", "b"), List.copyOf(viaAbstractMap.keySet()));
    }

    @Test
    public void testB15_concreteMapTypesAreStillHonored() {
        final Map<String, String> sorted = URLEncodedUtil.decode("c=3&a=1&b=2", StandardCharsets.UTF_8, TreeMap.class);
        assertEquals(TreeMap.class, sorted.getClass());
        assertEquals(List.of("a", "b", "c"), List.copyOf(sorted.keySet()));

        final Map<String, String> linked = URLEncodedUtil.decode("c=3&a=1", StandardCharsets.UTF_8, LinkedHashMap.class);
        assertEquals(LinkedHashMap.class, linked.getClass());
    }

    // ---------------------------------------------------------------------------------------------
    // B16 - the '=' lookup rescanned the whole query per token (behaviour must be unchanged)
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testB16_tokenParsingIsUnchanged() {
        assertEquals(linkedMap("a", "1", "b", "2"), URLEncodedUtil.decode("a=1&b=2"));
        assertEquals(linkedMap("a", "1", "b", "2"), URLEncodedUtil.decode("a=1;b=2"));

        // A token without '=' keeps a null value; a later token's '=' must not leak into it.
        final Map<String, String> flags = URLEncodedUtil.decode("flag&x=1");
        assertEquals(2, flags.size());
        assertNull(flags.get("flag"));
        assertEquals("1", flags.get("x"));

        // Only the FIRST '=' of a token separates name from value.
        assertEquals("b=c", URLEncodedUtil.decode("a=b=c").get("a"));

        // An empty value, and empty tokens from repeated/leading/trailing separators.
        assertEquals("", URLEncodedUtil.decode("a=").get("a"));
        assertEquals(linkedMap("a", "1", "b", "2"), URLEncodedUtil.decode("&a=1&&b=2&"));

        // '=' in the value of a preceding token must not be reused by the following valueless token.
        final Map<String, String> mixed = URLEncodedUtil.decode("a=1&flag");
        assertEquals("1", mixed.get("a"));
        assertNull(mixed.get("flag"));

        // A token whose FIRST character is '=' has an empty name, not a null value.
        final Map<String, String> emptyName = URLEncodedUtil.decode("=v");
        assertEquals(1, emptyName.size());
        assertEquals("v", emptyName.get(""));

        assertEquals("", URLEncodedUtil.decode("=").get(""));
        assertEquals("=b", URLEncodedUtil.decode("a==b").get("a"));

        // Both separators, and a '=' token following a valued one.
        final Map<String, String> semi = URLEncodedUtil.decode("a=1;b");
        assertEquals("1", semi.get("a"));
        assertNull(semi.get("b"));

        final Map<String, String> emptyNameAfterValue = URLEncodedUtil.decode("a=1&=2");
        assertEquals("1", emptyNameAfterValue.get("a"));
        assertEquals("2", emptyNameAfterValue.get(""));
    }

    @Test
    public void testB16_multimapAndBeanPathsAgree() {
        final ListMultimap<String, String> multi = URLEncodedUtil.decodeToMultimap("t=a&t=b&flag");
        assertEquals(List.of("a", "b"), multi.get("t"));
        assertEquals(1, multi.get("flag").size());
        assertNull(multi.get("flag").get(0));
    }

    @Test
    @Timeout(20)
    public void testB16_valuelessTokenQueryScalesLinearly() {
        // Valueless tokens followed by a single '=' - the shape the quadratic rescan choked on. Measured
        // on the pre-fix code: 60k tokens took ~3.3 s and the cost grows with the square of the token
        // count, so 300k tokens cannot finish inside this timeout; the linear scan needs ~0.1 s.
        final int tokenCount = 300_000;
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < tokenCount; i++) {
            sb.append('t').append(i).append('&');
        }

        sb.append("last=1");

        final Map<String, String> decoded = URLEncodedUtil.decode(sb.toString());
        assertEquals(tokenCount + 1, decoded.size());
        assertEquals("1", decoded.get("last"));
        assertNull(decoded.get("t0"));
    }

    // ---------------------------------------------------------------------------------------------
    // B4 - jsonToCsv wrote nothing at all for an empty source even with explicit headers
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testB4_explicitHeadersAreWrittenForAnEmptyJsonArray() {
        final StringWriter out = new StringWriter();
        final long rows = CsvUtil.jsonToCsv(new StringReader("[]"), Arrays.asList("a", "b"), out);

        assertEquals(0, rows);
        assertEquals("\"a\",\"b\"", out.toString());

        // ...and the result is a loadable CSV that still knows its columns.
        final Dataset loaded = CsvUtil.load(new StringReader(out.toString()));
        assertEquals(List.of("a", "b"), loaded.columnNames());
        assertEquals(0, loaded.size());
    }

    @Test
    public void testB4_inferredHeadersStillProduceAnEmptyFileForAnEmptyJsonArray() {
        final StringWriter out = new StringWriter();
        // Without an explicit selection the schema can only come from a record, and there is none.
        assertEquals(0, CsvUtil.jsonToCsv(new StringReader("[]"), null, out));
        assertEquals(0, out.toString().length());
    }

    @Test
    public void testB4_nonEmptySourceIsUnchanged() {
        final StringWriter explicit = new StringWriter();
        assertEquals(2, CsvUtil.jsonToCsv(new StringReader("[{\"a\":1,\"b\":2},{\"a\":3,\"b\":4}]"), Arrays.asList("a", "b"), explicit));
        assertEquals("\"a\",\"b\"\n1,2\n3,4", explicit.toString());

        final StringWriter inferred = new StringWriter();
        assertEquals(1, CsvUtil.jsonToCsv(new StringReader("[{\"a\":1,\"b\":2}]"), null, inferred));
        assertEquals("\"a\",\"b\"\n1,2", inferred.toString());
    }

    @Test
    public void testB4_emptySelectionStillWritesNothing() {
        // An explicitly empty selection means "no columns" (the library's null=ALL / empty=honored rule).
        final StringWriter out = new StringWriter();
        assertEquals(2, CsvUtil.jsonToCsv(new StringReader("[{\"a\":1},{\"a\":2}]"), List.of(), out));
        assertEquals(0, out.toString().length());
    }

    // ---------------------------------------------------------------------------------------------
    // CsvUtil.stream - the two duplicated branches were collapsed; behaviour must be identical
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testCsvStreamFilteringIsUnchanged() {
        final String csv = "a,b\n1,x\n2,y\n3,z\n";

        try (Stream<String> all = CsvUtil.stream(new StringReader(csv), null, 0, Long.MAX_VALUE, Fn.alwaysTrue(), (names, row) -> row.get(0) + ":" + row.get(1),
                false)) {
            assertEquals(List.of("1:x", "2:y", "3:z"), all.toList());
        }

        try (Stream<String> filtered = CsvUtil.stream(new StringReader(csv), null, 0, Long.MAX_VALUE, row -> !"2".equals(row[0]),
                (names, row) -> row.get(0) + ":" + row.get(1), false)) {
            assertEquals(List.of("1:x", "3:z"), filtered.toList());
        }

        try (Stream<String> windowed = CsvUtil.stream(new StringReader(csv), null, 1, 1, Fn.alwaysTrue(), (names, row) -> row.get(0), false)) {
            assertEquals(List.of("2"), windowed.toList());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // B20 - Hex error messages named neither the character nor the length
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testB20_illegalCharacterMessageIdentifiesTheCharacter() {
        // A control character used to render as nothing at all: "Illegal hexadecimal character  at index 1".
        final IllegalArgumentException control = assertThrows(IllegalArgumentException.class, () -> Hex.decode("0"));
        assertTrue(control.getMessage().contains("U+0007"), control.getMessage());
        assertTrue(control.getMessage().contains("at index 1"), control.getMessage());

        final IllegalArgumentException printable = assertThrows(IllegalArgumentException.class, () -> Hex.decode("0g"));
        assertTrue(printable.getMessage().contains("'g'"), printable.getMessage());
        assertTrue(printable.getMessage().contains("U+0067"), printable.getMessage());
        assertTrue(printable.getMessage().contains("at index 1"), printable.getMessage());
    }

    @Test
    public void testB20_oddLengthMessageIncludesTheLength() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Hex.decode("abc"));
        assertTrue(e.getMessage().contains("Odd number of characters: 3"), e.getMessage());
    }

    @Test
    public void testHexRoundTripIsUnchanged() {
        final byte[] data = { 0x00, 0x01, (byte) 0xAB, (byte) 0xFF, 0x7F };

        assertEquals("0001abff7f", Hex.encodeToString(data));
        assertEquals("0001ABFF7F", Hex.encodeToString(data, false));
        assertArrayEqualsBytes(data, Hex.decode(Hex.encodeToString(data)));
        assertArrayEqualsBytes(data, Hex.decode(Hex.encodeToString(data, false)));
        assertArrayEqualsBytes(data, Hex.decode(Hex.encode(data)));
        assertEquals(0, Hex.decode("").length);
        assertEquals(0, Hex.encodeToString(new byte[0]).length());
    }

    private static Map<String, String> linkedMap(final String... keyValues) {
        final Map<String, String> map = new LinkedHashMap<>();

        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }

        return map;
    }

    private static void assertArrayEqualsBytes(final byte[] expected, final byte[] actual) {
        assertTrue(Arrays.equals(expected, actual), Arrays.toString(expected) + " != " + Arrays.toString(actual));
    }

    // ---------------------------------------------------------------------------------------------
    // D14 - getDigest(algorithm, default) swallowed everything, not just "not available"
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testD14_unavailableAlgorithmStillFallsBack() {
        final java.security.MessageDigest fallback = DigestUtil.getSha256Digest();

        assertSame(fallback, DigestUtil.getDigest("NoSuchAlgorithm-12345", fallback));
        assertNull(DigestUtil.getDigest("NoSuchAlgorithm-12345", null));
        assertFalse(DigestUtil.isAvailable("NoSuchAlgorithm-12345"));

        assertNotNull(DigestUtil.getDigest("SHA-256", null));
        assertTrue(DigestUtil.isAvailable("SHA-256"));
    }

    @Test
    public void testD14_nullAlgorithmIsStillTreatedAsUnavailable() {
        assertNull(DigestUtil.getDigest(null, null));
        assertFalse(DigestUtil.isAvailable(null));
    }

    @Test
    public void testDigestUtilStillDigests() throws Exception {
        assertEquals("5eb63bbbe01eeed093cb22bb8f5acdc3", DigestUtil.md5Hex("hello world"));
        assertEquals("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", DigestUtil.sha256Hex("hello world"));
    }

    // ---------------------------------------------------------------------------------------------
    // RegExUtil - the null/empty rules the class javadoc now states must actually hold
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testRegExUtilDocumentedNullAndEmptyBehaviour() {
        final java.util.regex.Pattern aStar = java.util.regex.Pattern.compile("a*");

        // Empty source: matched normally by every method.
        assertTrue(RegExUtil.find("", aStar));
        assertEquals("", RegExUtil.findFirst("", aStar));
        assertEquals(List.of(""), RegExUtil.findAll("", aStar));
        assertEquals(1, RegExUtil.countMatches("", aStar));
        assertEquals("X", RegExUtil.replaceAll("", aStar, "X"));

        // Null source: short-circuited to the method's empty result without matching, in EVERY method.
        // This used to be split in two - find/matches/findFirst/findLast normalized null to "" and matched
        // it, so find(null, "a*") was true while countMatches(null, "a*") was 0.
        assertFalse(RegExUtil.find(null, aStar));
        assertFalse(RegExUtil.matches(null, aStar));
        assertNull(RegExUtil.findFirst(null, aStar));
        assertNull(RegExUtil.findLast(null, aStar));

        assertEquals(List.of(), RegExUtil.findAll(null, aStar));
        assertEquals(0, RegExUtil.countMatches(null, aStar));
        assertEquals("", RegExUtil.replaceAll(null, aStar, "X"));
        assertEquals(0, RegExUtil.matchResults(null, aStar).count());
        assertEquals(0, RegExUtil.matchIndices(null, aStar).count());
        assertEquals(0, RegExUtil.split(null, aStar).length);
    }

    @Test
    public void testRegExUtilReplaceAllIsStillNullSafe() {
        assertEquals("Hello World", RegExUtil.replaceAll("Hello   World", "\\s+", " "));
        assertEquals("", RegExUtil.replaceAll(null, "\\s+", " "));
        assertEquals("a-b", RegExUtil.replaceAll("a b", " ", "-"));
    }
}
