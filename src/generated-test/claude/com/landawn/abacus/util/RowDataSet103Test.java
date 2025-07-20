package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class RowDataSet103Test extends TestBase {

    private RowDataSet dataSet;
    private List<String> columnNames;
    private List<List<Object>> columnList;

    @BeforeEach
    public void setUp() {
        columnNames = Arrays.asList("id", "name", "age", "city");
        columnList = new ArrayList<>();
        columnList.add(Arrays.asList(1, 2, 3, 4)); // id column
        columnList.add(Arrays.asList("John", "Jane", "Bob", "Alice")); // name column
        columnList.add(Arrays.asList(25, 30, 35, 28)); // age column
        columnList.add(Arrays.asList("NYC", "LA", "Chicago", "Boston")); // city column

        dataSet = new RowDataSet(columnNames, columnList);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Person {
        private int id;
        private String name;
        private int age;
        private String city;
        private String skill;
    }

    @Test
    public void testToListWithColumnFilterAndConverter() {
        // Test with column filter - only select name and age columns
        Predicate<String> columnFilter = col -> col.equals("name") || col.equals("age");
        Function<String, String> columnConverter = String::toUpperCase;
        IntFunction<Map<String, Object>> rowSupplier = capacity -> new HashMap<>();

        List<Map<String, Object>> result = dataSet.toList(columnFilter, columnConverter, rowSupplier);

        Assertions.assertEquals(4, result.size());
        Map<String, Object> firstRow = result.get(0);
        Assertions.assertTrue(firstRow.containsKey("NAME"));
        Assertions.assertTrue(firstRow.containsKey("AGE"));
        Assertions.assertFalse(firstRow.containsKey("id"));
        Assertions.assertFalse(firstRow.containsKey("city"));
    }

    @Test
    public void testToListWithColumnFilterAndConverterNullValues() {
        // Test with null filter and converter
        IntFunction<List<Object>> rowSupplier = capacity -> new ArrayList<>();

        List<List<Object>> result = dataSet.toList(null, null, rowSupplier);

        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals(4, result.get(0).size()); // All columns included
    }

    @Test
    public void testToListWithRowIndexRange() {
        // Test with specific row range
        Predicate<String> columnFilter = col -> col.equals("name") || col.equals("age");
        IntFunction<Map<String, Object>> rowSupplier = capacity -> new HashMap<>();

        List<Map<String, Object>> result = dataSet.toList(1, 3, columnFilter, null, rowSupplier);

        Assertions.assertEquals(2, result.size()); // Rows 1 and 2 (0-based index)
        Map<String, Object> firstRow = result.get(0);
        Assertions.assertEquals("Jane", firstRow.get("name"));
        Assertions.assertEquals(30, firstRow.get("age"));
    }

    @Test
    public void testToMergedEntities() {
        // Create a dataset with duplicate IDs to test merging
        List<String> mergeColumnNames = Arrays.asList("id", "name", "skill");
        List<List<Object>> mergeColumnList = new ArrayList<>();
        mergeColumnList.add(Arrays.asList(1, 1, 2, 2)); // id column with duplicates
        mergeColumnList.add(Arrays.asList("John", "John", "Jane", "Jane")); // name column
        mergeColumnList.add(Arrays.asList("Java", "Python", "JavaScript", "SQL")); // skill column

        RowDataSet mergeDataSet = new RowDataSet(mergeColumnNames, mergeColumnList);

        // Define a Person class with skills list for testing
        Collection<String> idPropNames = Arrays.asList("id");
        Collection<String> selectPropNames = Arrays.asList("id", "name", "skill");
        Map<String, String> prefixAndFieldNameMap = new HashMap<>();

        // This will merge entities with the same ID
        List<Person> mergedEntities = mergeDataSet.toMergedEntities(idPropNames, selectPropNames, prefixAndFieldNameMap, Person.class);

        // Should result in 2 entities (one for each unique ID)
        Assertions.assertEquals(2, mergedEntities.size());
    }

    @Test
    public void testToMergedEntitiesWithInvalidIdProps() {
        Collection<String> idPropNames = Arrays.asList("nonexistent_id");
        Collection<String> selectPropNames = Arrays.asList("name", "age");
        Map<String, String> prefixAndFieldNameMap = new HashMap<>();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataSet.toMergedEntities(idPropNames, selectPropNames, prefixAndFieldNameMap, Person.class);
        });
    }

    @Test
    public void testToMapWithKeyAndValueColumns() {
        String keyColumn = "id";
        Collection<String> valueColumns = Arrays.asList("name", "age");
        IntFunction<Map<Integer, Object[]>> supplier = capacity -> new HashMap<>();

        Map<Integer, Object[]> result = dataSet.toMap(0, dataSet.size(), keyColumn, valueColumns, Object[].class, supplier);

        Assertions.assertEquals(4, result.size());
        Object[] firstValue = result.get(1);
        Assertions.assertEquals("John", firstValue[0]);
        Assertions.assertEquals(25, firstValue[1]);
    }

    @Test
    public void testToMapWithCollectionRowType() {
        String keyColumn = "id";
        Collection<String> valueColumns = Arrays.asList("name", "age", "city");
        IntFunction<Map<Integer, List<Object>>> supplier = capacity -> new HashMap<>();

        Map<Integer, List<Object>> result = dataSet.toMap(0, dataSet.size(), keyColumn, valueColumns, Clazz.ofList(), supplier);

        Assertions.assertEquals(4, result.size());
        List<Object> firstValue = result.get(1);
        Assertions.assertEquals(3, firstValue.size());
        Assertions.assertTrue(firstValue.contains("John"));
        Assertions.assertTrue(firstValue.contains(25));
        Assertions.assertTrue(firstValue.contains("NYC"));
    }

    @Test
    public void testToMapWithMapRowType() {
        String keyColumn = "id";
        Collection<String> valueColumns = Arrays.asList("name", "age", "city");
        IntFunction<Map<Integer, Map<String, Object>>> supplier = capacity -> new HashMap<>();

        Map<Integer, Map<String, Object>> result = dataSet.toMap(0, dataSet.size(), keyColumn, valueColumns, Clazz.ofMap(), supplier);

        Assertions.assertEquals(4, result.size());
        Map<String, Object> firstValue = result.get(1);
        Assertions.assertEquals("John", firstValue.get("name"));
        Assertions.assertEquals(25, firstValue.get("age"));
        Assertions.assertEquals("NYC", firstValue.get("city"));
    }

    @Test
    public void testToMapWithBeanRowType() {
        String keyColumn = "id";
        Collection<String> valueColumns = Arrays.asList("name", "age", "city");
        IntFunction<Map<Integer, Person>> supplier = capacity -> new HashMap<>();

        Map<Integer, Person> result = dataSet.toMap(0, dataSet.size(), keyColumn, valueColumns, Person.class, supplier);

        Assertions.assertEquals(4, result.size());
        Person firstPerson = result.get(1);
        Assertions.assertEquals("John", firstPerson.getName());
        Assertions.assertEquals(25, firstPerson.getAge());
        Assertions.assertEquals("NYC", firstPerson.getCity());
    }

    @Test
    public void testToMapWithRowSupplier() {
        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<Map<Integer, Object[]>> mapSupplier = capacity -> new HashMap<>();

            Map<Integer, Object[]> result = dataSet.toMap(0, dataSet.size(), keyColumn, valueColumns, IntFunctions.ofObjectArray(), mapSupplier);

            Assertions.assertEquals(4, result.size());
            Object[] firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue[0]);
            Assertions.assertEquals(25, firstValue[1]);
        }
        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<Map<Integer, List<Object>>> mapSupplier = capacity -> new HashMap<>();

            Map<Integer, List<Object>> result = dataSet.toMap(0, dataSet.size(), keyColumn, valueColumns, IntFunctions.ofList(), mapSupplier);

            Assertions.assertEquals(4, result.size());
            List<Object> firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.get(0));
            Assertions.assertEquals(25, firstValue.get(1));
        }

        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<Map<Integer, Map<String, Object>>> mapSupplier = capacity -> new HashMap<>();

            Map<Integer, Map<String, Object>> result = dataSet.toMap(0, dataSet.size(), keyColumn, valueColumns, IntFunctions.ofMap(), mapSupplier);

            Assertions.assertEquals(4, result.size());
            Map<String, Object> firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.get("name"));
            Assertions.assertEquals(25, firstValue.get("age"));
        }

        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<Map<Integer, Person>> mapSupplier = capacity -> new HashMap<>();

            Map<Integer, Person> result = dataSet.toMap(0, dataSet.size(), keyColumn, valueColumns, i -> new Person(), mapSupplier);

            Assertions.assertEquals(4, result.size());
            Person firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.getName());
            Assertions.assertEquals(25, firstValue.getAge());
        }
    }

    @Test
    public void testToMultimapWithRowSupplier() {
        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<ListMultimap<Integer, Object[]>> mapSupplier = IntFunctions.ofListMultimap();

            Multimap<Integer, Object[], List<Object[]>> result = dataSet.toMultimap(0, dataSet.size(), keyColumn, valueColumns, IntFunctions.ofObjectArray(),
                    mapSupplier);

            Assertions.assertEquals(4, result.size());
            List<Object[]> firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.get(0)[0]);
            Assertions.assertEquals(25, firstValue.get(0)[1]);
        }
        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<ListMultimap<Integer, List<Object>>> mapSupplier = IntFunctions.ofListMultimap();

            Multimap<Integer, List<Object>, List<List<Object>>> result = dataSet.toMultimap(0, dataSet.size(), keyColumn, valueColumns, IntFunctions.ofList(),
                    mapSupplier);

            Assertions.assertEquals(4, result.size());
            List<List<Object>> firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.get(0).get(0));
            Assertions.assertEquals(25, firstValue.get(0).get(1));
        }
        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<ListMultimap<Integer, Map<String, Object>>> mapSupplier = IntFunctions.ofListMultimap();

            Multimap<Integer, Map<String, Object>, List<Map<String, Object>>> result = dataSet.toMultimap(0, dataSet.size(), keyColumn, valueColumns,
                    IntFunctions.ofMap(), mapSupplier);

            Assertions.assertEquals(4, result.size());
            List<Map<String, Object>> firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.get(0).get("name"));
            Assertions.assertEquals(25, firstValue.get(0).get("age"));
        }
        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<ListMultimap<Integer, Person>> mapSupplier = IntFunctions.ofListMultimap();

            Multimap<Integer, Person, List<Person>> result = dataSet.toMultimap(0, dataSet.size(), keyColumn, valueColumns, i -> new Person(), mapSupplier);

            Assertions.assertEquals(4, result.size());
            List<Person> firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.get(0).getName());
            Assertions.assertEquals(25, firstValue.get(0).getAge());
        }
    }

    @Test
    public void testToMultimapWithCollectionRowType() {
        // Create dataset with duplicate keys for multimap testing
        List<String> dupColumnNames = Arrays.asList("category", "product", "price");
        List<List<Object>> dupColumnList = new ArrayList<>();
        dupColumnList.add(Arrays.asList("A", "A", "B", "B")); // category with duplicates
        dupColumnList.add(Arrays.asList("P1", "P2", "P3", "P4")); // product
        dupColumnList.add(Arrays.asList(10, 20, 30, 40)); // price

        RowDataSet dupDataSet = new RowDataSet(dupColumnNames, dupColumnList);

        String keyColumn = "category";
        Collection<String> valueColumns = Arrays.asList("product", "price");
        IntFunction<ListMultimap<String, List<Object>>> supplier = capacity -> N.newLinkedListMultimap();

        ListMultimap<String, List<Object>> result = dupDataSet.toMultimap(0, dupDataSet.size(), keyColumn, valueColumns, Clazz.ofList(), supplier);

        Assertions.assertEquals(2, result.keySet().size());
        Assertions.assertEquals(2, result.get("A").size());
        Assertions.assertEquals(2, result.get("B").size());
    }

    @Test
    public void testToMultimapWithMapRowType() {
        String keyColumn = "city";
        Collection<String> valueColumns = Arrays.asList("id", "name", "age");
        IntFunction<ListMultimap<String, Map<String, Object>>> supplier = capacity -> N.newLinkedListMultimap();

        ListMultimap<String, Map<String, Object>> result = dataSet.toMultimap(0, dataSet.size(), keyColumn, valueColumns, Clazz.ofMap(), supplier);

        Assertions.assertEquals(4, result.keySet().size()); // Each city is unique in our test data
        Map<String, Object> nycPerson = result.get("NYC").get(0);
        Assertions.assertEquals(1, nycPerson.get("id"));
        Assertions.assertEquals("John", nycPerson.get("name"));
    }

    @Test
    public void testToMultimapWithBeanRowType() {
        String keyColumn = "city";
        Collection<String> valueColumns = Arrays.asList("id", "name", "age");
        IntFunction<ListMultimap<String, Person>> supplier = capacity -> N.newLinkedListMultimap();

        ListMultimap<String, Person> result = dataSet.toMultimap(0, dataSet.size(), keyColumn, valueColumns, Person.class, supplier);

        Assertions.assertEquals(4, result.keySet().size());
        Person nycPerson = result.get("NYC").get(0);
        Assertions.assertEquals(1, nycPerson.getId());
        Assertions.assertEquals("John", nycPerson.getName());
    }

    @Test
    public void testToMultimapWithRowSupplierSimple() {
        String keyColumn = "city";
        Collection<String> valueColumns = Arrays.asList("name", "age");
        IntFunction<Object[]> rowSupplier = size -> new Object[size];

        ListMultimap<String, Object[]> result = dataSet.toMultimap(keyColumn, valueColumns, rowSupplier);

        Assertions.assertEquals(4, result.keySet().size());
        Object[] nycData = result.get("NYC").get(0);
        Assertions.assertEquals("John", nycData[0]);
        Assertions.assertEquals(25, nycData[1]);
    }

    @Test
    public void testToMultimapWithRowSupplierAndSupplier() {
        String keyColumn = "city";
        Collection<String> valueColumns = Arrays.asList("name", "age");
        IntFunction<Object[]> rowSupplier = size -> new Object[size];
        IntFunction<ListMultimap<String, Object[]>> supplier = capacity -> N.newLinkedListMultimap();

        ListMultimap<String, Object[]> result = dataSet.toMultimap(keyColumn, valueColumns, rowSupplier, supplier);

        Assertions.assertEquals(4, result.keySet().size());
        Object[] nycData = result.get("NYC").get(0);
        Assertions.assertEquals("John", nycData[0]);
        Assertions.assertEquals(25, nycData[1]);
    }

    @Test
    public void testToMultimapWithRowSupplierAndRange() {
        String keyColumn = "city";
        Collection<String> valueColumns = Arrays.asList("name", "age");
        IntFunction<Object[]> rowSupplier = size -> new Object[size];

        ListMultimap<String, Object[]> result = dataSet.toMultimap(1, 3, keyColumn, valueColumns, rowSupplier);

        Assertions.assertEquals(2, result.keySet().size()); // Only rows 1 and 2
        Assertions.assertTrue(result.containsKey("LA"));
        Assertions.assertTrue(result.containsKey("Chicago"));
    }

    @Test
    public void testGroupBy() {
        // Create dataset with groupable data
        List<String> groupColumnNames = Arrays.asList("department", "employee", "salary");
        List<List<Object>> groupColumnList = new ArrayList<>();
        groupColumnList.add(Arrays.asList("IT", "IT", "HR", "HR")); // department
        groupColumnList.add(Arrays.asList("John", "Jane", "Bob", "Alice")); // employee
        groupColumnList.add(Arrays.asList(70000, 80000, 60000, 65000)); // salary

        RowDataSet groupDataSet = new RowDataSet(groupColumnNames, groupColumnList);

        String keyColumn = "department";
        Collection<String> aggregateColumns = Arrays.asList("employee", "salary");
        String aggregateResultColumn = "employees";

        DataSet grouped = groupDataSet.groupBy(keyColumn, null, aggregateColumns, aggregateResultColumn, List.class);

        Assertions.assertEquals(2, grouped.columnCount());
        Assertions.assertEquals(2, grouped.size()); // Two departments
        Assertions.assertTrue(grouped.containsColumn("department"));
        Assertions.assertTrue(grouped.containsColumn("employees"));
    }

    @Test
    public void testGroupByWithKeyExtractor() {
        String keyColumn = "age";
        Function<Integer, String> keyExtractor = age -> age < 30 ? "Young" : "Adult";
        Collection<String> aggregateColumns = Arrays.asList("name", "city");
        String aggregateResultColumn = "people";

        DataSet grouped = dataSet.groupBy(keyColumn, keyExtractor, aggregateColumns, aggregateResultColumn, Map.class);

        Assertions.assertEquals(2, grouped.columnCount());
        Assertions.assertEquals(2, grouped.size()); // Young and Adult groups
    }

    @Test
    public void testIntersection() {
        // Create another dataset with some overlapping data
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(2, 3, 5, 6)); // id column
        otherColumnList.add(Arrays.asList("Jane", "Bob", "Eve", "Frank")); // name column
        otherColumnList.add(Arrays.asList(30, 35, 40, 45)); // age column
        otherColumnList.add(Arrays.asList("LA", "Chicago", "Miami", "Seattle")); // city column

        RowDataSet otherDataSet = new RowDataSet(otherColumnNames, otherColumnList);

        DataSet intersection = dataSet.intersection(otherDataSet);

        // Should contain rows with id 2 and 3
        Assertions.assertEquals(2, intersection.size());
    }

    @Test
    public void testIntersectionWithRequireSameColumns() {
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(2, 3)); // id column
        otherColumnList.add(Arrays.asList("Jane", "Bob")); // name column
        otherColumnList.add(Arrays.asList(30, 35)); // age column
        otherColumnList.add(Arrays.asList("LA", "Chicago")); // city column

        RowDataSet otherDataSet = new RowDataSet(otherColumnNames, otherColumnList);

        DataSet intersection = dataSet.intersection(otherDataSet, true);

        Assertions.assertEquals(2, intersection.size());
    }

    @Test
    public void testIntersectionWithKeyColumns() {
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(1, 2, 5, 6)); // id column
        otherColumnList.add(Arrays.asList("Different", "Different", "Eve", "Frank")); // different names
        otherColumnList.add(Arrays.asList(30, 35, 40, 45)); // age column
        otherColumnList.add(Arrays.asList("LA", "Chicago", "Miami", "Seattle")); // city column

        RowDataSet otherDataSet = new RowDataSet(otherColumnNames, otherColumnList);

        // Use only 'id' as key column
        Collection<String> keyColumns = Arrays.asList("id");
        DataSet intersection = dataSet.intersection(otherDataSet, keyColumns);

        // Should find 2 rows with matching ids (1 and 2)
        Assertions.assertEquals(2, intersection.size());
    }

    @Test
    public void testDifference() {
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(2, 3, 5, 6)); // id column
        otherColumnList.add(Arrays.asList("Jane", "Bob", "Eve", "Frank")); // name column
        otherColumnList.add(Arrays.asList(30, 35, 40, 45)); // age column
        otherColumnList.add(Arrays.asList("LA", "Chicago", "Miami", "Seattle")); // city column

        RowDataSet otherDataSet = new RowDataSet(otherColumnNames, otherColumnList);

        DataSet difference = dataSet.difference(otherDataSet);

        // Should contain rows with id 1 and 4 (not in other dataset)
        Assertions.assertEquals(2, difference.size());
    }

    @Test
    public void testDifferenceWithRequireSameColumns() {
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(3, 4)); // id column
        otherColumnList.add(Arrays.asList("Bob", "Alice")); // name column
        otherColumnList.add(Arrays.asList(35, 28)); // age column
        otherColumnList.add(Arrays.asList("Chicago", "Boston")); // city column

        RowDataSet otherDataSet = new RowDataSet(otherColumnNames, otherColumnList);

        DataSet difference = dataSet.difference(otherDataSet, true);

        // Should contain rows with id 1 and 2
        Assertions.assertEquals(2, difference.size());
    }

    @Test
    public void testDifferenceWithKeyColumns() {
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(1, 2)); // id column
        otherColumnList.add(Arrays.asList("Different", "Different")); // different names
        otherColumnList.add(Arrays.asList(30, 35)); // age column
        otherColumnList.add(Arrays.asList("LA", "Chicago")); // city column

        RowDataSet otherDataSet = new RowDataSet(otherColumnNames, otherColumnList);

        // Use only 'id' as key column
        Collection<String> keyColumns = Arrays.asList("id");
        DataSet difference = dataSet.difference(otherDataSet, keyColumns);

        // Should find 2 rows with ids 3 and 4 (not in other dataset)
        Assertions.assertEquals(2, difference.size());
    }

    @Test
    public void testSymmetricDifference() {
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(2, 3, 5, 6)); // id column
        otherColumnList.add(Arrays.asList("Jane", "Bob", "Eve", "Frank")); // name column
        otherColumnList.add(Arrays.asList(30, 35, 40, 45)); // age column
        otherColumnList.add(Arrays.asList("LA", "Chicago", "Miami", "Seattle")); // city column

        RowDataSet otherDataSet = new RowDataSet(otherColumnNames, otherColumnList);

        DataSet symmetricDiff = dataSet.symmetricDifference(otherDataSet);

        // Should contain rows with id 1, 4 (from first) and 5, 6 (from second)
        Assertions.assertEquals(4, symmetricDiff.size());
    }

    @Test
    public void testSymmetricDifferenceWithRequireSameColumns() {
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(3, 4, 5)); // id column
        otherColumnList.add(Arrays.asList("Bob", "Alice", "Eve")); // name column
        otherColumnList.add(Arrays.asList(35, 28, 40)); // age column
        otherColumnList.add(Arrays.asList("Chicago", "Boston", "Miami")); // city column

        RowDataSet otherDataSet = new RowDataSet(otherColumnNames, otherColumnList);

        DataSet symmetricDiff = dataSet.symmetricDifference(otherDataSet, true);

        // Should contain rows with id 1, 2 (from first) and 5 (from second)
        Assertions.assertEquals(3, symmetricDiff.size());
    }

    @Test
    public void testSymmetricDifferenceWithKeyColumns() {
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(1, 5, 6)); // id column
        otherColumnList.add(Arrays.asList("Different", "Eve", "Frank")); // name column
        otherColumnList.add(Arrays.asList(30, 40, 45)); // age column
        otherColumnList.add(Arrays.asList("LA", "Miami", "Seattle")); // city column

        RowDataSet otherDataSet = new RowDataSet(otherColumnNames, otherColumnList);

        // Use only 'id' as key column
        Collection<String> keyColumns = Arrays.asList("id");
        DataSet symmetricDiff = dataSet.symmetricDifference(otherDataSet, keyColumns);

        // Should contain rows with id 2, 3, 4 (from first) and 5, 6 (from second)
        Assertions.assertEquals(5, symmetricDiff.size());
    }

    @Test
    public void testSymmetricDifferenceWithKeyColumnsAndRequireSameColumns() {
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(2, 5)); // id column
        otherColumnList.add(Arrays.asList("Jane", "Eve")); // name column
        otherColumnList.add(Arrays.asList(30, 40)); // age column
        otherColumnList.add(Arrays.asList("LA", "Miami")); // city column

        RowDataSet otherDataSet = new RowDataSet(otherColumnNames, otherColumnList);

        Collection<String> keyColumns = Arrays.asList("id");
        DataSet symmetricDiff = dataSet.symmetricDifference(otherDataSet, keyColumns, true);

        // Should contain rows with id 1, 3, 4 (from first) and 5 (from second)
        Assertions.assertEquals(4, symmetricDiff.size());
    }

    // Edge case tests

    @Test
    public void testToListWithEmptyDataSet() {
        RowDataSet emptyDataSet = new RowDataSet(new ArrayList<>(), new ArrayList<>());
        IntFunction<List<Object>> rowSupplier = capacity -> new ArrayList<>();

        List<List<Object>> result = emptyDataSet.toList(null, null, rowSupplier);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testToMapWithInvalidRowType() {
        String keyColumn = "id";
        Collection<String> valueColumns = Arrays.asList("name", "age");
        IntFunction<Map<Integer, String>> supplier = capacity -> new HashMap<>();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataSet.toMap(0, dataSet.size(), keyColumn, valueColumns, String.class, supplier);
        });
    }

    @Test
    public void testGroupByWithEmptyAggregateColumns() {
        String keyColumn = "city";
        Collection<String> aggregateColumns = new ArrayList<>();
        String aggregateResultColumn = "data";

        Assertions.assertThrows(IllegalArgumentException.class, () -> dataSet.groupBy(keyColumn, null, aggregateColumns, aggregateResultColumn, List.class));
    }
}