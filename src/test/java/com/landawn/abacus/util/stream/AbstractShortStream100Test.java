package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.IndexedShort;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalShort;

@Tag("new-test")
public class AbstractShortStream100Test extends TestBase {

    private ShortStream stream;
    private ShortStream stream2;

    @BeforeEach
    public void setUp() {
        stream = createShortStream(new short[] { 1, 2, 3, 4, 5 });
        stream2 = createShortStream(new short[] { 1, 2, 3, 4, 5 });
    }

    protected ShortStream createShortStream(short... elements) {
        return ShortStream.of(elements);
    }

    @Test
    public void testRateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(10.0);
        ShortStream limitedStream = stream.rateLimited(rateLimiter);
        assertNotNull(limitedStream);

        assertThrows(IllegalArgumentException.class, () -> stream.rateLimited(null));
    }

    @Test
    public void testDelay() {
        Duration delay = Duration.ofMillis(10);
        ShortStream delayedStream = stream.delay(delay);
        assertNotNull(delayedStream);

        long startTime = System.currentTimeMillis();
        delayedStream.forEach(v -> {
        });
        long endTime = System.currentTimeMillis();
        assertTrue(endTime - startTime >= 40);

        assertThrows(IllegalArgumentException.class, () -> stream2.delay((Duration) null));
    }

    @Test
    public void testSkipUntil() {
        ShortStream result = createShortStream(new short[] { 1, 2, 3, 4, 5 }).skipUntil(v -> v > 3);
        assertArrayEquals(new short[] { 4, 5 }, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3 }).skipUntil(v -> v > 10);
        assertArrayEquals(new short[] {}, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3 }).skipUntil(v -> v >= 1);
        assertArrayEquals(new short[] { 1, 2, 3 }, result.toArray());
    }

    @Test
    public void testDistinct() {
        ShortStream result = createShortStream(new short[] { 1, 2, 2, 3, 3, 3, 4 }).distinct();
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, result.toArray());

        result = createShortStream(new short[] {}).distinct();
        assertArrayEquals(new short[] {}, result.toArray());

        result = createShortStream(new short[] { 5 }).distinct();
        assertArrayEquals(new short[] { 5 }, result.toArray());
    }

    @Test
    public void testFlatmap() {
        ShortStream result = createShortStream(new short[] { 1, 2, 3 }).flatmap(v -> new short[] { v, (short) (v * 10) });
        assertArrayEquals(new short[] { 1, 10, 2, 20, 3, 30 }, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3 }).flatmap(v -> new short[] {});
        assertArrayEquals(new short[] {}, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3 }).flatmap(v -> v == 2 ? new short[] {} : new short[] { v });
        assertArrayEquals(new short[] { 1, 3 }, result.toArray());
    }

    @Test
    public void testFlatmapToObj() {
        Stream<String> result = createShortStream(new short[] { 1, 2, 3 }).flatmapToObj(v -> Arrays.asList(String.valueOf(v), String.valueOf(v * 10)));
        assertArrayEquals(new String[] { "1", "10", "2", "20", "3", "30" }, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3 }).flatmapToObj(v -> Collections.emptyList());
        assertArrayEquals(new String[] {}, result.toArray());
    }

    @Test
    public void testFlattMapToObj() {
        Stream<String> result = createShortStream(new short[] { 1, 2, 3 }).flattmapToObj(v -> new String[] { String.valueOf(v), String.valueOf(v * 10) });
        assertArrayEquals(new String[] { "1", "10", "2", "20", "3", "30" }, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3 }).flattmapToObj(v -> new String[] {});
        assertArrayEquals(new String[] {}, result.toArray());
    }

    @Test
    public void testMapPartial() {
        ShortStream result = createShortStream(new short[] { 1, 2, 3, 4, 5 })
                .mapPartial(v -> v % 2 == 0 ? OptionalShort.of((short) (v * 2)) : OptionalShort.empty());
        assertArrayEquals(new short[] { 4, 8 }, result.toArray());

        result = createShortStream(new short[] { 1, 3, 5 }).mapPartial(v -> OptionalShort.empty());
        assertArrayEquals(new short[] {}, result.toArray());

        result = createShortStream(new short[] { 2, 4, 6 }).mapPartial(v -> OptionalShort.of(v));
        assertArrayEquals(new short[] { 2, 4, 6 }, result.toArray());
    }

    @Test
    public void testRangeMap() {
        ShortStream result = createShortStream(new short[] { 1, 2, 3, 10, 11, 20 }).rangeMap((a, b) -> Math.abs(b - a) <= 1,
                (first, last) -> (short) (first + last));
        assertArrayEquals(new short[] { 3, 6, 21, 40 }, result.toArray());

        result = createShortStream(new short[] { 1, 5, 10 }).rangeMap((a, b) -> false, (first, last) -> (short) (first * 2));
        assertArrayEquals(new short[] { 2, 10, 20 }, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3, 4 }).rangeMap((a, b) -> true, (first, last) -> last);
        assertArrayEquals(new short[] { 4 }, result.toArray());
    }

    @Test
    public void testRangeMapToObj() {
        Stream<String> result = createShortStream(new short[] { 1, 2, 3, 10, 11, 20 }).rangeMapToObj((a, b) -> Math.abs(b - a) <= 1,
                (first, last) -> first + "-" + last);
        assertArrayEquals(new String[] { "1-2", "3-3", "10-11", "20-20" }, result.toArray());

        result = createShortStream(new short[] {}).rangeMapToObj((a, b) -> true, (first, last) -> first + "-" + last);
        assertArrayEquals(new String[] {}, result.toArray());
    }

    @Test
    public void testCollapseBiPredicate() {
        Stream<ShortList> result = createShortStream(new short[] { 1, 2, 2, 3, 3, 3, 4 }).collapse((a, b) -> a == b);
        ShortList[] lists = result.toArray(ShortList[]::new);
        assertEquals(4, lists.length);
        assertArrayEquals(new short[] { 1 }, lists[0].toArray());
        assertArrayEquals(new short[] { 2, 2 }, lists[1].toArray());
        assertArrayEquals(new short[] { 3, 3, 3 }, lists[2].toArray());
        assertArrayEquals(new short[] { 4 }, lists[3].toArray());

        result = createShortStream(new short[] { 1, 2, 3 }).collapse((a, b) -> false);
        lists = result.toArray(ShortList[]::new);
        assertEquals(3, lists.length);
        for (ShortList list : lists) {
            assertEquals(1, list.size());
        }
    }

    @Test
    public void testCollapseBiPredicateWithMerge() {
        ShortStream result = createShortStream(new short[] { 1, 2, 5, 6, 10 }).collapse((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 3, 11, 10 }, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3, 4 }).collapse((a, b) -> true, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 10 }, result.toArray());
    }

    @Test
    public void testCollapseTriPredicate() {
        ShortStream result = createShortStream(new short[] { 1, 2, 3, 10, 11, 12 }).collapse((first, last, next) -> next - first <= 3,
                (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 6, 33 }, result.toArray());

        result = createShortStream(new short[] { 1, 5, 10 }).collapse((first, last, next) -> false, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 1, 5, 10 }, result.toArray());
    }

    @Test
    public void testScan() {
        ShortStream result = createShortStream(new short[] { 1, 2, 3, 4 }).scan((a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 1, 3, 6, 10 }, result.toArray());

        result = createShortStream(new short[] { 5 }).scan((a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 5 }, result.toArray());

        result = createShortStream(new short[] {}).scan((a, b) -> (short) (a + b));
        assertArrayEquals(new short[] {}, result.toArray());
    }

    @Test
    public void testScanWithInit() {
        ShortStream result = createShortStream(new short[] { 1, 2, 3, 4 }).scan((short) 10, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 11, 13, 16, 20 }, result.toArray());

        result = createShortStream(new short[] {}).scan((short) 10, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] {}, result.toArray());
    }

    @Test
    public void testScanWithInitIncluded() {
        ShortStream result = createShortStream(new short[] { 1, 2, 3 }).scan((short) 10, true, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 10, 11, 13, 16 }, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3 }).scan((short) 10, false, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 11, 13, 16 }, result.toArray());

        result = createShortStream(new short[] {}).scan((short) 10, true, (a, b) -> (short) (a + b));
        assertArrayEquals(new short[] { 10 }, result.toArray());
    }

    @Test
    public void testTop() {
        ShortStream result = createShortStream(new short[] { 3, 1, 4, 1, 5, 9, 2, 6 }).top(3);
        short[] topElements = result.toArray();
        Arrays.sort(topElements);
        assertArrayEquals(new short[] { 5, 6, 9 }, topElements);

        result = createShortStream(new short[] { 1, 2, 3 }).top(5);
        assertArrayEquals(new short[] { 1, 2, 3 }, result.toArray());

        assertThrows(IllegalArgumentException.class, () -> createShortStream(new short[] { 1, 2, 3 }).top(0));
    }

    @Test
    public void testIntersection() {
        Collection<Short> collection = Arrays.asList((short) 2, (short) 3, (short) 4, (short) 5, (short) 3);
        ShortStream result = createShortStream(new short[] { 1, 2, 3, 2, 3 }).intersection(collection);
        assertArrayEquals(new short[] { 2, 3, 3 }, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3 }).intersection(Collections.emptyList());
        assertArrayEquals(new short[] {}, result.toArray());

        collection = Arrays.asList((short) 6, (short) 7);
        result = createShortStream(new short[] { 1, 2, 3 }).intersection(collection);
        assertArrayEquals(new short[] {}, result.toArray());
    }

    @Test
    public void testDifference() {
        Collection<Short> collection = Arrays.asList((short) 2, (short) 3);
        ShortStream result = createShortStream(new short[] { 1, 2, 3, 4, 2 }).difference(collection);
        assertArrayEquals(new short[] { 1, 4, 2 }, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3 }).difference(Collections.emptyList());
        assertArrayEquals(new short[] { 1, 2, 3 }, result.toArray());

        collection = Arrays.asList((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        result = createShortStream(new short[] { 1, 2, 3 }).difference(collection);
        assertArrayEquals(new short[] {}, result.toArray());
    }

    @Test
    public void testSymmetricDifference() {
        Collection<Short> collection = Arrays.asList((short) 3, (short) 4, (short) 5);
        ShortStream result = createShortStream(new short[] { 1, 2, 3 }).symmetricDifference(collection);
        short[] resultArray = result.toArray();
        Arrays.sort(resultArray);
        assertArrayEquals(new short[] { 1, 2, 4, 5 }, resultArray);

        result = createShortStream(new short[] { 1, 2, 3 }).symmetricDifference(Collections.emptyList());
        assertArrayEquals(new short[] { 1, 2, 3 }, result.toArray());

        collection = Arrays.asList((short) 1, (short) 2, (short) 3);
        result = createShortStream(new short[] { 1, 2, 3 }).symmetricDifference(collection);
        assertArrayEquals(new short[] {}, result.toArray());
    }

    @Test
    public void testReversed() {
        ShortStream result = createShortStream(new short[] { 1, 2, 3, 4, 5 }).reversed();
        assertArrayEquals(new short[] { 5, 4, 3, 2, 1 }, result.toArray());

        result = createShortStream(new short[] {}).reversed();
        assertArrayEquals(new short[] {}, result.toArray());

        result = createShortStream(new short[] { 42 }).reversed();
        assertArrayEquals(new short[] { 42 }, result.toArray());
    }

    @Test
    public void testRotated() {
        ShortStream result = createShortStream(new short[] { 1, 2, 3, 4, 5 }).rotated(2);
        assertArrayEquals(new short[] { 4, 5, 1, 2, 3 }, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3, 4, 5 }).rotated(-2);
        assertArrayEquals(new short[] { 3, 4, 5, 1, 2 }, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3, 4, 5 }).rotated(5);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3 }).rotated(7);
        assertArrayEquals(new short[] { 3, 1, 2 }, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3 }).rotated(0);
        assertArrayEquals(new short[] { 1, 2, 3 }, result.toArray());
    }

    @Test
    public void testShuffled() {
        Random rnd = new Random(42);
        short[] original = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        ShortStream result = createShortStream(original).shuffled(rnd);
        short[] shuffled = result.toArray();

        assertEquals(original.length, shuffled.length);
        Arrays.sort(shuffled);
        assertArrayEquals(original, shuffled);

        assertThrows(IllegalArgumentException.class, () -> createShortStream(new short[] { 1, 2, 3 }).shuffled(null));
    }

    @Test
    public void testSorted() {
        ShortStream result = createShortStream(new short[] { 3, 1, 4, 1, 5, 9, 2, 6 }).sorted();
        assertArrayEquals(new short[] { 1, 1, 2, 3, 4, 5, 6, 9 }, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3, 4, 5 }).sorted();
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result.toArray());

        result = createShortStream(new short[] {}).sorted();
        assertArrayEquals(new short[] {}, result.toArray());
    }

    @Test
    public void testReverseSorted() {
        ShortStream result = createShortStream(new short[] { 3, 1, 4, 1, 5, 9, 2, 6 }).reverseSorted();
        assertArrayEquals(new short[] { 9, 6, 5, 4, 3, 2, 1, 1 }, result.toArray());

        result = createShortStream(new short[] { 5, 4, 3, 2, 1 }).reverseSorted();
        assertArrayEquals(new short[] { 5, 4, 3, 2, 1 }, result.toArray());

        result = createShortStream(new short[] { 42 }).reverseSorted();
        assertArrayEquals(new short[] { 42 }, result.toArray());
    }

    @Test
    public void testCycled() {
        ShortStream result = createShortStream(new short[] { 1, 2, 3 }).cycled().limit(10);
        assertArrayEquals(new short[] { 1, 2, 3, 1, 2, 3, 1, 2, 3, 1 }, result.toArray());

        result = createShortStream(new short[] {}).cycled().limit(5);
        assertArrayEquals(new short[] {}, result.toArray());
    }

    @Test
    public void testCycledWithRounds() {
        ShortStream result = createShortStream(new short[] { 1, 2, 3 }).cycled(0);
        assertArrayEquals(new short[] {}, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3 }).cycled(1);
        assertArrayEquals(new short[] { 1, 2, 3 }, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3 }).cycled(3);
        assertArrayEquals(new short[] { 1, 2, 3, 1, 2, 3, 1, 2, 3 }, result.toArray());

        result = createShortStream(new short[] {}).cycled(5);
        assertArrayEquals(new short[] {}, result.toArray());

        assertThrows(IllegalArgumentException.class, () -> createShortStream(new short[] { 1, 2, 3 }).cycled(-1));
    }

    @Test
    public void testIndexed() {
        Stream<IndexedShort> result = createShortStream(new short[] { 10, 20, 30 }).indexed();
        IndexedShort[] indexed = result.toArray(IndexedShort[]::new);

        assertEquals(3, indexed.length);
        assertEquals(10, indexed[0].value());
        assertEquals(0, indexed[0].index());
        assertEquals(20, indexed[1].value());
        assertEquals(1, indexed[1].index());
        assertEquals(30, indexed[2].value());
        assertEquals(2, indexed[2].index());

        result = createShortStream(new short[] {}).indexed();
        assertEquals(0, result.count());
    }

    @Test
    public void testBoxed() {
        Stream<Short> result = createShortStream(new short[] { 1, 2, 3, 4, 5 }).boxed();
        Short[] boxed = result.toArray(Short[]::new);
        assertArrayEquals(new Short[] { 1, 2, 3, 4, 5 }, boxed);

        result = createShortStream(new short[] {}).boxed();
        assertEquals(0, result.count());
    }

    @Test
    public void testPrepend() {
        ShortStream result = createShortStream(new short[] { 3, 4, 5 }).prepend((short) 1, (short) 2);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3 }).prepend(new short[] {});
        assertArrayEquals(new short[] { 1, 2, 3 }, result.toArray());

        result = createShortStream(new short[] {}).prepend((short) 1, (short) 2);
        assertArrayEquals(new short[] { 1, 2 }, result.toArray());
    }

    @Test
    public void testPrependStream() {
        ShortStream toAdd = createShortStream(new short[] { 1, 2 });
        ShortStream result = createShortStream(new short[] { 3, 4, 5 }).prepend(toAdd);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void testPrependOptional() {
        OptionalShort op = OptionalShort.of((short) 1);
        ShortStream result = createShortStream(new short[] { 2, 3, 4 }).prepend(op);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, result.toArray());

        op = OptionalShort.empty();
        result = createShortStream(new short[] { 2, 3, 4 }).prepend(op);
        assertArrayEquals(new short[] { 2, 3, 4 }, result.toArray());
    }

    @Test
    public void testAppend() {
        ShortStream result = createShortStream(new short[] { 1, 2, 3 }).append((short) 4, (short) 5);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3 }).append(new short[] {});
        assertArrayEquals(new short[] { 1, 2, 3 }, result.toArray());

        result = createShortStream(new short[] {}).append((short) 1, (short) 2);
        assertArrayEquals(new short[] { 1, 2 }, result.toArray());
    }

    @Test
    public void testAppendStream() {
        ShortStream toAdd = createShortStream(new short[] { 4, 5 });
        ShortStream result = createShortStream(new short[] { 1, 2, 3 }).append(toAdd);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result.toArray());
    }

    @Test
    public void testAppendOptional() {
        OptionalShort op = OptionalShort.of((short) 4);
        ShortStream result = createShortStream(new short[] { 1, 2, 3 }).append(op);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, result.toArray());

        op = OptionalShort.empty();
        result = createShortStream(new short[] { 1, 2, 3 }).append(op);
        assertArrayEquals(new short[] { 1, 2, 3 }, result.toArray());
    }

    @Test
    public void testAppendIfEmpty() {
        ShortStream result = createShortStream(new short[] { 1, 2, 3 }).appendIfEmpty((short) 4, (short) 5);
        assertArrayEquals(new short[] { 1, 2, 3 }, result.toArray());

        result = createShortStream(new short[] {}).appendIfEmpty((short) 4, (short) 5);
        assertArrayEquals(new short[] { 4, 5 }, result.toArray());
    }

    @Test
    public void testMergeWith() {
        ShortStream a = createShortStream(new short[] { 1, 3, 5 });
        ShortStream b = createShortStream(new short[] { 2, 4, 6 });
        ShortStream result = a.mergeWith(b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, result.toArray());

        a = createShortStream(new short[] {});
        b = createShortStream(new short[] { 1, 2, 3 });
        result = a.mergeWith(b, (x, y) -> MergeResult.TAKE_FIRST);
        assertArrayEquals(new short[] { 1, 2, 3 }, result.toArray());
    }

    @Test
    public void testZipWith() {
        ShortStream a = createShortStream(new short[] { 1, 2, 3 });
        ShortStream b = createShortStream(new short[] { 4, 5, 6 });
        ShortStream result = a.zipWith(b, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 5, 7, 9 }, result.toArray());

        a = createShortStream(new short[] { 1, 2, 3, 4 });
        b = createShortStream(new short[] { 5, 6 });
        result = a.zipWith(b, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 6, 8 }, result.toArray());
    }

    @Test
    public void testZipWithThree() {
        ShortStream a = createShortStream(new short[] { 1, 2, 3 });
        ShortStream b = createShortStream(new short[] { 4, 5, 6 });
        ShortStream c = createShortStream(new short[] { 7, 8, 9 });
        ShortStream result = a.zipWith(b, c, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 12, 15, 18 }, result.toArray());

        a = createShortStream(new short[] { 1, 2 });
        b = createShortStream(new short[] { 3, 4, 5 });
        c = createShortStream(new short[] { 6 });
        result = a.zipWith(b, c, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 10 }, result.toArray());
    }

    @Test
    public void testZipWithDefaults() {
        ShortStream a = createShortStream(new short[] { 1, 2 });
        ShortStream b = createShortStream(new short[] { 3, 4, 5, 6 });
        ShortStream result = a.zipWith(b, (short) 10, (short) 20, (x, y) -> (short) (x + y));
        assertArrayEquals(new short[] { 4, 6, 15, 16 }, result.toArray());
    }

    @Test
    public void testZipWithThreeDefaults() {
        ShortStream a = createShortStream(new short[] { 1 });
        ShortStream b = createShortStream(new short[] { 2, 3 });
        ShortStream c = createShortStream(new short[] { 4, 5, 6 });
        ShortStream result = a.zipWith(b, c, (short) 10, (short) 20, (short) 30, (x, y, z) -> (short) (x + y + z));
        assertArrayEquals(new short[] { 7, 18, 36 }, result.toArray());
    }

    @Test
    public void testToMap() {
        Map<String, Integer> map = createShortStream(new short[] { 1, 2, 3 }).toMap(v -> "key" + v, v -> (int) v);

        assertEquals(3, map.size());
        assertEquals(1, map.get("key1"));
        assertEquals(2, map.get("key2"));
        assertEquals(3, map.get("key3"));

        assertThrows(IllegalStateException.class, () -> createShortStream(new short[] { 1, 2, 1 }).toMap(v -> "key" + v, v -> (int) v));
    }

    @Test
    public void testToMapWithSupplier() {
        Map<String, Integer> map = createShortStream(new short[] { 1, 2, 3 }).toMap(v -> "key" + v, v -> (int) v, Suppliers.ofLinkedHashMap());

        assertEquals(3, map.size());
        assertEquals(LinkedHashMap.class, map.getClass());
        assertEquals(1, map.get("key1"));
        assertEquals(2, map.get("key2"));
        assertEquals(3, map.get("key3"));
    }

    @Test
    public void testToMapWithMergeFunction() {
        Map<String, Integer> map = createShortStream(new short[] { 1, 2, 1, 3, 2 }).toMap(v -> "key" + v, v -> 1, Integer::sum);

        assertEquals(3, map.size());
        assertEquals(2, map.get("key1"));
        assertEquals(2, map.get("key2"));
        assertEquals(1, map.get("key3"));
    }

    @Test
    public void testToMapWithMergeFunctionAndSupplier() {
        TreeMap<String, Integer> map = createShortStream(new short[] { 1, 2, 1, 3, 2 }).toMap(v -> "key" + v, v -> 1, Integer::sum, TreeMap::new);

        assertEquals(3, map.size());
        assertEquals(TreeMap.class, map.getClass());
        assertEquals(2, map.get("key1"));
        assertEquals(2, map.get("key2"));
        assertEquals(1, map.get("key3"));
    }

    @Test
    public void testGroupTo() {
        Map<Boolean, List<Short>> map = createShortStream(new short[] { 1, 2, 3, 4, 5, 6 }).groupTo(v -> v % 2 == 0, Collectors.toList());

        assertEquals(2, map.size());
        assertEquals(Arrays.asList((short) 2, (short) 4, (short) 6), map.get(true));
        assertEquals(Arrays.asList((short) 1, (short) 3, (short) 5), map.get(false));
    }

    @Test
    public void testGroupToWithSupplier() {
        TreeMap<Boolean, Long> map = createShortStream(new short[] { 1, 2, 3, 4, 5, 6 }).groupTo(v -> v % 2 == 0, Collectors.counting(), TreeMap::new);

        assertEquals(2, map.size());
        assertEquals(TreeMap.class, map.getClass());
        assertEquals(3L, map.get(true));
        assertEquals(3L, map.get(false));
    }

    @Test
    public void testForEachIndexed() {
        List<String> result = new ArrayList<>();
        createShortStream(new short[] { 10, 20, 30 }).forEachIndexed((idx, value) -> result.add(idx + ":" + value));

        assertEquals(Arrays.asList("0:10", "1:20", "2:30"), result);

        result.clear();
        createShortStream(new short[] {}).forEachIndexed((idx, value) -> result.add(idx + ":" + value));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFirst() {
        OptionalShort result = createShortStream(new short[] { 1, 2, 3 }).first();
        assertTrue(result.isPresent());
        assertEquals(1, result.orElseThrow());

        result = createShortStream(new short[] {}).first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast() {
        OptionalShort result = createShortStream(new short[] { 1, 2, 3 }).last();
        assertTrue(result.isPresent());
        assertEquals(3, result.orElseThrow());

        result = createShortStream(new short[] {}).last();
        assertFalse(result.isPresent());

        result = createShortStream(new short[] { 42 }).last();
        assertTrue(result.isPresent());
        assertEquals(42, result.orElseThrow());
    }

    @Test
    public void testOnlyOne() {
        OptionalShort result = createShortStream(new short[] { 42 }).onlyOne();
        assertTrue(result.isPresent());
        assertEquals(42, result.orElseThrow());

        result = createShortStream(new short[] {}).onlyOne();
        assertFalse(result.isPresent());

        assertThrows(TooManyElementsException.class, () -> createShortStream(new short[] { 1, 2 }).onlyOne());
    }

    @Test
    public void testFindAny() {
        OptionalShort result = createShortStream(new short[] { 1, 2, 3, 4, 5 }).findAny(v -> v > 3);
        assertTrue(result.isPresent());
        assertEquals(4, result.orElseThrow());

        result = createShortStream(new short[] { 1, 2, 3 }).findAny(v -> v > 10);
        assertFalse(result.isPresent());

        result = createShortStream(new short[] {}).findAny(v -> true);
        assertFalse(result.isPresent());
    }

    @Test
    public void testPercentiles() {
        Optional<Map<Percentage, Short>> result = createShortStream(new short[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }).percentiles();

        assertTrue(result.isPresent());
        Map<Percentage, Short> percentiles = result.get();
        assertNotNull(percentiles);

        result = createShortStream(new short[] {}).percentiles();
        assertFalse(result.isPresent());
    }

    @Test
    public void testSummarizeAndPercentiles() {
        Pair<ShortSummaryStatistics, Optional<Map<Percentage, Short>>> result = createShortStream(new short[] { 1, 2, 3, 4, 5 }).summarizeAndPercentiles();

        assertNotNull(result);
        ShortSummaryStatistics stats = result.left();
        assertEquals(5, stats.getCount());
        assertEquals(1, stats.getMin());
        assertEquals(5, stats.getMax());
        assertEquals(15, stats.getSum());

        assertTrue(result.right().isPresent());

        result = createShortStream(new short[] {}).summarizeAndPercentiles();
        assertEquals(0, result.left().getCount());
        assertFalse(result.right().isPresent());
    }

    @Test
    public void testJoin() {
        String result = createShortStream(new short[] { 1, 2, 3 }).join(", ", "[", "]");
        assertEquals("[1, 2, 3]", result);

        result = createShortStream(new short[] {}).join(", ", "[", "]");
        assertEquals("[]", result);

        result = createShortStream(new short[] { 42 }).join(", ", "[", "]");
        assertEquals("[42]", result);

        result = createShortStream(new short[] { 1, 2, 3 }).join(" - ", "", "");
        assertEquals("1 - 2 - 3", result);
    }

    @Test
    public void testJoinTo() {
        Joiner joiner = Joiner.with(", ", "(", ")");
        Joiner result = createShortStream(new short[] { 1, 2, 3 }).joinTo(joiner);

        assertSame(joiner, result);
        assertEquals("(1, 2, 3)", joiner.toString());

        joiner = Joiner.with("|");
        joiner.append("start");
        createShortStream(new short[] { 4, 5, 6 }).joinTo(joiner);
        assertEquals("start|4|5|6", joiner.toString());
    }

    @Test
    public void testCollectWithSupplierAndAccumulator() {
        List<Short> list = createShortStream(new short[] { 1, 2, 3 }).collect(ArrayList::new, (l, v) -> l.add(v));
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), list);

        StringBuilder sb = createShortStream(new short[] { 1, 2, 3 }).collect(StringBuilder::new, (builder, v) -> builder.append(v).append(" "));
        assertEquals("1 2 3 ", sb.toString());

        list = createShortStream(new short[] {}).collect(ArrayList::new, (l, v) -> l.add(v));
        assertTrue(list.isEmpty());
    }

    @Test
    public void testIterator() {
        ShortIterator iter = createShortStream(new short[] { 1, 2, 3 }).iterator();

        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextShort());
        assertTrue(iter.hasNext());
        assertEquals(2, iter.nextShort());
        assertTrue(iter.hasNext());
        assertEquals(3, iter.nextShort());
        assertFalse(iter.hasNext());

        iter = createShortStream(new short[] {}).iterator();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipWithAction() {
        List<Short> skipped = new ArrayList<>();
        ShortStream result = createShortStream(new short[] { 1, 2, 3, 4, 5 }).skip(3, v -> skipped.add(v));

        assertArrayEquals(new short[] { 4, 5 }, result.toArray());
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), skipped);

        skipped.clear();
        result = createShortStream(new short[] { 1, 2, 3 }).skip(0, v -> skipped.add(v));
        assertArrayEquals(new short[] { 1, 2, 3 }, result.toArray());
        assertTrue(skipped.isEmpty());

        skipped.clear();
        result = createShortStream(new short[] { 1, 2, 3 }).skip(5, v -> skipped.add(v));
        assertArrayEquals(new short[] {}, result.toArray());
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), skipped);
    }

    @Test
    public void testFilterWithAction() {
        List<Short> dropped = new ArrayList<>();
        ShortStream result = createShortStream(new short[] { 1, 2, 3, 4, 5 }).filter(v -> v % 2 == 0, v -> dropped.add(v));

        assertArrayEquals(new short[] { 2, 4 }, result.toArray());
        assertEquals(Arrays.asList((short) 1, (short) 3, (short) 5), dropped);

        dropped.clear();
        result = createShortStream(new short[] { 1, 3, 5 }).filter(v -> v % 2 == 0, v -> dropped.add(v));
        assertArrayEquals(new short[] {}, result.toArray());
        assertEquals(Arrays.asList((short) 1, (short) 3, (short) 5), dropped);
    }

    @Test
    public void testDropWhileWithAction() {
        List<Short> dropped = new ArrayList<>();
        ShortStream result = createShortStream(new short[] { 1, 2, 3, 4, 5 }).dropWhile(v -> v < 3, v -> dropped.add(v));

        assertArrayEquals(new short[] { 3, 4, 5 }, result.toArray());
        assertEquals(Arrays.asList((short) 1, (short) 2), dropped);

        dropped.clear();
        result = createShortStream(new short[] { 3, 4, 5 }).dropWhile(v -> v < 1, v -> dropped.add(v));
        assertArrayEquals(new short[] { 3, 4, 5 }, result.toArray());
        assertTrue(dropped.isEmpty());

        dropped.clear();
        result = createShortStream(new short[] { 1, 2, 3 }).dropWhile(v -> v < 10, v -> dropped.add(v));
        assertArrayEquals(new short[] {}, result.toArray());
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), dropped);
    }

    @Test
    public void testStep() {
        ShortStream result = createShortStream(new short[] { 1, 2, 3, 4, 5 }).step(1);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3, 4, 5, 6 }).step(2);
        assertArrayEquals(new short[] { 1, 3, 5 }, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3, 4, 5, 6, 7 }).step(3);
        assertArrayEquals(new short[] { 1, 4, 7 }, result.toArray());

        result = createShortStream(new short[] { 1, 2, 3 }).step(5);
        assertArrayEquals(new short[] { 1 }, result.toArray());

        result = createShortStream(new short[] {}).step(2);
        assertArrayEquals(new short[] {}, result.toArray());

        assertThrows(IllegalArgumentException.class, () -> createShortStream(new short[] { 1, 2, 3 }).step(0));
        assertThrows(IllegalArgumentException.class, () -> createShortStream(new short[] { 1, 2, 3 }).step(-1));
    }
}
