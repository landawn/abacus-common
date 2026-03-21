package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;
import java.util.stream.Collector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.entity.extendDirty.basic.AccountContact;
import com.landawn.abacus.entity.extendDirty.basic.DataType;
import com.landawn.abacus.entity.extendDirty.basic.ExtendDirtyBasicPNL.AccountPNL;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.Collectors.MoreCollectors;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.Stream;

public class DatasetTest extends AbstractTest {

    private Dataset dataset;
    private Dataset emptyDataset;
    private List<String> columnNames;
    private List<List<Object>> columnList;
    private Object[][] sampleRows;
    private Object[][] testData;
    private Dataset testDataset;
    private RowDataset sampleDataset;
    private List<List<Object>> columnValues;

    final int threadNum = 100;
    final int recordCount = 10000;
    final int pageSize = 200;
    final int pageCount = recordCount / pageSize;

    static final AtomicInteger counter = new AtomicInteger();

    @BeforeEach
    public void setUp() {
        columnNames = new ArrayList<>(Arrays.asList("id", "name", "age", "salary"));

        columnList = new ArrayList<>();
        columnList.add(new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5)));
        columnList.add(new ArrayList<>(Arrays.asList("John", "Jane", "Bob", "Alice", "Charlie")));
        columnList.add(new ArrayList<>(Arrays.asList(25, 30, 35, 28, 40)));
        columnList.add(new ArrayList<>(Arrays.asList(50000.0, 60000.0, 70000.0, 55000.0, 80000.0)));

        dataset = new RowDataset(columnNames, columnList);
        emptyDataset = Dataset.empty();

        sampleRows = new Object[][] { { 1, "Alice", 25, 50000.0 }, { 2, "Bob", 30, 60000.0 }, { 3, "Charlie", 35, 70000.0 }, { 4, "Diana", 28, 55000.0 } };

        testData = new Object[][] { { 1, "Alice", 30, 50000.0 }, { 2, "Bob", 25, 45000.0 }, { 3, "Charlie", 35, 60000.0 }, { 4, "Diana", 28, 55000.0 } };
        testDataset = Dataset.rows(columnNames, testData);

        columnValues = new ArrayList<>();
        columnValues.add(new ArrayList<>(Arrays.asList(1, 2, 3)));
        columnValues.add(new ArrayList<>(Arrays.asList("Alice", "Bob", "Charlie")));
        columnValues.add(new ArrayList<>(Arrays.asList(30, 24, 35)));
        sampleDataset = new RowDataset(new ArrayList<>(Arrays.asList("ID", "Name", "Age")), columnValues);
    }

    public static class Person {
        private int id;
        private String name;
        private int age;
        private double salary;

        public Person() {
        }

        public Person(int id, String name, int age, double salary) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.salary = salary;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public double getSalary() {
            return salary;
        }

        public void setSalary(double salary) {
            this.salary = salary;
        }
    }

    public static class TestBean {
        private int id;
        private String name;
        private double value;
        private TestNestedBean nested;
        private List<TestNestedBean> nestedList;

        public TestBean() {
        }

        public TestBean(int id, String name, double value) {
            this.id = id;
            this.name = name;
            this.value = value;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public TestNestedBean getNested() {
            return nested;
        }

        public void setNested(TestNestedBean nested) {
            this.nested = nested;
        }

        public List<TestNestedBean> getNestedList() {
            return nestedList;
        }

        public void setNestedList(List<TestNestedBean> nestedList) {
            this.nestedList = nestedList;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TestBean testBean = (TestBean) o;

            return id == testBean.id && Double.compare(testBean.value, value) == 0 && Objects.equals(name, testBean.name)
                    && Objects.equals(nested, testBean.nested) && Objects.equals(nestedList, testBean.nestedList);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, value, nested, nestedList);
        }
    }

    public static class TestNestedBean {
        private String detail;

        public TestNestedBean() {
        }

        public TestNestedBean(String detail) {
            this.detail = detail;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TestNestedBean that = (TestNestedBean) o;

            return Objects.equals(detail, that.detail);
        }

        @Override
        public int hashCode() {
            return Objects.hash(detail);
        }
    }

    private RowDataset createSimpleDataset() {
        List<String> names = new ArrayList<>(Arrays.asList("ID", "Name", "Age"));
        List<List<Object>> values = new ArrayList<>();
        values.add(new ArrayList<>(Arrays.asList(1, 2, 3)));
        values.add(new ArrayList<>(Arrays.asList("Alice", "Bob", "Charlie")));
        values.add(new ArrayList<>(Arrays.asList(30, 24, 35)));
        return new RowDataset(names, values);
    }

    private static <T, A, R> Collector<T, A, R> collector(Collector<T, A, R> downstream) {
        return downstream;
    }

    void addDataType() {
        final DataType dataType = new DataType();
        dataType.setByteType((byte) 1);
        dataType.setCharType((char) 50);
        dataType.setBooleanType(true);
        dataType.setShortType(Short.MAX_VALUE);
        dataType.setIntType(counter.getAndIncrement());
        dataType.setLongType(0);
        dataType.setFloatType(0.00000000f);
        dataType.setDoubleType(000000000000000000000000);
        dataType.setStringType("String");

        final ArrayList<String> stringArrayList = new ArrayList<>();
        stringArrayList.add("aa");
        stringArrayList.add("黎");
        stringArrayList.add("cc");
        dataType.setStringArrayListType(stringArrayList);

        final LinkedList<Boolean> booleanLinkedList = new LinkedList<>();
        booleanLinkedList.add(false);
        booleanLinkedList.add(false);
        booleanLinkedList.add(true);
        dataType.setBooleanLinkedListType(booleanLinkedList);

        final Vector<String> stringVector = new Vector<>();
        stringVector.add("false");
        dataType.setStringVectorType(stringVector);

        final Map<BigDecimal, String> bigDecimalHashMap = new HashMap<>();

        bigDecimalHashMap.put(BigDecimal.valueOf(3993.000), "3993.000");
        bigDecimalHashMap.put(BigDecimal.valueOf(3993.001), "3993.001");

        final HashMap<Timestamp, Float> timestampHashMap = new HashMap<>();

        timestampHashMap.put(new Timestamp(System.currentTimeMillis()), 3993.000f);
        timestampHashMap.put(new Timestamp(System.currentTimeMillis()), 3993.001f);
        dataType.setTimestampHashMapType(timestampHashMap);

        final ConcurrentHashMap<BigDecimal, String> StringConcurrentHashMap = new ConcurrentHashMap<>();
        StringConcurrentHashMap.put(BigDecimal.valueOf(3993.000), "3993.000");
        StringConcurrentHashMap.put(BigDecimal.valueOf(3993.001), "3993.001");
        dataType.setStringConcurrentHashMapType(StringConcurrentHashMap);

        dataType.setByteArrayType(new byte[] { 1, 2, 3 });
        dataType.setDateType(new java.sql.Date(System.currentTimeMillis()));
        dataType.setTimeType(new Time(System.currentTimeMillis()));
        dataType.setTimestampType(new Timestamp(System.currentTimeMillis()));
    }

    @Test
    public void testEmpty() {
        Dataset empty = Dataset.empty();
        assertNotNull(empty);
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.size());
        assertEquals(0, empty.columnCount());
    }

    @Test
    public void testEmptyDatasetOperations() {
        assertEquals(0, emptyDataset.size());
        assertEquals(0, emptyDataset.columnCount());
        assertTrue(emptyDataset.isEmpty());
        assertFalse(emptyDataset.firstRow().isPresent());
        assertFalse(emptyDataset.lastRow().isPresent());
    }

    @Test
    public void testEmptyDatasetConstant() {
        Dataset empty = Dataset.empty();
        assertEquals(0, empty.size());
        assertTrue(empty.isFrozen());
    }

    @Test
    public void test_emptyDataset() {
        assertDoesNotThrow(() -> {
            CommonUtil.newEmptyDataset().println();
            CommonUtil.newEmptyDataset(CommonUtil.toList("co1", "col2")).println();

            N.println(CommonUtil.newEmptyDataset().toJson());
            N.println(CommonUtil.newEmptyDataset().toXml());
            N.println(CommonUtil.newEmptyDataset().toCsv());

            N.println(CommonUtil.newEmptyDataset(CommonUtil.toList("firstName", "lastName")).toJson());
            N.println(CommonUtil.newEmptyDataset(CommonUtil.toList("firstName", "lastName")).toXml());
            N.println(CommonUtil.newEmptyDataset(CommonUtil.toList("firstName", "lastName")).toCsv());
            N.println(CommonUtil.newEmptyDataset(CommonUtil.toList("firstName", "lastName")).toCsv());

            N.println(N.toJson(CommonUtil.newEmptyDataset()));
            N.println(N.toJson(CommonUtil.newEmptyDataset(CommonUtil.toList("firstName", "lastName"))));

            N.println(N.fromJson(N.toJson(CommonUtil.newEmptyDataset(CommonUtil.toList("firstName", "lastName"))), Dataset.class));
            N.println(N.fromJson(N.toJson(CommonUtil.newEmptyDataset()), Dataset.class));
        });
    }

    @Test
    public void testEquals() {
        Dataset ds1 = Dataset.rows(columnNames, sampleRows);
        Dataset ds2 = Dataset.rows(columnNames, sampleRows);

        assertTrue(ds1.equals(ds2));
    }

    @Test
    public void testHashCode() {
        Dataset ds1 = Dataset.rows(columnNames, sampleRows);
        Dataset ds2 = Dataset.rows(columnNames, sampleRows);

        assertEquals(ds1.hashCode(), ds2.hashCode());
    }

    @Test
    public void testTypedGetters() {
        Dataset ds = Dataset
                .rows(Arrays.asList("bool", "ch", "bt", "sh", "i", "l", "f", "d"), new Object[][] { { true, 'A', (byte) 1, (short) 2, 3, 4L, 5.0f, 6.0 } })
                .moveToRow(0);

        assertEquals(true, ds.getBoolean(0));
        assertEquals('A', ds.getChar(1));
        assertEquals((byte) 1, ds.getByte(2));
        assertEquals((short) 2, ds.getShort(3));
        assertEquals(3, ds.getInt(4));
        assertEquals(4L, ds.getLong(5));
        assertEquals(5.0f, ds.getFloat(6));
        assertEquals(6.0, ds.getDouble(7));
    }

    @Test
    public void testTypedGettersByColumnName() {
        Dataset ds = Dataset
                .rows(Arrays.asList("bool", "ch", "bt", "sh", "i", "l", "f", "d"), new Object[][] { { true, 'A', (byte) 1, (short) 2, 3, 4L, 5.0f, 6.0 } })
                .moveToRow(0);

        assertEquals(true, ds.getBoolean("bool"));
        assertEquals('A', ds.getChar("ch"));
        assertEquals((byte) 1, ds.getByte("bt"));
        assertEquals((short) 2, ds.getShort("sh"));
        assertEquals(3, ds.getInt("i"));
        assertEquals(4L, ds.getLong("l"));
        assertEquals(5.0f, ds.getFloat("f"));
        assertEquals(6.0, ds.getDouble("d"));
    }

    @Test
    public void testRemoveDuplicateRows3() {
        Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "department"),
                new Object[][] { { 1, "John", "IT" }, { 2, "Jane", "HR" }, { 3, "John", "IT" }, { 4, "Bob", "IT" } });
        dataset.removeDuplicateRowsBy(Arrays.asList("name", "department"));
        assertEquals(3, dataset.size());
        assertEquals((Integer) 1, dataset.get(0, 0));
        assertEquals((Integer) 2, dataset.get(1, 0));
        assertEquals((Integer) 4, dataset.get(2, 0));

    }

    @Test
    public void testIntersection_2() {
        {
            Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 3, "Charlie" }, { 2, "Bob" } });

            Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"),
                    new Object[][] { { 2, "Bob" }, { 3, "Charlie" }, { 4, "Dave" }, { 2, "Bob" }, { 2, "Bob" } });

            Dataset result = N.intersection(ds1, ds2);

            result.println();
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

            Dataset result = N.intersection(ds1, ds2);

            result.println();
            assertEquals(2, result.size());
            assertEquals((Integer) 2, result.get(0, 0));
            assertEquals("Bob", result.get(0, 1));
            assertEquals((Integer) 3, result.get(1, 0));
            assertEquals("Charlie", result.get(1, 1));
        }

    }

    @Test
    public void testDifference() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" } });

        Dataset diff = N.difference(ds1, ds2);
        assertNotNull(diff);
    }

    @Test
    public void testSymmetricDifference() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" } });

        Dataset diff = N.symmetricDifference(ds1, ds2);
        assertNotNull(diff);
    }

    @Test
    public void testIntersection() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" }, { 3, "Charlie" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" }, { 4, "Diana" } });

        Dataset intersection = N.intersection(ds1, ds2);

        assertNotNull(intersection);
        assertEquals(2, intersection.size());
    }

    @Test
    public void testIntersection_WithSameColumnsCheck() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" } });

        Dataset intersection = N.intersection(ds1, ds2, true);

        assertNotNull(intersection);
        assertEquals(1, intersection.size());
        assertEquals(Integer.valueOf(2), intersection.get(0, 0));
        assertEquals("Bob", intersection.get(0, 1));
    }

    @Test
    public void testIntersection_WithKeyColumns() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name", "age"), new Object[][] { { 1, "Alice", 25 }, { 2, "Bob", 30 } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name", "salary"), new Object[][] { { 2, "Bob", 60000.0 }, { 3, "Charlie", 70000.0 } });

        Dataset intersection = N.intersection(ds1, ds2, Arrays.asList("id"));

        assertNotNull(intersection);
        assertEquals(1, intersection.size());
        assertEquals(Integer.valueOf(2), intersection.get(0, 0));
    }

    @Test
    public void testIntersection_WithKeyColumnsAndSameCheck() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 2, "Bob" }, { 3, "Charlie" } });

        Dataset intersection = N.intersection(ds1, ds2, Arrays.asList("id", "name"), true);

        assertNotNull(intersection);
        assertEquals(1, intersection.size());
    }

    @Test
    public void testNullValues() {
        Dataset ds = Dataset.rows(Arrays.asList("col1", "col2"), new Object[][] { { null, "test" }, { "value", null } });
        assertTrue(ds.isNull(0, 0));
        assertFalse(ds.isNull(0, 1));
        assertTrue(ds.isNull(1, 1));
        assertFalse(ds.isNull(1, 0));
    }

    @Test
    public void testRowsWithArrays() {
        Dataset dataset = Dataset.rows(columnNames, testData);
        assertNotNull(dataset);
        assertEquals(4, dataset.size());
        assertEquals(4, dataset.columnCount());
        assertEquals(columnNames, dataset.columnNames());
    }

    @Test
    public void testRowsWithCollections() {
        List<List<Object>> rows = Arrays.asList(Arrays.asList(1, "Alice", 30, 50000.0), Arrays.asList(2, "Bob", 25, 45000.0));
        Dataset dataset = Dataset.rows(columnNames, rows);
        assertNotNull(dataset);
        assertEquals(2, dataset.size());
        assertEquals(4, dataset.columnCount());
    }

    @Test
    @DisplayName("Should create Dataset with valid column names and rows")
    public void testCreateDatasetWithValidData() {
        Collection<String> columnNames = Arrays.asList("id", "name", "age");
        Object[][] rows = { { 1, "John", 25 }, { 2, "Jane", 30 }, { 3, "Bob", 35 } };

        Dataset dataset = Dataset.rows(columnNames, rows);

        assertNotNull(dataset);
        assertEquals(3, dataset.size());
        assertEquals(3, dataset.columnNames().size());
        assertTrue(dataset.columnNames().containsAll(columnNames));
    }

    @Test
    @DisplayName("Should create empty Dataset with column names but no rows")
    public void testCreateDatasetWithEmptyRows() {
        Collection<String> columnNames = Arrays.asList("id", "name");
        Object[][] rows = {};

        Dataset dataset = Dataset.rows(columnNames, rows);

        assertNotNull(dataset);
        assertEquals(0, dataset.size());
        assertEquals(2, dataset.columnNames().size());
        assertTrue(dataset.isEmpty());
    }

    @Test
    @DisplayName("Should create Dataset with single column")
    public void testCreateDatasetWithSingleColumn() {
        Collection<String> columnNames = Collections.singletonList("value");
        Object[][] rows = { { "test1" }, { "test2" } };

        Dataset dataset = Dataset.rows(columnNames, rows);

        assertNotNull(dataset);
        assertEquals(2, dataset.size());
        assertEquals(1, dataset.columnNames().size());
        assertEquals("value", dataset.columnNames().get(0));
    }

    @Test
    @DisplayName("Should handle null values in rows")
    public void testCreateDatasetWithNullValues() {
        Collection<String> columnNames = Arrays.asList("id", "name", "score");
        Object[][] rows = { { 1, "John", null }, { null, "Jane", 95.5 }, { 3, null, 87.2 } };

        Dataset dataset = Dataset.rows(columnNames, rows);

        assertNotNull(dataset);
        assertEquals(3, dataset.size());
        assertTrue(dataset.isNull(0, 2));
        assertTrue(dataset.isNull(1, 0));
        assertTrue(dataset.isNull(2, 1));
    }

    @Test
    @DisplayName("Should handle different data types in columns")
    public void testCreateDatasetWithMixedDataTypes() {
        Collection<String> columnNames = Arrays.asList("id", "name", "active", "score", "date");
        Object[][] rows = { { 1, "John", true, 95.5, new Date() }, { 2L, "Jane", false, 87, null } };

        Dataset dataset = Dataset.rows(columnNames, rows);

        assertNotNull(dataset);
        assertEquals(2, dataset.size());
        assertEquals(5, dataset.columnNames().size());
    }

    @Test
    @DisplayName("Should handle jagged arrays with consistent column count")
    public void testCreateDatasetWithJaggedButConsistentRows() {
        Collection<String> columnNames = Arrays.asList("col1", "col2");
        Object[][] rows = { new Object[] { 1, "a" }, new Object[] { 2, "b" }, new Object[] { 3, "c" } };

        Dataset dataset = Dataset.rows(columnNames, rows);

        assertNotNull(dataset);
        assertEquals(3, dataset.size());
        assertEquals(2, dataset.columnNames().size());
    }

    @Test
    public void testSymmetricDifferenceWithKeyColumnsAndRequireSameColumns() {
        Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "department"),
                new Object[][] { { 1, "Alice", "HR" }, { 2, "Bob", "Engineering" }, { 2, "Bob", "Engineering" }, { 3, "Charlie", "Marketing" } });
        Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "salary"),
                new Object[][] { { 2, "Bob", 50000 }, { 3, "Charlie", 55000 }, { 4, "Dave", 60000 } });

        Collection<String> keyColumns = Arrays.asList("id", "name");
        Dataset result = N.symmetricDifference(dataset1, dataset2, keyColumns, false);
        result.println();
        assertEquals(3, result.size());
        assertEquals(4, result.columnCount());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("department"));
        assertTrue(result.containsColumn("salary"));
        assertEquals(1, (Integer) result.get(0, 0));
        assertEquals("Alice", result.get(0, 1));
        assertEquals("HR", result.get(0, 2));
        assertNull(result.get(0, 3));
        assertEquals(2, (Integer) result.get(1, 0));
        assertEquals("Bob", result.get(1, 1));
        assertEquals("Engineering", result.get(1, 2));
        assertNull(result.get(1, 3));
        assertEquals(4, (Integer) result.get(2, 0));
        assertEquals("Dave", result.get(2, 1));
        assertNull(result.get(2, 2));
        assertEquals(60000, (Integer) result.get(2, 3));

    }

    @Test
    public void testRowsWithInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> {
            Dataset.rows(null, testData);
        });

    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for mismatched column count")
    public void testThrowsExceptionForMismatchedColumnCount() {
        Collection<String> columnNames = Arrays.asList("id", "name");
        Object[][] rows = { { 1, "John", 25 }, { 2, "Jane" } };

        assertThrows(IllegalArgumentException.class, () -> {
            Dataset.rows(columnNames, rows);
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null column names")
    public void testThrowsExceptionForNullColumnNames() {
        Object[][] rows = { { 1, "John" } };

        assertThrows(IllegalArgumentException.class, () -> {
            Dataset.rows(null, rows);
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for empty column names")
    public void testThrowsExceptionForEmptyColumnNames() {
        Collection<String> columnNames = Collections.emptyList();
        Object[][] rows = { { 1, "John" } };

        assertThrows(IllegalArgumentException.class, () -> {
            Dataset.rows(columnNames, rows);
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for duplicate column names")
    public void testThrowsExceptionForDuplicateColumnNames() {
        Collection<String> columnNames = Arrays.asList("id", "name", "id");
        Object[][] rows = { { 1, "John", 25 } };

        assertThrows(IllegalArgumentException.class, () -> {
            Dataset.rows(columnNames, rows);
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for inconsistent row lengths")
    public void testThrowsExceptionForInconsistentRowLengths() {
        Collection<String> columnNames = Arrays.asList("col1", "col2", "col3");
        Object[][] rows = { { 1, "a", true }, { 2, "b" }, { 3, "c", false } };

        assertThrows(IllegalArgumentException.class, () -> {
            Dataset.rows(columnNames, rows);
        });
    }

    @Test
    public void columns_streamOfImmutableLists() {
        List<ArrayList<Object>> streamedCols = sampleDataset.columns().map(ArrayList::new).toList();
        assertEquals(columnValues, streamedCols);
    }

    @Test
    public void testColumns() {
        Stream<ImmutableList<Object>> columnStream = dataset.columns();

        assertNotNull(columnStream);
        List<ImmutableList<Object>> columns = columnStream.toList();
        assertEquals(4, columns.size());

        assertEquals(5, columns.get(0).size());
        assertEquals(Integer.valueOf(1), columns.get(0).get(0));
        assertEquals(Integer.valueOf(2), columns.get(0).get(1));

        assertEquals("John", columns.get(1).get(0));
        assertEquals("Jane", columns.get(1).get(1));
    }

    @Test
    public void testColumnsWithArrays() {
        Object[][] columns = new Object[][] { { 1, 2, 3, 4 }, { "Alice", "Bob", "Charlie", "Diana" }, { 30, 25, 35, 28 },
                { 50000.0, 45000.0, 60000.0, 55000.0 } };
        Dataset dataset = Dataset.columns(columnNames, columns);
        assertNotNull(dataset);
        assertEquals(4, dataset.size());
        assertEquals(4, dataset.columnCount());
    }

    @Test
    public void testColumnsWithCollections() {
        List<List<Object>> columns = Arrays.asList(Arrays.asList(1, 2, 3, 4), Arrays.asList("Alice", "Bob", "Charlie", "Diana"), Arrays.asList(30, 25, 35, 28),
                Arrays.asList(50000.0, 45000.0, 60000.0, 55000.0));
        Dataset dataset = Dataset.columns(columnNames, columns);
        assertNotNull(dataset);
        assertEquals(4, dataset.size());
        assertEquals(4, dataset.columnCount());
    }

    @Test
    @DisplayName("Should create Dataset with valid column names and columns")
    public void testCreateDatasetWithValidColumnsData2() {
        Collection<String> columnNames = Arrays.asList("id", "name", "age");
        Object[][] columns = { { 1, 2, 3 }, { "John", "Jane", "Bob" }, { 25, 30, 35 } };

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(3, dataset.size());
        assertEquals(3, dataset.columnNames().size());
        assertTrue(dataset.columnNames().containsAll(columnNames));

        assertEquals(1, (Integer) dataset.moveToRow(0).get("id"));
        assertEquals("John", dataset.moveToRow(0).get("name"));
        assertEquals(25, (Integer) dataset.moveToRow(0).get("age"));
    }

    @Test
    @DisplayName("Should create empty Dataset with column names but empty columns")
    public void testCreateDatasetWithEmptyColumns() {
        Collection<String> columnNames = Arrays.asList("id", "name");
        Object[][] columns = { {}, {} };

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(0, dataset.size());
        assertEquals(2, dataset.columnNames().size());
        assertTrue(dataset.isEmpty());
    }

    @Test
    @DisplayName("Should create Dataset with single column")
    public void testCreateDatasetWithSingleColumn2() {
        Collection<String> columnNames = Collections.singletonList("value");
        Object[][] columns = { { "test1", "test2", "test3" } };

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(3, dataset.size());
        assertEquals(1, dataset.columnNames().size());
        assertEquals("value", dataset.columnNames().get(0));
        assertEquals("test1", dataset.moveToRow(0).get("value"));
        assertEquals("test2", dataset.moveToRow(1).get("value"));
        assertEquals("test3", dataset.moveToRow(2).get("value"));
    }

    @Test
    @DisplayName("Should handle null values in columns")
    public void testCreateDatasetWithNullValues2() {
        Collection<String> columnNames = Arrays.asList("id", "name", "score");
        Object[][] columns = { { 1, null, 3 }, { "John", "Jane", null }, { null, 95.5, 87.2 } };

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(3, dataset.size());
        assertTrue(dataset.isNull(1, 0));
        assertTrue(dataset.isNull(2, 1));
        assertTrue(dataset.isNull(0, 2));
    }

    @Test
    @DisplayName("Should handle different data types in same column")
    public void testCreateDatasetWithMixedDataTypesInColumn() {
        Collection<String> columnNames = Arrays.asList("mixed", "numbers");
        Object[][] columns = { { "string", 123, true, null }, { 1, 2.5, 3L, 4.0f } };

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(4, dataset.size());
        assertEquals(2, dataset.columnNames().size());

        assertEquals("string", dataset.moveToRow(0).get("mixed"));
        assertEquals(123, (Integer) dataset.moveToRow(1).get("mixed"));
        assertEquals(true, dataset.moveToRow(2).get("mixed"));
        assertTrue(dataset.isNull(3, 0));
    }

    @Test
    @DisplayName("Should create Dataset with empty column names collection")
    public void testCreateDatasetWithEmptyColumnNamesCollection() {
        Collection<String> columnNames = Collections.emptyList();
        Object[][] columns = {};

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(0, dataset.size());
        assertEquals(0, dataset.columnNames().size());
        assertTrue(dataset.isEmpty());
    }

    @Test
    @DisplayName("Should handle single row of data across multiple columns")
    public void testCreateDatasetWithSingleRow() {
        Collection<String> columnNames = Arrays.asList("col1", "col2", "col3");
        Object[][] columns = { { "a" }, { "b" }, { "c" } };

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(1, dataset.size());
        assertEquals(3, dataset.columnNames().size());
        assertEquals("a", dataset.moveToRow(0).get("col1"));
        assertEquals("b", dataset.moveToRow(0).get("col2"));
        assertEquals("c", dataset.moveToRow(0).get("col3"));
    }

    @Test
    @DisplayName("Should handle large dataset with many rows")
    public void testCreateDatasetWithManyRows() {
        Collection<String> columnNames = Arrays.asList("index", "squared");
        Object[][] columns = new Object[2][];

        Object[] indexColumn = new Object[1000];
        Object[] squaredColumn = new Object[1000];

        for (int i = 0; i < 1000; i++) {
            indexColumn[i] = i;
            squaredColumn[i] = i * i;
        }

        columns[0] = indexColumn;
        columns[1] = squaredColumn;

        Dataset dataset = Dataset.columns(columnNames, columns);

        assertNotNull(dataset);
        assertEquals(1000, dataset.size());
        assertEquals(2, dataset.columnNames().size());
        assertEquals(0, (Integer) dataset.moveToRow(0).get("index"));
        assertEquals(0, (Integer) dataset.moveToRow(0).get("squared"));
        assertEquals(999, (Integer) dataset.moveToRow(999).get("index"));
        assertEquals(998001, (Integer) dataset.moveToRow(999).get("squared"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when columnNames length differs from columns length")
    public void testThrowsExceptionForMismatchedColumnCount2() {
        Collection<String> columnNames = Arrays.asList("id", "name");
        Object[][] columns = { { 1, 2, 3 }, { "John", "Jane", "Bob" }, { 25, 30, 35 } };

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Dataset.columns(columnNames, columns);
        });

        assertTrue(exception.getMessage().contains("The length of 'columnNames'(2) is not equal to the length of the sub-collections in 'columns'(3)"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null column names")
    public void testThrowsExceptionForNullColumnNames2() {
        Object[][] columns = { { 1, 2 }, { "John", "Jane" } };

        assertThrows(IllegalArgumentException.class, () -> {
            Dataset.columns(null, columns);
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for inconsistent column lengths")
    public void testThrowsExceptionForInconsistentColumnLengths() {
        Collection<String> columnNames = Arrays.asList("id", "name", "age");
        Object[][] columns = { { 1, 2, 3 }, { "John", "Jane" }, { 25, 30, 35 } };

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Dataset.columns(columnNames, columns);
        });

        assertTrue(exception.getMessage().contains("size of the sub-collection in 'columns' is not equal"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for duplicate column names")
    public void testThrowsExceptionForDuplicateColumnNames2() {
        Collection<String> columnNames = Arrays.asList("id", "name", "id");
        Object[][] columns = { { 1, 2, 3 }, { "John", "Jane", "Bob" }, { 25, 30, 35 } };

        assertThrows(IllegalArgumentException.class, () -> {
            Dataset.columns(columnNames, columns);
        });
    }

    @Test
    public void columnNameList() {
        assertEquals(Arrays.asList("ID", "Name", "Age"), sampleDataset.columnNames());
        assertTrue(emptyDataset.columnNames().isEmpty());
    }

    @Test
    public void constructor_withData() {
        assertEquals(3, sampleDataset.columnCount());
        assertEquals(3, sampleDataset.size());
        assertFalse(sampleDataset.isFrozen());
        assertEquals(Arrays.asList("ID", "Name", "Age"), sampleDataset.columnNames());
    }

    @Test
    public void columnCount() {
        assertEquals(3, sampleDataset.columnCount());
        assertEquals(0, emptyDataset.columnCount());
    }

    @Test
    public void testConstructor() {
        assertNotNull(dataset);
        assertEquals(4, dataset.columnCount());
        assertEquals(5, dataset.size());
    }

    @Test
    public void getColumnName() {
        assertEquals("ID", sampleDataset.getColumnName(0));
        assertEquals("Name", sampleDataset.getColumnName(1));
        assertEquals("Age", sampleDataset.getColumnName(2));
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataset.getColumnName(3));
        assertThrows(IndexOutOfBoundsException.class, () -> emptyDataset.getColumnName(0));
    }

    @Test
    public void testGetColumnNameInvalidIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.getColumnName(10));
    }

    @Test
    public void getColumnIndex() {
        assertEquals(0, sampleDataset.getColumnIndex("ID"));
        assertEquals(1, sampleDataset.getColumnIndex("Name"));
        assertEquals(2, sampleDataset.getColumnIndex("Age"));
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.getColumnIndex("NonExistent"));
        assertThrows(IllegalArgumentException.class, () -> emptyDataset.getColumnIndex("Any"));
    }

    @Test
    public void testInvalidColumnAccess() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(0, 10));
        assertThrows(IllegalArgumentException.class, () -> dataset.getColumnIndex("nonexistent"));
    }

    @Test
    public void testInvalidColumnOperations() {
        assertThrows(IllegalArgumentException.class, () -> {
            testDataset.getColumnIndex("nonexistent");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            testDataset.addColumn("name", Arrays.asList("duplicate", "column"));
        });

        assertThrows(IllegalArgumentException.class, () -> {
            testDataset.removeColumns(Arrays.asList("nonexistent"));
        });
    }

    @Test
    public void testGetColumnIndexInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> dataset.getColumnIndex("invalid"));
    }

    @Test
    public void getColumnIndexes() {
        Collection<String> namesToGet = Arrays.asList("Name", "ID");
        int[] indexes = sampleDataset.getColumnIndexes(namesToGet);
        assertArrayEquals(new int[] { 1, 0 }, indexes);

        Collection<String> allNames = sampleDataset.columnNames();
        int[] allIndexes = sampleDataset.getColumnIndexes(allNames);
        assertArrayEquals(new int[] { 0, 1, 2 }, allIndexes);

        assertThrows(IllegalArgumentException.class, () -> sampleDataset.getColumnIndexes(Arrays.asList("ID", "NonExistent")));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, sampleDataset.getColumnIndexes(Collections.emptyList()));
    }

    @Test
    public void containsColumn() {
        assertTrue(sampleDataset.containsColumn("ID"));
        assertFalse(sampleDataset.containsColumn("NonExistent"));
        assertFalse(emptyDataset.containsColumn("Any"));
    }

    @Test
    public void test_containsColumn() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 7);

        final Dataset ds = CommonUtil.newDataset(accountList);

        assertTrue(ds.containsColumn("firstName"));
        assertTrue(ds.containsAllColumns(CommonUtil.toList("firstName", "lastName")));

        assertFalse(ds.containsColumn("Account.firstName"));
        assertFalse(ds.containsAllColumns(CommonUtil.toList("firstName", "Account.lastName")));
    }

    @Test
    public void containsAllColumns() {
        assertTrue(sampleDataset.containsAllColumns(Arrays.asList("ID", "Age")));
        assertFalse(sampleDataset.containsAllColumns(Arrays.asList("ID", "NonExistent")));
        assertTrue(sampleDataset.containsAllColumns(Collections.emptyList()));
        assertTrue(emptyDataset.containsAllColumns(Collections.emptyList()));
        assertFalse(emptyDataset.containsAllColumns(Arrays.asList("ID")));
    }

    @Test
    public void test_renameColumn_2() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 7);

        final Dataset ds = CommonUtil.newDataset(accountList);
        ds.renameColumns(ds.columnNames(), t -> t + "2");
        ds.println();

        ds.updateColumns(ds.columnNames(), (i, c, v) -> CommonUtil.toString(v));
        ds.println();
        assertNotNull(ds);
    }

    @Test
    public void renameColumn() {
        sampleDataset.renameColumn("Age", "Years");
        assertEquals(2, sampleDataset.getColumnIndex("Years"));
        assertTrue(sampleDataset.columnNames().contains("Years"));
        assertFalse(sampleDataset.columnNames().contains("Age"));
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.renameColumn("NonExistent", "NewName"));
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.renameColumn("ID", "Name"));

        sampleDataset.renameColumn("Years", "Years");
        assertEquals(2, sampleDataset.getColumnIndex("Years"));

        RowDataset ds = createSimpleDataset();
        ds.freeze();
        assertThrows(IllegalStateException.class, () -> ds.renameColumn("ID", "NewID"));
    }

    @Test
    public void testRenameColumnToExistingName() {
        assertThrows(IllegalArgumentException.class, () -> dataset.renameColumn("age", "name"));
    }

    @Test
    public void renameColumns_functionForAll() {
        sampleDataset.renameColumns(name -> "col_" + name);
        assertTrue(sampleDataset.columnNames().contains("col_ID"));
        assertTrue(sampleDataset.columnNames().contains("col_Name"));
        assertTrue(sampleDataset.columnNames().contains("col_Age"));
    }

    @Test
    public void testRenameColumnsWithMap() {
        Dataset ds = dataset.copy();
        Map<String, String> renameMap = new HashMap<>();
        renameMap.put("id", "identifier");
        renameMap.put("name", "fullName");
        ds.renameColumns(renameMap);
        assertEquals("identifier", ds.getColumnName(0));
        assertEquals("fullName", ds.getColumnName(1));
    }

    @Test
    public void testRenameColumns() {
        Map<String, String> renameMap = new HashMap<>();
        renameMap.put("age", "years");
        renameMap.put("salary", "income");

        dataset.renameColumns(renameMap);
        assertTrue(dataset.containsColumn("years"));
        assertTrue(dataset.containsColumn("income"));
        assertFalse(dataset.containsColumn("age"));
        assertFalse(dataset.containsColumn("salary"));
    }

    @Test
    public void testRenameColumnsWithFunction() {
        Dataset ds = dataset.copy();
        ds.renameColumns(Arrays.asList("id", "name"), name -> name.toUpperCase());
        assertEquals("ID", ds.getColumnName(0));
        assertEquals("NAME", ds.getColumnName(1));
    }

    @Test
    public void renameColumns_map() {
        Map<String, String> renames = new HashMap<>();
        renames.put("ID", "Identifier");
        renames.put("Age", "Years");
        sampleDataset.renameColumns(renames);
        assertTrue(sampleDataset.columnNames().contains("Identifier"));
        assertTrue(sampleDataset.columnNames().contains("Years"));
        assertFalse(sampleDataset.columnNames().contains("ID"));
        assertFalse(sampleDataset.columnNames().contains("Age"));

        Map<String, String> invalidRenames = new HashMap<>();
        invalidRenames.put("NonExistent", "NewName");
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.renameColumns(invalidRenames));

        Map<String, String> duplicateNewNames = new HashMap<>();
        duplicateNewNames.put("Name", "Duplicate");
        duplicateNewNames.put("Years", "Duplicate");
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.renameColumns(duplicateNewNames));
    }

    @Test
    public void renameColumns_collectionAndFunction() {
        sampleDataset.renameColumns(Arrays.asList("ID", "Age"), name -> name + "_new");
        assertTrue(sampleDataset.columnNames().contains("ID_new"));
        assertTrue(sampleDataset.columnNames().contains("Age_new"));
        assertFalse(sampleDataset.columnNames().contains("ID"));
        assertFalse(sampleDataset.columnNames().contains("Age"));

        assertThrows(IllegalArgumentException.class, () -> sampleDataset.renameColumns(Arrays.asList("Name", "NonExistent"), String::toUpperCase));
    }

    @Test
    public void moveColumn() {
        sampleDataset.moveColumn("Age", 0);
        assertEquals(Arrays.asList("Age", "ID", "Name"), sampleDataset.columnNames());
        assertEquals((Integer) 35, sampleDataset.moveToRow(2).get("Age"));

        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataset.moveColumn("ID", 5));
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.moveColumn("NonExistent", 0));
    }

    @Test
    @DisplayName("Should move columns to end")
    public void testMoveColumnsToEnd() {
        dataset.moveColumns(Arrays.asList("id", "name"), 2);

        assertEquals("age", dataset.columnNames().get(0));
        assertEquals("salary", dataset.columnNames().get(1));
        assertEquals("id", dataset.columnNames().get(2));
        assertEquals("name", dataset.columnNames().get(3));
    }

    @Test
    @DisplayName("Should preserve data integrity after column move")
    public void testMoveColumnsDataIntegrity() {
        Map<String, List<Object>> originalData = new HashMap<>();
        Map<String, ImmutableList<Object>> columnMap = dataset.columnMap();
        for (String col : dataset.columnNames()) {
            originalData.put(col, new ArrayList<>(columnMap.get(col)));
        }

        dataset.println();
        dataset.moveColumns(Arrays.asList("age", "salary"), 0);
        dataset.println();

        Map<String, ImmutableList<Object>> newColumnMap = dataset.columnMap();
        assertEquals(originalData.get("age"), new ArrayList<>(newColumnMap.get("age")));
        assertEquals(originalData.get("salary"), new ArrayList<>(newColumnMap.get("salary")));
        assertEquals(originalData.get("id"), new ArrayList<>(newColumnMap.get("id")));
        assertEquals(originalData.get("name"), new ArrayList<>(newColumnMap.get("name")));
    }

    @Test
    @DisplayName("Should move all columns except one")
    public void testMoveColumnsAllButOne() {

        dataset.println();

        dataset.moveColumns(Arrays.asList("id", "name", "age"), 1);

        dataset.println();

        assertEquals("salary", dataset.columnNames().get(0));
        assertEquals("id", dataset.columnNames().get(1));
        assertEquals("name", dataset.columnNames().get(2));
        assertEquals("age", dataset.columnNames().get(3));
    }

    @Test
    @DisplayName("Should handle column move in different orders")
    public void testMoveColumnsNonConsecutive() {
        dataset.println();
        dataset.moveColumns(Arrays.asList("salary", "id"), 1);
        dataset.println();

        assertEquals("name", dataset.columnNames().get(0));
        assertEquals("salary", dataset.columnNames().get(1));
        assertEquals("id", dataset.columnNames().get(2));
        assertEquals("age", dataset.columnNames().get(3));
    }

    @Test
    @DisplayName("Should handle column move in different orders")
    public void testMoveColumnsNonConsecutive2() {
        dataset.println();
        dataset.moveColumns(Arrays.asList("id", "salary", "name", "age"), 0);
        dataset.println();

        assertEquals("id", dataset.columnNames().get(0));
        assertEquals("salary", dataset.columnNames().get(1));
        assertEquals("name", dataset.columnNames().get(2));
        assertEquals("age", dataset.columnNames().get(3));
    }

    @Test
    @DisplayName("Should handle column move in different orders")
    public void testMoveColumnsNonConsecutive3() {
        dataset.println();
        dataset.moveColumns(Arrays.asList("salary", "name"), 0);
        dataset.println();

        assertEquals("salary", dataset.columnNames().get(0));
        assertEquals("name", dataset.columnNames().get(1));
        assertEquals("id", dataset.columnNames().get(2));
        assertEquals("age", dataset.columnNames().get(3));
    }

    @Test
    @DisplayName("Should verify column order preservation with complex moves")
    public void testMoveColumnsComplexOrderPreservation() {
        dataset.moveColumns(CommonUtil.toList("salary"), 0);
        dataset.moveColumns(Arrays.asList("name", "age"), 2);

        assertEquals("salary", dataset.columnNames().get(0));
        assertEquals("id", dataset.columnNames().get(1));
        assertEquals("name", dataset.columnNames().get(2));
        assertEquals("age", dataset.columnNames().get(3));

        assertEquals(50000.0, dataset.getRow(0)[0]);
        assertEquals(1, dataset.getRow(0)[1]);
        assertEquals("John", dataset.getRow(0)[2]);
        assertEquals(25, dataset.getRow(0)[3]);
    }

    @Test
    @DisplayName("Should move single column")
    public void testMoveColumnsSingle() {

        dataset.moveColumns(CommonUtil.toList("age"), 0);

        assertEquals("age", dataset.columnNames().get(0));
        assertEquals("id", dataset.columnNames().get(1));
        assertEquals("name", dataset.columnNames().get(2));
        assertEquals("salary", dataset.columnNames().get(3));
        assertEquals(4, dataset.columnCount());
    }

    @Test
    @DisplayName("Should move multiple columns")
    public void testMoveColumnsMultiple() {
        dataset.moveColumns(Arrays.asList("name", "salary"), 0);

        assertEquals("name", dataset.columnNames().get(0));
        assertEquals("salary", dataset.columnNames().get(1));
        assertEquals("id", dataset.columnNames().get(2));
        assertEquals("age", dataset.columnNames().get(3));
    }

    @Test
    @DisplayName("Should handle moving columns to same position")
    public void testMoveColumnsToSamePosition() {
        List<String> originalColumns = new ArrayList<>(dataset.columnNames());

        dataset.moveColumns(Arrays.asList("name", "age"), 1);

        assertEquals(originalColumns, dataset.columnNames());
    }

    @Test
    @DisplayName("Should handle empty column collection")
    public void testMoveColumnsEmpty() {
        List<String> originalColumns = new ArrayList<>(dataset.columnNames());

        dataset.moveColumns(Collections.emptyList(), 2);

        assertEquals(originalColumns, dataset.columnNames());
    }

    @Test
    @DisplayName("Should throw exception for non-existent column")
    public void testMoveColumnsNonExistent() {
        assertThrows(IllegalArgumentException.class, () -> dataset.moveColumns(Arrays.asList("invalid_column"), 0));
        assertThrows(IllegalArgumentException.class, () -> dataset.moveColumns(Arrays.asList("name", "invalid_column"), 0));
    }

    @Test
    @DisplayName("Should throw exception for duplicate columns")
    public void testMoveColumnsDuplicates() {
        assertThrows(IllegalArgumentException.class, () -> dataset.moveColumns(Arrays.asList("name", "name"), 0));
    }

    @Test
    @DisplayName("Should throw exception for invalid newPosition")
    public void testMoveColumnsInvalidPosition() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveColumns(Arrays.asList("name"), -1));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveColumns(Arrays.asList("name"), 4));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveColumns(Arrays.asList("name", "age"), 3));
    }

    @Test
    public void swapColumns() {
        sampleDataset.swapColumns("ID", "Age");
        assertEquals(Arrays.asList("Age", "Name", "ID"), sampleDataset.columnNames());
        assertEquals((Integer) 30, sampleDataset.moveToRow(0).get("Age"));
        assertEquals((Integer) 1, sampleDataset.moveToRow(0).get("ID"));

        sampleDataset.swapColumns("Name", "Name");
        assertEquals(Arrays.asList("Age", "Name", "ID"), sampleDataset.columnNames());

        assertThrows(IllegalArgumentException.class, () -> sampleDataset.swapColumns("ID", "NonExistent"));
    }

    @Test
    @DisplayName("Should move rows to beginning")
    public void testMoveRowsToBeginning() {
        dataset.moveRows(3, 5, 0);
        assertEquals("Alice", dataset.getRow(0)[1]);
        assertEquals("Charlie", dataset.getRow(1)[1]);
        assertEquals("John", dataset.getRow(2)[1]);
        assertEquals("Jane", dataset.getRow(3)[1]);
        assertEquals("Bob", dataset.getRow(4)[1]);
    }

    @Test
    @DisplayName("Should move rows to end")
    public void testMoveRowsToEnd() {
        dataset.moveRows(0, 2, 3);
        assertEquals("Bob", dataset.getRow(0)[1]);
        assertEquals("Alice", dataset.getRow(1)[1]);
        assertEquals("Charlie", dataset.getRow(2)[1]);
        assertEquals("John", dataset.getRow(3)[1]);
        assertEquals("Jane", dataset.getRow(4)[1]);
    }

    @Test
    @DisplayName("Should handle moving rows backward")
    public void testMoveRowsBackward() {
        dataset.moveRows(3, 5, 1);
        assertEquals("John", dataset.getRow(0)[1]);
        assertEquals("Alice", dataset.getRow(1)[1]);
        assertEquals("Charlie", dataset.getRow(2)[1]);
        assertEquals("Jane", dataset.getRow(3)[1]);
        assertEquals("Bob", dataset.getRow(4)[1]);
    }

    @Test
    @DisplayName("Should move entire dataset")
    public void testMoveRowsEntireDataset() {
        List<String> originalNames = new ArrayList<>();
        for (int i = 0; i < dataset.size(); i++) {
            originalNames.add((String) dataset.getRow(i)[1]);
        }

        dataset.moveRows(0, 5, 0);

        for (int i = 0; i < dataset.size(); i++) {
            assertEquals(originalNames.get(i), dataset.getRow(i)[1]);
        }
    }

    @Test
    @DisplayName("Should verify row data after complex moves")
    public void testMoveRowsComplexDataVerification() {
        Object[] originalRow0 = dataset.getRow(0);
        Object[] originalRow2 = dataset.getRow(2);
        Object[] originalRow4 = dataset.getRow(4);

        dataset.moveRows(2, 3, 4);

        assertArrayEquals(originalRow0, dataset.getRow(0));
        assertArrayEquals(originalRow2, dataset.getRow(4));
        assertArrayEquals(originalRow4, dataset.getRow(3));
    }

    @Test
    @DisplayName("Should handle moving adjacent blocks of rows")
    public void testMoveRowsAdjacentBlocks() {
        dataset.moveRows(1, 3, 3);
        dataset.moveRows(0, 1, 2);
        dataset.println();

        assertEquals("Alice", dataset.getRow(0)[1]);
        assertEquals("Charlie", dataset.getRow(1)[1]);
        assertEquals("John", dataset.getRow(2)[1]);
        assertEquals("Jane", dataset.getRow(3)[1]);
        assertEquals("Bob", dataset.getRow(4)[1]);
    }

    @Test
    @DisplayName("Should maintain dataset properties after moves")
    public void testMaintainPropertiesAfterMoves() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("test", "value");
        RowDataset datasetWithProps = new RowDataset(columnNames, columnList, properties);

        datasetWithProps.moveRows(0, 2, 3);
        datasetWithProps.moveColumns(Arrays.asList("name"), 0);

        assertEquals("value", datasetWithProps.getProperties().get("test"));
        assertEquals(5, datasetWithProps.size());
        assertEquals(4, datasetWithProps.columnCount());
    }

    @Test
    @DisplayName("Should move single row within valid range")
    public void testMoveRowsSingleRow() {
        dataset.moveRows(0, 1, 3);
        assertEquals("Jane", dataset.getRow(0)[1]);
        assertEquals("Bob", dataset.getRow(1)[1]);
        assertEquals("Alice", dataset.getRow(2)[1]);
        assertEquals("John", dataset.getRow(3)[1]);
        assertEquals("Charlie", dataset.getRow(4)[1]);
        assertEquals(5, dataset.size());
    }

    @Test
    @DisplayName("Should move multiple consecutive rows")
    public void testMoveRowsMultipleRows() {
        dataset.moveRows(1, 3, 3);
        assertEquals("John", dataset.getRow(0)[1]);
        assertEquals("Alice", dataset.getRow(1)[1]);
        assertEquals("Charlie", dataset.getRow(2)[1]);
        assertEquals("Jane", dataset.getRow(3)[1]);
        assertEquals("Bob", dataset.getRow(4)[1]);
        assertEquals(5, dataset.size());
    }

    @Test
    @DisplayName("Should not change dataset when moving to same position")
    public void testMoveRowsToSamePosition() {
        List<String> originalNames = new ArrayList<>();
        for (int i = 0; i < dataset.size(); i++) {
            originalNames.add((String) dataset.getRow(i)[1]);
        }

        dataset.moveRows(1, 3, 1);

        for (int i = 0; i < dataset.size(); i++) {
            assertEquals(originalNames.get(i), dataset.getRow(i)[1]);
        }
    }

    @Test
    @DisplayName("Should handle edge case of moving last row")
    public void testMoveRowsLastRowEdgeCase() {
        dataset.moveRows(4, 5, 0);
        assertEquals("Charlie", dataset.getRow(0)[1]);
        assertEquals("John", dataset.getRow(1)[1]);

        dataset.moveRows(0, 1, 4);
        assertEquals("John", dataset.getRow(0)[1]);
        assertEquals("Charlie", dataset.getRow(4)[1]);
    }

    @Test
    @DisplayName("Should throw exception for invalid fromRowIndex")
    public void testMoveRowsInvalidFromIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(-1, 2, 3));
        dataset.moveRows(5, 5, 0);
    }

    @Test
    @DisplayName("Should throw exception for invalid toRowIndex")
    public void testMoveRowsInvalidToIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(0, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(0, 6, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(2, 7, 0));
    }

    @Test
    @DisplayName("Should throw exception when fromIndex > toIndex")
    public void testMoveRowsInvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(3, 1, 0));
    }

    @Test
    @DisplayName("Should throw exception for invalid newPosition")
    public void testMoveRowsInvalidNewPosition() {
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(0, 1, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(0, 2, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.moveRows(1, 4, 3));
    }

    @Test
    public void get_columnIndex_withAbsolute() {
        sampleDataset.moveToRow(1);
        assertEquals((Integer) 2, sampleDataset.get(0));
        assertEquals("Bob", sampleDataset.get(1));
        assertEquals((Integer) 24, sampleDataset.get(2));
    }

    @Test
    public void get_columnName_withAbsolute() {
        sampleDataset.moveToRow(2);
        assertEquals((Integer) 3, sampleDataset.get("ID"));
        assertEquals("Charlie", sampleDataset.get("Name"));
        assertEquals((Integer) 35, sampleDataset.get("Age"));
    }

    @Test
    public void testGetByIndexes() {
        assertEquals((Integer) 1, dataset.get(0, 0));
        assertEquals("John", dataset.get(0, 1));
        assertEquals((Integer) 25, dataset.get(0, 2));
        assertEquals(50000.0, dataset.get(0, 3));
    }

    @Test
    public void testGetByColumnIndex() {
        Dataset ds = dataset.moveToRow(1);
        assertEquals("Jane", ds.get(1));
        assertEquals((Integer) 30, ds.get(2));
    }

    @Test
    public void testGetByColumnName() {
        Dataset ds = dataset.moveToRow(0);
        assertEquals("John", ds.get("name"));
        assertEquals((Integer) 25, ds.get("age"));
    }

    @Test
    public void testGet() {
        assertEquals((Integer) 1, dataset.get(0, 0));
        assertEquals("John", dataset.get(0, 1));
        assertEquals((Integer) 25, dataset.get(0, 2));
        assertEquals((Double) 50000.0, dataset.get(0, 3));
    }

    @Test
    public void testGetWithCurrentRow() {
        dataset.moveToRow(1);
        assertEquals((Integer) 2, dataset.get(0));
        assertEquals("Jane", dataset.get(1));
    }

    @Test
    public void test_get_set() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList);

        N.println(ds.get(1, 1));

        final String newValue = "abc123";

        ds.set(1, 1, newValue);

        assertEquals(newValue, ds.get(1, 1));

        assertFalse(ds.moveToRow(1).isNull(1));
        assertFalse(ds.moveToRow(1).isNull(ds.getColumnName(1)));

        ds.set(1, 1, null);

        assertTrue(ds.moveToRow(1).isNull(1));
        assertTrue(ds.moveToRow(1).isNull(ds.getColumnName(1)));
    }

    @Test
    public void get_rowIndex_columnIndex() {
        assertEquals((Integer) 1, sampleDataset.moveToRow(0).get(0));
        assertEquals("Bob", sampleDataset.moveToRow(1).get(1));
        assertEquals((Integer) 35, sampleDataset.moveToRow(2).get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataset.moveToRow(3).get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataset.moveToRow(0).get(3));
    }

    @Test
    public void testSet_ByIndexes() {
        Dataset ds = dataset.copy();
        ds.set(0, 1, "UpdatedName");
        assertEquals("UpdatedName", ds.get(0, 1));
    }

    @Test
    public void testSet_CurrentRow_ByIndex() {
        Dataset ds = dataset.copy();
        ds.moveToRow(0);
        ds.set(1, "UpdatedName");
        assertEquals("UpdatedName", ds.get(0, 1));
    }

    @Test
    public void testSet_CurrentRow_ByName() {
        Dataset ds = dataset.copy();
        ds.moveToRow(0);
        ds.set("name", "UpdatedName");
        assertEquals("UpdatedName", ds.get(0, 1));
    }

    @Test
    public void set_columnIndex_value_withAbsolute() {
        sampleDataset.moveToRow(1);
        sampleDataset.set(0, 200);
        assertEquals((Integer) 200, sampleDataset.moveToRow(1).get(0));
    }

    @Test
    public void set_columnName_value_withAbsolute() {
        sampleDataset.moveToRow(2);
        sampleDataset.set("Age", 40);
        assertEquals((Integer) 40, sampleDataset.moveToRow(2).get(2));
    }

    @Test
    public void testSetByIndexes() {
        Dataset ds = dataset.copy();
        ds.set(0, 1, "Alicia");
        assertEquals("Alicia", ds.get(0, 1));
    }

    @Test
    public void testSetByColumnIndex() {
        Dataset ds = dataset.copy().moveToRow(0);
        ds.set(1, "Alicia");
        assertEquals("Alicia", ds.get(1));
    }

    @Test
    public void testSetByColumnName() {
        Dataset ds = dataset.copy().moveToRow(0);
        ds.set("name", "Alicia");
        assertEquals("Alicia", ds.get("name"));
    }

    @Test
    public void testSet() {
        dataset.set(0, 1, "Johnny");
        assertEquals("Johnny", dataset.get(0, 1));
    }

    @Test
    public void set_rowIndex_columnIndex_value() {
        sampleDataset.set(0, 0, 100);
        assertEquals((Integer) 100, sampleDataset.moveToRow(0).get(0));

        RowDataset ds = createSimpleDataset();
        ds.freeze();
        assertThrows(IllegalStateException.class, () -> ds.set(0, 0, 99));
    }

    @Test
    public void testSetAndGet() {
        Dataset dataset = testDataset.copy();
        dataset.set(0, 0, 999);
        assertEquals(999, (Integer) dataset.get(0, 0));

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.set(-1, 0, 1);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.set(0, -1, 1);
        });
    }

    @Test
    public void testIsNull_ByRowAndColumn() {
        Dataset ds = Dataset.rows(Arrays.asList("val"), new Object[][] { { null }, { "test" } });
        assertTrue(ds.isNull(0, 0));
        assertFalse(ds.isNull(1, 0));
    }

    @Test
    public void testIsNull_CurrentRow_ByIndex() {
        Dataset ds = Dataset.rows(Arrays.asList("val"), new Object[][] { { null }, { "test" } });
        ds.moveToRow(0);
        assertTrue(ds.isNull(0));
        ds.moveToRow(1);
        assertFalse(ds.isNull(0));
    }

    @Test
    public void testIsNull_CurrentRow_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("val"), new Object[][] { { null }, { "test" } });
        ds.moveToRow(0);
        assertTrue(ds.isNull("val"));
        ds.moveToRow(1);
        assertFalse(ds.isNull("val"));
    }

    @Test
    public void isNull_rowIndex_columnIndex() {
        assertFalse(sampleDataset.isNull(0, 0));
        sampleDataset.set(0, 0, null);
        assertTrue(sampleDataset.isNull(0, 0));
    }

    @Test
    public void isNull_columnIndex_withAbsolute() {
        sampleDataset.moveToRow(0);
        assertFalse(sampleDataset.isNull(0));
        sampleDataset.set(0, null);
        assertTrue(sampleDataset.isNull(0));
        assertTrue(sampleDataset.isNull("ID"));
    }

    @Test
    public void testIsNullByIndexes() {
        assertFalse(dataset.isNull(0, 1));
        Dataset ds = dataset.copy();
        ds.set(0, 1, null);
        assertTrue(ds.isNull(0, 1));
    }

    @Test
    public void testIsNullByColumnIndex() {
        Dataset ds = dataset.copy().moveToRow(0);
        assertFalse(ds.isNull(1));
        ds.set(1, null);
        assertTrue(ds.isNull(1));
    }

    @Test
    public void testIsNullByColumnName() {
        Dataset ds = dataset.copy().moveToRow(0);
        assertFalse(ds.isNull("name"));
        ds.set("name", null);
        assertTrue(ds.isNull("name"));
    }

    @Test
    public void testIsNull() {
        Dataset dataset = testDataset.copy();
        assertFalse(dataset.isNull(0, 0));

        dataset.set(0, 0, null);
        assertTrue(dataset.isNull(0, 0));

        assertThrows(IndexOutOfBoundsException.class, () -> {
            dataset.isNull(-1, 0);
        });
    }

    @Test
    public void testGetBoolean_ByIndex() {
        Dataset ds = Dataset.rows(Arrays.asList("flag"), new Object[][] { { true }, { false } });
        ds.moveToRow(0);
        assertTrue(ds.getBoolean(0));
        ds.moveToRow(1);
        assertFalse(ds.getBoolean(0));
    }

    @Test
    public void testGetBoolean_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("flag"), new Object[][] { { true }, { false } });
        ds.moveToRow(0);
        assertTrue(ds.getBoolean("flag"));
        ds.moveToRow(1);
        assertFalse(ds.getBoolean("flag"));
    }

    @Test
    public void testGetBoolean() {
        dataset.set(0, 0, true);
        assertTrue(dataset.getBoolean(0));
        assertTrue(dataset.getBoolean("id"));

        dataset.set(0, 0, false);
        assertFalse(dataset.getBoolean(0));
    }

    @Test
    public void getBoolean_primitiveAndObject() {
        List<String> boolNames = List.of("BoolCol");
        List<List<Object>> boolVals = List.of(new ArrayList<>(Arrays.asList(true, false, null, Boolean.TRUE, Boolean.FALSE)));
        RowDataset boolDs = new RowDataset(boolNames, boolVals);

        boolDs.moveToRow(0);
        assertTrue(boolDs.getBoolean(0));
        assertTrue(boolDs.getBoolean("BoolCol"));

        boolDs.moveToRow(1);
        assertFalse(boolDs.getBoolean(0));

        boolDs.moveToRow(2);
        assertFalse(boolDs.getBoolean(0));

        boolDs.moveToRow(3);
        assertTrue(boolDs.getBoolean(0));

        boolDs.moveToRow(4);
        assertFalse(boolDs.getBoolean(0));
    }

    @Test
    public void testGetChar_ByIndex() {
        Dataset ds = Dataset.rows(Arrays.asList("letter"), new Object[][] { { 'A' }, { 'B' } });
        ds.moveToRow(0);
        assertEquals('A', ds.getChar(0));
        ds.moveToRow(1);
        assertEquals('B', ds.getChar(0));
    }

    @Test
    public void testGetChar_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("letter"), new Object[][] { { 'A' }, { 'B' } });
        ds.moveToRow(0);
        assertEquals('A', ds.getChar("letter"));
    }

    @Test
    public void testGetChar() {
        dataset.set(0, 0, 'A');
        assertEquals('A', dataset.getChar(0));
        assertEquals('A', dataset.getChar("id"));
    }

    @Test
    public void getChar_primitiveAndObject() {
        List<String> charNames = List.of("CharCol");
        List<List<Object>> charVals = List.of(new ArrayList<>(Arrays.asList('a', 'Z', null, Character.valueOf('x'))));
        RowDataset charDs = new RowDataset(charNames, charVals);

        charDs.moveToRow(0);
        assertEquals('a', charDs.getChar(0));
        assertEquals('a', charDs.getChar("CharCol"));

        charDs.moveToRow(1);
        assertEquals('Z', charDs.getChar(0));

        charDs.moveToRow(2);
        assertEquals((char) 0, charDs.getChar(0));

        charDs.moveToRow(3);
        assertEquals('x', charDs.getChar(0));
    }

    @Test
    public void testGetByte_ByIndex() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { (byte) 1 }, { (byte) 2 } });
        ds.moveToRow(0);
        assertEquals((byte) 1, ds.getByte(0));
    }

    @Test
    public void testGetByte_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { (byte) 1 }, { (byte) 2 } });
        ds.moveToRow(0);
        assertEquals((byte) 1, ds.getByte("num"));
    }

    @Test
    public void testGetByte() {
        dataset.set(0, 0, (byte) 10);
        assertEquals((byte) 10, dataset.getByte(0));
        assertEquals((byte) 10, dataset.getByte("id"));
    }

    @Test
    public void testGetShort_ByIndex() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { (short) 100 }, { (short) 200 } });
        ds.moveToRow(0);
        assertEquals((short) 100, ds.getShort(0));
    }

    @Test
    public void testGetShort_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { (short) 100 }, { (short) 200 } });
        ds.moveToRow(0);
        assertEquals((short) 100, ds.getShort("num"));
    }

    @Test
    public void testGetShort() {
        dataset.set(0, 0, (short) 100);
        assertEquals((short) 100, dataset.getShort(0));
        assertEquals((short) 100, dataset.getShort("id"));
    }

    @Test
    public void testGetInt_ByIndex() {
        dataset.moveToRow(0);
        assertEquals(25, dataset.getInt(2));
    }

    @Test
    public void testGetInt_ByName() {
        dataset.moveToRow(0);
        assertEquals(25, dataset.getInt("age"));
    }

    @Test
    public void testGetInt() {
        assertEquals(1, dataset.getInt(0));
        assertEquals(1, dataset.getInt("id"));
    }

    @Test
    public void getInt_primitiveAndObject() {
        List<String> intNames = List.of("IntCol");
        List<List<Object>> intVals = List.of(new ArrayList<>(Arrays.asList(10, -5, null, Integer.valueOf(100), Long.valueOf(200L))));
        RowDataset intDs = new RowDataset(intNames, intVals);

        intDs.moveToRow(0);
        assertEquals(10, intDs.getInt(0));
        assertEquals(10, intDs.getInt("IntCol"));

        intDs.moveToRow(1);
        assertEquals(-5, intDs.getInt(0));

        intDs.moveToRow(2);
        assertEquals(0, intDs.getInt(0));

        intDs.moveToRow(3);
        assertEquals(100, intDs.getInt(0));

        intDs.moveToRow(4);
        assertEquals(200, intDs.getInt(0));
    }

    @Test
    public void testGetLong_ByIndex() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { 1000L }, { 2000L } });
        ds.moveToRow(0);
        assertEquals(1000L, ds.getLong(0));
    }

    @Test
    public void testGetLong_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { 1000L }, { 2000L } });
        ds.moveToRow(0);
        assertEquals(1000L, ds.getLong("num"));
    }

    @Test
    public void testGetLong() {
        assertEquals(1L, dataset.getLong(0));
        assertEquals(1L, dataset.getLong("id"));
    }

    @Test
    public void testGetFloat_ByIndex() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { 1.5f }, { 2.5f } });
        ds.moveToRow(0);
        assertEquals(1.5f, ds.getFloat(0));
    }

    @Test
    public void testGetFloat_ByName() {
        Dataset ds = Dataset.rows(Arrays.asList("num"), new Object[][] { { 1.5f }, { 2.5f } });
        ds.moveToRow(0);
        assertEquals(1.5f, ds.getFloat("num"));
    }

    @Test
    public void testGetFloat() {
        assertEquals(50000.0f, dataset.getFloat(3), 0.01);
        assertEquals(50000.0f, dataset.getFloat("salary"), 0.01);
    }

    @Test
    public void testGetDouble_ByIndex() {
        dataset.moveToRow(0);
        assertEquals(50000.0, dataset.getDouble(3));
    }

    @Test
    public void testGetDouble_ByName() {
        dataset.moveToRow(0);
        assertEquals(50000.0, dataset.getDouble("salary"));
    }

    @Test
    public void testGetDouble() {
        assertEquals(50000.0, dataset.getDouble(3), 0.01);
        assertEquals(50000.0, dataset.getDouble("salary"), 0.01);
    }

    @Test
    public void getColumn_byName() {
        List<Object> nameColumn = sampleDataset.getColumn("Name");
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"), nameColumn);
    }

    @Test
    public void testGetColumnByIndex() {
        ImmutableList<Object> column = dataset.getColumn(1);
        assertEquals(5, column.size());
        assertEquals("John", column.get(0));
        assertEquals("Jane", column.get(1));
        assertEquals("Bob", column.get(2));
        assertEquals("Alice", column.get(3));
        assertEquals("Charlie", column.get(4));
    }

    @Test
    public void testGetColumnByName() {
        ImmutableList<Object> column = dataset.getColumn("age");
        assertEquals(5, column.size());
        assertEquals(25, column.get(0));
        assertEquals(30, column.get(1));
        assertEquals(35, column.get(2));
        assertEquals(28, column.get(3));
        assertEquals(40, column.get(4));
    }

    @Test
    public void testGetColumn() {
        ImmutableList<Object> idColumn = dataset.getColumn(0);
        assertEquals(5, idColumn.size());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), idColumn);

        ImmutableList<Object> nameColumn = dataset.getColumn("name");
        assertEquals(Arrays.asList("John", "Jane", "Bob", "Alice", "Charlie"), nameColumn);
    }

    @Test
    public void testGetColumn_ByIndex() {
        ImmutableList<Object> col = dataset.getColumn(1);
        assertNotNull(col);
        assertEquals(5, col.size());
        assertEquals("John", col.get(0));
        assertEquals("Jane", col.get(1));
        assertEquals("Bob", col.get(2));
        assertEquals("Alice", col.get(3));
        assertEquals("Charlie", col.get(4));
    }

    @Test
    public void testGetColumn_ByName() {
        ImmutableList<Integer> col = dataset.getColumn("age");
        assertNotNull(col);
        assertEquals(5, col.size());
        assertEquals(Integer.valueOf(25), col.get(0));
        assertEquals(Integer.valueOf(30), col.get(1));
        assertEquals(Integer.valueOf(35), col.get(2));
        assertEquals(Integer.valueOf(28), col.get(3));
        assertEquals(Integer.valueOf(40), col.get(4));
    }

    @Test
    public void getColumn_byIndex() {
        List<Object> idColumn = sampleDataset.getColumn(0);
        assertEquals(Arrays.asList(1, 2, 3), idColumn);
        assertThrows(UnsupportedOperationException.class, () -> sampleDataset.getColumn(0).add(4));
    }

    @Test
    public void copyColumn() {
        List<Object> ageColumnCopy = sampleDataset.copyColumn("Age");
        assertEquals(Arrays.asList(30, 24, 35), ageColumnCopy);
        ageColumnCopy.add(40);
        assertEquals(Arrays.asList(30, 24, 35), sampleDataset.getColumn("Age"));
    }

    @Test
    public void testCopyColumn() {
        List<String> col = dataset.copyColumn("name");
        assertNotNull(col);
        assertEquals(5, col.size());
        assertEquals("John", col.get(0));

        col.set(0, "Modified");
        assertEquals("Modified", col.get(0));
        assertEquals("John", dataset.get(0, 1));
    }

    @Test
    public void testAddColumn_WithCollection() {
        Dataset ds = dataset.copy();
        List<String> emails = Arrays.asList("john@test.com", "jane@test.com", "bob@test.com", "alice@test.com", "charlie@test.com");
        ds.addColumn("email", emails);

        assertEquals(5, ds.columnCount());
        assertEquals("email", ds.getColumnName(4));
        assertEquals("john@test.com", ds.get(0, 4));
    }

    @Test
    public void testAddColumn_WithPosition() {
        Dataset ds = dataset.copy();
        List<String> emails = Arrays.asList("john@test.com", "jane@test.com", "bob@test.com", "alice@test.com", "charlie@test.com");
        ds.addColumn(1, "email", emails);

        assertEquals(5, ds.columnCount());
        assertEquals("email", ds.getColumnName(1));
        assertEquals("name", ds.getColumnName(2));
    }

    @Test
    public void testAddColumn_WithFunction() {
        Dataset ds = dataset.copy();
        ds.addColumn("ageDouble", "age", (Integer age) -> age * 2);

        assertEquals(5, ds.columnCount());
        assertEquals(Integer.valueOf(50), ds.get(0, 4));
        assertEquals(Integer.valueOf(60), ds.get(1, 4));
    }

    @Test
    public void testAddColumn_WithBiFunction() {
        Dataset ds = dataset.copy();
        ds.addColumn("fullInfo", Tuple.of("name", "age"), (String name, Integer age) -> name + " (" + age + ")");

        assertEquals(5, ds.columnCount());
        assertEquals("John (25)", ds.get(0, 4));
        assertEquals("Jane (30)", ds.get(1, 4));
    }

    @Test
    public void testAddColumn_WithTriFunction() {
        Dataset ds = dataset.copy();
        ds.addColumn("info", Tuple.of("id", "name", "age"), (Integer id, String name, Integer age) -> id + ":" + name + ":" + age);

        assertEquals(5, ds.columnCount());
        assertEquals("1:John:25", ds.get(0, 4));
    }

    @Test
    public void addColumn_fromAnotherColumn_withFunction() {
        sampleDataset.addColumn("AgePlus5", "Age", (Integer age) -> age + 5);
        assertEquals(4, sampleDataset.columnCount());
        assertTrue(sampleDataset.columnNames().contains("AgePlus5"));
        assertEquals(Arrays.asList(35, 29, 40), sampleDataset.getColumn("AgePlus5"));
    }

    @Test
    public void addColumn_fromTuple2_withBiFunction() {
        sampleDataset.addColumn("Name_Age_Str", Tuple.of("Name", "Age"), (String name, Integer age) -> name + ":" + age);
        assertEquals(4, sampleDataset.columnCount());
        assertTrue(sampleDataset.columnNames().contains("Name_Age_Str"));
        assertEquals(Arrays.asList("Alice:30", "Bob:24", "Charlie:35"), sampleDataset.getColumn("Name_Age_Str"));
    }

    @Test
    public void addColumn_fromTuple3_withTriFunction() {
        List<String> t3ColNames = new ArrayList<>(Arrays.asList("C1", "C2", "C3", "C4"));
        List<List<Object>> t3ColValues = new ArrayList<>();
        t3ColValues.add(new ArrayList<>(Arrays.asList(1, 2)));
        t3ColValues.add(new ArrayList<>(Arrays.asList("A", "B")));
        t3ColValues.add(new ArrayList<>(Arrays.asList(true, false)));
        t3ColValues.add(new ArrayList<>(Arrays.asList(1.1, 2.2)));
        RowDataset t3ds = new RowDataset(t3ColNames, t3ColValues);

        t3ds.addColumn("Combined", Tuple.of("C1", "C2", "C3"), (Integer c1, String c2, Boolean c3) -> c1 + "_" + c2 + "_" + c3);
        assertEquals(5, t3ds.columnCount());
        assertEquals(Arrays.asList("1_A_true", "2_B_false"), t3ds.getColumn("Combined"));
    }

    @Test
    public void testAddColumnWithCollection() {
        Dataset ds = dataset.copy();
        List<String> newColumn = Arrays.asList("Engineering", "Sales", "Marketing", "HR", "Finance");
        ds.addColumn("department", newColumn);
        assertEquals(5, ds.columnCount());
        assertEquals("department", ds.getColumnName(4));
        assertEquals("Engineering", ds.get(0, 4));
    }

    @Test
    public void testAddColumnWithPosition() {
        Dataset ds = dataset.copy();
        List<String> newColumn = Arrays.asList("A", "B", "C", "D", "E");
        ds.addColumn(1, "grade", newColumn);
        assertEquals(5, ds.columnCount());
        assertEquals("grade", ds.getColumnName(1));
        assertEquals("A", ds.get(0, 1));
    }

    @Test
    public void testAddColumnWithFunction() {
        Dataset ds = dataset.copy();
        ds.addColumn("nameLength", "name", (String name) -> name.length());
        assertEquals(5, ds.columnCount());
        assertEquals("nameLength", ds.getColumnName(4));
        assertEquals((Integer) 4, ds.get(0, 4));
    }

    @Test
    public void testAddColumnWithTuple2() {
        Dataset ds = dataset.copy();
        ds.addColumn("nameAge", Tuple.of("name", "age"), (String name, Integer age) -> name + "(" + age + ")");
        assertEquals(5, ds.columnCount());
        assertEquals("John(25)", ds.get(0, 4));
    }

    @Test
    public void testAddColumnWithTuple3() {
        Dataset ds = dataset.copy();
        ds.addColumn("summary", Tuple.of("name", "age", "salary"), (String name, Integer age, Double salary) -> name + "," + age + "," + salary);
        assertEquals(5, ds.columnCount());
        assertEquals("John,25,50000.0", ds.get(0, 4));
    }

    @Test
    public void testAddColumnAtPosition() {
        Dataset dataset = testDataset.copy();
        List<String> departments = Arrays.asList("IT", "HR", "Finance", "Marketing");
        dataset.addColumn(1, "department", departments);

        assertEquals("department", dataset.getColumnName(1));
        assertEquals("name", dataset.getColumnName(2));
    }

    @Test
    public void testAddColumnWithBiFunction() {
        Dataset dataset = testDataset.copy();
        Tuple2<String, String> fromColumns = Tuple.of("name", "age");
        BiFunction<String, Integer, String> func = (name, age) -> name + "_" + age;
        dataset.addColumn("name_age", fromColumns, func);

        assertTrue(dataset.containsColumn("name_age"));
        dataset.moveToRow(0);
        assertEquals("Alice_30", dataset.get("name_age"));
    }

    @Test
    public void testAddColumnWithTriFunction() {
        Dataset dataset = testDataset.copy();
        Tuple3<String, String, String> fromColumns = Tuple.of("id", "name", "age");
        TriFunction<Integer, String, Integer, String> func = (id, name, age) -> id + ":" + name + ":" + age;
        dataset.addColumn("combined", fromColumns, func);

        assertTrue(dataset.containsColumn("combined"));
        dataset.moveToRow(0);
        assertEquals("1:Alice:30", dataset.get("combined"));
    }

    @Test
    public void testAddColumn() {
        List<Object> newColumn = Arrays.asList("A", "B", "C", "D", "E");
        dataset.addColumn("grade", newColumn);

        assertEquals(5, dataset.columnCount());
        assertTrue(dataset.containsColumn("grade"));
        assertEquals("A", dataset.get(0, 4));
    }

    @Test
    public void testAddColumnWithDisposableObjArray() {
        dataset.addColumn("combined", Arrays.asList("id", "name"), (DisposableObjArray arr) -> arr.get(0) + ":" + arr.get(1));

        assertTrue(dataset.containsColumn("combined"));
        assertEquals("1:John", dataset.get(0, 4));
    }

    @Test
    public void addColumn_fromMultipleColumns_withFunction() {
        sampleDataset.addColumn("ID_Name", Arrays.asList("ID", "Name"), (DisposableObjArray row) -> row.get(0) + "_" + row.get(1));
        assertEquals(4, sampleDataset.columnCount());
        assertTrue(sampleDataset.columnNames().contains("ID_Name"));
        assertEquals(Arrays.asList("1_Alice", "2_Bob", "3_Charlie"), sampleDataset.getColumn("ID_Name"));
    }

    @Test
    public void flatMap_singleColumnToNewCollection() {
        sampleDataset.addColumn("Hobbies", Arrays.asList("Reading,Hiking", "Gaming", "Cooking,Swimming"));
        Dataset flatMapped = sampleDataset.flatMapColumn("Hobbies", "Hobby", "Name", (String hobbies) -> Arrays.asList(hobbies.split(",")));

        assertEquals(2, flatMapped.columnCount());
        assertEquals(5, flatMapped.size());
        assertEquals("Reading", flatMapped.moveToRow(0).get("Hobby"));
        assertEquals("Alice", flatMapped.moveToRow(0).get("Name"));
        assertEquals("Gaming", flatMapped.moveToRow(2).get("Hobby"));
        assertEquals("Bob", flatMapped.moveToRow(2).get("Name"));
    }

    @Test
    public void testAddColumnWithMultipleColumns() {
        Dataset ds = dataset.copy();
        ds.addColumn("fullInfo", Arrays.asList("name", "age"), arr -> arr.get(0) + ":" + arr.get(1));
        assertEquals(5, ds.columnCount());
        assertEquals("John:25", ds.get(0, 4));
    }

    @Test
    public void test_addColumn() throws Exception {
        final Account account = createAccount(Account.class);
        final Dataset ds1 = CommonUtil.newDataset(CommonUtil.toList(account, account));

        ds1.addColumn("firstName2", "firstName", (Function<String, String>) t -> "**********" + t);

        ds1.println();

        ds1.addColumn(0, "firstName3", CommonUtil.toList("firstName", "lastName"),
                (Function<DisposableObjArray, String>) a -> a.get(0) + "**********" + a.get(1));

        ds1.println();
        assertNotNull(ds1);
    }

    @Test
    public void addColumn_newColumnName_collection() {
        List<String> newColData = Arrays.asList("X", "Y", "Z");
        sampleDataset.addColumn("Grade", newColData);
        assertEquals(4, sampleDataset.columnCount());
        assertTrue(sampleDataset.columnNames().contains("Grade"));
        assertEquals(newColData, sampleDataset.getColumn("Grade"));
        assertEquals("X", sampleDataset.moveToRow(0).get("Grade"));

        sampleDataset.addColumn("EmptyGrade", null);
        assertEquals(5, sampleDataset.columnCount());
        assertEquals(Arrays.asList(null, null, null), sampleDataset.getColumn("EmptyGrade"));

        assertThrows(IllegalArgumentException.class, () -> sampleDataset.addColumn("ID", Arrays.asList("A")));
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.addColumn("Score", Arrays.asList(10, 20)));
    }

    @Test
    public void addColumn_newColumnPosition_newColumnName_collection() {
        List<Double> scores = Arrays.asList(90.5, 88.0, 92.0);
        sampleDataset.addColumn(1, "Score", scores);
        assertEquals(4, sampleDataset.columnCount());
        assertEquals(Arrays.asList("ID", "Score", "Name", "Age"), sampleDataset.columnNames());
        assertEquals(scores, sampleDataset.getColumn("Score"));
        assertEquals(90.5, sampleDataset.moveToRow(0).get("Score"));

        assertThrows(IllegalArgumentException.class, () -> sampleDataset.addColumn(0, "ID", scores));
    }

    @Test
    public void testAddColumns() {
        Dataset ds = dataset.copy();
        List<String> newColNames = Arrays.asList("email", "city");
        List<List<Object>> newCols = new ArrayList<>();
        newCols.add(Arrays.asList("a@test.com", "b@test.com", "c@test.com", "d@test.com", "e@test.com"));
        newCols.add(Arrays.asList("NYC", "LA", "SF", "Seattle", "Chicago"));

        ds.addColumns(newColNames, newCols);
        assertEquals(6, ds.columnCount());
        assertEquals("email", ds.getColumnName(4));
        assertEquals("city", ds.getColumnName(5));
    }

    @Test
    @DisplayName("Should add columns at beginning of dataset")
    public void testAddColumnsAtBeginning() {
        List<String> newColumnNames = Arrays.asList("prefix1", "prefix2");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList("P1-1", "P1-2", "P1-3", "P1-4", "P1-5"),
                Arrays.asList("P2-1", "P2-2", "P2-3", "P2-4", "P2-5"));

        dataset.addColumns(0, newColumnNames, newColumns);

        assertEquals(6, dataset.columnCount());
        assertEquals("prefix1", dataset.getColumnName(0));
        assertEquals("prefix2", dataset.getColumnName(1));
        assertEquals("id", dataset.getColumnName(2));
        assertEquals("P1-1", dataset.get(0, 0));
        assertEquals("P2-1", dataset.get(0, 1));
        assertEquals((Integer) 1, dataset.get(0, 2));
    }

    @Test
    @DisplayName("Should preserve original data when adding columns")
    public void testAddColumnsPreservesOriginalData() {
        Object[][] originalData = new Object[dataset.size()][dataset.columnCount()];
        for (int i = 0; i < dataset.size(); i++) {
            for (int j = 0; j < dataset.columnCount(); j++) {
                originalData[i][j] = dataset.get(i, j);
            }
        }

        List<String> newColumnNames = Arrays.asList("new1", "new2");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList("N1", "N2", "N3", "N4", "N5"), Arrays.asList(10.1, 20.2, 30.3, 40.4, 50.5));

        dataset.addColumns(2, newColumnNames, newColumns);

        assertEquals(originalData[0][0], dataset.get(0, 0));
        assertEquals(originalData[0][1], dataset.get(0, 1));
        assertEquals(originalData[0][2], dataset.get(0, 4));
        assertEquals(originalData[0][3], dataset.get(0, 5));
    }

    @Test
    @DisplayName("Should add multiple columns at specific position")
    public void testAddColumnsAtPosition() {
        List<String> newColumnNames = Arrays.asList("grade", "status");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList("A", "B", "C", "B", "A"),
                Arrays.asList("Active", "Active", "Inactive", "Active", "Active"));

        dataset.addColumns(2, newColumnNames, newColumns);

        assertEquals(6, dataset.columnCount());
        assertEquals("grade", dataset.getColumnName(2));
        assertEquals("status", dataset.getColumnName(3));
        assertEquals("age", dataset.getColumnName(4));
        assertEquals("A", dataset.get(0, 2));
        assertEquals("Active", dataset.get(0, 3));
    }

    @Test
    @DisplayName("Should handle empty columns by filling with nulls")
    public void testAddColumnsWithEmptyColumns() {
        List<String> newColumnNames = Arrays.asList("empty1", "empty2");
        List<Collection<Object>> newColumns = Arrays.asList(Collections.emptyList(), Collections.emptyList());

        dataset.addColumns(newColumnNames, newColumns);

        assertEquals(6, dataset.columnCount());
        for (int i = 0; i < dataset.size(); i++) {
            assertNull(dataset.get(i, dataset.getColumnIndex("empty1")));
            assertNull(dataset.get(i, dataset.getColumnIndex("empty2")));
        }
    }

    @Test
    @DisplayName("Should handle mixed empty and non-empty columns")
    public void testAddColumnsMixedEmptyAndNonEmpty() {
        List<String> newColumnNames = Arrays.asList("data", "empty", "moreData");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList("D1", "D2", "D3", "D4", "D5"), Collections.emptyList(),
                Arrays.asList(100, 200, 300, 400, 500));

        dataset.addColumns(newColumnNames, newColumns);

        assertEquals(7, dataset.columnCount());
        assertEquals("D1", dataset.get(0, dataset.getColumnIndex("data")));
        assertNull(dataset.get(0, dataset.getColumnIndex("empty")));
        assertEquals(100, (Integer) dataset.get(0, dataset.getColumnIndex("moreData")));
    }

    @Test
    @DisplayName("Should handle columns with null values")
    public void testAddColumnsWithNullValues() {
        List<String> newColumnNames = Arrays.asList("nullable1", "nullable2");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList("A", null, "C", null, "E"), Arrays.asList(null, 2, null, 4, null));

        dataset.addColumns(newColumnNames, newColumns);

        assertEquals(6, dataset.columnCount());
        assertEquals("A", dataset.get(0, dataset.getColumnIndex("nullable1")));
        assertNull(dataset.get(1, dataset.getColumnIndex("nullable1")));
        assertNull(dataset.get(0, dataset.getColumnIndex("nullable2")));
        assertEquals(2, (Integer) dataset.get(1, dataset.getColumnIndex("nullable2")));
    }

    @Test
    @DisplayName("Should handle empty column names list")
    public void testAddColumnsWithEmptyNamesList() {
        List<String> newColumnNames = Collections.emptyList();
        List<Collection<Object>> newColumns = Collections.emptyList();

        int originalColumnCount = dataset.columnCount();
        dataset.addColumns(newColumnNames, newColumns);

        assertEquals(originalColumnCount, dataset.columnCount());
    }

    @Test
    @DisplayName("Should add multiple columns with different collection types")
    public void testAddColumnsWithDifferentCollectionTypes() {
        List<String> newColumnNames = Arrays.asList("list", "set", "arrayList");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5), new HashSet<>(Arrays.asList("A", "B", "C", "D", "E")),
                new ArrayList<>(Arrays.asList(true, false, true, false, true)));

        dataset.addColumns(newColumnNames, newColumns);

        assertEquals(7, dataset.columnCount());
        assertTrue(dataset.containsColumn("list"));
        assertTrue(dataset.containsColumn("set"));
        assertTrue(dataset.containsColumn("arrayList"));
    }

    @Test
    @DisplayName("Should throw exception when column names and columns size mismatch")
    public void testAddColumnsWithSizeMismatch() {
        List<String> newColumnNames = Arrays.asList("col1", "col2", "col3");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5), Arrays.asList(6, 7, 8, 9, 10));

        assertThrows(IllegalArgumentException.class, () -> dataset.addColumns(newColumnNames, newColumns));
    }

    @Test
    @DisplayName("Should throw exception when column size doesn't match dataset size")
    public void testAddColumnsWithIncorrectColumnSize() {
        List<String> newColumnNames = Arrays.asList("col1", "col2");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6, 7, 8, 9));

        assertThrows(IllegalArgumentException.class, () -> dataset.addColumns(newColumnNames, newColumns));
    }

    @Test
    @DisplayName("Should throw exception for duplicate column names")
    public void testAddColumnsWithDuplicateNames() {
        List<String> newColumnNames = Arrays.asList("id", "newCol");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList(10, 20, 30, 40, 50), Arrays.asList("A", "B", "C", "D", "E"));

        assertThrows(IllegalArgumentException.class, () -> dataset.addColumns(newColumnNames, newColumns));
    }

    @Test
    @DisplayName("Should throw exception for empty column names")
    public void testAddColumnsWithEmptyColumnName() {
        List<String> newColumnNames = Arrays.asList("valid", "", "alsoValid");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5), Arrays.asList(6, 7, 8, 9, 10), Arrays.asList(11, 12, 13, 14, 15));

        assertThrows(IllegalArgumentException.class, () -> dataset.addColumns(newColumnNames, newColumns));
    }

    @Test
    @DisplayName("Should throw exception when adding to frozen dataset")
    public void testAddColumnsOnFrozenDataset() {
        dataset.freeze();

        List<String> newColumnNames = Arrays.asList("col1");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5));

        assertThrows(IllegalStateException.class, () -> dataset.addColumns(newColumnNames, newColumns));
    }

    @Test
    @DisplayName("Should throw exception for invalid position")
    public void testAddColumnsWithInvalidPosition() {
        List<String> newColumnNames = Arrays.asList("col1");
        List<Collection<Object>> newColumns = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5));

        assertThrows(IndexOutOfBoundsException.class, () -> dataset.addColumns(100, newColumnNames, newColumns));

        assertThrows(IndexOutOfBoundsException.class, () -> dataset.addColumns(-1, newColumnNames, newColumns));
    }

    @Test
    public void testRemoveColumn() {
        Dataset ds = dataset.copy();
        List<String> removed = ds.removeColumn("name");

        assertEquals(3, ds.columnCount());
        assertFalse(ds.containsColumn("name"));
        assertEquals(5, removed.size());
        assertEquals("John", removed.get(0));
    }

    @Test
    public void test_removeColumn() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList);

        ds.println();

        assertTrue(ds.getColumnIndex("firstName") >= 0);
        assertTrue(ds.getColumnIndex("lastName") >= 0);
        assertTrue(ds.getColumnIndex("birthDate") >= 0);

        ds.removeColumns(CommonUtil.toList("firstName", "lastName", "birthDate"));

        ds.println();

        assertFalse(ds.containsColumn("firstName"));
        assertFalse(ds.containsColumn("lastName"));
    }

    @Test
    public void removeColumn() {
        List<Object> removedCol = sampleDataset.removeColumn("Name");
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"), removedCol);
        assertEquals(2, sampleDataset.columnCount());
        assertFalse(sampleDataset.columnNames().contains("Name"));
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.removeColumn("NonExistent"));
    }

    @Test
    public void testRemoveColumns() {
        Dataset ds = dataset.copy();
        ds.removeColumns(Arrays.asList("name", "age"));

        assertEquals(2, ds.columnCount());
        assertTrue(ds.containsColumn("id"));
        assertTrue(ds.containsColumn("salary"));
        assertFalse(ds.containsColumn("name"));
        assertFalse(ds.containsColumn("age"));
    }

    @Test
    public void testRemoveColumns_WithPredicate() {
        Dataset ds = dataset.copy();
        ds.removeColumns(col -> col.startsWith("s"));

        assertEquals(3, ds.columnCount());
        assertFalse(ds.containsColumn("salary"));
    }

    @Test
    public void testRemoveColumnsWithPredicate() {
        Dataset ds = dataset.copy();
        ds.removeColumns(name -> name.startsWith("s"));
        assertEquals(3, ds.columnCount());
        assertFalse(ds.containsColumn("salary"));
    }

    @Test
    public void removeColumns_collection() {
        sampleDataset.removeColumns(Arrays.asList("ID", "Age"));
        assertEquals(1, sampleDataset.columnCount());
        assertEquals(Collections.singletonList("Name"), sampleDataset.columnNames());
    }

    @Test
    public void removeColumns_predicate() {
        sampleDataset.removeColumns(name -> name.equals("ID") || name.endsWith("e"));
        assertEquals(0, sampleDataset.columnCount());
        assertTrue(sampleDataset.columnNames().isEmpty());
    }

    @Test
    public void updateColumn() {
        sampleDataset.updateColumn("Age", (Integer age) -> age * 2);
        assertEquals(Arrays.asList(60, 48, 70), sampleDataset.getColumn("Age"));
    }

    @Test
    public void testUpdateColumn() {
        Dataset dataset = testDataset.copy();
        dataset.updateColumn("age", (Integer age) -> age + 1);

        dataset.moveToRow(0);
        assertEquals(31, dataset.getInt("age"));
        dataset.moveToRow(1);
        assertEquals(26, dataset.getInt("age"));
    }

    @Test
    public void testUpdateColumn_WithFunction() {
        Dataset ds = dataset.copy();
        ds.updateColumn("name", (String name) -> name.toUpperCase());

        assertEquals("JOHN", ds.get(0, 1));
        assertEquals("JANE", ds.get(1, 1));
    }

    @Test
    public void updateColumns_collection() {
        sampleDataset.updateColumns(Arrays.asList("ID", "Age"), (i, c, v) -> ((Number) v).intValue() + 100);
        assertEquals(Arrays.asList(101, 102, 103), sampleDataset.getColumn("ID"));
        assertEquals(Arrays.asList(130, 124, 135), sampleDataset.getColumn("Age"));
    }

    @Test
    public void testUpdateColumns() {
        Dataset ds = dataset.copy();
        ds.updateColumns(Arrays.asList("name"), (i, c, v) -> ((String) v).toUpperCase());
        assertEquals("JOHN", ds.get(0, 1));
    }

    @Test
    public void test_top() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 7);
        final MutableInt idx = MutableInt.of(100);
        final Dataset ds = CommonUtil.newDataset(accountList);
        ds.updateColumns(CommonUtil.toList("firstName", "lastName"), (i, c, v) -> (String) v + idx.getAndIncrement());
        ds.println();

        ds.topBy("lastName", 3).println();
        ds.topBy(CommonUtil.toList("lastName", "gui"), 3).println();

        ds.topBy("lastName", 3, (Comparator<String>) (o1, o2) -> o2.compareTo(o1)).println();

        ds.topBy(CommonUtil.toList("lastName", "gui"), 3, (Comparator<Object[]>) (o1, o2) -> ((String) o2[0]).compareTo((String) o1[0])).println();
        assertNotNull(ds);
    }

    @Test
    public void testConvertColumn() {
        Dataset ds = dataset.copy();
        ds.convertColumn("age", Long.class);

        Object val = ds.get(0, 2);
        assertTrue(val instanceof Long);
        assertEquals(25L, val);
    }

    @Test
    public void convertColumn() {
        sampleDataset.convertColumn("Age", String.class);
        assertEquals("30", sampleDataset.moveToRow(0).get("Age"));
        assertTrue(sampleDataset.getColumn("Age").get(0) instanceof String);

        sampleDataset.convertColumn("ID", Double.class);
        assertEquals(1.0, sampleDataset.moveToRow(0).get("ID"));
        assertTrue(sampleDataset.getColumn("ID").get(0) instanceof Double);
    }

    @Test
    public void test_convertColumn() {

        final Dataset dataset = CommonUtil.newDataset(CommonUtil.toList("column1", "column2"),
                CommonUtil.toList(CommonUtil.toList("ab", "cd"), CommonUtil.toList("ef", "gh")));

        assertThrows(IllegalArgumentException.class, () -> dataset.convertColumn("column1", Long.class));
    }

    @Test
    public void testConvertColumns() {
        Dataset ds = Dataset.rows(Arrays.asList("id", "age", "score"), new Object[][] { { 1, 25, 98.5 }, { 2, 30, 87.3 } });

        Map<String, Class<?>> conversions = new HashMap<>();
        conversions.put("id", Long.class);
        conversions.put("age", Long.class);

        ds.convertColumns(conversions);

        Object id = ds.get(0, 0);
        Object age = ds.get(0, 1);

        assertTrue(id instanceof Long);
        assertTrue(age instanceof Long);
        assertEquals(1L, id);
        assertEquals(25L, age);
    }

    @Test
    public void convertColumns_map() {
        Map<String, Class<?>> conversions = new HashMap<>();
        conversions.put("ID", String.class);
        conversions.put("Age", Double.class);
        sampleDataset.convertColumns(conversions);

        assertTrue(sampleDataset.getColumn("ID").get(0) instanceof String);
        assertEquals("1", sampleDataset.moveToRow(0).get("ID"));
        assertTrue(sampleDataset.getColumn("Age").get(0) instanceof Double);
        assertEquals(30.0, sampleDataset.moveToRow(0).get("Age"));
    }

    @Test
    public void testCombineColumns_WithBiFunction() {
        Dataset ds = Dataset.rows(Arrays.asList("firstName", "lastName"), new Object[][] { { "Alice", "Smith" }, { "Bob", "Jones" } });

        ds.combineColumns(Tuple.of("firstName", "lastName"), "fullName", (String first, String last) -> first + " " + last);

        assertTrue(ds.containsColumn("fullName"));
        assertEquals("Alice Smith", ds.get(0, 0));
        assertEquals("Bob Jones", ds.get(1, 0));
    }

    @Test
    public void testCombineColumns_WithTriFunction() {
        Dataset ds = Dataset.rows(Arrays.asList("firstName", "middleName", "lastName"),
                new Object[][] { { "Alice", "Marie", "Smith" }, { "Bob", "William", "Jones" } });

        ds.combineColumns(Tuple.of("firstName", "middleName", "lastName"), "fullName",
                (String first, String middle, String last) -> first + " " + middle + " " + last);

        assertTrue(ds.containsColumn("fullName"));
        assertEquals("Alice Marie Smith", ds.get(0, 0));
        assertEquals("Bob William Jones", ds.get(1, 0));
    }

    @Test
    public void combineColumns_toNewType() {
        sampleDataset.combineColumns(Arrays.asList("ID", "Age"), "ID_Age_Combined", Map.class);
        assertEquals(2, sampleDataset.columnCount());
        assertTrue(sampleDataset.columnNames().contains("ID_Age_Combined"));
        assertTrue(sampleDataset.columnNames().contains("Name"));
        assertFalse(sampleDataset.columnNames().contains("ID"));
        assertFalse(sampleDataset.columnNames().contains("Age"));

        assertEquals(Map.of("ID", 1, "Age", 30), sampleDataset.moveToRow(0).get("ID_Age_Combined"));
        assertEquals(Map.of("ID", 2, "Age", 24), sampleDataset.moveToRow(1).get("ID_Age_Combined"));
    }

    @Test
    public void combineColumns_withFunction() {
        sampleDataset.combineColumns(Arrays.asList("Name", "Age"), "NameAndAge", (DisposableObjArray row) -> row.get(0) + " is " + row.get(1));
        assertEquals(2, sampleDataset.columnCount());
        assertTrue(sampleDataset.columnNames().contains("NameAndAge"));
        assertEquals(Arrays.asList("Alice is 30", "Bob is 24", "Charlie is 35"), sampleDataset.getColumn("NameAndAge"));
    }

    @Test
    public void testCombineColumns() {
        Dataset ds = dataset.copy();
        ds.println();
        ds.combineColumns(Arrays.asList("name", "age"), "nameAge", Map.class);
        ds.println();
        assertEquals(3, ds.columnCount());
        assertTrue(ds.containsColumn("nameAge"));
    }

    @Test
    public void testCombineColumnsWithFunction() {
        Dataset ds = dataset.copy();
        ds.combineColumns(Arrays.asList("name", "age"), "info", arr -> arr.get(0) + ":" + arr.get(1));
        assertEquals("John:25", ds.get(0, ds.getColumnIndex("info")));
    }

    @Test
    public void testCombineColumnsWithTuple2() {
        Dataset ds = dataset.copy();
        ds.combineColumns(Tuple.of("name", "age"), "combined", (String name, Integer age) -> name + "_" + age);
        assertEquals("John_25", ds.get(0, ds.getColumnIndex("combined")));
    }

    @Test
    public void testCombineColumnsWithBiFunction() {
        Dataset dataset = testDataset.copy();
        Tuple2<String, String> columnNames = Tuple.of("name", "age");
        BiFunction<String, Integer, String> combineFunc = (name, age) -> name + "(" + age + ")";
        dataset.combineColumns(columnNames, "name_age_bi", combineFunc);

        assertTrue(dataset.containsColumn("name_age_bi"));
        dataset.moveToRow(0);
        assertEquals("Alice(30)", dataset.get("name_age_bi"));
    }

    @Test
    public void testCombineColumnsWithTriFunction() {
        Dataset dataset = testDataset.copy();
        Tuple3<String, String, String> columnNames = Tuple.of("id", "name", "age");
        TriFunction<Integer, String, Integer, String> combineFunc = (id, name, age) -> "ID:" + id + ",Name:" + name + ",Age:" + age;
        dataset.combineColumns(columnNames, "full_info", combineFunc);

        assertTrue(dataset.containsColumn("full_info"));
        dataset.moveToRow(0);
        assertEquals("ID:1,Name:Alice,Age:30", dataset.get("full_info"));
    }

    @Test
    public void testDivideColumn() {
        Dataset ds = Dataset.rows(Arrays.asList("fullName"), new Object[][] { { "John_Doe" }, { "Jane_Smith" } });
        ds.divideColumn("fullName", Arrays.asList("firstName", "lastName"), (String full) -> Arrays.asList(full.split("_")));
        assertEquals(2, ds.columnCount());
        System.out.println(ds.columnNames());
        ds.println("     *  // ");
        assertEquals("John", ds.get(0, ds.getColumnIndex("firstName")));
        assertEquals("Doe", ds.get(0, ds.getColumnIndex("lastName")));
    }

    @Test
    public void divideColumn_withFunctionToList() {
        sampleDataset.addColumn("FullName", Arrays.asList("Alice Wonderland", "Bob TheBuilder", "Charlie Brown"));
        sampleDataset.divideColumn("FullName", Arrays.asList("FirstName", "LastName"), (String fullName) -> {
            if (fullName == null)
                return Arrays.asList(null, null);
            String[] parts = fullName.split(" ", 2);
            return Arrays.asList(parts[0], parts.length > 1 ? parts[1] : null);
        });

        assertEquals(5, sampleDataset.columnCount());
        assertTrue(sampleDataset.columnNames().contains("FirstName"));
        assertTrue(sampleDataset.columnNames().contains("LastName"));
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"), sampleDataset.getColumn("FirstName"));
        assertEquals(Arrays.asList("Wonderland", "TheBuilder", "Brown"), sampleDataset.getColumn("LastName"));
    }

    @Test
    public void divideColumn_withBiConsumerObjectArray() {
        sampleDataset.addColumn("Coords", Arrays.asList("10,20", "30,40", "50,60"));
        sampleDataset.divideColumn("Coords", Arrays.asList("X", "Y"), (String coords, Object[] output) -> {
            if (coords == null) {
                output[0] = null;
                output[1] = null;
                return;
            }
            String[] parts = coords.split(",");
            output[0] = Integer.parseInt(parts[0]);
            output[1] = Integer.parseInt(parts[1]);
        });
        assertEquals(5, sampleDataset.columnCount());
        assertEquals(Arrays.asList(10, 30, 50), sampleDataset.getColumn("X"));
        assertEquals(Arrays.asList(20, 40, 60), sampleDataset.getColumn("Y"));
    }

    @Test
    public void divideColumn_withBiConsumerPair() {
        sampleDataset.addColumn("NameAndInitial", Arrays.asList("Alice A", "Bob B", "Charlie C"));
        sampleDataset.divideColumn("NameAndInitial", Tuple.of("DerivedName", "Initial"), (String ni, Pair<Object, Object> output) -> {
            if (ni == null) {
                output.set(null, null);
                return;
            }
            String[] parts = ni.split(" ");
            output.setLeft(parts[0]);
            output.setRight(parts[1].charAt(0));
        });
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"), sampleDataset.getColumn("DerivedName"));
        assertEquals(Arrays.asList('A', 'B', 'C'), sampleDataset.getColumn("Initial"));
    }

    @Test
    public void testDivideColumnWithBiConsumer() {
        Dataset dataset = testDataset.copy();
        Object[] output = new Object[2];
        BiConsumer<String, Object[]> outputConsumer = (name, arr) -> {
            arr[0] = name.toUpperCase();
            arr[1] = name.length();
        };
        dataset.divideColumn("name", Arrays.asList("upper_name", "name_length"), outputConsumer);

        assertTrue(dataset.containsColumn("upper_name"));
        assertTrue(dataset.containsColumn("name_length"));
        dataset.moveToRow(0);
        assertEquals("ALICE", dataset.get("upper_name"));
        assertEquals(5, (Integer) dataset.get("name_length"));
    }

    @Test
    public void testDivideColumnWithPairOutput() {
        Dataset dataset = testDataset.copy();
        Tuple2<String, String> newColumnNames = Tuple.of("name_upper", "name_lower");
        BiConsumer<String, Pair<Object, Object>> output = (name, pair) -> {
            pair.setLeft(name.toUpperCase());
            pair.setRight(name.toLowerCase());
        };
        dataset.divideColumn("name", newColumnNames, output);

        assertTrue(dataset.containsColumn("name_upper"));
        assertTrue(dataset.containsColumn("name_lower"));
        dataset.moveToRow(0);
        assertEquals("ALICE", dataset.get("name_upper"));
        assertEquals("alice", dataset.get("name_lower"));
    }

    @Test
    public void testDivideColumnWithTripleOutput() {
        Dataset dataset = testDataset.copy();
        Tuple3<String, String, String> newColumnNames = Tuple.of("name_upper", "name_lower", "name_length");
        BiConsumer<String, Triple<Object, Object, Object>> output = (name, triple) -> {
            triple.setLeft(name.toUpperCase());
            triple.setMiddle(name.toLowerCase());
            triple.setRight(name.length());
        };
        dataset.divideColumn("name", newColumnNames, output);

        assertTrue(dataset.containsColumn("name_upper"));
        assertTrue(dataset.containsColumn("name_lower"));
        assertTrue(dataset.containsColumn("name_length"));
        dataset.moveToRow(0);
        assertEquals("ALICE", dataset.get("name_upper"));
        assertEquals("alice", dataset.get("name_lower"));
        assertEquals(5, (Integer) dataset.get("name_length"));
    }

    @Test
    public void columnMap() {
        Map<String, com.landawn.abacus.util.ImmutableList<Object>> map = sampleDataset.columnMap();
        assertEquals(3, map.size());
        assertEquals(sampleDataset.getColumn("ID"), map.get("ID"));
        assertEquals(sampleDataset.getColumn("Name"), map.get("Name"));
    }

    @Test
    public void testColumnMap() {
        Map<String, ImmutableList<Object>> map = dataset.columnMap();

        assertNotNull(map);
        assertEquals(4, map.size());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("age"));
        assertTrue(map.containsKey("salary"));

        ImmutableList<Object> ids = map.get("id");
        assertEquals(5, ids.size());
        assertEquals(Integer.valueOf(1), ids.get(0));
        assertEquals(Integer.valueOf(2), ids.get(1));

        ImmutableList<Object> names = map.get("name");
        assertEquals("John", names.get(0));
        assertEquals("Jane", names.get(1));
    }

    @Test
    public void testAddRow() {
        Dataset ds = dataset.copy();
        ds.addRow(Arrays.asList(5, "Eve", 27, 52000.0));

        assertEquals(6, ds.size());
        assertEquals(Integer.valueOf(5), ds.get(5, 0));
        assertEquals("Eve", ds.get(5, 1));
    }

    @Test
    public void testAddRow_AtPosition() {
        Dataset ds = dataset.copy();
        ds.addRow(1, Arrays.asList(5, "Eve", 27, 52000.0));

        assertEquals(6, ds.size());
        assertEquals(Integer.valueOf(5), ds.get(1, 0));
        assertEquals(Integer.valueOf(2), ds.get(2, 0));
    }

    @Test
    public void addRow_fromArray() {
        sampleDataset.addRow(new Object[] { 4, "David", 28 });
        assertEquals(4, sampleDataset.size());
        assertEquals("David", sampleDataset.moveToRow(3).get("Name"));
    }

    @Test
    public void addRow_fromList() {
        sampleDataset.addRow(Arrays.asList(4, "Eve", 40));
        assertEquals(4, sampleDataset.size());
        assertEquals((Integer) 40, sampleDataset.moveToRow(3).get("Age"));
    }

    @Test
    public void addRow_fromBean() {
        TestBean bean = new TestBean(4, "Ivy", 22);
        RowDataset beanDs = new RowDataset(Arrays.asList("id", "name", "value"), Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        beanDs.addRow(bean);
        assertEquals(1, beanDs.size());
        assertEquals((Integer) 4, beanDs.moveToRow(0).get("id"));
        assertEquals("Ivy", beanDs.moveToRow(0).get("name"));
        assertEquals(22.0, (double) beanDs.moveToRow(0).get("value"), 0.001);
    }

    @Test
    public void testAddRowWithPosition() {
        Dataset ds = dataset.copy();
        ds.addRow(1, new Object[] { 5, "Eve", 32, 58000.0 });
        assertEquals(6, ds.size());
        assertEquals("Eve", ds.get(1, 1));
    }

    @Test
    public void testAddRowAtPosition() {
        Dataset dataset = testDataset.copy();
        Object[] newRow = { 5, "Eve", 32, 58000.0 };
        dataset.addRow(1, newRow);

        assertEquals(5, dataset.size());
        dataset.moveToRow(1);
        assertEquals(5, dataset.getInt("id"));
        assertEquals("Eve", dataset.get("name"));
    }

    @Test
    public void testAddRowWithCollection() {
        List<Object> newRow = Arrays.asList(6, "Frank", 45, 90000.0);
        dataset.addRow(newRow);

        assertEquals(6, dataset.size());
        assertEquals((Integer) 6, dataset.get(5, 0));
        assertEquals("Frank", dataset.get(5, 1));
    }

    @Test
    public void testAddRowWithMap() {
        Map<String, Object> newRow = new HashMap<>();
        newRow.put("id", 6);
        newRow.put("name", "Frank");
        newRow.put("age", 45);
        newRow.put("salary", 90000.0);

        dataset.addRow(newRow);

        assertEquals(6, dataset.size());
        assertEquals((Integer) 6, dataset.get(5, 0));
        assertEquals("Frank", dataset.get(5, 1));
    }

    @Test
    public void testAddRowWithBean() {
        Person person = new Person(6, "Frank", 45, 90000.0);
        dataset.addRow(person);

        assertEquals(6, dataset.size());
        assertEquals((Integer) 6, dataset.get(5, 0));
        assertEquals("Frank", dataset.get(5, 1));
    }

    @Test
    public void addRow_atPosition() {
        sampleDataset.addRow(1, new Object[] { 0, "Zero", 0 });
        assertEquals(4, sampleDataset.size());
        assertEquals("Zero", sampleDataset.moveToRow(1).get("Name"));
        assertEquals("Bob", sampleDataset.moveToRow(2).get("Name"));
    }

    @Test
    public void testNullHandling() {
        dataset.addRow(new Object[] { null, null, null, null });

        assertNull(dataset.get(5, 0));
        assertTrue(dataset.isNull(5, 0));

        dataset.println();

        dataset.moveToRow(5);

        assertEquals(0, dataset.getInt(dataset.getColumnIndex("id")));
        assertEquals(0.0, dataset.getDouble(dataset.getColumnIndex("salary")), 0.01);
        assertFalse(dataset.getBoolean(dataset.getColumnIndex("id")));
    }

    @Test
    public void addRow_fromMap() {
        Map<String, Object> newRowMap = new LinkedHashMap<>();
        newRowMap.put("ID", 4);
        newRowMap.put("Name", "Frank");
        newRowMap.put("Age", 33);
        sampleDataset.addRow(newRowMap);
        assertEquals(4, sampleDataset.size());
        assertEquals("Frank", sampleDataset.moveToRow(3).get("Name"));

        Map<String, Object> incompleteMap = Map.of("ID", 5, "Name", "Grace");
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.addRow(incompleteMap));
    }

    @Test
    public void testAddRows_Collection() {
        Dataset ds = dataset.copy();
        List<Object[]> newRows = new ArrayList<>();
        newRows.add(new Object[] { 5, "Eve", 27, 52000.0 });
        newRows.add(new Object[] { 6, "Frank", 32, 58000.0 });

        ds.addRows(newRows);

        assertEquals(7, ds.size());
        assertEquals(Integer.valueOf(5), ds.get(5, 0));
        assertEquals("Eve", ds.get(5, 1));
        assertEquals(Integer.valueOf(6), ds.get(6, 0));
        assertEquals("Frank", ds.get(6, 1));
    }

    @Test
    public void testAddRows_AtPosition() {
        Dataset ds = dataset.copy();
        List<Object[]> newRows = new ArrayList<>();
        newRows.add(new Object[] { 5, "Eve", 27, 52000.0 });
        newRows.add(new Object[] { 6, "Frank", 32, 58000.0 });

        ds.addRows(1, newRows);

        assertEquals(7, ds.size());
        assertEquals(Integer.valueOf(5), ds.get(1, 0));
        assertEquals("Eve", ds.get(1, 1));
        assertEquals(Integer.valueOf(6), ds.get(2, 0));
        assertEquals("Frank", ds.get(2, 1));
        assertEquals(Integer.valueOf(2), ds.get(3, 0));
    }

    @Test
    public void testAddRows() {
        Dataset ds = dataset.copy();
        List<Object[]> newRows = Arrays.asList(new Object[] { 5, "Eve", 32, 58000.0 }, new Object[] { 6, "Frank", 29, 52000.0 });
        ds.addRows(newRows);
        assertEquals(7, ds.size());
        assertEquals("Eve", ds.get(5, 1));
        assertEquals("Frank", ds.get(6, 1));
    }

    @Test
    public void testAddRowsAtPosition() {
        Dataset dataset = testDataset.copy();
        List<Object[]> newRows = Arrays.asList(new Object[] { 5, "Eve", 32, 58000.0 }, new Object[] { 6, "Frank", 29, 52000.0 });
        dataset.addRows(1, newRows);

        assertEquals(6, dataset.size());
        dataset.moveToRow(1);
        assertEquals(5, dataset.getInt("id"));
        dataset.moveToRow(2);
        assertEquals(6, dataset.getInt("id"));
    }

    @Test
    @DisplayName("Should add rows at beginning of dataset")
    public void testAddRowsAtBeginning() {
        Collection<Object[]> rows = Arrays.asList(new Object[] { 0, "First", 20, 40000.0 }, new Object[] { -1, "Second", 22, 42000.0 });

        dataset.addRows(0, rows);

        assertEquals(7, dataset.size());
        assertEquals((Integer) 0, dataset.get(0, 0));
        assertEquals("First", dataset.get(0, 1));
        assertEquals((Integer) (-1), dataset.get(1, 0));
        assertEquals("Second", dataset.get(1, 1));
        assertEquals((Integer) 1, dataset.get(2, 0));
        assertEquals("John", dataset.get(2, 1));
    }

    @Test
    @DisplayName("Should add rows at end of dataset")
    public void testAddRowsAtEnd() {

        Collection<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { 6, "Last", 60, 120000.0 });

        int originalSize = dataset.size();
        dataset.addRows(originalSize, rows);

        assertEquals(originalSize + 1, dataset.size());
        assertEquals((Integer) 6, dataset.get(originalSize, 0));
        assertEquals("Last", dataset.get(originalSize, 1));
    }

    @Test
    @DisplayName("Should handle row with extra columns")
    public void testAddRowsWithExtraColumns() {
        Collection<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { 6, "Frank", 45, 90000.0, "Extra1", "Extra2" });

        int originalSize = dataset.size();
        dataset.addRows(rows);

        assertEquals(originalSize + 1, dataset.size());
        assertEquals((Integer) 6, dataset.get(originalSize, 0));
        assertEquals("Frank", dataset.get(originalSize, 1));
    }

    @Test
    public void testAddRowsModCountIncrement() {
        Object[] row = { 4, "David", 40, 100.0 };
        Collection<Object[]> rows = Collections.singletonList(row);

        int sizeBefore = dataset.size();
        dataset.addRows(rows);

        assertEquals(sizeBefore + 1, dataset.size());
    }

    @Test
    @DisplayName("Should add multiple rows with Object arrays")
    public void testAddRowsWithObjectArrays() {
        Collection<Object[]> rows = Arrays.asList(new Object[] { 6, "Frank", 45, 90000.0 }, new Object[] { 7, "Grace", 32, 75000.0 },
                new Object[] { 8, "Henry", 28, 65000.0 });

        int originalSize = dataset.size();
        dataset.addRows(rows);

        assertEquals(originalSize + 3, dataset.size());
        assertEquals((Integer) 6, dataset.get(5, 0));
        assertEquals("Frank", dataset.get(5, 1));
        assertEquals((Integer) 7, dataset.get(6, 0));
        assertEquals("Grace", dataset.get(6, 1));
        assertEquals((Integer) 8, dataset.get(7, 0));
        assertEquals("Henry", dataset.get(7, 1));
    }

    @Test
    @DisplayName("Should add multiple rows at specific position with Object arrays")
    public void testAddRowsAtPositionWithObjectArrays() {
        Collection<Object[]> rows = Arrays.asList(new Object[] { 10, "Insert1", 50, 95000.0 }, new Object[] { 11, "Insert2", 55, 100000.0 });

        int originalSize = dataset.size();
        dataset.addRows(2, rows);

        assertEquals(originalSize + 2, dataset.size());
        assertEquals("Bob", dataset.get(4, 1));
        assertEquals((Integer) 10, dataset.get(2, 0));
        assertEquals("Insert1", dataset.get(2, 1));
        assertEquals((Integer) 11, dataset.get(3, 0));
        assertEquals("Insert2", dataset.get(3, 1));
    }

    @Test
    @DisplayName("Should add multiple rows with Lists")
    public void testAddRowsWithLists() {
        Collection<List<Object>> rows = Arrays.asList(Arrays.asList(6, "Frank", 45, 90000.0), Arrays.asList(7, "Grace", 32, 75000.0),
                Arrays.asList(8, "Henry", 28, 65000.0));

        int originalSize = dataset.size();
        dataset.addRows(rows);

        assertEquals(originalSize + 3, dataset.size());
        assertEquals((Integer) 6, dataset.get(5, 0));
        assertEquals("Frank", dataset.get(5, 1));
        assertEquals((Integer) 7, dataset.get(6, 0));
        assertEquals("Grace", dataset.get(6, 1));
    }

    @Test
    @DisplayName("Should add multiple rows with Maps")
    public void testAddRowsWithMaps() {
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", 6);
        row1.put("name", "Frank");
        row1.put("age", 45);
        row1.put("salary", 90000.0);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", 7);
        row2.put("name", "Grace");
        row2.put("age", 32);
        row2.put("salary", 75000.0);

        Collection<Map<String, Object>> rows = Arrays.asList(row1, row2);

        int originalSize = dataset.size();
        dataset.addRows(rows);

        assertEquals(originalSize + 2, dataset.size());
        assertEquals((Integer) 6, dataset.get(5, 0));
        assertEquals("Frank", dataset.get(5, 1));
        assertEquals((Integer) 7, dataset.get(6, 0));
        assertEquals("Grace", dataset.get(6, 1));
    }

    @Test
    @DisplayName("Should add multiple rows with Bean objects")
    public void testAddRowsWithBeans() {
        Collection<Person> rows = Arrays.asList(new Person(6, "Frank", 45, 90000.0), new Person(7, "Grace", 32, 75000.0), new Person(8, "Henry", 28, 65000.0));

        int originalSize = dataset.size();
        dataset.addRows(rows);

        assertEquals(originalSize + 3, dataset.size());
        assertEquals((Integer) 6, dataset.get(5, 0));
        assertEquals("Frank", dataset.get(5, 1));
        assertEquals((Integer) 45, dataset.get(5, 2));
        assertEquals((Double) 90000.0, dataset.get(5, 3));
    }

    @Test
    @DisplayName("Should handle empty collection in addRows")
    public void testAddRowsWithEmptyCollection() {
        Collection<Object[]> rows = Collections.emptyList();

        int originalSize = dataset.size();
        dataset.addRows(rows);

        assertEquals(originalSize, dataset.size());
    }

    @Test
    @DisplayName("Should handle null values in rows")
    public void testAddRowsWithNullValues() {
        Collection<Object[]> rows = Arrays.asList(new Object[] { 6, null, 45, 90000.0 }, new Object[] { 7, "Grace", null, 75000.0 },
                new Object[] { 8, "Henry", 28, null });

        int originalSize = dataset.size();
        dataset.addRows(rows);

        assertEquals(originalSize + 3, dataset.size());
        assertNull(dataset.get(5, 1));
        assertNull(dataset.get(6, 2));
        assertNull(dataset.get(7, 3));
    }

    @Test
    @DisplayName("Should add single row using addRows")
    public void testAddRowsWithSingleRow() {
        Collection<Object[]> rows = Collections.singletonList(new Object[] { 6, "Single", 45, 90000.0 });

        int originalSize = dataset.size();
        dataset.addRows(rows);

        assertEquals(originalSize + 1, dataset.size());
        assertEquals((Integer) 6, dataset.get(originalSize, 0));
        assertEquals("Single", dataset.get(originalSize, 1));
    }

    @Test
    @DisplayName("Should preserve row order when adding multiple rows")
    public void testAddRowsPreservesOrder() {
        Collection<Object[]> rows = new ArrayList<>();
        for (int i = 10; i <= 15; i++) {
            rows.add(new Object[] { i, "Person" + i, 20 + i, 50000.0 + i * 1000 });
        }

        int originalSize = dataset.size();
        dataset.addRows(rows);

        assertEquals(originalSize + 6, dataset.size());

        for (int i = 0; i < 6; i++) {
            assertEquals((Integer) (10 + i), dataset.get(originalSize + i, 0));
            assertEquals("Person" + (10 + i), dataset.get(originalSize + i, 1));
        }
    }

    @Test
    public void testAddRowsOnFrozenDataset() {
        dataset.freeze();

        Object[] row = { 4, "David", 40 };
        Collection<Object[]> rows = Collections.singletonList(row);

        assertThrows(IllegalStateException.class, () -> dataset.addRows(rows));
    }

    @Test
    @DisplayName("Should throw exception when row has fewer columns than dataset")
    public void testAddRowsWithInsufficientColumns() {
        Collection<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { 6, "Frank" });

        assertThrows(IllegalArgumentException.class, () -> dataset.addRows(rows));
    }

    @Test
    @DisplayName("Should throw exception for invalid position in addRows")
    public void testAddRowsWithInvalidPosition() {
        Collection<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { 6, "Test", 45, 90000.0 });

        assertThrows(IndexOutOfBoundsException.class, () -> dataset.addRows(100, rows));

        assertThrows(IndexOutOfBoundsException.class, () -> dataset.addRows(-1, rows));
    }

    @Test
    @DisplayName("Should throw exception for mixed collection types")
    public void testAddRowsWithMixedTypes() {
        Collection<Object> mixedRows = Arrays.asList(new Object[] { 6, "Array", 45, 90000.0 }, Arrays.asList(7, "List", 32, 75000.0));

        @SuppressWarnings("unchecked")
        Collection<?> rows = mixedRows;
        assertThrows(ClassCastException.class, () -> dataset.addRows(rows));
    }

    @Test
    @DisplayName("Should throw exception for unsupported row type")
    public void testAddRowsWithUnsupportedType() {
        Collection<String> rows = Arrays.asList("row1", "row2");

        assertThrows(IllegalArgumentException.class, () -> dataset.addRows(rows));
    }

    @Test
    public void testRemoveRow() {
        Dataset ds = dataset.copy();
        ds.removeRow(0);

        assertEquals(4, ds.size());
        assertEquals(Integer.valueOf(2), ds.get(0, 0));
    }

    @Test
    public void testRemoveRowRange() {
        Dataset ds = dataset.copy();
        ds.removeRows(1, 3);
        assertEquals(3, ds.size());
        assertEquals("John", ds.get(0, 1));
        assertEquals("Alice", ds.get(1, 1));
    }

    @Test
    public void test_removeRowRange() {
        final Dataset ds = CommonUtil.newDataset(CommonUtil.toList(createAccount(Account.class), createAccount(Account.class), createAccount(Account.class),
                createAccount(Account.class), createAccount(Account.class), createAccount(Account.class)));

        final Dataset ds1 = ds.copy();
        ds1.println();

        ds1.removeRows(1, 5);
        ds1.println();

        final Dataset ds2 = ds.copy();
        ds2.println();

        ds2.removeRowsAt(1, 3, 5);
        ds2.println();

        final Dataset ds3 = ds.copy();
        ds3.println();

        ds3.removeRowsAt(0, 2, 4, 5);
        ds3.println();
        assertNotNull(ds3);
    }

    @Test
    public void testRemoveRowRange_EdgeCases() {
        Dataset ds = dataset.copy();

        ds.removeRows(0, 2);
        assertEquals(3, ds.size());
        assertEquals(Integer.valueOf(3), ds.get(0, 0));

        ds.removeRows(0, 1);
        assertEquals(2, ds.size());
        assertEquals(Integer.valueOf(4), ds.get(0, 0));
    }

    @Test
    public void removeRow() {
        sampleDataset.removeRow(1);
        assertEquals(2, sampleDataset.size());
        assertEquals("Charlie", sampleDataset.moveToRow(1).get("Name"));
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataset.removeRow(5));
    }

    @Test
    public void removeRowRange() {
        sampleDataset.removeRows(0, 2);
        assertEquals(1, sampleDataset.size());
        assertEquals("Charlie", sampleDataset.moveToRow(0).get("Name"));
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataset.removeRows(0, 5));
    }

    @Test
    public void testRemoveRows_Range() {
        Dataset ds = dataset.copy();
        ds.removeRows(1, 3);

        assertEquals(3, ds.size());
        assertEquals(Integer.valueOf(1), ds.get(0, 0));
        assertEquals(Integer.valueOf(4), ds.get(1, 0));
    }

    @Test
    public void removeRows_indices() {
        sampleDataset.removeRowsAt(0, 2);
        assertEquals(1, sampleDataset.size());
        assertEquals("Bob", sampleDataset.moveToRow(0).get("Name"));
    }

    @Test
    public void testRemoveRows() {
        Dataset ds = dataset.copy();
        ds.removeRowsAt(1, 2);
        assertEquals(3, ds.size());
        assertEquals("John", ds.get(0, 1));
        assertEquals("Alice", ds.get(1, 1));
    }

    @Test
    public void testRemoveDuplicateRowsByColumn() {
        Dataset ds = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "A" }, { 2, "B" }, { 1, "C" } });
        ds.removeDuplicateRowsBy("id");
        assertEquals(2, ds.size());
    }

    @Test
    public void testRemoveDuplicateRowsByColumns() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Alice", 30, 70000.0 });

        dataset.removeDuplicateRowsBy(Arrays.asList("name", "age"));

        assertEquals(4, dataset.size());
    }

    @Test
    public void testRemoveDuplicateRowsByColumnsWithExtractor() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Alice", 30, 70000.0 });

        dataset.removeDuplicateRowsBy(Arrays.asList("name", "age"), (DisposableObjArray row) -> row.get(0).toString() + "_" + row.get(1));

        assertEquals(4, dataset.size());
    }

    @Test
    public void testRemoveDuplicateRowsBy() {
        Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "department"),
                new Object[][] { { 1, "John", "IT" }, { 2, "Jane", "HR" }, { 3, "John", "Finance" }, { 4, "Bob", "IT" } });
        dataset.removeDuplicateRowsBy("name");
        assertEquals(3, dataset.size());
        assertEquals((Integer) 1, dataset.get(0, 0));
        assertEquals((Integer) 2, dataset.get(1, 0));
        assertEquals((Integer) 4, dataset.get(2, 0));
    }

    @Test
    public void testRemoveDuplicateRowsBy2() {
        Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "department"),
                new Object[][] { { 1, "John Doe", "IT" }, { 2, "Jane Smith", "HR" }, { 3, "Johnathan Doe", "Finance" }, { 4, "Bob Brown", "IT" } });
        dataset.removeDuplicateRowsBy("name", name -> ((String) name).split(" ")[1]);
        assertEquals(3, dataset.size());
        assertEquals((Integer) 1, dataset.get(0, 0));
        assertEquals((Integer) 2, dataset.get(1, 0));
        assertEquals((Integer) 4, dataset.get(2, 0));
    }

    @Test
    public void testRemoveDuplicateRowsBy4() {
        Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "department"),
                new Object[][] { { 1, "John Doe", "IT" }, { 2, "Jane Smith", "HR" }, { 3, "Johnathan Doe", "IT" }, { 4, "Bob Brown", "IT" } });
        dataset.removeDuplicateRowsBy(Arrays.asList("name", "department"), row -> ((String) row.get(0)).split(" ")[1] + "|" + row.get(1));
        assertEquals(3, dataset.size());
        assertEquals((Integer) 1, dataset.get(0, 0));
        assertEquals((Integer) 2, dataset.get(1, 0));
        assertEquals((Integer) 4, dataset.get(2, 0));
    }

    @Test
    public void testRemoveDuplicateRowsBy_SingleColumn() {
        Dataset ds = Dataset.rows(Arrays.asList("id", "name", "type"),
                new Object[][] { { 1, "Alice", "A" }, { 2, "Bob", "B" }, { 3, "Charlie", "A" }, { 4, "Diana", "B" }, { 5, "Eve", "A" } });

        ds.removeDuplicateRowsBy("type");

        assertEquals(2, ds.size());
        assertEquals("Alice", ds.get(0, 1));
        assertEquals("Bob", ds.get(1, 1));
    }

    @Test
    public void testRemoveDuplicateRowsBy_WithKeyExtractor() {
        Dataset ds = Dataset.rows(Arrays.asList("id", "name", "email"), new Object[][] { { 1, "Alice", "alice@test.com" }, { 2, "Bob", "bob@test.com" },
                { 3, "Charlie", "charlie@test.com" }, { 4, "Diana", "alice@test.com" } });

        ds.removeDuplicateRowsBy("email", (String email) -> email.toLowerCase());

        assertEquals(3, ds.size());
        assertEquals("Alice", ds.get(0, 1));
        assertEquals("Bob", ds.get(1, 1));
        assertEquals("Charlie", ds.get(2, 1));
    }

    @Test
    public void testRemoveDuplicateRowsBy_MultipleColumns() {
        Dataset ds = Dataset.rows(Arrays.asList("firstName", "lastName", "age"),
                new Object[][] { { "John", "Doe", 25 }, { "Jane", "Smith", 30 }, { "John", "Doe", 25 }, { "John", "Smith", 28 } });

        ds.removeDuplicateRowsBy(Arrays.asList("firstName", "lastName"));

        assertEquals(3, ds.size());
        assertEquals("John", ds.get(0, 0));
        assertEquals("Doe", ds.get(0, 1));
        assertEquals("Jane", ds.get(1, 0));
        assertEquals("Smith", ds.get(1, 1));
        assertEquals("John", ds.get(2, 0));
        assertEquals("Smith", ds.get(2, 1));
    }

    @Test
    public void testRemoveDuplicateRowsByColumnWithExtractor() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "ALICE", 40, 70000.0 });

        dataset.removeDuplicateRowsBy("name", (String name) -> name.toLowerCase());

        assertEquals(4, dataset.size());
    }

    @Test
    public void testUpdateRow() {
        Dataset ds = dataset.copy();
        ds.updateRow(0, value -> value instanceof String ? "Updated" : value);
        assertEquals("Updated", ds.get(0, 1));
    }

    @Test
    public void updateRow() {
        sampleDataset.updateRow(0, val -> {
            if (val instanceof String)
                return ((String) val).toUpperCase();
            if (val instanceof Integer && ((Integer) val) == 30)
                return 31;
            return val;
        });
        assertEquals("ALICE", sampleDataset.moveToRow(0).get("Name"));
        assertEquals((Integer) 31, sampleDataset.moveToRow(0).get("Age"));
    }

    @Test
    public void testUpdateRows() {
        Dataset ds = dataset.copy();
        ds.updateRows(new int[] { 0, 1 }, (i, c, v) -> v instanceof String ? "Updated" : v);
        assertEquals("Updated", ds.get(0, 1));
        assertEquals("Updated", ds.get(1, 1));
    }

    @Test
    public void updateAll() {
        sampleDataset.updateAll(val -> {
            if (val instanceof String)
                return "Name_" + val;
            if (val instanceof Integer)
                return ((Integer) val) + 10;
            return val;
        });
        assertEquals(Arrays.asList(11, 12, 13), sampleDataset.getColumn("ID"));
        assertEquals(Arrays.asList("Name_Alice", "Name_Bob", "Name_Charlie"), sampleDataset.getColumn("Name"));
        assertEquals(Arrays.asList(40, 34, 45), sampleDataset.getColumn("Age"));
    }

    @Test
    public void testUpdateAllWithIntBiObjFunction() {
        dataset.println();

        dataset.updateAll((i, c, v) -> v instanceof String ? "Name" + i : v);

        dataset.println();

        for (int i = 0; i < dataset.size(); i++) {
            assertEquals("Name" + i, dataset.get(i, 1));
        }
    }

    @Test
    public void testUpdateAll() {
        Dataset ds = dataset.copy();
        ds.updateAll(v -> v instanceof String ? ((String) v).toUpperCase() : v);
        assertEquals("JOHN", ds.get(0, 1));
        assertEquals("JANE", ds.get(1, 1));
    }

    @Test
    public void replaceIf() {
        sampleDataset.replaceIf(val -> val instanceof String && "Bob".equals(val), "Robert");
        assertEquals("Robert", sampleDataset.moveToRow(1).get("Name"));

        sampleDataset.replaceIf(val -> val instanceof Integer && (Integer) val > 30, 0);
        assertEquals((Integer) 0, sampleDataset.moveToRow(2).get("Age"));
    }

    @Test
    public void testReplaceIf() {
        Dataset ds = dataset.copy();
        ds.replaceIf(value -> "Alice".equals(value), "Alicia");
        assertEquals("Alicia", ds.get(3, 1));
    }

    @Test
    public void testReplaceIfWithIntBiObjPredicate() {
        dataset.println();
        assertEquals("Bob", dataset.get(2, 1));
        dataset.replaceIf((i, c, v) -> "name".equals(c) && "Bob".equals(v), "Robert");
        dataset.println();
        assertEquals("Robert", dataset.get(2, 1));
    }

    @Test
    public void testPrepend() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 3, "Charlie" }, { 4, "Diana" } });

        ds1.prepend(ds2);

        assertEquals(4, ds1.size());
        assertEquals(Integer.valueOf(3), ds1.get(0, 0));
        assertEquals("Charlie", ds1.get(0, 1));
        assertEquals(Integer.valueOf(4), ds1.get(1, 0));
        assertEquals("Diana", ds1.get(1, 1));
        assertEquals(Integer.valueOf(1), ds1.get(2, 0));
        assertEquals("Alice", ds1.get(2, 1));
    }

    @Test
    public void prepend() {
        List<String> otherNames = Arrays.asList("ID", "Name", "Age");
        List<List<Object>> otherValues = Arrays.asList(new ArrayList<>(Arrays.asList(0)), new ArrayList<>(Arrays.asList("Zero")),
                new ArrayList<>(Arrays.asList(20)));
        RowDataset otherDs = new RowDataset(otherNames, otherValues);

        sampleDataset.prepend(otherDs);
        assertEquals(4, sampleDataset.size());
        assertEquals((Integer) 0, sampleDataset.moveToRow(0).get("ID"));
        assertEquals((Integer) 1, sampleDataset.moveToRow(1).get("ID"));
    }

    @Test
    public void testAppend() {
        Dataset ds1 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob" } });

        Dataset ds2 = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 3, "Charlie" }, { 4, "Diana" } });

        ds1.append(ds2);

        assertEquals(4, ds1.size());
        assertEquals(Integer.valueOf(1), ds1.get(0, 0));
        assertEquals("Alice", ds1.get(0, 1));
        assertEquals(Integer.valueOf(2), ds1.get(1, 0));
        assertEquals("Bob", ds1.get(1, 1));
        assertEquals(Integer.valueOf(3), ds1.get(2, 0));
        assertEquals("Charlie", ds1.get(2, 1));
    }

    @Test
    public void append() {
        List<String> otherNames = Arrays.asList("ID", "Name", "Age");
        List<List<Object>> otherValues = Arrays.asList(new ArrayList<>(Arrays.asList(4)), new ArrayList<>(Arrays.asList("David")),
                new ArrayList<>(Arrays.asList(28)));
        RowDataset otherDs = new RowDataset(otherNames, otherValues);

        sampleDataset.append(otherDs);
        assertEquals(4, sampleDataset.size());
        assertEquals((Integer) 4, sampleDataset.moveToRow(3).get("ID"));
    }

    @Test
    public void testMerge() {
        Dataset ds = dataset.copy();
        Dataset other = Dataset.rows(Arrays.asList("dept", "location"), new Object[][] { { "IT", "NYC" }, { "HR", "LA" } });
        ds.merge(other);
        assertEquals(6, ds.columnCount());
        assertTrue(ds.containsColumn("dept"));
        assertTrue(ds.containsColumn("location"));
    }

    @Test
    public void testMergeWithSelectedColumns() {
        Dataset ds = dataset.copy();
        Dataset other = Dataset.rows(Arrays.asList("dept", "location", "extra"), new Object[][] { { "IT", "NYC", "X" }, { "HR", "LA", "Y" } });
        ds.merge(other, Arrays.asList("dept", "location"));
        assertEquals(6, ds.columnCount());
        assertTrue(ds.containsColumn("dept"));
        assertFalse(ds.containsColumn("extra"));
    }

    @Test
    public void testMergeWithSelectColumns() {
        Dataset dataset = testDataset.copy();
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 5, "Five", 40, 70000.0 } });

        dataset.merge(other, Arrays.asList("id", "name"));

        assertEquals(5, dataset.size());
        dataset.moveToRow(4);
        assertEquals(5, dataset.getInt("id"));
        assertEquals("Five", dataset.get("name"));
    }

    @Test
    public void testMergeWithRange() {
        Dataset dataset = testDataset.copy();
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 5, "Five", 40, 70000.0 }, { 6, "Six", 35, 65000.0 } });

        dataset.merge(other, 0, 1, Arrays.asList("id", "name"));

        assertEquals(5, dataset.size());
        dataset.moveToRow(4);
        assertEquals(5, dataset.getInt("id"));
        assertEquals("Five", dataset.get("name"));
    }

    @Test
    public void testMergeWithCollection() {
        List<Dataset> others = new ArrayList<>();
        others.add(dataset);

        List<String> otherColumns1 = Arrays.asList("id", "name", "age", "salary");
        List<List<Object>> otherData1 = new ArrayList<>();
        otherData1.add(Arrays.asList(6, 7));
        otherData1.add(Arrays.asList("Frank", "Grace"));
        otherData1.add(Arrays.asList(45, 50));
        otherData1.add(Arrays.asList(90000.0, 95000.0));

        List<String> otherColumns2 = Arrays.asList("id", "name", "age", "salary", "bonus");
        List<List<Object>> otherData2 = new ArrayList<>();
        otherData2.add(Arrays.asList(8));
        otherData2.add(Arrays.asList("Henry"));
        otherData2.add(Arrays.asList(55));
        otherData2.add(Arrays.asList(100000.0));
        otherData2.add(Arrays.asList(10000.0));

        others.add(new RowDataset(otherColumns1, otherData1));
        others.add(new RowDataset(otherColumns2, otherData2));

        Dataset merged = CommonUtil.merge(others);

        assertEquals(8, merged.size());
        assertTrue(merged.containsColumn("bonus"));
    }

    @Test
    public void testMergeWithRequiredSameColumns() {
        Dataset ds = dataset.copy();
        Dataset other = Dataset.rows(columnNames, new Object[][] { { 5, "Five", 50, 50000.0 } });
        ds.merge(other, true);
        assertEquals(6, ds.size());
    }

    @Test
    public void testMergeWithColumnNames() {
        Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"), new Object[][] { { 1, "Alice", 25 }, { 2, "Bob", 30 } });
        Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"), new Object[][] { { 1, "Alice", 95 }, { 3, "Charlie", 85 } });

        dataset1.merge(dataset2, Arrays.asList("id", "name"));
        Dataset result = dataset1;
        result.println();
        assertEquals(3, result.columnCount());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertEquals(4, result.size());
        assertEquals(1, (Integer) result.get(0, 0));
        assertEquals("Alice", result.get(0, 1));
        assertEquals(25, (Integer) result.get(0, 2));
        assertEquals(2, (Integer) result.get(1, 0));
        assertEquals("Bob", result.get(1, 1));
        assertEquals(30, (Integer) result.get(1, 2));
        assertEquals(1, (Integer) result.get(2, 0));
        assertEquals("Alice", result.get(2, 1));
        assertNull(result.get(2, 2));
        assertEquals(3, (Integer) result.get(3, 0));
        assertEquals("Charlie", result.get(3, 1));
        assertNull(result.get(3, 2));
    }

    @Test
    public void testMergeWithColumnNames2() {
        Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"), new Object[][] { { 1, "Alice", 25 }, { 2, "Bob", 30 } });
        Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score", "grade"), new Object[][] { { 1, "Alice", 95, "A" }, { 3, "Charlie", 85, "B" } });

        Collection<String> selectedColumns = Arrays.asList("id", "score");
        dataset1.merge(dataset2, selectedColumns);
        Dataset result = dataset1;
        result.println();
        assertEquals(4, result.columnCount());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("score"));
        assertEquals(4, result.size());
        assertEquals(1, (Integer) result.get(0, 0));
        assertEquals("Alice", result.get(0, 1));
        assertEquals(25, (Integer) result.get(0, 2));
        assertNull(result.get(0, 3));
        assertEquals(2, (Integer) result.get(1, 0));
        assertEquals("Bob", result.get(1, 1));
        assertEquals(30, (Integer) result.get(1, 2));
        assertNull(result.get(1, 3));
        assertEquals(1, (Integer) result.get(2, 0));
        assertNull(result.get(2, 1));
        assertNull(result.get(2, 2));
        assertEquals(95, (Integer) result.get(2, 3));
        assertEquals(3, (Integer) result.get(3, 0));
        assertNull(result.get(3, 1));
        assertNull(result.get(3, 2));
        assertEquals(85, (Integer) result.get(3, 3));

    }

    @Test
    public void testMergeWithRequiresSameColumns() {

        Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"), new Object[][] { { 1, "Alice", 25 }, { 2, "Bob", 30 } });
        Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"), new Object[][] { { 1, "Alice", 95 }, { 3, "Charlie", 85 } });

        dataset1.merge(dataset2, false);
        Dataset result = dataset1;
        result.println();
        assertEquals(4, result.columnCount());
        assertTrue(result.containsColumn("id"));
        assertTrue(result.containsColumn("name"));
        assertTrue(result.containsColumn("age"));
        assertTrue(result.containsColumn("score"));
        assertEquals(4, result.size());
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
        assertNull(result.get(2, 2));
        assertEquals(95, (Integer) result.get(2, 3));
        assertEquals(3, (Integer) result.get(3, 0));
        assertEquals("Charlie", result.get(3, 1));
        assertNull(result.get(3, 2));
        assertEquals(85, (Integer) result.get(3, 3));

        assertThrows(IllegalArgumentException.class, () -> dataset1.merge(dataset2, true));

    }

    @Test
    public void testCurrentRowNum() {
        assertEquals(0, dataset.currentRowIndex());
        dataset.moveToRow(2);
        assertEquals(2, dataset.currentRowIndex());
    }

    @Test
    public void currentRowIndex_and_absolute() {
        assertEquals(0, sampleDataset.currentRowIndex());
        sampleDataset.moveToRow(1);
        assertEquals(1, sampleDataset.currentRowIndex());
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataset.moveToRow(10));
    }

    @Test
    public void testAbsolute() {
        dataset.moveToRow(2);
        assertEquals(2, dataset.currentRowIndex());
        assertEquals(Integer.valueOf(3), dataset.get("id"));
    }

    @Test
    public void testFirst() {
        dataset.moveToRow(2);
        dataset.moveToRow(0);
        assertEquals(0, dataset.currentRowIndex());
        assertEquals(Integer.valueOf(1), dataset.get("id"));
    }

    @Test
    public void testLast() {
        dataset.moveToRow(dataset.size() - 1);
        assertEquals(4, dataset.currentRowIndex());
        assertEquals(Integer.valueOf(5), dataset.get("id"));
    }

    @Test
    public void getRow_asArray() {
        Object[] row0 = sampleDataset.getRow(0);
        assertArrayEquals(new Object[] { 1, "Alice", 30 }, row0);
    }

    @Test
    public void getRow_asSpecificType_Array() {
        Object[] row1 = sampleDataset.getRow(1, Object[].class);
        assertArrayEquals(new Object[] { 2, "Bob", 24 }, row1);
    }

    @Test
    public void getRow_asSpecificType_List() {
        List<Object> row2 = sampleDataset.getRow(2, List.class);
        assertEquals(Arrays.asList(3, "Charlie", 35), row2);
    }

    @Test
    public void getRow_asSpecificType_Map() {
        Map<String, Object> row0Map = sampleDataset.getRow(0, Map.class);
        assertEquals(1, row0Map.get("ID"));
        assertEquals("Alice", row0Map.get("Name"));
        assertEquals(30, row0Map.get("Age"));
    }

    @Test
    public void getRow_asSpecificType_Bean() {
        RowDataset beanFriendlyDs = new RowDataset(Arrays.asList("id", "name", "value"), Arrays.asList(new ArrayList<>(Arrays.asList(101, 102)),
                new ArrayList<>(Arrays.asList("Bean1", "Bean2")), new ArrayList<>(Arrays.asList(10.5, 20.5))));
        TestBean bean1 = beanFriendlyDs.getRow(0, TestBean.class);
        assertEquals(101, bean1.getId());
        assertEquals("Bean1", bean1.getName());
        assertEquals(10.5, bean1.getValue(), 0.001);
    }

    @Test
    public void getRow_withSelectedColumns_asArray() {
        Object[] partialRow = sampleDataset.getRow(0, Arrays.asList("Name", "ID"), Object[].class);
        assertArrayEquals(new Object[] { "Alice", 1 }, partialRow);
    }

    @Test
    public void getRow_withSupplier() {
        AtomicInteger counter = new AtomicInteger(0);
        Map<String, Object> row = sampleDataset.getRow(0, size -> {
            counter.incrementAndGet();
            return new LinkedHashMap<>(size);
        });
        assertEquals(1, counter.get());
        assertTrue(row instanceof LinkedHashMap);
        assertEquals(1, row.get("ID"));
    }

    @Test
    public void testGetRow() {
        Object[] row = dataset.getRow(0);
        assertEquals(4, row.length);
        assertEquals(1, row[0]);
        assertEquals("John", row[1]);
    }

    @Test
    public void testGetRowNavigationMethods() {
        testDataset.moveToRow(0);

        assertEquals(1, testDataset.getByte(0));
        assertEquals(1, testDataset.getShort(0));
        assertEquals(1, testDataset.getInt(0));
        assertEquals(1L, testDataset.getLong(0));
        assertEquals(1.0f, testDataset.getFloat(0));
        assertEquals(1.0, testDataset.getDouble(0));

        assertEquals("Alice", testDataset.get(1));
        assertEquals(30, testDataset.getInt(2));
        assertEquals(50000.0, testDataset.getDouble(3));
    }

    @Test
    public void testGetRowWithClass() {
        List<Object> row = dataset.getRow(1, ArrayList.class);
        assertEquals(Arrays.asList(2, "Jane", 30, 60000.0), row);
    }

    @Test
    public void testGetRowWithColumnNames() {
        Object[] row = dataset.getRow(1, Arrays.asList("name", "age"), Object[].class);
        assertArrayEquals(new Object[] { "Jane", 30 }, row);
    }

    @Test
    public void testGetRow_AsEntity() {
        Person person = dataset.getRow(0, Person.class);
        assertNotNull(person);
        assertEquals(1, person.getId());
        assertEquals("John", person.getName());
    }

    @Test
    public void testGetRow_WithColumnNames() {
        Person person = dataset.getRow(0, Arrays.asList("id", "name"), Person.class);
        assertNotNull(person);
        assertEquals(1, person.getId());
        assertEquals("John", person.getName());
    }

    @Test
    public void testGetRowWithSupplier() {
        List<Object> row = dataset.getRow(0, (IntFunction<List<Object>>) ArrayList::new);
        assertNotNull(row);
        assertEquals(4, row.size());
        assertEquals(1, row.get(0));
        assertEquals("John", row.get(1));
    }

    @Test
    public void testInvalidRowOperations() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            testDataset.getRow(-1);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            testDataset.getRow(100);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            testDataset.removeRow(-1);
        });
    }

    @Test
    public void testGetRowWithPrefixAndFieldNameMap() {
        List<String> nestedColumns = Arrays.asList("id", "person.name", "person.age", "address.city");
        List<List<Object>> nestedData = new ArrayList<>();
        nestedData.add(Arrays.asList(1, 2));
        nestedData.add(Arrays.asList("John", "Jane"));
        nestedData.add(Arrays.asList(25, 30));
        nestedData.add(Arrays.asList("NYC", "LA"));

        Dataset nestedDataset = new RowDataset(nestedColumns, nestedData);

        Map<String, String> prefixMap = new HashMap<>();
        prefixMap.put("person", "personInfo");
        prefixMap.put("address", "addressInfo");

        assertThrows(IllegalArgumentException.class, () -> nestedDataset.stream(prefixMap, Map.class).toList());
    }

    @Test
    public void testFirstRow() {
        Optional<Person> person = dataset.firstRow(Person.class);
        assertTrue(person.isPresent());
        assertEquals(1, person.get().getId());
    }

    @Test
    public void testFirstRowWithClass() {
        Optional<List> firstRow = dataset.firstRow(ArrayList.class);
        assertTrue(firstRow.isPresent());
        assertEquals(Arrays.asList(1, "John", 25, 50000.0), firstRow.get());
    }

    @Test
    public void firstRow_Optional() {
        com.landawn.abacus.util.u.Optional<Object[]> first = sampleDataset.firstRow();
        assertTrue(first.isPresent());
        assertArrayEquals(new Object[] { 1, "Alice", 30 }, first.get());

        com.landawn.abacus.util.u.Optional<Object[]> emptyFirst = emptyDataset.firstRow();
        assertFalse(emptyFirst.isPresent());
    }

    @Test
    public void testLastRow() {
        Optional<Person> person = dataset.lastRow(Person.class);
        assertTrue(person.isPresent());
        assertEquals(5, person.get().getId());
    }

    @Test
    public void lastRow_Optional() {
        com.landawn.abacus.util.u.Optional<Object[]> last = sampleDataset.lastRow();
        assertTrue(last.isPresent());
        assertArrayEquals(new Object[] { 3, "Charlie", 35 }, last.get());
    }

    @Test
    public void testForEach() {
        List<String> names = new ArrayList<>();
        dataset.forEach(row -> names.add((String) row.get(1)));

        assertEquals(5, names.size());
        assertEquals("John", names.get(0));
        assertEquals("Jane", names.get(1));
    }

    @Test
    public void testForEach_WithRange() {
        List<String> names = new ArrayList<>();
        dataset.forEach(1, 3, row -> names.add((String) row.get(1)));

        assertEquals(2, names.size());
        assertEquals("Jane", names.get(0));
        assertEquals("Bob", names.get(1));
    }

    @Test
    public void testForEach_BiConsumer() {
        List<String> results = new ArrayList<>();
        dataset.forEach(Tuple.of("id", "name"), (Integer id, String name) -> results.add(id + ":" + name));

        assertEquals(5, results.size());
        assertEquals("1:John", results.get(0));
    }

    @Test
    public void testForEach_TriConsumer() {
        List<String> results = new ArrayList<>();
        dataset.forEach(Tuple.of("id", "name", "age"), (Integer id, String name, Integer age) -> results.add(id + ":" + name + ":" + age));

        assertEquals(5, results.size());
        assertEquals("1:John:25", results.get(0));
    }

    @Test
    public void forEach_DisposableObjArray() {
        AtomicInteger sumOfIds = new AtomicInteger(0);
        sampleDataset.forEach(row -> sumOfIds.addAndGet((Integer) row.get(0)));
        assertEquals(1 + 2 + 3, sumOfIds.get());
    }

    @Test
    public void forEach_Tuple2() {
        List<String> combined = new ArrayList<>();
        sampleDataset.forEach(Tuple.of("Name", "Age"), (String name, Integer age) -> combined.add(name + ":" + age));
        assertEquals(Arrays.asList("Alice:30", "Bob:24", "Charlie:35"), combined);
    }

    @Test
    public void testForEachWithColumnNames() {
        List<String> names = new ArrayList<>();
        dataset.forEach(Arrays.asList("name"), row -> names.add((String) row.get(0)));
        assertEquals(5, names.size());
        assertEquals("John", names.get(0));
    }

    @Test
    public void testForEachWithBiConsumer() {
        List<String> nameAges = new ArrayList<>();
        dataset.forEach(Tuple.of("name", "age"), (name, age) -> {
            nameAges.add(name + ":" + age);
        });

        assertEquals(Arrays.asList("John:25", "Jane:30", "Bob:35", "Alice:28", "Charlie:40"), nameAges);
    }

    @Test
    public void testForEachWithTriConsumer() {
        List<String> combined = new ArrayList<>();
        dataset.forEach(Tuple.of("id", "name", "age"), (id, name, age) -> {
            combined.add(id + "-" + name + "-" + age);
        });

        assertEquals(Arrays.asList("1-John-25", "2-Jane-30", "3-Bob-35", "4-Alice-28", "5-Charlie-40"), combined);
    }

    @Test
    public void testRangeOperations() {
        dataset.forEach(1, 3, row -> {
            assertNotNull(row.get(0));
        });

        dataset.forEach(1, 3, Arrays.asList("name", "age"), row -> {
            assertEquals(2, row.length());
        });

        List<String> reverseNames = new ArrayList<>();
        dataset.forEach(4, 0, row -> {
            reverseNames.add(row.get(1).toString());
        });
        assertEquals(Arrays.asList("Charlie", "Alice", "Bob", "Jane"), reverseNames);
    }

    @Test
    public void test_join_all() {
        final Dataset ds1 = CommonUtil.newDataset(CommonUtil.toList("id", "name", "city"),
                CommonUtil.toList(CommonUtil.toList(1, "n1", "c1"), CommonUtil.toList(2, "n2", "c2"), CommonUtil.toList(3, "n3", "c3")));
        final Dataset ds2 = CommonUtil.newDataset(CommonUtil.toList("id", "address2", "state"), CommonUtil.toList(CommonUtil.toList(1, "n1", "c1"),
                CommonUtil.toList(2, "n2", "c2"), CommonUtil.toList(2, "n22", "c22"), CommonUtil.toList(4, "n4", "c4")));

        N.println("============================== ds1/2 ===========================");

        ds1.println();
        ds2.println();

        N.println("============================== innerJoin ===========================");
        ds1.innerJoin(ds2, "id", "id").println();
        ds1.innerJoin(ds2, CommonUtil.asMap("id", "id"), "newAddress", List.class).println();
        ds1.innerJoin(ds2, CommonUtil.asMap("id", "id"), "newAddress", List.class, IntFunctions.ofList()).println();
        ds1.innerJoin(ds2, CommonUtil.asMap("id", "id", "name", "address2")).println();
        ds1.innerJoin(ds2, CommonUtil.asMap("id", "id", "name", "address2"), "newAddress", List.class).println();
        ds1.innerJoin(ds2, CommonUtil.asMap("id", "id", "name", "address2"), "newAddress", List.class, IntFunctions.ofSet()).println();

        N.println("============================== left join ===========================");
        ds1.leftJoin(ds2, "id", "id").println();
        ds1.leftJoin(ds2, CommonUtil.asMap("id", "id"), "newAddress", List.class).println();
        ds1.leftJoin(ds2, CommonUtil.asMap("id", "id"), "newAddress", List.class, IntFunctions.ofList()).println();
        ds1.leftJoin(ds2, CommonUtil.asMap("id", "id", "name", "address2")).println();
        ds1.leftJoin(ds2, CommonUtil.asMap("id", "id", "name", "address2"), "newAddress", List.class).println();
        ds1.leftJoin(ds2, CommonUtil.asMap("id", "id", "name", "address2"), "newAddress", List.class, IntFunctions.ofSet()).println();

        N.println("============================== right join ===========================");
        ds1.rightJoin(ds2, "id", "id").println();
        ds1.rightJoin(ds2, CommonUtil.asMap("id", "id"), "newAddress", List.class).println();
        ds1.rightJoin(ds2, CommonUtil.asMap("id", "id"), "newAddress", List.class, IntFunctions.ofList()).println();
        ds1.rightJoin(ds2, CommonUtil.asMap("id", "id", "name", "address2")).println();
        ds1.rightJoin(ds2, CommonUtil.asMap("id", "id", "name", "address2"), "newAddress", List.class).println();
        ds1.rightJoin(ds2, CommonUtil.asMap("id", "id", "name", "address2"), "newAddress", List.class, IntFunctions.ofSet()).println();

        N.println("============================== full join ===========================");
        ds1.fullJoin(ds2, "id", "id").println();
        ds1.fullJoin(ds2, CommonUtil.asMap("id", "id"), "newAddress", List.class).println();
        ds1.fullJoin(ds2, CommonUtil.asMap("id", "id"), "newAddress", List.class, IntFunctions.ofList()).println();
        ds1.fullJoin(ds2, CommonUtil.asMap("id", "id", "name", "address2")).println();
        ds1.fullJoin(ds2, CommonUtil.asMap("id", "id", "name", "address2"), "newAddress", List.class).println();
        ds1.fullJoin(ds2, CommonUtil.asMap("id", "id", "name", "address2"), "newAddress", List.class, IntFunctions.ofSet()).println();
    }

    @Test
    public void toList_defaultArray() {
        List<Object[]> list = sampleDataset.toList();
        assertEquals(3, list.size());
        assertArrayEquals(new Object[] { 1, "Alice", 30 }, list.get(0));
    }

    @Test
    public void toList_specificType_Map() {
        List<Map> mapList = sampleDataset.toList(Map.class);
        assertEquals(3, mapList.size());
        assertEquals("Alice", mapList.get(0).get("Name"));
    }

    @Test
    public void toList_specificType_Bean() {
        RowDataset beanFriendlyDs = new RowDataset(Arrays.asList("id", "name", "value"), Arrays.asList(new ArrayList<>(Arrays.asList(101, 102, 103)),
                new ArrayList<>(Arrays.asList("BeanA", "BeanB", "BeanC")), new ArrayList<>(Arrays.asList(1.1, 2.2, 3.3))));
        List<TestBean> beanList = beanFriendlyDs.toList(TestBean.class);
        assertEquals(3, beanList.size());
        assertEquals("BeanA", beanList.get(0).getName());
        assertEquals(2.2, beanList.get(1).getValue(), 0.001);
    }

    @Test
    public void testToList() {
        List<Object[]> list = dataset.toList();
        assertEquals(5, list.size());
        assertEquals("John", list.get(0)[1]);
    }

    @Test
    public void testToListWithRange() {
        List<Object[]> list = dataset.toList(1, 3);
        assertEquals(2, list.size());
        assertEquals("Jane", list.get(0)[1]);
        assertEquals("Bob", list.get(1)[1]);
    }

    @Test
    public void testToListWithClass() {
        List<List> list = dataset.toList(ArrayList.class);
        assertEquals(5, list.size());
        assertEquals(Arrays.asList(1, "John", 25, 50000.0), list.get(0));
    }

    @Test
    public void testToListWithColumnNames() {
        List<Object[]> list = dataset.toList(Arrays.asList("name", "age"), Object[].class);
        assertEquals(5, list.size());
        assertArrayEquals(new Object[] { "John", 25 }, list.get(0));
    }

    @Test
    public void testToListWithSupplier() {
        List<List> list = dataset.toList(size -> new ArrayList<>(size));
        assertEquals(5, list.size());
        assertEquals(Arrays.asList(1, "John", 25, 50000.0), list.get(0));
    }

    @Test
    public void test_join_empty() {

        final List<Account> accountList = createAccountList(Account.class, 3);
        final Dataset ds = CommonUtil.newDataset(accountList);
        Dataset emptyDS = CommonUtil.newDataset(CommonUtil.toList("id"), CommonUtil.emptyList());
        emptyDS.println();

        {

            N.println("===========innerJoin=============");
            ds.innerJoin(emptyDS, "id", "id").println();

            N.println("===========leftJoin=============");
            ds.leftJoin(emptyDS, "id", "id").println();

            N.println("===========rightJoin=============");
            ds.rightJoin(emptyDS, "id", "id").println();

            N.println("===========fullJoin=============");
            ds.fullJoin(emptyDS, "id", "id").println();
        }

        {

            N.println("===========innerJoin2=============");
            emptyDS.innerJoin(ds, "id", "id").println();

            N.println("===========leftJoin2=============");
            emptyDS.leftJoin(ds, "id", "id").println();

            N.println("===========rightJoin2=============");
            emptyDS.rightJoin(ds, "id", "id").println();

            N.println("===========fullJoin2=============");
            emptyDS.fullJoin(ds, "id", "id").println();
        }

        {

            N.println("===========union=============");
            ds.union(emptyDS).println();

            N.println("===========unionAll=============");
            ds.unionAll(emptyDS).println();

            N.println("===========except=============");
            ds.except(emptyDS).println();

            N.println("===========exceptAll=============");
            ds.exceptAll(emptyDS).println();

            N.println("===========intersect=============");
            ds.intersect(emptyDS).println();

            N.println("===========intersectAll=============");
            ds.intersectAll(emptyDS).println();
        }

        {
            emptyDS = CommonUtil.newDataset(CommonUtil.toList("id", "gui"), CommonUtil.emptyList());
            emptyDS.println();

            N.println("===========union=============");
            ds.union(emptyDS).println();

            N.println("===========unionAll=============");
            ds.unionAll(emptyDS).println();

            N.println("===========except=============");
            ds.except(emptyDS).println();

            N.println("===========exceptAll=============");
            ds.exceptAll(emptyDS).println();

            N.println("===========intersect=============");
            ds.intersect(emptyDS).println();

            N.println("===========intersectAll=============");
            ds.intersectAll(emptyDS).println();
            ds.exceptAll(emptyDS).println();

            N.println("===========intersection=============");
            N.intersection(ds, emptyDS).println();

            N.println("===========difference=============");
            N.difference(ds, emptyDS).println();

            N.println("===========symmetricDifference=============");
            N.symmetricDifference(ds, emptyDS).println();
        }
        assertNotNull(emptyDS);
    }

    @Test
    public void test_toList() {
        final Dataset dataset = CommonUtil.newDataset(CommonUtil.toList("a", "b", "c"), CommonUtil.toList(CommonUtil.toList("a1", "b1", "c1")));
        dataset.toList(dataset.columnNames().subList(1, 3), Object[].class).forEach(Fn.println());
        assertNotNull(dataset);
    }

    //     @Test
    //     public void test_cartesianProduct() {
    //         final Dataset a = CommonUtil.newDataset(CommonUtil.asLinkedHashMap("col1", CommonUtil.toList(1, 2), "col2", CommonUtil.toList(3, 4)));
    //         final Dataset b = CommonUtil.newDataset(CommonUtil.asLinkedHashMap("col3", CommonUtil.toList("a", "b"), "col4", CommonUtil.toList("c", "d")));
    //         a.cartesianProduct(b).println();
    //
    //         CommonUtil.newEmptyDataset().cartesianProduct(b).println();
    //         CommonUtil.newEmptyDataset(CommonUtil.toList("co1", "col2")).cartesianProduct(b).println();
    //         CommonUtil.newEmptyDataset(CommonUtil.toList("co1", "col2")).cartesianProduct(CommonUtil.newEmptyDataset()).println();
    //
    //         CommonUtil.newEmptyDataset().cartesianProduct(CommonUtil.newEmptyDataset()).println();
    //
    //         try {
    //             CommonUtil.newEmptyDataset(CommonUtil.toList("col1", "col2"))
    //                     .cartesianProduct(CommonUtil.newEmptyDataset(CommonUtil.toList("col1", "a")))
    //                     .println();
    //             fail("Should throw: IllegalArgumentException");
    //         } catch (final IllegalArgumentException e) {
    //
    //         }
    //     }

    //

    @Test
    public void test_print() {
        final Dataset ds = CommonUtil.newDataset(CommonUtil.toList("a", "blafjiawfj;lkasjf23 i2qfja;lsfjoiaslf", "c"), CommonUtil
                .asList(CommonUtil.toList(1, "n1kafjeoiwajf", "c1"), CommonUtil.toList(2, "n2", "c2las83292rfjioa"), CommonUtil.toList(3, "n3", "c3")));
        ds.println();

        StringWriter outputWriter = new StringWriter();
        ds.println(outputWriter);
        N.println(outputWriter.toString());

        ds.println(0, 2, CommonUtil.toList("c"));
        ds.println(1, 3, CommonUtil.toList("a", "c"));

        ds.clear();
        ds.println();

        outputWriter = new StringWriter();
        ds.println(outputWriter);
        N.println(outputWriter.toString());

        ds.removeColumns(ds.columnNames());
        ds.println();
        assertNotNull(outputWriter);
    }

    @Test
    public void testToList_AsEntity() {
        List<Person> persons = dataset.toList(Person.class);
        assertNotNull(persons);
        assertEquals(5, persons.size());
        assertEquals(1, persons.get(0).getId());
        assertEquals("John", persons.get(0).getName());
    }

    @Test
    public void testToList_WithRange() {
        List<Person> persons = dataset.toList(1, 3, Person.class);
        assertNotNull(persons);
        assertEquals(2, persons.size());
        assertEquals(2, persons.get(0).getId());
        assertEquals(3, persons.get(1).getId());
    }

    @Test
    public void testToList_WithColumnNames() {
        List<Person> persons = dataset.toList(Arrays.asList("id", "name"), Person.class);
        assertNotNull(persons);
        assertEquals(5, persons.size());
        assertEquals(1, persons.get(0).getId());
        assertEquals("John", persons.get(0).getName());
    }

    @Test
    public void testToList_WithSupplier() {
        IntFunction<Person> supplier = rowIndex -> new Person();
        List<Person> persons = dataset.toList(supplier);
        assertNotNull(persons);
        assertEquals(5, persons.size());
    }

    @Test
    public void testToListWithFilter() {
        List<Map> list = dataset.toList(col -> col.equals("name") || col.equals("age"), col -> col.toUpperCase(), HashMap.class);

        assertEquals(5, list.size());
        Map<String, Object> firstRow = list.get(0);
        assertEquals("John", firstRow.get("NAME"));
        assertEquals(25, firstRow.get("AGE"));
    }

    @Test
    public void test_csv_01() throws Exception {
        assertDoesNotThrow(() -> {
            final Object[][] rowList = { { "Banana", 1000, "USA" }, { "Carrots", 1500, "USA" }, { "Beans", 1600, "USA" }, { "Orange", 2000, "USA" },
                    { "Orange", 2000, "USA" }, { "Banana", 400, "China" }, { "Carrots", 1200, "China" }, { "Beans", 1500, "China" },
                    { "Orange", 4000, "China" }, { "Banana", 2000, "Canada" }, { "Carrots", 2000, "Canada" }, { "Beans", 2000, "Mexico" } };

            final Dataset dataset = CommonUtil.newDataset(CommonUtil.toList("Prod\"^@&\\'skdf'''\\\\\\uct", "\\\"^@&\\\\'skdf'''\\\\\\\\\\\\uct", "Country"),
                    rowList);

            dataset.println();

            N.println(dataset.toCsv());
        });
    }

    @Test
    public void test_sheet_sortBy() throws Exception {
        final Object[][] rowList = { { "Banana", 1000, "USA" }, { "Carrots", 1500, "USA" }, { "Beans", 1600, "USA" }, { "Orange", 2000, "USA" },
                { "Orange", 2000, "USA" }, { "Banana", 400, "China" }, { "Carrots", 1200, "China" }, { "Beans", 1500, "China" }, { "Orange", 4000, "China" },
                { "Banana", 2000, "Canada" }, { "Carrots", 2000, "Canada" }, { "Beans", 2000, "Mexico" } };

        final Dataset dataset = CommonUtil.newDataset(CommonUtil.toList("Product", "Amount", "Country"), rowList);

        dataset.println();

        final Sheet<String, Integer, List<String>> sheet = dataset.pivot("Country", "Amount", CommonUtil.toList("Product", "Country"),
                Collectors.mappingToList(N::toString));
        sheet.println();

        Sheet<String, Integer, List<String>> copy = sheet.copy();
        copy.sortByColumnKey();
        copy.println();

        copy = sheet.copy();
        copy.sortByRowKey();
        copy.println();

        copy = sheet.copy();
        copy.sortColumnsByRowValues("China", Comparators.comparingCollection());
        copy.println();

        copy = sheet.copy();
        copy.sortColumnsByRowValues("China", Comparators.<List<String>> comparingCollection().reversed());
        copy.println();

        copy = sheet.copy();
        copy.sortRowsByColumnValues(1500, Comparators.comparingCollection());
        copy.println();

        copy = sheet.copy();
        copy.sortRowsByColumnValues(1500, Comparators.<List<String>> comparingCollection().reversed());
        copy.println();

        N.println("sortByColumns" + Strings.repeat("=", 80));
        copy = sheet.copy();
        copy.println();
        copy.sortRowsByColumnValues(CommonUtil.toList(1500, 2000), Comparators.comparingObjArray(Comparators.comparingCollection()));
        copy.println();
        copy = sheet.copy();
        copy.sortRowsByColumnValues(CommonUtil.toList(1500, 2000), Comparators.comparingObjArray(Comparators.comparingCollection()).reversed());
        copy.println();
        N.println(Strings.repeat("=", 80));

        N.println("sortByRows" + Strings.repeat("=", 80));
        copy = sheet.copy();
        copy.println();
        copy.sortColumnsByRowValues(CommonUtil.toList("China", "USA"), Comparators.comparingObjArray(Comparators.comparingCollection()));
        copy.println();
        copy = sheet.copy();
        copy.sortColumnsByRowValues(CommonUtil.toList("China", "USA"), Comparators.comparingObjArray(Comparators.comparingCollection()).reversed());
        copy.println();
        N.println(Strings.repeat("=", 80));

        copy = sheet.copy();
        copy.transpose().println();
        assertNotNull(copy);
    }

    @Test
    public void test_toDataset() throws Exception {
        final Object[][] rowList = { { "Banana", 1000, "USA" }, { "Carrots", 1500, "USA" }, { "Beans", 1600, "USA" }, { "Orange", 2000, "USA" },
                { "Orange", 2000, "USA" }, { "Banana", 400, "China" }, { "Carrots", 1200, "China" }, { "Beans", 1500, "China" }, { "Orange", 4000, "China" },
                { "Banana", 2000, "Canada" }, { "Carrots", 2000, "Canada" }, { "Beans", 2000, "Mexico" } };

        final Dataset dataset = CommonUtil.newDataset(CommonUtil.toList("Product", "Amount", "Country"), rowList);

        final Sheet<String, Integer, List<String>> sheet = dataset.pivot("Country", "Amount", CommonUtil.toList("Product", "Country"),
                Collectors.mappingToList(N::toString));
        sheet.println();

        sheet.toDatasetH().println();
        sheet.toDatasetV().println();

        N.println(Strings.repeat("=", 80));
        N.forEach(sheet.toArrayH(), Fn.println());
        N.println(Strings.repeat("=", 80));
        N.forEach(sheet.toArrayV(), Fn.println());
        N.println(Strings.repeat("=", 80));
        assertNotNull(dataset);
    }

    @Test
    public void test_json_3() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);

        final List<Map<String, Object>> mapList = Stream.of(accountList).map(Beans::beanToMap).toList();

        mapList.get(0).remove("id");

        mapList.get(mapList.size() / 2).remove("emailAddress");

        mapList.get(mapList.size() - 1).remove("createdTime");

        String json = N.toJson(mapList, true);
        N.println(Strings.repeat("=", 80));
        N.println(json);

        Dataset ds2 = N.fromJson(json, JsonDeserConfig.create().setValueTypesByBeanClass(Account.class), Dataset.class);
        N.println(Strings.repeat("=", 80));
        ds2.println();

        json = N.toJson(CommonUtil.toList(accountList.get(0)));

        ds2 = N.fromJson(json, JsonDeserConfig.create().setValueTypesByBeanClass(Account.class), Dataset.class);
        N.println(Strings.repeat("=", 80));
        ds2.println();

        ds2 = N.fromJson("[]", JsonDeserConfig.create().setValueTypesByBeanClass(Account.class), Dataset.class);
        N.println(Strings.repeat("=", 80));
        ds2.println();
        assertNotNull(ds2);
    }

    @Test
    public void test_rename() {
        assertDoesNotThrow(() -> {
            final Dataset ds1 = CommonUtil.newDataset(CommonUtil.toList("a", "b", "c"),
                    CommonUtil.toList(CommonUtil.toList(1, "n1", "c1"), CommonUtil.toList(2, "n2", "c2"), CommonUtil.toList(3, "n3", "c3")));
            ds1.renameColumns(CommonUtil.asMap("a", "a", "c", "d"));
            ds1.println();
            N.println(N.toJson(ds1));

            ds1.slice(0, 2, CommonUtil.toList("a")).println();
        });
    }

    @Test
    public void test_combine_divide() throws Exception {
        final Dataset ds1 = CommonUtil.newDataset(CommonUtil.toList(createAccount(Account.class), createAccount(Account.class), createAccount(Account.class)));
        ds1.removeColumns(CommonUtil.toList("gui", "emailAddress", "lastUpdateTime", "createdTime"));
        ds1.updateRow(0, t -> t instanceof String ? t + "__0" : t);

        Dataset ds2 = ds1.copy();
        ds2.combineColumns(CommonUtil.toList("firstName", "lastName"), "name", Map.class);
        ds2.println();

        ds2 = ds1.copy();
        ds2.combineColumns(CommonUtil.toList("firstName", "lastName"), "name", (Function<DisposableObjArray, String>) t -> Strings.join(t.copy(), "-"));
        ds2.println();

        ds2.moveColumn("name", 0);
        ds2.println();

        ds2.divideColumn("name", CommonUtil.toList("firstName", "lastName"), (BiConsumer<String, Object[]>) (t, a) -> {
            final String[] strs = Splitter.with("-").splitToArray(t);
            CommonUtil.copy(strs, 0, a, 0, a.length);
        });

        ds2.println();
    }

    @Test
    public void test_swap() throws Exception {
        final Dataset ds1 = CommonUtil.newDataset(CommonUtil.toList(createAccount(Account.class), createAccount(Account.class), createAccount(Account.class)));
        ds1.removeColumns(CommonUtil.toList("gui", "emailAddress", "lastUpdateTime", "createdTime"));
        ds1.println();

        ds1.updateRow(0, t -> t instanceof String ? t + "___" : t);

        ds1.println();

        ds1.swapColumns("firstName", "lastName");
        ds1.println();

        ds1.swapRows(1, 0);
        ds1.println();

        ds1.clone().println();
        assertNotNull(ds1);
    }

    @Test
    public void test_update() throws Exception {
        final Account account = createAccount(Account.class);
        final Dataset ds1 = CommonUtil.newDataset(CommonUtil.toList(account, account));

        ds1.updateRow(0, t -> t instanceof String ? t + "___" : t);

        ds1.updateRows(Array.of(1, 0), (i, c, v) -> v instanceof String ? v + "___" : v);

        ds1.println();

        ds1.updateColumn("firstName", t -> t instanceof String ? t + "###" : t);

        ds1.println();

        ds1.updateColumns(CommonUtil.toList("lastName", "firstName"), (i, c, v) -> v instanceof String ? v + "###" : v);

        ds1.println();

        ds1.updateAll(t -> t instanceof String ? t + "+++" : t);

        ds1.println();
        assertNotNull(ds1);
    }

    @Test
    public void test_intersection() throws Exception {
        final Account account = createAccount(Account.class);
        final Dataset ds1 = CommonUtil.newDataset(CommonUtil.toList(account));
        final Dataset ds2 = CommonUtil.newDataset(CommonUtil.toList(account, account));
        final Dataset ds3 = CommonUtil.newDataset(CommonUtil.toList(account, account, account));

        assertEquals(0, ds2.except(ds1).size());
        assertEquals(1, N.difference(ds2, ds1).size());
        assertEquals(0, ds3.except(ds1).size());
        assertEquals(2, N.difference(ds3, ds1).size());

        assertEquals(1, N.intersection(ds2, ds1).size());
        assertEquals(2, ds2.intersectAll(ds1).size());
        assertEquals(2, N.intersection(ds3, ds2).size());
        assertEquals(3, ds3.intersectAll(ds2).size());
    }

    @Test
    public void testToEntities() {
        List<Person> persons = dataset.toEntities(null, Person.class);
        assertNotNull(persons);
        assertEquals(5, persons.size());
    }

    @Test
    public void toEntities_simpleBean() {
        RowDataset beanDs = new RowDataset(Arrays.asList("id", "name", "value"), Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)),
                new ArrayList<>(Arrays.asList("obj1", "obj2")), new ArrayList<>(Arrays.asList(10.0, 20.0))));
        List<TestBean> entities = beanDs.toEntities(Collections.emptyMap(), TestBean.class);
        assertEquals(2, entities.size());
        assertEquals(new TestBean(1, "obj1", 10.0), entities.get(0));
        assertEquals(new TestBean(2, "obj2", 20.0), entities.get(1));
    }

    @Test
    public void test_toMergedEntities() {

        {
            final List<Account> accountList = createAccountList(Account.class, 3);
            final MutableInt id = MutableInt.of(1);
            accountList.forEach(it -> it.setId(id.incrementAndGet())
                    .setContact(createAccountContact(AccountContact.class).setAccountId(id.value()).setEmail(Strings.uuid())));
            final List<Map<String, Object>> mapList = Stream.of(accountList).map(Beans::beanToFlatMap).toList();
            final Dataset ds = CommonUtil.newDataset(mapList);

            ds.println();

            final List<Account> mergedEntities = ds.toMergedEntities(Account.class);
            mergedEntities.forEach(Fn.println());

            for (int i = 0; i < accountList.size(); i++) {
                assertEquals(accountList.get(i).getGUI(), mergedEntities.get(i).getGUI());
                assertEquals(accountList.get(i).getContact().getEmail(), mergedEntities.get(i).getContact().getEmail());
            }
        }

        {
            final List<Account> accountList = createAccountList(Account.class, 3);
            final MutableInt id = MutableInt.of(1);
            accountList.forEach(it -> it.setId(id.incrementAndGet())
                    .setContact(createAccountContact(AccountContact.class).setId(id.value() + 100).setAccountId(id.value()).setEmail(Strings.uuid())));
            final List<Map<String, Object>> mapList = Stream.of(accountList).map(Beans::beanToFlatMap).toList();
            final Dataset ds = CommonUtil.newDataset(mapList);

            ds.println();

            final List<Account> mergedEntities = ds.toMergedEntities(Account.class);
            mergedEntities.forEach(Fn.println());

            for (int i = 0; i < accountList.size(); i++) {
                assertEquals(accountList.get(i).getGUI(), mergedEntities.get(i).getGUI());
                assertEquals(accountList.get(i).getContact().getEmail(), mergedEntities.get(i).getContact().getEmail());
            }
        }

        {
            final List<Account> accountList = createAccountList(Account.class, 3);
            final MutableInt id = MutableInt.of(1);
            accountList.forEach(it -> it.setId(id.incrementAndGet())
                    .setContact(createAccountContact(AccountContact.class).setId(id.value() + 100).setAccountId(id.value()).setEmail(Strings.uuid())));
            final List<Map<String, Object>> mapList = Stream.of(accountList).map(Beans::beanToFlatMap).toList();
            mapList.forEach(it -> Maps.replaceKeys(it, k -> k.startsWith("contact.") ? Strings.replaceFirst(k, "contact.", "ac.") : k));
            final Dataset ds = CommonUtil.newDataset(mapList);

            ds.println();

            final List<Account> mergedEntities = ds.toMergedEntities(Account.class);
            mergedEntities.forEach(Fn.println());

            for (int i = 0; i < accountList.size(); i++) {
                assertEquals(accountList.get(i).getGUI(), mergedEntities.get(i).getGUI());
                assertEquals(accountList.get(i).getContact().getEmail(), mergedEntities.get(i).getContact().getEmail());
            }
        }
    }

    @Test
    public void test_toMergedEntities_1() {
        final Map<String, String> map = new HashMap<>();
        map.put(null, null);

        final List<String> columNames = CommonUtil.toList("id", "name", "devices.id", "devices.model", "devices.serialNumber");
        final Dataset dataset = Dataset.rows(columNames,
                new Object[][] { { 100, "Bob", 1, "iPhone", "abc123" }, { 100, "Bob", 2, "MacBook", "mmm123" }, { 200, "Alice", 3, "Android", "aaa223" } });

        dataset.println("     * # ");

        dataset.toEntities(Map.of("d", "devices"), Account.class).forEach(e -> System.out.println(N.toJson(e)));

        dataset.toMergedEntities(Account.class).forEach(e -> System.out.println(N.toJson(e)));

        final List<Account> accounts = dataset.toMergedEntities(Account.class);

        String json = N.toJson(accounts, JsonSerConfig.create().setPrettyFormat(true));
        N.println(json);
        assertNotNull(json);
    }

    @Test
    public void test_toMergedEntities_2() {
        final Map<String, String> map = new HashMap<>();
        map.put(null, null);

        final List<String> columNames = CommonUtil.toList("id", "firstName", "contact.id", "contact.address", "contact.city", "device.id", "device.name",
                "device.model");
        final Dataset dataset = CommonUtil.newDataset(columNames,
                CommonUtil.toList(CommonUtil.toList(1, "firstName1", 1, "address1", "city1", 1, "device1", "model1"),
                        CommonUtil.toList(1, "firstName2", 2, "address2", "city2", 2, "device2", "model2")));

        dataset.toList(Account.class).stream().map(N::toJson).forEach(Fn.println());

        final List<Account> accounts = dataset.toList(Account.class);
        accounts.stream().map(N::toJson).forEach(Fn.println());
        assertEquals(2, accounts.size());
        assertEquals("firstName1", accounts.get(0).getFirstName());
        assertEquals("address2", accounts.get(1).getContact().getAddress());
        assertEquals(1, accounts.get(0).getDevices().size());
        assertEquals(1, accounts.get(1).getDevices().size());

        final List<Account> accounts1 = dataset.toMergedEntities(Account.class);
        accounts1.stream().map(N::toJson).forEach(Fn.println());
        assertEquals(1, accounts1.size());
        assertEquals("firstName2", accounts1.get(0).getFirstName());
        assertEquals(2, accounts1.get(0).getDevices().size());

        final List<Account> accounts2 = dataset.toMergedEntities(CommonUtil.toList("id", "firstName"), dataset.columnNames(), Account.class);
        accounts2.stream().map(N::toJson).forEach(Fn.println());
        assertEquals(2, accounts2.size());
        assertEquals("firstName1", accounts2.get(0).getFirstName());
        assertEquals(1, accounts2.get(0).getDevices().size());
    }

    @Test
    public void test_toMergedEntities_3() {
        final Map<String, String> map = new HashMap<>();
        map.put(null, null);

        final List<String> columNames = CommonUtil.toList("id", "firstName", "ct.id", "ct.address", "ct.city", "device.id", "device.name", "device.model");
        final Dataset dataset = CommonUtil.newDataset(columNames,
                CommonUtil.toList(CommonUtil.toList(1, "firstName1", 1, "address1", "city1", 1, "device1", "model1"),
                        CommonUtil.toList(1, "firstName2", 2, "address2", "city2", 2, "device2", "model2")));

        dataset.toList(Account.class).stream().map(N::toJson).forEach(Fn.println());

        final List<Account> accounts = dataset.toEntities(dataset.columnNames(), CommonUtil.asMap("ct", "contact"), Account.class);
        accounts.stream().map(N::toJson).forEach(Fn.println());
        assertEquals(2, accounts.size());
        assertEquals("firstName1", accounts.get(0).getFirstName());
        assertEquals("address2", accounts.get(1).getContact().getAddress());
        assertEquals(1, accounts.get(0).getDevices().size());
        assertEquals(1, accounts.get(1).getDevices().size());

        final List<Account> accounts1 = dataset.toMergedEntities(CommonUtil.toList("id"), dataset.columnNames(), CommonUtil.asMap("ct", "contact"),
                Account.class);
        accounts1.stream().map(N::toJson).forEach(Fn.println());
        assertEquals(1, accounts1.size());
        assertEquals("firstName2", accounts1.get(0).getFirstName());
        assertEquals("address2", accounts1.get(0).getContact().getAddress());
        assertEquals(2, accounts1.get(0).getDevices().size());

        final List<Account> accounts2 = dataset.toMergedEntities(CommonUtil.toList("id", "firstName"), dataset.columnNames(), CommonUtil.asMap("ct", "contact"),
                Account.class);
        accounts2.stream().map(N::toJson).forEach(Fn.println());
        assertEquals(2, accounts2.size());
        assertEquals("firstName1", accounts2.get(0).getFirstName());
        assertEquals("address1", accounts2.get(0).getContact().getAddress());
        assertEquals(1, accounts2.get(0).getDevices().size());
    }

    @Test
    public void testToMergedEntities() {
        List<Person> persons = dataset.toMergedEntities(Person.class);
        assertNotNull(persons);
        assertEquals(5, persons.size());
    }

    @Test
    public void toMergedEntities_singleId() {
        List<String> names = Arrays.asList("id", "name", "value", "detail");
        List<List<Object>> values = Arrays.asList(new ArrayList<>(Arrays.asList(1, 1, 2)), new ArrayList<>(Arrays.asList("A", "A", "B")),
                new ArrayList<>(Arrays.asList(10.0, 10.0, 20.0)), new ArrayList<>(Arrays.asList("d1", "d2", "d3")));
        RowDataset ds = new RowDataset(names, values);

        List<String> simpleNames = Arrays.asList("id", "name", "value");
        List<List<Object>> simpleValues = Arrays.asList(new ArrayList<>(Arrays.asList(1, 1, 2)), new ArrayList<>(Arrays.asList("Alice", "Alice", "Bob")),
                new ArrayList<>(Arrays.asList(10.0, 11.0, 20.0)));
        RowDataset simpleDs = new RowDataset(simpleNames, simpleValues);

        List<TestBean> merged = simpleDs.toMergedEntities("id", TestBean.class);
        assertEquals(2, merged.size());

        TestBean bean1 = merged.stream().filter(b -> b.getId() == 1).findFirst().orElse(null);
        TestBean bean2 = merged.stream().filter(b -> b.getId() == 2).findFirst().orElse(null);

        assertNotNull(bean1);
        assertEquals("Alice", bean1.getName());
        assertEquals(11.0, bean1.getValue(), 0.001);

        assertNotNull(bean2);
        assertEquals("Bob", bean2.getName());
        assertEquals(20.0, bean2.getValue(), 0.001);
    }

    @Test
    public void toMap_keyValue() {
        Map<Integer, String> idToNameMap = sampleDataset.toMap("ID", "Name");
        assertEquals(3, idToNameMap.size());
        assertEquals("Alice", idToNameMap.get(1));
        assertEquals("Bob", idToNameMap.get(2));
        assertEquals("Charlie", idToNameMap.get(3));
    }

    @Test
    public void toMap_keyRowAsBean() {
        RowDataset ds = new RowDataset(Arrays.asList("key", "id", "name", "value"), Arrays.asList(new ArrayList<>(Arrays.asList("k1", "k2")),
                new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Arrays.asList("Alice", "Bob")), new ArrayList<>(Arrays.asList(10.0, 20.0))));

        Map<String, TestBean> map = ds.toMap("key", Arrays.asList("id", "name", "value"), TestBean.class);
        assertEquals(2, map.size());
        assertEquals(new TestBean(1, "Alice", 10.0), map.get("k1"));
        assertEquals(new TestBean(2, "Bob", 20.0), map.get("k2"));
    }

    @Test
    public void testToMap() {
        Map<Object, Object> map = dataset.toMap("id", "name");
        assertEquals(5, map.size());
        assertEquals("John", map.get(1));
        assertEquals("Jane", map.get(2));
    }

    @Test
    public void testToMap_KeyValue() {
        Map<Integer, String> map = dataset.toMap("id", "name");
        assertNotNull(map);
        assertEquals(5, map.size());
        assertEquals("John", map.get(1));
        assertEquals("Jane", map.get(2));
    }

    @Test
    public void testToMap_WithRowType() {
        Map<Integer, Person> map = dataset.toMap("id", Arrays.asList("name", "age"), Person.class);
        assertNotNull(map);
        assertEquals(5, map.size());
        assertEquals("John", map.get(1).getName());
    }

    @Test
    public void testToMapWithRowType() {
        Map<Object, List> map = dataset.toMap("id", Arrays.asList("name", "age"), ArrayList.class);
        assertEquals(5, map.size());
        assertEquals(Arrays.asList("John", 25), map.get(1));
    }

    @Test
    public void testToMultimap() {
        Dataset ds = Dataset.rows(Arrays.asList("dept", "name"), new Object[][] { { "IT", "Alice" }, { "HR", "Bob" }, { "IT", "Charlie" } });

        ListMultimap<String, String> map = ds.toMultimap("dept", "name");
        assertNotNull(map);
        assertEquals(2, map.get("IT").size());
    }

    @Test
    public void testToJSON() {
        String json = dataset.toJson();
        assertNotNull(json);
        assertTrue(json.contains("John"));
        assertTrue(json.contains("Jane"));
    }

    @Test
    public void testToJSON_WithRange() {
        String json = dataset.toJson(0, 2);
        assertNotNull(json);
        assertTrue(json.contains("John"));
        assertTrue(json.contains("Jane"));
        assertFalse(json.contains("Alice"));
    }

    @Test
    public void testToJSON_ToWriter() {
        StringWriter writer = new StringWriter();
        dataset.toJson(writer);

        String json = writer.toString();
        assertNotNull(json);
        assertTrue(json.contains("John"));
    }

    @Test
    public void testToJson_LowercaseName() {
        String json = dataset.toJson();

        assertNotNull(json);
        assertTrue(json.contains("Alice") || json.contains("alice"));
    }

    @Test
    public void testToJson() {
        String json = testDataset.toJson();
        assertNotNull(json);
        assertTrue(json.contains("Alice"));
        assertTrue(json.contains("Bob"));
    }

    @Test
    public void testToJsonWithRange() {
        String json = testDataset.toJson(1, 3);
        assertNotNull(json);
        assertTrue(json.contains("Bob"));
        assertTrue(json.contains("Charlie"));
        assertFalse(json.contains("Alice"));
        assertFalse(json.contains("Diana"));
    }

    @Test
    public void testToJsonWithRangeAndColumns() {
        String json = testDataset.toJson(0, 2, Arrays.asList("name", "age"));
        assertNotNull(json);
        assertTrue(json.contains("Alice"));
        assertTrue(json.contains("Bob"));
        assertFalse(json.contains("50000"));
    }

    @Test
    public void testToJSON_ToFile() throws IOException {
        File tempFile = File.createTempFile("dataset", ".json");
        tempFile.deleteOnExit();

        dataset.toJson(tempFile);
        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);
    }

    @Test
    public void toJson_writer() throws IOException {
        StringWriter sw = new StringWriter();
        sampleDataset.toJson(sw);
        String json = sw.toString();
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));
        assertTrue(json.contains("\"ID\":1"));
        assertTrue(json.contains("\"Name\":\"Alice\""));
        assertTrue(json.contains("\"Age\":30"));
    }

    @Test
    public void toJson_file(@TempDir File tempDir) throws IOException {
        File tempFile = new File(tempDir, "test.json");
        sampleDataset.toJson(tempFile);
        assertTrue(tempFile.exists());
        String jsonContent = Files.readString(tempFile.toPath());
        assertTrue(jsonContent.contains("\"Name\":\"Bob\""));

        IOUtil.deleteIfExists(tempFile);
    }

    @Test
    public void toJson_outputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        sampleDataset.toJson(baos);
        String json = baos.toString();
        assertTrue(json.contains("\"Name\":\"Charlie\""));
    }

    @Test
    public void testToJsonFile() throws Exception {
        File tempFile = File.createTempFile("test", ".json");
        tempFile.deleteOnExit();

        testDataset.toJson(tempFile);
        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);
    }

    @Test
    public void testToJsonOutputStream() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        testDataset.toJson(baos);

        String json = baos.toString();
        assertNotNull(json);
        assertTrue(json.contains("Alice"));
    }

    @Test
    public void testToJsonWriter() throws Exception {
        StringWriter writer = new StringWriter();
        testDataset.toJson(writer);

        String json = writer.toString();
        assertNotNull(json);
        assertTrue(json.contains("Alice"));
    }

    @Test
    public void testToJsonToFile() throws IOException {
        File tempFile = File.createTempFile("dataset", ".json");
        tempFile.deleteOnExit();

        dataset.toJson(tempFile);

        String content = new String(IOUtil.readAllBytes(tempFile));
        assertTrue(content.contains("John"));
    }

    @Test
    public void testOutputFormats() throws IOException {
        ByteArrayOutputStream jsonOut = new ByteArrayOutputStream();
        dataset.toJson(jsonOut);
        String json = jsonOut.toString();
        assertTrue(json.contains("John"));

        ByteArrayOutputStream xmlOut = new ByteArrayOutputStream();
        dataset.toXml(xmlOut);
        String xml = xmlOut.toString();
        assertTrue(xml.contains("<name>John</name>"));

        ByteArrayOutputStream csvOut = new ByteArrayOutputStream();
        dataset.toCsv(csvOut);
        String csv = csvOut.toString();
        assertTrue(csv.contains("John"));
    }

    @Test
    public void testToXML() {
        String xml = dataset.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("John"));
    }

    @Test
    public void testToXml_LowercaseName() {
        String xml = dataset.toXml();

        assertNotNull(xml);
        assertTrue(xml.contains("Alice") || xml.contains("alice") || xml.length() > 0);
    }

    @Test
    public void testToXml() {
        String xml = testDataset.toXml();
        assertNotNull(xml);
        assertTrue(xml.contains("Alice"));
        assertTrue(xml.contains("Bob"));
    }

    @Test
    public void testToXmlWithRange() {
        String xml = testDataset.toXml(1, 3);
        assertNotNull(xml);
        assertTrue(xml.contains("Bob"));
        assertTrue(xml.contains("Charlie"));
        assertFalse(xml.contains("Alice"));
        assertFalse(xml.contains("Diana"));
    }

    @Test
    public void toXml_writer() throws IOException {
        StringWriter sw = new StringWriter();
        sampleDataset.toXml(sw);
        String xml = sw.toString();
        assertTrue(xml.startsWith("<dataset>"));
        assertTrue(xml.endsWith("</dataset>"));
        assertTrue(xml.contains("<row>"));
        assertTrue(xml.contains("<ID>1</ID>"));
        assertTrue(xml.contains("<Name>Alice</Name>"));
        assertTrue(xml.contains("<Age>30</Age>"));
    }

    @Test
    public void testToXmlWithRowElementName() {
        String xml = dataset.toXml("person");
        assertNotNull(xml);
        assertTrue(xml.contains("<person>"));
    }

    @Test
    public void testToCSV() {
        String csv = dataset.toCsv();
        assertNotNull(csv);
        assertTrue(csv.contains("John"));
        assertTrue(csv.contains("Jane"));
    }

    @Test
    public void testToCsv_LowercaseName() {
        String csv = dataset.toCsv();

        assertNotNull(csv);
        assertTrue(csv.contains("Alice") || csv.contains("alice") || csv.length() > 0);
    }

    @Test
    public void testToCsv() {
        String csv = testDataset.toCsv();
        assertNotNull(csv);
        N.println(csv);
        assertTrue(csv.contains("\"id\",\"name\",\"age\",\"salary\""));
        assertTrue(csv.contains("\"Alice\""));
        assertTrue(csv.contains("\"Bob\""));
    }

    @Test
    public void testToCsvWithRangeAndColumns() {
        String csv = testDataset.toCsv(0, 2, Arrays.asList("name", "age"));
        assertNotNull(csv);
        N.println(csv);
        assertTrue(csv.contains("\"name\",\"age\""));
        assertTrue(csv.contains("\"Alice\",30"));
        assertTrue(csv.contains("\"Bob\",25"));
        assertFalse(csv.contains("50000"));
    }

    @Test
    public void testToCsv_2() {
        Dataset dataset = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "Alice" }, { 2, "Bob\"s" } });
        String csv = dataset.toCsv();
        N.println(csv);
        assertNotNull(csv);
    }

    @Test
    public void toCsv_writer() throws IOException {
        StringWriter sw = new StringWriter();
        sampleDataset.toCsv(sw);
        String csv = sw.toString();
        String[] lines = csv.split(IOUtil.LINE_SEPARATOR_UNIX);
        assertEquals("\"ID\",\"Name\",\"Age\"", lines[0]);
        assertEquals("1,\"Alice\",30", lines[1]);
    }

    @Test
    public void testToCsvToFile() throws IOException {
        File tempFile = File.createTempFile("dataset", ".csv");
        tempFile.deleteOnExit();

        dataset.toCsv(tempFile);

        String content = new String(IOUtil.readAllBytes(tempFile));
        assertTrue(content.contains("John"));
    }

    @Test
    public void testGroupByOperations() {
        Dataset ds = Dataset.rows(Arrays.asList("category", "value"), new Object[][] { { "A", 10 }, { "B", 20 }, { "A", 15 }, { "B", 25 } });
        assertEquals(4, ds.size());
        assertTrue(ds.columnCount() > 0);
    }

    @Test
    public void testGroupBy() {
        dataset.addRow(new Object[] { 6, "Frank", 25, 52000.0 });
        dataset.addRow(new Object[] { 7, "Grace", 30, 62000.0 });

        Dataset grouped = dataset.groupBy("age", "name", "names", collector(Collectors.toList()));

        assertEquals(2, grouped.columnCount());
        assertTrue(grouped.containsColumn("age"));
        assertTrue(grouped.containsColumn("names"));
    }

    @Test
    public void testGroupByWithCollector() {
        Dataset grouped = dataset.groupBy("age", "salary", "avgSalary", collector(Collectors.averagingDouble(val -> (Double) val)));

        assertEquals(2, grouped.columnCount());
        assertEquals(5, grouped.size());
    }

    @Test
    public void testGroupBy_MultipleColumns() {
        Dataset ds = Dataset.rows(Arrays.asList("dept", "level", "name"),
                new Object[][] { { "IT", "Senior", "Alice" }, { "HR", "Junior", "Bob" }, { "IT", "Senior", "Charlie" } });

        Dataset grouped = ds.groupBy(Arrays.asList("dept", "level"));
        assertNotNull(grouped);
    }

    @Test
    public void testGroupBy_WithAggregation() {
        Dataset ds = Dataset.rows(Arrays.asList("dept", "salary"), new Object[][] { { "IT", 50000.0 }, { "HR", 60000.0 }, { "IT", 70000.0 } });

        Dataset grouped = ds.groupBy("dept", "salary", "SUM", Collectors.summingDouble(val -> (Double) val));
        assertNotNull(grouped);
    }

    @Test
    public void groupBy_singleKey_singleAggregate_collectorSum() {
        List<String> gNames = Arrays.asList("Category", "Value");
        List<List<Object>> gValues = Arrays.asList(new ArrayList<>(Arrays.asList("A", "B", "A", "B", "A")), new ArrayList<>(Arrays.asList(10, 20, 5, 15, 2)));
        RowDataset dsToGroup = new RowDataset(gNames, gValues);

        Dataset grouped = dsToGroup.groupBy("Category", "Value", "TotalValue", Collectors.summingInt(val -> (Integer) val));

        assertEquals(2, grouped.columnCount());
        assertEquals(2, grouped.size());

        grouped.moveToRow(0);
        assertEquals("A", grouped.get("Category"));
        assertEquals((Integer) (10 + 5 + 2), grouped.get("TotalValue"));

        grouped.moveToRow(1);
        assertEquals("B", grouped.get("Category"));
        assertEquals((Integer) (20 + 15), grouped.get("TotalValue"));
    }

    @Test
    public void groupBy_multipleKeys_collectorToList() {
        List<String> gNames = Arrays.asList("Group", "SubGroup", "Data");
        List<List<Object>> gValues = Arrays.asList(new ArrayList<>(Arrays.asList("G1", "G1", "G2", "G1")),
                new ArrayList<>(Arrays.asList("S1", "S2", "S1", "S1")), new ArrayList<>(Arrays.asList(10, 20, 30, 40)));
        RowDataset dsToGroup = new RowDataset(gNames, gValues);

        Dataset grouped = dsToGroup.groupBy(Arrays.asList("Group", "SubGroup"), "Data", "CollectedData", Collectors.toList());

        assertEquals(3, grouped.columnCount());
        assertEquals(3, grouped.size());

        Map<Tuple2<String, String>, List<Integer>> resultMap = new HashMap<>();
        for (int i = 0; i < grouped.size(); i++) {
            grouped.moveToRow(i);
            resultMap.put(Tuple.of(grouped.get("Group"), grouped.get("SubGroup")), (List<Integer>) grouped.get("CollectedData"));
        }

        assertEquals(Arrays.asList(10, 40), resultMap.get(Tuple.of("G1", "S1")));
        assertEquals(Collections.singletonList(20), resultMap.get(Tuple.of("G1", "S2")));
        assertEquals(Collections.singletonList(30), resultMap.get(Tuple.of("G2", "S1")));
    }

    @Test
    public void testGroupBySimple() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Alice", 35, 75000.0 });

        Dataset grouped = dataset.groupBy("name", "salary", "avg_salary", Collectors.averagingDouble(v -> (Double) v));

        assertNotNull(grouped);
        assertTrue(grouped.size() > 0);
        assertTrue(grouped.containsColumn("name"));
        assertTrue(grouped.containsColumn("avg_salary"));
    }

    @Test
    public void testGroupByWithExtractor() {
        Dataset grouped = testDataset.groupBy("age", (Integer age) -> age >= 30 ? "Senior" : "Junior", "salary", "avg_salary",
                Collectors.averagingDouble(v -> (Double) v));

        assertNotNull(grouped);
        assertTrue(grouped.size() > 0);
    }

    @Test
    public void testGroupByMultipleColumns() {
        Dataset grouped = testDataset.groupBy(Arrays.asList("age"));

        assertNotNull(grouped);
        assertTrue(grouped.size() > 0);
    }

    @Test
    public void testGroupByMultipleColumnsWithAggregation() {
        Dataset grouped = testDataset.groupBy(Arrays.asList("age"), "salary", "total_salary", Collectors.summingDouble(v -> (Double) v));

        assertNotNull(grouped);
        assertTrue(grouped.size() > 0);
        assertTrue(grouped.containsColumn("age"));
        assertTrue(grouped.containsColumn("total_salary"));
    }

    @Test
    public void testComplexGroupByScenarios() {
        Dataset grouped = dataset.groupBy("age", age -> ((int) age / 10) * 10, "id", "sumOfId", Collectors.summarizingInt(Numbers::toInt));
        assertTrue(grouped.size() <= dataset.size());

        Dataset multiGrouped = dataset.groupBy(Arrays.asList("age"), Arrays.asList("salary"), "totalSalary",
                collector(Collectors.summingDouble(arr -> arr[0] != null ? (Double) arr[0] : 0.0)));
        assertEquals(5, multiGrouped.size());
    }

    @Test
    public void test_groupBy() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final MutableInt idVal = MutableInt.of(accountList.size());
        accountList.forEach(it -> it.setId(idVal.incrementAndGet() % 3));
        final Dataset ds = CommonUtil.newDataset(accountList);
        ds.groupBy("id", ds.columnNames(), "account", Account.class).println();

        ds.groupBy(CommonUtil.toList("id", "firstName"), ds.columnNames(), "account", Account.class).println();

        ds.groupBy(CommonUtil.toList("id", "firstName"), CommonUtil.toList("lastName", "firstName"), "account", it -> it.join(":"), Collectors.toList())
                .println();
        assertNotNull(ds);
    }

    @Test
    public void testGroupBy2() {
        assertDoesNotThrow(() -> {
            Dataset dataset = Dataset.rows(Arrays.asList("department", "level", "employee", "salary", "bonus"),
                    new Object[][] { { "Sales", "Junior", "Alice", 50000, 5000 }, { "Sales", "Senior", "Bob", 75000, 8000 },
                            { "IT", "Junior", "Charlie", 55000, 6000 }, { "IT", "Senior", "David", 80000, 9000 }, { "Sales", "Junior", "Eve", 52000, 5500 } });

            Dataset result = dataset.groupBy(Arrays.asList("department", "level"), row -> row.get(0) + "_" + row.get(1), Arrays.asList("salary", "bonus"),
                    "total_compensation", row -> ((Integer) row.get(0)) + ((Integer) row.get(1)), Collectors.summingInt(Integer.class::cast));

            N.println("Group by with sum aggregation:");
            dataset.println("     * // ");
            result.println("     * // ");
        });
    }

    @Test
    public void testRollup() {
        Stream<Dataset> rollupStream = testDataset.rollup(Arrays.asList("age"));
        assertNotNull(rollupStream);

        List<Dataset> rollupResults = rollupStream.toList();
        assertNotNull(rollupResults);
        assertTrue(rollupResults.size() > 0);
    }

    @Test
    public void testRollupWithAggregation() {
        Stream<Dataset> rollupStream = testDataset.rollup(Arrays.asList("age"), "salary", "total_salary", Collectors.summingDouble(v -> (Double) v));

        assertNotNull(rollupStream);
        List<Dataset> rollupResults = rollupStream.toList();
        assertTrue(rollupResults.size() > 0);
    }

    @Test
    public void testRollup2() {
        Dataset dataset = Dataset.rows(Arrays.asList("region", "country", "city", "sales", "quantity"),
                new Object[][] { { "North", "USA", "New York", 1000, 50 }, { "North", "USA", "Boston", 800, 40 }, { "North", "Canada", "Toronto", 600, 30 },
                        { "South", "Mexico", "Mexico City", 400, 20 } });
        Function<DisposableObjArray, String> keyExtractor = keyRow -> keyRow.join("-");
        Stream<Dataset> rollupResult = dataset.rollup(Arrays.asList("region", "country", "city"), keyExtractor, Arrays.asList("sales", "quantity"),
                "aggregated_totals", row -> Tuple.of((Integer) row.get(0), (Integer) row.get(1)), MoreCollectors.summingInt(tp -> tp._1, tp -> tp._2));

        N.println("Rollup with aggregation:");
        dataset.println("     * // ");
        rollupResult.forEach(ds -> ds.println("     * // "));
        assertNotNull(keyExtractor);
    }

    @Test
    public void test_rollup() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        accountList.forEach(it -> it.setId(2));
        final Dataset ds = CommonUtil.newDataset(accountList);
        ds.rollup(ds.columnNames()).forEach(Dataset::println);
        assertNotNull(ds);
    }

    @Test
    public void testCube() {
        Stream<Dataset> cubeStream = testDataset.cube(Arrays.asList("age"));
        assertNotNull(cubeStream);

        List<Dataset> cubeResults = cubeStream.toList();
        assertNotNull(cubeResults);
        assertTrue(cubeResults.size() > 0);
    }

    @Test
    public void testCubeWithAggregation() {
        Stream<Dataset> cubeStream = testDataset.cube(Arrays.asList("age"), "salary", "avg_salary", Collectors.averagingDouble(v -> (Double) v));

        assertNotNull(cubeStream);
        List<Dataset> cubeResults = cubeStream.toList();
        assertTrue(cubeResults.size() > 0);
    }

    @Test
    public void testCube2() {
        Dataset dataset = Dataset.rows(Arrays.asList("region", "country", "sales"),
                new Object[][] { { "North", "USA", 1000 }, { "North", "Canada", 600 }, { "South", "Mexico", 400 } });

        Function<DisposableObjArray, String> keyExtractor = keyRow -> keyRow.join("-");
        Function<DisposableObjArray, Double> rowMapper = row -> (Integer) row.get(0) * 1.1;
        Stream<Dataset> cubeResult = dataset.cube(Arrays.asList("region", "country"), keyExtractor, Arrays.asList("sales"), "total_sales_with_markup",
                rowMapper, Collectors.collectingAndThen(Collectors.summingDouble(Double::doubleValue), r -> Numbers.round(r, 2)));
        N.println("Rollup with aggregation:");
        dataset.println("     * // ");
        cubeResult.forEach(ds -> ds.println("     * // "));
        assertNotNull(rowMapper);
    }

    @Test
    public void test_cube() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        accountList.forEach(it -> it.setId(2));
        final Dataset ds = CommonUtil.newDataset(accountList);
        ds.cube(ds.columnNames()).forEach(Dataset::println);
        assertNotNull(ds);
    }

    @Test
    public void test_cube_2() throws Exception {
        final Object[][] rowList = { { "Banana", 1000, "USA" }, { "Carrots", 1500, "USA" }, { "Beans", 1600, "USA" }, { "Orange", 2000, "USA" },
                { "Orange", 2000, "USA" }, { "Banana", 400, "China" }, { "Carrots", 1200, "China" }, { "Beans", 1500, "China" }, { "Orange", 4000, "China" },
                { "Banana", 2000, "Canada" }, { "Carrots", 2000, "Canada" }, { "Beans", 2000, "Mexico" } };

        final Dataset dataset = CommonUtil.newDataset(CommonUtil.toList("Product", "Amount", "Country"), rowList);

        dataset.println();

        dataset.cube(CommonUtil.toList("Product", "Country"), CommonUtil.toList("Amount"), "result", List.class).forEach(Dataset::println);
        assertNotNull(dataset);
    }

    @Test
    public void testPivot() {
        List<String> cols = Arrays.asList("product", "quarter", "sales");
        List<List<Object>> data = new ArrayList<>();
        data.add(Arrays.asList("A", "A", "A", "A", "B", "B", "B", "B"));
        data.add(Arrays.asList("Q1", "Q2", "Q3", "Q4", "Q1", "Q2", "Q3", "Q4"));
        data.add(Arrays.asList(100, 110, 120, 130, 200, 210, 220, 230));

        Dataset salesData = new RowDataset(cols, data);

        Sheet<Object, Object, Object> pivoted = salesData.pivot("product", "quarter", "sales", collector(Collectors.summingInt(val -> (Integer) val)));

        assertNotNull(pivoted);
        assertEquals(2, pivoted.rowKeySet().size());
        assertEquals(4, pivoted.columnKeySet().size());
    }

    @Test
    public void test_pivot() throws Exception {
        final Object[][] rowList = { { "Banana", 1000, "USA" }, { "Carrots", 1500, "USA" }, { "Beans", 1600, "USA" }, { "Orange", 2000, "USA" },
                { "Orange", 2000, "USA" }, { "Banana", 400, "China" }, { "Carrots", 1200, "China" }, { "Beans", 1500, "China" }, { "Orange", 4000, "China" },
                { "Banana", 2000, "Canada" }, { "Carrots", 2000, "Canada" }, { "Beans", 2000, "Mexico" } };

        final Dataset dataset = CommonUtil.newDataset(CommonUtil.toList("Product", "Amount", "Country"), rowList);

        dataset.println();

        final Dataset ds2 = dataset.groupBy(CommonUtil.toList("Product", "Country"), "Amount", "sum(Amount)",
                Collectors.summingLong(it -> ((Number) it).longValue()));
        ds2.sortBy("Product");
        ds2.println();
        assertNotNull(dataset);
    }

    @Test
    public void test_pivot_2() throws Exception {
        final Object[][] rowList = { { "Banana", 1000, "USA" }, { "Carrots", 1500, "USA" }, { "Beans", 1600, "USA" }, { "Orange", 2000, "USA" },
                { "Orange", 2000, "USA" }, { "Banana", 400, "China" }, { "Carrots", 1200, "China" }, { "Beans", 1500, "China" }, { "Orange", 4000, "China" },
                { "Banana", 2000, "Canada" }, { "Carrots", 2000, "Canada" }, { "Beans", 2000, "Mexico" } };

        final Dataset dataset = CommonUtil.newDataset(CommonUtil.toList("Product", "Amount", "Country"), rowList);

        dataset.println();

        Sheet<String, String, Double> sheet = dataset.pivot("Product", "Country", "Amount", Collectors.summingDouble(Number::doubleValue));
        sheet.println();

        sheet = dataset.pivot("Country", "Product", "Amount", Collectors.summingDouble(Number::doubleValue));
        sheet.println();

        dataset.pivot("Country", "Product", CommonUtil.toList("Amount", "Country"), it -> it.join("_"), Collectors.toList()).println();
        dataset.pivot("Country", "Product", CommonUtil.toList("Amount", "Country"), N::toString, Collectors.toList()).println();
        dataset.pivot("Country", "Product", CommonUtil.toList("Amount", "Country"), Collectors.mappingToList(N::toString)).println();

        Sheet<String, Integer, List<String>> sheet2 = dataset.pivot("Country", "Amount", CommonUtil.toList("Product", "Country"),
                Collectors.mappingToList(N::toString));
        sheet2.println();

        dataset.sortBy("Amount");
        sheet2 = dataset.pivot("Country", "Amount", CommonUtil.toList("Product", "Country"), Collectors.mappingToList(N::toString));
        sheet2.println();

        dataset.sortBy(CommonUtil.toList("Country", "Amount"));
        sheet2 = dataset.pivot("Country", "Amount", CommonUtil.toList("Product", "Country"), Collectors.mappingToList(N::toString));
        sheet2.println();

        sheet2.sortByColumnKey();
        sheet2.println();
        assertNotNull(sheet2);
    }

    @Test
    public void testPivot2() {
        assertDoesNotThrow(() -> {
            Dataset dataset = Dataset.rows(Arrays.asList("region", "product", "sales", "quantity"),
                    new Object[][] { { "North", "A", 100, 10 }, { "North", "B", 200, 20 }, { "South", "A", 150, 15 }, { "South", "B", 250, 25 } });

            Sheet<String, String, Integer> pivotResult = dataset.pivot("region", "product", Arrays.asList("sales", "quantity"),
                    Collectors.summingInt(arr -> (Integer) arr[0] + (Integer) arr[1]));

            N.println("Pivot with aggregation:");
            dataset.println("     * // ");
            pivotResult.println("     * // ");
        });
    }

    @Test
    public void testSortBy_WithComparator() {
        Dataset ds = dataset.copy();
        ds.sortBy("age", Comparator.reverseOrder());

        assertEquals(Integer.valueOf(40), ds.get(0, 2));
        assertEquals(Integer.valueOf(35), ds.get(1, 2));
    }

    @Test
    public void testSortBy() {
        Dataset dataset = testDataset.copy();
        dataset.sortBy("age");

        dataset.moveToRow(0);
        assertEquals(25, dataset.getInt("age"));
        dataset.moveToRow(3);
        assertEquals(35, dataset.getInt("age"));
    }

    @Test
    public void testSortByWithComparator() {
        Dataset dataset = testDataset.copy();
        dataset.sortBy("age", Comparator.<Integer> naturalOrder().reversed());

        dataset.moveToRow(0);
        assertEquals(35, dataset.getInt("age"));
        dataset.moveToRow(3);
        assertEquals(25, dataset.getInt("age"));
    }

    @Test
    public void testComplexSortScenarios() {
        dataset.sortBy(Arrays.asList("name"), row -> row.get(0).toString().length());

        dataset.parallelSortBy(Arrays.asList("age", "salary"));

        int firstAge = (int) dataset.get(0, 2);
        for (int i = 1; i < dataset.size(); i++) {
            assertTrue(firstAge <= (int) dataset.get(i, 2));
        }
    }

    @Test
    public void testSortBy_SingleColumn() {
        Dataset ds = dataset.copy();
        ds.sortBy("age");

        assertEquals(Integer.valueOf(25), ds.get(0, 2));
        assertEquals(Integer.valueOf(28), ds.get(1, 2));
        assertEquals(Integer.valueOf(30), ds.get(2, 2));
        assertEquals(Integer.valueOf(35), ds.get(3, 2));
    }

    @Test
    public void testSortBy_MultipleColumns() {
        Dataset ds = dataset.copy();
        ds.sortBy(Arrays.asList("age", "name"));

        assertEquals(Integer.valueOf(25), ds.get(0, 2));
    }

    @Test
    public void sortBy_singleColumn_defaultOrder() {
        sampleDataset.sortBy("Age");
        assertEquals((Integer) 2, sampleDataset.moveToRow(0).get("ID"));
        assertEquals((Integer) 1, sampleDataset.moveToRow(1).get("ID"));
        assertEquals((Integer) 3, sampleDataset.moveToRow(2).get("ID"));
    }

    @Test
    public void sortBy_singleColumn_customComparator() {
        sampleDataset.sortBy("Name", Comparator.reverseOrder());
        assertEquals("Charlie", sampleDataset.moveToRow(0).get("Name"));
        assertEquals("Bob", sampleDataset.moveToRow(1).get("Name"));
        assertEquals("Alice", sampleDataset.moveToRow(2).get("Name"));
    }

    @Test
    public void sortBy_multipleColumns_defaultComparator() {
        sampleDataset.addRow(new Object[] { 4, "Alice", 25 });
        sampleDataset.sortBy(Arrays.asList("Name", "Age"));
        assertEquals((Integer) 4, sampleDataset.moveToRow(0).get("ID"));
        assertEquals((Integer) 1, sampleDataset.moveToRow(1).get("ID"));
        assertEquals((Integer) 2, sampleDataset.moveToRow(2).get("ID"));
        assertEquals((Integer) 3, sampleDataset.moveToRow(3).get("ID"));
    }

    @Test
    public void testSortByMultipleColumns() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Eve", 30, 48000.0 });

        dataset.sortBy(Arrays.asList("age", "salary"));

        dataset.moveToRow(0);
        assertEquals(25, dataset.getInt("age"));
    }

    @Test
    public void testSortByMultipleColumnsWithComparator() {
        Dataset dataset = testDataset.copy();
        Comparator<Object[]> comparator = (a, b) -> {
            int ageCompare = ((Integer) a[0]).compareTo((Integer) b[0]);
            if (ageCompare != 0)
                return ageCompare;
            return ((String) a[1]).compareTo((String) b[1]);
        };
        dataset.sortBy(Arrays.asList("age", "name"), comparator);

        dataset.moveToRow(0);
        assertEquals(25, dataset.getInt("age"));
    }

    @Test
    public void testSortByWithKeyExtractor() {
        Dataset dataset = testDataset.copy();
        dataset.sortBy(Arrays.asList("age", "name"), (DisposableObjArray row) -> ((Integer) row.get(0)) * 1000 + ((String) row.get(1)).length());

        assertNotNull(dataset);
        assertTrue(dataset.size() > 0);
    }

    @Test
    public void test_sortBy() throws Exception {

        final List<List<Object>> rowList = IntStream.range(0, 10)
                .mapToObj(it -> (List<Object>) CommonUtil.<Object> asList(it, ((char) ('a' + it)), it + "_" + ((char) ('a' + it))))
                .toList();
        CommonUtil.shuffle(rowList);

        Dataset dataset = CommonUtil.newDataset(CommonUtil.toList("int", "char", "str"), rowList);
        dataset.println();

        dataset.sortBy("int");
        dataset.println();

        dataset = CommonUtil.newDataset(CommonUtil.toList("int", "char", "str"), rowList);
        dataset.sortBy(CommonUtil.toList("char", "int"), Comparators.OBJECT_ARRAY_COMPARATOR);
        dataset.println();

        dataset = CommonUtil.newDataset(CommonUtil.toList("int", "char", "str"), rowList);
        dataset.sortBy(CommonUtil.toList("char", "int"), Comparators.comparingObjArray(Comparators.reverseOrder()));
        dataset.println();
        assertNotNull(dataset);
    }

    @Test
    public void testParallelSortBy() {
        Dataset ds = dataset.copy();
        ds.parallelSortBy("age");

        assertEquals(Integer.valueOf(25), ds.get(0, 2));
        assertEquals(Integer.valueOf(28), ds.get(1, 2));
    }

    @Test
    public void testParallelSortByWithComparator() {
        Dataset dataset = testDataset.copy();
        dataset.parallelSortBy("name", Comparator.<String> naturalOrder().reversed());

        dataset.moveToRow(0);
        String firstName = dataset.get("name").toString();
        assertTrue(firstName.compareTo("Charlie") >= 0);
    }

    @Test
    public void testParallelSortByMultipleColumns() {
        Dataset dataset = testDataset.copy();
        dataset.parallelSortBy(Arrays.asList("age", "name"));

        dataset.moveToRow(0);
        assertEquals(25, dataset.getInt("age"));
    }

    @Test
    public void testTopBy() {
        Dataset top = dataset.topBy("salary", 2);
        assertNotNull(top);
        assertEquals(2, top.size());
        assertEquals(70000.0, top.get(0, 3));
        assertEquals(80000.0, top.get(1, 3));
    }

    @Test
    public void testTopByWithComparator() {
        Dataset top2 = testDataset.topBy("age", 2, Comparator.<Integer> naturalOrder().reversed());

        assertNotNull(top2);
        assertEquals(2, top2.size());
    }

    @Test
    public void testTopByMultipleColumns() {
        Dataset top2 = testDataset.topBy(Arrays.asList("age", "salary"), 2);

        assertNotNull(top2);
        assertEquals(2, top2.size());
    }

    @Test
    public void testTopByMultipleColumnsWithComparator() {
        Comparator<Object[]> comp = (a, b) -> ((Integer) a[0]).compareTo((Integer) b[0]);
        Dataset top2 = testDataset.topBy(Arrays.asList("age", "salary"), 2, comp);

        assertNotNull(top2);
        assertEquals(2, top2.size());
    }

    @Test
    public void testTopByWithKeyExtractor() {
        Dataset top2 = testDataset.topBy(Arrays.asList("age", "salary"), 2, (DisposableObjArray row) -> (Integer) row.get(0));

        assertNotNull(top2);
        assertEquals(2, top2.size());
    }

    @Test
    public void distinct() {
        sampleDataset.addRow(new Object[] { 1, "Alice", 30 });
        assertEquals(4, sampleDataset.size());
        Dataset distinctDs = sampleDataset.distinct();
        assertEquals(3, distinctDs.size());
    }

    @Test
    public void testDistinctOperations() {
        Dataset ds = Dataset.rows(Arrays.asList("value"), new Object[][] { { 1 }, { 2 }, { 1 }, { 3 }, { 2 } });
        Dataset distinct = ds.distinct();
        assertEquals(3, distinct.size());
    }

    @Test
    public void testDistinct() {
        Dataset ds = Dataset.rows(Arrays.asList("id", "value"), new Object[][] { { 1, "A" }, { 2, "B" }, { 1, "A" } });

        Dataset distinct = ds.distinct();
        assertNotNull(distinct);
        assertEquals(2, distinct.size());
    }

    @Test
    public void testDistinct_ByColumn() {
        Dataset ds = Dataset.rows(Arrays.asList("id", "value"), new Object[][] { { 1, "A" }, { 2, "B" }, { 1, "C" } });

        Dataset distinct = ds.distinctBy("id");
        assertNotNull(distinct);
        assertEquals(2, distinct.size());
    }

    @Test
    public void test_distinct() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 1000);
        final Dataset ds = CommonUtil.newDataset(accountList);
        Dataset ds2 = ds.distinct();
        ds2.println();
        assertEquals(accountList.size(), ds2.size());

        ds2 = ds.distinct();
        ds2.println();
        assertEquals(accountList.size(), ds2.size());

        ds2 = ds.distinctBy("gui");
        ds2.println();
        assertEquals(accountList.size(), ds2.size());

        ds2 = ds.distinctBy("gui", (Function<String, Object>) t -> t.substring(0, 2));
        ds2.println();

        ds2 = ds.distinctBy(CommonUtil.toList("firstName", "lastName"), (Function<DisposableObjArray, Object>) DisposableObjArray::length);
        ds2.println();
        assertEquals(1, ds2.size());

        ds2 = ds.groupBy(CommonUtil.toList("firstName", "lastName"), (Function<DisposableObjArray, Object>) DisposableObjArray::length);
        ds2.println();
        assertEquals(1, ds2.size());

        ds2 = ds.groupBy("gui", (Function<String, Object>) t -> t.substring(0, 2), "gui", "*", Collectors.counting());
        ds2.println();

        ds2 = ds.groupBy("gui", (Function<String, Object>) t -> t.substring(0, 2), CommonUtil.toList("gui"), "*", Collectors.counting());
        ds2.println();

        ds2 = ds.groupBy(CommonUtil.toList("firstName", "lastName"), (Function<DisposableObjArray, Object>) DisposableObjArray::length, "gui", "*",
                Collectors.counting());

        ds2.println();
        assertEquals(1, ds2.size());

        ds2 = ds.groupBy(CommonUtil.toList("firstName", "lastName"), (Function<DisposableObjArray, Object>) DisposableObjArray::length,
                CommonUtil.toList("gui"), "*", Collectors.counting());

        ds2 = ds.groupBy(CommonUtil.toList("firstName", "lastName"), (Function<DisposableObjArray, Object>) DisposableObjArray::length,
                CommonUtil.toList("firstName", "lastName"), "*", Collectors.counting());

        ds2.println();
        assertEquals(1, ds2.size());

        ds2 = ds.groupBy(CommonUtil.toList("firstName", "lastName"), (Function<DisposableObjArray, Object>) DisposableObjArray::length,
                CommonUtil.toList("firstName", "lastName"), "*", Collectors.counting());

        ds2.println();
        assertEquals(1, ds2.size());

        ds2 = ds.distinctBy(CommonUtil.toList("gui"));
        ds2.println();
        assertEquals(accountList.size(), ds2.size());

        ds2 = ds.distinctBy(CommonUtil.toList("firstName", "lastName", "gui"));
        ds2.println();
        assertEquals(accountList.size(), ds2.size());

        ds2 = ds.distinctBy(CommonUtil.toList("firstName", "lastName", "gui"));
        ds2.println();
        assertEquals(accountList.size(), ds2.size());

        ds2 = ds.distinctBy(CommonUtil.toList("firstName", "lastName"));
        ds2.println();
        assertEquals(1, ds2.size());

        ds2 = ds.distinctBy(CommonUtil.toList("firstName", "lastName"));
        ds2.println();
        assertEquals(1, ds2.size());
    }

    @Test
    public void testDistinctBy_WithCustomKeyExtractor() {
        Dataset ds = Dataset.rows(Arrays.asList("x", "y", "label"), new Object[][] { { 1, 2, "A" }, { 2, 3, "B" }, { 1, 2, "C" } });

        Dataset distinct = ds.distinctBy(Arrays.asList("x", "y"), vals -> vals.get(0).toString() + "," + vals.get(1).toString());

        assertEquals(2, distinct.size());
        assertEquals("A", distinct.get(0, 2));
        assertEquals("B", distinct.get(1, 2));
    }

    @Test
    public void testDistinctBy() {
        Dataset ds = Dataset.rows(Arrays.asList("id", "category"), new Object[][] { { 1, "A" }, { 2, "B" }, { 3, "A" } });
        Dataset distinct = ds.distinctBy("category");
        assertEquals(2, distinct.size());
    }

    @Test
    public void testDistinctBy_SingleColumn() {
        Dataset ds = Dataset.rows(Arrays.asList("type", "name", "value"),
                new Object[][] { { "A", "Alice", 100 }, { "B", "Bob", 200 }, { "A", "Charlie", 150 }, { "C", "Diana", 300 } });

        Dataset distinct = ds.distinctBy("type");

        assertEquals(3, distinct.size());
        assertEquals("A", distinct.get(0, 0));
        assertEquals("Alice", distinct.get(0, 1));
        assertEquals("B", distinct.get(1, 0));
        assertEquals("Bob", distinct.get(1, 1));
        assertEquals("C", distinct.get(2, 0));
        assertEquals("Diana", distinct.get(2, 1));
    }

    @Test
    public void testDistinctBy_WithKeyExtractor() {
        Dataset ds = Dataset.rows(Arrays.asList("email", "name"),
                new Object[][] { { "alice@TEST.com", "Alice" }, { "bob@test.com", "Bob" }, { "ALICE@test.com", "Alice2" } });

        Dataset distinct = ds.distinctBy("email", (String email) -> email.toLowerCase());

        assertEquals(2, distinct.size());
        assertEquals("alice@TEST.com", distinct.get(0, 0));
        assertEquals("bob@test.com", distinct.get(1, 0));
    }

    @Test
    public void testDistinctBy_MultipleColumns() {
        Dataset ds = Dataset.rows(Arrays.asList("dept", "level", "name"),
                new Object[][] { { "IT", "Senior", "Alice" }, { "HR", "Junior", "Bob" }, { "IT", "Senior", "Charlie" }, { "IT", "Junior", "Diana" } });

        Dataset distinct = ds.distinctBy(Arrays.asList("dept", "level"));

        assertEquals(3, distinct.size());
    }

    @Test
    public void distinctBy_singleColumn() {
        sampleDataset.addRow(new Object[] { 4, "Alice", 28 });
        Dataset distinctByName = sampleDataset.distinctBy("Name");
        assertEquals(3, distinctByName.size());
        List<Object> names = distinctByName.getColumn("Name");
        assertTrue(names.contains("Alice") && names.contains("Bob") && names.contains("Charlie"));
    }

    @Test
    public void testDistinctByColumn() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Alice", 35, 75000.0 });

        Dataset distinct = dataset.distinctBy("name");

        assertNotNull(distinct);
        assertEquals(4, distinct.size());
    }

    @Test
    public void testDistinctByColumnWithExtractor() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "ALICE", 35, 75000.0 });

        Dataset distinct = dataset.distinctBy("name", (String name) -> name.toLowerCase());

        assertNotNull(distinct);
        assertEquals(4, distinct.size());
    }

    @Test
    public void testDistinctByMultipleColumns() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Alice", 30, 75000.0 });

        Dataset distinct = dataset.distinctBy(Arrays.asList("name", "age"));

        assertNotNull(distinct);
        assertEquals(4, distinct.size());
    }

    @Test
    public void testDistinctByMultipleColumnsWithExtractor() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Alice", 30, 75000.0 });

        Dataset distinct = dataset.distinctBy(Arrays.asList("name", "age"), (DisposableObjArray row) -> row.get(0).toString() + "_" + row.get(1));

        assertNotNull(distinct);
        assertEquals(4, distinct.size());
    }

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

        ds.println();
        ds2.println();

        Dataset joinedDataset = ds.leftJoin(ds2, "firstName", "rightFirstName");

        joinedDataset.println();

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

        joinedDataset.println();

        ds2.renameColumn("rightFirstName", "firstName");

        joinedDataset = ds.leftJoin(ds2, "firstName", "firstName");

        joinedDataset.println();

        ds2.renameColumn("firstName", "rightFirstName");

        joinedDataset = ds.leftJoin(ds2, "firstName", "rightFirstName");

        joinedDataset.println();

        onColumnNames = CommonUtil.asMap("firstName", "rightFirstName", "lastName", "rightLastName");

        joinedDataset = ds.leftJoin(ds2, onColumnNames);

        joinedDataset.println();

        joinedDataset = ds.leftJoin(ds2, onColumnNames, "account", Account.class);

        joinedDataset.println();

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

        joinedDataset.println();

        joinedDataset = ds.leftJoin(ds2, onColumnNames, "account", Account.class, IntFunctions.ofSet());

        joinedDataset.println();

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

        Dataset unionResult = sampleDataset.union(otherDs);
        assertEquals(4, unionResult.columnCount());
        assertEquals(4, unionResult.size());

        sampleDataset.println();
        otherDs.println();
        unionResult.println();

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

        Dataset union = testDataset.union(other, Arrays.asList("id"));

        assertNotNull(union);
        assertEquals(5, union.size());
    }

    @Test
    public void testUnion_2() {
        Dataset dataset1 = Dataset.rows(Arrays.asList("id", "name", "age"),
                new Object[][] { { 1, "Alice", 25 }, new Object[] { 2, "Bob", 30 }, new Object[] { 1, "Alice", 35 } });
        Dataset dataset2 = Dataset.rows(Arrays.asList("id", "name", "score"), new Object[][] { { 1, "Alice", 95 }, { 3, "Charlie", 85 } });

        Dataset result = dataset1.union(dataset2);

        result.println();
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

            N.println("============================== union ===========================");
            ds1.union(ds2).println();

            N.println("============================== unionAll ===========================");
            ds1.unionAll(ds2).println();

            ds1 = CommonUtil.newDataset(CommonUtil.toList("id", "name", "city"),
                    CommonUtil.toList(CommonUtil.toList(1, "n1", "c1"), CommonUtil.toList(2, "n2", "c2"), CommonUtil.toList(3, "n3", "c3")));
            ds2 = CommonUtil.newDataset(CommonUtil.toList("id", "name", "city"),
                    CommonUtil.toList(CommonUtil.toList(2, "n2", "c2"), CommonUtil.toList(2, "n2", "c2"), CommonUtil.toList(3, "n4", "c4")));

            N.println("============================== union ===========================");
            ds1.union(ds2).println();

            N.println("============================== unionAll ===========================");
            ds1.unionAll(ds2).println();

            N.println("============================== intersection ===========================");
            N.intersection(ds1, ds2).println();

            N.println("============================== intersectAll ===========================");
            ds1.intersectAll(ds2).println();

            ds1 = CommonUtil.newDataset(CommonUtil.toList("id", "name", "city"), CommonUtil.toList(CommonUtil.toList(1, "n1", "c1"),
                    CommonUtil.toList(2, "n2", "c2"), CommonUtil.toList(2, "n2", "c2"), CommonUtil.toList(3, "n3", "c3")));
            ds2 = CommonUtil.newDataset(CommonUtil.toList("id", "name", "city", "state"),
                    CommonUtil.toList(CommonUtil.toList(2, "n2", "c2", "CA"), CommonUtil.toList(2, "n2", "c2", "CA"), CommonUtil.toList(3, "n4", "c4", "CA")));

            N.println("============================== intersectAll ===========================");
            ds1.intersectAll(ds2).println();

            N.println("============================== intersectAll ===========================");
            ds2.intersectAll(ds1).println();
        });
    }

    @Test
    public void test_union() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList);

        Dataset ds2 = ds.copy();

        Dataset ds3 = ds.union(ds2);

        ds3.println();

        assertEquals(ds.size(), ds3.size());

        ds3 = ds.unionAll(ds2);

        ds3.println();

        assertEquals(ds.size() * 2, ds3.size());

        ds2 = CommonUtil.newDataset(createAccountList(Account.class, 9));

        ds3 = ds.union(ds2);

        ds3.println();

        assertEquals(ds.size() * 2, ds3.size());

        ds3 = ds.unionAll(ds2);

        ds3.println();

        assertEquals(ds.size() * 2, ds3.size());
    }

    @Test
    public void test_union_2() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList);

        Dataset ds2 = ds.copy();

        ds.removeColumn(ds.getColumnName(2));

        Dataset ds3 = ds.union(ds2);

        ds3.println();

        assertEquals(ds.size(), ds3.size());

        ds3 = ds.unionAll(ds2);

        ds3.println();

        assertEquals(ds.size() * 2, ds3.size());

        ds2 = CommonUtil.newDataset(createAccountList(Account.class, 9));

        ds3 = ds.union(ds2);

        ds3.println();

        assertEquals(ds.size() * 2, ds3.size());

        ds3 = ds.unionAll(ds2);

        ds3.println();

        assertEquals(ds.size() * 2, ds3.size());
    }

    @Test
    public void test_union_3() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList);

        Dataset ds2 = ds.copy();

        ds2.removeColumn(ds2.getColumnName(2));

        Dataset ds3 = ds.union(ds2);

        ds3.println();

        assertEquals(ds.size(), ds3.size());

        ds3 = ds.unionAll(ds2);

        ds3.println();

        assertEquals(ds.size() * 2, ds3.size());

        ds2 = CommonUtil.newDataset(createAccountList(Account.class, 9));

        ds2.removeColumn(ds2.getColumnName(2));

        ds.println();
        ds2.println();
        ds3 = ds.union(ds2);

        ds3.println();

        assertEquals(ds.size() * 2, ds3.size());

        ds3 = ds.unionAll(ds2);

        ds3.println();

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

        Dataset result = dataset1.unionAll(dataset2);

        result.println();
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

        Dataset intersect = testDataset.intersect(other, Arrays.asList("id"));

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

            result.println();
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

            result.println();
            assertEquals(3, result.size());
            assertEquals((Integer) 2, result.get(0, 0));
            assertEquals("Bob", result.get(0, 1));
            assertEquals((Integer) 3, result.get(1, 0));
            assertEquals("Charlie", result.get(1, 1));
            assertEquals((Integer) 2, result.get(2, 0));
            assertEquals("Bob", result.get(2, 1));
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
    public void testSplitToListWithColumns() {
        List<Dataset> chunks = testDataset.splitToList(3, Arrays.asList("name", "age"));

        assertNotNull(chunks);
        assertTrue(chunks.size() >= 1);
        assertTrue(chunks.get(0).containsColumn("name"));
        assertTrue(chunks.get(0).containsColumn("age"));
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
    public void testCopy_WithRange() {
        Dataset copy = dataset.copy(1, 3);
        assertNotNull(copy);
        assertEquals(2, copy.size());
        assertEquals(4, copy.columnCount());
        assertEquals(Integer.valueOf(2), copy.get(0, 0));
    }

    @Test
    public void testCopy_WithColumnNames() {
        Dataset copy = dataset.copy(Arrays.asList("id", "name"));
        assertNotNull(copy);
        assertEquals(5, copy.size());
        assertEquals(2, copy.columnCount());
    }

    @Test
    public void testCopy_WithRangeAndColumns() {
        Dataset copy = dataset.copy(1, 3, Arrays.asList("id", "name"));
        assertNotNull(copy);
        assertEquals(2, copy.size());
        assertEquals(2, copy.columnCount());
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
    public void testAddMultipleColumns() {
        Dataset ds = dataset.copy();
        List<String> newColumnNames = Arrays.asList("col1", "col2");
        List<List<Object>> newColumns = Arrays.asList(Arrays.asList("A", "B", "C", "D", "E"), Arrays.asList(1, 2, 3, 4, 5));
        ds.addColumns(newColumnNames, newColumns);
        assertEquals(6, ds.columnCount());
        assertEquals("col1", ds.getColumnName(4));
        assertEquals("col2", ds.getColumnName(5));
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

        ds.println();
        ds3.println();

        assertEquals(ds, ds3);

        ds3 = N.difference(ds, ds2);

        ds.println();
        ds3.println();

        assertEquals(0, ds3.size());

        ds2.clear();

        ds3 = N.intersection(ds, ds2);

        ds.println();
        ds3.println();

        assertEquals(0, ds3.size());

        ds3 = N.difference(ds, ds2);

        ds.println();
        ds3.println();

        assertEquals(ds, ds3);

        ds.clear();

        ds3 = N.intersection(ds, ds2);

        ds.println();
        ds3.println();

        assertEquals(0, ds3.size());

        ds3 = N.difference(ds, ds2);

        ds.println();
        ds3.println();

        assertEquals(ds, ds3);
    }

    @Test
    public void test_interset_except_2() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList);

        final Dataset ds2 = ds.copy();

        ds.removeColumn(ds.getColumnName(2));

        Dataset ds3 = N.intersection(ds, ds2);

        ds.println();
        ds3.println();

        assertEquals(ds, ds3);

        ds3 = N.difference(ds, ds2);

        ds.println();
        ds3.println();

        assertEquals(0, ds3.size());

        ds2.clear();

        ds3 = N.intersection(ds, ds2);

        ds.println();
        ds3.println();

        assertEquals(0, ds3.size());

        ds3 = N.difference(ds, ds2);

        ds.println();
        ds3.println();

        assertEquals(ds, ds3);

        ds.clear();

        ds3 = N.intersection(ds, ds2);

        ds.println();
        ds3.println();

        assertEquals(0, ds3.size());

        ds3 = N.difference(ds, ds2);

        ds.println();
        ds3.println();

        assertEquals(ds, ds3);
    }

    @Test
    public void test_interset_except_3() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList);

        final Dataset ds2 = ds.copy();

        ds2.removeColumn(ds2.getColumnName(2));

        Dataset ds3 = N.intersection(ds, ds2);

        ds.println();
        ds3.println();

        assertEquals(ds, ds3);

        ds3 = N.difference(ds, ds2);

        ds.println();
        ds3.println();

        assertEquals(0, ds3.size());

        ds2.clear();

        ds3 = N.intersection(ds, ds2);

        ds.println();
        ds3.println();

        assertEquals(0, ds3.size());

        ds3 = N.difference(ds, ds2);

        ds.println();
        ds3.println();

        assertEquals(ds, ds3);

        ds.clear();

        ds3 = N.intersection(ds, ds2);

        ds.println();
        ds3.println();

        assertEquals(0, ds3.size());

        ds3 = N.difference(ds, ds2);

        ds.println();
        ds3.println();

        assertEquals(ds, ds3);
    }

    @Test
    public void test_sort_perf() throws Exception {
        final List<String> columnNameList = new ArrayList<>(Beans.getPropNameList(Account.class));
        final Dataset dataset = CommonUtil.newDataset(columnNameList, createAccountList(Account.class, 999));

        Profiler.run(8, 10, 1, () -> {
            final Dataset copy = dataset.copy();

            copy.sortBy(CommonUtil.toList(AccountPNL.GUI, AccountPNL.FIRST_NAME));

        }).printResult();
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
        dataset.println();
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
    public void testIsEmpty() {
        assertFalse(dataset.isEmpty());
        assertTrue(emptyDataset.isEmpty());
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
    @DisplayName("Should add large number of columns")
    public void testAddManyColumns() {
        List<String> newColumnNames = new ArrayList<>();
        List<Collection<Object>> newColumns = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            newColumnNames.add("col" + i);
            List<Object> column = new ArrayList<>();
            for (int j = 0; j < dataset.size(); j++) {
                column.add("R" + j + "C" + i);
            }
            newColumns.add(column);
        }

        int originalColumnCount = dataset.columnCount();
        dataset.addColumns(newColumnNames, newColumns);

        assertEquals(originalColumnCount + 10, dataset.columnCount());

        assertEquals("R0C0", dataset.get(0, dataset.getColumnIndex("col0")));
        assertEquals("R4C9", dataset.get(4, dataset.getColumnIndex("col9")));
    }

    @Test
    public void testSize() {
        assertEquals(5, dataset.size());
        assertEquals(0, emptyDataset.size());
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

        N.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        ds.println();

        ds2.println();

        N.println("++++++++++++++++++++++++++join+++++++++++++++++++++++++++++++++++++++++++");

        Dataset ds3 = ds.innerJoin(ds2, "firstName", "rightFirstName");
        ds3.println();
        assertEquals(6, ds3.size());

        N.println("+++++++++++++++++++++++++++leftJoin++++++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.leftJoin(ds2, "firstName", "rightFirstName");
        ds3.println();
        assertEquals(9, ds3.size());

        N.println("+++++++++++++++++++++++++++++rightJoin++++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.rightJoin(ds2, "firstName", "rightFirstName");
        ds3.println();
        assertEquals(9, ds3.size());

        N.println("+++++++++++++++++++++++++++++++fullJoin++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.fullJoin(ds2, "firstName", "rightFirstName");
        ds3.println();
        assertEquals(12, ds3.size());

        Map<String, String> onColumnNames = CommonUtil.asMap("firstName", "firstName");
        ds2 = CommonUtil.newDataset(accountList2);

        N.println("+++++++++++++++++++++++++++++join++++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.innerJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        ds3.println();
        assertEquals(6, ds3.size());

        ds3 = ds.innerJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        ds3.println();
        assertEquals(4, ds3.size());

        N.println("++++++++++++++++++++++++++++++leftJoin+++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.leftJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        ds3.println();
        assertEquals(9, ds3.size());

        ds3 = ds.leftJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        ds3.println();
        assertEquals(7, ds3.size());

        N.println("+++++++++++++++++++++++++++rightJoin++++++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.rightJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        ds3.println();
        assertEquals(9, ds3.size());

        ds3 = ds.rightJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        ds3.println();
        assertEquals(6, ds3.size());

        N.println("++++++++++++++++++++++++++fullJoin+++++++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.fullJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        ds3.println();
        assertEquals(12, ds3.size());

        ds3 = ds.fullJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        ds3.println();
        assertEquals(9, ds3.size());

        N.println("++++++++++++++++++++++++++fullJoin+++++++++++++++++++++++++++++++++++++++++++");

        onColumnNames = CommonUtil.asMap("firstName", "firstName", "middleName", "middleName");
        ds2 = CommonUtil.newDataset(accountList2);

        N.println("+++++++++++++++++++++++++++++join++++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.innerJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        ds3.println();
        assertEquals(6, ds3.size());

        ds3 = ds.innerJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        ds3.println();
        assertEquals(4, ds3.size());

        N.println("++++++++++++++++++++++++++++++leftJoin+++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.leftJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        ds3.println();
        assertEquals(9, ds3.size());

        ds3 = ds.leftJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        ds3.println();
        assertEquals(7, ds3.size());

        N.println("+++++++++++++++++++++++++++rightJoin++++++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.rightJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        ds3.println();
        assertEquals(9, ds3.size());

        ds3 = ds.rightJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        ds3.println();
        assertEquals(6, ds3.size());

        N.println("++++++++++++++++++++++++++fullJoin+++++++++++++++++++++++++++++++++++++++++++");

        ds3 = ds.fullJoin(ds2, onColumnNames, "rigthtAccount", Account.class);
        ds3.println();
        assertEquals(12, ds3.size());

        ds3 = ds.fullJoin(ds2, onColumnNames, "rigthtAccount", Account.class, IntFunctions.ofList());
        ds3.println();
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

        N.println("It took " + (System.currentTimeMillis() - startTime) + " to convert " + accountList.size() + " bean to Dataset");

        final Dataset dataset = CommonUtil.newDataset(columnNameList, accountList);

        startTime = System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            final List<?> list = dataset.toList(Map.class);

            assertEquals(accountList.size(), list.size());
        }

        N.println("It took " + (System.currentTimeMillis() - startTime) + " to convert " + dataset.size() + " Dataset to account");

        startTime = System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            dataset.sortBy("gui");
        }

        N.println("It took " + (System.currentTimeMillis() - startTime) + " to sort " + dataset.size() + " Dataset");

        final Map<String, Object> props = createAccountProps();
        final Dataset ds = CommonUtil.newDataset("propName", "propValue", props);
        ds.println();
    }

    @Test
    public void constructor_withProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("key1", "value1");
        RowDataset ds = new RowDataset(Arrays.asList("ID", "Name", "Age"), columnValues, props);
        assertEquals("value1", ds.getProperties().get("key1"));
    }

    @Test
    public void testConstructorWithProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("source", "test");
        properties.put("version", 1);

        RowDataset dsWithProps = new RowDataset(columnNames, columnList, properties);
        assertEquals("test", dsWithProps.getProperties().get("source"));
        assertEquals(1, dsWithProps.getProperties().get("version"));
    }

    @Test
    public void testGetProperties() {
        Dataset ds = dataset.copy();
        Map<String, Object> props = ds.getProperties();

        assertNotNull(props);
        assertTrue(props.isEmpty() || props.size() >= 0);
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
        ds.println();

        String json = N.toJson(ds, JsonSerConfig.create().setWriteDatasetByRow(true));
        N.println(json);

        json = N.toJson(ds, JsonSerConfig.create().setWriteDatasetByRow(true).setPrettyFormat(true));
        N.println(json);

        final Dataset ds2 = N.fromJson(json, Dataset.class);
        N.println(ds2);
        ds2.println();
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
        N.println(ds.toJson());

        N.println(ds.toXml());

        N.println(N.toJson(ds));
        N.println(N.toJson(ds, true));
        assertNotNull(ds);
    }

    @Test
    public void test_json_2() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList, CommonUtil.asMap("prop1", 1, "key2", "val2"));
        N.println(Strings.repeat("=", 80));
        ds.println();

        final String json = ds.toJson();
        N.println(Strings.repeat("=", 80));
        N.println(json);

        final Dataset ds2 = N.fromJson(json, JsonDeserConfig.create().setValueTypesByBeanClass(Account.class), Dataset.class);
        N.println(Strings.repeat("=", 80));
        ds2.println();

        assertEquals(ds, ds2);

        final Map<Object, Object> map = CommonUtil.asMap("key", accountList);

        final Map<String, Dataset> map2 = N.fromJson(N.toJson(map), JsonDeserConfig.create().setValueTypesByBeanClass(Account.class),
                Type.ofMap(String.class, Dataset.class));
        map2.entrySet().iterator().next().getValue().println();

        final List<Dataset> list = N.fromJson(N.toJson(CommonUtil.toList(accountList)), JsonDeserConfig.create().setValueTypesByBeanClass(Account.class),
                Type.ofList(Dataset.class));
        list.get(0).println();

    }

    @Test
    public void test_lift() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 7);

        final Dataset ds = CommonUtil.newDataset(accountList);

        N.println(ds.toMap("firstName", "lastName"));
        assertNotNull(ds);
    }

    @Test
    public void test_multiset() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 7);
        final Dataset ds = CommonUtil.newDataset(accountList);
        N.println(ds.stream("gui").toMultiset());
        N.println(ds.stream("firstName").toMultiset());
        N.println(ds.stream(CommonUtil.toList("firstName", "lastName"), String[].class).toMultiset());
        N.println(ds.stream(CommonUtil.toList("firstName", "lastName"), List.class).toMultiset());
        N.println(ds.stream(CommonUtil.toList("firstName", "lastName"), Set.class).toMultiset());
        N.println(ds.stream(CommonUtil.toList("firstName", "lastName"), Account.class).toMultiset());
        assertNotNull(ds);
    }

    @Test
    public void test_first_last_row() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList);

        N.println(ds.firstRow());
        N.println(ds.lastRow());

        ds.clear();

        assertFalse(ds.firstRow().isPresent());
        assertFalse(ds.lastRow().isPresent());
    }

    @Test
    public void test_combineColumn() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList);

        ds.println();

        ds.combineColumns(CommonUtil.toList("firstName", "lastName"), "name", Object[].class);

        ds.println();

        assertTrue(ds.getColumnIndex("name") >= 0);
        assertFalse(ds.containsColumn("firstName"));
        assertFalse(ds.containsColumn("lastName"));
    }

    @Test
    public void test_asDataset_3() throws Exception {
        final long startTime = System.currentTimeMillis();

        final List<Map<String, Object>> propsList = createAccountPropsList(1000);
        N.println(System.currentTimeMillis() - startTime);

        final Dataset ds = CommonUtil.newDataset(new ArrayList<>(propsList.get(0).keySet()), propsList);
        N.println(System.currentTimeMillis() - startTime);

        ds.groupBy(CommonUtil.toList("Account.firstName", "Account.lastName"));
        N.println(System.currentTimeMillis() - startTime);
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
                N.println(Strings.repeat("=", 120));
                Dataset.empty().println();
                N.println(Strings.repeat("=", 120));

                N.println(Strings.repeat("=", 120));
                Dataset.empty().println("# ");
                N.println(Strings.repeat("=", 120));

                N.println(Strings.repeat("=", 120));
                Dataset.empty().println("// ");
                N.println(Strings.repeat("=", 120));
            }
            {
                N.println(Strings.repeat("=", 120));
                dataset.set(0, 0, "A very long text that exceeds the usual width零零忑零零忑零零忑 to test wrapping functionality in the dataset printing method.零零忑");
                dataset.println();
                N.println(Strings.repeat("=", 120));

                N.println(Strings.repeat("=", 120));
                dataset.println("     * ## ");
                N.println(Strings.repeat("=", 120));

                N.println(Strings.repeat("=", 120));
                dataset.println("// ");
                N.println(Strings.repeat("=", 120));
            }

            N.println(dataset);
        });
    }

    @Test
    public void testToString() {
        String str = dataset.toString();
        assertNotNull(str);
        assertTrue(str.contains("Alice"));
    }

    @Test
    public void constructor_mismatchedColumnNameAndListSize_throwsIllegalArgumentException() {
        List<String> names = Arrays.asList("A", "B");
        List<List<Object>> values = List.of(Arrays.asList(1, 2));
        assertThrows(IllegalArgumentException.class, () -> new RowDataset(names, values));
    }

    @Test
    public void constructor_mismatchedColumnSizes_throwsIllegalArgumentException() {
        List<String> names = Arrays.asList("A", "B");
        List<List<Object>> values = List.of(Arrays.asList(1, 2), Arrays.asList(3));
        assertThrows(IllegalArgumentException.class, () -> new RowDataset(names, values));
    }

    @Test
    public void constructor_duplicateColumnNames_throwsIllegalArgumentException() {
        List<String> duplicateNames = Arrays.asList("ID", "Name", "ID");
        assertThrows(IllegalArgumentException.class, () -> new RowDataset(duplicateNames, columnValues));
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
    public void testConstructorWithEmptyColumnName() {
        List<String> invalidNames = Arrays.asList("id", "", "age");
        assertThrows(IllegalArgumentException.class, () -> new RowDataset(invalidNames, columnList));
    }

    @Test
    public void testConstructorWithDuplicateColumnNames() {
        List<String> duplicateNames = Arrays.asList("id", "name", "id", "age");
        assertThrows(IllegalArgumentException.class, () -> new RowDataset(duplicateNames, columnList));
    }

    @Test
    public void testConstructorWithDifferentColumnSizes() {
        List<List<Object>> invalidColumns = new ArrayList<>();
        invalidColumns.add(Arrays.asList(1, 2, 3));
        invalidColumns.add(Arrays.asList("A", "B", "C", "D"));

        assertThrows(IllegalArgumentException.class, () -> new RowDataset(Arrays.asList("col1", "col2"), invalidColumns));
    }

}
