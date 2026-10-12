package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.IntFunctions;
import com.landawn.abacus.util.N;

/** EntryStream window contracts. */
public class EntryStreamWindowTest extends EntryStreamTestSupport {

    // sliding

    @Test
    public void testsliding() {
        final Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");
        map.put(4, "d");

        final List<List<Map.Entry<Integer, String>>> windows = EntryStream.of(map).sliding(2).toList();

        assertEquals(3, windows.size());
        assertEquals(Arrays.asList(N.newEntry(1, "a"), N.newEntry(2, "b")), windows.get(0));
        assertEquals(Arrays.asList(N.newEntry(2, "b"), N.newEntry(3, "c")), windows.get(1));
        assertEquals(Arrays.asList(N.newEntry(3, "c"), N.newEntry(4, "d")), windows.get(2));
    }

    @Test
    public void testSliding_WindowSize() {
        List<List<Entry<String, Integer>>> result = EntryStream.of(testMap).sliding(2).toList();
        assertEquals(4, result.size());
    }

    @Test
    public void testSliding_WithIncrement() {
        List<List<Entry<String, Integer>>> result = EntryStream.of(testMap).sliding(2, 2).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testSlidingAndSplitWithCollectionSupplier() {
        List<LinkedList<Entry<String, Integer>>> windows = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4).sliding(2, IntFunctions.ofLinkedList()).toList();

        assertEquals(3, windows.size());
        assertTrue(windows.get(0) instanceof LinkedList);
        assertEquals(2, windows.get(0).size());

        List<ArrayList<Entry<String, Integer>>> chunks = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5).split(2, ArrayList::new).toList();

        assertEquals(3, chunks.size());
        assertTrue(chunks.get(0) instanceof ArrayList);
        assertEquals(2, chunks.get(0).size());
        assertEquals(1, chunks.get(2).size());
    }

    @Test
    public void testSlidingWithCollectionSupplier() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<LinkedList<Map.Entry<String, Integer>>> windows = stream.sliding(3, IntFunctions.ofLinkedList()).toList();
        assertEquals(2, windows.size());
        assertTrue(windows.get(0) instanceof LinkedList);
    }

    @Test
    public void testSlidingWithIncrement() {
        final Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");
        map.put(4, "d");
        map.put(5, "e");

        final List<List<Map.Entry<Integer, String>>> windows = EntryStream.of(map).sliding(2, 3).toList();
        assertEquals(2, windows.size());
        assertEquals(Arrays.asList(N.newEntry(1, "a"), N.newEntry(2, "b")), windows.get(0));
        assertEquals(Arrays.asList(N.newEntry(4, "d"), N.newEntry(5, "e")), windows.get(1));
    }

    @Test
    public void testSlidingWithIncrementAndCollectionSupplier() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<LinkedList<Map.Entry<String, Integer>>> windows = stream.sliding(3, 2, IntFunctions.ofLinkedList()).toList();
        assertEquals(2, windows.size());
        assertTrue(windows.get(0) instanceof LinkedList);
    }

    // split

    @Test
    public void testSplit() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);

        final List<List<Map.Entry<String, Integer>>> result = EntryStream.of(map).split(2).toList();

        assertEquals(3, result.size());
        assertEquals(2, result.get(0).size());
        assertEquals(2, result.get(1).size());
        assertEquals(1, result.get(2).size());
    }

    @Test
    public void testSplit_Size() {
        List<List<Entry<String, Integer>>> result = EntryStream.of(testMap).split(2).toList();
        assertEquals(3, result.size());
        assertEquals(2, result.get(0).size());
    }

    @Test
    public void testSplitByPredicate() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        final List<List<Map.Entry<String, Integer>>> result = EntryStream.of(map).entries().split(e -> e.getValue() % 2 == 0).toList();

        assertEquals(4, result.size());
        assertEquals(Arrays.asList(N.newEntry("a", 1)), result.get(0));
        assertEquals(Arrays.asList(N.newEntry("b", 2)), result.get(1));
        assertEquals(Arrays.asList(N.newEntry("c", 3)), result.get(2));
        assertEquals(Arrays.asList(N.newEntry("d", 4)), result.get(3));
    }

    @Test
    public void testSplitWithCollectionSupplier() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<LinkedList<Map.Entry<String, Integer>>> chunks = stream.split(2, IntFunctions.ofLinkedList()).toList();
        assertEquals(3, chunks.size());
        assertTrue(chunks.get(0) instanceof LinkedList);
    }

}
