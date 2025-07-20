package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.Stream;

public class Seq106Test extends TestBase {

    @Test
    public void testFlattmap_WithEmptySequence() throws Exception {
        Seq<Integer, Exception> emptySeq = Seq.of();
        Seq<Integer, Exception> result = emptySeq.flattmap(n -> new Integer[] { n, n * 2 });

        assertEquals(0, result.count());
    }

    @Test
    public void testFlattmap_WithSingleElement() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(5);
        List<Integer> result = seq.flattmap(n -> new Integer[] { n, n * 10 }).toList();

        assertEquals(Arrays.asList(5, 50), result);
    }

    @Test
    public void testFlattmap_WithMultipleElements() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        List<Integer> result = seq.flattmap(n -> new Integer[] { n, n * 10 }).toList();

        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);
    }

    @Test
    public void testFlattmap_WithEmptyArrays() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        List<Integer> result = seq.flattmap(n -> new Integer[0]).toList();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlattmap_WithMixedArraySizes() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        List<Integer> result = seq.flattmap(n -> {
            if (n == 1)
                return new Integer[] { n };
            else if (n == 2)
                return new Integer[0];
            else
                return new Integer[] { n, n * 10, n * 100 };
        }).toList();

        assertEquals(Arrays.asList(1, 3, 30, 300), result);
    }

    @Test
    public void testFlattmap_WithNullArrays() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        List<Integer> result = seq.flattmap(n -> n == 2 ? null : new Integer[] { n }).toList();

        assertEquals(Arrays.asList(1, 3), result);
    }

    @Test
    public void testFlattmap_WithStringToCharArray() throws Exception {
        Seq<String, Exception> seq = Seq.of("Hello", "World");
        List<Character> result = seq.flattmap(s -> s.chars().mapToObj(c -> (char) c).toArray(Character[]::new)).toList();

        assertEquals(Arrays.asList('H', 'e', 'l', 'l', 'o', 'W', 'o', 'r', 'l', 'd'), result);
    }

    @Test
    public void testFlattmap_CalledOnClosedSequence() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        seq.close();

        assertThrows(IllegalStateException.class, () -> {
            seq.flattmap(n -> new Integer[] { n });
        });
    }

    @Test
    public void testSliding_WithWindowSizeOne() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4);
        List<List<Integer>> result = seq.sliding(1, 1, IntFunctions.ofList()).toList();

        assertEquals(4, result.size());
        assertEquals(Arrays.asList(1), result.get(0));
        assertEquals(Arrays.asList(2), result.get(1));
        assertEquals(Arrays.asList(3), result.get(2));
        assertEquals(Arrays.asList(4), result.get(3));
    }

    @Test
    public void testSliding_WithWindowSizeTwoIncrementOne() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        List<List<Integer>> result = seq.sliding(2, 1, IntFunctions.ofList()).toList();

        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(2, 3), result.get(1));
    }

    @Test
    public void testSliding_WithWindowSizeTwoIncrementTwo() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4);
        List<List<Integer>> result = seq.sliding(2, 2, IntFunctions.ofList()).toList();

        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4), result.get(1));
    }

    @Test
    public void testSliding_WithWindowSizeLargerThanIncrement() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        List<List<Integer>> result = seq.sliding(3, 2, IntFunctions.ofList()).toList();

        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(3, 4, 5), result.get(1));
    }

    @Test
    public void testSliding_WithIncrementLargerThanWindowSize() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6);
        List<List<Integer>> result = seq.sliding(2, 3, IntFunctions.ofList()).toList();

        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(4, 5), result.get(1));
    }

    @Test
    public void testSliding_WithSingleElement() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1);
        List<List<Integer>> result = seq.sliding(2, 1, IntFunctions.ofList()).toList();

        assertEquals(1, result.size());
        assertEquals(Arrays.asList(1), result.get(0));
    }

    @Test
    public void testSliding_WithEmptySequence() throws Exception {
        Seq<Integer, Exception> seq = Seq.of();
        List<List<Integer>> result = seq.sliding(2, 1, IntFunctions.ofList()).toList();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testSliding_WithLinkedListSupplier() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4);
        List<LinkedList<Integer>> result = seq.sliding(2, 1, IntFunctions.ofLinkedList()).toList();

        assertEquals(3, result.size());
        assertTrue(result.get(0) instanceof LinkedList);
        assertEquals(Arrays.asList(1, 2), result.get(0));
    }

    @Test
    public void testSliding_WithHashSetSupplier() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4);
        List<HashSet<Integer>> result = seq.sliding(2, 1, HashSet::new).toList();

        assertEquals(3, result.size());
        assertTrue(result.get(0) instanceof HashSet);
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), result.get(0));
    }

    @Test
    public void testSliding_WithInvalidWindowSize() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> {
            Seq.of(1, 2, 3).sliding(0, 1, IntFunctions.ofList());
        });

        assertThrows(IllegalArgumentException.class, () -> {
            Seq.of(1, 2, 3).sliding(-1, 1, IntFunctions.ofList());
        });
    }

    @Test
    public void testSliding_WithInvalidIncrement() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> {
            Seq.of(1, 2, 3).sliding(1, 0, IntFunctions.ofList());
        });

        assertThrows(IllegalArgumentException.class, () -> {
            Seq.of(1, 2, 3).sliding(1, -1, IntFunctions.ofList());
        });
    }

    @Test
    public void testSliding_WithNullSupplier() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> {
            seq.sliding(2, 1, (IntFunction) null);
        });
    }

    @Test
    public void testSliding_CalledOnClosedSequence() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        seq.close();

        assertThrows(IllegalStateException.class, () -> {
            seq.sliding(2, 1, IntFunctions.ofList());
        });
    }

    @Test
    public void testSliding_CountMethod() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        Seq<List<Integer>, Exception> slidingSeq = seq.sliding(2, 1, IntFunctions.ofList());

        assertEquals(4, slidingSeq.count());
    }

    @Test
    public void testSliding_AdvanceMethod() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        Seq<List<Integer>, Exception> slidingSeq = seq.sliding(2, 1, IntFunctions.ofList()).skip(2);
        List<List<Integer>> result = slidingSeq.toList();

        assertEquals(2, result.size());
        assertEquals(Arrays.asList(3, 4), result.get(0));
        assertEquals(Arrays.asList(4, 5), result.get(1));
    }

    @Test
    public void testSlidingWithCollector_SimpleCase() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4);
        List<String> result = seq.map(String::valueOf).sliding(2, 1, Collectors.joining(",")).toList();

        assertEquals(3, result.size());
        assertEquals("1,2", result.get(0));
        assertEquals("2,3", result.get(1));
        assertEquals("3,4", result.get(2));
    }

    @Test
    public void testSlidingWithCollector_SumCollector() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        List<Integer> result = seq.sliding(3, 2, Collectors.summingInt(Integer::intValue)).toList();

        assertEquals(2, result.size());
        assertEquals(6, result.get(0)); // 1+2+3
        assertEquals(12, result.get(1)); // 3+4+5
    }

    @Test
    public void testSlidingWithCollector_ListCollector() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4);
        List<List<Integer>> result = seq.sliding(2, 1, Collectors.toList()).toList();

        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(2, 3), result.get(1));
        assertEquals(Arrays.asList(3, 4), result.get(2));
    }

    @Test
    public void testSlidingWithCollector_EmptySequence() throws Exception {
        Seq<Integer, Exception> seq = Seq.of();
        List<String> result = seq.map(String::valueOf).sliding(2, 1, Collectors.joining(",")).toList();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testSlidingWithCollector_SingleElement() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1);
        List<String> result = seq.map(String::valueOf).sliding(2, 1, Collectors.joining(",")).toList();

        assertEquals(1, result.size());
        assertEquals("1", result.get(0));
    }

    @Test
    public void testSlidingWithCollector_IncrementLargerThanWindowSize() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6);
        List<String> result = seq.map(String::valueOf).sliding(2, 3, Collectors.joining(",")).toList();

        assertEquals(2, result.size());
        assertEquals("1,2", result.get(0));
        assertEquals("4,5", result.get(1));
    }

    @Test
    public void testSlidingWithCollector_InvalidWindowSize() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> {
            seq.sliding(0, 1, Collectors.toList());
        });
    }

    @Test
    public void testSlidingWithCollector_InvalidIncrement() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> {
            seq.sliding(2, 0, Collectors.toList());
        });
    }

    @Test
    public void testSlidingWithCollector_NullCollector() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> {
            seq.sliding(2, 1, (Collector) null);
        });
    }

    @Test
    public void testSlidingWithCollector_CalledOnClosedSequence() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        seq.close();

        assertThrows(IllegalStateException.class, () -> {
            seq.map(String::valueOf).sliding(2, 1, Collectors.toList());
        });
    }

    @Test
    public void testSlidingWithCollector_CountMethod() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        Seq<String, Exception> slidingSeq = seq.map(String::valueOf).sliding(2, 1, Collectors.joining(","));

        assertEquals(4, slidingSeq.count());
    }

    @Test
    public void testSlidingWithCollector_AdvanceMethod() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        Seq<String, Exception> slidingSeq = seq.map(String::valueOf).sliding(2, 1, Collectors.joining(",")).skip(2);

        List<String> result = slidingSeq.toList();

        assertEquals(2, result.size());
        assertEquals("3,4", result.get(0));
        assertEquals("4,5", result.get(1));
    }

    @Test
    public void testTransformB_DeferredTrue() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        Seq<Integer, Exception> result = seq.transformB(stream -> stream.map(n -> n * 2), true);

        // Deferred execution - transformation happens when consumed
        List<Integer> list = result.toList();
        assertEquals(Arrays.asList(2, 4, 6), list);
    }

    @Test
    public void testTransformB_DeferredFalse() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        Seq<Integer, Exception> result = seq.transformB(stream -> stream.map(n -> n * 2), false);

        // Immediate execution
        List<Integer> list = result.toList();
        assertEquals(Arrays.asList(2, 4, 6), list);
    }

    @Test
    public void testTransformB_WithFilter() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);

        Seq<Integer, Exception> result = seq.transformB(stream -> stream.filter(n -> n % 2 == 0), false);

        List<Integer> list = result.toList();
        assertEquals(Arrays.asList(2, 4), list);
    }

    @Test
    public void testTransformB_WithFlatMap() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        Seq<Integer, Exception> result = seq.transformB(stream -> stream.flatMap(n -> Stream.of(n, n * 10)), false);

        List<Integer> list = result.toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), list);
    }

    @Test
    public void testTransformB_ReturningEmptyStream() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        Seq<Integer, Exception> result = seq.transformB(stream -> Stream.empty(), false);

        assertTrue(result.toList().isEmpty());
    }

    @Test
    public void testTransformB_ReturningNull() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        Seq<Integer, Exception> result = seq.transformB(stream -> null, false);

        assertTrue(result.toList().isEmpty());
    }

    @Test
    public void testTransformB_ComplexTransformation() throws Exception {
        Seq<String, Exception> seq = Seq.of("hello", "world", "java");

        Seq<Character, Exception> result = seq.transformB(stream -> stream.filter(s -> s.length() > 4).flatmapToChar(s -> s.toCharArray()).mapToObj(c -> c),
                false);

        List<Character> list = result.toList();
        assertEquals(Arrays.asList('h', 'e', 'l', 'l', 'o', 'w', 'o', 'r', 'l', 'd'), list);
    }

    @Test
    public void testTransformB_NullTransferFunction() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> {
            seq.transformB(null, false);
        });
    }

    @Test
    public void testTransformB_CalledOnClosedSequence() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        seq.close();

        assertThrows(IllegalStateException.class, () -> {
            seq.transformB(stream -> stream.map(n -> n * 2), false);
        });
    }

    @Test
    public void testTransformB_DeferredExceptionPropagation() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        Seq<Integer, Exception> result = seq.transformB(stream -> stream.map(n -> {
            if (n == 2)
                throw new RuntimeException("Test exception");
            return n;
        }), true);

        // Exception should be thrown when the deferred sequence is consumed
        assertThrows(Exception.class, () -> {
            result.toList();
        });
    }

    @Test
    public void testTransformB_MultipleDeferredTransformations() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        Seq<Integer, Exception> result = seq.transformB(stream -> stream.map(n -> n + 1), true).transformB(stream -> stream.map(n -> n * 2), true);

        List<Integer> list = result.toList();
        assertEquals(Arrays.asList(4, 6, 8), list);
    }
}