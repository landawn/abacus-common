package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.ByteArrayOutputStream;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;

/** AbstractStream lifecycle contracts. */
public class AbstractStreamLifecycleTest extends AbstractStreamTestSupport {

    /**
     * The class javadoc (Stream.java:329-336) promises that a null {@code collector} is "rejected with
     * {@code IllegalArgumentException} naming the parameter" and that "the stream is closed before the
     * exception propagates". All four backends used to throw a raw NPE from
     * {@code collector.supplier()} / {@code collector.characteristics()} and leave the stream open.
     */
    @Test
    public void testCollect_nullCollectorThrowsIaeAndClosesTheStream() {
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2, 3).collect((Collector<Integer, ?, ?>) null));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(Arrays.asList(1, 2, 3).iterator()).collect((Collector<Integer, ?, ?>) null));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2, 3).parallel(2).collect((Collector<Integer, ?, ?>) null));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(Arrays.asList(1, 2, 3).iterator()).parallel(2).collect((Collector<Integer, ?, ?>) null));

        final Stream<Integer> stream = Stream.of(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> stream.collect((Collector<Integer, ?, ?>) null));
        // the stream must have been closed by the failed call, not left usable
        assertThrows(IllegalStateException.class, stream::count);
    }

    @Test
    public void testJoinByRangeClosesStreamWithoutCloseHandlersExactlyOnce() {
        final AtomicInteger closeCount = new AtomicInteger();

        final Stream<Integer> rightForList = newCloseTrackingArrayStream(closeCount);
        final Stream<Pair<Integer, List<Integer>>> listJoin = Stream.of(1).joinByRange(rightForList, (left, right) -> true);
        listJoin.close();
        listJoin.close();
        assertEquals(1, closeCount.get());
        assertThrows(IllegalStateException.class, rightForList::count);

        final Stream<Integer> rightForCollector = newCloseTrackingArrayStream(closeCount);
        final Stream<Integer> collectorJoin = Stream.of(1)
                .joinByRange(rightForCollector, (left, right) -> true, Collectors.toList(), (left, matches) -> matches.size());
        collectorJoin.close();
        collectorJoin.close();
        assertEquals(2, closeCount.get());
        assertThrows(IllegalStateException.class, rightForCollector::count);

        final Stream<Integer> rightForUnjoined = newCloseTrackingArrayStream(closeCount);
        final Stream<Integer> unjoinedJoin = Stream.of(1)
                .joinByRange(rightForUnjoined, (left, right) -> true, Collectors.toList(), (left, matches) -> matches.size(),
                        iter -> Stream.of(iter).map(value -> -value));
        unjoinedJoin.close();
        unjoinedJoin.close();
        assertEquals(3, closeCount.get());
        assertThrows(IllegalStateException.class, rightForUnjoined::count);
    }

    @Test
    public void testJoinByRangeWithCollector_StreamCloseHandler() {
        AtomicInteger closeCount = new AtomicInteger();
        Stream<Integer> left = Stream.of(1, 4);
        Stream<Integer> right = Stream.of(2, 3, 5).onClose(closeCount::incrementAndGet);

        List<Pair<Integer, Long>> result;

        try (Stream<Pair<Integer, Long>> joined = left.joinByRange(right, (l, r) -> r > l && r < l + 3, Collectors.counting())) {
            result = joined.toList();
        }

        assertEquals(1, closeCount.get());
        assertEquals(2L, result.get(0).right().longValue());
        assertEquals(1L, result.get(1).right().longValue());
    }

    // persist

    @Test
    public void testPersistClosesSourceWhenOutputCannotBeOpened() throws IOException {
        // The point of this test is that the SOURCE is closed when the output cannot be opened, whatever the
        // reason. Both provocations below are exercised, because the two report differently: as of the
        // 2026-08-31 IOUtil pass a directory handed to a write destination is a wrong-KIND argument
        // (IllegalArgumentException, which persist(..) already declares), while a file that exists but cannot
        // be written is a genuine I/O failure (UncheckedIOException).
        final File directory = tempFolder.toFile();

        final AtomicInteger persistCloseCount = new AtomicInteger();
        final Stream<Integer> source = Stream.of(1).onClose(persistCloseCount::incrementAndGet);
        assertThrows(IllegalArgumentException.class, () -> source.persist(directory));
        assertEquals(1, persistCloseCount.get());

        final AtomicInteger csvCloseCount = new AtomicInteger();
        final Stream<Map<String, Integer>> csvSource = Stream.of(List.of(Map.of("value", 1))).onClose(csvCloseCount::incrementAndGet);
        assertThrows(IllegalArgumentException.class, () -> csvSource.persistToCsv(directory));
        assertEquals(1, csvCloseCount.get());

        final AtomicInteger jsonCloseCount = new AtomicInteger();
        final Stream<Integer> jsonSource = Stream.of(1).onClose(jsonCloseCount::incrementAndGet);
        assertThrows(IllegalArgumentException.class, () -> jsonSource.persistToJson(directory));
        assertEquals(1, jsonCloseCount.get());

        final File unwritable = new File(directory, "unwritable-persist-target.txt");
        assertTrue(unwritable.createNewFile());
        assertTrue(unwritable.setReadOnly());

        if (unwritable.canWrite()) {
            // This platform/user can write a read-only file; the directory cases above already cover the
            // close-on-failure property.
            return;
        }

        try {
            final AtomicInteger unwritableCloseCount = new AtomicInteger();
            final Stream<Integer> unwritableSource = Stream.of(1).onClose(unwritableCloseCount::incrementAndGet);
            assertThrows(UncheckedIOException.class, () -> unwritableSource.persist(unwritable));
            assertEquals(1, unwritableCloseCount.get());
        } finally {
            // Leave nothing read-only behind, or @TempDir cleanup fails on Windows.
            unwritable.setWritable(true);
        }
    }

    @Test
    public void testPersistClosesSourceWhenStatementPreparationFails() throws SQLException {
        final Connection conn = mock(Connection.class);
        when(conn.prepareStatement(anyString())).thenThrow(new SQLException("prepare failed"));

        final AtomicInteger closeCount = new AtomicInteger();
        final Stream<Integer> source = Stream.of(1).onClose(closeCount::incrementAndGet);

        assertThrows(SQLException.class, () -> source.persist(conn, "INSERT INTO test VALUES (?)", 1, 0, (value, stmt) -> stmt.setInt(1, value)));
        assertEquals(1, closeCount.get());
    }

    @Test
    public void testPersistToOutputStream_doesNotCloseCallerStream() throws IOException {
        // control: persist(toLine, OutputStream) — established behavior (Objectory.recycle, no close)
        {
            final java.io.ByteArrayOutputStream sink = new java.io.ByteArrayOutputStream();
            final CloseCountingOutputStream os = new CloseCountingOutputStream(sink);
            final long cnt = createStream(1, 2, 3).persist(String::valueOf, os);
            assertEquals(3, cnt);
            assertTrue(sink.size() > 0, "persist must write data to the OutputStream");
            assertEquals(0, os.closeCount.get(), "persist(OutputStream) must NOT close the caller's stream");
        }
        // persistToJson(OutputStream)
        {
            final java.io.ByteArrayOutputStream sink = new java.io.ByteArrayOutputStream();
            final CloseCountingOutputStream os = new CloseCountingOutputStream(sink);
            final long cnt = createStream(1, 2, 3).persistToJson(os);
            assertEquals(3, cnt);
            assertTrue(sink.size() > 0, "persistToJson must write data to the OutputStream");
            assertEquals(0, os.closeCount.get(), "persistToJson(OutputStream) must NOT close the caller's stream");
        }
        // persistToCsv(OutputStream)
        {
            final java.io.ByteArrayOutputStream sink = new java.io.ByteArrayOutputStream();
            final CloseCountingOutputStream os = new CloseCountingOutputStream(sink);
            final long cnt = createStream(new TestBean("John", 25)).persistToCsv(os);
            assertEquals(1, cnt);
            assertTrue(sink.size() > 0, "persistToCsv must write data to the OutputStream");
            assertEquals(0, os.closeCount.get(), "persistToCsv(OutputStream) must NOT close the caller's stream");
        }
    }

    @Test
    public void testPersistToCsv_ClosedStream() throws IOException {
        StringWriter writer = new StringWriter();
        Stream<TestBean> beanStream = createStream(new TestBean("John", 25));
        beanStream.close();
        assertThrows(IllegalStateException.class, () -> beanStream.persistToCsv(writer));
    }

    /**
     * Sibling of {@code EntryStreamTest.testSelectByKeyValue_nullClassIsRejectedAndTheStreamIsClosed}:
     * {@code Fn.instanceOf(targetType)} is evaluated as the argument to {@code filter}, so a null class threw
     * before {@code filter}'s own closing {@code checkArgNotNull} ran, leaving the stream open.
     */
    @Test
    public void testSelect_nullClassIsRejectedAndTheStreamIsClosed() {
        final MutableBoolean closed = MutableBoolean.of(false);

        assertThrows(IllegalArgumentException.class, () -> Stream.of("a", 1).onClose(closed::setTrue).select(null));
        assertTrue(closed.isTrue(), "select(null) must close the stream before throwing");

        // the ordinary path is unaffected, sequential and parallel
        assertEquals(N.asList("a"), Stream.of("a", 1).select(String.class).toList());
        assertEquals(N.asList(1), Stream.of("a", 1).select(Integer.class).toList());
        assertEquals(1, Stream.of("a", 1, "b").parallel(2).select(Integer.class).count());
    }

}
