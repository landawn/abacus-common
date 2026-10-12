package com.landawn.abacus.util.stream;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Suppliers;

/** ByteStream collection contracts. */
public class ByteStreamCollectionTest extends ByteStreamTestSupport {

    // collect

    @Test
    public void testCollect() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteList list = stream.collect(ByteList::new, ByteList::add, ByteList::addAll);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
    }

    @Test
    public void testCollectFull() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        StringBuilder sb = byteStream.collect(StringBuilder::new, (s, b) -> s.append(b), StringBuilder::append);
        assertEquals("123", sb.toString());
    }

    @Test
    public void testCollectSimplified() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ByteList list = byteStream.collect(ByteList::new, ByteList::add);
        assertEquals(ByteList.of((byte) 1, (byte) 2, (byte) 3), list);
    }

    @Test
    public void testCollectWithoutCombiner() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteList list = stream.collect(ByteList::new, ByteList::add);
        assertEquals(3, list.size());
    }

    @Test
    public void testCollectWithSupplierOnly() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);

        ByteList list = stream.collect(ByteList::new, ByteList::add);

        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    // groupTo

    @Test
    public void testGroupTo() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        Map<Boolean, List<Byte>> map = stream.groupTo(b -> b % 2 == 0, java.util.stream.Collectors.toList());
        assertEquals(2, map.size());
        assertTrue(map.containsKey(true));
        assertTrue(map.containsKey(false));
    }

    @Test
    public void testGroupToCollector() {
        byteStream = createByteStream(TEST_ARRAY);
        Map<String, List<Byte>> result = byteStream.groupTo(b -> (b % 2 == 0) ? "Even" : "Odd", Collectors.mapping(b -> b, Collectors.toList()));
        assertEquals(2, result.size());
        assertEquals(Arrays.asList((byte) 2, (byte) 4), result.get("Even"));
        assertEquals(Arrays.asList((byte) 1, (byte) 3, (byte) 5), result.get("Odd"));
    }

    @Test
    public void testGroupToCollectorMapFactory() {
        byteStream = createByteStream(TEST_ARRAY);
        Map<String, Long> result = byteStream.groupTo(b -> (b % 2 == 0) ? "Even" : "Odd", Collectors.counting(), Suppliers.ofTreeMap());
        assertEquals(2, result.size());
        assertEquals(Long.valueOf(2), result.get("Even"));
        assertEquals(Long.valueOf(3), result.get("Odd"));
    }

    @Test
    public void testGroupToWithFactory() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        Map<Integer, List<Byte>> map = stream.groupTo(b -> (int) b, java.util.stream.Collectors.toList(), HashMap::new);
        assertEquals(3, map.size());
    }

    @Test
    public void testGroupToWithSupplier() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);

        var grouped = stream.groupTo(b -> b % 2 == 0 ? "even" : "odd", Collectors.counting(), java.util.TreeMap::new);

        assertTrue(grouped instanceof java.util.TreeMap);
        assertEquals(2, grouped.size());
        assertEquals(Long.valueOf(3), grouped.get("odd"));
        assertEquals(Long.valueOf(2), grouped.get("even"));
    }

    @Test
    public void testToArray() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        byte[] result = byteStream.toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);

        result = createByteStream(new byte[] {}).toArray();
        assertArrayEquals(new byte[] {}, result);
    }

    @Test
    public void testToByteList() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteList list = stream.toByteList();
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void testToCollection() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        ArrayList<Byte> result = byteStream.toCollection(ArrayList::new);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
    }

    @Test
    public void testToImmutableList() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<Byte> result = byteStream.toImmutableList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
        try {
            result.add((byte) 4);
            fail("Should be immutable");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testToImmutableSet() {
        byteStream = createByteStream(new byte[] { 1, 2, 2, 3 });
        Set<Byte> result = byteStream.toImmutableSet();
        assertEquals(new HashSet<>(Arrays.asList((byte) 1, (byte) 2, (byte) 3)), result);
        try {
            result.add((byte) 4);
            fail("Should be immutable");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testToList() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        List<Byte> list = stream.toList();
        assertEquals(3, list.size());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), list);

        ByteStream emptyStream = ByteStream.empty();
        assertEquals(0, emptyStream.toList().size());
    }

    // toMap

    @Test
    public void testToMap() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        Map<Integer, String> map = stream.toMap(b -> (int) b, b -> "value" + b);
        assertEquals(3, map.size());
        assertEquals("value1", map.get(1));
        assertEquals("value2", map.get(2));
        assertEquals("value3", map.get(3));
    }

    @Test
    public void testToMapKeyValue() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        Map<String, Integer> result = byteStream.toMap(b -> "K" + b, b -> b * 10);
        assertEquals(3, result.size());
        assertEquals(10, (int) result.get("K1"));
        assertEquals(20, (int) result.get("K2"));
        assertEquals(30, (int) result.get("K3"));
    }

    @Test
    public void testToMapKeyValueMapFactory() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        Map<String, Integer> result = byteStream.toMap(b -> "K" + b, b -> b * 10, () -> N.newHashMap());
        assertEquals(3, result.size());
        assertEquals(10, (int) result.get("K1"));
    }

    @Test
    public void testToMapKeyValueMergeFunction() {
        byteStream = createByteStream(new byte[] { 1, 2, 1 });
        Map<String, Integer> result = byteStream.toMap(b -> "K" + b, b -> b * 10, (v1, v2) -> v1 + v2);
        assertEquals(2, result.size());
        assertEquals(20, (int) result.get("K1"));
        assertEquals(20, (int) result.get("K2"));
    }

    @Test
    public void testToMapKeyValueMergeFunctionMapFactory() {
        byteStream = createByteStream(new byte[] { 1, 2, 1 });
        Map<String, Integer> result = byteStream.toMap(b -> "K" + b, b -> b * 10, (v1, v2) -> v1 + v2, () -> N.newLinkedHashMap());
        assertEquals(2, result.size());
        assertEquals(20, (int) result.get("K1"));
        assertEquals(20, (int) result.get("K2"));
    }

    @Test
    public void testToMapWithFactory() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        Map<Integer, String> map = stream.toMap(b -> (int) b, b -> "val" + b, Suppliers.ofMap());
        assertEquals(3, map.size());
        assertTrue(map instanceof HashMap);
    }

    @Test
    public void testToMapWithMerge() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 1);
        Map<Integer, String> map = stream.toMap(b -> (int) b, b -> "v" + b, (a, b) -> a + "," + b);
        assertEquals(2, map.size());
        assertEquals("v1,v1", map.get(1));
    }

    @Test
    public void testToMapWithMergeAndFactory() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 1);
        Map<Integer, String> map = stream.toMap(b -> (int) b, b -> "x" + b, (a, b) -> a + b, HashMap::new);
        assertEquals(2, map.size());
    }

    @Test
    public void testToMapWithMergeFunction() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 1, (byte) 3);

        var map = stream.toMap(b -> b % 2 == 0 ? "even" : "odd", b -> (int) b, (v1, v2) -> v1 + v2);

        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(5), map.get("odd"));
        assertEquals(Integer.valueOf(2), map.get("even"));
    }

    @Test
    public void testToMapWithMergeFunctionAndSupplier() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 1, (byte) 3);

        var map = stream.toMap(b -> b % 2 == 0 ? "even" : "odd", b -> (int) b, (v1, v2) -> v1 + v2, java.util.TreeMap::new);

        assertEquals(2, map.size());
        assertTrue(map instanceof java.util.TreeMap);
        assertEquals(Integer.valueOf(5), map.get("odd"));
        assertEquals(Integer.valueOf(2), map.get("even"));
    }

    @Test
    public void testToMapWithSupplier() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);

        var map = stream.toMap(b -> "key" + b, b -> (int) b, Suppliers.ofLinkedHashMap());

        assertEquals(3, map.size());
        assertTrue(map instanceof java.util.LinkedHashMap);
    }

    // toMultiset

    @Test
    public void testToMultiset() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3, (byte) 3);
        Multiset<Byte> multiset = stream.toMultiset();
        assertEquals(1, multiset.getCount((byte) 1));
        assertEquals(2, multiset.getCount((byte) 2));
        assertEquals(3, multiset.getCount((byte) 3));
    }

    @Test
    public void testToMultisetWithDefaultSupplier() {
        Multiset<Byte> multiset = createByteStream((byte) 1, (byte) 2, (byte) 1).toMultiset();
        assertEquals(2, multiset.getCount((byte) 1));
        assertEquals(1, multiset.getCount((byte) 2));
    }

    @Test
    public void testToMultisetWithSupplier() {
        byteStream = createByteStream(new byte[] { 1, 2, 2, 3, 3, 3 });
        Multiset<Byte> result = byteStream.toMultiset(Suppliers.ofMultiset());
        assertEquals(1, result.getCount((byte) 1));
        assertEquals(2, result.getCount((byte) 2));
        assertEquals(3, result.getCount((byte) 3));
    }

    @Test
    public void testToSet() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 1);
        Set<Byte> set = stream.toSet();
        assertEquals(3, set.size());
        assertTrue(set.contains((byte) 1));
        assertTrue(set.contains((byte) 2));
        assertTrue(set.contains((byte) 3));

        ByteStream stream2 = ByteStream.of((byte) 5, (byte) 5, (byte) 5);
        assertEquals(1, stream2.toSet().size());
    }

}
