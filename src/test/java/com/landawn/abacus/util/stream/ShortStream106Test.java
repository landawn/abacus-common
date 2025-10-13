package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Comparator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.u.OptionalShort;

@Tag("new-test")
public class ShortStream106Test extends TestBase {

    @Test
    public void testStreamCreatedAfterFilter() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.asList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2).toList());
        assertEquals(N.asList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2).toArray());
        assertArrayEquals(new short[] { 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.asList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2).toList());
        assertEquals(N.asList((short) 4, (short) 5),
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
        assertEquals(N.asList((short) 3, (short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2, i -> {
        }).toList());
        assertEquals(N.asList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).filter(i -> i > 2, i -> {
        }).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2, i -> {
        }).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2, i -> {
        }).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).toArray());
        assertEquals(N.asList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2, i -> {
                }).toList());
        assertEquals(N.asList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).filter(i -> i > 2, i -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTakeWhile() {
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).takeWhile(i -> i < 3).count());
        assertEquals(1, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new short[] { 1, 2 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new short[] { 2 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 2), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).takeWhile(i -> i < 3).toList());
        assertEquals(N.asList((short) 2), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).takeWhile(i -> i < 3).skip(1).toList());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).takeWhile(i -> i < 3).count());
        assertEquals(1, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new short[] { 1, 2 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new short[] { 2 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 2),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).takeWhile(i -> i < 3).toList());
        assertEquals(N.asList((short) 2),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhile() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.asList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3).toList());
        assertEquals(N.asList((short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new short[] { 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.asList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3).toList());
        assertEquals(N.asList((short) 4, (short) 5),
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
        assertEquals(N.asList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3, i -> {
                }).toList());
        assertEquals(N.asList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).dropWhile(i -> i < 3, i -> {
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
        assertEquals(N.asList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3, i -> {
                }).toList());
        assertEquals(N.asList((short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).dropWhile(i -> i < 3, i -> {
                }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipUntil() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(i -> i >= 3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(N.asList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(i -> i >= 3).toList());
        assertEquals(N.asList((short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skipUntil(i -> i >= 3).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skipUntil(i -> i >= 3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new short[] { 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(N.asList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skipUntil(i -> i >= 3).toList());
        assertEquals(N.asList((short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDistinct() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).distinct().count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).distinct().skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).distinct().toArray());
        assertArrayEquals(new short[] { 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).distinct().skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 2, (short) 3), ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).distinct().toList());
        assertEquals(N.asList((short) 2, (short) 3), ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).distinct().skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).map(e -> e).distinct().count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).map(e -> e).distinct().skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).map(e -> e).distinct().toArray());
        assertArrayEquals(new short[] { 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).map(e -> e).distinct().skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).map(e -> e).distinct().toList());
        assertEquals(N.asList((short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 3).map(e -> e).distinct().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSorted() {
        assertEquals(5, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).sorted().count());
        assertEquals(4, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).sorted().skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).sorted().toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 }, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).sorted().skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).sorted().toList());
        assertEquals(N.asList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).sorted().skip(1).toList());
        assertEquals(5, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).sorted().count());
        assertEquals(4, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).sorted().skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).sorted().toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 },
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).sorted().skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).sorted().toList());
        assertEquals(N.asList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).sorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSorted() {
        assertEquals(5, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).reverseSorted().count());
        assertEquals(4, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).reverseSorted().skip(1).count());
        assertArrayEquals(new short[] { 5, 4, 3, 2, 1 }, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).reverseSorted().toArray());
        assertArrayEquals(new short[] { 4, 3, 2, 1 }, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).reverseSorted().skip(1).toArray());
        assertEquals(N.asList((short) 5, (short) 4, (short) 3, (short) 2, (short) 1),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).reverseSorted().toList());
        assertEquals(N.asList((short) 4, (short) 3, (short) 2, (short) 1),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).reverseSorted().skip(1).toList());
        assertEquals(5, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).reverseSorted().count());
        assertEquals(4, ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).reverseSorted().skip(1).count());
        assertArrayEquals(new short[] { 5, 4, 3, 2, 1 },
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).reverseSorted().toArray());
        assertArrayEquals(new short[] { 4, 3, 2, 1 },
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).reverseSorted().skip(1).toArray());
        assertEquals(N.asList((short) 5, (short) 4, (short) 3, (short) 2, (short) 1),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).reverseSorted().toList());
        assertEquals(N.asList((short) 4, (short) 3, (short) 2, (short) 1),
                ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).map(e -> e).reverseSorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMap() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(i -> (short) (i * 2)).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(i -> (short) (i * 2)).skip(1).count());
        assertArrayEquals(new short[] { 2, 4, 6, 8, 10 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(i -> (short) (i * 2)).toArray());
        assertArrayEquals(new short[] { 4, 6, 8, 10 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(i -> (short) (i * 2)).skip(1).toArray());
        assertEquals(N.asList((short) 2, (short) 4, (short) 6, (short) 8, (short) 10),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(i -> (short) (i * 2)).toList());
        assertEquals(N.asList((short) 4, (short) 6, (short) 8, (short) 10),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(i -> (short) (i * 2)).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).map(i -> (short) (i * 2)).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).map(i -> (short) (i * 2)).skip(1).count());
        assertArrayEquals(new short[] { 2, 4, 6, 8, 10 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).map(i -> (short) (i * 2)).toArray());
        assertArrayEquals(new short[] { 4, 6, 8, 10 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).map(i -> (short) (i * 2)).skip(1).toArray());
        assertEquals(N.asList((short) 2, (short) 4, (short) 6, (short) 8, (short) 10),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).map(i -> (short) (i * 2)).toList());
        assertEquals(N.asList((short) 4, (short) 6, (short) 8, (short) 10),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).map(i -> (short) (i * 2)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMap() {
        assertEquals(6, ShortStream.of((short) 1, (short) 2, (short) 3).flatMap(i -> ShortStream.of(i, (short) (i + 10))).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).flatMap(i -> ShortStream.of(i, (short) (i + 10))).skip(2).count());
        assertArrayEquals(new short[] { 1, 11, 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).flatMap(i -> ShortStream.of(i, (short) (i + 10))).toArray());
        assertArrayEquals(new short[] { 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).flatMap(i -> ShortStream.of(i, (short) (i + 10))).skip(2).toArray());
        assertEquals(N.asList((short) 1, (short) 11, (short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).flatMap(i -> ShortStream.of(i, (short) (i + 10))).toList());
        assertEquals(N.asList((short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).flatMap(i -> ShortStream.of(i, (short) (i + 10))).skip(2).toList());
        assertEquals(6, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMap(i -> ShortStream.of(i, (short) (i + 10))).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMap(i -> ShortStream.of(i, (short) (i + 10))).skip(2).count());
        assertArrayEquals(new short[] { 1, 11, 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMap(i -> ShortStream.of(i, (short) (i + 10))).toArray());
        assertArrayEquals(new short[] { 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMap(i -> ShortStream.of(i, (short) (i + 10))).skip(2).toArray());
        assertEquals(N.asList((short) 1, (short) 11, (short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMap(i -> ShortStream.of(i, (short) (i + 10))).toList());
        assertEquals(N.asList((short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatMap(i -> ShortStream.of(i, (short) (i + 10))).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmap() {
        assertEquals(6, ShortStream.of((short) 1, (short) 2, (short) 3).flatmap(i -> new short[] { i, (short) (i + 10) }).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).flatmap(i -> new short[] { i, (short) (i + 10) }).skip(2).count());
        assertArrayEquals(new short[] { 1, 11, 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).flatmap(i -> new short[] { i, (short) (i + 10) }).toArray());
        assertArrayEquals(new short[] { 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).flatmap(i -> new short[] { i, (short) (i + 10) }).skip(2).toArray());
        assertEquals(N.asList((short) 1, (short) 11, (short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).flatmap(i -> new short[] { i, (short) (i + 10) }).toList());
        assertEquals(N.asList((short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).flatmap(i -> new short[] { i, (short) (i + 10) }).skip(2).toList());
        assertEquals(6, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatmap(i -> new short[] { i, (short) (i + 10) }).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatmap(i -> new short[] { i, (short) (i + 10) }).skip(2).count());
        assertArrayEquals(new short[] { 1, 11, 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatmap(i -> new short[] { i, (short) (i + 10) }).toArray());
        assertArrayEquals(new short[] { 2, 12, 3, 13 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatmap(i -> new short[] { i, (short) (i + 10) }).skip(2).toArray());
        assertEquals(N.asList((short) 1, (short) 11, (short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatmap(i -> new short[] { i, (short) (i + 10) }).toList());
        assertEquals(N.asList((short) 2, (short) 12, (short) 3, (short) 13),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).flatmap(i -> new short[] { i, (short) (i + 10) }).skip(2).toList());
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
        assertEquals(N.asList((short) 1, (short) 3, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .toList());
        assertEquals(N.asList((short) 3, (short) 5),
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
        assertEquals(N.asList((short) 1, (short) 3, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .toList());
        assertEquals(N.asList((short) 3, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .mapPartial(i -> i % 2 == 1 ? OptionalShort.of(i) : OptionalShort.empty())
                        .skip(1)
                        .toList());
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
        assertEquals(N.asList((short) 3, (short) 9, (short) 14),
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7).rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.asList((short) 9, (short) 14),
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
        assertEquals(N.asList((short) 3, (short) 9, (short) 14),
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7)
                        .map(e -> e)
                        .rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .toList());
        assertEquals(N.asList((short) 9, (short) 14),
                ShortStream.of((short) 1, (short) 2, (short) 4, (short) 5, (short) 7)
                        .map(e -> e)
                        .rangeMap((a, b) -> b - a <= 1, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterCollapse() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).skip(1).count());
        assertArrayEquals(new short[] { 3, 5, 7 },
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 5, 7 },
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).skip(1).toArray());
        assertEquals(N.asList((short) 3, (short) 5, (short) 7),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.asList((short) 5, (short) 7),
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
        assertEquals(N.asList((short) 3, (short) 5, (short) 7),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.asList((short) 5, (short) 7),
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
        assertEquals(N.asList((short) 3, (short) 12),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.asList((short) 12),
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
        assertEquals(N.asList((short) 3, (short) 12),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7).map(e -> e).collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.asList((short) 12),
                ShortStream.of((short) 1, (short) 2, (short) 5, (short) 7)
                        .map(e -> e)
                        .collapse((a, b, c) -> c - a <= 2, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterScan() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((a, b) -> (short) (a + b)).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((a, b) -> (short) (a + b)).skip(1).count());
        assertArrayEquals(new short[] { 1, 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((a, b) -> (short) (a + b)).skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((a, b) -> (short) (a + b)).toList());
        assertEquals(N.asList((short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((a, b) -> (short) (a + b)).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((a, b) -> (short) (a + b)).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((a, b) -> (short) (a + b)).skip(1).count());
        assertArrayEquals(new short[] { 1, 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((a, b) -> (short) (a + b)).skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((a, b) -> (short) (a + b)).toList());
        assertEquals(N.asList((short) 3, (short) 6, (short) 10, (short) 15),
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
        assertEquals(N.asList((short) 1, (short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 0, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.asList((short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 0, (a, b) -> (short) (a + b)).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 0, (a, b) -> (short) (a + b)).count());
        assertEquals(4,
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 0, (a, b) -> (short) (a + b)).skip(1).count());
        assertArrayEquals(new short[] { 1, 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 0, (a, b) -> (short) (a + b)).toArray());
        assertArrayEquals(new short[] { 3, 6, 10, 15 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 0, (a, b) -> (short) (a + b)).skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 3, (short) 6, (short) 10, (short) 15),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 0, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.asList((short) 3, (short) 6, (short) 10, (short) 15),
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
        assertEquals(N.asList((short) 10, (short) 11, (short) 13, (short) 16, (short) 20, (short) 25),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).scan((short) 10, true, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.asList((short) 11, (short) 13, (short) 16, (short) 20, (short) 25),
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
        assertEquals(N.asList((short) 10, (short) 11, (short) 13, (short) 16, (short) 20, (short) 25),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).scan((short) 10, true, (a, b) -> (short) (a + b)).toList());
        assertEquals(N.asList((short) 11, (short) 13, (short) 16, (short) 20, (short) 25),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                        .map(e -> e)
                        .scan((short) 10, true, (a, b) -> (short) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterPrepend() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3).prepend((short) 10, (short) 11).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).prepend((short) 10, (short) 11).skip(1).count());
        assertArrayEquals(new short[] { 10, 11, 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3).prepend((short) 10, (short) 11).toArray());
        assertArrayEquals(new short[] { 11, 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3).prepend((short) 10, (short) 11).skip(1).toArray());
        assertEquals(N.asList((short) 10, (short) 11, (short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3).prepend((short) 10, (short) 11).toList());
        assertEquals(N.asList((short) 11, (short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3).prepend((short) 10, (short) 11).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).prepend((short) 10, (short) 11).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).prepend((short) 10, (short) 11).skip(1).count());
        assertArrayEquals(new short[] { 10, 11, 1, 2, 3 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).prepend((short) 10, (short) 11).toArray());
        assertArrayEquals(new short[] { 11, 1, 2, 3 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).prepend((short) 10, (short) 11).skip(1).toArray());
        assertEquals(N.asList((short) 10, (short) 11, (short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).prepend((short) 10, (short) 11).toList());
        assertEquals(N.asList((short) 11, (short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).prepend((short) 10, (short) 11).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppend() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3).append((short) 10, (short) 11).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).append((short) 10, (short) 11).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 10, 11 }, ShortStream.of((short) 1, (short) 2, (short) 3).append((short) 10, (short) 11).toArray());
        assertArrayEquals(new short[] { 2, 3, 10, 11 }, ShortStream.of((short) 1, (short) 2, (short) 3).append((short) 10, (short) 11).skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 2, (short) 3, (short) 10, (short) 11),
                ShortStream.of((short) 1, (short) 2, (short) 3).append((short) 10, (short) 11).toList());
        assertEquals(N.asList((short) 2, (short) 3, (short) 10, (short) 11),
                ShortStream.of((short) 1, (short) 2, (short) 3).append((short) 10, (short) 11).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).append((short) 10, (short) 11).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).append((short) 10, (short) 11).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 10, 11 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).append((short) 10, (short) 11).toArray());
        assertArrayEquals(new short[] { 2, 3, 10, 11 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).append((short) 10, (short) 11).skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 2, (short) 3, (short) 10, (short) 11),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).append((short) 10, (short) 11).toList());
        assertEquals(N.asList((short) 2, (short) 3, (short) 10, (short) 11),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).append((short) 10, (short) 11).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmpty() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3).appendIfEmpty((short) 10, (short) 11).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3).appendIfEmpty((short) 10, (short) 11).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3).appendIfEmpty((short) 10, (short) 11).toArray());
        assertArrayEquals(new short[] { 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3).appendIfEmpty((short) 10, (short) 11).skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 2, (short) 3), ShortStream.of((short) 1, (short) 2, (short) 3).appendIfEmpty((short) 10, (short) 11).toList());
        assertEquals(N.asList((short) 2, (short) 3), ShortStream.of((short) 1, (short) 2, (short) 3).appendIfEmpty((short) 10, (short) 11).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).appendIfEmpty((short) 10, (short) 11).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).appendIfEmpty((short) 10, (short) 11).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).appendIfEmpty((short) 10, (short) 11).toArray());
        assertArrayEquals(new short[] { 2, 3 },
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).appendIfEmpty((short) 10, (short) 11).skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).appendIfEmpty((short) 10, (short) 11).toList());
        assertEquals(N.asList((short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3).map(e -> e).appendIfEmpty((short) 10, (short) 11).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTop() {
        assertEquals(3, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3).skip(1).count());
        assertArrayEquals(new short[] { 3, 5, 4 }, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3).toArray());
        assertArrayEquals(new short[] { 5, 4 }, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3).skip(1).toArray());
        assertEquals(N.asList((short) 3, (short) 5, (short) 4), ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3).toList());
        assertEquals(N.asList((short) 5, (short) 4), ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3).skip(1).count());
        assertArrayEquals(new short[] { 3, 5, 4 }, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3).toArray());
        assertArrayEquals(new short[] { 5, 4 }, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3).skip(1).toArray());
        assertEquals(N.asList((short) 3, (short) 5, (short) 4),
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3).toList());
        assertEquals(N.asList((short) 5, (short) 4), ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTopWithComparator() {
        assertEquals(3, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3, Comparator.reverseOrder()).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3, Comparator.reverseOrder()).skip(1).count());
        assertArrayEquals(new short[] { 3, 1, 2 },
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3, Comparator.reverseOrder()).toArray());
        assertArrayEquals(new short[] { 1, 2 },
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3, Comparator.reverseOrder()).skip(1).toArray());
        assertEquals(N.asList((short) 3, (short) 1, (short) 2),
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3, Comparator.reverseOrder()).toList());
        assertEquals(N.asList((short) 1, (short) 2),
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).top(3, Comparator.reverseOrder()).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3, Comparator.reverseOrder()).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3, Comparator.reverseOrder()).skip(1).count());
        assertArrayEquals(new short[] { 3, 1, 2 },
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3, Comparator.reverseOrder()).toArray());
        assertArrayEquals(new short[] { 1, 2 },
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3, Comparator.reverseOrder()).skip(1).toArray());
        assertEquals(N.asList((short) 3, (short) 1, (short) 2),
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3, Comparator.reverseOrder()).toList());
        assertEquals(N.asList((short) 1, (short) 2),
                ShortStream.of((short) 1, (short) 5, (short) 3, (short) 2, (short) 4).map(e -> e).top(3, Comparator.reverseOrder()).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkip() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2).skip(1).toArray());
        assertEquals(N.asList((short) 3, (short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2).toList());
        assertEquals(N.asList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2).skip(1).toArray());
        assertEquals(N.asList((short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2).toList());
        assertEquals(N.asList((short) 4, (short) 5),
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
        assertEquals(N.asList((short) 3, (short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2, i -> {
        }).toList());
        assertEquals(N.asList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).skip(2, i -> {
        }).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2, i -> {
        }).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2, i -> {
        }).skip(1).count());
        assertArrayEquals(new short[] { 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2, i -> {
        }).toArray());
        assertArrayEquals(new short[] { 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2, i -> {
        }).skip(1).toArray());
        assertEquals(N.asList((short) 3, (short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2, i -> {
        }).toList());
        assertEquals(N.asList((short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).skip(2, i -> {
        }).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterLimit() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(3).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(3).toArray());
        assertArrayEquals(new short[] { 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(3).skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 2, (short) 3), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(3).toList());
        assertEquals(N.asList((short) 2, (short) 3), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).limit(3).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).limit(3).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).limit(3).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).limit(3).toArray());
        assertArrayEquals(new short[] { 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).limit(3).skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).limit(3).toList());
        assertEquals(N.asList((short) 2, (short) 3),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).limit(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterStep() {
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).step(2).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).step(2).skip(1).count());
        assertArrayEquals(new short[] { 1, 3, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).step(2).toArray());
        assertArrayEquals(new short[] { 3, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).step(2).skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 3, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).step(2).toList());
        assertEquals(N.asList((short) 3, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).step(2).skip(1).toList());
        assertEquals(3, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).map(e -> e).step(2).count());
        assertEquals(2, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).map(e -> e).step(2).skip(1).count());
        assertArrayEquals(new short[] { 1, 3, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).map(e -> e).step(2).toArray());
        assertArrayEquals(new short[] { 3, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).map(e -> e).step(2).skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 3, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).map(e -> e).step(2).toList());
        assertEquals(N.asList((short) 3, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6).map(e -> e).step(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRateLimited() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(100.0).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(100.0).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(100.0).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(100.0).skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(100.0).toList());
        assertEquals(N.asList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(100.0).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(100.0).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(100.0).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(100.0).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(100.0).skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(100.0).toList());
        assertEquals(N.asList((short) 2, (short) 3, (short) 4, (short) 5),
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
        assertEquals(N.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(rateLimiter).toList());
        assertEquals(N.asList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).rateLimited(rateLimiter).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(rateLimiter).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(rateLimiter).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(rateLimiter).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(rateLimiter).skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(rateLimiter).toList());
        assertEquals(N.asList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).rateLimited(rateLimiter).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDelay() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).delay(Duration.ofMillis(1)).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).delay(Duration.ofMillis(1)).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).delay(Duration.ofMillis(1)).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).delay(Duration.ofMillis(1)).skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).delay(Duration.ofMillis(1)).toList());
        assertEquals(N.asList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).delay(Duration.ofMillis(1)).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).delay(Duration.ofMillis(1)).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).delay(Duration.ofMillis(1)).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).delay(Duration.ofMillis(1)).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 },
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).delay(Duration.ofMillis(1)).skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).delay(Duration.ofMillis(1)).toList());
        assertEquals(N.asList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).delay(Duration.ofMillis(1)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOnEach() {
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).onEach(i -> {
        }).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).onEach(i -> {
        }).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).onEach(i -> {
        }).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).onEach(i -> {
        }).skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).onEach(i -> {
                }).toList());
        assertEquals(N.asList((short) 2, (short) 3, (short) 4, (short) 5), ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).onEach(i -> {
        }).skip(1).toList());
        assertEquals(5, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).onEach(i -> {
        }).count());
        assertEquals(4, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).onEach(i -> {
        }).skip(1).count());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).onEach(i -> {
        }).toArray());
        assertArrayEquals(new short[] { 2, 3, 4, 5 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).onEach(i -> {
        }).skip(1).toArray());
        assertEquals(N.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).onEach(i -> {
                }).toList());
        assertEquals(N.asList((short) 2, (short) 3, (short) 4, (short) 5),
                ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(e -> e).onEach(i -> {
                }).skip(1).toList());
    }
}
