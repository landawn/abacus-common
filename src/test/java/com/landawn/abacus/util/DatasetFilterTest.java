package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.stream.Stream;

import testfixtures.entity.extendDirty.basic.Account;

public class DatasetFilterTest extends DatasetTestSupport {
    @Test
    public void filter_byPredicateOnDisposableObjArray() {
        Dataset filtered = sampleDataset.filter(row -> (Integer) row.get(2) > 30);
        assertEquals(1, filtered.size());
        assertEquals("Charlie", filtered.getRow(0, Map.class).get("Name"));
    }

    @Test
    public void testComplexFiltering() {
        Dataset filtered = dataset.filter(Arrays.asList("age", "salary"), row -> (Integer) row.get(0) > 25 && (Double) row.get(1) > 55000);
        assertEquals(3, filtered.size());
        assertEquals("Jane", filtered.get(0, 1));
        assertEquals("Bob", filtered.get(1, 1));
        assertEquals("Charlie", filtered.get(2, 1));
    }

    @Test
    public void testFilterByColumn() {
        Dataset filtered = dataset.filter("age", (Integer age) -> age > 28);
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilterByBiPredicate() {
        Dataset filtered = dataset.filter(Tuple.of("age", "salary"), (age, salary) -> (int) age > 30 && (double) salary > 60000);

        assertEquals(2, filtered.size());
    }

    @Test
    public void testComplexFilterScenarios() {
        Dataset filtered = dataset.filter(1, 4, row -> (int) row.get(2) > 25);
        assertTrue(filtered.size() <= 3);

        Dataset colFiltered = dataset.filter(Arrays.asList("name", "age"), row -> row.get(0).toString().length() > 3 && (int) row.get(1) > 25);
        assertTrue(colFiltered.size() < dataset.size());
    }

    @Test
    public void testFilter_TwoColumns() {
        Dataset filtered = dataset.filter(Tuple.of("name", "age"), (String name, Integer age) -> age > 26);
        assertNotNull(filtered);
        assertEquals(4, filtered.size());
    }

    @Test
    public void testFilter_ThreeColumns() {
        Dataset filtered = dataset.filter(Tuple.of("id", "name", "age"), (Integer id, String name, Integer age) -> age > 26);
        assertNotNull(filtered);
        assertEquals(4, filtered.size());
    }

    @Test
    public void filter_byPredicateOnSingleColumn() {
        Dataset filtered = sampleDataset.filter("Name", (String name) -> name.startsWith("A"));
        assertEquals(1, filtered.size());
        assertEquals("Alice", filtered.getRow(0, Map.class).get("Name"));
    }

    @Test
    public void testFilter() {
        Dataset filtered = testDataset.filter((DisposableObjArray row) -> (Integer) row.get(2) >= 30);

        assertNotNull(filtered);
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterWithMax() {
        Dataset filtered = testDataset.filter((DisposableObjArray row) -> (Integer) row.get(2) >= 25, 2);

        assertNotNull(filtered);
        assertTrue(filtered.size() <= 2);
    }

    @Test
    public void testFilterWithRange() {
        Dataset filtered = testDataset.filter(1, 3, (DisposableObjArray row) -> (Integer) row.get(2) >= 25);

        assertNotNull(filtered);
        assertTrue(filtered.size() >= 0);
    }

    @Test
    public void testFilterWithRangeAndMax() {
        Dataset filtered = testDataset.filter(0, 4, (DisposableObjArray row) -> (Integer) row.get(2) >= 25, 1);

        assertNotNull(filtered);
        assertTrue(filtered.size() <= 1);
    }

    @Test
    public void testFilterWithBiPredicate() {
        Tuple2<String, String> columns = Tuple.of("age", "salary");
        BiPredicate<Integer, Double> predicate = (age, salary) -> age >= 30 && salary >= 50000;
        Dataset filtered = testDataset.filter(columns, predicate);

        assertNotNull(filtered);
        assertTrue(filtered.size() >= 0);
    }

    @Test
    public void testFilterWithTriPredicate() {
        Tuple3<String, String, String> columns = Tuple.of("id", "age", "salary");
        TriPredicate<Integer, Integer, Double> predicate = (id, age, salary) -> id > 1 && age >= 30 && salary >= 50000;
        Dataset filtered = testDataset.filter(columns, predicate);

        assertNotNull(filtered);
        assertTrue(filtered.size() >= 0);
    }

    @Test
    public void testFilterByColumnWithMax() {
        Dataset filtered = testDataset.filter("age", (Integer age) -> age >= 25, 2);

        assertNotNull(filtered);
        assertTrue(filtered.size() <= 2);
    }

    @Test
    public void testFilterByColumnsWithSelection() {
        Dataset filtered = testDataset.filter(Arrays.asList("name", "age"), (DisposableObjArray row) -> ((String) row.get(0)).startsWith("A"));

        assertNotNull(filtered);
        assertEquals(1, filtered.size());
    }

    @Test
    public void map_singleColumnToNew() {
        Dataset mapped = sampleDataset.mapColumn("Age", "AgeInMonths", "ID", (Integer age) -> age * 12);
        assertEquals(2, mapped.columnCount());
        assertTrue(mapped.columnNames().contains("AgeInMonths"));
        assertTrue(mapped.columnNames().contains("ID"));
        assertEquals(30 * 12, mapped.getRow(0, Map.class).get("AgeInMonths"));
    }

    @Test
    public void testMap() {
        Dataset mapped = testDataset.mapColumn("name", "upper_name", "age", (String name) -> name.toUpperCase());

        assertNotNull(mapped);
        assertTrue(mapped.containsColumn("upper_name"));
        assertTrue(mapped.containsColumn("age"));
        assertFalse(mapped.containsColumn("name"));
        assertFalse(mapped.containsColumn("id"));
        assertFalse(mapped.containsColumn("salary"));
    }

    @Test
    public void testMapWithMultipleCopyColumns() {
        Dataset mapped = testDataset.mapColumn("name", "upper_name", Arrays.asList("age", "salary"), (String name) -> name.toUpperCase());

        assertNotNull(mapped);
        assertTrue(mapped.containsColumn("upper_name"));
        assertTrue(mapped.containsColumn("age"));
        assertTrue(mapped.containsColumn("salary"));
        assertFalse(mapped.containsColumn("name"));
    }

    @Test
    public void testMapWithBiFunction() {
        Tuple2<String, String> fromColumns = Tuple.of("name", "age");
        Dataset mapped = testDataset.mapColumns(fromColumns, "name_age", Arrays.asList("id"), (String name, Integer age) -> name + "_" + age);

        assertNotNull(mapped);
        assertTrue(mapped.containsColumn("name_age"));
        assertTrue(mapped.containsColumn("id"));
    }

    @Test
    public void testMapWithTriFunction() {
        Tuple3<String, String, String> fromColumns = Tuple.of("id", "name", "age");
        Dataset mapped = testDataset.mapColumns(fromColumns, "combined", Arrays.asList("salary"),
                (Integer id, String name, Integer age) -> id + ":" + name + ":" + age);

        assertNotNull(mapped);
        assertTrue(mapped.containsColumn("combined"));
        assertTrue(mapped.containsColumn("salary"));
    }

    @Test
    public void testMapWithDisposableArray() {
        Dataset mapped = testDataset.mapColumns(Arrays.asList("name", "age"), "name_age", Arrays.asList("id", "salary"),
                (DisposableObjArray row) -> row.get(0) + "_" + row.get(1));

        assertNotNull(mapped);
        assertTrue(mapped.containsColumn("name_age"));
        assertTrue(mapped.containsColumn("id"));
        assertTrue(mapped.containsColumn("salary"));
    }

    @Test
    public void testFlatMap() {
        Dataset mapped = testDataset.flatMapColumn("name", "name_chars", "id", (String name) -> Arrays.asList(name.split("")));

        assertNotNull(mapped);
        assertTrue(mapped.containsColumn("name_chars"));
        assertTrue(mapped.containsColumn("id"));
        assertTrue(mapped.size() > testDataset.size());
    }

    @Test
    public void testFlatMapWithBiFunction() {
        Tuple2<String, String> fromColumns = Tuple.of("name", "age");
        Dataset mapped = testDataset.flatMapColumns(fromColumns, "name_parts", Arrays.asList("id"),
                (String name, Integer age) -> Arrays.asList(name, age.toString()));

        assertNotNull(mapped);
        assertTrue(mapped.containsColumn("name_parts"));
        assertTrue(mapped.size() > testDataset.size());
    }

    @Test
    public void testInnerJoin() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "dept"), new Object[][] { { 1, "IT" }, { 2, "HR" } });

        Dataset joined = ds1.innerJoin(ds2, CommonUtil.asMap("id", "id"));
        assertNotNull(joined);
        assertEquals(2, joined.size());
    }

    @Test
    public void testInnerJoin_MultipleKeys() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "year", "name"), new Object[][] { { 1, 2023, "Alice" }, { 2, 2023, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "year", "dept"), new Object[][] { { 1, 2023, "IT" }, { 2, 2023, "HR" } });

        Dataset joined = ds1.innerJoin(ds2, CommonUtil.asMap("id", "id", "year", "year"));
        assertNotNull(joined);
        assertEquals(2, joined.size());
    }

    @Test
    public void innerJoin_singleColumn() {
        List<String> orderCN = Arrays.asList("OrderID", "ID", "Product");
        List<List<Object>> orderCV = Arrays.asList(new ArrayList<>(Arrays.asList(101, 102, 103, 104)), new ArrayList<>(Arrays.asList(1, 1, 2, 4)),
                new ArrayList<>(Arrays.asList("Book", "Pen", "Paper", "Clip")));
        RowDataset orders = new RowDataset(orderCN, orderCV);

        Dataset joined = sampleDataset.innerJoin(orders, "ID", "ID");
        assertEquals(3, joined.size());
        assertEquals(sampleDataset.columnCount() + orders.columnCount(), joined.columnCount());
        assertTrue(joined.columnNames().contains("ID_2"));

        joined.moveToRow(0);
        assertEquals((Integer) 1, joined.get("ID"));
        assertEquals("Alice", joined.get("Name"));
        assertEquals((Integer) 101, joined.get("OrderID"));
        assertEquals("Book", joined.get("Product"));
    }

    @Test
    public void testInnerJoinWithMap() {
        Dataset right = Dataset.rows(Arrays.asList("emp_id", "department"), new Object[][] { { 1, "Engineering" }, { 2, "Marketing" } });

        Map<String, String> joinColumns = new HashMap<>();
        joinColumns.put("id", "emp_id");

        Dataset joined = testDataset.innerJoin(right, joinColumns);

        assertNotNull(joined);
        assertEquals(2, joined.size());
        assertTrue(joined.containsColumn("department"));
    }

    @Test
    public void testInnerJoinWithCollectingColumn() {
        Dataset right = Dataset.rows(Arrays.asList("id", "skill"), new Object[][] { { 1, "Java" }, { 1, "Python" }, { 2, "JavaScript" } });

        Map<String, String> joinColumns = new HashMap<>();
        joinColumns.put("id", "id");

        Dataset joined = testDataset.innerJoin(right, joinColumns, "skills", List.class);

        assertNotNull(joined);
        assertTrue(joined.containsColumn("skills"));
    }

    @Test
    public void testInnerJoinWithMultipleKeys() {
        List<String> otherColumns = Arrays.asList("name", "age", "dept");
        List<List<Object>> otherData = new ArrayList<>();
        otherData.add(Arrays.asList("John", "Bob"));
        otherData.add(Arrays.asList(25, 35));
        otherData.add(Arrays.asList("IT", "HR"));

        Dataset other = new RowDataset(otherColumns, otherData);

        Map<String, String> joinKeys = new HashMap<>();
        joinKeys.put("name", "name");
        joinKeys.put("age", "age");

        Dataset joined = dataset.innerJoin(other, joinKeys);

        assertEquals(2, joined.size());
    }

    @Test
    public void testLeftJoin() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 3, "Charlie" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "dept"), new Object[][] { { 1, "IT" }, { 2, "HR" } });

        Dataset joined = ds1.leftJoin(ds2, CommonUtil.asMap("id", "id"));
        assertNotNull(joined);
        assertEquals(3, joined.size());
    }

    @Test
    public void leftJoin_singleColumn() {
        List<String> orderCN = Arrays.asList("OrderID", "ID", "Product");
        List<List<Object>> orderCV = Arrays.asList(new ArrayList<>(Arrays.asList(101, 102)), new ArrayList<>(Arrays.asList(1, 1)),
                new ArrayList<>(Arrays.asList("Book", "Pen")));
        RowDataset orders = new RowDataset(orderCN, orderCV);

        Dataset joined = sampleDataset.leftJoin(orders, "ID", "ID");
        assertEquals(4, joined.size());

        joined.moveToRow(2);
        assertEquals((Integer) 2, joined.get("ID"));
        assertNull(joined.get("OrderID"));
    }

    @Test
    public void testComplexJoinScenarios() {
        List<String> otherColumns = Arrays.asList("id", "skill");
        List<List<Object>> otherData = new ArrayList<>();
        otherData.add(Arrays.asList(1, 1, 2, 2));
        otherData.add(Arrays.asList("Java", "Python", "C++", "Java"));

        Dataset skills = new RowDataset(otherColumns, otherData);

        Dataset joined = dataset.leftJoin(skills, Collections.singletonMap("id", "id"), "skills", List.class, size -> new ArrayList<>(size));

        assertTrue(joined.containsColumn("skills"));
        Object skillsList = joined.get(0, joined.getColumnIndex("skills"));
        assertTrue(skillsList instanceof Collection);
    }

    @Test
    public void test_leftJoin() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 7);
        for (int i = 0; i < accountList.size(); i++) {
            accountList.get(i).setFirstName("firstName" + i);
        }

        final List<Account> accountList2 = createAccountList(Account.class, 9);
        for (int i = 0; i < accountList2.size(); i++) {
            accountList2.get(i).setFirstName("firstName" + i);
        }

        final Dataset ds = CommonUtil.newDataset(accountList);

        final Dataset ds2 = CommonUtil.newDataset(accountList2);
        ds2.removeColumn("gui");
        final Map<String, String> oldNewNames = new HashMap<>();
        for (final String columnName : ds2.columnNames()) {
            oldNewNames.put(columnName, "right" + Strings.capitalize(columnName));
        }

        ds2.renameColumns(oldNewNames);

        Dataset joinedDataset = ds.leftJoin(ds2, "firstName", "rightFirstName");

        {

            try {
                ds.leftJoin(ds2, "Account.firstName11", "Account.firstName");
                fail("SHould throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }

            try {
                ds.leftJoin(ds2, "Account.firstName", "Account.firstName11");
                fail("SHould throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
        }

        Map<String, String> onColumnNames = CommonUtil.asMap("firstName", "rightFirstName");
        joinedDataset = ds.leftJoin(ds2, onColumnNames);

        ds2.renameColumn("rightFirstName", "firstName");

        joinedDataset = ds.leftJoin(ds2, "firstName", "firstName");

        ds2.renameColumn("firstName", "rightFirstName");

        joinedDataset = ds.leftJoin(ds2, "firstName", "rightFirstName");

        onColumnNames = CommonUtil.asMap("firstName", "rightFirstName", "lastName", "rightLastName");

        joinedDataset = ds.leftJoin(ds2, onColumnNames);

        joinedDataset = ds.leftJoin(ds2, onColumnNames, "account", Account.class);

        try {
            joinedDataset = ds.leftJoin(ds2, (Map<String, String>) null, "account", Account.class);
            fail("SHould throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {

        }

        try {
            joinedDataset = ds.leftJoin(ds2, onColumnNames, "firstName", Account.class);
            fail("SHould throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {

        }

        joinedDataset = ds.leftJoin(ds2, onColumnNames, "account", Account.class, IntFunctions.ofList());

        joinedDataset = ds.leftJoin(ds2, onColumnNames, "account", Account.class, IntFunctions.ofSet());

        try {
            joinedDataset = ds.leftJoin(ds2, (Map<String, String>) null, "account", Account.class, IntFunctions.ofList());
            fail("SHould throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {

        }

        try {
            joinedDataset = ds.leftJoin(ds2, onColumnNames, "firstName", Account.class, IntFunctions.ofList());
            fail("SHould throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {

        }
    }

    @Test
    public void testRightJoin() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "dept"), new Object[][] { { 1, "IT" }, { 2, "HR" }, { 3, "Finance" } });

        Dataset joined = ds1.rightJoin(ds2, CommonUtil.asMap("id", "id"));
        assertNotNull(joined);
        assertEquals(3, joined.size());
    }

    @Test
    public void testFullJoin() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "dept"), new Object[][] { { 2, "HR" }, { 3, "Finance" } });

        Dataset joined = ds1.fullJoin(ds2, CommonUtil.asMap("id", "id"));
        assertNotNull(joined);
        assertEquals(3, joined.size());
    }

    @Test
    public void testUnion() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 3, "Charlie" }, { 4, "Diana" } });

        Dataset union = ds1.union(ds2);
        assertNotNull(union);
        assertEquals(4, union.size());
    }

    @Test
    public void union_datasets() {
        List<String> otherNames = Arrays.asList("ID", "Name", "Salary");
        List<List<Object>> otherValues = Arrays.asList(new ArrayList<>(Arrays.asList(3, 4)), new ArrayList<>(Arrays.asList("Charlie", "David")),
                new ArrayList<>(Arrays.asList(70000, 80000)));
        RowDataset otherDs = new RowDataset(otherNames, otherValues);

        assertThrows(IllegalArgumentException.class, () -> sampleDataset.union(otherDs));
        Dataset unionResult = sampleDataset.unionBy(otherDs, List.of("ID", "Name"));
        assertEquals(4, unionResult.columnCount());
        assertEquals(4, unionResult.size());

        Map<Integer, Map<String, Object>> resultMap = new HashMap<>();
        for (int i = 0; i < unionResult.size(); i++) {
            resultMap.put((Integer) unionResult.moveToRow(i).get("ID"), unionResult.getRow(i, Map.class));
        }

        assertEquals("Alice", resultMap.get(1).get("Name"));
        assertEquals(30, resultMap.get(1).get("Age"));
        assertNull(resultMap.get(1).get("Salary"));

        assertEquals("Charlie", resultMap.get(3).get("Name"));
        assertEquals(35, resultMap.get(3).get("Age"));
        assertNull(resultMap.get(3).get("Salary"));

        assertEquals("David", resultMap.get(4).get("Name"));
        assertNull(resultMap.get(4).get("Age"));
        assertEquals(80000, resultMap.get(4).get("Salary"));
    }

    @Test
    public void testUnionWithRequiredSameColumns() {
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 5, "Eve", 32, 58000.0 } });

        Dataset union = testDataset.union(other, true);

        assertNotNull(union);
        assertEquals(5, union.size());
    }

    @Test
    public void testUnionWithKeyColumns() {
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 5, "Eve", 32, 58000.0 }, { 1, "Alice_Modified", 31, 51000.0 } });

        Dataset union = testDataset.unionBy(other, Arrays.asList("id"));

        assertNotNull(union);
        assertEquals(5, union.size());
    }

    @Test
    public void testUnion_2() {
        Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
                new Object[][] { { 1, "Alice", 25 }, new Object[] { 2, "Bob", 30 }, new Object[] { 1, "Alice", 35 } });
        Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"), new Object[][] { { 1, "Alice", 95 }, { 3, "Charlie", 85 } });

        assertThrows(IllegalArgumentException.class, () -> dataset1.union(dataset2));
        Dataset result = dataset1.unionBy(dataset2, List.of("id", "name"));

        assertEquals(4, result.columnCount());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("score"));
        assertEquals(3, result.size());
        assertEquals(1, (Integer) result.get(0, 0));
        assertEquals("Alice", result.get(0, 1));
        assertEquals(25, (Integer) result.get(0, 2));
        assertNull(result.get(0, 3));
        assertEquals(2, (Integer) result.get(1, 0));
        assertEquals("Bob", result.get(1, 1));
        assertEquals(30, (Integer) result.get(1, 2));
        assertNull(result.get(1, 3));
        assertEquals(3, (Integer) result.get(2, 0));
        assertEquals("Charlie", result.get(2, 1));
        assertNull(result.get(2, 2));
        assertEquals(85, (Integer) result.get(2, 3));
    }

    @Test
    public void test_union_all() {
        assertDoesNotThrow(() -> {
            Dataset ds1 = CommonUtil.newDataset(CommonUtil.toList("id", "name", "city"),
                    CommonUtil.toList(CommonUtil.toList(1, "n1", "c1"), CommonUtil.toList(2, "n2", "c2"), CommonUtil.toList(3, "n3", "c3")));
            Dataset ds2 = CommonUtil.newDataset(CommonUtil.toList("id", "address2", "state"),
                    CommonUtil.toList(CommonUtil.toList(1, "n1", "c1"), CommonUtil.toList(2, "n2", "c2"), CommonUtil.toList(2, "n22", "c22")));

            assertNotNull(ds1.unionBy(ds2, N.intersection(ds1.columnNames(), ds2.columnNames())));
            assertNotNull(ds1.unionAll(ds2, false));
        });
    }

    @Test
    public void test_union() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList);

        Dataset ds2 = ds.copy();

        Dataset ds3 = ds.union(ds2);

        assertEquals(ds.size(), ds3.size());

        ds3 = ds.unionAll(ds2);

        assertEquals(ds.size() * 2, ds3.size());

        ds2 = CommonUtil.newDataset(createAccountList(Account.class, 9));

        ds3 = ds.union(ds2);

        assertEquals(ds.size() * 2, ds3.size());

        ds3 = ds.unionAll(ds2);

        assertEquals(ds.size() * 2, ds3.size());
    }

    @Test
    public void test_union_2() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList);

        Dataset ds2 = ds.copy();

        ds.removeColumn(ds.getColumnName(2));

        Dataset ds3 = ds.unionBy(ds2, N.intersection(ds.columnNames(), ds2.columnNames()));

        assertEquals(ds.size(), ds3.size());

        ds3 = ds.unionAll(ds2, false);

        assertEquals(ds.size() * 2, ds3.size());

        ds2 = CommonUtil.newDataset(createAccountList(Account.class, 9));

        ds3 = ds.unionBy(ds2, N.intersection(ds.columnNames(), ds2.columnNames()));

        assertEquals(ds.size() * 2, ds3.size());

        ds3 = ds.unionAll(ds2, false);

        assertEquals(ds.size() * 2, ds3.size());
    }

    @Test
    public void test_union_3() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList);

        Dataset ds2 = ds.copy();

        ds2.removeColumn(ds2.getColumnName(2));

        Dataset ds3 = ds.unionBy(ds2, N.intersection(ds.columnNames(), ds2.columnNames()));

        assertEquals(ds.size(), ds3.size());

        ds3 = ds.unionAll(ds2, false);

        assertEquals(ds.size() * 2, ds3.size());

        ds2 = CommonUtil.newDataset(createAccountList(Account.class, 9));

        ds2.removeColumn(ds2.getColumnName(2));

        ds3 = ds.unionBy(ds2, N.intersection(ds.columnNames(), ds2.columnNames()));

        assertEquals(ds.size() * 2, ds3.size());

        ds3 = ds.unionAll(ds2, false);

        assertEquals(ds.size() * 2, ds3.size());
    }

    @Test
    public void testUnionAll() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 3, "Charlie" } });

        Dataset union = ds1.unionAll(ds2);
        assertNotNull(union);
        assertEquals(4, union.size());
    }

    @Test
    public void testUnionAll_2() {
        Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
                new Object[][] { { 1, "Alice", 25 }, new Object[] { 2, "Bob", 30 }, new Object[] { 1, "Alice", 35 } });
        Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"), new Object[][] { { 1, "Alice", 95 }, { 3, "Charlie", 85 } });

        assertThrows(IllegalArgumentException.class, () -> dataset1.unionAll(dataset2));
        Dataset result = dataset1.unionAll(dataset2, false);

        assertEquals(4, result.columnCount());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("score"));
        assertEquals(5, result.size());
        assertEquals(1, (Integer) result.get(0, 0));
        assertEquals("Alice", result.get(0, 1));
        assertEquals(25, (Integer) result.get(0, 2));
        assertNull(result.get(0, 3));
        assertEquals(2, (Integer) result.get(1, 0));
        assertEquals("Bob", result.get(1, 1));
        assertEquals(30, (Integer) result.get(1, 2));
        assertNull(result.get(1, 3));
        assertEquals(1, (Integer) result.get(2, 0));
        assertEquals("Alice", result.get(2, 1));
        assertEquals(35, (Integer) result.get(2, 2));
        assertNull(result.get(2, 3));
        assertEquals(1, (Integer) result.get(3, 0));
        assertEquals("Alice", result.get(3, 1));
        assertNull(result.get(3, 2));
        assertEquals(95, (Integer) result.get(3, 3));
        assertEquals(3, (Integer) result.get(4, 0));
        assertEquals("Charlie", result.get(4, 1));
        assertNull(result.get(4, 2));
        assertEquals(85, (Integer) result.get(4, 3));

        assertThrows(IllegalArgumentException.class, () -> dataset1.unionAll(dataset2, true));
    }

    @Test
    public void intersect_datasets() {
        List<String> otherNames = Arrays.asList("ID", "Name", "Age");
        List<List<Object>> otherValues = Arrays.asList(new ArrayList<>(Arrays.asList(2, 3, 4)), new ArrayList<>(Arrays.asList("Bob", "Charlie", "David")),
                new ArrayList<>(Arrays.asList(24, 35, 28)));
        RowDataset otherDs = new RowDataset(otherNames, otherValues);

        Dataset intersectResult = sampleDataset.intersect(otherDs);
        assertEquals(2, intersectResult.size());
        List<String> names = intersectResult.toList(Map.class).stream().map(m -> (String) m.get("Name")).sorted().toList();
        assertEquals(Arrays.asList("Bob", "Charlie"), names);
    }

    @Test
    public void testIntersect() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 3, "Charlie" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" }, { 4, "Diana" } });

        Dataset intersect = ds1.intersect(ds2);
        assertNotNull(intersect);
        assertEquals(2, intersect.size());
    }

    @Test
    public void testIntersectWithKeyColumns() {
        Dataset other = Dataset.rows(columnNames,
                new Object[][] { { 1, "Alice_Modified", 31, 51000.0 }, { 2, "Bob", 25, 45000.0 }, { 5, "Eve", 32, 58000.0 } });

        Dataset intersect = testDataset.intersectBy(other, Arrays.asList("id"));

        assertNotNull(intersect);
        assertEquals(2, intersect.size());
    }

    @Test
    public void testIntersectAll_2() {
        {
            Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 3, "Charlie" }, { 2, "Bob" } });

            Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"),
                    new Object[][] { { 2, "Bob" }, { 3, "Charlie" }, { 4, "Dave" }, { 2, "Bob" }, { 2, "Bob" } });

            Dataset result = ds1.intersectAll(ds2);

            assertEquals(3, result.size());
            assertEquals((Integer) 2, result.get(0, 0));
            assertEquals("Bob", result.get(0, 1));
            assertEquals((Integer) 3, result.get(1, 0));
            assertEquals("Charlie", result.get(1, 1));
            assertEquals((Integer) 2, result.get(2, 0));
            assertEquals("Bob", result.get(2, 1));
        }

        {
            Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 3, "Charlie" }, { 2, "Bob" } });

            Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" }, { 4, "Dave" } });

            Dataset result = ds1.intersectAll(ds2);

            assertEquals(2, result.size());
            assertEquals((Integer) 2, result.get(0, 0));
            assertEquals("Bob", result.get(0, 1));
            assertEquals((Integer) 3, result.get(1, 0));
            assertEquals("Charlie", result.get(1, 1));
        }

    }

    @Test
    public void testIntersectAll() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 2, "Bob" }, { 3, "Charlie" } });

        Dataset intersect = ds1.intersectAll(ds2);
        assertNotNull(intersect);
    }

    @Test
    public void except_datasets() {
        List<String> otherNames = Arrays.asList("ID", "Name", "Age");
        List<List<Object>> otherValues = Arrays.asList(new ArrayList<>(Arrays.asList(2, 3)), new ArrayList<>(Arrays.asList("Bob", "Charlie")),
                new ArrayList<>(Arrays.asList(24, 35)));
        RowDataset otherDs = new RowDataset(otherNames, otherValues);

        Dataset exceptResult = sampleDataset.except(otherDs);
        assertEquals(1, exceptResult.size());
        assertEquals("Alice", exceptResult.moveToRow(0).get("Name"));
    }

    @Test
    public void testExcept() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 3, "Charlie" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 4, "Diana" } });

        Dataset except = ds1.except(ds2);
        assertNotNull(except);
        assertEquals(2, except.size());
    }

    @Test
    public void testExceptAll() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" } });

        Dataset except = ds1.exceptAll(ds2);
        assertNotNull(except);
    }

    @Test
    public void testCartesianProduct() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("color", "size"), new Object[][] { { "Red", "S" }, { "Blue", "M" } });

        Dataset product = ds1.cartesianProduct(ds2);

        assertNotNull(product);
        assertEquals(4, product.size());
        assertEquals(4, product.columnCount());

        assertEquals(Integer.valueOf(1), product.get(0, 0));
        assertEquals("Alice", product.get(0, 1));
        assertEquals("Red", product.get(0, 2));
        assertEquals("S", product.get(0, 3));

        assertEquals(Integer.valueOf(2), product.get(3, 0));
        assertEquals("Bob", product.get(3, 1));
        assertEquals("Blue", product.get(3, 2));
        assertEquals("M", product.get(3, 3));
    }

    @Test
    public void split_intoChunks() {
        Stream<Dataset> stream = sampleDataset.split(2);
        List<Dataset> chunks = stream.toList();
        assertEquals(2, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(1, chunks.get(1).size());
        assertEquals("Alice", chunks.get(0).moveToRow(0).get("Name"));
        assertEquals("Charlie", chunks.get(1).moveToRow(0).get("Name"));
    }

    @Test
    public void testSplit_BySize() {
        List<Dataset> splits = dataset.split(2).toList();
        assertNotNull(splits);
        assertEquals(3, splits.size());
        assertEquals(2, splits.get(0).size());
        assertEquals(2, splits.get(1).size());
        assertEquals(1, splits.get(2).size());
    }

    @Test
    public void testSplit() {
        Stream<Dataset> chunks = testDataset.split(2);

        assertNotNull(chunks);
        List<Dataset> chunkList = chunks.toList();
        assertEquals(2, chunkList.size());
        assertEquals(2, chunkList.get(0).size());
        assertEquals(2, chunkList.get(1).size());
    }

    @Test
    public void testSplitWithColumns() {
        Stream<Dataset> chunks = testDataset.split(3, Arrays.asList("name", "age"));

        assertNotNull(chunks);
        List<Dataset> chunkList = chunks.toList();
        assertTrue(chunkList.size() >= 1);
        assertTrue(chunkList.get(0).containsColumn("name"));
        assertTrue(chunkList.get(0).containsColumn("age"));
        assertFalse(chunkList.get(0).containsColumn("id"));
    }

    @Test
    public void testInvalidSplitOperations() {
        assertThrows(IllegalArgumentException.class, () -> {
            testDataset.split(0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            testDataset.split(-1);
        });
    }

    @Test
    public void testSplitToList() {
        List<Dataset> splits = dataset.splitToList(2);

        assertNotNull(splits);
        assertEquals(3, splits.size());
        assertEquals(2, splits.get(0).size());
        assertEquals(2, splits.get(1).size());
        assertEquals(1, splits.get(2).size());
        assertEquals(4, splits.get(0).columnCount());
    }

    @Test
    public void testSplitToList_WithColumns() {
        List<Dataset> splits = dataset.splitToList(2, Arrays.asList("id", "name"));

        assertNotNull(splits);
        assertEquals(3, splits.size());
        assertEquals(2, splits.get(0).size());
        assertEquals(2, splits.get(0).columnCount());
        assertTrue(splits.get(0).containsColumn("id"));
        assertTrue(splits.get(0).containsColumn("name"));
        assertFalse(splits.get(0).containsColumn("age"));
    }

    @Test
    public void slice_dataset() {
        Dataset sliced = sampleDataset.slice(1, 2, Arrays.asList("Name"));
        assertEquals(1, sliced.size());
        assertEquals(1, sliced.columnCount());
        assertEquals("Bob", sliced.get(0, 0));
        assertTrue(sliced.isFrozen());
    }

    @Test
    public void testSlice() {
        Dataset slice = dataset.slice(1, 3);
        assertNotNull(slice);
        assertEquals(2, slice.size());
        assertEquals(Integer.valueOf(2), slice.get(0, 0));
        assertEquals(Integer.valueOf(3), slice.get(1, 0));
    }

    @Test
    public void testSlice_WithColumnNames() {
        Dataset slice = dataset.slice(1, 3, Arrays.asList("id", "name"));
        assertNotNull(slice);
        assertEquals(2, slice.size());
        assertEquals(2, slice.columnCount());
    }

    @Test
    public void testSliceWithColumns() {
        Dataset slice = testDataset.slice(Arrays.asList("name", "age"));

        assertNotNull(slice);
        assertEquals(4, slice.size());
        assertEquals(2, slice.columnCount());
        assertTrue(slice.containsColumn("name"));
        assertTrue(slice.containsColumn("age"));
        assertFalse(slice.containsColumn("id"));
        assertFalse(slice.containsColumn("salary"));
    }

    @Test
    public void testSliceWithRangeAndColumns() {
        Dataset slice = testDataset.slice(1, 3, Arrays.asList("name", "age"));

        assertNotNull(slice);
        assertEquals(2, slice.size());
        assertEquals(2, slice.columnCount());
        slice.moveToRow(0);
        assertEquals("Bob", slice.get("name"));
    }

    @Test
    public void testInvalidSliceOperations() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            testDataset.slice(-1, 2);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            testDataset.slice(2, 1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            testDataset.slice(Arrays.asList("nonexistent"));
        });
    }

    @Test
    public void testTrim() {
        Dataset ds = dataset.copy();
        ds.removeRow(0);
        ds.removeRow(0);

        assertEquals(3, ds.size());
        ds.trimToSize();
        assertEquals(3, ds.size());
    }

    @Test
    public void copy_selectedColumns() {
        Dataset copied = sampleDataset.copy(Arrays.asList("Name", "Age"));
        assertEquals(2, copied.columnCount());
        assertTrue(copied.columnNames().contains("Name"));
        assertTrue(copied.columnNames().contains("Age"));
        assertEquals("Alice", copied.get(0, 0));
    }

    @Test
    public void copy_range() {
        Dataset copied = sampleDataset.copy(1, 3);
        assertEquals(2, copied.size());
        assertEquals("Bob", copied.moveToRow(0).get("Name"));
        assertEquals("Charlie", copied.moveToRow(1).get("Name"));
    }

    @Test
    public void testRenameAllColumnsWithFunction() {
        Dataset ds = dataset.copy();
        ds.renameColumns(name -> "col_" + name);
        assertEquals("col_id", ds.getColumnName(0));
        assertEquals("col_name", ds.getColumnName(1));
    }

    @Test
    public void testCopyWithRange() {
        Dataset copy = dataset.copy(1, 3);
        assertEquals(2, copy.size());
        assertEquals("Jane", copy.get(0, 1));
        assertEquals("Bob", copy.get(1, 1));
    }

    @Test
    public void testCopyWithColumns() {
        Dataset copy = dataset.copy(Arrays.asList("id", "name"));
        assertEquals(5, copy.size());
        assertEquals(2, copy.columnCount());
        assertEquals("John", copy.get(0, 1));
    }

    @Test
    public void testOperationsOnFrozenDataset() {
        Dataset ds = dataset.copy();
        ds.freeze();
        assertTrue(ds.isFrozen());
    }

    @Test
    public void testCopyWithColumnNames() {
        Dataset copy = dataset.copy(Arrays.asList("name", "age"));

        assertEquals(2, copy.columnCount());
        assertTrue(copy.containsColumn("name"));
        assertTrue(copy.containsColumn("age"));
        assertFalse(copy.containsColumn("id"));
    }

    @Test
    public void testCopy() {
        Dataset copy = dataset.copy();
        assertNotNull(copy);
        assertEquals(dataset.size(), copy.size());
        assertEquals(dataset.columnCount(), copy.columnCount());

        copy.set(0, 1, "Modified");
        assertEquals("Modified", copy.get(0, 1));
        assertEquals("John", dataset.get(0, 1));
    }

    @Test
    public void testProperties() {
        Dataset ds = dataset.copy();
        Map<String, Object> props = new java.util.HashMap<>();
        props.put("key1", "value1");
        ds.setProperties(props);

        Map<String, Object> retrievedProps = ds.getProperties();
        assertNotNull(retrievedProps);
        assertEquals("value1", retrievedProps.get("key1"));
    }

    @Test
    public void copy_full() {
        Dataset copied = sampleDataset.copy();
        assertNotSame(sampleDataset, copied);
        assertEquals(sampleDataset, copied);
        copied.set(0, 0, 100);
        assertEquals((Integer) 1, sampleDataset.moveToRow(0).get(0));
    }

    @Test
    public void testCopyWithRangeAndColumns() {
        Dataset copy = testDataset.copy(1, 3, Arrays.asList("name", "age"));

        assertNotNull(copy);
        assertEquals(2, copy.size());
        assertEquals(2, copy.columnCount());
        copy.moveToRow(0);
        assertEquals("Bob", copy.get("name"));
    }

    @Test
    public void test_interset_difference() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList);

        final Dataset ds2 = ds.copy();

        Dataset ds3 = N.intersection(ds, ds2);

        assertEquals(ds, ds3);

        ds3 = N.difference(ds, ds2);

        assertEquals(0, ds3.size());

        ds2.clear();

        ds3 = N.intersection(ds, ds2);

        assertEquals(0, ds3.size());

        ds3 = N.difference(ds, ds2);

        assertEquals(ds, ds3);

        ds.clear();

        ds3 = N.intersection(ds, ds2);

        assertEquals(0, ds3.size());

        ds3 = N.difference(ds, ds2);

        assertEquals(ds, ds3);
    }

    @Test
    public void test_interset_except_2() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList);

        final Dataset ds2 = ds.copy();

        ds.removeColumn(ds.getColumnName(2));

        Dataset ds3 = N.intersection(ds, ds2);

        assertEquals(ds, ds3);

        ds3 = N.difference(ds, ds2);

        assertEquals(0, ds3.size());

        ds2.clear();

        ds3 = N.intersection(ds, ds2);

        assertEquals(0, ds3.size());

        ds3 = N.difference(ds, ds2);

        assertEquals(ds, ds3);

        ds.clear();

        ds3 = N.intersection(ds, ds2);

        assertEquals(0, ds3.size());

        ds3 = N.difference(ds, ds2);

        assertEquals(ds, ds3);
    }

    @Test
    public void testFrozenDatasetOperations() {
        Dataset dataset = testDataset.copy();
        dataset.freeze();

        assertThrows(IllegalStateException.class, () -> {
            dataset.addRow(new Object[] { 5, "Eve", 32, 58000.0 });
        });

        assertThrows(IllegalStateException.class, () -> {
            dataset.removeRow(0);
        });

        assertThrows(IllegalStateException.class, () -> {
            dataset.clear();
        });

        assertThrows(IllegalStateException.class, () -> {
            dataset.addColumn("newCol", Arrays.asList(1, 2, 3, 4));
        });
    }

    @Test
    public void clone_method() {

        Dataset copy = sampleDataset.clone();
        assertFalse(copy == sampleDataset);
        assertEquals(sampleDataset, copy);

    }

    @Test
    public void testClone() {
        Dataset clone = dataset.clone();
        assertNotNull(clone);
        assertEquals(dataset.size(), clone.size());
        assertEquals(dataset.columnCount(), clone.columnCount());
    }

    @Test
    public void testClone_Frozen() {
        Dataset frozen = dataset.copy();
        frozen.freeze();
        Dataset clone = frozen.clone();
        assertNotNull(clone);
        assertTrue(clone.isFrozen());
    }

    @Test
    public void testCloneWithFreeze() {
        Dataset clone = dataset.clone(true);
        assertNotSame(dataset, clone);
        assertTrue(clone.isFrozen());
    }

    @Test
    public void test_clone() throws Exception {
        final Account account = createAccount(Account.class);
        final Dataset ds1 = CommonUtil.newDataset(CommonUtil.toList(account, account));

        assertEquals(ds1, ds1.copy());
        assertEquals(ds1, ds1.clone());
        assertEquals(ds1.copy(), ds1.clone());
        assertEquals(ds1.clone(), ds1.clone());

        assertFalse(ds1.copy().isFrozen());
        assertFalse(ds1.clone().isFrozen());
        assertFalse(ds1.clone(false).isFrozen());
        assertTrue(ds1.clone(true).isFrozen());

        ds1.freeze();

        assertFalse(ds1.copy().isFrozen());
        assertTrue(ds1.clone().isFrozen());
        assertFalse(ds1.clone(false).isFrozen());
        assertTrue(ds1.clone(true).isFrozen());
    }

    @Test
    public void iterator_Tuple2() {
        BiIterator<String, Integer> iter = sampleDataset.iterator("Name", "Age");
        assertTrue(iter.hasNext());
        Pair<String, Integer> p = iter.next();
        assertEquals("Alice", p.left());
        assertEquals(30, p.right());
        iter.next();
        iter.next();
        assertFalse(iter.hasNext());
    }

    @Test
    public void iterator_Tuple3() {
        TriIterator<Integer, String, Integer> iter = sampleDataset.iterator("ID", "Name", "Age");
        assertTrue(iter.hasNext());
        Triple<Integer, String, Integer> t = iter.next();
        assertEquals(1, t.left());
        assertEquals("Alice", t.middle());
        assertEquals(30, t.right());
    }

    @Test
    public void testBiIterator() {
        BiIterator<Object, Object> iter = dataset.iterator("id", "name");

        assertTrue(iter.hasNext());
        Pair<Object, Object> pair = iter.next();
        assertEquals(1, pair.left());
        assertEquals("John", pair.right());
    }

    @Test
    public void testTriIterator() {
        TriIterator<Object, Object, Object> iter = dataset.iterator("id", "name", "age");

        assertTrue(iter.hasNext());
        Triple<Object, Object, Object> triple = iter.next();
        assertEquals(1, triple.left());
        assertEquals("John", triple.middle());
        assertEquals(25, triple.right());
    }

    @Test
    public void testIterator_TwoColumns() {
        BiIterator<Integer, String> iter = dataset.iterator("id", "name");
        assertNotNull(iter);

        assertTrue(iter.hasNext());
        Pair<Integer, String> pair = iter.next();
        assertEquals(Integer.valueOf(1), pair.left());
        assertEquals("John", pair.right());
    }

    @Test
    public void testIterator_ThreeColumns() {
        TriIterator<Integer, String, Integer> iter = dataset.iterator("id", "name", "age");
        assertNotNull(iter);

        assertTrue(iter.hasNext());
        Triple<Integer, String, Integer> triple = iter.next();
        assertEquals(Integer.valueOf(1), triple.left());
        assertEquals("John", triple.middle());
        assertEquals(Integer.valueOf(25), triple.right());
    }

    @Test
    public void testPaginate() {
        Paginated<Dataset> pages = dataset.paginate(2);
        assertNotNull(pages);

        List<Dataset> pageList = pages.stream().toList();

        assertEquals(3, pageList.size());
        assertEquals(2, pageList.get(0).size());
        assertEquals(2, pageList.get(1).size());
        assertEquals(1, pageList.get(2).size());
    }

    @Test
    public void stream_asBean() {
        RowDataset beanDs = new RowDataset(Arrays.asList("id", "name", "value"), Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)),
                new ArrayList<>(Arrays.asList("obj1", "obj2")), new ArrayList<>(Arrays.asList(10.0, 20.0))));
        List<Integer> ids = beanDs.stream(TestBean.class).map(TestBean::getId).toList();
        assertEquals(Arrays.asList(1, 2), ids);
    }

    @Test
    public void testStream() {
        Stream<Object[]> stream = dataset.stream(Object[].class);
        List<Object[]> list = stream.toList();
        assertEquals(5, list.size());
        assertEquals("John", list.get(0)[1]);
    }

    @Test
    public void testStreamWithRange() {
        Stream<Object[]> stream = dataset.stream(1, 3, Object[].class);
        List<Object[]> list = stream.toList();
        assertEquals(2, list.size());
        assertEquals("Jane", list.get(0)[1]);
    }

    @Test
    public void testStreamWithSupplier() {
        Stream<List<Object>> stream = dataset.stream(ArrayList::new);
        List<List<Object>> list = stream.toList();
        assertEquals(5, list.size());
        assertEquals("John", list.get(0).get(1));
    }

    @Test
    public void testStreamWithClass() {
        List<Person> persons = dataset.stream(Person.class).toList();

        assertEquals(5, persons.size());
        assertEquals("John", persons.get(0).getName());
    }

    @Test
    public void testStreamWithBiFunctionMapper() {
        List<String> nameAges = dataset.stream(Tuple.of("name", "age"), (name, age) -> name + " is " + age + " years old").toList();

        assertEquals(5, nameAges.size());
        assertEquals("John is 25 years old", nameAges.get(0));
    }

    @Test
    public void testStreamWithTriFunctionMapper() {
        List<String> combined = dataset.stream(Tuple.of("id", "name", "age"), (id, name, age) -> "ID:" + id + " Name:" + name + " Age:" + age).toList();

        assertEquals(5, combined.size());
        assertEquals("ID:1 Name:John Age:25", combined.get(0));
    }

    @Test
    public void testConcurrentModification() {
        Stream<Object> stream = dataset.stream("id");

        Iterator<Object> iter = stream.iterator();
        assertTrue(iter.hasNext());
        iter.next();

        dataset.set(0, 0, 999);

        assertEquals(2, iter.next());
    }

    @Test
    public void testStream_AsEntity() {
        Stream<Person> stream = dataset.stream(Person.class);
        assertNotNull(stream);

        List<Person> persons = stream.toList();
        assertEquals(5, persons.size());
        assertEquals("John", persons.get(0).getName());
    }

    @Test
    public void stream_singleColumn() {
        List<String> names = sampleDataset.stream("Name").map(s -> ((String) s).toUpperCase()).toList();
        assertEquals(Arrays.asList("ALICE", "BOB", "CHARLIE"), names);
    }

    @Test
    public void testStreamWithRowMapper() {
        List<String> result = dataset.stream((rowIndex, row) -> rowIndex + ":" + row.get(1)).toList();

        assertEquals(Arrays.asList("0:John", "1:Jane", "2:Bob", "3:Alice", "4:Charlie"), result);
    }

    @Test
    public void testApply() {
        Integer result = dataset.apply(ds -> ds.size());

        assertEquals(5, result.intValue());
    }

    @Test
    public void testApply_WithTransformation() {
        List<String> names = dataset.apply(ds -> {
            List<String> result = new ArrayList<>();
            ds.forEach(row -> result.add((String) row.get(1)));
            return result;
        });

        assertEquals(5, names.size());
        assertEquals("John", names.get(0));
        assertEquals("Jane", names.get(1));
    }

    @Test
    public void apply_function() {
        Integer totalAge = sampleDataset.apply(ds -> {
            int sum = 0;
            for (int i = 0; i < ds.size(); i++) {
                sum += (Integer) ds.moveToRow(i).get("Age");
            }
            return sum;
        });
        assertEquals(30 + 24 + 35, totalAge.intValue());
    }

    @Test
    public void testApplyIfNotEmpty() {
        Optional<Integer> result = dataset.applyIfNotEmpty(ds -> ds.size());

        assertTrue(result.isPresent());
        assertEquals(5, result.get().intValue());
    }

    @Test
    public void testApplyIfNotEmpty_OnEmptyDataset() {
        Optional<Integer> result = emptyDataset.applyIfNotEmpty(ds -> ds.size());

        assertFalse(result.isPresent());
    }

    @Test
    public void testAccept() {
        List<Integer> ids = new ArrayList<>();

        dataset.accept(ds -> {
            ds.forEach(row -> ids.add((Integer) row.get(0)));
        });

        assertEquals(5, ids.size());
        assertEquals(Integer.valueOf(1), ids.get(0));
        assertEquals(Integer.valueOf(2), ids.get(1));
        assertEquals(Integer.valueOf(3), ids.get(2));
        assertEquals(Integer.valueOf(4), ids.get(3));
        assertEquals(Integer.valueOf(5), ids.get(4));
    }

    @Test
    public void accept_consumer() {
        AtomicInteger count = new AtomicInteger(0);
        sampleDataset.accept(ds -> {
            count.set(ds.size());
        });
        assertEquals(3, count.get());
    }

    @Test
    public void testAcceptIfNotEmpty() {
        List<Integer> ids = new ArrayList<>();

        dataset.acceptIfNotEmpty(ds -> {
            ds.forEach(row -> ids.add((Integer) row.get(0)));
        });

        assertEquals(5, ids.size());
    }

    @Test
    public void testAcceptIfNotEmpty_OnEmptyDataset() {
        List<Integer> ids = new ArrayList<>();

        emptyDataset.acceptIfNotEmpty(ds -> {
            ds.forEach(row -> ids.add((Integer) row.get(0)));
        });

        assertEquals(0, ids.size());
    }

    @Test
    public void testFreeze() {
        Dataset ds = dataset.copy();
        assertFalse(ds.isFrozen());

        ds.freeze();
        assertTrue(ds.isFrozen());

        assertThrows(IllegalStateException.class, () -> ds.set(0, 0, 999));
    }

    @Test
    public void freeze_and_isFrozen() {
        assertFalse(sampleDataset.isFrozen());
        sampleDataset.freeze();
        assertTrue(sampleDataset.isFrozen());
        assertThrows(IllegalStateException.class, () -> sampleDataset.set(0, 0, 100));
    }

    @Test
    public void testModifyFrozenDataset() {
        dataset.freeze();
        assertThrows(IllegalStateException.class, () -> dataset.set(0, 0, 999));
    }

    @Test
    public void testFrozen() {
        assertFalse(dataset.isFrozen());

        Dataset frozen = dataset.copy();
        frozen.freeze();
        assertTrue(frozen.isFrozen());
    }

    @Test
    public void testIsFrozen() {
        Dataset ds = dataset.copy();
        assertFalse(ds.isFrozen());

        ds.freeze();
        assertTrue(ds.isFrozen());
    }

    @Test
    public void testClear() {
        Dataset ds = dataset.copy();
        ds.clear();

        assertEquals(0, ds.size());
        assertEquals(4, ds.columnCount());
        assertTrue(ds.isEmpty());
    }

    @Test
    public void constructor_emptyLists() {
        RowDataset ds = new RowDataset(Collections.emptyList(), Collections.emptyList());
        assertNotNull(ds);
        assertTrue(ds.isEmpty());
        assertEquals(0, ds.columnCount());
    }

    @Test
    public void isEmpty_size_clear() {
        assertFalse(sampleDataset.isEmpty());
        assertEquals(3, sampleDataset.size());

        sampleDataset.clear();
        assertTrue(sampleDataset.isEmpty());
        assertEquals(0, sampleDataset.size());
        assertEquals(3, sampleDataset.columnCount());
        assertTrue(sampleDataset.getColumn(0).isEmpty());

        RowDataset frozenDs = createSimpleDataset();
        frozenDs.freeze();
        assertThrows(IllegalStateException.class, frozenDs::clear);
    }

    @Test
    public void testTrimToSize() {
        Dataset ds = dataset.copy();

        ds.trimToSize();

        assertEquals(5, ds.size());
        assertEquals(4, ds.columnCount());
        assertEquals("John", ds.get(0, 1));
    }

    @Test
    public void testEdgeCases() {
        Dataset empty = new RowDataset(Arrays.asList("col1"), Arrays.asList(new ArrayList<>()));
        assertEquals(0, empty.size());
        assertTrue(empty.isEmpty());

        Dataset single = new RowDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList("value")));
        assertEquals(1, single.size());
        assertEquals(1, single.columnCount());

        List<String> nullColumns = Arrays.asList("col1", "col2");
        List<List<Object>> nullData = new ArrayList<>();
        nullData.add(Arrays.asList(null, "A"));
        nullData.add(Arrays.asList("B", null));

        Dataset withNulls = new RowDataset(nullColumns, nullData);
        assertTrue(withNulls.isNull(0, 0));
        assertFalse(withNulls.isNull(0, 1));
    }

    @Test
    public void test_join() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 5);
        for (int i = 0; i < accountList.size(); i++) {
            accountList.get(i).setLastName("lastName" + i);
            accountList.get(i).setFirstName("firstName" + i);
        }

        accountList.add(accountList.get(1));
        accountList.add(accountList.get(3));

        final List<Account> accountList2 = createAccountList(Account.class, 5);
        for (int i = 0; i < accountList2.size(); i++) {
            accountList2.get(i).setLastName("lastName" + i);
            accountList2.get(i).setFirstName("firstName" + (i + 2));
        }

        accountList2.add(accountList2.get(1));
        accountList2.add(accountList2.get(3));

        final Dataset ds = CommonUtil.newDataset(accountList);

        Dataset ds2 = CommonUtil.newDataset(accountList2);
        ds2.removeColumn("gui");
        final Map<String, String> oldNewNames = new HashMap<>();
        for (final String columnName : ds2.columnNames()) {
            oldNewNames.put(columnName, "right" + Strings.capitalize(columnName));
        }

        ds2.renameColumns(oldNewNames);

        Dataset ds3 = ds.innerJoin(ds2, "firstName", "rightFirstName");
        assertEquals(6, ds3.size());

        ds3 = ds.leftJoin(ds2, "firstName", "rightFirstName");
        assertEquals(9, ds3.size());

        ds3 = ds.rightJoin(ds2, "firstName", "rightFirstName");
        assertEquals(9, ds3.size());

        ds3 = ds.fullJoin(ds2, "firstName", "rightFirstName");
        assertEquals(12, ds3.size());

        Map<String, String> onColumnNames = CommonUtil.asMap("firstName", "firstName");
        ds2 = CommonUtil.newDataset(accountList2);

        ds3 = ds.innerJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        assertEquals(6, ds3.size());

        ds3 = ds.innerJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        assertEquals(4, ds3.size());

        ds3 = ds.leftJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        assertEquals(9, ds3.size());

        ds3 = ds.leftJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        assertEquals(7, ds3.size());

        ds3 = ds.rightJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        assertEquals(9, ds3.size());

        ds3 = ds.rightJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        assertEquals(6, ds3.size());

        ds3 = ds.fullJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        assertEquals(12, ds3.size());

        ds3 = ds.fullJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        assertEquals(9, ds3.size());

        onColumnNames = CommonUtil.asMap("firstName", "firstName", "middleName", "middleName");
        ds2 = CommonUtil.newDataset(accountList2);

        ds3 = ds.innerJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        assertEquals(6, ds3.size());

        ds3 = ds.innerJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        assertEquals(4, ds3.size());

        ds3 = ds.leftJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        assertEquals(9, ds3.size());

        ds3 = ds.leftJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        assertEquals(7, ds3.size());

        ds3 = ds.rightJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        assertEquals(9, ds3.size());

        ds3 = ds.rightJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        assertEquals(6, ds3.size());

        ds3 = ds.fullJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        assertEquals(12, ds3.size());

        ds3 = ds.fullJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        assertEquals(9, ds3.size());
    }

    @Test
    public void test_asDataset() throws Exception {
        final List<String> columnNameList = new ArrayList<>(Beans.getPropNameList(Account.class));

        final List<Account> accountList = createAccountList(Account.class, 1000);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            final Dataset dataset = CommonUtil.newDataset(columnNameList, accountList);
            assertEquals(accountList.size(), dataset.size());
        }

        final Dataset dataset = CommonUtil.newDataset(columnNameList, accountList);

        startTime = System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            final List<?> list = dataset.toList(Map.class);

            assertEquals(accountList.size(), list.size());
        }

        startTime = System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            dataset.sortBy("gui");
        }

        final Map<String, Object> props = createAccountProps();
        final Dataset ds = CommonUtil.newDataset("propName", "propValue", props);
    }

    @Test
    public void properties_access() {
        assertTrue(sampleDataset.getProperties().isEmpty());
        Map<String, Object> props = new HashMap<>();
        props.put("version", 1.2);
        RowDataset dsWithProps = new RowDataset(Arrays.asList("ID", "Name", "Age"), columnValues, props);
        assertEquals(1.2, (Double) dsWithProps.getProperties().get("version"), 0.001);
        assertThrows(UnsupportedOperationException.class, () -> dsWithProps.getProperties().put("newKey", "newVal"));
    }

    @Test
    public void testSetProperties() {
        Dataset ds = dataset.copy();
        Map<String, Object> props = new HashMap<>();
        props.put("key1", "value1");
        props.put("key2", 123);

        ds.setProperties(props);

        Map<String, Object> retrieved = ds.getProperties();
        assertEquals("value1", retrieved.get("key1"));
        assertEquals(123, retrieved.get("key2"));
    }

    @Test
    public void test_json() {
        final List<Account> accountList = createAccountList(Account.class, 3);
        final Dataset ds = CommonUtil.newDataset(accountList);

        String json = N.toJson(ds, JsonSerConfig.create().setWriteDatasetAsRows(true));

        json = N.toJson(ds, JsonSerConfig.create().setWriteDatasetAsRows(true).setPrettyFormat(true));

        final Dataset ds2 = N.fromJson(json, Dataset.class);
        assertNotNull(ds2);
    }

    @Test
    public void testPrintlnWithWriter() {
        StringWriter writer = new StringWriter();
        testDataset.println(0, 2, Arrays.asList("name", "age"), writer);

        String output = writer.toString();
        assertNotNull(output);
        assertTrue(output.contains("Alice"));
        assertTrue(output.contains("Bob"));
        assertFalse(output.contains("Charlie"));
    }

    @Test
    public void testPrintlnWithPrefixAndWriter() {
        StringWriter writer = new StringWriter();
        testDataset.println(0, 2, Arrays.asList("name", "age"), "TEST: ", writer);

        String output = writer.toString();
        assertNotNull(output);
        assertTrue(output.contains("TEST: "));
        assertTrue(output.contains("Alice"));
    }

    @Test
    public void test_json_xml() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList, CommonUtil.asMap("prop1", 1, "key2", "val2"));

        assertNotNull(ds);
    }

    @Test
    public void test_json_2() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList, CommonUtil.asMap("prop1", 1, "key2", "val2"));

        final String json = ds.toJson();

        final Dataset ds2 = N.fromJson(json, JsonDeserConfig.create().setValueTypesByBeanClass(Account.class), Dataset.class);

        assertEquals(ds, ds2);

        final Map<Object, Object> map = CommonUtil.asMap("key", accountList);

        final Map<String, Dataset> map2 = N.fromJson(N.toJson(map), JsonDeserConfig.create().setValueTypesByBeanClass(Account.class),
                Type.ofMap(String.class, Dataset.class));
        map2.entrySet().iterator().next().getValue();
    }

    @Test
    public void test_lift() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 7);

        final Dataset ds = CommonUtil.newDataset(accountList);

        assertNotNull(ds);
    }

    @Test
    public void test_multiset() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 7);
        final Dataset ds = CommonUtil.newDataset(accountList);
        assertNotNull(ds);
    }

    @Test
    public void test_first_last_row() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList);

        ds.clear();

        assertFalse(ds.firstRow().isPresent());
        assertFalse(ds.lastRow().isPresent());
    }

    @Test
    public void test_combineColumn() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList);

        ds.combineColumns(CommonUtil.toList("firstName", "lastName"), "name", Object[].class);

        assertTrue(ds.getColumnIndex("name") >= 0);
        assertFalse(ds.containsColumn("firstName"));
        assertFalse(ds.containsColumn("lastName"));
    }

    @Test
    public void test_asDataset_3() throws Exception {
        final long startTime = System.currentTimeMillis();

        final List<Map<String, Object>> propsList = createAccountPropsList(1000);

        final Dataset ds = CommonUtil.newDataset(new ArrayList<>(propsList.get(0).keySet()), propsList);

        ds.groupBy(CommonUtil.toList("Account.firstName", "Account.lastName"));
        assertNotNull(ds);
    }

    @Test
    public void testPrintln() {
        assertDoesNotThrow(() -> {
            dataset.println();
        });
    }

    @Test
    public void testPrintln_WithRange() {
        assertDoesNotThrow(() -> {
            dataset.println(0, 2);
        });
    }

    @Test
    public void testPrintln_WithColumns() {
        assertDoesNotThrow(() -> {
            dataset.println(0, 2, Arrays.asList("id", "name"));
        });
    }

    @Test
    public void println_toWriter() throws IOException {
        StringWriter stringWriter = new StringWriter();
        sampleDataset.println(stringWriter);
        String output = stringWriter.toString();

        assertTrue(output.contains("+----+---------+-----+"));
        assertTrue(output.contains("| ID | Name    | Age |"));
        assertTrue(output.contains("| 1  | Alice   | 30  |"));
        assertTrue(output.contains("| 2  | Bob     | 24  |"));
        assertTrue(output.contains("| 3  | Charlie | 35  |"));
    }

    @Test
    public void testPrintln_01() {
        assertDoesNotThrow(() -> {
            {
                Dataset.empty().println();

                Dataset.empty().println("# ");

                Dataset.empty().println("// ");
            }
            {
                dataset.set(0, 0, "A very long text that exceeds the usual width零零忑零零忑零零忑 to test wrapping functionality in the dataset printing method.零零忑");
                dataset.println();

                dataset.println("     * ## ");

                dataset.println("// ");
            }

        });
    }

    @Test
    public void hashCode_equals_toString() {
        RowDataset ds1 = createSimpleDataset();
        RowDataset ds2 = createSimpleDataset();
        RowDataset ds3 = new RowDataset(Arrays.asList("ID", "Name", "Value"), Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 3)),
                new ArrayList<>(Arrays.asList("Alice", "Bob", "Charlie")), new ArrayList<>(Arrays.asList(30.0, 24.0, 35.0))));
        RowDataset ds4 = new RowDataset(Arrays.asList("ID", "Name", "Age"), Arrays.asList(new ArrayList<>(Arrays.asList(1, 2, 99)),
                new ArrayList<>(Arrays.asList("Alice", "Bob", "David")), new ArrayList<>(Arrays.asList(30, 24, 40))));

        assertEquals(ds1.hashCode(), ds2.hashCode());
        assertTrue(ds1.equals(ds2));
        assertTrue(ds2.equals(ds1));

        assertFalse(ds1.equals(ds3));
        assertFalse(ds1.equals(ds4));
        assertFalse(ds1.equals(null));
        assertFalse(ds1.equals("a string"));

        assertNotNull(ds1.toString());
        assertTrue(ds1.toString().contains("columnNames=[ID, Name, Age]"));
    }

    @Test
    public void testInvalidRowAccess() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(10, 0));
    }

    @Test
    @DisplayName("filter(column, predicate, max=0) must not return any matching rows")
    public void testFilterSingleColumnWithZeroMax() {
        // Predicate would match every row; with max=0 the result must be empty.
        Dataset filtered = dataset.filter("age", (Integer age) -> true, 0);
        assertEquals(0, filtered.size(), "filter with max=0 must yield 0 rows");
        assertEquals(dataset.columnNames(), filtered.columnNames());
    }

    @Test
    @DisplayName("filter(columns, predicate, max=0) must not return any matching rows")
    public void testFilterMultiColumnWithZeroMax() {
        Dataset filtered = dataset.filter(Arrays.asList("id", "age"), arr -> true, 0);
        assertEquals(0, filtered.size(), "filter with max=0 must yield 0 rows");
        assertEquals(dataset.columnNames(), filtered.columnNames());
    }

    @Test
    @DisplayName("filter respects the max cap when many rows match")
    public void testFilterRespectsMaxCap() {
        Dataset filtered = dataset.filter("age", (Integer age) -> true, 2);
        assertEquals(2, filtered.size());
    }

    @Test
    @DisplayName("paginate(0) and negative page sizes must be rejected")
    public void testPaginateInvalidPageSize() {
        assertThrows(IllegalArgumentException.class, () -> dataset.paginate(0));
        assertThrows(IllegalArgumentException.class, () -> dataset.paginate(-1));
    }

    @Test
    @DisplayName("groupBy treats null keys as a single group (NULL_SENTINEL)")
    public void testGroupByNullKeys() {
        List<String> cols = new ArrayList<>(Arrays.asList("k", "v"));
        List<List<Object>> data = new ArrayList<>();
        data.add(new ArrayList<>(Arrays.asList(null, null, "x", "x", null)));
        data.add(new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5)));
        Dataset ds = new RowDataset(cols, data);
        Dataset grouped = ds.groupBy(Arrays.asList("k"));
        // Two distinct groups: null group and "x" group.
        assertEquals(2, grouped.size());
    }

    @Test
    @DisplayName("sortBy on a single column is stable for equal keys")
    public void testSortByStableWithEqualKeys() {
        // Two rows have the same age (28); their relative original order must be preserved.
        // Original: (A,30) (B,28) (C,28) (D,31) → stable-sorted by age: (B,28) (C,28) (A,30) (D,31)
        List<String> cols = new ArrayList<>(Arrays.asList("name", "age"));
        List<List<Object>> data = new ArrayList<>();
        data.add(new ArrayList<>(Arrays.asList("A", "B", "C", "D")));
        data.add(new ArrayList<>(Arrays.asList(30, 28, 28, 31)));
        Dataset ds = new RowDataset(cols, data);
        ds.sortBy("age");
        assertEquals("B", ds.moveToRow(0).get("name"));
        assertEquals("C", ds.moveToRow(1).get("name"));
        assertEquals("A", ds.moveToRow(2).get("name"));
        assertEquals("D", ds.moveToRow(3).get("name"));
    }

    @Test
    @DisplayName("copy makes a fresh column list — mutations don't leak across datasets")
    public void testCopyIsolation() {
        Dataset copy = dataset.copy();
        copy.moveToRow(0).set("name", "MUTATED");
        assertEquals("John", dataset.moveToRow(0).get("name"));
        assertEquals("MUTATED", copy.moveToRow(0).get("name"));
    }

    @Test
    @DisplayName("frozen dataset rejects mutations")
    public void testFrozenIsImmutable() {
        Dataset frozen = dataset.copy();
        frozen.freeze();
        assertTrue(frozen.isFrozen());
        assertThrows(IllegalStateException.class, () -> frozen.addRow(new Object[] { 99, "X", 99, 0d }));
        assertThrows(IllegalStateException.class, () -> frozen.removeRow(0));
        assertThrows(IllegalStateException.class, () -> frozen.renameColumn("name", "renamed"));
    }

    @Test
    public void testCopyOutOfRangeRowIndexThrowsIndexOutOfBounds() {
        // Dataset.copy(int,int) is contractually documented to throw IndexOutOfBoundsException for an
        // invalid row-index range, consistent with slice(int,int)/stream(int,int). The private copy()
        // previously skipped validation: from>to surfaced an IllegalArgumentException from List.subList,
        // and an out-of-range range on a column-less Dataset was silently a no-op.
        final Dataset ds = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 3, "Charlie" } });

        assertThrows(IndexOutOfBoundsException.class, () -> ds.copy(2, 1)); // from > to (was IllegalArgumentException)
        assertThrows(IndexOutOfBoundsException.class, () -> ds.copy(0, ds.size() + 1));
        assertThrows(IndexOutOfBoundsException.class, () -> ds.copy(-1, 2));

        // A column-less / empty Dataset must still validate the range (was silently a no-op).
        final Dataset empty = CommonUtil.newEmptyDataset();
        assertThrows(IndexOutOfBoundsException.class, () -> empty.copy(5, 10));

        // Valid ranges keep working.
        assertEquals(2, ds.copy(1, 3).size());
        assertEquals(0, empty.copy(0, 0).size());
    }

}
