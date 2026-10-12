package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Suppliers;

/** DoubleStream collection contracts. */
public class DoubleStreamCollectionTest extends DoubleStreamTestSupport {

    // collect

    @Test
    public void testCollect() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        DoubleList result = stream.collect(DoubleList::new, DoubleList::add, DoubleList::addAll);
        assertEquals(3, result.size());
    }

    @Test
    public void testCollect_TwoArg2() {
        List<Double> result = DoubleStream.of(1.0, 2.0, 3.0).collect(ArrayList::new, (list, val) -> list.add(val));
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), result);
    }

    @Test
    public void testCollect_WithoutCombiner() {
        List<Double> result = createDoubleStream(1.0, 2.0, 3.0).collect(ArrayList::new, (list, n) -> list.add(n));
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), result);
    }

    @Test
    public void testCollectTwoArgs() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        DoubleList result = stream.collect(DoubleList::new, DoubleList::add);
        assertEquals(3, result.size());
    }

    @Test
    public void testCollectWithoutCombiner() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0);
        DoubleList result = stream.collect(DoubleList::new, DoubleList::add);

        assertEquals(3, result.size());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result.toArray(), 0.0001);
    }

    // groupTo

    @Test
    public void testGroupTo() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        Map<Boolean, List<Double>> map = stream.groupTo(x -> x > 2.0, java.util.stream.Collectors.toList());
        assertEquals(2, map.size());
    }

    @Test
    public void testGroupTo_WithMapSupplier() {
        HashMap<Boolean, List<Double>> grouped = createDoubleStream(1.0, 2.0, 3.0, 4.0, 5.0, 6.0).groupTo(n -> n % 2 == 0, java.util.stream.Collectors.toList(),
                HashMap::new);
        assertEquals(2, grouped.size());
        assertTrue(grouped.get(true).contains(2.0));
    }

    @Test
    public void testGroupToAveragingDoubleIsNotExactlyDecimal() {
        final Map<String, Double> averages = DoubleStream.of(85.5, 92.3, 78.9, 88.7, 95.1)
                .groupTo(d -> d >= 90 ? "A" : (d >= 80 ? "B" : "C"), Collectors.averagingDouble(Double::doubleValue));

        assertEquals(3, averages.size());
        // (92.3 + 95.1) / 2 is not exactly representable: the documented result is 93.69999999999999, not 93.7
        assertEquals(93.69999999999999, averages.get("A"));
        assertFalse(averages.get("A") == 93.7);
        assertEquals(87.1, averages.get("B"));
        assertEquals(78.9, averages.get("C"));
    }

    @Test
    public void testGroupToWithFactory() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        Map<Boolean, List<Double>> map = stream.groupTo(x -> x > 2.0, java.util.stream.Collectors.toList(), HashMap::new);
        assertEquals(2, map.size());
    }

    @Test
    public void testToArray() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        double[] result = stream.toArray();
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.0001);
    }

    @Test
    public void test_toCollection() {
        {
            DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);

            List<Double> result = stream.toList();
            assertHaveSameElements(N.toSet(1.0, 2.0, 3.0, 4.0, 5.0), result);
        }
        {
            DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);

            Set<Double> result = stream.toSet();
            assertHaveSameElements(N.toSet(1.0, 2.0, 3.0, 4.0, 5.0), result);
        }
        {
            DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);

            Set<Double> result = stream.toCollection(Suppliers.ofLinkedHashSet());
            assertHaveSameElements(N.toSet(1.0, 2.0, 3.0, 4.0, 5.0), result);
        }
    }

    @Test
    public void testToDoubleList() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        DoubleList list = stream.toDoubleList();
        assertEquals(3, list.size());
        assertEquals(1.0, list.get(0), 0.0001);
    }

    @Test
    public void testToImmutableList_HappyPath() {
        com.landawn.abacus.util.ImmutableList<Double> result = DoubleStream.of(1.0, 2.0, 3.0).toImmutableList();
        assertEquals(3, result.size());
        assertEquals(Double.valueOf(1.0), result.get(0));
    }

    @Test
    public void testToImmutableSet_HappyPath() {
        com.landawn.abacus.util.ImmutableSet<Double> result = DoubleStream.of(1.0, 2.0, 2.0, 3.0).toImmutableSet();
        assertEquals(3, result.size());
        assertTrue(result.contains(1.0));
    }

    @Test
    public void testToJdkStream() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        java.util.stream.DoubleStream jdkStream = stream.toJdkStream();
        assertEquals(3, jdkStream.count());
    }

    @Test
    public void testToList_HappyPath() {
        List<Double> result = DoubleStream.of(1.0, 2.0, 3.0).toList();
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), result);
    }

    // toMap

    @Test
    public void testToMap() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        Map<Integer, Double> map = stream.toMap(x -> (int) x, x -> x * 2);
        assertEquals(3, map.size());
        assertEquals(2.0, map.get(1), 0.0001);
    }

    @Test
    public void testToMap_WithMapSupplier() {
        Map<String, Double> map = createDoubleStream(1.0, 2.0, 3.0).toMap(n -> "key" + n, n -> n * 10, Suppliers.ofMap());
        assertEquals(3, map.size());
        assertEquals(10.0, map.get("key1.0"), 0.001);
    }

    @Test
    public void testToMap_WithMerge() {
        Map<Boolean, Double> map = createDoubleStream(1.0, 2.0, 3.0, 4.0, 5.0, 6.0).toMap(n -> n % 2 == 0, n -> n, Double::sum);
        assertEquals(12.0, map.get(true), 0.001);
        assertEquals(9.0, map.get(false), 0.001);
    }

    @Test
    public void testToMap_WithMergeFunction2() {
        Map<Double, Double> result = DoubleStream.of(1.0, 2.0, 1.0, 3.0, 2.0).toMap(d -> d, d -> 1.0, Double::sum);
        assertEquals(3, result.size());
        assertEquals(2.0, result.get(1.0), 0.001);
        assertEquals(2.0, result.get(2.0), 0.001);
        assertEquals(1.0, result.get(3.0), 0.001);
    }

    @Test
    public void testToMapWithFactory() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        Map<Integer, Double> map = stream.toMap(x -> (int) x, x -> x * 2, Suppliers.ofMap());
        assertEquals(3, map.size());
    }

    @Test
    public void testToMapWithMerge() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 1.0);
        Map<Integer, Double> map = stream.toMap(x -> (int) x, x -> x, (a, b) -> a + b);
        assertEquals(2, map.size());
        assertEquals(2.0, map.get(1), 0.0001);
    }

    @Test
    public void testToMapWithMergeAndFactory() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 1.0);
        Map<Integer, Double> map = stream.toMap(x -> (int) x, x -> x, (a, b) -> a + b, HashMap::new);
        assertEquals(2, map.size());
    }

    @Test
    public void test_toMultiset() {
        {
            DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);

            Multiset<Double> result = stream.toMultiset();
            assertEquals(Multiset.of(1.0, 2.0, 3.0, 4.0, 5.0), result);
        }
        {
            DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);

            Multiset<Double> result = stream.toMultiset(Suppliers.ofMultiset());
            assertEquals(Multiset.of(1.0, 2.0, 3.0, 4.0, 5.0), result);
        }
        {
            DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).map(e -> e);

            Multiset<Double> result = stream.toMultiset();
            assertEquals(Multiset.of(1.0, 2.0, 3.0, 4.0, 5.0), result);
        }
        {
            DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).map(e -> e);

            Multiset<Double> result = stream.toMultiset(Suppliers.ofMultiset());
            assertEquals(Multiset.of(1.0, 2.0, 3.0, 4.0, 5.0), result);
        }
    }

    @Test
    public void testToMultiset_HappyPath() {
        Multiset<Double> result = DoubleStream.of(1.0, 2.0, 2.0, 3.0, 3.0, 3.0).toMultiset();
        assertEquals(1, result.getCount(1.0));
        assertEquals(2, result.getCount(2.0));
        assertEquals(3, result.getCount(3.0));
    }

    @Test
    public void testToSet() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 2.0, 1.0);
        DoubleList list = stream.toDoubleList();
        assertNotNull(list);
        assertTrue(list.size() == 5);
    }

    @Test
    public void testToSet_HappyPath2() {
        Set<Double> result = DoubleStream.of(1.0, 2.0, 2.0, 3.0, 3.0).toSet();
        assertEquals(3, result.size());
        assertTrue(result.contains(1.0));
    }

}
