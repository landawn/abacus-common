package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Predicate;

public class RowDatasetToTest extends RowDatasetTestSupport {
    @Test
    public void testToString() {
        String str = dataset.toString();
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }

    @Test
    public void testToList() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<Object[]> list = dataset.toList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(1, list.get(0)[0]);
        Assertions.assertEquals("John", list.get(0)[1]);
    }

    @Test
    public void testToListWithRange() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<Object[]> list = dataset.toList(1, 3);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(2, list.get(0)[0]);
        Assertions.assertEquals("Jane", list.get(0)[1]);
    }

    @Test
    public void testToListAsClass() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<TestBean> list = dataset.toList(TestBean.class);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("John", list.get(0).name);
        Assertions.assertEquals(25, list.get(0).age);
    }

    @Test
    public void testToListWithRangeAsClass() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<TestBean> list = dataset.toList(0, 2, TestBean.class);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals("John", list.get(0).name);
        Assertions.assertEquals("Jane", list.get(1).name);
    }

    @Test
    public void testToListWithColumnsAsClass() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<TestBean> list = dataset.toList(Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("John", list.get(0).name);
        Assertions.assertEquals(25, list.get(0).age);
    }

    @Test
    public void testToListWithSupplier() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<Map<String, Object>> list = dataset.toList((IntFunction<Map<String, Object>>) size -> new HashMap<>());
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(1, list.get(0).get("id"));
        Assertions.assertEquals("John", list.get(0).get("name"));
    }

    @Test
    public void testToListWithRangeAndColumnsAsClass() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<TestBean> list = ds.toList(1, 3, Arrays.asList("name", "age"), TestBean.class);
        assertEquals(2, list.size());
        assertEquals("Jane", list.get(0).name);
        assertEquals(35, list.get(1).age);
    }

    @Test
    public void testToListWithRangeAndColumnsAndSupplier() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<Map<String, Object>> list = ds.toList(0, 2, Arrays.asList("name", "age"), (IntFunction<Map<String, Object>>) size -> new HashMap<>());
        assertEquals(2, list.size());
        assertEquals("John", list.get(0).get("name"));
        assertEquals(30, list.get(1).get("age"));
    }

    @Test
    public void testToListWithRangeAndFilterAndConverter() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<TestBean> list = ds.toList(0, 2, col -> col.equals("name") || col.equals("age"), col -> col, TestBean.class);
        assertEquals(2, list.size());
        assertEquals("John", list.get(0).name);
        assertEquals(25, list.get(0).age);
    }

    @Test
    public void testToListWithColumnsAsList() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<List<Object>> list = ds.toList(0, 3, Arrays.asList("name", "age"), (IntFunction<List<Object>>) size -> new ArrayList<>());
        assertEquals(3, list.size());
        assertEquals("John", list.get(0).get(0));
        assertEquals(25, list.get(0).get(1));
    }

    @Test
    public void testToListWithColumnsAsObjectArray() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<Object[]> list = ds.toList(0, 2, Arrays.asList("name", "age"), Object[].class);
        assertEquals(2, list.size());
        assertEquals("John", list.get(0)[0]);
        assertEquals(25, list.get(0)[1]);
    }

    @Test
    public void testToListWithColumnsAsMap() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<Map<String, Object>> list = ds.toList(0, 2, Arrays.asList("name", "age"), (Class) Map.class);
        assertEquals(2, list.size());
        assertEquals("John", list.get(0).get("name"));
    }

    // ===== toList with columnNameFilter and columnNameConverter =====

    @SuppressWarnings("unchecked")
    @Test
    public void testToList_WithColumnNameFilter() {
        // filter to only include "id" and "name" columns
        List<Object> result = dataset.<Object> toList((Predicate<? super String>) colName -> colName.equals("id") || colName.equals("name"),
                (Function<? super String, String>) colName -> colName, (Class<Object>) (Class<?>) Map.class);

        assertEquals(5, result.size());
        assertTrue(((Map<?, ?>) result.get(0)).containsKey("id"));
        assertTrue(((Map<?, ?>) result.get(0)).containsKey("name"));
        assertFalse(((Map<?, ?>) result.get(0)).containsKey("age"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testToList_WithColumnNameConverter() {
        // rename columns: prefix with "col_"
        List<Object> result = dataset.<Object> toList((Predicate<? super String>) colName -> true,
                (Function<? super String, String>) colName -> "col_" + colName, (Class<Object>) (Class<?>) Map.class);

        assertEquals(5, result.size());
        assertTrue(((Map<?, ?>) result.get(0)).containsKey("col_id"));
        assertTrue(((Map<?, ?>) result.get(0)).containsKey("col_name"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testToList_WithColumnNameFilterAndRange() {
        List<Object> result = dataset.<Object> toList(1, 3, (Predicate<? super String>) colName -> colName.equals("name") || colName.equals("age"),
                (Function<? super String, String>) colName -> colName, (Class<Object>) (Class<?>) Map.class);

        assertEquals(2, result.size());
        assertTrue(((Map<?, ?>) result.get(0)).containsKey("name"));
        assertEquals("Bob", ((Map<?, ?>) result.get(0)).get("name"));
    }

    @Test
    public void testToListWithColumnFilterAndConverter() {
        final RowDataset dataset = createFourRowCityDataset();
        Predicate<String> columnFilter = col -> col.equals("name") || col.equals("age");
        Function<String, String> columnConverter = String::toUpperCase;
        IntFunction<Map<String, Object>> rowSupplier = capacity -> new HashMap<>();

        List<Map<String, Object>> result = dataset.toList(columnFilter, columnConverter, rowSupplier);

        Assertions.assertEquals(4, result.size());
        Map<String, Object> firstRow = result.get(0);
        Assertions.assertTrue(firstRow.containsKey("NAME"));
        Assertions.assertTrue(firstRow.containsKey("AGE"));
        Assertions.assertFalse(firstRow.containsKey("id"));
        Assertions.assertFalse(firstRow.containsKey("city"));
    }

    @Test
    public void testToListWithColumnFilterAndConverterNullValues() {
        final RowDataset dataset = createFourRowCityDataset();
        IntFunction<List<Object>> rowSupplier = capacity -> new ArrayList<>();

        List<List<Object>> result = dataset.toList(Fn.alwaysTrue(), Fn.identity(), rowSupplier);

        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals(4, result.get(0).size());
    }

    @Test
    public void testToListWithEmptyDataset() {
        RowDataset emptyDataset = new RowDataset(new ArrayList<>(), new ArrayList<>());
        IntFunction<List<Object>> rowSupplier = capacity -> new ArrayList<>();

        List<List<Object>> result = emptyDataset.toList(Fn.alwaysTrue(), Fn.identity(), rowSupplier);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testToListWithFilters() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<TestBean> list = dataset.toList(col -> col.equals("name") || col.equals("age"), col -> col.toUpperCase(), TestBean.class);
        Assertions.assertEquals(3, list.size());
    }

    @Test
    public void testToListWithRangeAndFilterAndConverterAndSupplier() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<Map<String, Object>> list = ds.toList(0, 2, col -> !col.equals("score"), Fn.identity(),
                (IntFunction<Map<String, Object>>) size -> new HashMap<>());
        assertEquals(2, list.size());
        assertEquals("John", list.get(0).get("name"));
        assertFalse(list.get(0).containsKey("score"));
    }

    @Test
    public void testToListWithRowIndexRange() {
        final RowDataset dataset = createFourRowCityDataset();
        Predicate<String> columnFilter = col -> col.equals("name") || col.equals("age");
        IntFunction<Map<String, Object>> rowSupplier = capacity -> new HashMap<>();

        List<Map<String, Object>> result = dataset.toList(1, 3, columnFilter, Fn.identity(), rowSupplier);

        Assertions.assertEquals(2, result.size());
        Map<String, Object> firstRow = result.get(0);
        Assertions.assertEquals("Jane", firstRow.get("name"));
        Assertions.assertEquals(30, firstRow.get("age"));
    }

    @Test
    public void testToEntitiesWithPrefixMap() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Map<String, String> prefixMap = new HashMap<>();
        prefixMap.put("n", "name");
        List<TestBean> entities = dataset.toEntities(prefixMap, TestBean.class);
        Assertions.assertEquals(3, entities.size());
    }

    @Test
    public void testToEntitiesWithColumnsAndPrefix() {
        final RowDataset ds = createThreeRowScoreDataset();
        Map<String, String> prefixMap = new HashMap<>();
        List<TestBean> entities = ds.toEntities(Arrays.asList("name", "age"), prefixMap, TestBean.class);
        assertEquals(3, entities.size());
        assertEquals("John", entities.get(0).name);
    }

    @Test
    public void testToEntities() {
        final RowDataset dataset = createThreeRowScoreDataset();
        List<TestBean> entities = dataset.toEntities(null, TestBean.class);
        Assertions.assertEquals(3, entities.size());
        Assertions.assertEquals("John", entities.get(0).name);
    }

    @Test
    public void testToEntitiesWithRange() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<TestBean> entities = ds.toEntities(1, 3, null, TestBean.class);
        assertEquals(2, entities.size());
        assertEquals("Jane", entities.get(0).name);
        assertEquals("Bob", entities.get(1).name);
    }

    @Test
    public void testToEntitiesWithRangeAndColumns() {
        final RowDataset ds = createThreeRowScoreDataset();
        List<TestBean> entities = ds.toEntities(0, 2, Arrays.asList("name", "age"), null, TestBean.class);
        assertEquals(2, entities.size());
        assertEquals("John", entities.get(0).name);
        assertEquals(25, entities.get(0).age);
    }

    @Test
    public void testToMergedEntities() {
        List<String> mergeColumnNames = Arrays.asList("id", "name", "skill");
        List<List<Object>> mergeColumnList = new ArrayList<>();
        mergeColumnList.add(Arrays.asList(1, 1, 2, 2));
        mergeColumnList.add(Arrays.asList("John", "John", "Jane", "Jane"));
        mergeColumnList.add(Arrays.asList("Java", "Python", "JavaScript", "SQL"));

        RowDataset mergeDataset = new RowDataset(mergeColumnNames, mergeColumnList);

        Collection<String> idPropNames = Arrays.asList("id");
        Collection<String> selectPropNames = Arrays.asList("id", "name", "skill");
        Map<String, String> prefixAndFieldNameMap = new HashMap<>();

        List<PersonSkill> mergedEntities = mergeDataset.toMergedEntities(idPropNames, selectPropNames, prefixAndFieldNameMap, PersonSkill.class);

        Assertions.assertEquals(2, mergedEntities.size());
    }

    @Test
    public void testToMergedEntitiesWithInvalidIdProps() {
        Collection<String> idPropNames = Arrays.asList("nonexistent_id");
        Collection<String> selectPropNames = Arrays.asList("name", "age");
        Map<String, String> prefixAndFieldNameMap = new HashMap<>();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.toMergedEntities(idPropNames, selectPropNames, prefixAndFieldNameMap, Person.class);
        });
    }

    @Test
    public void testToMergedEntitiesNullBeanClassThrowsIllegalArgumentException() {
        // The three overloads that resolve the BeanInfo before delegating answered a raw
        // NullPointerException ("Cannot read field \"classValueMap\"") for a null bean class, while the other
        // six answered IllegalArgumentException for the identical argument. All nine are asserted here, so a
        // new overload that forgets the guard is caught.
        final Map<String, String> prefixAndFieldNameMap = new HashMap<>();
        final Collection<String> selectPropNames = Arrays.asList("id", "name");
        final Collection<String> idPropNames = Arrays.asList("id");

        final List<Throwables.Runnable<RuntimeException>> allNineOverloads = Arrays.asList( //
                () -> dataset.toMergedEntities((Class<Object>) null), //
                () -> dataset.toMergedEntities(selectPropNames, (Class<Object>) null), //
                () -> dataset.toMergedEntities(prefixAndFieldNameMap, (Class<Object>) null), //
                () -> dataset.toMergedEntities("id", (Class<Object>) null), //
                () -> dataset.toMergedEntities("id", selectPropNames, (Class<Object>) null), //
                () -> dataset.toMergedEntities("id", prefixAndFieldNameMap, (Class<Object>) null), //
                () -> dataset.toMergedEntities(idPropNames, selectPropNames, (Class<Object>) null), //
                () -> dataset.toMergedEntities(idPropNames, prefixAndFieldNameMap, (Class<Object>) null), //
                () -> dataset.toMergedEntities(idPropNames, selectPropNames, prefixAndFieldNameMap, (Class<Object>) null));

        assertEquals(9, allNineOverloads.size());

        for (final Throwables.Runnable<RuntimeException> overload : allNineOverloads) {
            final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, overload::run);
            assertEquals("null is not a bean class", ex.getMessage());
        }
    }

    @Test
    public void testToMergedEntitiesNonBeanClassReportsTheSameMessageAsItsSiblings() {
        // Side effect of the null guard above: for a non-null, non-bean rowType the two overloads that used to
        // fall through to ParserUtil.getBeanInfo reported its "No property getter/setter method or public field
        // found in the specified bean: ..." message, while the other seven already reported "... is not a bean
        // class". The exception type was IllegalArgumentException on both sides; only the message moved, and it
        // moved onto the message the other seven overloads were already using.
        final Map<String, String> prefixAndFieldNameMap = new HashMap<>();
        final Collection<String> selectPropNames = Arrays.asList("id", "name");
        final Collection<String> idPropNames = Arrays.asList("id");

        final List<Throwables.Runnable<RuntimeException>> allNineOverloads = Arrays.asList( //
                () -> dataset.toMergedEntities(Map.class), //
                () -> dataset.toMergedEntities(selectPropNames, Map.class), //
                () -> dataset.toMergedEntities(prefixAndFieldNameMap, Map.class), //
                () -> dataset.toMergedEntities("id", Map.class), //
                () -> dataset.toMergedEntities("id", selectPropNames, Map.class), //
                () -> dataset.toMergedEntities("id", prefixAndFieldNameMap, Map.class), //
                () -> dataset.toMergedEntities(idPropNames, selectPropNames, Map.class), //
                () -> dataset.toMergedEntities(idPropNames, prefixAndFieldNameMap, Map.class), //
                () -> dataset.toMergedEntities(idPropNames, selectPropNames, prefixAndFieldNameMap, Map.class));

        assertEquals(9, allNineOverloads.size());

        for (final Throwables.Runnable<RuntimeException> overload : allNineOverloads) {
            final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, overload::run);
            assertEquals("interface java.util.Map is not a bean class", ex.getMessage());
        }
    }

    @Test
    public void testToMapWithKeyAndValueColumns() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "id";
        Collection<String> valueColumns = Arrays.asList("name", "age");
        IntFunction<Map<Integer, Object[]>> supplier = capacity -> new HashMap<>();

        Map<Integer, Object[]> result = dataset.toMap(0, dataset.size(), keyColumn, valueColumns, Object[].class, supplier);

        Assertions.assertEquals(4, result.size());
        Object[] firstValue = result.get(1);
        Assertions.assertEquals("John", firstValue[0]);
        Assertions.assertEquals(25, firstValue[1]);
    }

    @Test
    public void testToMapWithCollectionRowType() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "id";
        Collection<String> valueColumns = Arrays.asList("name", "age", "city");
        IntFunction<Map<Integer, List<Object>>> supplier = capacity -> new HashMap<>();

        Map<Integer, List<Object>> result = dataset.toMap(0, dataset.size(), keyColumn, valueColumns, Clazz.ofList(), supplier);

        Assertions.assertEquals(4, result.size());
        List<Object> firstValue = result.get(1);
        Assertions.assertEquals(3, firstValue.size());
        Assertions.assertTrue(firstValue.contains("John"));
        Assertions.assertTrue(firstValue.contains(25));
        Assertions.assertTrue(firstValue.contains("NYC"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testToMapHonorsAbstractCollectionAndMapRowTypes() {
        final Map<Object, Object> queueRows = dataset.<Object, Object> toMap("id", Arrays.asList("name", "age"), (Class<Object>) (Class<?>) Queue.class);
        final Object queueRow = queueRows.get(1);

        assertTrue(queueRow instanceof Queue);
        assertEquals(Arrays.asList("Alice", 25), new ArrayList<>((Queue<?>) queueRow));

        final Map<Object, Object> sortedMapRows = dataset.<Object, Object> toMap("id", Arrays.asList("name", "age"),
                (Class<Object>) (Class<?>) SortedMap.class);
        final Object sortedMapRow = sortedMapRows.get(1);

        assertTrue(sortedMapRow instanceof SortedMap);
        assertEquals(25, ((SortedMap<?, ?>) sortedMapRow).get("age"));
        assertEquals("Alice", ((SortedMap<?, ?>) sortedMapRow).get("name"));
    }

    @Test
    public void testToMapWithMapRowType() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "id";
        Collection<String> valueColumns = Arrays.asList("name", "age", "city");
        IntFunction<Map<Integer, Map<String, Object>>> supplier = capacity -> new HashMap<>();

        Map<Integer, Map<String, Object>> result = dataset.toMap(0, dataset.size(), keyColumn, valueColumns, Clazz.ofMap(), supplier);

        Assertions.assertEquals(4, result.size());
        Map<String, Object> firstValue = result.get(1);
        Assertions.assertEquals("John", firstValue.get("name"));
        Assertions.assertEquals(25, firstValue.get("age"));
        Assertions.assertEquals("NYC", firstValue.get("city"));
    }

    @Test
    public void testToMapWithBeanRowType() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "id";
        Collection<String> valueColumns = Arrays.asList("name", "age", "city");
        IntFunction<Map<Integer, Person>> supplier = capacity -> new HashMap<>();

        Map<Integer, Person> result = dataset.toMap(0, dataset.size(), keyColumn, valueColumns, Person.class, supplier);

        Assertions.assertEquals(4, result.size());
        Person firstPerson = result.get(1);
        Assertions.assertEquals("John", firstPerson.getName());
        Assertions.assertEquals(25, firstPerson.getAge());
        Assertions.assertEquals("NYC", firstPerson.getCity());
    }

    @Test
    public void testToMapWithKeyValueColumns() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Map<Integer, String> map = dataset.toMap("id", "name");
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("John", map.get(1));
        Assertions.assertEquals("Jane", map.get(2));
        Assertions.assertEquals("Bob", map.get(3));
    }

    @Test
    public void testToMapWithSupplier() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Map<Integer, String> map = dataset.toMap("id", "name", (IntFunction<LinkedHashMap<Integer, String>>) size -> new LinkedHashMap<>());
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("John", map.get(1));
    }

    @Test
    public void testToMapWithValueSupplier() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Map<Integer, List<Object>> map = dataset.toMap("id", Arrays.asList("name", "age"), (IntFunction<List<Object>>) ArrayList::new);
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("John", map.get(1).get(0));
        Assertions.assertEquals(25, map.get(1).get(1));
    }

    @Test
    public void testToMap_WithValueColumnNamesAndRange() {
        Map<Object, Object[]> result = dataset.toMap(0, 3, "id", Arrays.asList("name", "age"), Object[].class);

        assertEquals(3, result.size());
        assertTrue(result.containsKey(1));
        assertTrue(result.containsKey(2));
        assertTrue(result.containsKey(3));
        assertFalse(result.containsKey(4));
    }

    @Test
    public void testToMapWithRange() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Map<Integer, String> map = dataset.toMap(0, 2, "id", "name");
        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals("John", map.get(1));
        Assertions.assertEquals("Jane", map.get(2));
        Assertions.assertNull(map.get(3));
    }

    @Test
    public void testToMapWithMultipleValueColumns() {
        final RowDataset dataset = createThreeRowScoreDataset();
        Map<Integer, TestBean> map = dataset.toMap("id", Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("John", map.get(1).name);
        Assertions.assertEquals(25, map.get(1).age);
    }

    // ===== toMap with valueColumnNames and rowType =====

    @Test
    public void testToMap_WithValueColumnNamesAsArray() {
        Map<Object, Object[]> result = dataset.toMap("id", Arrays.asList("name", "age"), Object[].class);

        assertEquals(5, result.size());
        Object[] row1 = result.get(1);
        assertNotNull(row1);
        assertEquals(2, row1.length);
        assertEquals("Alice", row1[0]);
        assertEquals(25, row1[1]);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testToMap_WithValueColumnNamesAsList() {
        Map<Object, Object> result = dataset.<Object, Object> toMap("id", Arrays.asList("name", "age"), (Class<Object>) (Class<?>) List.class);

        assertEquals(5, result.size());
        List<?> row1 = (List<?>) result.get(1);
        assertNotNull(row1);
        assertEquals(2, row1.size());
        assertEquals("Alice", row1.get(0));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testToMap_WithValueColumnNamesAsMap() {
        Map<Object, Object> result = dataset.<Object, Object> toMap("id", Arrays.asList("name", "age"), (Class<Object>) (Class<?>) Map.class);

        assertEquals(5, result.size());
        Map<?, ?> row1 = (Map<?, ?>) result.get(1);
        assertNotNull(row1);
        assertTrue(row1.containsKey("name"));
        assertEquals("Alice", row1.get("name"));
    }

    // ========== toMap - with supplier ==========

    @Test
    public void testToMap_WithValueColumnNamesAndSupplier_ReturnsLinkedHashMap() {
        Map<Object, Object[]> result = dataset.toMap("id", Arrays.asList("name", "age"), Object[].class, LinkedHashMap::new);
        assertNotNull(result);
        assertEquals(5, result.size());
        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testToMapWithRowSupplier() {
        final RowDataset dataset = createFourRowCityDataset();
        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<Map<Integer, Object[]>> mapSupplier = capacity -> new HashMap<>();

            Map<Integer, Object[]> result = dataset.toMap(0, dataset.size(), keyColumn, valueColumns, IntFunctions.ofObjectArray(), mapSupplier);

            Assertions.assertEquals(4, result.size());
            Object[] firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue[0]);
            Assertions.assertEquals(25, firstValue[1]);
        }
        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<Map<Integer, List<Object>>> mapSupplier = capacity -> new HashMap<>();

            Map<Integer, List<Object>> result = dataset.toMap(0, dataset.size(), keyColumn, valueColumns, IntFunctions.ofList(), mapSupplier);

            Assertions.assertEquals(4, result.size());
            List<Object> firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.get(0));
            Assertions.assertEquals(25, firstValue.get(1));
        }

        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<Map<Integer, Map<String, Object>>> mapSupplier = capacity -> new HashMap<>();

            Map<Integer, Map<String, Object>> result = dataset.toMap(0, dataset.size(), keyColumn, valueColumns, IntFunctions.ofMap(), mapSupplier);

            Assertions.assertEquals(4, result.size());
            Map<String, Object> firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.get("name"));
            Assertions.assertEquals(25, firstValue.get("age"));
        }

        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<Map<Integer, Person>> mapSupplier = capacity -> new HashMap<>();

            Map<Integer, Person> result = dataset.toMap(0, dataset.size(), keyColumn, valueColumns, i -> new Person(), mapSupplier);

            Assertions.assertEquals(4, result.size());
            Person firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.getName());
            Assertions.assertEquals(25, firstValue.getAge());
        }
    }

    @Test
    public void testToMapWithInvalidRowType() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "id";
        Collection<String> valueColumns = Arrays.asList("name", "age");
        IntFunction<Map<Integer, String>> supplier = capacity -> new HashMap<>();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.toMap(0, dataset.size(), keyColumn, valueColumns, String.class, supplier);
        });
    }

    @Test
    public void testToMap_EmptyRange() {
        Map<Integer, Object[]> result = dataset.toMap(1, 1, "id", Arrays.asList("name", "age"), IntFunctions.ofObjectArray(), IntFunctions.ofMap());

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testToMultimapWithCollectionRowType() {
        List<String> dupColumnNames = Arrays.asList("category", "product", "price");
        List<List<Object>> dupColumnList = new ArrayList<>();
        dupColumnList.add(Arrays.asList("A", "A", "B", "B"));
        dupColumnList.add(Arrays.asList("P1", "P2", "P3", "P4"));
        dupColumnList.add(Arrays.asList(10, 20, 30, 40));

        RowDataset dupDataset = new RowDataset(dupColumnNames, dupColumnList);

        String keyColumn = "category";
        Collection<String> valueColumns = Arrays.asList("product", "price");
        IntFunction<ListMultimap<String, List<Object>>> supplier = capacity -> CommonUtil.newLinkedListMultimap();

        ListMultimap<String, List<Object>> result = dupDataset.toMultimap(0, dupDataset.size(), keyColumn, valueColumns, Clazz.ofList(), supplier);

        Assertions.assertEquals(2, result.keySet().size());
        Assertions.assertEquals(2, result.get("A").size());
        Assertions.assertEquals(2, result.get("B").size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testToMultimapHonorsAbstractCollectionAndMapRowTypes() {
        final ListMultimap<Object, Object> queueRows = dataset.<Object, Object> toMultimap("id", Arrays.asList("name", "age"),
                (Class<Object>) (Class<?>) Queue.class);
        final Object queueRow = queueRows.get(1).get(0);

        assertTrue(queueRow instanceof Queue);
        assertEquals(Arrays.asList("Alice", 25), new ArrayList<>((Queue<?>) queueRow));

        final ListMultimap<Object, Object> sortedMapRows = dataset.<Object, Object> toMultimap("id", Arrays.asList("name", "age"),
                (Class<Object>) (Class<?>) SortedMap.class);
        final Object sortedMapRow = sortedMapRows.get(1).get(0);

        assertTrue(sortedMapRow instanceof SortedMap);
        assertEquals(25, ((SortedMap<?, ?>) sortedMapRow).get("age"));
        assertEquals("Alice", ((SortedMap<?, ?>) sortedMapRow).get("name"));
    }

    @Test
    public void testToMultimapWithMapRowType() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "city";
        Collection<String> valueColumns = Arrays.asList("id", "name", "age");
        IntFunction<ListMultimap<String, Map<String, Object>>> supplier = capacity -> CommonUtil.newLinkedListMultimap();

        ListMultimap<String, Map<String, Object>> result = dataset.toMultimap(0, dataset.size(), keyColumn, valueColumns, Clazz.ofMap(), supplier);

        Assertions.assertEquals(4, result.keySet().size());
        Map<String, Object> nycPerson = result.get("NYC").get(0);
        Assertions.assertEquals(1, nycPerson.get("id"));
        Assertions.assertEquals("John", nycPerson.get("name"));
    }

    @Test
    public void testToMultimapWithBeanRowType() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "city";
        Collection<String> valueColumns = Arrays.asList("id", "name", "age");
        IntFunction<ListMultimap<String, Person>> supplier = capacity -> CommonUtil.newLinkedListMultimap();

        ListMultimap<String, Person> result = dataset.toMultimap(0, dataset.size(), keyColumn, valueColumns, Person.class, supplier);

        Assertions.assertEquals(4, result.keySet().size());
        Person nycPerson = result.get("NYC").get(0);
        Assertions.assertEquals(1, nycPerson.getId());
        Assertions.assertEquals("John", nycPerson.getName());
    }

    @Test
    public void testToMultimap() {
        List<List<Object>> dupColumns = new ArrayList<>();
        dupColumns.add(Arrays.asList(1, 1, 2));
        dupColumns.add(Arrays.asList("A", "B", "C"));

        RowDataset dupDataset = new RowDataset(Arrays.asList("id", "value"), dupColumns);
        ListMultimap<Integer, String> multimap = dupDataset.toMultimap("id", "value");

        Assertions.assertEquals(2, multimap.get(1).size());
        Assertions.assertTrue(multimap.get(1).contains("A"));
        Assertions.assertTrue(multimap.get(1).contains("B"));
    }

    @Test
    public void testToMultimapWithClass() {
        final RowDataset dataset = createThreeRowScoreDataset();
        ListMultimap<Integer, TestBean> multimap = dataset.toMultimap("id", Arrays.asList("name", "age"), TestBean.class);
        Assertions.assertEquals(1, multimap.get(1).size());
        Assertions.assertEquals("John", multimap.get(1).get(0).name);
    }

    @Test
    public void testToMultimap_WithValueColumnNamesAndRange() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("dept", "name", "age")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList("Eng", "Eng", "HR")), new ArrayList<>(Arrays.asList("Alice", "Bob", "Charlie")),
                        new ArrayList<>(Arrays.asList(25, 30, 28)))));

        ListMultimap<Object, Object[]> result = ds.toMultimap(0, 2, "dept", Arrays.asList("name", "age"), Object[].class);

        assertEquals(2, result.get("Eng").size()); // rows 0 and 1 both have dept="Eng"
    }

    // ===== toMultimap with valueColumnNames and rowType =====

    @Test
    public void testToMultimap_WithValueColumnNamesArray() {
        RowDataset ds = new RowDataset(new ArrayList<>(Arrays.asList("dept", "name", "age")),
                new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList("Eng", "Eng", "HR")), new ArrayList<>(Arrays.asList("Alice", "Bob", "Charlie")),
                        new ArrayList<>(Arrays.asList(25, 30, 28)))));

        ListMultimap<Object, Object[]> result = ds.toMultimap("dept", Arrays.asList("name", "age"), Object[].class);

        assertNotNull(result);
        assertTrue(result.containsKey("Eng"));
        assertEquals(2, result.get("Eng").size());
    }

    // ========== toMultimap - with supplier ==========

    @Test
    public void testToMultimap_SingleValueCol_WithSupplier_ReturnsMultimap() {
        ListMultimap<Object, Object> result = dataset.toMultimap("id", "name", len -> CommonUtil.newLinkedListMultimap());
        assertNotNull(result);
        assertEquals(5, result.size());
    }

    @Test
    public void testToMultimapWithRowSupplier() {
        final RowDataset dataset = createFourRowCityDataset();
        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<ListMultimap<Integer, Object[]>> mapSupplier = IntFunctions.ofListMultimap();

            Multimap<Integer, Object[], List<Object[]>> result = dataset.toMultimap(0, dataset.size(), keyColumn, valueColumns, IntFunctions.ofObjectArray(),
                    mapSupplier);

            Assertions.assertEquals(4, result.totalValueCount());
            List<Object[]> firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.get(0)[0]);
            Assertions.assertEquals(25, firstValue.get(0)[1]);
        }
        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<ListMultimap<Integer, List<Object>>> mapSupplier = IntFunctions.ofListMultimap();

            Multimap<Integer, List<Object>, List<List<Object>>> result = dataset.toMultimap(0, dataset.size(), keyColumn, valueColumns, IntFunctions.ofList(),
                    mapSupplier);

            Assertions.assertEquals(4, result.totalValueCount());
            List<List<Object>> firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.get(0).get(0));
            Assertions.assertEquals(25, firstValue.get(0).get(1));
        }
        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<ListMultimap<Integer, Map<String, Object>>> mapSupplier = IntFunctions.ofListMultimap();

            Multimap<Integer, Map<String, Object>, List<Map<String, Object>>> result = dataset.toMultimap(0, dataset.size(), keyColumn, valueColumns,
                    IntFunctions.ofMap(), mapSupplier);

            Assertions.assertEquals(4, result.totalValueCount());
            List<Map<String, Object>> firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.get(0).get("name"));
            Assertions.assertEquals(25, firstValue.get(0).get("age"));
        }
        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<ListMultimap<Integer, Person>> mapSupplier = IntFunctions.ofListMultimap();

            Multimap<Integer, Person, List<Person>> result = dataset.toMultimap(0, dataset.size(), keyColumn, valueColumns, i -> new Person(), mapSupplier);

            Assertions.assertEquals(4, result.totalValueCount());
            List<Person> firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.get(0).getName());
            Assertions.assertEquals(25, firstValue.get(0).getAge());
        }
    }

    @Test
    public void testToMultimapWithRowSupplierSimple() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "city";
        Collection<String> valueColumns = Arrays.asList("name", "age");
        IntFunction<Object[]> rowSupplier = size -> new Object[size];

        ListMultimap<String, Object[]> result = dataset.toMultimap(keyColumn, valueColumns, rowSupplier);

        Assertions.assertEquals(4, result.keySet().size());
        Object[] nycData = result.get("NYC").get(0);
        Assertions.assertEquals("John", nycData[0]);
        Assertions.assertEquals(25, nycData[1]);
    }

    @Test
    public void testToMultimapWithRowSupplierAndSupplier() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "city";
        Collection<String> valueColumns = Arrays.asList("name", "age");
        IntFunction<Object[]> rowSupplier = size -> new Object[size];
        IntFunction<ListMultimap<String, Object[]>> supplier = capacity -> CommonUtil.newLinkedListMultimap();

        ListMultimap<String, Object[]> result = dataset.toMultimap(keyColumn, valueColumns, rowSupplier, supplier);

        Assertions.assertEquals(4, result.keySet().size());
        Object[] nycData = result.get("NYC").get(0);
        Assertions.assertEquals("John", nycData[0]);
        Assertions.assertEquals(25, nycData[1]);
    }

    @Test
    public void testToMultimapWithRowSupplierAndRange() {
        final RowDataset dataset = createFourRowCityDataset();
        String keyColumn = "city";
        Collection<String> valueColumns = Arrays.asList("name", "age");
        IntFunction<Object[]> rowSupplier = size -> new Object[size];

        ListMultimap<String, Object[]> result = dataset.toMultimap(1, 3, keyColumn, valueColumns, rowSupplier);

        Assertions.assertEquals(2, result.keySet().size());
        Assertions.assertTrue(result.containsKey("LA"));
        Assertions.assertTrue(result.containsKey("Chicago"));
    }

    // ========== toJson with Writer and empty columns ==========

    @Test
    public void testToJson_Writer_EmptyColumnNames_WritesEmptyArray() throws Exception {
        java.io.StringWriter sw = new java.io.StringWriter();
        dataset.toJson(0, dataset.size(), new ArrayList<>(), sw);
        assertEquals("[]", sw.toString());
    }

    @Test
    public void testToJson() {
        final RowDataset dataset = createThreeRowScoreDataset();
        String json = dataset.toJson();
        Assertions.assertTrue(json.contains("\"id\":1"));
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
        Assertions.assertTrue(json.contains("\"age\":25"));
    }

    @Test
    public void testToJsonWithRange() {
        final RowDataset dataset = createThreeRowScoreDataset();
        String json = dataset.toJson(0, 1);
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
        Assertions.assertFalse(json.contains("\"name\":\"Jane\""));
    }

    @Test
    public void testToJsonWithColumns() {
        final RowDataset dataset = createThreeRowScoreDataset();
        String json = dataset.toJson(0, 3, Arrays.asList("name", "age"));
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
        Assertions.assertTrue(json.contains("\"age\":25"));
        Assertions.assertFalse(json.contains("\"id\""));
        Assertions.assertFalse(json.contains("\"score\""));
    }

    @Test
    public void testToJsonEscapesColumnNames() {
        final List<String> columnNames = Arrays.asList("a\"b", "c\\d", "line\nbreak");
        final Dataset ds = Dataset.rows(columnNames, new Object[][] { { 1, 2, 3 } });

        final String json = ds.toJson();

        assertTrue(json.contains("\"a\\\"b\":1"));
        assertTrue(json.contains("\"c\\\\d\":2"));
        assertTrue(json.contains("\"line\\nbreak\":3"));

        final Dataset parsed = N.fromJson(json, Dataset.class);

        assertEquals(columnNames, parsed.columnNames());
        assertEquals(1, ((Number) parsed.get(0, 0)).intValue());
        assertEquals(2, ((Number) parsed.get(0, 1)).intValue());
        assertEquals(3, ((Number) parsed.get(0, 2)).intValue());
    }

    @Test
    public void testToJsonToFile() throws IOException {
        final RowDataset dataset = createThreeRowScoreDataset();
        File tempFile = File.createTempFile("dataset", ".json");
        tempFile.deleteOnExit();

        dataset.toJson(tempFile);
        String content = new String(java.nio.file.Files.readAllBytes(tempFile.toPath()));
        Assertions.assertTrue(content.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJsonToOutputStream() throws IOException {
        final RowDataset dataset = createThreeRowScoreDataset();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataset.toJson(baos);
        String json = baos.toString();
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJsonToWriter() throws IOException {
        final RowDataset dataset = createThreeRowScoreDataset();
        StringWriter writer = new StringWriter();
        dataset.toJson(writer);
        String json = writer.toString();
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
    }

    @Test
    public void testToJsonToOutputStreamWithRangeAndColumns() throws IOException {
        final RowDataset ds = createThreeRowScoreDataset();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        ds.toJson(0, 2, Arrays.asList("name", "age"), baos);
        String json = baos.toString();
        assertNotNull(json);
        assertTrue(json.contains("John"));
        assertFalse(json.contains("Bob"));
    }

    @Test
    public void testToJsonToWriterWithRangeAndColumns() throws IOException {
        final RowDataset ds = createThreeRowScoreDataset();
        StringWriter writer = new StringWriter();
        ds.toJson(0, 2, Arrays.asList("name", "age"), writer);
        String json = writer.toString();
        assertNotNull(json);
        assertTrue(json.contains("John"));
        assertFalse(json.contains("Bob"));
    }

    @Test
    public void testToJsonToFileWithRangeAndColumns(@TempDir Path tempDir) throws IOException {
        final RowDataset ds = createThreeRowScoreDataset();
        File file = tempDir.resolve("test.json").toFile();
        ds.toJson(0, 2, Arrays.asList("name", "age"), file);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }

    // ===== toJson with Writer =====

    @Test
    public void testToJson_WithWriter() throws Exception {
        java.io.StringWriter sw = new java.io.StringWriter();
        dataset.toJson(sw);
        String json = sw.toString();

        assertNotNull(json);
        assertTrue(json.contains("Alice"));
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));
    }

    @Test
    public void testToJson_WithWriterAndRange() throws Exception {
        java.io.StringWriter sw = new java.io.StringWriter();
        dataset.toJson(0, 2, sw);
        String json = sw.toString();

        assertNotNull(json);
        assertTrue(json.contains("Alice"));
        assertTrue(json.contains("Bob"));
        assertFalse(json.contains("Charlie"));
    }

    @Test
    public void testToJson_WithWriterRangeAndColumnNames() throws Exception {
        java.io.StringWriter sw = new java.io.StringWriter();
        dataset.toJson(0, 3, Arrays.asList("id", "name"), sw);
        String json = sw.toString();

        assertNotNull(json);
        assertTrue(json.contains("name"));
        assertFalse(json.contains("age"));
    }

    @Test
    public void testToXml() {
        final RowDataset dataset = createFiveRowCityDataset();
        String xml = dataset.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("<dataset>"));
        assertTrue(xml.contains("</dataset>"));
        assertTrue(xml.contains("<row>"));
        assertTrue(xml.contains("<id>"));
        assertTrue(xml.contains("<name>"));
    }

    @Test
    public void testToXmlWithCustomRowElementName() {
        final RowDataset dataset = createFiveRowCityDataset();
        String xml = dataset.toXml("record");
        assertNotNull(xml);
        assertTrue(xml.contains("<record>"));
        assertTrue(xml.contains("</record>"));
    }

    @Test
    public void testToXmlWithAllParameters() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collection<String> columns = CommonUtil.toList("name", "age");
        String xml = dataset.toXml(1, 3, columns, "employee");
        assertNotNull(xml);
        assertTrue(xml.contains("<employee>"));
        assertTrue(xml.contains("Jane"));
        assertTrue(xml.contains("30"));
    }

    @Test
    public void testToXmlWithRowRange() {
        final RowDataset dataset = createFiveRowCityDataset();
        String xml = dataset.toXml(1, 3);
        assertNotNull(xml);
        assertTrue(xml.contains("Jane"));
        assertTrue(xml.contains("Bob"));
        assertFalse(xml.contains("John"));
        assertFalse(xml.contains("Alice"));
    }

    @Test
    public void testToXmlWithRowRangeAndCustomElementName() {
        final RowDataset dataset = createFiveRowCityDataset();
        String xml = dataset.toXml(1, 3, "person");
        assertNotNull(xml);
        assertTrue(xml.contains("<person>"));
        assertTrue(xml.contains("Jane"));
        assertTrue(xml.contains("Bob"));
    }

    @Test
    public void testToXmlWithRowRangeAndColumnNames() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collection<String> columns = CommonUtil.toList("name", "age");
        String xml = dataset.toXml(1, 3, columns);
        assertNotNull(xml);
        assertTrue(xml.contains("Jane"));
        assertTrue(xml.contains("30"));
        assertFalse(xml.contains("<id>"));
        assertFalse(xml.contains("<city>"));
    }

    @Test
    public void testToXmlToFile(@TempDir Path tempDir) throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        File outputFile = tempDir.resolve("test.xml").toFile();
        dataset.toXml(outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("<dataset>"));
        assertTrue(content.contains("John"));
    }

    @Test
    public void testToXmlToFileWithCustomElementName(@TempDir Path tempDir) throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        File outputFile = tempDir.resolve("test2.xml").toFile();
        dataset.toXml("item", outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("<item>"));
    }

    @Test
    public void testToXmlToFileWithRowRange(@TempDir Path tempDir) throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        File outputFile = tempDir.resolve("test3.xml").toFile();
        dataset.toXml(1, 3, outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("Jane"));
        assertFalse(content.contains("John"));
    }

    @Test
    public void testToXmlToFileWithAllParams(@TempDir Path tempDir) throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        File outputFile = tempDir.resolve("test4.xml").toFile();
        Collection<String> columns = CommonUtil.toList("name", "age");
        dataset.toXml(0, 2, columns, "record", outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("<record>"));
        assertTrue(content.contains("John"));
        assertFalse(content.contains("<city>"));
    }

    @Test
    public void testToXmlToOutputStream() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataset.toXml(baos);

        String xml = baos.toString();
        assertTrue(xml.contains("<dataset>"));
        assertTrue(xml.contains("John"));
    }

    @Test
    public void testToXmlToOutputStreamWithElementName() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataset.toXml("data", baos);

        String xml = baos.toString();
        assertTrue(xml.contains("<data>"));
    }

    @Test
    public void testToXmlToOutputStreamWithRowRange() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataset.toXml(1, 3, baos);

        String xml = baos.toString();
        assertTrue(xml.contains("Jane"));
        assertFalse(xml.contains("John"));
    }

    @Test
    public void testToXmlToOutputStreamWithAllParams() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Collection<String> columns = CommonUtil.toList("name");
        dataset.toXml(0, 2, columns, "person", baos);

        String xml = baos.toString();
        assertTrue(xml.contains("<person>"));
        assertTrue(xml.contains("John"));
        assertFalse(xml.contains("<age>"));
    }

    @Test
    public void testToXmlToWriter() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        StringWriter writer = new StringWriter();
        dataset.toXml(writer);

        String xml = writer.toString();
        assertTrue(xml.contains("<dataset>"));
        assertTrue(xml.contains("John"));
    }

    @Test
    public void testToXmlToWriterWithElementName() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        StringWriter writer = new StringWriter();
        dataset.toXml("entry", writer);

        String xml = writer.toString();
        assertTrue(xml.contains("<entry>"));
    }

    @Test
    public void testToXmlToWriterWithRowRange() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        StringWriter writer = new StringWriter();
        dataset.toXml(2, 4, writer);

        String xml = writer.toString();
        assertTrue(xml.contains("Bob"));
        assertTrue(xml.contains("Alice"));
        assertFalse(xml.contains("John"));
    }

    @Test
    public void testToXmlToWriterWithAllParams() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        StringWriter writer = new StringWriter();
        Collection<String> columns = CommonUtil.toList("id", "name");
        dataset.toXml(0, 2, columns, "user", writer);

        String xml = writer.toString();
        assertTrue(xml.contains("<user>"));
        assertTrue(xml.contains("<id>1</id>"));
        assertTrue(xml.contains("John"));
        assertFalse(xml.contains("<age>"));
    }

    // ===== toXml with Writer and rowElementName =====

    @Test
    public void testToXml_WithRowElementNameAndWriter() throws Exception {
        java.io.StringWriter sw = new java.io.StringWriter();
        dataset.toXml(0, 2, dataset.columnNames(), "person", sw);
        String xml = sw.toString();

        assertNotNull(xml);
        assertTrue(xml.contains("<person>"));
        assertTrue(xml.contains("Alice"));
    }

    @Test
    public void testToCsv() {
        final RowDataset dataset = createFiveRowCityDataset();
        String csv = dataset.toCsv();
        assertNotNull(csv);
        assertTrue(csv.contains("\"id\",\"name\",\"age\",\"city\""));
        assertTrue(csv.contains("1,\"John\",25,\"NYC\""));
    }

    @Test
    public void testToCsvWithRowRangeAndColumns() {
        final RowDataset dataset = createFiveRowCityDataset();
        Collection<String> columns = CommonUtil.toList("name", "age");
        String csv = dataset.toCsv(1, 3, columns);
        assertNotNull(csv);
        assertTrue(csv.contains("\"name\",\"age\""));
        assertTrue(csv.contains("\"Jane\",30"));
        assertTrue(csv.contains("\"Bob\",35"));
        assertFalse(csv.contains("John"));
    }

    @Test
    public void testToCsvToFile(@TempDir Path tempDir) throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        File outputFile = tempDir.resolve("test.csv").toFile();
        dataset.toCsv(outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("\"id\",\"name\",\"age\",\"city\""));
        assertTrue(content.contains("\"John\""));
    }

    @Test
    public void testToCsvToFileWithParams(@TempDir Path tempDir) throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        File outputFile = tempDir.resolve("test2.csv").toFile();
        Collection<String> columns = CommonUtil.toList("name", "city");
        dataset.toCsv(1, 3, columns, outputFile);
        assertTrue(outputFile.exists());

        String content = IOUtil.readAllToString(outputFile);
        assertTrue(content.contains("\"name\",\"city\""));
        assertTrue(content.contains("\"Jane\",\"LA\""));
        assertFalse(content.contains("John"));
    }

    @Test
    public void testToCsvToOutputStream() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataset.toCsv(baos);

        String csv = baos.toString();
        assertTrue(csv.contains("\"id\",\"name\",\"age\",\"city\""));
        assertTrue(csv.contains("John"));
    }

    @Test
    public void testToCsvToOutputStreamWithParams() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Collection<String> columns = CommonUtil.toList("id", "name");
        dataset.toCsv(0, 2, columns, baos);

        String csv = baos.toString();
        assertTrue(csv.contains("\"id\",\"name\""));
        assertTrue(csv.contains("1,\"John\""));
        assertFalse(csv.contains("age"));
    }

    @Test
    public void testToCsvToWriter() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        StringWriter writer = new StringWriter();
        dataset.toCsv(writer);

        String csv = writer.toString();
        assertTrue(csv.contains("\"id\",\"name\",\"age\",\"city\""));
        assertTrue(csv.contains("\"John\""));
    }

    @Test
    public void testToCsvToWriterWithParams() throws IOException {
        final RowDataset dataset = createFiveRowCityDataset();
        StringWriter writer = new StringWriter();
        Collection<String> columns = CommonUtil.toList("name");
        dataset.toCsv(2, 4, columns, writer);

        String csv = writer.toString();
        assertTrue(csv.contains("name"));
        assertTrue(csv.contains("Bob"));
        assertTrue(csv.contains("Alice"));
        assertFalse(csv.contains("John"));
    }

}
