package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.PermutationIterator;
import com.landawn.abacus.util.Profiler;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.stream.Stream.WindowHandler;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u;

/** Stream general contracts. */
public class StreamTest extends StreamTestSupport {

    @Test
    public void test_02() {
        N.println(0 - Long.MIN_VALUE);
        N.println(-1 - Long.MIN_VALUE);
        N.println(Long.MAX_VALUE);
        N.println(Long.MAX_VALUE * 1D + 10);
        N.println(Double.MAX_VALUE - Long.MAX_VALUE);
        assertFalse((double) Long.MAX_VALUE + 2 - Long.MAX_VALUE > 0);

        assertEquals(Integer.MAX_VALUE, ((int) (Integer.MAX_VALUE + 0.9999999999999999999D)));
        assertEquals(Long.MAX_VALUE, ((long) (Long.MAX_VALUE + 0.999999999999999D)));

        N.println(Integer.MAX_VALUE - Integer.MIN_VALUE);
        N.println(Integer.MAX_VALUE * 1L - Integer.MIN_VALUE);

        double d = 100;
        for (int i = 0; i < 100; i++) {
            N.println(d--);
        }

        N.println(Long.MAX_VALUE - Long.MIN_VALUE);
        N.println(Long.MAX_VALUE);
        N.println(Long.MIN_VALUE);
        assertEquals(Long.MAX_VALUE, (long) ((Long.MAX_VALUE * 1D - Long.MIN_VALUE) + Long.MIN_VALUE));
        N.println(String.format("%s", Long.MAX_VALUE * 1D - Long.MIN_VALUE));
        assertTrue(N.equals(new int[] { 1, 2, 3, 4, 5 }, IntStream.range(1, 6).toArray()));
        assertTrue(N.equals(new int[] { 1, 2, 3, 4, 5 }, IntStream.rangeClosed(1, 5).toArray()));
        assertTrue(N.equals(new int[] { 1, 3, 5 }, IntStream.range(1, 6, 2).toArray()));
        assertTrue(N.equals(new int[] { 1, 3, 5 }, IntStream.rangeClosed(1, 5, 2).toArray()));
        assertTrue(N.equals(new int[] { 1, 1, 1 }, IntStream.repeat(1, 3).toArray()));

        assertTrue(N.equals(Integer.MAX_VALUE, IntStream.range(0, Integer.MAX_VALUE).count()));
        assertTrue(N.equals(Integer.MAX_VALUE + 1L, IntStream.rangeClosed(0, Integer.MAX_VALUE).count()));
        assertTrue(N.equals(Integer.MAX_VALUE, IntStream.repeat(0, Integer.MAX_VALUE).count()));
    }

    @Test
    public void test_03() {
        assertEquals(Character.MAX_VALUE, CharStream.range(Character.MIN_VALUE, Character.MAX_VALUE).count());
        assertEquals(Character.MAX_VALUE + 1, CharStream.rangeClosed(Character.MIN_VALUE, Character.MAX_VALUE).count());

        assertEquals(Byte.MAX_VALUE * 2 + 1, ByteStream.range(Byte.MIN_VALUE, Byte.MAX_VALUE).count());
        assertEquals(Byte.MAX_VALUE * 2 + 2, ByteStream.rangeClosed(Byte.MIN_VALUE, Byte.MAX_VALUE).count());

        assertEquals(Short.MAX_VALUE * 2 + 1, ShortStream.range(Short.MIN_VALUE, Short.MAX_VALUE).count());
        assertEquals(Short.MAX_VALUE * 2 + 2, ShortStream.rangeClosed(Short.MIN_VALUE, Short.MAX_VALUE).count());

        assertEquals(Character.MAX_VALUE, Array.range(Character.MIN_VALUE, Character.MAX_VALUE).length);
        assertEquals(Character.MAX_VALUE + 1, Array.rangeClosed(Character.MIN_VALUE, Character.MAX_VALUE).length);

        assertEquals(Byte.MAX_VALUE * 2 + 1, Array.range(Byte.MIN_VALUE, Byte.MAX_VALUE).length);
        assertEquals(Byte.MAX_VALUE * 2 + 2, Array.rangeClosed(Byte.MIN_VALUE, Byte.MAX_VALUE).length);

        assertEquals(Short.MAX_VALUE * 2 + 1, Array.range(Short.MIN_VALUE, Short.MAX_VALUE).length);
        assertEquals(Short.MAX_VALUE * 2 + 2, Array.rangeClosed(Short.MIN_VALUE, Short.MAX_VALUE).length);
    }

    @Test
    public void test_04() {
        Iterator<List<String>> iter = PermutationIterator.of(N.toList("a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.of(N.toList("a", "b"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.of(N.toList("a", "a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.of(N.toList("a", "b", "c"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.of(N.toList("a", "b", "a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.of(new ArrayList<String>());
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        iter = PermutationIterator.ordered(N.toList("a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.toList("a", "b"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.toList("a", "a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.toList("a", "b", "c"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.toList("a", "b", "a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(new ArrayList<String>());
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        iter = PermutationIterator.of(N.toList("1", "2", "2", "1"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.toList("a", "b", "c"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.toList("1", "2", "2", "1"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        iter = PermutationIterator.ordered(N.toList("a", "b", "a"));
        while (iter.hasNext()) {
            N.println(iter.next());
        }

        N.println("=========================================================");

        final Consumer<? super List<Character>> consumer = N::println;

        CharStream.rangeClosed('a', 'e').boxed().permutations().forEach(consumer);
        assertNotNull(consumer);
    }

    // append

    @Test
    public void testStreamCreatedAfterAppend() {
        assertEquals(5, Stream.of(1, 2, 3).append(4, 5).count());
        assertEquals(4, Stream.of(1, 2, 3).append(4, 5).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3).append(4, 5).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3).append(4, 5).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3).append(4, 5).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3).append(4, 5).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3).map(e -> e).append(4, 5).count());
        assertEquals(4, Stream.of(1, 2, 3).map(e -> e).append(4, 5).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).append(4, 5).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).append(4, 5).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3).map(e -> e).append(4, 5).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3).map(e -> e).append(4, 5).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendCollection() {
        assertEquals(5, Stream.of(1, 2, 3).append(Arrays.asList(4, 5)).count());
        assertEquals(4, Stream.of(1, 2, 3).append(Arrays.asList(4, 5)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3).append(Arrays.asList(4, 5)).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3).append(Arrays.asList(4, 5)).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3).append(Arrays.asList(4, 5)).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3).append(Arrays.asList(4, 5)).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3).map(e -> e).append(Arrays.asList(4, 5)).count());
        assertEquals(4, Stream.of(1, 2, 3).map(e -> e).append(Arrays.asList(4, 5)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).append(Arrays.asList(4, 5)).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).append(Arrays.asList(4, 5)).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3).map(e -> e).append(Arrays.asList(4, 5)).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3).map(e -> e).append(Arrays.asList(4, 5)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendVarargs() {
        assertEquals(5, Stream.of(1, 2, 3).append(4, 5).count());
        assertEquals(3, Stream.of(1, 2, 3).append(4, 5).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3).append(4, 5).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3).append(4, 5).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3).append(4, 5).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3).append(4, 5).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3).map(e -> e).append(4, 5).count());
        assertEquals(3, Stream.of(1, 2, 3).map(e -> e).append(4, 5).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).append(4, 5).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).append(4, 5).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3).map(e -> e).append(4, 5).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3).map(e -> e).append(4, 5).skip(2).toList());
    }

    // appendIfEmpty

    @Test
    public void testStreamCreatedAfterAppendIfEmpty() {
        assertEquals(3, Stream.<Integer> empty().appendIfEmpty(1, 2, 3).count());
        assertEquals(2, Stream.<Integer> empty().appendIfEmpty(1, 2, 3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.<Integer> empty().appendIfEmpty(1, 2, 3).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.<Integer> empty().appendIfEmpty(1, 2, 3).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.<Integer> empty().appendIfEmpty(1, 2, 3).toList());
        assertEquals(N.toList(2, 3), Stream.<Integer> empty().appendIfEmpty(1, 2, 3).skip(1).toList());
        assertEquals(3, Stream.<Integer> empty().map(e -> e).appendIfEmpty(1, 2, 3).count());
        assertEquals(2, Stream.<Integer> empty().map(e -> e).appendIfEmpty(1, 2, 3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.<Integer> empty().map(e -> e).appendIfEmpty(1, 2, 3).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.<Integer> empty().map(e -> e).appendIfEmpty(1, 2, 3).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.<Integer> empty().map(e -> e).appendIfEmpty(1, 2, 3).toList());
        assertEquals(N.toList(2, 3), Stream.<Integer> empty().map(e -> e).appendIfEmpty(1, 2, 3).skip(1).toList());

        assertEquals(2, Stream.of(4, 5).appendIfEmpty(1, 2, 3).count());
        assertEquals(1, Stream.of(4, 5).appendIfEmpty(1, 2, 3).skip(1).count());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(4, 5).appendIfEmpty(1, 2, 3).toArray());
        assertArrayEquals(new Integer[] { 5 }, Stream.of(4, 5).appendIfEmpty(1, 2, 3).skip(1).toArray());
        assertEquals(N.toList(4, 5), Stream.of(4, 5).appendIfEmpty(1, 2, 3).toList());
        assertEquals(N.toList(5), Stream.of(4, 5).appendIfEmpty(1, 2, 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmptyCollection() {
        assertEquals(3, Stream.<Integer> empty().appendIfEmpty(Arrays.asList(1, 2, 3)).count());
        assertEquals(2, Stream.<Integer> empty().appendIfEmpty(Arrays.asList(1, 2, 3)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.<Integer> empty().appendIfEmpty(Arrays.asList(1, 2, 3)).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.<Integer> empty().appendIfEmpty(Arrays.asList(1, 2, 3)).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.<Integer> empty().appendIfEmpty(Arrays.asList(1, 2, 3)).toList());
        assertEquals(N.toList(2, 3), Stream.<Integer> empty().appendIfEmpty(Arrays.asList(1, 2, 3)).skip(1).toList());
        assertEquals(3, Stream.<Integer> empty().map(e -> e).appendIfEmpty(Arrays.asList(1, 2, 3)).count());
        assertEquals(2, Stream.<Integer> empty().map(e -> e).appendIfEmpty(Arrays.asList(1, 2, 3)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.<Integer> empty().map(e -> e).appendIfEmpty(Arrays.asList(1, 2, 3)).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.<Integer> empty().map(e -> e).appendIfEmpty(Arrays.asList(1, 2, 3)).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.<Integer> empty().map(e -> e).appendIfEmpty(Arrays.asList(1, 2, 3)).toList());
        assertEquals(N.toList(2, 3), Stream.<Integer> empty().map(e -> e).appendIfEmpty(Arrays.asList(1, 2, 3)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmptyVarargs() {
        assertEquals(3, Stream.of(1, 2, 3).appendIfEmpty(4, 5).count());
        assertEquals(2, Stream.of(1, 2, 3).appendIfEmpty(4, 5).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, 2, 3).appendIfEmpty(4, 5).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, 2, 3).appendIfEmpty(4, 5).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of(1, 2, 3).appendIfEmpty(4, 5).toList());
        assertEquals(N.toList(2, 3), Stream.of(1, 2, 3).appendIfEmpty(4, 5).skip(1).toList());

        assertEquals(2, Stream.<Integer> empty().appendIfEmpty(4, 5).count());
        assertEquals(1, Stream.<Integer> empty().appendIfEmpty(4, 5).skip(1).count());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.<Integer> empty().appendIfEmpty(4, 5).toArray());
        assertArrayEquals(new Integer[] { 5 }, Stream.<Integer> empty().appendIfEmpty(4, 5).skip(1).toArray());
        assertEquals(N.toList(4, 5), Stream.<Integer> empty().appendIfEmpty(4, 5).toList());
        assertEquals(N.toList(5), Stream.<Integer> empty().appendIfEmpty(4, 5).skip(1).toList());
    }

    @Test
    public void test_ArrayDeque() {
        assertDoesNotThrow(() -> {
            final ArrayDeque<Integer> queue = new ArrayDeque<>(10);

            for (int i = 0; i < 16; i++) {
                if (queue.size() > 10) {
                    queue.removeFirst();
                }

                queue.add(i);
                N.println(queue);
            }
        });
    }

    // buffered

    @Test
    public void testStreamCreatedAfterBuffered() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).buffered().count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).buffered().skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).buffered().toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).buffered().skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).buffered().toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).buffered().skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered().count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered().skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered().toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered().skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered().toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered().skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterBufferedWithQueue() {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).buffered(queue).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).buffered(queue).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).buffered(queue).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).buffered(queue).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).buffered(queue).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).buffered(queue).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(queue).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(queue).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(queue).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(queue).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(queue).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(queue).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterBufferedWithSize() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).buffered(10).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).buffered(10).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).buffered(10).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).buffered(10).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).buffered(10).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).buffered(10).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(10).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(10).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(10).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(10).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(10).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).buffered(10).skip(2).toList());
    }

    @Test
    public void testcallAsync() throws Exception {
        com.landawn.abacus.util.ContinuableFuture<Integer> future = Stream.of(1, 2, 3).callAsync(s -> s.reduce(0, Integer::sum));
        assertEquals(Integer.valueOf(6), future.get());
    }

    @Test
    public void testComplexNestedOperations() {
        List<String> result = createStream(Arrays.asList(1, 2, 3, 4, 5)).flatMap(n -> createStream(n, n * 10))
                .filter(n -> n % 2 == 0)
                .map(n -> "Value: " + n)
                .distinct()
                .sorted()
                .toList();

        assertEquals(Arrays.asList("Value: 10", "Value: 2", "Value: 20", "Value: 30", "Value: 4", "Value: 40", "Value: 50"), result);
    }

    @Test
    public void testComplexStreamOperations() {
        List<String> result = Stream.range(1, 10).filter(x -> x % 2 == 0).map(x -> "Number: " + x).toList();

        assertEquals(Arrays.asList("Number: 2", "Number: 4", "Number: 6", "Number: 8"), result);
    }

    @Test
    public void testComplexStreamPipeline() {
        List<String> result = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).filter(n -> n % 2 == 0).map(n -> n * 2).sorted().limit(3).map(String::valueOf).toList();

        assertEquals(Arrays.asList("4", "8", "12"), result);
    }

    @Test
    public void testPerformance_LargeConcat() {
        List<Stream<Integer>> manyStreams = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            final int base = i * 10;
            manyStreams.add(Stream.range(base, base + 10));
        }

        long count = Stream.concat(manyStreams).count();
        assertEquals(1000, count);
    }

    @Test
    public void testStreamCreatedAfterCycled() {
        assertEquals(15, Stream.of(1, 2, 3).cycled().limit(15).count());
        assertEquals(14, Stream.of(1, 2, 3).cycled().limit(15).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 1, 2, 3, 1, 2, 3, 1 }, Stream.of(1, 2, 3).cycled().limit(10).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 1, 2, 3, 1, 2, 3, 1 }, Stream.of(1, 2, 3).cycled().limit(10).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 1, 2, 3), Stream.of(1, 2, 3).cycled().limit(6).toList());
        assertEquals(N.toList(2, 3, 1, 2, 3), Stream.of(1, 2, 3).cycled().limit(6).skip(1).toList());
        assertEquals(15, Stream.of(1, 2, 3).map(e -> e).cycled().limit(15).count());
        assertEquals(14, Stream.of(1, 2, 3).map(e -> e).cycled().limit(15).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 1, 2, 3, 1, 2, 3, 1 }, Stream.of(1, 2, 3).map(e -> e).cycled().limit(10).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 1, 2, 3, 1, 2, 3, 1 }, Stream.of(1, 2, 3).map(e -> e).cycled().limit(10).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 1, 2, 3), Stream.of(1, 2, 3).map(e -> e).cycled().limit(6).toList());
        assertEquals(N.toList(2, 3, 1, 2, 3), Stream.of(1, 2, 3).map(e -> e).cycled().limit(6).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCycledWithRounds() {
        assertEquals(6, Stream.of(1, 2, 3).cycled(2).count());
        assertEquals(5, Stream.of(1, 2, 3).cycled(2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 1, 2, 3 }, Stream.of(1, 2, 3).cycled(2).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 1, 2, 3 }, Stream.of(1, 2, 3).cycled(2).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 1, 2, 3), Stream.of(1, 2, 3).cycled(2).toList());
        assertEquals(N.toList(2, 3, 1, 2, 3), Stream.of(1, 2, 3).cycled(2).skip(1).toList());
        assertEquals(6, Stream.of(1, 2, 3).map(e -> e).cycled(2).count());
        assertEquals(5, Stream.of(1, 2, 3).map(e -> e).cycled(2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 1, 2, 3 }, Stream.of(1, 2, 3).map(e -> e).cycled(2).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 1, 2, 3 }, Stream.of(1, 2, 3).map(e -> e).cycled(2).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 1, 2, 3), Stream.of(1, 2, 3).map(e -> e).cycled(2).toList());
        assertEquals(N.toList(2, 3, 1, 2, 3), Stream.of(1, 2, 3).map(e -> e).cycled(2).skip(1).toList());
    }

    @Test
    public void testDefaultIfEmpty() {
        List<Integer> result1 = Stream.<Integer> empty().defaultIfEmpty(() -> Stream.of(1, 2, 3)).toList();
        assertEquals(Arrays.asList(1, 2, 3), result1);

        List<Integer> result2 = Stream.of(1, 2).defaultIfEmpty(() -> Stream.of(3, 4)).toList();
        assertEquals(Arrays.asList(1, 2), result2);
    }

    @Test
    public void testStreamCreatedAfterDefaultIfEmpty() {
        assertEquals(1, Stream.<Integer> empty().defaultIfEmpty(99).count());
        assertEquals(0, Stream.<Integer> empty().defaultIfEmpty(99).skip(1).count());
        assertArrayEquals(new Integer[] { 99 }, Stream.<Integer> empty().defaultIfEmpty(99).toArray());
        assertArrayEquals(new Integer[] {}, Stream.<Integer> empty().defaultIfEmpty(99).skip(1).toArray());
        assertEquals(N.toList(99), Stream.<Integer> empty().defaultIfEmpty(99).toList());
        assertEquals(N.toList(), Stream.<Integer> empty().defaultIfEmpty(99).skip(1).toList());
        assertEquals(1, Stream.<Integer> empty().map(e -> e).defaultIfEmpty(99).count());
        assertEquals(0, Stream.<Integer> empty().map(e -> e).defaultIfEmpty(99).skip(1).count());
        assertArrayEquals(new Integer[] { 99 }, Stream.<Integer> empty().map(e -> e).defaultIfEmpty(99).toArray());
        assertArrayEquals(new Integer[] {}, Stream.<Integer> empty().map(e -> e).defaultIfEmpty(99).skip(1).toArray());
        assertEquals(N.toList(99), Stream.<Integer> empty().map(e -> e).defaultIfEmpty(99).toList());
        assertEquals(N.toList(), Stream.<Integer> empty().map(e -> e).defaultIfEmpty(99).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDifference() {
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2, 3, 4)).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2, 3, 4)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 5 }, Stream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2, 3, 4)).toArray());
        assertArrayEquals(new Integer[] { 5 }, Stream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2, 3, 4)).skip(1).toArray());
        assertEquals(N.toList(1, 5), Stream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2, 3, 4)).toList());
        assertEquals(N.toList(5), Stream.of(1, 2, 3, 4, 5).difference(Arrays.asList(2, 3, 4)).skip(1).toList());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2, 3, 4)).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2, 3, 4)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2, 3, 4)).toArray());
        assertArrayEquals(new Integer[] { 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2, 3, 4)).skip(1).toArray());
        assertEquals(N.toList(1, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2, 3, 4)).toList());
        assertEquals(N.toList(5), Stream.of(1, 2, 3, 4, 5).map(e -> e).difference(Arrays.asList(2, 3, 4)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDistinct() {
        assertEquals(4, Stream.of(1, 2, 2, 3, 3, 4).distinct().count());
        assertEquals(3, Stream.of(1, 2, 2, 3, 3, 4).distinct().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, Stream.of(1, 2, 2, 3, 3, 4).distinct().toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4 }, Stream.of(1, 2, 2, 3, 3, 4).distinct().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4), Stream.of(1, 2, 2, 3, 3, 4).distinct().toList());
        assertEquals(N.toList(2, 3, 4), Stream.of(1, 2, 2, 3, 3, 4).distinct().skip(1).toList());
        assertEquals(4, Stream.of(1, 2, 2, 3, 3, 4).map(e -> e).distinct().count());
        assertEquals(3, Stream.of(1, 2, 2, 3, 3, 4).map(e -> e).distinct().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, Stream.of(1, 2, 2, 3, 3, 4).map(e -> e).distinct().toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4 }, Stream.of(1, 2, 2, 3, 3, 4).map(e -> e).distinct().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4), Stream.of(1, 2, 2, 3, 3, 4).map(e -> e).distinct().toList());
        assertEquals(N.toList(2, 3, 4), Stream.of(1, 2, 2, 3, 3, 4).map(e -> e).distinct().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhile() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toList());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).toList());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhileWithAction() {
        List<Integer> dropped = new ArrayList<>();
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).count());
        dropped.clear();
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).toList());
        dropped.clear();
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3, dropped::add).skip(1).toList());
        dropped.clear();
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).count());
        dropped.clear();
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).toList());
        dropped.clear();
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).toList());
    }

    @Test
    public void testExceptionPropagation() {
        Iterator<Integer> failingIterator = new Iterator<>() {
            private int count = 0;

            @Override
            public boolean hasNext() {
                if (count == 3) {
                    throw new RuntimeException("Iterator failed");
                }
                return count < 5;
            }

            @Override
            public Integer next() {
                return count++;
            }
        };

        Stream<Integer> stream = createStream(failingIterator);

        try {
            stream.toList();
            fail("Expected exception");
        } catch (RuntimeException e) {
            assertEquals("Iterator failed", e.getMessage());
        }
    }

    @Test
    public void testStreamCreatedAfterFilter() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2).toList());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).toList());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFilterWithAction() {
        List<Integer> dropped = new ArrayList<>();
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).count());
        dropped.clear();
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).toList());
        dropped.clear();
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).filter(i -> i > 2, dropped::add).skip(1).toList());
        dropped.clear();
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).count());
        dropped.clear();
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).toList());
        dropped.clear();
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMap() {
        assertEquals(6, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatMap(Stream::of).count());
        assertEquals(5, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatMap(Stream::of).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatMap(Stream::of).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatMap(Stream::of).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6), Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatMap(Stream::of).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6), Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatMap(Stream::of).skip(1).toList());
        assertEquals(6, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatMap(Stream::of).count());
        assertEquals(5, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatMap(Stream::of).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatMap(Stream::of).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatMap(Stream::of).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatMap(Stream::of).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatMap(Stream::of).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmap() {
        assertEquals(6, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatmap(Fn.identity()).count());
        assertEquals(5, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatmap(Fn.identity()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatmap(Fn.identity()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatmap(Fn.identity()).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6), Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatmap(Fn.identity()).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6), Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flatmap(Fn.identity()).skip(1).toList());
        assertEquals(6, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatmap(Fn.identity()).count());
        assertEquals(5, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatmap(Fn.identity()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatmap(Fn.identity()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatmap(Fn.identity()).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatmap(Fn.identity()).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flatmap(Fn.identity()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmapIfNotNull() {
        assertEquals(4, Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).flatmapIfNotNull(Fn.identity()).count());
        assertEquals(3, Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).flatmapIfNotNull(Fn.identity()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).flatmapIfNotNull(Fn.identity()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4 },
                Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).flatmapIfNotNull(Fn.identity()).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4), Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).flatmapIfNotNull(Fn.identity()).toList());
        assertEquals(N.toList(2, 3, 4), Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).flatmapIfNotNull(Fn.identity()).skip(1).toList());
        assertEquals(4, Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).map(e -> e).flatmapIfNotNull(Fn.identity()).count());
        assertEquals(3, Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).map(e -> e).flatmapIfNotNull(Fn.identity()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 },
                Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).map(e -> e).flatmapIfNotNull(Fn.identity()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4 },
                Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).map(e -> e).flatmapIfNotNull(Fn.identity()).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4), Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).map(e -> e).flatmapIfNotNull(Fn.identity()).toList());
        assertEquals(N.toList(2, 3, 4), Stream.of(Arrays.asList(1, 2), null, Arrays.asList(3, 4)).map(e -> e).flatmapIfNotNull(Fn.identity()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmapIfNotNull2() {
        assertEquals(8,
                Stream.of("a", null, "b").flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2")).count());
        assertEquals(7,
                Stream.of("a", null, "b")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .skip(1)
                        .count());
        assertArrayEquals(new String[] { "A", "A2", "A1", "A12", "B", "B2", "B1", "B12" },
                Stream.of("a", null, "b")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .toArray());
        assertArrayEquals(new String[] { "A2", "A1", "A12", "B", "B2", "B1", "B12" },
                Stream.of("a", null, "b")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList("A", "A2", "A1", "A12", "B", "B2", "B1", "B12"),
                Stream.of("a", null, "b")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .toList());
        assertEquals(N.toList("A2", "A1", "A12", "B", "B2", "B1", "B12"),
                Stream.of("a", null, "b")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .skip(1)
                        .toList());
        assertEquals(8,
                Stream.of("a", null, "b")
                        .map(e -> e)
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .count());
        assertEquals(7,
                Stream.of("a", null, "b")
                        .map(e -> e)
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .skip(1)
                        .count());
        assertArrayEquals(new String[] { "A", "A2", "A1", "A12", "B", "B2", "B1", "B12" },
                Stream.of("a", null, "b")
                        .map(e -> e)
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .toArray());
        assertArrayEquals(new String[] { "A2", "A1", "A12", "B", "B2", "B1", "B12" },
                Stream.of("a", null, "b")
                        .map(e -> e)
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList("A", "A2", "A1", "A12", "B", "B2", "B1", "B12"),
                Stream.of("a", null, "b")
                        .map(e -> e)
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .toList());
        assertEquals(N.toList("A2", "A1", "A12", "B", "B2", "B1", "B12"),
                Stream.of("a", null, "b")
                        .map(e -> e)
                        .flatmapIfNotNull(s -> Arrays.asList(s, s + "1"), s -> Arrays.asList(s.toUpperCase(), s.toUpperCase() + "2"))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testHasDuplicates() {
        assertTrue(Stream.of(1, 2, 2, 3).containsDuplicates());
        assertFalse(Stream.of(1, 2, 3).containsDuplicates());
        assertFalse(Stream.<Integer> empty().containsDuplicates());
    }

    // ifEmpty

    @Test
    public void testIfEmpty() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Stream.empty().ifEmpty(() -> executed.set(true)).count();
        assertTrue(executed.get());
    }

    @Test
    public void testIfEmptyNonEmpty() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Stream.of(1, 2, 3).ifEmpty(() -> executed.set(true)).count();
        assertFalse(executed.get());
    }

    @Test
    public void testStreamCreatedAfterIfEmpty() {
        AtomicBoolean flag = new AtomicBoolean(false);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).ifEmpty(() -> flag.set(true)).count());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).ifEmpty(() -> flag.set(true)).skip(1).count());
        assertFalse(flag.get());
        flag.set(false);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).ifEmpty(() -> flag.set(true)).toArray());
        assertFalse(flag.get());
        flag.set(false);
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).ifEmpty(() -> flag.set(true)).skip(1).toArray());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).ifEmpty(() -> flag.set(true)).toList());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).ifEmpty(() -> flag.set(true)).skip(1).toList());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> flag.set(true)).count());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> flag.set(true)).skip(1).count());
        assertFalse(flag.get());
        flag.set(false);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> flag.set(true)).toArray());
        assertFalse(flag.get());
        flag.set(false);
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> flag.set(true)).skip(1).toArray());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> flag.set(true)).toList());
        assertFalse(flag.get());
        flag.set(false);
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).ifEmpty(() -> flag.set(true)).skip(1).toList());
        assertFalse(flag.get());
    }

    @Test
    public void testIntegration_AllFactoryMethods() {

        assertEquals(0, Stream.empty().count());

        assertEquals(1, Stream.just("test").count());

        assertEquals(0, Stream.ofNullable(null).count());
        assertEquals(1, Stream.ofNullable("test").count());

        assertEquals(3, Stream.defer(() -> createStream(1, 2, 3)).count());

        assertEquals(3, createStream01(new Integer[] { 1, 2, 3 }).count());
        assertEquals(3, createStream(Arrays.asList(1, 2, 3)).count());
        assertEquals(3, createStream(Arrays.asList(1, 2, 3).iterator()).count());

        assertEquals(5, Stream.range(0, 5).count());
        assertEquals(6, Stream.rangeClosed(0, 5).count());

        assertEquals(10, Stream.repeat("X", 10).count());

        assertEquals(5, Stream.generate(() -> "Y").limit(5).count());

        assertEquals(5, Stream.iterate(1, x -> x + 1).limit(5).count());
    }

    @Test
    public void testStreamCreatedAfterIntersection() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2, 3, 4, 6)).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2, 3, 4, 6)).skip(1).count());
        assertArrayEquals(new Integer[] { 2, 3, 4 }, Stream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2, 3, 4, 6)).toArray());
        assertArrayEquals(new Integer[] { 3, 4 }, Stream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2, 3, 4, 6)).skip(1).toArray());
        assertEquals(N.toList(2, 3, 4), Stream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2, 3, 4, 6)).toList());
        assertEquals(N.toList(3, 4), Stream.of(1, 2, 3, 4, 5).intersection(Arrays.asList(2, 3, 4, 6)).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2, 3, 4, 6)).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2, 3, 4, 6)).skip(1).count());
        assertArrayEquals(new Integer[] { 2, 3, 4 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2, 3, 4, 6)).toArray());
        assertArrayEquals(new Integer[] { 3, 4 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2, 3, 4, 6)).skip(1).toArray());
        assertEquals(N.toList(2, 3, 4), Stream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2, 3, 4, 6)).toList());
        assertEquals(N.toList(3, 4), Stream.of(1, 2, 3, 4, 5).map(e -> e).intersection(Arrays.asList(2, 3, 4, 6)).skip(1).toList());
    }

    @Test
    public void testIsParallel() {
        assertFalse(Stream.of(1, 2, 3).isParallel());
        assertTrue(Stream.of(1, 2, 3).parallel().isParallel());
    }

    @Test
    public void testBooleanSupplierIteratorsStayExhausted() {
        final AtomicInteger conditionCalls = new AtomicInteger();
        Iterator<Integer> iter = Stream.iterate(() -> conditionCalls.getAndIncrement() > 0, () -> 1).iterator();

        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
        assertEquals(1, conditionCalls.get());

        conditionCalls.set(0);
        iter = Stream.iterate(1, () -> conditionCalls.getAndIncrement() > 0, value -> value + 1).iterator();

        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
        assertEquals(1, conditionCalls.get());
    }

    // just

    @Test
    public void testJust() {
        Stream<String> stream = Stream.just("hello");
        assertEquals("hello", stream.first().get());
    }

    @Test
    public void testJust2() {
        Stream<String> stream = Stream.just("hello");
        assertEquals(Arrays.asList("hello"), stream.toList());
    }

    @Test
    public void testJust_NullElement() {
        Stream<String> stream = Stream.just(null);

        List<String> result = stream.toList();
        assertEquals(1, result.size());
        assertNull(result.get(0));
    }

    @Test
    public void testLargeDataHandling() {
        int size = 100000;

        long sum = Stream.range(0, size).mapToLong(i -> i).sum();

        long expectedSum = ((long) size * (size - 1)) / 2;
        assertEquals(expectedSum, sum);
    }

    @Test
    @Timeout(10)
    public void testLargeDataset_Performance() throws Exception {
        int size = 10000;
        List<Integer> data = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            data.add(i);
        }
        AtomicInteger mainCount = new AtomicInteger(0);
        AtomicInteger subscriberCount = new AtomicInteger(0);

        Stream.of(data.toArray(new Integer[0])).addSubscriber(s -> {
            s.forEach(x -> subscriberCount.incrementAndGet());
        }).forEach(x -> {
            mainCount.incrementAndGet();
            if (mainCount.get() == size) {
            }
        });
        assertEquals(size, mainCount.get());
        assertEquals(size, subscriberCount.get());
    }

    @Test
    public void test_lazyEvalation() throws Exception {
        final MutableBoolean moved = MutableBoolean.of(false);

        Stream<String> s = Stream.of("a", "b", "c", "d").peek(Fn.println()).peek(it -> {
            new RuntimeException("Testing lazy evaluation").printStackTrace();
            moved.setTrue();
        })
                .parallel()
                .map(it -> it + "111")
                .buffered()
                .shuffled()
                .sorted()
                .rotated(2)
                .filter(Fn.alwaysTrue())
                .flatMap(Stream::of)
                .takeWhile(Fn.alwaysTrue())
                .dropWhile(Fn.alwaysFalse())
                .sequential()
                .map(it -> it + "111")
                .buffered()
                .shuffled()
                .sorted()
                .rotated(2)
                .filter(Fn.alwaysTrue())
                .flatMap(Stream::of)
                .takeWhile(Fn.alwaysTrue())
                .dropWhile(Fn.alwaysFalse());

        assertFalse(moved.value());

        s = s.filter(Fn.notNull());

        assertFalse(moved.value());

        s = s.buffered();

        assertFalse(moved.value());

        final ObjIterator<String> iter = s.iterator();

        assertFalse(moved.value());

        s.forEach(Fn.println());

        assertTrue(moved.value());

        assertFalse(iter.hasNext());

        moved.setFalse();
        final List<ByteStream> list = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            list.add(ByteStream.range((byte) 0, (byte) i).peek(it -> moved.setTrue()).peek(N::println));
        }

        ByteStream bs = ByteStream.merge(list, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);

        bs = bs.filter(it -> it < 10);

        assertFalse(moved.value());

        final ByteIterator byteIter = bs.iterator();

        bs.forEach(N::println);

        assertFalse(byteIter.hasNext());

        moved.setFalse();

        final IntStream is = Stream.range(0, 10_000)
                .peek(it -> moved.setTrue())
                .parallel(128)
                .map(it -> it - 0)
                .map(it -> it + 0)
                .flatMap(Stream::of)
                .map(it -> it + 1)
                .map(it -> it - 1)
                .filter(it -> it >= 0)
                .buffered(1024)
                .peek(it -> {
                    if (it % 37 == 0) {
                        N.sleep(3);
                    }
                })
                .map(it -> it - 0)
                .map(it -> it + 0)
                .flatMap(Stream::of)
                .map(it -> it + 1)
                .sequential()
                .map(it -> it - 1)
                .filter(it -> it >= 0)
                .buffered(1024)
                .reverseSorted()
                .peek(it -> {
                    if (it % 79 == 0) {
                        N.sleep(3);
                    }
                })
                .map(it -> it - 0)
                .map(it -> it + 0)
                .flatMap(Stream::of)
                .map(it -> it + 1)
                .map(it -> it - 1)
                .filter(it -> it >= 0)
                .buffered(1024)
                .peek(it -> {
                    if (it % 137 == 0) {
                        N.sleep(3);
                    }
                })
                .map(it -> it - 0)
                .parallel(64)
                .map(it -> it + 0)
                .flatMap(Stream::of)
                .map(it -> it + 1)
                .map(it -> it - 1)
                .filter(it -> it >= 0)
                .buffered(137)
                .peek(it -> {
                    if (it % 537 == 0) {
                        N.sleep(3);
                    }
                })
                .mapToInt(ToIntFunction.UNBOX);

        assertFalse(moved.value());

        final IntIterator ii = is.iterator();

        assertFalse(moved.value());

        assertEquals(IntStream.range(0, 10_000).sum(), is.sum());

        assertFalse(ii.hasNext());
    }

    @Test
    public void testStreamCreatedAfterLimit() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).limit(3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).limit(3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).limit(3).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, 2, 3, 4, 5).limit(3).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of(1, 2, 3, 4, 5).limit(3).toList());
        assertEquals(N.toList(2, 3), Stream.of(1, 2, 3, 4, 5).limit(3).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).toList());
        assertEquals(N.toList(2, 3), Stream.of(1, 2, 3, 4, 5).map(e -> e).limit(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlattmap() {
        assertEquals(6, Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).flatMapArray(Fn.identity()).count());
        assertEquals(5, Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).flatMapArray(Fn.identity()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).flatMapArray(Fn.identity()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).flatMapArray(Fn.identity()).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).flatMapArray(Fn.identity()).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6),
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).flatMapArray(Fn.identity()).skip(1).toList());
        assertEquals(6, Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).map(e -> e).flatMapArray(Fn.identity()).count());
        assertEquals(5,
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).map(e -> e).flatMapArray(Fn.identity()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).map(e -> e).flatMapArray(Fn.identity()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).map(e -> e).flatMapArray(Fn.identity()).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).map(e -> e).flatMapArray(Fn.identity()).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6),
                Stream.of(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 }).map(e -> e).flatMapArray(Fn.identity()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMap() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(x -> x * 2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(x -> x * 2).skip(1).count());
        assertArrayEquals(new Integer[] { 2, 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).map(x -> x * 2).toArray());
        assertArrayEquals(new Integer[] { 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).map(x -> x * 2).skip(1).toArray());
        assertEquals(N.toList(2, 4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).map(x -> x * 2).toList());
        assertEquals(N.toList(4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).map(x -> x * 2).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).map(x -> x * 2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).map(x -> x * 2).skip(1).count());
        assertArrayEquals(new Integer[] { 2, 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).map(x -> x * 2).toArray());
        assertArrayEquals(new Integer[] { 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).map(x -> x * 2).skip(1).toArray());
        assertEquals(N.toList(2, 4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).map(e -> e).map(x -> x * 2).toList());
        assertEquals(N.toList(4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).map(e -> e).map(x -> x * 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapFirst() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).mapFirst(x -> x * 10).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).mapFirst(x -> x * 10).skip(1).count());
        assertArrayEquals(new Integer[] { 10, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).mapFirst(x -> x * 10).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).mapFirst(x -> x * 10).skip(1).toArray());
        assertEquals(N.toList(10, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).mapFirst(x -> x * 10).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).mapFirst(x -> x * 10).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirst(x -> x * 10).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirst(x -> x * 10).skip(1).count());
        assertArrayEquals(new Integer[] { 10, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirst(x -> x * 10).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirst(x -> x * 10).skip(1).toArray());
        assertEquals(N.toList(10, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirst(x -> x * 10).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirst(x -> x * 10).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapFirstOrElse() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).mapFirstOrElse(x -> x * 10, x -> x * 2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).mapFirstOrElse(x -> x * 10, x -> x * 2).skip(1).count());
        assertArrayEquals(new Integer[] { 10, 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).mapFirstOrElse(x -> x * 10, x -> x * 2).toArray());
        assertArrayEquals(new Integer[] { 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).mapFirstOrElse(x -> x * 10, x -> x * 2).skip(1).toArray());
        assertEquals(N.toList(10, 4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).mapFirstOrElse(x -> x * 10, x -> x * 2).toList());
        assertEquals(N.toList(4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).mapFirstOrElse(x -> x * 10, x -> x * 2).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirstOrElse(x -> x * 10, x -> x * 2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirstOrElse(x -> x * 10, x -> x * 2).skip(1).count());
        assertArrayEquals(new Integer[] { 10, 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirstOrElse(x -> x * 10, x -> x * 2).toArray());
        assertArrayEquals(new Integer[] { 4, 6, 8, 10 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirstOrElse(x -> x * 10, x -> x * 2).skip(1).toArray());
        assertEquals(N.toList(10, 4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirstOrElse(x -> x * 10, x -> x * 2).toList());
        assertEquals(N.toList(4, 6, 8, 10), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapFirstOrElse(x -> x * 10, x -> x * 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapIfNotNull() {
        assertEquals(3, Stream.of("a", null, "b", null, "c").mapIfNotNull(String::toUpperCase).count());
        assertEquals(2, Stream.of("a", null, "b", null, "c").mapIfNotNull(String::toUpperCase).skip(1).count());
        assertArrayEquals(new String[] { "A", "B", "C" }, Stream.of("a", null, "b", null, "c").mapIfNotNull(String::toUpperCase).toArray());
        assertArrayEquals(new String[] { "B", "C" }, Stream.of("a", null, "b", null, "c").mapIfNotNull(String::toUpperCase).skip(1).toArray());
        assertEquals(N.toList("A", "B", "C"), Stream.of("a", null, "b", null, "c").mapIfNotNull(String::toUpperCase).toList());
        assertEquals(N.toList("B", "C"), Stream.of("a", null, "b", null, "c").mapIfNotNull(String::toUpperCase).skip(1).toList());
        assertEquals(3, Stream.of("a", null, "b", null, "c").map(e -> e).mapIfNotNull(String::toUpperCase).count());
        assertEquals(2, Stream.of("a", null, "b", null, "c").map(e -> e).mapIfNotNull(String::toUpperCase).skip(1).count());
        assertArrayEquals(new String[] { "A", "B", "C" }, Stream.of("a", null, "b", null, "c").map(e -> e).mapIfNotNull(String::toUpperCase).toArray());
        assertArrayEquals(new String[] { "B", "C" }, Stream.of("a", null, "b", null, "c").map(e -> e).mapIfNotNull(String::toUpperCase).skip(1).toArray());
        assertEquals(N.toList("A", "B", "C"), Stream.of("a", null, "b", null, "c").map(e -> e).mapIfNotNull(String::toUpperCase).toList());
        assertEquals(N.toList("B", "C"), Stream.of("a", null, "b", null, "c").map(e -> e).mapIfNotNull(String::toUpperCase).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapLast() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).mapLast(x -> x * 10).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).mapLast(x -> x * 10).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 50 }, Stream.of(1, 2, 3, 4, 5).mapLast(x -> x * 10).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 50 }, Stream.of(1, 2, 3, 4, 5).mapLast(x -> x * 10).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 50), Stream.of(1, 2, 3, 4, 5).mapLast(x -> x * 10).toList());
        assertEquals(N.toList(2, 3, 4, 50), Stream.of(1, 2, 3, 4, 5).mapLast(x -> x * 10).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLast(x -> x * 10).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLast(x -> x * 10).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 50 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLast(x -> x * 10).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 50 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLast(x -> x * 10).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 50), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLast(x -> x * 10).toList());
        assertEquals(N.toList(2, 3, 4, 50), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLast(x -> x * 10).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapLastOrElse() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).mapLastOrElse(x -> x * 10, x -> x * 2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).mapLastOrElse(x -> x * 10, x -> x * 2).skip(1).count());
        assertArrayEquals(new Integer[] { 2, 4, 6, 8, 50 }, Stream.of(1, 2, 3, 4, 5).mapLastOrElse(x -> x * 10, x -> x * 2).toArray());
        assertArrayEquals(new Integer[] { 4, 6, 8, 50 }, Stream.of(1, 2, 3, 4, 5).mapLastOrElse(x -> x * 10, x -> x * 2).skip(1).toArray());
        assertEquals(N.toList(2, 4, 6, 8, 50), Stream.of(1, 2, 3, 4, 5).mapLastOrElse(x -> x * 10, x -> x * 2).toList());
        assertEquals(N.toList(4, 6, 8, 50), Stream.of(1, 2, 3, 4, 5).mapLastOrElse(x -> x * 10, x -> x * 2).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLastOrElse(x -> x * 10, x -> x * 2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLastOrElse(x -> x * 10, x -> x * 2).skip(1).count());
        assertArrayEquals(new Integer[] { 2, 4, 6, 8, 50 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLastOrElse(x -> x * 10, x -> x * 2).toArray());
        assertArrayEquals(new Integer[] { 4, 6, 8, 50 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLastOrElse(x -> x * 10, x -> x * 2).skip(1).toArray());
        assertEquals(N.toList(2, 4, 6, 8, 50), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLastOrElse(x -> x * 10, x -> x * 2).toList());
        assertEquals(N.toList(4, 6, 8, 50), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapLastOrElse(x -> x * 10, x -> x * 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapMulti() {
        assertEquals(10, Stream.of(1, 2, 3, 4, 5).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).count());
        assertEquals(9, Stream.of(1, 2, 3, 4, 5).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 }, Stream.of(1, 2, 3, 4, 5).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).toArray());
        assertArrayEquals(new Integer[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 }, Stream.of(1, 2, 3, 4, 5).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).skip(1).toArray());
        assertEquals(N.toList(1, 10, 2, 20, 3, 30, 4, 40, 5, 50), Stream.of(1, 2, 3, 4, 5).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).toList());
        assertEquals(N.toList(10, 2, 20, 3, 30, 4, 40, 5, 50), Stream.of(1, 2, 3, 4, 5).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).skip(1).toList());
        assertEquals(10, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).count());
        assertEquals(9, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).toArray());
        assertArrayEquals(new Integer[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).skip(1).toArray());
        assertEquals(N.toList(1, 10, 2, 20, 3, 30, 4, 40, 5, 50), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).toList());
        assertEquals(N.toList(10, 2, 20, 3, 30, 4, 40, 5, 50), Stream.of(1, 2, 3, 4, 5).map(e -> e).mapMulti((e, consumer) -> {
            consumer.accept(e);
            consumer.accept(e * 10);
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartial() {
        assertEquals(3, Stream.of("1", "a", "2", "b", "3").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).count());
        assertEquals(2, Stream.of("1", "a", "2", "b", "3").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of("1", "a", "2", "b", "3").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of("1", "a", "2", "b", "3").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of("1", "a", "2", "b", "3").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).toList());
        assertEquals(N.toList(2, 3), Stream.of("1", "a", "2", "b", "3").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).skip(1).toList());
        assertEquals(3, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).count());
        assertEquals(2, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).toList());
        assertEquals(N.toList(2, 3), Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartialJdk() {
        assertEquals(3, Stream.of("1", "a", "2", "b", "3").mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).count());
        assertEquals(2, Stream.of("1", "a", "2", "b", "3").mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of("1", "a", "2", "b", "3").mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of("1", "a", "2", "b", "3").mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of("1", "a", "2", "b", "3").mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).toList());
        assertEquals(N.toList(2, 3), Stream.of("1", "a", "2", "b", "3").mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).skip(1).toList());
        assertEquals(3, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).count());
        assertEquals(2, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).toList());
        assertEquals(N.toList(2, 3), Stream.of("1", "a", "2", "b", "3").map(e -> e).mapPartialJdk(s -> {
            try {
                return java.util.Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMaxWaitWithDefaultValue() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).maxWait(Duration.ofMillis(100), 99).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).maxWait(Duration.ofMillis(100), 99).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).maxWait(Duration.ofMillis(100), 99).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).maxWait(Duration.ofMillis(100), 99).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).maxWait(Duration.ofMillis(100), 99).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).maxWait(Duration.ofMillis(100), 99).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).maxWait(Duration.ofMillis(100), 99).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).maxWait(Duration.ofMillis(100), 99).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).maxWait(Duration.ofMillis(100), 99).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).maxWait(Duration.ofMillis(100), 99).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).maxWait(Duration.ofMillis(100), 99).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).maxWait(Duration.ofMillis(100), 99).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterMaxWaitWithSupplier() {
        Supplier<Integer> defaultSupplier = () -> 99;

        assertEquals(5, Stream.of(1, 2, 3, 4, 5).maxWait(Duration.ofMillis(100), defaultSupplier).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).maxWait(Duration.ofMillis(100), defaultSupplier).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).maxWait(Duration.ofMillis(100), defaultSupplier).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).maxWait(Duration.ofMillis(100), defaultSupplier).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).maxWait(Duration.ofMillis(100), defaultSupplier).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).maxWait(Duration.ofMillis(100), defaultSupplier).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).maxWait(Duration.ofMillis(100), defaultSupplier).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).maxWait(Duration.ofMillis(100), defaultSupplier).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).maxWait(Duration.ofMillis(100), defaultSupplier).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).maxWait(Duration.ofMillis(100), defaultSupplier).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).maxWait(Duration.ofMillis(100), defaultSupplier).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).maxWait(Duration.ofMillis(100), defaultSupplier).skip(2).toList());
    }

    @Test
    public void testMemoryEfficientOperations() {
        AtomicInteger generatedCount = new AtomicInteger(0);

        Stream<Integer> infiniteStream = Stream.generate(() -> generatedCount.incrementAndGet());

        List<Integer> result = infiniteStream.limit(5).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

        assertTrue(generatedCount.get() <= 10);
    }

    @Test
    public void testPrimitiveMergeNullSelectorThrowsOnConsumption() {
        assertThrows(IllegalArgumentException.class, () -> ByteStream.merge(new byte[] { 1 }, new byte[] { 2 }, null).count());
        assertThrows(IllegalArgumentException.class, () -> CharStream.merge(new char[] { 'a' }, new char[] { 'b' }, null).count());
        assertThrows(IllegalArgumentException.class, () -> ShortStream.merge(new short[] { 1 }, new short[] { 2 }, null).count());
        assertThrows(IllegalArgumentException.class, () -> IntStream.merge(new int[] { 1 }, new int[] { 2 }, null).count());
        assertThrows(IllegalArgumentException.class, () -> LongStream.merge(new long[] { 1 }, new long[] { 2 }, null).count());
        assertThrows(IllegalArgumentException.class, () -> FloatStream.merge(new float[] { 1 }, new float[] { 2 }, null).count());
        assertThrows(IllegalArgumentException.class, () -> DoubleStream.merge(new double[] { 1 }, new double[] { 2 }, null).count());

        assertThrows(IllegalArgumentException.class, () -> ByteStream.merge(ByteIterator.of((byte) 1), ByteIterator.of((byte) 2), null).count());
        assertThrows(IllegalArgumentException.class, () -> ByteStream.merge(ByteStream.of((byte) 1), ByteStream.of((byte) 2), null).count());
        assertThrows(IllegalArgumentException.class, () -> ByteStream.merge(N.asList(ByteStream.of((byte) 1), ByteStream.of((byte) 2)), null).count());
    }

    @Test
    public void testReferenceArrayMergeNullSelectorThrowsOnConsumption() {
        assertThrows(IllegalArgumentException.class, () -> Stream.merge(new Integer[] { 1 }, new Integer[] { 2 }, null).count());
        assertThrows(IllegalArgumentException.class, () -> Stream.merge(new Integer[] { 1 }, new Integer[] { 2 }, new Integer[] { 3 }, null).count());
    }

    @Test
    public void testStreamCreatedAfterMergeWithCollection() {
        List<Integer> collection = Arrays.asList(2, 4, 6);
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        assertEquals(6, Stream.of(1, 3, 5).mergeWith(collection, selector).count());
        assertEquals(4, Stream.of(1, 3, 5).mergeWith(collection, selector).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 }, Stream.of(1, 3, 5).mergeWith(collection, selector).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5, 6 }, Stream.of(1, 3, 5).mergeWith(collection, selector).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6), Stream.of(1, 3, 5).mergeWith(collection, selector).toList());
        assertEquals(N.toList(3, 4, 5, 6), Stream.of(1, 3, 5).mergeWith(collection, selector).skip(2).toList());
        assertEquals(6, Stream.of(1, 3, 5).map(e -> e).mergeWith(collection, selector).count());
        assertEquals(4, Stream.of(1, 3, 5).map(e -> e).mergeWith(collection, selector).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 }, Stream.of(1, 3, 5).map(e -> e).mergeWith(collection, selector).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5, 6 }, Stream.of(1, 3, 5).map(e -> e).mergeWith(collection, selector).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6), Stream.of(1, 3, 5).map(e -> e).mergeWith(collection, selector).toList());
        assertEquals(N.toList(3, 4, 5, 6), Stream.of(1, 3, 5).map(e -> e).mergeWith(collection, selector).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterMergeWithStream() {
        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        assertEquals(6, Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), selector).count());
        assertEquals(4, Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), selector).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 }, Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), selector).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5, 6 }, Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), selector).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6), Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), selector).toList());
        assertEquals(N.toList(3, 4, 5, 6), Stream.of(1, 3, 5).mergeWith(Stream.of(2, 4, 6), selector).skip(2).toList());
        assertEquals(6, Stream.of(1, 3, 5).map(e -> e).mergeWith(Stream.of(2, 4, 6), selector).count());
        assertEquals(4, Stream.of(1, 3, 5).map(e -> e).mergeWith(Stream.of(2, 4, 6), selector).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 }, Stream.of(1, 3, 5).map(e -> e).mergeWith(Stream.of(2, 4, 6), selector).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5, 6 }, Stream.of(1, 3, 5).map(e -> e).mergeWith(Stream.of(2, 4, 6), selector).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6), Stream.of(1, 3, 5).map(e -> e).mergeWith(Stream.of(2, 4, 6), selector).toList());
        assertEquals(N.toList(3, 4, 5, 6), Stream.of(1, 3, 5).map(e -> e).mergeWith(Stream.of(2, 4, 6), selector).skip(2).toList());
    }

    @Test
    public void testNMatch() {
        assertTrue(Stream.of(2, 4, 6, 8).hasMatchCountBetween(2, 3, n -> n > 5));
        assertFalse(Stream.of(2, 4, 6, 8).hasMatchCountBetween(5, 10, n -> n > 5));
    }

    @Test
    @Timeout(10)
    public void testNullElementHandling_InFilters() throws Exception {
        Stream.of(1, null, 3, null, 5).filterWhileAddSubscriber(x -> x != null && x % 2 == 1, s -> {
            s.forEach(subscriberStreamResults::add);
        }).forEach(x -> {
            mainStreamResults.add(x);
        });

        assertEquals(Arrays.asList(1, 3, 5), mainStreamResults);
        assertEquals(Arrays.asList(null, null), subscriberStreamResults);
    }

    @Test
    public void testNullFunctionalArgumentsRejectedEagerly() {
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).filter((Predicate<Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).filter(value -> true, (Consumer<Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> Stream.defer(null));
    }

    @Test
    public void testNullHandling() {
        List<Integer> withNulls = Arrays.asList(1, null, 2, null, 3);

        List<Integer> result = Stream.of(withNulls).filter(n -> n != null).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testNullSafety() {
        List<String> withNulls = Arrays.asList("a", null, "b", null, "c");

        assertEquals(5, createStream(withNulls).count());

        List<String> nonNulls = createStream(withNulls).filter(Objects::nonNull).toList();
        assertEquals(Arrays.asList("a", "b", "c"), nonNulls);

        List<Integer> lengths = createStream(withNulls).map(s -> s == null ? -1 : s.length()).toList();
        assertEquals(Arrays.asList(1, -1, 1, -1, 1), lengths);
    }

    @Test
    public void testStreamCreatedAfterOnFirst() {
        List<Integer> firstElements = new ArrayList<>();
        Consumer<Integer> captureFirst = firstElements::add;

        assertEquals(5, Stream.of(1, 2, 3, 4, 5).onFirst(captureFirst).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).onFirst(captureFirst).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).onFirst(captureFirst).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).onFirst(captureFirst).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).onFirst(captureFirst).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).onFirst(captureFirst).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).onFirst(captureFirst).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).onFirst(captureFirst).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).onFirst(captureFirst).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).onFirst(captureFirst).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).onFirst(captureFirst).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).onFirst(captureFirst).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterOnLast() {
        List<Integer> lastElements = new ArrayList<>();
        Consumer<Integer> captureLast = lastElements::add;

        assertEquals(5, Stream.of(1, 2, 3, 4, 5).onLast(captureLast).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).onLast(captureLast).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).onLast(captureLast).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).onLast(captureLast).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).onLast(captureLast).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).onLast(captureLast).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).onLast(captureLast).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).onLast(captureLast).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).onLast(captureLast).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).onLast(captureLast).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).onLast(captureLast).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).onLast(captureLast).skip(2).toList());
    }

    @Test
    public void testOrderedPermutations() {
        List<List<Integer>> result = Stream.of(1, 2, 3).orderedPermutations().toList();
        assertEquals(6, result.size());
    }

    // pairWith

    @Test
    public void testPairWith() {
        List<Pair<String, Integer>> result = Stream.of("a", "bb", "ccc").pairWith(String::length).toList();
        assertEquals(Pair.of("a", 1), result.get(0));
        assertEquals(Pair.of("bb", 2), result.get(1));
        assertEquals(Pair.of("ccc", 3), result.get(2));
    }

    @Test
    public void testPairWith_Empty() {
        List<Pair<String, Integer>> result = Stream.<String> empty().pairWith(String::length).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testStreamCreatedAfterPairWith() {
        assertEquals(3, Stream.of("a", "bb", "ccc").pairWith(String::length).count());
        assertEquals(2, Stream.of("a", "bb", "ccc").pairWith(String::length).skip(1).count());
        assertEquals(3, Stream.of("a", "bb", "ccc").pairWith(String::length).toArray().length);
        assertEquals(2, Stream.of("a", "bb", "ccc").pairWith(String::length).skip(1).toArray().length);
        assertEquals(3, Stream.of("a", "bb", "ccc").pairWith(String::length).toList().size());
        assertEquals(2, Stream.of("a", "bb", "ccc").pairWith(String::length).skip(1).toList().size());
        assertEquals(3, Stream.of("a", "bb", "ccc").map(e -> e).pairWith(String::length).count());
        assertEquals(2, Stream.of("a", "bb", "ccc").map(e -> e).pairWith(String::length).skip(1).count());
        assertEquals(3, Stream.of("a", "bb", "ccc").map(e -> e).pairWith(String::length).toArray().length);
        assertEquals(2, Stream.of("a", "bb", "ccc").map(e -> e).pairWith(String::length).skip(1).toArray().length);
        assertEquals(3, Stream.of("a", "bb", "ccc").map(e -> e).pairWith(String::length).toList().size());
        assertEquals(2, Stream.of("a", "bb", "ccc").map(e -> e).pairWith(String::length).skip(1).toList().size());
    }

    @Test
    public void testComplexScenarios_NestedParallelism() {
        List<Integer> result = Stream.range(0, 10)
                .parallel()
                .flatMap(i -> Stream.range(i * 10, (i + 1) * 10).parallel())
                .filter(n -> n % 3 == 0)
                .sorted()
                .toList();

        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < 100; i += 3) {
            expected.add(i);
        }
        assertEquals(expected, result);
    }

    @Test
    public void test_exception_parallel() {
        try {
            Stream.range(0, 10).forEach(e -> {
                throw new IOException("oooh");
            });
            fail("Should throw IOException");
        } catch (IOException e) {
        }

        try {
            Stream.range(0, 10).peek(e -> {
                throw new UncheckedIOException(new IOException("oooh"));
            }).forEach(Fn.println());
            fail("Should throw UncheckedIOException");
        } catch (UncheckedIOException e) {
        }

        try {
            Stream.range(0, 10).parallel().forEach(e -> {
                throw new IOException("oooh");
            });
            fail("Should throw IOException");
        } catch (IOException e) {
        }

        try {
            Stream.range(0, 10).parallel().peek(e -> {
                throw new UncheckedIOException(new IOException("oooh"));
            }).forEach(Fn.println());
            fail("Should throw UncheckedIOException");
        } catch (UncheckedIOException e) {
        }

    }

    @Test
    public void testStreamCreatedAfterPeek() {
        List<Integer> list = new ArrayList<>();
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).peek(list::add).count());
        list.clear();
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).peek(list::add).skip(1).count());
        list.clear();
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peek(list::add).toArray());
        list.clear();
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peek(list::add).skip(1).toArray());
        list.clear();
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).peek(list::add).toList());
        list.clear();
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).peek(list::add).skip(1).toList());
        list.clear();
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).count());
        list.clear();
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).skip(1).count());
        list.clear();
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).toArray());
        list.clear();
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).skip(1).toArray());
        list.clear();
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).toList());
        list.clear();
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPeekFirst() {
        List<Integer> firstElements = new ArrayList<>();
        Consumer<Integer> captureFirst = firstElements::add;

        assertEquals(5, Stream.of(1, 2, 3, 4, 5).peekFirst(captureFirst).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).peekFirst(captureFirst).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekFirst(captureFirst).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekFirst(captureFirst).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekFirst(captureFirst).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekFirst(captureFirst).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekFirst(captureFirst).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekFirst(captureFirst).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekFirst(captureFirst).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekFirst(captureFirst).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekFirst(captureFirst).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekFirst(captureFirst).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterPeekIfWithBiPredicate() {
        List<Integer> elements = new ArrayList<>();
        BiPredicate<Integer, Long> isSecondElement = (e, count) -> count == 2L;
        Consumer<Integer> capture = elements::add;

        assertEquals(5, Stream.of(1, 2, 3, 4, 5).peekIf(isSecondElement, capture).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).peekIf(isSecondElement, capture).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekIf(isSecondElement, capture).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekIf(isSecondElement, capture).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekIf(isSecondElement, capture).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekIf(isSecondElement, capture).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isSecondElement, capture).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isSecondElement, capture).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isSecondElement, capture).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isSecondElement, capture).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isSecondElement, capture).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isSecondElement, capture).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterPeekIfWithPredicate() {
        List<Integer> evenElements = new ArrayList<>();
        Predicate<Integer> isEven = i -> i % 2 == 0;
        Consumer<Integer> captureEven = evenElements::add;

        assertEquals(5, Stream.of(1, 2, 3, 4, 5).peekIf(isEven, captureEven).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).peekIf(isEven, captureEven).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekIf(isEven, captureEven).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekIf(isEven, captureEven).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekIf(isEven, captureEven).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekIf(isEven, captureEven).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isEven, captureEven).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isEven, captureEven).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isEven, captureEven).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isEven, captureEven).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isEven, captureEven).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekIf(isEven, captureEven).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterPeekLast() {
        List<Integer> lastElements = new ArrayList<>();
        Consumer<Integer> captureLast = lastElements::add;

        assertEquals(5, Stream.of(1, 2, 3, 4, 5).peekLast(captureLast).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).peekLast(captureLast).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekLast(captureLast).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).peekLast(captureLast).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekLast(captureLast).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).peekLast(captureLast).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekLast(captureLast).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekLast(captureLast).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekLast(captureLast).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peekLast(captureLast).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekLast(captureLast).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peekLast(captureLast).skip(2).toList());
    }

    @Test
    public void test_perf() {
        final String[] strs = new String[10_000];
        N.fill(strs, Strings.uuid());

        final int m = 500;
        final Function<String, Long> mapper = str -> {
            long result = 0;
            for (int i = 0; i < m; i++) {
                result += N.sum(str.toCharArray()) + 1;
            }
            return result;
        };

        long tmp = 0;

        for (final String str : strs) {
            tmp += mapper.apply(str);
        }

        final long sum = tmp;

        Profiler.run(1, 10, 1, "For loop", () -> {
            long result = 0;

            for (final String str : strs) {
                result += mapper.apply(str);
            }

            assertEquals(sum, result);
        }).printResult();

        Profiler.run(1, 10, 1, "Abacus sequential", () -> assertEquals(sum, Stream.of(strs).map(mapper).mapToLong(t -> t).sum())).printResult();

        Profiler.run(1, 10, 1, "Abacus parallel", () -> assertEquals(sum, Stream.of(strs).parallel().map(mapper).mapToLong(t -> t).sum())).printResult();
    }

    // prepend

    @Test
    public void testStreamCreatedAfterPrepend() {
        assertEquals(5, Stream.of(3, 4, 5).prepend(1, 2).count());
        assertEquals(4, Stream.of(3, 4, 5).prepend(1, 2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(3, 4, 5).prepend(1, 2).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(3, 4, 5).prepend(1, 2).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(3, 4, 5).prepend(1, 2).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(3, 4, 5).prepend(1, 2).skip(1).toList());
        assertEquals(5, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).count());
        assertEquals(4, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependCollection() {
        assertEquals(5, Stream.of(3, 4, 5).prepend(Arrays.asList(1, 2)).count());
        assertEquals(4, Stream.of(3, 4, 5).prepend(Arrays.asList(1, 2)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(3, 4, 5).prepend(Arrays.asList(1, 2)).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(3, 4, 5).prepend(Arrays.asList(1, 2)).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(3, 4, 5).prepend(Arrays.asList(1, 2)).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(3, 4, 5).prepend(Arrays.asList(1, 2)).skip(1).toList());
        assertEquals(5, Stream.of(3, 4, 5).map(e -> e).prepend(Arrays.asList(1, 2)).count());
        assertEquals(4, Stream.of(3, 4, 5).map(e -> e).prepend(Arrays.asList(1, 2)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(3, 4, 5).map(e -> e).prepend(Arrays.asList(1, 2)).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(3, 4, 5).map(e -> e).prepend(Arrays.asList(1, 2)).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(3, 4, 5).map(e -> e).prepend(Arrays.asList(1, 2)).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(3, 4, 5).map(e -> e).prepend(Arrays.asList(1, 2)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependVarargs() {
        assertEquals(5, Stream.of(3, 4, 5).prepend(1, 2).count());
        assertEquals(3, Stream.of(3, 4, 5).prepend(1, 2).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(3, 4, 5).prepend(1, 2).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(3, 4, 5).prepend(1, 2).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(3, 4, 5).prepend(1, 2).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(3, 4, 5).prepend(1, 2).skip(2).toList());
        assertEquals(5, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).count());
        assertEquals(3, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).skip(2).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).toArray());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).skip(2).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).toList());
        assertEquals(N.toList(3, 4, 5), Stream.of(3, 4, 5).map(e -> e).prepend(1, 2).skip(2).toList());
    }

    @Test
    public void testPrintln() {
        assertDoesNotThrow(() -> {
            Stream.of(1, 2, 3).println();

            Stream.range(0, 1000).println();

            Stream.range(0, 1001).println();
        });
    }

    @Test
    public void testStreamCreatedAfterRangeMap() {
        assertEquals(3, Stream.of(1, 2, 5, 6, 10).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).count());
        assertEquals(2, Stream.of(1, 2, 5, 6, 10).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).skip(1).count());
        assertArrayEquals(new String[] { "1-2", "5-6", "10-10" },
                Stream.of(1, 2, 5, 6, 10).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).toArray());
        assertArrayEquals(new String[] { "5-6", "10-10" },
                Stream.of(1, 2, 5, 6, 10).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).skip(1).toArray());
        assertEquals(N.toList("1-2", "5-6", "10-10"),
                Stream.of(1, 2, 5, 6, 10).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).toList());
        assertEquals(N.toList("5-6", "10-10"),
                Stream.of(1, 2, 5, 6, 10).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).skip(1).toList());
        assertEquals(3,
                Stream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).count());
        assertEquals(2,
                Stream.of(1, 2, 5, 6, 10)
                        .map(e -> e)
                        .rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last)
                        .skip(1)
                        .count());
        assertArrayEquals(new String[] { "1-2", "5-6", "10-10" },
                Stream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).toArray());
        assertArrayEquals(new String[] { "5-6", "10-10" },
                Stream.of(1, 2, 5, 6, 10)
                        .map(e -> e)
                        .rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last)
                        .skip(1)
                        .toArray());
        assertEquals(N.toList("1-2", "5-6", "10-10"),
                Stream.of(1, 2, 5, 6, 10).map(e -> e).rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last).toList());
        assertEquals(N.toList("5-6", "10-10"),
                Stream.of(1, 2, 5, 6, 10)
                        .map(e -> e)
                        .rangeMap((first, next) -> Math.abs(first - next) <= 1, (first, last) -> first + "-" + last)
                        .skip(1)
                        .toList());
    }

    @Test
    public void testEdgeCases_VeryLargeRepeat() {
        long veryLarge = Long.MAX_VALUE / 2;

        Stream<String> repeated = Stream.repeat("X", veryLarge);

        List<String> first10 = repeated.limit(10).toList();
        assertEquals(10, first10.size());
        assertTrue(first10.stream().allMatch("X"::equals));
    }

    @Test
    public void testStreamCreatedAfterReversed() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).reversed().count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).reversed().skip(1).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reversed().toArray());
        assertArrayEquals(new Integer[] { 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reversed().skip(1).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).reversed().toList());
        assertEquals(N.toList(4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).reversed().skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).reversed().count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toArray());
        assertArrayEquals(new Integer[] { 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reversed().toList());
        assertEquals(N.toList(4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reversed().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSorted() {
        assertEquals(5, Stream.of(3, 1, 4, 2, 5).reverseSorted().count());
        assertEquals(4, Stream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).reverseSorted().toArray());
        assertArrayEquals(new Integer[] { 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).reverseSorted().toList());
        assertEquals(N.toList(4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).reverseSorted().skip(1).toList());
        assertEquals(5, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().count());
        assertEquals(4, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().toArray());
        assertArrayEquals(new Integer[] { 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().toList());
        assertEquals(N.toList(4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSortedByComparator() {
        assertEquals(5, Stream.of(3, 1, 4, 2, 5).reverseSorted(Comparator.naturalOrder()).count());
        assertEquals(4, Stream.of(3, 1, 4, 2, 5).reverseSorted(Comparator.naturalOrder()).skip(1).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).reverseSorted(Comparator.naturalOrder()).toArray());
        assertArrayEquals(new Integer[] { 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).reverseSorted(Comparator.naturalOrder()).skip(1).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).reverseSorted(Comparator.naturalOrder()).toList());
        assertEquals(N.toList(4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).reverseSorted(Comparator.naturalOrder()).skip(1).toList());
        assertEquals(5, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted(Comparator.naturalOrder()).count());
        assertEquals(4, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted(Comparator.naturalOrder()).skip(1).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted(Comparator.naturalOrder()).toArray());
        assertArrayEquals(new Integer[] { 4, 3, 2, 1 }, Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted(Comparator.naturalOrder()).skip(1).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted(Comparator.naturalOrder()).toList());
        assertEquals(N.toList(4, 3, 2, 1), Stream.of(3, 1, 4, 2, 5).map(e -> e).reverseSorted(Comparator.naturalOrder()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSortedBy() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).reverseSortedBy(i -> i).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).reverseSortedBy(i -> i).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedBy(i -> i).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedBy(i -> i).skip(2).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedBy(i -> i).toList());
        assertEquals(N.toList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedBy(i -> i).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedBy(i -> i).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedBy(i -> i).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedBy(i -> i).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedBy(i -> i).skip(2).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedBy(i -> i).toList());
        assertEquals(N.toList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedBy(i -> i).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSortedByDouble() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).reverseSortedByDouble(i -> i.doubleValue()).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).reverseSortedByDouble(i -> i.doubleValue()).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedByDouble(i -> i.doubleValue()).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedByDouble(i -> i.doubleValue()).skip(2).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedByDouble(i -> i.doubleValue()).toList());
        assertEquals(N.toList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedByDouble(i -> i.doubleValue()).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByDouble(i -> i.doubleValue()).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByDouble(i -> i.doubleValue()).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByDouble(i -> i.doubleValue()).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByDouble(i -> i.doubleValue()).skip(2).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByDouble(i -> i.doubleValue()).toList());
        assertEquals(N.toList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByDouble(i -> i.doubleValue()).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSortedByInt() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).reverseSortedByInt(i -> i.intValue()).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).reverseSortedByInt(i -> i.intValue()).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedByInt(i -> i.intValue()).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedByInt(i -> i.intValue()).skip(2).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedByInt(i -> i.intValue()).toList());
        assertEquals(N.toList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedByInt(i -> i.intValue()).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByInt(i -> i.intValue()).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByInt(i -> i.intValue()).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByInt(i -> i.intValue()).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByInt(i -> i.intValue()).skip(2).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByInt(i -> i.intValue()).toList());
        assertEquals(N.toList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByInt(i -> i.intValue()).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSortedByLong() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).reverseSortedByLong(i -> i.longValue()).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).reverseSortedByLong(i -> i.longValue()).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedByLong(i -> i.longValue()).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).reverseSortedByLong(i -> i.longValue()).skip(2).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedByLong(i -> i.longValue()).toList());
        assertEquals(N.toList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).reverseSortedByLong(i -> i.longValue()).skip(2).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByLong(i -> i.longValue()).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByLong(i -> i.longValue()).skip(2).count());
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByLong(i -> i.longValue()).toArray());
        assertArrayEquals(new Integer[] { 3, 2, 1 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByLong(i -> i.longValue()).skip(2).toArray());
        assertEquals(N.toList(5, 4, 3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByLong(i -> i.longValue()).toList());
        assertEquals(N.toList(3, 2, 1), Stream.of(1, 2, 3, 4, 5).map(e -> e).reverseSortedByLong(i -> i.longValue()).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterRotated() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).rotated(2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).rotated(2).skip(1).count());
        assertArrayEquals(new Integer[] { 4, 5, 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).rotated(2).toArray());
        assertArrayEquals(new Integer[] { 5, 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toArray());
        assertEquals(N.toList(4, 5, 1, 2, 3), Stream.of(1, 2, 3, 4, 5).rotated(2).toList());
        assertEquals(N.toList(5, 1, 2, 3), Stream.of(1, 2, 3, 4, 5).rotated(2).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).count());
        assertArrayEquals(new Integer[] { 4, 5, 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toArray());
        assertArrayEquals(new Integer[] { 5, 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toArray());
        assertEquals(N.toList(4, 5, 1, 2, 3), Stream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).toList());
        assertEquals(N.toList(5, 1, 2, 3), Stream.of(1, 2, 3, 4, 5).map(e -> e).rotated(2).skip(1).toList());
    }

    @Test
    public void testrunAsync() throws Exception {
        AtomicInteger result = new AtomicInteger(0);
        com.landawn.abacus.util.ContinuableFuture<Void> future = Stream.of(1, 2, 3).runAsync(s -> s.forEach(result::addAndGet));
        future.get();
        assertEquals(6, result.get());
    }

    @Test
    public void testStreamCreatedAfterSelect() {
        assertEquals(3, Stream.<Object> of("a", 1, "b", 2, "c").select(String.class).count());
        assertEquals(2, Stream.<Object> of("a", 1, "b", 2, "c").select(String.class).skip(1).count());
        assertArrayEquals(new String[] { "a", "b", "c" }, Stream.<Object> of("a", 1, "b", 2, "c").select(String.class).toArray());
        assertArrayEquals(new String[] { "b", "c" }, Stream.<Object> of("a", 1, "b", 2, "c").select(String.class).skip(1).toArray());
        assertEquals(N.toList("a", "b", "c"), Stream.<Object> of("a", 1, "b", 2, "c").select(String.class).toList());
        assertEquals(N.toList("b", "c"), Stream.<Object> of("a", 1, "b", 2, "c").select(String.class).skip(1).toList());
        assertEquals(3, Stream.<Object> of("a", 1, "b", 2, "c").map(e -> e).select(String.class).count());
        assertEquals(2, Stream.<Object> of("a", 1, "b", 2, "c").map(e -> e).select(String.class).skip(1).count());
        assertArrayEquals(new String[] { "a", "b", "c" }, Stream.<Object> of("a", 1, "b", 2, "c").map(e -> e).select(String.class).toArray());
        assertArrayEquals(new String[] { "b", "c" }, Stream.<Object> of("a", 1, "b", 2, "c").map(e -> e).select(String.class).skip(1).toArray());
        assertEquals(N.toList("a", "b", "c"), Stream.<Object> of("a", 1, "b", 2, "c").map(e -> e).select(String.class).toList());
        assertEquals(N.toList("b", "c"), Stream.<Object> of("a", 1, "b", 2, "c").map(e -> e).select(String.class).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterShuffled() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).shuffled().count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).shuffled().skip(1).count());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).shuffled().toArray().length);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).shuffled().skip(1).toArray().length);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).shuffled().toList().size());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).shuffled().skip(1).toList().size());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).count());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().toArray().length);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).toArray().length);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().toList().size());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled().skip(1).toList().size());
    }

    @Test
    public void testStreamCreatedAfterShuffledWithRandom() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).skip(1).count());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).toArray().length);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).skip(1).toArray().length);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).toList().size());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).shuffled(new Random(42)).skip(1).toList().size());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).skip(1).count());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).toArray().length);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).skip(1).toArray().length);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).toList().size());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).shuffled(new Random(42)).skip(1).toList().size());
    }

    @Test
    public void testSingleElementStream() {
        Stream<Integer> single = Stream.of(42);

        assertEquals(1, single.count());
        assertEquals(Integer.valueOf(42), Stream.of(42).first().get());
        assertEquals(Integer.valueOf(42), Stream.of(42).last().get());
        assertEquals(Integer.valueOf(42), Stream.of(42).onlyOne().get());
    }

    @Test
    public void test_sjps() {
        {
            Stream<String> strings = createStream01("a", "bb", "ccc", "dd", "e");
            List<String> result = strings.sjps(s -> s.filter(it -> it.length() <= 1)).toList();
            assertHaveSameElements(Arrays.asList("a", "e"), result);
        }
        {
            Stream<String> strings = createStream01("a", "bb", "ccc", "dd", "e");
            List<String> result = strings.parallel().sjps(s -> s.filter(it -> it.length() <= 1)).toList();
            assertHaveSameElements(Arrays.asList("a", "e"), result);
        }
    }

    @Test
    public void testStreamCreatedAfterSkip() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).skip(2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).skip(2).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).skip(2).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).skip(2).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).skip(2).toList());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).skip(2).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).toList());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipWithAction() {
        List<Integer> skipped = new ArrayList<>();
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add).count());
        skipped.clear();
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add).skip(1).count());
        skipped.clear();
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add).toArray());
        skipped.clear();
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add).skip(1).toArray());
        skipped.clear();
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add).toList());
        skipped.clear();
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add).skip(1).toList());
        skipped.clear();
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).count());
        skipped.clear();
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).skip(1).count());
        skipped.clear();
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).toArray());
        skipped.clear();
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).skip(1).toArray());
        skipped.clear();
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).toList());
        skipped.clear();
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skip(2, skipped::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipLast() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).skipLast(2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).skipLast(2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).skipLast(2).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, 2, 3, 4, 5).skipLast(2).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of(1, 2, 3, 4, 5).skipLast(2).toList());
        assertEquals(N.toList(2, 3), Stream.of(1, 2, 3, 4, 5).skipLast(2).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipLast(2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipLast(2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipLast(2).toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipLast(2).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of(1, 2, 3, 4, 5).map(e -> e).skipLast(2).toList());
        assertEquals(N.toList(2, 3), Stream.of(1, 2, 3, 4, 5).map(e -> e).skipLast(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipNulls() {
        assertEquals(3, Stream.of(1, null, 2, null, 3).skipNulls().count());
        assertEquals(2, Stream.of(1, null, 2, null, 3).skipNulls().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, null, 2, null, 3).skipNulls().toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, null, 2, null, 3).skipNulls().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of(1, null, 2, null, 3).skipNulls().toList());
        assertEquals(N.toList(2, 3), Stream.of(1, null, 2, null, 3).skipNulls().skip(1).toList());
        assertEquals(3, Stream.of(1, null, 2, null, 3).map(e -> e).skipNulls().count());
        assertEquals(2, Stream.of(1, null, 2, null, 3).map(e -> e).skipNulls().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3 }, Stream.of(1, null, 2, null, 3).map(e -> e).skipNulls().toArray());
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(1, null, 2, null, 3).map(e -> e).skipNulls().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3), Stream.of(1, null, 2, null, 3).map(e -> e).skipNulls().toList());
        assertEquals(N.toList(2, 3), Stream.of(1, null, 2, null, 3).map(e -> e).skipNulls().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipRange() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 4, 5 }, Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).skip(1).toArray());
        assertEquals(N.toList(1, 4, 5), Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).toList());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).skipRange(1, 3).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipRange(1, 3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipRange(1, 3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipRange(1, 3).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipRange(1, 3).skip(1).toArray());
        assertEquals(N.toList(1, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skipRange(1, 3).toList());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skipRange(1, 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipUntil() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).toList());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).skipUntil(i -> i >= 3).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(N.toList(3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).toList());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toList());
    }

    // slidingMap

    @Test
    public void testStreamCreatedAfterSlidingMap() {
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 5, 7, 9 }, Stream.of(1, 2, 3, 4, 5).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toArray());
        assertArrayEquals(new Integer[] { 5, 7, 9 },
                Stream.of(1, 2, 3, 4, 5).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toArray());
        assertEquals(N.toList(3, 5, 7, 9), Stream.of(1, 2, 3, 4, 5).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toList());
        assertEquals(N.toList(5, 7, 9), Stream.of(1, 2, 3, 4, 5).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toList());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).count());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 5, 7, 9 },
                Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toArray());
        assertArrayEquals(new Integer[] { 5, 7, 9 },
                Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toArray());
        assertEquals(N.toList(3, 5, 7, 9), Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toList());
        assertEquals(N.toList(5, 7, 9), Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSlidingMapTri() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).count());
        assertArrayEquals(new Integer[] { 6, 9, 12 },
                Stream.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toArray());
        assertArrayEquals(new Integer[] { 9, 12 },
                Stream.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).toArray());
        assertEquals(N.toList(6, 9, 12),
                Stream.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toList());
        assertEquals(N.toList(9, 12),
                Stream.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).count());
        assertEquals(2,
                Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).count());
        assertArrayEquals(new Integer[] { 6, 9, 12 },
                Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toArray());
        assertArrayEquals(new Integer[] { 9, 12 },
                Stream.of(1, 2, 3, 4, 5)
                        .map(e -> e)
                        .slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList(6, 9, 12),
                Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toList());
        assertEquals(N.toList(9, 12),
                Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap((a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSlidingMapTriWithIncrement() {
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).count());
        assertEquals(1,
                Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).count());
        assertArrayEquals(new Integer[] { 6, 15 },
                Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toArray());
        assertArrayEquals(new Integer[] { 15 },
                Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).toArray());
        assertEquals(N.toList(6, 15),
                Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toList());
        assertEquals(N.toList(15),
                Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).skip(1).toList());
        assertEquals(2,
                Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).count());
        assertEquals(1,
                Stream.of(1, 2, 3, 4, 5, 6)
                        .map(e -> e)
                        .slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c))
                        .skip(1)
                        .count());
        assertArrayEquals(new Integer[] { 6, 15 },
                Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toArray());
        assertArrayEquals(new Integer[] { 15 },
                Stream.of(1, 2, 3, 4, 5, 6)
                        .map(e -> e)
                        .slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList(6, 15),
                Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c)).toList());
        assertEquals(N.toList(15),
                Stream.of(1, 2, 3, 4, 5, 6)
                        .map(e -> e)
                        .slidingMap(3, (a, b, c) -> (a == null ? 0 : a) + (b == null ? 0 : b) + (c == null ? 0 : c))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterSlidingMapTriWithIncrementAndIgnore() {
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, true, (a, b, c) -> a + b + c).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, true, (a, b, c) -> a + b + c).skip(1).count());
        assertArrayEquals(new Integer[] { 6, 15 }, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, true, (a, b, c) -> a + b + c).toArray());
        assertArrayEquals(new Integer[] { 15 }, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, true, (a, b, c) -> a + b + c).skip(1).toArray());
        assertEquals(N.toList(6, 15), Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, true, (a, b, c) -> a + b + c).toList());
        assertEquals(N.toList(15), Stream.of(1, 2, 3, 4, 5, 6).slidingMap(3, true, (a, b, c) -> a + b + c).skip(1).toList());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, true, (a, b, c) -> a + b + c).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, true, (a, b, c) -> a + b + c).skip(1).count());
        assertArrayEquals(new Integer[] { 6, 15 }, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, true, (a, b, c) -> a + b + c).toArray());
        assertArrayEquals(new Integer[] { 15 }, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, true, (a, b, c) -> a + b + c).skip(1).toArray());
        assertEquals(N.toList(6, 15), Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, true, (a, b, c) -> a + b + c).toList());
        assertEquals(N.toList(15), Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(3, true, (a, b, c) -> a + b + c).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSlidingMapWithIncrement() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 7, 11 }, Stream.of(1, 2, 3, 4, 5, 6).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toArray());
        assertArrayEquals(new Integer[] { 7, 11 },
                Stream.of(1, 2, 3, 4, 5, 6).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toArray());
        assertEquals(N.toList(3, 7, 11), Stream.of(1, 2, 3, 4, 5, 6).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toList());
        assertEquals(N.toList(7, 11), Stream.of(1, 2, 3, 4, 5, 6).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 7, 11 },
                Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toArray());
        assertArrayEquals(new Integer[] { 7, 11 },
                Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toArray());
        assertEquals(N.toList(3, 7, 11), Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).toList());
        assertEquals(N.toList(7, 11),
                Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).slidingMap(2, (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSlidingMapWithIncrementAndIgnore() {
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).slidingMap(2, true, (a, b) -> a + b).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).slidingMap(2, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 7 }, Stream.of(1, 2, 3, 4, 5).slidingMap(2, true, (a, b) -> a + b).toArray());
        assertArrayEquals(new Integer[] { 7 }, Stream.of(1, 2, 3, 4, 5).slidingMap(2, true, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(3, 7), Stream.of(1, 2, 3, 4, 5).slidingMap(2, true, (a, b) -> a + b).toList());
        assertEquals(N.toList(7), Stream.of(1, 2, 3, 4, 5).slidingMap(2, true, (a, b) -> a + b).skip(1).toList());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap(2, true, (a, b) -> a + b).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap(2, true, (a, b) -> a + b).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 7 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap(2, true, (a, b) -> a + b).toArray());
        assertArrayEquals(new Integer[] { 7 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap(2, true, (a, b) -> a + b).skip(1).toArray());
        assertEquals(N.toList(3, 7), Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap(2, true, (a, b) -> a + b).toList());
        assertEquals(N.toList(7), Stream.of(1, 2, 3, 4, 5).map(e -> e).slidingMap(2, true, (a, b) -> a + b).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSorted() {
        assertEquals(5, Stream.of(5, 3, 1, 4, 2).sorted().count());
        assertEquals(4, Stream.of(5, 3, 1, 4, 2).sorted().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).sorted().toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).sorted().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).sorted().toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).sorted().skip(1).toList());
        assertEquals(5, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted().count());
        assertEquals(4, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted().toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted().toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSortedByComparator() {
        assertEquals(5, Stream.of(5, 3, 1, 4, 2).sorted(Comparator.naturalOrder()).count());
        assertEquals(4, Stream.of(5, 3, 1, 4, 2).sorted(Comparator.naturalOrder()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).sorted(Comparator.naturalOrder()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).sorted(Comparator.naturalOrder()).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).sorted(Comparator.naturalOrder()).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).sorted(Comparator.naturalOrder()).skip(1).toList());
        assertEquals(5, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted(Comparator.naturalOrder()).count());
        assertEquals(4, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted(Comparator.naturalOrder()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted(Comparator.naturalOrder()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted(Comparator.naturalOrder()).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted(Comparator.naturalOrder()).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(5, 3, 1, 4, 2).map(e -> e).sorted(Comparator.naturalOrder()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSortedBy() {
        assertEquals(3, Stream.of("bb", "a", "ccc").sortedBy(String::length).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").sortedBy(String::length).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedBy(String::length).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedBy(String::length).skip(1).toArray());
        assertEquals(N.toList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").sortedBy(String::length).toList());
        assertEquals(N.toList("bb", "ccc"), Stream.of("bb", "a", "ccc").sortedBy(String::length).skip(1).toList());
        assertEquals(3, Stream.of("bb", "a", "ccc").map(e -> e).sortedBy(String::length).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").map(e -> e).sortedBy(String::length).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedBy(String::length).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedBy(String::length).skip(1).toArray());
        assertEquals(N.toList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedBy(String::length).toList());
        assertEquals(N.toList("bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedBy(String::length).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSortedByDouble() {
        assertEquals(3, Stream.of("bb", "a", "ccc").sortedByDouble(s -> (double) s.length()).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").sortedByDouble(s -> (double) s.length()).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedByDouble(s -> (double) s.length()).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedByDouble(s -> (double) s.length()).skip(1).toArray());
        assertEquals(N.toList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").sortedByDouble(s -> (double) s.length()).toList());
        assertEquals(N.toList("bb", "ccc"), Stream.of("bb", "a", "ccc").sortedByDouble(s -> (double) s.length()).skip(1).toList());
        assertEquals(3, Stream.of("bb", "a", "ccc").map(e -> e).sortedByDouble(s -> (double) s.length()).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").map(e -> e).sortedByDouble(s -> (double) s.length()).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedByDouble(s -> (double) s.length()).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedByDouble(s -> (double) s.length()).skip(1).toArray());
        assertEquals(N.toList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedByDouble(s -> (double) s.length()).toList());
        assertEquals(N.toList("bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedByDouble(s -> (double) s.length()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSortedByInt() {
        assertEquals(3, Stream.of("bb", "a", "ccc").sortedByInt(String::length).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").sortedByInt(String::length).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedByInt(String::length).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedByInt(String::length).skip(1).toArray());
        assertEquals(N.toList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").sortedByInt(String::length).toList());
        assertEquals(N.toList("bb", "ccc"), Stream.of("bb", "a", "ccc").sortedByInt(String::length).skip(1).toList());
        assertEquals(3, Stream.of("bb", "a", "ccc").map(e -> e).sortedByInt(String::length).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").map(e -> e).sortedByInt(String::length).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedByInt(String::length).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedByInt(String::length).skip(1).toArray());
        assertEquals(N.toList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedByInt(String::length).toList());
        assertEquals(N.toList("bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedByInt(String::length).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSortedByLong() {
        assertEquals(3, Stream.of("bb", "a", "ccc").sortedByLong(s -> (long) s.length()).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").sortedByLong(s -> (long) s.length()).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedByLong(s -> (long) s.length()).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").sortedByLong(s -> (long) s.length()).skip(1).toArray());
        assertEquals(N.toList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").sortedByLong(s -> (long) s.length()).toList());
        assertEquals(N.toList("bb", "ccc"), Stream.of("bb", "a", "ccc").sortedByLong(s -> (long) s.length()).skip(1).toList());
        assertEquals(3, Stream.of("bb", "a", "ccc").map(e -> e).sortedByLong(s -> (long) s.length()).count());
        assertEquals(2, Stream.of("bb", "a", "ccc").map(e -> e).sortedByLong(s -> (long) s.length()).skip(1).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedByLong(s -> (long) s.length()).toArray());
        assertArrayEquals(new String[] { "bb", "ccc" }, Stream.of("bb", "a", "ccc").map(e -> e).sortedByLong(s -> (long) s.length()).skip(1).toArray());
        assertEquals(N.toList("a", "bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedByLong(s -> (long) s.length()).toList());
        assertEquals(N.toList("bb", "ccc"), Stream.of("bb", "a", "ccc").map(e -> e).sortedByLong(s -> (long) s.length()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterStep() {
        assertEquals(3, Stream.of(1, 2, 3, 4, 5, 6).step(2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 3, 5 }, Stream.of(1, 2, 3, 4, 5, 6).step(2).toArray());
        assertArrayEquals(new Integer[] { 3, 5 }, Stream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).toArray());
        assertEquals(N.toList(1, 3, 5), Stream.of(1, 2, 3, 4, 5, 6).step(2).toList());
        assertEquals(N.toList(3, 5), Stream.of(1, 2, 3, 4, 5, 6).step(2).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 3, 5 }, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).toArray());
        assertArrayEquals(new Integer[] { 3, 5 }, Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).toArray());
        assertEquals(N.toList(1, 3, 5), Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).toList());
        assertEquals(N.toList(3, 5), Stream.of(1, 2, 3, 4, 5, 6).map(e -> e).step(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlattMap() {
        assertEquals(6, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flattMap(l -> l.stream()).count());
        assertEquals(5, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flattMap(l -> l.stream()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flattMap(l -> l.stream()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flattMap(l -> l.stream()).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6), Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flattMap(l -> l.stream()).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).flattMap(l -> l.stream()).skip(1).toList());
        assertEquals(6, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flattMap(l -> l.stream()).count());
        assertEquals(5, Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flattMap(l -> l.stream()).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flattMap(l -> l.stream()).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5, 6 },
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flattMap(l -> l.stream()).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flattMap(l -> l.stream()).toList());
        assertEquals(N.toList(2, 3, 4, 5, 6),
                Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)).map(e -> e).flattMap(l -> l.stream()).skip(1).toList());
    }

    @Test
    public void testStreamArgJoinsWithEmptyLeftKeepRightRows() {
        // regression: the right-side rows were only materialized inside the flatMap mapper, which
        // never runs for an empty left stream, so full/right joins silently returned []
        final List<Pair<Integer, Integer>> full = Stream.<Integer> empty()
                .fullJoin(Stream.of(10, 20), Fn.identity(), Fn.identity(), (t, u) -> Pair.of(t, u))
                .toList();
        assertEquals(N.asList(Pair.of((Integer) null, 10), Pair.of((Integer) null, 20)), full);

        final List<Pair<Integer, Integer>> rightJoined = Stream.<Integer> empty()
                .rightJoin(Stream.of(10, 20), Fn.identity(), Fn.identity(), (t, u) -> Pair.of(t, u))
                .toList();
        assertEquals(N.asList(Pair.of((Integer) null, 10), Pair.of((Integer) null, 20)), rightJoined);
    }

    @Test
    public void testStreamCreatedAfterOnEach() {
        List<Integer> list = new ArrayList<>();
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).peek(list::add).count());
        list.clear();
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).peek(list::add).skip(1).count());
        list.clear();
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).toArray());
        list.clear();
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).skip(1).toArray());
        list.clear();
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).toList());
        list.clear();
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).peek(list::add).skip(1).toList());
    }

    @Test
    public void testStreamEx_AbstractClass() {
        try {
            Class<?> streamExClass = Stream.StreamEx.class;
            assertTrue(java.lang.reflect.Modifier.isAbstract(streamExClass.getModifiers()));

            assertEquals(Stream.class, streamExClass.getSuperclass());

            assertEquals(0, streamExClass.getConstructors().length);
        } catch (Exception e) {
            fail("Failed to verify StreamEx class structure: " + e.getMessage());
        }
    }

    @Test
    public void testStreamEx_Inheritance() {
        assertTrue(Stream.StreamEx.class.getSuperclass().equals(Stream.class));
    }

    @Test
    @Timeout(10)
    public void testStreamState_SortingAndComparator() throws Exception {
        Stream.of(3, 1, 4, 1, 5).sorted().addSubscriber(s -> {
            s.forEach(subscriberStreamResults::add);
        }).forEach(x -> {
            mainStreamResults.add(x);
        });

        assertEquals(Arrays.asList(1, 1, 3, 4, 5), mainStreamResults);
        assertEquals(Arrays.asList(1, 1, 3, 4, 5), subscriberStreamResults);
    }

    @Test
    public void testStreamWithNullHandling() {
        List<String> input = Arrays.asList("a", null, "b", null, "c");

        List<String> result = createStream(input).filter(Objects::nonNull).map(String::toUpperCase).toList();

        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void testStreamCreatedAfterSymmetricDifference() {
        assertEquals(4, Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(3, 4, 5)).count());
        assertEquals(3, Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(3, 4, 5)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 4, 5 }, Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(3, 4, 5)).toArray());
        assertArrayEquals(new Integer[] { 2, 4, 5 }, Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(3, 4, 5)).skip(1).toArray());
        assertEquals(N.toList(1, 2, 4, 5), Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(3, 4, 5)).toList());
        assertEquals(N.toList(2, 4, 5), Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(3, 4, 5)).skip(1).toList());
        assertEquals(4, Stream.of(1, 2, 3).map(e -> e).symmetricDifference(Arrays.asList(3, 4, 5)).count());
        assertEquals(3, Stream.of(1, 2, 3).map(e -> e).symmetricDifference(Arrays.asList(3, 4, 5)).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).symmetricDifference(Arrays.asList(3, 4, 5)).toArray());
        assertArrayEquals(new Integer[] { 2, 4, 5 }, Stream.of(1, 2, 3).map(e -> e).symmetricDifference(Arrays.asList(3, 4, 5)).skip(1).toArray());
        assertEquals(N.toList(1, 2, 4, 5), Stream.of(1, 2, 3).map(e -> e).symmetricDifference(Arrays.asList(3, 4, 5)).toList());
        assertEquals(N.toList(2, 4, 5), Stream.of(1, 2, 3).map(e -> e).symmetricDifference(Arrays.asList(3, 4, 5)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTakeLast() {
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).takeLast(2).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).takeLast(2).skip(1).count());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).takeLast(2).toArray());
        assertArrayEquals(new Integer[] { 5 }, Stream.of(1, 2, 3, 4, 5).takeLast(2).skip(1).toArray());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).takeLast(2).toList());
        assertEquals(N.toList(5), Stream.of(1, 2, 3, 4, 5).takeLast(2).skip(1).toList());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeLast(2).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeLast(2).skip(1).count());
        assertArrayEquals(new Integer[] { 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeLast(2).toArray());
        assertArrayEquals(new Integer[] { 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeLast(2).skip(1).toArray());
        assertEquals(N.toList(4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).takeLast(2).toList());
        assertEquals(N.toList(5), Stream.of(1, 2, 3, 4, 5).map(e -> e).takeLast(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTakeWhile() {
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2 }, Stream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new Integer[] { 2 }, Stream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList(1, 2), Stream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).toList());
        assertEquals(N.toList(2), Stream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).skip(1).toList());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).count());
        assertEquals(1, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new Integer[] { 2 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList(1, 2), Stream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).toList());
        assertEquals(N.toList(2), Stream.of(1, 2, 3, 4, 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testThreadSafety() throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(1000);

        Thread producer = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                try {
                    queue.put(i);
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        producer.start();

        List<Integer> consumed = Stream.observe(queue, Duration.ofMillis(500)).filter(n -> n % 2 == 0).toList();

        producer.join();

        assertTrue(consumed.size() > 0);
        assertTrue(consumed.stream().allMatch(n -> n % 2 == 0));
    }

    // throwIfEmpty

    @Test
    public void testStreamCreatedAfterThrowIfEmpty() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).throwIfEmpty().count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).throwIfEmpty().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).throwIfEmpty().toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).throwIfEmpty().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).throwIfEmpty().toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).throwIfEmpty().skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterThrowIfEmptyWithSupplier() {
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty!")).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty!")).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty!")).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty!")).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty!")).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).throwIfEmpty(() -> new RuntimeException("Empty!")).skip(1).toList());
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty!")).count());
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty!")).skip(1).count());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty!")).toArray());
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 },
                Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty!")).skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty!")).toList());
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).throwIfEmpty(() -> new RuntimeException("Empty!")).skip(1).toList());
    }

    @Test
    public void testThrowIfEmpty() {
        assertThrows(NoSuchElementException.class, () -> Stream.empty().throwIfEmpty().count());
    }

    @Test
    public void testThrowIfEmptyNonEmpty() {
        assertEquals(3, Stream.of(1, 2, 3).throwIfEmpty().count());
    }

    @Test
    public void testThrowIfEmptyWithSupplier() {
        assertThrows(IllegalStateException.class, () -> Stream.empty().throwIfEmpty(IllegalStateException::new).count());
    }

    @Test
    public void test_advance_toArray() {
        {
            boolean[] a = new boolean[] { true, false, true, false, true };
            assertArrayEquals(Array.box(a), Stream.of(a).toArray());
            Boolean[] ret = Stream.of(a).step(2).toArray(Boolean[]::new);
            assertArrayEquals(new Boolean[] { true, true, true }, ret);
        }
        {
            char[] a = new char[] { 'a', 'b', 'c', 'd', 'e' };
            assertArrayEquals(Array.box(a), Stream.of(a).toArray());
            Character[] ret = Stream.of(a).step(2).toArray(Character[]::new);
            assertArrayEquals(new Character[] { 'a', 'c', 'e' }, ret);
        }
        {
            byte[] a = new byte[] { 1, 2, 3, 4, 5 };
            assertArrayEquals(Array.box(a), Stream.of(a).toArray());
            Byte[] ret = Stream.of(a).step(2).toArray(Byte[]::new);
            assertArrayEquals(new Byte[] { 1, 3, 5 }, ret);
        }
        {
            short[] a = new short[] { 1, 2, 3, 4, 5 };
            assertArrayEquals(Array.box(a), Stream.of(a).toArray());
            Short[] ret = Stream.of(a).step(2).toArray(Short[]::new);
            assertArrayEquals(new Short[] { 1, 3, 5 }, ret);
        }
        {
            int[] a = new int[] { 1, 2, 3, 4, 5 };
            assertArrayEquals(Array.box(a), Stream.of(a).toArray());
            Integer[] ret = Stream.of(a).step(2).toArray(Integer[]::new);
            assertArrayEquals(new Integer[] { 1, 3, 5 }, ret);
        }
        {
            long[] a = new long[] { 1, 2, 3, 4, 5 };
            assertArrayEquals(Array.box(a), Stream.of(a).toArray());
            Long[] ret = Stream.of(a).step(2).toArray(Long[]::new);
            assertArrayEquals(new Long[] { 1l, 3l, 5l }, ret);
        }
        {
            float[] a = new float[] { 1, 2, 3, 4, 5 };
            assertArrayEquals(Array.box(a), Stream.of(a).toArray());
            Float[] ret = Stream.of(a).step(2).toArray(Float[]::new);
            assertArrayEquals(new Float[] { 1f, 3f, 5f }, ret);
        }
        {
            double[] a = new double[] { 1, 2, 3, 4, 5 };
            assertArrayEquals(Array.box(a), Stream.of(a).toArray());
            Double[] ret = Stream.of(a).step(2).toArray(Double[]::new);
            assertArrayEquals(new Double[] { 1d, 3d, 5d }, ret);
        }
        {
            String[] a = new String[] { "1", "2", "3", "4", "5" };
            assertArrayEquals(a, Stream.of(a).toArray());
            String[] ret = Stream.of(a).step(2).toArray(String[]::new);
            assertArrayEquals(new String[] { "1", "3", "5" }, ret);
        }
    }

    @Test
    public void testFastPathToArrayNullTerminatesOversizedArray() {
        final Boolean[] booleans = { null, null, Boolean.TRUE };
        assertSame(booleans, Stream.of(new boolean[] { true, false }).iteratorEx().toArray(booleans));
        assertArrayEquals(new Boolean[] { Boolean.TRUE, Boolean.FALSE, null }, booleans);

        final Byte[] bytes = { null, null, (byte) 9 };
        assertSame(bytes, Stream.of(new byte[] { 1, 2 }).iteratorEx().toArray(bytes));
        assertArrayEquals(new Byte[] { (byte) 1, (byte) 2, null }, bytes);

        final String[] repeated = { null, null, "stale" };
        assertSame(repeated, Stream.repeat("x", 2).iteratorEx().toArray(repeated));
        assertArrayEquals(new String[] { "x", "x", null }, repeated);
    }

    @Test
    public void testStreamCreatedAfterTop() {
        assertEquals(3, Stream.of(5, 3, 1, 4, 2).top(3).count());
        assertEquals(2, Stream.of(5, 3, 1, 4, 2).top(3).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 5, 4 }, Stream.of(5, 3, 1, 4, 2).top(3).toArray());
        assertArrayEquals(new Integer[] { 5, 4 }, Stream.of(5, 3, 1, 4, 2).top(3).skip(1).toArray());
        assertEquals(N.toList(3, 5, 4), Stream.of(5, 3, 1, 4, 2).top(3).toList());
        assertEquals(N.toList(5, 4), Stream.of(5, 3, 1, 4, 2).top(3).skip(1).toList());
        assertEquals(3, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3).count());
        assertEquals(2, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 5, 4 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3).toArray());
        assertArrayEquals(new Integer[] { 5, 4 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3).skip(1).toArray());
        assertEquals(N.toList(3, 5, 4), Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3).toList());
        assertEquals(N.toList(5, 4), Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTopWithComparator() {
        assertEquals(3, Stream.of(5, 3, 1, 4, 2).top(3, Comparator.naturalOrder()).count());
        assertEquals(2, Stream.of(5, 3, 1, 4, 2).top(3, Comparator.naturalOrder()).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 5, 4 }, Stream.of(5, 3, 1, 4, 2).top(3, Comparator.naturalOrder()).toArray());
        assertArrayEquals(new Integer[] { 5, 4 }, Stream.of(5, 3, 1, 4, 2).top(3, Comparator.naturalOrder()).skip(1).toArray());
        assertEquals(N.toList(3, 5, 4), Stream.of(5, 3, 1, 4, 2).top(3, Comparator.naturalOrder()).toList());
        assertEquals(N.toList(5, 4), Stream.of(5, 3, 1, 4, 2).top(3, Comparator.naturalOrder()).skip(1).toList());
        assertEquals(3, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3, Comparator.naturalOrder()).count());
        assertEquals(2, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3, Comparator.naturalOrder()).skip(1).count());
        assertArrayEquals(new Integer[] { 3, 5, 4 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3, Comparator.naturalOrder()).toArray());
        assertArrayEquals(new Integer[] { 5, 4 }, Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3, Comparator.naturalOrder()).skip(1).toArray());
        assertEquals(N.toList(3, 5, 4), Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3, Comparator.naturalOrder()).toList());
        assertEquals(N.toList(5, 4), Stream.of(5, 3, 1, 4, 2).map(e -> e).top(3, Comparator.naturalOrder()).skip(1).toList());
    }

    @Test
    public void testTimeBasedSlidingWindow_noDuplicatesAcrossOverlappingWindows() {
        // regression: next()'s pull loop queued an element belonging to a future window AND kept it as
        // timedNext, so overlapping windows accumulated it twice and re-queued a second copy
        final long t0 = System.currentTimeMillis();
        final Long a = t0 + 60_000; // 1 min after the first window start
        final Long x = t0 + 720_000; // 12 min: belongs to the windows starting at 5 and 10 min
        final WindowHandler<Long, List<Long>> handler = WindowHandler.of(Long::longValue);

        final List<List<Long>> windows = Stream.of(a, x).window(Duration.ofMinutes(10), Duration.ofMinutes(5), () -> t0, handler, Collectors.toList()).toList();

        assertEquals(N.asList(N.asList(a), N.asList(x), N.asList(x)), windows);
    }

    @Test
    public void testPrimitiveCollectionZipWithDefaultsTreatsNullStreamAsEmpty() {
        assertEquals(Collections.singletonList((byte) 3),
                Stream.zip(Arrays.asList(ByteStream.of((byte) 1), null), new byte[] { 0, 2 }, values -> (byte) (values[0] + values[1])).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithCollection() {
        List<String> collection = Arrays.asList("a", "b", "c");
        BiFunction<Integer, String, String> zipFunc = (i, s) -> i + s;

        assertEquals(3, Stream.of(1, 2, 3, 4, 5).zipWith(collection, zipFunc).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).zipWith(collection, zipFunc).skip(1).count());
        assertArrayEquals(new String[] { "1a", "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).zipWith(collection, zipFunc).toArray());
        assertArrayEquals(new String[] { "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).zipWith(collection, zipFunc).skip(1).toArray());
        assertEquals(N.toList("1a", "2b", "3c"), Stream.of(1, 2, 3, 4, 5).zipWith(collection, zipFunc).toList());
        assertEquals(N.toList("2b", "3c"), Stream.of(1, 2, 3, 4, 5).zipWith(collection, zipFunc).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(collection, zipFunc).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(collection, zipFunc).skip(1).count());
        assertArrayEquals(new String[] { "1a", "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(collection, zipFunc).toArray());
        assertArrayEquals(new String[] { "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(collection, zipFunc).skip(1).toArray());
        assertEquals(N.toList("1a", "2b", "3c"), Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(collection, zipFunc).toList());
        assertEquals(N.toList("2b", "3c"), Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(collection, zipFunc).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithStream() {
        BiFunction<Integer, String, String> zipFunc = (i, s) -> i + s;

        assertEquals(3, Stream.of(1, 2, 3, 4, 5).zipWith(Stream.of("a", "b", "c"), zipFunc).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).zipWith(Stream.of("a", "b", "c"), zipFunc).skip(1).count());
        assertArrayEquals(new String[] { "1a", "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).zipWith(Stream.of("a", "b", "c"), zipFunc).toArray());
        assertArrayEquals(new String[] { "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).zipWith(Stream.of("a", "b", "c"), zipFunc).skip(1).toArray());
        assertEquals(N.toList("1a", "2b", "3c"), Stream.of(1, 2, 3, 4, 5).zipWith(Stream.of("a", "b", "c"), zipFunc).toList());
        assertEquals(N.toList("2b", "3c"), Stream.of(1, 2, 3, 4, 5).zipWith(Stream.of("a", "b", "c"), zipFunc).skip(1).toList());
        assertEquals(3, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(Stream.of("a", "b", "c"), zipFunc).count());
        assertEquals(2, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(Stream.of("a", "b", "c"), zipFunc).skip(1).count());
        assertArrayEquals(new String[] { "1a", "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(Stream.of("a", "b", "c"), zipFunc).toArray());
        assertArrayEquals(new String[] { "2b", "3c" }, Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(Stream.of("a", "b", "c"), zipFunc).skip(1).toArray());
        assertEquals(N.toList("1a", "2b", "3c"), Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(Stream.of("a", "b", "c"), zipFunc).toList());
        assertEquals(N.toList("2b", "3c"), Stream.of(1, 2, 3, 4, 5).map(e -> e).zipWith(Stream.of("a", "b", "c"), zipFunc).skip(1).toList());
    }

}
