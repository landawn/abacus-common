package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Stream performance and memory regression contracts. */
@Tag("slow-test")
public class CharStreamPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G091 end ----
    // ---- perf review 2026-10-08 G092 begin ----
    @Nested
    class G092 {

        /**
         * Mostly ASCII (a Latin-1 compact String); with {@code utf16} also chars above 0xFF and lone surrogates (a UTF-16 String).
         */
        private String randomString(final Random random, final int length, final boolean utf16) {
            final StringBuilder sb = new StringBuilder(length);

            for (int i = 0; i < length; i++) {
                final int kind = utf16 ? random.nextInt(10) : 0;

                if (kind < 7) {
                    sb.append((char) ('a' + random.nextInt(26)));
                } else if (kind < 9) {
                    sb.append((char) (0x0100 + random.nextInt(0x3000)));
                } else {
                    sb.append((char) (0xD800 + random.nextInt(0x800)));
                }
            }

            return sb.toString();
        }

        private char[] reversedCopy(final char[] a) {
            final char[] result = new char[a.length];

            for (int i = 0; i < a.length; i++) {
                result[i] = a[a.length - 1 - i];
            }

            return result;
        }

        // G092-01: the String-backed iterator's bulk toList() (also behind toArray(), sorted(), reversed(), skip(n).toArray())
        // returns exactly the chars of the range, for Latin-1 and UTF-16 strings, any range and any skip.
        @Test
        void testOfString_bulkToArrayAndToCharListMatchSubstringReference() {
            final Random random = new Random(92);

            for (int round = 0; round < 400; round++) {
                final int length = round < 40 ? round : random.nextInt(round < 380 ? 300 : 5000);
                final String str = randomString(random, length, round % 3 == 0);
                final int fromIndex = random.nextInt(length + 1);
                final int toIndex = fromIndex + random.nextInt(length - fromIndex + 1);
                final char[] expected = str.substring(fromIndex, toIndex).toCharArray();
                final String message = "round " + round + ": [" + fromIndex + ", " + toIndex + ") of length " + length;

                assertArrayEquals(expected, CharStream.of(str, fromIndex, toIndex).toArray(), message);
                assertArrayEquals(str.toCharArray(), CharStream.of(str).toArray(), message);

                final com.landawn.abacus.util.CharList list = CharStream.of(str, fromIndex, toIndex).toCharList();
                assertArrayEquals(expected, list.toArray(), message);
                list.add('#');
                assertEquals(expected.length + 1, list.size(), message);
                assertEquals('#', list.get(expected.length), message);

                final int skip = random.nextInt(expected.length + 2);
                assertArrayEquals(Arrays.copyOfRange(expected, Math.min(skip, expected.length), expected.length),
                        CharStream.of(str, fromIndex, toIndex).skip(skip).toArray(), message + ", skip " + skip);

                final char[] sorted = expected.clone();
                Arrays.sort(sorted);
                assertArrayEquals(sorted, CharStream.of(str, fromIndex, toIndex).sorted().toArray(), message);
                assertArrayEquals(reversedCopy(expected), CharStream.of(str, fromIndex, toIndex).reversed().toArray(), message);
                assertEquals(expected.length, CharStream.of(str, fromIndex, toIndex).count(), message);
            }
        }

        // G092-01: an empty remainder still takes the inherited path: the shared empty array, and a fresh empty mutable list.
        @Test
        void testOfString_emptyRemainderKeepsSharedEmptyArrayAndFreshList() {
            assertSame(com.landawn.abacus.util.N.EMPTY_CHAR_ARRAY, CharStream.of("abc", 1, 1).toArray());
            assertSame(com.landawn.abacus.util.N.EMPTY_CHAR_ARRAY, CharStream.of("abc").skip(3).toArray());
            assertSame(com.landawn.abacus.util.N.EMPTY_CHAR_ARRAY, CharStream.of("abc").skip(7).toArray());

            final com.landawn.abacus.util.CharList first = CharStream.of("abc", 3, 3).toCharList();
            final com.landawn.abacus.util.CharList second = CharStream.of("abc", 3, 3).toCharList();
            assertNotSame(first, second);
            assertEquals(0, first.size());
            first.add('x');
            assertEquals(1, first.size());
            assertEquals(0, second.size());

            // a non-empty result is a fresh array each time
            final char[] a1 = CharStream.of("abc").toArray();
            final char[] a2 = CharStream.of("abc").toArray();
            assertNotSame(a1, a2);
            assertArrayEquals(new char[] { 'a', 'b', 'c' }, a1);
        }

        // G092-01: toList() copies only what was not consumed yet and leaves the iterator exhausted, like the element-wise path.
        @Test
        void testOfString_iteratorToListCopiesRemainderAndExhausts() {
            final String latin1 = "hello, world";
            final String utf16 = "ab" + (char) 0x20AC + "cd" + (char) 0xD83D + (char) 0xDE00 + "ef";

            for (final String str : new String[] { latin1, utf16 }) {
                final CharIteratorEx iter = CharStream.of(str, 1, str.length() - 1).iteratorEx();
                assertEquals(str.charAt(1), iter.nextChar());
                iter.advance(2);

                final com.landawn.abacus.util.CharList rest = iter.toList();
                assertArrayEquals(str.substring(4, str.length() - 1).toCharArray(), rest.toArray());
                assertFalse(iter.hasNext());
                assertThrows(java.util.NoSuchElementException.class, iter::nextChar);
                assertEquals(0, iter.toList().size());
                assertSame(com.landawn.abacus.util.N.EMPTY_CHAR_ARRAY, iter.toArray());
                assertEquals(0, iter.count());

                final CharIteratorEx iter2 = CharStream.of(str).iteratorEx();
                assertArrayEquals(str.toCharArray(), iter2.toArray());
                assertFalse(iter2.hasNext());
                assertSame(com.landawn.abacus.util.N.EMPTY_CHAR_ARRAY, iter2.toArray());
            }
        }

        private WeakReference<Object> payloadRef;

        private boolean awaitCleared(final WeakReference<?> ref) {
            for (int i = 0; i < 50 && ref.get() != null; i++) {
                System.gc();

                try {
                    Thread.sleep(10);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            return ref.get() == null;
        }

        /**
         * Built in a separate frame so that only the returned stream references the payload array.
         */
        private CharStream concatOverPrivateArray(final int n) {
            final char[] payload = new char[n];

            for (int i = 0; i < n; i++) {
                payload[i] = (char) ('a' + i % 20);
            }

            payloadRef = new WeakReference<>(payload);

            return CharStream.concat(Arrays.asList(CharStream.of(payload), null, CharStream.of('Y', 'Z')));
        }

        /**
         * Built in a separate frame so that only the returned stream references the sorted() stream (and its sorted copy).
         */
        private CharStream sortedThenAppend(final int n) {
            final char[] values = new char[n];

            for (int i = 0; i < n; i++) {
                values[i] = (char) ('a' + 99 - i % 100);
            }

            final CharStream sorted = CharStream.of(values).sorted();
            payloadRef = new WeakReference<>(sorted);

            return sorted.append(CharStream.of('Y', 'Z'));
        }

        // G092-07: an exhausted (and closed) input of concat(Collection) is no longer reachable from the concatenation while later inputs run
        @Test
        void testConcat_releasesExhaustedInputWhileLaterInputsRun() {
            final CharStream stream = concatOverPrivateArray(1 << 20);
            final boolean[] released = { false };
            final long[] sum = { 0 };
            final List<Character> tail = new ArrayList<>();

            stream.forEach(x -> {
                if (x < 'a') {
                    if (x == 'Y') {
                        released[0] = awaitCleared(payloadRef);
                    }

                    tail.add(x);
                } else {
                    sum[0] += x;
                }
            });

            long expected = 0;

            for (int i = 0; i < 1 << 20; i++) {
                expected += 'a' + i % 20;
            }

            assertEquals(expected, sum[0]);
            assertEquals(Arrays.asList('Y', 'Z'), tail);
            assertTrue(released[0], "the exhausted first input (and its array) is still reachable from the concatenation");
        }

        // G092-07: append(CharStream) delegates to concat(this, other): the exhausted sorted() receiver (and its sorted copy) is released too
        @Test
        void testAppend_releasesExhaustedSortedReceiverWhileAppendedStreamRuns() {
            final CharStream stream = sortedThenAppend(1 << 20);
            final boolean[] released = { false };
            final List<Character> seen = new ArrayList<>();

            stream.forEach(x -> {
                if (x == 'Y' && seen.size() == 3) {
                    released[0] = awaitCleared(payloadRef);
                }

                if (x < 'a' || seen.size() < 3) {
                    seen.add(x);
                }
            });

            assertEquals(Arrays.asList('a', 'a', 'a', 'Y', 'Z'), seen);
            assertTrue(released[0], "the exhausted sorted receiver (and its sorted copy) is still reachable from the appended stream");
        }

        // G092-07: element order, null inputs, close order (each exhausted input before the next one is opened, the rest on close),
        // and the caller's collection / varargs array are never modified
        @Test
        void testConcat_orderNullInputsAndCloseOrder() {
            final List<String> log = new ArrayList<>();
            final CharStream a = CharStream.of('1', '2').onClose(() -> log.add("closeA"));
            final CharStream b = CharStream.of('3').onClose(() -> log.add("closeB"));
            final CharStream c = CharStream.of('4', '5').onClose(() -> log.add("closeC"));
            final CharStream d = CharStream.of('6').onClose(() -> log.add("closeD"));
            final List<CharStream> input = new ArrayList<>(Arrays.asList(a, null, b, null, c, d));
            final CharStream stream = CharStream.concat(input);
            final CharIteratorEx iter = stream.iteratorEx();
            final List<Character> seen = new ArrayList<>();

            while (seen.size() < 4 && iter.hasNext()) {
                final char x = iter.nextChar();
                seen.add(x);
                log.add("next" + x);
            }

            stream.close();

            assertEquals(Arrays.asList('1', '2', '3', '4'), seen);
            assertEquals(Arrays.asList("next1", "next2", "closeA", "next3", "closeB", "next4", "closeC", "closeD"), log);
            assertEquals(6, input.size());
            assertSame(a, input.get(0));
            assertNull(input.get(1));
            assertSame(b, input.get(2));
            assertNull(input.get(3));
            assertSame(c, input.get(4));
            assertSame(d, input.get(5));
            assertThrows(IllegalStateException.class, a::toArray);
            assertThrows(IllegalStateException.class, d::toArray);

            final CharStream e = CharStream.of('7', '8');
            final CharStream f = CharStream.of('9');
            final CharStream[] array = { e, null, f };
            assertArrayEquals(new char[] { '7', '8', '9' }, CharStream.concat(array).toArray());
            assertSame(e, array[0]);
            assertNull(array[1]);
            assertSame(f, array[2]);

            assertEquals(0, CharStream.concat(Arrays.asList((CharStream) null, null)).count());
            assertEquals(0, CharStream.concat(new ArrayList<CharStream>()).count());
            assertArrayEquals(new char[] { '1', '2', '3', '4' }, CharStream.of('1', '2').append(CharStream.of('3', '4')).toArray());
            assertArrayEquals(new char[] { '3', '4', '1', '2' }, CharStream.of('1', '2').prepend(CharStream.of('3', '4')).toArray());
            assertEquals(5, CharStream.concat(CharStream.of('1', '2'), CharStream.empty(), CharStream.of('3', '4', '5')).count());
            assertEquals(com.landawn.abacus.util.CharList.of('1', '3', '4'),
                    CharStream.concat(Arrays.asList(CharStream.of('1', '2'), CharStream.of('3', '4'))).filter(x -> x != '2').toCharList());
            assertArrayEquals(new char[] { '2', '3' },
                    CharStream.concat(Arrays.asList(CharStream.of('1', '2'), CharStream.of('3', '4'))).skip(1).limit(2).toArray());
        }

        // G092-07: a failing close of an exhausted input and an already-closed input propagate from hasNext(); a retry continues;
        // the remaining inputs are closed with the concatenation
        @Test
        void testConcat_failingCloseAndClosedInputThenRetry() {
            final List<String> log = new ArrayList<>();
            final CharStream a = CharStream.of('1').onClose(() -> {
                log.add("closeA");
                throw new IllegalStateException("closeA failed");
            });
            final CharStream closed = CharStream.of('x');
            closed.close();
            final CharStream c = CharStream.of('3').onClose(() -> log.add("closeC"));
            final CharStream stream = CharStream.concat(Arrays.asList(a, closed, c));
            final CharIteratorEx iter = stream.iteratorEx();

            assertEquals('1', iter.nextChar());
            final IllegalStateException closeFailure = assertThrows(IllegalStateException.class, iter::hasNext);
            assertEquals("closeA failed", closeFailure.getMessage());
            assertEquals(Arrays.asList("closeA"), log);

            // retry: `a` is closed already (no second close call); the next input is closed and is rejected when reached
            final IllegalStateException rejected = assertThrows(IllegalStateException.class, iter::hasNext);
            assertEquals("This stream is already terminated.", rejected.getMessage());
            assertEquals(Arrays.asList("closeA"), log);

            // retry again: traversal continues with the input after the rejected one
            assertTrue(iter.hasNext());
            assertEquals('3', iter.nextChar());
            assertFalse(iter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iter::nextChar);

            stream.close();
            assertEquals(Arrays.asList("closeA", "closeC"), log);

            // the same stream twice: the second occurrence is rejected when it is reached
            final CharStream twice = CharStream.of('1', '2');
            final List<Character> collected = new ArrayList<>();
            final IllegalStateException repeated = assertThrows(IllegalStateException.class, () -> CharStream.concat(twice, twice).forEach(collected::add));
            assertEquals("This stream is already terminated.", repeated.getMessage());
            assertEquals(Arrays.asList('1', '2'), collected);

            // a terminal operation that fails on a closed input closes every other input
            final List<String> log2 = new ArrayList<>();
            final CharStream x = CharStream.of('1').onClose(() -> log2.add("closeX"));
            final CharStream closed2 = CharStream.of('2');
            closed2.close();
            final CharStream z = CharStream.of('3').onClose(() -> log2.add("closeZ"));
            assertThrows(IllegalStateException.class, () -> CharStream.concat(x, closed2, z).toArray());
            assertEquals(Arrays.asList("closeX", "closeZ"), log2);

            // a failing close handler of a not yet exhausted input still runs when the concatenation is closed
            final List<String> log3 = new ArrayList<>();
            final CharStream p = CharStream.of('1', '2').onClose(() -> log3.add("closeP"));
            final CharStream q = CharStream.of('3').onClose(() -> {
                log3.add("closeQ");
                throw new IllegalStateException("closeQ failed");
            });
            final CharStream r = CharStream.of('4').onClose(() -> log3.add("closeR"));
            final CharStream pqr = CharStream.concat(Arrays.asList(p, q, r));
            assertEquals('1', pqr.iteratorEx().nextChar());
            final IllegalStateException onClose = assertThrows(IllegalStateException.class, pqr::close);
            assertEquals("closeQ failed", onClose.getMessage());
            assertEquals(Arrays.asList("closeP", "closeQ", "closeR"), log3);
        }

        // G092-07: parallel append/concat (the concatenation's iterator is shared by the workers) delivers every element and closes every input
        @Test
        void testConcat_parallelDeliversAllElementsAndClosesInputs() {
            for (int round = 0; round < 20; round++) {
                final AtomicInteger closes = new AtomicInteger();
                final List<CharStream> inputs = new ArrayList<>();
                final List<Character> expected = new ArrayList<>();

                for (int i = 0; i < 50; i++) {
                    final char[] values = new char[i % 7];

                    for (int j = 0; j < values.length; j++) {
                        values[j] = (char) ('A' + i + j);
                    }

                    if (i % 10 == 3) {
                        inputs.add(null);
                    } else {
                        inputs.add(CharStream.of(values).onClose(closes::incrementAndGet));

                        for (final char v : values) {
                            expected.add(v);
                        }
                    }
                }

                final char[] actual = CharStream.concat(inputs).parallel(4).map(v -> (char) (v + 1)).toArray();
                final char[] sortedExpected = new char[expected.size()];

                for (int i = 0; i < sortedExpected.length; i++) {
                    sortedExpected[i] = (char) (expected.get(i) + 1);
                }

                Arrays.sort(sortedExpected);
                Arrays.sort(actual);
                assertArrayEquals(sortedExpected, actual);
                assertEquals(45, closes.get());

                final AtomicInteger closes2 = new AtomicInteger();
                final char[] appended = CharStream.of('1', '2', '3')
                        .onClose(closes2::incrementAndGet)
                        .parallel(3)
                        .append(CharStream.of('4', '5').onClose(closes2::incrementAndGet))
                        .toArray();
                Arrays.sort(appended);
                assertArrayEquals(new char[] { '1', '2', '3', '4', '5' }, appended);
                assertEquals(2, closes2.get());
            }
        }
    }
}
