package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

public class SeqSkipTest extends SeqTestSupport {

    @Test
    public void testSkip() throws Exception {
        assertEquals(Arrays.asList(3, 4, 5), Seq.of(1, 2, 3, 4, 5).skip(2).toList());
        assertArrayEquals(new String[] { "e" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skip(2).skip(2).toArray(String[]::new));

        Seq<Integer, Exception> closed = Seq.of(1, 2, 3);
        drainWithException(closed);
        assertThrows(IllegalStateException.class, () -> closed.skip(1));
    }

    @Test
    public void testSkip_WithAction() throws Exception {
        List<Integer> skipped = new ArrayList<>();
        assertEquals(Arrays.asList(4, 5), Seq.of(1, 2, 3, 4, 5).skip(3, skipped::add).toList());
        assertEquals(Arrays.asList(1, 2, 3), skipped);

        skipped.clear();
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).skip(0, skipped::add).toList());
        assertTrue(skipped.isEmpty());
    }

    @Test
    public void testSkip_PreservesProgressAfterSourceFailure() {
        for (int operation = 0; operation < 4; operation++) {
            try (Seq<Integer, RuntimeException> seq = Seq.of(failingSlicingIterator()).skip(2)) {
                Throwables.Iterator<Integer, RuntimeException> iterator = seq.iteratorEx();
                switch (operation) {
                    case 0 -> assertThrows(IllegalStateException.class, iterator::hasNext);
                    case 1 -> assertThrows(IllegalStateException.class, iterator::next);
                    case 2 -> assertThrows(IllegalStateException.class, iterator::count);
                    case 3 -> assertThrows(IllegalStateException.class, () -> iterator.advance(1));
                    default -> throw new AssertionError(operation);
                }
                assertEquals(Arrays.asList(3, 4), iterator.toList());
            }
        }
    }

    @Test
    public void testSkipUntil() throws Exception {
        assertEquals(Arrays.asList(3, 4, 5), Seq.of(1, 2, 3, 4, 5).skipUntil(x -> x >= 3).toList());
        assertEquals(Arrays.asList(3, 2, 1), Seq.of(1, 2, 3, 2, 1).skipUntil(x -> x == 3).toList());
        assertTrue(Seq.of(1, 2, 3).skipUntil(x -> x > 10).toList().isEmpty());
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).skipUntil(x -> x > 0).toList());
        assertArrayEquals(new String[] { "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skipUntil(s -> s.compareTo("d") >= 0).skip(1).toArray(String[]::new));
    }

    @Test
    public void testSkipNulls() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, null, 2, null, 3).skipNulls().toList());
        assertArrayEquals(new String[] { "e" }, Seq.<String, RuntimeException> of("a", null, "c", null, "e").skipNulls().skip(2).toArray(String[]::new));
    }

    @Test
    public void testSkipLast() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3, 4, 5).skipLast(2).toList());
        assertEquals(Arrays.asList(1, 2), Seq.of(1, 2).skipLast(0).toList());
        assertTrue(Seq.of(1, 2).skipLast(3).toList().isEmpty());
        assertArrayEquals(new String[] { "c" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skipLast(2).skip(2).toArray(String[]::new));
    }

    @Test
    public void testSkipLast_ResumesAfterSourceFailure() {
        for (boolean failHasNext : new boolean[] { false, true }) {
            for (int size : new int[] { 2, 4 }) {
                for (boolean nextFirst : new boolean[] { false, true }) {
                    Throwables.Iterator<Integer, RuntimeException> source = new Throwables.Iterator<>() {
                        private int next = 1;
                        private boolean failed;

                        private void failOnce() {
                            if (next == 2 && !failed) {
                                failed = true;
                                throw new IllegalStateException("source failed while filling the trailing buffer");
                            }
                        }

                        @Override
                        public boolean hasNext() {
                            if (failHasNext) {
                                failOnce();
                            }
                            return next <= size;
                        }

                        @Override
                        public Integer next() {
                            if (!failHasNext) {
                                failOnce();
                            }
                            if (!hasNext()) {
                                throw new NoSuchElementException();
                            }
                            return next++ == 1 ? null : next - 1;
                        }
                    };
                    try (Seq<Integer, RuntimeException> seq = Seq.of(source).skipLast(2)) {
                        Throwables.Iterator<Integer, RuntimeException> iterator = seq.iteratorEx();
                        if (nextFirst) {
                            assertThrows(IllegalStateException.class, iterator::next);
                        } else {
                            assertThrows(IllegalStateException.class, iterator::hasNext);
                        }
                        assertEquals(size == 2 ? Collections.emptyList() : Arrays.asList(null, 2), iterator.toList());
                        assertThrows(NoSuchElementException.class, iterator::next);
                    }
                }
            }
        }
    }

    @Test
    public void testSkipLastTakeLastSliding_tolerateNullElements() throws Exception {
        assertEquals(Arrays.asList("a", null), Seq.of("a", null, "c").skipLast(1).toList());
        assertEquals(Arrays.asList(null, "c"), Seq.of("a", null, "c").takeLast(2).toList());
        List<List<String>> windows = Seq.of("a", null, "c").sliding(2).toList();
        assertEquals(2, windows.size());
        assertEquals(Arrays.asList("a", null), windows.get(0));
        assertEquals(Arrays.asList(null, "c"), windows.get(1));
    }

    @Test
    public void testSkipAndLimit() throws Exception {
        assertEquals(Arrays.asList(3, 4, 5), Seq.of(1, 2, 3, 4, 5, 6, 7).skipAndLimit(2, 3).toList());
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3, 4, 5).skipAndLimit(0, 3).toList());
        assertEquals(Arrays.asList(3, 4, 5), Seq.of(1, 2, 3, 4, 5).skipAndLimit(2, Long.MAX_VALUE).toList());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), Seq.of(1, 2, 3, 4, 5).skipAndLimit(0, Long.MAX_VALUE).toList());
        assertEquals(Arrays.asList(3, 4), Seq.of(1, 2, 3, 4, 5).skipAndLimit(2L, 2L).toList());

        Seq<Integer, Exception> closed = Seq.of(1, 2, 3);
        drainWithException(closed);
        assertThrows(IllegalStateException.class, () -> closed.skipAndLimit(0, Long.MAX_VALUE));
    }
}
