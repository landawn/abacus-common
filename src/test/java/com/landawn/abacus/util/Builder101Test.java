package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Builder.DatasetBuilder;
import com.landawn.abacus.util.Builder.MapBuilder;
import com.landawn.abacus.util.Builder.MultimapBuilder;
import com.landawn.abacus.util.Builder.MultisetBuilder;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;

public class Builder101Test extends TestBase {

    // Test data
    private Dataset testDataset;
    private List<String> columnNames;
    private List<List<Object>> data;

    @BeforeEach
    public void setUp() {
        columnNames = N.asList("name", "age", "city");
        data = new ArrayList<>();
        data.add(N.asList("John", 25, "NYC"));
        data.add(N.asList("Jane", 30, "LA"));
        data.add(N.asList("Bob", 35, "Chicago"));
        testDataset = new RowDataset(columnNames, data);
    }

    // ===== Additional DatasetBuilder Tests =====

    @Test
    public void testDatasetBuilderRenameColumns() {
        DatasetBuilder builder = Builder.of(testDataset);

        Map<String, String> oldNewNames = new HashMap<>();
        oldNewNames.put("name", "firstName");
        oldNewNames.put("age", "years");

        builder.renameColumns(oldNewNames);

        assertTrue(testDataset.columnNameList().contains("firstName"));
        assertTrue(testDataset.columnNameList().contains("years"));
        assertFalse(testDataset.columnNameList().contains("name"));
        assertFalse(testDataset.columnNameList().contains("age"));
    }

    @Test
    public void testDatasetBuilderRenameColumnsWithFunction() {
        DatasetBuilder builder = Builder.of(testDataset);

        // Rename specific columns with a function
        builder.renameColumns(Arrays.asList("name", "city"), col -> col.toUpperCase());

        assertTrue(testDataset.columnNameList().contains("NAME"));
        assertTrue(testDataset.columnNameList().contains("CITY"));
        assertTrue(testDataset.columnNameList().contains("age")); // unchanged
    }

    @Test
    public void testDatasetBuilderRenameAllColumnsWithFunction() {
        DatasetBuilder builder = Builder.of(testDataset);

        // Rename all columns
        builder.renameColumns(col -> "col_" + col);

        assertTrue(testDataset.columnNameList().contains("col_name"));
        assertTrue(testDataset.columnNameList().contains("col_age"));
        assertTrue(testDataset.columnNameList().contains("col_city"));
    }

    @Test
    public void testDatasetBuilderAddColumnWithBiFunction() {
        DatasetBuilder builder = Builder.of(testDataset);

        // Add column combining two columns
        BiFunction<Object, Object, String> combineFunc = (name, city) -> name + " from " + city;

        builder.addColumn("description", Tuple.of("name", "city"), combineFunc);

        assertTrue(testDataset.columnNameList().contains("description"));
    }

    @Test
    public void testDatasetBuilderAddColumnWithMultipleColumns() {
        DatasetBuilder builder = Builder.of(testDataset);

        // Add column based on multiple columns using DisposableObjArray
        Function<DisposableObjArray, String> func = arr -> {
            return arr.get(0) + ":" + arr.get(1);
        };

        builder.addColumn("nameAge", Arrays.asList("name", "age"), func);

        assertTrue(testDataset.columnNameList().contains("nameAge"));
    }

    @Test
    public void testDatasetBuilderRemoveColumns() {
        DatasetBuilder builder = Builder.of(testDataset);

        builder.removeColumns(Arrays.asList("age", "city"));

        assertEquals(1, testDataset.columnCount());
        assertTrue(testDataset.columnNameList().contains("name"));
        assertFalse(testDataset.columnNameList().contains("age"));
        assertFalse(testDataset.columnNameList().contains("city"));
    }

    @Test
    public void testDatasetBuilderRemoveColumnsWithPredicate() {
        DatasetBuilder builder = Builder.of(testDataset);

        // Remove columns that start with 'c'
        builder.removeColumns(col -> col.startsWith("c"));

        assertEquals(2, testDataset.columnCount());
        assertFalse(testDataset.columnNameList().contains("city"));
        assertTrue(testDataset.columnNameList().contains("name"));
        assertTrue(testDataset.columnNameList().contains("age"));
    }

    @Test
    public void testDatasetBuilderDivideColumn() {
        // Add a combined column first
        testDataset.addColumn("fullName", Arrays.asList("John Doe", "Jane Smith", "Bob Jones"));

        DatasetBuilder builder = Builder.of(testDataset);

        Function<Object, List<String>> splitFunc = fullName -> Arrays.asList(fullName.toString().split(" "));

        builder.divideColumn("fullName", Arrays.asList("firstName", "lastName"), splitFunc);

        assertFalse(testDataset.columnNameList().contains("fullName"));
        assertTrue(testDataset.columnNameList().contains("firstName"));
        assertTrue(testDataset.columnNameList().contains("lastName"));
    }

    // ===== Additional MultimapBuilder Tests =====

    @Test
    public void testMultimapBuilderPutMany() {
        Multimap<String, Integer, List<Integer>> multimap = N.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, Multimap<String, Integer, List<Integer>>> builder = Builder.of(multimap);

        // Test putMany with Map
        Map<String, Collection<Integer>> mapToAdd = new HashMap<>();
        mapToAdd.put("key1", Arrays.asList(1, 2, 3));
        mapToAdd.put("key2", Arrays.asList(4, 5));

        builder.putMany(mapToAdd);

        assertEquals(Arrays.asList(1, 2, 3), multimap.get("key1"));
        assertEquals(Arrays.asList(4, 5), multimap.get("key2"));
    }

    @Test
    public void testMultimapBuilderPutManyFromMultimap() {
        Multimap<String, Integer, List<Integer>> multimap1 = N.newListMultimap();
        Multimap<String, Integer, List<Integer>> multimap2 = N.newListMultimap();

        multimap2.put("key1", 10);
        multimap2.put("key1", 20);
        multimap2.put("key2", 30);

        MultimapBuilder<String, Integer, List<Integer>, Multimap<String, Integer, List<Integer>>> builder = Builder.of(multimap1);

        builder.putMany(multimap2);

        assertEquals(Arrays.asList(10, 20), multimap1.get("key1"));
        assertEquals(Arrays.asList(30), multimap1.get("key2"));
    }

    @Test
    public void testMultimapBuilderRemoveOperations() {
        Multimap<String, Integer, List<Integer>> multimap = N.newListMultimap();
        multimap.put("key1", 1);
        multimap.put("key1", 2);
        multimap.put("key1", 3);
        multimap.put("key2", 4);
        multimap.put("key3", 5);

        MultimapBuilder<String, Integer, List<Integer>, Multimap<String, Integer, List<Integer>>> builder = Builder.of(multimap);

        // Test removeOne with map
        Map<String, Integer> toRemoveOne = new HashMap<>();
        toRemoveOne.put("key1", 2);
        builder.removeOne(toRemoveOne);

        assertEquals(Arrays.asList(1, 3), multimap.get("key1"));

        // Test removeMany with collection
        builder.removeMany("key1", Arrays.asList(1, 3));
        assertNull(multimap.get("key1"));

        // Test removeAll
        builder.removeAll("key2");
        assertNull(multimap.get("key2"));

        // Test removeMany with map
        Map<String, Collection<Integer>> toRemoveMany = new HashMap<>();
        toRemoveMany.put("key3", Arrays.asList(5));
        builder.removeMany(toRemoveMany);
        assertNull(multimap.get("key3"));
    }

    // ===== Additional primitive list builder tests =====

    // ===== ComparisonBuilder with custom comparator =====

    @Test
    public void testComparisonBuilderWithCustomComparator() {
        // Case-insensitive string comparator
        Comparator<String> caseInsensitive = String.CASE_INSENSITIVE_ORDER;

        int result = Builder.compare("Hello", "hello", caseInsensitive).result();
        assertEquals(0, result);

        result = Builder.compare("ABC", "xyz", caseInsensitive).result();
        assertTrue(result < 0);
    }

    // ===== X<T> Builder (Beta) =====

    @Test
    public void testXBuilder() {
        // Test the X builder wrapper (if accessible)
        String value = "test";
        Builder<String> builder = Builder.of(value);

        // X is package-private, so we can only test through public API
        assertTrue(builder instanceof Builder);
        assertEquals(value, builder.val());
    }

    // ===== Testing edge cases for various builders =====

    @Test
    public void testBuilderChainingComplex() {
        // Complex chaining test
        List<String> list = new ArrayList<>();

        Builder.of(list).add("first").addAll(Arrays.asList("second", "third")).add(1, "inserted").remove("third").accept(l -> l.add("fourth")).apply(l -> {
            Collections.reverse(l);
            return l;
        });

        assertEquals(Arrays.asList("fourth", "second", "inserted", "first"), list);
    }

    @Test
    public void testMultisetBuilderEdgeCases() {
        Multiset<String> multiset = N.newMultiset();
        MultisetBuilder<String> builder = Builder.of(multiset);

        // Test removing more occurrences than exist
        builder.add("item", 3).remove("item", 5); // Remove more than exists

        assertEquals(0, multiset.count("item"));

        // Test setCount
        builder.setCount("item2", 10).setCount("item2", 5); // Override count

        assertEquals(5, multiset.count("item2"));
    }

    @Test
    public void testMapBuilderPutIfAbsentEdgeCases() {
        Map<String, String> map = new HashMap<>();
        map.put("null-value", null);

        MapBuilder<String, String, Map<String, String>> builder = Builder.of(map);

        // Test putIfAbsent with null value in map
        builder.putIfAbsent("null-value", "replacement");
        assertEquals("replacement", map.get("null-value"));

        // Test putIfAbsent with supplier when value is null
        map.put("null-value2", null);
        builder.putIfAbsent("null-value2", () -> "generated");
        assertEquals("generated", map.get("null-value2"));
    }

    @Test
    public void testHashCodeBuilderConsistency() {
        // Test that hash code is consistent
        Object[] values = { "test", 123, true, 45.6, 'x' };

        int hash1 = Builder.hash(values[0]).hash(values[1]).hash((boolean) values[2]).hash((double) values[3]).hash((char) values[4]).result();

        int hash2 = Builder.hash(values[0]).hash(values[1]).hash((boolean) values[2]).hash((double) values[3]).hash((char) values[4]).result();

        assertEquals(hash1, hash2);
    }

    @Test
    public void testEquivalenceBuilderShortCircuit() {
        // Test that equivalence builder short-circuits on first false
        boolean[] evaluated = { false, false, false };

        BiPredicate<String, String> tracker1 = (a, b) -> {
            evaluated[0] = true;
            return true;
        };

        BiPredicate<String, String> tracker2 = (a, b) -> {
            evaluated[1] = true;
            return false;
        };

        BiPredicate<String, String> tracker3 = (a, b) -> {
            evaluated[2] = true;
            return true;
        };

        boolean result = Builder.equals("a", "a", tracker1).equals("b", "b", tracker2).equals("c", "c", tracker3).result();

        assertFalse(result);
        assertTrue(evaluated[0]);
        assertTrue(evaluated[1]);
        assertFalse(evaluated[2]); // Should not be evaluated due to short-circuit
    }
}
