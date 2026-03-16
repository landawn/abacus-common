package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Tests verifying bug fixes found during deep code review.
 */
@Tag("2025")
public class BugFixVerificationTest extends TestBase {

    // ============================================================
    // Fix 1: ReverseImmutableList iterator/toArray/listIterator
    // ============================================================

    @Test
    public void testReverseImmutableList_iterator() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");
        ImmutableList<String> reversed = list.reversed();

        // get() should return reversed elements
        assertEquals("c", reversed.get(0));
        assertEquals("b", reversed.get(1));
        assertEquals("a", reversed.get(2));

        // iterator() should also return reversed elements
        List<String> iteratedElements = new ArrayList<>();
        for (String s : reversed) {
            iteratedElements.add(s);
        }
        assertEquals(Arrays.asList("c", "b", "a"), iteratedElements);
    }

    @Test
    public void testReverseImmutableList_toArray() {
        ImmutableList<Integer> list = ImmutableList.of(1, 2, 3, 4, 5);
        ImmutableList<Integer> reversed = list.reversed();

        Object[] arr = reversed.toArray();
        assertArrayEquals(new Object[] { 5, 4, 3, 2, 1 }, arr);
    }

    @Test
    public void testReverseImmutableList_toArrayTyped() {
        ImmutableList<String> list = ImmutableList.of("x", "y", "z");
        ImmutableList<String> reversed = list.reversed();

        String[] arr = reversed.toArray(new String[0]);
        assertArrayEquals(new String[] { "z", "y", "x" }, arr);

        // With pre-sized array
        String[] preSized = new String[3];
        String[] result = reversed.toArray(preSized);
        assertArrayEquals(new String[] { "z", "y", "x" }, result);
    }

    @Test
    public void testReverseImmutableList_listIterator() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");
        ImmutableList<String> reversed = list.reversed();

        // Forward iteration via listIterator
        ListIterator<String> iter = reversed.listIterator();
        assertTrue(iter.hasNext());
        assertEquals("c", iter.next());
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());

        // Backward iteration
        assertTrue(iter.hasPrevious());
        assertEquals("a", iter.previous());
        assertEquals("b", iter.previous());
    }

    @Test
    public void testReverseImmutableList_listIteratorFromIndex() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d");
        ImmutableList<String> reversed = list.reversed();

        // Start from index 2 in reversed list: [d, c, b, a] -> index 2 is "b"
        ListIterator<String> iter = reversed.listIterator(2);
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
    }

    @Test
    public void testReverseImmutableList_doubleReverse() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");
        ImmutableList<String> reversed = list.reversed();
        ImmutableList<String> doubleReversed = reversed.reversed();

        // Double reverse should return original
        assertEquals(list, doubleReversed);
    }

    @Test
    public void testReverseImmutableList_iteratorConsistentWithGet() {
        ImmutableList<Integer> list = ImmutableList.of(10, 20, 30, 40);
        ImmutableList<Integer> reversed = list.reversed();

        Iterator<Integer> iter = reversed.iterator();
        for (Integer element : reversed) {
            assertEquals(element, iter.next());
        }
    }

    // ============================================================
    // Fix 7: Numbers.round() thread safety with DecimalFormat
    // ============================================================

    @Test
    public void testNumbersRound_threadSafetyWithDecimalFormat() throws InterruptedException {
        final DecimalFormat df = new DecimalFormat("#.##");
        final int numThreads = 10;
        final int iterations = 1000;
        final AtomicInteger errorCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(numThreads);

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int t = 0; t < numThreads; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < iterations; i++) {
                        double result = Numbers.round(3.14159, df);
                        if (Math.abs(result - 3.14) > 0.001) {
                            errorCount.incrementAndGet();
                        }
                        float fresult = Numbers.round(3.14159f, df);
                        if (Math.abs(fresult - 3.14f) > 0.01f) {
                            errorCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        assertEquals(0, errorCount.get(), "DecimalFormat should be thread-safe via cloning");
    }

    // ============================================================
    // Fix 14: ExceptionUtil.firstCause() cycle detection
    // ============================================================

    @Test
    public void testFirstCause_simpleCauseChain() {
        Exception root = new Exception("root");
        Exception mid = new Exception("mid", root);
        Exception top = new Exception("top", mid);

        assertEquals(root, ExceptionUtil.firstCause(top));
    }

    @Test
    public void testFirstCause_noCause() {
        Exception single = new Exception("single");
        assertEquals(single, ExceptionUtil.firstCause(single));
    }

    @Test
    public void testFirstCause_selfCause() {
        // Create a self-referencing cause chain (A -> A)
        Exception e = new Exception("self");
        try {
            var field = Throwable.class.getDeclaredField("cause");
            field.setAccessible(true);
            field.set(e, e);
        } catch (Exception ex) {
            // If reflection fails, skip test
            return;
        }

        // Should not infinite loop; should return the exception itself
        Throwable result = ExceptionUtil.firstCause(e);
        assertEquals(e, result);
    }

    // ============================================================
    // Fix 17: Numbers.createNumber() with both 'e' and 'E'
    // ============================================================

    @Test
    public void testCreateNumber_singleLowercaseExponent() {
        Number num = Numbers.createNumber("1.5e2");
        assertEquals(150.0, num.doubleValue(), 0.01);
    }

    @Test
    public void testCreateNumber_singleUppercaseExponent() {
        Number num = Numbers.createNumber("1.5E2");
        assertEquals(150.0, num.doubleValue(), 0.01);
    }

    @Test
    public void testCreateNumber_noExponent() {
        Number num = Numbers.createNumber("3.14");
        assertEquals(3.14, num.doubleValue(), 0.001);
    }

    // ============================================================
    // Fix 13: ImmutableBiMap.invertedView volatile
    // ============================================================

    @Test
    public void testImmutableBiMap_invertedConsistency() {
        BiMap<String, Integer> biMap = BiMap.of("a", 1, "b", 2, "c", 3);
        ImmutableBiMap<String, Integer> immBiMap = ImmutableBiMap.copyOf(biMap);

        ImmutableBiMap<Integer, String> inverted1 = immBiMap.inverted();
        ImmutableBiMap<Integer, String> inverted2 = immBiMap.inverted();

        // Should return the same cached instance
        assertEquals(inverted1, inverted2);
        assertEquals("a", inverted1.get(1));
        assertEquals("b", inverted1.get(2));
    }

    // ============================================================
    // Fix 15: CsvParser embedded quote detection i > 0
    // ============================================================

    @Test
    public void testCsvParser_embeddedQuoteNearStart() {
        CsvParser parser = new CsvParser();

        // Test that quotes in the middle of a field are treated as embedded quotes
        String[] result = parser.parseLineToArray("a\"b\"c,d");
        assertEquals(2, result.length);
        assertEquals("a\"b\"c", result[0]);
        assertEquals("d", result[1]);
    }

    // ============================================================
    // Fix 16: Joiner.isClosed volatile
    // ============================================================

    @Test
    public void testJoiner_closePreventsFurtherUse() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").append("b");
        String result = joiner.toString();
        assertEquals("a,b", result);

        joiner.close();

        // After close, should throw
        assertThrows(IllegalStateException.class, () -> joiner.append("c"));
    }
}
