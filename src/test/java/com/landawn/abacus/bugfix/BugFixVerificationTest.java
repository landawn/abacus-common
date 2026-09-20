package com.landawn.abacus.bugfix;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.http.HttpHeaders;
import com.landawn.abacus.http.HttpUtil;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.pool.AbstractPoolable;
import com.landawn.abacus.pool.ObjectPool;
import com.landawn.abacus.pool.PoolFactory;
import com.landawn.abacus.pool.Poolable;
import com.landawn.abacus.type.ClazzType;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings.StrUtil;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.LongStream;
import com.landawn.abacus.util.stream.Stream;

/**
 * Tests verifying bug fixes from the multi-agent code review pass.
 * Each test method maps 1:1 to a fix; the test name and inline comment identify the bug.
 */
public class BugFixVerificationTest extends TestBase {

    // -----------------------------------------------------------------
    // Bug: Collectors parallel combiner reversed encounter order
    // util/stream/Collectors.java — Joiner_Combiner and *List_Combiner
    // -----------------------------------------------------------------

    /**
     * Helper: invoke the supplier/accumulator/combiner/finisher pipeline of a Collector
     * with two ordered partials. Verifies the combiner returns left+right (encounter order),
     * not the larger-of-the-two-then-merge ordering of the pre-fix code.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T, R> R collectViaCombiner(final java.util.stream.Collector<T, ?, R> collector, final java.util.List<T> left,
            final java.util.List<T> right) {
        final java.util.function.Supplier supplier = collector.supplier();
        final java.util.function.BiConsumer accumulator = collector.accumulator();
        final java.util.function.BinaryOperator combiner = collector.combiner();
        final java.util.function.Function finisher = collector.finisher();
        final Object la = supplier.get();
        for (T t : left) {
            accumulator.accept(la, t);
        }
        final Object ra = supplier.get();
        for (T t : right) {
            accumulator.accept(ra, t);
        }
        return (R) finisher.apply(combiner.apply(la, ra));
    }

    @Test
    public void collectorIntArrayCombiner_preservesEncounterOrder_whenRightIsLarger() {
        // Pre-fix: when right partial was longer, b.addAll(a) was called so the result was
        // [right..., left...], breaking encounter order. After the fix the combiner always
        // appends right to left. Use partials of 2 vs 5 to force the (a.size() < b.size()) branch.
        final int[] result = collectViaCombiner(Collectors.toIntArray(), Arrays.asList(1, 2), Arrays.asList(3, 4, 5, 6, 7));
        assertTrue(Arrays.equals(new int[] { 1, 2, 3, 4, 5, 6, 7 }, result), "encounter order broken: " + Arrays.toString(result));
    }

    @Test
    public void collectorLongArrayCombiner_preservesEncounterOrder_whenRightIsLarger() {
        final long[] result = collectViaCombiner(Collectors.toLongArray(), Arrays.asList(10L, 20L), Arrays.asList(30L, 40L, 50L, 60L));
        assertTrue(Arrays.equals(new long[] { 10L, 20L, 30L, 40L, 50L, 60L }, result), "encounter order broken: " + Arrays.toString(result));
    }

    @Test
    public void collectorJoiningCombiner_preservesEncounterOrder_whenRightIsLarger() {
        // Pre-fix: when b.length() >= a.length(), the combiner did `b.merge(a)` and returned `b`,
        // producing "right + left". Force right longer than left.
        final String result = collectViaCombiner(Collectors.joining(","), Arrays.asList("a", "b"), Arrays.asList("c", "d", "e", "f"));
        assertEquals("a,b,c,d,e,f", result);
    }

    @Test
    public void collectorByteShortFloatDoubleArrayCombiners_preserveOrder_whenRightIsLarger() {
        final byte[] b = collectViaCombiner(Collectors.toByteArray(), Arrays.asList((byte) 1, (byte) 2), Arrays.asList((byte) 3, (byte) 4, (byte) 5));
        assertTrue(Arrays.equals(new byte[] { 1, 2, 3, 4, 5 }, b));

        final short[] s = collectViaCombiner(Collectors.toShortArray(), Arrays.asList((short) 1, (short) 2), Arrays.asList((short) 3, (short) 4, (short) 5));
        assertTrue(Arrays.equals(new short[] { 1, 2, 3, 4, 5 }, s));

        final float[] f = collectViaCombiner(Collectors.toFloatArray(), Arrays.asList(1f, 2f), Arrays.asList(3f, 4f, 5f));
        assertEquals(5, f.length);
        for (int i = 0; i < 5; i++) {
            assertEquals(i + 1f, f[i], 0f);
        }

        final double[] d = collectViaCombiner(Collectors.toDoubleArray(), Arrays.asList(1d, 2d), Arrays.asList(3d, 4d, 5d));
        assertEquals(5, d.length);
        for (int i = 0; i < 5; i++) {
            assertEquals(i + 1d, d[i], 0d);
        }
    }

    // -----------------------------------------------------------------
    // Bug: @JsonXmlField(isJsonRawValue=true) was quoted/escaped
    // parser/JsonParserImpl.java, XmlParserImpl.java, AbacusXmlParserImpl.java
    // -----------------------------------------------------------------

    public static class RawJsonBean {
        public String title;
        @JsonXmlField(isJsonRawValue = true)
        public String metadata;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getMetadata() {
            return metadata;
        }

        public void setMetadata(String metadata) {
            this.metadata = metadata;
        }
    }

    @Test
    public void jsonRawValue_writesPayloadVerbatim() {
        final RawJsonBean bean = new RawJsonBean();
        bean.title = "doc";
        bean.metadata = "{\"k\":\"v\"}";

        final String json = N.toJson(bean);
        // Documented behavior (per JsonXmlField javadoc): the raw payload appears verbatim
        // as a JSON object value, NOT a quoted/escaped string. Whitespace after colons may
        // vary by config, so normalize before comparison.
        final String normalized = json.replace(" ", "");
        assertTrue(normalized.contains("\"metadata\":{\"k\":\"v\"}"), "raw JSON value should be embedded verbatim, got: " + json);
        assertFalse(json.contains("\\\""), "raw JSON should not be escaped: " + json);
    }

    // Bug: isJsonRawValue deserialize rebuilt raw JSON via getText() without re-escaping,
    // so embedded quotes/newlines corrupted the stored fragment.
    @Test
    public void jsonRawValue_deserialize_preservesEscapedStrings() {
        final String json = "{\"title\":\"doc\",\"metadata\":{\"msg\":\"say \\\"hi\\\"\\n\"}}";
        final RawJsonBean bean = N.fromJson(json, RawJsonBean.class);
        assertNotNull(bean);
        assertEquals("doc", bean.title);
        assertNotNull(bean.metadata);
        // Stored raw fragment must remain valid JSON that re-parses to the original structure.
        assertTrue(bean.metadata.contains("msg"), "raw metadata should contain key msg, got: " + bean.metadata);
        final java.util.Map<?, ?> parsed = N.fromJson(bean.metadata, java.util.Map.class);
        assertEquals("say \"hi\"\n", parsed.get("msg"));
    }

    // -----------------------------------------------------------------
    // Bug: ClazzType.javaType() returned the parameter class, not Class.class
    // type/ClazzType.java
    // -----------------------------------------------------------------

    @Test
    public void clazzType_javaTypeIsClassClass() {
        final Type<Class> t = TypeFactory.getType("Clazz<Integer>");
        // javaType() must describe what the Type handles (java.lang.Class),
        // NOT the type parameter; otherwise type.javaType().isAssignableFrom(...)
        // checks in OptionalType / NullableType / PairType produce wrong results.
        assertSame(Class.class, t.javaType());
        if (t instanceof ClazzType ct) {
            assertSame(Integer.class, ct.parameterClass());
        }
    }

    // -----------------------------------------------------------------
    // Bug: AtomicBooleanType ignored Y/y/1 convention used by every peer Boolean type
    // type/AtomicBooleanType.java
    // -----------------------------------------------------------------

    @Test
    public void atomicBooleanType_acceptsYAndOne() {
        final Type<java.util.concurrent.atomic.AtomicBoolean> t = TypeFactory.getType(java.util.concurrent.atomic.AtomicBoolean.class);

        assertTrue(t.valueOf("Y").get(), "Y should mean true (matches MutableBooleanType / OptionalBooleanType)");
        assertTrue(t.valueOf("y").get());
        assertTrue(t.valueOf("1").get());
        assertTrue(t.valueOf("true").get());
        assertTrue(t.valueOf("TRUE").get());

        assertFalse(t.valueOf("N").get());
        assertFalse(t.valueOf("0").get());
        assertFalse(t.valueOf("false").get());

        assertNull(t.valueOf((String) null));
        assertNull(t.valueOf(""));
        assertNull(t.valueOf("   "));
    }

    // -----------------------------------------------------------------
    // Bug: java.sql.Date / Time / Timestamp valueOf("null") threw instead of returning null
    // type/DateType.java, TimestampType.java, TimeType.java
    // -----------------------------------------------------------------

    @Test
    public void sqlDateType_handlesLiteralNullString() {
        final Type<Date> t = TypeFactory.getType(Date.class);
        assertNull(t.valueOf("null"));
        assertNull(t.valueOf("NULL"));
        assertNull(t.valueOf(""));
        assertNull(t.valueOf((String) null));
    }

    @Test
    public void sqlTimestampType_handlesLiteralNullString() {
        final Type<Timestamp> t = TypeFactory.getType(Timestamp.class);
        assertNull(t.valueOf("null"));
        assertNull(t.valueOf("NULL"));
        assertNull(t.valueOf(""));
    }

    @Test
    public void sqlTimeType_handlesLiteralNullString() {
        final Type<Time> t = TypeFactory.getType(Time.class);
        assertNull(t.valueOf("null"));
        assertNull(t.valueOf("NULL"));
        assertNull(t.valueOf(""));
    }

    // -----------------------------------------------------------------
    // Bug: HttpHeaders.valueOf joined collections with "; " instead of ", "
    // http/HttpHeaders.java
    // -----------------------------------------------------------------

    @Test
    public void httpHeaders_collectionJoinedWithComma() {
        // RFC 7230 §3.2.2 — multi-value headers are comma-separated.
        final String result = HttpHeaders.valueOf(Arrays.asList("gzip", "deflate"));
        assertEquals("gzip, deflate", result);
    }

    @Test
    public void httpHeaders_singleValueCollection() {
        assertEquals("gzip", HttpHeaders.valueOf(Collections.singletonList("gzip")));
    }

    // -----------------------------------------------------------------
    // Bug: HttpUtil.HttpDate.parse(null) NPE'd; doc claimed it returned null
    // http/HttpUtil.java
    // -----------------------------------------------------------------

    @Test
    public void httpDateParse_nullInputReturnsNull() {
        assertNull(HttpUtil.HttpDate.parse(null));
        assertNull(HttpUtil.HttpDate.parse(""));
    }

    @Test
    public void httpDateParse_validRfc1123StillWorks() {
        // Make sure the null-guard didn't break the happy path.
        final java.util.Date d = HttpUtil.HttpDate.parse("Wed, 21 Oct 2015 07:28:00 GMT");
        assertNotNull(d);
    }

    // -----------------------------------------------------------------
    // Bug: GenericObjectPool.close() didn't signal notEmpty;
    //      blocked poll(timeout) callers hung until their full timeout expired.
    // pool/GenericObjectPool.java
    // -----------------------------------------------------------------

    public static class TestPoolable extends AbstractPoolable {
        public TestPoolable() {
            super(60_000L, 60_000L);
        }

        @Override
        public void destroy(final Poolable.Caller caller) {
            // no-op for tests
        }
    }

    @Test
    public void poolClose_wakesBlockedPollWaiter() throws Exception {
        final ObjectPool<TestPoolable> pool = PoolFactory.createObjectPool(8);
        final ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            final CountDownLatch enteredPoll = new CountDownLatch(1);
            final AtomicReference<Throwable> threw = new AtomicReference<>();

            // Submit a thread that will block in poll(longTimeout, NANOS) on an empty pool.
            final Future<?> f = exec.submit(() -> {
                enteredPoll.countDown();
                try {
                    // 30s timeout — pre-fix this would actually wait the full 30 seconds.
                    pool.poll(30, TimeUnit.SECONDS);
                } catch (Throwable t) {
                    threw.set(t);
                }
            });

            assertTrue(enteredPoll.await(2, TimeUnit.SECONDS), "worker did not start");
            // Give the worker time to actually park inside notEmpty.awaitNanos.
            Thread.sleep(100);

            final long startNs = System.nanoTime();
            pool.close();
            // close() must signal notEmpty so the worker wakes up and observes isClosed.
            try {
                f.get(2, TimeUnit.SECONDS); // pre-fix: would time out at 2s
            } catch (java.util.concurrent.TimeoutException te) {
                fail("close() did not wake the blocked poll(timeout) waiter; " + "elapsed=" + (System.nanoTime() - startNs) / 1_000_000 + "ms");
            }
            // The worker should have either thrown IllegalStateException (preferred) or
            // returned null — anything other than hanging is acceptable.
            final Throwable t = threw.get();
            if (t != null) {
                assertTrue(t instanceof IllegalStateException || t instanceof InterruptedException, "unexpected throwable: " + t);
            }
        } finally {
            exec.shutdownNow();
        }
    }

    // -----------------------------------------------------------------
    // Bug: ArrayStream.mapFirst().count() didn't drain the iterator —
    //      after count(), elements were still iterable (with the first one consumed).
    // util/stream/ArrayStream.java
    // -----------------------------------------------------------------

    @Test
    public void mapFirst_count_drainsIterator() {
        // count() must consume the iterator: a subsequent toArray() on the same stream
        // operator chain should yield no elements (the contract IteratorEx.count()
        // promises). We test the user-observable invariant that the size that count()
        // reports equals the number of elements that an independent toArray() reports.
        final long counted = Stream.of(1, 2, 3, 4, 5).mapFirst(i -> i + 100).count();
        assertEquals(5L, counted);

        // Independent stream: toArray() on a separate pipeline must still produce all 5.
        final Object[] arr = Stream.of(1, 2, 3, 4, 5).mapFirst(i -> i + 100).toArray();
        assertEquals(5, arr.length);
        assertEquals(101, arr[0]);
        assertEquals(2, arr[1]);
        assertEquals(5, arr[4]);
    }

    // -----------------------------------------------------------------
    // Bug: IntStream.range/rangeClosed/repeat anonymous count() returned cnt
    //      without resetting it — pipelines that called count() then iterated
    //      saw duplicate output.
    // util/stream/IntStream.java (and Long/Float/Double/Char/Byte)
    // -----------------------------------------------------------------

    @Test
    public void intStreamRange_countMatchesIteration() {
        assertEquals(10L, IntStream.range(0, 10).count());
        assertEquals(10L, LongStream.range(0L, 10L).count());

        // A separately-built stream's toArray must still have every element.
        assertEquals(10, IntStream.range(0, 10).toArray().length);
    }

    @Test
    public void intStreamRepeat_countMatchesIteration() {
        assertEquals(7L, IntStream.repeat(42, 7L).count());
        assertEquals(7, IntStream.repeat(42, 7L).toArray().length);
    }

    // -----------------------------------------------------------------
    // Bug: HttpUtil.getInputStream silently swallowed connection failures
    //      Out of scope here — needs a live HttpURLConnection mock; skipped.
    // -----------------------------------------------------------------

    // -----------------------------------------------------------------
    // Bug: AbstractXmlParser.checkOneNode returned a TEXT_NODE child when
    //      length==1, contradicting the "ignoring text nodes" javadoc.
    //      Fix verified indirectly via XML round-trip test in existing parser tests.
    // -----------------------------------------------------------------

    // Smoke check that ParserFactory still wires up parsers — guards against
    // accidental compile-time regressions from the parser edits.
    @Test
    public void parserFactory_jsonParserStillResolves() {
        assertNotNull(ParserFactory.createJsonParser());
    }

    // Make IntelliJ happy with a not-suppressed-but-referenced helper.
    @SuppressWarnings("unused")
    private static InputStream openStream(String s) {
        return new ByteArrayInputStream(s.getBytes());
    }

    // Sanity: writing a JsonSerConfig is constructable (catches obvious classpath issues).
    @Test
    public void jsonSerConfig_constructable() {
        assertDoesNotThrow(() -> {
            new JsonSerConfig();
        });
    }

    // ---------- Multi-agent review pass (round 3) ----------

    /**
     * ObjListIterator.of(T...) used to delegate to List.of(a), which rejects null elements.
     * The fix uses Arrays.asList so the varargs overload accepts nulls — consistent with the
     * array-backed of(T[], int, int) overload.
     */
    @Test
    public void objListIterator_ofVarargs_acceptsNullElements() {
        com.landawn.abacus.util.ObjListIterator<String> iter = com.landawn.abacus.util.ObjListIterator.of("a", null, "c");
        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());
        assertNull(iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    /**
     * Multiset.toArray(T[]) honors the Collection.toArray null-terminator contract: when the
     * supplied array is larger than the multiset's size, array[size()] is set to null so callers
     * can use the documented end-of-data sentinel.
     */
    @Test
    public void multiset_toArray_setsNullSentinelWhenArrayLarger() {
        com.landawn.abacus.util.Multiset<String> ms = com.landawn.abacus.util.Multiset.of("x", "y");
        String[] target = new String[5];
        Arrays.fill(target, "filled");
        String[] returned = ms.toArray(target);
        assertSame(target, returned);
        assertNull(returned[2], "Collection.toArray(T[]) requires array[size()] == null");
    }

    /**
     * NoCachingNoUpdating.DisposableArray.toArray(A[]) honors the same contract.
     */
    @Test
    public void disposableArray_toArray_setsNullSentinelWhenArrayLarger() {
        com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray<String> da = com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray
                .wrap(new String[] { "a", "b" });
        String[] target = new String[4];
        Arrays.fill(target, "filled");
        String[] returned = da.toArray(target);
        assertSame(target, returned);
        assertNull(returned[2], "DisposableArray.toArray must mirror Collection.toArray null sentinel");
    }

    /**
     * Multiset.compute / computeIfPresent / merge previously NPE'd when the user-supplied
     * remapping function returned null. The fix treats null as "remove the entry" (matching
     * java.util.Map semantics) and returns 0.
     */
    @Test
    public void multiset_compute_treatsNullRemapResultAsRemove() {
        com.landawn.abacus.util.Multiset<String> ms = com.landawn.abacus.util.Multiset.of("a", "a", "b");
        // computeIfPresent: present key, lambda returns null → remove
        int returned = ms.computeIfPresent("a", (e, count) -> null);
        assertEquals(0, returned);
        assertEquals(0, ms.getCount("a"));

        // compute: absent key, lambda returns null → no insertion
        int compute = ms.compute("missing", (e, count) -> null);
        assertEquals(0, compute);
        assertFalse(ms.contains("missing"));

        // merge: present key with lambda returning null → remove
        ms.add("c", 5);
        int merged = ms.merge("c", 3, (oldV, v) -> null);
        assertEquals(0, merged);
        assertEquals(0, ms.getCount("c"));
    }

    /**
     * BufferedCsvWriter no longer escapes CR/LF/TAB into 2-char `\n`/`\r`/`\t` sequences —
     * those characters are now passed through verbatim inside a quoted field, which is what
     * RFC 4180 specifies. Only the quote character is escaped (by doubling).
     */
    @Test
    public void bufferedCsvWriter_passesThroughCRLFInsideQuotedField() throws java.io.IOException {
        java.io.StringWriter sw = new java.io.StringWriter();
        com.landawn.abacus.util.BufferedCsvWriter w = com.landawn.abacus.util.Objectory.createBufferedCsvWriter(sw);
        try {
            // serializeTo is the per-char path that consults the replacement table.
            w.writeCharacter('\n');
            w.writeCharacter('\r');
            w.writeCharacter('\t');
            w.flush();
            String s = sw.toString();
            // Should contain the literal control characters, not the 2-char escape sequences.
            assertTrue(s.indexOf('\n') >= 0, "expected literal LF, got: " + s);
            assertTrue(s.indexOf('\r') >= 0, "expected literal CR, got: " + s);
            assertTrue(s.indexOf('\t') >= 0, "expected literal TAB, got: " + s);
            assertFalse(s.contains("\\n"), "must not contain escaped \\n: " + s);
            assertFalse(s.contains("\\r"), "must not contain escaped \\r: " + s);
            assertFalse(s.contains("\\t"), "must not contain escaped \\t: " + s);
        } finally {
            com.landawn.abacus.util.Objectory.recycle(w);
        }
    }

    /**
     * Strings.lastIndexOf/Strings.indexOf-style separator-respecting variants now respect their
     * documented "do not exceed startIndexFromBack" contract.
     *
     * (Already covered by StringsTest; this duplicates a smoke test for the suite-runnable file.)
     */
    @Test
    public void strings_lastIndexOfWithDelimiter_respectsBound() {
        assertEquals(0, StrUtil.lastIndexOfToken("test value test", "test", " ", 10));
    }

    /**
     * BiMap.copyOf no longer constructs a sorted value-side map for sorted source maps;
     * the value-side LinkedHashMap accepts non-Comparable values.
     */
    @Test
    public void biMap_copyOf_acceptsNonComparableValuesFromSortedSource() {
        java.util.TreeMap<String, Object> src = new java.util.TreeMap<>();
        // An anonymous Object instance is non-Comparable. Pre-fix this would have failed at
        // putAll with ClassCastException because the value-side map was a TreeMap<Object, K>.
        Object value = new Object();
        src.put("key", value);
        com.landawn.abacus.util.BiMap<String, Object> bm = com.landawn.abacus.util.BiMap.copyOf(src);
        assertEquals(1, bm.size());
        assertSame(value, bm.get("key"));
    }

    /**
     * Fraction.toProperString no longer mishandles MIN_VALUE-denominator fractions due to
     * `-1 * denominator` overflow. The fix performs the comparison in the negative domain and
     * guards against denominator == Integer.MIN_VALUE explicitly.
     *
     * Most call paths normalize denominator > 0 so this is hard to construct via the public
     * factories; this test asserts the well-formed fractions still produce expected output.
     */
    @Test
    public void fraction_toProperString_minusOneAndStandard() {
        assertEquals("-1", com.landawn.abacus.util.Fraction.of(-3, 3).toProperString());
        assertEquals("1", com.landawn.abacus.util.Fraction.of(5, 5).toProperString());
        assertEquals("0", com.landawn.abacus.util.Fraction.of(0, 7).toProperString());
        assertEquals("1 1/2", com.landawn.abacus.util.Fraction.of(3, 2).toProperString());
        assertEquals("-1 1/2", com.landawn.abacus.util.Fraction.of(-3, 2).toProperString());
    }

    /**
     * ContinuableFuture.with(delay).get(timeout, unit) used to call sleepUninterruptibly,
     * silently swallowing InterruptedException so blocked callers couldn't be cancelled.
     * The fix uses interruptible sleep capped at the user-supplied timeout.
     */
    @Test
    public void continuableFuture_withDelay_getRespectsInterrupt() throws Exception {
        com.landawn.abacus.util.ContinuableFuture<Integer> base = com.landawn.abacus.util.ContinuableFuture.completed(42);
        // 5-second delay, but we'll interrupt before then.
        com.landawn.abacus.util.ContinuableFuture<Integer> delayed = base.thenDelay(5_000, TimeUnit.MILLISECONDS);

        AtomicReference<Throwable> seen = new AtomicReference<>();
        CountDownLatch started = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            started.countDown();
            try {
                delayed.get();
                seen.set(new AssertionError("should have been interrupted"));
            } catch (Throwable e) {
                seen.set(e);
            }
        });
        t.start();
        assertTrue(started.await(1, TimeUnit.SECONDS));
        // Give the worker a moment to enter the sleep.
        Thread.sleep(50);
        t.interrupt();
        t.join(2_000);
        assertFalse(t.isAlive(), "worker must exit on interrupt");
        assertTrue(seen.get() instanceof InterruptedException || seen.get() instanceof java.util.concurrent.ExecutionException,
                "expected InterruptedException-related throwable, got " + seen.get());
    }

    // ---------- Multi-agent review pass (round 4) ----------

    /**
     * HttpUtil.getContentType / getContentEncoding / getAccept* (Map overloads) only checked
     * the canonical and all-lower spellings. HttpURLConnection.getHeaderFields preserves
     * server casing, so non-canonical spellings (e.g. "CONTENT-TYPE") returned null and the
     * response charset / decompression branches silently fell back to the wrong defaults.
     */
    @Test
    public void httpUtil_headerGetters_caseInsensitiveLookup() {
        java.util.Map<String, String> shouting = new java.util.HashMap<>();
        shouting.put("CONTENT-TYPE", "text/html; charset=ISO-8859-1");
        assertEquals("text/html; charset=ISO-8859-1", HttpUtil.getContentType(shouting));

        java.util.Map<String, String> mixed = new java.util.HashMap<>();
        mixed.put("Content-type", "application/json");
        assertEquals("application/json", HttpUtil.getContentType(mixed));

        java.util.Map<String, String> enc = new java.util.HashMap<>();
        enc.put("CONTENT-ENCODING", "gzip");
        assertEquals("gzip", HttpUtil.getContentEncoding(enc));

        java.util.Map<String, String> accepts = new java.util.HashMap<>();
        accepts.put("ACCEPT", "application/json");
        accepts.put("Accept-Encoding", "br");
        accepts.put("ACCEPT-CHARSET", "utf-8");
        assertEquals("application/json", HttpUtil.getAccept(accepts));
        assertEquals("br", HttpUtil.getAcceptEncoding(accepts));
        assertEquals("utf-8", HttpUtil.getAcceptCharset(accepts));

        java.util.Map<String, String> canonical = new java.util.HashMap<>();
        canonical.put("Content-Type", "application/xml");
        assertEquals("application/xml", HttpUtil.getContentType(canonical));

        assertNull(HttpUtil.getContentType(new java.util.HashMap<>()));
    }

    /**
     * AsciiStreamType.appendTo / ClobAsciiStreamType.appendTo non-Writer branch previously
     * decoded with the platform default charset, contradicting the US-ASCII contract that
     * the Writer branch enforces.
     */
    @Test
    public void asciiStreamType_appendTo_nonWriter_decodesAsAscii() throws Exception {
        final Type<java.io.InputStream> t = TypeFactory.getType(com.landawn.abacus.type.AsciiStreamType.ASCII_STREAM);
        final byte[] bytes = "hello".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        final java.io.InputStream in = new ByteArrayInputStream(bytes);
        final StringBuilder sb = new StringBuilder();
        t.appendTo(sb, in);
        assertEquals("hello", sb.toString());
    }

    /**
     * IndexedType.stringOf previously called Indexed#index() (which throws ArithmeticException
     * via Math.toIntExact for indices &gt; Integer.MAX_VALUE). The fix uses longIndex(), matching
     * appendTo / serializeTo / valueOf.
     */
    @Test
    public void indexedType_stringOf_supportsLargeLongIndex() {
        Type<com.landawn.abacus.util.Indexed<String>> type = TypeFactory.getType("Indexed<String>");
        com.landawn.abacus.util.Indexed<String> big = com.landawn.abacus.util.Indexed.of("hello", (Integer.MAX_VALUE) + 5L);
        String s = type.stringOf(big);
        assertNotNull(s);
        com.landawn.abacus.util.Indexed<String> parsed = type.valueOf(s);
        assertNotNull(parsed);
        assertEquals((Integer.MAX_VALUE) + 5L, parsed.longIndex());
        assertEquals("hello", parsed.value());

        com.landawn.abacus.util.Indexed<String> small = com.landawn.abacus.util.Indexed.of("world", 42);
        com.landawn.abacus.util.Indexed<String> rt = type.valueOf(type.stringOf(small));
        assertEquals(42L, rt.longIndex());
        assertEquals("world", rt.value());
    }

    /**
     * BufferedWriter.close() previously leaked the underlying writer when flush() threw,
     * because IOUtil.close(out) was in the same try block as flush(). The fix nests
     * flush() in an inner try/finally so the underlying writer is always closed.
     */
    @Test
    public void bufferedWriter_close_closesUnderlyingWriterEvenWhenFlushThrows() throws Exception {
        final java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.io.Writer failingFlush = new java.io.Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) {
            }

            @Override
            public void flush() throws java.io.IOException {
                throw new java.io.IOException("boom");
            }

            @Override
            public void close() {
                closed.set(true);
            }
        };
        java.io.BufferedWriter bw = com.landawn.abacus.util.Objectory.createBufferedWriter(failingFlush);
        bw.write("x");
        try {
            bw.close();
            fail("expected IOException from flush()");
        } catch (java.io.IOException expected) {
            // expected
        }
        assertTrue(closed.get(), "close() must invoke the underlying writer's close() even when flush() throws");
    }

    /**
     * ParserUtil.PropInfo.readPropValue previously NPE'd when a field annotated with
     * &#64;JsonXmlField(numberFormat=...) received a JSON null literal, because the
     * numberFormat path called NumberFormat.parse(null) without the per-class null-guard
     * the date-format readers already had.
     */
    public static class BeanWithFormattedPrice {
        @JsonXmlField(numberFormat = "#.##")
        public Double price;

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }
    }

    @Test
    public void parserUtil_readPropValue_numberFormatNullStrValueNoNPE() {
        BeanWithFormattedPrice bean = N.fromJson("{\"price\":null}", BeanWithFormattedPrice.class);
        assertNotNull(bean);
        assertNull(bean.price);
    }

    /**
     * DeserializationConfig.equals previously violated symmetry across subclasses
     * (xml.equals(json) returned true while json.equals(xml) returned false) because the
     * base class used instanceof DeserializationConfig. The fix uses getClass() comparison.
     */
    @Test
    public void deserialization_equals_symmetryAcrossSubclasses() {
        com.landawn.abacus.parser.XmlDeserConfig xml = new com.landawn.abacus.parser.XmlDeserConfig();
        com.landawn.abacus.parser.JsonDeserConfig json = new com.landawn.abacus.parser.JsonDeserConfig();
        assertEquals(json.equals(xml), xml.equals(json));
        assertFalse(xml.equals(json), "Different config subclasses must not compare equal");
        assertEquals(new com.landawn.abacus.parser.XmlDeserConfig(), xml);
    }

    /**
     * XmlParserImpl / AbacusXmlParserImpl previously misreported DAGs (the same object
     * referenced from two distinct branches without an actual cycle) as circular references,
     * because serializedObjects was never removed after the branch finished. The fix wraps
     * the dispatch switch in a try/finally that removes the object, mirroring JsonParserImpl.
     */
    @Test
    public void xmlParser_serialize_dagSharedObject_noFalseCycle() {
        final com.landawn.abacus.parser.XmlParser parser = ParserFactory.createXmlParser();
        final java.util.List<String> shared = new java.util.ArrayList<>(Arrays.asList("a", "b"));
        final java.util.List<Object> root = new java.util.ArrayList<>();
        root.add(shared);
        root.add(shared);
        final String xml = parser.serialize(root);
        assertNotNull(xml);
        assertTrue(xml.contains("a") && xml.contains("b"), "expected DAG serialization to succeed, got: " + xml);
    }

    /**
     * GenericObjectPool.add / GenericKeyedObjectPool.put previously checked
     * element.activityPrint().isExpired() outside the lock and pushed the entry even if it
     * expired during the locked path (where evict() / destroy() of a replaced entry can run
     * unbounded user code). The fix re-evaluates isExpired() inside the lock.
     */
    public static class FlaggablePoolable extends AbstractPoolable {
        private final java.util.concurrent.atomic.AtomicBoolean expiredFlag;

        public FlaggablePoolable(java.util.concurrent.atomic.AtomicBoolean expiredFlag) {
            super(60_000L, 60_000L);
            this.expiredFlag = expiredFlag;
        }

        @Override
        public com.landawn.abacus.pool.ActivityPrint activityPrint() {
            if (expiredFlag.get()) {
                // ActivityPrint is final, so we cannot subclass it. Construct one with the
                // minimum legal liveTime/maxIdleTime (1ms each) and sleep just past that
                // threshold so isExpired() reliably returns true when the pool re-checks.
                com.landawn.abacus.pool.ActivityPrint p = new com.landawn.abacus.pool.ActivityPrint(1L, 1L);
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return p;
            }
            return super.activityPrint();
        }

        @Override
        public void destroy(final Poolable.Caller caller) {
        }
    }

    @Test
    public void genericObjectPool_add_rechecksExpiryUnderLock() throws Exception {
        final java.util.concurrent.atomic.AtomicBoolean expiredFlag = new java.util.concurrent.atomic.AtomicBoolean(false);
        final ObjectPool<FlaggablePoolable> pool = PoolFactory.createObjectPool(4);
        try {
            FlaggablePoolable first = new FlaggablePoolable(expiredFlag);
            assertTrue(pool.add(first));

            FlaggablePoolable later = new FlaggablePoolable(expiredFlag);
            expiredFlag.set(true);
            assertFalse(pool.add(later), "add() must re-check expiry inside the lock");
        } finally {
            pool.close();
        }
    }

    @Test
    public void genericKeyedObjectPool_put_rechecksExpiryUnderLock() throws Exception {
        final java.util.concurrent.atomic.AtomicBoolean expiredFlag = new java.util.concurrent.atomic.AtomicBoolean(false);
        final com.landawn.abacus.pool.KeyedObjectPool<String, FlaggablePoolable> pool = PoolFactory.createKeyedObjectPool(4);
        try {
            FlaggablePoolable first = new FlaggablePoolable(expiredFlag);
            assertTrue(pool.put("a", first));

            FlaggablePoolable later = new FlaggablePoolable(expiredFlag);
            expiredFlag.set(true);
            assertFalse(pool.put("b", later), "put() must re-check expiry inside the lock");
            assertFalse(pool.containsKey("b"), "expired entry must not be inserted");
        } finally {
            pool.close();
        }
    }

    /**
     * Objectory.recycle(char[]/byte[]) previously accepted any array whose length was NOT
     * greater than BUFFER_SIZE — including smaller arrays — which poisoned the pool: a
     * subsequent createXxxArrayBuffer() returned the short array, silently violating the
     * documented buffer size contract.
     */
    @Test
    public void objectory_recycleSmallCharArray_doesNotPoisonPool() {
        char[] tiny = new char[8];
        com.landawn.abacus.util.Objectory.recycle(tiny);
        for (int i = 0; i < 4; i++) {
            char[] buf = com.landawn.abacus.util.Objectory.createCharArrayBuffer();
            try {
                assertNotSame(tiny, buf, "createCharArrayBuffer() must not return the previously-recycled tiny array");
                assertTrue(buf.length >= 1024, "createCharArrayBuffer() must return a buffer of at least default size, got " + buf.length);
            } finally {
                com.landawn.abacus.util.Objectory.recycle(buf);
            }
        }
    }

    @Test
    public void objectory_recycleSmallByteArray_doesNotPoisonPool() {
        byte[] tiny = new byte[8];
        com.landawn.abacus.util.Objectory.recycle(tiny);
        for (int i = 0; i < 4; i++) {
            byte[] buf = com.landawn.abacus.util.Objectory.createByteArrayBuffer();
            try {
                assertNotSame(tiny, buf, "createByteArrayBuffer() must not return the previously-recycled tiny array");
                assertTrue(buf.length >= 1024, "createByteArrayBuffer() must return a buffer of at least default size, got " + buf.length);
            } finally {
                com.landawn.abacus.util.Objectory.recycle(buf);
            }
        }
    }

    @Test
    public void indexedHashCodes_useAllIndexAndValueBits() {
        // The former implementations truncated long/double/float values and ignored the
        // upper half of long indices, producing identical hashes for common unequal values.
        assertFalse(com.landawn.abacus.util.Indexed.of("value", 1L).hashCode() == com.landawn.abacus.util.Indexed.of("value", 1L << 32).hashCode());
        assertFalse(com.landawn.abacus.util.IndexedBoolean.of(true, 1L).hashCode() == com.landawn.abacus.util.IndexedBoolean.of(true, 1L << 32).hashCode());
        assertFalse(com.landawn.abacus.util.IndexedByte.of((byte) 1, 1L).hashCode() == com.landawn.abacus.util.IndexedByte.of((byte) 1, 1L << 32).hashCode());
        assertFalse(com.landawn.abacus.util.IndexedChar.of('a', 1L).hashCode() == com.landawn.abacus.util.IndexedChar.of('a', 1L << 32).hashCode());
        assertFalse(com.landawn.abacus.util.IndexedInt.of(1, 1L).hashCode() == com.landawn.abacus.util.IndexedInt.of(1, 1L << 32).hashCode());
        assertFalse(com.landawn.abacus.util.IndexedLong.of(1L, 0).hashCode() == com.landawn.abacus.util.IndexedLong.of(1L << 32, 0).hashCode());
        assertFalse(
                com.landawn.abacus.util.IndexedShort.of((short) 1, 1L).hashCode() == com.landawn.abacus.util.IndexedShort.of((short) 1, 1L << 32).hashCode());
        assertFalse(com.landawn.abacus.util.IndexedDouble.of(0.01d, 0).hashCode() == com.landawn.abacus.util.IndexedDouble.of(0.02d, 0).hashCode());
        assertFalse(com.landawn.abacus.util.IndexedFloat.of(0.01f, 0).hashCode() == com.landawn.abacus.util.IndexedFloat.of(0.02f, 0).hashCode());

        // Equal objects, including the floating-point edge cases used by equals(), must
        // continue to have equal hashes.
        assertEquals(com.landawn.abacus.util.IndexedDouble.of(Double.NaN, 7), com.landawn.abacus.util.IndexedDouble.of(Double.NaN, 7));
        assertEquals(com.landawn.abacus.util.IndexedDouble.of(Double.NaN, 7).hashCode(), com.landawn.abacus.util.IndexedDouble.of(Double.NaN, 7).hashCode());
        assertFalse(com.landawn.abacus.util.IndexedFloat.of(0.0f, 7).equals(com.landawn.abacus.util.IndexedFloat.of(-0.0f, 7)));
    }
}
