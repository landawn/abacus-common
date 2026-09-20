package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

public class SeqTransformTest extends SeqTestSupport {

    @Test
    public void testTransform() throws Exception {
        assertEquals(Arrays.asList(2, 4, 6), Seq.of(1, 2, 3).transform(seq -> seq.map(x -> x * 2)).toList());
        assertEquals(Arrays.asList("1", "2", "3", "end"), Seq.of(1, 2, 3).transform(s -> s.map(String::valueOf).append("end")).toList());
        assertEquals(Arrays.asList(2, 4, 6), Seq.of(1, 2, 3, 4, 5, 6).transform(s -> s.filter(i -> i % 2 == 0)).toList());
        assertEquals(CommonUtil.asList(4, 16),
                Seq.<Integer, Exception> of(1, 2, 3, 4)
                        .transform(s -> Seq.<Integer, Exception> defer(() -> s.filter(n -> n % 2 == 0).map(n -> n * n)))
                        .toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a", "b").transform(null));
    }

    @Test
    public void testTransform_CheckedException() {
        assertThrows(SQLException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).peek(e -> {
            throw new SQLException("TheXyzSQLException");
        }).toList());

        SQLException ex = assertThrows(SQLException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).peek(e -> {
            throw new SQLException("TheXyzSQLException");
        }).transformViaStream(s -> s.map(e -> e * 2)).toList());
        assertEquals("TheXyzSQLException", ex.getMessage());
        assertThrows(SQLException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).peek(e -> {
            throw new SQLException("TheXyzSQLException");
        }).sps(s -> s.map(e -> e * 2)).toList());
    }

    @Test
    public void testTransformViaStream() throws Exception {
        assertEquals(Arrays.asList(2, 4, 6), Seq.of(1, 2, 3).transformViaStream(seq -> seq.map(x -> x * 2)).toList());
        assertEquals(Arrays.asList("1", "2", "3", "endB"), Seq.of(1, 2, 3).transformViaStream(s -> s.map(String::valueOf).append("endB")).toList());
        assertEquals(Arrays.asList(2, 4), Seq.of(1, 2, 3, 4, 5).transformViaStream(stream -> stream.filter(n -> n % 2 == 0), false).toList());
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30),
                Seq.of(1, 2, 3).transformViaStream(stream -> stream.flatMap(n -> Stream.of(n, n * 10)), false).toList());
        assertTrue(Seq.of(1, 2, 3).transformViaStream(stream -> Stream.empty(), false).toList().isEmpty());
        assertTrue(Seq.of(1, 2, 3).transformViaStream(stream -> null, false).toList().isEmpty());
        assertEquals(Arrays.asList('h', 'e', 'l', 'l', 'o', 'w', 'o', 'r', 'l', 'd'),
                Seq.of("hello", "world", "java")
                        .transformViaStream(stream -> stream.filter(s -> s.length() > 4).flatMapArrayToChar(s -> s.toCharArray()).mapToObj(c -> c), false)
                        .toList());
        assertEquals(Arrays.asList("A", "B", "C"), Seq.of("a", "b", "c").transformViaStream(stream -> stream.map(String::toUpperCase), false).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).transformViaStream(null, false));

        Seq<Integer, Exception> closed = Seq.of(1, 2, 3);
        closed.close();
        assertThrows(IllegalStateException.class, () -> closed.transformViaStream(stream -> stream.map(n -> n * 2), false));
    }

    @Test
    public void testTransformViaStream_Deferred() throws Exception {
        final AtomicInteger transferCalls = new AtomicInteger();
        final Seq<Integer, Exception> result = Seq.<Integer, Exception> of(1, 2, 3).transformViaStream(s -> {
            transferCalls.incrementAndGet();
            return s.map(x -> x * 2);
        }, true);
        assertEquals(0, transferCalls.get());
        assertEquals(CommonUtil.asList(2, 4, 6), result.toList());
        assertEquals(1, transferCalls.get());

        assertEquals(3, Seq.<Integer, Exception> of(1, 2, 3).transformViaStream(s -> s.map(x -> x * 2), true).count());
        assertEquals(Nullable.of(2), Seq.<Integer, Exception> of(1, 2, 3).transformViaStream(s -> s.map(x -> x * 2), true).first());
        assertThrows(Exception.class, () -> Seq.of(1, 2, 3).transformViaStream(stream -> stream.map(n -> {
            if (n == 2) {
                throw new RuntimeException("Test exception");
            }
            return n;
        }), true).toList());
    }

    @Test
    public void testTransformViaStream_CheckedException() {
        SQLException sql = assertThrows(SQLException.class,
                () -> Seq.<Integer, SQLException> of(1, 2, 3).transformViaStream(s -> s.map(e -> e * 2).peek(Fn.cc(e -> {
                    throw new SQLException("TheXyzSQLException");
                }))).toList());
        assertEquals("TheXyzSQLException", sql.getMessage());
        assertThrows(SQLException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).sps(s -> s.map(e -> e * 2).peek(Fn.cc(e -> {
            throw new SQLException("TheXyzSQLException");
        }))).toList());

        IOException io = assertThrows(IOException.class,
                () -> Seq.<Integer, SQLException> of(1, 2, 3).transformViaStream(s -> s.map(e -> e * 2).peek(Fn.cc(e -> {
                    throw new IOException("TheXyzIOException");
                }))).toList());
        assertEquals("TheXyzIOException", io.getMessage());
        assertThrows(IOException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).sps(s -> s.map(e -> e * 2).peek(Fn.cc(e -> {
            throw new IOException("TheXyzIOException");
        }))).toList());
    }

    // --- G13-002: create(Stream, ..) flagged itself initialized before assigning its iterator, so a hasNext()
    // --- retried after a failing Stream.iterator() threw NullPointerException instead of re-reporting the cause.
    @SuppressWarnings("deprecation")
    @Test
    public void testSps_failingStreamIteratorIsReReportedWhenRetried() throws Exception {
        final Iterator<Integer> iter = Seq.<Integer, Exception> of(1, 2, 3).sps(s -> {
            final Stream<Integer> terminated = s.map(v -> v);
            terminated.close();
            return terminated;
        }).stream().iterator();

        assertThrows(IllegalStateException.class, iter::hasNext);
        assertThrows(IllegalStateException.class, iter::hasNext);
    }

    // --- G13-005: every eagerly invoked transform/sps callback must close this sequence when it throws, the way
    // --- collectorParts(..) and every argument check in Seq do.
    private void assertEagerCallbackFailureClosesTheSequence(final String route, final Consumer<Seq<Integer, Exception>> call) {
        final AtomicInteger closed = new AtomicInteger();
        final Seq<Integer, Exception> seq = Seq.<Integer, Exception> of(1, 2, 3).onClose(closed::incrementAndGet);

        assertThrows(IllegalStateException.class, () -> call.accept(seq), route);
        assertEquals(1, closed.get(), route);
    }

    @Test
    public void testEagerCallbackFailureClosesThisSequence() throws Exception {
        assertEagerCallbackFailureClosesTheSequence("transform", s -> s.transform(in -> {
            throw new IllegalStateException("callback failed");
        }));
        assertEagerCallbackFailureClosesTheSequence("transformViaStream", s -> s.transformViaStream(in -> {
            throw new IllegalStateException("callback failed");
        }));
        assertEagerCallbackFailureClosesTheSequence("transformViaStream(false)", s -> s.transformViaStream(in -> {
            throw new IllegalStateException("callback failed");
        }, false));
        assertEagerCallbackFailureClosesTheSequence("sps", s -> s.sps(in -> {
            throw new IllegalStateException("callback failed");
        }));
        assertEagerCallbackFailureClosesTheSequence("sps(int)", s -> s.sps(2, in -> {
            throw new IllegalStateException("callback failed");
        }));

        final ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            assertEagerCallbackFailureClosesTheSequence("sps(int, Executor)", s -> s.sps(2, executor, in -> {
                throw new IllegalStateException("callback failed");
            }));
        } finally {
            executor.shutdown();
        }

        // The deferred route is unaffected: nothing has failed yet at construction time.
        final AtomicInteger deferredClosed = new AtomicInteger();
        final Seq<Object, Exception> deferred = Seq.<Integer, Exception> of(1, 2, 3).onClose(deferredClosed::incrementAndGet).transformViaStream(in -> {
            throw new IllegalStateException("callback failed");
        }, true);
        assertEquals(0, deferredClosed.get());
        assertThrows(IllegalStateException.class, deferred::toList);
        assertEquals(1, deferredClosed.get());
    }
}
