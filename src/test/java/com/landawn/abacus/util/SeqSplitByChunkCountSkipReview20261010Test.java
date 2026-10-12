package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * {@code Seq.splitByChunkCount} skips chunks with index arithmetic, like {@code Stream.splitByChunkCount}: a
 * {@code skip(n)} applied to it never maps the skipped chunks, and still returns exactly the chunks that remain.
 */
class SeqSplitByChunkCountSkipReview20261010Test extends TestBase {
    @Test
    void skipReturnsTheRemainingChunksForBothOrdersAndEverySkipCount() {
        // 17 split into 5 chunks: sizes 3,3,3,4,4 (smaller first) or 4,4,3,3,3 (larger first).
        final List<List<Integer>> expected = List.of(List.of(3, 306, 609, 913, 1317), List.of(4, 408, 811, 1114, 1417));

        for (int order = 0; order < 2; order++) {
            final boolean smallerFirst = order == 0;

            for (long skipped = 0; skipped <= 7; skipped++) {
                final AtomicInteger mapped = new AtomicInteger();
                final List<Integer> actual = Seq.<Integer, RuntimeException> splitByChunkCount(17, 5, smallerFirst, (from, to) -> {
                    mapped.incrementAndGet();
                    return from * 100 + to;
                }).skip(skipped).toList();

                final int remaining = (int) Math.max(0, 5 - skipped);
                assertEquals(expected.get(order).subList(5 - remaining, 5), actual, "order " + order + ", skip " + skipped);
                assertEquals(remaining, mapped.get(), "only the chunks that are read may be mapped");
            }
        }
    }

    @Test
    void skipCoversSmallTotalsAndExactDivisions() {
        for (final boolean smallerFirst : new boolean[] { true, false }) {
            // Fewer elements than chunks: every chunk has size 1.
            assertEquals(List.of("2-3"), chunks(3, 8, smallerFirst).skip(2).toList());
            // Exact division: a single chunk size.
            assertEquals(List.of("6-9", "9-12"), chunks(12, 4, smallerFirst).skip(2).toList());
            // One chunk.
            assertEquals(List.of(), chunks(5, 1, smallerFirst).skip(1).toList());
        }

        // 10 into 4 chunks: 2,2,3,3 or 3,3,2,2; 10 into 3 chunks: 3,3,4 or 4,3,3.
        assertEquals(List.of("4-7", "7-10"), chunks(10, 4, true).skip(2).toList());
        assertEquals(List.of("6-8", "8-10"), chunks(10, 4, false).skip(2).toList());
        assertEquals(List.of("3-6", "6-10"), chunks(10, 3, true).skip(1).toList());
        assertEquals(List.of("4-7", "7-10"), chunks(10, 3, false).skip(1).toList());
    }

    @Test
    void skipDoesNotMapSkippedChunksEvenWhenCounting() {
        for (final boolean smallerFirst : new boolean[] { true, false }) {
            final AtomicInteger mapped = new AtomicInteger();
            final Seq<Integer, RuntimeException> chunks = Seq.splitByChunkCount(17, 5, smallerFirst, (from, to) -> {
                mapped.incrementAndGet();
                return from;
            });

            assertEquals(0, chunks.skip(Long.MAX_VALUE).count());
            assertEquals(0, mapped.get());

            assertEquals(2, Seq.<Integer, RuntimeException> splitByChunkCount(17, 5, smallerFirst, (from, to) -> {
                mapped.incrementAndGet();
                return from;
            }).skip(3).count());
            assertEquals(0, mapped.get());
        }
    }

    @Test
    void skippedChunksCannotThrowButReadChunksStillDo() {
        for (final boolean smallerFirst : new boolean[] { true, false }) {
            final IllegalStateException failure = new IllegalStateException("first chunk");
            final Throwables.IntBiFunction<Integer, RuntimeException> failsOnFirstChunk = (from, to) -> {
                if (from == 0) {
                    throw failure;
                }
                return from;
            };

            assertEquals(4, Seq.splitByChunkCount(17, 5, smallerFirst, failsOnFirstChunk).skip(1).toList().size());
            assertSame(failure, assertThrows(IllegalStateException.class, () -> Seq.splitByChunkCount(17, 5, smallerFirst, failsOnFirstChunk).toList()));
            assertSame(failure, assertThrows(IllegalStateException.class, () -> Seq.splitByChunkCount(17, 5, smallerFirst, failsOnFirstChunk).skip(0).first()));
        }
    }

    @Test
    void skipIsLazyUntilTheSequenceIsConsumed() {
        final AtomicInteger mapped = new AtomicInteger();
        final Seq<Integer, RuntimeException> skipped = Seq.<Integer, RuntimeException> splitByChunkCount(17, 5, (from, to) -> {
            mapped.incrementAndGet();
            return from;
        }).skip(2);

        assertEquals(0, mapped.get());
        assertEquals(List.of(8, 11), skipped.limit(2).toList());
        assertEquals(2, mapped.get());
    }

    @Test
    void skipHandlesMaximumCountsInConstantTime() {
        for (final boolean smallerFirst : new boolean[] { true, false }) {
            final AtomicInteger mapped = new AtomicInteger();
            // About 2^31 single-element chunks: stepping through them one by one would map every skipped chunk.
            final List<Integer> tail = Seq.<Integer, RuntimeException> splitByChunkCount(Integer.MAX_VALUE, Integer.MAX_VALUE, smallerFirst, (from, to) -> {
                mapped.incrementAndGet();
                assertEquals(from + 1, to);
                return from;
            }).skip(Integer.MAX_VALUE - 2L).toList();

            assertEquals(List.of(Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1), tail);
            assertEquals(2, mapped.get());

            // Three large chunks whose sizes differ by one: the cursor arithmetic must stay within int range.
            final List<String> last = chunks(Integer.MAX_VALUE, 3, smallerFirst).skip(2).toList();
            assertEquals(1, last.size());
            assertTrue(last.get(0).endsWith("-" + Integer.MAX_VALUE), last.get(0));
        }
    }

    private static Seq<String, RuntimeException> chunks(final int totalSize, final int maxChunkCount, final boolean smallerFirst) {
        return Seq.splitByChunkCount(totalSize, maxChunkCount, smallerFirst, (from, to) -> from + "-" + to);
    }
}
