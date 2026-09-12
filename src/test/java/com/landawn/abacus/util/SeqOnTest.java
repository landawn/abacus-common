package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class SeqOnTest extends SeqTestSupport {

    @Test
    public void testOnEach() throws Exception {
        List<Integer> sideEffect = new ArrayList<>();
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).onEach(sideEffect::add).toList());
        assertEquals(Arrays.asList(1, 2, 3), sideEffect);
    }

    @Test
    public void testOnFirst() throws Exception {
        AtomicInteger firstValue = new AtomicInteger(0);
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).onFirst(firstValue::set).toList());
        assertEquals(1, firstValue.get());

        AtomicInteger emptyHits = new AtomicInteger();
        assertTrue(Seq.<Integer, Exception> empty().onFirst(x -> emptyHits.incrementAndGet()).toList().isEmpty());
        assertEquals(0, emptyHits.get());

        List<String> seen = new ArrayList<>();
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onFirst(seen::add).skip(2).toArray(String[]::new));
        assertEquals(1, seen.size());
    }

    @Test
    public void testOnFirst_WaitsForSuccessfulSourceRead() throws IOException {
        for (final boolean usePeekAlias : new boolean[] { false, true }) {
            for (final boolean actionFails : new boolean[] { false, true }) {
                final IOException readFailure = new IOException("first source read failed");
                final IOException actionFailure = new IOException("first action failed");
                final AtomicInteger attempts = new AtomicInteger();
                final List<String> observed = new ArrayList<>();
                final Throwables.Iterator<String, IOException> source = new Throwables.Iterator<>() {
                    private final Iterator<String> values = Arrays.asList((String) null, "last").iterator();

                    @Override
                    public boolean hasNext() {
                        return values.hasNext();
                    }

                    @Override
                    public String next() throws IOException {
                        if (attempts.getAndIncrement() == 0) {
                            throw readFailure;
                        }
                        return values.next();
                    }
                };
                final Throwables.Consumer<String, IOException> action = value -> {
                    observed.add(value);
                    if (actionFails) {
                        throw actionFailure;
                    }
                };
                final Seq<String, IOException> input = Seq.of(source);
                try (Seq<String, IOException> seq = usePeekAlias ? input.peekFirst(action) : input.onFirst(action)) {
                    final Throwables.Iterator<String, IOException> iter = seq.iteratorEx();
                    assertSame(readFailure, assertThrows(IOException.class, iter::next));
                    assertTrue(observed.isEmpty());
                    if (actionFails) {
                        assertSame(actionFailure, assertThrows(IOException.class, iter::next));
                    } else {
                        assertNull(iter.next());
                    }
                    assertEquals(Collections.singletonList(null), observed);
                    assertEquals("last", iter.next());
                    assertEquals(Collections.singletonList(null), observed);
                    assertFalse(iter.hasNext());
                }
            }
        }
    }

    @Test
    public void testOnLast() throws Exception {
        AtomicInteger lastValue = new AtomicInteger(0);
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).onLast(lastValue::set).toList());
        assertEquals(3, lastValue.get());

        AtomicInteger emptyHits = new AtomicInteger();
        assertTrue(Seq.<Integer, Exception> empty().onLast(x -> emptyHits.incrementAndGet()).toList().isEmpty());
        assertEquals(0, emptyHits.get());

        List<String> seen = new ArrayList<>();
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onLast(seen::add).skip(2).toArray(String[]::new));
        assertEquals(1, seen.size());
    }

    @Test
    public void testOnClose() throws Exception {
        AtomicInteger closed = new AtomicInteger();
        Seq.of(1, 2, 3).onClose(closed::incrementAndGet).toList();
        assertEquals(1, closed.get());

        List<Integer> closeOrder = new ArrayList<>();
        Seq.of(1, 2, 3).onClose(() -> closeOrder.add(1)).onClose(() -> closeOrder.add(2)).onClose(() -> closeOrder.add(3)).count();
        assertEquals(Arrays.asList(1, 2, 3), closeOrder);

        AtomicInteger twice = new AtomicInteger();
        Seq<String, Exception> seq = Seq.of("x", "y").onClose(twice::incrementAndGet);
        seq.close();
        seq.close();
        assertEquals(1, twice.get());

        assertThrows(IllegalArgumentException.class, () -> Seq.of("a", "b").onClose(null));
    }

    @Test
    public void testOnClose_FlatMap() throws Exception {
        AtomicBoolean outer = new AtomicBoolean();
        AtomicBoolean inner1 = new AtomicBoolean();
        AtomicBoolean inner2 = new AtomicBoolean();
        List<String> result = Seq.of(1, 2).onClose(() -> outer.set(true)).flatMap(i -> {
            if (i == 1) {
                return Seq.of("a" + i, "b" + i).onClose(() -> inner1.set(true));
            }
            return Seq.of("x" + i, "y" + i).onClose(() -> inner2.set(true));
        }).toList();
        assertEquals(Arrays.asList("a1", "b1", "x2", "y2"), result);
        assertTrue(outer.get() && inner1.get() && inner2.get());

        final AtomicInteger innerCloseCount = new AtomicInteger();
        final Seq<Integer, Exception> source = Seq.of(1);
        final Seq<Integer, Exception> flatMapped = source.flatMap(i -> Seq.<Integer, Exception> of(10, 20).onClose(innerCloseCount::incrementAndGet));
        assertEquals(10, flatMapped.iteratorEx().next());
        source.close();
        assertDoesNotThrow(flatMapped::close);
        assertEquals(1, innerCloseCount.get());

        AtomicBoolean outerNull = new AtomicBoolean();
        assertEquals(Collections.singletonList("a1"), Seq.of(1, 2).onClose(() -> outerNull.set(true)).flatMap(i -> i == 1 ? Seq.of("a" + i) : null).toList());
        assertTrue(outerNull.get());
    }

    @Test
    public void testOnClose_HandlerFailures() {
        AtomicBoolean first = new AtomicBoolean();
        AtomicBoolean second = new AtomicBoolean();
        RuntimeException failure = new RuntimeException("Close handler failed");
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> Seq.of(1, 2, 3).onClose(() -> {
            first.set(true);
            throw failure;
        }).onClose(() -> second.set(true)).toList());
        assertTrue(first.get() && second.get());
        assertEquals(failure, thrown);

        RuntimeException repeated = new RuntimeException("repeated close failure");
        AtomicInteger later = new AtomicInteger();
        Seq<Integer, Exception> seq = Seq.of(1).onClose(() -> {
            throw repeated;
        }).onClose(() -> {
            throw repeated;
        }).onClose(later::incrementAndGet);
        RuntimeException repeatedThrown = assertThrows(RuntimeException.class, seq::close);
        assertSame(repeated, repeatedThrown);
        assertEquals(0, repeatedThrown.getSuppressed().length);
        assertEquals(1, later.get());
        assertDoesNotThrow(seq::close);

        AssertionError error = new AssertionError("close failure");
        AtomicInteger afterError = new AtomicInteger();
        Seq<Integer, Exception> seq2 = Seq.of(1).onClose(() -> {
            throw error;
        }).onClose(afterError::incrementAndGet);
        assertSame(error, assertThrows(AssertionError.class, seq2::close));
        assertEquals(1, afterError.get());
    }

    @Test
    public void testOnClose_CalledOnOperationFailure() {
        AtomicBoolean closed = new AtomicBoolean();
        RuntimeException op = new RuntimeException("Operation failed");
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> Seq.of(1, 2, 3).onClose(() -> closed.set(true)).map(i -> {
            if (i == 2) {
                throw op;
            }
            return i * 2;
        }).toList());
        assertSame(op, thrown);
        assertTrue(closed.get());

        AtomicBoolean closed2 = new AtomicBoolean();
        AtomicInteger processed = new AtomicInteger();
        RuntimeException terminal = new RuntimeException("Terminal op failed mid-way");
        RuntimeException thrown2 = assertThrows(RuntimeException.class, () -> Seq.of(1, 2, 3, 4).onClose(() -> closed2.set(true)).forEach(val -> {
            processed.incrementAndGet();
            if (val == 3) {
                throw terminal;
            }
        }));
        assertSame(terminal, thrown2);
        assertEquals(3, processed.get());
        assertTrue(closed2.get());
    }
}
