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
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Tag("new-test")
public class RowDataset103Test extends TestBase {

    private RowDataset dataset;
    private List<String> columnNames;
    private List<List<Object>> columnList;

    @BeforeEach
    public void setUp() {
        columnNames = Arrays.asList("id", "name", "age", "city");
        columnList = new ArrayList<>();
        columnList.add(Arrays.asList(1, 2, 3, 4));
        columnList.add(Arrays.asList("John", "Jane", "Bob", "Alice"));
        columnList.add(Arrays.asList(25, 30, 35, 28));
        columnList.add(Arrays.asList("NYC", "LA", "Chicago", "Boston"));

        dataset = new RowDataset(columnNames, columnList);
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
        IntFunction<List<Object>> rowSupplier = capacity -> new ArrayList<>();

        List<List<Object>> result = dataset.toList(null, null, rowSupplier);

        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals(4, result.get(0).size());
    }

    @Test
    public void testToListWithRowIndexRange() {
        Predicate<String> columnFilter = col -> col.equals("name") || col.equals("age");
        IntFunction<Map<String, Object>> rowSupplier = capacity -> new HashMap<>();

        List<Map<String, Object>> result = dataset.toList(1, 3, columnFilter, null, rowSupplier);

        Assertions.assertEquals(2, result.size());
        Map<String, Object> firstRow = result.get(0);
        Assertions.assertEquals("Jane", firstRow.get("name"));
        Assertions.assertEquals(30, firstRow.get("age"));
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

        List<Person> mergedEntities = mergeDataset.toMergedEntities(idPropNames, selectPropNames, prefixAndFieldNameMap, Person.class);

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
    public void testToMapWithKeyAndValueColumns() {
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

    @Test
    public void testToMapWithMapRowType() {
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
    public void testToMapWithRowSupplier() {
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
    public void testToMultimapWithRowSupplier() {
        {
            String keyColumn = "id";
            Collection<String> valueColumns = Arrays.asList("name", "age");
            IntFunction<ListMultimap<Integer, Object[]>> mapSupplier = IntFunctions.ofListMultimap();

            Multimap<Integer, Object[], List<Object[]>> result = dataset.toMultimap(0, dataset.size(), keyColumn, valueColumns, IntFunctions.ofObjectArray(),
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

            Multimap<Integer, List<Object>, List<List<Object>>> result = dataset.toMultimap(0, dataset.size(), keyColumn, valueColumns, IntFunctions.ofList(),
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

            Multimap<Integer, Map<String, Object>, List<Map<String, Object>>> result = dataset.toMultimap(0, dataset.size(), keyColumn, valueColumns,
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

            Multimap<Integer, Person, List<Person>> result = dataset.toMultimap(0, dataset.size(), keyColumn, valueColumns, i -> new Person(), mapSupplier);

            Assertions.assertEquals(4, result.size());
            List<Person> firstValue = result.get(1);
            Assertions.assertEquals("John", firstValue.get(0).getName());
            Assertions.assertEquals(25, firstValue.get(0).getAge());
        }
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
        IntFunction<ListMultimap<String, List<Object>>> supplier = capacity -> N.newLinkedListMultimap();

        ListMultimap<String, List<Object>> result = dupDataset.toMultimap(0, dupDataset.size(), keyColumn, valueColumns, Clazz.ofList(), supplier);

        Assertions.assertEquals(2, result.keySet().size());
        Assertions.assertEquals(2, result.get("A").size());
        Assertions.assertEquals(2, result.get("B").size());
    }

    @Test
    public void testToMultimapWithMapRowType() {
        String keyColumn = "city";
        Collection<String> valueColumns = Arrays.asList("id", "name", "age");
        IntFunction<ListMultimap<String, Map<String, Object>>> supplier = capacity -> N.newLinkedListMultimap();

        ListMultimap<String, Map<String, Object>> result = dataset.toMultimap(0, dataset.size(), keyColumn, valueColumns, Clazz.ofMap(), supplier);

        Assertions.assertEquals(4, result.keySet().size());
        Map<String, Object> nycPerson = result.get("NYC").get(0);
        Assertions.assertEquals(1, nycPerson.get("id"));
        Assertions.assertEquals("John", nycPerson.get("name"));
    }

    @Test
    public void testToMultimapWithBeanRowType() {
        String keyColumn = "city";
        Collection<String> valueColumns = Arrays.asList("id", "name", "age");
        IntFunction<ListMultimap<String, Person>> supplier = capacity -> N.newLinkedListMultimap();

        ListMultimap<String, Person> result = dataset.toMultimap(0, dataset.size(), keyColumn, valueColumns, Person.class, supplier);

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

        ListMultimap<String, Object[]> result = dataset.toMultimap(keyColumn, valueColumns, rowSupplier);

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

        ListMultimap<String, Object[]> result = dataset.toMultimap(keyColumn, valueColumns, rowSupplier, supplier);

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

        ListMultimap<String, Object[]> result = dataset.toMultimap(1, 3, keyColumn, valueColumns, rowSupplier);

        Assertions.assertEquals(2, result.keySet().size());
        Assertions.assertTrue(result.containsKey("LA"));
        Assertions.assertTrue(result.containsKey("Chicago"));
    }

    @Test
    public void testGroupBy() {
        List<String> groupColumnNames = Arrays.asList("department", "employee", "salary");
        List<List<Object>> groupColumnList = new ArrayList<>();
        groupColumnList.add(Arrays.asList("IT", "IT", "HR", "HR"));
        groupColumnList.add(Arrays.asList("John", "Jane", "Bob", "Alice"));
        groupColumnList.add(Arrays.asList(70000, 80000, 60000, 65000));

        RowDataset groupDataset = new RowDataset(groupColumnNames, groupColumnList);

        String keyColumn = "department";
        Collection<String> aggregateColumns = Arrays.asList("employee", "salary");
        String aggregateResultColumn = "employees";

        Dataset grouped = groupDataset.groupBy(keyColumn, null, aggregateColumns, aggregateResultColumn, List.class);

        Assertions.assertEquals(2, grouped.columnCount());
        Assertions.assertEquals(2, grouped.size());
        Assertions.assertTrue(grouped.containsColumn("department"));
        Assertions.assertTrue(grouped.containsColumn("employees"));
    }

    @Test
    public void testGroupByWithKeyExtractor() {
        String keyColumn = "age";
        Function<Integer, String> keyExtractor = age -> age < 30 ? "Young" : "Adult";
        Collection<String> aggregateColumns = Arrays.asList("name", "city");
        String aggregateResultColumn = "people";

        Dataset grouped = dataset.groupBy(keyColumn, keyExtractor, aggregateColumns, aggregateResultColumn, Map.class);

        Assertions.assertEquals(2, grouped.columnCount());
        Assertions.assertEquals(2, grouped.size());
    }

    @Test
    public void testIntersection() {
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(2, 3, 5, 6));
        otherColumnList.add(Arrays.asList("Jane", "Bob", "Eve", "Frank"));
        otherColumnList.add(Arrays.asList(30, 35, 40, 45));
        otherColumnList.add(Arrays.asList("LA", "Chicago", "Miami", "Seattle"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Dataset intersection = dataset.intersection(otherDataset);

        Assertions.assertEquals(2, intersection.size());
    }

    @Test
    public void testIntersectionWithRequireSameColumns() {
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(2, 3));
        otherColumnList.add(Arrays.asList("Jane", "Bob"));
        otherColumnList.add(Arrays.asList(30, 35));
        otherColumnList.add(Arrays.asList("LA", "Chicago"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Dataset intersection = dataset.intersection(otherDataset, true);

        Assertions.assertEquals(2, intersection.size());
    }

    @Test
    public void testIntersectionWithKeyColumns() {
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(1, 2, 5, 6));
        otherColumnList.add(Arrays.asList("Different", "Different", "Eve", "Frank"));
        otherColumnList.add(Arrays.asList(30, 35, 40, 45));
        otherColumnList.add(Arrays.asList("LA", "Chicago", "Miami", "Seattle"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Collection<String> keyColumns = Arrays.asList("id");
        Dataset intersection = dataset.intersection(otherDataset, keyColumns);

        Assertions.assertEquals(2, intersection.size());
    }

    @Test
    public void testDifference() {
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(2, 3, 5, 6));
        otherColumnList.add(Arrays.asList("Jane", "Bob", "Eve", "Frank"));
        otherColumnList.add(Arrays.asList(30, 35, 40, 45));
        otherColumnList.add(Arrays.asList("LA", "Chicago", "Miami", "Seattle"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Dataset difference = dataset.difference(otherDataset);

        Assertions.assertEquals(2, difference.size());
    }

    @Test
    public void testDifferenceWithRequireSameColumns() {
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(3, 4));
        otherColumnList.add(Arrays.asList("Bob", "Alice"));
        otherColumnList.add(Arrays.asList(35, 28));
        otherColumnList.add(Arrays.asList("Chicago", "Boston"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Dataset difference = dataset.difference(otherDataset, true);

        Assertions.assertEquals(2, difference.size());
    }

    @Test
    public void testDifferenceWithKeyColumns() {
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(1, 2));
        otherColumnList.add(Arrays.asList("Different", "Different"));
        otherColumnList.add(Arrays.asList(30, 35));
        otherColumnList.add(Arrays.asList("LA", "Chicago"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Collection<String> keyColumns = Arrays.asList("id");
        Dataset difference = dataset.difference(otherDataset, keyColumns);

        Assertions.assertEquals(2, difference.size());
    }

    @Test
    public void testSymmetricDifference() {
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(2, 3, 5, 6));
        otherColumnList.add(Arrays.asList("Jane", "Bob", "Eve", "Frank"));
        otherColumnList.add(Arrays.asList(30, 35, 40, 45));
        otherColumnList.add(Arrays.asList("LA", "Chicago", "Miami", "Seattle"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Dataset symmetricDiff = dataset.symmetricDifference(otherDataset);

        Assertions.assertEquals(4, symmetricDiff.size());
    }

    @Test
    public void testSymmetricDifferenceWithRequireSameColumns() {
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(3, 4, 5));
        otherColumnList.add(Arrays.asList("Bob", "Alice", "Eve"));
        otherColumnList.add(Arrays.asList(35, 28, 40));
        otherColumnList.add(Arrays.asList("Chicago", "Boston", "Miami"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Dataset symmetricDiff = dataset.symmetricDifference(otherDataset, true);

        Assertions.assertEquals(3, symmetricDiff.size());
    }

    @Test
    public void testSymmetricDifferenceWithKeyColumns() {
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(1, 5, 6));
        otherColumnList.add(Arrays.asList("Different", "Eve", "Frank"));
        otherColumnList.add(Arrays.asList(30, 40, 45));
        otherColumnList.add(Arrays.asList("LA", "Miami", "Seattle"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Collection<String> keyColumns = Arrays.asList("id");
        Dataset symmetricDiff = dataset.symmetricDifference(otherDataset, keyColumns);

        Assertions.assertEquals(5, symmetricDiff.size());
    }

    @Test
    public void testSymmetricDifferenceWithKeyColumnsAndRequireSameColumns() {
        List<String> otherColumnNames = Arrays.asList("id", "name", "age", "city");
        List<List<Object>> otherColumnList = new ArrayList<>();
        otherColumnList.add(Arrays.asList(2, 5));
        otherColumnList.add(Arrays.asList("Jane", "Eve"));
        otherColumnList.add(Arrays.asList(30, 40));
        otherColumnList.add(Arrays.asList("LA", "Miami"));

        RowDataset otherDataset = new RowDataset(otherColumnNames, otherColumnList);

        Collection<String> keyColumns = Arrays.asList("id");
        Dataset symmetricDiff = dataset.symmetricDifference(otherDataset, keyColumns, true);

        Assertions.assertEquals(4, symmetricDiff.size());
    }

    @Test
    public void testToListWithEmptyDataset() {
        RowDataset emptyDataset = new RowDataset(new ArrayList<>(), new ArrayList<>());
        IntFunction<List<Object>> rowSupplier = capacity -> new ArrayList<>();

        List<List<Object>> result = emptyDataset.toList(null, null, rowSupplier);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testToMapWithInvalidRowType() {
        String keyColumn = "id";
        Collection<String> valueColumns = Arrays.asList("name", "age");
        IntFunction<Map<Integer, String>> supplier = capacity -> new HashMap<>();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            dataset.toMap(0, dataset.size(), keyColumn, valueColumns, String.class, supplier);
        });
    }

    @Test
    public void testGroupByWithEmptyAggregateColumns() {
        String keyColumn = "city";
        Collection<String> aggregateColumns = new ArrayList<>();
        String aggregateResultColumn = "data";

        Assertions.assertThrows(IllegalArgumentException.class, () -> dataset.groupBy(keyColumn, null, aggregateColumns, aggregateResultColumn, List.class));
    }
}
