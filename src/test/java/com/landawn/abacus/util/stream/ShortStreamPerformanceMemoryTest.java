package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Stream performance and memory regression contracts. */
@Tag("slow-test")
public class ShortStreamPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G098 end ----
    // ---- perf review 2026-10-08 G119 begin ----
    @Nested
    class G119 {
        private static final int OP_STEP = 0;
        private static final int OP_SKIP = 1;
        private static final int OP_LIMIT = 2;

        private final long[] interestingCounts = { 0, 1, 2, 3, 5, 7, 31, 64, Integer.MAX_VALUE, Integer.MAX_VALUE + 1L, Long.MAX_VALUE };

        private long randomCount(final Random random, final int size) {
            return random.nextInt(4) == 0 ? interestingCounts[random.nextInt(interestingCounts.length)] : random.nextInt(size + 4);
        }

        private List<long[]> randomTail(final Random random, final int size) {
            final List<long[]> ops = new ArrayList<>();
            final int opCount = random.nextInt(4);

            for (int i = 0; i < opCount; i++) {
                final int kind = random.nextInt(3);
                final long arg = kind == OP_STEP ? 1 + random.nextInt(5) : randomCount(random, size);
                ops.add(new long[] { kind, arg });
            }

            return ops;
        }

        private List<Integer> applyOracle(final List<Integer> positions, final List<long[]> ops) {
            List<Integer> cur = positions;

            for (final long[] op : ops) {
                final List<Integer> next = new ArrayList<>();

                if (op[0] == OP_STEP) {
                    for (long i = 0; i < cur.size(); i += op[1]) {
                        next.add(cur.get((int) i));
                    }
                } else if (op[0] == OP_SKIP) {
                    next.addAll(cur.subList((int) Math.min(op[1], cur.size()), cur.size()));
                } else {
                    next.addAll(cur.subList(0, (int) Math.min(op[1], cur.size())));
                }

                cur = next;
            }

            return cur;
        }

        private List<Integer> range(final int fromIndex, final int toIndex) {
            final List<Integer> result = new ArrayList<>();

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(i);
            }

            return result;
        }

        private ShortStream applyShort(ShortStream s, final List<long[]> ops) {
            for (final long[] op : ops) {
                s = op[0] == OP_STEP ? s.step(op[1]) : op[0] == OP_SKIP ? s.skip(op[1]) : s.limit(op[1]);
            }

            return s;
        }

        private IntStream applyInt(IntStream s, final List<long[]> ops) {
            for (final long[] op : ops) {
                s = op[0] == OP_STEP ? s.step(op[1]) : op[0] == OP_SKIP ? s.skip(op[1]) : s.limit(op[1]);
            }

            return s;
        }

        private String describe(final int length, final int fromIndex, final int toIndex, final String head, final List<long[]> ops) {
            final StringBuilder sb = new StringBuilder("len=" + length + " range=[" + fromIndex + "," + toIndex + ") " + head);

            for (final long[] op : ops) {
                sb.append(op[0] == OP_STEP ? ".step(" : op[0] == OP_SKIP ? ".skip(" : ".limit(").append(op[1]).append(')');
            }

            return sb.toString();
        }

        private short[] randomShorts(final Random random, final int length) {
            final short[] a = new short[length];

            for (int i = 0; i < length; i++) {
                a[i] = random.nextInt(8) == 0 ? (random.nextBoolean() ? Short.MIN_VALUE : Short.MAX_VALUE) : (short) random.nextInt();
            }

            return a;
        }

        // G119-01: step(k) on short array streams followed by random skip/limit/step chains matches an index oracle
        @Test
        void testStepSkip_matchesOracle() {
            final Random random = new Random(20261008119L);

            for (int round = 0; round < 2500; round++) {
                final int length = random.nextInt(4) == 0 ? 0 : random.nextInt(40);
                final int fromIndex = length == 0 ? 0 : random.nextInt(length + 1);
                final int toIndex = fromIndex + (length == fromIndex ? 0 : random.nextInt(length - fromIndex + 1));
                final long step = random.nextInt(8) == 0 ? (random.nextBoolean() ? Integer.MAX_VALUE + 7L : Long.MAX_VALUE) : 2 + random.nextInt(6);
                final List<long[]> ops = new ArrayList<>();
                ops.add(new long[] { OP_STEP, step });
                ops.addAll(randomTail(random, length));
                ops.add(1, new long[] { OP_SKIP, randomCount(random, length) });

                final List<Integer> expected = applyOracle(range(fromIndex, toIndex), ops);
                final String msg = describe(length, fromIndex, toIndex, "step-first", ops);
                final short[] shorts = randomShorts(random, length);
                final short[] expectedShorts = new short[expected.size()];
                int expectedSum = 0;

                for (int i = 0; i < expected.size(); i++) {
                    expectedShorts[i] = shorts[expected.get(i)];
                    expectedSum += expectedShorts[i];
                }

                assertArrayEquals(expectedShorts, applyShort(ShortStream.of(shorts, fromIndex, toIndex), ops).toArray(), msg);
                assertEquals(expected.size(), applyShort(ShortStream.of(shorts, fromIndex, toIndex), ops).count(), msg);
                assertEquals(expectedSum, applyShort(ShortStream.of(shorts, fromIndex, toIndex), ops).sum(), msg);

                final int position = random.nextInt(expected.size() + 3);
                final com.landawn.abacus.util.u.OptionalShort element = applyShort(ShortStream.of(shorts, fromIndex, toIndex), ops).elementAt(position);
                assertEquals(position < expected.size(), element.isPresent(), msg + " elementAt(" + position + ")");

                if (position < expected.size()) {
                    assertEquals(expectedShorts[position], element.get(), msg + " elementAt(" + position + ")");
                }

                // iteratorEx() is what iterator() returns, minus its close-handler warning
                final ShortIteratorEx iter = applyShort(ShortStream.of(shorts, fromIndex, toIndex), ops).iteratorEx();

                for (int i = 0; i < expected.size(); i++) {
                    assertTrue(iter.hasNext(), msg);
                    assertTrue(iter.hasNext(), msg);
                    assertEquals(expectedShorts[i], iter.nextShort(), msg);
                }

                assertFalse(iter.hasNext(), msg);
                assertThrows(java.util.NoSuchElementException.class, iter::nextShort, msg);
            }
        }

        // G119-01: asIntStream() of short array streams followed by random skip/limit/step chains matches an index oracle
        @Test
        void testAsIntStreamSkip_matchesOracle() {
            final Random random = new Random(1190081026L);

            for (int round = 0; round < 2500; round++) {
                final int length = random.nextInt(4) == 0 ? 0 : random.nextInt(40);
                final int fromIndex = length == 0 ? 0 : random.nextInt(length + 1);
                final int toIndex = fromIndex + (length == fromIndex ? 0 : random.nextInt(length - fromIndex + 1));
                final List<long[]> ops = new ArrayList<>();
                ops.add(new long[] { OP_SKIP, randomCount(random, length) });
                ops.addAll(randomTail(random, length));

                if (random.nextBoolean()) {
                    ops.add(random.nextInt(ops.size() + 1), new long[] { OP_SKIP, randomCount(random, length) });
                }

                final List<Integer> expected = applyOracle(range(fromIndex, toIndex), ops);
                final String msg = describe(length, fromIndex, toIndex, "asIntStream()", ops);
                final short[] shorts = randomShorts(random, length);
                final int[] expectedInts = new int[expected.size()];
                long expectedSum = 0;

                for (int i = 0; i < expected.size(); i++) {
                    expectedInts[i] = shorts[expected.get(i)];
                    expectedSum += expectedInts[i];
                }

                assertArrayEquals(expectedInts, applyInt(ShortStream.of(shorts, fromIndex, toIndex).asIntStream(), ops).toArray(), msg);
                assertEquals(expected.size(), applyInt(ShortStream.of(shorts, fromIndex, toIndex).asIntStream(), ops).count(), msg);
                assertEquals(expectedSum, applyInt(ShortStream.of(shorts, fromIndex, toIndex).asIntStream(), ops).sum(), msg);

                final int position = random.nextInt(expected.size() + 3);
                final com.landawn.abacus.util.u.OptionalInt element = applyInt(ShortStream.of(shorts, fromIndex, toIndex).asIntStream(), ops)
                        .elementAt(position);
                assertEquals(position < expected.size(), element.isPresent(), msg + " elementAt(" + position + ")");

                if (position < expected.size()) {
                    assertEquals(expectedInts[position], element.get(), msg + " elementAt(" + position + ")");
                }

                // iteratorEx() is what iterator() returns, minus its close-handler warning
                final IntIteratorEx iter = applyInt(ShortStream.of(shorts, fromIndex, toIndex).asIntStream(), ops).iteratorEx();

                for (int i = 0; i < expected.size(); i++) {
                    assertTrue(iter.hasNext(), msg);
                    assertTrue(iter.hasNext(), msg);
                    assertEquals(expectedInts[i], iter.nextInt(), msg);
                }

                assertFalse(iter.hasNext(), msg);
                assertThrows(java.util.NoSuchElementException.class, iter::nextInt, msg);
            }
        }

        // G119-01: hand-picked boundaries of skip/limit/elementAt after step()/asIntStream(): exact remaining count, one past it, huge counts
        @Test
        void testStepSkip_boundaries() {
            final short[] shorts = { 10, -20, 30, Short.MIN_VALUE, 50, Short.MAX_VALUE, 70 };

            assertArrayEquals(new short[] { 50, 70 }, ShortStream.of(shorts).step(2).skip(2).toArray());
            assertArrayEquals(new short[] { 70 }, ShortStream.of(shorts).step(2).skip(3).toArray());
            assertArrayEquals(new short[0], ShortStream.of(shorts).step(2).skip(4).toArray());
            assertArrayEquals(new short[0], ShortStream.of(shorts).step(2).skip(Long.MAX_VALUE).toArray());
            assertArrayEquals(new short[] { Short.MIN_VALUE }, ShortStream.of(shorts, 1, 6).step(2).skip(1).limit(1).toArray());
            assertArrayEquals(new short[] { -20, 50 }, ShortStream.of(shorts, 1, 7).step(3).toArray());
            assertArrayEquals(new short[] { 50 }, ShortStream.of(shorts, 1, 7).step(3).skip(1).toArray());
            assertArrayEquals(new short[] { 10 }, ShortStream.of(shorts).step(Long.MAX_VALUE).toArray());
            assertArrayEquals(new short[0], ShortStream.of(shorts).step(Long.MAX_VALUE).skip(1).toArray());
            assertArrayEquals(new short[] { 70 }, ShortStream.of(shorts).step(3).skip(1).skip(1).toArray());
            assertEquals(1, ShortStream.of(shorts).step(2).skip(1).limit(2).skip(1).count());
            assertEquals(0, ShortStream.of(shorts).step(2).skip(1).limit(5).skip(Integer.MAX_VALUE + 1L).sum());
            assertEquals(50 + 70, ShortStream.of(shorts).step(2).skip(1).limit(5).skip(1).sum());
            assertArrayEquals(new short[0], ShortStream.empty().step(2).skip(1).toArray());
            assertEquals(50, ShortStream.of(shorts).step(2).elementAt(2).get());
            assertEquals(70, ShortStream.of(shorts).step(2).elementAt(3).get());
            assertFalse(ShortStream.of(shorts).step(2).elementAt(4).isPresent());
            assertFalse(ShortStream.of(shorts).step(2).elementAt(Long.MAX_VALUE).isPresent());
            assertEquals(Short.MAX_VALUE, ShortStream.of(shorts).step(5).elementAt(1).get());

            assertArrayEquals(new int[] { 50, Short.MAX_VALUE, 70 }, ShortStream.of(shorts).asIntStream().skip(4).toArray());
            assertArrayEquals(new int[] { 70 }, ShortStream.of(shorts).asIntStream().skip(6).toArray());
            assertArrayEquals(new int[0], ShortStream.of(shorts).asIntStream().skip(7).toArray());
            assertArrayEquals(new int[0], ShortStream.of(shorts).asIntStream().skip(Long.MAX_VALUE).toArray());
            assertArrayEquals(new int[] { Short.MIN_VALUE, 50 }, ShortStream.of(shorts, 2, 5).asIntStream().skip(1).toArray());
            assertArrayEquals(new int[] { -20, Short.MAX_VALUE }, ShortStream.of(shorts, 1, 7).asIntStream().step(4).toArray());
            assertArrayEquals(new int[] { 50 }, ShortStream.of(shorts).asIntStream().skip(1).step(3).skip(1).limit(1).toArray());
            assertArrayEquals(new int[0], ShortStream.of(shorts, 3, 3).asIntStream().skip(1).toArray());
            assertEquals(0, ShortStream.of(shorts).asIntStream().limit(3).skip(5).count());
            assertEquals(2, ShortStream.of(shorts).asIntStream().skip(5).count());
            assertEquals(Short.MIN_VALUE + 50 + Short.MAX_VALUE + 70, ShortStream.of(shorts).asIntStream().skip(3).sum());
            assertEquals(Short.MIN_VALUE, ShortStream.of(shorts).asIntStream().elementAt(3).get());
            assertEquals(70, ShortStream.of(shorts).asIntStream().elementAt(6).get());
            assertFalse(ShortStream.of(shorts).asIntStream().elementAt(7).isPresent());
            assertFalse(ShortStream.of(shorts).asIntStream().elementAt(Long.MAX_VALUE).isPresent());
        }

        // G119-01: a skip() over a partially consumed limit() keeps the same position; the source is read lazily, on first access
        @Test
        void testStepSkip_iteratorProtocol() {
            final short[] shorts = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };

            final ShortIteratorEx stepIter = ShortStream.of(shorts).step(2).iteratorEx();
            assertEquals(1, stepIter.nextShort());
            stepIter.advance(2);
            assertEquals(7, stepIter.nextShort());
            stepIter.advance(Long.MAX_VALUE);
            assertFalse(stepIter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, stepIter::nextShort);

            final IntIteratorEx intIter = ShortStream.of(shorts, 2, 9).asIntStream().skip(2).limit(3).iteratorEx();
            assertEquals(5, intIter.nextInt());
            intIter.advance(1);
            assertEquals(7, intIter.nextInt());
            assertFalse(intIter.hasNext());
            intIter.advance(5);
            assertFalse(intIter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, intIter::nextInt);

            final ShortIteratorEx limitIter = ShortStream.of(shorts).step(3).limit(3).iteratorEx();
            assertEquals(1, limitIter.nextShort());
            limitIter.advance(10);
            assertFalse(limitIter.hasNext());
            assertEquals(0, limitIter.count());

            final AtomicInteger closed = new AtomicInteger();
            assertEquals(3, ShortStream.of(shorts).onClose(closed::incrementAndGet).step(2).skip(3).count());
            assertEquals(1, closed.get());
            assertEquals(3, ShortStream.of(shorts).onClose(closed::incrementAndGet).asIntStream().skip(8).count());
            assertEquals(2, closed.get());
        }

        // G119-01: parallel array streams inherit step()/asIntStream(); skip after them still yields the same elements
        @Test
        void testAsIntStreamSkip_parallel() {
            final short[] shorts = new short[2000];

            for (int i = 0; i < shorts.length; i++) {
                shorts[i] = (short) (i * 37);
            }

            final int[] expectedInts = new int[1990];

            for (int i = 0; i < 1990; i++) {
                expectedInts[i] = shorts[i + 10];
            }

            final int[] actualInts = ShortStream.of(shorts).parallel(3).asIntStream().skip(10).toArray();
            Arrays.sort(expectedInts);
            Arrays.sort(actualInts);
            assertArrayEquals(expectedInts, actualInts);
            assertEquals(1990, ShortStream.of(shorts).parallel(3).asIntStream().skip(10).count());

            final short[] expectedShorts = new short[400];

            for (int i = 0; i < 400; i++) {
                expectedShorts[i] = shorts[1200 + 2 * i];
            }

            final short[] actualShorts = ShortStream.of(shorts).parallel(3).step(2).skip(600).toArray();
            Arrays.sort(expectedShorts);
            Arrays.sort(actualShorts);
            assertArrayEquals(expectedShorts, actualShorts);
            assertEquals(400, ShortStream.of(shorts).parallel(3).step(2).skip(600).count());
            assertEquals(332, ShortStream.of(shorts).parallel(3).step(3).skip(335).count());
        }

        // G119-01 (guard, RED on base): the step()/asIntStream() iterators opt into bulk advance, so skip()/limit() skip in O(1)
        @Test
        void testStepAndAsIntStream_optIntoBulkAdvance() {
            final short[] shorts = { 1, 2, 3, 4, 5 };

            assertTrue(ShortStream.of(shorts).step(2).iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(ShortStream.of(shorts).step(2).skip(1).iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(ShortStream.of(shorts).step(2).limit(2).iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(ShortStream.of(shorts).asIntStream().iteratorEx().supportsFailureAtomicAdvance());
            assertTrue(ShortStream.of(shorts).asIntStream().skip(1).limit(2).iteratorEx().supportsFailureAtomicAdvance());
        }
    }
}
