package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings.PS;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

/**
 * Tests for the bugfix of race condition in ParallelArrayStream/ParallelIteratorStream
 * intersection() and difference() methods.
 *
 * Bug: multiset.isEmpty() was called outside the synchronized(multiset) block, reading
 * a HashMap concurrently with modifications from other threads. This is undefined behavior
 * in Java and can cause infinite loops on some JVM implementations.
 *
 * Fix: Moved the isEmpty() check inside the synchronized block.
 */
@Tag("new-test")
public class ParallelStreamBugfixTest extends TestBase {

    /**
     * Test intersection on a parallel stream with enough elements to trigger parallel execution.
     * Verifies correctness of the synchronized fix.
     */
    @Test
    @DisplayName("Parallel intersection should produce correct results")
    public void test_parallelIntersection_correctness() {
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            data.add(i);
        }

        List<Integer> intersectWith = Arrays.asList(1, 3, 5, 7, 9, 11, 50, 100, 500, 999);

        List<Integer> result = Stream.of(data).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(4)).intersection(intersectWith).toList();

        Collections.sort(result);
        assertEquals(intersectWith.size(), result.size(), "Intersection result should contain exactly the common elements");
        assertEquals(Arrays.asList(1, 3, 5, 7, 9, 11, 50, 100, 500, 999), result);
    }

    /**
     * Test difference on a parallel stream with enough elements to trigger parallel execution.
     */
    @Test
    @DisplayName("Parallel difference should produce correct results")
    public void test_parallelDifference_correctness() {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<Integer> removeThese = Arrays.asList(2, 4, 6, 8, 10);

        List<Integer> result = Stream.of(data).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(4)).difference(removeThese).toList();

        Collections.sort(result);
        assertEquals(Arrays.asList(1, 3, 5, 7, 9), result);
    }

    /**
     * Test intersection with an empty collection (exercises the isEmpty() path heavily).
     */
    @Test
    @DisplayName("Parallel intersection with empty collection returns empty")
    public void test_parallelIntersection_emptyCollection() {
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            data.add(i);
        }

        List<Integer> result = Stream.of(data).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(4)).intersection(Collections.emptyList()).toList();

        assertTrue(result.isEmpty(), "Intersection with empty collection should be empty");
    }

    /**
     * Test difference with an empty collection (exercises the isEmpty() path).
     */
    @Test
    @DisplayName("Parallel difference with empty collection returns all elements")
    public void test_parallelDifference_emptyCollection() {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);

        List<Integer> result = Stream.of(data).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(4)).difference(Collections.emptyList()).toList();

        Collections.sort(result);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    /**
     * Stress test: run intersection many times with high parallelism to increase the chance
     * of exposing the race condition (which would cause incorrect results or hangs).
     */
    @RepeatedTest(10)
    @DisplayName("Stress test: parallel intersection under contention")
    public void test_parallelIntersection_stress() {
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            data.add(i);
        }

        // Use a small intersection set so the multiset empties quickly, exercising the isEmpty() check
        List<Integer> intersectWith = Arrays.asList(10, 20, 30);

        List<Integer> result = Stream.of(data).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(8)).intersection(intersectWith).toList();

        Collections.sort(result);
        assertEquals(Arrays.asList(10, 20, 30), result, "Intersection should find exactly the common elements");
    }

    /**
     * Stress test: run difference many times with high parallelism.
     */
    @RepeatedTest(10)
    @DisplayName("Stress test: parallel difference under contention")
    public void test_parallelDifference_stress() {
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            data.add(i);
        }

        // Remove all elements: tests that isEmpty() is properly synchronized
        List<Integer> removeAll = new ArrayList<>(data);

        List<Integer> result = Stream.of(data).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(8)).difference(removeAll).toList();

        assertTrue(result.isEmpty(), "Difference with all elements removed should be empty");
    }

    /**
     * Test intersection on ParallelArrayStream (via array-backed Stream.of).
     */
    @Test
    @DisplayName("ParallelArrayStream intersection correctness")
    public void test_parallelArrayStream_intersection() {
        Integer[] data = new Integer[200];
        for (int i = 0; i < 200; i++) {
            data[i] = i;
        }

        List<Integer> intersectWith = Arrays.asList(0, 50, 99, 150, 199);

        List<Integer> result = Stream.of(data).parallel(PS.create(Splitor.ARRAY).maxThreadNum(4)).intersection(intersectWith).toList();

        Collections.sort(result);
        assertEquals(Arrays.asList(0, 50, 99, 150, 199), result);
    }

    /**
     * Test difference on ParallelArrayStream (via array-backed Stream.of).
     */
    @Test
    @DisplayName("ParallelArrayStream difference correctness")
    public void test_parallelArrayStream_difference() {
        Integer[] data = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        List<Integer> removeThese = Arrays.asList(1, 3, 5, 7, 9);

        List<Integer> result = Stream.of(data).parallel(PS.create(Splitor.ARRAY).maxThreadNum(4)).difference(removeThese).toList();

        Collections.sort(result);
        assertEquals(Arrays.asList(2, 4, 6, 8, 10), result);
    }

    /**
     * Test intersection with duplicate elements in both the stream and the collection.
     * The multiset should handle occurrence counting correctly.
     */
    @Test
    @DisplayName("Parallel intersection with duplicates should respect occurrence count")
    public void test_parallelIntersection_withDuplicates() {
        List<Integer> data = Arrays.asList(1, 1, 2, 2, 3, 3, 3, 4, 5);
        List<Integer> intersectWith = Arrays.asList(1, 2, 2, 3);

        List<Integer> result = Stream.of(data).parallel(PS.create(Splitor.ITERATOR).maxThreadNum(4)).intersection(intersectWith).toList();

        Collections.sort(result);
        // Intersection with multiset semantics: 1 appears once, 2 appears twice, 3 appears once
        assertEquals(4, result.size());
        assertEquals(Arrays.asList(1, 2, 2, 3), result);
    }
}
