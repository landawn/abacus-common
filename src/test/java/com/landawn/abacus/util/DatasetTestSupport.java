package com.landawn.abacus.util;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;

import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.AbstractTest;

import testfixtures.entity.extendDirty.basic.DataType;

public abstract class DatasetTestSupport extends AbstractTest {

    protected Dataset dataset;
    protected Dataset emptyDataset;
    protected List<String> columnNames;
    protected List<List<Object>> columnList;
    protected Object[][] sampleRows;
    protected Object[][] testData;
    protected Dataset testDataset;
    protected RowDataset sampleDataset;
    protected List<List<Object>> columnValues;

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
        protected int id;
        protected String name;
        protected int age;
        protected double salary;

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
        protected int id;
        protected String name;
        protected double value;
        protected TestNestedBean nested;
        protected List<TestNestedBean> nestedList;

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
        protected String detail;

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

    protected RowDataset createSimpleDataset() {
        List<String> names = new ArrayList<>(Arrays.asList("ID", "Name", "Age"));
        List<List<Object>> values = new ArrayList<>();
        values.add(new ArrayList<>(Arrays.asList(1, 2, 3)));
        values.add(new ArrayList<>(Arrays.asList("Alice", "Bob", "Charlie")));
        values.add(new ArrayList<>(Arrays.asList(30, 24, 35)));
        return new RowDataset(names, values);
    }

    protected static <T, A, R> Collector<T, A, R> collector(Collector<T, A, R> downstream) {
        return downstream;
    }

    public void addDataType() {
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

    //

    // ----- Bug-fix regression tests -----
}
