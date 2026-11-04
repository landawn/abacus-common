package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.u.OptionalShort;

/** ShortStream general contracts. */
public class ShortStreamTest extends ShortStreamTestSupport {

    @Test
    public void testStreamCreatedAfterAppend() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3).append((short) 10, (short) 11).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).append((short) 10, (short) 11).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 10, 11 }, ShortStream.of((short) 1, (short) 2, (short) 3).append((short) 10, (short) 11).toArray());
        assertArrayEquals(new short[] { 2, 3, 10, 11 }, ShortStream.of((short) 1, (short) 2, (short) 3).append((short) 10, (short) 11).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 10, (short) 11),
                ShortStream.of((short) 1, (short) 2, (short) 3).append((short) 10, (short) 11).toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 10, (short) 11),
                ShortStream.of((short) 1, (short) 2, (short) 3).append((short) 10, (short) 11).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).append((short) 10, (short) 11).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).append((short) 10, (short) 11).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 10, 11 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).append((short) 10, (short) 11).toArray());
        assertArrayEquals(new short[] { 2, 3, 10, 11 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).append((short) 10, (short) 11).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 10, (short) 11),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).append((short) 10, (short) 11).toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 10, (short) 11),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).append((short) 10, (short) 11).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmpty() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3).appendIfEmpty((short) 10, (short) 11).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3).appendIfEmpty((short) 10, (short) 11).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3).appendIfEmpty((short) 10, (short) 11).toArray());
        assertArrayEquals(new short[] { 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3).appendIfEmpty((short) 10, (short) 11).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3), ShortStream.of((short) 1, (short) 2, (short) 3).appendIfEmpty((short) 10, (short) 11).toList());
        assertEquals(N.toList((short) 2, (short) 3), ShortStream.of((short) 1, (short) 2, (short) 3).appendIfEmpty((short) 10, (short) 11).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).appendIfEmpty((short) 10, (short) 11).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).appendIfEmpty((short) 10, (short) 11).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).appendIfEmpty((short) 10, (short) 11).toArray());
        assertArrayEquals(new short[] { 2, 3 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).appendIfEmpty((short) 10, (short) 11).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).appendIfEmpty((short) 10, (short) 11).toList());
        assertEquals(N.toList((short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).appendIfEmpty((short) 10, (short) 11).skip(1).toList());
    }

    @Test
    public void testChainedOperations() {
        long count = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                .filter(n -> n > 2)
                .map(n -> (short) (n * 10))
                .peek(n -> assertTrue(n >= 30))
                .count();

        assertEquals(3, count);
    }

    @Test
    public void testStreamCreatedAfterCollapse() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).skip(1).count());
        assertArrayEquals(new short[] { 3, 5, 7 },
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 5, 7 },
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 5, (short) 7),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 5, (short) 7),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).skip(1).toList());
        assertEquals(3,
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).count());
        assertEquals(2,
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7)
                        .map(e -> e)
                        .collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new short[] { 3, 5, 7 },
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 5, 7 },
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7)
                        .map(e -> e)
                        .collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((short) 3, (short) 5, (short) 7),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 5, (short) 7),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7)
                        .map(e -> e)
                        .collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterCollapseWithTriPredicate() {
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b)).count());
        assertEquals(1,
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b)).skip(1).count());
        assertArrayEquals(new short[] { 3, 12 },
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 12 },
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b)).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 12),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 12),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b)).skip(1).toList());
        assertEquals(2,
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).map(e -> e).collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b)).count());
        assertEquals(1,
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7)
                        .map(e -> e)
                        .collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new short[] { 3, 12 },
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).map(e -> e).collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 12 },
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7)
                        .map(e -> e)
                        .collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((short) 3, (short) 12),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).map(e -> e).collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 12),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7)
                        .map(e -> e)
                        .collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testComplexPipeline() {
        short[] result = ShortStream.range((short) 1, (short) 10).filter(n -> n % 2 == 0).map(n -> (short) (n * 2)).sorted().limit(3).toArray();

        assertArrayEquals(new short[] { 4, 8, 12 }, result);
    }

    // defaultIfEmpty

    @Test
    public void testDefaultIfEmpty() {
        ShortStream stream = ShortStream.empty().defaultIfEmpty(() -> createShortStream((short) 42));
        assertArrayEquals(new short[] { 42 }, stream.toArray());
    }

    @Test
    public void testDefaultIfEmpty_NonEmpty() {
        short[] result = createShortStream((short) 1, (short) 2).defaultIfEmpty(() -> ShortStream.of((short) 99)).toArray();
        assertArrayEquals(new short[] { 1, 2 }, result);
    }

    @Test
    public void testDefaultIfEmptyOnEmpty() {
        short[] result = ShortStream.empty().defaultIfEmpty(() -> ShortStream.of((short) 99)).toArray();
        assertArrayEquals(new short[] { 99 }, result);
    }

    @Test
    public void testStreamCreatedAfterDelay() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).delay(Duration.ofMillis(1)).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).delay(Duration.ofMillis(1)).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).delay(Duration.ofMillis(1)).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).delay(Duration.ofMillis(1)).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).delay(Duration.ofMillis(1)).toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).delay(Duration.ofMillis(1)).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).delay(Duration.ofMillis(1)).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).delay(Duration.ofMillis(1)).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).delay(Duration.ofMillis(1)).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).delay(Duration.ofMillis(1)).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).delay(Duration.ofMillis(1)).toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).delay(Duration.ofMillis(1)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDistinct() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).distinct().count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).distinct().skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).distinct().toArray());
        assertArrayEquals(new short[] { 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).distinct().skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3), ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).distinct().toList());
        assertEquals(N.toList((short) 2, (short) 3), ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).distinct().skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).map(e -> e).distinct().count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).map(e -> e).distinct().skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).map(e -> e).distinct().toArray());
        assertArrayEquals(new short[] { 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).map(e -> e).distinct().skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).map(e -> e).distinct().toList());
        assertEquals(N.toList((short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).map(e -> e).distinct().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhile() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3).toList());
        assertEquals(N.toList((short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new short[] { 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3).toList());
        assertEquals(N.toList((short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhileWithAction() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3, i -> {
        }).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3, i -> {
        }).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3, i -> {
        }).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3, i -> {
        }).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3, i -> {
                }).toList());
        assertEquals(N.toList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3, i -> {
        }).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3, i -> {
                }).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3, i -> {
        }).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3, i -> {
                }).toList());
        assertEquals(N.toList((short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3, i -> {
                }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFilter() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2).toList());
        assertEquals(N.toList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2).toArray());
        assertArrayEquals(new short[] { 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2).toList());
        assertEquals(N.toList((short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFilterWithAction() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2, i -> {
        }).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2, i -> {
        }).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2, i -> {
        }).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2, i -> {
        }).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2, i -> {
        }).toList());
        assertEquals(N.toList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2, i -> {
        }).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2, i -> {
        }).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2, i -> {
        }).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2, i -> {
                }).toList());
        assertEquals(N.toList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMap() {
        assertEquals(6, ShortStream.of((short) 1, (short) 2, (short) 3).flatMap(i -> ShortStream.of(i, (short) (i + 10))).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).flatMap(i -> ShortStream.of(i, (short) (i + 10))).skip(2).count());
        assertArrayEquals(new short[] { 1, 11, 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).flatMap(i -> ShortStream.of(i, (short) (i + 10))).toArray());
        assertArrayEquals(new short[] { 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).flatMap(i -> ShortStream.of(i, (short) (i + 10))).skip(2).toArray());
        assertEquals(N.toList((short) 1, (short) 11, (short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).flatMap(i -> ShortStream.of(i, (short) (i + 10))).toList());
        assertEquals(N.toList((short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).flatMap(i -> ShortStream.of(i, (short) (i + 10))).skip(2).toList());
        assertEquals(6, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMap(i -> ShortStream.of(i, (short) (i + 10))).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMap(i -> ShortStream.of(i, (short) (i + 10))).skip(2).count());
        assertArrayEquals(new short[] { 1, 11, 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMap(i -> ShortStream.of(i, (short) (i + 10))).toArray());
        assertArrayEquals(new short[] { 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMap(i -> ShortStream.of(i, (short) (i + 10))).skip(2).toArray());
        assertEquals(N.toList((short) 1, (short) 11, (short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMap(i -> ShortStream.of(i, (short) (i + 10))).toList());
        assertEquals(N.toList((short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMap(i -> ShortStream.of(i, (short) (i + 10))).skip(2).toList());
    }

    // ifEmpty

    @Test
    @DisplayName("Test ifEmpty() method")
    public void testIfEmpty() {
        final boolean[] actionExecuted = { false };

        stream.ifEmpty(() -> actionExecuted[0] = true).toArray();
        assertFalse(actionExecuted[0]);

        ShortStream.empty().ifEmpty(() -> actionExecuted[0] = true).toArray();
        assertTrue(actionExecuted[0]);
    }

    @Test
    public void testIfEmpty_Empty() {
        AtomicInteger counter = new AtomicInteger(0);
        ShortStream.empty().ifEmpty(counter::incrementAndGet).toArray();
        assertEquals(1, counter.get());
    }

    @Test
    public void testIfEmpty_NotEmpty() {
        AtomicInteger counter = new AtomicInteger(0);
        createShortStream((short) 1, (short) 2).ifEmpty(counter::incrementAndGet).toArray();
        assertEquals(0, counter.get());
    }

    @Test
    public void testIsParallel() {
        ShortStream stream1 = ShortStream.of((short) 1, (short) 2, (short) 3);
        assertFalse(stream1.isParallel());

        ShortStream stream2 = ShortStream.of((short) 1, (short) 2, (short) 3).parallel();
        assertTrue(stream2.isParallel());
    }

    // iterator

    @Test
    public void testBooleanSupplierIteratorsStayExhausted() {
        final AtomicInteger conditionCalls = new AtomicInteger();
        ShortIterator iter = ShortStream.iterate(() -> conditionCalls.getAndIncrement() > 0, () -> (short) 1).iterator();

        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextShort);
        assertEquals(1, conditionCalls.get());

        conditionCalls.set(0);
        iter = ShortStream.iterate((short) 1, () -> conditionCalls.getAndIncrement() > 0, value -> (short) (value + 1)).iterator();

        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextShort);
        assertEquals(1, conditionCalls.get());
    }

    @Test
    public void testIterator() {
        ShortIterator iterator = ShortStream.of((short) 1, (short) 2, (short) 3).iterator();
        assertTrue(iterator.hasNext());
        assertEquals(1, iterator.nextShort());
        assertTrue(iterator.hasNext());
        assertEquals(2, iterator.nextShort());
        assertTrue(iterator.hasNext());
        assertEquals(3, iterator.nextShort());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testIterator_HasNextAndNext() {
        ShortIterator iter = createShortStream((short) 1, (short) 2, (short) 3).iterator();
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextShort());
        assertTrue(iter.hasNext());
        assertEquals(2, iter.nextShort());
        assertTrue(iter.hasNext());
        assertEquals(3, iter.nextShort());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIteratorEmpty() {
        ShortIterator iterator = ShortStream.empty().iterator();
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testIteratorThrowsNoSuchElement() {
        ShortIterator iterator = ShortStream.of((short) 1).iterator();
        iterator.nextShort();
        assertThrows(NoSuchElementException.class, iterator::nextShort);
    }

    @Test
    public void testStreamCreatedAfterLimit() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(3).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(3).toArray());
        assertArrayEquals(new short[] { 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(3).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(3).toList());
        assertEquals(N.toList((short) 2, (short) 3), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(3).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).limit(3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).limit(3).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).limit(3).toArray());
        assertArrayEquals(new short[] { 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).limit(3).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).limit(3).toList());
        assertEquals(N.toList((short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).limit(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmap() {
        assertEquals(6, ShortStream.of((short) 1, (short) 2, (short) 3).flatMapArray(i -> new short[] { i, (short) (i + 10) }).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).flatMapArray(i -> new short[] { i, (short) (i + 10) }).skip(2).count());
        assertArrayEquals(new short[] { 1, 11, 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).flatMapArray(i -> new short[] { i, (short) (i + 10) }).toArray());
        assertArrayEquals(new short[] { 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).flatMapArray(i -> new short[] { i, (short) (i + 10) }).skip(2).toArray());
        assertEquals(N.toList((short) 1, (short) 11, (short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).flatMapArray(i -> new short[] { i, (short) (i + 10) }).toList());
        assertEquals(N.toList((short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).flatMapArray(i -> new short[] { i, (short) (i + 10) }).skip(2).toList());
        assertEquals(6, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMapArray(i -> new short[] { i, (short) (i + 10) }).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMapArray(i -> new short[] { i, (short) (i + 10) }).skip(2).count());
        assertArrayEquals(new short[] { 1, 11, 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMapArray(i -> new short[] { i, (short) (i + 10) }).toArray());
        assertArrayEquals(new short[] { 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMapArray(i -> new short[] { i, (short) (i + 10) }).skip(2).toArray());
        assertEquals(N.toList((short) 1, (short) 11, (short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMapArray(i -> new short[] { i, (short) (i + 10) }).toList());
        assertEquals(N.toList((short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMapArray(i -> new short[] { i, (short) (i + 10) }).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterMap() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(i -> (short) (i * 2)).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(i -> (short) (i * 2)).skip(1).count());
        assertArrayEquals(new short[] { 2, 4, 6, 8, 10 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(i -> (short) (i * 2)).toArray());
        assertArrayEquals(new short[] { 4, 6, 8, 10 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(i -> (short) (i * 2)).skip(1).toArray());
        assertEquals(N.toList((short) 2, (short) 4, (short) 6, (short) 8, (short) 10),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(i -> (short) (i * 2)).toList());
        assertEquals(N.toList((short) 4, (short) 6, (short) 8, (short) 10),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(i -> (short) (i * 2)).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).map(i -> (short) (i * 2)).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).map(i -> (short) (i * 2)).skip(1).count());
        assertArrayEquals(new short[] { 2, 4, 6, 8, 10 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).map(i -> (short) (i * 2)).toArray());
        assertArrayEquals(new short[] { 4, 6, 8, 10 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).map(i -> (short) (i * 2)).skip(1).toArray());
        assertEquals(N.toList((short) 2, (short) 4, (short) 6, (short) 8, (short) 10),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).map(i -> (short) (i * 2)).toList());
        assertEquals(N.toList((short) 4, (short) 6, (short) 8, (short) 10),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).map(i -> (short) (i * 2)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartial() {
        assertEquals(3,
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .count());
        assertEquals(2,
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .skip(1)
                        .count());
        assertArrayEquals(new short[] { 1, 3, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .toArray());
        assertArrayEquals(new short[] { 3, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((short) 1, (short) 3, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .toList());
        assertEquals(N.toList((short) 3, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .skip(1)
                        .toList());
        assertEquals(3,
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .count());
        assertEquals(2,
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .skip(1)
                        .count());
        assertArrayEquals(new short[] { 1, 3, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .toArray());
        assertArrayEquals(new short[] { 3, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((short) 1, (short) 3, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .toList());
        assertEquals(N.toList((short) 3, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterPrepend() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3).prepend((short) 10, (short) 11).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).prepend((short) 10, (short) 11).skip(1).count());
        assertArrayEquals(new short[] { 10, 11, 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3).prepend((short) 10, (short) 11).toArray());
        assertArrayEquals(new short[] { 11, 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3).prepend((short) 10, (short) 11).skip(1).toArray());
        assertEquals(N.toList((short) 10, (short) 11, (short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3).prepend((short) 10, (short) 11).toList());
        assertEquals(N.toList((short) 11, (short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3).prepend((short) 10, (short) 11).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).prepend((short) 10, (short) 11).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).prepend((short) 10, (short) 11).skip(1).count());
        assertArrayEquals(new short[] { 10, 11, 1, 2, 3 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).prepend((short) 10, (short) 11).toArray());
        assertArrayEquals(new short[] { 11, 1, 2, 3 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).prepend((short) 10, (short) 11).skip(1).toArray());
        assertEquals(N.toList((short) 10, (short) 11, (short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).prepend((short) 10, (short) 11).toList());
        assertEquals(N.toList((short) 11, (short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).prepend((short) 10, (short) 11).skip(1).toList());
    }

    @Test
    public void testPrintln_EmptyStream() {
        assertDoesNotThrow(() -> ShortStream.empty().println());
    }

    @Test
    public void testPrintlnMethod() {
        assertDoesNotThrow(() -> createShortStream((short) 1, (short) 2, (short) 3).println());
    }

    @Test
    public void testPsp() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).parallel()
                .psp(s -> s.map(n -> (short) (n * 2)))
                .sequential()
                .toArray();

        Arrays.sort(result);
        assertArrayEquals(new short[] { 2, 4, 6, 8, 10 }, result);
    }

    @Test
    public void testPspMethod2() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).psp(s -> s.filter(v -> v > 2)).toArray();
        assertEquals(3, result.length);
    }

    @Test
    public void testStreamCreatedAfterRangeMap() {
        assertEquals(3,
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7).rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).count());
        assertEquals(2,
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7)
                        .rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new short[] { 3, 9, 14 },
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7).rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 9, 14 },
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7)
                        .rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((short) 3, (short) 9, (short) 14),
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7).rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 9, (short) 14),
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7)
                        .rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toList());
        assertEquals(3,
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7)
                        .map(e -> e)
                        .rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .count());
        assertEquals(2,
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7)
                        .map(e -> e)
                        .rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new short[] { 3, 9, 14 },
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7)
                        .map(e -> e)
                        .rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .toArray());
        assertArrayEquals(new short[] { 9, 14 },
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7)
                        .map(e -> e)
                        .rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((short) 3, (short) 9, (short) 14),
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7)
                        .map(e -> e)
                        .rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .toList());
        assertEquals(N.toList((short) 9, (short) 14),
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7)
                        .map(e -> e)
                        .rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterRateLimited() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(100.0).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(100.0).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(100.0).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(100.0).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(100.0).toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(100.0).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(100.0).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(100.0).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(100.0).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(100.0).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(100.0).toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(100.0).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRateLimitedWithRateLimiter() {
        RateLimiter rateLimiter = RateLimiter.create(100.0);
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(rateLimiter).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(rateLimiter).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(rateLimiter).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(rateLimiter).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(rateLimiter).toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(rateLimiter).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(rateLimiter).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(rateLimiter).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(rateLimiter).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(rateLimiter).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(rateLimiter).toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(rateLimiter).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSorted() {
        assertEquals(5, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).reverseSorted().count());
        assertEquals(4, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).reverseSorted().skip(1).count());
        assertArrayEquals(new short[] { 5, 4, 3, 2, 1 }, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).reverseSorted().toArray());
        assertArrayEquals(new short[] { 4, 3, 2, 1 }, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).reverseSorted().skip(1).toArray());
        assertEquals(N.toList((short) 5, (short) 4, (short) 3, (short) 2, (short) 1),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).reverseSorted().toList());
        assertEquals(N.toList((short) 4, (short) 3, (short) 2, (short) 1),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).reverseSorted().skip(1).toList());
        assertEquals(5, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).reverseSorted().count());
        assertEquals(4, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).reverseSorted().skip(1).count());
        assertArrayEquals(new short[] { 5, 4, 3, 2, 1 },
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).reverseSorted().toArray());
        assertArrayEquals(new short[] { 4, 3, 2, 1 },
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).reverseSorted().skip(1).toArray());
        assertEquals(N.toList((short) 5, (short) 4, (short) 3, (short) 2, (short) 1),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).reverseSorted().toList());
        assertEquals(N.toList((short) 4, (short) 3, (short) 2, (short) 1),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).reverseSorted().skip(1).toList());
    }

    // scan

    @Test
    public void testStreamCreatedAfterScan() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((a, b) -> (short) (a + b)).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((a, b) -> (short) (a + b)).skip(1).count());
        assertArrayEquals(new short[] { 1, 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((a, b) -> (short) (a + b)).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((a, b) -> (short) (a + b)).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((a, b) -> (short) (a + b)).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((a, b) -> (short) (a + b)).skip(1).count());
        assertArrayEquals(new short[] { 1, 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((a, b) -> (short) (a + b)).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((a, b) -> (short) (a + b)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInit() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 0, (a, b) -> (short) (a + b)).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 0, (a, b) -> (short) (a + b)).skip(1).count());
        assertArrayEquals(new short[] { 1, 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 0, (a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 0, (a, b) -> (short) (a + b)).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 0, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 0, (a, b) -> (short) (a + b)).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 0, (a, b) -> (short) (a + b)).count());
        assertEquals(4,
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 0, (a, b) -> (short) (a + b)).skip(1).count());
        assertArrayEquals(new short[] { 1, 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 0, (a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 0, (a, b) -> (short) (a + b)).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 0, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 0, (a, b) -> (short) (a + b)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInitIncluded() {
        assertEquals(6, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 10, true, (a, b) -> (short) (a + b)).count());
        assertEquals(5,
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 10, true, (a, b) -> (short) (a + b)).skip(1).count());
        assertArrayEquals(new short[] { 10, 11, 13, 16, 20, 25 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 10, true, (a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 11, 13, 16, 20, 25 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 10, true, (a, b) -> (short) (a + b)).skip(1).toArray());
        assertEquals(N.toList((short) 10, (short) 11, (short) 13, (short) 16, (short) 20, (short) 25),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 10, true, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 11, (short) 13, (short) 16, (short) 20, (short) 25),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 10, true, (a, b) -> (short) (a + b)).skip(1).toList());
        assertEquals(6,
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 10, true, (a, b) -> (short) (a + b)).count());
        assertEquals(5,
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .scan((short) 10, true, (a, b) -> (short) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new short[] { 10, 11, 13, 16, 20, 25 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 10, true, (a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 11, 13, 16, 20, 25 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .scan((short) 10, true, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((short) 10, (short) 11, (short) 13, (short) 16, (short) 20, (short) 25),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 10, true, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.toList((short) 11, (short) 13, (short) 16, (short) 20, (short) 25),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .scan((short) 10, true, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterSkip() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2).toList());
        assertEquals(N.toList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2).toList());
        assertEquals(N.toList((short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipWithAction() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2, i -> {
        }).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2, i -> {
        }).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2, i -> {
        }).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2, i -> {
        }).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2, i -> {
        }).toList());
        assertEquals(N.toList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2, i -> {
        }).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2, i -> {
        }).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2, i -> {
        }).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2, i -> {
        }).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2, i -> {
        }).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2, i -> {
        }).toList());
        assertEquals(N.toList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2, i -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipUntil() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(i -> i >= 3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(i -> i >= 3).toList());
        assertEquals(N.toList((short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(i -> i >= 3).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skipUntil(i -> i >= 3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new short[] { 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skipUntil(i -> i >= 3).toList());
        assertEquals(N.toList((short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSorted() {
        assertEquals(5, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).sorted().count());
        assertEquals(4, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).sorted().skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).sorted().toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 }, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).sorted().skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).sorted().toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).sorted().skip(1).toList());
        assertEquals(5, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).sorted().count());
        assertEquals(4, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).sorted().skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).sorted().toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 },
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).sorted().skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).sorted().toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).sorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterStep() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).step(2).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).step(2).skip(1).count());
        assertArrayEquals(new short[] { 1, 3, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).step(2).toArray());
        assertArrayEquals(new short[] { 3, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).step(2).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 3, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).step(2).toList());
        assertEquals(N.toList((short) 3, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).step(2).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).map(e -> e).step(2).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).map(e -> e).step(2).skip(1).count());
        assertArrayEquals(new short[] { 1, 3, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).map(e -> e).step(2).toArray());
        assertArrayEquals(new short[] { 3, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).map(e -> e).step(2).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 3, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).map(e -> e).step(2).toList());
        assertEquals(N.toList((short) 3, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).map(e -> e).step(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOnEach() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).peek(i -> {
        }).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).peek(i -> {
        }).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).peek(i -> {
        }).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).peek(i -> {
        }).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).peek(i -> {
                }).toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).peek(i -> {
        }).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).peek(i -> {
        }).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).peek(i -> {
        }).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).peek(i -> {
        }).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).peek(i -> {
        }).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).peek(i -> {
                }).toList());
        assertEquals(N.toList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).peek(i -> {
                }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTakeWhile() {
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).takeWhile(i -> i < 3).count());
        assertEquals(1, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new short[] { 1, 2 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new short[] { 2 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).takeWhile(i -> i < 3).toList());
        assertEquals(N.toList((short) 2), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).takeWhile(i -> i < 3).skip(1).toList());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).takeWhile(i -> i < 3).count());
        assertEquals(1, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new short[] { 1, 2 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new short[] { 2 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList((short) 1, (short) 2),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).takeWhile(i -> i < 3).toList());
        assertEquals(N.toList((short) 2),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toList());
    }

    // throwIfEmpty

    @Test
    @DisplayName("Test throwIfEmpty() methods")
    public void testThrowIfEmpty() {
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, stream.throwIfEmpty().toArray());

        assertThrows(NoSuchElementException.class, () -> ShortStream.empty().throwIfEmpty().toArray());

        assertThrows(IllegalStateException.class, () -> ShortStream.empty().throwIfEmpty(() -> new IllegalStateException("Stream is empty")).toArray());
    }

    @Test
    public void testThrowIfEmpty_NotEmpty() {
        assertDoesNotThrow(() -> createShortStream((short) 1).throwIfEmpty(() -> new RuntimeException("empty")).toArray());
    }

    @Test
    public void testThrowIfEmpty_WithSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ShortStream.empty().throwIfEmpty(() -> new IllegalArgumentException("empty")).toArray());
    }

    @Test
    public void testThrowIfEmptyWithSupplier() {
        try {
            ShortStream.empty().throwIfEmpty(() -> new IllegalStateException("Stream is empty")).toArray();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Stream is empty", e.getMessage());
        }
    }

    @Test
    public void testStreamCreatedAfterTop() {
        assertEquals(3, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3).skip(1).count());
        assertArrayEquals(new short[] { 3, 5, 4 }, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3).toArray());
        assertArrayEquals(new short[] { 5, 4 }, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 5, (short) 4), ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3).toList());
        assertEquals(N.toList((short) 5, (short) 4), ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3).skip(1).count());
        assertArrayEquals(new short[] { 3, 5, 4 }, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3).toArray());
        assertArrayEquals(new short[] { 5, 4 }, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 5, (short) 4),
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3).toList());
        assertEquals(N.toList((short) 5, (short) 4), ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTopWithComparator() {
        assertEquals(3, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3, Comparator.reverseOrder()).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3, Comparator.reverseOrder()).skip(1).count());
        assertArrayEquals(new short[] { 3, 1, 2 },
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3, Comparator.reverseOrder()).toArray());
        assertArrayEquals(new short[] { 1, 2 },
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3, Comparator.reverseOrder()).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 1, (short) 2),
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3, Comparator.reverseOrder()).toList());
        assertEquals(N.toList((short) 1, (short) 2),
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3, Comparator.reverseOrder()).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3, Comparator.reverseOrder()).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3, Comparator.reverseOrder()).skip(1).count());
        assertArrayEquals(new short[] { 3, 1, 2 },
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3, Comparator.reverseOrder()).toArray());
        assertArrayEquals(new short[] { 1, 2 },
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3, Comparator.reverseOrder()).skip(1).toArray());
        assertEquals(N.toList((short) 3, (short) 1, (short) 2),
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3, Comparator.reverseOrder()).toList());
        assertEquals(N.toList((short) 1, (short) 2),
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3, Comparator.reverseOrder()).skip(1).toList());
    }

    // ---- deep review 2026-11-02 G121 begin ----
    private static final short[] G121_SAMPLES = { 0, -1, 1, Short.MIN_VALUE, Short.MAX_VALUE };
    private static final short G121_PAD_A = (short) -1;
    private static final short G121_PAD_B = (short) 9;
    private static final short G121_PAD_C = (short) 7;

    private static short[] g121Of(final int... values) {
        final short[] result = new short[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = (short) values[i];
        }

        return result;
    }

    private static short[] g121RandomArray(final java.util.Random rnd, final int maxLength) {
        final short[] a = new short[rnd.nextInt(maxLength + 1)];

        for (int i = 0; i < a.length; i++) {
            a[i] = rnd.nextInt(4) == 0 ? G121_SAMPLES[rnd.nextInt(G121_SAMPLES.length)] : (short) rnd.nextInt(1000);
        }

        return a;
    }

    private static short[] g121Drain(final com.landawn.abacus.util.ShortIterator iterator) {
        final com.landawn.abacus.util.ShortList list = new com.landawn.abacus.util.ShortList();

        while (iterator.hasNext()) {
            list.add(iterator.nextShort());
        }

        return list.toArray();
    }

    private static short[] g121Suffix(final short[] a, final int from) {
        return from >= a.length ? new short[0] : java.util.Arrays.copyOfRange(a, from, a.length);
    }

    // variant 0: zip(a, b), 1: zip(a, b, c), 2: zip(a, b, padA, padB), 3: zip(a, b, c, padA, padB, padC); every call is recorded
    private static ShortStream g121Zip(final int variant, final short[] a, final short[] b, final short[] c, final java.util.List<String> calls) {
        switch (variant) {
            case 0:
                return ShortStream.zip(a, b, (x, y) -> {
                    calls.add(x + "," + y);
                    return (short) (x * 3 + y);
                });
            case 1:
                return ShortStream.zip(a, b, c, (x, y, z) -> {
                    calls.add(x + "," + y + "," + z);
                    return (short) (x * 3 + y - z);
                });
            case 2:
                return ShortStream.zip(a, b, G121_PAD_A, G121_PAD_B, (x, y) -> {
                    calls.add(x + "," + y);
                    return (short) (x * 3 + y);
                });
            default:
                return ShortStream.zip(a, b, c, G121_PAD_A, G121_PAD_B, G121_PAD_C, (x, y, z) -> {
                    calls.add(x + "," + y + "," + z);
                    return (short) (x * 3 + y - z);
                });
        }
    }

    private static short[] g121ZipReference(final int variant, final short[] a, final short[] b, final short[] c) {
        final int len = variant == 0 ? Math.min(a.length, b.length)
                : variant == 1 ? Math.min(a.length, Math.min(b.length, c.length))
                        : variant == 2 ? Math.max(a.length, b.length) : Math.max(a.length, Math.max(b.length, c.length));
        final short[] result = new short[len];

        for (int i = 0; i < len; i++) {
            final short x = i < a.length ? a[i] : G121_PAD_A;
            final short y = i < b.length ? b[i] : G121_PAD_B;
            final short z = i < c.length ? c[i] : G121_PAD_C;
            result[i] = variant == 0 || variant == 2 ? (short) (x * 3 + y) : (short) (x * 3 + y - z);
        }

        return result;
    }

    // G121-01: zip(short[]...) toArray()/toShortList()/skip(k).toArray()/iterator().toArray()|toList()/sorted() fill an exactly sized
    // array; values and the zipFunction call sequence equal the element-by-element traversal (pin: GREEN on base and fix)
    @org.junit.jupiter.api.Test
    public void testZipArrays_bulkTerminalsMatchElementwiseTraversal() {
        final java.util.Random rnd = new java.util.Random(121);

        for (int round = 0; round < 200; round++) {
            final short[] a = g121RandomArray(rnd, 12);
            final short[] b = g121RandomArray(rnd, 12);
            final short[] c = g121RandomArray(rnd, 12);

            for (int variant = 0; variant < 4; variant++) {
                final short[] expected = g121ZipReference(variant, a, b, c);
                final java.util.List<String> expectedCalls = new java.util.ArrayList<>();
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Drain(g121Zip(variant, a, b, c, expectedCalls).iterator()));
                org.junit.jupiter.api.Assertions.assertEquals(expected.length, expectedCalls.size());

                final java.util.List<String> calls = new java.util.ArrayList<>();
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Zip(variant, a, b, c, calls).toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

                calls.clear();
                final com.landawn.abacus.util.ShortList list = g121Zip(variant, a, b, c, calls).toShortList();
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, list.toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
                list.add((short) 42);
                org.junit.jupiter.api.Assertions.assertEquals(expected.length + 1, list.size());

                final int k = rnd.nextInt(expected.length + 2);
                calls.clear();
                org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, k), g121Zip(variant, a, b, c, calls).skip(k).toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

                calls.clear();
                final com.landawn.abacus.util.ShortIterator iter = g121Zip(variant, a, b, c, calls).iterator();
                final int consumed = Math.min(k, expected.length);

                for (int i = 0; i < consumed; i++) {
                    iter.nextShort();
                }

                org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, consumed),
                        rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
                org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
                org.junit.jupiter.api.Assertions.assertSame(com.landawn.abacus.util.N.EMPTY_SHORT_ARRAY, iter.toArray());
                org.junit.jupiter.api.Assertions.assertEquals(0, iter.toList().size());

                calls.clear();
                final short[] sorted = expected.clone();
                java.util.Arrays.sort(sorted);
                org.junit.jupiter.api.Assertions.assertArrayEquals(sorted, g121Zip(variant, a, b, c, calls).sorted().toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
            }
        }
    }

    private static boolean g121TakeFirst(final boolean stateful, final short x, final short y, final java.util.List<String> calls) {
        calls.add(x + "," + y);
        return stateful ? calls.size() % 3 != 0 : x <= y;
    }

    private static ShortStream g121Merge(final short[] a, final short[] b, final boolean stateful, final java.util.List<String> calls) {
        return ShortStream.merge(a, b, (x, y) -> g121TakeFirst(stateful, x, y, calls) ? com.landawn.abacus.util.MergeResult.TAKE_FIRST
                : com.landawn.abacus.util.MergeResult.TAKE_SECOND);
    }

    // G121-01: merge(short[], short[]) toArray()/toShortList() fill an exactly sized array; values and the order and number of
    // nextSelector calls (a stateful selector makes the result depend on them) equal the element-by-element traversal (pin)
    @org.junit.jupiter.api.Test
    public void testMergeArrays_bulkTerminalsMatchElementwiseTraversal() {
        final java.util.Random rnd = new java.util.Random(12103);

        for (int round = 0; round < 200; round++) {
            final short[] a = g121RandomArray(rnd, 10);
            final short[] b = g121RandomArray(rnd, 10);

            if (rnd.nextBoolean()) {
                java.util.Arrays.sort(a);
                java.util.Arrays.sort(b);
            }

            final boolean stateful = rnd.nextBoolean();
            final com.landawn.abacus.util.ShortList expectedList = new com.landawn.abacus.util.ShortList();
            final java.util.List<String> expectedCalls = new java.util.ArrayList<>();
            int i = 0;
            int j = 0;

            while (i < a.length || j < b.length) {
                if (i < a.length && (j >= b.length || g121TakeFirst(stateful, a[i], b[j], expectedCalls))) {
                    expectedList.add(a[i++]);
                } else {
                    expectedList.add(b[j++]);
                }
            }

            final short[] expected = expectedList.toArray();
            final java.util.List<String> calls = new java.util.ArrayList<>();
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Drain(g121Merge(a, b, stateful, calls).iterator()));
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

            calls.clear();
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Merge(a, b, stateful, calls).toArray());
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

            calls.clear();
            final com.landawn.abacus.util.ShortList list = g121Merge(a, b, stateful, calls).toShortList();
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, list.toArray());
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
            list.add((short) 1);
            org.junit.jupiter.api.Assertions.assertEquals(expected.length + 1, list.size());

            final int k = rnd.nextInt(expected.length + 2);
            calls.clear();
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, k), g121Merge(a, b, stateful, calls).skip(k).toArray());
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

            calls.clear();
            final com.landawn.abacus.util.ShortIterator iter = g121Merge(a, b, stateful, calls).iterator();
            final int consumed = Math.min(k, expected.length);

            for (int n = 0; n < consumed; n++) {
                iter.nextShort();
            }

            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, consumed), rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
            org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
            org.junit.jupiter.api.Assertions.assertEquals(0, iter.toArray().length);
        }
    }

    private static Object g121Bulk(final com.landawn.abacus.util.ShortIterator iterator, final boolean useList) {
        return useList ? iterator.toList() : iterator.toArray();
    }

    // G121-01: a failing zipFunction or nextSelector inside toArray()/toList() leaves the iterator exactly where element-by-element
    // traversal leaves it (zip(a, b)/zip(a, b, c) advance before the call, the padded zips and merge do not) (pin)
    @org.junit.jupiter.api.Test
    public void testZipAndMergeArrays_failingCallbackKeepsIteratorState() {
        final short[] a = g121Of(1, 2, 3, 4, 5);
        final short[] b = g121Of(10, 20, 30, 40, 50, 60);
        final short[] c = g121Of(100, 200, 300, 400, 500, 600, 700);

        for (int viaList = 0; viaList < 2; viaList++) {
            final boolean useList = viaList == 1;
            final int[] calls = { 0 };

            final com.landawn.abacus.util.ShortIterator zip2 = ShortStream.zip(a, b, (x, y) -> {
                if (++calls[0] == 3) {
                    throw new IllegalStateException("zip2");
                }

                return (short) (x + y);
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("zip2",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(zip2, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertEquals(3, calls[0]);
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(44, 55), g121Drain(zip2));
            org.junit.jupiter.api.Assertions.assertEquals(5, calls[0]);

            calls[0] = 0;
            final com.landawn.abacus.util.ShortIterator zip3 = ShortStream.zip(a, b, c, (x, y, z) -> {
                if (++calls[0] == 2) {
                    throw new IllegalStateException("zip3");
                }

                return (short) (x + y + z);
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("zip3",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(zip3, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(333, 444, 555), g121Drain(zip3));
            org.junit.jupiter.api.Assertions.assertEquals(5, calls[0]);

            calls[0] = 0;
            final com.landawn.abacus.util.ShortIterator pad2 = ShortStream.zip(a, b, (short) -1, (short) -2, (x, y) -> {
                if (++calls[0] == 3) {
                    throw new IllegalStateException("pad2");
                }

                return (short) (x + y);
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("pad2",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(pad2, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(33, 44, 55, 59), g121Drain(pad2));
            org.junit.jupiter.api.Assertions.assertEquals(7, calls[0]);

            calls[0] = 0;
            final com.landawn.abacus.util.ShortIterator pad3 = ShortStream.zip(a, b, c, (short) -1, (short) -2, (short) -3, (x, y, z) -> {
                if (++calls[0] == 6) {
                    throw new IllegalStateException("pad3");
                }

                return (short) (x + y + z);
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("pad3",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(pad3, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(659, 697), g121Drain(pad3));
            org.junit.jupiter.api.Assertions.assertEquals(8, calls[0]);

            calls[0] = 0;
            final com.landawn.abacus.util.ShortIterator merge = ShortStream.merge(g121Of(1, 3, 5), g121Of(2, 4, 6), (x, y) -> {
                if (++calls[0] == 3) {
                    throw new IllegalStateException("merge");
                }

                return x <= y ? com.landawn.abacus.util.MergeResult.TAKE_FIRST : com.landawn.abacus.util.MergeResult.TAKE_SECOND;
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("merge",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(merge, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(3, 4, 5, 6), g121Drain(merge));
            org.junit.jupiter.api.Assertions.assertEquals(6, calls[0]);
        }
    }

    // G121-01: a zipFunction that consumes its own stream's iterator re-entrantly (once) ends toArray()/toShortList() where the
    // hasNext()-driven loop ends, instead of failing with NoSuchElementException (pin)
    @org.junit.jupiter.api.Test
    public void testZipArrays_reentrantCallbackEndsBulkTerminalEarly() {
        final short[] a = g121Of(1, 2, 3, 4, 5);
        final short[] b = g121Of(10, 20, 30, 40, 50);
        final short[] c = g121Of(100, 200, 300, 400, 500);
        final Runnable[] consume = new Runnable[1];
        final boolean[] once = { true };

        ShortStream s = ShortStream.zip(a, b, (x, y) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (short) (x + y);
        });
        com.landawn.abacus.util.ShortIterator it = s.iterator();
        consume[0] = it::nextShort;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(11, 33, 44, 55), it.toArray());
        org.junit.jupiter.api.Assertions.assertFalse(it.hasNext());

        once[0] = true;
        s = ShortStream.zip(a, b, (x, y) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (short) (x + y);
        });
        it = s.iterator();
        consume[0] = it::nextShort;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(11, 33, 44, 55), s.toShortList().toArray());

        once[0] = true;
        s = ShortStream.zip(a, b, c, (x, y, z) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (short) (x + y + z);
        });
        it = s.iterator();
        consume[0] = it::nextShort;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(111, 333, 444, 555), it.toArray());

        once[0] = true;
        s = ShortStream.zip(a, g121Of(7), (short) -1, (short) -2, (x, y) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (short) (x + y);
        });
        it = s.iterator();
        consume[0] = it::nextShort;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(8, 1, 2, 3), it.toArray());

        once[0] = true;
        s = ShortStream.zip(a, g121Of(7), new short[0], (short) -1, (short) -2, (short) -3, (x, y, z) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (short) (x + y + z);
        });
        it = s.iterator();
        consume[0] = it::nextShort;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(5, -2, -1, 0), it.toArray());
    }

    private static ShortStream g121Flatten(final short[][] a, final int mode, final short pad) {
        return mode == 0 ? ShortStream.flatten(a, true) : ShortStream.flatten(a, pad, mode == 1);
    }

    // G121-01: flatten(short[][], true) (rectangular walk and the sparse jagged walk) and flatten(short[][], value, vertically)
    // toArray()/toShortList() fill an exactly sized array; pinned against plain column-major / row-major references, also after
    // skip(k) and partial iteration (pin)
    @org.junit.jupiter.api.Test
    public void testFlattenVerticalAndAligned_bulkTerminalsMatchReference() {
        final java.util.Random rnd = new java.util.Random(12102);

        for (int round = 0; round < 300; round++) {
            final int shape = rnd.nextInt(3);
            final int rows = shape == 2 ? 5 + rnd.nextInt(6) : rnd.nextInt(7);
            final short[][] a = new short[rows][];
            final int width = 1 + rnd.nextInt(6);

            for (int r = 0; r < rows; r++) {
                if (shape == 1) {
                    a[r] = new short[width];
                    java.util.Arrays.fill(a[r], (short) (r + 1));
                } else if (shape == 2) {
                    // one long row among null, empty and one-element rows: rows * maxLen > 4 * n selects the jagged walk
                    a[r] = r == rows / 2 ? g121RandomArray(rnd, 40) : rnd.nextInt(3) == 0 ? null : new short[rnd.nextInt(2)];
                } else {
                    a[r] = rnd.nextInt(5) == 0 ? null : g121RandomArray(rnd, 6);
                }
            }

            int maxLen = 0;

            for (final short[] row : a) {
                maxLen = Math.max(maxLen, row == null ? 0 : row.length);
            }

            final short pad = G121_SAMPLES[rnd.nextInt(G121_SAMPLES.length)];
            final com.landawn.abacus.util.ShortList vertical = new com.landawn.abacus.util.ShortList();
            final com.landawn.abacus.util.ShortList alignedVertical = new com.landawn.abacus.util.ShortList();
            final com.landawn.abacus.util.ShortList alignedHorizontal = new com.landawn.abacus.util.ShortList();

            for (int col = 0; col < maxLen; col++) {
                for (final short[] row : a) {
                    if (row != null && col < row.length) {
                        vertical.add(row[col]);
                        alignedVertical.add(row[col]);
                    } else {
                        alignedVertical.add(pad);
                    }
                }
            }

            for (final short[] row : a) {
                for (int col = 0; col < maxLen; col++) {
                    alignedHorizontal.add(row != null && col < row.length ? row[col] : pad);
                }
            }

            for (int mode = 0; mode < 3; mode++) {
                // a single row is returned as-is (no padding) by both flatten overloads
                final short[] expected = rows == 1 ? (a[0] == null ? new short[0] : a[0].clone())
                        : (mode == 0 ? vertical : mode == 1 ? alignedVertical : alignedHorizontal).toArray();

                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Flatten(a, mode, pad).toArray());
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Flatten(a, mode, pad).toShortList().toArray());
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Drain(g121Flatten(a, mode, pad).iterator()));

                final int k = rnd.nextInt(expected.length + 2);
                org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, k), g121Flatten(a, mode, pad).skip(k).toArray());

                final com.landawn.abacus.util.ShortIterator iter = g121Flatten(a, mode, pad).iterator();
                final int consumed = Math.min(k, expected.length);

                for (int n = 0; n < consumed; n++) {
                    iter.nextShort();
                }

                org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, consumed),
                        rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
                org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
                org.junit.jupiter.api.Assertions.assertEquals(0, iter.toList().size());
                org.junit.jupiter.api.Assertions.assertThrows(java.util.NoSuchElementException.class, iter::nextShort);
            }
        }
    }
    // ---- deep review 2026-11-02 G121 end ----

    // ---- deep review 2026-11-02b G112 begin ----
    private static short[] g112Drain(final ShortIterator iterator) {
        final com.landawn.abacus.util.ShortList list = new com.landawn.abacus.util.ShortList();

        while (iterator.hasNext()) {
            list.add(iterator.nextShort());
        }

        return list.toArray();
    }

    // Element-wise reference of range / rangeClosed (by == 0: step 1) without the stream classes.
    private static short[] g112RangeReference(final short start, final short end, final short by, final boolean closed) {
        final com.landawn.abacus.util.ShortList list = new com.landawn.abacus.util.ShortList();
        final int step = by == 0 ? 1 : by;

        for (int v = start; step > 0 ? (closed ? v <= end : v < end) : (closed ? v >= end : v > end); v += step) {
            list.add((short) v);
        }

        return list.toArray();
    }

    // Checks toShortList(), iterator().toList() after a partial read, and skip(k).toShortList() against the reference, plus
    // the iterator state afterwards and the independence/mutability of the returned list.
    private static void g112CheckBulkList(final String name, final java.util.function.Supplier<ShortStream> supplier, final short[] expected, final int k) {
        assertArrayEquals(expected, g112Drain(supplier.get().iterator()), name);

        final com.landawn.abacus.util.ShortList list = supplier.get().toShortList();
        assertArrayEquals(expected, list.toArray(), name);
        list.add((short) 42);
        list.add(0, (short) -42);
        assertEquals(expected.length + 2, list.size(), name);
        assertEquals((short) -42, list.get(0), name);
        assertEquals((short) 42, list.get(list.size() - 1), name);

        final int consumed = Math.min(k, expected.length);
        final ShortIterator iter = supplier.get().iterator();

        for (int i = 0; i < consumed; i++) {
            assertEquals(expected[i], iter.nextShort(), name);
        }

        final com.landawn.abacus.util.ShortList rest = iter.toList();
        assertArrayEquals(Arrays.copyOfRange(expected, consumed, expected.length), rest.toArray(), name);
        assertFalse(iter.hasNext(), name);
        assertEquals(0, iter.toList().size(), name);
        assertEquals(0, iter.toArray().length, name);
        assertEquals(0, iter.count(), name);
        assertThrows(NoSuchElementException.class, iter::nextShort, name);
        rest.add((short) 1);
        assertEquals(expected.length - consumed + 1, rest.size(), name);

        assertArrayEquals(Arrays.copyOfRange(expected, Math.min(k, expected.length), expected.length), supplier.get().skip(k).toShortList().toArray(), name);
    }

    // G112-01: range/rangeClosed (step 1 and stepped, ascending and descending, full span, MIN_VALUE/MAX_VALUE steps) build
    // toShortList()/iterator().toList() from their exactly sized toArray(); the result and the iterator state must equal the
    // element-by-element traversal (pin: GREEN on base and fix)
    @Test
    public void testRangeToShortList_matchesElementwiseTraversal_r116() {
        final short[] specials = { 0, 1, -1, 2, -3, 100, -100, Short.MAX_VALUE, Short.MIN_VALUE, (short) (Short.MAX_VALUE - 1), (short) (Short.MIN_VALUE + 1) };
        final short[] steps = { 1, 2, 3, 7, -1, -2, -5, Short.MAX_VALUE, Short.MIN_VALUE };
        final java.util.Random rnd = new java.util.Random(11201);

        for (int round = 0; round < 400; round++) {
            final short start = rnd.nextBoolean() ? specials[rnd.nextInt(specials.length)] : (short) (rnd.nextInt(200) - 100);
            final short end = rnd.nextBoolean() ? specials[rnd.nextInt(specials.length)] : (short) (start + rnd.nextInt(40) - 20);
            final short by = steps[rnd.nextInt(steps.length)];
            final int k = rnd.nextInt(5);

            g112CheckBulkList("range(" + start + "," + end + ")", () -> ShortStream.range(start, end), g112RangeReference(start, end, (short) 0, false), k);
            g112CheckBulkList("rangeClosed(" + start + "," + end + ")", () -> ShortStream.rangeClosed(start, end),
                    g112RangeReference(start, end, (short) 0, true), k);
            g112CheckBulkList("range(" + start + "," + end + "," + by + ")", () -> ShortStream.range(start, end, by), g112RangeReference(start, end, by, false),
                    k);
            g112CheckBulkList("rangeClosed(" + start + "," + end + "," + by + ")", () -> ShortStream.rangeClosed(start, end, by),
                    g112RangeReference(start, end, by, true), k);
        }

        g112CheckBulkList("range(MIN,MAX)", () -> ShortStream.range(Short.MIN_VALUE, Short.MAX_VALUE),
                g112RangeReference(Short.MIN_VALUE, Short.MAX_VALUE, (short) 0, false), 3);
        g112CheckBulkList("rangeClosed(MIN,MAX)", () -> ShortStream.rangeClosed(Short.MIN_VALUE, Short.MAX_VALUE),
                g112RangeReference(Short.MIN_VALUE, Short.MAX_VALUE, (short) 0, true), 2);
        g112CheckBulkList("rangeClosed(MAX,MIN,-1)", () -> ShortStream.rangeClosed(Short.MAX_VALUE, Short.MIN_VALUE, (short) -1),
                g112RangeReference(Short.MAX_VALUE, Short.MIN_VALUE, (short) -1, true), 1);
        g112CheckBulkList("range(0,MIN,MIN)", () -> ShortStream.range((short) 0, Short.MIN_VALUE, Short.MIN_VALUE), new short[] { 0 }, 0);
        g112CheckBulkList("rangeClosed(0,MIN,MIN)", () -> ShortStream.rangeClosed((short) 0, Short.MIN_VALUE, Short.MIN_VALUE),
                new short[] { 0, Short.MIN_VALUE }, 1);

        // the stream stays sorted for an ascending range: sorted() is the same stream, so toShortList() goes through the override
        assertArrayEquals(new short[] { 3, 4, 5, 6 }, ShortStream.range((short) 3, (short) 7).sorted().toShortList().toArray());
        assertArrayEquals(new short[] { 9, 6, 3 }, ShortStream.rangeClosed((short) 9, (short) 3, (short) -3).toShortList().toArray());
    }

    // ---- deep review 2026-11-02b G112 end ----
    // ---- deep review 2026-11-02b G120 begin ----
    // G120-02 (pin): ShortStream.of(ShortBuffer) for a buffer without an accessible array (direct, read-only, sliced, view of a heap
    // ByteBuffer) reads the window [position, limit) through a dedicated iterator; toArray()/toList() fill an exact-size
    // array with the same get(i) reads. Results, laziness (live view), window snapshot and iterator states must match element-wise traversal.
    @Test
    public void testOfShortBuffer_nonArrayBufferTerminalsMatchElementwise_r116() {
        final short[] content = { 10, Short.MIN_VALUE, Short.MAX_VALUE, 0, -1, 5, 5, 3, 9, -7, 2, 1 };

        for (int kind = 0; kind < 5; kind++) {
            for (int from = 0; from <= content.length; from++) {
                for (int to = from; to <= content.length; to++) {
                    final short[] expected = Arrays.copyOfRange(content, from, to);
                    final java.nio.ShortBuffer buf = g120ShortBuffer(kind, content, from, to);

                    assertFalse(buf.hasArray());
                    assertArrayEquals(expected, ShortStream.of(buf).toArray());
                    assertArrayEquals(expected, ShortStream.of(buf).toShortList().toArray());
                    assertEquals(expected.length, ShortStream.of(buf).count());
                    assertArrayEquals(g120ShortSuffix(expected, 3), ShortStream.of(buf).skip(3).toArray());
                    assertArrayEquals(g120ShortSorted(expected), ShortStream.of(buf).sorted().toArray());

                    final com.landawn.abacus.util.ShortList list = ShortStream.of(buf).toShortList();
                    list.add((short) 42); // a fresh, mutable list
                    assertEquals(expected.length + 1, list.size());

                    for (int consumed = 0; consumed <= expected.length; consumed++) {
                        final ShortIterator iter = ShortStream.of(buf).iterator();

                        for (int n = 0; n < consumed; n++) {
                            assertEquals(expected[n], iter.nextShort());
                        }

                        final boolean viaArray = (consumed + from + kind) % 2 == 0;
                        final short[] rest = viaArray ? iter.toArray() : iter.toList().toArray();
                        assertArrayEquals(g120ShortSuffix(expected, consumed), rest);
                        assertFalse(iter.hasNext());
                        assertEquals(0, iter.toList().size());
                        assertThrows(NoSuchElementException.class, iter::nextShort);

                        if (expected.length > 0) {
                            // an exhausted iterator returns the shared empty array, as the element-wise toArray() did
                            org.junit.jupiter.api.Assertions.assertSame(com.landawn.abacus.util.N.EMPTY_SHORT_ARRAY, iter.toArray());
                        } else {
                            assertEquals(0, iter.toArray().length);
                        }
                    }

                    final ShortIteratorEx iterEx = (ShortIteratorEx) ShortStream.of(buf).iterator();
                    iterEx.advance(2);
                    assertArrayEquals(g120ShortSuffix(expected, 2), iterEx.toArray());
                }
            }
        }

        // live view of the window taken at the call: later writes are seen, later position/limit changes are not
        final java.nio.ShortBuffer direct = java.nio.ByteBuffer.allocateDirect(6 * 2).asShortBuffer();
        direct.put(new short[] { 1, 2, 3, 4, 5, 6 });
        direct.position(1);
        direct.limit(5);

        final ShortStream stream = ShortStream.of(direct);
        direct.put(4, (short) 40);
        direct.clear();
        direct.limit(0);

        assertArrayEquals(new short[] { 2, 3, 4, 40 }, stream.toArray());
        assertEquals(0, direct.position());
        assertEquals(0, direct.limit());
    }

    private static java.nio.ShortBuffer g120ShortBuffer(final int kind, final short[] content, final int from, final int to) {
        final java.nio.ShortBuffer buf;

        if (kind == 0) {
            buf = java.nio.ByteBuffer.allocateDirect(content.length * 2).asShortBuffer().put(content);
        } else if (kind == 1) {
            buf = java.nio.ShortBuffer.wrap(content.clone()).asReadOnlyBuffer();
        } else if (kind == 2) {
            final java.nio.ShortBuffer outer = java.nio.ByteBuffer.allocateDirect((content.length + 3) * 2).asShortBuffer();
            outer.position(2);
            outer.put(content);
            outer.position(2);
            outer.limit(2 + content.length);
            buf = outer.slice();
        } else if (kind == 3) {
            buf = java.nio.ByteBuffer.allocateDirect(content.length * 2).asShortBuffer().put(content).asReadOnlyBuffer();
        } else {
            buf = java.nio.ByteBuffer.allocate(content.length * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(content);
        }

        buf.limit(to);
        buf.position(from);

        return buf;
    }

    private static short[] g120ShortSuffix(final short[] a, final int k) {
        return Arrays.copyOfRange(a, Math.min(k, a.length), a.length);
    }

    private static short[] g120ShortSorted(final short[] a) {
        final short[] copy = a.clone();
        Arrays.sort(copy);
        return copy;
    }

    // ---- deep review 2026-11-02b G120 end ----
    // ---- deep review 2026-11-02b G124 begin ----
    // G124: a buffer without an accessible array keeps the close-handler shape of the former
    // IntStream.range(position, limit).mapToShort(view::get) stream: exactly one (run-once, no-op) close handler.
    @Test
    public void testOfShortBuffer_nonArrayBufferKeepsRangeCloseHandler_r116() {
        final java.nio.ShortBuffer direct = java.nio.ByteBuffer.allocateDirect(4 * 2).asShortBuffer();
        direct.put(new short[] { (short) 1, (short) 2, (short) 3, (short) 4 });
        direct.position(1);
        final java.nio.ShortBuffer emptyWindow = direct.duplicate();
        emptyWindow.position(4);

        for (final java.nio.ShortBuffer buf : java.util.List.of(direct, emptyWindow)) {
            // what iterator()'s "Remember to close" warning checks
            final ShortStream stream = ShortStream.of(buf);
            org.junit.jupiter.api.Assertions.assertFalse(StreamBase.isEmptyCloseHandlers(stream.closeHandlers()));
            org.junit.jupiter.api.Assertions.assertEquals(1, stream.closeHandlers().size());

            // handlers registered later run once, in order; a failing one does not stop the others
            final java.util.List<String> calls = new java.util.ArrayList<>();
            final ShortStream withHandlers = ShortStream.of(buf).onClose(() -> calls.add("a")).onClose(() -> {
                calls.add("b");
                throw new IllegalStateException("b");
            }).onClose(() -> calls.add("c"));
            org.junit.jupiter.api.Assertions.assertEquals("b",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, withHandlers::close).getMessage());
            withHandlers.close();
            org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("a", "b", "c"), calls);
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, withHandlers::count);
        }

        org.junit.jupiter.api.Assertions.assertArrayEquals(new short[] { (short) 2, (short) 3, (short) 4 }, ShortStream.of(direct).toArray());
    }
    // ---- deep review 2026-11-02b G124 end ----

    // ---- re-review 2026-11-03 R04 begin ----
    // R04 (pin): ShortStream.of(ShortBuffer) for a buffer without an accessible array counts and skips by moving the cursor of its
    // window iterator instead of reading every skipped element. Pinned for every window, read position and skip length:
    // count(), advance(n) (negative, zero, inside, exactly to and past the end, Long.MAX_VALUE), the elements read after
    // it, the stream-level skip/step/count, the failure-atomic advance flag, and the live view of elements not yet read.
    @Test
    public void testOfShortBuffer_nonArrayBufferCountAndAdvanceMoveCursor_r116() {
        final short[] content = { 10, -32768, 32767, 0, -1, 5, 5, 3, 9, -7, 2, 1 };

        for (int kind = 0; kind < 2; kind++) {
            for (int from = 0; from <= content.length; from++) {
                for (int to = from; to <= content.length; to++) {
                    final short[] expected = java.util.Arrays.copyOfRange(content, from, to);
                    final java.nio.ShortBuffer buf = r04Buffer(kind, content, from, to);
                    org.junit.jupiter.api.Assertions.assertFalse(buf.hasArray());

                    org.junit.jupiter.api.Assertions.assertEquals(expected.length, ShortStream.of(buf).count());

                    for (int k = 0; k <= expected.length + 1; k++) {
                        final int start = Math.min(k, expected.length);
                        final short[] rest = java.util.Arrays.copyOfRange(expected, start, expected.length);
                        org.junit.jupiter.api.Assertions.assertEquals(rest.length, ShortStream.of(buf).skip(k).count());
                        org.junit.jupiter.api.Assertions.assertArrayEquals(rest, ShortStream.of(buf).skip(k).toArray());
                    }

                    org.junit.jupiter.api.Assertions.assertArrayEquals(r04Step(expected, 3), ShortStream.of(buf).step(3).toArray());

                    for (int consumed = 0; consumed <= expected.length; consumed++) {
                        final int remaining = expected.length - consumed;

                        for (final long n : new long[] { -1, 0, 1, 2, remaining, remaining + 1L, Long.MAX_VALUE }) {
                            final int next = consumed + (int) Math.min(Math.max(n, 0), remaining);
                            final ShortIteratorEx iter = (ShortIteratorEx) ShortStream.of(buf).iterator();

                            if (expected.length > 0) {
                                org.junit.jupiter.api.Assertions.assertTrue(iter.supportsFailureAtomicAdvance());
                            }

                            for (int i = 0; i < consumed; i++) {
                                org.junit.jupiter.api.Assertions.assertEquals(expected[i], iter.nextShort());
                            }

                            iter.advance(n);

                            for (int i = next; i < expected.length; i++) {
                                org.junit.jupiter.api.Assertions.assertTrue(iter.hasNext());
                                org.junit.jupiter.api.Assertions.assertEquals(expected[i], iter.nextShort());
                            }

                            org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
                            org.junit.jupiter.api.Assertions.assertEquals(0, iter.count());
                            org.junit.jupiter.api.Assertions.assertThrows(java.util.NoSuchElementException.class, iter::nextShort);

                            final ShortIteratorEx counted = (ShortIteratorEx) ShortStream.of(buf).iterator();

                            for (int i = 0; i < consumed; i++) {
                                counted.nextShort();
                            }

                            counted.advance(n);
                            org.junit.jupiter.api.Assertions.assertEquals(expected.length - next, counted.count());
                            org.junit.jupiter.api.Assertions.assertFalse(counted.hasNext());
                            counted.advance(1);
                            org.junit.jupiter.api.Assertions.assertEquals(0, counted.count());
                        }
                    }
                }
            }
        }

        // A skip moves the cursor only: an element written after the skip is read live, a skipped one is never read.
        final java.nio.ShortBuffer live = r04Buffer(1, content, 0, 6);
        final ShortIteratorEx liveIter = (ShortIteratorEx) ShortStream.of(live).iterator();
        liveIter.advance(2);
        live.put(1, (short) 777);
        live.put(3, (short) 888);
        org.junit.jupiter.api.Assertions.assertEquals(content[2], liveIter.nextShort());
        org.junit.jupiter.api.Assertions.assertEquals((short) 888, liveIter.nextShort());
        org.junit.jupiter.api.Assertions.assertEquals(2, liveIter.count());
        org.junit.jupiter.api.Assertions.assertFalse(liveIter.hasNext());
    }

    private static java.nio.ShortBuffer r04Buffer(final int kind, final short[] content, final int from, final int to) {
        // kind 0: read-only view of a heap array; kind 1: writable view of direct memory. Neither exposes an array.
        final java.nio.ShortBuffer buf = kind == 0 ? java.nio.ShortBuffer.wrap(content.clone()).asReadOnlyBuffer()
                : java.nio.ByteBuffer.allocateDirect(content.length * 2).asShortBuffer().put(content).clear();
        buf.limit(to);
        buf.position(from);
        return buf;
    }

    private static short[] r04Step(final short[] a, final int step) {
        final short[] result = new short[(a.length + step - 1) / step];

        for (int i = 0; i < result.length; i++) {
            result[i] = a[i * step];
        }

        return result;
    }
    // ---- re-review 2026-11-03 R04 end ----
}
