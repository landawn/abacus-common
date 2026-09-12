package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("unit")
public class OpenUtilFixesTest {
    @TempDir Path directory;

    @Test
    void hostlessUrlsAreRejectedInBothEntryPoints() throws Exception {
        for (final String input : List.of("http:/path", "http://:80/path", "file:/文件")) {
            final URL url = new URL(input);
            assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressFromUrl(url));
            assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressListFromUrls(List.of(url)));
        }
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressFromUrl(null));
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressListFromUrls(Arrays.asList((URL) null)));
        assertTrue(AddrUtil.getAddressListFromUrls(null).isEmpty());
        assertTrue(AddrUtil.getAddressListFromUrls(List.of()).isEmpty());
        assertEquals(80, AddrUtil.getAddressFromUrl(new URL("http://127.0.0.1/文件")).getPort());
        assertEquals(65535, AddrUtil.getAddressFromUrl(new URL("http://127.0.0.1:65535/")).getPort());
    }

    @Test
    void lineReadFailureSurvivesCleanupError() {
        for (int route = 0; route < 3; route++) {
            final IOException read = new IOException("读取");
            final AssertionError close = new AssertionError("关闭");
            final AtomicInteger closes = new AtomicInteger();
            final LineIterator iterator = LineIterator.of(new Reader() {
                @Override public int read(char[] buffer, int offset, int length) throws IOException { throw read; }
                @Override public void close() { closes.incrementAndGet(); throw close; }
            });
            final int selected = route;
            final com.landawn.abacus.exception.UncheckedIOException failure = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class, () -> {
                if (selected == 0) iterator.hasNext();
                else if (selected == 1) iterator.next();
                else iterator.stream().toList();
            });
            assertSame(read, failure.getCause());
            assertArrayEquals(new Throwable[] { close }, failure.getSuppressed());
            iterator.close();
            assertEquals(1, closes.get());
        }
        try (LineIterator iterator = LineIterator.of(new StringReader("你好\n\n🙂"))) {
            assertEquals(List.of("你好", "", "🙂"), iterator.toList());
        }
    }

    public static class ThrowingTarget {
        int calls;
        public String invoke(final String value) throws IOException, InterruptedException {
            calls++;
            if (value.equals("io")) throw new IOException("读取");
            if (value.equals("interrupt")) throw new InterruptedException("中断");
            return value;
        }
    }

    @Test
    void reflectiveDispatchNormalizesCheckedFailuresWithoutRetrying() {
        final ThrowingTarget target = new ThrowingTarget();
        final RuntimeException io = assertThrows(RuntimeException.class, () -> Reflection.on(target).invoke("invoke", "io"));
        assertInstanceOf(IOException.class, io.getCause());
        assertEquals(1, target.calls);
        final boolean interrupted = Thread.interrupted();
        try {
            final RuntimeException failure = assertThrows(RuntimeException.class, () -> Reflection.on(target).invoke("invoke", "interrupt"));
            assertInstanceOf(InterruptedException.class, failure.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            if (interrupted) Thread.currentThread().interrupt();
        }
        assertEquals(2, target.calls);
        assertEquals("你好🙂", Reflection.on(target).invoke("invoke", "你好🙂"));
    }

    @Test
    void invalidTypedArrayDoesNotAdvanceTheIterator() {
        for (final List<String> values : List.of(List.<String>of(), Arrays.asList("", "你好🙂", null))) {
            final ObjListIterator<String> iterator = ObjListIterator.of(values);
            assertThrows(NullPointerException.class, () -> iterator.toArray((String[]) null));
            assertEquals(0, iterator.nextIndex());
            final String[] destination = new String[values.size() + 2];
            Arrays.fill(destination, "tail");
            assertSame(destination, iterator.toArray(destination));
            assertEquals(values, Arrays.asList(destination).subList(0, values.size()));
            assertNull(destination[values.size()]);
        }
        final ObjListIterator<String> partial = ObjListIterator.of("a", "b");
        partial.next();
        assertThrows(NullPointerException.class, () -> partial.toArray((String[]) null));
        assertEquals("b", partial.next());
    }

    public record DecimalRecord(double value) {}
    public record BoxedRecord(Double value) {}
    public record IntRecord(int value) {}
    public record TextRecord(String value) {}

    @Test
    void recordsReceiveConvertedScalarConstructorArguments() {
        assertEquals(new DecimalRecord(1.25), JsonUtil.unwrap(new JSONObject("{\"value\":1.25}"), DecimalRecord.class));
        assertEquals(new BoxedRecord(1.25), JsonUtil.unwrap(new JSONObject("{\"value\":1.25}"), BoxedRecord.class));
        assertEquals(new IntRecord(12), JsonUtil.unwrap(new JSONObject().put("value", "12"), IntRecord.class));
        assertEquals(new TextRecord("12"), JsonUtil.unwrap(new JSONObject().put("value", 12), TextRecord.class));
        assertEquals(new TextRecord("你好🙂"), JsonUtil.unwrap(new JSONObject().put("value", "你好🙂"), TextRecord.class));
        assertEquals(new IntRecord(0), JsonUtil.unwrap(new JSONObject().put("value", JSONObject.NULL), IntRecord.class));
        assertEquals(new BoxedRecord(null), JsonUtil.unwrap(new JSONObject(), BoxedRecord.class));
        assertThrows(RuntimeException.class, () -> JsonUtil.unwrap(new JSONObject().put("value", "invalid"), IntRecord.class));
    }

    @Test
    void orderedBiMapViewsPreserveParallelEncounterOrderAndImmutableEntries() {
        final BiMap<Integer, String> map = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
        final var values = map.values();
        final var entries = map.entrySet();
        final var late = values.spliterator();
        for (int i = 0; i < 100; i++) map.put(i, "值" + i);
        assertTrue(late.hasCharacteristics(Spliterator.ORDERED));
        final List<String> copied = new ArrayList<>();
        late.forEachRemaining(copied::add);
        assertEquals(100, copied.size());
        assertEquals("值0", values.parallelStream().findFirst().orElseThrow());
        assertEquals(0, entries.parallelStream().findFirst().orElseThrow().getKey());
        assertFalse(entries.spliterator().hasCharacteristics(Spliterator.SORTED));
        assertThrows(UnsupportedOperationException.class, () -> entries.iterator().next().setValue("changed"));
        assertFalse(new BiMap<>().values().spliterator().hasCharacteristics(Spliterator.ORDERED));
    }

    @Test
    void sourceRelativeLookupPrecedesWorkingDirectoryAndRecursiveDecoys() throws Exception {
        final Path source = Files.writeString(directory.resolve("source.xml"), "");
        final Path preferred = Files.writeString(directory.resolve("pom.xml"), "source-relative");
        assertEquals(preferred.toFile(), PropertiesUtil.findFileRelativeTo(source.toFile(), "pom.xml"));
        final Path exact = Files.createDirectories(directory.resolve("nested")).resolve("配置.xml");
        Files.writeString(exact, "exact");
        Files.writeString(Files.createDirectories(directory.resolve("aaa/nested")).resolve("配置.xml"), "decoy");
        assertEquals(exact.toFile(), PropertiesUtil.findFileRelativeTo(source.toFile(), "nested/配置.xml"));
        assertEquals(exact.toFile(), PropertiesUtil.findFileRelativeTo(null, exact.toString()));
        assertThrows(IllegalArgumentException.class, () -> PropertiesUtil.findFileRelativeTo(source.toFile(), null));
        assertThrows(IllegalArgumentException.class, () -> PropertiesUtil.findFileRelativeTo(source.toFile(), ""));
    }

    static final class TrackingInput extends ByteArrayInputStream {
        int closes;
        TrackingInput(String xml) { super(xml.getBytes(StandardCharsets.UTF_8)); }
        @Override public void close() throws IOException { closes++; super.close(); }
    }
    static final class TrackingReader extends StringReader {
        int closes;
        TrackingReader(String xml) { super(xml); }
        @Override public void close() { closes++; super.close(); }
    }

    @Test
    void xmlApisLeaveCallerOwnedInputsOpen() throws Exception {
        for (final String xml : List.of("<config><value>你好🙂</value></config>", "<config/>", "<config>")) {
            for (int route = 0; route < 3; route++) {
                final TrackingInput input = new TrackingInput(xml);
                final TrackingReader reader = new TrackingReader(xml);
                final int selected = route;
                final org.junit.jupiter.api.function.Executable bytes = () -> {
                    if (selected == 0) PropertiesUtil.loadFromXml(input);
                    else if (selected == 1) PropertiesUtil.loadFromXml(input, Properties.class);
                    else PropertiesUtil.xmlToJava(input, directory.toString(), "example", "Config", false);
                };
                final org.junit.jupiter.api.function.Executable chars = () -> {
                    if (selected == 0) PropertiesUtil.loadFromXml(reader);
                    else if (selected == 1) PropertiesUtil.loadFromXml(reader, Properties.class);
                    else PropertiesUtil.xmlToJava(reader, directory.toString(), "example", "Config", false);
                };
                if (xml.equals("<config>")) {
                    assertThrows(RuntimeException.class, bytes);
                    assertThrows(RuntimeException.class, chars);
                } else {
                    assertDoesNotThrow(bytes);
                    assertDoesNotThrow(chars);
                }
                assertEquals(0, input.closes);
                assertEquals(0, reader.closes);
                input.close(); reader.close();
                assertEquals(1, input.closes); assertEquals(1, reader.closes);
            }
        }
    }

    @Test
    void profilerPreservesDirectInvocationTargetExceptionIdentity() throws Exception {
        final boolean suspended = Profiler.isSuspended();
        try {
            Profiler.suspend();
            for (final InvocationTargetException failure : List.of(new InvocationTargetException(null), new InvocationTargetException(new IOException("读取")))) {
                final Throwables.Runnable<Exception> command = () -> { throw failure; };
                final List<Profiler.MultiLoopsStatistics> results = List.of(Profiler.run(1, 1, 1, command), Profiler.run(1, 1, 1, "你好", command),
                        Profiler.run(1, 0, 1, 0, 1, "", command));
                for (final var result : results) {
                    assertEquals(1, result.getAllFailedMethodStatisticsList().size());
                    assertSame(failure, result.getAllFailedMethodStatisticsList().get(0).getResult());
                }
            }
        } finally {
            if (!suspended) Profiler.resume();
        }
    }

    @Test
    void joinerReacquiresItsBuilderAfterRenderingReadsIt() {
        for (final boolean pooled : new boolean[] { false, true }) {
            try (Joiner receiver = pooled ? Joiner.with(",").reuseBuffer() : Joiner.with(",")) {
                receiver.appendAll(new Object[] { "a", receiver, "b" });
                assertEquals("a,a,b", receiver.toString());
            }
            try (Joiner receiver = Joiner.with(",").reuseBuffer(); Joiner other = Joiner.with(",").reuseBuffer()) {
                final Object renderer = new Object() {
                    @Override public String toString() {
                        final String prefix = receiver.toString();
                        other.append("V");
                        return "x(" + prefix + ")";
                    }
                };
                receiver.appendAll(new Object[] { "a", renderer, "b" });
                assertEquals("a,x(a),b", receiver.toString());
                assertEquals("V", other.toString());
            }
        }
        final Joiner closedDuringRender = Joiner.with(",");
        assertThrows(IllegalStateException.class, () -> closedDuringRender.appendAll(new Object[] { "a", new Object() {
            @Override public String toString() { closedDuringRender.close(); return "b"; }
        } }));
    }

    static final class CountedTask extends FutureTask<Integer> {
        int cancels;
        int queries;
        CountedTask() { super(() -> 1); }
        @Override public boolean cancel(boolean interrupt) { cancels++; return super.cancel(interrupt); }
        @Override public boolean isCancelled() { queries++; return super.isCancelled(); }
    }

    @Test
    void cancellationVisitsSharedDependenciesOnceAndClearsItsContext() {
        final CountedTask leaf = new CountedTask();
        final List<Runnable> queued = new ArrayList<>();
        ContinuableFuture<Integer> graph = ContinuableFuture.wrap(leaf).thenUse(queued::add);
        for (int i = 0; i < 8; i++) {
            final var left = graph.thenCallAsync(value -> value);
            final var right = graph.thenCallAsync(value -> value);
            graph = left.callAsyncAfterBoth(right, (a, b) -> a + b);
        }
        final var wrapped = graph.map(value -> value).thenDelay(1, TimeUnit.SECONDS).thenUse(queued::add);
        assertTrue(wrapped.cancelAll(true));
        assertEquals(24, queued.size());
        assertEquals(1, leaf.cancels);
        assertTrue(wrapped.isAllCancelled());
        assertEquals(1, leaf.queries);
        assertFalse(wrapped.cancelAll(false));
        assertEquals(2, leaf.cancels);
        assertTrue(wrapped.isAllCancelled());
        assertEquals(2, leaf.queries);
    }

    @Test
    void cancellationPreservesOverridesAndRecoversAfterExceptions() {
        final AtomicInteger calls = new AtomicInteger();
        final ContinuableFuture<Integer> custom = new ContinuableFuture<>(new FutureTask<>(() -> 1)) {
            @Override public boolean cancelAll(boolean interrupt) { calls.incrementAndGet(); return super.cancelAll(interrupt); }
        };
        final var joined = new ContinuableFuture<>(new FutureTask<>(() -> 1), List.of(custom, custom), Runnable::run);
        assertTrue(joined.cancelAll(false));
        assertEquals(1, calls.get());
        final AtomicInteger attempts = new AtomicInteger();
        final ContinuableFuture<Integer> throwing = new ContinuableFuture<>(new FutureTask<>(() -> 1)) {
            @Override public boolean cancel(boolean interrupt) {
                if (attempts.incrementAndGet() == 1) throw new IllegalStateException("first attempt");
                return super.cancel(interrupt);
            }
        };
        assertThrows(IllegalStateException.class, () -> throwing.cancelAll(false));
        assertTrue(throwing.cancelAll(false));
        assertEquals(2, attempts.get());
    }

    @Test
    void compoundJdbcCleanupRetainsFirstFailureAndClosesEverything() {
        for (int first = 0; first < 3; first++) {
            for (int second = 0; second < 3; second++) {
                for (int third = 0; third < 3; third++) {
                    final Throwable[] failures = { closeFailure(first), closeFailure(second), closeFailure(third) };
                    final List<String> closed = new ArrayList<>();
                    final Connection connection = jdbc(Connection.class, "connection", failures[2], null, closed);
                    final Statement statement = jdbc(Statement.class, "statement", failures[1], connection, closed);
                    final ResultSet resultSet = jdbc(ResultSet.class, "result", failures[0], statement, closed);
                    final Throwable thrown = assertThrows(Throwable.class, () -> DataSourceUtil.close(resultSet, statement, connection));
                    final Throwable primary = thrown instanceof com.landawn.abacus.exception.UncheckedSQLException ? thrown.getCause() : thrown;
                    assertSame(failures[0], primary);
                    assertArrayEquals(new Throwable[] { failures[1], failures[2] }, primary.getSuppressed());
                    assertEquals(List.of("result", "statement", "connection"), closed);
                }
            }
        }
        for (int route = 0; route < 3; route++) {
            final List<String> closed = new ArrayList<>();
            final Error same = new AssertionError("same");
            final Connection connection = jdbc(Connection.class, "connection", same, null, closed);
            final Statement statement = jdbc(Statement.class, "statement", same, connection, closed);
            final ResultSet resultSet = jdbc(ResultSet.class, "result", same, statement, closed);
            final int selected = route;
            assertSame(same, assertThrows(Error.class, () -> {
                if (selected == 0) DataSourceUtil.close(resultSet, true, true);
                else if (selected == 1) DataSourceUtil.close(resultSet, statement);
                else DataSourceUtil.close(statement, connection);
            }));
            assertEquals(route == 0 ? 3 : 2, closed.size());
            assertEquals(0, same.getSuppressed().length);
        }
        assertDoesNotThrow(() -> DataSourceUtil.close(null, (Statement) null, null));
    }

    private static Throwable closeFailure(int kind) {
        return switch (kind) { case 0 -> new SQLException("SQL 关闭"); case 1 -> new IllegalStateException("runtime 关闭"); default -> new AssertionError("error 关闭"); };
    }

    private static <T> T jdbc(Class<T> type, String name, Throwable failure, Object parent, List<String> closed) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
            if (method.getName().equals("close")) { closed.add(name); if (failure != null) throw failure; return null; }
            if (method.getName().equals("getStatement") || method.getName().equals("getConnection")) return parent;
            if (method.getName().equals("toString")) return name;
            return null;
        }));
    }

    @Test
    void documentedStreamAndRateModesMatchExecution() {
        assertArrayEquals("你好🙂".toCharArray(), CharIterator.of("你好🙂".toCharArray()).stream().parallel(2).toArray());
        assertEquals(0, CharIterator.empty().stream().parallel(2).count());
        final RateLimiter.SleepingStopwatch clock = new RateLimiter.SleepingStopwatch() {
            @Override protected long readMicros() { return 0; }
            @Override protected void sleepMicrosUninterruptibly(long micros) { assertEquals(0, micros, "No positive wait expected"); }
        };
        final RateLimiter limiter = RateLimiter.create(10, clock);
        assertTrue(limiter.tryAcquire(1000));
        assertFalse(limiter.tryAcquire());
    }

    @Test
    void brotliCloseMayLeaveDecodedBytesBuffered() throws Exception {
        final byte[] compressed = java.util.HexFormat.of().parseHex("0b0680616263e69687e4bbb6f09f998203");
        try (BrotliInputStream input = new BrotliInputStream(new ByteArrayInputStream(compressed))) {
            assertEquals('a', input.read());
            input.close();
            assertEquals('b', input.read());
            assertEquals(1, input.skip(1));
            assertEquals(0, input.read(new byte[0]));
        }
    }
}
