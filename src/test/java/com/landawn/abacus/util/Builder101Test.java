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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Builder.DatasetBuilder;
import com.landawn.abacus.util.Builder.MapBuilder;
import com.landawn.abacus.util.Builder.MultimapBuilder;
import com.landawn.abacus.util.Builder.MultisetBuilder;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;

@Tag("new-test")
public class Builder101Test extends TestBase {

    private Dataset testDataset;
    private List<String> columnNames;
    private List<List<Object>> data;

    @BeforeEach
    public void setUp() {
        columnNames = CommonUtil.asList("name", "age", "city");
        data = new ArrayList<>();
        data.add(CommonUtil.asList("John", 25, "NYC"));
        data.add(CommonUtil.asList("Jane", 30, "LA"));
        data.add(CommonUtil.asList("Bob", 35, "Chicago"));
        testDataset = new RowDataset(columnNames, data);
    }

    @Test
    public void testDatasetBuilderRenameColumns() {
        DatasetBuilder builder = Builder.of(testDataset);

        Map<String, String> oldNewNames = new HashMap<>();
        oldNewNames.put("name", "firstName");
        oldNewNames.put("age", "years");

        builder.renameColumns(oldNewNames);

        assertTrue(testDataset.columnNames().contains("firstName"));
        assertTrue(testDataset.columnNames().contains("years"));
        assertFalse(testDataset.columnNames().contains("name"));
        assertFalse(testDataset.columnNames().contains("age"));
    }

    @Test
    public void testDatasetBuilderRenameColumnsWithFunction() {
        DatasetBuilder builder = Builder.of(testDataset);

        builder.renameColumns(Arrays.asList("name", "city"), col -> col.toUpperCase());

        assertTrue(testDataset.columnNames().contains("NAME"));
        assertTrue(testDataset.columnNames().contains("CITY"));
        assertTrue(testDataset.columnNames().contains("age"));
    }

    @Test
    public void testDatasetBuilderRenameAllColumnsWithFunction() {
        DatasetBuilder builder = Builder.of(testDataset);

        builder.renameColumns(col -> "col_" + col);

        assertTrue(testDataset.columnNames().contains("col_name"));
        assertTrue(testDataset.columnNames().contains("col_age"));
        assertTrue(testDataset.columnNames().contains("col_city"));
    }

    @Test
    public void testDatasetBuilderAddColumnWithBiFunction() {
        DatasetBuilder builder = Builder.of(testDataset);

        BiFunction<Object, Object, String> combineFunc = (name, city) -> name + " from " + city;

        builder.addColumn("description", Tuple.of("name", "city"), combineFunc);

        assertTrue(testDataset.columnNames().contains("description"));
    }

    @Test
    public void testDatasetBuilderAddColumnWithMultipleColumns() {
        DatasetBuilder builder = Builder.of(testDataset);

        Function<DisposableObjArray, String> func = arr -> {
            return arr.get(0) + ":" + arr.get(1);
        };

        builder.addColumn("nameAge", Arrays.asList("name", "age"), func);

        assertTrue(testDataset.columnNames().contains("nameAge"));
    }

    @Test
    public void testDatasetBuilderRemoveColumns() {
        DatasetBuilder builder = Builder.of(testDataset);

        builder.removeColumns(Arrays.asList("age", "city"));

        assertEquals(1, testDataset.columnCount());
        assertTrue(testDataset.columnNames().contains("name"));
        assertFalse(testDataset.columnNames().contains("age"));
        assertFalse(testDataset.columnNames().contains("city"));
    }

    @Test
    public void testDatasetBuilderRemoveColumnsWithPredicate() {
        DatasetBuilder builder = Builder.of(testDataset);

        builder.removeColumns(col -> col.startsWith("c"));

        assertEquals(2, testDataset.columnCount());
        assertFalse(testDataset.columnNames().contains("city"));
        assertTrue(testDataset.columnNames().contains("name"));
        assertTrue(testDataset.columnNames().contains("age"));
    }

    @Test
    public void testDatasetBuilderDivideColumn() {
        testDataset.addColumn("fullName", Arrays.asList("John Doe", "Jane Smith", "Bob Jones"));

        DatasetBuilder builder = Builder.of(testDataset);

        Function<Object, List<String>> splitFunc = fullName -> Arrays.asList(fullName.toString().split(" "));

        builder.divideColumn("fullName", Arrays.asList("firstName", "lastName"), splitFunc);

        assertFalse(testDataset.columnNames().contains("fullName"));
        assertTrue(testDataset.columnNames().contains("firstName"));
        assertTrue(testDataset.columnNames().contains("lastName"));
    }

    @Test
    public void testMultimapBuilderPutMany() {
        Multimap<String, Integer, List<Integer>> multimap = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, Multimap<String, Integer, List<Integer>>> builder = Builder.of(multimap);

        Map<String, Collection<Integer>> mapToAdd = new HashMap<>();
        mapToAdd.put("key1", Arrays.asList(1, 2, 3));
        mapToAdd.put("key2", Arrays.asList(4, 5));

        builder.putMany(mapToAdd);

        assertEquals(Arrays.asList(1, 2, 3), multimap.get("key1"));
        assertEquals(Arrays.asList(4, 5), multimap.get("key2"));
    }

    @Test
    public void testMultimapBuilderPutManyFromMultimap() {
        Multimap<String, Integer, List<Integer>> multimap1 = CommonUtil.newListMultimap();
        Multimap<String, Integer, List<Integer>> multimap2 = CommonUtil.newListMultimap();

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
        Multimap<String, Integer, List<Integer>> multimap = CommonUtil.newListMultimap();
        multimap.put("key1", 1);
        multimap.put("key1", 2);
        multimap.put("key1", 3);
        multimap.put("key2", 4);
        multimap.put("key3", 5);

        MultimapBuilder<String, Integer, List<Integer>, Multimap<String, Integer, List<Integer>>> builder = Builder.of(multimap);

        Map<String, Integer> toRemoveOne = new HashMap<>();
        toRemoveOne.put("key1", 2);
        builder.removeOne(toRemoveOne);

        assertEquals(Arrays.asList(1, 3), multimap.get("key1"));

        builder.removeMany("key1", Arrays.asList(1, 3));
        assertNull(multimap.get("key1"));

        builder.removeAll("key2");
        assertNull(multimap.get("key2"));

        Map<String, Collection<Integer>> toRemoveMany = new HashMap<>();
        toRemoveMany.put("key3", Arrays.asList(5));
        builder.removeMany(toRemoveMany);
        assertNull(multimap.get("key3"));
    }

    @Test
    public void testComparisonBuilderWithCustomComparator() {
        Comparator<String> caseInsensitive = String.CASE_INSENSITIVE_ORDER;

        int result = Builder.compare("Hello", "hello", caseInsensitive).result();
        assertEquals(0, result);

        result = Builder.compare("ABC", "xyz", caseInsensitive).result();
        assertTrue(result < 0);
    }

    @Test
    public void testXBuilder() {
        String value = "test";
        Builder<String> builder = Builder.of(value);

        assertTrue(builder instanceof Builder);
        assertEquals(value, builder.val());
    }

    @Test
    public void testBuilderChainingComplex() {
        List<String> list = new ArrayList<>();

        Builder.of(list).add("first").addAll(Arrays.asList("second", "third")).add(1, "inserted").remove("third").accept(l -> l.add("fourth")).apply(l -> {
            Collections.reverse(l);
            return l;
        });

        assertEquals(Arrays.asList("fourth", "second", "inserted", "first"), list);
    }

    @Test
    public void testMultisetBuilderEdgeCases() {
        Multiset<String> multiset = CommonUtil.newMultiset();
        MultisetBuilder<String> builder = Builder.of(multiset);

        builder.add("item", 3).remove("item", 5);

        assertEquals(0, multiset.count("item"));

        builder.setCount("item2", 10).setCount("item2", 5);

        assertEquals(5, multiset.count("item2"));
    }

    @Test
    public void testMapBuilderPutIfAbsentEdgeCases() {
        Map<String, String> map = new HashMap<>();
        map.put("null-value", null);

        MapBuilder<String, String, Map<String, String>> builder = Builder.of(map);

        builder.putIfAbsent("null-value", "replacement");
        assertEquals("replacement", map.get("null-value"));

        map.put("null-value2", null);
        builder.putIfAbsent("null-value2", () -> "generated");
        assertEquals("generated", map.get("null-value2"));
    }

    @Test
    public void testHashCodeBuilderConsistency() {
        Object[] values = { "test", 123, true, 45.6, 'x' };

        int hash1 = Builder.hash(values[0]).hash(values[1]).hash((boolean) values[2]).hash((double) values[3]).hash((char) values[4]).result();

        int hash2 = Builder.hash(values[0]).hash(values[1]).hash((boolean) values[2]).hash((double) values[3]).hash((char) values[4]).result();

        assertEquals(hash1, hash2);
    }

    @Test
    public void testEquivalenceBuilderShortCircuit() {
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
        assertFalse(evaluated[2]);
    }
}
